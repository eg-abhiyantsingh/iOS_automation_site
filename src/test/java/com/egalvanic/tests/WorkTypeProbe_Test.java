package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import io.appium.java_client.AppiumBy;
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
 * Ground-truths the v1.51 work-type surfaces against the QA-WT00..13 fixture
 * family (docs/worktype-gold-spec-2026-07-21.md): where the work-type label
 * renders on the Work Orders list rows and on the opened WO screen. Console
 * output is the deliverable. All raw queries run with implicit wait 0 —
 * probe v1 died on ThreadTimeout (360s) from implicit-wait burn.
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
    public void PROBE_workTypeSurfaces() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "WorkType Probe",
                "PROBE - dump work-type surfaces for QA-WT fixtures");
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Work Orders screen must open");
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            probeBody();
        } finally {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
    }

    private void probeBody() {
        // Stage 0 — what does the list top look like right now?
        System.out.println("=== PROBE stage 0: list top, warm entry ===");
        dumpQaWtPresence();

        // Stage 1 — cold restart (launch-time sync), generous settle, re-enter.
        System.out.println("=== PROBE stage 1: cold restart + resync ===");
        try {
            DriverManager.getDriver().terminateApp(AppConstants.APP_BUNDLE_ID);
            Thread.sleep(500);
            DriverManager.getDriver().activateApp(AppConstants.APP_BUNDLE_ID);
            Thread.sleep(12000); // allow post-launch session sync to land
        } catch (Exception e) {
            System.out.println("PROBE| restart failed: " + e.getMessage());
        }
        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        wo.waitForWorkOrdersScreen();
        DriverManager.getDriver().manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        dumpQaWtPresence();

        // Stage 2 — pull-to-refresh gesture on the list, then recheck.
        System.out.println("=== PROBE stage 2: pull-to-refresh ===");
        try {
            java.util.Map<String, Object> drag = new java.util.HashMap<>();
            drag.put("fromX", 200); drag.put("fromY", 300);
            drag.put("toX", 200);   drag.put("toY", 800);
            drag.put("duration", 0.6);
            DriverManager.getDriver().executeScript("mobile: dragFromToForDuration", drag);
            Thread.sleep(8000);
        } catch (Exception e) {
            System.out.println("PROBE| pull-to-refresh failed: " + e.getMessage());
        }
        dumpQaWtPresence();

        // Stage 3 — site re-selection (the SLD sync trigger), then recheck.
        System.out.println("=== PROBE stage 3: site re-selection sync ===");
        try {
            DriverManager.getDriver().manage().timeouts()
                    .implicitlyWait(java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
            wo.goBack();
            shortWait();
            siteSelectionPage.switchToSiteByIndex(0);
            siteSelectionPage.waitForDashboardFast();
            Thread.sleep(8000); // let the SLD sync land
            siteSelectionPage.clickWorkOrderCard();
            shortWait();
            wo.waitForWorkOrdersScreen();
        } catch (Exception e) {
            System.out.println("PROBE| site re-selection failed: " + e.getMessage());
        }
        DriverManager.getDriver().manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        dumpQaWtPresence();

        String[] targets = {
                "QA-WT08 Infrared Thermography",     // IR
                "QA-WT01 Arc Flash Data Collection", // AF
                "QA-WT05 Condition Assessment"       // COM
        };
        for (String target : targets) {
            System.out.println("=== PROBE: target [" + target + "] ===");
            boolean found = wo.scrollWorkOrderListTo(target);
            System.out.println("PROBE| row found on list: " + found);
            if (!found) continue;
            System.out.println("PROBE| row composite: '" + wo.getWorkOrderRowComposite(target) + "'");
            boolean opened = wo.openWorkOrderByName(target);
            System.out.println("PROBE| opened (verified): " + opened);
            if (!opened) continue;
            mediumWait();
            System.out.println("=== PROBE: static texts on [" + target + "] screen ===");
            dumpVisible("XCUIElementTypeStaticText");
            System.out.println("=== PROBE: buttons on [" + target + "] screen ===");
            dumpVisible("XCUIElementTypeButton");
            System.out.println("PROBE| getWorkTypeLabelOnScreen() = '" + wo.getWorkTypeLabelOnScreen() + "'");
            wo.goBack();
            shortWait();
            if (!wo.isWorkOrdersScreenDisplayed()) {
                siteSelectionPage.clickWorkOrderCard();
                shortWait();
                wo.waitForWorkOrdersScreen();
            }
        }
        logStepWithScreenshot("probe complete");
    }

    /** Any element mentioning QA-WT on screen + the visible WO row names. */
    private void dumpQaWtPresence() {
        try {
            List<WebElement> any = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "name CONTAINS 'QA-WT' AND visible == 1"));
            System.out.println("PROBE| elements containing 'QA-WT': " + any.size());
            for (int i = 0; i < Math.min(any.size(), 5); i++) {
                System.out.println("PROBE|   [" + any.get(i).getAttribute("type") + "] '"
                        + any.get(i).getAttribute("name") + "'");
            }
        } catch (Exception e) {
            System.out.println("PROBE| QA-WT scan failed: " + e.getMessage());
        }
        for (String row : wo.getVisibleWorkOrderRowNames()) {
            System.out.println("ROW| " + row);
        }
    }

    private void dumpVisible(String type) {
        try {
            List<WebElement> els = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == '" + type + "' AND visible == 1"));
            int i = 0;
            for (WebElement el : els) {
                try {
                    String name = el.getAttribute("name");
                    String label = el.getAttribute("label");
                    org.openqa.selenium.Rectangle r = el.getRect();
                    System.out.println(type.replace("XCUIElementType", "") + "| y=" + r.getY()
                            + " name='" + name + "'"
                            + (label != null && !label.equals(name) ? " label='" + label + "'" : ""));
                } catch (Exception ignored) { }
                if (++i > 60) { System.out.println("…(truncated)"); break; }
            }
        } catch (Exception e) {
            System.out.println("PROBE| dumpVisible(" + type + ") failed: " + e.getMessage());
        }
    }
}
