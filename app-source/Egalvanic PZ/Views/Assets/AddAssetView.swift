import SwiftUI
import SwiftData
import PhotosUI
import AVFoundation

enum AssetCreationMode: CaseIterable {
    case quick
    case detailed

    var title: String {
        switch self {
        case .quick: return AppStrings.Assets.quick
        case .detailed: return AppStrings.Assets.detailed
        }
    }

    var icon: String {
        switch self {
        case .quick: return "bolt.fill"
        case .detailed: return "doc.text.fill"
        }
    }
}

struct AddAssetViewV2: View {
    @StateObject private var viewModel: AddAssetViewModel

    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    @Query private var allNodeSubtypes: [NodeSubtype]

    @State private var assetClassSearchText = ""

    private var filteredAssetClasses: [NodeClass] {
        let trimmed = assetClassSearchText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return viewModel.availableNodeClasses }
        return viewModel.availableNodeClasses.filter { $0.name.localizedCaseInsensitiveContains(trimmed) }
    }

    init(
        availableLocations: [String],
        availableNodeClasses: [NodeClass],
        sld: SLDV2,
        preselectedRoom: Room? = nil,
        onSave: @escaping (NodeV2, [Photo], [IRPhoto]) -> Void,
        onCancel: @escaping () -> Void
    ) {
        _viewModel = StateObject(wrappedValue: AddAssetViewModel(
            availableLocations: availableLocations,
            availableNodeClasses: availableNodeClasses,
            sld: sld,
            preselectedRoom: preselectedRoom,
            onSave: onSave,
            onCancel: onCancel
        ))
    }

    // MARK: - View Computed Properties (depend on @EnvironmentObject for reactivity)

    private var listeningTasks: [UserTask] {
        guard let session = appState.activeSession else { return [] }
        return session.user_tasks.filter { !$0.is_deleted && networkState.isListening($0.id) }
    }

    private var isVisualPhotoDisabled: Bool {
        appState.activeSession?.photo_type == "FLIR-IND"
    }

    private var canAddIRPhoto: Bool {
        if appState.activeSession?.photo_type == "FLIR-IND" {
            return !viewModel.irPhotoKey.isEmpty
        } else {
            return !viewModel.visualPhotoKey.isEmpty && !viewModel.irPhotoKey.isEmpty
        }
    }

    private var filteredNodeSubtypes: [NodeSubtype] {
        viewModel.filteredNodeSubtypes(from: allNodeSubtypes)
    }

    private var showBottomBar: Bool {
        !viewModel.assetName.isEmpty && viewModel.selectedNodeClass != nil && viewModel.selectedRoom != nil
    }

    // MARK: - Body
    // Extracted heavy inline sections into @ViewBuilder computed properties
    // to keep constraint solver branches under the 1M SolverBindingThreshold.

    var body: some View {
        NavigationStack {
            ZStack {
                ScrollView {
                    VStack(spacing: 0) {
                        // Main Content
                        VStack(spacing: 20) {
                            // Title
                            Text(AppStrings.Assets.newAsset)
                                .font(.largeTitle)
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity, alignment: .leading)

                            // Auto-Link Listening Tasks
                            listeningTasksSection

                            // Basic Information Card
                            assetDetailsCard

                            // Equipment Library
                            equipmentLibraryCard

                            // Photos
                            photosCard

                            // Core Attributes
                            coreAttributesSection

                            // ZP-2415: Commercial section (COM, Suggested
                            // PM Plan, Replacement Cost) — detailed mode only,
                            // positioned after Engineering / before Notes
                            // to match web's AssetFormFields layout.
                            if viewModel.creationMode == .detailed {
                                CommercialSection(
                                    draftCOM: $viewModel.draftCOM,
                                    draftCOMCalculation: $viewModel.draftCOMCalculation,
                                    selectedShortcut: $viewModel.selectedShortcut,
                                    draftReplacementCost: $viewModel.draftReplacementCost,
                                    nodeClass: viewModel.selectedNodeClass,
                                    nodeSubtype: viewModel.selectedNodeSubtype,
                                    sld: viewModel.sld,
                                    onShowCOMCalculator: {
                                        viewModel.showCOMCalculator = true
                                    },
                                    showSuggestedPMPlan: true
                                )
                            }

                            // Notes (detailed mode)
                            notesCard

                            // IR Photos (if active session)
                            irPhotosCard
                        }
                        .padding()
                        .padding(.bottom, showBottomBar ? 100 : 20)
                    }
                }
                .scrollDismissesKeyboard(.interactively)
                .background(Color(.systemGroupedBackground))
                .onTapGesture {
                    // Dismiss keyboard when tapping outside
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }

                // Bottom Action Bar
                bottomActionBar
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    modeSelector
                }
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        if viewModel.hasUnsavedChanges {
                            viewModel.showDiscardChangesAlert = true
                        } else {
                            viewModel.onCancel()
                        }
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button {
                            viewModel.showCopyFromPicker = true
                        } label: {
                            Label(AppStrings.AssetsExtra.copyDetailsFrom, systemImage: "doc.on.doc")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .onAppear {
            viewModel.configure(modelContext: modelContext, networkState: networkState, appState: appState)
            viewModel.setupDefaultPhotoKeys()
            viewModel.selectedListeningTaskIds = Set(listeningTasks.map(\.id))
        }
        .alert(AppStrings.Assets.saveFailed, isPresented: Binding(
            get: { viewModel.saveError != nil },
            set: { if !$0 { viewModel.saveError = nil } }
        )) {
            Button(AppStrings.Common.ok) { viewModel.saveError = nil }
        } message: {
            Text(viewModel.saveError?.localizedDescription ?? "Unknown error")
        }
        .fullScreenCover(isPresented: $viewModel.showQRScanner) {
            QRCodeScannerView(scannedCode: $viewModel.qrCode) { code in
                AppLogger.log(.info, "QR Code scanned: \(code)", category: .node)
            }
        }
        .sheet(isPresented: $viewModel.showCOMCalculator) {
            COMCalculatorView(initialCalculation: viewModel.draftCOMCalculation) { rating, calculation in
                viewModel.draftCOM = rating
                viewModel.draftCOMCalculation = calculation
                if calculation.isNonserviceable {
                    viewModel.selectedServiceability = .nonServiceable
                } else {
                    viewModel.selectedServiceability = .serviceable
                    viewModel.draftServiceabilityNote = ""
                }
            }
        }
        .alert(AppStrings.Alerts.discardChanges, isPresented: $viewModel.showDiscardChangesAlert) {
            Button(AppStrings.Common.keepEditing, role: .cancel) { }
            Button(AppStrings.Common.discard, role: .destructive) {
                viewModel.discardAndCancel()
            }
        } message: {
            Text(AppStrings.Alerts.discardChangesMessage)
        }
        .alert(AppStrings.AssetsExtra.changeAssetClassTitle, isPresented: $viewModel.showClassChangeConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                viewModel.cancelClassChange()
            }
            Button(AppStrings.AssetsExtra.changeAssetClassConfirm, role: .destructive) {
                viewModel.confirmClassChange()
            }
        } message: {
            Text(AppStrings.AssetsExtra.changeAssetClassMessage)
        }
        .sheet(isPresented: $viewModel.showCopyFromPicker) {
            CopyAssetDetailsPickerView(
                sld: viewModel.sld,
                currentNode: viewModel.placeholderNode,
                nodeClass: viewModel.selectedNodeClass,
                direction: .from
            ) { selectedNode in
                viewModel.copySelectedNode = selectedNode
                viewModel.showCopyFieldSelection = true
            }
        }
        .sheet(isPresented: $viewModel.showCopyFieldSelection, onDismiss: {
            viewModel.copySelectedNode = nil
        }) {
            if let selectedNode = viewModel.copySelectedNode {
                CopyAssetFieldSelectionView(
                    sourceNodeLabel: selectedNode.label,
                    direction: .from
                ) { fields in
                    viewModel.applyCopyFrom(source: selectedNode, fields: fields)
                }
            }
        }
        .alert(AppStrings.AssetsExtra.copyCompleteAlert, isPresented: Binding(
            get: { viewModel.copySuccessMessage != nil },
            set: { if !$0 { viewModel.copySuccessMessage = nil } }
        )) {
            Button(AppStrings.Common.ok) { viewModel.copySuccessMessage = nil }
        } message: {
            Text(viewModel.copySuccessMessage ?? "")
        }
        .onChange(of: viewModel.qrCode) { _, newValue in
            viewModel.onQRCodeChanged(newValue)
        }
        .overlay {
            savingOverlay
        }
    }

    // MARK: - Asset Details Card (extracted from body)

    @ViewBuilder
    private var assetDetailsCard: some View {
        VStack(spacing: 16) {
            SectionHeader(title: AppStrings.Assets.assetDetails, systemImage: "info.circle")

            VStack(spacing: 12) {
                ModernTextField(
                    title: AppStrings.Common.name,
                    text: $viewModel.assetName,
                    icon: "tag"
                )

                RoomPickerView(
                    selectedRoom: $viewModel.selectedRoom,
                    isLocked: viewModel.preselectedRoom != nil,
                    sld: viewModel.sld
                )

                // Asset Class Picker (sheet-based to avoid Menu lag with many options)
                VStack(alignment: .leading, spacing: 8) {
                    Text(AppStrings.Assets.assetClass)
                        .font(.caption)
                        .foregroundColor(.secondary)

                    HStack {
                        Image(systemName: "cube")
                            .foregroundColor(.secondary)
                            .frame(width: 20)

                        Text(viewModel.selectedNodeClass?.name ?? AppStrings.Assets.selectAssetClass)
                            .foregroundColor(viewModel.selectedNodeClass != nil ? .primary : .secondary)

                        Spacer()

                        Image(systemName: "chevron.down")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                        viewModel.showAssetClassSheet = true
                    }
                    .sheet(isPresented: $viewModel.showAssetClassSheet) {
                        NavigationStack {
                            List {
                                ForEach(filteredAssetClasses, id: \.id) { nodeClass in
                                    Button {
                                        // Route through requestClassChange so a swap from one
                                        // class to another surfaces the destructive-change
                                        // confirmation. Initial selections still apply
                                        // immediately — no prior data to lose.
                                        viewModel.requestClassChange(to: nodeClass)
                                        viewModel.showAssetClassSheet = false
                                    } label: {
                                        HStack {
                                            Text(nodeClass.name)
                                                .foregroundColor(.primary)
                                            Spacer()
                                            if viewModel.selectedNodeClass?.id == nodeClass.id {
                                                Image(systemName: "checkmark")
                                                    .foregroundColor(.blue)
                                            }
                                        }
                                    }
                                }
                            }
                            .searchable(text: $assetClassSearchText, placement: .navigationBarDrawer(displayMode: .always), prompt: AppStrings.Common.search)
                            .navigationTitle(AppStrings.Assets.assetClass)
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .cancellationAction) {
                                    Button(AppStrings.Common.cancel) {
                                        viewModel.showAssetClassSheet = false
                                    }
                                }
                            }
                            .onDisappear { assetClassSearchText = "" }
                        }
                        .presentationDetents([.medium, .large])
                    }
                }

                // Asset Subtype Picker (Optional)
                // ZP-2368: when the engineering section owns the
                // subtype picker (detailed + eng-lib), hide the inline
                // one to avoid duplicate controls.
                if !(viewModel.creationMode == .detailed && AuthService.shared.hasFeature("eng-lib")) {
                    ModernPicker(
                        title: AppStrings.Assets.assetSubtype,
                        icon: "tag",
                        placeholder: AppStrings.Assets.selectAssetSubtype,
                        items: filteredNodeSubtypes,
                        selection: $viewModel.selectedNodeSubtype,
                        displayName: { $0.name },
                        useSheet: true
                    )
                    .disabled(viewModel.selectedNodeClass == nil)
                }

                // ZP-2415: Suggested PM Plan moved into the Commercial
                // section, which renders after the Engineering card.

                // QR Code field with scanner button and display
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text(AppStrings.Assets.qrCode)
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Spacer()

                        Button(action: {
                            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                            viewModel.showQRScanner = true
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: "qrcode.viewfinder")
                                    .font(.caption)
                                Text(AppStrings.Assets.scan)
                                    .font(.caption)
                            }
                            .foregroundColor(.blue)
                        }
                    }

                    HStack {
                        Image(systemName: "qrcode")
                            .foregroundColor(.secondary)
                            .frame(width: 20)

                        TextField(AppStrings.Assets.enterOrScanQR, text: $viewModel.qrCode)
                            .textFieldStyle(PlainTextFieldStyle())
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)

                    // Display QR Code if one exists (uses cached image to avoid regeneration on every keystroke)
                    if !viewModel.qrCode.isEmpty, let qrImage = viewModel.cachedQRImage {
                        VStack(spacing: 8) {
                            Text(AppStrings.Assets.qrCodePreview)
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Image(uiImage: qrImage)
                                .interpolation(.none)
                                .resizable()
                                .scaledToFit()
                                .frame(height: 120)
                                .background(Color.white)
                                .cornerRadius(8)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                                )
                        }
                        .padding(.top, 8)
                    }

                    // ZP-2415: COM moved into the Commercial section.

                    // Serviceability label (only when non-serviceable, set by COM Calculator)
                    if viewModel.creationMode == .detailed && viewModel.selectedServiceability == .nonServiceable {
                        serviceabilityLabel
                            .transition(.opacity.combined(with: .move(edge: .top)))
                    }

                    // Voltage (only in detailed mode)
                    // ZP-2368: when the engineering section owns the
                    // voltage pickers (detailed + eng-lib), hide the
                    // inline ones to avoid duplicate controls.
                    if viewModel.creationMode == .detailed && !AuthService.shared.hasFeature("eng-lib") {
                        if viewModel.selectedNodeClass?.primary_secondary_voltage == true {
                            HStack(spacing: 12) {
                                VoltagePickerField(
                                    label: AppStrings.AssetsExtra.primaryVoltage,
                                    selectedId: $viewModel.selectedVoltageId,
                                    selectedValue: $viewModel.selectedVoltage
                                )
                                VoltagePickerField(
                                    label: AppStrings.AssetsExtra.secondaryVoltage,
                                    selectedId: $viewModel.selectedSecondaryVoltageId,
                                    selectedValue: $viewModel.selectedSecondaryVoltage
                                )
                            }
                        } else {
                            VoltagePickerField(
                                label: AppStrings.AssetsExtra.voltage,
                                selectedId: $viewModel.selectedVoltageId,
                                selectedValue: $viewModel.selectedVoltage
                            )
                        }
                    }
                }
            }
        }
        .padding()
        .background(colorScheme == .dark ? Color(.secondarySystemBackground) : Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }

    // MARK: - Equipment Library Card (removed)

    /// ZP-2161 Phase 4B: the legacy ``EquipmentLibrarySection`` (EZP
    /// flow) was retired with the SKM library matcher. Library binding
    /// now lives in ``NodeEngineeringSection`` on the edit-asset path —
    /// the user creates the node first, then opens it to bind a
    /// library row. AddAssetView can be re-wired to surface the new
    /// SKM matcher inline later if creation-time binding becomes
    /// required again.
    @ViewBuilder
    private var equipmentLibraryCard: some View {
        EmptyView()
    }

    // MARK: - Photos Card (extracted from body)

    @ViewBuilder
    private var photosCard: some View {
        VStack(spacing: 16) {
            EntityFancyPhotoPicker(
                entity: viewModel.placeholderNode,
                displayedPhotos: $viewModel.displayedPhotos,
                isSaving: $viewModel.isSaving,
                onPhotoAdded: { photo in
                    viewModel.onPhotoAdded(photo)
                },
                onPhotoDeleted: { photo in
                    viewModel.onPhotoDeleted(photo)
                },
                sectionTitle: AppStrings.Assets.assetPhotos,
                sectionIcon: "photo.stack",
                showUploadIndicators: true,
                showPhotoTypeSelector: true,
                skipEntityAssociation: true,  // ← CRITICAL: Prevent tempNode auto-insertion!
                overrideSLD: viewModel.sld,  // Pass SLD directly instead of extracting from temp node
                // ZP-2230: the placeholder node has a stub label and
                // no room link yet; the form has the real values.
                overrideLabel: viewModel.assetName,
                overrideRoom: viewModel.selectedRoom,
                onSetDefaultPhoto: { photoId in
                    viewModel.draftDefaultPhotoId = photoId
                },
                draftDefaultPhotoId: viewModel.draftDefaultPhotoId
            )
        }
        .padding()
        .background(colorScheme == .dark ? Color(.secondarySystemBackground) : Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }

    // MARK: - Notes Card (extracted from body)

    @ViewBuilder
    private var notesCard: some View {
        if viewModel.creationMode == .detailed {
            VStack(alignment: .leading, spacing: 8) {
                Text(AppStrings.Common.notes)
                    .font(.caption)
                    .foregroundColor(.secondary)
                TextEditor(text: $viewModel.draftNotes)
                    .frame(minHeight: 80)
                    .padding(8)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    .overlay(
                        Group {
                            if viewModel.draftNotes.isEmpty {
                                Text(AppStrings.AssetsExtra.addNotesAboutAsset)
                                    .foregroundColor(.secondary.opacity(0.5))
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 16)
                                    .allowsHitTesting(false)
                            }
                        },
                        alignment: .topLeading
                    )
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
        }
    }

    // MARK: - IR Photos Card (extracted from body)

    @ViewBuilder
    private var irPhotosCard: some View {
        if appState.activeSession != nil {
            VStack(spacing: 16) {
                SectionHeader(title: AppStrings.Assets.infraredPhotos, systemImage: "camera.filters")

                // Session Display
                VStack(spacing: 12) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            if let session = appState.activeSession {
                                HStack {
                                    ActiveSessionBadge()
                                    Text(session.name)
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                }
                                HStack(spacing: 8) {
                                    Text("Type: \(session.photo_type)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)

                                    if session.photo_type == "FLIR-IND" {
                                        Label(AppStrings.Assets.irOnly, systemImage: "info.circle")
                                            .font(.caption2)
                                            .foregroundColor(.blue)
                                    }
                                }
                            }
                        }

                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)

                    // Input fields
                    VStack(spacing: 12) {
                        // IR Photo field (always shown first)
                        ModernTextField(
                            title: AppStrings.Assets.irPhotoFilename,
                            text: $viewModel.irPhotoKey,
                            icon: "camera.filters"
                        )

                        // Visual Photo field - disabled for FLIR-IND
                        if !isVisualPhotoDisabled {
                            ModernTextField(
                                title: AppStrings.Assets.visualPhotoFilename,
                                text: $viewModel.visualPhotoKey,
                                icon: "photo"
                            )
                        } else {
                            // Show disabled state for FLIR-IND
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Image(systemName: "photo")
                                        .foregroundColor(.gray)
                                    Text(AppStrings.Assets.visualPhotoNotRequired)
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 12)
                                .background(Color(.systemGray6).opacity(0.5))
                                .cornerRadius(10)
                            }
                        }

                        Button(action: { viewModel.addIRPhotoPair() }) {
                            Label(AppStrings.Assets.addIRPhotoPair, systemImage: "plus.circle.fill")
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(canAddIRPhoto ? Color.blue : Color.gray)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                        .disabled(!canAddIRPhoto)
                    }

                    // Staged IR photos
                    if !viewModel.stagedIRPhotos.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(AppStrings.Assets.newIRPhotos)
                                .font(.caption)
                                .foregroundColor(.secondary)

                            ForEach(viewModel.stagedIRPhotos) { irPhoto in
                                IRPhotoRowForAdd(irPhoto: irPhoto, isStaged: true) {
                                    viewModel.stagedIRPhotos.removeAll { $0.id == irPhoto.id }
                                }
                            }
                        }
                    }
                }
            }
            .padding()
            .background(colorScheme == .dark ? Color(.secondarySystemBackground) : Color(.systemBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
        }
    }

    // MARK: - Bottom Action Bar (extracted from body)

    @ViewBuilder
    private var bottomActionBar: some View {
        if showBottomBar {
            VStack {
                Spacer()

                VStack(spacing: 0) {
                    Rectangle()
                        .fill(Color(.systemBackground))
                        .frame(height: 0)
                        .shadow(color: .black.opacity(0.1), radius: 10, y: -5)

                    Button(action: { viewModel.performSave() }) {
                        Text(AppStrings.Assets.createAsset)
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(viewModel.isSaving ? Color.blue.opacity(0.6) : Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                    .disabled(viewModel.isSaving)
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 18)
                    .background(Color(.systemBackground))
                }
            }
            .ignoresSafeArea()
        }
    }

    // MARK: - Saving Overlay (extracted from body)

    @ViewBuilder
    private var savingOverlay: some View {
        if viewModel.isSaving {
            ZStack {
                Color.black.opacity(0.4)
                    .ignoresSafeArea()

                VStack(spacing: 16) {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .primary))
                        .scaleEffect(1.5)

                    Text(AppStrings.Assets.creatingAsset)
                        .font(.headline)
                        .foregroundColor(.primary)
                }
                .padding(32)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(16)
            }
        }
    }

    // MARK: - Mode Selector

    private var modeSelector: some View {
        Picker(AppStrings.Assets.creationMode, selection: $viewModel.creationMode) {
            ForEach(AssetCreationMode.allCases, id: \.self) { mode in
                Text(mode.title).tag(mode)
            }
        }
        .pickerStyle(.segmented)
        .frame(width: 180, height: 44)
    }

    // MARK: - Listening Tasks Section

    @ViewBuilder
    private var listeningTasksSection: some View {
        if !listeningTasks.isEmpty {
            VStack(spacing: 12) {
                HStack(spacing: 6) {
                    Image(systemName: "ear.fill")
                        .font(.subheadline)
                        .foregroundColor(.orange)
                    Text(AppStrings.Assets.autoLinkTasks)
                        .font(.headline)
                    Spacer()
                }

                ForEach(listeningTasks) { task in
                    ListeningTaskCheckRow(
                        task: task,
                        isSelected: viewModel.selectedListeningTaskIds.contains(task.id),
                        onToggle: {
                            if viewModel.selectedListeningTaskIds.contains(task.id) {
                                viewModel.selectedListeningTaskIds.remove(task.id)
                            } else {
                                viewModel.selectedListeningTaskIds.insert(task.id)
                            }
                        }
                    )
                }
            }
            .padding()
            .background(Color.orange.opacity(0.08))
            .cornerRadius(12)
        }
    }

    // MARK: - Serviceability Label (read-only, set by COM Calculator)

    @ViewBuilder
    private var serviceabilityLabel: some View {
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

            // Serviceability Note
            VStack(alignment: .leading, spacing: 4) {
                Text(AppStrings.AssetsExtra.serviceabilityNote)
                    .font(.caption)
                    .foregroundColor(.secondary)
                TextEditor(text: $viewModel.draftServiceabilityNote)
                    .frame(minHeight: 60, maxHeight: 120)
                    .padding(8)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    .overlay(
                        Group {
                            if viewModel.draftServiceabilityNote.isEmpty {
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

    // MARK: - Core Attributes Section

    @ViewBuilder
    private var coreAttributesSection: some View {
        if viewModel.creationMode == .detailed, let nodeClass = viewModel.selectedNodeClass {
            // ZP-2368: when the company has the `eng-lib` flag, render
            // the full Engineering section (subtype, voltage, first-class
            // engineering blocks, custom attributes, SKM library matcher)
            // — matching the EditNodeDetailView layout — so the user
            // doesn't have to save first to access these inputs.
            if AuthService.shared.hasFeature("eng-lib") {
                NodeEngineeringSection(
                    node: viewModel.placeholderNode,
                    nodeClass: nodeClass,
                    selectedNodeSubtype: $viewModel.selectedNodeSubtype,
                    filteredNodeSubtypes: filteredNodeSubtypes,
                    draftVoltage: $viewModel.selectedVoltage,
                    draftVoltageId: $viewModel.selectedVoltageId,
                    draftSecondaryVoltage: $viewModel.selectedSecondaryVoltage,
                    draftSecondaryVoltageId: $viewModel.selectedSecondaryVoltageId,
                    draft: $viewModel.engineeringDraft,
                    draftCoreAttributes: $viewModel.draftCoreAttributes,
                    eqpLibSelection: $viewModel.draftEqpLibSelection,
                    nameplatePhotos: viewModel.stagedNameplatePhotos,
                    onCoreAttrExtractionComplete: { newAttributes in
                        viewModel.draftCoreAttributes = newAttributes
                    }
                )
                .padding()
                .background(colorScheme == .dark ? Color(.secondarySystemBackground) : Color(.systemBackground))
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                .transition(.opacity.combined(with: .move(edge: .top)))
            } else {
                NodeCoreAttributesSection(
                    node: viewModel.placeholderNode,
                    selectedNodeClass: nodeClass,
                    draftAttributes: $viewModel.draftCoreAttributes,
                    nameplatePhotos: viewModel.stagedNameplatePhotos,
                    onExtractionComplete: { newAttributes in
                        viewModel.draftCoreAttributes = newAttributes
                    }
                )
                .padding()
                .background(colorScheme == .dark ? Color(.secondarySystemBackground) : Color(.systemBackground))
                .cornerRadius(16)
                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }
}

// MARK: - IR Photo Row

struct IRPhotoRowForAdd: View {
    let irPhoto: IRPhoto
    let isStaged: Bool
    let onDelete: (() -> Void)?

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("IR: \(irPhoto.ir_photo_key)")
                    .font(.caption)
                    .foregroundColor(.orange)
                if !irPhoto.visual_photo_key.isEmpty {
                    Text("Visual: \(irPhoto.visual_photo_key)")
                        .font(.caption)
                }
            }

            Spacer()

            if isStaged {
                Label(AppStrings.Common.pending, systemImage: "clock.fill")
                    .font(.caption2)
                    .foregroundColor(.orange)
                    .labelStyle(.iconOnly)
            } else {
                Text(irPhoto.date_created.formatted(date: .abbreviated, time: .omitted))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

            if let onDelete = onDelete {
                Button(action: onDelete) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.red)
                        .font(.caption)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

// Simplified photo picker that doesn't require a node or model context
struct StagedPhotoPickerView: View {
    @Binding var displayedPhotos: [Photo]
    @Binding var isSaving: Bool
    let onPhotoAdded: (Data, String) -> Void
    let onPhotoDeleted: (Photo) -> Void

    @State private var selectedItem: PhotosPickerItem? = nil
    @State private var showingCamera = false
    @State private var photoToDelete: Photo? = nil
    @State private var showingDeleteConfirmation = false
    @State private var selectedPhoto: Photo? = nil
    @State private var capturedImage: UIImage? = nil

    var body: some View {
        VStack {
            if !displayedPhotos.isEmpty && !isSaving {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 16) {
                        ForEach(displayedPhotos) { photo in
                            thumbnailView(for: photo)
                                .onTapGesture { selectedPhoto = photo }
                        }
                    }
                    .padding(.horizontal)
                }
                .frame(height: 140)
            }

            // Buttons for gallery and camera
            HStack(spacing: 20) {
                PhotosPicker(selection: $selectedItem, matching: .images) {
                    Label(AppStrings.Common.gallery, systemImage: "photo.on.rectangle")
                        .padding(8)
                        .background(Color.blue.opacity(0.1))
                        .cornerRadius(8)
                }
                .buttonStyle(.plain)

                Button {
                    showingCamera = true
                } label: {
                    Label(AppStrings.Common.camera, systemImage: "camera.fill")
                        .padding(8)
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(8)
                }
                .buttonStyle(.plain)
                .disabled(!UIImagePickerController.isSourceTypeAvailable(.camera))
            }
            .frame(maxWidth: .infinity, alignment: .center)
            .padding()
        }
        .confirmationDialog(
            "Delete this photo?",
            isPresented: $showingDeleteConfirmation,
            titleVisibility: .visible
        ) {
            Button(AppStrings.Common.delete, role: .destructive) {
                if let toDelete = photoToDelete {
                    onPhotoDeleted(toDelete)
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        }
        .fullScreenCover(item: $selectedPhoto) { photo in
            FullImageView(selected: photo, allPhotos: displayedPhotos)
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
                let maxDim: CGFloat = 1024
                let toUpload = image.resized(maxDimension: maxDim) ?? image
                if let data = toUpload.jpegData(compressionQuality: 0.7) {
                    handleNewPhoto(data: data)
                }
                capturedImage = nil
            }
        }
        .onChange(of: selectedItem) { oldItem, newItem in
            Task {
                guard
                    let item = newItem,
                    let data = try? await item.loadTransferable(type: Data.self),
                    let uiImage = UIImage(data: data)
                else { return }

                let resizedImage = uiImage.resized(maxDimension: 1024) ?? uiImage
                guard let jpeg = resizedImage.jpegData(compressionQuality: 0.7) else { return }

                handleNewPhoto(data: jpeg)
            }
        }
    }

    private func handleNewPhoto(data: Data) {
        // Save to disk
        let filename = "\(UUID().uuidString).jpg"
        let folder = FileManager.default
            .urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("photos", isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        let url = folder.appendingPathComponent(filename)

        do {
            try data.write(to: url)
            // Notify parent with data and filename
            onPhotoAdded(data, filename)
        } catch {
            AppLogger.log(.error, "Failed to save image: \(error)", category: .node)
        }
    }

    @ViewBuilder
    private func thumbnailView(for photo: Photo) -> some View {
        ZStack(alignment: .topTrailing) {
            Group {
                if let fileURL = photo.localFileURL,
                   let uiImage = UIImage(contentsOfFile: fileURL.path) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFill()
                } else {
                    // Placeholder
                    ZStack {
                        Color.gray.opacity(0.1)
                        Image(systemName: "photo")
                            .foregroundColor(.gray)
                    }
                }
            }
            .frame(width: 100, height: 100)
            .clipped()
            .cornerRadius(8)

            // Upload indicator
            if photo.upload_needed {
                Image(systemName: "icloud.and.arrow.up")
                    .font(.caption)
                    .padding(4)
                    .background(Color.orange)
                    .foregroundColor(.white)
                    .clipShape(Circle())
                    .shadow(radius: 2)
                    .offset(x: -5, y: 72)
            }

            // Trash button
            Button {
                photoToDelete = photo
                showingDeleteConfirmation = true
            } label: {
                Image(systemName: "trash.circle.fill")
                    .font(.title2)
                    .foregroundColor(.white)
                    .background(Color.red)
                    .clipShape(Circle())
                    .shadow(radius: 2)
            }
            .padding(4)
        }
    }
}

// Camera wrapper that uses navigation instead of sheet/fullScreenCover
// On iPad: uses IPadCameraView (custom AVCaptureSession) to avoid "Flash is Disabled" alert
// On iPhone: uses standard UIImagePickerController
struct CameraViewWrapper: View {
    let onImageCaptured: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var isPresented = true

    var body: some View {
        ZStack {
            if isPresented {
                if UIDevice.current.userInterfaceIdiom == .pad {
                    IPadCameraView(
                        onImageCaptured: { image in
                            onImageCaptured(image)
                            dismiss()
                        },
                        onDismiss: {
                            dismiss()
                        }
                    )
                    .ignoresSafeArea()
                    .toolbar(.hidden, for: .navigationBar)
                } else {
                    CameraViewRepresentable(
                        onImageCaptured: { image in
                            onImageCaptured(image)
                            dismiss()
                        },
                        onDismiss: {
                            dismiss()
                        }
                    )
                    .ignoresSafeArea()
                    .toolbar(.hidden, for: .navigationBar)
                }
            }
        }
        .onAppear {
            isPresented = true
        }
    }
}

struct CameraViewRepresentable: UIViewControllerRepresentable {
    let onImageCaptured: (UIImage) -> Void
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        // Set modal presentation style to prevent parent dismissal
        picker.modalPresentationStyle = .overFullScreen
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: CameraViewRepresentable

        init(_ parent: CameraViewRepresentable) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.onImageCaptured(image)
            }
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onDismiss()
        }
    }
}
