//
//  AppDelegate.swift
//  SwiftDataTutorial
//
//  Handles push notification setup and management
//

import UIKit
import UserNotifications
import Sentry
import DevRevSDK

class AppDelegate: NSObject, UIApplicationDelegate {

    static var shared: AppDelegate?
    private var pendingDeviceToken: String?
    private var hasRegisteredDeviceThisSession = false
    
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

        // Store reference for later use
        AppDelegate.shared = self

        // Initialize Sentry for crash reporting
        initializeSentry()

        // Initialize DevRev for in-app support
        DevRevService.shared.configure()

        // Check for fresh install and clean up stale keychain data
        checkForFreshInstallAndCleanup()

        // Set up notification center delegate
        UNUserNotificationCenter.current().delegate = self

        // DON'T request permissions here - wait for login
        // requestNotificationPermission()

        return true
    }

    // MARK: - Sentry Initialization

    private func initializeSentry() {
        guard let dsn = Configuration.sentryDSN else {
            AppLogger.log(.notice, "Sentry DSN not configured - crash reporting disabled", category: .general)
            return
        }

        SentrySDK.start { options in
            options.dsn = dsn
            options.environment = Configuration.environmentName.lowercased()

            // Disable debug logging in production
            options.debug = !Configuration.isProduction

            // Set release version
            if let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String,
               let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String {
                options.releaseName = "com.egalvanic.zplatform@\(version)+\(build)"
            }

            // Enable comprehensive error tracking
            options.attachStacktrace = true

            // Enable automatic breadcrumb tracking
            options.enableAutoBreadcrumbTracking = true
            options.maxBreadcrumbs = 100

            // Enable network breadcrumbs for API debugging
            options.enableNetworkBreadcrumbs = true

            // Capture HTTP client errors (4xx, 5xx)
            options.enableCaptureFailedRequests = true
            options.failedRequestStatusCodes = [
                HttpStatusCodeRange(min: 400, max: 599)
            ]

            // App lifecycle monitoring
            options.enableAppHangTracking = true
            options.appHangTimeoutInterval = 5.0
            options.enableWatchdogTerminationTracking = true

            // Watchdog termination includes OOM tracking
            // options.enableOutOfMemoryTracking is not available in this SDK version

            // Disable performance monitoring per user preference (just error tracking)
            options.tracesSampleRate = 0

            // Before send hook for enriching events with sync state
            options.beforeSend = { event in
                // Add sync queue count to every event
                event.extra?["sync_queue_count"] = NetworkState.shared.syncQueueCount
                event.extra?["is_syncing"] = NetworkState.shared.isSyncing
                event.extra?["network_mode"] = "\(NetworkState.shared.mode)"
                return event
            }
        }
        AppLogger.log(.info, "Sentry initialized for \(Configuration.environmentName)", category: .general)
    }

    // MARK: - Fresh Install Detection
    private func checkForFreshInstallAndCleanup() {
        let hasLaunchedKey = "com.eg.projectz.hasLaunchedBefore"
        let hasLaunchedBefore = UserDefaults.standard.bool(forKey: hasLaunchedKey)

        if !hasLaunchedBefore {
            // Check if this is an app update by looking for ANY existing UserDefaults data
            // (UserDefaults are cleared on uninstall, but not on update)
            let existingDefaults = UserDefaults.standard.dictionaryRepresentation()

            // Check if we have any app-specific keys (not system keys)
            let hasAppData = existingDefaults.keys.contains { key in
                // Look for any of our app's specific keys
                key.contains("com.eg") ||
                key.contains("currentUserId") ||
                key.contains("deviceToken") ||
                key.contains("deviceId") ||
                key.contains("biometric")
            }

            if hasAppData {
                // This is an app update - user had the app before
                AppLogger.log(.info, "App update detected - preserving existing authentication", category: .auth)
            } else {
                // This is truly a fresh install - no UserDefaults means app was uninstalled
                AppLogger.log(.info, "Fresh install detected - cleaning up stale keychain data", category: .auth)

                // Clear all keychain data on fresh install
                KeychainService.shared.clearAllTokens()
                KeychainService.shared.deleteCredentials()

                // Clear any auth-related UserDefaults (though they should already be gone)
                UserDefaults.standard.removeObject(forKey: "currentUserId")
                UserDefaults.standard.removeObject(forKey: "deviceToken")
                UserDefaults.standard.removeObject(forKey: "biometricAuthEnabled")

                AppLogger.log(.info, "Fresh install cleanup completed", category: .auth)
            }

            // Mark that app has launched before (for both update and fresh install)
            UserDefaults.standard.set(true, forKey: hasLaunchedKey)
            UserDefaults.standard.synchronize()
        } else {
            AppLogger.log(.debug, "Not a fresh install - preserving existing data", category: .auth)
        }
    }

    // Call this AFTER successful login
    func setupPushNotificationsAfterLogin(userId: String) {
        AppLogger.log(.info, "Setting up push notifications for user: \(userId)", category: .auth)
        
        // Store the user ID first
        UserDefaults.standard.set(userId, forKey: "currentUserId")
        
        // Now request notification permissions
        requestNotificationPermission()
    }
    
    func requestNotificationPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if let error = error {
                AppLogger.log(.error, "Error requesting notifications: \(error)", category: .general)
                // Still register device with empty token on error
                self.registerDeviceAfterPermission(token: "")
                return
            }

            if granted {
                AppLogger.log(.info, "Notification permission granted", category: .general)
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
                // Token will be received in didRegisterForRemoteNotificationsWithDeviceToken
                // Registration will happen there
            } else {
                AppLogger.log(.notice, "Notification permission denied", category: .general)
                // Register device with empty token
                self.registerDeviceAfterPermission(token: "")
            }
        }
    }
    
    // Called when APNs registration succeeds
    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        AppLogger.log(.info, "Device token received", category: .auth)

        // Store token locally
        UserDefaults.standard.set(token, forKey: "deviceToken")
        self.pendingDeviceToken = token

        // Register device with the token
        registerDeviceAfterPermission(token: token)
    }
    
    // Call this method after login if we already have a token
    func registerPendingDeviceTokenIfNeeded() {
        guard let token = pendingDeviceToken ?? UserDefaults.standard.string(forKey: "deviceToken"),
              let userId = UserDefaults.standard.string(forKey: "currentUserId") else {
            AppLogger.log(.notice, "Missing token or user ID for registration", category: .auth)
            return
        }
        
        Task {
            await registerDeviceWithBackend(token: token, userId: userId)
        }
    }
    
    // MARK: - Device Registration

    /// Single entry point for device registration after permission dialog
    private func registerDeviceAfterPermission(token: String) {
        // Prevent duplicate registration in the same session
        guard !hasRegisteredDeviceThisSession else {
            AppLogger.log(.debug, "Device already registered this session", category: .auth)
            return
        }

        guard let userId = UserDefaults.standard.string(forKey: "currentUserId") else {
            AppLogger.log(.notice, "No user logged in - cannot register device", category: .auth)
            return
        }

        hasRegisteredDeviceThisSession = true

        Task {
            await registerDeviceWithBackend(token: token, userId: userId)
        }
    }

    private func registerDeviceWithBackend(token: String, userId: String) async {
        AppLogger.log(.info, "Registering device for user: \(userId)", category: .auth)
        
        // Get or create device ID
        let deviceId = getOrCreateDeviceId()
        
        // Determine environment
        #if DEBUG
        let environment = "sandbox"
        #else
        let environment = "production"
        #endif
        
        // Get auth token for authenticated request
        guard let authToken = AuthService.shared.getAccessToken() else {
            AppLogger.log(.error, "No auth token available for device registration", category: .auth)
            return
        }
        
        // Your API endpoint - use dynamic invoke URL from company config
        let urlString = CompanyConfigService.shared.getCurrentInvokeURL()
        guard let baseURL = URL(string: urlString) else {
            AppLogger.log(.error, "Invalid base URL: \(urlString)", category: .api)
            return
        }
        let url = baseURL.appendingPathComponent(APIEndpoints.Device.register)
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(authToken)", forHTTPHeaderField: "Authorization")
        if let subdomain = AuthService.shared.getCurrentSubdomain() {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
            AppLogger.log(.debug, "X-Subdomain header: \(subdomain)", category: .api)
        } else {
            AppLogger.log(.notice, "No subdomain available - backend may fail to validate token", category: .api)
        }
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        // Get app version info from Bundle
        let versionName = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "unknown"
        let versionCode = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown"

        let body: [String: Any] = [
            "user_id": userId,
            "device_id": deviceId,
            "device_token": token,
            "platform": "ios",
            "environment": environment,
            "device_name": UIDevice.current.name,
            "version_name": versionName,
            "version_code": versionCode,
            "phone_model": getDeviceModel(),
            "os_version": UIDevice.current.systemVersion
        ]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
            
            let (data, response) = try await URLSession.shared.data(for: request)
            
            if let httpResponse = response as? HTTPURLResponse {
                if httpResponse.statusCode == 200 || httpResponse.statusCode == 201 {
                    AppLogger.log(.info, "Device registered successfully", category: .auth)
                    
                    // Mark as registered for this user
                    UserDefaults.standard.set(true, forKey: "deviceRegistered_\(userId)")
                    
                } else {
                    let errorBody = String(data: data, encoding: .utf8) ?? "<no body>"
                    AppLogger.log(.error, "Device registration failed with status \(httpResponse.statusCode): \(errorBody)", category: .auth)
                }
            }
        } catch {
            AppLogger.log(.error, "Failed to register device: \(error)", category: .auth)
        }
    }
    
    // Call this on logout to clean up
    func clearUserNotificationData() {
        AppLogger.log(.info, "Clearing notification data for logout", category: .auth)

        // Reset registration flag so next login will register again
        hasRegisteredDeviceThisSession = false

        // Clear user-specific data but keep device token
        UserDefaults.standard.removeObject(forKey: "currentUserId")

        // Clear any delivered notifications
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        UIApplication.shared.applicationIconBadgeNumber = 0
    }
    
    private func getOrCreateDeviceId() -> String {
        let key = "com.yourapp.deviceId"
        if let existingId = UserDefaults.standard.string(forKey: key) {
            return existingId
        } else {
            let newId = UUID().uuidString
            UserDefaults.standard.set(newId, forKey: key)
            return newId
        }
    }

    private func getDeviceModel() -> String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let model = machineMirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return identifier }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }
        return model
    }
    
    // Called when APNs registration fails
    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        AppLogger.log(.error, "Failed to register for remote notifications: \(error)", category: .general)
    }
}

// Handle notifications when app is in foreground
extension AppDelegate: UNUserNotificationCenterDelegate {
    
    // Called when notification arrives while app is in foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                               willPresent notification: UNNotification,
                               withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        
        AppLogger.log(.info, "Notification received in foreground", category: .general)
        
        let userInfo = notification.request.content.userInfo
        
        // Extract report URL
        if let reportUrl = userInfo["report_url"] as? String {
            AppLogger.log(.info, "Report URL received: \(reportUrl)", category: .general)
            
            // Post notification to update UI
            NotificationCenter.default.post(
                name: .reportReady,
                object: nil,
                userInfo: [
                    "report_url": reportUrl,
                    "type": userInfo["type"] as? String ?? "report_complete"
                ]
            )
        }
        
        // Show banner even when app is open
        completionHandler([.banner, .sound, .badge])
    }
    
    func application(_ application: UIApplication,
                    didReceiveRemoteNotification userInfo: [AnyHashable : Any],
                    fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        
        AppLogger.log(.info, "Background notification received", category: .general)
        
        // Update ReportStore even when app is in background
        if let _ = userInfo["report_url"] as? String,
           let _ = userInfo["session_id"] as? String {
            
            // Post notification that ReportStore will handle
            NotificationCenter.default.post(
                name: .reportReady,
                object: nil,
                userInfo: userInfo
            )
            
            completionHandler(.newData)
        } else {
            completionHandler(.noData)
        }
    }
    
    // Called when user taps on notification
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        
        AppLogger.log(.debug, "User tapped notification", category: .general)
        
        let userInfo = response.notification.request.content.userInfo
        
        // Check if this is a report notification
        if let sessionId = userInfo["sessionId"] as? String {
            AppLogger.log(.info, "Report ready for session: \(sessionId)", category: .general)
            
            // Clear this specific notification
            center.removeDeliveredNotifications(
                withIdentifiers: [response.notification.request.identifier]
            )
            
            // Update badge count
            Task {
                await updateBadgeAfterNotificationTap()
            }
            
            // Post notification to update UI
            NotificationCenter.default.post(
                name: .reportReady,
                object: nil,
                userInfo: ["sessionId": sessionId, "openReport": true]
            )
        }
        
        completionHandler()
    }
    
    @MainActor
    private func updateBadgeAfterNotificationTap() async {
        // Get remaining delivered notifications
        let delivered = await UNUserNotificationCenter.current().deliveredNotifications()
        
        // Update badge to reflect remaining count
        UIApplication.shared.applicationIconBadgeNumber = delivered.count
        
        if delivered.count == 0 {
            AppLogger.log(.debug, "All notifications cleared", category: .general)
        } else {
            AppLogger.log(.debug, "\(delivered.count) notifications remaining", category: .general)
        }
    }
}

// Notification names for internal app communication
extension Notification.Name {
    static let reportReady = Notification.Name("reportReady")
}
