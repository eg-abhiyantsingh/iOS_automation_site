//
//  NodePhotoPickerSection.swift
//  SwiftDataTutorial
//
//  Configuration wrapper for Node photo picker
//
import SwiftUI
import SwiftData

struct NodePhotoPickerSection: View {
    let node: NodeV2
    @Binding var displayedPhotos: [Photo]
    @Binding var isSaving: Bool

    // Photo management callbacks
    var onPhotoAdded: ((Photo) -> Void)?
    var onPhotoDeleted: ((Photo) -> Void)?
    var onSetDefaultPhoto: ((UUID) -> Void)?

    // Draft state for immediate UI feedback
    var draftDefaultPhotoId: UUID?

    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState

    var body: some View {
        VStack(spacing: 16) {
            EntityFancyPhotoPicker(
                entity: node,
                displayedPhotos: $displayedPhotos,
                isSaving: $isSaving,
                onPhotoAdded: onPhotoAdded,
                onPhotoDeleted: onPhotoDeleted,
                sectionTitle: AppStrings.Assets.assetPhotos,
                sectionIcon: "photo.stack",
                showUploadIndicators: true,
                showPhotoTypeSelector: true,
                onSetDefaultPhoto: onSetDefaultPhoto,
                draftDefaultPhotoId: draftDefaultPhotoId
            )
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }
}
