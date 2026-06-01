import SwiftUI
import SwiftData

// MARK: - Node List Filter
public enum NodeListFilter: Hashable, CaseIterable {
    case showAll
    case showWithTasks
    case showWithIssues
    case showWithPhotos
}

// MARK: - Task Linked Nodes List Configuration
struct TaskLinkedNodesList: EntityLinkedListProtocol {
    typealias ParentEntity = UserTask
    typealias LinkedEntity = NodeV2
    typealias FilterType = NodeListFilter

    // Dependencies
    let modelContext: ModelContext
    let appState: AppStateManager
    let contextNode: NodeV2?  // Optional context node for limiting available nodes
    let onSelectNode: (NodeV2) -> Void
    let onDeleteNode: (NodeV2) -> Void
    let onAddNode: () -> Void

    // MARK: - Core Configuration
    var title: String { "Linked Assets" }
    var systemIcon: String { "cpu" }
    var emptyStateMessage: String { "No linked assets" }
    var emptyStateIcon: String { "cpu" }
    var addButtonLabel: String { "Link Asset" }
    var showSearch: Bool { false }
    var showFilters: Bool { false }
    var showAddButton: Bool { false }

    // MARK: - Data Access
    func getLinkedEntities(from parent: UserTask) -> [NodeV2] {
        // Only return nodes from the linkedNodes array
        // Legacy node field is deprecated and should not be shown
        return parent.linkedNodes
    }

    func isDeleted(_ entity: NodeV2) -> Bool {
        entity.is_deleted
    }

    // MARK: - Filtering
    func defaultFilter() -> NodeListFilter {
        .showAll
    }

    func filterMenuLabel(for filter: NodeListFilter) -> String {
        switch filter {
        case .showAll:
            return "Show All Assets"
        case .showWithTasks:
            return "Show With Tasks"
        case .showWithIssues:
            return "Show With Issues"
        case .showWithPhotos:
            return "Show With Photos"
        }
    }

    func shouldShow(_ entity: NodeV2, with filter: NodeListFilter) -> Bool {
        switch filter {
        case .showAll:
            return true
        case .showWithTasks:
            return !entity.node_tasks.isEmpty
        case .showWithIssues:
            return !entity.issues.isEmpty
        case .showWithPhotos:
            return !entity.photos.isEmpty
        }
    }

    // MARK: - Display Configuration
    func displayTitle(for entity: NodeV2) -> String {
        entity.label
    }

    func displaySubtitle(for entity: NodeV2) -> String? {
        var parts: [String] = []

        // Add type
        if !entity.type.isEmpty {
            parts.append("Type: \(entity.type)")
        }

        // Add location if available
        if let location = entity.location, !location.isEmpty {
            parts.append("Location: \(location)")
        }

        // Add QR code if available
        if let qrCode = entity.qr_code, !qrCode.isEmpty {
            parts.append("QR: \(qrCode)")
        }

        return parts.isEmpty ? nil : parts.joined(separator: " • ")
    }

    func displayIcon(for entity: NodeV2) -> String? {
        // Use type to determine icon
        switch entity.type.lowercased() {
        case "source":
            return "bolt.circle.fill"
        case "load":
            return "arrow.down.circle"
        case "junction":
            return "circle.grid.3x3"
        case "switch":
            return "switch.2"
        default:
            return "cpu"
        }
    }

    func displayIconColor(for entity: NodeV2) -> Color {
        // Color based on completion status
        if entity.af_isComplete {
            return .green
        } else if entity.af_completion > 50 {
            return .orange
        } else {
            return .gray
        }
    }

    func displayBadge(for entity: NodeV2) -> (text: String, color: Color)? {
        // Show completion percentage or issue count
        if !entity.issues.isEmpty {
            return ("\(entity.issues.count) issues", .red)
        } else if entity.af_completion < 100 {
            return ("\(entity.af_completion)%", .orange)
        }
        return nil
    }

    func displayAccessory(for entity: NodeV2) -> EntityAccessoryType? {
        .chevron
    }

    // MARK: - Sorting
    func sortEntities(_ entities: [NodeV2]) -> [NodeV2] {
        entities.sorted { node1, node2 in
            // Sort by completion status first (incomplete first)
            if node1.af_isComplete != node2.af_isComplete {
                return !node1.af_isComplete
            }

            // Then by location
            if let loc1 = node1.location, let loc2 = node2.location, loc1 != loc2 {
                return loc1 < loc2
            }

            // Finally by label
            return node1.label < node2.label
        }
    }

    // MARK: - Actions
    func onSelect(_ entity: NodeV2) {
        onSelectNode(entity)
    }

    func onDelete(_ entity: NodeV2) {
        onDeleteNode(entity)
    }

    func onAdd() {
        onAddNode()
    }

    // MARK: - Session Info
    func sessionInfo() -> SessionDisplayInfo? {
        // Show session info if there's an active session
        guard let session = appState.activeSession else {
            return nil
        }

        return SessionDisplayInfo(
            title: session.name,
            subtitle: "Active Session",
            warningMessage: nil
        )
    }

    // MARK: - Linking Configuration
    func linkingConfiguration() -> EntityLinkingConfiguration<UserTask, NodeV2>? {
        // Re-enable adding nodes from TaskDetailView
        return EntityLinkingConfiguration(
            relationship: .manyToMany,
            linkButtonLabel: "Link Assets",
            linkViewTitle: "Select Assets to Link",
            searchPrompt: "Search by label, location, or QR code...",
            fetchAvailable: { task in
                // Fetch nodes based on context
                let descriptor = FetchDescriptor<NodeV2>(
                    predicate: #Predicate<NodeV2> { node in
                        !node.is_deleted
                    }
                )

                do {
                    let allNodes = try modelContext.fetch(descriptor)

                    // If we have a context node (opened from asset detail), limit to that node and its children
                    if let contextNode = contextNode {
                        // Get nodes in the same SLD
                        let sldNodes = allNodes.filter { $0.sld?.id == task.sld?.id }
                        // Filter to context node and its children only
                        return sldNodes.filter { node in
                            node.id == contextNode.id || node.parent_id == contextNode.id
                        }
                    }

                    // Otherwise, return all nodes in the task's SLD
                    if let sld = task.sld {
                        return allNodes.filter { $0.sld?.id == sld.id }
                    }

                    return allNodes
                } catch {
                    AppLogger.log(.error, "Failed to fetch nodes: \(error)", category: .task)
                    return []
                }
            },
            currentlyLinked: { task in
                // Get IDs of currently linked nodes from array only
                // Legacy node field is deprecated and should not be included
                return Set(task.linkedNodes.filter { !$0.is_deleted }.map { $0.id })
            },
            applyLinking: { task, selectedIds, availableNodes in
                AppLogger.log(.debug, "[TaskLinkedNodesList] Starting link/unlink operation - Task: \(task.title) (ID: \(task.id))", category: .task)

                // Get current linked IDs from array only
                // Legacy node field is deprecated and should not be included
                let currentIds = Set(task.linkedNodes.filter { !$0.is_deleted }.map { $0.id })

                AppLogger.log(.debug, "Current linked: \(currentIds.count), Selected: \(selectedIds.count)", category: .task)

                // Find nodes to unlink
                let toUnlink = currentIds.subtracting(selectedIds)
                let toLink = selectedIds.subtracting(currentIds)

                if !toUnlink.isEmpty {
                    AppLogger.log(.debug, "Unlinking \(toUnlink.count) nodes", category: .task)
                }

                if !toLink.isEmpty {
                    AppLogger.log(.debug, "Linking \(toLink.count) nodes", category: .task)
                }

                // Track if we need to update the task entity for legacy field changes
                var needsTaskUpdate = false

                // Update local arrays immediately for UI feedback
                for nodeId in toUnlink {
                    // Remove from task's linkedNodes array
                    task.linkedNodes.removeAll { $0.id == nodeId }

                    // Find the node and update bidirectional relationship
                    if let node = availableNodes.first(where: { $0.id == nodeId }) ??
                                  (task.node?.id == nodeId ? task.node : nil) {
                        AppLogger.log(.debug, "Removing node: \(node.label) (ID: \(node.id))", category: .task)
                        // Remove task from node's node_tasks (bidirectional)
                        node.node_tasks.removeAll { $0.id == task.id }
                    }

                    // Clear legacy single node if it's being unlinked
                    if task.node?.id == nodeId {
                        AppLogger.log(.debug, "Clearing legacy task.node reference", category: .task)
                        task.node = nil
                        needsTaskUpdate = true
                    }
                }

                // Add nodes to arrays immediately for UI feedback
                for nodeId in toLink {
                    if let node = availableNodes.first(where: { $0.id == nodeId }) {
                        AppLogger.log(.debug, "Adding node: \(node.label) (ID: \(node.id))", category: .task)

                        // Add to task's linkedNodes if not already there
                        if !task.linkedNodes.contains(where: { $0.id == nodeId }) {
                            task.linkedNodes.append(node)
                        }

                        // Add to node's node_tasks (bidirectional)
                        if !node.node_tasks.contains(where: { $0.id == task.id }) {
                            node.node_tasks.append(task)
                        }

                        // Set as legacy single node if none exists
                        if task.node == nil {
                            task.node = node
                            AppLogger.log(.debug, "Set as legacy task.node reference", category: .task)
                        }
                    }
                }

                // Save local changes
                do {
                    try modelContext.save()
                    AppLogger.log(.debug, "Local arrays updated successfully", category: .task)
                } catch {
                    AppLogger.log(.error, "Failed to save local changes: \(error)", category: .task)
                }

                // Queue sync operations for persistence
                Task {
                    do {
                        // Queue removal operations for soft delete
                        if !toUnlink.isEmpty {
                            try await TaskMappingService.shared.removeTaskNodes(
                                taskId: task.id,
                                nodeIds: Array(toUnlink)
                            )
                            AppLogger.log(.debug, "Queued \(toUnlink.count) nodes for soft delete in database", category: .task)

                            // If we cleared the legacy node field, queue task update
                            if needsTaskUpdate {
                                let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                                await NetworkState.shared.enqueue(op)
                                AppLogger.log(.debug, "Queued task update for legacy node field change", category: .task)
                            }
                        }

                        // Queue addition operations
                        if !toLink.isEmpty {
                            try await TaskMappingService.shared.addTaskNodes(
                                taskId: task.id,
                                nodeIds: Array(toLink)
                            )
                            AppLogger.log(.debug, "Queued \(toLink.count) nodes for linking in database", category: .task)
                        }
                    } catch {
                        AppLogger.log(.error, "Failed to queue task-node mappings: \(error)", category: .task)
                    }
                }

                AppLogger.log(.debug, "Link/unlink summary - Unlinked: \(toUnlink.count), Linked: \(toLink.count), Current total: \(task.linkedNodes.count)", category: .task)
            },
            displayInSelector: { node in
                EntitySelectorDisplay(
                    title: node.label,
                    subtitle: [
                        node.type.isEmpty ? nil : "Type: \(node.type)",
                        node.location.map { "Location: \($0)" },
                        node.qr_code.map { "QR: \($0)" }
                    ].compactMap { $0 }.joined(separator: " • "),
                    icon: {
                        switch node.type.lowercased() {
                        case "source": return "bolt.circle.fill"
                        case "load": return "arrow.down.circle"
                        case "junction": return "circle.grid.3x3"
                        case "switch": return "switch.2"
                        default: return "cpu"
                        }
                    }(),
                    iconColor: node.af_isComplete ? .green : (node.af_completion > 50 ? .orange : .gray),
                    badge: {
                        if !node.issues.isEmpty {
                            return "\(node.issues.count) issues"
                        } else if node.af_completion < 100 {
                            return "\(node.af_completion)%"
                        }
                        return nil
                    }(),
                    isAlreadyLinked: false // Will be handled by the selector
                )
            },
            canLink: { _ in
                // Can always link/unlink any node
                true
            }
        )
    }
}