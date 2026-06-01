//
//  IRSessionDetailView.swift
//  SwiftDataTutorial
//
//  Simplified session detail view without shared mapping
//

import SwiftUI
import SwiftData

struct IRSessionDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var sldService: SLDService
    @EnvironmentObject var appState: AppStateManager

    @StateObject private var viewModel: IRSessionDetailViewModel

    // Navigation path binding from parent for room navigation (optional for backward compatibility)
    private var externalNavigationPath: Binding<NavigationPath>?

    // For legacy flow (via IRSessionView): present room detail as fullScreenCover
    @State private var selectedRoomForSheet: Room?

    /// Initialize with external navigation path (preferred - used from SiteTabView)
    init(session: IRSession, navigationPath: Binding<NavigationPath>) {
        _viewModel = StateObject(wrappedValue: IRSessionDetailViewModel(session: session))
        self.externalNavigationPath = navigationPath
    }

    /// Initialize without navigation path (legacy - used from IRSessionView)
    init(session: IRSession) {
        _viewModel = StateObject(wrappedValue: IRSessionDetailViewModel(session: session))
        self.externalNavigationPath = nil
    }

    /// Whether we have an external navigation path (determines navigation strategy)
    private var hasExternalNavigation: Bool {
        externalNavigationPath != nil
    }
    
    var body: some View {
        let cachedIncompleteNodeTaskCount = viewModel.incompleteNodeTaskCount
        let cachedOpenIssueCount = viewModel.openIssueCount
        VStack(spacing: 0) {
                // Tab Content
                TabView(selection: $viewModel.selectedTab) {
                    SessionDetailsTab(session: viewModel.session,
                                    showingCloseConfirmation: $viewModel.showingCloseConfirmation,
                                    isClosingSession: $viewModel.isClosingSession,
                                    networkState: networkState)
                        .tag(0)

                    LocationHierarchyView(
                        diagram: viewModel.sld,
                        session: viewModel.session,
                        onRoomSelected: { room in
                            AppLogger.log(.debug, "IRSessionDetailView: onRoomSelected for room '\(room.name)', hasExternalNavigation=\(hasExternalNavigation)", category: .ui)
                            if let externalPath = externalNavigationPath {
                                // Use navigation stack (preferred - push navigation with swipe back)
                                externalPath.wrappedValue.append(SiteNavDestination.roomDetail(session: viewModel.session, room: room))
                            } else {
                                // Use fullScreenCover (fallback for legacy flow via IRSessionView)
                                selectedRoomForSheet = room
                            }
                        }
                    )
                        .environmentObject(networkState)
                        .environmentObject(appState)
                        .environmentObject(sldService)
                        .tag(1)

                    GroupedTasksTab(session: viewModel.session,
                            selectedTask: $viewModel.selectedTask)
                        .tag(2)

                    IssuesTab(session: viewModel.session,
                            selectedIssue: $viewModel.selectedIssue)
                        .tag(3)

                    IRPhotosTab(session: viewModel.session)
                        .environmentObject(networkState)
                        .environmentObject(appState)
                        .tag(4)

                    AttachmentsTab(session: viewModel.session)
                        .tag(5)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                
                // Custom Tab Bar at bottom
                HStack(spacing: 0) {
                    TabButton(title: AppStrings.Sessions.details, icon: "info.circle", isSelected: viewModel.selectedTab == 0) {
                        viewModel.selectedTab = 0
                    }

                    TabButton(title: AppStrings.Tabs.assets, icon: "list.bullet", isSelected: viewModel.selectedTab == 1) {
                        viewModel.selectedTab = 1
                    }

                    TabButton(title: AppStrings.Sessions.tasks, icon: "checklist", count: cachedIncompleteNodeTaskCount, badgeColor: .red, isSelected: viewModel.selectedTab == 2) {
                        viewModel.selectedTab = 2
                    }

                    TabButton(title: AppStrings.Sessions.issues, icon: "exclamationmark.triangle", count: cachedOpenIssueCount, badgeColor: .blue, isSelected: viewModel.selectedTab == 3) {
                        viewModel.selectedTab = 3
                    }

                    TabButton(title: "IR", icon: "camera.metering.multispot", isSelected: viewModel.selectedTab == 4) {
                        viewModel.selectedTab = 4
                    }

                    TabButton(title: AppStrings.Sessions.files, icon: "paperclip", isSelected: viewModel.selectedTab == 5) {
                        viewModel.selectedTab = 5
                    }
                }
                .frame(height: 65)
                .background(Color(UIColor.systemBackground))
                .overlay(
                    Rectangle()
                        .fill(Color(UIColor.separator))
                        .frame(height: 0.5),
                    alignment: .top
                )
            }
        .navigationTitle(viewModel.session.name)
        .navigationBarTitleDisplayMode(.inline)
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
                // ZP-2173 — refresh fetches the SLD from the server and overwrites
                // the local entity rows that legacy queue items still depend on,
                // so block until the user resolves them.
                .disabled(viewModel.isOffline || viewModel.isRefreshing || networkState.hasLegacySyncItems)
            }
        }
        .disabled(viewModel.isClosingSession)
        .overlay {
            if viewModel.isClosingSession {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.Sessions.closingSession)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            } else {
                // Refresh progress overlay
                RefreshProgressOverlay(syncService: SLDSyncService.shared)
            }
        }
        .onAppear {
            viewModel.configure(
                modelContext: modelContext,
                networkState: networkState,
                sldService: sldService
            )
        }
        .fullScreenCover(item: $viewModel.selectedTask) { task in
            TaskDetailView(task: task)
                .environmentObject(networkState)
        }
        .sheet(item: $viewModel.selectedIssue) { issue in
            IssueDetailView(issue: issue)
                .environmentObject(networkState)
        }
        .alert(AppStrings.Sessions.closeSessionTitle(name: viewModel.session.name), isPresented: $viewModel.showingCloseConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Sessions.closeSession, role: .destructive) {
                Task {
                    await viewModel.confirmCloseSession()
                }
            }
        } message: {
            Text(AppStrings.Sessions.closeSessionDescription)
            if viewModel.isOffline {
                Text(AppStrings.Sessions.changesWillSyncNote)
            }
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $viewModel.showError) {
            Button(AppStrings.Common.ok) {
                viewModel.dismissError()
            }
        } message: {
            Text(viewModel.errorMessage)
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
        // Fallback room detail presentation for legacy flow (via IRSessionView)
        // When there's no external navigation path, present as fullScreenCover
        .fullScreenCover(item: $selectedRoomForSheet) { room in
            NavigationStack {
                SessionRoomDetailView(session: viewModel.session, room: room, isModal: true)
                    .environment(\.modelContext, modelContext)
                    .environmentObject(appState)
                    .environmentObject(sldService)
                    .environmentObject(networkState)
            }
        }
    }
}

// MARK: - Tab Button Component
struct TabButton: View {
    let title: String
    let icon: String
    var count: Int? = nil
    var badgeColor: Color? = nil
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                ZStack {
                    Image(systemName: icon)
                        .font(.system(size: 24))
                        .foregroundColor(isSelected ? .blue : .gray)

                    if let count = count, count > 0 {
                        Text("\(count)")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                            .frame(minWidth: 18, minHeight: 18)
                            .padding(.horizontal, count >= 10 ? 4 : 2)
                            .padding(.vertical, 2)
                            .background(badgeColor ?? (isSelected ? Color.blue : Color.gray))
                            .clipShape(Capsule())
                            .offset(x: count >= 100 ? 18 : 14, y: -10)
                    }
                }
                .frame(width: 32, height: 28)
                
                Text(title)
                    .font(.system(size: 11))
                    .fontWeight(isSelected ? .medium : .regular)
                    .foregroundColor(isSelected ? .blue : .gray)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}
