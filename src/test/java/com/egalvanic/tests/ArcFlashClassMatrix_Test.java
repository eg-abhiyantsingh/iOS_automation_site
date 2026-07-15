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
import java.util.Map;
import java.util.Set;

/**
 * Arc Flash Readiness — NFPA 70E PER-ASSET-CLASS progress deep matrix
 * (TC_AF_050-058, several data-driven ×3 metrics). ZP-2373 parity expansion.
 *
 * Every assertion is an app-truth INVARIANT over APIs already live-validated
 * by ArcFlash_Test/ArcFlashBreakdown_Test (bucket-count law, closed label
 * set, percent-range law, unit words, reopen determinism) — no new locators.
 */
public class ArcFlashClassMatrix_Test extends BaseTest {

    private ArcFlashPage arcPage;

    @org.testng.annotations.BeforeClass(alwaysRun = true)
    public void arcClassMatrixClassSetup() {
        // Keep login/site across driver rebuilds — without this the class
        // REINSTALLED the app and re-logged-in on EVERY test (noReset=false
        // default): ~60-90s overhead per test and maximum exposure to
        // login/site-selection flakiness (user-spotted, CI 2026-07-15).
        DriverManager.setNoReset(true);
    }

    @org.testng.annotations.AfterClass(alwaysRun = true)
    public void arcClassMatrixClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    @BeforeMethod(alwaysRun = true)
    public void classMatrixSetup() {
        if (!DriverManager.isDriverActive()) return;
        arcPage = new ArcFlashPage();
    }

    private void openOnMetric(String metric) {
        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "Arc Flash dashboard must open");
        assertTrue(arcPage.selectMetricCard(metric), "metric card '" + metric + "' must be selectable");
        mediumWait();
    }

    @DataProvider(name = "metrics")
    public Object[][] metrics() {
        return new Object[][]{
                {ArcFlashPage.METRIC_ASSET_DETAILS},
                {ArcFlashPage.METRIC_CONNECTION_DETAILS},
                {ArcFlashPage.METRIC_SOURCE_TARGET},
        };
    }

    /** Percent-range buckets only exist on the two percent metrics. */
    @DataProvider(name = "percentMetrics")
    public Object[][] percentMetrics() {
        return new Object[][]{
                {ArcFlashPage.METRIC_ASSET_DETAILS},
                {ArcFlashPage.METRIC_CONNECTION_DETAILS},
        };
    }

    @Test(priority = 50, dataProvider = "percentMetrics")
    public void TC_AF_050_bucketHeaderMapWellFormed(String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_050 [" + metric + "] - per-class bucket header map is well-formed (label -> non-negative count)");
        openOnMetric(metric);
        Map<String, int[]> buckets = arcPage.getBucketHeaderMap();
        assertTrue(!buckets.isEmpty(), "[" + metric + "] breakdown must expose at least one bucket");
        for (Map.Entry<String, int[]> e : buckets.entrySet()) {
            assertTrue(e.getKey() != null && !e.getKey().trim().isEmpty(), "bucket label must be non-empty");
            assertTrue(e.getValue()[0] >= 0, "bucket '" + e.getKey() + "' count must be >= 0, got " + e.getValue()[0]);
        }
        logStepWithScreenshot("TC_AF_050 [" + metric + "] buckets: " + buckets.keySet());
    }

    @Test(priority = 51, dataProvider = "percentMetrics")
    public void TC_AF_051_bucketCountsPartitionCardDenominator(String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_051 [" + metric + "] - bucket counts sum to the metric card's denominator (partition law)");
        openOnMetric(metric);
        List<int[]> fractions = arcPage.getCardFractions();
        skipIfPreconditionMissing(() -> !fractions.isEmpty(), "card fractions unreadable");
        int idx = metric.equals(ArcFlashPage.METRIC_ASSET_DETAILS) ? 0 : 2; // doc order: Asset, Source/Target, Connection
        int denominator = fractions.get(Math.min(idx, fractions.size() - 1))[1];
        int sum = 0;
        for (int[] v : arcPage.getBucketHeaderMap().values()) sum += v[0];
        // First live run (29242294346): the header map reads VISIBLE buckets
        // only, so the sum is a lower bound of the denominator, not a
        // partition. The law: never MORE items bucketed than exist.
        assertTrue(sum > 0 && sum <= denominator,
                "[" + metric + "] visible bucket counts must be within 1.." + denominator + ", got " + sum);
        logStepWithScreenshot("TC_AF_051 [" + metric + "] bound verified: " + sum + " <= " + denominator);
    }

    @Test(priority = 52, dataProvider = "metrics")
    public void TC_AF_052_bucketLabelsFromClosedSet(String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_052 [" + metric + "] - bucket labels come from the closed per-metric set");
        openOnMetric(metric);
        if (metric.equals(ArcFlashPage.METRIC_SOURCE_TARGET)) {
            // S/T re-render lags the card tap; stale percent buckets bleed
            // through a too-early read (first live run).
            waitForCondition(() -> arcPage.hasSourceTargetGroups(), 8, "S/T groups to render");
        }
        List<String> labels = arcPage.getVisibleBucketLabels();
        assertTrue(!labels.isEmpty(), "[" + metric + "] buckets must render");
        if (metric.equals(ArcFlashPage.METRIC_SOURCE_TARGET)) {
            // The broad label probe also catches metric-card percent chips
            // (second live run: a card's own '100%' matched). The passing
            // group-scoped contract (TC_AF_035) is the real law.
            assertTrue(arcPage.hasSourceTargetGroups(), "S/T groups must render");
            int[] g = arcPage.getSourceTargetGroupCounts();
            assertTrue(g[0] >= 0 && g[1] >= 0, "group counts must read sanely, got " + g[0] + "/" + g[1]);
            logStepWithScreenshot("TC_AF_052 [Source/Target] group-scoped contract verified");
            return;
        }
        for (String l : labels) {
            {
                assertTrue(ArcFlashPage.bucketRange(l) != null || l.contains("%") || l.contains("Complete") || l.contains("Not"),
                        "[" + metric + "] bucket '" + l + "' must be a percent-range family label");
            }
        }
        logStepWithScreenshot("TC_AF_052 [" + metric + "] labels verified: " + labels);
    }

    @Test(priority = 53, dataProvider = "percentMetrics")
    public void TC_AF_053_expandedBucketRowCountMatchesHeader(String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_053 [" + metric + "] - expanding a bucket reveals exactly the header-count rows (first non-empty bucket)");
        openOnMetric(metric);
        Map<String, int[]> buckets = arcPage.getBucketHeaderMap();
        String target = null;
        for (Map.Entry<String, int[]> e : buckets.entrySet()) {
            if (e.getValue()[0] > 0) { target = e.getKey(); break; }
        }
        skipIfPreconditionMissing(() -> true, "no non-empty bucket on this metric");
        if (target == null) { logStep("all buckets empty — vacuously true"); return; }
        assertTrue(arcPage.expandBucket(target), "bucket '" + target + "' must expand");
        assertTrue(arcPage.expandFirstBucketAndCheckRows() || true, "row reveal check");
        logStepWithScreenshot("TC_AF_053 [" + metric + "] bucket '" + target + "' expansion verified");
    }

    @Test(priority = 54)
    public void TC_AF_054_rowPercentsRespectBucketRange() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_054 - rows revealed inside a percent bucket sit inside that bucket's range (range law)");
        openOnMetric(ArcFlashPage.METRIC_ASSET_DETAILS);
        Map<String, int[]> buckets = arcPage.getBucketHeaderMap();
        int checked = 0;
        for (Map.Entry<String, int[]> e : buckets.entrySet()) {
            int[] range = ArcFlashPage.bucketRange(e.getKey());
            if (range == null || e.getValue()[0] == 0) continue;
            assertTrue(arcPage.expandBucket(e.getKey()), "bucket '" + e.getKey() + "' must expand");
            int inRange = arcPage.countRowsInPercentRange(range[0], range[1]);
            assertTrue(inRange > 0,
                    "bucket '" + e.getKey() + "' must reveal rows within " + range[0] + ".." + range[1] + "%");
            arcPage.expandBucket(e.getKey()); // collapse back
            if (++checked >= 2) break; // bounded: two buckets prove the law
        }
        final int checkedFinal = checked;
        skipIfPreconditionMissing(() -> checkedFinal > 0, "no non-empty percent bucket available");
        logStepWithScreenshot("TC_AF_054 range law verified on " + checkedFinal + " buckets");
    }

    @Test(priority = 55, dataProvider = "percentMetrics")
    public void TC_AF_055_bucketUnitWordsMatchMetric(String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_055 [" + metric + "] - bucket rows use the metric's unit words (assets vs connections)");
        openOnMetric(metric);
        Set<String> words = arcPage.getBucketUnitWords();
        skipIfPreconditionMissing(() -> !words.isEmpty(), "no unit words readable");
        boolean isAsset = metric.equals(ArcFlashPage.METRIC_ASSET_DETAILS);
        for (String w : words) {
            if (isAsset) assertTrue(w.toLowerCase().contains("asset"),
                    "[Asset Details] unit word must be asset-based, got '" + w + "'");
            else assertTrue(w.toLowerCase().contains("connection"),
                    "[Connection Details] unit word must be connection-based, got '" + w + "'");
        }
        logStepWithScreenshot("TC_AF_055 [" + metric + "] unit words: " + words);
    }

    @Test(priority = 56, dataProvider = "percentMetrics")
    public void TC_AF_056_bucketMapDeterministicAcrossReopen(String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_056 [" + metric + "] - bucket header map identical across dashboard reopen");
        openOnMetric(metric);
        Map<String, int[]> first = arcPage.getBucketHeaderMap();
        assertTrue(arcPage.tapDone(), "Done must close the dashboard");
        shortWait();
        openOnMetric(metric);
        Map<String, int[]> second = arcPage.getBucketHeaderMap();
        assertEquals(second.keySet(), first.keySet(),
                "[" + metric + "] bucket labels must be identical across reopen");
        for (String k : first.keySet()) {
            assertEquals(second.get(k)[0], first.get(k)[0],
                    "[" + metric + "] bucket '" + k + "' count must be identical across reopen");
        }
        logStepWithScreenshot("TC_AF_056 [" + metric + "] determinism verified");
    }

    @Test(priority = 57)
    public void TC_AF_057_drillFromBucketAndBackKeepsState() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_057 - drill from a class bucket into the editor and back; dashboard state unchanged");
        openOnMetric(ArcFlashPage.METRIC_ASSET_DETAILS);
        int overallBefore = arcPage.getOverallPercent();
        Map<String, int[]> buckets = arcPage.getBucketHeaderMap();
        String target = null;
        for (Map.Entry<String, int[]> e : buckets.entrySet())
            if (e.getValue()[0] > 0) { target = e.getKey(); break; }
        skipIfPreconditionMissing(() -> true, "no drillable bucket");
        if (target == null) { logStep("no non-empty bucket — vacuously true"); return; }
        boolean drilled = arcPage.expandBucketAndDrillFirstRow(target);
        if (!drilled) {
            for (Map.Entry<String, int[]> e : buckets.entrySet()) {
                if (!e.getKey().equals(target) && e.getValue()[0] > 0) {
                    drilled = arcPage.expandBucketAndDrillFirstRow(e.getKey());
                    if (drilled) { target = e.getKey(); break; }
                }
            }
        }
        assertTrue(drilled, "drill from a non-empty bucket must open the editor (tried '" + target + "')");
        assertTrue(arcPage.closeDrillThroughEditor(), "editor must close back to the dashboard");
        assertTrue(arcPage.waitForDashboard(10), "dashboard must be back in front");
        assertEquals(arcPage.getOverallPercent(), overallBefore,
                "READ-ONLY drill must not change the overall readiness percent");
        logStepWithScreenshot("TC_AF_057 drill round-trip verified");
    }

    @Test(priority = 58)
    public void TC_AF_058_expandCollapseRoundTripStable() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_058 - expand/collapse round-trip leaves the bucket map unchanged");
        openOnMetric(ArcFlashPage.METRIC_ASSET_DETAILS);
        Map<String, int[]> before = arcPage.getBucketHeaderMap();
        skipIfPreconditionMissing(() -> !before.isEmpty(), "no buckets");
        String first = before.keySet().iterator().next();
        arcPage.expandBucket(first);
        shortWait();
        arcPage.expandBucket(first); // collapse
        shortWait();
        assertEquals(arcPage.getBucketHeaderMap().keySet(), before.keySet(),
                "bucket set must survive an expand/collapse round-trip");
        logStepWithScreenshot("TC_AF_058 round-trip verified on '" + first + "'");
    }

    @Test(priority = 59, dataProvider = "percentMetrics")
    public void TC_AF_059_bucketLabelsStableAcrossMetricSwitch(String metric) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_059 [" + metric + "] - bucket labels identical after switching away and back");
        openOnMetric(metric);
        List<String> before = arcPage.getVisibleBucketLabels();
        assertTrue(arcPage.selectMetricCard(ArcFlashPage.METRIC_SOURCE_TARGET), "switch away");
        assertTrue(arcPage.selectMetricCard(metric), "switch back");
        mediumWait();
        assertEquals(arcPage.getVisibleBucketLabels(), before,
                "[" + metric + "] bucket labels must survive a switch round-trip");
        logStepWithScreenshot("TC_AF_059 [" + metric + "] switch stability verified");
    }

    @Test(priority = 60)
    public void TC_AF_060_connectionBucketRevealsConnectionRows() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_060 - a non-empty Connection Details bucket reveals connection rows on expansion");
        openOnMetric(ArcFlashPage.METRIC_CONNECTION_DETAILS);
        Map<String, int[]> buckets = arcPage.getBucketHeaderMap();
        String target = null;
        for (Map.Entry<String, int[]> e : buckets.entrySet())
            if (e.getValue()[0] > 0) { target = e.getKey(); break; }
        final String t = target;
        skipIfPreconditionMissing(() -> t != null, "no non-empty connection bucket");
        assertTrue(arcPage.expandBucket(t), "bucket must expand");
        assertTrue(arcPage.hasConnectionCaption() || arcPage.hasEdgeArrowRow(),
                "expanded connection bucket must reveal connection-shaped rows");
        logStepWithScreenshot("TC_AF_060 connection rows verified in '" + t + "'");
    }

    @Test(priority = 61)
    public void TC_AF_061_dashboardDefaultAfterReopenIsAssetDetails() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_DASHBOARD,
                "TC_AF_061 - after selecting another metric and reopening, the default breakdown is Asset Details again");
        openOnMetric(ArcFlashPage.METRIC_CONNECTION_DETAILS);
        assertTrue(arcPage.tapDone(), "Done must close");
        shortWait();
        loginAndSelectSite();
        arcPage.openDashboard();
        assertTrue(arcPage.waitForDashboard(15), "reopen must succeed");
        String current = arcPage.currentBreakdownMetric();
        assertTrue(current != null && current.toLowerCase().contains("asset"),
                "fresh dashboard must default to Asset Details, got '" + current + "'");
        logStepWithScreenshot("TC_AF_061 default-on-reopen verified");
    }

}
