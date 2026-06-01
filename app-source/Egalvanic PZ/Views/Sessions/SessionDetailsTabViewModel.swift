//
//  SessionDetailsTabViewModel.swift
//  SwiftDataTutorial
//
//  Created by Claude on 1/5/26.
//

import SwiftUI

@MainActor
class SessionDetailsTabViewModel: ObservableObject {

    // MARK: - Published Properties (UI State)
    @Published var showingRegenerateConfirmation: Bool = false

    // MARK: - Properties
    let session: IRSession

    // These are passed from parent and managed externally
    var showingCloseConfirmation: Binding<Bool>
    var isClosingSession: Binding<Bool>

    // MARK: - Private Properties
    private var reportStore: ReportStore?
    private var networkState: NetworkState?

    // MARK: - Initialization
    init(session: IRSession,
         showingCloseConfirmation: Binding<Bool>,
         isClosingSession: Binding<Bool>) {
        self.session = session
        self.showingCloseConfirmation = showingCloseConfirmation
        self.isClosingSession = isClosingSession
    }

    // MARK: - Configuration
    func configure(reportStore: ReportStore, networkState: NetworkState) {
        self.reportStore = reportStore
        self.networkState = networkState
    }

    // MARK: - Computed Properties (Counts)

    var incompleteNodeTaskCount: Int {
        let sessionTaskIds = Set(session.user_tasks.filter { !$0.is_deleted }.map { $0.id })
        var count = 0
        for node in session.nodes where !node.is_deleted {
            for task in node.node_tasks where !task.is_deleted && sessionTaskIds.contains(task.id) {
                if !(task.nodeCompletions[node.id.uuidString] ?? false) {
                    count += 1
                }
            }
        }
        return count
    }

    var completedNodeTaskCount: Int {
        let sessionTaskIds = Set(session.user_tasks.filter { !$0.is_deleted }.map { $0.id })
        var count = 0
        for node in session.nodes where !node.is_deleted {
            for task in node.node_tasks where !task.is_deleted && sessionTaskIds.contains(task.id) {
                if task.nodeCompletions[node.id.uuidString] ?? false {
                    count += 1
                }
            }
        }
        return count
    }

    var openIssueCount: Int {
        session.issues.filter { !$0.is_deleted && $0.status?.lowercased() != "resolved" }.count
    }

    var isSessionActive: Bool {
        session.active
    }

    var isOffline: Bool {
        networkState?.mode == .offline
    }

    // MARK: - Date Formatter

    var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateStyle = .long
        formatter.timeStyle = .medium
        return formatter
    }

    var formattedStartDate: String {
        dateFormatter.string(from: session.date_created)
    }

    var formattedEndDate: String? {
        guard let dateClosed = session.date_closed else { return nil }
        return dateFormatter.string(from: dateClosed)
    }

    // MARK: - Report Status Properties

    var reportStatus: ReportStore.ReportStatus {
        reportStore?.getStatus(for: session.id) ?? .idle
    }

    var reportProgress: Double {
        reportStore?.getProgress(for: session.id) ?? 0.0
    }

    var reportButtonTitle: String {
        switch reportStatus {
        case .idle:
            return "Generate Report"
        case .generating:
            return "Generating Report..."
        case .completed:
            return "View Report (New)"
        case .viewed:
            return "View Report"
        case .failed:
            return "Retry Report Generation"
        }
    }

    var reportButtonIcon: String {
        switch reportStatus {
        case .completed:
            return "doc.badge.ellipsis"
        case .viewed:
            return "doc.text.fill"
        case .failed:
            return "exclamationmark.triangle"
        default:
            return "doc.text.fill"
        }
    }

    var reportButtonColor: Color {
        switch reportStatus {
        case .completed:
            return .green
        case .viewed:
            return .blue
        case .failed:
            return .orange
        default:
            return .blue
        }
    }

    // MARK: - Public Methods

    func requestRegenerateReport() {
        showingRegenerateConfirmation = true
    }

    func regenerateReport() {
        reportStore?.resetReport(for: session.id)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
            guard let self = self else { return }
            self.reportStore?.generateReport(for: self.session)
        }
    }

    func generateOrViewReport() {
        guard let reportStore = reportStore else { return }

        if reportStatus.isViewable {
            reportStore.viewReport(for: session.id)
            if let url = reportStatus.reportUrl,
               let reportURL = URL(string: url) {
                UIApplication.shared.open(reportURL)
            }
        } else {
            reportStore.generateReport(for: session)
        }
    }

    func handleReportNotification(sessionId: String, openReport: Bool) {
        guard sessionId == session.id.uuidString else { return }

        AppLogger.log(.debug, "Session details received notification for matching session", category: .ui)

        if openReport {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
                guard let self = self, let reportStore = self.reportStore else { return }

                reportStore.viewReport(for: self.session.id)

                if let url = reportStore.getStatus(for: self.session.id).reportUrl,
                   let reportURL = URL(string: url) {
                    UIApplication.shared.open(reportURL)
                }
            }
        }
    }
}
