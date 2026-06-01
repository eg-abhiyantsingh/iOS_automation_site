//
//  BuildingEditorView.swift
//  Egalvanic PZ
//
//  Editor for creating and editing buildings
//

import SwiftUI
import SwiftData

enum EditorMode {
    case create
    case edit
}

struct BuildingEditorView: View {
    @Environment(\.modelContext) private var modelContext

    let sld: SLDV2
    let building: Building?
    let mode: EditorMode
    let onSave: (Building) -> Void
    let onCancel: () -> Void

    @State private var name: String = ""
    @State private var accessNotes: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var displayedPhotos: [Photo] = []

    init(sld: SLDV2, building: Building? = nil, mode: EditorMode, onSave: @escaping (Building) -> Void, onCancel: @escaping () -> Void) {
        self.sld = sld
        self.building = building
        self.mode = mode
        self.onSave = onSave
        self.onCancel = onCancel

        if let building = building {
            _name = State(initialValue: building.name)
            _accessNotes = State(initialValue: building.access_notes ?? "")
            _displayedPhotos = State(initialValue: building.photos.filter { !$0.is_deleted })
        }
    }

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(AppStrings.Locations.buildingInformation)) {
                    TextField(AppStrings.Locations.buildingName, text: $name)
                        .autocapitalization(.words)
                }

                Section(header: Text(AppStrings.Locations.accessNotes)) {
                    TextEditor(text: $accessNotes)
                        .frame(minHeight: 100)
                }

                // Photo picker section (only in edit mode)
                if mode == .edit, let building = building {
                    Section {
                        EntitySimplePhotoPicker(
                            entity: building,
                            photoType: "building",
                            displayedPhotos: $displayedPhotos,
                            isSaving: $isSaving,
                            onPhotoAdded: { photo in
                                modelContext.insert(photo)
                                building.associatePhoto(photo)
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
            .navigationTitle(mode == .create ? AppStrings.Locations.newBuilding : AppStrings.Locations.editBuildingTitle)
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
                        saveBuilding()
                    }
                    .disabled(name.isEmpty || isSaving)
                }
            }
        }
    }

    private func saveBuilding() {
        guard !name.isEmpty else { return }

        isSaving = true
        errorMessage = nil

        let networkState = NetworkState.shared
        Task {
            do {
                if mode == .create {
                    let newBuilding = try await LocationHierarchyService.createBuilding(
                        name: name,
                        sld: sld,
                        accessNotes: accessNotes.isEmpty ? nil : accessNotes,
                        networkState: networkState,
                        modelContext: modelContext
                    )
                    await MainActor.run {
                        onSave(newBuilding)
                    }
                } else if let building = building {
                    try await LocationHierarchyService.updateBuilding(
                        building,
                        name: name,
                        accessNotes: accessNotes.isEmpty ? nil : accessNotes,
                        networkState: networkState,
                        modelContext: modelContext
                    )

                    // Upload any pending photos in background
                    Task {
                        await PhotoService.uploadPendingPhotos(
                            for: building,
                            stagedDeletions: Set<UUID>(),
                            displayedPhotos: displayedPhotos,
                            modelContext: modelContext,
                            networkState: networkState
                        )
                    }

                    await MainActor.run {
                        onSave(building)
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
