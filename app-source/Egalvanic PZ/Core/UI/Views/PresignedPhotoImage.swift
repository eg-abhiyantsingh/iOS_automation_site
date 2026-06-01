//
//  PresignedPhotoImage.swift
//  Egalvanic PZ
//
//  Reusable component for displaying photos with presigned URL support.
//  Handles local-first loading, photo ID-based caching, and presigned URL fetching.
//

import SwiftUI
import UIKit

/// A SwiftUI view that displays photos using presigned URLs for private S3 bucket access.
/// - Priority 1: Checks local file first for instant loading (works offline)
/// - Priority 2: Checks photo ID-based cache (works offline if previously downloaded)
/// - Priority 3: Fetches presigned URL and downloads image (caches for next time)
/// - Handles loading and error states with retry capability
struct PresignedPhotoImage<Content: View, Placeholder: View, FailurePlaceholder: View>: View {
    let photo: Photo
    let content: (Image) -> Content
    let placeholder: () -> Placeholder
    let failurePlaceholder: () -> FailurePlaceholder
    let retryButtonAlignment: Alignment

    @State private var cachedImage: UIImage?
    @State private var isLoading = false
    @State private var loadFailed = false

    var body: some View {
        Group {
            // Priority 1: Local file (instant, works offline)
            if photo.isLocallyAvailable,
               let localURL = photo.localFileURL,
               let uiImage = UIImage(contentsOfFile: localURL.path) {
                content(Image(uiImage: uiImage))
            }
            // Priority 2: Cached image loaded (from photo ID cache or just downloaded)
            else if let image = cachedImage {
                content(Image(uiImage: image))
            }
            // Priority 3: Loading state
            else if isLoading {
                placeholder()
            }
            // Priority 4: Failed to load or no URL available
            else if loadFailed || photo.url == nil {
                failurePlaceholder()
                    .overlay(alignment: retryButtonAlignment) {
                        if loadFailed {
                            Button(action: {
                                Task { await loadImage() }
                            }) {
                                Image(systemName: "arrow.clockwise")
                                    .font(.title3)
                                    .foregroundColor(.white)
                                    .padding(6)
                                    .background(Color.gray.opacity(0.8))
                                    .clipShape(Circle())
                                    .shadow(radius: 2)
                            }
                            .padding(retryButtonAlignment == .center ? 0 : 4)
                        }
                    }
            }
            // Priority 5: Initial state - trigger load
            else {
                placeholder()
                    .onAppear {
                        Task { await loadImage() }
                    }
            }
        }
        .task {
            await loadImage()
        }
        .onChange(of: photo.url) { _, _ in
            // Reset and reload when URL changes
            cachedImage = nil
            loadFailed = false
            Task { await loadImage() }
        }
    }

    private func loadImage() async {
        // Skip if local file exists - already handled by Priority 1
        guard !photo.isLocallyAvailable else { return }

        // Skip if no remote URL
        guard photo.url != nil else { return }

        // Skip if already loading or already have image
        guard !isLoading, cachedImage == nil else { return }

        // Check photo ID-based cache first (instant, works offline)
        if let cached = PhotoImageCache.shared.getImage(for: photo.id) {
            await MainActor.run {
                cachedImage = cached
            }
            return
        }

        // Need to fetch from network
        await MainActor.run {
            isLoading = true
            loadFailed = false
        }

        do {
            // Get presigned URL
            let presignedURL = try await photo.getPresignedDisplayURL()

            // Download image
            let (data, _) = try await ImageCacheManager.shared.urlSession.data(from: presignedURL)

            guard let image = UIImage(data: data) else {
                throw NSError(domain: "PresignedPhotoImage", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid image data"])
            }

            // Cache the image by photo ID for next time
            PhotoImageCache.shared.setImage(image, for: photo.id)

            await MainActor.run {
                cachedImage = image
                isLoading = false
            }
        } catch {
            AppLogger.log(.notice, "PresignedPhotoImage: Failed to load image for photo \(photo.id): \(error)", category: .photo)
            await MainActor.run {
                isLoading = false
                loadFailed = true
            }
        }
    }
}

// MARK: - Convenience Initializers

extension PresignedPhotoImage where FailurePlaceholder == Placeholder {
    /// Convenience initializer that uses the same view for both loading and failure states
    init(
        photo: Photo,
        @ViewBuilder content: @escaping (Image) -> Content,
        @ViewBuilder placeholder: @escaping () -> Placeholder
    ) {
        self.photo = photo
        self.content = content
        self.placeholder = placeholder
        self.failurePlaceholder = placeholder
        self.retryButtonAlignment = .topLeading
    }
}

extension PresignedPhotoImage {
    /// Full initializer with separate loading and failure placeholders
    init(
        photo: Photo,
        @ViewBuilder content: @escaping (Image) -> Content,
        @ViewBuilder placeholder: @escaping () -> Placeholder,
        @ViewBuilder onFailure: @escaping () -> FailurePlaceholder,
        retryButtonAlignment: Alignment = .topLeading
    ) {
        self.photo = photo
        self.content = content
        self.placeholder = placeholder
        self.failurePlaceholder = onFailure
        self.retryButtonAlignment = retryButtonAlignment
    }
}
