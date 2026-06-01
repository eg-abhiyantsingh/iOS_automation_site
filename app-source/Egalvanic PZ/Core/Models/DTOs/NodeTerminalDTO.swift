//
//  NodeTerminalDTO.swift
//  Egalvanic PZ
//
//  DTO for node terminal data from API
//

import Foundation

/// DTO representing a node terminal instance from the API
/// Node terminals are actual terminals instantiated per asset/node,
/// derived from NodeOrientationTerminal blueprints
struct NodeTerminalDTO: Codable {
    var id: UUID
    var node_id: UUID
    var node_orientation_terminal_id: UUID
    var handle_code: String?
    var label: String?
    var side: String?  // "LINE" = input/target, "LOAD" = output/source
    var position: String?  // "top", "bottom", "left", "right"
    var offset_percent: Double?
    var color: String?
    var show_label: Bool?
    var voltage_rating: Double?
    var amp_rating: Double?
    var phase_count: Int?
    var is_deleted: Bool
    var created_at: String?
    var updated_at: String?

    enum CodingKeys: String, CodingKey {
        case id, node_id, node_orientation_terminal_id
        case handle_code, label, side, position, offset_percent
        case color, show_label, voltage_rating, amp_rating, phase_count
        case is_deleted, created_at, updated_at
    }

    // Standard initializer for programmatic creation
    init(
        id: UUID,
        node_id: UUID,
        node_orientation_terminal_id: UUID,
        handle_code: String? = nil,
        label: String? = nil,
        side: String? = nil,
        position: String? = nil,
        offset_percent: Double? = nil,
        color: String? = nil,
        show_label: Bool? = nil,
        voltage_rating: Double? = nil,
        amp_rating: Double? = nil,
        phase_count: Int? = nil,
        is_deleted: Bool = false,
        created_at: String? = nil,
        updated_at: String? = nil
    ) {
        self.id = id
        self.node_id = node_id
        self.node_orientation_terminal_id = node_orientation_terminal_id
        self.handle_code = handle_code
        self.label = label
        self.side = side
        self.position = position
        self.offset_percent = offset_percent
        self.color = color
        self.show_label = show_label
        self.voltage_rating = voltage_rating
        self.amp_rating = amp_rating
        self.phase_count = phase_count
        self.is_deleted = is_deleted
        self.created_at = created_at
        self.updated_at = updated_at
    }

    // Custom decoder for resilient parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = try container.decode(UUID.self, forKey: .id)
        node_id = try container.decode(UUID.self, forKey: .node_id)
        node_orientation_terminal_id = try container.decode(UUID.self, forKey: .node_orientation_terminal_id)
        handle_code = try? container.decodeIfPresent(String.self, forKey: .handle_code)
        label = try? container.decodeIfPresent(String.self, forKey: .label)
        side = try? container.decodeIfPresent(String.self, forKey: .side)
        position = try? container.decodeIfPresent(String.self, forKey: .position)
        offset_percent = try? container.decodeIfPresent(Double.self, forKey: .offset_percent)
        color = try? container.decodeIfPresent(String.self, forKey: .color)
        show_label = try? container.decodeIfPresent(Bool.self, forKey: .show_label)
        voltage_rating = try? container.decodeIfPresent(Double.self, forKey: .voltage_rating)
        amp_rating = try? container.decodeIfPresent(Double.self, forKey: .amp_rating)
        phase_count = try? container.decodeIfPresent(Int.self, forKey: .phase_count)
        is_deleted = try container.decodeIfPresent(Bool.self, forKey: .is_deleted) ?? false
        created_at = try? container.decodeIfPresent(String.self, forKey: .created_at)
        updated_at = try? container.decodeIfPresent(String.self, forKey: .updated_at)
    }
}
