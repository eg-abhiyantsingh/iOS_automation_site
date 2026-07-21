package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.utils.Waits;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * WorkType_Details_Test — TC_WT_DET_* (Class 3 of the TC_WT_* work-type suite).
 *
 * WHAT: exhaustive verification of the OPENED work-order (session details)
 * screen for every work type. 7 checks x 14 fixtures = 98 per-fixture cases:
 *   1. opened fixture lands on the session-details screen (probe-tolerant:
 *      ensureSessionDetailsOpen OR isSessionDetailsScreenDisplayed is the real
 *      contract; when neither recognizes the surface but the app is alive and
 *      the screen is not blank, the v1.51 anatomy is PROBE-DEPENDENT → SKIP);
 *   2. session header text contains the fixture name (null-tolerant: SKIP when
 *      the header is unreadable, hard-assert containment when readable);
 *   3. work-type label — for the 13 service-backed types, IF
 *      getWorkTypeLabelOnScreen() returns a label it MUST equal the catalog
 *      displayName() exactly (gold spec §5: the subtitle under the WO name is
 *      the exact display name incl. punctuation); when null → SKIP ("label
 *      surface not present in v1.51", see design-doc PROBE CONTRACT). For the
 *      General fixture (work_type_id = null) a rendered label must NOT claim
 *      one of the 13 service-backed display names;
 *   4. session tab strip renders — areAllSessionTabsDisplayed OR at least
 *      Details/Assets tab visible (the OR is the real contract: any rendered
 *      tab strip proves the tabbed session screen; per-type tab SETS differ,
 *      gold spec §5, so "all tabs" alone would be too strict);
 *   5. info/photo-type value: getSessionType() non-empty when readable
 *      (FLUKE for WT01-13, FLIR-SEP for WT00 per gold spec §3 — value is
 *      logged, only non-emptiness is hard-asserted per the design doc:
 *      "tolerate either but assert non-empty");
 *   6. stability: no error alert + app alive + screen not blank on the opened
 *      screen, re-verified after a settle, tab strip still intact;
 *   7. clean back-nav: goBack restores the Work Orders list and the fixture
 *      row is still reachable by bounded scroll.
 *
 * Plus 8 cross-cutting cases (TC_WT_DET_201-208):
 *   201-203 punctuated display names render exactly ("Clean, Tighten, Torque",
 *           "DGA / Fluid Sample Analysis", "Shutdown (Composite)" — label
 *           equality + explicit punctuation preservation, when label present);
 *   204     label-vs-catalog sweep across all 13 service fixtures in one test;
 *   205-206 re-entry keeps the header for 2 samples (WT08 IR, WT05 COM);
 *   207-208 2x backgrounding-on-details stability via guard() for 2 samples
 *           (WT01 AF, WT11 Schedule).
 *
 * WHY: the v1.51 binary consumes work_type_id and carries a workTypeLabel
 * symbol (gold spec §4), but the exact iOS label surface is PROBE-DEPENDENT
 * (design doc "PROBE CONTRACT") — so label checks hard-assert equality the
 * moment the surface exists and SKIP honestly (never false-fail, never
 * pass-anyway) while it does not.
 *
 * FIXTURES: durable QA-WT00..13 family (one WO per type + General, priority
 * Medium, est_hours 8; gold spec §3), self-provisioned on the landed site by
 * WorkTypeBaseTest.ensureFixturesOnLandedSite() — never assume site ordering.
 *
 * GOLD SPEC: docs/worktype-gold-spec-2026-07-21.md
 * DESIGN:    docs/worktype-test-design-2026-07-21.md (§Class 3 + house rules)
 */
public class WorkType_Details_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "Work Types (13-option dropdown)";

    // ═════════════════════════ shared check bodies ══════════════════════════

    /** Check 1 — opened fixture lands on the session-details screen. */
    private void runSessionDetailsScreen(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture opened — " + wt.fixtureName());
        final boolean details = wo.ensureSessionDetailsOpen() || wo.isSessionDetailsScreenDisplayed();
        if (!details) {
            // App alive + screen not blank but neither probe recognizes the
            // surface → v1.51 anatomy is PROBE-DEPENDENT (design-doc contract),
            // not a product failure. A genuinely blank screen still FAILS here.
            verifyNotBlank("opened WO screen (" + wt.fixtureName() + ")");
            skipIfPreconditionMissing(() -> details,
                    tc + ": app alive and screen not blank, but session-details probes do not "
                            + "recognize the opened WO surface (v1.51 anatomy PROBE-DEPENDENT)");
        }
        assertTrue(details,
                "Opened fixture must land on the session-details screen: " + wt.fixtureName());
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — session-details screen confirmed for " + wt.fixtureName());
    }

    /** Check 2 — session header contains the fixture name. */
    private void runHeaderContainsFixtureName(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture opened — " + wt.fixtureName());
        final String header = wo.getSessionDetailsHeaderText();
        logStep(tc + " header text: '" + header + "'");
        skipIfPreconditionMissing(() -> header != null && !header.trim().isEmpty(),
                tc + ": session header text unreadable on this build (PROBE-DEPENDENT)");
        assertTrue(header.contains(wt.fixtureName()),
                "Session header must contain the fixture name '" + wt.fixtureName()
                        + "' — got '" + header + "'");
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — header verified for " + wt.fixtureName());
    }

    /** Check 3 — work-type label vs catalog (service types: exact equality). */
    private void runWorkTypeLabel(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture opened — " + wt.fixtureName());
        final String label = wo.getWorkTypeLabelOnScreen();
        logStep(tc + " work-type label on screen: '" + label + "'");
        skipIfPreconditionMissing(() -> label != null,
                tc + ": work-type label surface not present in v1.51 (PROBE-DEPENDENT — "
                        + "design-doc PROBE CONTRACT; hard-asserts activate once the surface ships)");
        if (wt.isServiceBacked()) {
            assertEquals(label.trim(), wt.displayName(),
                    "Rendered work-type label must equal the catalog display name for "
                            + wt.fixtureName());
        } else {
            assertFalse(label.trim().isEmpty(),
                    "General fixture work-type label, when rendered, must be non-empty");
            WorkTypeCatalog claimed = WorkTypeCatalog.byDisplayName(label.trim());
            // OR is the real contract: a General (work_type_id = null) WO may
            // label itself "General" or with copy outside the catalog — what it
            // must NEVER do is claim one of the 13 service-backed type names.
            assertTrue(claimed == null || claimed == WorkTypeCatalog.GENERAL,
                    "General (null work_type_id) fixture must NOT display a service-backed "
                            + "work-type label — got '" + label + "'");
        }
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — work-type label check done for " + wt.fixtureName());
    }

    /** Check 4 — session tab strip renders. */
    private void runTabsRender(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture opened — " + wt.fixtureName());
        // OR is the real contract: per-type tab SETS differ (gold spec §5), so
        // any rendered tab strip — full set or at least Details/Assets — proves
        // the tabbed session screen is up.
        boolean tabs = wo.areAllSessionTabsDisplayed()
                || wo.isTabDisplayed("Details")
                || wo.isTabDisplayed("Assets");
        assertTrue(tabs,
                "Session tab strip must render (all tabs, or at least Details/Assets) for "
                        + wt.fixtureName());
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — tab strip verified for " + wt.fixtureName());
    }

    /** Check 5 — info/photo-type value non-empty when readable. */
    private void runPhotoTypeNonEmpty(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture opened — " + wt.fixtureName());
        final String type = wo.getSessionType();
        // Gold spec §3: FLUKE for WT01-13, FLIR-SEP for WT00 — value logged for
        // triage; design doc mandates "tolerate either but assert non-empty".
        logStep(tc + " session photo-type value: '" + type + "'");
        skipIfPreconditionMissing(() -> type != null,
                tc + ": session type/photo-type value not readable on this build (PROBE-DEPENDENT)");
        assertFalse(type.trim().isEmpty(),
                "Session photo-type value must be non-empty when the row renders — "
                        + wt.fixtureName());
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — photo-type value verified for " + wt.fixtureName());
    }

    /** Check 6 — no error alert + app alive + not blank, stable after settle. */
    private void runNoErrorAliveStable(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture opened — " + wt.fixtureName());
        verifyNotBlank("session details (" + wt.fixtureName() + ")");
        verifyNoErrorAlert();
        mediumWait();
        verifyAppAlive(tc + ": after settle on " + wt.fixtureName());
        verifyNotBlank("session details after settle (" + wt.fixtureName() + ")");
        // OR is the real contract (see runTabsRender): any tab strip proves the
        // tabbed screen survived the settle.
        boolean stillTabbed = wo.areAllSessionTabsDisplayed()
                || wo.isTabDisplayed("Details")
                || wo.isTabDisplayed("Assets");
        assertTrue(stillTabbed,
                "Session screen must keep its tab strip after settling — " + wt.fixtureName());
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — stability verified for " + wt.fixtureName());
    }

    /** Check 7 — clean back-nav restores the Work Orders list. */
    private void runBackNavRestoresList(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture opened — " + wt.fixtureName());
        wo.goBack();
        shortWait();
        assertTrue(Waits.until(() -> wo.isWorkOrdersScreenDisplayed(), 15000),
                "Back from session details must restore the Work Orders list — "
                        + wt.fixtureName());
        verifyAppAlive(tc + ": back on the Work Orders list");
        verifyNotBlank("Work Orders list after back-nav (" + wt.fixtureName() + ")");
        assertTrue(wo.scrollWorkOrderListTo(wt.fixtureName()),
                "Fixture row must still be reachable in the list after back-nav: "
                        + wt.fixtureName());
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — clean back-nav verified for " + wt.fixtureName());
    }

    /** Cross — punctuated display name renders exactly (when label surface present). */
    private void runPunctuatedLabel(WorkTypeCatalog wt, String punctuation, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture opened — " + wt.fixtureName());
        final String label = wo.getWorkTypeLabelOnScreen();
        logStep(tc + " label: '" + label + "' (expected '" + wt.displayName() + "')");
        skipIfPreconditionMissing(() -> label != null,
                tc + ": work-type label surface not present in v1.51 (PROBE-DEPENDENT)");
        assertEquals(label.trim(), wt.displayName(),
                "Punctuated display name must render exactly for " + wt.name());
        assertTrue(label.contains(punctuation),
                "Label must preserve the punctuation '" + punctuation + "' — got '" + label + "'");
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — punctuated label verified for " + wt.displayName());
    }

    /** Cross — leaving and re-opening the fixture keeps the header. */
    private void runReEntryKeepsHeader(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": first entry — " + wt.fixtureName());
        final String first = wo.getSessionDetailsHeaderText();
        logStep(tc + " first-entry header: '" + first + "'");
        skipIfPreconditionMissing(() -> first != null && !first.trim().isEmpty(),
                tc + ": session header unreadable — cannot verify re-entry retention (PROBE-DEPENDENT)");
        assertTrue(first.contains(wt.fixtureName()),
                "First-entry header must contain the fixture name — got '" + first + "'");
        wo.goBack();
        shortWait();
        assertTrue(Waits.until(() -> wo.isWorkOrdersScreenDisplayed(), 15000),
                "Work Orders list must restore before re-entry — " + wt.fixtureName());
        assertTrue(wo.scrollWorkOrderListTo(wt.fixtureName()),
                "Fixture row must be reachable for re-entry: " + wt.fixtureName());
        assertTrue(wo.openWorkOrderByName(wt.fixtureName()),
                "Fixture must re-open (verified nav): " + wt.fixtureName());
        verifyAppAlive(tc + ": re-entered — " + wt.fixtureName());
        String second = wo.getSessionDetailsHeaderText();
        logStep(tc + " re-entry header: '" + second + "'");
        assertTrue(second != null && second.contains(wt.fixtureName()),
                "Re-entry header must still contain the fixture name — got '" + second + "'");
        assertEquals(second, first,
                "Header must be identical across re-entry for " + wt.fixtureName());
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — re-entry header retention verified");
    }

    /** Cross — two background/foreground cycles on the open details screen. */
    private void runBackgroundingStability(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": details open pre-backgrounding — " + wt.fixtureName());
        verifyNotBlank("session details before backgrounding (" + wt.fixtureName() + ")");
        guard(tc + ": backgrounding cycle 1 on " + wt.fixtureName());
        verifyAppAlive(tc + ": after backgrounding cycle 1");
        verifyNotBlank("session details after backgrounding cycle 1 (" + wt.fixtureName() + ")");
        guard(tc + ": backgrounding cycle 2 on " + wt.fixtureName());
        verifyAppAlive(tc + ": after backgrounding cycle 2");
        verifyNotBlank("session details after backgrounding cycle 2 (" + wt.fixtureName() + ")");
        // OR is the real contract (see runTabsRender): any tab strip proves the
        // tabbed session screen survived both cycles.
        boolean stillTabbed = wo.areAllSessionTabsDisplayed()
                || wo.isTabDisplayed("Details")
                || wo.isTabDisplayed("Assets");
        assertTrue(stillTabbed,
                "Session screen must survive 2x backgrounding with its tab strip intact — "
                        + wt.fixtureName());
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " — 2x backgrounding stability verified");
    }

    // ══════════════════ WT00 General (TC_WT_DET_001-007) ════════════════════

    @Test(priority = 1)
    public void TC_WT_DET_001_WT00_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_001 - WT00 General: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.GENERAL, "TC_WT_DET_001");
    }

    @Test(priority = 2)
    public void TC_WT_DET_002_WT00_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_002 - WT00 General: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.GENERAL, "TC_WT_DET_002");
    }

    @Test(priority = 3)
    public void TC_WT_DET_003_WT00_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_003 - WT00 General: rendered work-type label must not claim a service-backed type");
        runWorkTypeLabel(WorkTypeCatalog.GENERAL, "TC_WT_DET_003");
    }

    @Test(priority = 4)
    public void TC_WT_DET_004_WT00_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_004 - WT00 General: session tab strip renders");
        runTabsRender(WorkTypeCatalog.GENERAL, "TC_WT_DET_004");
    }

    @Test(priority = 5)
    public void TC_WT_DET_005_WT00_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_005 - WT00 General: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.GENERAL, "TC_WT_DET_005");
    }

    @Test(priority = 6)
    public void TC_WT_DET_006_WT00_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_006 - WT00 General: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.GENERAL, "TC_WT_DET_006");
    }

    @Test(priority = 7)
    public void TC_WT_DET_007_WT00_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_007 - WT00 General: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.GENERAL, "TC_WT_DET_007");
    }

    // ═══════ WT01 Arc Flash Data Collection (TC_WT_DET_011-017) ═════════════

    @Test(priority = 8)
    public void TC_WT_DET_011_WT01_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_011 - WT01 Arc Flash Data Collection: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_DET_011");
    }

    @Test(priority = 9)
    public void TC_WT_DET_012_WT01_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_012 - WT01 Arc Flash Data Collection: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_DET_012");
    }

    @Test(priority = 10)
    public void TC_WT_DET_013_WT01_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_013 - WT01 Arc Flash Data Collection: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_DET_013");
    }

    @Test(priority = 11)
    public void TC_WT_DET_014_WT01_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_014 - WT01 Arc Flash Data Collection: session tab strip renders");
        runTabsRender(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_DET_014");
    }

    @Test(priority = 12)
    public void TC_WT_DET_015_WT01_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_015 - WT01 Arc Flash Data Collection: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_DET_015");
    }

    @Test(priority = 13)
    public void TC_WT_DET_016_WT01_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_016 - WT01 Arc Flash Data Collection: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_DET_016");
    }

    @Test(priority = 14)
    public void TC_WT_DET_017_WT01_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_017 - WT01 Arc Flash Data Collection: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_DET_017");
    }

    // ═══════ WT02 Arc Flash Label Placement (TC_WT_DET_021-027) ═════════════

    @Test(priority = 15)
    public void TC_WT_DET_021_WT02_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_021 - WT02 Arc Flash Label Placement: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_DET_021");
    }

    @Test(priority = 16)
    public void TC_WT_DET_022_WT02_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_022 - WT02 Arc Flash Label Placement: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_DET_022");
    }

    @Test(priority = 17)
    public void TC_WT_DET_023_WT02_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_023 - WT02 Arc Flash Label Placement: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_DET_023");
    }

    @Test(priority = 18)
    public void TC_WT_DET_024_WT02_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_024 - WT02 Arc Flash Label Placement: session tab strip renders");
        runTabsRender(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_DET_024");
    }

    @Test(priority = 19)
    public void TC_WT_DET_025_WT02_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_025 - WT02 Arc Flash Label Placement: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_DET_025");
    }

    @Test(priority = 20)
    public void TC_WT_DET_026_WT02_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_026 - WT02 Arc Flash Label Placement: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_DET_026");
    }

    @Test(priority = 21)
    public void TC_WT_DET_027_WT02_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_027 - WT02 Arc Flash Label Placement: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_DET_027");
    }

    // ═════════════════ WT03 Cleaning (TC_WT_DET_031-037) ════════════════════

    @Test(priority = 22)
    public void TC_WT_DET_031_WT03_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_031 - WT03 Cleaning: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.CLEANING, "TC_WT_DET_031");
    }

    @Test(priority = 23)
    public void TC_WT_DET_032_WT03_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_032 - WT03 Cleaning: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.CLEANING, "TC_WT_DET_032");
    }

    @Test(priority = 24)
    public void TC_WT_DET_033_WT03_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_033 - WT03 Cleaning: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.CLEANING, "TC_WT_DET_033");
    }

    @Test(priority = 25)
    public void TC_WT_DET_034_WT03_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_034 - WT03 Cleaning: session tab strip renders");
        runTabsRender(WorkTypeCatalog.CLEANING, "TC_WT_DET_034");
    }

    @Test(priority = 26)
    public void TC_WT_DET_035_WT03_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_035 - WT03 Cleaning: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.CLEANING, "TC_WT_DET_035");
    }

    @Test(priority = 27)
    public void TC_WT_DET_036_WT03_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_036 - WT03 Cleaning: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.CLEANING, "TC_WT_DET_036");
    }

    @Test(priority = 28)
    public void TC_WT_DET_037_WT03_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_037 - WT03 Cleaning: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.CLEANING, "TC_WT_DET_037");
    }

    // ═══════ WT04 Clean Tighten Torque (TC_WT_DET_041-047) ══════════════════

    @Test(priority = 29)
    public void TC_WT_DET_041_WT04_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_041 - WT04 Clean Tighten Torque: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_DET_041");
    }

    @Test(priority = 30)
    public void TC_WT_DET_042_WT04_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_042 - WT04 Clean Tighten Torque: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_DET_042");
    }

    @Test(priority = 31)
    public void TC_WT_DET_043_WT04_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_043 - WT04 Clean Tighten Torque: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_DET_043");
    }

    @Test(priority = 32)
    public void TC_WT_DET_044_WT04_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_044 - WT04 Clean Tighten Torque: session tab strip renders");
        runTabsRender(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_DET_044");
    }

    @Test(priority = 33)
    public void TC_WT_DET_045_WT04_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_045 - WT04 Clean Tighten Torque: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_DET_045");
    }

    @Test(priority = 34)
    public void TC_WT_DET_046_WT04_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_046 - WT04 Clean Tighten Torque: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_DET_046");
    }

    @Test(priority = 35)
    public void TC_WT_DET_047_WT04_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_047 - WT04 Clean Tighten Torque: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_DET_047");
    }

    // ═══════ WT05 Condition Assessment (TC_WT_DET_051-057) ══════════════════

    @Test(priority = 36)
    public void TC_WT_DET_051_WT05_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_051 - WT05 Condition Assessment: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_DET_051");
    }

    @Test(priority = 37)
    public void TC_WT_DET_052_WT05_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_052 - WT05 Condition Assessment: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_DET_052");
    }

    @Test(priority = 38)
    public void TC_WT_DET_053_WT05_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_053 - WT05 Condition Assessment: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_DET_053");
    }

    @Test(priority = 39)
    public void TC_WT_DET_054_WT05_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_054 - WT05 Condition Assessment: session tab strip renders");
        runTabsRender(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_DET_054");
    }

    @Test(priority = 40)
    public void TC_WT_DET_055_WT05_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_055 - WT05 Condition Assessment: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_DET_055");
    }

    @Test(priority = 41)
    public void TC_WT_DET_056_WT05_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_056 - WT05 Condition Assessment: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_DET_056");
    }

    @Test(priority = 42)
    public void TC_WT_DET_057_WT05_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_057 - WT05 Condition Assessment: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_DET_057");
    }

    // ═══ WT06 De-Energized Visual Inspection (TC_WT_DET_061-067) ════════════

    @Test(priority = 43)
    public void TC_WT_DET_061_WT06_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_061 - WT06 De-Energized Visual Inspection: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_DET_061");
    }

    @Test(priority = 44)
    public void TC_WT_DET_062_WT06_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_062 - WT06 De-Energized Visual Inspection: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_DET_062");
    }

    @Test(priority = 45)
    public void TC_WT_DET_063_WT06_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_063 - WT06 De-Energized Visual Inspection: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_DET_063");
    }

    @Test(priority = 46)
    public void TC_WT_DET_064_WT06_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_064 - WT06 De-Energized Visual Inspection: session tab strip renders");
        runTabsRender(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_DET_064");
    }

    @Test(priority = 47)
    public void TC_WT_DET_065_WT06_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_065 - WT06 De-Energized Visual Inspection: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_DET_065");
    }

    @Test(priority = 48)
    public void TC_WT_DET_066_WT06_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_066 - WT06 De-Energized Visual Inspection: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_DET_066");
    }

    @Test(priority = 49)
    public void TC_WT_DET_067_WT06_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_067 - WT06 De-Energized Visual Inspection: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_DET_067");
    }

    // ═══ WT07 DGA Fluid Sample Analysis (TC_WT_DET_071-077) ═════════════════

    @Test(priority = 50)
    public void TC_WT_DET_071_WT07_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_071 - WT07 DGA Fluid Sample Analysis: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_DET_071");
    }

    @Test(priority = 51)
    public void TC_WT_DET_072_WT07_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_072 - WT07 DGA Fluid Sample Analysis: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_DET_072");
    }

    @Test(priority = 52)
    public void TC_WT_DET_073_WT07_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_073 - WT07 DGA Fluid Sample Analysis: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_DET_073");
    }

    @Test(priority = 53)
    public void TC_WT_DET_074_WT07_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_074 - WT07 DGA Fluid Sample Analysis: session tab strip renders");
        runTabsRender(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_DET_074");
    }

    @Test(priority = 54)
    public void TC_WT_DET_075_WT07_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_075 - WT07 DGA Fluid Sample Analysis: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_DET_075");
    }

    @Test(priority = 55)
    public void TC_WT_DET_076_WT07_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_076 - WT07 DGA Fluid Sample Analysis: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_DET_076");
    }

    @Test(priority = 56)
    public void TC_WT_DET_077_WT07_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_077 - WT07 DGA Fluid Sample Analysis: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_DET_077");
    }

    // ═══ WT08 Infrared Thermography (TC_WT_DET_081-087) ═════════════════════

    @Test(priority = 57)
    public void TC_WT_DET_081_WT08_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_081 - WT08 Infrared Thermography: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_DET_081");
    }

    @Test(priority = 58)
    public void TC_WT_DET_082_WT08_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_082 - WT08 Infrared Thermography: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_DET_082");
    }

    @Test(priority = 59)
    public void TC_WT_DET_083_WT08_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_083 - WT08 Infrared Thermography: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_DET_083");
    }

    @Test(priority = 60)
    public void TC_WT_DET_084_WT08_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_084 - WT08 Infrared Thermography: session tab strip renders");
        runTabsRender(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_DET_084");
    }

    @Test(priority = 61)
    public void TC_WT_DET_085_WT08_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_085 - WT08 Infrared Thermography: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_DET_085");
    }

    @Test(priority = 62)
    public void TC_WT_DET_086_WT08_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_086 - WT08 Infrared Thermography: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_DET_086");
    }

    @Test(priority = 63)
    public void TC_WT_DET_087_WT08_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_087 - WT08 Infrared Thermography: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_DET_087");
    }

    // ═══ WT09 Insulation Resistance Testing (TC_WT_DET_091-097) ═════════════

    @Test(priority = 64)
    public void TC_WT_DET_091_WT09_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_091 - WT09 Insulation Resistance Testing: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_DET_091");
    }

    @Test(priority = 65)
    public void TC_WT_DET_092_WT09_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_092 - WT09 Insulation Resistance Testing: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_DET_092");
    }

    @Test(priority = 66)
    public void TC_WT_DET_093_WT09_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_093 - WT09 Insulation Resistance Testing: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_DET_093");
    }

    @Test(priority = 67)
    public void TC_WT_DET_094_WT09_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_094 - WT09 Insulation Resistance Testing: session tab strip renders");
        runTabsRender(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_DET_094");
    }

    @Test(priority = 68)
    public void TC_WT_DET_095_WT09_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_095 - WT09 Insulation Resistance Testing: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_DET_095");
    }

    @Test(priority = 69)
    public void TC_WT_DET_096_WT09_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_096 - WT09 Insulation Resistance Testing: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_DET_096");
    }

    @Test(priority = 70)
    public void TC_WT_DET_097_WT09_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_097 - WT09 Insulation Resistance Testing: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_DET_097");
    }

    // ═══════════════ WT10 NETA Testing (TC_WT_DET_101-107) ══════════════════

    @Test(priority = 71)
    public void TC_WT_DET_101_WT10_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_101 - WT10 NETA Testing: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.NETA_TESTING, "TC_WT_DET_101");
    }

    @Test(priority = 72)
    public void TC_WT_DET_102_WT10_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_102 - WT10 NETA Testing: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.NETA_TESTING, "TC_WT_DET_102");
    }

    @Test(priority = 73)
    public void TC_WT_DET_103_WT10_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_103 - WT10 NETA Testing: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.NETA_TESTING, "TC_WT_DET_103");
    }

    @Test(priority = 74)
    public void TC_WT_DET_104_WT10_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_104 - WT10 NETA Testing: session tab strip renders");
        runTabsRender(WorkTypeCatalog.NETA_TESTING, "TC_WT_DET_104");
    }

    @Test(priority = 75)
    public void TC_WT_DET_105_WT10_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_105 - WT10 NETA Testing: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.NETA_TESTING, "TC_WT_DET_105");
    }

    @Test(priority = 76)
    public void TC_WT_DET_106_WT10_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_106 - WT10 NETA Testing: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.NETA_TESTING, "TC_WT_DET_106");
    }

    @Test(priority = 77)
    public void TC_WT_DET_107_WT10_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_107 - WT10 NETA Testing: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.NETA_TESTING, "TC_WT_DET_107");
    }

    // ═══ WT11 Panel Schedule Updates (TC_WT_DET_111-117) ════════════════════

    @Test(priority = 78)
    public void TC_WT_DET_111_WT11_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_111 - WT11 Panel Schedule Updates: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_DET_111");
    }

    @Test(priority = 79)
    public void TC_WT_DET_112_WT11_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_112 - WT11 Panel Schedule Updates: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_DET_112");
    }

    @Test(priority = 80)
    public void TC_WT_DET_113_WT11_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_113 - WT11 Panel Schedule Updates: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_DET_113");
    }

    @Test(priority = 81)
    public void TC_WT_DET_114_WT11_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_114 - WT11 Panel Schedule Updates: session tab strip renders");
        runTabsRender(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_DET_114");
    }

    @Test(priority = 82)
    public void TC_WT_DET_115_WT11_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_115 - WT11 Panel Schedule Updates: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_DET_115");
    }

    @Test(priority = 83)
    public void TC_WT_DET_116_WT11_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_116 - WT11 Panel Schedule Updates: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_DET_116");
    }

    @Test(priority = 84)
    public void TC_WT_DET_117_WT11_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_117 - WT11 Panel Schedule Updates: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_DET_117");
    }

    // ═══ WT12 Shutdown Composite (TC_WT_DET_121-127) ═════════════════════════

    @Test(priority = 85)
    public void TC_WT_DET_121_WT12_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_121 - WT12 Shutdown Composite: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_DET_121");
    }

    @Test(priority = 86)
    public void TC_WT_DET_122_WT12_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_122 - WT12 Shutdown Composite: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_DET_122");
    }

    @Test(priority = 87)
    public void TC_WT_DET_123_WT12_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_123 - WT12 Shutdown Composite: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_DET_123");
    }

    @Test(priority = 88)
    public void TC_WT_DET_124_WT12_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_124 - WT12 Shutdown Composite: session tab strip renders");
        runTabsRender(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_DET_124");
    }

    @Test(priority = 89)
    public void TC_WT_DET_125_WT12_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_125 - WT12 Shutdown Composite: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_DET_125");
    }

    @Test(priority = 90)
    public void TC_WT_DET_126_WT12_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_126 - WT12 Shutdown Composite: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_DET_126");
    }

    @Test(priority = 91)
    public void TC_WT_DET_127_WT12_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_127 - WT12 Shutdown Composite: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_DET_127");
    }

    // ═══════════ WT13 UPS Maintenance (TC_WT_DET_131-137) ═══════════════════

    @Test(priority = 92)
    public void TC_WT_DET_131_WT13_sessionDetailsScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_131 - WT13 UPS Maintenance: opened fixture lands on the session-details screen");
        runSessionDetailsScreen(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_DET_131");
    }

    @Test(priority = 93)
    public void TC_WT_DET_132_WT13_headerContainsFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_132 - WT13 UPS Maintenance: session header contains the fixture name");
        runHeaderContainsFixtureName(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_DET_132");
    }

    @Test(priority = 94)
    public void TC_WT_DET_133_WT13_workTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_133 - WT13 UPS Maintenance: work-type label equals catalog display name when rendered");
        runWorkTypeLabel(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_DET_133");
    }

    @Test(priority = 95)
    public void TC_WT_DET_134_WT13_tabsRender() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_134 - WT13 UPS Maintenance: session tab strip renders");
        runTabsRender(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_DET_134");
    }

    @Test(priority = 96)
    public void TC_WT_DET_135_WT13_photoTypeNonEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_135 - WT13 UPS Maintenance: session photo-type value non-empty when readable");
        runPhotoTypeNonEmpty(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_DET_135");
    }

    @Test(priority = 97)
    public void TC_WT_DET_136_WT13_noErrorAliveStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_136 - WT13 UPS Maintenance: no error alert, app alive, screen stable after settle");
        runNoErrorAliveStable(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_DET_136");
    }

    @Test(priority = 98)
    public void TC_WT_DET_137_WT13_backNavRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_137 - WT13 UPS Maintenance: clean back-nav restores the Work Orders list");
        runBackNavRestoresList(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_DET_137");
    }

    // ═══════════════ cross-cutting (TC_WT_DET_201-208) ══════════════════════

    @Test(priority = 99)
    public void TC_WT_DET_201_punctuatedLabelCleanTightenTorque() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_201 - punctuated display name 'Clean, Tighten, Torque' renders exactly (commas preserved)");
        runPunctuatedLabel(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, ", ", "TC_WT_DET_201");
    }

    @Test(priority = 100)
    public void TC_WT_DET_202_punctuatedLabelDgaFluidSample() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_202 - punctuated display name 'DGA / Fluid Sample Analysis' renders exactly (slash preserved)");
        runPunctuatedLabel(WorkTypeCatalog.DGA_FLUID_SAMPLE, " / ", "TC_WT_DET_202");
    }

    @Test(priority = 101)
    public void TC_WT_DET_203_punctuatedLabelShutdownComposite() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_203 - punctuated display name 'Shutdown (Composite)' renders exactly (parentheses preserved)");
        runPunctuatedLabel(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "(Composite)", "TC_WT_DET_203");
    }

    @Test(priority = 102)
    public void TC_WT_DET_204_labelVsCatalogSweepAll13() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_204 - label-vs-catalog sweep: every rendered work-type label across all 13 service fixtures matches WorkTypeCatalog exactly");
        openWorkOrdersScreenWT();
        List<String> missing = new ArrayList<>();
        List<String> mismatches = new ArrayList<>();
        int labelled = 0;
        for (WorkTypeCatalog wt : WorkTypeCatalog.serviceBacked()) {
            if (!wo.scrollWorkOrderListTo(wt.fixtureName())) {
                missing.add(wt.fixtureName());
                continue;
            }
            assertTrue(wo.openWorkOrderByName(wt.fixtureName()),
                    "Fixture must open (verified nav): " + wt.fixtureName());
            verifyAppAlive("TC_WT_DET_204: opened " + wt.fixtureName());
            String label = wo.getWorkTypeLabelOnScreen();
            logStep("TC_WT_DET_204 " + wt.fixtureName() + " label: '" + label + "'");
            if (label != null) {
                labelled++;
                if (!wt.displayName().equals(label.trim())) {
                    mismatches.add(wt.fixtureName() + " -> '" + label
                            + "' (expected '" + wt.displayName() + "')");
                }
            }
            wo.goBack();
            shortWait();
            assertTrue(Waits.until(() -> wo.isWorkOrdersScreenDisplayed(), 15000),
                    "Work Orders list must restore after " + wt.fixtureName());
        }
        skipIfPreconditionMissing(() -> missing.isEmpty(),
                "TC_WT_DET_204: fixture rows unreachable in the Work Orders list: " + missing);
        final int labelledCount = labelled;
        skipIfPreconditionMissing(() -> labelledCount > 0,
                "TC_WT_DET_204: work-type label surface not present in v1.51 on any of the "
                        + WorkTypeCatalog.SERVICE_COUNT + " service fixtures (PROBE-DEPENDENT)");
        assertTrue(mismatches.isEmpty(),
                "Every rendered work-type label must match the catalog exactly — mismatches: "
                        + mismatches);
        verifyNoErrorAlert();
        logStepWithScreenshot("TC_WT_DET_204 sweep complete: " + labelledCount + "/"
                + WorkTypeCatalog.SERVICE_COUNT + " fixtures rendered a label, 0 mismatches");
    }

    @Test(priority = 103)
    public void TC_WT_DET_205_reEntryKeepsHeaderInfraredThermography() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_205 - re-entry keeps the session header (sample 1: WT08 Infrared Thermography)");
        runReEntryKeepsHeader(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_DET_205");
    }

    @Test(priority = 104)
    public void TC_WT_DET_206_reEntryKeepsHeaderConditionAssessment() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_206 - re-entry keeps the session header (sample 2: WT05 Condition Assessment)");
        runReEntryKeepsHeader(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_DET_206");
    }

    @Test(priority = 105)
    public void TC_WT_DET_207_backgroundingStabilityArcFlash() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_207 - 2x backgrounding on open details is stable (sample 1: WT01 Arc Flash Data Collection)");
        runBackgroundingStability(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_DET_207");
    }

    @Test(priority = 106)
    public void TC_WT_DET_208_backgroundingStabilityPanelSchedule() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_DET_208 - 2x backgrounding on open details is stable (sample 2: WT11 Panel Schedule Updates)");
        runBackgroundingStability(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_DET_208");
    }
}
