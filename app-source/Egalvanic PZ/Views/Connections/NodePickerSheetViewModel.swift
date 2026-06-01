//
//  NodePickerSheetViewModel.swift
//  Egalvanic PZ
//

import SwiftUI
import Combine

final class NodePickerSheetViewModel: ObservableObject {

    // MARK: - Node Item

    struct NodeItem: Identifiable, Equatable {
        let id: UUID
        let displayLabel: String
        let subtitle: String?
        let qrCode: String?
        let searchableText: String

        init(id: UUID, displayLabel: String, subtitle: String?, qrCode: String? = nil) {
            self.id = id
            self.displayLabel = displayLabel
            self.subtitle = subtitle
            self.qrCode = qrCode
            self.searchableText = (displayLabel + " " + (subtitle ?? "") + " " + (qrCode ?? "")).lowercased()
        }
    }

    // MARK: - Published Properties

    @Published var searchText: String = ""
    @Published private(set) var filteredNodes: [NodeItem] = []

    // MARK: - Private

    private let allNodes: [NodeItem]
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Init

    init(nodes: [NodeItem]) {
        self.allNodes = nodes
        self.filteredNodes = nodes
        setupSearch()
    }

    // MARK: - Combine Pipeline

    private func setupSearch() {
        $searchText
            .debounce(for: .milliseconds(350), scheduler: DispatchQueue.main)
            .removeDuplicates()
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
            .receive(on: DispatchQueue.global(qos: .userInitiated))
            .scan((query: "", results: allNodes)) { [allNodes] state, newQuery in
                guard newQuery.count >= 2 else {
                    return (query: newQuery, results: allNodes)
                }

                let base = newQuery.hasPrefix(state.query) && state.query.count >= 2
                    ? state.results
                    : allNodes

                let filtered = base.filter { $0.searchableText.contains(newQuery) }
                return (query: newQuery, results: filtered)
            }
            .map(\.results)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] result in
                self?.filteredNodes = result
            }
            .store(in: &cancellables)
    }
}
