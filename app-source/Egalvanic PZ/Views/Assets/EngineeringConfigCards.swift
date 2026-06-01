//
//  EngineeringConfigCards.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 4B-4 — mobile mirrors of web's
//  ``components/unified/EngineeringConfigCards.jsx`` +
//  ``SkmDeviceConfigurator``. Three cards surface below the green
//  "Library Matched" bound card and let the user fine-tune the
//  linked row:
//
//    - ``TransformerConfigCard``  — kVA picker + derived %Z +
//                                   primary / secondary connection.
//    - ``CableConfigCard``        — length / qty / ground / connection
//                                   for cables and busways.
//    - ``SkmTripConfigCard``      — frame / sensor / plug / trip-unit
//                                   + per-segment settings + ground
//                                   fault toggle for protective
//                                   devices (CB / fuse / relay).
//
//  All three read the bound selection from the shared
//  ``Binding<EqpLibSelection?>`` owned by ``EditNodeDetailView``
//  (so saves persist), pull related metadata from local SwiftData
//  (frames / sensors / trip_units / segments / kva_entries) — the
//  full SKM tree downloaded once via Settings — and mirror any
//  first-class column changes onto the engineering draft so search /
//  reports / SLD stay in sync.
//
import SwiftUI
import SwiftData

// MARK: - Transformer Configuration

struct TransformerConfigCard: View {
    @Binding var eqpLibSelection: EqpLibSelection?
    /// First-class draft mirrors. kVA + %Z columns get re-pinned when
    /// the user picks a new kVA so SQL-side readers stay current.
    @Binding var draft: EngineeringDraft

    @Query private var allKvaEntries: [SkmTransformerKvaEntry]

    private var eqp: EqpLibSelection? { eqpLibSelection }

    /// Eligible kVA entries for the bound transformer.
    private var entries: [SkmTransformerKvaEntry] {
        guard let oid = eqp?.skm_oid else { return [] }
        return allKvaEntries
            .filter { $0.transformer_oid == oid }
            .sorted { ($0.nominal_kva ?? 0) < ($1.nominal_kva ?? 0) }
    }

    private var selectedEntry: SkmTransformerKvaEntry? {
        guard let id = eqp?.kva_entry_id else { return nil }
        return allKvaEntries.first(where: { $0.id == id })
    }

    private var derivedPctZ: Double? {
        guard let r = eqp?.percentage_r, let x = eqp?.percentage_x else { return nil }
        let rd: Double = r
        let xd: Double = x
        return sqrt(rd * rd + xd * xd)
    }

    private static let connectionOptions: [String] = [
        "Delta", "Wye", "Wye-Ground", "Wye-Ground-Resistor"
    ]

    var body: some View {
        if let sel = eqpLibSelection, sel.category == "transformers-skm" {
            VStack(alignment: .leading, spacing: 12) {
                Text(AppStrings.Engineering.transformerConfiguration)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                    .textCase(.uppercase)
                    .tracking(0.5)

                // kVA picker — drives R% / X% / derived %Z
                ModernPicker(
                    title: AppStrings.Engineering.kva,
                    icon: "",
                    placeholder: AppStrings.Engineering.selectKva,
                    items: entries.map { KvaOption(from: $0) },
                    selection: Binding(
                        get: { selectedEntry.map { KvaOption(from: $0) } },
                        set: { picked in
                            guard let picked, let entry = entries.first(where: { $0.id == picked.id }) else { return }
                            var s = sel
                            s.kva_rating = entry.nominal_kva
                            s.kva_entry_id = entry.id
                            s.percentage_r = entry.percentage_r
                            s.percentage_x = entry.percentage_x
                            eqpLibSelection = s
                            // Mirror onto draft for SQL-side readers.
                            if let kva = entry.nominal_kva {
                                draft.kva_rating = kva
                            }
                            if let r = entry.percentage_r, let x = entry.percentage_x {
                                let rd: Double = r
                                let xd: Double = x
                                draft.percent_impedance = sqrt(rd * rd + xd * xd)
                            }
                        }
                    ),
                    displayName: { $0.label },
                    allowClear: false
                )

                // Derived %Z (read-only)
                HStack {
                    Text(AppStrings.Engineering.percentImpedance)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text(derivedPctZ.map { String(format: "%.2f%%", $0) } ?? "—")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(.systemGray6))
                .cornerRadius(10)

                ConnectionPicker(
                    title: AppStrings.Engineering.primaryConnection,
                    value: Binding(
                        get: { sel.pri_connection ?? "Delta" },
                        set: { newVal in
                            var s = sel; s.pri_connection = newVal; eqpLibSelection = s
                        }
                    )
                )
                ConnectionPicker(
                    title: AppStrings.Engineering.secondaryConnection,
                    value: Binding(
                        get: { sel.sec_connection ?? "Wye-Ground" },
                        set: { newVal in
                            var s = sel; s.sec_connection = newVal; eqpLibSelection = s
                        }
                    )
                )
            }
            .padding(12)
            .background(Color(.systemGray6).opacity(0.5))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color(.systemGray4), lineWidth: 0.5)
            )
            .cornerRadius(12)
        }
    }

    /// Identifiable wrapper so ModernPicker (which requires
    /// ``T: Identifiable & Hashable``) can drive the kVA list.
    private struct KvaOption: Identifiable, Hashable {
        let id: Int
        let label: String

        init(from entry: SkmTransformerKvaEntry) {
            self.id = entry.id
            let kva = entry.nominal_kva.map { Int($0.rounded()) } ?? 0
            self.label = "\(kva) kVA"
        }
    }
}

// MARK: - Connection picker (shared)

private struct ConnectionPicker: View {
    let title: String
    @Binding var value: String

    private static let options = ["Delta", "Wye", "Wye-Ground", "Wye-Ground-Resistor"]

    var body: some View {
        ModernPicker(
            title: title,
            icon: "",
            placeholder: AppStrings.Engineering.select,
            items: Self.options.map { ConnOption(id: $0) },
            selection: Binding(
                get: { ConnOption(id: value) },
                set: { value = $0?.id ?? value }
            ),
            displayName: { $0.id },
            allowClear: false
        )
    }

    private struct ConnOption: Identifiable, Hashable { let id: String }
}

// MARK: - Cable / busway configuration

struct CableConfigCard: View {
    @Binding var eqpLibSelection: EqpLibSelection?

    private var isCableOrBusway: Bool {
        guard let cat = eqpLibSelection?.category else { return false }
        return cat == "cables-skm" || cat == "busway-skm"
    }

    var body: some View {
        if isCableOrBusway, let sel = eqpLibSelection {
            VStack(alignment: .leading, spacing: 12) {
                Text(sel.category == "busway-skm" ? AppStrings.Engineering.buswayConfiguration : AppStrings.Engineering.cableConfiguration)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                    .textCase(.uppercase)
                    .tracking(0.5)

                // Length (ft)
                EngineeringDoubleField(
                    label: AppStrings.Engineering.length,
                    value: Binding(
                        get: { sel.length },
                        set: { newVal in
                            var s = sel; s.length = newVal; eqpLibSelection = s
                        }
                    ),
                    suffix: "ft"
                )

                // Quantity per phase
                EngineeringIntField(
                    label: AppStrings.Engineering.qtyPerPhase,
                    value: Binding(
                        get: { sel.qty_per_phase },
                        set: { newVal in
                            var s = sel; s.qty_per_phase = newVal; eqpLibSelection = s
                        }
                    )
                )

                // Ground size (descriptive — e.g. "2/0")
                HStack {
                    Text(AppStrings.Engineering.groundSize)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                    TextField(
                        AppStrings.Engineering.groundSizePlaceholder,
                        text: Binding(
                            get: { sel.ground_size_no_parallel ?? "" },
                            set: { newVal in
                                var s = sel
                                s.ground_size_no_parallel = newVal.isEmpty ? nil : newVal
                                eqpLibSelection = s
                            }
                        )
                    )
                    .multilineTextAlignment(.trailing)
                    .frame(minWidth: 80)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(.systemGray6))
                .cornerRadius(10)

                ConnectionPicker(
                    title: AppStrings.Engineering.connectionType,
                    value: Binding(
                        get: { sel.connection_type
                               ?? (sel.category == "busway-skm" ? "Delta" : "Wye-Ground") },
                        set: { newVal in
                            var s = sel; s.connection_type = newVal; eqpLibSelection = s
                        }
                    )
                )
            }
            .padding(12)
            .background(Color(.systemGray6).opacity(0.5))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color(.systemGray4), lineWidth: 0.5)
            )
            .cornerRadius(12)
        }
    }
}

// MARK: - Protective device (CB / fuse / relay) trip configuration

struct SkmTripConfigCard: View {
    @Binding var eqpLibSelection: EqpLibSelection?
    /// Mirror frame / sensor / plug / trip-amp changes onto the
    /// first-class draft columns so search / reports / SLD reflect
    /// the bound row without dipping into the eqp_lib JSONB.
    @Binding var draft: EngineeringDraft

    @Environment(\.modelContext) private var modelContext

    /// ZP-2161 4B-4 perf: device-scoped state, refreshed only when
    /// the bound device, sensor, or trip-unit changes. The prior
    /// implementation used ``@Query`` on the full SkmFrame /
    /// SkmSensor / SkmTripUnit / SkmTuSegment tables (~440k rows
    /// combined) and filtered in Swift — every body eval re-walked
    /// every row. Now each list is fetched via a predicate scoped
    /// to the relevant id and cached in @State; we only refetch
    /// when the underlying id pivot changes.
    @State private var deviceFrames: [SkmFrame] = []
    @State private var deviceTripUnits: [SkmTripUnit] = []
    @State private var sensorsForSelectedFrame: [SkmSensor] = []
    @State private var pluggsForSelectedSensor: [Double] = []
    @State private var segmentsForSelectedTripUnit: [SkmTuSegment] = []

    /// ZP-2243 Ground Fault state. ``deviceHeader`` carries the
    /// bound primary device's sz_dev_type — needed for eligibility
    /// gating since the JSON eqp_lib only stores the slug category,
    /// not the SKM-canonical sz_dev_type. ``gfAutoSibling`` is
    /// resolved by ``GroundFaultResolver``; ``gfManualSibling`` is
    /// set when the user picks a row from the manual picker sheet
    /// (overrides the auto-pair when present).
    @State private var deviceHeader: SkmDeviceHeader? = nil
    @State private var gfAutoSibling: SkmDeviceHeader? = nil
    @State private var gfManualSibling: SkmDeviceHeader? = nil
    @State private var gfTripUnit: SkmTripUnit? = nil
    @State private var gfSegments: [SkmTuSegment] = []
    @State private var gfPickerOpen: Bool = false
    /// One-shot guard so the segments-loaded effect only seeds GF
    /// defaults when toggling on for the first time (prevents the
    /// seeder from clobbering user edits on subsequent re-renders).
    @State private var gfSeededForSiblingOid: Int? = nil

    private var eqp: EqpLibSelection? { eqpLibSelection }
    private var settings: EqpLibSelection.SkmSettings? { eqp?.skm_settings }

    /// Currently-active GF sibling (manual override beats auto).
    private var activeGfSibling: SkmDeviceHeader? {
        gfManualSibling ?? gfAutoSibling
    }

    /// SKM PowerTools surfaces Ground Fault settings only on chassis
    /// families that physically can carry a GF trip unit: SST, Power
    /// Circuit, and HV/MV with an integral trip-unit. Mirrors web's
    /// eligibility check in ``SkmDeviceConfigurator.jsx``.
    private var gfEligible: Bool {
        let dt = (deviceHeader?.sz_dev_type ?? "").lowercased()
        return dt.contains("static trip")
            || dt.contains("power circuit")
            || dt.contains("with integral trip-unit")
    }

    private func loadDeviceLevel() {
        guard let oid = eqp?.skm_oid else {
            deviceFrames = []
            deviceTripUnits = []
            deviceHeader = nil
            gfAutoSibling = nil
            AppLogger.log(.debug, "[SkmTripConfig/loadDeviceLevel] no skm_oid bound — clearing device state", category: .node)
            return
        }
        let frameDescriptor = FetchDescriptor<SkmFrame>(
            predicate: #Predicate { $0.device_oid == oid }
        )
        deviceFrames = (try? modelContext.fetch(frameDescriptor))?
            .sorted { ($0.f_voltage ?? 0) < ($1.f_voltage ?? 0) } ?? []

        let tuDescriptor = FetchDescriptor<SkmTripUnit>(
            predicate: #Predicate { $0.device_oid == oid }
        )
        deviceTripUnits = (try? modelContext.fetch(tuDescriptor))?
            .sorted { ($0.f_size ?? 0) < ($1.f_size ?? 0) } ?? []

        // ZP-2243: pull the device header so we have sz_dev_type for
        // eligibility and the (mfr / type / desc / catalog) signal
        // for GF auto-pair resolution.
        let headerDescriptor = FetchDescriptor<SkmDeviceHeader>(
            predicate: #Predicate { $0.id == oid }
        )
        let header = (try? modelContext.fetch(headerDescriptor))?.first
        deviceHeader = header
        if let header {
            if let gfOid = GroundFaultResolver.findGfSiblingOid(
                for: header, in: modelContext
            ) {
                let gfDescriptor = FetchDescriptor<SkmDeviceHeader>(
                    predicate: #Predicate { $0.id == gfOid }
                )
                gfAutoSibling = (try? modelContext.fetch(gfDescriptor))?.first
            } else {
                gfAutoSibling = nil
            }
        } else {
            gfAutoSibling = nil
        }

        // Re-hydrate manual GF sibling from a saved selection — when
        // a previously-bound row points at a GF cdevice that isn't
        // the auto-pair, restore it so the user sees the same picks.
        rehydrateManualGfSibling()
    }

    /// Fetch the manually-picked GF sibling header (if any) when the
    /// saved ``ground_fault.cdevice_oid`` doesn't match the auto-pair.
    /// Mirrors web's re-hydration effect in SkmDeviceConfigurator.jsx.
    private func rehydrateManualGfSibling() {
        guard let savedOid = settings?.ground_fault?.cdevice_oid else {
            gfManualSibling = nil
            return
        }
        if gfAutoSibling?.id == savedOid {
            gfManualSibling = nil
            return
        }
        if gfManualSibling?.id == savedOid { return }
        let descriptor = FetchDescriptor<SkmDeviceHeader>(
            predicate: #Predicate { $0.id == savedOid }
        )
        gfManualSibling = (try? modelContext.fetch(descriptor))?.first
    }

    /// Load the GF sibling's trip unit + segments so the GF settings
    /// editor can render. The GF cdevice universally has a single
    /// trip unit; we pick its first one to mirror web.
    private func loadGfSiblingData() {
        guard let sibling = activeGfSibling else {
            gfTripUnit = nil
            gfSegments = []
            return
        }
        let siblingOid = sibling.id
        let tuDescriptor = FetchDescriptor<SkmTripUnit>(
            predicate: #Predicate { $0.device_oid == siblingOid }
        )
        let tus: [SkmTripUnit] = (try? modelContext.fetch(tuDescriptor))?
            .sorted { ($0.f_size ?? 0) < ($1.f_size ?? 0) } ?? []
        gfTripUnit = tus.first
        if let tuId = gfTripUnit?.id {
            let segDescriptor = FetchDescriptor<SkmTuSegment>(
                predicate: #Predicate { $0.trip_unit_oid == tuId }
            )
            gfSegments = (try? modelContext.fetch(segDescriptor))?
                .sorted { $0.id < $1.id } ?? []
        } else {
            gfSegments = []
        }
    }

    private func loadSensorsForFrame() {
        guard let frameId = settings?.frame_id else {
            sensorsForSelectedFrame = []
            pluggsForSelectedSensor = []
            return
        }
        let descriptor = FetchDescriptor<SkmSensor>(
            predicate: #Predicate { $0.frame_id == frameId }
        )
        sensorsForSelectedFrame = (try? modelContext.fetch(descriptor))?
            .sorted { ($0.f_size ?? 0) < ($1.f_size ?? 0) } ?? []
        loadPlugsForSensor()
    }

    private func loadPlugsForSensor() {
        guard let sensorId = settings?.sensor_id,
              let sensor = sensorsForSelectedFrame.first(where: { $0.id == sensorId })
        else {
            pluggsForSelectedSensor = []
            return
        }
        // ZP-2161: some SKM sensors have no explicit plug list (e.g.
        // ABB EMAX / PR111 chassis where the "plug" rating equals
        // the sensor rating). When ``plugs`` is empty, synthesize a
        // single-value list using the sensor's own f_size so the
        // user still sees a meaningful pick.
        if sensor.plugs.isEmpty, let sz = sensor.f_size {
            pluggsForSelectedSensor = [sz]
        } else {
            pluggsForSelectedSensor = sensor.plugs.sorted()
        }
    }

    private func loadSegmentsForTripUnit() {
        guard let tuOid = settings?.trip_unit_oid else {
            segmentsForSelectedTripUnit = []
            return
        }
        let descriptor = FetchDescriptor<SkmTuSegment>(
            predicate: #Predicate { $0.trip_unit_oid == tuOid }
        )
        let loaded = (try? modelContext.fetch(descriptor))?
            .sorted { $0.id < $1.id } ?? []
        segmentsForSelectedTripUnit = loaded
        // ZP-2161 Phase 4B-4: seed defaults on first render once
        // segments are loaded. Mirrors web's seedSegmentDefaults +
        // seedIncludedSegmentIds (SkmDeviceConfigurator.jsx) — if a
        // saved selection already exists, leave it alone. Without
        // this, a freshly-bound device shows every settings row
        // disabled until the user manually toggles each one on.
        seedSegmentDefaultsIfNeeded(loaded)
    }

    /// SKM export cap — at most 10 segments can participate in the
    /// device's protection curve per export row. Matches the BE /
    /// web ``MAX_SEGMENTS_PER_EXPORT`` constant.
    private static let maxSegmentsPerExport = 10

    /// Populate ``segment_selections`` + ``included_segment_ids`` with
    /// reasonable defaults when the bound row hasn't been touched yet.
    /// Defaults per segment:
    ///   - value     = first label (or first value, stringified)
    ///   - included? = segments with ``init_on = true`` get included;
    ///                 if none have it, include every segment (capped
    ///                 at maxSegmentsPerExport).
    private func seedSegmentDefaultsIfNeeded(_ segments: [SkmTuSegment]) {
        guard let sel = eqpLibSelection else { return }
        // Only seed when there's nothing already there — preserve any
        // prior user edits or BE-side seeded selections.
        let existingSels = sel.skm_settings?.segment_selections ?? [:]
        let existingIds = sel.skm_settings?.included_segment_ids ?? []
        if !existingSels.isEmpty || !existingIds.isEmpty { return }
        if segments.isEmpty { return }

        var newSelections: [String: SegmentValue] = [:]
        for seg in segments {
            newSelections[String(seg.id)] = Self.seedValue(for: seg)
        }

        let initOnSegs = segments.filter { $0.init_on }
        let pickSource = initOnSegs.isEmpty ? segments : initOnSegs
        let newIncluded = pickSource
            .prefix(Self.maxSegmentsPerExport)
            .map { $0.id }

        var updated = sel
        var st = updated.skm_settings ?? EqpLibSelection.SkmSettings()
        st.segment_selections = newSelections
        st.included_segment_ids = Array(newIncluded)
        updated.skm_settings = st
        eqpLibSelection = updated
    }

    /// ZP-2243: seed a single segment's default ``SegmentValue`` based
    /// on its subtype. Mirrors web's seedSegmentDefaults logic so the
    /// initial picker state matches what PowerTools loads at the
    /// library default. Three cases:
    ///   - CSEGMINSTBANDS compound (dial_labels present): value = first
    ///     setting label, suffix = first dial label.
    ///   - CSEGMISQUARETDELAY (i²t toggle): value = first label,
    ///     suffix = ``display_suffix`` from the library default.
    ///   - Default: value = first label, suffix = "".
    private static func seedValue(for seg: SkmTuSegment) -> SegmentValue {
        let firstLabel = seg.setting_labels.first
        let firstValue = seg.setting_values.first.map { String($0) }
        let primary = firstLabel ?? firstValue ?? ""
        // ZP-2508: CSEGMINSTBANDS is a compound (primary + dial) whenever a
        // dial exists — primary defaults to the first setting label, suffix to
        // the first dial label. (Was hardcoding value="Fixed" + gating on
        // setting_labels.isEmpty, which broke once the SettingArray backfill
        // gave every band a real primary.)
        if seg.subtype == "CSEGMINSTBANDS" && !seg.dial_labels.isEmpty {
            return SegmentValue(value: primary, suffix: seg.dial_labels.first ?? "")
        }
        if seg.subtype == "CSEGMISQUARETDELAY" {
            return SegmentValue(value: primary, suffix: seg.display_suffix ?? "")
        }
        return SegmentValue(value: primary, suffix: "")
    }

    private var supportedCategory: Bool {
        guard let cat = eqp?.category else { return false }
        return ["mccb-breakers", "iccb-breakers", "pcb-breakers",
                "lv-fuses", "hv-fuses", "dc-fuses", "hv-breakers",
                "relays-electronic", "relays-electromechanical", "relays-iec"]
            .contains(cat)
    }

    var body: some View {
        if supportedCategory, let sel = eqpLibSelection {
            VStack(alignment: .leading, spacing: 12) {
                Text(AppStrings.Engineering.tripConfiguration)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                    .textCase(.uppercase)
                    .tracking(0.5)

                // ZP-2161: render rules per web SkmDeviceDetail.jsx:
                //   - Relays hide the entire frame/sensor/plug row
                //     (placeholder zeros; settings are the meaningful
                //     axis).
                //   - Fuses render only a single "Fuse Rating" picker
                //     (frame.f_size == sensor.f_size == trip_unit.f_size).
                //   - Everything else (breakers — SST or thermal-mag)
                //     renders frame + sensor + plug. The sensor is
                //     labeled "Trip" for thermal-mag / MCP / molded-
                //     case and "Sensor" for SST families. Trip unit is
                //     NEVER user-facing — it's auto-resolved from the
                //     frame's f_size on each frame change.
                let isFuse = sel.category == "lv-fuses"
                    || sel.category == "hv-fuses"
                    || sel.category == "dc-fuses"
                let isRelay = sel.category == "relays-electronic"
                    || sel.category == "relays-electromechanical"
                    || sel.category == "relays-iec"

                if !isRelay {
                    framePicker(sel: sel)
                }
                if !isRelay && !isFuse {
                    sensorPicker(sel: sel)
                    plugPicker(sel: sel)
                }
                if !isFuse {
                    segmentsEditor(sel: sel)
                }
                groundFaultSection(sel: sel)
            }
            .padding(12)
            .background(Color(.systemGray6).opacity(0.5))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color(.systemGray4), lineWidth: 0.5)
            )
            .cornerRadius(12)
            // ZP-2161 4B-4 perf: refetch device-scoped rows only
            // when the pivot ids change. Body re-evals (e.g. from
            // unrelated draft mutations) no longer re-walk the
            // ~440k SKM tree.
            .onAppear {
                loadDeviceLevel()
                loadSensorsForFrame()
                loadSegmentsForTripUnit()
                loadGfSiblingData()
            }
            .onChange(of: sel.skm_oid) { _, _ in
                loadDeviceLevel()
                loadSensorsForFrame()
                loadSegmentsForTripUnit()
                loadGfSiblingData()
                gfSeededForSiblingOid = nil
            }
            .onChange(of: settings?.frame_id) { _, _ in
                loadSensorsForFrame()
                rebuildGroundFaultPayload()
            }
            .onChange(of: settings?.sensor_id) { _, _ in
                loadPlugsForSensor()
                rebuildGroundFaultPayload()
            }
            .onChange(of: settings?.plug_amps) { _, _ in
                rebuildGroundFaultPayload()
            }
            .onChange(of: settings?.trip_unit_oid) { _, _ in
                loadSegmentsForTripUnit()
            }
            .onChange(of: activeGfSibling?.id) { _, _ in
                loadGfSiblingData()
            }
        }
    }

    // MARK: - Ground Fault payload assembly (mirrors web)

    /// Rebuild the full ``ground_fault`` block when the user is on
    /// (toggle enabled and a sibling is active). The block re-derives
    /// chassis pegs from the SST-side picks every time, so any change
    /// to the user's frame/sensor/plug propagates into the GF payload
    /// automatically. Settings array is computed from the current
    /// GF segment selections + included ids.
    private func rebuildGroundFaultPayload() {
        guard var sel = eqpLibSelection else { return }
        var st = sel.skm_settings ?? EqpLibSelection.SkmSettings()
        guard st.has_ground_fault == true, let sibling = activeGfSibling else {
            // Off OR no sibling — clear the payload but preserve flag
            // state (user may have toggled off; or sibling will resolve
            // later when SwiftData lands).
            if st.ground_fault != nil {
                st.ground_fault = nil
                sel.skm_settings = st
                eqpLibSelection = sel
            }
            return
        }

        let sstFrame = deviceFrames.first(where: { $0.id == st.frame_id })
        let sstSensor = sensorsForSelectedFrame.first(where: { $0.id == st.sensor_id })
        let picks = GroundFaultResolver.picks(
            sstFrame: sstFrame,
            sstSensor: sstSensor,
            sstPlugAmps: st.plug_amps,
            gfDeviceOid: sibling.id,
            in: modelContext
        )

        let gfSelections = st.ground_fault?.segment_selections ?? [:]
        let gfIncluded = st.ground_fault?.included_segment_ids ?? []
        let settingsArr = GroundFaultResolver.buildSettings(
            tripUnitSegments: gfSegments,
            selections: gfSelections,
            includedIds: gfIncluded
        )

        var gf = EqpLibSelection.GroundFault()
        gf.cdevice_oid = sibling.id
        gf.library_type = sibling.sz_type
        gf.library_description = sibling.sz_description
        gf.trip_unit_oid = gfTripUnit?.id
        gf.frame_id = picks.frame?.id
        gf.frame_label = picks.frameLabel
        gf.frame_amps = picks.frame?.f_size
        gf.frame_voltage = picks.frame?.f_voltage
        gf.frame_interrupting_rating = picks.frame?.f_interrupting_rating
        gf.sensor_id = picks.sensor?.id
        gf.sensor_amps = picks.sensor?.f_size
        gf.plug_amps = picks.plugAmps
        gf.segment_selections = gfSelections
        gf.included_segment_ids = gfIncluded
        gf.settings = settingsArr
        gf.ia_package = EqpLibSelection.GroundFault.IAPackage(
            libraryType: sibling.sz_type,
            libraryDescription: sibling.sz_description,
            frame: picks.frameLabel,
            sensor: picks.sensor?.f_size.map { String(Int($0.rounded())) },
            plug: picks.plugAmps.map { String(Int($0.rounded())) },
            settings: settingsArr
        )

        st.ground_fault = gf
        sel.skm_settings = st
        eqpLibSelection = sel
    }

    /// Populate GF segment defaults on first toggle-on, mirroring
    /// web's seedSegmentDefaults + seedIncludedSegmentIds applied to
    /// the GF sibling's first trip unit.
    private func seedGfSegmentDefaultsIfNeeded() {
        guard let sel = eqpLibSelection else { return }
        let existingSels = sel.skm_settings?.ground_fault?.segment_selections ?? [:]
        let existingIds = sel.skm_settings?.ground_fault?.included_segment_ids ?? []
        if !existingSels.isEmpty || !existingIds.isEmpty { return }
        if gfSegments.isEmpty { return }

        var newSelections: [String: SegmentValue] = [:]
        for seg in gfSegments {
            newSelections[String(seg.id)] = Self.seedValue(for: seg)
        }
        let initOnSegs = gfSegments.filter { $0.init_on }
        let pickSource = initOnSegs.isEmpty ? gfSegments : initOnSegs
        let newIncluded = pickSource
            .prefix(Self.maxSegmentsPerExport)
            .map { $0.id }

        var updated = sel
        var st = updated.skm_settings ?? EqpLibSelection.SkmSettings()
        var gf = st.ground_fault ?? EqpLibSelection.GroundFault()
        gf.segment_selections = newSelections
        gf.included_segment_ids = Array(newIncluded)
        st.ground_fault = gf
        updated.skm_settings = st
        eqpLibSelection = updated
        rebuildGroundFaultPayload()
    }

    // MARK: Pickers

    @ViewBuilder
    private func framePicker(sel: EqpLibSelection) -> some View {
        // ZP-2161: web's frame label is class-specific.
        //   fuses → "Fuse Rating"
        //   HV/MV without integral trip → "Rating"
        //   everything else → "Frame"
        // We approximate from the eqp_lib slug since we don't carry
        // the precise SKM ``sz_dev_type`` post-pick.
        let isFuse = sel.category == "lv-fuses"
            || sel.category == "hv-fuses"
            || sel.category == "dc-fuses"
        let title = isFuse ? AppStrings.Engineering.fuseRating : AppStrings.Engineering.frame
        ModernPicker(
            title: title,
            icon: "",
            placeholder: AppStrings.Engineering.selectLowercased(title.lowercased()),
            items: deviceFrames.map { FrameOption(from: $0) },
            selection: Binding(
                get: { settings?.frame_id.flatMap { id in deviceFrames.first(where: { $0.id == id }).map(FrameOption.init(from:)) } },
                set: { picked in
                    guard let picked,
                          let frame = deviceFrames.first(where: { $0.id == picked.id })
                    else { return }
                    handleFrameChange(to: frame, in: sel)
                }
            ),
            displayName: { $0.label },
            allowClear: false,
            useSheet: deviceFrames.count > 8
        )
    }

    /// Mirrors web's ``handleFrameChange`` in SkmDeviceDetail.jsx:
    /// frame change cascade-resolves the first sensor of the new
    /// frame, that sensor's first plug (falling back to sensor.f_size
    /// when ``plugs`` is empty), and the trip unit whose ``f_size``
    /// equals the frame's ``f_size`` (falling back to first). Segment
    /// selections are reset; the segment seeder re-runs on the next
    /// trip-unit reload.
    private func handleFrameChange(to frame: SkmFrame, in sel: EqpLibSelection) {
        // First sensor of the new frame.
        let frameId = frame.id
        let descriptor = FetchDescriptor<SkmSensor>(
            predicate: #Predicate { $0.frame_id == frameId }
        )
        let frameSensors = ((try? modelContext.fetch(descriptor)) ?? [])
            .sorted { ($0.f_size ?? 0) < ($1.f_size ?? 0) }
        let firstSensor = frameSensors.first
        // First plug of that sensor — synthesize from sensor.f_size
        // when ``plugs`` is empty.
        let firstPlug: Double? = firstSensor.flatMap {
            $0.plugs.first ?? $0.f_size
        }
        // Trip-unit whose f_size matches the frame's (auto-resolve).
        let matchingTU = deviceTripUnits.first(where: { tu in
            guard let tuSize = tu.f_size, let frameSize = frame.f_size else { return false }
            return tuSize == frameSize
        }) ?? deviceTripUnits.first

        var updated = sel
        var st = updated.skm_settings ?? EqpLibSelection.SkmSettings()
        st.frame_id = frame.id
        st.frame_amps = frame.f_size
        st.sensor_id = firstSensor?.id
        st.sensor_amps = firstSensor?.f_size
        st.plug_amps = firstPlug
        // Auto-resolved trip unit drives the segments list.
        if matchingTU?.id != st.trip_unit_oid {
            st.trip_unit_oid = matchingTU?.id
            // Reset segment selections — new trip-unit owns a
            // different segment set. The seeder re-populates
            // defaults when the segments reload.
            st.segment_selections = [:]
            st.included_segment_ids = []
        }
        updated.skm_settings = st
        eqpLibSelection = updated

        // Mirror onto first-class draft columns.
        if let sz = frame.f_size {
            draft.frame_amps = Int(sz.rounded())
        }
        if let sa = firstSensor?.f_size {
            draft.sensor_amps = Int(sa.rounded())
        } else {
            draft.sensor_amps = nil
        }
        if let pa = firstPlug {
            draft.plug_amps = Int(pa.rounded())
        } else {
            draft.plug_amps = nil
        }
    }

    @ViewBuilder
    private func sensorPicker(sel: EqpLibSelection) -> some View {
        // ZP-2161: SKM PowerTools labels the picker as "Trip" for
        // thermal-mag / MCP / molded-case devices (continuous-current
        // rating) and "Sensor" for SST families (sensor tier inside
        // a chassis). Mirror web's SkmDeviceDetail.jsx sensorLabel.
        let title = (sel.use_sst == true) ? AppStrings.Engineering.sensor : AppStrings.Engineering.trip
        ModernPicker(
            title: title,
            icon: "",
            placeholder: AppStrings.Engineering.selectLowercased(title.lowercased()),
            items: sensorsForSelectedFrame.map { SensorOption(from: $0) },
            selection: Binding(
                get: { settings?.sensor_id.flatMap { id in sensorsForSelectedFrame.first(where: { $0.id == id }).map(SensorOption.init(from:)) } },
                set: { picked in
                    guard let picked,
                          let sensor = sensorsForSelectedFrame.first(where: { $0.id == picked.id })
                    else { return }
                    // Sensor change → first plug of that sensor, falling
                    // back to sensor.f_size when the plug list is empty.
                    let firstPlug = sensor.plugs.first ?? sensor.f_size
                    var s = sel
                    var st = s.skm_settings ?? EqpLibSelection.SkmSettings()
                    st.sensor_id = sensor.id
                    st.sensor_amps = sensor.f_size
                    st.plug_amps = firstPlug
                    s.skm_settings = st
                    eqpLibSelection = s
                    if let sz = sensor.f_size {
                        draft.sensor_amps = Int(sz.rounded())
                    }
                    if let pa = firstPlug {
                        draft.plug_amps = Int(pa.rounded())
                    } else {
                        draft.plug_amps = nil
                    }
                }
            ),
            displayName: { $0.label },
            allowClear: false,
            useSheet: sensorsForSelectedFrame.count > 8
        )
        .disabled(settings?.frame_id == nil || sensorsForSelectedFrame.count <= 1)
    }

    @ViewBuilder
    private func plugPicker(sel: EqpLibSelection) -> some View {
        ModernPicker(
            title: AppStrings.Engineering.plug,
            icon: "",
            placeholder: AppStrings.Engineering.selectLowercased(AppStrings.Engineering.plug.lowercased()),
            items: pluggsForSelectedSensor.map { PlugOption(value: $0) },
            selection: Binding(
                get: { settings?.plug_amps.map { PlugOption(value: $0) } },
                set: { picked in
                    guard let picked else { return }
                    var s = sel
                    var st = s.skm_settings ?? EqpLibSelection.SkmSettings()
                    st.plug_amps = picked.value
                    s.skm_settings = st
                    eqpLibSelection = s
                    draft.plug_amps = Int(picked.value.rounded())
                }
            ),
            displayName: { $0.label },
            allowClear: false,
            useSheet: pluggsForSelectedSensor.count > 8
        )
        .disabled(settings?.sensor_id == nil || pluggsForSelectedSensor.count <= 1)
    }

    @ViewBuilder
    private func tripUnitPicker(sel: EqpLibSelection) -> some View {
        if !deviceTripUnits.isEmpty {
            ModernPicker(
                title: AppStrings.Engineering.tripUnit,
                icon: "",
                placeholder: AppStrings.Engineering.selectLowercased(AppStrings.Engineering.tripUnit.lowercased()),
                items: deviceTripUnits.map { TripUnitOption(from: $0) },
                selection: Binding(
                    get: { settings?.trip_unit_oid.flatMap { oid in deviceTripUnits.first(where: { $0.id == oid }).map(TripUnitOption.init(from:)) } },
                    set: { picked in
                        guard let picked,
                              let tu = deviceTripUnits.first(where: { $0.id == picked.id })
                        else { return }
                        var s = sel
                        var st = s.skm_settings ?? EqpLibSelection.SkmSettings()
                        st.trip_unit_oid = tu.id
                        // Trip-unit change resets segment selections —
                        // the new unit has its own segment set.
                        st.segment_selections = [:]
                        st.included_segment_ids = []
                        s.skm_settings = st
                        eqpLibSelection = s
                    }
                ),
                displayName: { $0.label },
                allowClear: false,
                useSheet: deviceTripUnits.count > 8
            )
        }
    }

    @ViewBuilder
    private func groundFaultSection(sel: EqpLibSelection) -> some View {
        // ZP-2243: gated to LV breaker trip-type families that can
        // physically carry GF — SST, Power Circuit, HV/MV with
        // integral trip-unit. Mirrors web's eligibility check.
        if gfEligible {
            VStack(alignment: .leading, spacing: 8) {
                Divider().padding(.vertical, 4)

                Toggle(isOn: Binding(
                    get: { sel.skm_settings?.has_ground_fault ?? false },
                    set: { newVal in
                        var s = sel
                        var st = s.skm_settings ?? EqpLibSelection.SkmSettings()
                        st.has_ground_fault = newVal
                        if !newVal {
                            st.ground_fault = nil
                        }
                        s.skm_settings = st
                        eqpLibSelection = s
                        if newVal {
                            // Seed segment defaults if this is the
                            // first time enabling GF for this sibling.
                            if let oid = activeGfSibling?.id,
                               gfSeededForSiblingOid != oid {
                                gfSeededForSiblingOid = oid
                                seedGfSegmentDefaultsIfNeeded()
                            }
                            rebuildGroundFaultPayload()
                        }
                    }
                )) {
                    Text(AppStrings.Engineering.addGroundFaultSettings)
                        .font(.subheadline)
                        .fontWeight(.medium)
                }

                if sel.skm_settings?.has_ground_fault == true {
                    if let sibling = activeGfSibling {
                        // Paired sibling + Change… button.
                        HStack(spacing: 6) {
                            Text(gfManualSibling != nil
                                 ? AppStrings.Engineering.manuallyPickedGfLibrary
                                 : AppStrings.Engineering.autoPairedGfLibrary)
                                .font(.caption)
                                .italic()
                                .foregroundColor(.secondary)
                            Text("\(sibling.sz_type ?? "—") — \(sibling.sz_description ?? "—")")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(2)
                            Spacer()
                            Button(AppStrings.Engineering.changeEllipsis) {
                                gfPickerOpen = true
                            }
                            .font(.caption)
                            .buttonStyle(.plain)
                            .foregroundColor(.blue)
                        }

                        // GF settings editor — reuses SegmentRow.
                        if !gfSegments.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                Text(AppStrings.Engineering.groundFaultSettings)
                                    .font(.caption2)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.secondary)
                                    .textCase(.uppercase)
                                    .padding(.top, 4)

                                ForEach(gfSegments) { seg in
                                    SegmentRow(
                                        segment: seg,
                                        selectedValue: sel.skm_settings?.ground_fault?
                                            .segment_selections?[String(seg.id)]?.value ?? "",
                                        selectedSuffix: sel.skm_settings?.ground_fault?
                                            .segment_selections?[String(seg.id)]?.suffix ?? "",
                                        included: sel.skm_settings?.ground_fault?
                                            .included_segment_ids?.contains(seg.id) ?? false,
                                        onChange: { value, suffix in
                                            updateGfSegment(segId: seg.id, value: value, suffix: suffix)
                                        },
                                        onToggleInclude: { include in
                                            toggleGfSegmentInclude(segId: seg.id, include: include)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // No GF sibling found — offer the manual picker.
                        HStack {
                            Image(systemName: "info.circle")
                                .foregroundColor(.blue)
                            Text(AppStrings.Engineering.noGfPairFound)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            Button(AppStrings.Engineering.addAnyway) {
                                gfPickerOpen = true
                            }
                            .font(.caption)
                            .buttonStyle(.borderedProminent)
                            .controlSize(.small)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.blue.opacity(0.08))
                        .cornerRadius(8)
                    }
                }
            }
            .sheet(isPresented: $gfPickerOpen) {
                GroundFaultPickerSheet(
                    manufacturerId: deviceHeader?.manufacturer_id,
                    manufacturerName: deviceHeader.flatMap { _ in eqp?.manufacturer },
                    defaultSzType: deviceHeader?.sz_type,
                    defaultSensorAmps: settings?.sensor_amps,
                    defaultPlugAmps: settings?.plug_amps,
                    defaultFrameAmps: settings?.frame_amps,
                    defaultVoltage: deviceFrames.first(where: { $0.id == settings?.frame_id })?.f_voltage,
                    onPick: { picked in
                        gfManualSibling = picked
                        gfSeededForSiblingOid = nil
                        // Clear stale GF selections so the seeder
                        // re-populates from the new sibling's segments.
                        if var sel = eqpLibSelection {
                            var st = sel.skm_settings ?? EqpLibSelection.SkmSettings()
                            var gf = st.ground_fault ?? EqpLibSelection.GroundFault()
                            gf.segment_selections = [:]
                            gf.included_segment_ids = []
                            st.ground_fault = gf
                            st.has_ground_fault = true
                            sel.skm_settings = st
                            eqpLibSelection = sel
                        }
                        gfPickerOpen = false
                    }
                )
            }
        }
    }

    /// Update a single GF segment's value + suffix and rebuild the
    /// payload. SegmentRow always passes both pieces — for compound
    /// segments the value is locked to "Fixed" and the suffix is the
    /// user-picked dial; for i²t segments the value is the user pick
    /// and the suffix is the i²t On/Off toggle's encoding.
    private func updateGfSegment(segId: Int, value: String, suffix: String) {
        guard var sel = eqpLibSelection else { return }
        var st = sel.skm_settings ?? EqpLibSelection.SkmSettings()
        var gf = st.ground_fault ?? EqpLibSelection.GroundFault()
        var sels = gf.segment_selections ?? [:]
        sels[String(segId)] = SegmentValue(value: value, suffix: suffix)
        gf.segment_selections = sels
        // Auto-include once the user picks a non-empty value.
        var inc = gf.included_segment_ids ?? []
        if !value.isEmpty && !inc.contains(segId) {
            inc.append(segId)
        }
        gf.included_segment_ids = inc
        st.ground_fault = gf
        sel.skm_settings = st
        eqpLibSelection = sel
        rebuildGroundFaultPayload()
    }

    private func toggleGfSegmentInclude(segId: Int, include: Bool) {
        guard var sel = eqpLibSelection else { return }
        var st = sel.skm_settings ?? EqpLibSelection.SkmSettings()
        var gf = st.ground_fault ?? EqpLibSelection.GroundFault()
        var inc = gf.included_segment_ids ?? []
        if include {
            if !inc.contains(segId), inc.count < Self.maxSegmentsPerExport {
                inc.append(segId)
            }
        } else {
            inc.removeAll { $0 == segId }
        }
        gf.included_segment_ids = inc
        st.ground_fault = gf
        sel.skm_settings = st
        eqpLibSelection = sel
        rebuildGroundFaultPayload()
    }

    @ViewBuilder
    private func segmentsEditor(sel: EqpLibSelection) -> some View {
        let segments = segmentsForSelectedTripUnit
        if !segments.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text(AppStrings.Engineering.settings)
                    .font(.caption2)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                    .textCase(.uppercase)
                    .padding(.top, 4)

                ForEach(segments) { seg in
                    SegmentRow(
                        segment: seg,
                        selectedValue: settings?.segment_selections?[String(seg.id)]?.value ?? "",
                        selectedSuffix: settings?.segment_selections?[String(seg.id)]?.suffix ?? "",
                        included: settings?.included_segment_ids?.contains(seg.id) ?? false,
                        onChange: { value, suffix in
                            var s = sel
                            var st = s.skm_settings ?? EqpLibSelection.SkmSettings()
                            var sels = st.segment_selections ?? [:]
                            sels[String(seg.id)] = SegmentValue(value: value, suffix: suffix)
                            st.segment_selections = sels
                            // Mark as included once the user picks a value.
                            var inc = st.included_segment_ids ?? []
                            if !value.isEmpty, !inc.contains(seg.id) {
                                inc.append(seg.id)
                            }
                            st.included_segment_ids = inc
                            s.skm_settings = st
                            eqpLibSelection = s
                        },
                        onToggleInclude: { include in
                            var s = sel
                            var st = s.skm_settings ?? EqpLibSelection.SkmSettings()
                            var inc = st.included_segment_ids ?? []
                            if include, !inc.contains(seg.id) {
                                inc.append(seg.id)
                            } else if !include {
                                inc.removeAll { $0 == seg.id }
                            }
                            st.included_segment_ids = inc
                            s.skm_settings = st
                            eqpLibSelection = s
                        }
                    )
                }
            }
        }
    }

    // ── ModernPicker option shims ────────────────────────────────

    private struct FrameOption: Identifiable, Hashable {
        let id: Int
        let label: String
        init(from f: SkmFrame) {
            self.id = f.id
            // ZP-2161: rich label — "{sz_name} {amps}A @ {volts}V (kA)".
            // sz_name comes through as the human-readable frame
            // identifier (e.g. "E6.2 H-A"). Voltage + kA are optional
            // — older / generic frames may not carry them.
            var parts: [String] = []
            if let name = f.sz_name, !name.isEmpty { parts.append(name) }
            if let s = f.f_size { parts.append("\(Int(s.rounded()))A") }
            if let v = f.f_voltage { parts.append("@ \(Int(v.rounded()))V") }
            var label = parts.joined(separator: " ")
            // kA list — only ``f_interrupting_rating`` is currently
            // synced on mobile; show it parenthesized when present.
            // Once we extend the BE/sync to also carry f_icw and
            // f_asymmetrical_rating, append them here as additional
            // entries inside the parens.
            if let ka = f.f_interrupting_rating, ka > 0 {
                let kaStr = ka.truncatingRemainder(dividingBy: 1) == 0
                    ? "\(Int(ka))"
                    : String(format: "%.0f", ka)
                label += " (\(kaStr)kA)"
            }
            self.label = label.isEmpty ? "Frame \(f.id)" : label
        }
    }

    private struct SensorOption: Identifiable, Hashable {
        let id: Int
        let label: String
        init(from s: SkmSensor) {
            self.id = s.id
            let size = s.f_size.map { Int($0.rounded()) } ?? 0
            self.label = "\(size)AS"
        }
    }

    private struct PlugOption: Identifiable, Hashable {
        let value: Double
        var id: Double { value }
        var label: String { "\(Int(value.rounded()))AP" }
    }

    private struct TripUnitOption: Identifiable, Hashable {
        let id: Int
        let label: String
        init(from tu: SkmTripUnit) {
            self.id = tu.id
            let size = tu.f_size.map { Int($0.rounded()) } ?? 0
            self.label = "\(size)A"
        }
    }
}

// MARK: - Segment row (per-axis settings)
//
// ZP-2243 / ZP-2508: three rendering modes driven by ``subtype`` + library:
//
//   1. CSEGMINSTBANDS compound: ``dial_labels`` non-empty. Two pickers —
//      Setting1 (``setting_labels``, with the LO/HI/{n}X display rule) and
//      the dial (``dial_labels``, e.g. "Fixed" / "B-A" / "N-A"). Stored as
//      {value: Setting1, suffix: dial}; exported as ``{value}\,{suffix}``.
//   2. CSEGMISQUARETDELAY (i²t toggle): value picker like normal,
//      plus a 2-state i²t On/Off toggle that drives the suffix
//      ``"(I^s T On)"`` / ``"(I^s T Off)"``. Disabled when isqt_locked.
//   3. Normal: single value picker; suffix is always empty.
//
// Matches the contract web's SkmDeviceConfigurator.jsx writes to
// ``segment_selections`` (string for normal, ``{value, suffix}`` for
// modes 1 and 2).

private struct SegmentRow: View {
    let segment: SkmTuSegment
    let selectedValue: String
    let selectedSuffix: String
    let included: Bool
    let onChange: (String, String) -> Void  // (newValue, newSuffix)
    let onToggleInclude: (Bool) -> Void

    // ZP-2508 display rule (mirrors web segDisplay): SKM shows the raw label
    // for almost everything; only the magnitude descriptors "LO"/"HI" and the
    // "{n}X" multipliers resolve to the numeric setting value (LO=3, 5X=5).
    // Mode keywords (Fixed/OFF), profile/curve letters, and MIN/INT/MAX show
    // verbatim. The stored value is ALWAYS the label; only the display swaps.
    private func cleanNum(_ v: Double) -> String {
        // Strip float32 noise (0.699999988 -> 0.7) to ~6 significant figures.
        String(format: "%g", v)
    }
    private func resolvesToValue(_ label: String) -> Bool {
        let s = label.trimmingCharacters(in: .whitespaces).lowercased()
        if s == "lo" || s == "hi" { return true }
        return s.range(of: #"^\d+(\.\d+)?x$"#, options: .regularExpression) != nil
    }
    /// (stored value = label) → (display text) pairs for the primary picker.
    private var settingChoices: [(value: String, display: String)] {
        if !segment.setting_labels.isEmpty {
            return segment.setting_labels.enumerated().map { i, lbl in
                let display = (resolvesToValue(lbl) && i < segment.setting_values.count)
                    ? cleanNum(segment.setting_values[i]) : lbl
                return (value: lbl, display: display)
            }
        }
        return segment.setting_values.map { (value: String($0), display: cleanNum($0)) }
    }
    private func displayForValue(_ value: String) -> String {
        settingChoices.first(where: { $0.value == value })?.display ?? value
    }

    private var hasIsqtToggle: Bool {
        segment.subtype == "CSEGMISQUARETDELAY"
    }

    // ZP-2508: every CSEGMINSTBANDS in the library carries a dial, so the
    // compound test is just subtype + dial presence — render Setting1
    // (setting_labels) and the dial (dial_labels) as two pickers.
    private var isInstBandsCompound: Bool {
        segment.subtype == "CSEGMINSTBANDS" && !segment.dial_labels.isEmpty
    }

    private var isqtMode: String {
        // Web's encoding: "(I^s T On)" vs "(I^s T Off)". Anything else
        // (including the empty default) treats as Off so the toggle
        // doesn't jump to On with no data.
        selectedSuffix == "(I^s T On)" ? "on" : "off"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(segment.sz_name ?? segment.subtype)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Spacer()
                Toggle("", isOn: Binding(
                    get: { included },
                    set: { onToggleInclude($0) }
                ))
                .labelsHidden()
            }
            if included {
                if isInstBandsCompound {
                    instBandsCompoundEditor
                } else {
                    // ZP-2243: segments with no setting_values /
                    // setting_labels (e.g. CSEGMOPENCLEARCURVE
                    // "Thermal Curve (Cold)") have nothing to pick;
                    // hide the empty Menu entirely. The include
                    // toggle alone communicates the segment's state.
                    if !settingChoices.isEmpty {
                        valuePicker
                    }
                    if hasIsqtToggle {
                        isqtToggle
                    }
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }

    // ── Normal value picker ──────────────────────────────────────
    private var valuePicker: some View {
        Menu {
            ForEach(settingChoices.indices, id: \.self) { i in
                Button(settingChoices[i].display) {
                    onChange(settingChoices[i].value, selectedSuffix)
                }
            }
        } label: {
            pickerLabel(displayForValue(selectedValue), placeholder: AppStrings.Engineering.selectValue)
        }
        .buttonStyle(.plain)
    }

    // Shared picker-label chrome (text + chevron + chip background).
    private func pickerLabel(_ text: String, placeholder: String) -> some View {
        HStack {
            Text(text.isEmpty ? placeholder : text)
                .font(.subheadline)
                .foregroundColor(text.isEmpty ? .secondary : .primary)
            Spacer()
            Image(systemName: "chevron.up.chevron.down")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Color(.systemGray5))
        .cornerRadius(8)
        .contentShape(Rectangle())
    }

    // ── i²t On/Off toggle (CSEGMISQUARETDELAY) ──────────────────
    private var isqtToggle: some View {
        HStack(spacing: 8) {
            Text("i²t")
                .font(.caption)
                .foregroundColor(.secondary)
            Picker("", selection: Binding(
                get: { isqtMode },
                set: { newMode in
                    let newSuffix = newMode == "on" ? "(I^s T On)" : "(I^s T Off)"
                    onChange(selectedValue, newSuffix)
                }
            )) {
                Text(AppStrings.Engineering.on).tag("on")
                Text(AppStrings.Engineering.off).tag("off")
            }
            .pickerStyle(.segmented)
            .frame(maxWidth: 160)
            .disabled(segment.isqt_locked)
            if segment.isqt_locked {
                Text(AppStrings.Engineering.locked)
                    .font(.caption2)
                    .italic()
                    .foregroundColor(.secondary)
            }
            Spacer()
        }
    }

    // ── Inst-bands compound editor: Setting1 (primary) + dial ───────
    // ZP-2508: two pickers. Setting1 = setting_labels (with the LO/HI/{n}X
    // display rule); the dial = dial_labels. Stored as {value: Setting1,
    // suffix: dial}; the backend exports `{value}\,{suffix}` (e.g.
    // 0.7\,Fixed, Fixed\,B-A).
    private var instBandsCompoundEditor: some View {
        HStack(spacing: 8) {
            Menu {
                ForEach(settingChoices.indices, id: \.self) { i in
                    Button(settingChoices[i].display) {
                        onChange(settingChoices[i].value, selectedSuffix)
                    }
                }
            } label: {
                pickerLabel(displayForValue(selectedValue), placeholder: AppStrings.Engineering.selectValue)
            }
            .buttonStyle(.plain)
            Text(",")
                .font(.caption)
                .foregroundColor(.secondary)
            Menu {
                ForEach(segment.dial_labels, id: \.self) { dial in
                    // Setting1 (value) is preserved; the dial is the suffix.
                    Button(dial) { onChange(selectedValue, dial) }
                }
            } label: {
                pickerLabel(selectedSuffix, placeholder: AppStrings.Engineering.dial)
            }
            .buttonStyle(.plain)
        }
    }
}
