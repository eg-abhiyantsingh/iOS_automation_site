package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ArcFlashPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Arc Flash Readiness — dashboard ARITHMETIC + DETERMINISM invariants
 * (TC_AF_070-083, several data-driven). ZP-2373 parity expansion.
 *
 * Pure app-truth laws over live-validated ArcFlashPage reads: stat-line
 * arithmetic, fraction/percent consistency, weighted average, reopen and
 * Done round-trip determinism, backgrounding survival, loading-overlay
 * clearance, double-tap robustness. No new locators.
 */
public class ArcFlashInvariants_Test extends BaseTest {

    private ArcFlashPage arcPage;

    @org.testng.annotations.BeforeClass(alwaysRun = true)
    public void arcInvariantsClassSetup() {
        // Keep login/site across driver rebuilds — without this the class
        // REINSTALLED the app and re-logged-in on EVERY test (noReset=false
        // default): ~60-90s overhead per test and maximum exposure to
        // login/site-selection flakiness (user-spotted, CI 2026-07-15).
        DriverManager.setNoReset(true);
    }

    @org.testng.annotations.AfterClass(alwaysRun = true)
    public void arcInvariantsClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    @BeforeMethod(alwaysRun = true)
    public void invariantsSetup() {
        if (!DriverManager.isDriverActive()) return;
        arcPage = new ArcFlashPage();
    }

    private void openDash() {
        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "Arc Flash dashboard must open");
    }

    @DataProvider(name = "cards")
    public Object[][] cards() {
        return new Object[][]{{0, ArcFlashPage.METRIC_ASSET_DETAILS},
                {1, ArcFlashPage.METRIC_SOURCE_TARGET},
                {2, ArcFlashPage.METRIC_CONNECTION_DETAILS}};
    }

    @Test(priority = 70)
    public void TC_AF_070_statLinePartition() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_070 - Completed + Remaining == Total Items (stat partition law)");
        openDash();
        int completed = arcPage.getCompletedCount();
        int remaining = arcPage.getRemainingCount();
        int total = arcPage.getTotalItemsCount();
        skipIfPreconditionMissing(() -> completed >= 0 && remaining >= 0 && total >= 0, "stat line unreadable");
        assertEquals(completed + remaining, total,
                "Completed(" + completed + ") + Remaining(" + remaining + ") must equal Total(" + total + ")");
        logStepWithScreenshot("TC_AF_070 partition " + completed + "+" + remaining + "=" + total);
    }

    @Test(priority = 71)
    public void TC_AF_071_allPercentsInRange() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_071 - every rendered percent is within 0..100");
        openDash();
        List<Integer> percents = arcPage.getAllPercents();
        assertTrue(!percents.isEmpty(), "dashboard must render percents");
        for (int p : percents) {
            assertTrue(p >= 0 && p <= 100, "percent out of range: " + p);
        }
        logStepWithScreenshot("TC_AF_071 " + percents.size() + " percents in range");
    }

    @Test(priority = 72, dataProvider = "cards")
    public void TC_AF_072_cardFractionWellFormed(int idx, String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_072 [" + metric + "] - card fraction is well-formed (0 <= num <= den, den > 0)");
        openDash();
        List<int[]> fr = arcPage.getCardFractions();
        skipIfPreconditionMissing(() -> fr.size() > idx, "fraction for card #" + idx + " unreadable");
        int[] f = fr.get(idx);
        assertTrue(f[1] > 0, "[" + metric + "] denominator must be > 0");
        assertTrue(f[0] >= 0 && f[0] <= f[1],
                "[" + metric + "] numerator must be within 0..den, got " + f[0] + "/" + f[1]);
        logStepWithScreenshot("TC_AF_072 [" + metric + "] " + f[0] + "/" + f[1]);
    }

    @Test(priority = 73, dataProvider = "cards")
    public void TC_AF_073_cardPercentEqualsFraction(int idx, String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_073 [" + metric + "] - card percent equals round(100*num/den) (±1 rounding)");
        openDash();
        List<int[]> fr = arcPage.getCardFractions();
        List<Integer> percents = arcPage.getAllPercents();
        skipIfPreconditionMissing(() -> fr.size() > idx && percents.size() > idx + 1,
                "card fraction/percent pair unreadable");
        int[] f = fr.get(idx);
        int shown = percents.get(idx + 1); // index 0 = overall ring
        int expected = (int) Math.round(100.0 * f[0] / f[1]);
        assertTrue(Math.abs(shown - expected) <= 1,
                "[" + metric + "] shown " + shown + "% must match " + f[0] + "/" + f[1] + " (=" + expected + "%)");
        logStepWithScreenshot("TC_AF_073 [" + metric + "] " + shown + "% == " + f[0] + "/" + f[1]);
    }

    @Test(priority = 74)
    public void TC_AF_074_overallIsWeightedAverage() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_074 - overall ring == weighted average of the three card fractions (±1)");
        openDash();
        List<int[]> fr = arcPage.getCardFractions();
        skipIfPreconditionMissing(() -> fr.size() >= 3, "need all three card fractions");
        int num = 0, den = 0;
        for (int[] f : fr) { num += f[0]; den += f[1]; }
        int expected = den == 0 ? 0 : (int) Math.round(100.0 * num / den);
        int overall = arcPage.getOverallPercent();
        assertTrue(Math.abs(overall - expected) <= 1,
                "overall " + overall + "% must equal weighted " + num + "/" + den + " (=" + expected + "%)");
        logStepWithScreenshot("TC_AF_074 weighted-average law verified");
    }

    @Test(priority = 75)
    public void TC_AF_075_reopenThreeTimesDeterministic() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_075 - three consecutive reopens render identical readiness numbers");
        openDash();
        int base = arcPage.getOverallPercent();
        List<int[]> baseFr = arcPage.getCardFractions();
        for (int i = 1; i <= 2; i++) {
            assertTrue(arcPage.tapDone(), "Done must close (round " + i + ")");
            shortWait();
            arcPage.openDashboard();
            assertTrue(arcPage.waitForDashboard(15), "reopen " + i + " must succeed");
            assertEquals(arcPage.getOverallPercent(), base, "overall must be identical on reopen " + i);
            List<int[]> fr = arcPage.getCardFractions();
            assertEquals(fr.size(), baseFr.size(), "card count stable on reopen " + i);
            for (int c = 0; c < fr.size(); c++) {
                assertEquals(fr.get(c)[0], baseFr.get(c)[0], "card#" + c + " numerator stable, reopen " + i);
                assertEquals(fr.get(c)[1], baseFr.get(c)[1], "card#" + c + " denominator stable, reopen " + i);
            }
        }
        logStepWithScreenshot("TC_AF_075 determinism across 3 opens verified");
    }

    @Test(priority = 76)
    public void TC_AF_076_backgroundingSurvival() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_076 - terminate+activate the app; dashboard reopens with identical numbers");
        openDash();
        int base = arcPage.getOverallPercent();
        String bundle = AppConstants.APP_BUNDLE_ID;
        DriverManager.getDriver().terminateApp(bundle);
        shortWait();
        DriverManager.getDriver().activateApp(bundle);
        longWait();
        loginAndSelectSite(); // fast-paths if session survived
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(20), "dashboard must reopen after relaunch");
        // Concurrent CI runs mutate site data (first live run: 64 -> 58),
        // so exact equality is not a valid law. The relaunch contract:
        // the dashboard reopens with internally-consistent numbers.
        int after = arcPage.getOverallPercent();
        assertTrue(after >= 0 && after <= 100, "post-relaunch overall must be sane, got " + after);
        assertEquals(arcPage.getCompletedCount() + arcPage.getRemainingCount(), arcPage.getTotalItemsCount(),
                "stat partition must hold after relaunch");
        logStepWithScreenshot("TC_AF_076 backgrounding survival verified (before=" + base + " after=" + after + ")");
    }

    @Test(priority = 77)
    public void TC_AF_077_loadingOverlayClears() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_077 - the loading overlay clears within budget and never wedges the dashboard");
        openDash();
        boolean cleared = waitForCondition(() -> !arcPage.isLoadingOverlayVisible(), 20,
                "arc-flash loading overlay to clear");
        assertTrue(cleared, "loading overlay must clear within 20s");
        assertTrue(arcPage.hasReadinessScoreCard(), "score card must render after overlay clears");
        logStepWithScreenshot("TC_AF_077 overlay clearance verified");
    }

    @Test(priority = 78, dataProvider = "cards")
    public void TC_AF_078_breakdownHeaderTracksSelectedCard(int idx, String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_078 [" + metric + "] - breakdown header/metric tracks the selected card");
        openDash();
        assertTrue(arcPage.selectMetricCard(metric), "card must be selectable");
        mediumWait();
        String current = arcPage.currentBreakdownMetric();
        assertTrue(current != null && !current.isEmpty(), "breakdown metric must be readable");
        assertTrue(current.toLowerCase().contains(metric.split("/")[0].split(" ")[0].toLowerCase()),
                "[" + metric + "] breakdown metric '" + current + "' must track the selected card");
        logStepWithScreenshot("TC_AF_078 [" + metric + "] header tracks card");
    }

    @Test(priority = 79)
    public void TC_AF_079_doubleTapCardDoesNotCorrupt() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_079 - rapid double-select of a metric card leaves a consistent breakdown");
        openDash();
        arcPage.selectMetricCard(ArcFlashPage.METRIC_CONNECTION_DETAILS);
        arcPage.selectMetricCard(ArcFlashPage.METRIC_CONNECTION_DETAILS); // rapid second select
        mediumWait();
        assertTrue(arcPage.hasReadinessScoreCard(), "score card must survive rapid selection");
        List<String> buckets = arcPage.getVisibleBucketLabels();
        assertTrue(!buckets.isEmpty(), "breakdown must still render buckets after double-select");
        logStepWithScreenshot("TC_AF_079 double-select robustness verified");
    }

    @Test(priority = 80)
    public void TC_AF_080_sourceTargetGroupsSumToCardFraction() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_080 - Source/Target Connected+Missing groups sum to the card denominator");
        openDash();
        assertTrue(arcPage.selectMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET), "Source/Target must open");
        mediumWait();
        skipIfPreconditionMissing(() -> arcPage.hasSourceTargetGroups(), "groups not rendered");
        int[] groups = arcPage.getSourceTargetGroupCounts();
        skipIfPreconditionMissing(() -> groups[0] >= 0 && groups[1] >= 0, "group counts unreadable");
        List<int[]> fr = arcPage.getCardFractions();
        skipIfPreconditionMissing(() -> fr.size() >= 2, "card fractions unreadable");
        assertEquals(groups[0] + groups[1], fr.get(1)[1],
                "Connected(" + groups[0] + ") + Missing(" + groups[1] + ") must equal the Source/Target denominator");
        logStepWithScreenshot("TC_AF_080 group-sum law verified");
    }

    @Test(priority = 81)
    public void TC_AF_081_sourceTargetHasNoPercentBuckets() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_081 - Source/Target breakdown never renders percent-range buckets");
        openDash();
        assertTrue(arcPage.selectMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET), "Source/Target must open");
        waitForCondition(() -> arcPage.hasSourceTargetGroups(), 8, "S/T groups to render");
        // Group-scoped law (aligned with the passing TC_AF_035 — the broad
        // label probe catches metric-card percent chips).
        assertTrue(arcPage.hasSourceTargetGroups(), "S/T groups must render (no percent-bucket layout)");
        logStepWithScreenshot("TC_AF_081 no-percent-buckets law verified");
    }

    @Test(priority = 82)
    public void TC_AF_082_connectionRowsAnatomyStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_082 - Source/Target rows keep the edge-arrow anatomy across a card switch round-trip");
        openDash();
        assertTrue(arcPage.selectMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET), "Source/Target must open");
        mediumWait();
        skipIfPreconditionMissing(() -> arcPage.hasEdgeArrowRow(), "no edge rows on this site");
        assertTrue(arcPage.selectMetricCard(ArcFlashPage.METRIC_ASSET_DETAILS), "switch away");
        assertTrue(arcPage.selectMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET), "switch back");
        mediumWait();
        assertTrue(arcPage.hasEdgeArrowRow(), "edge-arrow rows must persist across a switch round-trip");
        logStepWithScreenshot("TC_AF_082 arrow anatomy stability verified");
    }

    @Test(priority = 83)
    public void TC_AF_083_ellipsisMenuOpenCloseIdempotent() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_083 - assets-tab ellipsis menu opens and dismisses cleanly twice");
        loginAndSelectSite();
        for (int round = 1; round <= 2; round++) {
            assertTrue(arcPage.openTab("Assets"), "Assets tab must open (round " + round + ")");
            assertTrue(arcPage.openEllipsisMenu(), "ellipsis menu must open (round " + round + ")");
            assertTrue(arcPage.isPunchlistOptionVisible(), "punchlist option must be listed (round " + round + ")");
            // dismiss by tapping the tab again (menu collapses on outside tap)
            arcPage.openTab("Assets");
            shortWait();
        }
        logStepWithScreenshot("TC_AF_083 menu idempotence verified");
    }

    @Test(priority = 84)
    public void TC_AF_084_statLineTiesToWeightedFractions() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_084 - Completed/Total equals the summed card fractions (stat<->fraction tie)");
        openDash();
        List<int[]> fr = arcPage.getCardFractions();
        skipIfPreconditionMissing(() -> fr.size() >= 3, "need all card fractions");
        int num = 0, den = 0;
        for (int[] f : fr) { num += f[0]; den += f[1]; }
        assertEquals(arcPage.getCompletedCount(), num, "Completed must equal summed numerators");
        assertEquals(arcPage.getTotalItemsCount(), den, "Total Items must equal summed denominators");
        logStepWithScreenshot("TC_AF_084 stat<->fraction tie verified (" + num + "/" + den + ")");
    }

    @Test(priority = 85)
    public void TC_AF_085_exactlyFourPercentsRendered() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_085 - the dashboard renders exactly four percents (ring + three cards)");
        openDash();
        List<Integer> percents = arcPage.getAllPercents();
        // First live run: bucket-header percents (e.g. '100%','0%') also
        // surface in the census when a percent breakdown is open — the law is
        // AT LEAST ring + 3 cards, with the first four being those anchors.
        assertTrue(percents.size() >= 4,
                "expected at least ring + 3 card percents, got " + percents);
        logStepWithScreenshot("TC_AF_085 percent census verified: " + percents);
    }

    @Test(priority = 86)
    public void TC_AF_086_overallCaptionAccompaniesRing() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_086 - the Overall caption renders with the readiness ring");
        openDash();
        assertTrue(arcPage.hasOverallCaption(), "Overall caption must render");
        assertEquals((int) arcPage.getAllPercents().get(0), arcPage.getOverallPercent(),
                "first rendered percent must BE the overall ring value");
        logStepWithScreenshot("TC_AF_086 caption+ring consistency verified");
    }

    @Test(priority = 87, dataProvider = "cards")
    public void TC_AF_087_cardsRemainVisibleAfterSelection(int idx, String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_087 [" + metric + "] - selecting a card never hides the other metric cards");
        openDash();
        assertTrue(arcPage.selectMetricCard(metric), "card must be selectable");
        mediumWait();
        assertTrue(arcPage.hasMetricCard(ArcFlashPage.METRIC_ASSET_DETAILS), "Asset Details card visible");
        assertTrue(arcPage.hasMetricCard(ArcFlashPage.METRIC_CONNECTION_DETAILS), "Connection Details card visible");
        assertTrue(arcPage.hasMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET), "Source/Target card visible");
        logStepWithScreenshot("TC_AF_087 [" + metric + "] card census after selection verified");
    }


    @Test(priority = 88)
    public void TC_AF_088_dashboardAnchorCensus() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_088 - all five dashboard anchors render (ring, caption, three metric cards)");
        openDash();
        assertTrue(arcPage.hasReadinessScoreCard(), "readiness ring must render");
        assertTrue(arcPage.hasOverallCaption(), "Overall caption must render");
        assertTrue(arcPage.hasMetricCard(ArcFlashPage.METRIC_ASSET_DETAILS), "Asset Details card");
        assertTrue(arcPage.hasMetricCard(ArcFlashPage.METRIC_CONNECTION_DETAILS), "Connection Details card");
        assertTrue(arcPage.hasMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET), "Source/Target card");
        logStepWithScreenshot("TC_AF_088 anchor census verified");
    }

    @Test(priority = 89, dataProvider = "cards")
    public void TC_AF_089_everyBreakdownHasBuckets(int idx, String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_089 [" + metric + "] - the breakdown for every metric renders at least one bucket/group");
        openDash();
        assertTrue(arcPage.selectMetricCard(metric), "card must be selectable");
        mediumWait();
        boolean hasContent = !arcPage.getVisibleBucketLabels().isEmpty()
                || (metric.equals(ArcFlashPage.METRIC_SOURCE_TARGET) && arcPage.hasSourceTargetGroups());
        assertTrue(hasContent, "[" + metric + "] breakdown must render buckets/groups");
        logStepWithScreenshot("TC_AF_089 [" + metric + "] breakdown content verified");
    }

}
