//
//  QuickCountView.swift
//  Egalvanic PZ
//
//  Bulk asset creation with simplified count and optional photo/OCP capture
//

import SwiftUI
import SwiftData
import PhotosUI

// MARK: - Data Models

struct QuickCountPhotoset: Identifiable {
    let id = UUID()
    var assetIndex: Int  // Which asset (1-based) this photoset is for
    var photos: [QuickCountStagedPhoto]
    var ocpAssets: [QuickCountOCPAsset]  // OCPs documented for this photoset's asset (with photos)
    var ocpCountEntries: [QuickCountOCPCountEntry] = []  // OCPs added by count only (no photos)
}

struct QuickCountEntry: Identifiable {
    let id = UUID()
    var nodeClass: NodeClass
    var nodeSubtype: NodeSubtype? = nil  // Optional subtype for this entry
    var quantity: Int = 1
    var photosets: [QuickCountPhotoset] = []  // Multiple photosets, each for a different asset

    // Get the next available asset index for a new photoset
    var nextPhotosetIndex: Int {
        let usedIndices = Set(photosets.map { $0.assetIndex })
        for i in 1...quantity {
            if !usedIndices.contains(i) {
                return i
            }
        }
        return quantity + 1  // All slots used
    }

    // Check if all assets have photosets
    var allAssetsHavePhotosets: Bool {
        photosets.count >= quantity
    }
}

struct QuickCountOCPAsset: Identifiable {
    let id = UUID()
    var nodeClass: NodeClass
    var nodeSubtype: NodeSubtype? = nil  // Optional subtype for this OCPD
    var photos: [QuickCountStagedPhoto]
    var quantity: Int = 1  // For count-based OCPDs (no photos)
}

// Per-instance photoset attached to a count entry (optional photos for Relay 1, Relay 2, ...)
struct QuickCountOCPCountInstancePhotoset: Identifiable {
    let id = UUID()
    var instanceIndex: Int  // 1-based; matches ocpIndex in createAssets loop
    var photos: [QuickCountStagedPhoto]
}

// OCPD entry for count-based quick add, with optional per-instance photosets
struct QuickCountOCPCountEntry: Identifiable {
    let id = UUID()
    var nodeClass: NodeClass
    var nodeSubtype: NodeSubtype? = nil
    var quantity: Int = 1
    var instancePhotosets: [QuickCountOCPCountInstancePhotoset] = []

    // Next 1-based instance index without a photoset, bounded by quantity
    var nextInstanceIndex: Int {
        let used = Set(instancePhotosets.map { $0.instanceIndex })
        for i in 1...max(quantity, 1) where !used.contains(i) {
            return i
        }
        return quantity + 1
    }

    var allInstancesHavePhotosets: Bool {
        instancePhotosets.filter { (1...quantity).contains($0.instanceIndex) }.count >= quantity
    }
}

struct QuickCountStagedPhoto: Identifiable, CategorizedStagedPhoto {
    let id = UUID()
    var filename: String
    var localFilepath: String
    var type: String = "node_profile"

    // Resolve the persisted photo on disk; JPEG bytes are never retained in memory.
    var localFileURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(localFilepath)
    }
}

// MARK: - Quick Count View

struct QuickCountView: View {
    // ZP-2331: session is optional so the non-WO Locations tree can
    // launch Quick Count too. When nil, newly-created assets attach
    // only to the room (no nodeSession mapping, no session.nodes
    // append), and the SLD is sourced from the room hierarchy.
    let session: IRSession?
    let room: Room
    let onComplete: () -> Void
    let onCancel: () -> Void

    /// Resolve the SLD for this run: prefer the session's SLD when
    /// there is one (existing WO behavior), otherwise walk up from the
    /// room → floor → building → sld.
    private var contextSLD: SLDV2? {
        session?.sld ?? room.floor?.building?.sld
    }

    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    // Query all node classes and subtypes
    @Query(sort: \NodeClass.name) private var allNodeClasses: [NodeClass]
    @Query private var allNodeSubtypes: [NodeSubtype]

    // State
    @State private var entries: [QuickCountEntry] = []

    // State for subtype selection flow
    @State private var pendingNodeClass: NodeClass? = nil
    @State private var showSubtypeSelection = false
    @State private var selectedSubtypeForPending: NodeSubtype? = nil
    @State private var expandedEntryIds: Set<UUID> = []
    @State private var isCreating = false
    @State private var createProgress = 0
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var showSuccessAlert = false
    @State private var createdCount = 0
    @State private var showAddAssetMenu = false
    @State private var showDiscardChangesAlert = false

    // Computed properties
    private var availableNodeClasses: [NodeClass] {
        allNodeClasses.filter { !$0.is_deleted }
    }

    private var ocpNodeClasses: [NodeClass] {
        allNodeClasses.filter { !$0.is_deleted && $0.ocp }
    }

    // Get subtypes for a specific node class
    private func subtypesForClass(_ nodeClass: NodeClass) -> [NodeSubtype] {
        allNodeSubtypes
            .filter { !$0.is_deleted && $0.node_class_id == nodeClass.id }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var totalAssets: Int {
        entries.reduce(0) { sum, entry in
            let ocpPhotoCount = entry.photosets.reduce(0) { $0 + $1.ocpAssets.count }
            let ocpCountTotal = entry.photosets.reduce(0) { sum, photoset in
                sum + photoset.ocpCountEntries.reduce(0) { $0 + $1.quantity }
            }
            return sum + entry.quantity + ocpPhotoCount + ocpCountTotal
        }
    }

    private var totalPhotos: Int {
        entries.reduce(0) { sum, entry in
            sum + entry.photosets.reduce(0) { photosetSum, photoset in
                let photos = photoset.photos.count
                let ocpPhotos = photoset.ocpAssets.reduce(0) { $0 + $1.photos.count }
                let ocpCountPhotos = photoset.ocpCountEntries.reduce(0) { ceSum, ce in
                    ceSum + ce.instancePhotosets.reduce(0) { $0 + $1.photos.count }
                }
                return photosetSum + photos + ocpPhotos + ocpCountPhotos
            }
        }
    }

    private var hasEntries: Bool {
        !entries.isEmpty && totalAssets > 0
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Room breadcrumb
                HStack(spacing: 4) {
                    Image(systemName: "door.left.hand.open")
                        .font(.caption)
                        .foregroundColor(.orange)
                    Text(room.fullPath)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.top, 8)
                .padding(.bottom, 8)

                if entries.isEmpty {
                    // Empty state
                    ContentUnavailableView(
                        AppStrings.QuickCount.noAssetTypesAdded,
                        systemImage: "square.stack.3d.down.right",
                        description: Text(AppStrings.QuickCount.tapAddAssetTypeToStart)
                    )
                    .frame(maxHeight: .infinity)
                } else {
                    // Entries list
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach($entries) { $entry in
                                QuickCountEntryRow(
                                    entry: $entry,
                                    isExpanded: expandedEntryIds.contains(entry.id),
                                    ocpNodeClasses: ocpNodeClasses,
                                    allNodeSubtypes: allNodeSubtypes,
                                    onToggleExpand: {
                                        withAnimation(.easeInOut(duration: 0.2)) {
                                            if expandedEntryIds.contains(entry.id) {
                                                expandedEntryIds.remove(entry.id)
                                            } else {
                                                expandedEntryIds.insert(entry.id)
                                            }
                                        }
                                    },
                                    onDelete: {
                                        entries.removeAll { $0.id == entry.id }
                                    },
                                    room: room,
                                    sld: contextSLD
                                )
                            }
                        }
                        .padding()
                        .padding(.bottom, 150) // Space for summary bar
                    }
                }

                // Add Asset Type button
                Button(action: { showAddAssetMenu = true }) {
                    HStack {
                        Image(systemName: "plus.circle.fill")
                            .foregroundColor(.blue)
                        Text(AppStrings.QuickCount.addAssetType)
                            .fontWeight(.medium)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(10)
                }
                .padding(.horizontal)
                .padding(.bottom, 8)

                // Summary bar
                if hasEntries {
                    VStack(spacing: 8) {
                        Divider()

                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(AppStrings.QuickCount.summary)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text(AppStrings.QuickCount.summaryCount(assets: totalAssets, photos: totalPhotos))
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }

                            Spacer()

                            Button(action: createAssets) {
                                Text(AppStrings.QuickCount.createAssets(totalAssets))
                                    .fontWeight(.semibold)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(Color.blue)
                                    .foregroundColor(.white)
                                    .cornerRadius(10)
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 8)
                    }
                    .background(Color(UIColor.systemBackground))
                }
            }
            .navigationTitle(AppStrings.QuickCount.quickCount)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        if hasEntries {
                            showDiscardChangesAlert = true
                        } else {
                            onCancel()
                        }
                    }
                }
            }
        }
        .navigationViewStyle(.stack)
        .confirmationDialog(AppStrings.QuickCount.selectAssetType, isPresented: $showAddAssetMenu, titleVisibility: .visible) {
            ForEach(availableNodeClasses) { nodeClass in
                Button(nodeClass.name) {
                    handleNodeClassSelection(nodeClass)
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        }
        .sheet(isPresented: $showSubtypeSelection) {
            if let nodeClass = pendingNodeClass {
                SubtypeSelectionSheet(
                    nodeClass: nodeClass,
                    subtypes: subtypesForClass(nodeClass),
                    onSelect: { subtype in
                        addEntry(nodeClass: nodeClass, subtype: subtype)
                        showSubtypeSelection = false
                        pendingNodeClass = nil
                    },
                    onSkip: {
                        addEntry(nodeClass: nodeClass, subtype: nil)
                        showSubtypeSelection = false
                        pendingNodeClass = nil
                    },
                    onCancel: {
                        showSubtypeSelection = false
                        pendingNodeClass = nil
                    }
                )
            }
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(errorMessage)
        }
        .alert(AppStrings.QuickCount.success, isPresented: $showSuccessAlert) {
            Button(AppStrings.Common.ok) {
                onComplete()
            }
        } message: {
            Text(AppStrings.QuickCount.successfullyCreatedAssets(createdCount))
        }
        .alert(AppStrings.Alerts.discardChanges, isPresented: $showDiscardChangesAlert) {
            Button(AppStrings.Common.keepEditing, role: .cancel) { }
            Button(AppStrings.Common.discard, role: .destructive) {
                onCancel()
            }
        } message: {
            Text(AppStrings.QuickCount.discardAssetsMessage)
        }
        .overlay {
            if isCreating {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                VStack(spacing: 12) {
                    ProgressView()
                    Text(AppStrings.QuickCount.creatingProgress(createProgress, totalAssets))
                        .font(.subheadline)
                }
                .padding()
                .background(Color(UIColor.systemBackground))
                .cornerRadius(10)
                .shadow(radius: 5)
            }
        }
    }

    private func handleNodeClassSelection(_ nodeClass: NodeClass) {
        let subtypes = subtypesForClass(nodeClass)
        if subtypes.isEmpty {
            // No subtypes, add entry directly
            addEntry(nodeClass: nodeClass, subtype: nil)
        } else {
            // Has subtypes, show subtype selection sheet
            pendingNodeClass = nodeClass
            showSubtypeSelection = true
        }
    }

    private func addEntry(nodeClass: NodeClass, subtype: NodeSubtype?) {
        let entry = QuickCountEntry(nodeClass: nodeClass, nodeSubtype: subtype)
        // ZP-2252: prepend so newly added asset types appear at the top
        // and the user can adjust quantity without scrolling past
        // previously added entries.
        entries.insert(entry, at: 0)
        expandedEntryIds.insert(entry.id)
    }

    private func createAssets() {
        isCreating = true
        createProgress = 0

        Task {
            var createdNodeCount = 0
            let sld = contextSLD

            do {
                for entry in entries {
                    // Create the specified quantity of assets
                    for assetIndex in 1...entry.quantity {
                        let assetLabel = "\(entry.nodeClass.name) \(assetIndex)"

                        // Check if this specific asset has OCPs (from its photoset - photos or count-based)
                        let photosetForThisAsset = entry.photosets.first(where: { $0.assetIndex == assetIndex })
                        let hasPhotoOCPs = photosetForThisAsset?.ocpAssets.isEmpty == false
                        let hasCountOCPs = photosetForThisAsset?.ocpCountEntries.isEmpty == false
                        let hasOCPs = hasPhotoOCPs || hasCountOCPs

                        // Create parent asset node
                        let asset = NodeV2(
                            id: UUID(),
                            label: assetLabel,
                            type: hasOCPs ? "group" : entry.nodeClass.style,
                            sld: sld,
                            parent_id: nil,
                            x: Double.random(in: -1000...1000),
                            y: Double.random(in: -1000...1000),
                            width: entry.nodeClass.width,
                            height: entry.nodeClass.height,
                            photos: [],
                            is_deleted: false,
                            room: room,
                            node_class: entry.nodeClass,
                            node_subtype: entry.nodeSubtype,
                            core_attributes: [],
                            node_tasks: [],
                            ir_photos: [],
                            // ZP-2230: COM defaults to 1 (serviceable)
                            // rather than nil so newly counted assets
                            // pass the readiness check that requires a
                            // non-nil COM value.
                            com: 1
                        )
                        asset.lastModifiedAt = Date()

                        // Convert staged photos to Photo entities
                        var photos: [Photo] = []
                        if let photoset = photosetForThisAsset {
                            for stagedPhoto in photoset.photos {
                                let photo = Photo(
                                    id: UUID(),
                                    node: asset,
                                    userTask: nil,
                                    issue: nil,
                                    url: nil,
                                    type: stagedPhoto.type,
                                    sld: sld,
                                    upload_needed: true,
                                    local_filepath: stagedPhoto.localFilepath,
                                    filename: stagedPhoto.filename,
                                    is_deleted: false
                                )
                                photos.append(photo)
                            }
                        }

                        // Use NodeService to create node with photos (handles insert, terminals, sync)
                        await NodeService.createNewNodeWithPhotosAndIR(
                            node: asset,
                            photos: photos,
                            irPhotos: [],
                            networkState: networkState,
                            modelContext: modelContext,
                            skipGraphUpdate: true  // Skip graph update for each node, we'll do one at end
                        )

                        // Add session relationship (NodeService doesn't handle
                        // this) — only when there's a WO context.
                        if let session = session {
                            session.nodes.append(asset)
                            asset.ir_sessions.append(session)
                            await createSessionMapping(for: asset)
                        }

                        createdNodeCount += 1
                        await MainActor.run { createProgress += 1 }

                        // Create OCPs with photos as children of this asset
                        if let photoset = photosetForThisAsset {
                            for ocpAsset in photoset.ocpAssets {
                                let ocpLabel = "\(ocpAsset.nodeClass.name)"

                                let ocp = NodeV2(
                                    id: UUID(),
                                    label: ocpLabel,
                                    type: ocpAsset.nodeClass.style,
                                    sld: sld,
                                    parent_id: asset.id,
                                    x: 0,
                                    y: 0,
                                    width: ocpAsset.nodeClass.width,
                                    height: ocpAsset.nodeClass.height,
                                    photos: [],
                                    is_deleted: false,
                                    room: room,
                                    node_class: ocpAsset.nodeClass,
                                    node_subtype: ocpAsset.nodeSubtype,
                                    core_attributes: [],
                                    node_tasks: [],
                                    ir_photos: [],
                                    // ZP-2230: default COM to 1 for OCPDs too.
                                    com: 1
                                )
                                ocp.lastModifiedAt = Date()

                                // Convert OCP's staged photos to Photo entities
                                var ocpPhotos: [Photo] = []
                                for stagedPhoto in ocpAsset.photos {
                                    let photo = Photo(
                                        id: UUID(),
                                        node: ocp,
                                        userTask: nil,
                                        issue: nil,
                                        url: nil,
                                        type: stagedPhoto.type,
                                        sld: sld,
                                        upload_needed: true,
                                        local_filepath: stagedPhoto.localFilepath,
                                        filename: stagedPhoto.filename,
                                        is_deleted: false
                                    )
                                    ocpPhotos.append(photo)
                                }

                                // Use NodeService for OCP node
                                await NodeService.createNewNodeWithPhotosAndIR(
                                    node: ocp,
                                    photos: ocpPhotos,
                                    irPhotos: [],
                                    networkState: networkState,
                                    modelContext: modelContext,
                                    skipGraphUpdate: true
                                )

                                // Add session relationship (WO only)
                                if let session = session {
                                    session.nodes.append(ocp)
                                    ocp.ir_sessions.append(session)
                                    await createSessionMapping(for: ocp)
                                }

                                createdNodeCount += 1
                            }

                            // Create count-based OCPs as children of this asset (photos optional per instance)
                            for ocpCountEntry in photoset.ocpCountEntries {
                                for ocpIndex in 1...ocpCountEntry.quantity {
                                    let ocpLabel = "\(ocpCountEntry.nodeClass.name) \(ocpIndex)"

                                    let ocp = NodeV2(
                                        id: UUID(),
                                        label: ocpLabel,
                                        type: ocpCountEntry.nodeClass.style,
                                        sld: sld,
                                        parent_id: asset.id,
                                        x: 0,
                                        y: 0,
                                        width: ocpCountEntry.nodeClass.width,
                                        height: ocpCountEntry.nodeClass.height,
                                        photos: [],
                                        is_deleted: false,
                                        room: room,
                                        node_class: ocpCountEntry.nodeClass,
                                        node_subtype: ocpCountEntry.nodeSubtype,
                                        core_attributes: [],
                                        node_tasks: [],
                                        ir_photos: [],
                                        // ZP-2230: default COM to 1.
                                        com: 1
                                    )
                                    ocp.lastModifiedAt = Date()

                                    // Build Photo records for any staged instance photoset matching this index
                                    var ocpPhotos: [Photo] = []
                                    if let instancePhotoset = ocpCountEntry.instancePhotosets.first(where: { $0.instanceIndex == ocpIndex }) {
                                        for stagedPhoto in instancePhotoset.photos {
                                            let photoRecord = Photo(
                                                id: UUID(),
                                                node: ocp,
                                                userTask: nil,
                                                issue: nil,
                                                url: nil,
                                                type: stagedPhoto.type,
                                                sld: sld,
                                                upload_needed: true,
                                                local_filepath: stagedPhoto.localFilepath,
                                                filename: stagedPhoto.filename,
                                                is_deleted: false
                                            )
                                            ocpPhotos.append(photoRecord)
                                        }
                                    }

                                    await NodeService.createNewNodeWithPhotosAndIR(
                                        node: ocp,
                                        photos: ocpPhotos,
                                        irPhotos: [],
                                        networkState: networkState,
                                        modelContext: modelContext,
                                        skipGraphUpdate: true
                                    )

                                    // Add session relationship (WO only)
                                    if let session = session {
                                        session.nodes.append(ocp)
                                        ocp.ir_sessions.append(session)
                                        await createSessionMapping(for: ocp)
                                    }

                                    createdNodeCount += 1
                                    await MainActor.run { createProgress += 1 }
                                }
                            }
                        }
                    }
                }

                // Save final state
                try modelContext.save()

                // Update the WebView graph once at the end. `sld` is
                // resolved via `contextSLD` and can be nil if the room
                // hierarchy is detached from an SLD — skip the push
                // rather than crashing.
                if let sld = sld {
                    WebViewBridge.updateGraphFromSLD(sld.id, in: modelContext, animated: true)
                }

                await MainActor.run {
                    isCreating = false
                    createdCount = createdNodeCount
                    showSuccessAlert = true
                }

            } catch {
                guard !AuthError.isAuthError(error) else { return }
                await MainActor.run {
                    isCreating = false
                    errorMessage = error.localizedDescription
                    showError = true
                }
            }
        }
    }

    /// Creates a session mapping for a node (API call if online, queue if offline).
    /// No-op when there's no WO context — callers already gate by `if let session`.
    private func createSessionMapping(for node: NodeV2) async {
        guard let session = session else { return }
        if networkState.mode == .online {
            do {
                _ = try await APIClient.shared.createNodeSessionMapping(
                    nodeId: node.id,
                    sessionId: session.id
                )
            } catch {
                AppLogger.log(.error, "Failed to create session mapping for node \(node.id), queueing: \(error)", category: .ui)
                networkState.enqueue(SyncOp(
                    target: .mappingNodeSession,
                    operation: .create,
                    mappingData: MappingData.nodeSession(nodeId: node.id, sessionId: session.id, isDeleted: false)
                ))
            }
        } else {
            networkState.enqueue(SyncOp(
                target: .mappingNodeSession,
                operation: .create,
                mappingData: MappingData.nodeSession(nodeId: node.id, sessionId: session.id, isDeleted: false)
            ))
        }
    }
}

// MARK: - Entry Row

struct QuickCountEntryRow: View {
    @Binding var entry: QuickCountEntry
    let isExpanded: Bool
    let ocpNodeClasses: [NodeClass]
    let allNodeSubtypes: [NodeSubtype]
    let onToggleExpand: () -> Void
    let onDelete: () -> Void
    // ZP-2230: room + sld for photo-stamp location breadcrumb.
    var room: Room? = nil
    var sld: SLDV2? = nil

    @State private var showPhotoCaptureFlow = false
    @State private var showOCPPrompt = false
    @State private var showOCPCaptureFlow = false
    @State private var showOCPCountFlow = false  // For count-based OCPD entry
    @State private var showQuantityInput = false
    @State private var quantityInputText = ""
    @State private var currentPhotosetIndex: Int = 1  // Which asset index we're adding photos for

    // Get subtypes for a specific node class
    private func subtypesForClass(_ nodeClass: NodeClass) -> [NodeSubtype] {
        allNodeSubtypes
            .filter { !$0.is_deleted && $0.node_class_id == nodeClass.id }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var totalPhotosCount: Int {
        entry.photosets.reduce(0) { $0 + $1.photos.count }
    }

    private var totalOCPsCount: Int {
        let photoOCPs = entry.photosets.reduce(0) { $0 + $1.ocpAssets.count }
        let countOCPs = entry.photosets.reduce(0) { sum, photoset in
            sum + photoset.ocpCountEntries.reduce(0) { $0 + $1.quantity }
        }
        return photoOCPs + countOCPs
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header row
            VStack(spacing: 8) {
                HStack {
                    // Expand/collapse button
                    Button(action: onToggleExpand) {
                        Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(isExpanded ? .blue : .secondary)
                            .frame(width: 24, height: 24)
                    }
                    .buttonStyle(.plain)

                    // Class name
                    Text(entry.nodeClass.name)
                        .font(.headline)

                    Spacer()

                    // Quantity selector with long-press
                    QuickCountQuantitySelector(
                        value: $entry.quantity,
                        onLongPress: {
                            quantityInputText = "\(entry.quantity)"
                            showQuantityInput = true
                        }
                    )

                    // Delete button
                    Button(action: onDelete) {
                        Image(systemName: "trash")
                            .foregroundColor(.red)
                            .frame(width: 30, height: 30)
                    }
                    .buttonStyle(.plain)
                }

                // Subtype and summary on second line
                HStack {
                    if let subtype = entry.nodeSubtype {
                        Text(subtype.name)
                            .font(.caption)
                            .foregroundColor(.blue)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(4)
                    }

                    if !isExpanded && (totalPhotosCount > 0 || totalOCPsCount > 0) {
                        HStack(spacing: 6) {
                            if totalPhotosCount > 0 {
                                Text(AppStrings.QuickCount.photosCount(totalPhotosCount))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            if totalOCPsCount > 0 {
                                Text(AppStrings.QuickCount.ocpsCount(totalOCPsCount))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }

                    Spacer()
                }
                .padding(.leading, 24)  // Align with text above (after chevron)
            }
            .padding()
            .background(Color(UIColor.systemGray6))
            .cornerRadius(10)

            // Expanded content
            if isExpanded {
                VStack(alignment: .leading, spacing: 12) {
                    // Photosets section
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(AppStrings.QuickCount.photosets)
                                .font(.subheadline)
                                .fontWeight(.medium)
                            Spacer()
                        }
                        .padding(.horizontal)

                        // Show existing photosets
                        ForEach(entry.photosets.sorted(by: { $0.assetIndex < $1.assetIndex })) { photoset in
                            PhotosetRow(
                                photoset: photoset,
                                nodeClassName: entry.nodeClass.name,
                                canHaveOCPs: entry.nodeClass.box,
                                ocpNodeClasses: ocpNodeClasses,
                                allNodeSubtypes: allNodeSubtypes,
                                onDelete: {
                                    entry.photosets.removeAll { $0.id == photoset.id }
                                },
                                onAddOCP: { ocpAsset in
                                    if let index = entry.photosets.firstIndex(where: { $0.id == photoset.id }) {
                                        entry.photosets[index].ocpAssets.append(ocpAsset)
                                    }
                                },
                                onDeleteOCP: { ocpId in
                                    if let index = entry.photosets.firstIndex(where: { $0.id == photoset.id }) {
                                        entry.photosets[index].ocpAssets.removeAll { $0.id == ocpId }
                                    }
                                },
                                onAddOCPCountEntries: { countEntries in
                                    if let index = entry.photosets.firstIndex(where: { $0.id == photoset.id }) {
                                        entry.photosets[index].ocpCountEntries.append(contentsOf: countEntries)
                                    }
                                },
                                onDeleteOCPCountEntry: { countEntryId in
                                    if let index = entry.photosets.firstIndex(where: { $0.id == photoset.id }) {
                                        entry.photosets[index].ocpCountEntries.removeAll { $0.id == countEntryId }
                                    }
                                }
                            )
                        }

                        // Add photoset button (if not all assets have photosets) - PROMINENT
                        if !entry.allAssetsHavePhotosets {
                            Button(action: {
                                currentPhotosetIndex = entry.nextPhotosetIndex
                                showPhotoCaptureFlow = true
                            }) {
                                HStack {
                                    Image(systemName: "camera.fill")
                                    Text(AppStrings.QuickCount.addPhotosetFor(entry.nodeClass.name, entry.nextPhotosetIndex))
                                }
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.blue)
                                .cornerRadius(8)
                            }
                            .padding(.horizontal)
                        } else {
                            HStack {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                                Text(AppStrings.QuickCount.allAssetsHavePhotosets(entry.quantity))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .padding(.horizontal)
                        }
                    }
                }
                .padding(.vertical, 12)
                .background(Color(UIColor.systemGray6).opacity(0.5))
                .cornerRadius(10)
            }
        }
        .fullScreenCover(isPresented: $showPhotoCaptureFlow) {
            PhotoCaptureFlowView(
                assetLabel: "\(entry.nodeClass.name) \(currentPhotosetIndex)",
                onComplete: { photos in
                    let photoset = QuickCountPhotoset(
                        assetIndex: currentPhotosetIndex,
                        photos: photos,
                        ocpAssets: []
                    )
                    entry.photosets.append(photoset)
                    showPhotoCaptureFlow = false
                    // Prompt for OCPD if class supports it
                    if entry.nodeClass.box {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                            showOCPPrompt = true
                        }
                    }
                },
                onCancel: {
                    showPhotoCaptureFlow = false
                },
                room: room,
                sld: sld
            )
        }
        .fullScreenCover(isPresented: $showOCPPrompt) {
            OCPDPromptView(
                assetLabel: "\(entry.nodeClass.name) \(currentPhotosetIndex)",
                onAddByPhoto: {
                    showOCPPrompt = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showOCPCaptureFlow = true
                    }
                },
                onAddByCount: {
                    showOCPPrompt = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showOCPCountFlow = true
                    }
                },
                onNo: {
                    showOCPPrompt = false
                }
            )
        }
        .fullScreenCover(isPresented: $showOCPCaptureFlow) {
            OCPCaptureFlowView(
                ocpNodeClasses: ocpNodeClasses,
                allNodeSubtypes: allNodeSubtypes,
                ocpdIndex: 1,
                onComplete: { ocpAsset in
                    // Add OCPD to the most recently added photoset
                    if let lastIndex = entry.photosets.lastIndex(where: { $0.assetIndex == currentPhotosetIndex }) {
                        entry.photosets[lastIndex].ocpAssets.append(ocpAsset)
                    }
                    showOCPCaptureFlow = false
                },
                onCancel: {
                    showOCPCaptureFlow = false
                },
                room: room,
                sld: sld
            )
        }
        .fullScreenCover(isPresented: $showOCPCountFlow) {
            OCPDCountFlowView(
                ocpNodeClasses: ocpNodeClasses,
                allNodeSubtypes: allNodeSubtypes,
                onComplete: { countEntries in
                    // Add count entries to the most recently added photoset
                    if let lastIndex = entry.photosets.lastIndex(where: { $0.assetIndex == currentPhotosetIndex }) {
                        entry.photosets[lastIndex].ocpCountEntries.append(contentsOf: countEntries)
                    }
                    showOCPCountFlow = false
                },
                onCancel: {
                    showOCPCountFlow = false
                }
            )
        }
        .alert(AppStrings.QuickCount.enterQuantity, isPresented: $showQuantityInput) {
            TextField(AppStrings.QuickCount.quantity, text: $quantityInputText)
                .keyboardType(.numberPad)
            Button(AppStrings.Common.ok) {
                if let newValue = Int(quantityInputText), newValue >= 1 {
                    entry.quantity = newValue
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        }
        .onChange(of: entry.quantity) { _, newValue in
            entry.photosets.removeAll { $0.assetIndex > newValue }
        }
    }
}

// MARK: - OCPD Prompt View (full-screen prompt to add OCPDs)

struct OCPDPromptView: View {
    let assetLabel: String
    let onAddByPhoto: () -> Void
    let onAddByCount: () -> Void
    let onNo: () -> Void

    var body: some View {
        NavigationView {
            VStack(spacing: 30) {
                Spacer()

                Image(systemName: "bolt.shield.fill")
                    .font(.system(size: 60))
                    .foregroundColor(.orange)

                Text(AppStrings.QuickCount.ocpdsQuestion)
                    .font(.title)
                    .fontWeight(.bold)

                Text(AppStrings.QuickCount.addOcpdsFor(assetLabel))
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)

                Spacer()

                VStack(spacing: 12) {
                    Button(action: onAddByPhoto) {
                        HStack {
                            Image(systemName: "camera.fill")
                            Text(AppStrings.QuickCount.addByPhoto)
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.orange)
                        .cornerRadius(12)
                    }

                    Button(action: onAddByCount) {
                        HStack {
                            Image(systemName: "number")
                            Text(AppStrings.QuickCount.addByCount)
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(12)
                    }

                    Button(action: onNo) {
                        Text(AppStrings.QuickCount.noSkip)
                            .font(.headline)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(UIColor.systemGray5))
                            .cornerRadius(12)
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 40)
            }
            .navigationTitle(AppStrings.QuickCount.addOcpds)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        onNo()
                    }
                }
            }
        }
    }
}

// MARK: - Ordinal Helper

func ordinalName(for index: Int) -> String {
    switch index {
    case 1: return "first"
    case 2: return "second"
    case 3: return "third"
    case 4: return "fourth"
    case 5: return "fifth"
    case 6: return "sixth"
    case 7: return "seventh"
    case 8: return "eighth"
    case 9: return "ninth"
    case 10: return "tenth"
    default: return "next"
    }
}

// MARK: - Photoset Row (shows a single photoset with its OCPDs)

struct PhotosetRow: View {
    let photoset: QuickCountPhotoset
    let nodeClassName: String
    let canHaveOCPs: Bool
    let ocpNodeClasses: [NodeClass]
    let allNodeSubtypes: [NodeSubtype]
    let onDelete: () -> Void
    let onAddOCP: (QuickCountOCPAsset) -> Void
    let onDeleteOCP: (UUID) -> Void
    let onAddOCPCountEntries: ([QuickCountOCPCountEntry]) -> Void
    let onDeleteOCPCountEntry: (UUID) -> Void

    @State private var showOCPCaptureFlow = false
    @State private var showOCPCountFlow = false

    private var assetLabel: String {
        "\(nodeClassName) \(photoset.assetIndex)"
    }

    private var totalOCPDCount: Int {
        let photoOCPs = photoset.ocpAssets.count
        let countOCPs = photoset.ocpCountEntries.reduce(0) { $0 + $1.quantity }
        return photoOCPs + countOCPs
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Photoset header (non-collapsible)
            HStack {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
                Text(AppStrings.QuickCount.photosForAsset(photoset.photos.count, assetLabel))
                    .font(.subheadline)
                    .foregroundColor(.primary)
                if totalOCPDCount > 0 {
                    Text(AppStrings.QuickCount.ocpdCount(totalOCPDCount))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()

                // Delete button inline
                Button(action: onDelete) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal)

            // Photo thumbnails with category tabs (always visible)
            CategorizedStagedPhotoStrip(photos: photoset.photos)
                .padding(.horizontal)

            // OCPDs for this photoset
            if canHaveOCPs {
                // Photo-based OCPDs
                ForEach(photoset.ocpAssets) { ocp in
                    HStack {
                        HStack(spacing: 4) {
                            Text("  └─ \(ocp.nodeClass.name)")
                                .font(.caption)
                            if let subtype = ocp.nodeSubtype {
                                Text("(\(subtype.name))")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                        }
                        Text(AppStrings.QuickCount.ocpPhotosCount(ocp.photos.count))
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                        Button {
                            onDeleteOCP(ocp.id)
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal)
                }

                // Count-based OCPDs
                ForEach(photoset.ocpCountEntries) { countEntry in
                    let ceTotalPhotos = countEntry.instancePhotosets.reduce(0) { $0 + $1.photos.count }
                    HStack {
                        HStack(spacing: 4) {
                            Text("  └─ \(countEntry.nodeClass.name)")
                                .font(.caption)
                            if let subtype = countEntry.nodeSubtype {
                                Text("(\(subtype.name))")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                        }
                        Text(
                            ceTotalPhotos > 0
                                ? AppStrings.QuickCount.countWithPhotos(countEntry.quantity, ceTotalPhotos)
                                : AppStrings.QuickCount.countNoPhotos(countEntry.quantity)
                        )
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                        Button {
                            onDeleteOCPCountEntry(countEntry.id)
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal)
                }

                // Add OCPD buttons
                HStack(spacing: 12) {
                    Button(action: { showOCPCaptureFlow = true }) {
                        HStack {
                            Image(systemName: "camera.fill")
                            Text(AppStrings.QuickCount.addByPhoto)
                        }
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.orange)
                    }

                    Button(action: { showOCPCountFlow = true }) {
                        HStack {
                            Image(systemName: "number")
                            Text(AppStrings.QuickCount.addByCount)
                        }
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.blue)
                    }
                }
                .padding(.horizontal)
                .padding(.top, 4)
            }
        }
        .padding(.vertical, 8)
        .fullScreenCover(isPresented: $showOCPCaptureFlow) {
            OCPCaptureFlowView(
                ocpNodeClasses: ocpNodeClasses,
                allNodeSubtypes: allNodeSubtypes,
                ocpdIndex: totalOCPDCount + 1,
                onComplete: { ocpAsset in
                    onAddOCP(ocpAsset)
                    showOCPCaptureFlow = false
                },
                onCancel: {
                    showOCPCaptureFlow = false
                }
            )
        }
        .fullScreenCover(isPresented: $showOCPCountFlow) {
            OCPDCountFlowView(
                ocpNodeClasses: ocpNodeClasses,
                allNodeSubtypes: allNodeSubtypes,
                onComplete: { countEntries in
                    onAddOCPCountEntries(countEntries)
                    showOCPCountFlow = false
                },
                onCancel: {
                    showOCPCountFlow = false
                }
            )
        }
    }
}

// MARK: - Photo Capture Flow View

struct PhotoCaptureFlowView: View {
    let assetLabel: String
    let onComplete: ([QuickCountStagedPhoto]) -> Void
    let onCancel: () -> Void
    // ZP-2230: parent room + sld for stamp location breadcrumb.
    // Optional so call sites that don't have one handy still compile.
    var room: Room? = nil
    var sld: SLDV2? = nil

    @State private var photos: [QuickCountStagedPhoto] = []
    @State private var activePhotoType: String = PhotoCategory.defaultNodeType
    @State private var showingCamera = false
    @State private var capturedImage: UIImage? = nil
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    // ZP-2230
    @State private var photosetId = UUID()
    @State private var showCaptureError = false
    @State private var captureErrorMessage = ""
    @State private var showPermissionPrompt = false

    private var photosByType: [String: [QuickCountStagedPhoto]] {
        Dictionary(grouping: photos, by: { $0.type })
    }

    private var countByType: [String: Int] {
        photosByType.mapValues { $0.count }
    }

    private var activePhotos: [QuickCountStagedPhoto] {
        photosByType[activePhotoType] ?? []
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                Text(AppStrings.QuickCount.takePhotosFor(assetLabel))
                    .font(.headline)
                    .padding(.top, 20)

                PhotoCategoryChipsRow(
                    activePhotoType: $activePhotoType,
                    countByType: countByType
                )

                // Photo thumbnails (filtered to the active category)
                if !activePhotos.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(activePhotos) { photo in
                                ZStack(alignment: .topTrailing) {
                                    if let uiImage = UIImage(contentsOfFile: photo.localFileURL.path) {
                                        Image(uiImage: uiImage)
                                            .resizable()
                                            .scaledToFill()
                                            .frame(width: 80, height: 80)
                                            .clipped()
                                            .cornerRadius(8)
                                    }

                                    Button {
                                        photos.removeAll { $0.id == photo.id }
                                    } label: {
                                        Image(systemName: "xmark.circle.fill")
                                            .font(.caption)
                                            .foregroundColor(.white)
                                            .background(Color.red)
                                            .clipShape(Circle())
                                    }
                                    .offset(x: 4, y: -4)
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                    .frame(height: 100)
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "camera.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                        Text(AppStrings.QuickCount.noPhotosYet)
                            .foregroundColor(.secondary)
                    }
                    .frame(height: 100)
                }

                Spacer()

                // Camera controls
                HStack(spacing: 30) {
                    PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                        VStack(spacing: 8) {
                            Image(systemName: "photo.on.rectangle")
                                .font(.system(size: 28))
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
                                .font(.system(size: 28))
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
                    onComplete(photos)
                } label: {
                    Text(AppStrings.Common.done)
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(photos.isEmpty ? Color.gray : Color.blue)
                        .cornerRadius(12)
                }
                .disabled(photos.isEmpty)
                .padding(.horizontal)
                .padding(.bottom, 30)
            }
            .navigationTitle(AppStrings.QuickCount.addPhotos)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        onCancel()
                    }
                }
            }
        }
        .fullScreenCover(isPresented: $showingCamera) {
            CameraViewWrapper { image in
                capturedImage = image
                showingCamera = false
            }
        }
        .onChange(of: capturedImage) { _, newImage in
            if let image = newImage {
                handleNewPhoto(image: image, fromGallery: false)
                capturedImage = nil
            }
        }
        .onChange(of: selectedPhotoItem) { _, newItem in
            Task {
                guard
                    let item = newItem,
                    let data = try? await item.loadTransferable(type: Data.self),
                    let uiImage = UIImage(data: data)
                else { return }

                await MainActor.run {
                    handleNewPhoto(image: uiImage, fromGallery: true)
                    selectedPhotoItem = nil
                }
            }
        }
        // ZP-2230: surface capture failures + permission denials.
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
    }

    private func handleNewPhoto(image: UIImage, fromGallery: Bool) {
        // ZP-2230: capture-and-persist atomically (Photos library +
        // Documents/photos). Quick-count captures are pre-entity —
        // the eventual node isn't built until the user finalises the
        // count — so the stamp uses the user-visible asset label and
        // the photoset id for grouping in Photos library.
        // ZP-2404: gallery imports skip the audit-copy save (the
        // original already lives in the user's library).
        let ctx = PhotoStampContext.pending(
            kind: "Quick Count",
            label: assetLabel.isEmpty ? "Quick Count capture" : assetLabel,
            photoType: activePhotoType,
            photosetId: photosetId,
            sld: sld ?? room?.floor?.building?.sld,
            building: room?.floor?.building,
            floor: room?.floor,
            room: room
        )
        Task {
            do {
                let photo = try await PhotoCaptureService.captureAndPersistPending(
                    image: image,
                    context: ctx,
                    saveAuditCopy: !fromGallery
                )
                photos.append(QuickCountStagedPhoto(
                    filename: photo.filename ?? "",
                    localFilepath: photo.local_filepath ?? "",
                    type: activePhotoType
                ))
            } catch {
                AppLogger.log(.error,
                    "[PhotoCapture/quickcount] capture failed: \(error.localizedDescription)",
                    category: .ui)
                captureErrorMessage = error.localizedDescription
                showCaptureError = true
            }
        }
    }
}

// MARK: - OCPD Capture Flow View

struct OCPCaptureFlowView: View {
    let ocpNodeClasses: [NodeClass]
    let allNodeSubtypes: [NodeSubtype]
    let ocpdIndex: Int  // Which OCPD we're capturing (1-based)
    let onComplete: (QuickCountOCPAsset) -> Void
    let onCancel: () -> Void
    // ZP-2230: room + sld context for the stamp location breadcrumb.
    var room: Room? = nil
    var sld: SLDV2? = nil

    @State private var selectedNodeClass: NodeClass? = nil
    @State private var selectedNodeSubtype: NodeSubtype? = nil
    @State private var photos: [QuickCountStagedPhoto] = []
    @State private var activePhotoType: String = PhotoCategory.defaultNodeType
    @State private var showingCamera = false
    @State private var capturedImage: UIImage? = nil
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var currentStep: Int = 0  // 0 = photos, 1 = classify
    // ZP-2230
    @State private var photosetId = UUID()
    @State private var showCaptureError = false
    @State private var captureErrorMessage = ""
    @State private var showPermissionPrompt = false

    private var ocpdOrdinal: String {
        ordinalName(for: ocpdIndex)
    }

    private var photosByType: [String: [QuickCountStagedPhoto]] {
        Dictionary(grouping: photos, by: { $0.type })
    }

    private var countByType: [String: Int] {
        photosByType.mapValues { $0.count }
    }

    private var activePhotos: [QuickCountStagedPhoto] {
        photosByType[activePhotoType] ?? []
    }

    // Get subtypes for the selected node class
    private var filteredSubtypes: [NodeSubtype] {
        guard let nodeClass = selectedNodeClass else { return [] }
        return allNodeSubtypes
            .filter { !$0.is_deleted && $0.node_class_id == nodeClass.id }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                if currentStep == 0 {
                    ocpPhotoCaptureContent
                } else {
                    ocpClassificationContent
                }
            }
            .navigationTitle(currentStep == 0 ? AppStrings.QuickCount.ocpdPhotos : AppStrings.QuickCount.classifyOcpd)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(currentStep == 0 ? AppStrings.Common.cancel : AppStrings.Common.back) {
                        if currentStep == 0 {
                            onCancel()
                        } else {
                            currentStep = 0
                        }
                    }
                }
            }
        }
        .fullScreenCover(isPresented: $showingCamera) {
            CameraViewWrapper { image in
                capturedImage = image
                showingCamera = false
            }
        }
        .onChange(of: capturedImage) { _, newImage in
            if let image = newImage {
                handleNewPhoto(image: image, fromGallery: false)
                capturedImage = nil
            }
        }
        .onChange(of: selectedPhotoItem) { _, newItem in
            Task {
                guard
                    let item = newItem,
                    let data = try? await item.loadTransferable(type: Data.self),
                    let uiImage = UIImage(data: data)
                else { return }

                await MainActor.run {
                    handleNewPhoto(image: uiImage, fromGallery: true)
                    selectedPhotoItem = nil
                }
            }
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
    }

    // MARK: - Photo Capture Step

    @ViewBuilder
    private var ocpPhotoCaptureContent: some View {
        Text(AppStrings.QuickCount.takePhotosOfOcpd(ocpdOrdinal))
            .font(.headline)
            .padding(.top, 20)

        PhotoCategoryChipsRow(
            activePhotoType: $activePhotoType,
            countByType: countByType
        )

        ocpPhotoThumbnails

        Spacer()

        ocpCameraControls

        ocpNextButton
    }

    @ViewBuilder
    private var ocpPhotoThumbnails: some View {
        if !activePhotos.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(activePhotos) { photo in
                        OCPPhotoThumbnail(photo: photo) {
                            photos.removeAll { $0.id == photo.id }
                        }
                    }
                }
                .padding(.horizontal)
            }
            .frame(height: 100)
        } else {
            VStack(spacing: 12) {
                Image(systemName: "camera.fill")
                    .font(.system(size: 40))
                    .foregroundColor(.secondary)
                Text(AppStrings.QuickCount.noPhotosYet)
                    .foregroundColor(.secondary)
            }
            .frame(height: 100)
        }
    }

    @ViewBuilder
    private var ocpCameraControls: some View {
        HStack(spacing: 30) {
            PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                VStack(spacing: 8) {
                    Image(systemName: "photo.on.rectangle")
                        .font(.system(size: 28))
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
                        .font(.system(size: 28))
                    Text(AppStrings.Photos.camera)
                        .font(.caption)
                }
                .frame(width: 80, height: 80)
                .background(Color.green.opacity(0.1))
                .cornerRadius(12)
            }
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private var ocpNextButton: some View {
        Button {
            currentStep = 1
        } label: {
            Text(AppStrings.QuickCount.next)
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(photos.isEmpty ? Color.gray : Color.blue)
                .cornerRadius(12)
        }
        .disabled(photos.isEmpty)
        .padding(.horizontal)
        .padding(.bottom, 30)
    }

    // MARK: - Classification Step

    @ViewBuilder
    private var ocpClassificationContent: some View {
        Text(AppStrings.QuickCount.selectOcpdType)
            .font(.headline)
            .padding(.top, 20)

        ocpClassificationPhotoStrip

        ocpNodeClassPicker

        Spacer()

        ocpDoneButton
    }

    @ViewBuilder
    private var ocpClassificationPhotoStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(photos) { photo in
                    if let uiImage = UIImage(contentsOfFile: photo.localFileURL.path) {
                        Image(uiImage: uiImage)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 50, height: 50)
                            .clipped()
                            .cornerRadius(6)
                    }
                }
            }
            .padding(.horizontal)
        }
        .frame(height: 60)
        .background(Color(UIColor.systemGray6))
    }

    @ViewBuilder
    private var ocpNodeClassPicker: some View {
        VStack(spacing: 16) {
            // OCPD Type picker
            VStack(alignment: .leading, spacing: 8) {
                Text(AppStrings.QuickCount.ocpdType)
                    .font(.caption)
                    .foregroundColor(.secondary)

                Menu {
                    ForEach(ocpNodeClasses) { nodeClass in
                        Button {
                            selectedNodeClass = nodeClass
                            selectedNodeSubtype = nil  // Clear subtype when class changes
                        } label: {
                            HStack {
                                Text(nodeClass.name)
                                if selectedNodeClass?.id == nodeClass.id {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                } label: {
                    HStack {
                        Text(selectedNodeClass?.name ?? AppStrings.QuickCount.selectOcpdType)
                            .foregroundColor(selectedNodeClass == nil ? .secondary : .primary)
                        Spacer()
                        Image(systemName: "chevron.down")
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(10)
                }
            }

            // Subtype picker (only shown if subtypes exist for selected class)
            if !filteredSubtypes.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text(AppStrings.QuickCount.subtypeOptional)
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Menu {
                        Button {
                            selectedNodeSubtype = nil
                        } label: {
                            HStack {
                                Text(AppStrings.Common.none)
                                if selectedNodeSubtype == nil {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }

                        ForEach(filteredSubtypes) { subtype in
                            Button {
                                selectedNodeSubtype = subtype
                            } label: {
                                HStack {
                                    Text(subtype.name)
                                    if selectedNodeSubtype?.id == subtype.id {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack {
                            Text(selectedNodeSubtype?.name ?? AppStrings.QuickCount.selectSubtype)
                                .foregroundColor(selectedNodeSubtype == nil ? .secondary : .primary)
                            Spacer()
                            Image(systemName: "chevron.down")
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color(UIColor.systemGray6))
                        .cornerRadius(10)
                    }
                }
            }
        }
        .padding(.horizontal)
    }

    @ViewBuilder
    private var ocpDoneButton: some View {
        Button {
            if let nodeClass = selectedNodeClass {
                let ocpAsset = QuickCountOCPAsset(
                    nodeClass: nodeClass,
                    nodeSubtype: selectedNodeSubtype,
                    photos: photos
                )
                onComplete(ocpAsset)
            }
        } label: {
            Text(AppStrings.Common.done)
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(selectedNodeClass == nil ? Color.gray : Color.blue)
                .cornerRadius(12)
        }
        .disabled(selectedNodeClass == nil)
        .padding(.horizontal)
        .padding(.bottom, 30)
    }

    private func handleNewPhoto(image: UIImage, fromGallery: Bool) {
        // ZP-2230: capture-and-persist atomically (Photos library +
        // Documents/photos). OCP captures are pre-entity — the
        // eventual OCPD node doesn't exist yet — so stamp falls back
        // to the chosen node class name (or generic OCPD label) plus
        // the photoset id for grouping in the Photos library.
        // ZP-2404: gallery imports skip the audit-copy save (the
        // original already lives in the user's library).
        let label: String
        if let cls = selectedNodeClass {
            label = "Pending OCPD · \(cls.name) #\(ocpdIndex)"
        } else {
            label = "Pending OCPD #\(ocpdIndex)"
        }
        let ctx = PhotoStampContext.pending(
            kind: "Pending OCPD",
            label: label,
            photoType: activePhotoType,
            photosetId: photosetId,
            sld: sld ?? room?.floor?.building?.sld,
            building: room?.floor?.building,
            floor: room?.floor,
            room: room
        )
        Task {
            do {
                let photo = try await PhotoCaptureService.captureAndPersistPending(
                    image: image,
                    context: ctx,
                    saveAuditCopy: !fromGallery
                )
                photos.append(QuickCountStagedPhoto(
                    filename: photo.filename ?? "",
                    localFilepath: photo.local_filepath ?? "",
                    type: activePhotoType
                ))
            } catch {
                AppLogger.log(.error,
                    "[PhotoCapture/ocp-quickcount] capture failed: \(error.localizedDescription)",
                    category: .ui)
                captureErrorMessage = error.localizedDescription
                showCaptureError = true
            }
        }
    }
}

// Helper view for OCP photo thumbnail
struct OCPPhotoThumbnail: View {
    let photo: QuickCountStagedPhoto
    let onDelete: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            if let uiImage = UIImage(contentsOfFile: photo.localFileURL.path) {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 80, height: 80)
                    .clipped()
                    .cornerRadius(8)
            }

            Button(action: onDelete) {
                Image(systemName: "xmark.circle.fill")
                    .font(.caption)
                    .foregroundColor(.white)
                    .background(Color.red)
                    .clipShape(Circle())
            }
            .offset(x: 4, y: -4)
        }
    }
}

// MARK: - Quantity Selector

struct QuickCountQuantitySelector: View {
    @Binding var value: Int
    var onLongPress: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button {
                if value > 1 {
                    value -= 1
                }
            } label: {
                Image(systemName: "minus.circle.fill")
                    .foregroundColor(value <= 1 ? .gray : .red)
                    .font(.title3)
            }
            .buttonStyle(.plain)
            .disabled(value <= 1)

            Text("\(value)")
                .font(.headline)
                .monospacedDigit()
                .frame(minWidth: 40, minHeight: 40)
                .contentShape(Rectangle())
                .onLongPressGesture(minimumDuration: 0.5) {
                    let generator = UIImpactFeedbackGenerator(style: .medium)
                    generator.impactOccurred()
                    onLongPress()
                }

            Button {
                value += 1
            } label: {
                Image(systemName: "plus.circle.fill")
                    .foregroundColor(.green)
                    .font(.title3)
            }
            .buttonStyle(.plain)
        }
    }
}

// MARK: - Subtype Selection Sheet

struct SubtypeSelectionSheet: View {
    let nodeClass: NodeClass
    let subtypes: [NodeSubtype]
    let onSelect: (NodeSubtype) -> Void
    let onSkip: () -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 8) {
                    Image(systemName: "tag.fill")
                        .font(.system(size: 40))
                        .foregroundColor(.blue)

                    Text(AppStrings.QuickCount.selectSubtype)
                        .font(.title2)
                        .fontWeight(.bold)

                    Text(AppStrings.QuickCount.chooseSubtype(nodeClass.name))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 20)

                // Subtype list
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(subtypes) { subtype in
                            Button {
                                onSelect(subtype)
                            } label: {
                                HStack {
                                    Text(subtype.name)
                                        .font(.body)
                                        .foregroundColor(.primary)
                                        .multilineTextAlignment(.leading)
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding()
                                .background(Color(UIColor.systemGray6))
                                .cornerRadius(10)
                            }
                        }
                    }
                    .padding(.horizontal)
                }
                .scrollDismissesKeyboard(.interactively)

                Spacer()

                // Skip button
                Button(action: onSkip) {
                    Text(AppStrings.QuickCount.skipNoSubtype)
                        .font(.headline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(UIColor.systemGray5))
                        .cornerRadius(12)
                }
                .padding(.horizontal)
                .padding(.bottom, 20)
            }
            .navigationTitle(AppStrings.QuickCount.selectSubtype)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        onCancel()
                    }
                }
            }
        }
    }
}

// MARK: - OCPD Count Flow View (add OCPDs by count, no photos)

struct OCPDCountFlowView: View {
    let ocpNodeClasses: [NodeClass]
    let allNodeSubtypes: [NodeSubtype]
    let onComplete: ([QuickCountOCPCountEntry]) -> Void
    let onCancel: () -> Void
    // ZP-2230: room + sld for photo-stamp location breadcrumb.
    var room: Room? = nil
    var sld: SLDV2? = nil

    @State private var entries: [QuickCountOCPCountEntry] = []
    @State private var showAddOCPDMenu = false
    @State private var pendingNodeClass: NodeClass? = nil
    @State private var showSubtypeSelection = false
    @State private var expandedEntryIds: Set<UUID> = []
    @State private var capturingForEntryId: UUID? = nil
    @State private var capturingForInstanceIndex: Int = 0

    // Get subtypes for a specific node class
    private func subtypesForClass(_ nodeClass: NodeClass) -> [NodeSubtype] {
        allNodeSubtypes
            .filter { !$0.is_deleted && $0.node_class_id == nodeClass.id }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var totalOCPDCount: Int {
        entries.reduce(0) { $0 + $1.quantity }
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 8) {
                    Image(systemName: "number.circle.fill")
                        .font(.system(size: 40))
                        .foregroundColor(.blue)

                    Text(AppStrings.QuickCount.addOcpdsByCount)
                        .font(.title2)
                        .fontWeight(.bold)

                    Text(AppStrings.QuickCount.addOcpdByCountSubtitle)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 20)

                if entries.isEmpty {
                    // Empty state
                    VStack(spacing: 12) {
                        Image(systemName: "bolt.shield")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                        Text(AppStrings.QuickCount.noOcpdsAddedYet)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxHeight: .infinity)
                } else {
                    // Entries list
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(entries.indices, id: \.self) { index in
                                OCPDCountEntryRow(
                                    entry: $entries[index],
                                    isExpanded: expandedEntryIds.contains(entries[index].id),
                                    onToggleExpand: {
                                        let id = entries[index].id
                                        withAnimation(.easeInOut(duration: 0.2)) {
                                            if expandedEntryIds.contains(id) {
                                                expandedEntryIds.remove(id)
                                            } else {
                                                expandedEntryIds.insert(id)
                                            }
                                        }
                                    },
                                    onAddPhotoset: { instanceIndex in
                                        capturingForEntryId = entries[index].id
                                        capturingForInstanceIndex = instanceIndex
                                    },
                                    onRemovePhotoset: { instanceIndex in
                                        entries[index].instancePhotosets.removeAll { $0.instanceIndex == instanceIndex }
                                    },
                                    onDelete: {
                                        entries.remove(at: index)
                                    }
                                )
                            }
                        }
                        .padding(.horizontal)
                    }
                }

                Spacer()

                // Add OCPD button
                Button(action: { showAddOCPDMenu = true }) {
                    HStack {
                        Image(systemName: "plus.circle.fill")
                        Text(AppStrings.QuickCount.addOcpdType)
                    }
                    .font(.headline)
                    .foregroundColor(.blue)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(12)
                }
                .padding(.horizontal)

                // Done button
                Button {
                    onComplete(entries)
                } label: {
                    Text(AppStrings.QuickCount.doneOcpdCount(totalOCPDCount))
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(entries.isEmpty ? Color.gray : Color.blue)
                        .cornerRadius(12)
                }
                .disabled(entries.isEmpty)
                .padding(.horizontal)
                .padding(.bottom, 20)
            }
            .navigationTitle(AppStrings.QuickCount.addOcpdsByCount)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        onCancel()
                    }
                }
            }
        }
        .confirmationDialog(AppStrings.QuickCount.selectOcpdType, isPresented: $showAddOCPDMenu, titleVisibility: .visible) {
            ForEach(ocpNodeClasses) { nodeClass in
                Button(nodeClass.name) {
                    handleNodeClassSelection(nodeClass)
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        }
        .sheet(isPresented: $showSubtypeSelection) {
            if let nodeClass = pendingNodeClass {
                SubtypeSelectionSheet(
                    nodeClass: nodeClass,
                    subtypes: subtypesForClass(nodeClass),
                    onSelect: { subtype in
                        addEntry(nodeClass: nodeClass, subtype: subtype)
                        showSubtypeSelection = false
                        pendingNodeClass = nil
                    },
                    onSkip: {
                        addEntry(nodeClass: nodeClass, subtype: nil)
                        showSubtypeSelection = false
                        pendingNodeClass = nil
                    },
                    onCancel: {
                        showSubtypeSelection = false
                        pendingNodeClass = nil
                    }
                )
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { capturingForEntryId != nil },
            set: { if !$0 { capturingForEntryId = nil } }
        )) {
            if let entryId = capturingForEntryId,
               let entry = entries.first(where: { $0.id == entryId }) {
                PhotoCaptureFlowView(
                    assetLabel: "\(entry.nodeClass.name) \(capturingForInstanceIndex)",
                    onComplete: { photos in
                        handlePhotoCaptureComplete(entryId: entryId, instanceIndex: capturingForInstanceIndex, photos: photos)
                    },
                    onCancel: {
                        capturingForEntryId = nil
                    },
                    room: room,
                    sld: sld
                )
            }
        }
    }

    private func handleNodeClassSelection(_ nodeClass: NodeClass) {
        let subtypes = subtypesForClass(nodeClass)
        if subtypes.isEmpty {
            addEntry(nodeClass: nodeClass, subtype: nil)
        } else {
            pendingNodeClass = nodeClass
            showSubtypeSelection = true
        }
    }

    private func addEntry(nodeClass: NodeClass, subtype: NodeSubtype?) {
        let entry = QuickCountOCPCountEntry(nodeClass: nodeClass, nodeSubtype: subtype)
        entries.append(entry)
        expandedEntryIds.insert(entry.id)
    }

    private func handlePhotoCaptureComplete(entryId: UUID, instanceIndex: Int, photos: [QuickCountStagedPhoto]) {
        defer { capturingForEntryId = nil }
        guard !photos.isEmpty,
              let idx = entries.firstIndex(where: { $0.id == entryId }) else { return }
        entries[idx].instancePhotosets.removeAll { $0.instanceIndex == instanceIndex }
        entries[idx].instancePhotosets.append(
            QuickCountOCPCountInstancePhotoset(instanceIndex: instanceIndex, photos: photos)
        )
    }
}

// MARK: - OCPD Count Entry Row

struct OCPDCountEntryRow: View {
    @Binding var entry: QuickCountOCPCountEntry
    let isExpanded: Bool
    let onToggleExpand: () -> Void
    let onAddPhotoset: (Int) -> Void
    let onRemovePhotoset: (Int) -> Void
    let onDelete: () -> Void

    @State private var showQuantityInput = false
    @State private var quantityInputText = ""

    private var totalInstancePhotos: Int {
        entry.instancePhotosets.reduce(0) { $0 + $1.photos.count }
    }

    private func pruneStalePhotosets() {
        entry.instancePhotosets.removeAll { $0.instanceIndex > entry.quantity }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header row (always visible)
            HStack {
                Button(action: onToggleExpand) {
                    Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(isExpanded ? .blue : .secondary)
                        .frame(width: 24, height: 24)
                }
                .buttonStyle(.plain)

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 4) {
                        Text(entry.nodeClass.name)
                            .font(.headline)
                        if let subtype = entry.nodeSubtype {
                            Text("(\(subtype.name))")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    if !isExpanded && totalInstancePhotos > 0 {
                        Text(AppStrings.QuickCount.photosCount(totalInstancePhotos))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                // Quantity controls
                HStack(spacing: 12) {
                    Button {
                        if entry.quantity > 1 {
                            entry.quantity -= 1
                            pruneStalePhotosets()
                        }
                    } label: {
                        Image(systemName: "minus.circle.fill")
                            .foregroundColor(entry.quantity <= 1 ? .gray : .red)
                            .font(.title3)
                    }
                    .buttonStyle(.plain)
                    .disabled(entry.quantity <= 1)

                    Text("\(entry.quantity)")
                        .font(.headline)
                        .monospacedDigit()
                        .frame(minWidth: 40, minHeight: 40)
                        .contentShape(Rectangle())
                        .onLongPressGesture(minimumDuration: 0.5) {
                            let generator = UIImpactFeedbackGenerator(style: .medium)
                            generator.impactOccurred()
                            quantityInputText = "\(entry.quantity)"
                            showQuantityInput = true
                        }

                    Button {
                        entry.quantity += 1
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .foregroundColor(.green)
                            .font(.title3)
                    }
                    .buttonStyle(.plain)
                }

                // Delete button
                Button(action: onDelete) {
                    Image(systemName: "trash")
                        .foregroundColor(.red)
                        .frame(width: 30, height: 30)
                }
                .buttonStyle(.plain)
            }

            if isExpanded {
                VStack(alignment: .leading, spacing: 8) {
                    Text(AppStrings.QuickCount.photosets)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .padding(.leading, 4)

                    ForEach(
                        entry.instancePhotosets
                            .filter { (1...entry.quantity).contains($0.instanceIndex) }
                            .sorted(by: { $0.instanceIndex < $1.instanceIndex })
                    ) { ps in
                        CountInstancePhotosetCard(
                            className: entry.nodeClass.name,
                            instanceIndex: ps.instanceIndex,
                            photoset: ps,
                            onDelete: { onRemovePhotoset(ps.instanceIndex) }
                        )
                    }

                    if !entry.allInstancesHavePhotosets {
                        Button {
                            onAddPhotoset(entry.nextInstanceIndex)
                        } label: {
                            HStack {
                                Image(systemName: "camera.fill")
                                Text(AppStrings.QuickCount.addPhotosetFor(entry.nodeClass.name, entry.nextInstanceIndex))
                            }
                            .font(.subheadline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.blue)
                            .cornerRadius(8)
                        }
                        .buttonStyle(.plain)
                    } else {
                        HStack(spacing: 4) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                                .font(.caption)
                            Text(AppStrings.QuickCount.allInstancesHavePhotosets(entry.quantity))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding(.leading, 4)
                    }
                }
                .padding(.top, 12)
            }
        }
        .padding()
        .background(Color(UIColor.systemGray6))
        .cornerRadius(10)
        .alert(AppStrings.QuickCount.enterQuantity, isPresented: $showQuantityInput) {
            TextField(AppStrings.QuickCount.quantity, text: $quantityInputText)
                .keyboardType(.numberPad)
            Button(AppStrings.Common.ok) {
                if let newValue = Int(quantityInputText), newValue >= 1 {
                    entry.quantity = newValue
                    pruneStalePhotosets()
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        }
    }
}

// MARK: - Count Instance Photoset Card

struct CountInstancePhotosetCard: View {
    let className: String
    let instanceIndex: Int
    let photoset: QuickCountOCPCountInstancePhotoset
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
                    .font(.caption)
                Text(AppStrings.QuickCount.photosForInstance(photoset.photos.count, className, instanceIndex))
                    .font(.caption)
                    .foregroundColor(.primary)
                Spacer()
                Button(action: onDelete) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                        .font(.caption)
                }
                .buttonStyle(.plain)
            }

            CategorizedStagedPhotoStrip(photos: photoset.photos)
        }
        .padding(10)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(8)
    }
}
