//
//  CopyAssetDetailsPickerView.swift
//  Egalvanic PZ
//
//  Single-select node picker for copy asset details
//

import SwiftUI
import SwiftData

struct CopyAssetDetailsPickerView: View {
    @Environment(\.dismiss) private var dismiss

    let sld: SLDV2
    let currentNode: NodeV2
    let nodeClass: NodeClass?
    let direction: CopyDirection
    let onNodeSelected: (NodeV2) -> Void

    @State private var searchText = ""
    @State private var showQRScanner = false
    @State private var scannedQRCode = ""
    @State private var qrErrorMessage: String?

    private var eligibleNodes: [NodeV2] {
        sld.nodes.filter { node in
            guard !node.is_deleted && node.id != currentNode.id else { return false }
            if let nodeClass = nodeClass {
                return node.node_class?.id == nodeClass.id
            }
            return true // No class filter — show all non-deleted nodes
        }
        .sorted { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }
    }

    private var filteredNodes: [NodeV2] {
        guard !searchText.isEmpty else { return eligibleNodes }
        return eligibleNodes.filter { node in
            node.label.localizedCaseInsensitiveContains(searchText) ||
            (node.room?.name.localizedCaseInsensitiveContains(searchText) ?? false) ||
            (node.qr_code?.localizedCaseInsensitiveContains(searchText) ?? false)
        }
    }

    private var titleText: String {
        switch direction {
        case .from: return AppStrings.AssetsExtra.copyDetailsFromTitle
        case .to: return AppStrings.AssetsExtra.copyDetailsToTitle
        }
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                if eligibleNodes.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "doc.on.doc")
                            .font(.system(size: 60))
                            .foregroundColor(.gray.opacity(0.5))
                        Text(AppStrings.AssetsExtra.noSameClassAssets)
                            .font(.headline)
                        Text(nodeClass != nil
                        ? "There are no other \(nodeClass!.name) assets in this SLD to copy \(direction == .from ? "from" : "to")."
                        : "There are no assets in this SLD to copy from.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    // Search bar
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)

                        TextField(AppStrings.AssetsExtra.searchByLabelRoomQR, text: $searchText)
                            .textFieldStyle(PlainTextFieldStyle())

                        if !searchText.isEmpty {
                            Button {
                                searchText = ""
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.secondary)
                            }
                        }

                        Button {
                            showQRScanner = true
                        } label: {
                            Image(systemName: "qrcode.viewfinder")
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(8)
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(8)
                    .padding(.horizontal)
                    .padding(.bottom, 8)

                    // Node list
                    List {
                        ForEach(filteredNodes) { node in
                            Button {
                                onNodeSelected(node)
                                dismiss()
                            } label: {
                                HStack(spacing: 12) {
                                    NodeTypeIconCircle(
                                        style: node.node_class?.style,
                                        size: 32,
                                        iconSize: 16,
                                        backgroundColor: Color.blue.opacity(0.1),
                                        iconColor: .blue
                                    )

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(node.label)
                                            .font(.system(size: 15, weight: .medium))
                                            .foregroundColor(.primary)

                                        if let roomName = node.room?.name {
                                            Text(roomName)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }

                                        if nodeClass == nil, let className = node.node_class?.name {
                                            Text(className)
                                                .font(.caption2)
                                                .foregroundColor(.blue)
                                        }
                                    }

                                    Spacer()

                                    if let qr = node.qr_code, !qr.isEmpty {
                                        Image(systemName: "qrcode")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                }
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                }
            }
            .navigationTitle(titleText)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
            }
            .fullScreenCover(isPresented: $showQRScanner) {
                QRCodeScannerView(scannedCode: $scannedQRCode) { code in
                    // Find matching node by QR code among all SLD nodes
                    let allMatchingNodes = sld.nodes.filter { !$0.is_deleted && $0.qr_code == code }
                    if let nodeClass = nodeClass {
                        // Class filter active — match same class only
                        if let matchedNode = allMatchingNodes.first(where: { $0.id != currentNode.id && $0.node_class?.id == nodeClass.id }) {
                            onNodeSelected(matchedNode)
                            dismiss()
                        } else if allMatchingNodes.contains(where: { $0.node_class?.id != nodeClass.id }) {
                            qrErrorMessage = "Scanned asset is not the same class as this asset."
                        } else {
                            qrErrorMessage = "No asset found with that QR code."
                        }
                    } else {
                        // No class filter — accept any matching non-deleted node
                        if let matchedNode = allMatchingNodes.first(where: { $0.id != currentNode.id }) {
                            onNodeSelected(matchedNode)
                            dismiss()
                        } else {
                            qrErrorMessage = "No asset found with that QR code."
                        }
                    }
                }
            }
            .alert(AppStrings.AssetsExtra.qrScanAlert, isPresented: Binding(
                get: { qrErrorMessage != nil },
                set: { if !$0 { qrErrorMessage = nil } }
            )) {
                Button(AppStrings.Common.ok) { qrErrorMessage = nil }
            } message: {
                Text(qrErrorMessage ?? "")
            }
        }
    }
}
