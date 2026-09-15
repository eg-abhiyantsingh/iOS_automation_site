package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * ZP-3928 (iOS 1.56) §1 — Issues list "With Photos" / "Without Photos" filters.
 *
 * Build-verified: both literals live in the v1.63 binary as hardcoded SwiftUI strings
 * (they are NOT in Localizable.strings), so the locators match the literal text across
 * Button / StaticText / Other / Cell.
 *
 * The strongest oracle here is arithmetic, not presence: the two buckets must PARTITION
 * the list, so With + Without == All exactly. A presence-only check would pass even if
 * the filter did nothing.
 */
public final class ZP3928_IssuePhotoFilter_Test extends BaseTest {

    private static final String FEATURE = "Issues photo filter (ZP-3928)";

    private IssuePage issuePage;

    private IssuePage issues() {
        if (issuePage == null) issuePage = new IssuePage();
        return issuePage;
    }

    private void openIssues() {
        loginAndSelectSite();
        if (!issues().navigateToIssuesScreen()) {
            throw new SkipException("Issues screen did not open");
        }
        mediumWait();
    }

    private void requireFilter(String name) {
        if (!issues().isPhotoFilterDisplayed(name)) {
            throw new SkipException("'" + name + "' filter is not present on this build — "
                    + "ZP-3928 §1 is not shipped here");
        }
    }

    @Test(priority = 1)
    public void TC_IF_01_withPhotosFilterIsThere() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_IF_01 - 'With Photos' filter is present");
        openIssues();
        assertTrue(issues().isPhotoFilterDisplayed(IssuePage.FILTER_WITH_PHOTOS),
                "The Issues list should expose a 'With Photos' filter");
        logStepWithScreenshot("TC_IF_01: With Photos filter present");
    }

    @Test(priority = 2)
    public void TC_IF_02_withoutPhotosFilterIsThere() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_IF_02 - 'Without Photos' filter is present (new in 1.56)");
        openIssues();
        assertTrue(issues().isPhotoFilterDisplayed(IssuePage.FILTER_WITHOUT_PHOTOS),
                "The 1.56 'Without Photos' filter should be present — the literal exists in the build");
        logStepWithScreenshot("TC_IF_02: Without Photos filter present");
    }

    @Test(priority = 5)
    public void TC_IF_05_bucketsPartitionTheList() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_IF_05 - With + Without equals All (partition law)");
        openIssues();
        requireFilter(IssuePage.FILTER_WITH_PHOTOS);
        requireFilter(IssuePage.FILTER_WITHOUT_PHOTOS);

        issues().tapAllTab();
        mediumWait();
        int all = issues().getVisibleIssueCount();
        logStep("All: " + all);

        assertTrue(issues().tapPhotoFilter(IssuePage.FILTER_WITH_PHOTOS), "Should apply 'With Photos'");
        int with = issues().getVisibleIssueCount();
        logStep("With Photos: " + with);

        issues().tapAllTab();
        mediumWait();
        assertTrue(issues().tapPhotoFilter(IssuePage.FILTER_WITHOUT_PHOTOS), "Should apply 'Without Photos'");
        int without = issues().getVisibleIssueCount();
        logStep("Without Photos: " + without);

        assertEquals(with + without, all,
                "The two photo buckets must partition the list exactly: With(" + with + ") + Without("
                + without + ") should equal All(" + all + "). A mismatch means an issue is in both "
                + "buckets or in neither.");
        logStepWithScreenshot("TC_IF_05: partition law holds (" + with + " + " + without + " = " + all + ")");
    }

    @Test(priority = 3)
    public void TC_IF_03_withPhotosShowsOnlyPhotoIssues() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_IF_03 - 'With Photos' lists only issues that have a photo");
        openIssues();
        requireFilter(IssuePage.FILTER_WITH_PHOTOS);
        assertTrue(issues().tapPhotoFilter(IssuePage.FILTER_WITH_PHOTOS), "Should apply 'With Photos'");
        int n = issues().getVisibleIssueCount();
        if (n == 0) throw new SkipException("No photo-bearing issues on this site — nothing to verify");

        logStep("Opening the first filtered issue to confirm it really has a photo");
        assertTrue(issues().tapFirstIssue(), "Should open the first filtered issue");
        mediumWait();
        assertTrue(issues().currentIssueHasPhoto(),
                "An issue listed under 'With Photos' must actually carry a photo");
        logStepWithScreenshot("TC_IF_03: filtered issue has a photo");
    }

    @Test(priority = 6)
    public void TC_IF_06_switchingFiltersRefreshesTheList() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_IF_06 - Switching filters actually re-filters (no stale rows)");
        openIssues();
        requireFilter(IssuePage.FILTER_WITH_PHOTOS);
        requireFilter(IssuePage.FILTER_WITHOUT_PHOTOS);

        issues().tapPhotoFilter(IssuePage.FILTER_WITH_PHOTOS);
        int with = issues().getVisibleIssueCount();
        issues().tapAllTab(); mediumWait();
        issues().tapPhotoFilter(IssuePage.FILTER_WITHOUT_PHOTOS);
        int without = issues().getVisibleIssueCount();
        issues().tapAllTab(); mediumWait();
        int all = issues().getVisibleIssueCount();

        logStep("With=" + with + "  Without=" + without + "  All=" + all);
        assertTrue(all >= with && all >= without,
                "'All' must be at least as large as either bucket (All=" + all + ", With=" + with
                + ", Without=" + without + ") — a smaller All means the list did not refresh");
        logStepWithScreenshot("TC_IF_06: list refreshes per filter");
    }

    @Test(priority = 9)
    public void TC_IF_09_emptyBucketIsClean() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_IF_09 - An empty bucket renders cleanly and the app stays alive");
        openIssues();
        requireFilter(IssuePage.FILTER_WITHOUT_PHOTOS);
        issues().tapPhotoFilter(IssuePage.FILTER_WITHOUT_PHOTOS);
        int n = issues().getVisibleIssueCount();
        logStep("Rows in the Without-Photos bucket: " + n);
        assertTrue(issues().isIssuesScreenDisplayed(),
                "The Issues screen must stay rendered when a bucket filters to " + n + " rows");
        verifyAppAlive("after applying the Without-Photos filter");
        logStepWithScreenshot("TC_IF_09: bucket rendered cleanly");
    }
}
