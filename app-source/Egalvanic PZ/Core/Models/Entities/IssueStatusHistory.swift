//
//  IssueStatusHistory.swift
//  Egalvanic PZ
//

import Foundation
import SwiftData

@Model
final class IssueStatusHistory {
    @Attribute(.unique) var id: UUID
    var issue_id: UUID
    var old_status: String?
    var new_status: String
    var changed_by: UUID?
    var changed_by_name: String?
    var changed_at: Date?
    var change_reason: String?
    var issue: Issue?

    init(id: UUID, issue_id: UUID, old_status: String?, new_status: String,
         changed_by: UUID?, changed_by_name: String?, changed_at: Date?,
         change_reason: String?, issue: Issue? = nil) {
        self.id = id
        self.issue_id = issue_id
        self.old_status = old_status
        self.new_status = new_status
        self.changed_by = changed_by
        self.changed_by_name = changed_by_name
        self.changed_at = changed_at
        self.change_reason = change_reason
        self.issue = issue
    }
}
