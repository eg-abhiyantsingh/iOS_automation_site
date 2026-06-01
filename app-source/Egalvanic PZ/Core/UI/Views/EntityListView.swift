import SwiftUI
import SwiftData

struct EntityListView<Config: EntityListConfiguration>: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    
    let configuration: Config
    let entities: [Config.Entity]
    
    // View State
    @State private var searchText = ""
    @State private var selectedFilter: Config.FilterType
    @State private var selectedSort: SortOption<Config.Entity>
    @State private var showingCreateView = false

    // Scroll position preservation
    @State private var scrolledEntityID: UUID?
    
    init(configuration: Config, entities: [Config.Entity]) {
        self.configuration = configuration
        self.entities = entities
        self._selectedFilter = State(initialValue: configuration.defaultFilter())
        self._selectedSort = State(initialValue: configuration.defaultSort())
    }
    
    // Computed filtered and sorted entities
    private var processedEntities: [Config.Entity] {
        let filtered = entities
            .filter { !$0.isDeleted }
            .filter { entity in
                // Apply filter
                configuration.matches(entity: entity, filter: selectedFilter)
            }
            .filter { entity in
                // Apply search
                if searchText.isEmpty { return true }
                return configuration.searchText(for: entity)
                    .localizedCaseInsensitiveContains(searchText)
            }
        
        // Apply sort
        return filtered.sorted(by: selectedSort.comparator)
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Filter chips
                if configuration.availableFilters().count > 1 {
                    filterChipsView
                        .padding(.horizontal)
                        .padding(.vertical, 12)
                        .background(Color(UIColor.systemBackground))
                }
                
                Divider()
                
                // Main content
                if processedEntities.isEmpty {
                    emptyStateView
                } else {
                    listContent
                }
            }
            .navigationTitle(configuration.pluralEntityName)
            .navigationBarTitleDisplayMode(.large)
            .searchable(text: $searchText, prompt: AppStrings.Forms.searchEntities(configuration.pluralEntityName.lowercased()))
            .toolbar {
                toolbarContent
            }
        }
        .sheet(isPresented: $showingCreateView) {
            // This will call the configuration's onCreate method
            // which should present the appropriate creation view
            Color.clear.onAppear {
                configuration.onCreate()
                showingCreateView = false
            }
        }
    }
    
    // MARK: - View Components
    
    private var filterChipsView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(configuration.availableFilters(), id: \.self) { filter in
                    FilterChipView(
                        filter: filter,
                        count: configuration.filterCount(for: filter, in: entities.filter { !$0.isDeleted }),
                        isSelected: selectedFilter == filter,
                        configuration: configuration,
                        action: { selectedFilter = filter }
                    )
                }
            }
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Image(systemName: configuration.emptyStateIcon)
                .font(.system(size: 60))
                .foregroundColor(.gray.opacity(0.5))
            
            Text(AppStrings.Forms.noEntitiesFound(configuration.pluralEntityName))
                .font(.title3)
                .fontWeight(.medium)
                .foregroundColor(.secondary)
            
            Text(configuration.emptyStateMessage)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            if selectedFilter != configuration.defaultFilter() || !searchText.isEmpty {
                Button(AppStrings.Forms.clearFilters) {
                    selectedFilter = configuration.defaultFilter()
                    searchText = ""
                }
                .buttonStyle(.bordered)
                .padding(.top)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
    
    private var listContent: some View {
        List {
            if let groupBy = selectedSort.groupBy {
                let grouped = Dictionary(grouping: processedEntities) { entity in
                    groupBy(entity) ?? selectedSort.noGroupLabel
                }
                let sortedKeys = grouped.keys.sorted { key1, key2 in
                    if key1 == selectedSort.noGroupLabel { return false }
                    if key2 == selectedSort.noGroupLabel { return true }
                    return key1.localizedCaseInsensitiveCompare(key2) == .orderedAscending
                }
                ForEach(sortedKeys, id: \.self) { key in
                    Section {
                        ForEach(grouped[key] ?? [], id: \.id) { entity in
                            entityRow(for: entity)
                        }
                    } header: {
                        GroupSectionHeader(
                            label: key,
                            icon: selectedSort.groupIcon ?? "folder"
                        )
                    }
                }
            } else {
                ForEach(processedEntities, id: \.id) { entity in
                    entityRow(for: entity)
                }
            }
        }
        .scrollPosition(id: $scrolledEntityID)
        .listStyle(PlainListStyle())
        .onChange(of: selectedFilter) { _, _ in
            scrolledEntityID = nil
        }
    }

    @ViewBuilder
    private func entityRow(for entity: Config.Entity) -> some View {
        EntityRowView(entity: entity, configuration: configuration)
            .contentShape(Rectangle())
            .onTapGesture {
                configuration.onTap(entity: entity)
            }
            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                if let actions = configuration.swipeActions(for: entity) {
                    ForEach(actions.indices, id: \.self) { index in
                        let action = actions[index]
                        Button(role: action.role) {
                            action.action(entity)
                        } label: {
                            Label(action.title, systemImage: action.icon)
                        }
                        .tint(action.color)
                    }
                }
            }
            .contextMenu {
                if let actions = configuration.contextMenuActions(for: entity), !actions.isEmpty {
                    ForEach(actions.indices, id: \.self) { index in
                        let action = actions[index]
                        Button(role: action.role) {
                            action.action(entity)
                        } label: {
                            Label(action.title, systemImage: action.icon)
                        }
                    }
                }
            }
    }
    
    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            Button(AppStrings.Common.done) {
                dismiss()
            }
        }
        
        ToolbarItemGroup(placement: .navigationBarTrailing) {
            // Sort menu
            if configuration.sortOptions().count > 1 {
                Menu {
                    ForEach(configuration.sortOptions()) { option in
                        Button {
                            selectedSort = option
                        } label: {
                            Label(option.title, systemImage: option.icon)
                        }
                    }
                } label: {
                    Image(systemName: "arrow.up.arrow.down")
                }
            }
            
            // Create button
            Button {
                configuration.onCreate()
            } label: {
                Image(systemName: "plus")
            }
        }
    }
}

// MARK: - Filter Chip View
private struct FilterChipView<Config: EntityListConfiguration>: View {
    let filter: Config.FilterType
    let count: Int
    let isSelected: Bool
    let configuration: Config
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if let displayable = filter as? FilterChipDisplayable,
                   let icon = displayable.icon {
                    Image(systemName: icon)
                        .font(.caption)
                }
                
                let displayText = (filter as? FilterChipDisplayable)?.displayName ?? filter.description
                Text(displayText)
                    .font(.subheadline)
                    .fontWeight(isSelected ? .semibold : .regular)
                
                if count > 0 {
                    Text("\(count)")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(isSelected ? .white : .secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(
                            Capsule()
                                .fill(isSelected ? Color.white.opacity(0.25) : Color.gray.opacity(0.2))
                        )
                }
            }
            .foregroundColor(isSelected ? .white : .primary)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(isSelected ? chipColor : Color(UIColor.secondarySystemBackground))
            )
        }
    }
    
    private var chipColor: Color {
        if let displayable = filter as? FilterChipDisplayable {
            return displayable.color
        }
        return .blue
    }
}

// MARK: - Entity Row View
private struct EntityRowView<Config: EntityListConfiguration>: View {
    let entity: Config.Entity
    let configuration: Config
    
    var body: some View {
        HStack(spacing: 12) {
            // Icon
            Image(systemName: configuration.icon(for: entity))
                .foregroundColor(configuration.iconColor(for: entity))
                .font(.title3)
                .frame(width: 24)
            
            // Content
            VStack(alignment: .leading, spacing: 4) {
                // Title row with status badge
                HStack(alignment: .top) {
                    Text(configuration.title(for: entity))
                        .font(.headline)
                        .lineLimit(configuration.titleLineLimit)

                    Spacer()

                    if let badge = configuration.statusBadge(for: entity) {
                        Text(badge.text)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(badge.color)
                            .cornerRadius(4)
                            .fixedSize()
                    }
                }
                
                // Subtitle — wrap as needed. Tasks use this for the
                // full Building · Floor · Room path which can run
                // long; capping at 2 lines truncates mid-room name.
                if let subtitle = configuration.subtitle(for: entity) {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                
                // Metadata items
                let metadata = configuration.metadataItems(for: entity)
                if !metadata.isEmpty {
                    HStack(spacing: 12) {
                        ForEach(metadata.indices, id: \.self) { index in
                            let item = metadata[index]
                            HStack(spacing: 3) {
                                Image(systemName: item.icon)
                                Text(item.text)
                                    .lineLimit(1)
                                    .truncationMode(.tail)
                            }
                            .font(.caption)
                            .foregroundColor(item.color)
                            // Flexible chips (e.g. work-order name) expand into
                            // the remaining width and truncate at the edge;
                            // fixed chips stay at their intrinsic size.
                            .fixedSize(horizontal: !item.flexible, vertical: false)
                            .frame(maxWidth: item.flexible ? .infinity : nil, alignment: .leading)
                        }
                    }
                }

                // Footnote (e.g. "Due: 5/3/26"). Sits on its own line
                // beneath the metadata row so the labeled-value reads
                // naturally without competing for horizontal space.
                if let foot = configuration.footnote(for: entity) {
                    Text(foot.text)
                        .font(.caption)
                        .foregroundColor(foot.color)
                }
            }
            
            // Chevron
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.gray.opacity(0.3))
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Group Section Header
struct GroupSectionHeader: View {
    let label: String
    let icon: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.subheadline)
                .foregroundColor(.accentColor)
            Text(label)
                .font(.subheadline)
                .fontWeight(.bold)
                .foregroundColor(.primary)
                .lineLimit(1)
        }
    }
}