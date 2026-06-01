//
//  AddAssetViewModel.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData

@MainActor
class AddAssetViewModel: ObservableObject {

    // MARK: - Dependencies (injected via configure)

    private(set) var modelContext: ModelContext?
    private(set) var networkState: NetworkState?
    private(set) var appState: AppStateManager?

    // MARK: - Configuration (set at init, never changes)

    let availableLocations: [String]
    let availableNodeClasses: [NodeClass]
    let sld: SLDV2
    let preselectedRoom: Room?
    let onSave: (NodeV2, [Photo], [IRPhoto]) -> Void
    let onCancel: () -> Void

    // MARK: - Form State

    @Published var assetName: String = ""
    @Published var assetLocation: String = ""
    @Published var selectedNodeClass: NodeClass? = nil {
        didSet {
            guard oldValue?.id != selectedNodeClass?.id else { return }
            // Apply defaults synchronously so the core-attributes section,
            // if it renders in the same pass, sees populated values from init.
            onNodeClassChanged()
        }
    }
    @Published var selectedNodeSubtype: NodeSubtype? = nil
    @Published var selectedShortcut: NodeShortcut? = nil
    @Published var selectedRoom: Room?
    @Published var qrCode: String = ""
    @Published var selectedServiceability: Serviceability? = nil
    @Published var draftServiceabilityNote: String = ""
    @Published var selectedVoltageId: Int? = nil
    @Published var selectedVoltage: Double? = nil
    @Published var selectedSecondaryVoltageId: Int? = nil
    @Published var selectedSecondaryVoltage: Double? = nil
    @Published var draftNotes: String = ""
    @Published var creationMode: AssetCreationMode = .quick
    @Published var draftCOM: Int? = 1
    @Published var draftCOMCalculation: COMCalculation? = nil
    // ZP-2415: Replacement Cost lives in the Commercial section.
    @Published var draftReplacementCost: Double? = nil
    @Published var draftCoreAttributes: [UUID: String] = [:]
    @Published var selectedListeningTaskIds: Set<UUID> = []
    @Published var draftEqpLibSelection: EqpLibSelection? = nil
    // ZP-2368: bundle of first-class engineering fields (kVA, impedance,
    // ampere rating, cable/busway, etc.) for the Create Asset flow when
    // the `eng-lib` company feature flag is enabled. Mirrors the
    // EditNodeDetailView pattern (Phase 3b).
    @Published var engineeringDraft: EngineeringDraft

    // MARK: - UI State

    @Published var showQRScanner = false
    @Published var showLocationPicker = false
    @Published var showAssetClassSheet = false
    @Published var showCOMCalculator = false
    @Published var showDiscardChangesAlert = false
    // Asset class change confirmation. We defer the actual class swap until
    // the user accepts the destructive warning — pendingNodeClass holds the
    // candidate while the alert is up.
    @Published var pendingNodeClass: NodeClass? = nil
    @Published var showClassChangeConfirmation = false
    @Published var showOnlyRequiredAttributes = false
    @Published var searchText = ""
    @Published var isSaving = false
    @Published var saveError: Error?

    // MARK: - Copy From State

    @Published var showCopyFromPicker = false
    @Published var showCopyFieldSelection = false
    @Published var copySelectedNode: NodeV2?
    @Published var copySuccessMessage: String?
    private var suppressClassClear = false

    // MARK: - Photo State

    @Published var stagedPhotos: [Photo] = []
    @Published var displayedPhotos: [Photo] = []
    @Published var draftDefaultPhotoId: UUID?
    @Published var stagedIRPhotos: [IRPhoto] = []
    @Published var visualPhotoKey = ""
    @Published var irPhotoKey = ""

    // MARK: - QR Code Cache

    @Published var cachedQRImage: UIImage? = nil

    // MARK: - Placeholder Node

    let placeholderNode: NodeV2 = {
        let node = NodeV2(
            id: UUID(),
            label: "Placeholder",
            type: "default",
            sld: nil,  // nil SLD prevents tracking
            parent_id: nil,
            x: 0,
            y: 0,
            width: 100,
            height: 100,
            photos: [],
            is_deleted: false,
            location: nil,
            node_class: nil,
            core_attributes: [],
            node_tasks: []
        )
        return node
    }()

    // MARK: - Private State

    private var lastUsedVisualKey: String? = nil
    private var lastUsedIRKey: String? = nil

    // MARK: - Init

    init(
        availableLocations: [String],
        availableNodeClasses: [NodeClass],
        sld: SLDV2,
        preselectedRoom: Room? = nil,
        onSave: @escaping (NodeV2, [Photo], [IRPhoto]) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.availableLocations = availableLocations
        self.availableNodeClasses = availableNodeClasses
        self.sld = sld
        self.preselectedRoom = preselectedRoom
        self.onSave = onSave
        self.onCancel = onCancel
        self.selectedRoom = preselectedRoom
        self.engineeringDraft = EngineeringDraft(from: self.placeholderNode)
    }

    // MARK: - Configure Dependencies

    func configure(
        modelContext: ModelContext,
        networkState: NetworkState,
        appState: AppStateManager
    ) {
        self.modelContext = modelContext
        self.networkState = networkState
        self.appState = appState
    }

    // MARK: - Computed Properties

    var filteredLocations: [String] {
        if searchText.isEmpty {
            return availableLocations
        }
        return availableLocations.filter {
            $0.localizedCaseInsensitiveContains(searchText)
        }
    }

    var stagedNameplatePhotos: [Photo] {
        stagedPhotos.filter { $0.type == "node_nameplate" }
    }

    func filteredNodeSubtypes(from allNodeSubtypes: [NodeSubtype]) -> [NodeSubtype] {
        guard let selectedClass = selectedNodeClass else { return [] }
        return allNodeSubtypes
            .filter { !$0.is_deleted && $0.node_class_id == selectedClass.id }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    var hasUnsavedChanges: Bool {
        !assetName.isEmpty ||
        selectedNodeClass != nil ||
        selectedNodeSubtype != nil ||
        selectedShortcut != nil ||
        !qrCode.isEmpty ||
        selectedServiceability != nil ||
        !draftServiceabilityNote.isEmpty ||
        !stagedPhotos.isEmpty ||
        !stagedIRPhotos.isEmpty ||
        !draftCoreAttributes.isEmpty ||
        draftCOM != 1 ||
        draftCOMCalculation != nil ||
        draftReplacementCost != nil ||
        engineeringDraft.isDirty(against: placeholderNode)
    }

    // MARK: - Actions

    func onNodeClassChanged() {
        if suppressClassClear {
            suppressClassClear = false
            return
        }
        // Clear every class-specific field — the confirmation dialog warns the
        // user about manufacturer & model, trip configuration, nameplate
        // fields, COM, suggested PM plans, and engineering custom attributes.
        // Subtype is also class-scoped (NodeSubtype.node_class_id) so it has
        // to clear too even though it's not in the warning copy.
        selectedNodeSubtype = nil
        selectedShortcut = nil
        // Reset Condition of Maintenance to the system default (1).
        draftCOM = 1
        draftCOMCalculation = nil
        // ZP-2415: Replacement Cost is class-scoped (web defaults from
        // node_subtype.replacement_cost). Clear on class swap so a
        // stale value from a previous class doesn't survive.
        draftReplacementCost = nil
        draftCoreAttributes = [:]
        if let nodeClass = selectedNodeClass {
            CoreAttributesService.applyDefaultValues(from: nodeClass, into: &draftCoreAttributes)
        }
        // ZP-2368: clear engineering fields when the class changes,
        // matching how draftCoreAttributes resets above. Otherwise
        // a kVA value entered for a transformer class would silently
        // persist if the user then switches to a cable / busway class
        // that doesn't surface that field. The same reasoning applies
        // to the Equipment Library selection (category + frame + SKM
        // bindings are class-specific).
        engineeringDraft = EngineeringDraft(from: placeholderNode)
        draftEqpLibSelection = nil
    }

    /// Entry point from the class picker. The initial selection (no class
    /// chosen yet) applies immediately because there's nothing to clear; a
    /// real class swap stashes the candidate and surfaces the warning alert.
    /// Quick mode renders none of the class-specific sections (no engineering
    /// card, no core attributes, no equipment library), so the swap is
    /// non-destructive there — skip the confirmation entirely.
    func requestClassChange(to newClass: NodeClass) {
        guard selectedNodeClass?.id != newClass.id else { return }
        if selectedNodeClass == nil || creationMode == .quick {
            selectedNodeClass = newClass
            return
        }
        pendingNodeClass = newClass
        showClassChangeConfirmation = true
    }

    func confirmClassChange() {
        if let newClass = pendingNodeClass {
            // didSet on selectedNodeClass fires onNodeClassChanged() which
            // does the actual clearing of class-specific state.
            selectedNodeClass = newClass
        }
        pendingNodeClass = nil
    }

    func cancelClassChange() {
        // selectedNodeClass was never changed, so nothing to revert beyond
        // dropping the pending candidate.
        pendingNodeClass = nil
    }

    func onQRCodeChanged(_ newValue: String) {
        if newValue.isEmpty {
            cachedQRImage = nil
        } else {
            cachedQRImage = generateQRCode(from: newValue)
        }
    }

    func onPhotoAdded(_ photo: Photo) {
        stagedPhotos.append(photo)
        displayedPhotos.append(photo)

        // Auto-set as default if first photo
        if draftDefaultPhotoId == nil {
            draftDefaultPhotoId = photo.id
        }
    }

    func onPhotoDeleted(_ photo: Photo) {
        // Remove from staged photos
        if let index = stagedPhotos.firstIndex(where: { $0.id == photo.id }) {
            let removed = stagedPhotos.remove(at: index)
            // Clean up local file if it exists
            if let path = removed.local_filepath {
                let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let fileURL = documentsURL.appendingPathComponent(path)
                try? FileManager.default.removeItem(at: fileURL)
            }
        }
        // Remove from displayed photos
        displayedPhotos.removeAll { $0.id == photo.id }

        // Handle default photo deletion
        if draftDefaultPhotoId == photo.id {
            // Find replacement: prefer profile photos first
            let replacement = displayedPhotos
                .filter { $0.id != photo.id }
                .sorted { photo1, photo2 in
                    // Prioritize node_profile type
                    if photo1.type == "node_profile" && photo2.type != "node_profile" {
                        return true
                    }
                    return false
                }
                .first
            draftDefaultPhotoId = replacement?.id
        }
    }

    func discardAndCancel() {
        // Clean up any staged photos
        for photo in stagedPhotos {
            if let path = photo.local_filepath {
                let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let fileURL = documentsURL.appendingPathComponent(path)
                try? FileManager.default.removeItem(at: fileURL)
            }
        }
        onCancel()
    }

    // MARK: - QR Code Generation

    func generateQRCode(from string: String) -> UIImage? {
        let data = string.data(using: .ascii)

        if let filter = CIFilter(name: "CIQRCodeGenerator") {
            filter.setValue(data, forKey: "inputMessage")
            filter.setValue("H", forKey: "inputCorrectionLevel")

            let transform = CGAffineTransform(scaleX: 10, y: 10)

            if let output = filter.outputImage?.transformed(by: transform) {
                let context = CIContext()
                if let cgImage = context.createCGImage(output, from: output.extent) {
                    return UIImage(cgImage: cgImage)
                }
            }
        }

        return nil
    }

    // MARK: - Photo Key Helpers

    func incrementPhotoKey(_ key: String, by increment: Int = 1) -> String? {
        let trimmedKey = key.trimmingCharacters(in: .whitespacesAndNewlines)

        // Find the last continuous sequence of digits at the end of the string
        var digitStartIndex: String.Index? = nil

        // Iterate from the end to find where the trailing number begins
        for index in trimmedKey.indices.reversed() {
            if trimmedKey[index].isNumber {
                digitStartIndex = index
            } else {
                // Stop when we hit a non-digit character
                break
            }
        }

        // If no trailing digits found, return nil
        guard let startIndex = digitStartIndex else { return nil }

        // Extract the prefix and the number part
        let prefix = String(trimmedKey[..<startIndex])
        let numberPart = String(trimmedKey[startIndex...])

        // Convert to integer, increment, and format back with same number of digits
        guard let number = Int(numberPart) else { return nil }
        let incrementedNumber = number + increment
        let formattedNumber = String(format: "%0\(numberPart.count)d", incrementedNumber)

        return prefix + formattedNumber
    }

    func setupDefaultPhotoKeys() {
        guard let appState = appState else { return }
        guard let session = appState.activeSession else {
            // Clear the fields if no active session
            visualPhotoKey = ""
            irPhotoKey = ""
            return
        }

        if session.photo_type == "FLIR-SEP" {
            // For FLIR-SEP, try to increment both keys by 2
            // Use local values first, then fall back to appState
            let lastVisual = lastUsedVisualKey ?? appState.lastCreatedVisualPhotoKey
            let lastIR = lastUsedIRKey ?? appState.lastCreatedIRPhotoKey

            if let lastVisualKey = lastVisual,
               let incrementedVisual = incrementPhotoKey(lastVisualKey, by: 2) {
                visualPhotoKey = incrementedVisual
            } else {
                visualPhotoKey = ""
            }

            if let lastIRKey = lastIR,
               let incrementedIR = incrementPhotoKey(lastIRKey, by: 2) {
                irPhotoKey = incrementedIR
            } else {
                irPhotoKey = ""
            }
        } else if session.photo_type == "FLIR-IND" {
            // For FLIR-IND, only increment IR key by 1
            visualPhotoKey = "" // Always clear visual key for FLIR-IND

            // Use local value first, then fall back to appState
            let lastIR = lastUsedIRKey ?? appState.lastCreatedIRPhotoKey

            if let lastIRKey = lastIR,
               let incrementedIR = incrementPhotoKey(lastIRKey, by: 1) {
                irPhotoKey = incrementedIR
            } else {
                irPhotoKey = ""
            }
        }
    }

    // MARK: - IR Photo Pair

    func addIRPhotoPair() {
        guard let appState = appState else { return }
        guard let session = appState.activeSession else {
            AppLogger.log(.error, "Cannot add IR photo pair - no active session", category: .node)
            return
        }

        // Use placeholderNode temporarily - actual node will be set during save
        let irPhoto = IRPhoto(
            id: UUID(),
            ir_session: session,
            node: placeholderNode,
            sld: sld,
            visual_photo_key: session.photo_type == "FLIR-IND" ? "" : visualPhotoKey,  // Clear visual for FLIR-IND
            ir_photo_key: irPhotoKey,
            date_created: Date()
        )

        stagedIRPhotos.append(irPhoto)

        // Track the last used keys locally (don't update global state yet)
        if session.photo_type == "FLIR-SEP" {
            lastUsedVisualKey = visualPhotoKey
            lastUsedIRKey = irPhotoKey
        } else if session.photo_type == "FLIR-IND" {
            lastUsedIRKey = irPhotoKey
        }

        // Clear and setup next default values
        visualPhotoKey = ""
        irPhotoKey = ""
        setupDefaultPhotoKeys()
    }

    // MARK: - Copy From

    func applyCopyFrom(source: NodeV2, fields: Set<CopyableField>) {
        // Auto-apply source class if none selected
        if selectedNodeClass == nil, let sourceClass = source.node_class {
            suppressClassClear = true
            selectedNodeClass = sourceClass
        }

        // Auto-switch to detailed mode (core attributes only render in detailed)
        if creationMode == .quick {
            creationMode = .detailed
        }

        if fields.contains(.assetSubtype) {
            selectedNodeSubtype = source.node_subtype
        }

        if fields.contains(.coreAttributes) {
            var sourceAttrs: [UUID: String] = [:]
            for attr in source.core_attributes {
                sourceAttrs[attr.id] = attr.value
            }
            draftCoreAttributes = sourceAttrs
        }

        if fields.contains(.serviceability) {
            selectedServiceability = source.serviceabilityEnum
            draftServiceabilityNote = source.serviceability_note ?? ""
            draftCOM = source.com
            draftCOMCalculation = source.com_calculation
        }
        
        // Clear shortcut — ShortcutPickerView auto-reloads via its own onChange
        selectedShortcut = nil

        copySuccessMessage = "Details copied from '\(source.label)'. Review changes and save."
        copySelectedNode = nil
    }

    // MARK: - Save

    func performSave() {
        guard let nodeClass = selectedNodeClass else { return }
        guard let modelContext = modelContext else { return }
        guard let appState = appState else { return }

        isSaving = true

        // Update global state with the last used photo keys (deferred from addIRPhoto)
        if let lastVisual = lastUsedVisualKey {
            appState.setLastCreatedVisualPhotoKey(lastVisual)
        }
        if let lastIR = lastUsedIRKey {
            appState.setLastCreatedIRPhotoKey(lastIR)
        }

        // Create the new node
        let newNodeId = UUID()
        let newNode = NodeV2(
            id: newNodeId,
            label: assetName,
            type: nodeClass.style,
            sld: sld,
            parent_id: nil,
            x: 0,
            y: 0,
            width: nodeClass.width,
            height: nodeClass.height,
            photos: [],
            is_deleted: false,
            location: nil,  // Deprecated: now using room relationship
            node_class: nodeClass,
            node_subtype: selectedNodeSubtype,
            core_attributes: [],
            node_tasks: [],
            default_photo_id: draftDefaultPhotoId,
            suggested_shortcut_id: selectedShortcut?.id
        )

        // Set room relationship
        newNode.room = selectedRoom

        // Set QR code if provided
        if !qrCode.isEmpty {
            newNode.qr_code = qrCode
        }

        // Always set COM and serviceability defaults
        newNode.com = draftCOM
        newNode.com_calculation = draftCOMCalculation
        // ZP-2415: Replacement Cost is part of the Commercial section
        // surfaced in detailed mode. Persist when the user entered one.
        newNode.replacement_cost = draftReplacementCost
        newNode.serviceability = (selectedServiceability ?? .serviceable).rawValue
        if !draftServiceabilityNote.isEmpty { newNode.serviceability_note = draftServiceabilityNote }

        // Set additional fields in detailed mode
        if creationMode == .detailed {
            newNode.voltage = selectedVoltage
            newNode.voltage_id = selectedVoltageId
            newNode.secondary_voltage = selectedSecondaryVoltage
            newNode.secondary_voltage_id = selectedSecondaryVoltageId
            if !draftNotes.isEmpty { newNode.notes = draftNotes }

            // Equipment Library binding is only editable from the
            // Engineering section (detailed + eng-lib). Toggling to
            // Quick hides that section, so a Detailed-mode link must
            // not survive a Quick-mode save — otherwise Android/Web
            // render the library card on an asset whose engineering
            // scalars were never persisted.
            newNode.eqpLibSelection = draftEqpLibSelection

            // ZP-2368: write first-class engineering fields (kVA,
            // impedance, ampere rating, cable/busway specs, etc.).
            // Mirrors EditNodeDetailView's save path. The draft holds
            // nils when the user didn't touch the Engineering section,
            // so applyTo is safe even when the eng-lib flag is off.
            engineeringDraft.applyTo(node: newNode)

            // Apply core attributes
            CoreAttributesService.applyCoreAttributeChanges(
                to: newNode,
                selectedClass: nodeClass,
                originalClass: nil,
                draftAttributes: draftCoreAttributes,
                modelContext: modelContext
            )
        }

        newNode.lastModifiedAt = Date()
        newNode.needsSync = true
        newNode.lastSyncedAt = nil

        // Call the parent's save handler
        // onSave will: 1) insert node, 2) assign photos, 3) insert photos
        onSave(newNode, stagedPhotos, stagedIRPhotos)

        // Link listening tasks to the new node
        if !selectedListeningTaskIds.isEmpty {
            linkListeningTasks(to: newNode, taskIds: selectedListeningTaskIds)
        }
    }

    private func linkListeningTasks(to node: NodeV2, taskIds: Set<UUID>) {
        guard let appState = appState else { return }
        guard let modelContext = modelContext else { return }
        guard let networkState = networkState else { return }
        guard let session = appState.activeSession else { return }

        var reopenedTasks: [UserTask] = []

        for taskId in taskIds {
            guard let task = session.user_tasks.first(where: { $0.id == taskId }) else { continue }
            if !node.node_tasks.contains(where: { $0.id == taskId }) {
                node.node_tasks.append(task)
            }
            if !task.linkedNodes.contains(where: { $0.id == node.id }) {
                task.linkedNodes.append(node)
            }
            if task.completed {
                task.completed = false
                task.submitted_at = nil
                reopenedTasks.append(task)
            }
            task.nodeCompletions[node.id.uuidString] = false
        }

        do {
            try modelContext.save()
        } catch {
            AppLogger.log(.error, "Failed to save listening task links: \(error)", category: .node)
        }

        Task {
            for taskId in taskIds {
                try? await TaskMappingService.shared.addTaskNodes(taskId: taskId, nodeIds: [node.id])
            }
            for task in reopenedTasks {
                if networkState.mode == .online {
                    _ = try? await APIClient.shared.updateTask(task)
                } else {
                    networkState.enqueue(SyncOp(target: .userTask, operation: .update, userTask: task))
                }
            }
        }
    }
}
