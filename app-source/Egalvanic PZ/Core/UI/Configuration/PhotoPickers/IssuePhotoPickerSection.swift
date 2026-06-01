//
//  IssuePhotoPickerSection.swift
//  SwiftDataTutorial
//
//  Configuration wrapper for Issue photo picker
//
import SwiftUI
import SwiftData

struct IssuePhotoPickerSection: View {
    let issue: Issue
    @Binding var displayedPhotos: [Photo]
    @Binding var isSaving: Bool
    
    // Photo management callbacks
    var onPhotoAdded: ((Photo) -> Void)?
    var onPhotoDeleted: ((Photo) -> Void)?
    
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    
    var body: some View {
        VStack(spacing: 16) {
            EntitySimplePhotoPicker(
                entity: issue,
                photoType: "issue",
                displayedPhotos: $displayedPhotos,
                isSaving: $isSaving,
                onPhotoAdded: onPhotoAdded,
                onPhotoDeleted: onPhotoDeleted,
                sectionTitle: "Issue Photos",
                sectionIcon: "photo.stack",
                showUploadIndicators: true
            )
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }
}