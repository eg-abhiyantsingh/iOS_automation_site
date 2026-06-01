import Foundation

// A wrapper that can decode any value without failing
struct AnyCodable: Codable {
    let value: Any

    init(_ value: Any) {
        self.value = value
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        if let intVal = try? container.decode(Int.self) {
            value = intVal
        } else if let doubleVal = try? container.decode(Double.self) {
            value = doubleVal
        } else if let boolVal = try? container.decode(Bool.self) {
            value = boolVal
        } else if let stringVal = try? container.decode(String.self) {
            value = stringVal
        } else if let arrayVal = try? container.decode([AnyCodable].self) {
            value = arrayVal.map { $0.value }
        } else if let dictVal = try? container.decode([String: AnyCodable].self) {
            value = dictVal.mapValues { $0.value }
        } else {
            value = NSNull()
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        switch value {
        // IMPORTANT: Bool must be checked BEFORE Int / Double / NSNumber.
        // Swift's `Bool` round-tripped through `Any` becomes
        // `__NSCFBoolean`, which `as? Int` happily satisfies (returning
        // 1 / 0). That meant `true` was being encoded as JSON `1`,
        // which broke `=== true` strict-equality checks in form
        // `visible` expressions on the web side.
        case let v as Bool:
            try container.encode(v)
        case let v as Int:
            try container.encode(v)
        case let v as Double:
            try container.encode(v)
        case let v as String:
            try container.encode(v)
        case let v as [Any]:
            // Encode array by wrapping each element in AnyCodable
            try container.encode(v.map { AnyCodable($0) })
        case let v as [String: Any]:
            // Encode dictionary by wrapping each value in AnyCodable
            try container.encode(v.mapValues { AnyCodable($0) })
        case is NSNull:
            try container.encodeNil()
        default:
            // Try to encode as-is, fallback to nil
            try container.encodeNil()
        }
    }
}

// Alternative: A protocol for resilient decoding
protocol ResilientDecodable: Decodable {
    init()
}

extension ResilientDecodable {
    init(from decoder: Decoder) throws {
        self.init()

        // Use Mirror to set properties dynamically
        let mirror = Mirror(reflecting: self)
        _ = try decoder.container(keyedBy: DynamicCodingKey.self)

        for child in mirror.children {
            guard let label = child.label else { continue }
            _ = DynamicCodingKey(stringValue: label)!

            // Try to decode each property, but don't fail if it doesn't work
            // This is pseudocode - actual implementation would need type checking
        }
    }
}

struct DynamicCodingKey: CodingKey {
    var stringValue: String
    var intValue: Int?

    init?(stringValue: String) {
        self.stringValue = stringValue
        self.intValue = nil
    }

    init?(intValue: Int) {
        self.intValue = intValue
        self.stringValue = String(intValue)
    }
}
