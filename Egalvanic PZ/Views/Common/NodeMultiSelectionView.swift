//
//  NodeMultiSelectionView.swift
//  Egalvanic PZ
//
//  Reusable view for selecting multiple nodes/assets.
//

import SwiftUI

struct NodeMultiSelectionView: View {
    @Environment(\.dismiss) private var dismiss

    let nodes: [NodeV2]
    @Binding var selectedNodeIds: Set<UUID>

    @State private var searchText = ""

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Selection count header
                if !selectedNodeIds.isEmpty {
                    HStack {
                        Text(AppStrings.Supporting.selectedCount(selectedNodeIds.count))
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        Spacer()

                        Button(AppStrings.AssetsExtra.clearAll) {
                            selectedNodeIds.removeAll()
                        }
                        .font(.subheadline)
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                    .background(Color(.systemGray6))
                }

                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)

                    TextField(AppStrings.CommonExtra.searchAssets, text: $searchText)
                        .textFieldStyle(.plain)

                    if !searchText.isEmpty {
                        Button {
                            searchText = ""
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(10)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()

                // Node list
                if filteredNodes.isEmpty {
                    VStack(spacing: 12) {
                        Spacer()
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                        Text(AppStrings.Supporting.noAssetsFound)
                            .font(.headline)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                } else {
                    List {
                        ForEach(filteredNodes) { node in
                            AttachmentNodeSelectionRow(
                                node: node,
                                isSelected: selectedNodeIds.contains(node.id),
                                onToggle: {
                                    toggleSelection(for: node)
                                }
                            )
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle(AppStrings.Supporting.selectAssets)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button(AppStrings.Common.done) {
                        dismiss()
                    }
                }
            }
        }
    }

    // MARK: - Computed Properties

    private var filteredNodes: [NodeV2] {
        if searchText.isEmpty {
            return nodes
        }

        let searchLower = searchText.lowercased()
        return nodes.filter { node in
            node.label.lowercased().contains(searchLower) ||
            (node.qr_code?.lowercased().contains(searchLower) ?? false)
        }
    }

    // MARK: - Actions

    private func toggleSelection(for node: NodeV2) {
        if selectedNodeIds.contains(node.id) {
            selectedNodeIds.remove(node.id)
        } else {
            selectedNodeIds.insert(node.id)
        }
    }
}

// MARK: - Attachment Node Selection Row

private struct AttachmentNodeSelectionRow: View {
    let node: NodeV2
    let isSelected: Bool
    let onToggle: () -> Void

    var body: some View {
        Button {
            onToggle()
        } label: {
            HStack(spacing: 12) {
                // Checkbox
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22))
                    .foregroundColor(isSelected ? .blue : .secondary)

                // Node info
                VStack(alignment: .leading, spacing: 4) {
                    Text(node.label)
                        .font(.body)
                        .fontWeight(isSelected ? .medium : .regular)
                        .foregroundColor(.primary)

                    if let qrCode = node.qr_code, !qrCode.isEmpty {
                        Text(qrCode)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                // Node type icon
                if let nodeClass = node.node_class {
                    Image(systemName: nodeTypeIcon(for: nodeClass))
                        .foregroundColor(.secondary)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowBackground(isSelected ? Color.blue.opacity(0.1) : Color.clear)
    }

    private func nodeTypeIcon(for nodeClass: NodeClass) -> String {
        // Return appropriate icon based on node class
        if nodeClass.ocp {
            return "bolt.fill"
        }
        return "square.fill"
    }
}
