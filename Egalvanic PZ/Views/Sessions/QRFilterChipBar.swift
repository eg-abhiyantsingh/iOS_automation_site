//
//  QRFilterChipBar.swift
//  Egalvanic PZ
//
//  Reusable filter chip showing active QR-scanned asset filter
//

import SwiftUI

struct QRFilterChipBar: View {
    let nodeName: String
    let onClear: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "qrcode.viewfinder")
                .font(.caption)
                .foregroundColor(.blue)

            Text(AppStrings.Sessions.filteredByAsset(nodeName))
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.primary)
                .lineLimit(1)

            Spacer()

            Button(action: onClear) {
                Image(systemName: "xmark.circle.fill")
                    .font(.body)
                    .foregroundColor(.secondary)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.blue.opacity(0.1))
        .cornerRadius(10)
        .padding(.horizontal)
    }
}
