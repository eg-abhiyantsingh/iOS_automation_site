//
//  IRSessionView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/2/25.
//

import SwiftUI
import SwiftData

struct IRSessionView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var sldService: SLDService

    @StateObject private var viewModel: IRSessionViewModel

    @Query private var irSessions: [IRSession]

    // Navigation path binding from parent for session navigation
    private var externalNavigationPath: Binding<NavigationPath>?

    init(diagram: SLDV2, navigationPath: Binding<NavigationPath>? = nil) {
        _viewModel = StateObject(wrappedValue: IRSessionViewModel(diagram: diagram))
        self.externalNavigationPath = navigationPath
        let sldId = diagram.id

        // Query all IR sessions for this SLD
        _irSessions = Query(
            filter: #Predicate<IRSession> { session in
                session.sld.id == sldId
            },
            sort: [SortDescriptor(\IRSession.date_created, order: .reverse)]
        )
    }

    private var displayedSessions: [IRSession] {
        viewModel.displayedSessions(irSessions)
    }

    private var nonDeletedSessions: [IRSession] {
        viewModel.nonDeletedSessions(irSessions)
    }

    private func handleSessionTap(_ session: IRSession) {
        guard session.id == viewModel.activeSession?.id else { return }
        if let path = externalNavigationPath {
            path.wrappedValue.append(SiteNavDestination.sessionDetail(session))
        } else {
            viewModel.selectSession(session)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        // Create New Session Button (always visible when sessions exist)
                        if !nonDeletedSessions.isEmpty {
                            Button(action: {
                                if !viewModel.hasActiveSession {
                                    viewModel.showingCreateSession = true
                                }
                            }) {
                                HStack {
                                    Image(systemName: "plus.circle.fill")
                                        .font(.title2)
                                    
                                    VStack(alignment: .leading, spacing: 4) {
                                        if viewModel.hasActiveSession {
                                            Text(AppStrings.Sessions.startNewWorkOrder)
                                                .font(.headline)
                                            Text(AppStrings.Sessions.endCurrentFirst)
                                                .font(.caption)
                                                .foregroundColor(Color.secondary)
                                        } else {
                                            Text(AppStrings.Sessions.startNewWorkOrder)
                                                .font(.headline)
                                            Text(AppStrings.Sessions.beginCapturing)
                                                .font(.caption)
                                                .foregroundColor(.white.opacity(0.8))
                                        }
                                    }
                                    
                                    Spacer()
                                    
                                    Image(systemName: "chevron.right")
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundColor(viewModel.hasActiveSession ? .secondary : .white.opacity(0.7))
                                }
                                .foregroundColor(viewModel.hasActiveSession ? .secondary : .white)
                                .padding()
                                .background(
                                    viewModel.hasActiveSession
                                        ? LinearGradient(
                                            gradient: Gradient(colors: [Color(.systemGray5), Color(.systemGray5)]),
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                        : LinearGradient(
                                            gradient: Gradient(colors: [Color.blue, Color.blue.opacity(0.8)]),
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                )
                                .cornerRadius(16)
                                .shadow(color: viewModel.hasActiveSession ? Color.clear : Color.blue.opacity(0.3), radius: 8, x: 0, y: 4)
                            }
                            .disabled(viewModel.hasActiveSession)
                            .padding(.horizontal)
                        }
                        
                        // Toggle and Sessions List (only show when sessions exist)
                        if !nonDeletedSessions.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                HStack {
                                    Text(viewModel.showAllSessions ? AppStrings.Sessions.allWorkOrders : AppStrings.Sessions.availableWorkOrders)
                                        .font(.headline)
                                        .foregroundColor(.secondary)

                                    Spacer()

                                    // Additional create button in header (always visible)
                                    if viewModel.hasActiveSession {
                                        Button(action: {
                                            viewModel.showingCreateSession = true
                                        }) {
                                            Image(systemName: "plus.circle")
                                                .font(.system(size: 20))
                                                .foregroundColor(.blue)
                                        }
                                    }

                                    // Toggle button
                                    Button(action: {
                                        viewModel.toggleShowAllSessions()
                                    }) {
                                        HStack(spacing: 4) {
                                            Text(viewModel.showAllSessions ? AppStrings.Sessions.showAvailableOnly : AppStrings.Sessions.showAll)
                                                .font(.caption)
                                                .fontWeight(.medium)
                                            Image(systemName: viewModel.showAllSessions ? "line.3.horizontal.decrease.circle" : "line.3.horizontal.circle")
                                                .font(.caption)
                                        }
                                        .foregroundColor(.blue)
                                    }
                                }
                                .padding(.horizontal)

                                if !displayedSessions.isEmpty {
                                    VStack(spacing: 12) {
                                        ForEach(displayedSessions) { session in
                                            IRSessionCard(
                                                session: session,
                                                isCurrentlyActive: session.id == viewModel.activeSession?.id,
                                                onActivate: session.active ? {
                                                    viewModel.requestActivation(session)
                                                } : nil,
                                                onEnd: session.id == viewModel.activeSession?.id ? {
                                                    viewModel.requestDeactivation(session)
                                                } : nil
                                            )
                                            .contentShape(Rectangle())
                                            .onTapGesture {
                                                handleSessionTap(session)
                                            }
                                        }
                                    }
                                    .padding(.horizontal)
                                }
                            }
                        }

                        // Empty State (no sessions at all)
                        if nonDeletedSessions.isEmpty {
                            VStack(spacing: 20) {
                                Image(systemName: "camera.metering.unknown")
                                    .font(.system(size: 48))
                                    .foregroundColor(.secondary)
                                
                                Text(AppStrings.Sessions.noWorkOrdersYet)
                                    .font(.headline)
                                    .foregroundColor(.primary)

                                Text(AppStrings.Sessions.startFirstWorkOrder)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)

                                Button(action: {
                                    viewModel.showingCreateSession = true
                                }) {
                                    HStack {
                                        Image(systemName: "plus.circle.fill")
                                        Text(AppStrings.Sessions.createFirstWorkOrder)
                                    }
                                    .font(.callout)
                                    .fontWeight(.medium)
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 20)
                                    .padding(.vertical, 12)
                                    .background(Color.blue)
                                    .cornerRadius(12)
                                }
                                .padding(.top, 8)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 60)
                            .padding(.horizontal)
                        }
                        
                        // Bottom padding
                        Color.clear.frame(height: 20)
                    }
                }
                .background(Color(UIColor.systemBackground))
            }
            .overlay {
                // Refresh progress overlay
                RefreshProgressOverlay(syncService: SLDSyncService.shared)
            }
        .navigationTitle(AppStrings.Sessions.workOrders)
        .toolbar(.hidden, for: .tabBar)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    Task {
                        await viewModel.refreshFromServer()
                    }
                }) {
                    Image(systemName: "arrow.clockwise")
                }
                .disabled(viewModel.isOffline || viewModel.isRefreshing)
            }
        }
        .onAppear {
            viewModel.configure(
                modelContext: modelContext,
                networkState: networkState,
                appState: appState,
                sldService: sldService
            )
        }
        .sheet(isPresented: $viewModel.showingCreateSession) {
            IRSessionCreationView(
                sld: viewModel.diagram,
                onSave: { newSession in
                    viewModel.onSessionCreated(newSession)
                }
            )
            .environmentObject(networkState)
        }
        .alert(AppStrings.Sessions.endWorkOrderSession, isPresented: $viewModel.showingDeactivateAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                viewModel.cancelDeactivation()
            }
            Button(AppStrings.Sessions.endSession, role: .destructive) {
                viewModel.confirmDeactivation()
            }
        } message: {
            if let session = viewModel.sessionToDeactivate {
                Text(AppStrings.Sessions.endSessionConfirm(name: session.name))
            }
        }
        .alert(AppStrings.Sessions.startWorkOrderSession, isPresented: $viewModel.showingActivateAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                viewModel.cancelActivation()
            }
            Button(AppStrings.Sessions.startSession) {
                viewModel.confirmActivation()
            }
        } message: {
            if let session = viewModel.sessionToActivate {
                Text(AppStrings.Sessions.startSessionConfirm(name: session.name))
            }
        }
        .alert(AppStrings.Sessions.refreshFailed, isPresented: $viewModel.showRefreshError) {
            Button(AppStrings.Common.ok, role: .cancel) { }
            if viewModel.isOffline {
                Button(AppStrings.Sessions.goOnline) {
                    viewModel.toggleOnlineAndRefresh()
                }
            }
        } message: {
            Text(viewModel.refreshErrorMessage)
        }
    }
}

// Custom stat card matching SiteTabView style
struct SessionStatCard: View {
    let title: String
    let count: Int
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                
                Spacer()
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text("\(count)")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                Text(title)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(16)
    }
}

// Updated session card with modern styling
struct IRSessionCard: View {
    let session: IRSession
    let isCurrentlyActive: Bool
    let onActivate: (() -> Void)?
    let onEnd: (() -> Void)?
    
    private var photoCount: Int {
        session.ir_photos.filter { !$0.is_deleted }.count
    }
    
    private var issueCount: Int {
        session.issues.filter { !$0.is_deleted }.count
    }
    
    private var taskCount: Int {
        session.user_tasks.filter { !$0.is_deleted }.count
    }

    private var assetCount: Int {
        session.nodes.filter { !$0.is_deleted }.count
    }

    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter
    }
    
    var body: some View {
        HStack {
            // Status indicator
            Circle()
                .fill(session.active ? Color.green : Color.gray.opacity(0.3))
                .frame(width: 12, height: 12)
            
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(session.name)
                        .font(.headline)
                        .foregroundColor(.primary)

                    if isCurrentlyActive {
                        Text(AppStrings.Sessions.activeBadge)
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.blue)
                            .cornerRadius(6)
                    }
                }

                if !session.active {
                    Text(AppStrings.Sessions.completed)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            // Show End button for currently active session
            if isCurrentlyActive {
                Button(action: {
                    onEnd?()
                }) {
                    Text(AppStrings.Sessions.end)
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.gray)
                        .cornerRadius(8)
                }
                .buttonStyle(PlainButtonStyle())
            }
            // Show Start button for sessions that aren't currently selected globally
            else if session.active && onActivate != nil {
                Button(action: {
                    onActivate?()
                }) {
                    Text(AppStrings.Sessions.start)
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.blue)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Color.blue, lineWidth: 1)
                        )
                }
                .buttonStyle(PlainButtonStyle())
            } else {
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.secondary.opacity(0.5))
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}
