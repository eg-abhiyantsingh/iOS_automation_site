//
//  RoomEditorView.swift
//  Egalvanic PZ
//
//  Editor for creating and editing rooms
//

import SwiftUI
import SwiftData

struct RoomEditorView: View {
    @Environment(\.modelContext) private var modelContext

    let floor: Floor
    let room: Room?
    let mode: EditorMode
    let onSave: (Room) -> Void
    let onCancel: () -> Void

    @State private var name: String = ""
    @State private var accessNotes: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var displayedPhotos: [Photo] = []

    @State private var selectedFloor: Floor?
    @State private var selectedBuilding: Building?
    @State private var originalFloor: Floor
    @State private var showBuildingPicker = false
    @State private var showFloorPicker = false
    @State private var showMoveConfirm = false

    init(floor: Floor, room: Room? = nil, mode: EditorMode, onSave: @escaping (Room) -> Void, onCancel: @escaping () -> Void) {
        self.floor = floor
        self.room = room
        self.mode = mode
        self.onSave = onSave
        self.onCancel = onCancel

        let initialFloor = room?.floor ?? floor
        _selectedFloor = State(initialValue: initialFloor)
        _selectedBuilding = State(initialValue: initialFloor.building)
        _originalFloor = State(initialValue: initialFloor)

        if let room = room {
            _name = State(initialValue: room.name)
            _accessNotes = State(initialValue: room.access_notes ?? "")
            _displayedPhotos = State(initialValue: room.photos.filter { !$0.is_deleted })
        }
    }

    private var availableBuildings: [Building] {
        (selectedBuilding?.sld?.buildings ?? [])
            .filter { !$0.is_deleted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var availableFloors: [Floor] {
        (selectedBuilding?.floors ?? [])
            .filter { !$0.is_deleted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var canPickBuilding: Bool {
        mode == .edit && availableBuildings.count > 1
    }

    private var canPickFloor: Bool {
        mode == .edit && availableFloors.count > 1
    }

    private var hasFloorChanged: Bool {
        guard mode == .edit, let selectedFloor = selectedFloor else { return false }
        return selectedFloor.id != originalFloor.id
    }

    private var canSave: Bool {
        !name.isEmpty && !isSaving && selectedFloor != nil
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(AppStrings.Locations.roomInformation)) {
                    TextField(AppStrings.Locations.roomName, text: $name)
                        .autocapitalization(.words)

                    // Building selector — picker only when there are alternates
                    if canPickBuilding {
                        Button(action: { showBuildingPicker = true }) {
                            HStack {
                                Text(AppStrings.Locations.building)
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(selectedBuilding?.name ?? "")
                                    .foregroundColor(.primary)
                                Image(systemName: "chevron.right")
                                    .foregroundColor(.secondary)
                                    .font(.caption)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainButtonStyle())
                    } else if let building = selectedBuilding {
                        HStack {
                            Text(AppStrings.Locations.building)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(building.name)
                            Image(systemName: "chevron.right")
                                .foregroundColor(Color.secondary.opacity(0.4))
                                .font(.caption)
                        }
                    }

                    // Floor selector — cascades from building
                    if canPickFloor {
                        Button(action: { showFloorPicker = true }) {
                            HStack {
                                Text(AppStrings.Locations.floor)
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(selectedFloor?.name ?? "")
                                    .foregroundColor(selectedFloor == nil ? .secondary : .primary)
                                Image(systemName: "chevron.right")
                                    .foregroundColor(.secondary)
                                    .font(.caption)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainButtonStyle())
                    } else {
                        HStack {
                            Text(AppStrings.Locations.floor)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(selectedFloor?.name ?? "")
                            Image(systemName: "chevron.right")
                                .foregroundColor(Color.secondary.opacity(0.4))
                                .font(.caption)
                        }
                        if mode == .edit && selectedBuilding != nil && availableFloors.count <= 1 {
                            Text(AppStrings.Locations.noOtherFloors)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Section(header: Text(AppStrings.Locations.accessNotes)) {
                    TextEditor(text: $accessNotes)
                        .frame(minHeight: 100)
                }

                // Photo picker section (only in edit mode)
                if mode == .edit, let room = room {
                    Section {
                        EntitySimplePhotoPicker(
                            entity: room,
                            photoType: "room",
                            displayedPhotos: $displayedPhotos,
                            isSaving: $isSaving,
                            onPhotoAdded: { photo in
                                modelContext.insert(photo)
                                room.associatePhoto(photo)
                                displayedPhotos.append(photo)
                                do {
                                    try modelContext.save()
                                } catch {
                                    AppLogger.log(.error, "Failed to save photo: \(error)", category: .location)
                                }
                            },
                            onPhotoDeleted: { photo in
                                PhotoService.deletePhoto(
                                    photo,
                                    modelContext: modelContext
                                ) { success, error in
                                    if success {
                                        displayedPhotos.removeAll { $0.id == photo.id }
                                    } else {
                                        AppLogger.log(.error, "Failed to delete photo: \(error ?? "unknown error")", category: .location)
                                    }
                                }
                            }
                        )
                    }
                }

                if let errorMessage = errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle(mode == .create ? AppStrings.Locations.newRoom : AppStrings.Locations.editRoomTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        onCancel()
                    }
                    .disabled(isSaving)
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.save) {
                        if hasFloorChanged {
                            showMoveConfirm = true
                        } else {
                            saveRoom()
                        }
                    }
                    .disabled(!canSave)
                }
            }
            .sheet(isPresented: $showBuildingPicker) {
                LocationPickerSheet(
                    title: AppStrings.Locations.selectBuilding,
                    searchPrompt: AppStrings.Locations.searchBuildings,
                    emptyStateText: AppStrings.Locations.noBuildingsFound,
                    iconSystemName: "building.2",
                    items: availableBuildings,
                    selectedItemId: selectedBuilding?.id,
                    idKeyPath: \.id,
                    nameKeyPath: \.name,
                    onSelect: { building in
                        selectedBuilding = building
                        let floors = building.floors
                            .filter { !$0.is_deleted }
                        // If the previously-selected floor still belongs to this building, keep it.
                        // Else, auto-select when there's exactly one option; otherwise clear.
                        if let current = selectedFloor, floors.contains(where: { $0.id == current.id }) {
                            // keep
                        } else if floors.count == 1 {
                            selectedFloor = floors.first
                        } else {
                            selectedFloor = nil
                        }
                    },
                    isPresented: $showBuildingPicker
                )
            }
            .sheet(isPresented: $showFloorPicker) {
                LocationPickerSheet(
                    title: AppStrings.Locations.selectFloor,
                    searchPrompt: AppStrings.Locations.searchFloors,
                    emptyStateText: AppStrings.Locations.noFloorsFound,
                    iconSystemName: "square.stack.3d.up",
                    items: availableFloors,
                    selectedItemId: selectedFloor?.id,
                    idKeyPath: \.id,
                    nameKeyPath: \.name,
                    onSelect: { floor in
                        selectedFloor = floor
                    },
                    isPresented: $showFloorPicker
                )
            }
            .alert(AppStrings.Locations.moveRoomTitle, isPresented: $showMoveConfirm) {
                Button(AppStrings.Common.cancel, role: .cancel) { }
                Button(AppStrings.Locations.moveConfirm) {
                    saveRoom()
                }
            } message: {
                Text(AppStrings.Locations.moveRoomMessage(
                    name: name,
                    floor: selectedFloor?.name ?? "",
                    building: selectedBuilding?.name ?? ""
                ))
            }
        }
    }

    private func saveRoom() {
        guard !name.isEmpty, let targetFloor = selectedFloor else { return }

        isSaving = true
        errorMessage = nil

        let networkState = NetworkState.shared
        Task {
            do {
                if mode == .create {
                    let newRoom = try await LocationHierarchyService.createRoom(
                        name: name,
                        floor: targetFloor,
                        accessNotes: accessNotes.isEmpty ? nil : accessNotes,
                        networkState: networkState,
                        modelContext: modelContext
                    )
                    await MainActor.run {
                        onSave(newRoom)
                    }
                } else if let room = room {
                    try await LocationHierarchyService.updateRoom(
                        room,
                        name: name,
                        floor: targetFloor,
                        accessNotes: accessNotes.isEmpty ? nil : accessNotes,
                        networkState: networkState,
                        modelContext: modelContext
                    )

                    // Upload any pending photos in background
                    Task.detached {
                        await PhotoService.uploadPendingPhotos(
                            for: room,
                            stagedDeletions: Set<UUID>(),
                            displayedPhotos: displayedPhotos,
                            modelContext: modelContext,
                            networkState: networkState
                        )
                    }

                    await MainActor.run {
                        onSave(room)
                    }
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Failed to save: \(error.localizedDescription)"
                    isSaving = false
                }
            }
        }
    }
}
