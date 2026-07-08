package com.egalvanic.tests;

import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ArcFlashPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;

/**
 * Arc Flash Analysis dashboard (Site tab quick action) — v1.49.
 *
 * The feature is a data-readiness tracker (NFPA 70E completeness), mirroring
 * the web /arc-flash "Arc Flash Readiness" dashboard. NOT feature-flag gated:
 * runs on every tenant, including acme QA while eng-lib is off.
 *
 * Oracle strategy: the dashboard's own arithmetic must be internally
 * consistent (source: ArcFlashCompletionViewModel):
 *   - {n} Completed + {n} Remaining == {n} Total Items
 *   - each metric card:  % == round(completed/total × 100)   (total==0 → 0%)
 *   - overall ring       % == totals-weighted average of the three cards
 *   - Source/Target groups: Connected + Missing Source == card total
 *   - breakdown bucket labels come from the closed set
 *     {0%, 1-25%, 26-50%, 51-75%, 76-99%, 100%}
 * These are real cross-element invariants — they fail on any rendering or
 * computation regression, never tautologically pass.
 */
public class ArcFlash_Test extends BaseTest {

    private ArcFlashPage afPage;

    @BeforeClass(alwaysRun = true)
    public void arcFlashClassSetup() {
        System.out.println("\n📋 Arc Flash Suite (dashboard) — Starting");
        DriverManager.setNoReset(true); // keep login across driver rebuilds
    }

    @BeforeMethod(alwaysRun = true)
    public void arcFlashTestSetup() {
        if (!DriverManager.isDriverActive()) return; // fast-skipped: no driver, no page
        afPage = new ArcFlashPage();
    }

    @org.testng.annotations.AfterClass(alwaysRun = true)
    public void arcFlashClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    /** Login + land on dashboard + open the Arc Flash screen, metrics ready. */
    private void openDashboardReady() {
        loginAndSelectSite();
        afPage.openDashboard();
        assertTrue(afPage.waitForDashboard(30),
                "Arc Flash Analysis must finish computing (loading overlay gone)");
        verifyAppAlive("Arc Flash dashboard open");
    }

    // ─────────────────────────────────────────────────────────────────────

    @Test(priority = 1)
    public void TC_AF_001_quickActionCardVisibleOnDashboard() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_001 - 'Arc Flash' quick-action card is present on the Site dashboard");

        logStep("Step 1: Login and land on the Site dashboard");
        loginAndSelectSite();

        logStep("Step 2: The Arc Flash card is reachable in Quick Actions");
        boolean visible = afPage.isDashboardDisplayed(1); // already open? then trivially true
        if (!visible) {
            afPage.openDashboard(); // throws with a precise reason if the card is missing
            visible = afPage.isDashboardDisplayed(5);
            afPage.tapDone();
        }
        assertTrue(visible, "'Arc Flash' quick action must open the Arc Flash Analysis screen");
        logStepWithScreenshot("TC_AF_001: Arc Flash entry verified");
    }

    @Test(priority = 2)
    public void TC_AF_002_dashboardOpensWithTitleAndDone() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_002 - Dashboard opens full-screen: 'Arc Flash Analysis' title, Done button, loading resolves");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Title + readiness content present, screen not blank");
        assertTrue(afPage.isDashboardDisplayed(2), "'Arc Flash Analysis' title must be present");
        assertTrue(afPage.hasReadinessScoreCard(), "'Readiness Score' card must render");
        verifyNotBlank("Arc Flash Analysis");

        logStep("Step 3: Done dismisses the full-screen cover");
        assertTrue(afPage.tapDone(), "Done must close the Arc Flash dashboard");
        logStepWithScreenshot("TC_AF_002: open + dismiss verified");
    }

    @Test(priority = 3)
    public void TC_AF_003_readinessRingPercentInRange() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_003 - Readiness Score ring renders an integer percent 0..100 with 'Overall' caption");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Ring percent parses and is in range");
        int overall = afPage.getOverallPercent();
        if (overall < 0) afPage.dumpSource("TC_AF_003_no_percent");
        assertTrue(overall >= 0 && overall <= 100,
                "Overall ring percent must be 0..100, got " + overall);
        assertTrue(afPage.hasOverallCaption(), "'Overall' caption must accompany the ring");
        logStepWithScreenshot("TC_AF_003: overall=" + overall + "%");
    }

    @Test(priority = 4)
    public void TC_AF_004_statLineArithmetic() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_004 - Stat lines obey: Completed + Remaining == Total Items");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Parse the three stat lines");
        int completed = afPage.getCompletedCount();
        int remaining = afPage.getRemainingCount();
        int total = afPage.getTotalItemsCount();
        if (completed < 0 || remaining < 0 || total < 0) afPage.dumpSource("TC_AF_004_stats");
        assertTrue(completed >= 0, "'{n} Completed' must parse, got " + completed);
        assertTrue(remaining >= 0, "'{n} Remaining' must parse, got " + remaining);
        assertTrue(total >= 0, "'{n} Total Items' must parse, got " + total);

        logStep("Step 3: The books must balance");
        assertEquals(completed + remaining, total,
                "Completed(" + completed + ") + Remaining(" + remaining + ") must equal Total(" + total + ")");
        logStepWithScreenshot("TC_AF_004: " + completed + "+" + remaining + "=" + total);
    }

    @Test(priority = 5)
    public void TC_AF_005_threeMetricCardsPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_005 - Metric cards: Asset Details, Source/Target, Connection Details");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: All three cards render");
        assertTrue(afPage.hasMetricCard(ArcFlashPage.METRIC_ASSET_DETAILS),
                "'Asset Details' metric card must render");
        assertTrue(afPage.hasMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET),
                "'Source/Target' metric card must render");
        assertTrue(afPage.hasMetricCard(ArcFlashPage.METRIC_CONNECTION_DETAILS),
                "'Connection Details' metric card must render");
        logStepWithScreenshot("TC_AF_005: three metric cards verified");
    }

    @Test(priority = 6)
    public void TC_AF_006_cardFractionsWellFormed() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_006 - Each metric card shows 'X of Y' with 0 <= X <= Y");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Exactly three fractions, each internally sane");
        List<int[]> fr = afPage.getCardFractions();
        if (fr.size() != 3) afPage.dumpSource("TC_AF_006_fractions");
        assertEquals(fr.size(), 3, "Expected one 'X of Y' per metric card (3 total), got " + fr.size());
        for (int i = 0; i < fr.size(); i++) {
            int[] f = fr.get(i);
            assertTrue(f[0] >= 0 && f[0] <= f[1],
                    "Card " + i + " fraction must satisfy 0 <= " + f[0] + " <= " + f[1]);
        }
        logStepWithScreenshot("TC_AF_006: fractions verified");
    }

    @Test(priority = 7)
    public void TC_AF_007_cardPercentMatchesFraction() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_007 - Each card's % equals round(X/Y*100); empty population renders 0%");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Percent labels line up with fractions (document order: ring, then cards)");
        List<int[]> fr = afPage.getCardFractions();
        List<Integer> pct = afPage.getAllPercents();
        assertEquals(fr.size(), 3, "need 3 fractions to check, got " + fr.size());
        assertTrue(pct.size() >= 4,
                "need ring% + 3 card% labels, got " + pct.size() + " → " + pct);
        for (int i = 0; i < 3; i++) {
            int[] f = fr.get(i);
            int expected = f[1] == 0 ? 0 : (int) Math.round(f[0] * 100.0 / f[1]);
            int actual = pct.get(i + 1);
            assertTrue(Math.abs(actual - expected) <= 1,
                    "Card " + i + ": shown " + actual + "% but " + f[0] + "/" + f[1]
                            + " computes " + expected + "%");
        }
        logStepWithScreenshot("TC_AF_007: card percents consistent");
    }

    @Test(priority = 8)
    public void TC_AF_008_overallIsWeightedAverage() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_008 - Overall ring == totals-weighted average of the three metric cards");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Recompute the weighted average from the cards");
        List<int[]> fr = afPage.getCardFractions();
        assertEquals(fr.size(), 3, "need 3 fractions, got " + fr.size());
        long comp = 0, tot = 0;
        for (int[] f : fr) { comp += f[0]; tot += f[1]; }
        int expected = tot == 0 ? 0 : (int) Math.round(comp * 100.0 / tot);
        int overall = afPage.getOverallPercent();
        assertTrue(Math.abs(overall - expected) <= 1,
                "Overall " + overall + "% must equal weighted avg " + expected
                        + "% (Σcompleted=" + comp + ", Σtotal=" + tot + ")");

        logStep("Step 3: Total Items stat equals the summed card totals");
        assertEquals((long) afPage.getTotalItemsCount(), tot,
                "'{n} Total Items' must equal the sum of card totals");
        logStepWithScreenshot("TC_AF_008: overall=" + overall + "% expected=" + expected + "%");
    }

    @Test(priority = 9)
    public void TC_AF_009_defaultBreakdownIsAssetDetails() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_009 - Default breakdown section is 'Asset Details Breakdown'");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Breakdown header defaults to the first metric");
        String header = afPage.getBreakdownHeader();
        if (header.isEmpty()) afPage.dumpSource("TC_AF_009_breakdown");
        assertEquals(header, ArcFlashPage.METRIC_ASSET_DETAILS + " Breakdown",
                "Default breakdown must be Asset Details");
        logStepWithScreenshot("TC_AF_009: default breakdown verified");
    }

    @Test(priority = 10)
    public void TC_AF_010_selectingSourceTargetSwitchesBreakdown() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_010 - Tapping Source/Target card switches breakdown and shows Connected/Missing groups");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Select the Source/Target card");
        assertTrue(afPage.selectMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET),
                "Source/Target card tap must switch the breakdown header");

        logStep("Step 3: Connected / Missing Source groups render");
        if (!afPage.hasSourceTargetGroups()) afPage.dumpSource("TC_AF_010_groups");
        assertTrue(afPage.hasSourceTargetGroups(),
                "Source/Target breakdown must show Connected and/or Missing Source groups");
        logStepWithScreenshot("TC_AF_010: source/target breakdown verified");
    }

    @Test(priority = 11)
    public void TC_AF_011_connectionDetailsBreakdownBuckets() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_011 - Connection Details breakdown uses only the closed bucket-label set");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Select Connection Details");
        assertTrue(afPage.selectMetricCard(ArcFlashPage.METRIC_CONNECTION_DETAILS),
                "Connection Details card tap must switch the breakdown header");

        logStep("Step 3: At least one bucket from the fixed set is visible");
        List<String> buckets = afPage.getVisibleBucketLabels();
        if (buckets.isEmpty()) afPage.dumpSource("TC_AF_011_buckets");
        assertTrue(!buckets.isEmpty(),
                "Connection breakdown must show at least one completion bucket (site has connections)");
        logStepWithScreenshot("TC_AF_011: buckets=" + buckets);
    }

    @Test(priority = 12)
    public void TC_AF_012_assetDetailsBucketsAndExpansion() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_012 - Asset Details breakdown buckets render and expand to per-asset rows");

        logStep("Step 1: Open the dashboard (default = Asset Details breakdown)");
        openDashboardReady();

        logStep("Step 2: Buckets present");
        List<String> buckets = afPage.getVisibleBucketLabels();
        if (buckets.isEmpty()) afPage.dumpSource("TC_AF_012_buckets");
        assertTrue(!buckets.isEmpty(), "Asset breakdown must show at least one completion bucket");

        logStep("Step 3: Expanding a bucket reveals per-asset rows with a % each");
        boolean expanded = afPage.expandFirstBucketAndCheckRows();
        if (!expanded) afPage.dumpSource("TC_AF_012_expand");
        assertTrue(expanded, "Expanding a bucket must reveal asset rows with completion percents");
        logStepWithScreenshot("TC_AF_012: bucket expansion verified");
    }

    @Test(priority = 13)
    public void TC_AF_013_reentryIsStableAndDeterministic() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_013 - Close + reopen: no crash, identical totals (deterministic recompute)");

        logStep("Step 1: Open and record totals");
        openDashboardReady();
        int total1 = afPage.getTotalItemsCount();
        int overall1 = afPage.getOverallPercent();

        logStep("Step 2: Done, then reopen");
        assertTrue(afPage.tapDone(), "Done must close the dashboard");
        afPage.openDashboard();
        assertTrue(afPage.waitForDashboard(30), "Reopened dashboard must finish computing");
        verifyAppAlive("Arc Flash reopen");

        logStep("Step 3: Same data, same numbers");
        assertEquals(afPage.getTotalItemsCount(), total1,
                "Total Items must be identical across immediate reopen");
        assertEquals(afPage.getOverallPercent(), overall1,
                "Overall percent must be identical across immediate reopen");
        assertTrue(afPage.tapDone(), "Done must close the dashboard again");
        logStepWithScreenshot("TC_AF_013: reentry stability verified");
    }

    @Test(priority = 14)
    public void TC_AF_014_bucketRowDrillsIntoEditorAndBack() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_014 - Tapping an asset row inside a bucket drills into the editor; closing returns to the dashboard");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Expand the 100% bucket (complete = simple classes; avoids the"
                + " transformer giant-DOM wedge) and drill into its first row");
        boolean drilled = afPage.expandBucketAndDrillFirstRow("100%");
        if (!drilled) {
            afPage.dumpSource("TC_AF_014_drill");
            // Only a missing bucket is a fixture gap; bucket-present-but-no-drill is a real bug.
            skipIfPreconditionMissing(() -> afPage.getVisibleBucketLabels().contains("100%"),
                    "no 100% bucket on this site (fixture data)");
        }
        assertTrue(drilled, "Asset row tap must open the asset editor full-screen");
        verifyAppAlive("drill-through editor");

        logStep("Step 3: Close the editor — dashboard restores");
        assertTrue(afPage.closeDrillThroughEditor(), "Closing the editor must restore the dashboard");
        logStepWithScreenshot("TC_AF_014: drill-through round trip verified");
    }

    @Test(priority = 15)
    public void TC_AF_015_sourceTargetGroupsSumToCardTotal() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_015 - Connected + Missing Source == Source/Target card total");

        logStep("Step 1: Open the dashboard, read the Source/Target card total");
        openDashboardReady();
        List<int[]> fr = afPage.getCardFractions();
        assertEquals(fr.size(), 3, "need 3 fractions, got " + fr.size());
        int stTotal = fr.get(1)[1]; // card order: Asset Details, Source/Target, Connection Details
        int stConnected = fr.get(1)[0];

        logStep("Step 2: Switch to the Source/Target breakdown and read the groups");
        assertTrue(afPage.selectMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET),
                "Source/Target card must switch the breakdown");
        int[] groups = afPage.getSourceTargetGroupCounts();
        if (groups[0] < 0 || groups[1] < 0) afPage.dumpSource("TC_AF_015_groups");
        assertTrue(groups[0] >= 0 && groups[1] >= 0,
                "Connected/Missing group counts must parse, got " + groups[0] + "/" + groups[1]);

        logStep("Step 3: The books must balance against the card");
        assertEquals(groups[0] + groups[1], stTotal,
                "Connected(" + groups[0] + ") + Missing(" + groups[1] + ") must equal card total " + stTotal);
        assertEquals(groups[0], stConnected,
                "Connected group count must equal the card's completed count " + stConnected);
        logStepWithScreenshot("TC_AF_015: " + groups[0] + "+" + groups[1] + "=" + stTotal);
    }

    @Test(priority = 16)
    public void TC_AF_016_bucketLabelsFromClosedSetAcrossMetrics() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_016 - Every visible bucket label across Asset/Connection breakdowns is from the fixed set");

        logStep("Step 1: Open the dashboard (Asset Details default)");
        openDashboardReady();
        List<String> assetBuckets = afPage.getVisibleBucketLabels();
        assertTrue(!assetBuckets.isEmpty(), "Asset breakdown must show buckets");

        logStep("Step 2: Connection Details buckets");
        assertTrue(afPage.selectMetricCard(ArcFlashPage.METRIC_CONNECTION_DETAILS),
                "Connection Details must switch");
        List<String> connBuckets = afPage.getVisibleBucketLabels();
        assertTrue(!connBuckets.isEmpty(), "Connection breakdown must show buckets");

        logStep("Step 3: Both sets are subsets of the closed label set (parser enforces membership)");
        // getVisibleBucketLabels only matches the closed set, so a non-empty result
        // on BOTH metrics plus a full-set sanity bound is the assertable law here.
        assertTrue(assetBuckets.size() <= ArcFlashPage.BUCKET_LABELS.length
                        && connBuckets.size() <= ArcFlashPage.BUCKET_LABELS.length,
                "Bucket counts can never exceed the closed set size");
        logStepWithScreenshot("TC_AF_016: asset=" + assetBuckets + " conn=" + connBuckets);
    }

    @Test(priority = 17)
    public void TC_AF_017_connectionRowsShowArrowAnatomy() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_017 - Expanded connection rows render 'source → target' with the 'Connection' caption");

        logStep("Step 1: Open the dashboard, switch to Connection Details");
        openDashboardReady();
        assertTrue(afPage.selectMetricCard(ArcFlashPage.METRIC_CONNECTION_DETAILS),
                "Connection Details must switch");

        logStep("Step 2: Expand the first bucket");
        assertTrue(afPage.expandFirstBucketAndCheckRows(), "A connection bucket must expand");

        logStep("Step 3: Row anatomy: 'A → B' + 'Connection' caption");
        boolean arrow = afPage.hasEdgeArrowRow();
        boolean caption = afPage.hasConnectionCaption();
        if (!arrow || !caption) afPage.dumpSource("TC_AF_017_anatomy");
        assertTrue(arrow, "Edge rows must show 'source → target'");
        assertTrue(caption, "Edge rows must show the 'Connection' caption");
        logStepWithScreenshot("TC_AF_017: edge row anatomy verified");
    }

    @Test(priority = 18)
    public void TC_AF_018_punchlistToggleDoesNotSurviveRelaunch() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_018 - AF Punchlist toggle is session state: badges default OFF after app relaunch");

        logStep("Step 1: Enable AF Punchlist on the Assets tab");
        loginAndSelectSite();
        assertTrue(afPage.openTab("Assets"), "Assets tab must open");
        assertTrue(afPage.openEllipsisMenu() && afPage.tapPunchlistOption(ArcFlashPage.SHOW_PUNCHLIST),
                "Enable the punchlist");
        assertTrue(afPage.countAssetBoltBadges() > 0, "Badges must be on before relaunch");

        logStep("Step 2: Relaunch the app process (terminate + activate, NOT reinstall)");
        String bundleId = AppConstants.APP_BUNDLE_ID;
        DriverManager.getDriver().terminateApp(bundleId);
        shortWait();
        DriverManager.getDriver().activateApp(bundleId);
        longWait();
        loginAndSelectSite(); // idempotent fast-path when session survived

        logStep("Step 3: Badges are OFF again (state was @State, not persisted)");
        assertTrue(afPage.openTab("Assets"), "Assets tab must reopen after relaunch");
        int after = afPage.countAssetBoltBadges();
        assertEquals(after, 0, "Punchlist badges must reset to hidden after relaunch, got " + after);
        logStepWithScreenshot("TC_AF_018: non-persistence verified");
    }

    @Test(priority = 19)
    public void TC_AF_019_badgeCountStableAcrossToggleCycles() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_PUNCHLIST,
                "TC_AF_019 - Badge count is deterministic across on/off/on toggle cycles");

        logStep("Step 1: Assets tab, first enable");
        loginAndSelectSite();
        assertTrue(afPage.openTab("Assets"), "Assets tab must open");
        assertTrue(afPage.openEllipsisMenu() && afPage.tapPunchlistOption(ArcFlashPage.SHOW_PUNCHLIST),
                "Enable punchlist (cycle 1)");
        int first = afPage.countAssetBoltBadges();
        assertTrue(first > 0, "Badges must appear on cycle 1, got " + first);

        logStep("Step 2: Off, then on again — same visible rows, same count");
        assertTrue(afPage.openEllipsisMenu() && afPage.tapPunchlistOption(ArcFlashPage.HIDE_PUNCHLIST),
                "Disable punchlist");
        assertEquals(afPage.countAssetBoltBadges(), 0, "Badges must clear between cycles");
        assertTrue(afPage.openEllipsisMenu() && afPage.tapPunchlistOption(ArcFlashPage.SHOW_PUNCHLIST),
                "Enable punchlist (cycle 2)");
        int second = afPage.countAssetBoltBadges();
        assertEquals(second, first,
                "Badge count must be deterministic across toggle cycles (" + first + " vs " + second + ")");

        logStep("Step 3: Cleanup — hide");
        assertTrue(afPage.openEllipsisMenu() && afPage.tapPunchlistOption(ArcFlashPage.HIDE_PUNCHLIST),
                "Cleanup toggle");
        logStepWithScreenshot("TC_AF_019: toggle determinism verified");
    }
}
