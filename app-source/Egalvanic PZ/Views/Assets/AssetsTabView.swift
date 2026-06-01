// AssetsTabView.swift
// SwiftDataTutorial
//
// Created by Eric Ehlert on 8/7/25.
//

import SwiftUI
import SwiftData

enum AssetGroupingOption {
    case none
    case location
    case parentNode
    
    var title: String {
        switch self {
        case .none:
            return AppStrings.AssetsExtra.noGrouping
        case .location:
            return AppStrings.AssetsExtra.groupByLocation
        case .parentNode:
            return AppStrings.AssetsExtra.groupByEnclosure
        }
    }
    
    var icon: String {
        switch self {
        case .none:
            return "list.bullet"
        case .location:
            return "square.grid.3x1.folder.badge.plus"
        case .parentNode:
            return "square.stack"
        }
    }
}

struct AssetsTabView: View {
    @Bindable var diagram: SLDV2 // Use @Bindable to observe changes
    
    // Context
    @Environment(\.modelContext) private var modelContext
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    
    // Environment Objects
    @EnvironmentObject var languageManager: LanguageManager
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var sldService: SLDService

    // State: Alerts
    @State private var showRefreshError = false
    @State private var refreshErrorMessage = ""
    @State private var showDeleteConfirmation = false
    @State private var nodeToDelete: NodeV2?
    @State private var showDeleteError = false
    @State private var deleteErrorMessage = ""
    
    // State: Multi-select
    @State private var isMultiSelectMode = false
    @State private var selectedNodeIds: Set<UUID> = []
    
    // State: Search
    @State private var assetSearchText = ""
    @State private var showQRScanner = false
    @State private var showDuplicateQRAlert = false
    @State private var duplicateNodes: [NodeV2] = []
    
    // State: Edit Asset
    @State private var selectedNode: NodeV2?
    
    // State: Create Asset
    @State private var showingAddAsset = false
    @State private var newAssetName = ""
    @State private var newAssetLocation = ""
    @State private var selectedNodeClass: NodeClass?
    
    // State: Grouping
    @State private var groupingOption: AssetGroupingOption = .none
    @State private var collapsedSections: Set<String> = []
    @State private var showArcFlashStatus = false

    // State: Scroll position preservation
    @State private var scrolledNodeID: UUID?

    // PERFORMANCE: Dictionary for O(1) node lookups instead of O(n) linear searches
    private var nodeById: [UUID: NodeV2] {
        Dictionary(uniqueKeysWithValues: diagram.nodes.map { ($0.id, $0) })
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                // PERFORMANCE: Build dictionary once per render, not per row
                let nodeLookup = nodeById
                
                List {
                    if groupingOption != .none {
                        // Grouped view
                        ForEach(groupedNodes.sorted(by: {
                            // Put "Top-Level" at the bottom
                            if $0.key == AppStrings.AssetsExtra.topLevel { return false }
                            if $1.key == AppStrings.AssetsExtra.topLevel { return true }
                            return $0.key < $1.key
                        }), id: \.key) { groupName, nodes in
                            Section(
                                isExpanded: Binding(
                                    get: { !collapsedSections.contains(groupName) },
                                    set: { isExpanded in
                                        if isExpanded {
                                            collapsedSections.remove(groupName)
                                        } else {
                                            collapsedSections.insert(groupName)
                                        }
                                    }
                                )
                            ) {
                                ForEach(nodes, id: \.id) { node in
                                    if isMultiSelectMode {
                                        multiSelectNodeRow(node, nodeById: nodeLookup)
                                    } else {
                                        nodeRow(node, nodeById: nodeLookup)
                                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                                Button(role: .destructive) {
                                                    deleteNode(node)
                                                } label: {
                                                    Label(AppStrings.Common.delete, systemImage: "trash")
                                                }
                                            }
                                    }
                                }
                            } header: {
                                HStack {
                                    Text(groupName)
                                        .font(.headline)
                                    Spacer()
                                    Text("\(nodes.count)")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 2)
                                        .background(Color.secondary.opacity(0.2))
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    } else {
                        // Flat list view
                        ForEach(filteredNodes, id: \.id) { node in
                            if isMultiSelectMode {
                                multiSelectNodeRow(node, nodeById: nodeLookup)
                            } else {
                                nodeRow(node, nodeById: nodeLookup)
                                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                        Button(role: .destructive) {
                                            deleteNode(node)
                                        } label: {
                                            Label(AppStrings.Common.delete, systemImage: "trash")
                                        }
                                    }
                            }
                        }
                    }
                }
                .scrollPosition(id: $scrolledNodeID)
                .refreshable {
                    await refreshFromServer()
                }
                .onChange(of: groupingOption) { _, _ in
                    scrolledNodeID = nil
                }
                .overlay {
                    if filteredNodes.isEmpty && !assetSearchText.isEmpty {
                        ContentUnavailableView.search(text: assetSearchText)
                    }
                }

                // Floating QR Scanner Button
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: {
                            showQRScanner = true
                        }) {
                            Image(systemName: "qrcode.viewfinder")
                                .font(.title2)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                                .frame(width: 56, height: 56)
                                .background(Color.blue)
                                .clipShape(Circle())
                                .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                        }
                        .padding(.trailing, 20)
                        .padding(.bottom, 20)
                    }
                }
            }
            .navigationTitle(isMultiSelectMode ? AppStrings.AssetsExtra.selectedCount(selectedNodeIds.count) : AppStrings.Tabs.assets)
            .searchable(text: $assetSearchText, prompt: AppStrings.AssetsExtra.searchByNameTypeLocation)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if isMultiSelectMode {
                        Button(AppStrings.Common.cancel) {
                            exitMultiSelectMode()
                        }
                    } else {
                        NetworkStatusButton()
                    }
                }
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    if isMultiSelectMode {
                        // iPad portrait: Use Menu to prevent toolbar overflow hiding the delete button
                        if horizontalSizeClass == .regular {
                            Menu {
                                Button {
                                    selectedNodeIds = Set(filteredNodes.map { $0.id })
                                } label: {
                                    Label(AppStrings.AssetsExtra.selectAll, systemImage: "checkmark.circle.fill")
                                }
                                .disabled(selectedNodeIds.count == filteredNodes.count)
                                
                                Button(role: .destructive) {
                                    if !selectedNodeIds.isEmpty {
                                        showDeleteConfirmation = true
                                    }
                                } label: {
                                    Label(AppStrings.AssetsExtra.deleteSelected, systemImage: "trash")
                                }
                                .disabled(selectedNodeIds.isEmpty)
                            } label: {
                                Image(systemName: "ellipsis.circle")
                            }
                        } else {
                            // iPhone: Keep separate buttons
                            Button(AppStrings.AssetsExtra.selectAll) {
                                selectedNodeIds = Set(filteredNodes.map { $0.id })
                            }
                            .disabled(selectedNodeIds.count == filteredNodes.count)
                            
                            Button {
                                if !selectedNodeIds.isEmpty {
                                    showDeleteConfirmation = true
                                }
                            } label: {
                                Image(systemName: "trash")
                                    .foregroundColor(selectedNodeIds.isEmpty ? .gray : .red)
                            }
                            .disabled(selectedNodeIds.isEmpty)
                        }
                    } else {
                        Menu {
                            // iPad: Add Create Asset to dropdown (visible when toolbar overflows in portrait)
                            if horizontalSizeClass == .regular {
                                Button {
                                    createNewNode()
                                } label: {
                                    Label(AppStrings.Assets.createAsset, systemImage: "plus")
                                }
                                Divider()
                            }
                            
                            Section(AppStrings.AssetsExtra.grouping) {
                                ForEach([AssetGroupingOption.none, .location, .parentNode], id: \.self) { option in
                                    Button {
                                        withAnimation {
                                            groupingOption = option
                                        }
                                    } label: {
                                        Label(option.title, systemImage: option.icon)
                                    }
                                }
                            }
                            
                            Button {
                                withAnimation {
                                    showArcFlashStatus.toggle()
                                }
                            } label: {
                                Label(
                                    showArcFlashStatus ? AppStrings.AssetsExtra.hideAFPunchlist : AppStrings.AssetsExtra.showAFPunchlist,
                                    systemImage: showArcFlashStatus ? "bolt.circle.fill" : "bolt.circle"
                                )
                            }
                            
                            Divider()
                            
                            Button {
                                enterMultiSelectMode()
                            } label: {
                                Label(AppStrings.AssetsExtra.selectMultiple, systemImage: "checkmark.circle")
                            }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                        }
                        
                        Button(action: createNewNode) {
                            Image(systemName: "plus")
                        }
                        
                        IRSessionPickerButton()
                            .environmentObject(appState)
                            .environment(\.modelContext, modelContext)
                    }
                }
            }
        }
        // Refresh progress overlay
        .refreshProgressOverlay()
        // Alerts
        .alert(AppStrings.Sessions.refreshFailed, isPresented: $showRefreshError) {
            Button(AppStrings.Common.ok, role: .cancel) { }
            if networkState.mode == .offline {
                Button(AppStrings.Sessions.goOnline) {
                    networkState.toggleMode()
                    Task {
                        await refreshFromServer()
                    }
                }
            }
        } message: {
            Text(refreshErrorMessage)
        }
        .alert(AppStrings.AssetsExtra.deleteAsset, isPresented: $showDeleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                nodeToDelete = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let node = nodeToDelete {
                    performDelete(node)
                } else if !selectedNodeIds.isEmpty {
                    performMultiDelete()
                }
            }
        } message: {
            if let node = nodeToDelete {
                Text(AppStrings.AssetsExtra.deleteAssetConfirm(node.label))
            } else if !selectedNodeIds.isEmpty {
                Text(AppStrings.AssetsExtra.deleteAssetsConfirm(selectedNodeIds.count))
            }
        }
        .alert(AppStrings.AssetsExtra.deleteFailed, isPresented: $showDeleteError) {
            Button(AppStrings.Common.ok, role: .cancel) { }
        } message: {
            Text(deleteErrorMessage)
        }
        // edit node
        .fullScreenCover(item: $selectedNode) { node in
            EditNodeDetailViewV3(node: node, sld: diagram)
        }
        // create new node
        .fullScreenCover(isPresented: $showingAddAsset) {
            AddAssetViewV2(
                availableLocations: availableLocations,
                availableNodeClasses: availableNodeClasses,
                sld: diagram,
                onSave: { node, photos, irPhotos in
                    handleAssetSave(node: node, photos: photos, irPhotos: irPhotos)
                },
                onCancel: {
                    showingAddAsset = false
                }
            )
            .environmentObject(networkState)
            .environmentObject(appState)
        }
        // QR Scanner fullscreen
        .fullScreenCover(isPresented: $showQRScanner) {
            QRCodeScannerView(scannedCode: .constant("")) { scannedCode in
                // Search for all nodes with matching QR code
                let matchingNodes = diagram.nodes.filter {
                    !$0.is_deleted && $0.qr_code?.lowercased() == scannedCode.lowercased()
                }
                
                if matchingNodes.count == 1 {
                    // Single match - open directly
                    selectedNode = matchingNodes[0]
                } else if matchingNodes.count > 1 {
                    // Multiple matches - show alert to choose
                    duplicateNodes = matchingNodes
                    showDuplicateQRAlert = true
                } else {
                    // No exact match, set search text to show results
                    assetSearchText = scannedCode
                }
            }
        }
        .alert(AppStrings.AssetsExtra.multipleAssetsFound, isPresented: $showDuplicateQRAlert) {
            ForEach(duplicateNodes.prefix(5), id: \.id) { node in
                Button(node.label) {
                    selectedNode = node
                }
            }
            if duplicateNodes.count > 5 {
                Button(AppStrings.AssetsExtra.viewAllInSearch) {
                    assetSearchText = duplicateNodes.first?.qr_code ?? ""
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) {
                duplicateNodes = []
            }
        } message: {
            Text(AppStrings.AssetsExtra.foundAssetsWithQR(duplicateNodes.count))
        }
    }
    
    // View builder for node row
    @ViewBuilder
    private func nodeRow(_ node: NodeV2, nodeById: [UUID: NodeV2]) -> some View {
        // Snapshot relationship values once to avoid repeated SwiftData fault resolution (ZP-364)
        let nodeClassStyle = node.node_class?.style
        let nodeClassName = node.node_class?.name
        let roomName = node.room?.name

        // PERFORMANCE: O(1) dictionary lookup instead of O(n) linear search
        let hasDeletedParent = node.parent_id != nil &&
        nodeById[node.parent_id!]?.is_deleted == true

        Button(action: {
            selectedNode = node
        }) {
            HStack(spacing: 12) {
                // Node type icon
                NodeTypeIcon(style: nodeClassStyle, size: 22, color: .secondary)

                VStack(alignment: .leading, spacing: 2) {
                    Text(node.label)
                        .font(.subheadline)
                        .lineLimit(1)
                        .foregroundStyle(hasDeletedParent ? Color.red : .primary)

                    // Show room name or "No location" in red
                    if groupingOption != .location {
                        if let roomName, !roomName.isEmpty {
                            Text(roomName)
                                .font(.caption2)
                                .lineLimit(1)
                                .foregroundStyle(.tertiary)
                        } else {
                            Text(AppStrings.CommonExtra.noLocation)
                                .font(.caption2)
                                .lineLimit(1)
                                .foregroundStyle(.red)
                        }
                    }
                }
                Spacer()
                Text(nodeClassName ?? "")
                    .font(.caption)
                    .lineLimit(1)
                    .foregroundStyle(.secondary)
                
                // Arc Flash completion status indicator (only show if enabled)
                if showArcFlashStatus {
                    Image(systemName: "bolt.circle.fill")
                        .foregroundColor(node.af_isComplete ? .green : .red)
                        .imageScale(.medium)
                }
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            .padding(.vertical, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
    
    // View builder for multi-select node row
    @ViewBuilder
    private func multiSelectNodeRow(_ node: NodeV2, nodeById: [UUID: NodeV2]) -> some View {
        // Snapshot relationship values once to avoid repeated SwiftData fault resolution (ZP-364)
        let nodeClassStyle = node.node_class?.style
        let nodeClassName = node.node_class?.name
        let roomName = node.room?.name

        // PERFORMANCE: O(1) dictionary lookup instead of O(n) linear search
        let hasDeletedParent = node.parent_id != nil &&
        nodeById[node.parent_id!]?.is_deleted == true

        Button(action: {
            toggleNodeSelection(node.id)
        }) {
            HStack(spacing: 12) {
                Image(systemName: selectedNodeIds.contains(node.id) ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(selectedNodeIds.contains(node.id) ? .accentColor : .gray)
                    .imageScale(.large)

                // Node type icon
                NodeTypeIcon(style: nodeClassStyle, size: 22, color: .secondary)

                VStack(alignment: .leading, spacing: 2) {
                    Text(node.label)
                        .font(.subheadline)
                        .lineLimit(1)
                        .foregroundStyle(hasDeletedParent ? Color.red : .primary)

                    // Show room name or "No location" in red
                    if groupingOption != .location {
                        if let roomName, !roomName.isEmpty {
                            Text(roomName)
                                .font(.caption2)
                                .lineLimit(1)
                                .foregroundStyle(.tertiary)
                        } else {
                            Text(AppStrings.CommonExtra.noLocation)
                                .font(.caption2)
                                .lineLimit(1)
                                .foregroundStyle(.red)
                        }
                    }
                }
                Spacer()
                Text(nodeClassName ?? node.type)
                    .font(.caption)
                    .lineLimit(1)
                    .foregroundStyle(.secondary)
                
                // Arc Flash completion status indicator (only show if enabled)
                if showArcFlashStatus {
                    Image(systemName: "bolt.circle.fill")
                        .foregroundColor(node.af_isComplete ? .green : .red)
                        .imageScale(.medium)
                }
            }
            .padding(.vertical, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
    
    // Computed properties for filtering and grouping
    private var filteredNodes: [NodeV2] {
        let activeNodes = diagram.nodes.filter { !$0.is_deleted }
        
        if assetSearchText.isEmpty {
            return activeNodes.sorted { $0.label < $1.label }
        } else {
            return activeNodes.filter { node in
                node.label.localizedCaseInsensitiveContains(assetSearchText) ||
                (node.node_class?.name.localizedCaseInsensitiveContains(assetSearchText) ?? false) ||
                (node.room?.name.localizedCaseInsensitiveContains(assetSearchText) ?? false) ||
                (node.location?.localizedCaseInsensitiveContains(assetSearchText) ?? false) ||
                (node.qr_code?.localizedCaseInsensitiveContains(assetSearchText) ?? false)
            }.sorted { $0.label < $1.label }
        }
    }
    
    private var groupedNodes: [String: [NodeV2]] {
        // PERFORMANCE: Use class-level nodeById dictionary for O(1) lookups
        let nodeLookup = nodeById
        
        switch groupingOption {
        case .none:
            return [:]
        case .location:
            return Dictionary(grouping: filteredNodes) { node in
                // If node has a parent, use parent's room; otherwise use node's own room
                if let parentId = node.parent_id,
                   let parentNode = nodeLookup[parentId] {
                    // Try room name first, fall back to deprecated location field
                    if let room = parentNode.room, !room.name.isEmpty {
                        return room.name
                    } else {
                        return parentNode.location?.trimmingCharacters(in: .whitespacesAndNewlines) ?? AppStrings.CommonExtra.noLocation
                    }
                } else {
                    // Try room name first, fall back to deprecated location field
                    if let room = node.room, !room.name.isEmpty {
                        return room.name
                    } else {
                        return node.location?.trimmingCharacters(in: .whitespacesAndNewlines) ?? AppStrings.CommonExtra.noLocation
                    }
                }
            }.mapValues { nodes in
                nodes.sorted { $0.label < $1.label }
            }
        case .parentNode:
            // PERFORMANCE: Pre-compute which nodes have children - O(n) once instead of O(n) per node
            let nodesWithChildren: Set<UUID> = Set(filteredNodes.compactMap { $0.parent_id })
            
            return Dictionary(grouping: filteredNodes) { node in
                // O(1) set lookup instead of O(n) contains
                let hasChildren = nodesWithChildren.contains(node.id)
                
                if hasChildren {
                    // This is a parent node - group by its own label
                    return node.label
                } else if let parentId = node.parent_id,
                          let parentNode = nodeLookup[parentId] {
                    // This is a child node - group by parent's label
                    return parentNode.label
                } else {
                    // This is a top-level node without children
                    return AppStrings.AssetsExtra.topLevel
                }
            }.mapValues { nodes in
                // Sort: parent node first, then children alphabetically
                nodes.sorted { node1, node2 in
                    // O(1) set lookup instead of O(n) contains
                    let node1HasChildren = nodesWithChildren.contains(node1.id)
                    let node2HasChildren = nodesWithChildren.contains(node2.id)
                    
                    // Parent node always comes first
                    if node1HasChildren && !node2HasChildren {
                        return true
                    } else if !node1HasChildren && node2HasChildren {
                        return false
                    }
                    
                    // Both are children or both are parents - sort alphabetically
                    return node1.label < node2.label
                }
            }
        }
    }
    
    private func createNewNode() {
        newAssetName = ""
        newAssetLocation = ""
        selectedNodeClass = nil
        showingAddAsset = true
    }
    
    private func handleAssetSave(node: NodeV2, photos: [Photo], irPhotos: [IRPhoto]) {
        // Set the SLD relationship FIRST before any photo assignment
        if node.sld == nil {
            node.sld = diagram
        }
        
        // Use the NodeService async method to handle all the creation logic
        Task {
            await NodeService.createNewNodeWithPhotosAndIR(
                node: node,
                photos: photos,
                irPhotos: irPhotos,
                networkState: networkState,
                modelContext: modelContext
            )
            
            await MainActor.run {
                showingAddAsset = false
            }
        }
    }
    
    private var availableLocations: [String] {
        let locations = diagram.nodes
            .filter { !$0.is_deleted }
            .compactMap { $0.location?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        
        // Remove duplicates and sort
        return Array(Set(locations)).sorted()
    }
    
    private var availableNodeClasses: [NodeClass] {
        let descriptor = FetchDescriptor<NodeClass>()
        let nodeClasses = (try? modelContext.fetch(descriptor)) ?? []
        return nodeClasses
            .filter { !$0.is_deleted }
            .sorted { $0.name < $1.name }
    }
    
    private func refreshFromServer() async {
        // ZP-2173 — pull-to-refresh fetches the SLD from the server and
        // overwrites the local SwiftData entity rows. Legacy queue items
        // (`userId == nil`) still depend on those rows to flush, so abort
        // the refresh and surface the same blocker copy used elsewhere.
        if networkState.hasLegacySyncItems {
            await MainActor.run {
                refreshErrorMessage = AppStrings.Diagnostics.legacyItemsBlockingAction
                showRefreshError = true
            }
            return
        }
        do {
            try await sldService.refreshSLD(modelContext: modelContext)
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            await MainActor.run {
                if let sldError = error as? SLDServiceError {
                    refreshErrorMessage = sldError.recoverySuggestion ?? sldError.localizedDescription
                } else {
                    refreshErrorMessage = "Failed to refresh data: \(error.localizedDescription)"
                }
                showRefreshError = true
            }
        }
    }
    
    private func deleteNode(_ node: NodeV2) {
        nodeToDelete = node
        showDeleteConfirmation = true
    }
    
    private func performDelete(_ node: NodeV2) {
        // Clear the reference before deletion
        let nodeId = node.id
        nodeToDelete = nil
        
        NodeService.deleteNodes(
            nodeIds: Set([nodeId]),
            diagram: diagram,
            networkState: networkState,
            modelContext: modelContext
        ) { success, message in
            if success {
                // Optionally show a success message if offline
                if let message = message {
                    // The deletion was queued for later sync
                    AppLogger.log(.info, "\(message)", category: .node)
                }
            } else {
                deleteErrorMessage = message ?? "Failed to delete asset"
                showDeleteError = true
            }
        }
    }
    
    private func performMultiDelete() {
        // Copy the IDs before clearing the selection
        let nodeIdsToDelete = selectedNodeIds
        exitMultiSelectMode()
        
        NodeService.deleteNodes(
            nodeIds: nodeIdsToDelete,
            diagram: diagram,
            networkState: networkState,
            modelContext: modelContext
        ) { success, message in
            if success {
                // Optionally show a success message if offline
                if let message = message {
                    // The deletion was queued for later sync
                    AppLogger.log(.info, "\(message)", category: .node)
                }
            } else {
                deleteErrorMessage = message ?? "Failed to delete assets"
                showDeleteError = true
            }
        }
    }
    
    private func enterMultiSelectMode() {
        withAnimation {
            isMultiSelectMode = true
            selectedNodeIds.removeAll()
        }
    }
    
    private func exitMultiSelectMode() {
        withAnimation {
            isMultiSelectMode = false
            selectedNodeIds.removeAll()
        }
    }
    
    private func toggleNodeSelection(_ nodeId: UUID) {
        if selectedNodeIds.contains(nodeId) {
            selectedNodeIds.remove(nodeId)
        } else {
            selectedNodeIds.insert(nodeId)
        }
    }
}
