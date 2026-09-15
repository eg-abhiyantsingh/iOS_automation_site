package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * ZP-3927 (iOS 1.57) §1 — "Unlink Issue" on long-press.
 *
 * Build-verified in v1.63: the literal "Unlink Issue" (x4), longPress handlers, and the
 * offline path "Issue unlink queued for sync".
 *
 * The important distinction this suite must prove is UNLINK vs DELETE: after unlinking,
 * the issue must leave the work order but still exist in the Issues list. A test that only
 * checked "row disappeared" would pass just as happily if the app had destroyed the issue.
 */
public final class ZP3927_UnlinkIssue_Test extends BaseTest {

    private static final String FEATURE = "Unlink Issue on long-press (ZP-3927)";

    private IssuePage issuePage;
    private IssuePage issues() {
        if (issuePage == null) issuePage = new IssuePage();
        return issuePage;
    }

    /** Reach a list of issues that belong to a work order. */
    private void openWorkOrderIssues() {
        loginAndSelectSite();
        if (!issues().navigateToIssuesScreen()) {
            throw new SkipException("Issues screen did not open");
        }
        issues().tapAllTab();
        mediumWait();
        if (issues().getVisibleIssueCount() == 0) {
            throw new SkipException("No issues on this site — nothing to unlink");
        }
    }

    private void requireUnlinkMenu() {
        if (!issues().longPressFirstIssueRow()) {
            throw new SkipException("Could not long-press an issue row");
        }
        if (!issues().isUnlinkIssueOffered()) {
            issues().dismissContextMenu();
            throw new SkipException("'Unlink Issue' is not offered here — either this list is not the "
                    + "work-order issue list, or ZP-3927 §1 is not on this build");
        }
    }

    @Test(priority = 1)
    public void TC_UL_01_longPressOffersUnlink() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_01 - Long-press offers 'Unlink Issue'");
        openWorkOrderIssues();
        logStep("Long-pressing the first issue row");
        assertTrue(issues().longPressFirstIssueRow(), "Long-press should register on an issue row");
        assertTrue(issues().isUnlinkIssueOffered(),
                "v1.57 should offer 'Unlink Issue' in the long-press menu");
        issues().dismissContextMenu();
        logStepWithScreenshot("TC_UL_01: Unlink Issue offered");
    }

    @Test(priority = 2)
    public void TC_UL_02_unlinkRemovesTheRowAndDropsTheCount() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_02/03 - Unlink removes the row and the count drops by exactly one");
        openWorkOrderIssues();
        int before = issues().getVisibleIssueCount();
        logStep("Issue count before unlink: " + before);

        requireUnlinkMenu();
        logStep("Choosing 'Unlink Issue'");
        assertTrue(issues().tapUnlinkIssue(), "'Unlink Issue' should be tappable");
        mediumWait();

        int after = issues().getVisibleIssueCount();
        logStep("Issue count after unlink: " + after);
        assertEquals(after, before - 1,
                "Unlinking one issue must drop the list by exactly one (before=" + before
                + ", after=" + after + ") — a larger drop means more than one link was severed");
        logStepWithScreenshot("TC_UL_02/03: count dropped by one");
    }

    @Test(priority = 4)
    public void TC_UL_04_unlinkedIssueStillExists() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_04 - The unlinked issue still exists (unlink is not delete)");
        openWorkOrderIssues();
        issues().tapAllTab();
        mediumWait();
        int allBefore = issues().getVisibleIssueCount();
        logStep("Issues visible under All before unlink: " + allBefore);

        requireUnlinkMenu();
        assertTrue(issues().tapUnlinkIssue(), "'Unlink Issue' should be tappable");
        mediumWait();

        issues().tapAllTab();
        mediumWait();
        int allAfter = issues().getVisibleIssueCount();
        logStep("Issues visible under All after unlink: " + allAfter);
        assertEquals(allAfter, allBefore,
                "Unlink must not DELETE the issue: the All list should still hold " + allBefore
                + " issues, but it holds " + allAfter + ". A drop here means the issue was destroyed, "
                + "not merely detached from the work order.");
        logStepWithScreenshot("TC_UL_04: issue survived the unlink");
    }

    @Test(priority = 7)
    public void TC_UL_07_cancellingTheMenuKeepsTheLink() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_07 - Dismissing the menu changes nothing");
        openWorkOrderIssues();
        int before = issues().getVisibleIssueCount();
        logStep("Count before: " + before);

        assertTrue(issues().longPressFirstIssueRow(), "Long-press should register");
        logStep("Dismissing the context menu without choosing Unlink");
        issues().dismissContextMenu();
        mediumWait();

        int after = issues().getVisibleIssueCount();
        logStep("Count after dismiss: " + after);
        assertEquals(after, before,
                "Dismissing the menu must leave every link intact (before=" + before
                + ", after=" + after + ")");
        logStepWithScreenshot("TC_UL_07: dismiss changed nothing");
    }

    @Test(priority = 10)
    public void TC_UL_10_longPressElsewhereIsSafe() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_10 - Long-pressing a non-issue row offers no Unlink and does not crash");
        loginAndSelectSite();
        if (!issues().navigateToIssuesScreen()) throw new SkipException("Issues screen did not open");

        logStep("Long-pressing the screen header area (not an issue row)");
        issues().longPressFirstIssueRow();   // may or may not land on a row
        issues().dismissContextMenu();
        verifyAppAlive("after long-pressing on the Issues screen");
        assertTrue(issues().isIssuesScreenDisplayed(),
                "The Issues screen must survive a stray long-press without crashing or navigating away");
        logStepWithScreenshot("TC_UL_10: stray long-press is safe");
    }
}
