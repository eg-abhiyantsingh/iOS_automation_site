//
//  UserTaskForm.swift
//  SwiftDataTutorial
//
import Foundation
import SwiftData

@Model
final class UserTaskForm {
    @Attribute(.unique) var id: UUID
    var schema: String  // Store as JSON string
    var title: String
    var is_global: Bool
    var is_deleted: Bool
    var node_class_id: UUID?
    var node_subtype: String?

    // Computed property to get schema as Data for WebView
    var schemaData: Data? {
        schema.data(using: .utf8)
    }

    // Computed property to validate it's proper JSON
    var isValidJSON: Bool {
        guard let data = schemaData else { return false }
        return (try? JSONSerialization.jsonObject(with: data)) != nil
    }

    init(id: UUID, schema: String, title: String, is_global: Bool, is_deleted: Bool, node_class_id: UUID? = nil, node_subtype: String? = nil) {
        self.id = id
        self.schema = schema
        self.title = title
        self.is_global = is_global
        self.is_deleted = is_deleted
        self.node_class_id = node_class_id
        self.node_subtype = node_subtype
    }

    // Convenience init from DTO
    convenience init(from dto: UserTaskFormDTO) {
        self.init(
            id: dto.id,
            schema: dto.schema,
            title: dto.title,
            is_global: dto.is_global,
            is_deleted: dto.is_deleted,
            node_class_id: dto.node_class_id,
            node_subtype: dto.node_subtype
        )
    }
}