//
//  ForceUpdateSyncView.swift
//  Egalvanic PZ
//
//  Modal card overlay for syncing data before force update
//

import SwiftUI

struct ForceUpdateSyncView: View {
    @ObservedObject var versionCheckService: VersionCheckService
    @EnvironmentObject var networkState: NetworkState

    // Safely clamp progress values to avoid ProgressView warnings
    private var safeTotal: Double {
        Double(max(networkState.syncTotal, 1))
    }

    private var safeProgress: Double {
        let progress = Double(networkState.syncProgress)
        return min(max(progress, 0), safeTotal)
    }

    var body: some View {
        ZStack {
            // Semi-transparent background
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture { } // Prevent tap-through

            // Modal card
            VStack(spacing: 16) {
                if versionCheckService.syncBeforeUpdateFailed {
                    syncFailedContent
                } else {
                    syncingContent
                }
            }
            .frame(maxWidth: 280)
            .padding(20)
            .background(Color(UIColor.systemBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.2), radius: 12, x: 0, y: 6)
        }
        .onAppear {
            versionCheckService.startSyncBeforeUpdate()
        }
    }

    // MARK: - Syncing Content
    private var syncingContent: some View {
        VStack(spacing: 12) {
            // Animated sync icon
            Image(systemName: "arrow.triangle.2.circlepath")
                .font(.system(size: 32))
                .foregroundColor(.blue)
                .rotationEffect(.degrees(versionCheckService.isSyncingBeforeUpdate ? 360 : 0))
                .animation(
                    .linear(duration: 1.5).repeatForever(autoreverses: false),
                    value: versionCheckService.isSyncingBeforeUpdate
                )

            Text(AppStrings.Sync.syncingYourData)
                .font(.headline)

            // Progress indicator
            if networkState.syncTotal > 0 {
                VStack(spacing: 4) {
                    ProgressView(value: safeProgress, total: safeTotal)
                        .progressViewStyle(LinearProgressViewStyle(tint: .blue))

                    Text(AppStrings.Sync.itemsProgress(networkState.syncProgress, networkState.syncTotal))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            } else {
                ProgressView()
            }

            Text(AppStrings.Sync.pleaseWaitWhileWeSave)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    // MARK: - Sync Failed Content
    private var syncFailedContent: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 32))
                .foregroundColor(.orange)

            Text(AppStrings.Sync.syncIncomplete)
                .font(.headline)

            Text(versionCheckService.syncFailureMessage)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Text(AppStrings.Sync.canSyncLaterFromSettings)
                .font(.caption2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            VStack(spacing: 8) {
                Button(action: {
                    versionCheckService.retrySyncBeforeUpdate()
                }) {
                    Text(AppStrings.Sync.retrySync)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(Color.blue)
                        .cornerRadius(10)
                }

                Button(action: {
                    versionCheckService.proceedToUpdateAnyway()
                }) {
                    Text(AppStrings.Sync.updateAnyway)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.orange)
                }
            }
        }
    }
}
