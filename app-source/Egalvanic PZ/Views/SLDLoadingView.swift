//
//  SLDLoadingView.swift
//  Egalvanic PZ
//
//  Created by Claude on date.
//

import SwiftUI

struct SLDLoadingView: View {
    @ObservedObject var syncService: SLDSyncService
    @ObservedObject var sldService: SLDService
    let onReturnToSiteSelection: (() -> Void)?
    let siteName: String?

    init(
        syncService: SLDSyncService = SLDSyncService.shared,
        sldService: SLDService = SLDService.shared,
        siteName: String? = nil,
        onReturnToSiteSelection: (() -> Void)? = nil
    ) {
        self.syncService = syncService
        self.sldService = sldService
        self.siteName = siteName
        self.onReturnToSiteSelection = onReturnToSiteSelection
    }

    private var progressDetails: SyncProgressDetails {
        syncService.progressDetails
    }

    private var clearProgress: ClearProgressDetails {
        sldService.clearProgress
    }

    private var isClearing: Bool {
        sldService.isClearing
    }

    private var currentProgress: Double {
        if isClearing {
            return clearProgress.progress * 0.3  // Clearing is 0-30%
        } else {
            return 0.3 + (syncService.syncProgress * 0.7)  // Loading is 30-100%
        }
    }

    var body: some View {
        VStack(spacing: 24) {
            // Header
            VStack(spacing: 12) {
                if isClearing {
                    Text(AppStrings.Site.switchingSites)
                        .font(.title2)
                        .fontWeight(.semibold)
                        .multilineTextAlignment(.center)
                } else if let siteName = siteName {
                    Text(AppStrings.Site.loadingSite(siteName))
                        .font(.title2)
                        .fontWeight(.semibold)
                        .multilineTextAlignment(.center)
                } else {
                    Text(AppStrings.Site.loadingSiteGeneric)
                        .font(.title2)
                        .fontWeight(.semibold)
                }

                if syncService.syncErrorMessage == nil {
                    // Spinner
                    ProgressView()
                        .scaleEffect(1.5)
                        .padding(.bottom, 16)

                    // Progress bar
                    if currentProgress > 0 {
                        ProgressView(value: currentProgress, total: 1.0)
                            .progressViewStyle(LinearProgressViewStyle())
                            .frame(maxWidth: 280)
                            .padding(.horizontal)
                    }
                }
            }
            .padding(.top, 40)

            // Status or error message
            if let errorMessage = syncService.syncErrorMessage {
                // Error state
                VStack(spacing: 20) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.red)

                    Text(AppStrings.Site.syncFailed)
                        .font(.title3)
                        .fontWeight(.semibold)

                    Text(errorMessage)
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)

                    if let onReturn = onReturnToSiteSelection {
                        VStack(spacing: 12) {
                            Button(action: onReturn) {
                                HStack {
                                    Image(systemName: "arrow.left")
                                    Text(AppStrings.Site.returnToSiteSelection)
                                }
                                .font(.body)
                                .fontWeight(.medium)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 50)
                                .background(Color.blue)
                                .cornerRadius(12)
                            }
                            .padding(.horizontal, 32)
                            .padding(.top, 8)
                        }
                    }
                }
            } else {
                // Loading state with detailed progress
                VStack(spacing: 16) {
                    // Current step message
                    if isClearing {
                        Text(clearProgress.currentStep.isEmpty ? AppStrings.Site.preparing : clearProgress.currentStep)
                            .font(.headline)
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                    } else {
                        Text(syncService.syncStatusMessage)
                            .font(.headline)
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                    }

                    // Percentage
                    if currentProgress > 0 {
                        Text("\(Int(currentProgress * 100))%")
                            .font(.title3)
                            .fontWeight(.bold)
                            .foregroundColor(.accentColor)
                    }

                    // Detailed entity counts for clearing phase
                    if isClearing && clearProgress.totalEntities > 0 {
                        ClearProgressDetailView(details: clearProgress)
                            .padding(.horizontal, 24)
                            .padding(.top, 8)
                    }

                    // Detailed entity counts for loading phase
                    if !isClearing && progressDetails.totalEntities > 0 {
                        DetailedProgressView(details: progressDetails)
                            .padding(.horizontal, 24)
                            .padding(.top, 8)
                    }
                }
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(UIColor.systemBackground))
    }
}

// MARK: - Detailed Progress View

struct DetailedProgressView: View {
    let details: SyncProgressDetails

    var body: some View {
        VStack(spacing: 12) {
            // Summary line
            Text(AppStrings.Site.itemsProcessed(details.processedEntities, details.totalEntities))
                .font(.subheadline)
                .foregroundColor(.secondary)

            // Entity breakdown grid - ordered by upsert sequence (parents first, children last)
            // Upsert order: Equipment → Connections → Sessions → Tasks → Quotes → Issues → Photos → IR Photos
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 8) {
                // Row 1: Equipment, Connections (processed first)
                if details.totalNodes > 0 {
                    EntityProgressRow(
                        icon: "cube.fill",
                        label: AppStrings.Site.equipment,
                        processed: details.processedNodes,
                        total: details.totalNodes,
                        isActive: details.currentStep == .nodes
                    )
                }
                if details.totalEdges > 0 {
                    EntityProgressRow(
                        icon: "line.diagonal",
                        label: AppStrings.Tabs.connections,
                        processed: details.processedEdges,
                        total: details.totalEdges,
                        isActive: details.currentStep == .edges
                    )
                }
                // Row 2: Sessions, Tasks
                if details.totalIRSessions > 0 {
                    EntityProgressRow(
                        icon: "video.fill",
                        label: AppStrings.Sessions.irSessions,
                        processed: details.processedIRSessions,
                        total: details.totalIRSessions,
                        isActive: details.currentStep == .sessions
                    )
                }
                if details.totalTasks > 0 {
                    EntityProgressRow(
                        icon: "checkmark.circle.fill",
                        label: AppStrings.Sessions.tasks,
                        processed: details.processedTasks,
                        total: details.totalTasks,
                        isActive: details.currentStep == .tasks
                    )
                }
                // Row 3: Quotes, Issues
                if details.totalQuotes > 0 {
                    EntityProgressRow(
                        icon: "doc.text.fill",
                        label: AppStrings.Quotes.quotes,
                        processed: details.processedQuotes,
                        total: details.totalQuotes,
                        isActive: details.currentStep == .quotes
                    )
                }
                if details.totalIssues > 0 {
                    EntityProgressRow(
                        icon: "exclamationmark.triangle.fill",
                        label: AppStrings.Sessions.issues,
                        processed: details.processedIssues,
                        total: details.totalIssues,
                        isActive: details.currentStep == .issues
                    )
                }
                // Row 4: Photos, IR Photos (processed last)
                if details.totalPhotos > 0 {
                    EntityProgressRow(
                        icon: "photo.fill",
                        label: AppStrings.Sessions.photos,
                        processed: details.processedPhotos,
                        total: details.totalPhotos,
                        isActive: details.currentStep == .photos
                    )
                }
                if details.totalIRPhotos > 0 {
                    EntityProgressRow(
                        icon: "camera.metering.center.weighted",
                        label: AppStrings.Sessions.irPhotos,
                        processed: details.processedIRPhotos,
                        total: details.totalIRPhotos,
                        isActive: details.currentStep == .irPhotos
                    )
                }
            }
        }
        .padding(16)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - Clear Progress Detail View

struct ClearProgressDetailView: View {
    let details: ClearProgressDetails

    private var currentEntityType: String {
        details.currentEntityType
    }

    var body: some View {
        VStack(spacing: 12) {
            // Summary line
            Text(AppStrings.Site.itemsCleared(details.clearedEntities, details.totalEntities))
                .font(.subheadline)
                .foregroundColor(.secondary)

            // Entity breakdown grid - ordered by deletion sequence
            // Deletion order: Properties → IR Photos → Tasks → Forms → Issues → Quotes → Sessions → Connections → Equipment → Photos (last for bulk delete)
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 8) {
                // Row 1: Properties, IR Photos (deleted first)
                if details.totalProperties > 0 {
                    EntityProgressRow(
                        icon: "list.bullet",
                        label: AppStrings.Site.properties,
                        processed: details.clearedProperties,
                        total: details.totalProperties,
                        isActive: currentEntityType == "properties"
                    )
                }
                if details.totalIRPhotos > 0 {
                    EntityProgressRow(
                        icon: "camera.metering.center.weighted",
                        label: AppStrings.Sessions.irPhotos,
                        processed: details.clearedIRPhotos,
                        total: details.totalIRPhotos,
                        isActive: currentEntityType == "IR photos"
                    )
                }
                // Row 2: Tasks, Forms
                if details.totalTasks > 0 {
                    EntityProgressRow(
                        icon: "checkmark.circle.fill",
                        label: AppStrings.Sessions.tasks,
                        processed: details.clearedTasks,
                        total: details.totalTasks,
                        isActive: currentEntityType == "tasks"
                    )
                }
                if details.totalForms > 0 {
                    EntityProgressRow(
                        icon: "doc.plaintext.fill",
                        label: AppStrings.Tasks.forms,
                        processed: details.clearedForms,
                        total: details.totalForms,
                        isActive: currentEntityType == "forms"
                    )
                }
                // Row 3: Issues, Quotes
                if details.totalIssues > 0 {
                    EntityProgressRow(
                        icon: "exclamationmark.triangle.fill",
                        label: AppStrings.Sessions.issues,
                        processed: details.clearedIssues,
                        total: details.totalIssues,
                        isActive: currentEntityType == "issues"
                    )
                }
                if details.totalQuotes > 0 {
                    EntityProgressRow(
                        icon: "doc.text.fill",
                        label: AppStrings.Quotes.quotes,
                        processed: details.clearedQuotes,
                        total: details.totalQuotes,
                        isActive: currentEntityType == "quotes"
                    )
                }
                // Row 4: Sessions, Connections
                if details.totalSessions > 0 {
                    EntityProgressRow(
                        icon: "video.fill",
                        label: AppStrings.Sessions.irSessions,
                        processed: details.clearedSessions,
                        total: details.totalSessions,
                        isActive: currentEntityType == "sessions"
                    )
                }
                if details.totalEdges > 0 {
                    EntityProgressRow(
                        icon: "line.diagonal",
                        label: AppStrings.Tabs.connections,
                        processed: details.clearedEdges,
                        total: details.totalEdges,
                        isActive: currentEntityType == "connections"
                    )
                }
                // Row 5: Equipment, Photos (Photos deleted after Equipment to avoid inverse relationship issues)
                if details.totalNodes > 0 {
                    EntityProgressRow(
                        icon: "cube.fill",
                        label: AppStrings.Site.equipment,
                        processed: details.clearedNodes,
                        total: details.totalNodes,
                        isActive: currentEntityType == "equipment"
                    )
                }
                if details.totalPhotos > 0 {
                    EntityProgressRow(
                        icon: "photo.fill",
                        label: AppStrings.Sessions.photos,
                        processed: details.clearedPhotos,
                        total: details.totalPhotos,
                        isActive: currentEntityType == "photos"
                    )
                }
            }
        }
        .padding(16)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(12)
    }
}

// MARK: - Entity Progress Row

struct EntityProgressRow: View {
    let icon: String
    let label: String
    let processed: Int
    let total: Int
    let isActive: Bool

    private var isComplete: Bool {
        processed >= total && total > 0
    }

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundColor(isActive ? .accentColor : (isComplete ? .green : .secondary))
                .frame(width: 20)

            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption)
                    .foregroundColor(isActive ? .primary : .secondary)
                    .fontWeight(isActive ? .medium : .regular)

                Text("\(processed)/\(total)")
                    .font(.caption2)
                    .foregroundColor(isComplete ? .green : (isActive ? .accentColor : .secondary))
            }

            Spacer()

            if isComplete {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 12))
                    .foregroundColor(.green)
            } else if isActive {
                ProgressView()
                    .scaleEffect(0.6)
            }
        }
        .padding(.vertical, 4)
        .padding(.horizontal, 8)
        .background(isActive ? Color.accentColor.opacity(0.1) : Color.clear)
        .cornerRadius(6)
    }
}

// MARK: - Preview
struct SLDLoadingView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            // Loading state
            SLDLoadingView(
                syncService: {
                    let service = SLDSyncService.shared
                    service.isSyncing = true
                    service.syncProgress = 0.6
                    service.syncStatusMessage = "Processing diagram data..."
                    return service
                }(),
                siteName: "Main Facility",
                onReturnToSiteSelection: {}
            )
            .previewDisplayName("Loading")

            // Error state
            SLDLoadingView(
                syncService: {
                    let service = SLDSyncService.shared
                    service.isSyncing = false
                    service.syncErrorMessage = "Failed to process site data. The server returned data in an unexpected format."
                    return service
                }(),
                siteName: "Main Facility",
                onReturnToSiteSelection: {}
            )
            .previewDisplayName("Error")
        }
    }
}
