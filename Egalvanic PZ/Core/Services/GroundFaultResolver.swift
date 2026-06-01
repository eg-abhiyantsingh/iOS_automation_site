//
//  GroundFaultResolver.swift
//  Egalvanic PZ
//
//  ZP-2243: Ground Fault sibling resolver — on-device mirror of
//  ``_find_gf_sibling_oid`` in BE's ``skm_library_routes.py``.
//
//  SKM stores Ground Fault settings as a separate ``cdevice``
//  (``sz_dev_type='Low Voltage Breakers - Ground Fault'``) paired
//  with the Static Trip / Power Circuit / HVMV-with-trip device the
//  user picks. This resolver finds the paired GF cdevice given a
//  primary device using three matching rules, in strength order:
//
//    1. Catalog AND (sz_type_norm + desc_rest) both agree — safest.
//    2. (sz_type_norm + desc_rest) only — covers products without
//       a catalog or where catalog is shared across multiple GF
//       variants.
//    3. Catalog alone — last resort when descriptions diverge.
//
//  Normalization rules mirror BE's regex strips:
//    - ``sz_type``: trailing ``(G)`` / ``(GF)`` removed (some mfrs,
//      e.g. ABB EMAX PR111(G), put the GF marker in sz_type itself).
//    - ``sz_description``: leading ``[A-Z]{1,5},`` removed (LSI, LI,
//      LSIG, GF, etc. — pure-letter prefixes only, so descriptions
//      with numeric leads like "400AF, ..." aren't truncated).
//
//  Returns ``nil`` when no GF sibling resolves — the UI then offers
//  the manual picker (``GroundFaultPickerSheet``).
//
import Foundation
import SwiftData

enum GroundFaultResolver {
    /// SKM's literal ``sz_dev_type`` for Ground Fault breakers.
    static let gfSzDevType = "Low Voltage Breakers - Ground Fault"

    /// Catalog placeholders that don't carry real pairing signal.
    private static let placeholderCatalogs: Set<String> = [
        "See Notes", "NA", "N/A", "TBD", "UNK", "-"
    ]

    /// Strip trailing `` (G)`` / `` (GF)`` from sz_type.
    private static func normalizeSzType(_ s: String?) -> String {
        guard let s = s else { return "" }
        // Manual regex-equivalent: trim trailing whitespace, then strip
        // a trailing "(G)" or "(GF)" (case-insensitive on the F).
        var out = s.trimmingCharacters(in: .whitespaces)
        let patterns = [" (G)", " (g)", " (GF)", " (gf)", " (Gf)", " (gF)",
                        "(G)", "(g)", "(GF)", "(gf)"]
        for p in patterns {
            if out.hasSuffix(p) {
                out = String(out.dropLast(p.count)).trimmingCharacters(in: .whitespaces)
                break
            }
        }
        return out
    }

    /// Strip leading ``[A-Z]{1,5},`` from sz_description (the trip-
    /// label prefix — LSI, LI, LSIG, GF, etc.).
    private static func normalizeDescription(_ s: String?) -> String {
        guard let s = s else { return "" }
        // Find a comma in the first ~8 chars; if everything before it
        // is 1–5 uppercase letters, strip the prefix (plus trailing
        // whitespace).
        guard let commaIdx = s.firstIndex(of: ",") else { return s }
        let prefix = s[..<commaIdx]
        let letters = prefix.trimmingCharacters(in: .whitespaces)
        guard letters.count >= 1, letters.count <= 5 else { return s }
        let allUpper = letters.allSatisfy { $0.isLetter && $0.isUppercase }
        guard allUpper else { return s }
        // Drop "<letters>," plus any whitespace following the comma.
        let afterComma = s.index(after: commaIdx)
        let tail = s[afterComma...].drop { $0 == " " || $0 == "\t" }
        return String(tail)
    }

    /// Resolve the GF sibling for ``primary`` against the device
    /// headers already synced into SwiftData. Returns the matched
    /// header's ``id`` (= cdevice_oid) or ``nil`` when no rule fires.
    static func findGfSiblingOid(
        for primary: SkmDeviceHeader,
        in modelContext: ModelContext
    ) -> Int? {
        // Don't auto-pair when the primary IS a GF device.
        if primary.sz_dev_type == gfSzDevType { return nil }

        guard let manufacturerId = primary.manufacturer_id else { return nil }
        let primaryOid = primary.id
        let catalog = (primary.sz_catalog ?? "").trimmingCharacters(in: .whitespaces)
        let hasCleanCatalog = !catalog.isEmpty
            && !placeholderCatalogs.contains(catalog)
        let szTypeNorm = normalizeSzType(primary.sz_type)
        let descRest = normalizeDescription(primary.sz_description)
        let hasDescSignal = !szTypeNorm.isEmpty

        // Pre-load candidate GF rows. We fetch by sz_dev_type only in
        // the predicate to keep the #Predicate macro simple (Optional
        // Int comparisons can trigger type-check timeouts), then narrow
        // by manufacturer + oid in Swift. The full GF row count is
        // small (~thousands max), so the post-filter cost is fine.
        let gfDevType = gfSzDevType
        let descriptor = FetchDescriptor<SkmDeviceHeader>(
            predicate: #Predicate { row in
                row.sz_dev_type == gfDevType
            }
        )
        let allGf: [SkmDeviceHeader] = (try? modelContext.fetch(descriptor)) ?? []
        let candidates: [SkmDeviceHeader] = allGf.filter { row in
            row.manufacturer_id == manufacturerId && row.id != primaryOid
        }
        if candidates.isEmpty { return nil }

        // Pre-sort once — all three rules want the lowest-id match.
        let sorted: [SkmDeviceHeader] = candidates.sorted { $0.id < $1.id }

        // Rule 1: catalog AND (sz_type_norm + desc_rest) both agree.
        if hasCleanCatalog && hasDescSignal {
            for row in sorted {
                let catalogMatches: Bool = (row.sz_catalog ?? "") == catalog
                let typeMatches: Bool = normalizeSzType(row.sz_type) == szTypeNorm
                let descMatches: Bool = normalizeDescription(row.sz_description) == descRest
                if catalogMatches && typeMatches && descMatches {
                    return row.id
                }
            }
        }

        // Rule 2: description signal alone.
        if hasDescSignal {
            for row in sorted {
                let typeMatches: Bool = normalizeSzType(row.sz_type) == szTypeNorm
                let descMatches: Bool = normalizeDescription(row.sz_description) == descRest
                if typeMatches && descMatches {
                    return row.id
                }
            }
        }

        // Rule 3: catalog alone.
        if hasCleanCatalog {
            for row in sorted {
                if (row.sz_catalog ?? "") == catalog {
                    return row.id
                }
            }
        }

        return nil
    }

    // MARK: - GF chassis pegs (mirrors web's gfPicks memo)

    /// Resolved Ground Fault chassis pegs — mirrors the chassis
    /// structure of the SST-side picks. Each SKM cdevice (SST or GF)
    /// has its own row IDs, but a paired SST+GF for the same physical
    /// breaker shares chassis structure, so we match deterministically
    /// on ``(f_voltage, f_size)`` for frames and ``f_size`` for sensors.
    struct Picks {
        var frame: SkmFrame?
        var frameLabel: String?
        var sensor: SkmSensor?
        var plugAmps: Double?
    }

    /// Compute the GF picks for an active SST selection. Loads the
    /// GF device's frames/sensors fresh — caller is responsible for
    /// caching when invoked frequently.
    static func picks(
        sstFrame: SkmFrame?,
        sstSensor: SkmSensor?,
        sstPlugAmps: Double?,
        gfDeviceOid: Int,
        in modelContext: ModelContext
    ) -> Picks {
        let frameDescriptor = FetchDescriptor<SkmFrame>(
            predicate: #Predicate { $0.device_oid == gfDeviceOid }
        )
        let gfFrames: [SkmFrame] = (try? modelContext.fetch(frameDescriptor)) ?? []

        // Match (f_voltage, f_size) — strongest, since paired GF
        // shares chassis structure across voltage tiers.
        var matchedFrame: SkmFrame? = nil
        if let sstF = sstFrame {
            matchedFrame = gfFrames.first(where: {
                guard let v = $0.f_voltage, let sstV = sstF.f_voltage,
                      let s = $0.f_size, let sstS = sstF.f_size else { return false }
                return v == sstV && s == sstS
            })
            if matchedFrame == nil {
                matchedFrame = gfFrames.first(where: {
                    guard let s = $0.f_size, let sstS = sstF.f_size else { return false }
                    return s == sstS
                })
            }
        }
        if matchedFrame == nil { matchedFrame = gfFrames.first }

        // Sensors of the matched frame.
        var matchedSensor: SkmSensor? = nil
        if let frameId = matchedFrame?.id {
            let sensorDescriptor = FetchDescriptor<SkmSensor>(
                predicate: #Predicate { $0.frame_id == frameId }
            )
            let frameSensors: [SkmSensor] = (try? modelContext.fetch(sensorDescriptor))
                ?? []
            if let sstS = sstSensor {
                matchedSensor = frameSensors.first(where: {
                    guard let v = $0.f_size, let sstV = sstS.f_size else { return false }
                    return v == sstV
                })
            }
            if matchedSensor == nil {
                matchedSensor = frameSensors
                    .sorted { ($0.f_size ?? 0) < ($1.f_size ?? 0) }
                    .first
            }
        }

        // Plug: exact match against the requested value, falling back
        // to the sensor's first plug, then to the sensor's f_size (for
        // sensors with empty plug lists, like ABB EMAX/PR111).
        var matchedPlug: Double? = nil
        if let sensor = matchedSensor {
            let plugs = sensor.plugs
            if let requested = sstPlugAmps,
               let found = plugs.first(where: { $0 == requested }) {
                matchedPlug = found
            } else if let firstPlug = plugs.first {
                matchedPlug = firstPlug
            } else {
                matchedPlug = sensor.f_size
            }
        }

        // SKM-shaped composite frame label —
        //   "{f_voltage}V {sz_name} {f_size}A {f_interrupting_rating}kA"
        var labelParts: [String] = []
        if let v = matchedFrame?.f_voltage { labelParts.append("\(Int(v.rounded()))V") }
        if let name = matchedFrame?.sz_name, !name.isEmpty { labelParts.append(name) }
        if let s = matchedFrame?.f_size { labelParts.append("\(Int(s.rounded()))A") }
        if let ka = matchedFrame?.f_interrupting_rating, ka > 0 {
            let kaStr = ka.truncatingRemainder(dividingBy: 1) == 0
                ? "\(Int(ka))"
                : String(format: "%.1f", ka)
            labelParts.append("\(kaStr)kA")
        }
        let frameLabel = labelParts.isEmpty ? nil : labelParts.joined(separator: " ")

        return Picks(
            frame: matchedFrame,
            frameLabel: frameLabel,
            sensor: matchedSensor,
            plugAmps: matchedPlug
        )
    }

    // MARK: - Settings array (mirrors web's buildSettingsArray)

    /// Flatten the user's segment picks into a [{name, value}] array
    /// shaped for the SKM UI-automation JSON. Only segments marked
    /// for export (``includedIds``) are emitted.
    static func buildSettings(
        tripUnitSegments: [SkmTuSegment],
        selections: [String: SegmentValue],
        includedIds: [Int]
    ) -> [EqpLibSelection.GroundFault.SettingEntry] {
        let includedSet = Set(includedIds)
        return tripUnitSegments
            .filter { includedSet.contains($0.id) }
            .map { seg in
                let name = seg.sz_name ?? seg.subtype
                let value = selections[String(seg.id)]?.value ?? ""
                return EqpLibSelection.GroundFault.SettingEntry(name: name, value: value)
            }
    }
}
