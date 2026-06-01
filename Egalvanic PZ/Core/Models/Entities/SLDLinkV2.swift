//
//  SLDLinkV2.swift
//  Egalvanic PZ
//
//  SwiftData model for SLD Links (cross-view navigation)
//
import SwiftUI
import SwiftData

@Model
final class SLDLinkV2 {
    @Attribute(.unique) var id: UUID
    var source_sld_view_id: UUID
    var target_sld_view_id: UUID
    var source_node_id: UUID?
    var target_node_id: UUID?
    var source_x: Double?
    var source_y: Double?
    var target_x: Double?
    var target_y: Double?
    var edge_direction: String?  // 'downstream' or 'upstream'
    var source_view_handle: String?
    var target_view_handle: String?
    var is_deleted: Bool
    var created_at: Date?
    var modified_at: Date?

    // Enriched fields (optional, may be nil if not provided)
    var target_view_name: String?
    var target_node_label: String?
    var target_node_parent_label: String?

    init(
        id: UUID,
        source_sld_view_id: UUID,
        target_sld_view_id: UUID,
        source_node_id: UUID? = nil,
        target_node_id: UUID? = nil,
        source_x: Double? = nil,
        source_y: Double? = nil,
        target_x: Double? = nil,
        target_y: Double? = nil,
        edge_direction: String? = nil,
        source_view_handle: String? = nil,
        target_view_handle: String? = nil,
        is_deleted: Bool = false,
        created_at: Date? = nil,
        modified_at: Date? = nil,
        target_view_name: String? = nil,
        target_node_label: String? = nil,
        target_node_parent_label: String? = nil
    ) {
        self.id = id
        self.source_sld_view_id = source_sld_view_id
        self.target_sld_view_id = target_sld_view_id
        self.source_node_id = source_node_id
        self.target_node_id = target_node_id
        self.source_x = source_x
        self.source_y = source_y
        self.target_x = target_x
        self.target_y = target_y
        self.edge_direction = edge_direction
        self.source_view_handle = source_view_handle
        self.target_view_handle = target_view_handle
        self.is_deleted = is_deleted
        self.created_at = created_at
        self.modified_at = modified_at
        self.target_view_name = target_view_name
        self.target_node_label = target_node_label
        self.target_node_parent_label = target_node_parent_label
    }
}
