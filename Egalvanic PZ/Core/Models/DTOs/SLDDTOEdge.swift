//
//  SLDDTOEdge.swift
//  SwiftDataTutorial
//
import Foundation

struct SLDDTOEdge: Codable {
    var id: UUID
    var source: UUID?
    var target: UUID?
    var sld_id: UUID
    var source_handle: String?
    var target_handle: String?
    var source_node_terminal_id: UUID?
    var target_node_terminal_id: UUID?
    var is_deleted: Bool = false
    var edge_class: UUID?
    let core_attributes: [EdgePropertyDTO]?
    var af_completion: Bool?
    var points: [EdgePoint]?
    var algorithm: String?

    enum CodingKeys: String, CodingKey {
        case id, source, target, sld_id
        case source_handle, target_handle
        case source_node_terminal_id, target_node_terminal_id
        case is_deleted, edge_class, core_attributes, af_completion, points, algorithm
    }

    // Standard initializer for programmatic creation
    init(id: UUID, source: UUID? = nil, target: UUID? = nil, sld_id: UUID,
         source_handle: String? = nil, target_handle: String? = nil,
         source_node_terminal_id: UUID? = nil, target_node_terminal_id: UUID? = nil,
         is_deleted: Bool = false, edge_class: UUID? = nil, core_attributes: [EdgePropertyDTO]? = nil,
         af_completion: Bool? = nil, points: [EdgePoint]? = nil, algorithm: String? = nil) {
        self.id = id
        self.source = source
        self.target = target
        self.sld_id = sld_id
        self.source_handle = source_handle
        self.target_handle = target_handle
        self.source_node_terminal_id = source_node_terminal_id
        self.target_node_terminal_id = target_node_terminal_id
        self.is_deleted = is_deleted
        self.edge_class = edge_class
        self.core_attributes = core_attributes
        self.af_completion = af_completion
        self.points = points
        self.algorithm = algorithm
    }

    // Custom encoder to ensure source/target are always sent (as null when nil, not omitted)
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(source, forKey: .source)
        try container.encode(target, forKey: .target)
        try container.encode(sld_id, forKey: .sld_id)
        try container.encodeIfPresent(source_handle, forKey: .source_handle)
        try container.encodeIfPresent(target_handle, forKey: .target_handle)
        try container.encodeIfPresent(source_node_terminal_id, forKey: .source_node_terminal_id)
        try container.encodeIfPresent(target_node_terminal_id, forKey: .target_node_terminal_id)
        try container.encode(is_deleted, forKey: .is_deleted)
        try container.encodeIfPresent(edge_class, forKey: .edge_class)
        try container.encodeIfPresent(core_attributes, forKey: .core_attributes)
        try container.encodeIfPresent(af_completion, forKey: .af_completion)
        try container.encodeIfPresent(points, forKey: .points)
        try container.encodeIfPresent(algorithm, forKey: .algorithm)
    }

    // Custom decoder for resilient parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = try container.decode(UUID.self, forKey: .id)
        source = try? container.decodeIfPresent(UUID.self, forKey: .source)
        target = try? container.decodeIfPresent(UUID.self, forKey: .target)
        sld_id = try container.decode(UUID.self, forKey: .sld_id)

        // Terminal connection fields
        source_handle = try? container.decodeIfPresent(String.self, forKey: .source_handle)
        target_handle = try? container.decodeIfPresent(String.self, forKey: .target_handle)
        source_node_terminal_id = try? container.decodeIfPresent(UUID.self, forKey: .source_node_terminal_id)
        target_node_terminal_id = try? container.decodeIfPresent(UUID.self, forKey: .target_node_terminal_id)

        is_deleted = try container.decodeIfPresent(Bool.self, forKey: .is_deleted) ?? false
        edge_class = try? container.decodeIfPresent(UUID.self, forKey: .edge_class)

        // Resilient parsing for core_attributes - handle dictionary format gracefully
        if let attributesArray = try? container.decodeIfPresent([EdgePropertyDTO].self, forKey: .core_attributes) {
            core_attributes = attributesArray
        } else {
            // If it's not an array (e.g., it's a dictionary), set to nil and log warning
            core_attributes = nil
            AppLogger.log(.notice, "Edge \(id): core_attributes is not an array, skipping (setting to nil)", category: .node)
        }

        af_completion = try? container.decodeIfPresent(Bool.self, forKey: .af_completion)
        points = try? container.decodeIfPresent([EdgePoint].self, forKey: .points)
        algorithm = try? container.decodeIfPresent(String.self, forKey: .algorithm)
    }
}