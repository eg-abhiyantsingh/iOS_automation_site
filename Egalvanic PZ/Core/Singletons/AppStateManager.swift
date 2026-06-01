//
//  AppStateManager.swift
//  SwiftDataTutorial
//
//  Global app state management singleton
//

import SwiftUI
import SwiftData

class AppStateManager: ObservableObject {
    static let shared = AppStateManager()

    @Published var activeSLDId: UUID = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
    @Published var userId: UUID = UUID(uuidString: "916be5f0-00a1-7079-0310-e888fdc35924")!
    @Published var loggedIn: Bool = false
    @Published var activeSession: IRSession?
    @Published var lastCreatedRoom: String?
    @Published var lastCreatedVisualPhotoKey: String?
    @Published var lastCreatedIRPhotoKey: String?
    @Published var isSettingUpPushNotifications = false

    /// ModelContainer for background batch operations
    var modelContainer: ModelContainer?

    func setModelContainer(_ container: ModelContainer) {
        self.modelContainer = container
    }
    
    private init() {
        // Check if user was previously logged in
        if let savedUserId = UserDefaults.standard.string(forKey: "userId"),
           let uuid = UUID(uuidString: savedUserId) {
            self.userId = uuid
            self.loggedIn = UserDefaults.standard.bool(forKey: "userLoggedIn")
        }

        // Restore active SLD from UserDefaults
        if let savedActiveSLDId = UserDefaults.standard.string(forKey: "activeSLDId"),
           let uuid = UUID(uuidString: savedActiveSLDId) {
            self.activeSLDId = uuid
        }
    }
    
    func setActiveSLD(_ id: UUID) {
        activeSLDId = id
        // Persist to UserDefaults so it survives app restarts
        UserDefaults.standard.set(id.uuidString, forKey: "activeSLDId")
    }
    
    func setActiveSession(_ session: IRSession?) {
        activeSession = session

        // Reset Quick QR Action to default when session changes
        NetworkState.shared.quickQRAction = .fullAsset
    }
    
    func setLastCreatedRoom(_ room: String?) {
        lastCreatedRoom = room
    }
    
    func setLastCreatedVisualPhotoKey(_ key: String?) {
        lastCreatedVisualPhotoKey = key
    }
    
    func setLastCreatedIRPhotoKey(_ key: String?) {
        lastCreatedIRPhotoKey = key
    }
    
    // MARK: - Authentication Orchestration
    
    /// Called after successful login to coordinate all post-login setup
    func handleSuccessfulLogin(userId: String) {
        AppLogger.log(.info, "Handling successful login for user: \(userId)", category: .auth)
        
        // Update state
        if let uuid = UUID(uuidString: userId) {
            self.userId = uuid
        }
        self.loggedIn = true
        
        // Store in UserDefaults for persistence
        UserDefaults.standard.set(userId, forKey: "userId")
        UserDefaults.standard.set(userId, forKey: "currentUserId") // For AppDelegate compatibility
        UserDefaults.standard.set(true, forKey: "userLoggedIn")
        
        // Setup push notifications (device registration happens after permission dialog)
        setupPushNotifications(for: userId)
    }

    /// Setup push notifications for the logged-in user
    private func setupPushNotifications(for userId: String) {
        guard !isSettingUpPushNotifications else {
            AppLogger.log(.debug, "Push notification setup already in progress", category: .auth)
            return
        }

        isSettingUpPushNotifications = true

        DispatchQueue.main.async {
            if let appDelegate = AppDelegate.shared {
                AppLogger.log(.debug, "Setting up push notifications via AppDelegate", category: .auth)
                appDelegate.setupPushNotificationsAfterLogin(userId: userId)
            } else {
                AppLogger.log(.error, "AppDelegate not available for push notification setup", category: .auth)
            }

            self.isSettingUpPushNotifications = false
        }
    }
    
    func logout() {
        loggedIn = false

        // Clear user data
        UserDefaults.standard.removeObject(forKey: "userId")
        UserDefaults.standard.removeObject(forKey: "currentUserId")
        UserDefaults.standard.set(false, forKey: "userLoggedIn")

        // Clear push notification data
        if let appDelegate = AppDelegate.shared {
            appDelegate.clearUserNotificationData()
        }

        // Reset active SLD to force site selection on next login
        activeSLDId = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
        UserDefaults.standard.removeObject(forKey: "activeSLDId")

        // Reset session data
        activeSession = nil
        lastCreatedRoom = nil
        lastCreatedVisualPhotoKey = nil
        lastCreatedIRPhotoKey = nil
    }
}