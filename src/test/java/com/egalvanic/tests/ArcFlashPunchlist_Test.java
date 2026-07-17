package com.egalvanic.tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ArcFlashPage;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;

/**
 * Arc Flash punchlist overlays + session Collect-AF-Data flow (v1.49).
 *
 * Surfaces (app-source):
 *  - AssetsTabView / ConnectionsTabView ellipsis menu "Show/Hide AF Punchlist"
 *    → per-row bolt (assets) / check-x (edges) badges from af_isComplete.
 *    Toggle state is @State (NOT persisted) and defaults to hidden.
 *  - SessionRoomDetailView trailing filter menu (None/IR/Arc Flash/C.O.M.,
 *    persisted via AssetMetricFilterSettings) + long-press context menu
 *    "Collect AF Data" → EditNodeDetailView focus mode, nav title
 *    "Collect AF Data". With eng-lib OFF the editor shows the LEGACY layout —
 *    assertions here stick to flag-independent parts (title, dismissal).
 *
 * Session tests require an ACTIVE work order; when the fixture is absent they
 * skip via skipIfPreconditionMissing with an explicit fixture reason.
 */
public class ArcFlashPunchlist_Test extends BaseTest {

    private ArcFlashPage afPage;
    private WorkOrderPage workOrderPage;

    @BeforeClass(alwaysRun = true)
    public void punchlistClassSetup() {
        System.out.println("\n📋 Arc Flash Suite (punchlist + session) — Starting");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void punchlistTestSetup() {
        if (!DriverManager.isDriverActive()) return; // fast-skipped: no driver, no page
        afPage = new ArcFlashPage();
        workOrderPage = new WorkOrderPage();
    }

    @AfterClass(alwaysRun = true)
    public void punchlistClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    // ─────────────────────────────────────────────────────────────────────

    @Test(priority = 20)
    public void TC_AF_020_assetsTabMenuOffersPunchlistToggle() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_020 - Assets tab ellipsis menu offers the AF Punchlist toggle (default: Show)");

        logStep("Step 1: Login and open the Assets tab");
        loginAndSelectSite();
        assertTrue(afPage.openTab("Assets"), "Assets tab must open");

        logStep("Step 2: Ellipsis menu contains the punchlist toggle");
        boolean open = afPage.openEllipsisMenu();
        if (!open) afPage.dumpSource("TC_AF_020_menu");
        assertTrue(open, "Assets ellipsis menu must open and offer the AF Punchlist row");
        assertEquals(afPage.getPunchlistOptionLabel(), ArcFlashPage.SHOW_PUNCHLIST,
                "Fresh screen must offer 'Show AF Punchlist' (badges default hidden)");
        logStepWithScreenshot("TC_AF_020: punchlist toggle present");
    }

    @Test(priority = 21)
    public void TC_AF_021_enablingPunchlistShowsAssetBadges() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_021 - Enabling AF Punchlist decorates asset rows with bolt badges; menu flips to Hide");

        logStep("Step 1: Login, Assets tab, badges hidden baseline");
        loginAndSelectSite();
        assertTrue(afPage.openTab("Assets"), "Assets tab must open");
        int before = afPage.countAssetBoltBadges();

        logStep("Step 2: Toggle punchlist ON");
        assertTrue(afPage.openEllipsisMenu(), "Ellipsis menu must open");
        assertTrue(afPage.tapPunchlistOption(ArcFlashPage.SHOW_PUNCHLIST),
                "'Show AF Punchlist' row must be tappable");

        logStep("Step 3: Badges appear on visible rows");
        int after = afPage.countAssetBoltBadges();
        if (after <= before) afPage.dumpSource("TC_AF_021_badges");
        assertTrue(after > before,
                "Bolt badges must appear after enabling (before=" + before + ", after=" + after + ")");

        logStep("Step 4: Menu now offers Hide");
        assertTrue(afPage.openEllipsisMenu(), "Ellipsis menu must reopen");
        assertEquals(afPage.getPunchlistOptionLabel(), ArcFlashPage.HIDE_PUNCHLIST,
                "Menu must flip to 'Hide AF Punchlist' while badges are on");
        assertTrue(afPage.tapPunchlistOption(ArcFlashPage.HIDE_PUNCHLIST),
                "Cleanup: toggle back off");
        logStepWithScreenshot("TC_AF_021: badges " + before + "→" + after);
    }

    @Test(priority = 22)
    public void TC_AF_022_disablingPunchlistRemovesBadges() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_022 - Disabling AF Punchlist removes all bolt badges");

        logStep("Step 1: Login, Assets tab, punchlist ON");
        loginAndSelectSite();
        assertTrue(afPage.openTab("Assets"), "Assets tab must open");
        assertTrue(afPage.openEllipsisMenu(), "Ellipsis menu must open");
        assertTrue(afPage.tapPunchlistOption(ArcFlashPage.SHOW_PUNCHLIST),
                "Enable punchlist first");
        assertTrue(afPage.countAssetBoltBadges() > 0, "Badges must be on before the hide check");

        logStep("Step 2: Toggle OFF removes every badge");
        assertTrue(afPage.openEllipsisMenu(), "Ellipsis menu must reopen");
        assertTrue(afPage.tapPunchlistOption(ArcFlashPage.HIDE_PUNCHLIST),
                "'Hide AF Punchlist' row must be tappable");
        int after = afPage.countAssetBoltBadges();
        assertEquals(after, 0, "No bolt badges may remain after hiding, got " + after);
        logStepWithScreenshot("TC_AF_022: badges removed");
    }

    @Test(priority = 23)
    public void TC_AF_023_connectionsTabPunchlistShowsEdgeBadges() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_023 - Connections tab AF Punchlist decorates edge rows with check/x badges");

        logStep("Step 1: Login and open the Connections tab");
        loginAndSelectSite();
        assertTrue(afPage.openTab("Connections"), "Connections tab must open");
        int before = afPage.countEdgeBadges();

        logStep("Step 2: Toggle punchlist ON via the ellipsis menu");
        boolean open = afPage.openEllipsisMenu();
        if (!open) afPage.dumpSource("TC_AF_023_menu");
        assertTrue(open, "Connections ellipsis menu must open with the punchlist row");
        assertTrue(afPage.tapPunchlistOption(afPage.getPunchlistOptionLabel()),
                "Punchlist row must be tappable");

        logStep("Step 3: Edge badges appear");
        int after = afPage.countEdgeBadges();
        if (after <= before) afPage.dumpSource("TC_AF_023_badges");
        assertTrue(after > before,
                "Edge check/x badges must appear (before=" + before + ", after=" + after + ")");

        logStep("Step 4: Cleanup — hide again");
        assertTrue(afPage.openEllipsisMenu() && afPage.tapPunchlistOption(ArcFlashPage.HIDE_PUNCHLIST),
                "Cleanup toggle must work");
        logStepWithScreenshot("TC_AF_023: edge badges " + before + "→" + after);
    }

    @Test(priority = 24)
    public void TC_AF_024_sessionRoomArcFlashOverlayFilter() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_024 - Session room: overlay filter 'Arc Flash' decorates asset rows");

        logStep("Step 1: Login and open the active work order session");
        loginAndSelectSite();
        openSessionRoomOrSkip();

        logStep("Step 2: Pick the Arc Flash overlay from the filter menu");
        boolean selected = afPage.selectArcFlashOverlay();
        if (!selected) afPage.dumpSource("TC_AF_024_overlay");
        assertTrue(selected, "'Arc Flash' overlay must be selectable from the room filter menu");

        logStep("Step 3: Rows show the AF readiness badge");
        int badges = afPage.countAssetBoltBadges();
        if (badges == 0) afPage.dumpSource("TC_AF_024_badges");
        assertTrue(badges > 0, "Arc Flash overlay must decorate room asset rows, got " + badges);
        logStepWithScreenshot("TC_AF_024: overlay badges=" + badges);
    }

    @Test(priority = 25)
    public void TC_AF_025_collectAFDataOpensFocusedEditor() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_COLLECT,
                "TC_AF_025 - Long-press asset row → 'Collect AF Data' opens the focused editor");

        logStep("Step 1: Login and open a session room with assets");
        loginAndSelectSite();
        openSessionRoomOrSkip();

        logStep("Step 2: Long-press the first asset row for the context menu");
        org.openqa.selenium.WebElement row = afPage.firstRoomAssetRow();
        skipIfPreconditionMissing(() -> row != null, "no asset row available in the session room");
        assertTrue(afPage.longPressElement(row), "Long-press must dispatch");
        boolean menuShown = waitFor(() -> afPage.isCollectAFDataRowVisible(), 6);
        if (!menuShown) afPage.dumpSource("TC_AF_025_ctxmenu");
        assertTrue(menuShown, "Context menu must include 'Collect AF Data'");

        logStep("Step 3: Tapping it opens the 'Collect AF Data' editor");
        assertTrue(afPage.tapCollectAFData(), "Editor with nav title 'Collect AF Data' must open");
        verifyAppAlive("Collect AF Data editor");
        // NOTE: no verifyNotBlank here — the editor's Engineering DOM is giant
        // (whole-tree census = known WDA wedge, ThreadTimeout run 2026-07-16);
        // tapCollectAFData already verified the content signature
        // ('Engineering' + 'System Voltage' visible), which subsumes non-blank.
        logStepWithScreenshot("TC_AF_025: Collect AF Data editor verified");
    }

    // ─────────────────────────────────────────────────────────────────────

    /** Active WO → session details → first room with assets, or SKIP with reason. */
    private void openSessionRoomOrSkip() {
        skipIfPreconditionMissing(() -> siteSelectionPage.clickWorkOrderCard(),
                "no active Work Order card on the dashboard (session fixture missing)");
        skipIfPreconditionMissing(() -> workOrderPage.ensureSessionDetailsOpen(),
                "active WO card did not open Session Details");
        skipIfPreconditionMissing(() -> workOrderPage.tapFirstRoomWithAssets(),
                "session has no room containing assets");
    }

    private boolean waitFor(java.util.function.Supplier<Boolean> cond, int seconds) {
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < end) {
            try {
                if (Boolean.TRUE.equals(cond.get())) return true;
            } catch (Exception ignored) { }
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        return false;
    }
}
