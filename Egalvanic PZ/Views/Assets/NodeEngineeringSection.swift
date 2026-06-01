//
//  NodeEngineeringSection.swift
//  Egalvanic PZ
//
//  ZP-2161 — read-only mirror of the web AssetFormFields Engineering
//  section. Conditional sub-blocks gated on the node class's
//  ``device_role_id`` + ``eqp_lib_type_name`` + ``primary_secondary_voltage``
//  + ``box`` flags. Values rendered as labels resolved from the locally-
//  synced enum tables in ``EngineeringEnums.swift``.
//
//  Phase 2 is read-only; Phase 3 will swap these into pickers and wire
//  edits through ``NodeService.updateNode``.
//
import SwiftUI
import SwiftData

struct NodeEngineeringSection: View {
    let node: NodeV2
    let nodeClass: NodeClass?

    // Subtype picker bindings — moved here from Basic Information.
    @Binding var selectedNodeSubtype: NodeSubtype?
    let filteredNodeSubtypes: [NodeSubtype]

    // Voltage picker bindings — moved here from the standalone Voltage
    // card. The pickers themselves come from VoltagePickerField.
    @Binding var draftVoltage: Double?
    @Binding var draftVoltageId: Int?
    @Binding var draftSecondaryVoltage: Double?
    @Binding var draftSecondaryVoltageId: Int?

    // ZP-2161 Phase 3b: editable engineering field bundle — every
    // first-class spec the asset form lets the user touch.
    @Binding var draft: EngineeringDraft

    // Core attributes (rendered below the first-class engineering
    // blocks, in the same card).
    @Binding var draftCoreAttributes: [UUID: String]

    // ZP-2161 Phase 4B-3: bound library selection. Owned by the
    // parent (EditNodeDetailView) so the existing save path
    // (NodeService.updateNode + NodeUpdateData.eqpLibSelection)
    // persists what the matcher writes. Reading + writing through
    // this Binding keeps everything in sync.
    @Binding var eqpLibSelection: EqpLibSelection?

    let nameplatePhotos: [Photo]
    let onCoreAttrExtractionComplete: ([UUID: String]) -> Void

    @Query(sort: \EnumNodeVoltage.sort_order) private var voltages: [EnumNodeVoltage]
    @Query(sort: \EnumNodeMainsType.sort_order) private var mainsTypes: [EnumNodeMainsType]
    @Query(sort: \EnumNodePhaseConfiguration.sort_order) private var phaseConfigs: [EnumNodePhaseConfiguration]
    @Query(sort: \EnumNodeTripType.sort_order) private var tripTypes: [EnumNodeTripType]
    @Query(sort: \EnumSkmManufacturer.name) private var manufacturers: [EnumSkmManufacturer]
    @Query(sort: \EnumCableSize.sort_order) private var cableSizes: [EnumCableSize]
    @Query(sort: \EnumCableConductorConfiguration.sort_order) private var conductorConfigs: [EnumCableConductorConfiguration]
    @Query(sort: \EnumCableConductorDescription.sort_order) private var conductorDescs: [EnumCableConductorDescription]
    @Query(sort: \EnumCableDuctMaterial.sort_order) private var ductMaterials: [EnumCableDuctMaterial]
    @Query(sort: \EnumCableInsulationClass.sort_order) private var insulationClasses: [EnumCableInsulationClass]
    @Query(sort: \EnumCableInsulationType.sort_order) private var insulationTypes: [EnumCableInsulationType]
    @Query(sort: \EnumCableInstallation.sort_order) private var installations: [EnumCableInstallation]
    @Query(sort: \EnumBuswayAmpereRating.sort_order) private var buswayAmps: [EnumBuswayAmpereRating]

    // ZP-2161 Phase 4a — SKM headers + routing for Engineering filters.
    @Query private var devLibRouting: [EgDevLibRouting]
    @Query private var skmDevices: [SkmDeviceHeader]
    @Query private var skmTransformerModels: [SkmTransformerModelHeader]

    // ZP-2161 Phase 4b — matcher needs the modelContext to query the
    // local SKM deep tree.
    @Environment(\.modelContext) private var modelContext

    /// Free-text search bar inside the match-results panel. Filters
    /// matches by sz_type / sz_description / sz_catalog (case-insensitive
    /// contains), mirroring web's ``matchSearch`` axis.
    @State private var matchSearchText: String = ""

    /// ZP-2267: present-state for the Add Custom sheet. Single
    /// boolean covers both add and edit paths — the sheet pulls its
    /// context (class + trip type + existing selection) from current
    /// state at present time.
    @State private var presentingCustomSheet: Bool = false

    // ── Gating ─────────────────────────────────────────────────────

    private var showTransformer: Bool { nodeClass?.primary_secondary_voltage == true }
    private var showBoxLevel: Bool { nodeClass?.box == true }
    private var showOcp: Bool { nodeClass?.device_role_id == 2 }
    private var libTypeName: String? { nodeClass?.eqp_lib_type_name }
    private var showCable: Bool { libTypeName == "cable" }
    private var showBusway: Bool { libTypeName == "busway" }
    private var hasAny: Bool { showTransformer || showBoxLevel || showOcp || showCable || showBusway }

    // ── ZP-2161 Phase 4a filters ───────────────────────────────────

    /// Subtypes the user can actually pick for this node: scoped to the
    /// passed-in ``filteredNodeSubtypes`` (already filtered by the
    /// active node class), then further narrowed by the node's
    /// current voltage / ampere_rating fitting within each subtype's
    /// volt/amp floor/ceiling. A nil bound = unbounded on that side;
    /// a nil node value = skip that check entirely.
    private var voltageAmpereEligibleSubtypes: [NodeSubtype] {
        filteredNodeSubtypes.filter { subtype in
            // Voltage range check
            if let v = node.voltage {
                if let floor = subtype.volt_floor, v < floor { return false }
                if let ceiling = subtype.volt_ceiling, v > ceiling { return false }
            }
            // Ampere range check
            if let a = draft.ampere_rating.map(Double.init) {
                if let floor = subtype.amp_floor, a < floor { return false }
                if let ceiling = subtype.amp_ceiling, a > ceiling { return false }
            }
            return true
        }
    }

    /// Trip-type options allowed under the selected subtype, derived
    /// from ``eg_dev_lib_routing``. When no routing rule exists for
    /// the current (eqp_lib_type, subtype) pair, we fall back to the
    /// full trip_types list so the picker isn't accidentally empty.
    private var eligibleTripTypes: [EnumNodeTripType] {
        // ZP-2161: "Ground Fault" is not a standalone trip type users
        // should pick — it's a modifier on top of an LSI breaker. Hide
        // it across the board so the engineering picker only offers
        // pickable trip-type primaries.
        let pickable = tripTypes.filter { $0.display_name != "Ground Fault" }
        guard let typeId = nodeClass?.eqp_lib_type_id else { return pickable }
        let subtypeId = selectedNodeSubtype?.eqp_lib_subtype_id
        let allowed = devLibRouting
            .filter { rule in
                guard rule.is_active && rule.eqp_lib_type_id == typeId else { return false }
                // No subtype picked → union of all trip types under this
                // eqp_lib_type (any rule applies). So a fuse class shows
                // only fuse trip types, never "Oil Filled" etc.
                guard let subtypeId else { return true }
                // Subtype picked → narrow to that subtype, plus any
                // type-wide rules (rule.subtype is nil).
                return rule.eqp_lib_subtype_id == subtypeId
                    || rule.eqp_lib_subtype_id == nil
            }
            .flatMap { $0.allowed_trip_type_ids }
        let allowedSet = Set(allowed)
        guard !allowedSet.isEmpty else { return pickable }
        return pickable.filter { allowedSet.contains($0.id) }
    }

    /// Manufacturers that have at least one SKM device matching the
    /// server's join. Mirrors web's ``/skm-library/manufacturers`` API
    /// precedence: ``eqp_lib_type_id`` always applies, then EXACTLY ONE
    /// additional axis in priority order — the single selected
    /// ``trip_type_id`` if picked, else the routing-engine's allowed
    /// ``trip_type_ids`` set, else ``eqp_lib_subtype_id`` as a last
    /// resort. Subtype and trip are never combined: SKM devices are keyed
    /// on trip type, not our subtype taxonomy, so an AND would filter out
    /// every device and collapse the dropdown to "none".
    ///
    /// ZP-2457: previously this filtered only by subtype + trip and fell
    /// back to the full manufacturer list when the join was empty, which
    /// let users pick manufacturers the server would reject.
    ///
    /// Transformers use a different library table
    /// (``skm_transformer_models``) — for those, route through
    /// ``eligibleTransformerManufacturers`` instead. Web hits separate
    /// endpoints (``/skm-library/manufacturers`` vs
    /// ``/skm-transformer-library/manufacturers``) for the same reason.
    private var eligibleManufacturers: [EnumSkmManufacturer] {
        if libTypeName == "transformer" {
            return eligibleTransformerManufacturers
        }
        let typeId = nodeClass?.eqp_lib_type_id
        let subtypeId = selectedNodeSubtype?.eqp_lib_subtype_id
        let tripId = draft.trip_type_id
        // Allowed trip-type set from the routing engine, used when no
        // single trip type is picked yet (mirrors web's `trip_type_ids[]`).
        let allowedTripIds = Set(eligibleTripTypes.map(\.id))

        // Mirror web's `/skm-library/manufacturers` precedence (ZP-2210):
        // `eqp_lib_type_id` always applies, then EXACTLY ONE additional
        // narrowing axis — never subtype AND trip together. SKM devices are
        // keyed on trip type, not our subtype taxonomy, so combining the two
        // filters out every device (e.g. subtype=ICCB) and the dropdown
        // collapses to "none". Subtype is a last-resort fallback only.
        let matching = skmDevices.filter { d in
            if let typeId, d.eqp_lib_type_id != typeId { return false }
            if let tripId {
                return d.trip_type_id == tripId
            }
            if !allowedTripIds.isEmpty {
                guard let t = d.trip_type_id else { return false }
                return allowedTripIds.contains(t)
            }
            if let subtypeId {
                return d.eqp_lib_subtype_id == subtypeId
            }
            return true
        }
        let mfrIds = Set(matching.compactMap(\.manufacturer_id))
        return manufacturers.filter { mfrIds.contains($0.id) }
    }

    /// Manufacturers drawn from the SKM transformer-models table.
    /// Mirrors web's ``/skm-transformer-library/manufacturers`` endpoint.
    private var eligibleTransformerManufacturers: [EnumSkmManufacturer] {
        let mfrIds = Set(skmTransformerModels.compactMap(\.manufacturer_id))
        guard !mfrIds.isEmpty else { return manufacturers }
        return manufacturers.filter { mfrIds.contains($0.id) }
    }

    // ── Lookup helpers ─────────────────────────────────────────────

    private func voltageLabel(_ id: Int?) -> String? {
        id.flatMap { vid in voltages.first(where: { $0.id == vid })?.label }
    }
    private func mainsLabel(_ id: Int?) -> String? {
        id.flatMap { mid in mainsTypes.first(where: { $0.id == mid })?.label }
    }
    private func phaseLabel(_ id: Int?) -> String? {
        id.flatMap { pid in phaseConfigs.first(where: { $0.id == pid })?.label }
    }
    private func tripTypeLabel(_ id: Int?) -> String? {
        id.flatMap { tid in tripTypes.first(where: { $0.id == tid })?.display_name }
    }
    private func manufacturerName(_ id: Int?) -> String? {
        id.flatMap { mid in manufacturers.first(where: { $0.id == mid })?.name }
    }
    private func cableSizeLabel(_ id: Int?) -> String? {
        id.flatMap { sid in cableSizes.first(where: { $0.id == sid })?.name }
    }
    private func conductorConfigLabel(_ id: Int?) -> String? {
        id.flatMap { cid in conductorConfigs.first(where: { $0.id == cid })?.name }
    }
    private func conductorDescLabel(_ id: Int?) -> String? {
        id.flatMap { cid in conductorDescs.first(where: { $0.id == cid })?.name }
    }
    private func ductMaterialLabel(_ id: Int?) -> String? {
        id.flatMap { did in ductMaterials.first(where: { $0.id == did })?.name }
    }
    private func insulationClassLabel(_ id: Int?) -> String? {
        id.flatMap { iid in insulationClasses.first(where: { $0.id == iid })?.name }
    }
    private func insulationTypeLabel(_ id: Int?) -> String? {
        id.flatMap { iid in insulationTypes.first(where: { $0.id == iid })?.name }
    }
    private func installationLabel(_ id: Int?) -> String? {
        id.flatMap { iid in installations.first(where: { $0.id == iid })?.name }
    }

    // ── Body ───────────────────────────────────────────────────────

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Header — matches the prior EquipmentLibrarySection card so
            // the asset form keeps a consistent "engineering" identity.
            Label(AppStrings.AssetsExtra.engineeringCategory, systemImage: "bolt.shield")
                .font(.headline)
                .foregroundColor(.primary)

            // ZP-2161 Phase 4B-3: bound-library summary. Sits at the
            // top of the card when a library row is bound; the rest
            // of the engineering inputs lock until the user taps
            // Unbind. Hidden when no eqp_lib is set.
            boundLibraryCard

            // ZP-2161 Phase 4B-3 / 4B-4: when a library row is bound,
            // hide the entire engineering input stack — the bound
            // card + the dedicated configurator cards are the only
            // things the user sees until they Unlink.
            if isLibraryBound {
                // ZP-2267: custom entries don't bind to a real SKM
                // library row, so the configurator cards (which read
                // skm_oid / kva_entry_id / etc.) have nothing to
                // surface. The bound-card alone is enough; the user
                // taps Edit to refine the custom payload.
                if eqpLibSelection?.isCustom != true {
                    TransformerConfigCard(
                        eqpLibSelection: $eqpLibSelection,
                        draft: $draft
                    )
                    CableConfigCard(eqpLibSelection: $eqpLibSelection)
                    SkmTripConfigCard(
                        eqpLibSelection: $eqpLibSelection,
                        draft: $draft
                    )
                }
            } else {
                engineeringInputs
            }
        }
        .sheet(isPresented: $presentingCustomSheet) {
            CustomEqpLibSheet(
                eqpLibTypeName: libTypeName ?? "",
                tripTypeSlug: selectedTripTypeSlug,
                tripTypeHasTripUnit: selectedTripTypeHasTripUnit,
                prefilledManufacturer: manufacturerName(draft.manufacturer_id) ?? "",
                existingSelection: (eqpLibSelection?.isCustom == true) ? eqpLibSelection : nil,
                onSave: { applyCustomSelection($0) }
            )
        }
    }

    /// Trip-type slug for the current draft selection — drives the
    /// custom sheet's SST vs GF vs free-form branch.
    private var selectedTripTypeSlug: String? {
        guard let tid = draft.trip_type_id else { return nil }
        return tripTypes.first(where: { $0.id == tid })?.slug
    }

    /// Trip-type ``has_trip_unit`` flag — gates SST layout in the
    /// custom sheet.
    private var selectedTripTypeHasTripUnit: Bool {
        guard let tid = draft.trip_type_id else { return false }
        return tripTypes.first(where: { $0.id == tid })?.has_trip_unit ?? false
    }

    /// Slug allowlist for the frame → sensor → plug matcher path.
    /// Any trip type whose SKM devices carry ``skm_sensors`` rows routes
    /// here; everyone else matches on frame / trip-unit.
    ///
    /// This mirrors the web's match request (``AssetFormFields.jsx``),
    /// which sends ``sensor_amps`` + ``plug_amps`` whenever the trip type
    /// has ``has_trip_unit == true`` and lets the backend narrow on the
    /// sensor+plug join — and the engineering UI here gates the Sensor /
    /// Plug fields on the same ``has_trip_unit`` flag (see
    /// ``engineeringInputs``). The matcher MUST consume the same axes it
    /// asks the user to fill in.
    ///
    /// ZP-2478: ``mcp`` (Motor Circuit Protector) deliberately does NOT
    /// belong here. Although its devices carry ``skm_sensors`` rows, the
    /// web matches MCP on the frame amp tier — not on a sensor+plug join.
    /// Two MA trip-range variants on the same 250A frame (e.g. ABB XT4
    /// "25-50A" and "80-250A") are both valid 250A-frame matches even
    /// though only one has a sensor at the picked rating, so an exact
    /// sensor+plug join returns 0. MCP routes through ``isMcpTripSelection``
    /// → the matcher's ``is_mcp`` (frame-amps) path instead. Keep this set
    /// in sync with the backend's true-SST allowlist.
    private static let sstTripSlugs: Set<String> = [
        "static_trip", "power_circuit", "ground_fault",
        "hvmv_with_trip", "dc_breaker_with_trip"
    ]

    /// Whether the current trip-type selection is a true SST trip.
    /// Drives the matcher's ``is_sst_trip`` routing axis.
    private var isSstTripSelection: Bool {
        guard let slug = selectedTripTypeSlug else { return false }
        return Self.sstTripSlugs.contains(slug)
    }

    /// ZP-2478: whether the current trip type is a Motor Circuit
    /// Protector. Drives the matcher's ``is_mcp`` (frame-amps) path.
    private var isMcpTripSelection: Bool {
        selectedTripTypeSlug == "mcp"
    }

    /// ZP-2267: apply a custom-built selection to the parent binding +
    /// backfill first-class draft fields where the data overlaps.
    /// Mirrors the relevant parts of ``applyPickedMatch`` for the
    /// custom path. Idempotent — re-saving the sheet with edits just
    /// overwrites the binding + replays the backfill.
    ///
    /// Dirty/sync wiring: this binding write propagates up to the
    /// parent's authoritative state — ``AddAssetViewModel.draftEqpLibSelection``
    /// (@Published, picked up at ``performSave`` line 492) or
    /// ``EditNodeDetailView.draftEqpLibSelection`` (@State, which
    /// drives ``hasUnsavedChanges`` via the != comparison on line 337).
    /// No explicit dirty-marking call is needed; the binding IS the
    /// dirty channel, same as ``applyPickedMatch``.
    private func applyCustomSelection(_ sel: EqpLibSelection) {
        eqpLibSelection = sel

        // Protective: mirror cont_current onto ampere_rating so the
        // dashboard / search picks it up. Frontend does the same in
        // its CustomLibraryDialog onSave handler.
        if sel.isCustomProtective, let cc = sel.cont_current {
            draft.ampere_rating = Int(cc.rounded())
        }
        // Transformer: kVA is the only first-class draft field that
        // overlaps with the custom payload; voltages stay on the
        // node's primary/secondary voltage pickers (separate UX).
        if sel.isCustomTransformer, let kva = sel.kva_rating {
            draft.kva_rating = kva
        }
        // Busway: mirror cable.cable_size (typed as the amp rating
        // for busways) onto busway_ampere_rating — same as the
        // matched-pick path does — so SQL / dashboard filters that
        // query this column find the asset.
        //
        // ZP-2421 review #5: strip non-numeric / non-decimal chars
        // before parsing so paste-ins like "1200A" or "1200 A" still
        // mirror correctly. The .decimalPad keyboard prevents typed
        // junk on iOS, but pasted text can carry units.
        if sel.category == "custom-busway",
           let raw = sel.cable?.cable_size {
            let cleaned = raw.filter { $0.isNumber || $0 == "." }
            if let dval = Double(cleaned) {
                draft.busway_ampere_rating = Int(dval.rounded())
            }
        }
        // Cable: the iOS draft uses enum IDs (cable_size_id,
        // duct_material_id, etc.) which don't map cleanly to the
        // free-text custom payload — we keep those untouched and let
        // the bound-card surface cable.* fields directly.
    }

    /// All editable engineering inputs in their original order. Pulled
    /// out so the bound-library lock can ``.disabled(...)`` the whole
    /// group in one shot.
    @ViewBuilder private var engineeringInputs: some View {
        VStack(alignment: .leading, spacing: 14) {
            // 1. Subtype — the underlying field is optional but we
            // surface it without an "(optional)" suffix per design.
            // Filtered by the node's voltage + ampere_rating against
            // each subtype's volt/amp floor/ceiling (Phase 4a).
            // ZP-2397: hide when a library row is bound — the library
            // item supersedes the subtype. Also hide when the asset
            // class declares no subtypes at all (mirrors web's
            // ``nodeSubtypes.length > 0`` gate).
            if eqpLibSelection == nil && !filteredNodeSubtypes.isEmpty {
                ModernPicker(
                    title: AppStrings.Engineering.subtype,
                    icon: "tag",
                    placeholder: AppStrings.Assets.selectAssetSubtype,
                    items: voltageAmpereEligibleSubtypes,
                    selection: $selectedNodeSubtype,
                    displayName: { $0.name },
                    onSelectionChange: { _ in },
                    useSheet: true
                )
                .disabled(nodeClass == nil)
            }

            // 2. Voltage(s) — primary/secondary stack when the class is
            // transformer-shaped (``primary_secondary_voltage``), single
            // picker otherwise.
            // ZP-2397: hide when a library row is bound (library pins
            // the voltage rating) or when the class is cable/busway
            // (those carry whatever bus they're tapped into — no system
            // voltage of their own).
            if eqpLibSelection == nil && !showCable && !showBusway {
                if nodeClass?.primary_secondary_voltage == true {
                    HStack(spacing: 12) {
                        VoltagePickerField(
                            label: AppStrings.AssetsExtra.primaryVoltage,
                            selectedId: $draftVoltageId,
                            selectedValue: $draftVoltage
                        )
                        VoltagePickerField(
                            label: AppStrings.AssetsExtra.secondaryVoltage,
                            selectedId: $draftSecondaryVoltageId,
                            selectedValue: $draftSecondaryVoltage
                        )
                    }
                } else {
                    VoltagePickerField(
                        label: AppStrings.AssetsExtra.voltage,
                        selectedId: $draftVoltageId,
                        selectedValue: $draftVoltage
                    )
                }
            }

            // 3. First-class engineering blocks (read-only in Phase 2 —
            // editing arrives in Phase 3b).
            if hasAny {
                if showTransformer { transformerBlock }
                if showBoxLevel { boxBlock }
                if showOcp { ocpBlock }
                if showCable { cableBlock }
                if showBusway { buswayBlock }
            }

            // 4. Custom attributes — below the first-class engineering
            // rows. Editable; uses the existing component verbatim.
            // Auto-hides when the node class declares no properties.
            // Divider visually separates the standard engineering
            // fields above from the class-defined custom attributes
            // below, since both live in the same Engineering card.
            if !(nodeClass?.definition.isEmpty ?? true) {
                Divider()
                    .padding(.top, 8)
                NodeCoreAttributesSection(
                    node: node,
                    selectedNodeClass: nodeClass,
                    draftAttributes: $draftCoreAttributes,
                    nameplatePhotos: nameplatePhotos,
                    onExtractionComplete: onCoreAttrExtractionComplete
                )
            }
        }
        // ZP-2420 PR review #8: shared search-text debounce. Mounted
        // once at the parent so all three match panels (protective /
        // cable-busway / transformer) drive ``debouncedSearch`` through
        // the same task. Previously each panel attached its own
        // identical ``.task(id: matchSearchText)``, which was redundant
        // since at most one panel is visible at a time anyway.
        .task(id: matchSearchText) {
            do {
                try await Task.sleep(nanoseconds: 250_000_000)
                debouncedSearch = matchSearchText
            } catch {
                // Cancelled — a newer keystroke arrived.
            }
        }
    }

    // ── Sub-blocks ─────────────────────────────────────────────────

    @ViewBuilder private var transformerBlock: some View {
        EngineeringBlock(title: "") {
            if nodeClass?.tertiary_voltage == true {
                VoltagePickerField(
                    label: AppStrings.Engineering.tertiaryVoltage,
                    selectedId: $draft.tertiary_voltage_id,
                    selectedValue: $draft.tertiary_voltage
                )
            }
            EngineeringDoubleField(label: AppStrings.Engineering.kvaRating, value: $draft.kva_rating, suffix: "kVA")
            EngineeringDoubleField(label: AppStrings.Engineering.percentImpedance, value: $draft.percent_impedance, suffix: "%")
            TripTypePicker(
                label: AppStrings.Engineering.type,
                items: eligibleTripTypes,
                selection: tripTypeBinding
            )
            EngineeringEnumPicker(
                label: AppStrings.Engineering.manufacturer,
                items: eligibleManufacturers,
                selectedId: $draft.manufacturer_id,
                idOf: { $0.id },
                labelOf: { $0.name }
            )
            // ZP-2420: SKM transformer library matches surface here
            // once the user has picked a manufacturer. Network-backed
            // (mirrors web's /skm-transformer-library/match call).
            transformerMatchResultsPanel
        }
    }

    @ViewBuilder private var boxBlock: some View {
        EngineeringBlock(title: "") {
            EngineeringEnumPicker(
                label: AppStrings.Engineering.mainsType,
                items: mainsTypes,
                selectedId: $draft.mains_type_id,
                idOf: { $0.id },
                labelOf: { $0.label }
            )
            EngineeringEnumPicker(
                label: AppStrings.Engineering.phaseConfiguration,
                items: phaseConfigs,
                selectedId: $draft.phase_configuration_id,
                idOf: { $0.id },
                labelOf: { $0.label }
            )
            if node.circuit_voltage_id != nil {
                // Circuit voltage is computed by the backend's voltage
                // propagation pass; surface as read-only.
                EngineeringRow(label: AppStrings.Engineering.circuitVoltage, value: voltageLabel(node.circuit_voltage_id))
            }
        }
    }

    @ViewBuilder private var ocpBlock: some View {
        EngineeringBlock(title: "") {
            switch libTypeName {
            case "fuse":
                // Fuse — "Pole Count" reads as "Fuse Count",
                // "Ampere Rating" as "Fuse Amperage". No frame /
                // sensor / plug.
                PoleCountPicker(label: AppStrings.Engineering.fuseCount, value: $draft.pole_count, style: .count)
                TripTypePicker(
                    label: AppStrings.Engineering.type,
                    items: eligibleTripTypes,
                    selection: tripTypeBinding
                )
                EngineeringEnumPicker(
                    label: AppStrings.Engineering.manufacturer,
                    items: eligibleManufacturers,
                    selectedId: $draft.manufacturer_id,
                    idOf: { $0.id },
                    labelOf: { $0.name }
                )
                EngineeringIntField(label: AppStrings.Engineering.fuseAmperage, value: $draft.ampere_rating, suffix: "A", icon: "bolt.horizontal")

            case "relay":
                // Relay — just Type + Manufacturer.
                TripTypePicker(
                    label: AppStrings.Engineering.type,
                    items: eligibleTripTypes,
                    selection: tripTypeBinding
                )
                EngineeringEnumPicker(
                    label: AppStrings.Engineering.manufacturer,
                    items: eligibleManufacturers,
                    selectedId: $draft.manufacturer_id,
                    idOf: { $0.id },
                    labelOf: { $0.name }
                )

            default:
                // Circuit breakers + anything else under device_role=2.
                // Order: Pole Count, Type, Manufacturer, Frame Amps,
                // Trip Amps (ampere_rating), Sensor Amps, Plug Amps.
                PoleCountPicker(label: AppStrings.Engineering.poleCount, value: $draft.pole_count, style: .pole)
                TripTypePicker(
                    label: AppStrings.Engineering.type,
                    items: eligibleTripTypes,
                    selection: tripTypeBinding
                )
                EngineeringEnumPicker(
                    label: AppStrings.Engineering.manufacturer,
                    items: eligibleManufacturers,
                    selectedId: $draft.manufacturer_id,
                    idOf: { $0.id },
                    labelOf: { $0.name }
                )
                if libTypeName == "circuit_breaker" {
                    EngineeringIntField(label: AppStrings.Engineering.frameAmps, value: $draft.frame_amps, suffix: "A", icon: "bolt.horizontal")
                    // Mutually exclusive: a breaker either reports Trip
                    // Amps directly (fixed-trip) OR a Sensor + Plug pair
                    // (trip-unit). Drive the split off the selected trip
                    // type's ``has_trip_unit`` flag — matches web.
                    //
                    // Perf: ``.animation(nil, value:)`` suppresses the
                    // implicit show/hide animation that ``ModernSegmentedPicker``
                    // would otherwise propagate from its button-tap
                    // ``withAnimation`` block. Destroying / recreating
                    // two ``ModernTextField``s under animation drops
                    // frames noticeably; skipping the transition keeps
                    // the segment swap responsive.
                    Group {
                        if draft.has_trip_unit == true {
                            EngineeringIntField(label: AppStrings.Engineering.sensorAmps, value: $draft.sensor_amps, suffix: "A", icon: "bolt.horizontal")
                            EngineeringIntField(label: AppStrings.Engineering.plugAmps, value: $draft.plug_amps, suffix: "A", icon: "bolt.horizontal")
                        } else {
                            EngineeringIntField(label: AppStrings.Engineering.tripAmps, value: $draft.ampere_rating, suffix: "A", icon: "bolt.horizontal")
                        }
                    }
                    .animation(nil, value: draft.has_trip_unit)
                } else {
                    EngineeringIntField(label: AppStrings.Engineering.ampereRating, value: $draft.ampere_rating, suffix: "A", icon: "bolt.horizontal")
                }
            }
            // ZP-2161 Phase 4b: live match results from the on-device
            // SKM library. Empty array → empty panel hidden; non-empty
            // renders below the OCP inputs.
            matchResultsPanel
        }
    }

    // MARK: - Bound library card (Phase 4B-3)

    /// True when a library row is currently bound. Reads through the
    /// parent's binding so the engineering UI and the save path
    /// (NodeUpdateData.eqpLibSelection) stay in sync.
    private var isLibraryBound: Bool {
        eqpLibSelection != nil
    }

    @ViewBuilder private var boundLibraryCard: some View {
        if let sel = eqpLibSelection {
            // ZP-2267: visual accent flips to orange for custom
            // entries so the user can tell at a glance that they're
            // looking at a one-shot they typed in vs a real library
            // row. Header text + seal icon mirror.
            let accent: Color = sel.isCustom ? .orange : .green
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: sel.isCustom ? "pencil.circle.fill" : "checkmark.seal.fill")
                    .font(.title3)
                    .foregroundColor(accent)
                VStack(alignment: .leading, spacing: 6) {
                    Text(sel.isCustom
                         ? AppStrings.Engineering.customEntry
                         : AppStrings.Engineering.libraryMatched)
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(accent)
                        .textCase(.uppercase)
                        .tracking(0.5)

                    // Body content varies by category — cable / busway
                    // and transformer use the web-style two-line
                    // (header · attrs) layout; protective devices keep
                    // their original manufacturer/type/style summary
                    // since their data shape differs. ZP-2267: custom-*
                    // categories route through the same per-shape body.
                    boundCardBody(for: sel)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 6) {
                    if sel.isCustom {
                        Button {
                            presentingCustomSheet = true
                        } label: {
                            Text(AppStrings.Engineering.edit)
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(.blue)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(Color.blue.opacity(0.08))
                                .cornerRadius(8)
                        }
                        .buttonStyle(.plain)
                    }
                    Button {
                        unbindLibrary()
                    } label: {
                        Text(AppStrings.AssetsExtra.unlink)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.red)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color.red.opacity(0.08))
                            .cornerRadius(8)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(12)
            .background(accent.opacity(0.06))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(accent.opacity(0.25), lineWidth: 1)
            )
            .cornerRadius(12)
        }
    }

    /// Per-category body for the bound-library card.
    @ViewBuilder
    private func boundCardBody(for sel: EqpLibSelection) -> some View {
        let category = sel.category ?? ""
        // ZP-2267: custom-cable / custom-busway / custom-transformer
        // route through the same per-shape body since EqpLibSelection.makeCustom
        // mirrors the user's inputs onto the top-level cable / transformer
        // fields the body readers already use.
        let isCableOrBusway = (
            category == "cables-skm" ||
            category == "busway-skm" ||
            sel.isCustomCableOrBusway
        )
        let isTransformerSel = (
            category == "transformers-skm" ||
            sel.isCustomTransformer
        )

        if isCableOrBusway {
            boundCableBuswayContent(sel)
        } else if isTransformerSel {
            boundTransformerContent(sel)
        } else {
            // Protective devices: manufacturer / type / style + amp
            // subtitle. ``displaySummary`` already appends amp from
            // ``cont_current`` when set, so the below-summary line
            // is only useful when it adds NEW information (sensor /
            // plug breakdown for SST, or draft.ampere_rating when
            // cont_current isn't on the selection).
            Text(sel.displaySummary)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.primary)
            if sel.isCustom {
                // ZP-2267: custom protective — displaySummary already
                // covers the Amps the user typed (via cont_current).
                // Below it, list each trip setting on its own row with
                // label · value · (suffix) — mirrors web's bound-card
                // SETTINGS section.
                if let settings = sel.settings, !settings.isEmpty {
                    customSettingsList(settings)
                }
            } else {
                // Matched entry — keep the existing amp / sensor /
                // plug breakdown. Only fires when cont_current isn't
                // already in the summary, or when SST surfaces sensor
                // / plug values.
                if draft.has_trip_unit == true,
                   let sa = draft.sensor_amps, let pa = draft.plug_amps {
                    Text("\(sa)AS / \(pa)AP")
                        .font(.caption)
                        .foregroundColor(.secondary)
                } else if sel.cont_current == nil,
                          let amp = draft.ampere_rating {
                    Text("\(amp)A")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
    }

    /// ZP-2267: trip-settings list for the custom protective bound
    /// card. Mirrors web's "SETTINGS" section — one row per setting
    /// with bold label, value, and optional ``(suffix)`` tail. Empty
    /// label/value rows are dropped at save time so this never has
    /// to filter.
    @ViewBuilder
    private func customSettingsList(_ settings: [EqpLibSelection.CustomSetting]) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            // ZP-2267: thin separator above the SETTINGS header to
            // visually divide identity (mfr/type/style/amps) from the
            // trip-settings list — mirrors web's bound-card divider.
            Divider()
                .padding(.top, 6)
            Text(AppStrings.Engineering.settings)
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
                .textCase(.uppercase)
                .tracking(0.5)
                .padding(.top, 2)
            ForEach(settings.indices, id: \.self) { idx in
                let row = settings[idx]
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(row.label)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                    Text(row.value)
                        .font(.caption)
                        .foregroundColor(.primary)
                    if !row.suffix.isEmpty {
                        // ZP-2267: I²t suffix values ship pre-wrapped
                        // (e.g. ``(I^s T On)``) to match web's wire
                        // format; dial values (R1..R5) ship bare. Wrap
                        // only when not already wrapped — preserves
                        // any wire-supplied parens losslessly while
                        // still wrapping bare values for visual parity.
                        let s = row.suffix
                        let display = (s.hasPrefix("(") && s.hasSuffix(")"))
                            ? s
                            : "(\(s))"
                        Text(display)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    Spacer(minLength: 0)
                }
            }
        }
    }

    /// Cable / busway bound-card content. Web parity:
    ///   line 1: "MANUFACTURER · 600 A · @600V"
    ///   line 2: "Copper · Busway · Sandwich · Class B · Epoxy"
    @ViewBuilder
    private func boundCableBuswayContent(_ sel: EqpLibSelection) -> some View {
        HStack(spacing: 6) {
            Text((sel.manufacturer?.isEmpty == false) ? sel.manufacturer! : "—")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.primary)
                .lineLimit(1)
            // ZP-2267: resolvedCableSize falls back to ``sel.cable.cable_size``
            // for web-saved custom entries that don't mirror to the top-level.
            if let size = sel.resolvedCableSize {
                Text("·")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                let unit = sel.matched_cable_size_unit ?? ""
                Text(unit.isEmpty ? size : "\(size) \(unit)")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                    .lineLimit(1)
            }
            if let v = sel.voltage_rating, v.isFinite, v > 0 {
                Text("@\(Int(v.rounded()))V")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        if let subtitle = boundAttributeSubtitle(sel) {
            Text(subtitle)
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(2)
        }
    }

    /// Transformer bound-card content. ZP-2514 — web parity.
    /// Title: ``manufacturer · str_type · NN kVA``.
    /// Subtitle: ``str_type_symbol · NΦ · R% X.XX · X% Y.YY``.
    @ViewBuilder
    private func boundTransformerContent(_ sel: EqpLibSelection) -> some View {
        HStack(spacing: 6) {
            Text((sel.manufacturer?.isEmpty == false) ? sel.manufacturer! : "—")
                .font(.footnote)
                .fontWeight(.semibold)
                .foregroundColor(.primary)
                .lineLimit(1)
            if let strType = sel.str_type, !strType.isEmpty {
                Text("·")
                    .font(.footnote)
                    .foregroundColor(.secondary)
                Text(strType)
                    .font(.footnote)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            // ZP-2267: resolvedKvaRating falls back to ``sel.transformer.kva``
            // for web-saved custom entries.
            if let kva = sel.resolvedKvaRating {
                Text("·")
                    .font(.footnote)
                    .foregroundColor(.secondary)
                let v = kva.truncatingRemainder(dividingBy: 1) == 0
                    ? "\(Int(kva))"
                    : String(format: "%.1f", kva)
                Text("\(v) kVA")
                    .font(.footnote)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        if let subtitle = boundTransformerSubtitle(sel) {
            Text(subtitle)
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(2)
        }
    }

    /// Dot-joined transformer subtitle — mirrors the match-card
    /// subtitle so the bound state reads the same as the picker row
    /// the user tapped.
    private func boundTransformerSubtitle(_ sel: EqpLibSelection) -> String? {
        var parts: [String] = []
        if let sym = sel.str_type_symbol, !sym.isEmpty { parts.append(sym) }
        if let p = sel.phase, p > 0 { parts.append("\(p)Φ") }
        if let r = sel.percentage_r, let x = sel.percentage_x {
            parts.append(String(format: "R%% %.2f · X%% %.2f", r, x))
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    /// Dot-joined attribute subtitle for cable / busway bound cards.
    /// Drops missing, empty, and placeholder values (``-`` / ``****``).
    /// ZP-2267: uses resolved fields so web-saved custom cable entries
    /// (only nested ``cable.*``) surface their conductor / duct subtitle.
    private func boundAttributeSubtitle(_ sel: EqpLibSelection) -> String? {
        let parts = [
            sel.resolvedConductorType,
            sel.resolvedDuctMaterial,
            sel.installation,
            sel.resolvedInsulationClass,
            sel.insulation_type
        ].compactMap { $0?.trimmingCharacters(in: .whitespaces) }
         .filter { !$0.isEmpty && $0 != "-" && $0 != "****" }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    /// Clears the bound library. First-class fields stay populated so
    /// the user keeps a starting point and can re-narrow / re-pick
    /// without losing their inputs.
    private func unbindLibrary() {
        eqpLibSelection = nil
    }

    // MARK: - Match panel (Phase 4b)

    /// Build matcher inputs from the current draft + class context.
    /// Scoped to protective devices (breaker / fuse / relay) — the
    /// other three classes (cable / busway / transformer) have their
    /// own matcher paths (``cableMatchInputs`` /
    /// ``transformerMatchInputs``) since their library schemas differ.
    private var matchInputs: SkmMatchInputs? {
        guard let typeId = nodeClass?.eqp_lib_type_id,
              let typeName = nodeClass?.eqp_lib_type_name,
              ["circuit_breaker", "fuse", "relay"].contains(typeName)
        else { return nil }
        return SkmMatchInputs(
            eqp_lib_type_id: typeId,
            type_name: typeName,
            eqp_lib_subtype_id: selectedNodeSubtype?.eqp_lib_subtype_id,
            manufacturer_id: draft.manufacturer_id,
            trip_type_id: draft.trip_type_id,
            is_sst_trip: isSstTripSelection,
            is_mcp: isMcpTripSelection,
            pole_count: draft.pole_count,
            sensor_amps: draft.sensor_amps,
            plug_amps: draft.plug_amps,
            trip_amps: draft.ampere_rating,
            frame_amps: draft.frame_amps,
            voltage: node.voltage,
            // ZP-2161 4B-4 perf: debounced version so a keystroke in
            // the search field doesn't re-run the matcher synchronously.
            // ``debouncedSearch`` updates ~250 ms after the user stops
            // typing (see the .task on matchSearchText in matchResultsPanel).
            search: debouncedSearch
        )
    }

    /// Cached matcher results. Driven by ``.task(id: matchInputsKey)``
    /// so a body re-eval from an unrelated draft change doesn't
    /// re-run the matcher. The search-text field has its own debounce
    /// path via a separate task to avoid re-fetching on every keystroke.
    @State private var cachedMatches: SkmMatchResults = SkmMatchResults(matches: [], truncated: false)
    @State private var debouncedSearch: String = ""

    /// ZP-2420: separate caches for cable / busway / transformer
    /// matches. Offline-backed via ``SkmMatcher.matchCableOrBusway``
    /// and ``SkmMatcher.matchTransformer`` — once the user has
    /// downloaded the SKM library from Settings, these query local
    /// ``SkmCableEntry`` (cable + busway, partitioned by
    /// ``duct_material``) / ``SkmTransformerModelHeader`` +
    /// ``SkmTransformerKvaEntry`` rows. Only one of the three is
    /// ever populated at a time (the node class is mutually
    /// exclusive across them).
    @State private var cachedCableMatches: SkmMatchResults = SkmMatchResults(matches: [], truncated: false)
    @State private var cachedTransformerMatches: SkmMatchResults = SkmMatchResults(matches: [], truncated: false)

    /// Compact discriminator for the matcher inputs — used as the
    /// ``.task(id:)`` key. Mirrors every axis SkmMatcher reads except
    /// the search text (which uses ``debouncedSearch``).
    private var matchInputsKey: String {
        guard let inputs = matchInputs else { return "none" }
        // Built stepwise — Swift's type-checker times out on a single
        // 12-element ``[String]`` literal that mixes ``String(Int)``,
        // ``Optional.map(String.init)``, ternaries, and closures.
        var parts: [String] = []
        parts.append(String(inputs.eqp_lib_type_id))
        parts.append(inputs.type_name)
        parts.append(intOrDash(inputs.eqp_lib_subtype_id))
        parts.append(intOrDash(inputs.manufacturer_id))
        parts.append(intOrDash(inputs.trip_type_id))
        parts.append(inputs.is_sst_trip ? "1" : "0")
        parts.append(inputs.is_mcp ? "1" : "0")
        parts.append(intOrDash(inputs.sensor_amps))
        parts.append(intOrDash(inputs.plug_amps))
        parts.append(intOrDash(inputs.trip_amps))
        parts.append(intOrDash(inputs.frame_amps))
        if let v = inputs.voltage {
            parts.append(String(Int(v)))
        } else {
            parts.append("-")
        }
        parts.append(debouncedSearch)
        return parts.joined(separator: "|")
    }

    private func intOrDash(_ v: Int?) -> String {
        guard let v else { return "-" }
        return String(v)
    }

    @ViewBuilder private var matchResultsPanel: some View {
        // ZP-2161 Phase 4b: progressive matching. Driven by a
        // task(id:) so SwiftData fetches happen only on real input
        // changes — not on every body re-eval / keystroke in
        // unrelated fields.
        if let inputs = matchInputs, inputs.manufacturer_id != nil {
            MatchResultsPanel(
                matches: cachedMatches.matches,
                truncated: cachedMatches.truncated,
                typeName: inputs.type_name,
                searchText: $matchSearchText,
                onPick: { match in applyPickedMatch(match) },
                onAddCustom: { presentingCustomSheet = true }
            )
            .padding(.top, 4)
            .task(id: matchInputsKey) {
                // Recompute only when a discriminator actually changed.
                let inputs = matchInputs
                guard let inputs else {
                    cachedMatches = SkmMatchResults(matches: [], truncated: false)
                    return
                }
                cachedMatches = SkmMatcher.match(inputs: inputs, in: modelContext)
            }
            // Note: search-text debounce is shared at the parent
            // (``engineeringInputs``); see PR review #8.
        }
    }

    // MARK: - Cable / busway match (ZP-2420)

    /// Build the BE-bound cable / busway match inputs from the current
    /// draft + class context. Returns nil when the user hasn't yet
    /// picked a ``conductor_material`` (the FE's minimum readiness
    /// gate) or when the class isn't cable / busway. Frontend parity:
    /// ``AssetFormFields.jsx`` ``isCable || isBusway`` branch.
    private var cableMatchInputs: SkmCableMatchInputs? {
        guard let typeId = nodeClass?.eqp_lib_type_id,
              showCable || showBusway,
              let material = draft.conductor_material,
              !material.isEmpty
        else { return nil }
        // Cable: pass the AWG / kcmil label resolved from the FK.
        // Busway: pass the ampere rating as a string (the BE's
        // matcher treats this column polymorphically — Amps for
        // busway rows, AWG/kcmil for cable rows).
        let sizeArg: String? = showBusway
            ? draft.busway_ampere_rating.map { String($0) }
            : cableSizeLabel(draft.cable_size_id)
        return SkmCableMatchInputs(
            eqp_lib_type_id: typeId,
            is_busway: showBusway,
            conductor_material: material,
            cable_size: sizeArg,
            conductor_desc: conductorDescLabel(draft.conductor_description_id),
            duct_material: ductMaterialLabel(draft.duct_material_id),
            insulation_class: insulationClassLabel(draft.insulation_class_id),
            insulation_type: insulationTypeLabel(draft.insulation_type_id),
            installation: installationLabel(draft.installation_id),
            voltage: node.voltage,
            search: debouncedSearch
        )
    }

    private var cableMatchInputsKey: String {
        guard let inputs = cableMatchInputs else { return "none" }
        var parts: [String] = []
        parts.append(inputs.is_busway ? "busway" : "cable")
        parts.append(String(inputs.eqp_lib_type_id))
        parts.append(inputs.conductor_material)
        parts.append(inputs.cable_size ?? "-")
        parts.append(inputs.conductor_desc ?? "-")
        parts.append(inputs.duct_material ?? "-")
        parts.append(inputs.insulation_class ?? "-")
        parts.append(inputs.insulation_type ?? "-")
        parts.append(inputs.installation ?? "-")
        if let v = inputs.voltage {
            parts.append(String(Int(v)))
        } else {
            parts.append("-")
        }
        parts.append(inputs.search)
        return parts.joined(separator: "|")
    }

    @ViewBuilder private var cableMatchResultsPanel: some View {
        if let inputs = cableMatchInputs {
            MatchResultsPanel(
                matches: cachedCableMatches.matches,
                truncated: cachedCableMatches.truncated,
                typeName: inputs.is_busway ? "busway" : "cable",
                searchText: $matchSearchText,
                onPick: { match in applyPickedMatch(match) },
                onAddCustom: { presentingCustomSheet = true }
            )
            .padding(.top, 4)
            .task(id: cableMatchInputsKey) {
                // ZP-2420: queries local SkmCableEntry (partitioned by
                // duct_material) via SkmMatcher. Sync call, same pattern
                // as the existing protective-device matcher.
                guard let inputs = cableMatchInputs else {
                    cachedCableMatches = SkmMatchResults(matches: [], truncated: false)
                    return
                }
                cachedCableMatches = SkmMatcher.matchCableOrBusway(inputs: inputs, in: modelContext)
            }
            // Search-text debounce shared at the parent — see PR review #8.
        }
    }

    // MARK: - Transformer match (ZP-2420)

    /// Build the BE-bound transformer match inputs. Mirrors the FE
    /// readiness gate: ``manufacturer_id`` is the minimum field
    /// required to fire the matcher.
    private var transformerMatchInputs: SkmTransformerMatchInputs? {
        guard let typeId = nodeClass?.eqp_lib_type_id,
              showTransformer,
              let mfrId = draft.manufacturer_id
        else { return nil }
        return SkmTransformerMatchInputs(
            eqp_lib_type_id: typeId,
            manufacturer_id: mfrId,
            trip_type_id: draft.trip_type_id,
            kva: draft.kva_rating,
            voltage: node.voltage,
            search: debouncedSearch
        )
    }

    private var transformerMatchInputsKey: String {
        guard let inputs = transformerMatchInputs else { return "none" }
        var parts: [String] = []
        parts.append(String(inputs.eqp_lib_type_id))
        parts.append(String(inputs.manufacturer_id))
        parts.append(inputs.trip_type_id.map { String($0) } ?? "-")
        if let k = inputs.kva {
            parts.append(String(Int(k)))
        } else {
            parts.append("-")
        }
        if let v = inputs.voltage {
            parts.append(String(Int(v)))
        } else {
            parts.append("-")
        }
        parts.append(inputs.search)
        return parts.joined(separator: "|")
    }

    @ViewBuilder private var transformerMatchResultsPanel: some View {
        if transformerMatchInputs != nil {
            MatchResultsPanel(
                matches: cachedTransformerMatches.matches,
                truncated: cachedTransformerMatches.truncated,
                typeName: "transformer",
                searchText: $matchSearchText,
                onPick: { match in applyPickedMatch(match) },
                onAddCustom: { presentingCustomSheet = true }
            )
            .padding(.top, 4)
            .task(id: transformerMatchInputsKey) {
                // ZP-2420: now offline — queries local
                // SkmTransformerModelHeader + SkmTransformerKvaEntry
                // via SkmMatcher.matchTransformer.
                guard let inputs = transformerMatchInputs else {
                    cachedTransformerMatches = SkmMatchResults(matches: [], truncated: false)
                    return
                }
                cachedTransformerMatches = SkmMatcher.matchTransformer(inputs: inputs, in: modelContext)
            }
            // Search-text debounce shared at the parent — see PR review #8.
        }
    }

    /// ZP-2161 Phase 4B-3: serialize the picked match into ``eqp_lib``
    /// and back-fill any first-class draft fields per device type.
    /// Mirrors web's onClick handler in AssetFormFields.jsx.
    private func applyPickedMatch(_ match: SkmMatch) {
        // 1) Build the SKM-canonical eqp_lib shape: top-level identity
        // fields + a ``skm_settings`` block carrying the editable
        // axes (frame/sensor/plug/trip-unit + segments). Matches the
        // shape production has migrated to.
        var sel = EqpLibSelection()
        sel.category = match.slug
        sel.skm_oid = match.item_id  // = cdevice_oid
        sel.manufacturer = match.manufacturer
        sel.type = match.type
        sel.style = match.style
        sel.cont_current = match.cont_current
        sel.use_sst = match.use_sst

        // Settings block for protective devices. Cable / busway /
        // transformer matches won't populate this — different shape
        // entirely (handled by their own matcher paths).
        let isProtective = ["mccb-breakers", "iccb-breakers", "pcb-breakers",
                            "lv-fuses", "hv-fuses", "dc-fuses",
                            "relays-electronic", "relays-electromechanical",
                            "relays-iec", "hv-breakers"].contains(match.slug)
        var resolved: ResolvedSkm? = nil
        if isProtective {
            // ZP-2161: cascade-resolve frame / sensor / plug / trip
            // unit for the picked device, honoring the user's typed
            // filters. The matcher's match struct only carries the
            // axes it actually resolved at search time (e.g. partial
            // SST has nil sensor/plug). Refetching device-scoped rows
            // here lets us write a fully-populated skm_settings even
            // when the search was partial, so the configurator card
            // opens with sensible pre-selections.
            let r = resolveSkmSelections(for: match)
            resolved = r
            var settings = EqpLibSelection.SkmSettings()
            settings.frame_id = r.frame?.id
            settings.sensor_id = r.sensor?.id
            settings.trip_unit_oid = r.tripUnit?.id
            settings.frame_amps = r.frame?.f_size
            settings.sensor_amps = r.sensor?.f_size
            settings.plug_amps = r.plug
            settings.has_ground_fault = false
            // ``segment_selections`` + ``included_segment_ids`` are
            // populated by the configurator card's segment seeder once
            // the segments load for the resolved trip unit.
            settings.segment_selections = [:]
            settings.included_segment_ids = []
            sel.skm_settings = settings
        }
        // ZP-2420: deferred commit. The original implementation wrote
        // ``eqpLibSelection`` here for protective devices, then again
        // at the end for cable / transformer extensions. That worked
        // but committed a partial selection mid-function for cable /
        // transformer picks (CableConfigCard / TransformerConfigCard
        // would briefly see a `category` without their type-specific
        // payload fields). Single-write at the end is cleaner and
        // race-free.

        // 2) Back-fill the first-class draft columns per device-type
        // bucket. Slug tells us which path the match came from.
        let isBreaker = ["mccb-breakers", "iccb-breakers", "pcb-breakers"]
            .contains(match.slug)
        let isFuse = ["lv-fuses", "hv-fuses", "dc-fuses"].contains(match.slug)
        let isRelay = ["relays-electronic",
                       "relays-electromechanical",
                       "relays-iec"].contains(match.slug)

        if isBreaker {
            // ZP-2161: prefer resolved values over the match's own
            // (which may be nil for partial-search picks). Resolved
            // values reflect the cascade-pick the configurator card
            // will surface, so first-class draft + skm_settings stay
            // in lock-step.
            if let amp = match.matched_trip_amp ?? resolved?.tripUnit?.f_size {
                draft.ampere_rating = Int(amp)
            }
            if let sa = match.matched_sensor_value ?? resolved?.sensor?.f_size {
                draft.sensor_amps = Int(sa)
            }
            if let pa = match.matched_plug_value ?? resolved?.plug {
                draft.plug_amps = Int(pa)
            }
            // Web defaults pole_count to 3 when the matched style has
            // no pole tagging (matcher's NULL-includes-3P rule).
            draft.pole_count = match.pole_count ?? 3
            if let cc = match.cont_current ?? resolved?.frame?.f_size {
                draft.frame_amps = Int(cc.rounded())
            }
            if let tt = match.trip_type_id {
                draft.trip_type_id = tt
            }
            // has_trip_unit follows the matched trip type's own flag, NOT
            // the matcher's SST routing axis. MCP is has_trip_unit (it
            // collects Sensor / Plug, gated on this flag at line ~631) yet
            // matches on frame amps (use_sst == false) — keying off
            // use_sst would wrongly hide the Sensor / Plug inputs after an
            // MCP pick. For true SST / thermal-mag the trip flag and
            // use_sst agree, so this is a no-op there.
            draft.has_trip_unit = match.trip_type_id
                .flatMap { tt in tripTypes.first(where: { $0.id == tt })?.has_trip_unit }
                ?? match.use_sst
        } else if isFuse {
            // Fuse: frame.f_size == trip_unit.f_size == rating. Use
            // resolved values when match.matched_trip_amp is nil
            // (partial-search pick).
            if let amp = match.matched_trip_amp ?? resolved?.tripUnit?.f_size ?? resolved?.frame?.f_size {
                draft.ampere_rating = Int(amp)
                draft.frame_amps = Int(amp)
            }
            if let tt = match.trip_type_id {
                draft.trip_type_id = tt
            }
            draft.has_trip_unit = false
        } else if isRelay {
            if let tt = match.trip_type_id {
                draft.trip_type_id = tt
            }
            draft.has_trip_unit = false
        }

        // ZP-2420: cable / busway / transformer post-pick mirroring.
        // Mirrors web's onClick handler in AssetFormFields.jsx for
        // these three classes — first-class draft fields get the
        // matched values, and the cable / transformer-specific
        // ``eqp_lib`` payload fields get rebuilt from the match so
        // the dedicated configurator cards (CableConfigCard /
        // TransformerConfigCard) open with sensible pre-selections.
        let isCableOrBusway = (match.slug == "cables-skm" || match.slug == "busway-skm")
        let isTransformerPick = (match.slug == "transformers-skm")

        if isCableOrBusway {
            // First-class draft mirroring.
            if let cm = match.conductor_type {
                draft.conductor_material = cm
            }
            if let cdId = match.conductor_description_id {
                draft.conductor_description_id = cdId
            }
            if let icId = match.insulation_class_id {
                draft.insulation_class_id = icId
            }
            if let itId = match.insulation_type_id {
                draft.insulation_type_id = itId
            }
            if let instId = match.installation_id {
                draft.installation_id = instId
            }
            if let dmId = match.duct_material_id {
                draft.duct_material_id = dmId
            }
            if let csId = match.cable_size_id {
                draft.cable_size_id = csId
            }
            // Busway: matched_cable_size is an ampere rating; mirror
            // it onto busway_ampere_rating so the engineering picker +
            // SQL-side readers stay aligned. PR review #6: parse via
            // Double first so decimal strings ("1200.0") and integer
            // strings ("1200") both succeed — ``Int("1200.0")`` returns
            // nil in Swift.
            if match.slug == "busway-skm",
               let mcs = match.matched_cable_size,
               let dval = Double(mcs) {
                draft.busway_ampere_rating = Int(dval.rounded())
            }
            // Build the cable / busway eqp_lib payload — the bound
            // card + CableConfigCard read from here.
            sel.length = draft.length  // preserve user-typed length
            sel.matched_cable_size = match.matched_cable_size
            sel.matched_cable_size_unit = match.matched_cable_size_unit
            sel.conductor_type = match.conductor_type
            sel.conductor_desc = match.conductor_desc
            sel.duct_material = match.duct_material
            sel.installation = match.installation
            sel.insulation_class = match.insulation_class
            sel.insulation_type = match.insulation_type
            sel.voltage_rating = match.voltage_rating
            sel.cable_size_id = match.cable_size_id
            // Web defaults busway connection to "Delta" and cable to
            // "Wye-Ground" — the user can override in the
            // CableConfigCard if needed.
            sel.connection_type = (match.slug == "busway-skm") ? "Delta" : "Wye-Ground"
            sel.qty_per_phase = 1
        }

        if isTransformerPick {
            // First-class draft mirroring.
            if let kva = match.matched_kva {
                draft.kva_rating = kva
            }
            if let tt = match.trip_type_id {
                draft.trip_type_id = tt
            }
            // ZP-2420: TransformerConfigCard looks up kVA entries via
            // ``SkmTransformerKvaEntry.transformer_oid == eqp.skm_oid``.
            // The BE match response carries ``style_id`` (set as
            // skm_oid above) AND ``transformer_oid`` — these can be
            // different identifiers for the same row. iOS's local
            // ``SkmTransformerKvaEntry.transformer_oid`` is synced
            // from the BE's transformer_oid, so prefer that here.
            // Falls back to style_id when transformer_oid is missing.
            if let txOid = match.transformer_oid {
                sel.skm_oid = txOid
            }
            // Transformer eqp_lib payload — TransformerConfigCard
            // reads kva_entry_id / percentage_r / percentage_x to
            // re-pin %Z when the user picks a different kVA tier.
            sel.kva_entry_id = match.kva_entry_id
            if let kva = match.matched_kva {
                sel.kva_rating = kva
            }
            if let st = match.str_type {
                sel.str_type = st
            }
            if let sts = match.str_type_symbol {
                sel.str_type_symbol = sts
            }
            if let ph = match.phase {
                sel.phase = ph
            }
            if let r = match.percentage_r {
                sel.percentage_r = r
            }
            if let x = match.percentage_x {
                sel.percentage_x = x
            }
            // Voltage + connection defaults — user refines in the
            // TransformerConfigCard. ``draftVoltage`` / ``draftSecondaryVoltage``
            // are the parent's authoritative source; fall back to 480 / 208
            // (mirrors web's null-coalescing defaults).
            sel.pri_voltage = draftVoltage ?? node.voltage ?? 480
            sel.sec_voltage = draftSecondaryVoltage ?? node.secondary_voltage ?? 208
            sel.pri_connection = "Delta"
            sel.sec_connection = "Wye-Ground"
        }

        // Single commit point — fully-populated selection (protective
        // settings block OR cable / transformer extensions, depending
        // on slug) lands on the parent's binding in one write.
        eqpLibSelection = sel
    }

    /// Resolved (frame, sensor, plug, tripUnit) for a picked match —
    /// honors the user's typed amp filters and the voltage floor.
    /// Falls back to "first valid" at each axis when the user hasn't
    /// narrowed it down. Mirrors web's SkmDeviceDetail.jsx
    /// ``handleFrameChange`` cascade so a pick lands with a fully-
    /// populated skm_settings even when the search was partial.
    private struct ResolvedSkm {
        var frame: SkmFrame?
        var sensor: SkmSensor?
        var plug: Double?
        var tripUnit: SkmTripUnit?
    }

    private func resolveSkmSelections(for match: SkmMatch) -> ResolvedSkm {
        let deviceOid = match.item_id
        // 1) Fetch device-scoped rows from the local SKM cache.
        let frameDescriptor = FetchDescriptor<SkmFrame>(
            predicate: #Predicate { $0.device_oid == deviceOid }
        )
        let allFrames = ((try? modelContext.fetch(frameDescriptor)) ?? [])
        let tuDescriptor = FetchDescriptor<SkmTripUnit>(
            predicate: #Predicate { $0.device_oid == deviceOid }
        )
        let allTripUnits = ((try? modelContext.fetch(tuDescriptor)) ?? [])

        // 2) Frame resolution — voltage-floor first, then filter by
        // user's frame_amps if set; pick closest-voltage tier.
        let assetV = node.voltage
        var frameCandidates = allFrames.filter { f in
            guard let v = assetV else { return true }
            guard let fv = f.f_voltage else { return true }
            return fv >= v
        }
        if let fa = draft.frame_amps {
            let faD = Double(fa)
            let narrowed = frameCandidates.filter { $0.f_size == faD }
            // Only apply the filter when it yields anything — otherwise
            // fall back to voltage-only candidates so we don't end up
            // with a nil frame on the picked device.
            if !narrowed.isEmpty { frameCandidates = narrowed }
        }
        // Sort by voltage ascending (closest-tier first); nil last.
        frameCandidates.sort { (a, b) in
            switch (a.f_voltage, b.f_voltage) {
            case (nil, nil): return false
            case (nil, _):   return false
            case (_, nil):   return true
            case (let av?, let bv?): return av < bv
            }
        }
        let frame = frameCandidates.first

        // 3) Sensor resolution — first sensor of the chosen frame
        // matching sensor_amps if set; else first sensor.
        let frameSensors: [SkmSensor] = frame.map { f in
            let fid = f.id
            let descriptor = FetchDescriptor<SkmSensor>(
                predicate: #Predicate { $0.frame_id == fid }
            )
            return ((try? modelContext.fetch(descriptor)) ?? [])
                .sorted { ($0.f_size ?? 0) < ($1.f_size ?? 0) }
        } ?? []
        var sensor: SkmSensor? = nil
        if let sa = draft.sensor_amps {
            let saD = Double(sa)
            sensor = frameSensors.first(where: { $0.f_size == saD })
        }
        if sensor == nil {
            sensor = frameSensors.first
        }

        // 4) Plug resolution — match plug_amps if set; else first
        // plug (or sensor.f_size when the plug list is empty).
        var plug: Double? = nil
        if let s = sensor {
            if let pa = draft.plug_amps {
                let paD = Double(pa)
                if s.plugs.contains(paD) {
                    plug = paD
                }
            }
            if plug == nil {
                plug = s.plugs.first ?? s.f_size
            }
        }

        // 5) Trip unit resolution. Web's rule: pick the trip unit
        // whose f_size matches the frame's f_size, fall back to first.
        // For thermal-mag / MCP / molded-case breakers the trip unit
        // IS the chassis tier (1:1 with the frame), so a draft-side
        // ``ampere_rating`` filter is not a tie-breaker — using it
        // would pick a trip unit whose f_size disagrees with the
        // frame's, which then has no matching segments on the FE.
        var tripUnit: SkmTripUnit? = nil
        if let fSize = frame?.f_size {
            tripUnit = allTripUnits.first(where: { $0.f_size == fSize })
        }
        if tripUnit == nil {
            tripUnit = allTripUnits.first
        }

        return ResolvedSkm(frame: frame, sensor: sensor, plug: plug, tripUnit: tripUnit)
    }

    @ViewBuilder private var cableBlock: some View {
        EngineeringBlock(title: "") {
            EngineeringDoubleField(label: AppStrings.Engineering.length, value: $draft.length, suffix: "ft")
            ConductorMaterialPicker(value: $draft.conductor_material)
            // Web binds "Conductor Description" to ``conductor_description_id``
            // — source enum is ``enum_cable_conductor_descriptions``.
            // ``is_busway`` partitions cable rows from busway rows.
            EngineeringEnumPicker(
                label: AppStrings.Engineering.conductorDescription,
                items: conductorDescs.filter { !$0.is_busway },
                selectedId: $draft.conductor_description_id,
                idOf: { $0.id }, labelOf: { $0.name }
            )
            // Web parity: cable_size_id is only editable AFTER a library
            // match has been bound (web's "Cable Configuration" section
            // sits below the bound-card summary). Without a matcher on
            // mobile yet, we gate purely on ``node.eqp_lib`` being set.
            if node.eqp_lib != nil {
                EngineeringEnumPicker(
                    label: AppStrings.Engineering.size,
                    items: cableSizes,
                    selectedId: $draft.cable_size_id,
                    idOf: { $0.id }, labelOf: { $0.name }
                )
            }
            EngineeringEnumPicker(
                label: AppStrings.Engineering.insulationClass,
                items: insulationClasses.filter { !$0.is_busway },
                selectedId: $draft.insulation_class_id,
                idOf: { $0.id }, labelOf: { $0.name }
            )
            EngineeringEnumPicker(
                label: AppStrings.Engineering.insulationType,
                items: insulationTypes.filter { !$0.is_busway },
                selectedId: $draft.insulation_type_id,
                idOf: { $0.id }, labelOf: { $0.name }
            )
            EngineeringEnumPicker(
                label: AppStrings.Engineering.installation,
                items: installations.filter { !$0.is_busway },
                selectedId: $draft.installation_id,
                idOf: { $0.id }, labelOf: { $0.name }
            )
            EngineeringEnumPicker(
                label: AppStrings.Engineering.ductMaterial,
                items: ductMaterials.filter { !$0.is_busway },
                selectedId: $draft.duct_material_id,
                idOf: { $0.id }, labelOf: { $0.name }
            )
            // ZP-2420: SKM cable library matches surface here once the
            // user has picked a conductor material. Network-backed
            // (mirrors web's /skm-cable-library/match call).
            cableMatchResultsPanel
        }
    }

    @ViewBuilder private var buswayBlock: some View {
        EngineeringBlock(title: "") {
            EngineeringDoubleField(label: AppStrings.Engineering.length, value: $draft.length, suffix: "ft")
            ConductorMaterialPicker(value: $draft.conductor_material)
            // Busway Size (Amps) — enum-backed, integer rating. The
            // node column ``busway_ampere_rating`` stores the actual
            // ampere value (NOT the enum FK), so we tag picker items by
            // ``ampere_value`` rather than ``id``.
            EngineeringEnumPicker(
                label: AppStrings.Engineering.buswaySizeAmps,
                items: buswayAmps,
                selectedId: $draft.busway_ampere_rating,
                idOf: { $0.ampere_value },
                labelOf: { "\($0.ampere_value)A" },
                icon: "bolt.horizontal"
            )
            // Conductor Configuration intentionally not surfaced for
            // busway — it's set silently by the library matcher (web
            // parity).
            EngineeringEnumPicker(
                label: AppStrings.Engineering.insulation,
                items: insulationClasses.filter { $0.is_busway },
                selectedId: $draft.insulation_class_id,
                idOf: { $0.id }, labelOf: { $0.name }
            )
            EngineeringEnumPicker(
                label: AppStrings.Engineering.construction,
                items: installations.filter { $0.is_busway },
                selectedId: $draft.installation_id,
                idOf: { $0.id }, labelOf: { $0.name }
            )
            // ZP-2420: SKM busway library matches — same endpoint as
            // cable, differentiated by ``is_busway=true``.
            cableMatchResultsPanel
        }
    }

    /// Trip-type binding with a side effect: selecting a trip type
    /// also updates ``draft.has_trip_unit`` from the trip's own flag.
    /// Mirrors the web AssetFormFields behavior where Sensor / Plug
    /// rows only appear when the selected trip type has a trip unit.
    private var tripTypeBinding: Binding<Int?> {
        Binding(
            get: { draft.trip_type_id },
            set: { newId in
                draft.trip_type_id = newId
                let hasUnit: Bool?
                if let id = newId, let trip = tripTypes.first(where: { $0.id == id }) {
                    hasUnit = trip.has_trip_unit
                } else {
                    hasUnit = nil
                }
                draft.has_trip_unit = hasUnit
                // Web parity: Trip Amps and Sensor+Plug are mutually
                // exclusive — null the set the new trip type doesn't
                // use so we don't carry stale values into the DB.
                if hasUnit == true {
                    draft.ampere_rating = nil
                } else if hasUnit == false {
                    draft.sensor_amps = nil
                    draft.plug_amps = nil
                }
            }
        )
    }

    // ── Display helpers ────────────────────────────────────────────

    /// Prefer the enum label (e.g. "480V"), fall back to the raw
    /// Double in case the FK didn't resolve.
    private func voltageDisplay(_ id: Int?, fallback: Double?) -> String? {
        if let label = voltageLabel(id) { return label }
        if let v = fallback { return "\(Int(v.rounded())) V" }
        return nil
    }

    private func numericDisplay(_ value: Int?, suffix: String) -> String? {
        value.map { "\($0)\(suffix)" }
    }

    private func numericDisplay(_ value: Double?, suffix: String) -> String? {
        value.map { v in
            // Drop trailing .0 for integral values
            v.truncatingRemainder(dividingBy: 1) == 0
                ? "\(Int(v))\(suffix)"
                : "\(v)\(suffix)"
        }
    }

    /// OCP block's header swaps between "Protective Device" /
    /// "Circuit Breaker" / "Fuse" / "Relay" based on lib type.
    private var blockTitleForOcp: String {
        switch libTypeName {
        case "circuit_breaker": return "Circuit Breaker"
        case "fuse": return "Fuse"
        case "relay": return "Relay"
        default: return "Protective Device"
        }
    }

    /// "Ampere Rating" for plain OCP, "Fuse Ampere Rating" for fuses,
    /// "Trip Amps" for SST (has_trip_unit) breakers, matching the web
    /// AssetFormFields naming.
    private var ampereLabelForOcp: String {
        if libTypeName == "fuse" { return "Fuse Ampere Rating" }
        if node.has_trip_unit == true { return "Trip Amps" }
        return "Ampere Rating"
    }
}

// MARK: - Row + Block primitives

private struct EngineeringBlock<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Empty title hides the row label entirely — used by the
            // OCP block which doesn't need a "CIRCUIT BREAKER" /
            // "FUSE" subtitle since the device class is already
            // implicit from the asset context.
            if !title.isEmpty {
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .textCase(.uppercase)
                    .tracking(0.5)
            }
            content
        }
    }
}

private struct EngineeringRow: View {
    let label: String
    let value: String?

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(label)
                .font(.subheadline)
                .foregroundColor(.secondary)
            Spacer()
            Text(value?.isEmpty == false ? value! : "—")
                .font(.subheadline)
                .fontWeight(value == nil ? .regular : .medium)
                .foregroundColor(value == nil ? .secondary : .primary)
        }
    }
}
