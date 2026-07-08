package com.egalvanic.tests;

import java.util.Map;
import java.util.Set;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ArcFlashPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;

/**
 * Arc Flash breakdown deep-laws (v1.49, changelog 112). Complements
 * ArcFlash_Test with structural invariants read from the FOLDED accessibility
 * names (live DOM shape: bucket header Button "0%, 3, assets", child row
 * Button "Transformer-1, 0%", metric card "Asset Details, 8 of 15, 53%"):
 *
 *  - metric-switch matrix: every card switches the breakdown header
 *  - bucket-count law: a header advertising N items reveals exactly N child
 *    rows whose trailing percent lies inside the bucket's range
 *  - unit-word law: asset breakdowns say "asset(s)", connection breakdowns
 *    say "connection(s)" — never mixed
 *  - determinism: bucket header map identical across a close/reopen cycle
 */
public class ArcFlashBreakdown_Test extends BaseTest {

    private ArcFlashPage afPage;

    @BeforeClass(alwaysRun = true)
    public void breakdownClassSetup() {
        System.out.println("\n📋 Arc Flash Suite (breakdown deep-laws) — Starting");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void breakdownTestSetup() {
        if (!DriverManager.isDriverActive()) return; // fast-skipped: no driver, no page
        afPage = new ArcFlashPage();
    }

    @AfterClass(alwaysRun = true)
    public void breakdownClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    private void openDashboardReady() {
        loginAndSelectSite();
        afPage.openDashboard();
        assertTrue(afPage.waitForDashboard(30),
                "Arc Flash Analysis must finish computing (loading overlay gone)");
        verifyAppAlive("Arc Flash dashboard open");
    }

    // ─────────────────────────────────────────────────────────────────────

    @DataProvider(name = "metricCards")
    public Object[][] metricCards() {
        return new Object[][] {
                { ArcFlashPage.METRIC_ASSET_DETAILS },
                { ArcFlashPage.METRIC_SOURCE_TARGET },
                { ArcFlashPage.METRIC_CONNECTION_DETAILS },
        };
    }

    @Test(priority = 30, dataProvider = "metricCards")
    public void TC_AF_030_everyMetricCardSwitchesBreakdown(String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_030 - Metric card '" + metric + "' switches the breakdown section to itself");

        logStep("Step 1: Open the dashboard");
        openDashboardReady();

        logStep("Step 2: Select '" + metric + "' and verify the header follows");
        assertTrue(afPage.selectMetricCard(metric),
                "'" + metric + "' card tap must switch the breakdown header");
        assertEquals(afPage.currentBreakdownMetric(), metric,
                "Breakdown header must name the selected metric");
        logStepWithScreenshot("TC_AF_030[" + metric + "]: switch verified");
    }

    @Test(priority = 31)
    public void TC_AF_031_bucketHeaderCountMatchesRevealedRows() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_031 - Each asset bucket advertising N items reveals exactly N rows in its percent range");

        logStep("Step 1: Open the dashboard (Asset Details default) and read bucket headers");
        openDashboardReady();
        Map<String, int[]> buckets = afPage.getBucketHeaderMap();
        if (buckets.isEmpty()) afPage.dumpSource("TC_AF_031_headers");
        assertTrue(!buckets.isEmpty(), "Asset breakdown must expose folded bucket headers");

        logStep("Step 2: Expand each bucket and count its revealed rows");
        for (Map.Entry<String, int[]> e : buckets.entrySet()) {
            String label = e.getKey();
            int advertised = e.getValue()[0];
            assertTrue(afPage.expandBucket(label), "Bucket '" + label + "' must expand");
            int[] range = ArcFlashPage.bucketRange(label);
            int revealed = afPage.countRowsInPercentRange(range[0], range[1]);
            assertEquals(revealed, advertised,
                    "Bucket '" + label + "' advertises " + advertised + " but revealed " + revealed
                            + " rows in range [" + range[0] + ".." + range[1] + "]");
            assertTrue(afPage.expandBucket(label), "Bucket '" + label + "' must collapse again");
        }
        logStepWithScreenshot("TC_AF_031: bucket-count law verified for " + buckets.keySet());
    }

    @Test(priority = 32)
    public void TC_AF_032_assetBucketsUseAssetUnitWords() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_032 - Asset Details buckets label their counts in 'asset(s)'");

        logStep("Step 1: Open the dashboard (Asset Details default)");
        openDashboardReady();

        logStep("Step 2: Every bucket unit word is asset/assets");
        Set<String> units = afPage.getBucketUnitWords();
        assertTrue(!units.isEmpty(), "Asset breakdown must expose bucket unit words");
        for (String u : units) {
            assertTrue(u.equals("asset") || u.equals("assets"),
                    "Asset breakdown bucket must count 'asset(s)', got '" + u + "' (units=" + units + ")");
        }
        logStepWithScreenshot("TC_AF_032: units=" + units);
    }

    @Test(priority = 33)
    public void TC_AF_033_connectionBucketsUseConnectionUnitWords() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_033 - Connection Details buckets label their counts in 'connection(s)'");

        logStep("Step 1: Open the dashboard, switch to Connection Details");
        openDashboardReady();
        assertTrue(afPage.selectMetricCard(ArcFlashPage.METRIC_CONNECTION_DETAILS),
                "Connection Details must switch");

        logStep("Step 2: Every bucket unit word is connection/connections");
        Set<String> units = afPage.getBucketUnitWords();
        if (units.isEmpty()) afPage.dumpSource("TC_AF_033_units");
        assertTrue(!units.isEmpty(), "Connection breakdown must expose bucket unit words");
        for (String u : units) {
            assertTrue(u.equals("connection") || u.equals("connections"),
                    "Connection breakdown bucket must count 'connection(s)', got '" + u + "' (units=" + units + ")");
        }
        logStepWithScreenshot("TC_AF_033: units=" + units);
    }

    @Test(priority = 34)
    public void TC_AF_034_bucketMapDeterministicAcrossReopen() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_034 - Bucket header map is identical across close/reopen (deterministic recompute)");

        logStep("Step 1: Open and snapshot the bucket map");
        openDashboardReady();
        Map<String, int[]> first = afPage.getBucketHeaderMap();
        assertTrue(!first.isEmpty(), "Bucket headers must be present");

        logStep("Step 2: Close, reopen, snapshot again");
        assertTrue(afPage.tapDone(), "Done must close the dashboard");
        afPage.openDashboard();
        assertTrue(afPage.waitForDashboard(30), "Reopened dashboard must finish computing");
        Map<String, int[]> second = afPage.getBucketHeaderMap();

        logStep("Step 3: Same buckets, same counts");
        assertEquals(second.keySet(), first.keySet(),
                "Bucket label set must be identical across reopen");
        for (String label : first.keySet()) {
            assertEquals(second.get(label)[0], first.get(label)[0],
                    "Bucket '" + label + "' count must be identical across reopen");
        }
        assertTrue(afPage.tapDone(), "Cleanup: close the dashboard");
        logStepWithScreenshot("TC_AF_034: determinism verified for " + first.keySet());
    }

    @Test(priority = 35)
    public void TC_AF_035_sourceTargetBreakdownHasNoPercentBuckets() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_035 - Source/Target breakdown groups by status, never by percent buckets");

        logStep("Step 1: Open the dashboard, switch to Source/Target");
        openDashboardReady();
        assertTrue(afPage.selectMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET),
                "Source/Target must switch");

        logStep("Step 2: Status groups render; percent buckets do not");
        assertTrue(afPage.hasSourceTargetGroups(),
                "Source/Target breakdown must show Connected/Missing Source groups");
        // Folded-header map, not getVisibleBucketLabels: a metric card reading
        // exactly "100%" would false-match the loose bucket-label probe.
        Map<String, int[]> buckets = afPage.getBucketHeaderMap();
        assertTrue(buckets.isEmpty(),
                "Source/Target breakdown must NOT render percent buckets, got " + buckets.keySet());
        logStepWithScreenshot("TC_AF_035: status-group anatomy verified");
    }
}
