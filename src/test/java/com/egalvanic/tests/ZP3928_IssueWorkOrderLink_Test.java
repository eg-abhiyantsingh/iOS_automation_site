package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * ZP-3928 (iOS 1.56) §3 — link an issue to a work order FROM the issue editing screen.
 *
 * Build-verified Swift symbols in v1.63: IssueWorkOrderLinkCard.linkedContent(for: IRSession)
 * and readOnlyLinkedRow. Linking from the session side is already covered by TC_JOB_046/051-055;
 * this is the new entry point in the opposite direction.
 */
public final class ZP3928_IssueWorkOrderLink_Test extends BaseTest {

    private static final String FEATURE = "Issue → Work Order link card (ZP-3928)";

    private IssuePage issuePage;
    private IssuePage issues() {
        if (issuePage == null) issuePage = new IssuePage();
        return issuePage;
    }

    private void openIssueDetails() {
        loginAndSelectSite();
        if (!issues().navigateToIssuesScreen()) throw new SkipException("Issues screen did not open");
        issues().tapAllTab();
        mediumWait();
        if (!issues().tapFirstIssue()) throw new SkipException("No issue available to open");
        mediumWait();
        if (!issues().isIssueDetailsScreenDisplayed()) {
            throw new SkipException("Issue Details did not open");
        }
    }

    @Test(priority = 1)
    public void TC_ILW_01_linkCardIsOnTheIssueScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_ILW_01 - The work-order link card renders on the issue screen");
        openIssueDetails();
        logStep("Looking for the work-order link card");
        boolean card = issues().isWorkOrderLinkCardPresent();
        assertTrue(card,
                "v1.56 adds IssueWorkOrderLinkCard to the issue screen — no work-order link "
                + "affordance was found. Without it a technician cannot attach an issue to a work "
                + "order from here, which is the whole point of the change.");
        logStepWithScreenshot("TC_ILW_01: link card present");
    }

    @Test(priority = 3)
    public void TC_ILW_03_linkControlOpensThePicker() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_ILW_03 - The link control opens a work-order picker");
        openIssueDetails();
        if (!issues().isWorkOrderLinkCardPresent()) {
            throw new SkipException("Work-order link card is not on this build");
        }
        logStep("Tapping the link control");
        if (!issues().tapWorkOrderLinkControl()) {
            throw new SkipException("Link control was not tappable (issue may already be linked)");
        }
        mediumWait();
        assertTrue(issues().isWorkOrderPickerOpen(),
                "Tapping the link control should open a work-order picker listing sessions to attach to");
        logStepWithScreenshot("TC_ILW_03: work-order picker opened");
    }

    @Test(priority = 10)
    public void TC_ILW_10_cancelLinksNothing() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_ILW_10 - Cancelling the picker creates no link");
        openIssueDetails();
        if (!issues().isWorkOrderLinkCardPresent()) {
            throw new SkipException("Work-order link card is not on this build");
        }
        String before = issues().linkedWorkOrderName();
        logStep("Linked work order before: " + (before == null ? "(none)" : before));

        if (!issues().tapWorkOrderLinkControl()) {
            throw new SkipException("Link control not tappable");
        }
        mediumWait();
        logStep("Cancelling the picker");
        issues().dismissContextMenu();
        mediumWait();

        String after = issues().linkedWorkOrderName();
        logStep("Linked work order after cancel: " + (after == null ? "(none)" : after));
        assertEquals(String.valueOf(after), String.valueOf(before),
                "Cancelling the picker must leave the link exactly as it was (before="
                + before + ", after=" + after + ")");
        logStepWithScreenshot("TC_ILW_10: cancel created no link");
    }
}
