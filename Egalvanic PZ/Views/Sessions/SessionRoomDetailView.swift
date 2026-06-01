//
//  SessionRoomDetailView.swift
//  Egalvanic PZ
//
//  Displays assets within a specific room for a session, with hierarchical view
//

import SwiftUI
import SwiftData

struct SessionRoomDetailView: View {
    @StateObject private var viewModel: SessionRoomDetailViewModel

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var sldService: SLDService

    @StateObject private var metricFilterSettings = AssetMetricFilterSettings.shared

    // Determines if presented as modal (needs Done button) or pushed (uses back button)
    let isModal: Bool

    // ZP-2198: Copy Assets to Rooms wizard
    @State private var showCopyAssetsSheet = false

    // MARK: - Initialization

    init(session: IRSession, room: Room, autoOpenNodeForIR: NodeV2? = nil, isModal: Bool = true) {
        _viewModel = StateObject(wrappedValue: SessionRoomDetailViewModel(
            session: session,
            room: room,
            autoOpenNodeForIR: autoOpenNodeForIR
        ))
        self.isModal = isModal
    }

    // Convenience accessors
    private var session: IRSession { viewModel.session }
    private var room: Room { viewModel.room }

    var body: some View {
        mainContentView
    }

    private var mainContentView: some View {
        let cachedHierarchy = viewModel.hierarchy
        let cachedTaskNodeCounts = viewModel.taskNodeCounts
        let cachedIssueNodeCounts = viewModel.issueNodeCounts
        let cachedSessionUserTaskIds = viewModel.sessionUserTaskIds
        let cachedSessionIssueIds = viewModel.sessionIssueIds
        return ZStack {
            VStack(spacing: 0) {
                    // Search bar with QR scanner and filter button
                    HStack(spacing: 8) {
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .foregroundColor(.secondary)

                            TextField(AppStrings.CommonExtra.searchAssets, text: $viewModel.searchText)
                                .textFieldStyle(PlainTextFieldStyle())

                            if !viewModel.searchText.isEmpty {
                                Button(action: {
                                    viewModel.searchText = ""
                                }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundColor(.secondary)
                                }
                            }

                            // QR Scanner button
                            Button(action: {
                                viewModel.showQRScanner = true
                            }) {
                                Image(systemName: "qrcode.viewfinder")
                                    .foregroundColor(.blue)
                            }
                        }
                        .padding(8)
                        .background(Color(UIColor.systemGray6))
                        .cornerRadius(8)

                        // Filter menu to toggle individual metrics
                        // AssetMetricFilterMenu(settings: metricFilterSettings)
                    }
                    .padding(.horizontal)
                    .padding(.top, 8)

                    // Room breadcrumb
                    HStack(spacing: 4) {
                        Text(room.fullPath)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                    .padding(.horizontal)
                    .padding(.top, 8)
                    .padding(.bottom, 4)

                    if viewModel.roomAssets.isEmpty {
                        ContentUnavailableView(
                            AppStrings.Sessions.noAssets,
                            systemImage: "cube.box",
                            description: Text(AppStrings.Sessions.tapPlusToAddAssets)
                        )
                        .frame(minHeight: 400)
                    } else {
                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: 8) {
                                ForEach(cachedHierarchy.topLevelAssets) { asset in
                                    SessionRoomAssetRow(
                                        session: session,
                                        asset: asset,
                                        children: cachedHierarchy.childrenMap[asset.id] ?? [],
                                        isExpanded: viewModel.expandedNodes.contains(asset.id),
                                        childrenMap: cachedHierarchy.childrenMap,
                                        searchText: viewModel.searchText,
                                        edges: session.sld.edges,
                                        metricFilterSettings: metricFilterSettings,
                                        viewModel: viewModel,
                                        taskNodeCounts: cachedTaskNodeCounts,
                                        issueNodeCounts: cachedIssueNodeCounts,
                                        sessionUserTaskIds: cachedSessionUserTaskIds,
                                        sessionIssueIds: cachedSessionIssueIds
                                    )
                                }
                            }
                            .padding(.horizontal)
                            .padding(.vertical)
                            .padding(.bottom, 80) // Space for floating button
                        }
                        .scrollDismissesKeyboard(.interactively)
                    }
                }

                // Floating Action Menu - disabled if session is not active
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        VStack(spacing: 12) {
                            // ZP-2198: Copy FAB sits above the "+" FAB and only appears when
                            // the room has session-mapped assets to copy and the session is active.
                            if session.active && !cachedHierarchy.topLevelAssets.isEmpty {
                                Button {
                                    showCopyAssetsSheet = true
                                } label: {
                                    Image(systemName: "doc.on.doc")
                                        .font(.title3)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.white)
                                        .frame(width: 48, height: 48)
                                        .background(Color.blue)
                                        .clipShape(Circle())
                                        .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                                }
                                .accessibilityLabel(AppStrings.CopyAssetsRooms.fabLabel)
                            }

                            Menu {
                                Button {
                                    viewModel.showingQuickCountDirect = true
                                } label: {
                                    Label(AppStrings.Sessions.quickCountLabel, systemImage: "number.square")
                                }

                                Button {
                                    viewModel.showingPhotoWalkthroughDirect = true
                                } label: {
                                    Label(AppStrings.Sessions.photoWalkthroughLabel, systemImage: "camera.viewfinder")
                                }

                                Button {
                                    viewModel.showingAddNodes = true
                                } label: {
                                    Label(AppStrings.Sessions.linkExistingAssetLabel, systemImage: "link.badge.plus")
                                }

                                Button {
                                    viewModel.showingAddAssetDirect = true
                                } label: {
                                    Label(AppStrings.Assets.newAsset, systemImage: "plus.square")
                                }
                            } label: {
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
                        }
                        .padding(.trailing, 20)
                        .padding(.bottom, 20)
                    }
                }
            }
            .navigationTitle(AppStrings.Sessions.assetsInRoom)
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
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        ForEach(AssetOverlayFilter.allCases, id: \.self) { filter in
                            Button {
                                metricFilterSettings.assetOverlayFilter = filter
                            } label: {
                                Label {
                                    Text(filter.rawValue)
                                } icon: {
                                    if metricFilterSettings.assetOverlayFilter == filter {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        Image(systemName: metricFilterSettings.assetOverlayFilter == .none
                            ? "line.3.horizontal.decrease.circle"
                            : "line.3.horizontal.decrease.circle.fill")
                            .foregroundColor(metricFilterSettings.assetOverlayFilter == .none ? .secondary : .blue)
                    }
                }
            }
        .fullScreenCover(isPresented: $viewModel.showingAddNodes) {
            RoomNodeAdditionView(
                viewModel: viewModel,
                onComplete: {
                    // Refresh will happen automatically through SwiftData
                }
            )
            .environmentObject(networkState)
            .environmentObject(appState)
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $viewModel.showError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(viewModel.errorMessage)
        }
        .overlay {
            if viewModel.isUpdating {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.CommonExtra.updating)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
        .fullScreenCover(item: $viewModel.nodeIRDetail) { node in
            EditNodeDetailViewV3(
                node: node,
                sld: session.sld,
                onDismiss: { viewModel.nodeIRDetail = nil },
                focusMode: networkState.quickQRAction.focusMode
            )
            .environmentObject(networkState)
            .environmentObject(appState)
            .environmentObject(sldService)
        }
        .sheet(isPresented: $viewModel.showQRScanner) {
            QRCodeScannerView(scannedCode: $viewModel.searchText) { scannedCode in
                // When a code is scanned, it automatically populates the search field
                viewModel.searchText = scannedCode
            }
        }
        // Edit node fullScreenCover (state lifted from SessionRoomAssetRow to prevent dismissal on data changes)
        .fullScreenCover(item: $viewModel.selectedEditNode) { node in
            EditNodeDetailViewV3(
                node: node,
                sld: session.sld,
                onDismiss: { viewModel.selectedEditNode = nil },
                focusMode: viewModel.selectedEditFocusMode
            )
            .environmentObject(networkState)
            .environmentObject(appState)
            .environmentObject(sldService)
        }
        .onChange(of: viewModel.selectedEditNode) { newValue in
            if newValue == nil {
                viewModel.onEditNodeDismissed()
            }
        }
        // Task detail fullScreenCover (state lifted from SessionRoomAssetRow)
        .fullScreenCover(item: $viewModel.selectedTask) { task in
            NavigationView {
                TaskDetailView(
                    task: task,
                    showNodesSection: true,
                    hideNavigationBar: true,
                    onDismiss: { viewModel.selectedTask = nil }
                )
                .navigationBarItems(
                    leading: Button(action: {
                        viewModel.selectedTask = nil
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text(AppStrings.CommonExtra.close)
                        }
                    }
                )
            }
            .navigationViewStyle(.stack)
        }
        // Issue detail fullScreenCover (state lifted from SessionRoomAssetRow)
        .fullScreenCover(item: $viewModel.selectedIssue) { issue in
            NavigationView {
                IssueDetailView(
                    issue: issue,
                    hideNavigationBar: true,
                    onDismiss: { viewModel.selectedIssue = nil }
                )
                .navigationBarItems(
                    leading: Button(action: {
                        viewModel.selectedIssue = nil
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text(AppStrings.CommonExtra.close)
                        }
                    }
                )
            }
            .navigationViewStyle(.stack)
        }
        // Direct menu action: New Asset
        .fullScreenCover(isPresented: $viewModel.showingAddAssetDirect) {
            AddAssetViewV2(
                availableLocations: [],
                availableNodeClasses: viewModel.getAvailableNodeClasses(),
                sld: session.sld,
                preselectedRoom: room,
                onSave: { node, photos, irPhotos in
                    viewModel.handleDirectAssetCreation(
                        node: node,
                        photos: photos,
                        irPhotos: irPhotos
                    )
                },
                onCancel: {
                    viewModel.showingAddAssetDirect = false
                }
            )
            .environmentObject(appState)
            .environmentObject(networkState)
        }
        // Direct menu action: Quick Count
        .fullScreenCover(isPresented: $viewModel.showingQuickCountDirect) {
            QuickCountView(
                session: session,
                room: room,
                onComplete: {
                    viewModel.showingQuickCountDirect = false
                },
                onCancel: {
                    viewModel.showingQuickCountDirect = false
                }
            )
            .environmentObject(networkState)
            .environmentObject(appState)
        }
        // Direct menu action: Photo Walkthrough
        .fullScreenCover(isPresented: $viewModel.showingPhotoWalkthroughDirect) {
            PhotoWalkthroughView(
                session: session,
                room: room,
                onComplete: {
                    viewModel.showingPhotoWalkthroughDirect = false
                },
                onCancel: {
                    viewModel.showingPhotoWalkthroughDirect = false
                }
            )
            .environmentObject(networkState)
            .environmentObject(appState)
        }
        // Link Task picker sheet
        .sheet(item: $viewModel.showingLinkTaskNode) { node in
            NodeTaskPickerView(
                session: session,
                node: node,
                onSave: { selectedTaskIds in
                    viewModel.linkTasksToNode(node, selectedTaskIds: selectedTaskIds)
                }
            )
        }
        // ZP-2198: Copy Assets to Rooms wizard
        .sheet(isPresented: $showCopyAssetsSheet) {
            CopyAssetsToRoomsSheet(
                sourceRoom: room,
                session: session,
                sld: session.sld,
                onDismiss: { showCopyAssetsSheet = false }
            )
            .environmentObject(networkState)
            .interactiveDismissDisabled(true)
        }
        .onAppear {
            viewModel.configure(
                modelContext: modelContext,
                networkState: networkState,
                appState: appState,
                sldService: sldService
            )
            viewModel.checkAutoOpenNode()
        }
    }
}

// MARK: - Room Node Addition View
struct RoomNodeAdditionView: View {
    @ObservedObject var viewModel: SessionRoomDetailViewModel
    let onComplete: () -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    // Query all nodes in the same SLD — room-filtered and no-location nodes
    @Query private var roomNodes: [NodeV2]
    @Query private var noLocationNodes: [NodeV2]

    @State private var selectedListeningTaskIds: Set<UUID> = []
    // Convenience accessors
    private var session: IRSession { viewModel.session }
    private var room: Room { viewModel.room }

    private var listeningTasks: [UserTask] {
        session.user_tasks.filter { !$0.is_deleted && networkState.isListening($0.id) }
    }

    init(viewModel: SessionRoomDetailViewModel, onComplete: @escaping () -> Void) {
        self.viewModel = viewModel
        self.onComplete = onComplete

        let sldId = viewModel.session.sld.id
        let roomId = viewModel.room.id

        _roomNodes = Query(
            filter: #Predicate<NodeV2> { node in
                node.sld?.id == sldId &&
                !node.is_deleted &&
                node.room?.id == roomId
            },
            sort: [SortDescriptor(\NodeV2.label)]
        )

        _noLocationNodes = Query(
            filter: #Predicate<NodeV2> { node in
                node.sld?.id == sldId &&
                !node.is_deleted &&
                node.room == nil
            },
            sort: [SortDescriptor(\NodeV2.label)]
        )
    }

    private var filteredRoomNodes: [NodeV2] {
        viewModel.getFilteredNodes(from: roomNodes)
    }

    private var filteredNoLocationNodes: [NodeV2] {
        viewModel.getFilteredNodes(from: noLocationNodes)
    }

    private var allFilteredNodes: [NodeV2] {
        filteredRoomNodes + filteredNoLocationNodes
    }

    @ViewBuilder
    private var listeningTasksSection: some View {
        if !listeningTasks.isEmpty {
            VStack(spacing: 8) {
                HStack(spacing: 6) {
                    Image(systemName: "ear.fill")
                        .font(.caption)
                        .foregroundColor(.orange)
                    Text(AppStrings.Assets.autoLinkTasks)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    Spacer()
                }

                ForEach(listeningTasks) { task in
                    ListeningTaskCheckRow(
                        task: task,
                        isSelected: selectedListeningTaskIds.contains(task.id),
                        onToggle: {
                            if selectedListeningTaskIds.contains(task.id) {
                                selectedListeningTaskIds.remove(task.id)
                            } else {
                                selectedListeningTaskIds.insert(task.id)
                            }
                        }
                    )
                }
            }
            .padding(12)
            .background(Color.orange.opacity(0.08))
            .cornerRadius(10)
            .padding(.horizontal)
            .padding(.bottom, 8)
        }
    }

    var body: some View {
        mainContent
    }

    private var mainContent: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)

                    TextField(AppStrings.CommonExtra.searchAssets, text: $viewModel.addNodeSearchText)
                        .textFieldStyle(PlainTextFieldStyle())

                    if !viewModel.addNodeSearchText.isEmpty {
                        Button(action: {
                            viewModel.addNodeSearchText = ""
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }

                    // QR Scanner button
                    Button(action: {
                        viewModel.addNodeShowQRScanner = true
                    }) {
                        Image(systemName: "qrcode.viewfinder")
                            .foregroundColor(.blue)
                    }
                }
                .padding(8)
                .background(Color(UIColor.systemGray6))
                .cornerRadius(8)
                .padding(.horizontal)
                .padding(.bottom, listeningTasks.isEmpty ? 8 : 8)

                listeningTasksSection

                // Existing Asset List
                if allFilteredNodes.isEmpty {
                    ContentUnavailableView(
                        AppStrings.Sessions.noAvailableAssets,
                        systemImage: "cube.box",
                        description: Text(viewModel.addNodeSearchText.isEmpty ? AppStrings.Sessions.allAssetsLinked : AppStrings.Sessions.noMatchingAssets)
                    )
                    .frame(maxHeight: .infinity)
                } else {
                    List {
                        if !filteredRoomNodes.isEmpty {
                            Section {
                                ForEach(filteredRoomNodes) { node in
                                    NodeSelectionRow(
                                        node: node,
                                        isSelected: viewModel.addNodeSelectedNodes.contains(node),
                                        onToggle: {
                                            viewModel.toggleNodeSelection(node)
                                        }
                                    )
                                }
                            } header: {
                                Text(room.name)
                            }
                        }

                        if !filteredNoLocationNodes.isEmpty {
                            Section {
                                ForEach(filteredNoLocationNodes) { node in
                                    NodeSelectionRow(
                                        node: node,
                                        isSelected: viewModel.addNodeSelectedNodes.contains(node),
                                        onToggle: {
                                            viewModel.toggleNodeSelection(node)
                                        }
                                    )
                                }
                            } header: {
                                Text(AppStrings.Common.noLocation)
                            }
                        }
                    }
                }
            }
            .navigationTitle(AppStrings.Sessions.linkExistingAssets)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !allFilteredNodes.isEmpty {
                        Button(action: {
                            if viewModel.addNodeSelectedNodes.count == allFilteredNodes.count {
                                viewModel.addNodeSelectedNodes.removeAll()
                            } else {
                                viewModel.addNodeSelectedNodes = Set(allFilteredNodes)
                            }
                        }) {
                            Text(viewModel.addNodeSelectedNodes.count == allFilteredNodes.count ? "Deselect All" : "Select All")
                                .font(.subheadline)
                        }
                    }
                }
            }
            .overlay(alignment: .bottom) {
                if !viewModel.addNodeSelectedNodes.isEmpty {
                    Button(action: {
                        let nodesToLink = Array(viewModel.addNodeSelectedNodes)
                        let listeningIds = selectedListeningTaskIds
                        Task {
                            await viewModel.addSelectedNodesToSession(onComplete: onComplete, dismiss: dismiss)
                            if !listeningIds.isEmpty {
                                for node in nodesToLink {
                                    viewModel.linkListeningTasksToNode(node, taskIds: listeningIds)
                                }
                            }
                        }
                    }) {
                        Text(AppStrings.CommonExtra.addCount(viewModel.addNodeSelectedNodes.count))
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
        .fullScreenCover(isPresented: $viewModel.showingAddAsset) {
            AddAssetViewV2(
                availableLocations: [],
                availableNodeClasses: viewModel.getAvailableNodeClasses(),
                sld: session.sld,
                preselectedRoom: room,
                onSave: { node, photos, irPhotos in
                    viewModel.handleNewAssetCreation(
                        node: node,
                        photos: photos,
                        irPhotos: irPhotos,
                        onComplete: onComplete,
                        dismiss: dismiss
                    )
                },
                onCancel: {
                    viewModel.showingAddAsset = false
                }
            )
            .environmentObject(appState)
            .environmentObject(networkState)
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $viewModel.addNodeShowError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(viewModel.addNodeErrorMessage)
        }
        .sheet(isPresented: $viewModel.addNodeShowQRScanner) {
            QRCodeScannerView(scannedCode: $viewModel.addNodeSearchText) { scannedCode in
                // When a code is scanned, it automatically populates the search field
                viewModel.addNodeSearchText = scannedCode
            }
        }
        .fullScreenCover(isPresented: $viewModel.showingQuickCount) {
            QuickCountView(
                session: session,
                room: room,
                onComplete: {
                    viewModel.showingQuickCount = false
                    onComplete()
                    dismiss()
                },
                onCancel: {
                    viewModel.showingQuickCount = false
                }
            )
            .environmentObject(networkState)
            .environmentObject(appState)
        }
        .fullScreenCover(isPresented: $viewModel.showingPhotoWalkthrough) {
            PhotoWalkthroughView(
                session: session,
                room: room,
                onComplete: {
                    viewModel.showingPhotoWalkthrough = false
                    onComplete()
                    dismiss()
                },
                onCancel: {
                    viewModel.showingPhotoWalkthrough = false
                }
            )
            .environmentObject(networkState)
            .environmentObject(appState)
        }
        .overlay {
            if viewModel.addNodeIsUpdating {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.Sessions.addingAssets)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
        .onAppear {
            viewModel.configure(
                modelContext: modelContext,
                networkState: networkState,
                appState: appState,
                sldService: SLDService.shared
            )
            // Pre-select all listening tasks (checked by default)
            selectedListeningTaskIds = Set(listeningTasks.map(\.id))
        }
        .onDisappear {
            viewModel.resetAddNodeState()
        }
    }
}

// Re-use NodeSelectionRow from AssetsTab
// (Already defined in AssetsTab.swift, lines 1428-1456)

// MARK: - Session Room Asset Row
struct SessionRoomAssetRow<ViewModel: SessionAssetRowViewModel>: View {
    let session: IRSession
    let asset: NodeV2
    let children: [NodeV2]
    let isExpanded: Bool
    let childrenMap: [UUID: [NodeV2]]
    var searchText: String = ""
    let edges: [EdgeV2]
    @ObservedObject var metricFilterSettings: AssetMetricFilterSettings
    var isTopLevel: Bool = true  // Only show completion metrics on top-level rows
    @ObservedObject var viewModel: ViewModel
    let taskNodeCounts: [UUID: Int]
    let issueNodeCounts: [UUID: Int]
    let sessionUserTaskIds: Set<UUID>
    let sessionIssueIds: Set<UUID>

    // Init-time snapshots to avoid live SwiftData relationship access during body re-renders (ZP-364)
    private let nodeClassStyle: String?
    private let nodeClassName: String?
    private let hasNodeClass: Bool
    private let nodeClassNeedsSource: Bool
    private let hasNodeSubtype: Bool
    private let snapshotNodeTasks: [UserTask]
    private let snapshotIssues: [Issue]
    private let snapshotIRPhotos: [IRPhoto]

    @State private var showingRemoveConfirmation = false

    init(session: IRSession, asset: NodeV2, children: [NodeV2], isExpanded: Bool, childrenMap: [UUID: [NodeV2]], searchText: String = "", edges: [EdgeV2], metricFilterSettings: AssetMetricFilterSettings, isTopLevel: Bool = true, viewModel: ViewModel, taskNodeCounts: [UUID: Int], issueNodeCounts: [UUID: Int], sessionUserTaskIds: Set<UUID>, sessionIssueIds: Set<UUID>) {
        self.session = session
        self.asset = asset
        self.children = children
        self.isExpanded = isExpanded
        self.childrenMap = childrenMap
        self.searchText = searchText
        self.edges = edges
        self._metricFilterSettings = ObservedObject(wrappedValue: metricFilterSettings)
        self.isTopLevel = isTopLevel
        self._viewModel = ObservedObject(wrappedValue: viewModel)
        self.taskNodeCounts = taskNodeCounts
        self.issueNodeCounts = issueNodeCounts
        self.sessionUserTaskIds = sessionUserTaskIds
        self.sessionIssueIds = sessionIssueIds

        // Snapshot SwiftData relationships at init time to prevent EXC_BAD_ACCESS
        // during sync-triggered re-renders (same pattern as EditNodeDetailView fix in ZP-364)
        self.nodeClassStyle = asset.node_class?.style
        self.nodeClassName = asset.node_class?.name
        self.hasNodeClass = asset.node_class != nil
        self.nodeClassNeedsSource = asset.node_class?.needs_source == true
        self.hasNodeSubtype = asset.node_subtype != nil
        self.snapshotNodeTasks = asset.node_tasks
        self.snapshotIssues = asset.issues
        self.snapshotIRPhotos = asset.ir_photos
    }

    private var isDirectMatch: Bool {
        guard !searchText.isEmpty else { return false }
        let searchLower = searchText.lowercased()
        return asset.label.lowercased().contains(searchLower) ||
               asset.type.lowercased().contains(searchLower)
    }

    private var incompleteNodeTasks: [UserTask] {
        nodeTasks.filter { !(($0.nodeCompletions[asset.id.uuidString]) ?? false) }
    }

    private var completedNodeTasks: [UserTask] {
        nodeTasks.filter { ($0.nodeCompletions[asset.id.uuidString]) ?? false }
    }

    private var nodeTasks: [UserTask] {
        let filtered = snapshotNodeTasks.filter { task in
            !task.is_deleted && sessionUserTaskIds.contains(task.id)
        }
        return filtered.sorted { $0.title < $1.title }
    }

    private var nodeIssues: [Issue] {
        let filtered = snapshotIssues.filter { issue in
            !issue.is_deleted && sessionIssueIds.contains(issue.id)
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

    /// Arc flash readiness check:
    /// 1. Node must have a class (unclassified = incomplete)
    /// 2. All af_required core attributes must be filled in
    /// 3. If node class has needs_source: must have inbound edge OR parent asset OR child with valid inbound edge
    /// 4. If using inbound edge for source: that edge must have a class and all af_required attributes filled
    private var arcFlashReady: Bool {
        guard hasNodeClass else { return false }

        // Check node's own af_required attributes
        guard asset.af_isComplete else { return false }

        // Check source requirement
        if nodeClassNeedsSource {
            let hasParent = asset.parent_id != nil

            if hasParent {
                // Parent asset satisfies source requirement
                return true
            }

            // Check for inbound edges (edges where this node is the target)
            if hasValidInboundEdge(for: asset) {
                return true
            }

            // Check if any child node has a valid inbound edge (child's source serves as parent's source)
            let childNodes = getAllDescendants(of: asset)
            let childHasValidInbound = childNodes.contains { child in
                hasValidInboundEdge(for: child)
            }
            return childHasValidInbound
        }

        return true
    }

    /// Check if a node has at least one valid inbound edge (with class and af_required attrs filled)
    private func hasValidInboundEdge(for node: NodeV2) -> Bool {
        let inboundEdges = edges.filter { !$0.is_deleted && $0.target == node.id }
        return inboundEdges.contains { edge in
            guard edge.edge_class != nil else { return false }
            return edge.af_isComplete
        }
    }

    /// Get all descendants of a node using the childrenMap
    private func getAllDescendants(of node: NodeV2) -> [NodeV2] {
        var descendants: [NodeV2] = []
        if let directChildren = childrenMap[node.id] {
            descendants.append(contentsOf: directChildren)
            for child in directChildren {
                descendants.append(contentsOf: getAllDescendants(of: child))
            }
        }
        return descendants
    }

    /// C.O.M. readiness check:
    /// 1. Node must have a class
    /// 2. Node must have a subtype
    /// 3. Node must have a suggested shortcut
    /// 4. Node must have a COM value
    /// 5. All three condition assessment components must be filled (criticality, operating env, maintenance history)
    private var comReady: Bool {
        guard hasNodeClass else { return false }
        guard hasNodeSubtype else { return false }
        guard asset.suggested_shortcut_id != nil else { return false }
        guard asset.com != nil else { return false }
        guard let calc = asset.com_calculation else { return false }
        return calc.criticalityValue > 0 && calc.operatingConditionsValue > 0 && (calc.maintenanceValue ?? 0) > 0
    }

    // Get all descendants for composite metrics
    private var allDescendants: [NodeV2] {
        AssetMetricCalculator.getAllDescendants(of: asset, using: childrenMap)
    }

    // // Composite inbound connection status (parent + all children)
    // private var inboundConnectionStatus: InboundConnectionStatus {
    //     AssetMetricCalculator.getInboundConnectionStatus(
    //         node: asset,
    //         allDescendants: allDescendants,
    //         edges: edges
    //     )
    // }

    // // Composite arc flash status (parent + all children)
    // private var compositeArcFlashStatus: CompositeArcFlashStatus {
    //     AssetMetricCalculator.getCompositeArcFlashStatus(
    //         node: asset,
    //         allDescendants: allDescendants
    //     )
    // }

    // MARK: - Session-Scoped Task & Issue State (includes children)

    /// All session-scoped tasks for this node (regardless of how many nodes the task is linked to)
    private var sessionTasksForNode: [UserTask] {
        snapshotNodeTasks.filter { task in
            !task.is_deleted && sessionUserTaskIds.contains(task.id)
        }
    }

    /// All session-scoped tasks across this node and all its descendants
    private var compositeSessionTasks: [(task: UserTask, nodeId: UUID)] {
        var results: [(task: UserTask, nodeId: UUID)] = []
        // Add this node's tasks
        for task in sessionTasksForNode {
            results.append((task: task, nodeId: asset.id))
        }
        // Add descendant tasks
        for descendant in allDescendants {
            let descendantTasks = descendant.node_tasks.filter { task in
                !task.is_deleted && sessionUserTaskIds.contains(task.id)
            }
            for task in descendantTasks {
                results.append((task: task, nodeId: descendant.id))
            }
        }
        return results
    }

    /// Task state: nil = no tasks, false = has incomplete, true = all complete
    private var compositeTaskState: Bool? {
        let taskPairs = compositeSessionTasks
        if taskPairs.isEmpty { return nil }

        let allComplete = taskPairs.allSatisfy { pair in
            pair.task.nodeCompletions[pair.nodeId.uuidString] ?? false
        }
        return allComplete
    }

    /// All session-scoped issues for this node
    private var sessionIssuesForNode: [Issue] {
        snapshotIssues.filter { issue in
            !issue.is_deleted && sessionIssueIds.contains(issue.id)
        }
    }

    /// All session-scoped issues across this node and all its descendants
    private var compositeSessionIssues: [Issue] {
        var results = sessionIssuesForNode
        for descendant in allDescendants {
            let descendantIssues = descendant.issues.filter { issue in
                !issue.is_deleted && sessionIssueIds.contains(issue.id)
            }
            results.append(contentsOf: descendantIssues)
        }
        return results
    }

    /// Session-scoped tasks for this node that are not yet marked complete on this node
    private var incompleteSessionTasks: [UserTask] {
        sessionTasksForNode.filter { task in
            !(task.nodeCompletions[asset.id.uuidString] ?? false)
        }
    }

    /// Session-scoped tasks for this node that ARE marked complete on this node
    private var completedSessionTasks: [UserTask] {
        sessionTasksForNode.filter { task in
            task.nodeCompletions[asset.id.uuidString] ?? false
        }
    }

    /// Session-scoped issues for this node that are not yet resolved
    private var unresolvedSessionIssues: [Issue] {
        sessionIssuesForNode.filter { issue in
            issue.status?.lowercased() != "resolved"
        }
    }

    /// Session-scoped issues for this node that ARE resolved
    private var resolvedSessionIssues: [Issue] {
        sessionIssuesForNode.filter { issue in
            issue.status?.lowercased() == "resolved"
        }
    }

    /// Issue state: nil = no issues, false = has unresolved, true = all resolved
    private var compositeIssueState: Bool? {
        let issues = compositeSessionIssues
        if issues.isEmpty { return nil }

        let allResolved = issues.allSatisfy {
            $0.status?.lowercased() == "resolved"
        }
        return allResolved
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Parent asset row
            HStack(spacing: 0) {
                // Expand/collapse button with larger hit area
                Button(action: {
                    if hasExpandableContent {
                        viewModel.toggleNodeExpansion(asset)
                    }
                }) {
                    if hasExpandableContent {
                        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(isExpanded ? .blue : .secondary)
                            .frame(width: 44, height: 44)
                            .contentShape(Rectangle())
                    } else {
                        Color.clear
                            .frame(width: 44, height: 44)
                    }
                }
                .buttonStyle(.plain)

                // Main content area - tappable to open detail view
                Button(action: {
                    viewModel.selectedEditFocusMode = .all
                    viewModel.selectedEditNode = asset
                }) {
                    HStack(spacing: 0) {
                        // Asset icon based on node class style
                        NodeTypeIcon(
                            style: nodeClassStyle,
                            size: 18,
                            color: isExpanded ? .blue : .secondary
                        )
                        .padding(.leading, 4)
                        .padding(.trailing, 8)

                        // Asset name and class
                        VStack(alignment: .leading, spacing: 2) {
                            Text(asset.label)
                                .font(.subheadline)
                                .fontWeight(isExpanded ? .semibold : .medium)
                                .foregroundColor(isExpanded ? .blue : (isDirectMatch ? .blue : .primary))
                                .background(
                                    isDirectMatch ? Color.blue.opacity(0.1) : Color.clear
                                )
                                .lineLimit(1)

                            if let nodeClassName = nodeClassName {
                                Text(nodeClassName)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                            }
                        }

                        Spacer()

                        // Overlay filter indicator
                        if metricFilterSettings.assetOverlayFilter == .ir {
                            let irCount = snapshotIRPhotos.filter { !$0.is_deleted && $0.ir_session?.id == session.id }.count
                            Text("\(irCount)")
                                .font(.caption2)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(irCount > 0 ? Color.green : Color.gray)
                                .clipShape(Capsule())
                                .padding(.trailing, 4)
                        } else if metricFilterSettings.assetOverlayFilter == .arcFlash {
                            let afReady = arcFlashReady
                            Image(systemName: "bolt.circle.fill")
                                .font(.caption)
                                .foregroundColor(afReady ? .green : .red)
                                .padding(.trailing, 4)
                        } else if metricFilterSettings.assetOverlayFilter == .com {
                            let ready = comReady
                            Image(systemName: ready ? "checkmark.circle.fill" : "xmark.circle.fill")
                                .font(.caption)
                                .foregroundColor(ready ? .green : .red)
                                .padding(.trailing, 4)
                        }

                        // Task and Issue state indicators
                        HStack(spacing: 6) {
                            Group {
                                if let taskComplete = compositeTaskState {
                                    Image(systemName: "list.clipboard.fill")
                                        .foregroundColor(taskComplete ? .green : .red)
                                } else {
                                    Color.clear
                                }
                            }
                            .font(.caption2)
                            .frame(width: 14)

                            Group {
                                if let issueResolved = compositeIssueState {
                                    Image(systemName: "exclamationmark.triangle.fill")
                                        .foregroundColor(issueResolved ? .green : .blue)
                                } else {
                                    Color.clear
                                }
                            }
                            .font(.caption2)
                            .frame(width: 14)
                        }
                        .frame(width: 34)
                        .padding(.trailing, 4)
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
                    viewModel.selectedEditFocusMode = .irPhotos
                    viewModel.selectedEditNode = asset
                } label: {
                    Label(AppStrings.Sessions.addIRPhotos, systemImage: "camera.filters")
                }

                Button {
                    viewModel.selectedEditFocusMode = .collectAFData
                    viewModel.selectedEditNode = asset
                } label: {
                    Label(AppStrings.Sessions.collectAFData, systemImage: "bolt.circle")
                }

                Button {
                    viewModel.selectedEditFocusMode = .collectCOMData
                    viewModel.selectedEditNode = asset
                } label: {
                    Label(AppStrings.Sessions.collectCOMData, systemImage: "gauge.with.dots.needle.33percent")
                }

                Button {
                    viewModel.selectedEditFocusMode = .issues
                    viewModel.selectedEditNode = asset
                } label: {
                    Label(AppStrings.Sessions.addIssue, systemImage: "exclamationmark.triangle")
                }

                Button {
                    viewModel.selectedEditFocusMode = .tasks
                    viewModel.selectedEditNode = asset
                } label: {
                    Label(AppStrings.Sessions.addTask, systemImage: "checklist")
                }

                Button {
                    viewModel.showingLinkTaskNode = asset
                } label: {
                    Label(AppStrings.Sessions.linkTask, systemImage: "link.badge.plus")
                }

                if !incompleteSessionTasks.isEmpty || !completedSessionTasks.isEmpty || !unresolvedSessionIssues.isEmpty || !resolvedSessionIssues.isEmpty {
                    Divider()
                }

                if !incompleteSessionTasks.isEmpty {
                    if incompleteSessionTasks.count == 1 {
                        Button {
                            viewModel.markNodeTaskComplete(task: incompleteSessionTasks[0], node: asset)
                        } label: {
                            Label(AppStrings.Sessions.completeTask(incompleteSessionTasks[0].title), systemImage: "checkmark.circle")
                        }
                    } else {
                        Menu {
                            ForEach(incompleteSessionTasks) { task in
                                Button {
                                    viewModel.markNodeTaskComplete(task: task, node: asset)
                                } label: {
                                    Text(task.title)
                                }
                            }
                            Divider()
                            Button {
                                viewModel.markNodeTasksComplete(node: asset, session: session)
                            } label: {
                                Text(AppStrings.Sessions.allCount(incompleteSessionTasks.count))
                            }
                        } label: {
                            Label(AppStrings.Sessions.markTasksComplete, systemImage: "checkmark.circle")
                        }
                    }
                }

                if !completedSessionTasks.isEmpty {
                    if completedSessionTasks.count == 1 {
                        Button {
                            viewModel.unmarkNodeTaskComplete(task: completedSessionTasks[0], node: asset)
                        } label: {
                            Label(AppStrings.Sessions.uncompleteTask(completedSessionTasks[0].title), systemImage: "arrow.uturn.backward.circle")
                        }
                    } else {
                        Menu {
                            ForEach(completedSessionTasks) { task in
                                Button {
                                    viewModel.unmarkNodeTaskComplete(task: task, node: asset)
                                } label: {
                                    Text(task.title)
                                }
                            }
                            Divider()
                            Button {
                                viewModel.unmarkNodeTasksComplete(node: asset, session: session)
                            } label: {
                                Text(AppStrings.Sessions.allCount(completedSessionTasks.count))
                            }
                        } label: {
                            Label(AppStrings.Sessions.uncompleteTasks, systemImage: "arrow.uturn.backward.circle")
                        }
                    }
                }

                if !unresolvedSessionIssues.isEmpty {
                    if unresolvedSessionIssues.count == 1 {
                        Button {
                            viewModel.resolveIssue(unresolvedSessionIssues[0])
                        } label: {
                            Label(AppStrings.Sessions.resolveIssue(unresolvedSessionIssues[0].title ?? "Issue"), systemImage: "exclamationmark.triangle")
                        }
                    } else {
                        Menu {
                            ForEach(unresolvedSessionIssues) { issue in
                                Button {
                                    viewModel.resolveIssue(issue)
                                } label: {
                                    Text(issue.title ?? AppStrings.CommonExtra.untitledIssue)
                                }
                            }
                            Divider()
                            Button {
                                viewModel.resolveNodeIssues(node: asset, session: session)
                            } label: {
                                Text(AppStrings.Sessions.allCount(unresolvedSessionIssues.count))
                            }
                        } label: {
                            Label(AppStrings.Sessions.resolveIssues, systemImage: "exclamationmark.triangle")
                        }
                    }
                }

                if !resolvedSessionIssues.isEmpty {
                    if resolvedSessionIssues.count == 1 {
                        Button {
                            viewModel.reopenIssue(resolvedSessionIssues[0])
                        } label: {
                            Label(AppStrings.Sessions.reopenIssue(resolvedSessionIssues[0].title ?? "Issue"), systemImage: "arrow.uturn.backward")
                        }
                    } else {
                        Menu {
                            ForEach(resolvedSessionIssues) { issue in
                                Button {
                                    viewModel.reopenIssue(issue)
                                } label: {
                                    Text(issue.title ?? AppStrings.CommonExtra.untitledIssue)
                                }
                            }
                            Divider()
                            Button {
                                viewModel.reopenNodeIssues(node: asset, session: session)
                            } label: {
                                Text(AppStrings.Sessions.allCount(resolvedSessionIssues.count))
                            }
                        } label: {
                            Label(AppStrings.Sessions.reopenIssues, systemImage: "arrow.uturn.backward")
                        }
                    }
                }

                Divider()

                Button(role: .destructive) {
                    showingRemoveConfirmation = true
                } label: {
                    Label(AppStrings.Sessions.removeFromWorkOrderLabel, systemImage: "minus.circle")
                }
            }

            // Expanded content: Incomplete tasks, Completed tasks, Issues, then Children
            if isExpanded && hasExpandableContent {
                VStack(alignment: .leading, spacing: 0) {
                    // Incomplete tasks first (actionable)
                    ForEach(incompleteNodeTasks) { task in
                        TaskRow(
                            task: task,
                            searchText: searchText,
                            nodeId: asset.id,
                            nodeCount: taskNodeCounts[task.id] ?? 1
                        )
                        .padding(.leading, 24)
                        .onTapGesture {
                            viewModel.selectedTask = task
                        }
                        .contextMenu {
                            Button {
                                viewModel.markNodeTaskComplete(task: task, node: asset)
                            } label: {
                                Label(AppStrings.Sessions.markComplete, systemImage: "checkmark.circle")
                            }
                        }
                    }

                    // Completed tasks
                    ForEach(completedNodeTasks) { task in
                        TaskRow(
                            task: task,
                            searchText: searchText,
                            nodeId: asset.id,
                            nodeCount: taskNodeCounts[task.id] ?? 1
                        )
                        .padding(.leading, 24)
                        .onTapGesture {
                            viewModel.selectedTask = task
                        }
                        .contextMenu {
                            Button {
                                viewModel.unmarkNodeTaskComplete(task: task, node: asset)
                            } label: {
                                Label(AppStrings.Sessions.uncomplete, systemImage: "arrow.uturn.backward.circle")
                            }
                        }
                    }

                    // Issues for this node
                    ForEach(nodeIssues) { issue in
                        IssueRow(
                            issue: issue,
                            searchText: searchText,
                            nodeCount: issueNodeCounts[issue.id] ?? 1
                        )
                        .padding(.leading, 24)
                        .onTapGesture {
                            viewModel.selectedIssue = issue
                        }
                        .contextMenu {
                            if issue.status?.lowercased() == "resolved" {
                                Button {
                                    viewModel.reopenIssue(issue)
                                } label: {
                                    Label(AppStrings.Sessions.reopen, systemImage: "arrow.uturn.backward")
                                }
                            } else {
                                Button {
                                    viewModel.resolveIssue(issue)
                                } label: {
                                    Label(AppStrings.Sessions.resolve, systemImage: "checkmark.circle")
                                }
                            }
                        }
                    }

                    // Child nodes (recursively)
                    ForEach(children) { child in
                        SessionRoomAssetRow(
                            session: session,
                            asset: child,
                            children: childrenMap[child.id] ?? [],
                            isExpanded: viewModel.expandedNodes.contains(child.id),
                            childrenMap: childrenMap,
                            searchText: searchText,
                            edges: edges,
                            metricFilterSettings: metricFilterSettings,
                            isTopLevel: false,
                            viewModel: viewModel,
                            taskNodeCounts: taskNodeCounts,
                            issueNodeCounts: issueNodeCounts,
                            sessionUserTaskIds: sessionUserTaskIds,
                            sessionIssueIds: sessionIssueIds
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
                viewModel.removeNode(asset)
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        } message: {
            Text(AppStrings.Sessions.removeAssetMessage)
        }
    }
}
