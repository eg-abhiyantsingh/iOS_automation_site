//
//  Floor.swift
//  Egalvanic PZ
//
//  Location hierarchy: Floor model
//

import SwiftUI
import SwiftData

@Model
final class Floor {
    var id: UUID
    var name: String
    var building: Building?
    var is_deleted: Bool = false
    var access_notes: String?
    @Relationship(inverse: \Room.floor) var rooms: [Room] = []
    @Relationship(inverse: \Photo.floor) var photos: [Photo] = []
    var lastSyncedAt: Date?
    var needsSync: Bool = false

    init(id: UUID, name: String, building: Building? = nil, is_deleted: Bool = false, access_notes: String? = nil, rooms: [Room] = [], photos: [Photo] = []) {
        self.id = id
        self.name = name
        self.building = building
        self.is_deleted = is_deleted
        self.access_notes = access_notes
        self.rooms = rooms
        self.photos = photos
    }
}
