import SwiftUI
import SwiftData

@main
struct EgalvanicApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    @StateObject private var appState = AppStateManager.shared
    @StateObject private var networkState = NetworkState.shared
    @StateObject private var networkReachability = NetworkReachability.shared
    @StateObject private var irSessionManager = IRSessionManager.shared
    @StateObject private var sldService = SLDService.shared
    @StateObject private var reportStore = ReportStore.shared
    @StateObject private var authService = AuthService.shared
    @StateObject private var languageManager = LanguageManager.shared

    @Environment(\.scenePhase) private var scenePhase

    /// Shared ModelContainer for background operations
    let sharedModelContainer: ModelContainer = {
        let schema = Schema([
            SLDV2.self,
            SLDChoice.self,
            NodeV2.self,
            EdgeV2.self,
            Photo.self,
            SyncQueueItem.self,
            SyncLog.self,
            NodeClass.self,
            NodeClassProperty.self,
            NodeIcon.self,
            NodeOrientation.self,
            NodeOrientationTerminal.self,
            NodeTerminal.self,
            NodeShortcut.self,
            UserTaskForm.self,
            FormInstance.self,
            EGForm.self,
            EGFormInstance.self,
            NodeProperty.self,
            IRSession.self,
            IRPhoto.self,
            SessionWorkBlock.self,
            Issue.self,
            Quote.self,
            IssueClass.self,
            IssueClassProperty.self,
            IssueProperty.self,
            IssueStatusHistory.self,
            SLDComment.self,
            Attachment.self,
            AttachmentNodeMapping.self,
            SLDViewV2.self,
            SLDLinkV2.self,
            MappingNodeSLDView.self,
            MappingEdgeSLDView.self,
            TestEquipment.self,
            EqpCategory.self,
            EqpItem.self,
            EqpStyle.self,
            EqpSensor.self,
            EqpPlug.self,
            EqpLtPickup.self,
            EqpLtDelay.self,
            EqpStPickup.self,
            EqpStDelay.self,
            EqpInstPickup.self,
            EqpCurveType.self,
            EqpFrame.self,
            EqpFrameAmp.self,
            EqpFrameSetting.self,
            EgEqpLibType.self,
            EgEqpLibSubtype.self,
            // ZP-2161 engineering enums
            EnumNodeVoltage.self,
            EnumNodeMainsType.self,
            EnumNodePhaseConfiguration.self,
            EnumNodeTripType.self,
            EnumDeviceRole.self,
            EnumSkmManufacturer.self,
            EnumCableSize.self,
            EnumCableConductorConfiguration.self,
            EnumCableConductorDescription.self,
            EnumCableDuctMaterial.self,
            EnumCableInsulationClass.self,
            EnumCableInsulationType.self,
            EnumCableInstallation.self,
            EnumBuswayAmpereRating.self,
            // ZP-2161 Phase 4a — SKM library headers
            EgDevLibRouting.self,
            SkmDeviceHeader.self,
            SkmTransformerModelHeader.self,
            SkmCableAcHeader.self,
            SkmCableDcHeader.self,
            SkmCableIeeeWHeader.self,
            SkmBusModelHeader.self,
            // ZP-2161 Phase 4b — SKM device deep tree
            SkmFrame.self,
            SkmSensor.self,
            SkmTripUnit.self,
            SkmTuSegment.self,
            SkmTransformerKvaEntry.self,
            // ZP-2420 — single SkmCableEntry table holds BOTH cable and
            // busway distribution rows; ``SkmMatcher.matchCableOrBusway``
            // partitions them by ``duct_material`` ∈ {"Busway","Bus"}.
            SkmCableEntry.self,
            // Cached lookup for the task creation procedure picker.
            Procedure.self
        ])
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()

    init() {
        // Make ModelContainer available for background operations
        AppStateManager.shared.setModelContainer(sharedModelContainer)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(networkState)
                .environmentObject(irSessionManager)
                .environmentObject(appState)
                .environmentObject(sldService)
                .environmentObject(reportStore)
                .environmentObject(authService)
                .environmentObject(languageManager)
                .onChange(of: scenePhase) { _, newPhase in
                    guard newPhase == .active else { return }
                    // Skip if the re-auth sheet is already up — the next refresh
                    // is guaranteed to fail and would just produce noise.
                    guard !authService.isSessionExpired else { return }
                    Task { await APIClient.shared.proactiveRefreshIfNeeded(for: nil) }
                }
        }
        .modelContainer(sharedModelContainer)
    }
}
