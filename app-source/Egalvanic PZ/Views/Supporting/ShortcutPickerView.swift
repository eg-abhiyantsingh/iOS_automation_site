//
//  ShortcutPickerView.swift
//  Egalvanic PZ
//
//  Shortcut Picker View - Select suggested shortcut for an asset
//

import SwiftUI
import SwiftData
import UIKit

struct ShortcutPickerView: View {
    @Binding var selectedShortcut: NodeShortcut?
    let nodeClass: NodeClass?
    let nodeSubtype: NodeSubtype?
    let sld: SLDV2

    @Environment(\.modelContext) private var modelContext
    @State private var availableShortcuts: [NodeShortcut] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showSheet = false
    // Track last-loaded inputs to avoid redundant fetches
    @State private var lastLoadedClassId: UUID?
    @State private var lastLoadedSubtypeId: UUID?

    // ZP-2336: PM Plans (formerly Shortcuts) are gated on the ``emp``
    // company feature flag. When disabled, show a locked placeholder so
    // users know the feature exists but can't interact.
    private var hasEmp: Bool {
        AuthService.shared.hasFeature("emp")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(AppStrings.Supporting.suggestedShortcuts)
                .font(.caption)
                .foregroundColor(.secondary)

            if !hasEmp {
                HStack {
                    Image(systemName: "lock.fill")
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                    Text("Not enabled for your company. Contact your admin.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .opacity(0.7)
            } else if isLoading {
                HStack {
                    Image(systemName: "bolt.fill")
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                    Text(AppStrings.Supporting.loadingShortcuts)
                        .foregroundColor(.secondary)
                    Spacer()
                    ProgressView()
                        .scaleEffect(0.8)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
            } else if let error = errorMessage {
                HStack {
                    Image(systemName: "exclamationmark.triangle")
                        .foregroundColor(.orange)
                        .frame(width: 20)
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
            } else if nodeClass == nil {
                HStack {
                    Image(systemName: "bolt.fill")
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                    Text(AppStrings.Supporting.selectAssetClassFirst)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(.systemGray5))
                .cornerRadius(10)
            } else if availableShortcuts.isEmpty {
                HStack {
                    Image(systemName: "bolt.fill")
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                    Text(AppStrings.Supporting.noShortcutsAvailable)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
            } else {
                Button {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    showSheet = true
                } label: {
                    HStack {
                        Image(systemName: "bolt.fill")
                            .foregroundColor(.secondary)
                            .frame(width: 20)

                        Text(selectedShortcut?.name ?? AppStrings.Supporting.selectShortcut)
                            .foregroundColor(selectedShortcut != nil ? .primary : .secondary)
                            .multilineTextAlignment(.leading)

                        Spacer()

                        Image(systemName: "chevron.down")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                }
                .buttonStyle(.plain)
                .sheet(isPresented: $showSheet) {
                    NavigationStack {
                        List {
                            Button {
                                selectedShortcut = nil
                                showSheet = false
                            } label: {
                                HStack {
                                    Text("None")
                                        .foregroundColor(.primary)
                                    Spacer()
                                    if selectedShortcut == nil {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.blue)
                                    }
                                }
                            }

                            ForEach(availableShortcuts, id: \.id) { shortcut in
                                Button {
                                    selectedShortcut = shortcut
                                    showSheet = false
                                } label: {
                                    HStack {
                                        Text(shortcut.name)
                                            .foregroundColor(.primary)
                                        Spacer()
                                        if selectedShortcut?.id == shortcut.id {
                                            Image(systemName: "checkmark")
                                                .foregroundColor(.blue)
                                        }
                                    }
                                }
                            }
                        }
                        .navigationTitle(AppStrings.Supporting.suggestedShortcuts)
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .navigationBarTrailing) {
                                Button(AppStrings.Common.done) { showSheet = false }
                            }
                        }
                    }
                    .presentationDetents([.medium, .large])
                }
            }
        }
        .onChange(of: nodeClass?.id) { _, _ in
            loadShortcuts()
        }
        .onChange(of: nodeSubtype?.id) { _, _ in
            loadShortcuts()
        }
        .onAppear {
            loadShortcuts()
        }
    }

    private func loadShortcuts() {
        let currentClassId = nodeClass?.id
        let currentSubtypeId = nodeSubtype?.id

        // Skip if inputs haven't changed since last load
        if currentClassId == lastLoadedClassId && currentSubtypeId == lastLoadedSubtypeId && !isLoading {
            return
        }

        guard let nodeClass = nodeClass else {
            availableShortcuts = []
            isLoading = false
            lastLoadedClassId = nil
            lastLoadedSubtypeId = nil
            return
        }

        isLoading = true
        errorMessage = nil

        Task { @MainActor in
            do {
                availableShortcuts = try ShortcutService.getFilteredShortcuts(
                    for: nodeClass,
                    nodeSubtype: nodeSubtype,
                    in: modelContext
                )
                isLoading = false
                lastLoadedClassId = currentClassId
                lastLoadedSubtypeId = currentSubtypeId

                // Clear selection if current shortcut is not in filtered list
                if let current = selectedShortcut,
                   !availableShortcuts.contains(where: { $0.id == current.id }) {
                    selectedShortcut = nil
                }
            } catch {
                errorMessage = "Failed to load shortcuts"
                isLoading = false
                AppLogger.log(.error, "Error loading shortcuts: \(error)", category: .ui)
            }
        }
    }
}
