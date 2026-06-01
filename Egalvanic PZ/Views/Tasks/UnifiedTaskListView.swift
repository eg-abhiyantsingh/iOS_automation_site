import SwiftUI
import SwiftData

struct UnifiedTaskListView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var networkState: NetworkState

    @Query(filter: #Predicate<UserTask> { !$0.is_deleted }) private var allTasks: [UserTask]
    @State private var selectedTask: UserTask?
    @State private var selectedPhotosTask: UserTask?
    @State private var showingTaskCreation = false
    @State private var showingCompleteConfirmation = false
    @State private var taskPendingCompletion: UserTask?
    @State private var showingDeleteConfirmation = false
    @State private var taskPendingDeletion: UserTask?
    // Long-press → Open Form: when the tapped task has multiple linked
    // forms we have to disambiguate. Otherwise we open directly.
    @State private var selectedLegacyFormInstance: FormInstance?
    @State private var selectedEGFormInstance: EGFormInstance?
    @State private var formPickerTask: UserTask?
    @State private var noFormsAlertTask: UserTask?
    @State private var goToWorkOrderSession: IRSession?

    var body: some View {
        EntityListView(
            configuration: TaskListConfiguration(
                appState: appState,
                networkState: networkState,
                modelContext: modelContext,
                onTaskTapped: { task in
                    selectedTask = task
                },
                onCreateTapped: {
                    showingTaskCreation = true
                },
                onDeleteTask: { task in
                    taskPendingDeletion = task
                    showingDeleteConfirmation = true
                },
                onCompleteTask: { task in
                    completeTask(task)
                },
                onReopenTask: { task in
                    reopenTask(task)
                },
                onOpenForm: { task in handleOpenForm(task) },
                onOpenPhotos: { task in selectedPhotosTask = task },
                onGoToWorkOrder: { _, session in goToWorkOrderSession = session }
            ),
            entities: allTasks
        )
        .environmentObject(networkState)
        .fullScreenCover(item: $selectedTask) { task in
            TaskDetailView(task: task)
        }
        .fullScreenCover(item: $selectedPhotosTask) { task in
            TaskDetailView(task: task, photosOnly: true)
        }
        .fullScreenCover(item: $goToWorkOrderSession) { session in
            NavigationStack {
                IRSessionDetailView(session: session)
                    .toolbar {
                        // Leading Done button — the cover has no other
                        // way to dismiss because IRSessionDetailView is
                        // normally pushed from a parent NavigationStack
                        // (which would provide a back button) but here
                        // it's the root of the cover.
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button(AppStrings.Common.done) {
                                goToWorkOrderSession = nil
                            }
                        }
                    }
            }
            .environmentObject(networkState)
            .environmentObject(appState)
        }
        .fullScreenCover(item: $selectedLegacyFormInstance) { instance in
            // FormWebAppContainerView needs a UserTask reference for
            // its WebView bridge context. The form instance is linked
            // to at least one (the task we long-pressed); use it.
            if let parentTask = instance.linkedTasks.first(where: { !$0.is_deleted }) ?? instance.linkedTasks.first {
                NavigationStack {
                    FormWebAppContainerView(
                        task: parentTask,
                        formInstance: instance,
                        onSubmit: { selectedLegacyFormInstance = nil }
                    )
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button(AppStrings.Common.cancel) {
                                selectedLegacyFormInstance = nil
                            }
                        }
                    }
                }
                .environmentObject(networkState)
            }
        }
        .fullScreenCover(item: $selectedEGFormInstance) { instance in
            EGFormView(instance: instance, onCancel: { selectedEGFormInstance = nil })
        }
        .fullScreenCover(isPresented: $showingTaskCreation) {
            UnifiedTaskCreationFromListView { task in
                // Task was created successfully
                AppLogger.log(.info, "Created task: \(task.title)", category: .task)
            }
        }
        .sheet(item: $formPickerTask) { task in
            // ZP-2336: only surface EG form instances when the company has
            // the ``eg-forms`` feature flag enabled.
            TaskFormChoicePickerSheet(
                legacy: task.linkedFormInstances.filter { !$0.is_deleted },
                eg: AuthService.shared.hasFeature("eg-forms")
                    ? fetchEGFormInstances(for: task)
                    : [],
                onPickLegacy: { instance in
                    formPickerTask = nil
                    selectedLegacyFormInstance = instance
                },
                onPickEG: { instance in
                    formPickerTask = nil
                    selectedEGFormInstance = instance
                },
                onCancel: { formPickerTask = nil }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
        .alert("No Forms Linked", isPresented: Binding(
            get: { noFormsAlertTask != nil },
            set: { if !$0 { noFormsAlertTask = nil } }
        )) {
            Button("OK", role: .cancel) { noFormsAlertTask = nil }
        } message: {
            Text("This task has no forms linked. Open the task and use Link Forms to add one.")
        }
        .alert(AppStrings.Tasks.completeTask, isPresented: $showingCompleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                taskPendingCompletion = nil
            }
            Button(AppStrings.Tasks.complete) {
                if let task = taskPendingCompletion {
                    performTaskCompletion(task)
                }
                taskPendingCompletion = nil
            }
        } message: {
            Text(AppStrings.Tasks.completeTaskMessage)
        }
        .alert(AppStrings.Tasks.deleteTask, isPresented: $showingDeleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                taskPendingDeletion = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let task = taskPendingDeletion {
                    deleteTask(task)
                }
                taskPendingDeletion = nil
            }
        } message: {
            Text(AppStrings.Tasks.deleteTaskConfirm)
        }
    }

    // MARK: - Long-press: Open Form routing

    private func handleOpenForm(_ task: UserTask) {
        let legacy = task.linkedFormInstances.filter { !$0.is_deleted }
        let eg = fetchEGFormInstances(for: task)
        let total = legacy.count + eg.count

        switch total {
        case 0:
            noFormsAlertTask = task
        case 1:
            if let only = legacy.first {
                selectedLegacyFormInstance = only
            } else if let only = eg.first {
                selectedEGFormInstance = only
            }
        default:
            formPickerTask = task
        }
    }

    private func fetchEGFormInstances(for task: UserTask) -> [EGFormInstance] {
        // Read the canonical task-side array. `inst.linkedTasks` is the
        // auto-synthesized single-valued reverse and can move between
        // instances after a re-sync — TaskDetailView already standardized
        // on `task.linkedEGFormInstances` for the same reason.
        return task.linkedEGFormInstances
            .filter { !$0.is_deleted }
            .sorted { $0.created_at > $1.created_at }
    }
    
    private func deleteTask(_ task: UserTask) {
        task.is_deleted = true

        do {
            try modelContext.save()

            if networkState.mode == .online {
                Task {
                    do {
                        _ = try await APIClient.shared.updateTask(task)
                    } catch {
                        AppLogger.log(.error, "Failed to sync task deletion: \(error)", category: .task)
                        let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                        networkState.enqueue(op)
                    }
                }
            } else {
                let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                networkState.enqueue(op)
            }
        } catch {
            AppLogger.log(.error, "Failed to delete task: \(error)", category: .task)
        }
    }

    private func completeTask(_ task: UserTask) {
        let activeNodes = task.linkedNodes.filter { !$0.is_deleted }
        let hasIncompleteNodes = activeNodes.contains { node in
            !(task.nodeCompletions[node.id.uuidString] ?? false)
        }

        // Show confirmation if there are any incomplete linked nodes
        if hasIncompleteNodes {
            taskPendingCompletion = task
            showingCompleteConfirmation = true
        } else {
            performTaskCompletion(task)
        }
    }

    private func performTaskCompletion(_ task: UserTask) {
        task.completed = true
        task.submitted_at = Date()

        // Mark all linked nodes as complete locally
        let activeNodes = task.linkedNodes.filter { !$0.is_deleted }
        for node in activeNodes {
            task.nodeCompletions[node.id.uuidString] = true
        }

        do {
            try modelContext.save()

            // Sync to server
            Task {
                if networkState.mode == .online {
                    do {
                        if !activeNodes.isEmpty {
                            // Call bulk completion API for tasks with linked nodes
                            let completions = activeNodes.map { (nodeId: $0.id, isCompleted: true) }
                            try await TaskMappingService.shared.bulkUpdateNodeCompletions(
                                taskId: task.id,
                                completions: completions
                            )
                        } else {
                            // Call task update API for tasks without linked nodes
                            _ = try await APIClient.shared.updateTask(task)
                        }
                    } catch {
                        AppLogger.log(.error, "Failed to sync task completion: \(error)", category: .task)
                        let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                        networkState.enqueue(op)
                    }
                } else {
                    if !activeNodes.isEmpty {
                        // Queue bulk completion for tasks with linked nodes
                        let completions = activeNodes.map { (nodeId: $0.id, isCompleted: true) }
                        try? await TaskMappingService.shared.bulkUpdateNodeCompletions(
                            taskId: task.id,
                            completions: completions
                        )
                    } else {
                        // Queue task update for tasks without linked nodes
                        let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                        networkState.enqueue(op)
                    }
                }
            }
        } catch {
            AppLogger.log(.error, "Failed to complete task: \(error)", category: .task)
        }
    }

    private func reopenTask(_ task: UserTask) {
        task.completed = false
        task.submitted_at = nil
        TaskService.updateTask(task: task, modelContext: modelContext) { success, error in
            if let error = error {
                AppLogger.log(.error, "Failed to reopen task: \(error)", category: .task)
            }
        }
    }
}