//
//  PhotoWalkthroughView.swift
//  Egalvanic PZ
//
//  Camera-first bulk asset creation with 1:1 photo-to-asset mapping
//

import SwiftUI
import SwiftData
import PhotosUI

// MARK: - Data Models

struct WalkthroughStagedPhoto: Identifiable, CategorizedStagedPhoto {
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

struct WalkthroughChildAsset: Identifiable {
    let id = UUID()
    var nodeClass: NodeClass
    var nodeSubtype: NodeSubtype?
    var photos: [WalkthroughStagedPhoto]
    var draftAttributes: [UUID: String] = [:]
    var label: String? = nil
    var com: Int? = 1
    var comCalculation: COMCalculation? = nil
    var serviceability: Serviceability? = .serviceable
    var serviceabilityNote: String? = nil
}

struct WalkthroughParentAsset: Identifiable {
    let id = UUID()
    var nodeClass: NodeClass
    var nodeSubtype: NodeSubtype?
    var photos: [WalkthroughStagedPhoto]
    var children: [WalkthroughChildAsset]
    var draftAttributes: [UUID: String] = [:]
    var label: String? = nil
    var com: Int? = 1
    var comCalculation: COMCalculation? = nil
    var serviceability: Serviceability? = .serviceable
    var serviceabilityNote: String? = nil
}

// MARK: - State Machine

enum WalkthroughState {
    case capturingParentPhotos   // Camera open, taking photos of parent
    case classifyingParent       // Select NodeClass/Subtype for parent
    case promptingForChildren    // "Add OCPD photos?" (only if box == true)
    case capturingChildPhotos    // Camera open for OCPD
    case classifyingChild        // Select NodeClass/Subtype for OCPD
    case decidingChildAction     // "Add Another OCPD" or "Done with OCP"
    case decidingNextAction      // "Add Another Asset" or "Finish"
    case reviewing               // Review all captured assets before creation
    case creating                // Creating assets (show progress)
}

// MARK: - Photo Walkthrough View

struct PhotoWalkthroughView: View {
    // ZP-2331: session optional so the non-WO Locations tree can run
    // Photo Walkthrough too. nil → assets attach only to the room; no
    // nodeSession mapping is enqueued and the SLD is sourced from the
    // room hierarchy.
    let session: IRSession?
    let room: Room
    let onComplete: () -> Void
    let onCancel: () -> Void

    /// SLD resolution: prefer the session's SLD when there is one,
    /// otherwise walk the room → floor → building → sld chain.
    private var contextSLD: SLDV2? {
        session?.sld ?? room.floor?.building?.sld
    }

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    // Query all node classes
    @Query(sort: \NodeClass.name) private var allNodeClasses: [NodeClass]

    // State machine
    @State private var currentState: WalkthroughState = .capturingParentPhotos

    // Current work-in-progress photos
    @State private var currentPhotos: [WalkthroughStagedPhoto] = []
    @State private var activePhotoType: String = PhotoCategory.defaultNodeType
    @State private var selectedNodeClass: NodeClass? = nil
    @State private var selectedNodeSubtype: NodeSubtype? = nil
    @State private var draftCoreAttributes: [UUID: String] = [:]
    @State private var assetLabel: String = ""
    @State private var draftCOM: Int? = 1
    @State private var draftCOMCalculation: COMCalculation? = nil
    @State private var draftServiceability: Serviceability? = nil
    @State private var draftServiceabilityNote: String = ""
    @State private var showCOMCalculator = false
    @State private var showSubtypePicker = false

    // Shared voice input for classification view
    @State private var speechManager = SpeechRecognitionManager()
    // nil = asset label field, UUID = specific attribute field
    @State private var voiceTargetField: UUID? = nil
    @State private var voiceTargetIsLabel: Bool = false

    // Accumulated assets
    @State private var parentAssets: [WalkthroughParentAsset] = []
    @State private var currentParent: WalkthroughParentAsset? = nil

    // Camera state
    @State private var showingCamera = false
    @State private var capturedImage: UIImage? = nil
    @State private var selectedPhotoItem: PhotosPickerItem? = nil

    // Progress/alerts
    @State private var createProgress = 0
    @State private var totalToCreate = 0
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var showSuccessAlert = false
    @State private var createdCount = 0
    @State private var showDiscardChangesAlert = false
    @State private var isKeyboardVisible = false

    // ZP-2230: stable id for the CURRENT parent device. Regenerated
    // whenever we start capturing a new parent so each device is its
    // own photoset (xxx). OCP children captured under the parent
    // share the parent's id with a sub-index suffix (xxx-1, xxx-2)
    // so the audit copy in the Photos library can be grouped both
    // ways: by parent device and by individual OCPD.
    @State private var photosetId = UUID()
    @State private var childIndexInCurrentParent: Int = 0
    @State private var showCaptureError = false
    @State private var captureErrorMessage = ""
    @State private var showPermissionPrompt = false

    // Computed properties
    private var availableNodeClasses: [NodeClass] {
        allNodeClasses.filter { !$0.is_deleted }
    }

    private var ocpNodeClasses: [NodeClass] {
        allNodeClasses.filter { !$0.is_deleted && $0.ocp }
    }

    private var filteredSubtypes: [NodeSubtype] {
        selectedNodeClass?.node_subtypes?.filter { !$0.is_deleted } ?? []
    }

    private var totalAssetCount: Int {
        parentAssets.reduce(0) { sum, parent in
            sum + 1 + parent.children.count
        }
    }

    private var totalPhotoCount: Int {
        parentAssets.reduce(0) { sum, parent in
            let parentPhotos = parent.photos.count
            let childPhotos = parent.children.reduce(0) { $0 + $1.photos.count }
            return sum + parentPhotos + childPhotos
        }
    }

    private var hasUnsavedData: Bool {
        !parentAssets.isEmpty || !currentPhotos.isEmpty
    }

    var body: some View {
        NavigationView {
            Group {
                switch currentState {
                case .capturingParentPhotos:
                    photoCaptureView(title: "Photograph Asset", isChild: false)
                case .classifyingParent:
                    classificationView(isChild: false)
                case .promptingForChildren:
                    childPromptView
                case .capturingChildPhotos:
                    photoCaptureView(title: "Photograph Sub-Component", isChild: true)
                case .classifyingChild:
                    classificationView(isChild: true)
                case .decidingChildAction:
                    childDecisionView
                case .decidingNextAction:
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
                            showDiscardChangesAlert = true
                        } else {
                            onCancel()
                        }
                    }
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
        .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
            Button(AppStrings.Common.ok, role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert(AppStrings.QuickCount.success, isPresented: $showSuccessAlert) {
            Button(AppStrings.Common.done) {
                onComplete()
            }
        } message: {
            Text(AppStrings.PhotoWalkthrough.createdAssetsWithPhotos(createdCount, totalPhotoCount))
        }
        .alert(AppStrings.Alerts.discardChanges, isPresented: $showDiscardChangesAlert) {
            Button(AppStrings.Common.keepEditing, role: .cancel) { }
            Button(AppStrings.Common.discard, role: .destructive) {
                onCancel()
            }
        } message: {
            Text(AppStrings.PhotoWalkthrough.discardAssetsPhotosMessage)
        }
        // ZP-2230: photo-capture failure alerts.
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
        .sheet(isPresented: $showCOMCalculator) {
            COMCalculatorView(initialCalculation: draftCOMCalculation) { rating, calculation in
                draftCOM = rating
                draftCOMCalculation = calculation
                if calculation.isNonserviceable {
                    draftServiceability = .nonServiceable
                } else {
                    draftServiceability = .serviceable
                    draftServiceabilityNote = ""
                }
            }
        }
        .sheet(isPresented: $showSubtypePicker) {
            NavigationStack {
                List {
                    Button {
                        selectedNodeSubtype = nil
                        showSubtypePicker = false
                    } label: {
                        HStack {
                            Text("None")
                                .foregroundColor(.primary)
                            Spacer()
                            if selectedNodeSubtype == nil {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                            }
                        }
                    }

                    ForEach(filteredSubtypes) { subtype in
                        Button {
                            selectedNodeSubtype = subtype
                            showSubtypePicker = false
                        } label: {
                            HStack {
                                Text(subtype.name)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedNodeSubtype?.id == subtype.id {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
                .navigationTitle(AppStrings.Issues.selectSubtype)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button(AppStrings.Common.done) {
                            showSubtypePicker = false
                        }
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
    }

    private var navigationTitle: String {
        switch currentState {
        case .capturingParentPhotos:
            return AppStrings.PhotoWalkthrough.photoWalkthrough
        case .classifyingParent:
            return AppStrings.PhotoWalkthrough.classifyAsset
        case .promptingForChildren:
            return "Add Sub-Components?"
        case .capturingChildPhotos:
            return "Photograph Sub-Component"
        case .classifyingChild:
            return "Classify Sub-Component"
        case .decidingChildAction:
            return "More Sub-Components?"
        case .decidingNextAction:
            return AppStrings.PhotoWalkthrough.whatsNext
        case .reviewing:
            return AppStrings.PhotoWalkthrough.reviewAssets
        case .creating:
            return AppStrings.PhotoWalkthrough.creating
        }
    }

    // MARK: - Photo Capture View

    @ViewBuilder
    private func photoCaptureView(title: String, isChild: Bool) -> some View {
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

            // Instructions
            Text(isChild ? "Take photos of the \(ordinalName(for: (currentParent?.children.count ?? 0) + 1)) sub-component" : "Take photos of the asset")
                .font(.headline)
                .padding(.top, 20)

            Text(AppStrings.PhotoWalkthrough.canTakeMultiplePhotos)
                .font(.subheadline)
                .foregroundColor(.secondary)

            // Photo category tabs
            let photosByType = Dictionary(grouping: currentPhotos, by: { $0.type })
            let countByType = photosByType.mapValues { $0.count }
            let activePhotos = photosByType[activePhotoType] ?? []

            PhotoCategoryChipsRow(
                activePhotoType: $activePhotoType,
                countByType: countByType
            )
            .padding(.top, 12)

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
                .padding(.top, 12)
            } else {
                // Empty state
                VStack(spacing: 12) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.secondary)
                    Text(AppStrings.PhotoWalkthrough.noPhotosYet)
                        .foregroundColor(.secondary)
                }
                .frame(height: 130)
                .padding(.top, 24)
            }

            Spacer()

            // Camera controls
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
                        // ZP-2230: require Photos write access — without
                        // it we can't durably stash captured photos
                        // off-app, so the walkthrough refuses to open
                        // the camera.
                        Task {
                            let granted = await PhotoCaptureService.ensureLibraryWriteAccess()
                            if granted {
                                showingCamera = true
                            } else {
                                showPermissionPrompt = true
                            }
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
                    advanceFromPhotoCapture(isChild: isChild)
                } label: {
                    Text(isChild ? "Done with this sub-component" : "Done with this asset")
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
            ) {
                EmptyView()
            }
            .hidden()
        )
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
    }

    // MARK: - Classification View

    private var requiredAttributes: [NodeClassProperty] {
        selectedNodeClass?.definition.filter { $0.af_required } ?? []
    }

    /// Start (or switch) voice recording to a specific field
    private func activateVoice(forLabel: Bool = false, forAttribute attributeId: UUID? = nil) {
        let wasRecording = speechManager.isRecording

        // Stop any current recording
        if wasRecording {
            speechManager.stopRecording()
        }

        // Request authorization if needed (non-blocking — startRecording will
        // fail gracefully if not yet authorized, and next tap will work)
        if !speechManager.isAuthorized {
            speechManager.requestAuthorization()
        }

        // Set the target first so onChange handlers are ready
        voiceTargetIsLabel = forLabel
        voiceTargetField = attributeId

        // Clear previous transcription so the new field starts fresh
        speechManager.transcribedText = ""

        if wasRecording {
            // Brief delay for audio engine teardown before restarting
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                speechManager.startRecording()
            }
        } else {
            speechManager.startRecording()
        }
    }

    @ViewBuilder
    private func classificationView(isChild: Bool) -> some View {
        VStack(spacing: 0) {
            // Photo thumbnails strip
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
                    // NodeClass picker
                    VStack(alignment: .leading, spacing: 8) {
                        Text(AppStrings.Assets.assetType)
                            .font(.headline)

                        Menu {
                            let classes = isChild ? ocpNodeClasses : availableNodeClasses
                            ForEach(classes) { nodeClass in
                                Button {
                                    selectedNodeClass = nodeClass
                                    selectedNodeSubtype = nil
                                    draftCoreAttributes = [:]
                                    // Apply class-defined defaults (ZP-2251)
                                    // for the newly picked class.
                                    CoreAttributesService.applyDefaultValues(from: nodeClass, into: &draftCoreAttributes)
                                    assetLabel = ""
                                    draftCOM = 1
                                    draftCOMCalculation = nil
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
                                Text(selectedNodeClass?.name ?? "Select Asset Type")
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

                    // NodeSubtype picker (if subtypes exist)
                    if !filteredSubtypes.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(AppStrings.PhotoWalkthrough.subtypeOptional)
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

                    // Asset name (voice-to-text enabled)
                    if selectedNodeClass != nil {
                        SpeechTextField(
                            title: "Asset Name (Optional)",
                            text: $assetLabel,
                            icon: "tag",
                            sharedSpeechManager: speechManager,
                            isVoiceTarget: voiceTargetIsLabel,
                            onMicTapped: { activateVoice(forLabel: true) }
                        )

                        // Required core attributes
                        if !requiredAttributes.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                Text(AppStrings.Assets.requiredFields)
                                    .font(.headline)

                                ForEach(requiredAttributes) { prop in
                                    VoiceAttributeRowView(
                                        classProperty: prop,
                                        currentValue: draftCoreAttributes[prop.id] ?? "",
                                        onValueChange: { newValue in
                                            draftCoreAttributes[prop.id] = newValue
                                        },
                                        speechManager: speechManager,
                                        isVoiceTarget: voiceTargetField == prop.id
                                    ) {
                                        activateVoice(forAttribute: prop.id)
                                    }
                                }
                            }
                        }

                        // Condition of Maintenance
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(AppStrings.Assets.conditionOfMaintenance)
                                    .font(.caption)
                                    .foregroundColor(.secondary)

                                Spacer()

                                Button(action: { showCOMCalculator = true }) {
                                    Text(AppStrings.Assets.calculator)
                                        .font(.caption)
                                        .foregroundColor(.blue)
                                }
                            }

                            ZStack {
                                HStack(spacing: 0) {
                                    ForEach([1, 2, 3], id: \.self) { level in
                                        let isSelected = draftCOM == level
                                        let isDisabled = draftCOM == nil
                                        let color: Color = level == 1 ? .green : level == 2 ? .yellow : .red

                                        Button(action: {
                                            guard !isDisabled else { return }
                                            withAnimation(.easeInOut(duration: 0.2)) {
                                                draftCOM = level
                                                draftCOMCalculation = nil
                                                draftServiceability = .serviceable
                                                draftServiceabilityNote = ""
                                            }
                                        }) {
                                            VStack(spacing: 4) {
                                                Circle()
                                                    .fill(color)
                                                    .frame(width: 30, height: 30)
                                                    .overlay(
                                                        Circle()
                                                            .stroke(Color.white, lineWidth: isSelected ? 2 : 0)
                                                    )

                                                Text("\(level)")
                                                    .font(.caption2)
                                                    .foregroundColor(isSelected ? .primary : .secondary)
                                            }
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 8)
                                            .background(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .fill(isSelected ? Color(.systemGray5) : Color.clear)
                                            )
                                        }
                                        .buttonStyle(PlainButtonStyle())
                                    }
                                }
                                .padding(4)
                                .background(Color(.systemGray6))
                                .cornerRadius(10)
                                .opacity(draftCOM == nil ? 0.3 : 1)

                                if draftCOM == nil {
                                    Text(AppStrings.AssetsExtra.nonserviceable)
                                        .font(.subheadline)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 8)
                                        .background(Color(.darkGray))
                                        .cornerRadius(8)
                                }
                            }
                        }

                        // Serviceability label (when nonserviceable)
                        if draftServiceability == .nonServiceable {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack(spacing: 3) {
                                    Text(AppStrings.AssetsExtra.serviceabilityLabelPrefix)
                                        .font(.caption)
                                        .fontWeight(.medium)

                                    Text(Serviceability.nonServiceable.displayName)
                                        .font(.caption)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.red)

                                    Text(AppStrings.AssetsExtra.serviceabilitySetByCOM)
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }

                                VStack(alignment: .leading, spacing: 4) {
                                    Text(AppStrings.AssetsExtra.serviceabilityNote)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    TextEditor(text: $draftServiceabilityNote)
                                        .frame(minHeight: 60, maxHeight: 120)
                                        .padding(8)
                                        .background(Color(.systemGray6))
                                        .cornerRadius(10)
                                        .overlay(
                                            Group {
                                                if draftServiceabilityNote.isEmpty {
                                                    Text(AppStrings.AssetsExtra.explainNotServiceable)
                                                        .foregroundColor(.secondary.opacity(0.5))
                                                        .padding(.horizontal, 12)
                                                        .padding(.vertical, 12)
                                                }
                                            },
                                            alignment: .topLeading
                                        )
                                }
                            }
                        }
                    }
                }
                .padding()
            }
            .scrollDismissesKeyboard(.interactively)

            // Continue button pinned at bottom
            Button {
                if speechManager.isRecording { speechManager.stopRecording() }
                advanceFromClassification(isChild: isChild)
            } label: {
                Text(AppStrings.Common.continueAction)
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background((selectedNodeClass == nil || isKeyboardVisible) ? Color.gray : Color.blue)
                    .cornerRadius(12)
            }
            .disabled(selectedNodeClass == nil || isKeyboardVisible)
            .padding()
        }
        .onAppear {
            speechManager.requestAuthorization()
        }
        .onDisappear {
            if speechManager.isRecording { speechManager.stopRecording() }
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in
            isKeyboardVisible = true
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
            isKeyboardVisible = false
        }
    }

    // MARK: - Child Prompt View

    private var childPromptView: some View {
        VStack(spacing: 30) {
            Spacer()

            Image(systemName: "bolt.shield.fill")
                .font(.system(size: 60))
                .foregroundColor(.orange)

            Text(AppStrings.PhotoWalkthrough.addSubComponents)
                .font(.title2)
                .fontWeight(.semibold)

            Text(AppStrings.PhotoWalkthrough.subComponentsDescription)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Spacer()

            VStack(spacing: 12) {
                Button {
                    // Start capturing sub-component photos
                    currentPhotos = []
                    activePhotoType = PhotoCategory.defaultNodeType
                    selectedNodeClass = nil
                    selectedNodeSubtype = nil
                    // ZP-2230: bump the OCP sub-index for the current parent.
                    childIndexInCurrentParent += 1
                    currentState = .capturingChildPhotos
                } label: {
                    HStack {
                        Image(systemName: "camera.fill")
                        Text(AppStrings.PhotoWalkthrough.yesAddPhotos)
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.orange)
                    .cornerRadius(12)
                }

                Button {
                    // Skip sub-components, go to decision
                    currentState = .decidingNextAction
                } label: {
                    Text(AppStrings.PhotoWalkthrough.noSkip)
                        .font(.headline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(UIColor.systemGray5))
                        .cornerRadius(12)
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 30)
        }
    }

    // MARK: - Child Decision View

    private var childDecisionView: some View {
        VStack(spacing: 30) {
            Spacer()

            // Current parent summary
            if let parent = currentParent {
                VStack(spacing: 8) {
                    Text(parent.label ?? getLabel(for: parent))
                        .font(.title3)
                        .fontWeight(.semibold)

                    Text("\(parent.children.count) sub-component\(parent.children.count == 1 ? "" : "s") added")
                        .font(.headline)
                        .foregroundColor(.orange)
                }
            }

            Spacer()

            VStack(spacing: 12) {
                Button {
                    // Add another sub-component
                    currentPhotos = []
                    activePhotoType = PhotoCategory.defaultNodeType
                    selectedNodeClass = nil
                    selectedNodeSubtype = nil
                    // ZP-2230: bump the OCP sub-index for the current parent.
                    childIndexInCurrentParent += 1
                    currentState = .capturingChildPhotos
                } label: {
                    HStack {
                        Image(systemName: "plus.circle.fill")
                        Text(AppStrings.PhotoWalkthrough.addAnotherSubComponent)
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.orange)
                    .cornerRadius(12)
                }

                Button {
                    // Done with sub-components, move to next action decision
                    currentState = .decidingNextAction
                } label: {
                    Text(AppStrings.PhotoWalkthrough.doneWithSubComponents)
                        .font(.headline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(UIColor.systemGray5))
                        .cornerRadius(12)
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 30)
        }
    }

    // MARK: - Decision View

    private var decisionView: some View {
        VStack(spacing: 30) {
            Spacer()

            // Summary
            VStack(spacing: 8) {
                Text("\(totalAssetCount)")
                    .font(.system(size: 60, weight: .bold))
                    .foregroundColor(.blue)
                Text(totalAssetCount == 1 ? AppStrings.PhotoWalkthrough.assetCaptured : AppStrings.PhotoWalkthrough.assetsCaptured)
                    .font(.headline)
                    .foregroundColor(.secondary)

                if totalPhotoCount > 0 {
                    Text(AppStrings.PhotoWalkthrough.photosTotalCount(totalPhotoCount))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            VStack(spacing: 12) {
                Button {
                    // Add another asset
                    currentPhotos = []
                    activePhotoType = PhotoCategory.defaultNodeType
                    selectedNodeClass = nil
                    selectedNodeSubtype = nil
                    // ZP-2230: fresh photoset id for the new parent
                    // device, reset its child counter.
                    photosetId = UUID()
                    childIndexInCurrentParent = 0
                    currentState = .capturingParentPhotos
                } label: {
                    HStack {
                        Image(systemName: "plus.circle.fill")
                        Text(AppStrings.PhotoWalkthrough.addAnotherAsset)
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.green)
                    .cornerRadius(12)
                }

                Button {
                    // Go to review
                    currentState = .reviewing
                } label: {
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                        Text(AppStrings.PhotoWalkthrough.finishWalkthrough)
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .cornerRadius(12)
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 30)
        }
    }

    // MARK: - Review View

    private var reviewView: some View {
        VStack(spacing: 0) {
            List {
                ForEach(Array(parentAssets.enumerated()), id: \.element.id) { parentIndex, parent in
                    Section {
                        // Parent row
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(parent.label ?? getLabel(for: parent))
                                    .font(.headline)
                                if let subtype = parent.nodeSubtype {
                                    Text(subtype.name)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                if let breakdown = photoCategoryBreakdown(for: parent.photos) {
                                    Text(breakdown)
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                            }
                            Spacer()
                            Text(AppStrings.PhotoWalkthrough.photosCount(parent.photos.count))
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                parentAssets.remove(at: parentIndex)
                            } label: {
                                Label(AppStrings.Common.delete, systemImage: "trash")
                            }
                        }

                        // Child rows
                        ForEach(Array(parent.children.enumerated()), id: \.element.id) { childIndex, child in
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("  └─ \(child.label ?? getChildLabel(for: child, in: parent))")
                                        .font(.subheadline)
                                    if let breakdown = photoCategoryBreakdown(for: child.photos) {
                                        Text(breakdown)
                                            .font(.caption2)
                                            .foregroundColor(.secondary)
                                            .padding(.leading, 16)
                                    }
                                }
                                Spacer()
                                Text(AppStrings.PhotoWalkthrough.photosCount(child.photos.count))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    parentAssets[parentIndex].children.remove(at: childIndex)
                                } label: {
                                    Label(AppStrings.Common.delete, systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)

            // Bottom bar
            VStack(spacing: 12) {
                HStack {
                    Text(AppStrings.PhotoWalkthrough.assetsCount(totalAssetCount))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text(AppStrings.PhotoWalkthrough.photosCount(totalPhotoCount))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal)

                HStack(spacing: 12) {
                    Button {
                        // Add more assets
                        currentPhotos = []
                        activePhotoType = PhotoCategory.defaultNodeType
                        selectedNodeClass = nil
                        selectedNodeSubtype = nil
                        // ZP-2230: fresh photoset id for the new
                        // parent device.
                        photosetId = UUID()
                        childIndexInCurrentParent = 0
                        currentState = .capturingParentPhotos
                    } label: {
                        Text(AppStrings.PhotoWalkthrough.addMore)
                            .font(.headline)
                            .foregroundColor(.blue)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(12)
                    }

                    Button {
                        Task {
                            await createAllAssets()
                        }
                    } label: {
                        Text(AppStrings.PhotoWalkthrough.createAll(totalAssetCount))
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(parentAssets.isEmpty ? Color.gray : Color.blue)
                            .cornerRadius(12)
                    }
                    .disabled(parentAssets.isEmpty)
                }
                .padding(.horizontal)
            }
            .padding(.vertical)
            .background(Color(UIColor.systemBackground))
        }
    }

    // MARK: - Creating View

    private var creatingView: some View {
        VStack(spacing: 20) {
            Spacer()

            ProgressView()
                .scaleEffect(1.5)

            Text(AppStrings.PhotoWalkthrough.creatingAssets)
                .font(.headline)

            if totalToCreate > 0 {
                Text(AppStrings.PhotoWalkthrough.progressOfTotal(createProgress, totalToCreate))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
    }

    // MARK: - Helper Functions

    private func handleNewPhoto(image: UIImage, fromGallery: Bool) {
        // ZP-2230: route through PhotoCaptureService so the photo
        // lands in the user's Photos library + Documents/photos
        // atomically (or not at all). Stamp uses the walkthrough's
        // pending context — eventual node entities don't exist yet,
        // so the burned-in label is the current parent's asset
        // name (when classified) or "Walkthrough" + photoset id.
        // ZP-2404: gallery imports skip the audit-copy save — the
        // original already lives in the user's library, so a
        // stamped duplicate is redundant and would force an
        // addOnly permission grant.
        // ZP-2230: distinguish parent vs child captures for label +
        // photoset sub-index. Children share the parent's photosetId
        // and append a sub-index (1, 2, …) so the burned-in stamp
        // groups visually as ``set abc12345-2`` while still tracing
        // back to its parent device's ``set abc12345``.
        let isChildCapture = currentState == .capturingChildPhotos
        let label: String
        let subIndex: Int?
        if isChildCapture {
            label = "OCPD #\(childIndexInCurrentParent)"
            subIndex = childIndexInCurrentParent
        } else if let parent = currentParent, let plabel = parent.label, !plabel.isEmpty {
            label = plabel
            subIndex = nil
        } else if !assetLabel.isEmpty {
            label = assetLabel
            subIndex = nil
        } else {
            label = "Walkthrough parent"
            subIndex = nil
        }
        let ctx = PhotoStampContext.pending(
            kind: isChildCapture ? "Walkthrough OCPD" : "Walkthrough",
            label: label,
            photoType: activePhotoType,
            photosetId: photosetId,
            photosetSubIndex: subIndex,
            // ZP-2230: prefer session.sld so the breadcrumb still
            // names the SLD when the room→floor→building chain is
            // incomplete. ZP-2331: falls back to the room-hierarchy
            // SLD when there is no WO context.
            sld: contextSLD,
            building: room.floor?.building,
            floor: room.floor,
            room: room
        )
        Task {
            do {
                let photo = try await PhotoCaptureService.captureAndPersistPending(
                    image: image,
                    context: ctx,
                    saveAuditCopy: !fromGallery
                )
                // Adopt the filename + local path the service produced.
                let stagedPhoto = WalkthroughStagedPhoto(
                    filename: photo.filename ?? "",
                    localFilepath: photo.local_filepath ?? "",
                    type: activePhotoType
                )
                currentPhotos.append(stagedPhoto)
            } catch {
                AppLogger.log(.error,
                    "[PhotoCapture/walkthrough] capture failed: \(error.localizedDescription)",
                    category: .photo)
                captureErrorMessage = error.localizedDescription
                showCaptureError = true
            }
        }
    }

    private func advanceFromPhotoCapture(isChild: Bool) {
        if isChild {
            currentState = .classifyingChild
        } else {
            currentState = .classifyingParent
        }
    }

    private func advanceFromClassification(isChild: Bool) {
        guard let nodeClass = selectedNodeClass else { return }

        let trimmedLabel = assetLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        let savedLabel: String? = trimmedLabel.isEmpty ? nil : trimmedLabel
        let savedAttributes = draftCoreAttributes
        let savedCOM = draftCOM
        let savedCOMCalculation = draftCOMCalculation
        let savedServiceability = draftServiceability
        let savedServiceabilityNote = draftServiceabilityNote.trimmingCharacters(in: .whitespacesAndNewlines)

        if isChild {
            // Create child asset and add to current parent
            let child = WalkthroughChildAsset(
                nodeClass: nodeClass,
                nodeSubtype: selectedNodeSubtype,
                photos: currentPhotos,
                draftAttributes: savedAttributes,
                label: savedLabel,
                // ZP-2230: fall back to 1 (serviceable) when the
                // user never opened the COM calculator. Previous
                // behavior persisted nil, which blocked readiness.
                com: savedCOM ?? 1,
                comCalculation: savedCOMCalculation,
                serviceability: savedServiceability,
                serviceabilityNote: savedServiceabilityNote.isEmpty ? nil : savedServiceabilityNote
            )

            // Find and update the parent in the array directly
            if let parentId = currentParent?.id,
               let index = parentAssets.firstIndex(where: { $0.id == parentId }) {
                parentAssets[index].children.append(child)
                currentParent = parentAssets[index]  // Keep currentParent in sync
            }

            // Reset for next child or move on
            currentPhotos = []
            activePhotoType = PhotoCategory.defaultNodeType
            selectedNodeClass = nil
            selectedNodeSubtype = nil
            draftCoreAttributes = [:]
            assetLabel = ""
            draftCOM = 1
            draftCOMCalculation = nil
            draftServiceability = nil
            draftServiceabilityNote = ""
            voiceTargetField = nil
            voiceTargetIsLabel = false

            // Show options: Add Another OCP or Done with OCPs
            currentState = .decidingChildAction
        } else {
            // Create parent asset
            let parent = WalkthroughParentAsset(
                nodeClass: nodeClass,
                nodeSubtype: selectedNodeSubtype,
                photos: currentPhotos,
                children: [],
                draftAttributes: savedAttributes,
                label: savedLabel,
                // ZP-2230: fall back to 1 (serviceable) when the
                // user never opened the COM calculator. Previous
                // behavior persisted nil, which blocked readiness.
                com: savedCOM ?? 1,
                comCalculation: savedCOMCalculation,
                serviceability: savedServiceability,
                serviceabilityNote: savedServiceabilityNote.isEmpty ? nil : savedServiceabilityNote
            )
            parentAssets.append(parent)
            currentParent = parent

            // Reset
            currentPhotos = []
            activePhotoType = PhotoCategory.defaultNodeType
            selectedNodeClass = nil
            selectedNodeSubtype = nil
            draftCoreAttributes = [:]
            assetLabel = ""
            draftCOM = 1
            draftCOMCalculation = nil
            draftServiceability = nil
            draftServiceabilityNote = ""
            voiceTargetField = nil
            voiceTargetIsLabel = false

            // Check if this class supports children
            if nodeClass.box {
                currentState = .promptingForChildren
            } else {
                currentState = .decidingNextAction
            }
        }
    }

    private func getLabel(for parent: WalkthroughParentAsset) -> String {
        let className = parent.nodeClass.name
        let sameClassAssets = parentAssets.filter { $0.nodeClass.id == parent.nodeClass.id }
        let index = sameClassAssets.firstIndex(where: { $0.id == parent.id }) ?? 0
        return "\(className) \(index + 1)"
    }

    private func getChildLabel(for child: WalkthroughChildAsset, in parent: WalkthroughParentAsset) -> String {
        let className = child.nodeClass.name
        let sameClassChildren = parent.children.filter { $0.nodeClass.id == child.nodeClass.id }
        let index = sameClassChildren.firstIndex(where: { $0.id == child.id }) ?? 0
        return "\(className) \(index + 1)"
    }

    // MARK: - Asset Creation

    private func createAllAssets() async {
        await MainActor.run {
            currentState = .creating
            createProgress = 0
            totalToCreate = totalAssetCount
        }

        var createdNodeCount = 0
        let sld = contextSLD

        do {
            for parent in parentAssets {
                // Calculate label — use custom name if provided
                let parentLabel = parent.label ?? getLabel(for: parent)
                let hasChildren = !parent.children.isEmpty

                // Create parent NodeV2
                let parentNode = NodeV2(
                    id: UUID(),
                    label: parentLabel,
                    type: hasChildren ? "group" : parent.nodeClass.style,
                    sld: sld,
                    parent_id: nil,
                    x: Double.random(in: -1000...1000),
                    y: Double.random(in: -1000...1000),
                    width: parent.nodeClass.width,
                    height: parent.nodeClass.height,
                    photos: [],
                    is_deleted: false,
                    room: room,
                    node_class: parent.nodeClass,
                    node_subtype: parent.nodeSubtype,
                    core_attributes: [],
                    node_tasks: [],
                    ir_photos: []
                )

                parentNode.lastModifiedAt = Date()
                parentNode.com = parent.com ?? 1
                parentNode.com_calculation = parent.comCalculation
                parentNode.serviceability = parent.serviceability?.rawValue
                parentNode.serviceability_note = parent.serviceabilityNote

                // Convert staged photos to Photo entities
                var parentPhotos: [Photo] = []
                for stagedPhoto in parent.photos {
                    let photo = Photo(
                        id: UUID(),
                        node: parentNode,
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
                    parentPhotos.append(photo)
                }

                // Apply core attributes BEFORE server sync so they're included in the create payload
                if !parent.draftAttributes.isEmpty {
                    CoreAttributesService.applyCoreAttributeChanges(
                        to: parentNode,
                        selectedClass: parent.nodeClass,
                        originalClass: nil,
                        draftAttributes: parent.draftAttributes,
                        modelContext: modelContext
                    )
                }

                // Use NodeService - handles insert, terminals, sync, photo upload
                await NodeService.createNewNodeWithPhotosAndIR(
                    node: parentNode,
                    photos: parentPhotos,
                    irPhotos: [],
                    networkState: networkState,
                    modelContext: modelContext,
                    skipGraphUpdate: true
                )

                // Session relationship (NodeService doesn't handle this) — WO only.
                if let session = session {
                    session.nodes.append(parentNode)
                    parentNode.ir_sessions.append(session)
                    await createSessionMapping(for: parentNode)
                }

                createdNodeCount += 1
                await MainActor.run { createProgress += 1 }

                // Create children (each with their own photos)
                for child in parent.children {
                    let childLabel = child.label ?? getChildLabel(for: child, in: parent)

                    let childNode = NodeV2(
                        id: UUID(),
                        label: childLabel,
                        type: child.nodeClass.style,
                        sld: sld,
                        parent_id: parentNode.id,
                        x: 0,
                        y: 0,
                        width: child.nodeClass.width,
                        height: child.nodeClass.height,
                        photos: [],
                        is_deleted: false,
                        room: room,
                        node_class: child.nodeClass,
                        node_subtype: child.nodeSubtype,
                        core_attributes: [],
                        node_tasks: [],
                        ir_photos: []
                    )

                    childNode.lastModifiedAt = Date()
                    childNode.com = child.com ?? 1
                    childNode.com_calculation = child.comCalculation
                    childNode.serviceability = child.serviceability?.rawValue
                    childNode.serviceability_note = child.serviceabilityNote

                    // Convert staged photos to Photo entities
                    var childPhotos: [Photo] = []
                    for stagedPhoto in child.photos {
                        let photo = Photo(
                            id: UUID(),
                            node: childNode,
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
                        childPhotos.append(photo)
                    }

                    // Apply core attributes BEFORE server sync so they're included in the create payload
                    if !child.draftAttributes.isEmpty {
                        CoreAttributesService.applyCoreAttributeChanges(
                            to: childNode,
                            selectedClass: child.nodeClass,
                            originalClass: nil,
                            draftAttributes: child.draftAttributes,
                            modelContext: modelContext
                        )
                    }

                    // Use NodeService - handles insert, terminals, sync, photo upload
                    await NodeService.createNewNodeWithPhotosAndIR(
                        node: childNode,
                        photos: childPhotos,
                        irPhotos: [],
                        networkState: networkState,
                        modelContext: modelContext,
                        skipGraphUpdate: true
                    )

                    // Session relationship (NodeService doesn't handle this) — WO only.
                    if let session = session {
                        session.nodes.append(childNode)
                        childNode.ir_sessions.append(session)
                        await createSessionMapping(for: childNode)
                    }

                    createdNodeCount += 1
                    await MainActor.run { createProgress += 1 }
                }
            }

            // Save final state
            try modelContext.save()

            // Update the WebView graph once at the end. Both context
            // paths (WO + standalone) resolve an SLD via `contextSLD`
            // above, but guard for the edge case where the room
            // hierarchy is genuinely detached.
            if let sld = sld {
                WebViewBridge.updateGraphFromSLD(sld.id, in: modelContext, animated: true)
            }

            await MainActor.run {
                createdCount = createdNodeCount
                showSuccessAlert = true
            }

        } catch {
            guard !AuthError.isAuthError(error) else { return }
            await MainActor.run {
                errorMessage = "Failed to create assets: \(error.localizedDescription)"
                showError = true
                currentState = .reviewing
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
                AppLogger.log(.error, "Failed to create session mapping for node \(node.id), queueing: \(error)", category: .photo)
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
