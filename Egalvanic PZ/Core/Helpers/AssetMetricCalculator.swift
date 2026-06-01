//
//  AssetMetricCalculator.swift
//  Egalvanic PZ
//
//  Pure functions for calculating asset metrics
//

import Foundation

/// Result of inbound connection analysis for a node hierarchy
struct InboundConnectionStatus {
    let needsSource: Bool        // Whether this node hierarchy requires a source connection
    let hasConnection: Bool      // At least one node in hierarchy has an inbound connection (or doesn't need one)
    let allHaveClasses: Bool     // All present inbound connections have classes assigned
    let completionPercentage: Int // Average completion % across all inbound connections

    /// Analyzes inbound connections for a node and all its descendants
    /// - At least one node (parent or child) must have an inbound connection
    /// - Shows average completion of all present inbound connections
    /// - Nodes with node_class.needs_source == false (e.g., utility, generator) are treated as 100% complete
    static func analyze(
        node: NodeV2,
        allDescendants: [NodeV2],
        edges: [EdgeV2]
    ) -> InboundConnectionStatus {
        let allNodes = [node] + allDescendants

        // Determine if any node in the hierarchy needs a source
        // A node needs a source if needs_source is true or nil (default to needing source)
        let anyNodeNeedsSource = allNodes.contains { n in
            n.node_class?.needs_source != false
        }

        // Separate nodes into those that need a source and those that don't
        var completions: [Int] = []
        var hasAnyConnection = false
        var allEdgesHaveClasses = true

        for n in allNodes {
            // If node_class.needs_source == false, this node is 100% complete for inbound connections
            if n.node_class?.needs_source == false {
                completions.append(100)
                hasAnyConnection = true
                continue
            }

            // Find inbound edges for this node
            let nodeInboundEdges = edges.filter { edge in
                !edge.is_deleted && edge.target == n.id
            }

            if !nodeInboundEdges.isEmpty {
                hasAnyConnection = true

                // Check if all edges have classes
                if !nodeInboundEdges.allSatisfy({ $0.edge_class != nil }) {
                    allEdgesHaveClasses = false
                }

                // Add completion for each inbound edge
                for edge in nodeInboundEdges {
                    completions.append(edge.af_completion)
                }
            } else {
                // Node needs source but has no inbound connection - contributes 0%
                completions.append(0)
            }
        }

        // If no node needs a source, return 100% complete
        if !anyNodeNeedsSource {
            return InboundConnectionStatus(
                needsSource: false,
                hasConnection: true,
                allHaveClasses: true,
                completionPercentage: 100
            )
        }

        // Calculate average completion percentage
        let totalCompletion = completions.reduce(0, +)
        let avgCompletion = completions.isEmpty ? 0 : totalCompletion / completions.count

        return InboundConnectionStatus(
            needsSource: anyNodeNeedsSource,
            hasConnection: hasAnyConnection,
            allHaveClasses: allEdgesHaveClasses,
            completionPercentage: avgCompletion
        )
    }
}

/// Result of composite arc flash analysis for a node hierarchy
struct CompositeArcFlashStatus {
    let allHaveClasses: Bool     // All nodes in hierarchy have classes assigned
    let completionPercentage: Int // Average completion % across all nodes

    /// Analyzes arc flash completion for a node and all its descendants
    /// - Returns the average completion % across the entire hierarchy
    /// - If any node lacks a class, that node contributes 0% to the calculation
    static func analyze(node: NodeV2, allDescendants: [NodeV2]) -> CompositeArcFlashStatus {
        let allNodes = [node] + allDescendants

        // Check if all nodes have classes
        let allHaveClasses = allNodes.allSatisfy { $0.node_class != nil }

        // Get completion for each node (0% if no class)
        let completions = allNodes.map { n -> Int in
            guard n.node_class != nil else { return 0 }
            return n.af_completion
        }

        // Calculate average completion percentage across all nodes
        let totalCompletion = completions.reduce(0, +)
        let avgCompletion = completions.isEmpty ? 0 : totalCompletion / completions.count

        return CompositeArcFlashStatus(
            allHaveClasses: allHaveClasses,
            completionPercentage: avgCompletion
        )
    }
}

/// Pure functions for calculating asset metrics
struct AssetMetricCalculator {

    /// Get detailed inbound connection status including class and completion info
    /// Composite metric: checks parent + all descendants
    static func getInboundConnectionStatus(
        node: NodeV2,
        allDescendants: [NodeV2],
        edges: [EdgeV2]
    ) -> InboundConnectionStatus {
        return InboundConnectionStatus.analyze(node: node, allDescendants: allDescendants, edges: edges)
    }

    /// Get composite arc flash status for a node and all its descendants
    /// Returns worst-case completion across the entire hierarchy
    static func getCompositeArcFlashStatus(
        node: NodeV2,
        allDescendants: [NodeV2]
    ) -> CompositeArcFlashStatus {
        return CompositeArcFlashStatus.analyze(node: node, allDescendants: allDescendants)
    }

    /// Get all descendants of a node recursively
    /// - Parameters:
    ///   - node: The parent node
    ///   - childrenMap: Map of parent ID to child nodes
    /// - Returns: Array of all descendant nodes
    static func getAllDescendants(
        of node: NodeV2,
        using childrenMap: [UUID: [NodeV2]]
    ) -> [NodeV2] {
        var descendants: [NodeV2] = []
        if let children = childrenMap[node.id] {
            descendants.append(contentsOf: children)
            for child in children {
                descendants.append(contentsOf: getAllDescendants(of: child, using: childrenMap))
            }
        }
        return descendants
    }
}
