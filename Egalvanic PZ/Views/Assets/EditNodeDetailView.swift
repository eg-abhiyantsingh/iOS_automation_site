import SwiftUI
import SwiftData
import UIKit

enum NodeDetailFocusMode {
    case all
    case coreAttributes
    case tasks
    case irPhotos
    case issues
    case ocp            // Child nodes / OCP devices
    case connections    // Edges connected to this node
    case collectAFData  // Core attributes + OCP + connections + photos
    case collectCOMData // Basic info + photos
}

/// Lightweight struct to hold staged IR photo data without creating SwiftData relationships.
/// This prevents view dismissal when adding the first IR photo, since creating an IRPhoto
/// object with node/session relationships can trigger SwiftData observation updates.
struct StagedIRPhotoData: Identifiable {
    let id: UUID
    let visualPhotoKey: String
    let irPhotoKey: String
    let dateCreated: Date
}

struct EditNodeDetailViewV3: View {
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var sldService: SLDService

    // PERFORMANCE: Removed @Query for allNodes which was causing massive re-renders
    // SwiftData objects are reference types, so originalNode stays in sync automatically
    // The previous approach queried ALL nodes and did O(n) search on every render
    private var node: NodeV2 { originalNode }

    let nodeId: UUID
    let originalNode: NodeV2
    let sld: SLDV2
    let showTasksSection: Bool
    let hideNavigationBar: Bool
    let lockRoomSelection: Bool
    let onDismiss: (() -> Void)?
    let focusMode: NodeDetailFocusMode
    /// Optional callback when save starts - allows parent to show loading UI
    let onSaveStarted: (() -> Void)?
    /// Optional callback when save completes - allows parent to hide loading UI
    let onSaveCompleted: (() -> Void)?
    /// Optional active SLD view ID - when set, child nodes created from this view will be added to this view's mapping
    let activeViewId: UUID?

    // Snapshot of original values
    private let originalLabel: String
    private let originalType: String
    private let originalPhotos: [Photo]
    private let originalIRPhotos: [IRPhoto]
    private let originalLocation: String?
    private let originalRoom: Room?
    @State private var originalCoreAttributes: [UUID: String]
    private let originalCOM: Int?
    private let originalCOMCalculation: COMCalculation?
    private let originalNodeClass: NodeClass?
    private let originalNodeSubtype: NodeSubtype?
    private let originalShortcutId: UUID?
    private let originalIssueCount: Int
    private let originalQRCode: String?
    private let originalServiceability: String?
    private let originalServiceabilityNote: String?
    private let originalVoltage: Double?
    private let originalVoltageId: Int?
    private let originalSecondaryVoltage: Double?
    private let originalSecondaryVoltageId: Int?
    private let originalNotes: String?

    // Add this state variable at the top with other issue states:
    @State private var selectedIssueClass: IssueClass?

    // Add this query near the other queries:
    @Query private var allIssueClasses: [IssueClass]
    
    // Draft states
    @State private var draftLabel: String
    @State private var draftType: String
    @State private var draftLocation: String
    @State private var draftCoreAttributes: [UUID: String] = [:]
    @State private var draftCOM: Int?
    @State private var draftCOMCalculation: COMCalculation?
    // ZP-2415: Replacement Cost lives in the Commercial section.
    @State private var draftReplacementCost: Double?
    @State private var draftQRCode: String
    @State private var draftServiceability: Serviceability?
    @State private var draftServiceabilityNote: String
    @State private var draftVoltage: Double?
    @State private var draftVoltageId: Int?
    @State private var draftSecondaryVoltage: Double?
    @State private var draftSecondaryVoltageId: Int?
    // ZP-2161 Phase 3b: every editable engineering scalar / FK id is
    // bundled here so we don't bloat the view with 24 more @State
    // properties. Initialized from ``EngineeringDraft(from: node)`` on
    // appear and written back via ``draft.applyTo(node:)`` on save.
    @State private var engineeringDraft: EngineeringDraft
    @State private var draftNotes: String
    @State private var draftDefaultPhotoId: UUID?
    @State private var isSaving = false
    @State private var selectedNodeClass: NodeClass?
    @State private var selectedNodeSubtype: NodeSubtype?
    @State private var selectedShortcut: NodeShortcut?
    @State private var selectedNodeType: String?
    @State private var selectedRoom: Room?
    @State private var apiError: Error?

    // Equipment Library selection
    @State private var draftEqpLibSelection: EqpLibSelection?

    // Asset class change confirmation
    // The picker writes through `$selectedNodeClass` immediately, so we capture
    // the last confirmed value at init / on confirm and revert on cancel.
    @State private var lastConfirmedNodeClass: NodeClass?
    @State private var showClassChangeConfirmation = false

    // QR code scanner state
    @State private var showQRScanner = false
    @State private var cachedQRImage: UIImage?

    // Task states
    @State private var selectedTask: UserTask?
    @State private var selectedForm: UserTaskForm?
    @State private var showingTaskCreation = false

    // Photo staging (we'll keep this for photos only)
    @State private var displayedPhotos: [Photo]
    @State private var stagedPhotoAdditions: [Photo] = []
    @State private var stagedPhotoDeletions: Set<UUID> = []
    @State private var showingDiscardChangesAlert = false
    
    // IR Photo states
    @State private var visualPhotoKey = ""
    @State private var irPhotoKey = ""
    @State private var stagedIRPhotoData: [StagedIRPhotoData] = []  // Lightweight staging to avoid SwiftData relationship issues
    
    // Issue states
    @State private var showingIssueCreation = false
    @State private var selectedIssue: Issue?
    @State private var issuesModifiedInSession = false
    
    // Core Attributes state
    @State private var showOnlyRequiredAttributes = true

    // COM Calculator state
    @State private var showCOMCalculator = false

    // Child Nodes and Edges states
    @State private var selectedChildNode: NodeV2?
    @State private var selectedEdge: EdgeV2?
    @State private var showingCreateChild = false
    @State private var showingCreateMultiple = false
    @State private var showingOCPWalkthrough = false
    @State private var showingLinkExisting = false

    // Loading overlay state for async operations from child views
    @State private var isPerformingBackgroundOperation = false
    @State private var backgroundOperationMessage = ""

    // Copy asset details state
    @State private var showCopyFromPicker = false
    @State private var showCopyToPicker = false
    @State private var showCopyFieldSelection = false
    @State private var copyDirection: CopyDirection = .from
    @State private var copySelectedNode: NodeV2?
    @State private var showCopyToTarget = false
    @State private var pendingCopyData: CopyAssetData?
    @State private var copySuccessMessage: String?

    // Performance optimization: Removed @Query for currentNodes, currentEdges, irSessions
    // - currentNodes/currentEdges: Now uses sld.nodes/sld.edges directly (same data, no extra query)
    // - irSessions: Use appState.activeSession instead
    @Query private var allNodeClasses: [NodeClass]
    @Query private var allNodeSubtypes: [NodeSubtype]
    @State private var tasksModifiedInSession = false
    @State private var originalTaskCount: Int

    private let api = APIClient.shared
    
    private var profilePhoto: Photo? {
        let availablePhotos = visiblePhotos
        // Use draft default photo ID for immediate UI feedback
        if let draftId = draftDefaultPhotoId,
           let photo = availablePhotos.first(where: { $0.id == draftId && !$0.is_deleted }) {
            return photo
        }
        // Fallback: replicate node.defaultPhoto logic using snapshot data
        if let defaultId = node.default_photo_id,
           let photo = availablePhotos.first(where: { $0.id == defaultId && !$0.is_deleted }) {
            return photo
        }
        if let photo = availablePhotos.first(where: { $0.type == "node_profile" && !$0.is_deleted }) {
            return photo
        }
        return availablePhotos.first(where: { !$0.is_deleted })
    }

    private var titleForFocusMode: String {
        switch focusMode {
        case .all:
            return AppStrings.Assets.assetDetails
        case .coreAttributes:
            return AppStrings.Sessions.collectData
        case .tasks:
            return AppStrings.Sessions.addTask
        case .irPhotos:
            return AppStrings.Sessions.addIRPhotos
        case .issues:
            return AppStrings.Sessions.addIssue
        case .ocp:
            return AppStrings.Sessions.editOCP
        case .connections:
            return AppStrings.Sessions.editConnections
        case .collectAFData:
            return AppStrings.Sessions.collectAFData
        case .collectCOMData:
            return AppStrings.Sessions.collectCOMData
        }
    }
    
    init(node: NodeV2, sld: SLDV2, showTasksSection: Bool = true, hideNavigationBar: Bool = false, lockRoomSelection: Bool = false, onDismiss: (() -> Void)? = nil, focusMode: NodeDetailFocusMode = .all, onSaveStarted: (() -> Void)? = nil, onSaveCompleted: (() -> Void)? = nil, activeViewId: UUID? = nil, prefillData: CopyAssetData? = nil) {
        self.nodeId = node.id
        self.originalNode = node
        self.sld = sld
        self.showTasksSection = showTasksSection
        self.hideNavigationBar = hideNavigationBar
        self.lockRoomSelection = lockRoomSelection
        self.onDismiss = onDismiss
        self.focusMode = focusMode
        self.onSaveStarted = onSaveStarted
        self.onSaveCompleted = onSaveCompleted
        self.activeViewId = activeViewId
        self.originalLabel = node.label
        self.originalType = node.type
        self.originalPhotos = node.photos
        self.originalIRPhotos = node.ir_photos

        // PERFORMANCE: Removed verbose logging that runs on every init
        // print("📸 EditNodeDetailView INIT for node: \(node.label) (ID: \(node.id))")
        self.originalLocation = node.location
        self.originalRoom = node.room
        self.originalCOM = node.com
        self.originalCOMCalculation = node.com_calculation
        self.originalIssueCount = node.issues.filter { !$0.is_deleted }.count
        self.originalQRCode = node.qr_code
        self.originalServiceability = node.serviceability
        self.originalServiceabilityNote = node.serviceability_note
        self.originalVoltage = node.voltage
        self.originalVoltageId = node.voltage_id
        self.originalSecondaryVoltage = node.secondary_voltage
        self.originalSecondaryVoltageId = node.secondary_voltage_id
        self.originalNotes = node.notes

        // Capture original task count
        self.originalTaskCount = node.node_tasks.filter { !$0.is_deleted }.count

        // Build original attributes dictionary (handle duplicates gracefully)
        var originalAttrs: [UUID: String] = [:]
        for attr in node.core_attributes {
            originalAttrs[attr.id] = attr.value
        }
        self._originalCoreAttributes = State(initialValue: originalAttrs)
        self.originalNodeClass = node.node_class
        self.originalNodeSubtype = node.node_subtype
        self.originalShortcutId = node.suggested_shortcut_id
        self._selectedNodeClass = State(initialValue: node.node_class)
        self._lastConfirmedNodeClass = State(initialValue: node.node_class)
        self._selectedNodeSubtype = State(initialValue: node.node_subtype)
        self._selectedShortcut = State(initialValue: nil)  // Will be loaded in onAppear
        self._selectedNodeType = State(initialValue: node.type)

        _draftLabel = State(initialValue: node.label)
        _draftType = State(initialValue: node.type)
        _draftLocation = State(initialValue: node.location ?? "")
        _selectedRoom = State(initialValue: node.room)
        _displayedPhotos = State(initialValue: node.photos)
        _draftCoreAttributes = State(initialValue: originalAttrs)
        _draftCOM = State(initialValue: node.com)
        _draftCOMCalculation = State(initialValue: node.com_calculation)
        // ZP-2415: load Replacement Cost into the Commercial section draft.
        _draftReplacementCost = State(initialValue: node.replacement_cost)
        _draftQRCode = State(initialValue: node.qr_code ?? "")
        _draftServiceability = State(initialValue: node.serviceabilityEnum)
        _draftServiceabilityNote = State(initialValue: node.serviceability_note ?? "")
        _draftVoltage = State(initialValue: node.voltage)
        _draftVoltageId = State(initialValue: node.voltage_id)
        _draftSecondaryVoltage = State(initialValue: node.secondary_voltage)
        _draftSecondaryVoltageId = State(initialValue: node.secondary_voltage_id)
        _engineeringDraft = State(initialValue: EngineeringDraft(from: node))
        _draftNotes = State(initialValue: node.notes ?? "")
        _draftDefaultPhotoId = State(initialValue: node.default_photo_id)
        _draftEqpLibSelection = State(initialValue: node.eqpLibSelection)
        _visualPhotoKey = State(initialValue: "")
        _irPhotoKey = State(initialValue: "")

        // Apply prefill data from "Copy Details To" flow
        if let prefill = prefillData {
            if prefill.selectedFields.contains(.coreAttributes), let attrs = prefill.coreAttributes {
                _draftCoreAttributes = State(initialValue: attrs)
            }
            if prefill.selectedFields.contains(.assetSubtype) {
                _selectedNodeSubtype = State(initialValue: prefill.nodeSubtype)
            }
            if prefill.selectedFields.contains(.serviceability) {
                _draftServiceability = State(initialValue: prefill.serviceability)
                _draftServiceabilityNote = State(initialValue: prefill.serviceabilityNote ?? "")
                _draftCOM = State(initialValue: prefill.com)
                _draftCOMCalculation = State(initialValue: prefill.comCalculation)
            }
            _copySuccessMessage = State(initialValue: "Details copied from \(prefill.sourceLabel). Review and save.")
        }
    }

    // Performance optimization: Use flags instead of filtering arrays on each render
    // tasksModifiedInSession and issuesModifiedInSession are set when tasks/issues change
    private var hasChanges: Bool {
        draftLabel != originalLabel ||
        draftType != originalType ||
        draftLocation != (originalLocation ?? "") ||
        selectedRoom?.id != originalRoom?.id ||
        draftCOM != originalCOM ||
        draftCOMCalculation != originalCOMCalculation ||
        draftReplacementCost != node.replacement_cost ||
        draftQRCode != (originalQRCode ?? "") ||
        draftServiceability?.rawValue != originalServiceability ||
        draftServiceabilityNote != (originalServiceabilityNote ?? "") ||
        draftVoltage != originalVoltage ||
        draftVoltageId != originalVoltageId ||
        draftSecondaryVoltage != originalSecondaryVoltage ||
        draftSecondaryVoltageId != originalSecondaryVoltageId ||
        engineeringDraft.isDirty(against: node) ||
        draftNotes != (originalNotes ?? "") ||
        draftDefaultPhotoId != node.default_photo_id ||
        draftCoreAttributes != originalCoreAttributes ||
        !stagedPhotoAdditions.isEmpty ||
        !stagedPhotoDeletions.isEmpty ||
        !stagedIRPhotoData.isEmpty ||
        tasksModifiedInSession ||
        selectedNodeClass?.id != originalNodeClass?.id ||
        selectedNodeSubtype?.id != originalNodeSubtype?.id ||
        selectedShortcut?.id != originalShortcutId ||
        issuesModifiedInSession ||
        draftEqpLibSelection != node.eqpLibSelection
    }
    
    private var sortedNodeClasses: [NodeClass] {
        allNodeClasses
            .filter { !$0.is_deleted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var filteredNodeSubtypes: [NodeSubtype] {
        guard let selectedClass = selectedNodeClass else { return [] }
        return allNodeSubtypes
            .filter { !$0.is_deleted && $0.node_class_id == selectedClass.id }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var visiblePhotos: [Photo] {
        if isSaving {
            return originalPhotos.filter { !$0.is_deleted }
        }

        let existingPhotos = originalPhotos.filter { photo in
            !photo.is_deleted && !stagedPhotoDeletions.contains(photo.id)
        }
        return existingPhotos + stagedPhotoAdditions
    }

    /// Should show Child Nodes section:
    /// - Node is NOT a child itself (parent_id == nil)
    /// - Node's class has box == true (is an enclosure)
    private var shouldShowChildNodesSection: Bool {
        node.parent_id == nil && (node.node_class?.box ?? false)
    }

    // ZP-2415: COM picker UI now lives inside CommercialSection,
    // shared by Create Asset and Edit Asset.

    var body: some View {
        NavigationStack {
            ZStack {
                ScrollView {
                    VStack(spacing: 0) {
                        // Profile Section
                        ProfileHeaderSection(
                            profilePhoto: profilePhoto,
                            nodeLabel: draftLabel,
                            nodeType: selectedNodeClass?.name ?? AppStrings.AssetsExtra.unknownType,
                            location: selectedRoom?.name,
                            com: draftCOM
                        )
                        
                        // Main Content
                        VStack(spacing: 20) {
                            // Basic Information Card
                            if focusMode == .all || focusMode == .collectCOMData || focusMode == .collectAFData {
                                DetailSection(
                                    title: AppStrings.AssetsExtra.basicInformation,
                                    icon: "info.circle"
                                ) {
                                    AnyView(
                                        VStack(spacing: 12) {
                                        // Asset name
                                        ModernTextField(
                                            title: AppStrings.AssetsExtra.assetName,
                                            text: $draftLabel,
                                            icon: "tag"
                                        )

                                        // Room picker for top-level nodes, parent indicator + inherited room for child nodes
                                        if node.parent_id == nil {
                                            RoomPickerView(
                                                selectedRoom: $selectedRoom,
                                                isLocked: lockRoomSelection,
                                                sld: sld
                                            )
                                        } else {
                                            // Show parent node label for child nodes
                                            HStack(spacing: 12) {
                                                Image(systemName: "square.stack")
                                                    .foregroundColor(.secondary)
                                                    .frame(width: 24, height: 24)

                                                VStack(alignment: .leading, spacing: 2) {
                                                    Text(AppStrings.AssetsExtra.enclosure)
                                                        .font(.caption)
                                                        .foregroundColor(.secondary)
                                                    if let parentId = node.parent_id,
                                                       let parentNode = sld.nodes.first(where: { $0.id == parentId }) {
                                                        Text(parentNode.label)
                                                            .font(.body)
                                                            .foregroundColor(.primary)
                                                    } else {
                                                        Text(AppStrings.AssetsExtra.unknown)
                                                            .font(.body)
                                                            .foregroundColor(.secondary)
                                                    }
                                                }
                                                Spacer()
                                            }
                                            .padding(.vertical, 8)
                                            .padding(.horizontal, 12)
                                            .background(Color(.systemGray6))
                                            .cornerRadius(10)

                                            // ZP-1956: child's room is inherited from the parent enclosure;
                                            // show it locked so users can see the location without being able to diverge it.
                                            RoomPickerView(
                                                selectedRoom: $selectedRoom,
                                                isLocked: true,
                                                sld: sld
                                            )
                                        }

                                        // Node class picker
                                        ModernPicker(
                                            title: AppStrings.Assets.assetClass,
                                            icon: "cube",
                                            placeholder: AppStrings.Assets.selectAssetClass,
                                            items: sortedNodeClasses,
                                            selection: $selectedNodeClass,
                                            displayName: { $0.name },
                                            onSelectionChange: { _ in
                                                handleNodeClassChange()
                                            },
                                            useSheet: true
                                        )

                                        // ZP-2161: subtype picker moved to the
                                        // Engineering card. Subtype change still
                                        // flows through selectedNodeSubtype state
                                        // and triggers the same downstream effects.

                                        // Remaining fields only in full or COM mode
                                        if focusMode != .collectAFData {

                                        // ZP-2415: Suggested PM Plan moved
                                        // into the Commercial section,
                                        // rendered after the Engineering card.

                                        // QR Code field with scanner button
                                        ModernTextField(
                                            title: AppStrings.Assets.qrCode,
                                            text: $draftQRCode,
                                            icon: "qrcode",
                                            trailingContent: {
                                                AnyView(
                                                    Button(action: { showQRScanner = true }) {
                                                        Image(systemName: "qrcode.viewfinder")
                                                            .foregroundColor(.blue)
                                                            .frame(width: 24, height: 24)
                                                    }
                                                )
                                            }
                                        )
                                        .onChange(of: draftQRCode) { _, newValue in
                                            cachedQRImage = newValue.isEmpty ? nil : generateQRCode(from: newValue)
                                        }

                                        // QR Code Preview
                                        if !draftQRCode.isEmpty, let qrImage = cachedQRImage {
                                            VStack(alignment: .leading, spacing: 8) {
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
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .padding(.top, 8)
                                        }

                                        // ZP-2415: COM moved into the
                                        // Commercial section.

                                        // Serviceability (read-only, set by COM Calculator)
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
                                )
                                }
                            }

                            // Tasks Card using EntityLinkedList
                            if showTasksSection && (focusMode == .all || focusMode == .tasks) {
                                EntityLinkedList(
                                    configuration: NodeTaskList(
                                        appState: appState,
                                        onSelectTask: { task in
                                            selectedTask = task
                                        },
                                        onDeleteTask: { task in
                                            deleteTask(task)
                                        },
                                        onAddTask: {
                                            showingTaskCreation = true
                                        }
                                    ),
                                    parent: node
                                )
                                .padding()
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                                // PERFORMANCE: Removed verbose debug logging
                            }

                            // Photos Section
                            if focusMode == .all || focusMode == .coreAttributes || focusMode == .collectAFData || focusMode == .collectCOMData {
                                NodePhotoPickerSection(
                                node: node,
                                displayedPhotos: Binding(
                                    get: { visiblePhotos },
                                    set: { _ in }
                                ),
                                isSaving: $isSaving,
                                onPhotoAdded: { photo in
                                    stagedPhotoAdditions.append(photo)

                                    // Auto-set as default if first photo
                                    if draftDefaultPhotoId == nil {
                                        draftDefaultPhotoId = photo.id
                                    }
                                },
                                onPhotoDeleted: { photo in
                                    if let index = stagedPhotoAdditions.firstIndex(where: { $0.id == photo.id }) {
                                        let removed = stagedPhotoAdditions.remove(at: index)
                                        if let path = removed.local_filepath {
                                            try? FileManager.default.removeItem(atPath: path)
                                        }
                                    } else {
                                        stagedPhotoDeletions.insert(photo.id)
                                    }

                                    // Handle default photo deletion
                                    if draftDefaultPhotoId == photo.id {
                                        let remainingPhotos = visiblePhotos.filter { $0.id != photo.id }
                                        let replacement = remainingPhotos
                                            .sorted { $0.type == "node_profile" && $1.type != "node_profile" }
                                            .first
                                        draftDefaultPhotoId = replacement?.id
                                    }
                                },
                                onSetDefaultPhoto: { photoId in
                                    draftDefaultPhotoId = photoId
                                },
                                draftDefaultPhotoId: draftDefaultPhotoId
                                )
                            }

                            // ZP-2161: combined Engineering card.
                            // Subtype → voltage(s) → first-class
                            // engineering blocks (transformer / box / OCP
                            // / cable / busway) → core attributes — all
                            // in one card, matching the web layout. The
                            // standalone Equipment Library, Voltage, and
                            // Core Attributes cards were rolled into this
                            // single section.
                            // ZP-2161: ``eng-lib`` company feature flag
                            // gates the new engineering section. When
                            // absent, fall back to the legacy split:
                            // a standalone Voltage card + the basic
                            // NodeCoreAttributesSection (custom-attrs
                            // grid + extract-from-photos), no SKM
                            // matcher / configurator / library binding.
                            if focusMode == .all || focusMode == .coreAttributes || focusMode == .collectAFData {
                                if AuthService.shared.hasFeature("eng-lib") {
                                    NodeEngineeringSection(
                                        node: node,
                                        nodeClass: selectedNodeClass,
                                        selectedNodeSubtype: $selectedNodeSubtype,
                                        filteredNodeSubtypes: filteredNodeSubtypes,
                                        draftVoltage: $draftVoltage,
                                        draftVoltageId: $draftVoltageId,
                                        draftSecondaryVoltage: $draftSecondaryVoltage,
                                        draftSecondaryVoltageId: $draftSecondaryVoltageId,
                                        draft: $engineeringDraft,
                                        draftCoreAttributes: $draftCoreAttributes,
                                        eqpLibSelection: $draftEqpLibSelection,
                                        nameplatePhotos: visiblePhotos.filter { $0.type == "node_nameplate" },
                                        onCoreAttrExtractionComplete: { newAttributes in
                                            originalCoreAttributes = newAttributes
                                        }
                                    )
                                    .padding()
                                    .background(Color(.systemBackground))
                                    .cornerRadius(16)
                                    .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                                } else {
                                    // Legacy Voltage card
                                    VStack(alignment: .leading, spacing: 12) {
                                        if selectedNodeClass?.primary_secondary_voltage == true {
                                            HStack(spacing: 12) {
                                                VoltagePickerField(
                                                    label: AppStrings.AssetsExtra.primaryVoltage,
                                                    selectedId: $draftVoltageId,
                                                    selectedValue: $draftVoltage
                                                )
                                                VoltagePickerField(
                                                    label: AppStrings.AssetsExtra.secondaryVoltage,
                                                    selectedId: $draftSecondaryVoltageId,
                                                    selectedValue: $draftSecondaryVoltage
                                                )
                                            }
                                        } else {
                                            VoltagePickerField(
                                                label: AppStrings.AssetsExtra.voltage,
                                                selectedId: $draftVoltageId,
                                                selectedValue: $draftVoltage
                                            )
                                        }
                                    }
                                    .padding()
                                    .background(Color(.systemBackground))
                                    .cornerRadius(16)
                                    .shadow(color: .black.opacity(0.05), radius: 10, y: 5)

                                    // Legacy Core Attributes card
                                    NodeCoreAttributesSection(
                                        node: node,
                                        selectedNodeClass: selectedNodeClass,
                                        draftAttributes: $draftCoreAttributes,
                                        nameplatePhotos: visiblePhotos.filter { $0.type == "node_nameplate" },
                                        onExtractionComplete: { newAttributes in
                                            originalCoreAttributes = newAttributes
                                        }
                                    )
                                    .padding()
                                    .background(Color(.systemBackground))
                                    .cornerRadius(16)
                                    .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                                }
                            }

                            // ZP-2415: Commercial section (COM, Suggested
                            // PM Plan, Replacement Cost) — positioned
                            // after the Engineering card to match web's
                            // AssetFormFields layout. Hidden in
                            // .collectAFData since COM + PM Plan were
                            // already hidden in that focus mode today.
                            if focusMode == .all || focusMode == .collectCOMData {
                                CommercialSection(
                                    draftCOM: $draftCOM,
                                    draftCOMCalculation: $draftCOMCalculation,
                                    selectedShortcut: $selectedShortcut,
                                    draftReplacementCost: $draftReplacementCost,
                                    nodeClass: selectedNodeClass,
                                    nodeSubtype: selectedNodeSubtype,
                                    sld: sld,
                                    onShowCOMCalculator: {
                                        showCOMCalculator = true
                                    },
                                    showSuggestedPMPlan: true
                                )
                            }

                            // IR Photos Section (if available)
                            if (focusMode == .all || focusMode == .irPhotos) && (appState.activeSession != nil || !getExistingIRPhotos().isEmpty) {
                                IRPhotosCard(
                                    existingIRPhotos: getExistingIRPhotos(),
                                    visualPhotoKey: $visualPhotoKey,
                                    irPhotoKey: $irPhotoKey,
                                    stagedIRPhotoData: $stagedIRPhotoData,
                                    onAddIRPhoto: {
                                        addIRPhotoPair()
                                    }
                                )
                                .padding()
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }

                            // Issues Card using EntityLinkedList
                            if focusMode == .all || focusMode == .issues {
                                EntityLinkedList(
                                configuration: NodeIssueList(
                                    appState: appState,
                                    onSelectIssue: { issue in
                                        selectedIssue = issue
                                    },
                                    onDeleteIssue: { issue in
                                        deleteIssue(issue)
                                    },
                                    onAddIssue: {
                                        showingIssueCreation = true
                                    }
                                ),
                                parent: node
                                )
                                .padding()
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }

                            // Child Nodes / OCP Section (only for non-child nodes with box=true)
                            if (focusMode == .all || focusMode == .ocp || focusMode == .collectAFData) && shouldShowChildNodesSection {
                                ChildNodesSection(
                                    node: node,
                                    sld: sld,
                                    activeSession: appState.activeSession,
                                    activeViewId: activeViewId,
                                    onSelectChild: { child in
                                        selectedChildNode = child
                                    },
                                    onCreateChild: {
                                        showingCreateChild = true
                                    },
                                    onCreateMultiple: {
                                        showingCreateMultiple = true
                                    },
                                    onPhotoWalkthrough: {
                                        showingOCPWalkthrough = true
                                    },
                                    onLinkExisting: {
                                        showingLinkExisting = true
                                    }
                                )
                            }

                            // Connections / Edges Section (only for non-child nodes)
                            if node.parent_id == nil && (focusMode == .all || focusMode == .connections || focusMode == .collectAFData) {
                                NodeEdgesSection(
                                    node: node,
                                    networkState: networkState,
                                    sld: sld,
                                    activeViewId: activeViewId,
                                    onSelectEdge: { edge in
                                        selectedEdge = edge
                                    }
                                )
                            }

                            // Notes Section — last card on the page so
                            // catch-all freeform context sits below the
                            // structured data above.
                            if focusMode == .all {
                                VStack(alignment: .leading, spacing: 12) {
                                    SectionHeader(title: AppStrings.Common.notes, systemImage: "note.text")
                                    TextEditor(text: $draftNotes)
                                        .frame(minHeight: 80)
                                        .padding(8)
                                        .background(Color(.systemGray6))
                                        .cornerRadius(10)
                                        .overlay(
                                            Group {
                                                if draftNotes.isEmpty {
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
                        .padding()
                        .padding(.bottom, hasChanges ? 80 : 20) // Reduced padding since we removed cancel button
                    }
                }
                .scrollDismissesKeyboard(.interactively)
                .background(Color(.systemGray6))
                .onTapGesture {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }


                // Bottom Action Bar
                if hasChanges {
                    VStack {
                        Spacer()
                        
                        VStack(spacing: 0) {
                            // Add a background that extends to cover the tab bar
                            Rectangle()
                                .fill(Color(.systemBackground))
                                .frame(height: 0)
                                .shadow(color: .black.opacity(0.1), radius: 10, y: -5)
                            
                            Button(action: saveAndSync) {
                                if isSaving {
                                    HStack {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            .scaleEffect(0.8)
                                        Text(AppStrings.AssetsExtra.saving)
                                            .foregroundColor(.white)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 16)
                                    .background(Color.blue.opacity(0.6))
                                    .cornerRadius(12)
                                } else {
                                    Text(AppStrings.AssetsExtra.saveChanges)
                                        .fontWeight(.semibold)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 16)
                                        .background(Color.blue)
                                        .foregroundColor(.white)
                                        .cornerRadius(12)
                                }
                            }
                            .disabled(isSaving)
                            .padding(.horizontal, 20)
                            .padding(.top, 16)
                            .padding(.bottom, 18)
                            .background(Color(.systemBackground))
                        }
                    }
                    .ignoresSafeArea()
                }

                // Loading overlay for save and background operations
                if isSaving || isPerformingBackgroundOperation {
                    Color.black.opacity(0.5)
                        .ignoresSafeArea()
                        .overlay(
                            VStack(spacing: 16) {
                                ProgressView()
                                    .scaleEffect(1.8)
                                    .tint(.blue)
                                Text(isSaving ? AppStrings.AssetsExtra.saving : backgroundOperationMessage)
                                    .font(.headline)
                                    .fontWeight(.medium)
                                    .foregroundColor(.primary)
                            }
                            .padding(32)
                            .background(Color(.systemBackground))
                            .cornerRadius(16)
                            .shadow(color: .black.opacity(0.2), radius: 10, y: 5)
                        )
                        .transition(.opacity)
                        .animation(.easeInOut(duration: 0.2), value: isSaving || isPerformingBackgroundOperation)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(hasChanges || hideNavigationBar)
            .navigationTitle(hideNavigationBar ? "" : titleForFocusMode)
            .navigationBarHidden(hideNavigationBar)
            .toolbar(hideNavigationBar ? .hidden : .visible, for: .navigationBar)
            .toolbar {
                if !hideNavigationBar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        if hasChanges {
                            Button(AppStrings.Common.cancel) {
                                showingDiscardChangesAlert = true
                            }
                            .foregroundColor(.red)
                        } else {
                            Button(AppStrings.CommonExtra.close) {
                                dismiss()
                            }
                        }
                    }

                    if focusMode == .all && selectedNodeClass != nil {
                        ToolbarItem(placement: .navigationBarTrailing) {
                            Menu {
                                Button {
                                    copyDirection = .from
                                    showCopyFromPicker = true
                                } label: {
                                    Label(AppStrings.AssetsExtra.copyDetailsFrom, systemImage: "doc.on.doc")
                                }

                                Button {
                                    copyDirection = .to
                                    showCopyToPicker = true
                                } label: {
                                    Label(AppStrings.AssetsExtra.copyDetailsTo, systemImage: "arrow.right.doc.on.clipboard")
                                }
                            } label: {
                                Image(systemName: "ellipsis.circle")
                            }
                        }
                    }
                }
            }
        }
        .onAppear {
            setupDefaultPhotoKeys()

            // Generate initial QR code preview if one exists
            if !draftQRCode.isEmpty {
                cachedQRImage = generateQRCode(from: draftQRCode)
            }

            // Auto-open creation sheets based on focus mode
            switch focusMode {
            case .tasks:
                showingTaskCreation = true
            case .issues:
                showingIssueCreation = true
            default:
                break
            }

            // Load existing shortcut if present
            AppLogger.log(.debug, "[EditNodeDetail] Loading shortcut - Node: \(node.label) (ID: \(node.id)), Class: \(node.node_class?.name ?? "None"), Subtype: \(node.node_subtype?.name ?? "None"), shortcutId: \(node.suggested_shortcut_id?.uuidString ?? "nil")", category: .node)
            if let shortcutId = node.suggested_shortcut_id {
                Task { @MainActor in
                    if let shortcut = try? ShortcutService.getShortcut(by: shortcutId, in: context) {
                        selectedShortcut = shortcut
                        AppLogger.log(.debug, "[EditNodeDetail] Found shortcut: \(shortcut.name) (ID: \(shortcut.id))", category: .node)
                    } else {
                        AppLogger.log(.notice, "[EditNodeDetail] Shortcut with ID \(shortcutId) not found in database", category: .node)
                    }
                }
            } else {
                AppLogger.log(.debug, "[EditNodeDetail] No shortcut ID on node", category: .node)
            }
        }
        .environmentObject(networkState)
        .fullScreenCover(isPresented: $showQRScanner) {
            QRCodeScannerView(scannedCode: $draftQRCode) { code in
                AppLogger.log(.info, "QR Code scanned: \(code)", category: .node)
            }
        }
        .fullScreenCover(item: $selectedTask) { task in
            TaskDetailView(task: task, showNodesSection: true, contextNode: node)
        }
        .fullScreenCover(item: $selectedIssue) { issue in
            IssueDetailView(issue: issue, stagedIRPhotos: [])
        }
        .fullScreenCover(item: $selectedChildNode) { child in
            // EditNodeDetailViewV3 has its own NavigationStack, don't wrap it
            EditNodeDetailViewV3(
                node: child,
                sld: sld,
                showTasksSection: showTasksSection,
                onSaveStarted: {
                    // Show loading overlay in parent while child node saves
                    isPerformingBackgroundOperation = true
                    backgroundOperationMessage = AppStrings.AssetsExtra.savingAsset
                },
                onSaveCompleted: {
                    // Hide loading overlay when done
                    isPerformingBackgroundOperation = false
                    backgroundOperationMessage = ""
                }
            )
        }
        .fullScreenCover(item: $selectedEdge) { edge in
            // EditEdgeDetailViewV3 has its own NavigationStack, don't wrap it
            // Pass the current node as context to restrict source/target options
            EditEdgeDetailViewV3(
                edge: edge,
                contextNode: node,
                onSaveStarted: {
                    // Show loading overlay in parent while edge saves
                    isPerformingBackgroundOperation = true
                    backgroundOperationMessage = AppStrings.AssetsExtra.savingConnection
                },
                onSaveCompleted: {
                    // Hide loading overlay when done
                    isPerformingBackgroundOperation = false
                    backgroundOperationMessage = ""
                }
            )
        }
        .fullScreenCover(isPresented: $showingTaskCreation) {
            UnifiedTaskCreationFromNodeView(
                node: node,
                sld: sld
            ) { task in
                AppLogger.log(.info, "Created task: \(task.title)", category: .node)
                // Refresh tasks if needed
            }
        }
        .sheet(isPresented: $showingIssueCreation) {
            UnifiedIssueCreationFromNodeView(
                node: node,
                sld: sld
            ) { issue in
                AppLogger.log(.info, "Created issue: \(issue.title ?? "Untitled")", category: .node)
                issuesModifiedInSession = true
                // Refresh issues if needed
            }
        }
        .sheet(isPresented: $showingCreateChild) {
            CreateChildNodeView(
                parent: node,
                sld: sld,
                activeSession: appState.activeSession,
                activeViewId: activeViewId,
                onCreated: { _ in
                    showingCreateChild = false
                }
            )
        }
        .sheet(isPresented: $showingCreateMultiple) {
            CreateMultipleOCPView(
                parent: node,
                sld: sld,
                activeSession: appState.activeSession,
                activeViewId: activeViewId,
                onComplete: {
                    showingCreateMultiple = false
                }
            )
        }
        .fullScreenCover(isPresented: $showingOCPWalkthrough) {
            OCPPhotoWalkthroughView(
                parent: node,
                sld: sld,
                activeSession: appState.activeSession,
                activeViewId: activeViewId,
                onComplete: {
                    showingOCPWalkthrough = false
                },
                onCancel: {
                    showingOCPWalkthrough = false
                }
            )
        }
        .fullScreenCover(isPresented: $showingLinkExisting) {
            LinkExistingNodeView(
                parent: node,
                sld: sld,
                activeSession: appState.activeSession,
                activeViewId: activeViewId,
                onLinked: {
                    showingLinkExisting = false
                }
            )
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
        .alert(AppStrings.Assets.saveFailed, isPresented: Binding(
            get: { apiError != nil },
            set: { if !$0 { apiError = nil } }
        )) {
            Button(AppStrings.Common.ok) { apiError = nil }
        } message: {
            Text(apiError?.localizedDescription ?? AppStrings.AssetsExtra.unknownError)
        }
        .alert(AppStrings.Alerts.discardChanges, isPresented: $showingDiscardChangesAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Common.discard, role: .destructive) {
                dismiss()
            }
        } message: {
            Text(AppStrings.Alerts.discardChangesMessage)
        }
        .alert(AppStrings.AssetsExtra.changeAssetClassTitle, isPresented: $showClassChangeConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                cancelNodeClassChange()
            }
            Button(AppStrings.AssetsExtra.changeAssetClassConfirm, role: .destructive) {
                confirmNodeClassChange()
            }
        } message: {
            Text(AppStrings.AssetsExtra.changeAssetClassMessage)
        }
        .sheet(isPresented: $showCopyFromPicker) {
            if let nodeClass = selectedNodeClass {
                CopyAssetDetailsPickerView(
                    sld: sld,
                    currentNode: node,
                    nodeClass: nodeClass,
                    direction: .from
                ) { selectedNode in
                    copySelectedNode = selectedNode
                    copyDirection = .from
                    showCopyFieldSelection = true
                }
            }
        }
        .sheet(isPresented: $showCopyToPicker) {
            if let nodeClass = selectedNodeClass {
                CopyAssetDetailsPickerView(
                    sld: sld,
                    currentNode: node,
                    nodeClass: nodeClass,
                    direction: .to
                ) { selectedNode in
                    copySelectedNode = selectedNode
                    copyDirection = .to
                    showCopyFieldSelection = true
                }
            }
        }
        .sheet(isPresented: $showCopyFieldSelection, onDismiss: {
            // Clear stale node reference if user cancelled without applying
            if copyDirection == .from {
                copySelectedNode = nil
            }
        }) {
            if let selectedNode = copySelectedNode {
                CopyAssetFieldSelectionView(
                    sourceNodeLabel: copyDirection == .from ? selectedNode.label : node.label,
                    direction: copyDirection
                ) { fields in
                    if copyDirection == .from {
                        applyCopyFrom(source: selectedNode, fields: fields)
                    } else {
                        prepareCopyTo(target: selectedNode, fields: fields)
                    }
                }
            }
        }
        .fullScreenCover(isPresented: $showCopyToTarget, onDismiss: {
            copySelectedNode = nil
            pendingCopyData = nil
        }) {
            if let target = copySelectedNode, let data = pendingCopyData {
                EditNodeDetailViewV3(
                    node: target,
                    sld: sld,
                    prefillData: data
                )
            }
        }
        .alert(AppStrings.AssetsExtra.copyCompleteAlert, isPresented: Binding(
            get: { copySuccessMessage != nil },
            set: { if !$0 { copySuccessMessage = nil } }
        )) {
            Button(AppStrings.Common.ok) { copySuccessMessage = nil }
        } message: {
            Text(copySuccessMessage ?? "")
        }
    }
    
    /// Called after the picker has written the new value into selectedNodeClass.
    /// We don't apply any clears yet — instead, show a confirmation listing the
    /// class-specific fields that will be wiped. confirm/cancelNodeClassChange
    /// handle the two outcomes.
    private func handleNodeClassChange() {
        if selectedNodeClass?.id == lastConfirmedNodeClass?.id { return }
        showClassChangeConfirmation = true
    }

    private func confirmNodeClassChange() {
        let newNodeClass = selectedNodeClass

        // Clear every class-specific field listed in the confirmation warning:
        // manufacturer & model, trip configuration, nameplate fields,
        // Condition of Maintenance, suggested PM plan (shortcut), and
        // engineering custom attributes — plus the Equipment Library
        // selection, whose category/frame/SKM bindings are also class-specific.
        selectedNodeSubtype = nil
        selectedShortcut = nil
        selectedNodeType = newNodeClass?.style
        // ZP-1432 / ZP-1783: carry over core-attribute values whose property
        // names match between the old and new class, so users don't re-enter
        // the same nameplate values across similar classes. Attributes with no
        // match on the new class are dropped — matching the warning copy. When
        // the class is being cleared entirely there's no target to map onto,
        // so just wipe.
        if let newClass = newNodeClass {
            draftCoreAttributes = CoreAttributesService.preserveAttributeValues(
                from: lastConfirmedNodeClass,
                to: newClass,
                currentAttributes: draftCoreAttributes
            )
        } else {
            draftCoreAttributes = [:]
        }
        // Reset Condition of Maintenance to the system default (1).
        draftCOM = 1
        draftCOMCalculation = nil
        engineeringDraft = EngineeringDraft()
        draftEqpLibSelection = nil

        // Clear any in-progress copy state since class changed
        copySelectedNode = nil
        pendingCopyData = nil

        lastConfirmedNodeClass = newNodeClass
    }

    private func cancelNodeClassChange() {
        // Revert the picker binding to the previously confirmed class
        selectedNodeClass = lastConfirmedNodeClass
    }

    private func applyCopyFrom(source: NodeV2, fields: Set<CopyableField>) {
        // Guard: source must still be same class as current node
        guard source.node_class?.id == selectedNodeClass?.id else {
            copySuccessMessage = nil
            return
        }

        if fields.contains(.coreAttributes) {
            var sourceAttrs: [UUID: String] = [:]
            for attr in source.core_attributes {
                sourceAttrs[attr.id] = attr.value
            }
            draftCoreAttributes = sourceAttrs
        }
        if fields.contains(.assetSubtype) {
            selectedNodeSubtype = source.node_subtype
        }
        if fields.contains(.serviceability) {
            draftServiceability = source.serviceabilityEnum
            draftServiceabilityNote = source.serviceability_note ?? ""
            draftCOM = source.com
            draftCOMCalculation = source.com_calculation
        }
        copySuccessMessage = "Details copied from \(source.label). Review and save."
    }

    private func prepareCopyTo(target: NodeV2, fields: Set<CopyableField>) {
        var attrs: [UUID: String]?
        if fields.contains(.coreAttributes) {
            attrs = draftCoreAttributes
        }
        pendingCopyData = CopyAssetData(
            coreAttributes: attrs,
            nodeSubtype: fields.contains(.assetSubtype) ? selectedNodeSubtype : nil,
            serviceability: fields.contains(.serviceability) ? draftServiceability : nil,
            serviceabilityNote: fields.contains(.serviceability) ? draftServiceabilityNote : nil,
            com: fields.contains(.serviceability) ? draftCOM : nil,
            comCalculation: fields.contains(.serviceability) ? draftCOMCalculation : nil,
            selectedFields: fields,
            sourceLabel: draftLabel
        )
        showCopyToTarget = true
    }

    private func getExistingIRPhotos() -> [IRPhoto] {
        return originalIRPhotos.filter { !$0.is_deleted }
    }
    
    private func addIRPhotoPair() {
        guard let session = appState.activeSession else { return }

        // Store as lightweight data struct to avoid SwiftData relationship triggers
        let stagedData = StagedIRPhotoData(
            id: UUID(),
            visualPhotoKey: session.photo_type == "FLIR-IND" ? "" : visualPhotoKey,
            irPhotoKey: irPhotoKey,
            dateCreated: Date()
        )

        stagedIRPhotoData.append(stagedData)

        // Track last used keys locally for next photo key setup
        // Note: We defer updating appState to avoid triggering parent view re-renders
        let lastVisual = visualPhotoKey
        let lastIR = irPhotoKey

        // Clear and setup next default values
        visualPhotoKey = ""
        irPhotoKey = ""

        // Setup next keys based on session type
        if session.photo_type == "FLIR-SEP" {
            if let nextVisual = IRPhotoService.incrementPhotoKey(lastVisual, by: 2) {
                visualPhotoKey = nextVisual
            }
            if let nextIR = IRPhotoService.incrementPhotoKey(lastIR, by: 2) {
                irPhotoKey = nextIR
            }
        } else if session.photo_type == "FLIR-IND" {
            if let nextIR = IRPhotoService.incrementPhotoKey(lastIR, by: 1) {
                irPhotoKey = nextIR
            }
        }
    }
    
    private func deleteTask(_ task: UserTask) {
        TaskService.deleteTask(
            task,
            modelContext: context
        ) { success, message in
            if success {
                tasksModifiedInSession = true
                if let errorMessage = message {
                    AppLogger.log(.info, errorMessage, category: .node)
                }
            } else {
                apiError = NSError(
                    domain: "TaskDeletion",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: message ?? "Failed to delete task"]
                )
            }
        }
    }

    private func deleteIssue(_ issue: Issue) {
        IssueService.deleteIssue(
            issue,
            modelContext: context
        ) { success, message in
            if success {
                issuesModifiedInSession = true
                if let errorMessage = message {
                    AppLogger.log(.info, errorMessage, category: .node)
                }
            } else {
                apiError = NSError(
                    domain: "IssueDeletion",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: message ?? "Failed to delete issue"]
                )
            }
        }
    }

    private func saveAndSync() {
        Task {
            await performSave()
        }
    }

    private func intOrNil(_ v: Int?) -> String { v.map(String.init) ?? "nil" }
    
    private func performSave() async {
        isSaving = true

        // Notify parent that save is starting (allows showing loading overlay)
        onSaveStarted?()

        // Convert staged IR photo data to actual IRPhoto objects at save time
        // This is done here to avoid creating SwiftData relationships during editing
        var irPhotosToSave: [IRPhoto] = []
        if let session = appState.activeSession {
            for data in stagedIRPhotoData {
                let irPhoto = IRPhoto(
                    id: data.id,
                    ir_session: session,
                    node: node,
                    sld: sld,
                    visual_photo_key: data.visualPhotoKey,
                    ir_photo_key: data.irPhotoKey,
                    date_created: data.dateCreated
                )
                irPhotosToSave.append(irPhoto)
            }

            // Update appState with the last used keys for future photos
            if let lastData = stagedIRPhotoData.last {
                if session.photo_type == "FLIR-SEP" {
                    appState.setLastCreatedVisualPhotoKey(lastData.visualPhotoKey)
                    appState.setLastCreatedIRPhotoKey(lastData.irPhotoKey)
                } else if session.photo_type == "FLIR-IND" {
                    appState.setLastCreatedIRPhotoKey(lastData.irPhotoKey)
                }
            }
        }

        // Create the update data structure
        // Performance optimization: Pass sld directly instead of copying all nodes/edges arrays
        let updateData = NodeUpdateData(
            label: draftLabel,
            type: selectedNodeType ?? draftType,
            location: draftLocation,
            room: selectedRoom,
            com: draftCOM,
            comCalculation: draftCOMCalculation,
            replacementCost: draftReplacementCost,
            qrCode: draftQRCode.isEmpty ? nil : draftQRCode,
            serviceability: (draftServiceability ?? .serviceable).rawValue,
            serviceabilityNote: draftServiceabilityNote.isEmpty ? nil : draftServiceabilityNote,
            voltage: draftVoltage,
            voltageId: draftVoltageId,
            secondaryVoltage: draftSecondaryVoltage,
            secondaryVoltageId: draftSecondaryVoltageId,
            notes: draftNotes.isEmpty ? nil : draftNotes,
            nodeClass: selectedNodeClass,
            nodeSubtype: selectedNodeSubtype,
            originalNodeClass: originalNodeClass,
            coreAttributes: draftCoreAttributes,
            stagedIRPhotos: irPhotosToSave,
            stagedPhotoAdditions: stagedPhotoAdditions,
            stagedPhotoDeletions: stagedPhotoDeletions,
            originalPhotos: originalPhotos,
            displayedPhotos: displayedPhotos,
            sld: sld,
            defaultPhotoId: draftDefaultPhotoId,
            suggestedShortcutId: selectedShortcut?.id,
            eqpLibSelection: draftEqpLibSelection
        )

        // ZP-2161: write the engineering-section draft scalars / FKs
        // onto the node before NodeService.updateNode picks it up. The
        // node-level fields (voltage, secondary_voltage, etc.) are
        // already on `updateData`; the engineering bundle is set
        // directly on the entity so the same backend PUT payload
        // carries it through (the API client serializes the whole node).
        var draftLog: [String: Any] = [:]
        draftLog["node_id"] = node.id.uuidString
        draftLog["draft.ampere_rating"] = intOrNil(engineeringDraft.ampere_rating)
        draftLog["draft.pole_count"] = intOrNil(engineeringDraft.pole_count)
        draftLog["draft.manufacturer_id"] = intOrNil(engineeringDraft.manufacturer_id)
        draftLog["draft.trip_type_id"] = intOrNil(engineeringDraft.trip_type_id)
        draftLog["draft.frame_amps"] = intOrNil(engineeringDraft.frame_amps)
        draftLog["draft.sensor_amps"] = intOrNil(engineeringDraft.sensor_amps)
        draftLog["draft.plug_amps"] = intOrNil(engineeringDraft.plug_amps)
        draftLog["node.ampere_rating (pre-apply)"] = intOrNil(node.ampere_rating)
        draftLog["node.frame_amps (pre-apply)"] = intOrNil(node.frame_amps)
        slog("performSave engineering draft", category: .general, data: draftLog)

        engineeringDraft.applyTo(node: node)

        var appliedLog: [String: Any] = [:]
        appliedLog["node_id"] = node.id.uuidString
        appliedLog["node.ampere_rating"] = intOrNil(node.ampere_rating)
        appliedLog["node.pole_count"] = intOrNil(node.pole_count)
        appliedLog["node.manufacturer_id"] = intOrNil(node.manufacturer_id)
        appliedLog["node.trip_type_id"] = intOrNil(node.trip_type_id)
        appliedLog["node.frame_amps"] = intOrNil(node.frame_amps)
        appliedLog["node.sensor_amps"] = intOrNil(node.sensor_amps)
        appliedLog["node.plug_amps"] = intOrNil(node.plug_amps)
        slog("performSave engineering applied", category: .general, data: appliedLog)

        // Use NodeService to handle the update
        // Skip graph update when in a view context - parent will handle refresh with view-filtered nodes
        do {
            try await NodeService.updateNode(
                node,
                with: updateData,
                sldService: sldService,
                networkState: networkState,
                modelContext: context,
                skipGraphUpdate: activeViewId != nil
            )

            await MainActor.run {
                stagedPhotoAdditions.removeAll()
                stagedPhotoDeletions.removeAll()
                stagedIRPhotoData.removeAll()
                isSaving = false

                // Notify parent that save is complete
                onSaveCompleted?()

                // Call custom dismiss if provided (for fullScreenCover), otherwise use environment dismiss
                if let onDismiss = onDismiss {
                    onDismiss()
                } else {
                    dismiss()
                }
            }
        } catch {
            isSaving = false
            onSaveCompleted?()
            apiError = error
        }
    }
    
    private func setupDefaultPhotoKeys() {
        let keys = IRPhotoService.setupDefaultPhotoKeys(
            for: appState.activeSession,
            lastVisualKey: appState.lastCreatedVisualPhotoKey,
            lastIRKey: appState.lastCreatedIRPhotoKey
        )
        visualPhotoKey = keys.visualKey
        irPhotoKey = keys.irKey
    }

    private func generateQRCode(from string: String) -> UIImage? {
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

}

// MARK: - Supporting Views
struct IRPhotosCard: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var networkState: NetworkState

    let existingIRPhotos: [IRPhoto]
    @Binding var visualPhotoKey: String
    @Binding var irPhotoKey: String
    @Binding var stagedIRPhotoData: [StagedIRPhotoData]
    let onAddIRPhoto: () -> Void

    @State private var selectedIRPhotoForViewer: IRPhoto?
    /// Pre-fetched presigned URLs for all existing IR photos (batch fetched on appear)
    @State private var irPhotoPresignedURLs: [String: String] = [:]
    /// Whether batch presigned URL fetch has completed (gates thumbnail rendering to avoid N individual calls)
    @State private var hasLoadedPresignedURLs = false
    @State private var photoToEdit: IRPhoto?
    @State private var photoToDelete: IRPhoto?
    @State private var editErrorMessage: String?
    @State private var showEditError = false

    private var isVisualPhotoDisabled: Bool {
        appState.activeSession?.photo_type == "FLIR-IND"
    }

    private var isFlirInd: Bool {
        appState.activeSession?.photo_type == "FLIR-IND"
    }

    private var canAddPhoto: Bool {
        if appState.activeSession?.photo_type == "FLIR-IND" {
            return !irPhotoKey.isEmpty
        } else {
            return !visualPhotoKey.isEmpty && !irPhotoKey.isEmpty
        }
    }

    var body: some View {
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
                                Text(AppStrings.Sessions.typeLabel(session.photo_type))
                                    .font(.caption)
                                    .foregroundColor(.secondary)

                                if session.photo_type == "FLIR-IND" {
                                    Label(AppStrings.Assets.irOnly, systemImage: "info.circle")
                                        .font(.caption2)
                                        .foregroundColor(.blue)
                                }
                            }
                        } else {
                            HStack {
                                Image(systemName: "exclamationmark.circle")
                                    .foregroundColor(.yellow)
                                Text(AppStrings.AssetsExtra.noActiveIRSession)
                                    .font(.subheadline)
                            }
                            Text(AppStrings.AssetsExtra.selectSessionFromSessions)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                .cornerRadius(10)

                // Input fields if session is active
                if appState.activeSession != nil {
                    VStack(spacing: 12) {
                        // IR Photo field (always shown first)
                        ModernTextField(
                            title: AppStrings.Assets.irPhotoFilename,
                            text: $irPhotoKey,
                            icon: "camera.filters"
                        )

                        // Visual Photo field - disabled for FLIR-IND
                        if !isVisualPhotoDisabled {
                            ModernTextField(
                                title: AppStrings.Assets.visualPhotoFilename,
                                text: $visualPhotoKey,
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

                        Button(action: onAddIRPhoto) {
                            Label(AppStrings.Assets.addIRPhotoPair, systemImage: "plus.circle.fill")
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(canAddPhoto ? Color.blue : Color.gray)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                        .disabled(!canAddPhoto)
                    }
                }

                // Existing and staged photos
                if !existingIRPhotos.isEmpty || !stagedIRPhotoData.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        if !existingIRPhotos.isEmpty {
                            Text(AppStrings.AssetsExtra.existingIRPhotos)
                                .font(.caption)
                                .foregroundColor(.secondary)

                            // Wait for batch presigned URL fetch before rendering thumbnails
                            // to avoid N individual API calls. Skip wait if offline (use cache).
                            if hasLoadedPresignedURLs || networkState.mode != .online {
                                ForEach(existingIRPhotos) { irPhoto in
                                    IRPhotoRow(
                                        irPhoto: irPhoto,
                                        isOnline: networkState.mode == .online,
                                        onTap: { selectedIRPhotoForViewer = irPhoto },
                                        irPhotoPresignedURLs: irPhotoPresignedURLs,
                                        onEdit: { photoToEdit = irPhoto },
                                        onDelete: { photoToDelete = irPhoto }
                                    )
                                }
                            } else {
                                HStack {
                                    Spacer()
                                    ProgressView()
                                        .scaleEffect(0.8)
                                    Spacer()
                                }
                                .padding(.vertical, 8)
                            }
                        }

                        if !stagedIRPhotoData.isEmpty {
                            Text(AppStrings.Assets.newIRPhotos)
                                .font(.caption)
                                .foregroundColor(.secondary)

                            ForEach(stagedIRPhotoData) { data in
                                StagedIRPhotoRow(data: data, isFlirInd: isFlirInd) {
                                    stagedIRPhotoData.removeAll { $0.id == data.id }
                                }
                            }
                        }
                    }
                }
            }
        }
        .fullScreenCover(item: $selectedIRPhotoForViewer) { photo in
            IRFullImageView(
                irPhotos: existingIRPhotos,
                initialPhoto: photo,
                isOnline: networkState.mode == .online,
                irPhotoPresignedURLs: irPhotoPresignedURLs
            )
        }
        .task(id: existingIRPhotos.map(\.id)) {
            await loadPresignedURLs(markLoaded: true)
        }
        .sheet(item: $photoToEdit) { photo in
            IRPhotoEditSheet(
                irKey: photo.ir_photo_key,
                visKey: photo.visual_photo_key,
                isFlirInd: photo.ir_session?.photo_type == "FLIR-IND",
                onSave: { irKey, visKey in
                    savePhotoEdit(photo: photo, irKey: irKey, visKey: visKey)
                }
            )
        }
        .alert(
            AppStrings.Sessions.deleteIRPhoto,
            isPresented: Binding(
                get: { photoToDelete != nil },
                set: { if !$0 { photoToDelete = nil } }
            )
        ) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                photoToDelete = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let photo = photoToDelete {
                    deletePhoto(photo)
                }
                photoToDelete = nil
            }
        } message: {
            Text(AppStrings.Sessions.deleteIRPhotoConfirm)
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showEditError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(editErrorMessage ?? "")
        }
    }

    // MARK: - Edit / Delete

    private func savePhotoEdit(photo: IRPhoto, irKey: String, visKey: String) {
        // Drop only the presigned URL entries — the URL signs the old S3 path which
        // is now stale. Keep the in-memory image cache: cacheKey is photo-ID-based,
        // and the image bytes are unchanged by a rename, so the cached image stays
        // valid and visible while the new presigned URL is refetched.
        irPhotoPresignedURLs.removeValue(forKey: "\(photo.id.uuidString)_ir")
        irPhotoPresignedURLs.removeValue(forKey: "\(photo.id.uuidString)_vis")

        // FLIR-IND: visual key is server-managed. Rewrite it from the new IR base
        // so the visual file stays paired when the user renames the IR key.
        let finalVisKey: String
        if photo.ir_session?.photo_type == "FLIR-IND", !photo.visual_photo_key.isEmpty {
            let visExt = (photo.visual_photo_key as NSString).pathExtension
            let irBase = (irKey as NSString).deletingPathExtension
            finalVisKey = visExt.isEmpty ? "\(irBase)_visual" : "\(irBase)_visual.\(visExt)"
        } else {
            finalVisKey = visKey
        }

        photo.ir_photo_key = irKey
        photo.visual_photo_key = finalVisKey

        do {
            try modelContext.save()
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            editErrorMessage = "Failed to save: \(error.localizedDescription)"
            showEditError = true
            return
        }

        if networkState.mode == .online {
            Task {
                do {
                    _ = try await APIClient.shared.updateIRPhoto(photo)

                    // FLIR-IND: the visual file is server-extracted from the IR
                    // file and stored at "<ir_key_base>_visual.<ext>". The IR
                    // update endpoint renames the IR S3 object but does not
                    // re-extract the visual at the new path, so trigger
                    // extraction explicitly. Best-effort — manual extract menu
                    // is still available if this fails.
                    if photo.ir_session?.photo_type == "FLIR-IND", !photo.visual_photo_key.isEmpty {
                        _ = try? await APIClient.shared.extractVisualFromIRPhotos(photoIds: [photo.id])
                    }

                    // Brief delay before refetching presigned URLs: gives S3 time to
                    // propagate the rename so the new presigned URL resolves to the
                    // freshly renamed object instead of 404-ing.
                    try? await Task.sleep(for: .seconds(2))
                    // Don't toggle hasLoadedPresignedURLs — that would hide the row list.
                    await loadPresignedURLs(markLoaded: false)
                } catch {
                    await MainActor.run {
                        networkState.enqueue(SyncOp(target: .irPhoto, operation: .update, irPhoto: photo))
                    }
                }
            }
        } else {
            networkState.enqueue(SyncOp(target: .irPhoto, operation: .update, irPhoto: photo))
        }
    }

    private func deletePhoto(_ photo: IRPhoto) {
        PhotoImageCache.shared.removeImage(forKey: "\(photo.id.uuidString)_ir")
        PhotoImageCache.shared.removeImage(forKey: "\(photo.id.uuidString)_vis")
        irPhotoPresignedURLs.removeValue(forKey: "\(photo.id.uuidString)_ir")
        irPhotoPresignedURLs.removeValue(forKey: "\(photo.id.uuidString)_vis")
        IRPhotoService.deleteIRPhoto(photo, modelContext: modelContext)
    }

    private func loadPresignedURLs(markLoaded: Bool) async {
        guard networkState.mode == .online, !existingIRPhotos.isEmpty else {
            if markLoaded { hasLoadedPresignedURLs = true }
            return
        }
        // Extract data from @Model objects on main actor before passing to async service
        let requests = existingIRPhotos.compactMap { photo -> S3PresignedURLService.IRPhotoURLRequest? in
            guard let sessionId = photo.ir_session?.id else { return nil }
            return S3PresignedURLService.IRPhotoURLRequest(
                photoId: photo.id,
                sessionId: sessionId,
                irPhotoKey: photo.ir_photo_key,
                visualPhotoKey: photo.visual_photo_key
            )
        }
        if requests.isEmpty {
            if markLoaded { hasLoadedPresignedURLs = true }
            return
        }
        let urls = await S3PresignedURLService.shared.batchGetIRPhotoPresignedURLs(requests: requests)
        await MainActor.run {
            irPhotoPresignedURLs = urls
            if markLoaded { hasLoadedPresignedURLs = true }
        }
    }
}

struct IRPhotoRow: View {
    let irPhoto: IRPhoto
    let isOnline: Bool
    let onTap: () -> Void
    var irPhotoPresignedURLs: [String: String] = [:]
    var onEdit: (() -> Void)? = nil
    var onDelete: (() -> Void)? = nil

    private var isFlirInd: Bool {
        irPhoto.ir_session?.photo_type == "FLIR-IND"
    }

    var body: some View {
        HStack(spacing: 8) {
            Button(action: onTap) {
                HStack(spacing: 10) {
                    // Thumbnails
                    if !isFlirInd && !irPhoto.visual_photo_key.isEmpty {
                        IRPhotoThumbnail(
                            photoKey: irPhoto.visual_photo_key,
                            cacheKey: "\(irPhoto.id.uuidString)_vis",
                            sessionId: irPhoto.ir_session?.id,
                            label: AppStrings.AssetsExtra.visLabel,
                            labelColor: .blue,
                            isOnline: isOnline,
                            presignedURL: irPhotoPresignedURLs["\(irPhoto.id.uuidString)_vis"]
                        )
                    }

                    IRPhotoThumbnail(
                        photoKey: irPhoto.ir_photo_key,
                        cacheKey: "\(irPhoto.id.uuidString)_ir",
                        sessionId: irPhoto.ir_session?.id,
                        label: AppStrings.AssetsExtra.irLabel,
                        labelColor: .orange,
                        isOnline: isOnline,
                        presignedURL: irPhotoPresignedURLs["\(irPhoto.id.uuidString)_ir"]
                    )

                    // Metadata
                    VStack(alignment: .leading, spacing: 2) {
                        Text(AppStrings.AssetsExtra.irPrefix(irPhoto.ir_photo_key))
                            .font(.caption)
                            .foregroundColor(.orange)
                        if !isFlirInd && !irPhoto.visual_photo_key.isEmpty {
                            Text(AppStrings.AssetsExtra.visualPrefix(irPhoto.visual_photo_key))
                                .font(.caption)
                                .foregroundColor(.primary)
                        }
                        Text(irPhoto.date_created.formatted(date: .abbreviated, time: .omitted))
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .buttonStyle(.plain)

            if onEdit != nil || onDelete != nil {
                Menu {
                    if let onEdit {
                        Button {
                            onEdit()
                        } label: {
                            Label(AppStrings.Sessions.editIRPhoto, systemImage: "pencil")
                        }
                    }
                    if let onDelete {
                        Button(role: .destructive) {
                            onDelete()
                        } label: {
                            Label(AppStrings.Common.delete, systemImage: "trash")
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.system(size: 18))
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 4)
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

/// Row view for staged IR photo data (not yet saved to SwiftData)
struct StagedIRPhotoRow: View {
    let data: StagedIRPhotoData
    let isFlirInd: Bool
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            // Placeholder thumbnails for staged (unsaved) photos
            if !isFlirInd && !data.visualPhotoKey.isEmpty {
                StagedThumbnailPlaceholder(label: AppStrings.AssetsExtra.visLabel, labelColor: .blue)
            }
            StagedThumbnailPlaceholder(label: AppStrings.AssetsExtra.irLabel, labelColor: .orange)

            // Metadata
            VStack(alignment: .leading, spacing: 2) {
                Text(AppStrings.AssetsExtra.irPrefix(data.irPhotoKey))
                    .font(.caption)
                    .foregroundColor(.orange)
                if !isFlirInd && !data.visualPhotoKey.isEmpty {
                    Text(AppStrings.AssetsExtra.visualPrefix(data.visualPhotoKey))
                        .font(.caption)
                        .foregroundColor(.primary)
                }
                Label(AppStrings.Common.pending, systemImage: "clock.fill")
                    .font(.caption2)
                    .foregroundColor(.orange)
            }

            Spacer()

            Button(action: onDelete) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.red)
                    .font(.body)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

/// Placeholder thumbnail for staged photos that haven't been uploaded yet
private struct StagedThumbnailPlaceholder: View {
    let label: String
    let labelColor: Color

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(UIColor.systemGray5))
                .frame(width: 60, height: 60)

            Image(systemName: "clock.fill")
                .foregroundColor(.orange.opacity(0.6))
                .font(.title3)

            // Label overlay
            VStack {
                Spacer()
                HStack {
                    Text(label)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                        .background(labelColor.opacity(0.8))
                        .cornerRadius(4)
                    Spacer()
                }
                .padding(4)
            }
        }
        .frame(width: 60, height: 60)
    }
}
