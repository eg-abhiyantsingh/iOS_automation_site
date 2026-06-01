//
//  QuickInventoryView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/2/25.
//

import SwiftUI
import SwiftData

struct QuickInventoryView: View {
    let sld: SLDV2
    let onComplete: () -> Void
    
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var sldService: SLDService
    
    // Fetch all available node classes
    @Query(sort: \NodeClass.name) private var nodeClasses: [NodeClass]
    
    // Fetch all nodes to extract existing locations
    @Query private var allNodes: [NodeV2]
    
    // State for mode selection
    @State private var inventoryMode: InventoryMode = .simple
    
    // Simple mode state
    @State private var quantities: [UUID: Int] = [:]
    
    // Location mode state
    @State private var locations: [Location] = []
    @State private var showAddLocation = false
    @State private var newLocationName = ""
    @State private var editingLocation: Location?
    
    // Creation state
    @State private var isCreating = false
    @State private var creationError: Error?
    @State private var showSuccessAlert = false
    @State private var createdCount = 0
    @State private var showLimitExceededAlert = false
    
    // Asset limit
    private let maxAssetLimit = 500
    
    // Computed property for existing location names
    private var existingLocationNames: [String] {
        let locationSet = Set(allNodes.compactMap { $0.location }.filter { !$0.isEmpty })
        return Array(locationSet).sorted()
    }
    
    // Computed property for filtered node classes
    private var filteredNodeClasses: [NodeClass] {
        nodeClasses.filter { !$0.is_deleted }
    }
    
    enum InventoryMode {
        case simple
        case byLocation
        
        var title: String {
            switch self {
            case .simple: return "Simple"
            case .byLocation: return "By Location"
            }
        }
        
        var icon: String {
            switch self {
            case .simple: return "square.grid.2x2"
            case .byLocation: return "location"
            }
        }
    }
    
    struct Location: Identifiable {
        let id = UUID()
        var name: String
        var quantities: [UUID: Int] = [:]
        
        var totalItems: Int {
            quantities.values.reduce(0, +)
        }
    }
    
    private var totalNodesToCreate: Int {
        switch inventoryMode {
        case .simple:
            return quantities.values.reduce(0, +)
        case .byLocation:
            return locations.reduce(0) { total, location in
                total + location.quantities.values.reduce(0, +)
            }
        }
    }
    
    private var hasSelection: Bool {
        totalNodesToCreate > 0
    }
    
    private var isOverLimit: Bool {
        totalNodesToCreate > maxAssetLimit
    }
    
    var body: some View {
        NavigationView {
            Form {
                Section {
                    Picker(AppStrings.QuickInventory.inventoryMode, selection: $inventoryMode) {
                        Label(InventoryMode.simple.title, systemImage: InventoryMode.simple.icon)
                            .tag(InventoryMode.simple)
                        Label(InventoryMode.byLocation.title, systemImage: InventoryMode.byLocation.icon)
                            .tag(InventoryMode.byLocation)
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    .listRowBackground(Color.clear)
                }
                
                switch inventoryMode {
                case .simple:
                    simpleInventorySection
                case .byLocation:
                    locationInventorySection
                }
                
                if hasSelection {
                    summarySection
                }
            }
            .navigationTitle(AppStrings.QuickInventory.quickInventory)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.create) {
                        if isOverLimit {
                            showLimitExceededAlert = true
                        } else {
                            createNodes()
                        }
                    }
                    .fontWeight(.semibold)
                    .disabled(!hasSelection || isCreating)
                }
            }
            .overlay {
                if isCreating {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                    ProgressView(AppStrings.QuickInventory.creatingProgress(createdCount, totalNodesToCreate))
                        .padding()
                        .background(Color(UIColor.systemBackground))
                        .cornerRadius(10)
                        .shadow(radius: 5)
                }
            }
            .alert(AppStrings.QuickInventory.creationFailed, isPresented: Binding(
                get: { creationError != nil },
                set: { if !$0 { creationError = nil } }
            )) {
                Button(AppStrings.Common.ok) { creationError = nil }
            } message: {
                Text(creationError?.localizedDescription ?? "Unknown error")
            }
            .alert(AppStrings.QuickInventory.success, isPresented: $showSuccessAlert) {
                Button(AppStrings.Common.ok) {
                    dismiss()
                    onComplete()
                }
            } message: {
                Text(AppStrings.QuickInventory.successfullyCreatedAssets(createdCount))
            }
            .alert(AppStrings.QuickInventory.assetLimitExceeded, isPresented: $showLimitExceededAlert) {
                Button(AppStrings.Common.ok) { }
            } message: {
                Text(AppStrings.QuickInventory.assetLimitExceededMessage(maxAssetLimit, totalNodesToCreate))
            }
            .sheet(isPresented: $showAddLocation) {
                addLocationSheet
            }
            .sheet(item: $editingLocation) { location in
                editLocationSheet(location: location)
            }
        }
    }
    
    // MARK: - Simple Mode View
    @ViewBuilder
    private var simpleInventorySection: some View {
        Section {
            Text(AppStrings.QuickInventory.selectQuantityDescription)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        
        Section(AppStrings.QuickInventory.assetTypes) {
            ForEach(filteredNodeClasses) { nodeClass in
                HStack {
                    VStack(alignment: .leading) {
                        Text(nodeClass.name)
                            .font(.headline)
                        if !nodeClass.style.isEmpty {
                            Text(nodeClass.style)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    Spacer()
                    
                    QuantitySelector(
                        value: Binding(
                            get: { quantities[nodeClass.id] ?? 0 },
                            set: { quantities[nodeClass.id] = $0 }
                        ),
                        maxTotal: maxAssetLimit,
                        currentTotal: totalNodesToCreate
                    )
                }
                .padding(.vertical, 4)
            }
        }
    }
    
    // MARK: - Location Mode Views
    @ViewBuilder
    private var locationInventorySection: some View {
        Section {
            HStack {
                Text(AppStrings.QuickInventory.createByLocation)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Spacer()
                Button {
                    showAddLocation = true
                } label: {
                    Label(AppStrings.QuickInventory.addLocation, systemImage: "plus.circle.fill")
                        .font(.caption)
                }
            }
        }
        
        if locations.isEmpty {
            Section {
                VStack(spacing: 12) {
                    Image(systemName: "location")
                        .font(.largeTitle)
                        .foregroundColor(.secondary)
                    Text(AppStrings.QuickInventory.noLocationsAdded)
                        .foregroundColor(.secondary)
                    Button(AppStrings.QuickInventory.addFirstLocation) {
                        showAddLocation = true
                    }
                    .buttonStyle(.bordered)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical)
            }
        } else {
            ForEach(locations.indices, id: \.self) { index in
                Section(header: locationHeader(for: locations[index], at: index)) {
                    ForEach(filteredNodeClasses) { nodeClass in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(nodeClass.name)
                                    .font(.subheadline)
                                if !nodeClass.style.isEmpty {
                                    Text(nodeClass.style)
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                            }
                            
                            Spacer()
                            
                            QuantitySelector(
                                value: Binding(
                                    get: { locations[index].quantities[nodeClass.id] ?? 0 },
                                    set: { newValue in
                                        locations[index].quantities[nodeClass.id] = newValue
                                    }
                                ),
                                maxTotal: maxAssetLimit,
                                currentTotal: totalNodesToCreate
                            )
                        }
                        .padding(.vertical, 2)
                    }
                }
            }
        }
    }
    
    private func locationHeader(for location: Location, at index: Int) -> some View {
        HStack {
            Label(location.name, systemImage: "location.fill")
                .font(.headline)
            
            if location.totalItems > 0 {
                Text(AppStrings.QuickInventory.itemsCount(location.totalItems))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Menu {
                Button {
                    editingLocation = location
                } label: {
                    Label(AppStrings.QuickInventory.editName, systemImage: "pencil")
                }
                
                Button {
                    duplicateLocation(at: index)
                } label: {
                    Label(AppStrings.QuickInventory.duplicate, systemImage: "doc.on.doc")
                }
                
                Button(role: .destructive) {
                    locations.remove(at: index)
                } label: {
                    Label(AppStrings.Common.delete, systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .foregroundColor(.accentColor)
            }
        }
    }
    
    @ViewBuilder
    private var addLocationSheet: some View {
        NavigationView {
            Form {
                Section(AppStrings.QuickInventory.locationName) {
                    TextField(AppStrings.QuickInventory.locationNamePlaceholder, text: $newLocationName)
                        .textFieldStyle(.roundedBorder)
                }
                
                if !existingLocationNames.isEmpty {
                    Section(AppStrings.QuickInventory.orSelectFromExisting) {
                        ForEach(existingLocationNames, id: \.self) { locationName in
                            Button {
                                newLocationName = locationName
                            } label: {
                                HStack {
                                    Text(locationName)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    if newLocationName == locationName {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.accentColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(AppStrings.QuickInventory.addLocation)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        newLocationName = ""
                        showAddLocation = false
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.CommonExtra.add) {
                        if !newLocationName.isEmpty {
                            locations.append(Location(name: newLocationName))
                            newLocationName = ""
                            showAddLocation = false
                        }
                    }
                    .disabled(newLocationName.isEmpty)
                }
            }
        }
    }
    
    private func editLocationSheet(location: Location) -> some View {
        NavigationView {
            Form {
                Section(AppStrings.QuickInventory.locationName) {
                    TextField(AppStrings.QuickInventory.locationName, text: Binding(
                        get: { location.name },
                        set: { newName in
                            if let index = locations.firstIndex(where: { $0.id == location.id }) {
                                locations[index].name = newName
                            }
                        }
                    ))
                    .textFieldStyle(.roundedBorder)
                }
            }
            .navigationTitle(AppStrings.QuickInventory.editLocation)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.done) {
                        editingLocation = nil
                    }
                }
            }
        }
    }
    
    // MARK: - Summary Section
    @ViewBuilder
    private var summarySection: some View {
        Section(AppStrings.QuickInventory.summary) {
            if inventoryMode == .byLocation {
                ForEach(locations.filter { $0.totalItems > 0 }) { location in
                    HStack {
                        Label(location.name, systemImage: "location.fill")
                            .font(.subheadline)
                        Spacer()
                        Text(AppStrings.QuickInventory.itemsCount(location.totalItems))
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                Divider()
            }
            
            HStack {
                Text(AppStrings.QuickInventory.totalAssetsToCreate(totalNodesToCreate))
                    .font(.headline)
                    .foregroundColor(isOverLimit ? .red : .primary)
                Spacer()
            }
            
            if isOverLimit {
                Label(AppStrings.QuickInventory.maximumAssetsAllowed(maxAssetLimit), systemImage: "exclamationmark.triangle.fill")
                    .font(.caption)
                    .foregroundColor(.red)
            }
            
            if networkState.mode == .offline {
                Label(AppStrings.QuickInventory.assetsQueuedForSync, systemImage: "wifi.slash")
                    .font(.caption)
                    .foregroundColor(.orange)
            }
        }
    }
    
    // MARK: - Helper Methods
    private func incrementQuantity(for nodeClassId: UUID) {
        quantities[nodeClassId] = (quantities[nodeClassId] ?? 0) + 1
    }
    
    private func decrementQuantity(for nodeClassId: UUID) {
        if let current = quantities[nodeClassId], current > 0 {
            quantities[nodeClassId] = current - 1
        }
    }
    
    private func duplicateLocation(at index: Int) {
        var newLocation = Location(name: "\(locations[index].name) Copy")
        newLocation.quantities = locations[index].quantities
        locations.insert(newLocation, at: index + 1)
    }
    
    // MARK: - Spatial Layout Helpers
    private struct ClusterLayout {
        let originX: Double
        let originY: Double
        let spacing: Double = 200.0
        let nodesPerRow: Int = 10
        
        func getPosition(for index: Int) -> (x: Double, y: Double) {
            let column = index % nodesPerRow
            let row = index / nodesPerRow
            
            return (
                x: originX + (Double(column) * spacing),
                y: originY + (Double(row) * spacing)
            )
        }
        
        static func randomOrigin(maxCoordinate: Double = 100000) -> ClusterLayout {
            // Generate random origin within the coordinate plane
            // Leave margin for cluster expansion (500 nodes = ~2000x10000px area)
            let margin = 15000.0  // Increased margin for larger clusters
            let range = maxCoordinate - margin * 2
            
            return ClusterLayout(
                originX: Double.random(in: -range/2...range/2),
                originY: Double.random(in: -range/2...range/2)
            )
        }
    }
    
    // MARK: - Smart Cluster Placement
    private class ClusterManager {
        private var clusterIndex = 0
        private let baseSpacing: Double = 3000.0  // Base spacing between clusters
        
        func getNextClusterOrigin(nodeCount: Int, totalRooms: Int) -> ClusterLayout {
            // Calculate cluster dimensions
            let nodesPerRow = 10
            let rows = (nodeCount + nodesPerRow - 1) / nodesPerRow
            let clusterWidth = Double(nodesPerRow) * 200.0
            let clusterHeight = Double(rows) * 200.0
            
            // Determine spacing multiplier based on total asset count
            let spacingMultiplier: Double
            if nodeCount <= 50 {
                spacingMultiplier = 1.0  // Tight spacing for small clusters
            } else if nodeCount <= 100 {
                spacingMultiplier = 1.5  // Medium spacing
            } else if nodeCount <= 200 {
                spacingMultiplier = 2.5  // Larger spacing
            } else {
                spacingMultiplier = 4.0  // Maximum spacing for huge clusters
            }
            
            let adjustedSpacing = baseSpacing * spacingMultiplier
            
            // Use a spiral pattern for cluster placement
            let angle = Double(clusterIndex) * (2.0 * .pi / max(6.0, Double(totalRooms)))
            let radius = adjustedSpacing * (1.0 + Double(clusterIndex) / 3.0)
            
            // Add small random offset to avoid perfect geometric patterns
            let randomOffset = 500.0
            let offsetX = Double.random(in: -randomOffset...randomOffset)
            let offsetY = Double.random(in: -randomOffset...randomOffset)
            
            let originX = cos(angle) * radius + offsetX
            let originY = sin(angle) * radius + offsetY
            
            clusterIndex += 1
            
            return ClusterLayout(
                originX: originX,
                originY: originY
            )
        }
        
        func reset() {
            clusterIndex = 0
        }
    }
    
    // MARK: - Node Creation
    private func createNodes() {
        // Double-check the limit before creating
        guard totalNodesToCreate <= maxAssetLimit else {
            showLimitExceededAlert = true
            return
        }
        
        isCreating = true
        createdCount = 0
        
        Task {
            var createdNodes: [NodeV2] = []
            
            switch inventoryMode {
            case .simple:
                createdNodes = await createSimpleNodes()
            case .byLocation:
                createdNodes = await createLocationNodes()
            }
            
            do {
                try modelContext.save()
                
                // Update the graph
                let dto = sldService.createDTO(forSLDId: sld.id, modelContext: modelContext, customName: "quick-inventory-update")
                await MainActor.run {
                    WebViewBridge.updateGraph(with: dto, animated: true)
                }
                
                // Handle sync based on network mode
                if networkState.mode == .online {
                    // Try to sync each node
                    for node in createdNodes {
                        do {
                            _ = try await APIClient.shared.createNode(node: node)
                            node.lastSyncedAt = Date()
                            node.needsSync = false
                        } catch {
                            AppLogger.log(.error, "Failed to sync node \(node.id): \(error)", category: .ui)
                            let op = SyncOp(
                                target: .node,
                                operation: .create,
                                node: node
                            )
                            networkState.enqueue(op)
                        }
                    }
                    try? modelContext.save()
                } else {
                    // Queue all nodes for sync
                    for node in createdNodes {
                        let op = SyncOp(
                            target: .node,
                            operation: .create,
                            node: node
                        )
                        networkState.enqueue(op)
                    }
                }
                
                await MainActor.run {
                    isCreating = false
                    showSuccessAlert = true
                }
                
            } catch {
                await MainActor.run {
                    isCreating = false
                    creationError = error
                }
            }
        }
    }
    
    private func createSimpleNodes() async -> [NodeV2] {
        var createdNodes: [NodeV2] = []
        
        // For simple mode, place cluster at origin or slightly offset
        let cluster = ClusterLayout(
            originX: Double.random(in: -1000...1000),
            originY: Double.random(in: -1000...1000)
        )
        var nodeIndex = 0
        
        for (nodeClassId, quantity) in quantities where quantity > 0 {
            guard let nodeClass = filteredNodeClasses.first(where: { $0.id == nodeClassId }) else { continue }
            
            for i in 1...quantity {
                let position = cluster.getPosition(for: nodeIndex)
                
                let node = NodeV2(
                    id: UUID(),
                    label: "\(nodeClass.name) \(i)",
                    type: nodeClass.style.isEmpty ? "unknown" : nodeClass.style,
                    sld: sld,
                    parent_id: nil,
                    x: position.x,
                    y: position.y,
                    width: nodeClass.width,
                    height: nodeClass.height,
                    photos: [],
                    is_deleted: false,
                    location: nil,  // No location in simple mode
                    node_class: nodeClass,
                    core_attributes: [],
                    node_tasks: [],
                    ir_photos: []
                )
                
                node.lastModifiedAt = Date()
                node.needsSync = true
                node.lastSyncedAt = nil

                modelContext.insert(node)
                sld.nodes.append(node)
                createdNodes.append(node)
                nodeIndex += 1
                
                await MainActor.run {
                    createdCount += 1
                }
            }
        }
        
        return createdNodes
    }
    
    private func createLocationNodes() async -> [NodeV2] {
        var createdNodes: [NodeV2] = []
        let clusterManager = ClusterManager()
        
        for location in locations {
            // Calculate total nodes for this location
            let totalNodesInLocation = location.quantities.values.reduce(0, +)
            
            // Get smart cluster placement based on node count
            let cluster = clusterManager.getNextClusterOrigin(
                nodeCount: totalNodesInLocation,
                totalRooms: locations.count
            )
            var nodeIndexInLocation = 0
            
            // Create nodes for each asset type in the location
            for (nodeClassId, quantity) in location.quantities where quantity > 0 {
                guard let nodeClass = filteredNodeClasses.first(where: { $0.id == nodeClassId }) else { continue }
                
                for i in 1...quantity {
                    let position = cluster.getPosition(for: nodeIndexInLocation)
                    
                    let node = NodeV2(
                        id: UUID(),
                        label: "\(nodeClass.name) \(i)",
                        type: nodeClass.style.isEmpty ? "unknown" : nodeClass.style,
                        sld: sld,
                        parent_id: nil,
                        x: position.x,
                        y: position.y,
                        width: nodeClass.width,
                        height: nodeClass.height,
                        photos: [],
                        is_deleted: false,
                        location: location.name,  // Set the location property
                        node_class: nodeClass,
                        core_attributes: [],
                        node_tasks: [],
                        ir_photos: []
                    )
                    
                    node.lastModifiedAt = Date()
                    node.needsSync = true
                    node.lastSyncedAt = nil

                    modelContext.insert(node)
                    sld.nodes.append(node)
                    createdNodes.append(node)
                    nodeIndexInLocation += 1
                    
                    await MainActor.run {
                        createdCount += 1
                    }
                }
            }
        }
        
        return createdNodes
    }
}

// MARK: - Quantity Selector Component
struct QuantitySelector: View {
    @Binding var value: Int
    var maxTotal: Int = Int.max
    var currentTotal: Int = 0
    
    @State private var timer: Timer?
    @State private var isLongPressing = false
    @State private var incrementSpeed = 0
    
    private var remainingCapacity: Int {
        maxTotal - currentTotal + value
    }
    
    var body: some View {
        HStack(spacing: 20) {
            // Decrement button
            Button {
                if value > 0 {
                    value -= 1
                }
            } label: {
                Image(systemName: "minus.circle.fill")
                    .foregroundColor(.red)
                    .font(.title2)
            }
            .buttonStyle(.plain)
            .disabled(value == 0)
            .onLongPressGesture(minimumDuration: 0.5, maximumDistance: .infinity, pressing: { pressing in
                if pressing {
                    startDecrementing()
                } else {
                    stopTimer()
                }
            }, perform: {})
            
            Text("\(value)")
                .font(.title3)
                .monospacedDigit()
                .frame(minWidth: 40)
            
            // Increment button
            Button {
                if value < remainingCapacity {
                    value += 1
                }
            } label: {
                Image(systemName: "plus.circle.fill")
                    .foregroundColor(value >= remainingCapacity ? .gray : .green)
                    .font(.title2)
            }
            .buttonStyle(.plain)
            .disabled(value >= remainingCapacity)
            .onLongPressGesture(minimumDuration: 0.5, maximumDistance: .infinity, pressing: { pressing in
                if pressing {
                    startIncrementing()
                } else {
                    stopTimer()
                }
            }, perform: {})
        }
    }
    
    private func startIncrementing() {
        isLongPressing = true
        incrementSpeed = 0
        
        timer = Timer.scheduledTimer(withTimeInterval: 0.3, repeats: true) { _ in
            incrementSpeed += 1
            
            // Calculate increment amount
            let incrementAmount = incrementSpeed > 3 ? 5 : 1
            
            // Check if we can increment without exceeding capacity
            if value + incrementAmount <= remainingCapacity {
                value += incrementAmount
            } else if value < remainingCapacity {
                // Increment to exactly the remaining capacity
                value = remainingCapacity
                stopTimer()
                return
            } else {
                // Already at capacity, stop
                stopTimer()
                return
            }
            
            // After 10 iterations, go even faster
            if incrementSpeed > 10 {
                timer?.invalidate()
                timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
                    if value + 5 <= remainingCapacity {
                        value += 5
                    } else if value < remainingCapacity {
                        value = remainingCapacity
                        stopTimer()
                    } else {
                        stopTimer()
                    }
                }
            }
        }
    }
    
    private func startDecrementing() {
        isLongPressing = true
        incrementSpeed = 0
        
        timer = Timer.scheduledTimer(withTimeInterval: 0.3, repeats: true) { _ in
            incrementSpeed += 1
            
            // After 3 iterations, switch to bulk mode (decrement by 5)
            let decrementAmount = incrementSpeed > 3 ? 5 : 1
            
            if value >= decrementAmount {
                value -= decrementAmount
            } else {
                value = 0
            }
            
            // Stop if we hit zero
            if value == 0 {
                stopTimer()
                return
            }
            
            // After 10 iterations, go even faster
            if incrementSpeed > 10 {
                timer?.invalidate()
                timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
                    if value >= 5 {
                        value -= 5
                    } else {
                        value = 0
                        stopTimer()
                    }
                }
            }
        }
    }
    
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
        isLongPressing = false
        incrementSpeed = 0
    }
}
