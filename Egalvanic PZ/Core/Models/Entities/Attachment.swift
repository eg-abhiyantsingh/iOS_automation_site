//
//  Attachment.swift
//  Egalvanic PZ
//
//  Created for attachment upload and node mapping functionality.
//

import Foundation
import SwiftData

/// Attachment model for storing file attachments associated with sessions, tasks, and nodes.
@Model
final class Attachment {
    var id: UUID
    var companyId: UUID
    var sldId: UUID
    var sessionId: UUID?
    var taskId: UUID?
    var type: String
    var filename: String
    var fileSize: Int64
    var key: String
    var localFilePath: String?
    var isDeleted: Bool
    var uploadNeeded: Bool
    var visibility: String = "internal"
    var visibilityId: String?
    var createdAt: Date
    var modifiedAt: Date?

    // MARK: - Static Constants

    static let ATTACHMENT_TYPES = [
        "Third-Party Report",
        "SLD",
        "Floor Plan",
        "General Documentation",
        "Product Manual",
        "Reference",
        "Other"
    ]

    static let MAX_FILE_SIZE: Int64 = 100 * 1000 * 1000  // 100MB (decimal, matches iOS file size display)

    // MARK: - Initializer

    init(
        id: UUID = UUID(),
        companyId: UUID,
        sldId: UUID,
        sessionId: UUID? = nil,
        taskId: UUID? = nil,
        type: String = "General Documentation",
        filename: String,
        fileSize: Int64,
        key: String = "",
        localFilePath: String? = nil,
        isDeleted: Bool = false,
        uploadNeeded: Bool = true,
        visibility: String = "internal",
        visibilityId: String? = nil,
        createdAt: Date = Date(),
        modifiedAt: Date? = nil
    ) {
        self.id = id
        self.companyId = companyId
        self.sldId = sldId
        self.sessionId = sessionId
        self.taskId = taskId
        self.type = type
        self.filename = filename
        self.fileSize = fileSize
        self.key = key
        self.localFilePath = localFilePath
        self.isDeleted = isDeleted
        self.uploadNeeded = uploadNeeded
        self.visibility = visibility
        self.visibilityId = visibilityId
        self.createdAt = createdAt
        self.modifiedAt = modifiedAt
    }

    // MARK: - Computed Properties

    /// File extension extracted from filename
    var fileExtension: String {
        (filename as NSString).pathExtension.lowercased()
    }

    /// Human-readable file size
    var formattedFileSize: String {
        ByteCountFormatter.string(fromByteCount: fileSize, countStyle: .file)
    }

    /// Parsed visibility enum (defaults to .internal if raw value is unrecognized)
    var attachmentVisibility: AttachmentVisibility {
        AttachmentVisibility(rawValue: visibility) ?? .internal
    }

    /// Content type based on file extension
    var contentType: String {
        switch fileExtension {
        case "pdf":
            return "application/pdf"
        case "doc":
            return "application/msword"
        case "docx":
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        case "xls":
            return "application/vnd.ms-excel"
        case "xlsx":
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        case "ppt":
            return "application/vnd.ms-powerpoint"
        case "pptx":
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        case "jpg", "jpeg":
            return "image/jpeg"
        case "png":
            return "image/png"
        case "gif":
            return "image/gif"
        case "txt":
            return "text/plain"
        case "csv":
            return "text/csv"
        case "zip":
            return "application/zip"
        case "rar":
            return "application/x-rar-compressed"
        default:
            return "application/octet-stream"
        }
    }
}
