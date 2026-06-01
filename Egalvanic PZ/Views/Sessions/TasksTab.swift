//
//  TasksTab.swift
//  SwiftDataTutorial
//
//  Tasks tab with multi-link/unlink functionality
//

import SwiftUI
import SwiftData

struct TasksTab: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    let session: IRSession
    @Binding var selectedTask: UserTask?

    @State private var showTaskLinkingSheet = false
    @State private var showingTaskCreation = false
    @State private var isProcessing = false
    @State private var errorMessage: String?
    @State private var showError = false

    private let api = APIClient.shared
    
    private var tasks: [UserTask] {
        session.user_tasks.filter { !$0.is_deleted }
            .sorted(by: { $0.title < $1.title })
    }
    
    private var completedTasks: [UserTask] {
        tasks.filter { $0.completed }
    }
    
    private var pendingTasks: [UserTask] {
        tasks.filter { !$0.completed }
    }
    
    var body: some View {
        ZStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Link/Unlink Tasks Button
                    HStack {
                        Spacer()

                        Button(action: { showTaskLinkingSheet = true }) {
                            Label(AppStrings.Sessions.manageTasks, systemImage: "link.circle.fill")
                                .font(.subheadline)
                                .fontWeight(.medium)
                        }
                        .buttonStyle(.borderedProminent)
                        .padding(.horizontal)
                    }
                
                if !tasks.isEmpty {
                    // Summary
                    HStack {
                        Spacer()
                        
                        VStack {
                            Text("\(tasks.count)")
                                .font(.title2)
                                .fontWeight(.bold)
                            Text(AppStrings.CommonExtra.total)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        VStack {
                            Text("\(completedTasks.count)")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                            Text(AppStrings.Sessions.completed)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        VStack {
                            Text("\(pendingTasks.count)")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.orange)
                            Text(AppStrings.Common.pending)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                    }
                    .padding()
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(12)
                    .padding(.horizontal)
                    
                    // Pending Tasks
                    if !pendingTasks.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(AppStrings.Sessions.pendingTasks)
                                .font(.headline)
                                .padding(.horizontal)
                            
                            ForEach(pendingTasks) { task in
                                TaskCard(task: task, isListening: networkState.isListening(task.id))
                                    .padding(.horizontal)
                                    .contentShape(Rectangle())
                                    .onTapGesture {
                                        selectedTask = task
                                    }
                                    .contextMenu {
                                        Button {
                                            networkState.toggleListening(task.id)
                                        } label: {
                                            Label(
                                                networkState.isListening(task.id) ? AppStrings.Sessions.stopListening : AppStrings.Sessions.listenForAssets,
                                                systemImage: networkState.isListening(task.id) ? "ear.trianglebadge.exclamationmark" : "ear"
                                            )
                                        }
                                    }
                            }
                        }
                    }
                    
                    // Completed Tasks
                    if !completedTasks.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(AppStrings.Sessions.completedTasks)
                                .font(.headline)
                                .padding(.horizontal)
                                .padding(.top)
                            
                            ForEach(completedTasks) { task in
                                TaskCard(task: task, isListening: networkState.isListening(task.id))
                                    .padding(.horizontal)
                                    .contentShape(Rectangle())
                                    .onTapGesture {
                                        selectedTask = task
                                    }
                                    .contextMenu {
                                        Button {
                                            networkState.toggleListening(task.id)
                                        } label: {
                                            Label(
                                                networkState.isListening(task.id) ? AppStrings.Sessions.stopListening : AppStrings.Sessions.listenForAssets,
                                                systemImage: networkState.isListening(task.id) ? "ear.trianglebadge.exclamationmark" : "ear"
                                            )
                                        }
                                    }
                            }
                        }
                    }
                } else {
                    ContentUnavailableView(
                        AppStrings.Sessions.noTasks,
                        systemImage: "checklist",
                        description: Text(AppStrings.Sessions.linkTasksDescription)
                    )
                    .frame(minHeight: 400)
                }
                }
                .padding(.vertical)
            }

            // Floating Plus Button
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Button(action: {
                        showingTaskCreation = true
                    }) {
                        Image(systemName: "plus")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(appState.activeSession?.id == session.id ? Color.blue : Color.gray)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                    }
                    .disabled(appState.activeSession?.id != session.id)
                    .padding(.trailing, 20)
                    .padding(.bottom, 20)
                }
            }
        }
        .sheet(isPresented: $showTaskLinkingSheet) {
            TaskLinkingView(
                session: session,
                onUpdate: { linkedTaskIds in
                    Task {
                        await updateTaskLinks(linkedTaskIds: linkedTaskIds)
                    }
                }
            )
        }
        .fullScreenCover(isPresented: $showingTaskCreation) {
            UnifiedTaskCreationView()
                .environmentObject(networkState)
        }
        .overlay {
            if isProcessing {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.CommonExtra.updating)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(errorMessage ?? AppStrings.CommonExtra.anErrorOccurred)
        }
    }
    
    private func updateTaskLinks(linkedTaskIds: Set<UUID>) async {
        isProcessing = true
        defer { isProcessing = false }
        
        let currentTaskIds = Set(tasks.map { $0.id })
        
        // Tasks to link (not currently linked)
        let tasksToLink = linkedTaskIds.subtracting(currentTaskIds)
        
        // Tasks to unlink (currently linked but not in new selection)
        let tasksToUnlink = currentTaskIds.subtracting(linkedTaskIds)
        
        var errors: [String] = []
        
        // Process links
        for taskId in tasksToLink {
            do {
                if networkState.mode == .online {
                    _ = try await api.createTaskSessionMapping(
                        taskId: taskId,
                        sessionId: session.id
                    )
                } else {
                    // Queue for offline sync
                    let mappingOp = SyncOp(
                        target: .mappingTaskSession,
                        operation: .create,
                        mappingData: MappingData(
                            issueId: nil,
                            taskId: taskId,
                            sessionId: session.id,
                            quoteId: nil,
                            userId: nil,
                            nodeId: nil,
                            formId: nil,
                            formInstanceId: nil,
                            mappingType: nil,
                            isDeleted: false
                        )
                    )
                    networkState.enqueue(mappingOp)
                }
                
                // Update local data
                if let task = try? modelContext.fetch(
                    FetchDescriptor<UserTask>(
                        predicate: #Predicate { $0.id == taskId }
                    )
                ).first {
                    session.user_tasks.append(task)
                }
            } catch {
                guard !AuthError.isAuthError(error) else { return }
                errors.append("Failed to link task: \(error.localizedDescription)")
            }
        }

        // Process unlinks
        for taskId in tasksToUnlink {
            do {
                if networkState.mode == .online {
                    _ = try await api.updateTaskSessionMapping(
                        taskId: taskId,
                        sessionId: session.id,
                        isDeleted: true
                    )
                } else {
                    // Queue for offline sync
                    let mappingOp = SyncOp(
                        target: .mappingTaskSession,
                        operation: .update,
                        mappingData: MappingData(
                            issueId: nil,
                            taskId: taskId,
                            sessionId: session.id,
                            quoteId: nil,
                            userId: nil,
                            nodeId: nil,
                            formId: nil,
                            formInstanceId: nil,
                            mappingType: nil,
                            isDeleted: true
                        )
                    )
                    networkState.enqueue(mappingOp)
                }
                
                // Update local data
                session.user_tasks.removeAll { $0.id == taskId }
            } catch {
                guard !AuthError.isAuthError(error) else { return }
                errors.append("Failed to unlink task: \(error.localizedDescription)")
            }
        }

        // Save changes
        do {
            try modelContext.save()
        } catch {
            errors.append("Failed to save changes: \(error.localizedDescription)")
        }
        
        if !errors.isEmpty {
            await MainActor.run {
                errorMessage = errors.joined(separator: "\n")
                showError = true
            }
        }
    }
}

struct TaskLinkingView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    
    let session: IRSession
    let onUpdate: (Set<UUID>) -> Void
    
    @State private var selectedTaskIds: Set<UUID> = []
    @State private var searchText = ""
    
    @Query private var allTasks: [UserTask]
    
    private var sldTasks: [UserTask] {
        allTasks.filter {
            !$0.is_deleted &&
            $0.sld?.id == session.sld.id
        }
    }
    
    private var filteredTasks: [UserTask] {
        let filtered = if searchText.isEmpty {
            sldTasks
        } else {
            sldTasks.filter {
                $0.title.localizedCaseInsensitiveContains(searchText) ||
                ($0.node?.label.localizedCaseInsensitiveContains(searchText) ?? false)
            }
        }
        
        // Sort by: 1) Linked tasks first, 2) Then alphabetical by title
        return filtered.sorted { task1, task2 in
            let isTask1Linked = currentLinkedTaskIds.contains(task1.id)
            let isTask2Linked = currentLinkedTaskIds.contains(task2.id)
            
            if isTask1Linked != isTask2Linked {
                return isTask1Linked // Linked tasks come first
            }
            
            return task1.title.localizedCompare(task2.title) == .orderedAscending
        }
    }
    
    private var currentLinkedTaskIds: Set<UUID> {
        Set(session.user_tasks.filter { !$0.is_deleted }.map { $0.id })
    }
    
    var body: some View {
        NavigationView {
            List {
                Section {
                    ForEach(filteredTasks) { task in
                        TaskSelectionRow(
                            task: task,
                            isSelected: selectedTaskIds.contains(task.id),
                            onToggle: {
                                if selectedTaskIds.contains(task.id) {
                                    selectedTaskIds.remove(task.id)
                                } else {
                                    selectedTaskIds.insert(task.id)
                                }
                            }
                        )
                    }
                } header: {
                    Text(AppStrings.Sessions.selectTasksToLink)
                }
            }
            .searchable(text: $searchText, prompt: AppStrings.Sessions.searchTasks)
            .navigationTitle(AppStrings.Sessions.linkTasks)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.CommonExtra.update) {
                        onUpdate(selectedTaskIds)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(selectedTaskIds == currentLinkedTaskIds)
                }
            }
        }
        .onAppear {
            // Initialize with currently linked tasks
            selectedTaskIds = currentLinkedTaskIds
        }
    }
}

struct TaskSelectionRow: View {
    let task: UserTask
    let isSelected: Bool
    let onToggle: () -> Void
    
    private var truncatedNodeLabel: String? {
        guard let label = task.node?.label else { return nil }
        if label.count > 10 {
            return String(label.prefix(10)) + "..."
        }
        return label
    }
    
    var body: some View {
        Button(action: onToggle) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(task.title)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.primary)
                        
                        if task.completed {
                            Text(AppStrings.Sessions.completed)
                                .font(.caption2)
                                .fontWeight(.medium)
                                .foregroundColor(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.green)
                                .cornerRadius(4)
                        }
                    }
                    
                    HStack {
                        if let nodeLabel = truncatedNodeLabel {
                            Label(nodeLabel, systemImage: "location.fill")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        if let submittedAt = task.submitted_at {
                            HStack(spacing: 4) {
                                Text(submittedAt.formatted(date: .abbreviated, time: .omitted))
                                    .font(.caption2)
                                    .foregroundColor(.blue)
                                Image(systemName: "checkmark.seal")
                                    .font(.caption2)
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                }
                
                Spacer()
                
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .blue : .gray)
                    .font(.title3)
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct TaskCard: View {
    let task: UserTask
    var isListening: Bool = false

    private var completionColor: Color {
        task.completed ? .green : .gray
    }

    private var nodeDisplayLabel: String? {
        let linkedNodes = task.linkedNodes.filter { !$0.is_deleted }

        if linkedNodes.isEmpty {
            return nil
        } else if linkedNodes.count == 1 {
            return linkedNodes[0].label
        } else {
            // Show first node + count of others
            return "\(linkedNodes[0].label) + \(linkedNodes.count - 1) others"
        }
    }

    private var formDisplayLabel: String? {
        let linkedForms = task.linkedForms.filter { !$0.is_deleted }

        if linkedForms.isEmpty {
            return nil
        } else if linkedForms.count == 1 {
            return linkedForms[0].title
        } else {
            // Show first form + count of others
            return "\(linkedForms[0].title) + \(linkedForms.count - 1) others"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: task.completed ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(completionColor)
                    .font(.system(size: 16))

                Text(task.title)
                    .font(.system(size: 15, weight: .semibold))
                    .lineLimit(2)
                    .strikethrough(task.completed, color: .secondary)

                Spacer()

                if isListening {
                    Image(systemName: "ear.fill")
                        .font(.caption)
                        .foregroundColor(.orange)
                }

                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }

            if !task.task_description.isEmpty {
                Text(task.task_description)
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .lineLimit(3)
            }

            HStack(spacing: 16) {
                if let nodeLabel = nodeDisplayLabel {
                    Label(nodeLabel, systemImage: "location.fill")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                if let formLabel = formDisplayLabel {
                    Label(formLabel, systemImage: "doc.text")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                if task.photos.count > 0 {
                    Label("\(task.photos.count) photo\(task.photos.count == 1 ? "" : "s")", systemImage: "camera.fill")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
        }
        .padding()
        .background(Color(UIColor.systemGray6))
        .cornerRadius(10)
        .contentShape(Rectangle())
    }
}
