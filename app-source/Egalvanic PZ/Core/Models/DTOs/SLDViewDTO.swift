//
//  SLDViewDTO.swift
//  Egalvanic PZ
//
//  DTO for SLD View data
//
import Foundation

struct SLDViewDTO: Codable {
    var id: String
    var sld_id: String
    var name: String
    var description: String?
    var created_by: String?
    var view_type: String  // 'default', 'location', 'custom'
    var is_default: Bool
    var is_deleted: Bool
    var created_at: String?
    var modified_at: String?
}
