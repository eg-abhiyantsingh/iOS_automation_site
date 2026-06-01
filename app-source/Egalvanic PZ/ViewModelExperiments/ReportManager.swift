//
//  ReportManager.swift
//  SwiftDataTutorial
//
//  Manages report generation with dummy local notification for testing
//

import Foundation
import UserNotifications
import UIKit
import SwiftUI

@MainActor
class ReportManager: ObservableObject {
    @Published var reportStatus: ReportStatus = .idle
    @Published var progress: Double = 0.0
    
    enum ReportStatus: Equatable {
        case idle
        case generating
        case completed(reportUrl: String)
        case failed(error: String)
        
        var isGenerating: Bool {
            if case .generating = self {
                return true
            }
            return false
        }
    }
    
    func generateReport(for session: IRSession) {
        AppLogger.log(.info, "Starting dummy report generation for session: \(session.name)", category: .general)
        
        // Update status to generating
        reportStatus = .generating
        progress = 0.0
        
        // Schedule a local notification for 10 seconds from now
        scheduleLocalNotification(for: session, delay: 10)
        
        // Simulate progress updates
        simulateProgress()
    }
    
    private func scheduleLocalNotification(for session: IRSession, delay: TimeInterval) {
        let content = UNMutableNotificationContent()
        content.title = "Report Ready! 📊"
        content.body = "Your IR session report for \"\(session.name)\" is ready to view."
        content.sound = .default
        content.badge = 1
        
        // Add custom data
        content.userInfo = [
            "sessionId": session.id.uuidString,
            "sessionName": session.name,
            "type": "reportComplete"
        ]
        
        // Create trigger for 10 seconds from now
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: delay, repeats: false)
        
        // Create request
        let request = UNNotificationRequest(
            identifier: "report-\(session.id.uuidString)",
            content: content,
            trigger: trigger
        )
        
        // Schedule the notification
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                AppLogger.log(.error, "Error scheduling notification: \(error)", category: .general)
            } else {
                AppLogger.log(.info, "Notification scheduled for \(delay) seconds from now", category: .general)
            }
        }
    }
    
    private func simulateProgress() {
        // Simulate progress over 10 seconds
        Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { timer in
            Task { @MainActor in
                self.progress += 0.05
                
                if self.progress >= 1.0 {
                    timer.invalidate()
                    // After 10 seconds, mark as complete
                    self.reportStatus = .completed(reportUrl: "https://example.com/report/dummy")
                    AppLogger.log(.info, "Report generation complete!", category: .general)
                }
            }
        }
    }
    
    func cancelReportGeneration(for sessionId: UUID) {
        // Cancel the scheduled notification
        UNUserNotificationCenter.current().removePendingNotificationRequests(
            withIdentifiers: ["report-\(sessionId.uuidString)"]
        )
        
        reportStatus = .idle
        progress = 0.0
        AppLogger.log(.info, "Report generation cancelled", category: .general)
    }
    
    func resetStatus() {
        reportStatus = .idle
        progress = 0.0
    }
    
    func viewReport(for session: IRSession) {
        // Clear any delivered notifications for this session
        clearDeliveredNotification(for: session.id)
        
        // In a real app, you'd open the report URL here
        if case .completed(let url) = reportStatus {
            AppLogger.log(.info, "Opening report: \(url)", category: .general)
        }
    }
    
    private func clearDeliveredNotification(for sessionId: UUID) {
        // Remove the specific notification
        UNUserNotificationCenter.current().removeDeliveredNotifications(
            withIdentifiers: ["report-\(sessionId.uuidString)"]
        )
        
        // Update badge count
        Task {
            await updateBadgeCount()
        }
    }
    
    @MainActor
    private func updateBadgeCount() async {
        // Get all delivered notifications
        let delivered = await UNUserNotificationCenter.current().deliveredNotifications()
        
        // Filter for report notifications
        let reportNotifications = delivered.filter { notification in
            notification.request.content.userInfo["type"] as? String == "reportComplete"
        }
        
        // Update badge to reflect remaining notifications
        UIApplication.shared.applicationIconBadgeNumber = reportNotifications.count
        
        AppLogger.log(.debug, "Badge updated to: \(reportNotifications.count)", category: .general)
    }
}
