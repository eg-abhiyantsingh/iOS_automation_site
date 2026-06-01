//
//  IssueIRPhotoList.swift
//  SwiftDataTutorial
//
//  EntityLinkedList configuration for Issue IR Photos with linking support
//

import SwiftUI
import SwiftData

// MARK: - IR Photo List Filter
public enum IRPhotoListFilter: Hashable, CaseIterable {
    case showAll
    case showLinked
    case showAvailable
    
    var displayName: String {
        switch self {
        case .showAll: return "All"
        case .showLinked: return "Linked"
        case .showAvailable: return "Available"
        }
    }
}

// MARK: - Issue IR Photo List Configuration
struct IssueIRPhotoList: EntityLinkedListProtocol {
    typealias ParentEntity = Issue
    typealias LinkedEntity = IRPhoto
    typealias FilterType = IRPhotoListFilter
    
    let modelContext: ModelContext
    
    // MARK: - Core Configuration
    var title: String { "IR Photos" }
    var systemIcon: String { "camera.filters" }
    var emptyStateMessage: String { "No IR photos available" }
    var emptyStateIcon: String { "camera.filters" }
    var addButtonLabel: String { "Add IR Photo" }
    var showSearch: Bool { false }
    var showFilters: Bool { false }
    var showAddButton: Bool { false }
    
    // MARK: - Data Access
    func getLinkedEntities(from issue: Issue) -> [IRPhoto] {
        issue.ir_photos
    }
    
    func isDeleted(_ irPhoto: IRPhoto) -> Bool {
        irPhoto.is_deleted
    }
    
    // MARK: - Filtering
    func defaultFilter() -> IRPhotoListFilter {
        .showAll
    }
    
    func filterMenuLabel(for filter: IRPhotoListFilter) -> String {
        filter.displayName
    }
    
    func shouldShow(_ irPhoto: IRPhoto, with filter: IRPhotoListFilter) -> Bool {
        switch filter {
        case .showAll:
            return true
        case .showLinked:
            return irPhoto.issue != nil
        case .showAvailable:
            return irPhoto.issue == nil
        }
    }
    
    // MARK: - Display Configuration
    func displayTitle(for irPhoto: IRPhoto) -> String {
        "IR: \(irPhoto.ir_photo_key)"
    }
    
    func displaySubtitle(for irPhoto: IRPhoto) -> String? {
        if !irPhoto.visual_photo_key.isEmpty {
            return "Visual: \(irPhoto.visual_photo_key)"
        }
        return irPhoto.node.label
    }
    
    func displayIcon(for irPhoto: IRPhoto) -> String? {
        "camera.filters"
    }
    
    func displayIconColor(for irPhoto: IRPhoto) -> Color {
        .orange
    }
    
    func displayBadge(for irPhoto: IRPhoto) -> (text: String, color: Color)? {
        return nil
    }
    
    func displayAccessory(for irPhoto: IRPhoto) -> EntityAccessoryType? {
        .chevron
    }
    
    // MARK: - Sorting
    func sortEntities(_ entities: [IRPhoto]) -> [IRPhoto] {
        entities.sorted {
            // First by photo key
            if $0.ir_photo_key != $1.ir_photo_key {
                return $0.ir_photo_key < $1.ir_photo_key
            }
            // Then by date
            return $0.date_created > $1.date_created
        }
    }
    
    // MARK: - Actions
    func onSelect(_ irPhoto: IRPhoto) {
        // Navigate to IR photo detail view or show full screen
    }
    
    func onDelete(_ irPhoto: IRPhoto) {
        // Unlink the IR photo from the issue
        irPhoto.issue = nil
        try? modelContext.save()
    }
    
    func onAdd() {
        // This would typically show a camera or photo picker for IR photos
    }
    
    // MARK: - Session Info
    func sessionInfo() -> SessionDisplayInfo? {
        // Could show session info if relevant
        nil
    }
    
    // MARK: - Linking Configuration
    func linkingConfiguration() -> EntityLinkingConfiguration<Issue, IRPhoto>? {
        EntityLinkingConfiguration(
            relationship: .oneToMany,
            linkButtonLabel: "Link",
            linkViewTitle: "Select IR Photos",
            searchPrompt: "Search by photo key or node...",
            fetchAvailable: { issue in
                // Fetch all IR photos from the same session and node
                guard let session = issue.session,
                      let node = issue.node else {
                    return []
                }
                
                // Fetch all IR photos and filter in memory due to SwiftData predicate limitations
                let descriptor = FetchDescriptor<IRPhoto>()
                
                do {
                    let allPhotos = try modelContext.fetch(descriptor)
                    // Filter for photos from the same node that aren't deleted
                    return allPhotos.filter { irPhoto in
                        irPhoto.node.id == node.id && !irPhoto.is_deleted
                    }
                } catch {
                    return []
                }
            },
            currentlyLinked: { issue in
                Set(issue.ir_photos.map { $0.id })
            },
            applyLinking: { issue, selectedIds, availablePhotos in
                // Get current linked IDs
                let currentIds = Set(issue.ir_photos.map { $0.id })
                
                // Track photos that need syncing
                var photosToSync: [IRPhoto] = []
                
                // Find photos to unlink
                let toUnlink = currentIds.subtracting(selectedIds)
                for photoId in toUnlink {
                    if let photo = issue.ir_photos.first(where: { $0.id == photoId }) {
                        photo.issue = nil
                        issue.ir_photos.removeAll { $0.id == photoId }
                        photosToSync.append(photo) // Track for sync
                    }
                }
                
                // Find photos to link
                let toLink = selectedIds.subtracting(currentIds)
                for photoId in toLink {
                    if let photo = availablePhotos.first(where: { $0.id == photoId }) {
                        photo.issue = issue
                        if !issue.ir_photos.contains(where: { $0.id == photoId }) {
                            issue.ir_photos.append(photo)
                        }
                        photosToSync.append(photo) // Track for sync
                    }
                }
                
                // Save changes locally first
                do {
                    try modelContext.save()
                } catch {
                    // Handle error silently
                }
            },
            displayInSelector: { irPhoto in
                EntitySelectorDisplay(
                    title: "IR: \(irPhoto.ir_photo_key)",
                    subtitle: irPhoto.visual_photo_key.isEmpty ? 
                        irPhoto.node.label : 
                        "Visual: \(irPhoto.visual_photo_key) • \(irPhoto.node.label)",
                    icon: "camera.filters",
                    iconColor: .orange,
                    badge: irPhoto.date_created.formatted(date: .abbreviated, time: .omitted),
                    isAlreadyLinked: irPhoto.issue != nil
                )
            },
            canLink: { irPhoto in
                // Can always link/unlink photos from this issue
                true
            }
        )
    }
}

// MARK: - Convenience Extension for Issue
extension Issue {
    /// Create an IR Photo linked list view for this issue
    func irPhotoListView(
        modelContext: ModelContext,
        hasChanges: Binding<Bool> = .constant(false),
        onLinkedPhotosChanged: (([IRPhoto]) -> Void)? = nil
    ) -> some View {
        EntityLinkedList(
            configuration: IssueIRPhotoList(modelContext: modelContext),
            parent: self,
            hasChanges: hasChanges,
            onLinkedEntitiesChanged: onLinkedPhotosChanged
        )
    }
}
