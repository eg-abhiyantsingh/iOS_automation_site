//
//  AppLogger.swift
//  Egalvanic PZ
//
//  Centralized logging service using Apple's os.Logger for structured local logging
//  and Sentry for remote error tracking. Single entry point for all logging.
//

import OSLog
import Sentry

// MARK: - Log Level

enum LogLevel {
    case debug   // Developer debugging only (stripped in release)
    case info    // Normal app lifecycle events
    case notice  // Important but expected events
    case error   // Recoverable errors
    case fault   // Critical / unrecoverable errors
}

// MARK: - Log Category

enum LogCategory: String {
    case auth       // Authentication flows
    case sync       // Sync operations
    case api        // API requests/responses
    case network    // Network connectivity
    case photo      // Photo uploads
    case task       // Task creation/updates
    case location   // Location services
    case database   // SwiftData operations
    case ui         // UI events
    case node       // Node/Edge operations
    case issue      // Issue operations
    case form       // Form operations
    case webBridge  // WebView bridge communication
    case background // Background processing
    case general    // General logs
}

// MARK: - AppLogger

struct AppLogger {

    private static let subsystem = Bundle.main.bundleIdentifier ?? "com.egalvanic.pz"

    // MARK: - Main Log Method

    /// Single entry point for all logging. Routes to os.Logger (local) and Sentry (remote).
    ///
    /// - Parameters:
    ///   - level: Log severity level
    ///   - message: The log message
    ///   - category: Category for filtering (default: .general)
    ///   - error: Optional error to capture in Sentry
    ///   - data: Additional context data for Sentry breadcrumbs
    ///   - file: Auto-captured source file
    ///   - function: Auto-captured function name
    ///   - line: Auto-captured line number
    static func log(
        _ level: LogLevel,
        _ message: String,
        category: LogCategory = .general,
        error: Error? = nil,
        data: [String: Any]? = nil,
        file: String = #fileID,
        function: String = #function,
        line: Int = #line
    ) {
        // 1. Skip debug logs in release builds
        #if !DEBUG
        if level == .debug { return }
        #endif

        // 2. Local structured logging via os.Logger
        let logger = Logger(subsystem: subsystem, category: category.rawValue)
        let formatted = "[\(file):\(line)] \(function) -> \(message)"

        switch level {
        case .debug:
            logger.debug("\(formatted, privacy: .public)")
        case .info:
            logger.info("\(formatted, privacy: .public)")
        case .notice:
            logger.notice("\(formatted, privacy: .public)")
        case .error:
            logger.error("\(formatted, privacy: .public)")
        case .fault:
            logger.fault("\(formatted, privacy: .public)")
        }

        // 3. Sentry breadcrumb (for all levels)
        let crumb = Breadcrumb(level: level.sentryLevel, category: category.rawValue)
        crumb.message = message
        crumb.timestamp = Date()
        if let data {
            crumb.data = data
        }
        SentrySDK.addBreadcrumb(crumb)

        // 4. Sentry event capture (errors and faults only)
        if level == .error || level == .fault {
            if let error {
                SentrySDK.capture(error: error) { scope in
                    scope.setContext(value: [
                        "message": message,
                        "file": file,
                        "function": function,
                        "line": line
                    ], key: category.rawValue)
                    if let data {
                        scope.setContext(value: data, key: "extra")
                    }
                }
            } else {
                SentrySDK.capture(message: message) { scope in
                    scope.setLevel(level.sentryLevel)
                    scope.setContext(value: [
                        "file": file,
                        "function": function,
                        "line": line
                    ], key: category.rawValue)
                    if let data {
                        scope.setContext(value: data, key: "extra")
                    }
                }
            }
        }
    }

    // MARK: - API Request/Response Logging

    /// Logs an outgoing API request with URL, method, headers, and pretty-printed JSON body.
    /// Debug-only — stripped in release builds.
    static func logRequest(_ request: URLRequest, file: String = #fileID, function: String = #function, line: Int = #line) {
        #if !DEBUG
        return
        #endif

        let method = request.httpMethod ?? "GET"
        let url = request.url?.absoluteString ?? "<no url>"

        var parts: [String] = []
        parts.append("→ \(method) \(url)")

        // Headers (mask Authorization token)
        if let headers = request.allHTTPHeaderFields, !headers.isEmpty {
            var masked: [String: String] = [:]
            for (key, value) in headers {
                if key == "Authorization", value.hasPrefix("Bearer ") {
                    let token = String(value.dropFirst(7))
                    if token.count > 10 {
                        masked[key] = "Bearer \(token.prefix(6))...\(token.suffix(4))"
                    } else {
                        masked[key] = value
                    }
                } else {
                    masked[key] = value
                }
            }
            if let headersData = try? JSONSerialization.data(withJSONObject: masked, options: [.prettyPrinted, .sortedKeys]),
               let headersJSON = String(data: headersData, encoding: .utf8) {
                parts.append("→ Headers: \(headersJSON)")
            }
        }

        // Body — pretty-print JSON
        if let body = request.httpBody, !body.isEmpty {
            parts.append("→ Body: \(prettyJSON(body))")
        }

        let message = parts.joined(separator: "\n")
        log(.debug, "API REQUEST\n\(message)", category: .api, file: file, function: function, line: line)
    }

    /// Logs an API response with status code, duration, and pretty-printed JSON body.
    /// Debug-only — stripped in release builds. Error responses are logged at .error level.
    static func logResponse(
        _ request: URLRequest,
        data: Data,
        httpResponse: HTTPURLResponse,
        duration: TimeInterval,
        file: String = #fileID,
        function: String = #function,
        line: Int = #line
    ) {
        let method = request.httpMethod ?? "GET"
        let url = request.url?.absoluteString ?? "<no url>"
        let status = httpResponse.statusCode
        let durationMs = String(format: "%.0fms", duration * 1000)
        let isError = !(200..<300).contains(status)

        var parts: [String] = []
        parts.append("← \(method) \(url)")
        parts.append("← Status: \(status) (\(durationMs))")

        // Response body — pretty-print JSON (truncate large responses)
        if !data.isEmpty {
            let bodyString = prettyJSON(data, maxLength: isError ? 5000 : 2000)
            parts.append("← Body: \(bodyString)")
        }

        let message = parts.joined(separator: "\n")
        let level: LogLevel = isError ? .error : .debug
        log(level, "API RESPONSE\n\(message)", category: .api, file: file, function: function, line: line)
    }

    // MARK: - JSON Formatting Helper

    /// Pretty-prints JSON data. Falls back to raw string if not valid JSON.
    private static func prettyJSON(_ data: Data, maxLength: Int = 5000) -> String {
        if let json = try? JSONSerialization.jsonObject(with: data),
           let pretty = try? JSONSerialization.data(withJSONObject: json, options: [.prettyPrinted, .sortedKeys]),
           var string = String(data: pretty, encoding: .utf8) {
            if string.count > maxLength {
                string = String(string.prefix(maxLength)) + "\n... (\(data.count) bytes total, truncated)"
            }
            return string
        }
        // Not valid JSON — return raw string
        let raw = String(data: data, encoding: .utf8) ?? "<\(data.count) bytes binary>"
        if raw.count > maxLength {
            return String(raw.prefix(maxLength)) + "... (\(data.count) bytes total, truncated)"
        }
        return raw
    }
}

// MARK: - LogLevel → SentryLevel Mapping

extension LogLevel {
    var sentryLevel: SentryLevel {
        switch self {
        case .debug:  return .debug
        case .info:   return .info
        case .notice: return .info
        case .error:  return .error
        case .fault:  return .fatal
        }
    }
}
