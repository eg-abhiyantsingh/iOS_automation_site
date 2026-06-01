//
//  NodeV2+Terminals.swift
//  Egalvanic PZ
//
//  Extension for terminal eligibility helpers on NodeV2.
//  Used for edge creation to determine which terminals can be source/target.
//

import Foundation

extension NodeV2 {
    /// Terminals eligible for source connections (LOAD side).
    /// Power flows OUT from LOAD terminals, so they can be edge sources.
    var sourceEligibleTerminals: [NodeTerminal] {
        node_terminals.filter { !$0.is_deleted && $0.side == "LOAD" }
    }

    /// Terminals eligible for target connections (LINE side).
    /// Power flows IN to LINE terminals, so they can be edge targets.
    var targetEligibleTerminals: [NodeTerminal] {
        node_terminals.filter { !$0.is_deleted && $0.side == "LINE" }
    }

    /// Whether this node can be an edge source (has LOAD terminals).
    var canBeEdgeSource: Bool {
        !sourceEligibleTerminals.isEmpty
    }

    /// Whether this node can be an edge target (has LINE terminals).
    var canBeEdgeTarget: Bool {
        !targetEligibleTerminals.isEmpty
    }

    /// Get display label for a terminal (combines label and handle_code).
    func terminalDisplayLabel(_ terminal: NodeTerminal) -> String {
        let label = terminal.label ?? ""
        let handleCode = terminal.handle_code ?? ""
        if !label.isEmpty && !handleCode.isEmpty {
            return "\(label) (\(handleCode))"
        }
        return label.isEmpty ? (handleCode.isEmpty ? "Terminal" : handleCode) : label
    }
}
