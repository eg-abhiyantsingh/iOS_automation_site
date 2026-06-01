//
//  NodeV2.swift
//  SwiftDataTutorial
//
import SwiftUI
import SwiftData

/// Stores the individual values used to calculate COM rating
struct COMCalculation: Codable, Equatable {
    var criticalityValue: Int
    var maintenanceValue: Int?
    var operatingConditionsValue: Int
    var maintenanceAnswers: [String]?

    init(criticalityValue: Int = 1, maintenanceValue: Int? = 1, operatingConditionsValue: Int = 1, maintenanceAnswers: [String]? = nil) {
        self.criticalityValue = criticalityValue
        self.maintenanceValue = maintenanceValue
        self.operatingConditionsValue = operatingConditionsValue
        self.maintenanceAnswers = maintenanceAnswers
    }

    /// Whether the maintenance assessment indicates nonserviceable
    var isNonserviceable: Bool {
        maintenanceValue == nil && maintenanceAnswers?.contains(where: { $0.hasPrefix("ns_") }) == true
    }

    /// The calculated COM rating is the maximum of all three values, or nil if nonserviceable
    var calculatedCOM: Int? {
        guard let mv = maintenanceValue else { return nil }
        return max(criticalityValue, mv, operatingConditionsValue)
    }
}

@Model
final class NodeV2 {
    var id: UUID
    var label: String
    var type: String
    var sld: SLDV2?
    var parent_id: UUID?
    var x: Double
    var y: Double
    var width: Double
    var height: Double
    @Relationship(inverse: \Photo.node) var photos: [Photo] = []
    var lastSyncedAt: Date?
    var lastModifiedAt: Date = Date()
    var needsSync: Bool = false
    var is_deleted: Bool = false
    var location: String?  // Legacy field, replaced by room relationship
    var room: Room? = nil
    var node_class: NodeClass? = nil
    var node_subtype: NodeSubtype? = nil
    var core_attributes: [NodeProperty] = []
    @Relationship(inverse: \NodeTerminal.node) var node_terminals: [NodeTerminal] = []
    @Relationship(inverse: \UserTask.linkedNodes) var node_tasks: [UserTask] = []
    var issues: [Issue] = []
    var ir_photos: [IRPhoto] = []
    @Relationship(inverse: \IRSession.nodes) var ir_sessions: [IRSession] = []
    var com: Int?
    var com_calculation: COMCalculation?
    var qr_code: String?
    var serviceability: String?
    var serviceability_note: String?
    var voltage: Double?
    var voltage_id: Int?
    var secondary_voltage: Double?
    var secondary_voltage_id: Int?
    // New engineering fields (ZP-2161). Default to nil so the existing
    // init signature stays unchanged; populate via the dedicated
    // assignment block below in SLDSyncService / BackgroundImporter.
    var tertiary_voltage: Double? = nil
    var tertiary_voltage_id: Int? = nil
    var system_voltage_id: Int? = nil
    var circuit_voltage_id: Int? = nil
    var voltage_user_overridden: Bool? = nil
    var notes: String?
    var default_photo_id: UUID?
    var suggested_shortcut_id: UUID?
    var applied_shortcut_id: UUID? = nil
    var eqp_lib: String?  // JSON string of EqpLibSelection
    var eqp_lib_suggested: String? = nil  // JSON string of suggested EqpLibSelection
    var eqp_note: String? = nil
    var eqp_engineering_approved: Bool? = nil
    var skm_lib_name: String? = nil
    var skm_lib_name_suggested: String? = nil
    var ocr_signature: String? = nil  // JSON string of OCR-extracted nameplate fields

    // ── Transformer ────────────────────────────────────────────────
    var kva_rating: Double? = nil
    var percent_impedance: Double? = nil

    // ── Box-level (mains breakers / panels) ────────────────────────
    var mains_type_id: Int? = nil
    var phase_configuration_id: Int? = nil

    // ── OCP (circuit breakers / fuses / relays) ────────────────────
    var ampere_rating: Int? = nil
    var pole_count: Int? = nil
    var manufacturer_id: Int? = nil
    var has_trip_unit: Bool? = nil
    var trip_type_id: Int? = nil
    var frame_amps: Int? = nil
    var sensor_amps: Int? = nil
    var plug_amps: Int? = nil

    // ── Cable / Busway ─────────────────────────────────────────────
    var length: Double? = nil
    var conductor_material: String? = nil
    var cable_size_id: Int? = nil
    var conductor_configuration_id: Int? = nil
    var duct_material_id: Int? = nil
    var conductor_description_id: Int? = nil
    var insulation_class_id: Int? = nil
    var insulation_type_id: Int? = nil
    var installation_id: Int? = nil
    var busway_ampere_rating: Int? = nil

    // ── Misc ───────────────────────────────────────────────────────
    var replacement_cost: Double? = nil
    var panel_schedule_status: String? = nil
    var rotation: Double? = nil
    var locked: Bool? = nil

    /// Typed accessor for the eqp_lib JSON field
    var eqpLibSelection: EqpLibSelection? {
        get { EqpLibSelection.from(jsonString: eqp_lib) }
        set { eqp_lib = newValue?.toJSONString() }
    }

    init(id: UUID, label: String, type: String, sld: SLDV2? = nil, parent_id: UUID? = nil, x: Double, y: Double, width: Double, height: Double, photos: [Photo], is_deleted: Bool, location: String? = nil, room: Room? = nil, node_class: NodeClass? = nil, node_subtype: NodeSubtype? = nil, core_attributes: [NodeProperty] = [], node_terminals: [NodeTerminal] = [], node_tasks: [UserTask] = [], issues: [Issue] = [], ir_photos: [IRPhoto] = [], ir_sessions: [IRSession] = [], com: Int? = nil, com_calculation: COMCalculation? = nil, qr_code: String? = nil, serviceability: String? = nil, serviceability_note: String? = nil, voltage: Double? = nil, voltage_id: Int? = nil, secondary_voltage: Double? = nil, secondary_voltage_id: Int? = nil, notes: String? = nil, default_photo_id: UUID? = nil, suggested_shortcut_id: UUID? = nil, eqp_lib: String? = nil) {
        self.id = id
        self.label = label
        self.type = type
        self.sld = sld
        self.parent_id = parent_id
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.photos = photos
        self.is_deleted = is_deleted
        self.location = location
        self.room = room
        self.node_class = node_class
        self.node_subtype = node_subtype
        self.core_attributes = core_attributes
        self.node_terminals = node_terminals
        self.node_tasks = node_tasks
        self.issues = issues
        self.ir_photos = ir_photos
        self.ir_sessions = ir_sessions
        self.com = com
        self.com_calculation = com_calculation
        self.qr_code = qr_code
        self.serviceability = serviceability
        self.serviceability_note = serviceability_note
        self.voltage = voltage
        self.voltage_id = voltage_id
        self.secondary_voltage = secondary_voltage
        self.secondary_voltage_id = secondary_voltage_id
        self.notes = notes
        self.default_photo_id = default_photo_id
        self.suggested_shortcut_id = suggested_shortcut_id
        self.eqp_lib = eqp_lib
    }

    /// Type-safe accessor for serviceability enum
    var serviceabilityEnum: Serviceability? {
        get { serviceability.flatMap { Serviceability(rawValue: $0) } }
        set { serviceability = newValue?.rawValue }
    }
    
    var af_completion: Int {
        guard let nodeClass = self.node_class else { return 0 }

        // Get all required properties
        let requiredProperties = nodeClass.definition.filter { $0.af_required }

        // If no required properties, consider it 100% complete
        guard !requiredProperties.isEmpty else { return 100 }

        // Create a lookup of current attribute values by ID (handling duplicates gracefully)
        // If duplicates exist, keep the last value
        var attributeValues: [UUID: String] = [:]
        for attr in core_attributes {
            attributeValues[attr.id] = attr.value
        }

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
    
    /// Returns true if all required fields are completed
    var af_isComplete: Bool {
        af_completion == 100
    }
    
    /// Returns a list of required fields that are missing values
    var af_missingRequiredFields: [NodeClassProperty] {
        guard let nodeClass = self.node_class else { return [] }

        // Create a lookup of current attribute values by ID (handling duplicates gracefully)
        // If duplicates exist, keep the last value
        var attributeValues: [UUID: String] = [:]
        for attr in core_attributes {
            attributeValues[attr.id] = attr.value
        }

        return nodeClass.definition.filter { property in
            guard property.af_required else { return false }

            if let value = attributeValues[property.id] {
                return value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            }
            return true // Missing attribute
        }
    }
    
    /// Returns a human-readable summary of AF completion
    var af_completionSummary: String {
        guard let nodeClass = self.node_class else { return "No requirements" }
        
        let requiredCount = nodeClass.definition.filter { $0.af_required }.count
        guard requiredCount > 0 else { return "No required fields" }
        
        let completedCount = requiredCount - af_missingRequiredFields.count
        return "\(completedCount) of \(requiredCount) required fields completed"
    }
    
    var openTasksCount: Int {
        self.node_tasks.filter { !$0.is_deleted && !$0.completed }.count
    }
}

// MARK: - Default Photo Logic
extension NodeV2 {
    /// Returns the default photo for this node with smart fallback logic
    var defaultPhoto: Photo? {
        // Priority 1: Use explicitly set default photo
        if let defaultId = default_photo_id,
           let photo = photos.first(where: { $0.id == defaultId && !$0.is_deleted }) {
            return photo
        }

        // Priority 2: Auto-select first profile photo
        if let profilePhoto = photos.first(where: { $0.type == "node_profile" && !$0.is_deleted }) {
            return profilePhoto
        }

        // Priority 3: Any non-deleted photo
        return photos.first(where: { !$0.is_deleted })
    }
}