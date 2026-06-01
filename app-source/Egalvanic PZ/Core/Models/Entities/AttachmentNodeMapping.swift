//
//  AttachmentNodeMapping.swift
//  Egalvanic PZ
//
//  Junction table for many-to-many relationship between Attachment and Node.
//

import Foundation
import SwiftData

/// Mapping model for associating attachments with nodes (many-to-many relationship).
@Model
final class AttachmentNodeMapping {
    var id: UUID
    var attachmentId: UUID
    var nodeId: UUID
    var isDeleted: Bool
    var createdAt: Date

    // MARK: - Initializer

    init(
        id: UUID = UUID(),
        attachmentId: UUID,
        nodeId: UUID,
        isDeleted: Bool = false,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.attachmentId = attachmentId
        self.nodeId = nodeId
        self.isDeleted = isDeleted
        self.createdAt = createdAt
    }
}
