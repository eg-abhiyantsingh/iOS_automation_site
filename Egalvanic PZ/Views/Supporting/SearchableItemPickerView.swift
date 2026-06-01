//
//  SearchableItemPickerView.swift
//  Egalvanic PZ
//

import SwiftUI

struct SearchableItemPickerView<Item: Identifiable>: View {
    let title: String
    let items: [Item]
    let selectedId: String?
    let idToString: (Item) -> String
    let nameOf: (Item) -> String
    let searchPrompt: String
    var allowClear: Bool = false
    let onSelect: (Item?) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""

    private var filteredItems: [Item] {
        if searchText.isEmpty { return items }
        return items.filter { nameOf($0).localizedCaseInsensitiveContains(searchText) }
    }

    var body: some View {
        NavigationStack {
            List {
                if allowClear {
                    Button {
                        onSelect(nil)
                        dismiss()
                    } label: {
                        HStack {
                            Text(AppStrings.Site.noneOptional)
                                .foregroundColor(.primary)
                            Spacer()
                            if selectedId == nil {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
                ForEach(filteredItems) { item in
                    Button {
                        onSelect(item)
                        dismiss()
                    } label: {
                        HStack {
                            Text(nameOf(item))
                                .foregroundColor(.primary)
                            Spacer()
                            if selectedId == idToString(item) {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
            }
            .searchable(text: $searchText, prompt: searchPrompt)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(AppStrings.Common.done) { dismiss() }
                }
            }
        }
    }
}
