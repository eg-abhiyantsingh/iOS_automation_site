package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * TEMPORARY diagnostic — v1.51 session Details INFORMATION anatomy (Started row)
 * + Quick QR Action dropdown options. Run with -Dtest=DebugSessionInfoProbe_Test.
 */
public class DebugSessionInfoProbe_Test extends BaseTest {

    @Test
    public void PROBE_sessionInformationAnatomy() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "Probe",
            "PROBE - v1.51 session INFORMATION + Quick QR anatomy");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        mediumWait();
        // open the first WO row
        com.egalvanic.pages.WorkOrderPage wop = new com.egalvanic.pages.WorkOrderPage();
        wop.openWorkOrderByName("");
        mediumWait();

        // census of statictexts around INFORMATION (whole Details tab)
        System.out.println("PROBE-INFO: statictext census:");
        int i = 0;
        for (WebElement t : d.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
            try {
                if (i++ > 45) break;
                System.out.println("   st: y=" + t.getRect().y + " '" + t.getAttribute("name") + "'");
            } catch (Exception ignored) {}
        }

        // Quick QR Action row: find and open
        try {
            WebElement qr = d.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Quick QR' AND visible == 1"));
            System.out.println("PROBE-INFO: QuickQR row: type=" + qr.getTagName()
                + " label=" + qr.getAttribute("label") + " y=" + qr.getRect().y);
            // buttons near the row
            for (WebElement b : d.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1"))) {
                try {
                    int y = b.getRect().y;
                    if (Math.abs(y - qr.getRect().y) < 120) {
                        System.out.println("   qr-near-btn: y=" + y + " '" + b.getAttribute("name") + "'");
                    }
                } catch (Exception ignored) {}
            }
            // tap the row's control (the row itself if it's a button)
            WebElement control = qr;
            if (!"XCUIElementTypeButton".equals(qr.getTagName())) {
                try {
                    control = d.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND label CONTAINS 'QR'"));
                } catch (Exception e) { control = null; }
            }
            if (control != null) {
                control.click();
                Thread.sleep(1000);
                System.out.println("PROBE-INFO: after QuickQR tap — visible buttons/texts:");
                for (WebElement e : d.findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND visible == 1"))) {
                    try {
                        System.out.println("   opt: " + e.getTagName().substring(16) + " y=" + e.getRect().y
                            + " '" + e.getAttribute("name") + "'");
                    } catch (Exception ignored) {}
                }
                // close whatever opened
                try { d.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label == 'Cancel' OR label == 'Done')")).click(); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("PROBE-INFO: QuickQR row not found: " + e.getClass().getSimpleName());
        }
    }
}
