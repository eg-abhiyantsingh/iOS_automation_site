//
//  CopyAssetsToRoomsViewModel.swift
//  Egalvanic PZ
//
//  ZP-2198: State machine for the "Copy assets to other rooms" wizard.
//  Mirrors Android's CopyAssetsToRoomsViewModel (commit cea4810).
//

import Foundation
import SwiftData
import SwiftUI

enum CopyWizardStep {
    case targetRooms
    case strategy
    case confirm
    case running
    case done
}

enum CopyConflictStrategy {
    case append
    case overwrite
}

struct CopyRoomItem: Identifiable, Hashable {
    let id: UUID
    let roomName: String
    let floorName: String
    let buildingName: String

    var breadcrumb: String {
        "\(buildingName) › \(floorName) › \(roomName)"
    }
}

struct CopyTargetResult {
    var created: Int = 0
    var failed: Int = 0
    var skipped: Bool = false
    var errorMessage: String? = nil
}

@MainActor
final class CopyAssetsToRoomsViewModel: ObservableObject {

    // MARK: - State

    @Published var step: CopyWizardStep = .targetRooms
    @Published var isLoading: Bool = true
    @Published var sourceBreadcrumb: String = ""
    @Published var targetRooms: [CopyRoomItem] = []
    @Published var selectedTargetRoomIds: Set<UUID> = []
    @Published var roomSearch: String = ""
    @Published var existingAssetsByTarget: [UUID: Int] = [:]
    @Published var sourceParentCount: Int = 0
    @Published var strategy: CopyConflictStrategy = .append

    @Published var totalRooms: Int = 0
    @Published var completedRooms: Int = 0
    @Published var perTargetResult: [UUID: CopyTargetResult] = [:]
    @Published var totalCreated: Int = 0
    @Published var totalFailed: Int = 0
    /// True if the network was in offline mode when `start()` ran. Drives the wording of
    /// the Done banner (offline shows the "saved locally / will sync" caveat).
    @Published var wasOffline: Bool = false

    @Published var error: String? = nil

    // MARK: - Inputs (configured at sheet open)

    private var sourceRoom: Room?
    private var session: IRSession?
    private var sld: SLDV2?
    private var modelContext: ModelContext?
    private var networkState: NetworkState?

    // Cached at load time so step.start() doesn't have to walk relationships again.
    private var sourceParents: [NodeV2] = []

    // MARK: - Configuration

    func initialize(
        sourceRoom: Room,
        session: IRSession,
        sld: SLDV2,
        modelContext: ModelContext,
        networkState: NetworkState
    ) {
        // Reset state on every open.
        self.step = .targetRooms
        self.isLoading = true
        self.targetRooms = []
        self.selectedTargetRoomIds = []
        self.roomSearch = ""
        self.existingAssetsByTarget = [:]
        self.sourceParentCount = 0
        self.strategy = .append
        self.totalRooms = 0
        self.completedRooms = 0
        self.perTargetResult = [:]
        self.totalCreated = 0
        self.totalFailed = 0
        self.wasOffline = false
        self.error = nil

        self.sourceRoom = sourceRoom
        self.session = session
        self.sld = sld
        self.modelContext = modelContext
        self.networkState = networkState

        sourceBreadcrumb = sourceRoom.fullPath

        // Source parents = top-level session-mapped nodes in this room.
        let parents = session.nodes.filter {
            !$0.is_deleted && $0.room?.id == sourceRoom.id && $0.parent_id == nil
        }
        sourceParents = parents
        sourceParentCount = parents.count

        // Build the target room list (every non-deleted room in the SLD except the source).
        var items: [CopyRoomItem] = []
        var existingMap: [UUID: Int] = [:]
        for building in sld.buildings where !building.is_deleted {
            for floor in (building.floors) where !floor.is_deleted {
                for room in floor.rooms where !room.is_deleted && room.id != sourceRoom.id {
                    items.append(CopyRoomItem(
                        id: room.id,
                        roomName: room.name,
                        floorName: floor.name,
                        buildingName: building.name
                    ))
                    // ZP-2234: count every session-mapped asset in the room (parents AND
                    // children) so the per-row badge and Overwrite warning match what
                    // the unmap loop in start() will actually clear.
                    let existingCount = session.nodes.filter {
                        !$0.is_deleted && $0.room?.id == room.id
                    }.count
                    if existingCount > 0 {
                        existingMap[room.id] = existingCount
                    }
                }
            }
        }
        targetRooms = items.sorted { $0.breadcrumb.localizedCaseInsensitiveCompare($1.breadcrumb) == .orderedAscending }
        existingAssetsByTarget = existingMap

        isLoading = false
    }

    // MARK: - Step navigation

    var canGoNext: Bool {
        switch step {
        case .targetRooms:
            return !selectedTargetRoomIds.isEmpty && sourceParentCount > 0
        case .strategy, .confirm:
            return true
        case .running, .done:
            return false
        }
    }

    var canGoBack: Bool {
        switch step {
        case .strategy, .confirm:
            return true
        default:
            return false
        }
    }

    func goNext() {
        switch step {
        case .targetRooms:
            guard canGoNext else { return }
            step = .strategy
        case .strategy:
            step = .confirm
        case .confirm:
            step = .running
            Task { await start() }
        case .running, .done:
            break
        }
    }

    func goBack() {
        switch step {
        case .strategy:
            step = .targetRooms
        case .confirm:
            step = .strategy
        default:
            break
        }
    }

    // MARK: - Selection

    var visibleTargetRooms: [CopyRoomItem] {
        let trimmed = roomSearch.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return targetRooms }
        let needle = trimmed.lowercased()
        return targetRooms.filter { $0.breadcrumb.lowercased().contains(needle) }
    }

    func toggleTargetRoom(_ id: UUID) {
        if selectedTargetRoomIds.contains(id) {
            selectedTargetRoomIds.remove(id)
        } else {
            selectedTargetRoomIds.insert(id)
        }
    }

    /// Toggles every currently visible (search-filtered) target room. If any visible row is
    /// not selected, selects all visible rows; otherwise clears them.
    func toggleAllVisibleTargetRooms() {
        let visibleIds = Set(visibleTargetRooms.map(\.id))
        let allSelected = !visibleIds.isEmpty && visibleIds.isSubset(of: selectedTargetRoomIds)
        if allSelected {
            selectedTargetRoomIds.subtract(visibleIds)
        } else {
            selectedTargetRoomIds.formUnion(visibleIds)
        }
    }

    func setStrategy(_ s: CopyConflictStrategy) {
        strategy = s
    }

    func setRoomSearch(_ q: String) {
        roomSearch = q
    }

    func clearError() {
        error = nil
    }

    // MARK: - Confirm helpers

    /// Total existing session parents across selected targets. Drives the OVERWRITE warning.
    var overwriteUnlinkCount: Int {
        selectedTargetRoomIds.reduce(0) { $0 + (existingAssetsByTarget[$1] ?? 0) }
    }

    var selectedTargetRooms: [CopyRoomItem] {
        targetRooms.filter { selectedTargetRoomIds.contains($0.id) }
    }

    // MARK: - Execution

    func start() async {
        guard
            let session = session,
            let sld = sld,
            let modelContext = modelContext,
            let networkState = networkState
        else {
            error = "Wizard not configured"
            step = .done
            return
        }
        guard !sourceParents.isEmpty else {
            step = .done
            return
        }

        let selectedTargets = targetRooms.filter { selectedTargetRoomIds.contains($0.id) }
        totalRooms = selectedTargets.count
        completedRooms = 0
        perTargetResult = [:]
        totalCreated = 0
        totalFailed = 0
        // Snapshot connectivity once at start so the Done banner reflects the mode the
        // copy actually ran under, even if the user toggles network mode mid-run.
        // ZP-2234: use !canDirectSync (not just mode == .offline) so "online but with a
        // pending sync queue" still shows the offline message — NodeService enqueues writes
        // in that case, so they really do "save locally and sync when online" from the
        // user's perspective.
        wasOffline = !networkState.canDirectSync

        // Build sourceChildren map up front: for each parent, its non-deleted children.
        let allNodes = sld.nodes
        let parentIdToChildren: [UUID: [NodeV2]] = Dictionary(
            uniqueKeysWithValues: sourceParents.map { parent in
                let kids = allNodes.filter { !$0.is_deleted && $0.parent_id == parent.id }
                return (parent.id, kids)
            }
        )

        for target in selectedTargets {
            guard let targetRoom = lookupRoom(id: target.id, in: sld) else {
                perTargetResult[target.id] = CopyTargetResult(skipped: true, errorMessage: "Target room not found")
                completedRooms += 1
                continue
            }

            // OVERWRITE: unmap every session-mapped asset (parents + children, ZP-2234) in
            // the target room first. Halt-on-failure: if unmapping fails, skip cloning into
            // this room entirely.
            if strategy == .overwrite {
                do {
                    try await CopyAssetsToRoomsService.unmapSessionAssets(
                        targetRoom: targetRoom,
                        session: session,
                        modelContext: modelContext,
                        networkState: networkState
                    )
                } catch {
                    perTargetResult[target.id] = CopyTargetResult(
                        skipped: true,
                        errorMessage: error.localizedDescription
                    )
                    completedRooms += 1
                    continue
                }
            }

            var cloneMap: [UUID: UUID] = [:]
            var roomCounts = CopyAssetsToRoomsService.CloneCounts()

            for parent in sourceParents {
                let children = parentIdToChildren[parent.id] ?? []
                let counts = await CopyAssetsToRoomsService.cloneParentAndChildren(
                    sourceParent: parent,
                    sourceChildren: children,
                    targetRoom: targetRoom,
                    session: session,
                    sld: sld,
                    cloneMap: &cloneMap,
                    modelContext: modelContext,
                    networkState: networkState
                )
                roomCounts.created += counts.created
                roomCounts.failed += counts.failed
            }

            // Edges that connect two cloned nodes inside this room (regardless of which
            // source room those nodes lived in — Android filters by "both ends in cloneMap"
            // and we mirror that).
            let intraEdges = sld.edges.filter { e in
                guard !e.is_deleted, let s = e.source, let t = e.target else { return false }
                return cloneMap.keys.contains(s) && cloneMap.keys.contains(t)
            }
            let edgeCounts = await CopyAssetsToRoomsService.cloneIntraRoomEdges(
                sourceEdges: intraEdges,
                cloneMap: cloneMap,
                sld: sld,
                modelContext: modelContext,
                networkState: networkState
            )
            roomCounts.failed += edgeCounts.failed

            perTargetResult[target.id] = CopyTargetResult(
                created: roomCounts.created,
                failed: roomCounts.failed,
                skipped: false
            )
            totalCreated += roomCounts.created
            totalFailed += roomCounts.failed
            completedRooms += 1
        }

        // Persist any unsaved appends to session.nodes (NodeService.createNewNodeWithPhotosAndIR
        // saves the node insert, but the session relationship append happens after that save).
        // Mirrors the final save QuickCountView does after its per-node loop.
        do {
            try modelContext.save()
        } catch {
            self.error = error.localizedDescription
        }

        // Refresh the SLD canvas once at the end (cloning loop passes skipGraphUpdate: true
        // for performance).
        WebViewBridge.updateGraphFromSLD(sld.id, in: modelContext, animated: true)

        step = .done
    }

    private func lookupRoom(id: UUID, in sld: SLDV2) -> Room? {
        for building in sld.buildings where !building.is_deleted {
            for floor in building.floors where !floor.is_deleted {
                if let match = floor.rooms.first(where: { $0.id == id && !$0.is_deleted }) {
                    return match
                }
            }
        }
        return nil
    }
}
