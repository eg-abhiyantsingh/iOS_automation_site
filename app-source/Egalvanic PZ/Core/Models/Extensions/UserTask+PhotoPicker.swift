//
//  UserTask+PhotoPicker.swift
//  SwiftDataTutorial
//
//  Extensions for UserTask to support photo pickers
//
import Foundation

// MARK: - EntityWithPhotos Conformance
extension UserTask: EntityWithPhotos {
    var entityId: UUID {
        return self.id
    }
    
    var entityType: PhotoEntityType {
        return .userTask
    }
    
    func associatePhoto(_ photo: Photo) {
        photo.userTask = self
        photo.node = nil
        photo.issue = nil
        if !photos.contains(where: { $0.id == photo.id }) {
            photos.append(photo)
        }
    }
    
    func disassociatePhoto(_ photo: Photo) {
        if photo.userTask?.id == self.id {
            photo.userTask = nil
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
extension UserTask: EntityWithMultiTypePhotos {
    var supportedPhotoTypes: [PhotoTypeConfiguration] {
        // Common task photo types
        return [
            PhotoTypeConfiguration(
                type: "task_general",
                displayName: AppStrings.Photos.typeGeneral,
                icon: "photo",
                color: "blue"
            ),
            PhotoTypeConfiguration(
                type: "task_before",
                displayName: AppStrings.Photos.typeBefore,
                icon: "clock.badge.questionmark",
                color: "orange"
            ),
            PhotoTypeConfiguration(
                type: "task_after",
                displayName: AppStrings.Photos.typeAfter,
                icon: "clock.badge.checkmark",
                color: "green"
            )
        ]
    }
}