//
//  LoginViewWithConfig.swift
//  Egalvanic PZ
//
//  Login view with company branding loaded from configuration
//

import SwiftUI

struct LoginViewWithConfig: View {
    @EnvironmentObject var authService: AuthService
    @EnvironmentObject var appState: AppStateManager
    @Environment(\.verticalSizeClass) var verticalSizeClass
    @Environment(\.horizontalSizeClass) var horizontalSizeClass

    let companyConfig: CompanyConfig
    let onLoginSuccess: (String, String) -> Void
    let onBackToCompanyCode: () -> Void

    @StateObject private var viewModel: LoginViewModel
    @State private var formOpacity: Double = 0
    @State private var formOffset: CGFloat = 30
    @State private var logoOpacity: Double = 0
    @State private var logoScale: CGFloat = 0.8
    @State private var welcomeOpacity: Double = 0
    @State private var welcomeOffset: CGFloat = -20
    @State private var loginButtonScale: CGFloat = 1.0
    @State private var biometricButtonScale: CGFloat = 1.0
    @State private var isPasswordSecure: Bool = true
    @State private var logoImage: UIImage?
    @State private var showForgotPassword = false
    @State private var showResetPassword = false
    @State private var forgotPasswordEmail = ""
    @State private var emailFocused: Bool = false
    @State private var passwordFocused: Bool = false

    init(companyConfig: CompanyConfig, onLoginSuccess: @escaping (String, String) -> Void, onBackToCompanyCode: @escaping () -> Void) {
        self.companyConfig = companyConfig
        self.onLoginSuccess = onLoginSuccess
        self.onBackToCompanyCode = onBackToCompanyCode
        self._viewModel = StateObject(wrappedValue: LoginViewModel(onLoginSuccess: onLoginSuccess))
    }

    var body: some View {
        GeometryReader { geometry in
            // Only use landscape layout on iPhone in landscape (verticalSizeClass == .compact)
            // iPad should always use portrait-style centered layout
            let isCompactHeight = verticalSizeClass == .compact

            ZStack(alignment: .topLeading) {
                // Main login content
                if isCompactHeight {
                    landscapeLayout(geometry: geometry)
                } else {
                    portraitLayout(geometry: geometry)
                }

                // Back button overlay
                Button(action: onBackToCompanyCode) {
                    HStack(spacing: 6) {
                        Image(systemName: "chevron.left")
                            .font(.system(.body, weight: .semibold))
                        Text(AppStrings.Auth.changeCompany)
                            .font(.system(.subheadline, design: .rounded, weight: .medium))
                    }
                    .foregroundColor(.blue)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(Color.blue.opacity(0.1))
                    )
                }
                .padding(.top, 16)
                .padding(.leading, 16)
            }
        }
        .background(Color(UIColor.systemBackground))
        .sheet(isPresented: $viewModel.showingNewPasswordSheet) {
            NewPasswordView(
                session: viewModel.challengeSession,
                email: viewModel.email,
                newPassword: $viewModel.newPassword,
                givenName: $viewModel.givenName,
                familyName: $viewModel.familyName,
                timezone: $viewModel.timezone,
                onComplete: viewModel.handleNewPasswordSheetDismissal
            )
            .environmentObject(authService)
        }
        .navigationDestination(isPresented: $showForgotPassword) {
            ForgotPasswordView(
                email: forgotPasswordEmail,
                subdomain: companyConfig.company.subdomain,
                onBackToLogin: { showForgotPassword = false },
                onNavigateToReset: { email in
                    forgotPasswordEmail = email
                    showForgotPassword = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showResetPassword = true
                    }
                }
            )
            .navigationBarBackButtonHidden(true)
        }
        .navigationDestination(isPresented: $showResetPassword) {
            ResetPasswordView(
                email: forgotPasswordEmail,
                subdomain: companyConfig.company.subdomain,
                onBackToLogin: { showResetPassword = false },
                onRequestNewCode: {
                    showResetPassword = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showForgotPassword = true
                    }
                }
            )
            .navigationBarBackButtonHidden(true)
        }
        .onTapGesture {
            viewModel.dismissKeyboard()
        }
        .onAppear {
            viewModel.companyCode = companyConfig.company.subdomain
            viewModel.legalDocuments = companyConfig.legalDocuments
            viewModel.checkBiometricAvailability()
            loadCompanyLogo()

            // Animate logo appearance
            withAnimation(.easeOut(duration: 0.6)) {
                logoOpacity = 1
                logoScale = 1.0
            }

            // Animate welcome text
            withAnimation(.easeOut(duration: 0.6).delay(0.1)) {
                welcomeOpacity = 1
                welcomeOffset = 0
            }

            // Animate form appearance
            withAnimation(.easeOut(duration: 0.8).delay(0.3)) {
                formOpacity = 1
                formOffset = 0
            }
        }
    }

    // MARK: - Layout Views

    @ViewBuilder
    private func landscapeLayout(geometry: GeometryProxy) -> some View {
        HStack(spacing: 0) {
            // Left side - Logo and welcome message
            VStack(spacing: 16) {
                Spacer()

                // Company Logo
                companyLogoView(size: 80)
                    .opacity(logoOpacity)
                    .scaleEffect(logoScale)

                // Welcome message
                VStack(spacing: 4) {
                    Text(companyConfig.company.name)
                        .font(.system(.title3, design: .rounded, weight: .bold))
                        .multilineTextAlignment(.center)

                    Text(AppStrings.Auth.signInToContinue)
                        .font(.system(.caption, design: .rounded))
                        .foregroundColor(.secondary)
                }
                .opacity(welcomeOpacity)
                .offset(y: welcomeOffset)

                Spacer()
            }
            .frame(maxWidth: geometry.size.width * 0.4)
            .padding(.leading, 20)

            // Right side - Login form
            ScrollView {
                loginForm(isCompact: true)
                    .padding(.vertical, 20)
                    .opacity(formOpacity)
                    .offset(y: formOffset)
            }
            .frame(maxWidth: geometry.size.width * 0.6)
            .padding(.horizontal, 20)
        }
    }

    @ViewBuilder
    private func portraitLayout(geometry: GeometryProxy) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                // Top section with logo and welcome message
                VStack(spacing: 20) {
                    // Company Logo
                    companyLogoView(size: 120)
                        .padding(.bottom, 10)
                        .opacity(logoOpacity)
                        .scaleEffect(logoScale)

                    // Welcome message
                    VStack(spacing: 8) {
                        Text(companyConfig.company.name)
                            .font(.system(.largeTitle, design: .rounded, weight: .bold))
                            .multilineTextAlignment(.center)

                        Text(AppStrings.Auth.signInToContinue)
                            .font(.system(.subheadline, design: .rounded))
                            .foregroundColor(.secondary)
                    }
                    .opacity(welcomeOpacity)
                    .offset(y: welcomeOffset)
                }
                .padding(.top, 60) // Extra padding for back button
                .padding(.bottom, 30)

                // Login form section
                loginForm(isCompact: false)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 30)
                    .opacity(formOpacity)
                    .offset(y: formOffset)
            }
        }
    }

    // MARK: - Reusable Components

    @ViewBuilder
    private func companyLogoView(size: CGFloat) -> some View {
        if let logoImage = logoImage {
            Image(uiImage: logoImage)
                .resizable()
                .scaledToFit()
                .frame(width: size, height: size)
        } else {
            // Fallback to default logo while loading
            Image("Logo")
                .resizable()
                .scaledToFit()
                .frame(width: size, height: size)
        }
    }

    @ViewBuilder
    private func loginForm(isCompact: Bool) -> some View {
        VStack(spacing: isCompact ? 20 : 24) {
            VStack(spacing: isCompact ? 16 : 20) {
                // Email field
                VStack(alignment: .leading, spacing: isCompact ? 8 : 10) {
                    Text(AppStrings.Common.email)
                        .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                        .foregroundColor(.secondary)

                    ModernPlainFieldStyle(
                        text: $viewModel.email,
                        placeholder: AppStrings.Auth.enterYourEmail,
                        keyboardType: .emailAddress,
                        textContentType: .emailAddress,
                        autocapitalizationType: .none,
                        isFocused: $emailFocused
                    )
                }

                // Password field
                VStack(alignment: .leading, spacing: isCompact ? 8 : 10) {
                    Text(AppStrings.Common.password)
                        .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                        .foregroundColor(.secondary)

                    ModernSecureFieldStyle(
                        text: $viewModel.password,
                        placeholder: AppStrings.Auth.enterYourPassword,
                        isSecure: $isPasswordSecure,
                        isFocused: $passwordFocused
                    )
                }

                // Error message
                if let errorMessage = viewModel.errorMessage {
                    HStack(spacing: 8) {
                        Image(systemName: "exclamationmark.circle.fill")
                            .font(.system(.caption, weight: .medium))
                        Text(errorMessage)
                            .font(.system(.caption, design: .rounded))
                    }
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 4)
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }

            }

            VStack(spacing: isCompact ? 12 : 16) {
                // Login button
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.1)) {
                        loginButtonScale = 0.95
                    }

                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                        withAnimation(.easeInOut(duration: 0.1)) {
                            loginButtonScale = 1.0
                        }
                    }

                    viewModel.login()
                }) {
                    HStack(spacing: 8) {
                        if authService.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.9)
                        } else {
                            Text(AppStrings.Auth.signIn)
                                .font(.system(isCompact ? .callout : .body, design: .rounded, weight: .semibold))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: isCompact ? 48 : 56)
                    .background(
                        RoundedRectangle(cornerRadius: isCompact ? 14 : 16)
                            .fill(
                                LinearGradient(
                                    gradient: Gradient(colors: viewModel.isLoginDisabled ?
                                        [Color.gray.opacity(0.6), Color.gray.opacity(0.4)] :
                                        [Color.blue, Color.blue.opacity(0.8)]
                                    ),
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                    )
                    .foregroundColor(.white)
                    .shadow(color: viewModel.isLoginDisabled ? .clear : .blue.opacity(0.3), radius: isCompact ? 6 : 8, x: 0, y: isCompact ? 3 : 4)
                    .scaleEffect(loginButtonScale)
                }
                .disabled(viewModel.isLoginDisabled)

                // Inline legal consent — tapping Sign In implies acceptance
                if viewModel.requiresLegalConsent {
                    LegalConsentText(
                        termsURL: viewModel.legalDocuments?.termsAndConditions.flatMap { URL(string: $0.contentUrl) },
                        privacyURL: viewModel.legalDocuments?.privacyPolicy.flatMap { URL(string: $0.contentUrl) }
                    )
                }

                // Biometric login button
                if viewModel.canShowBiometricLogin {
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.1)) {
                            biometricButtonScale = 0.95
                        }

                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                            withAnimation(.easeInOut(duration: 0.1)) {
                                biometricButtonScale = 1.0
                            }
                        }

                        viewModel.loginWithBiometric()
                    }) {
                        HStack(spacing: isCompact ? 10 : 12) {
                            Image(systemName: viewModel.biometricIconName)
                                .font(isCompact ? .callout : .title3)
                                .foregroundColor(.blue)
                            Text(AppStrings.Auth.signInWithBiometric(type: viewModel.biometricDisplayName))
                                .font(.system(isCompact ? .caption : .body, design: .rounded, weight: .medium))
                                .foregroundColor(.blue)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: isCompact ? 48 : 56)
                        .background(
                            RoundedRectangle(cornerRadius: isCompact ? 14 : 16)
                                .fill(Color.blue.opacity(0.05))
                                .stroke(Color.blue.opacity(0.3), lineWidth: 1)
                        )
                        .scaleEffect(biometricButtonScale)
                    }
                    .disabled(viewModel.isBiometricLoginDisabled)
                    .transition(.opacity.combined(with: .scale))
                }

                // Forgot password link
                Button(action: {
                    forgotPasswordEmail = viewModel.email
                    showForgotPassword = true
                }) {
                    Text(AppStrings.ForgotPassword.forgotPassword)
                        .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                        .foregroundColor(.blue)
                }
                .padding(.top, 4)
            }
        }
    }

    // MARK: - Private Methods

    private func loadCompanyLogo() {
        // Get logo URL (company logo, with fallback to alliance partner)
        let logoURLString = companyConfig.company.branding.largeLogo ?? companyConfig.alliancePartner?.branding.largeLogo

        guard let logoURLString = logoURLString,
              let url = URL(string: logoURLString) else {
            return
        }

        Task {
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                if let image = UIImage(data: data) {
                    await MainActor.run {
                        self.logoImage = image
                    }
                }
            } catch {
                AppLogger.log(.error, "Failed to load company logo: \(error.localizedDescription)", category: .auth)
            }
        }
    }
}
