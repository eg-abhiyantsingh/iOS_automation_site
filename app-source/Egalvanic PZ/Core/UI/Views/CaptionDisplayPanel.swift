//
//  CaptionDisplayPanel.swift
//  Egalvanic PZ
//
import SwiftUI

struct CaptionDisplayPanel: View {
    let caption: String
    let onEditTap: () -> Void
    @State private var isExpanded = false

    private let lineLimit = 3

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Caption Text
            VStack(alignment: .leading, spacing: 4) {
                Text(caption)
                    .font(.body)
                    .foregroundColor(.white)
                    .lineLimit(isExpanded ? nil : lineLimit)
                    .multilineTextAlignment(.leading)

                // Show "Show more/less" button if caption is long
                if caption.count > 100 {
                    Button(action: {
                        withAnimation {
                            isExpanded.toggle()
                        }
                    }) {
                        Text(isExpanded ? "Show less" : "Show more")
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                }
            }

            Spacer()

            // Edit Button
            Button(action: onEditTap) {
                Image(systemName: "pencil.circle.fill")
                    .font(.title2)
                    .foregroundColor(.white.opacity(0.8))
            }
        }
        .padding()
        .background(Color.black.opacity(0.7))
    }
}

#Preview {
    ZStack {
        Color.black.ignoresSafeArea()

        VStack {
            Spacer()
            CaptionDisplayPanel(
                caption: "This is a sample caption for a photo. It can be quite long and will show a 'Show more' button if it exceeds 100 characters. This allows users to see the full caption without cluttering the interface.",
                onEditTap: { AppLogger.log(.debug, "Edit tapped", category: .photo) }
            )
        }
    }
}
