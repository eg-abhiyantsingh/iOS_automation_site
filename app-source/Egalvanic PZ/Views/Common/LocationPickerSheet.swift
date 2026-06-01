//
//  LocationPickerSheet.swift
//  Egalvanic PZ
//
//  Searchable list-picker sheet used to pick a parent Building or Floor when
//  reassigning a location. Generic over the item type so the same view works
//  for both Building and Floor without coupling to either model.
//

import SwiftUI

struct LocationPickerSheet<Item>: View {
    let title: String
    let searchPrompt: String
    let emptyStateText: String
    let iconSystemName: String
    let items: [Item]
    let selectedItemId: UUID?
    let idKeyPath: KeyPath<Item, UUID>
    let nameKeyPath: KeyPath<Item, String>
    let onSelect: (Item) -> Void
    @Binding var isPresented: Bool

    @State private var searchText: String = ""

    private var filteredItems: [Item] {
        let trimmed = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return items }
        return items.filter { $0[keyPath: nameKeyPath].localizedCaseInsensitiveContains(trimmed) }
    }

    var body: some View {
        NavigationStack {
            Group {
                if filteredItems.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: iconSystemName)
                            .font(.system(size: 36))
                            .foregroundColor(.secondary)
                        Text(emptyStateText)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(filteredItems, id: idKeyPath) { item in
                            Button(action: {
                                onSelect(item)
                                isPresented = false
                            }) {
                                HStack {
                                    Image(systemName: iconSystemName)
                                        .foregroundColor(.secondary)
                                    Text(item[keyPath: nameKeyPath])
                                        .foregroundColor(.primary)
                                    Spacer()
                                    if selectedItemId == item[keyPath: idKeyPath] {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.blue)
                                    }
                                }
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                    .transaction { $0.animation = nil }
                }
            }
            .searchable(text: $searchText, prompt: searchPrompt)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.cancel) {
                        isPresented = false
                    }
                }
            }
        }
    }
}
