//
//  AttachmentsTab.swift
//  Egalvanic PZ
//
//  Displays and manages attachments for an IR session.
//

import SwiftUI
import SwiftData
import UniformTypeIdentifiers

struct AttachmentsTab: View {
    @Environment(\.modelContext) private var modelContext
    @ObservedObject private var networkState = NetworkState.shared

    let session: IRSession

    @State private var showingFilePicker = false
    @State private var showingConfigurationSheet = false
    @State private var selectedFileURL: URL?
    @State private var selectedFileName: String = ""
    @State private var selectedFileSize: Int64 = 0
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var showingDeleteConfirmation = false
    @State private var attachmentToDelete: Attachment?
    @State private var showingPublicConfirmation = false
    @State private var attachmentToMakePublic: Attachment?

    // Query attachments for this session
    @Query private var attachments: [Attachment]

    init(session: IRSession) {
        self.session = session

        // Filter attachments for this session
        let sessionId = session.id
        _attachments = Query(
            filter: #Predicate<Attachment> { attachment in
                attachment.sessionId == sessionId && !attachment.isDeleted
            },
            sort: [SortDescriptor(\.createdAt, order: .reverse)]
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header with Add button
            HStack {
                Text(AppStrings.Sessions.attachments)
                    .font(.title2)
                    .fontWeight(.bold)

                Spacer()

                Button {
                    showingFilePicker = true
                } label: {
                    Label(AppStrings.Sessions.addFile, systemImage: "plus.circle.fill")
                        .font(.system(size: 16, weight: .medium))
                }
            }
            .padding()

            Divider()

            if attachments.isEmpty {
                emptyStateView
            } else {
                attachmentListView
            }
        }
        .fileImporter(
            isPresented: $showingFilePicker,
            allowedContentTypes: [.item],
            allowsMultipleSelection: false
        ) { result in
            handleFileSelection(result)
        }
        .sheet(isPresented: $showingConfigurationSheet) {
            if let fileURL = selectedFileURL {
                AttachmentConfigurationView(
                    session: session,
                    fileURL: fileURL,
                    filename: selectedFileName,
                    fileSize: selectedFileSize
                )
            }
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
            Button(AppStrings.Common.ok, role: .cancel) { }
        } message: {
            Text(errorMessage)
        }
        .alert(AppStrings.Sessions.deleteAttachment, isPresented: $showingDeleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                attachmentToDelete = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let attachment = attachmentToDelete {
                    performDeleteAttachment(attachment)
                }
                attachmentToDelete = nil
            }
        } message: {
            Text(AppStrings.Sessions.deleteAttachmentConfirm)
        }
        .alert(AppStrings.Sessions.makePublicTitle, isPresented: $showingPublicConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                attachmentToMakePublic = nil
            }
            Button(AppStrings.Sessions.makePublicButton, role: .destructive) {
                if let attachment = attachmentToMakePublic {
                    updateVisibility(attachment, to: .public)
                }
                attachmentToMakePublic = nil
            }
        } message: {
            Text(AppStrings.Sessions.makePublicMessage)
        }
    }

    // MARK: - Empty State View

    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Spacer()

            Image(systemName: "doc.text.image")
                .font(.system(size: 60))
                .foregroundColor(.gray)

            Text(AppStrings.Sessions.noAttachments)
                .font(.title3)
                .fontWeight(.medium)
                .foregroundColor(.primary)

            Text(AppStrings.Sessions.addFilesDescription)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button {
                showingFilePicker = true
            } label: {
                Label(AppStrings.Sessions.browseFiles, systemImage: "folder")
                    .font(.system(size: 16, weight: .medium))
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }

            Spacer()
        }
    }

    // MARK: - Attachment List View

    private var attachmentListView: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(attachments) { attachment in
                    AttachmentRow(
                        attachment: attachment,
                        onDownload: { downloadAttachment(attachment) },
                        onDelete: {
                            attachmentToDelete = attachment
                            showingDeleteConfirmation = true
                        },
                        onVisibilityChange: { newVisibility in
                            if newVisibility == .public {
                                attachmentToMakePublic = attachment
                                showingPublicConfirmation = true
                            } else {
                                updateVisibility(attachment, to: newVisibility)
                            }
                        },
                        isOffline: networkState.mode == .offline
                    )
                    Divider()
                        .padding(.leading, 60)
                }
            }
        }
    }

    // MARK: - File Selection Handler

    private func handleFileSelection(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }

            // Start accessing security-scoped resource
            let didStartAccessing = url.startAccessingSecurityScopedResource()
            defer {
                if didStartAccessing {
                    url.stopAccessingSecurityScopedResource()
                }
            }

            // Get file info
            let filename = url.lastPathComponent
            let fileSize = getFileSize(for: url)

            // Validate file size
            guard fileSize <= Attachment.MAX_FILE_SIZE else {
                errorMessage = AppStrings.Sessions.fileTooLarge
                showError = true
                return
            }

            // Store file info and show configuration sheet
            selectedFileURL = url
            selectedFileName = filename
            selectedFileSize = fileSize
            showingConfigurationSheet = true

        case .failure(let error):
            errorMessage = "Failed to select file: \(error.localizedDescription)"
            showError = true
        }
    }

    // MARK: - Helper Methods

    private func getFileSize(for url: URL) -> Int64 {
        do {
            let attributes = try FileManager.default.attributesOfItem(atPath: url.path)
            return attributes[.size] as? Int64 ?? 0
        } catch {
            return 0
        }
    }

    private func performDeleteAttachment(_ attachment: Attachment) {
        // If the attachment hasn't been uploaded yet, just hard-delete locally
        if attachment.uploadNeeded {
            // Remove any pending sync queue items for this attachment
            NetworkState.shared.removeQueueItems(forAttachmentId: attachment.id)
            if let localPath = attachment.localFilePath {
                try? FileManager.default.removeItem(atPath: localPath)
            }
            modelContext.delete(attachment)
            try? modelContext.save()
            return
        }

        // Soft-delete locally (hides from UI immediately via @Query filter)
        attachment.isDeleted = true
        attachment.modifiedAt = Date()

        do {
            try modelContext.save()

            if NetworkState.shared.mode == .online {
                Task {
                    do {
                        _ = try await APIClient.shared.deleteAttachment(attachmentId: attachment.id)
                        await MainActor.run {
                            if let localPath = attachment.localFilePath {
                                try? FileManager.default.removeItem(atPath: localPath)
                            }
                            modelContext.delete(attachment)
                            try? modelContext.save()
                        }
                    } catch {
                        await MainActor.run {
                            let op = SyncOp(target: .attachment, operation: .delete, attachment: attachment)
                            NetworkState.shared.enqueue(op)
                        }
                    }
                }
            } else {
                let op = SyncOp(target: .attachment, operation: .delete, attachment: attachment)
                NetworkState.shared.enqueue(op)
            }
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            errorMessage = "Failed to delete attachment: \(error.localizedDescription)"
            showError = true
        }
    }

    private func updateVisibility(_ attachment: Attachment, to newVisibility: AttachmentVisibility) {
        attachment.visibility = newVisibility.rawValue
        attachment.modifiedAt = Date()

        do {
            try modelContext.save()

            if NetworkState.shared.mode == .online {
                Task {
                    do {
                        _ = try await APIClient.shared.updateAttachmentVisibility(
                            attachmentId: attachment.id,
                            visibility: newVisibility.rawValue
                        )
                    } catch {
                        await MainActor.run {
                            let op = SyncOp(target: .attachment, operation: .update, attachment: attachment)
                            NetworkState.shared.enqueue(op)
                        }
                    }
                }
            } else {
                let op = SyncOp(target: .attachment, operation: .update, attachment: attachment)
                NetworkState.shared.enqueue(op)
            }
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            errorMessage = "\(error.localizedDescription)"
            showError = true
        }
    }

    private func downloadAttachment(_ attachment: Attachment) {
        Task {
            do {
                let uploadService = AttachmentUploadService(modelContext: modelContext)
                let response = try await uploadService.getPresignedDownloadURL(attachmentId: attachment.id)

                // Open URL in Safari/browser
                if let url = URL(string: response.url) {
                    await MainActor.run {
                        UIApplication.shared.open(url)
                    }
                }
            } catch {
                guard !AuthError.isAuthError(error) else { return }
                await MainActor.run {
                    errorMessage = "Failed to download: \(error.localizedDescription)"
                    showError = true
                }
            }
        }
    }

}

// MARK: - Attachment Row Component

struct AttachmentRow: View {
    let attachment: Attachment
    let onDownload: () -> Void
    let onDelete: () -> Void
    var onVisibilityChange: ((AttachmentVisibility) -> Void)?
    var isOffline: Bool = false

    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy"
        return formatter
    }

    private var fileIcon: String {
        switch attachment.fileExtension {
        case "pdf":
            return "doc.text.fill"
        case "jpg", "jpeg", "png", "gif":
            return "photo.fill"
        case "mp4", "mov", "avi", "mkv", "wmv", "flv", "webm":
            return "video.fill"
        case "xlsx", "xls":
            return "tablecells.fill"
        case "docx", "doc":
            return "doc.richtext.fill"
        case "txt":
            return "doc.plaintext.fill"
        case "zip", "rar":
            return "doc.zipper"
        default:
            return "doc.fill"
        }
    }

    private var iconColor: Color {
        switch attachment.fileExtension {
        case "pdf":
            return .red
        case "jpg", "jpeg", "png", "gif":
            return .green
        case "mp4", "mov", "avi", "mkv", "wmv", "flv", "webm":
            return .purple
        case "xlsx", "xls":
            return .green
        case "docx", "doc":
            return .blue
        default:
            return .gray
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            // File icon
            Image(systemName: fileIcon)
                .font(.system(size: 20))
                .foregroundColor(iconColor)
                .frame(width: 40, height: 40)
                .background(iconColor.opacity(0.1))
                .cornerRadius(8)

            // File details
            VStack(alignment: .leading, spacing: 4) {
                Text(attachment.filename)
                    .font(.system(size: 15, weight: .medium))
                    .lineLimit(3)

                HStack(spacing: 8) {
                    Text(attachment.formattedFileSize)
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Text("•")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Text(attachment.type)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)

                    Text("•")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Text(attachment.attachmentVisibility.displayName)
                        .font(.caption2)
                        .fontWeight(.medium)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(attachment.attachmentVisibility.color.opacity(0.15))
                        .foregroundColor(attachment.attachmentVisibility.color)
                        .cornerRadius(4)

                    if attachment.uploadNeeded {
                        Text("•")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Image(systemName: "icloud.and.arrow.up")
                            .font(.caption2)
                            .padding(3)
                            .background(Color.orange)
                            .foregroundColor(.white)
                            .cornerRadius(4)
                    }
                }

                Text(dateFormatter.string(from: attachment.createdAt))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Download button (disabled for pending uploads or offline)
            Button {
                onDownload()
            } label: {
                Image(systemName: "square.and.arrow.down")
                    .font(.system(size: 18))
                    .foregroundColor(attachment.uploadNeeded || isOffline ? .gray : .blue)
            }
            .disabled(attachment.uploadNeeded || isOffline)

            // Delete button
            Button {
                onDelete()
            } label: {
                Image(systemName: "trash")
                    .font(.system(size: 16))
                    .foregroundColor(.red)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
        .contextMenu {
            let targetVisibility: AttachmentVisibility = attachment.attachmentVisibility == .internal ? .public : .internal

            Button {
                onVisibilityChange?(targetVisibility)
            } label: {
                Label(
                    AppStrings.Sessions.makeVisibility(targetVisibility.displayName),
                    systemImage: targetVisibility == .public ? "globe" : "lock.fill"
                )
            }
        }
    }
}
