package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
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

    /** The driver is re-created per test; never reuse a page bound to a quit session ("Session ID is null"). */
    @BeforeMethod(alwaysRun = true)
    public void resetPages() { issuePage = null; }

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
                "TC_IF_05 - With + Without equals All (partition law, from the tab counts)");
        openIssues();
        requireFilter(IssuePage.FILTER_WITH_PHOTOS);
        requireFilter(IssuePage.FILTER_WITHOUT_PHOTOS);

        // The tabs carry their counts in the label ("All 12", "With Photos 3"). Counting visible
        // cells instead would cap at one screenful and make the law false-fail past ~8 issues.
        int all     = issues().getAllTabCount();
        int with    = issues().getWithPhotosTabCount();
        int without = issues().getWithoutPhotosTabCount();
        logStep("Tab counts — All: " + all + " · With Photos: " + with + " · Without Photos: " + without);
        if (all < 0 || with < 0 || without < 0) {
            throw new SkipException("A filter tab carries no count on this build (All=" + all + ", With="
                    + with + ", Without=" + without + ") — the partition law needs all three");
        }
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
        skipIfPreconditionMissing(() -> issues().scrollToIssuePhotosSection(), "'Issue Photos' section not found on the opened issue");
        int thumbs = issues().issuePhotoThumbnailCount();
        logStep("Issue Photos thumbnails on the opened issue: " + thumbs);
        assertTrue(thumbs > 0,
                "An issue listed under 'With Photos' must actually carry at least one Issue Photo thumbnail — found " + thumbs);
        logStepWithScreenshot("TC_IF_03: filtered issue has a photo");
    }

    @Test(priority = 6)
    public void TC_IF_06_switchingFiltersRefreshesTheList() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_IF_06 - Applying a bucket really re-filters the rows (no stale list)");
        openIssues();
        requireFilter(IssuePage.FILTER_WITH_PHOTOS);
        requireFilter(IssuePage.FILTER_WITHOUT_PHOTOS);

        int allTab = issues().getAllTabCount();
        int withTab = issues().getWithPhotosTabCount();
        int withoutTab = issues().getWithoutPhotosTabCount();
        if (allTab < 0 || withTab < 0 || withoutTab < 0) {
            throw new SkipException("Filter tabs carry no counts on this build");
        }
        if (withTab == allTab || withoutTab == allTab) {
            throw new SkipException("Every issue is in one bucket (All=" + allTab + ", With=" + withTab
                    + ", Without=" + withoutTab + ") — applying a filter cannot change the row set, "
                    + "so refresh cannot be observed here");
        }
        // Apply the SMALLER bucket: its visible row count must drop below the All view's.
        boolean smallerIsWith = withTab <= withoutTab;
        issues().tapAllTab(); mediumWait();
        int rowsAll = issues().getVisibleIssueCount();
        assertTrue(issues().tapPhotoFilter(smallerIsWith ? IssuePage.FILTER_WITH_PHOTOS : IssuePage.FILTER_WITHOUT_PHOTOS),
                "Should apply the smaller photo bucket");
        int rowsBucket = issues().getVisibleIssueCount();
        int expectedBucket = smallerIsWith ? withTab : withoutTab;
        logStep("visible rows — All: " + rowsAll + " · bucket(" + expectedBucket + "): " + rowsBucket);
        assertTrue(rowsBucket <= expectedBucket,
                "The filtered list shows " + rowsBucket + " rows but the tab says the bucket holds only "
                + expectedBucket + " — rows from outside the bucket are still on screen (stale list)");
        assertTrue(rowsBucket < rowsAll || expectedBucket >= rowsAll,
                "Applying a bucket smaller than All (" + expectedBucket + " < " + allTab + ") must reduce "
                + "the visible rows (All view: " + rowsAll + ", bucket view: " + rowsBucket + ")");
        logStepWithScreenshot("TC_IF_06: bucket view re-filtered (" + rowsAll + " → " + rowsBucket + ")");
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
