//
// APIClient.swift
// SwiftDataTutorial
//
// Created by Eric Ehlert on 7/11/25.
//

import Foundation
import SwiftUI
import SwiftData

final class APIClient {
    static let shared = APIClient()
    private init() {}
    
    /// Dynamic base URL - uses company-specific invoke URL from config, falls back to Configuration.apiBaseURL
    var baseURL: URL {
        let urlString = CompanyConfigService.shared.getCurrentInvokeURL()
        return URL(string: urlString) ?? Configuration.apiBaseURL
    }
    
    // MARK: - Token Refresh State
    // Token refresh coordination is now handled by TokenRefreshCoordinator actor
    // to prevent race conditions with concurrent 401 responses
    
    // MARK: - Authorization Helper
    
    /// Creates a URLRequest with Authorization header if user is authenticated
    func createAuthorizedRequest(url: URL, method: String = "GET") async -> URLRequest {
        await proactiveRefreshIfNeeded(for: url)

        var request = URLRequest(url: url)
        request.httpMethod = method

        // Add authorization header if token is available
        if let token = await AuthService.shared.getAccessToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        } else {
            slog("No access token available, request will be unauthenticated", category: .auth, level: .warning)
        }
        
        // CRITICAL: Add X-Subdomain header for multi-tenant token validation
        // Backend validates tokens against different Cognito pools based on subdomain
        if let subdomain = await AuthService.shared.getCurrentSubdomain() {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
            slog("Added X-Subdomain header", category: .auth, data: ["subdomain": subdomain])
        } else {
            slog("No subdomain available - multi-tenant token validation may fail", category: .auth, level: .warning)
        }

        // Add X-Language header for localized API responses
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        return request
    }
    
    // MARK: - Proactive Token Refresh

    /// Endpoint paths that must skip the proactive-refresh check to avoid recursion
    /// (the refresh call itself goes to /auth/v2/refresh).
    private static let authEndpointSuffixes: [String] = [
        "/auth/v2/login",
        "/auth/v2/refresh",
        "/auth/v2/logout",
        "/auth/v2/me",
        "/auth/respond-to-challenge",
        "/auth/forgot-password",
        "/auth/reset-password"
    ]

    private func isAuthEndpoint(_ url: URL) -> Bool {
        let path = url.path
        return Self.authEndpointSuffixes.contains(where: { path.hasSuffix($0) })
    }

    /// Refresh the access token if it's within the 60-second proactive buffer of expiry.
    /// Pass `nil` for `url` from non-request call sites (e.g. scenePhase hook) where no
    /// recursion guard is needed. Failures here are swallowed; callers fall back to the
    /// reactive 401 path which surfaces session-expired UI.
    func proactiveRefreshIfNeeded(for url: URL?) async {
        if let url, isAuthEndpoint(url) { return }
        guard let expiresAt = await AuthService.shared.getExpiresAt() else { return }
        let threshold = expiresAt.addingTimeInterval(-Self.proactiveRefreshBufferSeconds)
        guard Date() > threshold else { return }
        slog("Token within proactive-refresh buffer; refreshing", category: .auth)
        do {
            try await tokenRefreshCoordinator.refreshIfNeeded()
        } catch AuthError.notAuthenticated {
            // Refresh token is rejected — surface the session-expired sheet now
            // instead of letting the request fall through to a guaranteed 401.
            slog("Proactive refresh: refresh token rejected; surfacing session-expired", category: .auth, level: .warning)
            await AuthService.shared.handleSessionExpired()
        } catch {
            // Transient failure (network, parse, etc.) — let the request proceed
            // and the reactive 401 path retry once.
            slog("Proactive refresh failed (transient): \(error)", category: .auth, level: .warning)
        }
    }

    private static let proactiveRefreshBufferSeconds: TimeInterval = 60

    // MARK: - Core Request Methods with Auto-Refresh
    
    /// Execute any request with automatic token refresh on 401
    /// Returns decoded response of type T
    private func executeRequest<T: Decodable>(
        _ request: URLRequest,
        decoder: JSONDecoder = JSONDecoder(),
        retryCount: Int = 0,
        maxRetries: Int = 1
    ) async throws -> T {
        // Log outgoing request
        AppLogger.logRequest(request)
        let startTime = CFAbsoluteTimeGetCurrent()
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            let duration = CFAbsoluteTimeGetCurrent() - startTime
            
            guard let httpResponse = response as? HTTPURLResponse else {
                AppLogger.log(.error, "Invalid response type", category: .api)
                throw URLError(.badServerResponse)
            }
            
            // Log the response
            AppLogger.logResponse(request, data: data, httpResponse: httpResponse, duration: duration)
            
            // Handle 401 - Token Expired
            if httpResponse.statusCode == 401 {
                AppLogger.log(.notice, "Received 401 - attempting token refresh (retry \(retryCount + 1)/\(maxRetries))", category: .api)
                
                guard retryCount < maxRetries else {
                    AppLogger.log(.error, "Max retries reached after 401", category: .api)
                    throw URLError(.userAuthenticationRequired)
                }
                
                do {
                    try await refreshTokenIfNeeded()
                } catch {
                    AppLogger.log(.error, "Token refresh failed: \(error)", category: .api)
                    if case AuthError.notAuthenticated = error {
                        AppLogger.log(.notice, "Refresh token invalid - triggering re-authentication flow", category: .auth)
                        await AuthService.shared.handleSessionExpired()
                    }
                    throw error
                }
                
                var newRequest = request
                guard let newToken = await AuthService.shared.getAccessToken() else {
                    AppLogger.log(.error, "No token available after refresh - triggering re-authentication", category: .api)
                    await AuthService.shared.handleSessionExpired()
                    throw URLError(.userAuthenticationRequired)
                }
                newRequest.setValue("Bearer \(newToken)", forHTTPHeaderField: "Authorization")
                
                if let subdomain = await AuthService.shared.getCurrentSubdomain() {
                    newRequest.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
                }
                
                return try await executeRequest(
                    newRequest,
                    decoder: decoder,
                    retryCount: retryCount + 1,
                    maxRetries: maxRetries
                )
            }
            
            // Handle other error status codes
            guard (200..<300).contains(httpResponse.statusCode) else {
                throw URLError(.badServerResponse)
            }
            
            // Success - decode response
            let decoded = try decoder.decode(T.self, from: data)
            return decoded
            
        } catch {
            // Only log network/other errors — HTTP errors were already logged by logResponse
            if (error as? URLError)?.code != .badServerResponse {
                AppLogger.log(.error, "Request failed: \(request.httpMethod ?? "GET") \(request.url?.absoluteString ?? "") -> \(error)", category: .api)
            }
            throw error
        }
    }
    
    /// Execute request that returns URLResponse (for non-decoded responses)
    func executeRequestRaw(
        _ request: URLRequest,
        retryCount: Int = 0,
        maxRetries: Int = 1
    ) async throws -> URLResponse {
        // Log outgoing request
        AppLogger.logRequest(request)
        let startTime = CFAbsoluteTimeGetCurrent()
        
        let data: Data
        let response: Foundation.URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            print("‼️ executeRequestRaw NETWORK ERROR: \(error.localizedDescription) url=\(request.url?.absoluteString ?? "nil")")
            throw error
        }
        let duration = CFAbsoluteTimeGetCurrent() - startTime

        guard let httpResponse = response as? HTTPURLResponse else {
            AppLogger.log(.error, "Invalid response type", category: .api)
            throw URLError(.badServerResponse)
        }
        
        // Log the response
        AppLogger.logResponse(request, data: data, httpResponse: httpResponse, duration: duration)
        
        // Handle 401 - Token Expired
        if httpResponse.statusCode == 401 {
            AppLogger.log(.notice, "Received 401 - attempting token refresh (retry \(retryCount + 1)/\(maxRetries))", category: .api)
            
            guard retryCount < maxRetries else {
                AppLogger.log(.error, "Max retries reached after 401", category: .api)
                throw URLError(.userAuthenticationRequired)
            }
            
            do {
                try await refreshTokenIfNeeded()
            } catch {
                AppLogger.log(.error, "Token refresh failed: \(error)", category: .api)
                if case AuthError.notAuthenticated = error {
                    AppLogger.log(.notice, "Refresh token invalid - triggering re-authentication flow", category: .auth)
                    await AuthService.shared.handleSessionExpired()
                }
                throw error
            }
            
            var newRequest = request
            guard let newToken = await AuthService.shared.getAccessToken() else {
                AppLogger.log(.error, "No token available after refresh - triggering re-authentication", category: .api)
                await AuthService.shared.handleSessionExpired()
                throw URLError(.userAuthenticationRequired)
            }
            newRequest.setValue("Bearer \(newToken)", forHTTPHeaderField: "Authorization")
            
            if let subdomain = await AuthService.shared.getCurrentSubdomain() {
                newRequest.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
            }
            
            return try await executeRequestRaw(
                newRequest,
                retryCount: retryCount + 1,
                maxRetries: maxRetries
            )
        }
        
        // Handle other error status codes
        guard (200..<300).contains(httpResponse.statusCode) else {
            print("‼️ executeRequestRaw NON-2XX: status=\(httpResponse.statusCode) url=\(request.url?.absoluteString ?? "nil") method=\(request.httpMethod ?? "nil")")
            if let body = String(data: data, encoding: .utf8)?.prefix(200) {
                print("‼️ response body: \(body)")
            }
            throw URLError(.badServerResponse)
        }
        
        return response
    }
    
    // MARK: - Token Refresh with Mutex
    
    /// Actor to serialize token refresh operations and prevent race conditions
    private actor TokenRefreshCoordinator {
        private var refreshTask: Task<Void, Error>?
        
        func refreshIfNeeded() async throws {
            // If already refreshing, wait for that to complete
            if let existingTask = refreshTask {
                slog("Token refresh already in progress, waiting...", category: .api)
                try await existingTask.value
                return
            }
            
            // Create and immediately store the task BEFORE awaiting
            // This prevents race conditions where multiple callers check refreshTask
            // before the first one has assigned it
            let task = Task {
                slog("Starting token refresh...", category: .api)
                try await AuthService.shared.refreshAccessToken()
                slog("Token refresh complete", category: .api)
            }
            
            refreshTask = task
            
            defer {
                refreshTask = nil
            }
            
            try await task.value
        }
    }
    
    private let tokenRefreshCoordinator = TokenRefreshCoordinator()
    
    /// Refresh token with mutex to prevent concurrent refresh attempts
    private func refreshTokenIfNeeded() async throws {
        try await tokenRefreshCoordinator.refreshIfNeeded()
    }
    
    func fetchUserChoiceDTOs(user_id: UUID) async throws -> [SLDChoiceDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.SLD.userSLDs(userId: user_id.uuidString))
        slog("Fetching user choice DTOs", category: .api, data: ["url": url.absoluteString])
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        let dtos: [SLDChoiceDTO] = try await executeRequest(request, decoder: decoder)
        slog("Decoded DTOs", category: .api, data: ["count": dtos.count])
        return dtos
    }
    
    func fetchSLDDTOV2(sld_id: UUID) async throws -> SLDDTO {
        let url = baseURL.appendingPathComponent(APIEndpoints.SLD.details(sldId: sld_id.uuidString))
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        let dto: SLDDTO = try await executeRequest(request, decoder: decoder)
        slog("Decoded SLDDTO", category: .api)
        return dto
    }
    
    // MARK: - SLD CREATION
    func createSLD(name: String, companyId: UUID, addressLine1: String? = nil, addressLine2: String? = nil, city: String? = nil, stateProvince: String? = nil, postalCode: String? = nil, countryCode: String? = nil, addressFormatted: String? = nil, latitude: Double? = nil, longitude: Double? = nil, accountId: String? = nil, complexityLevel: Double? = nil, laborUnionId: String? = nil, officeId: String? = nil) async throws -> SLDChoiceDTO {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.SLD.create)
        
        // 2) Build coordinates dictionary if lat/long provided
        var addressCoordinates: [String: Double]? = nil
        if let lat = latitude, let lon = longitude {
            addressCoordinates = ["latitude": lat, "longitude": lon]
        }
        
        // 3) Create the payload
        var payload: [String: Any] = [
            "name": name,
            "company_id": companyId.uuidString
        ]
        
        // Add optional address fields
        if let addressLine1 = addressLine1 { payload["address_line_1"] = addressLine1 }
        if let addressLine2 = addressLine2 { payload["address_line_2"] = addressLine2 }
        if let city = city { payload["city"] = city }
        if let stateProvince = stateProvince { payload["state_province"] = stateProvince }
        if let postalCode = postalCode { payload["postal_code"] = postalCode }
        if let countryCode = countryCode { payload["country_code"] = countryCode }
        if let addressFormatted = addressFormatted { payload["address_formatted"] = addressFormatted }
        if let addressCoordinates = addressCoordinates { payload["address_coordinates"] = addressCoordinates }
        if let accountId = accountId { payload["account_id"] = accountId }
        if let complexityLevel = complexityLevel { payload["complexity_level"] = complexityLevel }
        if let laborUnionId = laborUnionId { payload["labor_union_id"] = laborUnionId }
        if let officeId = officeId { payload["office_id"] = officeId }

        // 4) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)

        // 5) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createSLD()", category: .api, data: [
            "url": url.absoluteString,
            "method": request.httpMethod ?? "N/A"
        ])
        
        // 6) Fire request with auto-refresh and decode response
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        // Backend returns { "success": true, "sld": {...} }
        struct CreateSLDResponse: Decodable {
            let success: Bool
            let sld: SLDChoiceDTO
        }
        
        let response: CreateSLDResponse = try await executeRequest(request, decoder: decoder)
        slog("createSLD successful", category: .api, data: ["sld_id": response.sld.id.uuidString])
        
        return response.sld
    }
    
    // MARK: - SLD UPDATE
    func updateSLD(sldId: UUID, name: String, addressLine1: String? = nil, addressLine2: String? = nil, city: String? = nil, stateProvince: String? = nil, postalCode: String? = nil, countryCode: String? = nil, addressFormatted: String? = nil, latitude: Double? = nil, longitude: Double? = nil, accountId: String? = nil, complexityLevel: Double? = nil, laborUnionId: String? = nil, officeId: String? = nil) async throws -> SLDChoiceDTO {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.SLD.update(sldId: sldId.uuidString))
        
        // 2) Build coordinates dictionary if lat/long provided
        var addressCoordinates: [String: Double]? = nil
        if let lat = latitude, let lon = longitude {
            addressCoordinates = ["latitude": lat, "longitude": lon]
        }
        
        // 3) Create the payload
        var payload: [String: Any] = [
            "name": name
        ]
        
        // Add optional address fields
        if let addressLine1 = addressLine1 { payload["address_line_1"] = addressLine1 }
        if let addressLine2 = addressLine2 { payload["address_line_2"] = addressLine2 }
        if let city = city { payload["city"] = city }
        if let stateProvince = stateProvince { payload["state_province"] = stateProvince }
        if let postalCode = postalCode { payload["postal_code"] = postalCode }
        if let countryCode = countryCode { payload["country_code"] = countryCode }
        if let addressFormatted = addressFormatted { payload["address_formatted"] = addressFormatted }
        if let addressCoordinates = addressCoordinates { payload["address_coordinates"] = addressCoordinates }
        if let accountId = accountId { payload["account_id"] = accountId }
        if let complexityLevel = complexityLevel { payload["complexity_level"] = complexityLevel }
        // Send explicit null for labor_union_id so backend clears it when user removes selection
        payload["labor_union_id"] = laborUnionId ?? NSNull()
        // Same for office_id (ZP-2061)
        payload["office_id"] = officeId ?? NSNull()

        // 4) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)

        // 5) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("updateSLD()", category: .api, data: [
            "url": url.absoluteString,
            "method": request.httpMethod ?? "N/A",
            "sld_id": sldId.uuidString
        ])
        
        // 6) Fire request with auto-refresh and decode response
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        // Backend returns { "success": true, "sld": {...} }
        struct UpdateSLDResponse: Decodable {
            let success: Bool
            let sld: SLDChoiceDTO
        }
        
        let response: UpdateSLDResponse = try await executeRequest(request, decoder: decoder)
        slog("updateSLD successful", category: .api, data: ["sld_id": response.sld.id.uuidString])
        
        return response.sld
    }
    
    // MARK: - ACCOUNT ROUTES
    func fetchAccounts(companyId: UUID) async throws -> [AccountDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Account.byCompany(companyId: companyId.uuidString))
        let request = await createAuthorizedRequest(url: url, method: "GET")

        let decoder = JSONDecoder()
        let response: AccountListResponse = try await executeRequest(request, decoder: decoder)
        slog("Decoded AccountDTOs", category: .api, data: ["count": response.accounts.count])
        return response.accounts
    }

    // MARK: - LABOR UNION ROUTES
    func fetchLaborUnions() async throws -> [LaborUnionDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.LaborUnion.list)
        let request = await createAuthorizedRequest(url: url, method: "GET")

        let decoder = JSONDecoder()
        let response: LaborUnionListResponse = try await executeRequest(request, decoder: decoder)
        slog("Decoded LaborUnionDTOs", category: .api, data: ["count": response.data.count])
        return response.data
    }

    // MARK: - OFFICE ROUTES
    func fetchOffices() async throws -> [OfficeDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Office.list)
        let request = await createAuthorizedRequest(url: url, method: "GET")

        let decoder = JSONDecoder()
        let response: OfficeListResponse = try await executeRequest(request, decoder: decoder)
        slog("Decoded OfficeDTOs", category: .api, data: ["count": response.data.count])
        return response.data.filter { !$0.isDeleted }
    }

    func fetchUserTaskFormDTOS() async throws -> [UserTaskFormDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Form.list)
        let request = await createAuthorizedRequest(url: url, method: "GET")

        let decoder = JSONDecoder()
        let dtos: [UserTaskFormDTO] = try await executeRequest(request, decoder: decoder)
        slog("Decoded UserTaskFormDTOs", category: .api, data: ["count": dtos.count])
        return dtos
    }

    // MARK: - TEST EQUIPMENT ROUTES
    func fetchTestEquipment() async throws -> [TestEquipmentDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Equipment.list)
        let request = await createAuthorizedRequest(url: url, method: "GET")

        let decoder = JSONDecoder()
        let response: TestEquipmentResponse = try await executeRequest(request, decoder: decoder)
        slog("Decoded TestEquipmentDTOs", category: .api, data: ["count": response.data.count])
        return response.data
    }

    // MARK: - PROCEDURE LOOKUP
    /// Fetch the flat procedure catalog. We always pull the full list
    /// during refresh and let the picker filter by node class locally,
    /// so the procedure dropdown still works when the user creates a
    /// task offline.
    func fetchProcedures() async throws -> [ProcedureDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Lookup.procedures)
        let request = await createAuthorizedRequest(url: url, method: "GET")

        let decoder = JSONDecoder()
        let response: ProcedureLookupResponse = try await executeRequest(request, decoder: decoder)
        slog("Decoded ProcedureDTOs", category: .api, data: ["count": response.data.count])
        return response.data
    }

    // MARK: - Equipment Library Bulk Download

    func fetchEquipmentLibraryBulk() async throws -> EqpLibraryBulkResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.EquipmentLibrary.bulk)
        var request = await createAuthorizedRequest(url: url, method: "GET")
        request.timeoutInterval = 300  // Large payload (~100MB) needs generous timeout on cellular

        var (data, response) = try await URLSession.shared.data(for: request)
        print("‼️ EqpLib first attempt status: \((response as? HTTPURLResponse)?.statusCode ?? -1), bytes: \(data.count)")

        // Handle 401 with token refresh (matching executeRequestRaw pattern)
        if let http = response as? HTTPURLResponse, http.statusCode == 401 {
            do {
                try await refreshTokenIfNeeded()
            } catch {
                if case AuthError.notAuthenticated = error {
                    await AuthService.shared.handleSessionExpired()
                }
                throw error
            }
            request = await createAuthorizedRequest(url: url, method: "GET")
            request.timeoutInterval = 300
            (data, response) = try await URLSession.shared.data(for: request)
        }

        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw NSError(domain: "APIClient", code: http.statusCode,
                          userInfo: [NSLocalizedDescriptionKey: "Equipment library download failed (\(http.statusCode))"])
        }

        let decoder = JSONDecoder()
        do {
            let bulkResponse = try decoder.decode(EqpLibraryBulkResponse.self, from: data)
            slog("fetchEquipmentLibraryBulk", category: .api, data: [
                "categories": bulkResponse.categories.count,
                "sensors": bulkResponse.sensors.count
            ])
            return bulkResponse
        } catch {
            print("‼️ EqpLib DECODE ERROR: \(error)")
            let preview = String(data: data.prefix(500), encoding: .utf8) ?? "(non-utf8, \(data.count) bytes)"
            print("‼️ EqpLib RESPONSE PREVIEW (\(data.count) bytes): \(preview)")
            throw error
        }
    }
    
    // MARK: - SLD Viewer Update

    func fetchSldViewerLatest() async throws -> SldViewerLatestResponse {
        var components = URLComponents(url: baseURL.appendingPathComponent(APIEndpoints.SldViewer.latest), resolvingAgainstBaseURL: false)!
        components.queryItems = [URLQueryItem(name: "platform", value: "ios")]

        let request = await createAuthorizedRequest(url: components.url!, method: "GET")

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase

        let response: SldViewerLatestResponse = try await executeRequest(request, decoder: decoder)
        slog("Fetched SLD viewer latest", category: .api, data: [
            "success": response.success,
            "version": response.data?.sldViewerVersion ?? "nil"
        ])
        return response
    }

    // MARK: - EDGE CLASS ROUTES
    func fetchEdgeClassDTOs() async throws -> [EdgeClassDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Classes.edgeClasses)
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        let dtos: [EdgeClassDTO] = try await executeRequest(request, decoder: decoder)
        slog("Decoded EdgeClassDTOs", category: .api, data: ["count": dtos.count])
        return dtos
    }
    
    func fetchEdgeClassDTOsByUser(user_id: UUID) async throws -> [EdgeClassDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Classes.edgeClasses(userId: user_id.uuidString))
        slog("Fetching edge classes for user", category: .api, data: ["url": url.absoluteString, "user_id": user_id.uuidString])
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        let dtos: [EdgeClassDTO] = try await executeRequest(request, decoder: decoder)
        slog("Decoded EdgeClassDTOs for user", category: .api, data: ["count": dtos.count, "user_id": user_id.uuidString])
        return dtos
    }
    
    // MARK: - ISSUE CLASS ROUTES
    func fetchIssueClassDTOs() async throws -> [IssueClassDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Classes.issueClasses)
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        let dtos: [IssueClassDTO] = try await executeRequest(request, decoder: decoder)
        slog("Decoded IssueClassDTOs", category: .api, data: ["count": dtos.count])
        return dtos
    }
    
    func fetchIssueClassDTOsByUser(user_id: UUID) async throws -> [IssueClassDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Classes.issueClasses(userId: user_id.uuidString))
        slog("Fetching issue classes for user", category: .api, data: ["url": url.absoluteString, "user_id": user_id.uuidString])
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        let dtos: [IssueClassDTO] = try await executeRequest(request, decoder: decoder)
        slog("Decoded IssueClassDTOs for user", category: .api, data: ["count": dtos.count, "user_id": user_id.uuidString])
        return dtos
    }
    
    // MARK: - SHORTCUT ROUTES
    func fetchShortcutDTOsByUser(user_id: UUID) async throws -> [NodeShortcutDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Shortcuts.userShortcuts(userId: user_id.uuidString))
        slog("Fetching user shortcuts", category: .api, data: ["url": url.absoluteString, "user_id": user_id.uuidString])
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        let decoder = JSONDecoder()
        let dtos: [NodeShortcutDTO] = try await executeRequest(request, decoder: decoder)
        slog("Decoded shortcut DTOs", category: .api, data: ["count": dtos.count, "user_id": user_id.uuidString])
        return dtos
    }
    
    // MARK: - NODE CLASS ROUTES
    func fetchNodeClassDTOs() async throws -> [NodeClassDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Classes.nodeClasses)
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        let dtos: [NodeClassDTO] = try await executeRequest(request, decoder: decoder)
        slog("Decoded NodeClassDTOs", category: .api, data: ["count": dtos.count])
        return dtos
    }
    
    func fetchNodeClassDTOsByUser(user_id: UUID) async throws -> [NodeClassDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Classes.nodeClasses(userId: user_id.uuidString))
        slog("Fetching node classes for user", category: .api, data: ["url": url.absoluteString, "user_id": user_id.uuidString])
        let request = await createAuthorizedRequest(url: url, method: "GET")

        let decoder = JSONDecoder()
        let dtos: [NodeClassDTO] = try await executeRequest(request, decoder: decoder)
        slog("Decoded NodeClassDTOs for user", category: .api, data: ["count": dtos.count, "user_id": user_id.uuidString])
        return dtos
    }

    /// ZP-2161: fetch the ``eg_eqp_lib_types`` + ``eg_eqp_lib_subtypes``
    /// taxonomy so the Engineering section can resolve
    /// ``NodeClass.eqp_lib_type_id`` into a kind/slug and drive its
    /// conditional rendering.
    func fetchEqpLibTaxonomy() async throws -> [EgEqpLibTypeDTO] {
        let url = baseURL.appendingPathComponent("/equipment-library/taxonomy")
        let request = await createAuthorizedRequest(url: url, method: "GET")
        let decoder = JSONDecoder()
        let response: EgEqpLibTaxonomyResponse = try await executeRequest(request, decoder: decoder)
        slog("Decoded EgEqpLibTaxonomy", category: .api, data: ["count": response.types.count])
        return response.types
    }

    /// ZP-2161: fetch every enum table referenced by NodeV2 / NodeClass
    /// / NodeSubtype FK columns in a single payload. Resolves
    /// voltage_id, mains_type_id, phase_configuration_id, trip_type_id,
    /// device_role_id, manufacturer_id, and the 7 cable enums into
    /// human-readable labels for the Engineering section.
    func fetchEnumsBundle() async throws -> EnumsBundleDTO {
        let url = baseURL.appendingPathComponent("/equipment-library/enums")
        let request = await createAuthorizedRequest(url: url, method: "GET")
        let decoder = JSONDecoder()
        let bundle: EnumsBundleDTO = try await executeRequest(request, decoder: decoder)
        slog("Decoded EnumsBundle", category: .api, data: [
            "voltages": bundle.voltages.count,
            "manufacturers": bundle.manufacturers.count,
            "trip_types": bundle.trip_types.count,
            "cable_sizes": bundle.cable_sizes.count,
        ])
        return bundle
    }

    /// ZP-2161 Phase 4a: fetch the SKM library "headers" bundle —
    /// device, transformer, cable, and bus headers + the (eqp_lib
    /// type, subtype) → allowed-trip-type routing rules. Drives the
    /// Engineering section's Type / Manufacturer / Subtype filters
    /// offline. ~1 MB payload.
    func fetchSkmHeadersBundle() async throws -> SkmHeadersBundleDTO {
        let url = baseURL.appendingPathComponent("/equipment-library/skm-headers")
        let request = await createAuthorizedRequest(url: url, method: "GET")
        let decoder = JSONDecoder()
        let bundle: SkmHeadersBundleDTO = try await executeRequest(request, decoder: decoder)
        slog("Decoded SkmHeadersBundle", category: .api, data: [
            "devices": bundle.devices.count,
            "transformer_models": bundle.transformer_models.count,
            "cables_ac": bundle.cables_ac.count,
            "bus_models": bundle.bus_models.count,
            "dev_lib_routing": bundle.dev_lib_routing.count,
        ])
        return bundle
    }

    /// ZP-2161 Phase 4b: fetch the SKM device deep tree (frames +
    /// sensors + trip_units + segments). URLSession transparently
    /// gzip-decompresses when the server sends ``Content-Encoding: gzip``
    /// — we just need ``Accept-Encoding: gzip`` on the request.
    /// Backend tested at ~6 MB raw / ~1.5 MB gzipped.
    func fetchSkmTreeBundle() async throws -> SkmTreeBundleDTO {
        let url = baseURL.appendingPathComponent("/equipment-library/skm-tree")
        var request = await createAuthorizedRequest(url: url, method: "GET")
        request.setValue("gzip", forHTTPHeaderField: "Accept-Encoding")
        let decoder = JSONDecoder()
        let bundle: SkmTreeBundleDTO = try await executeRequest(request, decoder: decoder)
        slog("Decoded SkmTreeBundle", category: .api, data: [
            "frames": bundle.frames.count,
            "sensors": bundle.sensors.count,
            "trip_units": bundle.trip_units.count,
            "segments": bundle.segments.count,
        ])
        return bundle
    }

    func createNode(node: NodeV2) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Node.create)
        
        // 2) Ensure node has an SLD relationship
        guard let sldId = node.sld?.id else {
            slog("createNode: Node has no SLD relationship", category: .api, level: .error, data: ["node_id": node.id.uuidString])
            throw NSError(domain: "APIClient", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Node must have an SLD relationship to be created"
            ])
        }
        
        // 3) Create the DTO payload with core_attributes and node_terminals
        var payload = SLDDTONode(
            id: node.id,
            type: node.type,
            label: node.label,
            sld_id: sldId,
            parent_id: node.parent_id,
            x: node.x,
            y: node.y,
            width: node.width,
            height: node.height,
            is_deleted: node.is_deleted,
            location: node.location,
            node_class: node.node_class?.id,
            node_subtype: node.node_subtype?.id,
            core_attributes: node.core_attributes.map { attr in
                NodePropertyDTO(
                    id: attr.id,
                    node_class_property: attr.node_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value
                )
            },
            node_terminals: node.node_terminals.filter { !$0.is_deleted }.map { $0.toDTO() },
            com: node.com,
            com_calculation: node.com_calculation.map { COMCalculationDTO(from: $0) },
            qr_code: node.qr_code,
            serviceability: node.serviceability,
            serviceability_note: node.serviceability_note,
            voltage: node.voltage,
            voltage_id: node.voltage_id,
            secondary_voltage: node.secondary_voltage,
            secondary_voltage_id: node.secondary_voltage_id,
            notes: node.notes,
            room_id: node.room?.id,
            default_photo_id: node.default_photo_id,
            suggested_shortcut: node.suggested_shortcut_id,
            eqp_lib: node.eqp_lib
        )

        // ZP-2419: thread every engineering field through to the create
        // payload — without this, kva/ampere/busway/etc. entered during
        // Create Asset are lost server-side and wiped on next refresh.
        // Mirrors the same block in updateNode/buildNodeUpdatePayload.
        payload.tertiary_voltage = node.tertiary_voltage
        payload.tertiary_voltage_id = node.tertiary_voltage_id
        payload.system_voltage_id = node.system_voltage_id
        payload.circuit_voltage_id = node.circuit_voltage_id
        payload.voltage_user_overridden = node.voltage_user_overridden
        payload.applied_shortcut = node.applied_shortcut_id
        payload.eqp_lib_suggested = node.eqp_lib_suggested
        payload.eqp_note = node.eqp_note
        payload.eqp_engineering_approved = node.eqp_engineering_approved
        payload.skm_lib_name = node.skm_lib_name
        payload.skm_lib_name_suggested = node.skm_lib_name_suggested
        payload.ocr_signature = node.ocr_signature
        payload.kva_rating = node.kva_rating
        payload.percent_impedance = node.percent_impedance
        payload.mains_type_id = node.mains_type_id
        payload.phase_configuration_id = node.phase_configuration_id
        payload.ampere_rating = node.ampere_rating
        payload.pole_count = node.pole_count
        payload.manufacturer_id = node.manufacturer_id
        payload.has_trip_unit = node.has_trip_unit
        payload.trip_type_id = node.trip_type_id
        payload.frame_amps = node.frame_amps
        payload.sensor_amps = node.sensor_amps
        payload.plug_amps = node.plug_amps
        payload.length = node.length
        payload.conductor_material = node.conductor_material
        payload.cable_size_id = node.cable_size_id
        payload.conductor_configuration_id = node.conductor_configuration_id
        payload.duct_material_id = node.duct_material_id
        payload.conductor_description_id = node.conductor_description_id
        payload.insulation_class_id = node.insulation_class_id
        payload.insulation_type_id = node.insulation_type_id
        payload.installation_id = node.installation_id
        payload.busway_ampere_rating = node.busway_ampere_rating
        payload.replacement_cost = node.replacement_cost
        payload.panel_schedule_status = node.panel_schedule_status
        payload.rotation = node.rotation
        payload.locked = node.locked

        // 3) Encode to JSON, then handle nil fields and eqp_lib raw object
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let encodedData = try encoder.encode(payload)
        var jsonObj = try JSONSerialization.jsonObject(with: encodedData) as? [String: Any] ?? [:]
        // Explicitly send null for fields that JSONEncoder skips when nil
        if node.com == nil {
            jsonObj["com"] = NSNull()
        }
        if node.com_calculation == nil {
            jsonObj["com_calculation"] = NSNull()
        }
        // Replace eqp_lib string with raw JSON object
        if let eqpStr = node.eqp_lib, let eqpData = eqpStr.data(using: .utf8),
           let eqpObj = try? JSONSerialization.jsonObject(with: eqpData) {
            jsonObj["eqp_lib"] = eqpObj
        }
        let bodyData = try JSONSerialization.data(withJSONObject: jsonObj)

        // 4) Build URLRequest
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createNode()", category: .api, data: [
            "url": url.absoluteString,
            "node_id": node.id.uuidString,
            "node_subtype_id": node.node_subtype?.id.uuidString ?? "nil",
            "node_subtype_name": node.node_subtype?.name ?? "nil",
            "core_attributes_count": node.core_attributes.count
        ])
        
        // 5) Add authorization and fire request with auto-refresh
        request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        return try await executeRequestRaw(request)
    }
    
    func updateNode(_ node: NodeV2, extraData: [String: Any] = [:]) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Node.update(nodeId: node.id.uuidString))
        
        // 2) Ensure node has an SLD relationship
        guard let sldId = node.sld?.id else {
            slog("updateNode: Node has no SLD relationship", category: .api, level: .error, data: ["node_id": node.id.uuidString])
            throw NSError(domain: "APIClient", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Node must have an SLD relationship to be updated"
            ])
        }
        
        // 3) Create the DTO payload with core_attributes
        // Note: We always send the current type value, which preserves "group" if it's already set
        var payload = SLDDTONode(
            id: node.id,
            type: node.type, // This will be "group" if it was already "group"
            label: node.label,
            sld_id: sldId,
            parent_id: node.parent_id,
            x: node.x,
            y: node.y,
            width: node.width,
            height: node.height,
            is_deleted: node.is_deleted,
            location: node.location,
            node_class: node.node_class?.id,
            node_subtype: node.node_subtype?.id,
            core_attributes: node.core_attributes.map { attr in
                NodePropertyDTO(
                    id: attr.id,
                    node_class_property: attr.node_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value
                )
            },
            node_terminals: node.node_terminals.filter { !$0.is_deleted }.map { $0.toDTO() },
            com: node.com,
            com_calculation: node.com_calculation.map { COMCalculationDTO(from: $0) },
            qr_code: node.qr_code,
            serviceability: node.serviceability,
            serviceability_note: node.serviceability_note,
            voltage: node.voltage,
            voltage_id: node.voltage_id,
            secondary_voltage: node.secondary_voltage,
            secondary_voltage_id: node.secondary_voltage_id,
            notes: node.notes,
            room_id: node.room?.id,
            default_photo_id: node.default_photo_id,
            suggested_shortcut: node.suggested_shortcut_id,
            eqp_lib: node.eqp_lib
        )

        // ZP-2161 Phase 3b: thread every engineering field through to
        // the backend PUT payload. The DTO's primary init() pre-dates
        // these fields so we set them after construction rather than
        // bloating the init parameter list with another 30+ args.
        payload.tertiary_voltage = node.tertiary_voltage
        payload.tertiary_voltage_id = node.tertiary_voltage_id
        payload.system_voltage_id = node.system_voltage_id
        payload.circuit_voltage_id = node.circuit_voltage_id
        payload.voltage_user_overridden = node.voltage_user_overridden
        payload.applied_shortcut = node.applied_shortcut_id
        payload.eqp_lib_suggested = node.eqp_lib_suggested
        payload.eqp_note = node.eqp_note
        payload.eqp_engineering_approved = node.eqp_engineering_approved
        payload.skm_lib_name = node.skm_lib_name
        payload.skm_lib_name_suggested = node.skm_lib_name_suggested
        payload.ocr_signature = node.ocr_signature
        payload.kva_rating = node.kva_rating
        payload.percent_impedance = node.percent_impedance
        payload.mains_type_id = node.mains_type_id
        payload.phase_configuration_id = node.phase_configuration_id
        payload.ampere_rating = node.ampere_rating
        payload.pole_count = node.pole_count
        payload.manufacturer_id = node.manufacturer_id
        payload.has_trip_unit = node.has_trip_unit
        payload.trip_type_id = node.trip_type_id
        payload.frame_amps = node.frame_amps
        payload.sensor_amps = node.sensor_amps
        payload.plug_amps = node.plug_amps
        payload.length = node.length
        payload.conductor_material = node.conductor_material
        payload.cable_size_id = node.cable_size_id
        payload.conductor_configuration_id = node.conductor_configuration_id
        payload.duct_material_id = node.duct_material_id
        payload.conductor_description_id = node.conductor_description_id
        payload.insulation_class_id = node.insulation_class_id
        payload.insulation_type_id = node.insulation_type_id
        payload.installation_id = node.installation_id
        payload.busway_ampere_rating = node.busway_ampere_rating
        payload.replacement_cost = node.replacement_cost
        payload.panel_schedule_status = node.panel_schedule_status
        payload.rotation = node.rotation
        payload.locked = node.locked

        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)

        // Handle null parent_id, eqp_lib raw JSON, and merge extra data
        var jsonObj = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
        if node.parent_id == nil {
            jsonObj?["parent_id"] = NSNull()
        }
        // Explicitly send null for fields that JSONEncoder skips when nil
        if node.com == nil {
            jsonObj?["com"] = NSNull()
        }
        if node.com_calculation == nil {
            jsonObj?["com_calculation"] = NSNull()
        }
        // Replace eqp_lib string with raw JSON object so backend receives JSONB
        if let eqpStr = node.eqp_lib, let eqpData = eqpStr.data(using: .utf8),
           let eqpObj = try? JSONSerialization.jsonObject(with: eqpData) {
            jsonObj?["eqp_lib"] = eqpObj
        } else {
            jsonObj?["eqp_lib"] = NSNull()
        }

        // Merge in any extra data (e.g., force_type_change flag)
        for (key, value) in extraData {
            jsonObj?[key] = value
        }

        slog("updateNode", category: .api, data: [
            "node_id": node.id.uuidString,
            "parent_id": node.parent_id?.uuidString ?? "nil",
            "is_deleted": node.is_deleted,
            "core_attributes_count": node.core_attributes.count,
            "node_terminals_count": node.node_terminals.count
        ])

        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: jsonObj as Any)

        // 5) Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }

    /// Pre-builds the HTTP request body for a node update.
    /// Call this synchronously BEFORE creating async Tasks to avoid holding @Model references across await points.
    func buildNodeUpdatePayload(_ node: NodeV2, extraData: [String: Any] = [:]) throws -> Data {
        guard let sldId = node.sld?.id else {
            slog("buildNodeUpdatePayload: Node has no SLD relationship", category: .api, level: .error, data: ["node_id": node.id.uuidString])
            throw NSError(domain: "APIClient", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Node must have an SLD relationship to be updated"
            ])
        }

        var payload = SLDDTONode(
            id: node.id,
            type: node.type,
            label: node.label,
            sld_id: sldId,
            parent_id: node.parent_id,
            x: node.x,
            y: node.y,
            width: node.width,
            height: node.height,
            is_deleted: node.is_deleted,
            location: node.location,
            node_class: node.node_class?.id,
            node_subtype: node.node_subtype?.id,
            core_attributes: node.core_attributes.map { attr in
                NodePropertyDTO(
                    id: attr.id,
                    node_class_property: attr.node_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value
                )
            },
            node_terminals: node.node_terminals.filter { !$0.is_deleted }.map { $0.toDTO() },
            com: node.com,
            com_calculation: node.com_calculation.map { COMCalculationDTO(from: $0) },
            qr_code: node.qr_code,
            serviceability: node.serviceability,
            serviceability_note: node.serviceability_note,
            voltage: node.voltage,
            voltage_id: node.voltage_id,
            secondary_voltage: node.secondary_voltage,
            secondary_voltage_id: node.secondary_voltage_id,
            notes: node.notes,
            room_id: node.room?.id,
            default_photo_id: node.default_photo_id,
            suggested_shortcut: node.suggested_shortcut_id,
            eqp_lib: node.eqp_lib
        )

        // ZP-2161 Phase 3b: thread every engineering field through to
        // the backend PUT payload. The DTO's primary init() pre-dates
        // these fields so we set them after construction.
        payload.tertiary_voltage = node.tertiary_voltage
        payload.tertiary_voltage_id = node.tertiary_voltage_id
        payload.system_voltage_id = node.system_voltage_id
        payload.circuit_voltage_id = node.circuit_voltage_id
        payload.voltage_user_overridden = node.voltage_user_overridden
        payload.applied_shortcut = node.applied_shortcut_id
        payload.eqp_lib_suggested = node.eqp_lib_suggested
        payload.eqp_note = node.eqp_note
        payload.eqp_engineering_approved = node.eqp_engineering_approved
        payload.skm_lib_name = node.skm_lib_name
        payload.skm_lib_name_suggested = node.skm_lib_name_suggested
        payload.ocr_signature = node.ocr_signature
        payload.kva_rating = node.kva_rating
        payload.percent_impedance = node.percent_impedance
        payload.mains_type_id = node.mains_type_id
        payload.phase_configuration_id = node.phase_configuration_id
        payload.ampere_rating = node.ampere_rating
        payload.pole_count = node.pole_count
        payload.manufacturer_id = node.manufacturer_id
        payload.has_trip_unit = node.has_trip_unit
        payload.trip_type_id = node.trip_type_id
        payload.frame_amps = node.frame_amps
        payload.sensor_amps = node.sensor_amps
        payload.plug_amps = node.plug_amps
        payload.length = node.length
        payload.conductor_material = node.conductor_material
        payload.cable_size_id = node.cable_size_id
        payload.conductor_configuration_id = node.conductor_configuration_id
        payload.duct_material_id = node.duct_material_id
        payload.conductor_description_id = node.conductor_description_id
        payload.insulation_class_id = node.insulation_class_id
        payload.insulation_type_id = node.insulation_type_id
        payload.installation_id = node.installation_id
        payload.busway_ampere_rating = node.busway_ampere_rating
        payload.replacement_cost = node.replacement_cost
        payload.panel_schedule_status = node.panel_schedule_status
        payload.rotation = node.rotation
        payload.locked = node.locked

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)

        var jsonObj = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
        if node.parent_id == nil {
            jsonObj?["parent_id"] = NSNull()
        }
        // Explicitly send null for fields that JSONEncoder skips when nil
        if node.com == nil {
            jsonObj?["com"] = NSNull()
        }
        if node.com_calculation == nil {
            jsonObj?["com_calculation"] = NSNull()
        }
        // Replace eqp_lib string with raw JSON object
        if let eqpStr = node.eqp_lib, let eqpData = eqpStr.data(using: .utf8),
           let eqpObj = try? JSONSerialization.jsonObject(with: eqpData) {
            jsonObj?["eqp_lib"] = eqpObj
        } else {
            jsonObj?["eqp_lib"] = NSNull()
        }

        for (key, value) in extraData {
            jsonObj?[key] = value
        }

        slog("updateNode", category: .api, data: [
            "node_id": node.id.uuidString,
            "parent_id": node.parent_id?.uuidString ?? "nil",
            "is_deleted": node.is_deleted,
            "core_attributes_count": node.core_attributes.count
        ])

        // ZP-2161 — confirm the engineering scalars are on the wire
        func intStr(_ v: Int?) -> String { v.map(String.init) ?? "nil" }
        func dblStr(_ v: Double?) -> String { v.map { "\($0)" } ?? "nil" }

        var ocpLog: [String: Any] = [:]
        ocpLog["node_id"] = node.id.uuidString
        ocpLog["ampere_rating"] = intStr(node.ampere_rating)
        ocpLog["pole_count"] = intStr(node.pole_count)
        ocpLog["manufacturer_id"] = intStr(node.manufacturer_id)
        ocpLog["trip_type_id"] = intStr(node.trip_type_id)
        ocpLog["frame_amps"] = intStr(node.frame_amps)
        ocpLog["sensor_amps"] = intStr(node.sensor_amps)
        ocpLog["plug_amps"] = intStr(node.plug_amps)
        ocpLog["kva_rating"] = dblStr(node.kva_rating)
        ocpLog["percent_impedance"] = dblStr(node.percent_impedance)
        slog("updateNode engineering payload", category: .api, data: ocpLog)

        var cableLog: [String: Any] = [:]
        cableLog["node_id"] = node.id.uuidString
        cableLog["length"] = dblStr(node.length)
        cableLog["conductor_material"] = node.conductor_material ?? "nil"
        cableLog["cable_size_id"] = intStr(node.cable_size_id)
        cableLog["conductor_configuration_id"] = intStr(node.conductor_configuration_id)
        cableLog["conductor_description_id"] = intStr(node.conductor_description_id)
        cableLog["duct_material_id"] = intStr(node.duct_material_id)
        cableLog["insulation_class_id"] = intStr(node.insulation_class_id)
        cableLog["insulation_type_id"] = intStr(node.insulation_type_id)
        cableLog["installation_id"] = intStr(node.installation_id)
        cableLog["busway_ampere_rating"] = intStr(node.busway_ampere_rating)
        slog("updateNode cable/busway payload", category: .api, data: cableLog)

        return try JSONSerialization.data(withJSONObject: jsonObj as Any)
    }

    /// Sends a pre-built node update payload to the server.
    /// Safe to call from async Tasks without holding @Model references.
    func sendNodeUpdate(nodeId: UUID, payload: Data) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Node.update(nodeId: nodeId.uuidString))
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = payload
        return try await executeRequestRaw(request)
    }

    // MARK: - EDGE ROUTES
    func createEdge(edge: EdgeV2) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Edge.create)
        
        // 2) Ensure edge has an SLD relationship
        guard let sldId = edge.sld?.id else {
            slog("createEdge: Edge has no SLD relationship", category: .api, level: .error, data: ["edge_id": edge.id.uuidString])
            throw NSError(domain: "APIClient", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Edge must have an SLD relationship to be created"
            ])
        }
        
        // 3) Create the DTO payload
        let payload = SLDDTOEdge(
            id: edge.id,
            source: edge.source,
            target: edge.target,
            sld_id: sldId,
            source_handle: edge.sourceHandle,
            target_handle: edge.targetHandle,
            source_node_terminal_id: edge.sourceNodeTerminalId,
            target_node_terminal_id: edge.targetNodeTerminalId,
            is_deleted: edge.is_deleted,
            edge_class: edge.edge_class?.id,
            core_attributes: edge.core_attributes.map { attr in
                EdgePropertyDTO(
                    id: attr.id,
                    edge_class_property: attr.edge_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value
                )
            },
            points: edge.points,
            algorithm: edge.algorithm
        )
        
        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateEdge(_ edge: EdgeV2) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Edge.update(edgeId: edge.id.uuidString))
        
        // 2) Ensure edge has an SLD relationship
        guard let sldId = edge.sld?.id else {
            slog("updateEdge: Edge has no SLD relationship", category: .api, level: .error, data: ["edge_id": edge.id.uuidString])
            throw NSError(domain: "APIClient", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Edge must have an SLD relationship to be updated"
            ])
        }
        
        // 3) Create the DTO payload
        let payload = SLDDTOEdge(
            id: edge.id,
            source: edge.source,
            target: edge.target,
            sld_id: sldId,
            source_handle: edge.sourceHandle,
            target_handle: edge.targetHandle,
            source_node_terminal_id: edge.sourceNodeTerminalId,
            target_node_terminal_id: edge.targetNodeTerminalId,
            is_deleted: edge.is_deleted,
            edge_class: edge.edge_class?.id, // Include the edge_class field
            core_attributes: edge.core_attributes.map { attr in
                EdgePropertyDTO(
                    id: attr.id,
                    edge_class_property: attr.edge_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value
                )
            },
            af_completion: edge.af_isComplete,
            points: edge.points,
            algorithm: edge.algorithm
        )
        
        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        let jsonObj = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
        
        slog("updateEdge", category: .api, data: [
            "edge_id": edge.id.uuidString,
            "source": edge.source?.uuidString ?? "nil",
            "target": edge.target?.uuidString ?? "nil",
            "is_deleted": edge.is_deleted,
            "points_count": edge.points?.count ?? 0,
            "algorithm": edge.algorithm ?? "nil"
        ])
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: jsonObj as Any)
        
        // 5) Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }

    /// Pre-builds the HTTP request body for an edge update.
    /// Call this synchronously BEFORE creating async Tasks to avoid holding @Model references across await points.
    func buildEdgeUpdatePayload(_ edge: EdgeV2) throws -> Data {
        guard let sldId = edge.sld?.id else {
            slog("buildEdgeUpdatePayload: Edge has no SLD relationship", category: .api, level: .error, data: ["edge_id": edge.id.uuidString])
            throw NSError(domain: "APIClient", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Edge must have an SLD relationship to be updated"
            ])
        }

        let payload = SLDDTOEdge(
            id: edge.id,
            source: edge.source,
            target: edge.target,
            sld_id: sldId,
            source_handle: edge.sourceHandle,
            target_handle: edge.targetHandle,
            source_node_terminal_id: edge.sourceNodeTerminalId,
            target_node_terminal_id: edge.targetNodeTerminalId,
            is_deleted: edge.is_deleted,
            edge_class: edge.edge_class?.id,
            core_attributes: edge.core_attributes.map { attr in
                EdgePropertyDTO(
                    id: attr.id,
                    edge_class_property: attr.edge_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value
                )
            },
            af_completion: edge.af_isComplete,
            points: edge.points,
            algorithm: edge.algorithm
        )

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        let jsonObj = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]

        slog("updateEdge", category: .api, data: [
            "edge_id": edge.id.uuidString,
            "source": edge.source?.uuidString ?? "nil",
            "target": edge.target?.uuidString ?? "nil",
            "is_deleted": edge.is_deleted,
            "points_count": edge.points?.count ?? 0,
            "algorithm": edge.algorithm ?? "nil"
        ])

        return try JSONSerialization.data(withJSONObject: jsonObj as Any)
    }

    /// Sends a pre-built edge update payload to the server.
    /// Safe to call from async Tasks without holding @Model references.
    func sendEdgeUpdate(edgeId: UUID, payload: Data) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Edge.update(edgeId: edgeId.uuidString))
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = payload
        return try await executeRequestRaw(request)
    }

    // MARK: - PHOTO ROUTES
    func createPhoto(photo: Photo) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Photo.create)
        
        // 2) Determine entity_id based on photo type and association
        let entityId: UUID?
        if photo.type.hasPrefix("task_") {
            entityId = photo.userTask?.id
        } else if photo.type.hasPrefix("issue") {
            entityId = photo.issue?.id
        } else if photo.type == "building" {
            entityId = photo.building?.id
        } else if photo.type == "floor" {
            entityId = photo.floor?.id
        } else if photo.type == "room" {
            entityId = photo.room?.id
        } else {
            entityId = photo.node?.id
        }
        
        // 3) Create the DTO payload
        let payload = SLDDTOPhoto(
            id: photo.id,
            entity_id: entityId,
            url: photo.url,
            type: photo.type,
            sld_id: photo.sld?.id,
            filename: photo.filename,
            local_filepath: photo.local_filepath,
            upload_needed: false,
            is_deleted: false,
            caption: photo.caption
        )
        
        // 4) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        
        // 5) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createPhoto()", category: .photo, data: [
            "url": url.absoluteString,
            "photo_id": photo.id.uuidString,
            "entity_id": entityId?.uuidString ?? "nil",
            "type": photo.type
        ])
        
        // 6) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updatePhoto(_ photo: Photo) async throws -> URLResponse {
        // Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Photo.update(photoId: photo.id.uuidString))
        
        let entityId: UUID?
        if photo.type.hasPrefix("task_") {
            entityId = photo.userTask?.id
        } else if photo.type.hasPrefix("issue") {
            entityId = photo.issue?.id
        } else if photo.type == "building" {
            entityId = photo.building?.id
        } else if photo.type == "floor" {
            entityId = photo.floor?.id
        } else if photo.type == "room" {
            entityId = photo.room?.id
        } else {
            entityId = photo.node?.id
        }
        
        // Create the DTO payload
        let payload = SLDDTOPhoto(
            id: photo.id,
            entity_id: entityId,
            url: photo.url,
            type: photo.type,
            filename: photo.filename,
            local_filepath: photo.local_filepath,
            upload_needed: false, // Always false - we're actively uploading/updating now!
            is_deleted: photo.is_deleted,
            caption: photo.caption
        )
        
        slog("updatePhoto", category: .photo, data: [
            "photo_id": photo.id.uuidString,
            "url": url.absoluteString,
            "entity_id": entityId?.uuidString ?? "nil",
            "type": photo.type
        ])
        
        // Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        
        // Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    // MARK: - USER TASK ROUTES
    func createTask(task: UserTask) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Task.create)
        
        // 2) Ensure task has an SLD relationship
        guard let sldId = task.sld?.id else {
            slog("createTask: Task has no SLD relationship", category: .task, level: .error, data: ["task_id": task.id.uuidString])
            throw NSError(domain: "APIClient", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Task must have an SLD relationship to be created"
            ])
        }
        
        // 3) Create the DTO payload
        let payload = UserTaskDTO(
            id: task.id,
            title: task.title,
            task_description: task.task_description,
            completed: task.completed,
            form_id: task.form?.id,
            node_id: task.node?.id,
            sld_id: sldId,
            is_deleted: task.is_deleted,
            submission: task.submission,
            submitted_at: task.submitted_at,
            due_date: task.due_date,
            created_at: task.created_at,
            task_type: task.task_type,
            interval: task.interval,
            recurring: task.recurring,
            procedure_id: task.procedure_id,
            shortcut_id: task.shortcut_id
        )
        
        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createTask()", category: .task, data: [
            "url": url.absoluteString,
            "task_id": task.id.uuidString,
            "sld_id": sldId.uuidString
        ])
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateTask(_ task: UserTask) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Task.update(taskId: task.id.uuidString))
        
        // 2) Ensure task has an SLD relationship
        guard let sldId = task.sld?.id else {
            slog("updateTask: Task has no SLD relationship", category: .task, level: .error, data: ["task_id": task.id.uuidString])
            throw NSError(domain: "APIClient", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Task must have an SLD relationship to be updated"
            ])
        }
        
        // 3) Create the DTO payload
        let payload = UserTaskDTO(
            id: task.id,
            title: task.title,
            task_description: task.task_description,
            completed: task.completed,
            form_id: task.form?.id,
            node_id: task.node?.id,
            sld_id: sldId,
            is_deleted: task.is_deleted,
            submission: task.submission,
            submitted_at: task.submitted_at,
            due_date: task.due_date,
            created_at: task.created_at,
            task_type: task.task_type,
            interval: task.interval,
            recurring: task.recurring,
            procedure_id: task.procedure_id,
            shortcut_id: task.shortcut_id
        )
        
        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        let jsonObj = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
        
        slog("updateTask", category: .task, data: [
            "task_id": task.id.uuidString,
            "title": task.title,
            "completed": task.completed,
            "is_deleted": task.is_deleted,
            "node_id": task.node?.id.uuidString ?? "nil",
            "form_id": task.form?.id.uuidString ?? "nil"
        ])
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: jsonObj as Any)
        
        // 5) Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    // MARK: - IR PHOTO SESSION ROUTES
    func createIRSession(irSession: IRSession) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.IRSession.create)

        // 2) Create the DTO payload — always send equipment_ids (lowercase UUIDs to match Android/backend)
        let equipmentIdStrings = irSession.equipmentIds.map { $0.uuidString.lowercased() }
        let payload = IRSessionDTO(
            id: irSession.id,
            name: irSession.name,
            photo_type: irSession.photo_type,
            active_visual_prefix: irSession.active_visual_prefix,
            active_ir_prefix: irSession.active_ir_prefix,
            date_created: irSession.date_created,
            date_closed: irSession.date_closed,
            active: irSession.active,
            sld_id: irSession.sld.id,
            is_deleted: irSession.is_deleted,
            equipment_ids: equipmentIdStrings
        )

        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)

        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        slog("createIRSession()", category: .api, data: [
            "url": url.absoluteString,
            "session_id": irSession.id.uuidString,
            "sld_id": irSession.sld.id.uuidString
        ])

        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }

    func updateIRSession(_ irSession: IRSession) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.IRSession.update(sessionId: irSession.id.uuidString))

        // 2) Create the DTO payload — always send equipment_ids (lowercase UUIDs to match Android/backend)
        let equipmentIdStrings = irSession.equipmentIds.map { $0.uuidString.lowercased() }
        let payload = IRSessionDTO(
            id: irSession.id,
            name: irSession.name,
            photo_type: irSession.photo_type,
            active_visual_prefix: irSession.active_visual_prefix,
            active_ir_prefix: irSession.active_ir_prefix,
            date_created: irSession.date_created,
            date_closed: irSession.date_closed,
            active: irSession.active,
            sld_id: irSession.sld.id,
            is_deleted: irSession.is_deleted,
            equipment_ids: equipmentIdStrings
        )

        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        let jsonObj = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]

        slog("updateIRSession", category: .api, data: [
            "session_id": irSession.id.uuidString,
            "photo_type": irSession.photo_type,
            "active": irSession.active,
            "date_closed": irSession.date_closed?.description ?? "nil"
        ])
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: jsonObj as Any)
        
        // 5) Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    // MARK: - IR PHOTO ROUTES
    func createIRPhoto(irPhoto: IRPhoto) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.IRPhoto.create)
        
        // 2) Create the DTO payload
        let payload = IRPhotoDTO(
            id: irPhoto.id,
            ir_session_id: irPhoto.ir_session?.id,
            node_id: irPhoto.node.id,
            visual_photo_key: irPhoto.visual_photo_key,
            ir_photo_key: irPhoto.ir_photo_key,
            date_created: irPhoto.date_created,
            sld_id: irPhoto.sld.id,
            issue_id: irPhoto.issue?.id,
            is_deleted: irPhoto.is_deleted
        )
        
        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createIRPhoto()", category: .photo, data: [
            "url": url.absoluteString,
            "ir_photo_id": irPhoto.id.uuidString,
            "node_id": irPhoto.node.id.uuidString,
            "sld_id": irPhoto.sld.id.uuidString
        ])
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateIRPhoto(_ irPhoto: IRPhoto) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.IRPhoto.update(photoId: irPhoto.id.uuidString))
        
        // 2) Create a dictionary payload to ensure nil values are included
        // NOTE::: DTO not being used here to force nil for issue_id and ir_session_id if being cleared out
        // IMPORTANT: Use NSNull() for nil values to ensure they're sent as JSON null
        let payload: [String: Any] = [
            "id": irPhoto.id.uuidString,
            "ir_session_id": irPhoto.ir_session?.id.uuidString ?? NSNull(),
            "issue_id": irPhoto.issue?.id.uuidString ?? NSNull(),
            "node_id": irPhoto.node.id.uuidString,
            "visual_photo_key": irPhoto.visual_photo_key,
            "ir_photo_key": irPhoto.ir_photo_key,
            "date_created": ISO8601DateFormatter().string(from: irPhoto.date_created),
            "sld_id": irPhoto.sld.id.uuidString,
            "is_deleted": irPhoto.is_deleted
        ]
        
        // 3) Manually encode to JSON using JSONSerialization to preserve null values
        let bodyData = try JSONSerialization.data(withJSONObject: payload, options: [])
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("updateIRPhoto()", category: .photo, data: [
            "url": url.absoluteString,
            "ir_photo_id": irPhoto.id.uuidString,
            "issue_id": irPhoto.issue?.id.uuidString ?? "null (clearing foreign key)"
        ])
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    /// Fetch full session details including IR photos.
    /// Endpoint: GET /ir_session/{sessionId}/full
    func fetchIRSessionFull(sessionId: UUID) async throws -> IRSessionFullResponse {
        let url = baseURL.appendingPathComponent(
            APIEndpoints.IRSession.full(sessionId: sessionId.uuidString.lowercased())
        )

        let request = await createAuthorizedRequest(url: url, method: "GET")

        slog("fetchIRSessionFull()", category: .photo, data: [
            "url": url.absoluteString,
            "session_id": sessionId.uuidString
        ])

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try await executeRequest(request, decoder: decoder)
    }

    // MARK: - QUOTE ROUTES
    func createQuote(quote: Quote) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Quote.create)
        
        // 2) Create the DTO payload
        let payload = QuoteDTO(
            id: quote.id,
            title: quote.title,
            sow: quote.sow,
            tnm: quote.tnm,
            sld_id: quote.sld?.id,
            description: quote.quoteDescription,
            status: quote.status,
            is_deleted: quote.is_deleted
        )
        
        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createQuote()", category: .api, data: [
            "url": url.absoluteString,
            "quote_id": quote.id.uuidString
        ])
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateQuote(_ quote: Quote) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Quote.update(quoteId: quote.id.uuidString))
        
        // 2) Create the DTO payload
        let payload = QuoteDTO(
            id: quote.id,
            title: quote.title,
            sow: quote.sow,
            tnm: quote.tnm,
            sld_id: quote.sld?.id,
            description: quote.quoteDescription,
            status: quote.status,
            is_deleted: quote.is_deleted
        )
        
        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)
        let jsonObj = try JSONSerialization.jsonObject(with: bodyData) as? [String: Any]
        
        slog("updateQuote", category: .api, data: [
            "quote_id": quote.id.uuidString,
            "title": quote.title,
            "status": quote.status ?? "nil",
            "is_deleted": quote.is_deleted
        ])
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: jsonObj as Any)
        
        // 5) Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    // MARK: - ISSUE ROUTES

    func fetchIssueStatusHistory(issueId: UUID) async throws -> [IssueStatusHistoryDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Issue.statusHistory(issueId: issueId.uuidString))
        let request = await createAuthorizedRequest(url: url, method: "GET")

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        struct StatusHistoryResponse: Decodable {
            let success: Bool
            let data: [IssueStatusHistoryDTO]
        }

        let response: StatusHistoryResponse = try await executeRequest(request, decoder: decoder)
        return response.data
    }

    func createIssue(issue: Issue) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Issue.create)
        
        // 2) Create the DTO payload
        let payload = IssueDTO(
            id: issue.id,
            title: issue.title,
            description: issue.issueDescription,
            created_date: issue.created_date,
            node_id: issue.node?.id,
            issue_class: issue.issue_class?.id,
            issue_type: issue.issue_type,
            issue_subtype: issue.issue_subtype,
            is_deleted: issue.is_deleted,
            session_id: issue.session?.id,
            sld_id: issue.sld?.id,
            details: issue.details.map { attr in
                IssuePropertyDTO(
                    id: attr.id,
                    issue_class_property: attr.issue_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value,
                    unit: attr.unit,
                    description: attr.attributeNotes
                )
            },
            status: issue.status,
            proposed_resolution: issue.proposed_resolution,
            modified_date: issue.modified_date,
            priority: issue.priority,
            immediate_hazard: issue.immediateHazard,
            customer_notified: issue.customerNotified
        )

        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)

        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createIssue()", category: .api, data: [
            "url": url.absoluteString,
            "issue_id": issue.id.uuidString,
            "priority": payload.priority ?? "nil"
        ])
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateIssue(_ issue: Issue) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Issue.update(issueId: issue.id.uuidString))
        
        // 2) Create the DTO payload
        let payload = IssueDTO(
            id: issue.id,
            title: issue.title,
            description: issue.issueDescription,
            created_date: issue.created_date,
            node_id: issue.node?.id,
            issue_class: issue.issue_class?.id,
            issue_type: issue.issue_type,
            issue_subtype: issue.issue_subtype,
            is_deleted: issue.is_deleted,
            session_id: issue.session?.id,
            sld_id: issue.sld?.id,
            details: issue.details.map { attr in
                IssuePropertyDTO(
                    id: attr.id,
                    issue_class_property: attr.issue_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value,
                    unit: attr.unit,
                    description: attr.attributeNotes
                )
            },
            status: issue.status,
            proposed_resolution: issue.proposed_resolution,
            modified_date: issue.modified_date,
            priority: issue.priority,
            immediate_hazard: issue.immediateHazard,
            customer_notified: issue.customerNotified
        )

        // 3) Encode to JSON
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let bodyData = try encoder.encode(payload)

        slog("updateIssue", category: .api, data: [
            "url": url.absoluteString,
            "issue_id": issue.id.uuidString,
            "title": issue.title ?? "nil",
            "type": issue.issue_type ?? "nil",
            "status": issue.status ?? "nil",
            "is_deleted": issue.is_deleted
        ])
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // 5) Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    // MARK: - MAPPING ROUTES
    func createIssueTaskMapping(issueId: UUID, taskId: UUID) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.issueTaskCreate)
        
        // 2) Create the payload
        let payload = [
            "issue_id": issueId.uuidString,
            "task_id": taskId.uuidString
        ]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        slog("createIssueTaskMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func createTaskSessionMapping(taskId: UUID, sessionId: UUID) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskSessionCreate)
        
        // 2) Create the payload
        let payload = [
            "task_id": taskId.uuidString,
            "session_id": sessionId.uuidString
        ]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "createTaskSessionMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func createQuoteTaskMapping(quoteId: UUID, taskId: UUID) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.quoteTaskCreate)
        
        // 2) Create the payload
        let payload = [
            "quote_id": quoteId.uuidString,
            "task_id": taskId.uuidString
        ]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "createQuoteTaskMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func createUserTaskMapping(userId: UUID, taskId: UUID, mappingType: String) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.userTaskCreate)
        
        let payload = [
            "user_id": userId.uuidString,
            "task_id": taskId.uuidString,
            "mapping_type": mappingType
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        AppLogger.log(.info, "createUserTaskMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        return try await executeRequestRaw(request)
    }
    
    // MARK: - UPDATE MAPPING ROUTES (for soft delete)
    func updateIssueTaskMapping(issueId: UUID, taskId: UUID, isDeleted: Bool) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.issueTaskUpdate(issueId: issueId.uuidString, taskId: taskId.uuidString))
        
        // 2) Create the payload
        let payload = ["is_deleted": isDeleted]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "updateIssueTaskMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateTaskSessionMapping(taskId: UUID, sessionId: UUID, isDeleted: Bool) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskSessionUpdate(taskId: taskId.uuidString, sessionId: sessionId.uuidString))
        
        // 2) Create the payload
        let payload = ["is_deleted": isDeleted]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "updateTaskSessionMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateQuoteTaskMapping(quoteId: UUID, taskId: UUID, isDeleted: Bool) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.quoteTaskUpdate(quoteId: quoteId.uuidString, taskId: taskId.uuidString))
        
        // 2) Create the payload
        let payload = ["is_deleted": isDeleted]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "updateQuoteTaskMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateUserTaskMapping(userId: UUID, taskId: UUID, isDeleted: Bool) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.userTaskUpdate(userId: userId.uuidString, taskId: taskId.uuidString))
        
        // 2) Create the payload
        let payload = ["is_deleted": isDeleted]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "updateUserTaskMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    // MARK: - Task-Node Mapping Methods
    func createTaskNodeMapping(taskId: UUID, nodeId: UUID, isCompleted: Bool? = nil) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskNodeCreate)
        
        // 2) Create the payload
        var payload: [String: Any] = [
            "task_id": taskId.uuidString,
            "node_id": nodeId.uuidString
        ]
        if let isCompleted = isCompleted {
            payload["is_completed"] = isCompleted
        }
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "createTaskNodeMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateTaskNodeMapping(taskId: UUID, nodeId: UUID, isDeleted: Bool, isCompleted: Bool? = nil) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskNodeUpdate(taskId: taskId.uuidString, nodeId: nodeId.uuidString))
        
        // 2) Create the payload - include task_id and node_id for mutation handler
        var payload: [String: Any] = [
            "task_id": taskId.uuidString,
            "node_id": nodeId.uuidString,
            "is_deleted": isDeleted
        ]
        if let isCompleted = isCompleted {
            payload["is_completed"] = isCompleted
        }
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "updateTaskNodeMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    /// Bulk update completion status for multiple task-node mappings
    func bulkUpdateTaskNodeCompletions(taskId: UUID, completions: [(nodeId: UUID, isCompleted: Bool)]) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskNodeCompletion)
        
        // 2) Create the payload
        let completionsArray = completions.map { completion in
            [
                "node_id": completion.nodeId.uuidString,
                "is_completed": completion.isCompleted
            ] as [String: Any]
        }
        
        let payload: [String: Any] = [
            "task_id": taskId.uuidString,
            "completions": completionsArray
        ]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "bulkUpdateTaskNodeCompletions()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    // MARK: - Node-Session Mapping Methods
    func createNodeSessionMapping(nodeId: UUID, sessionId: UUID) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.nodeSessionCreate)
        
        // 2) Create the payload
        let payload = [
            "id": UUID().uuidString,
            "node_id": nodeId.uuidString,
            "session_id": sessionId.uuidString,
            "is_deleted": false
        ] as [String : Any]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "createNodeSessionMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func updateNodeSessionMapping(nodeId: UUID, sessionId: UUID, isDeleted: Bool) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.nodeSessionUpdate(nodeId: nodeId.uuidString, sessionId: sessionId.uuidString))
        
        // 2) Create the payload
        let payload = ["is_deleted": isDeleted]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "updateNodeSessionMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    // MARK: - User Methods
    func fetchCompanyUsers(companyId: String) async throws -> [CompanyUser] {
        let url = baseURL.appendingPathComponent(APIEndpoints.User.companyUsers(companyId: companyId))
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        AppLogger.log(.info, "fetchCompanyUsers()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        
        let users: [CompanyUser] = try await executeRequest(request)
        return users
    }
    
    // MARK: - User-Session Mapping Methods
    func createUserSessionMapping(id: UUID, userId: UUID, sessionId: UUID, mappingType: String) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.userSessionCreate)
        
        let payload = [
            "id": id.uuidString,
            "user_id": userId.uuidString,
            "session_id": sessionId.uuidString,
            "mapping_type": mappingType,
            "is_deleted": false
        ] as [String : Any]
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        AppLogger.log(.info, "createUserSessionMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        return try await executeRequestRaw(request)
    }
    
    func updateUserSessionMapping(mappingId: UUID, mappingType: String?, isDeleted: Bool) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.userSessionUpdate(mappingId: mappingId.uuidString))
        
        var payload: [String: Any] = ["is_deleted": isDeleted]
        if let mappingType = mappingType {
            payload["mapping_type"] = mappingType
        }
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        AppLogger.log(.info, "updateUserSessionMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        return try await executeRequestRaw(request)
    }
    
    // MARK: - Task-Form Mapping Methods
    func createTaskFormMapping(taskId: UUID, formId: UUID) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskFormCreate)
        
        // 2) Create the payload
        let payload = [
            "task_id": taskId.uuidString,
            "form_id": formId.uuidString
        ]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "createTaskFormMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    // MARK: - Batch Task-Node/Form Operations
    
    func batchUpdateTaskNodes(taskId: UUID, nodeIds: [UUID], operation: String = "set") async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskNodeBatch)
        
        // 2) Create the payload
        let payload: [String: Any] = [
            "task_id": taskId.uuidString,
            "node_ids": nodeIds.map { $0.uuidString },
            "operation": operation // "set", "add", or "remove"
        ]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "batchUpdateTaskNodes()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func batchUpdateTaskForms(taskId: UUID, formIds: [UUID], operation: String = "set") async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskFormBatch)
        
        // 2) Create the payload
        let payload: [String: Any] = [
            "task_id": taskId.uuidString,
            "form_ids": formIds.map { $0.uuidString },
            "operation": operation // "set", "add", or "remove"
        ]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "batchUpdateTaskForms()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    func getTaskNodes(taskId: UUID) async throws -> [MappingTaskNodeDTO] {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskNodes(taskId: taskId.uuidString))
        
        // 2) Build authorized request and fire it
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        // 3) Decode the response
        let decoder = JSONDecoder()
        struct ResponseWrapper: Codable {
            let mappings: [MappingTaskNodeDTO]
        }
        let wrapper: ResponseWrapper = try await executeRequest(request, decoder: decoder)
        return wrapper.mappings
    }
    
    func getTaskForms(taskId: UUID) async throws -> [MappingTaskFormDTO] {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskForms(taskId: taskId.uuidString))
        
        // 2) Build authorized request and fire it
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        // 3) Decode the response
        let decoder = JSONDecoder()
        struct ResponseWrapper: Codable {
            let mappings: [MappingTaskFormDTO]
        }
        let wrapper: ResponseWrapper = try await executeRequest(request, decoder: decoder)
        return wrapper.mappings
    }
    
    func updateTaskFormMapping(taskId: UUID, formId: UUID, isDeleted: Bool) async throws -> URLResponse {
        // 1) Build the URL
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskFormUpdate(taskId: taskId.uuidString, formId: formId.uuidString))
        
        // 2) Create the payload - explicitly handle is_deleted
        let payload = ["is_deleted": isDeleted]
        
        // 3) Encode to JSON
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        // 4) Build URLRequest with authorization
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // DEBUG LOGGING
        AppLogger.log(.info, "updateTaskFormMapping()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        // 5) Fire the request with auto-refresh
        return try await executeRequestRaw(request)
    }
    
    // MARK: - Get Task Mappings V2 (with better error handling)
    
    func getTaskNodesV2(taskId: UUID) async throws -> [MappingTaskNodeDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskNodes(taskId: taskId.uuidString))
        
        AppLogger.log(.info, "getTaskNodesV2()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        let result: MappingResponse<[MappingTaskNodeDTO]> = try await executeRequest(request, decoder: decoder)
        return result.mappings ?? []
    }
    
    func getTaskFormsV2(taskId: UUID) async throws -> [MappingTaskFormDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskForms(taskId: taskId.uuidString))
        
        AppLogger.log(.info, "getTaskFormsV2()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        let result: MappingResponse<[MappingTaskFormDTO]> = try await executeRequest(request, decoder: decoder)
        return result.mappings ?? []
    }
}

// Helper struct for decoding mapping responses
private struct MappingResponse<T: Decodable>: Decodable {
 let success: Bool
 let mappings: T?
}


// MARK: - Form Instance Endpoints

extension APIClient {
    
    // Create form instance
    func createFormInstance(_ dto: FormInstanceDTO) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.FormInstance.create)
        
        let encoder = JSONEncoder()
        let bodyData = try encoder.encode(dto)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    // Get form instance
    func getFormInstance(id: UUID) async throws -> FormInstanceDTO {
        let url = baseURL.appendingPathComponent(APIEndpoints.FormInstance.get(instanceId: id.uuidString))
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        return try await executeRequest(request, decoder: decoder)
    }
    
    // Update form instance
    func updateFormInstance(id: UUID, dto: FormInstanceDTO) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.FormInstance.update(instanceId: id.uuidString))

        let encoder = JSONEncoder()
        let bodyData = try encoder.encode(dto)

        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        // Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }

    // MARK: - EG Form Instance Endpoints (ZP-1723)
    // Parallel to FormInstance above. Backend strips base64 from
    // form_submission on responses, but client POSTs/PUTs still ship
    // base64; backend extract_and_upload_media handles S3 conversion.

    func createEGFormInstance(_ body: EGFormInstanceCreateBody) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.EGFormInstance.create)
        let bodyData = try JSONEncoder().encode(body)
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        // Bypass the BE mutation inbox — iOS runs its own offline
        // queue, so going through the server-side inbox would double-
        // queue and break the synchronous "instance object" response
        // shape the callers rely on.
        request.setValue("true", forHTTPHeaderField: "X-Direct-Write")
        request.httpBody = bodyData
        return try await executeRequestRaw(request)
    }

    func getEGFormInstance(id: UUID) async throws -> EGFormInstanceDTO {
        let url = baseURL.appendingPathComponent(APIEndpoints.EGFormInstance.get(instanceId: id.uuidString))
        let request = await createAuthorizedRequest(url: url, method: "GET")
        return try await executeRequest(request, decoder: JSONDecoder())
    }

    func updateEGFormInstance(id: UUID, body: EGFormInstanceUpdateBody) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.EGFormInstance.update(instanceId: id.uuidString))
        let bodyData = try JSONEncoder().encode(body)
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("true", forHTTPHeaderField: "X-Direct-Write")
        request.httpBody = bodyData
        return try await executeRequestRaw(request)
    }
}

// Backend's POST /eg-form-instance/ and PUT /eg-form-instance/<id> have
// different body shapes (POST takes single task_id + node_id link;
// PUT takes node_ids array for full sync). Modeling each explicitly
// keeps wire shapes obvious and lets us drop unused fields.

struct EGFormInstanceCreateBody: Codable {
    // Send the row's UUID so backend uses it instead of minting a new
    // one — keeps iOS and server agreeing on the id across the create →
    // later-update flow (especially offline-queued creates).
    let id: String
    let eg_form_id: String
    let form_submission: AnyCodable?
    let submitted: Bool?
    let task_id: String?
    let node_id: String?
}

struct EGFormInstanceUpdateBody: Codable {
    let form_submission: AnyCodable?
    let submitted: Bool?
    let is_deleted: Bool?
    let node_ids: [String]?
}

extension APIClient {
    
    // Form Instance-Node Mapping
    func createFormInstanceNodeMapping(formInstanceId: UUID, nodeId: UUID) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.formInstanceNodeCreate)
        
        let body: [String: Any] = [
            "form_instance_id": formInstanceId.uuidString,
            "node_id": nodeId.uuidString
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: body)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        return try await executeRequestRaw(request)
    }
    
    func updateFormInstanceNodeMapping(formInstanceId: UUID, nodeId: UUID, isDeleted: Bool) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.formInstanceNodeUpdate(formInstanceId: formInstanceId.uuidString, nodeId: nodeId.uuidString))
        
        let body: [String: Any] = ["is_deleted": isDeleted]
        let bodyData = try JSONSerialization.data(withJSONObject: body)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        return try await executeRequestRaw(request)
    }
    
    func getFormInstanceNodes(formInstanceId: UUID) async throws -> [MappingFormInstanceNodeDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.formInstanceNodes(formInstanceId: formInstanceId.uuidString))
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        
        // The endpoint should return an array of mappings
        struct ResponseItem: Decodable {
            let mapping: MappingFormInstanceNodeDTO
        }
        
        let items: [ResponseItem] = try await executeRequest(request, decoder: decoder)
        return items.map { $0.mapping }
    }
    
    // Task-Form Instance Mapping
    func createTaskFormInstanceMapping(taskId: UUID, formInstanceId: UUID) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskFormInstanceCreate)
        
        let body: [String: Any] = [
            "task_id": taskId.uuidString,
            "form_instance_id": formInstanceId.uuidString
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: body)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    func updateTaskFormInstanceMapping(taskId: UUID, formInstanceId: UUID, isDeleted: Bool) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskFormInstanceUpdate(taskId: taskId.uuidString, formInstanceId: formInstanceId.uuidString))
        
        slog("updateTaskFormInstanceMapping", category: .api, data: [
            "task_id": taskId.uuidString,
            "form_instance_id": formInstanceId.uuidString,
            "is_deleted": isDeleted
        ])
        
        let body: [String: Any] = ["is_deleted": isDeleted]
        let bodyData = try JSONSerialization.data(withJSONObject: body)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    func batchUpdateTaskFormInstances(taskId: UUID, formInstanceIds: [UUID], operation: String) async throws -> [MappingTaskFormInstanceDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskFormInstanceBatch)
        
        AppLogger.log(.info, "API REQUEST: POST \(url.absoluteString)", category: .api)
        AppLogger.log(.info, "Operation: \(operation)", category: .api)
        AppLogger.log(.info, "Task ID: \(taskId.uuidString)", category: .api)
        AppLogger.log(.info, "Form Instance IDs: \(formInstanceIds.count) items", category: .api)
        for id in formInstanceIds {
            AppLogger.log(.info, "\(id.uuidString)", category: .api)
        }
        
        let body: [String: Any] = [
            "task_id": taskId.uuidString,
            "form_instance_ids": formInstanceIds.map { $0.uuidString },
            "operation": operation
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: body)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        let decoder = JSONDecoder()
        let result: [MappingTaskFormInstanceDTO] = try await executeRequest(request, decoder: decoder)
        
        AppLogger.log(.info, "API SUCCESS: Batch updated \(result.count) Task-FormInstance mappings", category: .api)
        for mapping in result {
            AppLogger.log(.info, "form_instance=\(mapping.form_instance_id), deleted=\(mapping.is_deleted ?? false)", category: .api)
        }
        
        return result
    }
    
    func getTaskFormInstances(taskId: UUID) async throws -> [MappingTaskFormInstanceDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskFormInstances(taskId: taskId.uuidString))
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        let decoder = JSONDecoder()
        
        // The endpoint returns an array of objects with mapping and form_instance
        struct ResponseItem: Decodable {
            let mapping: MappingTaskFormInstanceDTO
            let form_instance: FormInstanceDTO
        }
        
        let items: [ResponseItem] = try await executeRequest(request, decoder: decoder)
        return items.map { $0.mapping }
    }
    
    func createFormInstanceAndLink(taskId: UUID, formMasterId: UUID, formInstanceId: UUID, nodeIds: [UUID]) async throws -> FormInstanceDTO {
        let url = baseURL.appendingPathComponent(APIEndpoints.Mapping.taskFormInstanceCreateAndLink)
        
        let body: [String: Any] = [
            "task_id": taskId.uuidString,
            "form_master_id": formMasterId.uuidString,
            "form_instance_id": formInstanceId.uuidString,
            "node_ids": nodeIds.map { $0.uuidString }
        ]
        
        AppLogger.log(.info, "API REQUEST: POST \(url.absoluteString)", category: .api)
        AppLogger.log(.info, "Task ID: \(taskId.uuidString)", category: .api)
        AppLogger.log(.info, "Form Master ID: \(formMasterId.uuidString)", category: .api)
        AppLogger.log(.info, "Form Instance ID: \(formInstanceId.uuidString)", category: .api)
        AppLogger.log(.info, "Node IDs: \(nodeIds.count) items", category: .api)
        for nodeId in nodeIds {
            AppLogger.log(.info, "\(nodeId.uuidString)", category: .api)
        }
        
        let bodyData = try JSONSerialization.data(withJSONObject: body)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        let decoder = JSONDecoder()
        
        struct Response: Decodable {
            let form_instance: FormInstanceDTO
            let task_mapping: MappingTaskFormInstanceDTO
        }
        
        let result: Response = try await executeRequest(request, decoder: decoder)
        
        AppLogger.log(.info, "API SUCCESS: Created form instance and linked to task", category: .api)
        AppLogger.log(.info, "Form Instance ID: \(result.form_instance.id)", category: .api)
        AppLogger.log(.info, "Task Mapping: task=\(result.task_mapping.task_id), form_instance=\(result.task_mapping.form_instance_id)", category: .api)
        
        return result.form_instance
    }
}

// MARK: - Location Hierarchy Endpoints

extension APIClient {
    
    // MARK: - Building Operations
    
    /// Create a new building
    func createBuilding(building: Building) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Location.buildingCreate)
        
        let payload: [String: Any] = [
            "id": building.id.uuidString,
            "name": building.name,
            "sld_id": building.sld?.id.uuidString ?? "",
            "is_deleted": building.is_deleted
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        return try await executeRequestRaw(request)
    }
    
    /// Update an existing building
    func updateBuilding(_ building: Building) async throws -> URLResponse {
        slog("updateBuilding", category: .api, data: [
            "building_id": building.id.uuidString,
            "name": building.name,
            "is_deleted": building.is_deleted
        ])
        
        let url = baseURL.appendingPathComponent(APIEndpoints.Location.buildingUpdate(buildingId: building.id.uuidString))
        
        var payload: [String: Any] = [
            "id": building.id.uuidString,
            "name": building.name,
            "is_deleted": building.is_deleted
        ]
        
        // Include access_notes if present
        if let accessNotes = building.access_notes {
            payload["access_notes"] = accessNotes
        }
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        // Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    // MARK: - Floor Operations
    
    /// Create a new floor
    func createFloor(floor: Floor) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Location.floorCreate)
        
        let payload: [String: Any] = [
            "id": floor.id.uuidString,
            "name": floor.name,
            "building_id": floor.building?.id.uuidString ?? "",
            "is_deleted": floor.is_deleted
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        AppLogger.log(.info, "createFloor()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        return try await executeRequestRaw(request)
    }
    
    /// Update an existing floor
    func updateFloor(_ floor: Floor) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Location.floorUpdate(floorId: floor.id.uuidString))
        
        var payload: [String: Any] = [
            "id": floor.id.uuidString,
            "name": floor.name,
            "building_id": floor.building?.id.uuidString ?? "",
            "is_deleted": floor.is_deleted
        ]
        
        // Include access_notes if present
        if let accessNotes = floor.access_notes {
            payload["access_notes"] = accessNotes
        }
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("updateFloor", category: .api, data: [
            "floor_id": floor.id.uuidString,
            "name": floor.name,
            "is_deleted": floor.is_deleted
        ])
        
        // Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    // MARK: - Room Operations
    
    /// Create a new room
    func createRoom(room: Room) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Location.roomCreate)
        
        let payload: [String: Any] = [
            "id": room.id.uuidString,
            "name": room.name,
            "floor_id": room.floor?.id.uuidString ?? "",
            "is_deleted": room.is_deleted
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        AppLogger.log(.info, "createRoom()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.debug, "Body JSON: \(String(data: bodyData, encoding: .utf8) ?? "<invalid JSON>")", category: .api)
        
        return try await executeRequestRaw(request)
    }
    
    /// Update an existing room
    func updateRoom(_ room: Room) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Location.roomUpdate(roomId: room.id.uuidString))
        
        var payload: [String: Any] = [
            "id": room.id.uuidString,
            "name": room.name,
            "floor_id": room.floor?.id.uuidString ?? "",
            "is_deleted": room.is_deleted
        ]
        
        // Include access_notes if present
        if let accessNotes = room.access_notes {
            payload["access_notes"] = accessNotes
        }
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("updateRoom", category: .api, data: [
            "room_id": room.id.uuidString,
            "name": room.name,
            "is_deleted": room.is_deleted
        ])
        
        // Fire request - mutation middleware returns 200 with payload echoed back
        return try await executeRequestRaw(request)
    }
    
    // MARK: - Get Locations for SLD
    
    /// Get all locations (buildings, floors, rooms) for an SLD
    func getLocationsForSLD(sldId: UUID) async throws -> (buildings: [BuildingDTO], floors: [FloorDTO], rooms: [RoomDTO]) {
        let url = baseURL.appendingPathComponent(APIEndpoints.Location.sldLocations(sldId: sldId.uuidString))
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        AppLogger.log(.info, "getLocationsForSLD()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        
        let decoder = JSONDecoder()
        
        struct LocationResponse: Decodable {
            let buildings: [BuildingDTO]
            let floors: [FloorDTO]
            let rooms: [RoomDTO]
        }
        
        let response: LocationResponse = try await executeRequest(request, decoder: decoder)
        
        AppLogger.log(.info, "Fetched \(response.buildings.count) buildings, \(response.floors.count) floors, \(response.rooms.count) rooms", category: .api)
        
        return (response.buildings, response.floors, response.rooms)
    }
    
    // MARK: - Report Generation
    
    func generateReport(irSessionId: UUID, userId: UUID, deviceId: String) async throws -> ReportGenerationResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Reporting.generate)
        
        let payload: [String: Any] = [
            "ir_session_id": irSessionId.uuidString,
            "type": "session",
            "user_id": userId.uuidString,
            "device_id": deviceId
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        AppLogger.log(.info, "generateReport()", category: .api)
        AppLogger.log(.info, "URL: \(url)", category: .api)
        AppLogger.log(.info, "Session: \(irSessionId.uuidString)", category: .api)
        AppLogger.log(.info, "User: \(userId.uuidString)", category: .api)
        AppLogger.log(.info, "Device: \(deviceId)", category: .api)
        
        let decoder = JSONDecoder()
        let response: ReportGenerationResponse = try await executeRequest(request, decoder: decoder)
        
        AppLogger.log(.info, "Report generation started", category: .api)
        if let executionArn = response.execution_arn {
            AppLogger.log(.info, "Execution ARN: \(executionArn)", category: .api)
        }
        
        return response
    }
    
    // MARK: - Attachment Methods
    
    /// Get presigned URL for uploading attachment
    func getAttachmentPresignedUploadURL(filename: String, fileSize: Int64) async throws -> AttachmentPresignedUploadResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Attachment.presignedUpload)
        
        let payload = AttachmentPresignedUploadRequest(filename: filename, file_size: fileSize)
        let bodyData = try JSONEncoder().encode(payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("getAttachmentPresignedUploadURL()", category: .api, data: [
            "filename": filename,
            "file_size": fileSize
        ])
        
        let decoder = JSONDecoder()
        return try await executeRequest(request, decoder: decoder)
    }
    
    /// Create attachment record
    func createAttachment(attachment: Attachment) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Attachment.create)
        
        let payload = AttachmentCreateRequest(attachment: attachment)
        let bodyData = try JSONEncoder().encode(payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createAttachment()", category: .api, data: [
            "attachment_id": attachment.id.uuidString,
            "filename": attachment.filename,
            "type": attachment.type
        ])
        
        return try await executeRequestRaw(request)
    }
    
    /// Create attachment-node mapping
    func createAttachmentNodeMapping(attachmentId: UUID, nodeId: UUID) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Attachment.mappingNode)
        
        let payload = AttachmentNodeMappingRequest(attachmentId: attachmentId, nodeId: nodeId)
        let bodyData = try JSONEncoder().encode(payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createAttachmentNodeMapping()", category: .api, data: [
            "attachment_id": attachmentId.uuidString,
            "node_id": nodeId.uuidString
        ])
        
        return try await executeRequestRaw(request)
    }
    
    /// Update attachment-node mapping (soft delete)
    func updateAttachmentNodeMapping(attachmentId: UUID, nodeId: UUID, isDeleted: Bool) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Attachment.mappingNodeUpdate(attachmentId: attachmentId.uuidString, nodeId: nodeId.uuidString))
        
        let payload = ["is_deleted": isDeleted]
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("updateAttachmentNodeMapping()", category: .api, data: [
            "attachment_id": attachmentId.uuidString,
            "node_id": nodeId.uuidString,
            "is_deleted": isDeleted
        ])
        
        return try await executeRequestRaw(request)
    }
    
    /// Get presigned URL for downloading attachment
    func getAttachmentPresignedDownloadURL(attachmentId: UUID) async throws -> AttachmentPresignedDownloadResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Attachment.presignedDownload(attachmentId: attachmentId.uuidString))
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        slog("getAttachmentPresignedDownloadURL()", category: .api, data: [
            "attachment_id": attachmentId.uuidString
        ])
        
        let decoder = JSONDecoder()
        return try await executeRequest(request, decoder: decoder)
    }
    
    /// Get list of attachments for a session
    func getSessionAttachments(sessionId: UUID) async throws -> [AttachmentDTO] {
        let url = baseURL.appendingPathComponent(APIEndpoints.Attachment.list(sessionId: sessionId.uuidString))
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        slog("getSessionAttachments()", category: .api, data: [
            "session_id": sessionId.uuidString
        ])
        
        let decoder = JSONDecoder()
        
        // Try to decode as array first
        do {
            return try await executeRequest(request, decoder: decoder)
        } catch {
            // Try to decode as object with attachments key
            let response: AttachmentListResponse = try await executeRequest(request, decoder: decoder)
            return response.attachments ?? []
        }
    }
    
    /// Delete attachment from server
    func deleteAttachment(attachmentId: UUID) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Attachment.delete(attachmentId: attachmentId.uuidString))

        let request = await createAuthorizedRequest(url: url, method: "DELETE")

        slog("deleteAttachment()", category: .api, data: [
            "attachment_id": attachmentId.uuidString
        ])

        return try await executeRequestRaw(request)
    }

    // MARK: - Attachment Visibility Operations

    /// Update a single attachment's visibility
    func updateAttachmentVisibility(attachmentId: UUID, visibility: String) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Attachment.update(attachmentId: attachmentId.uuidString))

        let payload = AttachmentUpdateVisibilityRequest(visibility: visibility)
        let bodyData = try JSONEncoder().encode(payload)

        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        slog("updateAttachmentVisibility()", category: .api, data: [
            "attachment_id": attachmentId.uuidString,
            "visibility": visibility
        ])

        return try await executeRequestRaw(request)
    }

    /// Bulk update visibility for multiple attachments
    func bulkUpdateAttachmentVisibility(attachmentIds: [UUID], visibility: String) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Attachment.bulkVisibility)

        let payload = AttachmentBulkVisibilityRequest(
            attachment_ids: attachmentIds.map { $0.uuidString.lowercased() },
            visibility: visibility
        )
        let bodyData = try JSONEncoder().encode(payload)

        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        slog("bulkUpdateAttachmentVisibility()", category: .api, data: [
            "attachment_count": "\(attachmentIds.count)",
            "visibility": visibility
        ])

        return try await executeRequestRaw(request)
    }

    // MARK: - SLD View Operations

    /// Create a new SLD view
    func createSLDView(sldView: SLDViewV2) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.SLDView.create)
        
        let payload: [String: Any] = [
            "id": sldView.id.uuidString,
            "sld_id": sldView.sld_id.uuidString,
            "name": sldView.name,
            "description": sldView.viewDescription ?? "",
            "view_type": sldView.view_type
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("createSLDView()", category: .api, data: [
            "view_id": sldView.id.uuidString,
            "sld_id": sldView.sld_id.uuidString,
            "name": sldView.name
        ])
        
        return try await executeRequestRaw(request)
    }
    
    /// Update an existing SLD view
    func updateSLDView(_ sldView: SLDViewV2) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.SLDView.update(viewId: sldView.id.uuidString))
        
        let payload: [String: Any] = [
            "name": sldView.name,
            "description": sldView.viewDescription ?? ""
        ]
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("updateSLDView()", category: .api, data: [
            "view_id": sldView.id.uuidString,
            "name": sldView.name
        ])
        
        return try await executeRequestRaw(request)
    }
    
    // MARK: - SLD View Mapping Methods
    
    /// Update node position in a view (uses existing backend route)
    func updateNodePositionInView(viewId: UUID, nodeId: UUID, x: Double, y: Double, width: Double? = nil, height: Double? = nil) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.SLDView.updateNodePosition(viewId: viewId.uuidString, nodeId: nodeId.uuidString))
        
        var payload: [String: Any] = [
            "x": x,
            "y": y
        ]
        if let w = width {
            payload["width"] = w
        }
        if let h = height {
            payload["height"] = h
        }
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("updateNodePositionInView()", category: .api, data: [
            "view_id": viewId.uuidString,
            "node_id": nodeId.uuidString,
            "x": x,
            "y": y,
            "width": width as Any,
            "height": height as Any
        ])
        
        return try await executeRequestRaw(request)
    }
    
    /// Add a node to a view (creates node-view mapping)
    func addNodeToView(viewId: UUID, nodeId: UUID, x: Double, y: Double, width: Double?, height: Double?) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.SLDView.addNodes(viewId: viewId.uuidString))
        
        var nodeData: [String: Any] = [
            "node_id": nodeId.uuidString,
            "x": x,
            "y": y
        ]
        if let width = width {
            nodeData["width"] = width
        }
        if let height = height {
            nodeData["height"] = height
        }
        
        let payload: [String: Any] = [
            "nodes": [nodeData]
        ]
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("addNodeToView()", category: .api, data: [
            "view_id": viewId.uuidString,
            "node_id": nodeId.uuidString,
            "payload": payload
        ])
        
        return try await executeRequestRaw(request)
    }
    
    /// Remove a node from a view
    func removeNodeFromView(viewId: UUID, nodeId: UUID) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.SLDView.removeNodes(viewId: viewId.uuidString))
        
        let payload: [String: Any] = [
            "node_ids": [nodeId.uuidString]
        ]
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("removeNodeFromView()", category: .api, data: [
            "view_id": viewId.uuidString,
            "node_id": nodeId.uuidString
        ])
        
        return try await executeRequestRaw(request)
    }
    
    /// Update edge points/routing in a view
    func updateEdgePointsInView(viewId: UUID, edgeId: UUID, points: [[String: Double]]?, algorithm: String?) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.SLDView.updateEdgePoints(viewId: viewId.uuidString, edgeId: edgeId.uuidString))
        
        var payload: [String: Any] = [:]
        if let points = points {
            payload["points"] = points
        }
        if let algorithm = algorithm {
            payload["algorithm"] = algorithm
        }
        
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("updateEdgePointsInView()", category: .api, data: [
            "view_id": viewId.uuidString,
            "edge_id": edgeId.uuidString
        ])
        
        return try await executeRequestRaw(request)
    }
    
    /// Update node collapse state in a view
    func updateNodeCollapseStateInView(viewId: UUID, nodeId: UUID, isCollapsed: Bool) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.SLDView.updateNodeCollapseState(viewId: viewId.uuidString, nodeId: nodeId.uuidString))
        
        let payload: [String: Any] = [
            "is_collapsed": isCollapsed
        ]
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        slog("updateNodeCollapseStateInView()", category: .api, data: [
            "view_id": viewId.uuidString,
            "node_id": nodeId.uuidString,
            "is_collapsed": isCollapsed
        ])
        
        return try await executeRequestRaw(request)
    }
    
    // MARK: - User Schedule
    
    /// Get the current user's work blocks across all SLDs they have access to
    /// - Parameters:
    /// - startDate: Optional start date filter (ISO format)
    /// - endDate: Optional end date filter (ISO format)
    /// - includePast: Whether to include past work blocks (default: false)
    /// - Returns: UserScheduleResponseDTO containing work blocks and session info
    func getUserSchedule(startDate: String? = nil, endDate: String? = nil, includePast: Bool = false) async throws -> UserScheduleResponseDTO {
        // Build URL with query parameters properly (not using appendingPathComponent which encodes ?)
        var components = URLComponents(url: baseURL.appendingPathComponent(APIEndpoints.Schedule.userSchedule), resolvingAgainstBaseURL: true)!
        
        var queryItems: [URLQueryItem] = []
        if let start = startDate {
            queryItems.append(URLQueryItem(name: "start_date", value: start))
        }
        if let end = endDate {
            queryItems.append(URLQueryItem(name: "end_date", value: end))
        }
        if includePast {
            queryItems.append(URLQueryItem(name: "include_past", value: "true"))
        }
        if !queryItems.isEmpty {
            components.queryItems = queryItems
        }
        
        let url = components.url!
        
        let request = await createAuthorizedRequest(url: url, method: "GET")
        
        slog("getUserSchedule()", category: .api, data: [
            "full_url": url.absoluteString,
            "start_date": startDate ?? "nil",
            "end_date": endDate ?? "nil",
            "include_past": includePast
        ])
        
        let decoder = JSONDecoder()
        return try await executeRequest(request, decoder: decoder)
    }
    
    // MARK: - Extraction Methods
    
    /// Extract nameplate data from photos for the given node IDs
    func extractNameplateData(nodeIds: [UUID], overwriteExisting: Bool) async throws -> ExtractNameplateResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Extraction.extractNameplateData)
        slog("Extracting nameplate data", category: .api, data: [
            "url": url.absoluteString,
            "node_ids_count": nodeIds.count,
            "overwrite_existing": overwriteExisting
        ])
        
        let payload: [String: Any] = [
            "node_ids": nodeIds.map { $0.uuidString },
            "overwrite_existing": overwriteExisting
        ]
        let bodyData = try JSONSerialization.data(withJSONObject: payload)
        
        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData
        
        return try await executeRequest(request)
    }
    
    /// Extract nameplate data from staged photos via temp S3 upload
    func extractTempNameplateData(
        photoURLs: [String],
        nodeClassId: UUID,
        coreAttributes: [[String: String]]?,
        overwriteExisting: Bool
    ) async throws -> ExtractTempNameplateResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.Extraction.extractTempNameplateData)
        slog("Extracting temp nameplate data", category: .api, data: [
            "url": url.absoluteString,
            "photo_count": photoURLs.count,
            "node_class_id": nodeClassId.uuidString,
            "overwrite_existing": overwriteExisting
        ])

        var payload: [String: Any] = [
            "photos": photoURLs,
            "node_class_id": nodeClassId.uuidString,
            "overwrite_existing": overwriteExisting
        ]

        if let attrs = coreAttributes {
            payload["core_attributes"] = attrs
        }

        let bodyData = try JSONSerialization.data(withJSONObject: payload)

        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        return try await executeRequest(request)
    }

    /// Ask the backend to extract embedded visual images from uploaded FLIR-IND
    /// radiometric JPEGs for the given IR photo IDs. Returns a summary of how many
    /// were processed; partial failures are non-fatal.
    func extractVisualFromIRPhotos(photoIds: [UUID]) async throws -> ExtractVisualBatchResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.IRPhoto.extractVisualBatch)
        slog("extractVisualFromIRPhotos()", category: .api, data: [
            "url": url.absoluteString,
            "photo_count": photoIds.count
        ])

        let payload: [String: Any] = [
            "photo_ids": photoIds.map { $0.uuidString }
        ]
        let bodyData = try JSONSerialization.data(withJSONObject: payload)

        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        return try await executeRequest(request)
    }

    /// Fetch enriched node details (core attributes, terminals, etc.)
    func fetchEnrichedNode(nodeId: UUID) async throws -> SLDDTONode {
        let url = baseURL.appendingPathComponent(APIEndpoints.Graph.enrichedNode(nodeId: nodeId.uuidString))
        slog("Fetching enriched node", category: .api, data: [
            "url": url.absoluteString,
            "node_id": nodeId.uuidString
        ])

        let request = await createAuthorizedRequest(url: url, method: "GET")
        return try await executeRequest(request)
    }

    // MARK: - Sync Job

    /// Store a sync job snapshot (pre/post dedup payloads) to the backend for audit/debugging.
    /// Uses executeRequestRaw for automatic 401 token refresh, same as createNode/createEdge.
    func createSyncJob(payload: [String: Any]) async throws -> URLResponse {
        let url = baseURL.appendingPathComponent(APIEndpoints.SyncJob.create)
        let bodyData = try JSONSerialization.data(withJSONObject: payload)

        var request = await createAuthorizedRequest(url: url, method: "POST")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = bodyData

        slog("createSyncJob()", category: .api, data: ["url": url.absoluteString])

        return try await executeRequestRaw(request)
    }
}

// MARK: - Report Generation Response

struct ReportGenerationResponse: Decodable {
    let success: Bool?
    let execution_arn: String?
    let message: String?
}

// MARK: - Extract Nameplate Response

struct ExtractNameplateResponse: Decodable {
    let success: Bool
    let processed: Int
    let updated: Int
    let skipped: Int
    let errors: [String]
    let skipped_no_photos: [String]
    let skipped_no_class: [String]
}

// MARK: - Extract Temp Nameplate Response

struct ExtractedAttribute: Decodable {
    let id: UUID
    let name: String
    let value: String?
}

struct ExtractTempNameplateResponse: Decodable {
    let success: Bool
    let extracted_attributes: [ExtractedAttribute]
}

// MARK: - Extract Visual Batch Response

struct ExtractVisualBatchResponse: Decodable {
    struct Summary: Decodable {
        let total: Int?
        let successful: Int?
        let failed: Int?
        let skipped: Int?
    }
    let summary: Summary?
}
