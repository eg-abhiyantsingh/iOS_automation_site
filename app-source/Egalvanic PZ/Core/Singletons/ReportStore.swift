//
//  ReportStore.swift
//  SwiftDataTutorial
//
//  Centralized store for managing report generation status across the app
//

import Foundation
import SwiftUI
import UserNotifications

@MainActor
class ReportStore: ObservableObject {
    static let shared = ReportStore()

    // Track report status for each session
    @Published private(set) var reportStatuses: [UUID: ReportStatus] = [:]
    @Published private(set) var reportProgress: [UUID: Double] = [:]

    /// Dynamic base URL - uses company-specific invoke URL from config, falls back to Configuration.apiBaseURLString
    private var apiBaseURL: String {
        CompanyConfigService.shared.getCurrentInvokeURL()
    }
    
    enum ReportStatus: Equatable {
        case idle
        case generating
        case completed(reportUrl: String, generatedAt: Date)
        case failed(error: String)
        case viewed(reportUrl: String, viewedAt: Date)
        
        var isGenerating: Bool {
            if case .generating = self {
                return true
            }
            return false
        }
        
        var isCompleted: Bool {
            if case .completed = self {
                return true
            }
            return false
        }
        
        var isViewable: Bool {
            switch self {
            case .completed, .viewed:
                return true
            default:
                return false
            }
        }
        
        var reportUrl: String? {
            switch self {
            case .completed(let url, _), .viewed(let url, _):
                return url
            default:
                return nil
            }
        }
    }
    
    private init() {
        // Listen for report ready notifications
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleReportReady),
            name: .reportReady,
            object: nil
        )
        
        // Load any persisted report states
        loadPersistedReports()
    }
    
    // MARK: - Public Methods
    
    func getStatus(for sessionId: UUID) -> ReportStatus {
        return reportStatuses[sessionId] ?? .idle
    }
    
    func getProgress(for sessionId: UUID) -> Double {
        return reportProgress[sessionId] ?? 0.0
    }
    
    func generateReport(for session: IRSession) {
        let sessionId = session.id
        
        AppLogger.log(.info, "Starting report generation for session: \(session.name)", category: .general)
        
        // Update status to generating
        reportStatuses[sessionId] = .generating
        reportProgress[sessionId] = 0.0
        
        // Call the actual API
        Task {
            await callReportGenerationAPI(for: session)
        }
        
        // Persist the state
        persistReports()
    }
    
    private func callReportGenerationAPI(for session: IRSession) async {
        let sessionId = session.id

        // Get user ID and device ID
        let userId = AppStateManager.shared.userId

        // Get device ID from UserDefaults (same as used for device registration)
        let deviceId = getDeviceId()

        do {
            // Use APIClient for authenticated request with automatic token refresh
            _ = try await APIClient.shared.generateReport(
                irSessionId: sessionId,
                userId: userId,
                deviceId: deviceId
            )

            // Keep status as generating - will be updated when push notification arrives
            // Optionally simulate progress for UI feedback
            simulateProgress(for: sessionId)

        } catch {
            AppLogger.log(.error, "Error calling report generation API: \(error)", category: .api)
            reportStatuses[sessionId] = .failed(error: error.localizedDescription)
        }
        
        persistReports()
    }
    
    private func getDeviceId() -> String {
        let deviceIdKey = "com.yourapp.deviceId"
        if let existingId = UserDefaults.standard.string(forKey: deviceIdKey) {
            return existingId
        } else {
            let newId = UUID().uuidString
            UserDefaults.standard.set(newId, forKey: deviceIdKey)
            return newId
        }
    }
    
    private func simulateProgress(for sessionId: UUID) {
        // Optional: Show progress animation while waiting for real notification
        Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { timer in
            Task { @MainActor in
                guard self.reportStatuses[sessionId]?.isGenerating == true else {
                    timer.invalidate()
                    return
                }
                
                let currentProgress = self.reportProgress[sessionId] ?? 0.0
                // Slowly increment but never reach 100% (max 0.9)
                self.reportProgress[sessionId] = min(currentProgress + 0.1, 0.9)
            }
        }
    }
    
    func viewReport(for sessionId: UUID) {
        guard let status = reportStatuses[sessionId],
              case .completed(let url, _) = status else { return }
        
        // Mark as viewed
        reportStatuses[sessionId] = .viewed(reportUrl: url, viewedAt: Date())
        
        // Clear the notification
        clearDeliveredNotification(for: sessionId)
        
        // Persist the state
        persistReports()
        
        AppLogger.log(.debug, "Report viewed for session: \(sessionId)", category: .general)
    }
    
    func resetReport(for sessionId: UUID) {
        reportStatuses[sessionId] = .idle
        reportProgress[sessionId] = 0.0
        persistReports()
    }
    
    func regenerateReport(for session: IRSession) {
        // Clear any existing notifications for this session
        clearDeliveredNotification(for: session.id)
        
        // Reset and then generate
        resetReport(for: session.id)
        
        // Small delay to show the reset state
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.generateReport(for: session)
        }
    }
    
    // MARK: - Private Methods
    
    private func clearDeliveredNotification(for sessionId: UUID) {
        // Remove the specific notification
        UNUserNotificationCenter.current().removeDeliveredNotifications(
            withIdentifiers: ["report-\(sessionId.uuidString)"]
        )
        
        // Update badge count
        updateBadgeCount()
    }
    
    private func updateBadgeCount() {
        let unviewedCount = getUnviewedReportCount()
        UIApplication.shared.applicationIconBadgeNumber = unviewedCount
        AppLogger.log(.debug, "Badge updated to: \(unviewedCount)", category: .general)
    }
    
    private func getUnviewedReportCount() -> Int {
        return reportStatuses.values.filter { status in
            if case .completed = status {
                return true
            }
            return false
        }.count
    }
    
    @objc private func handleReportReady(_ notification: Notification) {
        guard let userInfo = notification.userInfo,
              let reportUrl = userInfo["report_url"] as? String else {
            AppLogger.log(.notice, "No report URL in notification", category: .general)
            return
        }
        
        // Try to get session ID from notification
        if let sessionIdString = userInfo["session_id"] as? String,
           let sessionId = UUID(uuidString: sessionIdString) {
            AppLogger.log(.info, "Report ready for specific session: \(sessionId)", category: .general)
            reportStatuses[sessionId] = .completed(
                reportUrl: reportUrl,
                generatedAt: Date()
            )
            reportProgress[sessionId] = 1.0
        } else {
            // Fallback: Update the first generating report
            AppLogger.log(.notice, "No session ID in notification, updating first generating report", category: .general)
            for (sessionId, status) in reportStatuses {
                if status.isGenerating {
                    reportStatuses[sessionId] = .completed(
                        reportUrl: reportUrl,
                        generatedAt: Date()
                    )
                    reportProgress[sessionId] = 1.0
                    AppLogger.log(.debug, "Report updated for session: \(sessionId)", category: .general)
                    break
                }
            }
        }
        
        persistReports()
        
        // Force UI update
        objectWillChange.send()
    }
    
    // MARK: - Persistence
    
    private func persistReports() {
        // Simple persistence using UserDefaults
        // In production, use Core Data or another proper storage solution
        
        var persistedData: [[String: Any]] = []
        
        for (sessionId, status) in reportStatuses {
            var data: [String: Any] = ["sessionId": sessionId.uuidString]
            
            switch status {
            case .idle:
                data["status"] = "idle"
            case .generating:
                data["status"] = "generating"
            case .completed(let url, let date):
                data["status"] = "completed"
                data["url"] = url
                data["date"] = date.timeIntervalSince1970
            case .viewed(let url, let date):
                data["status"] = "viewed"
                data["url"] = url
                data["date"] = date.timeIntervalSince1970
            case .failed(let error):
                data["status"] = "failed"
                data["error"] = error
            }
            
            persistedData.append(data)
        }
        
        UserDefaults.standard.set(persistedData, forKey: "reportStatuses")
    }
    
    private func loadPersistedReports() {
        guard let persistedData = UserDefaults.standard.array(forKey: "reportStatuses") as? [[String: Any]] else { return }
        
        for data in persistedData {
            guard let sessionIdString = data["sessionId"] as? String,
                  let sessionId = UUID(uuidString: sessionIdString),
                  let statusString = data["status"] as? String else { continue }
            
            switch statusString {
            case "completed":
                if let url = data["url"] as? String,
                   let timestamp = data["date"] as? TimeInterval {
                    reportStatuses[sessionId] = .completed(
                        reportUrl: url,
                        generatedAt: Date(timeIntervalSince1970: timestamp)
                    )
                }
            case "viewed":
                if let url = data["url"] as? String,
                   let timestamp = data["date"] as? TimeInterval {
                    reportStatuses[sessionId] = .viewed(
                        reportUrl: url,
                        viewedAt: Date(timeIntervalSince1970: timestamp)
                    )
                }
            case "failed":
                if let error = data["error"] as? String {
                    reportStatuses[sessionId] = .failed(error: error)
                }
            default:
                reportStatuses[sessionId] = .idle
            }
        }
        
        // Update badge count on load
        updateBadgeCount()
    }
}
