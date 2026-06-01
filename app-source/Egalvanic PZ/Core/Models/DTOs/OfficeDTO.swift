//
//  OfficeDTO.swift
//  Egalvanic PZ
//

import Foundation

struct OfficeDTO: Codable, Identifiable {
    let id: String
    let name: String
    let language: String?
    let isDeleted: Bool

    enum CodingKeys: String, CodingKey {
        case id, name, language
        case isDeleted = "is_deleted"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = try container.decode(String.self, forKey: .id)
        self.name = try container.decode(String.self, forKey: .name)
        self.language = try container.decodeIfPresent(String.self, forKey: .language)
        self.isDeleted = try container.decodeIfPresent(Bool.self, forKey: .isDeleted) ?? false
    }
}

struct OfficeListResponse: Codable {
    let data: [OfficeDTO]
}
