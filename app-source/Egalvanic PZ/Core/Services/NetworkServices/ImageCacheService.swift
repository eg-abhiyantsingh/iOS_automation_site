//
//  ImageCacheService.swift
//  SwiftDataTutorial
//
//  Created by Kush Jadia on 17/11/25.

//  Service for managing image caching across the app
//  Provides URL-based caching for remote images with memory and disk storage
//  Also provides photo ID-based caching for presigned URL images
//

import SwiftUI
import Foundation
import UIKit

// MARK: - Photo Image Cache
/// A cache for storing downloaded images by photo ID
/// This is needed because presigned URLs are different each time, so URLCache won't work
class PhotoImageCache {
    static let shared = PhotoImageCache()

    private let memoryCache = NSCache<NSString, UIImage>()
    private let fileManager = FileManager.default
    private let cacheDirectory: URL

    private init() {
        // Set up memory cache limits
        memoryCache.countLimit = 200 // Max 200 images in memory
        memoryCache.totalCostLimit = 100 * 1024 * 1024 // 100 MB

        // Set up disk cache directory
        let cachesDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
        cacheDirectory = cachesDirectory.appendingPathComponent("PhotoImageCache")

        // Create cache directory if needed
        try? fileManager.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }

    /// Get cached image for a photo ID
    func getImage(for photoId: UUID) -> UIImage? {
        let key = photoId.uuidString as NSString

        // Check memory cache first
        if let image = memoryCache.object(forKey: key) {
            return image
        }

        // Check disk cache
        let filePath = cacheDirectory.appendingPathComponent("\(photoId.uuidString).jpg")
        if let data = try? Data(contentsOf: filePath),
           let image = UIImage(data: data) {
            // Store in memory cache for faster access next time
            memoryCache.setObject(image, forKey: key, cost: data.count)
            return image
        }

        return nil
    }

    /// Cache an image for a photo ID
    func setImage(_ image: UIImage, for photoId: UUID) {
        let key = photoId.uuidString as NSString

        // Store in memory cache
        if let data = image.jpegData(compressionQuality: 0.8) {
            memoryCache.setObject(image, forKey: key, cost: data.count)

            // Store on disk asynchronously
            let filePath = cacheDirectory.appendingPathComponent("\(photoId.uuidString).jpg")
            Task.detached(priority: .background) {
                try? data.write(to: filePath)
            }
        }
    }

    /// Check if an image is cached (in memory or disk)
    func hasImage(for photoId: UUID) -> Bool {
        let key = photoId.uuidString as NSString

        // Check memory cache
        if memoryCache.object(forKey: key) != nil {
            return true
        }

        // Check disk cache
        let filePath = cacheDirectory.appendingPathComponent("\(photoId.uuidString).jpg")
        return fileManager.fileExists(atPath: filePath.path)
    }

    /// Clear memory cache only
    func clearMemoryCache() {
        memoryCache.removeAllObjects()
    }

    /// Clear all caches (memory and disk)
    func clearAllCaches() {
        memoryCache.removeAllObjects()
        try? fileManager.removeItem(at: cacheDirectory)
        try? fileManager.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }

    // MARK: - String-based key methods (for IR photo VIS/IR variants)

    /// Get cached image for a string key (e.g., "{uuid}_ir", "{uuid}_vis")
    func getImage(forKey key: String) -> UIImage? {
        let nsKey = key as NSString

        // Check memory cache first
        if let image = memoryCache.object(forKey: nsKey) {
            return image
        }

        // Check disk cache
        let safeFilename = key.replacingOccurrences(of: "/", with: "_")
        let filePath = cacheDirectory.appendingPathComponent("\(safeFilename).jpg")
        if let data = try? Data(contentsOf: filePath),
           let image = UIImage(data: data) {
            memoryCache.setObject(image, forKey: nsKey, cost: data.count)
            return image
        }

        return nil
    }

    /// Cache an image for a string key
    func setImage(_ image: UIImage, forKey key: String) {
        let nsKey = key as NSString

        if let data = image.jpegData(compressionQuality: 0.8) {
            memoryCache.setObject(image, forKey: nsKey, cost: data.count)

            let safeFilename = key.replacingOccurrences(of: "/", with: "_")
            let filePath = cacheDirectory.appendingPathComponent("\(safeFilename).jpg")
            Task.detached(priority: .background) {
                try? data.write(to: filePath)
            }
        }
    }

    /// Remove a cached image for a string key (memory + disk)
    func removeImage(forKey key: String) {
        let nsKey = key as NSString
        memoryCache.removeObject(forKey: nsKey)

        let safeFilename = key.replacingOccurrences(of: "/", with: "_")
        let filePath = cacheDirectory.appendingPathComponent("\(safeFilename).jpg")
        try? fileManager.removeItem(at: filePath)
    }

    /// Check if an image is cached for a string key
    func hasImage(forKey key: String) -> Bool {
        let nsKey = key as NSString

        if memoryCache.object(forKey: nsKey) != nil {
            return true
        }

        let safeFilename = key.replacingOccurrences(of: "/", with: "_")
        let filePath = cacheDirectory.appendingPathComponent("\(safeFilename).jpg")
        return fileManager.fileExists(atPath: filePath.path)
    }

    /// Get disk cache size in bytes
    func getDiskCacheSize() -> Int {
        guard let contents = try? fileManager.contentsOfDirectory(at: cacheDirectory, includingPropertiesForKeys: [.fileSizeKey]) else {
            return 0
        }

        return contents.reduce(0) { total, url in
            let size = (try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0
            return total + size
        }
    }
}

// MARK: - Image Cache Manager
/// Singleton service to manage URL caching for images across the app
class ImageCacheManager {
    static let shared = ImageCacheManager()

    let urlSession: URLSession

    private init() {
        // Configure URLCache with generous memory and disk capacity
        let memoryCapacity = 50 * 1024 * 1024 // 50 MB memory cache
        let diskCapacity = 150 * 1024 * 1024 // 150 MB disk cache
        let cache = URLCache(memoryCapacity: memoryCapacity, diskCapacity: diskCapacity)

        // Configure URLSession with caching enabled
        let config = URLSessionConfiguration.default
        config.urlCache = cache
        config.requestCachePolicy = .returnCacheDataElseLoad // Use cache if available

        self.urlSession = URLSession(configuration: config)
    }

    /// Clear all cached images from memory
    func clearMemoryCache() {
        urlSession.configuration.urlCache?.removeAllCachedResponses()
    }

    /// Clear all cached images from disk
    func clearDiskCache() {
        urlSession.configuration.urlCache?.removeAllCachedResponses()
    }

    /// Get current cache statistics
    func getCacheStats() -> (memoryUsage: Int, diskUsage: Int) {
        guard let cache = urlSession.configuration.urlCache else {
            return (0, 0)
        }
        return (cache.currentMemoryUsage, cache.currentDiskUsage)
    }
}

// MARK: - Cached Async Image View
/// A SwiftUI view wrapper that uses URLSession with caching for efficient image loading
/// - Loads from cache if available (memory or disk)
/// - Falls back to network request if not cached
/// - Handles errors gracefully with failure placeholder fallback
struct CachedAsyncImage<Content: View, Placeholder: View, FailurePlaceholder: View>: View {
    let url: URL?
    let content: (Image) -> Content
    let placeholder: () -> Placeholder
    let failurePlaceholder: () -> FailurePlaceholder
    let retryButtonAlignment: Alignment

    @State private var loadedImage: UIImage?
    @State private var isLoading = false
    @State private var loadFailed = false

    var body: some View {
        Group {
            if let image = loadedImage {
                content(Image(uiImage: image))
            } else if loadFailed {
                // Show failure placeholder with retry button
                failurePlaceholder()
                    .overlay(alignment: retryButtonAlignment) {
                        Button(action: retryLoad) {
                            Image(systemName: "arrow.clockwise")
                                .font(.title3)
                                .foregroundColor(.white)
                                .background(Color.gray.opacity(0.8))
                                .clipShape(Circle())
                                .shadow(radius: 2)
                        }
                        .padding(retryButtonAlignment == .center ? 0 : 4)
                    }
            } else {
                // Show loading placeholder while loading
                placeholder()
                    .onAppear {
                        loadImage()
                    }
            }
        }
        .onChange(of: url) { oldURL, newURL in
            // Reset state and reload when URL changes
            if oldURL != newURL {
                loadedImage = nil
                isLoading = false
                loadFailed = false
                loadImage()
            }
        }
    }

    private func retryLoad() {
        loadFailed = false
        loadImage()
    }

    private func loadImage() {
        guard let url = url, !isLoading else { return }

        isLoading = true
        loadFailed = false

        Task {
            do {
                let (data, _) = try await ImageCacheManager.shared.urlSession.data(from: url)
                if let uiImage = UIImage(data: data) {
                    await MainActor.run {
                        loadedImage = uiImage
                        isLoading = false
                    }
                } else {
                    // Data received but not a valid image
                    await MainActor.run {
                        isLoading = false
                        loadFailed = true
                    }
                }
            } catch {
                // Network or other error
                await MainActor.run {
                    isLoading = false
                    loadFailed = true
                }
            }
        }
    }
}

// MARK: - Convenience initializer for backward compatibility
extension CachedAsyncImage where FailurePlaceholder == Placeholder {
    /// Convenience initializer that uses the same view for both loading and failure states
    init(
        url: URL?,
        @ViewBuilder content: @escaping (Image) -> Content,
        @ViewBuilder placeholder: @escaping () -> Placeholder
    ) {
        self.url = url
        self.content = content
        self.placeholder = placeholder
        self.failurePlaceholder = placeholder
        self.retryButtonAlignment = .topLeading
    }
}

// MARK: - Initializer with separate failure placeholder
extension CachedAsyncImage {
    /// Full initializer with separate loading and failure placeholders
    init(
        url: URL?,
        @ViewBuilder content: @escaping (Image) -> Content,
        @ViewBuilder placeholder: @escaping () -> Placeholder,
        @ViewBuilder onFailure: @escaping () -> FailurePlaceholder,
        retryButtonAlignment: Alignment = .topLeading
    ) {
        self.url = url
        self.content = content
        self.placeholder = placeholder
        self.failurePlaceholder = onFailure
        self.retryButtonAlignment = retryButtonAlignment
    }
}
