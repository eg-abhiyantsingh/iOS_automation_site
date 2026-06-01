//
//  Issue+CoreAttributes.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/28/25.
//

import Foundation

// MARK: - Issue Conformances
extension Issue: EntityWithCoreAttributes {
    typealias ClassType = IssueClass
    typealias PropertyType = IssueProperty
    
    var entityClass: IssueClass? {
        return issue_class
    }
    
    var properties: [IssueProperty] {
        return details
    }
}

extension IssueClass: EntityClass {
    typealias PropertyDefinitionType = IssueClassProperty
}

extension IssueClassProperty: EntityPropertyDefinition {
    var fieldDescription: String? { issueDescription }
    // allowDescription uses the stored property directly (name matches protocol requirement)
    var defaultValue: String? { default_value }
}

extension IssueProperty: EntityProperty {
    typealias PropertyDefinitionType = IssueClassProperty
    
    var propertyDefinition: IssueClassProperty? {
        return issue_class_property
    }
}
