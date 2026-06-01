//
//  Building.swift
//  Egalvanic PZ
//
//  Location hierarchy: Building model
//

import SwiftUI
import SwiftData

@Model
final class Building {
    var id: UUID
    var name: String
    var sld: SLDV2?
    var is_deleted: Bool = false
    var access_notes: String?
    @Relationship(inverse: \Floor.building) var floors: [Floor] = []
    @Relationship(inverse: \Photo.building) var photos: [Photo] = []
    var lastSyncedAt: Date?
    var needsSync: Bool = false

    init(id: UUID, name: String, sld: SLDV2? = nil, is_deleted: Bool = false, access_notes: String? = nil, floors: [Floor] = [], photos: [Photo] = []) {
        self.id = id
        self.name = name
        self.sld = sld
        self.is_deleted = is_deleted
        self.access_notes = access_notes
        self.floors = floors
        self.photos = photos
    }
}
