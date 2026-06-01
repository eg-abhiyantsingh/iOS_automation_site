import Foundation
import Network
import Combine

/// Service to monitor actual network connectivity status
final class NetworkReachability: ObservableObject {
    @MainActor
    static let shared = NetworkReachability()
    
    @MainActor
    @Published private(set) var isConnected: Bool = false
    @MainActor
    @Published private(set) var connectionType: NWInterface.InterfaceType?
    @MainActor
    @Published private(set) var isExpensive: Bool = false
    @MainActor
    @Published private(set) var isConstrained: Bool = false
    
    private let monitor: NWPathMonitor
    private let queue = DispatchQueue(label: "NetworkReachability")
    
    private init() {
        monitor = NWPathMonitor()
        startMonitoring()
    }
    
    deinit {
        // Stop monitoring synchronously without MainActor
        monitor.cancel()
    }
    
    private func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor [weak self] in
                guard let self = self else { return }
                
                let wasConnected = self.isConnected
                
                self.isConnected = path.status == .satisfied
                self.isExpensive = path.isExpensive
                self.isConstrained = path.isConstrained
                
                // Determine connection type
                if path.usesInterfaceType(.wifi) {
                    self.connectionType = .wifi
                } else if path.usesInterfaceType(.cellular) {
                    self.connectionType = .cellular
                } else if path.usesInterfaceType(.wiredEthernet) {
                    self.connectionType = .wiredEthernet
                } else {
                    self.connectionType = nil
                }
                
                // Log status changes
                if wasConnected != self.isConnected {
                    if self.isConnected {
                        slog("Network Connected", category: .network, data: [
                            "connection_type": self.connectionTypeString,
                            "is_expensive": self.isExpensive,
                            "is_constrained": self.isConstrained
                        ])
                    } else {
                        slog("Network Disconnected", category: .network, level: .warning)
                    }
                }
            }
        }
        
        monitor.start(queue: queue)
        slog("Network monitoring started", category: .network)
    }
    
    /// Get a human-readable connection type string
    @MainActor
    var connectionTypeString: String {
        switch connectionType {
        case .wifi:
            return "WiFi"
        case .cellular:
            return "Cellular"
        case .wiredEthernet:
            return "Ethernet"
        default:
            return "Unknown"
        }
    }
    
    /// Check if we have a usable network connection for syncing
    @MainActor
    var canSync: Bool {
        // We can sync if connected and not in a constrained state
        // (unless user explicitly wants to sync on constrained connections)
        return isConnected && !isConstrained
    }
    
    /// Async method to wait for network connectivity with timeout
    @MainActor
    func waitForConnectivity(timeout: TimeInterval = 10) async -> Bool {
        if isConnected {
            return true
        }
        
        slog("Waiting for network connectivity", category: .network, data: ["timeout_seconds": timeout])
        
        // Use a simple polling approach instead of Combine to avoid concurrency issues
        let startTime = Date()
        let timeoutDate = startTime.addingTimeInterval(timeout)
        
        while Date() < timeoutDate {
            try? await Task.sleep(nanoseconds: 100_000_000) // Check every 0.1 seconds
            if isConnected {
                slog("Network connectivity restored", category: .network)
                return true
            }
        }

        slog("Network connectivity timeout", category: .network, level: .warning, data: ["timeout_seconds": timeout])
        return false
    }
    
    /// Test network connectivity by attempting to reach a reliable endpoint
    @MainActor
    func testConnectivity() async -> Bool {
        guard isConnected else {
            return false
        }
        
        // Try to reach a reliable endpoint (Apple's connectivity check)
        let url = URL(string: "https://www.apple.com/library/test/success.html")!
        var request = URLRequest(url: url)
        request.httpMethod = "HEAD"
        request.timeoutInterval = 5
        
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            if let httpResponse = response as? HTTPURLResponse {
                return httpResponse.statusCode == 200
            }
        } catch {
            slog("Connectivity test failed", category: .network, level: .warning, data: ["error": error.localizedDescription])
        }
        
        return false
    }
}