//
//  ContentViewModel.swift
//  Egalvanic PZ
//
//  Created by Assistant on 29/09/25.
//

import SwiftUI
import SwiftData
import Foundation
import Combine

@MainActor
class ContentViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var showingSplash = true
    @Published var availableSLDs: [SLDChoice] = []
    @Published var isLoadingSLDs = false
    @Published var isCheckingAuth = true
    @Published var showBiometricSetupAlert = false
    
    // MARK: - Private Properties
    private var savedCredentials: (email: String, password: String)?
    private let biometricService = BiometricAuthService.shared
    @ObservedObject var syncService = SLDSyncService.shared
    
    // MARK: - Environment Dependencies
    private var modelContext: ModelContext?
    private var networkState: NetworkState?
    private var appState: AppStateManager?
    private var sldService: SLDService?
    private var authService: AuthService?
    
    // MARK: - Computed Properties
    var biometricAlertTitle: String {
        AppStrings.Auth.enableBiometricForLogin(type: biometricService.biometricType.displayName)
    }

    var biometricAlertMessage: String {
        AppStrings.Auth.biometricQuickSignIn(type: biometricService.biometricType.displayName)
    }
    
    // MARK: - Dependency Injection
    func configure(
        modelContext: ModelContext,
        networkState: NetworkState,
        appState: AppStateManager,
        sldService: SLDService,
        authService: AuthService
    ) {
        self.modelContext = modelContext
        self.networkState = networkState
        self.appState = appState
        self.sldService = sldService
        self.authService = authService
    }
    
    // MARK: - Public Methods
    func onViewAppear() {
        // Set model context immediately when view appears
        networkState?.setModelContext(modelContext!)
        // Don't check auth state immediately - wait for splash to complete
    }
    
    func onSplashComplete() {
        showingSplash = false
        checkAuthState()
    }
    
    func onAuthenticationChange(_ isAuthenticated: Bool) {
        if isAuthenticated {
            handleAuthenticationSuccess()
        }
    }
    
    func onNetworkModeChange() {
        AppLogger.log(.info, "Network mode changed in ViewModel", category: .sync)
        networkState?.handleNetworkModeChange()
    }
    
    func handleLoginSuccess(email: String, password: String) {
        // Save credentials for potential biometric setup
        savedCredentials = (email, password)
        AppLogger.log(.debug, "Credentials saved for biometric setup", category: .auth)
    }
    
    func handleSLDSelection(_ sldId: UUID) {

        let siteName = availableSLDs.first(where: {$0.id == sldId})?.name

        Task {
            do {
                try await sldService?.switchToSLD(sldId, siteName: siteName, modelContext: modelContext!)

                // Check if we should show biometric setup after SLD selection
                if let _ = savedCredentials,
                   biometricService.canUseBiometric() && !biometricService.isBiometricAuthEnabled {
                    AppLogger.log(.debug, "Showing biometric setup alert after SLD selection", category: .auth)
                    showBiometricSetupAlert = true
                }
            } catch {
                AppLogger.log(.error, "Failed to switch SLD: \(error)", category: .sync)
            }
        }
    }

    func setupBiometric() async {
        guard let credentials = savedCredentials else {
            AppLogger.log(.notice, "No saved credentials available for biometric setup", category: .auth)
            return
        }

        do {
            AppLogger.log(.debug, "Setting up biometric auth with email: \(credentials.email)", category: .auth)
            try await biometricService.setupBiometricAuth(email: credentials.email, password: credentials.password)
            AppLogger.log(.info, "Biometric auth setup successful", category: .auth)

            // Clear saved credentials after successful setup
            savedCredentials = nil
        } catch {
            AppLogger.log(.error, "Failed to setup biometric auth: \(error)", category: .auth)
            // Clear saved credentials on failure too
            savedCredentials = nil
        }
    }
    
    func dismissNetworkAlert() {
        networkState?.showNetworkAlert = false
        networkState?.networkAlertTitle = AppStrings.Alerts.networkConnectionRequired
    }
    
    func dismissBiometricSetupAlert() {
        showBiometricSetupAlert = false
    }
    
    func enableBiometric() {
        Task {
            await setupBiometric()
        }
    }
    
    func ensureModelContextIsSet() {
        // Ensure model context is set when returning to main view
        if networkState?.modelContext == nil {
            networkState?.setModelContext(modelContext!)
        }
    }

    func cancelSiteSelection() {
        // User cancelled site selection, do nothing - stay on current site
        // The view will automatically show the main content since diagrams is not empty
    }
    
    // MARK: - Private Methods
    private func checkAuthState() {
        Task {
            // AuthService will check stored tokens on init
            await authService?.checkAuthStatus()
            
            if authService?.isAuthenticated == true {
                // Set the user ID from auth service
                if let userSub = authService?.currentUser?.sub,
                   let uuid = UUID(uuidString: userSub) {
                    appState?.userId = uuid
                    appState?.loggedIn = true
                }
                
                // Check if we have cached SLD data
                await handleCachedData()
            } else {
                isCheckingAuth = false
            }
        }
    }
    
    private func handleCachedData() async {
        guard let modelContext = modelContext else { return }

        // Create a fetch descriptor to check for existing diagrams
        let fetchDescriptor = FetchDescriptor<SLDV2>()
        let diagrams = try? modelContext.fetch(fetchDescriptor)

        // Check if activeSLDId was reset (user logged out)
        let defaultSLDId = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
        let currentActiveSLDId = appState?.activeSLDId ?? defaultSLDId

        // Simple check: if activeSLDId is null (default), show site selector
        // activeSLDId is persisted to UserDefaults when user selects a site,
        // and removed on logout - so checking this is sufficient
        let shouldShowSiteSelector = currentActiveSLDId == defaultSLDId

        if diagrams?.isEmpty != false {
            // No cached data but user is authenticated - potential reinstall scenario
            AppLogger.log(.notice, "No cached data found but user is authenticated", category: .sync)

            // This could indicate app was reinstalled with stale keychain data
            // For safety, verify authentication is valid by fetching SLDs
            // If this fails, the auth service will handle logout

            await fetchSLDChoices()

            // If after fetching we still have no SLDs and authentication failed,
            // the auth service should have cleared tokens and set isAuthenticated to false
            if availableSLDs.isEmpty && !(authService?.isAuthenticated ?? false) {
                AppLogger.log(.error, "Authentication invalid after reinstall - redirecting to login", category: .auth)
                isCheckingAuth = false
                return
            }
        } else if shouldShowSiteSelector {
            // ZP-1847 (Android-parity): no auto-restore based on pending
            // offline data. Queue items carry their own siteId in the
            // snapshot, so they flush correctly to whichever site
            // originated them regardless of which site the user picks
            // here. The selector just lets the user pick freely.
            AppLogger.log(.debug, "activeSLDId is null - showing site selector", category: .sync)
            isCheckingAuth = false
            await fetchSLDChoices()
        } else {
            // We have cached data AND a valid activeSLDId - go straight to main view
            // IMPORTANT: Set activeSLDId BEFORE isCheckingAuth to prevent UI flash
            if let firstDiagram = diagrams?.first {
                appState?.setActiveSLD(firstDiagram.id)
            }
            isCheckingAuth = false
        }
    }
    
    private func handleAuthenticationSuccess() {
        // Set user ID from auth service
        if let userSub = authService?.currentUser?.sub,
           let uuid = UUID(uuidString: userSub) {
            appState?.userId = uuid
            appState?.loggedIn = true
        }
        
        // Set model context after login
        networkState?.setModelContext(modelContext!)
        
        Task {
            await fetchSLDChoices()
        }
    }
    
    private func fetchSLDChoices() async {
        guard let appState = appState,
              let sldService = sldService,
              let modelContext = modelContext else { return }

        isLoadingSLDs = true
        isCheckingAuth = false

        let choices = await sldService.fetchAndUpsertSLDChoices(user_id: appState.userId, modelContext: modelContext)
        // Sort alphabetically by name
        availableSLDs = choices.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        isLoadingSLDs = false
    }
}
