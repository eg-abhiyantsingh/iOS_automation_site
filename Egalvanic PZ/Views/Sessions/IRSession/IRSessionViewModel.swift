//
//  IRSessionViewModel.swift
//  SwiftDataTutorial
//
//  Created by Claude on 1/5/26.
//

import SwiftUI
import SwiftData

@MainActor
class IRSessionViewModel: ObservableObject {

    // MARK: - Published Properties (UI State)
    @Published var selectedSession: IRSession?
    @Published var showingSessionDetail: Bool = false
    @Published var showingDeactivateAlert: Bool = false
    @Published var sessionToDeactivate: IRSession?
    @Published var showingActivateAlert: Bool = false
    @Published var sessionToActivate: IRSession?
    @Published var showAllSessions: Bool = false
    @Published var showingCreateSession: Bool = false
    @Published var showRefreshError: Bool = false
    @Published var refreshErrorMessage: String = ""

    // MARK: - Private Properties
    private var modelContext: ModelContext?
    private var networkState: NetworkState?
    private var appState: AppStateManager?
    private var sldService: SLDService?
    let diagram: SLDV2

    // MARK: - Initialization
    init(diagram: SLDV2) {
        self.diagram = diagram
    }

    // MARK: - Configuration
    func configure(
        modelContext: ModelContext,
        networkState: NetworkState,
        appState: AppStateManager,
        sldService: SLDService
    ) {
        self.modelContext = modelContext
        self.networkState = networkState
        self.appState = appState
        self.sldService = sldService
    }

    // MARK: - Computed Properties (Session Filtering)

    func nonDeletedSessions(_ sessions: [IRSession]) -> [IRSession] {
        sessions.filter { !$0.is_deleted }
    }

    func activeSessions(_ sessions: [IRSession]) -> [IRSession] {
        nonDeletedSessions(sessions).filter { $0.active }
    }

    func inactiveSessions(_ sessions: [IRSession]) -> [IRSession] {
        nonDeletedSessions(sessions).filter { !$0.active }
    }

    func displayedSessions(_ sessions: [IRSession]) -> [IRSession] {
        showAllSessions ? nonDeletedSessions(sessions) : activeSessions(sessions)
    }

    func totalPhotoCount(_ sessions: [IRSession]) -> Int {
        nonDeletedSessions(sessions).reduce(0) { sum, session in
            sum + session.ir_photos.filter { !$0.is_deleted }.count
        }
    }

    // MARK: - Convenience Properties

    var isRefreshing: Bool {
        sldService?.isRefreshing ?? false
    }

    var isOffline: Bool {
        networkState?.mode == .offline
    }

    var hasActiveSession: Bool {
        appState?.activeSession != nil
    }

    var activeSession: IRSession? {
        appState?.activeSession
    }

    // MARK: - Public Methods

    func activateSession(_ session: IRSession) {
        appState?.setActiveSession(session)
    }

    func requestActivation(_ session: IRSession) {
        sessionToActivate = session
        showingActivateAlert = true
    }

    func confirmActivation() {
        if let session = sessionToActivate {
            activateSession(session)
        }
        sessionToActivate = nil
    }

    func cancelActivation() {
        sessionToActivate = nil
    }

    func requestDeactivation(_ session: IRSession) {
        sessionToDeactivate = session
        showingDeactivateAlert = true
    }

    func confirmDeactivation() {
        if let session = sessionToDeactivate {
            deactivateSession(session)
        }
    }

    func cancelDeactivation() {
        sessionToDeactivate = nil
    }

    func selectSession(_ session: IRSession) {
        // Only open detail view if this is the currently active session
        if session.id == appState?.activeSession?.id {
            selectedSession = session
            showingSessionDetail = true
        }
    }

    func onSessionCreated(_ session: IRSession) {
        // Automatically activate the newly created session
        activateSession(session)

        // Show the detail view after creation with a small delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.selectedSession = session
            self?.showingSessionDetail = true
        }
    }

    func refreshFromServer() async {
        guard let modelContext = modelContext, let sldService = sldService else { return }

        do {
            try await sldService.refreshSLD(modelContext: modelContext)
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            if let sldError = error as? SLDServiceError {
                refreshErrorMessage = sldError.recoverySuggestion ?? sldError.localizedDescription
            } else {
                refreshErrorMessage = "Failed to refresh data: \(error.localizedDescription)"
            }
            showRefreshError = true
        }
    }

    func toggleOnlineAndRefresh() {
        networkState?.toggleMode()
        Task {
            await refreshFromServer()
        }
    }

    func toggleShowAllSessions() {
        withAnimation(.easeInOut(duration: 0.2)) {
            showAllSessions.toggle()
        }
    }

    // MARK: - Private Methods

    private func deactivateSession(_ session: IRSession) {
        // Only clear it from app state, don't modify the session itself
        if appState?.activeSession?.id == session.id {
            appState?.setActiveSession(nil)
        }

        sessionToDeactivate = nil
    }
}
