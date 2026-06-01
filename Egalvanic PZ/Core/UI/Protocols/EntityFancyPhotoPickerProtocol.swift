//
//  EntityFancyPhotoPickerProtocol.swift
//  SwiftDataTutorial
//
//  Protocol for entities that support multiple photo types in a single picker
//

import Foundation
import SwiftData

// MARK: - Photo Type Configuration
struct PhotoTypeConfiguration {
    let type: String
    let displayName: String
    let icon: String
    let color: String // Color name like "blue", "green", etc.
    
    init(type: String, displayName: String, icon: String = "photo", color: String = "blue") {
        self.type = type
        self.displayName = displayName
        self.icon = icon
        self.color = color
    }
}

// MARK: - Entity Protocol for Fancy Photo Picker
protocol EntityWithMultiTypePhotos: EntityWithPhotos {
    var supportedPhotoTypes: [PhotoTypeConfiguration] { get }
    var defaultPhotoType: PhotoTypeConfiguration? { get }
    
    // Get photos by specific type
    func getPhotosByType(_ type: String) -> [Photo]
    
    // Validate if a photo type is supported
    func supportsPhotoType(_ type: String) -> Bool
}

// MARK: - Default Implementation
extension EntityWithMultiTypePhotos {
    var defaultPhotoType: PhotoTypeConfiguration? {
        supportedPhotoTypes.first
    }
    
    func supportsPhotoType(_ type: String) -> Bool {
        supportedPhotoTypes.contains { $0.type == type }
    }
    
    func getPhotosByType(_ type: String) -> [Photo] {
        return getAssociatedPhotos(ofType: type).filter { $0.type == type }
    }
}

// Note: Model conformances have been moved to Models/Extensions/ for better separation of concerns
// - UserTask+PhotoPicker.swift (EntityWithMultiTypePhotos)
// - NodeV2+PhotoPicker.swift (EntityWithMultiTypePhotos)
// - Issue+PhotoPicker.swift (EntityWithMultiTypePhotos)
