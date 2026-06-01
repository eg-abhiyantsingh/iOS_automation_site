//
//  NoLocationSessionDetailViewModel.swift
//  Egalvanic PZ
//
//  ViewModel for NoLocationSessionDetailView - manages unassigned assets in a session
//

import SwiftUI
import SwiftData

@MainActor
class NoLocationSessionDetailViewModel: ObservableObject, SessionAssetRowViewModel {

    // MARK: - Published Properties

    @Published var expandedNodes: Set<UUID> = []
    @Published var searchText: String = ""
    @Published var isUpdating: Bool = false
    @Published var showError: Bool = false
    @Published var errorMessage: String = ""
    @Published var nodeIRDetail: NodeV2? = nil
    @Published var showingAddNodes: Bool = false

    // Edit node state (lifted from SessionRoomAssetRow to prevent dismissal on data changes)
    @Published var selectedEditNode: NodeV2? = nil
    @Published var selectedEditFocusMode: NodeDetailFocusMode = .all
    @Published var selectedTask: UserTask? = nil
    @Published var selectedIssue: Issue? = nil
    @Published var showingLinkTaskNode: NodeV2? = nil

    // MARK: - Properties

    let session: IRSession
    let diagram: SLDV2
    var autoOpenNodeForIR: NodeV2?

    // MARK: - Private Properties

    private var modelContext: ModelContext?
    private var networkState: NetworkState?
    private var appState: AppStateManager?

    // MARK: - Initialization

    init(session: IRSession, diagram: SLDV2, autoOpenNodeForIR: NodeV2? = nil) {
        self.session = session
        self.diagram = diagram
        self.autoOpenNodeForIR = autoOpenNodeForIR
    }

    // MARK: - Configuration

    func configure(
        modelContext: ModelContext,
        networkState: NetworkState,
        appState: AppStateManager
    ) {
        self.modelContext = modelContext
        self.networkState = networkState
        self.appState = appState
    }

    // MARK: - Computed Properties

    /// Session nodes without a room (unassigned)
    var sessionNodesWithoutRoom: [NodeV2] {
        session.nodes
            .filter { !$0.is_deleted && $0.room == nil }
            .sorted(by: { $0.label < $1.label })
    }

    var sessionUserTaskIds: Set<UUID> {
        Set(session.user_tasks.map(\.id))
    }

    var sessionIssueIds: Set<UUID> {
        Set(session.issues.map(\.id))
    }

    var taskNodeCounts: [UUID: Int] {
        let taskIds = sessionUserTaskIds
        var counts: [UUID: Int] = [:]
        for node in session.nodes where !node.is_deleted {
            for task in node.node_tasks where !task.is_deleted && taskIds.contains(task.id) {
                counts[task.id, default: 0] += 1
            }
        }
        return counts
    }

    var issueNodeCounts: [UUID: Int] {
        let issueIds = sessionIssueIds
        var counts: [UUID: Int] = [:]
        for node in session.nodes where !node.is_deleted {
            for issue in node.issues where !issue.is_deleted && issueIds.contains(issue.id) {
                counts[issue.id, default: 0] += 1
            }
        }
        return counts
    }

    var hierarchy: (topLevelAssets: [NodeV2], childrenMap: [UUID: [NodeV2]]) {
        buildNodeHierarchy()
    }

    // MARK: - Search Methods

    func nodeMatchesSearch(_ node: NodeV2) -> Bool {
        guard !searchText.isEmpty else { return true }
        let searchLower = searchText.lowercased()
        return node.label.lowercased().contains(searchLower) ||
               node.type.lowercased().contains(searchLower) ||
               (node.node_class?.name.lowercased().contains(searchLower) ?? false)
    }

    func getAllDescendants(of node: NodeV2, using childrenMap: [UUID: [NodeV2]]) -> [NodeV2] {
        var descendants: [NodeV2] = []
        if let children = childrenMap[node.id] {
            descendants.append(contentsOf: children)
            for child in children {
                descendants.append(contentsOf: getAllDescendants(of: child, using: childrenMap))
            }
        }
        return descendants
    }

    func anyDescendantMatchesSearch(of node: NodeV2, using childrenMap: [UUID: [NodeV2]]) -> Bool {
        let descendants = getAllDescendants(of: node, using: childrenMap)
        return descendants.contains { nodeMatchesSearch($0) }
    }

    // MARK: - Hierarchy Building

    func buildNodeHierarchy() -> (topLevelAssets: [NodeV2], childrenMap: [UUID: [NodeV2]]) {
        // Create a map of node ID to node for quick lookup
        let nodeMap = Dictionary(uniqueKeysWithValues: sessionNodesWithoutRoom.map { ($0.id, $0) })

        // Create a map of parent ID to children
        var childrenMap: [UUID: [NodeV2]] = [:]
        var topLevelNodes: [NodeV2] = []

        for node in sessionNodesWithoutRoom {
            if let parentId = node.parent_id,
               nodeMap[parentId] != nil {
                // This node has a parent in our unassigned nodes
                childrenMap[parentId, default: []].append(node)
            } else {
                // This is a top-level node (no parent or parent not in unassigned nodes)
                topLevelNodes.append(node)
            }
        }

        // Sort children in each group
        for parentId in childrenMap.keys {
            childrenMap[parentId]?.sort { $0.label < $1.label }
        }

        // Filter based on search if there's search text
        if !searchText.isEmpty {
            // Filter top-level nodes: include if they match OR if any descendant matches
            topLevelNodes = topLevelNodes.filter { node in
                nodeMatchesSearch(node) || anyDescendantMatchesSearch(of: node, using: childrenMap)
            }
        }

        return (topLevelNodes.sorted { $0.label < $1.label }, childrenMap)
    }

    // MARK: - Node Expansion

    func toggleNodeExpansion(_ node: NodeV2) {
        if expandedNodes.contains(node.id) {
            expandedNodes.remove(node.id)
        } else {
            expandedNodes.insert(node.id)
        }
    }

    // MARK: - Auto Open Node for IR

    func checkAutoOpenNode() {
        if let nodeToOpen = autoOpenNodeForIR {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                self?.nodeIRDetail = nodeToOpen
            }
            // Clear so it doesn't auto-open again
            autoOpenNodeForIR = nil
        }
    }

    // MARK: - Edit Node Dismissal Handler

    func onEditNodeDismissed() {
        selectedEditFocusMode = .all
    }

    // MARK: - Node Operations

    func removeNode(_ node: NodeV2) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        Task {
            await MainActor.run { isUpdating = true }

            do {
                // Remove from session with bidirectional relationship
                session.nodes.removeAll { $0.id == node.id }
                node.ir_sessions.removeAll { $0.id == session.id }

                try modelContext.save()

                // Sync mapping deletion
                if networkState.mode == .online {
                    do {
                        _ = try await APIClient.shared.updateNodeSessionMapping(
                            nodeId: node.id,
                            sessionId: session.id,
                            isDeleted: true
                        )
                    } catch {
                        networkState.enqueue(SyncOp(
                            target: .mappingNodeSession,
                            operation: .delete,
                            mappingData: MappingData.nodeSession(
                                nodeId: node.id,
                                sessionId: session.id,
                                isDeleted: true
                            )
                        ))
                    }
                } else {
                    networkState.enqueue(SyncOp(
                        target: .mappingNodeSession,
                        operation: .delete,
                        mappingData: MappingData.nodeSession(
                            nodeId: node.id,
                            sessionId: session.id,
                            isDeleted: true
                        )
                    ))
                }

                await MainActor.run { isUpdating = false }
            } catch {
                guard !AuthError.isAuthError(error) else { return }
                await MainActor.run {
                    isUpdating = false
                    errorMessage = "Failed to remove asset: \(error.localizedDescription)"
                    showError = true
                }
            }
        }
    }

    // MARK: - Individual Task/Issue Actions

    func markNodeTaskComplete(task: UserTask, node: NodeV2) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        task.nodeCompletions[node.id.uuidString] = true

        let allNodesComplete = task.linkedNodes
            .filter { !$0.is_deleted }
            .allSatisfy { task.nodeCompletions[$0.id.uuidString] == true }

        if allNodesComplete {
            task.completed = true
            task.submitted_at = Date()
        }

        try? modelContext.save()

        Task {
            try? await TaskMappingService.shared.updateNodeCompletion(
                taskId: task.id, nodeId: node.id, isCompleted: true
            )
            if task.completed {
                if networkState.mode == .online {
                    _ = try? await APIClient.shared.updateTask(task)
                } else {
                    let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                    networkState.enqueue(op)
                }
            }
        }
    }

    func unmarkNodeTaskComplete(task: UserTask, node: NodeV2) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        task.nodeCompletions[node.id.uuidString] = false

        let wasCompleted = task.completed
        if wasCompleted {
            task.completed = false
            task.submitted_at = nil
        }

        try? modelContext.save()

        Task {
            try? await TaskMappingService.shared.updateNodeCompletion(
                taskId: task.id, nodeId: node.id, isCompleted: false
            )
            if wasCompleted {
                if networkState.mode == .online {
                    _ = try? await APIClient.shared.updateTask(task)
                } else {
                    let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                    networkState.enqueue(op)
                }
            }
        }
    }

    func resolveIssue(_ issue: Issue) {
        guard let modelContext = modelContext else { return }
        IssueService.resolveIssue(issue, modelContext: modelContext)
    }

    func reopenIssue(_ issue: Issue) {
        guard let modelContext = modelContext else { return }
        IssueService.reopenIssue(issue, modelContext: modelContext)
    }

    // MARK: - Bulk Node Actions

    func markNodeTasksComplete(node: NodeV2, session: IRSession) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        let sessionTasks = node.node_tasks.filter { task in
            !task.is_deleted &&
            session.user_tasks.contains(where: { $0.id == task.id }) &&
            !(task.nodeCompletions[node.id.uuidString] ?? false)
        }

        guard !sessionTasks.isEmpty else { return }

        for task in sessionTasks {
            task.nodeCompletions[node.id.uuidString] = true

            let allNodesComplete = task.linkedNodes
                .filter { !$0.is_deleted }
                .allSatisfy { task.nodeCompletions[$0.id.uuidString] == true }

            if allNodesComplete {
                task.completed = true
                task.submitted_at = Date()
            }
        }

        try? modelContext.save()

        Task {
            for task in sessionTasks {
                try? await TaskMappingService.shared.updateNodeCompletion(
                    taskId: task.id, nodeId: node.id, isCompleted: true
                )
                if task.completed {
                    if networkState.mode == .online {
                        _ = try? await APIClient.shared.updateTask(task)
                    } else {
                        let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                        networkState.enqueue(op)
                    }
                }
            }
        }
    }

    func unmarkNodeTasksComplete(node: NodeV2, session: IRSession) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        let completedTasks = node.node_tasks.filter { task in
            !task.is_deleted &&
            session.user_tasks.contains(where: { $0.id == task.id }) &&
            (task.nodeCompletions[node.id.uuidString] ?? false)
        }

        guard !completedTasks.isEmpty else { return }

        for task in completedTasks {
            task.nodeCompletions[node.id.uuidString] = false

            if task.completed {
                task.completed = false
                task.submitted_at = nil
            }
        }

        try? modelContext.save()

        Task {
            for task in completedTasks {
                try? await TaskMappingService.shared.updateNodeCompletion(
                    taskId: task.id, nodeId: node.id, isCompleted: false
                )
                if !task.completed {
                    if networkState.mode == .online {
                        _ = try? await APIClient.shared.updateTask(task)
                    } else {
                        let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                        networkState.enqueue(op)
                    }
                }
            }
        }
    }

    func resolveNodeIssues(node: NodeV2, session: IRSession) {
        guard let modelContext = modelContext else { return }

        let unresolvedIssues = node.issues.filter { issue in
            !issue.is_deleted &&
            session.issues.contains(where: { $0.id == issue.id }) &&
            issue.status?.lowercased() != "resolved"
        }

        guard !unresolvedIssues.isEmpty else { return }

        for issue in unresolvedIssues {
            IssueService.resolveIssue(issue, modelContext: modelContext)
        }
    }

    func reopenNodeIssues(node: NodeV2, session: IRSession) {
        guard let modelContext = modelContext else { return }

        let resolvedIssues = node.issues.filter { issue in
            !issue.is_deleted &&
            session.issues.contains(where: { $0.id == issue.id }) &&
            issue.status?.lowercased() == "resolved"
        }

        guard !resolvedIssues.isEmpty else { return }

        for issue in resolvedIssues {
            IssueService.reopenIssue(issue, modelContext: modelContext)
        }
    }

    // MARK: - Link Tasks to Node

    func linkTasksToNode(_ node: NodeV2, selectedTaskIds: Set<UUID>) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        let currentlyLinkedIds = Set(
            node.node_tasks
                .filter { task in !task.is_deleted && session.user_tasks.contains(where: { ut in ut.id == task.id }) }
                .map(\.id)
        )

        let toAdd = selectedTaskIds.subtracting(currentlyLinkedIds)
        let toRemove = currentlyLinkedIds.subtracting(selectedTaskIds)

        for taskId in toAdd {
            guard let task = session.user_tasks.first(where: { $0.id == taskId }) else { continue }
            if !node.node_tasks.contains(where: { $0.id == taskId }) {
                node.node_tasks.append(task)
            }
            if !task.linkedNodes.contains(where: { $0.id == node.id }) {
                task.linkedNodes.append(node)
            }
        }

        for taskId in toRemove {
            guard let task = session.user_tasks.first(where: { $0.id == taskId }) else { continue }
            node.node_tasks.removeAll { $0.id == taskId }
            task.linkedNodes.removeAll { $0.id == node.id }
        }

        do {
            try modelContext.save()
        } catch {
            AppLogger.log(.error, "Failed to save task-node links: \(error)", category: .location)
        }

        Task {
            for taskId in toAdd {
                try? await TaskMappingService.shared.addTaskNodes(taskId: taskId, nodeIds: [node.id])
            }
            for taskId in toRemove {
                try? await TaskMappingService.shared.removeTaskNodes(taskId: taskId, nodeIds: [node.id])
            }
        }
    }
}
