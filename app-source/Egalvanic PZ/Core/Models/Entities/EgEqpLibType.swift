//
//  EgEqpLibType.swift
//  Egalvanic PZ
//
//  SwiftData mirror of the backend ``eg_eqp_lib_types`` taxonomy table.
//  Used by the Engineering section of the asset form to resolve a
//  ``NodeClass.eqp_lib_type_id`` into a human-readable label and the
//  ``api_kind`` slug that drives conditional rendering
//  (circuit_breaker / fuse / relay / transformer / cable / busway).
//
//  Synced via ``GET /equipment-library/taxonomy`` on each SLD refresh
//  through ``SLDSyncService.smartUpsertEqpLibTypeDTOs(...)``.
//
import Foundation
import SwiftData

@Model
final class EgEqpLibType {
    @Attribute(.unique) var id: Int
    var name: String
    var display_name: String
    var category_id: Int
    var api_kind: String?
    var api_slug: String?
    var sort_order: Int
    var is_active: Bool

    @Relationship(deleteRule: .cascade, inverse: \EgEqpLibSubtype.type)
    var subtypes: [EgEqpLibSubtype] = []

    init(
        id: Int,
        name: String,
        display_name: String,
        category_id: Int,
        api_kind: String? = nil,
        api_slug: String? = nil,
        sort_order: Int = 0,
        is_active: Bool = true,
        subtypes: [EgEqpLibSubtype] = []
    ) {
        self.id = id
        self.name = name
        self.display_name = display_name
        self.category_id = category_id
        self.api_kind = api_kind
        self.api_slug = api_slug
        self.sort_order = sort_order
        self.is_active = is_active
        self.subtypes = subtypes
    }
}


@Model
final class EgEqpLibSubtype {
    @Attribute(.unique) var id: Int
    var type_id: Int
    var name: String
    var display_name: String
    var api_kind: String?
    var api_slug: String?
    var sort_order: Int
    var is_active: Bool

    var type: EgEqpLibType?

    init(
        id: Int,
        type_id: Int,
        name: String,
        display_name: String,
        api_kind: String? = nil,
        api_slug: String? = nil,
        sort_order: Int = 0,
        is_active: Bool = true,
        type: EgEqpLibType? = nil
    ) {
        self.id = id
        self.type_id = type_id
        self.name = name
        self.display_name = display_name
        self.api_kind = api_kind
        self.api_slug = api_slug
        self.sort_order = sort_order
        self.is_active = is_active
        self.type = type
    }
}
