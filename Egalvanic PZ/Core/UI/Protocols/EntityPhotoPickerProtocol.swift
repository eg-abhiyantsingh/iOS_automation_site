//
//  EntityPhotoPickerProtocol.swift
//  SwiftDataTutorial
//
//  Protocol for standardizing photo management across Node, Issue, and UserTask entities
//

import Foundation
import SwiftData

// MARK: - Entity Protocol
protocol EntityWithPhotos {
    var entityId: UUID { get }
    var entityType: PhotoEntityType { get }
    
    // Method to associate a photo with this entity
    func associatePhoto(_ photo: Photo)
    
    // Method to disassociate a photo from this entity
    func disassociatePhoto(_ photo: Photo)
    
    // Get photos associated with this entity
    func getAssociatedPhotos(ofType type: String?) -> [Photo]
}

// MARK: - Photo Entity Type
enum PhotoEntityType {
    case node
    case issue
    case userTask
    case building
    case floor
    case room

    var defaultPhotoType: String {
        switch self {
        case .node:
            return "node_profile"
        case .issue:
            return "issue"
        case .userTask:
            return "task_photo"
        case .building:
            return "building"
        case .floor:
            return "floor"
        case .room:
            return "room"
        }
    }
}

// Note: Model conformances have been moved to Models/Extensions/ for better separation of concerns
// - NodeV2+PhotoPicker.swift
// - Issue+PhotoPicker.swift
// - UserTask+PhotoPicker.swift
