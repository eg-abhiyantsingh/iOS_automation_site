//
//  NodeClassDTO.swift
//  SwiftDataTutorial
//
import Foundation

struct NodeClassDTO: Codable {
    var id: UUID
    var name: String
    var style: String
    var box: Bool
    var definition: [NodeClassPropertyDTO]
    var ocp: Bool
    var width: Double
    var height: Double
    var color: String
    var needs_source: Bool?
    var is_deleted: Bool?
    var node_subtypes: [NodeSubtypeDTO]?

    // Orientation and icon - can come as nested objects or just IDs
    var node_orientation: NodeOrientationDTO?
    var node_orientation_id: UUID?
    var orientation: String?
    var in_ports: Int?
    var out_ports: Int?
    var icon: NodeIconDTO?
    var icon_id: UUID?

    // Additional fields
    var company_id: UUID?
    var is_global: Bool?
    var is_override: Bool?
    var for_entity: UUID?
    var skm_config: SKMConfigDTO?
    var default_datablock_config: String?
    var use_eqp_lib: Bool?
    var primary_secondary_voltage: Bool?

    // ZP-2161: Engineering-section drivers. These come from the
    // backend NodeClass.to_dict() — all optional so older payloads
    // continue to decode cleanly.
    var tertiary_voltage: Bool?
    var eqp_lib_type_id: Int?
    var eqp_lib_type_name: String?
    var device_role_id: Int?
    var device_role_code: String?
    var device_role_label: String?
    var is_pseudo_edge: Bool?
    var is_impedance: Bool?
    var is_default_impedance: Bool?
    var is_node_bus: Bool?
    var has_panel_schedule: Bool?
    var description: String?

    // Standard initializer for manual construction
    init(id: UUID, name: String, style: String, box: Bool, definition: [NodeClassPropertyDTO],
         ocp: Bool, width: Double, height: Double, color: String,
         needs_source: Bool? = nil, is_deleted: Bool? = nil, node_subtypes: [NodeSubtypeDTO]? = nil,
         node_orientation: NodeOrientationDTO? = nil, node_orientation_id: UUID? = nil,
         orientation: String? = nil, in_ports: Int? = nil, out_ports: Int? = nil,
         icon: NodeIconDTO? = nil, icon_id: UUID? = nil, company_id: UUID? = nil,
         is_global: Bool? = nil, is_override: Bool? = nil, for_entity: UUID? = nil,
         skm_config: SKMConfigDTO? = nil, default_datablock_config: String? = nil,
         use_eqp_lib: Bool? = nil, primary_secondary_voltage: Bool? = nil) {
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
        self.node_orientation = node_orientation
        self.node_orientation_id = node_orientation_id
        self.orientation = orientation
        self.in_ports = in_ports
        self.out_ports = out_ports
        self.icon = icon
        self.icon_id = icon_id
        self.company_id = company_id
        self.is_global = is_global
        self.is_override = is_override
        self.for_entity = for_entity
        self.skm_config = skm_config
        self.default_datablock_config = default_datablock_config
        self.use_eqp_lib = use_eqp_lib
        self.primary_secondary_voltage = primary_secondary_voltage
    }

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        // Required fields - throw if missing
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        style = try container.decode(String.self, forKey: .style)
        box = try container.decode(Bool.self, forKey: .box)
        ocp = try container.decode(Bool.self, forKey: .ocp)
        width = try container.decode(Double.self, forKey: .width)
        height = try container.decode(Double.self, forKey: .height)
        color = try container.decode(String.self, forKey: .color)

        // Optional fields
        needs_source = try? container.decode(Bool.self, forKey: .needs_source)
        is_deleted = try? container.decode(Bool.self, forKey: .is_deleted)

        // Defensive decoding for definition array
        if container.contains(.definition) {
            do {
                definition = try container.decode([NodeClassPropertyDTO].self, forKey: .definition)
            } catch {
                AppLogger.log(.notice, "NodeClassDTO: Failed to decode definition array, using empty array. Error: \(error)", category: .node)
                definition = []
            }
        } else {
            AppLogger.log(.notice, "NodeClassDTO: Missing definition field, using empty array", category: .node)
            definition = []
        }

        // Defensive decoding for node_subtypes array
        if container.contains(.node_subtypes) {
            do {
                node_subtypes = try container.decode([NodeSubtypeDTO].self, forKey: .node_subtypes)
            } catch {
                AppLogger.log(.notice, "NodeClassDTO: Failed to decode node_subtypes array, using nil. Error: \(error)", category: .node)
                node_subtypes = nil
            }
        } else {
            node_subtypes = nil
        }

        // Orientation and icon fields - defensive decoding
        node_orientation = try? container.decode(NodeOrientationDTO.self, forKey: .node_orientation)
        node_orientation_id = try? container.decode(UUID.self, forKey: .node_orientation_id)
        orientation = try? container.decode(String.self, forKey: .orientation)
        in_ports = try? container.decode(Int.self, forKey: .in_ports)
        out_ports = try? container.decode(Int.self, forKey: .out_ports)
        icon = try? container.decode(NodeIconDTO.self, forKey: .icon)
        icon_id = try? container.decode(UUID.self, forKey: .icon_id)

        // Additional fields
        company_id = try? container.decode(UUID.self, forKey: .company_id)
        is_global = try? container.decode(Bool.self, forKey: .is_global)
        is_override = try? container.decode(Bool.self, forKey: .is_override)
        for_entity = try? container.decode(UUID.self, forKey: .for_entity)
        skm_config = try? container.decode(SKMConfigDTO.self, forKey: .skm_config)
        default_datablock_config = try? container.decode(String.self, forKey: .default_datablock_config)
        use_eqp_lib = try? container.decode(Bool.self, forKey: .use_eqp_lib)
        primary_secondary_voltage = try? container.decode(Bool.self, forKey: .primary_secondary_voltage)

        // ZP-2161 engineering-section drivers
        tertiary_voltage = try? container.decode(Bool.self, forKey: .tertiary_voltage)
        eqp_lib_type_id = try? container.decode(Int.self, forKey: .eqp_lib_type_id)
        eqp_lib_type_name = try? container.decode(String.self, forKey: .eqp_lib_type_name)
        device_role_id = try? container.decode(Int.self, forKey: .device_role_id)
        device_role_code = try? container.decode(String.self, forKey: .device_role_code)
        device_role_label = try? container.decode(String.self, forKey: .device_role_label)
        is_pseudo_edge = try? container.decode(Bool.self, forKey: .is_pseudo_edge)
        is_impedance = try? container.decode(Bool.self, forKey: .is_impedance)
        is_default_impedance = try? container.decode(Bool.self, forKey: .is_default_impedance)
        is_node_bus = try? container.decode(Bool.self, forKey: .is_node_bus)
        has_panel_schedule = try? container.decode(Bool.self, forKey: .has_panel_schedule)
        description = try? container.decode(String.self, forKey: .description)
    }
}

struct NodeClassPropertyDTO: Codable {
    var id: UUID
    var name: String
    var type: String
    var af_required: Bool
    var options: [String]?
    let index: Int?
    var default_value: String?

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        // Required fields
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        type = try container.decode(String.self, forKey: .type)

        // Optional fields with defensive decoding
        af_required = (try? container.decode(Bool.self, forKey: .af_required)) ?? false
        index = try? container.decode(Int.self, forKey: .index)
        default_value = try? container.decode(String.self, forKey: .default_value)

        // Options - handle various malformed cases
        if container.contains(.options) {
            do {
                options = try container.decode([String]?.self, forKey: .options)
            } catch {
                AppLogger.log(.notice, "NodeClassPropertyDTO: Failed to decode options array, using nil. Error: \(error)", category: .node)
                options = nil
            }
        } else {
            options = nil
        }
    }
}

struct NodePropertyDTO: Codable {
    var id: UUID
    var node_class_property: String
    var name: String
    var value: String?  // Made optional to handle null values from backend
}

struct NodeSubtypeDTO: Codable {
    var id: UUID
    var name: String
    var node_class_id: UUID?
    var is_global: Bool?
    var is_deleted: Bool?
    var company_id: UUID?

    // ZP-2161: subtype-driven engineering fields
    var eqp_lib_subtype_id: Int?
    var volt_floor: Double?
    var volt_ceiling: Double?
    var amp_floor: Double?
    var amp_ceiling: Double?
    var description: String?
    var voltage_level: String?
    var replacement_cost: Double?
    var is_override: Bool?
    var for_entity: UUID?

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        // Required fields
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)

        // Optional fields
        node_class_id = try? container.decode(UUID.self, forKey: .node_class_id)
        is_global = try? container.decode(Bool.self, forKey: .is_global)
        is_deleted = try? container.decode(Bool.self, forKey: .is_deleted)
        company_id = try? container.decode(UUID.self, forKey: .company_id)

        // ZP-2161
        eqp_lib_subtype_id = try? container.decode(Int.self, forKey: .eqp_lib_subtype_id)
        volt_floor = try? container.decode(Double.self, forKey: .volt_floor)
        volt_ceiling = try? container.decode(Double.self, forKey: .volt_ceiling)
        amp_floor = try? container.decode(Double.self, forKey: .amp_floor)
        amp_ceiling = try? container.decode(Double.self, forKey: .amp_ceiling)
        description = try? container.decode(String.self, forKey: .description)
        voltage_level = try? container.decode(String.self, forKey: .voltage_level)
        replacement_cost = try? container.decode(Double.self, forKey: .replacement_cost)
        is_override = try? container.decode(Bool.self, forKey: .is_override)
        for_entity = try? container.decode(UUID.self, forKey: .for_entity)
    }
}

// MARK: - Node Orientation DTOs

struct NodeOrientationDTO: Codable {
    var id: UUID
    var code: String
    var name: String
    var orientationDescription: String?
    var orientation_terminals: [OrientationTerminalDTO]
    var is_deleted: Bool?
    var created_at: String?
    var updated_at: String?

    enum CodingKeys: String, CodingKey {
        case id, code, name
        case orientationDescription = "description"
        case orientation_terminals, is_deleted, created_at, updated_at
    }

    init(id: UUID, code: String, name: String, orientationDescription: String? = nil,
         orientation_terminals: [OrientationTerminalDTO] = [], is_deleted: Bool? = nil,
         created_at: String? = nil, updated_at: String? = nil) {
        self.id = id
        self.code = code
        self.name = name
        self.orientationDescription = orientationDescription
        self.orientation_terminals = orientation_terminals
        self.is_deleted = is_deleted
        self.created_at = created_at
        self.updated_at = updated_at
    }

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = try container.decode(UUID.self, forKey: .id)
        code = try container.decode(String.self, forKey: .code)
        name = try container.decode(String.self, forKey: .name)
        orientationDescription = try? container.decode(String.self, forKey: .orientationDescription)
        is_deleted = try? container.decode(Bool.self, forKey: .is_deleted)
        created_at = try? container.decode(String.self, forKey: .created_at)
        updated_at = try? container.decode(String.self, forKey: .updated_at)

        // Defensive decoding for orientation_terminals array
        if container.contains(.orientation_terminals) {
            do {
                orientation_terminals = try container.decode([OrientationTerminalDTO].self, forKey: .orientation_terminals)
            } catch {
                AppLogger.log(.notice, "NodeOrientationDTO: Failed to decode orientation_terminals array, using empty array. Error: \(error)", category: .node)
                orientation_terminals = []
            }
        } else {
            orientation_terminals = []
        }
    }
}

struct OrientationTerminalDTO: Codable {
    var id: UUID
    var handle_code: String
    var label: String
    var side: String
    var position: String?
    var offset_percent: Double?
    var color: String?
    var show_label: Bool?
    var max_connections: Int
    var node_orientation_id: UUID
    var notes: String?
    var is_deleted: Bool?
    var created_at: String?
    var updated_at: String?

    init(id: UUID, handle_code: String, label: String, side: String,
         position: String? = nil, offset_percent: Double? = nil, color: String? = nil, show_label: Bool? = nil,
         max_connections: Int, node_orientation_id: UUID, notes: String? = nil,
         is_deleted: Bool? = nil, created_at: String? = nil, updated_at: String? = nil) {
        self.id = id
        self.handle_code = handle_code
        self.label = label
        self.side = side
        self.position = position
        self.offset_percent = offset_percent
        self.color = color
        self.show_label = show_label
        self.max_connections = max_connections
        self.node_orientation_id = node_orientation_id
        self.notes = notes
        self.is_deleted = is_deleted
        self.created_at = created_at
        self.updated_at = updated_at
    }

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = try container.decode(UUID.self, forKey: .id)
        handle_code = try container.decode(String.self, forKey: .handle_code)
        label = try container.decode(String.self, forKey: .label)
        side = try container.decode(String.self, forKey: .side)
        position = try? container.decode(String.self, forKey: .position)
        offset_percent = try? container.decode(Double.self, forKey: .offset_percent)
        color = try? container.decode(String.self, forKey: .color)
        show_label = try? container.decode(Bool.self, forKey: .show_label)
        max_connections = (try? container.decode(Int.self, forKey: .max_connections)) ?? 1
        node_orientation_id = try container.decode(UUID.self, forKey: .node_orientation_id)
        notes = try? container.decode(String.self, forKey: .notes)
        is_deleted = try? container.decode(Bool.self, forKey: .is_deleted)
        created_at = try? container.decode(String.self, forKey: .created_at)
        updated_at = try? container.decode(String.self, forKey: .updated_at)
    }
}

struct NodeIconDTO: Codable {
    var id: UUID
    var name: String
    var svg: String
    var company_id: UUID?
    var is_deleted: Bool?

    init(id: UUID, name: String, svg: String, company_id: UUID? = nil, is_deleted: Bool? = nil) {
        self.id = id
        self.name = name
        self.svg = svg
        self.company_id = company_id
        self.is_deleted = is_deleted
    }

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        svg = try container.decode(String.self, forKey: .svg)
        company_id = try? container.decode(UUID.self, forKey: .company_id)
        is_deleted = try? container.decode(Bool.self, forKey: .is_deleted)
    }
}

struct SKMConfigDTO: Codable {
    var skm_type: String?
    var fields: [String: String]?

    init(skm_type: String? = nil, fields: [String: String]? = nil) {
        self.skm_type = skm_type
        self.fields = fields
    }

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        skm_type = try? container.decode(String.self, forKey: .skm_type)
        fields = try? container.decode([String: String].self, forKey: .fields)
    }
}

// MARK: - DTO to Entity Conversions

extension NodeIconDTO {
    /// Creates a new NodeIcon entity from this DTO
    func toEntity() -> NodeIcon {
        NodeIcon(
            id: id,
            name: name,
            svg: svg,
            company_id: company_id,
            is_deleted: is_deleted ?? false
        )
    }
}

extension NodeOrientationDTO {
    /// Creates a new NodeOrientation entity from this DTO (without terminals)
    func toEntity() -> NodeOrientation {
        NodeOrientation(
            id: id,
            code: code,
            name: name,
            orientationDescription: orientationDescription,
            is_deleted: is_deleted ?? false,
            created_at: created_at,
            updated_at: updated_at
        )
    }
}

extension OrientationTerminalDTO {
    /// Creates a new NodeOrientationTerminal entity from this DTO
    func toEntity(nodeOrientation: NodeOrientation? = nil) -> NodeOrientationTerminal {
        NodeOrientationTerminal(
            id: id,
            handle_code: handle_code,
            label: label,
            side: side,
            position: position,
            offset_percent: offset_percent,
            color: color,
            show_label: show_label,
            max_connections: max_connections,
            node_orientation_id: node_orientation_id,
            notes: notes,
            is_deleted: is_deleted ?? false,
            created_at: created_at,
            updated_at: updated_at,
            nodeOrientation: nodeOrientation
        )
    }
}

extension SKMConfigDTO {
    /// Converts to the embedded SKMConfigData struct for NodeClass storage
    func toData() -> SKMConfigData {
        SKMConfigData(
            skm_type: skm_type,
            fields: fields
        )
    }
}

// MARK: - Entity to DTO Conversions (for WebView serialization)

extension NodeIcon {
    func toDTO() -> NodeIconDTO {
        NodeIconDTO(
            id: id,
            name: name,
            svg: svg,
            company_id: company_id,
            is_deleted: is_deleted
        )
    }
}

extension NodeOrientation {
    func toDTO() -> NodeOrientationDTO {
        NodeOrientationDTO(
            id: id,
            code: code,
            name: name,
            orientationDescription: orientationDescription,
            orientation_terminals: orientation_terminals.map { $0.toDTO() },
            is_deleted: is_deleted,
            created_at: created_at,
            updated_at: updated_at
        )
    }
}

extension NodeOrientationTerminal {
    func toDTO() -> OrientationTerminalDTO {
        OrientationTerminalDTO(
            id: id,
            handle_code: handle_code,
            label: label,
            side: side,
            position: position,
            offset_percent: offset_percent,
            color: color,
            show_label: show_label,
            max_connections: max_connections,
            node_orientation_id: node_orientation_id,
            notes: notes,
            is_deleted: is_deleted,
            created_at: created_at,
            updated_at: updated_at
        )
    }
}

extension SKMConfigData {
    func toDTO() -> SKMConfigDTO {
        SKMConfigDTO(
            skm_type: skm_type,
            fields: fields
        )
    }
}
