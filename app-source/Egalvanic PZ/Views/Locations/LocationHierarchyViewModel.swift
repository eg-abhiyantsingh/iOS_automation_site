//
//  LocationHierarchyViewModel.swift
//  Egalvanic PZ
//
//  ViewModel for LocationHierarchyView - manages building/floor/room hierarchy
//

import SwiftUI
import SwiftData

// MARK: - Helper Types

struct NodeIRDetailContext: Identifiable {
    let id = UUID()
    let node: NodeV2
    let session: IRSession
}

enum QRScanResult: Identifiable {
    case success(node: NodeV2, room: Room)
    case notFound(code: String)
    case alreadyInSession(node: NodeV2)
    case noRoom(node: NodeV2)

    var id: String {
        switch self {
        case .success(let node, _): return "success-\(node.id)"
        case .notFound(let code): return "notFound-\(code)"
        case .alreadyInSession(let node): return "alreadyIn-\(node.id)"
        case .noRoom(let node): return "noRoom-\(node.id)"
        }
    }
}

// MARK: - ViewModel

@MainActor
class LocationHierarchyViewModel: ObservableObject {

    // MARK: - Published Properties

    // Data state
    @Published var buildings: [Building] = []
    @Published var noLocationNodeCount: Int = 0
    @Published var buildingsRefreshTrigger = UUID()

    // Expansion state
    @Published var expandedBuildings: Set<UUID> = []
    @Published var expandedFloors: Set<UUID> = []

    // Editor state
    @Published var showAddBuilding = false
    @Published var editBuildingData: BuildingEditorData?
    @Published var editFloorData: FloorEditorData?
    @Published var editRoomData: RoomEditorData?
    @Published var addFloorToBuilding: Building?
    @Published var addRoomToFloor: Floor?

    // Navigation state
    @Published var showNoLocationDetail = false
    @Published var errorMessage: String?

    // QR Scanner state (only used in session context)
    @Published var showQRScanner = false
    @Published var isProcessingQR = false
    @Published var qrScanResult: QRScanResult?
    @Published var showNodeIRDetail: NodeIRDetailContext?
    @Published var showRoomForQRScan: Room?
    @Published var qrScannedNode: NodeV2?

    // MARK: - Properties

    let diagram: SLDV2
    let session: IRSession?

    // MARK: - Private Properties

    private var modelContext: ModelContext?
    private var networkState: NetworkState?
    private var appState: AppStateManager?
    private var sldService: SLDService?

    // MARK: - Initialization

    init(diagram: SLDV2, session: IRSession? = nil) {
        self.diagram = diagram
        self.session = session
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

    var isSessionContext: Bool {
        session != nil
    }

    var isSessionActive: Bool {
        session?.active ?? false
    }

    var shouldShowNoLocationCard: Bool {
        isSessionContext || noLocationNodeCount > 0
    }

    // MARK: - Data Loading Methods

    func loadBuildings() {
        guard let modelContext = modelContext else { return }
        do {
            buildings = try LocationHierarchyService.getBuildingsForSLD(diagram, modelContext: modelContext)
            buildingsRefreshTrigger = UUID()

            // Expand all buildings by default
            for building in buildings {
                expandedBuildings.insert(building.id)
            }
        } catch {
            errorMessage = "Failed to load buildings: \(error.localizedDescription)"
        }
    }

    /// Expand the given floors (called when floors are loaded to expand them by default)
    func expandFloors(_ floors: [Floor]) {
        for floor in floors {
            expandedFloors.insert(floor.id)
        }
    }

    func loadNoLocationCount() {
        guard let modelContext = modelContext else { return }
        if let session = session {
            let sessionLinkedNodesWithoutRoom = session.nodes.filter { !$0.is_deleted && $0.room == nil }
            noLocationNodeCount = sessionLinkedNodesWithoutRoom.count
        } else {
            do {
                let nodes = try LocationHierarchyService.getNodesWithoutRoom(diagram, modelContext: modelContext)
                noLocationNodeCount = nodes.count
            } catch {
                errorMessage = "Failed to load unassigned assets: \(error.localizedDescription)"
            }
        }
    }

    // MARK: - Toggle Methods

    func toggleBuilding(_ building: Building) {
        if expandedBuildings.contains(building.id) {
            expandedBuildings.remove(building.id)
        } else {
            expandedBuildings.insert(building.id)
        }
    }

    func toggleFloor(_ floor: Floor) {
        if expandedFloors.contains(floor.id) {
            expandedFloors.remove(floor.id)
        } else {
            expandedFloors.insert(floor.id)
        }
    }

    // MARK: - Editor Preparation Methods

    func prepareEditBuilding(_ building: Building) {
        loadBuildings()
        editBuildingData = BuildingEditorData(
            buildingId: building.id,
            buildingName: building.name
        )
    }

    func prepareEditFloor(_ floor: Floor, buildingId: UUID) {
        loadBuildings()
        editFloorData = FloorEditorData(
            floorId: floor.id,
            floorName: floor.name,
            buildingId: floor.building?.id ?? buildingId
        )
    }

    func prepareEditRoom(_ room: Room) {
        loadBuildings()
        editRoomData = RoomEditorData(
            roomId: room.id,
            roomName: room.name,
            floorId: room.floor?.id ?? UUID()
        )
    }

    func prepareAddFloor(to building: Building) {
        AppLogger.log(.debug, "[LocationHierarchy] prepareAddFloor: Building '\(building.name)' ID: \(building.id)", category: .location)
        addFloorToBuilding = building
    }

    func prepareAddRoom(to floor: Floor) {
        addRoomToFloor = floor
    }

    // MARK: - Delete Methods

    func deleteBuilding(_ building: Building) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        Task {
            do {
                let floors = try LocationHierarchyService.getFloorsForBuilding(building, modelContext: modelContext)
                var hasRooms = false
                for floor in floors {
                    let rooms = try LocationHierarchyService.getRoomsForFloor(floor, modelContext: modelContext)
                    if !rooms.isEmpty {
                        hasRooms = true
                        break
                    }
                }

                if !floors.isEmpty {
                    await MainActor.run {
                        errorMessage = hasRooms ?
                            "Cannot delete building with floors containing rooms. Delete rooms and floors first." :
                            "Cannot delete building with floors. Delete floors first."
                    }
                    return
                }

                try await LocationHierarchyService.deleteBuilding(
                    building,
                    networkState: networkState,
                    modelContext: modelContext
                )
                await MainActor.run {
                    loadBuildings()
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Failed to delete building: \(error.localizedDescription)"
                }
            }
        }
    }

    func deleteFloor(_ floor: Floor) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        Task {
            do {
                let rooms = try LocationHierarchyService.getRoomsForFloor(floor, modelContext: modelContext)
                var hasNodes = false
                for room in rooms {
                    let nodes = try LocationHierarchyService.getNodesForRoom(room, modelContext: modelContext)
                    if !nodes.isEmpty {
                        hasNodes = true
                        break
                    }
                }

                if !rooms.isEmpty {
                    await MainActor.run {
                        errorMessage = hasNodes ?
                            "Cannot delete floor with rooms containing assets. Delete assets and rooms first." :
                            "Cannot delete floor with rooms. Delete rooms first."
                    }
                    return
                }

                try await LocationHierarchyService.deleteFloor(
                    floor,
                    networkState: networkState,
                    modelContext: modelContext
                )
                await MainActor.run {
                    loadBuildings()
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Failed to delete floor: \(error.localizedDescription)"
                }
            }
        }
    }

    func deleteRoom(_ room: Room) {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        Task {
            do {
                let nodes = try LocationHierarchyService.getNodesForRoom(room, modelContext: modelContext)

                if !nodes.isEmpty {
                    await MainActor.run {
                        errorMessage = "Cannot delete room with \(nodes.count) asset\(nodes.count == 1 ? "" : "s"). Delete or move assets first."
                    }
                    return
                }

                try await LocationHierarchyService.deleteRoom(
                    room,
                    networkState: networkState,
                    modelContext: modelContext
                )
                await MainActor.run {
                    loadBuildings()
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Failed to delete room: \(error.localizedDescription)"
                }
            }
        }
    }

    // MARK: - QR Scanner Methods

    func handleQRCodeScan(_ scannedCode: String) {
        guard let session = session, let modelContext = modelContext else { return }

        AppLogger.log(.info, "QR Code scanned: \(scannedCode)", category: .location)

        let sldId = diagram.id
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

            guard let node = matchingNodes.first else {
                AppLogger.log(.info, "No node found with QR code", category: .location)
                qrScanResult = .notFound(code: scannedCode)
                return
            }

            if session.nodes.contains(where: { $0.id == node.id }) {
                AppLogger.log(.info, "Node already in session", category: .location)
                qrScanResult = .alreadyInSession(node: node)
                return
            }

            AppLogger.log(.info, "Adding node to session", category: .location)
            addNodeToSession(node)

        } catch {
            AppLogger.log(.error, "Error querying for node: \(error)", category: .location)
            errorMessage = "Failed to search for asset: \(error.localizedDescription)"
        }
    }

    func addNodeToSession(_ node: NodeV2) {
        guard let session = session, let modelContext = modelContext, let networkState = networkState else { return }

        Task {
            await MainActor.run { isProcessingQR = true }

            do {
                if !session.nodes.contains(where: { $0.id == node.id }) {
                    session.nodes.append(node)
                    if !node.ir_sessions.contains(where: { $0.id == session.id }) {
                        node.ir_sessions.append(session)
                    }
                }

                try modelContext.save()

                if networkState.mode == .online {
                    do {
                        _ = try await APIClient.shared.createNodeSessionMapping(
                            nodeId: node.id,
                            sessionId: session.id
                        )
                        AppLogger.log(.info, "Node-session mapping synced to server", category: .location)
                    } catch {
                        AppLogger.log(.error, "Failed to sync mapping, queuing for later: \(error)", category: .location)
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
                    AppLogger.log(.info, "Offline mode - queuing mapping for later", category: .location)
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

                await MainActor.run {
                    isProcessingQR = false

                    let room = node.room
                    qrScanResult = room != nil ? .success(node: node, room: room!) : .noRoom(node: node)

                    qrScannedNode = node

                    if let room = room {
                        showRoomForQRScan = room
                    } else {
                        showNoLocationDetail = true
                    }
                }
            } catch {
                await MainActor.run {
                    isProcessingQR = false
                    errorMessage = "Failed to add asset: \(error.localizedDescription)"
                }
            }
        }
    }

    // MARK: - No Location Metrics

    func calculateNoLocationTaskCompletionRatio() -> (completed: Int, total: Int) {
        guard let session = session else { return (0, 0) }

        let sessionNodesWithoutRoom = session.nodes.filter { !$0.is_deleted && $0.room == nil }
        return calculateRoomTaskCompletionRatio(nodes: sessionNodesWithoutRoom)
    }

    func calculateNoLocationIssueResolutionRatio() -> (resolved: Int, total: Int) {
        guard let session = session else { return (0, 0) }

        let sessionNodesWithoutRoom = session.nodes.filter { !$0.is_deleted && $0.room == nil }
        return calculateRoomIssueResolutionRatio(nodes: sessionNodesWithoutRoom)
    }

    // MARK: - Task Completion Ratios

    func calculateRoomTaskCompletionRatio(nodes: [NodeV2]) -> (completed: Int, total: Int) {
        guard let session = session else { return (0, 0) }

        var completedCount = 0
        var totalCount = 0

        let sessionNodeIds = Set(session.nodes.map { $0.id })
        let sessionTaskIds = Set(session.user_tasks.map { $0.id })

        for node in nodes where sessionNodeIds.contains(node.id) {
            let tasks = node.node_tasks.filter { !$0.is_deleted && sessionTaskIds.contains($0.id) }
            for task in tasks {
                totalCount += 1
                if task.nodeCompletions[node.id.uuidString] ?? false {
                    completedCount += 1
                }
            }
        }
        return (completedCount, totalCount)
    }

    func calculateFloorTaskCompletionRatio(rooms: [Room]) -> (completed: Int, total: Int) {
        guard let modelContext = modelContext else { return (0, 0) }

        var completedCount = 0
        var totalCount = 0

        for room in rooms {
            do {
                let nodes = try LocationHierarchyService.getNodesForRoom(room, modelContext: modelContext)
                let ratio = calculateRoomTaskCompletionRatio(nodes: nodes)
                completedCount += ratio.completed
                totalCount += ratio.total
            } catch {
                AppLogger.log(.error, "Failed to calculate task completion: \(error)", category: .location)
            }
        }
        return (completedCount, totalCount)
    }

    func calculateBuildingTaskCompletionRatio(floors: [Floor]) -> (completed: Int, total: Int) {
        guard let modelContext = modelContext else { return (0, 0) }

        var completedCount = 0
        var totalCount = 0

        for floor in floors {
            do {
                let rooms = try LocationHierarchyService.getRoomsForFloor(floor, modelContext: modelContext)
                let ratio = calculateFloorTaskCompletionRatio(rooms: rooms)
                completedCount += ratio.completed
                totalCount += ratio.total
            } catch {
                AppLogger.log(.error, "Failed to calculate task completion: \(error)", category: .location)
            }
        }
        return (completedCount, totalCount)
    }

    // MARK: - Issue Resolution Ratios

    func calculateRoomIssueResolutionRatio(nodes: [NodeV2]) -> (resolved: Int, total: Int) {
        guard let session = session else { return (0, 0) }

        var resolvedCount = 0
        var totalCount = 0

        let sessionNodeIds = Set(session.nodes.map { $0.id })
        let sessionIssueIds = Set(session.issues.map { $0.id })

        for node in nodes where sessionNodeIds.contains(node.id) {
            let issues = node.issues.filter { !$0.is_deleted && sessionIssueIds.contains($0.id) }
            for issue in issues {
                totalCount += 1
                if issue.status?.lowercased() == "resolved" {
                    resolvedCount += 1
                }
            }
        }
        return (resolvedCount, totalCount)
    }

    func calculateFloorIssueResolutionRatio(rooms: [Room]) -> (resolved: Int, total: Int) {
        guard let modelContext = modelContext else { return (0, 0) }

        var resolvedCount = 0
        var totalCount = 0

        for room in rooms {
            do {
                let nodes = try LocationHierarchyService.getNodesForRoom(room, modelContext: modelContext)
                let ratio = calculateRoomIssueResolutionRatio(nodes: nodes)
                resolvedCount += ratio.resolved
                totalCount += ratio.total
            } catch {
                AppLogger.log(.error, "Failed to calculate issue resolution: \(error)", category: .location)
            }
        }
        return (resolvedCount, totalCount)
    }

    func calculateBuildingIssueResolutionRatio(floors: [Floor]) -> (resolved: Int, total: Int) {
        guard let modelContext = modelContext else { return (0, 0) }

        var resolvedCount = 0
        var totalCount = 0

        for floor in floors {
            do {
                let rooms = try LocationHierarchyService.getRoomsForFloor(floor, modelContext: modelContext)
                let ratio = calculateFloorIssueResolutionRatio(rooms: rooms)
                resolvedCount += ratio.resolved
                totalCount += ratio.total
            } catch {
                AppLogger.log(.error, "Failed to calculate issue resolution: \(error)", category: .location)
            }
        }
        return (resolvedCount, totalCount)
    }

    // MARK: - Sheet Dismissal Handlers

    func onAddBuildingDismissed() {
        showAddBuilding = false
        loadBuildings()
    }

    func onEditBuildingDismissed() {
        editBuildingData = nil
        loadBuildings()
    }

    func onAddFloorDismissed() {
        addFloorToBuilding = nil
        loadBuildings()
    }

    func onEditFloorDismissed() {
        editFloorData = nil
        loadBuildings()
    }

    func onAddRoomDismissed() {
        addRoomToFloor = nil
        loadBuildings()
    }

    func onEditRoomDismissed() {
        editRoomData = nil
        loadBuildings()
    }

    func onNoLocationDetailDismissed() {
        loadNoLocationCount()
    }
}
