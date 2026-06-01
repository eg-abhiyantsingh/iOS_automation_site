//
//  SLDDTOPhoto.swift
//  SwiftDataTutorial
//
import Foundation

struct SLDDTOPhoto: Codable {
    var id: UUID
    var entity_id: UUID?
    var url: String?
    var type: String
    var sld_id: UUID?
    var filename: String?
    var local_filepath: String?
    var upload_needed: Bool
    var is_deleted: Bool
    var caption: String?
}