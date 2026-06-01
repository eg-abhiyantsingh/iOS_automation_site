//
//  NodeOrientation.swift
//  Egalvanic PZ
//
import Foundation
import SwiftData

@Model
final class NodeOrientation {
    @Attribute(.unique) var id: UUID
    var code: String
    var name: String
    var orientationDescription: String?
    var is_deleted: Bool = false
    var created_at: String?
    var updated_at: String?

    @Relationship(deleteRule: .cascade, inverse: \NodeOrientationTerminal.nodeOrientation)
    var orientation_terminals: [NodeOrientationTerminal] = []

    init(id: UUID, code: String, name: String, orientationDescription: String? = nil,
         is_deleted: Bool = false, created_at: String? = nil, updated_at: String? = nil,
         orientation_terminals: [NodeOrientationTerminal] = []) {
        self.id = id
        self.code = code
        self.name = name
        self.orientationDescription = orientationDescription
        self.is_deleted = is_deleted
        self.created_at = created_at
        self.updated_at = updated_at
        self.orientation_terminals = orientation_terminals
    }
}
