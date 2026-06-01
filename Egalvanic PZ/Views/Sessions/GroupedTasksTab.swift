//
//  GroupedTasksTab.swift
//  Egalvanic PZ
//
//  Enhanced tasks tab with grouping by location and parent node
//

import SwiftUI
import SwiftData

enum TaskGroupingMode: String, CaseIterable {
    case none = "none"
    case location = "location"
    case parentNode = "parentNode"

    var displayName: String {
        switch self {
        case .none: return AppStrings.Sessions.noGrouping
        case .location: return AppStrings.Sessions.byLocation
        case .parentNode: return AppStrings.Sessions.byParentNode
        }
    }

    var icon: String {
        switch self {
        case .none: return "list.bullet"
        case .location: return "location.fill"
        case .parentNode: return "diagram.project"
        }
    }
}

struct GroupedTasksTab: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    let session: IRSession
    @Binding var selectedTask: UserTask?

    @State private var showTaskLinkingSheet = false
    @State private var showingTaskCreation = false
    @State private var showingTaskTypeSelection = false
    @State private var showingSimpleTaskCreation = false
    @State private var showingComplexTaskCreation = false
    @State private var isProcessing = false
    @State private var errorMessage: String?
    @State private var showError = false
    @State private var groupingMode: TaskGroupingMode = .none
    @State private var expandedGroups: Set<String> = []

    // QR filter state
    @State private var filterNode: NodeV2? = nil
    @State private var showQRScanner = false
    @State private var showDuplicateQRAlert = false
    @State private var duplicateQRNodes: [NodeV2] = []
    @State private var showAssetNotFound = false
    @State private var notFoundQRCode = ""

    private let api = APIClient.shared

    private var allTasks: [UserTask] {
        session.user_tasks.filter { !$0.is_deleted }
            .sorted(by: { $0.title < $1.title })
    }

    private var tasks: [UserTask] {
        guard let filterNode = filterNode else { return allTasks }
        return allTasks.filter { task in
            task.linkedNodes.contains(where: { $0.id == filterNode.id }) ||
            task.node?.id == filterNode.id
        }
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
                    if !allTasks.isEmpty {
                        // Summary Card (Status) - First (always shows unfiltered counts)
                        TaskSummaryView(
                            totalCount: allTasks.count,
                            completedCount: allTasks.filter { $0.completed }.count,
                            pendingCount: allTasks.filter { !$0.completed }.count
                        )
                        .padding(.horizontal)

                        // QR Filter Chip
                        if let filterNode = filterNode {
                            QRFilterChipBar(nodeName: filterNode.label) {
                                withAnimation {
                                    self.filterNode = nil
                                }
                            }
                        }

                        // Controls Row - Manage Tasks button
                        HStack {
                            // MARK: - Group By Selector (Hidden per ZP-317)
                            // HStack {
                            //     Text("Group By:")
                            //         .font(.subheadline)
                            //         .foregroundColor(.secondary)
                            //
                            //     Picker("", selection: $groupingMode) {
                            //         ForEach(TaskGroupingMode.allCases, id: \.self) { mode in
                            //             Text(mode.displayName).tag(mode)
                            //         }
                            //     }
                            //     .pickerStyle(.menu)
                            //     .tint(.blue)
                            // }

                            Spacer()

                            // Manage Tasks Button
                            Button(action: { showTaskLinkingSheet = true }) {
                                Label(AppStrings.Sessions.manage, systemImage: "link.circle.fill")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }
                            .buttonStyle(.borderedProminent)
                        }
                        .padding(.horizontal)

                        // Tasks Title and Display - Third
                        if tasks.isEmpty && filterNode != nil {
                            ContentUnavailableView(
                                AppStrings.Sessions.noTasksForAsset,
                                systemImage: "line.3.horizontal.decrease.circle",
                                description: Text(AppStrings.Sessions.tryClearingFilter)
                            )
                            .frame(minHeight: 200)
                        } else {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Tasks")
                                    .font(.headline)
                                    .padding(.horizontal)

                                // Tasks Display
                                switch groupingMode {
                                case .none:
                                    UngroupedTasksView(
                                        pendingTasks: pendingTasks,
                                        completedTasks: completedTasks,
                                        selectedTask: $selectedTask
                                    )
                                case .location:
                                    GroupedByLocationView(
                                        tasks: tasks,
                                        expandedGroups: $expandedGroups,
                                        selectedTask: $selectedTask
                                    )
                                case .parentNode:
                                    GroupedByParentView(
                                        tasks: tasks,
                                        modelContext: modelContext,
                                        expandedGroups: $expandedGroups,
                                        selectedTask: $selectedTask
                                    )
                                }
                            }
                        }
                    } else {
                        VStack(spacing: 20) {
                            ContentUnavailableView(
                                AppStrings.Sessions.noTasks,
                                systemImage: "checklist",
                                description: Text(AppStrings.Sessions.getStartedTasks)
                            )
                            .frame(minHeight: 300)

                            Button(action: { showTaskLinkingSheet = true }) {
                                Label(AppStrings.Sessions.manageTasks, systemImage: "link.circle.fill")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }
                            .buttonStyle(.borderedProminent)
                        }
                        .padding(.top, 40)
                    }
                }
                .padding(.vertical)
                .padding(.bottom, 80) // Space for floating button
            }

            // Floating Action Buttons
            VStack {
                Spacer()
                HStack(spacing: 12) {
                    Spacer()

                    // QR Scanner FAB
                    Button(action: {
                        showQRScanner = true
                    }) {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(Color.orange)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                    }

                    // Create Task FAB
                    Button(action: {
                        showingTaskTypeSelection = true
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
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
        }
        .overlay {
            if showingTaskTypeSelection {
                TaskTypeSelectionView(
                    isPresented: $showingTaskTypeSelection,
                    onSimpleSelected: {
                        showingSimpleTaskCreation = true
                    },
                    onComplexSelected: {
                        showingComplexTaskCreation = true
                    }
                )
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
        .fullScreenCover(isPresented: $showingSimpleTaskCreation) {
            SimpleTaskCreationView()
                .environmentObject(appState)
                .environmentObject(networkState)
        }
        .fullScreenCover(isPresented: $showingComplexTaskCreation) {
            UnifiedTaskCreationView()
                .environmentObject(appState)
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
        .sheet(isPresented: $showQRScanner) {
            QRCodeScannerView(scannedCode: .constant(""), onScanComplete: { scannedCode in
                handleQRScan(scannedCode)
            })
        }
        .alert(AppStrings.AssetsExtra.multipleAssetsFound, isPresented: $showDuplicateQRAlert) {
            ForEach(duplicateQRNodes.prefix(5), id: \.id) { node in
                Button("\(node.label) (\(node.type))") {
                    withAnimation {
                        filterNode = node
                    }
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) {
                duplicateQRNodes = []
            }
        } message: {
            Text(AppStrings.AssetsExtra.foundAssetsWithQR(duplicateQRNodes.count))
        }
        .alert(AppStrings.Locations.assetNotFound, isPresented: $showAssetNotFound) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(AppStrings.Locations.assetNotFoundMessage(notFoundQRCode))
        }
    }

    private func handleQRScan(_ scannedCode: String) {
        let sldId = session.sld.id
        let qrCode = scannedCode

        let descriptor = FetchDescriptor<NodeV2>(
            predicate: #Predicate<NodeV2> { node in
                node.sld?.id == sldId &&
                node.qr_code == qrCode &&
                !node.is_deleted
            }
        )

        do {
            let matchingNodes = try modelContext.fetch(descriptor)

            if matchingNodes.count == 1 {
                withAnimation {
                    filterNode = matchingNodes[0]
                }
            } else if matchingNodes.count > 1 {
                duplicateQRNodes = matchingNodes
                showDuplicateQRAlert = true
            } else {
                notFoundQRCode = scannedCode
                showAssetNotFound = true
            }
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            errorMessage = "Failed to search for asset: \(error.localizedDescription)"
            showError = true
        }
    }

    private func updateTaskLinks(linkedTaskIds: Set<UUID>) async {
        isProcessing = true
        defer { isProcessing = false }

        let currentTaskIds = Set(allTasks.map { $0.id })

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

        // Show error if any operations failed
        if !errors.isEmpty {
            errorMessage = errors.joined(separator: "\n")
            showError = true
        }
    }
}

// MARK: - Summary View
struct TaskSummaryView: View {
    let totalCount: Int
    let completedCount: Int
    let pendingCount: Int

    var body: some View {
        HStack {
            Spacer()

            VStack {
                Text("\(totalCount)")
                    .font(.title2)
                    .fontWeight(.bold)
                Text(AppStrings.CommonExtra.total)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            VStack {
                Text("\(pendingCount)")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.orange)
                Text(AppStrings.Common.pending)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            VStack {
                Text("\(completedCount)")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.green)
                Text(AppStrings.Sessions.completed)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
        .padding()
        .background(Color(UIColor.systemGray6))
        .cornerRadius(12)
    }
}

// MARK: - Ungrouped Tasks View
struct UngroupedTasksView: View {
    let pendingTasks: [UserTask]
    let completedTasks: [UserTask]
    @Binding var selectedTask: UserTask?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Pending Tasks
            if !pendingTasks.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text(AppStrings.Sessions.pendingTasks)
                        .font(.headline)
                        .padding(.horizontal)

                    ForEach(pendingTasks) { task in
                        TaskCard(task: task)
                            .padding(.horizontal)
                            .onTapGesture {
                                selectedTask = task
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
                        TaskCard(task: task)
                            .padding(.horizontal)
                            .onTapGesture {
                                selectedTask = task
                            }
                    }
                }
            }
        }
    }
}

// MARK: - Grouped by Location View
struct GroupedByLocationView: View {
    let tasks: [UserTask]
    @Binding var expandedGroups: Set<String>
    @Binding var selectedTask: UserTask?

    private var groupedTasks: [(location: String, tasks: [UserTask], isMultiNode: Bool)] {
        var groups: [String: [UserTask]] = [:]
        var multiNodeTasks: [UserTask] = []

        for task in tasks {
            let nodes = task.linkedNodes

            if nodes.count > 1 {
                // Check if all nodes share the same location
                let locations = Set(nodes.compactMap { $0.location })

                if locations.count == 1, let sharedLocation = locations.first {
                    // All nodes have the same non-nil location
                    if groups[sharedLocation] == nil {
                        groups[sharedLocation] = []
                    }
                    groups[sharedLocation]?.append(task)
                } else if locations.isEmpty {
                    // All nodes have nil location
                    if groups[AppStrings.CommonExtra.noLocation] == nil {
                        groups[AppStrings.CommonExtra.noLocation] = []
                    }
                    groups[AppStrings.CommonExtra.noLocation]?.append(task)
                } else if locations.count <= 2 {
                    // Check for parent-child with single location case
                    // If we have one location and some nil locations, check if the nil ones are children
                    let nodesWithLocation = nodes.filter { $0.location != nil }
                    let nodesWithoutLocation = nodes.filter { $0.location == nil }

                    if nodesWithLocation.count == 1,
                       let parentNode = nodesWithLocation.first,
                       let parentLocation = parentNode.location {
                        // Check if all nodes without location are children of the node with location
                        let allAreChildren = nodesWithoutLocation.allSatisfy { child in
                            child.parent_id == parentNode.id
                        }

                        if allAreChildren {
                            // Group by the parent's location
                            if groups[parentLocation] == nil {
                                groups[parentLocation] = []
                            }
                            groups[parentLocation]?.append(task)
                        } else {
                            // Mixed locations, treat as multi-node
                            multiNodeTasks.append(task)
                        }
                    } else {
                        // Multiple different locations
                        multiNodeTasks.append(task)
                    }
                } else {
                    // Multiple different locations
                    multiNodeTasks.append(task)
                }
            } else if nodes.count == 1 {
                // Single node task
                let location = nodes[0].location ?? AppStrings.CommonExtra.noLocation
                if groups[location] == nil {
                    groups[location] = []
                }
                groups[location]?.append(task)
            } else if let node = task.node {
                // Legacy single node field
                let location = node.location ?? AppStrings.CommonExtra.noLocation
                if groups[location] == nil {
                    groups[location] = []
                }
                groups[location]?.append(task)
            } else {
                // No nodes at all
                if groups[AppStrings.CommonExtra.noLocation] == nil {
                    groups[AppStrings.CommonExtra.noLocation] = []
                }
                groups[AppStrings.CommonExtra.noLocation]?.append(task)
            }
        }

        var result: [(String, [UserTask], Bool)] = []

        // Add regular location groups
        for (location, locationTasks) in groups.sorted(by: { $0.key < $1.key }) {
            result.append((location, locationTasks.sorted { $0.title < $1.title }, false))
        }

        // Add multi-node group if there are any
        if !multiNodeTasks.isEmpty {
            result.append((AppStrings.Sessions.multiNodeTasks, multiNodeTasks.sorted { $0.title < $1.title }, true))
        }

        return result
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(groupedTasks, id: \.location) { group in
                TaskGroupSection(
                    title: group.location,
                    tasks: group.tasks,
                    isExpanded: expandedGroups.contains(group.location),
                    isSpecialGroup: group.isMultiNode,
                    icon: group.isMultiNode ? "square.stack.3d.up" : "location.fill",
                    onToggle: {
                        if expandedGroups.contains(group.location) {
                            expandedGroups.remove(group.location)
                        } else {
                            expandedGroups.insert(group.location)
                        }
                    },
                    selectedTask: $selectedTask
                )
            }
        }
        .padding(.horizontal)
    }
}

// MARK: - Grouped by Parent Node View
struct GroupedByParentView: View {
    let tasks: [UserTask]
    let modelContext: ModelContext
    @Binding var expandedGroups: Set<String>
    @Binding var selectedTask: UserTask?

    private var groupedTasks: [(parent: String, tasks: [UserTask], isMultiNode: Bool)] {
        var groups: [String: [UserTask]] = [:]
        var multiNodeTasks: [UserTask] = []
        var noParentTasks: [UserTask] = []

        for task in tasks {
            let nodes = task.linkedNodes

            if nodes.count > 1 {
                // Check if this is a parent-children relationship
                // First, check if one node is the parent of all others
                var foundParentChild = false

                for potentialParent in nodes {
                    let otherNodes = nodes.filter { $0.id != potentialParent.id }
                    let allAreChildren = otherNodes.allSatisfy { child in
                        child.parent_id == potentialParent.id
                    }

                    if allAreChildren && !otherNodes.isEmpty {
                        // This is a parent with its children
                        let parentLabel = "\(potentialParent.label) (\(potentialParent.type))"
                        if groups[parentLabel] == nil {
                            groups[parentLabel] = []
                        }
                        groups[parentLabel]?.append(task)
                        foundParentChild = true
                        break
                    }
                }

                if !foundParentChild {
                    // Check if all nodes share the same parent
                    let parentIds = Set(nodes.compactMap { $0.parent_id })

                    if parentIds.count == 1, let sharedParentId = parentIds.first {
                        // All nodes have the same parent
                        // Try to find parent node to get its label
                        let fetchDescriptor = FetchDescriptor<NodeV2>(
                            predicate: #Predicate { node in
                                node.id == sharedParentId && !node.is_deleted
                            }
                        )

                        var parentLabel = AppStrings.Sessions.unknownParent
                        if let parentNodes = try? modelContext.fetch(fetchDescriptor),
                           let parentNode = parentNodes.first {
                            parentLabel = "\(parentNode.label) (\(parentNode.type))"
                        }

                        if groups[parentLabel] == nil {
                            groups[parentLabel] = []
                        }
                        groups[parentLabel]?.append(task)
                    } else if parentIds.isEmpty {
                        // All nodes have no parent
                        noParentTasks.append(task)
                    } else {
                        // Mixed parent situation - treat as multi-node
                        multiNodeTasks.append(task)
                    }
                }
            } else if nodes.count == 1 {
                // Single node task
                if let parentId = nodes[0].parent_id {
                    // Try to find parent node to get its label
                    let fetchDescriptor = FetchDescriptor<NodeV2>(
                        predicate: #Predicate { node in
                            node.id == parentId && !node.is_deleted
                        }
                    )

                    var parentLabel = AppStrings.Sessions.unknownParent
                    if let parentNodes = try? modelContext.fetch(fetchDescriptor),
                       let parentNode = parentNodes.first {
                        parentLabel = "\(parentNode.label) (\(parentNode.type))"
                    }

                    if groups[parentLabel] == nil {
                        groups[parentLabel] = []
                    }
                    groups[parentLabel]?.append(task)
                } else {
                    noParentTasks.append(task)
                }
            } else if let node = task.node {
                // Legacy single node field
                if let parentId = node.parent_id {
                    // Try to find parent node to get its label
                    let fetchDescriptor = FetchDescriptor<NodeV2>(
                        predicate: #Predicate { node in
                            node.id == parentId && !node.is_deleted
                        }
                    )

                    var parentLabel = AppStrings.Sessions.unknownParent
                    if let parentNodes = try? modelContext.fetch(fetchDescriptor),
                       let parentNode = parentNodes.first {
                        parentLabel = "\(parentNode.label) (\(parentNode.type))"
                    }

                    if groups[parentLabel] == nil {
                        groups[parentLabel] = []
                    }
                    groups[parentLabel]?.append(task)
                } else {
                    noParentTasks.append(task)
                }
            } else {
                // No nodes at all
                noParentTasks.append(task)
            }
        }

        var result: [(String, [UserTask], Bool)] = []

        // Add parent-grouped tasks
        for (parent, parentTasks) in groups.sorted(by: { $0.key < $1.key }) {
            result.append((parent, parentTasks.sorted { $0.title < $1.title }, false))
        }

        // Add no-parent tasks if any
        if !noParentTasks.isEmpty {
            result.append((AppStrings.Sessions.noParentNode, noParentTasks.sorted { $0.title < $1.title }, false))
        }

        // Add multi-node group if there are any
        if !multiNodeTasks.isEmpty {
            result.append((AppStrings.Sessions.multiNodeTasks, multiNodeTasks.sorted { $0.title < $1.title }, true))
        }

        return result
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(groupedTasks, id: \.parent) { group in
                TaskGroupSection(
                    title: group.parent,
                    tasks: group.tasks,
                    isExpanded: expandedGroups.contains(group.parent),
                    isSpecialGroup: group.isMultiNode,
                    icon: group.isMultiNode ? "square.stack.3d.up" : "diagram.project",
                    onToggle: {
                        if expandedGroups.contains(group.parent) {
                            expandedGroups.remove(group.parent)
                        } else {
                            expandedGroups.insert(group.parent)
                        }
                    },
                    selectedTask: $selectedTask
                )
            }
        }
        .padding(.horizontal)
    }
}

// MARK: - Task Group Section
struct TaskGroupSection: View {
    let title: String
    let tasks: [UserTask]
    let isExpanded: Bool
    let isSpecialGroup: Bool
    let icon: String
    let onToggle: () -> Void
    @Binding var selectedTask: UserTask?

    private var pendingCount: Int {
        tasks.filter { !$0.completed }.count
    }

    private var completedCount: Int {
        tasks.filter { $0.completed }.count
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Group Header
            Button(action: onToggle) {
                HStack {
                    Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .frame(width: 20)

                    Image(systemName: icon)
                        .font(.body)
                        .foregroundColor(isSpecialGroup ? .orange : .secondary)

                    Text(title)
                        .font(.headline)
                        .foregroundColor(.primary)

                    Spacer()

                    // Task counts
                    HStack(spacing: 12) {
                        if pendingCount > 0 {
                            Label("\(pendingCount)", systemImage: "circle")
                                .font(.caption)
                                .foregroundColor(.orange)
                        }
                        if completedCount > 0 {
                            Label("\(completedCount)", systemImage: "checkmark.circle.fill")
                                .font(.caption)
                                .foregroundColor(.green)
                        }
                    }
                }
                .padding(.vertical, 10)
                .padding(.horizontal, 12)
                .background(isSpecialGroup ? Color.orange.opacity(0.1) : Color(UIColor.systemGray6))
                .cornerRadius(10)
            }
            .buttonStyle(.plain)

            if isExpanded {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(tasks) { task in
                        TaskCard(task: task)
                            .padding(.leading, 32)
                            .onTapGesture {
                                selectedTask = task
                            }
                    }
                }
            }
        }
    }
}