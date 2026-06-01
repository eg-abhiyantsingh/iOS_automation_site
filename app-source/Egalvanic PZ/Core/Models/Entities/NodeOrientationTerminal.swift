//
//  NodeOrientationTerminal.swift
//  Egalvanic PZ
//
import Foundation
import SwiftData

@Model
final class NodeOrientationTerminal {
    @Attribute(.unique) var id: UUID
    var handle_code: String
    var label: String
    var side: String
    var position: String?
    var offset_percent: Double?
    var color: String?
    var show_label: Bool?
    var max_connections: Int = 1
    var node_orientation_id: UUID
    var notes: String?
    var is_deleted: Bool = false
    var created_at: String?
    var updated_at: String?

    var nodeOrientation: NodeOrientation?

    init(id: UUID, handle_code: String, label: String, side: String,
         position: String? = nil, offset_percent: Double? = nil, color: String? = nil, show_label: Bool? = nil,
         max_connections: Int = 1, node_orientation_id: UUID, notes: String? = nil,
         is_deleted: Bool = false, created_at: String? = nil, updated_at: String? = nil,
         nodeOrientation: NodeOrientation? = nil) {
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
        self.nodeOrientation = nodeOrientation
    }
}
