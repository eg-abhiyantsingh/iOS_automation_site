//
//  FloorEditorView.swift
//  Egalvanic PZ
//
//  Editor for creating and editing floors
//

import SwiftUI
import SwiftData

struct FloorEditorView: View {
    @Environment(\.modelContext) private var modelContext

    let building: Building
    let floor: Floor?
    let mode: EditorMode
    let onSave: (Floor) -> Void
    let onCancel: () -> Void

    @State private var name: String = ""
    @State private var accessNotes: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var displayedPhotos: [Photo] = []

    @State private var selectedBuilding: Building
    @State private var originalBuilding: Building
    @State private var showBuildingPicker = false
    @State private var showMoveConfirm = false

    init(building: Building, floor: Floor? = nil, mode: EditorMode, onSave: @escaping (Floor) -> Void, onCancel: @escaping () -> Void) {
        self.building = building
        self.floor = floor
        self.mode = mode
        self.onSave = onSave
        self.onCancel = onCancel

        let initialBuilding = floor?.building ?? building
        _selectedBuilding = State(initialValue: initialBuilding)
        _originalBuilding = State(initialValue: initialBuilding)

        if let floor = floor {
            _name = State(initialValue: floor.name)
            _accessNotes = State(initialValue: floor.access_notes ?? "")
            _displayedPhotos = State(initialValue: floor.photos.filter { !$0.is_deleted })
        }
    }

    private var availableBuildings: [Building] {
        (selectedBuilding.sld?.buildings ?? [])
            .filter { !$0.is_deleted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var canPickBuilding: Bool {
        mode == .edit && availableBuildings.count > 1
    }

    private var hasBuildingChanged: Bool {
        mode == .edit && selectedBuilding.id != originalBuilding.id
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(AppStrings.Locations.floorInformation)) {
                    TextField(AppStrings.Locations.floorName, text: $name)
                        .autocapitalization(.words)

                    if canPickBuilding {
                        Button(action: { showBuildingPicker = true }) {
                            HStack {
                                Text(AppStrings.Locations.building)
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(selectedBuilding.name)
                                    .foregroundColor(.primary)
                                Image(systemName: "chevron.right")
                                    .foregroundColor(.secondary)
                                    .font(.caption)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainButtonStyle())
                    } else {
                        HStack {
                            Text(AppStrings.Locations.building)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(selectedBuilding.name)
                        }
                        if mode == .edit && availableBuildings.count <= 1 {
                            Text(AppStrings.Locations.noOtherBuildings)
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
                if mode == .edit, let floor = floor {
                    Section {
                        EntitySimplePhotoPicker(
                            entity: floor,
                            photoType: "floor",
                            displayedPhotos: $displayedPhotos,
                            isSaving: $isSaving,
                            onPhotoAdded: { photo in
                                modelContext.insert(photo)
                                floor.associatePhoto(photo)
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
            .navigationTitle(mode == .create ? AppStrings.Locations.newFloor : AppStrings.Locations.editFloorTitle)
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
                        if hasBuildingChanged {
                            showMoveConfirm = true
                        } else {
                            saveFloor()
                        }
                    }
                    .disabled(name.isEmpty || isSaving)
                }
            }
            .sheet(isPresented: $showBuildingPicker) {
                LocationPickerSheet(
                    title: AppStrings.Locations.selectBuilding,
                    searchPrompt: AppStrings.Locations.searchBuildings,
                    emptyStateText: AppStrings.Locations.noBuildingsFound,
                    iconSystemName: "building.2",
                    items: availableBuildings,
                    selectedItemId: selectedBuilding.id,
                    idKeyPath: \.id,
                    nameKeyPath: \.name,
                    onSelect: { building in
                        selectedBuilding = building
                    },
                    isPresented: $showBuildingPicker
                )
            }
            .alert(AppStrings.Locations.moveFloorTitle, isPresented: $showMoveConfirm) {
                Button(AppStrings.Common.cancel, role: .cancel) { }
                Button(AppStrings.Locations.moveConfirm) {
                    saveFloor()
                }
            } message: {
                Text(AppStrings.Locations.moveFloorMessage(
                    name: name,
                    from: originalBuilding.name,
                    to: selectedBuilding.name
                ))
            }
        }
    }

    private func saveFloor() {
        guard !name.isEmpty else { return }

        isSaving = true
        errorMessage = nil

        let networkState = NetworkState.shared
        Task {
            do {
                if mode == .create {
                    let newFloor = try await LocationHierarchyService.createFloor(
                        name: name,
                        building: selectedBuilding,
                        accessNotes: accessNotes.isEmpty ? nil : accessNotes,
                        networkState: networkState,
                        modelContext: modelContext
                    )
                    await MainActor.run {
                        onSave(newFloor)
                    }
                } else if let floor = floor {
                    try await LocationHierarchyService.updateFloor(
                        floor,
                        name: name,
                        building: selectedBuilding,
                        accessNotes: accessNotes.isEmpty ? nil : accessNotes,
                        networkState: networkState,
                        modelContext: modelContext
                    )

                    // Upload any pending photos in background
                    Task.detached {
                        await PhotoService.uploadPendingPhotos(
                            for: floor,
                            stagedDeletions: Set<UUID>(),
                            displayedPhotos: displayedPhotos,
                            modelContext: modelContext,
                            networkState: networkState
                        )
                    }

                    await MainActor.run {
                        onSave(floor)
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
