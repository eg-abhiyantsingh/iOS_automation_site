import SwiftUI
import SwiftData
import Foundation

@MainActor
class ArcFlashCompletionViewModel: ObservableObject {
    // MARK: - Published Properties

    @Published var selectedMetricIndex: Int = 0
    @Published var arcFlashCompletion: (percentage: Double, completedCount: Int, totalCount: Int) = (0.0, 0, 0)
    @Published var sourceTargetCompletion: (percentage: Double, completedCount: Int, totalCount: Int) = (0.0, 0, 0)
    @Published var connectionDetailsCompletion: (percentage: Double, completedCount: Int, totalCount: Int) = (0.0, 0, 0)
    @Published var overallCompletion: (percentage: Double, completedCount: Int, totalCount: Int) = (0.0, 0, 0)
    @Published var completionBreakdown: [(range: String, count: Int, nodes: [NodeV2])] = []
    @Published var sourceTargetBreakdown: [(status: String, count: Int, nodes: [NodeV2])] = []
    @Published var connectionDetailsBreakdown: [(range: String, count: Int, edges: [EdgeV2])] = []
    @Published var isComputed: Bool = false
    @Published var isLoading: Bool = false

    // MARK: - Private Properties

    private let diagram: SLDV2
    private var computationTask: Task<Void, Never>?

    // Cached node label lookup for O(1) access instead of O(n)
    private lazy var nodeIdToLabel: [UUID: String] = {
        Dictionary(uniqueKeysWithValues: diagram.nodes.map { ($0.id, $0.label) })
    }()

    // MARK: - Initialization

    init(diagram: SLDV2) {
        self.diagram = diagram
    }

    // MARK: - Computed Properties

    var selectedMetricData: (percentage: Double, completedCount: Int, totalCount: Int) {
        switch selectedMetricIndex {
        case 0:
            return arcFlashCompletion
        case 1:
            return sourceTargetCompletion
        case 2:
            return connectionDetailsCompletion
        default:
            return (0.0, 0, 0)
        }
    }

    var shouldShowBreakdown: Bool {
        (selectedMetricIndex == 0 && !completionBreakdown.isEmpty) ||
        (selectedMetricIndex == 1 && !sourceTargetBreakdown.isEmpty) ||
        (selectedMetricIndex == 2 && !connectionDetailsBreakdown.isEmpty)
    }

    // MARK: - Public Methods

    /// Compute all metrics asynchronously (called in onAppear)
    func computeAllMetrics() {
        guard !isComputed else {
            AppLogger.log(.debug, "[ArcFlash] Metrics already computed, skipping...", category: .task)
            return
        }

        // Cancel any existing computation task
        computationTask?.cancel()

        // Set loading state immediately
        isLoading = true

        // Compute metrics asynchronously to allow UI to render first
        // Store task so it can be cancelled if view disappears
        computationTask = Task {
            // Small delay to allow loading UI to appear
            try? await Task.sleep(nanoseconds: 50_000_000) // 0.05 seconds

            // Check if cancelled before starting expensive computation
            guard !Task.isCancelled else {
                AppLogger.log(.debug, "[ArcFlash] Computation cancelled before starting", category: .task)
                isLoading = false
                return
            }

            let totalStartTime = Date()
            AppLogger.log(.debug, "[ArcFlash] ========== Starting metric computation ==========", category: .task)

            // Perform computation (must stay on main thread for SwiftData access)
            arcFlashCompletion = computeArcFlashCompletion()
            guard !Task.isCancelled else { isLoading = false; return }

            sourceTargetCompletion = computeSourceTargetCompletion()
            guard !Task.isCancelled else { isLoading = false; return }

            connectionDetailsCompletion = computeConnectionDetailsCompletion()
            guard !Task.isCancelled else { isLoading = false; return }

            overallCompletion = computeOverallCompletion()
            guard !Task.isCancelled else { isLoading = false; return }

            completionBreakdown = computeCompletionBreakdown()
            guard !Task.isCancelled else { isLoading = false; return }

            sourceTargetBreakdown = computeSourceTargetBreakdown()
            guard !Task.isCancelled else { isLoading = false; return }

            connectionDetailsBreakdown = computeConnectionDetailsBreakdown()
            guard !Task.isCancelled else { isLoading = false; return }

            isComputed = true
            isLoading = false

            let totalDuration = Date().timeIntervalSince(totalStartTime)
            AppLogger.log(.debug, "[ArcFlash] ========== All metrics computed in \(String(format: "%.3f", totalDuration))s ==========", category: .task)
        }
    }

    /// Cancel ongoing computation (called when view disappears)
    func cancelComputation() {
        computationTask?.cancel()
        computationTask = nil
        isLoading = false
    }

    /// Get metric data for a specific index
    func getMetricData(for index: Int) -> (percentage: Double, completedCount: Int, totalCount: Int) {
        switch index {
        case 0:
            return arcFlashCompletion
        case 1:
            return sourceTargetCompletion
        case 2:
            return connectionDetailsCompletion
        default:
            return (0.0, 0, 0)
        }
    }

    /// Get node label with O(1) lookup instead of O(n) search
    func getNodeLabel(for nodeId: UUID) -> String {
        nodeIdToLabel[nodeId] ?? "Unknown"
    }

    // MARK: - Private Computation Methods

    /// Computes arc flash completion for Asset Details (Nodes)
    /// Counts total required fields completed vs total required fields across all nodes
    /// Unclassified nodes are penalized as 0% complete (1 required field, 0 completed)
    /// Nodes with classes but no required fields don't contribute to the calculation
    private func computeArcFlashCompletion() -> (percentage: Double, completedCount: Int, totalCount: Int) {
        let startTime = Date()
        AppLogger.log(.debug, "[ArcFlash] Computing arcFlashCompletion...", category: .task)

        let activeNodes = diagram.nodes.filter { !$0.is_deleted }
        AppLogger.log(.debug, "[ArcFlash] Active nodes count: \(activeNodes.count)", category: .task)

        guard !activeNodes.isEmpty else {
            AppLogger.log(.debug, "[ArcFlash] No active nodes, returning 0", category: .task)
            return (0.0, 0, 0)
        }

        var totalRequiredFields = 0
        var totalCompletedFields = 0

        for node in activeNodes {
            if let nodeClass = node.node_class {
                let requiredProperties = nodeClass.definition.filter { $0.af_required }
                totalRequiredFields += requiredProperties.count

                if !requiredProperties.isEmpty {
                    // Build attribute map with multiple keys (matching web app logic)
                    var attributeMap: [String: String] = [:]
                    for attr in node.core_attributes {
                        // Map by node_class_property ID
                        if let ncp = attr.node_class_property {
                            attributeMap[ncp.id.uuidString] = attr.value
                        }
                        // Map by attribute ID
                        attributeMap[attr.id.uuidString] = attr.value
                        // Map by name
                        attributeMap[attr.name] = attr.value
                    }

                    var completedFieldNames: [String] = []
                    var missingFieldNames: [String] = []

                    for property in requiredProperties {
                        let value = attributeMap[property.id.uuidString] ?? attributeMap[property.name]
                        if let value = value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            completedFieldNames.append(property.name)
                        } else {
                            missingFieldNames.append(property.name)
                        }
                    }

                    let completedCount = completedFieldNames.count
                    totalCompletedFields += completedCount
                }
            } else {
                // Unclassified node: penalize as 0% complete (1 required, 0 completed)
                totalRequiredFields += 1
            }
        }

        let percentage = totalRequiredFields > 0
            ? (Double(totalCompletedFields) / Double(totalRequiredFields)) * 100.0
            : 100.0

        let duration = Date().timeIntervalSince(startTime)
        AppLogger.log(.debug, "[ArcFlash] arcFlashCompletion completed in \(String(format: "%.3f", duration))s - \(Int(percentage))% (\(totalCompletedFields)/\(totalRequiredFields))", category: .task)

        return (percentage, totalCompletedFields, totalRequiredFields)
    }

    /// Computes source/target completion with optimized parent-child lookup
    private func computeSourceTargetCompletion() -> (percentage: Double, completedCount: Int, totalCount: Int) {
        let startTime = Date()
        AppLogger.log(.debug, "[ArcFlash] Computing sourceTargetCompletion...", category: .task)

        // Get all active (non-deleted) edges
        let activeEdges = diagram.edges.filter { !$0.is_deleted }
        AppLogger.log(.debug, "[ArcFlash] Active edges count: \(activeEdges.count)", category: .task)

        // Get all node IDs that are targets of at least one active edge
        let targetNodeIds = Set(activeEdges.compactMap { $0.target })
        AppLogger.log(.debug, "[ArcFlash] Target node IDs count: \(targetNodeIds.count)", category: .task)

        // Build parent-to-children lookup map once for efficiency (O(n) instead of O(n²))
        let mapStartTime = Date()
        var parentToChildren: [UUID: [UUID]] = [:]
        for node in diagram.nodes where !node.is_deleted {
            if let parentId = node.parent_id {
                parentToChildren[parentId, default: []].append(node.id)
            }
        }
        let mapDuration = Date().timeIntervalSince(mapStartTime)
        AppLogger.log(.debug, "[ArcFlash] Built parent-to-children map in \(String(format: "%.3f", mapDuration))s", category: .task)

        // Optimized helper function using lookup map
        func hasDescendantWithSource(nodeId: UUID, visited: inout Set<UUID>) -> Bool {
            // Prevent infinite loops in case of circular references
            guard !visited.contains(nodeId) else { return false }
            visited.insert(nodeId)

            guard let children = parentToChildren[nodeId] else { return false }

            for childId in children {
                if targetNodeIds.contains(childId) || hasDescendantWithSource(nodeId: childId, visited: &visited) {
                    return true
                }
            }
            return false
        }

        // Get all active nodes that need a source and are not child nodes
        let nodesNeedingSource = diagram.nodes.filter { node in
            !node.is_deleted &&
            node.node_class?.needs_source == true &&
            node.parent_id == nil
        }

        // If no nodes need sources, return 0% completion (consistent with other metrics)
        guard !nodesNeedingSource.isEmpty else {
            AppLogger.log(.debug, "[ArcFlash] No nodes needing source, returning 0", category: .task)
            return (0.0, 0, 0)
        }
        AppLogger.log(.debug, "[ArcFlash] Nodes needing source: \(nodesNeedingSource.count)", category: .task)

        // Count nodes that have source connections (direct or indirect via child/descendant)
        let checkStartTime = Date()
        let nodesWithSource = nodesNeedingSource.filter { node in
            if targetNodeIds.contains(node.id) {
                return true
            }
            var visited = Set<UUID>()
            return hasDescendantWithSource(nodeId: node.id, visited: &visited)
        }
        let checkDuration = Date().timeIntervalSince(checkStartTime)
        AppLogger.log(.debug, "[ArcFlash] Checked descendant sources in \(String(format: "%.3f", checkDuration))s", category: .task)

        let completedCount = nodesWithSource.count
        let totalCount = nodesNeedingSource.count
        let percentage = totalCount > 0 ? (Double(completedCount) / Double(totalCount)) * 100.0 : 100.0

        let duration = Date().timeIntervalSince(startTime)
        AppLogger.log(.debug, "[ArcFlash] sourceTargetCompletion completed in \(String(format: "%.3f", duration))s - \(Int(percentage))% (\(completedCount)/\(totalCount))", category: .task)

        return (percentage, completedCount, totalCount)
    }

    /// Computes connection details completion for edges
    /// Counts total required fields completed vs total required fields across all edges
    /// Unclassified edges are penalized as 0% complete (1 required field, 0 completed)
    private func computeConnectionDetailsCompletion() -> (percentage: Double, completedCount: Int, totalCount: Int) {
        let startTime = Date()
        AppLogger.log(.debug, "[ArcFlash] Computing connectionDetailsCompletion...", category: .task)

        let activeEdges = diagram.edges.filter { !$0.is_deleted }
        AppLogger.log(.debug, "[ArcFlash] Active edges count: \(activeEdges.count)", category: .task)

        guard !activeEdges.isEmpty else {
            AppLogger.log(.debug, "[ArcFlash] No active edges, returning 0", category: .task)
            return (0.0, 0, 0)
        }

        var totalRequiredFields = 0
        var totalCompletedFields = 0

        for edge in activeEdges {
            if let edgeClass = edge.edge_class {
                let requiredProperties = edgeClass.definition.filter { $0.af_required }
                totalRequiredFields += requiredProperties.count

                if !requiredProperties.isEmpty {
                    let attributeValues = Dictionary(
                        uniqueKeysWithValues: edge.core_attributes.map { ($0.id, $0.value) }
                    )

                    let completedCount = requiredProperties.filter { property in
                        if let value = attributeValues[property.id] {
                            return !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        }
                        return false
                    }.count

                    totalCompletedFields += completedCount
                }
            } else {
                // Unclassified edge: penalize as 0% complete (1 required, 0 completed)
                totalRequiredFields += 1
            }
        }

        let percentage = totalRequiredFields > 0
            ? (Double(totalCompletedFields) / Double(totalRequiredFields)) * 100.0
            : 100.0

        let duration = Date().timeIntervalSince(startTime)
        AppLogger.log(.debug, "[ArcFlash] connectionDetailsCompletion completed in \(String(format: "%.3f", duration))s - \(Int(percentage))% (\(totalCompletedFields)/\(totalRequiredFields))", category: .task)

        return (percentage, totalCompletedFields, totalRequiredFields)
    }

    /// Computes overall completion by aggregating all metrics
    private func computeOverallCompletion() -> (percentage: Double, completedCount: Int, totalCount: Int) {
        let startTime = Date()
        AppLogger.log(.debug, "[ArcFlash] Computing overallCompletion...", category: .task)

        let nodeData = arcFlashCompletion
        let sourceData = sourceTargetCompletion
        let edgeData = connectionDetailsCompletion

        // Calculate weighted average based on total items
        let totalItems = nodeData.totalCount + sourceData.totalCount + edgeData.totalCount
        guard totalItems > 0 else { return (0.0, 0, 0) }

        let weightedSum = (nodeData.percentage * Double(nodeData.totalCount)) +
                         (sourceData.percentage * Double(sourceData.totalCount)) +
                         (edgeData.percentage * Double(edgeData.totalCount))

        let averagePercentage = weightedSum / Double(totalItems)
        let totalCompleted = nodeData.completedCount + sourceData.completedCount + edgeData.completedCount

        let duration = Date().timeIntervalSince(startTime)
        AppLogger.log(.debug, "[ArcFlash] overallCompletion completed in \(String(format: "%.3f", duration))s - \(Int(averagePercentage))% (\(totalCompleted)/\(totalItems))", category: .task)

        return (averagePercentage, totalCompleted, totalItems)
    }

    /// Groups nodes by completion ranges for detailed breakdown
    private func computeCompletionBreakdown() -> [(range: String, count: Int, nodes: [NodeV2])] {
        let startTime = Date()
        AppLogger.log(.debug, "[ArcFlash] Computing completionBreakdown...", category: .task)

        let activeNodes = diagram.nodes.filter { !$0.is_deleted }

        let ranges = [
            (range: "0%", min: 0, max: 0),
            (range: "1-25%", min: 1, max: 25),
            (range: "26-50%", min: 26, max: 50),
            (range: "51-75%", min: 51, max: 75),
            (range: "76-99%", min: 76, max: 99),
            (range: "100%", min: 100, max: 100)
        ]

        let breakdown = ranges.map { range in
            let nodesInRange = activeNodes.filter { node in
                node.af_completion >= range.min && node.af_completion <= range.max
            }.sorted { $0.label < $1.label }

            return (range: range.range, count: nodesInRange.count, nodes: nodesInRange)
        }.filter { $0.count > 0 }

        let duration = Date().timeIntervalSince(startTime)
        AppLogger.log(.debug, "[ArcFlash] completionBreakdown completed in \(String(format: "%.3f", duration))s - \(breakdown.count) groups", category: .task)

        return breakdown
    }

    /// Computes source/target breakdown with optimized lookup
    private func computeSourceTargetBreakdown() -> [(status: String, count: Int, nodes: [NodeV2])] {
        let startTime = Date()
        AppLogger.log(.debug, "[ArcFlash] Computing sourceTargetBreakdown...", category: .task)

        let activeEdges = diagram.edges.filter { !$0.is_deleted }
        let targetNodeIds = Set(activeEdges.compactMap { $0.target })

        // Build parent-to-children lookup map once for efficiency
        var parentToChildren: [UUID: [UUID]] = [:]
        for node in diagram.nodes where !node.is_deleted {
            if let parentId = node.parent_id {
                parentToChildren[parentId, default: []].append(node.id)
            }
        }

        // Optimized helper function using lookup map
        func hasDescendantWithSource(nodeId: UUID, visited: inout Set<UUID>) -> Bool {
            guard !visited.contains(nodeId) else { return false }
            visited.insert(nodeId)

            guard let children = parentToChildren[nodeId] else { return false }

            for childId in children {
                if targetNodeIds.contains(childId) || hasDescendantWithSource(nodeId: childId, visited: &visited) {
                    return true
                }
            }
            return false
        }

        let nodesNeedingSource = diagram.nodes.filter { node in
            !node.is_deleted &&
            node.node_class?.needs_source == true &&
            node.parent_id == nil
        }

        // A node is connected if it has a direct source OR an indirect source via descendant
        let connected = nodesNeedingSource.filter { node in
            if targetNodeIds.contains(node.id) {
                return true
            }
            var visited = Set<UUID>()
            return hasDescendantWithSource(nodeId: node.id, visited: &visited)
        }.sorted { $0.label < $1.label }

        let unconnected = nodesNeedingSource.filter { node in
            if targetNodeIds.contains(node.id) {
                return false
            }
            var visited = Set<UUID>()
            return !hasDescendantWithSource(nodeId: node.id, visited: &visited)
        }.sorted { $0.label < $1.label }

        var breakdown: [(status: String, count: Int, nodes: [NodeV2])] = []

        if !connected.isEmpty {
            breakdown.append(("connected", connected.count, connected))
        }
        if !unconnected.isEmpty {
            breakdown.append(("missing_source", unconnected.count, unconnected))
        }

        let duration = Date().timeIntervalSince(startTime)
        AppLogger.log(.debug, "[ArcFlash] sourceTargetBreakdown completed in \(String(format: "%.3f", duration))s - \(breakdown.count) groups", category: .task)

        return breakdown
    }

    /// Computes connection details breakdown for edges
    private func computeConnectionDetailsBreakdown() -> [(range: String, count: Int, edges: [EdgeV2])] {
        let startTime = Date()
        AppLogger.log(.debug, "[ArcFlash] Computing connectionDetailsBreakdown...", category: .task)

        let activeEdges = diagram.edges.filter { !$0.is_deleted }

        // Build node ID to label lookup map once for efficient O(1) sorting (O(n) instead of O(m log m * n))
        let lookupStartTime = Date()
        let nodeIdToLabel: [UUID: String] = Dictionary(
            uniqueKeysWithValues: diagram.nodes.map { ($0.id, $0.label) }
        )
        let lookupDuration = Date().timeIntervalSince(lookupStartTime)
        AppLogger.log(.debug, "[ArcFlash] Built node label lookup map in \(String(format: "%.3f", lookupDuration))s", category: .task)

        let ranges = [
            (range: "0%", min: 0, max: 0),
            (range: "1-25%", min: 1, max: 25),
            (range: "26-50%", min: 26, max: 50),
            (range: "51-75%", min: 51, max: 75),
            (range: "76-99%", min: 76, max: 99),
            (range: "100%", min: 100, max: 100)
        ]

        let breakdown = ranges.map { range in
            let edgesInRange = activeEdges.filter { edge in
                edge.af_completion >= range.min && edge.af_completion <= range.max
            }.sorted { edge1, edge2 in
                // Sort by source node label, then target node label using O(1) lookup
                let sourceA = edge1.source.flatMap { nodeIdToLabel[$0] } ?? ""
                let sourceB = edge2.source.flatMap { nodeIdToLabel[$0] } ?? ""
                if sourceA != sourceB {
                    return sourceA < sourceB
                }
                let targetA = edge1.target.flatMap { nodeIdToLabel[$0] } ?? ""
                let targetB = edge2.target.flatMap { nodeIdToLabel[$0] } ?? ""
                return targetA < targetB
            }

            return (range: range.range, count: edgesInRange.count, edges: edgesInRange)
        }.filter { $0.count > 0 }

        let duration = Date().timeIntervalSince(startTime)
        AppLogger.log(.debug, "[ArcFlash] connectionDetailsBreakdown completed in \(String(format: "%.3f", duration))s - \(breakdown.count) groups", category: .task)

        return breakdown
    }
}
