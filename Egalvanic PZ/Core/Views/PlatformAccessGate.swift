//
//  PlatformAccessGate.swift
//  Egalvanic PZ
//
//  Platform access control - ensures users can only access platforms they have permission for
//

import SwiftUI

struct PlatformAccessGate<Content: View>: View {
    @EnvironmentObject var authService: AuthService

    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        Group {
            if authService.isCheckingPlatformAccess {
                // Loading state while checking permissions
                loadingView
            } else if !authService.hasMobileAccess {
                // Block access if user doesn't have platform.mobile permission
                blockedAccessView
            } else {
                // User has mobile access - render the app
                content
            }
        }
    }

    // MARK: - View Components

    private var loadingView: some View {
        ZStack {
            // White background
            Color(.systemBackground)
                .ignoresSafeArea()

            VStack(spacing: 20) {
                ProgressView()
                    .scaleEffect(1.5)

                Text(AppStrings.PlatformAccess.checkingAccess)
                    .font(.title2)
                    .fontWeight(.semibold)

                Text(AppStrings.PlatformAccess.verifyingPermissions)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding(40)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(Color(.systemBackground))
                    .shadow(radius: 10)
            )
            .padding(40)
        }
    }

    private var blockedAccessView: some View {
        ZStack {
            // White background
            Color(.systemBackground)
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    // Icon
                    Image(systemName: "exclamationmark.shield.fill")
                        .font(.system(size: 80))
                        .foregroundColor(.red)
                        .padding(.top, 40)

                    // Title
                    Text(AppStrings.PlatformAccess.mobileAccessRestricted)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)

                    // Description
                    VStack(spacing: 12) {
                        Text(AppStrings.PlatformAccess.noPermissionMessage)
                            .font(.body)
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.center)

                        Text(AppStrings.PlatformAccess.contactAdminMessage)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.horizontal, 20)

                    // Current role display
                    if let activeRole = authService.currentUserActiveRole {
                        Text(AppStrings.PlatformAccess.currentRole(activeRole))
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 20)
                            .padding(.top, 8)
                    }

                    // Logout button
                    Button(action: {
                        Task {
                            await authService.logout()
                        }
                    }) {
                        Text(AppStrings.Common.logout)
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .cornerRadius(12)
                    }
                    .padding(.horizontal, 40)
                    .padding(.top, 20)

                    Spacer()
                }
                .padding(.horizontal, 20)
            }
        }
    }
}
