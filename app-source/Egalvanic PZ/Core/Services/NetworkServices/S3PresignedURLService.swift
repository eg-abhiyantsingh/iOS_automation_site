//
//  S3PresignedURLService.swift
//  Egalvanic PZ
//
//  Service for managing S3 presigned URLs for photo upload and download
//

import Foundation

/// Response structure for presigned URL requests
struct S3PresignedURLResponse: Codable {
    let bucket: String
    let key: String
    let operation: String?
    let original_key: String?
    let public_url: String
    let url: String  // The presigned URL with query parameters
}

/// Request structure for presigned URL
struct S3PresignedURLRequest: Codable {
    let bucket: String
    let key: String
    let operation: String  // "read" or "write"
}

/// Response structure for the batch presigned URL endpoint (s3/urls/batch)
struct S3BatchPresignedURLResponse: Codable {
    let operation: String
    let results: [S3PresignedURLResponse]
    let total: Int
}

/// Batch request structure for presigned URLs with prefix matching (used for IR photos)
/// Endpoint: s3/urls/batch
struct S3BatchPresignedURLRequest: Codable {
    struct Item: Codable {
        let bucket: String
        let key: String
    }
    let items: [Item]
    let operation: String
    let find_by_prefix: Bool
}

/// Service for managing S3 presigned URL operations
final class S3PresignedURLService {
    static let shared = S3PresignedURLService()
    private init() {}

    // MARK: - Bucket Name Mapping

    /// Map photo type to S3 bucket name
    func getBucketName(for photoType: String) -> String {
        switch photoType {
        // Asset Photos Bucket
        case "node_profile",
             "node_nameplate",
             "node_panel_schedule",
             "node_arc_flash_sticker",
             "building",
             "floor",
             "room":
            return "asset_photos"

        // Attachments Bucket
        case "issue",
             "task_visual",
             "task_general",
             "task_before",
             "task_after",
             "quote":
            return "attachments"

        // IR Photos Bucket
        case "ir_metadata":
            return "ir_photos"

        default:
            // Default to attachments for unknown types
            AppLogger.log(.notice, "Unknown photo type '\(photoType)', defaulting to 'attachments' bucket", category: .photo)
            return "attachments"
        }
    }

    // MARK: - Presigned URL Generation

    /// Get presigned URL for uploading a photo
    /// - Parameters:
    ///   - filename: The filename to upload (e.g., "uuid.jpg")
    ///   - photoType: The type of photo to determine the bucket
    /// - Returns: Presigned URL response with upload URL and public URL
    func getPresignedUploadURL(filename: String, photoType: String) async throws -> S3PresignedURLResponse {
        let bucket = getBucketName(for: photoType)

        // Add "photo_" prefix for write operations
        let keyWithPrefix = "photo_\(filename)"

        let request = S3PresignedURLRequest(
            bucket: bucket,
            key: keyWithPrefix,
            operation: "write"
        )

        return try await requestPresignedURL(request: request)
    }

    /// Get presigned URL for downloading/reading a photo
    /// - Parameters:
    ///   - key: The S3 key (already includes "photo_" prefix, e.g., "photo_uuid.jpg")
    ///   - photoType: The type of photo to determine the bucket
    /// - Returns: Presigned URL response with download URL
    func getPresignedDownloadURL(key: String, photoType: String) async throws -> S3PresignedURLResponse {
        let bucket = getBucketName(for: photoType)

        // Use key as-is - it already includes "photo_" prefix from photo.url
        let request = S3PresignedURLRequest(
            bucket: bucket,
            key: key,
            operation: "read"
        )

        return try await requestPresignedURL(request: request)
    }

    // MARK: - IR Photo Presigned URLs (batch endpoint with prefix matching)

    /// Get presigned URL for an IR photo using the batch endpoint with prefix matching.
    /// Matches the web platform: POST s3/urls/batch
    /// - Parameters:
    ///   - sessionId: The IR session UUID
    ///   - photoKey: The photo key (e.g., "Test 1")
    /// - Returns: Presigned URL response for the matched object
    func getIRPhotoPresignedURL(sessionId: UUID, photoKey: String) async throws -> S3PresignedURLResponse {
        let key = "\(sessionId.uuidString.lowercased())/\(photoKey)"

        let request = S3BatchPresignedURLRequest(
            items: [S3BatchPresignedURLRequest.Item(bucket: "ir_photos", key: key)],
            operation: "read",
            find_by_prefix: true
        )

        do {
            return try await executeBatchPresignedURLRequest(request: request)
        } catch let error as NSError where error.code == 401 || error.code == 403 {
            AppLogger.log(.notice, "Token may be expired, attempting refresh for IR photo...", category: .photo)
            do {
                try await AuthService.shared.refreshAccessToken()
                return try await executeBatchPresignedURLRequest(request: request)
            } catch {
                AppLogger.log(.error, "Token refresh failed: \(error)", category: .photo)
                await MainActor.run {
                    AuthService.shared.handleSessionExpired()
                }
                throw error
            }
        }
    }

    /// Execute batch presigned URL request against s3/urls/batch endpoint
    private func executeBatchPresignedURLRequest(request: S3BatchPresignedURLRequest) async throws -> S3PresignedURLResponse {
        let baseURL = Configuration.dynamicAPIURL
        let endpoint = baseURL.appendingPathComponent(APIEndpoints.S3.urlsBatch)

        var urlRequest = URLRequest(url: endpoint)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let token = await AuthService.shared.getAccessToken() {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let subdomain = await AuthService.shared.getCurrentSubdomain() {
            urlRequest.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        }
        urlRequest.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        let encoder = JSONEncoder()
        urlRequest.httpBody = try encoder.encode(request)

        let (data, response) = try await URLSession.shared.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        if httpResponse.statusCode != 200 {
            let errorMessage = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw NSError(
                domain: "S3PresignedURLService",
                code: httpResponse.statusCode,
                userInfo: [NSLocalizedDescriptionKey: "Failed to get IR photo presigned URL: \(errorMessage)"]
            )
        }

        let decoder = JSONDecoder()
        let batchResponse = try decoder.decode(S3BatchPresignedURLResponse.self, from: data)

        guard let first = batchResponse.results.first else {
            throw NSError(
                domain: "S3PresignedURLService",
                code: 404,
                userInfo: [NSLocalizedDescriptionKey: "No results returned for IR photo"]
            )
        }

        return first
    }

    // MARK: - IR Photo Upload Presigned URLs

    /// Get presigned URL for uploading a single IR photo to S3.
    /// Key format: {sessionId}/{fileName} in the ir_photos bucket.
    func getIRPhotoUploadPresignedURL(sessionId: UUID, fileName: String) async throws -> S3PresignedURLResponse {
        let key = "\(sessionId.uuidString.lowercased())/\(fileName)"
        let request = S3PresignedURLRequest(
            bucket: "ir_photos",
            key: key,
            operation: "write"
        )
        return try await requestPresignedURL(request: request)
    }

    /// Batch fetch presigned upload (write) URLs for multiple IR photos.
    /// Returns a dictionary mapping each fileName to its presigned URL response.
    /// Files are batched in chunks of 20. Each chunk includes automatic token refresh on 401/403.
    /// Partial success: if one chunk fails, previously fetched URLs are still returned.
    func batchGetIRPhotoUploadPresignedURLs(sessionId: UUID, fileNames: [String]) async throws -> [String: S3PresignedURLResponse] {
        guard !fileNames.isEmpty else { return [:] }

        let sessionPrefix = sessionId.uuidString.lowercased()
        var result: [String: S3PresignedURLResponse] = [:]
        var lastError: Error?

        // Build lookup: S3 key -> fileName for matching results back
        func s3Key(for fileName: String) -> String {
            "\(sessionPrefix)/\(fileName)"
        }

        let batchSize = 20
        for batchStart in stride(from: 0, to: fileNames.count, by: batchSize) {
            let batchEnd = min(batchStart + batchSize, fileNames.count)
            let batch = Array(fileNames[batchStart..<batchEnd])

            let items = batch.map { fileName in
                S3BatchPresignedURLRequest.Item(bucket: "ir_photos", key: s3Key(for: fileName))
            }

            let request = S3BatchPresignedURLRequest(
                items: items,
                operation: "write",
                find_by_prefix: false
            )

            do {
                let batchResponse = try await executeBatchPresignedURLRequestFull(request: request)

                for urlResult in batchResponse.results {
                    let resultKey = urlResult.key.removingPercentEncoding ?? urlResult.key

                    // Primary match: exact key comparison
                    if let matchedFileName = batch.first(where: { s3Key(for: $0) == resultKey }) {
                        result[matchedFileName] = urlResult
                    }
                    // Fallback: match by original_key if server returns it
                    else if let originalKey = urlResult.original_key,
                            let matchedFileName = batch.first(where: { s3Key(for: $0) == originalKey }) {
                        result[matchedFileName] = urlResult
                    }
                }

                // Log any files that didn't get a presigned URL in this batch
                let missingFiles = batch.filter { result[$0] == nil }
                if !missingFiles.isEmpty {
                    AppLogger.log(.notice, "Batch upload URLs: \(missingFiles.count) files missing from response", category: .photo)
                }
            } catch {
                AppLogger.log(.error, "Batch upload presigned URL fetch failed for chunk \(batchStart/batchSize + 1): \(error)", category: .photo)
                lastError = error
                // Continue to next batch — partial success is better than total failure
            }
        }

        // If we got zero results and there was an error, propagate it
        if result.isEmpty, let lastError {
            throw lastError
        }

        AppLogger.log(.info, "Batch upload URLs: got \(result.count)/\(fileNames.count) presigned URLs", category: .photo)
        return result
    }

    // MARK: - Batch IR Photo Presigned URLs (multiple photos at once)

    /// Lightweight value type for batch presigned URL requests.
    /// Use this instead of passing SwiftData @Model objects to avoid actor isolation issues.
    struct IRPhotoURLRequest {
        let photoId: UUID
        let sessionId: UUID
        let irPhotoKey: String
        let visualPhotoKey: String
    }

    /// Batch fetch presigned URLs for multiple IR photos, matching the Android approach.
    /// Returns a dictionary mapping cache keys (e.g., "{uuid}_ir", "{uuid}_vis") to presigned URLs.
    /// Photos are batched in chunks of 20 to avoid oversized requests.
    ///
    /// - Parameter requests: Pre-extracted photo data (use `IRPhotoURLRequest` to avoid passing @Model objects off main actor)
    func batchGetIRPhotoPresignedURLs(requests: [IRPhotoURLRequest]) async -> [String: String] {
        var result: [String: String] = [:]
        var itemsToFetch: [(cacheKey: String, item: S3BatchPresignedURLRequest.Item)] = []

        for request in requests {
            let sessionPrefix = request.sessionId.uuidString.lowercased()

            // IR key
            if !request.irPhotoKey.isEmpty {
                let cacheKey = "\(request.photoId.uuidString)_ir"
                let s3Key = "\(sessionPrefix)/\(request.irPhotoKey)"
                itemsToFetch.append((cacheKey, S3BatchPresignedURLRequest.Item(bucket: "ir_photos", key: s3Key)))
            }

            // VIS key
            if !request.visualPhotoKey.isEmpty {
                let cacheKey = "\(request.photoId.uuidString)_vis"
                let s3Key = "\(sessionPrefix)/\(request.visualPhotoKey)"
                itemsToFetch.append((cacheKey, S3BatchPresignedURLRequest.Item(bucket: "ir_photos", key: s3Key)))
            }
        }

        guard !itemsToFetch.isEmpty else { return result }

        AppLogger.log(.info, "Batch fetching presigned URLs for \(itemsToFetch.count) IR photo items", category: .photo)

        let batchSize = 20
        for batchStart in stride(from: 0, to: itemsToFetch.count, by: batchSize) {
            let batchEnd = min(batchStart + batchSize, itemsToFetch.count)
            let batch = Array(itemsToFetch[batchStart..<batchEnd])

            // Build lookup: S3 key -> [cache keys] (handles deduplication)
            var s3KeyToCacheKeys: [String: [String]] = [:]
            for (cacheKey, item) in batch {
                s3KeyToCacheKeys[item.key, default: []].append(cacheKey)
            }

            // Deduplicate items sent to API
            let uniqueItems = s3KeyToCacheKeys.keys.map { key in
                S3BatchPresignedURLRequest.Item(bucket: "ir_photos", key: key)
            }

            let request = S3BatchPresignedURLRequest(
                items: uniqueItems,
                operation: "read",
                find_by_prefix: true
            )

            do {
                let batchResponse = try await executeBatchPresignedURLRequestFull(request: request)
                for urlResult in batchResponse.results {
                    guard !urlResult.url.isEmpty else { continue }

                    // Primary match: by original_key (the prefix we sent)
                    var cacheKeys = urlResult.original_key.flatMap { s3KeyToCacheKeys[$0] }

                    // Fallback: match by checking if result key starts with any of our prefixes
                    if cacheKeys == nil {
                        let decodedKey = urlResult.key.removingPercentEncoding ?? urlResult.key
                        for (prefix, keys) in s3KeyToCacheKeys {
                            if decodedKey.hasPrefix(prefix) {
                                cacheKeys = keys
                                break
                            }
                        }
                    }

                    cacheKeys?.forEach { cacheKey in
                        result[cacheKey] = urlResult.url
                    }
                }
            } catch {
                AppLogger.log(.error, "Batch IR photo presigned URL fetch failed: \(error)", category: .photo)
            }
        }

        AppLogger.log(.info, "Batch fetch complete: got \(result.count) presigned URLs", category: .photo)
        return result
    }

    /// Execute batch presigned URL request and return the full response (all results).
    /// Includes automatic token refresh on 401/403.
    private func executeBatchPresignedURLRequestFull(request: S3BatchPresignedURLRequest) async throws -> S3BatchPresignedURLResponse {
        do {
            return try await performBatchRequest(request: request)
        } catch let error as NSError where error.code == 401 || error.code == 403 {
            AppLogger.log(.notice, "Token may be expired, attempting refresh for batch IR photos...", category: .photo)
            do {
                try await AuthService.shared.refreshAccessToken()
                return try await performBatchRequest(request: request)
            } catch {
                AppLogger.log(.error, "Token refresh failed: \(error)", category: .photo)
                await MainActor.run {
                    AuthService.shared.handleSessionExpired()
                }
                throw error
            }
        }
    }

    /// Perform the actual batch HTTP request and return the decoded response.
    private func performBatchRequest(request: S3BatchPresignedURLRequest) async throws -> S3BatchPresignedURLResponse {
        let baseURL = Configuration.dynamicAPIURL
        let endpoint = baseURL.appendingPathComponent(APIEndpoints.S3.urlsBatch)

        var urlRequest = URLRequest(url: endpoint)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let token = await AuthService.shared.getAccessToken() {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let subdomain = await AuthService.shared.getCurrentSubdomain() {
            urlRequest.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        }
        urlRequest.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        let encoder = JSONEncoder()
        urlRequest.httpBody = try encoder.encode(request)

        let (data, response) = try await URLSession.shared.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        if httpResponse.statusCode != 200 {
            let errorMessage = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw NSError(
                domain: "S3PresignedURLService",
                code: httpResponse.statusCode,
                userInfo: [NSLocalizedDescriptionKey: "Batch presigned URL request failed: \(errorMessage)"]
            )
        }

        return try JSONDecoder().decode(S3BatchPresignedURLResponse.self, from: data)
    }

    // MARK: - Temp Photo Upload (for extraction from staged photos)

    /// Lightweight value type for temp upload requests.
    /// Use this instead of passing SwiftData @Model Photo objects to avoid actor isolation issues.
    struct TempPhotoUploadRequest {
        let filename: String
        let photoId: UUID
    }

    /// Upload photos to temp_photos bucket for nameplate extraction.
    /// Uses the batch presigned URL endpoint (`/s3/urls/batch`) matching Android implementation.
    /// Does NOT modify the Photo model's upload_needed status.
    /// - Parameter photos: Lightweight structs with filename and photoId
    /// - Returns: Array of public URLs for the uploaded temp photos
    func uploadPhotosToTempBucket(photos: [TempPhotoUploadRequest]) async throws -> [String] {
        guard !photos.isEmpty else { return [] }

        guard let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            throw NSError(domain: "S3TempUpload", code: 1, userInfo: [NSLocalizedDescriptionKey: "Could not access documents directory"])
        }

        AppLogger.log(.info, "Uploading \(photos.count) staged photos to temp S3...", category: .photo)

        // 1. Build batch request items for temp_photos bucket
        let items = photos.map { photo in
            S3BatchPresignedURLRequest.Item(
                bucket: "temp_photos",
                key: "photo_\(photo.filename)"
            )
        }

        let batchRequest = S3BatchPresignedURLRequest(
            items: items,
            operation: "write",
            find_by_prefix: false
        )

        // 2. Get batch presigned URLs
        let batchResponse = try await executeBatchPresignedURLRequestFull(request: batchRequest)

        // 3. Map results by key for lookup
        var urlsByKey: [String: S3PresignedURLResponse] = [:]
        for result in batchResponse.results {
            urlsByKey[result.key] = result
        }

        // 4. Upload each photo file to S3 using presigned URLs
        var publicURLs: [String] = []

        for photo in photos {
            let key = "photo_\(photo.filename)"
            guard let urlResult = urlsByKey[key] else {
                AppLogger.log(.error, "Temp upload: no presigned URL returned for key \(key)", category: .photo)
                throw NSError(domain: "S3TempUpload", code: 1,
                             userInfo: [NSLocalizedDescriptionKey: "No presigned URL returned for \(key)"])
            }

            let fileURL = documentsURL.appendingPathComponent("photos").appendingPathComponent(photo.filename)
            guard FileManager.default.fileExists(atPath: fileURL.path) else {
                AppLogger.log(.error, "Temp upload: file not found at \(fileURL.path)", category: .photo)
                continue
            }

            let data = try Data(contentsOf: fileURL)

            guard let presignedURL = URL(string: urlResult.url) else {
                AppLogger.log(.error, "Temp upload: invalid presigned URL for \(key)", category: .photo)
                continue
            }

            var uploadRequest = URLRequest(url: presignedURL)
            uploadRequest.httpMethod = "PUT"
            uploadRequest.setValue("image/jpeg", forHTTPHeaderField: "Content-Type")

            let (_, response) = try await URLSession.shared.upload(for: uploadRequest, from: data)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
                AppLogger.log(.error, "Temp upload failed with status \(statusCode) for \(photo.filename)", category: .photo)
                throw NSError(domain: "S3TempUpload", code: statusCode,
                             userInfo: [NSLocalizedDescriptionKey: "Temp S3 upload failed for \(photo.filename)"])
            }

            publicURLs.append(urlResult.public_url)
            AppLogger.log(.info, "Temp uploaded photo \(photo.filename) to temp_photos bucket", category: .photo)
        }

        return publicURLs
    }

    // MARK: - API Communication (single-item endpoint: s3/url)

    /// Request presigned URL from the backend with automatic token refresh on expiry
    private func requestPresignedURL(request: S3PresignedURLRequest) async throws -> S3PresignedURLResponse {
        do {
            return try await executeSinglePresignedURLRequest(request: request)
        } catch let error as NSError where error.code == 401 || error.code == 403 {
            AppLogger.log(.notice, "Token may be expired, attempting refresh...", category: .photo)
            do {
                try await AuthService.shared.refreshAccessToken()
                AppLogger.log(.info, "Token refreshed, retrying presigned URL request", category: .photo)
                return try await executeSinglePresignedURLRequest(request: request)
            } catch {
                AppLogger.log(.error, "Token refresh failed: \(error)", category: .photo)
                await MainActor.run {
                    AuthService.shared.handleSessionExpired()
                }
                throw error
            }
        }
    }

    /// Execute the single-item presigned URL request against s3/url
    private func executeSinglePresignedURLRequest(request: S3PresignedURLRequest) async throws -> S3PresignedURLResponse {
        let baseURL = Configuration.dynamicAPIURL
        let endpoint = baseURL.appendingPathComponent(APIEndpoints.S3.url)

        var urlRequest = URLRequest(url: endpoint)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let token = await AuthService.shared.getAccessToken() {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let subdomain = await AuthService.shared.getCurrentSubdomain() {
            urlRequest.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        }
        urlRequest.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        let encoder = JSONEncoder()
        urlRequest.httpBody = try encoder.encode(request)

        AppLogger.log(.debug, "Requesting presigned URL - Operation: \(request.operation), Bucket: \(request.bucket), Key: \(request.key)", category: .photo)

        let (data, response) = try await URLSession.shared.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        if httpResponse.statusCode != 200 {
            let errorMessage = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw NSError(
                domain: "S3PresignedURLService",
                code: httpResponse.statusCode,
                userInfo: [NSLocalizedDescriptionKey: "Failed to get presigned URL: \(errorMessage)"]
            )
        }

        let decoder = JSONDecoder()
        let presignedResponse = try decoder.decode(S3PresignedURLResponse.self, from: data)

        AppLogger.log(.debug, "Presigned URL received - Public URL: \(presignedResponse.public_url), Presigned URL: \(presignedResponse.url.prefix(100))...", category: .photo)

        return presignedResponse
    }
}
