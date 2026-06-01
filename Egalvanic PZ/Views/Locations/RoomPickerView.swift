//
//  RoomPickerView.swift
//  Egalvanic PZ
//
//  Room picker component for node creation/editing
//

import SwiftUI
import SwiftData
import UIKit

struct RoomPickerView: View {
    @Binding var selectedRoom: Room?
    let isLocked: Bool
    let sld: SLDV2

    @State private var showLocationPicker = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(AppStrings.AssetsExtra.location)
                .font(.caption)
                .foregroundColor(.secondary)

            Button(action: {
                if !isLocked {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    showLocationPicker = true
                }
            }) {
                HStack {
                    Image(systemName: "building.2")
                        .foregroundColor(.secondary)
                        .frame(width: 20)

                    if let room = selectedRoom {
                        Text(room.fullPath)
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                    } else {
                        Text(AppStrings.AssetsExtra.selectLocationPlaceholder)
                            .foregroundColor(.secondary)
                    }

                    Spacer(minLength: 8)

                    if !isLocked {
                        Image(systemName: "chevron.down")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    } else {
                        Image(systemName: "lock.fill")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
            .disabled(isLocked)
        }
        .fullScreenCover(isPresented: $showLocationPicker) {
            HierarchicalLocationPickerView(
                sld: sld,
                selectedRoom: $selectedRoom,
                onDismiss: {
                    showLocationPicker = false
                }
            )
        }
    }
}
