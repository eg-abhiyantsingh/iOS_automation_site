import SwiftUI

/// View component for displaying sync status and triggering manual sync
struct SyncStatusView: View {
    @EnvironmentObject var networkState: NetworkState
    
    var body: some View {
        Button {
            if networkState.mode == .online && !networkState.isSyncing {
                networkState.flushQueue()
            }
        } label: {
            HStack(spacing: 4) {
                if networkState.isSyncing {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                        .scaleEffect(0.8)
                } else {
                    Image(systemName: "arrow.2.circlepath")
                }
                
                if networkState.isSyncing && networkState.syncTotal > 0 {
                    Text("\(networkState.syncQueueCount)")
                        .monospacedDigit()
                        .animation(.easeInOut(duration: 0.3), value: networkState.syncQueueCount)
                } else {
                    Text("\(networkState.syncQueueCount)")
                        .monospacedDigit()
                }
            }
            .foregroundColor(networkState.syncQueueCount == 0 ? .secondary : .primary)
        }
        .disabled(networkState.syncQueueCount == 0 || networkState.isSyncing)
    }
}