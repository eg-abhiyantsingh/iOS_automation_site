//
//  NodeV2+CoreAttributes.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/28/25.
//

import Foundation

// MARK: - Node Conformances
extension NodeV2: EntityWithCoreAttributes {
    typealias ClassType = NodeClass
    typealias PropertyType = NodeProperty
    
    var entityClass: NodeClass? {
        return node_class
    }
    
    var properties: [NodeProperty] {
        return core_attributes
    }
}

extension NodeClass: EntityClass {
    typealias PropertyDefinitionType = NodeClassProperty
}

extension NodeClassProperty: EntityPropertyDefinition {
    var fieldDescription: String? { nil }
    var columns: [String?]? { nil }
    var internal_type: [String?]? { nil }
    var calculationExpression: String? { nil }
    var calculationPrecision: Int? { nil }
    var defaultValue: String? { default_value }
}

extension NodeProperty: EntityProperty {
    typealias PropertyDefinitionType = NodeClassProperty
    
    var propertyDefinition: NodeClassProperty? {
        return node_class_property
    }
}
