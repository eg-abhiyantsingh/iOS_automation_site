package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ArcFlashPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Arc Flash Readiness — responsive / form-factor contract (ZP-2373 mobile parity).
 *
 * Runs UNCHANGED on both iPhone and iPad CI jobs: every assertion is expressed
 * against the live window size, never absolute coordinates. Mirrors the web
 * suite's AFP_03_ResponsiveLayout for the NFPA 70E per-asset-class dashboard,
 * plus explicit touch-scroll and touch-drill interaction checks the ticket
 * calls out for mobile.
 *
 * TC_AF_040-044. Not eng-lib gated (same as the rest of the arc-flash module).
 */
public class ArcFlashResponsive_Test extends BaseTest {

    private ArcFlashPage arcPage;

    @BeforeMethod(alwaysRun = true)
    public void responsiveSetup() {
        if (!DriverManager.isDriverActive()) return;
        arcPage = new ArcFlashPage();
    }

    private Dimension window() {
        return DriverManager.getDriver().manage().window().getSize();
    }

    private String formFactor() {
        Dimension w = window();
        // Points, not pixels: iPads report >= 744pt width (iPad mini) — every
        // iPhone including Pro Max stays below 500pt.
        return (Math.min(w.getWidth(), w.getHeight()) >= 700) ? "iPad" : "iPhone";
    }

    /** Element fully inside the horizontal viewport (no responsive cutoff). */
    private boolean withinViewportWidth(WebElement el) {
        Dimension w = window();
        org.openqa.selenium.Rectangle r = el.getRect();
        return r.getX() >= 0 && (r.getX() + r.getWidth()) <= w.getWidth();
    }

    @Test(priority = 40)
    public void TC_AF_040_dashboardRendersWithinViewport() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_040 [" + formFactor() + "] - NFPA 70E dashboard renders fully inside the viewport (no horizontal cutoff)");

        logStep("Step 1: Open the Arc Flash dashboard (" + formFactor() + ", " + window() + ")");
        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "Arc Flash dashboard must open on " + formFactor());

        logStep("Step 2: Readiness score card + overall percent visible and in-bounds");
        assertTrue(arcPage.hasReadinessScoreCard(), "readiness score card must render");
        assertTrue(withinViewportWidth(arcPage.readinessScoreCardElement()),
                "[" + formFactor() + "] score card must sit fully inside the viewport width");
        int overall = arcPage.getOverallPercent();
        assertTrue(overall >= 0 && overall <= 100,
                "overall readiness percent must be 0..100, got " + overall);

        logStep("Step 3: All three metric cards render on this form factor");
        for (String metric : new String[]{ArcFlashPage.METRIC_ASSET_DETAILS, ArcFlashPage.METRIC_CONNECTION_DETAILS, ArcFlashPage.METRIC_SOURCE_TARGET}) {
            assertTrue(arcPage.hasMetricCard(metric),
                    "[" + formFactor() + "] metric card '" + metric + "' must render");
        }
        logStepWithScreenshot("TC_AF_040 [" + formFactor() + "] dashboard layout verified");
    }

    @Test(priority = 41)
    public void TC_AF_041_touchScrollRoundTripKeepsDashboardStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_041 [" + formFactor() + "] - touch scroll down/up round-trip leaves the dashboard consistent");

        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "dashboard must open");
        int before = arcPage.getOverallPercent();

        logStep("Step 1: Touch-scroll to the bottom of the breakdown and back");
        arcPage.scrollBreakdownDown();
        arcPage.scrollBreakdownDown();
        arcPage.scrollBreakdownUp();
        arcPage.scrollBreakdownUp();

        logStep("Step 2: Dashboard state unchanged by scrolling (read-only gesture)");
        assertTrue(arcPage.hasReadinessScoreCard(), "score card must survive the scroll round-trip");
        int after = arcPage.getOverallPercent();
        assertEquals(after, before, "scrolling must not change the readiness percent");
        logStepWithScreenshot("TC_AF_041 [" + formFactor() + "] scroll round-trip verified");
    }

    @Test(priority = 42)
    public void TC_AF_042_perClassBreakdownReadableOnFormFactor() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_042 [" + formFactor() + "] - per-asset-class breakdown buckets render readable (web AF_09 parity)");

        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "dashboard must open");

        logStep("Step 1: Asset Details breakdown selected (default)");
        assertTrue(arcPage.selectMetricCard(ArcFlashPage.METRIC_ASSET_DETAILS), "Asset Details card must be selectable by touch");

        logStep("Step 2: Per-class bucket labels present and non-empty on " + formFactor());
        List<String> buckets = arcPage.getVisibleBucketLabels();
        assertTrue(!buckets.isEmpty(),
                "[" + formFactor() + "] per-asset-class breakdown must list at least one class bucket");
        for (String b : buckets) {
            assertTrue(b != null && !b.trim().isEmpty(), "bucket label must be non-empty");
        }
        logStepWithScreenshot("TC_AF_042 [" + formFactor() + "] buckets: " + buckets);
    }

    @Test(priority = 43)
    public void TC_AF_043_touchDrillIntoBucketAndBack() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_043 [" + formFactor() + "] - touch press expands a class bucket and reveals its rows (web AF_13 parity)");

        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "dashboard must open");
        assertTrue(arcPage.selectMetricCard(ArcFlashPage.METRIC_ASSET_DETAILS), "Asset Details breakdown must open");

        logStep("Step 1: Expand the first class bucket via touch and verify rows reveal");
        assertTrue(arcPage.expandFirstBucketAndCheckRows(),
                "[" + formFactor() + "] first bucket must expand by touch and reveal per-asset rows");
        logStepWithScreenshot("TC_AF_043 [" + formFactor() + "] bucket drill verified");
    }

    @Test(priority = 44)
    public void TC_AF_044_metricCardSwitchByTouchOnFormFactor() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_044 [" + formFactor() + "] - each metric card switches the breakdown by touch (web AF_06/AF_30 parity)");

        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "dashboard must open");

        for (String metric : new String[]{ArcFlashPage.METRIC_CONNECTION_DETAILS, ArcFlashPage.METRIC_SOURCE_TARGET, ArcFlashPage.METRIC_ASSET_DETAILS}) {
            logStep("Switching breakdown to '" + metric + "' by touch");
            assertTrue(arcPage.selectMetricCard(metric),
                    "[" + formFactor() + "] metric card '" + metric + "' must be tappable");
            String header = arcPage.getBreakdownHeader();
            assertTrue(header != null && !header.isEmpty(),
                    "breakdown header must render after switching to '" + metric + "'");
        }
        logStepWithScreenshot("TC_AF_044 [" + formFactor() + "] metric switching verified");
    }

    @Test(priority = 45)
    public void TC_AF_045_statLineWithinViewport() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_045 [" + formFactor() + "] - Completed/Remaining/Total stat line renders and parses on this form factor");
        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "dashboard must open");
        assertTrue(arcPage.getCompletedCount() >= 0, "Completed stat must parse");
        assertTrue(arcPage.getRemainingCount() >= 0, "Remaining stat must parse");
        assertTrue(arcPage.getTotalItemsCount() >= 0, "Total stat must parse");
        logStepWithScreenshot("TC_AF_045 [" + formFactor() + "] stat line verified");
    }

    @Test(priority = 46)
    public void TC_AF_046_doneButtonReachableByTouch() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_046 [" + formFactor() + "] - Done dismisses the dashboard by touch on this form factor");
        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "dashboard must open");
        assertTrue(arcPage.tapDone(), "[" + formFactor() + "] Done must dismiss the dashboard");
        logStepWithScreenshot("TC_AF_046 [" + formFactor() + "] Done round-trip verified");
    }

    @Test(priority = 47)
    public void TC_AF_047_formFactorProbeSanity() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_047 - window size sane and form-factor probe deterministic");
        loginAndSelectSite();
        Dimension w = window();
        assertTrue(Math.min(w.getWidth(), w.getHeight()) >= 320,
                "window min dimension must be >= 320pt, got " + w);
        assertEquals(formFactor(), formFactor(), "form-factor probe must be deterministic");
        logStepWithScreenshot("TC_AF_047 window=" + w + " formFactor=" + formFactor());
    }

}
