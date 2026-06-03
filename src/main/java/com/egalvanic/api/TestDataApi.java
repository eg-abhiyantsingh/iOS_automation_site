package com.egalvanic.api;

import com.egalvanic.constants.AppConstants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * TestDataApi — thin authenticated client for the QA backend, used to set up
 * DETERMINISTIC test state via the API instead of fighting unknown UI state
 * (the active-vs-no-active-job / session-ordering churn that breaks Site Visit
 * navigation). Contract reverse-engineered from the vendored app source
 * (app-source/Egalvanic PZ/Configuration/APIEndpoints.swift + AuthService.swift):
 *
 *   POST {base}/auth/v2/login
 *     headers: Content-Type: application/json, X-Subdomain: <code>, X-Language: en
 *     body:    {"email","password","subdomain"}
 *     resp:    {"access_token": "..."}
 *
 * All later calls send Authorization: Bearer <token>.
 *
 * Config (override via -D): api.baseUrl, api.subdomain.
 */
public class TestDataApi {

    private static final String BASE =
            System.getProperty("api.baseUrl", "https://api.qa.egalvanic.ai/api");
    /** Subdomain = company code before the first dot ("acme.egalvanic" -> "acme"). */
    private static final String SUBDOMAIN =
            System.getProperty("api.subdomain",
                    AppConstants.VALID_COMPANY_CODE.split("\\.")[0]);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private String token;

    /**
     * Authenticate with the QA test user; caches the bearer token.
     * Prefers runtime-injected secrets so CI/secret stores can override the
     * committed local-dev fallback:
     *   email    -> -Dapi.email    / env QA_API_EMAIL    / AppConstants.VALID_EMAIL
     *   password -> -Dapi.password / env QA_API_PASSWORD / AppConstants.VALID_PASSWORD
     */
    public void login() {
        login(cred("api.email", "QA_API_EMAIL", AppConstants.VALID_EMAIL),
              cred("api.password", "QA_API_PASSWORD", AppConstants.VALID_PASSWORD),
              SUBDOMAIN);
    }

    private static String cred(String sysProp, String envVar, String fallback) {
        String v = System.getProperty(sysProp);
        if (v == null || v.isEmpty()) v = System.getenv(envVar);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    public void login(String email, String password, String subdomain) {
        String body = String.format(
                "{\"email\":%s,\"password\":%s,\"subdomain\":%s}",
                jsonStr(email), jsonStr(password), jsonStr(subdomain));
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/auth/v2/login"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("X-Subdomain", subdomain == null ? "" : subdomain)
                .header("X-Language", "en")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = send(req);
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("Login failed: HTTP " + resp.statusCode()
                    + " — " + truncate(redact(resp.body()), 300));
        }
        this.token = extract(resp.body(), "access_token");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Login OK but no access_token in response: "
                    + truncate(redact(resp.body()), 300));
        }
        System.out.println("🔑 TestDataApi authenticated (token len=" + token.length() + ")");
    }

    public boolean isAuthenticated() { return token != null && !token.isEmpty(); }
    public String token() { return token; }

    /** Authenticated GET; returns the response (caller inspects status/body). */
    public HttpResponse<String> get(String path) {
        return send(authed(HttpRequest.newBuilder(URI.create(BASE + path))).GET().build());
    }

    /** Authenticated POST with a raw JSON body. */
    public HttpResponse<String> post(String path, String json) {
        return send(authed(HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json"))
                .POST(HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json)).build());
    }

    // ── internals ──────────────────────────────────────────────────────────
    private HttpRequest.Builder authed(HttpRequest.Builder b) {
        b.timeout(Duration.ofSeconds(30)).header("X-Language", "en");
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (SUBDOMAIN != null) b.header("X-Subdomain", SUBDOMAIN);
        return b;
    }

    private HttpResponse<String> send(HttpRequest req) {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("API call failed (" + req.method() + " "
                    + req.uri() + "): " + e.getMessage(), e);
        }
    }

    /** Minimal first-match string-field extractor (avoids adding a JSON dep). */
    public static String extract(String json, String field) {
        if (json == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    private static String truncate(String s, int n) {
        if (s == null) return "null";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    /** Mask token/secret field VALUES so they never reach logs or exceptions. */
    private static String redact(String s) {
        if (s == null) return "null";
        return s.replaceAll(
            "(?i)(\"(?:access_token|refresh_token|id_token|token|password|mfa|secret)\"\\s*:\\s*\")[^\"]*\"",
            "$1***\"");
    }
}
