//
//  SldViewerAssetManager.swift
//  Egalvanic PZ
//
//  Manages downloading, verifying, and extracting OTA SLD viewer updates.
//  Downloaded assets take priority over bundled assets in the Flow/ directory.
//

import Foundation
import CryptoKit
import Compression

final class SldViewerAssetManager {
    static let shared = SldViewerAssetManager()
    private init() {}

    // MARK: - UserDefaults Keys

    private enum Keys {
        static let checksum = "sld_viewer_checksum"
        static let version = "sld_viewer_version"
    }

    // MARK: - Directory Paths

    private var assetsDirectory: URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
            .appendingPathComponent("sld-viewer")
    }

    private var tempDirectory: URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
            .appendingPathComponent("sld-viewer-temp")
    }

    private var tempZipURL: URL {
        FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
            .appendingPathComponent("sld-viewer-update.zip")
    }

    // MARK: - Public API

    /// Returns the stored SLD viewer version string, or nil if no version has been downloaded.
    func getStoredVersion() -> String? {
        UserDefaults.standard.string(forKey: Keys.version)
    }

    /// Returns the downloaded assets directory if it contains a valid index.html, otherwise nil.
    func getDownloadedAssetsDir() -> URL? {
        let indexFile = assetsDirectory.appendingPathComponent("index.html")
        if FileManager.default.fileExists(atPath: indexFile.path) {
            return assetsDirectory
        }
        return nil
    }

    /// Main entry point: checks for updates and downloads if a new version is available.
    func checkAndUpdate() async {
        // Skip if not logged in
        guard await AuthService.shared.getAccessToken() != nil else {
            AppLogger.log(.debug, "[SldViewerAssetManager] Skipping update check — not logged in", category: .api)
            return
        }

        do {
            let response = try await APIClient.shared.fetchSldViewerLatest()

            guard response.success, let data = response.data else {
                AppLogger.log(.debug, "[SldViewerAssetManager] No SLD viewer update available", category: .api)
                return
            }

            // Compare checksums — skip if already up to date
            let storedChecksum = UserDefaults.standard.string(forKey: Keys.checksum)
            if storedChecksum == data.checksum {
                AppLogger.log(.debug, "[SldViewerAssetManager] Already up to date (v\(data.sldViewerVersion))", category: .api)
                return
            }

            AppLogger.log(.info, "[SldViewerAssetManager] New SLD viewer available: v\(data.sldViewerVersion)", category: .api)

            let success = await downloadAndExtract(
                urlString: data.sldViewerDownloadUrl,
                expectedChecksum: data.checksum
            )

            if success {
                UserDefaults.standard.set(data.checksum, forKey: Keys.checksum)
                UserDefaults.standard.set(data.sldViewerVersion, forKey: Keys.version)
                AppLogger.log(.info, "[SldViewerAssetManager] Successfully updated to v\(data.sldViewerVersion)", category: .api)
            } else {
                AppLogger.log(.error, "[SldViewerAssetManager] Failed to download/extract update", category: .api)
            }
        } catch {
            AppLogger.log(.error, "[SldViewerAssetManager] Update check failed: \(error)", category: .api)
        }
    }

    /// Clears downloaded assets and stored version info. Called on logout.
    func clearDownloadedAssets() {
        let fm = FileManager.default
        try? fm.removeItem(at: assetsDirectory)
        try? fm.removeItem(at: tempDirectory)
        try? fm.removeItem(at: tempZipURL)
        UserDefaults.standard.removeObject(forKey: Keys.checksum)
        UserDefaults.standard.removeObject(forKey: Keys.version)
        AppLogger.log(.info, "[SldViewerAssetManager] Cleared downloaded assets", category: .api)
    }

    // MARK: - Download & Extract

    private func downloadAndExtract(urlString: String, expectedChecksum: String) async -> Bool {
        guard let url = URL(string: urlString) else {
            AppLogger.log(.error, "[SldViewerAssetManager] Invalid download URL", category: .api)
            return false
        }

        let fm = FileManager.default

        do {
            // 1. Download zip (plain URLSession, no auth — presigned S3 URL)
            let config = URLSessionConfiguration.default
            config.timeoutIntervalForRequest = 300
            config.timeoutIntervalForResource = 300
            let session = URLSession(configuration: config)

            let (tempFileURL, response) = try await session.download(from: url)

            guard let httpResponse = response as? HTTPURLResponse,
                  (200..<300).contains(httpResponse.statusCode) else {
                AppLogger.log(.error, "[SldViewerAssetManager] Download failed with status: \((response as? HTTPURLResponse)?.statusCode ?? -1)", category: .api)
                return false
            }

            // Move downloaded file to our temp zip path
            try? fm.removeItem(at: tempZipURL)
            try fm.moveItem(at: tempFileURL, to: tempZipURL)

            // 2. Verify SHA-256 checksum
            let checksumHex = expectedChecksum.hasPrefix("sha256:")
                ? String(expectedChecksum.dropFirst(7))
                : expectedChecksum

            let fileData = try Data(contentsOf: tempZipURL)
            let computedHash = SHA256.hash(data: fileData)
            let computedHex = computedHash.compactMap { String(format: "%02x", $0) }.joined()

            guard computedHex == checksumHex else {
                AppLogger.log(.error, "[SldViewerAssetManager] Checksum mismatch: expected \(checksumHex), got \(computedHex)", category: .api)
                try? fm.removeItem(at: tempZipURL)
                return false
            }

            // 3. Extract to temp directory
            try? fm.removeItem(at: tempDirectory)
            try fm.createDirectory(at: tempDirectory, withIntermediateDirectories: true)

            guard extractZip(from: tempZipURL, to: tempDirectory) else {
                AppLogger.log(.error, "[SldViewerAssetManager] Zip extraction failed", category: .api)
                try? fm.removeItem(at: tempDirectory)
                try? fm.removeItem(at: tempZipURL)
                return false
            }

            // 4. Merge bundled flow files (top-level only, excluding assets/ subdirectory)
            copyBundledFlowFiles(to: tempDirectory)

            // 5. Rewrite index.html asset paths
            updateIndexHtmlPaths(in: tempDirectory)

            // 6. Validate index.html exists
            let indexPath = tempDirectory.appendingPathComponent("index.html")
            guard fm.fileExists(atPath: indexPath.path) else {
                AppLogger.log(.error, "[SldViewerAssetManager] index.html not found after extraction", category: .api)
                try? fm.removeItem(at: tempDirectory)
                try? fm.removeItem(at: tempZipURL)
                return false
            }

            // 7. Atomic swap
            try? fm.removeItem(at: assetsDirectory)
            try fm.moveItem(at: tempDirectory, to: assetsDirectory)

            // 8. Cleanup
            try? fm.removeItem(at: tempZipURL)

            return true
        } catch {
            AppLogger.log(.error, "[SldViewerAssetManager] Download/extract error: \(error)", category: .api)
            try? fm.removeItem(at: tempDirectory)
            try? fm.removeItem(at: tempZipURL)
            return false
        }
    }

    // MARK: - Zip Extraction

    private func extractZip(from zipURL: URL, to destinationURL: URL) -> Bool {
        let fm = FileManager.default

        guard let data = try? Data(contentsOf: zipURL) else { return false }

        return data.withUnsafeBytes { (rawBuffer: UnsafeRawBufferPointer) -> Bool in
            guard let baseAddress = rawBuffer.baseAddress else { return false }
            let bytes = baseAddress.assumingMemoryBound(to: UInt8.self)
            let count = rawBuffer.count

            // 1. Find End of Central Directory record (search backwards for PK\5\6)
            guard let eocdOffset = findEOCDOffset(bytes: bytes, count: count) else {
                AppLogger.log(.error, "[SldViewerAssetManager] Could not find End of Central Directory", category: .api)
                return false
            }

            // 2. Parse EOCD to get central directory offset and entry count
            let cdEntryCount = Int(readUInt16(bytes, eocdOffset + 10))
            let cdOffset = Int(readUInt32(bytes, eocdOffset + 16))

            guard cdOffset < count else { return false }

            // 3. Walk central directory entries (which always have correct sizes)
            var cdPos = cdOffset
            for _ in 0..<cdEntryCount {
                guard cdPos + 46 <= count else { return false }

                // Verify central directory signature: PK\1\2
                guard bytes[cdPos] == 0x50, bytes[cdPos + 1] == 0x4B,
                      bytes[cdPos + 2] == 0x01, bytes[cdPos + 3] == 0x02 else { return false }

                let compressionMethod = readUInt16(bytes, cdPos + 10)
                let compressedSize = Int(readUInt32(bytes, cdPos + 20))
                let uncompressedSize = Int(readUInt32(bytes, cdPos + 24))
                let fileNameLength = Int(readUInt16(bytes, cdPos + 28))
                let extraFieldLength = Int(readUInt16(bytes, cdPos + 30))
                let fileCommentLength = Int(readUInt16(bytes, cdPos + 32))
                let localHeaderOffset = Int(readUInt32(bytes, cdPos + 42))

                let nameStart = cdPos + 46
                guard nameStart + fileNameLength <= count else { return false }

                let fileNameData = Data(bytes: bytes + nameStart, count: fileNameLength)
                guard let fileName = String(data: fileNameData, encoding: .utf8) else {
                    cdPos = nameStart + fileNameLength + extraFieldLength + fileCommentLength
                    continue
                }

                // Advance to next central directory entry
                cdPos = nameStart + fileNameLength + extraFieldLength + fileCommentLength

                // 4. Locate file data via the local header
                guard localHeaderOffset + 30 <= count else { return false }
                let localNameLength = Int(readUInt16(bytes, localHeaderOffset + 26))
                let localExtraLength = Int(readUInt16(bytes, localHeaderOffset + 28))
                let dataStart = localHeaderOffset + 30 + localNameLength + localExtraLength
                let dataEnd = dataStart + compressedSize

                guard dataEnd <= count else { return false }

                let fileURL = destinationURL.appendingPathComponent(fileName)

                // Zip-slip protection
                guard fileURL.standardizedFileURL.path.hasPrefix(destinationURL.standardizedFileURL.path) else {
                    AppLogger.log(.error, "[SldViewerAssetManager] Zip-slip detected: \(fileName)", category: .api)
                    return false
                }

                if fileName.hasSuffix("/") {
                    // Directory entry
                    try? fm.createDirectory(at: fileURL, withIntermediateDirectories: true)
                } else {
                    // File entry
                    try? fm.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)

                    if compressionMethod == 0 {
                        // Stored (no compression)
                        let fileData = Data(bytes: bytes + dataStart, count: compressedSize)
                        try? fileData.write(to: fileURL)
                    } else if compressionMethod == 8 {
                        // Deflate
                        let compressedData = Data(bytes: bytes + dataStart, count: compressedSize)
                        if let decompressed = decompressDeflate(compressedData, expectedSize: uncompressedSize) {
                            try? decompressed.write(to: fileURL)
                        } else {
                            AppLogger.log(.error, "[SldViewerAssetManager] Failed to decompress: \(fileName)", category: .api)
                            return false
                        }
                    } else {
                        AppLogger.log(.error, "[SldViewerAssetManager] Unsupported compression method \(compressionMethod) for: \(fileName)", category: .api)
                        return false
                    }
                }
            }

            return true
        }
    }

    /// Search backwards from end of file for the End of Central Directory signature (PK\5\6)
    private func findEOCDOffset(bytes: UnsafePointer<UInt8>, count: Int) -> Int? {
        // EOCD is at least 22 bytes; search backwards up to 65KB (max comment size)
        let searchLimit = min(count, 65557)
        var pos = count - 22
        let lowerBound = count - searchLimit
        while pos >= lowerBound {
            if bytes[pos] == 0x50, bytes[pos + 1] == 0x4B,
               bytes[pos + 2] == 0x05, bytes[pos + 3] == 0x06 {
                return pos
            }
            pos -= 1
        }
        return nil
    }

    private func readUInt16(_ bytes: UnsafePointer<UInt8>, _ offset: Int) -> UInt16 {
        UInt16(bytes[offset]) | (UInt16(bytes[offset + 1]) << 8)
    }

    private func readUInt32(_ bytes: UnsafePointer<UInt8>, _ offset: Int) -> UInt32 {
        UInt32(bytes[offset]) | (UInt32(bytes[offset + 1]) << 8) | (UInt32(bytes[offset + 2]) << 16) | (UInt32(bytes[offset + 3]) << 24)
    }

    /// Decompress deflate-compressed data using the Compression framework
    private func decompressDeflate(_ data: Data, expectedSize: Int) -> Data? {
        // Use a buffer that's at least the expected size, with extra room
        let bufferSize = max(expectedSize * 2, 4096)
        let destinationBuffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer { destinationBuffer.deallocate() }

        let decodedSize = data.withUnsafeBytes { (srcBuffer: UnsafeRawBufferPointer) -> Int in
            guard let srcBase = srcBuffer.baseAddress?.assumingMemoryBound(to: UInt8.self) else { return 0 }
            return compression_decode_buffer(
                destinationBuffer,
                bufferSize,
                srcBase,
                data.count,
                nil,
                COMPRESSION_ZLIB
            )
        }

        guard decodedSize > 0 else { return nil }
        return Data(bytes: destinationBuffer, count: decodedSize)
    }

    // MARK: - Asset Merging

    /// Copy top-level files from the bundled Flow/ directory into the extracted directory,
    /// excluding subdirectories (the downloaded zip provides the assets/ subfolder).
    private func copyBundledFlowFiles(to directory: URL) {
        let fm = FileManager.default
        guard let bundleFlowURL = Bundle.main.resourceURL?.appendingPathComponent("Flow") else { return }

        guard let contents = try? fm.contentsOfDirectory(at: bundleFlowURL, includingPropertiesForKeys: [.isDirectoryKey]) else { return }

        for fileURL in contents {
            let resourceValues = try? fileURL.resourceValues(forKeys: [.isDirectoryKey])
            if resourceValues?.isDirectory == true {
                continue // Skip subdirectories (like assets/)
            }

            let destURL = directory.appendingPathComponent(fileURL.lastPathComponent)
            try? fm.removeItem(at: destURL)
            try? fm.copyItem(at: fileURL, to: destURL)
        }
    }

    // MARK: - Index.html Path Rewriting

    /// Scan the assets/ subfolder for .js and .css files, then update the src/href
    /// attributes in index.html to point to the actual hashed filenames.
    private func updateIndexHtmlPaths(in directory: URL) {
        let fm = FileManager.default
        let indexURL = directory.appendingPathComponent("index.html")
        let assetsDir = directory.appendingPathComponent("assets")

        guard fm.fileExists(atPath: indexURL.path),
              fm.fileExists(atPath: assetsDir.path),
              var html = try? String(contentsOf: indexURL, encoding: .utf8) else { return }

        guard let assetFiles = try? fm.contentsOfDirectory(at: assetsDir, includingPropertiesForKeys: nil) else { return }

        for file in assetFiles {
            let name = file.lastPathComponent
            let ext = file.pathExtension.lowercased()

            if ext == "js" && file.deletingPathExtension().lastPathComponent.hasPrefix("index") {
                // Replace src="assets/<anything>.js" or src="/assets/<anything>.js"
                if let range = html.range(of: #"src="/?assets/[^"]*\.js""#, options: .regularExpression) {
                    html.replaceSubrange(range, with: "src=\"assets/\(name)\"")
                }
            } else if ext == "css" && file.deletingPathExtension().lastPathComponent.hasPrefix("index") {
                // Replace href="assets/<anything>.css" or href="/assets/<anything>.css"
                if let range = html.range(of: #"href="/?assets/[^"]*\.css""#, options: .regularExpression) {
                    html.replaceSubrange(range, with: "href=\"assets/\(name)\"")
                }
            }
        }

        try? html.write(to: indexURL, atomically: true, encoding: .utf8)
    }
}
