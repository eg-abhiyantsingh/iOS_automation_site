import Foundation
import SwiftData

// MARK: - SwiftData Model for Persistent Sync Queue
@Model
final class SyncQueueItem {
    var id: UUID = UUID()
    var targetRawValue: Int
    var operationRawValue: Int
    var nodeId: UUID?
    var edgeId: UUID?
    var photoId: UUID?
    var userTaskId: UUID?
    var irPhotoId: UUID?
    var irSessionId: UUID?
    var issueId: UUID?
    var quoteId: UUID?
    var formInstanceId: UUID?
    var egFormInstanceId: UUID?
    var buildingId: UUID?
    var floorId: UUID?
    var roomId: UUID?
    var attachmentId: UUID?
    var sldViewId: UUID?
    var mappingDataJSON: Data?  // Store mapping data as JSON
    var extraDataJSON: Data?  // Store extra data (like force_type_change) as JSON
    var createdAt: Date = Date()
    var retryCount: Int = 0

    // ZP-1847: self-contained queue item — survives site switch / logout / account switch.
    // Snapshot is the entity DTO at enqueue time; flush prefers it over a live SwiftData
    // fetch so queued work is not lost when entity tables are wiped.
    var userId: UUID?
    var siteId: UUID?
    var snapshotJSON: Data?
    var snapshotTimestamp: Date?
    var photoFilePath: String?

    // ZP-2097 — strict-stop failure tracking. Populated when a flush attempt fails
    // on this item; cleared by the per-item Retry action so the UI doesn't show
    // stale failure text while the new attempt is in flight. "Failed" is derived
    // from `lastFailureAt != nil` — there is no separate status enum.
    // retryCount above stays as a historical "Retried N times" signal but no
    // longer drives auto-deletion; items live forever until explicitly resolved.
    var lastFailureMessage: String?
    var lastFailureAt: Date?
    var lastFailureCode: Int?

    // Computed properties for enum access
    var target: SyncTarget {
        get { SyncTarget(rawValue: targetRawValue) ?? .node }
        set { targetRawValue = newValue.rawValue }
    }

    var operation: SyncOperation {
        get { SyncOperation(rawValue: operationRawValue) ?? .create }
        set { operationRawValue = newValue.rawValue }
    }

    var mappingData: MappingData? {
        get {
            guard let json = mappingDataJSON else { return nil }
            return try? JSONDecoder().decode(MappingData.self, from: json)
        }
        set {
            mappingDataJSON = try? JSONEncoder().encode(newValue)
        }
    }

    var extraData: [String: Any]? {
        get {
            guard let json = extraDataJSON else { return nil }
            return try? JSONSerialization.jsonObject(with: json) as? [String: Any]
        }
        set {
            extraDataJSON = try? JSONSerialization.data(withJSONObject: newValue ?? [:])
        }
    }

    init(target: SyncTarget,
         operation: SyncOperation,
         nodeId: UUID? = nil,
         edgeId: UUID? = nil,
         photoId: UUID? = nil,
         userTaskId: UUID? = nil,
         irPhotoId: UUID? = nil,
         irSessionId: UUID? = nil,
         issueId: UUID? = nil,
         quoteId: UUID? = nil,
         formInstanceId: UUID? = nil,
         egFormInstanceId: UUID? = nil,
         buildingId: UUID? = nil,
         floorId: UUID? = nil,
         roomId: UUID? = nil,
         attachmentId: UUID? = nil,
         sldViewId: UUID? = nil,
         mappingData: MappingData? = nil,
         extraData: [String: Any]? = nil,
         userId: UUID? = nil,
         siteId: UUID? = nil,
         snapshotJSON: Data? = nil,
         snapshotTimestamp: Date? = nil,
         photoFilePath: String? = nil,
         lastFailureMessage: String? = nil,
         lastFailureAt: Date? = nil,
         lastFailureCode: Int? = nil) {
        self.targetRawValue = target.rawValue
        self.operationRawValue = operation.rawValue
        self.nodeId = nodeId
        self.edgeId = edgeId
        self.photoId = photoId
        self.userTaskId = userTaskId
        self.irPhotoId = irPhotoId
        self.irSessionId = irSessionId
        self.issueId = issueId
        self.quoteId = quoteId
        self.formInstanceId = formInstanceId
        self.egFormInstanceId = egFormInstanceId
        self.buildingId = buildingId
        self.floorId = floorId
        self.roomId = roomId
        self.attachmentId = attachmentId
        self.sldViewId = sldViewId
        self.mappingData = mappingData
        self.extraData = extraData
        self.userId = userId
        self.siteId = siteId
        self.snapshotJSON = snapshotJSON
        self.snapshotTimestamp = snapshotTimestamp
        self.photoFilePath = photoFilePath
        self.lastFailureMessage = lastFailureMessage
        self.lastFailureAt = lastFailureAt
        self.lastFailureCode = lastFailureCode
    }
}
