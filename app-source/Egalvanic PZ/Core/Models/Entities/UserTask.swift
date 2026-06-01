//
//  UserTask.swift
//  SwiftDataTutorial
//
import Foundation
import SwiftData

@Model
final class UserTask {
    @Attribute(.unique) var id: UUID
    var title: String
    var task_description: String
    var completed: Bool
    var form: UserTaskForm?
    var node: NodeV2?
    @Relationship var linkedNodes: [NodeV2] = []  // New: many-to-many relationship
    var linkedForms: [UserTaskForm] = []  // New: many-to-many relationship
    var linkedFormInstances: [FormInstance] = []  // New: form instances instead of templates
    // ZP-1723: explicit many-to-many for EG forms. Without this
    // inverse SwiftData implicitly synthesizes a single-valued
    // reverse pointer, which makes setting `instance.linkedTasks =
    // [task]` on a second instance silently steal the task away from
    // the first — and on re-sync the loop that rebuilds from mappings
    // only retains the last assignment. Mirrors the symmetric pair
    // used for legacy: FormInstance.linkedTasks ↔ UserTask.linkedFormInstances.
    var linkedEGFormInstances: [EGFormInstance] = []
    @Relationship(inverse: \IRSession.user_tasks) var sessions: [IRSession] = []  // Bidirectional relationship with IRSession
    var sld: SLDV2?
    var is_deleted: Bool
    var submission: String?
    var submitted_at: Date?
    @Relationship(inverse: \Photo.userTask) var photos: [Photo]
    var due_date: Date?
    var created_at: Date?
    var task_type: String?
    var interval: Int?
    var recurring: Bool?
    var procedure_id: UUID?
    var shortcut_id: UUID?
    var owned_by: [UUID?]
    var assigned_to: [UUID?]
    var nodeCompletions: [String: Bool] = [:]  // node_id -> is_completed
    var lastSyncedAt: Date?

    init(id: UUID, title: String, task_description: String, completed: Bool, form: UserTaskForm? = nil, node: NodeV2? = nil, linkedNodes: [NodeV2] = [], linkedForms: [UserTaskForm] = [], linkedFormInstances: [FormInstance] = [], sessions: [IRSession] = [], sld: SLDV2? = nil, is_deleted: Bool, submission: String?, submitted_at: Date?, photos: [Photo], due_date: Date? = nil, created_at: Date? = nil, task_type: String? = nil, interval: Int? = nil, recurring: Bool? = nil, procedure_id: UUID? = nil, shortcut_id: UUID? = nil, owned_by: [UUID?] = [], assigned_to: [UUID?] = [], nodeCompletions: [String: Bool] = [:]) {
        self.id = id
        self.title = title
        self.task_description = task_description
        self.completed = completed
        self.form = form
        self.node = node
        self.linkedNodes = linkedNodes
        self.linkedForms = linkedForms
        self.linkedFormInstances = linkedFormInstances
        self.sessions = sessions
        self.sld = sld
        self.is_deleted = is_deleted
        self.submission = submission
        self.submitted_at = submitted_at
        self.photos = photos
        self.due_date = due_date
        self.created_at = created_at
        self.completed = completed
        self.task_type = task_type
        self.interval = interval
        self.recurring = recurring
        self.procedure_id = procedure_id
        self.shortcut_id = shortcut_id
        self.owned_by = owned_by
        self.assigned_to = assigned_to
        self.nodeCompletions = nodeCompletions
    }
}

@Model
final class UserTaskPool {
    @Attribute(.unique) var id: UUID
    var user_tasks: [UserTask]
    
    init(id: UUID, user_tasks: [UserTask], user_task_forms: [UserTaskForm]) {
        self.id = id
        self.user_tasks = user_tasks
    }
}