//
//  ActiveSessionBadge.swift
//  Egalvanic PZ
//
//  Reusable "WO" badge for active session indicators
//

import SwiftUI

struct ActiveSessionBadge: View {
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 6)
                .fill(Color.blue)
                .frame(width: 34, height: 24)
            Text(AppStrings.Forms.wo)
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(.white)
        }
    }
}
