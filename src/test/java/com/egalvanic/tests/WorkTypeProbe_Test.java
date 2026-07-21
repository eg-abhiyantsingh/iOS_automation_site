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
        // Run 7 (fresh install) proved the sync contract and populated the
        // local store; warm sessions are fine again. Flip back to noReset=false
        // only when the store must re-sync (see gold-spec §3b).
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
        System.out.println("PROBE| dashboard site: '" + siteSelectionPage.getCurrentSiteName() + "'");
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
        // Run 7: fresh-install session (classSetup noReset=false) — login and
        // site selection already happened in the @Test preamble, which is the
        // guaranteed SLD/session sync path. Just look at the list.
        System.out.println("=== PROBE stage 4: fresh-install sync ===");
        dumpQaWtPresence();

        // Run 10: activation-aware open. Run 9 showed a bare row tap leaves the
        // list unchanged (no alert VISIBLE — autoAcceptAlerts may race it) and
        // that generic visible==1 whole-screen dumps WEDGE WDA on this list.
        // Only bounded queries below.
        String target = "QA-WT08 Infrared Thermography";
        System.out.println("=== PROBE: target [" + target + "] ===");
        boolean found = wo.scrollWorkOrderListTo(target);
        System.out.println("PROBE| row found on list: " + found);
        if (found) {
            System.out.println("PROBE| composite: '" + wo.getWorkOrderRowComposite(target) + "'");
            System.out.println("PROBE| hasActiveWorkOrder BEFORE open: " + wo.hasActiveWorkOrder());
            boolean opened = wo.openWorkOrderByName(target);
            System.out.println("PROBE| opened (verified): " + opened);
            if (opened) {
                System.out.println("PROBE| header: '" + wo.getSessionDetailsHeaderText() + "'");
                for (String tab : new String[]{"Details", "Assets", "Locations", "Issues", "Tasks",
                        "Files", "Forms", "IR Photos", "Photos", "Panel Schedules",
                        "Condition Assessment", "SLD", "Equipment Designations"}) {
                    try {
                        if (wo.isTabDisplayed(tab)) System.out.println("PROBE| TAB present: " + tab);
                    } catch (Exception ignored) { }
                }
                System.out.println("PROBE| getSessionType(): '" + wo.getSessionType() + "'");
                System.out.println("PROBE| getWorkTypeLabelOnScreen(): '" + wo.getWorkTypeLabelOnScreen() + "'");
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
