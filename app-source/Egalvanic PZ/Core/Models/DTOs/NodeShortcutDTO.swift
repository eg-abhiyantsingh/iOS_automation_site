//
//  NodeShortcutDTO.swift
//  Egalvanic PZ
//
//  DTO for User Shortcut from API
//  Used for quick asset creation with predefined settings
//

import Foundation

struct NodeShortcutDTO: Codable {
    var id: UUID
    var name: String
    var company_id: UUID?
    var node_class_id: UUID
    var node_subtype_id: UUID?
    var node_subtype: String?  // Subtype name (for display purposes)
    var for_entity: UUID?
    var is_global: Bool
    var global_default: Bool
    var is_default: Bool
    var is_deleted: Bool
    var is_override: Bool
    var created_at: String?  // ISO 8601 date string
    var modified_at: String?  // ISO 8601 date string

    enum CodingKeys: String, CodingKey {
        case id, name, company_id, node_class_id, node_subtype_id, node_subtype
        case for_entity, is_global, global_default, is_default, is_deleted
        case is_override, created_at, modified_at
    }

    // Defensive decoding
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        company_id = try? container.decodeIfPresent(UUID.self, forKey: .company_id)
        node_class_id = try container.decode(UUID.self, forKey: .node_class_id)
        node_subtype_id = try? container.decodeIfPresent(UUID.self, forKey: .node_subtype_id)
        node_subtype = try? container.decodeIfPresent(String.self, forKey: .node_subtype)
        for_entity = try? container.decodeIfPresent(UUID.self, forKey: .for_entity)
        is_global = try container.decodeIfPresent(Bool.self, forKey: .is_global) ?? false
        global_default = try container.decodeIfPresent(Bool.self, forKey: .global_default) ?? false
        is_default = try container.decodeIfPresent(Bool.self, forKey: .is_default) ?? false
        is_deleted = try container.decodeIfPresent(Bool.self, forKey: .is_deleted) ?? false
        is_override = try container.decodeIfPresent(Bool.self, forKey: .is_override) ?? false
        created_at = try? container.decodeIfPresent(String.self, forKey: .created_at)
        modified_at = try? container.decodeIfPresent(String.self, forKey: .modified_at)
    }

    func toModel() -> NodeShortcut {
        // Parse ISO 8601 dates
        let dateFormatter = ISO8601DateFormatter()
        let createdDate = created_at.flatMap { dateFormatter.date(from: $0) }
        let modifiedDate = modified_at.flatMap { dateFormatter.date(from: $0) }

        return NodeShortcut(
            id: id,
            name: name,
            company_id: company_id,
            node_class_id: node_class_id,
            node_subtype_id: node_subtype_id,
            for_entity: for_entity,
            is_global: is_global,
            global_default: global_default,
            is_default: is_default,
            is_deleted: is_deleted,
            is_override: is_override,
            created_at: createdDate,
            modified_at: modifiedDate
        )
    }
}
