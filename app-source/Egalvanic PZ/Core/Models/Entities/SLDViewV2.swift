//
//  SLDViewV2.swift
//  Egalvanic PZ
//
//  SwiftData model for SLD Views
//
import SwiftUI
import SwiftData

@Model
final class SLDViewV2 {
    @Attribute(.unique) var id: UUID
    var sld_id: UUID
    var name: String
    var viewDescription: String?  // 'description' is reserved in Swift
    var created_by: UUID?
    var view_type: String  // 'default', 'location', 'custom'
    var is_default: Bool
    var is_deleted: Bool
    var created_at: Date?
    var modified_at: Date?

    init(
        id: UUID,
        sld_id: UUID,
        name: String,
        viewDescription: String? = nil,
        created_by: UUID? = nil,
        view_type: String = "custom",
        is_default: Bool = false,
        is_deleted: Bool = false,
        created_at: Date? = nil,
        modified_at: Date? = nil
    ) {
        self.id = id
        self.sld_id = sld_id
        self.name = name
        self.viewDescription = viewDescription
        self.created_by = created_by
        self.view_type = view_type
        self.is_default = is_default
        self.is_deleted = is_deleted
        self.created_at = created_at
        self.modified_at = modified_at
    }
}
