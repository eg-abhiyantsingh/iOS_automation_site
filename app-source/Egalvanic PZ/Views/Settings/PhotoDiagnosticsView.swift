//
//  PhotoDiagnosticsView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 7/21/25.
//

import SwiftUI
import SwiftData
import Photos

// MARK: - Photo Diagnostics View
struct PhotoDiagnosticsView: View {
    @Environment(\.modelContext) private var modelContext
    @State private var photoFiles: [PhotoFileInfo] = []
    @State private var totalSize: Int64 = 0
    @State private var selectedPhotos: Set<String> = []
    @State private var isEditMode = false
    @State private var showingDeleteConfirmation = false
    @State private var showingSaveSuccess = false
    @State private var saveSuccessMessage = ""
    
    // Fetch all photos from SwiftData
    @Query private var allPhotos: [Photo]
    
    struct PhotoFileInfo: Identifiable {
        let id = UUID()
        let filename: String
        let size: Int64
        let url: URL
        let creationDate: Date?
        var photo: Photo?
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Main content
            List {
                Section {
                    HStack {
                        Text(AppStrings.Diagnostics.totalFiles)
                        Spacer()
                        Text("\(photoFiles.count)")
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        Text(AppStrings.Diagnostics.totalSize)
                        Spacer()
                        Text(ByteCountFormatter.string(fromByteCount: totalSize, countStyle: .file))
                            .foregroundColor(.secondary)
                    }
                } header: {
                    Text(AppStrings.Diagnostics.photoStorage)
                }

                Section {
                    if photoFiles.isEmpty {
                        Text(AppStrings.Diagnostics.noPhotosFound)
                            .foregroundColor(.secondary)
                    } else {
                        ForEach(photoFiles) { fileInfo in
                            PhotoFileRow(
                                fileInfo: fileInfo,
                                isSelected: selectedPhotos.contains(fileInfo.filename),
                                isEditMode: isEditMode,
                                onToggleSelection: {
                                    if selectedPhotos.contains(fileInfo.filename) {
                                        selectedPhotos.remove(fileInfo.filename)
                                    } else {
                                        selectedPhotos.insert(fileInfo.filename)
                                    }
                                }
                            )
                        }
                    }
                } header: {
                    Text(AppStrings.Diagnostics.files)
                }
            }
            
            // Bottom toolbar replacement
            if isEditMode && !selectedPhotos.isEmpty {
                VStack {
                    Divider()
                    HStack {
                        Button(role: .destructive) {
                            showingDeleteConfirmation = true
                        } label: {
                            HStack {
                                Image(systemName: "trash")
                                Text(AppStrings.Diagnostics.deleteCount(selectedPhotos.count))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(Color.red)
                            .cornerRadius(8)
                        }
                        
                        Spacer()
                        
                        Button {
                            saveSelectedToGallery()
                        } label: {
                            HStack {
                                Image(systemName: "square.and.arrow.down")
                                Text(AppStrings.Diagnostics.saveToGallery)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(Color.accentColor)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                        }
                    }
                    .padding()
                    .background(Color(UIColor.systemGroupedBackground))
                }
            }
        }
        .navigationTitle(AppStrings.Diagnostics.photoDiagnosticsTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarVisibility(.hidden, for: .tabBar)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if !photoFiles.isEmpty {
                    Button(isEditMode ? AppStrings.Common.done : AppStrings.Diagnostics.selectLabel) {
                        withAnimation {
                            isEditMode.toggle()
                            if !isEditMode {
                                selectedPhotos.removeAll()
                            }
                        }
                    }
                }
            }
        }
        .onAppear {
            loadPhotoFiles()
        }
        .alert(AppStrings.Diagnostics.deletePhotosQuestion, isPresented: $showingDeleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Common.delete, role: .destructive) {
                deleteSelectedPhotos()
            }
        } message: {
            Text("This will permanently delete \(selectedPhotos.count) photo(s) from local storage.")
        }
        .alert(AppStrings.Common.success, isPresented: $showingSaveSuccess) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(saveSuccessMessage)
        }
    }
    
    private func loadPhotoFiles() {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let photosURL = documentsURL.appendingPathComponent("photos")
        
        do {
            let files = try FileManager.default.contentsOfDirectory(at: photosURL, includingPropertiesForKeys: [.fileSizeKey, .creationDateKey])
            var fileInfos: [PhotoFileInfo] = []

            for fileURL in files {
                let filename = fileURL.lastPathComponent
                let resourceValues = try? fileURL.resourceValues(forKeys: [.fileSizeKey, .creationDateKey])
                let size = resourceValues?.fileSize ?? 0
                let creationDate = resourceValues?.creationDate

                // Find associated Photo object
                let associatedPhoto = allPhotos.first { $0.filename == filename }

                fileInfos.append(PhotoFileInfo(
                    filename: filename,
                    size: Int64(size),
                    url: fileURL,
                    creationDate: creationDate,
                    photo: associatedPhoto
                ))
            }

            photoFiles = fileInfos.sorted { ($0.creationDate ?? .distantPast) > ($1.creationDate ?? .distantPast) }
            totalSize = fileInfos.reduce(0) { $0 + $1.size }
        } catch {
            AppLogger.log(.error, "Failed to list photos: \(error)", category: .photo)
            photoFiles = []
            totalSize = 0
        }
    }
    
    private func deleteSelectedPhotos() {
        for filename in selectedPhotos {
            if let fileInfo = photoFiles.first(where: { $0.filename == filename }) {
                do {
                    try FileManager.default.removeItem(at: fileInfo.url)
                    
                    // Also mark the photo as deleted in SwiftData if it exists
                    if let photo = fileInfo.photo {
                        photo.is_deleted = true
                        photo.local_filepath = nil
                    }
                } catch {
                    AppLogger.log(.error, "Failed to delete \(filename): \(error)", category: .photo)
                }
            }
        }
        
        try? modelContext.save()
        selectedPhotos.removeAll()
        isEditMode = false
        loadPhotoFiles()
    }
    
    private func saveSelectedToGallery() {
        var savedCount = 0
        let total = selectedPhotos.count
        
        for filename in selectedPhotos {
            if let fileInfo = photoFiles.first(where: { $0.filename == filename }) {
                if let image = UIImage(contentsOfFile: fileInfo.url.path) {
                    UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
                    savedCount += 1
                }
            }
        }
        
        saveSuccessMessage = "Saved \(savedCount) of \(total) photos to gallery"
        showingSaveSuccess = true
        selectedPhotos.removeAll()
        isEditMode = false
    }
}

// MARK: - Photo File Row
struct PhotoFileRow: View {
    let fileInfo: PhotoDiagnosticsView.PhotoFileInfo
    let isSelected: Bool
    let isEditMode: Bool
    let onToggleSelection: () -> Void
    
    @State private var thumbnailImage: UIImage?

    var body: some View {
        Group {
            if isEditMode {
                rowContent
                    .onTapGesture {
                        onToggleSelection()
                    }
            } else {
                NavigationLink {
                    PhotoDetailView(fileInfo: fileInfo)
                } label: {
                    rowContent
                }
            }
        }
        .onAppear {
            loadThumbnail()
        }
    }
    
    private var rowContent: some View {
        HStack(spacing: 12) {
            // Selection indicator
            if isEditMode {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .accentColor : .secondary)
            }

            // Thumbnail
            ZStack {
                Rectangle()
                    .fill(Color.secondary.opacity(0.2))
                    .frame(width: 50, height: 50)
                    .cornerRadius(8)

                if let thumbnail = thumbnailImage {
                    Image(uiImage: thumbnail)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 50, height: 50)
                        .cornerRadius(8)
                } else {
                    ProgressView()
                        .scaleEffect(0.6)
                }
            }

            // File info
            VStack(alignment: .leading, spacing: 4) {
                Text(fileInfo.filename)
                    .font(.system(.caption, design: .monospaced))
                    .lineLimit(1)

                HStack(spacing: 8) {
                    Text(ByteCountFormatter.string(fromByteCount: fileInfo.size, countStyle: .file))
                        .font(.caption2)
                        .foregroundColor(.secondary)

                    if let date = fileInfo.creationDate {
                        Text("·")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text(date, format: .dateTime.month(.abbreviated).day().hour().minute())
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }

                    if let photo = fileInfo.photo {
                        Label {
                            Text(photo.upload_needed ? AppStrings.Common.pending : AppStrings.Diagnostics.syncedLabel)
                                .font(.caption2)
                        } icon: {
                            Image(systemName: photo.upload_needed ? "arrow.clockwise" : "checkmark.circle")
                                .font(.caption2)
                        }
                        .foregroundColor(photo.upload_needed ? .orange : .green)
                    }
                }
            }

            Spacer()
        }
        .contentShape(Rectangle())
    }

    private func loadThumbnail() {
        DispatchQueue.global(qos: .background).async {
            if let image = UIImage(contentsOfFile: fileInfo.url.path) {
                let size = CGSize(width: 100, height: 100)
                UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
                image.draw(in: CGRect(origin: .zero, size: size))
                let thumbnail = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()
                
                DispatchQueue.main.async {
                    self.thumbnailImage = thumbnail
                }
            }
        }
    }
}

// MARK: - Photo Detail View
struct PhotoDetailView: View {
    let fileInfo: PhotoDiagnosticsView.PhotoFileInfo
    @State private var fullImage: UIImage?
    @State private var showingSaveSuccess = false
    @State private var isLoadingPresignedURL = false
    @State private var presignedURLError: String?
    @State private var showingNodeDetail = false
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Image preview
                if let image = fullImage {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(maxHeight: 400)
                        .cornerRadius(12)
                        .shadow(radius: 5)
                } else {
                    ProgressView()
                        .frame(height: 400)
                        .frame(maxWidth: .infinity)
                }

                // File information
                GroupBox("File Information") {
                    VStack(alignment: .leading, spacing: 8) {
                        DetailRow(label: "Filename", value: fileInfo.filename)
                        DetailRow(label: "Size", value: ByteCountFormatter.string(fromByteCount: fileInfo.size, countStyle: .file))
                        DetailRow(label: "Path", value: fileInfo.url.path)
                    }
                }

                // Photo metadata
                if let photo = fileInfo.photo {
                    GroupBox("Database Information") {
                        VStack(alignment: .leading, spacing: 8) {
                            DetailRow(label: "Photo ID", value: photo.id.uuidString)
                            DetailRow(label: "Upload Status", value: photo.upload_needed ? AppStrings.Diagnostics.pendingUpload : AppStrings.Diagnostics.syncedLabel)
                            if let url = photo.url {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(AppStrings.Diagnostics.remoteUrl)
                                        .fontWeight(.medium)
                                    Text(url)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                        .textSelection(.enabled)
                                }
                            }
                        }
                    }

                    // Associated node
                    if let node = photo.node {
                        GroupBox("Associated Node") {
                            VStack(alignment: .leading, spacing: 8) {
                                DetailRow(label: "Node ID", value: node.id.uuidString)
                                DetailRow(label: "Label", value: node.label)
                                DetailRow(label: "Type", value: node.type)

                                if node.sld != nil {
                                    Button {
                                        showingNodeDetail = true
                                    } label: {
                                        Label(AppStrings.Diagnostics.viewNode, systemImage: "cube.box")
                                    }
                                }
                            }
                        }
                    }
                }

                // Actions
                VStack(spacing: 12) {
                    Button {
                        saveToGallery()
                    } label: {
                        Label(AppStrings.Diagnostics.saveToGallery, systemImage: "square.and.arrow.down")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)

                    if let photo = fileInfo.photo, photo.url != nil {
                        Button {
                            openRemotePhoto(photo: photo)
                        } label: {
                            if isLoadingPresignedURL {
                                ProgressView()
                                    .frame(maxWidth: .infinity)
                            } else {
                                Label(AppStrings.Diagnostics.viewRemotePhoto, systemImage: "globe")
                                    .frame(maxWidth: .infinity)
                            }
                        }
                        .buttonStyle(.bordered)
                        .disabled(isLoadingPresignedURL)

                        if let error = presignedURLError {
                            Text(error)
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }
                }
                .padding(.top)
            }
            .padding()
        }
        .navigationTitle(AppStrings.Diagnostics.photoDetailsTitle)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            loadFullImage()
        }
        .alert(AppStrings.Common.success, isPresented: $showingSaveSuccess) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(AppStrings.Diagnostics.photoSavedToGalleryMsg)
        }
        .sheet(isPresented: $showingNodeDetail) {
            if let photo = fileInfo.photo, let node = photo.node, let sld = node.sld {
                NavigationView {
                    EditNodeDetailViewV3(node: node, sld: sld)
                }
                .navigationViewStyle(StackNavigationViewStyle())
            }
        }
    }
    
    private func loadFullImage() {
        DispatchQueue.global(qos: .userInitiated).async {
            let image = UIImage(contentsOfFile: fileInfo.url.path)
            DispatchQueue.main.async {
                self.fullImage = image
            }
        }
    }
    
    private func saveToGallery() {
        guard let image = fullImage else { return }
        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
        showingSaveSuccess = true
    }

    private func openRemotePhoto(photo: Photo) {
        isLoadingPresignedURL = true
        presignedURLError = nil

        Task {
            do {
                let presignedURL = try await photo.getPresignedDisplayURL()
                await MainActor.run {
                    isLoadingPresignedURL = false
                    UIApplication.shared.open(presignedURL)
                }
            } catch {
                await MainActor.run {
                    isLoadingPresignedURL = false
                    presignedURLError = "Failed to get URL: \(error.localizedDescription)"
                }
            }
        }
    }
}
