//
//  OCPPhotoWalkthroughView.swift
//  Egalvanic PZ
//
//  Camera-first bulk OCP child creation for a parent asset.
//  Simplified version of PhotoWalkthroughView — only creates children, no parent.
//

import SwiftUI
import SwiftData
import PhotosUI

// MARK: - Staged OCP

struct StagedOCP: Identifiable {
    let id = UUID()
    var nodeClass: NodeClass
    var nodeSubtype: NodeSubtype?
    var photos: [WalkthroughStagedPhoto]
    var label: String?
    var draftAttributes: [UUID: String] = [:]
}

// MARK: - State Machine

private enum OCPWalkthroughState {
    case capturing       // Taking photos
    case classifying     // Pick class/subtype, label, required attrs
    case deciding        // Add another or done?
    case reviewing       // Review all before creation
    case creating        // Creating assets
}

// MARK: - View

struct OCPPhotoWalkthroughView: View {
    let parent: NodeV2
    let sld: SLDV2
    let activeSession: IRSession?
    let activeViewId: UUID?
    let onComplete: () -> Void
    let onCancel: () -> Void

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState

    @Query(sort: \NodeClass.name) private var allNodeClasses: [NodeClass]

    @State private var currentState: OCPWalkthroughState = .capturing
    @State private var stagedOCPs: [StagedOCP] = []

    // Current WIP
    @State private var currentPhotos: [WalkthroughStagedPhoto] = []
    @State private var selectedNodeClass: NodeClass?
    @State private var selectedNodeSubtype: NodeSubtype?
    @State private var assetLabel = ""
    @State private var draftCoreAttributes: [UUID: String] = [:]

    // Camera
    @State private var showingCamera = false
    @State private var capturedImage: UIImage?
    // ZP-2230
    @State private var photosetId = UUID()
    @State private var showCaptureError = false
    @State private var captureErrorMessage = ""
    @State private var showPermissionPrompt = false
    @State private var selectedPhotoItem: PhotosPickerItem?

    // Voice input
    @State private var speechManager = SpeechRecognitionManager()
    @State private var voiceTargetField: UUID? = nil
    @State private var voiceTargetIsLabel: Bool = false

    // Alerts
    @State private var showDiscardAlert = false
    @State private var showSuccessAlert = false
    @State private var showSubtypePicker = false

    // Creation progress
    @State private var createProgress = 0
    @State private var createdCount = 0

    private var ocpNodeClasses: [NodeClass] {
        allNodeClasses.filter { !$0.is_deleted && $0.ocp }
    }

    private var filteredSubtypes: [NodeSubtype] {
        guard let nc = selectedNodeClass else { return [] }
        return (nc.node_subtypes ?? []).filter { !$0.is_deleted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var requiredAttributes: [NodeClassProperty] {
        selectedNodeClass?.definition.filter { $0.af_required } ?? []
    }

    private var hasUnsavedData: Bool {
        !stagedOCPs.isEmpty || !currentPhotos.isEmpty
    }

    // MARK: - Body

    var body: some View {
        NavigationView {
            Group {
                switch currentState {
                case .capturing:
                    photoCaptureView
                case .classifying:
                    classificationView
                case .deciding:
                    decisionView
                case .reviewing:
                    reviewView
                case .creating:
                    creatingView
                }
            }
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        if hasUnsavedData {
                            showDiscardAlert = true
                        } else {
                            onCancel()
                        }
                    }
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
        .alert(AppStrings.Alerts.discardChanges, isPresented: $showDiscardAlert) {
            Button(AppStrings.Common.keepEditing, role: .cancel) { }
            Button(AppStrings.Common.discard, role: .destructive) { onCancel() }
        } message: {
            Text("You have \(stagedOCPs.count) staged sub-component(s). Discard?")
        }
        .alert(AppStrings.Common.success, isPresented: $showSuccessAlert) {
            Button(AppStrings.Common.done) { onComplete() }
        } message: {
            Text("Created \(createdCount) sub-component(s) with photos.")
        }
        // ZP-2230
        .alert("Could not save photo", isPresented: $showCaptureError) {
            Button("OK", role: .cancel) {}
        } message: { Text(captureErrorMessage) }
        .alert("Photos library access required", isPresented: $showPermissionPrompt) {
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Egalvanic PZ saves a stamped copy of every captured photo to your Photos library as an audit-recovery copy. Enable “Add Photos Only” in Settings → Privacy → Photos → Egalvanic PZ to use the camera.")
        }
        .sheet(isPresented: $showSubtypePicker) {
            subtypePickerSheet
        }
    }

    private var navigationTitle: String {
        switch currentState {
        case .capturing: return "Photograph Sub-Component"
        case .classifying: return "Classify Sub-Component"
        case .deciding: return "More Sub-Components?"
        case .reviewing: return "Review (\(stagedOCPs.count))"
        case .creating: return "Creating..."
        }
    }

    // MARK: - Photo Capture

    private var photoCaptureView: some View {
        VStack(spacing: 0) {
            // Parent context
            HStack(spacing: 8) {
                NodeTypeIconCircle(
                    style: parent.node_class?.style,
                    size: 28,
                    iconSize: 14,
                    backgroundColor: Color.blue.opacity(0.1),
                    iconColor: .blue
                )
                Text(parent.label)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                Spacer()
                Text("\(stagedOCPs.count) staged")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal)
            .padding(.top, 8)

            Text("Take photos of sub-component \(stagedOCPs.count + 1)")
                .font(.headline)
                .padding(.top, 20)

            Text(AppStrings.PhotoWalkthrough.multiplePhotosPerSub)
                .font(.subheadline)
                .foregroundColor(.secondary)

            // Photo thumbnails
            if !currentPhotos.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(currentPhotos) { photo in
                            ZStack(alignment: .topTrailing) {
                                if let uiImage = UIImage(contentsOfFile: photo.localFileURL.path) {
                                    Image(uiImage: uiImage)
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: 100, height: 100)
                                        .clipped()
                                        .cornerRadius(10)
                                }
                                Button {
                                    currentPhotos.removeAll { $0.id == photo.id }
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.title3)
                                        .foregroundColor(.white)
                                        .background(Color.red)
                                        .clipShape(Circle())
                                }
                                .offset(x: 6, y: -6)
                            }
                        }
                    }
                    .padding()
                }
                .frame(height: 130)
                .padding(.top, 20)
            } else {
                VStack(spacing: 12) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.secondary)
                    Text(AppStrings.Common.noPhotosYet)
                        .foregroundColor(.secondary)
                }
                .frame(height: 130)
                .padding(.top, 40)
            }

            Spacer()

            VStack(spacing: 16) {
                HStack(spacing: 30) {
                    PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                        VStack(spacing: 8) {
                            Image(systemName: "photo.on.rectangle")
                                .font(.system(size: 30))
                            Text(AppStrings.Photos.gallery)
                                .font(.caption)
                        }
                        .frame(width: 80, height: 80)
                        .background(Color.blue.opacity(0.1))
                        .cornerRadius(12)
                    }
                    .buttonStyle(.plain)

                    Button {
                        Task {
                            let granted = await PhotoCaptureService.ensureLibraryWriteAccess()
                            if granted { showingCamera = true } else { showPermissionPrompt = true }
                        }
                    } label: {
                        VStack(spacing: 8) {
                            Image(systemName: "camera.fill")
                                .font(.system(size: 30))
                            Text(AppStrings.Photos.camera)
                                .font(.caption)
                        }
                        .frame(width: 80, height: 80)
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(12)
                    }
                    .buttonStyle(.plain)
                }

                Button {
                    currentState = .classifying
                } label: {
                    Text(AppStrings.PhotoWalkthrough.doneWithThisSubComponent)
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(currentPhotos.isEmpty ? Color.gray : Color.blue)
                        .cornerRadius(12)
                }
                .disabled(currentPhotos.isEmpty)
                .padding(.horizontal)
            }
            .padding(.bottom, 30)
        }
        .background(
            NavigationLink(
                destination: CameraViewWrapper { image in
                    capturedImage = image
                    showingCamera = false
                },
                isActive: $showingCamera
            ) { EmptyView() }.hidden()
        )
        .onChange(of: capturedImage) { _, newImage in
            if let image = newImage {
                handleNewPhoto(image: image, fromGallery: false)
                capturedImage = nil
            }
        }
        .onChange(of: selectedPhotoItem) { _, newItem in
            Task {
                guard let item = newItem,
                      let data = try? await item.loadTransferable(type: Data.self),
                      let uiImage = UIImage(data: data)
                else { return }
                await MainActor.run {
                    handleNewPhoto(image: uiImage, fromGallery: true)
                    selectedPhotoItem = nil
                }
            }
        }
    }

    // MARK: - Classification

    private var classificationView: some View {
        VStack(spacing: 0) {
            // Photo strip
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(currentPhotos) { photo in
                        if let uiImage = UIImage(contentsOfFile: photo.localFileURL.path) {
                            Image(uiImage: uiImage)
                                .resizable()
                                .scaledToFill()
                                .frame(width: 60, height: 60)
                                .clipped()
                                .cornerRadius(8)
                        }
                    }
                }
                .padding()
            }
            .frame(height: 80)
            .background(Color(UIColor.systemGray6))

            ScrollView {
                VStack(spacing: 24) {
                    // Class picker
                    VStack(alignment: .leading, spacing: 8) {
                        Text(AppStrings.Assets.assetType)
                            .font(.headline)

                        Picker("", selection: $selectedNodeClass) {
                            Text(AppStrings.PhotoWalkthrough.selectAssetType)
                                .foregroundColor(.secondary)
                                .tag(nil as NodeClass?)
                            ForEach(ocpNodeClasses) { nc in
                                Text(nc.name).tag(nc as NodeClass?)
                            }
                        }
                        .pickerStyle(.menu)
                        .padding()
                        .background(Color(UIColor.systemGray6))
                        .cornerRadius(10)
                        .onChange(of: selectedNodeClass) { _, newClass in
                            selectedNodeSubtype = nil
                            draftCoreAttributes = [:]
                            // Apply class-defined defaults (ZP-2251) for the
                            // newly picked class.
                            if let nc = newClass {
                                CoreAttributesService.applyDefaultValues(from: nc, into: &draftCoreAttributes)
                            }
                            assetLabel = ""
                        }
                    }

                    // Subtype
                    if !filteredSubtypes.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Subtype (Optional)")
                                .font(.headline)

                            Button {
                                showSubtypePicker = true
                            } label: {
                                HStack {
                                    Text(selectedNodeSubtype?.name ?? "None")
                                        .foregroundColor(selectedNodeSubtype == nil ? .secondary : .primary)
                                        .lineLimit(nil)
                                        .multilineTextAlignment(.leading)
                                        .fixedSize(horizontal: false, vertical: true)
                                    Spacer()
                                    Image(systemName: "chevron.down")
                                        .foregroundColor(.secondary)
                                }
                                .padding()
                                .background(Color(UIColor.systemGray6))
                                .cornerRadius(10)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    // Label (voice-enabled)
                    if selectedNodeClass != nil {
                        SpeechTextField(
                            title: "Asset Name (Optional)",
                            text: $assetLabel,
                            icon: "tag",
                            sharedSpeechManager: speechManager,
                            isVoiceTarget: voiceTargetIsLabel,
                            onMicTapped: { activateVoice(forLabel: true) }
                        )

                        // Required attrs (voice-enabled)
                        if !requiredAttributes.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(AppStrings.Assets.requiredFields)
                                    .font(.headline)

                                ForEach(requiredAttributes) { prop in
                                    VoiceAttributeRowView(
                                        classProperty: prop,
                                        currentValue: draftCoreAttributes[prop.id] ?? "",
                                        onValueChange: { draftCoreAttributes[prop.id] = $0 },
                                        speechManager: speechManager,
                                        isVoiceTarget: voiceTargetField == prop.id
                                    ) {
                                        activateVoice(forAttribute: prop.id)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding()
            }
            .scrollDismissesKeyboard(.interactively)

            // Next button
            Button {
                advanceFromClassification()
            } label: {
                Text(AppStrings.PhotoWalkthrough.saveAndContinue)
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(selectedNodeClass == nil ? Color.gray : Color.blue)
                    .cornerRadius(12)
            }
            .disabled(selectedNodeClass == nil)
            .padding()
        }
    }

    // MARK: - Decision

    private var decisionView: some View {
        VStack(spacing: 30) {
            Spacer()

            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 60))
                .foregroundColor(.green)

            Text("\(stagedOCPs.count) sub-component(s) ready")
                .font(.title3)
                .fontWeight(.semibold)

            VStack(spacing: 16) {
                Button {
                    resetForNext()
                    currentState = .capturing
                } label: {
                    Label(AppStrings.PhotoWalkthrough.addAnotherSubComponent, systemImage: "plus.circle")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }

                Button {
                    currentState = .reviewing
                } label: {
                    Label(AppStrings.PhotoWalkthrough.reviewAndCreate, systemImage: "checkmark.circle")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
            }
            .padding(.horizontal, 30)

            Spacer()
        }
    }

    // MARK: - Review

    private var reviewView: some View {
        VStack(spacing: 0) {
            List {
                ForEach(stagedOCPs) { ocp in
                    HStack(spacing: 12) {
                        // First photo thumbnail
                        if let firstPhoto = ocp.photos.first,
                           let uiImage = UIImage(contentsOfFile: firstPhoto.localFileURL.path) {
                            Image(uiImage: uiImage)
                                .resizable()
                                .scaledToFill()
                                .frame(width: 50, height: 50)
                                .clipped()
                                .cornerRadius(8)
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            Text(ocp.label ?? autoLabel(for: ocp))
                                .font(.body)
                                .fontWeight(.medium)

                            HStack(spacing: 4) {
                                Text(ocp.nodeClass.name)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text("· \(ocp.photos.count) photo(s)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }

                        Spacer()
                    }
                }
                .onDelete { indexSet in
                    stagedOCPs.remove(atOffsets: indexSet)
                    if stagedOCPs.isEmpty {
                        resetForNext()
                        currentState = .capturing
                    }
                }
            }

            // Create button
            Button {
                Task { await createAll() }
            } label: {
                Text("Create \(stagedOCPs.count) Sub-Component(s)")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .cornerRadius(12)
            }
            .padding()
        }
    }

    // MARK: - Creating

    private var creatingView: some View {
        VStack(spacing: 20) {
            Spacer()
            ProgressView()
                .scaleEffect(1.5)
            Text(AppStrings.PhotoWalkthrough.creatingSubComponents)
                .font(.headline)
            Text("\(createProgress) of \(stagedOCPs.count)")
                .font(.subheadline)
                .foregroundColor(.secondary)
            Spacer()
        }
    }

    // MARK: - Subtype Picker Sheet

    private var subtypePickerSheet: some View {
        NavigationStack {
            List {
                Button {
                    selectedNodeSubtype = nil
                    showSubtypePicker = false
                } label: {
                    HStack {
                        Text("None").foregroundColor(.primary)
                        Spacer()
                        if selectedNodeSubtype == nil {
                            Image(systemName: "checkmark").foregroundColor(.blue)
                        }
                    }
                }
                ForEach(filteredSubtypes) { subtype in
                    Button {
                        selectedNodeSubtype = subtype
                        showSubtypePicker = false
                    } label: {
                        HStack {
                            Text(subtype.name).foregroundColor(.primary)
                            Spacer()
                            if selectedNodeSubtype?.id == subtype.id {
                                Image(systemName: "checkmark").foregroundColor(.blue)
                            }
                        }
                    }
                }
            }
            .navigationTitle(AppStrings.Issues.selectSubtype)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.done) { showSubtypePicker = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Helpers

    private func handleNewPhoto(image: UIImage, fromGallery: Bool) {
        // ZP-2230: capture-and-persist atomically (Photos library +
        // Documents/photos). Stamp uses the OCP wizard's pending
        // context — eventual child node doesn't exist yet, so label
        // falls back to the user-typed asset name (or "Pending OCP").
        // ZP-2404: gallery imports skip the audit-copy save (the
        // original already lives in the user's library).
        let trimmed = assetLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        let label = trimmed.isEmpty ? "Pending OCP under \(parent.label)" : trimmed
        let ctx = PhotoStampContext.pending(
            kind: "Pending OCP",
            label: label,
            photoType: "node_profile",
            photosetId: photosetId,
            sld: sld,
            building: parent.room?.floor?.building,
            floor: parent.room?.floor,
            room: parent.room
        )
        Task {
            do {
                let photo = try await PhotoCaptureService.captureAndPersistPending(
                    image: image,
                    context: ctx,
                    saveAuditCopy: !fromGallery
                )
                currentPhotos.append(WalkthroughStagedPhoto(
                    filename: photo.filename ?? "",
                    localFilepath: photo.local_filepath ?? ""
                ))
            } catch {
                AppLogger.log(.error,
                    "[PhotoCapture/ocp] capture failed: \(error.localizedDescription)",
                    category: .photo)
                captureErrorMessage = error.localizedDescription
                showCaptureError = true
            }
        }
    }

    private func advanceFromClassification() {
        guard let nodeClass = selectedNodeClass else { return }
        let trimmedLabel = assetLabel.trimmingCharacters(in: .whitespacesAndNewlines)

        stagedOCPs.append(StagedOCP(
            nodeClass: nodeClass,
            nodeSubtype: selectedNodeSubtype,
            photos: currentPhotos,
            label: trimmedLabel.isEmpty ? nil : trimmedLabel,
            draftAttributes: draftCoreAttributes
        ))

        resetForNext()

        if stagedOCPs.count >= 10 {
            currentState = .reviewing
        } else {
            currentState = .deciding
        }
    }

    private func resetForNext() {
        currentPhotos = []
        selectedNodeClass = nil
        selectedNodeSubtype = nil
        assetLabel = ""
        draftCoreAttributes = [:]
        voiceTargetField = nil
        voiceTargetIsLabel = false
        if speechManager.isRecording { speechManager.stopRecording() }
    }

    private func activateVoice(forLabel: Bool = false, forAttribute attributeId: UUID? = nil) {
        let wasRecording = speechManager.isRecording
        if wasRecording { speechManager.stopRecording() }
        if !speechManager.isAuthorized { speechManager.requestAuthorization() }

        voiceTargetIsLabel = forLabel
        voiceTargetField = attributeId
        speechManager.transcribedText = ""

        if wasRecording {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                speechManager.startRecording()
            }
        } else {
            speechManager.startRecording()
        }
    }

    private func autoLabel(for ocp: StagedOCP) -> String {
        let existingChildren = sld.nodes.filter {
            $0.parent_id == parent.id && !$0.is_deleted && $0.node_class?.id == ocp.nodeClass.id
        }.count
        let sameClassBefore = stagedOCPs.filter { $0.nodeClass.id == ocp.nodeClass.id }
        let index = sameClassBefore.firstIndex(where: { $0.id == ocp.id }) ?? 0
        return "\(ocp.nodeClass.name) \(existingChildren + index + 1)"
    }

    // MARK: - Creation

    private func createAll() async {
        await MainActor.run {
            currentState = .creating
            createProgress = 0
        }

        var count = 0

        for ocp in stagedOCPs {
            let label = ocp.label ?? autoLabel(for: ocp)

            // Create child node
            let childNode = NodeV2(
                id: UUID(),
                label: label,
                type: ocp.nodeClass.style,
                sld: sld,
                parent_id: parent.id,
                x: 0,
                y: 0,
                width: ocp.nodeClass.width,
                height: ocp.nodeClass.height,
                photos: [],
                is_deleted: false,
                location: parent.location,
                room: parent.room,
                node_class: ocp.nodeClass,
                node_subtype: ocp.nodeSubtype,
                core_attributes: [],
                node_tasks: [],
                issues: [],
                ir_photos: [],
                ir_sessions: [],
                com: 1,
                qr_code: nil
            )

            // Apply core attributes before sync
            if !ocp.draftAttributes.isEmpty {
                await MainActor.run {
                    CoreAttributesService.applyCoreAttributeChanges(
                        to: childNode,
                        selectedClass: ocp.nodeClass,
                        originalClass: nil,
                        draftAttributes: ocp.draftAttributes,
                        modelContext: modelContext
                    )
                }
            }

            // Convert staged photos
            var photos: [Photo] = []
            for staged in ocp.photos {
                photos.append(Photo(
                    id: UUID(),
                    node: childNode,
                    userTask: nil,
                    issue: nil,
                    url: nil,
                    type: "node_profile",
                    sld: sld,
                    upload_needed: true,
                    local_filepath: staged.localFilepath,
                    filename: staged.filename,
                    is_deleted: false
                ))
            }

            // Ensure parent is group type
            if count == 0 && parent.type != "group" {
                await MainActor.run {
                    parent.type = "group"
                    parent.needsSync = true
                    parent.lastModifiedAt = Date()
                    try? modelContext.save()
                }
                if networkState.mode == .online {
                    if let payload = try? APIClient.shared.buildNodeUpdatePayload(parent) {
                        let parentId = parent.id
                        Task {
                            do {
                                _ = try await APIClient.shared.sendNodeUpdate(nodeId: parentId, payload: payload)
                            } catch {
                                await MainActor.run {
                                    networkState.enqueue(SyncOp(target: .node, operation: .update, node: parent))
                                }
                            }
                        }
                    }
                } else {
                    await MainActor.run {
                        networkState.enqueue(SyncOp(target: .node, operation: .update, node: parent))
                    }
                }
            }

            // Create node with photos
            await NodeService.createNewNodeWithPhotosAndIR(
                node: childNode,
                photos: photos,
                irPhotos: [],
                networkState: networkState,
                modelContext: modelContext,
                skipGraphUpdate: activeViewId != nil
            )

            // Session mapping
            if let session = activeSession {
                await linkNodeAndAncestorsToSession(node: childNode, session: session)
            }

            // View mapping
            if let viewId = activeViewId {
                await linkNodeToView(node: childNode, viewId: viewId)
            }

            count += 1
            await MainActor.run { createProgress = count }
        }

        await MainActor.run {
            createdCount = count
            showSuccessAlert = true
        }
    }

    // MARK: - Session & View Linking

    private func linkNodeAndAncestorsToSession(node: NodeV2, session: IRSession) async {
        var nodesToLink: [NodeV2] = [node, parent]
        var current: NodeV2? = parent
        while let pid = current?.parent_id,
              let ancestor = sld.nodes.first(where: { $0.id == pid && !$0.is_deleted }) {
            nodesToLink.append(ancestor)
            current = ancestor
        }
        for n in nodesToLink {
            await linkSingleNodeToSession(node: n, session: session)
        }
    }

    private func linkSingleNodeToSession(node: NodeV2, session: IRSession) async {
        if session.nodes.contains(where: { $0.id == node.id }) { return }
        await MainActor.run {
            if !session.nodes.contains(where: { $0.id == node.id }) { session.nodes.append(node) }
            if !node.ir_sessions.contains(where: { $0.id == session.id }) { node.ir_sessions.append(session) }
            try? modelContext.save()
        }
        if networkState.mode == .online {
            do {
                _ = try await APIClient.shared.createNodeSessionMapping(nodeId: node.id, sessionId: session.id)
            } catch {
                await MainActor.run {
                    networkState.enqueue(SyncOp(target: .mappingNodeSession, operation: .create, mappingData: MappingData.nodeSession(nodeId: node.id, sessionId: session.id, isDeleted: false)))
                }
            }
        } else {
            await MainActor.run {
                networkState.enqueue(SyncOp(target: .mappingNodeSession, operation: .create, mappingData: MappingData.nodeSession(nodeId: node.id, sessionId: session.id, isDeleted: false)))
            }
        }
    }

    private func linkNodeToView(node: NodeV2, viewId: UUID) async {
        let mappingId = UUID()
        await MainActor.run {
            modelContext.insert(MappingNodeSLDView(
                id: mappingId, node_id: node.id, sld_view_id: viewId,
                x: 0, y: 0, width: node.width, height: node.height,
                is_collapsed: false, is_deleted: false, created_at: Date(), modified_at: Date()
            ))
            try? modelContext.save()
        }
        if networkState.mode == .online {
            do {
                _ = try await APIClient.shared.addNodeToView(viewId: viewId, nodeId: node.id, x: 0, y: 0, width: node.width, height: node.height)
            } catch {
                await MainActor.run {
                    networkState.enqueueNodeSLDViewCreate(mappingId: mappingId, nodeId: node.id, viewId: viewId, x: 0, y: 0, width: node.width, height: node.height)
                }
            }
        } else {
            await MainActor.run {
                networkState.enqueueNodeSLDViewCreate(mappingId: mappingId, nodeId: node.id, viewId: viewId, x: 0, y: 0, width: node.width, height: node.height)
            }
        }
    }
}
