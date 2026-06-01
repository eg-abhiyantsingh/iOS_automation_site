import Foundation
import SwiftData

@MainActor
class TaskMappingService: ObservableObject {
    static let shared = TaskMappingService()

    private let apiClient = APIClient.shared
    private let networkState = NetworkState.shared
    private let syncQueueService = SyncQueueService(modelContext: nil)

    private init() {}

    // Set the model context (should be called from ContentView or App)
    func setModelContext(_ context: ModelContext) {
        syncQueueService.setModelContext(context)
    }

    // MARK: - Node Operations

    /// Link multiple nodes to a task (replaces all existing links)
    func setTaskNodes(taskId: UUID, nodeIds: [UUID]) async throws {
        if networkState.isOnline {
            _ = try await apiClient.batchUpdateTaskNodes(
                taskId: taskId,
                nodeIds: nodeIds,
                operation: "set"
            )
        } else {
            // Queue individual operations for each node
            // First, we'd need to clear existing nodes (not implemented in current sync system)
            // Then add new ones
            for nodeId in nodeIds {
                enqueueTaskNodeMapping(taskId: taskId, nodeId: nodeId, isDeleted: false)
            }
        }
    }

    /// Add nodes to existing task links
    func addTaskNodes(taskId: UUID, nodeIds: [UUID]) async throws {
        if networkState.isOnline {
            _ = try await apiClient.batchUpdateTaskNodes(
                taskId: taskId,
                nodeIds: nodeIds,
                operation: "add"
            )
        } else {
            // Queue individual operations for each node
            for nodeId in nodeIds {
                enqueueTaskNodeMapping(taskId: taskId, nodeId: nodeId, isDeleted: false)
            }
        }
    }

    /// Remove specific nodes from task links
    func removeTaskNodes(taskId: UUID, nodeIds: [UUID]) async throws {
        if networkState.isOnline {
            _ = try await apiClient.batchUpdateTaskNodes(
                taskId: taskId,
                nodeIds: nodeIds,
                operation: "remove"
            )
        } else {
            // Queue individual delete operations for each node
            for nodeId in nodeIds {
                enqueueTaskNodeMapping(taskId: taskId, nodeId: nodeId, isDeleted: true)
            }
        }
    }

    /// Get all nodes linked to a task
    func getTaskNodes(taskId: UUID) async throws -> [MappingTaskNodeDTO] {
        if networkState.isOnline {
            return try await apiClient.getTaskNodesV2(taskId: taskId)
        } else {
            // In offline mode, return empty array or cached data
            AppLogger.log(.notice, "getTaskNodes called while offline, returning empty array", category: .task)
            return []
        }
    }

    // MARK: - Form Operations

    /// Link multiple forms to a task (replaces all existing links)
    func setTaskForms(taskId: UUID, formIds: [UUID]) async throws {
        if networkState.isOnline {
            _ = try await apiClient.batchUpdateTaskForms(
                taskId: taskId,
                formIds: formIds,
                operation: "set"
            )
        } else {
            // Queue individual operations for each form
            for formId in formIds {
                enqueueTaskFormMapping(taskId: taskId, formId: formId, isDeleted: false)
            }
        }
    }

    /// Add forms to existing task links
    func addTaskForms(taskId: UUID, formIds: [UUID]) async throws {
        if networkState.isOnline {
            _ = try await apiClient.batchUpdateTaskForms(
                taskId: taskId,
                formIds: formIds,
                operation: "add"
            )
        } else {
            // Queue individual operations for each form
            for formId in formIds {
                enqueueTaskFormMapping(taskId: taskId, formId: formId, isDeleted: false)
            }
        }
    }

    /// Remove specific forms from task links
    func removeTaskForms(taskId: UUID, formIds: [UUID]) async throws {
        if networkState.isOnline {
            _ = try await apiClient.batchUpdateTaskForms(
                taskId: taskId,
                formIds: formIds,
                operation: "remove"
            )
        } else {
            // Queue individual delete operations for each form
            for formId in formIds {
                enqueueTaskFormMapping(taskId: taskId, formId: formId, isDeleted: true)
            }
        }
    }

    /// Get all forms linked to a task
    func getTaskForms(taskId: UUID) async throws -> [MappingTaskFormDTO] {
        if networkState.isOnline {
            return try await apiClient.getTaskFormsV2(taskId: taskId)
        } else {
            // In offline mode, return empty array or cached data
            AppLogger.log(.notice, "getTaskForms called while offline, returning empty array", category: .task)
            return []
        }
    }

    // MARK: - Node Completion Operations

    /// Update the completion state of a specific node within a task
    func updateNodeCompletion(taskId: UUID, nodeId: UUID, isCompleted: Bool) async throws {
        if networkState.isOnline {
            _ = try await apiClient.updateTaskNodeMapping(
                taskId: taskId,
                nodeId: nodeId,
                isDeleted: false,
                isCompleted: isCompleted
            )
        } else {
            enqueueTaskNodeMapping(taskId: taskId, nodeId: nodeId, isDeleted: false, isCompleted: isCompleted)
        }
    }

    /// Bulk update the completion state of multiple nodes within a task
    func bulkUpdateNodeCompletions(taskId: UUID, completions: [(nodeId: UUID, isCompleted: Bool)]) async throws {
        if networkState.isOnline {
            _ = try await apiClient.bulkUpdateTaskNodeCompletions(
                taskId: taskId,
                completions: completions
            )
        } else {
            // Queue bulk completion operation for sync
            let mappingData = MappingData.taskNodeBulkCompletion(taskId: taskId, completions: completions)
            let op = SyncOp(
                target: .mappingTaskNodeBulkCompletion,
                operation: .update,
                mappingData: mappingData
            )
            await networkState.enqueue(op)
            AppLogger.log(.debug, "Queued bulk task-node completion: Task \(taskId.uuidString.prefix(8)) with \(completions.count) node(s)", category: .task)
        }
    }

    // MARK: - Single Entity Operations (backward compatibility)

    /// Link a single node to a task
    func linkTaskToNode(taskId: UUID, nodeId: UUID) async throws {
        if networkState.isOnline {
            _ = try await apiClient.createTaskNodeMapping(taskId: taskId, nodeId: nodeId)
        } else {
            // Queue for sync when online
            enqueueTaskNodeMapping(taskId: taskId, nodeId: nodeId, isDeleted: false)
        }
    }

    /// Unlink a single node from a task
    func unlinkTaskFromNode(taskId: UUID, nodeId: UUID) async throws {
        if networkState.isOnline {
            _ = try await apiClient.updateTaskNodeMapping(
                taskId: taskId,
                nodeId: nodeId,
                isDeleted: true
            )
        } else {
            // Queue for sync when online
            enqueueTaskNodeMapping(taskId: taskId, nodeId: nodeId, isDeleted: true)
        }
    }

    /// Link a single form to a task
    func linkTaskToForm(taskId: UUID, formId: UUID) async throws {
        if networkState.isOnline {
            _ = try await apiClient.createTaskFormMapping(taskId: taskId, formId: formId)
        } else {
            // Queue for sync when online
            enqueueTaskFormMapping(taskId: taskId, formId: formId, isDeleted: false)
        }
    }

    /// Unlink a single form from a task
    func unlinkTaskFromForm(taskId: UUID, formId: UUID) async throws {
        if networkState.isOnline {
            _ = try await apiClient.updateTaskFormMapping(
                taskId: taskId,
                formId: formId,
                isDeleted: true
            )
        } else {
            // Queue for sync when online
            enqueueTaskFormMapping(taskId: taskId, formId: formId, isDeleted: true)
        }
    }

    // MARK: - Private Helper Methods

    private func enqueueTaskNodeMapping(taskId: UUID, nodeId: UUID, isDeleted: Bool, isCompleted: Bool? = nil) {
        // If we're trying to delete a mapping that might not exist in the database,
        // we should check if it was created locally first
        if isDeleted {
            // For now, we'll queue an update operation and handle 404 errors gracefully
            // TODO: Track which mappings exist in DB vs created locally
            AppLogger.log(.notice, "Queuing delete for task-node mapping that may not exist in DB", category: .task)
        }

        // Create mapping data for task-node
        let mappingData = MappingData.taskNode(taskId: taskId, nodeId: nodeId, isDeleted: isDeleted, isCompleted: isCompleted)

        let op = SyncOp(
            target: .mappingTaskNode,
            operation: isDeleted ? .update : .create,
            mappingData: mappingData
        )

        syncQueueService.enqueue(op)
        AppLogger.log(.debug, "Queued task-node mapping: Task \(taskId.uuidString.prefix(8)) - Node \(nodeId.uuidString.prefix(8)) [deleted: \(isDeleted)]", category: .task)
    }

    private func enqueueTaskFormMapping(taskId: UUID, formId: UUID, isDeleted: Bool) {
        // If we're trying to delete a mapping that might not exist in the database,
        // we should check if it was created locally first
        if isDeleted {
            // For now, we'll queue an update operation and handle 404 errors gracefully
            // TODO: Track which mappings exist in DB vs created locally
            AppLogger.log(.notice, "Queuing delete for task-form mapping that may not exist in DB", category: .task)
        }

        // Create mapping data for task-form
        let mappingData = MappingData.taskForm(taskId: taskId, formId: formId, isDeleted: isDeleted)

        let op = SyncOp(
            target: .mappingTaskForm,
            operation: isDeleted ? .update : .create,
            mappingData: mappingData
        )

        syncQueueService.enqueue(op)
        AppLogger.log(.debug, "Queued task-form mapping: Task \(taskId.uuidString.prefix(8)) - Form \(formId.uuidString.prefix(8)) [deleted: \(isDeleted)]", category: .task)
    }

    // MARK: - Form Instance Operations

    /// Create a new form instance from a form template
    func createFormInstance(formMasterId: UUID, modelContext: ModelContext) async throws -> FormInstance {
        let instance = FormInstance(
            id: UUID(),
            form_master_id: formMasterId,
            created_at: Date(),
            submitted: false,
            is_deleted: false
        )
        
        modelContext.insert(instance)
        try modelContext.save()
        
        if networkState.isOnline {
            // Create instance in API
            let dto = instance.toDTO()
            _ = try await apiClient.createFormInstance(dto)
        } else {
            // Queue for sync when online
            let op = SyncOp(target: .formInstance, operation: .create, formInstance: instance)
            await networkState.enqueue(op)
        }
        
        return instance
    }

    /// Link form instance to task
    func linkTaskToFormInstance(taskId: UUID, formInstanceId: UUID) async throws {
        if networkState.isOnline {
            _ = try await apiClient.createTaskFormInstanceMapping(
                taskId: taskId,
                formInstanceId: formInstanceId
            )
        } else {
            // Queue for sync when online
            enqueueTaskFormInstanceMapping(taskId: taskId, formInstanceId: formInstanceId, isDeleted: false)
        }
    }

    /// Unlink form instance from task
    func unlinkTaskFromFormInstance(taskId: UUID, formInstanceId: UUID) async throws {
        if networkState.isOnline {
            _ = try await apiClient.updateTaskFormInstanceMapping(
                taskId: taskId,
                formInstanceId: formInstanceId,
                isDeleted: true
            )
        } else {
            // Queue for sync when online
            enqueueTaskFormInstanceMapping(taskId: taskId, formInstanceId: formInstanceId, isDeleted: true)
        }
    }

    /// Create form instance and link to task in one operation
    func createFormInstanceAndLink(
        taskId: UUID,
        formMasterId: UUID,
        nodeIds: [UUID] = [],
        modelContext: ModelContext
    ) async throws -> FormInstance {
        // Create form instance locally
        let instance = FormInstance(
            id: UUID(),
            form_master_id: formMasterId,
            created_at: Date(),
            submitted: false,
            is_deleted: false
        )
        
        modelContext.insert(instance)
        
        // Find task and add to linkedFormInstances
        let descriptor = FetchDescriptor<UserTask>(
            predicate: #Predicate<UserTask> { task in
                task.id == taskId
            }
        )
        
        if let task = try? modelContext.fetch(descriptor).first {
            task.linkedFormInstances.append(instance)
        }
        
        try modelContext.save()
        
        if networkState.isOnline {
            // Create in API with linkage, passing client-generated UUID
            _ = try await apiClient.createFormInstanceAndLink(
                taskId: taskId,
                formMasterId: formMasterId,
                formInstanceId: instance.id,  // Pass client UUID
                nodeIds: nodeIds
            )
        } else {
            // Queue operations
            let op1 = SyncOp(target: .formInstance, operation: .create, formInstance: instance)
            await networkState.enqueue(op1)
            
            enqueueTaskFormInstanceMapping(taskId: taskId, formInstanceId: instance.id, isDeleted: false)
            
            // Queue node mappings if provided
            for nodeId in nodeIds {
                enqueueFormInstanceNodeMapping(formInstanceId: instance.id, nodeId: nodeId, isDeleted: false)
            }
        }
        
        return instance
    }

    /// Batch update task-form instance mappings
    func setTaskFormInstances(taskId: UUID, formInstanceIds: [UUID]) async throws {
        if networkState.isOnline {
            _ = try await apiClient.batchUpdateTaskFormInstances(
                taskId: taskId,
                formInstanceIds: formInstanceIds,
                operation: "set"
            )
        } else {
            // Queue operations - would need to track existing mappings for proper offline handling
            for instanceId in formInstanceIds {
                enqueueTaskFormInstanceMapping(taskId: taskId, formInstanceId: instanceId, isDeleted: false)
            }
        }
    }

    /// Get all form instances linked to a task
    func getTaskFormInstances(taskId: UUID) async throws -> [MappingTaskFormInstanceDTO] {
        if networkState.isOnline {
            return try await apiClient.getTaskFormInstances(taskId: taskId)
        } else {
            AppLogger.log(.notice, "getTaskFormInstances called while offline, returning empty array", category: .task)
            return []
        }
    }

    /// Remove specific form instances from task links
    func removeTaskFormInstances(taskId: UUID, formInstanceIds: [UUID]) async throws {
        if networkState.isOnline {
            _ = try await apiClient.batchUpdateTaskFormInstances(
                taskId: taskId,
                formInstanceIds: formInstanceIds,
                operation: "remove"
            )
        } else {
            // Queue individual soft delete operations for each form instance
            for instanceId in formInstanceIds {
                enqueueTaskFormInstanceMapping(taskId: taskId, formInstanceId: instanceId, isDeleted: true)
            }
        }
    }

    // MARK: - FormInstance-Node Operations

    /// Update FormInstance-Node mappings (replaces all existing)
    func updateFormInstanceNodes(formInstanceId: UUID, nodeIds: [UUID]) async throws {
        if networkState.isOnline {
            // Get current mappings to determine what to add/remove
            let currentMappings = try await apiClient.getFormInstanceNodes(formInstanceId: formInstanceId)
            let currentNodeIds = Set(currentMappings.compactMap { UUID(uuidString: $0.node_id) })
            let newNodeIds = Set(nodeIds)

            // Find nodes to add and remove
            let nodesToAdd = newNodeIds.subtracting(currentNodeIds)
            let nodesToRemove = currentNodeIds.subtracting(newNodeIds)

            // Add new mappings
            for nodeId in nodesToAdd {
                _ = try await apiClient.createFormInstanceNodeMapping(
                    formInstanceId: formInstanceId,
                    nodeId: nodeId
                )
            }

            // Remove old mappings (soft delete)
            for nodeId in nodesToRemove {
                _ = try await apiClient.updateFormInstanceNodeMapping(
                    formInstanceId: formInstanceId,
                    nodeId: nodeId,
                    isDeleted: true
                )
            }
        } else {
            // Queue operations for offline sync
            // Note: In offline mode, we queue all as creates/updates
            // The sync process will handle deduplication
            for nodeId in nodeIds {
                enqueueFormInstanceNodeMapping(
                    formInstanceId: formInstanceId,
                    nodeId: nodeId,
                    isDeleted: false
                )
            }
        }
    }

    /// Add nodes to a form instance
    func addFormInstanceNodes(formInstanceId: UUID, nodeIds: [UUID]) async throws {
        if networkState.isOnline {
            for nodeId in nodeIds {
                _ = try await apiClient.createFormInstanceNodeMapping(
                    formInstanceId: formInstanceId,
                    nodeId: nodeId
                )
            }
        } else {
            // Queue for offline sync
            for nodeId in nodeIds {
                enqueueFormInstanceNodeMapping(
                    formInstanceId: formInstanceId,
                    nodeId: nodeId,
                    isDeleted: false
                )
            }
        }
    }

    /// Remove nodes from a form instance
    func removeFormInstanceNodes(formInstanceId: UUID, nodeIds: [UUID]) async throws {
        if networkState.isOnline {
            for nodeId in nodeIds {
                _ = try await apiClient.updateFormInstanceNodeMapping(
                    formInstanceId: formInstanceId,
                    nodeId: nodeId,
                    isDeleted: true
                )
            }
        } else {
            // Queue for offline sync
            for nodeId in nodeIds {
                enqueueFormInstanceNodeMapping(
                    formInstanceId: formInstanceId,
                    nodeId: nodeId,
                    isDeleted: true
                )
            }
        }
    }

    // MARK: - Private Helper Methods for Form Instances

    private func enqueueTaskFormInstanceMapping(taskId: UUID, formInstanceId: UUID, isDeleted: Bool) {
        let mappingData = MappingData.taskFormInstance(
            taskId: taskId,
            formInstanceId: formInstanceId,
            isDeleted: isDeleted
        )
        
        let op = SyncOp(
            target: .mappingTaskFormInstance,
            operation: isDeleted ? .update : .create,
            mappingData: mappingData
        )
        
        Task {
            await networkState.enqueue(op)
        }
    }

    private func enqueueFormInstanceNodeMapping(formInstanceId: UUID, nodeId: UUID, isDeleted: Bool) {
        let mappingData = MappingData.formInstanceNode(
            formInstanceId: formInstanceId,
            nodeId: nodeId,
            isDeleted: isDeleted
        )
        
        let op = SyncOp(
            target: .mappingFormInstanceNode,
            operation: isDeleted ? .update : .create,
            mappingData: mappingData
        )
        
        Task {
            await networkState.enqueue(op)
        }
    }
}
