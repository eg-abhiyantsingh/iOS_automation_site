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
