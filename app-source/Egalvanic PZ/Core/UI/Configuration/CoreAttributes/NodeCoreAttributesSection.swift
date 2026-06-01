import SwiftUI
import SwiftData

// MARK: - Node Core Attributes Section
struct NodeCoreAttributesSection: View {
    let node: NodeV2
    let selectedNodeClass: NodeClass?
    @Binding var draftAttributes: [UUID: String]
    let nameplatePhotos: [Photo]
    @State private var showOnlyRequired = false

    @Environment(\.modelContext) private var context
    @EnvironmentObject var networkState: NetworkState

    /// Callback when extraction completes — passes the new attributes so parent can sync originalCoreAttributes
    var onExtractionComplete: (([UUID: String]) -> Void)?

    // Extraction state
    @State private var isExtracting = false
    @State private var showOverwriteConfirmation = false
    @State private var overwriteExisting = false
    @State private var extractionAlert: ExtractionAlertType?

    // MARK: - Computed Helpers

    private var hasNameplatePhotos: Bool { !nameplatePhotos.isEmpty }
    private var anyPhotoStaged: Bool { nameplatePhotos.contains { $0.upload_needed } }
    private var isNewAsset: Bool { node.sld == nil }

    var body: some View {
        // ZP-2161: when ``eng-lib`` is enabled this renders as a
        // sub-section of the unified Engineering card — compact
        // "Custom Attributes" subtitle, no icon. When the company
        // doesn't have the feature flag, fall back to the legacy
        // "Core Attributes" full-header with the icon, since this
        // is its own top-level card on the page.
        let hasEngLib = AuthService.shared.hasFeature("eng-lib")
        let title = hasEngLib
            ? AppStrings.Assets.customAttributes
            : AppStrings.Assets.coreAttributes
        let icon = hasEngLib ? "" : "slider.horizontal.3"
        EntityCoreAttributesView(
            entity: node,
            selectedEntityClass: selectedNodeClass,
            draftAttributes: $draftAttributes,
            showOnlyRequired: $showOnlyRequired,
            sectionTitle: title,
            sectionIcon: icon,
            headerTrailingContent: {
                if networkState.isOnline {
                    Button {
                        showOverwriteConfirmation = true
                    } label: {
                        if isExtracting {
                            ProgressView()
                                .controlSize(.regular)
                                .tint(.blue)
                        } else {
                            let isDisabled = isExtracting || !hasNameplatePhotos
                            Image(systemName: "sparkles")
                                .font(.body)
                                .padding(6)
                                .background(isDisabled ? Color(.systemGray5) : Color.blue.opacity(0.1))
                                .foregroundColor(isDisabled ? Color(.systemGray) : .blue)
                                .cornerRadius(6)
                        }
                    }
                    .buttonStyle(.plain)
                    .disabled(isExtracting || !hasNameplatePhotos)
                }
            }
        )
        .alert(AppStrings.Forms.extractNameplateData, isPresented: $showOverwriteConfirmation) {
            Button(AppStrings.Forms.keepExistingValues) {
                overwriteExisting = false
                performExtraction()
            }
            Button(AppStrings.Forms.overwriteExistingValues) {
                overwriteExisting = true
                performExtraction()
            }
            Button(AppStrings.Common.cancel, role: .cancel) {}
        } message: {
            Text(AppStrings.Forms.extractWillPopulateAttributes)
        }
        .alert(item: $extractionAlert) { alertType in
            switch alertType {
            case .success(let message):
                Alert(
                    title: Text(AppStrings.Forms.extractionComplete),
                    message: Text(message),
                    dismissButton: .default(Text(AppStrings.Common.ok))
                )
            case .error(let message):
                Alert(
                    title: Text(AppStrings.Forms.extractionFailed),
                    message: Text(message),
                    dismissButton: .default(Text(AppStrings.Common.ok))
                )
            }
        }
    }

    // MARK: - Extraction

    private func performExtraction() {
        isExtracting = true
        Task {
            do {
                if isNewAsset || anyPhotoStaged {
                    try await performTempExtraction()
                } else {
                    try await performStandardExtraction()
                }
            } catch {
                await MainActor.run {
                    isExtracting = false
                    extractionAlert = .error(error.localizedDescription)
                }
            }
        }
    }

    /// Temp extraction: upload staged photos to temp S3 bucket, then call temp extraction API
    private func performTempExtraction() async throws {
        guard let nodeClass = selectedNodeClass else { return }

        // 1. Extract filenames from staged nameplate photos (lightweight values for actor safety)
        let stagedRequests = nameplatePhotos
            .filter { $0.upload_needed && $0.filename != nil }
            .map { S3PresignedURLService.TempPhotoUploadRequest(filename: $0.filename!, photoId: $0.id) }

        // 2. Upload staged photos to temp_photos bucket
        let tempURLs = try await S3PresignedURLService.shared.uploadPhotosToTempBucket(photos: stagedRequests)

        // 3. Collect already-uploaded photo URLs
        let uploadedURLs = nameplatePhotos
            .filter { !$0.upload_needed && $0.url != nil }
            .compactMap { $0.url }

        let allPhotoURLs = tempURLs + uploadedURLs
        guard !allPhotoURLs.isEmpty else {
            await MainActor.run {
                isExtracting = false
                extractionAlert = .error(AppStrings.Forms.noPhotosForExtraction)
            }
            return
        }

        // 4. Build current core attributes context for the request (send ALL properties, matching Android)
        let coreAttrs: [[String: String]] = nodeClass.definition.map { prop in
            return ["id": prop.id.uuidString, "name": prop.name, "value": draftAttributes[prop.id] ?? ""]
        }

        // 5. Call temp extraction API
        let response = try await APIClient.shared.extractTempNameplateData(
            photoURLs: allPhotoURLs,
            nodeClassId: nodeClass.id,
            coreAttributes: coreAttrs,
            overwriteExisting: overwriteExisting
        )

        guard response.success else {
            await MainActor.run {
                isExtracting = false
                extractionAlert = .error(AppStrings.Forms.extractionNotSuccessful)
            }
            return
        }

        // 6. Merge extracted attributes into draft (filter blanks, respect overwrite preference as safety net)
        var newAttributes: [UUID: String] = draftAttributes
        var extractedCount = 0
        for attr in response.extracted_attributes {
            guard let value = attr.value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { continue }
            if overwriteExisting || (newAttributes[attr.id] ?? "").isEmpty {
                newAttributes[attr.id] = value
                extractedCount += 1
            }
        }

        await MainActor.run {
            draftAttributes = newAttributes

            // Only persist to SwiftData for existing assets (not placeholder nodes in AddAsset)
            if !isNewAsset {
                CoreAttributesService.applyCoreAttributeChanges(
                    to: node,
                    selectedClass: selectedNodeClass,
                    originalClass: selectedNodeClass,
                    draftAttributes: newAttributes,
                    modelContext: context
                )
                try? context.save()
            }

            onExtractionComplete?(newAttributes)
            isExtracting = false
            extractionAlert = .success(AppStrings.Forms.attributesUpdated(extractedCount))
        }
    }

    /// Standard extraction: all photos already uploaded, use nodeId-based API
    private func performStandardExtraction() async throws {
        // Step 1: Call extraction API
        let extractionResponse = try await APIClient.shared.extractNameplateData(
            nodeIds: [node.id],
            overwriteExisting: overwriteExisting
        )

        guard extractionResponse.success else {
            let errorMsg = extractionResponse.errors.joined(separator: ", ")
            await MainActor.run {
                isExtracting = false
                extractionAlert = .error(errorMsg.isEmpty ? AppStrings.Forms.extractionNotSuccessful : errorMsg)
            }
            return
        }

        // Step 2: Fetch enriched node to get updated attributes
        let enrichedNode = try await APIClient.shared.fetchEnrichedNode(nodeId: node.id)

        // Step 3: Build new attributes from enriched response
        var newAttributes: [UUID: String] = draftAttributes
        for attr in enrichedNode.core_attributes {
            if let classPropertyId = UUID(uuidString: attr.node_class_property) {
                newAttributes[classPropertyId] = attr.value ?? ""
            }
        }

        await MainActor.run {
            // Step 4: Update draft attributes (updates the form UI)
            draftAttributes = newAttributes

            // Step 5: Persist to SwiftData
            CoreAttributesService.applyCoreAttributeChanges(
                to: node,
                selectedClass: selectedNodeClass,
                originalClass: selectedNodeClass,
                draftAttributes: newAttributes,
                modelContext: context
            )
            try? context.save()

            // Step 6: Notify parent to sync originalCoreAttributes (prevents hasChanges)
            onExtractionComplete?(newAttributes)

            isExtracting = false
            let message = buildSuccessMessage(extractionResponse)
            extractionAlert = .success(message)
        }
    }

    private func buildSuccessMessage(_ response: ExtractNameplateResponse) -> String {
        var parts: [String] = []
        if response.updated > 0 {
            parts.append(AppStrings.Forms.attributesUpdated(response.updated))
        }
        if response.skipped > 0 {
            parts.append(AppStrings.Forms.skippedCount(response.skipped))
        }
        if !response.skipped_no_photos.isEmpty {
            parts.append(AppStrings.Forms.noPhotosForExtraction)
        }
        return parts.isEmpty ? AppStrings.Forms.extractionCompletedSuccessfully : parts.joined(separator: ". ") + "."
    }
}

// MARK: - Extraction Alert Type

enum ExtractionAlertType: Identifiable {
    case success(String)
    case error(String)

    var id: String {
        switch self {
        case .success(let msg): return "success-\(msg)"
        case .error(let msg): return "error-\(msg)"
        }
    }
}
