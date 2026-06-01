//
//  GroundFaultPickerSheet.swift
//  Egalvanic PZ
//
//  ZP-2243: Manual Ground Fault library picker — mobile mirror of
//  web's ``GroundFaultPickerDialog``. Opens from the "Add anyway"
//  button in SkmTripConfigCard when auto-pairing fails to resolve a
//  GF sibling for the user's chosen Static Trip / Power Circuit /
//  HVMV-with-trip device, or from "Change…" when the user wants to
//  override the auto-pair.
//
//  Lets the user search the local SKM library (synced into SwiftData
//  via ``SkmLibrarySyncService``) for the right Ground Fault cdevice.
//  Search axes mirror the matcher's main filters: catalog text, type
//  contains, sensor / plug / frame amps, voltage. Free-text search
//  also looks across type / description / catalog.
//
//  Selection: tapping a row calls ``onPick`` with the matched
//  ``SkmDeviceHeader`` and dismisses the sheet. The configurator
//  then binds that header as the manual GF sibling, replacing the
//  auto-pair (or supplying one where none was found).
//
import SwiftUI
import SwiftData

struct GroundFaultPickerSheet: View {
    let manufacturerId: Int?
    let manufacturerName: String?
    var defaultSzType: String?
    var defaultSensorAmps: Double?
    var defaultPlugAmps: Double?
    var defaultFrameAmps: Double?
    var defaultVoltage: Double?
    var onPick: (SkmDeviceHeader) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    @State private var search: String = ""
    @State private var catalog: String = ""
    @State private var szType: String = ""
    @State private var sensorAmps: String = ""
    @State private var plugAmps: String = ""
    @State private var frameAmps: String = ""
    @State private var voltage: String = ""

    @State private var results: [GfMatch] = []
    @State private var truncated: Bool = false

    /// Ranked GF picker result — a header row joined to its matched
    /// frame/sensor/plug context for richer list labels.
    private struct GfMatch: Identifiable {
        let id: Int
        let header: SkmDeviceHeader
        let frameSize: Double?
        let sensorSize: Double?
        let frameVoltage: Double?

        var label: String {
            let mfr = header.sz_type ?? "—"
            let style = header.sz_description ?? "—"
            var tail: [String] = []
            if let f = frameSize { tail.append("\(Int(f.rounded()))AF") }
            if let s = sensorSize { tail.append("\(Int(s.rounded()))AS") }
            if let v = frameVoltage { tail.append("\(Int(v.rounded()))V") }
            if let c = header.sz_catalog, !c.isEmpty { tail.append(c) }
            let suffix = tail.isEmpty ? "" : " — \(tail.joined(separator: " / "))"
            return "\(mfr) — \(style)\(suffix)"
        }
    }

    private static let maxResults = 50

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                filterBar
                Divider()
                resultsList
            }
            .navigationTitle(AppStrings.Engineering.pickGroundFaultLibrary)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) { dismiss() }
                }
            }
            .onAppear { seedDefaults(); runSearch() }
            .onChange(of: search) { _, _ in runSearch() }
            .onChange(of: catalog) { _, _ in runSearch() }
            .onChange(of: szType) { _, _ in runSearch() }
            .onChange(of: sensorAmps) { _, _ in runSearch() }
            .onChange(of: plugAmps) { _, _ in runSearch() }
            .onChange(of: frameAmps) { _, _ in runSearch() }
            .onChange(of: voltage) { _, _ in runSearch() }
        }
    }

    private var filterBar: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if let mfr = manufacturerName, !mfr.isEmpty {
                    Text(AppStrings.Engineering.gfManufacturerHint(mfr))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                TextField(AppStrings.Engineering.gfSearchPlaceholder, text: $search)
                    .textFieldStyle(.roundedBorder)

                HStack(spacing: 8) {
                    TextField(AppStrings.Engineering.gfCatalog, text: $catalog)
                        .textFieldStyle(.roundedBorder)
                    TextField(AppStrings.Engineering.gfTypeContains, text: $szType)
                        .textFieldStyle(.roundedBorder)
                }

                HStack(spacing: 8) {
                    TextField(AppStrings.Engineering.gfSensorA, text: $sensorAmps)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.numberPad)
                    TextField(AppStrings.Engineering.gfPlugA, text: $plugAmps)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.numberPad)
                }
                HStack(spacing: 8) {
                    TextField(AppStrings.Engineering.gfFrameA, text: $frameAmps)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.numberPad)
                    TextField(AppStrings.Engineering.gfVoltage, text: $voltage)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.numberPad)
                }
            }
            .padding(12)
        }
        .frame(maxHeight: 280)
    }

    private var resultsList: some View {
        ScrollView {
            if results.isEmpty {
                Text(AppStrings.Engineering.gfNoMatches)
                    .font(.caption)
                    .italic()
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
            } else {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(results) { match in
                        Button {
                            onPick(match.header)
                            dismiss()
                        } label: {
                            HStack {
                                Text(match.label)
                                    .font(.subheadline)
                                    .foregroundColor(.primary)
                                    .multilineTextAlignment(.leading)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 10)
                            .background(Color(.systemGray6))
                            .cornerRadius(8)
                        }
                        .buttonStyle(.plain)
                    }
                    if truncated {
                        Text(AppStrings.Engineering.gfMoreResults)
                            .font(.caption)
                            .italic()
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 4)
                    }
                }
                .padding(12)
            }
        }
    }

    private func seedDefaults() {
        szType = defaultSzType ?? ""
        sensorAmps = defaultSensorAmps.map { String(Int($0.rounded())) } ?? ""
        plugAmps = defaultPlugAmps.map { String(Int($0.rounded())) } ?? ""
        frameAmps = defaultFrameAmps.map { String(Int($0.rounded())) } ?? ""
        voltage = defaultVoltage.map { String(Int($0.rounded())) } ?? ""
    }

    private func runSearch() {
        guard let mfr = manufacturerId else {
            results = []
            truncated = false
            return
        }
        // Fetch all GF rows for this manufacturer — small set, post-
        // filter in Swift. Optional Int comparisons in #Predicate can
        // trigger type-check timeouts so we keep the predicate simple.
        let gfDevType = GroundFaultResolver.gfSzDevType
        let descriptor = FetchDescriptor<SkmDeviceHeader>(
            predicate: #Predicate { $0.sz_dev_type == gfDevType }
        )
        let all: [SkmDeviceHeader] = (try? modelContext.fetch(descriptor)) ?? []
        let mfrRows: [SkmDeviceHeader] = all.filter { $0.manufacturer_id == mfr }
        let searchLower = search.trimmingCharacters(in: .whitespaces).lowercased()
        let catalogLower = catalog.trimmingCharacters(in: .whitespaces).lowercased()
        let szTypeLower = szType.trimmingCharacters(in: .whitespaces).lowercased()
        let sensorA = Double(sensorAmps.trimmingCharacters(in: .whitespaces))
        let plugA = Double(plugAmps.trimmingCharacters(in: .whitespaces))
        let frameA = Double(frameAmps.trimmingCharacters(in: .whitespaces))
        let voltageA = Double(voltage.trimmingCharacters(in: .whitespaces))

        var matches: [GfMatch] = []
        for header in mfrRows {
            // Text filters — case-insensitive contains.
            if !searchLower.isEmpty {
                let hay = "\(header.sz_type ?? "") \(header.sz_description ?? "") \(header.sz_catalog ?? "")"
                    .lowercased()
                if !hay.contains(searchLower) { continue }
            }
            if !catalogLower.isEmpty {
                if !(header.sz_catalog ?? "").lowercased().contains(catalogLower) {
                    continue
                }
            }
            if !szTypeLower.isEmpty {
                if !(header.sz_type ?? "").lowercased().contains(szTypeLower) {
                    continue
                }
            }
            // Numeric filters — fetch a frame/sensor that hits.
            let headerId = header.id
            let frameDescriptor = FetchDescriptor<SkmFrame>(
                predicate: #Predicate { $0.device_oid == headerId }
            )
            let frames: [SkmFrame] = (try? modelContext.fetch(frameDescriptor)) ?? []
            // Apply frame/voltage filters first.
            var eligibleFrames: [SkmFrame] = frames
            if let v = voltageA {
                let above = eligibleFrames.filter {
                    guard let fv = $0.f_voltage else { return false }
                    return fv >= v
                }
                if !above.isEmpty {
                    let minTier = above.compactMap { $0.f_voltage }.min() ?? 0
                    eligibleFrames = above.filter { ($0.f_voltage ?? 0) == minTier }
                }
            }
            if let fa = frameA {
                let narrowed = eligibleFrames.filter { ($0.f_size ?? 0) == fa }
                if !narrowed.isEmpty { eligibleFrames = narrowed }
            }
            if eligibleFrames.isEmpty && (voltageA != nil || frameA != nil) {
                continue
            }
            // Sensor / plug — load sensors of the first eligible frame.
            let pickedFrame = eligibleFrames.first ?? frames.first
            var pickedSensor: SkmSensor? = nil
            if let fId = pickedFrame?.id {
                let sensorDescriptor = FetchDescriptor<SkmSensor>(
                    predicate: #Predicate { $0.frame_id == fId }
                )
                let sensors: [SkmSensor] = (try? modelContext.fetch(sensorDescriptor)) ?? []
                var sensorPool = sensors
                if let sa = sensorA {
                    let narrowed = sensorPool.filter { ($0.f_size ?? 0) == sa }
                    if !narrowed.isEmpty { sensorPool = narrowed }
                    else if sensorA != nil { continue }
                }
                if let pa = plugA {
                    let narrowed = sensorPool.filter { s in
                        s.plugs.contains(pa) || s.f_size == pa
                    }
                    if !narrowed.isEmpty { sensorPool = narrowed }
                    else if plugA != nil { continue }
                }
                pickedSensor = sensorPool.first
            }
            matches.append(GfMatch(
                id: header.id,
                header: header,
                frameSize: pickedFrame?.f_size,
                sensorSize: pickedSensor?.f_size,
                frameVoltage: pickedFrame?.f_voltage
            ))
            if matches.count >= Self.maxResults * 2 { break }
        }

        truncated = matches.count > Self.maxResults
        results = Array(matches.prefix(Self.maxResults))
    }
}
