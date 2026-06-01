//
//  AutoFillEngine.swift
//  Egalvanic PZ
//
//  Pure computation engine for auto-filling core attribute values
//  based on rules defined on select/multi_select fields.
//

import Foundation

struct AutoFillEngine {

    /// Compute auto-fill values from ALL select/multi_select fields that have rules.
    /// Returns [targetAttrId: AutoFillResult] for attributes that should be auto-filled,
    /// including optional descriptions when configured.
    static func computeAutoFills(
        properties: [IssueClassProperty],
        currentValues: [UUID: String]
    ) -> [UUID: AutoFillResult] {
        // Build a lookup for target property types
        let propertyTypeById = Dictionary(uniqueKeysWithValues: properties.map { ($0.id, $0.type) })

        // Accumulate fill values and descriptions per target
        var accumulatedValues: [UUID: [String]] = [:]
        var accumulatedDescriptions: [UUID: String] = [:]  // last-wins for descriptions

        for property in properties {
            guard let rules = property.autoFillRules else { continue }
            guard property.type == "select" || property.type == "dropdown" || property.type == "multi_select" else { continue }

            let currentValue = currentValues[property.id] ?? ""
            guard !currentValue.isEmpty else { continue }

            // Get the selected option(s)
            let selectedOptions: [String]
            if property.type == "multi_select" {
                selectedOptions = parseMultiSelectValue(currentValue)
            } else {
                selectedOptions = [currentValue]
            }

            // For each selected option, look up its rules
            for option in selectedOptions {
                guard let targetMappings = rules[option] else { continue }

                for (targetIdString, fillTarget) in targetMappings {
                    guard let targetId = UUID(uuidString: targetIdString) else { continue }
                    guard !fillTarget.values.isEmpty else { continue }

                    // Accumulate fill values (union for multi_select targets)
                    var existing = accumulatedValues[targetId] ?? []
                    for val in fillTarget.values where !existing.contains(val) {
                        existing.append(val)
                    }
                    accumulatedValues[targetId] = existing

                    // Description: last-wins (matching frontend mergeFillValue behavior)
                    if let desc = fillTarget.description {
                        accumulatedDescriptions[targetId] = desc
                    }
                }
            }
        }

        // Convert accumulated values to final results
        var result: [UUID: AutoFillResult] = [:]
        for (targetId, fillValues) in accumulatedValues {
            let targetType = propertyTypeById[targetId] ?? "text"
            let finalValue: String

            if targetType == "multi_select" {
                finalValue = serializeMultiSelectValue(fillValues)
            } else {
                // For select/text/other: last value wins
                guard let lastValue = fillValues.last else { continue }
                finalValue = lastValue
            }

            result[targetId] = AutoFillResult(
                value: finalValue,
                description: accumulatedDescriptions[targetId]
            )
        }

        return result
    }

    // MARK: - Multi-Select Helpers

    static func parseMultiSelectValue(_ value: String) -> [String] {
        guard !value.isEmpty,
              let data = value.data(using: .utf8),
              let arr = try? JSONSerialization.jsonObject(with: data) as? [String] else {
            return []
        }
        return arr
    }

    static func serializeMultiSelectValue(_ values: [String]) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: values),
              let str = String(data: data, encoding: .utf8) else {
            return "[]"
        }
        return str
    }
}
