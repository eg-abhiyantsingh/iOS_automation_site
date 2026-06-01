//
//  NewPasswordView.swift
//  Egalvanic PZ
//
//  Created by Kush Jadia on 24/09/25.
//

import SwiftUI

// MARK: - NewPasswordView
struct NewPasswordView: View {
    @EnvironmentObject var authService: AuthService
    @StateObject private var viewModel: NewPasswordViewModel
    
    let session: String
    let email: String
    @Binding var newPassword: String
    @Binding var givenName: String
    @Binding var familyName: String
    @Binding var timezone: String
    let onComplete: () -> Void
    
    init(session: String, email: String, newPassword: Binding<String>, givenName: Binding<String>, familyName: Binding<String>, timezone: Binding<String>, onComplete: @escaping () -> Void) {
        self.session = session
        self.email = email
        self._newPassword = newPassword
        self._givenName = givenName
        self._familyName = familyName
        self._timezone = timezone
        self.onComplete = onComplete
        
        // Initialize the view model with the current values
        self._viewModel = StateObject(wrappedValue: NewPasswordViewModel(
            session: session,
            email: email,
            givenName: givenName.wrappedValue,
            familyName: familyName.wrappedValue,
            timezone: timezone.wrappedValue,
            onComplete: onComplete
        ))
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(spacing: 8) {
                        Image(systemName: "lock.shield")
                            .font(.system(size: 50))
                            .foregroundColor(.blue)
                            .padding(.top, 20)
                        
                        Text(AppStrings.AuthExtra.setYourPassword)
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        Text(AppStrings.AuthExtra.setNewPasswordDescription)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .padding(.bottom, 10)
                    
                    VStack(spacing: 20) {
                        // Password Section
                        VStack(alignment: .leading, spacing: 16) {
                            Text(AppStrings.AuthExtra.passwordLabel)
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(.secondary)
                            
                            VStack(spacing: 12) {
                                SecureField(AppStrings.AuthExtra.newPassword, text: $viewModel.newPassword)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                                
                                SecureField(AppStrings.AuthExtra.confirmPassword, text: $viewModel.confirmPassword)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                            }
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Label(AppStrings.AuthExtra.atLeast8Characters, systemImage: viewModel.passwordLengthValid ? "checkmark.circle.fill" : "circle")
                                    .font(.caption)
                                    .foregroundColor(viewModel.passwordLengthValid ? .green : .secondary)
                                
                                Label(AppStrings.AuthExtra.containsUpperAndLower, systemImage: viewModel.passwordCaseValid ? "checkmark.circle.fill" : "circle")
                                    .font(.caption)
                                    .foregroundColor(viewModel.passwordCaseValid ? .green : .secondary)
                                
                                Label(AppStrings.AuthExtra.containsNumberAndSpecial, systemImage: viewModel.passwordSpecialValid ? "checkmark.circle.fill" : "circle")
                                    .font(.caption)
                                    .foregroundColor(viewModel.passwordSpecialValid ? .green : .secondary)
                                
                                Label(AppStrings.AuthExtra.passwordsMatch, systemImage: viewModel.passwordsMatch ? "checkmark.circle.fill" : "circle")
                                    .font(.caption)
                                    .foregroundColor(viewModel.passwordsMatch ? .green : .secondary)
                            }
                            .padding(.vertical, 8)
                        }
                        
                        Divider()
                        
                        // User Information Section
                        VStack(alignment: .leading, spacing: 16) {
                            Text(AppStrings.AuthExtra.profileInformation)
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(.secondary)
                            
                            VStack(spacing: 12) {
                                TextField(AppStrings.AuthExtra.firstName, text: $viewModel.givenName)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                                
                                TextField(AppStrings.AuthExtra.lastName, text: $viewModel.familyName)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                                
                                HStack {
                                    Image(systemName: "globe")
                                        .foregroundColor(.secondary)
                                    TextField(AppStrings.AuthExtra.timezone, text: $viewModel.timezone)
                                        .textFieldStyle(RoundedBorderTextFieldStyle())
                                }
                            }
                        }
                        
                        // Error message
                        if let errorMessage = viewModel.errorMessage {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .font(.caption)
                                Text(errorMessage)
                                    .font(.caption)
                            }
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 8)
                        }
                    }
                    .padding(.horizontal)
                    
                    // Submit Button
                    Button(action: viewModel.submitNewPassword) {
                        Text(AppStrings.AuthExtra.completeSetup)
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(viewModel.submitButtonColor)
                            )
                            .foregroundColor(.white)
                    }
                    .disabled(!viewModel.canSubmit)
                    .padding(.horizontal)
                    .padding(.bottom, 20)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        viewModel.cancelSetup()
                    }
                }
            }
        }
        .onChange(of: viewModel.newPassword) { _, newValue in
            newPassword = newValue
        }
        .onChange(of: viewModel.givenName) { _, newValue in
            givenName = newValue
        }
        .onChange(of: viewModel.familyName) { _, newValue in
            familyName = newValue
        }
        .onChange(of: viewModel.timezone) { _, newValue in
            timezone = newValue
        }
    }
}
