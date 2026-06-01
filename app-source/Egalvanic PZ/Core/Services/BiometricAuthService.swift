import Foundation
import LocalAuthentication
import SwiftUI

@MainActor
class BiometricAuthService: ObservableObject {
    static let shared = BiometricAuthService()

    @Published var biometricType: BiometricType = .none
    @Published var isAuthenticating = false

    private let keychainService = KeychainService.shared

    // Enable logging only in DEBUG builds
    #if DEBUG
    private let enableLogging = true
    #else
    private let enableLogging = false
    #endif

    enum BiometricType {
        case none
        case faceID
        case touchID

        var displayName: String {
            switch self {
            case .none:
                return "Not Available"
            case .faceID:
                return "Face ID"
            case .touchID:
                return "Touch ID"
            }
        }

        var iconName: String {
            switch self {
            case .none:
                return "lock.slash"
            case .faceID:
                return "faceid"
            case .touchID:
                return "touchid"
            }
        }
    }

    enum BiometricError: LocalizedError {
        case notAvailable
        case notEnrolled
        case authenticationFailed
        case userCancelled
        case passcodeNotSet
        case biometricLockout
        case unknown

        var errorDescription: String? {
            switch self {
            case .notAvailable:
                return "Biometric authentication is not available on this device"
            case .notEnrolled:
                return "No biometric data is enrolled. Please set up Face ID or Touch ID in Settings"
            case .authenticationFailed:
                return "Biometric authentication failed"
            case .userCancelled:
                return "Authentication was cancelled"
            case .passcodeNotSet:
                return "Device passcode is not set"
            case .biometricLockout:
                return "Biometric authentication is locked. Please try again later or use passcode"
            case .unknown:
                return "An unknown error occurred"
            }
        }
    }

    private init() {
        checkBiometricAvailability()
    }

    // MARK: - Logging Helper
    private func log(_ message: String) {
        if enableLogging {
            AppLogger.log(.debug, "[BiometricAuthService] \(message)", category: .auth)
        }
    }

    // MARK: - Public Methods

    func checkBiometricAvailability() {
        let context = LAContext()
        var error: NSError?
        let isAvailable = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)

        if isAvailable {
            switch context.biometryType {
            case .faceID:
                biometricType = .faceID
            case .touchID:
                biometricType = .touchID
            case .none:
                biometricType = .none
            @unknown default:
                biometricType = .none
            }
        } else {
            biometricType = .none
        }
    }

    func canUseBiometric() -> Bool {
        let context = LAContext()
        var error: NSError?

        // First check if biometrics are available (not locked)
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            return biometricType != .none
        }

        // If biometrics are locked out, check if device authentication (passcode fallback) is available
        // This ensures the button stays visible when Face ID is temporarily locked
        if let error = error as? LAError, error.code == .biometryLockout {
            return context.canEvaluatePolicy(.deviceOwnerAuthentication, error: nil) && biometricType != .none
        }

        return false
    }

    func authenticateWithBiometric() async throws -> (email: String, password: String) {
        guard canUseBiometric() else {
            throw BiometricError.notAvailable
        }

        guard let credentials = keychainService.retrieveCredentials() else {
            throw BiometricError.unknown
        }

        isAuthenticating = true
        defer { isAuthenticating = false }

        let reason = "Authenticate to access your account"
        let context = LAContext()  // Create fresh context for authentication

        log("🔐 Triggering FaceID authentication prompt")

        do {
            let success = try await context.evaluatePolicy(
                .deviceOwnerAuthentication,
                localizedReason: reason
            )

            if success {
                log("✅ Authentication successful, returning credentials")
                return credentials
            } else {
                log("❌ FaceID authentication failed")
                throw BiometricError.authenticationFailed
            }
        } catch let error as LAError {
            log("❌ FaceID error: \(error.localizedDescription)")
            throw mapLAError(error)
        } catch {
            log("❌ Unknown FaceID error: \(error)")
            throw BiometricError.unknown
        }
    }

    func setupBiometricAuth(email: String, password: String) async throws {
        guard canUseBiometric() else {
            log("❌ Biometric not available")
            throw BiometricError.notAvailable
        }

        // First authenticate with biometric to ensure user consent
        let reason = "Authenticate to enable \(biometricType.displayName) for login"
        log("🔐 Requesting biometric authentication for setup")

        let context = LAContext()  // Create fresh context for authentication

        do {
            let success = try await context.evaluatePolicy(
                .deviceOwnerAuthentication,
                localizedReason: reason
            )

            if success {
                log("✅ Authentication successful")
                // Save credentials to keychain
                let saved = keychainService.saveCredentials(email: email, password: password)
                if saved {
                    log("✅ Credentials saved to keychain")
                    keychainService.isBiometricAuthEnabled = true
                    log("✅ Biometric auth enabled flag set")
                    log("  - isBiometricAuthEnabled: \(isBiometricAuthEnabled)")
                    log("  - hasStoredCredentials: \(hasStoredCredentials)")
                } else {
                    log("❌ Failed to save credentials to keychain")
                    throw BiometricError.unknown
                }
            } else {
                log("❌ Biometric authentication failed")
                throw BiometricError.authenticationFailed
            }
        } catch let error as LAError {
            throw mapLAError(error)
        } catch {
            throw BiometricError.unknown
        }
    }

    func disableBiometricAuth() {
        keychainService.isBiometricAuthEnabled = false
        _ = keychainService.deleteCredentials()
    }

    var isBiometricAuthEnabled: Bool {
        return keychainService.isBiometricAuthEnabled && keychainService.hasStoredCredentials()
    }

    var hasStoredCredentials: Bool {
        return keychainService.hasStoredCredentials()
    }

    // MARK: - Private Methods

    private func mapLAError(_ error: LAError) -> BiometricError {
        switch error.code {
        case .authenticationFailed:
            return .authenticationFailed
        case .userCancel, .systemCancel, .appCancel:
            return .userCancelled
        case .passcodeNotSet:
            return .passcodeNotSet
        case .biometryNotAvailable:
            return .notAvailable
        case .biometryNotEnrolled:
            return .notEnrolled
        case .biometryLockout:
            return .biometricLockout
        default:
            return .unknown
        }
    }
}