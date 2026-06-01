//
//  EdgeClassDTO.swift
//  SwiftDataTutorial
//
import Foundation

struct EdgeClassDTO: Codable {
    var id: UUID
    var name: String
    var definition: [EdgeClassPropertyDTO]
    var is_deleted: Bool?

    // Memberwise initializer for manual construction
    init(id: UUID, name: String, definition: [EdgeClassPropertyDTO], is_deleted: Bool? = nil) {
        self.id = id
        self.name = name
        self.definition = definition
        self.is_deleted = is_deleted
    }

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
                definition = try container.decode([EdgeClassPropertyDTO].self, forKey: .definition)
            } catch {
                AppLogger.log(.notice, "EdgeClassDTO: Failed to decode definition array, using empty array. Error: \(error)", category: .node)
                definition = []
            }
        } else {
            AppLogger.log(.notice, "EdgeClassDTO: Missing definition field, using empty array", category: .node)
            definition = []
        }
    }
}

struct EdgeClassPropertyDTO: Codable {
    var id: UUID
    var name: String
    var type: String
    var af_required: Bool
    var options: [String]?
    let index: Int?
    var default_value: String?

    // Custom decoder for defensive parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        // Required fields
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        type = try container.decode(String.self, forKey: .type)

        // af_required with fallback to false if missing or wrong type
        af_required = (try? container.decode(Bool.self, forKey: .af_required)) ?? false

        // Optional index for sorting
        index = try? container.decode(Int.self, forKey: .index)

        // Class-defined default for empty fields (ZP-2251)
        default_value = try? container.decode(String.self, forKey: .default_value)

        // Options - handle various malformed cases
        if container.contains(.options) {
            do {
                options = try container.decode([String]?.self, forKey: .options)
            } catch {
                AppLogger.log(.notice, "EdgeClassPropertyDTO: Failed to decode options array, using nil. Error: \(error)", category: .node)
                options = nil
            }
        } else {
            options = nil
        }
    }
}

struct EdgePropertyDTO: Codable {
    var id: UUID
    var edge_class_property: String
    var name: String
    var value: String?  // Made optional to handle null values from backend
}