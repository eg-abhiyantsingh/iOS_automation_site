//
//  TerminalPicker.swift
//  Egalvanic PZ
//
//  Terminal selection picker for edge creation/editing.
//  Filters terminals based on connection side (source vs target).
//

import SwiftUI
import SwiftData

/// Terminal picker that shows available terminals for a node based on connection side
struct TerminalPicker: View {
    let node: NodeV2?
    let side: TerminalSide
    @Binding var selectedTerminalId: UUID?

    enum TerminalSide {
        case source  // Needs LOAD terminals (power flows out)
        case target  // Needs LINE terminals (power flows in)

        var terminalFilter: String {
            switch self {
            case .source: return "LOAD"
            case .target: return "LINE"
            }
        }

        var label: String {
            switch self {
            case .source: return AppStrings.Supporting.sourceTerminal
            case .target: return AppStrings.Supporting.targetTerminal
            }
        }
    }

    /// Filtered terminals for this side
    private var availableTerminals: [NodeTerminal] {
        guard let node = node else { return [] }
        return node.node_terminals.filter { terminal in
            !terminal.is_deleted && terminal.side == side.terminalFilter
        }.sorted { ($0.label ?? "") < ($1.label ?? "") }
    }

    /// Selected terminal object
    private var selectedTerminal: NodeTerminal? {
        guard let selectedId = selectedTerminalId else { return nil }
        return availableTerminals.first { $0.id == selectedId }
    }

    /// Whether to show the picker (more than one option)
    private var shouldShowPicker: Bool {
        availableTerminals.count > 1
    }

    /// Whether there's exactly one terminal (auto-selected)
    private var hasAutoSelectedTerminal: Bool {
        availableTerminals.count == 1
    }

    var body: some View {
        if availableTerminals.isEmpty {
            // No terminals available - show nothing
            EmptyView()
        } else if hasAutoSelectedTerminal {
            // Single terminal - show label only (auto-selected)
            let terminal = availableTerminals[0]
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
                // Auto-select the single terminal
                if selectedTerminalId == nil {
                    selectedTerminalId = terminal.id
                }
            }
        } else {
            // Multiple terminals - show picker
            VStack(alignment: .leading, spacing: 4) {
                Text(side.label)
                    .font(.caption)
                    .foregroundColor(.secondary)

                Picker(AppStrings.Connections.terminal, selection: $selectedTerminalId) {
                    Text(AppStrings.Connections.selectTerminal)
                        .foregroundColor(.secondary)
                        .tag(nil as UUID?)
                    ForEach(availableTerminals, id: \.id) { terminal in
                        Text(terminal.label ?? AppStrings.Connections.terminal)
                            .tag(terminal.id as UUID?)
                    }
                }
                .pickerStyle(.menu)
                .labelsHidden()
            }
        }
    }
}

// Note: Preview removed as it requires complex model setup
