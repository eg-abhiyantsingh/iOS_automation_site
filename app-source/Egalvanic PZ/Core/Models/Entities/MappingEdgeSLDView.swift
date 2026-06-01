//
//  MappingEdgeSLDView.swift
//  Egalvanic PZ
//
//  SwiftData model for edge-view routing mappings
//
import SwiftUI
import SwiftData

@Model
final class MappingEdgeSLDView {
    @Attribute(.unique) var id: UUID
    var edge_id: UUID
    var sld_view_id: UUID
    var points: [EdgePoint]?
    var algorithm: String?
    var is_deleted: Bool

    init(
        id: UUID,
        edge_id: UUID,
        sld_view_id: UUID,
        points: [EdgePoint]? = nil,
        algorithm: String? = nil,
        is_deleted: Bool = false
    ) {
        self.id = id
        self.edge_id = edge_id
        self.sld_view_id = sld_view_id
        self.points = points
        self.algorithm = algorithm
        self.is_deleted = is_deleted
    }
}
