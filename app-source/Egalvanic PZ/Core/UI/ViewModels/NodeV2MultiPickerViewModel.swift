//
//  NodeV2MultiPickerViewModel.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData
import Combine

@MainActor
class NodeV2MultiPickerViewModel: ObservableObject {

    // MARK: - Dependencies (injected via configure)

    private(set) var modelContext: ModelContext?

    // MARK: - Selection State

    @Published var selectedNodes: Set<NodeV2> = []
    @Published var showingSelector = false
    @Published var showingAddAsset = false
    @Published var searchText = ""

    // MARK: - QR Scanner State

    @Published var showingQRScanner = false
    @Published var scannedQRCode = ""
    @Published var showingQRError = false
    @Published var qrErrorMessage = ""
    @Published var showDuplicateQRAlert = false
    @Published var duplicateNodes: [NodeV2] = []

    // MARK: - Cached Data (avoids expensive recomputation on every render)

    @Published var cachedNodeClasses: [NodeClass] = []
    @Published var cachedLocations: [String] = []

    // MARK: - Init

    init() {
        setupSearchDebounce()
    }

    // MARK: - Configure

    func configure(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    // MARK: - Data Loading

    func loadInitialValues(from storage: FieldValueStorage, fieldId: String) {
        if let storedNodes = storage.getValue(for: fieldId) as? [NodeV2] {
            selectedNodes = Set(storedNodes)
        }
    }

    func refreshCachedData(slds: [SLDV2]) {
        // Cache locations from SLD
        if let sld = slds.first {
            let locations = sld.nodes
                .filter { !$0.is_deleted }
                .compactMap { $0.location?.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
            cachedLocations = Array(Set(locations)).sorted()
        } else {
            cachedLocations = []
        }

        // Cache node classes from SwiftData (avoids fetch on every render)
        guard let modelContext = modelContext else { return }
        let descriptor = FetchDescriptor<NodeClass>()
        let nodeClasses = (try? modelContext.fetch(descriptor)) ?? []
        cachedNodeClasses = nodeClasses
            .filter { !$0.is_deleted }
            .sorted { $0.name < $1.name }
    }

    // MARK: - Computed Helpers

    func nodesFromForms(_ storage: FieldValueStorage) -> [NodeV2] {
        if let formData = storage.getValue(for: "forms") as? [[String: Any]] {
            var linkedNodes: [NodeV2] = []
            for data in formData {
                if let nodes = data["nodes"] as? [NodeV2] {
                    linkedNodes.append(contentsOf: nodes)
                }
            }
            return linkedNodes
        }
        return []
    }

    @Published var sortedNodes: [NodeV2] = []
    @Published var displayedNodes: [NodeV2] = []
    private var searchCancellable: AnyCancellable?

    // Call once during init — sets up debounced search pipeline
    private func setupSearchDebounce() {
        searchCancellable = $searchText
            .debounce(for: .milliseconds(100), scheduler: RunLoop.main)
            .removeDuplicates()
            .sink { [weak self] query in
                guard let self else { return }
                if query.isEmpty {
                    self.displayedNodes = self.sortedNodes
                } else {
                    let q = query.lowercased()
                    self.displayedNodes = self.sortedNodes.filter { node in
                        node.label.lowercased().contains(q) ||
                        (node.location?.lowercased().contains(q) ?? false)
                    }
                }
            }
    }

    func updateSortedNodes(from allNodes: [NodeV2]) {
        sortedNodes = allNodes.sorted { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }
        if searchText.isEmpty {
            displayedNodes = sortedNodes
        } else {
            let q = searchText.lowercased()
            displayedNodes = sortedNodes.filter { node in
                node.label.lowercased().contains(q) ||
                (node.location?.lowercased().contains(q) ?? false)
            }
        }
    }

    // MARK: - Actions

    func updateStorage(_ storage: StandardFieldValueStorage, fieldId: String) {
        storage.setValue(Array(selectedNodes), for: fieldId)
    }

    func removeNode(_ node: NodeV2, storage: StandardFieldValueStorage, fieldId: String) {
        selectedNodes.remove(node)
        updateStorage(storage, fieldId: fieldId)
    }

    func toggleNode(_ node: NodeV2) {
        if selectedNodes.contains(node) {
            selectedNodes.remove(node)
        } else {
            selectedNodes.insert(node)
        }
    }

    func handleQRScan(_ code: String, nodes: [NodeV2], storage: StandardFieldValueStorage, fieldId: String) {
        let matchingNodes = nodes.filter { $0.qr_code?.lowercased() == code.lowercased() }

        if matchingNodes.count == 1 {
            selectedNodes.insert(matchingNodes[0])
            updateStorage(storage, fieldId: fieldId)
        } else if matchingNodes.count > 1 {
            duplicateNodes = matchingNodes
            duplicateQRStorage = storage
            duplicateQRFieldId = fieldId
            showDuplicateQRAlert = true
        } else {
            qrErrorMessage = "No asset found with QR code: \(code)"
            showingQRError = true
        }
    }

    // Temporary storage for duplicate QR selection callback
    private var duplicateQRStorage: StandardFieldValueStorage?
    private var duplicateQRFieldId: String?

    func selectDuplicateNode(_ node: NodeV2) {
        selectedNodes.insert(node)
        if let storage = duplicateQRStorage, let fieldId = duplicateQRFieldId {
            updateStorage(storage, fieldId: fieldId)
        }
        clearDuplicateQRState()
    }

    func clearDuplicateQRState() {
        duplicateNodes = []
        duplicateQRStorage = nil
        duplicateQRFieldId = nil
    }

    func dismissSelector() {
        searchText = ""
        showingSelector = false
    }

    func confirmSelection(storage: StandardFieldValueStorage, fieldId: String) {
        updateStorage(storage, fieldId: fieldId)
        searchText = ""
        showingSelector = false
    }

    func handleAssetSaved(
        node: NodeV2,
        photos: [Photo],
        irPhotos: [IRPhoto],
        sld: SLDV2,
        modelContext: ModelContext,
        networkState: NetworkState,
        storage: StandardFieldValueStorage,
        fieldId: String,
        slds: [SLDV2]
    ) {
        showingAddAsset = false

        // Add node to SLD's nodes array immediately for offline visibility
        if !sld.nodes.contains(where: { $0.id == node.id }) {
            sld.nodes.append(node)
        }

        // Insert into model context immediately
        modelContext.insert(node)
        photos.forEach { modelContext.insert($0) }
        irPhotos.forEach { modelContext.insert($0) }

        // Save context immediately for offline use
        do {
            try modelContext.save()
        } catch {
            AppLogger.log(.error, "Failed to save node locally: \(error)", category: .ui)
        }

        // Auto-select the newly created asset
        selectedNodes.insert(node)
        updateStorage(storage, fieldId: fieldId)
        searchText = ""

        // Refresh cached data since new node may have new location
        refreshCachedData(slds: slds)

        // Then handle the backend sync
        Task {
            await NodeService.createNewNodeWithPhotosAndIR(
                node: node,
                photos: photos,
                irPhotos: irPhotos,
                networkState: networkState,
                modelContext: modelContext
            )
        }
    }
}
