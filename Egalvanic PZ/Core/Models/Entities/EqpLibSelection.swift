//
//  EqpLibSelection.swift
//  Egalvanic PZ
//
//  Codable mirror of the SKM-canonical ``node.eqp_lib`` JSON.
//
//  ZP-2161 Phase 4B: rewritten to match the shape production has
//  migrated to. Three sibling field groups exist depending on the
//  bound device type:
//
//    • Protective devices (CB / fuse / relay):
//        top-level — type, style, skm_oid, category, cont_current,
//                    manufacturer
//        skm_settings — frame_id, sensor_id, trip_unit_oid,
//                       frame_amps, sensor_amps, plug_amps,
//                       ground_fault, has_ground_fault,
//                       segment_selections, included_segment_ids
//
//    • Cable / busway:
//        top-level — type, style, length, skm_oid, category,
//                    installation, manufacturer, cable_size_id,
//                    duct_material, qty_per_phase, conductor_desc,
//                    conductor_type, voltage_rating, connection_type,
//                    insulation_type, insulation_class,
//                    matched_cable_size, ground_size_parallel,
//                    ground_size_no_parallel, matched_cable_size_unit
//
//    • Transformer:
//        top-level — type, style, skm_oid, category, str_type,
//                    str_type_symbol, phase, kva_rating, kva_entry_id,
//                    pri_voltage, sec_voltage, pri_connection,
//                    sec_connection, percentage_r, percentage_x,
//                    manufacturer
//
//  All fields are optional so any device type's payload decodes
//  cleanly — the keys a given row doesn't use stay nil. The legacy
//  EZP-era ``trip_unit`` / ``trip_settings`` / ``frame_settings``
//  blocks have been retired (production migrated).
//
//  ``cont_current`` is an EZP-era key name retained for cross-platform
//  / on-disk schema compat — the value stored is SKM-correct
//  (``skm_frames.f_size``).
//
import Foundation

/// ZP-2243: web stores ``segment_selections`` values as either a plain
/// string (most segments — LT pickup, ST delay, etc.) or a compound
/// ``{value, suffix}`` object (CSEGMINSTBANDS dial+suffix compounds
/// and CSEGMISQUARETDELAY isqt-toggle suffix segments). Mobile mirrors
/// that contract so we can round-trip web-edited payloads without
/// losing data.
///
/// Decoded form is normalized to ``value`` + ``suffix``; ``suffix``
/// is empty for the plain-string case. On encode we re-emit the
/// compact form (bare string when suffix is empty, full
/// ``{value, suffix}`` object otherwise) so the wire payload stays
/// byte-identical to what web produces.
struct SegmentValue: Codable, Equatable {
    var value: String
    var suffix: String

    init(value: String, suffix: String = "") {
        self.value = value
        self.suffix = suffix
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if let s = try? c.decode(String.self) {
            self.value = s
            self.suffix = ""
            return
        }
        // Compound form. Use a flexible string-keyed dict decode
        // since web only sends ``value`` / ``suffix`` string keys.
        if let dict = try? c.decode([String: String].self) {
            self.value = dict["value"] ?? ""
            self.suffix = dict["suffix"] ?? ""
            return
        }
        throw DecodingError.dataCorruptedError(
            in: c,
            debugDescription: "Expected string or {value, suffix} object"
        )
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        if suffix.isEmpty {
            try c.encode(value)
        } else {
            try c.encode(["value": value, "suffix": suffix])
        }
    }

    /// User-facing display string — what the picker shows in its
    /// "selected value" slot. The suffix is metadata for export, not
    /// shown directly in the picker (mirrors web's UX).
    var displayValue: String { value }
}


struct EqpLibSelection: Codable, Equatable {
    // ── Common across all device types ────────────────────────────
    var type: String?
    var style: String?
    var skm_oid: Int?
    var category: String?
    var manufacturer: String?
    var cont_current: Double?

    // ── ZP-2267: Custom (one-shot, no library row) ────────────────
    /// When ``true``, this selection isn't bound to an SKM library
    /// row — the user filled in the Add Custom sheet. Wire-shape
    /// mirrors web's ``CustomLibraryDialog.handleSave`` payload so
    /// the same backend JSONB column handles both sources.
    /// ``category`` carries ``custom-{eqp_lib_type_name}`` (e.g.
    /// ``custom-cable`` / ``custom-circuit_breaker``).
    var custom: Bool?
    /// Trip / protective-device settings rows (free-form or
    /// SST/GF fixed slots, dropped when empty). Encoded inline at
    /// the top level — same as web.
    var settings: [CustomSetting]?
    /// Cable / busway custom section. Present only when the user
    /// added a custom cable or busway.
    var cable: CustomCableFields?
    /// Transformer custom section. Present only when the user
    /// added a custom transformer.
    var transformer: CustomTransformerFields?

    /// Convenience for read-paths that need a bool.
    var isCustom: Bool { custom == true }

    // ── Protective-device extensions ──────────────────────────────
    var skm_settings: SkmSettings?
    /// ZP-2161: SST vs thermal-mag flavor flag — pinned at pick time
    /// from the matcher's ``use_sst`` so the configurator can label
    /// the sensor picker "Sensor" (SST) vs "Trip" (thermal-mag /
    /// MCP / molded-case) without re-deriving from sz_dev_type.
    var use_sst: Bool?

    // ── Cable / busway extensions ─────────────────────────────────
    var length: Double?
    var installation: String?
    var cable_size_id: Int?
    var duct_material: String?
    var qty_per_phase: Int?
    var conductor_desc: String?
    var conductor_type: String?
    var voltage_rating: Double?
    var connection_type: String?
    var insulation_type: String?
    var insulation_class: String?
    var matched_cable_size: String?
    var ground_size_parallel: String?
    var ground_size_no_parallel: String?
    var matched_cable_size_unit: String?

    // ── Transformer extensions ────────────────────────────────────
    var phase: Int?
    var str_type: String?
    var str_type_symbol: String?
    var kva_rating: Double?
    var kva_entry_id: Int?
    var pri_voltage: Double?
    var sec_voltage: Double?
    var pri_connection: String?
    var sec_connection: String?
    var percentage_r: Double?
    var percentage_x: Double?

    /// Protective-device settings block — the editable axes the
    /// configurator surfaces after a library row is bound.
    struct SkmSettings: Codable, Equatable {
        var frame_id: Int?
        var sensor_id: Int?
        var trip_unit_oid: Int?
        var frame_amps: Double?
        var sensor_amps: Double?
        var plug_amps: Double?
        /// ZP-2243: full Ground Fault payload — paired SKM cdevice +
        /// resolved chassis pegs + per-segment selections. Matches
        /// web's ``skm_settings.ground_fault`` JSON shape exactly so
        /// the BE export step can read either source. ``nil`` when
        /// GF isn't enabled.
        var ground_fault: GroundFault?
        /// Convenience flag for query/filter use — the BE column
        /// ``nodes.has_ground_fault`` mirrors this for indexing.
        var has_ground_fault: Bool?
        /// segment id → user-picked value (always a string in the
        /// JSON, even when the underlying type is numeric — matches
        /// web's storage convention).
        var segment_selections: [String: SegmentValue]?
        /// IDs of segments that participate in the device's
        /// protection curve (vs. inert reference rows in the
        /// catalog).
        var included_segment_ids: [Int]?
    }

    /// ZP-2243: Ground Fault payload (parallel SKM cdevice + chassis
    /// pegs auto-resolved from the SST-side picks, plus per-segment
    /// selections from the GF cdevice's first trip unit). Mirrors
    /// web's ``SkmDeviceConfigurator.jsx`` JSON shape — see lines
    /// 640–676 of that file for the canonical contract.
    struct GroundFault: Codable, Equatable {
        // ── Library identity ──────────────────────────────────────
        var cdevice_oid: Int?
        var library_type: String?
        var library_description: String?
        // ── Trip unit + chassis pegs (mirrors SST-side picks) ────
        var trip_unit_oid: Int?
        var frame_id: Int?
        var frame_label: String?
        var frame_amps: Double?
        var frame_voltage: Double?
        var frame_interrupting_rating: Double?
        var sensor_id: Int?
        var sensor_amps: Double?
        var plug_amps: Double?
        // ── Per-segment selections ───────────────────────────────
        var segment_selections: [String: SegmentValue]?
        var included_segment_ids: [Int]?
        /// Flat name/value pairs the SKM UI-automation JSON expects
        /// — same buildSettingsArray output the SST side produces.
        var settings: [SettingEntry]?
        /// Full SKM UI-automation entry ready to dump into the
        /// script's settings.json. Key names mirror the script
        /// contract exactly (libraryType / libraryDescription /
        /// frame / sensor / plug / settings).
        var ia_package: IAPackage?

        struct SettingEntry: Codable, Equatable {
            var name: String
            var value: String
        }

        struct IAPackage: Codable, Equatable {
            var libraryType: String?
            var libraryDescription: String?
            var frame: String?
            var sensor: String?
            var plug: String?
            var settings: [SettingEntry]?
        }
    }

    // MARK: ZP-2267 Custom sub-types

    /// One free-form / fixed-slot trip-setting row in the custom
    /// dialog. ``suffix`` is the I²t-On/Off or dial-label compound
    /// (R1..R5 etc.) the web dialog surfaces for a subset of subtypes;
    /// empty string when none.
    struct CustomSetting: Codable, Equatable {
        var subtype: String
        var label: String
        var value: String
        var suffix: String
    }

    /// Cable / busway custom section. String-typed for numeric fields
    /// (cable_size like "4/0" or "1200") to mirror the web wire
    /// shape; ``length`` is a number per web's default of ``100``.
    struct CustomCableFields: Codable, Equatable {
        var conductor_type: String
        var cable_size: String
        var duct_material: String
        var insulation_class: String
        var length: Double
    }

    /// Transformer custom section. Numeric fields land as strings so
    /// blank inputs round-trip cleanly; connection axes are the four
    /// canonical Delta / Wye / Wye-Ground / Wye-Ground-Resistor.
    struct CustomTransformerFields: Codable, Equatable {
        var kva: String
        var z_pct: String
        var pri_voltage: String
        var sec_voltage: String
        var pri_connection: String
        var sec_connection: String

        static let connectionOptions: [String] = [
            "Delta", "Wye", "Wye-Ground", "Wye-Ground-Resistor"
        ]
    }

    /// Build a fully-populated EqpLibSelection from the inputs the
    /// user filled in the Add Custom sheet. Mirrors web's
    /// ``CustomLibraryDialog.handleSave`` (top-level identity +
    /// ``custom: true`` + ``category: "custom-{type}"`` + conditional
    /// ``cable`` / ``transformer`` / ``settings`` blocks) and also
    /// mirrors the cable / transformer extras onto the top-level
    /// fields the bound-library card already reads, so display
    /// works without a second code path.
    static func makeCustom(
        eqpLibTypeName: String,
        manufacturer: String,
        type: String,
        style: String,
        contCurrent: Double?,
        settings: [CustomSetting],
        cable: CustomCableFields? = nil,
        transformer: CustomTransformerFields? = nil
    ) -> EqpLibSelection {
        var sel = EqpLibSelection()
        sel.custom = true
        sel.category = "custom-\(eqpLibTypeName)"
        // Web defaults blank manufacturer to "Custom"; mirror it so
        // backend rows never carry an empty manufacturer string.
        let trimmedMfr = manufacturer.trimmingCharacters(in: .whitespaces)
        sel.manufacturer = trimmedMfr.isEmpty ? "Custom" : trimmedMfr
        sel.type = type
        sel.style = style
        sel.cont_current = contCurrent
        // Only attach a settings array for protective-device customs;
        // cable / busway / transformer leave it nil so the JSON stays
        // narrow. The web dialog also drops empty rows pre-emit.
        sel.settings = settings.isEmpty ? nil : settings
        sel.cable = cable
        sel.transformer = transformer

        // ── Top-level mirroring for the iOS bound-library card. The
        // existing display reads cable / transformer fields from the
        // top level (matched_cable_size, conductor_type, kva_rating,
        // pri_voltage, etc.). Populate those so the same card renders
        // custom and matched entries with one code path.
        if let cable {
            sel.conductor_type = cable.conductor_type.nilIfEmpty
            sel.duct_material = cable.duct_material.nilIfEmpty
            sel.insulation_class = cable.insulation_class.nilIfEmpty
            sel.matched_cable_size = cable.cable_size.nilIfEmpty
            sel.length = cable.length
        }
        if let transformer {
            sel.kva_rating = Double(transformer.kva)
            sel.pri_voltage = Double(transformer.pri_voltage)
            sel.sec_voltage = Double(transformer.sec_voltage)
            sel.pri_connection = transformer.pri_connection.nilIfEmpty
            sel.sec_connection = transformer.sec_connection.nilIfEmpty
        }
        return sel
    }

    /// Encode to JSON string for storage on ``NodeV2.eqp_lib``. Uses
    /// sorted keys so two equivalent selections serialize identically
    /// — keeps diff/PR signal clean when these strings show up in
    /// downstream dumps.
    func toJSONString() -> String? {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        guard let data = try? encoder.encode(self) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func from(jsonString: String?) -> EqpLibSelection? {
        guard let str = jsonString, let data = str.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(EqpLibSelection.self, from: data)
    }

    /// Compact summary for the bound-card chip. Falls back through
    /// {mfr → type → style → cont_current} for protective devices and
    /// {mfr → type → matched_cable_size} for cables / busways.
    var displaySummary: String {
        var parts: [String] = []
        if let mfr = manufacturer, !mfr.isEmpty { parts.append(mfr) }
        if let t = type, !t.isEmpty { parts.append(t) }
        if let s = style, !s.isEmpty { parts.append(s) }
        if let mcs = matched_cable_size, !mcs.isEmpty {
            let unit = matched_cable_size_unit ?? ""
            parts.append("\(mcs)\(unit.isEmpty ? "" : " \(unit)")")
        } else if let cc = cont_current {
            parts.append("\(Int(cc))A")
        }
        return parts.isEmpty ? "No equipment selected" : parts.joined(separator: " / ")
    }

    // MARK: ZP-2267 Custom helpers

    /// True for any custom category (``custom-cable``,
    /// ``custom-busway``, ``custom-transformer``, ``custom-circuit_breaker``,
    /// ``custom-fuse``, ``custom-relay``). Used by the bound-card to
    /// pick the right body layout for custom entries.
    var isCustomCableOrBusway: Bool {
        category == "custom-cable" || category == "custom-busway"
    }
    var isCustomTransformer: Bool { category == "custom-transformer" }
    var isCustomProtective: Bool {
        category == "custom-circuit_breaker" ||
        category == "custom-fuse" ||
        category == "custom-relay"
    }

    // MARK: ZP-2267 Display resolvers
    //
    // iOS ``makeCustom`` mirrors the user's cable / transformer inputs
    // onto the top-level fields the bound-library card already reads,
    // so iOS-saved entries render via the same code path as matched
    // entries. Web-saved custom entries don't carry that mirror — they
    // only populate ``cable: {...}`` / ``transformer: {...}`` blocks.
    // These resolvers prefer the top-level mirror (matched entries +
    // iOS-saved custom) then fall back to the nested block (web-saved
    // custom), so the bound card displays cleanly regardless of source.

    var resolvedCableSize: String? {
        if let m = matched_cable_size, !m.isEmpty { return m }
        return cable?.cable_size.nilIfEmpty
    }
    var resolvedConductorType: String? {
        if let m = conductor_type, !m.isEmpty { return m }
        return cable?.conductor_type.nilIfEmpty
    }
    var resolvedDuctMaterial: String? {
        if let m = duct_material, !m.isEmpty { return m }
        return cable?.duct_material.nilIfEmpty
    }
    var resolvedInsulationClass: String? {
        if let m = insulation_class, !m.isEmpty { return m }
        return cable?.insulation_class.nilIfEmpty
    }
    var resolvedKvaRating: Double? {
        if let k = kva_rating { return k }
        if let s = transformer?.kva { return Double(s) }
        return nil
    }
    var resolvedPriVoltage: Double? {
        if let v = pri_voltage { return v }
        if let s = transformer?.pri_voltage { return Double(s) }
        return nil
    }
    var resolvedSecVoltage: Double? {
        if let v = sec_voltage { return v }
        if let s = transformer?.sec_voltage { return Double(s) }
        return nil
    }
    var resolvedPriConnection: String? {
        if let c = pri_connection, !c.isEmpty { return c }
        return transformer?.pri_connection.nilIfEmpty
    }
    var resolvedSecConnection: String? {
        if let c = sec_connection, !c.isEmpty { return c }
        return transformer?.sec_connection.nilIfEmpty
    }
}
