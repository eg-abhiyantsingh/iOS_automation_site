package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.ExtentReportManager;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * WorkType_List_Test — TC_WT_LIST_* — iOS Work Orders LIST anatomy for the
 * QA-WT work-type fixture family (design doc Class 2,
 * docs/worktype-test-design-2026-07-21.md; domain truth
 * docs/worktype-gold-spec-2026-07-21.md §3).
 *
 * WHAT: for each of the 14 durable fixtures (QA-WT00 General + QA-WT01..13,
 * one per service-backed work type) this class proves the Work Orders list
 * renders exactly one well-formed row and that the row round-trips:
 *   1. reachable by the bounded bidirectional scroll (scrollWorkOrderListTo);
 *   2. row composite BEGINSWITH the exact untruncated fixture name;
 *   3. row composite ENDSWITH ', Medium' (ZP-3109 priority-chip contract —
 *      every fixture was created with priority Medium, gold spec §3);
 *   4. openWorkOrderByName is a VERIFIED nav (row disappears) and the opened
 *      screen is alive + not blank + alert-free;
 *   5. back-nav restores the list (waitForWorkOrdersScreen) with the row
 *      still findable;
 *   6. WorkOrderPage.rowPriority(composite) parses to exactly "Medium".
 *
 * WHY: work types landed server-side first (v1.51 consumes work_type_id);
 * the list is the entry point for every deeper TC_WT_* class, so a truncated
 * name, a lost priority chip, a duplicate row, or a broken open/back cycle
 * here invalidates the Details/Behavior classes. Rows are Buttons named
 * '<name>, <Priority>' — all matching is BEGINSWITH on the fixture name,
 * never index-based (house rule).
 *
 * FIXTURE FAMILY: QA-WT00..13, self-provisioned on the landed site by
 * WorkTypeBaseTest.ensureFixturesOnLandedSite() (site ordering drifted once —
 * never assume a fixed first site). Fixture names strip punctuation so the
 * NS-predicate BEGINSWITH match stays trivial (WorkTypeCatalog.fixtureName()).
 *
 * TC ID SCHEME: per-fixture ids are TC_WT_LIST_&lt;NN&gt;&lt;k&gt; where NN =
 * fixture number 00..13 and k = check 1..6 (e.g. TC_WT_LIST_081 = QA-WT08
 * reachability, TC_WT_LIST_136 = QA-WT13 priority parse). Cross-cutting tests
 * are TC_WT_LIST_201..212.
 *
 * DUPLICATE DETECTION STRATEGY (TC_WT_LIST_202..205): the list API is
 * first-match-by-prefix, so a UI-only test cannot see a second identical row
 * directly. We therefore (a) force a second independent scroll pass (scroll
 * away to a far family member, then re-find) and assert composite EQUALITY —
 * any duplicate that differs in truncation or priority chip shows up as an
 * inequality between passes; and (b) close the identical-duplicate gap on the
 * API side (TC_WT_LIST_205) by counting exact '"name": "<fixture>"'
 * occurrences in listWorkOrdersJson — the backend must hold EXACTLY ONE WO
 * per fixture name.
 *
 * SKIP policy: a missing fixture row is a genuine environment precondition
 * ONLY for the anatomy/open/back/parse checks (the k=1 reachability test is
 * the one that hard-fails when the backend provisioned the fixture but the
 * list does not show it). Backend down + family absent ⇒ honest SKIP with the
 * TC id in the reason. No pass-anyway shapes anywhere: every test ends in a
 * hard assert through the BaseTest wrappers, with crash/blank guards on every
 * UI flow.
 */
public class WorkType_List_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "Work Types (13-option dropdown)";

    // Fixture aliases — WTnn matches the QA-WTnn fixture numbering (gold spec §3).
    private static final WorkTypeCatalog WT00 = WorkTypeCatalog.GENERAL;
    private static final WorkTypeCatalog WT01 = WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION;
    private static final WorkTypeCatalog WT02 = WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT;
    private static final WorkTypeCatalog WT03 = WorkTypeCatalog.CLEANING;
    private static final WorkTypeCatalog WT04 = WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE;
    private static final WorkTypeCatalog WT05 = WorkTypeCatalog.CONDITION_ASSESSMENT;
    private static final WorkTypeCatalog WT06 = WorkTypeCatalog.DE_ENERGIZED_VISUAL;
    private static final WorkTypeCatalog WT07 = WorkTypeCatalog.DGA_FLUID_SAMPLE;
    private static final WorkTypeCatalog WT08 = WorkTypeCatalog.INFRARED_THERMOGRAPHY;
    private static final WorkTypeCatalog WT09 = WorkTypeCatalog.INSULATION_RESISTANCE;
    private static final WorkTypeCatalog WT10 = WorkTypeCatalog.NETA_TESTING;
    private static final WorkTypeCatalog WT11 = WorkTypeCatalog.PANEL_SCHEDULE_UPDATES;
    private static final WorkTypeCatalog WT12 = WorkTypeCatalog.SHUTDOWN_COMPOSITE;
    private static final WorkTypeCatalog WT13 = WorkTypeCatalog.UPS_MAINTENANCE;

    /** All 14 fixtures in fixture-number order: WT00 General first, then WT01..WT13. */
    private static List<WorkTypeCatalog> fixtureOrder() {
        List<WorkTypeCatalog> order = new ArrayList<>();
        order.add(WorkTypeCatalog.GENERAL);
        order.addAll(WorkTypeCatalog.serviceBacked());
        return order;
    }

    /** Count non-overlapping occurrences of {@code needle} in {@code hay} (no JSON lib in this project). */
    private static int countNeedle(String hay, String needle) {
        if (hay == null || needle == null || needle.isEmpty()) return 0;
        int count = 0;
        int i = 0;
        while ((i = hay.indexOf(needle, i)) >= 0) {
            count++;
            i += needle.length();
        }
        return count;
    }

    // ────────────────────────── shared check bodies ──────────────────────────

    /** Entry with crash/blank guards: login → dashboard → Work Orders list. */
    private void openListGuarded(String tc) {
        openWorkOrdersScreenWT();
        verifyAppAlive(tc + ": Work Orders list opened");
        verifyNotBlank("Work Orders list (" + tc + ")");
    }

    /**
     * Check 1 — reachability. Missing row is only a SKIP when the backend
     * could not provision the family (genuine environment precondition);
     * if provisioning succeeded, an unreachable row is a hard FAIL (the list
     * is hiding a work order the server knows about).
     */
    private void runReachable(WorkTypeCatalog wt, String tc) {
        String fixture = wt.fixtureName();
        openListGuarded(tc);
        boolean reachable = wo.scrollWorkOrderListTo(fixture);
        if (!reachable) {
            boolean provisioned = ensureFixturesOnLandedSite();
            skipIfPreconditionMissing(() -> provisioned,
                    tc + ": fixture '" + fixture + "' absent AND backend provisioning unavailable"
                    + " — cannot distinguish a list bug from a missing fixture");
        }
        assertTrue(reachable,
                "Fixture row '" + fixture + "' must be reachable by bounded scroll in the Work Orders list");
        verifyAppAlive(tc + ": row on screen");
        logStepWithScreenshot(tc + " verified: '" + fixture + "' reachable");
    }

    /** Bring the fixture row on screen (SKIP when genuinely absent) and read its composite. */
    private String compositeOrSkip(WorkTypeCatalog wt, String tc) {
        String fixture = wt.fixtureName();
        openListGuarded(tc);
        boolean onScreen = wo.scrollWorkOrderListTo(fixture);
        skipIfPreconditionMissing(() -> onScreen,
                tc + ": fixture '" + fixture + "' not present in the Work Orders list");
        String composite = wo.getWorkOrderRowComposite(fixture);
        assertTrue(composite != null && !composite.isEmpty(),
                "Row composite must be readable for on-screen fixture '" + fixture + "'");
        return composite;
    }

    /** Check 2 — composite BEGINSWITH the exact untruncated fixture name. */
    private void runCompositeBeginsWith(WorkTypeCatalog wt, String tc) {
        String fixture = wt.fixtureName();
        String composite = compositeOrSkip(wt, tc);
        logStep(tc + " composite: '" + composite + "'");
        assertTrue(composite.startsWith(fixture),
                "Row composite must BEGIN WITH the exact untruncated fixture name '" + fixture
                + "' — got '" + composite + "'");
        logStepWithScreenshot(tc + " verified: name untruncated");
    }

    /** Check 3 — composite ENDSWITH ', Medium' (ZP-3109 priority-chip contract). */
    private void runCompositeEndsMedium(WorkTypeCatalog wt, String tc) {
        String composite = compositeOrSkip(wt, tc);
        logStep(tc + " composite: '" + composite + "'");
        assertTrue(composite.endsWith(", Medium"),
                "Row composite must END WITH ', Medium' (fixture priority chip, ZP-3109) — got '"
                + composite + "'");
        logStepWithScreenshot(tc + " verified: ', Medium' suffix present");
    }

    /** Check 4 — verified-nav open; opened screen alive, rendering, alert-free. */
    private void runOpenVerifiedNav(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc); // scroll + honest SKIP + verified-open assert
        verifyAppAlive(tc + ": opened fixture '" + wt.fixtureName() + "'");
        verifyNotBlank("Opened WO screen for '" + wt.fixtureName() + "' (" + tc + ")");
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " verified: opened screen alive and rendering");
        wo.goBack(); // nav hygiene only — the back CONTRACT is check 5's assert
        shortWait();
    }

    /** Check 5 — back-nav restores the list AND the row is still findable. */
    private void runBackRestoresList(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture opened");
        wo.goBack();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(),
                "Back from '" + wt.fixtureName() + "' must land on the Work Orders list");
        verifyNotBlank("Work Orders list after back-nav (" + tc + ")");
        assertTrue(wo.scrollWorkOrderListTo(wt.fixtureName()),
                "Row '" + wt.fixtureName() + "' must still be findable after back-nav");
        verifyAppAlive(tc + ": list restored with row present");
        logStepWithScreenshot(tc + " verified: open→back round-trip clean");
    }

    /** Check 6 — rowPriority(composite) parses to exactly "Medium". */
    private void runRowPriorityMedium(WorkTypeCatalog wt, String tc) {
        String composite = compositeOrSkip(wt, tc);
        String priority = WorkOrderPage.rowPriority(composite);
        logStep(tc + " composite='" + composite + "' → priority='" + priority + "'");
        assertEquals(priority, "Medium",
                "rowPriority('" + composite + "') must parse the fixture's Medium chip");
        logStepWithScreenshot(tc + " verified: priority chip parses to Medium");
    }

    /**
     * Cross helper — two-pass duplicate probe: find the row, remember its
     * composite, scroll AWAY to a far family member (forces a genuinely fresh
     * second pass), re-find, and assert composite EQUALITY. A duplicate row
     * that differs in truncation or chip shows up as pass-2 resolving to a
     * different composite. (Identical duplicates are caught API-side by
     * TC_WT_LIST_205.)
     */
    private void runDuplicateTwoPass(WorkTypeCatalog wt, WorkTypeCatalog farAnchor, String tc) {
        String fixture = wt.fixtureName();
        String first = compositeOrSkip(wt, tc);
        assertTrue(first.startsWith(fixture),
                "Pass-1 composite must start with '" + fixture + "' — got '" + first + "'");
        boolean anchorFound = wo.scrollWorkOrderListTo(farAnchor.fixtureName());
        skipIfPreconditionMissing(() -> anchorFound,
                tc + ": far anchor '" + farAnchor.fixtureName()
                + "' unavailable — cannot force an independent second scroll pass");
        assertTrue(wo.scrollWorkOrderListTo(fixture),
                "Second scroll pass must re-find '" + fixture + "'");
        String second = wo.getWorkOrderRowComposite(fixture);
        logStep(tc + " pass1='" + first + "' pass2='" + second + "'");
        assertEquals(second, first,
                "Second scroll pass must resolve to the SAME single row (composite equality) — "
                + "a differing composite means a duplicate '" + fixture + "' row exists");
        verifyAppAlive(tc + ": two-pass duplicate probe done");
        logStepWithScreenshot(tc + " verified: single well-formed row across two passes");
    }

    /**
     * Cross helper — stability re-check: capture the composite, churn the
     * viewport twice (scroll to a far anchor and back), and assert the row
     * re-resolves to the identical composite with the Medium chip intact.
     */
    private void runStabilityRecheck(WorkTypeCatalog wt, WorkTypeCatalog awayAnchor, String tc) {
        String fixture = wt.fixtureName();
        String first = compositeOrSkip(wt, tc);
        assertTrue(first.startsWith(fixture),
                "Baseline composite must start with '" + fixture + "' — got '" + first + "'");
        assertEquals(WorkOrderPage.rowPriority(first), "Medium",
                "Baseline priority must parse to Medium for '" + fixture + "'");
        for (int pass = 1; pass <= 2; pass++) {
            boolean away = wo.scrollWorkOrderListTo(awayAnchor.fixtureName());
            final int p = pass;
            skipIfPreconditionMissing(() -> away,
                    tc + ": churn anchor '" + awayAnchor.fixtureName()
                    + "' unavailable on re-check pass " + p);
            assertTrue(wo.scrollWorkOrderListTo(fixture),
                    "Re-check pass " + pass + " must re-find '" + fixture + "'");
            String again = wo.getWorkOrderRowComposite(fixture);
            assertEquals(again, first,
                    "Composite must be byte-stable across re-check pass " + pass
                    + " — baseline '" + first + "', got '" + again + "'");
            verifyAppAlive(tc + ": re-check pass " + pass + " done");
        }
        logStepWithScreenshot(tc + " verified: composite stable across 2 churn passes");
    }

    // ═══════════════ Block 1 — reachability (14 × check 1) ═══════════════

    @Test(priority = 1)
    public void TC_WT_LIST_001_wt00GeneralRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_001 - " + WT00.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT00, "TC_WT_LIST_001");
    }

    @Test(priority = 2)
    public void TC_WT_LIST_011_wt01AfDataCollectionRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_011 - " + WT01.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT01, "TC_WT_LIST_011");
    }

    @Test(priority = 3)
    public void TC_WT_LIST_021_wt02AfLabelPlacementRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_021 - " + WT02.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT02, "TC_WT_LIST_021");
    }

    @Test(priority = 4)
    public void TC_WT_LIST_031_wt03CleaningRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_031 - " + WT03.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT03, "TC_WT_LIST_031");
    }

    @Test(priority = 5)
    public void TC_WT_LIST_041_wt04CleanTightenTorqueRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_041 - " + WT04.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT04, "TC_WT_LIST_041");
    }

    @Test(priority = 6)
    public void TC_WT_LIST_051_wt05ConditionAssessmentRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_051 - " + WT05.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT05, "TC_WT_LIST_051");
    }

    @Test(priority = 7)
    public void TC_WT_LIST_061_wt06DeEnergizedVisualRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_061 - " + WT06.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT06, "TC_WT_LIST_061");
    }

    @Test(priority = 8)
    public void TC_WT_LIST_071_wt07DgaFluidSampleRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_071 - " + WT07.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT07, "TC_WT_LIST_071");
    }

    @Test(priority = 9)
    public void TC_WT_LIST_081_wt08InfraredThermographyRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_081 - " + WT08.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT08, "TC_WT_LIST_081");
    }

    @Test(priority = 10)
    public void TC_WT_LIST_091_wt09InsulationResistanceRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_091 - " + WT09.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT09, "TC_WT_LIST_091");
    }

    @Test(priority = 11)
    public void TC_WT_LIST_101_wt10NetaTestingRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_101 - " + WT10.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT10, "TC_WT_LIST_101");
    }

    @Test(priority = 12)
    public void TC_WT_LIST_111_wt11PanelScheduleUpdatesRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_111 - " + WT11.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT11, "TC_WT_LIST_111");
    }

    @Test(priority = 13)
    public void TC_WT_LIST_121_wt12ShutdownCompositeRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_121 - " + WT12.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT12, "TC_WT_LIST_121");
    }

    @Test(priority = 14)
    public void TC_WT_LIST_131_wt13UpsMaintenanceRowReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_131 - " + WT13.fixtureName() + ": row reachable by bounded scroll");
        runReachable(WT13, "TC_WT_LIST_131");
    }

    // ═══════════ Block 2 — composite BEGINSWITH exact name (14 × check 2) ═══════════

    @Test(priority = 15)
    public void TC_WT_LIST_002_wt00GeneralCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_002 - " + WT00.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT00, "TC_WT_LIST_002");
    }

    @Test(priority = 16)
    public void TC_WT_LIST_012_wt01AfDataCollectionCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_012 - " + WT01.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT01, "TC_WT_LIST_012");
    }

    @Test(priority = 17)
    public void TC_WT_LIST_022_wt02AfLabelPlacementCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_022 - " + WT02.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT02, "TC_WT_LIST_022");
    }

    @Test(priority = 18)
    public void TC_WT_LIST_032_wt03CleaningCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_032 - " + WT03.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT03, "TC_WT_LIST_032");
    }

    @Test(priority = 19)
    public void TC_WT_LIST_042_wt04CleanTightenTorqueCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_042 - " + WT04.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT04, "TC_WT_LIST_042");
    }

    @Test(priority = 20)
    public void TC_WT_LIST_052_wt05ConditionAssessmentCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_052 - " + WT05.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT05, "TC_WT_LIST_052");
    }

    @Test(priority = 21)
    public void TC_WT_LIST_062_wt06DeEnergizedVisualCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_062 - " + WT06.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT06, "TC_WT_LIST_062");
    }

    @Test(priority = 22)
    public void TC_WT_LIST_072_wt07DgaFluidSampleCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_072 - " + WT07.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT07, "TC_WT_LIST_072");
    }

    @Test(priority = 23)
    public void TC_WT_LIST_082_wt08InfraredThermographyCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_082 - " + WT08.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT08, "TC_WT_LIST_082");
    }

    @Test(priority = 24)
    public void TC_WT_LIST_092_wt09InsulationResistanceCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_092 - " + WT09.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT09, "TC_WT_LIST_092");
    }

    @Test(priority = 25)
    public void TC_WT_LIST_102_wt10NetaTestingCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_102 - " + WT10.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT10, "TC_WT_LIST_102");
    }

    @Test(priority = 26)
    public void TC_WT_LIST_112_wt11PanelScheduleUpdatesCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_112 - " + WT11.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT11, "TC_WT_LIST_112");
    }

    @Test(priority = 27)
    public void TC_WT_LIST_122_wt12ShutdownCompositeCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_122 - " + WT12.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT12, "TC_WT_LIST_122");
    }

    @Test(priority = 28)
    public void TC_WT_LIST_132_wt13UpsMaintenanceCompositeBeginsWithName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_132 - " + WT13.fixtureName() + ": composite begins with exact untruncated name");
        runCompositeBeginsWith(WT13, "TC_WT_LIST_132");
    }

    // ═══════════ Block 3 — composite ENDSWITH ', Medium' (14 × check 3) ═══════════

    @Test(priority = 29)
    public void TC_WT_LIST_003_wt00GeneralCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_003 - " + WT00.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT00, "TC_WT_LIST_003");
    }

    @Test(priority = 30)
    public void TC_WT_LIST_013_wt01AfDataCollectionCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_013 - " + WT01.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT01, "TC_WT_LIST_013");
    }

    @Test(priority = 31)
    public void TC_WT_LIST_023_wt02AfLabelPlacementCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_023 - " + WT02.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT02, "TC_WT_LIST_023");
    }

    @Test(priority = 32)
    public void TC_WT_LIST_033_wt03CleaningCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_033 - " + WT03.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT03, "TC_WT_LIST_033");
    }

    @Test(priority = 33)
    public void TC_WT_LIST_043_wt04CleanTightenTorqueCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_043 - " + WT04.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT04, "TC_WT_LIST_043");
    }

    @Test(priority = 34)
    public void TC_WT_LIST_053_wt05ConditionAssessmentCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_053 - " + WT05.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT05, "TC_WT_LIST_053");
    }

    @Test(priority = 35)
    public void TC_WT_LIST_063_wt06DeEnergizedVisualCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_063 - " + WT06.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT06, "TC_WT_LIST_063");
    }

    @Test(priority = 36)
    public void TC_WT_LIST_073_wt07DgaFluidSampleCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_073 - " + WT07.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT07, "TC_WT_LIST_073");
    }

    @Test(priority = 37)
    public void TC_WT_LIST_083_wt08InfraredThermographyCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_083 - " + WT08.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT08, "TC_WT_LIST_083");
    }

    @Test(priority = 38)
    public void TC_WT_LIST_093_wt09InsulationResistanceCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_093 - " + WT09.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT09, "TC_WT_LIST_093");
    }

    @Test(priority = 39)
    public void TC_WT_LIST_103_wt10NetaTestingCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_103 - " + WT10.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT10, "TC_WT_LIST_103");
    }

    @Test(priority = 40)
    public void TC_WT_LIST_113_wt11PanelScheduleUpdatesCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_113 - " + WT11.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT11, "TC_WT_LIST_113");
    }

    @Test(priority = 41)
    public void TC_WT_LIST_123_wt12ShutdownCompositeCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_123 - " + WT12.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT12, "TC_WT_LIST_123");
    }

    @Test(priority = 42)
    public void TC_WT_LIST_133_wt13UpsMaintenanceCompositeEndsMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_133 - " + WT13.fixtureName() + ": composite ends with ', Medium' (priority chip)");
        runCompositeEndsMedium(WT13, "TC_WT_LIST_133");
    }

    // ═══════════ Block 4 — verified-nav open, alive + not blank (14 × check 4) ═══════════

    @Test(priority = 43)
    public void TC_WT_LIST_004_wt00GeneralOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_004 - " + WT00.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT00, "TC_WT_LIST_004");
    }

    @Test(priority = 44)
    public void TC_WT_LIST_014_wt01AfDataCollectionOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_014 - " + WT01.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT01, "TC_WT_LIST_014");
    }

    @Test(priority = 45)
    public void TC_WT_LIST_024_wt02AfLabelPlacementOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_024 - " + WT02.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT02, "TC_WT_LIST_024");
    }

    @Test(priority = 46)
    public void TC_WT_LIST_034_wt03CleaningOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_034 - " + WT03.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT03, "TC_WT_LIST_034");
    }

    @Test(priority = 47)
    public void TC_WT_LIST_044_wt04CleanTightenTorqueOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_044 - " + WT04.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT04, "TC_WT_LIST_044");
    }

    @Test(priority = 48)
    public void TC_WT_LIST_054_wt05ConditionAssessmentOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_054 - " + WT05.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT05, "TC_WT_LIST_054");
    }

    @Test(priority = 49)
    public void TC_WT_LIST_064_wt06DeEnergizedVisualOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_064 - " + WT06.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT06, "TC_WT_LIST_064");
    }

    @Test(priority = 50)
    public void TC_WT_LIST_074_wt07DgaFluidSampleOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_074 - " + WT07.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT07, "TC_WT_LIST_074");
    }

    @Test(priority = 51)
    public void TC_WT_LIST_084_wt08InfraredThermographyOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_084 - " + WT08.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT08, "TC_WT_LIST_084");
    }

    @Test(priority = 52)
    public void TC_WT_LIST_094_wt09InsulationResistanceOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_094 - " + WT09.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT09, "TC_WT_LIST_094");
    }

    @Test(priority = 53)
    public void TC_WT_LIST_104_wt10NetaTestingOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_104 - " + WT10.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT10, "TC_WT_LIST_104");
    }

    @Test(priority = 54)
    public void TC_WT_LIST_114_wt11PanelScheduleUpdatesOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_114 - " + WT11.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT11, "TC_WT_LIST_114");
    }

    @Test(priority = 55)
    public void TC_WT_LIST_124_wt12ShutdownCompositeOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_124 - " + WT12.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT12, "TC_WT_LIST_124");
    }

    @Test(priority = 56)
    public void TC_WT_LIST_134_wt13UpsMaintenanceOpensVerifiedNav() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_134 - " + WT13.fixtureName() + ": opens with verified nav; screen alive and not blank");
        runOpenVerifiedNav(WT13, "TC_WT_LIST_134");
    }

    // ═══════════ Block 5 — back-nav restores list + row (14 × check 5) ═══════════

    @Test(priority = 57)
    public void TC_WT_LIST_005_wt00GeneralBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_005 - " + WT00.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT00, "TC_WT_LIST_005");
    }

    @Test(priority = 58)
    public void TC_WT_LIST_015_wt01AfDataCollectionBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_015 - " + WT01.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT01, "TC_WT_LIST_015");
    }

    @Test(priority = 59)
    public void TC_WT_LIST_025_wt02AfLabelPlacementBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_025 - " + WT02.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT02, "TC_WT_LIST_025");
    }

    @Test(priority = 60)
    public void TC_WT_LIST_035_wt03CleaningBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_035 - " + WT03.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT03, "TC_WT_LIST_035");
    }

    @Test(priority = 61)
    public void TC_WT_LIST_045_wt04CleanTightenTorqueBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_045 - " + WT04.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT04, "TC_WT_LIST_045");
    }

    @Test(priority = 62)
    public void TC_WT_LIST_055_wt05ConditionAssessmentBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_055 - " + WT05.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT05, "TC_WT_LIST_055");
    }

    @Test(priority = 63)
    public void TC_WT_LIST_065_wt06DeEnergizedVisualBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_065 - " + WT06.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT06, "TC_WT_LIST_065");
    }

    @Test(priority = 64)
    public void TC_WT_LIST_075_wt07DgaFluidSampleBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_075 - " + WT07.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT07, "TC_WT_LIST_075");
    }

    @Test(priority = 65)
    public void TC_WT_LIST_085_wt08InfraredThermographyBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_085 - " + WT08.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT08, "TC_WT_LIST_085");
    }

    @Test(priority = 66)
    public void TC_WT_LIST_095_wt09InsulationResistanceBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_095 - " + WT09.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT09, "TC_WT_LIST_095");
    }

    @Test(priority = 67)
    public void TC_WT_LIST_105_wt10NetaTestingBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_105 - " + WT10.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT10, "TC_WT_LIST_105");
    }

    @Test(priority = 68)
    public void TC_WT_LIST_115_wt11PanelScheduleUpdatesBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_115 - " + WT11.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT11, "TC_WT_LIST_115");
    }

    @Test(priority = 69)
    public void TC_WT_LIST_125_wt12ShutdownCompositeBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_125 - " + WT12.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT12, "TC_WT_LIST_125");
    }

    @Test(priority = 70)
    public void TC_WT_LIST_135_wt13UpsMaintenanceBackRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_135 - " + WT13.fixtureName() + ": back-nav restores list with row still present");
        runBackRestoresList(WT13, "TC_WT_LIST_135");
    }

    // ═══════════ Block 6 — rowPriority(composite) == "Medium" (14 × check 6) ═══════════

    @Test(priority = 71)
    public void TC_WT_LIST_006_wt00GeneralPriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_006 - " + WT00.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT00, "TC_WT_LIST_006");
    }

    @Test(priority = 72)
    public void TC_WT_LIST_016_wt01AfDataCollectionPriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_016 - " + WT01.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT01, "TC_WT_LIST_016");
    }

    @Test(priority = 73)
    public void TC_WT_LIST_026_wt02AfLabelPlacementPriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_026 - " + WT02.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT02, "TC_WT_LIST_026");
    }

    @Test(priority = 74)
    public void TC_WT_LIST_036_wt03CleaningPriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_036 - " + WT03.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT03, "TC_WT_LIST_036");
    }

    @Test(priority = 75)
    public void TC_WT_LIST_046_wt04CleanTightenTorquePriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_046 - " + WT04.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT04, "TC_WT_LIST_046");
    }

    @Test(priority = 76)
    public void TC_WT_LIST_056_wt05ConditionAssessmentPriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_056 - " + WT05.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT05, "TC_WT_LIST_056");
    }

    @Test(priority = 77)
    public void TC_WT_LIST_066_wt06DeEnergizedVisualPriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_066 - " + WT06.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT06, "TC_WT_LIST_066");
    }

    @Test(priority = 78)
    public void TC_WT_LIST_076_wt07DgaFluidSamplePriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_076 - " + WT07.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT07, "TC_WT_LIST_076");
    }

    @Test(priority = 79)
    public void TC_WT_LIST_086_wt08InfraredThermographyPriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_086 - " + WT08.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT08, "TC_WT_LIST_086");
    }

    @Test(priority = 80)
    public void TC_WT_LIST_096_wt09InsulationResistancePriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_096 - " + WT09.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT09, "TC_WT_LIST_096");
    }

    @Test(priority = 81)
    public void TC_WT_LIST_106_wt10NetaTestingPriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_106 - " + WT10.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT10, "TC_WT_LIST_106");
    }

    @Test(priority = 82)
    public void TC_WT_LIST_116_wt11PanelScheduleUpdatesPriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_116 - " + WT11.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT11, "TC_WT_LIST_116");
    }

    @Test(priority = 83)
    public void TC_WT_LIST_126_wt12ShutdownCompositePriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_126 - " + WT12.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT12, "TC_WT_LIST_126");
    }

    @Test(priority = 84)
    public void TC_WT_LIST_136_wt13UpsMaintenancePriorityChipMedium() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_136 - " + WT13.fixtureName() + ": rowPriority(composite) parses to 'Medium'");
        runRowPriorityMedium(WT13, "TC_WT_LIST_136");
    }

    // ═══════════════════ Cross-cutting tests (TC_WT_LIST_201..212) ═══════════════════

    /**
     * TC_WT_LIST_201 — single-visit full sweep sees all 14 fixture rows.
     *
     * ORDER-OF-SCROLL REASONING: we sweep WT00→WT13 in fixture-number order
     * WITHOUT reopening the screen between targets. The 14 fixture names all
     * share the 'QA-WT' prefix, so whatever global ordering the app applies
     * (alphabetical or created-date), the family rows are lexicographically
     * CONTIGUOUS in an alphabetical list and creation-adjacent in a dated one
     * — once the first family row is in the viewport, each subsequent target
     * is at most a few rows away, and scrollWorkOrderListTo's down-first
     * sweep resolves it in O(1) swipes from the previous target's viewport.
     * If the ordering assumption ever breaks, the helper's bounded
     * 15-down/18-up bidirectional sweep still finds the row (or terminates)
     * — the order is an efficiency choice, never a correctness dependency.
     */
    @Test(priority = 85)
    public void TC_WT_LIST_201_fullSweepFindsAll14FixtureRows() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_201 - single-visit full sweep finds all 14 QA-WT fixture rows");
        openListGuarded("TC_WT_LIST_201");
        boolean familyPresent = wo.scrollWorkOrderListTo("QA-WT");
        skipIfPreconditionMissing(() -> familyPresent,
                "TC_WT_LIST_201: no QA-WT row reachable at all — fixture family absent on landed site");
        List<String> missing = new ArrayList<>();
        for (WorkTypeCatalog wt : fixtureOrder()) {
            if (!wo.scrollWorkOrderListTo(wt.fixtureName())) {
                missing.add(wt.fixtureName());
            }
        }
        verifyAppAlive("TC_WT_LIST_201: sweep finished");
        assertTrue(missing.isEmpty(),
                "All 14 QA-WT fixture rows must be found in ONE list visit — missing: " + missing);
        logStepWithScreenshot("TC_WT_LIST_201 verified: 14/14 fixture rows seen in one visit");
    }

    @Test(priority = 86)
    public void TC_WT_LIST_202_duplicateTwoPassProbeWt00General() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_202 - " + WT00.fixtureName() + ": two-pass duplicate probe (composite equality)");
        runDuplicateTwoPass(WT00, WT13, "TC_WT_LIST_202");
    }

    @Test(priority = 87)
    public void TC_WT_LIST_203_duplicateTwoPassProbeWt08InfraredThermography() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_203 - " + WT08.fixtureName() + ": two-pass duplicate probe (composite equality)");
        runDuplicateTwoPass(WT08, WT00, "TC_WT_LIST_203");
    }

    @Test(priority = 88)
    public void TC_WT_LIST_204_duplicateTwoPassProbeWt13UpsMaintenance() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_204 - " + WT13.fixtureName() + ": two-pass duplicate probe (composite equality)");
        runDuplicateTwoPass(WT13, WT00, "TC_WT_LIST_204");
    }

    /**
     * TC_WT_LIST_205 — API-side name uniqueness closes the identical-duplicate
     * gap the UI two-pass probes cannot see: the backend must hold EXACTLY ONE
     * work order per fixture name. JSON is counted with a plain indexOf loop
     * (no JSON library in this project); both '"name": "x"' and '"name":"x"'
     * serializations are counted, and the needle's closing quote makes the
     * match exact-name (no prefix bleed).
     */
    @Test(priority = 89)
    public void TC_WT_LIST_205_apiHoldsExactlyOneWorkOrderPerFixtureName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_205 - backend holds exactly ONE work order per QA-WT fixture name");
        TestDataApi a = requireApi("TC_WT_LIST_205");
        loginAndSelectSite(); // dashboard needed so fixture-ensure can read the landed site
        boolean ensured = ensureFixturesOnLandedSite();
        skipIfPreconditionMissing(() -> ensured,
                "TC_WT_LIST_205: fixture family could not be ensured on the landed site");
        for (WorkTypeCatalog wt : fixtureOrder()) {
            String fixture = wt.fixtureName();
            String json = a.listWorkOrdersJson(fixture);
            assertTrue(json != null && !json.isEmpty(),
                    "listWorkOrdersJson must return a body when searching '" + fixture + "'");
            int hits = countNeedle(json, "\"name\": \"" + fixture + "\"")
                     + countNeedle(json, "\"name\":\"" + fixture + "\"");
            logStep("TC_WT_LIST_205 '" + fixture + "' exact-name hits: " + hits);
            assertEquals(hits, 1,
                    "Backend must hold EXACTLY ONE work order named '" + fixture
                    + "' — duplicates corrupt every list-anatomy contract");
        }
        logStep("TC_WT_LIST_205 verified: 14/14 fixture names unique server-side");
    }

    /** TC_WT_LIST_206 — the list survives a dashboard round-trip with the row byte-identical. */
    @Test(priority = 90)
    public void TC_WT_LIST_206_dashboardRoundTripKeepsRowIntact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_206 - dashboard round-trip keeps " + WT01.fixtureName() + " row intact");
        String fixture = WT01.fixtureName();
        String before = compositeOrSkip(WT01, "TC_WT_LIST_206");
        logStep("TC_WT_LIST_206 composite before round-trip: '" + before + "'");
        wo.goBack(); // list → dashboard
        shortWait();
        siteSelectionPage.clickWorkOrderCard(); // dashboard → list again
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(),
                "Work Orders list must reopen after the dashboard round-trip");
        verifyAppAlive("TC_WT_LIST_206: list reopened");
        verifyNotBlank("Work Orders list after dashboard round-trip (TC_WT_LIST_206)");
        assertTrue(wo.scrollWorkOrderListTo(fixture),
                "Row '" + fixture + "' must still be reachable after the dashboard round-trip");
        String after = wo.getWorkOrderRowComposite(fixture);
        assertEquals(after, before,
                "Row composite must be unchanged across the dashboard round-trip");
        logStepWithScreenshot("TC_WT_LIST_206 verified: round-trip kept '" + after + "'");
    }

    /**
     * TC_WT_LIST_207 — screen identity stays correct after a full 14-fixture
     * scroll sweep (the sweep is the stressor; completeness is TC_WT_LIST_201's
     * contract, so misses here are logged, not asserted).
     */
    @Test(priority = 91)
    public void TC_WT_LIST_207_headerStaysCorrectAfterFullSweep() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_207 - Work Orders screen header still correct after a full 14-fixture sweep");
        openListGuarded("TC_WT_LIST_207");
        boolean familyPresent = wo.scrollWorkOrderListTo("QA-WT");
        skipIfPreconditionMissing(() -> familyPresent,
                "TC_WT_LIST_207: no QA-WT row reachable — fixture family absent on landed site");
        int seen = 0;
        for (WorkTypeCatalog wt : fixtureOrder()) {
            if (wo.scrollWorkOrderListTo(wt.fixtureName())) seen++;
        }
        logStep("TC_WT_LIST_207 sweep saw " + seen + "/14 fixture rows (completeness owned by TC_WT_LIST_201)");
        assertTrue(wo.isWorkOrdersScreenDisplayed(),
                "Work Orders screen markers must still identify the list after the full sweep"
                + " — scroll churn must not navigate away or corrupt the screen");
        verifyAppAlive("TC_WT_LIST_207: sweep finished");
        verifyNotBlank("Work Orders list after full sweep (TC_WT_LIST_207)");
        logStepWithScreenshot("TC_WT_LIST_207 verified: header intact after sweep");
    }

    @Test(priority = 92)
    public void TC_WT_LIST_208_stabilityRecheckWt02AfLabelPlacement() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_208 - " + WT02.fixtureName() + ": composite stable across 2 scroll-churn re-checks");
        runStabilityRecheck(WT02, WT13, "TC_WT_LIST_208");
    }

    @Test(priority = 93)
    public void TC_WT_LIST_209_stabilityRecheckWt05ConditionAssessment() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_209 - " + WT05.fixtureName() + ": composite stable across 2 scroll-churn re-checks");
        runStabilityRecheck(WT05, WT00, "TC_WT_LIST_209");
    }

    @Test(priority = 94)
    public void TC_WT_LIST_210_stabilityRecheckWt10NetaTesting() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_210 - " + WT10.fixtureName() + ": composite stable across 2 scroll-churn re-checks");
        runStabilityRecheck(WT10, WT00, "TC_WT_LIST_210");
    }

    /**
     * TC_WT_LIST_211 — reverse-order sweep (WT13→WT00) in one visit: proves
     * the UP direction of the bidirectional scroll also reaches every family
     * row (the forward sweep 201 predominantly exercises DOWN swipes).
     */
    @Test(priority = 95)
    public void TC_WT_LIST_211_reverseSweepFindsAll14FixtureRows() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_211 - reverse-order sweep (WT13→WT00) finds all 14 fixture rows in one visit");
        openListGuarded("TC_WT_LIST_211");
        boolean familyPresent = wo.scrollWorkOrderListTo("QA-WT");
        skipIfPreconditionMissing(() -> familyPresent,
                "TC_WT_LIST_211: no QA-WT row reachable — fixture family absent on landed site");
        List<WorkTypeCatalog> reversed = new ArrayList<>(fixtureOrder());
        Collections.reverse(reversed);
        List<String> missing = new ArrayList<>();
        for (WorkTypeCatalog wt : reversed) {
            if (!wo.scrollWorkOrderListTo(wt.fixtureName())) {
                missing.add(wt.fixtureName());
            }
        }
        verifyAppAlive("TC_WT_LIST_211: reverse sweep finished");
        assertTrue(missing.isEmpty(),
                "All 14 QA-WT fixture rows must also be reachable sweeping WT13→WT00 — missing: " + missing);
        logStepWithScreenshot("TC_WT_LIST_211 verified: 14/14 rows seen in reverse order");
    }

    /**
     * TC_WT_LIST_212 — one-visit anatomy sweep: every family composite is
     * '<exact fixture name>…, Medium' AND the 14 composites are pairwise
     * distinct. Distinctness catches BEGINSWITH cross-contamination (a prefix
     * accidentally resolving to a sibling's row), which no single-fixture
     * check can see.
     */
    @Test(priority = 96)
    public void TC_WT_LIST_212_allCompositesWellFormedAndDistinctInOneVisit() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_LIST_212 - all 14 fixture composites well-formed ('<name>, Medium') and pairwise distinct");
        openListGuarded("TC_WT_LIST_212");
        boolean familyPresent = wo.scrollWorkOrderListTo("QA-WT");
        skipIfPreconditionMissing(() -> familyPresent,
                "TC_WT_LIST_212: no QA-WT row reachable — fixture family absent on landed site");
        Set<String> composites = new LinkedHashSet<>();
        List<String> problems = new ArrayList<>();
        for (WorkTypeCatalog wt : fixtureOrder()) {
            String fixture = wt.fixtureName();
            if (!wo.scrollWorkOrderListTo(fixture)) {
                problems.add("MISSING: " + fixture);
                continue;
            }
            String composite = wo.getWorkOrderRowComposite(fixture);
            if (composite == null || !composite.startsWith(fixture)) {
                problems.add("BAD-PREFIX: " + fixture + " -> '" + composite + "'");
            } else if (!composite.endsWith(", Medium")) {
                problems.add("BAD-SUFFIX: '" + composite + "'");
            } else {
                composites.add(composite);
            }
        }
        verifyAppAlive("TC_WT_LIST_212: anatomy sweep finished");
        assertTrue(problems.isEmpty(),
                "Every QA-WT row composite must be '<exact name>…, Medium' — problems: " + problems);
        assertEquals(composites.size(), 14,
                "The 14 fixture composites must be pairwise distinct (BEGINSWITH cross-contamination check)"
                + " — got " + composites.size() + ": " + composites);
        logStepWithScreenshot("TC_WT_LIST_212 verified: 14 well-formed, distinct composites");
    }
}
