//
//  Photo.swift
//  SwiftDataTutorial
//
import SwiftUI
import SwiftData

@Model
final class Photo {
    var id: UUID
    var node: NodeV2?
    var userTask: UserTask?
    var issue: Issue?
    var building: Building?
    var floor: Floor?
    var room: Room?
    var url: String?
    var type: String
    var sld: SLDV2?
    var local_filepath: String?
    var filename: String?
    var upload_needed: Bool
    var is_deleted: Bool
    var caption: String?

    init(id: UUID, node: NodeV2? = nil, userTask: UserTask?, issue: Issue?, building: Building? = nil, floor: Floor? = nil, room: Room? = nil, url: String?, type: String, sld: SLDV2? = nil, upload_needed: Bool = false, local_filepath: String?, filename: String?, is_deleted: Bool = false, caption: String? = nil) {
        self.id = id
        self.node = node
        self.userTask = userTask
        self.issue = issue
        self.building = building
        self.floor = floor
        self.room = room
        self.url = url
        self.type = type
        self.sld = sld
        self.local_filepath = local_filepath
        self.filename = filename
        self.upload_needed = upload_needed
        self.is_deleted = is_deleted
        self.caption = caption
    }
}

extension Photo {
    var localFileURL: URL? {
        guard let filename = self.filename else { return nil }
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        return documentsURL.appendingPathComponent("photos").appendingPathComponent(filename)
    }

    var displayURL: URL? {
        // First try local file
        if let localURL = self.localFileURL {
            return localURL
        }

        // Then try remote URL - handle the S3 URL format
        if let urlString = self.url,
           !urlString.isEmpty,
           let url = URL(string: urlString) {
            return url
        }

        return nil
    }

    var isLocallyAvailable: Bool {
        if let localURL = self.localFileURL {
            return FileManager.default.fileExists(atPath: localURL.path)
        }
        return false
    }

    /// Get presigned URL for downloading/viewing this photo
    /// Returns the presigned URL with query parameters for authorized access
    /// Handles both full URLs and plain keys/filenames in photo.url
    func getPresignedDisplayURL() async throws -> URL {
        guard let urlString = self.url else {
            throw NSError(domain: "Photo", code: 1, userInfo: [NSLocalizedDescriptionKey: "Photo has no URL"])
        }

        // Try to extract key from URL, or use urlString directly if it's just a key/filename
        let key: String
       // if let photoURL = URL(string: urlString), photoURL.scheme != nil {
            // It's a full URL - extract the path component
        //    key = photoURL.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
      //  } else {
            // It's just a key/filename - use directly
            key = urlString
       // }

        guard !key.isEmpty else {
            throw NSError(domain: "Photo", code: 3, userInfo: [NSLocalizedDescriptionKey: "Empty key extracted from URL"])
        }

        let presignedResponse = try await S3PresignedURLService.shared.getPresignedDownloadURL(
            key: key,  // Pass full key including photo_ prefix
            photoType: self.type
        )

        guard let presignedURL = URL(string: presignedResponse.url) else {
            throw NSError(domain: "Photo", code: 4, userInfo: [NSLocalizedDescriptionKey: "Invalid presigned URL"])
        }

        return presignedURL
    }

    /// Get the appropriate URL for display (local first, then presigned remote)
    /// This method handles the async presigned URL fetch
    func getDisplayURLAsync() async -> URL? {
        // First try local file
        if let localURL = self.localFileURL, FileManager.default.fileExists(atPath: localURL.path) {
            return localURL
        }

        // Try to get presigned URL for remote access
        do {
            return try await getPresignedDisplayURL()
        } catch {
            AppLogger.log(.notice, "Failed to get presigned display URL: \(error)", category: .photo)
            // Fallback to direct URL (may not work without presigning)
            return displayURL
        }
    }
}
