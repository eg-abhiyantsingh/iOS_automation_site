//
//  IssueClass.swift
//  SwiftDataTutorial
//
import Foundation
import SwiftData

@Model
final class IssueClass {
    @Attribute(.unique) var id: UUID
    var name: String
    var definition: [IssueClassProperty]
    var is_deleted: Bool = false
    
    init(id: UUID, name: String, definition: [IssueClassProperty], is_deleted: Bool = false) {
        self.id = id
        self.name = name
        self.definition = definition
        self.is_deleted = is_deleted
    }
}

@Model
final class IssueClassProperty {
    @Attribute(.unique) var id: UUID
    var name: String
    var type: String
    var options: [String?]
    var af_required: Bool = false
    var issueClass: IssueClass?
    var issueDescription: String?
    var columns: [String?]?  // Optional array for table column headers
    var internal_type: [String?]?  // Optional array for table column types
    var index: Int?
    var calculationExpression: String?  // Expression string for calculated fields
    var calculationPrecision: Int?      // Decimal precision for calculated results
    var allowDescription: Bool = false  // Whether this property allows per-attribute description
    var autoFillRulesJSON: String?  // JSON-encoded auto-fill rules for select/multi_select fields
    var default_value: String?  // Class-defined default applied to empty fields (ZP-2251)

    /// Decoded auto-fill rules: optionValue → targetAttrId → AutoFillTarget
    /// Handles both old format (plain arrays) and new format ({ value, description }).
    var autoFillRules: [String: [String: AutoFillTarget]]? {
        guard let json = autoFillRulesJSON,
              let data = json.data(using: .utf8),
              let raw = try? JSONSerialization.jsonObject(with: data) as? [String: [String: Any]]
        else { return nil }

        var result: [String: [String: AutoFillTarget]] = [:]
        for (optionValue, targetMappings) in raw {
            var targets: [String: AutoFillTarget] = [:]
            for (targetId, targetValue) in targetMappings {
                if let valueArray = targetValue as? [String] {
                    // Old format: plain array of strings
                    targets[targetId] = AutoFillTarget(values: valueArray)
                } else if let dict = targetValue as? [String: Any],
                          let valueArray = dict["value"] as? [String] {
                    // New format: { "value": [...], "description": "..." }
                    let desc = dict["description"] as? String
                    targets[targetId] = AutoFillTarget(values: valueArray, description: desc)
                }
            }
            if !targets.isEmpty {
                result[optionValue] = targets
            }
        }
        return result.isEmpty ? nil : result
    }

    init(
        id: UUID,
        name: String,
        type: String,
        options: [String?] = [],
        af_required: Bool,
        issueClass: IssueClass? = nil,
        issueDescription: String? = nil,
        columns: [String?]? = nil,
        internal_type: [String?]? = nil,
        index: Int? = nil,
        calculationExpression: String? = nil,
        calculationPrecision: Int? = nil,
        allowDescription: Bool = false,
        autoFillRulesJSON: String? = nil,
        default_value: String? = nil
    ) {
        self.id = id
        self.name = name
        self.type = type
        self.options = options
        self.af_required = af_required
        self.issueClass = issueClass
        self.issueDescription = issueDescription
        self.columns = columns
        self.internal_type = internal_type
        self.index = index
        self.calculationExpression = calculationExpression
        self.calculationPrecision = calculationPrecision
        self.allowDescription = allowDescription
        self.autoFillRulesJSON = autoFillRulesJSON
        self.default_value = default_value
    }
}

@Model
final class IssueProperty {
    var id: UUID
    var issue_class_property: IssueClassProperty?
    var name: String
    var value: String
    var unit: String?
    var attributeNotes: String?  // Per-attribute notes

    init(id: UUID, issue_class_property: IssueClassProperty, name: String, value: String, unit: String? = nil, attributeNotes: String? = nil) {
        self.id = id
        self.issue_class_property = issue_class_property
        self.name = name
        self.value = value
        self.unit = unit
        self.attributeNotes = attributeNotes
    }
}