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

    // ── identity / site (SLD) lookups ──────────────────────────────────────
    // Contract from APIEndpoints.swift: GET /auth/v2/me, GET /users/{id}/slds,
    // GET /sld/v3/{sldId} (full details incl. nodes + issues — the backend has
    // no dedicated issue/node search endpoints; the app syncs whole SLDs).

    private String userId;
    /** Per-SLD details cache — getAssetByName/getIssueByTitle both scan it. */
    private final java.util.Map<String, String> sldDetailsCache = new java.util.HashMap<>();

    /** Current user's id (GET /auth/v2/me); cached after first call. */
    public String currentUserId() {
        if (userId != null) return userId;
        HttpResponse<String> resp = get("/auth/v2/me");
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("GET /auth/v2/me failed: HTTP " + resp.statusCode());
        }
        userId = extract(resp.body(), "id");
        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("No user id in /auth/v2/me response: "
                    + truncate(redact(resp.body()), 300));
        }
        return userId;
    }

    /** Raw JSON list of the current user's SLDs (sites). */
    public String listSlds() {
        HttpResponse<String> resp = get("/users/" + currentUserId() + "/slds");
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("GET /users/{id}/slds failed: HTTP " + resp.statusCode());
        }
        return resp.body();
    }

    /** Id of the SLD whose name matches (exact field match), or null. */
    public String findSldIdByName(String sldName) {
        return extractSiblingField(listSlds(), "name", sldName, "id");
    }

    /** First SLD id in the user's list — deterministic fallback when no name given. */
    public String firstSldId() {
        java.util.List<String> ids = accessibleSldIds();
        if (!ids.isEmpty()) return ids.get(0);
        return extract(listSlds(), "id");  // legacy fallback
    }

    /** SLD ids the current user can access, from /auth/v2/me's "accessible_sld_ids".
     *  The legacy GET /users/{id}/slds returns [] for admin/RBAC accounts, so this is
     *  the reliable source (confirmed live 2026-06-17). */
    public java.util.List<String> accessibleSldIds() {
        HttpResponse<String> resp = get("/auth/v2/me");
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (resp.statusCode() / 100 != 2) return ids;
        java.util.regex.Matcher block = java.util.regex.Pattern
                .compile("\"accessible_sld_ids\"\\s*:\\s*\\[(.*?)\\]", java.util.regex.Pattern.DOTALL)
                .matcher(resp.body());
        if (block.find()) {
            java.util.regex.Matcher id = java.util.regex.Pattern
                    .compile("\"([0-9a-fA-F-]{36})\"").matcher(block.group(1));
            while (id.find()) ids.add(id.group(1));
        }
        return ids;
    }

    /** Full SLD details JSON (GET /sld/v3/{id}) — nodes + issues; cached per id. */
    public String getSldDetails(String sldId) {
        String cached = sldDetailsCache.get(sldId);
        if (cached != null) return cached;
        HttpResponse<String> resp = get("/sld/v3/" + sldId);
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("GET /sld/v3/" + sldId + " failed: HTTP " + resp.statusCode());
        }
        sldDetailsCache.put(sldId, resp.body());
        return resp.body();
    }

    // ── asset (node) lookups ───────────────────────────────────────────────

    /** Id of the node (asset) named {@code assetName} within the SLD, or null. */
    public String getAssetByName(String sldId, String assetName) {
        return extractSiblingField(getSldDetails(sldId), "name", assetName, "id");
    }

    /** Id of the first node in the SLD whose name contains {@code fragment}, or null. */
    public String findAssetIdByNameFragment(String sldId, String fragment) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"name\"\\s*:\\s*\"([^\"]*" + java.util.regex.Pattern.quote(fragment) + "[^\"]*)\"")
                .matcher(getSldDetails(sldId));
        return m.find() ? extractSiblingField(getSldDetails(sldId), "name", m.group(1), "id") : null;
    }

    // ── issue seeding / lookups ────────────────────────────────────────────

    /**
     * Create an issue via POST /issue/create. Body mirrors the app's own sync
     * export (SyncQueueExportService.buildIssueRequest): client-generated UUID,
     * title, sld_id, optional node_id/priority/status. Returns the issue id.
     */
    public String createIssue(String sldId, String nodeId, String title, String priority) {
        String issueId = java.util.UUID.randomUUID().toString().toUpperCase();
        StringBuilder body = new StringBuilder("{")
                .append("\"id\":").append(jsonStr(issueId))
                .append(",\"title\":").append(jsonStr(title))
                .append(",\"issue_description\":").append(jsonStr("Seeded by TestDataApi"))
                .append(",\"sld_id\":").append(jsonStr(sldId))
                .append(",\"is_deleted\":false")
                .append(",\"status\":").append(jsonStr("open"));
        if (nodeId != null && !nodeId.isEmpty()) body.append(",\"node_id\":").append(jsonStr(nodeId));
        if (priority != null && !priority.isEmpty()) body.append(",\"priority\":").append(jsonStr(priority));
        body.append("}");
        HttpResponse<String> resp = post("/issue/create", body.toString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("POST /issue/create failed: HTTP " + resp.statusCode()
                    + " — " + truncate(redact(resp.body()), 300));
        }
        String serverId = extract(resp.body(), "id");
        sldDetailsCache.remove(sldId); // details now stale — issue list changed
        System.out.println("🌱 Seeded issue '" + title + "' (id=" + (serverId != null ? serverId : issueId) + ")");
        return serverId != null ? serverId : issueId;
    }

    /** Id of the issue titled {@code title} within the SLD, or null. */
    public String getIssueByTitle(String sldId, String title) {
        return extractSiblingField(getSldDetails(sldId), "title", title, "id");
    }

    /** All issue titles in the SLD containing {@code fragment} (case-insensitive). */
    public java.util.List<String> searchIssues(String sldId, String fragment) {
        java.util.List<String> hits = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"title\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(getSldDetails(sldId));
        String needle = fragment == null ? "" : fragment.toLowerCase();
        while (m.find()) {
            if (m.group(1).toLowerCase().contains(needle)) hits.add(m.group(1));
        }
        return hits;
    }

    // ── work orders (IR sessions) + work-type services ─────────────────────
    // Live-verified 2026-07-21 (docs/worktype-gold-spec-2026-07-21.md):
    //   GET  /procedures-v2/services                      → the 13 work types
    //   POST /company/{companyId}/workorders/v2           → WO list (POST, 405 on GET)
    //   POST /ir_session/create                           → create WO (client uuid)
    //   POST /mapping/user-session/create                 → certifier/technician mapping

    private String companyId;

    /** Current user's company id (from /auth/v2/me); cached after first call. */
    public String companyId() {
        if (companyId != null) return companyId;
        HttpResponse<String> resp = get("/auth/v2/me");
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("GET /auth/v2/me failed: HTTP " + resp.statusCode());
        }
        companyId = extract(resp.body(), "company_id");
        if (companyId == null || companyId.isEmpty()) {
            throw new IllegalStateException("No company_id in /auth/v2/me response: "
                    + truncate(redact(resp.body()), 300));
        }
        return companyId;
    }

    /** Raw JSON of GET /procedures-v2/services — the work-type catalog. */
    public String workTypeServicesJson() {
        HttpResponse<String> resp = get("/procedures-v2/services");
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("GET /procedures-v2/services failed: HTTP "
                    + resp.statusCode() + " — " + truncate(redact(resp.body()), 300));
        }
        return resp.body();
    }

    /**
     * Raw JSON of the company work-order list, optionally filtered by a search
     * fragment (server-side name search — same call the web WO screen makes).
     */
    public String listWorkOrdersJson(String search) {
        String body = "{\"page\":1,\"page_size\":100,\"search\":"
                + jsonStr(search == null ? "" : search) + ",\"filters\":{}}";
        HttpResponse<String> resp = post("/company/" + companyId() + "/workorders/v2", body);
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("POST /company/{id}/workorders/v2 failed: HTTP "
                    + resp.statusCode() + " — " + truncate(redact(resp.body()), 300));
        }
        return resp.body();
    }

    /** Id of the work order named exactly {@code name}, or null. */
    public String findWorkOrderIdByName(String name) {
        return extractSiblingField(listWorkOrdersJson(name), "name", name, "id");
    }

    /** work_type_id of the WO named {@code name} — null if unset (General/legacy) or WO absent. */
    public String workOrderWorkTypeId(String name) {
        return extractSiblingField(listWorkOrdersJson(name), "name", name, "work_type_id");
    }

    /**
     * Create a work order (IR session) with an explicit work type. Payload
     * mirrors the web create dialog byte-for-byte (captured live); the id is
     * client-generated. {@code workTypeId} null ⇒ "General". Returns the WO id.
     */
    public String createWorkOrder(String name, String workTypeId, String sldId,
                                  String photoType, String priority, Integer estHours) {
        String id = java.util.UUID.randomUUID().toString();
        // Millisecond-precision timestamps and ASCII-only description are
        // load-bearing: microsecond instants / start_date:null drew HTTP 500
        // from /ir_session/create (observed live 2026-07-21).
        String nowMs = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.now());
        String body = "{"
                + "\"id\":" + jsonStr(id)
                + ",\"name\":" + jsonStr(name)
                + ",\"description\":" + jsonStr("QA automation work-type fixture - do not delete")
                + ",\"photo_type\":" + jsonStr(photoType == null ? "FLUKE" : photoType)
                + ",\"sld_id\":" + jsonStr(sldId)
                + ",\"priority\":" + jsonStr(priority == null ? "Medium" : priority)
                + ",\"start_date\":" + jsonStr(nowMs) + ",\"due_date\":null"
                + ",\"date_created\":" + jsonStr(nowMs)
                + ",\"active_visual_prefix\":\"visual_\",\"active_ir_prefix\":\"ir_\""
                + ",\"active\":true,\"job_id\":null"
                + ",\"est_hours\":" + (estHours == null ? "8" : estHours)
                + ",\"work_type_id\":" + (workTypeId == null ? "null" : jsonStr(workTypeId))
                + ",\"asset_scope\":null}";
        HttpResponse<String> resp = post("/ir_session/create", body);
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("POST /ir_session/create failed: HTTP "
                    + resp.statusCode() + " — " + truncate(redact(resp.body()), 300));
        }
        // Attach the current user as field_technician + certifier. The mapping
        // payload MUST carry a client-generated id (id-less POSTs "succeed" but
        // don't persist — observed live 2026-07-21), and the field_technician
        // mapping is what makes the WO visible in the iOS Work Orders list.
        for (String mappingType : new String[]{"field_technician", "certifier"}) {
            HttpResponse<String> m = post("/mapping/user-session/create",
                    "{\"id\":" + jsonStr(java.util.UUID.randomUUID().toString())
                    + ",\"user_id\":" + jsonStr(currentUserId())
                    + ",\"session_id\":" + jsonStr(id)
                    + ",\"mapping_type\":" + jsonStr(mappingType) + "}");
            if (m.statusCode() / 100 != 2) {
                System.out.println("⚠️ user-session mapping (" + mappingType + ") failed: HTTP "
                        + m.statusCode() + " — " + truncate(redact(m.body()), 200));
            }
        }
        System.out.println("🌱 Created WO '" + name + "' (work_type_id="
                + (workTypeId == null ? "null" : workTypeId) + ", id=" + id + ")");
        return id;
    }

    /**
     * Find-or-create the durable QA-WT fixture for {@code fixtureName}. Returns
     * the WO id — self-heals the fixture family if someone deletes one.
     */
    public String ensureWorkOrderFixture(String fixtureName, String workTypeId, String sldId) {
        String existing = findWorkOrderIdByName(fixtureName);
        if (existing != null) return existing;
        return createWorkOrder(fixtureName, workTypeId, sldId, "FLUKE", "Medium", 8);
    }

    /**
     * Resolve an SLD (site) id by display name. Tries the user SLD list first,
     * then falls back to scanning the company WO list's sld_name fields —
     * GET /users/{id}/slds returns [] for admin/RBAC accounts (see
     * accessibleSldIds), so the fallback is load-bearing for the QA admin user.
     */
    public String resolveSldIdByName(String siteName) {
        try {
            String direct = findSldIdByName(siteName);
            if (direct != null) return direct;
        } catch (Exception e) { /* fall through to WO-list scan */ }
        try {
            return extractSiblingField(listWorkOrdersJson(""), "sld_name", siteName, "sld_id");
        } catch (Exception e) {
            System.out.println("⚠️ resolveSldIdByName('" + siteName + "'): " + e.getMessage());
            return null;
        }
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

    /**
     * Find the JSON object containing "{matchField}":"{matchValue}" and return
     * {wantField} from that SAME object (brace-balanced scan; same no-JSON-dep
     * tradeoff as {@link #extract} — values containing unescaped braces would
     * confuse it, which our seeded names/titles never do).
     */
    public static String extractSiblingField(String json, String matchField,
                                             String matchValue, String wantField) {
        if (json == null || matchValue == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(matchField) + "\"\\s*:\\s*\""
                        + java.util.regex.Pattern.quote(matchValue) + "\"")
                .matcher(json);
        if (!m.find()) return null;
        // Walk back to the '{' that opens the enclosing object…
        int depth = 0, start = -1;
        for (int i = m.start() - 1; i >= 0; i--) {
            char c = json.charAt(i);
            if (c == '}') depth++;
            else if (c == '{') {
                if (depth == 0) { start = i; break; }
                depth--;
            }
        }
        if (start < 0) return null;
        // …then forward to its matching '}' and extract within that slice.
        depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) {
                return extract(json.substring(start, i + 1), wantField);
            }
        }
        return null;
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
