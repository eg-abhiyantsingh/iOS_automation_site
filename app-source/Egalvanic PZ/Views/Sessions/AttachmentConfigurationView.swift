//
//  AttachmentConfigurationView.swift
//  Egalvanic PZ
//
//  View for configuring attachment details before upload.
//

import SwiftUI
import SwiftData

struct AttachmentConfigurationView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    let session: IRSession
    let fileURL: URL
    let filename: String
    let fileSize: Int64

    @State private var selectedType: String = "General Documentation"
    @State private var selectedVisibility: AttachmentVisibility = .internal
    @State private var selectedTask: UserTask?
    @State private var selectedNodeIds: Set<UUID> = []
    @State private var isUploading = false
    @State private var uploadProgress: Double = 0.0
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var showNodeSelection = false
    @State private var showCancelConfirmation = false

    @StateObject private var uploadService = AttachmentUploadService()

    // Tasks linked to this session (using the relationship from IRSession)
    private var sessionTasks: [UserTask] {
        session.user_tasks.filter { !$0.is_deleted }
    }

    init(session: IRSession, fileURL: URL, filename: String, fileSize: Int64) {
        self.session = session
        self.fileURL = fileURL
        self.filename = filename
        self.fileSize = fileSize
    }

    var body: some View {
        NavigationStack {
            Form {
                // File Info Section
                Section {
                    HStack(spacing: 12) {
                        fileIcon
                            .font(.system(size: 40))
                            .foregroundColor(fileIconColor)

                        VStack(alignment: .leading, spacing: 4) {
                            Text(filename)
                                .font(.headline)
                                .lineLimit(2)

                            Text(formattedFileSize)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 8)
                } header: {
                    Text(AppStrings.Sessions.file)
                }

                // Attachment Type Section
                Section {
                    Picker("Type", selection: $selectedType) {
                        ForEach(Attachment.ATTACHMENT_TYPES, id: \.self) { type in
                            Text(type).tag(type)
                        }
                    }
                } header: {
                    Text(AppStrings.Sessions.attachmentType)
                }

                // Visibility Section
                Section {
                    Picker(AppStrings.Sessions.visibility, selection: $selectedVisibility) {
                        ForEach(AttachmentVisibility.allCases) { visibility in
                            Text(visibility.displayName).tag(visibility)
                        }
                    }
                    .pickerStyle(.segmented)
                } header: {
                    Text(AppStrings.Sessions.visibility)
                } footer: {
                    Text(AppStrings.Sessions.visibilityFooter)
                }

                // Task Selection Section (Optional)
                Section {
                    Picker("Task", selection: $selectedTask) {
                        Text(AppStrings.Common.none).tag(nil as UserTask?)
                        ForEach(sessionTasks) { task in
                            Text(task.title).tag(task as UserTask?)
                        }
                    }
                    .onChange(of: selectedTask) { _, _ in
                        // Clear node selection when task changes
                        selectedNodeIds.removeAll()
                    }
                } header: {
                    Text(AppStrings.Sessions.associatedTask)
                } footer: {
                    Text(AppStrings.Sessions.linkToTask)
                }

                // Node Selection Section (Optional, shown when task is selected)
                if let task = selectedTask, !task.linkedNodes.isEmpty {
                    Section {
                        Button {
                            showNodeSelection = true
                        } label: {
                            HStack {
                                Text(AppStrings.Sessions.selectAssets)
                                Spacer()
                                Text(AppStrings.Sessions.selectedOfTotal(selectedNodeIds.count, task.linkedNodes.count))
                                    .foregroundColor(.secondary)
                                Image(systemName: "chevron.right")
                                    .foregroundColor(.secondary)
                                    .font(.caption)
                            }
                        }
                        .foregroundColor(.primary)
                    } header: {
                        Text(AppStrings.Sessions.associatedAssets)
                    } footer: {
                        Text(AppStrings.Sessions.attachmentAssetsFooter)
                    }
                }

                // Upload Progress Section
                if isUploading {
                    Section {
                        VStack(spacing: 12) {
                            ProgressView(value: uploadProgress) {
                                Text(AppStrings.Sessions.uploading)
                                    .font(.subheadline)
                            }

                            Text("\(Int(uploadProgress * 100))%")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 8)
                    }
                }
            }
            .navigationTitle(AppStrings.Sessions.uploadFile)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) {
                        if isUploading {
                            showCancelConfirmation = true
                        } else {
                            cleanupAndDismiss()
                        }
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button(AppStrings.CommonExtra.upload) {
                        Task {
                            await uploadAttachment()
                        }
                    }
                    .disabled(isUploading)
                }
            }
            .sheet(isPresented: $showNodeSelection) {
                if let task = selectedTask {
                    NodeMultiSelectionView(
                        nodes: task.linkedNodes.filter { !$0.is_deleted },
                        selectedNodeIds: $selectedNodeIds
                    )
                }
            }
            .alert(AppStrings.Sessions.uploadError, isPresented: $showError) {
                Button(AppStrings.Common.ok, role: .cancel) { }
            } message: {
                Text(errorMessage)
            }
            .alert(AppStrings.Sessions.cancelUpload, isPresented: $showCancelConfirmation) {
                Button(AppStrings.Sessions.continueUpload, role: .cancel) { }
                Button(AppStrings.Sessions.cancelUploadButton, role: .destructive) {
                    Task {
                        await cancelUpload()
                    }
                }
            } message: {
                Text(AppStrings.Sessions.uploadInProgress)
            }
            .onReceive(uploadService.$uploadProgress) { progress in
                uploadProgress = progress
            }
            .onReceive(uploadService.$isUploading) { uploading in
                isUploading = uploading
            }
        }
    }

    // MARK: - Computed Properties

    private var fileIcon: Image {
        let ext = (filename as NSString).pathExtension.lowercased()
        switch ext {
        case "pdf":
            return Image(systemName: "doc.text.fill")
        case "jpg", "jpeg", "png", "gif":
            return Image(systemName: "photo.fill")
        case "xlsx", "xls":
            return Image(systemName: "tablecells.fill")
        case "docx", "doc":
            return Image(systemName: "doc.richtext.fill")
        case "txt":
            return Image(systemName: "doc.plaintext.fill")
        case "zip", "rar":
            return Image(systemName: "doc.zipper")
        default:
            return Image(systemName: "doc.fill")
        }
    }

    private var fileIconColor: Color {
        let ext = (filename as NSString).pathExtension.lowercased()
        switch ext {
        case "pdf":
            return .red
        case "jpg", "jpeg", "png", "gif":
            return .green
        case "xlsx", "xls":
            return .green
        case "docx", "doc":
            return .blue
        default:
            return .gray
        }
    }

    private var formattedFileSize: String {
        ByteCountFormatter.string(fromByteCount: fileSize, countStyle: .file)
    }

    // MARK: - Actions

    private func uploadAttachment() async {
        // Validate file size
        guard fileSize <= Attachment.MAX_FILE_SIZE else {
            errorMessage = AppStrings.Sessions.fileTooLarge
            showError = true
            return
        }

        uploadService.setModelContext(modelContext)

        do {
            // Copy file to cache directory
            let cachedFileURL = try uploadService.copyFileToCache(from: fileURL, filename: filename)

            // Get SLD from session
            let sld = session.sld

            // Get company ID from AuthService
            guard let companyIdString = AuthService.shared.currentUser?.company_id,
                  let companyId = UUID(uuidString: companyIdString) else {
                errorMessage = AppStrings.Sessions.unableToGetCompanyInfo
                showError = true
                return
            }

            // Create attachment
            let attachment = Attachment(
                companyId: companyId,
                sldId: sld.id,
                sessionId: session.id,
                taskId: selectedTask?.id,
                type: selectedType,
                filename: filename,
                fileSize: fileSize,
                localFilePath: cachedFileURL.path,
                visibility: selectedVisibility.rawValue
            )

            // Insert into context
            modelContext.insert(attachment)
            try modelContext.save()

            // Check if online (respects manual offline mode)
            if NetworkState.shared.mode == .online {
                // Upload immediately
                try await uploadService.uploadAttachment(attachment, nodeIds: Array(selectedNodeIds))

                // Save node mappings locally
                for nodeId in selectedNodeIds {
                    let mapping = AttachmentNodeMapping(
                        attachmentId: attachment.id,
                        nodeId: nodeId
                    )
                    modelContext.insert(mapping)
                }
                try modelContext.save()

                dismiss()
            } else {
                // Queue for later sync
                let syncOp = SyncOp(
                    target: .attachment,
                    operation: .create,
                    attachment: attachment
                )
                await NetworkState.shared.enqueue(syncOp)

                // Also queue node mappings
                for nodeId in selectedNodeIds {
                    let mappingData = MappingData.attachmentNode(
                        attachmentId: attachment.id,
                        nodeId: nodeId,
                        isDeleted: false
                    )
                    let mappingSyncOp = SyncOp(
                        target: .mappingAttachmentNode,
                        operation: .create,
                        mappingData: mappingData
                    )
                    await NetworkState.shared.enqueue(mappingSyncOp)

                    // Save mapping locally
                    let mapping = AttachmentNodeMapping(
                        attachmentId: attachment.id,
                        nodeId: nodeId
                    )
                    modelContext.insert(mapping)
                }
                try modelContext.save()

                dismiss()
            }
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            errorMessage = error.localizedDescription
            showError = true
        }
    }

    private func cancelUpload() async {
        // Cancel ongoing upload if any
        // For now, just dismiss
        cleanupAndDismiss()
    }

    private func cleanupAndDismiss() {
        // Clean up temp file if needed
        dismiss()
    }
}
