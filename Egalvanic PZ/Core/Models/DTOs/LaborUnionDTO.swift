//
//  LaborUnionDTO.swift
//  Egalvanic PZ
//

import Foundation

struct LaborUnionDTO: Codable, Identifiable {
    let id: String
    let name: String
}

struct LaborUnionListResponse: Codable {
    let data: [LaborUnionDTO]
}
