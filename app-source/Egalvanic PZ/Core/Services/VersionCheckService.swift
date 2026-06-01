//
//  VersionCheckService.swift
//  Egalvanic PZ
//
//  Service for checking app version and handling updates
//

import Foundation
import SwiftUI

@MainActor
class VersionCheckService: ObservableObject {
    static let shared = VersionCheckService()

    // MARK: - Published State
    @Published var showOptionalUpdate = false
    @Published var showForceUpdate = false
    @Published var updateInfo: UpdateInfo?

    // MARK: - Force Update Sync State
    @Published var showForceUpdateSync = false
    @Published var isSyncingBeforeUpdate = false
    @Published var syncBeforeUpdateFailed = false
    @Published var syncFailureMessage: String = ""

    // MARK: - Private
    private let remindLaterTimestampKey = "updateRemindLaterTimestamp"
    private let remindLaterDurationKey = "updateRemindLaterDuration"
    private let lastKnownVersionKey = "lastKnownAppVersion"
    private var isChecking = false

    // Access NetworkState for sync queue operations
    private var networkState: NetworkState { NetworkState.shared }

    // Enable logging only in DEBUG builds
    #if DEBUG
    private let enableLogging = true
    #else
    private let enableLogging = false
    #endif

    private init() {}

    // MARK: - Logging Helper
    private func log(_ message: String) {
        if enableLogging {
            AppLogger.log(.debug, "[VersionCheckService] \(message)", category: .general)
        }
    }

    // MARK: - Public API

    var currentAppVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }

    func checkForUpdate() async {
        guard !isChecking else {
            log("Already checking, skipping")
            return
        }
        isChecking = true
        defer { isChecking = false }

        log("Checking for update, current version: \(currentAppVersion)")

        // TEMPORARILY DISABLED: Auto-sync on app update
        // Check if app was just updated - trigger sync/refresh first
        // if wasAppJustUpdated() {
        //     log("App was updated, performing post-update sync")
        //     await performPostUpdateSync()
        // }

        do {
            let response = try await performVersionCheck()
            handleResponse(response)
        } catch {
            // Silently continue on failure
            log("Version check failed: \(error.localizedDescription)")
        }
    }

    func dismissOptionalUpdate() {
        // Store current timestamp and remind later duration
        let currentTimestamp = Date().timeIntervalSince1970
        UserDefaults.standard.set(currentTimestamp, forKey: remindLaterTimestampKey)

        // Store the remind later duration (default to 24 hours if not provided)
        let duration = updateInfo?.remind_later_seconds ?? 86400
        UserDefaults.standard.set(duration, forKey: remindLaterDurationKey)

        log("Dismissed update, will remind after \(duration) seconds")

        showOptionalUpdate = false
        updateInfo = nil
    }

    func openAppStore() {
        guard let urlString = updateInfo?.store_url,
              let url = URL(string: urlString) else {
            log("Invalid or missing store URL")
            return
        }
        log("Opening App Store: \(urlString)")
        UIApplication.shared.open(url)
    }

    // MARK: - App Update Detection

    /// Check if this is the first launch after an app update
    private func wasAppJustUpdated() -> Bool {
        let storedVersion = UserDefaults.standard.string(forKey: lastKnownVersionKey)
        let currentVersion = currentAppVersion

        // No stored version - user upgrading from older version without this feature
        // Trigger refresh to ensure data is synced
        guard let storedVersion = storedVersion else {
            log("No stored version found, triggering refresh for version \(currentVersion)")
            return true
        }

        // Compare versions
        if storedVersion != currentVersion {
            log("App updated from \(storedVersion) to \(currentVersion)")
            return true
        }

        return false
    }

    /// Mark current version as processed
    private func markVersionAsProcessed() {
        UserDefaults.standard.set(currentAppVersion, forKey: lastKnownVersionKey)
        log("Marked version \(currentAppVersion) as processed")
    }

    /// Perform auto-sync and refresh after app update
    /// Uses existing SLDSyncService which shows RefreshProgressOverlay automatically
    private func performPostUpdateSync() async {
        // Check actual device connectivity, not just app mode
        let reachability = NetworkReachability.shared
        guard reachability.isConnected else {
            log("Skipping post-update sync - no internet connection, will retry when online")
            // Don't mark version - will retry when device has internet
            return
        }

        let defaultSLDId = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
        guard AppStateManager.shared.activeSLDId != defaultSLDId else {
            log("Skipping post-update sync - no active SLD")
            return // Don't mark - wait for site selection
        }

        guard let modelContext = networkState.modelContext else {
            log("Skipping post-update sync - no model context")
            return
        }

        log("Performing post-update sync")

        // Check if there are pending sync items
        if networkState.syncQueueCount > 0 {
            // Flush pending items - flushQueue() already refreshes from server after successful sync
            log("Post-update: flushing \(networkState.syncQueueCount) pending items")
            await withCheckedContinuation { continuation in
                networkState.flushQueue {
                    continuation.resume()
                }
            }
            log("Post-update sync completed (via flushQueue)")
        } else {
            // No pending items - just refresh from server
            log("Post-update: no pending items, refreshing from server")
            do {
                try await SLDSyncService.shared.upsertAllData(
                    sld_id: AppStateManager.shared.activeSLDId,
                    modelContext: modelContext
                )
                log("Post-update sync completed successfully")
            } catch {
                log("Post-update sync refresh failed: \(error.localizedDescription)")
            }
        }

        markVersionAsProcessed()
    }

    // MARK: - Private Methods

    private func performVersionCheck() async throws -> VersionCheckResponse {
        let baseURL = CompanyConfigService.shared.getCurrentInvokeURL()
        guard let url = URL(string: baseURL)?.appendingPathComponent(APIEndpoints.Version.check) else {
            throw URLError(.badURL)
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        // Add authorization if available
        if let token = AuthService.shared.getAccessToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        // Add subdomain header for multi-tenant support
        if let subdomain = AuthService.shared.getCurrentSubdomain() {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        }
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        let requestBody = VersionCheckRequest(versionCode: currentAppVersion)
        request.httpBody = try JSONEncoder().encode(requestBody)

        log("POST \(url.absoluteString)")

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse,
              (200..<300).contains(httpResponse.statusCode) else {
            log("HTTP error")
            throw URLError(.badServerResponse)
        }

        let versionResponse = try JSONDecoder().decode(VersionCheckResponse.self, from: data)
        log("Response: requires_update=\(versionResponse.requires_update), force_update=\(versionResponse.force_update)")

        return versionResponse
    }

    private func handleResponse(_ response: VersionCheckResponse) {
        // No update needed
        guard response.requires_update, let info = response.update_info else {
            log("No update required")
            return
        }

        updateInfo = info

        if info.force_update {
            log("Force update required")
            // Check if sync is needed first
            if networkState.syncQueueCount > 0 {
                log("Pending sync items: \(networkState.syncQueueCount), syncing before update")
                showForceUpdateSync = true
            } else {
                log("No pending sync, showing force update directly")
                showForceUpdate = true
            }
        } else {
            // Check if remind later time has passed
            if shouldShowOptionalUpdate() {
                log("Optional update available")
                showOptionalUpdate = true
            } else {
                log("Update available but within remind later period")
            }
        }
    }

    /// Check if enough time has passed since user dismissed the update
    private func shouldShowOptionalUpdate() -> Bool {
        let storedTimestamp = UserDefaults.standard.double(forKey: remindLaterTimestampKey)

        // If no timestamp stored, show the update
        guard storedTimestamp > 0 else {
            log("No previous dismiss timestamp, showing update")
            return true
        }

        let storedDuration = UserDefaults.standard.integer(forKey: remindLaterDurationKey)
        let duration = storedDuration > 0 ? storedDuration : 86400 // Default 24 hours

        let currentTimestamp = Date().timeIntervalSince1970
        let elapsedSeconds = currentTimestamp - storedTimestamp

        if elapsedSeconds >= Double(duration) {
            log("Remind later period expired (\(Int(elapsedSeconds))s >= \(duration)s)")
            return true
        } else {
            let remaining = duration - Int(elapsedSeconds)
            log("Remind later period active, \(remaining)s remaining")
            return false
        }
    }

    // MARK: - Force Update Sync Methods

    func startSyncBeforeUpdate() {
        isSyncingBeforeUpdate = true
        syncBeforeUpdateFailed = false
        log("Starting sync before update")

        networkState.flushQueue { [weak self] in
            Task { @MainActor in
                guard let self = self else { return }
                self.isSyncingBeforeUpdate = false

                // Check if sync was successful
                if self.networkState.syncQueueCount == 0 {
                    self.log("Sync complete, showing force update")
                    self.showForceUpdateSync = false
                    self.showForceUpdate = true
                } else {
                    self.log("Sync incomplete, \(self.networkState.syncQueueCount) items remaining")
                    self.syncBeforeUpdateFailed = true
                    self.syncFailureMessage = "\(self.networkState.syncQueueCount) items could not be synced"
                }
            }
        }
    }

    func proceedToUpdateAnyway() {
        log("User chose to proceed to update without complete sync")
        showForceUpdateSync = false
        showForceUpdate = true
    }

    func retrySyncBeforeUpdate() {
        log("Retrying sync before update")
        syncBeforeUpdateFailed = false
        startSyncBeforeUpdate()
    }
}
