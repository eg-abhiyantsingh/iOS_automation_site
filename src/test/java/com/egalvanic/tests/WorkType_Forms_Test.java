package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.WorkOrderFormsPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * TC_WT_FORM_* — circle-activation + per-asset PM-FORMS execution feature
 * (v1.51, NEW). Anatomy is live-probed (probe runs 12-15, 2026-07-27 +
 * user-verified screenshots same day); companion contract docs:
 * docs/worktype-gold-spec-2026-07-21.md §3b-§3d and the
 * {@link WorkOrderFormsPage} javadoc (the probe-verified anatomy contract).
 *
 * PROBE-VERIFIED FACTS this class asserts as HARD contracts:
 *  - WO list rows are ONE full-width Button '(name), (type label), (priority)';
 *    the right-edge activation CIRCLE raises the 'Start Work Order'/'Cancel'
 *    alert; confirm opens session details (opens on the Details tab).
 *  - ACTIVE row composite gains a ', ACTIVE' suffix + an 'ACTIVE' StaticText
 *    on the list; Start-New composite flips to
 *    '…, End current work order session first' while a session is active.
 *  - Session Assets tab → building/floor tree → '(room), N assets' rows →
 *    'Assets in Room' screen with rows '(asset), (class), (formCount)' where
 *    the trailing digit is the per-asset FORM BADGE and differs BY CLASS
 *    (probe run 14: Switch-1=4, Transformer-1=3).
 *  - Form screen: chip strip ('Clean, Tighten, Torque — Cleaning' /
 *    '— Lubrication' / '— Mechanical Servicing' / 'Torque Record' + 'plus'),
 *    nav Buttons Back/trash/square.and.pencil/checkmark, 'Procedure Steps'
 *    info, 'Result' + 'Value / Notes' table with per-step result dropdowns
 *    (named '—' until set → 'Pass'/'Fail') and one TextField per step.
 *  - A Fail result reveals the failure card ('… — Failure Details' /
 *    'Description of Failure' + Photos).
 *
 * UNPROBED surfaces (skip-guarded, never false-failed, never pass-anyway):
 * result-picker option-sheet anatomy, failure-card internals, chip
 * completed-state, the switch-while-active alert (TC_WT_FORM_009 runs it as
 * DISCOVERY and hard-asserts only invariants that must hold either way).
 *
 * CAMERA BAN: the failure card carries a Photos picker but simulators SIGABRT
 * on any camera tap (known app bug CAM-CRASH-01) — NO photo-capture tests
 * here; noted as future real-device work.
 *
 * Fixture: WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE → "QA-WT04 Clean Tighten
 * Torque" (durable, self-provisioned by WorkTypeBaseTest). WT05/WT13 are used
 * as never-activated control rows.
 */
public class WorkType_Forms_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "Work Types (13-option dropdown)";

    private static final String WT04 = WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE.fixtureName();
    private static final String WT05 = WorkTypeCatalog.CONDITION_ASSESSMENT.fixtureName();
    private static final String WT13 = WorkTypeCatalog.UPS_MAINTENANCE.fixtureName();
    private static final String WT04_TYPE_LABEL = WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE.displayName();

    private static final String SWITCH_ASSET = "Switch-1";
    private static final String TRANSFORMER_ASSET = "Transformer-1";

    private WorkOrderFormsPage forms;

    @BeforeMethod(alwaysRun = true)
    public void initFormsPage() {
        if (!DriverManager.isDriverActive()) return;
        try {
            forms = new WorkOrderFormsPage();
        } catch (IllegalStateException e) {
            DriverManager.initDriver();
            forms = new WorkOrderFormsPage();
        }
    }

    // ════════════════════════════ helpers ═══════════════════════════════════

    /** Fresh nav: login (idempotent) → dashboard → Work Orders list. */
    private void navToListFresh() {
        openWorkOrdersScreenWT();
    }

    /** Scroll the fixture row on screen, SKIP (env precondition) when absent. */
    private void bringRowOnScreenOrSkip(String rowName, String tcId) {
        boolean onScreen = wo.scrollWorkOrderListTo(rowName);
        skipIfPreconditionMissing(() -> onScreen,
                tcId + ": fixture row '" + rowName + "' not present in the Work Orders list");
    }

    /** Back-nav to the WO list with a full re-nav as recovery. */
    private void backToListBestEffort() {
        wo.goBack();
        shortWait();
        if (!wo.isWorkOrdersScreenDisplayed()) {
            navToListFresh();
        }
    }

    /**
     * Ensure WT04 owns the ACTIVE session and finish standing on the WO list
     * with the WT04 row on screen. Activation itself is HARD-asserted (it is
     * the probe-verified core interaction); only fixture presence skips.
     */
    private void ensureWT04ActiveOnList(String tcId) {
        navToListFresh();
        bringRowOnScreenOrSkip(WT04, tcId);
        if (forms.isRowActive(WT04)) return;
        assertTrue(wo.openWorkOrderByName(WT04),
                tcId + ": WT04 fixture must activate via the Start dance (verified session-details open)");
        verifyAppAlive(tcId + " after WT04 activation");
        backToListBestEffort();
        bringRowOnScreenOrSkip(WT04, tcId);
    }

    /** Open the WT04 session and land on its Assets tab (skip-guarded setup nav). */
    private void openSessionAssetsTabOrSkip(String tcId) {
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(),
                tcId + ": session details did not settle open");
        skipIfPreconditionMissing(() -> wo.tapSessionTab("Assets"),
                tcId + ": 'Assets' session tab not tappable");
        mediumWait();
    }

    /** Session → Assets tab → first room with assets → 'Assets in Room' (skip-guarded setup nav). */
    private void openRoomOrSkip(String tcId) {
        openSessionAssetsTabOrSkip(tcId);
        skipIfPreconditionMissing(() -> forms.openFirstRoomWithAssetsInTree(),
                tcId + ": no '<room>, N assets' row reachable in the session tree");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.isAssetsInRoomOpen(),
                tcId + ": 'Assets in Room' screen did not open");
    }

    /** Full setup nav to the Switch-1 form screen (skip-guarded at every stage). */
    private void openSwitch1FormsOrSkip(String tcId) {
        openRoomOrSkip(tcId);
        skipIfPreconditionMissing(() -> forms.rowComposite(SWITCH_ASSET) != null,
                tcId + ": asset '" + SWITCH_ASSET + "' not visible in the room");
        skipIfPreconditionMissing(() -> forms.openAssetForms(SWITCH_ASSET),
                tcId + ": form screen did not open for " + SWITCH_ASSET);
        verifyNotBlank("form screen (" + tcId + ")");
    }

    /** Best-effort: focus the probed 3-step Cleaning form chip before result/notes work. */
    private void selectCleaningChipBestEffort() {
        List<String> chips = forms.getFormChipNames();
        for (String c : chips) {
            if (c.contains("Cleaning")) {
                forms.selectFormChip("Cleaning");
                mediumWait();
                break;
            }
        }
    }

    /**
     * Set a step result. The option-sheet anatomy is probe-DEPENDENT
     * (unprobed) → a false return SKIPs honestly; a true return is followed by
     * a HARD readback assert (page contract: '—' → 'Pass'/'Fail').
     */
    private void setStepResultOrSkip(int idx, String value, String tcId) {
        skipIfPreconditionMissing(() -> forms.stepCount() > 0,
                tcId + ": no step-result rows readable on the form");
        boolean set = forms.setStepResult(idx, value);
        skipIfPreconditionMissing(() -> set,
                tcId + ": result-picker option surface (probe-dependent anatomy) did not yield '" + value + "'");
        assertEquals(forms.stepResult(idx), value,
                tcId + ": step " + idx + " Result dropdown must read back '" + value + "' after selection");
    }

    // ═══════════════ A. Activation contract (TC_WT_FORM_001-012) ════════════

    @Test(priority = 1)
    public void TC_WT_FORM_001_circleTapRaisesStartAlertAndCancelKeepsRowInactive() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_001 - Circle tap on a WO row raises the Start alert; Cancel keeps the row inactive");
        navToListFresh();
        skipIfPreconditionMissing(() -> !forms.isStartNewBlockedByActiveSession(),
                "TC_WT_FORM_001: a session is already ACTIVE — free-list circle-tap contract not testable (switch path is unprobed)");
        bringRowOnScreenOrSkip(WT05, "TC_WT_FORM_001");
        boolean alertUp = forms.tapCircleExpectAlert(WT05);
        if (!alertUp) forms.cancelStartAlert(); // restores auto-accept even on the failure path
        assertTrue(alertUp,
                "Circle tap on the row's right edge must raise the 'Start Work Order' alert (probe run 12)");
        assertTrue(forms.cancelStartAlert(), "Cancel on the Start alert must be tappable");
        shortWait();
        assertFalse(forms.isRowActive(WT05),
                "Row must NOT be ACTIVE after cancelling the Start alert");
        verifyAppAlive("TC_WT_FORM_001 post-cancel");
        verifyNoErrorAlert();
        logStepWithScreenshot("TC_WT_FORM_001 verified: alert raised and cancelled cleanly");
    }

    @Test(priority = 2)
    public void TC_WT_FORM_002_cancelKeepsStartNewCompositeUnchanged() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_002 - Cancelling the Start alert leaves the Start-New button composite unchanged");
        navToListFresh();
        skipIfPreconditionMissing(() -> !forms.isStartNewBlockedByActiveSession(),
                "TC_WT_FORM_002: a session is already ACTIVE — free-list contract not testable");
        String before = forms.getStartNewComposite();
        skipIfPreconditionMissing(() -> before != null,
                "TC_WT_FORM_002: Start-New button composite unreadable on this list");
        bringRowOnScreenOrSkip(WT05, "TC_WT_FORM_002");
        boolean alertUp = forms.tapCircleExpectAlert(WT05);
        if (!alertUp) forms.cancelStartAlert();
        assertTrue(alertUp, "Circle tap must raise the Start alert (probe run 12)");
        assertTrue(forms.cancelStartAlert(), "Cancel must be tappable on the Start alert");
        shortWait();
        String after = forms.getStartNewComposite();
        logStep("Start-New composite before='" + before + "' after='" + after + "'");
        assertEquals(after, before,
                "Cancelling the Start alert must leave the Start-New composite unchanged (still session-free)");
        verifyAppAlive("TC_WT_FORM_002 post-cancel");
        logStepWithScreenshot("TC_WT_FORM_002 verified");
    }

    @Test(priority = 3)
    public void TC_WT_FORM_003_activateWT04OpensSessionDetails() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_003 - Activating QA-WT04 via the Start dance opens verified session details");
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_FORM_003");
        assertTrue(wo.isSessionDetailsScreenDisplayed(),
                "Verified-open: session details must be displayed after activation (gold spec §3d)");
        logStep("Session header: '" + wo.getSessionDetailsHeaderText() + "'");
        verifyNotBlank("session details (TC_WT_FORM_003)");
        verifyAppAlive("TC_WT_FORM_003");
        logStepWithScreenshot("TC_WT_FORM_003 verified: WT04 session open");
    }

    @Test(priority = 4)
    public void TC_WT_FORM_004_activeRowCompositeEndsWithActiveSuffix() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_004 - After back-nav the WT04 row composite carries the ', ACTIVE' suffix");
        ensureWT04ActiveOnList("TC_WT_FORM_004");
        String composite = forms.rowComposite(WT04);
        logStep("WT04 row composite: '" + composite + "'");
        assertTrue(composite != null, "WT04 row composite must be readable on the list");
        assertTrue(forms.isRowActive(WT04),
                "WT04 composite must end ', ACTIVE' while its session is running (probe run 13)");
        verifyAppAlive("TC_WT_FORM_004");
        logStepWithScreenshot("TC_WT_FORM_004 verified");
    }

    @Test(priority = 5)
    public void TC_WT_FORM_005_exactlyOneActiveBadgeVisible() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_005 - Exactly one ACTIVE StaticText badge is visible on the list (radio invariant)");
        ensureWT04ActiveOnList("TC_WT_FORM_005");
        skipIfPreconditionMissing(() -> forms.isRowActive(WT04),
                "TC_WT_FORM_005: WT04 row not on screen in ACTIVE state — badge zone off-screen");
        int badges = forms.visibleActiveBadgeCount();
        logStep("Visible ACTIVE badges: " + badges);
        skipIfPreconditionMissing(() -> badges >= 0,
                "TC_WT_FORM_005: ACTIVE badge query failed (WDA error, not a product signal)");
        assertEquals(badges, 1,
                "Exactly ONE 'ACTIVE' StaticText badge must be visible while WT04's row is on screen");
        verifyAppAlive("TC_WT_FORM_005");
        logStepWithScreenshot("TC_WT_FORM_005 verified");
    }

    @Test(priority = 6)
    public void TC_WT_FORM_006_startNewBlockedWhileSessionActive() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_006 - Start-New composite demands ending the current session while one is active");
        ensureWT04ActiveOnList("TC_WT_FORM_006");
        logStep("Start-New composite: '" + forms.getStartNewComposite() + "'");
        assertTrue(forms.isStartNewBlockedByActiveSession(),
                "Start-New must read '…End current work order session first' while WT04 is ACTIVE (probe run 13)");
        verifyAppAlive("TC_WT_FORM_006");
        logStepWithScreenshot("TC_WT_FORM_006 verified");
    }

    @Test(priority = 7)
    public void TC_WT_FORM_007_reopenActiveRowIsDirectOpen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_007 - Re-opening the ACTIVE WT04 row direct-opens the running session");
        ensureWT04ActiveOnList("TC_WT_FORM_007");
        assertTrue(wo.openWorkOrderByName(WT04),
                "ACTIVE row must re-open directly into the session (verified session-details open)");
        verifyAppAlive("TC_WT_FORM_007 after re-open");
        verifyNotBlank("re-opened session (TC_WT_FORM_007)");
        logStepWithScreenshot("TC_WT_FORM_007 verified");
    }

    @Test(priority = 8)
    public void TC_WT_FORM_008_sessionHeaderEqualsFixtureNameOnReopen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_008 - Session-details header equals the fixture WO name on re-open");
        ensureWT04ActiveOnList("TC_WT_FORM_008");
        assertTrue(wo.openWorkOrderByName(WT04),
                "ACTIVE WT04 row must re-open (verified session details)");
        String header = wo.getSessionDetailsHeaderText();
        logStep("Session header on re-open: '" + header + "'");
        assertEquals(header, WT04,
                "Session-details header must equal the fixture WO name (gold spec §3d)");
        verifyAppAlive("TC_WT_FORM_008");
        logStepWithScreenshot("TC_WT_FORM_008 verified");
    }

    @Test(priority = 9)
    public void TC_WT_FORM_009_circleTapOtherRowWhileActive_discovery() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_009 - DISCOVERY: circle-tap on WT05 while WT04 is ACTIVE (switch path is unprobed)");
        ensureWT04ActiveOnList("TC_WT_FORM_009");
        bringRowOnScreenOrSkip(WT05, "TC_WT_FORM_009");
        boolean alertUp = forms.tapCircleExpectAlert(WT05);
        logStep("DISCOVERY (unprobed switch-while-active): Start alert appeared = " + alertUp);
        boolean cancelled = forms.cancelStartAlert(); // always: restores auto-accept, cancels when present
        if (alertUp) {
            assertTrue(cancelled, "The switch-while-active alert must be cancellable");
        }
        shortWait();
        verifyAppAlive("TC_WT_FORM_009 after switch attempt");
        if (!wo.isWorkOrdersScreenDisplayed()) {
            wo.goBack();
            shortWait();
        }
        assertTrue(wo.isWorkOrdersScreenDisplayed(),
                "WO list must be restored after the cancelled switch-while-active attempt");
        bringRowOnScreenOrSkip(WT04, "TC_WT_FORM_009");
        assertTrue(forms.isRowActive(WT04),
                "WT04 must STILL be the active row after the cancelled switch attempt");
        logStepWithScreenshot("TC_WT_FORM_009 discovery recorded (alert=" + alertUp + ")");
    }

    @Test(priority = 10)
    public void TC_WT_FORM_010_activeBadgeSurvivesDashboardRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_010 - ACTIVE state survives a dashboard round-trip back to the WO list");
        ensureWT04ActiveOnList("TC_WT_FORM_010");
        assertTrue(forms.isRowActive(WT04), "Pre-round-trip: WT04 must be ACTIVE");
        navToListFresh(); // dashboard → Work Orders again
        bringRowOnScreenOrSkip(WT04, "TC_WT_FORM_010");
        assertTrue(forms.isRowActive(WT04),
                "ACTIVE badge must survive the dashboard round-trip (session persistence)");
        verifyAppAlive("TC_WT_FORM_010");
        logStepWithScreenshot("TC_WT_FORM_010 verified");
    }

    @Test(priority = 11)
    public void TC_WT_FORM_011_activeCompositeKeepsNameAndTypeLabel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_011 - ACTIVE row composite still begins with the WO name and carries the type label");
        ensureWT04ActiveOnList("TC_WT_FORM_011");
        String composite = forms.rowComposite(WT04);
        logStep("WT04 ACTIVE composite: '" + composite + "'");
        assertTrue(composite != null, "WT04 row composite must be readable");
        assertTrue(composite != null && composite.startsWith(WT04),
                "ACTIVE composite must still BEGINSWITH the fixture name (gold spec §3c parse contract)");
        assertTrue(composite != null && composite.contains(WT04_TYPE_LABEL),
                "ACTIVE composite must still contain the work-type label segment '" + WT04_TYPE_LABEL + "'");
        verifyAppAlive("TC_WT_FORM_011");
        logStepWithScreenshot("TC_WT_FORM_011 verified");
    }

    @Test(priority = 12)
    public void TC_WT_FORM_012_neverActivatedRowIsNotActive() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_012 - A never-activated fixture row (WT13) carries no ACTIVE suffix");
        navToListFresh();
        bringRowOnScreenOrSkip(WT13, "TC_WT_FORM_012");
        logStep("WT13 composite: '" + forms.rowComposite(WT13) + "'");
        assertFalse(forms.isRowActive(WT13),
                "Never-activated fixture WT13 must NOT read as ACTIVE");
        verifyAppAlive("TC_WT_FORM_012");
        logStepWithScreenshot("TC_WT_FORM_012 verified");
    }

    // ═══════════════ B. Session tree → room (TC_WT_FORM_013-020) ════════════

    @Test(priority = 13)
    public void TC_WT_FORM_013_sessionOpensOnDetailsTab() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_013 - The activated session opens on the Details tab");
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_FORM_013");
        assertTrue(wo.ensureSessionDetailsOpen(),
                "Session must open (and settle) on the Details tab (probe run 14)");
        verifyNotBlank("session Details tab (TC_WT_FORM_013)");
        verifyAppAlive("TC_WT_FORM_013");
        logStepWithScreenshot("TC_WT_FORM_013 verified");
    }

    @Test(priority = 14)
    public void TC_WT_FORM_014_assetsTabTapSucceeds() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_014 - The Assets session tab is tappable from Details");
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_FORM_014");
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(),
                "TC_WT_FORM_014: session details did not settle open");
        assertTrue(wo.tapSessionTab("Assets"),
                "The 'Assets' session tab must be tappable (common tab strip, gold spec §3d)");
        mediumWait();
        verifyAppAlive("TC_WT_FORM_014 after Assets tab");
        verifyNotBlank("session Assets tab (TC_WT_FORM_014)");
        logStepWithScreenshot("TC_WT_FORM_014 verified");
    }

    @Test(priority = 15)
    public void TC_WT_FORM_015_treeOpensFirstRoomWithAssets() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_015 - The session tree exposes and opens a '<room>, N assets' row");
        openSessionAssetsTabOrSkip("TC_WT_FORM_015");
        assertTrue(forms.openFirstRoomWithAssetsInTree(),
                "Session tree must expose a '<room>, N assets' row and open it (probe run 14: "
                        + "'Optional Notes Room_21, 10 assets' under 'Floor 1 - Ground_416')");
        mediumWait();
        verifyAppAlive("TC_WT_FORM_015 after room open");
        verifyNotBlank("room screen (TC_WT_FORM_015)");
        logStepWithScreenshot("TC_WT_FORM_015 verified");
    }

    @Test(priority = 16)
    public void TC_WT_FORM_016_assetsInRoomScreenOpens() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_016 - Opening a room lands on the 'Assets in Room' screen");
        openSessionAssetsTabOrSkip("TC_WT_FORM_016");
        skipIfPreconditionMissing(() -> forms.openFirstRoomWithAssetsInTree(),
                "TC_WT_FORM_016: no room with assets reachable in the session tree");
        mediumWait();
        assertTrue(forms.isAssetsInRoomOpen(),
                "Nav bar named 'Assets in Room' must be present after opening a room (probe run 14)");
        verifyAppAlive("TC_WT_FORM_016");
        logStepWithScreenshot("TC_WT_FORM_016 verified");
    }

    @Test(priority = 17)
    public void TC_WT_FORM_017_assetRowCompositesMatchBadgeShape() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_017 - Asset rows follow the '<name>, <class>, <formCount>' composite shape");
        openRoomOrSkip("TC_WT_FORM_017");
        List<String> rows = forms.visibleAssetRowComposites();
        logStep("Asset rows: " + rows);
        assertTrue(!rows.isEmpty(), "At least one asset row must be visible in the room");
        for (String row : rows) {
            assertTrue(row.matches(".+, .+, \\d+"),
                    "Asset row '" + row + "' must match '<name>, <class>, <formCount>' (probe run 14)");
        }
        verifyAppAlive("TC_WT_FORM_017");
        logStepWithScreenshot("TC_WT_FORM_017 verified (" + rows.size() + " rows)");
    }

    @Test(priority = 18)
    public void TC_WT_FORM_018_switch1FormBadgeIsPositive() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_018 - Switch-1 carries a positive per-asset form badge");
        openRoomOrSkip("TC_WT_FORM_018");
        String comp = forms.rowComposite(SWITCH_ASSET);
        skipIfPreconditionMissing(() -> comp != null,
                "TC_WT_FORM_018: asset '" + SWITCH_ASSET + "' not visible in this room");
        int badge = forms.assetFormBadge(SWITCH_ASSET);
        logStep("Switch-1 badge=" + badge + " (row: '" + comp + "')");
        assertTrue(badge > 0,
                "Switch-1 must carry a positive form badge in the CTT session (probe run 14: 4) — got " + badge);
        verifyAppAlive("TC_WT_FORM_018");
        logStepWithScreenshot("TC_WT_FORM_018 verified");
    }

    @Test(priority = 19)
    public void TC_WT_FORM_019_badgesPerClass_discovery() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_019 - DISCOVERY: per-class form badges (Switch-1 vs Transformer-1), both positive");
        openRoomOrSkip("TC_WT_FORM_019");
        skipIfPreconditionMissing(
                () -> forms.rowComposite(SWITCH_ASSET) != null && forms.rowComposite(TRANSFORMER_ASSET) != null,
                "TC_WT_FORM_019: Switch-1 and Transformer-1 not both visible in this room");
        int sw = forms.assetFormBadge(SWITCH_ASSET);
        int tr = forms.assetFormBadge(TRANSFORMER_ASSET);
        logStep("DISCOVERY per-class badges: Switch-1=" + sw + ", Transformer-1=" + tr
                + " — differ: " + (sw != tr) + " (probe run 14 observed 4 vs 3)");
        assertTrue(sw > 0, "Switch-1 form badge must be positive — got " + sw);
        assertTrue(tr > 0, "Transformer-1 form badge must be positive — got " + tr);
        verifyAppAlive("TC_WT_FORM_019");
        logStepWithScreenshot("TC_WT_FORM_019 recorded");
    }

    @Test(priority = 20)
    public void TC_WT_FORM_020_backFromRoomReturnsToTree() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_020 - Back from the room returns to the session tree without wreckage");
        openRoomOrSkip("TC_WT_FORM_020");
        wo.goBack();
        mediumWait();
        verifyAppAlive("TC_WT_FORM_020 after back from room");
        verifyNotBlank("session tree after back (TC_WT_FORM_020)");
        assertFalse(forms.isAssetsInRoomOpen(),
                "'Assets in Room' must be closed after back-nav from the room");
        logStepWithScreenshot("TC_WT_FORM_020 verified");
    }

    // ═══════════════ C. Form anatomy (TC_WT_FORM_021-030) ═══════════════════

    @Test(priority = 21)
    public void TC_WT_FORM_021_assetTapOpensFormScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_021 - Tapping an asset row opens the per-asset form screen");
        openRoomOrSkip("TC_WT_FORM_021");
        skipIfPreconditionMissing(() -> forms.rowComposite(SWITCH_ASSET) != null,
                "TC_WT_FORM_021: asset '" + SWITCH_ASSET + "' not visible in the room");
        assertTrue(forms.openAssetForms(SWITCH_ASSET),
                "Form screen must open on asset tap (chip strip / 'Procedure Steps' signature, probe run 15)");
        assertTrue(forms.isFormScreenOpen(), "Form-screen signature must be detectable after open");
        verifyNotBlank("form screen (TC_WT_FORM_021)");
        verifyAppAlive("TC_WT_FORM_021");
        logStepWithScreenshot("TC_WT_FORM_021 verified");
    }

    @Test(priority = 22)
    public void TC_WT_FORM_022_chipNamesFollowWorkTypeProcedurePattern() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_022 - Every form chip is '<Work Type> — <Procedure>' or a known record name");
        openSwitch1FormsOrSkip("TC_WT_FORM_022");
        List<String> chips = forms.getFormChipNames();
        logStep("Form chips: " + chips);
        assertTrue(!chips.isEmpty(), "Chip strip must list at least one form instance");
        for (String chip : chips) {
            // ' — ' = em-dash separator (probe run 15); 'Torque Record' is a known
            // separator-less record chip; a bare '—' is a fresh unfilled instance
            // chip (page-object contract).
            boolean shapeOk = chip.contains(" — ") || chip.equals("Torque Record") || chip.equals("—");
            assertTrue(shapeOk,
                    "Chip '" + chip + "' must contain ' — ' or be a known record/unfilled chip name");
        }
        verifyAppAlive("TC_WT_FORM_022");
        logStepWithScreenshot("TC_WT_FORM_022 verified (" + chips.size() + " chips)");
    }

    @Test(priority = 23)
    public void TC_WT_FORM_023_chipCountEqualsListBadge_invariant() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_023 - INVARIANT: form-chip count equals the asset's list badge (captured pre-open)");
        openRoomOrSkip("TC_WT_FORM_023");
        String comp = forms.rowComposite(SWITCH_ASSET);
        skipIfPreconditionMissing(() -> comp != null,
                "TC_WT_FORM_023: asset '" + SWITCH_ASSET + "' not visible in the room");
        int badge = forms.assetFormBadge(SWITCH_ASSET); // capture BEFORE opening
        skipIfPreconditionMissing(() -> badge >= 0,
                "TC_WT_FORM_023: badge unparsable from row '" + comp + "'");
        skipIfPreconditionMissing(() -> forms.openAssetForms(SWITCH_ASSET),
                "TC_WT_FORM_023: form screen did not open for " + SWITCH_ASSET);
        List<String> chips = forms.getFormChipNames();
        logStep("badge=" + badge + " chips=" + chips);
        assertEquals(chips.size(), badge,
                "Form-chip count must equal the per-asset list badge (badge↔chips invariant, probe run 15)");
        verifyAppAlive("TC_WT_FORM_023");
        logStepWithScreenshot("TC_WT_FORM_023 verified");
    }

    @Test(priority = 24)
    public void TC_WT_FORM_024_procedureStepsInfoPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_024 - The 'Procedure Steps' info renders on the form screen");
        openSwitch1FormsOrSkip("TC_WT_FORM_024");
        assertTrue(forms.isFormScreenOpen(),
                "Form-screen signature ('Procedure Steps' StaticText or chip strip) must be present");
        assertTrue(!forms.getFormChipNames().isEmpty(),
                "Chip strip must be populated alongside the Procedure Steps info (probe run 15)");
        verifyAppAlive("TC_WT_FORM_024");
        logStepWithScreenshot("TC_WT_FORM_024 verified");
    }

    @Test(priority = 25)
    public void TC_WT_FORM_025_navControlsPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_025 - Nav controls Back / trash / square.and.pencil / checkmark all present");
        openSwitch1FormsOrSkip("TC_WT_FORM_025");
        for (String control : new String[] { "Back", "trash", "square.and.pencil", "checkmark" }) {
            assertTrue(forms.isFormControlPresent(control),
                    "Nav control '" + control + "' must be present on the form screen (probe run 15)");
        }
        verifyAppAlive("TC_WT_FORM_025");
        logStepWithScreenshot("TC_WT_FORM_025 verified (4 controls)");
    }

    @Test(priority = 26)
    public void TC_WT_FORM_026_plusControlPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_026 - The 'plus' (add form instance) control is present in the chip strip");
        openSwitch1FormsOrSkip("TC_WT_FORM_026");
        assertTrue(forms.isFormControlPresent("plus"),
                "The 'plus' add-instance Button must be present next to the chips (probe run 15)");
        verifyAppAlive("TC_WT_FORM_026");
        logStepWithScreenshot("TC_WT_FORM_026 verified");
    }

    @Test(priority = 27)
    public void TC_WT_FORM_027_stepRowsPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_027 - The form table exposes at least one step-result row");
        openSwitch1FormsOrSkip("TC_WT_FORM_027");
        selectCleaningChipBestEffort();
        int steps = forms.stepCount();
        logStep("Step-result rows: " + steps);
        assertTrue(steps > 0,
                "The form must expose step Result dropdowns (probe run 15: 3 on the Cleaning form) — got " + steps);
        verifyAppAlive("TC_WT_FORM_027");
        logStepWithScreenshot("TC_WT_FORM_027 verified");
    }

    @Test(priority = 28)
    public void TC_WT_FORM_028_oneNotesFieldPerStep() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_028 - One Value/Notes TextField per step-result row");
        openSwitch1FormsOrSkip("TC_WT_FORM_028");
        selectCleaningChipBestEffort();
        int steps = forms.stepCount();
        int notes = forms.noteFields().size();
        logStep("steps=" + steps + " noteFields=" + notes);
        skipIfPreconditionMissing(() -> steps > 0 && notes > 0,
                "TC_WT_FORM_028: no step/notes rows readable on this form");
        assertEquals(notes, steps,
                "Each step row must pair one Result dropdown with one Value/Notes TextField (probe run 15)");
        verifyAppAlive("TC_WT_FORM_028");
        logStepWithScreenshot("TC_WT_FORM_028 verified");
    }

    @Test(priority = 29)
    public void TC_WT_FORM_029_chipSwitchToLubrication() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_029 - Switching to the Lubrication chip keeps the form screen stable");
        openSwitch1FormsOrSkip("TC_WT_FORM_029");
        List<String> chips = forms.getFormChipNames();
        skipIfPreconditionMissing(() -> chips.stream().anyMatch(c -> c.contains("Lubrication")),
                "TC_WT_FORM_029: no 'Lubrication' chip on this asset's form — chips: " + chips);
        assertTrue(forms.selectFormChip("Lubrication"),
                "The Lubrication chip must be tappable in the chip strip");
        mediumWait();
        verifyAppAlive("TC_WT_FORM_029 after chip switch");
        assertTrue(forms.isFormScreenOpen(),
                "Form screen must remain open after switching to the Lubrication chip");
        logStepWithScreenshot("TC_WT_FORM_029 verified");
    }

    @Test(priority = 30)
    public void TC_WT_FORM_030_chipSwitchBackToCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_030 - Switching back to the Cleaning chip works after Lubrication");
        openSwitch1FormsOrSkip("TC_WT_FORM_030");
        List<String> chips = forms.getFormChipNames();
        skipIfPreconditionMissing(
                () -> chips.stream().anyMatch(c -> c.contains("Lubrication"))
                        && chips.stream().anyMatch(c -> c.contains("Cleaning")),
                "TC_WT_FORM_030: Lubrication + Cleaning chips not both present — chips: " + chips);
        skipIfPreconditionMissing(() -> forms.selectFormChip("Lubrication"),
                "TC_WT_FORM_030: could not move off the Cleaning chip first");
        mediumWait();
        assertTrue(forms.selectFormChip("Cleaning"),
                "The Cleaning chip must be tappable again after visiting Lubrication");
        mediumWait();
        verifyAppAlive("TC_WT_FORM_030 after chip round-trip");
        assertTrue(forms.isFormScreenOpen(),
                "Form screen must remain open after the chip round-trip");
        logStepWithScreenshot("TC_WT_FORM_030 verified");
    }

    // ═══════════════ D. Form filling (TC_WT_FORM_031-040) ═══════════════════

    @Test(priority = 31)
    public void TC_WT_FORM_031_setResultPassReadsBack() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_031 - Setting step-0 Result to Pass reads back 'Pass'");
        openSwitch1FormsOrSkip("TC_WT_FORM_031");
        selectCleaningChipBestEffort();
        setStepResultOrSkip(0, "Pass", "TC_WT_FORM_031"); // hard readback inside on success
        verifyAppAlive("TC_WT_FORM_031 after Pass");
        logStepWithScreenshot("TC_WT_FORM_031 verified");
    }

    @Test(priority = 32)
    public void TC_WT_FORM_032_setResultFailReadsBack() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_032 - Setting step-0 Result to Fail reads back 'Fail'");
        openSwitch1FormsOrSkip("TC_WT_FORM_032");
        selectCleaningChipBestEffort();
        setStepResultOrSkip(0, "Fail", "TC_WT_FORM_032");
        verifyAppAlive("TC_WT_FORM_032 after Fail");
        logStepWithScreenshot("TC_WT_FORM_032 verified");
    }

    @Test(priority = 33)
    public void TC_WT_FORM_033_failRevealsFailureCard() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_033 - A Fail result reveals the failure-details card");
        openSwitch1FormsOrSkip("TC_WT_FORM_033");
        selectCleaningChipBestEffort();
        setStepResultOrSkip(0, "Fail", "TC_WT_FORM_033"); // skips honestly if picker path unavailable
        mediumWait();
        assertTrue(forms.isFailureDetailsVisible(),
                "Fail result must reveal the '… — Failure Details' / 'Description of Failure' card (probe run 15)");
        verifyAppAlive("TC_WT_FORM_033");
        logStepWithScreenshot("TC_WT_FORM_033 verified");
    }

    @Test(priority = 34)
    public void TC_WT_FORM_034_failToPassHidesFailureCard() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_034 - Flipping Fail back to Pass hides the failure-details card");
        openSwitch1FormsOrSkip("TC_WT_FORM_034");
        selectCleaningChipBestEffort();
        setStepResultOrSkip(0, "Fail", "TC_WT_FORM_034");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.isFailureDetailsVisible(),
                "TC_WT_FORM_034: failure card did not surface after Fail (card internals are probe-dependent)");
        setStepResultOrSkip(0, "Pass", "TC_WT_FORM_034");
        mediumWait();
        assertFalse(forms.isFailureDetailsVisible(),
                "Failure card must hide once the Fail result is flipped to Pass");
        verifyAppAlive("TC_WT_FORM_034");
        logStepWithScreenshot("TC_WT_FORM_034 verified");
    }

    @Test(priority = 35)
    public void TC_WT_FORM_035_stepNotesTypeAndReadBack() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_035 - Typing into step-0 Value/Notes reads back the typed text");
        openSwitch1FormsOrSkip("TC_WT_FORM_035");
        selectCleaningChipBestEffort();
        skipIfPreconditionMissing(() -> !forms.noteFields().isEmpty(),
                "TC_WT_FORM_035: no Value/Notes fields readable on this form");
        assertTrue(forms.typeStepNotes(0, "QA auto note"),
                "Typing into the step-0 Value/Notes field must succeed (probe-verified TextField)");
        String readback = forms.stepNotes(0);
        logStep("step-0 notes readback: '" + readback + "'");
        assertTrue(readback != null && readback.contains("QA auto note"),
                "Step-0 notes must read back the typed text — got '" + readback + "'");
        verifyAppAlive("TC_WT_FORM_035");
        logStepWithScreenshot("TC_WT_FORM_035 verified");
    }

    @Test(priority = 36)
    public void TC_WT_FORM_036_secondStepNotesIndependent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_036 - Notes on the second step are independent of the first");
        openSwitch1FormsOrSkip("TC_WT_FORM_036");
        selectCleaningChipBestEffort();
        skipIfPreconditionMissing(() -> forms.noteFields().size() >= 2,
                "TC_WT_FORM_036: fewer than 2 Value/Notes fields on this form");
        assertTrue(forms.typeStepNotes(0, "QA note alpha"), "Typing step-0 notes must succeed");
        assertTrue(forms.typeStepNotes(1, "QA note beta"), "Typing step-1 notes must succeed");
        String n0 = forms.stepNotes(0);
        String n1 = forms.stepNotes(1);
        logStep("readback: step0='" + n0 + "' step1='" + n1 + "'");
        assertTrue(n0 != null && n0.contains("QA note alpha"),
                "Step-0 notes must keep their own text after step-1 was typed — got '" + n0 + "'");
        assertTrue(n1 != null && n1.contains("QA note beta"),
                "Step-1 notes must hold independent text — got '" + n1 + "'");
        verifyAppAlive("TC_WT_FORM_036");
        logStepWithScreenshot("TC_WT_FORM_036 verified");
    }

    @Test(priority = 37)
    public void TC_WT_FORM_037_failureDescriptionTypeSticksWithCardOpen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_037 - Failure description typing succeeds and leaves the card intact");
        openSwitch1FormsOrSkip("TC_WT_FORM_037");
        selectCleaningChipBestEffort();
        setStepResultOrSkip(0, "Fail", "TC_WT_FORM_037");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.isFailureDetailsVisible(),
                "TC_WT_FORM_037: failure card not visible after Fail — card internals are probe-dependent");
        boolean typed = forms.typeFailureDescription("QA auto failure description");
        skipIfPreconditionMissing(() -> typed,
                "TC_WT_FORM_037: failure-card TextView (probe-dependent internals) not typeable");
        guard("TC_WT_FORM_037 after failure description");
        assertTrue(forms.isFailureDetailsVisible(),
                "Failure card must remain on screen after typing the description");
        logStepWithScreenshot("TC_WT_FORM_037 verified");
    }

    @Test(priority = 38)
    public void TC_WT_FORM_038_saveFormCompletesCleanly() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_038 - Saving the form (checkmark) raises no error and keeps the app healthy");
        openSwitch1FormsOrSkip("TC_WT_FORM_038");
        selectCleaningChipBestEffort();
        boolean prefilled = forms.setStepResult(0, "Pass"); // best-effort content before save
        logStep("Pre-save Pass set: " + prefilled);
        assertTrue(forms.saveForm(),
                "The 'checkmark' save control must be tappable (probe-verified nav control)");
        verifyNoErrorAlert();
        verifyAppAlive("TC_WT_FORM_038 after save");
        verifyNotBlank("post-save screen (TC_WT_FORM_038)");
        logStepWithScreenshot("TC_WT_FORM_038 verified");
    }

    @Test(priority = 39)
    public void TC_WT_FORM_039_badgeChipInvariantHoldsAfterSave() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_039 - Reopening the form after save keeps the badge↔chip invariant");
        openRoomOrSkip("TC_WT_FORM_039");
        String comp = forms.rowComposite(SWITCH_ASSET);
        skipIfPreconditionMissing(() -> comp != null,
                "TC_WT_FORM_039: asset '" + SWITCH_ASSET + "' not visible in the room");
        int badgeBefore = forms.assetFormBadge(SWITCH_ASSET);
        skipIfPreconditionMissing(() -> badgeBefore >= 0,
                "TC_WT_FORM_039: badge unparsable from row '" + comp + "'");
        skipIfPreconditionMissing(() -> forms.openAssetForms(SWITCH_ASSET),
                "TC_WT_FORM_039: form screen did not open");
        skipIfPreconditionMissing(() -> forms.saveForm(),
                "TC_WT_FORM_039: save (checkmark) not tappable");
        mediumWait();
        if (forms.isFormScreenOpen()) {
            skipIfPreconditionMissing(() -> forms.backFromForm(),
                    "TC_WT_FORM_039: Back not available after save");
            mediumWait();
        }
        skipIfPreconditionMissing(() -> forms.isAssetsInRoomOpen(),
                "TC_WT_FORM_039: did not land back on 'Assets in Room' after save");
        int badgeAfter = forms.assetFormBadge(SWITCH_ASSET);
        skipIfPreconditionMissing(() -> forms.openAssetForms(SWITCH_ASSET),
                "TC_WT_FORM_039: could not reopen the form after save");
        List<String> chips = forms.getFormChipNames();
        logStep("badgeBefore=" + badgeBefore + " badgeAfter=" + badgeAfter + " chipsOnReopen=" + chips);
        assertTrue(!chips.isEmpty(), "Chip strip must still be populated after save + reopen");
        int expectedBadge = badgeAfter >= 0 ? badgeAfter : badgeBefore;
        assertEquals(chips.size(), expectedBadge,
                "badge↔chip invariant must hold after save (chips vs per-asset badge)");
        verifyAppAlive("TC_WT_FORM_039");
        logStepWithScreenshot("TC_WT_FORM_039 verified");
    }

    @Test(priority = 40)
    public void TC_WT_FORM_040_savedPassPersistsOnReopen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_040 - A saved Pass result persists when the form is reopened (core promise)");
        openSwitch1FormsOrSkip("TC_WT_FORM_040");
        selectCleaningChipBestEffort();
        setStepResultOrSkip(0, "Pass", "TC_WT_FORM_040"); // skips honestly when the picker path is unavailable
        skipIfPreconditionMissing(() -> forms.saveForm(),
                "TC_WT_FORM_040: save (checkmark) not tappable");
        mediumWait();
        if (forms.isFormScreenOpen()) {
            skipIfPreconditionMissing(() -> forms.backFromForm(),
                    "TC_WT_FORM_040: Back not available after save");
            mediumWait();
        }
        skipIfPreconditionMissing(() -> forms.isAssetsInRoomOpen(),
                "TC_WT_FORM_040: did not land back on 'Assets in Room' after save");
        skipIfPreconditionMissing(() -> forms.openAssetForms(SWITCH_ASSET),
                "TC_WT_FORM_040: could not reopen the form");
        selectCleaningChipBestEffort();
        String readback = forms.stepResult(0);
        logStep("step-0 result after save + reopen: '" + readback + "'");
        skipIfPreconditionMissing(() -> readback != null,
                "TC_WT_FORM_040: step rows not readable after reopen");
        if ("—".equals(readback)) {
            logStep("POTENTIAL PERSISTENCE BUG: saved Pass read back as '—' after reopen — "
                    + "persisted results are the feature's core promise; failing hard.");
        }
        assertEquals(readback, "Pass",
                "TC_WT_FORM_040: saved Pass must persist on reopen ('—' readback = persistence bug)");
        verifyAppAlive("TC_WT_FORM_040");
        logStepWithScreenshot("TC_WT_FORM_040 verified");
    }

    // ═══════════════ E. Cross-cutting / safety (TC_WT_FORM_041-045) ═════════

    @Test(priority = 41)
    public void TC_WT_FORM_041_backFromFormReturnsToRoom() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_041 - Back from the form returns to 'Assets in Room'");
        openSwitch1FormsOrSkip("TC_WT_FORM_041");
        assertTrue(forms.backFromForm(),
                "The 'Back' nav control must be tappable on the form screen");
        mediumWait();
        assertTrue(forms.isAssetsInRoomOpen(),
                "Back from the form must land on 'Assets in Room' (probe run 15)");
        verifyAppAlive("TC_WT_FORM_041");
        verifyNotBlank("Assets in Room after back (TC_WT_FORM_041)");
        logStepWithScreenshot("TC_WT_FORM_041 verified");
    }

    @Test(priority = 42)
    public void TC_WT_FORM_042_secondBackReturnsTowardSession() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_042 - A second back closes the room and returns toward the session");
        openSwitch1FormsOrSkip("TC_WT_FORM_042");
        assertTrue(forms.backFromForm(), "First back (form → room) must be tappable");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.isAssetsInRoomOpen(),
                "TC_WT_FORM_042: first back did not land on 'Assets in Room'");
        wo.goBack();
        mediumWait();
        verifyAppAlive("TC_WT_FORM_042 after second back");
        verifyNotBlank("session screen after second back (TC_WT_FORM_042)");
        assertFalse(forms.isAssetsInRoomOpen(),
                "'Assets in Room' must be closed after the second back");
        logStepWithScreenshot("TC_WT_FORM_042 verified");
    }

    @Test(priority = 43)
    public void TC_WT_FORM_043_formScreenSurvivesStabilityGuard() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_043 - The form screen stays stable under the composite crash/alert guard");
        openSwitch1FormsOrSkip("TC_WT_FORM_043");
        guard("TC_WT_FORM_043 stability guard on form screen"); // app-alive + no-error-alert composite
        mediumWait();
        assertTrue(forms.isFormScreenOpen(),
                "Form screen must remain open and detectable after the stability guard");
        verifyAppAlive("TC_WT_FORM_043");
        verifyNotBlank("form screen post-guard (TC_WT_FORM_043)");
        logStepWithScreenshot("TC_WT_FORM_043 verified");
    }

    @Test(priority = 44)
    public void TC_WT_FORM_044_formFlowLeavesWorkOrderListFunctional() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_044 - The form flow leaves the Work Orders list fully functional");
        openSwitch1FormsOrSkip("TC_WT_FORM_044");
        forms.backFromForm(); // best-effort unwind before the fresh nav
        navToListFresh();
        assertTrue(wo.isWorkOrdersScreenDisplayed(),
                "Work Orders list must be reachable and displayed after the form flow");
        assertTrue(wo.scrollWorkOrderListTo(WT04),
                "The WT04 fixture row must still be findable on the list after the form flow");
        verifyAppAlive("TC_WT_FORM_044");
        verifyNoErrorAlert();
        logStepWithScreenshot("TC_WT_FORM_044 verified");
    }

    @Test(priority = 45)
    public void TC_WT_FORM_045_fullLoopSmoke() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WT_FORM_045 - Full-loop smoke: activate → room → form → Pass → save → back → back → list");
        // Stage 1: activate / open the WT04 session.
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_FORM_045");
        guard("TC_WT_FORM_045 after activation");
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(),
                "TC_WT_FORM_045: session details did not settle open");
        // Stage 2: Assets tab → room.
        skipIfPreconditionMissing(() -> wo.tapSessionTab("Assets"),
                "TC_WT_FORM_045: Assets tab not tappable");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.openFirstRoomWithAssetsInTree(),
                "TC_WT_FORM_045: no room with assets reachable");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.isAssetsInRoomOpen(),
                "TC_WT_FORM_045: 'Assets in Room' did not open");
        guard("TC_WT_FORM_045 room open");
        // Stage 3: form → Pass → save.
        skipIfPreconditionMissing(() -> forms.rowComposite(SWITCH_ASSET) != null,
                "TC_WT_FORM_045: asset '" + SWITCH_ASSET + "' not visible");
        skipIfPreconditionMissing(() -> forms.openAssetForms(SWITCH_ASSET),
                "TC_WT_FORM_045: form screen did not open");
        guard("TC_WT_FORM_045 form open");
        selectCleaningChipBestEffort();
        setStepResultOrSkip(0, "Pass", "TC_WT_FORM_045");
        guard("TC_WT_FORM_045 after Pass");
        assertTrue(forms.saveForm(), "Save (checkmark) must fire on the full loop");
        guard("TC_WT_FORM_045 after save");
        // Stage 4: unwind back to the list.
        if (forms.isFormScreenOpen()) {
            forms.backFromForm();
            mediumWait();
        }
        wo.goBack();
        shortWait();
        navToListFresh();
        assertTrue(wo.isWorkOrdersScreenDisplayed(),
                "Work Orders list must be displayed at the end of the full loop");
        bringRowOnScreenOrSkip(WT04, "TC_WT_FORM_045");
        assertTrue(forms.rowComposite(WT04) != null,
                "The WT04 fixture row must still be present after the full loop");
        verifyNoErrorAlert();
        verifyAppAlive("TC_WT_FORM_045 end of loop");
        logStepWithScreenshot("TC_WT_FORM_045 full-loop smoke verified");
    }
}
