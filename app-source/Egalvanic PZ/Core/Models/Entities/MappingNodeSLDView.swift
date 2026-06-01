//
//  MappingNodeSLDView.swift
//  Egalvanic PZ
//
//  SwiftData model for node-view position mappings
//
import SwiftUI
import SwiftData

@Model
final class MappingNodeSLDView {
    @Attribute(.unique) var id: UUID
    var node_id: UUID
    var sld_view_id: UUID
    var x: Double?
    var y: Double?
    var width: Double?
    var height: Double?
    var is_collapsed: Bool
    var is_deleted: Bool
    var created_at: Date?
    var modified_at: Date?

    init(
        id: UUID,
        node_id: UUID,
        sld_view_id: UUID,
        x: Double? = nil,
        y: Double? = nil,
        width: Double? = nil,
        height: Double? = nil,
        is_collapsed: Bool = false,
        is_deleted: Bool = false,
        created_at: Date? = nil,
        modified_at: Date? = nil
    ) {
        self.id = id
        self.node_id = node_id
        self.sld_view_id = sld_view_id
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.is_collapsed = is_collapsed
        self.is_deleted = is_deleted
        self.created_at = created_at
        self.modified_at = modified_at
    }
}
