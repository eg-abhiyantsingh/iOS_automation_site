//
//  SLDSelectorView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/8/25.
//

import SwiftUI
import SwiftData

// Helper struct for pending site selection
private struct PendingSite: Equatable {
    let id: UUID
    let name: String
}

struct SLDSelectorView: View {
    let availableSLDs: [SLDChoice]
    let isLoading: Bool
    let onSelection: (UUID) -> Void
    let onCancel: (() -> Void)?
    var activeSLDId: UUID? = nil

    @State private var searchText: String = ""
    @State private var showCreateSite = false
    @State private var pendingSiteSelection: PendingSite? = nil
    @State private var showEditSite = false
    @State private var siteToEdit: SLDChoice? = nil

    // ZP-1847: per-site pending sync counts shown as a chip on each row.
    // Mirrors Android's site picker: items with NULL siteId or NULL userId
    // count toward every site / every user (legacy + migration safety).
    @Query private var pendingSyncItems: [SyncQueueItem]
    @EnvironmentObject private var appState: AppStateManager

    private var pendingSyncCounts: [UUID: Int] {
        let nullSentinel = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
        let currentUserId: UUID? = appState.userId == nullSentinel ? nil : appState.userId

        let userScoped = pendingSyncItems.filter { item in
            guard let cuid = currentUserId else { return true }
            return item.userId == nil || item.userId == cuid
        }

        let nilSiteCount = userScoped.filter { $0.siteId == nil }.count
        var counts: [UUID: Int] = [:]
        for item in userScoped {
            if let sid = item.siteId {
                counts[sid, default: 0] += 1
            }
        }
        if nilSiteCount > 0 {
            for sld in availableSLDs {
                counts[sld.id, default: 0] += nilSiteCount
            }
        }
        return counts
    }

    // Filter out deleted SLDs, apply search, and sort alphabetically
    private var filteredSLDs: [SLDChoice] {
        let active = availableSLDs.filter { !$0.is_deleted }

        let filtered: [SLDChoice]
        if searchText.isEmpty {
            filtered = active
        } else {
            filtered = active.filter { sld in
                sld.name.localizedCaseInsensitiveContains(searchText) ||
                sld.city?.localizedCaseInsensitiveContains(searchText) == true ||
                sld.state_province?.localizedCaseInsensitiveContains(searchText) == true ||
                sld.address_formatted?.localizedCaseInsensitiveContains(searchText) == true ||
                sld.address_line_1?.localizedCaseInsensitiveContains(searchText) == true ||
                sld.postal_code?.localizedCaseInsensitiveContains(searchText) == true
            }
        }

        // Sort alphabetically by name
        return filtered.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Compact header with cancel button
            if onCancel != nil {
                compactHeaderWithCancel
                    .padding(.bottom, 12)
            } else {
                standardHeader
                    .padding(.bottom, 20)
            }

            searchBar
                .padding(.bottom, 12)
            createButton
                .padding(.bottom, 16)
            contentView
        }
        .frame(maxHeight: .infinity, alignment: .top)
        .padding(.vertical, 8)
        .sheet(isPresented: $showCreateSite) {
            CreateSiteView(onSiteCreated: { sldId, siteName in
                // Auto-select the newly created site
                pendingSiteSelection = PendingSite(id: sldId, name: siteName)
            })
        }
        .sheet(item: $siteToEdit) { site in
            EditSiteView(site: site, onSiteUpdated: {
                // Site was updated, refresh the list if needed
                // The availableSLDs should be refreshed by the parent
            })
        }
        .onChange(of: pendingSiteSelection) { _, newValue in
            if let pendingSite = newValue {
                onSelection(pendingSite.id)
                pendingSiteSelection = nil
            }
        }
    }

    // Standard header without cancel (original design)
    private var standardHeader: some View {
        Text(AppStrings.Site.selectSite)
            .font(.largeTitle)
            .fontWeight(.semibold)
            .padding(.horizontal)
    }

    // Compact header with cancel button (iOS navigation bar style)
    private var compactHeaderWithCancel: some View {
        HStack {
            Button(action: onCancel ?? {}) {
                Text(AppStrings.Common.cancel)
                    .font(.body)
                    .foregroundColor(.accentColor)
            }

            Spacer()

            Text(AppStrings.Site.selectSite)
                .font(.title3)
                .fontWeight(.semibold)

            Spacer()

            // Invisible placeholder to center the title
            Text(AppStrings.Common.cancel)
                .font(.body)
                .opacity(0)
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }

    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)
            TextField(AppStrings.Site.searchSites, text: $searchText)
                .textFieldStyle(.plain)
            if !searchText.isEmpty {
                Button(action: { searchText = "" }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(10)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(10)
        .padding(.horizontal)
    }

    private var createButton: some View {
        Button(action: { showCreateSite = true }) {
            HStack {
                Image(systemName: "plus.circle.fill")
                Text(AppStrings.Site.createNewSite)
                    .fontWeight(.medium)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.accentColor)
            .foregroundColor(.white)
            .cornerRadius(10)
        }
        .padding(.horizontal)
    }

    @ViewBuilder
    private var contentView: some View {
        if isLoading {
            loadingView
        } else if filteredSLDs.isEmpty {
            emptyView
        } else {
            sldListView
        }
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text(AppStrings.Site.loadingAvailableSites)
                .foregroundStyle(.secondary)
        }
        .padding()
    }

    private var emptyView: some View {
        VStack(spacing: 12) {
            if searchText.isEmpty {
                Text(AppStrings.Site.noSitesAvailable)
                    .foregroundStyle(.secondary)
            } else {
                Text(AppStrings.Site.noSitesMatch(searchText))
                    .foregroundStyle(.secondary)
                Button(AppStrings.Site.clearSearch) {
                    searchText = ""
                }
                .buttonStyle(.bordered)
            }
        }
        .padding()
    }

    private var sldListView: some View {
        ScrollView(.vertical) {
            VStack(spacing: 12) {
                ForEach(filteredSLDs, id: \.id) { sldChoice in
                    sldButton(for: sldChoice)
                }
            }
            .padding(.horizontal)
            .containerRelativeFrame(.horizontal)
        }
        .scrollDismissesKeyboard(.interactively)
        .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
    }

    private func sldButton(for sldChoice: SLDChoice) -> some View {
        Button(action: {
            onSelection(sldChoice.id)
        }) {
            sldButtonContent(for: sldChoice)
        }
        .buttonStyle(PlainButtonStyle())
        .contextMenu {
            Button {
                siteToEdit = sldChoice
            } label: {
                Label(AppStrings.Site.editSite, systemImage: "pencil")
            }
        }
    }

    private func sldButtonContent(for sldChoice: SLDChoice) -> some View {
        let isActive = activeSLDId == sldChoice.id
        let pendingCount = pendingSyncCounts[sldChoice.id] ?? 0

        return HStack(spacing: 12) {
            // Location icon if coordinates are available
            if sldChoice.address_latitude != nil && sldChoice.address_longitude != nil {
                Image(systemName: "mappin.circle.fill")
                    .foregroundColor(isActive ? .green : .accentColor)
                    .font(.title2)
            } else {
                Image(systemName: "building.2")
                    .foregroundColor(isActive ? .green : .secondary)
                    .font(.title2)
            }

            sldInfo(for: sldChoice, pendingCount: pendingCount)
            Spacer()

            if isActive {
                Text(AppStrings.Site.active)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.green)
                    .cornerRadius(6)
            }

            chevronIcon
        }
        .padding()
        .background(isActive ? Color.green.opacity(0.1) : backgroundColor)
        .cornerRadius(10)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(isActive ? Color.green : Color.clear, lineWidth: 2)
        )
    }

    private func sldInfo(for sldChoice: SLDChoice, pendingCount: Int) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(sldChoice.name)
                .font(.headline)
                .foregroundColor(.primary)

            // Display formatted address if available, otherwise fallback to city/state
            if let formattedAddress = sldChoice.address_formatted, !formattedAddress.isEmpty {
                // Show first line of formatted address (replace newlines with commas for compact display)
                let compactAddress = formattedAddress.replacingOccurrences(of: "\n", with: ", ")
                Text(compactAddress)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            } else if let city = sldChoice.city, let state = sldChoice.state_province {
                Text("\(city), \(state)")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            } else if let city = sldChoice.city {
                Text(city)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            } else if let state = sldChoice.state_province {
                Text(state)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            pendingSyncChip(count: pendingCount)
        }
    }

    private var chevronIcon: some View {
        Image(systemName: "chevron.right")
            .foregroundColor(.secondary)
    }

    @ViewBuilder
    private func pendingSyncChip(count: Int) -> some View {
        if count > 0 {
            Text(AppStrings.Site.pendingSyncCount(count))
                .font(.caption2)
                .fontWeight(.medium)
                .foregroundColor(.accentColor)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.accentColor.opacity(0.12))
                .cornerRadius(6)
        }
    }

    private var backgroundColor: Color {
        Color(UIColor.secondarySystemBackground)
    }
}
