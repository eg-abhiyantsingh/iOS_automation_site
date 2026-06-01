//
//  NodeIcon.swift
//  Egalvanic PZ
//
import Foundation
import SwiftData

@Model
final class NodeIcon {
    @Attribute(.unique) var id: UUID
    var name: String
    var svg: String
    var company_id: UUID?
    var is_deleted: Bool = false

    init(id: UUID, name: String, svg: String, company_id: UUID? = nil, is_deleted: Bool = false) {
        self.id = id
        self.name = name
        self.svg = svg
        self.company_id = company_id
        self.is_deleted = is_deleted
    }
}
