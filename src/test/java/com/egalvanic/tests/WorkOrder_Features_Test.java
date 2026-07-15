package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Work Order feature coverage — v1.50 dev changes (2026-07-15).
 *
 *   ZP-3109  Work order priority (Low/Medium/High) — create-form picker,
 *            list-row chips.
 *   ZP-3054  "More Actions" option in Work Order (session) Details.
 *   ZP-3092  Sync stability with many queued tasks (no hang).
 *   ZP-3003  Bulk-create node-session mappings when linking assets.
 *
 * Anatomy is probe- + app-source-verified (see WorkOrderPage v1.50 primitives).
 * The session-dependent cases (3054/3003/3092) gate on reaching the active
 * session and SKIP honestly with a precise reason if the fixture/nav isn't
 * available — they never false-fail, and the first live run surfaces the real
 * Session Details anatomy for tightening.
 */
public class WorkOrder_Features_Test extends BaseTest {

    private WorkOrderPage wo;

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        DriverManager.setNoReset(true);
    }

    @AfterClass(alwaysRun = true)
    public void classTeardown() {
        DriverManager.resetNoResetOverride();
    }

    @BeforeMethod(alwaysRun = true)
    public void initPage() {
        if (!DriverManager.isDriverActive()) return;
        try {
            wo = new WorkOrderPage();
        } catch (IllegalStateException e) {
            DriverManager.initDriver();
            wo = new WorkOrderPage();
        }
    }

    private void openWorkOrdersScreen() {
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(),
                "Work Orders screen must open from the dashboard tile");
    }

    // ══════════════════════ ZP-3109 — Priority ═════════════════════════

    @Test(priority = 1)
    public void TC_WO_PRIO_01_createFormHasPriorityRow() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_DETAILS,
                "TC_WO_PRIO_01 - Start New Work Order form exposes a Priority config row (ZP-3109)");
        openWorkOrdersScreen();
        assertTrue(wo.openCreateForm(), "Start New Work Order form must open");
        String value = wo.getCreateFormRowValue("Priority");
        logStep("Priority row default value: '" + value + "'");
        assertTrue(!value.isEmpty(), "Priority row must render with a default value (ZP-3109)");
        assertTrue(java.util.Arrays.asList("Low", "Medium", "High").contains(value),
                "Priority default must be one of Low/Medium/High, got '" + value + "'");
        wo.cancelCreateForm();
        logStepWithScreenshot("TC_WO_PRIO_01 verified");
    }

    @Test(priority = 2)
    public void TC_WO_PRIO_02_priorityOptionsAreLowMediumHigh() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_DETAILS,
                "TC_WO_PRIO_02 - Priority picker lists exactly Low / Medium / High (ZP-3109)");
        openWorkOrdersScreen();
        assertTrue(wo.openCreateForm(), "Create form must open");
        assertTrue(wo.openCreateFormRow("Priority"), "Priority picker must open");
        List<String> opts = wo.getVisibleSheetOptions();
        logStep("Priority options: " + opts);
        for (String expected : new String[] { "Low", "Medium", "High" }) {
            assertTrue(opts.contains(expected),
                    "Priority options must include '" + expected + "' — got " + opts);
        }
        wo.cancelCreateForm();
        logStepWithScreenshot("TC_WO_PRIO_02 verified");
    }

    @Test(priority = 3)
    public void TC_WO_PRIO_03_selectingHighPersistsToRow() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_DETAILS,
                "TC_WO_PRIO_03 - Selecting High updates the Priority row value (ZP-3109)");
        openWorkOrdersScreen();
        assertTrue(wo.openCreateForm(), "Create form must open");
        String before = wo.getCreateFormRowValue("Priority");
        String pick = "High".equals(before) ? "Low" : "High"; // guarantee a real change
        assertTrue(wo.openCreateFormRow("Priority"), "Priority picker must open");
        assertTrue(wo.pressSheetOption(pick), "Option '" + pick + "' must be selectable");
        shortWait();
        String after = wo.getCreateFormRowValue("Priority");
        logStep("Priority " + before + " -> " + after + " (picked " + pick + ")");
        assertEquals(after, pick, "Priority row must show the newly selected value (ZP-3109)");
        wo.cancelCreateForm();
        logStepWithScreenshot("TC_WO_PRIO_03 verified");
    }

    @Test(priority = 4)
    public void TC_WO_PRIO_04_listRowsCarryPriorityChip() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDERS_SCREEN,
                "TC_WO_PRIO_04 - Available Work Order rows show a priority chip (ZP-3109)");
        openWorkOrdersScreen();
        List<String> rows = wo.getVisibleWorkOrderRowNames();
        logStep("WO rows: " + rows);
        assertTrue(!rows.isEmpty(), "At least one available work order must be listed");
        int withPriority = 0;
        for (String name : rows) {
            String p = WorkOrderPage.rowPriority(name);
            if (java.util.Arrays.asList("Low", "Medium", "High").contains(p)) withPriority++;
        }
        assertTrue(withPriority > 0,
                "At least one WO row must carry a Low/Medium/High priority chip (ZP-3109) — rows: " + rows);
        logStepWithScreenshot("TC_WO_PRIO_04 verified (" + withPriority + "/" + rows.size() + " rows chipped)");
    }

    // ══════════════════════ ZP-3054 — More Actions ═════════════════════

    @Test(priority = 5)
    public void TC_WO_MORE_01_moreActionsInSessionDetails() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_DETAILS,
                "TC_WO_MORE_01 - 'More Actions' affordance present in active Work Order session (ZP-3054)");
        openWorkOrdersScreen();
        if (!wo.startFirstAvailableWorkOrder()) {
            throw new SkipException("Precondition: could not activate a work order (v1.50 Start dance)");
        }
        skipIfPreconditionMissing(() -> wo.openActiveWorkOrderSession(),
                "active session did not open from the Work Order banner");
        boolean moreOpened = wo.openMoreActionsMenu();
        assertTrue(moreOpened, "'More Actions' menu must be present and open in the session (ZP-3054)");
        List<String> actions = wo.getVisibleSheetOptions();
        logStep("More Actions options: " + actions);
        assertTrue(!actions.isEmpty(), "More Actions menu must list at least one action");
        wo.dismissMoreActionsMenu();
        logStepWithScreenshot("TC_WO_MORE_01 verified");
    }

    @Test(priority = 6)
    public void TC_WO_MORE_02_moreActionsOpenCloseIdempotent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_DETAILS,
                "TC_WO_MORE_02 - More Actions menu opens and dismisses cleanly twice (ZP-3054)");
        loginAndSelectSite();
        skipIfPreconditionMissing(() -> wo.hasActiveWorkOrder() && wo.openActiveWorkOrderSession(),
                "no active work order session to exercise the menu");
        for (int round = 1; round <= 2; round++) {
            assertTrue(wo.openMoreActionsMenu(), "More Actions must open (round " + round + ")");
            wo.dismissMoreActionsMenu();
            shortWait();
        }
        logStepWithScreenshot("TC_WO_MORE_02 idempotence verified");
    }

    // ══════════════════════ ZP-3003 — Bulk asset linking ═══════════════

    @Test(priority = 7)
    public void TC_WO_LINK_01_bulkLinkAssetsGrowsSession() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_DETAILS,
                "TC_WO_LINK_01 - Linking existing assets bulk-adds node-session mappings (ZP-3003)");
        openWorkOrdersScreen();
        if (!wo.startFirstAvailableWorkOrder()) {
            throw new SkipException("Precondition: could not activate a work order");
        }
        skipIfPreconditionMissing(() -> wo.openActiveWorkOrderSession(),
                "active session did not open");
        skipIfPreconditionMissing(() -> wo.openFirstSessionRoom(),
                "no room available in the session to link assets into");
        int before = wo.getRoomAssetCount();
        logStep("Room asset count before link: " + before);
        skipIfPreconditionMissing(() -> wo.openLinkExistingAssets(),
                "'Link Existing Asset' affordance not reachable (session may be inactive)");
        int selected = wo.selectMultipleLinkableAssets(2);
        skipIfPreconditionMissing(() -> selected > 0,
                "no linkable assets available to select in this room");
        assertTrue(wo.confirmLinkAssets(), "Confirm/Add must complete the bulk link");
        longWait();
        int after = wo.getRoomAssetCount();
        logStep("Room asset count after linking " + selected + ": " + after);
        assertTrue(after >= before + selected,
                "Session room asset count must grow by the number linked (ZP-3003) — before "
                        + before + ", linked " + selected + ", after " + after);
        logStepWithScreenshot("TC_WO_LINK_01 verified");
    }

    // ══════════════════════ ZP-3092 — Sync stability ═══════════════════

    @Test(priority = 8)
    public void TC_WO_SYNC_01_manyTasksSyncDoesNotHang() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, AppConstants.FEATURE_WORK_ORDER_DETAILS,
                "TC_WO_SYNC_01 - Queue with many tasks syncs to zero without hanging (ZP-3092)");
        loginAndSelectSite();
        // Offline-create several tasks against the shared asset, then go online
        // and assert the queue drains within a hard budget with the app alive.
        // Reuses the validated offline task-creation + sync-drain building
        // blocks; gates on the ability to create offline so it never false-fails.
        skipIfPreconditionMissing(() -> wo.canQueueOfflineTasks(),
                "environment can't queue offline tasks (Wi-Fi mode not toggleable)");
        int queued = wo.queueOfflineTasks(3);
        skipIfPreconditionMissing(() -> queued >= 1,
                "could not queue any offline task in this environment");
        logStep("Queued " + queued + " offline tasks");
        long budgetMs = 180_000L;
        boolean drained = wo.goOnlineAndAwaitQueueDrain(budgetMs);
        verifyAppAlive("post-sync many-tasks (ZP-3092)");
        assertTrue(drained,
                "Sync must drain the task queue to zero within " + (budgetMs / 1000)
                        + "s without hanging (ZP-3092)");
        logStepWithScreenshot("TC_WO_SYNC_01 verified");
    }
}
