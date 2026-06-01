//
//  DebugOverlay.swift
//  Egalvanic PZ
//
//  Simple debug overlay to show logs in TestFlight builds
//

import SwiftUI

class DebugLogger: ObservableObject {
    static let shared = DebugLogger()

    @Published var logs: [String] = []
    @Published var isVisible: Bool = false

    private init() {}

    func log(_ message: String) {
        let timestamp = DateFormatter.localizedString(from: Date(), dateStyle: .none, timeStyle: .medium)
        let logMessage = "\(timestamp): \(message)"

        DispatchQueue.main.async {
            self.logs.insert(logMessage, at: 0)
            if self.logs.count > 100 {
                self.logs.removeLast()
            }
        }

        AppLogger.log(.debug, message)  // Still log to console
    }

    func toggle() {
        isVisible.toggle()
    }

    func clear() {
        logs.removeAll()
    }
}

struct DebugOverlay: View {
    @ObservedObject var logger = DebugLogger.shared

    var body: some View {
        if logger.isVisible {
            VStack(spacing: 0) {
                // Header
                HStack {
                    Text(AppStrings.Supporting.debugLog)
                        .font(.headline)
                        .foregroundColor(.white)

                    Spacer()

                    Button(AppStrings.Common.clear) {
                        logger.clear()
                    }
                    .foregroundColor(.white)

                    Button(AppStrings.CommonExtra.close) {
                        logger.toggle()
                    }
                    .foregroundColor(.white)
                }
                .padding()
                .background(Color.black.opacity(0.9))

                // Log list
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 4) {
                        ForEach(logger.logs, id: \.self) { log in
                            Text(log)
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(.green)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                        }
                    }
                }
                .background(Color.black.opacity(0.8))
            }
            .frame(maxWidth: .infinity, maxHeight: 300)
            .transition(.move(edge: .bottom))
        }
    }
}

// Add this to any view to enable the debug button
struct DebugButton: View {
    var body: some View {
        Button(action: {
            DebugLogger.shared.toggle()
        }) {
            Image(systemName: "ladybug.fill")
                .foregroundColor(.red)
                .padding(8)
                .background(Color.white.opacity(0.8))
                .clipShape(Circle())
        }
    }
}
