//
//  CompanyUser.swift
//  Egalvanic PZ
//

import Foundation

struct CompanyUser: Identifiable, Codable, Hashable {
    let id: UUID
    let email: String?
    let first_name: String?
    let last_name: String?
    let username: String?

    var displayName: String {
        if let first = first_name, !first.isEmpty {
            if let last = last_name, !last.isEmpty {
                return "\(first) \(last)"
            }
            return first
        }
        if let username = username, !username.isEmpty {
            return username
        }
        return email ?? "Unknown"
    }

    private enum CodingKeys: String, CodingKey {
        case id, email, first_name, last_name, username
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        // Backend returns id as String UUID
        let idString = try container.decode(String.self, forKey: .id)
        guard let uuid = UUID(uuidString: idString) else {
            throw DecodingError.dataCorruptedError(forKey: .id, in: container, debugDescription: "Invalid UUID string: \(idString)")
        }
        self.id = uuid
        self.email = try container.decodeIfPresent(String.self, forKey: .email)
        self.first_name = try container.decodeIfPresent(String.self, forKey: .first_name)
        self.last_name = try container.decodeIfPresent(String.self, forKey: .last_name)
        self.username = try container.decodeIfPresent(String.self, forKey: .username)
    }

    init(id: UUID, email: String?, first_name: String?, last_name: String?, username: String?) {
        self.id = id
        self.email = email
        self.first_name = first_name
        self.last_name = last_name
        self.username = username
    }
}
