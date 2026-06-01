//
//  LocationPickerView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 7/28/25.
//

import SwiftUI
import SwiftData
import Foundation

// MARK: - Location Picker View

struct LocationPickerView: View {
    let locations: [String]
    @Binding var selectedLocation: String
    @Binding var searchText: String
    let onDismiss: () -> Void
    
    private var filteredLocations: [String] {
        if searchText.isEmpty {
            return locations
        }
        return locations.filter {
            $0.localizedCaseInsensitiveContains(searchText)
        }
    }
    
    var body: some View {
        NavigationView {
            List {
                ForEach(filteredLocations, id: \.self) { location in
                    Button(action: {
                        selectedLocation = location
                        onDismiss()
                    }) {
                        HStack {
                            Text(location)
                                .foregroundColor(.primary)
                            Spacer()
                            if selectedLocation == location {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
            }
            .searchable(text: $searchText, prompt: AppStrings.AssetsExtra.searchLocations)
            .navigationTitle(AppStrings.AssetsExtra.selectLocation)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.done) {
                        onDismiss()
                    }
                }
            }
        }
    }
}
