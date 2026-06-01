//
//  NodeClass.swift
//  SwiftDataTutorial
//
import Foundation
import SwiftData

// MARK: - Codable Structs for SwiftData Storage

/// SKMConfig is stored as embedded JSON (matching Android pattern)
struct SKMConfigData: Codable {
    var skm_type: String?
    var fields: [String: String]?

    init(skm_type: String? = nil, fields: [String: String]? = nil) {
        self.skm_type = skm_type
        self.fields = fields
    }
}

// MARK: - SwiftData Models

@Model
final class NodeClass {
    @Attribute(.unique) var id: UUID
    var name: String
    var style: String
    var box: Bool
    var definition: [NodeClassProperty]
    var ocp: Bool
    var width: Double
    var height: Double
    var color: String
    var needs_source: Bool?
    var is_deleted: Bool = false
    @Relationship(deleteRule: .nullify, inverse: \NodeSubtype.nodeClass) var node_subtypes: [NodeSubtype]?

    // Orientation and icon - stored as FK references to separate entities
    var node_orientation_id: UUID?
    var node_orientation: NodeOrientation?
    var icon_id: UUID?

    // Additional fields
    var orientation: String = "vertical"
    var in_ports: Int = 1
    var out_ports: Int = 1
    var company_id: UUID?
    var is_global: Bool?
    var is_override: Bool?
    var for_entity: UUID?
    var skm_config: SKMConfigData?
    var default_datablock_config: String?
    var use_eqp_lib: Bool = false
    var primary_secondary_voltage: Bool = false

    // ZP-2161 — new fields that drive the Engineering section's
    // conditional rendering. All optional / defaulted so the existing
    // init() signature stays unchanged.
    var tertiary_voltage: Bool = false  // class supports a tertiary winding (3-winding transformers)
    var eqp_lib_type_id: Int? = nil     // FK -> eg_eqp_lib_types.id (circuit_breaker, fuse, relay, transformer, cable, busway, ...)
    var eqp_lib_type_name: String? = nil // denormalized from eg_eqp_lib_types.name so the FE doesn't need a second lookup
    var device_role_id: Int? = nil       // FK -> enum_device_roles.id (2 = protective device)
    var device_role_code: String? = nil  // denormalized enum_device_roles.code
    var is_pseudo_edge: Bool = false     // SLD viewer collapses 1-in/1-out instances into edges outside engineering view
    var is_impedance: Bool = false       // contributes impedance to short-circuit calc
    var is_default_impedance: Bool = false
    var is_node_bus: Bool = false        // bus-bar — single global class, rendered specially
    var has_panel_schedule: Bool = false
    var class_description: String? = nil // human-readable class description (avoid clashing with NSObject.description)

    init(id: UUID, name: String, style: String, box: Bool, definition: [NodeClassProperty],
         ocp: Bool, width: Double, height: Double, color: String,
         needs_source: Bool? = nil, is_deleted: Bool = false, node_subtypes: [NodeSubtype]? = nil,
         node_orientation_id: UUID? = nil, node_orientation: NodeOrientation? = nil, icon_id: UUID? = nil,
         orientation: String = "vertical", in_ports: Int = 1, out_ports: Int = 1,
         company_id: UUID? = nil, is_global: Bool? = nil, is_override: Bool? = nil,
         for_entity: UUID? = nil, skm_config: SKMConfigData? = nil,
         default_datablock_config: String? = nil, use_eqp_lib: Bool = false,
         primary_secondary_voltage: Bool = false) {
        self.id = id
        self.name = name
        self.style = style
        self.box = box
        self.definition = definition
        self.ocp = ocp
        self.width = width
        self.height = height
        self.color = color
        self.needs_source = needs_source
        self.is_deleted = is_deleted
        self.node_subtypes = node_subtypes
        self.node_orientation_id = node_orientation_id
        self.node_orientation = node_orientation
        self.icon_id = icon_id
        self.orientation = orientation
        self.in_ports = in_ports
        self.out_ports = out_ports
        self.company_id = company_id
        self.is_global = is_global
        self.is_override = is_override
        self.for_entity = for_entity
        self.skm_config = skm_config
        self.default_datablock_config = default_datablock_config
        self.use_eqp_lib = use_eqp_lib
        self.primary_secondary_voltage = primary_secondary_voltage
    }
}

@Model
final class NodeClassProperty {
    @Attribute(.unique) var id: UUID
    var name: String
    var type: String
    var options: [String?]
    var af_required: Bool = false
    var index: Int?
    var default_value: String?
    var nodeClass: NodeClass?

    init(id: UUID, name: String, type: String, options: [String?] = [], af_required: Bool, index: Int? = nil, default_value: String? = nil, nodeClass: NodeClass) {
        self.id = id
        self.name = name
        self.type = type
        self.options = options
        self.af_required = af_required
        self.index = index
        self.default_value = default_value
        self.nodeClass = nodeClass
    }
}

@Model
final class NodeProperty {
    var id: UUID
    var node_class_property: NodeClassProperty?
    var name: String
    var value: String

    init(id: UUID, node_class_property: NodeClassProperty, name: String, value: String) {
        self.id = id
        self.node_class_property = node_class_property
        self.name = name
        self.value = value
    }
}
