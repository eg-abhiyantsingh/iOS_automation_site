//
//  EdgeV2.swift
//  SwiftDataTutorial
//
import SwiftUI
import SwiftData

struct EdgePoint: Codable {
    var x: Double
    var y: Double
    var id: String
    var active: Bool

    // Custom decoder to handle missing id/active fields from legacy data
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        x = try container.decode(Double.self, forKey: .x)
        y = try container.decode(Double.self, forKey: .y)
        // Use defaults if not present
        id = try container.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString
        active = try container.decodeIfPresent(Bool.self, forKey: .active) ?? true
    }

    // Standard initializer for creating new points
    init(x: Double, y: Double, id: String, active: Bool) {
        self.x = x
        self.y = y
        self.id = id
        self.active = active
    }
}

@Model
final class EdgeV2 {
    var id: UUID
    var source: UUID? = nil
    var target: UUID? = nil
    var sourceHandle: String? = nil
    var targetHandle: String? = nil
    var sourceNodeTerminalId: UUID? = nil
    var targetNodeTerminalId: UUID? = nil
    var sld: SLDV2?
    var is_deleted: Bool = false
    var lastSyncedAt: Date?
    var lastModifiedAt: Date = Date()
    var needsSync: Bool = false
    var edge_class: EdgeClass? = nil
    var core_attributes: [EdgeProperty] = []
    var points: [EdgePoint]? = nil
    var algorithm: String? = nil
  
    init(
        id: UUID,
        source: UUID? = nil,
        target: UUID? = nil,
        sld: SLDV2?,
        is_deleted: Bool,
        sourceHandle: String? = nil,
        targetHandle: String? = nil,
        sourceNodeTerminalId: UUID? = nil,
        targetNodeTerminalId: UUID? = nil,
        edge_class: EdgeClass? = nil,
        core_attributes: [EdgeProperty] = [],
        points: [EdgePoint]? = nil,
        algorithm: String? = nil
    ) {
        self.id = id
        self.source = source
        self.target = target
        self.sourceHandle = sourceHandle
        self.targetHandle = targetHandle
        self.sourceNodeTerminalId = sourceNodeTerminalId
        self.targetNodeTerminalId = targetNodeTerminalId
        self.sld = sld
        self.is_deleted = is_deleted
        self.edge_class = edge_class
        self.core_attributes = core_attributes
        self.points = points
        self.algorithm = algorithm
    }
    
    var af_completion: Int {
        guard let edgeClass = self.edge_class else { return 0 }
        
        // Get all required properties
        let requiredProperties = edgeClass.definition.filter { $0.af_required }
        
        // If no required properties, consider it 100% complete
        guard !requiredProperties.isEmpty else { return 100 }
        
        // Create a lookup of current attribute values by ID
        let attributeValues = Dictionary(
            uniqueKeysWithValues: core_attributes.map { ($0.id, $0.value) }
        )
        
        // Count how many required properties have non-empty values
        let completedCount = requiredProperties.filter { property in
            if let value = attributeValues[property.id] {
                return !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            }
            return false
        }.count
        
        // Calculate percentage
        let percentage = (Double(completedCount) / Double(requiredProperties.count)) * 100
        return Int(percentage.rounded())
    }
    
    var af_isComplete: Bool {
        af_completion == 100
    }
}