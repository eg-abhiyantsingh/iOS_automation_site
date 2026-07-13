package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ArcFlashPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Arc Flash Readiness — PUNCHLIST deep laws (TC_AF_090-096, data-driven).
 * ZP-2373 parity expansion: toggle idempotence, badge-count stability and
 * bounds, per-tab behavior, and dashboard-consistency cross-checks — all on
 * ArcFlashPage APIs live-validated by ArcFlashPunchlist_Test.
 */
public class ArcFlashPunchlistDeep_Test extends BaseTest {

    private ArcFlashPage arcPage;

    @BeforeMethod(alwaysRun = true)
    public void punchlistDeepSetup() {
        if (!DriverManager.isDriverActive()) return;
        arcPage = new ArcFlashPage();
    }

    /** Ensure punchlist is OFF (label reads Show …) — idempotent pre-state. */
    private void ensurePunchlistOff(String tab) {
        assertTrue(arcPage.openTab(tab), tab + " tab must open");
        assertTrue(arcPage.openEllipsisMenu(), "ellipsis menu must open");
        String label = arcPage.getPunchlistOptionLabel();
        if (label != null && label.contains("Hide")) {
            arcPage.tapPunchlistOption(label);
            shortWait();
        } else {
            arcPage.openTab(tab); // dismiss menu
        }
        shortWait();
    }

    private String toggle(String tab) {
        assertTrue(arcPage.openTab(tab), tab + " tab must open");
        assertTrue(arcPage.openEllipsisMenu(), "ellipsis menu must open");
        String label = arcPage.getPunchlistOptionLabel();
        assertTrue(label != null && !label.isEmpty(), "punchlist option must be present");
        assertTrue(arcPage.tapPunchlistOption(label), "punchlist option must be tappable");
        mediumWait();
        return label;
    }

    @DataProvider(name = "tabs")
    public Object[][] tabs() {
        return new Object[][]{{"Assets"}, {"Connections"}};
    }

    @Test(priority = 90, dataProvider = "tabs")
    public void TC_AF_090_toggleLabelFlipsShowHide(String tab) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_090 [" + tab + "] - punchlist option label flips Show<->Hide on each toggle");
        loginAndSelectSite();
        ensurePunchlistOff(tab);
        String first = toggle(tab);
        assertTrue(first.contains(ArcFlashPage.SHOW_PUNCHLIST) || first.contains("Show"),
                "[" + tab + "] first toggle must have offered Show, got '" + first + "'");
        String second = toggle(tab);
        assertTrue(second.contains("Hide"),
                "[" + tab + "] second toggle must offer Hide, got '" + second + "'");
        logStepWithScreenshot("TC_AF_090 [" + tab + "] Show<->Hide flip verified");
    }

    @Test(priority = 91)
    public void TC_AF_091_assetBadgeCountStableAcrossThreeCycles() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_091 - asset bolt-badge count identical across three enable cycles");
        loginAndSelectSite();
        ensurePunchlistOff("Assets");
        Integer baseline = null;
        for (int cycle = 1; cycle <= 3; cycle++) {
            toggle("Assets"); // ON
            int count = arcPage.countAssetBoltBadges();
            if (baseline == null) baseline = count;
            assertEquals(count, (int) baseline,
                    "cycle " + cycle + ": bolt-badge count must be stable (got " + count + ")");
            toggle("Assets"); // OFF
        }
        logStepWithScreenshot("TC_AF_091 badge stability x3 verified (count=" + baseline + ")");
    }

    @Test(priority = 92)
    public void TC_AF_092_disableRemovesAllAssetBadges() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_092 - disabling the punchlist removes every asset bolt badge");
        loginAndSelectSite();
        ensurePunchlistOff("Assets");
        toggle("Assets"); // ON
        skipIfPreconditionMissing(() -> arcPage.countAssetBoltBadges() > 0,
                "site currently has no punchlist assets to badge");
        toggle("Assets"); // OFF
        assertEquals(arcPage.countAssetBoltBadges(), 0,
                "no bolt badge may remain after disabling the punchlist");
        logStepWithScreenshot("TC_AF_092 disable-removes-badges verified");
    }

    @Test(priority = 93)
    public void TC_AF_093_edgeBadgeCountStableAcrossCycles() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_093 - connections-tab edge-badge count identical across two enable cycles");
        loginAndSelectSite();
        ensurePunchlistOff("Connections");
        toggle("Connections");
        int first = arcPage.countEdgeBadges();
        toggle("Connections");
        toggle("Connections");
        assertEquals(arcPage.countEdgeBadges(), first,
                "edge-badge count must be stable across cycles (first=" + first + ")");
        toggle("Connections"); // leave OFF
        logStepWithScreenshot("TC_AF_093 edge-badge stability verified");
    }

    @Test(priority = 94)
    public void TC_AF_094_badgeCountBoundedByDashboardRemaining() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_094 - visible asset bolt badges never exceed the dashboard's Remaining count");
        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "dashboard must open");
        int remaining = arcPage.getRemainingCount();
        assertTrue(arcPage.tapDone(), "Done must close the dashboard");
        skipIfPreconditionMissing(() -> remaining >= 0, "Remaining stat unreadable");
        ensurePunchlistOff("Assets");
        toggle("Assets"); // ON
        int badges = arcPage.countAssetBoltBadges();
        assertTrue(badges <= remaining,
                "visible bolt badges (" + badges + ") must never exceed dashboard Remaining (" + remaining + ")");
        toggle("Assets"); // OFF
        logStepWithScreenshot("TC_AF_094 badge<=remaining bound verified (" + badges + "<=" + remaining + ")");
    }

    @Test(priority = 95, dataProvider = "tabs")
    public void TC_AF_095_punchlistOptionAlwaysListed(String tab) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_095 [" + tab + "] - the punchlist option is always present in the tab's ellipsis menu");
        loginAndSelectSite();
        assertTrue(arcPage.openTab(tab), tab + " tab must open");
        assertTrue(arcPage.openEllipsisMenu(), "ellipsis menu must open");
        assertTrue(arcPage.isPunchlistOptionVisible(),
                "[" + tab + "] punchlist option must be listed");
        String label = arcPage.getPunchlistOptionLabel();
        assertTrue(label.contains("Punchlist"), "option label must mention Punchlist, got '" + label + "'");
        arcPage.openTab(tab); // dismiss
        logStepWithScreenshot("TC_AF_095 [" + tab + "] option presence verified");
    }

    @Test(priority = 96)
    public void TC_AF_096_punchlistStateSurvivesTabSwitch() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_096 - assets-tab punchlist stays enabled across an Assets->Connections->Assets switch");
        loginAndSelectSite();
        ensurePunchlistOff("Assets");
        toggle("Assets"); // ON
        int before = arcPage.countAssetBoltBadges();
        assertTrue(arcPage.openTab("Connections"), "Connections tab must open");
        shortWait();
        assertTrue(arcPage.openTab("Assets"), "back to Assets");
        mediumWait();
        assertEquals(arcPage.countAssetBoltBadges(), before,
                "punchlist badges must survive a tab switch round-trip");
        toggle("Assets"); // OFF cleanup
        logStepWithScreenshot("TC_AF_096 tab-switch persistence verified");
    }

    @Test(priority = 97, dataProvider = "tabs")
    public void TC_AF_097_menuDismissLeavesBadgeStateUnchanged(String tab) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_097 [" + tab + "] - opening and dismissing the ellipsis menu changes no badge state");
        loginAndSelectSite();
        ensurePunchlistOff(tab);
        int before = tab.equals("Assets") ? arcPage.countAssetBoltBadges() : arcPage.countEdgeBadges();
        assertTrue(arcPage.openEllipsisMenu(), "menu must open");
        arcPage.openTab(tab); // dismiss without choosing
        shortWait();
        int after = tab.equals("Assets") ? arcPage.countAssetBoltBadges() : arcPage.countEdgeBadges();
        assertEquals(after, before, "[" + tab + "] menu open/dismiss must not alter badges");
        logStepWithScreenshot("TC_AF_097 [" + tab + "] no-op dismissal verified");
    }

    @Test(priority = 98)
    public void TC_AF_098_disableRemovesEdgeBadges() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_098 - disabling the connections punchlist removes every edge badge");
        loginAndSelectSite();
        ensurePunchlistOff("Connections");
        toggle("Connections"); // ON
        skipIfPreconditionMissing(() -> arcPage.countEdgeBadges() > 0,
                "site currently has no punchlist edges to badge");
        toggle("Connections"); // OFF
        assertEquals(arcPage.countEdgeBadges(), 0, "no edge badge may remain after disable");
        logStepWithScreenshot("TC_AF_098 edge-badge removal verified");
    }

    @Test(priority = 99)
    public void TC_AF_099_punchlistToggleNeverAltersDashboardNumbers() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_099 - the punchlist overlay is a VIEW toggle: dashboard readiness numbers are unchanged by it");
        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "dashboard must open");
        int overallBefore = arcPage.getOverallPercent();
        assertTrue(arcPage.tapDone(), "Done must close");
        ensurePunchlistOff("Assets");
        toggle("Assets"); // ON
        toggle("Assets"); // OFF
        assertTrue(arcPage.openTab("Site"), "Site tab must open (back to home)"); // dashboard card lives on Site home
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "dashboard must reopen");
        assertEquals(arcPage.getOverallPercent(), overallBefore,
                "punchlist view toggling must never change readiness numbers (view filter law)");
        logStepWithScreenshot("TC_AF_099 view-toggle law verified");
    }


    @Test(priority = 100)
    public void TC_AF_100_punchlistDefaultsOffAfterEnsure() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_100 - with the punchlist normalized OFF, no badge renders on either tab");
        loginAndSelectSite();
        ensurePunchlistOff("Assets");
        assertEquals(arcPage.countAssetBoltBadges(), 0, "no asset badge in OFF state");
        ensurePunchlistOff("Connections");
        assertEquals(arcPage.countEdgeBadges(), 0, "no edge badge in OFF state");
        logStepWithScreenshot("TC_AF_100 OFF-state census verified");
    }

}
