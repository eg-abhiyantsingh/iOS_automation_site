//
//  SentryLogger.swift
//  Egalvanic PZ
//
//  Centralized logging service wrapping Sentry for comprehensive observability.
//  Replaces print() statements with Sentry breadcrumbs for debugging.
//

import Foundation
import Sentry

/// Centralized logging service that wraps Sentry for comprehensive error tracking and debugging.
/// Use `slog()` as a drop-in replacement for `print()` to capture breadcrumbs in Sentry.
final class SentryLogger {
    static let shared = SentryLogger()

    private init() {}

    // MARK: - Log Levels

    enum Level: String {
        case debug
        case info
        case warning
        case error

        var sentryLevel: SentryLevel {
            switch self {
            case .debug: return .debug
            case .info: return .info
            case .warning: return .warning
            case .error: return .error
            }
        }
    }

    // MARK: - Categories (for filtering in Sentry dashboard)

    enum Category: String {
        case auth       // Authentication flows
        case sync       // Sync operations
        case api        // API requests/responses
        case network    // Network connectivity
        case photo      // Photo uploads
        case task       // Task creation/updates
        case location   // Location services
        case database   // SwiftData operations
        case general    // General logs
        case ui         // UI events
    }

    // MARK: - Breadcrumb Logging (replaces print)

    /// Log a message as a Sentry breadcrumb.
    /// Use this as a replacement for print() to capture debugging trails in Sentry.
    ///
    /// - Parameters:
    ///   - message: The log message
    ///   - category: Category for filtering (default: .general)
    ///   - level: Log level (default: .info)
    ///   - data: Additional context data
    func log(
        _ message: String,
        category: Category = .general,
        level: Level = .info,
        data: [String: Any]? = nil
    ) {
        let crumb = Breadcrumb(level: level.sentryLevel, category: category.rawValue)
        crumb.message = message
        crumb.data = data
        crumb.timestamp = Date()
        SentrySDK.addBreadcrumb(crumb)

        // Keep console logging in debug builds
        #if DEBUG
        let prefix = levelPrefix(level)
        let dataStr = data != nil ? " | \(data!)" : ""
        print("\(prefix) [\(category.rawValue.uppercased())] \(message)\(dataStr)")
        #endif
    }

    private func levelPrefix(_ level: Level) -> String {
        switch level {
        case .debug: return "🔍"
        case .info: return "ℹ️"
        case .warning: return "⚠️"
        case .error: return "❌"
        }
    }

    // MARK: - Error Capture

    /// Capture an error with context to Sentry.
    ///
    /// - Parameters:
    ///   - error: The error to capture
    ///   - category: Category for context
    ///   - context: Additional context data
    ///   - tags: Tags for filtering in Sentry dashboard
    func captureError(
        _ error: Error,
        category: Category,
        context: [String: Any]? = nil,
        tags: [String: String]? = nil
    ) {
        SentrySDK.capture(error: error) { scope in
            scope.setContext(value: context ?? [:], key: category.rawValue)

            tags?.forEach { key, value in
                scope.setTag(value: value, key: key)
            }

            // Always add sync state context
            self.addSyncContext(to: scope)
        }

        // Also log as breadcrumb for trail
        log("Error captured: \(error.localizedDescription)",
            category: category,
            level: .error,
            data: context)
    }

    /// Capture a message as a Sentry event (not just breadcrumb).
    /// Use for important events you want to see in the Issues view.
    func captureMessage(
        _ message: String,
        level: Level = .info,
        category: Category,
        context: [String: Any]? = nil,
        tags: [String: String]? = nil
    ) {
        SentrySDK.capture(message: message) { scope in
            scope.setLevel(level.sentryLevel)
            scope.setContext(value: context ?? [:], key: category.rawValue)
            tags?.forEach { scope.setTag(value: $1, key: $0) }
            self.addSyncContext(to: scope)
        }
    }

    // MARK: - Sync-Specific Capture

    /// Capture a sync failure with comprehensive context.
    /// This is the key method for debugging "sync stuck" issues.
    /// Must be called from MainActor since it accesses NetworkState and NetworkReachability.
    ///
    /// - Parameters:
    ///   - target: The sync target type (node, edge, photo, etc.)
    ///   - operation: The operation type (create, update, delete)
    ///   - entityId: The entity ID that failed to sync
    ///   - error: The error that occurred
    ///   - retryCount: Number of retry attempts made
    ///   - httpStatusCode: HTTP status code if available
    ///   - queueItemId: The sync queue item ID
    @MainActor
    func captureSyncFailure(
        target: SyncTarget,
        operation: SyncOperation,
        entityId: UUID?,
        error: Error,
        retryCount: Int,
        httpStatusCode: Int?,
        queueItemId: UUID? = nil,
        userId: UUID? = nil,
        siteId: UUID? = nil
    ) {
        // ZP-1847: include the originating user + site so support can
        // disambiguate cross-site offline issues. After site-switch / logout
        // the active SLD may differ from the snapshot's siteId — both are
        // useful in the breadcrumb.
        let context: [String: Any] = [
            "target": "\(target)",
            "operation": "\(operation)",
            "entity_id": entityId?.uuidString ?? "nil",
            "retry_count": retryCount,
            "http_status_code": httpStatusCode ?? -1,
            "queue_item_id": queueItemId?.uuidString ?? "nil",
            "queue_count": NetworkState.shared.syncQueueCount,
            "network_connected": NetworkReachability.shared.isConnected,
            "network_mode": "\(NetworkState.shared.mode)",
            "snapshot_user_id": userId?.uuidString ?? "nil",
            "snapshot_site_id": siteId?.uuidString ?? "nil",
            "active_site_id": AppStateManager.shared.activeSLDId.uuidString
        ]

        let tags: [String: String] = [
            "sync_target": "\(target)",
            "sync_operation": "\(operation)",
            "error_type": categorizeError(error)
        ]

        captureError(error, category: .sync, context: context, tags: tags)
    }

    /// Categorize error for better grouping in Sentry dashboard.
    private func categorizeError(_ error: Error) -> String {
        if let syncError = error as? SyncError {
            switch syncError {
            case .missingEntity: return "missing_entity"
            case .missingEntityWithMessage: return "missing_entity"
            case .timeout: return "timeout"
            case .fileNotFound: return "file_not_found"
            case .fileTooLarge: return "file_too_large"
            }
        }

        if let urlError = error as? URLError {
            switch urlError.code {
            case .notConnectedToInternet: return "no_internet"
            case .networkConnectionLost: return "connection_lost"
            case .timedOut: return "network_timeout"
            case .userAuthenticationRequired: return "auth_required"
            default:
                if (400..<500).contains(urlError.code.rawValue) {
                    return "client_error_\(urlError.code.rawValue)"
                }
                if (500..<600).contains(urlError.code.rawValue) {
                    return "server_error_\(urlError.code.rawValue)"
                }
                return "url_error_\(urlError.code.rawValue)"
            }
        }

        if let nsError = error as NSError? {
            if (400..<500).contains(nsError.code) {
                return "client_error_\(nsError.code)"
            }
            if (500..<600).contains(nsError.code) {
                return "server_error_\(nsError.code)"
            }
        }

        return "unknown_error"
    }

    // MARK: - User Context

    /// Set user context after successful login.
    /// This associates all subsequent errors with the user.
    func setUser(id: String, email: String?, companyId: String?, subdomain: String?) {
        let user = User(userId: id)
        user.email = email
        user.data = [
            "company_id": companyId ?? "unknown",
            "subdomain": subdomain ?? "unknown"
        ]
        SentrySDK.setUser(user)

        log("User context set", category: .auth, data: [
            "user_id": id,
            "company_id": companyId ?? "unknown"
        ])
    }

    /// Clear user context on logout.
    func clearUser() {
        SentrySDK.setUser(nil)
        log("User context cleared", category: .auth)
    }

    // MARK: - API Error Capture

    /// Capture an API error with request context.
    func captureAPIError(
        _ error: Error,
        endpoint: String,
        method: String,
        statusCode: Int?,
        responseBody: String? = nil
    ) {
        let context: [String: Any] = [
            "endpoint": endpoint,
            "method": method,
            "status_code": statusCode ?? -1,
            "response_body": responseBody ?? "nil"
        ]

        let tags: [String: String] = [
            "api_endpoint": endpoint,
            "http_method": method,
            "error_type": categorizeError(error)
        ]

        captureError(error, category: .api, context: context, tags: tags)
    }

    // MARK: - Photo Upload Error

    /// Capture a photo upload failure with detailed context.
    func capturePhotoUploadFailure(
        photoId: UUID,
        filename: String?,
        nodeId: UUID?,
        error: Error,
        fileExists: Bool,
        fileSize: Int64?
    ) {
        let context: [String: Any] = [
            "photo_id": photoId.uuidString,
            "filename": filename ?? "nil",
            "node_id": nodeId?.uuidString ?? "nil",
            "file_exists": fileExists,
            "file_size_bytes": fileSize ?? -1
        ]

        let tags: [String: String] = [
            "sync_target": "photo",
            "file_exists": "\(fileExists)",
            "error_type": categorizeError(error)
        ]

        captureError(error, category: .photo, context: context, tags: tags)
    }

    // MARK: - Testing

    /// Test function to verify Sentry is working.
    /// Call this to send a test event with breadcrumbs to Sentry.
    /// Usage: SentryLogger.shared.testSentryConnection()
    @MainActor
    func testSentryConnection() {
        // Add some test breadcrumbs
        log("Test breadcrumb 1 - User opened app", category: .general)
        log("Test breadcrumb 2 - User logged in", category: .auth)
        log("Test breadcrumb 3 - Starting sync", category: .sync)
        log("Test breadcrumb 4 - Network connected", category: .network)

        // Capture a test message (this creates an actual event in Sentry)
        captureMessage(
            "Sentry Test Event - Connection Verified",
            level: .info,
            category: .general,
            context: [
                "test": true,
                "timestamp": "\(Date())",
                "queue_count": NetworkState.shared.syncQueueCount,
                "is_online": NetworkState.shared.isOnline
            ],
            tags: ["test_event": "true"]
        )

        log("Test event sent to Sentry - check your dashboard!", category: .general)
    }

    // MARK: - Private Helpers

    /// Add sync state context to all captured errors.
    private func addSyncContext(to scope: Scope) {
        scope.setContext(value: [
            "queue_count": NetworkState.shared.syncQueueCount,
            "is_syncing": NetworkState.shared.isSyncing,
            "network_mode": "\(NetworkState.shared.mode)",
            "is_online": NetworkState.shared.isOnline
        ], key: "sync_state")
    }
}

// MARK: - Global Convenience Function (Legacy Shim)

/// Legacy shim — routes through AppLogger internally.
/// Kept for backward compatibility during migration. New code should use AppLogger.log() directly.
///
/// Usage (prefer AppLogger.log instead):
/// ```swift
/// AppLogger.log(.info, "Starting sync flush", category: .sync, data: ["count": count])
/// ```
func slog(
    _ message: String,
    category: SentryLogger.Category = .general,
    level: SentryLogger.Level = .info,
    data: [String: Any]? = nil
) {
    AppLogger.log(
        level.toLogLevel,
        message,
        category: category.toLogCategory,
        data: data
    )
}

// MARK: - Legacy Type Mapping

extension SentryLogger.Level {
    var toLogLevel: LogLevel {
        switch self {
        case .debug:   return .debug
        case .info:    return .info
        case .warning: return .notice
        case .error:   return .error
        }
    }
}

extension SentryLogger.Category {
    var toLogCategory: LogCategory {
        switch self {
        case .auth:     return .auth
        case .sync:     return .sync
        case .api:      return .api
        case .network:  return .network
        case .photo:    return .photo
        case .task:     return .task
        case .location: return .location
        case .database: return .database
        case .general:  return .general
        case .ui:       return .ui
        }
    }
}
