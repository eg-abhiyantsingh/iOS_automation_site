//
//  PhotoCaptionBadge.swift
//  Egalvanic PZ
//
import SwiftUI

struct PhotoCaptionBadge: View {
    var body: some View {
        Image(systemName: "text.bubble.fill")
            .font(.caption2)
            .padding(4)
            .background(Color.blue)
            .foregroundColor(.white)
            .clipShape(Circle())
    }
}

#Preview {
    PhotoCaptionBadge()
}
