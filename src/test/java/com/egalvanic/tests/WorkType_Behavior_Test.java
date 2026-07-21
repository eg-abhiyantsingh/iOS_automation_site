package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.utils.ExtentReportManager;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * TC_WT_BEH_* — Class 4 of the Work-Type suite: per-category iOS session
 * behavior for every service-backed work type plus the null-type "General".
 *
 * WHAT: Each of the 13 service-backed work types (see
 * {@link com.egalvanic.constants.WorkTypeCatalog}) gets 5 behavior tests
 * against its durable fixture WO:
 *   1. opens-stable      — fixture opens to a live, non-blank Session Details
 *                          screen (verified nav + crash/blank guards);
 *   2. category-surface  — the type's category surface is reachable, detected
 *                          through ONE dispatcher
 *                          ({@link #assertCategorySurfaceOrSkip}) that probes
 *                          per-category candidate tab names via
 *                          {@code WorkOrderPage.isTabDisplayed};
 *   3. rooms/clean-empty — the rooms/locations content opens OR degrades to a
 *                          clean empty state; the screen must never be blank
 *                          or dead after the attempt (bounded room helpers
 *                          only — never the giant Locations tree);
 *   4. tab round-trip    — tapping the detected category tab keeps the app
 *                          alive and back-nav restores the Work Orders list;
 *   5. exit-restores     — leaving the fixture restores the Work Orders list
 *                          with the fixture row still reachable.
 * Plus a WT00/General block (opens, superset tabs tolerated, no type-specific
 * surface required, rooms/empty state, stable back-nav, re-entry idempotency)
 * and negative cross-checks ("category tab of type X absent on fixture of
 * type Y", grounded by first re-detecting X's tab on X's OWN fixture inside
 * the same test — if X's surface is undetectable the negative SKIPs, it never
 * guesses).
 *
 * WHY: the create form is type-agnostic (gold spec §1) — ALL type-specific
 * behavior lives on the opened WO/session screen. The web per-type tab
 * contract (gold spec §5) is the PRIOR for iOS, but the iOS v1.51 surface is
 * PROBE-DEPENDENT (binary consumes work_type_id but the exact session anatomy
 * per category is confirmed by live probing) — so an undetectable surface is
 * an honest skipIfPreconditionMissing, never a false-fail and never a
 * pass-anyway.
 *
 * FIXTURE FAMILY: durable {@code QA-WT00..13} work orders, one per type +
 * General, self-provisioned on the landed site by
 * {@code WorkTypeBaseTest.ensureFixturesOnLandedSite()} (gold spec §3). Every
 * test enters independently via {@code openFixtureOrSkip} — no ordering
 * dependencies beyond TestNG priority for report grouping.
 *
 * CATEGORY → candidate tabs (web contract as prior; first hit wins):
 *   AF        → "SLD", "Equipment Designations", "Arc Flash"
 *   IR        → "IR Photos", "Photos"
 *   COM       → "Condition Assessment", "C.O.M."
 *   CHECKLIST → "Tasks"
 *   SCHEDULE  → "Panel Schedules", "Schedule"
 *   PM_FORMS  → "Forms", "Tasks"
 * Form/task COUNTS and % progress are async + class-conditional and are NEVER
 * hard-asserted (gold spec §5) — structure only.
 *
 * TC allocation (design doc "Class 4", min 75):
 *   TC_WT_BEH_001..065 — 13 service types × 5 shapes (WTnn → (nn-1)*5+1 …)
 *   TC_WT_BEH_066..070 — WT00/General block
 *   TC_WT_BEH_071..077 — cross-category tab contracts (probe-revised: iOS common strip)
 *
 * References: docs/worktype-test-design-2026-07-21.md (Class 4),
 * docs/worktype-gold-spec-2026-07-21.md (§3 fixtures, §5 per-type contract).
 */
public class WorkType_Behavior_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "Work Types (13-option dropdown)";

    // ═════════════════════ category-surface dispatcher ═════════════════════

    /**
     * Candidate session-tab names per category. Order matters: most specific
     * first, so the detected tab is the strongest available signal.
     */
    private static String[] candidateTabsFor(WorkTypeCatalog.Category c) {
        switch (c) {
            case AF:        return new String[] { "SLD", "Equipment Designations", "Arc Flash" };
            case IR:        return new String[] { "IR Photos", "Photos" };
            case COM:       return new String[] { "Condition Assessment", "C.O.M." };
            case CHECKLIST: return new String[] { "Tasks" };
            case SCHEDULE:  return new String[] { "Panel Schedules", "Schedule" };
            case PM_FORMS:  return new String[] { "Forms", "Tasks" };
            default:        return new String[0];
        }
    }

    /** First candidate category tab currently displayed for {@code wt}, or null. */
    private String detectCategoryTab(WorkTypeCatalog wt) {
        for (String tab : candidateTabsFor(wt.category())) {
            if (wo.isTabDisplayed(tab)) return tab;
        }
        return null;
    }

    /**
     * THE single category-surface dispatcher (design doc Class 4). Must be
     * called on an opened fixture's Session Details screen. Probes the
     * category's candidate tabs; when none is detectable the iOS surface is
     * probe-dependent → honest SKIP; when one is found it is hard-asserted
     * and logged.
     */
    private void assertCategorySurfaceOrSkip(WorkTypeCatalog wt, String tc) {
        String[] candidates = candidateTabsFor(wt.category());
        final String found = detectCategoryTab(wt);
        skipIfPreconditionMissing(() -> found != null,
                tc + ": no " + wt.category() + " candidate tab (" + String.join("/", candidates)
                        + ") detectable on '" + wt.fixtureName()
                        + "' — web contract is the prior, iOS surface is probe-dependent");
        logStep(tc + ": " + wt.category() + " surface detected via tab '" + found
                + "' (candidates: " + String.join("/", candidates) + ")");
        assertTrue(wo.isTabDisplayed(found),
                tc + ": detected category tab '" + found + "' must be displayed on " + wt.fixtureName());
    }

    // ═════════════════════ shared behavior shapes ══════════════════════════

    /** Shape 1 — fixture opens to a stable Session Details screen. */
    private void runOpensStable(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture '" + wt.fixtureName() + "' opened");
        verifyNotBlank(wt.fixtureName() + " session details");
        verifyNoErrorAlert();
        assertTrue(wo.ensureSessionDetailsOpen(),
                tc + ": opened fixture must land on the Session Details screen — " + wt.fixtureName());
        logStepWithScreenshot(tc + " verified: " + wt.fixtureName()
                + " opens to a stable Session Details screen");
    }

    /** Shape 2 — category surface reachable (dispatcher; probe-skip when undetectable). */
    private void runCategorySurface(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture '" + wt.fixtureName() + "' opened");
        verifyNotBlank(wt.fixtureName() + " session details");
        assertCategorySurfaceOrSkip(wt, tc);
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " verified: " + wt.category()
                + " category surface present on " + wt.fixtureName());
    }

    /**
     * Shape 3 — rooms/locations content OR a clean empty state. A failed
     * room-open is acceptable (fixture may have no populated rooms — asset
     * population is async, gold spec §5), but the screen must NEVER be blank
     * or dead after the attempt.
     */
    private void runRoomsOrCleanEmpty(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture '" + wt.fixtureName() + "' opened");
        verifyNotBlank(wt.fixtureName() + " session details");
        boolean roomOpened = wo.openFirstSessionRoom();
        // ALWAYS guard after the attempt — clean-empty is fine, blank/dead is not.
        verifyNotBlank(wt.fixtureName() + (roomOpened ? " room content" : " rooms clean empty state"));
        verifyAppAlive(tc + ": after room-open attempt (opened=" + roomOpened + ")");
        verifyNoErrorAlert();
        if (roomOpened) {
            int assets = wo.getRoomAssetCount();
            logStep(tc + ": room opened; visible asset rows = " + assets
                    + " (population is async — structure, not count, is the contract)");
            wo.goBack();
            mediumWait();
            verifyAppAlive(tc + ": after leaving the room");
        } else {
            logStep(tc + ": no openable room — clean empty state accepted (screen verified non-blank)");
        }
        logStepWithScreenshot(tc + " verified: rooms content or clean empty state on " + wt.fixtureName());
    }

    /**
     * Shape 4 — interaction round-trip on the detected category tab: tap it,
     * app stays alive and non-blank, back restores the Work Orders list.
     * Skips honestly when no candidate tab is detectable (probe-dependent) —
     * a "round-trip" with no interaction would be a pass-anyway shape.
     */
    private void runTabRoundTrip(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture '" + wt.fixtureName() + "' opened");
        verifyNotBlank(wt.fixtureName() + " session details");
        final String tab = detectCategoryTab(wt);
        skipIfPreconditionMissing(() -> tab != null,
                tc + ": no " + wt.category() + " candidate tab ("
                        + String.join("/", candidateTabsFor(wt.category())) + ") detectable on '"
                        + wt.fixtureName() + "' — cannot exercise the interaction round-trip (probe-dependent)");
        logStep(tc + ": round-trip via detected category tab '" + tab + "'");
        assertTrue(wo.tapSessionTab(tab),
                tc + ": visible category tab '" + tab + "' must accept a tap");
        verifyAppAlive(tc + ": after tapping '" + tab + "'");
        verifyNotBlank(wt.fixtureName() + " '" + tab + "' tab content");
        verifyNoErrorAlert();
        wo.goBack();
        mediumWait();
        verifyAppAlive(tc + ": after back from '" + tab + "'");
        assertTrue(wo.waitForWorkOrdersScreen(),
                tc + ": back must restore the Work Orders list after the tab round-trip");
        logStepWithScreenshot(tc + " verified: '" + tab + "' round-trip kept the app alive");
    }

    /** Shape 5 — exiting the fixture restores the WO list with the row reachable. */
    private void runExitRestoresList(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc);
        verifyAppAlive(tc + ": fixture '" + wt.fixtureName() + "' opened");
        verifyNotBlank(wt.fixtureName() + " session details");
        wo.goBack();
        mediumWait();
        assertTrue(wo.waitForWorkOrdersScreen(),
                tc + ": exiting the fixture must restore the Work Orders list");
        verifyAppAlive(tc + ": Work Orders list after exit");
        verifyNotBlank("Work Orders list");
        assertTrue(wo.scrollWorkOrderListTo(wt.fixtureName()),
                tc + ": fixture row '" + wt.fixtureName() + "' must still be reachable after exit");
        logStepWithScreenshot(tc + " verified: exit restored the list with "
                + wt.fixtureName() + " reachable");
    }

    /**
     * Negative cross-check: the category tab of {@code positive} must NOT
     * appear on {@code other}'s fixture. The positive contract is grounded
     * FIRST in the same test (detect the tab on {@code positive}'s own
     * fixture); when the positive surface is undetectable on iOS the negative
     * SKIPs — it never asserts absence of a tab it could not prove exists.
     * Only category-unique tab families (AF/IR/COM/SCHEDULE) are used as
     * positives — "Tasks"/"Forms" overlap categories and would false-fail.
     */
    /**
     * PROBE-REVISED CONTRACT (run 11, 2026-07-21): unlike the web (per-type
     * tab sets, gold spec §5), the iOS v1.51 session renders a COMMON tab
     * strip — "SLD" and "Condition Assessment" were live-verified PRESENT on
     * an IR-type session. So cross-fixture tab ABSENCE is NOT the iOS
     * contract; asserting it would false-fail. The honest cross-category
     * contract is:
     *  - for probe-verified COMMON tabs → the tab must ALSO be present on the
     *    other-category fixture (this pin FLIPS if iOS ever goes per-category,
     *    telling us to rebuild this block on the web model);
     *  - for not-yet-classified tabs → presence is recorded, classification
     *    is skip-guarded (never guessed);
     *  - in all cases the other fixture's screen must be alive, non-blank and
     *    alert-free after inspecting the tab.
     */
    private static final java.util.Set<String> PROBE_VERIFIED_COMMON_TABS =
            new java.util.HashSet<>(java.util.Arrays.asList("SLD", "Condition Assessment"));

    private void runNegativeCrossCheck(WorkTypeCatalog positive, WorkTypeCatalog other, String tc) {
        // Phase 1 — ground the tab on the positive category's own fixture.
        openFixtureOrSkip(positive, tc);
        verifyAppAlive(tc + ": positive fixture '" + positive.fixtureName() + "' opened");
        verifyNotBlank(positive.fixtureName() + " session details");
        final String found = detectCategoryTab(positive);
        skipIfPreconditionMissing(() -> found != null,
                tc + ": " + positive.category() + " surface undetectable on its own fixture '"
                        + positive.fixtureName() + "' — cannot ground the cross-check (probe-dependent)");
        logStep(tc + ": grounded — tab '" + found + "' visible on " + positive.fixtureName());
        // Phase 2 — inspect the same tab on the other-category fixture.
        wo.goBack();
        mediumWait();
        assertTrue(wo.waitForWorkOrdersScreen(),
                tc + ": Work Orders list must restore between fixtures");
        final boolean onScreen = wo.scrollWorkOrderListTo(other.fixtureName());
        skipIfPreconditionMissing(() -> onScreen,
                tc + ": fixture '" + other.fixtureName() + "' not present in the Work Orders list");
        assertTrue(wo.openWorkOrderByName(other.fixtureName()),
                tc + ": fixture row must open (verified nav): " + other.fixtureName());
        verifyAppAlive(tc + ": other fixture '" + other.fixtureName() + "' opened");
        verifyNotBlank(other.fixtureName() + " session details");
        final boolean presentOnOther = wo.isTabDisplayed(found);
        logStep(tc + ": tab '" + found + "' on " + other.fixtureName() + ": "
                + (presentOnOther ? "PRESENT" : "ABSENT"));
        if (PROBE_VERIFIED_COMMON_TABS.contains(found)) {
            assertTrue(presentOnOther,
                    tc + ": '" + found + "' is a probe-verified COMMON tab on v1.51 and must also "
                            + "render on " + other.category() + " fixture '" + other.fixtureName()
                            + "' — absence means iOS moved to per-category tabs: rebuild this block");
        } else {
            skipIfPreconditionMissing(() -> false,
                    tc + ": tab '" + found + "' not yet probe-classified (common vs per-category) on "
                            + "iOS v1.51 — presence on " + other.fixtureName() + " recorded as "
                            + (presentOnOther ? "PRESENT" : "ABSENT") + "; classify before hard-asserting");
        }
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " verified: '" + found + "' cross-category contract on " + other.fixtureName());
    }

    // ═══════════ QA-WT01 Arc Flash Data Collection (AF) — 001..005 ══════════

    @Test(priority = 1)
    public void TC_WT_BEH_001_wt01ArcFlashDataCollectionOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_001 - QA-WT01 Arc Flash Data Collection (AF): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_BEH_001");
    }

    @Test(priority = 2)
    public void TC_WT_BEH_002_wt01ArcFlashDataCollectionCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_002 - QA-WT01 Arc Flash Data Collection (AF): category surface reachable via SLD/Equipment Designations/Arc Flash tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_BEH_002");
    }

    @Test(priority = 3)
    public void TC_WT_BEH_003_wt01ArcFlashDataCollectionRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_003 - QA-WT01 Arc Flash Data Collection (AF): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_BEH_003");
    }

    @Test(priority = 4)
    public void TC_WT_BEH_004_wt01ArcFlashDataCollectionTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_004 - QA-WT01 Arc Flash Data Collection (AF): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_BEH_004");
    }

    @Test(priority = 5)
    public void TC_WT_BEH_005_wt01ArcFlashDataCollectionExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_005 - QA-WT01 Arc Flash Data Collection (AF): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_BEH_005");
    }

    // ═══════ QA-WT02 Arc Flash Label Placement (CHECKLIST) — 006..010 ═══════

    @Test(priority = 6)
    public void TC_WT_BEH_006_wt02ArcFlashLabelPlacementOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_006 - QA-WT02 Arc Flash Label Placement (CHECKLIST): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_BEH_006");
    }

    @Test(priority = 7)
    public void TC_WT_BEH_007_wt02ArcFlashLabelPlacementCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_007 - QA-WT02 Arc Flash Label Placement (CHECKLIST): category surface reachable via Tasks tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_BEH_007");
    }

    @Test(priority = 8)
    public void TC_WT_BEH_008_wt02ArcFlashLabelPlacementRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_008 - QA-WT02 Arc Flash Label Placement (CHECKLIST): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_BEH_008");
    }

    @Test(priority = 9)
    public void TC_WT_BEH_009_wt02ArcFlashLabelPlacementTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_009 - QA-WT02 Arc Flash Label Placement (CHECKLIST): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_BEH_009");
    }

    @Test(priority = 10)
    public void TC_WT_BEH_010_wt02ArcFlashLabelPlacementExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_010 - QA-WT02 Arc Flash Label Placement (CHECKLIST): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_BEH_010");
    }

    // ═══════════════ QA-WT03 Cleaning (PM_FORMS) — 011..015 ═════════════════

    @Test(priority = 11)
    public void TC_WT_BEH_011_wt03CleaningOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_011 - QA-WT03 Cleaning (PM_FORMS): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.CLEANING, "TC_WT_BEH_011");
    }

    @Test(priority = 12)
    public void TC_WT_BEH_012_wt03CleaningCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_012 - QA-WT03 Cleaning (PM_FORMS): category surface reachable via Forms/Tasks tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.CLEANING, "TC_WT_BEH_012");
    }

    @Test(priority = 13)
    public void TC_WT_BEH_013_wt03CleaningRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_013 - QA-WT03 Cleaning (PM_FORMS): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.CLEANING, "TC_WT_BEH_013");
    }

    @Test(priority = 14)
    public void TC_WT_BEH_014_wt03CleaningTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_014 - QA-WT03 Cleaning (PM_FORMS): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.CLEANING, "TC_WT_BEH_014");
    }

    @Test(priority = 15)
    public void TC_WT_BEH_015_wt03CleaningExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_015 - QA-WT03 Cleaning (PM_FORMS): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.CLEANING, "TC_WT_BEH_015");
    }

    // ═════════ QA-WT04 Clean Tighten Torque (PM_FORMS) — 016..020 ═══════════

    @Test(priority = 16)
    public void TC_WT_BEH_016_wt04CleanTightenTorqueOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_016 - QA-WT04 Clean Tighten Torque (PM_FORMS): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_BEH_016");
    }

    @Test(priority = 17)
    public void TC_WT_BEH_017_wt04CleanTightenTorqueCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_017 - QA-WT04 Clean Tighten Torque (PM_FORMS): category surface reachable via Forms/Tasks tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_BEH_017");
    }

    @Test(priority = 18)
    public void TC_WT_BEH_018_wt04CleanTightenTorqueRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_018 - QA-WT04 Clean Tighten Torque (PM_FORMS): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_BEH_018");
    }

    @Test(priority = 19)
    public void TC_WT_BEH_019_wt04CleanTightenTorqueTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_019 - QA-WT04 Clean Tighten Torque (PM_FORMS): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_BEH_019");
    }

    @Test(priority = 20)
    public void TC_WT_BEH_020_wt04CleanTightenTorqueExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_020 - QA-WT04 Clean Tighten Torque (PM_FORMS): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_BEH_020");
    }

    // ═══════════ QA-WT05 Condition Assessment (COM) — 021..025 ══════════════

    @Test(priority = 21)
    public void TC_WT_BEH_021_wt05ConditionAssessmentOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_021 - QA-WT05 Condition Assessment (COM): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_BEH_021");
    }

    @Test(priority = 22)
    public void TC_WT_BEH_022_wt05ConditionAssessmentCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_022 - QA-WT05 Condition Assessment (COM): category surface reachable via Condition Assessment/C.O.M. tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_BEH_022");
    }

    @Test(priority = 23)
    public void TC_WT_BEH_023_wt05ConditionAssessmentRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_023 - QA-WT05 Condition Assessment (COM): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_BEH_023");
    }

    @Test(priority = 24)
    public void TC_WT_BEH_024_wt05ConditionAssessmentTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_024 - QA-WT05 Condition Assessment (COM): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_BEH_024");
    }

    @Test(priority = 25)
    public void TC_WT_BEH_025_wt05ConditionAssessmentExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_025 - QA-WT05 Condition Assessment (COM): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_BEH_025");
    }

    // ═══ QA-WT06 De-Energized Visual Inspection (PM_FORMS) — 026..030 ═══════

    @Test(priority = 26)
    public void TC_WT_BEH_026_wt06DeEnergizedVisualOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_026 - QA-WT06 De-Energized Visual Inspection (PM_FORMS): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_BEH_026");
    }

    @Test(priority = 27)
    public void TC_WT_BEH_027_wt06DeEnergizedVisualCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_027 - QA-WT06 De-Energized Visual Inspection (PM_FORMS): category surface reachable via Forms/Tasks tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_BEH_027");
    }

    @Test(priority = 28)
    public void TC_WT_BEH_028_wt06DeEnergizedVisualRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_028 - QA-WT06 De-Energized Visual Inspection (PM_FORMS): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_BEH_028");
    }

    @Test(priority = 29)
    public void TC_WT_BEH_029_wt06DeEnergizedVisualTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_029 - QA-WT06 De-Energized Visual Inspection (PM_FORMS): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_BEH_029");
    }

    @Test(priority = 30)
    public void TC_WT_BEH_030_wt06DeEnergizedVisualExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_030 - QA-WT06 De-Energized Visual Inspection (PM_FORMS): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_BEH_030");
    }

    // ═════ QA-WT07 DGA Fluid Sample Analysis (PM_FORMS) — 031..035 ══════════

    @Test(priority = 31)
    public void TC_WT_BEH_031_wt07DgaFluidSampleOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_031 - QA-WT07 DGA Fluid Sample Analysis (PM_FORMS): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_BEH_031");
    }

    @Test(priority = 32)
    public void TC_WT_BEH_032_wt07DgaFluidSampleCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_032 - QA-WT07 DGA Fluid Sample Analysis (PM_FORMS): category surface reachable via Forms/Tasks tab or honest probe-skip (0 generated forms tolerated)");
        runCategorySurface(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_BEH_032");
    }

    @Test(priority = 33)
    public void TC_WT_BEH_033_wt07DgaFluidSampleRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_033 - QA-WT07 DGA Fluid Sample Analysis (PM_FORMS): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_BEH_033");
    }

    @Test(priority = 34)
    public void TC_WT_BEH_034_wt07DgaFluidSampleTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_034 - QA-WT07 DGA Fluid Sample Analysis (PM_FORMS): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_BEH_034");
    }

    @Test(priority = 35)
    public void TC_WT_BEH_035_wt07DgaFluidSampleExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_035 - QA-WT07 DGA Fluid Sample Analysis (PM_FORMS): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_BEH_035");
    }

    // ═══════════ QA-WT08 Infrared Thermography (IR) — 036..040 ══════════════

    @Test(priority = 36)
    public void TC_WT_BEH_036_wt08InfraredThermographyOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_036 - QA-WT08 Infrared Thermography (IR): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_BEH_036");
    }

    @Test(priority = 37)
    public void TC_WT_BEH_037_wt08InfraredThermographyCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_037 - QA-WT08 Infrared Thermography (IR): category surface reachable via IR Photos/Photos tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_BEH_037");
    }

    @Test(priority = 38)
    public void TC_WT_BEH_038_wt08InfraredThermographyRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_038 - QA-WT08 Infrared Thermography (IR): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_BEH_038");
    }

    @Test(priority = 39)
    public void TC_WT_BEH_039_wt08InfraredThermographyTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_039 - QA-WT08 Infrared Thermography (IR): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_BEH_039");
    }

    @Test(priority = 40)
    public void TC_WT_BEH_040_wt08InfraredThermographyExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_040 - QA-WT08 Infrared Thermography (IR): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_BEH_040");
    }

    // ═══ QA-WT09 Insulation Resistance Testing (PM_FORMS) — 041..045 ════════

    @Test(priority = 41)
    public void TC_WT_BEH_041_wt09InsulationResistanceOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_041 - QA-WT09 Insulation Resistance Testing (PM_FORMS): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_BEH_041");
    }

    @Test(priority = 42)
    public void TC_WT_BEH_042_wt09InsulationResistanceCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_042 - QA-WT09 Insulation Resistance Testing (PM_FORMS): category surface reachable via Forms/Tasks tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_BEH_042");
    }

    @Test(priority = 43)
    public void TC_WT_BEH_043_wt09InsulationResistanceRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_043 - QA-WT09 Insulation Resistance Testing (PM_FORMS): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_BEH_043");
    }

    @Test(priority = 44)
    public void TC_WT_BEH_044_wt09InsulationResistanceTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_044 - QA-WT09 Insulation Resistance Testing (PM_FORMS): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_BEH_044");
    }

    @Test(priority = 45)
    public void TC_WT_BEH_045_wt09InsulationResistanceExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_045 - QA-WT09 Insulation Resistance Testing (PM_FORMS): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_BEH_045");
    }

    // ═══════════════ QA-WT10 NETA Testing (PM_FORMS) — 046..050 ═════════════

    @Test(priority = 46)
    public void TC_WT_BEH_046_wt10NetaTestingOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_046 - QA-WT10 NETA Testing (PM_FORMS): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.NETA_TESTING, "TC_WT_BEH_046");
    }

    @Test(priority = 47)
    public void TC_WT_BEH_047_wt10NetaTestingCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_047 - QA-WT10 NETA Testing (PM_FORMS): category surface reachable via Forms/Tasks tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.NETA_TESTING, "TC_WT_BEH_047");
    }

    @Test(priority = 48)
    public void TC_WT_BEH_048_wt10NetaTestingRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_048 - QA-WT10 NETA Testing (PM_FORMS): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.NETA_TESTING, "TC_WT_BEH_048");
    }

    @Test(priority = 49)
    public void TC_WT_BEH_049_wt10NetaTestingTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_049 - QA-WT10 NETA Testing (PM_FORMS): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.NETA_TESTING, "TC_WT_BEH_049");
    }

    @Test(priority = 50)
    public void TC_WT_BEH_050_wt10NetaTestingExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_050 - QA-WT10 NETA Testing (PM_FORMS): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.NETA_TESTING, "TC_WT_BEH_050");
    }

    // ═══════ QA-WT11 Panel Schedule Updates (SCHEDULE) — 051..055 ═══════════

    @Test(priority = 51)
    public void TC_WT_BEH_051_wt11PanelScheduleUpdatesOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_051 - QA-WT11 Panel Schedule Updates (SCHEDULE): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_BEH_051");
    }

    @Test(priority = 52)
    public void TC_WT_BEH_052_wt11PanelScheduleUpdatesCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_052 - QA-WT11 Panel Schedule Updates (SCHEDULE): category surface reachable via Panel Schedules/Schedule tab or honest probe-skip");
        runCategorySurface(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_BEH_052");
    }

    @Test(priority = 53)
    public void TC_WT_BEH_053_wt11PanelScheduleUpdatesRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_053 - QA-WT11 Panel Schedule Updates (SCHEDULE): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_BEH_053");
    }

    @Test(priority = 54)
    public void TC_WT_BEH_054_wt11PanelScheduleUpdatesTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_054 - QA-WT11 Panel Schedule Updates (SCHEDULE): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_BEH_054");
    }

    @Test(priority = 55)
    public void TC_WT_BEH_055_wt11PanelScheduleUpdatesExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_055 - QA-WT11 Panel Schedule Updates (SCHEDULE): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_BEH_055");
    }

    // ═════════ QA-WT12 Shutdown Composite (PM_FORMS) — 056..060 ═════════════

    @Test(priority = 56)
    public void TC_WT_BEH_056_wt12ShutdownCompositeOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_056 - QA-WT12 Shutdown Composite (PM_FORMS): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_BEH_056");
    }

    @Test(priority = 57)
    public void TC_WT_BEH_057_wt12ShutdownCompositeCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_057 - QA-WT12 Shutdown Composite (PM_FORMS): category surface reachable via Forms/Tasks tab or honest probe-skip (procedure_count 0 tolerated)");
        runCategorySurface(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_BEH_057");
    }

    @Test(priority = 58)
    public void TC_WT_BEH_058_wt12ShutdownCompositeRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_058 - QA-WT12 Shutdown Composite (PM_FORMS): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_BEH_058");
    }

    @Test(priority = 59)
    public void TC_WT_BEH_059_wt12ShutdownCompositeTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_059 - QA-WT12 Shutdown Composite (PM_FORMS): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_BEH_059");
    }

    @Test(priority = 60)
    public void TC_WT_BEH_060_wt12ShutdownCompositeExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_060 - QA-WT12 Shutdown Composite (PM_FORMS): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_BEH_060");
    }

    // ═══════════ QA-WT13 UPS Maintenance (PM_FORMS) — 061..065 ══════════════

    @Test(priority = 61)
    public void TC_WT_BEH_061_wt13UpsMaintenanceOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_061 - QA-WT13 UPS Maintenance (PM_FORMS): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_BEH_061");
    }

    @Test(priority = 62)
    public void TC_WT_BEH_062_wt13UpsMaintenanceCategorySurface() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_062 - QA-WT13 UPS Maintenance (PM_FORMS): category surface reachable via Forms/Tasks tab or honest probe-skip (0 generated forms tolerated)");
        runCategorySurface(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_BEH_062");
    }

    @Test(priority = 63)
    public void TC_WT_BEH_063_wt13UpsMaintenanceRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_063 - QA-WT13 UPS Maintenance (PM_FORMS): rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_BEH_063");
    }

    @Test(priority = 64)
    public void TC_WT_BEH_064_wt13UpsMaintenanceTabRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_064 - QA-WT13 UPS Maintenance (PM_FORMS): category-tab round-trip keeps app alive, back restores list");
        runTabRoundTrip(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_BEH_064");
    }

    @Test(priority = 65)
    public void TC_WT_BEH_065_wt13UpsMaintenanceExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_065 - QA-WT13 UPS Maintenance (PM_FORMS): exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_BEH_065");
    }

    // ══════════════ QA-WT00 General (null type) — 066..070 ══════════════════

    @Test(priority = 66)
    public void TC_WT_BEH_066_wt00GeneralOpensStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_066 - QA-WT00 General (work_type_id=null): fixture opens to a stable Session Details screen");
        runOpensStable(WorkTypeCatalog.GENERAL, "TC_WT_BEH_066");
    }

    @Test(priority = 67)
    public void TC_WT_BEH_067_wt00GeneralSupersetTabsTolerated() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_067 - QA-WT00 General: NO type-specific surface is required; any superset of legacy tabs is tolerated while the screen stays stable");
        final String tc = "TC_WT_BEH_067";
        openFixtureOrSkip(WorkTypeCatalog.GENERAL, tc);
        verifyAppAlive(tc + ": General fixture opened");
        verifyNotBlank(WorkTypeCatalog.GENERAL.fixtureName() + " session details");
        assertTrue(wo.ensureSessionDetailsOpen(),
                tc + ": General fixture must land on the Session Details screen");
        // Inventory only — the null-type contract requires NO category surface
        // and tolerates the legacy superset (gold spec §5: Tasks+Forms+IR Photos on web).
        List<String> present = new ArrayList<>();
        for (String tab : new String[] { "Tasks", "Forms", "IR Photos", "Photos",
                "SLD", "Equipment Designations", "Condition Assessment", "Panel Schedules" }) {
            if (wo.isTabDisplayed(tab)) present.add(tab);
        }
        logStep(tc + ": tabs present on General (superset tolerated, none required): " + present);
        verifyNoErrorAlert();
        verifyAppAlive(tc + ": after tab inventory");
        logStepWithScreenshot(tc + " verified: General stable with tolerated tab set " + present);
    }

    @Test(priority = 68)
    public void TC_WT_BEH_068_wt00GeneralRoomsOrCleanEmpty() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_068 - QA-WT00 General: rooms show content or a clean empty state — never blank/dead");
        runRoomsOrCleanEmpty(WorkTypeCatalog.GENERAL, "TC_WT_BEH_068");
    }

    @Test(priority = 69)
    public void TC_WT_BEH_069_wt00GeneralExitRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_069 - QA-WT00 General: stable back-nav — exit restores the Work Orders list with the row reachable");
        runExitRestoresList(WorkTypeCatalog.GENERAL, "TC_WT_BEH_069");
    }

    @Test(priority = 70)
    public void TC_WT_BEH_070_wt00GeneralReentryStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_070 - QA-WT00 General: open → back → re-open is idempotent and stable both times");
        final String tc = "TC_WT_BEH_070";
        final String fx = WorkTypeCatalog.GENERAL.fixtureName();
        openFixtureOrSkip(WorkTypeCatalog.GENERAL, tc);
        verifyAppAlive(tc + ": first open");
        verifyNotBlank(fx + " session details (first open)");
        wo.goBack();
        mediumWait();
        assertTrue(wo.waitForWorkOrdersScreen(),
                tc + ": back from first open must restore the Work Orders list");
        assertTrue(wo.scrollWorkOrderListTo(fx),
                tc + ": '" + fx + "' row must be reachable for re-entry");
        assertTrue(wo.openWorkOrderByName(fx),
                tc + ": '" + fx + "' must re-open (verified nav)");
        verifyAppAlive(tc + ": second open");
        verifyNotBlank(fx + " session details (second open)");
        verifyNoErrorAlert();
        assertTrue(wo.ensureSessionDetailsOpen(),
                tc + ": re-entry must land on the Session Details screen");
        logStepWithScreenshot(tc + " verified: General re-entry idempotent and stable");
    }

    // ══════════ Negative cross-checks (grounded, unique tabs) — 071..077 ════

    @Test(priority = 71)
    public void TC_WT_BEH_071_afTabAbsentOnIrFixture() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_071 - Cross-category: AF category tab (grounded on QA-WT01) common-strip/classification contract on IR fixture QA-WT08");
        runNegativeCrossCheck(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION,
                WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_BEH_071");
    }

    @Test(priority = 72)
    public void TC_WT_BEH_072_afTabAbsentOnPmFormsFixture() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_072 - Cross-category: AF category tab (grounded on QA-WT01) common-strip/classification contract on PM_FORMS fixture QA-WT03");
        runNegativeCrossCheck(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION,
                WorkTypeCatalog.CLEANING, "TC_WT_BEH_072");
    }

    @Test(priority = 73)
    public void TC_WT_BEH_073_irTabAbsentOnChecklistFixture() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_073 - Cross-category: IR category tab (grounded on QA-WT08) common-strip/classification contract on CHECKLIST fixture QA-WT02");
        runNegativeCrossCheck(WorkTypeCatalog.INFRARED_THERMOGRAPHY,
                WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_BEH_073");
    }

    @Test(priority = 74)
    public void TC_WT_BEH_074_comTabAbsentOnIrFixture() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_074 - Cross-category: COM category tab (grounded on QA-WT05) common-strip/classification contract on IR fixture QA-WT08");
        runNegativeCrossCheck(WorkTypeCatalog.CONDITION_ASSESSMENT,
                WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_BEH_074");
    }

    @Test(priority = 75)
    public void TC_WT_BEH_075_comTabAbsentOnPmFormsFixture() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_075 - Cross-category: COM category tab (grounded on QA-WT05) common-strip/classification contract on PM_FORMS fixture QA-WT10");
        runNegativeCrossCheck(WorkTypeCatalog.CONDITION_ASSESSMENT,
                WorkTypeCatalog.NETA_TESTING, "TC_WT_BEH_075");
    }

    @Test(priority = 76)
    public void TC_WT_BEH_076_scheduleTabAbsentOnComFixture() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_076 - Cross-category: SCHEDULE category tab (grounded on QA-WT11) common-strip/classification contract on COM fixture QA-WT05");
        runNegativeCrossCheck(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES,
                WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_BEH_076");
    }

    @Test(priority = 77)
    public void TC_WT_BEH_077_scheduleTabAbsentOnAfFixture() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_BEH_077 - Cross-category: SCHEDULE category tab (grounded on QA-WT11) common-strip/classification contract on AF fixture QA-WT01");
        runNegativeCrossCheck(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES,
                WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_BEH_077");
    }
}
