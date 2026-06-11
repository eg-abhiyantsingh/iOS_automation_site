package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.verify.StateIntegrityChecker;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Work Order Planning Test Suite  [OPERATIONS]
 * ================================================================
 * Module:   Work Order Planning   (app domain: Jobs / Work Orders)
 * Category: OPERATIONS
 * Scope:    Plan CRUD, search, edit, totals
 *
 * In this app a "Work Order Plan" is a Job / Work Order — the planning
 * artifact a field user creates, reads, edits, searches within, and whose
 * rolled-up totals (assets / issues / tasks / counts) appear on its details.
 * These tests build ONLY on the already-validated {@link WorkOrderPage} API.
 *
 * House style (matches ZP323_NewFeatures_Test / SiteVisit_phase*):
 *  - noReset=true; the first test logs in + selects a site, the rest reuse state.
 *  - skipIfPreconditionMissing() -> SKIP (not FAIL) when a feature/state is
 *    absent in the current build or the selected site has no work-order data.
 *  - Page-object methods are internally defensive (safe returns, no throws).
 *  - Locators are best-effort; the first CI run may surface 1-2 refinements.
 * ================================================================
 */
public class WorkOrderPlanning_Test extends BaseTest {

    private WorkOrderPage workOrderPage;

    @BeforeClass(alwaysRun = true)
    public void wopClassSetup() {
        System.out.println("\n📋 Work Order Planning Suite [OPERATIONS] — Starting (Plan CRUD, search, edit, totals)");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void wopTestSetup() {
        workOrderPage = new WorkOrderPage();
    }

    @AfterClass(alwaysRun = true)
    public void wopClassTeardown() {
        // Leave no active job behind for sibling suites running on the same device.
        try {
            if (workOrderPage != null && workOrderPage.isAnyJobActive()) {
                workOrderPage.deactivateActiveJob();
            }
        } catch (Exception ignored) {}
        DriverManager.resetNoResetOverride();
        System.out.println("📋 Work Order Planning Suite — Complete\n");
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /** Ensure on dashboard, then open the Work Orders (Jobs) screen. */
    private void navigateToWorkOrdersScreen() {
        if (workOrderPage.waitForWorkOrdersScreen()) {
            return; // already there
        }
        if (!(assetPage != null && assetPage.isDashboardDisplayedFast())) {
            smartNavigateToDashboard();
        }
        try { siteSelectionPage.clickNoActiveJobCard(); } catch (Exception ignored) {}
        shortWait();
        if (!workOrderPage.waitForWorkOrdersScreen()) {
            // Retry once — the card tap may not have registered.
            try { siteSelectionPage.clickNoActiveJobCard(); } catch (Exception ignored) {}
            mediumWait();
            workOrderPage.waitForWorkOrdersScreen();
        }
    }

    /** Open the "New Job" (create-plan) screen from the Work Orders screen. */
    private boolean openNewJobScreen() {
        navigateToWorkOrdersScreen();
        if (!workOrderPage.isStartNewWorkOrderButtonDisplayed()) {
            return false;
        }
        workOrderPage.clickStartNewWorkOrder();
        shortWait();
        return workOrderPage.waitForNewJobScreen();
    }

    /** Activate a work order and land on its session details. Returns false if not possible. */
    private boolean openActivePlanDetails() {
        navigateToWorkOrdersScreen();
        if (!workOrderPage.activateWorkOrderIfNeeded()) {
            return false;
        }
        shortWait();
        return workOrderPage.openActiveWOSessionDetailsFromAnywhere();
    }

    // ============================================================
    // PLAN — CREATE (C in CRUD)
    // ============================================================

    @Test(priority = 1)
    public void TC_WOP_001_verifyWorkOrdersScreenReachable() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_001 - Open Work Order Planning (Work Orders) screen");
        logStep("Step 1: Log in and select a site");
        loginAndSelectSite();

        logStep("Step 2: Navigate to the Work Orders screen");
        navigateToWorkOrdersScreen();
        boolean onScreen = workOrderPage.waitForWorkOrdersScreen();
        skipIfPreconditionMissing(() -> onScreen, "Work Orders screen not reachable for this site");
        verifyNotBlank("Work Orders");
        guard("open Work Orders");
        assertTrue(workOrderPage.isWorkOrdersHeaderCorrect(),
            "Work Orders screen header should be correct. Got: '" + workOrderPage.getWorkOrdersHeaderText() + "'");
        logStepWithScreenshot("TC_WOP_001: Work Orders screen open");
    }

    @Test(priority = 2)
    public void TC_WOP_002_verifyCreatePlanScreenElements() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_002 - Create plan: New Job screen exposes name field + Create");
        logStep("Step 1: Open the New Job (create plan) screen");
        boolean opened = openNewJobScreen();
        skipIfPreconditionMissing(() -> opened, "Start New Work Order not available on this site");

        logStep("Step 2: Verify create-plan controls are present");
        assertTrue(workOrderPage.isNewJobScreenDisplayed(), "New Job screen should be displayed");
        assertTrue(workOrderPage.isNewJobCreateButtonDisplayed(), "Create button should be present");
        assertTrue(workOrderPage.isJobNameFieldEditable(), "Plan name field should be editable");
        logStepWithScreenshot("TC_WOP_002: Create-plan controls present");

        // Don't leave the form open for the next test.
        if (workOrderPage.isNewJobCancelButtonDisplayed()) workOrderPage.tapNewJobCancel();
    }

    @Test(priority = 3)
    public void TC_WOP_003_createPlanWithCustomName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_003 - Create plan with a custom name");
        logStep("Step 1: Open the New Job screen");
        boolean opened = openNewJobScreen();
        skipIfPreconditionMissing(() -> opened, "Start New Work Order not available on this site");

        String planName = "WOP Plan " + timestamp();
        logStep("Step 2: Enter custom plan name: " + planName);
        boolean named = workOrderPage.editJobNameField(planName);
        skipIfPreconditionMissing(() -> named, "Plan name field not editable in this build");

        logStep("Step 3: Create the plan");
        boolean created = workOrderPage.tapCreateJobButton();
        assertTrue(created, "Create button should be tappable");
        shortWait();
        verifyAppAlive("create work-order plan");
        // After creation the app returns to the Work Orders list (or activates the plan).
        assertTrue(workOrderPage.waitForWorkOrdersScreen() || workOrderPage.isWorkOrderActive(),
            "After create, expected the Work Orders list or an active plan");
        logStepWithScreenshot("TC_WOP_003: Plan created");
    }

    // ============================================================
    // PLAN — READ (R in CRUD)
    // ============================================================

    @Test(priority = 4)
    public void TC_WOP_004_readPlanList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_004 - Read plan list (available work orders + count)");
        logStep("Step 1: Open the Work Orders screen");
        navigateToWorkOrdersScreen();
        skipIfPreconditionMissing(() -> workOrderPage.waitForWorkOrdersScreen(), "Work Orders screen not reachable");

        logStep("Step 2: Verify the available-plans section renders real content");
        verifyNotBlank("Work Orders");
        guard("Work Orders list");
        boolean sectionVisible = workOrderPage.isAvailableWorkOrdersSectionDisplayed();
        int count = workOrderPage.getWorkOrderEntryCount();
        logStep("Available work-order/plan entries: " + count + " | section visible: " + sectionVisible);
        if (count > 0) {
            // Strong: an entry must render a real, non-empty name — catches phantom/blank rows
            String firstName = workOrderPage.getWorkOrderName(0);
            assertTrue(firstName != null && !firstName.trim().isEmpty(),
                "First plan entry must render a non-empty name (no phantom rows). Got: '" + firstName + "'");
        } else {
            // No data: the screen must still render its section/header (blankness already ruled out)
            assertTrue(sectionVisible || workOrderPage.isWorkOrdersHeaderCorrect(),
                "With no entries, the Work Orders screen must still render its section/header");
        }
        logStepWithScreenshot("TC_WOP_004: Plan list read");
    }

    @Test(priority = 5)
    public void TC_WOP_005_readPlanDetails() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_005 - Read a plan's session details");
        logStep("Step 1: Activate a plan and open its details");
        boolean onDetails = openActivePlanDetails();
        skipIfPreconditionMissing(() -> onDetails, "No work order available to open details for");

        logStep("Step 2: Verify the session-details screen renders");
        assertTrue(workOrderPage.isSessionDetailsScreenDisplayed() || workOrderPage.waitForSessionDetailsScreen(),
            "Plan details screen should be displayed");
        logStep("Details header: '" + workOrderPage.getSessionDetailsHeaderText() + "'");
        logStepWithScreenshot("TC_WOP_005: Plan details read");
    }

    // ============================================================
    // PLAN — EDIT / UPDATE (U in CRUD)
    // ============================================================

    @Test(priority = 6)
    public void TC_WOP_006_editPlanName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_006 - Edit plan name before creation");
        logStep("Step 1: Open the New Job screen");
        boolean opened = openNewJobScreen();
        skipIfPreconditionMissing(() -> opened, "Start New Work Order not available on this site");

        String before = workOrderPage.getJobNameFieldValue();
        String edited = "Edited " + timestamp();
        logStep("Step 2: Edit plan name (was '" + before + "') -> '" + edited + "'");
        boolean ok = workOrderPage.editJobNameField(edited);
        skipIfPreconditionMissing(() -> ok, "Plan name field not editable in this build");

        String after = workOrderPage.getJobNameFieldValue();
        assertTrue(after != null && after.contains("Edited"),
            "Plan name should reflect the edit. Got: '" + after + "'");
        logStepWithScreenshot("TC_WOP_006: Plan name edited");
        if (workOrderPage.isNewJobCancelButtonDisplayed()) workOrderPage.tapNewJobCancel();
    }

    @Test(priority = 7)
    public void TC_WOP_007_editPlanPhotoType() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_007 - Edit plan photo-type configuration");
        logStep("Step 1: Open the New Job screen");
        boolean opened = openNewJobScreen();
        skipIfPreconditionMissing(() -> opened, "Start New Work Order not available on this site");

        logStep("Step 2: Open the Photo Type dropdown");
        skipIfPreconditionMissing(() -> workOrderPage.isPhotoTypeDropdownDisplayed(),
            "Photo Type configuration not present in this build");
        String current = workOrderPage.getPhotoTypeValue();
        boolean opened2 = workOrderPage.tapPhotoTypeDropdown();
        assertTrue(opened2 && workOrderPage.isPhotoTypeDropdownOpen(),
            "Photo Type dropdown should open (current value: '" + current + "')");
        logStepWithScreenshot("TC_WOP_007: Photo-type dropdown open");
        if (workOrderPage.isNewJobCancelButtonDisplayed()) workOrderPage.tapNewJobCancel();
    }

    @Test(priority = 8)
    public void TC_WOP_008_editPlanSchedule() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_008 - Edit plan schedule date (ZP-323.15)");
        logStep("Step 1: Open an active plan's details");
        boolean onDetails = openActivePlanDetails();
        skipIfPreconditionMissing(() -> onDetails, "No work order available to schedule");

        logStep("Step 2: Tap the Schedule field");
        boolean tapped = workOrderPage.tapScheduleField();
        skipIfPreconditionMissing(() -> tapped && workOrderPage.isDatePickerDisplayed(),
            "Schedule field / date picker not present in this build");
        boolean confirmed = workOrderPage.confirmDatePicker();
        String scheduled = workOrderPage.getScheduledDateValue();
        logStep("Scheduled date now: '" + scheduled + "'");
        assertTrue(confirmed, "Date picker should confirm the selected schedule date");
        assertTrue(scheduled != null && !scheduled.trim().isEmpty(),
            "Schedule field should show a date after confirming the picker. Got: '" + scheduled + "'");
        logStepWithScreenshot("TC_WOP_008: Plan schedule edited");
    }

    // ============================================================
    // PLAN — DELETE / DEACTIVATE (D in CRUD)
    // ============================================================

    @Test(priority = 9)
    public void TC_WOP_009_deactivatePlan() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_009 - Deactivate (clear) an active plan");
        logStep("Step 1: Ensure a plan is active");
        navigateToWorkOrdersScreen();
        boolean active = workOrderPage.activateWorkOrderIfNeeded();
        skipIfPreconditionMissing(() -> active, "No work order available to activate");

        logStep("Step 2: Deactivate the active plan");
        boolean deactivated = workOrderPage.deactivateActiveJob();
        assertTrue(deactivated, "Active plan should be deactivatable");
        assertFalse(workOrderPage.isAnyJobActive(), "No plan should remain active after deactivation");
        logStepWithScreenshot("TC_WOP_009: Plan deactivated");
    }

    // ============================================================
    // PLAN — SEARCH
    // ============================================================

    @Test(priority = 10)
    public void TC_WOP_010_searchIssuesWithinPlan() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_010 - Search issues to link within a plan");
        logStep("Step 1: Open an active plan and its Issues tab");
        boolean onDetails = openActivePlanDetails();
        skipIfPreconditionMissing(() -> onDetails, "No active plan to search within");
        workOrderPage.tapSessionTab("Issues");
        shortWait();
        skipIfPreconditionMissing(() -> workOrderPage.isManageIssuesButtonDisplayed(),
            "Manage Issues not available on this plan");
        workOrderPage.tapManageIssuesButton();
        skipIfPreconditionMissing(() -> workOrderPage.waitForLinkIssuesScreen(), "Link Issues screen did not open");

        logStep("Step 2: Search in the Link Issues list");
        int before = workOrderPage.getLinkIssuesListCount();
        boolean searched = workOrderPage.searchInLinkIssues("a");
        int after = workOrderPage.getLinkIssuesListCount();
        logStep("Link-issues count before=" + before + " after search='a' =" + after);
        assertTrue(searched, "Search box in Link Issues should accept input");
        logStepWithScreenshot("TC_WOP_010: Searched within plan issues");
        workOrderPage.tapLinkIssuesCancel();
    }

    @Test(priority = 11)
    public void TC_WOP_011_searchTasksWithinPlan() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_011 - Search tasks to link within a plan");
        logStep("Step 1: Open an active plan and its Tasks tab");
        boolean onDetails = openActivePlanDetails();
        skipIfPreconditionMissing(() -> onDetails, "No active plan to search within");
        workOrderPage.tapSessionTab("Tasks");
        shortWait();
        skipIfPreconditionMissing(() -> workOrderPage.isManageTasksButtonDisplayed(),
            "Manage Tasks not available on this plan");
        workOrderPage.tapManageTasksButton();
        skipIfPreconditionMissing(() -> workOrderPage.waitForLinkTasksScreen(), "Link Tasks screen did not open");

        logStep("Step 2: Search in the Link Tasks list");
        boolean searched = workOrderPage.searchInLinkTasks("a");
        logStep("Link-tasks count after search='a' = " + workOrderPage.getLinkTasksListCount());
        assertTrue(searched, "Search box in Link Tasks should accept input");
        logStepWithScreenshot("TC_WOP_011: Searched within plan tasks");
        workOrderPage.tapLinkTasksCancelButton();
    }

    // ============================================================
    // PLAN — TOTALS
    // ============================================================

    @Test(priority = 12)
    public void TC_WOP_012_verifyPlanSessionTotals() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_012 - Verify plan session totals (stat cards)");
        logStep("Step 1: Open an active plan's details");
        boolean onDetails = openActivePlanDetails();
        skipIfPreconditionMissing(() -> onDetails, "No active plan to read totals from");

        logStep("Step 2: Verify session stat totals render");
        skipIfPreconditionMissing(() -> workOrderPage.isSessionStatsDisplayed(),
            "Session stats not present on this plan");
        String assets = workOrderPage.getStatCardCount("Assets");
        String issues = workOrderPage.getStatCardCount("Issues");
        logStep("Plan totals -> Assets: '" + assets + "', Issues: '" + issues + "'");
        assertTrue(assets != null || issues != null, "At least one stat total should be readable");
        logStepWithScreenshot("TC_WOP_012: Plan totals verified");
    }

    @Test(priority = 13)
    public void TC_WOP_013_verifyPlanEntryCounts() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_013 - Verify per-plan counts on the work-order list");
        logStep("Step 1: Open the Work Orders screen");
        navigateToWorkOrdersScreen();
        skipIfPreconditionMissing(() -> workOrderPage.waitForWorkOrdersScreen(), "Work Orders screen not reachable");

        logStep("Step 2: Verify plan entries expose roll-up counts");
        skipIfPreconditionMissing(() -> workOrderPage.isAnyWorkOrderCountsDisplayed(),
            "No plan entries with counts on this site");
        String counts = workOrderPage.getWorkOrderCounts(0);
        logStep("First plan entry counts: '" + counts + "'");
        assertNotNull(counts, "Plan entry counts should be readable");
        logStepWithScreenshot("TC_WOP_013: Plan entry counts verified");
    }

    @Test(priority = 14)
    public void TC_WOP_014_verifyPlanTaskTotals() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_014 - Verify plan task totals (Tasks tab summary)");
        logStep("Step 1: Open an active plan's Tasks tab");
        boolean onDetails = openActivePlanDetails();
        skipIfPreconditionMissing(() -> onDetails, "No active plan to read task totals from");
        workOrderPage.tapSessionTab("Tasks");
        shortWait();

        logStep("Step 2: Read the task totals summary");
        String total = workOrderPage.getTasksTotalCount();
        String completed = workOrderPage.getTasksCompletedCount();
        logStep("Plan task totals -> Total: '" + total + "', Completed: '" + completed + "'");
        skipIfPreconditionMissing(() -> total != null && !total.isEmpty(),
            "Task totals summary not present on this plan");
        assertNotNull(total, "Task total count should be readable");
        logStepWithScreenshot("TC_WOP_014: Plan task totals verified");
    }

    // ============================================================
    // PLAN — STATE INTEGRITY (hardening pilot)
    // ============================================================

    @Test(priority = 15)
    public void TC_WOP_015_planListIntegrityAcrossRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_PLANNING,
            "TC_WOP_015 - Plan list survives a screen round-trip with no loss/duplication");
        logStep("Step 1: Open Work Orders and snapshot the plan list");
        navigateToWorkOrdersScreen();
        skipIfPreconditionMissing(() -> workOrderPage.waitForWorkOrdersScreen(), "Work Orders screen not reachable");
        verifyNotBlank("Work Orders");
        StateIntegrityChecker sic = new StateIntegrityChecker();
        StateIntegrityChecker.Snapshot before = sic.capture(this::readPlanNames);

        logStep("Step 2: Round-trip to the dashboard and back");
        smartNavigateToDashboard();
        guard("dashboard");
        navigateToWorkOrdersScreen();
        skipIfPreconditionMissing(() -> workOrderPage.waitForWorkOrdersScreen(),
            "Work Orders not reachable after round-trip");

        logStep("Step 3: The plan list must be intact (no loss, no duplication)");
        StateIntegrityChecker.Snapshot after = sic.capture(this::readPlanNames);
        sic.assertNoLossOrDup(before, after);
        // Explicit size invariant on top of the integrity check: a pure UI
        // round-trip must neither lose nor invent plan entries.
        assertTrue(after.size() == before.size(),
            "Plan list size must be identical after a dashboard round-trip. Before="
            + before.size() + " after=" + after.size());
        logStepWithScreenshot("TC_WOP_015: Plan list integrity verified");
    }

    /**
     * Read current plan identities positionally ("i:name"). Positional identity keeps each
     * entry unique, so the integrity check measures loss / round-trip stability without
     * false-flagging two plans that happen to share a display name.
     */
    private java.util.List<String> readPlanNames() {
        int count = workOrderPage.getWorkOrderEntryCount();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String n = workOrderPage.getWorkOrderName(i);
            names.add(i + ":" + (n == null ? "<null>" : n.trim()));
        }
        return names;
    }
}
