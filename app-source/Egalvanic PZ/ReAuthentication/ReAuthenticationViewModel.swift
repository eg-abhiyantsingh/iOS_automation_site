import Foundation
import SwiftUI
import SwiftData

/// ViewModel for the re-authentication bottom sheet
/// Handles password validation, re-authentication, and switch user flows
@MainActor
class ReAuthenticationViewModel: ObservableObject {
    // MARK: - Published Properties

    @Published var email: String = ""
    @Published var password: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var passwordError: String?
    @Published var pendingSyncCount: Int = 0
    @Published var showSwitchUserWarning: Bool = false
    @Published var biometricAvailable: Bool = false
    @Published var biometricEnabled: Bool = false
    @Published var biometricType: BiometricAuthService.BiometricType = .none

    // MARK: - Callbacks

    var onReAuthSuccess: (() -> Void)?
    var onSwitchUserSuccess: (() -> Void)?

    // MARK: - Private Properties

    private let authService = AuthService.shared
    private let biometricService = BiometricAuthService.shared
    private let keychainService = KeychainService.shared
    private let networkState = NetworkState.shared
    private let sldService = SLDService.shared
    private var modelContext: ModelContext?

    // MARK: - Initialization

    init() {
        // Defer loading to avoid "Publishing changes from within view updates" warning
        Task { @MainActor in
            loadInitialData()
        }
    }

    /// Set the model context for database operations
    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
    }

    // MARK: - Public Methods

    /// Load initial data including stored email and biometric availability
    func loadInitialData() {
        // Get stored email
        email = keychainService.storedUserEmail ?? ""

        // Get biometric info (BiometricAuthService already checked availability in its init)
        biometricType = biometricService.biometricType
        biometricAvailable = biometricService.canUseBiometric()
        biometricEnabled = biometricService.isBiometricAuthEnabled

        // Get pending sync count
        pendingSyncCount = networkState.syncQueueCount
    }

    /// Update password field
    func onPasswordChange(_ newPassword: String) {
        password = newPassword
        passwordError = nil
        errorMessage = nil
    }

    /// Validate password input
    private func validatePassword() -> Bool {
        if password.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            passwordError = "Password is required"
            return false
        }
        return true
    }

    /// Re-authenticate with password
    func reAuthenticateWithPassword() {
        guard validatePassword() else { return }

        Task {
            isLoading = true
            errorMessage = nil

            do {
                let success = try await authService.reAuthenticate(password: password)
                if success {
                    isLoading = false
                    onReAuthSuccess?()
                } else {
                    isLoading = false
                    errorMessage = "Re-authentication failed"
                }
            } catch let error as AuthError {
                isLoading = false
                switch error {
                case .unauthorized(let message):
                    errorMessage = message.isEmpty ? "Invalid password" : message
                case .serverError(let message):
                    errorMessage = message
                default:
                    errorMessage = error.localizedDescription
                }
            } catch {
                isLoading = false
                errorMessage = error.localizedDescription
            }
        }
    }

    /// Re-authenticate with biometric
    func reAuthenticateWithBiometric() {
        Task {
            isLoading = true
            errorMessage = nil

            do {
                // Authenticate with biometric and get stored credentials
                let credentials = try await biometricService.authenticateWithBiometric()

                // Login with the retrieved credentials
                let (success, needsNewPassword, _) = try await authService.login(
                    email: credentials.email,
                    password: credentials.password,
                    subdomain: authService.getCurrentSubdomain()
                )

                if success && !needsNewPassword {
                    // Clear the session expired flag
                    authService.clearSessionExpiredFlag()
                    isLoading = false
                    onReAuthSuccess?()
                } else if needsNewPassword {
                    isLoading = false
                    errorMessage = "Password change required. Please contact support."
                } else {
                    isLoading = false
                    errorMessage = "Re-authentication failed"
                }
            } catch BiometricAuthService.BiometricError.userCancelled {
                // User cancelled - don't show error
                isLoading = false
            } catch let error as BiometricAuthService.BiometricError {
                isLoading = false
                errorMessage = error.localizedDescription
                // Disable biometric if there's an error (e.g., credentials not found)
                biometricEnabled = false
            } catch let error as AuthError {
                isLoading = false
                switch error {
                case .unauthorized(let message):
                    errorMessage = message.isEmpty ? "Invalid credentials" : message
                default:
                    errorMessage = error.localizedDescription
                }
            } catch {
                isLoading = false
                errorMessage = error.localizedDescription
            }
        }
    }

    /// Called when user clicks "Switch User"
    /// Shows warning dialog if there's pending offline data
    func onSwitchUserClicked() {
        // Refresh the sync count
        pendingSyncCount = networkState.syncQueueCount

        if pendingSyncCount > 0 {
            showSwitchUserWarning = true
        } else {
            // No pending data, proceed directly
            performSwitchUser()
        }
    }

    /// Confirm switch user after warning
    func confirmSwitchUser() {
        showSwitchUserWarning = false
        performSwitchUser()
    }

    /// Cancel switch user warning
    func cancelSwitchUser() {
        showSwitchUserWarning = false
    }

    /// Perform a non-destructive account switch (ZP-1847 / parity with Android
    /// `SwitchAccountUseCase`). Site-scoped entity tables are cleared, but the
    /// prior user's `SyncQueueItem`, `SyncLog`, and `SLDChoice` rows are
    /// preserved on device — they reappear when the same user logs back in.
    /// The new user only sees their own queue (per-user filter at flush + UI).
    private func performSwitchUser() {
        Task {
            isLoading = true
            errorMessage = nil

            if let context = modelContext {
                do {
                    try await sldService.clearSLDs(modelContext: context, preserveSyncMetadata: true)
                } catch {
                    AppLogger.log(.error, "Error clearing local data during switch user: \(error)", category: .auth)
                }
            }

            // Existing soft logout — clears auth tokens but does not touch
            // the queue / log / SLDChoice tables.
            await authService.performSwitchUser()

            isLoading = false
            onSwitchUserSuccess?()
        }
    }

    /// Clear error message
    func clearError() {
        errorMessage = nil
    }
}
