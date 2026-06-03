package com.egalvanic.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Backend API client for contract + UI↔API tests, grounded in the live OpenAPI/Swagger
 * spec at https://api.qa.egalvanic.ai/api/swagger.json (Swagger 2.0, basePath /api,
 * Bearer JWT auth).
 *
 * Security posture (applying the k6-review lessons):
 *   - Base URL comes from env BACKEND_API_URL (default = QA), and is ALLOWLISTED to
 *     *.egalvanic.ai so a mis-set target can't exfiltrate credentials.
 *   - Credentials come from env USER_EMAIL / USER_PASSWORD — NEVER hardcoded.
 *     authToken() returns null (and callers skip) when creds aren't provided.
 */
public final class ApiClient {

    public static final String BASE_URL =
            System.getProperty("BACKEND_API_URL",
                System.getenv().getOrDefault("BACKEND_API_URL", "https://api.qa.egalvanic.ai/api"));

    private static final String SUBDOMAIN =
            System.getProperty("TENANT_SUBDOMAIN",
                System.getenv().getOrDefault("TENANT_SUBDOMAIN", "acme"));

    private ApiClient() {}

    /** Fail fast if the target isn't a trusted eGalvanic host (no credential exfiltration). */
    public static void assertTrustedBaseUrl() {
        if (!BASE_URL.matches("^https://[a-z0-9.-]+\\.egalvanic\\.ai(/.*)?$")) {
            throw new IllegalStateException(
                "BACKEND_API_URL must be an https *.egalvanic.ai host. Refusing: " + BASE_URL);
        }
    }

    public static String email() {
        return System.getProperty("USER_EMAIL", System.getenv("USER_EMAIL"));
    }

    public static String password() {
        return System.getProperty("USER_PASSWORD", System.getenv("USER_PASSWORD"));
    }

    public static boolean hasCreds() {
        return email() != null && !email().isEmpty() && password() != null && !password().isEmpty();
    }

    /** POST /auth/v2/login → access_token, or null if creds absent / login fails. */
    public static String authToken() {
        if (!hasCreds()) return null;
        assertTrustedBaseUrl();
        Map<String, String> body = new HashMap<>();
        body.put("email", email());
        body.put("password", password());
        body.put("subdomain", SUBDOMAIN);
        Response r = given().baseUri(BASE_URL)
                .contentType("application/json").body(body)
                .post("/auth/v2/login");
        if (r.statusCode() < 200 || r.statusCode() >= 300) return null;
        return r.jsonPath().getString("access_token");
    }

    /** A request spec pre-authenticated with a Bearer token (or unauthenticated if null). */
    public static io.restassured.specification.RequestSpecification authed(String token) {
        var spec = given().baseUri(BASE_URL).contentType("application/json");
        return token == null ? spec : spec.header("Authorization", "Bearer " + token);
    }
}
