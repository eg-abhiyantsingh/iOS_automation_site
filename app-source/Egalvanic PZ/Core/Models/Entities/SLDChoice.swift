//
//  SLDChoice.swift
//  SwiftDataTutorial
//
import SwiftUI
import SwiftData

@Model
final class SLDChoice {
    var id: UUID
    var name: String
    var is_deleted: Bool

    // Address fields
    var address_line_1: String?
    var address_line_2: String?
    var city: String?
    var state_province: String?
    var postal_code: String?
    var country_code: String?
    var address_formatted: String?
    var address_latitude: Double?
    var address_longitude: Double?

    // Account, complexity, labor union
    var account_id: String?
    var complexity_level: Double?
    var labor_union_id: String?

    // Office assignment (ZP-2061)
    var office_id: String?
    var office_language: String?

    init(id: UUID, name: String, is_deleted: Bool, address_line_1: String? = nil, address_line_2: String? = nil, city: String? = nil, state_province: String? = nil, postal_code: String? = nil, country_code: String? = nil, address_formatted: String? = nil, address_latitude: Double? = nil, address_longitude: Double? = nil, account_id: String? = nil, complexity_level: Double? = nil, labor_union_id: String? = nil, office_id: String? = nil, office_language: String? = nil) {
        self.id = id
        self.name = name
        self.is_deleted = is_deleted
        self.address_line_1 = address_line_1
        self.address_line_2 = address_line_2
        self.city = city
        self.state_province = state_province
        self.postal_code = postal_code
        self.country_code = country_code
        self.address_formatted = address_formatted
        self.address_latitude = address_latitude
        self.address_longitude = address_longitude
        self.account_id = account_id
        self.complexity_level = complexity_level
        self.labor_union_id = labor_union_id
        self.office_id = office_id
        self.office_language = office_language
    }
}
