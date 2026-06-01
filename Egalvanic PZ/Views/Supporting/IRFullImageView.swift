//
//  IRFullImageView.swift
//  Egalvanic PZ
//
//  Full-screen viewer for IR photo pairs (VIS + IR side-by-side).
//  Supports swiping between photo pairs, zoom, and caching.
//

import SwiftUI
import UIKit

struct IRFullImageView: View {
    @Environment(\.dismiss) private var dismiss

    let irPhotos: [IRPhoto]
    let initialPhoto: IRPhoto
    let isOnline: Bool
    /// Pre-fetched presigned URLs from batch request. Keys are cache keys like "{uuid}_ir", "{uuid}_vis".
    var irPhotoPresignedURLs: [String: String] = [:]

    @State private var currentIndex: Int

    init(irPhotos: [IRPhoto], initialPhoto: IRPhoto, isOnline: Bool, irPhotoPresignedURLs: [String: String] = [:]) {
        self.irPhotos = irPhotos
        self.initialPhoto = initialPhoto
        self.isOnline = isOnline
        self.irPhotoPresignedURLs = irPhotoPresignedURLs
        _currentIndex = State(initialValue: irPhotos.firstIndex(where: { $0.id == initialPhoto.id }) ?? 0)
    }

    private var currentPhoto: IRPhoto {
        irPhotos[currentIndex]
    }

    private var isFlirInd: Bool {
        currentPhoto.ir_session?.photo_type == "FLIR-IND"
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 0) {
                // Close button
                HStack {
                    Spacer()
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.white)
                    }
                    .padding()
                }

                // Photo carousel
                TabView(selection: $currentIndex) {
                    ForEach(Array(irPhotos.enumerated()), id: \.element.id) { idx, photo in
                        IRPhotoPage(
                            irPhoto: photo,
                            isOnline: isOnline,
                            irPhotoPresignedURLs: irPhotoPresignedURLs
                        )
                        .tag(idx)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))

                // Photo key labels + counter
                VStack(spacing: 8) {
                    HStack(spacing: 16) {
                        if !currentPhoto.visual_photo_key.isEmpty {
                            Label(currentPhoto.visual_photo_key, systemImage: "photo")
                                .font(.caption)
                                .foregroundColor(.blue)
                        }
                        Label(currentPhoto.ir_photo_key, systemImage: "camera.filters")
                            .font(.caption)
                            .foregroundColor(.orange)
                    }

                    Text("\(currentIndex + 1) / \(irPhotos.count)")
                        .font(.caption2)
                        .foregroundColor(.gray)
                }
                .padding(.bottom, 24)
            }
        }
        .gesture(DragGesture().onEnded { value in
            if value.translation.width < -50 {
                withAnimation {
                    currentIndex = min(currentIndex + 1, irPhotos.count - 1)
                }
            } else if value.translation.width > 50 {
                withAnimation {
                    currentIndex = max(currentIndex - 1, 0)
                }
            }
        })
    }
}

// MARK: - Single page showing VIS + IR side-by-side

private struct IRPhotoPage: View {
    let irPhoto: IRPhoto
    let isOnline: Bool
    var irPhotoPresignedURLs: [String: String] = [:]

    private var sessionId: UUID? { irPhoto.ir_session?.id }
    private var isFlirInd: Bool { irPhoto.ir_session?.photo_type == "FLIR-IND" }

    var body: some View {
        ZoomableScrollView {
            if isFlirInd && irPhoto.visual_photo_key.isEmpty {
                // FLIR-IND before extraction has populated the visual key — IR only.
                IRFullImage(
                    photoKey: irPhoto.ir_photo_key,
                    cacheKey: "\(irPhoto.id.uuidString)_ir",
                    sessionId: sessionId,
                    isOnline: isOnline,
                    placeholderIcon: "camera.filters",
                    placeholderColor: .orange,
                    presignedURL: irPhotoPresignedURLs["\(irPhoto.id.uuidString)_ir"]
                )
            } else {
                // FLIR-SEP, or FLIR-IND after extraction — side-by-side visual + IR.
                HStack(spacing: 8) {
                    if !irPhoto.visual_photo_key.isEmpty {
                        IRFullImage(
                            photoKey: irPhoto.visual_photo_key,
                            cacheKey: "\(irPhoto.id.uuidString)_vis",
                            sessionId: sessionId,
                            isOnline: isOnline,
                            placeholderIcon: "photo",
                            placeholderColor: .blue,
                            presignedURL: irPhotoPresignedURLs["\(irPhoto.id.uuidString)_vis"]
                        )
                    }

                    IRFullImage(
                        photoKey: irPhoto.ir_photo_key,
                        cacheKey: "\(irPhoto.id.uuidString)_ir",
                        sessionId: sessionId,
                        isOnline: isOnline,
                        placeholderIcon: "camera.filters",
                        placeholderColor: .orange,
                        presignedURL: irPhotoPresignedURLs["\(irPhoto.id.uuidString)_ir"]
                    )
                }
                .padding(.horizontal, 8)
            }
        }
    }
}

// MARK: - Single full-size IR image with loading

private struct IRFullImage: View {
    let photoKey: String
    let cacheKey: String
    let sessionId: UUID?
    let isOnline: Bool
    let placeholderIcon: String
    let placeholderColor: Color
    /// Pre-fetched presigned URL from batch request. When provided, skips the individual API call.
    var presignedURL: String? = nil

    @State private var loadedImage: UIImage?
    @State private var isLoading = false
    @State private var loadFailed = false
    @State private var hasAttemptedLoad = false
    @State private var isNotFound = false

    var body: some View {
        Group {
            if let image = loadedImage {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if isLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .scaleEffect(1.5)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if isNotFound {
                VStack(spacing: 8) {
                    Image(systemName: placeholderIcon)
                        .font(.largeTitle)
                        .foregroundColor(placeholderColor)
                    Text(AppStrings.Common.noImage)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if loadFailed {
                VStack(spacing: 8) {
                    Image(systemName: placeholderIcon)
                        .font(.largeTitle)
                        .foregroundColor(placeholderColor)
                    Text(AppStrings.Common.noImage)
                        .font(.caption)
                        .foregroundColor(.gray)
                    Button(action: {
                        hasAttemptedLoad = false
                        loadFailed = false
                        Task { await loadImage() }
                    }) {
                        Label(AppStrings.Common.retry, systemImage: "arrow.clockwise")
                            .font(.caption)
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.gray.opacity(0.6))
                            .cornerRadius(8)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .scaleEffect(1.5)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task { await loadImage() }
    }

    private func loadImage() async {
        guard !hasAttemptedLoad, !isLoading, loadedImage == nil else { return }

        // Check cache
        if let cached = PhotoImageCache.shared.getImage(forKey: cacheKey) {
            await MainActor.run { loadedImage = cached }
            return
        }

        guard isOnline else {
            await MainActor.run { loadFailed = true }
            return
        }

        // Need either a pre-fetched URL or a sessionId to fetch one
        guard presignedURL != nil || sessionId != nil else {
            await MainActor.run { loadFailed = true }
            return
        }

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

            if let httpResponse = urlResponse as? HTTPURLResponse, httpResponse.statusCode != 200 {
                throw NSError(
                    domain: "IRFullImage",
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
            AppLogger.log(.notice, "IRFullImage: Failed to load \(cacheKey): \(error)", category: .photo)
            await MainActor.run {
                isLoading = false
                if is404 {
                    isNotFound = true
                } else {
                    loadFailed = true
                }
            }
        }
    }
}
