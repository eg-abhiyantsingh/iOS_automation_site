//
//  LocationDTOs.swift
//  Egalvanic PZ
//
//  DTOs for location hierarchy (Building, Floor, Room)
//

import Foundation

// MARK: - Building DTO

struct BuildingDTO: Codable {
    var id: String
    var name: String
    var sld_id: String
    var is_deleted: Bool
    var access_notes: String?
}

// MARK: - Floor DTO

struct FloorDTO: Codable {
    var id: String
    var name: String
    var building_id: String
    var is_deleted: Bool
    var access_notes: String?
}

// MARK: - Room DTO

struct RoomDTO: Codable {
    var id: String
    var name: String
    var floor_id: String
    var is_deleted: Bool
    var access_notes: String?
}

// MARK: - SLD DTO Extension for Locations

extension SLDMappingsDTO {
    // Add computed properties for location arrays if needed
}
