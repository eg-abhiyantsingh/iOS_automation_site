//
//  SessionWorkBlockDTO.swift
//  Egalvanic PZ
//
//  Data Transfer Object for session work blocks
//

import Foundation

struct SessionWorkBlockDTO: Codable {
    let id: String
    let session_id: String
    let start_time: String
    let end_time: String
    let work_length: Double?
    let total_days: Int?
    let notes: String?
    let created_at: String?
    let modified_at: String?
    let is_deleted: Bool
    let duration_hours: Double?

    // Additional fields from /user/schedule endpoint
    let session_name: String?
    let session_description: String?
    let sld_id: String?
    let sld_name: String?

    enum CodingKeys: String, CodingKey {
        case id
        case session_id
        case start_time
        case end_time
        case work_length
        case total_days
        case notes
        case created_at
        case modified_at
        case is_deleted
        case duration_hours
        case session_name
        case session_description
        case sld_id
        case sld_name
    }
}

/// Response DTO for /user/schedule endpoint
struct UserScheduleResponseDTO: Codable {
    let success: Bool
    let work_blocks: [SessionWorkBlockDTO]
    let sessions: [UserScheduleSessionDTO]?
}

/// Session info from /user/schedule endpoint
struct UserScheduleSessionDTO: Codable {
    let id: String
    let name: String?
    let description: String?
    let active: Bool?
    let start_date: String?
    let due_date: String?
    let sld: UserScheduleSLDDTO?
}

/// SLD info from /user/schedule endpoint
struct UserScheduleSLDDTO: Codable {
    let id: String?
    let name: String?
}
