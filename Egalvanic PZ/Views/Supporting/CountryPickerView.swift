//
//  CountryPickerView.swift
//  Egalvanic PZ
//

import SwiftUI

struct CountryPickerView: View {
    let countries: [(String, String)]  // (code, name)
    let selectedCode: String
    let onSelect: (String) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                ForEach(countries, id: \.0) { code, name in
                    Button {
                        onSelect(code)
                        dismiss()
                    } label: {
                        HStack(spacing: 12) {
                            Text(code.countryFlag)
                                .font(.title2)

                            Text(name)
                                .foregroundColor(.primary)

                            Spacer()

                            if selectedCode == code {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
            }
            .navigationTitle(AppStrings.Site.country)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(AppStrings.Common.done) { dismiss() }
                }
            }
        }
    }
}
