//
//  IRSessionDetailViewModel.swift
//  SwiftDataTutorial
//
//  Created by Claude on 1/5/26.
//

import SwiftUI
import SwiftData

@MainActor
class IRSessionDetailViewModel: ObservableObject {

    // MARK: - Published Properties (UI State)
    @Published var selectedTab: Int = 0
    @Published var showingCloseConfirmation: Bool = false
    @Published var isClosingSession: Bool = false
    @Published var showError: Bool = false
    @Published var errorMessage: String = ""
    @Published var selectedTask: UserTask?
    @Published var selectedIssue: Issue?
    @Published var showRefreshError: Bool = false
    @Published var refreshErrorMessage: String = ""

    // MARK: - Private Properties
    private var modelContext: ModelContext?
    private var networkState: NetworkState?
    private var sldService: SLDService?
    let session: IRSession

    // MARK: - Initialization
    init(session: IRSession) {
        self.session = session
    }

    // MARK: - Configuration
    func configure(
        modelContext: ModelContext,
        networkState: NetworkState,
        sldService: SLDService
    ) {
        self.modelContext = modelContext
        self.networkState = networkState
        self.sldService = sldService
    }

    // MARK: - Computed Properties

    var incompleteNodeTaskCount: Int {
        let filteredSessionTasks = session.user_tasks.filter { !$0.is_deleted }
        let sessionTaskIds = Set(filteredSessionTasks.map { $0.id })

        var count = 0
        let activeNodes = session.nodes.filter { !$0.is_deleted }

        for node in activeNodes {
            let nodeTasks = node.node_tasks.filter { !$0.is_deleted && sessionTaskIds.contains($0.id) }
            for task in nodeTasks {
                let isCompleteForNode = task.nodeCompletions[node.id.uuidString] ?? false
                if !isCompleteForNode {
                    count += 1
                }
            }
        }
        return count
    }

    var taskCount: Int {
        session.user_tasks.filter { !$0.is_deleted }.count
    }

    var issueCount: Int {
        session.issues.filter { !$0.is_deleted }.count
    }

    var openIssueCount: Int {
        session.issues.filter { !$0.is_deleted && $0.status?.lowercased() != "resolved" }.count
    }

    var isOffline: Bool {
        networkState?.mode == .offline
    }

    var isRefreshing: Bool {
        sldService?.isRefreshing ?? false
    }

    var sld: SLDV2 {
        session.sld
    }

    // MARK: - Public Methods

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

    func requestCloseSession() {
        showingCloseConfirmation = true
    }

    func confirmCloseSession() async {
        await closeSession()
    }

    func toggleOnlineAndRefresh() {
        networkState?.toggleMode()
        Task {
            await refreshFromServer()
        }
    }

    func dismissError() {
        showError = false
        errorMessage = ""
    }

    // MARK: - Private Methods

    private func closeSession() async {
        guard let modelContext = modelContext, let networkState = networkState else { return }

        isClosingSession = true

        AppLogger.log(.info, "Closing IR session: \(session.id), network mode: \(networkState.mode)", category: .ui)

        // Update local state
        session.active = false
        session.date_closed = Date()

        do {
            try modelContext.save()
            AppLogger.log(.info, "Session closed locally", category: .ui)

            // Handle sync based on network mode
            if networkState.mode == .online {
                AppLogger.log(.debug, "Online mode - attempting immediate sync", category: .ui)
                do {
                    _ = try await APIClient.shared.updateIRSession(session)
                    AppLogger.log(.info, "Successfully synced session update to server", category: .ui)
                } catch {
                    AppLogger.log(.notice, "Failed to sync immediately, queuing for later: \(error)", category: .ui)

                    // Queue for later sync
                    let syncOp = SyncOp(
                        target: .irSession,
                        operation: .update,
                        irSession: session
                    )
                    networkState.enqueue(syncOp)
                }
            } else {
                AppLogger.log(.info, "Offline mode - queuing for sync", category: .ui)

                // Queue for sync when online
                let syncOp = SyncOp(
                    target: .irSession,
                    operation: .update,
                    irSession: session
                )
                networkState.enqueue(syncOp)

                AppLogger.log(.info, "Session update queued for sync (queue size: \(networkState.syncQueueCount))", category: .ui)
            }

            isClosingSession = false

        } catch {
            guard !AuthError.isAuthError(error) else { return }
            AppLogger.log(.error, "Failed to close IR session: \(error)", category: .ui)
            isClosingSession = false
            errorMessage = "Failed to close session: \(error.localizedDescription)"
            showError = true
        }
    }
}
