//
//  AutoFillTarget.swift
//  Egalvanic PZ
//
//  Value types for auto-fill rules with optional description support.
//  Supports both old format (plain arrays) and new format ({ value, description }).
//

import Foundation

/// Normalized auto-fill target from a rule definition.
/// Old format: `["value1", "value2"]` → `AutoFillTarget(values: ["value1", "value2"], description: nil)`
/// New format: `{ "value": ["value1"], "description": "text" }` → `AutoFillTarget(values: ["value1"], description: "text")`
struct AutoFillTarget {
    let values: [String]
    let description: String?

    init(values: [String], description: String? = nil) {
        self.values = values
        self.description = description
    }
}

/// Result from AutoFillEngine for a single target field.
struct AutoFillResult: Equatable {
    let value: String
    let description: String?
}
