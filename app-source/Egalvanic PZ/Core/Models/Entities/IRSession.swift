//
//  IRSession.swift
//  SwiftDataTutorial
//
import Foundation
import SwiftData

@Model
final class IRSession {
    @Attribute(.unique) var id: UUID
    var name: String
    var sessionDescription: String?
    var photo_type: String
    var active_visual_prefix: String
    var active_ir_prefix: String
    var date_created: Date
    var date_closed: Date?
    var sld: SLDV2
    var active: Bool
    var is_deleted: Bool = false
    var owned_by: [UUID] = []
    var assigned_to: [UUID] = []
    var equipmentIds: [UUID] = []
    @Relationship var ir_photos: [IRPhoto] = []
    @Relationship var user_tasks: [UserTask] = []
    @Relationship var issues: [Issue] = []
    @Relationship var nodes: [NodeV2] = []
    @Relationship(deleteRule: .cascade, inverse: \SessionWorkBlock.session)
    var work_blocks: [SessionWorkBlock] = []

    init(id: UUID, name: String, sessionDescription: String? = nil, photo_type: String, active_visual_prefix: String, active_ir_prefix: String, date_created: Date, date_closed: Date?, sld: SLDV2, active: Bool, is_deleted: Bool = false, owned_by: [UUID] = [], assigned_to: [UUID] = [], equipmentIds: [UUID] = [], ir_photos: [IRPhoto] = [], user_tasks: [UserTask], issues: [Issue], nodes: [NodeV2] = [], work_blocks: [SessionWorkBlock] = []) {
        self.id = id
        self.name = name
        self.sessionDescription = sessionDescription
        self.photo_type = photo_type
        self.active_visual_prefix = active_visual_prefix
        self.active_ir_prefix = active_ir_prefix
        self.date_created = date_created
        self.date_closed = date_closed
        self.sld = sld
        self.active = active
        self.is_deleted = is_deleted
        self.owned_by = owned_by
        self.assigned_to = assigned_to
        self.equipmentIds = equipmentIds
        self.ir_photos = ir_photos
        self.user_tasks = user_tasks
        self.issues = issues
        self.nodes = nodes
        self.work_blocks = work_blocks
    }
}

@Model
final class IRPhoto {
    @Attribute var id: UUID
    @Relationship var ir_session: IRSession?
    var node: NodeV2
    var sld: SLDV2
    var visual_photo_key: String
    var ir_photo_key: String
    var date_created: Date
    var is_deleted: Bool = false
    var issue: Issue? = nil
    var isSynced: Bool = false
    
    init(id: UUID, ir_session: IRSession?, node: NodeV2, sld: SLDV2, visual_photo_key: String, ir_photo_key: String, date_created: Date, is_deleted: Bool = false, issue: Issue? = nil) {
        self.id = id
        self.ir_session = ir_session
        self.node = node
        self.sld = sld
        self.visual_photo_key = visual_photo_key
        self.ir_photo_key = ir_photo_key
        self.date_created = date_created
        self.is_deleted = is_deleted
        self.issue = issue
    }
}

// MARK: - Extensions for sync tracking
extension IRSession {
    var lastSyncedAt: Date? {
        get {
            // This would need to be a stored property in your actual model
            // For now, returning nil as placeholder
            nil
        }
        set {
            // Set the value when implemented
        }
    }
    
    var needsSync: Bool {
        get {
            // This would need to be a stored property in your actual model
            // For now, returning false as placeholder
            false
        }
        set {
            // Set the value when implemented
        }
    }
}
