//
//  EdgeService.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/8/25.
//

import Foundation
import SwiftData

class EdgeService {
    
    @MainActor
    static func createEdge(
        edge: EdgeV2,
        diagram: SLDV2,
        networkState: NetworkState,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) async {
        guard !diagram.edges.contains(where: { $0.id == edge.id }) else {
            onCompletion?(false, "Edge already exists")
            return
        }
        
        edge.sld = diagram
        edge.needsSync = true
        edge.lastSyncedAt = nil
        modelContext.insert(edge)
        for attr in edge.core_attributes {
            modelContext.insert(attr)
        }

        do {
            try modelContext.save()
            AppLogger.log(.info, "Created edge \(edge.id)", category: .node)

            // Update the WebView graph to show the new edge immediately
            WebViewBridge.updateGraphFromSLD(diagram.id, in: modelContext, animated: true)
            AppLogger.log(.debug, "WebView graph updated with new edge", category: .node)

            if networkState.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.createEdge(edge: edge)
                        await MainActor.run {
                            edge.needsSync = false
                            edge.lastSyncedAt = Date()
                            try? modelContext.save()
                            onCompletion?(true, nil)
                        }
                    } catch {
                        let op = SyncOp(target: .edge, operation: .create, edge: edge)
                        networkState.enqueue(op)
                        onCompletion?(true, "Server sync failed; Edge enqueued for sync")
                    }
                }
            } else {
                let op = SyncOp(target: .edge, operation: .create, edge: edge)
                networkState.enqueue(op)
                onCompletion?(true, nil)
            }
        } catch {
            AppLogger.log(.error, "Failed to save edge to local store: \(error)", category: .node)
            onCompletion?(false, "Failed to save edge to local store")
        }
    }

    @MainActor
    static func deleteEdges(
        edgeIds: Set<UUID>,
        diagram: SLDV2,
        networkState: NetworkState,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        let edgesToDelete = diagram.edges.filter { edgeIds.contains($0.id) && !$0.is_deleted }
        
        guard !edgesToDelete.isEmpty else {
            onCompletion?(false, "No edges to delete")
            return
        }
        
        // Mark nodes as deleted (soft delete only)
        for edge in edgesToDelete {
            edge.is_deleted = true
            edge.needsSync = true
            edge.lastModifiedAt = Date()
        }
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Marked \(edgesToDelete.count) edges as deleted", category: .node)
            
            // Update the graph immediately (toDTO already filters out is_deleted nodes)
            let dto = diagram.toDTO()
            WebViewBridge.updateGraph(with: dto, animated: true)
            
            // Handle server sync
            if networkState.canDirectSync {
                // Sync deletions to server asynchronously
                Task {
                    var failedCount = 0
                    
                    for edge in edgesToDelete {
                        do {
                            _ = try await APIClient.shared.updateEdge(edge)
                            AppLogger.log(.info, "Edge \(edge.id) deletion synced to server", category: .node)
                            
                            await MainActor.run {
                                edge.lastSyncedAt = Date()
                                edge.needsSync = false
                                try? modelContext.save()
                            }
                        } catch {
                            AppLogger.log(.error, "Failed to sync edge deletion to server: \(error)", category: .node)
                            failedCount += 1
                            
                            await MainActor.run {
                                let op = SyncOp(
                                    target: .edge,
                                    operation: .update,
                                    edge: edge
                                )
                                networkState.enqueue(op)
                            }
                        }
                    }
                    
                    await MainActor.run {
                        if failedCount == 0 {
                            onCompletion?(true, nil)
                        } else {
                            onCompletion?(true, "\(failedCount) deletion(s) will sync when connection improves")
                        }
                    }
                }
            } else {
                // Offline mode - queue for sync
                for edge in edgesToDelete {
                    let op = SyncOp(
                        target: .edge,
                        operation: .update,
                        edge: edge
                    )
                    networkState.enqueue(op)
                    AppLogger.log(.debug, "Delete of edge \(edge.id) queued for sync when online", category: .node)
                }
                onCompletion?(true, "Edges deleted locally. Will sync when online.")
            }
            
        } catch {
            AppLogger.log(.error, "Failed to save deletions: \(error)", category: .node)
            onCompletion?(false, "Failed to delete nodes: \(error.localizedDescription)")
        }
    }
    
    @MainActor
    static func updateEdge(
        _ edge: EdgeV2,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        do {
            try modelContext.save()
            AppLogger.log(.info, "Edge updated locally", category: .node)

            // Update the WebView graph to show the updated edge
            if let sldId = edge.sld?.id {
                WebViewBridge.updateGraphFromSLD(sldId, in: modelContext, animated: true)
                AppLogger.log(.debug, "WebView graph updated with edge changes", category: .node)
            }

            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.updateEdge(edge)
                        AppLogger.log(.info, "Edge update synced to server", category: .node)
                        await MainActor.run {
                            edge.needsSync = false
                            edge.lastSyncedAt = Date()
                            try? modelContext.save()
                            onCompletion?(true, nil)
                        }
                    } catch {
                        AppLogger.log(.notice, "Failed to sync edge update: \(error)", category: .node)
                        let op = SyncOp(target: .edge, operation: .update, edge: edge)
                        await MainActor.run {
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, "Edge updated locally, will sync when online")
                        }
                    }
                }
            } else {
                let op = SyncOp(target: .edge, operation: .update, edge: edge)
                NetworkState.shared.enqueue(op)
                onCompletion?(true, "Edge updated locally, will sync when online")
            }
        } catch {
            AppLogger.log(.error, "Failed to update edge: \(error)", category: .node)
            onCompletion?(false, error.localizedDescription)
        }
    }
}
