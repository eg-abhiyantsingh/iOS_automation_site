//
//  SLDDTONode.swift
//  SwiftDataTutorial
//
import Foundation

/// DTO for COM calculation values
struct COMCalculationDTO: Codable {
    var criticalityValue: Int
    var maintenanceValue: Int?
    var operatingConditionsValue: Int
    var maintenanceAnswers: [String]?

    enum CodingKeys: String, CodingKey {
        case criticalityValue = "criticality_value"
        case maintenanceValue = "maintenance_value"
        case operatingConditionsValue = "operating_conditions_value"
        case maintenanceAnswers = "maintenance_answers"
    }

    init(criticalityValue: Int = 1, maintenanceValue: Int? = 1, operatingConditionsValue: Int = 1, maintenanceAnswers: [String]? = nil) {
        self.criticalityValue = criticalityValue
        self.maintenanceValue = maintenanceValue
        self.operatingConditionsValue = operatingConditionsValue
        self.maintenanceAnswers = maintenanceAnswers
    }

    /// Convert from entity model
    init(from calculation: COMCalculation) {
        self.criticalityValue = calculation.criticalityValue
        self.maintenanceValue = calculation.maintenanceValue
        self.operatingConditionsValue = calculation.operatingConditionsValue
        self.maintenanceAnswers = calculation.maintenanceAnswers
    }

    /// Convert to entity model
    func toModel() -> COMCalculation {
        COMCalculation(
            criticalityValue: criticalityValue,
            maintenanceValue: maintenanceValue,
            operatingConditionsValue: operatingConditionsValue,
            maintenanceAnswers: maintenanceAnswers
        )
    }
}

struct SLDDTONode: Codable {
    var id: UUID
    var type: String
    var label: String
    var sld_id: UUID
    var parent_id: UUID?
    var x: Double
    var y: Double
    var width: Double
    var height: Double
    var is_deleted: Bool = false
    var location: String?
    var node_class: UUID?
    var node_subtype: UUID?
    var core_attributes: [NodePropertyDTO]
    var node_terminals: [NodeTerminalDTO]
    var com: Int?
    var com_calculation: COMCalculationDTO?
    var qr_code: String?
    var serviceability: String?
    var serviceability_note: String?
    var voltage: Double?
    var voltage_id: Int?
    var secondary_voltage: Double?
    var secondary_voltage_id: Int?
    var tertiary_voltage: Double?
    var tertiary_voltage_id: Int?
    var system_voltage_id: Int?
    var circuit_voltage_id: Int?
    var voltage_user_overridden: Bool?
    var notes: String?
    var af_completion: Bool?
    var room_id: UUID?
    var default_photo_id: UUID?
    var suggested_shortcut: UUID?
    var applied_shortcut: UUID?
    var is_collapsed: Bool?
    var eqp_lib: String?  // Raw JSON pass-through for equipment library selection
    var eqp_lib_suggested: String?
    var eqp_note: String?
    var eqp_engineering_approved: Bool?
    var skm_lib_name: String?
    var skm_lib_name_suggested: String?
    var ocr_signature: String?  // Raw JSON pass-through for the OCR-extracted nameplate

    // ── Transformer ────────────────────────────────────────────────
    var kva_rating: Double?
    var percent_impedance: Double?

    // ── Box-level (mains breakers / panels) ────────────────────────
    var mains_type_id: Int?
    var phase_configuration_id: Int?

    // ── OCP ────────────────────────────────────────────────────────
    var ampere_rating: Int?
    var pole_count: Int?
    var manufacturer_id: Int?
    var has_trip_unit: Bool?
    var trip_type_id: Int?
    var frame_amps: Int?
    var sensor_amps: Int?
    var plug_amps: Int?

    // ── Cable / Busway ─────────────────────────────────────────────
    var length: Double?
    var conductor_material: String?
    var cable_size_id: Int?
    var conductor_configuration_id: Int?
    var duct_material_id: Int?
    var conductor_description_id: Int?
    var insulation_class_id: Int?
    var insulation_type_id: Int?
    var installation_id: Int?
    var busway_ampere_rating: Int?

    // ── Misc ───────────────────────────────────────────────────────
    var replacement_cost: Double?
    var panel_schedule_status: String?
    var rotation: Double?
    var locked: Bool?

    enum CodingKeys: String, CodingKey {
        case id, type, label, sld_id, parent_id, x, y, width, height
        case is_deleted, location, node_class, node_subtype, core_attributes, node_terminals, com, qr_code, serviceability, serviceability_note, af_completion, room_id
        case voltage, voltage_id, secondary_voltage, secondary_voltage_id
        case tertiary_voltage, tertiary_voltage_id, system_voltage_id, circuit_voltage_id, voltage_user_overridden
        case notes, com_calculation, default_photo_id, suggested_shortcut, applied_shortcut, is_collapsed
        case eqp_lib, eqp_lib_suggested, eqp_note, eqp_engineering_approved
        case skm_lib_name, skm_lib_name_suggested, ocr_signature
        case kva_rating, percent_impedance
        case mains_type_id, phase_configuration_id
        case ampere_rating, pole_count, manufacturer_id, has_trip_unit, trip_type_id
        case frame_amps, sensor_amps, plug_amps
        case length, conductor_material, cable_size_id, conductor_configuration_id, duct_material_id
        case conductor_description_id, insulation_class_id, insulation_type_id, installation_id
        case busway_ampere_rating
        case replacement_cost, panel_schedule_status, rotation, locked
    }

    // Standard initializer for programmatic creation. Engineering
    // fields (ZP-2161) default to nil so existing call sites keep
    // working — new constructions that need engineering data should
    // assign properties directly after init or use the decoder.
    init(id: UUID, type: String, label: String, sld_id: UUID, parent_id: UUID? = nil,
         x: Double, y: Double, width: Double, height: Double, is_deleted: Bool = false,
         location: String? = nil, node_class: UUID? = nil, node_subtype: UUID? = nil, core_attributes: [NodePropertyDTO] = [],
         node_terminals: [NodeTerminalDTO] = [],
         com: Int? = nil, com_calculation: COMCalculationDTO? = nil, qr_code: String? = nil, serviceability: String? = nil, serviceability_note: String? = nil, voltage: Double? = nil, voltage_id: Int? = nil, secondary_voltage: Double? = nil, secondary_voltage_id: Int? = nil, notes: String? = nil, af_completion: Bool? = nil, room_id: UUID? = nil, default_photo_id: UUID? = nil, suggested_shortcut: UUID? = nil, is_collapsed: Bool? = nil, eqp_lib: String? = nil) {
        self.id = id
        self.type = type
        self.label = label
        self.sld_id = sld_id
        self.parent_id = parent_id
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.is_deleted = is_deleted
        self.location = location
        self.node_class = node_class
        self.node_subtype = node_subtype
        self.core_attributes = core_attributes
        self.node_terminals = node_terminals
        self.com = com
        self.com_calculation = com_calculation
        self.qr_code = qr_code
        self.serviceability = serviceability
        self.serviceability_note = serviceability_note
        self.voltage = voltage
        self.voltage_id = voltage_id
        self.secondary_voltage = secondary_voltage
        self.secondary_voltage_id = secondary_voltage_id
        self.notes = notes
        self.af_completion = af_completion
        self.room_id = room_id
        self.default_photo_id = default_photo_id
        self.suggested_shortcut = suggested_shortcut
        self.is_collapsed = is_collapsed
        self.eqp_lib = eqp_lib
    }

    // Custom decoder for resilient parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = try container.decode(UUID.self, forKey: .id)
        type = try container.decode(String.self, forKey: .type)
        label = try container.decode(String.self, forKey: .label)
        sld_id = try container.decode(UUID.self, forKey: .sld_id)
        parent_id = try? container.decodeIfPresent(UUID.self, forKey: .parent_id)
        x = try container.decodeIfPresent(Double.self, forKey: .x) ?? 0
        y = try container.decodeIfPresent(Double.self, forKey: .y) ?? 0
        width = try container.decodeIfPresent(Double.self, forKey: .width) ?? 0
        height = try container.decodeIfPresent(Double.self, forKey: .height) ?? 0
        is_deleted = try container.decodeIfPresent(Bool.self, forKey: .is_deleted) ?? false
        location = try? container.decodeIfPresent(String.self, forKey: .location)

        // Resilient parsing for node_class UUID - handle invalid/empty UUIDs gracefully
        if let nodeClassString = try? container.decodeIfPresent(String.self, forKey: .node_class),
           !nodeClassString.isEmpty,
           let uuid = UUID(uuidString: nodeClassString) {
            node_class = uuid
        } else if let uuid = try? container.decodeIfPresent(UUID.self, forKey: .node_class) {
            node_class = uuid
        } else {
            node_class = nil
            if let invalidValue = try? container.decodeIfPresent(String.self, forKey: .node_class), !invalidValue.isEmpty {
                AppLogger.log(.notice, "Node \(id): node_class has invalid UUID '\(invalidValue)', setting to nil", category: .node)
            }
        }

        // Resilient parsing for node_subtype UUID - handle invalid/empty UUIDs gracefully
        if let nodeSubtypeString = try? container.decodeIfPresent(String.self, forKey: .node_subtype),
           !nodeSubtypeString.isEmpty,
           let uuid = UUID(uuidString: nodeSubtypeString) {
            node_subtype = uuid
        } else if let uuid = try? container.decodeIfPresent(UUID.self, forKey: .node_subtype) {
            node_subtype = uuid
        } else {
            node_subtype = nil
        }

        // Resilient parsing for core_attributes - handle dictionary format gracefully
        if let attributesArray = try? container.decode([NodePropertyDTO].self, forKey: .core_attributes) {
            core_attributes = attributesArray
        } else {
            // If it's not an array (e.g., it's a dictionary), set to empty array and log warning
            core_attributes = []
            AppLogger.log(.notice, "Node \(id): core_attributes is not an array, using empty array", category: .node)
        }

        // Resilient parsing for node_terminals
        if let terminalsArray = try? container.decode([NodeTerminalDTO].self, forKey: .node_terminals) {
            node_terminals = terminalsArray
        } else {
            node_terminals = []
        }

        com = try? container.decodeIfPresent(Int.self, forKey: .com)
        com_calculation = try? container.decodeIfPresent(COMCalculationDTO.self, forKey: .com_calculation)
        qr_code = try? container.decodeIfPresent(String.self, forKey: .qr_code)
        serviceability = try? container.decodeIfPresent(String.self, forKey: .serviceability)
        serviceability_note = try? container.decodeIfPresent(String.self, forKey: .serviceability_note)
        voltage = try? container.decodeIfPresent(Double.self, forKey: .voltage)
        voltage_id = try? container.decodeIfPresent(Int.self, forKey: .voltage_id)
        secondary_voltage = try? container.decodeIfPresent(Double.self, forKey: .secondary_voltage)
        secondary_voltage_id = try? container.decodeIfPresent(Int.self, forKey: .secondary_voltage_id)
        notes = try? container.decodeIfPresent(String.self, forKey: .notes)
        af_completion = try? container.decodeIfPresent(Bool.self, forKey: .af_completion)
        room_id = try? container.decodeIfPresent(UUID.self, forKey: .room_id)
        default_photo_id = try? container.decodeIfPresent(UUID.self, forKey: .default_photo_id)
        suggested_shortcut = try? container.decodeIfPresent(UUID.self, forKey: .suggested_shortcut)
        is_collapsed = try? container.decodeIfPresent(Bool.self, forKey: .is_collapsed)

        // eqp_lib: Accept either a JSON string or a raw JSON object, normalise to string
        if let str = try? container.decodeIfPresent(String.self, forKey: .eqp_lib) {
            eqp_lib = str
        } else if container.contains(.eqp_lib) {
            // Raw JSON object — re-encode the raw JSON fragment to a string
            if let rawData = try? container.decode(EqpLibRawJSON.self, forKey: .eqp_lib).jsonString {
                eqp_lib = rawData
            } else {
                eqp_lib = nil
            }
        } else {
            eqp_lib = nil
        }

        // ── ZP-2161 engineering fields ─────────────────────────────
        tertiary_voltage = try? container.decodeIfPresent(Double.self, forKey: .tertiary_voltage)
        tertiary_voltage_id = try? container.decodeIfPresent(Int.self, forKey: .tertiary_voltage_id)
        system_voltage_id = try? container.decodeIfPresent(Int.self, forKey: .system_voltage_id)
        circuit_voltage_id = try? container.decodeIfPresent(Int.self, forKey: .circuit_voltage_id)
        voltage_user_overridden = try? container.decodeIfPresent(Bool.self, forKey: .voltage_user_overridden)
        applied_shortcut = try? container.decodeIfPresent(UUID.self, forKey: .applied_shortcut)

        // eqp_lib_suggested + ocr_signature: same string-or-object pattern as eqp_lib
        eqp_lib_suggested = SLDDTONode.decodeStringOrJSONObject(
            container: container, key: .eqp_lib_suggested
        )
        ocr_signature = SLDDTONode.decodeStringOrJSONObject(
            container: container, key: .ocr_signature
        )
        eqp_note = try? container.decodeIfPresent(String.self, forKey: .eqp_note)
        eqp_engineering_approved = try? container.decodeIfPresent(Bool.self, forKey: .eqp_engineering_approved)
        skm_lib_name = try? container.decodeIfPresent(String.self, forKey: .skm_lib_name)
        skm_lib_name_suggested = try? container.decodeIfPresent(String.self, forKey: .skm_lib_name_suggested)

        kva_rating = try? container.decodeIfPresent(Double.self, forKey: .kva_rating)
        percent_impedance = try? container.decodeIfPresent(Double.self, forKey: .percent_impedance)

        mains_type_id = try? container.decodeIfPresent(Int.self, forKey: .mains_type_id)
        phase_configuration_id = try? container.decodeIfPresent(Int.self, forKey: .phase_configuration_id)

        ampere_rating = try? container.decodeIfPresent(Int.self, forKey: .ampere_rating)
        pole_count = try? container.decodeIfPresent(Int.self, forKey: .pole_count)
        manufacturer_id = try? container.decodeIfPresent(Int.self, forKey: .manufacturer_id)
        has_trip_unit = try? container.decodeIfPresent(Bool.self, forKey: .has_trip_unit)
        trip_type_id = try? container.decodeIfPresent(Int.self, forKey: .trip_type_id)
        frame_amps = try? container.decodeIfPresent(Int.self, forKey: .frame_amps)
        sensor_amps = try? container.decodeIfPresent(Int.self, forKey: .sensor_amps)
        plug_amps = try? container.decodeIfPresent(Int.self, forKey: .plug_amps)

        length = try? container.decodeIfPresent(Double.self, forKey: .length)
        conductor_material = try? container.decodeIfPresent(String.self, forKey: .conductor_material)
        cable_size_id = try? container.decodeIfPresent(Int.self, forKey: .cable_size_id)
        conductor_configuration_id = try? container.decodeIfPresent(Int.self, forKey: .conductor_configuration_id)
        duct_material_id = try? container.decodeIfPresent(Int.self, forKey: .duct_material_id)
        conductor_description_id = try? container.decodeIfPresent(Int.self, forKey: .conductor_description_id)
        insulation_class_id = try? container.decodeIfPresent(Int.self, forKey: .insulation_class_id)
        insulation_type_id = try? container.decodeIfPresent(Int.self, forKey: .insulation_type_id)
        installation_id = try? container.decodeIfPresent(Int.self, forKey: .installation_id)
        busway_ampere_rating = try? container.decodeIfPresent(Int.self, forKey: .busway_ampere_rating)

        replacement_cost = try? container.decodeIfPresent(Double.self, forKey: .replacement_cost)
        panel_schedule_status = try? container.decodeIfPresent(String.self, forKey: .panel_schedule_status)
        rotation = try? container.decodeIfPresent(Double.self, forKey: .rotation)
        locked = try? container.decodeIfPresent(Bool.self, forKey: .locked)
    }

    /// Decodes a JSONB-shaped field that may arrive as either a JSON
    /// string or a raw JSON object. Returns the canonical string form
    /// (or nil if absent / unparseable) so callers can treat the value
    /// uniformly.
    private static func decodeStringOrJSONObject(
        container: KeyedDecodingContainer<CodingKeys>,
        key: CodingKeys
    ) -> String? {
        if let str = try? container.decodeIfPresent(String.self, forKey: key) {
            return str
        }
        guard container.contains(key) else { return nil }
        // Try generic JSON object → re-encode to canonical string
        if let data = try? container.decode(AnyJSON.self, forKey: key).rawData,
           let str = String(data: data, encoding: .utf8) {
            return str
        }
        return nil
    }
}

/// Helper to capture an arbitrary JSON value and round-trip it as a
/// canonical UTF-8 string. Used by ``SLDDTONode`` for fields like
/// ``ocr_signature`` and ``eqp_lib_suggested`` that the backend emits
/// as JSON objects but we store as strings on the SwiftData model.
private struct AnyJSON: Decodable {
    let rawData: Data

    init(from decoder: Decoder) throws {
        // Decode into Foundation JSON-compatible types, then re-encode.
        let container = try decoder.singleValueContainer()
        if let dict = try? container.decode([String: JSONValue].self) {
            rawData = (try? JSONSerialization.data(
                withJSONObject: dict.mapValues(\.unwrapped),
                options: [.sortedKeys]
            )) ?? Data()
        } else if let arr = try? container.decode([JSONValue].self) {
            rawData = (try? JSONSerialization.data(
                withJSONObject: arr.map(\.unwrapped),
                options: [.sortedKeys]
            )) ?? Data()
        } else {
            rawData = Data()
        }
    }
}

private enum JSONValue: Decodable {
    case null
    case bool(Bool)
    case int(Int)
    case double(Double)
    case string(String)
    case array([JSONValue])
    case object([String: JSONValue])

    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if c.decodeNil() { self = .null; return }
        if let v = try? c.decode(Bool.self) { self = .bool(v); return }
        if let v = try? c.decode(Int.self) { self = .int(v); return }
        if let v = try? c.decode(Double.self) { self = .double(v); return }
        if let v = try? c.decode(String.self) { self = .string(v); return }
        if let v = try? c.decode([JSONValue].self) { self = .array(v); return }
        if let v = try? c.decode([String: JSONValue].self) { self = .object(v); return }
        self = .null
    }

    var unwrapped: Any {
        switch self {
        case .null: return NSNull()
        case .bool(let v): return v
        case .int(let v): return v
        case .double(let v): return v
        case .string(let v): return v
        case .array(let v): return v.map(\.unwrapped)
        case .object(let v): return v.mapValues(\.unwrapped)
        }
    }
}

/// Helper to capture a raw JSON value and re-serialize it as a string.
/// Used only for eqp_lib which can arrive as either a string or an object.
private struct EqpLibRawJSON: Decodable {
    let jsonString: String?

    init(from decoder: Decoder) throws {
        // Decode as EqpLibSelection (strongly typed), then re-encode to string
        let container = try decoder.singleValueContainer()
        if let selection = try? container.decode(EqpLibSelection.self),
           let data = try? JSONEncoder().encode(selection) {
            jsonString = String(data: data, encoding: .utf8)
        } else {
            jsonString = nil
        }
    }
}