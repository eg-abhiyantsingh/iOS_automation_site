//
//  UserTaskDTO.swift
//  SwiftDataTutorial
//
import Foundation

struct UserTaskPoolDTO: Codable {
    var user_tasks: [UserTaskDTO]
}

struct UserTaskDTO: Codable {
    var id: UUID
    var title: String
    var task_description: String
    var completed: Bool
    var form_id: UUID?
    var node_id: UUID?
    var sld_id: UUID
    var is_deleted: Bool
    var submission: String?
    var submitted_at: Date?
    var due_date: Date?
    var created_at: Date?
    var task_type: String?
    var interval: Int?
    var recurring: Bool?
    var procedure_id: UUID?
    var shortcut_id: UUID?
}