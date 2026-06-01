//
//  IssueClassDTO.swift
//  SwiftDataTutorial
//
import Foundation

struct IssueClassDTO: Codable {
    var id: UUID
    var name: String
    var definition: [IssueClassPropertyDTO]
    var is_deleted: Bool?

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        // Required fields - throw if missing
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)

        // Optional fields
        is_deleted = try? container.decode(Bool.self, forKey: .is_deleted)

        // Defensive decoding for definition array
        if container.contains(.definition) {
            do {
                definition = try container.decode([IssueClassPropertyDTO].self, forKey: .definition)
            } catch {
                AppLogger.log(.notice, "IssueClassDTO: Failed to decode definition array, using empty array. Error: \(error)", category: .issue)
                definition = []
            }
        } else {
            AppLogger.log(.notice, "IssueClassDTO: Missing definition field, using empty array", category: .issue)
            definition = []
        }
    }
}

struct IssueClassPropertyDTO: Codable {
    var id: UUID
    var name: String
    var type: String
    let index: Int?
    var af_required: Bool
    var options: [String]?
    var description: String?
    var columns: [String]?  // Optional for table types
    var internal_type: [String]?  // Changed from column_types to match JSON
    var calculationExpression: String?  // For calculated fields
    var calculationPrecision: Int?      // For calculated fields
    var allowDescription: Bool = false  // Whether per-attribute description is allowed
    var autoFillRules: [String: [String: Any]]?  // Auto-fill rules for select/multi_select fields (polymorphic: array or {value, description})
    var default_value: String?  // Class-defined default applied to empty fields (ZP-2251)

    private enum CodingKeys: String, CodingKey {
        case id, name, type, index, af_required, options, description
        case columns, internal_type, calculation, allow_description, auto_fill_rules, default_value
    }

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        // Required fields
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        type = try container.decode(String.self, forKey: .type)

        // Optional fields with defensive decoding
        index = try? container.decode(Int.self, forKey: .index)
        af_required = (try? container.decode(Bool.self, forKey: .af_required)) ?? false
        description = try? container.decode(String.self, forKey: .description)
        default_value = try? container.decode(String.self, forKey: .default_value)

        // Options - handle various malformed cases
        if container.contains(.options) {
            do {
                options = try container.decode([String]?.self, forKey: .options)
            } catch {
                AppLogger.log(.notice, "IssueClassPropertyDTO: Failed to decode options array, using nil. Error: \(error)", category: .issue)
                options = nil
            }
        } else {
            options = nil
        }

        // Columns - handle various malformed cases
        if container.contains(.columns) {
            do {
                columns = try container.decode([String]?.self, forKey: .columns)
            } catch {
                AppLogger.log(.notice, "IssueClassPropertyDTO: Failed to decode columns array, using nil. Error: \(error)", category: .issue)
                columns = nil
            }
        } else {
            columns = nil
        }

        // Internal type - handle various malformed cases (might be string instead of array)
        if container.contains(.internal_type) {
            do {
                internal_type = try container.decode([String]?.self, forKey: .internal_type)
            } catch {
                // Try to decode as single string and wrap in array
                if let singleType = try? container.decode(String.self, forKey: .internal_type) {
                    AppLogger.log(.notice, "IssueClassPropertyDTO: internal_type was string, converting to array", category: .issue)
                    internal_type = [singleType]
                } else {
                    AppLogger.log(.notice, "IssueClassPropertyDTO: Failed to decode internal_type, using nil. Error: \(error)", category: .issue)
                    internal_type = nil
                }
            }
        } else {
            internal_type = nil
        }

        // Allow description flag
        allowDescription = (try? container.decode(Bool.self, forKey: .allow_description)) ?? false

        // Auto-fill rules - decode polymorphic structure (old: [String] or new: {value, description})
        if container.contains(.auto_fill_rules) {
            if let anyCodable = try? container.decode([String: [String: AnyCodable]].self, forKey: .auto_fill_rules) {
                autoFillRules = anyCodable.mapValues { targets in
                    targets.mapValues { $0.value }
                }
            } else {
                autoFillRules = nil
            }
        } else {
            autoFillRules = nil
        }

        // Calculation - decode from nested JSON object: { "expression": "...", "precision": 2 }
        if container.contains(.calculation) {
            do {
                let calcContainer = try container.nestedContainer(keyedBy: CalculationCodingKeys.self, forKey: .calculation)
                calculationExpression = try? calcContainer.decode(String.self, forKey: .expression)
                calculationPrecision = try? calcContainer.decode(Int.self, forKey: .precision)
            } catch {
                AppLogger.log(.notice, "IssueClassPropertyDTO: Failed to decode calculation, using nil. Error: \(error)", category: .issue)
                calculationExpression = nil
                calculationPrecision = nil
            }
        } else {
            calculationExpression = nil
            calculationPrecision = nil
        }
    }

    private enum CalculationCodingKeys: String, CodingKey {
        case expression
        case precision
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(type, forKey: .type)
        try container.encodeIfPresent(index, forKey: .index)
        try container.encode(af_required, forKey: .af_required)
        try container.encodeIfPresent(options, forKey: .options)
        try container.encodeIfPresent(description, forKey: .description)
        try container.encodeIfPresent(columns, forKey: .columns)
        try container.encodeIfPresent(internal_type, forKey: .internal_type)
        try container.encodeIfPresent(default_value, forKey: .default_value)
        try container.encode(allowDescription, forKey: .allow_description)
        if let rules = autoFillRules {
            let encoded = rules.mapValues { targets in
                targets.mapValues { AnyCodable($0) }
            }
            try container.encode(encoded, forKey: .auto_fill_rules)
        }
        if calculationExpression != nil || calculationPrecision != nil {
            var calcContainer = container.nestedContainer(keyedBy: CalculationCodingKeys.self, forKey: .calculation)
            try calcContainer.encodeIfPresent(calculationExpression, forKey: .expression)
            try calcContainer.encodeIfPresent(calculationPrecision, forKey: .precision)
        }
    }
}

struct IssuePropertyDTO: Codable {
    var id: UUID
    var issue_class_property: String
    var name: String
    var value: String
    var unit: String?
    var description: String?

    private enum CodingKeys: String, CodingKey {
        case id, issue_class_property, name, value, unit, description
    }

    init(id: UUID, issue_class_property: String, name: String, value: String, unit: String? = nil, description: String? = nil) {
        self.id = id
        self.issue_class_property = issue_class_property
        self.name = name
        self.value = value
        self.unit = unit
        self.description = description
    }

    // Custom decoder: the API may return `value` as a number (e.g. 10) instead of a string ("10")
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        issue_class_property = try container.decode(String.self, forKey: .issue_class_property)
        name = try container.decode(String.self, forKey: .name)

        // Handle value as string, number, or null
        if let stringValue = try? container.decode(String.self, forKey: .value) {
            value = stringValue
        } else if let intValue = try? container.decode(Int.self, forKey: .value) {
            value = String(intValue)
        } else if let doubleValue = try? container.decode(Double.self, forKey: .value) {
            // Format without trailing zeros for whole numbers
            value = doubleValue.truncatingRemainder(dividingBy: 1) == 0
                ? String(format: "%.0f", doubleValue)
                : String(doubleValue)
        } else if let boolValue = try? container.decode(Bool.self, forKey: .value) {
            value = boolValue ? "true" : "false"
        } else {
            value = ""
        }

        unit = try? container.decodeIfPresent(String.self, forKey: .unit)
        description = try? container.decodeIfPresent(String.self, forKey: .description)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(issue_class_property, forKey: .issue_class_property)
        try container.encode(name, forKey: .name)
        try container.encode(value, forKey: .value)
        try container.encodeIfPresent(unit, forKey: .unit)
        try container.encodeIfPresent(description, forKey: .description)
    }
}