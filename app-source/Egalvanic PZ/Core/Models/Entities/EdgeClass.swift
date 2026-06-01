//
//  EdgeClass.swift
//  SwiftDataTutorial
//
import Foundation
import SwiftData

@Model
final class EdgeClass {
    @Attribute(.unique) var id: UUID
    var name: String
    var definition: [EdgeClassProperty]
    var is_deleted: Bool = false
    
    init(id: UUID, name: String, definition: [EdgeClassProperty], is_deleted: Bool = false) {
        self.id = id
        self.name = name
        self.definition = definition
        self.is_deleted = is_deleted
    }
}

@Model
final class EdgeClassProperty {
    @Attribute(.unique) var id: UUID
    var name: String
    var type: String
    var options: [String?]
    var af_required: Bool = false
    var index: Int?
    var default_value: String?
    var edgeClass: EdgeClass?

    init(id: UUID, name: String, type: String, options: [String?] = [], af_required: Bool, index: Int? = nil, default_value: String? = nil, edgeClass: EdgeClass) {
        self.id = id
        self.name = name
        self.type = type
        self.options = options
        self.af_required = af_required
        self.index = index
        self.default_value = default_value
        self.edgeClass = edgeClass
    }
}

@Model
final class EdgeProperty {
    var id: UUID
    var edge_class_property: EdgeClassProperty?
    var name: String
    var value: String
    
    init(id: UUID, edge_class_property: EdgeClassProperty, name: String, value: String) {
        self.id = id
        self.edge_class_property = edge_class_property
        self.name = name
        self.value = value
    }
}