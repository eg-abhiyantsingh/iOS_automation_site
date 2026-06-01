//
//  AccountDTO.swift
//  Egalvanic PZ
//

import Foundation

struct AccountDTO: Codable, Identifiable {
    let id: UUID
    let name: String
}

struct AccountListResponse: Codable {
    let accounts: [AccountDTO]
}
