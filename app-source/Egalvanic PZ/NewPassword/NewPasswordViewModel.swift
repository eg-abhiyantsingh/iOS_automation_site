//
//  NewPasswordViewModel.swift
//  Egalvanic PZ
//
//  Created by Kush Jadia on 24/09/25.
//

import SwiftUI

@MainActor
class NewPasswordViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var newPassword = ""
    @Published var confirmPassword = ""
    @Published var givenName = ""
    @Published var familyName = ""
    @Published var timezone = ""
    @Published var errorMessage: String?
    
    // MARK: - Private Properties
    private let authService = AuthService.shared
    private let session: String
    private let email: String
    private let onComplete: () -> Void
    
    // MARK: - Initialization
    init(session: String, email: String, givenName: String, familyName: String, timezone: String, onComplete: @escaping () -> Void) {
        self.session = session
        self.email = email
        self.givenName = givenName
        self.familyName = familyName
        self.timezone = timezone
        self.onComplete = onComplete
    }
    
    // MARK: - Computed Properties
    var passwordLengthValid: Bool {
        newPassword.count >= 8
    }
    
    var passwordCaseValid: Bool {
        let hasUppercase = newPassword.range(of: "[A-Z]", options: .regularExpression) != nil
        let hasLowercase = newPassword.range(of: "[a-z]", options: .regularExpression) != nil
        return hasUppercase && hasLowercase
    }
    
    var passwordSpecialValid: Bool {
        let hasNumber = newPassword.range(of: "[0-9]", options: .regularExpression) != nil
        let hasSpecial = newPassword.range(of: "[^A-Za-z0-9]", options: .regularExpression) != nil
        return hasNumber && hasSpecial
    }
    
    var passwordsMatch: Bool {
        !newPassword.isEmpty && newPassword == confirmPassword
    }
    
    var canSubmit: Bool {
        passwordLengthValid &&
        passwordCaseValid &&
        passwordSpecialValid &&
        passwordsMatch &&
        !givenName.isEmpty &&
        !familyName.isEmpty
    }
    
    var submitButtonColor: Color {
        canSubmit ? Color.blue : Color.blue.opacity(0.6)
    }
    
    // MARK: - Public Methods
    func submitNewPassword() {
        guard passwordsMatch else {
            errorMessage = "Passwords do not match"
            return
        }
        
        Task {
            do {
                try await authService.respondToNewPasswordChallenge(
                    session: session,
                    email: email,
                    newPassword: newPassword,
                    givenName: givenName,
                    familyName: familyName,
                    timezone: timezone
                )
                onComplete()
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
    
    func cancelSetup() {
        onComplete()
    }
}