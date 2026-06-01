//
//  RoomDetailView.swift
//  Egalvanic PZ
//
//  Detail view for a room showing hierarchical asset list
//

import SwiftUI
import SwiftData

struct RoomDetailView: View {
    let room: Room

    // Determines if presented as modal (needs Done button) or pushed (uses back button)
    let isModal: Bool

    init(room: Room, isModal: Bool = true) {
        self.room = room
        self.isModal = isModal
    }

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var sldService: SLDService

    @State private var nodes: [NodeV2] = []
    @State private var expandedNodes: Set<UUID> = []
    @State private var searchText = ""
    @State private var showingAddNode = false
    @State private var editNodeContext: EditNodeContext?
    @State private var editOCPContext: EditNodeContext?  // For OCP editor (long-press)
    @State private var focusContext: FocusContext?       // ZP-2331: focused-mode editor
    @State private var errorMessage: String?
    @State private var showQRScanner = false

    // ZP-2331: Quick Count + Photo Walkthrough from the non-WO Locations
    // tree. Both views accept an optional session and gracefully skip
    // the session-node mapping / append when session is nil.
    @State private var showingQuickCount = false
    @State private var showingPhotoWalkthrough = false
    @State private var showingLinkExisting = false  // ZP-2331: bulk relocator

    struct EditNodeContext: Identifiable {
        let id = UUID()
        let node: NodeV2
        let sld: SLDV2
    }

    /// ZP-2331: routes a focused-mode action (Add IR Photos, Collect AF
    /// Data, etc.) into a single `.fullScreenCover` that presents
    /// `EditNodeDetailViewV3` in the requested focus mode.
    struct FocusContext: Identifiable {
        let id = UUID()
        let node: NodeV2
        let sld: SLDV2
        let focusMode: NodeDetailFocusMode
    }

    private var sld: SLDV2? {
        let result = room.floor?.building?.sld
        AppLogger.log(.debug, "[RoomDetail] Computing sld property: floor=\(room.floor?.name ?? "nil"), building=\(room.floor?.building?.name ?? "nil"), sld=\(result?.id.uuidString ?? "nil")", category: .location)
        return result
    }

    // Build parent-child relationships for hierarchical display
    private func buildNodeHierarchy() -> (topLevelNodes: [NodeV2], childrenMap: [UUID: [NodeV2]]) {
        let nodeMap = Dictionary(uniqueKeysWithValues: nodes.map { ($0.id, $0) })
        var childrenMap: [UUID: [NodeV2]] = [:]
        var topLevelNodes: [NodeV2] = []

        for node in nodes {
            if let parentId = node.parent_id, nodeMap[parentId] != nil {
                childrenMap[parentId, default: []].append(node)
            } else {
                topLevelNodes.append(node)
            }
        }

        // Sort children
        for parentId in childrenMap.keys {
            childrenMap[parentId]?.sort { $0.label < $1.label }
        }

        return (topLevelNodes.sorted { $0.label < $1.label }, childrenMap)
    }

    // Filter nodes based on search
    private func nodeMatchesSearch(_ node: NodeV2) -> Bool {
        guard !searchText.isEmpty else { return true }
        let searchLower = searchText.lowercased()
        return node.label.lowercased().contains(searchLower) ||
               node.type.lowercased().contains(searchLower) ||
               (node.node_class?.name.lowercased().contains(searchLower) ?? false)
    }

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

    private func anyDescendantMatchesSearch(of node: NodeV2, using childrenMap: [UUID: [NodeV2]]) -> Bool {
        let descendants = getAllDescendants(of: node, using: childrenMap)
        return descendants.contains { nodeMatchesSearch($0) }
    }

    private var filteredHierarchy: (topLevelNodes: [NodeV2], childrenMap: [UUID: [NodeV2]]) {
        let hierarchy = buildNodeHierarchy()

        if searchText.isEmpty {
            return hierarchy
        }

        // Filter to include nodes that match or have matching descendants
        let filteredTopLevel = hierarchy.topLevelNodes.filter { node in
            nodeMatchesSearch(node) || anyDescendantMatchesSearch(of: node, using: hierarchy.childrenMap)
        }

        return (filteredTopLevel, hierarchy.childrenMap)
    }

    var body: some View {
        ZStack {
                VStack(spacing: 0) {
                    // Search bar with QR scanner
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

                        // QR Scanner button
                        Button(action: {
                            showQRScanner = true
                        }) {
                            Image(systemName: "qrcode.viewfinder")
                                .foregroundColor(.blue)
                        }
                    }
                    .padding(8)
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(8)
                    .padding(.horizontal)
                    .padding(.top, 8)

                if nodes.isEmpty {
                    ContentUnavailableView(
                        AppStrings.Locations.noAssets,
                        systemImage: "cube.box",
                        description: Text(AppStrings.Sessions.tapPlusToAddAssets)
                    )
                    .frame(maxHeight: .infinity)
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 8) {
                            let hierarchy = filteredHierarchy
                            ForEach(hierarchy.topLevelNodes) { node in
                                RoomNodeRow(
                                    node: node,
                                    children: hierarchy.childrenMap[node.id] ?? [],
                                    isExpanded: expandedNodes.contains(node.id),
                                    expandedNodes: $expandedNodes,
                                    childrenMap: hierarchy.childrenMap,
                                    searchText: searchText,
                                    onTap: { tappedNode in
                                        AppLogger.log(.debug, "[RoomDetail] Node tapped: \(tappedNode.label) (id: \(tappedNode.id))", category: .location)
                                        if let currentSLD = sld {
                                            AppLogger.log(.debug, "[RoomDetail] Creating EditNodeContext with node and SLD", category: .location)
                                            editNodeContext = EditNodeContext(node: tappedNode, sld: currentSLD)
                                        } else {
                                            AppLogger.log(.error, "[RoomDetail] SLD is nil, cannot create context", category: .location)
                                        }
                                    },
                                    onEditOCP: { tappedNode in
                                        if let currentSLD = sld {
                                            editOCPContext = EditNodeContext(node: tappedNode, sld: currentSLD)
                                        }
                                    },
                                    onFocusAction: { tappedNode, mode in
                                        if let currentSLD = sld {
                                            focusContext = FocusContext(node: tappedNode, sld: currentSLD, focusMode: mode)
                                        }
                                    }
                                )
                            }
                        }
                        .padding()
                        .padding(.bottom, 80) // Space for floating button
                    }
                    .scrollDismissesKeyboard(.interactively)
                }
                }

                // Floating Action Menu — single asset, Quick Count, or
                // Photo Walkthrough. The latter two used to be WO-only;
                // ZP-2331 brings them to the non-WO Locations tree.
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Menu {
                            Button {
                                showingAddNode = true
                            } label: {
                                Label("Add Asset", systemImage: "plus")
                            }
                            Button {
                                showingQuickCount = true
                            } label: {
                                Label("Quick Count", systemImage: "number.square")
                            }
                            Button {
                                showingPhotoWalkthrough = true
                            } label: {
                                Label("Photo Walkthrough", systemImage: "camera.viewfinder")
                            }
                            Button {
                                showingLinkExisting = true
                            } label: {
                                Label("Link Existing Asset", systemImage: "link.badge.plus")
                            }
                        } label: {
                            Image(systemName: "plus")
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
            .navigationTitle(room.fullPath)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                // Only show Done button when presented as modal (push navigation has back button)
                if isModal {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(AppStrings.Common.done) {
                            dismiss()
                        }
                    }
                }
            }
            .onAppear {
                loadNodes()
            }
            .fullScreenCover(isPresented: $showingAddNode) {
                if let sld = sld {
                    AddAssetViewV2(
                        availableLocations: [],
                        availableNodeClasses: {
                            let query = FetchDescriptor<NodeClass>(
                                predicate: #Predicate { nodeClass in
                                    !nodeClass.is_deleted
                                }
                            )
                            return (try? modelContext.fetch(query)) ?? []
                        }(),
                        sld: sld,
                        preselectedRoom: room,
                        onSave: { node, photos, irPhotos in
                            // Set room relationship
                            node.room = room

                            // Insert into context
                            modelContext.insert(node)
                            photos.forEach { modelContext.insert($0) }
                            irPhotos.forEach { modelContext.insert($0) }

                            do {
                                try modelContext.save()
                                showingAddNode = false
                                loadNodes()

                                // Sync to server
                                Task {
                                    await NodeService.createNewNodeWithPhotosAndIR(
                                        node: node,
                                        photos: photos,
                                        irPhotos: irPhotos,
                                        networkState: networkState,
                                        modelContext: modelContext
                                    )
                                }
                            } catch {
                                errorMessage = "Failed to save asset: \(error.localizedDescription)"
                            }
                        },
                        onCancel: {
                            showingAddNode = false
                        }
                    )
                    .environmentObject(appState)
                    .environmentObject(networkState)
                }
            }
            .fullScreenCover(item: $editNodeContext) { context in
                EditNodeDetailViewV3(
                    node: context.node,
                    sld: context.sld,
                    lockRoomSelection: true,
                    onDismiss: {
                        AppLogger.log(.debug, "[RoomDetail] onDismiss called", category: .location)
                        editNodeContext = nil
                        loadNodes()
                    }
                )
                .environmentObject(appState)
                .environmentObject(networkState)
                .environmentObject(sldService)
                .onAppear {
                    AppLogger.log(.debug, "[RoomDetail] EditNodeDetailViewV3 appeared with node: \(context.node.label)", category: .location)
                }
            }
            .fullScreenCover(item: $editOCPContext) { context in
                EditNodeDetailViewV3(
                    node: context.node,
                    sld: context.sld,
                    lockRoomSelection: true,
                    onDismiss: {
                        editOCPContext = nil
                        loadNodes()
                    },
                    focusMode: .ocp
                )
                .environmentObject(appState)
                .environmentObject(networkState)
                .environmentObject(sldService)
            }
            // ZP-2331: focused-mode actions from the row's long-press
            // menu (Add IR Photos / Collect AF Data / Collect COM Data /
            // Add Issue / Add Task). Same EditNodeDetailViewV3 used
            // elsewhere — just driven by the focus mode the menu chose.
            .fullScreenCover(item: $focusContext) { context in
                EditNodeDetailViewV3(
                    node: context.node,
                    sld: context.sld,
                    lockRoomSelection: true,
                    onDismiss: {
                        focusContext = nil
                        loadNodes()
                    },
                    focusMode: context.focusMode
                )
                .environmentObject(appState)
                .environmentObject(networkState)
                .environmentObject(sldService)
            }
            // ZP-2331: Quick Count + Photo Walkthrough without a WO.
            // Both views accept `session: nil`; created assets attach
            // only to the room.
            .fullScreenCover(isPresented: $showingQuickCount) {
                QuickCountView(
                    session: nil,
                    room: room,
                    onComplete: {
                        showingQuickCount = false
                        loadNodes()
                    },
                    onCancel: { showingQuickCount = false }
                )
                .environmentObject(networkState)
                .environmentObject(appState)
            }
            .fullScreenCover(isPresented: $showingPhotoWalkthrough) {
                PhotoWalkthroughView(
                    session: nil,
                    room: room,
                    onComplete: {
                        showingPhotoWalkthrough = false
                        loadNodes()
                    },
                    onCancel: { showingPhotoWalkthrough = false }
                )
                .environmentObject(networkState)
                .environmentObject(appState)
            }
            .fullScreenCover(isPresented: $showingLinkExisting) {
                if let sld = sld {
                    LinkExistingAssetsSheet(
                        room: room,
                        sld: sld,
                        onDone: {
                            showingLinkExisting = false
                            loadNodes()
                        },
                        onCancel: { showingLinkExisting = false }
                    )
                    .environmentObject(networkState)
                }
            }
            .alert(AppStrings.CommonExtra.error, isPresented: .constant(errorMessage != nil)) {
                Button(AppStrings.Common.ok) {
                    errorMessage = nil
                }
            } message: {
                if let errorMessage = errorMessage {
                    Text(errorMessage)
                }
            }
            .sheet(isPresented: $showQRScanner) {
                QRCodeScannerView(scannedCode: $searchText) { scannedCode in
                    searchText = scannedCode
                }
            }
    }

    private func loadNodes() {
        do {
            nodes = try LocationHierarchyService.getNodesForRoom(room, modelContext: modelContext)
        } catch {
            errorMessage = "Failed to load assets: \(error.localizedDescription)"
        }
    }
}

// MARK: - Room Node Row

struct RoomNodeRow: View {
    let node: NodeV2
    let children: [NodeV2]
    let isExpanded: Bool
    @Binding var expandedNodes: Set<UUID>
    let childrenMap: [UUID: [NodeV2]]
    var searchText: String = ""
    let onTap: (NodeV2) -> Void  // Changed to accept node parameter
    var onEditOCP: ((NodeV2) -> Void)? = nil  // Changed to accept node parameter
    // ZP-2331: routes "Add IR Photos", "Collect AF Data", etc. through
    // a single callback. Each opens EditNodeDetailViewV3 in the
    // matching focus mode — same surface the WO RoomDetailView uses,
    // minus the session-only entries (Mark Task Complete, Link Task).
    var onFocusAction: ((NodeV2, NodeDetailFocusMode) -> Void)? = nil

    private var hasChildren: Bool {
        !children.isEmpty
    }

    private var isDirectMatch: Bool {
        guard !searchText.isEmpty else { return false }
        let searchLower = searchText.lowercased()
        return node.label.lowercased().contains(searchLower) ||
               node.type.lowercased().contains(searchLower)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Node Row
            HStack(spacing: 12) {
                // Expand/collapse button - only show chevron if has children
                Button(action: {
                    if hasChildren {
                        if expandedNodes.contains(node.id) {
                            expandedNodes.remove(node.id)
                        } else {
                            expandedNodes.insert(node.id)
                        }
                    }
                }) {
                    if hasChildren {
                        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(isExpanded ? .blue : .secondary)
                            .frame(width: 44, height: 44)
                            .contentShape(Rectangle())
                    } else {
                        // Maintain spacing even when no chevron
                        Color.clear
                            .frame(width: 44, height: 44)
                    }
                }
                .buttonStyle(.plain)

                // Main content - tappable
                Button(action: { onTap(node) }) {
                    HStack(spacing: 12) {
                        NodeTypeIcon(
                            style: node.node_class?.style,
                            size: 18,
                            color: isExpanded ? .blue : .secondary
                        )

                        VStack(alignment: .leading, spacing: 2) {
                            Text(node.label)
                                .font(.subheadline)
                                .fontWeight(isExpanded ? .semibold : .medium)
                                .foregroundColor(isExpanded ? .blue : (isDirectMatch ? .blue : .primary))
                                .background(
                                    isDirectMatch ? Color.blue.opacity(0.1) : Color.clear
                                )

                            if let nodeClassName = node.node_class?.name {
                                HStack(spacing: 6) {
                                    Text(nodeClassName)
                                        .font(.caption)
                                        .foregroundColor(.secondary)

                                    if hasChildren {
                                        Text("•")
                                            .font(.caption)
                                            .foregroundColor(.secondary)

                                        Label("\(children.count)", systemImage: "cube")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                }
                            } else if hasChildren {
                                Label(AppStrings.Locations.childrenCount(children.count), systemImage: "cube")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
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
                    onTap(node)
                } label: {
                    Label(AppStrings.AssetsExtra.editAsset, systemImage: "pencil")
                }

                // Only show "Edit OCP" for box-type nodes that can have children
                if node.parent_id == nil && (node.node_class?.box ?? false) {
                    Button {
                        onEditOCP?(node)
                    } label: {
                        Label(AppStrings.Sessions.editOCP, systemImage: "cpu")
                    }
                }

                if let onFocusAction = onFocusAction {
                    Divider()
                    Button {
                        onFocusAction(node, .irPhotos)
                    } label: {
                        Label(AppStrings.Sessions.addIRPhotos, systemImage: "camera.filters")
                    }
                    Button {
                        onFocusAction(node, .collectAFData)
                    } label: {
                        Label(AppStrings.Sessions.collectAFData, systemImage: "bolt.circle")
                    }
                    Button {
                        onFocusAction(node, .collectCOMData)
                    } label: {
                        Label(AppStrings.Sessions.collectCOMData, systemImage: "gauge.with.dots.needle.33percent")
                    }
                    Button {
                        onFocusAction(node, .issues)
                    } label: {
                        Label(AppStrings.Sessions.addIssue, systemImage: "exclamationmark.triangle")
                    }
                    Button {
                        onFocusAction(node, .tasks)
                    } label: {
                        Label(AppStrings.Sessions.addTask, systemImage: "checklist")
                    }
                }
            }

            // Expanded children
            if isExpanded && hasChildren {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(children) { child in
                        RoomNodeRow(
                            node: child,
                            children: childrenMap[child.id] ?? [],
                            isExpanded: expandedNodes.contains(child.id),
                            expandedNodes: $expandedNodes,
                            childrenMap: childrenMap,
                            searchText: searchText,
                            onTap: onTap,
                            onEditOCP: onEditOCP,
                            onFocusAction: onFocusAction
                        )
                        .padding(.leading, 24)
                    }
                }
            }
        }
    }
}

// MARK: - Link Existing Assets (ZP-2331)

/// Lightweight bulk-relocator: lists every node on the SLD that isn't
/// already in this room (or has no room), lets the user pick any
/// number, and reassigns them to this room in one go. Useful for
/// fixing up mislocated assets without opening each one individually.
struct LinkExistingAssetsSheet: View {
    let room: Room
    let sld: SLDV2
    let onDone: () -> Void
    let onCancel: () -> Void

    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState

    @Query private var candidates: [NodeV2]
    @State private var selection: Set<UUID> = []
    @State private var searchText: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    init(room: Room, sld: SLDV2, onDone: @escaping () -> Void, onCancel: @escaping () -> Void) {
        self.room = room
        self.sld = sld
        self.onDone = onDone
        self.onCancel = onCancel

        let sldId = sld.id
        let roomId = room.id
        // Pull every non-deleted node on this SLD that isn't already in
        // this room. Room == nil shows up here too — those are the
        // unlocated nodes that often need to be placed.
        _candidates = Query(
            filter: #Predicate<NodeV2> { node in
                node.sld?.id == sldId &&
                !node.is_deleted &&
                node.room?.id != roomId
            },
            sort: [SortDescriptor(\NodeV2.label)]
        )
    }

    private var filteredCandidates: [NodeV2] {
        guard !searchText.isEmpty else { return candidates }
        let needle = searchText.lowercased()
        return candidates.filter { node in
            node.label.lowercased().contains(needle) ||
            (node.node_class?.name.lowercased().contains(needle) ?? false)
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                TextField("Search assets…", text: $searchText)
                    .textFieldStyle(.roundedBorder)
                    .padding(.horizontal)
                    .padding(.vertical, 8)

                if filteredCandidates.isEmpty {
                    Spacer()
                    Text(candidates.isEmpty
                         ? "Every asset on this SLD is already in this room."
                         : "No assets match your search.")
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding()
                    Spacer()
                } else {
                    List(filteredCandidates, id: \.id) { node in
                        Button {
                            if selection.contains(node.id) {
                                selection.remove(node.id)
                            } else {
                                selection.insert(node.id)
                            }
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: selection.contains(node.id)
                                      ? "checkmark.circle.fill"
                                      : "circle")
                                    .foregroundStyle(selection.contains(node.id) ? .blue : .secondary)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(node.label).foregroundStyle(.primary)
                                    if let currentRoomPath = node.room?.fullPath {
                                        Text(currentRoomPath)
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                    } else {
                                        Text("No room")
                                            .font(.caption)
                                            .foregroundStyle(.orange)
                                    }
                                }
                                Spacer()
                            }
                        }
                        .buttonStyle(.plain)
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Move to \(room.name)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel, action: onCancel)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: relocate) {
                        if isSaving {
                            ProgressView()
                        } else {
                            Text("Move (\(selection.count))")
                                .fontWeight(.semibold)
                        }
                    }
                    .disabled(selection.isEmpty || isSaving)
                }
            }
            .alert("Failed to move assets",
                   isPresented: .constant(errorMessage != nil)) {
                Button(AppStrings.Common.ok) { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "")
            }
        }
    }

    private func relocate() {
        guard !selection.isEmpty else { return }
        isSaving = true
        let chosen = candidates.filter { selection.contains($0.id) }
        for node in chosen {
            node.room = room
            node.lastModifiedAt = Date()
        }
        do {
            try modelContext.save()
        } catch {
            errorMessage = error.localizedDescription
            isSaving = false
            return
        }

        // Sync each node — mirror the deleteTask pattern: try direct
        // when online, fall back to the queue on failure.
        Task {
            for node in chosen {
                if networkState.mode == .online {
                    do {
                        _ = try await APIClient.shared.updateNode(node)
                    } catch {
                        networkState.enqueue(SyncOp(target: .node, operation: .update, node: node))
                    }
                } else {
                    networkState.enqueue(SyncOp(target: .node, operation: .update, node: node))
                }
            }
            await MainActor.run {
                isSaving = false
                onDone()
            }
        }
    }
}
