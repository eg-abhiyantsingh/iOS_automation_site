//
//  LinkExistingNodeView.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData

/// A sheet view for linking existing nodes as children of a parent node
struct LinkExistingNodeView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState

    @StateObject private var viewModel: LinkExistingNodeViewModel

    let activeSession: IRSession?
    let activeViewId: UUID?
    let onLinked: () -> Void

    init(parent: NodeV2, sld: SLDV2, activeSession: IRSession?, activeViewId: UUID?, onLinked: @escaping () -> Void) {
        self.activeSession = activeSession
        self.activeViewId = activeViewId
        self.onLinked = onLinked
        self._viewModel = StateObject(wrappedValue: LinkExistingNodeViewModel(sld: sld, parent: parent))
    }

    var body: some View {
        NavigationView {
            content
                .navigationTitle(AppStrings.AssetsExtra.linkExistingNodes)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(AppStrings.Common.cancel) { dismiss() }
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        if !viewModel.isLoading && !viewModel.filteredNodes.isEmpty {
                            Button(action: viewModel.toggleSelectAll) {
                                Text(viewModel.isAllFilteredSelected ? "Deselect All" : "Select All")
                                    .font(.subheadline)
                            }
                        }
                    }
                }
                .disabled(viewModel.isLinking)
                .overlay(alignment: .bottom) {
                    if !viewModel.selectedNodeIds.isEmpty && !viewModel.isLinking {
                        linkButton
                    }
                }
                .overlay {
                    if viewModel.isLinking {
                        ProgressView(AppStrings.AssetsExtra.linking)
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(10)
                            .shadow(radius: 10)
                    }
                }
                .sheet(isPresented: $viewModel.showingLocationPicker) {
                    HierarchicalLocationPickerView(
                        sld: viewModel.sld,
                        selectedRoom: $viewModel.filterRoom,
                        readOnly: true,
                        onDismiss: { viewModel.showingLocationPicker = false }
                    )
                }
        }
        .task {
            viewModel.loadEligibleNodes()
        }
        .onChange(of: viewModel.searchText) { _, _ in
            viewModel.onFilterInputChanged()
        }
        .onChange(of: viewModel.filterRoom?.id) { _, _ in
            viewModel.onFilterInputChanged(debounce: false)
        }
    }

    // MARK: - Content

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if !viewModel.hasEligibleNodes {
            emptyState
        } else {
            VStack(spacing: 0) {
                searchBar

                if let room = viewModel.filterRoom {
                    roomFilterChip(room)
                }

                nodeList
            }
        }
    }

    // MARK: - Subviews

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "link.circle")
                .font(.system(size: 60))
                .foregroundColor(.gray.opacity(0.5))
            Text(AppStrings.AssetsExtra.noEligibleNodes)
                .font(.headline)
            Text(AppStrings.AssetsExtra.noEligibleNodesDescription)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)

            TextField(AppStrings.AssetsExtra.searchByLabelClass, text: $viewModel.searchText)
                .textFieldStyle(PlainTextFieldStyle())

            if !viewModel.searchText.isEmpty {
                Button(action: { viewModel.searchText = "" }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }

            Button(action: { viewModel.showingLocationPicker = true }) {
                Image(systemName: "building.2")
                    .foregroundColor(viewModel.filterRoom != nil ? .blue : .secondary)
            }
        }
        .padding(8)
        .background(Color(UIColor.systemGray6))
        .cornerRadius(8)
        .padding(.horizontal)
        .padding(.bottom, viewModel.filterRoom != nil ? 4 : 8)
    }

    private func roomFilterChip(_ room: Room) -> some View {
        HStack(spacing: 6) {
            Image(systemName: "building.2")
                .font(.caption2)
            Text(room.fullPath)
                .font(.caption)
            Button(action: { viewModel.filterRoom = nil }) {
                Image(systemName: "xmark.circle.fill")
                    .font(.caption2)
            }
        }
        .foregroundColor(.blue)
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(Color.blue.opacity(0.1))
        .cornerRadius(16)
        .padding(.horizontal)
        .padding(.bottom, 4)
    }

    private var nodeList: some View {
        let nodes = viewModel.filteredNodes
        let lastIndex = nodes.count - 1
        return ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(Array(nodes.enumerated()), id: \.element.id) { index, item in
                    LinkExistingNodeRow(
                        item: item,
                        isSelected: viewModel.selectedNodeIds.contains(item.id),
                        onToggle: { viewModel.toggleSelection(item.id) }
                    )
                    .equatable()
                    .padding(.horizontal, 16)
                    .padding(.vertical, 4)

                    if index != lastIndex {
                        Divider()
                            .padding(.leading, 60)
                    }
                }
            }
            .padding(.vertical, 8)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private var linkButton: some View {
        Button(action: {
            viewModel.linkSelectedNodes(
                activeSession: activeSession,
                activeViewId: activeViewId,
                networkState: networkState,
                modelContext: modelContext,
                onComplete: onLinked
            )
        }) {
            Text(AppStrings.AssetsExtra.linkCountButton(viewModel.selectedNodeIds.count))
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Color.blue)
                .foregroundColor(.white)
                .cornerRadius(12)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }
}

// MARK: - Lightweight row using cached NodeItem (no SwiftData access during scrolling)

private struct LinkExistingNodeRow: View, Equatable {
    let item: LinkExistingNodeViewModel.NodeItem
    let isSelected: Bool
    let onToggle: () -> Void

    static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.item.id == rhs.item.id && lhs.isSelected == rhs.isSelected
    }

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundColor(isSelected ? .blue : .gray)

                NodeTypeIconCircle(
                    style: item.classStyle,
                    size: 32,
                    iconSize: 16,
                    backgroundColor: Color.blue.opacity(0.1),
                    iconColor: .blue
                )

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.label)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)

                    if let className = item.className {
                        Text(className)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    if let roomName = item.roomName {
                        Text(roomName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}
