import SwiftUI
import SwiftData

// MARK: - Navigation Destinations
/// Type-safe navigation destinations for SiteTabView's NavigationStack
enum SiteNavDestination: Hashable {
    case irSessions
    case sessionDetail(IRSession)
    case roomDetail(session: IRSession, room: Room)
}

struct SiteTabView: View {
    @EnvironmentObject var languageManager: LanguageManager
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @Environment(\.modelContext) private var modelContext
    @Environment(\.scenePhase) private var scenePhase
    @EnvironmentObject private var sldService: SLDService
    @ObservedObject private var authService = AuthService.shared
    @StateObject private var versionCheckService = VersionCheckService.shared
    @StateObject private var legalAcceptanceService = LegalAcceptanceService.shared
    
    @Query(
        filter:  #Predicate<UserTask> { !$0.is_deleted },
        sort:    [SortDescriptor(\UserTask.title)]
    )
    private var tasks: [UserTask]
    
    @Query(
        filter: #Predicate<Issue> { !$0.is_deleted && $0.status == "Open" }
    )
    private var openIssues: [Issue]
    
    @Query(
        filter: #Predicate<Quote> { !$0.is_deleted && $0.status == "draft" }
    )
    private var draftQuotes: [Quote]
    
    @Query(
        filter: #Predicate<IRSession> { $0.active == true }
    )
    private var activeSessions: [IRSession]

    // Navigation path for type-safe, value-based navigation
    @State private var navigationPath = NavigationPath()

    @State private var selectedTask: UserTask?
    @State private var showingArcFlashDetail = false
    @State private var showingAllTasks = false
    @State private var showingQuotes = false
    @State private var showingIssues = false
    @State private var showingSiteSwitcher = false
    @State private var showingQuickInventory = false
    @State private var showingAgent = false
    @State private var showingDeactivateAlert = false
    @State private var showRefreshError = false
    @State private var refreshErrorMessage = ""
    @State private var showingLocations = false
    @State private var showingHome = false
    @State private var showQRScanner = false
    @State private var selectedNode: NodeV2?
    @State private var showDuplicateQRAlert = false
    @State private var duplicateNodes: [NodeV2] = []
    @State private var showNoAssetFoundAlert = false
    @State private var scannedQRCode = ""

    let diagram: SLDV2

    private var greetingText: String {
        if let givenName = authService.currentUser?.given_name {
            return AppStrings.Site.hiName(givenName)
        } else if let name = authService.currentUser?.name {
            return AppStrings.Site.hiName(name)
        } else {
            return AppStrings.Site.hi
        }
    }

    private var totalTaskCount: Int {
        tasks.filter { !$0.is_deleted && !$0.completed }.count
    }
    
    private var activeNodeCount: Int {
        diagram.nodes.filter { !$0.is_deleted }.count
    }
    
    private var activeEdgeCount: Int {
        diagram.edges.filter { !$0.is_deleted }.count
    }

    var body: some View {
        let cachedIncompleteCount: Int = {
            guard let activeSession = appState.activeSession else { return 0 }
            let sessionTaskIds = Set(activeSession.user_tasks.filter { !$0.is_deleted }.map { $0.id })
            var count = 0
            for node in activeSession.nodes where !node.is_deleted {
                for task in node.node_tasks where !task.is_deleted && sessionTaskIds.contains(task.id) {
                    if !(task.nodeCompletions[node.id.uuidString] ?? false) {
                        count += 1
                    }
                }
            }
            return count
        }()
        NavigationStack(path: $navigationPath) {
            ZStack {
                VStack(spacing: 0) {
                    // Main scrollable content
                    ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        // Welcome Section
                        VStack(alignment: .leading, spacing: 4) {
                            Text(greetingText)
                                .font(.subheadline)
                                .foregroundColor(.secondary)

                            Text(AppStrings.Site.welcomeTo(diagram.name))
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                        }
                        .padding(.horizontal)
                        .padding(.top, 16)

                        // Active Session Card - consistent height design.
                        // Gated by the ``ops-core`` company feature flag: when
                        // disabled, the card renders greyed-out and the tap is
                        // a no-op so technicians can still see the surface but
                        // can't enter the work-order flow.
                        let opsCoreEnabled = AuthService.shared.hasFeature("ops-core")
                        Button(action: {
                            guard opsCoreEnabled else { return }
                            if let activeSession = appState.activeSession {
                                navigationPath.append(SiteNavDestination.sessionDetail(activeSession))
                            } else {
                                navigationPath.append(SiteNavDestination.irSessions)
                            }
                        }) {
                            HStack {
                                HStack(spacing: 12) {
                                    ZStack {
                                        RoundedRectangle(cornerRadius: 6)
                                            .fill(appState.activeSession != nil ? Color.blue : Color.gray.opacity(0.4))
                                            .frame(width: 34, height: 24)
                                        Text(AppStrings.Forms.wo)
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(.white)
                                    }

                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(appState.activeSession != nil ? AppStrings.Site.activeWorkOrder : AppStrings.Site.noActiveWorkOrder)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                        Text(appState.activeSession?.name ?? AppStrings.Site.tapToSelectWorkOrder)
                                            .font(.subheadline)
                                            .fontWeight(appState.activeSession != nil ? .medium : .regular)
                                            .foregroundColor(.primary)
                                            .lineLimit(1)
                                            .truncationMode(.tail)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                }

                                Spacer()

                                if let activeSession = appState.activeSession {
                                    // Incomplete node-task mapping count (cached at top of body)
                                    if cachedIncompleteCount > 0 {
                                        Text("\(cachedIncompleteCount)")
                                            .font(.caption2)
                                            .fontWeight(.bold)
                                            .foregroundColor(.white)
                                            .frame(minWidth: 18, minHeight: 18)
                                            .padding(.horizontal, cachedIncompleteCount >= 100 ? 6 : 4)
                                            .padding(.vertical, 2)
                                            .background(Color.red)
                                            .clipShape(Capsule())
                                    }

                                    Button(action: {
                                        showingDeactivateAlert = true
                                    }) {
                                        Text(AppStrings.Sessions.end)
                                            .font(.caption)
                                            .fontWeight(.medium)
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 5)
                                            .background(Color.gray)
                                            .cornerRadius(6)
                                    }
                                } else {
                                    Image(systemName: "chevron.right")
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundColor(.secondary.opacity(0.5))
                                }
                            }
                            .padding()
                            .frame(height: 72) // Fixed height
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(16)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(Color.gray.opacity(0.2), lineWidth: 1)
                            )
                            .opacity(opsCoreEnabled ? 1.0 : 0.5)
                        }
                        .buttonStyle(PlainButtonStyle())
                        .disabled(!opsCoreEnabled)
                        .padding(.horizontal)

                        // Stats Cards
                        HStack(spacing: 16) {
                            StatCard(
                                title: AppStrings.Tabs.assets,
                                count: activeNodeCount,
                                icon: "square.grid.2x2",
                                color: .blue
                            )

                            StatCard(
                                title: AppStrings.Tabs.connections,
                                count: activeEdgeCount,
                                icon: "arrow.triangle.swap",
                                color: .green
                            )
                        }
                        .padding(.horizontal)
                        
                        // Quick Actions Section - All in one VStack
                        VStack(alignment: .leading, spacing: 12) {
                            Text(AppStrings.Site.quickActions)
                                .font(.headline)
                                .foregroundColor(.secondary)
                                .padding(.horizontal)
                            
                            // Grid layout for cards
                            LazyVGrid(columns: [
                                GridItem(.adaptive(minimum: 100, maximum: 150), spacing: 16)
                            ], spacing: 16) {
                                // Tasks Card — all tasks on the site; filtered
                                // inside the list view (Open default).
                                FeatureCard(
                                    title: "Tasks",
                                    icon: "checklist",
                                    iconColor: .blue,
                                    badge: totalTaskCount > 0 ? "\(totalTaskCount)" : nil,
                                    action: { showingAllTasks = true }
                                )
                                
                                // Issues Card
                                FeatureCard(
                                    title: AppStrings.Sessions.issues,
                                    icon: "exclamationmark.triangle",
                                    iconColor: .red,
                                    badge: openIssues.count > 0 ? "\(openIssues.count)" : nil,
                                    action: { showingIssues = true }
                                )
                                
                                // Arc Flash Card
                                FeatureCard(
                                    title: AppStrings.Site.arcFlash,
                                    icon: "bolt.shield",
                                    iconColor: .orange,
                                    action: { showingArcFlashDetail = true }
                                )
                                
                                // IR Sessions Card
                                // FeatureCard(
                                //     title: "Sessions",
                                //     icon: "camera.aperture",
                                //     iconColor: .purple,
                                //     badge: activeSessions.count > 0 ? "\(activeSessions.count)" : nil,
                                //     action: { showingIRSessions = true }
                                // )

                                // Quotes Card
                                // FeatureCard(
                                //     title: "Quotes",
                                //     icon: "doc.text",
                                //     iconColor: .green,
                                //     badge: draftQuotes.count > 0 ? "\(draftQuotes.count)" : nil,
                                //     action: { showingQuotes = true }
                                // )
                                
                                // Refresh Site Card
                                FeatureCard(
                                    title: AppStrings.Site.refresh,
                                    icon: "arrow.clockwise",
                                    iconColor: .blue,
                                    isDisabled: networkState.mode == .offline || sldService.isRefreshing,
                                    action: {
                                        Task {
                                            await refreshFromServer()
                                        }
                                    }
                                )

                                // Switch Site Card. ZP-1847: a pending queue
                                // alone never blocks site switching — queue
                                // items are self-contained snapshots and
                                // survive the switch. Only an *active* sync
                                // disables the button (so we don't wipe entity
                                // tables mid-upload).
                                // ZP-2173: legacy queue items (`userId == nil`)
                                // are NOT self-contained snapshots — they need
                                // the live entity tables, so block site switch
                                // until the user resolves them.
                                FeatureCard(
                                    title: AppStrings.Site.sites,
                                    icon: "building.2",
                                    iconColor: .indigo,
                                    badge: nil,
                                    isDisabled: networkState.isSyncing || networkState.hasLegacySyncItems,
                                    action: {
                                        showingSiteSwitcher = true
                                    }
                                )

                                // Locations Card
                                FeatureCard(
                                    title: AppStrings.Locations.locations,
                                    icon: "building.columns",
                                    iconColor: .cyan,
                                    action: {
                                        showingLocations = true
                                    }
                                )

                                // Schedule Card (offline shows cached blocks; online refreshes from API).
                                // ZP-2173: blocked while legacy queue items exist — the
                                // schedule view triggers a server refresh that overwrites
                                // local entity rows the legacy items still depend on.
                                // Also gated on the ``ops-core`` company feature flag.
                                FeatureCard(
                                    title: AppStrings.Home.schedule,
                                    icon: "calendar",
                                    iconColor: .mint,
                                    isDisabled: networkState.hasLegacySyncItems || !opsCoreEnabled,
                                    action: {
                                        guard opsCoreEnabled else { return }
                                        showingHome = true
                                    }
                                )

                                // EGPT Card - Hidden
                                // FeatureCard(
                                //     title: "Maral-AI",
                                //     icon: "bolt.horizontal",
                                //     iconColor: .teal,
                                //     isDisabled: networkState.mode == .offline,
                                //     action: {
                                //         showingAgent = true
                                //     }
                                // )
                            }
                            .padding(.horizontal)
                        }
                        .padding(.bottom, 20)
                    }
                }
                .background(Color(UIColor.systemBackground))
                }

                // Floating Action Buttons
                SiteFloatingButtons(
                    showQRScanner: $showQRScanner
                )
            }
            .overlay {
                // Refresh progress overlay
                RefreshProgressOverlay(syncService: SLDSyncService.shared)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarLeading) {
                    NetworkStatusButton()
                }

                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    // IR Session Picker
                    IRSessionPickerButton()
                        .environmentObject(appState)
                        .environment(\.modelContext, modelContext)
                }
            }
            // Navigation destinations for type-safe navigation
            .navigationDestination(for: SiteNavDestination.self) { destination in
                switch destination {
                case .irSessions:
                    IRSessionView(diagram: diagram, navigationPath: $navigationPath)
                        .environmentObject(networkState)
                        .environmentObject(appState)
                        .environmentObject(sldService)
                case .sessionDetail(let session):
                    IRSessionDetailView(session: session, navigationPath: $navigationPath)
                        .environmentObject(networkState)
                        .environmentObject(appState)
                        .environmentObject(sldService)
                case .roomDetail(let session, let room):
                    SessionRoomDetailView(session: session, room: room, isModal: false)
                        .environment(\.modelContext, modelContext)
                        .environmentObject(appState)
                        .environmentObject(sldService)
                        .environmentObject(networkState)
                }
            }
        }
        .onAppear {
            Task {
                await versionCheckService.checkForUpdate()
                await legalAcceptanceService.checkPendingAcceptance()
            }
        }
        .onChange(of: navigationPath) { _, _ in
            checkForUpdates()
        }
        .onChange(of: scenePhase) { oldPhase, newPhase in
            if newPhase == .active {
                Task {
                    await versionCheckService.checkForUpdate()
                    await legalAcceptanceService.checkPendingAcceptance()
                }
            }
        }
        .fullScreenCover(item: $selectedTask, onDismiss: checkForUpdates) { task in
            TaskDetailView(task: task)
        }
        .fullScreenCover(isPresented: $showingArcFlashDetail, onDismiss: checkForUpdates) {
            NavigationView {
                ArcFlashCompletionView(diagram: diagram)
                    .navigationBarItems(leading: Button(AppStrings.Common.done) {
                        showingArcFlashDetail = false
                    })
            }
        }
        .fullScreenCover(isPresented: $showingAgent, onDismiss: checkForUpdates) {
            AgentWebAppContainerView(sld: diagram)
        }
        .fullScreenCover(isPresented: $showingQuickInventory, onDismiss: checkForUpdates) {
            QuickInventoryView(sld: diagram, onComplete: {
            })
        }
        .fullScreenCover(isPresented: $showingLocations, onDismiss: checkForUpdates) {
            StandaloneLocationHierarchyView(diagram: diagram)
                .environmentObject(networkState)
                .environmentObject(appState)
                .environmentObject(sldService)
                .environment(\.modelContext, modelContext)
        }
        .fullScreenCover(isPresented: $showingHome, onDismiss: checkForUpdates) {
            HomeView()
                .environmentObject(networkState)
                .environmentObject(appState)
        }
        .fullScreenCover(isPresented: $showingAllTasks, onDismiss: checkForUpdates) {
            UnifiedTaskListView()
                .environmentObject(networkState)
                .environmentObject(appState)
        }
        .fullScreenCover(isPresented: $showingQuotes, onDismiss: checkForUpdates) {
            QuotesView(diagram: diagram)
                .environmentObject(networkState)
        }
        .fullScreenCover(isPresented: $showingIssues, onDismiss: checkForUpdates) {
            UnifiedIssueListView()
                .environmentObject(networkState)
                .environmentObject(appState)
        }
        .fullScreenCover(isPresented: $showingSiteSwitcher, onDismiss: checkForUpdates) {
            SiteSwitcherView()
                .environmentObject(networkState)
                .environmentObject(appState)
                .environment(\.modelContext, modelContext)
        }
        .alert(AppStrings.Sessions.endWorkOrderSession, isPresented: $showingDeactivateAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Sessions.endSession, role: .destructive) {
                if let session = appState.activeSession {
                    appState.setActiveSession(nil)
                }
            }
        } message: {
            if let session = appState.activeSession {
                Text(AppStrings.Sessions.endSessionConfirm(name: session.name))
            }
        }
        .alert(AppStrings.Sessions.refreshFailed, isPresented: $showRefreshError) {
            Button(AppStrings.Common.ok, role: .cancel) { }
            if networkState.mode == .offline {
                Button(AppStrings.Sessions.goOnline) {
                    networkState.toggleMode()
                    Task {
                        await refreshFromServer()
                    }
                }
            }
        } message: {
            Text(refreshErrorMessage)
        }
        // QR Scanner fullscreen
        .fullScreenCover(isPresented: $showQRScanner, onDismiss: checkForUpdates) {
            QRCodeScannerView(scannedCode: .constant("")) { scannedCode in
                // Search for all nodes with matching QR code
                let matchingNodes = diagram.nodes.filter {
                    !$0.is_deleted && $0.qr_code?.lowercased() == scannedCode.lowercased()
                }

                if matchingNodes.count == 1 {
                    // Single match - open directly
                    selectedNode = matchingNodes[0]
                } else if matchingNodes.count > 1 {
                    // Multiple matches - show alert to choose
                    duplicateNodes = matchingNodes
                    showDuplicateQRAlert = true
                } else {
                    // No match - show alert
                    scannedQRCode = scannedCode
                    showNoAssetFoundAlert = true
                }
            }
        }
        .alert(AppStrings.Locations.assetNotFound, isPresented: $showNoAssetFoundAlert) {
            Button(AppStrings.Common.ok, role: .cancel) { }
        } message: {
            Text(AppStrings.Locations.assetNotFoundMessage(scannedQRCode))
        }
        .alert(AppStrings.AssetsExtra.multipleAssetsFound, isPresented: $showDuplicateQRAlert) {
            ForEach(duplicateNodes.prefix(5), id: \.id) { node in
                Button(node.label) {
                    selectedNode = node
                }
            }
            if duplicateNodes.count > 5 {
                Button(AppStrings.Site.showFirstFiveOnly) {
                    // Just dismiss - already showing first 5
                    duplicateNodes = []
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) {
                duplicateNodes = []
            }
        } message: {
            Text(AppStrings.AssetsExtra.foundAssetsWithQR(duplicateNodes.count))
        }
        // Node detail view
        .fullScreenCover(item: $selectedNode, onDismiss: checkForUpdates) { node in
            EditNodeDetailViewV3(node: node, sld: diagram)
        }
        // Optional Update Alert
        .alert(AppStrings.Site.updateAvailable, isPresented: $versionCheckService.showOptionalUpdate) {
            Button(AppStrings.Site.update) {
                versionCheckService.openAppStore()
            }
            Button(AppStrings.Site.remindMeLater, role: .cancel) {
                versionCheckService.dismissOptionalUpdate()
            }
        } message: {
            Text(versionCheckService.updateInfo?.message ?? AppStrings.Site.newVersionAvailable)
        }
        // Force Update Sync Overlay
        .overlay {
            if versionCheckService.showForceUpdateSync {
                ForceUpdateSyncView(versionCheckService: versionCheckService)
                    .environmentObject(networkState)
                    .transition(.opacity)
                    .animation(.easeInOut(duration: 0.3), value: versionCheckService.showForceUpdateSync)
            }
        }
        // Legal Acceptance Sheet (lower priority than update dialogs)
        .sheet(isPresented: Binding(
            get: {
                legalAcceptanceService.showLegalAcceptanceDialog
                    && !versionCheckService.showForceUpdate
                    && !versionCheckService.showForceUpdateSync
                    && !versionCheckService.showOptionalUpdate
            },
            set: { newValue in
                if !newValue { legalAcceptanceService.showLegalAcceptanceDialog = false }
            }
        )) {
            LegalAcceptanceDialogView(legalService: legalAcceptanceService)
                .presentationDetents([.medium])
                .interactiveDismissDisabled()
        }
        // Force Update Alert
        .alert(AppStrings.Site.updateRequired, isPresented: $versionCheckService.showForceUpdate) {
            Button(AppStrings.Site.update) {
                versionCheckService.openAppStore()
            }
        } message: {
            Text(versionCheckService.updateInfo?.message ?? AppStrings.Site.pleaseUpdateToContinue)
        }
    }

    private func refreshFromServer() async {
        do {
            try await sldService.refreshSLD(modelContext: modelContext)
        } catch {
            // Don't show error alert for auth errors — the re-auth sheet handles those
            guard !AuthError.isAuthError(error) else { return }
            await MainActor.run {
                if let sldError = error as? SLDServiceError {
                    refreshErrorMessage = sldError.recoverySuggestion ?? sldError.localizedDescription
                } else {
                    refreshErrorMessage = "Failed to refresh data: \(error.localizedDescription)"
                }
                showRefreshError = true
            }
        }
    }

    private func checkForUpdates() {
        Task {
            await versionCheckService.checkForUpdate()
        }
    }
}

// MARK: - Supporting Views

struct StatCard: View {
    let title: String
    let count: Int
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                
                Spacer()
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text("\(count)")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                Text(title)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(16)
    }
}

struct FeatureCard: View {
    let title: String
    let icon: String
    let iconColor: Color
    let badge: String?
    let isDisabled: Bool
    let action: () -> Void
    
    init(title: String, icon: String, iconColor: Color, badge: String? = nil, isDisabled: Bool = false, action: @escaping () -> Void) {
        self.title = title
        self.icon = icon
        self.iconColor = iconColor
        self.badge = badge
        self.isDisabled = isDisabled
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 12) {
                ZStack(alignment: .topTrailing) {
                    ZStack {
                        Circle()
                            .fill(iconColor.opacity(isDisabled ? 0.05 : 0.15))
                            .frame(width: 56, height: 56)
                        
                        Image(systemName: icon)
                            .font(.title2)
                            .foregroundColor(isDisabled ? .gray : iconColor)
                    }
                    
                    if let badge = badge {
                        Text(badge)
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.red)
                            .clipShape(Capsule())
                            .offset(x: 8, y: -4)
                    }
                }
                
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(isDisabled ? .gray : .primary)
                    .multilineTextAlignment(.center)
            }
            .frame(width: 110, height: 110) // Fixed size to ensure consistency
            .background(Color(UIColor.tertiarySystemBackground).opacity(isDisabled ? 0.5 : 1))
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color.gray.opacity(isDisabled ? 0.1 : 0.2), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(isDisabled ? 0.02 : 0.05), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(isDisabled)
    }
}

// MARK: - Site Switcher View
struct SiteSwitcherView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var sldService: SLDService
    @ObservedObject private var syncService = SLDSyncService.shared

    @State private var availableSLDs: [SLDChoice] = []
    @State private var isLoadingSLDs = false

    // Filter out deleted SLDs and sort alphabetically
    private var activeSLDs: [SLDChoice] {
        availableSLDs
            .filter { !$0.is_deleted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }
    
    var body: some View {
        ZStack {
            // Use the redesigned SLDSelectorView
            SLDSelectorView(
                availableSLDs: activeSLDs,
                isLoading: isLoadingSLDs,
                onSelection: { sldId in
                    Task {
                        await handleSiteSwitch(to: sldId)
                    }
                },
                onCancel: {
                    dismiss()
                },
                activeSLDId: appState.activeSLDId
            )

            // Show loading overlay when switching sites OR if there's an error
            if sldService.isSwitching || syncService.syncErrorMessage != nil {
                SLDLoadingView(
                    syncService: syncService,
                    sldService: sldService,
                    siteName: syncService.currentSiteName,
                    onReturnToSiteSelection: {
                        // Clear error and stay on site switcher
                        syncService.syncErrorMessage = nil
                        syncService.currentSiteName = nil
                        dismiss()
                    }
                )
                .background(Color(UIColor.systemBackground))
            }
        }
        .task {
            await fetchSLDChoices()
        }
    }
    
    private func fetchSLDChoices() async {
        isLoadingSLDs = true
        
        availableSLDs = await sldService.fetchAndUpsertSLDChoices(user_id: appState.userId, modelContext: modelContext)
        isLoadingSLDs = false
    }
    
    private func handleSiteSwitch(to sldId: UUID) async {
        guard sldId != appState.activeSLDId else { return }

        // Find the site name for display during loading
        let siteName = availableSLDs.first(where: { $0.id == sldId })?.name

        do {
            try await sldService.switchToSLD(sldId, siteName: siteName, modelContext: modelContext)
            dismiss()
        } catch {
            AppLogger.log(.error, "Failed to switch site: \(error)", category: .ui)
            // Error is already captured in syncService.syncErrorMessage
            // The SLDLoadingView overlay will display it with a return button
        }
    }
}

// MARK: - Floating Action Buttons

private struct SiteFloatingButtons: View {
    @Binding var showQRScanner: Bool

    var body: some View {
        VStack {
            Spacer()
            HStack {
                Spacer()
                VStack(spacing: 12) {
                    if DevRevService.shared.isConfigured {
                        Button {
                            Task { await DevRevService.shared.showSupport() }
                        } label: {
                            DevRevSupportButtonLabel()
                        }
                    }

                    Button {
                        showQRScanner = true
                    } label: {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(Color.blue)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                    }
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
        }
    }
}

private struct DevRevSupportButtonLabel: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Image("Logo")
            .resizable()
            .scaledToFit()
            .frame(width: 30, height: 30)
            .frame(width: 48, height: 48)
            .background(colorScheme == .dark ? Color(UIColor.secondarySystemBackground) : Color(UIColor.tertiarySystemBackground))
            .clipShape(Circle())
            .overlay(
                Circle()
                    .stroke(Color.gray.opacity(colorScheme == .dark ? 0.4 : 0.2), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}
