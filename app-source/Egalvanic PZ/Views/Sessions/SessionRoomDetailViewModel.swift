//
//  SessionRoomDetailViewModel.swift
//  Egalvanic PZ
//
//  ViewModel for SessionRoomDetailView - manages room assets, search, hierarchy, and node operations
//

import SwiftUI
import SwiftData

// MARK: - Protocol for SessionRoomAssetRow compatibility

/// Protocol that ViewModels must conform to for use with SessionRoomAssetRow
@MainActor
protocol SessionAssetRowViewModel: ObservableObject {
    var session: IRSession { get }
    var expandedNodes: Set<UUID> { get set }
    var selectedEditNode: NodeV2? { get set }
    var selectedEditFocusMode: NodeDetailFocusMode { get set }
    var selectedTask: UserTask? { get set }
    var selectedIssue: Issue? { get set }
    var showingLinkTaskNode: NodeV2? { get set }

    func toggleNodeExpansion(_ node: NodeV2)
    func removeNode(_ node: NodeV2)
    func markNodeTaskComplete(task: UserTask, node: NodeV2)
    func unmarkNodeTaskComplete(task: UserTask, node: NodeV2)
    func resolveIssue(_ issue: Issue)
    func reopenIssue(_ issue: Issue)
    func markNodeTasksComplete(node: NodeV2, session: IRSession)
    func unmarkNodeTasksComplete(node: NodeV2, session: IRSession)
    func resolveNodeIssues(node: NodeV2, session: IRSession)
    func reopenNodeIssues(node: NodeV2, session: IRSession)
}

/// Filter overlay for asset rows in room detail
enum AssetOverlayFilter: String, CaseIterable {
    case none = "None"
    case ir = "IR"
    case arcFlash = "Arc Flash"
    case com = "C.O.M."

    var iconName: String {
        switch self {
        case .none: return "line.3.horizontal.decrease.circle"
        case .ir: return "camera.filters"
        case .arcFlash: return "bolt.circle.fill"
        case .com: return "gauge.with.dots.needle.33percent"
        }
    }
}

@MainActor
class SessionRoomDetailViewModel: ObservableObject, SessionAssetRowViewModel {

    // MARK: - Published Properties (SessionRoomDetailView)

    @Published var expandedNodes: Set<UUID> = []
    @Published var searchText: String = ""
    @Published var showingAddNodes: Bool = false
    @Published var isUpdating: Bool = false
    @Published var showError: Bool = false
    @Published var errorMessage: String = ""
    @Published var nodeIRDetail: NodeV2? = nil
    @Published var showQRScanner: Bool = false

    // Edit node state (lifted from SessionRoomAssetRow to prevent dismissal on data changes)
    @Published var selectedEditNode: NodeV2? = nil
    @Published var selectedEditFocusMode: NodeDetailFocusMode = .all
    @Published var selectedTask: UserTask? = nil
    @Published var selectedIssue: Issue? = nil
    @Published var showingLinkTaskNode: NodeV2? = nil

    // MARK: - Published Properties (RoomNodeAdditionView)

    @Published var addNodeSelectedOption: Int = 0  // 0 = Existing Asset, 1 = New Asset
    @Published var addNodeSelectedNodes: Set<NodeV2> = []
    @Published var addNodeSearchText: String = ""
    @Published var addNodeIsUpdating: Bool = false
    @Published var addNodeShowError: Bool = false
    @Published var addNodeErrorMessage: String = ""
    @Published var showingAddAsset: Bool = false
    @Published var addNodeShowQRScanner: Bool = false
    @Published var showingQuickCount: Bool = false
    @Published var showingPhotoWalkthrough: Bool = false

    // MARK: - Published Properties (Direct Menu Actions)

    @Published var showingAddAssetDirect: Bool = false
    @Published var showingQuickCountDirect: Bool = false
    @Published var showingPhotoWalkthroughDirect: Bool = false

    // MARK: - Properties

    let session: IRSession
    let room: Room
    var autoOpenNodeForIR: NodeV2?

    // MARK: - Private Properties

    private var modelContext: ModelContext?
    private var networkState: NetworkState?
    private var appState: AppStateManager?
    private var sldService: SLDService?

    // MARK: - Initialization

    init(session: IRSession, room: Room, autoOpenNodeForIR: NodeV2? = nil) {
        self.session = session
        self.room = room
        self.autoOpenNodeForIR = autoOpenNodeForIR
    }

    // MARK: - Configuration

    func configure(
        modelContext: ModelContext,
        networkState: NetworkState,
        appState: AppStateManager,
        sldService: SLDService
    ) {
        self.modelContext = modelContext
        self.networkState = networkState
        self.appState = appState
        self.sldService = sldService
    }

    // MARK: - Computed Properties

    var roomAssets: [NodeV2] {
        session.nodes
            .filter { !$0.is_deleted && $0.room?.id == room.id }
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
               (node.node_class?.name.lowercased().contains(searchLower) ?? false) ||
               (node.qr_code?.lowercased().contains(searchLower) ?? false)
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
        let nodeMap = Dictionary(uniqueKeysWithValues: roomAssets.map { ($0.id, $0) })

        // Create a map of parent ID to children
        var childrenMap: [UUID: [NodeV2]] = [:]
        var topLevelNodes: [NodeV2] = []

        for node in roomAssets {
            if let parentId = node.parent_id,
               nodeMap[parentId] != nil {
                // This node has a parent in our room assets
                childrenMap[parentId, default: []].append(node)
            } else {
                // This is a top-level node (no parent or parent not in room assets)
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

            // Check if ALL linked nodes for this task are now complete
            let allNodesComplete = task.linkedNodes
                .filter { !$0.is_deleted }
                .allSatisfy { task.nodeCompletions[$0.id.uuidString] == true }

            if allNodesComplete {
                task.completed = true
                task.submitted_at = Date()
            }
        }

        try? modelContext.save()

        // Sync operations
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

        var tasksWithCompletionReverted: [UserTask] = []

        for task in completedTasks {
            task.nodeCompletions[node.id.uuidString] = false

            // If task was marked complete, revert it
            if task.completed {
                task.completed = false
                task.submitted_at = nil
                tasksWithCompletionReverted.append(task)
            }
        }

        try? modelContext.save()

        Task {
            for task in completedTasks {
                try? await TaskMappingService.shared.updateNodeCompletion(
                    taskId: task.id, nodeId: node.id, isCompleted: false
                )
            }
            for task in tasksWithCompletionReverted {
                if networkState.mode == .online {
                    _ = try? await APIClient.shared.updateTask(task)
                } else {
                    let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                    networkState.enqueue(op)
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

    // MARK: - Add Node Methods (from RoomNodeAdditionView)

    /// Filter available nodes from @Query result (nodes in room but not in session)
    func getFilteredNodes(from availableNodes: [NodeV2]) -> [NodeV2] {
        // Filter out nodes that are already linked to this session
        let unlinkedNodes = availableNodes.filter { node in
            !session.nodes.contains(where: { $0.id == node.id })
        }

        // Apply search filter
        if addNodeSearchText.isEmpty {
            return unlinkedNodes
        } else {
            return unlinkedNodes.filter { node in
                node.label.localizedCaseInsensitiveContains(addNodeSearchText) ||
                node.type.localizedCaseInsensitiveContains(addNodeSearchText) ||
                (node.qr_code?.localizedCaseInsensitiveContains(addNodeSearchText) ?? false)
            }
        }
    }

    func toggleNodeSelection(_ node: NodeV2) {
        if addNodeSelectedNodes.contains(node) {
            addNodeSelectedNodes.remove(node)
        } else {
            addNodeSelectedNodes.insert(node)
        }
    }

    func addSelectedNodesToSession(onComplete: @escaping () -> Void, dismiss: DismissAction) async {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        await MainActor.run { addNodeIsUpdating = true }

        AppLogger.log(.info, "Add selected nodes to session - start, count: \(addNodeSelectedNodes.count), session: \(session.id)", category: .node)

        do {
            // Collect all node IDs to link: selected nodes + their parents
            var nodeIdsToLink: Set<UUID> = []
            var nodesToAddLocally: [NodeV2] = []

            for node in addNodeSelectedNodes {
                AppLogger.log(.debug, "Processing node: \(node.label), id: \(node.id), parent_id: \(node.parent_id?.uuidString ?? "nil")", category: .node)

                nodeIdsToLink.insert(node.id)
                nodesToAddLocally.append(node)

                // If node has a parent, also link the parent
                if let parentId = node.parent_id {
                    AppLogger.log(.debug, "Parent ID found: \(parentId)", category: .node)
                    nodeIdsToLink.insert(parentId)

                    // Find parent node and add to local list if not already in session
                    if let parentNode = session.sld.nodes.first(where: { $0.id == parentId && !$0.is_deleted }) {
                        if !session.nodes.contains(where: { $0.id == parentNode.id }) {
                            nodesToAddLocally.append(parentNode)
                            AppLogger.log(.debug, "Parent node '\(parentNode.label)' will be added to session", category: .node)
                        } else {
                            AppLogger.log(.debug, "Parent node already in session", category: .node)
                        }
                    } else {
                        AppLogger.log(.notice, "Parent node not found in SLD", category: .node)
                    }
                } else {
                    AppLogger.log(.debug, "No parent_id - this is a top-level node", category: .node)
                }
            }

            AppLogger.log(.debug, "Total node IDs to link: \(nodeIdsToLink.count)", category: .node)

            // Add all nodes (selected + parents) to the session with bidirectional relationship
            for node in nodesToAddLocally {
                if !session.nodes.contains(where: { $0.id == node.id }) {
                    session.nodes.append(node)
                    // Also update the inverse relationship explicitly
                    if !node.ir_sessions.contains(where: { $0.id == session.id }) {
                        node.ir_sessions.append(session)
                    }
                    AppLogger.log(.debug, "Added '\(node.label)' to session locally", category: .node)
                }
            }

            // Place no-location assets into this room so they appear in roomAssets.
            // The picker only surfaces nodes already in this room or with no location,
            // so this only mutates the no-location case.
            var nodesRelocated: [NodeV2] = []
            for node in nodesToAddLocally where node.room == nil {
                node.room = room
                node.lastModifiedAt = Date()
                nodesRelocated.append(node)
                AppLogger.log(.debug, "Set room on no-location node '\(node.label)' -> \(room.id)", category: .node)
            }

            try modelContext.save()

            // Sync if online - create mappings for ALL nodes (selected + parents)
            if networkState.mode == .online {
                for nodeId in nodeIdsToLink {
                    do {
                        // Directly call API to create the mapping
                        _ = try await APIClient.shared.createNodeSessionMapping(
                            nodeId: nodeId,
                            sessionId: session.id
                        )
                        AppLogger.log(.debug, "Created mapping for node \(nodeId)", category: .sync)
                    } catch {
                        // If API call fails, queue for later sync
                        networkState.enqueue(SyncOp(
                            target: .mappingNodeSession,
                            operation: .create,
                            mappingData: MappingData.nodeSession(
                                nodeId: nodeId,
                                sessionId: session.id,
                                isDeleted: false
                            )
                        ))
                        AppLogger.log(.error, "Failed to create mapping for \(nodeId), queued: \(error)", category: .sync)
                    }
                }
            } else {
                // Queue all for sync when online
                for nodeId in nodeIdsToLink {
                    networkState.enqueue(SyncOp(
                        target: .mappingNodeSession,
                        operation: .create,
                        mappingData: MappingData.nodeSession(
                            nodeId: nodeId,
                            sessionId: session.id,
                            isDeleted: false
                        )
                    ))
                }
                AppLogger.log(.info, "Offline - queued \(nodeIdsToLink.count) mappings for later sync", category: .sync)
            }

            // Sync node updates for any no-location assets we placed into this room
            for node in nodesRelocated {
                if networkState.mode == .online {
                    do {
                        _ = try await APIClient.shared.updateNode(node)
                        AppLogger.log(.debug, "Synced room update for node \(node.id)", category: .sync)
                    } catch {
                        networkState.enqueue(SyncOp(target: .node, operation: .update, node: node))
                        AppLogger.log(.error, "Failed to sync room update for \(node.id), queued: \(error)", category: .sync)
                    }
                } else {
                    networkState.enqueue(SyncOp(target: .node, operation: .update, node: node))
                }
            }

            AppLogger.log(.info, "Add selected nodes to session - complete", category: .node)

            await MainActor.run {
                addNodeIsUpdating = false
                onComplete()
                dismiss()
            }
        } catch {
            AppLogger.log(.error, "Add selected nodes to session failed: \(error)", category: .node)
            await MainActor.run {
                addNodeIsUpdating = false
                addNodeErrorMessage = "Failed to add assets: \(error.localizedDescription)"
                addNodeShowError = true
            }
        }
    }

    func resetAddNodeState() {
        addNodeSelectedOption = 0
        addNodeSelectedNodes = []
        addNodeSearchText = ""
        addNodeIsUpdating = false
        addNodeShowError = false
        addNodeErrorMessage = ""
        showingAddAsset = false
        addNodeShowQRScanner = false
        showingQuickCount = false
        showingPhotoWalkthrough = false
    }

    // MARK: - New Asset Creation Handler

    func handleNewAssetCreation(
        node: NodeV2,
        photos: [Photo],
        irPhotos: [IRPhoto],
        onComplete: @escaping () -> Void,
        dismiss: DismissAction
    ) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        // CRITICAL: Explicitly set relationships before insertion
        node.room = room
        node.sld = session.sld

        // Insert into model context FIRST
        modelContext.insert(node)
        photos.forEach { modelContext.insert($0) }
        irPhotos.forEach { modelContext.insert($0) }

        // Add node to diagram's nodes array (after insertion)
        if !session.sld.nodes.contains(where: { $0.id == node.id }) {
            session.sld.nodes.append(node)
        }

        // Add node to session
        if !session.nodes.contains(where: { $0.id == node.id }) {
            session.nodes.append(node)
            // Also update the inverse relationship
            if !node.ir_sessions.contains(where: { $0.id == session.id }) {
                node.ir_sessions.append(session)
            }
        }

        // Save context
        do {
            try modelContext.save()
        } catch {
            AppLogger.log(.error, "Failed to save new asset: \(error)", category: .node)
        }

        showingAddAsset = false
        onComplete()
        dismiss()

        // Handle API sync in background
        Task {
            AppLogger.log(.info, "New asset creation started - node: \(node.label) (\(node.id)), session: \(session.name) (\(session.id)), room: \(room.fullPath), mode: \(networkState.mode == .online ? "online" : "offline")", category: .node)

            // Create node on server
            AppLogger.log(.debug, "Step 1: Creating node on server via NodeService", category: .node)
            await NodeService.createNewNodeWithPhotosAndIR(
                node: node,
                photos: photos,
                irPhotos: irPhotos,
                networkState: networkState,
                modelContext: modelContext
            )
            AppLogger.log(.debug, "Node creation completed", category: .node)

            // Create node-session mapping on server
            AppLogger.log(.debug, "Step 2: Creating node-session mapping, node: \(node.id), session: \(session.id)", category: .sync)
            if networkState.mode == .online {
                do {
                    let response = try await APIClient.shared.createNodeSessionMapping(
                        nodeId: node.id,
                        sessionId: session.id
                    )
                    AppLogger.log(.info, "Mapping created successfully: \(response)", category: .sync)
                } catch {
                    AppLogger.log(.error, "Mapping creation failed: \(error.localizedDescription), queueing for later sync", category: .sync)
                    networkState.enqueue(SyncOp(
                        target: .mappingNodeSession,
                        operation: .create,
                        mappingData: MappingData.nodeSession(
                            nodeId: node.id,
                            sessionId: session.id,
                            isDeleted: false
                        )
                    ))
                }
            } else {
                AppLogger.log(.info, "Offline - queueing mapping for later sync", category: .sync)
                networkState.enqueue(SyncOp(
                    target: .mappingNodeSession,
                    operation: .create,
                    mappingData: MappingData.nodeSession(
                        nodeId: node.id,
                        sessionId: session.id,
                        isDeleted: false
                    )
                ))
            }

            AppLogger.log(.info, "New asset creation and linking completed", category: .node)
        }
    }

    // MARK: - Available Node Classes

    func getAvailableNodeClasses() -> [NodeClass] {
        guard let modelContext = modelContext else { return [] }
        let query = FetchDescriptor<NodeClass>(
            predicate: #Predicate { nodeClass in
                !nodeClass.is_deleted
            }
        )
        return (try? modelContext.fetch(query)) ?? []
    }

    // MARK: - Direct Menu Action Handlers

    /// Handles new asset creation from direct menu action (bypasses RoomNodeAdditionView)
    func handleDirectAssetCreation(
        node: NodeV2,
        photos: [Photo],
        irPhotos: [IRPhoto]
    ) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        // CRITICAL: Explicitly set relationships before insertion
        node.room = room
        node.sld = session.sld

        // Insert into model context FIRST
        modelContext.insert(node)
        photos.forEach { modelContext.insert($0) }
        irPhotos.forEach { modelContext.insert($0) }

        // Add node to diagram's nodes array (after insertion)
        if !session.sld.nodes.contains(where: { $0.id == node.id }) {
            session.sld.nodes.append(node)
        }

        // Add node to session
        if !session.nodes.contains(where: { $0.id == node.id }) {
            session.nodes.append(node)
            // Also update the inverse relationship
            if !node.ir_sessions.contains(where: { $0.id == session.id }) {
                node.ir_sessions.append(session)
            }
        }

        // Save context
        do {
            try modelContext.save()
        } catch {
            AppLogger.log(.error, "Failed to save new asset: \(error)", category: .node)
        }

        showingAddAssetDirect = false

        // Handle API sync in background
        Task {
            AppLogger.log(.info, "Direct new asset creation started", category: .node)

            // Create node on server
            await NodeService.createNewNodeWithPhotosAndIR(
                node: node,
                photos: photos,
                irPhotos: irPhotos,
                networkState: networkState,
                modelContext: modelContext
            )

            // Create node-session mapping on server
            if networkState.mode == .online {
                do {
                    _ = try await APIClient.shared.createNodeSessionMapping(
                        nodeId: node.id,
                        sessionId: session.id
                    )
                } catch {
                    networkState.enqueue(SyncOp(
                        target: .mappingNodeSession,
                        operation: .create,
                        mappingData: MappingData.nodeSession(
                            nodeId: node.id,
                            sessionId: session.id,
                            isDeleted: false
                        )
                    ))
                }
            } else {
                networkState.enqueue(SyncOp(
                    target: .mappingNodeSession,
                    operation: .create,
                    mappingData: MappingData.nodeSession(
                        nodeId: node.id,
                        sessionId: session.id,
                        isDeleted: false
                    )
                ))
            }

            AppLogger.log(.info, "Direct new asset creation completed", category: .node)
        }
    }

    // MARK: - Link Tasks to Node

    func linkTasksToNode(_ node: NodeV2, selectedTaskIds: Set<UUID>) {
        guard let modelContext = modelContext, let _ = networkState else { return }

        let currentlyLinkedIds = Set(
            node.node_tasks
                .filter { task in !task.is_deleted && session.user_tasks.contains(where: { ut in ut.id == task.id }) }
                .map(\.id)
        )

        let toAdd = selectedTaskIds.subtracting(currentlyLinkedIds)
        let toRemove = currentlyLinkedIds.subtracting(selectedTaskIds)

        // Add new links
        for taskId in toAdd {
            guard let task = session.user_tasks.first(where: { $0.id == taskId }) else { continue }
            if !node.node_tasks.contains(where: { $0.id == taskId }) {
                node.node_tasks.append(task)
            }
            if !task.linkedNodes.contains(where: { $0.id == node.id }) {
                task.linkedNodes.append(node)
            }
        }

        // Remove old links
        for taskId in toRemove {
            guard let task = session.user_tasks.first(where: { $0.id == taskId }) else { continue }
            node.node_tasks.removeAll { $0.id == taskId }
            task.linkedNodes.removeAll { $0.id == node.id }
        }

        // Save context
        do {
            try modelContext.save()
        } catch {
            AppLogger.log(.error, "Failed to save task-node links: \(error)", category: .node)
        }

        // Sync via API
        Task {
            if !toAdd.isEmpty {
                for taskId in toAdd {
                    do {
                        try await TaskMappingService.shared.addTaskNodes(taskId: taskId, nodeIds: [node.id])
                    } catch {
                        AppLogger.log(.error, "Failed to sync addTaskNodes for task \(taskId): \(error)", category: .sync)
                    }
                }
            }
            if !toRemove.isEmpty {
                for taskId in toRemove {
                    do {
                        try await TaskMappingService.shared.removeTaskNodes(taskId: taskId, nodeIds: [node.id])
                    } catch {
                        AppLogger.log(.error, "Failed to sync removeTaskNodes for task \(taskId): \(error)", category: .sync)
                    }
                }
            }
        }
    }

    // MARK: - Link Listening Tasks to Node

    func linkListeningTasksToNode(_ node: NodeV2, taskIds: Set<UUID>) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        var reopenedTasks: [UserTask] = []

        for taskId in taskIds {
            guard let task = session.user_tasks.first(where: { $0.id == taskId }) else { continue }
            if !node.node_tasks.contains(where: { $0.id == taskId }) {
                node.node_tasks.append(task)
            }
            if !task.linkedNodes.contains(where: { $0.id == node.id }) {
                task.linkedNodes.append(node)
            }
            // New node means new work — reopen completed tasks
            if task.completed {
                task.completed = false
                task.submitted_at = nil
                reopenedTasks.append(task)
            }
            // Mark the new node as incomplete for this task
            task.nodeCompletions[node.id.uuidString] = false
        }

        do {
            try modelContext.save()
        } catch {
            AppLogger.log(.error, "Failed to save listening task links: \(error)", category: .node)
        }

        Task {
            for taskId in taskIds {
                try? await TaskMappingService.shared.addTaskNodes(taskId: taskId, nodeIds: [node.id])
            }
            // Sync reopened tasks
            for task in reopenedTasks {
                if networkState.mode == .online {
                    _ = try? await APIClient.shared.updateTask(task)
                } else {
                    networkState.enqueue(SyncOp(target: .userTask, operation: .update, userTask: task))
                }
            }
        }
    }
}
