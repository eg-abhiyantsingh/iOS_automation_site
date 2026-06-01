import Foundation
import SwiftData

// MARK: - Network Mode
enum NetworkMode {
    case online, offline
    mutating func toggle() {
        self = (self == .online) ? .offline : .online
    }
}

// MARK: - Sync Target
enum SyncTarget: Int, Codable {
    case node = 0
    case edge = 1
    case photo = 2
    case userTask = 3
    case irPhoto = 4
    case irSession = 5
    case issue = 6
    case quote = 7
    case mappingIssueTask = 8
    case mappingTaskSession = 9
    case mappingQuoteTask = 10
    case mappingUserTask = 11
    case mappingTaskNode = 12
    case mappingTaskForm = 13
    case formInstance = 14
    case mappingTaskFormInstance = 15
    case mappingFormInstanceNode = 16
    case mappingNodeSession = 17
    case building = 18
    case floor = 19
    case room = 20
    case attachment = 21
    case mappingAttachmentNode = 22
    case mappingNodeSLDView = 23
    case mappingEdgeSLDView = 24
    case sldView = 25
    case mappingUserSession = 26
    case mappingTaskNodeBulkCompletion = 27
    case egFormInstance = 28
    case mappingTaskEGFormInstance = 29
    case mappingEGFormInstanceNode = 30

    /// Mapping targets cannot be recovered from a SyncLog alone because their payload
    /// (relationships, isDeleted flag, position, etc.) lives only on the original
    /// SyncQueueItem.mappingDataJSON. UI "Retry" and self-heal matching need to treat
    /// mappings differently from entity targets.
    var isMapping: Bool {
        switch self {
        case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask,
             .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance,
             .mappingFormInstanceNode, .mappingNodeSession, .mappingAttachmentNode,
             .mappingNodeSLDView, .mappingEdgeSLDView, .mappingUserSession,
             .mappingTaskNodeBulkCompletion,
             .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
            return true
        case .node, .edge, .photo, .userTask, .irPhoto, .irSession, .issue, .quote,
             .formInstance, .building, .floor, .room, .attachment, .sldView,
             .egFormInstance:
            return false
        }
    }
}

// MARK: - Sync Operation
enum SyncOperation: Int, Codable {
    case create = 0
    case read = 1
    case update = 2
    case delete = 3
}

// MARK: - Sync Operation Data
struct SyncOp {
    let target: SyncTarget
    let operation: SyncOperation
    let node: NodeV2?
    let edge: EdgeV2?
    let photo: Photo?
    let userTask: UserTask?
    let irPhoto: IRPhoto?
    let irSession: IRSession?
    let issue: Issue?
    let quote: Quote?
    let formInstance: FormInstance?
    let egFormInstance: EGFormInstance?
    let building: Building?
    let floor: Floor?
    let room: Room?
    let attachment: Attachment?
    let mappingNodeSLDView: MappingNodeSLDView?
    let mappingEdgeSLDView: MappingEdgeSLDView?
    let sldView: SLDViewV2?
    let mappingData: MappingData?
    let queueItemId: UUID?
    let extraData: [String: Any]?  // For passing additional data to API (e.g., force_type_change)

    // ZP-1847 — snapshot context loaded alongside the queue row at flush time.
    // Non-nil snapshotJSON enables the snapshot fast-path in
    // SyncExecutionService.processSyncOperationWithRetry; nil means the
    // legacy live-fetch path runs (pre-migration items).
    let snapshotJSON: Data?
    let photoFilePath: String?
    let userId: UUID?
    let siteId: UUID?
    /// Target-specific primary key copied from the SyncQueueItem (nodeId,
    /// edgeId, …) so the snapshot fast-path can build URLs without a live
    /// SwiftData fetch. Populated by `SyncQueueService.getPendingSyncOps`;
    /// nil on the create-time path (entity refs carry the id directly).
    let entityId: UUID?

    // Convenience initializer for entities
    init(target: SyncTarget, operation: SyncOperation, node: NodeV2? = nil, edge: EdgeV2? = nil, photo: Photo? = nil, userTask: UserTask? = nil, irPhoto: IRPhoto? = nil, irSession: IRSession? = nil, issue: Issue? = nil, quote: Quote? = nil, formInstance: FormInstance? = nil, egFormInstance: EGFormInstance? = nil, building: Building? = nil, floor: Floor? = nil, room: Room? = nil, attachment: Attachment? = nil, mappingNodeSLDView: MappingNodeSLDView? = nil, mappingEdgeSLDView: MappingEdgeSLDView? = nil, sldView: SLDViewV2? = nil, mappingData: MappingData? = nil, extraData: [String: Any]? = nil) {
        self.target = target
        self.operation = operation
        self.node = node
        self.edge = edge
        self.photo = photo
        self.userTask = userTask
        self.irPhoto = irPhoto
        self.irSession = irSession
        self.issue = issue
        self.quote = quote
        self.formInstance = formInstance
        self.egFormInstance = egFormInstance
        self.building = building
        self.floor = floor
        self.room = room
        self.attachment = attachment
        self.mappingNodeSLDView = mappingNodeSLDView
        self.mappingEdgeSLDView = mappingEdgeSLDView
        self.sldView = sldView
        self.mappingData = mappingData
        self.queueItemId = nil
        self.extraData = extraData
        self.snapshotJSON = nil
        self.photoFilePath = nil
        self.userId = nil
        self.siteId = nil
        self.entityId = nil
    }

    // Internal initializer with queueItemId (for loading from SwiftData)
    init(target: SyncTarget, operation: SyncOperation, node: NodeV2? = nil, edge: EdgeV2? = nil, photo: Photo? = nil, userTask: UserTask? = nil, irPhoto: IRPhoto? = nil, irSession: IRSession? = nil, issue: Issue? = nil, quote: Quote? = nil, formInstance: FormInstance? = nil, egFormInstance: EGFormInstance? = nil, building: Building? = nil, floor: Floor? = nil, room: Room? = nil, attachment: Attachment? = nil, mappingNodeSLDView: MappingNodeSLDView? = nil, mappingEdgeSLDView: MappingEdgeSLDView? = nil, sldView: SLDViewV2? = nil, mappingData: MappingData? = nil, queueItemId: UUID?, extraData: [String: Any]? = nil, snapshotJSON: Data? = nil, photoFilePath: String? = nil, userId: UUID? = nil, siteId: UUID? = nil, entityId: UUID? = nil) {
        self.target = target
        self.operation = operation
        self.node = node
        self.edge = edge
        self.photo = photo
        self.userTask = userTask
        self.irPhoto = irPhoto
        self.irSession = irSession
        self.issue = issue
        self.quote = quote
        self.formInstance = formInstance
        self.egFormInstance = egFormInstance
        self.building = building
        self.floor = floor
        self.room = room
        self.attachment = attachment
        self.mappingNodeSLDView = mappingNodeSLDView
        self.mappingEdgeSLDView = mappingEdgeSLDView
        self.sldView = sldView
        self.mappingData = mappingData
        self.queueItemId = queueItemId
        self.extraData = extraData
        self.snapshotJSON = snapshotJSON
        self.photoFilePath = photoFilePath
        self.userId = userId
        self.siteId = siteId
        self.entityId = entityId
    }
}

// MARK: - Sync Result
struct SyncResult {
    let success: Bool
    let operation: SyncOp
    let error: Error?
    let entityId: UUID?
    /// ZP-1265 (aa123c2) — number of HTTP attempts the inner retry loop ran for
    /// this op. Surfaced so the chunk processor can bump `retryCount` by the
    /// actual attempt count (the "Retried N times" chip then reflects every
    /// individual attempt, matching Android's `persistFinalFailure` semantics).
    /// 1 by default for callers that don't track attempts.
    let attemptsRun: Int

    init(success: Bool, operation: SyncOp, error: Error?, entityId: UUID? = nil, attemptsRun: Int = 1) {
        self.success = success
        self.operation = operation
        self.error = error
        self.entityId = entityId
        self.attemptsRun = attemptsRun
    }
}

// MARK: - Sync Error
enum SyncError: LocalizedError {
    case missingEntity
    case missingEntityWithMessage(String)
    case timeout
    case fileNotFound(String)
    case fileTooLarge(Int64)

    var errorDescription: String? {
        switch self {
        case .missingEntity:
            return "Entity no longer exists"
        case .missingEntityWithMessage(let message):
            return message
        case .timeout:
            return "Operation timed out"
        case .fileNotFound(let path):
            return "File not found at: \(path)"
        case .fileTooLarge(let size):
            return "File too large: \(size / 1024 / 1024)MB"
        }
    }
}

// MARK: - Sync Configuration
struct SyncConfiguration {
    static let maxConcurrentOperations = 3
    static let maxRetryAttempts = 3
    static let chunkSize = 10
    static let retryDelay: TimeInterval = 2.0
    static let photoUploadTimeout: TimeInterval = 30.0
    static let maxPhotoSize: Int64 = 50 * 1024 * 1024 // 50MB
    static let batchFetchLimit = 100 // Max items to fetch at once

    /// Sentinel `lastFailureCode` marking a queue row whose CREATE can
    /// never succeed because both its captured snapshot and its live
    /// SwiftData entity are missing — typically a row carried over from a
    /// pre-ZP-1847 install where snapshots weren't captured at enqueue.
    /// `getPendingSyncOps` skips these so flushing doesn't churn on them;
    /// the user can still see them in the Pending tab and resolve via
    /// Retry (which clears the sentinel) or Delete.
    ///
    /// Picked well outside any plausible HTTP status (100–599) and outside
    /// the URLError range (-1…-3007 — notably `URLError.unknown` is -1) so
    /// a real network failure can't accidentally land on the sentinel and
    /// hide a regular item from the flush.
    static let unrecoverableLegacyFailureCode = -100_001
}