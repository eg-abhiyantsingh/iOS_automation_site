//
//  DevRevService.swift
//  Egalvanic PZ
//
//  Manages DevRev SDK lifecycle: configuration, user identity, and support widget.
//

import Foundation
import UIKit
import DevRevSDK

/// User-facing preferences for the DevRev SDK.
enum DevRevPreferences {
    static let recordingEnabledKey = "devrev_recording_enabled"

    /// Whether session recording should start automatically at app launch.
    /// Defaults to `true` when no preference has been set (matches Android parity).
    static var isRecordingEnabled: Bool {
        get {
            if UserDefaults.standard.object(forKey: recordingEnabledKey) == nil { return true }
            return UserDefaults.standard.bool(forKey: recordingEnabledKey)
        }
        set { UserDefaults.standard.set(newValue, forKey: recordingEnabledKey) }
    }
}

final class DevRevService {
    static let shared = DevRevService()

    /// Whether the SDK was successfully configured
    private(set) var isConfigured = false

    private init() {}

    // MARK: - SDK Configuration

    /// Configure the DevRev SDK with the App ID from xcconfig.
    /// Call this once at app startup (in AppDelegate).
    func configure() {
        guard let appID = Configuration.devRevAppID else {
            AppLogger.log(.notice, "DevRev App ID not configured - in-app support disabled", category: .general)
            return
        }

        // Both frame capture and auto-start are gated on the user toggle. Frame capture
        // is the pipeline that produces visual session replay; it also drove the ZP-1928
        // typing lag when enabled, so users can turn the whole thing off from Settings.
        let featureConfig = FeatureConfiguration(
            enableFrameCapture: DevRevPreferences.isRecordingEnabled,
            autoStartRecording: DevRevPreferences.isRecordingEnabled
        )
        DevRev.configure(appID: appID, featureConfiguration: featureConfig)
        isConfigured = true
        registerLifecycleObservers()
        AppLogger.log(.info, "DevRev SDK configured for \(Configuration.environmentName) (recording: \(DevRevPreferences.isRecordingEnabled))", category: .general)
    }

    // MARK: - Lifecycle

    /// Stop recording when the app backgrounds so the session is flushed to DevRev
    /// before the user can force-kill it from the app switcher. Resume on foreground.
    private func registerLifecycleObservers() {
        NotificationCenter.default.addObserver(
            forName: UIApplication.didEnterBackgroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleDidEnterBackground()
        }
        NotificationCenter.default.addObserver(
            forName: UIApplication.willEnterForegroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleWillEnterForeground()
        }
    }

    private func handleDidEnterBackground() {
        guard isConfigured, DevRevPreferences.isRecordingEnabled else { return }
        DevRev.stopRecording()
        AppLogger.log(.info, "DevRev: session stopped on background to flush buffer", category: .general)
    }

    private func handleWillEnterForeground() {
        guard isConfigured, DevRevPreferences.isRecordingEnabled else { return }
        Task {
            await DevRev.startRecording()
            AppLogger.log(.info, "DevRev: recording resumed on foreground", category: .general)
        }
    }

    /// Apply a recording preference change at runtime so the user doesn't need to relaunch.
    /// Updates the feature configuration (frame-capture pipeline) and starts/stops recording.
    func setRecordingEnabled(_ enabled: Bool) async {
        DevRevPreferences.isRecordingEnabled = enabled
        guard isConfigured else { return }

        let featureConfig = FeatureConfiguration(
            enableFrameCapture: enabled,
            autoStartRecording: enabled
        )
        DevRev.updateFeatureConfiguration(featureConfig)

        if enabled {
            await DevRev.startRecording()
        } else {
            DevRev.stopRecording()
        }
        AppLogger.log(.info, "DevRev recording \(enabled ? "enabled" : "disabled") at runtime", category: .general)
    }

    // MARK: - User Identity

    /// Identify the user after login. Tries verified (session token from backend) first,
    /// falls back to unverified identification.
    func identifyUser(userId: String, email: String?, displayName: String?) async {
        guard isConfigured else { return }

        // Try verified identification first (requires backend session token)
        if let sessionToken = await fetchSessionToken(userRef: userId, email: email, displayName: displayName) {
            AppLogger.log(.info, "DevRev: identifying user with verified session token", category: .auth)
            await DevRev.identifyVerifiedUser(userId, sessionToken: sessionToken)
            return
        }

        // Fallback to unverified identification
        AppLogger.log(.info, "DevRev: falling back to unverified user identification", category: .auth)
        await identifyUnverified(userId: userId, email: email, displayName: displayName)
    }

    /// Clear DevRev session on logout.
    func clearSession() async {
        guard isConfigured else { return }

        let deviceId = UserDefaults.standard.string(forKey: "com.yourapp.deviceId") ?? ""
        AppLogger.log(.info, "DevRev: logging out user", category: .auth)
        await DevRev.logout(deviceID: deviceId)
    }

    // MARK: - Support Widget

    /// Show the DevRev support widget.
    @MainActor
    func showSupport() async {
        guard isConfigured else { return }
        await DevRev.showSupport(isAnimated: true)
    }

    // MARK: - Private Helpers

    private func identifyUnverified(userId: String, email: String?, displayName: String?) async {
        let userTraits = Identity.UserTraits(
            displayName: displayName,
            email: email,
            fullName: nil,
            userDescription: nil,
            phoneNumbers: nil,
            customFields: nil
        )
        let identity = Identity(
            userID: userId,
            userTraits: userTraits
        )
        await DevRev.identifyUnverifiedUser(identity)
    }

    /// Fetch a verified session token from the backend.
    /// POST /devrev/session-token with user_ref, email, display_name.
    /// Returns nil on failure (caller should fall back to unverified).
    @MainActor
    private func fetchSessionToken(userRef: String, email: String?, displayName: String?) async -> String? {
        guard let token = AuthService.shared.getAccessToken() else {
            AppLogger.log(.notice, "DevRev: no auth token for session token request", category: .auth)
            return nil
        }

        let urlString = CompanyConfigService.shared.getCurrentInvokeURL()
        guard let baseURL = URL(string: urlString) else { return nil }
        let url = baseURL.appendingPathComponent(APIEndpoints.DevRev.sessionToken)

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        if let subdomain = AuthService.shared.getCurrentSubdomain() {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        }

        let body: [String: String] = [
            "user_ref": userRef,
            "email": email ?? "",
            "display_name": displayName ?? ""
        ]

        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200 else {
                AppLogger.log(.notice, "DevRev: session token request failed", category: .api)
                return nil
            }

            if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
               let sessionToken = json["session_token"] as? String {
                return sessionToken
            }
        } catch {
            AppLogger.log(.error, "DevRev: session token fetch error: \(error.localizedDescription)", category: .api)
        }

        return nil
    }
}

