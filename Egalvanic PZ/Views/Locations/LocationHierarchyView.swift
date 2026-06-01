//
//  LocationHierarchyView.swift
//  Egalvanic PZ
//
//  Tree view for managing Building > Floor > Room hierarchy
//

import SwiftUI
import SwiftData

// MARK: - Room Selection Environment Key

/// Environment key for passing room selection callback through the view hierarchy
/// This is more reliable than passing closures through multiple view layers
private struct RoomSelectionCallbackKey: EnvironmentKey {
    static let defaultValue: ((Room) -> Void)? = nil
}

extension EnvironmentValues {
    var roomSelectionCallback: ((Room) -> Void)? {
        get { self[RoomSelectionCallbackKey.self] }
        set { self[RoomSelectionCallbackKey.self] = newValue }
    }
}

// MARK: - Editor Data Structs

struct BuildingEditorData: Identifiable {
    let id = UUID()
    let buildingId: UUID
    let buildingName: String
}

struct FloorEditorData: Identifiable {
    let id = UUID()
    let floorId: UUID
    let floorName: String
    let buildingId: UUID
}

struct RoomEditorData: Identifiable {
    let id = UUID()
    let roomId: UUID
    let roomName: String
    let floorId: UUID
}

struct LocationHierarchyView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var sldService: SLDService

    @StateObject private var viewModel: LocationHierarchyViewModel

    /// Optional callback for room selection (used in session context for navigation)
    var onRoomSelected: ((Room) -> Void)?

    init(diagram: SLDV2, session: IRSession? = nil, onRoomSelected: ((Room) -> Void)? = nil) {
        _viewModel = StateObject(wrappedValue: LocationHierarchyViewModel(
            diagram: diagram,
            session: session
        ))
        self.onRoomSelected = onRoomSelected
    }

    var body: some View {
        // This view should ONLY be used directly when in session context
        // (inside IRSessionDetailView's TabView). The parent provides navigationDestination.
        //
        // For non-session context, use StandaloneLocationHierarchyView wrapper instead,
        // which provides its own NavigationStack and navigationDestination.
        mainContentWithModifiers
            .environment(\.roomSelectionCallback, onRoomSelected)
    }

    @ViewBuilder
    private var mainContentWithModifiers: some View {
        mainContent
            .navigationTitle(viewModel.session == nil ? AppStrings.Locations.locations : "")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if viewModel.session == nil {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(AppStrings.Common.done) {
                            dismiss()
                        }
                    }
                }
            }
            .onAppear {
                viewModel.configure(
                    modelContext: modelContext,
                    networkState: networkState,
                    appState: appState,
                    sldService: sldService
                )
                viewModel.loadBuildings()
                viewModel.loadNoLocationCount()
            }
        .sheet(isPresented: $viewModel.showAddBuilding) { addBuildingSheet() }
        .sheet(item: $viewModel.editBuildingData) { editBuildingSheet(data: $0) }
        .sheet(item: $viewModel.addFloorToBuilding) { building in addFloorSheet(building: building) }
        .sheet(item: $viewModel.editFloorData) { editFloorSheet(data: $0) }
        .sheet(item: $viewModel.addRoomToFloor) { floor in addRoomSheet(floor: floor) }
        .sheet(item: $viewModel.editRoomData) { editRoomSheet(data: $0) }
        .fullScreenCover(isPresented: $viewModel.showNoLocationDetail) { noLocationDetailView() }
        .onChange(of: viewModel.showNoLocationDetail) { _, newValue in
            if !newValue {
                viewModel.onNoLocationDetailDismissed()
            }
        }
        .fullScreenCover(item: $viewModel.showRoomForQRScan) { room in
            if let session = viewModel.session {
                NavigationStack {
                    SessionRoomDetailView(
                        session: session,
                        room: room,
                        autoOpenNodeForIR: viewModel.qrScannedNode,
                        isModal: true
                    )
                }
                .environment(\.modelContext, modelContext)
                .environmentObject(appState)
                .environmentObject(sldService)
                .environmentObject(networkState)
            }
        }
        .fullScreenCover(item: $viewModel.showNodeIRDetail) { context in
            NavigationView {
                EditNodeDetailViewV3(
                    node: context.node,
                    sld: viewModel.diagram,
                    hideNavigationBar: true,
                    onDismiss: { viewModel.showNodeIRDetail = nil },
                    focusMode: networkState.quickQRAction.focusMode
                )
                .navigationBarItems(
                    leading: Button(action: {
                        viewModel.showNodeIRDetail = nil
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text(AppStrings.CommonExtra.close)
                        }
                    }
                )
            }
            .navigationViewStyle(.stack)
            .environmentObject(networkState)
            .environmentObject(appState)
            .environmentObject(sldService)
        }
        .sheet(isPresented: $viewModel.showQRScanner) {
            QRCodeScannerView(scannedCode: .constant("")) { scannedCode in
                viewModel.handleQRCodeScan(scannedCode)
            }
        }
        .alert(item: $viewModel.qrScanResult) { result in
            switch result {
            case .success(let node, _):
                return Alert(
                    title: Text(AppStrings.Locations.assetFound),
                    message: Text(AppStrings.Locations.assetFoundMessage(node.label)),
                    dismissButton: .default(Text(AppStrings.Common.ok))
                )
            case .notFound(let code):
                return Alert(
                    title: Text(AppStrings.Locations.assetNotFound),
                    message: Text(AppStrings.Locations.assetNotFoundMessage(code)),
                    dismissButton: .default(Text(AppStrings.Common.ok))
                )
            case .alreadyInSession(let node):
                return Alert(
                    title: Text(AppStrings.Locations.assetAlreadyInSession),
                    message: Text(AppStrings.Locations.assetAlreadyMessage(node.label)),
                    dismissButton: .default(Text(AppStrings.Common.ok))
                )
            case .noRoom(let node):
                return Alert(
                    title: Text(AppStrings.Locations.assetNotAssignedToRoom),
                    message: Text(AppStrings.Locations.assetNotAssignedMessage(node.label)),
                    dismissButton: .default(Text(AppStrings.Common.ok))
                )
            }
        }
        .alert(AppStrings.CommonExtra.error, isPresented: .constant(viewModel.errorMessage != nil)) {
            Button(AppStrings.Common.ok) {
                viewModel.errorMessage = nil
            }
        } message: {
            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
            }
        }
        .overlay {
            if viewModel.isProcessingQR {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.Locations.processing)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
    }

    private var mainContent: some View {
        ZStack {
            VStack(spacing: 0) {
                if viewModel.buildings.isEmpty {
                    ContentUnavailableView(
                        AppStrings.Locations.noBuildings,
                        systemImage: "building.2",
                        description: Text(AppStrings.Locations.tapPlusToAddBuilding)
                    )
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            ForEach(viewModel.buildings) { building in
                                BuildingCard(
                                    building: building,
                                    viewModel: viewModel,
                                    modelContext: modelContext
                                )
                            }

                            // No Location Card
                            if viewModel.shouldShowNoLocationCard {
                                NoLocationCard(
                                    viewModel: viewModel,
                                    onTap: {
                                        viewModel.showNoLocationDetail = true
                                    }
                                )
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical)
                        .padding(.bottom, 80)
                    }
                    .scrollDismissesKeyboard(.interactively)
                }
            }
            .background(Color(UIColor.systemGroupedBackground))

            // Floating Buttons
            VStack {
                Spacer()
                HStack(spacing: 16) {
                    Spacer()

                    // QR Scanner Button (only in session context)
                    if viewModel.isSessionActive {
                        Button(action: {
                            viewModel.showQRScanner = true
                        }) {
                            Image(systemName: "qrcode.viewfinder")
                                .font(.title2)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                                .frame(width: 56, height: 56)
                                .background(Color.green)
                                .clipShape(Circle())
                                .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                        }
                    }

                    // Plus Button (always show)
                    Button(action: {
                        viewModel.showAddBuilding = true
                    }) {
                        Image(systemName: "plus")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(Color.blue)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                    }
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
        }
    }

    // MARK: - Sheet Helpers

    @ViewBuilder
    private func addBuildingSheet() -> some View {
        BuildingEditorView(
            sld: viewModel.diagram,
            mode: .create,
            onSave: { _ in
                viewModel.onAddBuildingDismissed()
            },
            onCancel: {
                viewModel.showAddBuilding = false
            }
        )
        .environment(\.modelContext, modelContext)
    }

    @ViewBuilder
    private func editBuildingSheet(data: BuildingEditorData) -> some View {
        let foundBuilding: Building? = {
            let buildingId = data.buildingId
            let descriptor = FetchDescriptor<Building>(
                predicate: #Predicate<Building> { building in
                    building.id == buildingId && !building.is_deleted
                }
            )
            return try? modelContext.fetch(descriptor).first
        }()

        if let building = foundBuilding {
            BuildingEditorView(
                sld: viewModel.diagram,
                building: building,
                mode: .edit,
                onSave: { _ in
                    viewModel.onEditBuildingDismissed()
                },
                onCancel: {
                    viewModel.editBuildingData = nil
                }
            )
            .environment(\.modelContext, modelContext)
        } else {
            errorView(message: "Building not found", id: data.buildingId.uuidString) {
                viewModel.editBuildingData = nil
            }
        }
    }

    @ViewBuilder
    private func addFloorSheet(building: Building) -> some View {
        FloorEditorView(
            building: building,
            mode: .create,
            onSave: { _ in
                viewModel.onAddFloorDismissed()
            },
            onCancel: {
                viewModel.addFloorToBuilding = nil
            }
        )
        .environment(\.modelContext, modelContext)
        .onAppear {
            AppLogger.log(.debug, "[LocationHierarchy] addFloorSheet: Building '\(building.name)' passed directly", category: .location)
        }
    }

    @ViewBuilder
    private func editFloorSheet(data: FloorEditorData) -> some View {
        let buildingAndFloor: (Building, Floor)? = {
            let buildingId = data.buildingId
            let floorId = data.floorId

            let buildingDescriptor = FetchDescriptor<Building>(
                predicate: #Predicate<Building> { building in
                    building.id == buildingId && !building.is_deleted
                }
            )
            guard let building = try? modelContext.fetch(buildingDescriptor).first else { return nil }

            let floorDescriptor = FetchDescriptor<Floor>(
                predicate: #Predicate<Floor> { floor in
                    floor.id == floorId && !floor.is_deleted
                }
            )
            guard let floor = try? modelContext.fetch(floorDescriptor).first else { return nil }

            return (building, floor)
        }()

        if let (building, floor) = buildingAndFloor {
            FloorEditorView(
                building: building,
                floor: floor,
                mode: .edit,
                onSave: { _ in
                    viewModel.onEditFloorDismissed()
                },
                onCancel: {
                    viewModel.editFloorData = nil
                }
            )
            .environment(\.modelContext, modelContext)
        } else {
            errorView(message: "Floor not found", id: data.floorId.uuidString) {
                viewModel.editFloorData = nil
            }
        }
    }

    @ViewBuilder
    private func addRoomSheet(floor: Floor) -> some View {
        RoomEditorView(
            floor: floor,
            mode: .create,
            onSave: { _ in
                viewModel.onAddRoomDismissed()
            },
            onCancel: {
                viewModel.addRoomToFloor = nil
            }
        )
        .environment(\.modelContext, modelContext)
    }

    @ViewBuilder
    private func editRoomSheet(data: RoomEditorData) -> some View {
        let roomAndFloor: (Room, Floor)? = {
            let floorId = data.floorId
            let roomId = data.roomId

            let floorDescriptor = FetchDescriptor<Floor>(
                predicate: #Predicate<Floor> { floor in
                    floor.id == floorId && !floor.is_deleted
                }
            )
            guard let floor = try? modelContext.fetch(floorDescriptor).first else { return nil }

            let roomDescriptor = FetchDescriptor<Room>(
                predicate: #Predicate<Room> { room in
                    room.id == roomId && !room.is_deleted
                }
            )
            guard let room = try? modelContext.fetch(roomDescriptor).first else { return nil }

            return (room, floor)
        }()

        if let (room, floor) = roomAndFloor {
            RoomEditorView(
                floor: floor,
                room: room,
                mode: .edit,
                onSave: { _ in
                    viewModel.onEditRoomDismissed()
                },
                onCancel: {
                    viewModel.editRoomData = nil
                }
            )
            .environment(\.modelContext, modelContext)
        } else {
            errorView(message: "Room not found", id: data.roomId.uuidString) {
                viewModel.editRoomData = nil
            }
        }
    }

    @ViewBuilder
    private func errorView(message: String, id: String? = nil, onClose: @escaping () -> Void) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 60))
                .foregroundColor(.red)
            Text("Error: \(message)")
                .font(.headline)
            if let id = id {
                Text("ID: \(id)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Button(AppStrings.CommonExtra.close) {
                onClose()
            }
            .buttonStyle(.bordered)
        }
        .padding()
    }

    @ViewBuilder
    private func noLocationDetailView() -> some View {
        if let session = viewModel.session {
            NoLocationSessionDetailView(
                session: session,
                diagram: viewModel.diagram,
                autoOpenNodeForIR: viewModel.qrScannedNode
            )
            .environment(\.modelContext, modelContext)
            .environmentObject(appState)
            .environmentObject(networkState)
        } else {
            NoLocationDetailView(diagram: viewModel.diagram)
                .environment(\.modelContext, modelContext)
                .environmentObject(appState)
                .environmentObject(networkState)
        }
    }
}
// MARK: - Building Card

struct BuildingCard: View {
    let building: Building
    @ObservedObject var viewModel: LocationHierarchyViewModel
    let modelContext: ModelContext

    @State private var floors: [Floor] = []
    @State private var floorsRefreshTrigger = UUID()
    @State private var buildingTaskCompletionRatio: (completed: Int, total: Int) = (0, 0)
    @State private var buildingIssueResolutionRatio: (resolved: Int, total: Int) = (0, 0)
    @State private var showingAccessNotes = false

    private var isExpanded: Bool {
        viewModel.expandedBuildings.contains(building.id)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Building Header
            Button(action: { viewModel.toggleBuilding(building) }) {
                HStack(spacing: 12) {
                    // Chevron - only show if building has floors
                    if !floors.isEmpty {
                        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(isExpanded ? .blue : .secondary)
                            .frame(width: 20)
                    } else {
                        Color.clear
                            .frame(width: 20)
                    }

                    // Building Icon
                    Image(systemName: "building.2.fill")
                        .font(.title3)
                        .foregroundColor(.blue)

                    // Building Name
                    VStack(alignment: .leading, spacing: 2) {
                        Text(building.name)
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundColor(.primary)

                        if !floors.isEmpty {
                            Text(AppStrings.Locations.floorsCount(floors.count))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    if let notes = building.access_notes, !notes.isEmpty {
                        Button(action: { showingAccessNotes = true }) {
                            Image(systemName: "note.text")
                                .font(.caption)
                                .foregroundColor(.blue)
                                .padding(6)
                                .background(Color.blue.opacity(0.12))
                                .cornerRadius(6)
                        }
                        .buttonStyle(.plain)
                    }

                    Spacer()

                    HStack(spacing: 6) {
                        Group {
                            if viewModel.isSessionContext, buildingTaskCompletionRatio.total > 0 {
                                Image(systemName: "list.clipboard.fill")
                                    .foregroundColor(buildingTaskCompletionRatio.completed == buildingTaskCompletionRatio.total ? .green : .red)
                            } else {
                                Color.clear
                            }
                        }
                        .font(.caption2)
                        .frame(width: 14)

                        Group {
                            if viewModel.isSessionContext, buildingIssueResolutionRatio.total > 0 {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(buildingIssueResolutionRatio.resolved == buildingIssueResolutionRatio.total ? .green : .blue)
                            } else {
                                Color.clear
                            }
                        }
                        .font(.caption2)
                        .frame(width: 14)
                    }
                    .frame(width: 34)

                    // Add Floor button (inline)
                    Button(action: { viewModel.prepareAddFloor(to: building) }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                            .foregroundColor(.blue)
                    }
                    .buttonStyle(.plain)
                    .frame(width: 28)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .background(Color(UIColor.systemBackground))
            .contextMenu {
                if let notes = building.access_notes, !notes.isEmpty {
                    Button {
                        showingAccessNotes = true
                    } label: {
                        Label(AppStrings.Locations.showAccessNotes, systemImage: "note.text")
                    }

                    Divider()
                }

                Button {
                    AppLogger.log(.debug, "[LocationHierarchy] BuildingCard contextMenu: Edit Building tapped for building: \(building.name)", category: .location)
                    viewModel.prepareEditBuilding(building)
                } label: {
                    Label(AppStrings.Locations.editBuilding, systemImage: "pencil")
                }

                Divider()

                Button(role: .destructive) {
                    AppLogger.log(.debug, "[LocationHierarchy] BuildingCard contextMenu: Delete Building tapped", category: .location)
                    viewModel.deleteBuilding(building)
                } label: {
                    Label(AppStrings.Locations.deleteBuilding, systemImage: "trash")
                }
            }
            .alert(AppStrings.Locations.accessNotes, isPresented: $showingAccessNotes) {
                Button(AppStrings.Common.ok) { }
            } message: {
                Text(building.access_notes ?? "")
            }

            // Expanded Content: Floors
            if isExpanded {
                Divider()
                    .padding(.horizontal, 16)

                VStack(alignment: .leading, spacing: 0) {
                    ForEach(floors) { floor in
                        FloorRow(
                            floor: floor,
                            buildingId: building.id,
                            viewModel: viewModel,
                            refreshTrigger: floorsRefreshTrigger,
                            modelContext: modelContext
                        )
                    }
                }
                .padding(.bottom, 8)
                .background(Color(UIColor.systemBackground))
            }
        }
        .background(Color(UIColor.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 2, x: 0, y: 1)
        .onAppear {
            loadFloors()
        }
        .onChange(of: isExpanded) { _, newValue in
            if newValue {
                loadFloors()
            }
        }
        .onChange(of: viewModel.buildingsRefreshTrigger) { _, _ in
            loadFloors()
        }
        .onChange(of: floors) { _, _ in
            if viewModel.isSessionContext {
                buildingTaskCompletionRatio = viewModel.calculateBuildingTaskCompletionRatio(floors: floors)
                buildingIssueResolutionRatio = viewModel.calculateBuildingIssueResolutionRatio(floors: floors)
            }
        }
    }

    private func loadFloors() {
        do {
            floors = try LocationHierarchyService.getFloorsForBuilding(building, modelContext: modelContext)
            floorsRefreshTrigger = UUID()

            // Expand all floors by default
            viewModel.expandFloors(floors)

            if viewModel.isSessionContext {
                buildingTaskCompletionRatio = viewModel.calculateBuildingTaskCompletionRatio(floors: floors)
                buildingIssueResolutionRatio = viewModel.calculateBuildingIssueResolutionRatio(floors: floors)
            }
        } catch {
            AppLogger.log(.error, "Failed to load floors: \(error)", category: .location)
        }
    }
}

// MARK: - Floor Row

struct FloorRow: View {
    let floor: Floor
    let buildingId: UUID
    @ObservedObject var viewModel: LocationHierarchyViewModel
    let refreshTrigger: UUID
    let modelContext: ModelContext

    @State private var rooms: [Room] = []
    @State private var floorTaskCompletionRatio: (completed: Int, total: Int) = (0, 0)
    @State private var floorIssueResolutionRatio: (resolved: Int, total: Int) = (0, 0)
    @State private var showingAccessNotes = false

    private var isExpanded: Bool {
        viewModel.expandedFloors.contains(floor.id)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Floor Header
            Button(action: { viewModel.toggleFloor(floor) }) {
                HStack(spacing: 12) {
                    // Indentation to show hierarchy
                    Color.clear.frame(width: 20)

                    // Chevron - only show if floor has rooms
                    if !rooms.isEmpty {
                        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(isExpanded ? .blue : .secondary)
                            .frame(width: 20)
                    } else {
                        Color.clear
                            .frame(width: 20)
                    }

                    // Floor Icon
                    Image(systemName: "square.stack.3d.up.fill")
                        .font(.body)
                        .foregroundColor(.green)

                    // Floor Name
                    VStack(alignment: .leading, spacing: 2) {
                        Text(floor.name)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.primary)

                        if !rooms.isEmpty {
                            Text(AppStrings.Locations.roomsCount(rooms.count))
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }

                    if let notes = floor.access_notes, !notes.isEmpty {
                        Button(action: { showingAccessNotes = true }) {
                            Image(systemName: "note.text")
                                .font(.caption)
                                .foregroundColor(.blue)
                                .padding(6)
                                .background(Color.blue.opacity(0.12))
                                .cornerRadius(6)
                        }
                        .buttonStyle(.plain)
                    }

                    Spacer()

                    HStack(spacing: 6) {
                        Group {
                            if viewModel.isSessionContext, floorTaskCompletionRatio.total > 0 {
                                Image(systemName: "list.clipboard.fill")
                                    .foregroundColor(floorTaskCompletionRatio.completed == floorTaskCompletionRatio.total ? .green : .red)
                            } else {
                                Color.clear
                            }
                        }
                        .font(.caption2)
                        .frame(width: 14)

                        Group {
                            if viewModel.isSessionContext, floorIssueResolutionRatio.total > 0 {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(floorIssueResolutionRatio.resolved == floorIssueResolutionRatio.total ? .green : .blue)
                            } else {
                                Color.clear
                            }
                        }
                        .font(.caption2)
                        .frame(width: 14)
                    }
                    .frame(width: 34)

                    // Add Room button (inline)
                    Button(action: { viewModel.prepareAddRoom(to: floor) }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.body)
                            .foregroundColor(.green)
                    }
                    .buttonStyle(.plain)
                    .frame(width: 28)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .contextMenu {
                if let notes = floor.access_notes, !notes.isEmpty {
                    Button {
                        showingAccessNotes = true
                    } label: {
                        Label(AppStrings.Locations.showAccessNotes, systemImage: "note.text")
                    }

                    Divider()
                }

                Button {
                    AppLogger.log(.debug, "[LocationHierarchy] FloorRow contextMenu: Edit Floor tapped for floor: \(floor.name)", category: .location)
                    viewModel.prepareEditFloor(floor, buildingId: buildingId)
                } label: {
                    Label(AppStrings.Locations.editFloor, systemImage: "pencil")
                }

                Divider()

                Button(role: .destructive) {
                    AppLogger.log(.debug, "[LocationHierarchy] FloorRow contextMenu: Delete Floor tapped", category: .location)
                    viewModel.deleteFloor(floor)
                } label: {
                    Label(AppStrings.Locations.deleteFloor, systemImage: "trash")
                }
            }
            .alert(AppStrings.Locations.accessNotes, isPresented: $showingAccessNotes) {
                Button(AppStrings.Common.ok) { }
            } message: {
                Text(floor.access_notes ?? "")
            }

            // Expanded Content: Rooms
            if isExpanded {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(rooms) { room in
                        RoomRow(
                            room: room,
                            viewModel: viewModel,
                            modelContext: modelContext
                        )
                    }
                }
            }
        }
        .onAppear {
            loadRooms()
        }
        .onChange(of: isExpanded) { _, newValue in
            if newValue {
                loadRooms()
            }
        }
        .onChange(of: refreshTrigger) { _, _ in
            loadRooms()
        }
        .onChange(of: rooms) { _, _ in
            if viewModel.isSessionContext {
                floorTaskCompletionRatio = viewModel.calculateFloorTaskCompletionRatio(rooms: rooms)
                floorIssueResolutionRatio = viewModel.calculateFloorIssueResolutionRatio(rooms: rooms)
            }
        }
    }

    private func loadRooms() {
        do {
            rooms = try LocationHierarchyService.getRoomsForFloor(floor, modelContext: modelContext)

            if viewModel.isSessionContext {
                floorTaskCompletionRatio = viewModel.calculateFloorTaskCompletionRatio(rooms: rooms)
                floorIssueResolutionRatio = viewModel.calculateFloorIssueResolutionRatio(rooms: rooms)
            }
        } catch {
            AppLogger.log(.error, "Failed to load rooms: \(error)", category: .location)
        }
    }
}

// MARK: - Room Row

struct RoomRow: View {
    let room: Room
    @ObservedObject var viewModel: LocationHierarchyViewModel
    let modelContext: ModelContext

    // Read the room selection callback from environment (more reliable than passing through views)
    @Environment(\.roomSelectionCallback) private var roomSelectionCallback

    @State private var nodeCount: Int = 0
    @State private var roomTaskCompletionRatio: (completed: Int, total: Int) = (0, 0)
    @State private var roomIssueResolutionRatio: (resolved: Int, total: Int) = (0, 0)
    @State private var showingAccessNotes = false

    var body: some View {
        Group {
            // Use callback-based navigation when roomSelectionCallback is provided (session context)
            // Use NavigationLink when roomSelectionCallback is nil (standalone context)
            if let callback = roomSelectionCallback {
                Button(action: {
                    AppLogger.log(.debug, "[LocationHierarchy] RoomRow: Button tapped for room '\(room.name)'", category: .location)
                    callback(room)
                }) {
                    roomContent
                }
                .buttonStyle(.plain)
            } else {
                NavigationLink(value: room) {
                    roomContent
                }
            }
        }
        .contextMenu {
            if let notes = room.access_notes, !notes.isEmpty {
                Button {
                    showingAccessNotes = true
                } label: {
                    Label(AppStrings.Locations.showAccessNotes, systemImage: "note.text")
                }

                Divider()
            }

            Button {
                AppLogger.log(.debug, "[LocationHierarchy] RoomRow contextMenu: Edit Room tapped for room: \(room.name)", category: .location)
                viewModel.prepareEditRoom(room)
            } label: {
                Label(AppStrings.Locations.editRoom, systemImage: "pencil")
            }

            Divider()

            Button(role: .destructive) {
                AppLogger.log(.debug, "[LocationHierarchy] RoomRow contextMenu: Delete Room tapped for room: \(room.name)", category: .location)
                viewModel.deleteRoom(room)
            } label: {
                Label(AppStrings.Locations.deleteRoom, systemImage: "trash")
            }
        }
        .alert(AppStrings.Locations.accessNotes, isPresented: $showingAccessNotes) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(room.access_notes ?? "")
        }
        .onAppear {
            loadNodeCount()
        }
    }

    @ViewBuilder
    private var roomContent: some View {
        HStack(spacing: 12) {
            // Indentation to show hierarchy (building + floor + room chevron space)
            Color.clear.frame(width: 20) // Building indent
            Color.clear.frame(width: 20) // Floor chevron space
            Color.clear.frame(width: 20) // Room indent (aligned as if it had a chevron)

            // Room Icon
            Image(systemName: "door.left.hand.open")
                .font(.body)
                .foregroundColor(.orange)

            // Room Name
            VStack(alignment: .leading, spacing: 2) {
                Text(room.name)
                    .font(.system(size: 15, weight: .regular))
                    .foregroundColor(.primary)

                if nodeCount > 0 {
                    Text(AppStrings.Sessions.assetsCount(nodeCount))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }

            if let notes = room.access_notes, !notes.isEmpty {
                Button(action: { showingAccessNotes = true }) {
                    Image(systemName: "note.text")
                        .font(.caption)
                        .foregroundColor(.blue)
                        .padding(6)
                        .background(Color.blue.opacity(0.12))
                        .cornerRadius(6)
                }
                .buttonStyle(.plain)
            }

            Spacer()

            HStack(spacing: 6) {
                Group {
                    if viewModel.isSessionContext, roomTaskCompletionRatio.total > 0 {
                        Image(systemName: "list.clipboard.fill")
                            .foregroundColor(roomTaskCompletionRatio.completed == roomTaskCompletionRatio.total ? .green : .red)
                    } else {
                        Color.clear
                    }
                }
                .font(.caption2)
                .frame(width: 14)

                Group {
                    if viewModel.isSessionContext, roomIssueResolutionRatio.total > 0 {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(roomIssueResolutionRatio.resolved == roomIssueResolutionRatio.total ? .green : .blue)
                    } else {
                        Color.clear
                    }
                }
                .font(.caption2)
                .frame(width: 14)
            }
            .frame(width: 34)

            // Chevron indicator for navigation
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary.opacity(0.5))
                .frame(width: 28)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .contentShape(Rectangle())
    }

    private func loadNodeCount() {
        do {
            let nodes = try LocationHierarchyService.getNodesForRoom(room, modelContext: modelContext)
            nodeCount = nodes.count

            if viewModel.isSessionContext {
                roomTaskCompletionRatio = viewModel.calculateRoomTaskCompletionRatio(nodes: nodes)
                roomIssueResolutionRatio = viewModel.calculateRoomIssueResolutionRatio(nodes: nodes)
            }
        } catch {
            AppLogger.log(.error, "Failed to load node count: \(error)", category: .location)
        }
    }
}

// MARK: - No Location Card

struct NoLocationCard: View {
    @ObservedObject var viewModel: LocationHierarchyViewModel
    let onTap: () -> Void

    @State private var taskCompletionRatio: (completed: Int, total: Int) = (0, 0)
    @State private var issueResolutionRatio: (resolved: Int, total: Int) = (0, 0)

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                // No chevron for no location
                Color.clear.frame(width: 20)

                // Icon (question mark or location slash)
                Image(systemName: "location.slash")
                    .font(.title3)
                    .foregroundColor(.gray)

                // Label
                VStack(alignment: .leading, spacing: 2) {
                    Text(AppStrings.CommonExtra.noLocation)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.primary)

                    Text(AppStrings.Locations.unassignedAssetsCount(viewModel.noLocationNodeCount))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                HStack(spacing: 6) {
                    Group {
                        if viewModel.isSessionContext, taskCompletionRatio.total > 0 {
                            Image(systemName: "list.clipboard.fill")
                                .foregroundColor(taskCompletionRatio.completed == taskCompletionRatio.total ? .green : .red)
                        } else {
                            Color.clear
                        }
                    }
                    .font(.caption2)
                    .frame(width: 14)

                    Group {
                        if viewModel.isSessionContext, issueResolutionRatio.total > 0 {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(issueResolutionRatio.resolved == issueResolutionRatio.total ? .green : .blue)
                        } else {
                            Color.clear
                        }
                    }
                    .font(.caption2)
                    .frame(width: 14)
                }
                .frame(width: 34)

                Color.clear.frame(width: 28)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.gray.opacity(0.3), style: StrokeStyle(lineWidth: 1, dash: [5]))
        )
        .shadow(color: Color.black.opacity(0.05), radius: 2, x: 0, y: 1)
        .onAppear {
            taskCompletionRatio = viewModel.calculateNoLocationTaskCompletionRatio()
            issueResolutionRatio = viewModel.calculateNoLocationIssueResolutionRatio()
        }
    }
}

// MARK: - No Location Detail Views

/// Detail view for unassigned assets (not in a session context)
struct NoLocationDetailView: View {
    let diagram: SLDV2

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    @State private var nodes: [NodeV2] = []
    @State private var expandedNodes: Set<UUID> = []
    @State private var searchText = ""
    @State private var showingAddNode = false
    @State private var editNodeContext: EditNodeContext?
    @State private var errorMessage: String?

    struct EditNodeContext: Identifiable {
        let id = UUID()
        let node: NodeV2
        let sld: SLDV2
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search bar
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
                .padding(.top, 8)

                if nodes.isEmpty {
                    ContentUnavailableView(
                        AppStrings.Locations.noUnassignedAssets,
                        systemImage: "location.slash",
                        description: Text(AppStrings.Locations.allAssetsAssigned)
                    )
                    .frame(maxHeight: .infinity)
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(filteredNodes) { node in
                                Button(action: {
                                    editNodeContext = EditNodeContext(node: node, sld: diagram)
                                }) {
                                    HStack {
                                        Image(systemName: "cube")
                                            .foregroundColor(.gray)
                                        Text(node.label)
                                            .font(.subheadline)
                                        Spacer()
                                    }
                                    .padding()
                                    .background(Color(UIColor.secondarySystemBackground))
                                    .cornerRadius(8)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle(AppStrings.CommonExtra.noLocation)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.done) {
                        dismiss()
                    }
                }
            }
            .onAppear {
                loadNodes()
            }
            .fullScreenCover(item: $editNodeContext) { context in
                EditNodeDetailViewV3(
                    node: context.node,
                    sld: context.sld,
                    lockRoomSelection: false,
                    onDismiss: {
                        editNodeContext = nil
                        loadNodes()
                    }
                )
                .environmentObject(appState)
                .environmentObject(networkState)
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
        }
        .navigationViewStyle(.stack)
    }

    private var filteredNodes: [NodeV2] {
        if searchText.isEmpty {
            return nodes
        }
        return nodes.filter { node in
            node.label.localizedCaseInsensitiveContains(searchText) ||
            node.type.localizedCaseInsensitiveContains(searchText) ||
            (node.node_class?.name.localizedCaseInsensitiveContains(searchText) ?? false)
        }
    }

    private func loadNodes() {
        do {
            nodes = try LocationHierarchyService.getNodesWithoutRoom(diagram, modelContext: modelContext)
        } catch {
            errorMessage = "Failed to load assets: \(error.localizedDescription)"
        }
    }
}

/// Detail view for unassigned assets in a session context
struct NoLocationSessionDetailView: View {
    @StateObject private var viewModel: NoLocationSessionDetailViewModel

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    @StateObject private var metricFilterSettings = AssetMetricFilterSettings.shared

    // Convenience accessors
    private var session: IRSession { viewModel.session }
    private var diagram: SLDV2 { viewModel.diagram }

    init(session: IRSession, diagram: SLDV2, autoOpenNodeForIR: NodeV2? = nil) {
        _viewModel = StateObject(wrappedValue: NoLocationSessionDetailViewModel(
            session: session,
            diagram: diagram,
            autoOpenNodeForIR: autoOpenNodeForIR
        ))
    }

    var body: some View {
        let cachedHierarchy = viewModel.hierarchy
        let cachedTaskNodeCounts = viewModel.taskNodeCounts
        let cachedIssueNodeCounts = viewModel.issueNodeCounts
        let cachedSessionUserTaskIds = viewModel.sessionUserTaskIds
        let cachedSessionIssueIds = viewModel.sessionIssueIds
        NavigationView {
            ZStack {
                VStack(spacing: 0) {
                    // Search bar with filter button
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
                        }
                        .padding(8)
                        .background(Color(UIColor.systemGray6))
                        .cornerRadius(8)

                        // Filter menu to toggle individual metrics
                        AssetMetricFilterMenu(settings: metricFilterSettings)
                    }
                    .padding(.horizontal)
                    .padding(.top, 8)

                    if viewModel.sessionNodesWithoutRoom.isEmpty {
                        ContentUnavailableView(
                            AppStrings.Locations.noUnassignedAssets,
                            systemImage: "location.slash",
                            description: Text(AppStrings.Locations.allSessionAssetsAssigned)
                        )
                        .frame(maxHeight: .infinity)
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
                                        edges: diagram.edges,
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
                    }
                }

                // Floating Action Button - disabled if session is not active
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: {
                            viewModel.showingAddNodes = true
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
            .navigationTitle(AppStrings.CommonExtra.noLocation)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.done) {
                        dismiss()
                    }
                }
            }
        }
        .navigationViewStyle(.stack)
        .fullScreenCover(isPresented: $viewModel.showingAddNodes) {
            NodeAdditionView(
                session: session,
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
            NavigationView {
                EditNodeDetailViewV3(
                    node: node,
                    sld: diagram,
                    hideNavigationBar: true,
                    onDismiss: { viewModel.nodeIRDetail = nil },
                    focusMode: networkState.quickQRAction.focusMode
                )
                .navigationBarItems(
                    leading: Button(action: {
                        viewModel.nodeIRDetail = nil
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text(AppStrings.CommonExtra.close)
                        }
                    }
                )
            }
            .navigationViewStyle(.stack)
            .environmentObject(networkState)
            .environmentObject(appState)
        }
        // Edit node fullScreenCover (state lifted from SessionRoomAssetRow to prevent dismissal on data changes)
        .fullScreenCover(item: $viewModel.selectedEditNode) { node in
            NavigationView {
                EditNodeDetailViewV3(
                    node: node,
                    sld: diagram,
                    hideNavigationBar: true,
                    onDismiss: { viewModel.selectedEditNode = nil },
                    focusMode: viewModel.selectedEditFocusMode
                )
                .navigationBarItems(
                    leading: Button(action: {
                        viewModel.selectedEditNode = nil
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                            Text(AppStrings.CommonExtra.close)
                        }
                    }
                )
            }
            .navigationViewStyle(.stack)
            .environmentObject(networkState)
            .environmentObject(appState)
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
        .onAppear {
            viewModel.configure(
                modelContext: modelContext,
                networkState: networkState,
                appState: appState
            )
            viewModel.checkAutoOpenNode()
        }
    }
}

// MARK: - Standalone Location Hierarchy View

/// Wrapper view for LocationHierarchyView when used outside of a session context.
/// Provides its own NavigationStack and navigationDestination for Room navigation.
/// Use this view when presenting LocationHierarchyView modally (e.g., from SiteTabView's Locations card).
struct StandaloneLocationHierarchyView: View {
    let diagram: SLDV2

    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var sldService: SLDService

    var body: some View {
        NavigationStack {
            LocationHierarchyView(diagram: diagram)
                .environmentObject(networkState)
                .environmentObject(appState)
                .environmentObject(sldService)
                .navigationDestination(for: Room.self) { room in
                    RoomDetailView(room: room, isModal: false)
                        .environment(\.modelContext, modelContext)
                        .environmentObject(appState)
                        .environmentObject(sldService)
                        .environmentObject(networkState)
                }
        }
    }
}
