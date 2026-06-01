//
//  Room.swift
//  Egalvanic PZ
//
//  Location hierarchy: Room model
//

import SwiftUI
import SwiftData

@Model
final class Room {
    var id: UUID
    var name: String
    var floor: Floor?
    var is_deleted: Bool = false
    var access_notes: String?
    @Relationship(inverse: \NodeV2.room) var nodes: [NodeV2] = []
    @Relationship(inverse: \Photo.room) var photos: [Photo] = []
    var lastSyncedAt: Date?
    var needsSync: Bool = false

    init(id: UUID, name: String, floor: Floor? = nil, is_deleted: Bool = false, access_notes: String? = nil, nodes: [NodeV2] = [], photos: [Photo] = []) {
        self.id = id
        self.name = name
        self.floor = floor
        self.is_deleted = is_deleted
        self.access_notes = access_notes
        self.nodes = nodes
        self.photos = photos
    }

    /// Helper to get the full location path (Building > Floor > Room)
    var fullPath: String {
        guard let floor = floor, let building = floor.building else {
            return name
        }
        return "\(building.name) > \(floor.name) > \(name)"
    }
}
