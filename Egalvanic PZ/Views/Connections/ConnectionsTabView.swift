//  ConnectionsTabView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/7/25.
//

import SwiftUI
import SwiftData

struct ConnectionsTabView: View {
    let diagram: SLDV2
    
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
    @State private var edgeToDelete: EdgeV2?
    @State private var showDeleteError = false
    @State private var deleteErrorMessage = ""
    
    // State: Multi-select
    @State private var isMultiSelectMode = false
    @State private var selectedEdgeIds: Set<UUID> = []
    
    // State: Search
    @State private var connectionSearchText = ""
    
    // State: Edit Edge
    @State private var selectedEdge: EdgeV2?
    
    // State: Create Edge
    @State private var showingAddConnection = false
    @State private var selectedEdgeClass: EdgeClass?
    
    // State: Grouping
    @State private var showArcFlashStatus = false

    // State: Scroll position preservation
    @State private var scrolledEdgeID: UUID?
    
    var body: some View {
        NavigationStack {
            ZStack {
                // PERFORMANCE: Build dictionary once per render, not per row
                let nodeLookup = nodeById

                List {
                    // Flat list view
                    ForEach(filteredEdges, id: \.id) { edge in
                        if isMultiSelectMode {
                            multiSelectEdgeRow(edge, nodeById: nodeLookup)
                        } else {
                            edgeRow(edge, nodeById: nodeLookup)
                                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                    Button(role: .destructive) {
                                        deleteEdge(edge)
                                    } label: {
                                        Label(AppStrings.Common.delete, systemImage: "trash")
                                    }
                                }
                        }
                    }
                }
                .scrollPosition(id: $scrolledEdgeID)
                .refreshable {
                    await refreshFromServer()
                }

                // Refresh progress overlay
                RefreshProgressOverlay(syncService: SLDSyncService.shared)
            }
            .navigationTitle(isMultiSelectMode ? AppStrings.Connections.selectedCount(selectedEdgeIds.count) : AppStrings.Tabs.connections)
            .searchable(text: $connectionSearchText, prompt: Text(AppStrings.Connections.searchConnections))
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
                                    selectedEdgeIds = Set(filteredEdges.map { $0.id })
                                } label: {
                                    Label(AppStrings.AssetsExtra.selectAll, systemImage: "checkmark.circle.fill")
                                }
                                .disabled(selectedEdgeIds.count == filteredEdges.count)

                                Button(role: .destructive) {
                                    if !selectedEdgeIds.isEmpty {
                                        showDeleteConfirmation = true
                                    }
                                } label: {
                                    Label(AppStrings.AssetsExtra.deleteSelected, systemImage: "trash")
                                }
                                .disabled(selectedEdgeIds.isEmpty)
                            } label: {
                                Image(systemName: "ellipsis.circle")
                            }
                        } else {
                            // iPhone: Keep separate buttons
                            Button(AppStrings.AssetsExtra.selectAll) {
                                selectedEdgeIds = Set(filteredEdges.map { $0.id })
                            }
                            .disabled(selectedEdgeIds.count == filteredEdges.count)

                            Button {
                                if !selectedEdgeIds.isEmpty {
                                    showDeleteConfirmation = true
                                }
                            } label: {
                                Image(systemName: "trash")
                                    .foregroundColor(selectedEdgeIds.isEmpty ? .gray : .red)
                            }
                            .disabled(selectedEdgeIds.isEmpty)
                        }
                    } else {
                        Menu {
                            // iPad: Add Create Connection to dropdown (visible when toolbar overflows in portrait)
                            if horizontalSizeClass == .regular {
                                Button {
                                    createNewEdge()
                                } label: {
                                    Label(AppStrings.Connections.createConnection, systemImage: "plus")
                                }
                                Divider()
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
                        
                        Button(action: createNewEdge) {
                            Image(systemName: "plus")
                        }
                        
                        IRSessionPickerButton()
                            .environmentObject(appState)
                            .environment(\.modelContext, modelContext)
                    }
                }
            }
        }
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
        .alert(AppStrings.AssetsExtra.deleteConnection, isPresented: $showDeleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                edgeToDelete = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let edge = edgeToDelete {
                    performDelete(edge)
                } else if !selectedEdgeIds.isEmpty {
                    performMultiDelete()
                }
            }
        } message: {
            if edgeToDelete != nil {
                Text(AppStrings.Connections.deleteConnectionConfirmSingle)
            } else if !selectedEdgeIds.isEmpty {
                Text(AppStrings.Connections.deleteConnectionsConfirm(selectedEdgeIds.count))
            }
        }
        .alert(AppStrings.AssetsExtra.deleteFailed, isPresented: $showDeleteError) {
            Button(AppStrings.Common.ok, role: .cancel) { }
        } message: {
            Text(deleteErrorMessage)
        }
        // edit edge
        .fullScreenCover(item: $selectedEdge) { edge in
            EditEdgeDetailViewV3(edge: edge)
        }
        // create new edge
        .fullScreenCover(isPresented: $showingAddConnection) {
            AddConnectionViewV2(
                availableNodes: availableNodes,
                availableEdgeClasses: availableEdgeClasses,
                sld: diagram,
                onSave: { edge in
                    Task {
                        // EdgeService handles adding to diagram and saving
                        await EdgeService.createEdge(
                            edge: edge,
                            diagram: diagram,
                            networkState: networkState,
                            modelContext: modelContext
                        ) { success, message in
                            if !success {
                                // Show error
                                deleteErrorMessage = message ?? "Failed to create connection"
                                showDeleteError = true
                            }
                        }
                        
                        showingAddConnection = false
                    }
                },
                onCancel: {
                    showingAddConnection = false
                }
            )
        }
    }
    
    // View builder for edge row
    @ViewBuilder
    private func edgeRow(_ edge: EdgeV2, nodeById: [UUID: NodeV2]) -> some View {
        // PERFORMANCE: O(1) dictionary lookups instead of O(n) linear searches
        // "Broken" only when an ID exists but the referenced node is missing (not when intentionally unassigned)
        let isBroken = (edge.source.map { nodeById[$0] == nil } ?? false) ||
                        (edge.target.map { nodeById[$0] == nil } ?? false)

        Button(action: {
            selectedEdge = edge
        }) {
            HStack(spacing: 12) {
                // Source side
                HStack {
                    nodeDisplay(for: edge.source, alignment: .leading, isBroken: isBroken, nodeById: nodeById)
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 44) // Fixed height for consistent layout

                // Arrow in fixed position
                Image(systemName: "arrow.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(isBroken ? .red : .secondary)
                    .frame(width: 20)

                // Target side
                HStack {
                    nodeDisplay(for: edge.target, alignment: .leading, isBroken: isBroken, nodeById: nodeById)
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 44) // Fixed height for consistent layout

                // Right side metadata
                HStack(spacing: 8) {
                    if isBroken {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                            .imageScale(.medium)
                    }

                    if showArcFlashStatus {
                        Image(systemName: edge.af_isComplete ? "checkmark.circle.fill" : "xmark.circle.fill")
                            .foregroundColor(edge.af_isComplete ? .green : .red)
                            .imageScale(.medium)
                    }

                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // View builder for multi-select edge row
    @ViewBuilder
    private func multiSelectEdgeRow(_ edge: EdgeV2, nodeById: [UUID: NodeV2]) -> some View {
        // PERFORMANCE: O(1) dictionary lookups instead of O(n) linear searches
        let isBroken = (edge.source.map { nodeById[$0] == nil } ?? false) ||
                        (edge.target.map { nodeById[$0] == nil } ?? false)

        Button(action: {
            toggleEdgeSelection(edge.id)
        }) {
            HStack(spacing: 12) {
                // Selection checkbox
                Image(systemName: selectedEdgeIds.contains(edge.id) ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(selectedEdgeIds.contains(edge.id) ? .accentColor : .gray)
                    .imageScale(.large)

                // Source side
                HStack {
                    nodeDisplay(for: edge.source, alignment: .leading, isBroken: isBroken, nodeById: nodeById)
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 44) // Fixed height for consistent layout

                // Arrow in fixed position
                Image(systemName: "arrow.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(isBroken ? .red : .secondary)
                    .frame(width: 20)

                // Target side
                HStack {
                    nodeDisplay(for: edge.target, alignment: .leading, isBroken: isBroken, nodeById: nodeById)
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 44) // Fixed height for consistent layout

                // Right side metadata
                HStack(spacing: 8) {
                    if isBroken {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                            .imageScale(.medium)
                    }

                    if showArcFlashStatus {
                        Image(systemName: edge.af_isComplete ? "checkmark.circle.fill" : "xmark.circle.fill")
                            .foregroundColor(edge.af_isComplete ? .green : .red)
                            .imageScale(.medium)
                    }
                }
            }
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // Helper method to display a node with optional parent
    @ViewBuilder
    private func nodeDisplay(for nodeId: UUID?, alignment: HorizontalAlignment, isBroken: Bool = false, nodeById: [UUID: NodeV2]) -> some View {
        // PERFORMANCE: O(1) dictionary lookups instead of O(n) linear searches
        if let nodeId, let node = nodeById[nodeId] {
            if let parentId = node.parent_id,
               let parentNode = nodeById[parentId] {
                // Has parent - show both labels
                VStack(alignment: alignment, spacing: 2) {
                    Text(parentNode.label)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(isBroken ? .red : .primary)
                        .lineLimit(1)
                        .truncationMode(.tail)

                    Text(node.label)
                        .font(.caption)
                        .foregroundStyle(isBroken ? .red.opacity(0.8) : .secondary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
            } else {
                // No parent - center the label vertically
                Text(node.label)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(isBroken ? .red : .primary)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxHeight: .infinity)
            }
        } else if nodeId != nil {
            // Node ID exists but node not found — Missing Node (red)
            Text(AppStrings.Connections.missingNode)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(.red)
                .italic()
                .lineLimit(1)
                .frame(maxHeight: .infinity)
        } else {
            // No node ID — Not Assigned (gray)
            Text(AppStrings.Connections.notAssigned)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(.secondary)
                .italic()
                .lineLimit(1)
                .frame(maxHeight: .infinity)
        }
    }
    
    // Helper method to get display text for an edge
    private func edgeDisplayText(for edge: EdgeV2, nodeById: [UUID: NodeV2]) -> String {
        // Build source display text
        let sourceText: String
        if let sourceId = edge.source, let sourceNode = nodeById[sourceId] {
            if let parentId = sourceNode.parent_id, let parentNode = nodeById[parentId] {
                sourceText = "\(parentNode.label) (\(sourceNode.label))"
            } else {
                sourceText = sourceNode.label
            }
        } else if edge.source != nil {
            sourceText = AppStrings.Connections.missingNode
        } else {
            sourceText = AppStrings.Connections.notAssigned
        }

        // Build target display text
        let targetText: String
        if let targetId = edge.target, let targetNode = nodeById[targetId] {
            if let parentId = targetNode.parent_id, let parentNode = nodeById[parentId] {
                targetText = "\(parentNode.label) (\(targetNode.label))"
            } else {
                targetText = targetNode.label
            }
        } else if edge.target != nil {
            targetText = AppStrings.Connections.missingNode
        } else {
            targetText = AppStrings.Connections.notAssigned
        }

        return "\(sourceText) → \(targetText)"
    }
    
    // Computed properties for filtering and grouping
    private var filteredEdges: [EdgeV2] {
        let activeEdges = diagram.edges.filter { !$0.is_deleted }
        // PERFORMANCE: Build dictionary once for O(1) lookups during sort
        let nodeLookup = nodeById

        if connectionSearchText.isEmpty {
            return activeEdges.sorted { edge1, edge2 in
                edgeDisplayText(for: edge1, nodeById: nodeLookup) < edgeDisplayText(for: edge2, nodeById: nodeLookup)
            }
        } else {
            return activeEdges.filter { edge in
                let displayText = edgeDisplayText(for: edge, nodeById: nodeLookup)
                return displayText.localizedCaseInsensitiveContains(connectionSearchText) ||
                       (edge.edge_class?.name.localizedCaseInsensitiveContains(connectionSearchText) ?? false)
            }.sorted { edge1, edge2 in
                edgeDisplayText(for: edge1, nodeById: nodeLookup) < edgeDisplayText(for: edge2, nodeById: nodeLookup)
            }
        }
    }
    
    // PERFORMANCE: Dictionary for O(1) node lookups instead of O(n) linear searches
    private var nodeById: [UUID: NodeV2] {
        Dictionary(uniqueKeysWithValues: diagram.nodes.map { ($0.id, $0) })
    }

    private var availableNodes: [NodeV2] {
        diagram.nodes.filter { !$0.is_deleted }.sorted { $0.label < $1.label }
    }
    
    private var availableEdgeClasses: [EdgeClass] {
        let descriptor = FetchDescriptor<EdgeClass>()
        let edgeClasses = (try? modelContext.fetch(descriptor)) ?? []
        return edgeClasses
            .filter { !$0.is_deleted }
            .sorted { $0.name < $1.name }
    }
    
    private func createNewEdge() {
        selectedEdgeClass = nil
        showingAddConnection = true
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
            // Don't show error alert for auth errors — the re-auth sheet handles those
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
    
    private func deleteEdge(_ edge: EdgeV2) {
        edgeToDelete = edge
        showDeleteConfirmation = true
    }
    
    private func performDelete(_ edge: EdgeV2) {
        // Clear the reference before deletion
        let edgeId = edge.id
        edgeToDelete = nil
        
        EdgeService.deleteEdges(
            edgeIds: Set([edgeId]),
            diagram: diagram,
            networkState: networkState,
            modelContext: modelContext
        ) { success, message in
            if success {
                // Optionally show a success message if offline
                if let message = message {
                    // The deletion was queued for later sync
                    AppLogger.log(.info, message, category: .node)
                }
            } else {
                deleteErrorMessage = message ?? "Failed to delete connection"
                showDeleteError = true
            }
        }
    }
    
    private func performMultiDelete() {
        // Copy the IDs before clearing the selection
        let edgeIdsToDelete = selectedEdgeIds
        exitMultiSelectMode()
        
        EdgeService.deleteEdges(
            edgeIds: edgeIdsToDelete,
            diagram: diagram,
            networkState: networkState,
            modelContext: modelContext
        ) { success, message in
            if success {
                // Optionally show a success message if offline
                if let message = message {
                    // The deletion was queued for later sync
                    AppLogger.log(.info, message, category: .node)
                }
            } else {
                deleteErrorMessage = message ?? "Failed to delete connections"
                showDeleteError = true
            }
        }
    }
    
    private func enterMultiSelectMode() {
        withAnimation {
            isMultiSelectMode = true
            selectedEdgeIds.removeAll()
        }
    }
    
    private func exitMultiSelectMode() {
        withAnimation {
            isMultiSelectMode = false
            selectedEdgeIds.removeAll()
        }
    }
    
    private func toggleEdgeSelection(_ edgeId: UUID) {
        if selectedEdgeIds.contains(edgeId) {
            selectedEdgeIds.remove(edgeId)
        } else {
            selectedEdgeIds.insert(edgeId)
        }
    }
}
