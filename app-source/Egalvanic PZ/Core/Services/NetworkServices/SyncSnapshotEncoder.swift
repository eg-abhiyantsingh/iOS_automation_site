import Foundation

/// Captures the JSON snapshot stored on a `SyncQueueItem` at enqueue time.
///
/// The snapshot is the literal HTTP request body the corresponding
/// `APIClient.create*` / `update*` call would have sent. At flush time the
/// bytes are replayed via `APIClient.replaySnapshotPayload(...)`. Capturing
/// bytes (not DTOs) makes wire-format drift impossible — the snapshot IS the
/// wire payload.
///
/// Returning nil is safe: the flush path falls back to the legacy
/// entity-fetch route, which is the pre-ZP-1847 behavior.
@MainActor
enum SyncSnapshotEncoder {

    /// Build the request body that would be sent for this op. Returns nil
    /// for mappings (handled via `mappingDataJSON`), for ops without a body
    /// (attachment hard-delete), for unsupported targets, or on any error
    /// (logged at warning level so the legacy path can take over).
    static func encode(_ op: SyncOp) -> Data? {
        guard !op.target.isMapping else { return nil }
        do {
            return try APIClient.shared.buildSnapshotPayload(for: op)
        } catch {
            slog("SyncSnapshotEncoder: build failed",
                 category: .sync, level: .warning,
                 data: ["target": "\(op.target)",
                        "operation": "\(op.operation)",
                        "error": "\(error)"])
            return nil
        }
    }
}
