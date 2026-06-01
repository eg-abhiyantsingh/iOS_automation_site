//
//  SLDViewSelectorView.swift
//  Egalvanic PZ
//
//  View selector screen for SLD views - gates access to SLD tab
//

import SwiftUI
import SwiftData

struct SLDViewSelectorView: View {
    let sld: SLDV2

    // Environment objects needed for WebView
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    // Query all views for this SLD
    @Query private var allViews: [SLDViewV2]
    @Query private var allLinks: [SLDLinkV2]

    // Filter views for this specific SLD
    private var views: [SLDViewV2] {
        let filtered = allViews.filter { $0.sld_id == sld.id && !$0.is_deleted }
            .sorted { v1, v2 in
                // Default view first, then by name
                if v1.is_default && !v2.is_default { return true }
                if !v1.is_default && v2.is_default { return false }
                return v1.name < v2.name
            }
        AppLogger.log(.debug, "views computed: \(filtered.count) views for SLD \(sld.id)", category: .ui)
        return filtered
    }

    // Get links for the selected view
    private func linksForView(_ viewId: UUID) -> [SLDLinkV2] {
        let links = allLinks.filter { $0.source_sld_view_id == viewId && !$0.is_deleted }
        AppLogger.log(.debug, "linksForView(\(viewId)): \(links.count) links", category: .ui)
        return links
    }

    // State for navigation
    @State private var selectedView: SLDViewV2?
    @State private var isShowingAllNodes = false  // True when "All Nodes" mode is selected
    @State private var isShowingSLD = false

    // State for view creation/editing
    @State private var showCreateViewSheet = false
    @State private var viewToEdit: SLDViewV2?

    var body: some View {
        let _ = AppLogger.log(.debug, "SLDViewSelectorView body evaluated, isShowingSLD=\(isShowingSLD), selectedView=\(selectedView?.name ?? "nil")", category: .ui)
        VStack(spacing: 0) {
            // Always show the view selector list (includes "All Nodes" option)
            let _ = AppLogger.log(.debug, "Showing viewSelectorList (\(views.count) views)", category: .ui)
            viewSelectorList
        }
        .navigationTitle(AppStrings.SLD.selectView)
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                NetworkStatusButton()
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                // Create new view button - always visible
                Button {
                    showCreateViewSheet = true
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 17))
                }
            }
        }
        .sheet(isPresented: $showCreateViewSheet) {
            SLDViewFormView(sld: sld, existingView: nil)
                .environmentObject(networkState)
        }
        .sheet(item: $viewToEdit) { view in
            SLDViewFormView(sld: sld, existingView: view)
                .environmentObject(networkState)
        }
        .fullScreenCover(isPresented: $isShowingSLD) {
            let _ = AppLogger.log(.debug, "fullScreenCover content building, isShowingAllNodes=\(isShowingAllNodes), selectedView=\(selectedView?.name ?? "nil")", category: .ui)
            if isShowingAllNodes {
                // All Assets mode - selectedViewId is nil
                let _ = AppLogger.log(.debug, "Creating SLDWebViewContainer for ALL ASSETS mode", category: .ui)
                SLDWebViewContainer(
                    sld: sld,
                    selectedViewId: nil,
                    sldLinks: [],  // No links in All Assets mode
                    viewName: AppStrings.SLD.allAssets,
                    onBack: {
                        AppLogger.log(.debug, "onBack called from All Assets mode", category: .ui)
                        isShowingSLD = false
                        isShowingAllNodes = false
                    },
                    onNavigateToView: { targetViewId in
                        AppLogger.log(.debug, "onNavigateToView called from All Assets mode - switching to view: \(targetViewId)", category: .ui)
                        // Switch from All Nodes mode to a specific view
                        if let targetView = views.first(where: { $0.id == targetViewId }) {
                            isShowingAllNodes = false
                            selectedView = targetView
                        } else {
                            AppLogger.log(.notice, "Target view not found", category: .ui)
                        }
                    }
                )
                .environmentObject(networkState)
                .environmentObject(appState)
            } else if let view = selectedView {
                let _ = AppLogger.log(.debug, "Creating SLDWebViewContainer for view: \(view.name) (\(view.id))", category: .ui)
                SLDWebViewContainer(
                    sld: sld,
                    selectedViewId: view.id,
                    sldLinks: linksForView(view.id),
                    viewName: view.name,
                    onBack: {
                        AppLogger.log(.debug, "onBack called, dismissing fullScreenCover", category: .ui)
                        isShowingSLD = false
                        selectedView = nil
                    },
                    onNavigateToView: { targetViewId in
                        AppLogger.log(.debug, "onNavigateToView called with targetViewId: \(targetViewId)", category: .ui)
                        // Find the target view and navigate to it
                        if let targetView = views.first(where: { $0.id == targetViewId }) {
                            AppLogger.log(.debug, "Found target view: \(targetView.name)", category: .ui)
                            selectedView = targetView
                            // The view will automatically update since we're just changing the selected view
                        } else {
                            AppLogger.log(.notice, "Target view not found", category: .ui)
                        }
                    }
                )
                .environmentObject(networkState)
                .environmentObject(appState)
            } else {
                // Debug: show error if selectedView is unexpectedly nil
                let _ = AppLogger.log(.error, "selectedView is nil in fullScreenCover", category: .ui)
                VStack {
                    Text(AppStrings.SLD.errorNoViewSelected)
                        .foregroundColor(.red)
                    Button(AppStrings.CommonExtra.close) {
                        isShowingSLD = false
                    }
                }
            }
        }
        .onAppear {
            AppLogger.log(.debug, "SLDViewSelectorView onAppear, views.count=\(views.count)", category: .ui)
        }
    }

    // MARK: - View Components

    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: "rectangle.3.group")
                .font(.system(size: 60))
                .foregroundColor(.secondary)

            Text(AppStrings.SLD.noViewsAvailable)
                .font(.title2)
                .fontWeight(.semibold)

            Text(AppStrings.SLD.noViewsDescription)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var viewSelectorList: some View {
        List {
            Section {
                // All Assets row at the top
                allNodesRow

                // View-specific options
                ForEach(views, id: \.id) { view in
                    viewRow(view)
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    private var allNodesRow: some View {
        Button {
            isShowingAllNodes = true
            selectedView = nil
            isShowingSLD = true
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "square.grid.2x2")
                    .font(.title2)
                    .foregroundColor(.orange)
                    .frame(width: 40)

                Text(AppStrings.SLD.allAssets)
                    .font(.headline)
                    .foregroundColor(.primary)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.vertical, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func viewRow(_ view: SLDViewV2) -> some View {
        Button {
            selectedView = view
            isShowingSLD = true
        } label: {
            HStack(spacing: 12) {
                // View type icon
                Image(systemName: iconForViewType(view.view_type))
                    .font(.title2)
                    .foregroundColor(colorForViewType(view.view_type))
                    .frame(width: 40)

                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(view.name)
                            .font(.headline)
                            .foregroundColor(.primary)

                        if view.is_default {
                            Text(AppStrings.SLD.defaultLabel)
                                .font(.caption)
                                .fontWeight(.medium)
                                .foregroundColor(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.blue)
                                .cornerRadius(4)
                        }
                    }

                    // Hide auto-generated descriptions for location-based views
                    if view.view_type != "location",
                       let description = view.viewDescription, !description.isEmpty {
                        Text(description)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }

                    // Only show type label if not empty
                    let typeLabel = viewTypeLabel(view.view_type)
                    if !typeLabel.isEmpty {
                        Text(typeLabel)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.vertical, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button {
                viewToEdit = view
            } label: {
                Label(AppStrings.SLD.edit, systemImage: "pencil")
            }
        }
    }

    // MARK: - Helper Functions

    private func iconForViewType(_ type: String) -> String {
        switch type {
        case "default":
            return "rectangle.3.group.fill"
        case "location":
            return "mappin.circle.fill"
        case "custom":
            return "square.grid.2x2.fill"
        default:
            return "rectangle.fill"
        }
    }

    private func colorForViewType(_ type: String) -> Color {
        switch type {
        case "default":
            return .blue
        case "location":
            return .green
        case "custom":
            return .purple
        default:
            return .gray
        }
    }

    private func viewTypeLabel(_ type: String) -> String {
        switch type {
        case "default":
            return AppStrings.SLD.defaultView
        case "location":
            return AppStrings.SLD.locationBasedView
        case "custom":
            return ""  // No label for custom views
        default:
            return ""
        }
    }
}

// MARK: - SLD WebView Container

/// Container view that wraps WebAppContainerView with view-specific data
struct SLDWebViewContainer: View {
    let sld: SLDV2
    let selectedViewId: UUID?  // nil = "All Nodes" mode
    let sldLinks: [SLDLinkV2]
    let onBack: () -> Void
    let onNavigateToView: (UUID) -> Void
    let viewName: String

    init(sld: SLDV2, selectedViewId: UUID?, sldLinks: [SLDLinkV2], viewName: String, onBack: @escaping () -> Void, onNavigateToView: @escaping (UUID) -> Void) {
        AppLogger.log(.debug, "SLDWebViewContainer init - sld: \(sld.id), viewId: \(selectedViewId?.uuidString ?? "ALL NODES"), links: \(sldLinks.count)", category: .ui)
        self.sld = sld
        self.selectedViewId = selectedViewId
        self.sldLinks = sldLinks
        self.viewName = viewName
        self.onBack = onBack
        self.onNavigateToView = onNavigateToView
    }

    var body: some View {
        let _ = AppLogger.log(.debug, "SLDWebViewContainer body evaluated", category: .ui)
        VStack(spacing: 0) {
            // Native header with back button and title
            HStack {
                Button(action: {
                    AppLogger.log(.debug, "Back button tapped", category: .ui)
                    onBack()
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 17, weight: .semibold))
                        Text(AppStrings.SLD.views)
                            .font(.body)
                    }
                }
                .padding(.leading, 16)

                Spacer()

                Text(viewName)
                    .font(.headline)
                    .lineLimit(1)

                Spacer()

                // Invisible spacer to balance the back button
                HStack(spacing: 4) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 17, weight: .semibold))
                    Text(AppStrings.SLD.views)
                        .font(.body)
                }
                .opacity(0)
                .padding(.trailing, 16)
            }
            .frame(height: 44)
            .background(Color(UIColor.systemBackground))

            Divider()

            WebAppContainerViewWithView(
                sld: sld,
                selectedViewId: selectedViewId,
                sldLinks: sldLinks,
                onBack: onBack,
                onNavigateToView: onNavigateToView
            )
        }
        .ignoresSafeArea(.all, edges: .bottom)
        .onAppear {
            AppLogger.log(.debug, "SLDWebViewContainer onAppear", category: .ui)
        }
    }
}

