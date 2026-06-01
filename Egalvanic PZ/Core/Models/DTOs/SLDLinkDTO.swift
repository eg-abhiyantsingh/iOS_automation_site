//
//  SLDLinkDTO.swift
//  Egalvanic PZ
//
//  DTO for SLD Link data (cross-view navigation)
//
import Foundation

struct SLDLinkDTO: Codable {
    var id: String
    var source_sld_view_id: String
    var target_sld_view_id: String
    var source_node_id: String?
    var target_node_id: String?
    var source_x: Double?
    var source_y: Double?
    var target_x: Double?
    var target_y: Double?
    var edge_direction: String?  // 'downstream' or 'upstream'
    var source_view_handle: String?
    var target_view_handle: String?
    var is_deleted: Bool
    var created_at: String?
    var modified_at: String?

    // Enriched fields from backend (optional)
    var target_view_name: String?
    var target_node_label: String?
    var target_node_parent_label: String?
}
