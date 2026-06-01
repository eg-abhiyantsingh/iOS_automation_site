import Foundation

/// Preserves photo and attachment binaries for queued offline uploads.
///
/// Files are copied (not moved) into a protected directory at enqueue time so
/// the queued upload survives a site switch — `SLDService.clearSLDs()` wipes
/// SwiftData entities but must not touch this directory while items are still
/// pending. The original file is left in place so in-app rendering keeps working.
///
/// On successful upload, `releaseFile(for:)` deletes the protected copy. On
/// app launch (and after `clearSLDs`), `gcOrphans(currentQueueItemIds:)` sweeps
/// any subdirectories that no longer correspond to a pending queue item.
final class SyncFileManager {
    static let shared = SyncFileManager()

    /// Exposed so callers reconstructing relative paths (migration, self-heal)
    /// don't have to repeat the literal — keeps the directory name in one place.
    static let directoryName = "sync_pending_attachments"
    private let fileManager: FileManager

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager
    }

    /// Absolute URL of the protected root (created lazily).
    var protectedRootURL: URL {
        let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        return documents.appendingPathComponent(Self.directoryName, isDirectory: true)
    }

    /// Copy `sourceURL` into the protected directory, keyed by `queueItemId`.
    /// Returns the URL of the preserved copy, or nil if the source does not exist.
    @discardableResult
    func preserveFile(at sourceURL: URL, queueItemId: UUID) throws -> URL? {
        guard fileManager.fileExists(atPath: sourceURL.path) else {
            slog("SyncFileManager: source missing — skipping preserve",
                 category: .sync, level: .warning,
                 data: ["queueItemId": queueItemId.uuidString,
                        "source": sourceURL.path])
            return nil
        }

        let itemDir = protectedRootURL.appendingPathComponent(queueItemId.uuidString, isDirectory: true)
        try ensureDirectory(at: itemDir)

        let destURL = itemDir.appendingPathComponent(sourceURL.lastPathComponent)

        // If a copy already exists for this queue item (e.g. coalesced UPDATE),
        // overwrite so the snapshot file matches the latest entity bytes.
        if fileManager.fileExists(atPath: destURL.path) {
            try? fileManager.removeItem(at: destURL)
        }

        try fileManager.copyItem(at: sourceURL, to: destURL)

        slog("SyncFileManager: preserved file",
             category: .sync,
             data: ["queueItemId": queueItemId.uuidString,
                    "dest": destURL.path,
                    "size": (try? fileManager.attributesOfItem(atPath: destURL.path)[.size] as? Int) ?? 0])

        return destURL
    }

    /// Delete the protected copy associated with `queueItemId`. Idempotent.
    func releaseFile(for queueItemId: UUID) {
        let itemDir = protectedRootURL.appendingPathComponent(queueItemId.uuidString, isDirectory: true)
        guard fileManager.fileExists(atPath: itemDir.path) else { return }
        do {
            try fileManager.removeItem(at: itemDir)
            slog("SyncFileManager: released file",
                 category: .sync, data: ["queueItemId": queueItemId.uuidString])
        } catch {
            slog("SyncFileManager: release failed",
                 category: .sync, level: .warning,
                 data: ["queueItemId": queueItemId.uuidString, "error": "\(error)"])
        }
    }

    /// Delete every subdirectory under the protected root whose name does not
    /// match a still-pending queue item ID. Safe to call repeatedly; called
    /// after `clearSLDs` and on app launch.
    func gcOrphans(currentQueueItemIds: Set<UUID>) {
        let root = protectedRootURL
        guard fileManager.fileExists(atPath: root.path) else { return }

        let allowedNames = Set(currentQueueItemIds.map { $0.uuidString })

        do {
            let entries = try fileManager.contentsOfDirectory(at: root, includingPropertiesForKeys: nil)
            var swept = 0
            for entry in entries where !allowedNames.contains(entry.lastPathComponent) {
                try? fileManager.removeItem(at: entry)
                swept += 1
            }
            if swept > 0 {
                slog("SyncFileManager: GC swept \(swept) orphan directories",
                     category: .sync,
                     data: ["remaining_pending": currentQueueItemIds.count])
            }
        } catch {
            slog("SyncFileManager: GC failed",
                 category: .sync, level: .warning,
                 data: ["error": "\(error)"])
        }
    }

    /// Resolve the URL of the preserved file for a queue item. Returns nil if
    /// the directory exists but contains no file (corrupt state) or if the
    /// directory is missing.
    func resolvePreservedFileURL(for queueItemId: UUID) -> URL? {
        let itemDir = protectedRootURL.appendingPathComponent(queueItemId.uuidString, isDirectory: true)
        guard fileManager.fileExists(atPath: itemDir.path) else { return nil }
        let entries = (try? fileManager.contentsOfDirectory(at: itemDir, includingPropertiesForKeys: nil)) ?? []
        return entries.first
    }

    /// Documents-relative path of the preserved file for a queue item, e.g.
    /// `sync_pending_attachments/<queueItemId>/<filename>`. Returns nil when
    /// the preserved file is missing.
    ///
    /// Storing this form (instead of the absolute URL.path) keeps the queue
    /// row valid across app updates and backup restores — iOS rebinds the
    /// Data container UUID, so a persisted absolute path like
    /// `/var/mobile/Containers/Data/Application/<UUID>/Documents/…` becomes
    /// stale on the next launch even though the file itself is still on disk
    /// under the new container. See Apple TN2406 and ZP-2-fix-2026-05.
    func relativePathForPreservedFile(queueItemId: UUID) -> String? {
        guard let absoluteURL = resolvePreservedFileURL(for: queueItemId) else { return nil }
        return "\(Self.directoryName)/\(queueItemId.uuidString)/\(absoluteURL.lastPathComponent)"
    }

    /// Resolve a persisted `SyncQueueItem.photoFilePath` to a current,
    /// absolute file URL. Accepts both forms for backward compatibility:
    ///
    /// 1. **Relative** (`sync_pending_attachments/<id>/<filename>`) — new
    ///    format. Resolved against the current Documents directory.
    /// 2. **Absolute** (`/var/mobile/Containers/Data/Application/<UUID>/…`) —
    ///    legacy format written by v1.33 and earlier. Tried as-is first; if
    ///    the file is missing (container UUID changed on app update), the
    ///    `sync_pending_attachments/…` suffix is extracted and re-resolved
    ///    against the current Documents directory.
    ///
    /// Returns the URL if a file exists at the resolved location; nil
    /// otherwise. `recoveredFromLegacyPath` is set to true when a stale
    /// absolute path was successfully recovered — callers can use this to
    /// rewrite the queue row to relative form.
    func resolveStoredPath(_ storedPath: String) -> (url: URL, recoveredFromLegacyPath: Bool)? {
        guard let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            slog("SyncFileManager: Documents directory unavailable",
                 category: .sync, level: .error,
                 data: ["stored": storedPath])
            return nil
        }

        if storedPath.hasPrefix("/") {
            // Legacy absolute path. Try as-is — if the container UUID didn't
            // move, this is the cheap happy path.
            if fileManager.fileExists(atPath: storedPath) {
                return (URL(fileURLWithPath: storedPath), false)
            }

            // Stale container UUID. Extract the `sync_pending_attachments/…`
            // suffix and re-resolve against the current Documents directory.
            let marker = "/\(Self.directoryName)/"
            guard let markerRange = storedPath.range(of: marker) else {
                slog("SyncFileManager: legacy path has no protected-dir marker",
                     category: .sync, level: .warning,
                     data: ["stored": storedPath])
                return nil
            }
            let relative = String(storedPath[markerRange.lowerBound...].dropFirst())
            let recovered = documents.appendingPathComponent(relative)
            if fileManager.fileExists(atPath: recovered.path) {
                slog("SyncFileManager: recovered stale legacy path",
                     category: .sync,
                     data: ["stored": storedPath, "recovered": recovered.path])
                return (recovered, true)
            }
            slog("SyncFileManager: file truly missing after recovery attempt",
                 category: .sync, level: .warning,
                 data: ["stored": storedPath, "tried": recovered.path])
            return nil
        }

        // Relative path. Resolve against current Documents directory.
        let resolved = documents.appendingPathComponent(storedPath)
        if fileManager.fileExists(atPath: resolved.path) {
            return (resolved, false)
        }
        slog("SyncFileManager: relative path resolved but file missing",
             category: .sync, level: .warning,
             data: ["stored": storedPath, "tried": resolved.path])
        return nil
    }

    private func ensureDirectory(at url: URL) throws {
        if !fileManager.fileExists(atPath: url.path) {
            try fileManager.createDirectory(at: url, withIntermediateDirectories: true)
        }
    }
}
