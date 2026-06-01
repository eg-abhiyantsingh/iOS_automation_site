//
//  CustomEqpLibSheet.swift
//  Egalvanic PZ
//
//  ZP-2267: Add Custom (one-shot, no library row) sheet — iOS mirror
//  of web's ``CustomLibraryDialog.jsx``. Surfaces from the match panel
//  when the SKM library doesn't return a matching row; collects a
//  custom equipment payload and routes it back through the same
//  ``eqp_lib`` binding the matched-pick path uses.
//
//  Layout branches by class:
//    • Cable / busway → identity + cable specs (conductor type, size,
//      duct material, insulation, length)
//    • Transformer    → identity + kVA / Z% / voltages / connections
//    • Protective devices, branched by trip type:
//        - SST families (``has_trip_unit == true``): fixed LT/ST/Inst
//          slots + optional GF block + appendable free-form rows
//        - Ground Fault breaker (slug ``ground_fault``): GF Pickup +
//          GF Delay slots + appendable free-form rows
//        - Everything else (thermal-mag, MCP, DC MCCB, fuses, relays):
//          free-form "Add Setting" rows only
//
import SwiftUI

struct CustomEqpLibSheet: View {
    // MARK: Inputs

    /// Drives the section layout. One of: ``circuit_breaker`` / ``fuse``
    /// / ``relay`` / ``cable`` / ``busway`` / ``transformer``. Comes
    /// from the node class's ``eqp_lib_type_name``.
    let eqpLibTypeName: String
    /// Selected trip type's slug — used by the protective branches to
    /// pick SST vs GF vs free-form. ``nil`` for non-protective classes.
    let tripTypeSlug: String?
    /// Selected trip type's ``has_trip_unit`` — gates the SST layout
    /// for protective classes. Matches web's ``tripTypeHasTripUnit``.
    let tripTypeHasTripUnit: Bool
    /// Manufacturer pre-fill from the asset form. Empty string when
    /// the user hasn't picked one yet.
    let prefilledManufacturer: String
    /// Existing custom selection — populated when editing an already-
    /// saved custom entry. ``nil`` for fresh adds.
    let existingSelection: EqpLibSelection?
    /// Submit handler — receives the fully-populated selection. The
    /// caller writes it to the parent ``eqpLibSelection`` binding and
    /// closes the sheet.
    let onSave: (EqpLibSelection) -> Void

    @Environment(\.dismiss) private var dismiss

    // MARK: Identity state

    @State private var manufacturer: String = ""
    @State private var typeField: String = ""
    @State private var styleField: String = ""
    @State private var ampsField: String = ""

    // MARK: Cable state (defaults match web)

    @State private var conductorType: String = "Copper"
    @State private var cableSize: String = ""
    @State private var ductMaterial: String = ""
    @State private var insulationClass: String = ""
    @State private var lengthFt: String = "100"

    // MARK: Transformer state (defaults match web)

    @State private var kva: String = ""
    @State private var zPct: String = ""
    @State private var priVoltage: String = ""
    @State private var secVoltage: String = ""
    @State private var priConnection: String = "Delta"
    @State private var secConnection: String = "Wye-Ground"

    // MARK: Protective state

    /// SST fixed-slot values keyed by ``slot.key``.
    @State private var sstValues: [String: SegmentValue] = [:]
    /// Whether the user has toggled on the GF section under SST.
    @State private var includeGf: Bool = false
    /// GF fixed-slot values keyed by ``slot.key``.
    @State private var gfValues: [String: SegmentValue] = [:]
    /// Appendable free-form rows (mirrors web's ``freeForm`` array).
    @State private var freeFormRows: [FreeFormRow] = []

    // MARK: Computed gates

    private var isProtective: Bool {
        ["circuit_breaker", "fuse", "relay"].contains(eqpLibTypeName)
    }
    private var isCable: Bool {
        eqpLibTypeName == "cable" || eqpLibTypeName == "busway"
    }
    private var isBusway: Bool { eqpLibTypeName == "busway" }
    private var isTransformer: Bool { eqpLibTypeName == "transformer" }

    /// ZP-2267 layout flag — SST families use fixed top slots.
    /// Mirrors web's ``useSstSlots`` (gated on ``tripTypeHasTripUnit``).
    private var useSstSlots: Bool { isProtective && tripTypeHasTripUnit }
    /// GF breakers use a 2-slot block. Mirrors web's ``useGfSlots``
    /// (slug equality on ``ground_fault``).
    private var useGfSlots: Bool {
        isProtective && (tripTypeSlug == "ground_fault")
    }
    /// Everything else (thermal-mag, MCP, DC MCCB, fuses, relays) uses
    /// only free-form rows.
    private var useFreeForm: Bool {
        isProtective && !useSstSlots && !useGfSlots
    }

    private var sheetTitle: String {
        existingSelection?.isCustom == true
            ? AppStrings.Engineering.editCustomEquipment
            : AppStrings.Engineering.addCustomEquipment
    }

    /// Save gate — at least one identifying field (manufacturer or
    /// type) must be non-empty. Prevents persisting an entry with
    /// nothing the user can recognize on the bound card later.
    private var canSave: Bool {
        let m = manufacturer.trimmingCharacters(in: .whitespacesAndNewlines)
        let t = typeField.trimmingCharacters(in: .whitespacesAndNewlines)
        return !m.isEmpty || !t.isEmpty
    }

    // MARK: Body

    var body: some View {
        NavigationStack {
            Form {
                identitySection
                if isCable { cableSection }
                if isTransformer { transformerSection }
                if useSstSlots {
                    sstSection
                    gfToggleSection
                }
                if useGfSlots { gfOnlySection }
                if useFreeForm || useSstSlots || useGfSlots { freeFormSection }
            }
            .navigationTitle(sheetTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(AppStrings.Common.save) {
                        onSave(buildSelection())
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    // ZP-2421 review #1: require at least one
                    // identifying field (manufacturer or type) so a
                    // fully-blank entry can't be persisted. Web allows
                    // it but defaults manufacturer to "Custom"; iOS is
                    // slightly stricter — we want either a real
                    // manufacturer or a real type/catalog before save.
                    .disabled(!canSave)
                }
            }
            .onAppear { hydrateFromExisting() }
        }
    }

    // MARK: - Identity section (all classes)

    @ViewBuilder private var identitySection: some View {
        Section(AppStrings.Engineering.identity) {
            TextField(AppStrings.Engineering.manufacturer, text: $manufacturer)
                .autocorrectionDisabled()
            TextField(AppStrings.Engineering.typeCatalog, text: $typeField)
                .autocorrectionDisabled()
            TextField(AppStrings.Engineering.styleModel, text: $styleField)
                .autocorrectionDisabled()
            if isProtective {
                TextField(AppStrings.Engineering.amps, text: $ampsField)
                    .keyboardType(.decimalPad)
            }
        }
    }

    // MARK: - Cable / busway

    @ViewBuilder private var cableSection: some View {
        Section(AppStrings.Engineering.cableSpecifications) {
            TextField(AppStrings.Engineering.conductorType, text: $conductorType)
                .autocorrectionDisabled()
            TextField(
                isBusway ? AppStrings.Engineering.sizeAmps : AppStrings.Engineering.cableSize,
                text: $cableSize
            )
            // ZP-2421 review #5: busway cable_size is a numeric amp
            // rating ("1200") — ``.decimalPad`` keeps the keyboard
            // restricted to digits. Cable uses default so AWG / kcmil
            // labels like "4/0" (slash, mixed) remain typeable.
            .keyboardType(isBusway ? .decimalPad : .default)
            .autocorrectionDisabled()
            TextField(AppStrings.Engineering.ductMaterial, text: $ductMaterial)
                .autocorrectionDisabled()
            TextField(AppStrings.Engineering.insulationClass, text: $insulationClass)
                .autocorrectionDisabled()
            TextField(AppStrings.Engineering.lengthFt, text: $lengthFt)
                .keyboardType(.decimalPad)
        }
    }

    // MARK: - Transformer

    @ViewBuilder private var transformerSection: some View {
        Section(AppStrings.Engineering.transformerSpecifications) {
            TextField(AppStrings.Engineering.kvaRatingLabel, text: $kva)
                .keyboardType(.decimalPad)
            TextField(AppStrings.Engineering.zPercent, text: $zPct)
                .keyboardType(.decimalPad)
            TextField(AppStrings.Engineering.primaryVoltageField, text: $priVoltage)
                .keyboardType(.decimalPad)
            TextField(AppStrings.Engineering.secondaryVoltageField, text: $secVoltage)
                .keyboardType(.decimalPad)
            Picker(AppStrings.Engineering.priConnection, selection: $priConnection) {
                ForEach(EqpLibSelection.CustomTransformerFields.connectionOptions, id: \.self) {
                    Text($0).tag($0)
                }
            }
            Picker(AppStrings.Engineering.secConnection, selection: $secConnection) {
                ForEach(EqpLibSelection.CustomTransformerFields.connectionOptions, id: \.self) {
                    Text($0).tag($0)
                }
            }
        }
    }

    // MARK: - SST fixed slots

    @ViewBuilder private var sstSection: some View {
        Section(AppStrings.Engineering.tripSettings) {
            ForEach(CustomEqpLibSheet.sstSlots, id: \.key) { slot in
                sstSlotRow(slot)
            }
        }
    }

    @ViewBuilder private func sstSlotRow(_ slot: TripSlot) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(slot.localizedLabel)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.secondary)
            HStack(spacing: 8) {
                TextField(
                    AppStrings.Engineering.settingPlaceholderValue,
                    text: sstValueBinding(slot.key)
                )
                .textFieldStyle(.roundedBorder)
                if SubtypeMeta.hasSuffix(slot.subtype) {
                    suffixPicker(
                        subtype: slot.subtype,
                        selection: sstSuffixBinding(slot.key)
                    )
                    .frame(maxWidth: 120)
                }
            }
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder private var gfToggleSection: some View {
        Section {
            Toggle(AppStrings.Engineering.includeGroundFault, isOn: $includeGf)
            if includeGf {
                ForEach(CustomEqpLibSheet.gfSlots, id: \.key) { slot in
                    gfSlotRow(slot)
                }
            }
        }
    }

    @ViewBuilder private var gfOnlySection: some View {
        Section(AppStrings.Engineering.groundFaultSettingsHeader) {
            ForEach(CustomEqpLibSheet.gfSlots, id: \.key) { slot in
                gfSlotRow(slot)
            }
        }
    }

    @ViewBuilder private func gfSlotRow(_ slot: TripSlot) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(slot.localizedLabel)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.secondary)
            TextField(
                AppStrings.Engineering.settingPlaceholderValue,
                text: gfValueBinding(slot.key)
            )
            .textFieldStyle(.roundedBorder)
        }
        .padding(.vertical, 4)
    }

    // MARK: - Free-form rows

    @ViewBuilder private var freeFormSection: some View {
        Section {
            ForEach($freeFormRows) { $row in
                freeFormRow($row)
            }
            .onDelete { offsets in
                freeFormRows.remove(atOffsets: offsets)
            }
            Button {
                freeFormRows.append(.blank())
            } label: {
                Label(AppStrings.Engineering.addSetting, systemImage: "plus.circle.fill")
            }
        } header: {
            Text(useFreeForm
                 ? AppStrings.Engineering.settings
                 : AppStrings.Engineering.additionalSettings)
        }
    }

    @ViewBuilder private func freeFormRow(_ row: Binding<FreeFormRow>) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Picker(AppStrings.Engineering.function, selection: row.subtype) {
                ForEach(SubtypeMeta.allSubtypes, id: \.0) { (code, _) in
                    Text(SubtypeMeta.label(for: code)).tag(code)
                }
            }
            .onChange(of: row.wrappedValue.subtype) { _, _ in
                // Clear suffix when the subtype switches — the suffix
                // dropdown options change with subtype (I²t vs dial).
                row.wrappedValue.suffix = ""
            }
            TextField(
                AppStrings.Engineering.settingPlaceholderLabel,
                text: row.label
            )
            .textFieldStyle(.roundedBorder)
            HStack(spacing: 8) {
                TextField(
                    AppStrings.Engineering.settingValue,
                    text: row.value
                )
                .textFieldStyle(.roundedBorder)
                if SubtypeMeta.hasSuffix(row.wrappedValue.subtype) {
                    suffixPicker(
                        subtype: row.wrappedValue.subtype,
                        selection: row.suffix
                    )
                    .frame(maxWidth: 120)
                }
            }
        }
        .padding(.vertical, 4)
    }

    // MARK: - Suffix picker

    @ViewBuilder private func suffixPicker(subtype: String, selection: Binding<String>) -> some View {
        if subtype == "CSEGMISQUARETDELAY" {
            Picker(AppStrings.Engineering.iSquaredT, selection: selection) {
                Text("—").tag("")
                Text(AppStrings.Engineering.on).tag("(I^s T On)")
                Text(AppStrings.Engineering.off).tag("(I^s T Off)")
            }
        } else {
            Picker(AppStrings.Engineering.dialPickerLabel, selection: selection) {
                Text("—").tag("")
                ForEach(["R1", "R2", "R3", "R4", "R5"], id: \.self) { d in
                    Text(d).tag(d)
                }
            }
        }
    }

    // MARK: - Bindings (dictionary into per-slot value/suffix)

    private func sstValueBinding(_ key: String) -> Binding<String> {
        Binding(
            get: { sstValues[key]?.value ?? "" },
            set: { newValue in
                var existing = sstValues[key] ?? SegmentValue(value: "")
                existing.value = newValue
                sstValues[key] = existing
            }
        )
    }
    private func sstSuffixBinding(_ key: String) -> Binding<String> {
        Binding(
            get: { sstValues[key]?.suffix ?? "" },
            set: { newValue in
                var existing = sstValues[key] ?? SegmentValue(value: "")
                existing.suffix = newValue
                sstValues[key] = existing
            }
        )
    }
    private func gfValueBinding(_ key: String) -> Binding<String> {
        Binding(
            get: { gfValues[key]?.value ?? "" },
            set: { newValue in
                var existing = gfValues[key] ?? SegmentValue(value: "")
                existing.value = newValue
                gfValues[key] = existing
            }
        )
    }

    // MARK: - Build / hydrate

    /// Builds the settings array from the current state, dropping
    /// empty rows. Mirrors web's ``buildSettings()`` exactly.
    private func buildSettings() -> [EqpLibSelection.CustomSetting] {
        var out: [EqpLibSelection.CustomSetting] = []
        if useSstSlots {
            for slot in CustomEqpLibSheet.sstSlots {
                guard let v = sstValues[slot.key], !v.value.isEmpty else { continue }
                out.append(.init(subtype: slot.subtype, label: slot.englishLabel, value: v.value, suffix: v.suffix))
            }
            if includeGf {
                for slot in CustomEqpLibSheet.gfSlots {
                    guard let v = gfValues[slot.key], !v.value.isEmpty else { continue }
                    out.append(.init(subtype: slot.subtype, label: slot.englishLabel, value: v.value, suffix: v.suffix))
                }
            }
        } else if useGfSlots {
            for slot in CustomEqpLibSheet.gfSlots {
                guard let v = gfValues[slot.key], !v.value.isEmpty else { continue }
                out.append(.init(subtype: slot.subtype, label: slot.englishLabel, value: v.value, suffix: v.suffix))
            }
        }
        if useFreeForm || useSstSlots || useGfSlots {
            for row in freeFormRows {
                guard !row.label.isEmpty, !row.value.isEmpty else { continue }
                out.append(.init(subtype: row.subtype, label: row.label, value: row.value, suffix: row.suffix))
            }
        }
        return out
    }

    private func buildSelection() -> EqpLibSelection {
        let cable: EqpLibSelection.CustomCableFields? = isCable
            ? .init(
                conductor_type: conductorType,
                cable_size: cableSize,
                duct_material: ductMaterial,
                insulation_class: insulationClass,
                length: Double(lengthFt) ?? 100
            )
            : nil
        let xfmr: EqpLibSelection.CustomTransformerFields? = isTransformer
            ? .init(
                kva: kva,
                z_pct: zPct,
                pri_voltage: priVoltage,
                sec_voltage: secVoltage,
                pri_connection: priConnection,
                sec_connection: secConnection
            )
            : nil
        return EqpLibSelection.makeCustom(
            eqpLibTypeName: eqpLibTypeName,
            manufacturer: manufacturer,
            type: typeField,
            style: styleField,
            contCurrent: isProtective ? Double(ampsField) : nil,
            settings: buildSettings(),
            cable: cable,
            transformer: xfmr
        )
    }

    /// Pre-fill state from an existing custom selection (edit path)
    /// or from the parent-form manufacturer prefill (fresh add path).
    /// Idempotent — runs on .onAppear once per sheet instance.
    private func hydrateFromExisting() {
        guard let sel = existingSelection, sel.isCustom else {
            // Fresh add — only pre-fill manufacturer, leave defaults
            // (e.g. duct_material) keyed off the class.
            if manufacturer.isEmpty {
                manufacturer = prefilledManufacturer
            }
            if isCable && ductMaterial.isEmpty {
                ductMaterial = isBusway ? "Bus" : "Magnetic"
            }
            return
        }
        manufacturer = sel.manufacturer ?? ""
        typeField = sel.type ?? ""
        styleField = sel.style ?? ""
        ampsField = sel.cont_current.map { trimNumber($0) } ?? ""
        if let cable = sel.cable {
            conductorType = cable.conductor_type
            cableSize = cable.cable_size
            ductMaterial = cable.duct_material
            insulationClass = cable.insulation_class
            lengthFt = trimNumber(cable.length)
        }
        if let xfmr = sel.transformer {
            kva = xfmr.kva
            zPct = xfmr.z_pct
            priVoltage = xfmr.pri_voltage
            secVoltage = xfmr.sec_voltage
            priConnection = xfmr.pri_connection
            secConnection = xfmr.sec_connection
        }
        if let settings = sel.settings {
            hydrateSettings(settings)
        }
    }

    private func hydrateSettings(_ settings: [EqpLibSelection.CustomSetting]) {
        if useSstSlots {
            // Map known SST/GF slot labels back to their dict keys;
            // anything else lands in free-form rows.
            let sstByLabel = Dictionary(
                uniqueKeysWithValues: CustomEqpLibSheet.sstSlots.map { ($0.englishLabel, $0) }
            )
            let gfByLabel = Dictionary(
                uniqueKeysWithValues: CustomEqpLibSheet.gfSlots.map { ($0.englishLabel, $0) }
            )
            var leftover: [EqpLibSelection.CustomSetting] = []
            for s in settings {
                if let slot = sstByLabel[s.label] {
                    sstValues[slot.key] = SegmentValue(value: s.value, suffix: s.suffix)
                } else if let slot = gfByLabel[s.label] {
                    gfValues[slot.key] = SegmentValue(value: s.value, suffix: s.suffix)
                    includeGf = true
                } else {
                    leftover.append(s)
                }
            }
            freeFormRows = leftover.map { .init(subtype: $0.subtype, label: $0.label, value: $0.value, suffix: $0.suffix) }
        } else if useGfSlots {
            let gfByLabel = Dictionary(
                uniqueKeysWithValues: CustomEqpLibSheet.gfSlots.map { ($0.englishLabel, $0) }
            )
            var leftover: [EqpLibSelection.CustomSetting] = []
            for s in settings {
                if let slot = gfByLabel[s.label] {
                    gfValues[slot.key] = SegmentValue(value: s.value, suffix: s.suffix)
                } else {
                    leftover.append(s)
                }
            }
            freeFormRows = leftover.map { .init(subtype: $0.subtype, label: $0.label, value: $0.value, suffix: $0.suffix) }
        } else {
            freeFormRows = settings.map { .init(subtype: $0.subtype, label: $0.label, value: $0.value, suffix: $0.suffix) }
        }
    }

    /// Trim trailing ``.0`` off integer-valued doubles for nicer
    /// pre-fill values when the user reopens the sheet.
    ///
    /// ZP-2421 review #6: use format-string conversion rather than
    /// ``Int(d)`` so very large or NaN doubles can't trap the Int
    /// initializer. ``%.0f`` rounds half-to-even; for whole-number
    /// inputs (the only branch that hits this path) the result is
    /// identical to ``String(Int(d))`` but never overflows.
    private func trimNumber(_ d: Double) -> String {
        if d.truncatingRemainder(dividingBy: 1) == 0 {
            return String(format: "%.0f", d)
        }
        return String(d)
    }
}

// MARK: - Supporting types

extension CustomEqpLibSheet {
    /// One row in the free-form ``Add Setting`` list. ``id`` lets the
    /// SwiftUI ForEach + onDelete identify rows stably across edits.
    struct FreeFormRow: Identifiable, Equatable {
        let id: UUID
        var subtype: String
        var label: String
        var value: String
        var suffix: String

        init(id: UUID = UUID(), subtype: String, label: String, value: String, suffix: String) {
            self.id = id
            self.subtype = subtype
            self.label = label
            self.value = value
            self.suffix = suffix
        }

        static func blank() -> FreeFormRow {
            FreeFormRow(subtype: "CSEGMVERTICAL", label: "", value: "", suffix: "")
        }
    }

    /// One fixed slot (SST or GF). ``englishLabel`` is the wire label
    /// (kept English so settings round-trip stably across locales);
    /// ``localizedLabel`` is the on-screen string.
    struct TripSlot {
        let key: String
        let englishLabel: String
        let localizedLabel: String
        let subtype: String
    }

    // Computed (not stored) so localized labels re-evaluate per
    // language switch — matches the rest of the AppStrings pattern.
    static var sstSlots: [TripSlot] {
        [
            TripSlot(key: "lt_pickup", englishLabel: "LT Pickup", localizedLabel: AppStrings.Engineering.ltPickup, subtype: "CSEGMVERTICAL"),
            TripSlot(key: "lt_delay",  englishLabel: "LT Delay",  localizedLabel: AppStrings.Engineering.ltDelay,  subtype: "CSEGMHORIZONTAL"),
            TripSlot(key: "st_pickup", englishLabel: "ST Pickup", localizedLabel: AppStrings.Engineering.stPickup, subtype: "CSEGMVERTICAL"),
            TripSlot(key: "st_delay",  englishLabel: "ST Delay",  localizedLabel: AppStrings.Engineering.stDelay,  subtype: "CSEGMISQUARETDELAY"),
            TripSlot(key: "inst",      englishLabel: "Inst",      localizedLabel: AppStrings.Engineering.inst,     subtype: "CSEGMINSTCURVE"),
        ]
    }

    static var gfSlots: [TripSlot] {
        [
            TripSlot(key: "gf_pickup", englishLabel: "GF Pickup", localizedLabel: AppStrings.Engineering.gfPickup, subtype: "CSEGMVERTICAL"),
            TripSlot(key: "gf_delay",  englishLabel: "GF Delay",  localizedLabel: AppStrings.Engineering.gfDelay,  subtype: "CSEGMHORIZONTAL"),
        ]
    }
}

/// SKM subtype metadata — mirrors web's ``SUBTYPE_FRIENDLY_LABELS`` +
/// ``SUBTYPE_HAS_SUFFIX``. Kept local to this file since it's only
/// the Add Custom dialog that needs the friendly-label mapping.
private enum SubtypeMeta {
    // Computed so locale switches re-evaluate the labels.
    static var allSubtypes: [(String, String)] {
        [
            ("CSEGMVERTICAL",           AppStrings.Engineering.subtypePickup),
            ("CSEGMHORIZONTAL",         AppStrings.Engineering.subtypeTimeDelay),
            ("CSEGMINSTCURVE",          AppStrings.Engineering.subtypeInst),
            ("CSEGMINSTTIME",           AppStrings.Engineering.subtypeInstDelay),
            ("CSEGMINSTBANDS",          AppStrings.Engineering.subtypeInstBands),
            ("CSEGMINSTADJTOLERANCE",   AppStrings.Engineering.subtypeInstTolerance),
            ("CSEGMISQUARETDELAY",      AppStrings.Engineering.subtypeStDelayI2t),
            ("CSEGMSLOPEOPENCLEARTIME", AppStrings.Engineering.subtypeClearingSlope),
            ("CSEGMOPENCLEARBANDS",     AppStrings.Engineering.subtypeClearingBands),
            ("CSEGMIPOWERT",            AppStrings.Engineering.subtypeIPowerT),
            ("CSEGMCURSET",             AppStrings.Engineering.subtypeCurrentSet),
            ("CSEGMTIMECURRENTBANDS",   AppStrings.Engineering.subtypeTccBands),
            ("CSEGMEQUATION1",          AppStrings.Engineering.subtypeEquation1),
            ("CSEGMEQUATION2",          AppStrings.Engineering.subtypeEquation2),
            ("CSEGMEQUATION3",          AppStrings.Engineering.subtypeEquation3),
        ]
    }
    static let suffixSubtypes: Set<String> = [
        "CSEGMISQUARETDELAY",
        "CSEGMINSTBANDS",
        "CSEGMOPENCLEARBANDS",
    ]
    static func label(for code: String) -> String {
        allSubtypes.first(where: { $0.0 == code })?.1 ?? code
    }
    static func hasSuffix(_ code: String) -> Bool {
        suffixSubtypes.contains(code)
    }
}
