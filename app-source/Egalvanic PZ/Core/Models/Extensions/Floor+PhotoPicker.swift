//
//  Floor+PhotoPicker.swift
//  Egalvanic PZ
//
//  Extensions for Floor to support photo pickers
//
import Foundation

// MARK: - EntityWithPhotos Conformance
extension Floor: EntityWithPhotos {
    var entityId: UUID {
        return self.id
    }

    var entityType: PhotoEntityType {
        return .floor
    }

    func associatePhoto(_ photo: Photo) {
        photo.floor = self
        photo.building = nil
        photo.room = nil
        photo.node = nil
        photo.issue = nil
        photo.userTask = nil
        if !photos.contains(where: { $0.id == photo.id }) {
            photos.append(photo)
        }
    }

    func disassociatePhoto(_ photo: Photo) {
        if photo.floor?.id == self.id {
            photo.floor = nil
        }
    }

    func getAssociatedPhotos(ofType type: String? = nil) -> [Photo] {
        if let type = type {
            return photos.filter { !$0.is_deleted && $0.type == type }
        }
        return photos.filter { !$0.is_deleted }
    }
}
