//
//  IssueDTO.swift
//  SwiftDataTutorial
//
import Foundation

struct IssueDTO: Codable {
    var id: UUID
    var title: String?
    var description: String?
    var created_date: Date?
    var node_id: UUID?
    var issue_class: UUID?
    var issue_type: String?
    var issue_subtype: String?
    var is_deleted: Bool
    var session_id: UUID?
    var sld_id: UUID?
    var details: [IssuePropertyDTO]?
    var status: String?
    var proposed_resolution: String?
    var modified_date: Date?
    var priority: String?
    var immediate_hazard: Bool?
    var customer_notified: Bool?
    var status_history: [IssueStatusHistoryDTO]?

    enum CodingKeys: String, CodingKey {
        case id, title, description, created_date, node_id, issue_class, issue_type, issue_subtype
        case is_deleted, session_id, sld_id, details, status, proposed_resolution, modified_date, priority
        case immediate_hazard, customer_notified, status_history
    }

    // Standard initializer for programmatic creation
    init(id: UUID, title: String? = nil, description: String? = nil, created_date: Date? = nil,
         node_id: UUID? = nil, issue_class: UUID? = nil, issue_type: String? = nil,
         issue_subtype: String? = nil, is_deleted: Bool = false, session_id: UUID? = nil,
         sld_id: UUID? = nil, details: [IssuePropertyDTO]? = nil, status: String? = nil,
         proposed_resolution: String? = nil, modified_date: Date? = nil, priority: String? = nil,
         immediate_hazard: Bool? = false, customer_notified: Bool? = false,
         status_history: [IssueStatusHistoryDTO]? = nil) {
        self.id = id
        self.title = title
        self.description = description
        self.created_date = created_date
        self.node_id = node_id
        self.issue_class = issue_class
        self.issue_type = issue_type
        self.issue_subtype = issue_subtype
        self.is_deleted = is_deleted
        self.session_id = session_id
        self.sld_id = sld_id
        self.details = details
        self.status = status
        self.proposed_resolution = proposed_resolution
        self.modified_date = modified_date
        self.priority = priority
        self.immediate_hazard = immediate_hazard
        self.customer_notified = customer_notified
        self.status_history = status_history
    }

    // Custom decoder for resilient parsing
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        id = try container.decode(UUID.self, forKey: .id)
        title = try? container.decodeIfPresent(String.self, forKey: .title)
        description = try? container.decodeIfPresent(String.self, forKey: .description)
        created_date = try? container.decodeIfPresent(Date.self, forKey: .created_date)
        node_id = try? container.decodeIfPresent(UUID.self, forKey: .node_id)

        // Resilient parsing for issue_class UUID - handle invalid/empty UUIDs gracefully
        if let issueClassString = try? container.decodeIfPresent(String.self, forKey: .issue_class),
           !issueClassString.isEmpty,
           let uuid = UUID(uuidString: issueClassString) {
            issue_class = uuid
        } else if let uuid = try? container.decodeIfPresent(UUID.self, forKey: .issue_class) {
            issue_class = uuid
        } else {
            issue_class = nil
            if let invalidValue = try? container.decodeIfPresent(String.self, forKey: .issue_class), !invalidValue.isEmpty {
                AppLogger.log(.notice, "Issue \(id): issue_class has invalid UUID '\(invalidValue)', setting to nil", category: .issue)
            }
        }

        issue_type = try? container.decodeIfPresent(String.self, forKey: .issue_type)
        issue_subtype = try? container.decodeIfPresent(String.self, forKey: .issue_subtype)
        is_deleted = try container.decodeIfPresent(Bool.self, forKey: .is_deleted) ?? false
        session_id = try? container.decodeIfPresent(UUID.self, forKey: .session_id)
        sld_id = try? container.decodeIfPresent(UUID.self, forKey: .sld_id)
        // Defensive decoding for details
        if container.contains(.details) {
            do {
                details = try container.decode([IssuePropertyDTO].self, forKey: .details)
            } catch {
                details = nil
            }
        } else {
            details = nil
        }
        status = try? container.decodeIfPresent(String.self, forKey: .status)
        proposed_resolution = try? container.decodeIfPresent(String.self, forKey: .proposed_resolution)
        modified_date = try? container.decodeIfPresent(Date.self, forKey: .modified_date)
        priority = try? container.decodeIfPresent(String.self, forKey: .priority)
        immediate_hazard = (try? container.decodeIfPresent(Bool.self, forKey: .immediate_hazard)) ?? false
        customer_notified = (try? container.decodeIfPresent(Bool.self, forKey: .customer_notified)) ?? false
        // Defensive decoding for status_history
        if container.contains(.status_history) {
            do {
                status_history = try container.decode([IssueStatusHistoryDTO].self, forKey: .status_history)
            } catch {
                status_history = nil
            }
        } else {
            status_history = nil
        }
    }

    // Custom encoder: skip status_history (read-only from server, should not be sent in create/update)
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encodeIfPresent(title, forKey: .title)
        try container.encodeIfPresent(description, forKey: .description)
        try container.encodeIfPresent(created_date, forKey: .created_date)
        try container.encodeIfPresent(node_id, forKey: .node_id)
        try container.encodeIfPresent(issue_class, forKey: .issue_class)
        try container.encodeIfPresent(issue_type, forKey: .issue_type)
        try container.encodeIfPresent(issue_subtype, forKey: .issue_subtype)
        try container.encode(is_deleted, forKey: .is_deleted)
        try container.encodeIfPresent(session_id, forKey: .session_id)
        try container.encodeIfPresent(sld_id, forKey: .sld_id)
        try container.encodeIfPresent(details, forKey: .details)
        try container.encodeIfPresent(status, forKey: .status)
        try container.encodeIfPresent(proposed_resolution, forKey: .proposed_resolution)
        try container.encodeIfPresent(modified_date, forKey: .modified_date)
        try container.encodeIfPresent(priority, forKey: .priority)
        try container.encodeIfPresent(immediate_hazard, forKey: .immediate_hazard)
        try container.encodeIfPresent(customer_notified, forKey: .customer_notified)
        // status_history intentionally omitted — read-only from server
    }
}