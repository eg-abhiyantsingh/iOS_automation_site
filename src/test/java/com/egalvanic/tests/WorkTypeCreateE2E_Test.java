package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;

import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * WorkTypeCreateE2E_Test — TC_WTC_E2E_* — v1.55 Work Type create-form dropdown,
 * END-TO-END: create → session → server parity → end → cleanup.
 *
 * Design doc: docs/worktype-create-dropdown-design-2026-08-04.md (Class 3).
 * All locators are PROBE-PINNED (PROBE_E/F/G, 2026-08-04, app v1.55):
 *   - Create form nav: Cancel / 'New Work Order' / Create; rows are Buttons
 *     ('Photo Type, FLIR-SEP', 'Priority, Medium', 'Work Type, *, General',
 *     'Equipment, None'); the Name TextField holds a default
 *     'Work Order - &lt;date&gt;' value.
 *   - Work Type row composite is 'Work Type, *, &lt;value&gt;' — value parsing MUST
 *     be the comma-safe prefix parse (wo.getCreateFormWorkTypeValue()); the
 *     last-segment parse mangles 'Clean, Tighten, Torque'.
 *   - Picker: stacked bottom sheet with its own 'Work Type' NavigationBar,
 *     14 full-width option Buttons (no scroll), 'General' FIRST then the 13
 *     display names in case-sensitive lexicographic order. A CENTER TAP
 *     COMMITS AND CLOSES (no sheet Done); swipe-down does NOT dismiss.
 *   - Post-create (PROBE_G): the app starts a session; the dashboard gains a
 *     top-right 'WO' chip whose menu carries 'End Session' + WO rows with
 *     radio selectors; 'End Session' raises the 'End Work Order Session?'
 *     alert (Cancel / End Session).
 *
 * COST DISCIPLINE: only the 14-entry create matrix (TC_WTC_E2E_001..014) and
 * the default-name create (TC_WTC_E2E_027) create work orders; every other
 * test reuses ONE shared lazily-created GENERAL fixture. Every created WO is
 * cleaned up in-test: active session ended via the dashboard chip menu, then
 * soft-deleted via TestDataApi.deleteWorkOrder (finally blocks).
 * TC_WTC_E2E_099 is the defensive last-resort cleanup for the shared fixture.
 *
 * SKIP policy: honest SKIP only for genuine environment preconditions
 * (backend unreachable via requireApi, in-app wifi toggle unavailable for the
 * offline smoke, shared-fixture creation already failed earlier). Every other
 * shape ends in a hard assert through the BaseTest wrappers.
 */
public class WorkTypeCreateE2E_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "Work Type Create Dropdown (v1.55)";

    // ── probe-pinned predicates (PROBE_E/F/G 2026-08-04) ────────────────────
    private static final String PRED_NEW_WO_NAV =
            "type == 'XCUIElementTypeNavigationBar' AND name == 'New Work Order'";
    private static final String PRED_DONE_NAV_BTN =
            "type == 'XCUIElementTypeButton' AND name == 'Done' AND visible == 1";
    private static final String PRED_CREATE_BTN =
            "type == 'XCUIElementTypeButton' AND name == 'Create' AND visible == 1";
    private static final String PRED_CANCEL_BTN =
            "type == 'XCUIElementTypeButton' AND name == 'Cancel' AND visible == 1";
    private static final String PRED_ALERT_END_BTN =
            "type == 'XCUIElementTypeButton' AND name == 'End Session' AND visible == 1";
    private static final String PRED_MENU_END_ROW =
            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
            + "OR type == 'XCUIElementTypeOther') AND visible == 1 AND name == 'End Session'";
    private static final String PRED_ACTIVE_BANNER =
            "type == 'XCUIElementTypeStaticText' AND name == 'Active Work Order' AND visible == 1";

    // ── shared lazily-created fixture (GENERAL) — see cost discipline ───────
    private static volatile String sharedName;        // set BEFORE Create is tapped (cleanup-traceable)
    private static volatile String sharedId;          // server id, resolved post-create
    private static volatile boolean sharedReady = false;
    private static volatile boolean sharedAttempted = false;

    // ═════════════════════════ low-level helpers ═════════════════════════

    private static String esc(String s) {
        return s == null ? "" : s.replace("'", "\\'");
    }

    /** Instant existence probe (implicit wait zeroed, always restored). */
    private boolean existsNowT(String nsPredicate) {
        IOSDriver d = DriverManager.getDriver();
        try {
            d.manage().timeouts().implicitlyWait(Duration.ZERO);
            return !d.findElements(AppiumBy.iOSNsPredicateString(nsPredicate)).isEmpty();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                d.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
            } catch (Exception ignored) { }
        }
    }

    /** Coordinate-press the first match of a type-bound predicate (modeled on cancelCreateForm). */
    private boolean tapFirstMatch(String nsPredicate, String label) {
        try {
            IOSDriver d = DriverManager.getDriver();
            WebElement el = d.findElement(AppiumBy.iOSNsPredicateString(nsPredicate));
            org.openqa.selenium.Rectangle r = el.getRect();
            d.executeScript("mobile: tap", Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
            shortWait();
            return true;
        } catch (Exception e) {
            logStep("tapFirstMatch('" + label + "') failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tapButtonByName(String name) {
        return tapFirstMatch(
                "type == 'XCUIElementTypeButton' AND name == '" + esc(name) + "' AND visible == 1",
                name);
    }

    /** True while the Site-home 'Active Work Order' banner is on screen (PROBE_G). */
    private boolean isActiveWoBannerPresent() {
        return existsNowT(PRED_ACTIVE_BANNER);
    }

    // ── create-form Name TextField (multi-strategy, probe-pinned default) ───

    /** Single quick pass over the Name-TextField strategies (500ms probes). */
    private WebElement findCreateFormNameField() {
        IOSDriver d = DriverManager.getDriver();
        String[] strategies = {
            // S1: the probe-pinned default 'Work Order - <date>' value
            "type == 'XCUIElementTypeTextField' AND visible == 1 AND value BEGINSWITH 'Work Order'",
            // S2: any visible TextField on the create form (the Name field is the only one)
            "type == 'XCUIElementTypeTextField' AND visible == 1",
            // S3: visible-attr quirk fallback (SwiftUI fields can report visible==0)
            "type == 'XCUIElementTypeTextField'"
        };
        try {
            d.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
            for (String s : strategies) {
                try {
                    List<WebElement> els = d.findElements(AppiumBy.iOSNsPredicateString(s));
                    if (!els.isEmpty()) return els.get(0);
                } catch (Exception ignored) { }
            }
            return null;
        } finally {
            try {
                d.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
            } catch (Exception ignored) { }
        }
    }

    /** Bounded wait (5s) for the Name TextField, then a fresh reference. */
    private WebElement awaitCreateFormNameField() {
        waitForCondition(() -> findCreateFormNameField() != null, 5, "create-form Name TextField");
        return findCreateFormNameField();
    }

    /** Current Name-TextField value ("" when unreadable). */
    private String readCreateFormNameValue() {
        WebElement f = awaitCreateFormNameField();
        if (f == null) return "";
        try {
            String v = f.getAttribute("value");
            return v == null || "null".equals(v) ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    /** Clear + type a unique name into the Name TextField; keyboard dismissed; readback-verified. */
    private void typeCreateFormName(String name, String tc) {
        WebElement field = awaitCreateFormNameField();
        assertTrue(field != null,
                tc + ": create-form Name TextField must be present (holds the 'Work Order - <date>' default)");
        try {
            field.clear();
            String residue = field.getAttribute("value");
            if (residue != null && !residue.isEmpty() && !"null".equals(residue)
                    && !residue.startsWith("Work Order")) {
                field.clear(); // one retry — iOS clear() occasionally half-clears
            }
        } catch (Exception e) {
            logStep(tc + ": TextField.clear threw (" + e.getMessage() + ") — re-finding field");
            field = awaitCreateFormNameField();
            assertTrue(field != null, tc + ": Name TextField must be re-findable after clear failure");
        }
        field.sendKeys(name);
        wo.dismissKeyboard();
        shortWait();
        String typed = readCreateFormNameValue();
        logStep(tc + ": Name field now reads '" + typed + "'");
        assertEquals(typed, name,
                tc + ": Name TextField must hold exactly the typed unique name (clear+type verified)");
    }

    // ── navigation + session hygiene ─────────────────────────────────────────

    /** Tap the nav 'Create' Button (coordinate press) and require the form to dismiss. */
    private void tapCreate(String tc) {
        assertTrue(tapButtonByName("Create"),
                tc + ": 'Create' nav Button must be pressable on the create form");
        // 30s window: the 'Creating work order...' spinner can hold the form
        // for well over 10s against the QA backend (observed live 2026-08-04
        // on '(s) Wild Goose Brewery' — create was in flight, not stuck).
        assertTrue(waitForCondition(() -> !existsNowT(PRED_NEW_WO_NAV), 30,
                        "'New Work Order' form to dismiss after Create"),
                tc + ": the create form must dismiss after Create is pressed");
    }

    /** Best-effort route to home: close picker, cancel form, tap the WO-list 'Done' nav button. */
    private void goHomeBestEffort() {
        try {
            if (wo.isWorkTypePickerOpen()) {
                wo.closeWorkTypePickerNoChange();
                shortWait();
            }
        } catch (Exception ignored) { }
        try {
            if (existsNowT(PRED_NEW_WO_NAV)) {
                wo.cancelCreateForm();
                shortWait();
            }
        } catch (Exception ignored) { }
        try {
            if (existsNowT(PRED_DONE_NAV_BTN)) {
                tapButtonByName("Done");
                mediumWait();
            }
        } catch (Exception ignored) { }
    }

    /**
     * Activation proof after Create: dashboard 'WO' chip OR the Site-home
     * 'Active Work Order' banner (documented v1.55 domain OR — the chip lives
     * on the dashboard, the banner on Site home; either proves the session
     * started). Falls back to a loginAndSelectSite dashboard recovery once.
     */
    private boolean verifySessionActivated(String tc) {
        goHomeBestEffort();
        boolean seen = waitForCondition(
                () -> wo.isDashboardWoChipPresent() || isActiveWoBannerPresent(),
                6, "'WO' chip or 'Active Work Order' banner");
        if (!seen) {
            logStep(tc + ": chip/banner not visible from current screen — dashboard recovery via loginAndSelectSite");
            try { loginAndSelectSite(); } catch (Exception e) {
                logStep(tc + ": dashboard recovery threw — " + e.getMessage());
            }
            seen = waitForCondition(
                    () -> wo.isDashboardWoChipPresent() || isActiveWoBannerPresent(),
                    6, "'WO' chip or banner after dashboard recovery");
        }
        return seen;
    }

    /** End the active session via the dashboard chip menu when a chip is present (best-effort guard). */
    private void endActiveSessionIfAny(String label) {
        try {
            if (wo.isDashboardWoChipPresent()) {
                logStep(label + ": active session detected — ending via dashboard chip menu");
                boolean menu = wo.openDashboardWoMenu();
                boolean ended = menu && wo.endActiveSessionViaDashboardMenu();
                logStep(label + ": end-session -> " + (ended ? "OK" : "FAILED (menuOpened=" + menu + ")"));
            }
        } catch (Exception e) {
            logStep(label + ": endActiveSessionIfAny threw — " + e.getMessage());
        }
    }

    /** Dismiss an open chip menu without ending the session (outside tap; menus swallow it). */
    private void dismissChipMenuBestEffort() {
        try {
            if (!existsNowT(PRED_MENU_END_ROW)) return;
            IOSDriver d = DriverManager.getDriver();
            int w = 390, h = 844;
            try {
                org.openqa.selenium.Dimension dim = d.manage().window().getSize();
                w = dim.getWidth();
                h = dim.getHeight();
            } catch (Exception ignored) { }
            d.executeScript("mobile: tap", Map.of("x", w / 2, "y", h - 120));
            waitForCondition(() -> !existsNowT(PRED_MENU_END_ROW), 5, "chip menu dismissed");
        } catch (Exception e) {
            logStep("dismissChipMenuBestEffort: " + e.getMessage());
        }
    }

    // ── backend polling + cleanup ────────────────────────────────────────────

    /** Bounded server-row poll: up to {@code rounds} lookups with mediumWait between. */
    private String pollWoId(TestDataApi api, String name, int rounds) {
        for (int i = 1; i <= rounds; i++) {
            try {
                String id = api.findWorkOrderIdByNameOnSld(name, landedSldId());
                if (id != null) return id;
            } catch (Exception e) {
                logStep("pollWoId round " + i + ": " + e.getMessage());
            }
            mediumWait();
        }
        return null;
    }

    /** Exact-name row count in a listWorkOrdersJson payload (whitespace-tolerant). */
    private static int countExactNameRows(String json, String name) {
        if (json == null || name == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"name\"\\s*:\\s*\"" + java.util.regex.Pattern.quote(name) + "\"")
                .matcher(json);
        int c = 0;
        while (m.find()) c++;
        return c;
    }

    /**
     * Full cleanup for a UI-created WO (finally-block workhorse): sheet/form
     * hygiene, dashboard recovery, end active session via chip menu, then
     * soft-delete the server row (resolving the id when the caller could not).
     */
    private void cleanupCreatedWo(TestDataApi api, String name, String id, String tc) {
        try {
            goHomeBestEffort();
            if (!wo.isDashboardWoChipPresent() && !isActiveWoBannerPresent()) {
                try { loginAndSelectSite(); } catch (Exception e) {
                    logStep(tc + " cleanup: dashboard recovery failed — " + e.getMessage());
                }
            }
            endActiveSessionIfAny(tc + " cleanup");
            if (api != null && name != null) {
                String resolved = id != null ? id : pollWoId(api, name, 2);
                if (resolved != null) {
                    boolean deleted = api.deleteWorkOrder(resolved);
                    logStep(tc + " cleanup: soft-delete " + resolved + " -> "
                            + (deleted ? "accepted" : "FAILED"));
                } else {
                    logStep(tc + " cleanup: no server row resolved for '" + name + "' — nothing to delete");
                }
            }
        } catch (Exception e) {
            logStep(tc + " cleanup: unexpected — " + e.getMessage());
        }
    }

    /** Restore in-app wifi to online from wherever the offline smoke left off (finally-block). */
    private void restoreOnlineBestEffort(String tc) {
        try {
            goHomeBestEffort();
            if (siteSelectionPage.isWifiOffline()) {
                siteSelectionPage.clickWifiButton();
                mediumWait();
                siteSelectionPage.goOnline();
                mediumWait();
            }
            logStep(tc + " cleanup: wifi online restored = " + !siteSelectionPage.isWifiOffline());
        } catch (Exception e) {
            logStep(tc + " cleanup: restore-online failed — " + e.getMessage());
        }
    }

    // ═════════════════════════ core E2E runner ═════════════════════════

    /**
     * Create → session → server parity → end → cleanup, for one work type:
     *  1. requireApi, WO list, create form.
     *  2. Unique name typed (clear + sendKeys + dismissKeyboard, readback-verified).
     *  3. Picker open → select displayName (tap commits+closes) → row value parity.
     *  4. Create pressed (form must dismiss — the app starts the session).
     *  5. Activation within 10s: dashboard 'WO' chip OR 'Active Work Order'
     *     banner (documented domain OR).
     *  6. API parity: row exists on the landed SLD (bounded poll) and
     *     work_type_id equals wt.serviceId() (null for GENERAL; null-safe wrapper).
     *  7. finally: end session via chip menu + soft-delete the server row.
     */
    private void runCreateE2E(WorkTypeCatalog wt, String tc) {
        TestDataApi api = requireApi(tc);
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": 'Start New Work Order' create form must open");
        verifyAppAlive(tc + ": create form open");
        String name = "QA-WTC " + tc + " " + System.currentTimeMillis();
        String createdId = null;
        try {
            typeCreateFormName(name, tc);
            assertTrue(wo.openWorkTypePicker(),
                    tc + ": Work Type picker sheet must open from the create-form row");
            assertTrue(wo.selectWorkTypeInPicker(wt.displayName()),
                    tc + ": tapping '" + wt.displayName() + "' must commit AND close the sheet (no Done exists)");
            assertEquals(wo.getCreateFormWorkTypeValue(), wt.displayName(),
                    tc + ": Work Type row value after selection (comma-safe prefix parse)");
            logStepWithScreenshot(tc + ": form filled — name='" + name + "', type='" + wt.displayName() + "'");
            tapCreate(tc);
            verifyAppAlive(tc + ": Create pressed");
            assertTrue(verifySessionActivated(tc),
                    tc + ": Create must start a session — dashboard 'WO' chip or 'Active Work Order' "
                    + "banner within 10s (v1.55 PROBE_G contract)");
            logStepWithScreenshot(tc + ": session active (chip/banner)");
            createdId = pollWoId(api, name, 5);
            assertTrue(createdId != null,
                    tc + ": created WO '" + name + "' must appear in the backend list on the landed SLD "
                    + "within the poll budget (5 rounds)");
            String serverTypeId = api.workOrderWorkTypeId(name);
            logStep(tc + ": server work_type_id='" + serverTypeId + "' expected='" + wt.serviceId() + "'");
            assertEquals(serverTypeId, wt.serviceId(),
                    tc + ": server work_type_id parity for '" + wt.displayName()
                    + "' (null expected for General)");
            verifyNoErrorAlert();
            logStepWithScreenshot(tc + " verified: create -> session -> server parity for '"
                    + wt.displayName() + "'");
        } finally {
            cleanupCreatedWo(api, name, createdId, tc);
        }
    }

    // ═════════════════════ shared GENERAL fixture ═════════════════════

    /**
     * Lazily create the ONE shared GENERAL work order the post-create surface
     * tests reuse. Hard-fails on first attempt (real signal); later calls SKIP
     * honestly if that attempt failed. The session is deliberately left ACTIVE
     * for the chip/menu tests; TC_WTC_E2E_099 tears everything down.
     */
    private void ensureSharedCreatedWo(String tc) {
        if (sharedReady) return;
        TestDataApi api = requireApi(tc);
        skipIfPreconditionMissing(() -> !sharedAttempted,
                tc + ": shared fixture creation already failed earlier in this class — see the first "
                + "surface test's failure for the root cause");
        synchronized (WorkTypeCreateE2E_Test.class) {
            if (sharedReady) return;
            sharedAttempted = true;
            openWorkOrdersScreenWT();
            // Leak guard: end any stray session so the chip we later observe is OURS.
            goHomeBestEffort();
            endActiveSessionIfAny(tc + " (pre-shared leak guard)");
            openWorkOrdersScreenWT();
            assertTrue(wo.openCreateForm(), tc + ": create form must open for the shared fixture");
            String name = "QA-WTC SHARED " + System.currentTimeMillis();
            sharedName = name; // set BEFORE Create so TC_WTC_E2E_099 can always trace it
            typeCreateFormName(name, tc);
            assertEquals(wo.getCreateFormWorkTypeValue(), "General",
                    tc + ": fresh form must default to General for the shared fixture");
            tapCreate(tc);
            assertTrue(verifySessionActivated(tc),
                    tc + ": shared-fixture Create must start a session (chip/banner within 10s)");
            sharedId = pollWoId(api, name, 5);
            assertTrue(sharedId != null,
                    tc + ": shared fixture '" + name + "' must resolve a server id on the landed SLD");
            sharedReady = true;
            logStepWithScreenshot(tc + ": shared fixture live — '" + name + "' id=" + sharedId);
        }
    }

    /** Surface-test prelude: shared fixture live + app on the dashboard. */
    private void openDashboardWithShared(String tc) {
        ensureSharedCreatedWo(tc);
        goHomeBestEffort();
        if (!wo.isDashboardWoChipPresent() && !isActiveWoBannerPresent()) {
            loginAndSelectSite(); // idempotent dashboard recovery
        }
    }

    private static String stripActiveSuffix(String composite) {
        return composite != null && composite.endsWith(", ACTIVE")
                ? composite.substring(0, composite.length() - ", ACTIVE".length())
                : composite;
    }

    // ═════════════ Group A — CREATE matrix, one per catalog value (001-014) ═════════════

    @Test(priority = 1, description = "TC_WTC_E2E_001 - create->session->server parity: General (work_type_id null)")
    public void TC_WTC_E2E_001_createGeneral() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_001 - create->session->server parity: General (work_type_id null)");
        runCreateE2E(WorkTypeCatalog.GENERAL, "TC_WTC_E2E_001");
    }

    @Test(priority = 2, description = "TC_WTC_E2E_002 - create->session->server parity: Arc Flash Data Collection")
    public void TC_WTC_E2E_002_createArcFlashDataCollection() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_002 - create->session->server parity: Arc Flash Data Collection");
        runCreateE2E(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WTC_E2E_002");
    }

    @Test(priority = 3, description = "TC_WTC_E2E_003 - create->session->server parity: Arc Flash Label Placement")
    public void TC_WTC_E2E_003_createArcFlashLabelPlacement() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_003 - create->session->server parity: Arc Flash Label Placement");
        runCreateE2E(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WTC_E2E_003");
    }

    @Test(priority = 4, description = "TC_WTC_E2E_004 - create->session->server parity: Cleaning")
    public void TC_WTC_E2E_004_createCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_004 - create->session->server parity: Cleaning");
        runCreateE2E(WorkTypeCatalog.CLEANING, "TC_WTC_E2E_004");
    }

    @Test(priority = 5, description = "TC_WTC_E2E_005 - create->session->server parity: Clean, Tighten, Torque (comma-embedded display name)")
    public void TC_WTC_E2E_005_createCleanTightenTorque() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_005 - create->session->server parity: Clean, Tighten, Torque (comma-embedded display name)");
        runCreateE2E(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WTC_E2E_005");
    }

    @Test(priority = 6, description = "TC_WTC_E2E_006 - create->session->server parity: Condition Assessment")
    public void TC_WTC_E2E_006_createConditionAssessment() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_006 - create->session->server parity: Condition Assessment");
        runCreateE2E(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WTC_E2E_006");
    }

    @Test(priority = 7, description = "TC_WTC_E2E_007 - create->session->server parity: De-Energized Visual Inspection")
    public void TC_WTC_E2E_007_createDeEnergizedVisual() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_007 - create->session->server parity: De-Energized Visual Inspection");
        runCreateE2E(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WTC_E2E_007");
    }

    @Test(priority = 8, description = "TC_WTC_E2E_008 - create->session->server parity: DGA / Fluid Sample Analysis (slash display name)")
    public void TC_WTC_E2E_008_createDgaFluidSample() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_008 - create->session->server parity: DGA / Fluid Sample Analysis (slash display name)");
        runCreateE2E(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WTC_E2E_008");
    }

    @Test(priority = 9, description = "TC_WTC_E2E_009 - create->session->server parity: Infrared Thermography")
    public void TC_WTC_E2E_009_createInfraredThermography() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_009 - create->session->server parity: Infrared Thermography");
        runCreateE2E(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WTC_E2E_009");
    }

    @Test(priority = 10, description = "TC_WTC_E2E_010 - create->session->server parity: Insulation Resistance Testing")
    public void TC_WTC_E2E_010_createInsulationResistance() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_010 - create->session->server parity: Insulation Resistance Testing");
        runCreateE2E(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WTC_E2E_010");
    }

    @Test(priority = 11, description = "TC_WTC_E2E_011 - create->session->server parity: NETA Testing (key de-energized-testing)")
    public void TC_WTC_E2E_011_createNetaTesting() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_011 - create->session->server parity: NETA Testing (key de-energized-testing)");
        runCreateE2E(WorkTypeCatalog.NETA_TESTING, "TC_WTC_E2E_011");
    }

    @Test(priority = 12, description = "TC_WTC_E2E_012 - create->session->server parity: Panel Schedule Updates")
    public void TC_WTC_E2E_012_createPanelScheduleUpdates() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_012 - create->session->server parity: Panel Schedule Updates");
        runCreateE2E(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WTC_E2E_012");
    }

    @Test(priority = 13, description = "TC_WTC_E2E_013 - create->session->server parity: Shutdown (Composite) (parenthesised display name)")
    public void TC_WTC_E2E_013_createShutdownComposite() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_013 - create->session->server parity: Shutdown (Composite) (parenthesised display name)");
        runCreateE2E(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WTC_E2E_013");
    }

    @Test(priority = 14, description = "TC_WTC_E2E_014 - create->session->server parity: UPS Maintenance")
    public void TC_WTC_E2E_014_createUpsMaintenance() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_014 - create->session->server parity: UPS Maintenance");
        runCreateE2E(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WTC_E2E_014");
    }

    // ═════════ Group B — POST-CREATE surface, shared GENERAL fixture (015-025) ═════════

    @Test(priority = 15, description = "TC_WTC_E2E_015 - dashboard shows the 'WO' chip while the created session is active")
    public void TC_WTC_E2E_015_dashboardChipPresent() {
        String tc = "TC_WTC_E2E_015";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - dashboard shows the 'WO' chip while the created session is active");
        openDashboardWithShared(tc);
        assertTrue(waitForCondition(() -> wo.isDashboardWoChipPresent(), 8, "dashboard 'WO' chip"),
                tc + ": the dashboard must show the top-right 'WO' chip while the shared session is active");
        verifyAppAlive(tc + ": chip visible");
        logStepWithScreenshot(tc + " verified: active-session 'WO' chip present on the dashboard");
    }

    @Test(priority = 16, description = "TC_WTC_E2E_016 - tapping the 'WO' chip opens the session menu")
    public void TC_WTC_E2E_016_chipMenuOpens() {
        String tc = "TC_WTC_E2E_016";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - tapping the 'WO' chip opens the session menu");
        openDashboardWithShared(tc);
        assertTrue(wo.isDashboardWoChipPresent(), tc + ": chip must be present before opening its menu");
        assertTrue(wo.openDashboardWoMenu(),
                tc + ": tapping the 'WO' chip must open the session menu (primitive verifies the "
                + "'End Session' row appearing)");
        logStepWithScreenshot(tc + " verified: chip menu open");
        dismissChipMenuBestEffort();
        verifyAppAlive(tc + ": menu dismissed");
    }

    @Test(priority = 17, description = "TC_WTC_E2E_017 - chip menu carries an 'End Session' entry (direct census)")
    public void TC_WTC_E2E_017_menuCarriesEndSession() {
        String tc = "TC_WTC_E2E_017";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - chip menu carries an 'End Session' entry (direct census)");
        openDashboardWithShared(tc);
        assertTrue(wo.isDashboardWoChipPresent(), tc + ": chip must be present before opening its menu");
        assertTrue(wo.openDashboardWoMenu(), tc + ": chip menu must open");
        // Independent evidence beyond the primitive's own wait: type-bounded census.
        assertTrue(existsNowT(PRED_MENU_END_ROW),
                tc + ": the open chip menu must carry an 'End Session' row (type-bounded census)");
        logStepWithScreenshot(tc + " verified: 'End Session' entry present in the chip menu");
        dismissChipMenuBestEffort();
    }

    @Test(priority = 18, description = "TC_WTC_E2E_018 - chip menu lists the active work order by name (session switcher)")
    public void TC_WTC_E2E_018_menuListsActiveWoName() {
        String tc = "TC_WTC_E2E_018";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - chip menu lists the active work order by name (session switcher)");
        openDashboardWithShared(tc);
        assertTrue(wo.isDashboardWoChipPresent(), tc + ": chip must be present before opening its menu");
        assertTrue(wo.openDashboardWoMenu(), tc + ": chip menu must open");
        String pred = "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                + "OR type == 'XCUIElementTypeOther') AND visible == 1 AND name BEGINSWITH '"
                + esc(sharedName) + "'";
        assertTrue(existsNowT(pred),
                tc + ": the chip menu (WO rows with radio selectors, PROBE_G) must list the active WO '"
                + sharedName + "'");
        logStepWithScreenshot(tc + " verified: active WO listed in the chip menu");
        dismissChipMenuBestEffort();
    }

    @Test(priority = 19, description = "TC_WTC_E2E_019 - 'End Work Order Session?' alert shows Cancel + End Session; Cancel is a no-op (chip survives)")
    public void TC_WTC_E2E_019_endSessionAlertCancelPath() {
        String tc = "TC_WTC_E2E_019";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - 'End Work Order Session?' alert shows Cancel + End Session; Cancel is a no-op (chip survives)");
        openDashboardWithShared(tc);
        assertTrue(wo.isDashboardWoChipPresent(), tc + ": chip must be present before the End-Session dance");
        assertTrue(wo.openDashboardWoMenu(), tc + ": chip menu must open");
        IOSDriver d = DriverManager.getDriver();
        boolean paused = false;
        try {
            // Manual alerts for the dance — autoAcceptAlerts otherwise races the
            // confirm away (autoacceptalerts-alert-race; primitive does the same).
            try { d.setSetting("defaultAlertAction", ""); paused = true; } catch (Exception e) {
                logStep(tc + ": could not pause auto-alerts — " + e.getMessage());
            }
            assertTrue(tapFirstMatch(PRED_MENU_END_ROW, "End Session menu row"),
                    tc + ": the 'End Session' menu row must be tappable");
            assertTrue(waitForCondition(() -> existsNowT(PRED_CANCEL_BTN), 8,
                            "'End Work Order Session?' alert (Cancel button)"),
                    tc + ": the 'End Work Order Session?' alert must appear with a Cancel Button");
            assertTrue(existsNowT(PRED_ALERT_END_BTN),
                    tc + ": the alert must also carry an 'End Session' confirm Button");
            logStepWithScreenshot(tc + ": alert up — Cancel + End Session Buttons present");
            assertTrue(tapButtonByName("Cancel"), tc + ": Cancel must be tappable on the alert");
            assertTrue(waitForCondition(() -> !existsNowT(PRED_CANCEL_BTN), 6, "alert dismissed"),
                    tc + ": the alert must dismiss after Cancel");
        } finally {
            if (paused) {
                try { d.setSetting("defaultAlertAction", "accept"); } catch (Exception ignored) { }
            }
        }
        mediumWait();
        dismissChipMenuBestEffort();
        assertTrue(waitForCondition(() -> wo.isDashboardWoChipPresent(), 8, "'WO' chip after Cancel"),
                tc + ": Cancel must be a NO-OP — the session stays active and the chip stays present");
        logStepWithScreenshot(tc + " verified: Cancel path leaves the session running");
    }

    @Test(priority = 20, description = "TC_WTC_E2E_020 - created work order row is findable in the Work Orders list")
    public void TC_WTC_E2E_020_sharedRowFindable() {
        String tc = "TC_WTC_E2E_020";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - created work order row is findable in the Work Orders list");
        ensureSharedCreatedWo(tc);
        openWorkOrdersScreenWT();
        assertTrue(wo.scrollWorkOrderListTo(sharedName),
                tc + ": the UI-created WO '" + sharedName + "' must be reachable by bounded scroll "
                + "in the Work Orders list");
        verifyAppAlive(tc + ": row on screen");
        logStepWithScreenshot(tc + " verified: created row present in the list");
    }

    @Test(priority = 21, description = "TC_WTC_E2E_021 - created row composite BEGINSWITH the exact untruncated name")
    public void TC_WTC_E2E_021_sharedRowUntruncated() {
        String tc = "TC_WTC_E2E_021";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - created row composite BEGINSWITH the exact untruncated name");
        ensureSharedCreatedWo(tc);
        openWorkOrdersScreenWT();
        assertTrue(wo.scrollWorkOrderListTo(sharedName),
                tc + ": created row must be on screen before reading its composite");
        String composite = wo.getWorkOrderRowComposite(sharedName);
        logStep(tc + " composite: '" + composite + "'");
        assertTrue(composite != null && composite.startsWith(sharedName),
                tc + ": row composite must BEGIN WITH the exact untruncated name '" + sharedName
                + "' — got '" + composite + "'");
        logStepWithScreenshot(tc + " verified: name untruncated in the list row");
    }

    @Test(priority = 22, description = "TC_WTC_E2E_022 - created row carries the default Medium priority chip (composite ENDSWITH ', Medium')")
    public void TC_WTC_E2E_022_sharedRowDefaultPriorityMedium() {
        String tc = "TC_WTC_E2E_022";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - created row carries the default Medium priority chip (composite ENDSWITH ', Medium')");
        ensureSharedCreatedWo(tc);
        openWorkOrdersScreenWT();
        assertTrue(wo.scrollWorkOrderListTo(sharedName),
                tc + ": created row must be on screen before reading its composite");
        String composite = stripActiveSuffix(wo.getWorkOrderRowComposite(sharedName));
        logStep(tc + " composite (ACTIVE-stripped): '" + composite + "'");
        assertTrue(composite != null && composite.endsWith(", Medium"),
                tc + ": create-form default priority is Medium (probe: 'Priority, Medium' row) — the "
                + "list row must END WITH ', Medium' (', ACTIVE' suffix tolerated); got '" + composite + "'");
        logStepWithScreenshot(tc + " verified: default Medium priority chip on the created row");
    }

    @Test(priority = 23, description = "TC_WTC_E2E_023 - backend holds the created WO on the landed SLD (id resolvable, lookup-idempotent)")
    public void TC_WTC_E2E_023_apiRowOnLandedSld() {
        String tc = "TC_WTC_E2E_023";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - backend holds the created WO on the landed SLD (id resolvable, lookup-idempotent)");
        ensureSharedCreatedWo(tc);
        TestDataApi api = requireApi(tc);
        String id = api.findWorkOrderIdByNameOnSld(sharedName, landedSldId());
        assertTrue(id != null,
                tc + ": '" + sharedName + "' must resolve to a server id on the landed SLD ("
                + landedSldId() + ")");
        assertEquals(id, sharedId,
                tc + ": repeat lookup must resolve the SAME id the create resolved (deterministic row)");
        logStep(tc + " verified: server id " + id + " stable across lookups");
    }

    @Test(priority = 24, description = "TC_WTC_E2E_024 - General persists work_type_id = null on the server")
    public void TC_WTC_E2E_024_apiWorkTypeIdNullForGeneral() {
        String tc = "TC_WTC_E2E_024";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - General persists work_type_id = null on the server");
        ensureSharedCreatedWo(tc);
        TestDataApi api = requireApi(tc);
        String typeId = api.workOrderWorkTypeId(sharedName);
        logStep(tc + ": server work_type_id = '" + typeId + "'");
        assertEquals(typeId, (String) null,
                tc + ": a General create must persist work_type_id = null (gold-spec UI-only 14th option)");
        logStep(tc + " verified: General -> null work_type_id parity");
    }

    @Test(priority = 25, description = "TC_WTC_E2E_025 - exactly ONE server row exists for the unique created name (no duplicate create)")
    public void TC_WTC_E2E_025_apiExactlyOneServerRow() {
        String tc = "TC_WTC_E2E_025";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - exactly ONE server row exists for the unique created name (no duplicate create)");
        ensureSharedCreatedWo(tc);
        TestDataApi api = requireApi(tc);
        int count = countExactNameRows(api.listWorkOrdersJson(sharedName), sharedName);
        logStep(tc + ": exact-name row count for '" + sharedName + "' = " + count);
        assertTrue(count == 1,
                tc + ": exactly ONE backend row must exist for the timestamp-unique name '" + sharedName
                + "' — got " + count + " (a second row means Create fired twice)");
        logStep(tc + " verified: single server row for the created WO");
    }

    // ═════════════════ Group C — DEFAULT-NAME create (026-027) ═════════════════

    @Test(priority = 26, description = "TC_WTC_E2E_026 - Name TextField holds a 'Work Order - <date>' default on a fresh form")
    public void TC_WTC_E2E_026_defaultNameFormat() {
        String tc = "TC_WTC_E2E_026";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - Name TextField holds a 'Work Order - <date>' default on a fresh form");
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": create form must open");
        try {
            String defName = readCreateFormNameValue();
            logStep(tc + ": default name reads '" + defName + "'");
            assertTrue(defName != null && !defName.isEmpty(),
                    tc + ": the Name TextField must hold a non-empty default value");
            assertTrue(defName.startsWith("Work Order"),
                    tc + ": the default name must BEGIN WITH 'Work Order' (probe-pinned "
                    + "'Work Order - <date>' default) — got '" + defName + "'");
            logStepWithScreenshot(tc + " verified: default name format");
        } finally {
            wo.cancelCreateForm();
            shortWait();
        }
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must land back on the Work Orders list");
    }

    @Test(priority = 27, description = "TC_WTC_E2E_027 - create with the untouched default name reaches the server (id resolvable)")
    public void TC_WTC_E2E_027_defaultNameCreateE2E() {
        String tc = "TC_WTC_E2E_027";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - create with the untouched default name reaches the server (id resolvable)");
        TestDataApi api = requireApi(tc);
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": create form must open");
        String readName = readCreateFormNameValue();
        assertTrue(readName != null && !readName.isEmpty(),
                tc + ": default name must be readable before Create (contract under test needs it "
                + "for the server lookup)");
        logStep(tc + ": creating with UNTOUCHED default name '" + readName + "' — NOTE: same-name older "
                + "WOs may exist (the default embeds only the date), so this test asserts id != null ONLY "
                + "and cleans up whichever id it resolves");
        String createdId = null;
        try {
            tapCreate(tc);
            assertTrue(verifySessionActivated(tc),
                    tc + ": default-name Create must start a session (chip/banner within 10s)");
            createdId = pollWoId(api, readName, 5);
            assertTrue(createdId != null,
                    tc + ": a server row named '" + readName + "' must exist after the default-name create");
            logStepWithScreenshot(tc + " verified: default-name create reached the server (id=" + createdId + ")");
        } finally {
            cleanupCreatedWo(api, readName, createdId, tc);
        }
    }

    // ═════════════════ Group D — CANCEL path (028-033) ═════════════════

    /** Fill name + select {@code wt}, Cancel, then prove the server NEVER sees the draft. */
    private void runCancelNoServerRow(WorkTypeCatalog wt, String tc) {
        TestDataApi api = requireApi(tc);
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": create form must open");
        String name = "QA-WTC " + tc + " " + System.currentTimeMillis();
        try {
            typeCreateFormName(name, tc);
            assertTrue(wo.openWorkTypePicker(), tc + ": Work Type picker must open");
            assertTrue(wo.selectWorkTypeInPicker(wt.displayName()),
                    tc + ": selecting '" + wt.displayName() + "' must commit and close the sheet");
            assertEquals(wo.getCreateFormWorkTypeValue(), wt.displayName(),
                    tc + ": row value before Cancel (comma/slash/parens-safe prefix parse)");
            wo.cancelCreateForm();
            shortWait();
            assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must land back on the Work Orders list");
            for (int round = 1; round <= 3; round++) {
                String id = api.findWorkOrderIdByNameOnSld(name, landedSldId());
                assertTrue(id == null,
                        tc + ": a CANCELLED create must never reach the server — found id '" + id
                        + "' on poll round " + round);
                mediumWait();
            }
            logStepWithScreenshot(tc + " verified: Cancel discarded the '" + wt.displayName()
                    + "' draft — no server row across 3 poll rounds");
        } finally {
            // Belt-and-braces: if a row leaked despite the asserts, resolve + delete it.
            cleanupCreatedWo(api, name, null, tc);
        }
    }

    @Test(priority = 28, description = "TC_WTC_E2E_028 - cancelled create never reaches the server (Cleaning)")
    public void TC_WTC_E2E_028_cancelNoServerRowCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_028 - cancelled create never reaches the server (Cleaning)");
        runCancelNoServerRow(WorkTypeCatalog.CLEANING, "TC_WTC_E2E_028");
    }

    @Test(priority = 29, description = "TC_WTC_E2E_029 - cancelled create never reaches the server (Clean, Tighten, Torque — comma name)")
    public void TC_WTC_E2E_029_cancelNoServerRowCommaType() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_029 - cancelled create never reaches the server (Clean, Tighten, Torque — comma name)");
        runCancelNoServerRow(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WTC_E2E_029");
    }

    @Test(priority = 30, description = "TC_WTC_E2E_030 - cancelled create never reaches the server (DGA / Fluid Sample Analysis — slash name)")
    public void TC_WTC_E2E_030_cancelNoServerRowSlashType() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_030 - cancelled create never reaches the server (DGA / Fluid Sample Analysis — slash name)");
        runCancelNoServerRow(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WTC_E2E_030");
    }

    @Test(priority = 31, description = "TC_WTC_E2E_031 - cancelled create never reaches the server (Shutdown (Composite) — parens name)")
    public void TC_WTC_E2E_031_cancelNoServerRowParensType() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_E2E_031 - cancelled create never reaches the server (Shutdown (Composite) — parens name)");
        runCancelNoServerRow(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WTC_E2E_031");
    }

    @Test(priority = 32, description = "TC_WTC_E2E_032 - cancelled create does NOT start a session (dashboard chip absent)")
    public void TC_WTC_E2E_032_cancelLeavesNoActiveSession() {
        String tc = "TC_WTC_E2E_032";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - cancelled create does NOT start a session (dashboard chip absent)");
        openWorkOrdersScreenWT();
        // Guard: end any session leaked by earlier tests so chip-absence is OUR signal.
        goHomeBestEffort();
        endActiveSessionIfAny(tc + " (pre-test session guard)");
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": create form must open");
        String name = "QA-WTC " + tc + " " + System.currentTimeMillis();
        typeCreateFormName(name, tc);
        assertTrue(wo.openWorkTypePicker(), tc + ": Work Type picker must open");
        assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.CONDITION_ASSESSMENT.displayName()),
                tc + ": selection must commit before the Cancel");
        wo.cancelCreateForm();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must land back on the Work Orders list");
        // Prove the DASHBOARD context before asserting absence (no vacuous pass).
        loginAndSelectSite();
        mediumWait();
        assertTrue(!wo.isDashboardWoChipPresent(),
                tc + ": a cancelled create must NOT start a session — the dashboard 'WO' chip must be absent");
        verifyAppAlive(tc + ": dashboard after cancel");
        logStepWithScreenshot(tc + " verified: no active-session chip after Cancel");
    }

    @Test(priority = 33, description = "TC_WTC_E2E_033 - Work Orders list stays healthy and interactive after a cancelled create")
    public void TC_WTC_E2E_033_cancelLeavesListHealthy() {
        String tc = "TC_WTC_E2E_033";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - Work Orders list stays healthy and interactive after a cancelled create");
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": create form must open");
        String name = "QA-WTC " + tc + " " + System.currentTimeMillis();
        typeCreateFormName(name, tc);
        assertTrue(wo.openWorkTypePicker(), tc + ": Work Type picker must open");
        assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.INFRARED_THERMOGRAPHY.displayName()),
                tc + ": selection must commit before the Cancel");
        wo.cancelCreateForm();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must restore the Work Orders list");
        verifyAppAlive(tc + ": list restored");
        verifyNoErrorAlert();
        // Interactivity proof: the create form must open again cleanly.
        assertTrue(wo.openCreateForm(),
                tc + ": the list must stay interactive — the create form must REOPEN after a cancelled create");
        wo.cancelCreateForm();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": second Cancel must restore the list again");
        logStepWithScreenshot(tc + " verified: list healthy and interactive after cancel");
    }

    // ═════════ Group E — reopen-after-cancel draft-discard contracts (034-035) ═════════

    @Test(priority = 34, description = "TC_WTC_E2E_034 - reopened form after Cancel defaults back to General (draft type discarded)")
    public void TC_WTC_E2E_034_reopenAfterCancelResetsTypeToGeneral() {
        String tc = "TC_WTC_E2E_034";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - reopened form after Cancel defaults back to General (draft type discarded)");
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": create form must open");
        assertTrue(wo.openWorkTypePicker(), tc + ": Work Type picker must open");
        assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.UPS_MAINTENANCE.displayName()),
                tc + ": selecting UPS Maintenance must commit");
        assertEquals(wo.getCreateFormWorkTypeValue(), WorkTypeCatalog.UPS_MAINTENANCE.displayName(),
                tc + ": row must read the selected type before Cancel");
        wo.cancelCreateForm();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must restore the list");
        assertTrue(wo.openCreateForm(), tc + ": form must reopen");
        try {
            assertEquals(wo.getCreateFormWorkTypeValue(), "General",
                    tc + ": Cancel must DISCARD the draft — a reopened form defaults back to General "
                    + "(v1.55 fresh-form default)");
            logStepWithScreenshot(tc + " verified: draft type discarded on cancel");
        } finally {
            wo.cancelCreateForm();
            shortWait();
        }
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": final Cancel must restore the list");
    }

    @Test(priority = 35, description = "TC_WTC_E2E_035 - reopened form after Cancel restores the default name (typed name discarded)")
    public void TC_WTC_E2E_035_reopenAfterCancelRestoresDefaultName() {
        String tc = "TC_WTC_E2E_035";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - reopened form after Cancel restores the default name (typed name discarded)");
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": create form must open");
        String typedName = "QA-WTC " + tc + " " + System.currentTimeMillis();
        typeCreateFormName(typedName, tc);
        wo.cancelCreateForm();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must restore the list");
        assertTrue(wo.openCreateForm(), tc + ": form must reopen");
        try {
            String reopened = readCreateFormNameValue();
            logStep(tc + ": reopened Name field reads '" + reopened + "'");
            assertTrue(reopened.startsWith("Work Order") && !reopened.equals(typedName),
                    tc + ": Cancel must discard the typed name — the reopened form must show a fresh "
                    + "'Work Order - <date>' default, not '" + typedName + "'; got '" + reopened + "'");
            logStepWithScreenshot(tc + " verified: typed name discarded on cancel");
        } finally {
            wo.cancelCreateForm();
            shortWait();
        }
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": final Cancel must restore the list");
    }

    // ═════════════════ Group F — REQUIRED gating (036-037) ═════════════════

    @Test(priority = 36, description = "TC_WTC_E2E_036 - fresh form: Work Type row present, required-marked, defaulting to General")
    public void TC_WTC_E2E_036_freshFormRequiredMarkerAndGeneralDefault() {
        String tc = "TC_WTC_E2E_036";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - fresh form: Work Type row present, required-marked, defaulting to General");
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": create form must open");
        try {
            assertTrue(wo.isCreateFormWorkTypeRowPresent(),
                    tc + ": a fresh create form must carry the 'Work Type, *, <value>' row (v1.55)");
            assertTrue(wo.workTypeRowHasRequiredMarker(),
                    tc + ": the Work Type row must carry the '*' required-marker segment");
            assertEquals(wo.getCreateFormWorkTypeValue(), "General",
                    tc + ": the fresh-form Work Type default must be 'General'");
            logStepWithScreenshot(tc + " verified: required Work Type row with General default");
        } finally {
            wo.cancelCreateForm();
            shortWait();
        }
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must restore the list");
    }

    @Test(priority = 37, description = "TC_WTC_E2E_037 - fresh form: the 'Create' nav Button EXISTS (enabled-attr semantics unprobed, not asserted)")
    public void TC_WTC_E2E_037_freshFormCreateButtonExists() {
        String tc = "TC_WTC_E2E_037";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - fresh form: the 'Create' nav Button EXISTS (enabled-attr semantics unprobed, not asserted)");
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), tc + ": create form must open");
        try {
            assertTrue(existsNowT(PRED_CREATE_BTN),
                    tc + ": the 'Create' nav Button must EXIST on a fresh form (Cancel / 'New Work Order' "
                    + "/ Create anatomy)");
            logStep(tc + ": enabled/disabled semantics of Create are deliberately NOT asserted — "
                    + "unprobed on v1.55");
            logStepWithScreenshot(tc + " verified: Create Button present");
        } finally {
            wo.cancelCreateForm();
            shortWait();
        }
        assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must restore the list");
    }

    // ═════════════════ Group G — OFFLINE smoke (038-039) ═════════════════

    @Test(priority = 38, description = "TC_WTC_E2E_038 - OFFLINE: create form renders the Work Type row with the General default")
    public void TC_WTC_E2E_038_offlineCreateFormShowsGeneralRow() {
        String tc = "TC_WTC_E2E_038";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - OFFLINE: create form renders the Work Type row with the General default");
        openWorkOrdersScreenWT();
        skipIfPreconditionMissing(() -> wo.canQueueOfflineTasks(),
                tc + ": in-app wifi toggle unavailable on this build — offline smoke needs it");
        goHomeBestEffort(); // the wifi toggle lives on the dashboard
        try {
            if (!siteSelectionPage.isWifiOffline()) {
                siteSelectionPage.clickWifiButton();
                mediumWait();
                siteSelectionPage.goOffline();
                mediumWait();
            }
            skipIfPreconditionMissing(() -> siteSelectionPage.isWifiOffline(),
                    tc + ": could not enter in-app offline mode (wifi dance no-op)");
            logStepWithScreenshot(tc + ": offline mode active");
            siteSelectionPage.clickWorkOrderCard();
            shortWait();
            assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Work Orders list must open while OFFLINE");
            assertTrue(wo.openCreateForm(), tc + ": create form must open while OFFLINE");
            assertTrue(wo.isCreateFormWorkTypeRowPresent(),
                    tc + ": the Work Type row must render on the OFFLINE create form");
            assertEquals(wo.getCreateFormWorkTypeValue(), "General",
                    tc + ": the OFFLINE fresh-form default must be 'General'");
            logStepWithScreenshot(tc + " verified: offline create form carries the Work Type row");
            wo.cancelCreateForm();
            shortWait();
            assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must restore the list while OFFLINE");
        } finally {
            restoreOnlineBestEffort(tc);
        }
        verifyAppAlive(tc + ": back online");
    }

    @Test(priority = 39, description = "TC_WTC_E2E_039 - OFFLINE: Work Type picker opens with all 14 options, General first")
    public void TC_WTC_E2E_039_offlinePickerShowsAllFourteen() {
        String tc = "TC_WTC_E2E_039";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - OFFLINE: Work Type picker opens with all 14 options, General first");
        openWorkOrdersScreenWT();
        skipIfPreconditionMissing(() -> wo.canQueueOfflineTasks(),
                tc + ": in-app wifi toggle unavailable on this build — offline smoke needs it");
        goHomeBestEffort();
        try {
            if (!siteSelectionPage.isWifiOffline()) {
                siteSelectionPage.clickWifiButton();
                mediumWait();
                siteSelectionPage.goOffline();
                mediumWait();
            }
            skipIfPreconditionMissing(() -> siteSelectionPage.isWifiOffline(),
                    tc + ": could not enter in-app offline mode (wifi dance no-op)");
            siteSelectionPage.clickWorkOrderCard();
            shortWait();
            assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Work Orders list must open while OFFLINE");
            assertTrue(wo.openCreateForm(), tc + ": create form must open while OFFLINE");
            assertTrue(wo.openWorkTypePicker(), tc + ": Work Type picker must open while OFFLINE");
            List<String> options = wo.getWorkTypePickerOptions(14);
            logStep(tc + ": OFFLINE picker options = " + options);
            assertTrue(options.size() == 14,
                    tc + ": the OFFLINE picker must render ALL 14 options (local catalog) — got "
                    + options.size() + ": " + options);
            assertEquals(options.get(0), "General",
                    tc + ": 'General' must stay the FIRST option offline");
            logStepWithScreenshot(tc + " verified: full 14-option catalog renders offline");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    tc + ": the no-op close (re-tap selected row) must work offline");
            wo.cancelCreateForm();
            shortWait();
            assertTrue(wo.waitForWorkOrdersScreen(), tc + ": Cancel must restore the list while OFFLINE");
        } finally {
            restoreOnlineBestEffort(tc);
        }
        verifyAppAlive(tc + ": back online");
    }

    // ═════════════════ FINAL — defensive shared-fixture cleanup (099) ═════════════════

    @Test(priority = 99, description = "TC_WTC_E2E_099 - cleanup: end + soft-delete the shared fixture; app healthy with no session leaked")
    public void TC_WTC_E2E_099_cleanupSharedFixture() {
        String tc = "TC_WTC_E2E_099";
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tc + " - cleanup: end + soft-delete the shared fixture; app healthy with no session leaked");
        goHomeBestEffort();
        try { loginAndSelectSite(); } catch (Exception e) {
            logStep(tc + ": dashboard recovery threw — " + e.getMessage());
        }
        endActiveSessionIfAny(tc);
        TestDataApi a = api(); // best-effort: cleanup must not SKIP just because the backend blipped
        if (sharedName != null && a != null) {
            String resolved = sharedId != null ? sharedId : pollWoId(a, sharedName, 2);
            if (resolved != null) {
                boolean deleted = a.deleteWorkOrder(resolved);
                logStep(tc + ": soft-delete of shared fixture '" + sharedName + "' (" + resolved + ") -> "
                        + (deleted ? "accepted" : "FAILED"));
                assertTrue(deleted,
                        tc + ": the shared fixture soft-delete must be accepted by the backend");
            } else {
                logStep(tc + ": no server row resolved for shared fixture '" + sharedName
                        + "' — nothing to delete (create may have failed earlier)");
            }
        } else {
            logStep(tc + ": nothing to clean (sharedName=" + sharedName + ", apiReachable=" + (a != null) + ")");
        }
        verifyAppAlive(tc + ": after cleanup");
        verifyNoErrorAlert();
        assertTrue(!wo.isDashboardWoChipPresent(),
                tc + ": no active session may remain after the suite's cleanup (dashboard chip must be absent)");
        logStepWithScreenshot(tc + " verified: shared fixture cleaned up, app healthy on the dashboard");
    }
}
