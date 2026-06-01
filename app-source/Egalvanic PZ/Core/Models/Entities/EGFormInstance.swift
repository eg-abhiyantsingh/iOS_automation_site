//
//  EGFormInstance.swift
//  Egalvanic PZ
//
//  ZP-1723: a single submission of an EGForm. form_submission carries
//  user-entered values as a JSON string (same shape as the web V2
//  renderer's output, with base64 photos when the user is filling the
//  form on-device — backend strips base64 before serving on sync).
//
import Foundation
import SwiftData

@Model
final class EGFormInstance {
    @Attribute(.unique) var id: UUID
    var eg_form_id: UUID
    var form_submission: String?
    var submitted: Bool
    var is_deleted: Bool
    var created_at: Date
    var modified_at: Date?

    // Relationships — populated during SLD sync from mapping DTOs.
    var egForm: EGForm?
    var linkedTasks: [UserTask] = []
    var linkedNodes: [NodeV2] = []

    init(
        id: UUID,
        eg_form_id: UUID,
        form_submission: String? = nil,
        submitted: Bool = false,
        is_deleted: Bool = false,
        created_at: Date = Date(),
        modified_at: Date? = nil,
        egForm: EGForm? = nil,
        linkedTasks: [UserTask] = [],
        linkedNodes: [NodeV2] = []
    ) {
        self.id = id
        self.eg_form_id = eg_form_id
        self.form_submission = form_submission
        self.submitted = submitted
        self.is_deleted = is_deleted
        self.created_at = created_at
        self.modified_at = modified_at
        self.egForm = egForm
        self.linkedTasks = linkedTasks
        self.linkedNodes = linkedNodes
    }

    var formTitle: String { egForm?.title ?? "Untitled Form" }

    var statusDescription: String {
        if submitted { return "Submitted" }
        if let s = form_submission, !s.isEmpty, s != "{}" { return "In Progress" }
        return "Not Started"
    }
}
