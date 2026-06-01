//
//  IssueStatusHistoryDTO.swift
//  Egalvanic PZ
//

import Foundation

struct IssueStatusHistoryDTO: Codable {
    var id: UUID
    var issue_id: UUID
    var old_status: String?
    var new_status: String
    var changed_by: UUID?
    var changed_by_name: String?
    var changed_at: Date?
    var change_reason: String?

    enum CodingKeys: String, CodingKey {
        case id, issue_id, old_status, new_status, changed_by, changed_by_name, changed_at, change_reason
    }

    init(id: UUID, issue_id: UUID, old_status: String?, new_status: String,
         changed_by: UUID?, changed_by_name: String?, changed_at: Date?,
         change_reason: String?) {
        self.id = id
        self.issue_id = issue_id
        self.old_status = old_status
        self.new_status = new_status
        self.changed_by = changed_by
        self.changed_by_name = changed_by_name
        self.changed_at = changed_at
        self.change_reason = change_reason
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        issue_id = try container.decode(UUID.self, forKey: .issue_id)
        old_status = try? container.decodeIfPresent(String.self, forKey: .old_status)
        new_status = try container.decode(String.self, forKey: .new_status)
        changed_by = try? container.decodeIfPresent(UUID.self, forKey: .changed_by)
        changed_by_name = try? container.decodeIfPresent(String.self, forKey: .changed_by_name)
        changed_at = try? container.decodeIfPresent(Date.self, forKey: .changed_at)
        change_reason = try? container.decodeIfPresent(String.self, forKey: .change_reason)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(issue_id, forKey: .issue_id)
        try container.encodeIfPresent(old_status, forKey: .old_status)
        try container.encode(new_status, forKey: .new_status)
        try container.encodeIfPresent(changed_by, forKey: .changed_by)
        try container.encodeIfPresent(changed_by_name, forKey: .changed_by_name)
        try container.encodeIfPresent(changed_at, forKey: .changed_at)
        try container.encodeIfPresent(change_reason, forKey: .change_reason)
    }
}
