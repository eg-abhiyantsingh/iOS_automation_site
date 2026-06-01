import Foundation
import SwiftUI
import Combine

/// Service responsible for managing network connectivity state
final class NetworkStateService: ObservableObject {
    @Published var mode: NetworkMode = .online
    
    init(initialMode: NetworkMode = .online) {
        self.mode = initialMode
    }
    
    /// Toggle between online and offline modes
    func toggleMode() {
        mode.toggle()
    }
    
    /// Handle network mode changes
    /// This can be extended to trigger specific actions when network state changes
    func handleNetworkModeChange(from oldMode: NetworkMode, to newMode: NetworkMode) {
        if oldMode == .offline && newMode == .online {
            AppLogger.log(.info, "Network came online from offline state", category: .network)
            // Could trigger sync queue flush here if needed
        } else if oldMode == .online && newMode == .offline {
            AppLogger.log(.info, "Network went offline", category: .network)
            // Could trigger any cleanup needed for offline mode
        }
    }
    
    /// Check if currently online
    var isOnline: Bool {
        mode == .online
    }
    
    /// Check if currently offline
    var isOffline: Bool {
        mode == .offline
    }
}