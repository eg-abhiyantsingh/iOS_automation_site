import Foundation
import SwiftData

/// Service responsible for deduplicating and optimizing the sync queue
@MainActor
final class SyncDeduplicationService {
    
    /// Deduplicate sync queue before processing
    func deduplicateSyncQueue(context: ModelContext) async {
        AppLogger.log(.info, "Deduplicating sync queue...", category: .sync)
        
        let descriptor = FetchDescriptor<SyncQueueItem>(
            sortBy: [SortDescriptor(\.createdAt)]
        )
        
        do {
            let allItems = try context.fetch(descriptor)
            var seenOperations = [String: [SyncQueueItem]]()
            var itemsToDelete: [SyncQueueItem] = []
            
            // Group operations by entity
            for item in allItems {
                // Special handling for mappings
                if item.target == .mappingUserTask {
                    if let mappingData = item.mappingData {
                        // Create a unique key that includes userId, taskId, and mappingType
                        let userId = mappingData.userId?.uuidString ?? "none"
                        let taskId = mappingData.taskId?.uuidString ?? "none"
                        let mappingType = mappingData.mappingType ?? "unknown"
                        let key = "userTaskMapping-\(userId)-\(taskId)-\(mappingType)"
                        
                        if seenOperations[key] == nil {
                            seenOperations[key] = []
                        }
                        seenOperations[key]?.append(item)
                    } else {
                        // Skip if no mapping data
                        continue
                    }
                } else if item.target == .mappingIssueTask || item.target == .mappingTaskSession || item.target == .mappingQuoteTask || item.target == .mappingTaskNode || item.target == .mappingTaskForm || item.target == .mappingTaskFormInstance || item.target == .mappingFormInstanceNode || item.target == .mappingNodeSession || item.target == .mappingUserSession {
                    // Handle other mapping types similarly
                    if let mappingData = item.mappingData {
                        let key: String
                        switch item.target {
                        case .mappingIssueTask:
                            let issueId = mappingData.issueId?.uuidString ?? "none"
                            let taskId = mappingData.taskId?.uuidString ?? "none"
                            key = "issueTaskMapping-\(issueId)-\(taskId)"
                        case .mappingTaskSession:
                            let taskId = mappingData.taskId?.uuidString ?? "none"
                            let sessionId = mappingData.sessionId?.uuidString ?? "none"
                            key = "taskSessionMapping-\(taskId)-\(sessionId)"
                        case .mappingQuoteTask:
                            let quoteId = mappingData.quoteId?.uuidString ?? "none"
                            let taskId = mappingData.taskId?.uuidString ?? "none"
                            key = "quoteTaskMapping-\(quoteId)-\(taskId)"
                        case .mappingTaskNode:
                            let taskId = mappingData.taskId?.uuidString ?? "none"
                            let nodeId = mappingData.nodeId?.uuidString ?? "none"
                            key = "taskNodeMapping-\(taskId)-\(nodeId)"
                        case .mappingTaskForm:
                            let taskId = mappingData.taskId?.uuidString ?? "none"
                            let formId = mappingData.formId?.uuidString ?? "none"
                            key = "taskFormMapping-\(taskId)-\(formId)"
                        case .mappingTaskFormInstance:
                            // Each task-form instance mapping is unique - different instances for the same form
                            let taskId = mappingData.taskId?.uuidString ?? "none"
                            let formInstanceId = mappingData.formInstanceId?.uuidString ?? "none"
                            key = "taskFormInstanceMapping-\(taskId)-\(formInstanceId)"
                        case .mappingFormInstanceNode:
                            let formInstanceId = mappingData.formInstanceId?.uuidString ?? "none"
                            let nodeId = mappingData.nodeId?.uuidString ?? "none"
                            key = "formInstanceNodeMapping-\(formInstanceId)-\(nodeId)"
                        case .mappingNodeSession:
                            let nodeId = mappingData.nodeId?.uuidString ?? "none"
                            let sessionId = mappingData.sessionId?.uuidString ?? "none"
                            key = "nodeSessionMapping-\(nodeId)-\(sessionId)"
                        case .mappingUserSession:
                            let userId = mappingData.userId?.uuidString ?? "none"
                            let sessionId = mappingData.sessionId?.uuidString ?? "none"
                            key = "userSessionMapping-\(userId)-\(sessionId)"
                        default:
                            continue
                        }

                        if seenOperations[key] == nil {
                            seenOperations[key] = []
                        }
                        seenOperations[key]?.append(item)
                    } else {
                        continue
                    }
                } else {
                    // Regular entities
                    let entityId = getEntityId(from: item)
                    
                    // Skip items without entity IDs - these are distinct new entities
                    guard let entityId = entityId else {
                        AppLogger.log(.debug, "Skipping deduplication for new \(item.target) (no entity ID yet)", category: .sync)
                        continue
                    }
                    
                    let key = "\(item.target.rawValue)-\(entityId.uuidString)"
                    
                    if seenOperations[key] == nil {
                        seenOperations[key] = []
                    }
                    seenOperations[key]?.append(item)
                }
            }
            
            // Now process each entity's operations
            for (key, operations) in seenOperations {
                // Sort by creation date to maintain order
                let sortedOps = operations.sorted { $0.createdAt < $1.createdAt }
                
                // For mappings, keep all operations - they represent state changes
                if key.contains("Mapping") {
                    // Don't deduplicate mapping operations at all
                    // Each one represents a state change that needs to be synced
                    // (e.g., link -> unlink -> link again are all valid operations)
                    continue
                } else {
                    // Regular entity deduplication logic
                    let creates = sortedOps.filter { $0.operation == .create }
                    let updates = sortedOps.filter { $0.operation == .update }
                    
                    // Rule 1: Keep only the first create
                    if creates.count > 1 {
                        for i in 1..<creates.count {
                            itemsToDelete.append(creates[i])
                            AppLogger.log(.debug, "Removing duplicate create for: \(creates[i].target)", category: .sync)
                        }
                    }
                    
                    // Rule 2: For updates, keep only the latest one
                    if updates.count > 1 {
                        for i in 0..<(updates.count - 1) {
                            itemsToDelete.append(updates[i])
                            AppLogger.log(.debug, "Removing older update for: \(updates[i].target)", category: .sync)
                        }
                    }
                }
            }
            
            // Delete redundant operations
            for item in itemsToDelete {
                context.delete(item)
            }
            
            if !itemsToDelete.isEmpty {
                try context.save()
                AppLogger.log(.info, "Removed \(itemsToDelete.count) redundant operations, remaining: \(allItems.count - itemsToDelete.count)", category: .sync)
            } else {
                AppLogger.log(.debug, "No redundant operations to remove", category: .sync)
            }
        } catch {
            AppLogger.log(.error, "Failed to deduplicate sync queue: \(error)", category: .sync)
        }
    }
    
    /// Create a unique key for an operation
    func createOperationKey(for item: SyncQueueItem) -> String {
        let entityId = getEntityId(from: item)?.uuidString ?? "none"
        return "\(item.target.rawValue)-\(item.operation.rawValue)-\(entityId)"
    }
    
    /// Get entity ID from sync queue item
    func getEntityId(from item: SyncQueueItem) -> UUID? {
        switch item.target {
        case .node: return item.nodeId
        case .edge: return item.edgeId
        case .photo: return item.photoId
        case .userTask: return item.userTaskId
        case .irPhoto: return item.irPhotoId
        case .irSession: return item.irSessionId
        case .issue: return item.issueId
        case .quote: return item.quoteId
        case .formInstance: return item.formInstanceId
        case .egFormInstance: return item.egFormInstanceId
        case .building: return item.buildingId
        case .floor: return item.floorId
        case .room: return item.roomId
        case .attachment: return item.attachmentId
        case .sldView: return item.sldViewId
        case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask, .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode, .mappingNodeSession, .mappingAttachmentNode, .mappingUserSession, .mappingTaskNodeBulkCompletion, .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
            if let mappingData = item.mappingData {
                return mappingData.issueId ?? mappingData.taskId ?? mappingData.quoteId ?? mappingData.sessionId ?? mappingData.formInstanceId ?? mappingData.nodeId ?? mappingData.attachmentId
            }
            return nil
        case .mappingNodeSLDView, .mappingEdgeSLDView:
            // Return the mapping ID for view mappings
            if let mappingData = item.mappingData {
                return mappingData.id ?? mappingData.nodeId ?? mappingData.edgeId
            }
            return nil
        }
    }
}