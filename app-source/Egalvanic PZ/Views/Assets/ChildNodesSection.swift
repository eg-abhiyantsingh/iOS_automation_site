//
//  ChildNodesSection.swift
//  Egalvanic PZ
//
//  Created by Claude on 2025.
//

import SwiftUI
import SwiftData

/// A standalone section view for managing child nodes within a parent node
struct ChildNodesSection: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState

    let node: NodeV2
    let sld: SLDV2
    let activeSession: IRSession?  // Optional active session for linking new children to session
    let activeViewId: UUID?  // Optional active SLD view ID - when set, skip graph update to avoid flash
    let onSelectChild: (NodeV2) -> Void
    let onCreateChild: () -> Void
    let onCreateMultiple: () -> Void
    let onPhotoWalkthrough: () -> Void
    let onLinkExisting: () -> Void

    @State private var showingAddMenu = false
    @State private var showingDeleteConfirmation = false
    @State private var nodeToDelete: NodeV2?
    @State private var showingUnlinkConfirmation = false
    @State private var nodeToUnlink: NodeV2?

    // Performance optimization: Cache child nodes instead of computing on every render
    @State private var cachedChildNodes: [NodeV2] = []

    /// Recomputes cached child nodes - call on appear and when children change
    private func recomputeCachedChildNodes() {
        cachedChildNodes = sld.nodes
            .filter { $0.parent_id == node.id && !$0.is_deleted }
            .sorted { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }
    }

    var body: some View {
        VStack(spacing: 16) {
            // Header
            HStack {
                HStack(spacing: 8) {
                    Image(systemName: "square.stack.3d.down.right")
                        .foregroundColor(.primary)
                    Text(AppStrings.AssetsExtra.ocp)
                        .font(.headline)

                    // Count badge
                    if !cachedChildNodes.isEmpty {
                        Text("\(cachedChildNodes.count)")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.blue)
                            .clipShape(Capsule())
                    }
                }

                Spacer()

                // Add button with menu
                Menu {
                    Button(action: onCreateChild) {
                        Label(AppStrings.AssetsExtra.createNewChild, systemImage: "plus.circle")
                    }
                    Button(action: onCreateMultiple) {
                        Label(AppStrings.PhotoWalkthrough.createMultiple, systemImage: "plus.rectangle.on.rectangle")
                    }
                    Button(action: onPhotoWalkthrough) {
                        Label(AppStrings.Sessions.photoWalkthroughLabel, systemImage: "camera.fill")
                    }
                    Button(action: onLinkExisting) {
                        Label(AppStrings.AssetsExtra.linkExistingNode, systemImage: "link")
                    }
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.title2)
                        .foregroundColor(.blue)
                }
            }

            if cachedChildNodes.isEmpty {
                // Empty state
                VStack(spacing: 12) {
                    Image(systemName: "square.stack.3d.down.right")
                        .font(.system(size: 40))
                        .foregroundColor(.gray.opacity(0.5))
                    Text(AppStrings.AssetsExtra.noChildAssets)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text(AppStrings.AssetsExtra.addOCPDevices)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
            } else {
                // Child nodes list
                VStack(spacing: 8) {
                    ForEach(cachedChildNodes) { child in
                        ChildNodeRow(
                            child: child,
                            onTap: { onSelectChild(child) },
                            onUnlink: {
                                nodeToUnlink = child
                                showingUnlinkConfirmation = true
                            },
                            onDelete: {
                                nodeToDelete = child
                                showingDeleteConfirmation = true
                            }
                        )
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
        .onAppear {
            recomputeCachedChildNodes()
        }
        .onChange(of: sld.nodes.filter { $0.parent_id == node.id && !$0.is_deleted }.count) { _, _ in
            recomputeCachedChildNodes()
        }
        .alert(AppStrings.AssetsExtra.deleteChildAsset, isPresented: $showingDeleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                nodeToDelete = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let child = nodeToDelete {
                    deleteChild(child)
                }
                nodeToDelete = nil
            }
        } message: {
            if let child = nodeToDelete {
                Text(AppStrings.AssetsExtra.deleteChildConfirm(child.label))
            }
        }
        .alert(AppStrings.AssetsExtra.unlinkChildAsset, isPresented: $showingUnlinkConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                nodeToUnlink = nil
            }
            Button(AppStrings.AssetsExtra.unlink, role: .destructive) {
                if let child = nodeToUnlink {
                    unlinkChild(child)
                }
                nodeToUnlink = nil
            }
        } message: {
            if let child = nodeToUnlink {
                Text(AppStrings.AssetsExtra.unlinkMessage(child.label))
            }
        }
    }

    private func deleteChild(_ child: NodeV2) {
        NodeService.deleteNodes(
            nodeIds: Set([child.id]),
            diagram: sld,
            networkState: networkState,
            modelContext: modelContext
        ) { success, message in
            if let msg = message {
                AppLogger.log(success ? .info : .error, msg, category: .node)
            }
            // Refresh cache after deletion
            if success {
                recomputeCachedChildNodes()
            }
        }
    }

    private func unlinkChild(_ child: NodeV2) {
        Task {
            await NodeService.unlinkChildNode(
                child: child,
                parent: node,
                networkState: networkState,
                modelContext: modelContext,
                skipGraphUpdate: activeViewId != nil
            )
            // Refresh cache after unlink
            await MainActor.run {
                recomputeCachedChildNodes()
            }
        }
    }
}

// MARK: - Child Node Row

struct ChildNodeRow: View {
    let child: NodeV2
    let onTap: () -> Void
    let onUnlink: () -> Void
    let onDelete: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                // Icon
                NodeTypeIconCircle(
                    style: child.node_class?.style,
                    size: 32,
                    iconSize: 16,
                    backgroundColor: (child.af_isComplete ? Color.green : Color.red).opacity(0.1),
                    iconColor: child.af_isComplete ? .green : .red
                )

                // Info
                VStack(alignment: .leading, spacing: 2) {
                    Text(child.label)
                        .font(.body)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)

                    if let className = child.node_class?.name {
                        Text(className)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color(.systemGray6))
            .cornerRadius(10)
        }
        .buttonStyle(PlainButtonStyle())
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button(role: .destructive, action: onDelete) {
                Label(AppStrings.Common.delete, systemImage: "trash")
            }

            Button(action: onUnlink) {
                Label(AppStrings.AssetsExtra.unlink, systemImage: "link.badge.plus")
            }
            .tint(.orange)
        }
        .contextMenu {
            Button(action: onTap) {
                Label(AppStrings.AssetsExtra.viewDetails, systemImage: "eye")
            }
            Button(action: onUnlink) {
                Label(AppStrings.AssetsExtra.unlinkFromParent, systemImage: "link.badge.plus")
            }
            Button(role: .destructive, action: onDelete) {
                Label(AppStrings.Common.delete, systemImage: "trash")
            }
        }
    }
}
