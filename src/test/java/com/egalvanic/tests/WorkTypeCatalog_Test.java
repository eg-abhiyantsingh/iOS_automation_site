package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.constants.WorkTypeCatalog.Category;
import com.egalvanic.utils.ExtentReportManager;

import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TC_WT_CAT_* / TC_WT_FIX_* — Work-type backend catalog parity + QA-WT fixture
 * family integrity (Class 1 of the TC_WT_* suite; design doc
 * docs/worktype-test-design-2026-07-21.md, domain truth
 * docs/worktype-gold-spec-2026-07-21.md §1 catalog + §3 fixture family).
 *
 * WHAT: the iOS v1.51 binary resolves work-type labels from synced backend data
 * (the 13 display names are NOT hardcoded in the app — gold spec §4), so the
 * backend catalog (GET /procedures-v2/services) is the single source of truth
 * for every downstream UI assertion in Classes 2-5. This class pins that
 * catalog: per type — presence by key, exact display name, exact category
 * `type`, exact de_energized flag, pinned deterministic UUIDv5 service id;
 * catalog-wide — exactly 13 services, unique ids/keys, slug-shaped keys, no
 * "General" service (General is a UI-only 14th dropdown option persisting
 * work_type_id = null), PM Forms == 7, AF/Checklist/COM/IR/Schedule singletons,
 * de-energized member set exact (6), procedure_count present + numeric.
 *
 * FIXTURE FAMILY: durable QA-WT00..13 work orders (one per type + General,
 * gold spec §3, "do not delete"), self-provisioned on the landed site by
 * WorkTypeBaseTest.ensureFixturesOnLandedSite(). TC_WT_FIX_001 is the single
 * UI-touching test — it logs in, lands on Work Orders and triggers the ensure
 * pass; every other TC here is API-only (fast, no navigation). FIX tests pin:
 * each fixture exists, the 13 typed fixtures carry their exact work_type_id,
 * WT00/General carries JSON null (raw-literal asserted too — extract() cannot
 * see unquoted values), all active, ids distinct, names distinct and
 * punctuation-stripped per the NS-predicate contract.
 *
 * PARSING: no JSON library in this project — assertions go through
 * TestDataApi.extract / extractSiblingField plus local indexOf/brace-balanced
 * helpers (objectSlice / objectBooleanField / countToken / hasNumericField).
 * Booleans are unquoted in JSON so extract() cannot match them: ALL boolean
 * field reads route through objectBooleanField (the one shared helper).
 *
 * TRAPS ENCODED: NETA Testing's key is 'de-energized-testing' (key != name
 * slug); procedure_count is point-in-time (async + class-conditional form
 * generation) so only presence/numericness is asserted, never the value;
 * the fixture family exists on MORE THAN ONE site (company-wide list), so
 * FIX_017 asserts per-name cardinality SYMMETRY, not an absolute row count.
 *
 * Backend down => requireApi()/services() SKIP honestly with the TC id in the
 * reason. Fixture missing while the backend is healthy is a HARD FAIL — the
 * family's integrity is this class's contract, not an environment precondition.
 */
public class WorkTypeCatalog_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "Work Types (13-option dropdown)";

    /** /procedures-v2/services fetched once per class (static catalog; Class 5 owns cross-call stability). */
    private static String servicesJsonCache;
    private static int servicesFetchAttempts = 0;
    private static String servicesFetchError = "not fetched yet";

    // =====================================================================
    // JSON micro-parsers (no JSON dependency in this project — see javadoc)
    // =====================================================================

    /** Count non-overlapping occurrences of {@code token} via an indexOf loop. */
    private static int countToken(String json, String token) {
        if (json == null || token == null || token.isEmpty()) return 0;
        int count = 0;
        int from = 0;
        while ((from = json.indexOf(token, from)) >= 0) {
            count++;
            from += token.length();
        }
        return count;
    }

    /**
     * Brace-balanced slice of the JSON object containing
     * {@code "matchField": "matchValue"} (both compact and single-space
     * serializations tolerated), or null when absent. Mirrors the walk used by
     * TestDataApi.extractSiblingField but returns the whole enclosing object so
     * non-string fields (booleans, numbers, null literals) can be inspected.
     */
    private static String objectSlice(String json, String matchField, String matchValue) {
        if (json == null || matchField == null || matchValue == null) return null;
        int at = json.indexOf("\"" + matchField + "\": \"" + matchValue + "\"");
        if (at < 0) at = json.indexOf("\"" + matchField + "\":\"" + matchValue + "\"");
        if (at < 0) return null;
        int depth = 0;
        int start = -1;
        for (int i = at - 1; i >= 0; i--) {
            char c = json.charAt(i);
            if (c == '}') depth++;
            else if (c == '{') {
                if (depth == 0) { start = i; break; }
                depth--;
            }
        }
        if (start < 0) return null;
        depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return json.substring(start, i + 1);
        }
        return null;
    }

    /**
     * THE boolean-field reader (booleans are unquoted in JSON, so
     * TestDataApi.extract() can never match them): locate the object whose
     * {@code matchField} equals {@code matchValue}, then read {@code wantField}
     * as an unquoted true/false literal inside that object. Returns null when
     * the object or the boolean field is absent (JSON null is NOT a boolean).
     */
    private static Boolean objectBooleanField(String json, String matchField,
                                              String matchValue, String wantField) {
        String obj = objectSlice(json, matchField, matchValue);
        if (obj == null) return null;
        if (obj.contains("\"" + wantField + "\": true") || obj.contains("\"" + wantField + "\":true")) {
            return Boolean.TRUE;
        }
        if (obj.contains("\"" + wantField + "\": false") || obj.contains("\"" + wantField + "\":false")) {
            return Boolean.FALSE;
        }
        return null;
    }

    /** True when {@code field} exists in {@code obj} and its value starts with a digit (unquoted number). */
    private static boolean hasNumericField(String obj, String field) {
        if (obj == null || field == null) return false;
        int at = obj.indexOf("\"" + field + "\"");
        if (at < 0) return false;
        int i = at + field.length() + 2; // past the closing quote of the field name
        while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) i++;
        if (i >= obj.length() || obj.charAt(i) != ':') return false;
        i++;
        while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) i++;
        return i < obj.length() && Character.isDigit(obj.charAt(i));
    }

    /** Truncate long JSON slices for assertion messages / logs. */
    private static String trunc(String s, int n) {
        if (s == null) return "null";
        return s.length() <= n ? s : s.substring(0, n) + "\u2026";
    }

    /** Expected raw `type` string for a catalog category (gold spec §1 table). */
    private static String expectedTypeString(Category c) {
        switch (c) {
            case AF:        return "AF";
            case CHECKLIST: return "Checklist";
            case COM:       return "COM";
            case IR:        return "IR";
            case SCHEDULE:  return "Schedule";
            case PM_FORMS:  return "PM Forms";
            default:        return null; // NONE — General has no backing service
        }
    }

    // =====================================================================
    // Backend access wrappers (SKIP honestly on unreachable backend; a
    // missing fixture with a healthy backend stays a HARD FAIL)
    // =====================================================================

    /** Cached services catalog; SKIPs with the TC id when the endpoint is unreachable (2 attempts max). */
    private String services(String tcId) {
        TestDataApi a = requireApi(tcId);
        synchronized (WorkTypeCatalog_Test.class) {
            if (servicesJsonCache == null && servicesFetchAttempts < 2) {
                servicesFetchAttempts++;
                try {
                    servicesJsonCache = a.workTypeServicesJson();
                    logStep("Fetched /procedures-v2/services (" + servicesJsonCache.length()
                            + " chars) — cached for the class");
                } catch (Exception e) {
                    servicesFetchError = String.valueOf(e.getMessage());
                }
            }
        }
        skipIfPreconditionMissing(() -> servicesJsonCache != null,
                tcId + ": GET /procedures-v2/services unreachable (" + servicesFetchError
                        + ") — cannot verify the catalog contract");
        return servicesJsonCache;
    }

    private String woList(String tcId, TestDataApi a, String search) {
        try {
            return a.listWorkOrdersJson(search);
        } catch (Exception e) {
            skipIfPreconditionMissing(() -> false,
                    tcId + ": company WO list endpoint unreachable — " + e.getMessage());
            return null; // unreachable: the skip above always throws
        }
    }

    private String findWoId(String tcId, TestDataApi a, String name) {
        try {
            return a.findWorkOrderIdByName(name);
        } catch (Exception e) {
            skipIfPreconditionMissing(() -> false,
                    tcId + ": WO lookup for '" + name + "' unreachable — " + e.getMessage());
            return null;
        }
    }

    private String woTypeId(String tcId, TestDataApi a, String name) {
        try {
            return a.workOrderWorkTypeId(name);
        } catch (Exception e) {
            skipIfPreconditionMissing(() -> false,
                    tcId + ": work_type_id lookup for '" + name + "' unreachable — " + e.getMessage());
            return null;
        }
    }

    // =====================================================================
    // Shared per-type checks (each @Test below is one contract on one type)
    // =====================================================================

    private void checkServicePresent(String tcId, WorkTypeCatalog wt) {
        String json = services(tcId);
        logStep(tcId + ": catalog lookup for key '" + wt.key() + "' (" + wt.displayName() + ")");
        assertTrue(json.contains("\"" + wt.key() + "\""),
                "Services catalog must contain the quoted key literal '" + wt.key()
                        + "' (gold spec \u00a71; NETA trap: key != name slug)");
        assertTrue(objectSlice(json, "key", wt.key()) != null,
                "A brace-balanced service object keyed '" + wt.key()
                        + "' must exist in /procedures-v2/services");
        logStep(tcId + ": service '" + wt.key() + "' present");
    }

    private void checkDisplayName(String tcId, WorkTypeCatalog wt) {
        String json = services(tcId);
        String actual = TestDataApi.extractSiblingField(json, "key", wt.key(), "name");
        logStep(tcId + ": server name for '" + wt.key() + "' = '" + actual + "'");
        assertTrue(actual != null,
                "Service '" + wt.key() + "' must expose a string 'name' field");
        assertEquals(actual, wt.displayName(),
                "Display name for key '" + wt.key()
                        + "' must match the gold spec EXACTLY (incl. punctuation) — iOS resolves labels from this value");
    }

    private void checkCategoryType(String tcId, WorkTypeCatalog wt) {
        String json = services(tcId);
        String expected = expectedTypeString(wt.category());
        String actual = TestDataApi.extractSiblingField(json, "key", wt.key(), "type");
        logStep(tcId + ": server type for '" + wt.key() + "' = '" + actual + "' (expected '" + expected + "')");
        assertTrue(actual != null,
                "Service '" + wt.key() + "' must expose a string 'type' (category) field");
        assertEquals(actual, expected,
                "Category 'type' for '" + wt.key() + "' must be '" + expected
                        + "' — drives which tabs/columns the WO detail exposes (gold spec \u00a75)");
    }

    private void checkDeEnergized(String tcId, WorkTypeCatalog wt) {
        String json = services(tcId);
        Boolean actual = objectBooleanField(json, "key", wt.key(), "de_energized");
        logStep(tcId + ": server de_energized for '" + wt.key() + "' = " + actual);
        assertTrue(actual != null,
                "'de_energized' must be present as an unquoted boolean on service '" + wt.key()
                        + "' (extract() cannot see booleans — objectBooleanField contract)");
        assertEquals(actual, Boolean.valueOf(wt.deEnergized()),
                "de_energized for '" + wt.key() + "' must be " + wt.deEnergized() + " (gold spec \u00a71)");
    }

    private void checkServiceId(String tcId, WorkTypeCatalog wt) {
        String json = services(tcId);
        String id = TestDataApi.extractSiblingField(json, "key", wt.key(), "id");
        logStep(tcId + ": server id for '" + wt.key() + "' = " + id);
        assertTrue(id != null, "Service '" + wt.key() + "' must expose an 'id' field");
        assertEquals(id, wt.serviceId(),
                "Service id for '" + wt.key()
                        + "' must equal the pinned constant (deterministic UUIDv5 — stable across environments/syncs)");
        assertEquals(Integer.valueOf(id.length()), Integer.valueOf(36),
                "Service id must be a canonical 36-char UUID, got '" + id + "'");
        assertEquals(String.valueOf(id.charAt(14)), "5",
                "UUID version nibble (charAt 14) must be '5' (UUIDv5) — got '" + id + "'");
    }

    // ================== Arc Flash Data Collection  (key: arc-flash-study) ==================

    @Test(priority = 1, description = "TC_WT_CAT_001 - Arc Flash Data Collection: service present by key 'arc-flash-study' in /procedures-v2/services")
    public void TC_WT_CAT_001_afDataCollection_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_001 - Arc Flash Data Collection: service present by key 'arc-flash-study' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_001", WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION);
    }

    @Test(priority = 2, description = "TC_WT_CAT_002 - Arc Flash Data Collection: catalog display name is exactly 'Arc Flash Data Collection' for key 'arc-flash-study'")
    public void TC_WT_CAT_002_afDataCollection_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_002 - Arc Flash Data Collection: catalog display name is exactly 'Arc Flash Data Collection' for key 'arc-flash-study'");
        checkDisplayName("TC_WT_CAT_002", WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION);
    }

    @Test(priority = 3, description = "TC_WT_CAT_003 - Arc Flash Data Collection: raw service object 'type' field equals 'AF'")
    public void TC_WT_CAT_003_afDataCollection_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_003 - Arc Flash Data Collection: raw service object 'type' field equals 'AF'");
        checkCategoryType("TC_WT_CAT_003", WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION);
    }

    @Test(priority = 4, description = "TC_WT_CAT_004 - Arc Flash Data Collection: de_energized is exactly false (unquoted JSON boolean)")
    public void TC_WT_CAT_004_afDataCollection_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_004 - Arc Flash Data Collection: de_energized is exactly false (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_004", WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION);
    }

    @Test(priority = 5, description = "TC_WT_CAT_005 - Arc Flash Data Collection: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_005_afDataCollection_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_005 - Arc Flash Data Collection: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_005", WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION);
    }

    // ================== Arc Flash Label Placement  (key: arc-flash-label-placement) ==================

    @Test(priority = 6, description = "TC_WT_CAT_006 - Arc Flash Label Placement: service present by key 'arc-flash-label-placement' in /procedures-v2/services")
    public void TC_WT_CAT_006_afLabelPlacement_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_006 - Arc Flash Label Placement: service present by key 'arc-flash-label-placement' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_006", WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT);
    }

    @Test(priority = 7, description = "TC_WT_CAT_007 - Arc Flash Label Placement: catalog display name is exactly 'Arc Flash Label Placement' for key 'arc-flash-label-placement'")
    public void TC_WT_CAT_007_afLabelPlacement_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_007 - Arc Flash Label Placement: catalog display name is exactly 'Arc Flash Label Placement' for key 'arc-flash-label-placement'");
        checkDisplayName("TC_WT_CAT_007", WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT);
    }

    @Test(priority = 8, description = "TC_WT_CAT_008 - Arc Flash Label Placement: raw service object 'type' field equals 'Checklist'")
    public void TC_WT_CAT_008_afLabelPlacement_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_008 - Arc Flash Label Placement: raw service object 'type' field equals 'Checklist'");
        checkCategoryType("TC_WT_CAT_008", WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT);
    }

    @Test(priority = 9, description = "TC_WT_CAT_009 - Arc Flash Label Placement: de_energized is exactly false (unquoted JSON boolean)")
    public void TC_WT_CAT_009_afLabelPlacement_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_009 - Arc Flash Label Placement: de_energized is exactly false (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_009", WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT);
    }

    @Test(priority = 10, description = "TC_WT_CAT_010 - Arc Flash Label Placement: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_010_afLabelPlacement_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_010 - Arc Flash Label Placement: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_010", WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT);
    }

    // ================== Cleaning  (key: cleaning) ==================

    @Test(priority = 11, description = "TC_WT_CAT_011 - Cleaning: service present by key 'cleaning' in /procedures-v2/services")
    public void TC_WT_CAT_011_cleaning_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_011 - Cleaning: service present by key 'cleaning' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_011", WorkTypeCatalog.CLEANING);
    }

    @Test(priority = 12, description = "TC_WT_CAT_012 - Cleaning: catalog display name is exactly 'Cleaning' for key 'cleaning'")
    public void TC_WT_CAT_012_cleaning_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_012 - Cleaning: catalog display name is exactly 'Cleaning' for key 'cleaning'");
        checkDisplayName("TC_WT_CAT_012", WorkTypeCatalog.CLEANING);
    }

    @Test(priority = 13, description = "TC_WT_CAT_013 - Cleaning: raw service object 'type' field equals 'PM Forms'")
    public void TC_WT_CAT_013_cleaning_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_013 - Cleaning: raw service object 'type' field equals 'PM Forms'");
        checkCategoryType("TC_WT_CAT_013", WorkTypeCatalog.CLEANING);
    }

    @Test(priority = 14, description = "TC_WT_CAT_014 - Cleaning: de_energized is exactly true (unquoted JSON boolean)")
    public void TC_WT_CAT_014_cleaning_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_014 - Cleaning: de_energized is exactly true (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_014", WorkTypeCatalog.CLEANING);
    }

    @Test(priority = 15, description = "TC_WT_CAT_015 - Cleaning: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_015_cleaning_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_015 - Cleaning: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_015", WorkTypeCatalog.CLEANING);
    }

    // ================== Clean, Tighten, Torque  (key: clean-tighten-torque) ==================

    @Test(priority = 16, description = "TC_WT_CAT_016 - Clean, Tighten, Torque: service present by key 'clean-tighten-torque' in /procedures-v2/services")
    public void TC_WT_CAT_016_cleanTightenTorque_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_016 - Clean, Tighten, Torque: service present by key 'clean-tighten-torque' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_016", WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE);
    }

    @Test(priority = 17, description = "TC_WT_CAT_017 - Clean, Tighten, Torque: catalog display name is exactly 'Clean, Tighten, Torque' for key 'clean-tighten-torque'")
    public void TC_WT_CAT_017_cleanTightenTorque_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_017 - Clean, Tighten, Torque: catalog display name is exactly 'Clean, Tighten, Torque' for key 'clean-tighten-torque'");
        checkDisplayName("TC_WT_CAT_017", WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE);
    }

    @Test(priority = 18, description = "TC_WT_CAT_018 - Clean, Tighten, Torque: raw service object 'type' field equals 'PM Forms'")
    public void TC_WT_CAT_018_cleanTightenTorque_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_018 - Clean, Tighten, Torque: raw service object 'type' field equals 'PM Forms'");
        checkCategoryType("TC_WT_CAT_018", WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE);
    }

    @Test(priority = 19, description = "TC_WT_CAT_019 - Clean, Tighten, Torque: de_energized is exactly true (unquoted JSON boolean)")
    public void TC_WT_CAT_019_cleanTightenTorque_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_019 - Clean, Tighten, Torque: de_energized is exactly true (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_019", WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE);
    }

    @Test(priority = 20, description = "TC_WT_CAT_020 - Clean, Tighten, Torque: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_020_cleanTightenTorque_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_020 - Clean, Tighten, Torque: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_020", WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE);
    }

    // ================== Condition Assessment  (key: condition-assessment) ==================

    @Test(priority = 21, description = "TC_WT_CAT_021 - Condition Assessment: service present by key 'condition-assessment' in /procedures-v2/services")
    public void TC_WT_CAT_021_conditionAssessment_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_021 - Condition Assessment: service present by key 'condition-assessment' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_021", WorkTypeCatalog.CONDITION_ASSESSMENT);
    }

    @Test(priority = 22, description = "TC_WT_CAT_022 - Condition Assessment: catalog display name is exactly 'Condition Assessment' for key 'condition-assessment'")
    public void TC_WT_CAT_022_conditionAssessment_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_022 - Condition Assessment: catalog display name is exactly 'Condition Assessment' for key 'condition-assessment'");
        checkDisplayName("TC_WT_CAT_022", WorkTypeCatalog.CONDITION_ASSESSMENT);
    }

    @Test(priority = 23, description = "TC_WT_CAT_023 - Condition Assessment: raw service object 'type' field equals 'COM'")
    public void TC_WT_CAT_023_conditionAssessment_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_023 - Condition Assessment: raw service object 'type' field equals 'COM'");
        checkCategoryType("TC_WT_CAT_023", WorkTypeCatalog.CONDITION_ASSESSMENT);
    }

    @Test(priority = 24, description = "TC_WT_CAT_024 - Condition Assessment: de_energized is exactly false (unquoted JSON boolean)")
    public void TC_WT_CAT_024_conditionAssessment_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_024 - Condition Assessment: de_energized is exactly false (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_024", WorkTypeCatalog.CONDITION_ASSESSMENT);
    }

    @Test(priority = 25, description = "TC_WT_CAT_025 - Condition Assessment: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_025_conditionAssessment_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_025 - Condition Assessment: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_025", WorkTypeCatalog.CONDITION_ASSESSMENT);
    }

    // ================== De-Energized Visual Inspection  (key: de-energized-visual-inspection) ==================

    @Test(priority = 26, description = "TC_WT_CAT_026 - De-Energized Visual Inspection: service present by key 'de-energized-visual-inspection' in /procedures-v2/services")
    public void TC_WT_CAT_026_deEnergizedVisual_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_026 - De-Energized Visual Inspection: service present by key 'de-energized-visual-inspection' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_026", WorkTypeCatalog.DE_ENERGIZED_VISUAL);
    }

    @Test(priority = 27, description = "TC_WT_CAT_027 - De-Energized Visual Inspection: catalog display name is exactly 'De-Energized Visual Inspection' for key 'de-energized-visual-inspection'")
    public void TC_WT_CAT_027_deEnergizedVisual_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_027 - De-Energized Visual Inspection: catalog display name is exactly 'De-Energized Visual Inspection' for key 'de-energized-visual-inspection'");
        checkDisplayName("TC_WT_CAT_027", WorkTypeCatalog.DE_ENERGIZED_VISUAL);
    }

    @Test(priority = 28, description = "TC_WT_CAT_028 - De-Energized Visual Inspection: raw service object 'type' field equals 'PM Forms'")
    public void TC_WT_CAT_028_deEnergizedVisual_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_028 - De-Energized Visual Inspection: raw service object 'type' field equals 'PM Forms'");
        checkCategoryType("TC_WT_CAT_028", WorkTypeCatalog.DE_ENERGIZED_VISUAL);
    }

    @Test(priority = 29, description = "TC_WT_CAT_029 - De-Energized Visual Inspection: de_energized is exactly true (unquoted JSON boolean)")
    public void TC_WT_CAT_029_deEnergizedVisual_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_029 - De-Energized Visual Inspection: de_energized is exactly true (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_029", WorkTypeCatalog.DE_ENERGIZED_VISUAL);
    }

    @Test(priority = 30, description = "TC_WT_CAT_030 - De-Energized Visual Inspection: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_030_deEnergizedVisual_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_030 - De-Energized Visual Inspection: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_030", WorkTypeCatalog.DE_ENERGIZED_VISUAL);
    }

    // ================== DGA / Fluid Sample Analysis  (key: dga-fluid-sample-analysis) ==================

    @Test(priority = 31, description = "TC_WT_CAT_031 - DGA / Fluid Sample Analysis: service present by key 'dga-fluid-sample-analysis' in /procedures-v2/services")
    public void TC_WT_CAT_031_dgaFluidSample_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_031 - DGA / Fluid Sample Analysis: service present by key 'dga-fluid-sample-analysis' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_031", WorkTypeCatalog.DGA_FLUID_SAMPLE);
    }

    @Test(priority = 32, description = "TC_WT_CAT_032 - DGA / Fluid Sample Analysis: catalog display name is exactly 'DGA / Fluid Sample Analysis' for key 'dga-fluid-sample-analysis'")
    public void TC_WT_CAT_032_dgaFluidSample_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_032 - DGA / Fluid Sample Analysis: catalog display name is exactly 'DGA / Fluid Sample Analysis' for key 'dga-fluid-sample-analysis'");
        checkDisplayName("TC_WT_CAT_032", WorkTypeCatalog.DGA_FLUID_SAMPLE);
    }

    @Test(priority = 33, description = "TC_WT_CAT_033 - DGA / Fluid Sample Analysis: raw service object 'type' field equals 'PM Forms'")
    public void TC_WT_CAT_033_dgaFluidSample_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_033 - DGA / Fluid Sample Analysis: raw service object 'type' field equals 'PM Forms'");
        checkCategoryType("TC_WT_CAT_033", WorkTypeCatalog.DGA_FLUID_SAMPLE);
    }

    @Test(priority = 34, description = "TC_WT_CAT_034 - DGA / Fluid Sample Analysis: de_energized is exactly false (unquoted JSON boolean)")
    public void TC_WT_CAT_034_dgaFluidSample_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_034 - DGA / Fluid Sample Analysis: de_energized is exactly false (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_034", WorkTypeCatalog.DGA_FLUID_SAMPLE);
    }

    @Test(priority = 35, description = "TC_WT_CAT_035 - DGA / Fluid Sample Analysis: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_035_dgaFluidSample_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_035 - DGA / Fluid Sample Analysis: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_035", WorkTypeCatalog.DGA_FLUID_SAMPLE);
    }

    // ================== Infrared Thermography  (key: infrared-thermography) ==================

    @Test(priority = 36, description = "TC_WT_CAT_036 - Infrared Thermography: service present by key 'infrared-thermography' in /procedures-v2/services")
    public void TC_WT_CAT_036_infraredThermography_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_036 - Infrared Thermography: service present by key 'infrared-thermography' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_036", WorkTypeCatalog.INFRARED_THERMOGRAPHY);
    }

    @Test(priority = 37, description = "TC_WT_CAT_037 - Infrared Thermography: catalog display name is exactly 'Infrared Thermography' for key 'infrared-thermography'")
    public void TC_WT_CAT_037_infraredThermography_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_037 - Infrared Thermography: catalog display name is exactly 'Infrared Thermography' for key 'infrared-thermography'");
        checkDisplayName("TC_WT_CAT_037", WorkTypeCatalog.INFRARED_THERMOGRAPHY);
    }

    @Test(priority = 38, description = "TC_WT_CAT_038 - Infrared Thermography: raw service object 'type' field equals 'IR'")
    public void TC_WT_CAT_038_infraredThermography_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_038 - Infrared Thermography: raw service object 'type' field equals 'IR'");
        checkCategoryType("TC_WT_CAT_038", WorkTypeCatalog.INFRARED_THERMOGRAPHY);
    }

    @Test(priority = 39, description = "TC_WT_CAT_039 - Infrared Thermography: de_energized is exactly false (unquoted JSON boolean)")
    public void TC_WT_CAT_039_infraredThermography_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_039 - Infrared Thermography: de_energized is exactly false (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_039", WorkTypeCatalog.INFRARED_THERMOGRAPHY);
    }

    @Test(priority = 40, description = "TC_WT_CAT_040 - Infrared Thermography: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_040_infraredThermography_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_040 - Infrared Thermography: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_040", WorkTypeCatalog.INFRARED_THERMOGRAPHY);
    }

    // ================== Insulation Resistance Testing  (key: insulation-resistance-testing) ==================

    @Test(priority = 41, description = "TC_WT_CAT_041 - Insulation Resistance Testing: service present by key 'insulation-resistance-testing' in /procedures-v2/services")
    public void TC_WT_CAT_041_insulationResistance_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_041 - Insulation Resistance Testing: service present by key 'insulation-resistance-testing' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_041", WorkTypeCatalog.INSULATION_RESISTANCE);
    }

    @Test(priority = 42, description = "TC_WT_CAT_042 - Insulation Resistance Testing: catalog display name is exactly 'Insulation Resistance Testing' for key 'insulation-resistance-testing'")
    public void TC_WT_CAT_042_insulationResistance_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_042 - Insulation Resistance Testing: catalog display name is exactly 'Insulation Resistance Testing' for key 'insulation-resistance-testing'");
        checkDisplayName("TC_WT_CAT_042", WorkTypeCatalog.INSULATION_RESISTANCE);
    }

    @Test(priority = 43, description = "TC_WT_CAT_043 - Insulation Resistance Testing: raw service object 'type' field equals 'PM Forms'")
    public void TC_WT_CAT_043_insulationResistance_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_043 - Insulation Resistance Testing: raw service object 'type' field equals 'PM Forms'");
        checkCategoryType("TC_WT_CAT_043", WorkTypeCatalog.INSULATION_RESISTANCE);
    }

    @Test(priority = 44, description = "TC_WT_CAT_044 - Insulation Resistance Testing: de_energized is exactly true (unquoted JSON boolean)")
    public void TC_WT_CAT_044_insulationResistance_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_044 - Insulation Resistance Testing: de_energized is exactly true (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_044", WorkTypeCatalog.INSULATION_RESISTANCE);
    }

    @Test(priority = 45, description = "TC_WT_CAT_045 - Insulation Resistance Testing: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_045_insulationResistance_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_045 - Insulation Resistance Testing: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_045", WorkTypeCatalog.INSULATION_RESISTANCE);
    }

    // ================== NETA Testing  (key: de-energized-testing) ==================

    @Test(priority = 46, description = "TC_WT_CAT_046 - NETA Testing: service present by key 'de-energized-testing' in /procedures-v2/services")
    public void TC_WT_CAT_046_netaTesting_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_046 - NETA Testing: service present by key 'de-energized-testing' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_046", WorkTypeCatalog.NETA_TESTING);
    }

    @Test(priority = 47, description = "TC_WT_CAT_047 - NETA Testing: catalog display name is exactly 'NETA Testing' for key 'de-energized-testing'")
    public void TC_WT_CAT_047_netaTesting_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_047 - NETA Testing: catalog display name is exactly 'NETA Testing' for key 'de-energized-testing'");
        checkDisplayName("TC_WT_CAT_047", WorkTypeCatalog.NETA_TESTING);
    }

    @Test(priority = 48, description = "TC_WT_CAT_048 - NETA Testing: raw service object 'type' field equals 'PM Forms'")
    public void TC_WT_CAT_048_netaTesting_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_048 - NETA Testing: raw service object 'type' field equals 'PM Forms'");
        checkCategoryType("TC_WT_CAT_048", WorkTypeCatalog.NETA_TESTING);
    }

    @Test(priority = 49, description = "TC_WT_CAT_049 - NETA Testing: de_energized is exactly true (unquoted JSON boolean)")
    public void TC_WT_CAT_049_netaTesting_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_049 - NETA Testing: de_energized is exactly true (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_049", WorkTypeCatalog.NETA_TESTING);
    }

    @Test(priority = 50, description = "TC_WT_CAT_050 - NETA Testing: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_050_netaTesting_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_050 - NETA Testing: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_050", WorkTypeCatalog.NETA_TESTING);
    }

    // ================== Panel Schedule Updates  (key: panel-schedule-updates) ==================

    @Test(priority = 51, description = "TC_WT_CAT_051 - Panel Schedule Updates: service present by key 'panel-schedule-updates' in /procedures-v2/services")
    public void TC_WT_CAT_051_panelScheduleUpdates_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_051 - Panel Schedule Updates: service present by key 'panel-schedule-updates' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_051", WorkTypeCatalog.PANEL_SCHEDULE_UPDATES);
    }

    @Test(priority = 52, description = "TC_WT_CAT_052 - Panel Schedule Updates: catalog display name is exactly 'Panel Schedule Updates' for key 'panel-schedule-updates'")
    public void TC_WT_CAT_052_panelScheduleUpdates_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_052 - Panel Schedule Updates: catalog display name is exactly 'Panel Schedule Updates' for key 'panel-schedule-updates'");
        checkDisplayName("TC_WT_CAT_052", WorkTypeCatalog.PANEL_SCHEDULE_UPDATES);
    }

    @Test(priority = 53, description = "TC_WT_CAT_053 - Panel Schedule Updates: raw service object 'type' field equals 'Schedule'")
    public void TC_WT_CAT_053_panelScheduleUpdates_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_053 - Panel Schedule Updates: raw service object 'type' field equals 'Schedule'");
        checkCategoryType("TC_WT_CAT_053", WorkTypeCatalog.PANEL_SCHEDULE_UPDATES);
    }

    @Test(priority = 54, description = "TC_WT_CAT_054 - Panel Schedule Updates: de_energized is exactly false (unquoted JSON boolean)")
    public void TC_WT_CAT_054_panelScheduleUpdates_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_054 - Panel Schedule Updates: de_energized is exactly false (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_054", WorkTypeCatalog.PANEL_SCHEDULE_UPDATES);
    }

    @Test(priority = 55, description = "TC_WT_CAT_055 - Panel Schedule Updates: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_055_panelScheduleUpdates_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_055 - Panel Schedule Updates: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_055", WorkTypeCatalog.PANEL_SCHEDULE_UPDATES);
    }

    // ================== Shutdown (Composite)  (key: composite-shutdown-emp) ==================

    @Test(priority = 56, description = "TC_WT_CAT_056 - Shutdown (Composite): service present by key 'composite-shutdown-emp' in /procedures-v2/services")
    public void TC_WT_CAT_056_shutdownComposite_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_056 - Shutdown (Composite): service present by key 'composite-shutdown-emp' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_056", WorkTypeCatalog.SHUTDOWN_COMPOSITE);
    }

    @Test(priority = 57, description = "TC_WT_CAT_057 - Shutdown (Composite): catalog display name is exactly 'Shutdown (Composite)' for key 'composite-shutdown-emp'")
    public void TC_WT_CAT_057_shutdownComposite_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_057 - Shutdown (Composite): catalog display name is exactly 'Shutdown (Composite)' for key 'composite-shutdown-emp'");
        checkDisplayName("TC_WT_CAT_057", WorkTypeCatalog.SHUTDOWN_COMPOSITE);
    }

    @Test(priority = 58, description = "TC_WT_CAT_058 - Shutdown (Composite): raw service object 'type' field equals 'PM Forms'")
    public void TC_WT_CAT_058_shutdownComposite_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_058 - Shutdown (Composite): raw service object 'type' field equals 'PM Forms'");
        checkCategoryType("TC_WT_CAT_058", WorkTypeCatalog.SHUTDOWN_COMPOSITE);
    }

    @Test(priority = 59, description = "TC_WT_CAT_059 - Shutdown (Composite): de_energized is exactly true (unquoted JSON boolean)")
    public void TC_WT_CAT_059_shutdownComposite_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_059 - Shutdown (Composite): de_energized is exactly true (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_059", WorkTypeCatalog.SHUTDOWN_COMPOSITE);
    }

    @Test(priority = 60, description = "TC_WT_CAT_060 - Shutdown (Composite): service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_060_shutdownComposite_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_060 - Shutdown (Composite): service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_060", WorkTypeCatalog.SHUTDOWN_COMPOSITE);
    }

    // ================== UPS Maintenance  (key: ups-maintenance) ==================

    @Test(priority = 61, description = "TC_WT_CAT_061 - UPS Maintenance: service present by key 'ups-maintenance' in /procedures-v2/services")
    public void TC_WT_CAT_061_upsMaintenance_servicePresentByKey() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_061 - UPS Maintenance: service present by key 'ups-maintenance' in /procedures-v2/services");
        checkServicePresent("TC_WT_CAT_061", WorkTypeCatalog.UPS_MAINTENANCE);
    }

    @Test(priority = 62, description = "TC_WT_CAT_062 - UPS Maintenance: catalog display name is exactly 'UPS Maintenance' for key 'ups-maintenance'")
    public void TC_WT_CAT_062_upsMaintenance_displayNameExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_062 - UPS Maintenance: catalog display name is exactly 'UPS Maintenance' for key 'ups-maintenance'");
        checkDisplayName("TC_WT_CAT_062", WorkTypeCatalog.UPS_MAINTENANCE);
    }

    @Test(priority = 63, description = "TC_WT_CAT_063 - UPS Maintenance: raw service object 'type' field equals 'PM Forms'")
    public void TC_WT_CAT_063_upsMaintenance_categoryTypeExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_063 - UPS Maintenance: raw service object 'type' field equals 'PM Forms'");
        checkCategoryType("TC_WT_CAT_063", WorkTypeCatalog.UPS_MAINTENANCE);
    }

    @Test(priority = 64, description = "TC_WT_CAT_064 - UPS Maintenance: de_energized is exactly false (unquoted JSON boolean)")
    public void TC_WT_CAT_064_upsMaintenance_deEnergizedExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_064 - UPS Maintenance: de_energized is exactly false (unquoted JSON boolean)");
        checkDeEnergized("TC_WT_CAT_064", WorkTypeCatalog.UPS_MAINTENANCE);
    }

    @Test(priority = 65, description = "TC_WT_CAT_065 - UPS Maintenance: service id equals the pinned constant and is UUIDv5 (version nibble '5')")
    public void TC_WT_CAT_065_upsMaintenance_serviceIdPinnedUuidV5() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_065 - UPS Maintenance: service id equals the pinned constant and is UUIDv5 (version nibble '5')");
        checkServiceId("TC_WT_CAT_065", WorkTypeCatalog.UPS_MAINTENANCE);
    }

    // ================== Catalog-wide contracts ==================

    @Test(priority = 66, description = "TC_WT_CAT_066 - catalog returns exactly 13 service-backed work types (SERVICE_COUNT)")
    public void TC_WT_CAT_066_exactlyThirteenServices() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_066 - catalog returns exactly 13 service-backed work types (SERVICE_COUNT)");
        String json = services("TC_WT_CAT_066");
        int keyFields = countToken(json, "\"key\"");
        logStep("\"key\" field occurrences in /procedures-v2/services: " + keyFields);
        assertEquals(Integer.valueOf(keyFields), Integer.valueOf(WorkTypeCatalog.SERVICE_COUNT),
                "Catalog must contain exactly " + WorkTypeCatalog.SERVICE_COUNT
                        + " services (one \"key\" field each) — gold spec \u00a71");
        assertEquals(Integer.valueOf(WorkTypeCatalog.serviceBacked().size()), Integer.valueOf(WorkTypeCatalog.SERVICE_COUNT),
                "Enum must model exactly 13 service-backed types");
        assertEquals(Integer.valueOf(WorkTypeCatalog.values().length), Integer.valueOf(14),
                "Enum must model 13 services + the UI-only General");
    }

    @Test(priority = 67, description = "TC_WT_CAT_067 - all 13 server-side service ids are non-null and pairwise distinct")
    public void TC_WT_CAT_067_serviceIdsUniqueAcrossCatalog() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_067 - all 13 server-side service ids are non-null and pairwise distinct");
        String json = services("TC_WT_CAT_067");
        Set<String> ids = new LinkedHashSet<>();
        for (WorkTypeCatalog wt : WorkTypeCatalog.serviceBacked()) {
            String id = TestDataApi.extractSiblingField(json, "key", wt.key(), "id");
            assertTrue(id != null, "Server id must exist for key '" + wt.key() + "'");
            assertTrue(ids.add(id), "Server ids must be pairwise distinct — duplicate " + id
                    + " on key '" + wt.key() + "'");
        }
        logStep("Distinct server service ids: " + ids.size());
        assertEquals(Integer.valueOf(ids.size()), Integer.valueOf(WorkTypeCatalog.SERVICE_COUNT),
                "Exactly 13 distinct service ids expected");
    }

    @Test(priority = 68, description = "TC_WT_CAT_068 - the 13 service keys are pairwise distinct, slug-shaped, and all present server-side")
    public void TC_WT_CAT_068_keysUniqueAndSlugShaped() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_068 - the 13 service keys are pairwise distinct, slug-shaped, and all present server-side");
        String json = services("TC_WT_CAT_068");
        Set<String> keys = new LinkedHashSet<>();
        for (WorkTypeCatalog wt : WorkTypeCatalog.serviceBacked()) {
            String k = wt.key();
            assertTrue(k != null && !k.isEmpty(), "Service-backed type " + wt + " must have a key");
            assertTrue(k.matches("[a-z0-9]+(-[a-z0-9]+)*"),
                    "Key must be slug-shaped (lowercase alnum + single hyphens): '" + k + "'");
            assertTrue(keys.add(k), "Keys must be pairwise distinct — duplicate '" + k + "'");
            assertTrue(json.contains("\"" + k + "\""),
                    "Server catalog must contain key '" + k + "'");
        }
        logStep("Distinct slug-shaped keys verified: " + keys.size());
        assertEquals(Integer.valueOf(keys.size()), Integer.valueOf(WorkTypeCatalog.SERVICE_COUNT),
                "Exactly 13 distinct keys expected");
    }

    @Test(priority = 69, description = "TC_WT_CAT_069 - 'General' is UI-only: no backing service exists for it in /procedures-v2/services")
    public void TC_WT_CAT_069_noGeneralServiceInCatalog() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_069 - 'General' is UI-only: no backing service exists for it in /procedures-v2/services");
        String json = services("TC_WT_CAT_069");
        assertTrue(TestDataApi.extractSiblingField(json, "name", "General", "id") == null,
                "No service named 'General' may exist — General is the UI-only 14th dropdown option (work_type_id = null)");
        assertEquals(Integer.valueOf(countToken(json, "\"General\"")), Integer.valueOf(0),
                "The quoted literal \"General\" must not appear anywhere in the services catalog");
        assertFalse(WorkTypeCatalog.GENERAL.isServiceBacked(),
                "Enum GENERAL must be modeled as NOT service-backed");
        assertTrue(WorkTypeCatalog.GENERAL.key() == null && WorkTypeCatalog.GENERAL.serviceId() == null,
                "Enum GENERAL must carry null key and null service id");
        logStep("No General service in catalog — UI-only option confirmed");
    }

    @Test(priority = 70, description = "TC_WT_CAT_070 - exactly 7 of 13 services are category 'PM Forms' (server literal count + enum model)")
    public void TC_WT_CAT_070_pmFormsCategoryHasExactlySeven() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_070 - exactly 7 of 13 services are category 'PM Forms' (server literal count + enum model)");
        String json = services("TC_WT_CAT_070");
        int serverPmForms = countToken(json, "\"PM Forms\"");
        logStep("Server \"PM Forms\" literal occurrences: " + serverPmForms);
        assertEquals(Integer.valueOf(serverPmForms), Integer.valueOf(7),
                "Exactly 7 services must carry type 'PM Forms' (gold spec \u00a71 distribution)");
        assertEquals(Integer.valueOf(WorkTypeCatalog.ofCategory(Category.PM_FORMS).size()), Integer.valueOf(7),
                "Enum must model exactly 7 PM_FORMS types");
    }

    @Test(priority = 71, description = "TC_WT_CAT_071 - category 'AF' is a singleton — only Arc Flash Data Collection")
    public void TC_WT_CAT_071_afCategorySingleton() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_071 - category 'AF' is a singleton — only Arc Flash Data Collection");
        String json = services("TC_WT_CAT_071");
        List<WorkTypeCatalog> members = WorkTypeCatalog.ofCategory(Category.AF);
        assertEquals(Integer.valueOf(members.size()), Integer.valueOf(1),
                "Exactly one AF type must exist in the model — got " + members);
        assertEquals(members.get(0), WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION,
                "The AF singleton must be Arc Flash Data Collection");
        int serverCount = countToken(json, "\"AF\"");
        logStep("Server \"AF\" literal occurrences: " + serverCount);
        assertEquals(Integer.valueOf(serverCount), Integer.valueOf(1),
                "Exactly one service must carry type 'AF' (gold spec \u00a71 distribution)");
    }

    @Test(priority = 72, description = "TC_WT_CAT_072 - category 'Checklist' is a singleton — only Arc Flash Label Placement")
    public void TC_WT_CAT_072_checklistCategorySingleton() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_072 - category 'Checklist' is a singleton — only Arc Flash Label Placement");
        String json = services("TC_WT_CAT_072");
        List<WorkTypeCatalog> members = WorkTypeCatalog.ofCategory(Category.CHECKLIST);
        assertEquals(Integer.valueOf(members.size()), Integer.valueOf(1),
                "Exactly one CHECKLIST type must exist in the model — got " + members);
        assertEquals(members.get(0), WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT,
                "The CHECKLIST singleton must be Arc Flash Label Placement");
        int serverCount = countToken(json, "\"Checklist\"");
        logStep("Server \"Checklist\" literal occurrences: " + serverCount);
        assertEquals(Integer.valueOf(serverCount), Integer.valueOf(1),
                "Exactly one service must carry type 'Checklist' (gold spec \u00a71 distribution)");
    }

    @Test(priority = 73, description = "TC_WT_CAT_073 - category 'COM' is a singleton — only Condition Assessment")
    public void TC_WT_CAT_073_comCategorySingleton() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_073 - category 'COM' is a singleton — only Condition Assessment");
        String json = services("TC_WT_CAT_073");
        List<WorkTypeCatalog> members = WorkTypeCatalog.ofCategory(Category.COM);
        assertEquals(Integer.valueOf(members.size()), Integer.valueOf(1),
                "Exactly one COM type must exist in the model — got " + members);
        assertEquals(members.get(0), WorkTypeCatalog.CONDITION_ASSESSMENT,
                "The COM singleton must be Condition Assessment");
        int serverCount = countToken(json, "\"COM\"");
        logStep("Server \"COM\" literal occurrences: " + serverCount);
        assertEquals(Integer.valueOf(serverCount), Integer.valueOf(1),
                "Exactly one service must carry type 'COM' (gold spec \u00a71 distribution)");
    }

    @Test(priority = 74, description = "TC_WT_CAT_074 - category 'IR' is a singleton — only Infrared Thermography")
    public void TC_WT_CAT_074_irCategorySingleton() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_074 - category 'IR' is a singleton — only Infrared Thermography");
        String json = services("TC_WT_CAT_074");
        List<WorkTypeCatalog> members = WorkTypeCatalog.ofCategory(Category.IR);
        assertEquals(Integer.valueOf(members.size()), Integer.valueOf(1),
                "Exactly one IR type must exist in the model — got " + members);
        assertEquals(members.get(0), WorkTypeCatalog.INFRARED_THERMOGRAPHY,
                "The IR singleton must be Infrared Thermography");
        int serverCount = countToken(json, "\"IR\"");
        logStep("Server \"IR\" literal occurrences: " + serverCount);
        assertEquals(Integer.valueOf(serverCount), Integer.valueOf(1),
                "Exactly one service must carry type 'IR' (gold spec \u00a71 distribution)");
    }

    @Test(priority = 75, description = "TC_WT_CAT_075 - category 'Schedule' is a singleton — only Panel Schedule Updates")
    public void TC_WT_CAT_075_scheduleCategorySingleton() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_075 - category 'Schedule' is a singleton — only Panel Schedule Updates");
        String json = services("TC_WT_CAT_075");
        List<WorkTypeCatalog> members = WorkTypeCatalog.ofCategory(Category.SCHEDULE);
        assertEquals(Integer.valueOf(members.size()), Integer.valueOf(1),
                "Exactly one SCHEDULE type must exist in the model — got " + members);
        assertEquals(members.get(0), WorkTypeCatalog.PANEL_SCHEDULE_UPDATES,
                "The SCHEDULE singleton must be Panel Schedule Updates");
        int serverCount = countToken(json, "\"Schedule\"");
        logStep("Server \"Schedule\" literal occurrences: " + serverCount);
        assertEquals(Integer.valueOf(serverCount), Integer.valueOf(1),
                "Exactly one service must carry type 'Schedule' (gold spec \u00a71 distribution)");
    }

    @Test(priority = 76, description = "TC_WT_CAT_076 - exactly 6 de-energized types and the member set matches the gold spec exactly")
    public void TC_WT_CAT_076_deEnergizedMemberSetExact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_076 - exactly 6 de-energized types and the member set matches the gold spec exactly");
        String json = services("TC_WT_CAT_076");
        Set<WorkTypeCatalog> expected = new HashSet<>();
        expected.add(WorkTypeCatalog.CLEANING);
        expected.add(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE);
        expected.add(WorkTypeCatalog.DE_ENERGIZED_VISUAL);
        expected.add(WorkTypeCatalog.INSULATION_RESISTANCE);
        expected.add(WorkTypeCatalog.NETA_TESTING);
        expected.add(WorkTypeCatalog.SHUTDOWN_COMPOSITE);
        int serverTrue = 0;
        for (WorkTypeCatalog wt : WorkTypeCatalog.serviceBacked()) {
            Boolean server = objectBooleanField(json, "key", wt.key(), "de_energized");
            assertTrue(server != null, "de_energized must be present on '" + wt.key() + "'");
            assertEquals(server, Boolean.valueOf(expected.contains(wt)),
                    "de_energized membership for '" + wt.key() + "' must match the gold-spec set exactly");
            assertEquals(Boolean.valueOf(wt.deEnergized()), Boolean.valueOf(expected.contains(wt)),
                    "Enum deEnergized flag for " + wt + " must match the gold-spec set");
            if (server) serverTrue++;
        }
        logStep("Server de_energized:true count = " + serverTrue);
        assertEquals(Integer.valueOf(serverTrue), Integer.valueOf(WorkTypeCatalog.DE_ENERGIZED_COUNT),
                "Exactly " + WorkTypeCatalog.DE_ENERGIZED_COUNT + " services must be de-energized");
    }

    @Test(priority = 77, description = "TC_WT_CAT_077 - every service exposes procedure_count as an unquoted number (value itself never pinned — async/class-conditional)")
    public void TC_WT_CAT_077_procedureCountPresentAndNumericForAll() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_CAT_077 - every service exposes procedure_count as an unquoted number (value itself never pinned — async/class-conditional)");
        String json = services("TC_WT_CAT_077");
        for (WorkTypeCatalog wt : WorkTypeCatalog.serviceBacked()) {
            String obj = objectSlice(json, "key", wt.key());
            assertTrue(obj != null, "Service object must exist for key '" + wt.key() + "'");
            assertTrue(hasNumericField(obj, "procedure_count"),
                    "'procedure_count' must be present as an unquoted number on '" + wt.key()
                            + "' — object: " + trunc(obj, 200));
        }
        logStep("procedure_count numeric on all 13 services (values intentionally NOT pinned — gold spec forbids count asserts)");
    }

    // ================== QA-WT fixture family (gold spec \u00a73) ==================

    @Test(priority = 78, description = "TC_WT_FIX_001 - UI bootstrap: land on Work Orders, self-provision the QA-WT family on the landed site (the ONLY UI-touching test in this class)")
    public void TC_WT_FIX_001_uiBootstrapProvisionsFixtureFamilyOnLandedSite() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_001 - UI bootstrap: land on Work Orders, self-provision the QA-WT family on the landed site (the ONLY UI-touching test in this class)");
        requireApi("TC_WT_FIX_001");
        openWorkOrdersScreenWT();
        verifyAppAlive("Work Orders list after work-type fixture provisioning");
        verifyNotBlank("Work Orders list");
        verifyNoErrorAlert();
        assertTrue(ensureFixturesOnLandedSite(),
                "Fixture family must be ensured on the landed site (backend reachable, site name readable, sld id resolvable)");
        logStep("Landed site: '" + landedSiteName() + "' (sld " + landedSldId() + ")");
        assertTrue(landedSiteName() != null && !landedSiteName().isEmpty(),
                "Landed site name must be readable from the dashboard (never assume first-site ordering)");
        assertTrue(landedSldId() != null, "Landed site must resolve to an sld id via the backend");
        TestDataApi a = requireApi("TC_WT_FIX_001");
        String generalId = findWoId("TC_WT_FIX_001", a, WorkTypeCatalog.GENERAL.fixtureName());
        assertTrue(generalId != null, "Spot check: '" + WorkTypeCatalog.GENERAL.fixtureName()
                + "' must exist after the ensure pass");
        logStepWithScreenshot("TC_WT_FIX_001 verified — QA-WT family present, landed on '" + landedSiteName() + "'");
    }

    @Test(priority = 79, description = "TC_WT_FIX_002 - QA-WT00 General exists AND carries work_type_id = null (the WO exists while its type is unset)")
    public void TC_WT_FIX_002_qaWt00General_existsWithNullWorkType() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_002 - QA-WT00 General exists AND carries work_type_id = null (the WO exists while its type is unset)");
        TestDataApi a = requireApi("TC_WT_FIX_002");
        String name = WorkTypeCatalog.GENERAL.fixtureName();
        String woId = findWoId("TC_WT_FIX_002", a, name);
        logStep("Fixture '" + name + "' -> id " + woId);
        assertTrue(woId != null, "Durable fixture '" + name + "' must exist (gold spec \u00a73 — do not delete)");
        String typeId = woTypeId("TC_WT_FIX_002", a, name);
        logStep("work_type_id = " + typeId);
        assertTrue(typeId == null,
                "'" + name + "' must carry work_type_id = null — General is the UI-only 14th option; got '" + typeId + "'");
    }

    @Test(priority = 80, description = "TC_WT_FIX_003 - QA-WT01 fixture exists AND its work_type_id equals the pinned 'arc-flash-study' service id")
    public void TC_WT_FIX_003_qaWt01_afDataCollection_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_003 - QA-WT01 fixture exists AND its work_type_id equals the pinned 'arc-flash-study' service id");
        checkFixtureBoundToType("TC_WT_FIX_003", WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION);
    }

    @Test(priority = 81, description = "TC_WT_FIX_004 - QA-WT02 fixture exists AND its work_type_id equals the pinned 'arc-flash-label-placement' service id")
    public void TC_WT_FIX_004_qaWt02_afLabelPlacement_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_004 - QA-WT02 fixture exists AND its work_type_id equals the pinned 'arc-flash-label-placement' service id");
        checkFixtureBoundToType("TC_WT_FIX_004", WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT);
    }

    @Test(priority = 82, description = "TC_WT_FIX_005 - QA-WT03 fixture exists AND its work_type_id equals the pinned 'cleaning' service id")
    public void TC_WT_FIX_005_qaWt03_cleaning_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_005 - QA-WT03 fixture exists AND its work_type_id equals the pinned 'cleaning' service id");
        checkFixtureBoundToType("TC_WT_FIX_005", WorkTypeCatalog.CLEANING);
    }

    @Test(priority = 83, description = "TC_WT_FIX_006 - QA-WT04 fixture exists AND its work_type_id equals the pinned 'clean-tighten-torque' service id")
    public void TC_WT_FIX_006_qaWt04_cleanTightenTorque_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_006 - QA-WT04 fixture exists AND its work_type_id equals the pinned 'clean-tighten-torque' service id");
        checkFixtureBoundToType("TC_WT_FIX_006", WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE);
    }

    @Test(priority = 84, description = "TC_WT_FIX_007 - QA-WT05 fixture exists AND its work_type_id equals the pinned 'condition-assessment' service id")
    public void TC_WT_FIX_007_qaWt05_conditionAssessment_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_007 - QA-WT05 fixture exists AND its work_type_id equals the pinned 'condition-assessment' service id");
        checkFixtureBoundToType("TC_WT_FIX_007", WorkTypeCatalog.CONDITION_ASSESSMENT);
    }

    @Test(priority = 85, description = "TC_WT_FIX_008 - QA-WT06 fixture exists AND its work_type_id equals the pinned 'de-energized-visual-inspection' service id")
    public void TC_WT_FIX_008_qaWt06_deEnergizedVisual_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_008 - QA-WT06 fixture exists AND its work_type_id equals the pinned 'de-energized-visual-inspection' service id");
        checkFixtureBoundToType("TC_WT_FIX_008", WorkTypeCatalog.DE_ENERGIZED_VISUAL);
    }

    @Test(priority = 86, description = "TC_WT_FIX_009 - QA-WT07 fixture exists AND its work_type_id equals the pinned 'dga-fluid-sample-analysis' service id")
    public void TC_WT_FIX_009_qaWt07_dgaFluidSample_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_009 - QA-WT07 fixture exists AND its work_type_id equals the pinned 'dga-fluid-sample-analysis' service id");
        checkFixtureBoundToType("TC_WT_FIX_009", WorkTypeCatalog.DGA_FLUID_SAMPLE);
    }

    @Test(priority = 87, description = "TC_WT_FIX_010 - QA-WT08 fixture exists AND its work_type_id equals the pinned 'infrared-thermography' service id")
    public void TC_WT_FIX_010_qaWt08_infraredThermography_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_010 - QA-WT08 fixture exists AND its work_type_id equals the pinned 'infrared-thermography' service id");
        checkFixtureBoundToType("TC_WT_FIX_010", WorkTypeCatalog.INFRARED_THERMOGRAPHY);
    }

    @Test(priority = 88, description = "TC_WT_FIX_011 - QA-WT09 fixture exists AND its work_type_id equals the pinned 'insulation-resistance-testing' service id")
    public void TC_WT_FIX_011_qaWt09_insulationResistance_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_011 - QA-WT09 fixture exists AND its work_type_id equals the pinned 'insulation-resistance-testing' service id");
        checkFixtureBoundToType("TC_WT_FIX_011", WorkTypeCatalog.INSULATION_RESISTANCE);
    }

    @Test(priority = 89, description = "TC_WT_FIX_012 - QA-WT10 fixture exists AND its work_type_id equals the pinned 'de-energized-testing' service id")
    public void TC_WT_FIX_012_qaWt10_netaTesting_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_012 - QA-WT10 fixture exists AND its work_type_id equals the pinned 'de-energized-testing' service id");
        checkFixtureBoundToType("TC_WT_FIX_012", WorkTypeCatalog.NETA_TESTING);
    }

    @Test(priority = 90, description = "TC_WT_FIX_013 - QA-WT11 fixture exists AND its work_type_id equals the pinned 'panel-schedule-updates' service id")
    public void TC_WT_FIX_013_qaWt11_panelScheduleUpdates_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_013 - QA-WT11 fixture exists AND its work_type_id equals the pinned 'panel-schedule-updates' service id");
        checkFixtureBoundToType("TC_WT_FIX_013", WorkTypeCatalog.PANEL_SCHEDULE_UPDATES);
    }

    @Test(priority = 91, description = "TC_WT_FIX_014 - QA-WT12 fixture exists AND its work_type_id equals the pinned 'composite-shutdown-emp' service id")
    public void TC_WT_FIX_014_qaWt12_shutdownComposite_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_014 - QA-WT12 fixture exists AND its work_type_id equals the pinned 'composite-shutdown-emp' service id");
        checkFixtureBoundToType("TC_WT_FIX_014", WorkTypeCatalog.SHUTDOWN_COMPOSITE);
    }

    @Test(priority = 92, description = "TC_WT_FIX_015 - QA-WT13 fixture exists AND its work_type_id equals the pinned 'ups-maintenance' service id")
    public void TC_WT_FIX_015_qaWt13_upsMaintenance_boundToServiceId() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_015 - QA-WT13 fixture exists AND its work_type_id equals the pinned 'ups-maintenance' service id");
        checkFixtureBoundToType("TC_WT_FIX_015", WorkTypeCatalog.UPS_MAINTENANCE);
    }

    @Test(priority = 93, description = "TC_WT_FIX_016 - all 14 QA-WT fixtures are active:true (durable, never deactivated)")
    public void TC_WT_FIX_016_allFourteenFixturesActive() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_016 - all 14 QA-WT fixtures are active:true (durable, never deactivated)");
        TestDataApi a = requireApi("TC_WT_FIX_016");
        for (WorkTypeCatalog wt : WorkTypeCatalog.values()) {
            String name = wt.fixtureName();
            String json = woList("TC_WT_FIX_016", a, name);
            Boolean active = objectBooleanField(json, "name", name, "active");
            assertTrue(active != null, "'active' boolean must be present on fixture row '" + name + "'");
            assertTrue(active, "Fixture '" + name + "' must be active:true — a deactivated fixture silently breaks Classes 2-5");
        }
        logStep("All 14 fixtures active:true");
    }

    @Test(priority = 94, description = "TC_WT_FIX_017 - the 14 fixture names are pairwise distinct and every name appears the same number of times company-wide (family exists on >1 site; symmetry catches per-fixture duplication/deletion)")
    public void TC_WT_FIX_017_fixtureNamesDistinctAndFamilyCardinalitySymmetric() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_017 - the 14 fixture names are pairwise distinct and every name appears the same number of times company-wide (family exists on >1 site; symmetry catches per-fixture duplication/deletion)");
        TestDataApi a = requireApi("TC_WT_FIX_017");
        String json = woList("TC_WT_FIX_017", a, "QA-WT");
        Set<String> names = new LinkedHashSet<>();
        int expectedCount = -1;
        for (WorkTypeCatalog wt : WorkTypeCatalog.values()) {
            String name = wt.fixtureName();
            assertTrue(names.add(name), "Fixture names must be pairwise distinct — duplicate '" + name + "'");
            int c = countToken(json, "\"" + name + "\"");
            logStep("'" + name + "' occurrences company-wide: " + c);
            assertTrue(c >= 1, "Fixture '" + name + "' must appear in the company WO search for 'QA-WT'");
            if (expectedCount < 0) {
                expectedCount = c;
            } else {
                assertEquals(Integer.valueOf(c), Integer.valueOf(expectedCount),
                        "Family cardinality must be symmetric — '" + name + "' appears " + c
                                + "x but earlier fixtures appear " + expectedCount
                                + "x (a fixture was duplicated or deleted on one site)");
            }
        }
        assertEquals(Integer.valueOf(names.size()), Integer.valueOf(14),
                "Exactly 14 distinct fixture names (QA-WT00..13) expected");
    }

    @Test(priority = 95, description = "TC_WT_FIX_018 - QA-WT00's raw list object serializes work_type_id as an unquoted JSON null (field present, not omitted)")
    public void TC_WT_FIX_018_qaWt00RawJsonSerializesWorkTypeIdAsNullLiteral() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_018 - QA-WT00's raw list object serializes work_type_id as an unquoted JSON null (field present, not omitted)");
        TestDataApi a = requireApi("TC_WT_FIX_018");
        String name = WorkTypeCatalog.GENERAL.fixtureName();
        String json = woList("TC_WT_FIX_018", a, name);
        String slice = objectSlice(json, "name", name);
        assertTrue(slice != null, "A brace-balanced list object must exist for '" + name + "'");
        logStep("WT00 object: " + trunc(slice, 300));
        // OR of the two spacing serializations IS the contract (compact vs single-space JSON).
        assertTrue(slice.contains("\"work_type_id\": null") || slice.contains("\"work_type_id\":null"),
                "WT00 must serialize work_type_id as unquoted JSON null (gold spec \u00a72: field present, null on untyped WOs) — object: "
                        + trunc(slice, 300));
        assertTrue(TestDataApi.extract(slice, "work_type_id") == null,
                "No quoted string work_type_id value may exist on WT00 (null is a literal, not a string)");
    }

    @Test(priority = 96, description = "TC_WT_FIX_019 - fixtureName() strips '/', ',', '(' , ')' exactly as the gold-spec family names require (NS-predicate matching contract)")
    public void TC_WT_FIX_019_fixtureNamesStripNsPredicateHostilePunctuation() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_019 - fixtureName() strips '/', ',', '(' , ')' exactly as the gold-spec family names require (NS-predicate matching contract)");
        assertEquals(WorkTypeCatalog.GENERAL.fixtureName(), "QA-WT00 General",
                "General fixture name must match gold spec \u00a73");
        assertEquals(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE.fixtureName(), "QA-WT04 Clean Tighten Torque",
                "Commas must be stripped from 'Clean, Tighten, Torque'");
        assertEquals(WorkTypeCatalog.DGA_FLUID_SAMPLE.fixtureName(), "QA-WT07 DGA Fluid Sample Analysis",
                "Slash must be stripped from 'DGA / Fluid Sample Analysis'");
        assertEquals(WorkTypeCatalog.SHUTDOWN_COMPOSITE.fixtureName(), "QA-WT12 Shutdown Composite",
                "Parentheses must be stripped from 'Shutdown (Composite)'");
        for (WorkTypeCatalog wt : WorkTypeCatalog.values()) {
            String n = wt.fixtureName();
            assertFalse(n.contains("/") || n.contains(",") || n.contains("(") || n.contains(")"),
                    "NS-predicate-hostile punctuation must be stripped from fixture name: '" + n + "'");
            assertTrue(n.startsWith(String.format("QA-WT%02d ", wt.fixtureNumber())),
                    "Fixture name must start with its zero-padded prefix: '" + n + "'");
        }
        logStep("All 14 fixture names punctuation-clean and correctly prefixed");
    }

    @Test(priority = 97, description = "TC_WT_FIX_020 - fixtureNumber() maps General->0 and the 13 services to 1..13 in catalog order, all distinct")
    public void TC_WT_FIX_020_fixtureNumbersMapZeroThroughThirteen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_020 - fixtureNumber() maps General->0 and the 13 services to 1..13 in catalog order, all distinct");
        assertEquals(Integer.valueOf(WorkTypeCatalog.GENERAL.fixtureNumber()), Integer.valueOf(0),
                "General must be fixture 0 (QA-WT00)");
        assertEquals(Integer.valueOf(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION.fixtureNumber()), Integer.valueOf(1),
                "Arc Flash Data Collection must be fixture 1 (QA-WT01)");
        assertEquals(Integer.valueOf(WorkTypeCatalog.UPS_MAINTENANCE.fixtureNumber()), Integer.valueOf(13),
                "UPS Maintenance must be fixture 13 (QA-WT13)");
        Set<Integer> nums = new HashSet<>();
        for (WorkTypeCatalog wt : WorkTypeCatalog.values()) {
            int n = wt.fixtureNumber();
            assertTrue(n >= 0 && n <= 13, "Fixture number must be within 0..13 for " + wt + ", got " + n);
            assertTrue(nums.add(n), "Fixture numbers must be pairwise distinct — duplicate " + n + " for " + wt);
            if (wt.isServiceBacked()) {
                assertTrue(n >= 1, "Service-backed type " + wt + " must not claim fixture 0");
            }
        }
        assertEquals(Integer.valueOf(nums.size()), Integer.valueOf(14), "All 14 fixture numbers must be used");
        logStep("Fixture numbers 0..13 all assigned exactly once");
    }

    @Test(priority = 98, description = "TC_WT_FIX_021 - every typed fixture's work_type_id resolves in the services catalog to the exact display name (fixture->catalog cross-link)")
    public void TC_WT_FIX_021_fixtureWorkTypesResolveBackToCatalogDisplayNames() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_021 - every typed fixture's work_type_id resolves in the services catalog to the exact display name (fixture->catalog cross-link)");
        TestDataApi a = requireApi("TC_WT_FIX_021");
        String servicesJson = services("TC_WT_FIX_021");
        for (WorkTypeCatalog wt : WorkTypeCatalog.serviceBacked()) {
            String typeId = woTypeId("TC_WT_FIX_021", a, wt.fixtureName());
            assertTrue(typeId != null, "Fixture '" + wt.fixtureName() + "' must carry a work_type_id");
            String svcName = TestDataApi.extractSiblingField(servicesJson, "id", typeId, "name");
            logStep(wt.fixtureName() + " -> " + typeId + " -> '" + svcName + "'");
            assertEquals(svcName, wt.displayName(),
                    "Fixture '" + wt.fixtureName() + "' work_type_id must resolve in the catalog to '"
                            + wt.displayName() + "' — the exact label iOS renders");
        }
    }

    @Test(priority = 99, description = "TC_WT_FIX_022 - the 14 fixture work orders are 14 distinct WOs (pairwise distinct ids)")
    public void TC_WT_FIX_022_fixtureWorkOrderIdsPairwiseDistinct() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FIX_022 - the 14 fixture work orders are 14 distinct WOs (pairwise distinct ids)");
        TestDataApi a = requireApi("TC_WT_FIX_022");
        Set<String> ids = new HashSet<>();
        for (WorkTypeCatalog wt : WorkTypeCatalog.values()) {
            String id = findWoId("TC_WT_FIX_022", a, wt.fixtureName());
            assertTrue(id != null, "Fixture '" + wt.fixtureName() + "' must exist");
            assertTrue(ids.add(id), "Fixture WO ids must be pairwise distinct — duplicate " + id
                    + " on '" + wt.fixtureName() + "'");
        }
        assertEquals(Integer.valueOf(ids.size()), Integer.valueOf(14),
                "14 distinct fixture WO ids expected");
        logStep("All 14 fixture WO ids distinct");
    }

    // =====================================================================
    // Shared fixture check
    // =====================================================================

    /** Fixture WO for {@code wt} exists and is bound to its exact pinned service id. */
    private void checkFixtureBoundToType(String tcId, WorkTypeCatalog wt) {
        TestDataApi a = requireApi(tcId);
        String name = wt.fixtureName();
        String woId = findWoId(tcId, a, name);
        logStep(tcId + ": fixture '" + name + "' -> id " + woId);
        assertTrue(woId != null,
                "Durable fixture WO '" + name + "' must exist (gold spec \u00a73; self-healed by ensureWorkOrderFixture)");
        String boundType = woTypeId(tcId, a, name);
        logStep(tcId + ": work_type_id = " + boundType);
        assertTrue(boundType != null,
                "Fixture '" + name + "' must carry a non-null work_type_id (only QA-WT00 General is untyped)");
        assertEquals(boundType, wt.serviceId(),
                "Fixture '" + name + "' must be bound to the pinned '" + wt.key()
                        + "' service id — a recreated fixture with the wrong type poisons Classes 2-5");
    }
}
