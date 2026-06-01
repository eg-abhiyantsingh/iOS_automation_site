//
//  Room+PhotoPicker.swift
//  Egalvanic PZ
//
//  Extensions for Room to support photo pickers
//
import Foundation

// MARK: - EntityWithPhotos Conformance
extension Room: EntityWithPhotos {
    var entityId: UUID {
        return self.id
    }

    var entityType: PhotoEntityType {
        return .room
    }

    func associatePhoto(_ photo: Photo) {
        photo.room = self
        photo.building = nil
        photo.floor = nil
        photo.node = nil
        photo.issue = nil
        photo.userTask = nil
        if !photos.contains(where: { $0.id == photo.id }) {
            photos.append(photo)
        }
    }

    func disassociatePhoto(_ photo: Photo) {
        if photo.room?.id == self.id {
            photo.room = nil
        }
    }

    func getAssociatedPhotos(ofType type: String? = nil) -> [Photo] {
        if let type = type {
            return photos.filter { !$0.is_deleted && $0.type == type }
        }
        return photos.filter { !$0.is_deleted }
    }
}
