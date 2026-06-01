//
//  Issue.swift
//  SwiftDataTutorial
//
import Foundation
import SwiftData

@Model
final class Issue {
    @Attribute(.unique) var id: UUID
    var title: String?
    var issueDescription: String?
    var created_date: Date?
    var node: NodeV2?
    var issue_class: IssueClass?
    var issue_type: String?
    var issue_subtype: String?
    var is_deleted: Bool
    @Relationship var session: IRSession?
    var sld: SLDV2?
    var details: [IssueProperty] = []
    var status: String?
    var proposed_resolution: String?
    var modified_date: Date?
    var priority: String?
    var tasks: [UserTask]
    var ir_photos: [IRPhoto] = []
    @Relationship(inverse: \Photo.issue) var photos: [Photo] = []
    var immediateHazard: Bool = false
    var customerNotified: Bool = false
    var lastSyncedAt: Date?
    @Relationship(inverse: \IssueStatusHistory.issue) var statusHistory: [IssueStatusHistory] = []

    init(id: UUID, title: String?, issueDescription: String?, created_date: Date?, node: NodeV2? = nil, issue_class: IssueClass? = nil, issue_type: String?, issue_subtype: String?, is_deleted: Bool, session: IRSession? = nil, sld: SLDV2? = nil, details: [IssueProperty] = [], status: String?, proposed_resolution: String?, modified_date: Date?, priority: String? = nil, tasks: [UserTask] = [], ir_photos: [IRPhoto] = [], photos: [Photo] = [], immediateHazard: Bool = false, customerNotified: Bool = false, statusHistory: [IssueStatusHistory] = []) {
        self.id = id
        self.title = title
        self.issueDescription = issueDescription
        self.created_date = created_date
        self.node = node
        self.issue_class = issue_class
        self.issue_type = issue_type
        self.issue_subtype = issue_subtype
        self.is_deleted = is_deleted
        self.session = session
        self.sld = sld
        self.details = details
        self.status = status
        self.proposed_resolution = proposed_resolution
        self.modified_date = modified_date
        self.priority = priority
        self.tasks = tasks
        self.ir_photos = ir_photos
        self.photos = photos
        self.immediateHazard = immediateHazard
        self.customerNotified = customerNotified
        self.statusHistory = statusHistory
    }
}
