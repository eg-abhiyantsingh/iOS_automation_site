//
// AssetsTab.swift
// Egalvanic PZ
//
// Assets tab for managing node-session relationships
//

import SwiftUI
import SwiftData

struct AssetsTab: View {
    let session: IRSession
    @EnvironmentObject var networkState: NetworkState
    @Environment(\.modelContext) private var modelContext
    
    @State private var expandedLocations: Set<String> = []
    @State private var expandedNodes: Set<UUID> = []
    @State private var searchText = ""
    @State private var showingAddNodes = false
    @State private var isUpdating = false
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var showMetrics = true
    
    private var linkedAssets: [NodeV2] {
        session.nodes
            .filter { !$0.is_deleted }
            .sorted(by: { $0.label < $1.label })
    }
    
    // Check if a node matches the search
    private func nodeMatchesSearch(_ node: NodeV2) -> Bool {
        guard !searchText.isEmpty else { return true }
        let searchLower = searchText.lowercased()
        return node.label.lowercased().contains(searchLower) ||
        node.type.lowercased().contains(searchLower) ||
        (node.location?.lowercased().contains(searchLower) ?? false)
    }
    
    // Get all descendants of a node
    private func getAllDescendants(of node: NodeV2, using childrenMap: [UUID: [NodeV2]]) -> [NodeV2] {
        var descendants: [NodeV2] = []
        if let children = childrenMap[node.id] {
            descendants.append(contentsOf: children)
            for child in children {
                descendants.append(contentsOf: getAllDescendants(of: child, using: childrenMap))
            }
        }
        return descendants
    }
    
    // Check if any descendant matches the search
    private func anyDescendantMatchesSearch(of node: NodeV2, using childrenMap: [UUID: [NodeV2]]) -> Bool {
        let descendants = getAllDescendants(of: node, using: childrenMap)
        return descendants.contains { nodeMatchesSearch($0) }
    }
    
    // Count how many nodes each task is linked to in this session
    private var taskNodeCounts: [UUID: Int] {
        var counts: [UUID: Int] = [:]
        for node in linkedAssets {
            for task in node.node_tasks where !task.is_deleted && session.user_tasks.contains(where: { $0.id == task.id }) {
                counts[task.id, default: 0] += 1
            }
        }
        return counts
    }
    
    // Count how many nodes each issue is linked to in this session
    private var issueNodeCounts: [UUID: Int] {
        var counts: [UUID: Int] = [:]
        for node in linkedAssets {
            for issue in node.issues where !issue.is_deleted && session.issues.contains(where: { $0.id == issue.id }) {
                counts[issue.id, default: 0] += 1
            }
        }
        return counts
    }
    
    // Get tasks for a node that are also in this session (excluding multi-node tasks)
    private func getTasksForNode(_ node: NodeV2) -> [UserTask] {
        // Tasks that are linked to both this node AND this session, and only to ONE node
        let counts = taskNodeCounts
        let filteredTasks = node.node_tasks.filter { task in
            !task.is_deleted &&
            session.user_tasks.contains(where: { $0.id == task.id }) &&
            (counts[task.id] ?? 0) == 1 // Only show tasks linked to single node
        }
        return filteredTasks.sorted { $0.title < $1.title }
    }
    
    // Get issues for a node that are also in this session (excluding multi-node issues)
    private func getIssuesForNode(_ node: NodeV2) -> [Issue] {
        // Issues that are linked to both this node AND this session, and only to ONE node
        let counts = issueNodeCounts
        let filteredIssues = node.issues.filter { issue in
            !issue.is_deleted &&
            session.issues.contains(where: { $0.id == issue.id }) &&
            (counts[issue.id] ?? 0) == 1 // Only show issues linked to single node
        }
        // Sort by title, handling optionals
        return filteredIssues.sorted {
            let title1 = $0.title ?? ""
            let title2 = $1.title ?? ""
            return title1 < title2
        }
    }
    
    // Build parent-child relationships
    private func buildNodeHierarchy() -> [(location: String, topLevelAssets: [NodeV2], childrenMap: [UUID: [NodeV2]])] {
        // Create a map of node ID to node for quick lookup
        let nodeMap = Dictionary(uniqueKeysWithValues: linkedAssets.map { ($0.id, $0) })
        
        // Create a map of parent ID to children
        var childrenMap: [UUID: [NodeV2]] = [:]
        var topLevelNodes: [NodeV2] = []
        
        for node in linkedAssets {
            if let parentId = node.parent_id,
               nodeMap[parentId] != nil {
                // This node has a parent in our linked assets
                childrenMap[parentId, default: []].append(node)
            } else {
                // This is a top-level node (no parent or parent not in linked assets)
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
            
            // Note: We'll handle auto-expansion in onChange modifier to avoid state mutation during view update
        }
        
        // Group top-level nodes by location
        let grouped = Dictionary(grouping: topLevelNodes) { node in
            node.location ?? AppStrings.CommonExtra.noLocation
        }
        
        return grouped.map { location, assets in
            (location: location,
             topLevelAssets: assets.sorted { $0.label < $1.label },
             childrenMap: childrenMap)
        }
        .sorted { $0.location < $1.location }
    }
    
    
    private var assetsByLocation: [(location: String, topLevelAssets: [NodeV2], childrenMap: [UUID: [NodeV2]])] {
        buildNodeHierarchy()
    }
    
    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Search bar with filter button
                HStack(spacing: 8) {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)
                        
                        TextField(AppStrings.CommonExtra.searchAssets, text: $searchText)
                            .textFieldStyle(PlainTextFieldStyle())
                        
                        if !searchText.isEmpty {
                            Button(action: {
                                searchText = ""
                            }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding(8)
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(8)
                    
                    // Filter button to toggle metrics
                    Button(action: {
                        showMetrics.toggle()
                    }) {
                        Image(systemName: showMetrics ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                            .font(.title3)
                            .foregroundColor(showMetrics ? .blue : .secondary)
                    }
                }
                .padding(.horizontal)
                .padding(.top, 8)
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if !linkedAssets.isEmpty {
                            // Hierarchical view grouped by location
                            VStack(spacing: 16) {
                                ForEach(assetsByLocation, id: \.location) { locationGroup in
                                    AssetLocationSection(
                                        session: session,
                                        location: locationGroup.location,
                                        topLevelAssets: locationGroup.topLevelAssets,
                                        childrenMap: locationGroup.childrenMap,
                                        isExpanded: expandedLocations.contains(locationGroup.location),
                                        expandedNodes: $expandedNodes,
                                        searchText: searchText,
                                        showMetrics: showMetrics,
                                        onToggleExpand: {
                                            if expandedLocations.contains(locationGroup.location) {
                                                expandedLocations.remove(locationGroup.location)
                                            } else {
                                                expandedLocations.insert(locationGroup.location)
                                            }
                                        },
                                        onRemoveNode: removeNode
                                    )
                                }
                            }
                            .padding(.horizontal)
                        } else {
                            ContentUnavailableView(
                                AppStrings.Sessions.noAssets,
                                systemImage: "cube.box",
                                description: Text(AppStrings.Sessions.tapPlusToLinkAssets)
                            )
                            .frame(minHeight: 400)
                        }
                    }
                    .padding(.vertical)
                    .padding(.bottom, 80) // Space for floating button
                }
                .scrollDismissesKeyboard(.interactively)
            }

            // Floating Plus Button - disabled if session is not active
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Button(action: {
                        AppLogger.log(.info, "FLOATING + BUTTON TAPPED in AssetsTab! session.active=\(session.active), showingAddNodes will be set to true", category: .node)
                        showingAddNodes = true
                    }) {
                        Image(systemName: "plus")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(session.active ? Color.blue : Color.gray)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                    }
                    .disabled(!session.active)
                    .padding(.trailing, 20)
                    .padding(.bottom, 20)
                }
            }
        }
        .fullScreenCover(isPresented: $showingAddNodes) {
            NodeAdditionView(
                session: session,
                onComplete: {
                    // Refresh will happen automatically through SwiftData
                }
            )
            .environmentObject(networkState)
            .environmentObject(AppStateManager.shared)
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(errorMessage)
        }
        .overlay {
            if isUpdating {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.CommonExtra.updating)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
    }
    
    private func removeNode(_ node: NodeV2) {
        Task {
            await MainActor.run { isUpdating = true }
            
            do {
                // Remove from session with bidirectional relationship
                session.nodes.removeAll { $0.id == node.id }
                // Also update the inverse relationship explicitly
                node.ir_sessions.removeAll { $0.id == session.id }
                
                try modelContext.save()
                
                // Sync mapping deletion
                if networkState.mode == .online {
                    do {
                        // Directly call API to update the mapping to mark as deleted
                        _ = try await APIClient.shared.updateNodeSessionMapping(
                            nodeId: node.id,
                            sessionId: session.id,
                            isDeleted: true
                        )
                    } catch {
                        // If API call fails, queue for later sync
                        networkState.enqueue(SyncOp(
                            target: .mappingNodeSession,
                            operation: .delete,
                            mappingData: MappingData.nodeSession(
                                nodeId: node.id,
                                sessionId: session.id,
                                isDeleted: true
                            )
                        ))
                        AppLogger.log(.error, "Failed to update node-session mapping, queued for later: \(error)", category: .sync)
                    }
                } else {
                    // Queue for sync when online
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
}

// MARK: - Location Section
struct AssetLocationSection: View {
    let session: IRSession
    let location: String
    let topLevelAssets: [NodeV2]
    let childrenMap: [UUID: [NodeV2]]
    let isExpanded: Bool
    @Binding var expandedNodes: Set<UUID>
    var searchText: String = ""
    var showMetrics: Bool = true
    let onToggleExpand: () -> Void
    let onRemoveNode: (NodeV2) -> Void
    
    private var totalAssetCount: Int {
        topLevelAssets.count
    }
    
    // Count how many top-level assets have at least one IR photo in this session
    private var locationIRPhotoCount: Int {
        return topLevelAssets.filter { asset in
            asset.ir_photos.contains { irPhoto in
                !irPhoto.is_deleted && irPhoto.ir_session?.id == session.id
            }
        }.count
    }
    
    // Total top-level assets for the fraction display
    private var totalTopLevelAssets: Int {
        return topLevelAssets.count
    }
    
    // Count how many nodes each task is linked to in this session
    private var taskNodeCounts: [UUID: Int] {
        var counts: [UUID: Int] = [:]
        for node in session.nodes where !node.is_deleted {
            for task in node.node_tasks where !task.is_deleted && session.user_tasks.contains(where: { $0.id == task.id }) {
                counts[task.id, default: 0] += 1
            }
        }
        return counts
    }
    
    // Count how many top-level assets have at least one single-node task in this session
    private var locationTaskCount: Int {
        let counts = taskNodeCounts
        return topLevelAssets.filter { asset in
            asset.node_tasks.contains { task in
                !task.is_deleted &&
                session.user_tasks.contains(where: { $0.id == task.id }) &&
                (counts[task.id] ?? 0) == 1 // Only count single-node tasks
            }
        }.count
    }
    
    // Total single-node tasks linked to top-level assets in this session
    private var totalLocationTasks: Int {
        let counts = taskNodeCounts
        return topLevelAssets.reduce(0) { count, asset in
            count + asset.node_tasks.filter { task in
                !task.is_deleted &&
                session.user_tasks.contains(where: { $0.id == task.id }) &&
                (counts[task.id] ?? 0) == 1 // Only count single-node tasks
            }.count
        }
    }
    
    // Count how many single-node tasks are complete
    private var completedLocationTasks: Int {
        let counts = taskNodeCounts
        return topLevelAssets.reduce(0) { count, asset in
            count + asset.node_tasks.filter { task in
                !task.is_deleted &&
                session.user_tasks.contains(where: { $0.id == task.id }) &&
                (counts[task.id] ?? 0) == 1 && // Only count single-node tasks
                task.completed
            }.count
        }
    }
    
    // Check if all tasks are complete for this location
    private var allTasksComplete: Bool {
        return totalLocationTasks > 0 && completedLocationTasks == totalLocationTasks
    }
    
    // Check if all IR photos are captured for this location
    private var allIRPhotosComplete: Bool {
        return locationIRPhotoCount == totalTopLevelAssets && totalTopLevelAssets > 0
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Location Header with background
            Button(action: onToggleExpand) {
                HStack {
                    HStack(spacing: 0) {
                        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .frame(width: 20)
                        
                        Image(systemName: "location.fill")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.leading, 4)
                        
                        Text(location)
                            .font(.headline)
                            .foregroundColor(.primary)
                            .padding(.leading, 4)
                    }
                    
                    Spacer()
                    
                    HStack(spacing: 8) {
                        if showMetrics {
                            // IR Photos indicator - always show as fraction with color coding
                            HStack(spacing: 2) {
                                Image(systemName: "camera.filters")
                                    .font(.caption)
                                    .foregroundColor(allIRPhotosComplete ? .green : .red)
                                Text("\(locationIRPhotoCount)/\(totalTopLevelAssets)")
                                    .font(.caption)
                                    .foregroundColor(allIRPhotosComplete ? .green : .red)
                            }
                            
                            // Tasks indicator - show completed/total tasks with color coding
                            if totalLocationTasks > 0 {
                                HStack(spacing: 2) {
                                    Image(systemName: "checklist")
                                        .font(.caption)
                                        .foregroundColor(allTasksComplete ? .green : .red)
                                    Text("\(completedLocationTasks)/\(totalLocationTasks)")
                                        .font(.caption)
                                        .foregroundColor(allTasksComplete ? .green : .red)
                                }
                            }
                            
                            Text("•")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Text(AppStrings.Sessions.assetsCount(totalAssetCount))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.vertical, 10)
                .padding(.horizontal, 12)
                .background(Color(UIColor.systemGray5))
                .cornerRadius(10)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            
            if isExpanded {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(topLevelAssets) { asset in
                        VStack(alignment: .leading, spacing: 0) {
                            AssetRowWithChildren(
                                session: session,
                                asset: asset,
                                children: childrenMap[asset.id] ?? [],
                                isExpanded: expandedNodes.contains(asset.id),
                                expandedNodes: $expandedNodes,
                                childrenMap: childrenMap,
                                searchText: searchText,
                                onRemoveNode: onRemoveNode
                            )
                        }
                    }
                }
                .padding(.leading, 24)
            }
        }
    }
}

// MARK: - Asset Row with Children Support
struct AssetRowWithChildren: View {
    let session: IRSession
    let asset: NodeV2
    let children: [NodeV2]
    let isExpanded: Bool
    @Binding var expandedNodes: Set<UUID>
    let childrenMap: [UUID: [NodeV2]]
    var searchText: String = ""
    let onRemoveNode: (NodeV2) -> Void
    
    @State private var showingRemoveConfirmation = false
    @State private var showingAddTask = false
    @State private var showingAddIssue = false
    @State private var showingEditNode = false
    @State private var selectedTask: UserTask?
    @State private var selectedIssue: Issue?
    @State private var focusMode: NodeDetailFocusMode = .all
    
    private var isDirectMatch: Bool {
        guard !searchText.isEmpty else { return false }
        let searchLower = searchText.lowercased()
        return asset.label.lowercased().contains(searchLower) ||
        asset.type.lowercased().contains(searchLower)
    }
    
    // Count IR photos linked to this node
    private var nodeIRPhotoCount: Int {
        asset.ir_photos.filter { !$0.is_deleted }.count
    }
    
    // Count IR photos linked to this node that are also in the current session
    private var sessionIRPhotoCount: Int {
        asset.ir_photos.filter { irPhoto in
            !irPhoto.is_deleted && irPhoto.ir_session?.id == session.id
        }.count
    }
    
    // Count how many nodes each task is linked to in this session
    private var taskNodeCounts: [UUID: Int] {
        var counts: [UUID: Int] = [:]
        for node in session.nodes where !node.is_deleted {
            for task in node.node_tasks where !task.is_deleted && session.user_tasks.contains(where: { $0.id == task.id }) {
                counts[task.id, default: 0] += 1
            }
        }
        return counts
    }
    
    // Count how many nodes each issue is linked to in this session
    private var issueNodeCounts: [UUID: Int] {
        var counts: [UUID: Int] = [:]
        for node in session.nodes where !node.is_deleted {
            for issue in node.issues where !issue.is_deleted && session.issues.contains(where: { $0.id == issue.id }) {
                counts[issue.id, default: 0] += 1
            }
        }
        return counts
    }
    
    private var nodeTasks: [UserTask] {
        // Tasks that are linked to both this node AND this session, and only to ONE node
        let counts = taskNodeCounts
        let filtered = asset.node_tasks.filter { task in
            !task.is_deleted &&
            session.user_tasks.contains(where: { $0.id == task.id }) &&
            (counts[task.id] ?? 0) == 1 // Only show tasks linked to single node
        }
        return filtered.sorted { $0.title < $1.title }
    }
    
    private var nodeIssues: [Issue] {
        // Issues that are linked to both this node AND this session, and only to ONE node
        let counts = issueNodeCounts
        let filtered = asset.issues.filter { issue in
            !issue.is_deleted &&
            session.issues.contains(where: { $0.id == issue.id }) &&
            (counts[issue.id] ?? 0) == 1 // Only show issues linked to single node
        }
        return filtered.sorted {
            let title1 = $0.title ?? ""
            let title2 = $1.title ?? ""
            return title1 < title2
        }
    }
    
    private var hasExpandableContent: Bool {
        !children.isEmpty || !nodeTasks.isEmpty || !nodeIssues.isEmpty
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Parent asset row
            HStack(spacing: 0) {
                // Expand/collapse button with larger hit area
                Button(action: {
                    if hasExpandableContent {
                        if expandedNodes.contains(asset.id) {
                            expandedNodes.remove(asset.id)
                        } else {
                            expandedNodes.insert(asset.id)
                        }
                    }
                }) {
                    if hasExpandableContent {
                        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(isExpanded ? .blue : .secondary)
                            .frame(width: 44, height: 44) // Large tap area
                            .contentShape(Rectangle())
                    } else {
                        Color.clear
                            .frame(width: 44, height: 44)
                    }
                }
                .buttonStyle(.plain)
                
                // Main content area - tappable to open detail view
                Button(action: {
                    focusMode = .all
                    showingEditNode = true
                }) {
                    HStack(spacing: 0) {
                        // Asset icon based on node class style
                        NodeTypeIcon(
                            style: asset.node_class?.style,
                            size: 18,
                            color: isExpanded ? .blue : .secondary
                        )
                        .padding(.leading, 4)
                        .padding(.trailing, 8)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text(asset.label)
                                .font(.subheadline)
                                .fontWeight(isExpanded ? .semibold : .medium)
                                .foregroundColor(isExpanded ? .blue : (isDirectMatch ? .blue : .primary))
                                .background(
                                    isDirectMatch ? Color.blue.opacity(0.1) : Color.clear
                                )
                            
                            if asset.node_class?.name != nil || hasExpandableContent || sessionIRPhotoCount > 0 {
                                HStack(spacing: 6) {
                                    if let nodeClassName = asset.node_class?.name {
                                        Text(nodeClassName)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                    
                                    if asset.node_class?.name != nil && (hasExpandableContent || sessionIRPhotoCount > 0) {
                                        Text("•")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                    
                                    // IR Photo indicator - green checkmark if has session IR photos
                                    if sessionIRPhotoCount > 0 {
                                        Image(systemName: "checkmark.circle.fill")
                                            .font(.caption)
                                            .foregroundColor(.green)
                                    }
                                    
                                    if hasExpandableContent {
                                        HStack(spacing: 6) {
                                            if sessionIRPhotoCount > 0 {
                                                Text("•")
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                            }
                                            
                                            if !nodeTasks.isEmpty {
                                                Label("\(nodeTasks.count)", systemImage: "checklist")
                                                    .font(.caption)
                                                    .foregroundColor(.blue)
                                            }
                                            
                                            if !nodeIssues.isEmpty {
                                                Label("\(nodeIssues.count)", systemImage: "exclamationmark.triangle")
                                                    .font(.caption)
                                                    .foregroundColor(.orange)
                                            }
                                            
                                            if !children.isEmpty {
                                                Label("\(children.count)", systemImage: "cube")
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer()
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            .padding(.vertical, 6)
            .background(Color(UIColor.systemBackground))
            .overlay(
                Rectangle()
                    .frame(height: 1)
                    .foregroundColor(Color(UIColor.separator).opacity(0.3)),
                alignment: .bottom
            )
            .contextMenu {
                Button {
                    focusMode = .coreAttributes
                    showingEditNode = true
                } label: {
                    Label(AppStrings.Sessions.collectData, systemImage: "list.bullet.clipboard")
                }
                
                Button {
                    focusMode = .tasks
                    showingEditNode = true
                } label: {
                    Label(AppStrings.Sessions.addTask, systemImage: "checklist")
                }
                
                Button {
                    focusMode = .irPhotos
                    showingEditNode = true
                } label: {
                    Label(AppStrings.Sessions.addIRPhotos, systemImage: "camera.filters")
                }
                
                Button {
                    focusMode = .issues
                    showingEditNode = true
                } label: {
                    Label(AppStrings.Sessions.addIssue, systemImage: "exclamationmark.triangle")
                }
                
                Divider()
                
                Button(role: .destructive) {
                    showingRemoveConfirmation = true
                } label: {
                    Label(AppStrings.Sessions.removeFromSession, systemImage: "minus.circle")
                }
            }
            
            // Expanded content: Tasks, Issues, then Children
            if isExpanded && hasExpandableContent {
                VStack(alignment: .leading, spacing: 0) {
                    // Tasks for this node
                    ForEach(nodeTasks) { task in
                        TaskRow(task: task, searchText: searchText)
                            .padding(.leading, 24)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                selectedTask = task
                            }
                    }
                    
                    // Issues for this node
                    ForEach(nodeIssues) { issue in
                        IssueRow(issue: issue, searchText: searchText)
                            .padding(.leading, 24)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                selectedIssue = issue
                            }
                    }
                    
                    // Child nodes (recursively)
                    ForEach(children) { child in
                        AssetRowWithChildren(
                            session: session,
                            asset: child,
                            children: childrenMap[child.id] ?? [],
                            isExpanded: expandedNodes.contains(child.id),
                            expandedNodes: $expandedNodes,
                            childrenMap: childrenMap,
                            searchText: searchText,
                            onRemoveNode: onRemoveNode
                        )
                        .padding(.leading, 24)
                    }
                }
            }
        }
        .confirmationDialog(
            AppStrings.Sessions.removeAssetConfirm,
            isPresented: $showingRemoveConfirmation,
            titleVisibility: .visible
        ) {
            Button(AppStrings.CommonExtra.remove, role: .destructive) {
                onRemoveNode(asset)
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        } message: {
            Text(AppStrings.Sessions.removeAssetMessage)
        }
        .fullScreenCover(isPresented: $showingEditNode) {
            EditNodeDetailViewV3(
                node: asset,
                sld: session.sld,
                onDismiss: { showingEditNode = false },
                focusMode: focusMode
            )
        }
        .onChange(of: showingEditNode) { isShowing in
            if !isShowing {
                // Reset focus mode when sheet is dismissed
                focusMode = .all
            }
        }
        .fullScreenCover(item: $selectedTask) { task in
            NavigationView {
                TaskDetailView(
                    task: task,
                    showNodesSection: true,
                    hideNavigationBar: true,
                    onDismiss: { selectedTask = nil }
                )
                .navigationBarItems(
                    leading: Button(action: {
                        selectedTask = nil
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text(AppStrings.CommonExtra.close)
                        }
                    }
                )
            }
        }
        .fullScreenCover(item: $selectedIssue) { issue in
            NavigationView {
                IssueDetailView(
                    issue: issue,
                    hideNavigationBar: true,
                    onDismiss: { selectedIssue = nil }
                )
                .navigationBarItems(
                    leading: Button(action: {
                        selectedIssue = nil
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text(AppStrings.CommonExtra.close)
                        }
                    }
                )
            }
        }
    }
}

// MARK: - Task Row
struct TaskRow: View {
    let task: UserTask
    var searchText: String = ""
    var nodeId: UUID? = nil
    var nodeCount: Int = 1
    
    private var isMatch: Bool {
        guard !searchText.isEmpty else { return false }
        let searchLower = searchText.lowercased()
        return task.title.lowercased().contains(searchLower) ||
        task.task_description.lowercased().contains(searchLower)
    }
    
    private var isComplete: Bool {
        if let nodeId = nodeId {
            return task.nodeCompletions[nodeId.uuidString] ?? false
        }
        return task.completed
    }
    
    var body: some View {
        HStack(spacing: 0) {
            // Align with asset row chevron (44pt width)
            Color.clear.frame(width: 44)
            
            Image(systemName: "list.clipboard.fill")
                .font(.body)
                .foregroundColor(isComplete ? .green : .red)
                .padding(.leading, 4)
                .padding(.trailing, 8)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(task.title)
                    .font(.subheadline)
                    .foregroundColor(isComplete ? .green : (isMatch ? .blue : .primary))
                    .background(isMatch ? Color.blue.opacity(0.1) : Color.clear)
                    .strikethrough(isComplete)
                
                HStack(spacing: 4) {
                    if !task.task_description.isEmpty {
                        Text(task.task_description)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                    
                    if nodeCount > 1 {
                        Text(AppStrings.Sessions.assetsCount(nodeCount))
                            .font(.caption2)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 1)
                            .background(Color.blue.opacity(0.15))
                            .foregroundColor(.blue)
                            .cornerRadius(4)
                    }
                }
            }
            
            Spacer()
        }
        .padding(.vertical, 6)
        .background(Color(UIColor.systemBackground))
        .overlay(
            Rectangle()
                .frame(height: 1)
                .foregroundColor(Color(UIColor.separator).opacity(0.3)),
            alignment: .bottom
        )
    }
}

// MARK: - Issue Row
struct IssueRow: View {
    let issue: Issue
    var searchText: String = ""
    var nodeCount: Int = 1
    
    private var isMatch: Bool {
        guard !searchText.isEmpty else { return false }
        let searchLower = searchText.lowercased()
        return (issue.title?.lowercased().contains(searchLower) ?? false) ||
        (issue.issueDescription?.lowercased().contains(searchLower) ?? false)
    }
    
    private var isResolved: Bool {
        guard let status = issue.status else { return false }
        return status.lowercased() == "resolved" || status.lowercased() == "closed" || status.lowercased() == "complete"
    }
    
    private var issueColor: Color {
        if isResolved {
            return .green
        } else {
            return .blue
        }
    }
    
    var body: some View {
        HStack(spacing: 0) {
            // Align with asset row chevron (44pt width)
            Color.clear.frame(width: 44)
            
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.body)
                .foregroundColor(issueColor)
                .padding(.leading, 4)
                .padding(.trailing, 8)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(issue.title ?? AppStrings.CommonExtra.untitledIssue)
                    .font(.subheadline)
                    .foregroundColor(isResolved ? .green : (isMatch ? .blue : .primary))
                    .background(isMatch ? Color.blue.opacity(0.1) : Color.clear)
                    .strikethrough(isResolved)
                
                HStack(spacing: 4) {
                    if let status = issue.status, !status.isEmpty {
                        Text(LanguageManager.localizedStatus(status))
                            .font(.caption2)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(issueColor.opacity(0.2))
                            .foregroundColor(issueColor)
                            .cornerRadius(4)
                    }
                    
                    if nodeCount > 1 {
                        Text(AppStrings.Sessions.assetsCount(nodeCount))
                            .font(.caption2)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 1)
                            .background(Color.blue.opacity(0.15))
                            .foregroundColor(.blue)
                            .cornerRadius(4)
                    }
                    
                    if let description = issue.issueDescription, !description.isEmpty {
                        Text(description)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                }
            }
            
            Spacer()
        }
        .padding(.vertical, 6)
        .background(Color(UIColor.systemBackground))
        .overlay(
            Rectangle()
                .frame(height: 1)
                .foregroundColor(Color(UIColor.separator).opacity(0.3)),
            alignment: .bottom
        )
    }
}
// MARK: - Node Addition View
struct NodeAdditionView: View {
    let session: IRSession
    let onComplete: () -> Void
    
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    
    @State private var selectedOption = 0 // 0 = Existing Asset, 1 = New Asset
    @State private var selectedNodes = Set<NodeV2>()
    @State private var searchText = ""
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var isUpdating = false
    @State private var showingAddAsset = false
    
    // Query all nodes in the same SLD that aren't already linked
    @Query private var availableNodes: [NodeV2]
    
    init(session: IRSession, onComplete: @escaping () -> Void) {
        self.session = session
        self.onComplete = onComplete
        
        let sldId = session.sld.id
        let sessionId = session.id
        
        _availableNodes = Query(
            filter: #Predicate<NodeV2> { node in
                node.sld?.id == sldId &&
                !node.is_deleted
            },
            sort: [SortDescriptor(\NodeV2.label)]
        )
    }
    
    private var filteredNodes: [NodeV2] {
        // Filter out nodes that are already linked to this session
        let unlinkedNodes = availableNodes.filter { node in
            !session.nodes.contains(where: { $0.id == node.id })
        }
        
        // Apply search filter
        if searchText.isEmpty {
            return unlinkedNodes
        } else {
            return unlinkedNodes.filter { node in
                node.label.localizedCaseInsensitiveContains(searchText) ||
                node.type.localizedCaseInsensitiveContains(searchText) ||
                (node.room?.fullPath.localizedCaseInsensitiveContains(searchText) ?? false)
            }
        }
    }
    
    // Group nodes by location (using room relationship)
    private var nodesByLocation: [(location: String, nodes: [NodeV2])] {
        let grouped = Dictionary(grouping: filteredNodes) { node in
            node.room?.fullPath ?? AppStrings.CommonExtra.noLocation
        }
        
        return grouped.map { location, nodes in
            (location: location, nodes: nodes.sorted { $0.label < $1.label })
        }
        .sorted { loc1, loc2 in
            // "No Location" should appear first
            switch (loc1.location == AppStrings.CommonExtra.noLocation, loc2.location == AppStrings.CommonExtra.noLocation) {
            case (true, true): return false // Both "No Location", equal
            case (true, false): return true // loc1 first
            case (false, true): return false // loc2 first
            case (false, false): return loc1.location < loc2.location
            }
        }
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Option Selector
                Picker("", selection: $selectedOption) {
                    Text(AppStrings.CommonExtra.existingAsset).tag(0)
                    Text(AppStrings.Assets.newAsset).tag(1)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)
                .padding(.top)
                .padding(.bottom, 16)
                
                // Search bar for existing assets - outside the white area
                if selectedOption == 0 {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)
                        
                        TextField(AppStrings.CommonExtra.searchAssets, text: $searchText)
                            .textFieldStyle(PlainTextFieldStyle())
                        
                        if !searchText.isEmpty {
                            Button(action: {
                                searchText = ""
                            }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding(8)
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(8)
                    .padding(.horizontal)
                    .padding(.bottom, 16)
                }
                
                if selectedOption == 0 {
                    // Existing Asset Option
                    VStack {
                        if filteredNodes.isEmpty {
                            ContentUnavailableView(
                                AppStrings.Sessions.noAvailableAssets,
                                systemImage: "cube.box",
                                description: Text(AppStrings.Sessions.allAssetsLinkedToSession)
                            )
                            .frame(maxHeight: .infinity)
                        } else {
                            List {
                                ForEach(nodesByLocation, id: \.location) { locationGroup in
                                    Section(header: HStack {
                                        Image(systemName: "location.fill")
                                            .font(.caption)
                                        Text(locationGroup.location)
                                        Spacer()
                                        Text("\(locationGroup.nodes.count)")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }) {
                                        ForEach(locationGroup.nodes) { node in
                                            NodeSelectionRow(
                                                node: node,
                                                isSelected: selectedNodes.contains(node),
                                                onToggle: {
                                                    AppLogger.log(.info, "Node toggled: \(node.label) (id: \(node.id), parent_id: \(node.parent_id?.uuidString ?? "NIL"))", category: .ui)
                                                    if selectedNodes.contains(node) {
                                                        selectedNodes.remove(node)
                                                        AppLogger.log(.info, "➖ Removed from selection", category: .ui)
                                                    } else {
                                                        selectedNodes.insert(node)
                                                        AppLogger.log(.info, "➕ Added to selection (total: \(selectedNodes.count))", category: .ui)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // New Asset Option
                    VStack(spacing: 16) {
                        Text(AppStrings.Sessions.createAndLinkAsset)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal)
                        
                        Button(action: {
                            showingAddAsset = true
                        }) {
                            HStack {
                                Image(systemName: "plus.circle.fill")
                                    .font(.title2)
                                    .foregroundColor(.blue)
                                Text(AppStrings.CommonExtra.createNewAsset)
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                Spacer()
                            }
                            .padding()
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(10)
                        }
                        .padding(.horizontal)
                        
                        Spacer()
                    }
                }
            }
            .navigationTitle(AppStrings.Sessions.addAssets)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    if selectedOption == 0 && !selectedNodes.isEmpty {
                        Button(AppStrings.CommonExtra.addCount(selectedNodes.count)) {
                            AppLogger.log(.info, "ADD BUTTON TAPPED! selectedNodes.count = \(selectedNodes.count)", category: .ui)
                            Task {
                                await addSelectedNodes()
                            }
                        }
                        .fontWeight(.semibold)
                    }
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
        .onAppear {
            AppLogger.log(.info, "NodeAdditionView APPEARED - session: \(session.name), availableNodes: \(availableNodes.count), filteredNodes: \(filteredNodes.count)", category: .node)
        }
        .fullScreenCover(isPresented: $showingAddAsset) {
            AddAssetViewV2(
                availableLocations: Array(Set(availableNodes.compactMap(\.location))).sorted(),
                availableNodeClasses: {
                    let query = FetchDescriptor<NodeClass>(
                        predicate: #Predicate { nodeClass in
                            !nodeClass.is_deleted
                        }
                    )
                    return (try? modelContext.fetch(query)) ?? []
                }(),
                sld: session.sld,
                onSave: { node, photos, irPhotos in
                    AppLogger.log(.debug, "[AssetsTab onSave] Starting...", category: .database)
                    AppLogger.log(.info, "Node ID: \(node.id)", category: .node)
                    AppLogger.log(.info, "Node.sld BEFORE fix: \(node.sld?.id.uuidString ?? "nil")", category: .node)
                    AppLogger.log(.info, "Session.sld ID: \(session.sld.id)", category: .node)
                    
                    // CRITICAL: Explicitly set the SLD relationship before insertion
                    // This ensures SwiftData properly persists the relationship
                    node.sld = session.sld
                    AppLogger.log(.info, "Node.sld AFTER fix: \(node.sld?.id.uuidString ?? "nil")", category: .node)
                    
                    // Insert into model context FIRST
                    modelContext.insert(node)
                    AppLogger.log(.info, "Node inserted into modelContext", category: .database)
                    
                    // NOW assign photos to node (after node is inserted)
                    photos.forEach { photo in
                        photo.node = node
                        modelContext.insert(photo)
                    }
                    AppLogger.log(.info, "\(photos.count) photos inserted and linked to node", category: .photo)
                    
                    irPhotos.forEach { irPhoto in
                        irPhoto.node = node
                        modelContext.insert(irPhoto)
                    }
                    AppLogger.log(.info, "\(irPhotos.count) IR photos inserted and linked to node", category: .photo)
                    
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
                        AppLogger.log(.error, "Failed to save new asset: \(error)", category: .database)
                    }
                    
                    showingAddAsset = false
                    onComplete()
                    dismiss()
                    
                    // Handle API sync in background
                    Task {
                        AppLogger.log(.info, "\n=== NEW ASSET CREATION AND LINKING STARTED ===", category: .node)
                        AppLogger.log(.info, "Node ID: \(node.id)", category: .node)
                        AppLogger.log(.info, "Node Label: \(node.label)", category: .node)
                        AppLogger.log(.info, "Session ID: \(session.id)", category: .node)
                        AppLogger.log(.info, "Session Name: \(session.name)", category: .node)
                        AppLogger.log(.info, "Network Mode: \(networkState.mode == .online ? "ONLINE" : "OFFLINE")", category: .network)
                        
                        // Create node on server
                        AppLogger.log(.debug, "\nStep 1: Creating node on server via NodeService...", category: .node)
                        await NodeService.createNewNodeWithPhotosAndIR(
                            node: node,
                            photos: photos,
                            irPhotos: irPhotos,
                            networkState: networkState,
                            modelContext: modelContext
                        )
                        AppLogger.log(.info, "Node creation completed (check NodeService logs for details)", category: .node)
                        
                        // Create node-session mapping on server
                        AppLogger.log(.debug, "\nStep 2: Creating node-session mapping...", category: .sync)
                        if networkState.mode == .online {
                            AppLogger.log(.info, "Network is ONLINE - attempting direct API call", category: .network)
                            do {
                                AppLogger.log(.info, "Calling createNodeSessionMapping...", category: .api)
                                AppLogger.log(.info, "Node ID: \(node.id.uuidString)", category: .node)
                                AppLogger.log(.info, "Session ID: \(session.id.uuidString)", category: .node)
                                
                                let response = try await APIClient.shared.createNodeSessionMapping(
                                    nodeId: node.id,
                                    sessionId: session.id
                                )
                                
                                AppLogger.log(.info, "Mapping created successfully!", category: .node)
                                AppLogger.log(.info, "Response: \(response)", category: .node)
                                
                            } catch {
                                AppLogger.log(.error, "Mapping creation FAILED!", category: .node)
                                AppLogger.log(.info, "Error type: \(type(of: error))", category: .node)
                                AppLogger.log(.info, "Error description: \(error.localizedDescription)", category: .node)
                                AppLogger.log(.error, "Full error: \(error)", category: .node)
                                
                                // Queue for later if failed
                                AppLogger.log(.info, "Queueing mapping for later sync...", category: .sync)
                                networkState.enqueue(SyncOp(
                                    target: .mappingNodeSession,
                                    operation: .create,
                                    mappingData: MappingData.nodeSession(
                                        nodeId: node.id,
                                        sessionId: session.id,
                                        isDeleted: false
                                    )
                                ))
                                AppLogger.log(.info, "Mapping queued in sync queue", category: .sync)
                            }
                        } else {
                            AppLogger.log(.info, "Network is OFFLINE - queueing for later", category: .sync)
                            // Queue for sync when online
                            networkState.enqueue(SyncOp(
                                target: .mappingNodeSession,
                                operation: .create,
                                mappingData: MappingData.nodeSession(
                                    nodeId: node.id,
                                    sessionId: session.id,
                                    isDeleted: false
                                )
                            ))
                            AppLogger.log(.info, "Mapping queued in sync queue (offline mode)", category: .sync)
                        }
                        
                        AppLogger.log(.info, "\n=== NEW ASSET CREATION AND LINKING COMPLETED ===\n", category: .node)
                    }
                },
                onCancel: {
                    showingAddAsset = false
                }
            )
            .environmentObject(appState)
            .environmentObject(networkState)
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(errorMessage)
        }
        .overlay {
            if isUpdating {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.Sessions.addingAssets)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
    }
    
    private func addSelectedNodes() async {
        await MainActor.run { isUpdating = true }
        
        AppLogger.log(.info, "ADD SELECTED NODES - START", category: .node)
        AppLogger.log(.info, "Selected nodes count: \(selectedNodes.count)", category: .ui)
        AppLogger.log(.info, "Session ID: \(session.id)", category: .node)
        AppLogger.log(.info, "Session nodes before: \(session.nodes.count)", category: .node)
        
        do {
            // Collect node IDs to link: selected nodes + their parents (if any)
            var nodeIdsToLink: Set<UUID> = []
            
            for node in selectedNodes {
                AppLogger.log(.info, "Processing node: \(node.label)", category: .node)
                AppLogger.log(.info, "Node ID: \(node.id)", category: .node)
                AppLogger.log(.info, "Node type: \(node.type)", category: .node)
                AppLogger.log(.info, "Node parent_id: \(node.parent_id?.uuidString ?? "NIL")", category: .node)
                
                nodeIdsToLink.insert(node.id)
                
                // If node has a parent, also link the parent
                if let parentId = node.parent_id {
                    nodeIdsToLink.insert(parentId)
                    AppLogger.log(.info, "Parent ID found! Adding \(parentId) to nodeIdsToLink", category: .node)
                } else {
                    AppLogger.log(.notice, "No parent_id - this node has no parent", category: .node)
                }
            }
            
            AppLogger.log(.info, "nodeIdsToLink set contains \(nodeIdsToLink.count) IDs:", category: .node)
            for id in nodeIdsToLink {
                AppLogger.log(.info, "\(id)", category: .node)
            }
            
            // Add nodes to session locally
            for node in selectedNodes {
                if !session.nodes.contains(where: { $0.id == node.id }) {
                    session.nodes.append(node)
                    if !node.ir_sessions.contains(where: { $0.id == session.id }) {
                        node.ir_sessions.append(session)
                    }
                }
            }
            
            // Also add parent nodes to session locally
            let sld = session.sld
            for node in selectedNodes {
                if let parentId = node.parent_id,
                   let parentNode = sld.nodes.first(where: { $0.id == parentId && !$0.is_deleted }) {
                    if !session.nodes.contains(where: { $0.id == parentNode.id }) {
                        session.nodes.append(parentNode)
                        if !parentNode.ir_sessions.contains(where: { $0.id == session.id }) {
                            parentNode.ir_sessions.append(session)
                        }
                        AppLogger.log(.info, "Added parent \(parentNode.label) to session locally", category: .node)
                    }
                }
            }
            
            try modelContext.save()
            
            // Sync mappings for ALL node IDs (selected + parents)
            if networkState.mode == .online {
                for nodeId in nodeIdsToLink {
                    do {
                        _ = try await APIClient.shared.createNodeSessionMapping(
                            nodeId: nodeId,
                            sessionId: session.id
                        )
                        AppLogger.log(.info, "Created mapping for node \(nodeId)", category: .sync)
                    } catch {
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
            }
            
            AppLogger.log(.info, "Session nodes after: \(session.nodes.count)", category: .node)
            AppLogger.log(.info, "ADD SELECTED NODES - COMPLETE", category: .node)
            
            await MainActor.run {
                isUpdating = false
                onComplete()
                dismiss()
            }
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            AppLogger.log(.error, "ADD SELECTED NODES - ERROR: \(error)", category: .node)
            await MainActor.run {
                isUpdating = false
                errorMessage = "Failed to add assets: \(error.localizedDescription)"
                showError = true
            }
        }
    }
}

// MARK: - Node Selection Row
struct NodeSelectionRow: View {
    let node: NodeV2
    let isSelected: Bool
    var showLocation: Bool = false
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 12) {
                // Selection indicator
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundColor(isSelected ? .blue : .gray)

                // Node class icon
                NodeTypeIconCircle(
                    style: node.node_class?.style,
                    size: 32,
                    iconSize: 16,
                    backgroundColor: Color.blue.opacity(0.1),
                    iconColor: .blue
                )

                // Node info
                VStack(alignment: .leading, spacing: 2) {
                    Text(node.label)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)

                    if let className = node.node_class?.name {
                        Text(className)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    if showLocation, let roomName = node.room?.name {
                        Text(roomName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}
