//
//  SLDChoiceDTO.swift
//  SwiftDataTutorial
//
import Foundation

struct SLDChoiceDTO: Codable {
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
    var address_coordinates: [String: Double]?

    // Account, complexity, labor union
    var account_id: String?
    var complexity_level: Double?
    var labor_union_id: String?

    // Office assignment (ZP-2061)
    var office_id: String?
    var office_language: String?
}