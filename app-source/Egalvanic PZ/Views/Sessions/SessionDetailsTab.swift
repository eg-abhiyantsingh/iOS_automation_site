//
//  SessionDetailsTab.swift
//  SwiftDataTutorial
//
//  Details tab for IR Session
//

import SwiftUI
import SwiftData
import UIKit
import UserNotifications

struct SessionDetailsTab: View {
    @EnvironmentObject private var reportStore: ReportStore
    @Query private var allEquipment: [TestEquipment]

    @StateObject private var viewModel: SessionDetailsTabViewModel

    let networkState: NetworkState

    init(session: IRSession,
         showingCloseConfirmation: Binding<Bool>,
         isClosingSession: Binding<Bool>,
         networkState: NetworkState) {
        _viewModel = StateObject(wrappedValue: SessionDetailsTabViewModel(
            session: session,
            showingCloseConfirmation: showingCloseConfirmation,
            isClosingSession: isClosingSession
        ))
        self.networkState = networkState
    }
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Session Header
                VStack(alignment: .leading, spacing: 8) {
                    Text(AppStrings.Sessions.workOrderDetailsHeader)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                        .tracking(0.5)
                    
                    Text(viewModel.session.name)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)

                    // Status Badge
                    HStack(spacing: 12) {
                        if viewModel.isSessionActive {
                            HStack(spacing: 6) {
                                Circle()
                                    .fill(Color.green)
                                    .frame(width: 8, height: 8)
                                Text(AppStrings.Sessions.activeWorkOrder)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                    .foregroundColor(.green)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.green.opacity(0.1))
                            .cornerRadius(20)
                        } else {
                            HStack(spacing: 6) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.caption)
                                Text(AppStrings.Sessions.completed)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(20)
                        }
                        
                        if networkState.mode == .offline {
                            HStack(spacing: 6) {
                                Image(systemName: "wifi.slash")
                                    .font(.caption)
                                Text(AppStrings.CommonExtra.offline)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }
                            .foregroundColor(.orange)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.orange.opacity(0.1))
                            .cornerRadius(20)
                        }
                        
                        Spacer()
                    }
                }
                .padding(.horizontal)
                .padding(.top, 8)

                // Stats Cards
                HStack(spacing: 12) {
                    SessionDetailStatCard(
                        title: AppStrings.Sessions.completed,
                        count: viewModel.completedNodeTaskCount,
                        icon: "checkmark.circle.fill",
                        color: .green
                    )

                    SessionDetailStatCard(
                        title: AppStrings.Sessions.incompleteLabel,
                        count: viewModel.incompleteNodeTaskCount,
                        icon: "circle",
                        color: .red
                    )

                    SessionDetailStatCard(
                        title: AppStrings.Sessions.openIssues,
                        count: viewModel.openIssueCount,
                        icon: "exclamationmark.triangle",
                        color: .blue
                    )
                }
                .padding(.horizontal)

                // Session Information Card
                VStack(alignment: .leading, spacing: 20) {
                    Text(AppStrings.Sessions.informationHeader)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                        .tracking(0.5)

                    VStack(spacing: 20) {
                        // Type Row
                        InfoRow(
                            label: AppStrings.Sessions.irPhotoType,
                            value: viewModel.session.photo_type,
                            icon: "dot.radiowaves.right"
                        )

                        Divider()

                        // Start Date Row
                        InfoRow(
                            label: AppStrings.Sessions.started,
                            value: viewModel.formattedStartDate,
                            icon: "calendar"
                        )

                        // End Date Row (if completed)
                        if !viewModel.isSessionActive, let formattedEndDate = viewModel.formattedEndDate {
                            Divider()

                            InfoRow(
                                label: AppStrings.Sessions.completed,
                                value: formattedEndDate,
                                icon: "calendar.badge.checkmark"
                            )
                        }

                        // Quick QR Action dropdown (only for active sessions)
                        if viewModel.isSessionActive {
                            Divider()

                            QuickQRActionRow(networkState: networkState)
                        }

                        // Equipment (inline, only if assigned)
                        if !viewModel.session.equipmentIds.isEmpty {
                            let sessionEquipment = allEquipment.filter { viewModel.session.equipmentIds.contains($0.id) }
                            if !sessionEquipment.isEmpty {
                                Divider()

                                HStack(alignment: .top, spacing: 12) {
                                    Image(systemName: "wrench.and.screwdriver")
                                        .font(.body)
                                        .foregroundColor(.secondary)
                                        .frame(width: 24)

                                    VStack(alignment: .leading, spacing: 8) {
                                        Text(AppStrings.Common.equipment)
                                            .font(.caption)
                                            .foregroundColor(.secondary)

                                        FlowLayout(spacing: 8) {
                                            ForEach(sessionEquipment, id: \.id) { equipment in
                                                Text(equipment.name)
                                                    .font(.caption)
                                                    .lineLimit(1)
                                                    .padding(.horizontal, 10)
                                                    .padding(.vertical, 6)
                                                    .background(Color(UIColor.tertiarySystemFill))
                                                    .cornerRadius(16)
                                            }
                                        }
                                    }

                                    Spacer()
                                }
                            }
                        }
                    }
                }
                .padding(20)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(16)
                .padding(.horizontal)

                // Action Buttons - Hidden for now
                // VStack(spacing: 12) {
                //     // Generate/View Report Button
                //     Button {
                //         if reportStatus.isViewable {
                //             // Mark report as viewed and open directly
                //             reportStore.viewReport(for: session.id)
                //
                //             if let url = reportStatus.reportUrl,
                //                let reportURL = URL(string: url) {
                //                 UIApplication.shared.open(reportURL)
                //             }
                //         } else {
                //             // Start generation
                //             reportStore.generateReport(for: session)
                //         }
                //     } label: {
                //         HStack {
                //             if reportStatus.isGenerating {
                //                 ProgressView()
                //                     .progressViewStyle(CircularProgressViewStyle(tint: .white))
                //                     .scaleEffect(0.8)
                //             } else {
                //                 Image(systemName: reportButtonIcon)
                //                     .font(.body)
                //             }
                //             Text(reportButtonTitle)
                //                 .fontWeight(.semibold)
                //         }
                //         .frame(maxWidth: .infinity)
                //         .padding(.vertical, 14)
                //         .background(reportButtonColor)
                //         .foregroundColor(.white)
                //         .cornerRadius(12)
                //     }
                //     .disabled(reportStatus.isGenerating)
                //
                //     // Regenerate Report Button (shows only when report exists)
                //     if reportStatus.isViewable {
                //         Button {
                //             showingRegenerateConfirmation = true
                //         } label: {
                //             HStack {
                //                 Image(systemName: "arrow.clockwise")
                //                     .font(.body)
                //                 Text("Regenerate Report")
                //                     .fontWeight(.semibold)
                //             }
                //             .frame(maxWidth: .infinity)
                //             .padding(.vertical, 14)
                //             .background(Color.orange)
                //             .foregroundColor(.white)
                //             .cornerRadius(12)
                //         }
                //         .disabled(reportStatus.isGenerating)
                //     }
                //
                //     // Progress bar when generating
                //     if reportStatus.isGenerating {
                //         VStack(spacing: 8) {
                //             ProgressView(value: reportProgress)
                //                 .progressViewStyle(LinearProgressViewStyle(tint: .blue))
                //
                //             Text("Generating report... Leave the app to test notification!")
                //                 .font(.caption)
                //                 .foregroundColor(.secondary)
                //         }
                //     }
                // }
                // .padding(.horizontal)
                // .padding(.bottom, 20)
            }
        }
        .background(Color(UIColor.systemBackground))
        .alert(AppStrings.Sessions.regenerateReport, isPresented: $viewModel.showingRegenerateConfirmation) {
            Button(AppStrings.Sessions.regenerate, role: .destructive) {
                viewModel.regenerateReport()
            }
            Button(AppStrings.Common.cancel, role: .cancel) {}
        } message: {
            Text(AppStrings.Sessions.regenerateDescription)
        }
        .onReceive(NotificationCenter.default.publisher(for: .reportReady)) { notification in
            // Handle notification when app returns to foreground
            if let userInfo = notification.userInfo,
               let sessionId = userInfo["session_id"] as? String,
               let openReport = userInfo["openReport"] as? Bool {
                viewModel.handleReportNotification(sessionId: sessionId, openReport: openReport)
            }
        }
        .onAppear {
            viewModel.configure(reportStore: reportStore, networkState: networkState)
        }
    }
}

// Helper view for information rows
struct InfoRow: View {
    let label: String
    let value: String
    let icon: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.body)
                .foregroundColor(.secondary)
                .frame(width: 24)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(label)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(value)
                    .font(.body)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
            }
            
            Spacer()
        }
    }
}

// Updated SessionDetailStatCard with better styling
struct SessionDetailStatCard: View {
    let title: String
    let count: Int
    let icon: String
    let color: Color

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundColor(color)
                Spacer()
            }

            VStack(alignment: .leading, spacing: 4) {
                Text("\(count)")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)

                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(16)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(color.opacity(0.2), lineWidth: 1)
        )
    }
}

// Quick QR Action picker row
struct QuickQRActionRow: View {
    @ObservedObject var networkState: NetworkState

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "qrcode.viewfinder")
                .font(.body)
                .foregroundColor(.secondary)
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 4) {
                Text(AppStrings.Sessions.quickQRAction)
                    .font(.caption)
                    .foregroundColor(.secondary)

                Menu {
                    ForEach(QuickQRAction.allCases, id: \.self) { action in
                        Button {
                            networkState.quickQRAction = action
                        } label: {
                            HStack {
                                Text(action.rawValue)
                                if networkState.quickQRAction == action {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                } label: {
                    HStack {
                        Text(networkState.quickQRAction.rawValue)
                            .font(.body)
                            .fontWeight(.medium)
                            .foregroundColor(.primary)
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
                .id(networkState.quickQRAction) // Force refresh when value changes
            }

            Spacer()
        }
    }
}
