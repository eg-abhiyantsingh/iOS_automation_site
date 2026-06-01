//
//  SkmMatcher.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 4b — offline mirror of the backend ``/skm-library/match``
//  endpoint. Pure-SwiftData query so the engineering section's match
//  panel runs without a round-trip.
//
//  Four execution paths keyed by trip-type slug, matching the BE:
//
//    1. Relay        — manufacturer + trip_type only; free-text search
//                      is the meaningful filter. No amp filter.
//    2. Fuse         — manufacturer + trip_type + trip_unit.f_size ==
//                      trip_amps. Frame voltage-floored.
//    3. SST breaker  — manufacturer + trip_type + sensor.f_size ==
//                      sensor_amps AND plug_amps ∈ sensor.plugs.
//                      Frame voltage-floored, closest tier above wins.
//    4. Thermal-mag  — manufacturer + trip_type + trip_unit.f_size ==
//                      trip_amps. Frame voltage-floored.
//    5. MCP          — manufacturer + trip_type + frame.f_size ==
//                      frame_amps (ZP-2478). Magnetic-only; sensor /
//                      plug describe the adjustable range, not a
//                      discrete pin, so they don't narrow the list.
//
//  All paths apply ``Accept-Encoding: gzip``-equivalent voltage floor
//  (``frame.f_voltage IS NULL OR frame.f_voltage >= node.voltage``)
//  and de-duplicate to one card per device (per (device, trip_unit)
//  for thermal-mag, since multiple trip units per chassis = different
//  amp tiers).
//
//  Returns a ranked ``[SkmMatch]``. The free-text search axis filters
//  on ``sz_type`` / ``sz_description`` / ``sz_catalog`` case-insensitive
//  contains — same as the BE's ``ILIKE :search`` clause.
//
import Foundation
import SwiftData

// MARK: - Match result + inputs

/// One row in the matcher's result list. Mirrors the BE response
/// shape so the same card renderer + tap handler can be reused.
///
/// ZP-2420: extended with optional cable / busway / transformer axes
/// so the same struct + ``MatchResultsPanel`` can render rows from
/// the three additional library endpoints (``/skm-cable-library/match``
/// and ``/skm-transformer-library/match``). Protective-device callers
/// leave them nil; the cable / transformer remote matchers populate
/// them.
struct SkmMatch: Identifiable, Hashable {
    let id: String           // stable composite for ForEach
    let slug: String
    let trip_type_id: Int?
    let item_id: Int         // cdevice_oid
    let style_id: Int        // cdevice_oid (BE uses this name)
    let manufacturer: String
    let type: String?        // sz_type
    let style: String?       // sz_description
    let cont_current: Double?
    let frame_id: Int?
    let frame_desc: String?
    /// Chosen frame's f_voltage — used by the final sort to rank
    /// equal-voltage matches above over-rated ones.
    let frame_voltage: Double?
    let pole_count: Int?
    let use_sst: Bool
    let sensor_id: Int?
    let matched_sensor_value: Double?
    let matched_plug_value: Double?
    let matched_trip_amp: Double?
    let trip_unit_style_id: Int?

    // ── Cable / busway extensions (ZP-2420) ────────────────────────
    var matched_cable_size: String? = nil
    var matched_cable_size_unit: String? = nil
    var conductor_type: String? = nil
    var conductor_desc: String? = nil
    var duct_material: String? = nil
    var installation: String? = nil
    var insulation_class: String? = nil
    var insulation_type: String? = nil
    var voltage_rating: Double? = nil
    var cable_size_id: Int? = nil
    var conductor_description_id: Int? = nil
    var insulation_class_id: Int? = nil
    var insulation_type_id: Int? = nil
    var installation_id: Int? = nil
    var duct_material_id: Int? = nil

    // ── Transformer extensions (ZP-2420) ───────────────────────────
    var transformer_oid: Int? = nil
    var matched_kva: Double? = nil
    var kva_label: String? = nil
    var kva_entry_id: Int? = nil
    var percentage_r: Double? = nil
    var percentage_x: Double? = nil
    var phase: Int? = nil
    var str_type: String? = nil
    var str_type_symbol: String? = nil
}

/// Matcher return type — the matches that fit under ``limit`` plus
/// a ``truncated`` flag the panel header uses to render "50+" when
/// the unfiltered result set was larger than the cap.
struct SkmMatchResults {
    let matches: [SkmMatch]
    let truncated: Bool
}

/// ZP-2420 — cable + busway filter inputs. The same shape works for
/// both classes; ``is_busway`` flips which local table the matcher
/// queries.
struct SkmCableMatchInputs {
    var eqp_lib_type_id: Int
    var is_busway: Bool
    var conductor_material: String
    /// Cable: AWG / kcmil label (String, e.g. "225", "4/0").
    /// Busway: ampere rating as String (parsed back to Double inside
    /// the matcher so the same field carries either shape).
    var cable_size: String?
    var conductor_desc: String?
    var duct_material: String?
    var insulation_class: String?
    var insulation_type: String?
    var installation: String?
    var voltage: Double?
    var search: String = ""
    var limit: Int = 50
}

/// ZP-2420 — transformer filter inputs. Mirrors the FE readiness gate:
/// ``manufacturer_id`` is required; everything else is optional.
struct SkmTransformerMatchInputs {
    var eqp_lib_type_id: Int
    var manufacturer_id: Int
    var trip_type_id: Int?
    var kva: Double?
    var voltage: Double?
    var search: String = ""
    var limit: Int = 50
}

/// All filter axes the matcher honors. Built from the engineering
/// draft + selected NodeClass / NodeSubtype.
struct SkmMatchInputs {
    var eqp_lib_type_id: Int
    /// "circuit_breaker" / "fuse" / "relay" — selects the path.
    var type_name: String
    var eqp_lib_subtype_id: Int?
    var manufacturer_id: Int?
    var trip_type_id: Int?
    /// Whether the trip type implies an SST sensor+plug picker.
    var is_sst_trip: Bool
    /// ZP-2478: Motor Circuit Protector. Magnetic-only — matches on the
    /// frame amp tier, NOT on a sensor+plug join (see the ``is_mcp``
    /// branch in ``match`` for why). Mutually exclusive with
    /// ``is_sst_trip``.
    var is_mcp: Bool = false
    var pole_count: Int?
    var sensor_amps: Int?
    var plug_amps: Int?
    /// For non-SST breakers + fuses, the chassis amp tier.
    var trip_amps: Int?
    var frame_amps: Int?
    var voltage: Double?
    var search: String = ""
    var limit: Int = 50
}

// MARK: - Service

enum SkmMatcher {

    /// Run a match. Returns an empty array when prerequisites are
    /// missing — e.g. SST path without sensor + plug amps, thermal-mag
    /// path without trip_amps. Matches BE's "0 matches" early-return
    /// behavior so the engineering section UI is straightforward.
    static func match(
        inputs: SkmMatchInputs,
        in context: ModelContext
    ) -> SkmMatchResults {
        guard let mfrId = inputs.manufacturer_id else {
            return SkmMatchResults(matches: [], truncated: false)
        }

        // 1) First-pass device filter via predicate. Mirrors the
        // model encoded in ``eg_dev_lib_routing``: a subtype's
        // identity is expressed through the set of allowed trip
        // types it permits (MCCB → {static_trip, thermal_mag,
        // ground_fault}; ICCB → {static_trip}; …). The matcher
        // therefore narrows on ``trip_type_id`` rather than on
        // ``skm_devices.eqp_lib_subtype_id``, because SKM's per-
        // device subtype tag is coarse and inconsistent (e.g. every
        // EATON static-trip breaker is tagged ``mccb`` even when
        // the physical part is insulated-case). Trip type is the
        // authoritative axis once it's set.
        //
        // When trip_type is unset, the subtype tag is still useful as
        // a soft narrowing axis to keep the device pool relevant.
        let typeId = inputs.eqp_lib_type_id
        var devicePredicate = #Predicate<SkmDeviceHeader> {
            $0.manufacturer_id == mfrId && $0.eqp_lib_type_id == typeId
        }
        if let tripId = inputs.trip_type_id {
            // Trip type set → it alone narrows; subtype is implicit
            // through routing.
            devicePredicate = #Predicate<SkmDeviceHeader> {
                $0.manufacturer_id == mfrId
                    && $0.eqp_lib_type_id == typeId
                    && $0.trip_type_id == tripId
            }
        } else if let subId = inputs.eqp_lib_subtype_id {
            // No trip type yet → fall back to SKM's per-device
            // subtype tag for soft narrowing.
            devicePredicate = #Predicate<SkmDeviceHeader> {
                $0.manufacturer_id == mfrId
                    && $0.eqp_lib_type_id == typeId
                    && $0.eqp_lib_subtype_id == subId
            }
        }
        var candidates: [SkmDeviceHeader]
        do {
            candidates = try context.fetch(
                FetchDescriptor<SkmDeviceHeader>(predicate: devicePredicate)
            )
        } catch {
            return SkmMatchResults(matches: [], truncated: false)
        }
        if candidates.isEmpty { return SkmMatchResults(matches: [], truncated: false) }

        // 2) Free-text search axis — sz_type / sz_description /
        // sz_catalog case-insensitive contains, same as the BE ILIKE.
        let search = inputs.search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if !search.isEmpty {
            candidates = candidates.filter { d in
                let t = d.sz_type?.lowercased() ?? ""
                let s = d.sz_description?.lowercased() ?? ""
                let c = d.sz_catalog?.lowercased() ?? ""
                return t.contains(search) || s.contains(search) || c.contains(search)
            }
            if candidates.isEmpty { return SkmMatchResults(matches: [], truncated: false) }
        }

        // 3) Resolve manufacturer name once.
        let mfrName: String = (try? context.fetch(
            FetchDescriptor<EnumSkmManufacturer>(
                predicate: #Predicate { $0.id == mfrId }
            )
        ).first?.name) ?? ""

        let isRelay = (inputs.type_name == "relay")
        let isFuse = (inputs.type_name == "fuse")

        // ZP-2161 4B-4 perf: when there's no amp-axis narrowing on
        // the matcher input, every surviving candidate becomes one
        // card. Truncate the candidate set to the output limit BEFORE
        // doing the heavy frame / sensor / trip-unit joins — for a
        // manufacturer with 918 static-trip MCCBs that's a >10x
        // reduction in joined rows we'd otherwise fetch + sort +
        // discard at the end.
        let hasAmpNarrowing =
            (isFuse && inputs.trip_amps != nil)
            || (inputs.is_sst_trip && inputs.sensor_amps != nil && inputs.plug_amps != nil)
            || (inputs.is_mcp && inputs.frame_amps != nil)
            || (!isRelay && !isFuse && !inputs.is_sst_trip && !inputs.is_mcp && inputs.trip_amps != nil)
        if !hasAmpNarrowing && candidates.count > inputs.limit {
            // ZP-2457: voltage-proximity pre-truncate. Pure
            // alphabetical-by-sz_type lets a manufacturer's earlier
            // letters (ABB → ADVAC, EMAX) fill the limit before
            // closer-fit devices (Formula at 600V class for a 480V
            // asset) get a chance. Mirror what the backend's
            // ``/skm-library/match`` does: rank by how close
            // ``f_max_voltage`` sits to the asset voltage (smaller
            // non-negative delta wins; devices rated below the asset
            // voltage rank last as unsafe). Alphabetical only as a
            // tiebreaker. When ``inputs.voltage`` is nil, fall back
            // to pure alphabetical to preserve prior behavior.
            let assetVoltage = inputs.voltage
            func voltageDelta(_ fv: Double?) -> Double {
                guard let fv else { return .infinity }
                guard let av = assetVoltage else { return 0 }
                return fv >= av ? fv - av : .infinity
            }
            candidates.sort { (a, b) in
                let da = voltageDelta(a.f_max_voltage)
                let db = voltageDelta(b.f_max_voltage)
                if da != db { return da < db }
                let at = a.sz_type ?? ""
                let bt = b.sz_type ?? ""
                if at != bt { return at < bt }
                return (a.sz_description ?? "") < (b.sz_description ?? "")
            }
            candidates = Array(candidates.prefix(inputs.limit))
        }

        let deviceOids = candidates.map { $0.id }

        // 4) Pre-fetch joined rows scoped to the candidate device set.
        let frames: [SkmFrame] = (try? context.fetch(
            FetchDescriptor<SkmFrame>(
                predicate: #Predicate { deviceOids.contains($0.device_oid) }
            )
        )) ?? []
        let tripUnits: [SkmTripUnit] = (try? context.fetch(
            FetchDescriptor<SkmTripUnit>(
                predicate: #Predicate { deviceOids.contains($0.device_oid) }
            )
        )) ?? []
        // Sensors are scoped by frame, not device — narrow via the
        // candidate frame ids.
        let frameIds = frames.map { $0.id }
        let sensors: [SkmSensor] = frameIds.isEmpty ? [] : ((try? context.fetch(
            FetchDescriptor<SkmSensor>(
                predicate: #Predicate { frameIds.contains($0.frame_id) }
            )
        )) ?? [])

        // 5) Index by parent for fast in-memory join.
        let framesByDevice = Dictionary(grouping: frames, by: \.device_oid)
        let tripUnitsByDevice = Dictionary(grouping: tripUnits, by: \.device_oid)
        let sensorsByFrame = Dictionary(grouping: sensors, by: \.frame_id)

        // 6) Dispatch to the appropriate path.
        var matches: [SkmMatch] = []
        for d in candidates {
            let devFrames = framesByDevice[d.id] ?? []
            let devTripUnits = tripUnitsByDevice[d.id] ?? []

            // ZP-2161 Phase 4b: progressive matching — surface devices
            // as soon as the manufacturer is set, then narrow as the
            // user types amp values. When the relevant amp axis is
            // nil, fall through to a "device-only" card (closest
            // voltage frame, no sensor / trip-unit join).
            if isRelay {
                let frame = closestVoltageFrame(devFrames, voltage: inputs.voltage)
                let tu = devTripUnits.first
                matches.append(buildMatch(
                    device: d, mfrName: mfrName,
                    frame: frame, sensor: nil, tripUnit: tu,
                    useSst: false,
                    matchedPlug: nil, matchedTripAmp: nil,
                    subtypeName: nil
                ))
            } else if isFuse {
                if let tripAmpsInt = inputs.trip_amps {
                    let target = Double(tripAmpsInt)
                    guard let tu = devTripUnits.first(where: { $0.f_size == target })
                    else { continue }
                    let candidatesFrames = devFrames.filter {
                        ($0.f_size == nil || $0.f_size == tu.f_size)
                        && passesVoltage($0, voltage: inputs.voltage)
                    }
                    let frame = closestVoltageFrame(candidatesFrames, voltage: inputs.voltage)
                    matches.append(buildMatch(
                        device: d, mfrName: mfrName,
                        frame: frame, sensor: nil, tripUnit: tu,
                        useSst: false,
                        matchedPlug: nil, matchedTripAmp: tu.f_size,
                        subtypeName: nil
                    ))
                } else {
                    // No trip amps yet — show the device as a
                    // candidate so the user gets feedback before
                    // they've typed the rating. ``frame_amps`` may
                    // still narrow: if set, require a frame match.
                    var frameCandidates = devFrames.filter {
                        passesVoltage($0, voltage: inputs.voltage)
                    }
                    if let fa = inputs.frame_amps {
                        frameCandidates = frameCandidates.filter { $0.f_size == Double(fa) }
                        if frameCandidates.isEmpty { continue }
                    }
                    let frame = closestVoltageFrame(frameCandidates, voltage: inputs.voltage)
                    matches.append(buildMatch(
                        device: d, mfrName: mfrName,
                        frame: frame, sensor: nil, tripUnit: devTripUnits.first,
                        useSst: false,
                        matchedPlug: nil, matchedTripAmp: nil,
                        subtypeName: nil
                    ))
                }
            } else if inputs.is_mcp {
                // ZP-2478: Motor Circuit Protector (magnetic-only motor
                // protector). The web matches MCP on the FRAME amp tier,
                // NOT on a sensor+plug join. Two MA trip-range variants on
                // the same 250A frame (e.g. ABB XT4 "25-50A" and XT4
                // "80-250A") are BOTH valid 250A-frame matches even though
                // only one carries a sensor at the picked rating — the
                // ``skm_sensors`` rows describe the adjustable range, not a
                // discrete (sensor, plug) to pin. So mirror the web: one
                // card per device whose frame ``f_size == frame_amps``
                // (voltage-floored), ignoring sensor_amps / plug_amps /
                // trip_amps for narrowing. (The engineering UI still
                // collects Sensor / Plug because MCP is ``has_trip_unit``,
                // exactly as the web form does — they just don't filter the
                // match list.)
                var frameCandidates = devFrames.filter {
                    passesVoltage($0, voltage: inputs.voltage)
                }
                if let fa = inputs.frame_amps {
                    frameCandidates = frameCandidates.filter { $0.f_size == Double(fa) }
                    if frameCandidates.isEmpty { continue }
                }
                let frame = closestVoltageFrame(frameCandidates, voltage: inputs.voltage)
                matches.append(buildMatch(
                    device: d, mfrName: mfrName,
                    frame: frame, sensor: nil, tripUnit: devTripUnits.first,
                    useSst: false,
                    matchedPlug: nil, matchedTripAmp: nil,
                    subtypeName: nil
                ))
            } else if inputs.is_sst_trip {
                if let sa = inputs.sensor_amps, let pa = inputs.plug_amps {
                    // Full SST join — narrow to (frame, sensor) where
                    // sensor.f_size matches and plug ∈ sensor.plugs.
                    let saD = Double(sa)
                    let paD = Double(pa)
                    var bestFrame: SkmFrame? = nil
                    var bestSensor: SkmSensor? = nil
                    let sortedFrames = devFrames
                        .filter { passesVoltage($0, voltage: inputs.voltage) }
                        .sorted { (a, b) in
                            switch (a.f_voltage, b.f_voltage) {
                            case (nil, nil): return false
                            case (nil, _):   return false
                            case (_, nil):   return true
                            case (let av?, let bv?): return av < bv
                            }
                        }
                    for f in sortedFrames {
                        if let fa = inputs.frame_amps, f.f_size != Double(fa) { continue }
                        let frameSensors = sensorsByFrame[f.id] ?? []
                        if let s = frameSensors.first(where: { $0.f_size == saD && $0.plugs.contains(paD) }) {
                            bestFrame = f
                            bestSensor = s
                            break
                        }
                    }
                    guard let f = bestFrame, let s = bestSensor else { continue }
                    let tu = devTripUnits.first
                    matches.append(buildMatch(
                        device: d, mfrName: mfrName,
                        frame: f, sensor: s, tripUnit: tu,
                        useSst: true,
                        matchedPlug: paD, matchedTripAmp: nil,
                        subtypeName: nil
                    ))
                } else {
                    // Partial SST input — narrow on whichever axes
                    // the user has filled in. When sensor_amps is
                    // set but plug_amps isn't (or vice versa), drop
                    // devices that don't have a sensor satisfying
                    // the partial filter. Frame_amps similarly drops
                    // devices when no frame matches.
                    var frameCandidates = devFrames.filter {
                        passesVoltage($0, voltage: inputs.voltage)
                    }
                    if let fa = inputs.frame_amps {
                        frameCandidates = frameCandidates.filter { $0.f_size == Double(fa) }
                    }
                    let saD = inputs.sensor_amps.map(Double.init)
                    let paD = inputs.plug_amps.map(Double.init)
                    if saD != nil || paD != nil {
                        frameCandidates = frameCandidates.filter { f in
                            let frameSensors = sensorsByFrame[f.id] ?? []
                            return frameSensors.contains { s in
                                if let saD, s.f_size != saD { return false }
                                if let paD, !s.plugs.contains(paD) { return false }
                                return true
                            }
                        }
                    }
                    if frameCandidates.isEmpty {
                        // No frame on this device satisfies the
                        // partial filters — drop the card entirely.
                        continue
                    }
                    let frame = closestVoltageFrame(frameCandidates, voltage: inputs.voltage)
                    matches.append(buildMatch(
                        device: d, mfrName: mfrName,
                        frame: frame, sensor: nil, tripUnit: devTripUnits.first,
                        useSst: true,
                        matchedPlug: nil, matchedTripAmp: nil,
                        subtypeName: nil
                    ))
                }
            } else {
                // Thermal-mag chassis: with trip_amps → one match per
                // (device, trip_unit). Without → one device card with
                // closest-voltage frame.
                if let tripAmpsInt = inputs.trip_amps {
                    let target = Double(tripAmpsInt)
                    for tu in devTripUnits where tu.f_size == target {
                        var frameCandidates = devFrames.filter {
                            passesVoltage($0, voltage: inputs.voltage)
                        }
                        if let fa = inputs.frame_amps {
                            frameCandidates = frameCandidates.filter { $0.f_size == Double(fa) }
                            // Drop the (device, trip_unit) row when
                            // frame_amps is set and no frame matches.
                            if frameCandidates.isEmpty { continue }
                        }
                        let frame = closestVoltageFrame(frameCandidates, voltage: inputs.voltage)
                        matches.append(buildMatch(
                            device: d, mfrName: mfrName,
                            frame: frame, sensor: nil, tripUnit: tu,
                            useSst: false,
                            matchedPlug: nil, matchedTripAmp: tu.f_size,
                            subtypeName: nil
                        ))
                    }
                } else {
                    var frameCandidates = devFrames.filter {
                        passesVoltage($0, voltage: inputs.voltage)
                    }
                    if let fa = inputs.frame_amps {
                        frameCandidates = frameCandidates.filter { $0.f_size == Double(fa) }
                        if frameCandidates.isEmpty { continue }
                    }
                    let frame = closestVoltageFrame(frameCandidates, voltage: inputs.voltage)
                    matches.append(buildMatch(
                        device: d, mfrName: mfrName,
                        frame: frame, sensor: nil, tripUnit: nil,
                        useSst: false,
                        matchedPlug: nil, matchedTripAmp: nil,
                        subtypeName: nil
                    ))
                }
            }
        }

        // 7) Sort by:
        //    1. Voltage distance from the asset voltage (smaller =
        //       closer fit; nil voltage ranks last).
        //    2. Manufacturer / type / style for stable in-group order.
        // Truncate to ``limit``.
        //
        // ZP-2161: ranking by voltage distance puts exact-fit frames
        // (e.g. 480V frame on a 480V asset) above over-rated ones
        // (e.g. 600V frame on a 480V asset). Both are "safe" per the
        // voltage-floor filter; the closer fit is just a better match.
        let assetVoltage = inputs.voltage
        func voltageRank(_ m: SkmMatch) -> Double {
            guard let fv = m.frame_voltage else { return .infinity }
            guard let av = assetVoltage else { return 0 }
            return fv - av  // non-negative since passesVoltage enforced fv >= av
        }
        matches.sort { (a, b) in
            let ra = voltageRank(a)
            let rb = voltageRank(b)
            if ra != rb { return ra < rb }
            if a.manufacturer != b.manufacturer { return a.manufacturer < b.manufacturer }
            let at = a.type ?? ""
            let bt = b.type ?? ""
            if at != bt { return at < bt }
            return (a.style ?? "") < (b.style ?? "")
        }
        let truncated = matches.count > inputs.limit
        if truncated {
            matches = Array(matches.prefix(inputs.limit))
        }
        return SkmMatchResults(matches: matches, truncated: truncated)
    }

    // MARK: - Helpers

    private static func passesVoltage(_ frame: SkmFrame, voltage: Double?) -> Bool {
        guard let v = voltage else { return true }
        guard let fv = frame.f_voltage else { return true }
        return fv >= v
    }

    /// Pick the lowest ``f_voltage`` that still clears the system
    /// voltage (closest tier above). Frames with nil f_voltage are
    /// considered last but accepted — some devices don't encode a
    /// voltage tier.
    private static func closestVoltageFrame(_ frames: [SkmFrame], voltage: Double?) -> SkmFrame? {
        let viable = frames.filter { passesVoltage($0, voltage: voltage) }
        if viable.isEmpty { return nil }
        return viable.sorted { (a, b) in
            switch (a.f_voltage, b.f_voltage) {
            case (nil, nil): return false
            case (nil, _):   return false
            case (_, nil):   return true
            case (let av?, let bv?): return av < bv
            }
        }.first
    }

    private static func buildMatch(
        device d: SkmDeviceHeader,
        mfrName: String,
        frame: SkmFrame?,
        sensor: SkmSensor?,
        tripUnit: SkmTripUnit?,
        useSst: Bool,
        matchedPlug: Double?,
        matchedTripAmp: Double?,
        subtypeName: String?
    ) -> SkmMatch {
        let id = "\(d.id)-\(frame?.id ?? sensor?.id ?? -1)"
        // BE parity: n_poles is 0 for devices that don't carry the
        // axis (fuses, relays). Coalesce to nil so the card hides
        // the "0P" badge.
        let poles: Int? = (d.n_poles ?? 0) > 0 ? d.n_poles : nil
        // Match cards show sz_type as the primary label (model
        // family — "Formula A1") and sz_description as the secondary
        // (size + pole context — "15-100A, 1 Pole"). No fallback to
        // sz_name — that's an internal SKM concatenated identifier
        // that doesn't read well in the UI.
        return SkmMatch(
            id: id,
            slug: device_slug(devType: d.sz_dev_type, subtypeName: subtypeName),
            trip_type_id: d.trip_type_id,
            item_id: d.id,
            style_id: d.id,
            manufacturer: mfrName,
            type: d.sz_type,
            style: d.sz_description,
            cont_current: frame?.f_size,
            frame_id: frame?.id,
            frame_desc: frame?.sz_name,
            frame_voltage: frame?.f_voltage,
            pole_count: poles,
            use_sst: useSst,
            sensor_id: sensor?.id,
            matched_sensor_value: sensor?.f_size,
            matched_plug_value: matchedPlug,
            matched_trip_amp: matchedTripAmp,
            trip_unit_style_id: useSst ? tripUnit?.id : nil
        )
    }

    /// Map ``sz_dev_type`` to the eqp_lib category slug used downstream.
    /// Mirrors BE's ``_slug_for_breaker`` / ``_FUSE_SLUG_BY_DEV_TYPE``
    /// / ``_RELAY_SLUG_BY_DEV_TYPE``. We don't need to be perfect — the
    /// slug is just metadata on the resulting eqp_lib JSON.
    private static func device_slug(devType: String?, subtypeName: String?) -> String {
        guard let dt = devType else { return "mccb-breakers" }
        switch dt {
        case "Low Voltage Fuse": return "lv-fuses"
        case "High Voltage Fuse": return "hv-fuses"
        case "Electro-Mechanical Relay": return "relays-electromechanical"
        case "Electronic Relay": return "relays-electronic"
        case "IEC Type Relay": return "relays-iec"
        case "Static Trip Breaker": return "iccb-breakers"
        case "Power Circuit Breaker": return "pcb-breakers"
        case "Thermal Magnetic Molded Case Breaker": return "mccb-breakers"
        case "HVMV with Trip-Unit", "HVMV without Trip-Unit": return "hv-breakers"
        default: return "mccb-breakers"
        }
    }

    // MARK: - Cable + busway match (ZP-2420)

    /// Local matcher for cable + busway. Pulls deep rows from
    /// ``SkmCableEntry`` (synced via Settings → SKM Library Download).
    /// The same table holds both kinds, partitioned by ``duct_material``
    /// ∈ {"Busway", "Bus"} for busway distribution vs everything else
    /// for cable. Joins to the parent header for the manufacturer label,
    /// resolves filter-string values to their enum FK IDs so the on-pick
    /// handler can write first-class columns. Same shape as the device
    /// matcher — sync, takes a ``ModelContext``, returns ``SkmMatchResults``.
    static func matchCableOrBusway(
        inputs: SkmCableMatchInputs,
        in context: ModelContext
    ) -> SkmMatchResults {
        let material = inputs.conductor_material.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !material.isEmpty else {
            return SkmMatchResults(matches: [], truncated: false)
        }
        let materialLower = material.lowercased()
        let search = inputs.search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let voltage = inputs.voltage

        // Pre-fetch enum tables once so we can resolve label-string
        // values to their FK ids in O(1) per match. The matcher
        // returns enum FK IDs (not strings) so the on-pick handler
        // can write first-class draft columns directly.
        let conductorDescs = (try? context.fetch(FetchDescriptor<EnumCableConductorDescription>())) ?? []
        let ductMaterials = (try? context.fetch(FetchDescriptor<EnumCableDuctMaterial>())) ?? []
        let insulationClasses = (try? context.fetch(FetchDescriptor<EnumCableInsulationClass>())) ?? []
        let insulationTypes = (try? context.fetch(FetchDescriptor<EnumCableInsulationType>())) ?? []
        let installations = (try? context.fetch(FetchDescriptor<EnumCableInstallation>())) ?? []
        let cableSizes = (try? context.fetch(FetchDescriptor<EnumCableSize>())) ?? []
        let manufacturers = (try? context.fetch(FetchDescriptor<EnumSkmManufacturer>())) ?? []
        let manufacturersById = Dictionary(uniqueKeysWithValues: manufacturers.map { ($0.id, $0.name) })

        // Index enum tables by name (case-insensitive) so we can
        // resolve cable_entry.duct_material ("Busway") → enum row id
        // quickly. ``is_busway`` partitions the cable/busway-side
        // enum rows from each other.
        func idByName<T>(_ rows: [T], name keyPath: KeyPath<T, String>, isBuswayKey: KeyPath<T, Bool>, busway: Bool, idKey: KeyPath<T, Int>) -> [String: Int] {
            var dict: [String: Int] = [:]
            for row in rows where row[keyPath: isBuswayKey] == busway {
                dict[row[keyPath: keyPath].lowercased()] = row[keyPath: idKey]
            }
            return dict
        }
        let conductorDescByName = idByName(conductorDescs, name: \.name, isBuswayKey: \.is_busway, busway: inputs.is_busway, idKey: \.id)
        let ductMaterialByName = idByName(ductMaterials, name: \.name, isBuswayKey: \.is_busway, busway: inputs.is_busway, idKey: \.id)
        let insulationClassByName = idByName(insulationClasses, name: \.name, isBuswayKey: \.is_busway, busway: inputs.is_busway, idKey: \.id)
        let insulationTypeByName = idByName(insulationTypes, name: \.name, isBuswayKey: \.is_busway, busway: inputs.is_busway, idKey: \.id)
        let installationByName = idByName(installations, name: \.name, isBuswayKey: \.is_busway, busway: inputs.is_busway, idKey: \.id)
        // Cable sizes aren't partitioned by is_busway in the iOS
        // schema, so look up by raw name across the whole table.
        let cableSizeByName: [String: Int] = Dictionary(uniqueKeysWithValues:
            cableSizes.map { ($0.name.lowercased(), $0.id) })

        // ZP-2420: cable AND busway distribution rows both live in
        // ``SkmCableEntry``; only the ``duct_material`` column
        // partitions them. The BE also serves a ``busway_entries``
        // section on /skm-tree which we don't sync — it's MV switchgear
        // data (voltage ratings up to 72kV, sz_name like "MV Switchgear"),
        // unrelated to asset-form busway distribution matching. Mirrors
        // the BE matcher's ``is_busway`` flag semantics: same underlying
        // table, opposite ``duct_material`` partitions.
        return matchCableEntries(
            inputs: inputs,
            materialLower: materialLower,
            search: search,
            voltage: voltage,
            in: context,
            manufacturersById: manufacturersById,
            conductorDescByName: conductorDescByName,
            ductMaterialByName: ductMaterialByName,
            insulationClassByName: insulationClassByName,
            insulationTypeByName: insulationTypeByName,
            installationByName: installationByName,
            cableSizeByName: cableSizeByName
        )
    }

    /// Duct material values that classify a row as busway (vs cable).
    /// Empirically derived from the BE response — busway rows carry
    /// either ``"Busway"`` or ``"Bus"`` here. Cable rows carry magnetic /
    /// non-magnetic / placeholder values. The matcher uses this set to
    /// partition the shared ``SkmCableEntry`` table per ``is_busway``.
    private static let buswayDuctMaterialValues: Set<String> = ["busway", "bus"]

    private static func matchCableEntries(
        inputs: SkmCableMatchInputs,
        materialLower: String,
        search: String,
        voltage: Double?,
        in context: ModelContext,
        manufacturersById: [Int: String],
        conductorDescByName: [String: Int],
        ductMaterialByName: [String: Int],
        insulationClassByName: [String: Int],
        insulationTypeByName: [String: Int],
        installationByName: [String: Int],
        cableSizeByName: [String: Int]
    ) -> SkmMatchResults {
        let isBusway = inputs.is_busway
        // ZP-2420 perf (PR review #1): push the conductor_type filter
        // down to SwiftData via #Predicate so the SQL layer narrows the
        // result set (Copper / Aluminum → ~half of the 15.7k cable rows)
        // BEFORE Swift-side filtering. Without this, every keystroke
        // walked the full table. The matcher input is already exactly
        // ``"Copper"`` or ``"Aluminum"`` from ``ConductorMaterialPicker``,
        // and the BE data uses the same casing — so a case-exact
        // predicate is safe and avoids the full-table scan.
        let materialExact = inputs.conductor_material
        let descriptor = FetchDescriptor<SkmCableEntry>(
            predicate: #Predicate { e in
                e.conductor_type == materialExact
            }
        )
        var entries = (try? context.fetch(descriptor)) ?? []
        entries = entries.filter { e in
            // Belt-and-suspenders lowercase recheck — handles any future
            // data drift (e.g. BE returning "COPPER") without a crash.
            // The SQL predicate already did the heavy lift; this is O(1)
            // per surviving row.
            guard (e.conductor_type ?? "").lowercased() == materialLower else { return false }
            // Partition: busway rows have duct_material ∈ {Busway, Bus};
            // cable rows have everything else (including nil).
            let dmLower = (e.duct_material ?? "").lowercased()
            let rowIsBusway = Self.buswayDuctMaterialValues.contains(dmLower)
            guard rowIsBusway == isBusway else { return false }
            // Optional: cable_size (string equality, case-insensitive)
            if let size = inputs.cable_size, !size.isEmpty {
                guard (e.cable_size ?? "").lowercased() == size.lowercased() else { return false }
            }
            // Optional: duct_material — applies only to cable (busway
            // is already partitioned above). Skip when the caller is
            // matching busway since the column is the partition key.
            if !isBusway, let dm = inputs.duct_material, !dm.isEmpty {
                guard dmLower == dm.lowercased() else { return false }
            }
            if let ic = inputs.insulation_class, !ic.isEmpty {
                guard (e.insulation_class ?? "").lowercased() == ic.lowercased() else { return false }
            }
            if let it = inputs.insulation_type, !it.isEmpty {
                guard (e.insulation_type ?? "").lowercased() == it.lowercased() else { return false }
            }
            if let inst = inputs.installation, !inst.isEmpty {
                guard (e.installation ?? "").lowercased() == inst.lowercased() else { return false }
            }
            // Voltage floor — entry must rate at or above the asset voltage.
            // PR review #3: when an asset voltage is set, EXCLUDE rows
            // whose voltage_rating is nil. Surfacing an unknown-rated
            // cable as a match at any voltage is a correctness risk for
            // a safety-engineering app; better to hide them than to
            // mis-suggest. When the asset has no voltage at all, the
            // filter is skipped entirely (matches any row).
            if let v = voltage {
                guard let ev = e.voltage_rating, ev >= v else { return false }
            }
            // Free-text search — sz_name contains.
            if !search.isEmpty {
                guard (e.sz_name ?? "").lowercased().contains(search) else { return false }
            }
            return true
        }

        // Resolve manufacturer per cable_oid — try AC, then DC, then
        // IEEE-W headers (the deep entry has no direct mfr column).
        // Array (not Set) for the #Predicate capture — matches the
        // shape SwiftData's macro lowers to a SQL ``IN`` clause and
        // is the pattern the existing protective-device matcher uses.
        let cableOidArr = Array(Set(entries.map(\.cable_oid)))
        let acHeaders = (try? context.fetch(FetchDescriptor<SkmCableAcHeader>(
            predicate: #Predicate { cableOidArr.contains($0.id) }
        ))) ?? []
        let dcHeaders = (try? context.fetch(FetchDescriptor<SkmCableDcHeader>(
            predicate: #Predicate { cableOidArr.contains($0.id) }
        ))) ?? []
        let ieeeHeaders = (try? context.fetch(FetchDescriptor<SkmCableIeeeWHeader>(
            predicate: #Predicate { cableOidArr.contains($0.id) }
        ))) ?? []
        var mfrIdByCableOid: [Int: Int] = [:]
        for h in acHeaders { if let m = h.manufacturer_id { mfrIdByCableOid[h.id] = m } }
        for h in dcHeaders { if let m = h.manufacturer_id, mfrIdByCableOid[h.id] == nil { mfrIdByCableOid[h.id] = m } }
        for h in ieeeHeaders { if let m = h.manufacturer_id, mfrIdByCableOid[h.id] == nil { mfrIdByCableOid[h.id] = m } }

        var matches: [SkmMatch] = entries.map { e in
            let mfrId = mfrIdByCableOid[e.cable_oid]
            let mfrName = mfrId.flatMap { manufacturersById[$0] } ?? ""
            // Slug + unit follow the partition: busway rows emit
            // slug="busway-skm" + unit="A", cable rows emit
            // slug="cables-skm" + no unit (raw size label is the
            // user-facing string).
            let slug = isBusway ? "busway-skm" : "cables-skm"
            let unit: String? = isBusway ? "A" : nil
            // Busway picks should NOT seed cable_size_id — the
            // busway block uses busway_ampere_rating (Int parsed
            // from matched_cable_size) instead.
            let cableSizeIdValue: Int? = isBusway
                ? nil
                : e.cable_size.flatMap { cableSizeByName[$0.lowercased()] }
            return SkmMatch(
                id: "\(slug)-\(e.id)",
                slug: slug,
                trip_type_id: nil,
                item_id: e.cable_oid,
                style_id: e.cable_oid,
                manufacturer: mfrName,
                type: e.sz_name,
                style: nil,
                cont_current: nil,
                frame_id: nil,
                frame_desc: nil,
                frame_voltage: nil,
                pole_count: nil,
                use_sst: false,
                sensor_id: nil,
                matched_sensor_value: nil,
                matched_plug_value: nil,
                matched_trip_amp: nil,
                trip_unit_style_id: nil,
                matched_cable_size: e.cable_size,
                matched_cable_size_unit: unit,
                conductor_type: e.conductor_type,
                conductor_desc: nil,
                duct_material: e.duct_material,
                installation: e.installation,
                insulation_class: e.insulation_class,
                insulation_type: e.insulation_type,
                voltage_rating: e.voltage_rating,
                cable_size_id: cableSizeIdValue,
                // PR review #7: ``conductor_description_id`` is
                // intentionally nil. ``SkmCableEntry`` has no
                // ``conductor_desc`` column (only ``conductor_type``,
                // which captures Copper/Aluminum). The user keeps the
                // conductor-description picker editable — we just
                // don't pre-seed it. If the BE adds the column later,
                // resolve it here via the same name→FK lookup used
                // for insulation_class etc.
                conductor_description_id: nil,
                insulation_class_id: e.insulation_class.flatMap { insulationClassByName[$0.lowercased()] },
                insulation_type_id: e.insulation_type.flatMap { insulationTypeByName[$0.lowercased()] },
                installation_id: e.installation.flatMap { installationByName[$0.lowercased()] },
                duct_material_id: e.duct_material.flatMap { ductMaterialByName[$0.lowercased()] }
            )
        }

        // Sort by closest voltage tier, then manufacturer.
        matches.sort { (a, b) in
            let av = a.voltage_rating ?? .infinity
            let bv = b.voltage_rating ?? .infinity
            if av != bv { return av < bv }
            return a.manufacturer < b.manufacturer
        }
        let truncated = matches.count > inputs.limit
        if truncated { matches = Array(matches.prefix(inputs.limit)) }
        return SkmMatchResults(matches: matches, truncated: truncated)
    }

    // MARK: - Transformer match (ZP-2420)

    /// Local matcher for transformer library rows. Joins
    /// ``SkmTransformerModelHeader`` (already synced via the headers
    /// bundle) with ``SkmTransformerKvaEntry`` (synced via the deep
    /// tree). Emits one match per (transformer header, kVA entry)
    /// tuple so the user picks both the family and the specific kVA
    /// tier in a single tap.
    /// ZP-2514 — mirror of the BE filter dict in
    /// ``app/routes/skm_transformer_library_routes.py:_TRIP_TYPE_SYMBOL_FILTERS``.
    /// SYNC WITH BE: any change to that dict must land here too.
    private enum TransformerTripTypeFilter {
        case symbolIn([String])
        case strTypeLike(String)  // case-insensitive substring (SQL ILIKE '%needle%')
    }
    private static let transformerTripTypeFilters: [String: TransformerTripTypeFilter] = [
        "xfmr_dry":  .symbolIn(["DT", "DRY", "CU"]),
        "xfmr_oil":  .symbolIn(["OA", "OAFA", "ONAN", "ONAF", "FA"]),
        "xfmr_pad":  .symbolIn(["PDMT"]),
        "xfmr_pole": .strTypeLike("pole"),
    ]

    static func matchTransformer(
        inputs: SkmTransformerMatchInputs,
        in context: ModelContext
    ) -> SkmMatchResults {
        let mfrId = inputs.manufacturer_id
        // Note: ``inputs.eqp_lib_type_id`` isn't used here — transformer
        // headers don't carry eqp_lib_type_id locally, so manufacturer
        // is the authoritative narrowing axis.

        var headers = (try? context.fetch(
            FetchDescriptor<SkmTransformerModelHeader>(
                predicate: #Predicate { $0.manufacturer_id == mfrId }
            )
        )) ?? []
        if headers.isEmpty {
            return SkmMatchResults(matches: [], truncated: false)
        }

        // ZP-2514: trip_type narrows by str_type_symbol family — look up
        // the slug, apply the BE-mirrored predicate. No trip_type → no
        // narrowing (manufacturer alone, as before).
        if let tripId = inputs.trip_type_id,
           let trip = (try? context.fetch(
               FetchDescriptor<EnumNodeTripType>(
                   predicate: #Predicate { $0.id == tripId }
               )
           ))?.first,
           let filter = transformerTripTypeFilters[trip.slug] {
            switch filter {
            case .symbolIn(let symbols):
                let allowed = Set(symbols)
                headers = headers.filter { h in
                    guard let sym = h.str_type_symbol else { return false }
                    return allowed.contains(sym)
                }
            case .strTypeLike(let needle):
                let needleLower = needle.lowercased()
                headers = headers.filter { h in
                    (h.str_type ?? "").lowercased().contains(needleLower)
                }
            }
            if headers.isEmpty {
                return SkmMatchResults(matches: [], truncated: false)
            }
        }

        let search = inputs.search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if !search.isEmpty {
            headers = headers.filter { h in
                let t = (h.str_type ?? "").lowercased()
                let d = (h.str_description ?? "").lowercased()
                let n = (h.sz_name ?? "").lowercased()
                return t.contains(search) || d.contains(search) || n.contains(search)
            }
            if headers.isEmpty { return SkmMatchResults(matches: [], truncated: false) }
        }

        // Pre-fetch kVA entries for the candidate transformers.
        // Array (not Set) for #Predicate capture — see comment in the
        // cable matcher above.
        let xfmrOidArr = Array(Set(headers.map(\.id)))
        let allKvas = (try? context.fetch(
            FetchDescriptor<SkmTransformerKvaEntry>(
                predicate: #Predicate { xfmrOidArr.contains($0.transformer_oid) }
            )
        )) ?? []
        let kvasByOid = Dictionary(grouping: allKvas, by: \.transformer_oid)

        let mfrName = (try? context.fetch(
            FetchDescriptor<EnumSkmManufacturer>(
                predicate: #Predicate { $0.id == mfrId }
            )
        ).first?.name) ?? ""

        var matches: [SkmMatch] = []
        let targetKva = inputs.kva
        for h in headers {
            let allEntries = (kvasByOid[h.id] ?? [])
                .sorted { ($0.nominal_kva ?? 0) < ($1.nominal_kva ?? 0) }

            // Headers with no kVA entries at all still produce a card (so
            // the user sees the family even before the deep tree download).
            // There's nothing to narrow on, so the kVA filter (if any) can't
            // apply here. Use a placeholder match.
            if allEntries.isEmpty {
                matches.append(SkmMatch(
                    id: "transformers-skm-\(h.id)",
                    slug: "transformers-skm",
                    trip_type_id: inputs.trip_type_id,
                    item_id: h.id,
                    style_id: h.id,
                    manufacturer: mfrName,
                    type: h.str_type,
                    style: h.str_description,
                    cont_current: nil,
                    frame_id: nil, frame_desc: nil, frame_voltage: nil,
                    pole_count: nil, use_sst: false,
                    sensor_id: nil,
                    matched_sensor_value: nil, matched_plug_value: nil,
                    matched_trip_amp: nil, trip_unit_style_id: nil,
                    transformer_oid: h.id,
                    matched_kva: nil,
                    kva_label: nil,
                    kva_entry_id: nil,
                    percentage_r: nil,
                    percentage_x: nil,
                    phase: h.n_phase,
                    str_type: h.str_type,
                    str_type_symbol: h.str_type_symbol
                ))
                continue
            }

            // ZP-2420 fix: parity with the web matcher — one card per
            // transformer family, not per (family × kVA tier). The kVA tier
            // picker lives inside the config card after the user picks a
            // family, so the matcher just needs to surface families.
            let kvas: [SkmTransformerKvaEntry]
            if let target = targetKva {
                // ZP-2539: when the user has typed a kVA, narrow to entries at
                // that rating. A family with no entry at the requested kVA is
                // dropped entirely so the surfaced results reflect the KVA
                // Rating filter — this matches the web matcher, whose BE
                // `/skm-transformer-library/match` excludes non-matching
                // families rather than surfacing them at a different kVA.
                //
                // Previously this fell back to the family's smallest tier when
                // nothing matched, which displayed a card whose kVA did not
                // match the user's filter (the reported bug).
                kvas = allEntries.filter { entry in
                    guard let nk = entry.nominal_kva else { return false }
                    // PR review #5: epsilon-compare Doubles. Different
                    // conversion paths (string-parsed BE values vs
                    // user-typed Doubles in the draft) can produce
                    // tiny float deltas where a strict == would silently
                    // drop the only valid row.
                    return abs(nk - target) < 0.001
                }
            } else {
                // No kVA narrowing → collapse to one representative (smallest)
                // tier so each family gets exactly one card.
                kvas = [allEntries[0]]
            }

            for entry in kvas {
                let kva = entry.nominal_kva
                let label: String? = kva.map { v in
                    v.truncatingRemainder(dividingBy: 1) == 0
                        ? "\(Int(v)) kVA"
                        : String(format: "%.1f kVA", v)
                }
                matches.append(SkmMatch(
                    id: "transformers-skm-\(h.id)-\(entry.id)",
                    slug: "transformers-skm",
                    trip_type_id: inputs.trip_type_id,
                    item_id: h.id,
                    style_id: h.id,
                    manufacturer: mfrName,
                    type: h.str_type,
                    style: h.str_description,
                    cont_current: nil,
                    frame_id: nil, frame_desc: nil, frame_voltage: nil,
                    pole_count: nil, use_sst: false,
                    sensor_id: nil,
                    matched_sensor_value: nil, matched_plug_value: nil,
                    matched_trip_amp: nil, trip_unit_style_id: nil,
                    transformer_oid: h.id,
                    matched_kva: kva,
                    kva_label: label,
                    kva_entry_id: entry.id,
                    percentage_r: entry.percentage_r,
                    percentage_x: entry.percentage_x,
                    phase: h.n_phase,
                    str_type: h.str_type,
                    str_type_symbol: h.str_type_symbol
                ))
            }
        }

        // ZP-2514 — sort by str_type (web parity: alphabetical by
        // family). kVA only breaks ties when a kVA was pinned and we
        // emitted multiple cards per family. localizedStandardCompare
        // gives natural ordering so "T2F" sorts before "T145HDIT".
        matches.sort { (a, b) in
            let at = a.type ?? ""
            let bt = b.type ?? ""
            let cmp = at.localizedStandardCompare(bt)
            if cmp != .orderedSame { return cmp == .orderedAscending }
            let ak = a.matched_kva ?? .infinity
            let bk = b.matched_kva ?? .infinity
            return ak < bk
        }
        let truncated = matches.count > inputs.limit
        if truncated { matches = Array(matches.prefix(inputs.limit)) }
        return SkmMatchResults(matches: matches, truncated: truncated)
    }
}
