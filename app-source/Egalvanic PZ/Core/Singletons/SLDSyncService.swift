import SwiftData
import Foundation

// SLDSyncService: Service responsible for syncing Single Line Diagram (SLD) data
// between remote API and local SwiftData storage

// MARK: - Sync Progress Tracking

/// Detailed progress information for SLD sync operations
struct SyncProgressDetails {
    // Entity counts from incoming data
    var totalNodes: Int = 0
    var totalEdges: Int = 0
    var totalPhotos: Int = 0
    var totalIRPhotos: Int = 0
    var totalIRSessions: Int = 0
    var totalTasks: Int = 0
    var totalQuotes: Int = 0
    var totalIssues: Int = 0
    var totalComments: Int = 0
    var totalMappings: Int = 0
    var totalBuildings: Int = 0
    var totalFloors: Int = 0
    var totalRooms: Int = 0
    var totalFormInstances: Int = 0
    
    // Processing progress
    var processedNodes: Int = 0
    var processedEdges: Int = 0
    var processedPhotos: Int = 0
    var processedIRPhotos: Int = 0
    var processedIRSessions: Int = 0
    var processedTasks: Int = 0
    var processedQuotes: Int = 0
    var processedIssues: Int = 0
    var processedComments: Int = 0
    var processedMappings: Int = 0
    var processedLocations: Int = 0
    var processedFormInstances: Int = 0
    
    // Current step information
    var currentStep: SyncStep = .fetching
    var currentStepDetail: String = ""
    
    /// Total entities to process
    var totalEntities: Int {
        totalNodes + totalEdges + totalPhotos + totalIRPhotos +
        totalIRSessions + totalTasks + totalQuotes + totalIssues +
        totalComments + totalMappings + totalBuildings + totalFloors +
        totalRooms + totalFormInstances
    }
    
    /// Total entities processed so far
    var processedEntities: Int {
        processedNodes + processedEdges + processedPhotos + processedIRPhotos +
        processedIRSessions + processedTasks + processedQuotes + processedIssues +
        processedComments + processedMappings + processedLocations + processedFormInstances
    }
    
    /// Progress as a percentage (0.0 to 1.0)
    var entityProgress: Double {
        guard totalEntities > 0 else { return 0.0 }
        return Double(processedEntities) / Double(totalEntities)
    }
    
    /// Reset all values
    mutating func reset() {
        self = SyncProgressDetails()
    }
}

/// Enumeration of sync steps for detailed progress tracking
enum SyncStep: CaseIterable {
    case fetching
    case nodeClasses
    case edgeClasses
    case issueClasses
    case locations
    case nodes
    case edges
    case sessions
    case tasks
    case formInstances
    case quotes
    case issues
    case mappings
    case photos
    case irPhotos
    case comments
    case saving
    case complete

    var displayName: String {
        switch self {
        case .fetching: return AppStrings.SyncStep.fetching
        case .nodeClasses: return AppStrings.SyncStep.nodeClasses
        case .edgeClasses: return AppStrings.SyncStep.edgeClasses
        case .issueClasses: return AppStrings.SyncStep.issueClasses
        case .locations: return AppStrings.SyncStep.locations
        case .nodes: return AppStrings.SyncStep.nodes
        case .edges: return AppStrings.SyncStep.edges
        case .sessions: return AppStrings.SyncStep.sessions
        case .tasks: return AppStrings.SyncStep.tasks
        case .formInstances: return AppStrings.SyncStep.formInstances
        case .quotes: return AppStrings.SyncStep.quotes
        case .issues: return AppStrings.SyncStep.issues
        case .mappings: return AppStrings.SyncStep.mappings
        case .photos: return AppStrings.SyncStep.photos
        case .irPhotos: return AppStrings.SyncStep.irPhotos
        case .comments: return AppStrings.SyncStep.comments
        case .saving: return AppStrings.SyncStep.saving
        case .complete: return AppStrings.SyncStep.complete
        }
    }

    var stepNumber: Int {
        switch self {
        case .fetching: return 1
        case .nodeClasses: return 2
        case .edgeClasses: return 3
        case .issueClasses: return 4
        case .locations: return 5
        case .nodes: return 6
        case .edges: return 7
        case .sessions: return 8
        case .tasks: return 9
        case .formInstances: return 10
        case .quotes: return 11
        case .issues: return 12
        case .mappings: return 13
        case .photos: return 14
        case .irPhotos: return 15
        case .comments: return 16
        case .saving: return 17
        case .complete: return 18
        }
    }
    
    static var totalSteps: Int { 18 }
}

// Helper struct for organizing mapping DTOs
struct MappingDTOs {
    var issueTask: [MappingIssueTaskDTO] = []
    var taskSession: [MappingTaskSessionDTO] = []
    var quoteTask: [MappingQuoteTaskDTO] = []
    var userTask: [MappingUserTaskDTO] = []
    var taskNode: [MappingTaskNodeDTO] = []
    var taskForm: [MappingTaskFormDTO] = []
    var taskFormInstance: [MappingTaskFormInstanceDTO] = []
    var formInstanceNode: [MappingFormInstanceNodeDTO] = []
    var taskEGFormInstance: [MappingTaskEGFormInstanceDTO] = []
    var egFormInstanceNode: [MappingEGFormInstanceNodeDTO] = []
    var nodeSession: [MappingNodeSessionDTO] = []
    var userSession: [MappingUserSessionDTO] = []
    var attachmentNode: [MappingAttachmentNodeDTO] = []
    var nodeSLDView: [MappingNodeSLDViewDTO] = []
    var edgeSLDView: [MappingEdgeSLDViewDTO] = []
}

// Helper struct for organizing all pending DTOs
struct PendingDTOs {
    var photos: [(photoDTO: SLDDTOPhoto, sldId: UUID)] = []
    var irPhotos: [(irPhotoDTO: IRPhotoDTO, sldId: UUID)] = []
    var irSessions: [(sessionDTO: IRSessionDTO, sldId: UUID)] = []
    var tasks: [(taskDTO: UserTaskDTO, sldId: UUID)] = []
    var quotes: [(quoteDTO: QuoteDTO, sldId: UUID)] = []
    var issues: [(issueDTO: IssueDTO, sldId: UUID)] = []
    var comments: [(commentDTO: SLDCommentDTO, sldId: UUID)] = []
    var formInstances: [FormInstanceDTO] = []
    var egForms: [EGFormDTO] = []
    var egFormInstances: [EGFormInstanceDTO] = []
    var attachments: [(attachmentDTO: AttachmentDTO, sldId: UUID)] = []
    var mappings = MappingDTOs()
    var buildings: [(buildingDTO: BuildingDTO, sldId: UUID)] = []
    var floors: [(floorDTO: FloorDTO, sldId: UUID)] = []
    var rooms: [(roomDTO: RoomDTO, sldId: UUID)] = []
    var sldViews: [(viewDTO: SLDViewDTO, sldId: UUID)] = []
    var sldLinks: [(linkDTO: SLDLinkDTO, sldId: UUID)] = []
}

/// Service responsible for syncing Single Line Diagram data between remote and local storage
@MainActor
class SLDSyncService: ObservableObject {
    static let shared = SLDSyncService()
    
    // Published properties for UI updates
    @Published var isSyncing = false
    @Published var lastSyncError: Error?
    @Published var syncProgress: Double = 0.0
    @Published var syncStatusMessage: String = ""
    @Published var syncErrorMessage: String? = nil
    @Published var currentSiteName: String? = nil
    @Published var isSiteSwitchMode = false // Tracks if we're switching sites (vs just refreshing)
    
    // Detailed progress tracking
    @Published var progressDetails = SyncProgressDetails()
    
    /// How often to update UI during loops (every N items)
    private let progressUpdateInterval: Int = 25
    
    private init() {} // Singleton pattern
    
    // MARK: - Progress Update Helpers
    
    /// Update the current sync step and refresh progress
    private func updateStep(_ step: SyncStep, detail: String = "") {
        progressDetails.currentStep = step
        progressDetails.currentStepDetail = detail
        syncStatusMessage = detail.isEmpty ? step.displayName : "\(step.displayName): \(detail)"
        updateOverallProgress()
    }
    
    /// Calculate and update the overall progress based on step and entity progress
    private func updateOverallProgress() {
        // Base progress from steps (0.0 to 0.3 for fetch/class processing)
        // Entity progress fills remaining 0.3 to 0.95
        let step = progressDetails.currentStep
        
        switch step {
        case .fetching:
            syncProgress = 0.05
        case .nodeClasses:
            syncProgress = 0.10
        case .edgeClasses:
            syncProgress = 0.15
        case .issueClasses:
            syncProgress = 0.20
        case .locations, .nodes, .edges, .sessions, .tasks, .formInstances,
                .quotes, .issues, .mappings, .photos, .irPhotos, .comments:
            // Entity processing phase: 0.25 to 0.90 based on entities processed
            let entityProgress = progressDetails.entityProgress
            syncProgress = 0.25 + (entityProgress * 0.65)
        case .saving:
            syncProgress = 0.92
        case .complete:
            syncProgress = 1.0
        }
    }
    
    /// Populate totals from the incoming DTO data
    private func populateTotalsFromDTO(_ sldDTO: SLDDTO) {
        progressDetails.totalNodes = sldDTO.nodes.count
        progressDetails.totalEdges = sldDTO.edges.count
        progressDetails.totalPhotos = sldDTO.photos.count
        progressDetails.totalIRPhotos = sldDTO.ir_photos.count
        progressDetails.totalIRSessions = sldDTO.ir_sessions.count
        progressDetails.totalTasks = sldDTO.tasks.count
        progressDetails.totalQuotes = sldDTO.quotes.count
        progressDetails.totalIssues = sldDTO.issues.count
        progressDetails.totalComments = sldDTO.comments.count
        progressDetails.totalBuildings = sldDTO.buildings?.count ?? 0
        progressDetails.totalFloors = sldDTO.floors?.count ?? 0
        progressDetails.totalRooms = sldDTO.rooms?.count ?? 0
        progressDetails.totalFormInstances = sldDTO.form_instances?.count ?? 0
        
        // Calculate total mappings
        if let mappings = sldDTO.mappings {
            progressDetails.totalMappings = mappings.issueTaskMappings.count +
            mappings.taskSessionMappings.count +
            mappings.quoteTaskMappings.count +
            mappings.userTaskMappings.count +
            mappings.taskNodeMappings.count +
            mappings.taskFormMappings.count +
            mappings.taskFormInstanceMappings.count +
            mappings.formInstanceNodeMappings.count +
            mappings.nodeSessionMappings.count +
            mappings.userSessionMappings.count
        }
        
        AppLogger.log(.info, "Sync totals - Nodes: \(progressDetails.totalNodes), Edges: \(progressDetails.totalEdges), Photos: \(progressDetails.totalPhotos), IR Photos: \(progressDetails.totalIRPhotos), Sessions: \(progressDetails.totalIRSessions), Tasks: \(progressDetails.totalTasks), Quotes: \(progressDetails.totalQuotes), Issues: \(progressDetails.totalIssues), Mappings: \(progressDetails.totalMappings), Total: \(progressDetails.totalEntities)", category: .sync)
    }
    
    @MainActor
    func upsertAllData(sld_id: UUID, modelContext: ModelContext, isSiteSwitch: Bool = false) async throws {
        AppLogger.log(.info, "upsertAllData: start (isSiteSwitch: \(isSiteSwitch))", category: .sync)
        isSyncing = true
        isSiteSwitchMode = isSiteSwitch
        syncProgress = 0.0
        syncErrorMessage = nil
        progressDetails.reset()
        // Note: currentSiteName should be set BEFORE calling this function
        defer {
            isSyncing = false
            isSiteSwitchMode = false
            if syncErrorMessage == nil {
                syncStatusMessage = ""
                currentSiteName = nil // Clear after successful completion
            }
        }
        
        do {
            // Step 1: Fetch data from server
            updateStep(.fetching, detail: "Connecting to server...")
            
            let userId = AppStateManager.shared.userId
            
            async let sldDTO = APIClient.shared.fetchSLDDTOV2(sld_id: sld_id)
            async let classDTOs = APIClient.shared.fetchNodeClassDTOsByUser(user_id: userId)
            async let edgeClassDTOs = APIClient.shared.fetchEdgeClassDTOsByUser(user_id: userId)
            async let issueClassDTOs = APIClient.shared.fetchIssueClassDTOsByUser(user_id: userId)
            async let userTaskFormDTOs = APIClient.shared.fetchUserTaskFormDTOS()
            async let shortcutDTOs = APIClient.shared.fetchShortcutDTOsByUser(user_id: userId)
            async let equipmentDTOs = APIClient.shared.fetchTestEquipment()
            // ZP-2161: taxonomy + enum tables that drive the Engineering
            // section. Tolerant of failure — the rest of the sync still
            // works if these endpoints are temporarily unavailable
            // (older backends).
            async let eqpLibTaxonomyDTOs = APIClient.shared.fetchEqpLibTaxonomy()
            async let enumsBundleDTO = APIClient.shared.fetchEnumsBundle()
            async let skmHeadersBundleDTO = APIClient.shared.fetchSkmHeadersBundle()
            // Procedure catalog for the task creation picker. Tolerant
            // of failure so older backends don't break the rest of the
            // refresh.
            async let procedureDTOs = APIClient.shared.fetchProcedures()
            // ZP-2161 Phase 4b: the SKM deep tree (frames + sensors +
            // trip_units + segments, ~440k rows / ~5 MB gzipped) is
            // user-triggered from Settings via ``SkmLibrarySyncService``
            // — same pattern as the EasyPower library, so it doesn't
            // re-download on every SLD refresh.

            let (dto, classes, edgeClasses, issueClasses, forms, shortcuts, equipment) = try await (sldDTO, classDTOs, edgeClassDTOs, issueClassDTOs, userTaskFormDTOs, shortcutDTOs, equipmentDTOs)
            let eqpLibTypes: [EgEqpLibTypeDTO] = (try? await eqpLibTaxonomyDTOs) ?? []
            let enumsBundle: EnumsBundleDTO? = try? await enumsBundleDTO
            let skmHeaders: SkmHeadersBundleDTO? = try? await skmHeadersBundleDTO
            let procedures: [ProcedureDTO] = (try? await procedureDTOs) ?? []

            // Populate totals from the fetched DTO for progress tracking
            populateTotalsFromDTO(dto)

            AppLogger.log(.info, "Received 1 SLD DTO and \(classes.count) NodeClass DTOs and \(edgeClasses.count) EdgeClass DTOs and \(issueClasses.count) IssueClass DTOs and \(forms.count) UserTaskForm DTOs and \(shortcuts.count) Shortcut DTOs and \(equipment.count) Equipment DTOs and \(eqpLibTypes.count) EqpLibType DTOs and \(enumsBundle == nil ? "no" : "an") enums bundle and \(skmHeaders?.devices.count ?? 0) SKM device headers; persisting…", category: .sync)

            // Step 2: Process node classes
            updateStep(.nodeClasses, detail: "\(classes.count) equipment classes")
            try smartUpsertNodeClassDTOs(dtos: classes, into: modelContext)

            // Step 2.1: Process eqp lib taxonomy
            try smartUpsertEqpLibTypeDTOs(dtos: eqpLibTypes, into: modelContext)

            // Step 2.2: Process engineering enum bundle
            if let enumsBundle {
                try smartUpsertEnumsBundle(enumsBundle, into: modelContext)
            }

            // Step 2.3: Process SKM library headers (Phase 4a)
            if let skmHeaders {
                try smartUpsertSkmHeadersBundle(skmHeaders, into: modelContext)
            }
            // ZP-2161 Phase 4b: deep tree is settings-triggered (see
            // ``SkmLibrarySyncService.downloadAndCache(...)``). Not part
            // of the per-SLD sync flow.
            
            // Step 3: Process edge classes
            updateStep(.edgeClasses, detail: "\(edgeClasses.count) connection classes")
            try smartUpsertEdgeClassDTOs(dtos: edgeClasses, into: modelContext)
            
            // Step 4: Process issue classes
            updateStep(.issueClasses, detail: "\(issueClasses.count) issue classes")
            try smartUpsertIssueClassDTOs(dtos: issueClasses, into: modelContext)
            
            // Step 4.5: Process shortcuts
            updateStep(.nodeClasses, detail: "\(shortcuts.count) shortcuts")
            try smartUpsertShortcutDTOs(dtos: shortcuts, into: modelContext)

            // Step 4.6: Process test equipment
            updateStep(.nodeClasses, detail: "\(equipment.count) test equipment")
            try smartUpsertTestEquipmentDTOs(dtos: equipment, into: modelContext)

            // Step 4.7: Process procedures (powers the task creation picker)
            try smartUpsertProcedureDTOs(dtos: procedures, into: modelContext)

            // Step 5: Process main diagram data (with granular progress updates)
            try await upsertSwiftDataObjects(
                sldDTO: dto,
                userTaskFormDTOs: forms,
                into: modelContext
            )
            
            // Step 6: Complete
            updateStep(.saving, detail: "Finalizing changes...")
            let finalCount = try modelContext.fetch(FetchDescriptor<SLDV2>()).count
            AppLogger.log(.info, "Sync complete. Context now has \(finalCount) diagrams", category: .sync)
            
            updateStep(.complete)
            syncStatusMessage = "Sync complete!"
            
        } catch {
            // Capture and publish error details
            lastSyncError = error
            syncProgress = 0.0
            progressDetails.reset()
            
            // Provide detailed error message for debugging
            if let decodingError = error as? DecodingError {
                syncErrorMessage = formatDecodingError(decodingError)
                AppLogger.log(.error, "Decoding error: \(decodingError)", category: .sync)
            } else if error.localizedDescription.contains("network") || error.localizedDescription.contains("connection") {
                syncErrorMessage = "Network error. Please check your internet connection and try again."
            } else {
                syncErrorMessage = "Failed to sync site data: \(error.localizedDescription)"
            }
            
            syncStatusMessage = "Sync failed"
            AppLogger.log(.error, "upsertAllData failed: \(error)", category: .sync)
            throw error
        }
    }
    
    /// Format a DecodingError into a user-friendly message with debugging details
    private func formatDecodingError(_ error: DecodingError) -> String {
        switch error {
        case .typeMismatch(let type, let context):
            let path = context.codingPath.map { $0.stringValue }.joined(separator: " → ")
            let expectedType = String(describing: type)
            
            // Check for common patterns
            if path.contains("core_attributes") {
                if path.contains("edges") {
                    return "Data format error: An edge has core_attributes as a dictionary {} but it should be an array [].\n\nPath: \(path)\nExpected: \(expectedType)\n\nThis is a backend data issue - check the edge data in the database."
                } else if path.contains("nodes") {
                    return "Data format error: A node has core_attributes as a dictionary {} but it should be an array [].\n\nPath: \(path)\nExpected: \(expectedType)\n\nThis is a backend data issue - check the node data in the database."
                }
            }
            
            return "Data format error: Server returned unexpected data type.\n\nPath: \(path)\nExpected: \(expectedType)\nDetails: \(context.debugDescription)"
            
        case .valueNotFound(let type, let context):
            let path = context.codingPath.map { $0.stringValue }.joined(separator: " → ")
            return "Missing required field.\n\nPath: \(path)\nMissing type: \(String(describing: type))\nDetails: \(context.debugDescription)"
            
        case .keyNotFound(let key, let context):
            let path = context.codingPath.map { $0.stringValue }.joined(separator: " → ")
            return "Missing required key: '\(key.stringValue)'\n\nPath: \(path)\nDetails: \(context.debugDescription)"
            
        case .dataCorrupted(let context):
            let path = context.codingPath.map { $0.stringValue }.joined(separator: " → ")
            let description = context.debugDescription
            
            // Check for UUID-specific errors
            if description.contains("UUID") {
                if path.contains("issue_class") {
                    return "Invalid UUID error: An issue has an invalid or malformed issue_class UUID.\n\nPath: \(path)\n\nThis is a backend data issue - the issue_class field should be a valid UUID or null. Check the database for empty strings or invalid UUID values."
                } else if path.contains("node_class") {
                    return "Invalid UUID error: A node has an invalid or malformed node_class UUID.\n\nPath: \(path)\n\nThis is a backend data issue - the node_class field should be a valid UUID or null."
                } else if path.contains("edge_class") {
                    return "Invalid UUID error: An edge has an invalid or malformed edge_class UUID.\n\nPath: \(path)\n\nThis is a backend data issue - the edge_class field should be a valid UUID or null."
                } else {
                    return "Invalid UUID error: A UUID field contains invalid data.\n\nPath: \(path)\n\nExpected a valid UUID format (e.g., '123e4567-e89b-12d3-a456-426614174000') or null, but got an invalid value."
                }
            }
            
            return "Corrupted data at path: \(path)\n\nDetails: \(description)"
            
        @unknown default:
            return "Failed to process site data. The server returned data in an unexpected format."
        }
    }
    
    // MARK: - Node Icon Upsert
    
    @MainActor
    func smartUpsertNodeIcons(
        dtos: [NodeIconDTO],
        into context: ModelContext
    ) throws {
        guard !dtos.isEmpty else { return }
        
        // Fetch existing icons
        let existingIcons = try context.fetch(FetchDescriptor<NodeIcon>())
        var iconsById = Dictionary(uniqueKeysWithValues: existingIcons.map { ($0.id, $0) })
        
        AppLogger.log(.info, "Upserting \(dtos.count) NodeIcons (existing: \(existingIcons.count))", category: .sync)
        
        var seenIds = Set<UUID>()
        
        for dto in dtos {
            seenIds.insert(dto.id)
            
            if let existing = iconsById[dto.id] {
                // Update existing
                existing.name = dto.name
                existing.svg = dto.svg
                existing.company_id = dto.company_id
                existing.is_deleted = dto.is_deleted ?? false
            } else {
                // Create new
                let newIcon = dto.toEntity()
                context.insert(newIcon)
                iconsById[dto.id] = newIcon
            }
        }
        
        // Delete icons not in the DTOs
        for (id, icon) in iconsById where !seenIds.contains(id) {
            context.delete(icon)
        }
        
        AppLogger.log(.info, "NodeIcons upsert completed: \(seenIds.count) icons", category: .sync)
    }
    
    // MARK: - Node Orientation Upsert
    
    @MainActor
    func smartUpsertNodeOrientations(
        dtos: [NodeOrientationDTO],
        into context: ModelContext
    ) throws {
        guard !dtos.isEmpty else { return }
        
        // Fetch existing orientations and terminals
        let existingOrientations = try context.fetch(FetchDescriptor<NodeOrientation>())
        let existingTerminals = try context.fetch(FetchDescriptor<NodeOrientationTerminal>())
        
        var orientationsById = Dictionary(uniqueKeysWithValues: existingOrientations.map { ($0.id, $0) })
        var terminalsById = Dictionary(uniqueKeysWithValues: existingTerminals.map { ($0.id, $0) })
        
        AppLogger.log(.info, "Upserting \(dtos.count) NodeOrientations (existing: \(existingOrientations.count))", category: .sync)
        
        var seenOrientationIds = Set<UUID>()
        var seenTerminalIds = Set<UUID>()
        
        for dto in dtos {
            seenOrientationIds.insert(dto.id)
            
            let orientation: NodeOrientation
            if let existing = orientationsById[dto.id] {
                // Update existing orientation
                existing.code = dto.code
                existing.name = dto.name
                existing.orientationDescription = dto.orientationDescription
                existing.is_deleted = dto.is_deleted ?? false
                existing.created_at = dto.created_at
                existing.updated_at = dto.updated_at
                orientation = existing
            } else {
                // Create new orientation
                orientation = dto.toEntity()
                context.insert(orientation)
                orientationsById[dto.id] = orientation
            }
            
            // Process terminals for this orientation
            for terminalDTO in dto.orientation_terminals {
                seenTerminalIds.insert(terminalDTO.id)
                
                if let existing = terminalsById[terminalDTO.id] {
                    // Update existing terminal
                    existing.handle_code = terminalDTO.handle_code
                    existing.label = terminalDTO.label
                    existing.side = terminalDTO.side
                    existing.max_connections = terminalDTO.max_connections
                    existing.node_orientation_id = terminalDTO.node_orientation_id
                    existing.notes = terminalDTO.notes
                    existing.is_deleted = terminalDTO.is_deleted ?? false
                    existing.created_at = terminalDTO.created_at
                    existing.updated_at = terminalDTO.updated_at
                    existing.nodeOrientation = orientation
                } else {
                    // Create new terminal
                    let newTerminal = terminalDTO.toEntity(nodeOrientation: orientation)
                    context.insert(newTerminal)
                    terminalsById[terminalDTO.id] = newTerminal
                }
            }
        }
        
        // Delete terminals not in the DTOs (children first for FK integrity)
        for (id, terminal) in terminalsById where !seenTerminalIds.contains(id) {
            context.delete(terminal)
        }
        
        // Delete orientations not in the DTOs
        for (id, orientation) in orientationsById where !seenOrientationIds.contains(id) {
            context.delete(orientation)
        }
        
        AppLogger.log(.info, "NodeOrientations upsert completed: \(seenOrientationIds.count) orientations, \(seenTerminalIds.count) terminals", category: .sync)
    }


    /// ZP-2161: upsert ``eg_eqp_lib_types`` + their subtypes. Mirrors
    /// the smart-upsert pattern used elsewhere — soft-update if the
    /// row exists, create otherwise. Rows present locally but missing
    /// from the payload are left alone (the taxonomy table is
    /// system-managed, never user-deleted, so we don't need to track
    /// soft-deletes here).
    func smartUpsertEqpLibTypeDTOs(
        dtos: [EgEqpLibTypeDTO],
        into context: ModelContext
    ) throws {
        guard !dtos.isEmpty else { return }

        let existingTypes = try context.fetch(FetchDescriptor<EgEqpLibType>())
        let existingSubtypes = try context.fetch(FetchDescriptor<EgEqpLibSubtype>())
        var typesById = Dictionary(uniqueKeysWithValues: existingTypes.map { ($0.id, $0) })
        var subtypesById = Dictionary(uniqueKeysWithValues: existingSubtypes.map { ($0.id, $0) })

        for dto in dtos {
            let type: EgEqpLibType
            if let existing = typesById[dto.id] {
                existing.name = dto.name
                existing.display_name = dto.display_name
                existing.category_id = dto.category_id
                existing.api_kind = dto.api_kind
                existing.api_slug = dto.api_slug
                existing.sort_order = dto.sort_order
                existing.is_active = dto.is_active
                type = existing
            } else {
                let created = EgEqpLibType(
                    id: dto.id,
                    name: dto.name,
                    display_name: dto.display_name,
                    category_id: dto.category_id,
                    api_kind: dto.api_kind,
                    api_slug: dto.api_slug,
                    sort_order: dto.sort_order,
                    is_active: dto.is_active
                )
                context.insert(created)
                typesById[dto.id] = created
                type = created
            }

            for subDTO in dto.subtypes ?? [] {
                if let existing = subtypesById[subDTO.id] {
                    existing.type_id = subDTO.type_id
                    existing.name = subDTO.name
                    existing.display_name = subDTO.display_name
                    existing.api_kind = subDTO.api_kind
                    existing.api_slug = subDTO.api_slug
                    existing.sort_order = subDTO.sort_order
                    existing.is_active = subDTO.is_active
                    existing.type = type
                } else {
                    let created = EgEqpLibSubtype(
                        id: subDTO.id,
                        type_id: subDTO.type_id,
                        name: subDTO.name,
                        display_name: subDTO.display_name,
                        api_kind: subDTO.api_kind,
                        api_slug: subDTO.api_slug,
                        sort_order: subDTO.sort_order,
                        is_active: subDTO.is_active,
                        type: type
                    )
                    context.insert(created)
                    subtypesById[subDTO.id] = created
                }
            }
        }

        AppLogger.log(.info, "EgEqpLibType upsert: \(dtos.count) types, \(dtos.reduce(0) { $0 + ($1.subtypes?.count ?? 0) }) subtypes", category: .sync)
    }


    /// ZP-2161: upsert every engineering enum table that NodeV2 FK
    /// columns reference. Each section is processed independently — a
    /// bad row in one enum doesn't break the others. Soft-deleted /
    /// is_deleted=true rows are preserved (system-managed tables; the
    /// backend payload filters where applicable, e.g. manufacturers).
    func smartUpsertEnumsBundle(
        _ bundle: EnumsBundleDTO,
        into context: ModelContext
    ) throws {
        // Voltage
        let existingVoltages = try context.fetch(FetchDescriptor<EnumNodeVoltage>())
        var voltageById = Dictionary(uniqueKeysWithValues: existingVoltages.map { ($0.id, $0) })
        for dto in bundle.voltages {
            if let row = voltageById[dto.id] {
                row.label = dto.label
                row.value = dto.value
                row.sort_order = dto.sort_order ?? 0
            } else {
                let created = EnumNodeVoltage(id: dto.id, label: dto.label, value: dto.value, sort_order: dto.sort_order ?? 0)
                context.insert(created)
                voltageById[dto.id] = created
            }
        }

        // Mains type
        let existingMains = try context.fetch(FetchDescriptor<EnumNodeMainsType>())
        var mainsById = Dictionary(uniqueKeysWithValues: existingMains.map { ($0.id, $0) })
        for dto in bundle.mains_types {
            if let row = mainsById[dto.id] {
                row.label = dto.label
                row.enum_description = dto.description
                row.sort_order = dto.sort_order ?? 0
            } else {
                let created = EnumNodeMainsType(id: dto.id, label: dto.label, enum_description: dto.description, sort_order: dto.sort_order ?? 0)
                context.insert(created)
                mainsById[dto.id] = created
            }
        }

        // Phase config
        let existingPhases = try context.fetch(FetchDescriptor<EnumNodePhaseConfiguration>())
        var phaseById = Dictionary(uniqueKeysWithValues: existingPhases.map { ($0.id, $0) })
        for dto in bundle.phase_configurations {
            if let row = phaseById[dto.id] {
                row.label = dto.label
                row.enum_description = dto.description
                row.sort_order = dto.sort_order ?? 0
                row.topology = dto.topology
                row.l_to_n_divisor = dto.l_to_n_divisor
                row.max_pole_count = dto.max_pole_count
                row.allowed_downstream_topology_ids = dto.allowed_downstream_topology_ids ?? []
            } else {
                let created = EnumNodePhaseConfiguration(
                    id: dto.id, label: dto.label, enum_description: dto.description,
                    sort_order: dto.sort_order ?? 0, topology: dto.topology,
                    l_to_n_divisor: dto.l_to_n_divisor, max_pole_count: dto.max_pole_count,
                    allowed_downstream_topology_ids: dto.allowed_downstream_topology_ids ?? []
                )
                context.insert(created)
                phaseById[dto.id] = created
            }
        }

        // Trip type
        let existingTrips = try context.fetch(FetchDescriptor<EnumNodeTripType>())
        var tripById = Dictionary(uniqueKeysWithValues: existingTrips.map { ($0.id, $0) })
        for dto in bundle.trip_types {
            if let row = tripById[dto.id] {
                row.slug = dto.slug
                row.display_name = dto.display_name
                row.skm_dev_type = dto.skm_dev_type
                row.sort_order = dto.sort_order ?? 0
                row.is_active = dto.is_active ?? true
                row.has_trip_unit = dto.has_trip_unit ?? false
            } else {
                let created = EnumNodeTripType(
                    id: dto.id, slug: dto.slug, display_name: dto.display_name,
                    skm_dev_type: dto.skm_dev_type, sort_order: dto.sort_order ?? 0,
                    is_active: dto.is_active ?? true, has_trip_unit: dto.has_trip_unit ?? false
                )
                context.insert(created)
                tripById[dto.id] = created
            }
        }

        // Device role
        let existingRoles = try context.fetch(FetchDescriptor<EnumDeviceRole>())
        var roleById = Dictionary(uniqueKeysWithValues: existingRoles.map { ($0.id, $0) })
        for dto in bundle.device_roles {
            if let row = roleById[dto.id] {
                row.code = dto.code
                row.label = dto.label
                row.sort_order = dto.sort_order ?? 0
            } else {
                let created = EnumDeviceRole(id: dto.id, code: dto.code, label: dto.label, sort_order: dto.sort_order ?? 0)
                context.insert(created)
                roleById[dto.id] = created
            }
        }

        // Manufacturer
        let existingMfrs = try context.fetch(FetchDescriptor<EnumSkmManufacturer>())
        var mfrById = Dictionary(uniqueKeysWithValues: existingMfrs.map { ($0.id, $0) })
        for dto in bundle.manufacturers {
            if let row = mfrById[dto.id] {
                row.name = dto.name
            } else {
                let created = EnumSkmManufacturer(id: dto.id, name: dto.name)
                context.insert(created)
                mfrById[dto.id] = created
            }
        }

        // Cable size
        let existingSizes = try context.fetch(FetchDescriptor<EnumCableSize>())
        var sizeById = Dictionary(uniqueKeysWithValues: existingSizes.map { ($0.id, $0) })
        for dto in bundle.cable_sizes {
            if let row = sizeById[dto.id] {
                row.name = dto.name
                row.is_awg = dto.is_awg
                row.awg_value = dto.awg_value
                row.kcmil_value = dto.kcmil_value
                row.circular_mils = dto.circular_mils
                row.sort_order = dto.sort_order ?? 0
                row.is_global = dto.is_global ?? true
                row.is_deleted = dto.is_deleted ?? false
            } else {
                let created = EnumCableSize(
                    id: dto.id, name: dto.name, is_awg: dto.is_awg,
                    awg_value: dto.awg_value, kcmil_value: dto.kcmil_value,
                    circular_mils: dto.circular_mils, sort_order: dto.sort_order ?? 0,
                    is_global: dto.is_global ?? true, is_deleted: dto.is_deleted ?? false
                )
                context.insert(created)
                sizeById[dto.id] = created
            }
        }

        // Cable conductor configuration
        let existingConductors = try context.fetch(FetchDescriptor<EnumCableConductorConfiguration>())
        var conductorById = Dictionary(uniqueKeysWithValues: existingConductors.map { ($0.id, $0) })
        for dto in bundle.cable_conductor_configurations {
            if let row = conductorById[dto.id] {
                row.name = dto.name
                row.conductor_count = dto.conductor_count
                row.has_ground = dto.has_ground ?? false
                row.is_paralleled = dto.is_paralleled ?? false
                row.sort_order = dto.sort_order ?? 0
                row.is_global = dto.is_global ?? true
                row.is_deleted = dto.is_deleted ?? false
            } else {
                let created = EnumCableConductorConfiguration(
                    id: dto.id, name: dto.name, conductor_count: dto.conductor_count,
                    has_ground: dto.has_ground ?? false, is_paralleled: dto.is_paralleled ?? false,
                    sort_order: dto.sort_order ?? 0,
                    is_global: dto.is_global ?? true, is_deleted: dto.is_deleted ?? false
                )
                context.insert(created)
                conductorById[dto.id] = created
            }
        }

        // The "simple" cable enums all share the same shape — process them
        // through a closure helper to keep this manageable.
        func upsertSimple<Model: PersistentModel>(
            _ dtos: [EnumCableSimpleDTO],
            existing: [Model],
            keyPath: (Model) -> Int,
            apply: (EnumCableSimpleDTO, Model) -> Void,
            create: (EnumCableSimpleDTO) -> Model
        ) {
            var byId = Dictionary(uniqueKeysWithValues: existing.map { (keyPath($0), $0) })
            for dto in dtos {
                if let row = byId[dto.id] {
                    apply(dto, row)
                } else {
                    let created = create(dto)
                    context.insert(created)
                    byId[dto.id] = created
                }
            }
        }

        upsertSimple(
            bundle.cable_conductor_descriptions,
            existing: try context.fetch(FetchDescriptor<EnumCableConductorDescription>()),
            keyPath: { $0.id },
            apply: { dto, row in
                row.name = dto.name
                row.sort_order = dto.sort_order ?? 0
                row.is_global = dto.is_global ?? true
                row.is_deleted = dto.is_deleted ?? false
                row.is_busway = dto.is_busway ?? false
            },
            create: { dto in
                EnumCableConductorDescription(
                    id: dto.id, name: dto.name, sort_order: dto.sort_order ?? 0,
                    is_global: dto.is_global ?? true, is_deleted: dto.is_deleted ?? false,
                    is_busway: dto.is_busway ?? false
                )
            }
        )

        upsertSimple(
            bundle.cable_insulation_classes,
            existing: try context.fetch(FetchDescriptor<EnumCableInsulationClass>()),
            keyPath: { $0.id },
            apply: { dto, row in
                row.name = dto.name
                row.sort_order = dto.sort_order ?? 0
                row.is_global = dto.is_global ?? true
                row.is_deleted = dto.is_deleted ?? false
                row.is_busway = dto.is_busway ?? false
            },
            create: { dto in
                EnumCableInsulationClass(
                    id: dto.id, name: dto.name, sort_order: dto.sort_order ?? 0,
                    is_global: dto.is_global ?? true, is_deleted: dto.is_deleted ?? false,
                    is_busway: dto.is_busway ?? false
                )
            }
        )

        upsertSimple(
            bundle.cable_insulation_types,
            existing: try context.fetch(FetchDescriptor<EnumCableInsulationType>()),
            keyPath: { $0.id },
            apply: { dto, row in
                row.name = dto.name
                row.sort_order = dto.sort_order ?? 0
                row.is_global = dto.is_global ?? true
                row.is_deleted = dto.is_deleted ?? false
                row.is_busway = dto.is_busway ?? false
            },
            create: { dto in
                EnumCableInsulationType(
                    id: dto.id, name: dto.name, sort_order: dto.sort_order ?? 0,
                    is_global: dto.is_global ?? true, is_deleted: dto.is_deleted ?? false,
                    is_busway: dto.is_busway ?? false
                )
            }
        )

        upsertSimple(
            bundle.cable_installations,
            existing: try context.fetch(FetchDescriptor<EnumCableInstallation>()),
            keyPath: { $0.id },
            apply: { dto, row in
                row.name = dto.name
                row.sort_order = dto.sort_order ?? 0
                row.is_global = dto.is_global ?? true
                row.is_deleted = dto.is_deleted ?? false
                row.is_busway = dto.is_busway ?? false
            },
            create: { dto in
                EnumCableInstallation(
                    id: dto.id, name: dto.name, sort_order: dto.sort_order ?? 0,
                    is_global: dto.is_global ?? true, is_deleted: dto.is_deleted ?? false,
                    is_busway: dto.is_busway ?? false
                )
            }
        )

        // Cable duct material — has an extra `description` column so
        // it can't use the simple helper.
        let existingDucts = try context.fetch(FetchDescriptor<EnumCableDuctMaterial>())
        var ductById = Dictionary(uniqueKeysWithValues: existingDucts.map { ($0.id, $0) })
        for dto in bundle.cable_duct_materials {
            if let row = ductById[dto.id] {
                row.name = dto.name
                row.enum_description = dto.description
                row.sort_order = dto.sort_order ?? 0
                row.is_global = dto.is_global ?? true
                row.is_deleted = dto.is_deleted ?? false
                row.is_busway = dto.is_busway ?? false
            } else {
                let created = EnumCableDuctMaterial(
                    id: dto.id, name: dto.name, enum_description: dto.description,
                    sort_order: dto.sort_order ?? 0, is_global: dto.is_global ?? true,
                    is_deleted: dto.is_deleted ?? false, is_busway: dto.is_busway ?? false
                )
                context.insert(created)
                ductById[dto.id] = created
            }
        }

        // Busway ampere
        let existingBuswayAmps = try context.fetch(FetchDescriptor<EnumBuswayAmpereRating>())
        var buswayById = Dictionary(uniqueKeysWithValues: existingBuswayAmps.map { ($0.id, $0) })
        for dto in bundle.busway_ampere_ratings {
            if let row = buswayById[dto.id] {
                row.name = dto.name
                row.ampere_value = dto.ampere_value
                row.sort_order = dto.sort_order ?? 0
                row.is_global = dto.is_global ?? true
                row.is_deleted = dto.is_deleted ?? false
            } else {
                let created = EnumBuswayAmpereRating(
                    id: dto.id, name: dto.name, ampere_value: dto.ampere_value,
                    sort_order: dto.sort_order ?? 0, is_global: dto.is_global ?? true,
                    is_deleted: dto.is_deleted ?? false
                )
                context.insert(created)
                buswayById[dto.id] = created
            }
        }

        AppLogger.log(.info, "Enums bundle upsert complete: \(bundle.voltages.count) voltages, \(bundle.manufacturers.count) manufacturers, \(bundle.trip_types.count) trip_types, \(bundle.cable_sizes.count) cable_sizes", category: .sync)
    }


    /// ZP-2161 Phase 4a: upsert the SKM library "headers" bundle —
    /// just the columns the Engineering section needs to filter
    /// manufacturer / type / subtype pickers offline. Deep tree
    /// (frames / sensors / trip_units / segments) is Phase 4b.
    func smartUpsertSkmHeadersBundle(
        _ bundle: SkmHeadersBundleDTO,
        into context: ModelContext
    ) throws {
        // dev_lib_routing
        let existingRouting = try context.fetch(FetchDescriptor<EgDevLibRouting>())
        var routingById = Dictionary(uniqueKeysWithValues: existingRouting.map { ($0.id, $0) })
        for dto in bundle.dev_lib_routing {
            if let row = routingById[dto.id] {
                row.eqp_lib_type_id = dto.eqp_lib_type_id
                row.eqp_lib_subtype_id = dto.eqp_lib_subtype_id
                row.allowed_trip_type_ids = dto.allowed_trip_type_ids
                row.sort_order = dto.sort_order ?? 0
                row.is_active = dto.is_active ?? true
            } else {
                let created = EgDevLibRouting(
                    id: dto.id,
                    eqp_lib_type_id: dto.eqp_lib_type_id,
                    eqp_lib_subtype_id: dto.eqp_lib_subtype_id,
                    allowed_trip_type_ids: dto.allowed_trip_type_ids,
                    sort_order: dto.sort_order ?? 0,
                    is_active: dto.is_active ?? true
                )
                context.insert(created)
                routingById[dto.id] = created
            }
        }

        // devices headers — high volume (~11k), full replace by id
        let existingDevices = try context.fetch(FetchDescriptor<SkmDeviceHeader>())
        var deviceById = Dictionary(uniqueKeysWithValues: existingDevices.map { ($0.id, $0) })
        for dto in bundle.devices {
            if let row = deviceById[dto.id] {
                row.eqp_lib_type_id = dto.eqp_lib_type_id
                row.eqp_lib_subtype_id = dto.eqp_lib_subtype_id
                row.trip_type_id = dto.trip_type_id
                row.manufacturer_id = dto.manufacturer_id
                row.sz_name = dto.sz_name
                row.sz_type = dto.sz_type
                row.sz_description = dto.sz_description
                row.sz_catalog = dto.sz_catalog
                row.sz_dev_type = dto.sz_dev_type
                row.n_poles = dto.n_poles
                row.f_max_voltage = dto.f_max_voltage
            } else {
                let created = SkmDeviceHeader(
                    id: dto.id,
                    eqp_lib_type_id: dto.eqp_lib_type_id,
                    eqp_lib_subtype_id: dto.eqp_lib_subtype_id,
                    trip_type_id: dto.trip_type_id,
                    manufacturer_id: dto.manufacturer_id,
                    sz_name: dto.sz_name,
                    sz_type: dto.sz_type,
                    sz_description: dto.sz_description,
                    sz_catalog: dto.sz_catalog,
                    sz_dev_type: dto.sz_dev_type,
                    n_poles: dto.n_poles,
                    f_max_voltage: dto.f_max_voltage
                )
                context.insert(created)
                deviceById[dto.id] = created
            }
        }

        // transformer headers
        let existingTx = try context.fetch(FetchDescriptor<SkmTransformerModelHeader>())
        var txById = Dictionary(uniqueKeysWithValues: existingTx.map { ($0.id, $0) })
        for dto in bundle.transformer_models {
            if let row = txById[dto.id] {
                row.sz_name = dto.sz_name
                row.manufacturer_id = dto.manufacturer_id
                row.str_type = dto.str_type
                row.str_type_symbol = dto.str_type_symbol
                row.str_description = dto.str_description
                row.n_phase = dto.n_phase
            } else {
                let created = SkmTransformerModelHeader(
                    id: dto.id, sz_name: dto.sz_name, manufacturer_id: dto.manufacturer_id,
                    str_type: dto.str_type, str_type_symbol: dto.str_type_symbol,
                    str_description: dto.str_description, n_phase: dto.n_phase
                )
                context.insert(created)
                txById[dto.id] = created
            }
        }

        // Cable headers — same shape across AC/DC/IEEE-W; small helper.
        try upsertCableHeadersAc(bundle.cables_ac, context: context)
        try upsertCableHeadersDc(bundle.cables_dc, context: context)
        try upsertCableHeadersIeeeW(bundle.cables_ieee_w, context: context)

        // bus model headers
        let existingBus = try context.fetch(FetchDescriptor<SkmBusModelHeader>())
        var busById = Dictionary(uniqueKeysWithValues: existingBus.map { ($0.id, $0) })
        for dto in bundle.bus_models {
            if let row = busById[dto.id] {
                row.sz_name = dto.sz_name
                row.manufacturer_id = dto.manufacturer_id
                row.sz_type = dto.sz_type
                row.sz_description = dto.sz_description
                row.f_max_voltage = dto.f_max_voltage
            } else {
                let created = SkmBusModelHeader(
                    id: dto.id, sz_name: dto.sz_name, manufacturer_id: dto.manufacturer_id,
                    sz_type: dto.sz_type, sz_description: dto.sz_description,
                    f_max_voltage: dto.f_max_voltage
                )
                context.insert(created)
                busById[dto.id] = created
            }
        }

        AppLogger.log(.info, "SKM headers upsert: \(bundle.devices.count) devices, \(bundle.transformer_models.count) transformers, \(bundle.cables_ac.count) ac_cables, \(bundle.cables_dc.count) dc_cables, \(bundle.cables_ieee_w.count) ieee_cables, \(bundle.bus_models.count) bus, \(bundle.dev_lib_routing.count) routing rules", category: .sync)
    }

    // Cable-header upsert helpers — separate functions per model so
    // the @Model type is known statically (SwiftData @Model classes
    // can't be parameterized generically).
    private func upsertCableHeadersAc(_ dtos: [SkmCableHeaderDTO], context: ModelContext) throws {
        let existing = try context.fetch(FetchDescriptor<SkmCableAcHeader>())
        var byId = Dictionary(uniqueKeysWithValues: existing.map { ($0.id, $0) })
        for dto in dtos {
            if let row = byId[dto.id] {
                row.sz_name = dto.sz_name
                row.manufacturer_id = dto.manufacturer_id
                row.str_description = dto.str_description
                row.f_rated_voltage = dto.f_rated_voltage
            } else {
                let created = SkmCableAcHeader(
                    id: dto.id, sz_name: dto.sz_name, manufacturer_id: dto.manufacturer_id,
                    str_description: dto.str_description, f_rated_voltage: dto.f_rated_voltage
                )
                context.insert(created)
                byId[dto.id] = created
            }
        }
    }

    private func upsertCableHeadersDc(_ dtos: [SkmCableHeaderDTO], context: ModelContext) throws {
        let existing = try context.fetch(FetchDescriptor<SkmCableDcHeader>())
        var byId = Dictionary(uniqueKeysWithValues: existing.map { ($0.id, $0) })
        for dto in dtos {
            if let row = byId[dto.id] {
                row.sz_name = dto.sz_name
                row.manufacturer_id = dto.manufacturer_id
                row.str_description = dto.str_description
                row.f_rated_voltage = dto.f_rated_voltage
            } else {
                let created = SkmCableDcHeader(
                    id: dto.id, sz_name: dto.sz_name, manufacturer_id: dto.manufacturer_id,
                    str_description: dto.str_description, f_rated_voltage: dto.f_rated_voltage
                )
                context.insert(created)
                byId[dto.id] = created
            }
        }
    }

    private func upsertCableHeadersIeeeW(_ dtos: [SkmCableHeaderDTO], context: ModelContext) throws {
        let existing = try context.fetch(FetchDescriptor<SkmCableIeeeWHeader>())
        var byId = Dictionary(uniqueKeysWithValues: existing.map { ($0.id, $0) })
        for dto in dtos {
            if let row = byId[dto.id] {
                row.sz_name = dto.sz_name
                row.manufacturer_id = dto.manufacturer_id
                row.str_description = dto.str_description
                row.f_rated_voltage = dto.f_rated_voltage
            } else {
                let created = SkmCableIeeeWHeader(
                    id: dto.id, sz_name: dto.sz_name, manufacturer_id: dto.manufacturer_id,
                    str_description: dto.str_description, f_rated_voltage: dto.f_rated_voltage
                )
                context.insert(created)
                byId[dto.id] = created
            }
        }
    }

    //MARK: - Node Class Upsert
    
    @MainActor
    func smartUpsertNodeClassDTOs(
        dtos: [NodeClassDTO],
        into context: ModelContext
    ) throws {
        // Step 1: Extract and upsert icons (deduplicated)
        var uniqueIcons: [UUID: NodeIconDTO] = [:]
        for dto in dtos {
            if let icon = dto.icon {
                uniqueIcons[icon.id] = icon
            }
        }
        if !uniqueIcons.isEmpty {
            try smartUpsertNodeIcons(dtos: Array(uniqueIcons.values), into: context)
        }
        
        // Step 2: Extract and upsert orientations with terminals (deduplicated)
        var uniqueOrientations: [UUID: NodeOrientationDTO] = [:]
        for dto in dtos {
            if let orientation = dto.node_orientation {
                uniqueOrientations[orientation.id] = orientation
            }
        }
        if !uniqueOrientations.isEmpty {
            try smartUpsertNodeOrientations(dtos: Array(uniqueOrientations.values), into: context)
        }
        
        // Step 3: Fetch existing classes, properties, and subtypes
        let existingClasses = try context.fetch(FetchDescriptor<NodeClass>())
        let existingProps = try context.fetch(FetchDescriptor<NodeClassProperty>())
        let existingSubtypes = try context.fetch(FetchDescriptor<NodeSubtype>())
        
        // Create lookup dictionaries
        var classesById = Dictionary(uniqueKeysWithValues: existingClasses.map { ($0.id, $0) })
        var propsById = Dictionary(uniqueKeysWithValues: existingProps.map { ($0.id, $0) })
        var subtypesById = Dictionary(uniqueKeysWithValues: existingSubtypes.map { ($0.id, $0) })
        
        AppLogger.log(.info, "Upserting \(dtos.count) NodeClasses (existing: \(existingClasses.count))", category: .sync)
        
        // Track which IDs we've seen
        var seenClassIds = Set<UUID>()
        var seenPropIds = Set<UUID>()
        var seenSubtypeIds = Set<UUID>()
        
        // Step 4: Update existing or create new node classes
        for dto in dtos {
            seenClassIds.insert(dto.id)
            
            // Resolve icon_id: prefer explicit ID, fallback to embedded icon's ID
            let resolvedIconId = dto.icon_id ?? dto.icon?.id
            // Resolve node_orientation_id: prefer explicit ID, fallback to embedded orientation's ID
            let resolvedOrientationId = dto.node_orientation_id ?? dto.node_orientation?.id
            
            let nodeClass: NodeClass
            if let existing = classesById[dto.id] {
                // Update existing class
                existing.name = dto.name
                existing.style = dto.style
                existing.box = dto.box
                existing.ocp = dto.ocp
                existing.width = dto.width
                existing.height = dto.height
                existing.color = dto.color
                existing.needs_source = dto.needs_source
                existing.is_deleted = dto.is_deleted ?? false
                // FK references to separate entities
                existing.node_orientation_id = resolvedOrientationId
                existing.icon_id = resolvedIconId
                // Additional fields
                existing.orientation = dto.orientation ?? "vertical"
                existing.in_ports = dto.in_ports ?? 1
                existing.out_ports = dto.out_ports ?? 1
                existing.company_id = dto.company_id
                existing.is_global = dto.is_global
                existing.is_override = dto.is_override
                existing.for_entity = dto.for_entity
                existing.skm_config = dto.skm_config?.toData()
                existing.default_datablock_config = dto.default_datablock_config
                existing.use_eqp_lib = dto.use_eqp_lib ?? false
                existing.primary_secondary_voltage = dto.primary_secondary_voltage ?? false
                // ZP-2161
                existing.tertiary_voltage = dto.tertiary_voltage ?? false
                existing.eqp_lib_type_id = dto.eqp_lib_type_id
                existing.eqp_lib_type_name = dto.eqp_lib_type_name
                existing.device_role_id = dto.device_role_id
                existing.device_role_code = dto.device_role_code
                existing.is_pseudo_edge = dto.is_pseudo_edge ?? false
                existing.is_impedance = dto.is_impedance ?? false
                existing.is_default_impedance = dto.is_default_impedance ?? false
                existing.is_node_bus = dto.is_node_bus ?? false
                existing.has_panel_schedule = dto.has_panel_schedule ?? false
                existing.class_description = dto.description
                nodeClass = existing
            } else {
                // Create new class
                nodeClass = NodeClass(
                    id: dto.id,
                    name: dto.name,
                    style: dto.style,
                    box: dto.box,
                    definition: [],
                    ocp: dto.ocp,
                    width: dto.width,
                    height: dto.height,
                    color: dto.color,
                    needs_source: dto.needs_source,
                    is_deleted: dto.is_deleted ?? false,
                    node_orientation_id: resolvedOrientationId,
                    icon_id: resolvedIconId,
                    orientation: dto.orientation ?? "vertical",
                    in_ports: dto.in_ports ?? 1,
                    out_ports: dto.out_ports ?? 1,
                    company_id: dto.company_id,
                    is_global: dto.is_global,
                    is_override: dto.is_override,
                    for_entity: dto.for_entity,
                    skm_config: dto.skm_config?.toData(),
                    default_datablock_config: dto.default_datablock_config,
                    use_eqp_lib: dto.use_eqp_lib ?? false,
                    primary_secondary_voltage: dto.primary_secondary_voltage ?? false
                )
                // ZP-2161 — set new fields outside init() since the
                // initializer signature is unchanged.
                nodeClass.tertiary_voltage = dto.tertiary_voltage ?? false
                nodeClass.eqp_lib_type_id = dto.eqp_lib_type_id
                nodeClass.eqp_lib_type_name = dto.eqp_lib_type_name
                nodeClass.device_role_id = dto.device_role_id
                nodeClass.device_role_code = dto.device_role_code
                nodeClass.is_pseudo_edge = dto.is_pseudo_edge ?? false
                nodeClass.is_impedance = dto.is_impedance ?? false
                nodeClass.is_default_impedance = dto.is_default_impedance ?? false
                nodeClass.is_node_bus = dto.is_node_bus ?? false
                nodeClass.has_panel_schedule = dto.has_panel_schedule ?? false
                nodeClass.class_description = dto.description
                context.insert(nodeClass)
                classesById[dto.id] = nodeClass
            }
            
            // 3) Update or create properties for this class
            for propDTO in dto.definition {
                seenPropIds.insert(propDTO.id)
                
                if let existing = propsById[propDTO.id] {
                    // Update existing property
                    existing.name = propDTO.name
                    existing.type = propDTO.type
                    existing.options = propDTO.options ?? []
                    existing.af_required = propDTO.af_required
                    existing.index = propDTO.index
                    existing.default_value = propDTO.default_value
                    existing.nodeClass = nodeClass
                } else {
                    // Create new property
                    let newProp = NodeClassProperty(
                        id: propDTO.id,
                        name: propDTO.name,
                        type: propDTO.type,
                        options: propDTO.options ?? [],
                        af_required: propDTO.af_required,
                        index: propDTO.index,
                        default_value: propDTO.default_value,
                        nodeClass: nodeClass
                    )
                    context.insert(newProp)
                    propsById[propDTO.id] = newProp
                }
            }
            
            // 4) Update or create node subtypes for this class
            if let subtypeDTOs = dto.node_subtypes {
                for subtypeDTO in subtypeDTOs {
                    seenSubtypeIds.insert(subtypeDTO.id)
                    
                    if let existing = subtypesById[subtypeDTO.id] {
                        // Update existing subtype
                        existing.name = subtypeDTO.name
                        existing.node_class_id = subtypeDTO.node_class_id ?? dto.id
                        existing.is_global = subtypeDTO.is_global ?? false
                        existing.is_deleted = subtypeDTO.is_deleted ?? false
                        existing.company_id = subtypeDTO.company_id
                        existing.nodeClass = nodeClass
                        // ZP-2161
                        existing.eqp_lib_subtype_id = subtypeDTO.eqp_lib_subtype_id
                        existing.volt_floor = subtypeDTO.volt_floor
                        existing.volt_ceiling = subtypeDTO.volt_ceiling
                        existing.amp_floor = subtypeDTO.amp_floor
                        existing.amp_ceiling = subtypeDTO.amp_ceiling
                        existing.subtype_description = subtypeDTO.description
                        existing.voltage_level = subtypeDTO.voltage_level
                        existing.replacement_cost = subtypeDTO.replacement_cost
                        existing.is_override = subtypeDTO.is_override
                        existing.for_entity = subtypeDTO.for_entity
                    } else {
                        // Create new subtype
                        let newSubtype = NodeSubtype(
                            id: subtypeDTO.id,
                            name: subtypeDTO.name,
                            node_class_id: subtypeDTO.node_class_id ?? dto.id,
                            is_global: subtypeDTO.is_global ?? false,
                            is_deleted: subtypeDTO.is_deleted ?? false,
                            company_id: subtypeDTO.company_id,
                            nodeClass: nodeClass
                        )
                        // ZP-2161 — populate new fields outside init()
                        newSubtype.eqp_lib_subtype_id = subtypeDTO.eqp_lib_subtype_id
                        newSubtype.volt_floor = subtypeDTO.volt_floor
                        newSubtype.volt_ceiling = subtypeDTO.volt_ceiling
                        newSubtype.amp_floor = subtypeDTO.amp_floor
                        newSubtype.amp_ceiling = subtypeDTO.amp_ceiling
                        newSubtype.subtype_description = subtypeDTO.description
                        newSubtype.voltage_level = subtypeDTO.voltage_level
                        newSubtype.replacement_cost = subtypeDTO.replacement_cost
                        newSubtype.is_override = subtypeDTO.is_override
                        newSubtype.for_entity = subtypeDTO.for_entity
                        context.insert(newSubtype)
                        subtypesById[subtypeDTO.id] = newSubtype
                    }
                }
            }
        }
        
        // 5) Delete classes, properties, and subtypes that weren't in the DTOs
        for (id, nodeClass) in classesById where !seenClassIds.contains(id) {
            context.delete(nodeClass)
        }

        // For each stale NodeClassProperty, nil out NodeProperty rows that
        // reference it before deleting, otherwise the per-node value rows keep
        // a SwiftData pointer to a tombstoned backing object and the next read
        // crashes with BackingData.swift "This model instance was invalidated"
        // (ZP-2366). The id-keyed predicate uses SwiftData's indexed lookup,
        // so we only fetch rows that actually reference this property.
        for (id, propToDelete) in propsById where !seenPropIds.contains(id) {
            let targetId = propToDelete.id
            let referencing = try context.fetch(
                FetchDescriptor<NodeProperty>(
                    predicate: #Predicate<NodeProperty> { $0.node_class_property?.id == targetId }
                )
            )
            for ref in referencing {
                ref.node_class_property = nil
            }
            context.delete(propToDelete)
        }

        for (id, subtype) in subtypesById where !seenSubtypeIds.contains(id) {
            context.delete(subtype)
        }
        
        // 6) One single save at the end
        do {
            try context.save()
            AppLogger.log(.info, "Node Class sync completed successfully", category: .sync)
            
            // Log final state
            let fnc = try context.fetch(FetchDescriptor<NodeClass>())
            let fncp = try context.fetch(FetchDescriptor<NodeClassProperty>())
            let fnst = try context.fetch(FetchDescriptor<NodeSubtype>())
            let fni = try context.fetch(FetchDescriptor<NodeIcon>())
            let fno = try context.fetch(FetchDescriptor<NodeOrientation>())
            let fnot = try context.fetch(FetchDescriptor<NodeOrientationTerminal>())
            
            AppLogger.log(.info, "Final counts:", category: .sync)
            AppLogger.log(.info, "Node Classes: \(fnc.count)", category: .sync)
            AppLogger.log(.info, "Node Class Properties: \(fncp.count)", category: .sync)
            AppLogger.log(.info, "Node Subtypes: \(fnst.count)", category: .sync)
            AppLogger.log(.info, "Node Icons: \(fni.count)", category: .sync)
            AppLogger.log(.info, "Node Orientations: \(fno.count)", category: .sync)
            AppLogger.log(.info, "Orientation Terminals: \(fnot.count)", category: .sync)
            
        } catch {
            AppLogger.log(.error, "Save error: \(error)", category: .sync)
            throw error
        }
        
    }
    
    //MARK: - Edge Class Upsert
    
    @MainActor
    func smartUpsertEdgeClassDTOs(
        dtos: [EdgeClassDTO],
        into context: ModelContext
    ) throws {
        // 1) Fetch existing classes and properties
        let existingClasses = try context.fetch(FetchDescriptor<EdgeClass>())
        let existingProps = try context.fetch(FetchDescriptor<EdgeClassProperty>())
        
        // Create lookup dictionaries
        var classesById = Dictionary(uniqueKeysWithValues: existingClasses.map { ($0.id, $0) })
        var propsById = Dictionary(uniqueKeysWithValues: existingProps.map { ($0.id, $0) })
        
        AppLogger.log(.info, "Upserting \(dtos.count) EdgeClasses (existing: \(existingClasses.count))", category: .sync)
        
        // Track which IDs we've seen
        var seenClassIds = Set<UUID>()
        var seenPropIds = Set<UUID>()
        
        // 2) Update existing or create new edge classes
        for dto in dtos {
            seenClassIds.insert(dto.id)
            
            let edgeClass: EdgeClass
            if let existing = classesById[dto.id] {
                // Update existing class
                existing.name = dto.name
                existing.is_deleted = dto.is_deleted ?? false
                edgeClass = existing
            } else {
                // Create new class
                edgeClass = EdgeClass(
                    id: dto.id,
                    name: dto.name,
                    definition: [],
                    is_deleted: dto.is_deleted ?? false
                )
                context.insert(edgeClass)
                classesById[dto.id] = edgeClass
            }
            
            // 3) Update or create properties for this class
            for p in dto.definition {
                seenPropIds.insert(p.id)
                
                if let existing = propsById[p.id] {
                    // Update existing property
                    existing.name = p.name
                    existing.type = p.type
                    existing.options = p.options ?? []
                    existing.af_required = p.af_required
                    existing.index = p.index
                    existing.default_value = p.default_value
                    existing.edgeClass = edgeClass
                } else {
                    // Create new property
                    let newProp = EdgeClassProperty(
                        id: p.id,
                        name: p.name,
                        type: p.type,
                        options: p.options ?? [],
                        af_required: p.af_required,
                        index: p.index,
                        default_value: p.default_value,
                        edgeClass: edgeClass
                    )
                    context.insert(newProp)
                    propsById[p.id] = newProp
                }
            }
        }
        
        // 4) Delete classes and properties that weren't in the DTOs
        for (id, edgeClass) in classesById where !seenClassIds.contains(id) {
            context.delete(edgeClass)
        }

        // For each stale EdgeClassProperty, nil out EdgeProperty rows that
        // reference it before deleting, to avoid the BackingData dangling-
        // pointer crash (ZP-2366). The id-keyed predicate uses SwiftData's
        // indexed lookup so we only fetch rows that actually reference this
        // property.
        for (id, propToDelete) in propsById where !seenPropIds.contains(id) {
            let targetId = propToDelete.id
            let referencing = try context.fetch(
                FetchDescriptor<EdgeProperty>(
                    predicate: #Predicate<EdgeProperty> { $0.edge_class_property?.id == targetId }
                )
            )
            for ref in referencing {
                ref.edge_class_property = nil
            }
            context.delete(propToDelete)
        }
        
        // 5) One single save at the end
        do {
            try context.save()
            AppLogger.log(.info, "Edge Class sync completed successfully", category: .sync)
            
            // Log final state
            let fnc = try context.fetch(FetchDescriptor<EdgeClass>())
            let fncp = try context.fetch(FetchDescriptor<EdgeClassProperty>())
            
            AppLogger.log(.info, "Final counts:", category: .sync)
            AppLogger.log(.info, "Edge Classes: \(fnc.count)", category: .sync)
            AppLogger.log(.info, "Edge Class Properties: \(fncp.count)", category: .sync)
            
        } catch {
            AppLogger.log(.error, "Save error: \(error)", category: .sync)
            throw error
        }
        
    }
    
    //**MARK: - Issue Class DTOs**
    @MainActor
    func smartUpsertIssueClassDTOs(
        dtos: [IssueClassDTO],
        into context: ModelContext
    ) throws {
        // 1) Fetch existing classes and properties
        let existingClasses = try context.fetch(FetchDescriptor<IssueClass>())
        let existingProps = try context.fetch(FetchDescriptor<IssueClassProperty>())
        
        // Create lookup dictionaries
        var classesById = Dictionary(uniqueKeysWithValues: existingClasses.map { ($0.id, $0) })
        var propsById = Dictionary(uniqueKeysWithValues: existingProps.map { ($0.id, $0) })
        
        AppLogger.log(.info, "Upserting \(dtos.count) IssueClasses (existing: \(existingClasses.count))", category: .sync)
        
        // Track which IDs we've seen
        var seenClassIds = Set<UUID>()
        var seenPropIds = Set<UUID>()
        
        // 2) Update existing or create new issue classes
        for dto in dtos {
            seenClassIds.insert(dto.id)
            
            let issueClass: IssueClass
            if let existing = classesById[dto.id] {
                // Update existing class
                existing.name = dto.name
                existing.is_deleted = dto.is_deleted ?? false
                issueClass = existing
            } else {
                // Create new class
                issueClass = IssueClass(
                    id: dto.id,
                    name: dto.name,
                    definition: [],
                    is_deleted: dto.is_deleted ?? false
                )
                context.insert(issueClass)
                classesById[dto.id] = issueClass
            }
            
            // 3) Update or create properties for this class
            for p in dto.definition {
                seenPropIds.insert(p.id)
                
                if let existing = propsById[p.id] {
                    // Update existing property
                    existing.name = p.name
                    existing.type = p.type
                    existing.options = p.options ?? []
                    existing.af_required = p.af_required
                    existing.issueClass = issueClass
                    existing.issueDescription = p.description
                    existing.columns = p.columns
                    existing.internal_type = p.internal_type
                    existing.index = p.index
                    existing.calculationExpression = p.calculationExpression
                    existing.calculationPrecision = p.calculationPrecision
                    existing.allowDescription = p.allowDescription
                    existing.default_value = p.default_value
                    existing.autoFillRulesJSON = p.autoFillRules.flatMap { rules in
                        guard let data = try? JSONSerialization.data(withJSONObject: rules),
                              let str = String(data: data, encoding: .utf8) else { return nil }
                        return str
                    }
                } else {
                    // Create new property
                    let autoFillRulesJSON: String? = p.autoFillRules.flatMap { rules in
                        guard let data = try? JSONSerialization.data(withJSONObject: rules),
                              let str = String(data: data, encoding: .utf8) else { return nil }
                        return str
                    }
                    let newProp = IssueClassProperty(
                        id: p.id,
                        name: p.name,
                        type: p.type,
                        options: p.options ?? [],
                        af_required: p.af_required,
                        issueClass: issueClass,
                        issueDescription: p.description,
                        columns: p.columns,
                        internal_type: p.internal_type,
                        index: p.index,
                        calculationExpression: p.calculationExpression,
                        calculationPrecision: p.calculationPrecision,
                        allowDescription: p.allowDescription,
                        autoFillRulesJSON: autoFillRulesJSON,
                        default_value: p.default_value
                    )
                    context.insert(newProp)
                    propsById[p.id] = newProp
                }
            }
        }
        
        // 4) Delete classes and properties that weren't in the DTOs
        for (id, issueClass) in classesById where !seenClassIds.contains(id) {
            context.delete(issueClass)
        }

        // For each stale IssueClassProperty, nil out IssueProperty rows that
        // reference it before deleting, to avoid the BackingData dangling-
        // pointer crash (ZP-2366). The id-keyed predicate uses SwiftData's
        // indexed lookup so we only fetch rows that actually reference this
        // property.
        for (id, propToDelete) in propsById where !seenPropIds.contains(id) {
            let targetId = propToDelete.id
            let referencing = try context.fetch(
                FetchDescriptor<IssueProperty>(
                    predicate: #Predicate<IssueProperty> { $0.issue_class_property?.id == targetId }
                )
            )
            for ref in referencing {
                ref.issue_class_property = nil
            }
            context.delete(propToDelete)
        }
        
        // 5) One single save at the end
        do {
            try context.save()
            AppLogger.log(.info, "Issue Class sync completed successfully", category: .sync)
            
            // Log final state with more detail
            let fnc = try context.fetch(FetchDescriptor<IssueClass>())
            let fncp = try context.fetch(FetchDescriptor<IssueClassProperty>())
            
            AppLogger.log(.info, "Final counts:", category: .sync)
            AppLogger.log(.info, "Issue Classes: \(fnc.count)", category: .sync)
            AppLogger.log(.info, "Issue Class Properties: \(fncp.count)", category: .sync)
            
            // Debug: Log table properties to verify they have columns
            for prop in fncp where prop.type == "table_with_column_headers" {
                AppLogger.log(.info, "Table property: \(prop.name)", category: .sync)
                AppLogger.log(.info, "Columns: \(prop.columns ?? [])", category: .sync)
                AppLogger.log(.info, "Internal types: \(prop.internal_type ?? [])", category: .sync)
            }
            
        } catch {
            AppLogger.log(.error, "Save error: \(error)", category: .sync)
            throw error
        }
    }
    
    //**MARK: - Shortcut DTOs**
    @MainActor
    func smartUpsertShortcutDTOs(
        dtos: [NodeShortcutDTO],
        into context: ModelContext
    ) throws {
        let existingShortcuts = try context.fetch(FetchDescriptor<NodeShortcut>())
        var shortcutsById = Dictionary(uniqueKeysWithValues: existingShortcuts.map { ($0.id, $0) })
        var seenIds = Set<UUID>()
        
        AppLogger.log(.info, "Upserting \(dtos.count) Shortcuts (existing: \(existingShortcuts.count))", category: .sync)
        
        for dto in dtos {
            seenIds.insert(dto.id)
            if let existing = shortcutsById[dto.id] {
                // Update existing shortcut with ALL fields from Android
                existing.name = dto.name
                existing.company_id = dto.company_id
                existing.node_class_id = dto.node_class_id
                existing.node_subtype_id = dto.node_subtype_id
                existing.for_entity = dto.for_entity
                existing.is_global = dto.is_global
                existing.global_default = dto.global_default
                existing.is_default = dto.is_default
                existing.is_deleted = dto.is_deleted
                existing.is_override = dto.is_override
                
                // Parse dates
                let dateFormatter = ISO8601DateFormatter()
                existing.created_at = dto.created_at.flatMap { dateFormatter.date(from: $0) }
                existing.modified_at = dto.modified_at.flatMap { dateFormatter.date(from: $0) }
                existing.lastSyncedAt = Date()
            } else {
                // Create new
                let newShortcut = dto.toModel()
                newShortcut.lastSyncedAt = Date()
                context.insert(newShortcut)
                shortcutsById[dto.id] = newShortcut
            }
        }
        
        // Soft delete unseen shortcuts (matching Android behavior)
        for shortcut in existingShortcuts where !seenIds.contains(shortcut.id) && !shortcut.is_deleted {
            shortcut.is_deleted = true
            AppLogger.log(.notice, "Marking shortcut as deleted: \(shortcut.name)", category: .sync)
        }
        
        try context.save()
        AppLogger.log(.info, "Shortcuts upserted successfully", category: .sync)
    }

    // MARK: - Test Equipment Upsert
    func smartUpsertTestEquipmentDTOs(
        dtos: [TestEquipmentDTO],
        into context: ModelContext
    ) throws {
        let existingEquipment = try context.fetch(FetchDescriptor<TestEquipment>())
        var equipmentById = Dictionary(uniqueKeysWithValues: existingEquipment.map { ($0.id, $0) })
        var seenIds = Set<UUID>()

        AppLogger.log(.info, "Upserting \(dtos.count) TestEquipment (existing: \(existingEquipment.count))", category: .sync)

        for dto in dtos {
            seenIds.insert(dto.id)
            if let existing = equipmentById[dto.id] {
                existing.name = dto.name
                existing.serialNumber = dto.serial_number
                existing.calibrationDate = dto.calibration_date
                existing.testEquipmentLibraryId = dto.test_equipment_library_id
            } else {
                let newEquipment = TestEquipment(
                    id: dto.id,
                    name: dto.name,
                    serialNumber: dto.serial_number,
                    calibrationDate: dto.calibration_date,
                    testEquipmentLibraryId: dto.test_equipment_library_id
                )
                context.insert(newEquipment)
                equipmentById[dto.id] = newEquipment
            }
        }

        // Delete stale equipment no longer returned by server
        for equipment in existingEquipment where !seenIds.contains(equipment.id) {
            context.delete(equipment)
            AppLogger.log(.notice, "Deleting stale test equipment: \(equipment.name)", category: .sync)
        }

        try context.save()
        AppLogger.log(.info, "TestEquipment upserted successfully", category: .sync)
    }

    // MARK: - Procedure Upsert
    /// Mirrors the test-equipment pattern: upsert by id, then prune any
    /// local row the server didn't return so the picker doesn't surface
    /// retired procedures.
    func smartUpsertProcedureDTOs(
        dtos: [ProcedureDTO],
        into context: ModelContext
    ) throws {
        // The procedure fetch is tolerant (wrapped in `try?` at the call
        // site), so an empty payload almost always means the lookup
        // request failed — not that the catalog is genuinely empty.
        // Pruning here would wipe the whole local cache on a transient
        // failure and empty the task-creation picker. Skip instead;
        // the next successful sync reconciles deletions.
        guard !dtos.isEmpty else {
            AppLogger.log(.notice, "Skipping Procedure upsert — empty payload (likely fetch failure)", category: .sync)
            return
        }

        let existing = try context.fetch(FetchDescriptor<Procedure>())
        var byId = Dictionary(uniqueKeysWithValues: existing.map { ($0.id, $0) })
        var seenIds = Set<UUID>()

        AppLogger.log(.info, "Upserting \(dtos.count) Procedures (existing: \(existing.count))", category: .sync)

        for dto in dtos {
            seenIds.insert(dto.id)
            if let row = byId[dto.id] {
                row.name = dto.name
                row.procedure_description = dto.description
                row.procedure_master_name = dto.procedure_master_name
                row.form_id = dto.form_id
                row.use_proxy = dto.use_proxy ?? false
                row.node_class_id = dto.node_class_id
                row.node_subtype = dto.node_subtype
                row.node_subtype_id = dto.node_subtype_id
                row.is_deleted = false
            } else {
                let newRow = Procedure(
                    id: dto.id,
                    name: dto.name,
                    procedure_description: dto.description,
                    procedure_master_name: dto.procedure_master_name,
                    form_id: dto.form_id,
                    use_proxy: dto.use_proxy ?? false,
                    node_class_id: dto.node_class_id,
                    node_subtype: dto.node_subtype,
                    node_subtype_id: dto.node_subtype_id,
                    is_deleted: false
                )
                context.insert(newRow)
                byId[dto.id] = newRow
            }
        }

        for row in existing where !seenIds.contains(row.id) {
            context.delete(row)
        }

        try context.save()
        AppLogger.log(.info, "Procedures upserted successfully", category: .sync)
    }

    // MARK: - MAIN UPSERT HELPER: UserTaskForm Processing Function
    @MainActor
    private func processUserTaskForms(
        _ userTaskFormDTOs: [UserTaskFormDTO],
        formsByID: inout [UUID: UserTaskForm],
        context: ModelContext
    ) async throws {
        AppLogger.log(.debug, "\nStep 1: Processing UserTaskForms...", category: .sync)
        let existingForms = try context.fetch(FetchDescriptor<UserTaskForm>())
        formsByID = Dictionary(uniqueKeysWithValues: existingForms.map { ($0.id, $0) })

        for (index, dto) in userTaskFormDTOs.enumerated() {
            if index > 0 && index % progressUpdateInterval == 0 {
                await Task.yield()
            }
            if let existing = formsByID[dto.id] {
                existing.schema = dto.schema
                existing.title = dto.title
                existing.is_global = dto.is_global
                existing.is_deleted = dto.is_deleted
                existing.node_class_id = dto.node_class_id
                existing.node_subtype = dto.node_subtype
                AppLogger.log(.info, "Updated form: \(dto.title)", category: .sync)
            } else {
                let newForm = UserTaskForm(
                    id: dto.id,
                    schema: dto.schema,
                    title: dto.title,
                    is_global: dto.is_global,
                    is_deleted: dto.is_deleted,
                    node_class_id: dto.node_class_id,
                    node_subtype: dto.node_subtype
                )
                context.insert(newForm)
                formsByID[dto.id] = newForm
                AppLogger.log(.info, "✨ Created form: \(dto.title)", category: .sync)
            }
        }
    }
    
    // MARK: - MAIN UPSERT HELPER: SLD Processing Function
    @MainActor
    private func processSLD(
        _ sldDTO: SLDDTO,
        existingSLDs: [SLDV2],
        sldLookup: inout [UUID: SLDV2],
        context: ModelContext
    ) -> SLDV2 {
        AppLogger.log(.info, "\n🏗️ Processing SLD: \(sldDTO.name) (id: \(sldDTO.id))", category: .sync)
        
        let sld: SLDV2
        if let existingSLD = sldLookup[sldDTO.id] {
            existingSLD.name = sldDTO.name
            
            // Update address fields
            existingSLD.address_line_1 = sldDTO.address_line_1
            existingSLD.address_line_2 = sldDTO.address_line_2
            existingSLD.city = sldDTO.city
            existingSLD.state_province = sldDTO.state_province
            existingSLD.postal_code = sldDTO.postal_code
            existingSLD.country_code = sldDTO.country_code
            existingSLD.address_formatted = sldDTO.address_formatted
            existingSLD.address_latitude = sldDTO.address_coordinates?["latitude"]
            existingSLD.address_longitude = sldDTO.address_coordinates?["longitude"]
            
            sld = existingSLD
            // print(" Updated SLD: \(sldDTO.name)")
        } else {
            sld = SLDV2(
                id: sldDTO.id,
                name: sldDTO.name,
                nodes: [],
                edges: [],
                photos: [],
                ir_photos: [],
                ir_sessions: [],
                user_tasks: [],
                issues: [],
                quotes: [],
                comments: [],
                address_line_1: sldDTO.address_line_1,
                address_line_2: sldDTO.address_line_2,
                city: sldDTO.city,
                state_province: sldDTO.state_province,
                postal_code: sldDTO.postal_code,
                country_code: sldDTO.country_code,
                address_formatted: sldDTO.address_formatted,
                address_latitude: sldDTO.address_coordinates?["latitude"],
                address_longitude: sldDTO.address_coordinates?["longitude"]
            )
            context.insert(sld)
            sldLookup[sldDTO.id] = sld
            // print(" ✨ Created SLD: \(sldDTO.name)")
        }
        
        return sld
    }
    
    // MARK: - MAIN UPSERT HELPER: Node Processing Function
    
    @MainActor
    private func processNodes(
        _ nodeDTOs: [SLDDTONode],
        sld: SLDV2,
        nodeLookup: inout [UUID: NodeV2],
        roomLookup: [UUID: Room],
        context: ModelContext
    ) async throws {
        
        let totalNodes = nodeDTOs.count
        AppLogger.log(.info, "Processing \(totalNodes) nodes...", category: .sync)

        // [ZAP] Debug voltage/notes field population from API
        let zapWithVoltage = nodeDTOs.filter { $0.voltage != nil }
        let zapWithVoltageId = nodeDTOs.filter { $0.voltage_id != nil }
        let zapWithSecV = nodeDTOs.filter { $0.secondary_voltage != nil }
        let zapWithSecVId = nodeDTOs.filter { $0.secondary_voltage_id != nil }
        let zapWithNotes = nodeDTOs.filter { $0.notes != nil && !($0.notes?.isEmpty ?? true) }
        AppLogger.log(.info, "[ZAP] Node sync — total: \(totalNodes), voltage: \(zapWithVoltage.count), voltage_id: \(zapWithVoltageId.count), sec_voltage: \(zapWithSecV.count), sec_voltage_id: \(zapWithSecVId.count), notes: \(zapWithNotes.count)", category: .sync)
        for dto in zapWithVoltage.prefix(3) {
            AppLogger.log(.info, "[ZAP] Sample — \(dto.label): voltage=\(dto.voltage ?? 0), voltage_id=\(dto.voltage_id ?? 0), sec_voltage=\(dto.secondary_voltage ?? 0), sec_voltage_id=\(dto.secondary_voltage_id ?? 0), notes=\(dto.notes ?? "nil")", category: .sync)
        }
        for dto in zapWithNotes.prefix(3) where dto.voltage == nil {
            AppLogger.log(.info, "[ZAP] Sample (notes only) — \(dto.label): notes=\(dto.notes ?? "nil")", category: .sync)
        }

        for (index, dto) in nodeDTOs.enumerated() {
            // Update progress periodically and yield to allow UI updates
            if shouldUpdateProgress(index: index, total: totalNodes, step: .nodes) {
                await Task.yield()
            }
            
            let nodeClass = try fetchNodeClass(dto.node_class, context: context)
            let nodeSubtype = try fetchNodeSubtype(dto.node_subtype, context: context)
            
            if let node = nodeLookup[dto.id] {
                try updateNode(
                    node,
                    from: dto,
                    sld: sld,
                    nodeClass: nodeClass,
                    nodeSubtype: nodeSubtype,
                    roomLookup: roomLookup,
                    context: context
                )
            } else {
                let node = try createNode(
                    from: dto,
                    sld: sld,
                    nodeClass: nodeClass,
                    nodeSubtype: nodeSubtype,
                    roomLookup: roomLookup,
                    context: context
                )
                nodeLookup[dto.id] = node
            }
        }
    }
    
    private func fetchNodeClass(
        _ id: UUID?,
        context: ModelContext
    ) throws -> NodeClass? {
        guard let id else { return nil }
        
        let desc = FetchDescriptor<NodeClass>(
            predicate: #Predicate { $0.id == id }
        )
        return try context.fetch(desc).first
    }
    
    private func fetchNodeSubtype(
        _ id: UUID?,
        context: ModelContext
    ) throws -> NodeSubtype? {
        guard let id else { return nil }
        
        let desc = FetchDescriptor<NodeSubtype>(
            predicate: #Predicate { $0.id == id }
        )
        return try context.fetch(desc).first
    }
    
    private func fetchNodeClassProperty(
        _ idString: String,
        context: ModelContext
    ) throws -> NodeClassProperty? {
        guard let uuid = UUID(uuidString: idString) else { return nil }
        
        let desc = FetchDescriptor<NodeClassProperty>(
            predicate: #Predicate { $0.id == uuid }
        )
        return try context.fetch(desc).first
    }
    
    
    private func updateNode(
        _ node: NodeV2,
        from dto: SLDDTONode,
        sld: SLDV2,
        nodeClass: NodeClass?,
        nodeSubtype: NodeSubtype?,
        roomLookup: [UUID: Room],
        context: ModelContext
    ) throws {
        
        node.label = dto.label
        node.type = dto.type
        node.parent_id = dto.parent_id
        node.x = dto.x
        node.y = dto.y
        node.width = dto.width
        node.height = dto.height
        node.sld = sld
        node.is_deleted = dto.is_deleted
        node.location = dto.location
        node.node_class = nodeClass
        node.node_subtype = nodeSubtype
        node.com = dto.com
        node.com_calculation = dto.com_calculation?.toModel()
        node.qr_code = dto.qr_code
        node.serviceability = dto.serviceability
        node.serviceability_note = dto.serviceability_note
        node.voltage = dto.voltage
        node.voltage_id = dto.voltage_id
        node.secondary_voltage = dto.secondary_voltage
        node.secondary_voltage_id = dto.secondary_voltage_id
        node.tertiary_voltage = dto.tertiary_voltage
        node.tertiary_voltage_id = dto.tertiary_voltage_id
        node.system_voltage_id = dto.system_voltage_id
        node.circuit_voltage_id = dto.circuit_voltage_id
        node.voltage_user_overridden = dto.voltage_user_overridden
        node.notes = dto.notes
        node.default_photo_id = dto.default_photo_id
        node.suggested_shortcut_id = dto.suggested_shortcut
        node.applied_shortcut_id = dto.applied_shortcut
        node.eqp_lib = dto.eqp_lib
        node.eqp_lib_suggested = dto.eqp_lib_suggested
        node.eqp_note = dto.eqp_note
        node.eqp_engineering_approved = dto.eqp_engineering_approved
        node.skm_lib_name = dto.skm_lib_name
        node.skm_lib_name_suggested = dto.skm_lib_name_suggested
        node.ocr_signature = dto.ocr_signature

        // Transformer
        node.kva_rating = dto.kva_rating
        node.percent_impedance = dto.percent_impedance

        // Box-level
        node.mains_type_id = dto.mains_type_id
        node.phase_configuration_id = dto.phase_configuration_id

        // OCP
        node.ampere_rating = dto.ampere_rating
        node.pole_count = dto.pole_count
        node.manufacturer_id = dto.manufacturer_id
        node.has_trip_unit = dto.has_trip_unit
        node.trip_type_id = dto.trip_type_id
        node.frame_amps = dto.frame_amps
        node.sensor_amps = dto.sensor_amps
        node.plug_amps = dto.plug_amps

        // Cable / Busway
        node.length = dto.length
        node.conductor_material = dto.conductor_material
        node.cable_size_id = dto.cable_size_id
        node.conductor_configuration_id = dto.conductor_configuration_id
        node.duct_material_id = dto.duct_material_id
        node.conductor_description_id = dto.conductor_description_id
        node.insulation_class_id = dto.insulation_class_id
        node.insulation_type_id = dto.insulation_type_id
        node.installation_id = dto.installation_id
        node.busway_ampere_rating = dto.busway_ampere_rating

        // Misc
        node.replacement_cost = dto.replacement_cost
        node.panel_schedule_status = dto.panel_schedule_status
        node.rotation = dto.rotation
        node.locked = dto.locked

        node.room = dto.room_id.flatMap { roomLookup[$0] }

        try syncNodeCoreAttributes(
            node: node,
            incoming: dto.core_attributes,
            context: context
        )

        try syncNodeTerminals(
            node: node,
            incoming: dto.node_terminals,
            context: context
        )
    }


    private func createNode(
        from dto: SLDDTONode,
        sld: SLDV2,
        nodeClass: NodeClass?,
        nodeSubtype: NodeSubtype?,
        roomLookup: [UUID: Room],
        context: ModelContext
    ) throws -> NodeV2 {
        
        let node = NodeV2(
            id: dto.id,
            label: dto.label,
            type: dto.type,
            sld: sld,
            parent_id: dto.parent_id,
            x: dto.x,
            y: dto.y,
            width: dto.width,
            height: dto.height,
            photos: [],
            is_deleted: dto.is_deleted,
            location: dto.location,
            node_class: nodeClass,
            node_subtype: nodeSubtype,
            core_attributes: [],
            node_terminals: [],
            node_tasks: [],
            ir_photos: [],
            com: dto.com,
            com_calculation: dto.com_calculation?.toModel(),
            qr_code: dto.qr_code,
            serviceability: dto.serviceability,
            serviceability_note: dto.serviceability_note,
            voltage: dto.voltage,
            voltage_id: dto.voltage_id,
            secondary_voltage: dto.secondary_voltage,
            secondary_voltage_id: dto.secondary_voltage_id,
            notes: dto.notes,
            default_photo_id: dto.default_photo_id,
            suggested_shortcut_id: dto.suggested_shortcut,
            eqp_lib: dto.eqp_lib
        )

        node.room = dto.room_id.flatMap { roomLookup[$0] }

        // ZP-2161: assign new engineering fields outside the init() so
        // the existing initializer signature stays unchanged. All
        // defaults are nil on the model side; we just overlay what the
        // DTO carries.
        node.tertiary_voltage = dto.tertiary_voltage
        node.tertiary_voltage_id = dto.tertiary_voltage_id
        node.system_voltage_id = dto.system_voltage_id
        node.circuit_voltage_id = dto.circuit_voltage_id
        node.voltage_user_overridden = dto.voltage_user_overridden
        node.applied_shortcut_id = dto.applied_shortcut
        node.eqp_lib_suggested = dto.eqp_lib_suggested
        node.eqp_note = dto.eqp_note
        node.eqp_engineering_approved = dto.eqp_engineering_approved
        node.skm_lib_name = dto.skm_lib_name
        node.skm_lib_name_suggested = dto.skm_lib_name_suggested
        node.ocr_signature = dto.ocr_signature

        node.kva_rating = dto.kva_rating
        node.percent_impedance = dto.percent_impedance
        node.mains_type_id = dto.mains_type_id
        node.phase_configuration_id = dto.phase_configuration_id

        node.ampere_rating = dto.ampere_rating
        node.pole_count = dto.pole_count
        node.manufacturer_id = dto.manufacturer_id
        node.has_trip_unit = dto.has_trip_unit
        node.trip_type_id = dto.trip_type_id
        node.frame_amps = dto.frame_amps
        node.sensor_amps = dto.sensor_amps
        node.plug_amps = dto.plug_amps

        node.length = dto.length
        node.conductor_material = dto.conductor_material
        node.cable_size_id = dto.cable_size_id
        node.conductor_configuration_id = dto.conductor_configuration_id
        node.duct_material_id = dto.duct_material_id
        node.conductor_description_id = dto.conductor_description_id
        node.insulation_class_id = dto.insulation_class_id
        node.insulation_type_id = dto.insulation_type_id
        node.installation_id = dto.installation_id
        node.busway_ampere_rating = dto.busway_ampere_rating

        node.replacement_cost = dto.replacement_cost
        node.panel_schedule_status = dto.panel_schedule_status
        node.rotation = dto.rotation
        node.locked = dto.locked

        context.insert(node)

        try syncNodeCoreAttributes(
            node: node,
            incoming: dto.core_attributes,
            context: context
        )

        try syncNodeTerminals(
            node: node,
            incoming: dto.node_terminals,
            context: context
        )

        return node
    }
    
    
    private func syncNodeCoreAttributes(
        node: NodeV2,
        incoming: [NodePropertyDTO],
        context: ModelContext
    ) throws {
        
        guard !incoming.isEmpty else {
            node.core_attributes.forEach { context.delete($0) }
            node.core_attributes.removeAll()
            return
        }
        
        let existingById = Dictionary(
            uniqueKeysWithValues: node.core_attributes.map { ($0.id, $0) }
        )
        
        var seenIds = Set<UUID>()
        
        for dto in incoming {
            seenIds.insert(dto.id)

            let property = try fetchNodeClassProperty(
                dto.node_class_property,
                context: context
            )

            let value = dto.value ?? ""

            if let existing = existingById[dto.id] {
                existing.name = dto.name
                existing.value = value
                // Assign unconditionally: if the class property is missing now,
                // overwrite any stale link with nil rather than leaving a
                // potentially dangling SwiftData reference behind (ZP-2366).
                existing.node_class_property = property
            } else if let property {
                let attr = NodeProperty(
                    id: dto.id,
                    node_class_property: property,
                    name: dto.name,
                    value: value
                )
                context.insert(attr)
                node.core_attributes.append(attr)
            }
        }
        
        let staleAttributes = node.core_attributes.filter {
            !seenIds.contains($0.id)
        }
        
        for attr in staleAttributes {
            node.core_attributes.removeAll { $0.id == attr.id }
            context.delete(attr)
        }
    }
    
    /// Sync node terminals from incoming DTOs
    private func syncNodeTerminals(
        node: NodeV2,
        incoming: [NodeTerminalDTO],
        context: ModelContext
    ) throws {
        
        guard !incoming.isEmpty else {
            node.node_terminals.forEach { context.delete($0) }
            node.node_terminals.removeAll()
            return
        }
        
        let existingById = Dictionary(
            uniqueKeysWithValues: node.node_terminals.map { ($0.id, $0) }
        )
        
        var seenIds = Set<UUID>()
        
        for dto in incoming {
            seenIds.insert(dto.id)
            
            if let existing = existingById[dto.id] {
                // Update existing terminal
                existing.update(from: dto)
            } else {
                // Create new terminal
                let terminal = NodeTerminal(from: dto, node: node)
                terminal.lastSyncedAt = Date()
                context.insert(terminal)
                node.node_terminals.append(terminal)
            }
        }
        
        // Remove terminals no longer in the incoming data
        let staleTerminals = node.node_terminals.filter {
            !seenIds.contains($0.id)
        }
        
        for terminal in staleTerminals {
            node.node_terminals.removeAll { $0.id == terminal.id }
            context.delete(terminal)
        }
    }
    
    // MARK: - MAIN UPSERT HELPER: Edge Processing Function
    @MainActor
    private func processEdges(
        _ edgeDTOs: [SLDDTOEdge],
        sld: SLDV2,
        edgeLookup: inout [UUID: EdgeV2],
        context: ModelContext
    ) async throws {
        
        let totalEdges = edgeDTOs.count
        AppLogger.log(.info, "Processing \(totalEdges) edges...", category: .sync)
        
        for (index, dto) in edgeDTOs.enumerated() {
            // Update progress periodically and yield to allow UI updates
            if shouldUpdateProgress(index: index, total: totalEdges, step: .edges) {
                await Task.yield()
            }
            
            let edgeClass = try fetchEdgeClass(dto.edge_class, context: context)
            
            if let edge = edgeLookup[dto.id] {
                // DEBUG: Track is_deleted values during upsert
                AppLogger.log(.debug, "EDGE UPSERT DEBUG: edge \(dto.id) found in lookup", category: .sync)
                AppLogger.log(.info, "dto.is_deleted = \(dto.is_deleted)", category: .sync)
                AppLogger.log(.info, "local.is_deleted BEFORE update = \(edge.is_deleted)", category: .sync)
                try updateEdge(edge, from: dto, sld: sld, edgeClass: edgeClass, context: context)
                AppLogger.log(.info, "local.is_deleted AFTER update = \(edge.is_deleted)", category: .sync)
            } else {
                AppLogger.log(.debug, "EDGE UPSERT DEBUG: edge \(dto.id) NOT in lookup, creating new", category: .sync)
                AppLogger.log(.info, "dto.is_deleted = \(dto.is_deleted)", category: .sync)
                let edge = try createEdge(from: dto, sld: sld, edgeClass: edgeClass, context: context)
                edgeLookup[dto.id] = edge
                AppLogger.log(.info, "new edge.is_deleted = \(edge.is_deleted)", category: .sync)
            }
        }
    }
    
    @MainActor
    private func shouldUpdateProgress(index: Int, total: Int, step: SyncStep) -> Bool {
        guard index % progressUpdateInterval == 0 || index == total - 1 else { return false }
        
        // Update the appropriate progress counter based on step type
        switch step {
        case .nodes:
            progressDetails.processedNodes = index + 1
        case .edges:
            progressDetails.processedEdges = index + 1
        case .photos:
            progressDetails.processedPhotos = index + 1
        case .irPhotos:
            progressDetails.processedIRPhotos = index + 1
        case .sessions:
            progressDetails.processedIRSessions = index + 1
        case .tasks:
            progressDetails.processedTasks = index + 1
        case .quotes:
            progressDetails.processedQuotes = index + 1
        case .issues:
            progressDetails.processedIssues = index + 1
        case .comments:
            progressDetails.processedComments = index + 1
        case .mappings:
            progressDetails.processedMappings = index + 1
        case .locations:
            progressDetails.processedLocations = index + 1
        case .formInstances:
            progressDetails.processedFormInstances = index + 1
        default:
            break
        }
        
        updateStep(step, detail: "\(index + 1) of \(total)")
        return true
    }
    
    private func fetchEdgeClass(
        _ id: UUID?,
        context: ModelContext
    ) throws -> EdgeClass? {
        guard let id else { return nil }
        
        let desc = FetchDescriptor<EdgeClass>(
            predicate: #Predicate { $0.id == id }
        )
        return try context.fetch(desc).first
    }
    
    private func fetchEdgeClassProperty(
        _ idString: String,
        context: ModelContext
    ) throws -> EdgeClassProperty? {
        guard let uuid = UUID(uuidString: idString) else { return nil }
        
        let desc = FetchDescriptor<EdgeClassProperty>(
            predicate: #Predicate { $0.id == uuid }
        )
        return try context.fetch(desc).first
    }
    
    private func updateEdge(
        _ edge: EdgeV2,
        from dto: SLDDTOEdge,
        sld: SLDV2,
        edgeClass: EdgeClass?,
        context: ModelContext
    ) throws {
        
        edge.source = dto.source
        edge.target = dto.target
        edge.sld = sld
        edge.is_deleted = dto.is_deleted
        edge.sourceHandle = dto.source_handle
        edge.targetHandle = dto.target_handle
        edge.sourceNodeTerminalId = dto.source_node_terminal_id
        edge.targetNodeTerminalId = dto.target_node_terminal_id
        edge.edge_class = edgeClass
        edge.points = dto.points
        edge.algorithm = dto.algorithm
        
        try syncCoreAttributes(
            edge: edge,
            incoming: dto.core_attributes ?? [],
            context: context
        )
    }
    
    private func createEdge(
        from dto: SLDDTOEdge,
        sld: SLDV2,
        edgeClass: EdgeClass?,
        context: ModelContext
    ) throws -> EdgeV2 {
        
        let edge = EdgeV2(
            id: dto.id,
            source: dto.source,
            target: dto.target,
            sld: sld,
            is_deleted: dto.is_deleted,
            sourceHandle: dto.source_handle,
            targetHandle: dto.target_handle,
            sourceNodeTerminalId: dto.source_node_terminal_id,
            targetNodeTerminalId: dto.target_node_terminal_id,
            edge_class: edgeClass,
            core_attributes: [],
            points: dto.points,
            algorithm: dto.algorithm
        )
        
        context.insert(edge)
        
        try syncCoreAttributes(
            edge: edge,
            incoming: dto.core_attributes ?? [],
            context: context
        )
        
        return edge
    }
    
    private func syncCoreAttributes(
        edge: EdgeV2,
        incoming: [EdgePropertyDTO],
        context: ModelContext
    ) throws {
        
        // Clear all if incoming is empty
        guard !incoming.isEmpty else {
            edge.core_attributes.forEach { context.delete($0) }
            edge.core_attributes.removeAll()
            return
        }
        
        let existingById = Dictionary(
            uniqueKeysWithValues: edge.core_attributes.map { ($0.id, $0) }
        )
        
        var seenIds = Set<UUID>()
        
        for dto in incoming {
            seenIds.insert(dto.id)

            let property = try fetchEdgeClassProperty(dto.edge_class_property, context: context)

            let value = dto.value ?? ""

            if let existing = existingById[dto.id] {
                existing.name = dto.name
                existing.value = value
                // Always reassign so any pre-existing dangling reference gets
                // cleared instead of preserved (ZP-2366).
                existing.edge_class_property = property
            } else if let property {
                let attr = EdgeProperty(
                    id: dto.id,
                    edge_class_property: property,
                    name: dto.name,
                    value: value
                )
                context.insert(attr)
                edge.core_attributes.append(attr)
            }
        }
        
        // Remove stale attributes
        let staleAttributes = edge.core_attributes.filter { !seenIds.contains($0.id) }
        for attr in staleAttributes {
            edge.core_attributes.removeAll { $0.id == attr.id }
            context.delete(attr)
        }
    }
    
    // MARK: - MAIN UPSERT HELPER: Core Data Processing Function
    @MainActor
    private func processCoreData(
        _ sldDTO: SLDDTO,
        roomLookup: [UUID: Room],
        context: ModelContext
    ) async throws -> (
        sldLookup: [UUID: SLDV2],
        nodeLookup: [UUID: NodeV2],
        edgeLookup: [UUID: EdgeV2],
        pendingDTOs: PendingDTOs
    ) {
        AppLogger.log(.debug, "\nStep 2: Processing SLDs, Nodes, and Edges...", category: .sync)
        updateStep(.nodes, detail: "Loading existing data...")
        
        let existingSLDs = try context.fetch(FetchDescriptor<SLDV2>())
        let existingNodes = try context.fetch(FetchDescriptor<NodeV2>())
        let existingEdges = try context.fetch(FetchDescriptor<EdgeV2>())
        
        AppLogger.log(.info, "Found \(existingNodes.count) existing nodes in SwiftData", category: .sync)
        
        // Check for duplicates BEFORE creating dictionary
        let nodesByID = Dictionary(grouping: existingNodes, by: { $0.id })
        let duplicates = nodesByID.filter { $0.value.count > 1 }
        
        var nodeLookup: [UUID: NodeV2]
        if !duplicates.isEmpty {
            AppLogger.log(.notice, "DUPLICATE NODES DETECTED:", category: .sync)
            for (id, nodes) in duplicates {
                AppLogger.log(.info, "Node ID: \(id)", category: .sync)
                for (index, node) in nodes.enumerated() {
                    AppLogger.log(.info, "[\(index)] Label: '\(node.label)', lastModified: \(node.lastModifiedAt), lastSynced: \(node.lastSyncedAt?.description ?? "nil"), needsSync: \(node.needsSync)", category: .sync)
                }
            }
            
            // Handle duplicates by keeping the most recently synced one
            var cleanedNodes: [NodeV2] = []
            for (_, nodes) in nodesByID {
                if nodes.count > 1 {
                    // Multiple nodes with same ID - keep the best one
                    let sortedNodes = nodes.sorted { node1, node2 in
                        // Prioritize synced nodes over unsynced
                        if let sync1 = node1.lastSyncedAt, let sync2 = node2.lastSyncedAt {
                            return sync1 > sync2
                        } else if node1.lastSyncedAt != nil {
                            return true
                        } else if node2.lastSyncedAt != nil {
                            return false
                        } else {
                            return node1.lastModifiedAt > node2.lastModifiedAt
                        }
                    }
                    
                    let keepNode = sortedNodes.first!
                    cleanedNodes.append(keepNode)
                    AppLogger.log(.info, "Keeping node: '\(keepNode.label)' (lastSynced: \(keepNode.lastSyncedAt?.description ?? "nil"))", category: .sync)
                    
                    // Delete the rest
                    for nodeToDelete in sortedNodes.dropFirst() {
                        AppLogger.log(.info, "Deleting duplicate: '\(nodeToDelete.label)' (lastSynced: \(nodeToDelete.lastSyncedAt?.description ?? "nil"))", category: .sync)
                        context.delete(nodeToDelete)
                    }
                } else {
                    cleanedNodes.append(nodes.first!)
                }
            }
            
            AppLogger.log(.info, "Cleaned nodes: \(existingNodes.count) → \(cleanedNodes.count)", category: .sync)
            nodeLookup = Dictionary(uniqueKeysWithValues: cleanedNodes.map { ($0.id, $0) })
        } else {
            nodeLookup = Dictionary(uniqueKeysWithValues: existingNodes.map { ($0.id, $0) })
        }
        
        var sldLookup = Dictionary(uniqueKeysWithValues: existingSLDs.map { ($0.id, $0) })
        var edgeLookup = Dictionary(uniqueKeysWithValues: existingEdges.map { ($0.id, $0) })
        
        // Initialize pending DTOs
        var pendingDTOs = PendingDTOs()
        
        // Process SLD
        let sld = processSLD(sldDTO, existingSLDs: existingSLDs, sldLookup: &sldLookup, context: context)
        
        // Process Nodes
        try await processNodes(sldDTO.nodes, sld: sld, nodeLookup: &nodeLookup, roomLookup: roomLookup, context: context)
        
        // After processing nodes, populate the SLD's nodes array
        // Get all active nodes from the nodeLookup that belong to this SLD
        let nodesForSLD = nodeLookup.values.filter { node in
            node.sld?.id == sld.id && !node.is_deleted
        }
        sld.nodes = Array(nodesForSLD)
        AppLogger.log(.info, "Added \(nodesForSLD.count) nodes to SLD.nodes array", category: .sync)
        
        // Update progress before edges
        updateStep(.edges, detail: "0 of \(sldDTO.edges.count)")
        
        // Process Edges
        try await processEdges(sldDTO.edges, sld: sld, edgeLookup: &edgeLookup, context: context)
        
        // After processing edges, populate the SLD's edges array
        let edgesForSLD = edgeLookup.values.filter { edge in
            edge.sld?.id == sld.id && !edge.is_deleted
        }
        sld.edges = Array(edgesForSLD)
        AppLogger.log(.info, "Added \(edgesForSLD.count) edges to SLD.edges array", category: .sync)
        
        // Store various DTOs for later processing
        for photoDTO in sldDTO.photos {
            pendingDTOs.photos.append((photoDTO, sld.id))
        }
        
        for irPhotoDTO in sldDTO.ir_photos {
            pendingDTOs.irPhotos.append((irPhotoDTO, sld.id))
        }
        
        for irSessionDTO in sldDTO.ir_sessions {
            pendingDTOs.irSessions.append((irSessionDTO, sld.id))
        }
        
        // NEW: Store tasks, quotes, issues, comments from SLD
        AppLogger.log(.info, "Storing for later processing:", category: .sync)
        AppLogger.log(.info, "\(sldDTO.tasks.count) tasks", category: .sync)
        AppLogger.log(.info, "\(sldDTO.quotes.count) quotes", category: .sync)
        AppLogger.log(.info, "\(sldDTO.issues.count) issues", category: .sync)
        AppLogger.log(.info, "\(sldDTO.comments.count) comments", category: .sync)
        
        for taskDTO in sldDTO.tasks {
            pendingDTOs.tasks.append((taskDTO, sld.id))
        }
        
        for quoteDTO in sldDTO.quotes {
            pendingDTOs.quotes.append((quoteDTO, sld.id))
        }
        
        for issueDTO in sldDTO.issues {
            pendingDTOs.issues.append((issueDTO, sld.id))
        }
        
        for commentDTO in sldDTO.comments {
            pendingDTOs.comments.append((commentDTO, sld.id))
        }
        
        // NEW: Store mappings
        if let mappings = sldDTO.mappings {
            AppLogger.log(.info, "Storing mappings:", category: .sync)
            AppLogger.log(.info, "\(mappings.issueTaskMappings.count) issue-task mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.taskSessionMappings.count) task-session mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.quoteTaskMappings.count) quote-task mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.userTaskMappings.count) user-task mappings", category: .sync)
            
            // Store the full DTOs including is_deleted flag
            pendingDTOs.mappings.issueTask.append(contentsOf: mappings.issueTaskMappings)
            pendingDTOs.mappings.taskSession.append(contentsOf: mappings.taskSessionMappings)
            pendingDTOs.mappings.quoteTask.append(contentsOf: mappings.quoteTaskMappings)
            pendingDTOs.mappings.userTask.append(contentsOf: mappings.userTaskMappings)
            pendingDTOs.mappings.taskNode.append(contentsOf: mappings.taskNodeMappings)
            pendingDTOs.mappings.taskForm.append(contentsOf: mappings.taskFormMappings)
            pendingDTOs.mappings.taskFormInstance.append(contentsOf: mappings.taskFormInstanceMappings)
            pendingDTOs.mappings.formInstanceNode.append(contentsOf: mappings.formInstanceNodeMappings)
            pendingDTOs.mappings.taskEGFormInstance.append(contentsOf: mappings.taskEGFormInstanceMappings)
            pendingDTOs.mappings.egFormInstanceNode.append(contentsOf: mappings.egFormInstanceNodeMappings)
            pendingDTOs.mappings.nodeSession.append(contentsOf: mappings.nodeSessionMappings)
            pendingDTOs.mappings.userSession.append(contentsOf: mappings.userSessionMappings)
            pendingDTOs.mappings.attachmentNode.append(contentsOf: mappings.attachmentNodeMappings)

            AppLogger.log(.info, "\(mappings.taskFormInstanceMappings.count) task-form-instance mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.formInstanceNodeMappings.count) form-instance-node mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.taskEGFormInstanceMappings.count) task-eg-form-instance mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.egFormInstanceNodeMappings.count) eg-form-instance-node mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.nodeSessionMappings.count) node-session mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.userSessionMappings.count) user-session mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.attachmentNodeMappings.count) attachment-node mappings", category: .sync)
        }

        // Store attachments
        if let attachments = sldDTO.attachments {
            AppLogger.log(.info, "📎 Storing \(attachments.count) attachments", category: .sync)
            for attachmentDTO in attachments {
                pendingDTOs.attachments.append((attachmentDTO, sld.id))
            }
        }

        // Store form instances
        if let formInstances = sldDTO.form_instances {
            AppLogger.log(.info, "📄 Storing \(formInstances.count) form instances", category: .sync)
            pendingDTOs.formInstances.append(contentsOf: formInstances)
        }

        // ZP-1723: EG form definitions + instances. Definitions ride
        // alongside instances so the renderer boots offline.
        if let egForms = sldDTO.eg_forms {
            AppLogger.log(.info, "📝 Storing \(egForms.count) EG forms", category: .sync)
            pendingDTOs.egForms.append(contentsOf: egForms)
        }
        if let egInstances = sldDTO.eg_form_instances {
            AppLogger.log(.info, "📝 Storing \(egInstances.count) EG form instances", category: .sync)
            pendingDTOs.egFormInstances.append(contentsOf: egInstances)
        }
        
        // Store location hierarchy data
        if let buildings = sldDTO.buildings {
            AppLogger.log(.info, "🏢 Storing \(buildings.count) buildings", category: .sync)
            for buildingDTO in buildings {
                pendingDTOs.buildings.append((buildingDTO, sld.id))
            }
        }
        
        if let floors = sldDTO.floors {
            AppLogger.log(.info, "🏗️ Storing \(floors.count) floors", category: .sync)
            for floorDTO in floors {
                pendingDTOs.floors.append((floorDTO, sld.id))
            }
        }
        
        if let rooms = sldDTO.rooms {
            AppLogger.log(.info, "🚪 Storing \(rooms.count) rooms", category: .sync)
            for roomDTO in rooms {
                pendingDTOs.rooms.append((roomDTO, sld.id))
            }
        }
        
        // Store SLD Views
        if let sldViews = sldDTO.sld_views {
            AppLogger.log(.info, "🗺️ Storing \(sldViews.count) SLD views", category: .sync)
            for viewDTO in sldViews {
                pendingDTOs.sldViews.append((viewDTO, sld.id))
            }
        }
        
        // Store SLD Links
        if let sldLinks = sldDTO.sld_links {
            AppLogger.log(.info, "Storing \(sldLinks.count) SLD links", category: .sync)
            for linkDTO in sldLinks {
                pendingDTOs.sldLinks.append((linkDTO, sld.id))
            }
        }
        
        // Store node-view and edge-view mappings (already stored in pendingDTOs.mappings above)
        AppLogger.log(.debug, "[VIEW DEBUG] sldDTO.mappings is \(sldDTO.mappings == nil ? "nil" : "present")", category: .sync)
        if let mappings = sldDTO.mappings {
            AppLogger.log(.debug, "[VIEW DEBUG] Raw node_sld_view: \(mappings.node_sld_view?.count ?? -1), computed: \(mappings.nodeSLDViewMappings.count)", category: .sync)
            AppLogger.log(.debug, "[VIEW DEBUG] Raw edge_sld_view: \(mappings.edge_sld_view?.count ?? -1), computed: \(mappings.edgeSLDViewMappings.count)", category: .sync)
            pendingDTOs.mappings.nodeSLDView.append(contentsOf: mappings.nodeSLDViewMappings)
            pendingDTOs.mappings.edgeSLDView.append(contentsOf: mappings.edgeSLDViewMappings)
            AppLogger.log(.info, "\(mappings.nodeSLDViewMappings.count) node-view mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.edgeSLDViewMappings.count) edge-view mappings", category: .sync)
        } else {
            AppLogger.log(.error, "[VIEW DEBUG] sldDTO.mappings is nil - no mappings to process!", category: .sync)
        }
        
        return (sldLookup, nodeLookup, edgeLookup, pendingDTOs)
    }
    
    // MARK: - Batch Import Version for Large Datasets
    
    /// Process core data (SLD, Nodes, Edges) using BackgroundImporter for better memory efficiency.
    /// This method uses batch processing with intermediate saves to reduce memory pressure.
    private func processCoreDataWithBatchImport(
        _ sldDTO: SLDDTO,
        roomLookup: [UUID: Room],
        context: ModelContext
    ) async throws -> (
        sldLookup: [UUID: SLDV2],
        nodeLookup: [UUID: NodeV2],
        edgeLookup: [UUID: EdgeV2],
        pendingDTOs: PendingDTOs
    ) {
        AppLogger.log(.debug, "\nStep 2: Processing SLDs, Nodes, and Edges (BATCH MODE)...", category: .sync)
        
        guard let modelContainer = AppStateManager.shared.modelContainer else {
            AppLogger.log(.notice, "ModelContainer not available, falling back to standard processing", category: .sync)
            return try await processCoreData(sldDTO, roomLookup: roomLookup, context: context)
        }
        
        // Process SLD on main context first
        let existingSLDs = try context.fetch(FetchDescriptor<SLDV2>())
        var sldLookup = Dictionary(uniqueKeysWithValues: existingSLDs.map { ($0.id, $0) })
        let sld = processSLD(sldDTO, existingSLDs: existingSLDs, sldLookup: &sldLookup, context: context)
        
        // Save SLD to ensure it's available for background importer
        try context.save()
        
        // Create background importer
        let importer = BackgroundImporter(modelContainer: modelContainer, batchSize: 1000)
        
        // Import nodes in background batches
        updateStep(.nodes, detail: "0 of \(sldDTO.nodes.count)")
        AppLogger.log(.info, "Starting batch node import: \(sldDTO.nodes.count) nodes", category: .sync)
        
        let nodeIds = try await importer.importNodes(sldDTO.nodes, sldId: sld.id) { processed, total in
            await MainActor.run {
                self.progressDetails.processedNodes = processed
                self.updateStep(.nodes, detail: "\(processed) of \(total)")
            }
        }
        AppLogger.log(.info, "Batch node import complete: \(nodeIds.count) nodes processed", category: .sync)
        
        // Import edges in background batches
        updateStep(.edges, detail: "0 of \(sldDTO.edges.count)")
        AppLogger.log(.info, "Starting batch edge import: \(sldDTO.edges.count) edges", category: .sync)
        
        let edgeIds = try await importer.importEdges(sldDTO.edges, sldId: sld.id) { processed, total in
            await MainActor.run {
                self.progressDetails.processedEdges = processed
                self.updateStep(.edges, detail: "\(processed) of \(total)")
            }
        }
        AppLogger.log(.info, "Batch edge import complete: \(edgeIds.count) edges processed", category: .sync)
        
        // Refresh main context to see background changes
        context.processPendingChanges()
        
        // Rebuild lookups from refreshed context
        let existingNodes = try context.fetch(FetchDescriptor<NodeV2>())
        let nodeLookup = Dictionary(uniqueKeysWithValues: existingNodes.map { ($0.id, $0) })
        AppLogger.log(.info, "Refreshed context has \(existingNodes.count) nodes", category: .sync)
        
        let existingEdges = try context.fetch(FetchDescriptor<EdgeV2>())
        let edgeLookup = Dictionary(uniqueKeysWithValues: existingEdges.map { ($0.id, $0) })
        AppLogger.log(.info, "Refreshed context has \(existingEdges.count) edges", category: .sync)
        
        // Populate the SLD's nodes and edges arrays
        let nodesForSLD = nodeLookup.values.filter { $0.sld?.id == sld.id && !$0.is_deleted }
        sld.nodes = Array(nodesForSLD)
        AppLogger.log(.info, "Added \(nodesForSLD.count) nodes to SLD.nodes array", category: .sync)
        
        let edgesForSLD = edgeLookup.values.filter { $0.sld?.id == sld.id && !$0.is_deleted }
        sld.edges = Array(edgesForSLD)
        AppLogger.log(.info, "Added \(edgesForSLD.count) edges to SLD.edges array", category: .sync)
        
        // Initialize pending DTOs for other entity types
        var pendingDTOs = PendingDTOs()
        
        // Store various DTOs for later processing
        for photoDTO in sldDTO.photos {
            pendingDTOs.photos.append((photoDTO, sld.id))
        }
        
        for irPhotoDTO in sldDTO.ir_photos {
            pendingDTOs.irPhotos.append((irPhotoDTO, sld.id))
        }
        
        for irSessionDTO in sldDTO.ir_sessions {
            pendingDTOs.irSessions.append((irSessionDTO, sld.id))
        }
        
        // Store tasks, quotes, issues, comments from SLD
        AppLogger.log(.info, "Storing for later processing:", category: .sync)
        AppLogger.log(.info, "\(sldDTO.tasks.count) tasks", category: .sync)
        AppLogger.log(.info, "\(sldDTO.quotes.count) quotes", category: .sync)
        AppLogger.log(.info, "\(sldDTO.issues.count) issues", category: .sync)
        AppLogger.log(.info, "\(sldDTO.comments.count) comments", category: .sync)
        
        for taskDTO in sldDTO.tasks {
            pendingDTOs.tasks.append((taskDTO, sld.id))
        }
        
        for quoteDTO in sldDTO.quotes {
            pendingDTOs.quotes.append((quoteDTO, sld.id))
        }
        
        for issueDTO in sldDTO.issues {
            pendingDTOs.issues.append((issueDTO, sld.id))
        }
        
        for commentDTO in sldDTO.comments {
            pendingDTOs.comments.append((commentDTO, sld.id))
        }
        
        // Store mappings
        if let mappings = sldDTO.mappings {
            AppLogger.log(.info, "Storing mappings:", category: .sync)
            AppLogger.log(.info, "\(mappings.issueTaskMappings.count) issue-task mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.taskSessionMappings.count) task-session mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.quoteTaskMappings.count) quote-task mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.userTaskMappings.count) user-task mappings", category: .sync)
            
            pendingDTOs.mappings.issueTask.append(contentsOf: mappings.issueTaskMappings)
            pendingDTOs.mappings.taskSession.append(contentsOf: mappings.taskSessionMappings)
            pendingDTOs.mappings.quoteTask.append(contentsOf: mappings.quoteTaskMappings)
            pendingDTOs.mappings.userTask.append(contentsOf: mappings.userTaskMappings)
            pendingDTOs.mappings.taskNode.append(contentsOf: mappings.taskNodeMappings)
            pendingDTOs.mappings.taskForm.append(contentsOf: mappings.taskFormMappings)
            pendingDTOs.mappings.taskFormInstance.append(contentsOf: mappings.taskFormInstanceMappings)
            pendingDTOs.mappings.formInstanceNode.append(contentsOf: mappings.formInstanceNodeMappings)
            pendingDTOs.mappings.taskEGFormInstance.append(contentsOf: mappings.taskEGFormInstanceMappings)
            pendingDTOs.mappings.egFormInstanceNode.append(contentsOf: mappings.egFormInstanceNodeMappings)
            pendingDTOs.mappings.nodeSession.append(contentsOf: mappings.nodeSessionMappings)
            pendingDTOs.mappings.userSession.append(contentsOf: mappings.userSessionMappings)
            pendingDTOs.mappings.attachmentNode.append(contentsOf: mappings.attachmentNodeMappings)

            AppLogger.log(.info, "\(mappings.taskFormInstanceMappings.count) task-form-instance mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.formInstanceNodeMappings.count) form-instance-node mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.taskEGFormInstanceMappings.count) task-eg-form-instance mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.egFormInstanceNodeMappings.count) eg-form-instance-node mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.nodeSessionMappings.count) node-session mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.userSessionMappings.count) user-session mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.attachmentNodeMappings.count) attachment-node mappings", category: .sync)
        }

        // Store attachments
        if let attachments = sldDTO.attachments {
            AppLogger.log(.info, "📎 Storing \(attachments.count) attachments", category: .sync)
            for attachmentDTO in attachments {
                pendingDTOs.attachments.append((attachmentDTO, sld.id))
            }
        }

        // Store form instances
        if let formInstances = sldDTO.form_instances {
            AppLogger.log(.info, "📄 Storing \(formInstances.count) form instances", category: .sync)
            pendingDTOs.formInstances.append(contentsOf: formInstances)
        }

        // ZP-1723: EG form definitions + instances. Definitions ride
        // alongside instances so the renderer boots offline.
        if let egForms = sldDTO.eg_forms {
            AppLogger.log(.info, "📝 Storing \(egForms.count) EG forms", category: .sync)
            pendingDTOs.egForms.append(contentsOf: egForms)
        }
        if let egInstances = sldDTO.eg_form_instances {
            AppLogger.log(.info, "📝 Storing \(egInstances.count) EG form instances", category: .sync)
            pendingDTOs.egFormInstances.append(contentsOf: egInstances)
        }
        
        // Store location hierarchy data
        if let buildings = sldDTO.buildings {
            AppLogger.log(.info, "🏢 Storing \(buildings.count) buildings", category: .sync)
            for buildingDTO in buildings {
                pendingDTOs.buildings.append((buildingDTO, sld.id))
            }
        }
        
        if let floors = sldDTO.floors {
            AppLogger.log(.info, "🏗️ Storing \(floors.count) floors", category: .sync)
            for floorDTO in floors {
                pendingDTOs.floors.append((floorDTO, sld.id))
            }
        }
        
        if let rooms = sldDTO.rooms {
            AppLogger.log(.info, "🚪 Storing \(rooms.count) rooms", category: .sync)
            for roomDTO in rooms {
                pendingDTOs.rooms.append((roomDTO, sld.id))
            }
        }
        
        // Store SLD Views
        if let sldViews = sldDTO.sld_views {
            AppLogger.log(.info, "🗺️ Storing \(sldViews.count) SLD views", category: .sync)
            for viewDTO in sldViews {
                pendingDTOs.sldViews.append((viewDTO, sld.id))
            }
        }
        
        // Store SLD Links
        if let sldLinks = sldDTO.sld_links {
            AppLogger.log(.info, "Storing \(sldLinks.count) SLD links", category: .sync)
            for linkDTO in sldLinks {
                pendingDTOs.sldLinks.append((linkDTO, sld.id))
            }
        }
        
        // Store node-view and edge-view mappings
        AppLogger.log(.debug, "[VIEW DEBUG] (batch) sldDTO.mappings is \(sldDTO.mappings == nil ? "nil" : "present")", category: .sync)
        if let mappings = sldDTO.mappings {
            AppLogger.log(.debug, "[VIEW DEBUG] (batch) Raw node_sld_view: \(mappings.node_sld_view?.count ?? -1), computed: \(mappings.nodeSLDViewMappings.count)", category: .sync)
            AppLogger.log(.debug, "[VIEW DEBUG] (batch) Raw edge_sld_view: \(mappings.edge_sld_view?.count ?? -1), computed: \(mappings.edgeSLDViewMappings.count)", category: .sync)
            pendingDTOs.mappings.nodeSLDView.append(contentsOf: mappings.nodeSLDViewMappings)
            pendingDTOs.mappings.edgeSLDView.append(contentsOf: mappings.edgeSLDViewMappings)
            AppLogger.log(.info, "\(mappings.nodeSLDViewMappings.count) node-view mappings", category: .sync)
            AppLogger.log(.info, "\(mappings.edgeSLDViewMappings.count) edge-view mappings", category: .sync)
        } else {
            AppLogger.log(.error, "[VIEW DEBUG] (batch) sldDTO.mappings is nil - no mappings to process!", category: .sync)
        }
        
        return (sldLookup, nodeLookup, edgeLookup, pendingDTOs)
    }
    
    // MARK: - MAIN UPSERT HELPER: IR Session Processing Function
    @MainActor
    private func processSessions(
        _ pendingIRSessionDTOs: [(sessionDTO: IRSessionDTO, sldId: UUID)],
        sldLookup: [UUID: SLDV2],
        irSessionLookup: inout [UUID: IRSession],
        context: ModelContext
    ) throws {
        AppLogger.log(.debug, "\nStep 3: Processing \(pendingIRSessionDTOs.count) IR Sessions...", category: .sync)
        let existingIRSessions = try context.fetch(FetchDescriptor<IRSession>())
        irSessionLookup = Dictionary(uniqueKeysWithValues: existingIRSessions.map { ($0.id, $0) })
        
        // Fetch existing work blocks for lookup
        let existingWorkBlocks = try context.fetch(FetchDescriptor<SessionWorkBlock>())
        var workBlockLookup = Dictionary(uniqueKeysWithValues: existingWorkBlocks.map { ($0.id, $0) })
        
        for (sessionDTO, sldId) in pendingIRSessionDTOs {
            guard let sld = sldLookup[sldId] else {
                // print("IR Session \(sessionDTO.id) references non-existent SLD \(sldId)")
                continue
            }
            
            let session: IRSession
            
            if let existingSession = irSessionLookup[sessionDTO.id] {
                // Update existing session
                existingSession.name = sessionDTO.name
                existingSession.sessionDescription = sessionDTO.description
                existingSession.photo_type = sessionDTO.photo_type
                existingSession.active_visual_prefix = sessionDTO.active_visual_prefix
                existingSession.active_ir_prefix = sessionDTO.active_ir_prefix
                existingSession.date_created = sessionDTO.date_created
                existingSession.date_closed = sessionDTO.date_closed
                existingSession.active = sessionDTO.active
                existingSession.is_deleted = sessionDTO.is_deleted
                existingSession.equipmentIds = sessionDTO.equipmentIdUUIDs
                existingSession.sld = sld
                session = existingSession
                // print(" Updated IR Session: \(sessionDTO.name)")
            } else {
                // Create new session
                let newSession = IRSession(
                    id: sessionDTO.id,
                    name: sessionDTO.name,
                    sessionDescription: sessionDTO.description,
                    photo_type: sessionDTO.photo_type,
                    active_visual_prefix: sessionDTO.active_visual_prefix,
                    active_ir_prefix: sessionDTO.active_ir_prefix,
                    date_created: sessionDTO.date_created,
                    date_closed: sessionDTO.date_closed,
                    sld: sld,
                    active: sessionDTO.active,
                    is_deleted: sessionDTO.is_deleted,
                    equipmentIds: sessionDTO.equipmentIdUUIDs,
                    user_tasks: [],
                    issues: []
                )
                context.insert(newSession)
                irSessionLookup[sessionDTO.id] = newSession
                session = newSession
                // print(" ✨ Created IR Session: \(sessionDTO.name)")
            }
            
            // Process work blocks for this session
            if let workBlockDTOs = sessionDTO.work_blocks {
                for blockDTO in workBlockDTOs {
                    guard let blockId = UUID(uuidString: blockDTO.id) else { continue }
                    
                    // Parse dates from ISO8601 strings
                    let dateFormatter = ISO8601DateFormatter()
                    dateFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                    
                    // Try with fractional seconds first, then without
                    let startTime = dateFormatter.date(from: blockDTO.start_time) ?? {
                        dateFormatter.formatOptions = [.withInternetDateTime]
                        return dateFormatter.date(from: blockDTO.start_time)
                    }()
                    
                    let endTime = dateFormatter.date(from: blockDTO.end_time) ?? {
                        dateFormatter.formatOptions = [.withInternetDateTime]
                        return dateFormatter.date(from: blockDTO.end_time)
                    }()
                    
                    guard let start = startTime, let end = endTime else {
                        AppLogger.log(.notice, "Could not parse dates for work block \(blockDTO.id)", category: .sync)
                        continue
                    }
                    
                    if let existingBlock = workBlockLookup[blockId] {
                        // Update existing work block
                        existingBlock.start_time = start
                        existingBlock.end_time = end
                        existingBlock.work_length = blockDTO.work_length
                        existingBlock.total_days = blockDTO.total_days
                        existingBlock.notes = blockDTO.notes
                        existingBlock.is_deleted = blockDTO.is_deleted
                        existingBlock.duration_hours = blockDTO.duration_hours
                        existingBlock.session = session
                    } else {
                        // Create new work block
                        let workBlock = SessionWorkBlock(
                            id: blockId,
                            session: session,
                            start_time: start,
                            end_time: end,
                            work_length: blockDTO.work_length,
                            total_days: blockDTO.total_days,
                            notes: blockDTO.notes,
                            is_deleted: blockDTO.is_deleted,
                            duration_hours: blockDTO.duration_hours
                        )
                        context.insert(workBlock)
                        workBlockLookup[blockId] = workBlock
                        
                        // Add to session's work_blocks array
                        if !session.work_blocks.contains(where: { $0.id == blockId }) {
                            session.work_blocks.append(workBlock)
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - MAIN UPSERT HELPER: UserTask Processing Function
    @MainActor
    private func processUserTasks(
        _ pendingTaskDTOs: [(taskDTO: UserTaskDTO, sldId: UUID)],
        sldLookup: [UUID: SLDV2],
        nodeLookup: [UUID: NodeV2],
        formsByID: [UUID: UserTaskForm],
        tasksByID: inout [UUID: UserTask],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nStep 4: Processing \(pendingTaskDTOs.count) UserTasks...", category: .sync)
        let existingTasks = try context.fetch(FetchDescriptor<UserTask>())
        tasksByID = Dictionary(uniqueKeysWithValues: existingTasks.map { ($0.id, $0) })
        
        for (taskDTO, sldId) in pendingTaskDTOs {
            guard let sld = sldLookup[sldId] else {
                AppLogger.log(.notice, "Task \(taskDTO.id) references non-existent SLD \(sldId)", category: .sync)
                continue
            }
            
            let userTask: UserTask
            
            if let existing = tasksByID[taskDTO.id] {
                // Update existing task
                AppLogger.log(.info, "Updating task: \(taskDTO.title)", category: .sync)
                existing.title = taskDTO.title
                existing.task_description = taskDTO.task_description
                existing.completed = taskDTO.completed
                existing.is_deleted = taskDTO.is_deleted
                existing.submission = taskDTO.submission
                existing.submitted_at = taskDTO.submitted_at
                existing.due_date = taskDTO.due_date
                existing.created_at = taskDTO.created_at
                existing.task_type = taskDTO.task_type
                existing.interval = taskDTO.interval
                existing.recurring = taskDTO.recurring
                existing.procedure_id = taskDTO.procedure_id
                existing.shortcut_id = taskDTO.shortcut_id
                
                // Ensure linkedNodes and linkedForms arrays are initialized
                // (for tasks created before these properties were added)
                if existing.linkedNodes.isEmpty && existing.node == nil {
                    // Only initialize if truly empty to avoid clearing existing links
                    existing.linkedNodes = []
                }
                if existing.linkedForms.isEmpty && existing.form == nil {
                    // Only initialize if truly empty to avoid clearing existing links
                    existing.linkedForms = []
                }
                
                userTask = existing
                existing.lastSyncedAt = Date()

                // Clear existing relationships to rebuild them
                if let oldNode = existing.node {
                    oldNode.node_tasks.removeAll { $0.id == existing.id }
                }
            } else {
                // Create new task
                AppLogger.log(.info, "✨ Creating task: \(taskDTO.title)", category: .sync)
                userTask = UserTask(
                    id: taskDTO.id,
                    title: taskDTO.title,
                    task_description: taskDTO.task_description,
                    completed: taskDTO.completed,
                    form: nil,
                    node: nil,
                    linkedNodes: [], // Initialize empty array
                    linkedForms: [], // Initialize empty array
                    sld: sld,
                    is_deleted: taskDTO.is_deleted,
                    submission: taskDTO.submission,
                    submitted_at: taskDTO.submitted_at,
                    photos: [],
                    due_date: taskDTO.due_date,
                    created_at: taskDTO.created_at,
                    task_type: taskDTO.task_type,
                    interval: taskDTO.interval,
                    recurring: taskDTO.recurring,
                    procedure_id: taskDTO.procedure_id,
                    shortcut_id: taskDTO.shortcut_id
                )
                userTask.lastSyncedAt = Date()
                context.insert(userTask)
                tasksByID[taskDTO.id] = userTask
            }
            
            // Link relationships
            if let formID = taskDTO.form_id {
                userTask.form = formsByID[formID]
                AppLogger.log(.info, "Linked to form: \(userTask.form?.title ?? "Unknown")", category: .sync)
            }
            
            // Link to node if specified
            if let nodeID = taskDTO.node_id {
                if let node = nodeLookup[nodeID] {
                    userTask.node = node
                    if !node.node_tasks.contains(where: { $0.id == userTask.id }) {
                        node.node_tasks.append(userTask)
                    }
                    AppLogger.log(.info, "Linked to node: \(node.label)", category: .sync)
                } else {
                    AppLogger.log(.notice, "Task references non-existent node \(nodeID)", category: .sync)
                }
            }
            
            userTask.sld = sld
        }
    }
    
    // MARK: - MAIN UPSERT HELPER: Form Instance Processing Function
    @MainActor
    private func processFormInstances(
        _ formInstanceDTOs: [FormInstanceDTO],
        formsByID: [UUID: UserTaskForm],
        formInstancesByID: inout [UUID: FormInstance],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nStep 4.5: Processing \(formInstanceDTOs.count) Form Instances...", category: .sync)
        let existingInstances = try context.fetch(FetchDescriptor<FormInstance>())
        formInstancesByID = Dictionary(uniqueKeysWithValues: existingInstances.map { ($0.id, $0) })
        
        for dto in formInstanceDTOs {
            guard let instanceId = UUID(uuidString: dto.id),
                  let formMasterId = UUID(uuidString: dto.form_master_id) else {
                AppLogger.log(.notice, "Invalid UUID in FormInstanceDTO", category: .sync)
                continue
            }
            
            // Find the form master
            let formMaster = formsByID[formMasterId]
            
            if let existing = formInstancesByID[instanceId] {
                // Update existing instance
                existing.form_master_id = formMasterId
                existing.form_submission = dto.formSubmissionString
                existing.submitted = dto.submitted ?? false
                existing.is_deleted = dto.is_deleted ?? false
                existing.formMaster = formMaster
                
                if let modifiedStr = dto.modified_at {
                    let formatter = ISO8601DateFormatter()
                    existing.modified_at = formatter.date(from: modifiedStr)
                }
            } else {
                // Create new instance
                let createdAt: Date
                if let createdStr = dto.created_at {
                    let formatter = ISO8601DateFormatter()
                    createdAt = formatter.date(from: createdStr) ?? Date()
                } else {
                    createdAt = Date()
                }
                
                let instance = FormInstance(
                    id: instanceId,
                    form_master_id: formMasterId,
                    created_at: createdAt,
                    modified_at: nil,
                    form_submission: dto.formSubmissionString,
                    submitted: dto.submitted ?? false,
                    is_deleted: dto.is_deleted ?? false,
                    formMaster: formMaster
                )
                context.insert(instance)
                formInstancesByID[instanceId] = instance
            }
        }
        
        AppLogger.log(.info, "Processed \(formInstanceDTOs.count) form instances", category: .sync)
    }

    // MARK: - MAIN UPSERT HELPER: EG Form + EG Form Instance Processing (ZP-1723)
    /// Upsert EGForm definitions, then EGFormInstance rows, then rebuild
    /// the linkedTasks / linkedNodes relationships from mapping DTOs. The
    /// backend already sanitized base64 out of form_submission for us so
    /// payloads are small.
    @MainActor
    private func processEGFormInstances(
        egForms: [EGFormDTO],
        egFormInstances: [EGFormInstanceDTO],
        taskEGFormInstanceMappings: [MappingTaskEGFormInstanceDTO],
        egFormInstanceNodeMappings: [MappingEGFormInstanceNodeDTO],
        tasksByID: [UUID: UserTask],
        nodeLookup: [UUID: NodeV2],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nStep 4.6: Processing \(egForms.count) EG forms + \(egFormInstances.count) EG instances...", category: .sync)
        let isoFormatter = ISO8601DateFormatter()

        // Upsert EGForm definitions first so instances can reference them.
        var egFormsByID: [UUID: EGForm] = Dictionary(
            uniqueKeysWithValues: (try? context.fetch(FetchDescriptor<EGForm>()))?.map { ($0.id, $0) } ?? []
        )

        for dto in egForms {
            guard let formId = UUID(uuidString: dto.id) else { continue }
            let createdAt = dto.created_at.flatMap { isoFormatter.date(from: $0) } ?? Date()
            let modifiedAt = dto.modified_at.flatMap { isoFormatter.date(from: $0) }
            let companyId = dto.company_id.flatMap { UUID(uuidString: $0) }

            if let existing = egFormsByID[formId] {
                existing.key = dto.key
                existing.title = dto.title
                existing.definition = dto.definitionString
                existing.sample_data = dto.sampleDataString
                existing.html_template = dto.html_template
                existing.references_json = dto.referencesString
                existing.form_template_key = dto.form_template_key
                existing.eg_form_type = dto.eg_form_type
                existing.eg_form_type_name = dto.eg_form_type_name
                existing.eg_form_type_key = dto.eg_form_type_key
                existing.node_class_names = dto.node_class_names ?? []
                existing.is_deleted = dto.is_deleted ?? false
                existing.is_global = dto.is_global ?? false
                existing.is_override = dto.is_override ?? false
                existing.company_id = companyId
                existing.modified_at = modifiedAt
            } else {
                let form = EGForm(
                    id: formId,
                    key: dto.key,
                    title: dto.title,
                    definition: dto.definitionString,
                    sample_data: dto.sampleDataString,
                    html_template: dto.html_template,
                    references_json: dto.referencesString,
                    form_template_key: dto.form_template_key,
                    eg_form_type: dto.eg_form_type,
                    eg_form_type_name: dto.eg_form_type_name,
                    eg_form_type_key: dto.eg_form_type_key,
                    node_class_names: dto.node_class_names ?? [],
                    is_deleted: dto.is_deleted ?? false,
                    is_global: dto.is_global ?? false,
                    is_override: dto.is_override ?? false,
                    company_id: companyId,
                    created_at: createdAt,
                    modified_at: modifiedAt
                )
                context.insert(form)
                egFormsByID[formId] = form
            }
        }

        // Upsert EGFormInstance rows.
        var instancesByID: [UUID: EGFormInstance] = Dictionary(
            uniqueKeysWithValues: (try? context.fetch(FetchDescriptor<EGFormInstance>()))?.map { ($0.id, $0) } ?? []
        )

        for dto in egFormInstances {
            guard let instanceId = UUID(uuidString: dto.id),
                  let formId = UUID(uuidString: dto.eg_form_id) else { continue }
            let createdAt = dto.created_at.flatMap { isoFormatter.date(from: $0) } ?? Date()
            let modifiedAt = dto.modified_at.flatMap { isoFormatter.date(from: $0) }
            let form = egFormsByID[formId]

            if let existing = instancesByID[instanceId] {
                existing.eg_form_id = formId
                existing.form_submission = dto.formSubmissionString
                existing.submitted = dto.submitted ?? false
                existing.is_deleted = dto.is_deleted ?? false
                existing.modified_at = modifiedAt
                existing.egForm = form
                // Do NOT clear linkedTasks / linkedNodes here. Legacy
                // form sync also avoids this — it applies the mapping
                // diff directly. Clearing one side of an implicit
                // SwiftData relationship moves the auto-synthesized
                // single-valued inverse pointer and breaks links on
                // every other instance that referenced the same task.
            } else {
                let inst = EGFormInstance(
                    id: instanceId,
                    eg_form_id: formId,
                    form_submission: dto.formSubmissionString,
                    submitted: dto.submitted ?? false,
                    is_deleted: dto.is_deleted ?? false,
                    created_at: createdAt,
                    modified_at: modifiedAt,
                    egForm: form
                )
                context.insert(inst)
                instancesByID[instanceId] = inst
            }
        }

        // Apply task-instance mapping diff on the TASK side
        // (`task.linkedEGFormInstances`). Mirrors the legacy pattern
        // in `processMappings` which appends to
        // `task.linkedFormInstances` and never writes
        // `instance.linkedTasks`. Touching the instance side triggers
        // SwiftData's auto-synthesized single-valued reverse to be
        // "moved" between instances, which is the bug that previously
        // caused only the most-recently-linked instance to retain its
        // task — and on re-sync, only the last-iterated mapping kept
        // its link.
        for m in taskEGFormInstanceMappings {
            guard let instId = UUID(uuidString: m.eg_form_instance_id),
                  let taskId = UUID(uuidString: m.task_id),
                  let inst = instancesByID[instId],
                  let task = tasksByID[taskId] else { continue }
            if m.is_deleted ?? false {
                task.linkedEGFormInstances.removeAll { $0.id == inst.id }
            } else if !task.linkedEGFormInstances.contains(where: { $0.id == inst.id }) {
                task.linkedEGFormInstances.append(inst)
            }
        }

        // Apply the instance-node mapping diff: add active mappings, and
        // REMOVE the ones the server marked deleted (e.g. a link removed on
        // web). Mirrors the legacy FormInstance-Node handling below — only
        // adding would leave stale links visible after a remote unlink.
        for m in egFormInstanceNodeMappings {
            guard let instId = UUID(uuidString: m.eg_form_instance_id),
                  let nodeId = UUID(uuidString: m.node_id),
                  let inst = instancesByID[instId],
                  let node = nodeLookup[nodeId] else { continue }
            if m.is_deleted ?? false {
                inst.linkedNodes.removeAll { $0.id == node.id }
            } else if !inst.linkedNodes.contains(where: { $0.id == node.id }) {
                inst.linkedNodes.append(node)
            }
        }

        AppLogger.log(.info, "Processed \(egForms.count) EG forms and \(egFormInstances.count) EG instances", category: .sync)

        // ZP-1723 diagnostic — dump per-instance state after the
        // rebuild loop so we can see whether each instance ended up
        // with its expected linkedTasks count, and which task IDs.
        for inst in instancesByID.values {
            let taskIds = inst.linkedTasks.map { $0.id.uuidString.prefix(8) }.joined(separator: ",")
            AppLogger.log(.info, """
            [EGForm DEBUG sync] inst=\(inst.id.uuidString.prefix(8)) \
            title=\(inst.egForm?.title ?? "?") submitted=\(inst.submitted) \
            deleted=\(inst.is_deleted) linkedTasks=\(inst.linkedTasks.count) [\(taskIds)]
            """, category: .sync)
        }
        AppLogger.log(.info, "[EGForm DEBUG sync] taskEGMappings count=\(taskEGFormInstanceMappings.count) (deleted filtered)", category: .sync)
        for m in taskEGFormInstanceMappings where !(m.is_deleted ?? false) {
            AppLogger.log(.info, "  task=\(m.task_id.prefix(8)) → inst=\(m.eg_form_instance_id.prefix(8))", category: .sync)
        }
    }

    // MARK: - MAIN UPSERT HELPER: Quote Processing Function
    @MainActor
    private func processQuotes(
        _ pendingQuoteDTOs: [(quoteDTO: QuoteDTO, sldId: UUID)],
        sldLookup: [UUID: SLDV2],
        quotesByID: inout [UUID: Quote],
        context: ModelContext
    ) async throws {
        AppLogger.log(.info, "\nStep 5: Processing \(pendingQuoteDTOs.count) Quotes...", category: .sync)
        let existingQuotes = try context.fetch(FetchDescriptor<Quote>())
        quotesByID = Dictionary(uniqueKeysWithValues: existingQuotes.map { ($0.id, $0) })

        for (index, (quoteDTO, sldId)) in pendingQuoteDTOs.enumerated() {
            if shouldUpdateProgress(index: index, total: pendingQuoteDTOs.count, step: .quotes) {
                await Task.yield()
            }
            guard let sld = sldLookup[sldId] else {
                // print("Quote \(quoteDTO.id) references non-existent SLD \(sldId)")
                continue
            }
            
            if let existing = quotesByID[quoteDTO.id] {
                // Update existing quote
                // print(" Updating quote: \(quoteDTO.title ?? "Untitled")")
                existing.created_date = quoteDTO.created_date
                existing.modified_date = quoteDTO.modified_date
                existing.title = quoteDTO.title
                existing.sow = quoteDTO.sow
                existing.tnm = quoteDTO.tnm
                existing.quoteDescription = quoteDTO.description
                existing.status = quoteDTO.status
                existing.is_deleted = quoteDTO.is_deleted
                existing.sld = sld
            } else {
                // Create new quote
                // print(" ✨ Creating quote: \(quoteDTO.title ?? "Untitled")")
                let quote = Quote(
                    id: quoteDTO.id,
                    created_date: quoteDTO.created_date,
                    modified_date: quoteDTO.modified_date,
                    title: quoteDTO.title,
                    sow: quoteDTO.sow,
                    tnm: quoteDTO.tnm,
                    sld: sld,
                    quoteDescription: quoteDTO.description,
                    status: quoteDTO.status,
                    is_deleted: quoteDTO.is_deleted,
                    tasks: []
                )
                context.insert(quote)
                quotesByID[quoteDTO.id] = quote
            }
        }
    }
    
    // MARK: - MAIN UPSERT HELPER: Issue Processing Function
    @MainActor
    private func processIssues(
        _ pendingIssueDTOs: [(issueDTO: IssueDTO, sldId: UUID)],
        sldLookup: [UUID: SLDV2],
        nodeLookup: [UUID: NodeV2],
        irSessionLookup: [UUID: IRSession],
        issuesByID: inout [UUID: Issue],
        context: ModelContext
    ) async throws {
        AppLogger.log(.info, "\nStep 6: Processing \(pendingIssueDTOs.count) Issues...", category: .sync)
        let existingIssues = try context.fetch(FetchDescriptor<Issue>())
        issuesByID = Dictionary(uniqueKeysWithValues: existingIssues.map { ($0.id, $0) })

        for (index, (issueDTO, sldId)) in pendingIssueDTOs.enumerated() {
            if shouldUpdateProgress(index: index, total: pendingIssueDTOs.count, step: .issues) {
                await Task.yield()
            }
            guard let sld = sldLookup[sldId] else {
                // print("Issue \(issueDTO.id) references non-existent SLD \(sldId)")
                continue
            }
            
            // Fetch IssueClass if provided
            let issueClassEntity: IssueClass?
            if let issueClassId = issueDTO.issue_class {
                let desc = FetchDescriptor<IssueClass>(
                    predicate: #Predicate<IssueClass> { $0.id == issueClassId }
                )
                issueClassEntity = try context.fetch(desc).first
            } else {
                issueClassEntity = nil
            }
            
            let issue: Issue
            
            if let existing = issuesByID[issueDTO.id] {
                // Update existing issue
                // print(" Updating issue: \(issueDTO.title ?? "Untitled")")
                
                // Store the old node to clean up if needed
                let oldNode = existing.node
                
                existing.title = issueDTO.title
                existing.issueDescription = issueDTO.description
                existing.created_date = issueDTO.created_date
                existing.issue_type = issueDTO.issue_type
                existing.issue_subtype = issueDTO.issue_subtype
                existing.is_deleted = issueDTO.is_deleted
                existing.status = issueDTO.status
                existing.proposed_resolution = issueDTO.proposed_resolution
                existing.modified_date = issueDTO.modified_date
                existing.priority = issueDTO.priority
                existing.immediateHazard = issueDTO.immediate_hazard ?? false
                existing.customerNotified = issueDTO.customer_notified ?? false
                existing.sld = sld
                existing.issue_class = issueClassEntity
                existing.lastSyncedAt = Date()
                issue = existing

                // Clean up old node relationship if it's changing
                if let oldNode = oldNode, oldNode.id != issueDTO.node_id {
                    oldNode.issues.removeAll { $0.id == issue.id }
                    // print(" Removed issue from old node: \(oldNode.label)")
                }
                
                // Handle details update (IssueProperty array)
                let incomingDetails = issueDTO.details ?? []
                
                if !incomingDetails.isEmpty {
                    // Debug: Print all incoming details
                    // print("Incoming details:")
                    for (_, _) in incomingDetails.enumerated() {
                        // print(" [\(index)] id: \(detail.id), name: '\(detail.name)', value: '\(detail.value)', issue_class_property: \(detail.issue_class_property ?? "nil")")
                    }
                    
                    // Create a lookup of existing details by their ID
                    let existingDetailsLookup = Dictionary(
                        uniqueKeysWithValues: existing.details.map { ($0.id, $0) }
                    )
                    
                    // Debug: Print all existing details
                    AppLogger.log(.info, "Existing details before update:", category: .sync)
                    for (_, _) in existingDetailsLookup {
                        // print(" id: \(id), name: '\(detail.name)', value: '\(detail.value)', issue_class_property: \(detail.issue_class_property?.id.uuidString ?? "nil")")
                    }
                    
                    // Track which detail IDs we've seen from the DTO
                    var seenDetailIds = Set<UUID>()
                    
                    // Update or create details
                    for detailDTO in incomingDetails {
                        // print("\nProcessing detail: '\(detailDTO.name)' (id: \(detailDTO.id))")

                        // Track that we've seen this detail ID
                        seenDetailIds.insert(detailDTO.id)
                        // print(" Added to seenDetailIds. Set now contains: \(seenDetailIds.count) items")

                        // Look up the IssueClassProperty (nil if missing/invalid).
                        // Don't `continue` on lookup miss when an existing detail
                        // is present — we still need to overwrite any dangling
                        // SwiftData reference it may hold (ZP-2366).
                        let issueClassProperty: IssueClassProperty?
                        if let uuid = UUID(uuidString: detailDTO.issue_class_property) {
                            let desc = FetchDescriptor<IssueClassProperty>(
                                predicate: #Predicate<IssueClassProperty> { $0.id == uuid }
                            )
                            issueClassProperty = try context.fetch(desc).first
                        } else {
                            issueClassProperty = nil
                        }

                        if let existingDetail = existingDetailsLookup[detailDTO.id] {
                            // Update existing detail
                            // print(" Updating existing detail:")
                            // print(" Old value: '\(existingDetail.value)'")
                            // print(" New value: '\(detailDTO.value)'")
                            existingDetail.name = detailDTO.name
                            existingDetail.value = detailDTO.value
                            existingDetail.unit = detailDTO.unit
                            existingDetail.attributeNotes = detailDTO.description
                            existingDetail.issue_class_property = issueClassProperty
                            // print(" Updated detail: \(detailDTO.name) with value: \(detailDTO.value)")
                        } else if let issueClassProperty {
                            // Create new detail
                            // print(" Creating new detail (not found in existingDetailsLookup)")
                            let newDetail = IssueProperty(
                                id: detailDTO.id,
                                issue_class_property: issueClassProperty,
                                name: detailDTO.name,
                                value: detailDTO.value,
                                unit: detailDTO.unit,
                                attributeNotes: detailDTO.description
                            )
                            context.insert(newDetail)
                            existing.details.append(newDetail)
                            // print(" ✨ Created detail: \(detailDTO.name) with value: \(detailDTO.value)")
                            // print(" Issue now has \(existing.details.count) details")
                        }
                    }
                    
                    // Debug: Print seen detail IDs
                    // print("\nSeen detail IDs (\(seenDetailIds.count) total):")
                    for id in seenDetailIds {
                        AppLogger.log(.info, "\(id)", category: .sync)
                    }
                    
                    // Remove details that are no longer in the DTO
                    // print("\nChecking for details to remove...")
                    let detailsToRemove = existing.details.filter {
                        let shouldRemove = !seenDetailIds.contains($0.id)
                        if shouldRemove {
                            // print(" Marking for removal: '\($0.name)' (id: \($0.id)) - NOT in seenDetailIds")
                        }
                        return shouldRemove
                    }
                    
                    // print("Found \(detailsToRemove.count) details to remove")
                    for detail in detailsToRemove {
                        // print(" Removing: '\(detail.name)' (id: \(detail.id))")
                        existing.details.removeAll { $0.id == detail.id }
                        context.delete(detail)
                        // print(" Removed detail: \(detail.name)")
                    }
                    
                    // Final state
                    // print("\nFinal state:")
                    // print(" Issue now has \(existing.details.count) details")
                    for detail in existing.details {
                        AppLogger.log(.info, "'\(detail.name)': '\(detail.value)' (id: \(detail.id))", category: .sync)
                    }
                    
                } else {
                    // If DTO has empty details, clear existing ones
                    // print("DTO has empty details, clearing all existing details")
                    for detail in existing.details {
                        context.delete(detail)
                        // print(" Deleting: '\(detail.name)'")
                    }
                    existing.details.removeAll()
                    // print(" Cleared all details")
                }
                
            } else {
                // Create new issue
                issue = Issue(
                    id: issueDTO.id,
                    title: issueDTO.title,
                    issueDescription: issueDTO.description,
                    created_date: issueDTO.created_date,
                    node: nil,
                    issue_class: issueClassEntity,
                    issue_type: issueDTO.issue_type,
                    issue_subtype: issueDTO.issue_subtype,
                    is_deleted: issueDTO.is_deleted,
                    session: nil,
                    sld: sld,
                    details: [], // Initialize empty, will be populated below
                    status: issueDTO.status,
                    proposed_resolution: issueDTO.proposed_resolution,
                    modified_date: issueDTO.modified_date,
                    priority: issueDTO.priority,
                    tasks: [],
                    immediateHazard: issueDTO.immediate_hazard ?? false,
                    customerNotified: issueDTO.customer_notified ?? false
                )
                issue.lastSyncedAt = Date()
                context.insert(issue)
                issuesByID[issueDTO.id] = issue

                // Then create and add details if they exist
                let detailDTOs = issueDTO.details ?? []
                if !detailDTOs.isEmpty {
                    for detailDTO in detailDTOs {
                        // Get the raw ID string and ensure it's not empty
                        let issueClassPropertyId = detailDTO.issue_class_property
                        guard !issueClassPropertyId.isEmpty else {
                            // print("Warning: No issue_class_property ID provided for detail: \(detailDTO.name)")
                            continue
                        }
                        
                        // Parse string → UUID
                        guard let uuid = UUID(uuidString: issueClassPropertyId) else {
                            // print("Warning: Invalid UUID string: \(issueClassPropertyId) for detail: \(detailDTO.name)")
                            continue
                        }
                        
                        // Fetch by comparing two UUIDs
                        let desc = FetchDescriptor<IssueClassProperty>(
                            predicate: #Predicate<IssueClassProperty> { $0.id == uuid }
                        )
                        guard let issueClassProperty = try context.fetch(desc).first else {
                            // print("Warning: IssueClassProperty with ID \(issueClassPropertyId) not found for detail: \(detailDTO.name)")
                            continue
                        }
                        
                        // Create & insert the IssueProperty
                        let issueProperty = IssueProperty(
                            id: detailDTO.id,
                            issue_class_property: issueClassProperty,
                            name: detailDTO.name,
                            value: detailDTO.value,
                            unit: detailDTO.unit,
                            attributeNotes: detailDTO.description
                        )
                        context.insert(issueProperty)
                        issue.details.append(issueProperty)
                        // print(" ✨ Created detail: \(detailDTO.name)")
                    }
                }
            }

            // Handle status_history upsert
            let incomingHistory = issueDTO.status_history ?? []
            if !incomingHistory.isEmpty {
                let existingHistoryLookup = Dictionary(
                    uniqueKeysWithValues: issue.statusHistory.map { ($0.id, $0) }
                )
                var seenHistoryIds = Set<UUID>()

                for historyDTO in incomingHistory {
                    seenHistoryIds.insert(historyDTO.id)
                    if let existingEntry = existingHistoryLookup[historyDTO.id] {
                        // Update existing entry
                        existingEntry.old_status = historyDTO.old_status
                        existingEntry.new_status = historyDTO.new_status
                        existingEntry.changed_by = historyDTO.changed_by
                        existingEntry.changed_by_name = historyDTO.changed_by_name
                        existingEntry.changed_at = historyDTO.changed_at
                        existingEntry.change_reason = historyDTO.change_reason
                    } else {
                        // Create new entry
                        let newEntry = IssueStatusHistory(
                            id: historyDTO.id,
                            issue_id: historyDTO.issue_id,
                            old_status: historyDTO.old_status,
                            new_status: historyDTO.new_status,
                            changed_by: historyDTO.changed_by,
                            changed_by_name: historyDTO.changed_by_name,
                            changed_at: historyDTO.changed_at,
                            change_reason: historyDTO.change_reason,
                            issue: issue
                        )
                        context.insert(newEntry)
                        issue.statusHistory.append(newEntry)
                    }
                }

                // Remove entries no longer in the incoming data
                let historyToRemove = issue.statusHistory.filter { !seenHistoryIds.contains($0.id) }
                for entry in historyToRemove {
                    issue.statusHistory.removeAll { $0.id == entry.id }
                    context.delete(entry)
                }
            }

            // Link to node if specified
            if let nodeID = issueDTO.node_id {
                if let node = nodeLookup[nodeID] {
                    issue.node = node
                    // Add to node's issues array if not already present
                    if !node.issues.contains(where: { $0.id == issue.id }) {
                        node.issues.append(issue)
                    }
                    // print(" Linked to node: \(node.label)")
                } else {
                    // print(" Issue references non-existent node \(nodeID)")
                }
            } else {
                // If no node_id is specified, clear the node relationship
                issue.node = nil
            }

            // Link to session if specified
            if let sessionID = issueDTO.session_id {
                if let session = irSessionLookup[sessionID] {
                    issue.session = session
                    if !session.issues.contains(where: { $0.id == issue.id }) {
                        session.issues.append(issue)
                    }
                    // print(" Linked to session: \(session.name)")
                } else {
                    // print(" Issue references non-existent session \(sessionID)")
                }
            }
        }
    }
    
    // MARK: - MAIN UPSERT HELPER: Mappings Processing Function
    @MainActor
    private func processMappings(
        _ pendingMappings: MappingDTOs,
        issuesByID: [UUID: Issue],
        tasksByID: [UUID: UserTask],
        irSessionLookup: [UUID: IRSession],
        quotesByID: [UUID: Quote],
        nodeLookup: [UUID: NodeV2],
        formsByID: [UUID: UserTaskForm],
        formInstancesByID: [UUID: FormInstance]
    ) {
        AppLogger.log(.info, "\nStep 7: Processing Mappings...", category: .sync)
        
        // Process Issue-Task mappings
        AppLogger.log(.info, "Processing \(pendingMappings.issueTask.count) Issue-Task mappings...", category: .sync)
        for mapping in pendingMappings.issueTask {
            guard let issueId = UUID(uuidString: mapping.issue_id),
                  let taskId = UUID(uuidString: mapping.task_id),
                  let issue = issuesByID[issueId],
                  let task = tasksByID[taskId] else {
                AppLogger.log(.notice, "Mapping references non-existent issue (\(mapping.issue_id)) or task (\(mapping.task_id))", category: .sync)
                continue
            }
            
            if mapping.is_deleted {
                // Remove the relationship if it exists
                if issue.tasks.contains(where: { $0.id == task.id }) {
                    issue.tasks.removeAll { $0.id == task.id }
                    AppLogger.log(.info, "Removed link between issue '\(issue.title ?? "Untitled")' and task '\(task.title)'", category: .sync)
                }
            } else {
                // Add the relationship if it doesn't exist
                if !issue.tasks.contains(where: { $0.id == task.id }) {
                    issue.tasks.append(task)
                    AppLogger.log(.info, "Linked issue '\(issue.title ?? "Untitled")' to task '\(task.title)'", category: .sync)
                }
            }
        }
        
        // Process Task-Session mappings
        AppLogger.log(.info, "Processing \(pendingMappings.taskSession.count) Task-Session mappings...", category: .sync)
        for mapping in pendingMappings.taskSession {
            guard let taskId = UUID(uuidString: mapping.task_id),
                  let sessionId = UUID(uuidString: mapping.session_id),
                  let task = tasksByID[taskId],
                  let session = irSessionLookup[sessionId] else {
                AppLogger.log(.notice, "Mapping references non-existent task (\(mapping.task_id)) or session (\(mapping.session_id))", category: .sync)
                continue
            }
            
            if mapping.is_deleted {
                // Remove the relationship if it exists
                if session.user_tasks.contains(where: { $0.id == task.id }) {
                    session.user_tasks.removeAll { $0.id == task.id }
                    AppLogger.log(.info, "Removed link between task '\(task.title)' and session '\(session.name)'", category: .sync)
                    AppLogger.log(.info, "Session '\(session.name)' now has \(session.user_tasks.count) tasks", category: .sync)
                }
            } else {
                // Add the relationship if it doesn't exist
                if !session.user_tasks.contains(where: { $0.id == task.id }) {
                    session.user_tasks.append(task)
                    AppLogger.log(.info, "Linked task '\(task.title)' to session '\(session.name)'", category: .sync)
                    AppLogger.log(.info, "Session '\(session.name)' now has \(session.user_tasks.count) tasks", category: .sync)
                } else {
                    AppLogger.log(.info, "ℹ️ Task '\(task.title)' already linked to session '\(session.name)'", category: .sync)
                }
            }
        }
        
        // Process Quote-Task mappings
        AppLogger.log(.info, "Processing \(pendingMappings.quoteTask.count) Quote-Task mappings...", category: .sync)
        for mapping in pendingMappings.quoteTask {
            guard let quoteId = UUID(uuidString: mapping.quote_id),
                  let taskId = UUID(uuidString: mapping.task_id),
                  let quote = quotesByID[quoteId],
                  let task = tasksByID[taskId] else {
                AppLogger.log(.notice, "Mapping references non-existent quote (\(mapping.quote_id)) or task (\(mapping.task_id))", category: .sync)
                continue
            }
            
            if mapping.is_deleted {
                // Remove the relationship if it exists
                if quote.tasks.contains(where: { $0.id == task.id }) {
                    quote.tasks.removeAll { $0.id == task.id }
                    AppLogger.log(.info, "Removed link between quote '\(quote.title ?? "Untitled")' and task '\(task.title)'", category: .sync)
                }
            } else {
                // Add the relationship if it doesn't exist
                if !quote.tasks.contains(where: { $0.id == task.id }) {
                    quote.tasks.append(task)
                    AppLogger.log(.info, "Linked quote '\(quote.title ?? "Untitled")' to task '\(task.title)'", category: .sync)
                }
            }
        }
        
        // Process User-Task mappings
        AppLogger.log(.info, "Processing \(pendingMappings.userTask.count) User-Task mappings...", category: .sync)
        for mapping in pendingMappings.userTask {
            // Convert IDs to uppercase for consistent UUID parsing
            let taskIdString = mapping.task_id.uppercased()
            let userIdString = mapping.user_id.uppercased()
            
            guard let taskId = UUID(uuidString: taskIdString),
                  let userId = UUID(uuidString: userIdString),
                  let task = tasksByID[taskId] else {
                AppLogger.log(.notice, "Mapping references non-existent task (\(mapping.task_id))", category: .sync)
                AppLogger.log(.info, "Task ID (original): \(mapping.task_id)", category: .sync)
                AppLogger.log(.info, "Task ID (uppercase): \(taskIdString)", category: .sync)
                AppLogger.log(.info, "User ID (original): \(mapping.user_id)", category: .sync)
                AppLogger.log(.info, "User ID (uppercase): \(userIdString)", category: .sync)
                continue
            }
            
            let mappingType = mapping.mapping_type ?? "assignee" // Default to assignee if not specified
            
            if mapping.is_deleted {
                // Remove the user from the appropriate list
                if mappingType == "owner" {
                    if task.owned_by.contains(where: { $0 == userId }) {
                        task.owned_by.removeAll { $0 == userId }
                        AppLogger.log(.info, "Removed user '\(mapping.user_id)' as owner from task '\(task.title)'", category: .sync)
                        AppLogger.log(.info, "👥 Task now has \(task.owned_by.count) owner(s)", category: .sync)
                    }
                } else if mappingType == "assignee" {
                    if task.assigned_to.contains(where: { $0 == userId }) {
                        task.assigned_to.removeAll { $0 == userId }
                        AppLogger.log(.info, "Removed user '\(mapping.user_id)' as assignee from task '\(task.title)'", category: .sync)
                        AppLogger.log(.info, "Task now has \(task.assigned_to.count) assignee(s)", category: .sync)
                    }
                } else {
                    AppLogger.log(.notice, "Unknown mapping type: '\(mappingType)' for user-task mapping", category: .sync)
                }
            } else {
                // Add the user to the appropriate list if not already present
                if mappingType == "owner" {
                    if !task.owned_by.contains(where: { $0 == userId }) {
                        task.owned_by.append(userId)
                        AppLogger.log(.info, "Added user '\(mapping.user_id)' as owner to task '\(task.title)'", category: .sync)
                        AppLogger.log(.info, "👥 Task now has \(task.owned_by.count) owner(s)", category: .sync)
                    } else {
                        AppLogger.log(.info, "ℹ️ User '\(mapping.user_id)' is already an owner of task '\(task.title)'", category: .sync)
                    }
                } else if mappingType == "assignee" {
                    if !task.assigned_to.contains(where: { $0 == userId }) {
                        task.assigned_to.append(userId)
                        AppLogger.log(.info, "Added user '\(mapping.user_id)' as assignee to task '\(task.title)'", category: .sync)
                        AppLogger.log(.info, "Task now has \(task.assigned_to.count) assignee(s)", category: .sync)
                    } else {
                        AppLogger.log(.info, "ℹ️ User '\(mapping.user_id)' is already assigned to task '\(task.title)'", category: .sync)
                    }
                } else {
                    AppLogger.log(.notice, "Unknown mapping type: '\(mappingType)' for user-task mapping", category: .sync)
                }
            }
        }
        
        // Process Task-Node mappings
        AppLogger.log(.info, "Processing \(pendingMappings.taskNode.count) Task-Node mappings...", category: .sync)
        AppLogger.log(.info, "Debug: Have \(nodeLookup.count) nodes and \(tasksByID.count) tasks in lookups", category: .sync)
        
        // Collect all task and node IDs that appear in the mappings
        var tasksInMappings = Set<UUID>()
        var nodesInMappings = Set<UUID>()
        for mapping in pendingMappings.taskNode {
            if let taskId = UUID(uuidString: mapping.task_id) {
                tasksInMappings.insert(taskId)
            }
            if let nodeId = UUID(uuidString: mapping.node_id) {
                nodesInMappings.insert(nodeId)
            }
        }
        
        // Only clear relationships for tasks and nodes that appear in the mappings
        // This prevents us from destroying relationships that aren't being updated
        for node in nodeLookup.values where nodesInMappings.contains(node.id) {
            let before = node.node_tasks.count
            node.node_tasks.removeAll()
            if before > 0 {
                AppLogger.log(.info, "Cleared \(before) tasks from node '\(node.label)'", category: .sync)
            }
        }
        for task in tasksByID.values where tasksInMappings.contains(task.id) {
            let before = task.linkedNodes.count
            task.linkedNodes.removeAll()
            task.nodeCompletions.removeAll()
            if before > 0 {
                AppLogger.log(.info, "Cleared \(before) nodes from task '\(task.title)'", category: .sync)
            }
        }
        
        // Now rebuild relationships from server data (only non-deleted mappings)
        for mapping in pendingMappings.taskNode {
            // Skip deleted mappings entirely since we cleared everything
            if mapping.is_deleted {
                continue
            }
            
            guard let taskId = UUID(uuidString: mapping.task_id),
                  let nodeId = UUID(uuidString: mapping.node_id),
                  let task = tasksByID[taskId],
                  let node = nodeLookup[nodeId] else {
                AppLogger.log(.notice, "Mapping references non-existent task (\(mapping.task_id)) or node (\(mapping.node_id))", category: .sync)
                continue
            }
            
            // Add the relationship (no need to check if it exists since we cleared everything)
            task.linkedNodes.append(node)
            node.node_tasks.append(task)
            
            // Track per-node completion state (use nodeId.uuidString for consistent uppercase keys)
            if let isCompleted = mapping.is_completed {
                task.nodeCompletions[nodeId.uuidString] = isCompleted
            }
            
            AppLogger.log(.info, "Linked task '\(task.title)' to node '\(node.label ?? "Untitled")')", category: .sync)
        }
        
        // Debug: Check SWITCHBOARD specifically
        if let switchboard = nodeLookup.values.first(where: { $0.label == "SWITCHBOARD" }) {
            AppLogger.log(.info, "SWITCHBOARD now has \(switchboard.node_tasks.count) tasks:", category: .sync)
            for task in switchboard.node_tasks {
                AppLogger.log(.info, "\(task.title)", category: .sync)
            }
            AppLogger.log(.info, "SWITCHBOARD node ID: \(switchboard.id)", category: .sync)
            AppLogger.log(.info, "SWITCHBOARD object: \(ObjectIdentifier(switchboard))", category: .sync)
            
            // Check what tasks exist in the lookups
            let taskTitles = tasksByID.values.map { $0.title }.sorted()
            AppLogger.log(.info, "All tasks in lookup: \(taskTitles)", category: .sync)
        }
        
        // Process Task-Form mappings
        AppLogger.log(.info, "Processing \(pendingMappings.taskForm.count) Task-Form mappings...", category: .sync)
        for mapping in pendingMappings.taskForm {
            guard let taskId = mapping.task_id.flatMap({ UUID(uuidString: $0) }),
                  let formId = mapping.form_id.flatMap({ UUID(uuidString: $0) }),
                  let task = tasksByID[taskId],
                  let form = formsByID[formId] else {
                AppLogger.log(.notice, "Mapping references non-existent task (\(mapping.task_id ?? "nil")) or form (\(mapping.form_id ?? "nil"))", category: .sync)
                continue
            }
            
            if mapping.is_deleted {
                // Remove the relationship if it exists
                if task.linkedForms.contains(where: { $0.id == form.id }) {
                    task.linkedForms.removeAll { $0.id == form.id }
                    AppLogger.log(.info, "Removed link between task '\(task.title)' and form '\(form.title)'", category: .sync)
                }
            } else {
                // Add the relationship if it doesn't exist
                if !task.linkedForms.contains(where: { $0.id == form.id }) {
                    task.linkedForms.append(form)
                    AppLogger.log(.info, "Linked task '\(task.title)' to form '\(form.title)'", category: .sync)
                }
            }
        }
        
        // Process Task-FormInstance mappings
        AppLogger.log(.info, "Processing \(pendingMappings.taskFormInstance.count) Task-FormInstance mappings...", category: .sync)
        for mapping in pendingMappings.taskFormInstance {
            guard let taskId = UUID(uuidString: mapping.task_id),
                  let instanceId = UUID(uuidString: mapping.form_instance_id),
                  let task = tasksByID[taskId],
                  let instance = formInstancesByID[instanceId] else {
                AppLogger.log(.notice, "Mapping references non-existent task (\(mapping.task_id)) or form instance (\(mapping.form_instance_id))", category: .sync)
                continue
            }
            
            if mapping.is_deleted ?? false {
                // Remove the relationship if it exists
                if task.linkedFormInstances.contains(where: { $0.id == instance.id }) {
                    task.linkedFormInstances.removeAll { $0.id == instance.id }
                    AppLogger.log(.info, "Removed link between task '\(task.title)' and form instance '\(instance.id)'", category: .sync)
                }
            } else {
                // Add the relationship if it doesn't exist
                if !task.linkedFormInstances.contains(where: { $0.id == instance.id }) {
                    task.linkedFormInstances.append(instance)
                    AppLogger.log(.info, "Linked task '\(task.title)' to form instance '\(instance.id)'", category: .sync)
                }
            }
        }
        
        // Process FormInstance-Node mappings
        AppLogger.log(.info, "Processing \(pendingMappings.formInstanceNode.count) FormInstance-Node mappings...", category: .sync)
        for mapping in pendingMappings.formInstanceNode {
            guard let instanceId = UUID(uuidString: mapping.form_instance_id),
                  let nodeId = UUID(uuidString: mapping.node_id),
                  let instance = formInstancesByID[instanceId],
                  let node = nodeLookup[nodeId] else {
                AppLogger.log(.notice, "Mapping references non-existent form instance (\(mapping.form_instance_id)) or node (\(mapping.node_id))", category: .sync)
                continue
            }
            
            if mapping.is_deleted ?? false {
                // Remove the relationship if it exists
                if instance.linkedNodes.contains(where: { $0.id == node.id }) {
                    instance.linkedNodes.removeAll { $0.id == node.id }
                    AppLogger.log(.info, "Removed link between form instance '\(instance.id)' and node '\(node.label)'", category: .sync)
                }
            } else {
                // Add the relationship if it doesn't exist
                if !instance.linkedNodes.contains(where: { $0.id == node.id }) {
                    instance.linkedNodes.append(node)
                    AppLogger.log(.info, "Linked form instance '\(instance.id)' to node '\(node.label)'", category: .sync)
                }
            }
        }
        
        // Process Node-Session mappings
        AppLogger.log(.info, "Processing \(pendingMappings.nodeSession.count) Node-Session mappings...", category: .sync)
        for mapping in pendingMappings.nodeSession {
            guard let nodeId = UUID(uuidString: mapping.node_id),
                  let sessionId = UUID(uuidString: mapping.session_id),
                  let node = nodeLookup[nodeId],
                  let session = irSessionLookup[sessionId] else {
                AppLogger.log(.notice, "Mapping references non-existent node (\(mapping.node_id)) or session (\(mapping.session_id))", category: .sync)
                continue
            }
            
            if mapping.is_deleted {
                // Remove the relationship if it exists
                if node.ir_sessions.contains(where: { $0.id == session.id }) {
                    node.ir_sessions.removeAll { $0.id == session.id }
                    session.nodes.removeAll { $0.id == node.id }
                    AppLogger.log(.info, "Removed link between node '\(node.label)' and session '\(session.name)'", category: .sync)
                }
            } else {
                // Add the relationship if it doesn't exist
                if !node.ir_sessions.contains(where: { $0.id == session.id }) {
                    node.ir_sessions.append(session)
                    if !session.nodes.contains(where: { $0.id == node.id }) {
                        session.nodes.append(node)
                    }
                    AppLogger.log(.info, "Linked node '\(node.label)' to session '\(session.name)'", category: .sync)
                }
            }
        }
        
        // Process User-Session mappings (populate owned_by and assigned_to on IRSession)
        AppLogger.log(.info, "Processing \(pendingMappings.userSession.count) User-Session mappings...", category: .sync)
        
        // Reset owned_by and assigned_to on all sessions that have mappings,
        // then rebuild from non-deleted mappings only. This ensures deleted
        // mappings (unassigned users) are properly reflected.
        let sessionIdsWithMappings = Set(pendingMappings.userSession.compactMap { UUID(uuidString: $0.session_id) })
        for sessionId in sessionIdsWithMappings {
            if let session = irSessionLookup[sessionId] {
                session.owned_by = []
                session.assigned_to = []
            }
        }
        
        for mapping in pendingMappings.userSession {
            guard let userId = UUID(uuidString: mapping.user_id),
                  let sessionId = UUID(uuidString: mapping.session_id),
                  let session = irSessionLookup[sessionId] else {
                continue
            }
            
            // Only add from non-deleted mappings
            guard !mapping.is_deleted else { continue }
            
            if mapping.mapping_type == "reporter" || mapping.mapping_type == "owner" {
                if !session.owned_by.contains(userId) {
                    session.owned_by.append(userId)
                }
            } else if mapping.mapping_type == "assignee" {
                if !session.assigned_to.contains(userId) {
                    session.assigned_to.append(userId)
                }
            }
        }
    }
    
    // MARK: - MAIN UPSERT HELPER: Comment Processing Function
    @MainActor
    private func processComments(
        _ pendingCommentDTOs: [(commentDTO: SLDCommentDTO, sldId: UUID)],
        sldLookup: [UUID: SLDV2],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nStep 10: Processing \(pendingCommentDTOs.count) Comments...", category: .sync)
        let existingComments = try context.fetch(FetchDescriptor<SLDComment>())
        var commentsByID = Dictionary(uniqueKeysWithValues: existingComments.map { ($0.id, $0) })
        
        for (commentDTO, sldId) in pendingCommentDTOs {
            guard let sld = sldLookup[sldId] else {
                AppLogger.log(.notice, "Comment \(commentDTO.id): Can't find SLD \(sldId)", category: .sync)
                continue
            }
            
            let comment: SLDComment
            if let existingComment = commentsByID[commentDTO.id] {
                // Update existing comment
                existingComment.body = commentDTO.body
                existingComment.createdAt = commentDTO.created_at
                existingComment.modifiedAt = commentDTO.modified_at
                existingComment.author = commentDTO.author
                existingComment.authorName = commentDTO.author_name
                existingComment.sldId = commentDTO.sld_id
                existingComment.x = commentDTO.x
                existingComment.y = commentDTO.y
                existingComment.width = commentDTO.width
                existingComment.height = commentDTO.height
                existingComment.sld = sld
                comment = existingComment
            } else {
                // Create new comment
                comment = SLDComment(from: commentDTO)
                comment.sld = sld
                context.insert(comment)
                commentsByID[commentDTO.id] = comment
            }
            
            // Link comment to SLD if not already linked
            if !sld.comments.contains(where: { $0.id == comment.id }) {
                sld.comments.append(comment)
            }
        }
        
        AppLogger.log(.info, "Processed \(pendingCommentDTOs.count) comments", category: .sync)
    }
    
    // MARK: - MAIN UPSERT HELPER: SLD View Processing Function
    @MainActor
    private func processSLDViews(
        _ pendingSLDViewDTOs: [(viewDTO: SLDViewDTO, sldId: UUID)],
        sldLookup: [UUID: SLDV2],
        viewLookup: inout [UUID: SLDViewV2],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nProcessing \(pendingSLDViewDTOs.count) SLD Views...", category: .sync)
        let existingViews = try context.fetch(FetchDescriptor<SLDViewV2>())
        viewLookup = Dictionary(uniqueKeysWithValues: existingViews.map { ($0.id, $0) })
        
        for (viewDTO, sldId) in pendingSLDViewDTOs {
            guard let sld = sldLookup[sldId] else {
                AppLogger.log(.notice, "SLD View \(viewDTO.id): Can't find SLD \(sldId)", category: .sync)
                continue
            }
            
            guard let viewId = UUID(uuidString: viewDTO.id) else {
                AppLogger.log(.notice, "SLD View has invalid UUID: \(viewDTO.id)", category: .sync)
                continue
            }
            
            if let existingView = viewLookup[viewId] {
                // Update existing view
                existingView.sld_id = UUID(uuidString: viewDTO.sld_id) ?? existingView.sld_id
                existingView.name = viewDTO.name
                existingView.viewDescription = viewDTO.description
                existingView.created_by = viewDTO.created_by != nil ? UUID(uuidString: viewDTO.created_by!) : nil
                existingView.view_type = viewDTO.view_type
                existingView.is_default = viewDTO.is_default
                existingView.is_deleted = viewDTO.is_deleted
                if let createdAt = viewDTO.created_at {
                    existingView.created_at = ISO8601DateFormatter().date(from: createdAt)
                }
                if let modifiedAt = viewDTO.modified_at {
                    existingView.modified_at = ISO8601DateFormatter().date(from: modifiedAt)
                }
            } else {
                // Create new view
                let view = SLDViewV2(
                    id: viewId,
                    sld_id: UUID(uuidString: viewDTO.sld_id) ?? sldId,
                    name: viewDTO.name,
                    viewDescription: viewDTO.description,
                    created_by: viewDTO.created_by != nil ? UUID(uuidString: viewDTO.created_by!) : nil,
                    view_type: viewDTO.view_type,
                    is_default: viewDTO.is_default,
                    is_deleted: viewDTO.is_deleted,
                    created_at: viewDTO.created_at != nil ? ISO8601DateFormatter().date(from: viewDTO.created_at!) : nil,
                    modified_at: viewDTO.modified_at != nil ? ISO8601DateFormatter().date(from: viewDTO.modified_at!) : nil
                )
                context.insert(view)
                viewLookup[viewId] = view
            }
        }
        
        AppLogger.log(.info, "Processed \(pendingSLDViewDTOs.count) SLD views", category: .sync)
    }
    
    // MARK: - MAIN UPSERT HELPER: SLD Link Processing Function
    @MainActor
    private func processSLDLinks(
        _ pendingSLDLinkDTOs: [(linkDTO: SLDLinkDTO, sldId: UUID)],
        viewLookup: [UUID: SLDViewV2],
        linkLookup: inout [UUID: SLDLinkV2],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nProcessing \(pendingSLDLinkDTOs.count) SLD Links...", category: .sync)
        let existingLinks = try context.fetch(FetchDescriptor<SLDLinkV2>())
        linkLookup = Dictionary(uniqueKeysWithValues: existingLinks.map { ($0.id, $0) })
        
        for (linkDTO, _) in pendingSLDLinkDTOs {
            guard let linkId = UUID(uuidString: linkDTO.id),
                  let sourceViewId = UUID(uuidString: linkDTO.source_sld_view_id),
                  let targetViewId = UUID(uuidString: linkDTO.target_sld_view_id) else {
                AppLogger.log(.notice, "SLD Link has invalid UUID(s)", category: .sync)
                continue
            }
            
            if let existingLink = linkLookup[linkId] {
                // Update existing link
                existingLink.source_sld_view_id = sourceViewId
                existingLink.target_sld_view_id = targetViewId
                existingLink.source_node_id = linkDTO.source_node_id != nil ? UUID(uuidString: linkDTO.source_node_id!) : nil
                existingLink.target_node_id = linkDTO.target_node_id != nil ? UUID(uuidString: linkDTO.target_node_id!) : nil
                existingLink.source_x = linkDTO.source_x
                existingLink.source_y = linkDTO.source_y
                existingLink.target_x = linkDTO.target_x
                existingLink.target_y = linkDTO.target_y
                existingLink.edge_direction = linkDTO.edge_direction
                existingLink.source_view_handle = linkDTO.source_view_handle
                existingLink.target_view_handle = linkDTO.target_view_handle
                existingLink.is_deleted = linkDTO.is_deleted
                existingLink.target_view_name = linkDTO.target_view_name
                existingLink.target_node_label = linkDTO.target_node_label
                existingLink.target_node_parent_label = linkDTO.target_node_parent_label
                if let createdAt = linkDTO.created_at {
                    existingLink.created_at = ISO8601DateFormatter().date(from: createdAt)
                }
                if let modifiedAt = linkDTO.modified_at {
                    existingLink.modified_at = ISO8601DateFormatter().date(from: modifiedAt)
                }
            } else {
                // Create new link
                let link = SLDLinkV2(
                    id: linkId,
                    source_sld_view_id: sourceViewId,
                    target_sld_view_id: targetViewId,
                    source_node_id: linkDTO.source_node_id != nil ? UUID(uuidString: linkDTO.source_node_id!) : nil,
                    target_node_id: linkDTO.target_node_id != nil ? UUID(uuidString: linkDTO.target_node_id!) : nil,
                    source_x: linkDTO.source_x,
                    source_y: linkDTO.source_y,
                    target_x: linkDTO.target_x,
                    target_y: linkDTO.target_y,
                    edge_direction: linkDTO.edge_direction,
                    source_view_handle: linkDTO.source_view_handle,
                    target_view_handle: linkDTO.target_view_handle,
                    is_deleted: linkDTO.is_deleted,
                    created_at: linkDTO.created_at != nil ? ISO8601DateFormatter().date(from: linkDTO.created_at!) : nil,
                    modified_at: linkDTO.modified_at != nil ? ISO8601DateFormatter().date(from: linkDTO.modified_at!) : nil,
                    target_view_name: linkDTO.target_view_name,
                    target_node_label: linkDTO.target_node_label,
                    target_node_parent_label: linkDTO.target_node_parent_label
                )
                context.insert(link)
                linkLookup[linkId] = link
            }
        }
        
        AppLogger.log(.info, "Processed \(pendingSLDLinkDTOs.count) SLD links", category: .sync)
    }
    
    // MARK: - MAIN UPSERT HELPER: Node-SLDView Mapping Processing Function
    @MainActor
    private func processNodeSLDViewMappings(
        _ mappingDTOs: [MappingNodeSLDViewDTO],
        nodeLookup: [UUID: NodeV2],
        viewLookup: [UUID: SLDViewV2],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nProcessing \(mappingDTOs.count) Node-SLDView Mappings...", category: .sync)
        let existingMappings = try context.fetch(FetchDescriptor<MappingNodeSLDView>())
        var mappingLookup = Dictionary(uniqueKeysWithValues: existingMappings.map { ($0.id, $0) })
        
        for mappingDTO in mappingDTOs {
            guard let mappingId = UUID(uuidString: mappingDTO.id),
                  let nodeId = UUID(uuidString: mappingDTO.node_id),
                  let viewId = UUID(uuidString: mappingDTO.sld_view_id) else {
                AppLogger.log(.notice, "Node-SLDView mapping has invalid UUID(s)", category: .sync)
                continue
            }
            
            if let existingMapping = mappingLookup[mappingId] {
                // Update existing mapping
                existingMapping.node_id = nodeId
                existingMapping.sld_view_id = viewId
                existingMapping.x = mappingDTO.x
                existingMapping.y = mappingDTO.y
                existingMapping.width = mappingDTO.width
                existingMapping.height = mappingDTO.height
                existingMapping.is_collapsed = mappingDTO.is_collapsed ?? false
                existingMapping.is_deleted = mappingDTO.is_deleted
                if let createdAt = mappingDTO.created_at {
                    existingMapping.created_at = ISO8601DateFormatter().date(from: createdAt)
                }
                if let modifiedAt = mappingDTO.modified_at {
                    existingMapping.modified_at = ISO8601DateFormatter().date(from: modifiedAt)
                }
            } else {
                // Create new mapping
                let mapping = MappingNodeSLDView(
                    id: mappingId,
                    node_id: nodeId,
                    sld_view_id: viewId,
                    x: mappingDTO.x,
                    y: mappingDTO.y,
                    width: mappingDTO.width,
                    height: mappingDTO.height,
                    is_collapsed: mappingDTO.is_collapsed ?? false,
                    is_deleted: mappingDTO.is_deleted,
                    created_at: mappingDTO.created_at != nil ? ISO8601DateFormatter().date(from: mappingDTO.created_at!) : nil,
                    modified_at: mappingDTO.modified_at != nil ? ISO8601DateFormatter().date(from: mappingDTO.modified_at!) : nil
                )
                context.insert(mapping)
                mappingLookup[mappingId] = mapping
            }
        }
        
        AppLogger.log(.info, "Processed \(mappingDTOs.count) node-view mappings", category: .sync)
    }
    
    // MARK: - MAIN UPSERT HELPER: Edge-SLDView Mapping Processing Function
    @MainActor
    private func processEdgeSLDViewMappings(
        _ mappingDTOs: [MappingEdgeSLDViewDTO],
        edgeLookup: [UUID: EdgeV2],
        viewLookup: [UUID: SLDViewV2],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nProcessing \(mappingDTOs.count) Edge-SLDView Mappings...", category: .sync)
        let existingMappings = try context.fetch(FetchDescriptor<MappingEdgeSLDView>())
        var mappingLookup = Dictionary(uniqueKeysWithValues: existingMappings.map { ($0.id, $0) })
        
        for mappingDTO in mappingDTOs {
            guard let mappingId = UUID(uuidString: mappingDTO.id),
                  let edgeId = UUID(uuidString: mappingDTO.edge_id),
                  let viewId = UUID(uuidString: mappingDTO.sld_view_id) else {
                AppLogger.log(.notice, "Edge-SLDView mapping has invalid UUID(s)", category: .sync)
                continue
            }
            
            if let existingMapping = mappingLookup[mappingId] {
                // Update existing mapping
                existingMapping.edge_id = edgeId
                existingMapping.sld_view_id = viewId
                existingMapping.points = mappingDTO.points
                existingMapping.algorithm = mappingDTO.algorithm
                existingMapping.is_deleted = mappingDTO.is_deleted
            } else {
                // Create new mapping
                let mapping = MappingEdgeSLDView(
                    id: mappingId,
                    edge_id: edgeId,
                    sld_view_id: viewId,
                    points: mappingDTO.points,
                    algorithm: mappingDTO.algorithm,
                    is_deleted: mappingDTO.is_deleted
                )
                context.insert(mapping)
                mappingLookup[mappingId] = mapping
            }
        }
        
        AppLogger.log(.info, "Processed \(mappingDTOs.count) edge-view mappings", category: .sync)
    }
    
    // MARK: - MAIN UPSERT HELPER: Location Hierarchy Processing Functions
    @MainActor
    private func processLocations(
        buildings: [(buildingDTO: BuildingDTO, sldId: UUID)],
        floors: [(floorDTO: FloorDTO, sldId: UUID)],
        rooms: [(roomDTO: RoomDTO, sldId: UUID)],
        sldLookup: [UUID: SLDV2],
        buildingLookup: inout [UUID: Building],
        floorLookup: inout [UUID: Floor],
        roomLookup: inout [UUID: Room],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nProcessing Location Hierarchy...", category: .sync)
        
        // Fetch existing entities
        let existingBuildings = try context.fetch(FetchDescriptor<Building>())
        let existingFloors = try context.fetch(FetchDescriptor<Floor>())
        let existingRooms = try context.fetch(FetchDescriptor<Room>())
        
        buildingLookup = Dictionary(uniqueKeysWithValues: existingBuildings.map { ($0.id, $0) })
        floorLookup = Dictionary(uniqueKeysWithValues: existingFloors.map { ($0.id, $0) })
        roomLookup = Dictionary(uniqueKeysWithValues: existingRooms.map { ($0.id, $0) })
        
        // Process buildings first (no dependencies)
        AppLogger.log(.debug, "🏢 [DEBUG-LOCATION-UPSERT] Processing \(buildings.count) buildings...", category: .sync)
        for (buildingDTO, sldId) in buildings {
            guard let sld = sldLookup[sldId],
                  let buildingId = UUID(uuidString: buildingDTO.id) else {
                AppLogger.log(.notice, "[DEBUG-LOCATION-UPSERT] Building \(buildingDTO.id): Can't find SLD \(sldId) or invalid UUID", category: .sync)
                continue
            }
            
            AppLogger.log(.debug, "🏢 [DEBUG-LOCATION-UPSERT] Building ID: \(buildingId), Name: \(buildingDTO.name), access_notes: \(buildingDTO.access_notes ?? "nil")", category: .sync)
            
            if let existingBuilding = buildingLookup[buildingId] {
                // Update existing building
                AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Updating existing building: \(buildingDTO.name)", category: .sync)
                existingBuilding.name = buildingDTO.name
                existingBuilding.is_deleted = buildingDTO.is_deleted
                existingBuilding.access_notes = buildingDTO.access_notes
                existingBuilding.sld = sld
                AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Updated building access_notes to: \(existingBuilding.access_notes ?? "nil")", category: .sync)
            } else {
                // Create new building
                AppLogger.log(.debug, "➕ [DEBUG-LOCATION-UPSERT] Creating new building: \(buildingDTO.name)", category: .sync)
                let building = Building(
                    id: buildingId,
                    name: buildingDTO.name,
                    sld: sld,
                    is_deleted: buildingDTO.is_deleted,
                    access_notes: buildingDTO.access_notes
                )
                context.insert(building)
                buildingLookup[buildingId] = building
                AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Created building with access_notes: \(building.access_notes ?? "nil")", category: .sync)
            }
        }
        AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Processed \(buildings.count) buildings", category: .sync)
        
        // Process floors (depend on buildings)
        AppLogger.log(.debug, "🏗️ [DEBUG-LOCATION-UPSERT] Processing \(floors.count) floors...", category: .sync)
        for (floorDTO, sldId) in floors {
            guard let _ = sldLookup[sldId],
                  let floorId = UUID(uuidString: floorDTO.id),
                  let buildingId = UUID(uuidString: floorDTO.building_id) else {
                AppLogger.log(.notice, "[DEBUG-LOCATION-UPSERT] Floor \(floorDTO.id): Can't find SLD \(sldId) or invalid UUID", category: .sync)
                continue
            }
            
            AppLogger.log(.debug, "🏗️ [DEBUG-LOCATION-UPSERT] Floor ID: \(floorId), Name: \(floorDTO.name), Building ID: \(buildingId), access_notes: \(floorDTO.access_notes ?? "nil")", category: .sync)
            
            // Find building
            let building = buildingLookup[buildingId]
            
            if let existingFloor = floorLookup[floorId] {
                // Update existing floor
                AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Updating existing floor: \(floorDTO.name)", category: .sync)
                existingFloor.name = floorDTO.name
                existingFloor.is_deleted = floorDTO.is_deleted
                existingFloor.access_notes = floorDTO.access_notes
                existingFloor.building = building
                AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Updated floor access_notes to: \(existingFloor.access_notes ?? "nil")", category: .sync)
            } else {
                // Create new floor
                AppLogger.log(.debug, "➕ [DEBUG-LOCATION-UPSERT] Creating new floor: \(floorDTO.name)", category: .sync)
                let floor = Floor(
                    id: floorId,
                    name: floorDTO.name,
                    building: building,
                    is_deleted: floorDTO.is_deleted,
                    access_notes: floorDTO.access_notes
                )
                context.insert(floor)
                floorLookup[floorId] = floor
                AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Created floor with access_notes: \(floor.access_notes ?? "nil")", category: .sync)
            }
        }
        AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Processed \(floors.count) floors", category: .sync)
        
        // Process rooms (depend on floors)
        AppLogger.log(.debug, "🚪 [DEBUG-LOCATION-UPSERT] Processing \(rooms.count) rooms...", category: .sync)
        for (roomDTO, sldId) in rooms {
            guard let sld = sldLookup[sldId],
                  let roomId = UUID(uuidString: roomDTO.id),
                  let floorId = UUID(uuidString: roomDTO.floor_id) else {
                AppLogger.log(.notice, "[DEBUG-LOCATION-UPSERT] Room \(roomDTO.id): Can't find SLD \(sldId) or invalid UUID", category: .sync)
                continue
            }
            
            AppLogger.log(.debug, "🚪 [DEBUG-LOCATION-UPSERT] Room ID: \(roomId), Name: \(roomDTO.name), Floor ID: \(floorId), access_notes: \(roomDTO.access_notes ?? "nil")", category: .sync)
            
            // Find floor
            let floor = floorLookup[floorId]
            
            if let existingRoom = roomLookup[roomId] {
                // Update existing room
                AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Updating existing room: \(roomDTO.name)", category: .sync)
                existingRoom.name = roomDTO.name
                existingRoom.is_deleted = roomDTO.is_deleted
                existingRoom.access_notes = roomDTO.access_notes
                existingRoom.floor = floor
                AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Updated room access_notes to: \(existingRoom.access_notes ?? "nil")", category: .sync)
            } else {
                // Create new room
                AppLogger.log(.debug, "➕ [DEBUG-LOCATION-UPSERT] Creating new room: \(roomDTO.name)", category: .sync)
                let room = Room(
                    id: roomId,
                    name: roomDTO.name,
                    floor: floor,
                    is_deleted: roomDTO.is_deleted,
                    access_notes: roomDTO.access_notes
                )
                context.insert(room)
                roomLookup[roomId] = room
                AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Created room with access_notes: \(room.access_notes ?? "nil")", category: .sync)
            }
        }
        AppLogger.log(.debug, "[DEBUG-LOCATION-UPSERT] Processed \(rooms.count) rooms", category: .sync)
    }
    
    @MainActor
    private func processPhotos(
        _ pendingPhotoDTOs: [(photoDTO: SLDDTOPhoto, sldId: UUID)],
        sldLookup: [UUID: SLDV2],
        nodeLookup: [UUID: NodeV2],
        tasksByID: [UUID: UserTask],
        issuesByID: [UUID: Issue],
        buildingsByID: [UUID: Building],
        floorsByID: [UUID: Floor],
        roomsByID: [UUID: Room],
        context: ModelContext
    ) async throws {
        
        let totalPhotos = pendingPhotoDTOs.count
        AppLogger.log(.info, "\nStep 8: Processing \(totalPhotos) Photos...", category: .sync)
        AppLogger.log(.debug, "DEBUG: Starting photo sync - Total photos to process: \(totalPhotos)", category: .sync)
        
        let existingPhotos = try context.fetch(FetchDescriptor<Photo>())
        AppLogger.log(.debug, "DEBUG: Existing photos in local DB: \(existingPhotos.count)", category: .sync)
        var photoLookup = Dictionary(uniqueKeysWithValues: existingPhotos.map { ($0.id, $0) })
        
        let batchSize = 1000
        var pendingInserts: [Photo] = []
        var pendingUpdates = 0
        
        pendingInserts.reserveCapacity(batchSize)
        
        for (index, (dto, sldId)) in pendingPhotoDTOs.enumerated() {
            
            // 🔹 Progress
            if index % progressUpdateInterval == 0 || index == totalPhotos - 1 {
                progressDetails.processedPhotos = index + 1
                updateStep(.photos, detail: "\(index + 1) of \(totalPhotos)")
                await Task.yield()
            }
            
            guard
                let entityID = dto.entity_id,
                let sld = sldLookup[sldId]
            else {
                AppLogger.log(.debug, "DEBUG: Skipping photo \(dto.id) - missing entity_id or sld", category: .sync)
                continue
            }
            
            // 🔹 Existing photo
            if let existing = photoLookup[dto.id] {
                AppLogger.log(.debug, "DEBUG: Found existing photo \(dto.id), updating...", category: .sync)
                updateExistingPhoto(
                    existing,
                    from: dto,
                    entityID: entityID,
                    sld: sld,
                    nodeLookup: nodeLookup,
                    tasksByID: tasksByID,
                    issuesByID: issuesByID,
                    buildingsByID: buildingsByID,
                    floorsByID: floorsByID,
                    roomsByID: roomsByID
                )
                pendingUpdates += 1
            } else {
                // 🔹 New photo
                AppLogger.log(.debug, "DEBUG: Creating new photo \(dto.id)...", category: .sync)
                guard let newPhoto = createPhoto(
                    from: dto,
                    entityID: entityID,
                    sld: sld,
                    nodeLookup: nodeLookup,
                    tasksByID: tasksByID,
                    issuesByID: issuesByID,
                    buildingsByID: buildingsByID,
                    floorsByID: floorsByID,
                    roomsByID: roomsByID
                ) else { continue }
                
                pendingInserts.append(newPhoto)
                photoLookup[newPhoto.id] = newPhoto
            }
            
            // 🔹 Batch save (inserts OR updates)
            if pendingInserts.count >= batchSize || pendingUpdates >= batchSize {
                try saveBatch(pendingInserts, context: context)
                pendingInserts.removeAll(keepingCapacity: true)
                pendingUpdates = 0
            }
        }
        
        // 🔹 Final save
        if !pendingInserts.isEmpty || pendingUpdates > 0 {
            try saveBatch(pendingInserts, context: context)
        }
    }
    
    
    //MARK: Batch Save (Single Responsibility)
    private func saveBatch(
        _ photos: [Photo],
        context: ModelContext
    ) throws {
        photos.forEach { context.insert($0) }
        try context.save()
    }
    
    //MARK: Update Existing Photo (No Duplication)
    private func updateExistingPhoto(
        _ photo: Photo,
        from dto: SLDDTOPhoto,
        entityID: UUID,
        sld: SLDV2,
        nodeLookup: [UUID: NodeV2],
        tasksByID: [UUID: UserTask],
        issuesByID: [UUID: Issue],
        buildingsByID: [UUID: Building],
        floorsByID: [UUID: Floor],
        roomsByID: [UUID: Room]
    ) {
        if !photo.upload_needed {
            photo.url = dto.url
            photo.type = dto.type
            photo.is_deleted = dto.is_deleted
        }
        
        // Always sync caption from server (server is source of truth for metadata)
        photo.caption = dto.caption
        
        // Debug: Log caption sync
        if let caption = dto.caption, !caption.isEmpty {
            AppLogger.log(.debug, "DEBUG: Updating photo with caption - ID: \(dto.id), Caption: '\(caption)'", category: .sync)
        } else {
            AppLogger.log(.debug, "DEBUG: Updating photo WITHOUT caption - ID: \(dto.id), Caption is nil or empty", category: .sync)
        }
        
        clearRelations(photo)
        assignRelations(
            photo,
            type: dto.type,
            entityID: entityID,
            nodeLookup: nodeLookup,
            tasksByID: tasksByID,
            issuesByID: issuesByID,
            buildingsByID: buildingsByID,
            floorsByID: floorsByID,
            roomsByID: roomsByID
        )
        
        photo.sld = sld
    }
    
    
    //MARK: Create New Photo (No Side Effects)
    private func createPhoto(
        from dto: SLDDTOPhoto,
        entityID: UUID,
        sld: SLDV2,
        nodeLookup: [UUID: NodeV2],
        tasksByID: [UUID: UserTask],
        issuesByID: [UUID: Issue],
        buildingsByID: [UUID: Building],
        floorsByID: [UUID: Floor],
        roomsByID: [UUID: Room]
    ) -> Photo? {
        
        let photo = Photo(
            id: dto.id,
            node: nil,
            userTask: nil,
            issue: nil,
            building: nil,
            floor: nil,
            room: nil,
            url: dto.url,
            type: dto.type,
            sld: sld,
            upload_needed: dto.upload_needed,
            local_filepath: dto.local_filepath,
            filename: dto.filename,
            is_deleted: dto.is_deleted,
            caption: dto.caption
        )
        
        // Debug: Log caption sync
        if let caption = dto.caption, !caption.isEmpty {
            AppLogger.log(.debug, "DEBUG: Creating photo with caption - ID: \(dto.id), Caption: '\(caption)'", category: .sync)
        } else {
            AppLogger.log(.debug, "DEBUG: Creating photo WITHOUT caption - ID: \(dto.id), Caption is nil or empty", category: .sync)
        }
        
        assignRelations(
            photo,
            type: dto.type,
            entityID: entityID,
            nodeLookup: nodeLookup,
            tasksByID: tasksByID,
            issuesByID: issuesByID,
            buildingsByID: buildingsByID,
            floorsByID: floorsByID,
            roomsByID: roomsByID
        )
        
        return photo
    }
    
    //MARK: Centralized Relationship Assignment
    /// Sets the appropriate relationship on the photo based on type.
    /// SwiftData automatically manages the inverse relationship via @Relationship(inverse:)
    private func assignRelations(
        _ photo: Photo,
        type: String,
        entityID: UUID,
        nodeLookup: [UUID: NodeV2],
        tasksByID: [UUID: UserTask],
        issuesByID: [UUID: Issue],
        buildingsByID: [UUID: Building],
        floorsByID: [UUID: Floor],
        roomsByID: [UUID: Room]
    ) {
        if type.hasPrefix("node_"), let node = nodeLookup[entityID] {
            photo.node = node
        } else if type.hasPrefix("task_"), let task = tasksByID[entityID] {
            photo.userTask = task
        } else if type == "issue", let issue = issuesByID[entityID] {
            photo.issue = issue
        } else if type == "building", let building = buildingsByID[entityID] {
            photo.building = building
        } else if type == "floor", let floor = floorsByID[entityID] {
            photo.floor = floor
        } else if type == "room", let room = roomsByID[entityID] {
            photo.room = room
        }
    }
    
    //MARK: Clear Relations (Prevents Orphan Links)
    private func clearRelations(_ photo: Photo) {
        photo.node = nil
        photo.userTask = nil
        photo.issue = nil
        photo.building = nil
        photo.floor = nil
        photo.room = nil
    }
    
    
    // MARK: - MAIN UPSERT HELPER: IR Photo Processing Function
    @MainActor
    private func processIRPhotos(
        _ pendingIRPhotoDTOs: [(irPhotoDTO: IRPhotoDTO, sldId: UUID)],
        sldLookup: [UUID: SLDV2],
        nodeLookup: [UUID: NodeV2],
        irSessionLookup: [UUID: IRSession],
        issuesByID: [UUID: Issue],
        context: ModelContext
    ) throws {
        AppLogger.log(.info, "\nStep 9: Processing \(pendingIRPhotoDTOs.count) IR Photos...", category: .sync)
        let existingIRPhotos = try context.fetch(FetchDescriptor<IRPhoto>())
        var irPhotoLookup = Dictionary(uniqueKeysWithValues: existingIRPhotos.map { ($0.id, $0) })
        
        for (irPhotoDTO, _) in pendingIRPhotoDTOs {
            // Get the required entities
            guard let node = nodeLookup[irPhotoDTO.node_id] else {
                // print("IR Photo \(irPhotoDTO.id) references non-existent node \(irPhotoDTO.node_id)")
                continue
            }
            
            guard let sld = sldLookup[irPhotoDTO.sld_id] else {
                // print("IR Photo \(irPhotoDTO.id) references non-existent SLD \(irPhotoDTO.sld_id)")
                continue
            }
            
            // Get the IR Session if provided (can be nil)
            let irSession: IRSession?
            if let sessionId = irPhotoDTO.ir_session_id {
                irSession = irSessionLookup[sessionId]
                if irSession == nil {
                    // print("IR Photo \(irPhotoDTO.id) references non-existent IR Session \(sessionId)")
                }
            } else {
                irSession = nil
            }
            
            // Get the Issue if provided (can be nil)
            let issue: Issue?
            if let issueId = irPhotoDTO.issue_id {
                issue = issuesByID[issueId]
                if issue == nil {
                    // print("IR Photo \(irPhotoDTO.id) references non-existent Issue \(issueId)")
                }
            } else {
                issue = nil
            }
            
            if let existingIRPhoto = irPhotoLookup[irPhotoDTO.id] {
                // Store old node for cleanup (non-optional)
                let oldNode = existingIRPhoto.node
                
                // Update existing IR photo
                existingIRPhoto.visual_photo_key = irPhotoDTO.visual_photo_key
                existingIRPhoto.ir_photo_key = irPhotoDTO.ir_photo_key
                existingIRPhoto.date_created = irPhotoDTO.date_created
                existingIRPhoto.is_deleted = irPhotoDTO.is_deleted
                existingIRPhoto.node = node
                existingIRPhoto.sld = sld
                // Mark as synced since it came from the server
                existingIRPhoto.isSynced = true
                
                // Clean up old node's array if node changed (no conditional binding needed)
                if oldNode.id != node.id {
                    oldNode.ir_photos.removeAll { $0.id == existingIRPhoto.id }
                }
                
                // Add to new node's array
                if !node.ir_photos.contains(where: { $0.id == existingIRPhoto.id }) {
                    node.ir_photos.append(existingIRPhoto)
                }
                
                // Similar for SLD if it's also non-optional
                let oldSLD = existingIRPhoto.sld
                if oldSLD.id != sld.id {
                    oldSLD.ir_photos.removeAll { $0.id == existingIRPhoto.id }
                }
                
                if !sld.ir_photos.contains(where: { $0.id == existingIRPhoto.id }) {
                    sld.ir_photos.append(existingIRPhoto)
                }
                
                // Handle session relationship (existing code is fine)
                existingIRPhoto.ir_session = irSession
                if let session = irSession {
                    if !session.ir_photos.contains(where: { $0.id == existingIRPhoto.id }) {
                        session.ir_photos.append(existingIRPhoto)
                    }
                } else {
                    for session in irSessionLookup.values {
                        session.ir_photos.removeAll { $0.id == existingIRPhoto.id }
                    }
                }
                
                // Handle issue relationship (existing code is fine)
                existingIRPhoto.issue = issue
                if let issueEntity = issue {
                    if !issueEntity.ir_photos.contains(where: { $0.id == existingIRPhoto.id }) {
                        issueEntity.ir_photos.append(existingIRPhoto)
                    }
                } else {
                    for issueEntity in issuesByID.values {
                        issueEntity.ir_photos.removeAll { $0.id == existingIRPhoto.id }
                    }
                }
                
                // print(" Updated IR Photo: \(irPhotoDTO.id)")
            } else {
                // Create new IR photo - session and issue can be nil
                let irPhoto = IRPhoto(
                    id: irPhotoDTO.id,
                    ir_session: irSession, // Can be nil
                    node: node,
                    sld: sld,
                    visual_photo_key: irPhotoDTO.visual_photo_key,
                    ir_photo_key: irPhotoDTO.ir_photo_key,
                    date_created: irPhotoDTO.date_created,
                    is_deleted: irPhotoDTO.is_deleted,
                    issue: issue // Can be nil
                )
                
                // Mark as synced since it came from the server
                irPhoto.isSynced = true
                
                context.insert(irPhoto)
                
                // Add to node's ir_photos array
                if !node.ir_photos.contains(where: { $0.id == irPhoto.id }) {
                    node.ir_photos.append(irPhoto)
                }
                
                // Add to session's ir_photos array if session exists
                if let session = irSession {
                    if !session.ir_photos.contains(where: { $0.id == irPhoto.id }) {
                        session.ir_photos.append(irPhoto)
                    }
                }
                
                // Add to issue's ir_photos array if issue exists
                if let issueEntity = issue {
                    if !issueEntity.ir_photos.contains(where: { $0.id == irPhoto.id }) {
                        issueEntity.ir_photos.append(irPhoto)
                    }
                }
                
                // print(" ✨ Created IR Photo: \(irPhotoDTO.id) for node '\(node.label)' (session: \(irSession?.name ?? "none"), issue: \(issue?.title ?? "none"))")
            }
        }
    }
    
    // MARK: - MAIN UPSERT HELPER: Save and Log Function
    @MainActor
    private func saveAll(context: ModelContext) throws {
        try context.save()
        AppLogger.log(.info, "\nCombined data upsert completed successfully!", category: .sync)
        
        // Debug: Check SWITCHBOARD after save
        let switchboardDescriptor = FetchDescriptor<NodeV2>(
            predicate: #Predicate<NodeV2> { node in
                node.label == "SWITCHBOARD"
            }
        )
        if let savedSwitchboard = try context.fetch(switchboardDescriptor).first {
            AppLogger.log(.debug, "After save - SWITCHBOARD has \(savedSwitchboard.node_tasks.count) tasks", category: .sync)
            AppLogger.log(.debug, "After save - SWITCHBOARD ID: \(savedSwitchboard.id)", category: .sync)
            AppLogger.log(.debug, "After save - SWITCHBOARD object: \(ObjectIdentifier(savedSwitchboard))", category: .sync)
            for task in savedSwitchboard.node_tasks {
                AppLogger.log(.info, "Task: '\(task.title)'", category: .sync)
            }
        }
        
        // Log final counts
        let finalSLDs = try context.fetch(FetchDescriptor<SLDV2>())
        let finalNodes = try context.fetch(FetchDescriptor<NodeV2>())
        let finalEdges = try context.fetch(FetchDescriptor<EdgeV2>())
        let finalTasks = try context.fetch(FetchDescriptor<UserTask>())
        let finalPhotos = try context.fetch(FetchDescriptor<Photo>())
        let finalForms = try context.fetch(FetchDescriptor<UserTaskForm>())
        let finalIRPhotos = try context.fetch(FetchDescriptor<IRPhoto>())
        let finalIRSessions = try context.fetch(FetchDescriptor<IRSession>())
        let finalQuotes = try context.fetch(FetchDescriptor<Quote>())
        let finalIssues = try context.fetch(FetchDescriptor<Issue>())
        
        AppLogger.log(.info, "\nFinal counts:", category: .sync)
        AppLogger.log(.info, "SLDs: \(finalSLDs.count)", category: .sync)
        AppLogger.log(.info, "Nodes: \(finalNodes.count)", category: .sync)
        AppLogger.log(.info, "Edges: \(finalEdges.count)", category: .sync)
        AppLogger.log(.info, "UserTasks: \(finalTasks.count)", category: .sync)
        AppLogger.log(.info, "Photos: \(finalPhotos.count)", category: .sync)
        AppLogger.log(.info, "UserTaskForms: \(finalForms.count)", category: .sync)
        AppLogger.log(.info, "IR Photos: \(finalIRPhotos.count)", category: .sync)
        AppLogger.log(.info, "IR Sessions: \(finalIRSessions.count)", category: .sync)
        AppLogger.log(.info, "Quotes: \(finalQuotes.count)", category: .sync)
        AppLogger.log(.info, "Issues: \(finalIssues.count)", category: .sync)
        
        // Log relationship counts
        AppLogger.log(.info, "\nRelationship counts:", category: .sync)
        var issueTaskCount = 0
        var taskSessionCount = 0
        var quoteTaskCount = 0
        var taskOwnerCount = 0
        var taskAssigneeCount = 0
        var tasksWithOwners = 0
        var tasksWithAssignees = 0
        var taskNodeCount = 0
        var taskFormCount = 0
        var tasksWithNodes = 0
        var tasksWithForms = 0
        
        for issue in finalIssues {
            issueTaskCount += issue.tasks.count
        }
        
        for session in finalIRSessions {
            taskSessionCount += session.user_tasks.count
        }
        
        for quote in finalQuotes {
            quoteTaskCount += quote.tasks.count
        }
        
        // Count User-Task relationships
        for task in finalTasks {
            let ownerCount = task.owned_by.count
            let assigneeCount = task.assigned_to.count
            
            taskOwnerCount += ownerCount
            taskAssigneeCount += assigneeCount
            
            if ownerCount > 0 {
                tasksWithOwners += 1
            }
            if assigneeCount > 0 {
                tasksWithAssignees += 1
            }
            
            // Count Task-Node and Task-Form relationships
            let nodeCount = task.linkedNodes.count
            taskNodeCount += nodeCount
            if nodeCount > 0 {
                tasksWithNodes += 1
            }
            
            let formCount = task.linkedForms.count
            taskFormCount += formCount
            if formCount > 0 {
                tasksWithForms += 1
            }
        }
        
        AppLogger.log(.info, "Issue-Task relationships: \(issueTaskCount)", category: .sync)
        AppLogger.log(.info, "Task-Session relationships: \(taskSessionCount)", category: .sync)
        AppLogger.log(.info, "Quote-Task relationships: \(quoteTaskCount)", category: .sync)
        AppLogger.log(.info, "User-Task (owner) relationships: \(taskOwnerCount)", category: .sync)
        AppLogger.log(.info, "User-Task (assignee) relationships: \(taskAssigneeCount)", category: .sync)
        AppLogger.log(.info, "Task-Node relationships: \(taskNodeCount)", category: .sync)
        AppLogger.log(.info, "Task-Form relationships: \(taskFormCount)", category: .sync)
        AppLogger.log(.info, "Tasks with owners: \(tasksWithOwners)/\(finalTasks.count)", category: .sync)
        AppLogger.log(.info, "Tasks with assignees: \(tasksWithAssignees)/\(finalTasks.count)", category: .sync)
        AppLogger.log(.info, "Tasks with nodes: \(tasksWithNodes)/\(finalTasks.count)", category: .sync)
        AppLogger.log(.info, "Tasks with forms: \(tasksWithForms)/\(finalTasks.count)", category: .sync)
        
        // Optional: Log detailed User-Task breakdown
        if taskOwnerCount > 0 || taskAssigneeCount > 0 {
            AppLogger.log(.info, "\n👥 User-Task mapping details:", category: .sync)
            
            // Count unique users across all tasks
            var uniqueOwners = Set<UUID>()
            var uniqueAssignees = Set<UUID>()
            
            for task in finalTasks {
                for ownerId in task.owned_by where ownerId != nil {
                    if let id = ownerId {
                        uniqueOwners.insert(id)
                    }
                }
                for assigneeId in task.assigned_to where assigneeId != nil {
                    if let id = assigneeId {
                        uniqueAssignees.insert(id)
                    }
                }
            }
            
            AppLogger.log(.info, "Unique owners: \(uniqueOwners.count)", category: .sync)
            AppLogger.log(.info, "Unique assignees: \(uniqueAssignees.count)", category: .sync)
            
            // Tasks with multiple owners/assignees
            let multiOwnerTasks = finalTasks.filter { $0.owned_by.count > 1 }.count
            let multiAssigneeTasks = finalTasks.filter { $0.assigned_to.count > 1 }.count
            
            if multiOwnerTasks > 0 {
                AppLogger.log(.info, "Tasks with multiple owners: \(multiOwnerTasks)", category: .sync)
            }
            if multiAssigneeTasks > 0 {
                AppLogger.log(.info, "Tasks with multiple assignees: \(multiAssigneeTasks)", category: .sync)
            }
        }
        
        AppLogger.log(.debug, "\n=== END COMBINED DATA UPSERT ===", category: .sync)
    }
    
    // MARK: - Main Upsert Function
    @MainActor
    func upsertSwiftDataObjects(
        sldDTO: SLDDTO,
        userTaskFormDTOs: [UserTaskFormDTO],
        into context: ModelContext
    ) async throws {
        AppLogger.log(.debug, "=== START COMBINED DATA UPSERT ===", category: .sync)
        
        // MARK: - 1. First, create all UserTaskForms (no dependencies)
        var formsByID: [UUID: UserTaskForm] = [:]
        try await processUserTaskForms(userTaskFormDTOs, formsByID: &formsByID, context: context)
        
        // MARK: - 1.5. Process location hierarchy BEFORE processing nodes (nodes depend on rooms)
        let totalLocations = progressDetails.totalBuildings + progressDetails.totalFloors + progressDetails.totalRooms
        if totalLocations > 0 {
            updateStep(.locations, detail: "0 of \(totalLocations)")
        }
        
        AppLogger.log(.info, "\nPre-processing SLD and extracting location data...", category: .sync)
        let existingSLDs = try context.fetch(FetchDescriptor<SLDV2>())
        var tempSldLookup = Dictionary(uniqueKeysWithValues: existingSLDs.map { ($0.id, $0) })
        let tempSld = processSLD(sldDTO, existingSLDs: existingSLDs, sldLookup: &tempSldLookup, context: context)
        
        // Create pending DTOs to extract locations
        var tempPendingDTOs = PendingDTOs()
        if let buildings = sldDTO.buildings {
            for buildingDTO in buildings {
                tempPendingDTOs.buildings.append((buildingDTO, tempSld.id))
            }
        }
        if let floors = sldDTO.floors {
            for floorDTO in floors {
                tempPendingDTOs.floors.append((floorDTO, tempSld.id))
            }
        }
        if let rooms = sldDTO.rooms {
            for roomDTO in rooms {
                tempPendingDTOs.rooms.append((roomDTO, tempSld.id))
            }
        }
        
        // Process locations if any exist
        var buildingLookup: [UUID: Building] = [:]
        var floorLookup: [UUID: Floor] = [:]
        var roomLookup: [UUID: Room] = [:]
        if !tempPendingDTOs.buildings.isEmpty || !tempPendingDTOs.floors.isEmpty || !tempPendingDTOs.rooms.isEmpty {
            try processLocations(
                buildings: tempPendingDTOs.buildings,
                floors: tempPendingDTOs.floors,
                rooms: tempPendingDTOs.rooms,
                sldLookup: tempSldLookup,
                buildingLookup: &buildingLookup,
                floorLookup: &floorLookup,
                roomLookup: &roomLookup,
                context: context
            )
            progressDetails.processedLocations = totalLocations
            updateOverallProgress()
        }
        
        // MARK: - 2. Process SLDs, Nodes, and Edges (but NOT Photos/Tasks/Quotes/Issues yet)
        updateStep(.nodes, detail: "0 of \(progressDetails.totalNodes)")
        
        // Use batch import for large datasets (>200 nodes+edges) to reduce main-thread blocking
        let useBatchImport = (progressDetails.totalNodes + progressDetails.totalEdges) > 200
        let (sldLookup, nodeLookup, _, pendingDTOs): ([UUID: SLDV2], [UUID: NodeV2], [UUID: EdgeV2], PendingDTOs)
        
        if useBatchImport {
            AppLogger.log(.info, "Using BATCH IMPORT for \(progressDetails.totalNodes) nodes + \(progressDetails.totalEdges) edges", category: .sync)
            (sldLookup, nodeLookup, _, pendingDTOs) = try await processCoreDataWithBatchImport(sldDTO, roomLookup: roomLookup, context: context)
        } else {
            AppLogger.log(.info, "Using standard import for \(progressDetails.totalNodes) nodes + \(progressDetails.totalEdges) edges", category: .sync)
            (sldLookup, nodeLookup, _, pendingDTOs) = try await processCoreData(sldDTO, roomLookup: roomLookup, context: context)
        }
        
        // MARK: - 3. Process IR Sessions (before IR Photos but after nodes)
        updateStep(.sessions, detail: "0 of \(progressDetails.totalIRSessions)")
        var irSessionLookup: [UUID: IRSession] = [:]
        try processSessions(pendingDTOs.irSessions, sldLookup: sldLookup, irSessionLookup: &irSessionLookup, context: context)
        progressDetails.processedIRSessions = pendingDTOs.irSessions.count
        updateOverallProgress()
        
        // MARK: - 4. Process UserTasks (now from SLD payload)
        updateStep(.tasks, detail: "0 of \(progressDetails.totalTasks)")
        var tasksByID: [UUID: UserTask] = [:]
        try processUserTasks(pendingDTOs.tasks, sldLookup: sldLookup, nodeLookup: nodeLookup, formsByID: formsByID, tasksByID: &tasksByID, context: context)
        progressDetails.processedTasks = pendingDTOs.tasks.count
        updateOverallProgress()
        
        // MARK: - 4.5. Process Form Instances
        if progressDetails.totalFormInstances > 0 {
            updateStep(.formInstances, detail: "0 of \(progressDetails.totalFormInstances)")
        }
        var formInstancesByID: [UUID: FormInstance] = [:]
        try processFormInstances(pendingDTOs.formInstances, formsByID: formsByID, formInstancesByID: &formInstancesByID, context: context)
        progressDetails.processedFormInstances = pendingDTOs.formInstances.count
        updateOverallProgress()

        // MARK: - 4.6 Process EG Forms + EG Form Instances (ZP-1723)
        try processEGFormInstances(
            egForms: pendingDTOs.egForms,
            egFormInstances: pendingDTOs.egFormInstances,
            taskEGFormInstanceMappings: pendingDTOs.mappings.taskEGFormInstance,
            egFormInstanceNodeMappings: pendingDTOs.mappings.egFormInstanceNode,
            tasksByID: tasksByID,
            nodeLookup: nodeLookup,
            context: context
        )

        // MARK: - 5. Process Quotes
        updateStep(.quotes, detail: "0 of \(progressDetails.totalQuotes)")
        var quotesByID: [UUID: Quote] = [:]
        try await processQuotes(pendingDTOs.quotes, sldLookup: sldLookup, quotesByID: &quotesByID, context: context)
        progressDetails.processedQuotes = pendingDTOs.quotes.count
        updateOverallProgress()
        
        // MARK: - 6. Process Issues
        updateStep(.issues, detail: "0 of \(progressDetails.totalIssues)")
        var issuesByID: [UUID: Issue] = [:]
        try await processIssues(pendingDTOs.issues, sldLookup: sldLookup, nodeLookup: nodeLookup, irSessionLookup: irSessionLookup, issuesByID: &issuesByID, context: context)
        progressDetails.processedIssues = pendingDTOs.issues.count
        updateOverallProgress()
        
        // MARK: - 7. Process Mappings (Issue-Task, Task-Session, Quote-Task, User-Task, Task-Node, Task-Form, Task-FormInstance, FormInstance-Node)
        updateStep(.mappings, detail: "0 of \(progressDetails.totalMappings)")
        processMappings(pendingDTOs.mappings, issuesByID: issuesByID, tasksByID: tasksByID, irSessionLookup: irSessionLookup, quotesByID: quotesByID, nodeLookup: nodeLookup, formsByID: formsByID, formInstancesByID: formInstancesByID)
        progressDetails.processedMappings = progressDetails.totalMappings
        updateOverallProgress()
        
        // MARK: - 8. Process Regular Photos (now that all entities exist)
        updateStep(.photos, detail: "0 of \(progressDetails.totalPhotos)")

        // Save main context before photos to commit all prior changes
        try context.save()

        // Process photos on the main context so entity relationships (photo↔node, photo↔task, etc.)
        // persist correctly. Using BackgroundImporter's separate ModelContext causes SwiftData to lose
        // inverse relationships across contexts — the same issue that required explicit re-linking for
        // sld.nodes/sld.edges after their background import.
        try await processPhotos(pendingDTOs.photos, sldLookup: sldLookup, nodeLookup: nodeLookup, tasksByID: tasksByID, issuesByID: issuesByID, buildingsByID: buildingLookup, floorsByID: floorLookup, roomsByID: roomLookup, context: context)
        progressDetails.processedPhotos = pendingDTOs.photos.count
        updateOverallProgress()
        
        // MARK: - 9. Process IR Photos
        updateStep(.irPhotos, detail: "0 of \(progressDetails.totalIRPhotos)")
        try processIRPhotos(pendingDTOs.irPhotos, sldLookup: sldLookup, nodeLookup: nodeLookup, irSessionLookup: irSessionLookup, issuesByID: issuesByID, context: context)
        progressDetails.processedIRPhotos = pendingDTOs.irPhotos.count
        updateOverallProgress()
        
        // MARK: - 9.5. Process Attachments
        if !pendingDTOs.attachments.isEmpty {
            AppLogger.log(.info, "📎 Processing \(pendingDTOs.attachments.count) attachments", category: .sync)
            if let modelContainer = AppStateManager.shared.modelContainer {
                let attachmentImporter = BackgroundImporter(modelContainer: modelContainer)
                _ = try await attachmentImporter.importAttachments(pendingDTOs.attachments) { processed, total in
                    await MainActor.run {
                        AppLogger.log(.info, "Processed \(processed) of \(total) attachments", category: .sync)
                    }
                }
                context.processPendingChanges()
            }
        }
        
        // MARK: - 9.6. Process Attachment-Node Mappings
        if !pendingDTOs.mappings.attachmentNode.isEmpty {
            AppLogger.log(.info, "Processing \(pendingDTOs.mappings.attachmentNode.count) attachment-node mappings", category: .sync)
            if let modelContainer = AppStateManager.shared.modelContainer {
                let mappingImporter = BackgroundImporter(modelContainer: modelContainer)
                _ = try await mappingImporter.importAttachmentNodeMappings(pendingDTOs.mappings.attachmentNode) { processed, total in
                    await MainActor.run {
                        AppLogger.log(.info, "Processed \(processed) of \(total) attachment-node mappings", category: .sync)
                    }
                }
                context.processPendingChanges()
            }
        }
        
        // MARK: - 10. Process Comments
        updateStep(.comments, detail: "0 of \(progressDetails.totalComments)")
        try processComments(pendingDTOs.comments, sldLookup: sldLookup, context: context)
        progressDetails.processedComments = pendingDTOs.comments.count
        updateOverallProgress()
        
        // MARK: - 10.5. Process SLD Views and Links
        var viewLookup: [UUID: SLDViewV2] = [:]
        var linkLookup: [UUID: SLDLinkV2] = [:]
        
        if !pendingDTOs.sldViews.isEmpty {
            AppLogger.log(.info, "🗺️ Processing \(pendingDTOs.sldViews.count) SLD views", category: .sync)
            try processSLDViews(pendingDTOs.sldViews, sldLookup: sldLookup, viewLookup: &viewLookup, context: context)
        }
        
        if !pendingDTOs.sldLinks.isEmpty {
            AppLogger.log(.info, "Processing \(pendingDTOs.sldLinks.count) SLD links", category: .sync)
            try processSLDLinks(pendingDTOs.sldLinks, viewLookup: viewLookup, linkLookup: &linkLookup, context: context)
        }
        
        // MARK: - 10.6. Process Node-View and Edge-View Mappings
        // Get edge lookup for edge-view mappings
        let existingEdges = try context.fetch(FetchDescriptor<EdgeV2>())
        let edgeLookup = Dictionary(uniqueKeysWithValues: existingEdges.map { ($0.id, $0) })
        
        if !pendingDTOs.mappings.nodeSLDView.isEmpty {
            AppLogger.log(.info, "Processing \(pendingDTOs.mappings.nodeSLDView.count) node-view mappings", category: .sync)
            try processNodeSLDViewMappings(pendingDTOs.mappings.nodeSLDView, nodeLookup: nodeLookup, viewLookup: viewLookup, context: context)
        }
        
        if !pendingDTOs.mappings.edgeSLDView.isEmpty {
            AppLogger.log(.info, "Processing \(pendingDTOs.mappings.edgeSLDView.count) edge-view mappings", category: .sync)
            try processEdgeSLDViewMappings(pendingDTOs.mappings.edgeSLDView, edgeLookup: edgeLookup, viewLookup: viewLookup, context: context)
        }
        
        // MARK: - 11. Save everything
        updateStep(.saving)
        try saveAll(context: context)
    }
}
