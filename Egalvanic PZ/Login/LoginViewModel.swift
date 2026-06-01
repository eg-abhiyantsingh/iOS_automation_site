import SwiftUI
import Combine

@MainActor
class LoginViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var email = ""
    @Published var password = ""
    @Published var companyCode = ""
    @Published var showingNewPasswordSheet = false
    @Published var challengeSession = ""
    @Published var errorMessage: String?
    
    // New password challenge fields
    @Published var newPassword = ""
    @Published var givenName = ""
    @Published var familyName = ""
    @Published var timezone = "America/New_York"
    
    var legalDocuments: LegalDocumentsContainer?

    var requiresLegalConsent: Bool {
        guard let docs = legalDocuments else { return false }
        return docs.termsAndConditions != nil || docs.privacyPolicy != nil
    }

    // MARK: - Private Properties
    private let authService = AuthService.shared
    private let biometricService = BiometricAuthService.shared
    private let onLoginSuccess: (String, String) -> Void

    // Enable logging only in DEBUG builds
    #if DEBUG
    private let enableLogging = true
    #else
    private let enableLogging = false
    #endif

    // MARK: - Initialization
    init(onLoginSuccess: @escaping (String, String) -> Void) {
        self.onLoginSuccess = onLoginSuccess
    }

    // MARK: - Logging Helper
    private func log(_ message: String) {
        AppLogger.log(.debug, "[LoginViewModel] \(message)", category: .auth)
    }

    // MARK: - Computed Properties
    var isValidEmail: Bool {
        let emailRegex = "^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return emailPredicate.evaluate(with: email)
    }

    var loginButtonColor: Color {
        if authService.isLoading || email.isEmpty || password.isEmpty || !isValidEmail {
            return Color.blue.opacity(0.6)
        }
        return Color.blue
    }

    var canShowBiometricLogin: Bool {
        biometricService.canUseBiometric() &&
        biometricService.isBiometricAuthEnabled &&
        biometricService.hasStoredCredentials
    }

    var isLoginDisabled: Bool {
        authService.isLoading || email.isEmpty || password.isEmpty || !isValidEmail
    }

    var isBiometricLoginDisabled: Bool {
        biometricService.isAuthenticating || authService.isLoading
    }
    
    var biometricIconName: String {
        biometricService.biometricType.iconName
    }
    
    var biometricDisplayName: String {
        biometricService.biometricType.displayName
    }
    
    // MARK: - Public Methods
    func checkBiometricAvailability() {
        biometricService.checkBiometricAvailability()
        
        // Save company code if provided
        if !companyCode.isEmpty {
            UserDefaults.standard.set(companyCode, forKey: "savedCompanyCode")
        }
        
        // Debug biometric state
        log("🔐 LoginView onAppear:")
        log("  - Biometric enabled: \(biometricService.isBiometricAuthEnabled)")
        log("  - Has stored credentials: \(biometricService.hasStoredCredentials)")
        log("  - Biometric type: \(biometricService.biometricType.displayName)")
        log("  - Can use biometric: \(biometricService.canUseBiometric())")
    }
    
    func dismissKeyboard() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
    
    func login() {
        // Dismiss keyboard
        dismissKeyboard()

        // Validate email format
        guard isValidEmail else {
            errorMessage = AppStrings.ForgotPassword.invalidEmailFormat
            return
        }

        Task {
            do {

                // Pass company code if provided, otherwise pass nil for fallback
                let subdomain = companyCode.isEmpty ? nil : companyCode
                let result = try await authService.login(email: email, password: password, subdomain: subdomain)
                
                if result.needsNewPassword {
                    challengeSession = result.session ?? ""
                    showingNewPasswordSheet = true
                } else if result.success {
                    // Authentication successful - ContentView will handle the rest
                    errorMessage = nil
                    
                    // Save company code if provided
                    if !companyCode.isEmpty {
                        UserDefaults.standard.set(companyCode, forKey: "savedCompanyCode")
                    }
                    
                    // Pass credentials to ContentView for biometric setup
                    onLoginSuccess(email, password)

                    // Fire-and-forget legal acceptance
                    if let docs = legalDocuments {
                        LegalAcceptanceService.shared.acceptDocumentsFromSignIn(documents: docs)
                    }

                    // Debug logging
                    log("🔐 Login successful, passing credentials to ContentView")
                    log("🔐 Company code: \(companyCode.isEmpty ? "none" : companyCode)")
                    log("🔐 Can use biometric: \(biometricService.canUseBiometric())")
                    log("🔐 Biometric enabled: \(biometricService.isBiometricAuthEnabled)")
                    log("🔐 Biometric type: \(biometricService.biometricType.displayName)")
                }
            } catch {
                // Extract a more user-friendly error message
                if error.localizedDescription.contains("Unauthorized") {
                    errorMessage = AppStrings.ForgotPassword.invalidCredentials
                } else if error.localizedDescription.contains("not confirmed") {
                    errorMessage = AppStrings.ForgotPassword.confirmEmail
                } else if error.localizedDescription.contains("Network") {
                    errorMessage = AppStrings.ForgotPassword.networkError
                } else {
                    errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    func loginWithBiometric() {
        log("🔐 FaceID button tapped")
        Task {
            do {
                log("🔐 Calling authenticateWithBiometric...")
                let credentials = try await biometricService.authenticateWithBiometric()

                log("🔐 FaceID passed, logging in with credentials...")
                
                // Use the retrieved credentials to login with stored company code
                let subdomain = companyCode.isEmpty ? nil : companyCode
                let result = try await authService.login(email: credentials.email, password: credentials.password, subdomain: subdomain)
                
                if result.needsNewPassword {
                    // If password needs to be changed, show the sheet and clear biometric auth
                    challengeSession = result.session ?? ""
                    email = credentials.email
                    showingNewPasswordSheet = true
                    biometricService.disableBiometricAuth()
                } else if result.success {
                    errorMessage = nil
                    // Call onLoginSuccess with the retrieved credentials
                    onLoginSuccess(credentials.email, credentials.password)

                    // Fire-and-forget legal acceptance
                    if let docs = legalDocuments {
                        LegalAcceptanceService.shared.acceptDocumentsFromSignIn(documents: docs)
                    }
                }
            } catch let error as BiometricAuthService.BiometricError {
                errorMessage = error.localizedDescription
            } catch {
                errorMessage = error.localizedDescription
                // If login fails, might be due to changed password - clear stored credentials
                biometricService.disableBiometricAuth()
            }
        }
    }
    
    func handleNewPasswordSheetDismissal() {
        showingNewPasswordSheet = false
    }
}
