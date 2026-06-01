//
//  IRSessionDTO.swift
//  SwiftDataTutorial
//
import Foundation

struct IRSessionDTO: Codable {
    var id: UUID
    var name: String
    var description: String?
    var photo_type: String
    var active_visual_prefix: String
    var active_ir_prefix: String
    var date_created: Date
    var date_closed: Date?
    var active: Bool
    var sld_id: UUID
    var is_deleted: Bool
    var work_blocks: [SessionWorkBlockDTO]?
    var equipment_ids: [String]?

    /// Convert equipment_ids strings to UUIDs (for inbound sync)
    var equipmentIdUUIDs: [UUID] {
        (equipment_ids ?? []).compactMap { UUID(uuidString: $0) }
    }
}

struct IRPhotoDTO: Codable {
    var id: UUID
    var ir_session_id: UUID?
    var node_id: UUID
    var visual_photo_key: String
    var ir_photo_key: String
    var date_created: Date
    var sld_id: UUID
    var issue_id: UUID?
    var is_deleted: Bool
}

/// Response from GET /ir_session/{id}/full
struct IRSessionFullResponse: Codable {
    struct Data: Codable {
        var photos: [IRPhotoDTO]?
    }
    var data: Data
}