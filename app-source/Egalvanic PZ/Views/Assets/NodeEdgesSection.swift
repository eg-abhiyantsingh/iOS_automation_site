//
//  NodeEdgesSection.swift
//  Egalvanic PZ
//
//  Created by Claude on 2025.
//

import SwiftUI
import SwiftData

/// Connection type for edge creation
enum EdgeConnectionType: Identifiable {
    case lineside  // Edge where target = this node (incoming)
    case loadside  // Edge where source = this node (outgoing)

    var id: String {
        switch self {
        case .lineside: return "lineside"
        case .loadside: return "loadside"
        }
    }
}

/// A standalone section view for managing edges connected to a node or its children
struct NodeEdgesSection: View {
    @Environment(\.modelContext) private var modelContext
    @StateObject private var vm: NodeEdgesViewModel

    let networkState: NetworkState
    let activeViewId: UUID?
    let onSelectEdge: (EdgeV2) -> Void

    // UI-only state
    @State private var selectedConnectionType: EdgeConnectionType?
    @State private var showingDeleteConfirmation = false
    @State private var edgeToDelete: EdgeV2?

    init(node: NodeV2, networkState: NetworkState, sld: SLDV2, activeViewId: UUID?, onSelectEdge: @escaping (EdgeV2) -> Void) {
        _vm = StateObject(wrappedValue: NodeEdgesViewModel(node: node, sld: sld))
        self.networkState = networkState
        self.activeViewId = activeViewId
        self.onSelectEdge = onSelectEdge
    }

    var body: some View {
        VStack(spacing: 16) {
            header

            ConnectionSubsection(
                title: AppStrings.AssetsExtra.lineside.uppercased(),
                items: vm.linesideItems,
                isMissingSource: vm.isMissingSource,
                onSelect: onSelectEdge,
                onDelete: { edge in
                    edgeToDelete = edge
                    showingDeleteConfirmation = true
                }
            )

            ConnectionSubsection(
                title: AppStrings.AssetsExtra.loadside.uppercased(),
                items: vm.loadsideItems,
                isMissingSource: false,
                onSelect: onSelectEdge,
                onDelete: { edge in
                    edgeToDelete = edge
                    showingDeleteConfirmation = true
                }
            )
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
        .onAppear {
            vm.recompute()
            vm.preloadEdgeClasses(modelContext: modelContext)
        }
        .sheet(item: $selectedConnectionType) { connectionType in
            CreateEdgeFromNodeView(
                contextNode: vm.node,
                sld: vm.sld,
                connectionType: connectionType,
                activeViewId: activeViewId,
                allEdgeClasses: vm.edgeClasses(),
                networkState: networkState,
                onCreated: { _ in
                    selectedConnectionType = nil
                    vm.recompute()
                }
            )
        }
        .alert(AppStrings.AssetsExtra.deleteConnection, isPresented: $showingDeleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                edgeToDelete = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let edge = edgeToDelete {
                    vm.deleteEdge(edge, networkState: networkState, modelContext: modelContext)
                }
                edgeToDelete = nil
            }
        } message: {
            if let edge = edgeToDelete {
                Text(AppStrings.AssetsExtra.deleteConnectionConfirm(
                    vm.linesideItems.first(where: { $0.edge.id == edge.id })?.title
                    ?? vm.loadsideItems.first(where: { $0.edge.id == edge.id })?.title
                    ?? AppStrings.AssetsExtra.unknown
                ))
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack {
            HStack(spacing: 8) {
                Image(systemName: "link")
                    .foregroundColor(.primary)
                Text(AppStrings.AssetsExtra.connections)
                    .font(.headline)

                let totalCount = vm.linesideItems.count + vm.loadsideItems.count
                if totalCount > 0 {
                    Text("\(totalCount)")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(Color.purple)
                        .clipShape(Capsule())
                }
            }

            Spacer()

            Menu {
                Button(action: { selectedConnectionType = .lineside }) {
                    Label(AppStrings.AssetsExtra.newLinesideConnection, systemImage: "arrow.up")
                }
                Button(action: { selectedConnectionType = .loadside }) {
                    Label(AppStrings.AssetsExtra.newLoadsideConnection, systemImage: "arrow.down")
                }
            } label: {
                Image(systemName: "plus.circle.fill")
                    .font(.title2)
                    .foregroundColor(.purple)
            }
        }
    }
}

// MARK: - Connection Subsection (LINE / LOAD)

private struct ConnectionSubsection: View {
    let title: String
    let items: [NodeEdgesViewModel.EdgeDisplayModel]
    let isMissingSource: Bool
    let onSelect: (EdgeV2) -> Void
    let onDelete: (EdgeV2) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.secondary)
                .tracking(1)

            if isMissingSource {
                RoundedRectangle(cornerRadius: 10)
                    .strokeBorder(style: StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                    .foregroundColor(.red)
                    .frame(height: 52)
                    .overlay(
                        Text(AppStrings.AssetsExtra.missingLabel)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.red)
                    )
            }

            if items.isEmpty && !isMissingSource {
                Text("None")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 8)
            }

            ForEach(items) { item in
                EdgeRow(
                    item: item,
                    onTap: { onSelect(item.edge) },
                    onDelete: { onDelete(item.edge) }
                )
            }
        }
    }
}

// MARK: - Edge Row

struct EdgeRow: View {
    let item: NodeEdgesViewModel.EdgeDisplayModel
    let onTap: () -> Void
    let onDelete: () -> Void

    private var statusColor: Color {
        item.isComplete ? .green : .red
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                NodeTypeIconCircle(
                    style: item.connectedNode?.node_class?.style,
                    size: 32,
                    iconSize: 16,
                    backgroundColor: statusColor.opacity(0.12),
                    iconColor: statusColor
                )

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.title)
                        .font(.body)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)

                    if let subtitle = item.subtitle {
                        Text(subtitle)
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
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive, action: onDelete) {
                Label(AppStrings.Common.delete, systemImage: "trash")
            }
        }
        .contextMenu {
            Button(action: onTap) {
                Label(AppStrings.AssetsExtra.viewDetails, systemImage: "eye")
            }
            Button(role: .destructive, action: onDelete) {
                Label(AppStrings.Common.delete, systemImage: "trash")
            }
        }
    }
}
