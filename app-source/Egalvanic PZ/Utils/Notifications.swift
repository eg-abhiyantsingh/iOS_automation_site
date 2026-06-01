//
//  No.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 7/17/25.
//

import Foundation

extension Notification.Name {
    /// Matches JS handler `window.webkit.messageHandlers.nodeClicked.postMessage(...)`
    static let nodeClicked           = Notification.Name("WebViewNodeClicked")
    /// Matches JS handler `window.webkit.messageHandlers.nodePositionChanged.postMessage(...)`
    static let nodePositionChanged   = Notification.Name("nodePositionChanged")
    /// Matches JS handler `window.webkit.messageHandlers.nodeParentChanged.postMessage(...)`
    static let nodeParentChanged     = Notification.Name("nodeParentChanged")
    static let graphUpdated          = Notification.Name("graphUpdate")
}
