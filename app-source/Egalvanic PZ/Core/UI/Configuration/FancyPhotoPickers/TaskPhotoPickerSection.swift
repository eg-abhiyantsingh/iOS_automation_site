//
//  TaskPhotoPickerSection.swift
//  SwiftDataTutorial
//
//  Configuration wrapper for Task fancy photo picker with multiple types
//
import SwiftUI
import SwiftData

struct TaskPhotoPickerSection: View {
    let task: UserTask
    @Binding var displayedPhotos: [Photo]
    @Binding var isSaving: Bool
    
    // Photo management callbacks
    var onPhotoAdded: ((Photo) -> Void)?
    var onPhotoDeleted: ((Photo) -> Void)?
    
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    
    var body: some View {
        VStack(spacing: 16) {
            EntityFancyPhotoPicker(
                entity: task,
                displayedPhotos: $displayedPhotos,
                isSaving: $isSaving,
                onPhotoAdded: onPhotoAdded,
                onPhotoDeleted: onPhotoDeleted,
                sectionTitle: AppStrings.Tasks.taskPhotos,
                sectionIcon: "photo.stack",
                showUploadIndicators: true,
                showPhotoTypeSelector: true
            )
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }
}