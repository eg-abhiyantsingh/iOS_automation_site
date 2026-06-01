//
//  ShortcutService.swift
//  Egalvanic PZ
//
//  Service for retrieving and filtering node shortcuts
//

import Foundation
import SwiftData

class ShortcutService {
    /// Get shortcuts filtered by node class and optional subtype
    ///
    /// Filtering Rules:
    /// 1. Must match node_class_id
    /// 2. If nodeSubtype is provided:
    ///    - ONLY include shortcuts where node_subtype_id exactly matches nodeSubtype.id
    /// 3. If nodeSubtype is nil:
    ///    - Only include shortcuts where node_subtype_id is nil
    /// 4. Exclude deleted shortcuts
    ///
    /// - Parameters:
    ///   - nodeClass: The required node class to filter by
    ///   - nodeSubtype: Optional node subtype for additional filtering
    ///   - modelContext: SwiftData context for querying
    /// - Returns: Array of matching shortcuts, sorted by name
    @MainActor
    static func getFilteredShortcuts(
        for nodeClass: NodeClass,
        nodeSubtype: NodeSubtype?,
        in modelContext: ModelContext
    ) throws -> [NodeShortcut] {
        // Fetch all shortcuts for the node class
        let classId = nodeClass.id
        let descriptor = FetchDescriptor<NodeShortcut>(
            predicate: #Predicate<NodeShortcut> { shortcut in
                shortcut.node_class_id == classId && !shortcut.is_deleted
            }
        )

        let allShortcuts = try modelContext.fetch(descriptor)

        // Manual filtering for exact subtype matching
        let filtered = allShortcuts.filter { shortcut in
            if let nodeSubtype = nodeSubtype {
                // Case: User selected a subtype
                // Only include shortcuts that exactly match this subtype
                return shortcut.node_subtype_id == nodeSubtype.id //|| shortcut.node_subtype_id == nil
            } else {
                // Case: User selected no subtype
                // Only include shortcuts with no subtype
                return shortcut.node_subtype_id == nil
            }
        }

        return filtered.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    /// Fetches a shortcut by ID
    ///
    /// - Parameters:
    ///   - id: The shortcut ID to fetch
    ///   - modelContext: SwiftData context for querying
    /// - Returns: The shortcut if found, nil otherwise
    @MainActor
    static func getShortcut(
        by id: UUID,
        in modelContext: ModelContext
    ) throws -> NodeShortcut? {
        let descriptor = FetchDescriptor<NodeShortcut>(
            predicate: #Predicate<NodeShortcut> { $0.id == id }
        )
        return try modelContext.fetch(descriptor).first
    }
}
