package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.pages.SessionIssuesPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * ZP-3927 (iOS 1.57) §1 — "Unlink Issue" on long-press.
 *
 * Where it lives (four probes, 1.63): NOT on the Issues screen — long-pressing there raises nothing, by
 * handle or by coordinates. It is the work-order session's Issues tab: long-press a linked row →
 * «Unlink Issue» → Alert «Unlink Issue?» («Remove “<title>” from this work order?» · Cancel · Unlink Issue)
 * → the row leaves the session. The fixture is self-provisioning: «Manage Issues» links the one issue
 * on the site that is not yet attached to any session (the picker warns issues can only be linked to
 * one session at a time, so only unlinked candidates are ever selected), and every test unlinks
 * everything again on exit so the QA-WT04 session is left as found.
 *
 * The distinction this suite must prove is UNLINK vs DELETE: after unlinking, the issue leaves the work
 * order but still exists on the Issues screen and shows as unlinked in the picker. A test that only
 * checked "row disappeared" would pass just as happily if the app had destroyed the issue.
 */
public final class ZP3927_UnlinkIssue_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "Unlink Issue on long-press (ZP-3927)";

    private SessionIssuesPage session;
    private IssuePage issuePage;

    @BeforeMethod(alwaysRun = true)
    public void initPages() {
        if (!DriverManager.isDriverActive()) return;
        try { session = new SessionIssuesPage(); } catch (IllegalStateException e) { DriverManager.initDriver(); session = new SessionIssuesPage(); }
        issuePage = null;
    }

    @AfterMethod(alwaysRun = true)
    public void leaveSessionUnlinked() {
        try {
            if (session != null && session.isOnSessionIssuesTab()) session.unlinkAllLinked();
        } catch (Exception ignored) { }
    }

    private IssuePage issues() {
        if (issuePage == null) issuePage = new IssuePage();
        return issuePage;
    }

    /** Open the QA-WT04 session's Issues tab (skip-guarded). */
    private void openSessionIssues(String tcId) {
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(), tcId + ": session details did not settle open");
        skipIfPreconditionMissing(() -> wo.tapSessionTab("Issues"), tcId + ": 'Issues' session tab not tappable");
        mediumWait();
        skipIfPreconditionMissing(() -> session.isOnSessionIssuesTab(), tcId + ": the session Issues tab did not render");
    }

    /** Open the tab and make sure exactly one issue is linked; returns its title. */
    private String openWithOneLinkedIssue(String tcId) {
        openSessionIssues(tcId);
        String title = session.ensureOneLinkedIssue();
        skipIfPreconditionMissing(() -> title != null,
                tcId + ": no issue could be linked to the session — every issue on the site is already linked to another "
                + "session (the picker allows one session per issue) or the picker did not open");
        logStep("Linked issue under test: '" + title + "'");
        return title;
    }

    @Test(priority = 1)
    public void TC_UL_01_longPressOffersUnlink() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_01 - Long-pressing a linked issue offers 'Unlink Issue'");
        String title = openWithOneLinkedIssue("TC_UL_01");
        logStep("Long-pressing the row");
        assertTrue(session.longPressLinkedRow(title), "Long-press should register on the linked issue row");
        assertTrue(session.isUnlinkOffered(),
                "1.57 must offer '" + SessionIssuesPage.MENU_UNLINK + "' in the long-press menu of a linked issue");
        logStepWithScreenshot("TC_UL_01: Unlink Issue offered");
        session.dismissContextMenu();
    }

    @Test(priority = 2)
    public void TC_UL_02_confirmationCopyNamesTheIssueAndTheWorkOrder() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_02 - Choosing Unlink asks 'Unlink Issue?' naming the issue, with Cancel / Unlink Issue");
        String title = openWithOneLinkedIssue("TC_UL_02");
        final String[] body = {null};
        final boolean[] seen = {false};
        session.withAlertsManual(() -> {
            assertTrue(session.longPressLinkedRow(title), "Long-press should register");
            assertTrue(session.tapUnlinkMenuItem(), "'Unlink Issue' should be tappable");
            seen[0] = session.waitForUnlinkAlert(4);
            if (seen[0]) {
                body[0] = session.unlinkAlertBody();
                logStep("Alert body: " + body[0]);
                logStepWithScreenshot("TC_UL_02: confirmation alert");
                assertTrue(session.cancelUnlinkAlert(), "Cancel on the alert should be tappable");
            }
        });
        skipIfPreconditionMissing(() -> seen[0],
                "the 'Unlink Issue?' alert was not observable even with auto-accept paused — nothing to assert on");
        assertTrue(body[0] != null && body[0].contains(title),
                "The confirmation must name the issue being unlinked ('" + SessionIssuesPage.ALERT_BODY_PREFIX + title
                + SessionIssuesPage.ALERT_BODY_SUFFIX + "') — got '" + body[0] + "'");
        assertTrue(body[0] != null && body[0].endsWith(SessionIssuesPage.ALERT_BODY_SUFFIX),
                "The confirmation must say the issue leaves THIS WORK ORDER, not that it is deleted — got '" + body[0] + "'");
        assertTrue(session.linkedIssueTitles().contains(title), "Cancel must keep the issue linked");
    }

    @Test(priority = 3)
    public void TC_UL_03_unlinkRemovesTheRowAndDropsTheCounts() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_03 - Unlink removes the row and the tab badge drops by exactly one");
        String title = openWithOneLinkedIssue("TC_UL_03");
        int rowsBefore = session.linkedIssueCount();
        int badgeBefore = session.issuesTabBadge();
        logStep("before — rows: " + rowsBefore + " · tab badge: " + badgeBefore);

        assertTrue(session.unlinkIssue(title), "Unlink should complete and the row should disappear");
        int rowsAfter = session.linkedIssueCount();
        int badgeAfter = session.issuesTabBadge();
        logStep("after — rows: " + rowsAfter + " · tab badge: " + badgeAfter);
        assertEquals(rowsAfter, rowsBefore - 1,
                "Unlinking one issue must drop the session list by exactly one (before=" + rowsBefore + ", after=" + rowsAfter + ")");
        if (badgeBefore > 0) {
            assertEquals(badgeAfter, badgeBefore - 1,
                    "The Issues tab badge must follow the list (before=" + badgeBefore + ", after=" + badgeAfter + ")");
        }
        if (rowsAfter == 0) {
            assertTrue(session.isEmptyState(),
                    "With no linked issues left the tab must show the '" + SessionIssuesPage.EMPTY_TITLE + "' empty state");
        }
        logStepWithScreenshot("TC_UL_03: row removed, counts dropped");
    }

    @Test(priority = 4)
    public void TC_UL_04_unlinkedIssueStillExists() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_04 - The unlinked issue still exists (unlink is not delete)");
        String title = openWithOneLinkedIssue("TC_UL_04");
        assertTrue(session.unlinkIssue(title), "Unlink should complete");

        logStep("Picker view: the issue must now be offered as UNLINKED, not gone");
        assertTrue(session.openManageIssues(), "'Manage Issues' should reopen the picker");
        Boolean linked = session.isPickerRowLinked(title);
        logStep("picker row '" + title + "' present=" + (linked != null) + " linked=" + linked);
        assertTrue(linked != null,
                "The issue '" + title + "' must still be listed in the Link Issues picker after unlinking — its absence "
                + "means the app destroyed it rather than detaching it");
        assertFalse(Boolean.TRUE.equals(linked), "After unlinking, the picker row must not read 'Linked to:' any more");
        session.cancelPicker();

        logStep("Issues screen: the issue must still be there");
        loginAndSelectSite();
        skipIfPreconditionMissing(() -> issues().navigateToIssuesScreen(), "Issues screen did not open for the survival check");
        issues().tapAllTab(); mediumWait();
        assertTrue(issues().isIssueTitleListed(title),
                "'" + title + "' must remain on the Issues screen after being unlinked from the work order");
        logStepWithScreenshot("TC_UL_04: issue survived the unlink");
    }

    @Test(priority = 5)
    public void TC_UL_05_cancellingTheAlertKeepsTheLink() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_05 - Cancel on 'Unlink Issue?' keeps the link");
        String title = openWithOneLinkedIssue("TC_UL_05");
        int before = session.linkedIssueCount();
        final boolean[] seen = {false};
        session.withAlertsManual(() -> {
            assertTrue(session.longPressLinkedRow(title), "Long-press should register");
            assertTrue(session.tapUnlinkMenuItem(), "'Unlink Issue' should be tappable");
            seen[0] = session.waitForUnlinkAlert(4);
            if (seen[0]) assertTrue(session.cancelUnlinkAlert(), "Cancel should be tappable");
        });
        skipIfPreconditionMissing(() -> seen[0], "the alert was not observable even with auto-accept paused");
        int after = session.linkedIssueCount();
        assertEquals(after, before, "Cancelling the confirmation must leave the link intact (before=" + before + ", after=" + after + ")");
        assertTrue(session.linkedIssueTitles().contains(title), "The issue must still be linked after Cancel");
        logStepWithScreenshot("TC_UL_05: cancel kept the link");
    }

    @Test(priority = 6)
    public void TC_UL_06_dismissingTheMenuChangesNothing() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_06 - Dismissing the long-press menu without choosing changes nothing");
        String title = openWithOneLinkedIssue("TC_UL_06");
        int before = session.linkedIssueCount();
        assertTrue(session.longPressLinkedRow(title), "Long-press should register");
        assertTrue(session.isUnlinkOffered(), "Menu should be up");
        session.dismissContextMenu();
        mediumWait();
        assertFalse(session.isUnlinkOffered(), "The menu should be gone after tapping outside it");
        assertEquals(session.linkedIssueCount(), before, "Dismissing the menu must leave every link intact");
        logStepWithScreenshot("TC_UL_06: dismiss changed nothing");
    }

    @Test(priority = 7)
    public void TC_UL_07_relinkAfterUnlinkWorks() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_07 - An unlinked issue can be linked again (unlink is reversible)");
        String title = openWithOneLinkedIssue("TC_UL_07");
        assertTrue(session.unlinkIssue(title), "Unlink should complete");
        assertFalse(session.linkedIssueTitles().contains(title), "Row should be gone after unlink");
        logStep("Re-linking through Manage Issues");
        assertTrue(session.openManageIssues(), "Picker should open");
        String picked = session.selectFirstUnlinkedCandidate();
        assertEquals(picked, title, "The just-unlinked issue should be the unlinked candidate offered first");
        assertTrue(session.tapUpdate(), "Update should be tappable");
        assertTrue(session.linkedIssueTitles().contains(title), "The issue must be linked again after Update");
        logStepWithScreenshot("TC_UL_07: relinked");
    }

    @Test(priority = 10)
    public void TC_UL_10_longPressElsewhereIsSafe() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_UL_10 - Long-pressing a non-row element offers no Unlink and does not crash");
        openSessionIssues("TC_UL_10");
        logStep("Long-pressing the 'Manage Issues' control (not an issue row)");
        boolean pressed = session.longPressManageIssues();
        logStep("long-press registered: " + pressed);
        assertFalse(session.isUnlinkOffered(), "'Unlink Issue' must not be offered for a non-row long-press");
        session.dismissContextMenu();
        session.cancelPicker();   // a plain tap may have opened the picker instead
        verifyAppAlive("after a stray long-press on the session Issues tab");
        logStepWithScreenshot("TC_UL_10: stray long-press is safe");
    }
}
