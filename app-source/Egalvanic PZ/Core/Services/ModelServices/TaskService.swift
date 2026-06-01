import Foundation
import SwiftData

// MARK: - Task Update Data
struct TaskUpdateData {
    let title: String
    let description: String
    let isCompleted: Bool
    let wasCompleted: Bool
    let stagedPhotoAdditions: [Photo]
    let stagedPhotoDeletions: Set<UUID>
    let originalPhotos: [Photo]
    /// nil means "leave existing value untouched" so callers that don't
    /// surface these fields don't accidentally clobber them.
    var taskType: String? = nil
    var procedureId: UUID? = nil
    /// Distinguishes "clear procedure" from "don't touch it"; only when
    /// true does the comprehensive update write the new procedureId.
    var procedureIdSet: Bool = false
}

class TaskService {
    
    @MainActor
    static func createTask(
        title: String,
        description: String,
        form: UserTaskForm? = nil,
        node: NodeV2? = nil,
        nodes: [NodeV2] = [], // Support multiple nodes
        forms: [UserTaskForm] = [], // Support multiple forms
        formSelections: [(form: UserTaskForm, nodes: [NodeV2])] = [], // Form instances with their linked nodes
        sld: SLDV2,
        dueDate: Date? = nil,
        taskType: String? = nil,
        procedureId: UUID? = nil,
        modelContext: ModelContext,
        activeSession: IRSession? = nil,
        onCompletion: ((Bool, UserTask?, String?) -> Void)? = nil
    ) -> UserTask? {
        // Combine single node/form with arrays for backward compatibility
        var allNodes = nodes
        if let singleNode = node, !allNodes.contains(where: { $0.id == singleNode.id }) {
            allNodes.append(singleNode)
        }
        
        // Allow duplicate forms - each will create its own form instance
        var allForms = forms
        if let singleForm = form {
            // Don't check for duplicates - allow multiple instances of the same form
            allForms.append(singleForm)
        }
        
        // Create task WITHOUT direct node/form assignment
        // Legacy fields will be set for backward compatibility but array relationships are primary
        let newTask = UserTask(
            id: UUID(),
            title: title,
            task_description: description,
            completed: false,
            form: allForms.first, // Set first form as legacy field for backward compatibility
            node: allNodes.first, // Set first node as legacy field for backward compatibility
            sld: sld,
            is_deleted: false,
            submission: nil,
            submitted_at: nil,
            photos: [],
            due_date: dueDate,
            created_at: Date(),
            task_type: taskType,
            procedure_id: procedureId,
            owned_by: [AppStateManager.shared.userId],
            assigned_to: [AppStateManager.shared.userId]
        )
        
        modelContext.insert(newTask)
        
        // Set up bidirectional relationships for nodes
        for node in allNodes {
            // Add to task's linkedNodes array
            if !newTask.linkedNodes.contains(where: { $0.id == node.id }) {
                newTask.linkedNodes.append(node)
            }
            // Add to node's node_tasks array (bidirectional)
            if !node.node_tasks.contains(where: { $0.id == newTask.id }) {
                node.node_tasks.append(newTask)
            }
        }
        
        // Don't populate linkedForms anymore - we use linkedFormInstances now
        // Forms will be linked via FormInstance objects created later
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Created task locally: \(newTask.title)", category: .task)
            
            if NetworkState.shared.canDirectSync {
                Task {
                    var taskCreated = false
                    
                    do {
                        // Create the task on server
                        _ = try await APIClient.shared.createTask(task: newTask)
                        taskCreated = true
                        AppLogger.log(.info, "Task created on server", category: .task)
                        
                        // Create owner mapping
                        do {
                            _ = try await APIClient.shared.createUserTaskMapping(
                                userId: AppStateManager.shared.userId,
                                taskId: newTask.id,
                                mappingType: "owner"
                            )
                            AppLogger.log(.info, "Owner mapping created", category: .task)
                        } catch {
                            AppLogger.log(.error, "Failed to create owner mapping: \(error)", category: .task)
                            await MainActor.run {
                                NetworkState.shared.enqueueUserTaskMapping(
                                    userId: AppStateManager.shared.userId,
                                    taskId: newTask.id,
                                    mappingType: "owner",
                                    isDeleted: false
                                )
                            }
                        }
                        
                        // Create assignee mapping
                        do {
                            _ = try await APIClient.shared.createUserTaskMapping(
                                userId: AppStateManager.shared.userId,
                                taskId: newTask.id,
                                mappingType: "assignee"
                            )
                            AppLogger.log(.info, "Assignee mapping created", category: .task)
                        } catch {
                            AppLogger.log(.error, "Failed to create assignee mapping: \(error)", category: .task)
                            await MainActor.run {
                                NetworkState.shared.enqueueUserTaskMapping(
                                    userId: AppStateManager.shared.userId,
                                    taskId: newTask.id,
                                    mappingType: "assignee",
                                    isDeleted: false
                                )
                            }
                        }
                        
                        // Create task-session mapping if active session exists
                        if let session = activeSession ?? AppStateManager.shared.activeSession {
                            // Add task to session's local array
                            await MainActor.run {
                                session.user_tasks.append(newTask)
                                try? modelContext.save()
                            }
                            
                            do {
                                _ = try await APIClient.shared.createTaskSessionMapping(
                                    taskId: newTask.id,
                                    sessionId: session.id
                                )
                                AppLogger.log(.info, "Task-session mapping created", category: .task)
                            } catch {
                                AppLogger.log(.error, "Failed to create task-session mapping: \(error)", category: .task)
                                await MainActor.run {
                                    NetworkState.shared.enqueueTaskSessionMapping(
                                        taskId: newTask.id,
                                        sessionId: session.id,
                                        isDeleted: false
                                    )
                                }
                            }
                        }
                        
                        // Collect all nodes that need to be linked to the task
                        // This includes both directly selected nodes and nodes from form selections
                        var allNodesToLink = Set(allNodes) // Use Set to avoid duplicates
                        for selection in formSelections {
                            allNodesToLink.formUnion(selection.nodes)
                        }
                        let uniqueNodes = Array(allNodesToLink)
                        
                        // Create task-node mappings for all linked nodes
                        if !uniqueNodes.isEmpty {
                            do {
                                try await TaskMappingService.shared.addTaskNodes(
                                    taskId: newTask.id,
                                    nodeIds: uniqueNodes.map { $0.id }
                                )
                                AppLogger.log(.info, "Created \(uniqueNodes.count) task-node mappings", category: .task)
                            } catch {
                                AppLogger.log(.error, "Failed to create task-node mappings: \(error)", category: .task)
                                // Queue for offline sync
                                for node in uniqueNodes {
                                    await MainActor.run {
                                        let op = SyncOp(
                                            target: .mappingTaskNode,
                                            operation: .create,
                                            mappingData: MappingData(
                                                issueId: nil,
                                                taskId: newTask.id,
                                                sessionId: nil,
                                                quoteId: nil,
                                                userId: nil,
                                                nodeId: node.id,
                                                formId: nil,
                                                formInstanceId: nil,
                                                mappingType: "task_node",
                                                isDeleted: false
                                            )
                                        )
                                        NetworkState.shared.enqueue(op)
                                    }
                                }
                            }
                        }
                        
                        // Create form instances with their linked nodes
                        // Use formSelections if available, otherwise fall back to allForms
                        let formInstancesData: [(form: UserTaskForm, nodeIds: [UUID])]
                        if !formSelections.isEmpty {
                            // Use the new formSelections with node mappings
                            formInstancesData = formSelections.map { selection in
                                (form: selection.form, nodeIds: selection.nodes.map { $0.id })
                            }
                        } else if !allForms.isEmpty {
                            // Fall back to simple forms without node mappings
                            formInstancesData = allForms.map { form in
                                (form: form, nodeIds: [])
                            }
                        } else {
                            formInstancesData = []
                        }
                        
                        for (form, nodeIds) in formInstancesData {
                            // Create local FormInstance FIRST with client-generated UUID
                            let formInstance = await MainActor.run { () -> FormInstance in
                                let instance = FormInstance(
                                    id: UUID(), // Client generates UUID
                                    form_master_id: form.id,
                                    created_at: Date(),
                                    modified_at: nil,
                                    form_submission: nil,
                                    submitted: false,
                                    is_deleted: false,
                                    formMaster: form
                                )
                                modelContext.insert(instance)
                                
                                // Add to task's linkedFormInstances
                                newTask.linkedFormInstances.append(instance)
                                
                                try? modelContext.save()
                                return instance
                            }
                            
                            do {
                                // Create form instance on server WITH client's UUID
                                let formInstanceDTO = try await APIClient.shared.createFormInstanceAndLink(
                                    taskId: newTask.id,
                                    formMasterId: form.id,
                                    formInstanceId: formInstance.id, // Pass client UUID!
                                    nodeIds: nodeIds
                                )
                                AppLogger.log(.info, "Created form instance and linked to task: \(formInstanceDTO.id)", category: .task)
                            } catch {
                                AppLogger.log(.error, "Failed to create form instance for form \(form.id): \(error)", category: .task)
                                // Queue for offline sync - formInstance already created above
                                await MainActor.run {
                                    // Queue form instance creation
                                    let formInstanceOp = SyncOp(
                                        target: .formInstance,
                                        operation: .create,
                                        formInstance: formInstance
                                    )
                                    NetworkState.shared.enqueue(formInstanceOp)
                                    
                                    // Queue task-form-instance mapping
                                    let mappingOp = SyncOp(
                                        target: .mappingTaskFormInstance,
                                        operation: .create,
                                        mappingData: MappingData.taskFormInstance(
                                            taskId: newTask.id,
                                            formInstanceId: formInstance.id,
                                            isDeleted: false
                                        )
                                    )
                                    NetworkState.shared.enqueue(mappingOp)
                                    
                                    // Queue form instance-node mappings if there are linked nodes
                                    for nodeId in nodeIds {
                                        let nodeMapping = SyncOp(
                                            target: .mappingFormInstanceNode,
                                            operation: .create,
                                            mappingData: MappingData.formInstanceNode(
                                                formInstanceId: formInstance.id,
                                                nodeId: nodeId,
                                                isDeleted: false
                                            )
                                        )
                                        NetworkState.shared.enqueue(nodeMapping)
                                    }
                                }
                            }
                        }
                        AppLogger.log(.info, "Created \(formInstancesData.count) form instances with mappings", category: .task)
                        
                        await MainActor.run {
                            onCompletion?(true, newTask, nil)
                        }
                        
                    } catch {
                        // Only queue task if task creation itself failed
                        if !taskCreated {
                            AppLogger.log(.error, "Failed to create task on server: \(error)", category: .task)
                            await MainActor.run {
                                // Queue task creation
                                let op = SyncOp(
                                    target: .userTask,
                                    operation: .create,
                                    userTask: newTask
                                )
                                NetworkState.shared.enqueue(op)
                                
                                // Queue all mappings
                                NetworkState.shared.enqueueUserTaskMapping(
                                    userId: AppStateManager.shared.userId,
                                    taskId: newTask.id,
                                    mappingType: "owner",
                                    isDeleted: false
                                )
                                
                                NetworkState.shared.enqueueUserTaskMapping(
                                    userId: AppStateManager.shared.userId,
                                    taskId: newTask.id,
                                    mappingType: "assignee",
                                    isDeleted: false
                                )
                                
                                if let session = activeSession ?? AppStateManager.shared.activeSession {
                                    // Add task to session's local array
                                    session.user_tasks.append(newTask)
                                    try? modelContext.save()
                                    
                                    NetworkState.shared.enqueueTaskSessionMapping(
                                        taskId: newTask.id,
                                        sessionId: session.id,
                                        isDeleted: false
                                    )
                                }
                                
                                // Queue task-node mappings
                                for node in allNodes {
                                    let op = SyncOp(
                                        target: .mappingTaskNode,
                                        operation: .create,
                                        mappingData: MappingData(
                                            issueId: nil,
                                            taskId: newTask.id,
                                            sessionId: nil,
                                            quoteId: nil,
                                            userId: nil,
                                            nodeId: node.id,
                                            formId: nil,
                                            formInstanceId: nil,
                                            mappingType: "task_node",
                                            isDeleted: false
                                        )
                                    )
                                    NetworkState.shared.enqueue(op)
                                }
                                
                                // Queue form instance creation and mappings
                                for form in allForms {
                                    // Create local form instance
                                    let formInstance = FormInstance(
                                        id: UUID(),
                                        form_master_id: form.id,
                                        created_at: Date(),
                                        modified_at: nil,
                                        form_submission: nil,
                                        submitted: false,
                                        is_deleted: false,
                                        formMaster: form
                                    )
                                    modelContext.insert(formInstance)
                                    
                                    // Add to task's linkedFormInstances
                                    newTask.linkedFormInstances.append(formInstance)
                                    
                                    // Queue form instance creation
                                    let formInstanceOp = SyncOp(
                                        target: .formInstance,
                                        operation: .create,
                                        formInstance: formInstance
                                    )
                                    NetworkState.shared.enqueue(formInstanceOp)
                                    
                                    // Queue task-form-instance mapping
                                    let mappingOp = SyncOp(
                                        target: .mappingTaskFormInstance,
                                        operation: .create,
                                        mappingData: MappingData.taskFormInstance(
                                            taskId: newTask.id,
                                            formInstanceId: formInstance.id,
                                            isDeleted: false
                                        )
                                    )
                                    NetworkState.shared.enqueue(mappingOp)
                                    
                                    // Don't automatically link form instances to nodes
                                }
                                
                                try? modelContext.save()
                                
                                onCompletion?(true, newTask, "Task created locally. Will sync when online.")
                            }
                        } else {
                            // Task was created but some mappings failed
                            await MainActor.run {
                                onCompletion?(true, newTask, "Task created. Some assignments will sync later.")
                            }
                        }
                    }
                } // End of Task
            } else {
                // Offline mode - queue everything
                let op = SyncOp(
                    target: .userTask,
                    operation: .create,
                    userTask: newTask
                )
                NetworkState.shared.enqueue(op)
                
                // Queue owner mapping
                NetworkState.shared.enqueueUserTaskMapping(
                    userId: AppStateManager.shared.userId,
                    taskId: newTask.id,
                    mappingType: "owner",
                    isDeleted: false
                )
                
                // Queue assignee mapping
                NetworkState.shared.enqueueUserTaskMapping(
                    userId: AppStateManager.shared.userId,
                    taskId: newTask.id,
                    mappingType: "assignee",
                    isDeleted: false
                )
                
                // Queue session mapping if applicable
                if let session = activeSession ?? AppStateManager.shared.activeSession {
                    // Add task to session's local array
                    session.user_tasks.append(newTask)
                    try? modelContext.save()
                    
                    NetworkState.shared.enqueueTaskSessionMapping(
                        taskId: newTask.id,
                        sessionId: session.id,
                        isDeleted: false
                    )
                }
                
                // Collect all nodes that need to be linked to the task
                // This includes both directly selected nodes and nodes from form selections
                var allNodesToLink = Set(allNodes) // Use Set to avoid duplicates
                for selection in formSelections {
                    allNodesToLink.formUnion(selection.nodes)
                }
                
                // Queue task-node mappings for all unique nodes
                for node in allNodesToLink {
                    let op = SyncOp(
                        target: .mappingTaskNode,
                        operation: .create,
                        mappingData: MappingData(
                            issueId: nil,
                            taskId: newTask.id,
                            sessionId: nil,
                            quoteId: nil,
                            userId: nil,
                            nodeId: node.id,
                            formId: nil,
                            formInstanceId: nil,
                            mappingType: "task_node",
                            isDeleted: false
                        )
                    )
                    NetworkState.shared.enqueue(op)
                }
                
                // Queue form instance creation and mappings
                // Use formSelections if available, otherwise fall back to allForms
                if !formSelections.isEmpty {
                    // Use the new formSelections with node mappings
                    for selection in formSelections {
                        // Create local form instance
                        let formInstance = FormInstance(
                            id: UUID(),
                            form_master_id: selection.form.id,
                            created_at: Date(),
                            modified_at: nil,
                            form_submission: nil,
                            submitted: false,
                            is_deleted: false,
                            formMaster: selection.form
                        )
                        modelContext.insert(formInstance)
                        
                        // Add to task's linkedFormInstances
                        newTask.linkedFormInstances.append(formInstance)
                        
                        // Link nodes to form instance locally
                        for node in selection.nodes {
                            if !formInstance.linkedNodes.contains(where: { $0.id == node.id }) {
                                formInstance.linkedNodes.append(node)
                            }
                        }
                        
                        // Queue form instance creation
                        let formInstanceOp = SyncOp(
                            target: .formInstance,
                            operation: .create,
                            formInstance: formInstance
                        )
                        NetworkState.shared.enqueue(formInstanceOp)
                        
                        // Queue task-form-instance mapping
                        let mappingOp = SyncOp(
                            target: .mappingTaskFormInstance,
                            operation: .create,
                            mappingData: MappingData.taskFormInstance(
                                taskId: newTask.id,
                                formInstanceId: formInstance.id,
                                isDeleted: false
                            )
                        )
                        NetworkState.shared.enqueue(mappingOp)
                        
                        // Queue form instance-node mappings
                        for node in selection.nodes {
                            let nodeMapping = SyncOp(
                                target: .mappingFormInstanceNode,
                                operation: .create,
                                mappingData: MappingData.formInstanceNode(
                                    formInstanceId: formInstance.id,
                                    nodeId: node.id,
                                    isDeleted: false
                                )
                            )
                            NetworkState.shared.enqueue(nodeMapping)
                        }
                    }
                } else {
                    // Fall back to simple forms without node mappings
                    for form in allForms {
                        // Create local form instance
                        let formInstance = FormInstance(
                            id: UUID(),
                            form_master_id: form.id,
                            created_at: Date(),
                            modified_at: nil,
                            form_submission: nil,
                            submitted: false,
                            is_deleted: false,
                            formMaster: form
                        )
                        modelContext.insert(formInstance)
                        
                        // Add to task's linkedFormInstances
                        newTask.linkedFormInstances.append(formInstance)
                        
                        // Queue form instance creation
                        let formInstanceOp = SyncOp(
                            target: .formInstance,
                            operation: .create,
                            formInstance: formInstance
                        )
                        NetworkState.shared.enqueue(formInstanceOp)
                        
                        // Queue task-form-instance mapping
                        let mappingOp = SyncOp(
                            target: .mappingTaskFormInstance,
                            operation: .create,
                            mappingData: MappingData.taskFormInstance(
                                taskId: newTask.id,
                                formInstanceId: formInstance.id,
                                isDeleted: false
                            )
                        )
                        NetworkState.shared.enqueue(mappingOp)
                    }
                }
                
                try? modelContext.save()
                
                AppLogger.log(.info, "Task and mappings queued for sync when online", category: .task)
                onCompletion?(true, newTask, "Task created locally. Will sync when online.")
            }
            return newTask
        } catch {
            AppLogger.log(.error, "Failed to save task: \(error)", category: .task)
            onCompletion?(false, nil, "Failed to create task: \(error.localizedDescription)")
            return nil
        }
    }
    
    @MainActor
    static func updateTask(
        task: UserTask,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        do {
            try modelContext.save()
            AppLogger.log(.info, "Updated task", category: .task)
            
            // Handle server sync
            if NetworkState.shared.canDirectSync {
                // Sync updates to server asynchronously
                Task {
                    do {
                        _ = try await APIClient.shared.updateTask(task)
                        AppLogger.log(.info, "Task \(task.id) update synced to server", category: .task)
                        
                        await MainActor.run {
                            try? modelContext.save()
                            onCompletion?(true, nil)
                        }
                    } catch {
                        AppLogger.log(.error, "Failed to sync task update to server: \(error)", category: .task)
                        
                        await MainActor.run {
                            let op = SyncOp(
                                target: .userTask,
                                operation: .update,
                                userTask: task
                            )
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, "Task updated locally. Will sync when online.")
                        }
                    }
                }
            } else {
                // Offline mode - queue for sync
                let op = SyncOp(
                    target: .userTask,
                    operation: .update,
                    userTask: task
                )
                NetworkState.shared.enqueue(op)
                AppLogger.log(.info, "Update of task \(task.id) queued for sync when online", category: .task)
                onCompletion?(true, "Task updated locally. Will sync when online.")
            }
            
        } catch {
            AppLogger.log(.error, "Failed to save updates: \(error)", category: .task)
            onCompletion?(false, "Failed to update task: \(error.localizedDescription)")
        }
    }
    
    // MARK: - Comprehensive Update
    @MainActor
    static func updateTaskComprehensive(
        _ task: UserTask,
        with updates: TaskUpdateData,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws {
        AppLogger.log(.info, "=== STARTING COMPREHENSIVE TASK UPDATE ===", category: .task)
        AppLogger.log(.info, "Task Title: \(updates.title)", category: .task)
        AppLogger.log(.info, "Completed: \(updates.isCompleted)", category: .task)
        AppLogger.log(.info, "Staged Photo Additions: \(updates.stagedPhotoAdditions.count)", category: .task)
        AppLogger.log(.info, "Staged Photo Deletions: \(updates.stagedPhotoDeletions.count)", category: .task)
        AppLogger.log(.info, "Network Mode: \(networkState.mode)", category: .task)
        
        // Step 1: Update basic task properties
        AppLogger.log(.debug, "Applying changes to task...", category: .task)
        task.title = updates.title
        task.task_description = updates.description
        task.completed = updates.isCompleted
        if let newType = updates.taskType {
            task.task_type = newType
        }
        if updates.procedureIdSet {
            task.procedure_id = updates.procedureId
        }
        
        // Set submitted_at when task is marked complete, clear it when unmarked
        if updates.isCompleted && !updates.wasCompleted {
            task.submitted_at = Date()
            AppLogger.log(.info, "Task marked as completed with timestamp", category: .task)
        } else if !updates.isCompleted && updates.wasCompleted {
            task.submitted_at = nil
            AppLogger.log(.info, "Task marked as incomplete, cleared timestamp", category: .task)
        }
        
        // Auto-complete node mappings when task is resolved
        let activeNodes = task.linkedNodes.filter { !$0.is_deleted }
        if !activeNodes.isEmpty {
            if updates.isCompleted && !updates.wasCompleted {
                // Mark all linked nodes as completed
                for node in activeNodes {
                    task.nodeCompletions[node.id.uuidString] = true
                }
                AppLogger.log(.info, "Auto-marked \(activeNodes.count) node mapping(s) as completed", category: .task)
            } else if !updates.isCompleted && updates.wasCompleted {
                // Clear completion for all linked nodes
                for node in activeNodes {
                    task.nodeCompletions[node.id.uuidString] = false
                }
                AppLogger.log(.info, "Cleared \(activeNodes.count) node mapping completion(s)", category: .task)
            }
        }
        
        AppLogger.log(.info, "Task properties updated", category: .task)
        
        // Step 2: Process photo changes using PhotoService
        AppLogger.log(.debug, "Processing photo changes...", category: .task)
        PhotoService.processPhotoChanges(
            stagedAdditions: updates.stagedPhotoAdditions,
            stagedDeletions: updates.stagedPhotoDeletions,
            originalPhotos: updates.originalPhotos,
            entity: task,
            modelContext: modelContext,
            networkState: networkState
        )
        AppLogger.log(.info, "Photo changes processed via PhotoService", category: .task)
        
        // Step 3: Save the context
        AppLogger.log(.debug, "Attempting to save context...", category: .task)
        try modelContext.save()
        AppLogger.log(.info, "Context saved successfully", category: .task)
        
        // Step 4: Handle network sync
        if networkState.canDirectSync {
            AppLogger.log(.info, "Online mode - starting async sync operations...", category: .task)
            
            do {
                // Determine if completion status changed
                let completionChanged = updates.isCompleted != updates.wasCompleted
                
                // If completion status changed AND task has linked nodes, use bulk completion API
                // Otherwise, use task update API
                if completionChanged && !activeNodes.isEmpty {
                    AppLogger.log(.info, "Syncing task completion via bulk completion API...", category: .task)
                    let completions = activeNodes.map { (nodeId: $0.id, isCompleted: updates.isCompleted) }
                    try await TaskMappingService.shared.bulkUpdateNodeCompletions(
                        taskId: task.id, completions: completions
                    )
                    AppLogger.log(.info, "Synced \(activeNodes.count) node completion(s) to server", category: .task)
                } else {
                    // Update task on server
                    AppLogger.log(.info, "Updating task on server...", category: .task)
                    _ = try await APIClient.shared.updateTask(task)
                    AppLogger.log(.info, "Task updated on server successfully", category: .task)
                }
                
                // Upload photos and handle deletions
                if !updates.stagedPhotoAdditions.isEmpty || !updates.stagedPhotoDeletions.isEmpty {
                    AppLogger.log(.info, "Processing \(updates.stagedPhotoAdditions.count) new photos and \(updates.stagedPhotoDeletions.count) deletions...", category: .task)
                    await PhotoService.uploadPendingPhotos(
                        for: task,
                        stagedDeletions: updates.stagedPhotoDeletions,
                        displayedPhotos: updates.originalPhotos,
                        modelContext: modelContext,
                        networkState: networkState
                    )
                    AppLogger.log(.info, "Photo operations completed", category: .task)
                }
                
                AppLogger.log(.info, "All sync operations completed successfully", category: .task)
                
            } catch {
                AppLogger.log(.error, "Online save failed: \(error)", category: .task)
                AppLogger.log(.info, "Error details: \(error.localizedDescription)", category: .task)
                
                // Queue for sync if online update fails
                AppLogger.log(.info, "Queueing task for later sync...", category: .task)
                let op = SyncOp(
                    target: .userTask,
                    operation: .update,
                    userTask: task
                )
                networkState.enqueue(op)
                
                // Photo operations are already queued by PhotoService.processPhotoChanges
                throw error
            }
        } else {
            AppLogger.log(.info, "Offline mode - queueing all operations for sync...", category: .task)
            
            // Determine if completion status changed
            let completionChanged = updates.isCompleted != updates.wasCompleted
            
            // If completion status changed AND task has linked nodes, queue bulk completion
            // Otherwise, queue task update
            if completionChanged && !activeNodes.isEmpty {
                let completions = activeNodes.map { (nodeId: $0.id, isCompleted: updates.isCompleted) }
                try? await TaskMappingService.shared.bulkUpdateNodeCompletions(
                    taskId: task.id, completions: completions
                )
                AppLogger.log(.info, "Queued \(activeNodes.count) node completion(s) for sync", category: .task)
            } else {
                // Queue task for sync
                let op = SyncOp(
                    target: .userTask,
                    operation: .update,
                    userTask: task
                )
                networkState.enqueue(op)
                AppLogger.log(.info, "Task queued for sync", category: .task)
            }
            
            // Photo operations are already queued by PhotoService.processPhotoChanges
            AppLogger.log(.info, "Photo operations already queued by PhotoService", category: .task)
            
            AppLogger.log(.info, "Offline save completed", category: .task)
        }
    }
    
    @MainActor
    static func deleteTask(
        _ task: UserTask,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        task.is_deleted = true
        
        // Collect all mapping IDs that need to be soft-deleted
        let nodeIds = task.linkedNodes.filter { !$0.is_deleted }.map { $0.id }
        let formIds = task.linkedForms.filter { !$0.is_deleted }.map { $0.id }
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Task marked as deleted locally", category: .task)
            
            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.updateTask(task)
                        AppLogger.log(.info, "Task deletion synced to server", category: .task)
                        
                        // Soft-delete task-node mappings
                        if !nodeIds.isEmpty {
                            do {
                                try await TaskMappingService.shared.removeTaskNodes(
                                    taskId: task.id,
                                    nodeIds: nodeIds
                                )
                                AppLogger.log(.info, "Soft-deleted \(nodeIds.count) task-node mappings", category: .task)
                            } catch {
                                AppLogger.log(.error, "Failed to soft-delete task-node mappings: \(error)", category: .task)
                                // Queue for offline sync
                                for nodeId in nodeIds {
                                    await MainActor.run {
                                        let op = SyncOp(
                                            target: .mappingTaskNode,
                                            operation: .update,
                                            mappingData: MappingData(
                                                issueId: nil,
                                                taskId: task.id,
                                                sessionId: nil,
                                                quoteId: nil,
                                                userId: nil,
                                                nodeId: nodeId,
                                                formId: nil,
                                                formInstanceId: nil,
                                                mappingType: "task_node",
                                                isDeleted: true
                                            )
                                        )
                                        NetworkState.shared.enqueue(op)
                                    }
                                }
                            }
                        }
                        
                        // Soft-delete task-form mappings
                        if !formIds.isEmpty {
                            do {
                                try await TaskMappingService.shared.removeTaskForms(
                                    taskId: task.id,
                                    formIds: formIds
                                )
                                AppLogger.log(.info, "Soft-deleted \(formIds.count) task-form mappings", category: .task)
                            } catch {
                                AppLogger.log(.error, "Failed to soft-delete task-form mappings: \(error)", category: .task)
                                // Queue for offline sync
                                for formId in formIds {
                                    await MainActor.run {
                                        let op = SyncOp(
                                            target: .mappingTaskForm,
                                            operation: .update,
                                            mappingData: MappingData(
                                                issueId: nil,
                                                taskId: task.id,
                                                sessionId: nil,
                                                quoteId: nil,
                                                userId: nil,
                                                nodeId: nil,
                                                formId: formId,
                                                formInstanceId: nil,
                                                mappingType: "task_form",
                                                isDeleted: true
                                            )
                                        )
                                        NetworkState.shared.enqueue(op)
                                    }
                                }
                            }
                        }
                        
                        await MainActor.run {
                            onCompletion?(true, nil)
                        }
                    } catch {
                        AppLogger.log(.error, "Failed to sync task deletion: \(error)", category: .task)
                        let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                        await MainActor.run {
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, "Task deleted locally, will sync when online")
                        }
                    }
                }
            } else {
                // Queue task deletion
                let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                NetworkState.shared.enqueue(op)
                
                // Queue soft-deletion of task-node mappings
                for nodeId in nodeIds {
                    let mappingOp = SyncOp(
                        target: .mappingTaskNode,
                        operation: .update,
                        mappingData: MappingData(
                            issueId: nil,
                            taskId: task.id,
                            sessionId: nil,
                            quoteId: nil,
                            userId: nil,
                            nodeId: nodeId,
                            formId: nil,
                            formInstanceId: nil,
                            mappingType: "task_node",
                            isDeleted: true
                        )
                    )
                    NetworkState.shared.enqueue(mappingOp)
                }
                
                // Queue soft-deletion of task-form mappings
                for formId in formIds {
                    let mappingOp = SyncOp(
                        target: .mappingTaskForm,
                        operation: .update,
                        mappingData: MappingData(
                            issueId: nil,
                            taskId: task.id,
                            sessionId: nil,
                            quoteId: nil,
                            userId: nil,
                            nodeId: nil,
                            formId: formId,
                            formInstanceId: nil,
                            mappingType: "task_form",
                            isDeleted: true
                        )
                    )
                    NetworkState.shared.enqueue(mappingOp)
                }
                
                onCompletion?(true, "Task deleted locally, will sync when online")
            }
        } catch {
            AppLogger.log(.error, "Failed to delete task: \(error)", category: .task)
            onCompletion?(false, error.localizedDescription)
        }
    }
} // End of TaskService class
