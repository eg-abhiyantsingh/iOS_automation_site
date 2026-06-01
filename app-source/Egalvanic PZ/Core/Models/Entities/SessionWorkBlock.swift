//
//  SessionWorkBlock.swift
//  Egalvanic PZ
//
//  SwiftData entity for session work blocks - represents scheduled work time
//

import Foundation
import SwiftData

@Model
final class SessionWorkBlock {
    @Attribute(.unique) var id: UUID
    var session: IRSession?
    var start_time: Date
    var end_time: Date
    var work_length: Double?
    var total_days: Int?
    var notes: String?
    var created_at: Date?
    var modified_at: Date?
    var is_deleted: Bool = false
    var duration_hours: Double?

    // Denormalized fields for work blocks from other SLDs (not synced locally)
    // These are populated from the /user/schedule API endpoint
    var cached_session_id: UUID?
    var cached_session_name: String?
    var cached_session_description: String?
    var cached_sld_id: UUID?
    var cached_sld_name: String?

    init(
        id: UUID,
        session: IRSession? = nil,
        start_time: Date,
        end_time: Date,
        work_length: Double? = nil,
        total_days: Int? = nil,
        notes: String? = nil,
        created_at: Date? = nil,
        modified_at: Date? = nil,
        is_deleted: Bool = false,
        duration_hours: Double? = nil,
        cached_session_id: UUID? = nil,
        cached_session_name: String? = nil,
        cached_session_description: String? = nil,
        cached_sld_id: UUID? = nil,
        cached_sld_name: String? = nil
    ) {
        self.id = id
        self.session = session
        self.start_time = start_time
        self.end_time = end_time
        self.work_length = work_length
        self.total_days = total_days
        self.notes = notes
        self.created_at = created_at
        self.modified_at = modified_at
        self.is_deleted = is_deleted
        self.duration_hours = duration_hours
        self.cached_session_id = cached_session_id
        self.cached_session_name = cached_session_name
        self.cached_session_description = cached_session_description
        self.cached_sld_id = cached_sld_id
        self.cached_sld_name = cached_sld_name
    }

    /// Computed property for display: returns session name from relationship or cached value
    var sessionName: String {
        session?.name ?? cached_session_name ?? "Unnamed Session"
    }

    /// Computed property for display: returns session description from relationship or cached value
    var sessionDescription: String? {
        session?.sessionDescription ?? cached_session_description
    }

    /// Computed property for display: returns SLD name from relationship or cached value
    var siteName: String? {
        session?.sld.name ?? cached_sld_name
    }

    /// Computed property: returns SLD ID from relationship or cached value
    var sldId: UUID? {
        session?.sld.id ?? cached_sld_id
    }

    /// Computed property for display: formatted time range (e.g., "9:00 AM - 5:00 PM")
    var timeRangeDisplay: String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        formatter.dateStyle = .none
        return "\(formatter.string(from: start_time)) - \(formatter.string(from: end_time))"
    }

    /// Computed property for display: formatted date (e.g., "Mon, Jan 15")
    var dateDisplay: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEE, MMM d"
        return formatter.string(from: start_time)
    }
}
