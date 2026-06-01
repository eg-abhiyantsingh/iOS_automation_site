//
//  UserTaskFormDTO.swift
//  SwiftDataTutorial
//
import Foundation

struct UserTaskFormDTO: Codable {
    var id: UUID
    var schema: String
    var title: String
    var is_global: Bool
    var is_deleted: Bool
    var node_class_id: UUID?
    var node_subtype: String?
}