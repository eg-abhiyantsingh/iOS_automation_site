//
//  EgEqpLibTypeDTO.swift
//  Egalvanic PZ
//
//  Mirrors the backend ``EgEqpLibType.to_dict(expand=True)`` shape
//  returned by ``GET /equipment-library/taxonomy``. Lives outside
//  ``SLDDTO`` because the taxonomy is global, not per-SLD.
//
import Foundation

struct EgEqpLibTaxonomyResponse: Codable {
    var types: [EgEqpLibTypeDTO]
}

struct EgEqpLibTypeDTO: Codable {
    var id: Int
    var name: String
    var display_name: String
    var category_id: Int
    var api_kind: String?
    var api_slug: String?
    var sort_order: Int
    var is_active: Bool
    var subtypes: [EgEqpLibSubtypeDTO]?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(Int.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        display_name = try container.decode(String.self, forKey: .display_name)
        category_id = try container.decode(Int.self, forKey: .category_id)
        api_kind = try? container.decodeIfPresent(String.self, forKey: .api_kind)
        api_slug = try? container.decodeIfPresent(String.self, forKey: .api_slug)
        sort_order = (try? container.decode(Int.self, forKey: .sort_order)) ?? 0
        is_active = (try? container.decode(Bool.self, forKey: .is_active)) ?? true
        subtypes = try? container.decodeIfPresent([EgEqpLibSubtypeDTO].self, forKey: .subtypes)
    }
}

struct EgEqpLibSubtypeDTO: Codable {
    var id: Int
    var type_id: Int
    var name: String
    var display_name: String
    var api_kind: String?
    var api_slug: String?
    var sort_order: Int
    var is_active: Bool

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(Int.self, forKey: .id)
        type_id = try container.decode(Int.self, forKey: .type_id)
        name = try container.decode(String.self, forKey: .name)
        display_name = try container.decode(String.self, forKey: .display_name)
        api_kind = try? container.decodeIfPresent(String.self, forKey: .api_kind)
        api_slug = try? container.decodeIfPresent(String.self, forKey: .api_slug)
        sort_order = (try? container.decode(Int.self, forKey: .sort_order)) ?? 0
        is_active = (try? container.decode(Bool.self, forKey: .is_active)) ?? true
    }
}
