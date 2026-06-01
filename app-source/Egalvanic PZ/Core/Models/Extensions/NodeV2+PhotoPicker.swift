//
//  NodeV2+PhotoPicker.swift
//  SwiftDataTutorial
//
//  Extensions for NodeV2 to support photo pickers
//
import Foundation

// MARK: - EntityWithPhotos Conformance
extension NodeV2: EntityWithPhotos {
    var entityId: UUID {
        return self.id
    }
    
    var entityType: PhotoEntityType {
        return .node
    }
    
    func associatePhoto(_ photo: Photo) {
        photo.node = self
        photo.issue = nil
        photo.userTask = nil
        if !photos.contains(where: { $0.id == photo.id }) {
            photos.append(photo)
        }
    }
    
    func disassociatePhoto(_ photo: Photo) {
        if photo.node?.id == self.id {
            photo.node = nil
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
extension NodeV2: EntityWithMultiTypePhotos {
    var supportedPhotoTypes: [PhotoTypeConfiguration] {
        return [
            PhotoTypeConfiguration(
                type: "node_profile",
                displayName: AppStrings.Photos.typeProfile,
                icon: "photo",
                color: "blue"
            ),
            PhotoTypeConfiguration(
                type: "node_nameplate",
                displayName: AppStrings.Photos.typeNameplate,
                icon: "tag.fill",
                color: "orange"
            ),
            PhotoTypeConfiguration(
                type: "node_panel_schedule",
                displayName: AppStrings.Photos.typePanelSchedule,
                icon: "list.clipboard.fill",
                color: "green"
            ),
            PhotoTypeConfiguration(
                type: "node_arc_flash_sticker",
                displayName: AppStrings.Photos.typeArcFlashSticker,
                icon: "bolt.shield.fill",
                color: "yellow"
            )
        ]
    }
}
