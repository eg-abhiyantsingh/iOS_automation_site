package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZP-3914 (iOS 1.59) acceptance items "Bulk export" and "Instance tags" — ZP-3840 "NETA-3 Forms tab: multi-edit
 * working sets, bulk export, and instance tags".
 *
 * WHY API-LEVEL: ZP-3840 shipped the multi-edit half on iOS (the binary carries /eg-form-instance/bulk-patch) but the
 * export dialog and the tag UI only on web — the v1.73 iOS build has no export/tags route, string or control
 * (checked 2026-10-30: strings of Z Platform-QA.debug.dylib + Localizable.strings). The backend contract both
 * clients share is what is tested here, through the exact calls the web Forms tab makes (SessionDetail chunk of
 * acme.qa.egalvanic.ai, 2026-10-30):
 *   GET  /eg-form-instance/by-session/{session}               → {success, data:[{id, tags, template_format, …}]}
 *   PUT  /eg-form-instance/by-session/{session}/bulk-tags      {instance_ids, add_tags, remove_tags} → {success, updated, tags_by_instance}
 *   GET|PUT /eg-form-tags                                      {name, color} — colour = palette key, never hex
 *   POST /eg-form-instance/by-session/{session}/export         {instance_ids, output_format: pdf|pdf_zip|docx|docx_zip} → {success, execution_arn}
 *   GET  /reporting/status/{base64(execution_arn)}             → {status, report_url|pdf_url}
 *
 * Driver-free: no simulator. Fixture = the QA-WT10 'NETA Testing' work order the iOS Forms-tab tests use. Every tag
 * this class adds is removed again; tags are namespaced 'qa-zp3914' so a leftover is recognisable.
 */
public final class ZP3914_FormsBulkExportTags_ApiTest {

    private static final String FIXTURE_PREFIX = System.getProperty("ZP3914_FORMS_WO", "QA-WT10");
    private static final String TAG = "qa-zp3914";
    private static final long EXPORT_TIMEOUT_MS = 8 * 60_000L;

    private TestDataApi api;
    private String sessionId;
    /** Every candidate fixture session with its instances (TC_BX_04 looks across them for a templateless form). */
    private final Map<String, List<JsonObject>> bySession = new LinkedHashMap<>();
    /** Instances of the fixture session, as listed by the backend. */
    private List<JsonObject> instances = List.of();
    /** Tags this class added, per instance — removed in teardown whatever happened. */
    private final Map<String, Set<String>> addedTags = new LinkedHashMap<>();

    // ── setup ────────────────────────────────────────────────────────────────────────────────────────────

    @BeforeClass(alwaysRun = true)
    public void resolveFixtureSession() {
        api = new TestDataApi();
        api.login();
        JsonObject wos = JsonParser.parseString(api.listWorkOrdersJson(FIXTURE_PREFIX)).getAsJsonObject();
        List<String> candidates = new ArrayList<>();
        for (JsonElement e : arrayIn(wos)) {
            JsonObject o = e.getAsJsonObject();
            if (str(o, "name").startsWith(FIXTURE_PREFIX)) candidates.add(str(o, "id"));
        }
        // The fixture exists per site (first-site drift); use the copy that actually carries form instances.
        for (String id : candidates) {
            List<JsonObject> rows = listInstances(id);
            bySession.put(id, rows);
            if (rows.size() > instances.size()) { sessionId = id; instances = rows; }
        }
        System.out.println("🧾 ZP-3914 forms API: session " + sessionId + " (of " + candidates.size() + " '" + FIXTURE_PREFIX
                + "' work orders) carries " + instances.size() + " form instance(s)");
        if (!instances.isEmpty()) System.out.println("   first instance keys: " + instances.get(0).keySet());
    }

    private void needInstances(int n, String tc) {
        if (sessionId == null || instances.size() < n) {
            throw new SkipException(tc + ": the '" + FIXTURE_PREFIX + "' fixture session has " + instances.size()
                    + " form instance(s); " + n + " needed (run the iOS Forms-tab tests once to provision them)");
        }
    }

    // ── instance tags ────────────────────────────────────────────────────────────────────────────────────

    @Test(priority = 1)
    public void TC_TAG_01_listingCarriesTagsAndTemplateFormat() {
        needInstances(1, "TC_TAG_01");
        for (JsonObject i : instances) {
            Assert.assertTrue(i.has("id"), "Every listed instance has an id: " + i.keySet());
            Assert.assertTrue(i.has("tags"), "Every listed instance carries 'tags' (egtags_a1) — keys " + i.keySet());
            JsonElement t = i.get("tags");
            Assert.assertTrue(t.isJsonNull() || t.isJsonArray(), "'tags' is a list — got " + t);
            Assert.assertTrue(i.has("template_format"), "The listing exposes 'template_format' for the export dialog — keys " + i.keySet());
        }
    }

    @Test(priority = 2)
    public void TC_TAG_02_bulkAddThenRemoveOnTwoInstances() {
        needInstances(2, "TC_TAG_02");
        List<String> ids = List.of(id(instances.get(0)), id(instances.get(1)));
        assertNoTestTag(ids);
        JsonObject add = bulkTags(ids, List.of(TAG), List.of());
        track(ids, TAG);
        Assert.assertTrue(add.get("success").getAsBoolean(), "bulk-tags add must succeed — " + add);
        Assert.assertEquals(add.get("updated").getAsInt(), 2, "Both instances are updated in one call — " + add);
        JsonObject byInstance = add.getAsJsonObject("tags_by_instance");
        for (String id : ids) {
            Assert.assertTrue(byInstance != null && byInstance.has(id) && containsIgnoreCase(byInstance.getAsJsonArray(id), TAG),
                    "tags_by_instance echoes '" + TAG + "' for " + id + " — " + byInstance);
            Assert.assertTrue(tagsOf(id).contains(TAG), "The listing shows '" + TAG + "' on " + id + " after the add");
        }
        JsonObject remove = bulkTags(ids, List.of(), List.of(TAG));
        Assert.assertTrue(remove.get("success").getAsBoolean(), "bulk-tags remove must succeed — " + remove);
        for (String id : ids) Assert.assertFalse(tagsOf(id).contains(TAG), "'" + TAG + "' is gone from " + id + " after the remove");
        untrack(ids, TAG);
    }

    @Test(priority = 3)
    public void TC_TAG_03_tagLongerThan50IsRejectedAtomically() {
        needInstances(1, "TC_TAG_03");
        String id = id(instances.get(0));
        String fifty = (TAG + "-" + "x".repeat(50)).substring(0, 50);
        String fiftyOne = fifty + "y";
        HttpResponse<String> r = rawBulkTags(List.of(id), List.of(TAG + "-ok", fiftyOne), List.of());
        track(List.of(id), TAG + "-ok");
        Assert.assertTrue(rejected(r), "A 51-character tag must be rejected (cap 50) — HTTP " + r.statusCode() + " " + r.body());
        Assert.assertFalse(tagsOf(id).contains(TAG + "-ok"),
                "The request is atomic: the valid tag sent alongside the over-long one must NOT be applied");
        untrack(List.of(id), TAG + "-ok");
        JsonObject ok = bulkTags(List.of(id), List.of(fifty), List.of());
        track(List.of(id), fifty);
        Assert.assertTrue(ok.get("success").getAsBoolean(), "Exactly 50 characters is allowed — " + ok);
        Assert.assertTrue(tagsOf(id).contains(fifty.toLowerCase()) || tagsOf(id).contains(fifty), "The 50-character tag is stored");
    }

    @Test(priority = 4)
    public void TC_TAG_04_moreThan20TagsPerRequestIsRejected() {
        needInstances(1, "TC_TAG_04");
        String id = id(instances.get(0));
        List<String> many = new ArrayList<>();
        for (int i = 1; i <= 21; i++) many.add(String.format("%s-%02d", TAG, i));
        HttpResponse<String> r = rawBulkTags(List.of(id), many, List.of());
        many.forEach(t -> track(List.of(id), t));
        Assert.assertTrue(rejected(r), "21 tags in one request must be rejected (cap 20) — HTTP " + r.statusCode() + " " + r.body());
        Set<String> now = tagsOf(id);
        Assert.assertTrue(many.stream().noneMatch(now::contains), "Nothing from the rejected request is applied — " + now);
        many.forEach(t -> untrack(List.of(id), t));
    }

    @Test(priority = 5)
    public void TC_TAG_05_registryColourIsAPaletteKeyNeverHex() {
        JsonObject list = json(api.get("/eg-form-tags"));
        Assert.assertTrue(list.get("success").getAsBoolean(), "GET /eg-form-tags must succeed — " + list);
        String before = colourOf(list, TAG);
        JsonObject set = json(api.put("/eg-form-tags", "{\"name\":\"" + TAG + "\",\"color\":\"teal\"}"));
        try {
            Assert.assertTrue(set.get("success").getAsBoolean(), "Setting palette colour 'teal' must succeed — " + set);
            Assert.assertEquals(colourOf(json(api.get("/eg-form-tags")), TAG), "teal", "The registry returns the stored palette key");
            HttpResponse<String> hex = api.put("/eg-form-tags", "{\"name\":\"" + TAG + "\",\"color\":\"#ff0000\"}");
            Assert.assertTrue(rejected(hex), "A hex colour must be rejected — colours are palette keys only — HTTP " + hex.statusCode() + " " + hex.body());
            Assert.assertEquals(colourOf(json(api.get("/eg-form-tags")), TAG), "teal", "A rejected colour leaves the stored one untouched");
        } finally {
            api.put("/eg-form-tags", "{\"name\":\"" + TAG + "\",\"color\":" + (before == null ? "null" : "\"" + before + "\"") + "}");
        }
    }

    // ── bulk export ──────────────────────────────────────────────────────────────────────────────────────

    @Test(priority = 10)
    public void TC_BX_01_pdfExportOfTwoFormsIsOneJoinedPdf() {
        List<String> ids = templated(2, "TC_BX_01");
        byte[] file = runExport(ids, "pdf");
        Assert.assertTrue(startsWith(file, "%PDF"), "'pdf' must deliver a PDF document (starts %PDF) — got " + head(file));
        int pages = countOccurrences(file, "/Type /Page") + countOccurrences(file, "/Type/Page") - countOccurrences(file, "/Type /Pages") - countOccurrences(file, "/Type/Pages");
        System.out.println("📄 joined PDF: " + file.length + " bytes, ~" + pages + " page object(s)");
        // Page objects can sit inside compressed object streams (PDF 1.5+) — only judge the count when it is visible.
        if (pages > 0) Assert.assertTrue(pages >= 2, "Two forms joined must give at least two pages — got ~" + pages);
    }

    @Test(priority = 11)
    public void TC_BX_02_pdfArchiveHasOneUniquelyNamedPdfPerForm() throws Exception {
        List<String> ids = templated(2, "TC_BX_02");
        byte[] file = runExport(ids, "pdf_zip");
        Assert.assertTrue(startsWith(file, "PK"), "'pdf_zip' must deliver a zip — got " + head(file));
        List<String> names = zipNames(file);
        System.out.println("🗜️ archive members: " + names);
        Assert.assertEquals(names.size(), ids.size(), "One archive member per exported form — " + names);
        Assert.assertEquals(new HashSet<>(names).size(), names.size(), "Member names are unique — " + names);
        for (String n : names) {
            Assert.assertTrue(n.toLowerCase().endsWith(".pdf"), "Each member is a PDF — " + n);
            Assert.assertFalse(n.matches(".*[<>:\"|?*\\\\].*"), "Names are Windows-safe — " + n);
        }
    }

    @Test(priority = 12)
    public void TC_BX_03_docxOnlyWhenEveryTemplateIsDocx() throws Exception {
        needInstances(1, "TC_BX_03");
        JsonObject nonDocx = instances.stream().filter(i -> !nullOrEmpty(str(i, "template_format"))
                && !"docx".equalsIgnoreCase(str(i, "template_format"))).findFirst().orElse(null);
        List<JsonObject> docx = instances.stream().filter(i -> "docx".equalsIgnoreCase(str(i, "template_format"))).toList();
        if (nonDocx == null && docx.isEmpty()) throw new SkipException("TC_BX_03: no templated form in the fixture session");
        if (nonDocx != null) {
            HttpResponse<String> r = api.post(exportPath(), "{\"instance_ids\":[\"" + id(nonDocx) + "\"],\"output_format\":\"docx\"}");
            Assert.assertTrue(rejected(r), "DOCX of a '" + str(nonDocx, "template_format") + "' template must be rejected — HTTP "
                    + r.statusCode() + " " + r.body());
        }
        if (!docx.isEmpty()) {
            byte[] file = runExport(List.of(id(docx.get(0))), "docx");
            Assert.assertTrue(startsWith(file, "PK") && zipNames(file).contains("word/document.xml"),
                    "'docx' of a .docx template must deliver a Word document");
        }
    }

    @Test(priority = 13)
    public void TC_BX_04_templatelessFormIsSkippedNotFatal() {
        // Any fixture session holding both a templateless and a templated form will do.
        String session = null;
        JsonObject bare = null, withTemplate = null;
        for (Map.Entry<String, List<JsonObject>> e : bySession.entrySet()) {
            JsonObject b = e.getValue().stream().filter(i -> nullOrEmpty(str(i, "template_format"))).findFirst().orElse(null);
            JsonObject t = e.getValue().stream().filter(i -> !nullOrEmpty(str(i, "template_format"))).findFirst().orElse(null);
            if (b != null && t != null) { session = e.getKey(); bare = b; withTemplate = t; break; }
        }
        if (session == null) {
            throw new SkipException("TC_BX_04: no '" + FIXTURE_PREFIX + "' session holds a templateless form next to a templated one ("
                    + bySession.size() + " session(s) checked) — nothing to drop as skipped_no_template");
        }
        List<String> ids = List.of(id(withTemplate), id(bare));
        StringBuilder trail = new StringBuilder();
        byte[] file = runExport(session, ids, "pdf", trail);
        Assert.assertTrue(startsWith(file, "%PDF"), "The run still delivers the templated form — got " + head(file));
        Assert.assertTrue(trail.toString().contains(id(bare)) || trail.toString().contains("skipped_no_template"),
                "The templateless form is reported as skipped (skipped_no_template) — responses: " + trail);
    }

    @Test(priority = 14)
    public void TC_BX_05_exportIsGatedToTheSessionsOwnForms() {
        needInstances(1, "TC_BX_05");
        String noSuchSession = UUID.randomUUID().toString();
        HttpResponse<String> foreignSession = api.post("/eg-form-instance/by-session/" + noSuchSession + "/export",
                "{\"instance_ids\":[\"" + id(instances.get(0)) + "\"],\"output_format\":\"pdf\"}");
        System.out.println("🔒 export via unknown session " + noSuchSession + " → HTTP " + foreignSession.statusCode() + " " + clip(foreignSession.body()));
        HttpResponse<String> strayInstance = api.post(exportPath(),
                "{\"instance_ids\":[\"" + UUID.randomUUID() + "\"],\"output_format\":\"pdf\"}");
        System.out.println("🔒 export of an instance outside the session → HTTP " + strayInstance.statusCode() + " " + clip(strayInstance.body()));
        HttpResponse<String> strayTag = rawBulkTags(List.of(UUID.randomUUID().toString()), List.of(TAG), List.of());
        System.out.println("🔒 bulk-tags on an instance outside the session → HTTP " + strayTag.statusCode() + " " + clip(strayTag.body()));
        // Live 2026-10-30: an unknown session comes back as HTTP 200 + an HTML page (not the API's JSON), i.e. the
        // request never reached the export handler. What matters for tenant gating is that NO export job starts.
        foreignSession.previousResponse().ifPresent(p -> System.out.println("   ↪ redirected from HTTP " + p.statusCode()
                + " " + p.headers().firstValue("location").orElse("") + " → " + foreignSession.uri()));
        Assert.assertFalse(exportStarted(foreignSession),
                "Export against a session id that does not exist must not start a job (ZP-3840: the session's SLD must belong"
                        + " to the caller's company — fail closed) — HTTP " + foreignSession.statusCode() + " " + clip(foreignSession.body()));
        Assert.assertTrue(rejected(strayInstance) && strayInstance.statusCode() < 500,
                "An instance id that does not belong to the session must be refused with a 4xx — HTTP " + strayInstance.statusCode() + " " + clip(strayInstance.body()));
        Assert.assertTrue(rejected(strayTag) && strayTag.statusCode() < 500,
                "bulk-tags with an instance outside the session must be refused with a 4xx — HTTP " + strayTag.statusCode() + " " + clip(strayTag.body()));
    }

    // ── teardown ─────────────────────────────────────────────────────────────────────────────────────────

    @AfterClass(alwaysRun = true)
    public void removeEveryTagThisClassAdded() {
        if (api == null || sessionId == null) return;
        for (Map.Entry<String, Set<String>> e : addedTags.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            List<String> tags = new ArrayList<>(e.getValue());
            for (int i = 0; i < tags.size(); i += 20) {   // stay inside the 20-per-request cap
                HttpResponse<String> r = rawBulkTags(List.of(e.getKey()), List.of(), tags.subList(i, Math.min(tags.size(), i + 20)));
                System.out.println("🧹 removed test tags from " + e.getKey() + " → HTTP " + r.statusCode());
            }
        }
        addedTags.clear();
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────────────────

    private String exportPath() { return exportPath(sessionId); }

    private static String exportPath(String session) { return "/eg-form-instance/by-session/" + session + "/export"; }

    private List<JsonObject> listInstances(String session) {
        HttpResponse<String> r = api.get("/eg-form-instance/by-session/" + session);
        if (r.statusCode() / 100 != 2) return List.of();
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : arrayIn(JsonParser.parseString(r.body()).getAsJsonObject())) out.add(e.getAsJsonObject());
        return out;
    }

    private Set<String> tagsOf(String instanceId) {
        for (JsonObject i : listInstances(sessionId)) {
            if (instanceId.equals(id(i))) {
                Set<String> s = new HashSet<>();
                if (i.has("tags") && i.get("tags").isJsonArray()) i.getAsJsonArray("tags").forEach(t -> s.add(t.getAsString()));
                return s;
            }
        }
        return Set.of();
    }

    private void assertNoTestTag(List<String> ids) {
        for (String id : ids) {
            if (tagsOf(id).contains(TAG)) {
                rawBulkTags(List.of(id), List.of(), List.of(TAG));   // leftover from an aborted run
                System.out.println("🧹 removed a leftover '" + TAG + "' from " + id);
            }
        }
    }

    private HttpResponse<String> rawBulkTags(List<String> ids, List<String> add, List<String> remove) {
        String body = "{\"instance_ids\":" + jsonList(ids) + ",\"add_tags\":" + jsonList(add) + ",\"remove_tags\":" + jsonList(remove) + "}";
        return api.put("/eg-form-instance/by-session/" + sessionId + "/bulk-tags", body);
    }

    private JsonObject bulkTags(List<String> ids, List<String> add, List<String> remove) {
        return json(rawBulkTags(ids, add, remove));
    }

    private void track(List<String> ids, String tag) {
        ids.forEach(i -> addedTags.computeIfAbsent(i, k -> new java.util.LinkedHashSet<>()).add(tag));
    }

    private void untrack(List<String> ids, String tag) {
        ids.forEach(i -> { Set<String> s = addedTags.get(i); if (s != null) s.remove(tag); });
    }

    /** {@code n} instance ids that have a template (export needs one), submitted ones first. */
    private List<String> templated(int n, String tc) {
        needInstances(n, tc);
        List<JsonObject> withTemplate = new ArrayList<>(instances.stream().filter(i -> !nullOrEmpty(str(i, "template_format"))).toList());
        withTemplate.sort((a, b) -> Boolean.compare(!"submitted".equalsIgnoreCase(str(b, "status")), !"submitted".equalsIgnoreCase(str(a, "status"))));
        if (withTemplate.size() < n) {
            throw new SkipException(tc + ": " + withTemplate.size() + " templated form(s) in the fixture session; " + n + " needed");
        }
        return withTemplate.subList(0, n).stream().map(this::id).toList();
    }

    private byte[] runExport(List<String> ids, String format) {
        return runExport(sessionId, ids, format, new StringBuilder());
    }

    /** POST export → poll /reporting/status until SUCCEEDED → download the file. {@code trail} collects the responses. */
    private byte[] runExport(String session, List<String> ids, String format, StringBuilder trail) {
        HttpResponse<String> start = api.post(exportPath(session), "{\"instance_ids\":" + jsonList(ids) + ",\"output_format\":\"" + format + "\"}");
        trail.append(start.body());
        JsonObject s = json(start);
        Assert.assertTrue(start.statusCode() / 100 == 2 && s.has("success") && s.get("success").getAsBoolean() && s.has("execution_arn"),
                "Export '" + format + "' must start (success + execution_arn) — HTTP " + start.statusCode() + " " + start.body());
        String token = Base64.getEncoder().encodeToString(s.get("execution_arn").getAsString().getBytes(StandardCharsets.UTF_8));
        long deadline = System.currentTimeMillis() + EXPORT_TIMEOUT_MS;
        long wait = 5_000;
        JsonObject st = null;
        while (System.currentTimeMillis() < deadline) {
            sleep(wait);
            HttpResponse<String> r = api.get("/reporting/status/" + token);
            if (r.statusCode() / 100 != 2) {
                Assert.assertTrue(r.statusCode() >= 500, "Status check must not 4xx — HTTP " + r.statusCode() + " " + r.body());
                wait = Math.min(wait * 2, 30_000);
                continue;
            }
            st = json(r);
            String status = str(st, "status");
            if ("SUCCEEDED".equals(status)) break;
            Assert.assertFalse(List.of("FAILED", "TIMED_OUT", "ABORTED").contains(status), "Export '" + format + "' ended " + status + " — " + r.body());
            wait = 5_000;
        }
        Assert.assertNotNull(st, "No status was ever readable for the '" + format + "' export");
        trail.append(st);
        Assert.assertEquals(str(st, "status"), "SUCCEEDED", "Export '" + format + "' must finish within " + EXPORT_TIMEOUT_MS / 60_000 + " min");
        String url = !nullOrEmpty(str(st, "report_url")) ? str(st, "report_url") : str(st, "pdf_url");
        Assert.assertFalse(nullOrEmpty(url), "A finished export carries report_url / pdf_url — " + st);
        HttpResponse<byte[]> file = api.downloadBytes(url);
        Assert.assertTrue(file != null && file.statusCode() == 200 && file.body().length > 0, "The export file downloads (HTTP "
                + (file == null ? "—" : file.statusCode()) + ")");
        System.out.println("📦 export '" + format + "' of " + ids.size() + " form(s): " + file.body().length + " bytes");
        return file.body();
    }

    private static List<String> zipNames(byte[] zip) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) if (!e.isDirectory()) names.add(e.getName());
        }
        return names;
    }

    private static JsonArray arrayIn(JsonObject o) {
        if (o.has("data") && o.get("data").isJsonArray()) return o.getAsJsonArray("data");
        if (o.has("data") && o.get("data").isJsonObject()) {
            for (String k : List.of("items", "work_orders", "results", "rows")) {
                if (o.getAsJsonObject("data").has(k)) return o.getAsJsonObject("data").getAsJsonArray(k);
            }
        }
        for (String k : List.of("items", "work_orders", "results")) if (o.has(k) && o.get(k).isJsonArray()) return o.getAsJsonArray(k);
        return new JsonArray();
    }

    private static String colourOf(JsonObject registry, String tag) {
        for (JsonElement e : arrayIn(registry)) {
            JsonObject o = e.getAsJsonObject();
            if (tag.equalsIgnoreCase(str(o, "name"))) return nullOrEmpty(str(o, "color")) ? null : str(o, "color");
        }
        return null;
    }

    /** An export job was started: 2xx JSON with success=true and an execution_arn. */
    private static boolean exportStarted(HttpResponse<String> r) {
        if (r.statusCode() / 100 != 2) return false;
        try {
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            return o.has("success") && o.get("success").getAsBoolean() && o.has("execution_arn");
        } catch (RuntimeException e) {
            return false;   // not the API's JSON (e.g. an HTML page) — nothing was started
        }
    }

    /** Refused: a non-2xx status, or a 2xx whose body says success=false. */
    private static boolean rejected(HttpResponse<String> r) {
        if (r.statusCode() / 100 != 2) return true;
        try {
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            return o.has("success") && !o.get("success").getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static JsonObject json(HttpResponse<String> r) {
        try {
            return JsonParser.parseString(r.body()).getAsJsonObject();
        } catch (RuntimeException e) {
            Assert.fail("HTTP " + r.statusCode() + " body is not JSON: " + r.body());
            return null;
        }
    }

    private String id(JsonObject o) { return str(o, "id"); }

    private static String str(JsonObject o, String k) {
        return o != null && o.has(k) && !o.get(k).isJsonNull() && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }

    private static boolean nullOrEmpty(String s) { return s == null || s.isEmpty(); }

    private static String clip(String s) { return s == null ? "" : s.length() <= 240 ? s : s.substring(0, 240) + "…"; }

    private static boolean containsIgnoreCase(JsonArray a, String v) {
        for (JsonElement e : a) if (v.equalsIgnoreCase(e.getAsString())) return true;
        return false;
    }

    private static String jsonList(List<String> v) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < v.size(); i++) b.append(i == 0 ? "" : ",").append('"').append(v.get(i).replace("\"", "\\\"")).append('"');
        return b.append(']').toString();
    }

    private static boolean startsWith(byte[] b, String magic) {
        return b != null && b.length >= magic.length() && new String(b, 0, magic.length(), StandardCharsets.ISO_8859_1).equals(magic);
    }

    private static String head(byte[] b) {
        return b == null ? "null" : "'" + new String(b, 0, Math.min(16, b.length), StandardCharsets.ISO_8859_1) + "…' (" + b.length + " bytes)";
    }

    private static int countOccurrences(byte[] b, String needle) {
        String s = new String(b, StandardCharsets.ISO_8859_1);
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
