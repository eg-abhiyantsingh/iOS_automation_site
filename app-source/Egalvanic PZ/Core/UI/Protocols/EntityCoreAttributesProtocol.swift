//
//  EntityCoreAttributesProtocol.swift
//  SwiftDataTutorial
//
//  Protocol for standardizing core attributes across Node, Edge, and Issue entities
//

import Foundation
import SwiftData
import SwiftUI

// MARK: - Entity Protocol
protocol EntityWithCoreAttributes {
    associatedtype ClassType: EntityClass
    associatedtype PropertyType: EntityProperty
    
    var entityClass: ClassType? { get }
    var properties: [PropertyType] { get }
}

// MARK: - Class Protocol
protocol EntityClass {
    associatedtype PropertyDefinitionType: EntityPropertyDefinition
    
    var id: UUID { get }
    var name: String { get }
    var definition: [PropertyDefinitionType] { get }
}

// MARK: - Property Definition Protocol
protocol EntityPropertyDefinition {
    var id: UUID { get }
    var name: String { get }
    var type: String { get }
    var af_required: Bool { get }
    var options: [String?] { get }
    
    // Optional properties for specific entity types
    var fieldDescription: String? { get }
    var columns: [String?]? { get }
    var internal_type: [String?]? { get }
    var index: Int? { get }

    // Calculated field properties
    var calculationExpression: String? { get }
    var calculationPrecision: Int? { get }

    // Per-attribute description support
    var allowDescription: Bool { get }

    // Auto-fill rules for select/multi_select fields (supports descriptions)
    var autoFillRules: [String: [String: AutoFillTarget]]? { get }

    // Class-defined default value applied to empty fields (ZP-2251).
    // Empty string is treated the same as nil.
    var defaultValue: String? { get }
}

// MARK: - Default Implementations
extension EntityPropertyDefinition {
    var allowDescription: Bool { false }
    var autoFillRules: [String: [String: AutoFillTarget]]? { nil }
    var defaultValue: String? { nil }
}

// MARK: - Property Protocol
protocol EntityProperty {
    associatedtype PropertyDefinitionType: EntityPropertyDefinition
    
    var id: UUID { get }
    var name: String { get }
    var value: String { get }
    var propertyDefinition: PropertyDefinitionType? { get }
}

// MARK: - Core Attributes Service
struct CoreAttributesService {
    
    /// Apply core attribute changes to an entity, handling class changes and attribute updates
    @MainActor
    static func applyCoreAttributeChanges(
        to node: NodeV2,
        selectedClass: NodeClass?,
        originalClass: NodeClass?,
        draftAttributes: [UUID: String],
        modelContext: ModelContext
    ) {
        // Clear existing attributes if node class changed or was removed
        if selectedClass?.id != originalClass?.id {
            // Delete all existing core attributes
            for attr in node.core_attributes {
                modelContext.delete(attr)
            }
            node.core_attributes.removeAll()
        }
        
        // Apply new attributes if there's a selected node class
        guard let nodeClass = selectedClass else { return }

        // Build existing attributes dictionary (handle duplicates gracefully)
        var existingAttrs: [UUID: NodeProperty] = [:]
        var duplicatesFound = false
        for attr in node.core_attributes {
            if existingAttrs[attr.id] != nil {
                duplicatesFound = true
            }
            existingAttrs[attr.id] = attr
        }

        // If duplicates found, clean them up first
        if duplicatesFound {
            // Keep only unique attributes (last one wins)
            let uniqueAttrs = Array(existingAttrs.values)
            for attr in node.core_attributes {
                if !uniqueAttrs.contains(where: { $0.id == attr.id && $0 === attr }) {
                    modelContext.delete(attr)
                }
            }
            node.core_attributes = uniqueAttrs
        }

        for classProperty in nodeClass.definition {
            let propertyId = classProperty.id
            
            if let newValue = draftAttributes[propertyId] {
                if let existingAttr = existingAttrs[propertyId] {
                    if existingAttr.value != newValue {
                        existingAttr.value = newValue
                        existingAttr.node_class_property = classProperty
                    }
                } else {
                    let newAttr = NodeProperty(
                        id: propertyId,
                        node_class_property: classProperty,
                        name: classProperty.name,
                        value: newValue
                    )
                    
                    modelContext.insert(newAttr)
                    node.core_attributes.append(newAttr)
                }
            } else if let existingAttr = existingAttrs[propertyId] {
                node.core_attributes.removeAll { $0.id == propertyId }
                modelContext.delete(existingAttr)
            }
        }
    }
    
    /// Apply core attribute changes to an edge
    @MainActor
    static func applyCoreAttributeChanges(
        to edge: EdgeV2,
        selectedClass: EdgeClass?,
        originalClass: EdgeClass?,
        draftAttributes: [UUID: String],
        modelContext: ModelContext
    ) {
        // Clear existing attributes if edge class changed or was removed
        if selectedClass?.id != originalClass?.id {
            // Delete all existing core attributes
            for attr in edge.core_attributes {
                modelContext.delete(attr)
            }
            edge.core_attributes.removeAll()
        }
        
        // Apply new attributes if there's a selected edge class
        guard let edgeClass = selectedClass else { return }

        // Build existing attributes dictionary (handle duplicates gracefully)
        var existingAttrs: [UUID: EdgeProperty] = [:]
        var duplicatesFound = false
        for attr in edge.core_attributes {
            if existingAttrs[attr.id] != nil {
                duplicatesFound = true
            }
            existingAttrs[attr.id] = attr
        }

        // If duplicates found, clean them up first
        if duplicatesFound {
            // Keep only unique attributes (last one wins)
            let uniqueAttrs = Array(existingAttrs.values)
            for attr in edge.core_attributes {
                if !uniqueAttrs.contains(where: { $0.id == attr.id && $0 === attr }) {
                    modelContext.delete(attr)
                }
            }
            edge.core_attributes = uniqueAttrs
        }

        for classProperty in edgeClass.definition {
            let propertyId = classProperty.id
            
            if let newValue = draftAttributes[propertyId] {
                if let existingAttr = existingAttrs[propertyId] {
                    if existingAttr.value != newValue {
                        existingAttr.value = newValue
                        existingAttr.edge_class_property = classProperty
                    }
                } else {
                    let newAttr = EdgeProperty(
                        id: propertyId,
                        edge_class_property: classProperty,
                        name: classProperty.name,
                        value: newValue
                    )
                    
                    modelContext.insert(newAttr)
                    edge.core_attributes.append(newAttr)
                }
            } else if let existingAttr = existingAttrs[propertyId] {
                edge.core_attributes.removeAll { $0.id == propertyId }
                modelContext.delete(existingAttr)
            }
        }
    }
    
    /// Apply core attribute changes to an issue
    @MainActor
    static func applyCoreAttributeChanges(
        to issue: Issue,
        selectedClass: IssueClass?,
        originalClass: IssueClass?,
        draftAttributes: [UUID: String],
        draftUnits: [UUID: String] = [:],
        draftDescriptions: [UUID: String] = [:],
        modelContext: ModelContext
    ) {
        // Clear existing attributes if issue class changed or was removed
        if selectedClass?.id != originalClass?.id {
            // Delete all existing details
            for detail in issue.details {
                modelContext.delete(detail)
            }
            issue.details.removeAll()
        }
        
        // Apply new attributes if there's a selected issue class
        guard let issueClass = selectedClass else { return }

        // Build existing details dictionary (handle duplicates gracefully)
        var existingDetails: [UUID: IssueProperty] = [:]
        var duplicatesFound = false
        for detail in issue.details {
            if existingDetails[detail.id] != nil {
                duplicatesFound = true
            }
            existingDetails[detail.id] = detail
        }

        // If duplicates found, clean them up first
        if duplicatesFound {
            // Keep only unique details (last one wins)
            let uniqueDetails = Array(existingDetails.values)
            for detail in issue.details {
                if !uniqueDetails.contains(where: { $0.id == detail.id && $0 === detail }) {
                    modelContext.delete(detail)
                }
            }
            issue.details = uniqueDetails
        }

        for classProperty in issueClass.definition {
            let propertyId = classProperty.id
            let unitValue = draftUnits[propertyId]

            if let newValue = draftAttributes[propertyId] {
                let descValue = draftDescriptions[propertyId]
                if let existingDetail = existingDetails[propertyId] {
                    if existingDetail.value != newValue || existingDetail.unit != unitValue || existingDetail.attributeNotes != descValue {
                        existingDetail.value = newValue
                        existingDetail.unit = unitValue
                        existingDetail.attributeNotes = descValue
                        existingDetail.issue_class_property = classProperty
                    }
                } else {
                    let newDetail = IssueProperty(
                        id: propertyId,
                        issue_class_property: classProperty,
                        name: classProperty.name,
                        value: newValue,
                        unit: unitValue,
                        attributeNotes: descValue
                    )

                    modelContext.insert(newDetail)
                    issue.details.append(newDetail)
                }
            } else if let existingDetail = existingDetails[propertyId] {
                issue.details.removeAll { $0.id == propertyId }
                modelContext.delete(existingDetail)
            }
        }
    }
    
    /// Apply class-defined `default_value` (ZP-2251) to any draft attributes
    /// that are currently missing or empty. Stored/user-entered values always win.
    /// Skips types where defaults aren't supported (`calculated`, `table_with_column_headers`).
    static func applyDefaultValues<T: EntityClass>(
        from entityClass: T,
        into draftAttributes: inout [UUID: String]
    ) {
        for property in entityClass.definition {
            // Skip unsupported types
            switch property.type {
            case "calculated", "table_with_column_headers":
                continue
            default:
                break
            }

            // Empty string is "no default" per the spec
            guard let defaultValue = property.defaultValue, !defaultValue.isEmpty else { continue }

            let current = draftAttributes[property.id] ?? ""
            if current.isEmpty {
                draftAttributes[property.id] = defaultValue
            }
        }
    }

    /// Preserve attribute values when switching between entity classes
    static func preserveAttributeValues<T: EntityClass>(
        from oldClass: T?,
        to newClass: T,
        currentAttributes: [UUID: String]
    ) -> [UUID: String] {
        guard let previousClass = oldClass else {
            // No previous class, return empty attributes
            return [:]
        }
        
        var preservedAttributes: [UUID: String] = [:]
        
        // Try to preserve values for properties with the same name
        for newProperty in newClass.definition {
            // First try to find by same ID
            if let existingValue = currentAttributes[newProperty.id] {
                preservedAttributes[newProperty.id] = existingValue
            } else {
                // Otherwise, try to find by same name in the previous class
                if let matchingOldProperty = previousClass.definition.first(where: { 
                    $0.name == newProperty.name 
                }),
                   let existingValue = currentAttributes[matchingOldProperty.id] {
                    preservedAttributes[newProperty.id] = existingValue
                }
            }
        }
        
        return preservedAttributes
    }
}


