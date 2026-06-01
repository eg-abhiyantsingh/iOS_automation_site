//
//  NodeTerminal.swift
//  Egalvanic PZ
//
//  SwiftData model for node terminal instances.
//  These are actual terminals instantiated per asset/node,
//  derived from NodeOrientationTerminal blueprints.
//

import SwiftUI
import SwiftData

@Model
final class NodeTerminal {
    var id: UUID
    var node: NodeV2?
    var node_orientation_terminal_id: UUID

    // Rendering properties (derived from blueprint but stored for offline access)
    var handle_code: String?
    var label: String?
    var side: String?  // "LINE" = input/target, "LOAD" = output/source
    var position: String?  // "top", "bottom", "left", "right"
    var offset_percent: Double?
    var color: String?
    var show_label: Bool?

    // Electrical properties (instance-specific)
    var voltage_rating: Double?
    var amp_rating: Double?
    var phase_count: Int?

    // Metadata
    var is_deleted: Bool = false
    var created_at: Date?
    var updated_at: Date?
    var lastSyncedAt: Date?
    var lastModifiedAt: Date = Date()
    var needsSync: Bool = false

    init(
        id: UUID,
        node: NodeV2? = nil,
        node_orientation_terminal_id: UUID,
        handle_code: String? = nil,
        label: String? = nil,
        side: String? = nil,
        position: String? = nil,
        offset_percent: Double? = nil,
        color: String? = nil,
        show_label: Bool? = nil,
        voltage_rating: Double? = nil,
        amp_rating: Double? = nil,
        phase_count: Int? = nil,
        is_deleted: Bool = false,
        created_at: Date? = nil,
        updated_at: Date? = nil
    ) {
        self.id = id
        self.node = node
        self.node_orientation_terminal_id = node_orientation_terminal_id
        self.handle_code = handle_code
        self.label = label
        self.side = side
        self.position = position
        self.offset_percent = offset_percent
        self.color = color
        self.show_label = show_label
        self.voltage_rating = voltage_rating
        self.amp_rating = amp_rating
        self.phase_count = phase_count
        self.is_deleted = is_deleted
        self.created_at = created_at
        self.updated_at = updated_at
    }

    /// Creates a NodeTerminal from a DTO
    convenience init(from dto: NodeTerminalDTO, node: NodeV2? = nil) {
        self.init(
            id: dto.id,
            node: node,
            node_orientation_terminal_id: dto.node_orientation_terminal_id,
            handle_code: dto.handle_code,
            label: dto.label,
            side: dto.side,
            position: dto.position,
            offset_percent: dto.offset_percent,
            color: dto.color,
            show_label: dto.show_label,
            voltage_rating: dto.voltage_rating,
            amp_rating: dto.amp_rating,
            phase_count: dto.phase_count,
            is_deleted: dto.is_deleted,
            created_at: ISO8601DateFormatter().date(from: dto.created_at ?? ""),
            updated_at: ISO8601DateFormatter().date(from: dto.updated_at ?? "")
        )
    }

    /// Updates this terminal from a DTO
    func update(from dto: NodeTerminalDTO) {
        self.node_orientation_terminal_id = dto.node_orientation_terminal_id
        self.handle_code = dto.handle_code
        self.label = dto.label
        self.side = dto.side
        self.position = dto.position
        self.offset_percent = dto.offset_percent
        self.color = dto.color
        self.show_label = dto.show_label
        self.voltage_rating = dto.voltage_rating
        self.amp_rating = dto.amp_rating
        self.phase_count = dto.phase_count
        self.is_deleted = dto.is_deleted
        if let createdStr = dto.created_at {
            self.created_at = ISO8601DateFormatter().date(from: createdStr)
        }
        if let updatedStr = dto.updated_at {
            self.updated_at = ISO8601DateFormatter().date(from: updatedStr)
        }
        self.lastSyncedAt = Date()
    }

    /// Converts to DTO for WebView/API
    func toDTO() -> NodeTerminalDTO {
        NodeTerminalDTO(
            id: id,
            node_id: node?.id ?? UUID(),
            node_orientation_terminal_id: node_orientation_terminal_id,
            handle_code: handle_code,
            label: label,
            side: side,
            position: position,
            offset_percent: offset_percent,
            color: color,
            show_label: show_label,
            voltage_rating: voltage_rating,
            amp_rating: amp_rating,
            phase_count: phase_count,
            is_deleted: is_deleted,
            created_at: created_at.map { ISO8601DateFormatter().string(from: $0) },
            updated_at: updated_at.map { ISO8601DateFormatter().string(from: $0) }
        )
    }
}
