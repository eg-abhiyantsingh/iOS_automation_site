//
//  EdgeV2+CoreAttributes.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/28/25.
//

import Foundation

// MARK: - Edge Conformances
extension EdgeV2: EntityWithCoreAttributes {
    typealias ClassType = EdgeClass
    typealias PropertyType = EdgeProperty
    
    var entityClass: EdgeClass? {
        return edge_class
    }
    
    var properties: [EdgeProperty] {
        return core_attributes
    }
}

extension EdgeClass: EntityClass {
    typealias PropertyDefinitionType = EdgeClassProperty
}

extension EdgeClassProperty: EntityPropertyDefinition {
    var fieldDescription: String? { nil }
    var columns: [String?]? { nil }
    var internal_type: [String?]? { nil }
    // index is now a stored property in EdgeClassProperty, not computed
    var calculationExpression: String? { nil }
    var calculationPrecision: Int? { nil }
    var defaultValue: String? { default_value }
}

extension EdgeProperty: EntityProperty {
    typealias PropertyDefinitionType = EdgeClassProperty
    
    var propertyDefinition: EdgeClassProperty? {
        return edge_class_property
    }
}
