import SwiftUI
import SwiftData

/// Bottom sheet for re-authentication when session expires.
///
/// Features:
/// - Pre-filled read-only email field
/// - Password entry field
/// - Biometric authentication option (if enabled)
/// - Switch user option with data loss warning
/// - Non-dismissible (user must take action)
struct ReAuthenticationView: View {
    @Environment(\.modelContext) private var modelContext
    @StateObject private var viewModel = ReAuthenticationViewModel()
    @State private var isPasswordSecure = true
    @FocusState private var passwordFieldFocused: Bool

    var onReAuthSuccess: () -> Void
    var onSwitchUser: () -> Void

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Lock Icon
                    lockIcon

                    // Title and Message
                    titleSection

                    // Email Field (Read-only)
                    emailField

                    // Password Field
                    passwordField

                    // Sign In Button
                    signInButton

                    // Legal consent text
                    legalConsentSection

                    // Biometric Button (if available)
                    if viewModel.biometricAvailable && viewModel.biometricEnabled {
                        biometricButton
                    }

                    // Error Message
                    if let error = viewModel.errorMessage {
                        errorCard(error)
                    }

                    // Switch User Link
                    switchUserLink
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
            }
            .navigationBarTitleDisplayMode(.inline)
        }
        .onAppear {
            viewModel.setModelContext(modelContext)
            viewModel.onReAuthSuccess = onReAuthSuccess
            viewModel.onSwitchUserSuccess = onSwitchUser
        }
        .alert(AppStrings.AuthExtra.offlineDataWarning, isPresented: $viewModel.showSwitchUserWarning) {
            Button(AppStrings.AuthExtra.switchUser, role: .destructive) {
                viewModel.confirmSwitchUser()
            }
            Button(AppStrings.Common.cancel, role: .cancel) {
                viewModel.cancelSwitchUser()
            }
        } message: {
            Text(AppStrings.AuthExtra.pendingOperationsWarning(viewModel.pendingSyncCount))
        }
    }

    // MARK: - View Components

    private var lockIcon: some View {
        ZStack {
            Circle()
                .fill(Color.red.opacity(0.15))
                .frame(width: 64, height: 64)

            Image(systemName: "lock.fill")
                .font(.system(size: 28))
                .foregroundColor(.red)
        }
        .padding(.top, 8)
    }

    private var titleSection: some View {
        VStack(spacing: 8) {
            Text(AppStrings.AuthExtra.sessionExpired)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.primary)

            Text(AppStrings.AuthExtra.sessionExpiredMessage)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    private var emailField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(AppStrings.Common.email)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.secondary)

            HStack {
                Image(systemName: "envelope")
                    .foregroundColor(.secondary)
                    .frame(width: 20)

                Text(viewModel.email)
                    .foregroundColor(.primary)

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(UIColor.systemGray5))
            )
        }
    }

    private var passwordField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(AppStrings.Common.password)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.secondary)

            HStack {
                Image(systemName: "lock")
                    .foregroundColor(.secondary)
                    .frame(width: 20)

                if isPasswordSecure {
                    SecureField(AppStrings.Auth.enterYourPassword, text: Binding(
                        get: { viewModel.password },
                        set: { viewModel.onPasswordChange($0) }
                    ))
                    .focused($passwordFieldFocused)
                    .textContentType(.password)
                    .submitLabel(.done)
                    .onSubmit {
                        if !viewModel.isLoading {
                            viewModel.reAuthenticateWithPassword()
                        }
                    }
                } else {
                    TextField(AppStrings.Auth.enterYourPassword, text: Binding(
                        get: { viewModel.password },
                        set: { viewModel.onPasswordChange($0) }
                    ))
                    .focused($passwordFieldFocused)
                    .textContentType(.password)
                    .submitLabel(.done)
                    .onSubmit {
                        if !viewModel.isLoading {
                            viewModel.reAuthenticateWithPassword()
                        }
                    }
                }

                Button(action: {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        isPasswordSecure.toggle()
                    }
                }) {
                    Image(systemName: isPasswordSecure ? "eye.slash" : "eye")
                        .font(.system(.body, weight: .medium))
                        .foregroundColor(.secondary)
                        .frame(width: 20, height: 20)
                }
                .buttonStyle(PlainButtonStyle())
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(UIColor.systemGray6))
                    .stroke(
                        viewModel.passwordError != nil ? Color.red :
                            (passwordFieldFocused ? Color.blue : Color.clear),
                        lineWidth: 2
                    )
            )

            // Password Error
            if let passwordError = viewModel.passwordError {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.circle.fill")
                        .font(.caption)
                    Text(passwordError)
                        .font(.caption)
                }
                .foregroundColor(.red)
            }
        }
    }

    private var signInButton: some View {
        Button(action: {
            passwordFieldFocused = false
            viewModel.reAuthenticateWithPassword()
        }) {
            HStack {
                if viewModel.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(0.9)
                } else {
                    Text(AppStrings.Auth.signIn)
                        .fontWeight(.semibold)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(viewModel.password.isEmpty || viewModel.isLoading ? Color.blue.opacity(0.5) : Color.blue)
            )
            .foregroundColor(.white)
        }
        .disabled(viewModel.password.isEmpty || viewModel.isLoading)
    }

    @ViewBuilder
    private var legalConsentSection: some View {
        let docs = CompanyConfigService.shared.loadStoredConfig()?.legalDocuments
        let termsURL = docs?.termsAndConditions.flatMap { URL(string: $0.contentUrl) }
        let privacyURL = docs?.privacyPolicy.flatMap { URL(string: $0.contentUrl) }

        if termsURL != nil || privacyURL != nil {
            LegalConsentText(termsURL: termsURL, privacyURL: privacyURL)
        }
    }

    private var biometricButton: some View {
        Button(action: {
            passwordFieldFocused = false
            viewModel.reAuthenticateWithBiometric()
        }) {
            HStack(spacing: 8) {
                if viewModel.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .blue))
                        .scaleEffect(0.9)
                } else {
                    Image(systemName: viewModel.biometricType.iconName)
                        .font(.system(size: 20))
                    Text(AppStrings.Auth.signInWithBiometric(type: viewModel.biometricType.displayName))
                        .fontWeight(.medium)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(viewModel.isLoading ? Color.blue.opacity(0.5) : Color.blue, lineWidth: 2)
            )
            .foregroundColor(.blue)
        }
        .disabled(viewModel.isLoading)
    }

    private func errorCard(_ error: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.red)
            Text(error)
                .font(.subheadline)
                .foregroundColor(.red)
            Spacer()
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.red.opacity(0.1))
        )
    }

    private var switchUserLink: some View {
        Button(action: {
            passwordFieldFocused = false
            viewModel.onSwitchUserClicked()
        }) {
            Text(AppStrings.AuthExtra.switchUser)
                .font(.body)
                .fontWeight(.medium)
                .foregroundColor(.blue)
                .underline()
        }
        .disabled(viewModel.isLoading)
        .padding(.top, 8)
        .padding(.bottom, 16)
    }
}

#Preview {
    ReAuthenticationView(
        onReAuthSuccess: {},
        onSwitchUser: {}
    )
}
