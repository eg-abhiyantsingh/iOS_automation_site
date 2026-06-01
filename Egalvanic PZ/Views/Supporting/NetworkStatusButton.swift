//
//  NetworkStatusButton.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/8/25.
//

import SwiftUI

struct NetworkStatusButton: View {
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var languageManager: LanguageManager

    var body: some View {
        Menu {
            if networkState.syncQueueCount > 0 {
                Button {
                    if networkState.mode == .online {
                        networkState.flushQueue()
                    }
                } label: {
                    Label(AppStrings.Supporting.syncRecords(networkState.syncQueueCount),
                          systemImage: "arrow.triangle.2.circlepath")
                }
                .disabled(networkState.mode == .offline)
                Divider()
            }
            Button {
                networkState.toggleMode()                
            } label: {
                Label(
                    networkState.mode == .online ? AppStrings.Supporting.goOffline : AppStrings.Supporting.goOnline,
                    systemImage: networkState.mode == .online ? "wifi" : "wifi.slash"
                )
            }
        } label: {
            ZStack(alignment: .topTrailing) {
                Image(systemName: networkState.mode == .online ? "wifi" : "wifi.slash")
                    .padding(.trailing, networkState.syncQueueCount > 0 ? 8 : 0)
                    .padding(.top, networkState.syncQueueCount > 0 ? 6 : 0)

                if networkState.syncQueueCount > 0 {
                    BadgeView(count: networkState.syncQueueCount)
                }
            }
        }
    }
}

// Separate the badge for even more reusability
struct BadgeView: View {
    let count: Int
    
    var body: some View {
        Text("\(count)")
            .font(.caption2)
            .fontWeight(.bold)
            .foregroundColor(.white)
            .padding(.horizontal, 5)
            .padding(.vertical, 2)
            .background(Color.red)
            .clipShape(Capsule())
            .offset(x: 6, y: -3)
    }
}
