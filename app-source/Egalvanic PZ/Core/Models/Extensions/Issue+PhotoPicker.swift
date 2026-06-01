//
//  Issue+PhotoPicker.swift
//  SwiftDataTutorial
//
//  Extensions for Issue to support photo pickers
//
import Foundation

// MARK: - EntityWithPhotos Conformance
extension Issue: EntityWithPhotos {
    var entityId: UUID {
        return self.id
    }
    
    var entityType: PhotoEntityType {
        return .issue
    }
    
    func associatePhoto(_ photo: Photo) {
        photo.issue = self
        photo.node = nil
        photo.userTask = nil
        if !photos.contains(where: { $0.id == photo.id }) {
            photos.append(photo)
        }
    }
    
    func disassociatePhoto(_ photo: Photo) {
        if photo.issue?.id == self.id {
            photo.issue = nil
        }
    }
    
    func getAssociatedPhotos(ofType type: String? = nil) -> [Photo] {
        if let type = type {
            return photos.filter { !$0.is_deleted && $0.type == type }
        }
        return photos.filter { !$0.is_deleted }
    }
}

// MARK: - EntityWithMultiTypePhotos Conformance
extension Issue: EntityWithMultiTypePhotos {
    var supportedPhotoTypes: [PhotoTypeConfiguration] {
        return [
            PhotoTypeConfiguration(
                type: "issue",
                displayName: AppStrings.Photos.typeIssue,
                icon: "exclamationmark.triangle",
                color: "red"
            ),
            PhotoTypeConfiguration(
                type: "issue_context",
                displayName: AppStrings.Photos.typeContext,
                icon: "location.circle",
                color: "blue"
            ),
            PhotoTypeConfiguration(
                type: "issue_resolution",
                displayName: AppStrings.Photos.typeResolution,
                icon: "checkmark.circle",
                color: "green"
            )
        ]
    }
}
