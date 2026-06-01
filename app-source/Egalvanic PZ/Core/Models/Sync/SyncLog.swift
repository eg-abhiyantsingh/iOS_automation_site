import Foundation
import SwiftData

// MARK: - Sync Log Model for Audit Trail
@Model
final class SyncLog {
    var id: UUID = UUID()
    var timestamp: Date = Date()
    var targetRawValue: Int
    var operationRawValue: Int
    var entityId: UUID?
    var entityLabel: String?
    var success: Bool
    var errorMessage: String?
    var httpStatusCode: Int?
    var duration: TimeInterval?
    var isRequeued: Bool = false

    // Photo-specific metadata
    var photoNodeId: UUID?
    var photoOriginalPath: String?
    var photoAttemptedPath: String?
    var photoRemoteURL: String?

    // Mapping-specific metadata
    var mappingType: String?
    var mappingIds: String?

    // ZP-1847: per-user/per-site scoping + snapshot copy so a failed item can be
    // requeued with full state even after entity tables have been wiped.
    var userId: UUID?
    var siteId: UUID?
    var snapshotJSON: Data?
    var photoFilePath: String?

    var target: SyncTarget {
        SyncTarget(rawValue: targetRawValue) ?? .node
    }

    var operation: SyncOperation {
        SyncOperation(rawValue: operationRawValue) ?? .create
    }

    init(target: SyncTarget,
         operation: SyncOperation,
         entityId: UUID?,
         entityLabel: String?,
         success: Bool,
         errorMessage: String? = nil,
         httpStatusCode: Int? = nil,
         duration: TimeInterval? = nil,
         userId: UUID? = nil,
         siteId: UUID? = nil,
         snapshotJSON: Data? = nil,
         photoFilePath: String? = nil) {
        self.targetRawValue = target.rawValue
        self.operationRawValue = operation.rawValue
        self.entityId = entityId
        self.entityLabel = entityLabel
        self.success = success
        self.errorMessage = errorMessage
        self.httpStatusCode = httpStatusCode
        self.duration = duration
        self.userId = userId
        self.siteId = siteId
        self.snapshotJSON = snapshotJSON
        self.photoFilePath = photoFilePath
    }

    // Convenience method for setting photo metadata
    func setPhotoMetadata(nodeId: UUID?, originalPath: String?, attemptedPath: String?, remoteURL: String?) {
        self.photoNodeId = nodeId
        self.photoOriginalPath = originalPath
        self.photoAttemptedPath = attemptedPath
        self.photoRemoteURL = remoteURL
    }

    // Convenience method for setting mapping metadata
    func setMappingMetadata(type: String, ids: String) {
        self.mappingType = type
        self.mappingIds = ids
    }
}
