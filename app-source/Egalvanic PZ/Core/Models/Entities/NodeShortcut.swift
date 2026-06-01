//
//  NodeShortcut.swift
//  Egalvanic PZ
//
//  Entity representing a Node Shortcut for quick asset creation
//  Shortcuts are filtered by nodeClassId and optionally nodeSubtypeId
//

import Foundation
import SwiftData

@Model
final class NodeShortcut {
    @Attribute(.unique) var id: UUID
    var name: String
    var company_id: UUID?
    var node_class_id: UUID
    var node_subtype_id: UUID?  // Nullable for generic shortcuts
    var for_entity: UUID?  // Entity this shortcut is associated with
    var is_global: Bool = false
    var global_default: Bool = false
    var is_default: Bool = false
    var is_deleted: Bool = false
    var is_override: Bool = false
    var created_at: Date?
    var modified_at: Date?

    // Sync tracking fields
    var lastSyncedAt: Date?
    var needsSync: Bool = false

    init(id: UUID, name: String, company_id: UUID?, node_class_id: UUID,
         node_subtype_id: UUID? = nil, for_entity: UUID? = nil,
         is_global: Bool = false, global_default: Bool = false,
         is_default: Bool = false, is_deleted: Bool = false,
         is_override: Bool = false, created_at: Date? = nil,
         modified_at: Date? = nil) {
        self.id = id
        self.name = name
        self.company_id = company_id
        self.node_class_id = node_class_id
        self.node_subtype_id = node_subtype_id
        self.for_entity = for_entity
        self.is_global = is_global
        self.global_default = global_default
        self.is_default = is_default
        self.is_deleted = is_deleted
        self.is_override = is_override
        self.created_at = created_at
        self.modified_at = modified_at
    }
}
