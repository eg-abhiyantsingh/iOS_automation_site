//
//  HierarchicalLocationPickerView.swift
//  Egalvanic PZ
//
//  Hierarchical location picker for selecting or creating rooms
//

import SwiftUI
import SwiftData

struct HierarchicalLocationPickerView: View {
    let sld: SLDV2
    @Binding var selectedRoom: Room?
    var readOnly: Bool = false
    let onDismiss: () -> Void

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    // Sheet content type - consolidated to single sheet to avoid SwiftUI multi-sheet bug
    private enum SheetContent: Identifiable, Equatable {
        case addBuilding
        case addFloor(Building)
        case addRoom(Floor)

        var id: String {
            switch self {
            case .addBuilding: return "addBuilding"
            case .addFloor(let b): return "addFloor-\(b.id)"
            case .addRoom(let f): return "addRoom-\(f.id)"
            }
        }

        static func == (lhs: SheetContent, rhs: SheetContent) -> Bool {
            lhs.id == rhs.id
        }
    }

    @State private var buildings: [Building] = []
    @State private var expandedBuildings: Set<UUID> = []
    @State private var expandedFloors: Set<UUID> = []
    @State private var activeSheet: SheetContent?
    @State private var errorMessage: String?
    @State private var buildingsRefreshTrigger = UUID()

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                if buildings.isEmpty {
                    ContentUnavailableView(
                        AppStrings.Locations.noLocations,
                        systemImage: "building.2",
                        description: Text(AppStrings.Locations.createBuildingToStart)
                    )
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            ForEach(buildings) { building in
                                LocationPickerBuildingCard(
                                    building: building,
                                    isExpanded: expandedBuildings.contains(building.id),
                                    expandedFloors: $expandedFloors,
                                    selectedRoom: $selectedRoom,
                                    readOnly: readOnly,
                                    refreshTrigger: buildingsRefreshTrigger,
                                    onToggleBuilding: {
                                        toggleBuilding(building)
                                    },
                                    onSelectRoom: { room in
                                        selectedRoom = room
                                        onDismiss()
                                        dismiss()
                                    },
                                    onAddFloor: {
                                        activeSheet = .addFloor(building)
                                    },
                                    onAddRoom: { floor in
                                        activeSheet = .addRoom(floor)
                                    },
                                    modelContext: modelContext
                                )
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical)
                    }
                }
            }
            .background(Color(UIColor.systemGroupedBackground))
            .navigationTitle(AppStrings.AssetsExtra.selectLocation)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        onDismiss()
                        dismiss()
                    }
                }

                if !readOnly {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button(action: {
                            activeSheet = .addBuilding
                        }) {
                            Image(systemName: "plus")
                        }
                    }
                }
            }
            .onAppear {
                loadBuildings()
            }
            .fullScreenCover(item: $activeSheet) { content in
                switch content {
                case .addBuilding:
                    BuildingEditorView(
                        sld: sld,
                        mode: .create,
                        onSave: { _ in
                            activeSheet = nil
                            loadBuildings()
                            buildingsRefreshTrigger = UUID()
                        },
                        onCancel: {
                            activeSheet = nil
                        }
                    )
                    .environment(\.modelContext, modelContext)

                case .addFloor(let building):
                    FloorEditorView(
                        building: building,
                        mode: .create,
                        onSave: { _ in
                            activeSheet = nil
                            loadBuildings()
                            buildingsRefreshTrigger = UUID()
                        },
                        onCancel: {
                            activeSheet = nil
                        }
                    )
                    .environment(\.modelContext, modelContext)

                case .addRoom(let floor):
                    RoomEditorView(
                        floor: floor,
                        mode: .create,
                        onSave: { newRoom in
                            activeSheet = nil
                            loadBuildings()
                            buildingsRefreshTrigger = UUID()
                            selectedRoom = newRoom
                            onDismiss()
                            dismiss()
                        },
                        onCancel: {
                            activeSheet = nil
                        }
                    )
                    .environment(\.modelContext, modelContext)
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
        }
    }

    private func loadBuildings() {
        do {
            buildings = try LocationHierarchyService.getBuildingsForSLD(sld, modelContext: modelContext)
            // Expand all buildings by default
            expandedBuildings = Set(buildings.map { $0.id })
            // Also expand all floors within each building
            for building in buildings {
                let floors = try LocationHierarchyService.getFloorsForBuilding(building, modelContext: modelContext)
                expandedFloors.formUnion(floors.map { $0.id })
            }
        } catch {
            errorMessage = "Failed to load buildings: \(error.localizedDescription)"
        }
    }

    private func toggleBuilding(_ building: Building) {
        if expandedBuildings.contains(building.id) {
            expandedBuildings.remove(building.id)
        } else {
            expandedBuildings.insert(building.id)
        }
    }
}

// MARK: - Location Picker Building Card

struct LocationPickerBuildingCard: View {
    let building: Building
    let isExpanded: Bool
    @Binding var expandedFloors: Set<UUID>
    @Binding var selectedRoom: Room?
    var readOnly: Bool = false
    let refreshTrigger: UUID
    let onToggleBuilding: () -> Void
    let onSelectRoom: (Room) -> Void
    let onAddFloor: () -> Void
    let onAddRoom: (Floor) -> Void
    let modelContext: ModelContext

    @State private var floors: [Floor] = []
    @State private var floorsRefreshTrigger = UUID()
    @State private var showingAccessNotes = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Building Header
            Button(action: onToggleBuilding) {
                HStack(spacing: 12) {
                    // Only show chevron if building has floors
                    if !floors.isEmpty {
                        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(isExpanded ? .blue : .secondary)
                            .frame(width: 20)
                    } else {
                        Color.clear
                            .frame(width: 20)
                    }

                    Image(systemName: "building.2.fill")
                        .font(.title3)
                        .foregroundColor(.blue)

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

                    // Add Floor button
                    if !readOnly {
                        Button(action: onAddFloor) {
                            Image(systemName: "plus.circle.fill")
                                .font(.title3)
                                .foregroundColor(.blue)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .background(Color(UIColor.systemBackground))

            // Expanded Content: Floors
            if isExpanded {
                Divider()
                    .padding(.horizontal, 16)

                VStack(alignment: .leading, spacing: 0) {
                    ForEach(floors) { floor in
                        LocationPickerFloorRow(
                            floor: floor,
                            isExpanded: expandedFloors.contains(floor.id),
                            selectedRoom: $selectedRoom,
                            readOnly: readOnly,
                            refreshTrigger: floorsRefreshTrigger,
                            onToggle: {
                                if expandedFloors.contains(floor.id) {
                                    expandedFloors.remove(floor.id)
                                } else {
                                    expandedFloors.insert(floor.id)
                                }
                            },
                            onSelectRoom: onSelectRoom,
                            onAddRoom: {
                                onAddRoom(floor)
                            },
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
        .alert(AppStrings.Locations.accessNotes, isPresented: $showingAccessNotes) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(building.access_notes ?? "")
        }
        .onAppear {
            loadFloors()
        }
        .onChange(of: isExpanded) { _, _ in
            loadFloors()
        }
        .onChange(of: refreshTrigger) { _, _ in
            // Parent has reloaded buildings, refresh our floors
            loadFloors()
        }
    }

    private func loadFloors() {
        do {
            floors = try LocationHierarchyService.getFloorsForBuilding(building, modelContext: modelContext)
            floorsRefreshTrigger = UUID() // Trigger refresh in all FloorRows
        } catch {
            AppLogger.log(.error, "Failed to load floors: \(error)", category: .location)
        }
    }
}

// MARK: - Location Picker Floor Row

struct LocationPickerFloorRow: View {
    let floor: Floor
    let isExpanded: Bool
    @Binding var selectedRoom: Room?
    var readOnly: Bool = false
    let refreshTrigger: UUID
    let onToggle: () -> Void
    let onSelectRoom: (Room) -> Void
    let onAddRoom: () -> Void
    let modelContext: ModelContext

    @State private var rooms: [Room] = []
    @State private var showingAccessNotes = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Floor Header
            Button(action: onToggle) {
                HStack(spacing: 12) {
                    Color.clear.frame(width: 20)

                    // Only show chevron if floor has rooms
                    if !rooms.isEmpty {
                        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(isExpanded ? .blue : .secondary)
                            .frame(width: 20)
                    } else {
                        Color.clear
                            .frame(width: 20)
                    }

                    Image(systemName: "square.stack.3d.up.fill")
                        .font(.body)
                        .foregroundColor(.green)

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

                    // Add Room button
                    if !readOnly {
                        Button(action: onAddRoom) {
                            Image(systemName: "plus.circle.fill")
                                .font(.body)
                                .foregroundColor(.green)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            // Expanded Content: Rooms
            if isExpanded {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(rooms) { room in
                        LocationPickerRoomRow(
                            room: room,
                            isSelected: selectedRoom?.id == room.id,
                            onSelect: {
                                onSelectRoom(room)
                            },
                            modelContext: modelContext
                        )
                    }
                }
            }
        }
        .alert(AppStrings.Locations.accessNotes, isPresented: $showingAccessNotes) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(floor.access_notes ?? "")
        }
        .onAppear {
            loadRooms()
        }
        .onChange(of: isExpanded) { _, _ in
            loadRooms()
        }
        .onChange(of: refreshTrigger) { _, _ in
            // Parent has reloaded floors, refresh our rooms
            loadRooms()
        }
    }

    private func loadRooms() {
        do {
            rooms = try LocationHierarchyService.getRoomsForFloor(floor, modelContext: modelContext)
        } catch {
            AppLogger.log(.error, "Failed to load rooms: \(error)", category: .location)
        }
    }
}

// MARK: - Location Picker Room Row

struct LocationPickerRoomRow: View {
    let room: Room
    let isSelected: Bool
    let onSelect: () -> Void
    let modelContext: ModelContext

    @State private var nodeCount: Int = 0
    @State private var showingAccessNotes = false

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 12) {
                Color.clear.frame(width: 20) // Building indent
                Color.clear.frame(width: 20) // Floor chevron space
                Color.clear.frame(width: 20) // Room indent

                Image(systemName: "door.left.hand.open")
                    .font(.body)
                    .foregroundColor(.orange)

                VStack(alignment: .leading, spacing: 2) {
                    Text(room.name)
                        .font(.system(size: 15, weight: .regular))
                        .foregroundColor(.primary)

                    if nodeCount > 0 {
                        Text(AppStrings.Locations.nodesCount(nodeCount))
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

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.title3)
                        .foregroundColor(.blue)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(isSelected ? Color.blue.opacity(0.1) : Color.clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .alert(AppStrings.Locations.accessNotes, isPresented: $showingAccessNotes) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(room.access_notes ?? "")
        }
        .onAppear {
            loadNodeCount()
        }
    }

    private func loadNodeCount() {
        do {
            let nodes = try LocationHierarchyService.getNodesForRoom(room, modelContext: modelContext)
            nodeCount = nodes.count
        } catch {
            AppLogger.log(.error, "Failed to load node count: \(error)", category: .location)
        }
    }
}
