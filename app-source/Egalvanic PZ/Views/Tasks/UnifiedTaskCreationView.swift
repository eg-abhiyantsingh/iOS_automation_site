import SwiftUI
import SwiftData

struct UnifiedTaskCreationView: View {
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var networkState: NetworkState

    // Optional fixed context (when creating from node detail)
    let fixedNode: NodeV2?
    let fixedSLD: SLDV2?
    let availableNodesForForms: [NodeV2]?

    // Callback when complete
    let onComplete: ((UserTask?) -> Void)?

    init(
        fixedNode: NodeV2? = nil,
        fixedSLD: SLDV2? = nil,
        availableNodesForForms: [NodeV2]? = nil,
        onComplete: ((UserTask?) -> Void)? = nil
    ) {
        self.fixedNode = fixedNode
        self.fixedSLD = fixedSLD
        self.availableNodesForForms = availableNodesForForms
        self.onComplete = onComplete
    }

    var body: some View {
        EntityCreationView(
            configuration: TaskCreationConfiguration(
                appState: appState,
                networkState: networkState,
                fixedNode: fixedNode,
                fixedSLD: fixedSLD,
                availableNodesForForms: availableNodesForForms,
                onComplete: { task in
                    onComplete?(task)
                }
            )
        )
        .environmentObject(appState)
        .environmentObject(networkState)
    }
}

// MARK: - Convenience Views for Different Contexts

// View for creating task from Tasks list (no fixed node)
struct UnifiedTaskCreationFromListView: View {
    @EnvironmentObject var appState: AppStateManager

    @Query private var allSLDs: [SLDV2]
    @Query(filter: #Predicate<NodeV2> { !$0.is_deleted }) private var allNodes: [NodeV2]

    let onTaskCreated: ((UserTask) -> Void)?

    // Compute available nodes for forms - all nodes in the active SLD
    private var availableNodesForForms: [NodeV2] {
        if let activeSLD = allSLDs.first(where: { $0.id == appState.activeSLDId }) {
            // Return all nodes belonging to the active SLD
            return allNodes.filter { $0.sld?.id == activeSLD.id }
        }
        return []
    }

    var body: some View {
        UnifiedTaskCreationView(
            fixedNode: nil,
            fixedSLD: allSLDs.first { $0.id == appState.activeSLDId },
            availableNodesForForms: availableNodesForForms,  // Pass available nodes
            onComplete: { task in
                if let task = task {
                    onTaskCreated?(task)
                }
            }
        )
    }
}

// View for creating task from node detail (fixed node)
struct UnifiedTaskCreationFromNodeView: View {
    @Query(filter: #Predicate<NodeV2> { !$0.is_deleted }) private var allNodes: [NodeV2]

    let node: NodeV2
    let sld: SLDV2
    let onTaskCreated: ((UserTask) -> Void)?

    // Compute available nodes here where we have access to @Query
    private var availableNodesForForms: [NodeV2] {
        // Filter by SLD first
        let sldNodes = allNodes.filter { $0.sld?.id == sld.id }
        // Then get node and its children
        return sldNodes.filter { $0.id == node.id || $0.parent_id == node.id }
    }

    var body: some View {
        UnifiedTaskCreationView(
            fixedNode: node,
            fixedSLD: sld,
            availableNodesForForms: availableNodesForForms,
            onComplete: { task in
                if let task = task {
                    onTaskCreated?(task)
                }
            }
        )
    }
}

// MARK: - Preview Provider
//#if DEBUG
//struct UnifiedTaskCreationView_Previews: PreviewProvider {
//    static var previews: some View {
//        let container = try! ModelContainer(
//            for: UserTask.self, NodeV2.self, SLDV2.self, UserTaskForm.self,
//            configurations: ModelConfiguration(isStoredInMemoryOnly: true)
//        )
//        
//        // Create sample data
//        let sld = SLDV2(id: UUID(), name: "Test SLD", is_deleted: false)
//        let node = NodeV2(id: UUID(), label: "Test Asset", sld: sld)
//        let form = UserTaskForm(
//            id: UUID(),
//            title: "Inspection Form",
//            description: "Standard inspection checklist",
//            fields: []
//        )
//        
//        container.mainContext.insert(sld)
//        container.mainContext.insert(node)
//        container.mainContext.insert(form)
//        
//        return Group {
//            // Preview without fixed node
//            NavigationView {
//                UnifiedTaskCreationView()
//            }
//            .previewDisplayName("Flexible Node")
//            
//            // Preview with fixed node
//            NavigationView {
//                UnifiedTaskCreationView(
//                    fixedNode: node,
//                    fixedSLD: sld
//                )
//            }
//            .previewDisplayName("Fixed Node")
//        }
//        .modelContainer(container)
//        .environmentObject(AppStateManager())
//        .environmentObject(NetworkState())
//    }
//}
//#endif
