package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * DIAGNOSTIC PROBE — not wired into any suite. Run manually:
 *   mvn test -Dtest=WorkTypeProbe_Test
 *
 * Probe run 14 (2026-07-27). Confirmed so far: WO row = ONE full-width Button;
 * the trailing "circle" is a tap ZONE (right edge) that raises the
 * 'Start Work Order'/'Cancel' alert; confirm → session details (activation).
 * UNTYPED `name CONTAINS` scans wedge WDA after activation — every query here
 * is TYPE-bound. This run: ACTIVE badge anatomy, switch-while-active alert,
 * then the per-asset PM-forms flow.
 */
public class WorkTypeProbe_Test extends BaseTest {

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

    @Test(priority = 1)
    public void PROBE_A_activationAndSwitch() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE A - activation badge + switch-while-active semantics");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        boolean opened = wo.openWorkOrderByName("QA-WT04 Clean Tighten Torque");
        System.out.println("PROBE| WT04 opened (activated): " + opened);
        if (!opened) return;
        wo.goBack();
        mediumWait();
        wo.waitForWorkOrdersScreen();
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            dumpMatches("activeBadge", "type == 'XCUIElementTypeStaticText' AND name == 'ACTIVE'");
            dumpMatches("activeBtnBadge", "type == 'XCUIElementTypeButton' AND name CONTAINS 'ACTIVE'");
            dumpMatches("startNewNow", "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Start New Work Order'");
            System.out.println("PROBE| WT04 composite now: '" + wo.getWorkOrderRowComposite("QA-WT04") + "'");

            // Switch semantics: circle-tap ANOTHER row while WT04 is active.
            try { DriverManager.getDriver().setSetting("defaultAlertAction", ""); } catch (Exception ignored) { }
            if (wo.scrollWorkOrderListTo("QA-WT05")) {
                WebElement other = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH 'QA-WT05'"));
                Rectangle r = other.getRect();
                DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width - 35, "y", r.y + r.height / 2));
                mediumWait();
                dumpMatches("switchAlertBtns", "type == 'XCUIElementTypeButton' AND visible == 1 AND "
                        + "(name CONTAINS 'Start' OR name CONTAINS 'Cancel' OR name CONTAINS 'End' "
                        + "OR name CONTAINS 'Switch' OR name CONTAINS 'OK')");
                dumpMatches("switchAlertTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND "
                        + "(name CONTAINS 'session' OR name CONTAINS 'Session' OR name CONTAINS 'current' "
                        + "OR name CONTAINS 'End' OR name CONTAINS 'work order' OR name CONTAINS 'Work Order')");
                // Dismiss with Cancel to keep WT04 active.
                try {
                    WebElement cancel = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND name == 'Cancel' AND visible == 1"));
                    Rectangle cc = cancel.getRect();
                    DriverManager.getDriver().executeScript("mobile: tap",
                            java.util.Map.of("x", cc.x + cc.width / 2, "y", cc.y + cc.height / 2));
                    System.out.println("PROBE| switch alert cancelled");
                } catch (Exception e) {
                    System.out.println("PROBE| no Cancel to tap: " + e.getMessage());
                }
            }
            try { DriverManager.getDriver().setSetting("defaultAlertAction", "accept"); } catch (Exception ignored) { }
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe A complete");
    }

    @Test(priority = 2)
    public void PROBE_B_formsFlow() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE B - per-asset forms flow inside the active WT04 session");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        // openWorkOrderByName handles the already-active case (no alert → direct open).
        boolean opened = wo.openWorkOrderByName("QA-WT04 Clean Tighten Torque");
        System.out.println("PROBE| session open: " + opened
                + " isSessionDetails=" + wo.isSessionDetailsScreenDisplayed());
        if (!wo.isSessionDetailsScreenDisplayed()) return;
        // Session opens on Details — go to the Assets tab explicitly, then walk
        // the locations tree (floors expand; room rows carry an "N assets" count).
        boolean tab = wo.tapSessionTab("Assets");
        System.out.println("PROBE| tapSessionTab(Assets)=" + tab);
        mediumWait();
        mediumWait();
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            dumpMatches("treeButtons", "type == 'XCUIElementTypeButton' AND visible == 1 AND "
                    + "(name CONTAINS 'Floor' OR name CONTAINS 'Room' OR name CONTAINS 'assets' OR name CONTAINS 'Bldg')");
            // Find a room row with assets; expand the first floor if none visible.
            List<WebElement> roomRows = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS ' assets'"));
            if (roomRows.isEmpty()) {
                List<WebElement> floors = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS 'Floor 77'"));
                if (floors.isEmpty()) floors = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH 'Floor'"));
                if (!floors.isEmpty()) {
                    System.out.println("PROBE| expanding floor: '" + floors.get(0).getAttribute("name") + "'");
                    floors.get(0).click();
                    mediumWait(); mediumWait();
                }
                for (int i = 0; i < 6 && roomRows.isEmpty(); i++) {
                    roomRows = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS ' assets'"));
                    if (roomRows.isEmpty()) {
                        try {
                            DriverManager.getDriver().executeScript("mobile: swipe",
                                    java.util.Map.of("direction", "up"));
                        } catch (Exception ignored) { }
                    }
                }
            }
            System.out.println("PROBE| room-with-assets rows: " + roomRows.size());
            if (roomRows.isEmpty()) { System.out.println("PROBE| no room row — abort"); return; }
            System.out.println("PROBE| tapping room: '" + roomRows.get(0).getAttribute("name") + "'");
            roomRows.get(0).click();
            mediumWait(); mediumWait();
            dumpMatches("roomNav", "type == 'XCUIElementTypeNavigationBar'");
            dumpMatches("assetRows", "type == 'XCUIElementTypeButton' AND visible == 1 AND "
                    + "(name CONTAINS 'Switch' OR name CONTAINS 'Transformer' OR name CONTAINS 'Busway' "
                    + "OR name CONTAINS 'Fuse' OR name CONTAINS 'ATS' OR name CONTAINS 'Panelboard')");
            List<WebElement> rows = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND "
                    + "(name CONTAINS 'Switch' OR name CONTAINS 'Transformer' OR name CONTAINS 'Fuse' OR name CONTAINS 'ATS')"));
            if (rows.isEmpty()) {
                System.out.println("PROBE| no asset row matched");
                return;
            }
            System.out.println("PROBE| tapping asset: '" + rows.get(0).getAttribute("name") + "'");
            rows.get(0).click();
            mediumWait();
            mediumWait();
            dumpMatches("afterAssetNav", "type == 'XCUIElementTypeNavigationBar'");
            dumpMatches("formChipsBtn", "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS '—'");
            dumpMatches("formChipsTxt", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND name CONTAINS '—'");
            dumpMatches("formTopButtons", "type == 'XCUIElementTypeButton' AND visible == 1");
            dumpMatches("resultTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND "
                    + "(name == 'Result' OR name == 'Pass' OR name == 'Fail' OR name == '—' OR name CONTAINS 'Value')");
            dumpMatches("formFields", "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'");
            dumpMatches("procedureSteps", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND name CONTAINS 'Procedure'");
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe B complete");
    }

    @Test(priority = 3)
    public void PROBE_C_moreActionsHunt() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE C - ZP-3054 More Actions affordance hunt in active session (v1.51)");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        if (!wo.startFirstAvailableWorkOrder()) {
            System.out.println("PROBE| could not activate a work order");
            return;
        }
        if (!wo.openActiveWorkOrderSession()) {
            System.out.println("PROBE| active session did not open");
            return;
        }
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            // 1. Every visible button in the nav zone (y < 200).
            dumpMatches("navButtons", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 200");
            // 2. Any ellipsis/More/menu-ish affordance anywhere.
            dumpMatches("moreish", "type == 'XCUIElementTypeButton' AND (name CONTAINS[c] 'more' "
                    + "OR name CONTAINS 'ellipsis' OR name CONTAINS 'circle' OR name CONTAINS 'arrow')");
            // 3. Nav bar identity.
            dumpMatches("navBars", "type == 'XCUIElementTypeNavigationBar'");
            // 4. Tap the top-right nav button (screenshot shows a circular-arrow icon) and dump result.
            List<WebElement> nav = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 200 AND rect.x > 250"));
            if (!nav.isEmpty()) {
                WebElement tr = nav.get(nav.size() - 1);
                System.out.println("PROBE| tapping top-right nav button '" + tr.getAttribute("name") + "'");
                Rectangle r = tr.getRect();
                DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                mediumWait();
                dumpMatches("afterTapButtons", "type == 'XCUIElementTypeButton' AND visible == 1");
                dumpMatches("afterTapSheets", "type == 'XCUIElementTypeSheet' OR type == 'XCUIElementTypeAlert' "
                        + "OR (type == 'XCUIElementTypeOther' AND name == 'Sheet Grabber')");
                // Dismiss whatever opened.
                wo.dismissMoreActionsMenu();
            }
            // 5. Also check the session ROOM surface (More may live there).
            if (wo.openFirstSessionRoom()) {
                dumpMatches("roomNavButtons", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 200");
                dumpMatches("roomMoreish", "type == 'XCUIElementTypeButton' AND (name CONTAINS[c] 'more' "
                        + "OR name CONTAINS 'ellipsis')");
            }
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe C complete");
    }

    @Test(priority = 4)
    public void PROBE_D_moreActionsTabSweep() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE D - ZP-3054 hunt across ALL session tabs + WO list nav");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            // WO LIST surface first.
            dumpMatches("listNavButtons", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 200");
            dumpMatches("listMoreish", "type == 'XCUIElementTypeButton' AND (name CONTAINS[c] 'more' OR name CONTAINS 'ellipsis')");
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        if (!wo.startFirstAvailableWorkOrder()) { System.out.println("PROBE| no WO activated"); return; }
        if (!wo.openActiveWorkOrderSession()) { System.out.println("PROBE| session did not open"); return; }
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            for (String tab : new String[]{"Details", "Assets", "Tasks", "Issues", "IR", "Files"}) {
                try {
                    WebElement t = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND name == '" + tab + "' AND visible == 1 AND rect.y > 800"));
                    Rectangle r = t.getRect();
                    DriverManager.getDriver().executeScript("mobile: tap",
                            java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                    mediumWait();
                } catch (Exception e) {
                    System.out.println("PROBE| tab '" + tab + "' not tappable: " + e.getMessage());
                    continue;
                }
                dumpMatches(tab + ".moreish", "type == 'XCUIElementTypeButton' AND visible == 1 AND "
                        + "(name CONTAINS[c] 'more' OR name CONTAINS 'ellipsis')");
                dumpMatches(tab + ".navBtns", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 200");
            }
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe D complete");
    }

    @Test(priority = 5)
    public void PROBE_E_createFormWorkTypeRow_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE E - v1.55 create form gained a required 'Work Type' row: anatomy + option surface");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        assertTrue(wo.openCreateForm(), "Start New Work Order form must open");
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            // 1. Full form-row census: how does the Work Type row name itself?
            dumpMatches("formRows", "type == 'XCUIElementTypeButton' AND name CONTAINS 'Work Type'");
            dumpMatches("formRowsAll", "type == 'XCUIElementTypeButton' AND visible == 1");
            dumpMatches("formTexts", "type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] 'work type'");
            // 2. Open the row (chevron row — left-zone press like other config rows).
            List<WebElement> rows = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Work Type'"));
            if (rows.isEmpty()) {
                // sheet visible==0 quirk: retry without visibility constraint via texts
                rows = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "name CONTAINS 'Work Type'"));
            }
            System.out.println("PROBE| workTypeRow candidates=" + rows.size());
            if (rows.isEmpty()) { System.out.println("PROBE| NO Work Type row found"); return; }
            WebElement row = rows.get(0);
            System.out.println("PROBE| tapping row '" + row.getAttribute("name") + "' type=" + row.getAttribute("type"));
            Rectangle r = row.getRect();
            DriverManager.getDriver().executeScript("mobile: tap",
                    java.util.Map.of("x", r.x + 40, "y", r.y + r.height / 2));
            mediumWait();
            // 3. Option surface census.
            dumpMatches("optNavBars", "type == 'XCUIElementTypeNavigationBar'");
            dumpMatches("optButtons", "type == 'XCUIElementTypeButton' AND visible == 1");
            dumpMatches("optTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1");
            dumpMatches("optCells", "type == 'XCUIElementTypeCell'");
            dumpMatches("optOthersNamed", "type == 'XCUIElementTypeOther' AND name CONTAINS 'Arc'");
            // 4. Try selecting 'Cleaning' (present in the 13-type catalog).
            try {
                WebElement opt = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                        + "OR type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeOther') "
                        + "AND name == 'Cleaning'"));
                Rectangle ro = opt.getRect();
                DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", ro.x + ro.width / 2, "y", ro.y + ro.height / 2));
                mediumWait();
                dumpMatches("afterSelectRows", "type == 'XCUIElementTypeButton' AND name CONTAINS 'Work Type'");
                dumpMatches("afterSelectNav", "type == 'XCUIElementTypeNavigationBar'");
            } catch (Exception e) {
                System.out.println("PROBE| 'Cleaning' option not tappable: " + e.getMessage());
            }
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe E complete");
    }

    @Test(priority = 6)
    public void PROBE_F_workTypePickerSemantics_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE F - v1.55 Work Type picker: commit semantics, selection indicator, full census, comma-name trap");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        assertTrue(wo.openCreateForm(), "Start New Work Order form must open");
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            tapWorkTypeRow();
            mediumWait();
            // 1. FULL option census with value attribute (selection indicator hunt).
            fullOptionCensus("initial");
            // 2. Tap 'Cleaning', re-census (does a checkmark/value appear? sheet still open?)
            tapByName("Cleaning");
            mediumWait();
            fullOptionCensus("afterTapCleaning");
            // 3. Press Done -> row value?
            tapByName("Done");
            mediumWait();
            dumpMatches("rowAfterDone", "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Work Type'");
            // 4. Reopen: which row is marked selected now?
            tapWorkTypeRow();
            mediumWait();
            fullOptionCensus("reopened");
            // 5. Comma-name trap: select 'Clean, Tighten, Torque' -> Done -> row?
            tapByName("Clean, Tighten, Torque");
            mediumWait();
            tapByName("Done");
            mediumWait();
            dumpMatches("rowAfterCTT", "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Work Type'");
            // 6. Swipe-dismiss (no Cancel on sheet): reopen, swipe down, row unchanged?
            tapWorkTypeRow();
            mediumWait();
            try {
                DriverManager.getDriver().executeScript("mobile: swipe", java.util.Map.of("direction", "down"));
            } catch (Exception e) { System.out.println("PROBE| swipe failed: " + e.getMessage()); }
            mediumWait();
            dumpMatches("rowAfterSwipe", "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Work Type'");
            dumpMatches("sheetGoneCheck", "type == 'XCUIElementTypeNavigationBar' AND name == 'Work Type'");
            // 7. Create button state on the form (required-* gating probe).
            dumpMatches("createBtn", "type == 'XCUIElementTypeButton' AND name == 'Create'");
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe F complete");
    }

    @Test(priority = 7)
    public void PROBE_G_createThenEndSessionViaDashboardChip_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE G - v1.55 create WO (typed) -> post-create state -> dashboard WO chip -> End Session flow");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        assertTrue(wo.openCreateForm(), "Start New Work Order form must open");
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            // Capture the default name (used later to find/clean the WO).
            String woName = "";
            try {
                WebElement nameField = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND visible == 1"));
                woName = String.valueOf(nameField.getAttribute("value"));
            } catch (Exception e) { System.out.println("PROBE| name field: " + e.getMessage()); }
            System.out.println("PROBE| creating WO named '" + woName + "'");
            // Select 'Cleaning' work type (tap row -> tap option -> Done).
            tapWorkTypeRow();
            mediumWait();
            tapByName("Cleaning");
            mediumWait();
            tapByName("Done");
            mediumWait();
            dumpMatches("rowBeforeCreate", "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Work Type'");
            // Create (manual alerts in case a confirm races).
            try { DriverManager.getDriver().setSetting("defaultAlertAction", ""); } catch (Exception ignored) { }
            tapByName("Create");
            sleep(2500);
            // Post-create state census.
            dumpMatches("postCreateNav", "type == 'XCUIElementTypeNavigationBar'");
            dumpMatches("postCreateAlerts", "type == 'XCUIElementTypeAlert'");
            dumpMatches("postCreateBanner", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND "
                    + "(name CONTAINS 'Active' OR name CONTAINS 'Session' OR name BEGINSWITH 'Work Order -')");
            // Navigate to dashboard (Site tab) and hunt the WO chip.
            tapByName("Done"); // leave WO list if still there
            sleep(1200);
            dumpMatches("dashTopRight", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 260");
            dumpMatches("dashWoChip", "visible == 1 AND (name == 'WO' OR name CONTAINS 'WO')");
            // Tap the chip (top-right, name 'WO' expected).
            tapByName("WO");
            mediumWait();
            dumpMatches("chipMenuButtons", "type == 'XCUIElementTypeButton' AND visible == 1");
            dumpMatches("chipMenuTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND "
                    + "(name == 'End Session' OR name BEGINSWITH 'Work Order' OR name CONTAINS 'critic')");
            dumpMatches("chipMenuOthers", "type == 'XCUIElementTypeOther' AND name == 'End Session'");
            // End Session -> alert census -> confirm.
            tapByName("End Session");
            mediumWait();
            dumpMatches("endAlertTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND "
                    + "(name CONTAINS 'End' OR name CONTAINS 'session' OR name CONTAINS 'sure')");
            dumpMatches("endAlertBtns", "type == 'XCUIElementTypeButton' AND visible == 1 AND "
                    + "(name == 'Cancel' OR name == 'End Session')");
            // Confirm (alert button — widest/last 'End Session').
            java.util.List<WebElement> confirms = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'End Session' AND visible == 1"));
            if (!confirms.isEmpty()) {
                WebElement c = confirms.get(confirms.size() - 1);
                Rectangle r = c.getRect();
                DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                System.out.println("PROBE| confirmed End Session");
            }
            sleep(1500);
            dumpMatches("postEndChip", "visible == 1 AND name == 'WO'");
            dumpMatches("postEndBanner", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND name CONTAINS 'Active'");
            System.out.println("PROBE| created-and-ended WO '" + woName + "' (API cleanup separately)");
        } finally {
            try { DriverManager.getDriver().setSetting("defaultAlertAction", "accept"); } catch (Exception ignored) { }
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe G complete");
    }

    @Test(priority = 9)
    public void PROBE_I_woChipTapStrategies_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE I - WO chip tap strategies: find what actually opens the session menu");
        loginAndSelectSite();
        if (!wo.isDashboardWoChipPresent()) { System.out.println("PROBE| no chip — nothing to probe"); return; }
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            // Chip geometry.
            for (WebElement el : DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "visible == 1 AND name == 'WO'"))) {
                try {
                    Rectangle r = el.getRect();
                    System.out.println("PROBE| chip [" + el.getAttribute("type") + "] rect=(" + r.x + "," + r.y
                            + "," + r.width + "," + r.height + ") hittable=" + el.getAttribute("hittable"));
                } catch (Exception ignored) { }
            }
            String[] tags = {"tapOther", "tapCoord", "pressHold"};
            for (String strategy : tags) {
                try {
                    if (strategy.equals("tapOther")) {
                        WebElement chip = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeOther' AND visible == 1 AND name == 'WO'"));
                        Rectangle r = chip.getRect();
                        DriverManager.getDriver().executeScript("mobile: tap",
                                java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                    } else if (strategy.equals("tapCoord")) {
                        org.openqa.selenium.Dimension win = DriverManager.getDriver().manage().window().getSize();
                        DriverManager.getDriver().executeScript("mobile: tap",
                                java.util.Map.of("x", win.getWidth() - 30, "y", 75));
                    } else {
                        WebElement chip = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                                "visible == 1 AND name == 'WO'"));
                        Rectangle r = chip.getRect();
                        DriverManager.getDriver().executeScript("mobile: touchAndHold",
                                java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2, "duration", 1.0));
                    }
                    sleep(1200);
                    int endRows = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                            "name == 'End Session'")).size();
                    System.out.println("PROBE| after " + strategy + ": endSessionRows=" + endRows);
                    if (endRows > 0) {
                        dumpMatches(strategy + ".menuAll", "visible == 1 AND (type == 'XCUIElementTypeButton' "
                                + "OR type == 'XCUIElementTypeOther' OR type == 'XCUIElementTypeStaticText') AND rect.y < 700");
                        // close the menu without ending: tap top-left safe zone
                        DriverManager.getDriver().executeScript("mobile: tap", java.util.Map.of("x", 30, "y", 400));
                        sleep(800);
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("PROBE| " + strategy + " failed: " + e.getMessage());
                }
            }
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe I complete");
    }

    @Test(priority = 10)
    public void PROBE_J_woChipMenuUnfilteredDump_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE J - tap WO chip then dump EVERYTHING (no name filter) to find the menu rows");
        loginAndSelectSite();
        if (!wo.isDashboardWoChipPresent()) { System.out.println("PROBE| no chip"); return; }
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            WebElement chip = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND visible == 1 AND name == 'WO'"));
            Rectangle r = chip.getRect();
            DriverManager.getDriver().executeScript("mobile: tap",
                    java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
            sleep(1500);
            logStepWithScreenshot("after chip tap");
            for (String t : new String[]{"Button", "Other", "StaticText", "Cell", "Image", "Sheet", "Menu", "MenuItem"}) {
                java.util.List<WebElement> els = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementType" + t + "' AND visible == 1 AND rect.y < 720"));
                System.out.println("PROBE| type=" + t + " count=" + els.size());
                int i = 0;
                for (WebElement el : els) {
                    if (++i > 22) { System.out.println("PROBE| …truncated"); break; }
                    try {
                        Rectangle er = el.getRect();
                        String n = el.getAttribute("name");
                        System.out.println("PROBE|  " + t + " y=" + er.y + " x=" + er.x + " '" + n + "'");
                    } catch (Exception ignored) { }
                }
            }
            java.util.List<WebElement> endish = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "name CONTAINS 'End' AND visible == 1"));
            System.out.println("PROBE| endish=" + endish.size());
            for (WebElement el : endish) {
                try {
                    System.out.println("PROBE| endish [" + el.getAttribute("type") + "] y=" + el.getRect().y
                            + " '" + el.getAttribute("name") + "'");
                } catch (Exception ignored) { }
            }
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe J complete");
    }

    @Test(priority = 11)
    public void PROBE_K_menuFirstRowAndAlert_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE K - chip menu: hidden labels? first-row tap -> End alert anatomy -> confirm end");
        loginAndSelectSite();
        if (!wo.isDashboardWoChipPresent()) { System.out.println("PROBE| no chip"); return; }
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            try { DriverManager.getDriver().setSetting("defaultAlertAction", ""); } catch (Exception ignored) { }
            WebElement chip = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND visible == 1 AND name == 'WO'"));
            Rectangle cr = chip.getRect();
            DriverManager.getDriver().executeScript("mobile: tap",
                    java.util.Map.of("x", cr.x + cr.width / 2, "y", cr.y + cr.height / 2));
            sleep(1200);
            // 1. Hidden labels? (no visible==1 constraint)
            java.util.List<WebElement> hidden = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND rect.x > 150 AND rect.y < 720"));
            System.out.println("PROBE| hiddenTexts=" + hidden.size());
            int i = 0;
            for (WebElement el : hidden) {
                if (++i > 14) break;
                try {
                    System.out.println("PROBE| hidden y=" + el.getRect().y + " vis=" + el.getAttribute("visible")
                            + " '" + el.getAttribute("name") + "'");
                } catch (Exception ignored) { }
            }
            // 2. Tap the TOPMOST menu row (Cell at x>150, min y).
            WebElement topRow = null;
            int minY = Integer.MAX_VALUE;
            for (WebElement c : DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell' AND visible == 1 AND rect.x > 150"))) {
                try {
                    int y = c.getRect().y;
                    if (y < minY) { minY = y; topRow = c; }
                } catch (Exception ignored) { }
            }
            if (topRow == null) { System.out.println("PROBE| no menu rows"); return; }
            Rectangle tr = topRow.getRect();
            System.out.println("PROBE| tapping top row y=" + tr.y);
            DriverManager.getDriver().executeScript("mobile: tap",
                    java.util.Map.of("x", tr.x + tr.width / 2, "y", tr.y + tr.height / 2));
            sleep(1200);
            // 3. Alert census.
            dumpMatches("alerts", "type == 'XCUIElementTypeAlert'");
            dumpMatches("alertTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1");
            dumpMatches("alertBtns", "type == 'XCUIElementTypeButton' AND visible == 1");
            // 4. Confirm End Session if offered (cleans the leftover session).
            java.util.List<WebElement> confirms = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'End Session' AND visible == 1"));
            if (!confirms.isEmpty()) {
                Rectangle r = confirms.get(confirms.size() - 1).getRect();
                DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                sleep(1500);
                System.out.println("PROBE| confirmed; chip now: " + wo.isDashboardWoChipPresent());
            } else {
                System.out.println("PROBE| no End Session confirm — first row was something else");
            }
        } finally {
            try { DriverManager.getDriver().setSetting("defaultAlertAction", "accept"); } catch (Exception ignored) { }
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe K complete");
    }

    @Test(priority = 8)
    public void PROBE_H_endSessionPrimitives_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE H - validate dashboard WO chip menu + end-session primitives against the leftover PROBE_G session");
        loginAndSelectSite();
        // We are on the dashboard post-login.
        boolean chip = wo.isDashboardWoChipPresent();
        System.out.println("PROBE| chip present on dashboard: " + chip);
        if (!chip) { System.out.println("PROBE| no active session — nothing to end"); return; }
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            boolean menu = wo.openDashboardWoMenu();
            System.out.println("PROBE| openDashboardWoMenu: " + menu);
            dumpMatches("menuEndRow", "visible == 1 AND name == 'End Session'");
            dumpMatches("menuWoRows", "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH 'Work Order'");
            dumpMatches("menuOtherRows", "type == 'XCUIElementTypeOther' AND visible == 1 AND name BEGINSWITH 'Work Order'");
            if (menu) {
                boolean ended = wo.endActiveSessionViaDashboardMenu();
                System.out.println("PROBE| endActiveSessionViaDashboardMenu: " + ended);
                mediumWait();
                System.out.println("PROBE| chip after end: " + wo.isDashboardWoChipPresent());
            }
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe H complete");
    }

    @Test(priority = 12)
    public void PROBE_L_addFormAndSignOffAnatomy_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE L - v1.55 per-asset Add Form sheet + Technician Sign-Off anatomy (user flow 'file the form')");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        if (!wo.startFirstAvailableWorkOrder()) { System.out.println("PROBE| no WO activated"); return; }
        if (!wo.openActiveWorkOrderSession()) { System.out.println("PROBE| session did not open"); return; }
        com.egalvanic.pages.WorkOrderFormsPage forms = new com.egalvanic.pages.WorkOrderFormsPage();
        if (!forms.openFirstRoomWithAssetsInTree()) {
            System.out.println("PROBE| no room with assets — one bounded dump (giant tree: full sweeps wedge WDA)");
            DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
            try {
                dumpMatches("treeCountTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND name MATCHES '[0-9]+ assets?'");
            } finally {
                DriverManager.getDriver().manage().timeouts()
                        .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
            }
            return;
        }
        java.util.List<String> assets = forms.visibleAssetRowComposites();
        System.out.println("PROBE| assets in room: " + assets);
        if (assets.isEmpty()) { System.out.println("PROBE| no assets"); return; }
        String firstAsset = assets.get(0).split(",")[0].trim();
        if (!forms.openAssetForms(firstAsset)) { System.out.println("PROBE| asset forms did not open"); return; }
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            // 1. Empty-state anatomy ('No forms for this asset' + Add Form + Close).
            dumpMatches("emptyState", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND "
                    + "(name CONTAINS 'No forms' OR name CONTAINS 'form')");
            dumpMatches("emptyButtons", "type == 'XCUIElementTypeButton' AND visible == 1");
            // 2. Open the Add Form sheet.
            tapByName("Add Form");
            mediumWait();
            dumpMatches("sheetNav", "type == 'XCUIElementTypeNavigationBar'");
            dumpMatches("sheetHeaders", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND name CONTAINS 'FORM'");
            dumpMatches("templateRows", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > 150");
            dumpMatches("templateCells", "type == 'XCUIElementTypeCell' AND visible == 1");
            dumpMatches("templateTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND rect.y > 150");
            // 3. Pick the FIRST template (ATEST1-style row) — tap its text.
            java.util.List<WebElement> tmpl = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == 1 AND rect.y > 150 AND NOT (name BEGINSWITH 'Applies to')"));
            if (!tmpl.isEmpty()) {
                WebElement t = tmpl.get(0);
                System.out.println("PROBE| picking template '" + t.getAttribute("name") + "'");
                Rectangle r = t.getRect();
                DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                sleep(1500);
            }
            // 4. Opened form: nav buttons (Back/trash/edit/checkmark names), chip strip, sign-off block.
            dumpMatches("formNavButtons", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 260");
            dumpMatches("formChips", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 160");
            dumpMatches("signOffTexts", "type == 'XCUIElementTypeStaticText' AND "
                    + "(name CONTAINS 'Sign' OR name CONTAINS 'Printed' OR name CONTAINS 'Signature' OR name CONTAINS 'Date')");
            dumpMatches("textViews", "type == 'XCUIElementTypeTextView'");
            dumpMatches("textFields", "type == 'XCUIElementTypeTextField'");
            dumpMatches("clearBtn", "name == 'Clear'");
            // 5. Signature canvas hunt: dump Others/Images near the 'Sign above' text.
            dumpMatches("signAbove", "name CONTAINS 'Sign above'");
            dumpMatches("canvasOthers", "type == 'XCUIElementTypeOther' AND visible == 1 AND rect.y > 900");
            // 6. Draw one stroke across the canvas zone (right half, below 'Technician Signature').
            try {
                java.util.List<WebElement> sig = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND name == 'Technician Signature'"));
                if (!sig.isEmpty()) {
                    Rectangle sr = sig.get(0).getRect();
                    int cx = sr.x + 40, cy = sr.y + 120;
                    DriverManager.getDriver().executeScript("mobile: dragFromToForDuration",
                            java.util.Map.of("fromX", cx, "fromY", cy, "toX", cx + 160, "toY", cy + 60, "duration", 0.8));
                    sleep(800);
                    System.out.println("PROBE| drew signature stroke from (" + cx + "," + cy + ")");
                    dumpMatches("clearAfterDraw", "name == 'Clear'");
                }
            } catch (Exception e) { System.out.println("PROBE| signature draw failed: " + e.getMessage()); }
            logStepWithScreenshot("form filled state");
            // 7. Save via the nav checkmark (name likely 'checkmark' — dump already shows it); try both.
            for (String cand : new String[]{"checkmark", "Done", "Save"}) {
                java.util.List<WebElement> btns = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name == '" + cand + "' AND visible == 1 AND rect.y < 260"));
                if (!btns.isEmpty()) {
                    System.out.println("PROBE| tapping save candidate '" + cand + "'");
                    Rectangle r = btns.get(0).getRect();
                    DriverManager.getDriver().executeScript("mobile: tap",
                            java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                    sleep(1200);
                    break;
                }
            }
            dumpMatches("postSaveNav", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 260");
            dumpMatches("postSaveAlerts", "type == 'XCUIElementTypeAlert'");
            // 8. Back out to Assets-in-Room: asset row should now carry a green check badge.
            tapByName("Back");
            sleep(1200);
            dumpMatches("roomRowImages", "type == 'XCUIElementTypeImage' AND visible == 1");
            dumpMatches("roomRowsAfterFile", "type == 'XCUIElementTypeCell' AND visible == 1");
            dumpMatches("roomRowChecks", "name CONTAINS 'checkmark'");
            // 9. Session TAB STRIP on v1.55 (user build shows Details/Assets/Issues/Files/More
            //    — Tasks/IR gone, 'More' NEW; if More exists, ZP-3054 may have moved there).
            dumpMatches("tabStrip", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > 800");
            java.util.List<WebElement> moreTab = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'More' AND visible == 1 AND rect.y > 800"));
            if (!moreTab.isEmpty()) {
                System.out.println("PROBE| 'More' TAB EXISTS on v1.55 — probing it (ZP-3054 relocation?)");
                Rectangle r = moreTab.get(0).getRect();
                DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                sleep(1200);
                dumpMatches("moreTabButtons", "type == 'XCUIElementTypeButton' AND visible == 1");
                dumpMatches("moreTabTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1");
            }
            // 10. Details tab: work-type chip + 'Forms Completed' ring (post-fill state).
            tapByName("Details");
            sleep(1200);
            dumpMatches("detailsChips", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND rect.y < 700");
            dumpMatches("formsCompleted", "name CONTAINS 'Forms Completed' OR name CONTAINS 'Completed'");
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe L complete");
    }

    @Test(priority = 13)
    public void PROBE_M_moreTabAndSessionTreeEntry_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE M - v1.55: 'More' tab contents (ZP-3054 relocation?) + session tab strip vs app tab bar anatomy");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        if (!wo.startFirstAvailableWorkOrder()) { System.out.println("PROBE| no WO activated"); return; }
        if (!wo.openActiveWorkOrderSession()) { System.out.println("PROBE| session did not open"); return; }
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            // 1. Bottom strip census with exact geometry (session strip vs app bar).
            for (WebElement b : DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > 780"))) {
                try {
                    Rectangle r = b.getRect();
                    System.out.println("PROBE|strip y=" + r.y + " x=" + r.x + " '" + b.getAttribute("name") + "'");
                } catch (Exception ignored) { }
            }
            // 2. Nav bar identity on the session surface.
            dumpMatches("sessNav", "type == 'XCUIElementTypeNavigationBar'");
            // 3. 'More' tab: tap + dump (ZP-3054 relocation check).
            java.util.List<WebElement> more = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'More' AND visible == 1 AND rect.y > 780"));
            System.out.println("PROBE| More tab present: " + !more.isEmpty());
            if (!more.isEmpty()) {
                Rectangle r = more.get(0).getRect();
                DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                sleep(1200);
                dumpMatches("moreButtons", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y < 780");
                dumpMatches("moreTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND rect.y < 780");
                logStepWithScreenshot("More tab contents");
            }
            // 4. Session 'Assets' tab (strip zone) → nav + FIRST tree rows, tightly bounded.
            java.util.List<WebElement> assets = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Assets' AND visible == 1 AND rect.y > 780"));
            if (!assets.isEmpty()) {
                Rectangle r = assets.get(0).getRect();
                System.out.println("PROBE| tapping session Assets tab at y=" + r.y);
                DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                sleep(1200);
                dumpMatches("treeNav", "type == 'XCUIElementTypeNavigationBar'");
                dumpMatches("treeTop", "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > 120 AND rect.y < 460");
                dumpMatches("treeTopTexts", "type == 'XCUIElementTypeStaticText' AND visible == 1 AND rect.y > 120 AND rect.y < 460");
                logStepWithScreenshot("session tree top");
            }
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe M complete");
    }

    @Test(priority = 14)
    public void PROBE_N_fixtureScrollMiss_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE N - why does scrollWorkOrderListTo miss QA-WT00 on the v1.55 list?");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        boolean found = wo.scrollWorkOrderListTo("QA-WT00 General");
        System.out.println("PROBE| scroll to QA-WT00 General: " + found);
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            dumpMatches("listNav", "type == 'XCUIElementTypeNavigationBar'");
            // Visible list rows right now (post-sweep position).
            int i = 0;
            for (WebElement b : DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > 100 AND rect.y < 820"))) {
                if (++i > 14) break;
                try {
                    System.out.println("PROBE|row y=" + b.getRect().y + " '" + b.getAttribute("name") + "'");
                } catch (Exception ignored) { }
            }
            // Any QA-WT row matchable WITHOUT visibility constraint?
            dumpMatches("qaAnywhere", "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'QA-WT'");
            dumpMatches("qaTexts", "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH 'QA-WT'");
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe N list state");
    }

    @Test(priority = 14)
    public void PROBE_O_censusBleedIdentify_v155() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE O - identify the 2 EXTRA rows the picker option census picks up (16 vs 14)");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        assertTrue(wo.openCreateForm(), "Create form must open");
        assertTrue(wo.openWorkTypePicker(), "Picker must open");
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            java.util.List<String> census = wo.getWorkTypePickerOptions(14);
            System.out.println("PROBE| census size=" + census.size());
            java.util.Set<String> known = new java.util.HashSet<>();
            for (com.egalvanic.constants.WorkTypeCatalog wt
                    : com.egalvanic.constants.WorkTypeCatalog.values()) {
                known.add(wt.displayName());
            }
            for (String c : census) {
                System.out.println("PROBE| censusRow " + (known.contains(c) ? "[option] " : "[EXTRA!] ") + "'" + c + "'");
            }
            // Geometry of the extras vs a known option, to find a scoping rule.
            for (String c : census) {
                if (known.contains(c)) continue;
                try {
                    WebElement el = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND name == '" + c.replace("'", "\\'") + "'"));
                    Rectangle r = el.getRect();
                    System.out.println("PROBE| EXTRA rect=(" + r.x + "," + r.y + "," + r.width + "," + r.height
                            + ") enabled=" + el.getAttribute("enabled") + " acc=" + el.getAttribute("accessible"));
                } catch (Exception e) {
                    System.out.println("PROBE| EXTRA '" + c + "' geometry failed: " + e.getMessage());
                }
            }
            try {
                WebElement opt = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name == 'Cleaning'"));
                Rectangle r = opt.getRect();
                System.out.println("PROBE| OPTION 'Cleaning' rect=(" + r.x + "," + r.y + "," + r.width + ","
                        + r.height + ") acc=" + opt.getAttribute("accessible"));
            } catch (Exception ignored) { }
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
        logStepWithScreenshot("probe O complete");
    }

    private void tapWorkTypeRow() {
        try {
            WebElement row = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Work Type,'"));
            Rectangle r = row.getRect();
            DriverManager.getDriver().executeScript("mobile: tap",
                    java.util.Map.of("x", r.x + 40, "y", r.y + r.height / 2));
        } catch (Exception e) { System.out.println("PROBE| tapWorkTypeRow: " + e.getMessage()); }
    }

    private void tapByName(String name) {
        try {
            WebElement el = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == '" + name + "' AND visible == 1"));
            Rectangle r = el.getRect();
            DriverManager.getDriver().executeScript("mobile: tap",
                    java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
            System.out.println("PROBE| tapped '" + name + "'");
        } catch (Exception e) { System.out.println("PROBE| tapByName(" + name + "): " + e.getMessage()); }
    }

    /** All picker option buttons with value/selected attrs (no 14-row cap). */
    private void fullOptionCensus(String tag) {
        try {
            List<WebElement> els = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > 120"));
            System.out.println("PROBE|" + tag + " optionButtons=" + els.size());
            for (WebElement el : els) {
                try {
                    Rectangle r = el.getRect();
                    System.out.println("PROBE|" + tag + " y=" + r.y + " '" + el.getAttribute("name")
                            + "' value='" + el.getAttribute("value") + "' selected=" + el.getAttribute("selected"));
                } catch (Exception ignored) { }
            }
            List<WebElement> marks = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage' AND visible == 1"));
            System.out.println("PROBE|" + tag + " images=" + marks.size());
            for (WebElement el : marks) {
                try {
                    System.out.println("PROBE|" + tag + " img y=" + el.getRect().y + " '" + el.getAttribute("name") + "'");
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            System.out.println("PROBE|" + tag + " census failed: " + e.getMessage());
        }
    }

    private void dumpMatches(String tag, String predicate) {
        try {
            List<WebElement> els = DriverManager.getDriver().findElements(
                    AppiumBy.iOSNsPredicateString(predicate));
            System.out.println("PROBE|" + tag + " count=" + els.size());
            int i = 0;
            for (WebElement el : els) {
                if (++i > 14) { System.out.println("PROBE|" + tag + " …(truncated)"); break; }
                try {
                    Rectangle r = el.getRect();
                    System.out.println("PROBE|" + tag + " [" + el.getAttribute("type")
                            + "] y=" + r.y + " '" + el.getAttribute("name") + "'");
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            System.out.println("PROBE|" + tag + " query failed: " + e.getMessage());
        }
    }
}
