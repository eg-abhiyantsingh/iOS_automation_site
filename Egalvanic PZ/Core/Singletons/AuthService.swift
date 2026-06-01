import Foundation
import SwiftUI

// MARK: - Auth Models
struct LoginRequest: Codable {
    let email: String
    let password: String
    let subdomain: String?  // Optional for backward compatibility
}

struct LoginResponse: Codable {
    let access_token: String?
    let id_token: String?
    let refresh_token: String?
    let expires_in: Int?
    let expires_at: Int64?

    // Challenge response fields
    let challenge: String?
    let session: String?
    let challenge_parameters: ChallengeParameters?
}

struct ChallengeParameters: Codable {
    let USER_ID_FOR_SRP: String?
    let requiredAttributes: String?
    let userAttributes: String?
}

struct ChallengeResponse: Codable {
    let challenge_name: String
    let session: String
    let email: String
    let new_password: String
    let given_name: String
    let family_name: String
    let timezone: String
}

struct UserRole: Codable {
    let id: String
    let name: String
    let description: String?
    let is_active: Bool?
}

struct UserInfo: Codable {
    let sub: String
    let email: String?
    let email_verified: Bool?
    let name: String?
    let given_name: String?
    let family_name: String?
    let company_id: String?
    let timezone: String?
    let cognito_username: String?
    let roles: [UserRole]?
    let permissions: [String]?
    let accessible_sld_ids: [String]?
    let is_admin: Bool?
    /// ZP-2161: active company feature flags (matches web's
    /// ``useAuthStore.userDetails.company_features``). Optional so
    /// older auth responses still decode. ``hasFeature(_:)`` is the
    /// canonical lookup.
    let company_features: [String]?

    /// True when the company has the given feature flag enabled.
    func hasFeature(_ key: String) -> Bool {
        company_features?.contains(key) ?? false
    }
}

struct LogoutResponse: Codable {
    let message: String
}

struct ErrorResponse: Codable {
    let error: String?
    let message: String?
}

// MARK: - AuthService
@MainActor
class AuthService: ObservableObject {
    static let shared = AuthService()

    /// Dynamic base URL - uses company-specific invoke URL from config, falls back to Configuration.apiBaseURLString
    private var baseURL: String {
        CompanyConfigService.shared.getCurrentInvokeURL()
    }
    private let keychainService = KeychainService.shared
    // Enable logging only in DEBUG builds to avoid leaking sensitive info in production
    #if DEBUG
    private let enableLogging = true
    #else
    private let enableLogging = false
    #endif

    @Published var isAuthenticated = false
    @Published var currentUser: UserInfo?
    @Published var isLoading = false
    @Published var errorMessage: String?

    // Platform access control
    @Published var isCheckingPlatformAccess = true
    @Published var hasMobileAccess = false

    // Session expiry handling
    @Published var isSessionExpired = false
    /// ZP-1847 — true while a logout (or non-destructive account switch) is
    /// mid-flight. Used by SettingsView to prevent double-taps and to gate
    /// other lifecycle buttons.
    @Published var isLoggingOut = false

    /// ZP-2161: company feature-flag lookup (mirror of web's
    /// ``useFeatureFlag``). Returns ``true`` when the active company
    /// has the given key in ``company_features``.
    func hasFeature(_ key: String) -> Bool {
        currentUser?.hasFeature(key) ?? false
    }

    var currentUserActiveRole: String? {
        currentUser?.roles?.first(where: { $0.is_active == true })?.name
    }

    func requireCompanyId() async throws -> UUID {
        guard let companyIdString = currentUser?.company_id,
              let companyId = UUID(uuidString: companyIdString) else {
            throw NSError(domain: "AuthService", code: 0, userInfo: [
                NSLocalizedDescriptionKey: AppStrings.Site.companyIdNotFound
            ])
        }
        return companyId
    }

    private var accessToken: String? {
        get {
            keychainService.accessToken
        }
        set {
            keychainService.accessToken = newValue
            log("📝 Access token \(newValue != nil ? "saved" : "cleared")")
        }
    }

    private var refreshToken: String? {
        get {
            keychainService.refreshToken
        }
        set {
            keychainService.refreshToken = newValue
            log("📝 Refresh token \(newValue != nil ? "saved" : "cleared")")
        }
    }

    private var idToken: String? {
        get {
            keychainService.idToken
        }
        set {
            keychainService.idToken = newValue
            log("📝 ID token \(newValue != nil ? "saved" : "cleared")")
        }
    }

    private var currentSubdomain: String? {
        get {
            keychainService.subdomain
        }
        set {
            keychainService.subdomain = newValue
            log("📝 Subdomain \(newValue != nil ? "saved: \(newValue!)" : "cleared")")
        }
    }

    private var expiresAt: Date? {
        get { keychainService.expiresAt }
        set {
            keychainService.expiresAt = newValue
            if let date = newValue {
                log("📝 Expiry saved: \(date)")
            } else {
                log("📝 Expiry cleared")
            }
        }
    }

    /// 3-level fallback: server `expires_at` (Unix sec) → JWT `exp` → now + expires_in.
    /// Returns nil only if all sources are missing.
    private func computeExpiresAt(
        serverExpiresAt: Int64?,
        accessToken: String?,
        expiresIn: Int?
    ) -> Date? {
        if let s = serverExpiresAt, s > 0 {
            return Date(timeIntervalSince1970: TimeInterval(s))
        }
        if let t = accessToken, let exp = JwtUtils.decodeJwtExp(t) {
            return Date(timeIntervalSince1970: exp)
        }
        if let i = expiresIn {
            return Date().addingTimeInterval(TimeInterval(i))
        }
        return nil
    }

    /// Returns the access-token expiry, lazily backfilling from the JWT `exp` claim
    /// for users updating from a pre-change build.
    func getExpiresAt() -> Date? {
        if let stored = expiresAt { return stored }
        if let token = accessToken, let exp = JwtUtils.decodeJwtExp(token) {
            let date = Date(timeIntervalSince1970: exp)
            self.expiresAt = date
            return date
        }
        return nil
    }
    
    private init() {
        log("🚀 AuthService initialized")
        // Check for persisted session expired state (survives app kill)
        if keychainService.sessionExpiredPendingReauth {
            log("⚠️ Found persisted session expired flag - will show re-auth sheet")
            isSessionExpired = true
        }
        // Check if user has valid tokens on init
        Task {
            await checkAuthStatus()
        }
    }
    
    // MARK: - Logging Helper
    private func log(_ message: String) {
        if enableLogging {
            // Determine log level based on message content
            let level: SentryLogger.Level
            if message.contains("❌") || message.lowercased().contains("error") || message.lowercased().contains("failed") {
                level = .error
            } else if message.contains("⚠️") || message.lowercased().contains("warning") {
                level = .warning
            } else {
                level = .info
            }
            slog(message, category: .auth, level: level)
        }
    }
    
    // MARK: - Public Methods
    
    func login(email: String, password: String, subdomain: String? = nil) async throws -> (success: Bool, needsNewPassword: Bool, session: String?) {
        log("🔐 Starting login for email: \(email), subdomain: \(subdomain ?? "default")")
        isLoading = true
        errorMessage = nil

        defer {
            isLoading = false
            log("🔐 Login process completed")
        }

        let loginRequest = LoginRequest(email: email, password: password, subdomain: subdomain)
        
        guard let url = URL(string: "\(baseURL)\(APIEndpoints.Auth.login)") else {
            log("❌ Invalid URL: \(baseURL)\(APIEndpoints.Auth.login)")
            throw AuthError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let subdomain = subdomain {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        }
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        do {
            let jsonData = try JSONEncoder().encode(loginRequest)
            request.httpBody = jsonData
            
            // Log request details
            log("📤 Request URL: \(url.absoluteString)")
            log("📤 Request Method: POST")
            log("📤 Request Body: \(String(data: jsonData, encoding: .utf8) ?? "nil")")
            
        } catch {
            log("❌ Failed to encode login request: \(error)")
            throw error
        }
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            // Log raw response
            log("📥 Response received")
            if let httpResponse = response as? HTTPURLResponse {
                log("📥 Status Code: \(httpResponse.statusCode)")
                log("📥 Headers: \(httpResponse.allHeaderFields)")
            }
            
            // Log raw data
            if let responseString = String(data: data, encoding: .utf8) {
                log("📥 Raw Response Data: \(responseString)")
            } else {
                log("📥 Raw Response Data: \(data.count) bytes (not UTF-8)")
            }
            
            guard let httpResponse = response as? HTTPURLResponse else {
                log("❌ Response is not HTTPURLResponse")
                throw AuthError.invalidResponse
            }
            
            switch httpResponse.statusCode {
            case 200:
                log("✅ 200 OK - Attempting to decode login response")
                
                do {
                    let loginResponse = try JSONDecoder().decode(LoginResponse.self, from: data)
                    
                    // Log decoded response
                    log("📦 Decoded Response:")
                    log("  - Has access_token: \(loginResponse.access_token != nil)")
                    log("  - Has refresh_token: \(loginResponse.refresh_token != nil)")
                    log("  - Has id_token: \(loginResponse.id_token != nil)")
                    log("  - Has challenge: \(loginResponse.challenge != nil)")
                    log("  - Challenge type: \(loginResponse.challenge ?? "none")")
                    
                    // Check if this is a challenge response
                    if let challenge = loginResponse.challenge,
                       challenge == "NEW_PASSWORD_REQUIRED",
                       let session = loginResponse.session {
                        log("🔄 NEW_PASSWORD_REQUIRED challenge detected")
                        // Store subdomain early so it's available for respondToNewPasswordChallenge
                        self.currentSubdomain = subdomain
                        return (false, true, session)
                    }
                    
                    // Normal login success
                    if let accessToken = loginResponse.access_token,
                       let refreshToken = loginResponse.refresh_token {
                        log("✅ Login successful, saving tokens")
                        self.accessToken = accessToken
                        self.refreshToken = refreshToken
                        self.idToken = loginResponse.id_token
                        self.currentSubdomain = subdomain
                        self.expiresAt = computeExpiresAt(
                            serverExpiresAt: loginResponse.expires_at,
                            accessToken: accessToken,
                            expiresIn: loginResponse.expires_in
                        )

                        // Fetch user info
                        log("👤 Fetching user info...")
                        try await fetchUserInfo()
                        
                        isAuthenticated = true
                        log("✅ Authentication complete")
                        return (true, false, nil)
                    }
                    
                    log("❌ Response missing required tokens")
                    throw AuthError.invalidResponse
                    
                } catch let decodingError as DecodingError {
                    log("❌ Failed to decode LoginResponse: \(decodingError)")
                    log("❌ Decoding error details: \(String(describing: decodingError))")
                    
                    // Try to decode as error response
                    if let errorResponse = try? JSONDecoder().decode(ErrorResponse.self, from: data) {
                        log("📦 Decoded as ErrorResponse: \(errorResponse.error ?? errorResponse.message ?? "unknown")")
                        throw AuthError.serverError(errorResponse.error ?? errorResponse.message ?? "Unknown error")
                    }
                    
                    throw decodingError
                }
                
            case 400:
                log("❌ 400 Bad Request")
                if let errorResponse = try? JSONDecoder().decode(ErrorResponse.self, from: data) {
                    log("📦 Error: \(errorResponse.error ?? errorResponse.message ?? "unknown")")
                    throw AuthError.badRequest(errorResponse.error ?? errorResponse.message ?? "Missing email or password")
                }
                throw AuthError.badRequest("Missing email or password")
                
            case 401:
                log("❌ 401 Unauthorized")
                if let errorResponse = try? JSONDecoder().decode(ErrorResponse.self, from: data) {
                    log("📦 Error: \(errorResponse.error ?? errorResponse.message ?? "unknown")")
                    throw AuthError.unauthorized(errorResponse.error ?? errorResponse.message ?? "Invalid credentials")
                }
                throw AuthError.unauthorized("Invalid credentials")
                
            case 403:
                log("❌ 403 Forbidden")
                if let errorResponse = try? JSONDecoder().decode(ErrorResponse.self, from: data) {
                    log("📦 Error: \(errorResponse.error ?? errorResponse.message ?? "unknown")")
                    throw AuthError.forbidden(errorResponse.error ?? errorResponse.message ?? "User not confirmed")
                }
                throw AuthError.forbidden("User not confirmed")
                
            default:
                log("❌ Unexpected status code: \(httpResponse.statusCode)")
                if let errorResponse = try? JSONDecoder().decode(ErrorResponse.self, from: data) {
                    log("📦 Error: \(errorResponse.error ?? errorResponse.message ?? "unknown")")
                    throw AuthError.serverError(errorResponse.error ?? errorResponse.message ?? "Server error")
                }
                throw AuthError.unknownError
            }
            
        } catch {
            log("❌ Network or parsing error: \(error)")
            log("❌ Error type: \(type(of: error))")
            log("❌ Error description: \(error.localizedDescription)")
            throw error
        }
    }
    
    func respondToNewPasswordChallenge(
        session: String,
        email: String,
        newPassword: String,
        givenName: String,
        familyName: String,
        timezone: String = "America/New_York"
    ) async throws {
        log("🔄 Responding to NEW_PASSWORD_REQUIRED challenge")
        isLoading = true
        errorMessage = nil
        
        defer { isLoading = false }
        
        let challengeResponse = ChallengeResponse(
            challenge_name: "NEW_PASSWORD_REQUIRED",
            session: session,
            email: email,
            new_password: newPassword,
            given_name: givenName,
            family_name: familyName,
            timezone: timezone
        )
        
        guard let url = URL(string: "\(baseURL)\(APIEndpoints.Auth.respondToChallenge)") else {
            log("❌ Invalid URL for challenge response")
            throw AuthError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        // Add subdomain header for multi-tenant support
        if let subdomain = currentSubdomain {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        }
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        request.httpBody = try JSONEncoder().encode(challengeResponse)

        log("📤 Sending challenge response to: \(url.absoluteString)")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        // Log response
        if let responseString = String(data: data, encoding: .utf8) {
            log("📥 Challenge Response: \(responseString)")
        }
        
        guard let httpResponse = response as? HTTPURLResponse else {
            log("❌ Invalid response type")
            throw AuthError.invalidResponse
        }
        
        log("📥 Challenge Response Status: \(httpResponse.statusCode)")
        
        if httpResponse.statusCode == 200 {
            let loginResponse = try JSONDecoder().decode(LoginResponse.self, from: data)
            
            if let accessToken = loginResponse.access_token,
               let refreshToken = loginResponse.refresh_token {
                log("✅ Challenge completed successfully")
                self.accessToken = accessToken
                self.refreshToken = refreshToken
                self.idToken = loginResponse.id_token
                self.expiresAt = computeExpiresAt(
                    serverExpiresAt: loginResponse.expires_at,
                    accessToken: accessToken,
                    expiresIn: loginResponse.expires_in
                )
                // Note: currentSubdomain should already be set from initial login

                // Fetch user info
                try await fetchUserInfo()
                
                isAuthenticated = true
            } else {
                log("❌ Challenge response missing tokens")
                throw AuthError.invalidResponse
            }
        } else {
            log("❌ Challenge failed with status: \(httpResponse.statusCode)")
            if let errorResponse = try? JSONDecoder().decode(ErrorResponse.self, from: data) {
                throw AuthError.serverError(errorResponse.error ?? errorResponse.message ?? "Unknown error")
            }
            throw AuthError.unknownError
        }
    }
    
    // MARK: - Forgot / Reset Password

    func forgotPassword(email: String, subdomain: String) async throws -> String? {
        log("🔐 Requesting password reset code for: \(email)")

        guard let url = URL(string: "\(baseURL)\(APIEndpoints.Auth.forgotPassword)") else {
            throw AuthError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        let body: [String: String] = ["email": email, "subdomain": subdomain]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AuthError.invalidResponse
        }

        switch httpResponse.statusCode {
        case 200...299:
            log("✅ Password reset code sent successfully")
            return decodeMessage(from: data)
        case 400:
            throw AuthError.badRequest(decodeMessage(from: data) ?? "Email is required")
        case 404:
            throw AuthError.unauthorized(decodeMessage(from: data) ?? "Invalid credentials")
        default:
            throw AuthError.serverError(decodeMessage(from: data) ?? "Something went wrong. Please try again.")
        }
    }

    func resetPassword(email: String, code: String, newPassword: String, subdomain: String) async throws -> String? {
        log("🔐 Resetting password for: \(email)")

        guard let url = URL(string: "\(baseURL)\(APIEndpoints.Auth.resetPassword)") else {
            throw AuthError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        let body: [String: String] = ["email": email, "code": code, "new_password": newPassword]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AuthError.invalidResponse
        }

        switch httpResponse.statusCode {
        case 200...299:
            log("✅ Password reset successfully")
            return decodeMessage(from: data)
        case 400:
            throw AuthError.badRequest(decodeMessage(from: data) ?? "Invalid or incorrect reset code")
        case 401:
            throw AuthError.unauthorized(decodeMessage(from: data) ?? "Invalid credentials")
        case 410:
            throw AuthError.serverError(decodeMessage(from: data) ?? "Reset code has expired")
        default:
            throw AuthError.serverError(decodeMessage(from: data) ?? "Something went wrong. Please try again.")
        }
    }

    private func decodeMessage(from data: Data) -> String? {
        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let message = json["message"] as? String {
            return message
        }
        return nil
    }

    /// ZP-1847 — guarded logout entry point.
    ///
    /// Refuses to proceed if the device is offline, a sync is running, or
    /// a logout is already in flight. The server logout request must
    /// succeed; on any failure (except 401 which is treated as already
    /// logged out) we abort and leave the user signed in so they can
    /// retry. Local state is cleared only on success.
    ///
    /// `userScopedQueueCount` is informational only — the queue itself is
    /// not cleared on logout; it persists per-user on device.
    @MainActor
    func requestLogout() async throws {
        guard !isLoggingOut else { throw AuthError.serverError("Logout already in progress") }
        guard !NetworkState.shared.isSyncing else { throw AuthError.syncing }
        guard NetworkState.shared.isOnline else { throw AuthError.offline }

        isLoggingOut = true
        defer { isLoggingOut = false }

        try await performServerLogoutOrThrow()
        // ZP-2183: do NOT call logout() here — it would fire /auth/v2/logout a
        // second time. requestLogout() owns the authoritative server call;
        // we only need the local cleanup now.
        await performLocalLogoutCleanup()
    }

    /// Hits the server `/auth/v2/logout` endpoint. Throws on any non-2xx
    /// response (except 401, which we treat as success — the token is
    /// already invalid server-side, which is what we wanted anyway).
    private func performServerLogoutOrThrow() async throws {
        guard let token = accessToken else {
            // No token → nothing to invalidate server-side. Treat as success.
            return
        }
        guard let url = URL(string: "\(baseURL)\(APIEndpoints.Auth.logout)") else {
            throw AuthError.invalidURL
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        if let subdomain = currentSubdomain {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        }
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        let (_, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AuthError.invalidResponse
        }
        switch httpResponse.statusCode {
        case 200..<300, 401:
            return
        default:
            throw AuthError.serverError("Logout failed (status \(httpResponse.statusCode))")
        }
    }

    func logout() async {
        log("🚪 Starting logout")
        
        // Try to call server logout endpoint if we have a token, but don't fail if it doesn't work
        if let token = accessToken,
           let url = URL(string: "\(baseURL)\(APIEndpoints.Auth.logout)") {
            
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

            // Add subdomain header for multi-tenant support
            if let subdomain = currentSubdomain {
                request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
            }
            request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

            log("📤 Attempting server logout")
            
            do {
                let (data, response) = try await URLSession.shared.data(for: request)
                
                if let responseString = String(data: data, encoding: .utf8) {
                    log("📥 Logout Response: \(responseString)")
                }
                
                if let httpResponse = response as? HTTPURLResponse {
                    log("📥 Server logout status: \(httpResponse.statusCode)")
                }
            } catch {
                // Don't throw - just log the error and continue with local logout
                log("⚠️ Server logout failed (continuing with local logout): \(error)")
            }
        } else {
            log("⚠️ No token available for server logout, proceeding with local logout")
        }

        await performLocalLogoutCleanup()
    }

    /// Local-only logout cleanup. Does NOT hit the server. Called by `logout()`
    /// after its best-effort server call and by `requestLogout()` after the
    /// authoritative `performServerLogoutOrThrow()` call (so the latter doesn't
    /// fire a duplicate `/auth/v2/logout`, see ZP-2183).
    private func performLocalLogoutCleanup() async {
        log("✅ Clearing local auth data")

        // Let AppStateManager handle the logout orchestration
        await MainActor.run {
            AppStateManager.shared.logout()
        }

        // Clear auth tokens locally regardless of server response
        clearAuthData()

        log("✅ Local logout complete")
    }

    // MARK: - Session Expiry Handling

    /// Called when refresh token has expired. Instead of logging out immediately,
    /// this sets a flag to show the re-authentication sheet, preserving user data.
    func handleSessionExpired() {
        log("🔒 Session expired - triggering re-authentication flow")

        // If we don't have a stored email, we can't show the re-auth sheet
        // This handles existing users who update the app with expired tokens
        // Fall back to normal logout (same behavior as before this feature)
        guard keychainService.storedUserEmail != nil else {
            log("⚠️ No stored email for re-auth - falling back to normal logout")
            Task {
                await logout()
            }
            return
        }

        // Set persisted flag (survives app kill)
        keychainService.sessionExpiredPendingReauth = true

        // Trigger UI to show re-auth sheet
        isSessionExpired = true

        // DO NOT clear auth data - we want to preserve it for re-auth
        // DO NOT set isAuthenticated = false - user should stay on current screen
        log("✅ Session expired flag set - re-auth sheet will be shown")
    }

    /// Re-authenticate with password only (email is pre-filled from stored data)
    /// Returns true on success, throws error on failure
    func reAuthenticate(password: String) async throws -> Bool {
        log("🔐 Attempting re-authentication")
        isLoading = true
        errorMessage = nil

        defer { isLoading = false }

        // Get stored email
        guard let email = keychainService.storedUserEmail else {
            log("❌ No stored email for re-authentication")
            throw AuthError.notAuthenticated
        }

        // Get stored subdomain
        let subdomain = currentSubdomain

        log("🔐 Re-authenticating user: \(email)")

        // Use existing login flow
        let (success, needsNewPassword, _) = try await login(email: email, password: password, subdomain: subdomain)

        if success && !needsNewPassword {
            // Clear the session expired flag
            clearSessionExpiredFlag()
            log("✅ Re-authentication successful")
            return true
        } else if needsNewPassword {
            log("❌ User needs new password - cannot re-authenticate")
            throw AuthError.serverError("Password change required. Please contact support.")
        }

        return false
    }

    /// Clear the session expired state (called after successful re-auth)
    func clearSessionExpiredFlag() {
        log("🧹 Clearing session expired flag")
        keychainService.sessionExpiredPendingReauth = false
        isSessionExpired = false
    }

    /// Perform full account switch - clears auth data and returns to login
    /// This is called when user explicitly chooses to switch account from re-auth sheet
    /// Note: Local database data will be cleared when user logs in to a new site
    func performSwitchUser() async {
        log("🔄 Performing account switch - clearing auth data")

        // Clear session expired flag first
        clearSessionExpiredFlag()

        // Clear stored user email since user is switching accounts
        keychainService.clearSessionData()

        // Perform regular logout (clears tokens, app state, etc.)
        await logout()

        log("✅ Account switch complete - user redirected to login")
    }

    func fetchUserInfo() async throws {
        log("👤 Fetching user info")

        // V2 contract: /auth/v2/me only accepts access_token; id_token returns 401.
        guard let token = accessToken else {
            log("❌ No access token for user info")
            throw AuthError.notAuthenticated
        }

        guard let url = URL(string: "\(baseURL)\(APIEndpoints.Auth.me)") else {
            log("❌ Invalid user info URL")
            throw AuthError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        // Add subdomain header if we have one
        if let subdomain = currentSubdomain {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
            log("📤 Including X-Subdomain header: \(subdomain)")
        }
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        log("📤 Requesting user info")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        if let responseString = String(data: data, encoding: .utf8) {
            log("📥 User Info Response: \(responseString)")
        }
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            log("❌ Failed to fetch user info")
            throw AuthError.invalidResponse
        }
        
        currentUser = try JSONDecoder().decode(UserInfo.self, from: data)

        // Store user email for re-authentication (works for both new and existing users)
        if let email = currentUser?.email {
            keychainService.storedUserEmail = email
            log("📝 Stored user email for re-authentication: \(email)")
        }

        // Set Sentry user context for error tracking
        if let user = currentUser {
            SentryLogger.shared.setUser(
                id: user.sub,
                email: user.email,
                companyId: user.company_id,
                subdomain: currentSubdomain
            )
        }

        // Identify user with DevRev for in-app support
        if let user = currentUser {
            Task {
                await DevRevService.shared.identifyUser(
                    userId: user.sub,
                    email: user.email,
                    displayName: user.name
                )
            }
        }

        // Check platform permissions
        await checkPlatformAccess()

        // Let AppStateManager orchestrate the post-login flow
        if let userId = currentUser?.sub {
            log("✅ User info fetched, sub: \(userId)")

            // AppStateManager handles all the orchestration
            await MainActor.run {
                AppStateManager.shared.handleSuccessfulLogin(userId: userId)
            }
        }
    }
    
    func checkAuthStatus() async {
        log("🔍 Checking auth status")

        // Check if we have tokens
        guard accessToken != nil else {
            log("❌ No access token found")
            isAuthenticated = false
            return
        }

        // Check network connectivity
        let isOffline = await !NetworkReachability.shared.isConnected

        if isOffline {
            // When offline, trust that existing tokens are valid
            // This allows users to access the app offline after logging in
            log("📵 Offline mode detected - trusting existing tokens")
            isAuthenticated = true

            // Also trust platform access when offline (user wouldn't have logged in successfully without it)
            isCheckingPlatformAccess = false
            hasMobileAccess = true

            // The app will use locally stored data from SwiftData
            log("✅ User can access app with offline data")
            return
        }

        log("🔍 Online - validating tokens...")

        // Try to fetch user info to validate token
        do {
            try await fetchUserInfo()
            isAuthenticated = true
            // Clear any stale session expired flag since tokens are valid
            if keychainService.sessionExpiredPendingReauth {
                log("🧹 Clearing stale session expired flag - tokens are valid")
                clearSessionExpiredFlag()
            }
            log("✅ Auth status: Authenticated")
        } catch {
            log("⚠️ Token validation failed: \(error)")

            // Check if this is a network error vs auth error
            if isNetworkError(error) {
                // Network issue - don't logout, trust existing tokens
                log("📵 Network error during validation - preserving authentication")
                isAuthenticated = true
                // Also set platform access flags to prevent blocking UI
                isCheckingPlatformAccess = false
                hasMobileAccess = true
                return
            }

            // Token might be expired, try to refresh
            if refreshToken != nil {
                log("🔄 Attempting token refresh")
                do {
                    try await refreshAccessToken()
                    try await fetchUserInfo()
                    isAuthenticated = true
                    log("✅ Token refreshed successfully")
                } catch {
                    log("❌ Token refresh failed: \(error)")

                    // Only logout if refresh failed due to auth issues, not network
                    if isNetworkError(error) {
                        log("📵 Network error during refresh - preserving authentication")
                        isAuthenticated = true
                        // Set platform access flags to prevent blocking UI
                        isCheckingPlatformAccess = false
                        hasMobileAccess = true
                    } else {
                        // Actual auth failure (401/403) - refresh token is invalid
                        // Use session expired handling instead of full logout to preserve data
                        log("🔒 Refresh token invalid - triggering re-authentication")
                        handleSessionExpired()
                    }
                }
            } else {
                log("❌ No refresh token available")
                clearAuthData()
            }
        }
    }

    // Helper to identify network errors
    private func isNetworkError(_ error: Error) -> Bool {
        if let urlError = error as? URLError {
            return urlError.code == .notConnectedToInternet ||
                   urlError.code == .timedOut ||
                   urlError.code == .networkConnectionLost ||
                   urlError.code == .dataNotAllowed ||
                   urlError.code == .cannotFindHost ||
                   urlError.code == .cannotConnectToHost
        }
        return false
    }
    
    func refreshAccessToken() async throws {
        log("🔄 Refreshing access token")
        
        guard let refreshToken = refreshToken else {
            log("❌ No refresh token available")
            throw AuthError.notAuthenticated
        }
        
        guard let url = URL(string: "\(baseURL)\(APIEndpoints.Auth.refresh)") else {
            log("❌ Invalid refresh URL")
            throw AuthError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        // Add subdomain header if we have one
        if let subdomain = currentSubdomain {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
            log("📤 Including X-Subdomain header for refresh: \(subdomain)")
        }
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        // CRITICAL: Include subdomain in request body for backend refresh endpoint
        var body: [String: String] = ["refresh_token": refreshToken]
        if let subdomain = currentSubdomain {
            body["subdomain"] = subdomain
            log("📤 Including subdomain in refresh request body: \(subdomain)")
        }
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        log("📤 Sending refresh request")

        let (data, response) = try await URLSession.shared.data(for: request)

        if let responseString = String(data: data, encoding: .utf8) {
            log("📥 Refresh Response: \(responseString)")
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            log("❌ Invalid response type")
            throw AuthError.invalidResponse
        }

        // Check for auth failures specifically
        if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
            log("❌ Refresh token is invalid or expired (Status: \(httpResponse.statusCode))")
            throw AuthError.notAuthenticated
        }

        guard httpResponse.statusCode == 200 else {
            log("❌ Token refresh failed with status: \(httpResponse.statusCode)")
            throw AuthError.invalidResponse
        }
        
        let loginResponse = try JSONDecoder().decode(LoginResponse.self, from: data)
        
        if let newAccessToken = loginResponse.access_token {
            log("✅ Access token refreshed")
            self.accessToken = newAccessToken
            self.idToken = loginResponse.id_token
            // Cognito may rotate the refresh token on each refresh — overwrite when present
            // so the stored token always matches what the server expects.
            if let newRefresh = loginResponse.refresh_token {
                self.refreshToken = newRefresh
            }
            self.expiresAt = computeExpiresAt(
                serverExpiresAt: loginResponse.expires_at,
                accessToken: newAccessToken,
                expiresIn: loginResponse.expires_in
            )
        } else {
            log("❌ No new access token in response")
            throw AuthError.invalidResponse
        }
    }
    
    func getAccessToken() -> String? {
        return accessToken
    }

    func getCurrentSubdomain() -> String? {
        return currentSubdomain
    }

    private func clearAuthData() {
        log("🧹 Clearing auth data")

        // Clear Sentry user context
        SentryLogger.shared.clearUser()

        // Clear DevRev user session
        Task { await DevRevService.shared.clearSession() }

        // Clear all tokens from keychain
        keychainService.clearAllTokens()
        currentUser = nil
        isAuthenticated = false
        hasMobileAccess = false
        isCheckingPlatformAccess = true

        // AppStateManager handles UserDefaults cleanup
        log("✅ Auth data cleared")
    }

    // MARK: - Platform Access Control

    /// Check if user has platform.mobile permission
    func checkPlatformAccess() async {
        log("🔐 Checking platform access permissions")
        isCheckingPlatformAccess = true

        defer {
            isCheckingPlatformAccess = false
        }

        guard let permissions = currentUser?.permissions else {
            log("⚠️ No permissions found in user info")
            hasMobileAccess = false
            return
        }

        // Check for platform.mobile permission
        let hasPermission = permissions.contains("platform.mobile")

        // Admins have access to everything
        let isAdmin = currentUser?.is_admin ?? false

        hasMobileAccess = hasPermission || isAdmin

        if hasMobileAccess {
            log("✅ User has mobile platform access")
        } else {
            log("❌ User does NOT have mobile platform access")
            log("   Current permissions: \(permissions)")
            log("   Active role: \(currentUserActiveRole ?? "none")")
        }
    }
}

// MARK: - Auth Errors
enum AuthError: LocalizedError {
    case invalidURL
    case invalidResponse
    case notAuthenticated
    case badRequest(String)
    case unauthorized(String)
    case forbidden(String)
    case serverError(String)
    case unknownError
    /// ZP-1847 — logout / account switch refused because the device is
    /// offline. The user must come back online before they can sign out.
    case offline
    /// ZP-1847 — logout / account switch refused because a sync is in
    /// progress. Once the flush finishes, the action becomes available.
    case syncing

    /// Returns true if the error is a session-expired error that is already
    /// handled by the re-authentication sheet flow.
    /// Only matches the specific errors that trigger `handleSessionExpired()`:
    /// - `AuthError.notAuthenticated` (refresh token expired/invalid)
    /// - `URLError.userAuthenticationRequired` (401 after max retries)
    /// Other AuthError cases (e.g., .invalidResponse, .serverError) are NOT
    /// suppressed, so the user still sees alerts for unexpected server errors.
    static func isAuthError(_ error: Error) -> Bool {
        if case AuthError.notAuthenticated = error { return true }
        if let urlError = error as? URLError, urlError.code == .userAuthenticationRequired { return true }
        return false
    }

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .notAuthenticated:
            return "Not authenticated"
        case .badRequest(let message):
            return "Bad request: \(message)"
        case .unauthorized(let message):
            return "Unauthorized: \(message)"
        case .forbidden(let message):
            return "Forbidden: \(message)"
        case .serverError(let message):
            return "Server error: \(message)"
        case .unknownError:
            return "An unknown error occurred"
        case .offline:
            return "You are offline. Please reconnect and try again."
        case .syncing:
            return "A sync is in progress. Please wait for it to finish, then try again."
        }
    }
}
