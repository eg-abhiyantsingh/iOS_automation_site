import Foundation
import Security

class KeychainService {
    static let shared = KeychainService()

    private let serviceName = "com.eg.projectz"
    private let accountEmail = "biometric.email"
    private let accountPassword = "biometric.password"
    private let biometricEnabledKey = "biometricAuthEnabled"

    // Token account names
    private let accountAccessToken = "auth.accessToken"
    private let accountRefreshToken = "auth.refreshToken"
    private let accountIdToken = "auth.idToken"
    private let accountSubdomain = "auth.subdomain"
    private let accountExpiresAt = "auth.expiresAt"

    // Session expiry and user email storage
    private let sessionExpiredKey = "auth.sessionExpiredPendingReauth"
    private let userEmailKey = "auth.userEmail"

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
            AppLogger.log(.debug, "[KeychainService] \(message)", category: .auth)
        }
    }

    // MARK: - Public Methods

    var isBiometricAuthEnabled: Bool {
        get {
            UserDefaults.standard.bool(forKey: biometricEnabledKey)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: biometricEnabledKey)
            if !newValue {
                // Clear stored credentials when disabling
                _ = deleteCredentials()
            }
        }
    }

    func saveCredentials(email: String, password: String) -> Bool {
        log("📝 Saving credentials for \(email)")
        let emailSaved = save(password: email, account: accountEmail)
        log("  - Email saved: \(emailSaved)")
        let passwordSaved = save(password: password, account: accountPassword)
        log("  - Password saved: \(passwordSaved)")
        return emailSaved && passwordSaved
    }

    func retrieveCredentials() -> (email: String, password: String)? {
        guard let email = retrieve(account: accountEmail),
              let password = retrieve(account: accountPassword) else {
            return nil
        }
        return (email, password)
    }

    func deleteCredentials() -> Bool {
        let emailDeleted = delete(account: accountEmail)
        let passwordDeleted = delete(account: accountPassword)
        return emailDeleted && passwordDeleted
    }

    func hasStoredCredentials() -> Bool {
        return retrieveCredentials() != nil
    }

    // MARK: - Token Storage Methods

    var accessToken: String? {
        get { retrieve(account: accountAccessToken) }
        set {
            if let token = newValue {
                _ = save(password: token, account: accountAccessToken)
            } else {
                _ = delete(account: accountAccessToken)
            }
        }
    }

    var refreshToken: String? {
        get { retrieve(account: accountRefreshToken) }
        set {
            if let token = newValue {
                _ = save(password: token, account: accountRefreshToken)
            } else {
                _ = delete(account: accountRefreshToken)
            }
        }
    }

    var idToken: String? {
        get { retrieve(account: accountIdToken) }
        set {
            if let token = newValue {
                _ = save(password: token, account: accountIdToken)
            } else {
                _ = delete(account: accountIdToken)
            }
        }
    }

    var subdomain: String? {
        get { retrieve(account: accountSubdomain) }
        set {
            if let subdomain = newValue {
                _ = save(password: subdomain, account: accountSubdomain)
            } else {
                _ = delete(account: accountSubdomain)
            }
        }
    }

    /// Access token expiry, persisted as Unix epoch seconds (string-encoded).
    var expiresAt: Date? {
        get {
            guard let s = retrieve(account: accountExpiresAt),
                  let seconds = TimeInterval(s) else { return nil }
            return Date(timeIntervalSince1970: seconds)
        }
        set {
            if let date = newValue {
                let seconds = Int64(date.timeIntervalSince1970)
                _ = save(password: String(seconds), account: accountExpiresAt)
            } else {
                _ = delete(account: accountExpiresAt)
            }
        }
    }

    func clearAllTokens() {
        _ = delete(account: accountAccessToken)
        _ = delete(account: accountRefreshToken)
        _ = delete(account: accountIdToken)
        _ = delete(account: accountSubdomain)
        _ = delete(account: accountExpiresAt)
    }

    // MARK: - Session Expiry Storage

    /// Flag to persist session expired state across app restarts
    /// When true, the re-authentication sheet should be shown on app launch
    var sessionExpiredPendingReauth: Bool {
        get {
            UserDefaults.standard.bool(forKey: sessionExpiredKey)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: sessionExpiredKey)
            log("📝 Session expired flag set to: \(newValue)")
        }
    }

    /// Stored user email for re-authentication
    /// This is stored separately from biometric credentials so it's available even without biometric
    var storedUserEmail: String? {
        get { retrieve(account: userEmailKey) }
        set {
            if let email = newValue {
                _ = save(password: email, account: userEmailKey)
                log("📝 Stored user email: \(email)")
            } else {
                _ = delete(account: userEmailKey)
                log("🗑️ Cleared stored user email")
            }
        }
    }

    /// Clear session-related data (used during switch user)
    func clearSessionData() {
        sessionExpiredPendingReauth = false
        storedUserEmail = nil
    }

    // MARK: - Private Keychain Methods

    private func save(password: String, account: String) -> Bool {
        guard let passwordData = password.data(using: .utf8) else {
            log("❌ Failed to convert password to data")
            return false
        }

        // Delete any existing item first
        _ = delete(account: account)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceName,
            kSecAttrAccount as String: account,
            kSecValueData as String: passwordData,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        let status = SecItemAdd(query as CFDictionary, nil)

        if status == errSecSuccess {
            log("✅ Keychain save successful for account: \(account)")
        } else {
            log("❌ Keychain save failed for account: \(account), status: \(status)")
        }

        return status == errSecSuccess
    }

    private func retrieve(account: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceName,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var dataTypeRef: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)

        guard status == errSecSuccess,
              let data = dataTypeRef as? Data,
              let password = String(data: data, encoding: .utf8) else {
            return nil
        }

        return password
    }

    private func delete(account: String) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceName,
            kSecAttrAccount as String: account
        ]

        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }
}