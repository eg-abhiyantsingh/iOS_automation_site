import SwiftUI
import SwiftData
import Foundation

// MARK: - Main ContentView
struct ContentView: View {
    @Query private var diagrams: [SLDV2]
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var sldService: SLDService
    @EnvironmentObject var authService: AuthService
    @EnvironmentObject var languageManager: LanguageManager

    @StateObject private var viewModel = ContentViewModel()

    // Re-authentication state
    @State private var showReAuthSheet = false

    // Show Schedule after first login (before selecting a site)
    @State private var showingScheduleAfterLogin = false

    var body: some View {
        Group {
            if viewModel.showingSplash {
                splashView
            } else if viewModel.isCheckingAuth {
                loadingView
            } else if !authService.isAuthenticated {
                loginView
                    .id(languageManager.currentLanguage.rawValue)
            } else {
                // Wrap authenticated content with PlatformAccessGate
                PlatformAccessGate {
                    authenticatedContent
                }
            }
        }
        .onChange(of: authService.isAuthenticated) { oldValue, isAuthenticated in
            viewModel.onAuthenticationChange(isAuthenticated)
            // Fresh login lands on the site selector instead of the schedule.
            // The schedule-after-login shortcut + its render branch are left
            // intact below so we can revive it by flipping this back to
            // ``true``. (Session-expiration re-auth keeps ``isAuthenticated``
            // true throughout, so it never triggers this onChange handler.)
            if isAuthenticated && !oldValue {
                showingScheduleAfterLogin = false
            }
        }
        .onChange(of: networkState.mode) { oldValue, newValue in
            AppLogger.log(.info, "Network mode changed: \(oldValue) -> \(newValue)", category: .sync)
            viewModel.onNetworkModeChange()
        }
        .alert(networkState.networkAlertTitle, isPresented: $networkState.showNetworkAlert) {
            Button(AppStrings.Common.ok) {
                viewModel.dismissNetworkAlert()
            }
        } message: {
            Text(networkState.networkAlertMessage)
        }
        .alert(viewModel.biometricAlertTitle, isPresented: $viewModel.showBiometricSetupAlert) {
            Button(AppStrings.Common.enable) {
                viewModel.enableBiometric()
            }
            Button(AppStrings.Common.notNow, role: .cancel) {
                viewModel.dismissBiometricSetupAlert()
            }
        } message: {
            Text(viewModel.biometricAlertMessage)
        }
        .onChange(of: authService.isSessionExpired) { _, isExpired in
            // Show re-auth sheet when session expires
            if isExpired {
                showReAuthSheet = true
            }
        }
        .sheet(isPresented: $showReAuthSheet, onDismiss: {
            // Guard against unexpected dismissal (e.g., SwiftUI dismissing the sheet
            // during rapid view hierarchy changes from concurrent API failures).
            // If the session is still expired, re-present the sheet.
            if authService.isSessionExpired {
                DispatchQueue.main.async {
                    showReAuthSheet = true
                }
            }
        }) {
            ReAuthenticationView(
                onReAuthSuccess: {
                    // Re-authentication successful - dismiss sheet
                    showReAuthSheet = false
                },
                onSwitchUser: {
                    // User chose to switch account - dismiss sheet
                    // AuthService.performSwitchUser() already handles logout
                    showReAuthSheet = false
                }
            )
            .interactiveDismissDisabled(true) // Prevent swipe to dismiss
            .presentationDetents([.large]) // Full screen for better UX
        }
        .onAppear {
            configureViewModel()
            viewModel.onViewAppear()
            // Check for persisted session expired state on appear
            if authService.isSessionExpired {
                showReAuthSheet = true
            }
        }
    }
    
    // MARK: - View Components
    @ViewBuilder
    private var splashView: some View {
        SplashScreenView {
            viewModel.onSplashComplete()
        }
    }
    
    @ViewBuilder
    private var loadingView: some View {
        VStack {
            ProgressView()
            Text(AppStrings.Common.loading)
                .foregroundStyle(.secondary)
        }
    }
    
    @ViewBuilder
    private var loginView: some View {
        CompanyCodeView(onLoginSuccess: viewModel.handleLoginSuccess)
            .environmentObject(authService)
            .environmentObject(appState)
    }
    
    @ViewBuilder
    private var sldLoadingView: some View {
        SLDLoadingView(
            syncService: viewModel.syncService,
            sldService: SLDService.shared,
            siteName: viewModel.syncService.currentSiteName,
            onReturnToSiteSelection: {
                // Reset state and return to site selector
                viewModel.syncService.syncErrorMessage = nil
                viewModel.syncService.currentSiteName = nil
                viewModel.syncService.isSiteSwitchMode = false
                // No need to set showingSLDSelector - diagrams.isEmpty will handle it
            }
        )
    }
    
    @ViewBuilder
    private var sldSelectorView: some View {
        SLDSelectorView(
            availableSLDs: viewModel.availableSLDs,
            isLoading: viewModel.isLoadingSLDs,
            onSelection: viewModel.handleSLDSelection,
            onCancel: diagrams.isEmpty ? nil : {
                // User can cancel back to current site if one is loaded
                viewModel.cancelSiteSelection()
            }
        )
    }
    
    @ViewBuilder
    private var authenticatedContent: some View {
        Group {
            if viewModel.syncService.isSiteSwitchMode && (viewModel.syncService.isSyncing || viewModel.syncService.syncErrorMessage != nil || SLDService.shared.isClearing) {
                // Show loading screen ONLY during site switches (not during pull-to-refresh)
                sldLoadingView
            } else if showingScheduleAfterLogin && (diagrams.isEmpty || appState.activeSLDId == UUID(uuidString: "00000000-0000-0000-0000-000000000000")!) {
                // Show Schedule first after login (before selecting a site)
                scheduleAfterLoginView
            } else if diagrams.isEmpty || appState.activeSLDId == UUID(uuidString: "00000000-0000-0000-0000-000000000000")! {
                sldSelectorView
            } else {
                mainContentView
            }
        }
    }

    @ViewBuilder
    private var scheduleAfterLoginView: some View {
        HomeView(
            onLogout: {
                // Back button triggers logout
                Task {
                    await authService.logout()
                    showingScheduleAfterLogin = false
                }
            },
            onSiteSelected: { sldId, siteName in
                // Check if this is a "View Sites" request (empty UUID)
                if sldId == UUID() {
                    // User wants to see site selector
                    showingScheduleAfterLogin = false
                } else {
                    // User tapped a work block and chose to switch to that site
                    showingScheduleAfterLogin = false
                    Task {
                        do {
                            try await sldService.switchToSLD(sldId, siteName: siteName, modelContext: modelContext)
                        } catch {
                            AppLogger.log(.error, "Failed to switch SLD: \(error)", category: .sync)
                        }
                    }
                }
            }
        )
    }

    @ViewBuilder
    private var mainContentView: some View {
        if let firstDiagram = diagrams.first {
            MainTabView(diagram: firstDiagram)
                .onAppear {
                    viewModel.ensureModelContextIsSet()
                }
        }
    }

    // MARK: - Private Methods
    private func configureViewModel() {
        viewModel.configure(
            modelContext: modelContext,
            networkState: networkState,
            appState: appState,
            sldService: sldService,
            authService: authService
        )
    }
}
