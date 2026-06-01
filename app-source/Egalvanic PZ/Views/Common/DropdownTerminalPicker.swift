//
//  DropdownTerminalPicker.swift
//  Egalvanic PZ
//
//  Sheet-based picker for terminal selection on edge creation/editing.
//  Auto-selects when only one terminal exists. Hidden when none available.
//

import SwiftUI

/// A button that opens a bottom sheet for terminal selection
struct DropdownTerminalPicker: View {
    let node: NodeV2?
    let side: TerminalPicker.TerminalSide
    @Binding var selectedTerminalId: UUID?

    @State private var isSheetPresented = false

    private var availableTerminals: [NodeTerminal] {
        guard let node = node else { return [] }
        return node.node_terminals.filter { terminal in
            !terminal.is_deleted && terminal.side == side.terminalFilter
        }.sorted { ($0.label ?? "") < ($1.label ?? "") }
    }

    private var selectedTerminalLabel: String? {
        guard let id = selectedTerminalId else { return nil }
        return availableTerminals.first { $0.id == id }?.label
    }

    var body: some View {
        let terminals = availableTerminals

        if terminals.isEmpty {
            EmptyView()
        } else if terminals.count == 1 {
            // Single terminal — show label, auto-select
            let terminal = terminals[0]
            HStack {
                Text(side.label)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Spacer()
                Text(terminal.label ?? AppStrings.Connections.terminal)
                    .font(.caption)
                    .foregroundColor(.primary)
            }
            .onAppear {
                if selectedTerminalId == nil {
                    selectedTerminalId = terminal.id
                }
            }
        } else {
            // Multiple terminals — show sheet picker
            VStack(alignment: .leading, spacing: 4) {
                Text(side.label)
                    .font(.caption)
                    .foregroundColor(.secondary)

                Button(action: { isSheetPresented = true }) {
                    HStack {
                        Text(selectedTerminalLabel ?? AppStrings.Connections.selectTerminal)
                            .foregroundColor(selectedTerminalLabel != nil ? .primary : .secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        Image(systemName: "chevron.right")
                            .foregroundColor(.secondary)
                            .font(.caption)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                }
                .buttonStyle(PlainButtonStyle())
            }
            .sheet(isPresented: $isSheetPresented) {
                TerminalPickerSheet(
                    selectedTerminalId: $selectedTerminalId,
                    terminals: terminals,
                    title: side.label,
                    isPresented: $isSheetPresented
                )
                .presentationDetents([.medium])
            }
        }
    }
}

// MARK: - Terminal Picker Sheet

private struct TerminalPickerSheet: View {
    @Binding var selectedTerminalId: UUID?
    let terminals: [NodeTerminal]
    let title: String
    @Binding var isPresented: Bool

    var body: some View {
        NavigationView {
            List {
                ForEach(terminals, id: \.id) { terminal in
                    Button(action: {
                        selectedTerminalId = terminal.id
                        isPresented = false
                    }) {
                        HStack {
                            Text(terminal.label ?? AppStrings.Connections.terminal)
                                .foregroundColor(.primary)

                            Spacer()

                            if selectedTerminalId == terminal.id {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                            }
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .transaction { $0.animation = nil }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.cancel) {
                        isPresented = false
                    }
                }
            }
        }
    }
}
