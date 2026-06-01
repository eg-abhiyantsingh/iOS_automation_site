//
//  IRSessionManager.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/3/25.
//

// MARK: - IR Session Manager (Environment Object)
import SwiftUI
import SwiftData

@MainActor
class IRSessionManager: ObservableObject {
    // MARK: - Singleton
    static let shared = IRSessionManager()
    
    @Published var selectedSessionId: UUID?
    
    // Store selection per SLD
    private var sessionSelections: [UUID: UUID] = [:]
    
    // Private initializer to enforce singleton pattern
    private init() {}
    
    func setSelectedSession(_ sessionId: UUID?, for sldId: UUID) {
        if let sessionId = sessionId {
            sessionSelections[sldId] = sessionId
        } else {
            sessionSelections.removeValue(forKey: sldId)
        }
        selectedSessionId = sessionId
    }
    
    func getSelectedSession(for sldId: UUID) -> UUID? {
        return sessionSelections[sldId]
    }
    
    func clearSelection(for sldId: UUID) {
        sessionSelections.removeValue(forKey: sldId)
        selectedSessionId = nil
    }
}
