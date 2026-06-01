//
//  RefreshProgressOverlay.swift
//  Egalvanic PZ
//
//  Reusable overlay component for showing sync progress during refresh operations.
//

import SwiftUI

/// A compact overlay that shows sync progress during refresh operations
struct RefreshProgressOverlay: View {
    @ObservedObject var syncService: SLDSyncService

    private var progressDetails: SyncProgressDetails {
        syncService.progressDetails
    }

    private var isActive: Bool {
        syncService.isSyncing && !syncService.isSiteSwitchMode
    }

    var body: some View {
        if isActive {
            VStack(spacing: 0) {
                // Progress content
                VStack(spacing: 12) {
                    // Header with spinner
                    HStack(spacing: 12) {
                        ProgressView()
                            .scaleEffect(0.8)

                        Text(AppStrings.Supporting.syncing)
                            .font(.headline)
                            .foregroundColor(.primary)

                        Spacer()

                        Text("\(Int(syncService.syncProgress * 100))%")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.accentColor)
                    }

                    // Progress bar
                    ProgressView(value: syncService.syncProgress, total: 1.0)
                        .progressViewStyle(LinearProgressViewStyle())

                    // Current step
                    if !syncService.syncStatusMessage.isEmpty {
                        Text(syncService.syncStatusMessage)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }

                    // Detailed counts (compact version)
                    if progressDetails.totalEntities > 0 {
                        CompactProgressGrid(details: progressDetails)
                    }
                }
                .padding(16)
                .background(Color(UIColor.systemBackground))
                .cornerRadius(16)
                .shadow(color: Color.black.opacity(0.15), radius: 10, x: 0, y: 5)
                .padding(.horizontal, 20)

                Spacer()
            }
            .padding(.top, 20)
            .transition(.move(edge: .top).combined(with: .opacity))
            .animation(.spring(response: 0.3), value: isActive)
        }
    }
}

// MARK: - Compact Progress Grid

/// A more compact version of the progress grid for overlay use
struct CompactProgressGrid: View {
    let details: SyncProgressDetails

    var body: some View {
        VStack(spacing: 8) {
            Text(AppStrings.Sync.itemsProgress(details.processedEntities, details.totalEntities))
                .font(.caption2)
                .foregroundColor(.secondary)

            // Show only the most relevant items in a single row
            HStack(spacing: 16) {
                if details.totalNodes > 0 {
                    CompactProgressItem(
                        icon: "cube.fill",
                        processed: details.processedNodes,
                        total: details.totalNodes,
                        isActive: details.currentStep == .nodes
                    )
                }
                if details.totalEdges > 0 {
                    CompactProgressItem(
                        icon: "line.diagonal",
                        processed: details.processedEdges,
                        total: details.totalEdges,
                        isActive: details.currentStep == .edges
                    )
                }
                if details.totalPhotos > 0 {
                    CompactProgressItem(
                        icon: "photo.fill",
                        processed: details.processedPhotos,
                        total: details.totalPhotos,
                        isActive: details.currentStep == .photos
                    )
                }
                if details.totalTasks > 0 {
                    CompactProgressItem(
                        icon: "checkmark.circle.fill",
                        processed: details.processedTasks,
                        total: details.totalTasks,
                        isActive: details.currentStep == .tasks
                    )
                }
                if details.totalIssues > 0 {
                    CompactProgressItem(
                        icon: "exclamationmark.triangle.fill",
                        processed: details.processedIssues,
                        total: details.totalIssues,
                        isActive: details.currentStep == .issues
                    )
                }
            }
        }
    }
}

// MARK: - Compact Progress Item

struct CompactProgressItem: View {
    let icon: String
    let processed: Int
    let total: Int
    let isActive: Bool

    private var isComplete: Bool {
        processed >= total && total > 0
    }

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 10))
                .foregroundColor(isActive ? .accentColor : (isComplete ? .green : .secondary))

            Text("\(processed)/\(total)")
                .font(.system(size: 10))
                .foregroundColor(isComplete ? .green : (isActive ? .accentColor : .secondary))

            if isActive {
                ProgressView()
                    .scaleEffect(0.4)
            } else if isComplete {
                Image(systemName: "checkmark")
                    .font(.system(size: 8))
                    .foregroundColor(.green)
            }
        }
    }
}

// MARK: - View Modifier for Easy Application

extension View {
    /// Adds a refresh progress overlay that shows during sync operations
    func refreshProgressOverlay(syncService: SLDSyncService = SLDSyncService.shared) -> some View {
        self.overlay(
            RefreshProgressOverlay(syncService: syncService)
        )
    }
}
