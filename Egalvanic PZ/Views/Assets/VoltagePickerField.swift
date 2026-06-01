//
//  VoltagePickerField.swift
//  Egalvanic PZ
//
import SwiftUI
import UIKit

/// Standard voltage options matching enum_node_voltages DB table
struct VoltageOption: Identifiable {
    let id: Int
    let label: String
    let value: Double

    static let all: [VoltageOption] = [
        VoltageOption(id: 1,  label: "120V",     value: 120),
        VoltageOption(id: 2,  label: "120/208V", value: 208),
        VoltageOption(id: 3,  label: "120/240V", value: 240),
        VoltageOption(id: 4,  label: "208V",     value: 208),
        VoltageOption(id: 5,  label: "240V",     value: 240),
        VoltageOption(id: 6,  label: "277V",     value: 277),
        VoltageOption(id: 7,  label: "277/480V", value: 480),
        VoltageOption(id: 20, label: "347V",     value: 347),
        VoltageOption(id: 8,  label: "480V",     value: 480),
        VoltageOption(id: 9,  label: "600V",     value: 600),
        VoltageOption(id: 10, label: "2.4kV",    value: 2400),
        VoltageOption(id: 11, label: "4.16kV",   value: 4160),
        VoltageOption(id: 12, label: "12.47kV",  value: 12470),
        VoltageOption(id: 13, label: "13.2kV",   value: 13200),
        VoltageOption(id: 14, label: "13.8kV",   value: 13800),
        VoltageOption(id: 15, label: "23kV",     value: 23000),
        VoltageOption(id: 16, label: "34.5kV",   value: 34500),
        VoltageOption(id: 17, label: "69kV",     value: 69000),
    ]
}

/// Reusable voltage picker field — uses sheet instead of Menu for performance.
/// 17 options eagerly evaluated in a Menu causes GPU blur + lag on lower devices.
struct VoltagePickerField: View {
    let label: String
    @Binding var selectedId: Int?
    @Binding var selectedValue: Double?

    @State private var showSheet = false

    private var selectedOption: VoltageOption? {
        guard let id = selectedId else { return nil }
        return VoltageOption.all.first { $0.id == id }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)

            HStack {
                Image(systemName: "bolt")
                    .foregroundColor(.secondary)
                    .frame(width: 20)

                Text(selectedOption?.label ?? "Select voltage")
                    .foregroundColor(selectedOption != nil ? .primary : .secondary)

                Spacer()

                // Inline clear button
                if selectedId != nil {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                        .font(.body)
                        .frame(width: 30, height: 30)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            selectedId = nil
                            selectedValue = nil
                        }
                }

                Image(systemName: "chevron.down")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(.systemGray6))
            .cornerRadius(10)
            .contentShape(Rectangle())
            .onTapGesture {
                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                showSheet = true
            }
            .sheet(isPresented: $showSheet) {
                NavigationStack {
                    List {
                        ForEach(VoltageOption.all) { option in
                            Button {
                                selectedId = option.id
                                selectedValue = option.value
                                showSheet = false
                            } label: {
                                HStack {
                                    Text(option.label)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    if selectedId == option.id {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.blue)
                                    }
                                }
                            }
                        }
                    }
                    .navigationTitle(label)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button(AppStrings.Common.cancel) {
                                showSheet = false
                            }
                        }
                    }
                }
                .presentationDetents([.medium, .large])
            }
        }
    }
}
