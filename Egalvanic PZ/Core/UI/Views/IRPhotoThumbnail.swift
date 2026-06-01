//
//  IRPhotoThumbnail.swift
//  Egalvanic PZ
//
//  Reusable cached thumbnail for IR photos (VIS or IR variant).
//  Fetches via presigned URL and caches by string key for offline support.
//

import SwiftUI
import UIKit

struct IRPhotoThumbnail: View {
    let photoKey: String
    let cacheKey: String
    let sessionId: UUID?
    let label: String
    let labelColor: Color
    let isOnline: Bool
    var size: CGFloat = 60
    /// Pre-fetched presigned URL from batch request. When provided, skips the individual API call.
    var presignedURL: String? = nil

    @State private var loadedImage: UIImage?
    @State private var isLoading = false
    @State private var loadFailed = false
    @State private var hasAttemptedLoad = false
    @State private var isNotFound = false
    /// Tracks whether the last failed load attempt ran without a presigned URL.
    /// Used to allow exactly one retry when a presigned URL later becomes available.
    @State private var failedWithoutPresignedURL = false

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(UIColor.systemGray6))
                .frame(width: size, height: size)

            if let image = loadedImage {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: size, height: size)
                    .clipped()
                    .cornerRadius(8)
            } else if isLoading {
                ProgressView()
                    .scaleEffect(0.5)
            } else if isNotFound {
                Image(systemName: label == "VIS" ? "photo" : "camera.filters")
                    .foregroundColor(.gray)
                    .font(.title3)
            } else if loadFailed {
                Image(systemName: label == "VIS" ? "photo" : "camera.filters")
                    .foregroundColor(.gray)
                    .font(.title3)
                    .overlay(alignment: .topTrailing) {
                        Button(action: {
                            hasAttemptedLoad = false
                            loadFailed = false
                            failedWithoutPresignedURL = false
                            Task { await loadImage() }
                        }) {
                            Image(systemName: "arrow.clockwise")
                                .font(.caption2)
                                .foregroundColor(.white)
                                .padding(3)
                                .background(Color.gray.opacity(0.8))
                                .clipShape(Circle())
                        }
                        .padding(2)
                    }
            } else if sessionId == nil {
                Image(systemName: "camera.badge.exclamationmark")
                    .foregroundColor(.gray)
                    .font(.title3)
            } else if !isOnline {
                VStack(spacing: 4) {
                    Image(systemName: "wifi.slash")
                        .foregroundColor(.gray)
                        .font(.title3)
                    Text(AppStrings.CommonExtra.offline)
                        .font(.system(size: 9))
                        .foregroundColor(.gray)
                }
            } else {
                ProgressView()
                    .scaleEffect(0.5)
            }

            // Label overlay
            VStack {
                Spacer()
                HStack {
                    Text(label)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                        .background(labelColor.opacity(0.8))
                        .cornerRadius(4)
                    Spacer()
                }
                .padding(4)
            }
        }
        .frame(width: size, height: size)
        .task { await loadImage() }
        .onChange(of: presignedURL) { _, newURL in
            guard newURL != nil, !isLoading else { return }

            if loadedImage == nil, failedWithoutPresignedURL {
                // Presigned URL arrived after a failed load — retry
                AppLogger.log(.info, "IRPhotoThumbnail[\(cacheKey)]: presignedURL arrived after failed load — retrying, photoKey=\(photoKey)", category: .photo)
                failedWithoutPresignedURL = false
                hasAttemptedLoad = false
                isNotFound = false
                loadFailed = false
                Task { await loadImage() }
            } else if loadedImage != nil, PhotoImageCache.shared.getImage(forKey: cacheKey) == nil {
                // Image cache was evicted (e.g., upload replaced S3 content) — reset and reload
                AppLogger.log(.info, "IRPhotoThumbnail[\(cacheKey)]: cache evicted, presignedURL changed — reloading", category: .photo)
                loadedImage = nil
                hasAttemptedLoad = false
                isNotFound = false
                loadFailed = false
                failedWithoutPresignedURL = false
                Task { await loadImage() }
            }
        }
        .onChange(of: photoKey) { oldKey, newKey in
            // Photo key changed (e.g., server updated the keys after upload) — full reset and reload
            if oldKey != newKey {
                AppLogger.log(.info, "IRPhotoThumbnail[\(cacheKey)]: photoKey changed '\(oldKey)' → '\(newKey)' — resetting for reload", category: .photo)
                loadedImage = nil
                hasAttemptedLoad = false
                isNotFound = false
                loadFailed = false
                failedWithoutPresignedURL = false
                Task { await loadImage() }
            }
        }
    }

    private func loadImage() async {
        guard !hasAttemptedLoad, !isLoading, loadedImage == nil else { return }

        // Check cache first
        if let cached = PhotoImageCache.shared.getImage(forKey: cacheKey) {
            await MainActor.run { loadedImage = cached }
            return
        }

        guard isOnline else { return }

        // Need either a pre-fetched URL or a sessionId to fetch one
        guard presignedURL != nil || sessionId != nil else { return }

        let hadPresignedURL = presignedURL != nil

        await MainActor.run {
            isLoading = true
            loadFailed = false
            hasAttemptedLoad = true
        }

        do {
            // Use pre-fetched URL if available, otherwise fall back to individual fetch
            let downloadURLString: String
            if let prefetched = presignedURL {
                downloadURLString = prefetched
            } else {
                let response = try await S3PresignedURLService.shared.getIRPhotoPresignedURL(
                    sessionId: sessionId!,
                    photoKey: photoKey
                )
                downloadURLString = response.url
            }

            guard let url = URL(string: downloadURLString) else {
                throw URLError(.badURL)
            }

            let (data, urlResponse) = try await ImageCacheManager.shared.urlSession.data(from: url)

            // Check for S3 error responses (e.g. 404 NoSuchKey)
            if let httpResponse = urlResponse as? HTTPURLResponse, httpResponse.statusCode != 200 {
                throw NSError(
                    domain: "IRPhotoThumbnail",
                    code: httpResponse.statusCode,
                    userInfo: [NSLocalizedDescriptionKey: "S3 returned \(httpResponse.statusCode)"]
                )
            }

            guard let image = UIImage(data: data) else {
                throw URLError(.cannotDecodeContentData)
            }

            PhotoImageCache.shared.setImage(image, forKey: cacheKey)

            await MainActor.run {
                loadedImage = image
                isLoading = false
            }
        } catch {
            let is404 = (error as NSError).code == 404
            AppLogger.log(.notice, "IRPhotoThumbnail: Failed to load \(cacheKey): \(error)", category: .photo)
            await MainActor.run {
                isLoading = false
                if is404 {
                    isNotFound = true
                } else {
                    loadFailed = true
                }

                if !hadPresignedURL {
                    // Load ran without a presigned URL and failed.
                    // If a presigned URL arrived during the load, retry immediately.
                    // Otherwise, flag so .onChange(of: presignedURL) can retry later.
                    if presignedURL != nil {
                        AppLogger.log(.info, "IRPhotoThumbnail[\(cacheKey)]: presignedURL arrived during load — retrying immediately", category: .photo)
                        hasAttemptedLoad = false
                        isNotFound = false
                        loadFailed = false
                        Task { await loadImage() }
                    } else {
                        failedWithoutPresignedURL = true
                    }
                }
            }
        }
    }
}
