//
//  AttachmentDTO.swift
//  Egalvanic PZ
//
//  Data Transfer Objects for attachment API communication.
//

import Foundation

// MARK: - Attachment DTO (for syncing from server)

struct AttachmentDTO: Codable {
    let id: String
    let company_id: String
    let sld_id: String
    let session_id: String?
    let task_id: String?
    let type: String
    let filename: String
    let file_size: Int64
    let key: String
    let is_deleted: Bool
    let visibility: String?
    let visibility_id: String?
    let created_at: String
    let modified_at: String?
}

// MARK: - Presigned Upload Request/Response

struct AttachmentPresignedUploadRequest: Codable {
    let filename: String
    let file_size: Int64
}

struct AttachmentPresignedUploadResponse: Codable {
    let url: String
    let key: String
}

// MARK: - Attachment Create Request/Response

struct AttachmentCreateRequest: Codable {
    let id: String
    let company_id: String
    let sld_id: String
    let session_id: String?
    let task_id: String?
    let type: String
    let filename: String
    let file_size: Int64
    let key: String
    let visibility: String

    init(attachment: Attachment) {
        self.id = attachment.id.uuidString.lowercased()
        self.company_id = attachment.companyId.uuidString.lowercased()
        self.sld_id = attachment.sldId.uuidString.lowercased()
        self.session_id = attachment.sessionId?.uuidString.lowercased()
        self.task_id = attachment.taskId?.uuidString.lowercased()
        self.type = attachment.type
        self.filename = attachment.filename
        self.file_size = attachment.fileSize
        self.key = attachment.key
        self.visibility = attachment.visibility
    }
}

struct AttachmentCreateResponse: Codable {
    let id: String
    let company_id: String
    let sld_id: String
    let session_id: String?
    let task_id: String?
    let type: String
    let filename: String
    let file_size: Int64
    let key: String
    let is_deleted: Bool
    let visibility: String?
    let visibility_id: String?
    let created_at: String
    let modified_at: String?
}

// MARK: - Attachment Node Mapping Request/Response

struct AttachmentNodeMappingRequest: Codable {
    let attachment_id: String
    let node_id: String

    init(attachmentId: UUID, nodeId: UUID) {
        self.attachment_id = attachmentId.uuidString.lowercased()
        self.node_id = nodeId.uuidString.lowercased()
    }
}

struct AttachmentNodeMappingResponse: Codable {
    let id: String
    let attachment_id: String
    let node_id: String
    let created_at: String
}

// MARK: - Attachment Node Mapping DTO (for syncing from server)

struct MappingAttachmentNodeDTO: Codable {
    let id: String
    let attachment_id: String
    let node_id: String
    let is_deleted: Bool

    var attachmentId: UUID? {
        UUID(uuidString: attachment_id)
    }

    var nodeId: UUID? {
        UUID(uuidString: node_id)
    }
}

// MARK: - Presigned Download Response

struct AttachmentPresignedDownloadResponse: Codable {
    let expires_in: Int
    let filename: String
    let url: String
}

// MARK: - Attachment Visibility Update Requests

struct AttachmentUpdateVisibilityRequest: Codable {
    let visibility: String
}

struct AttachmentBulkVisibilityRequest: Codable {
    let attachment_ids: [String]
    let visibility: String
}

// MARK: - Attachment List Response

struct AttachmentListResponse: Codable {
    let attachments: [AttachmentDTO]?

    // Handle both array and object response formats
    init(from decoder: Decoder) throws {
        // Try to decode as an array first
        if let container = try? decoder.singleValueContainer(),
           let attachments = try? container.decode([AttachmentDTO].self) {
            self.attachments = attachments
            return
        }

        // Try to decode as object with attachments key
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.attachments = try container.decodeIfPresent([AttachmentDTO].self, forKey: .attachments)
    }

    private enum CodingKeys: String, CodingKey {
        case attachments
    }
}
