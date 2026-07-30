package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * TEMPORARY diagnostic — v1.51 anatomy for two clusters:
 *  A) Connections create form: Source Node row (18 failing TC_CONN_*)
 *  B) Asset Engineer: 'Add Custom' affordance (9 failing TC_ENG_*)
 * Run with -Dtest=DebugEngConnProbe_Test.
 */
public class DebugEngConnProbe_Test extends BaseTest {

    @Test
    public void PROBE_connectionsCreateFormAnatomy() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_CONNECTIONS, "Probe",
            "PROBE - v1.51 create-connection form anatomy");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.ConnectionsPage connectionsPage = new com.egalvanic.pages.ConnectionsPage();
        connectionsPage.navigateToConnectionsScreen();
        mediumWait();
        // open the create form (+)
        try {
            d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name == 'plus' OR label == 'Add') AND visible == 1")).click();
            Thread.sleep(1200);
        } catch (Exception e) {
            System.out.println("PROBE-CONN: plus tap failed: " + e.getMessage());
        }
        System.out.println("PROBE-CONN: create-form census:");
        int i = 0;
        for (WebElement e : d.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeTextField') AND visible == 1"))) {
            try {
                if (i++ > 40) break;
                System.out.println("   c: " + e.getTagName().substring(16) + " y=" + e.getRect().y
                    + " name='" + e.getAttribute("name") + "'");
            } catch (Exception ignored) {}
        }
        try {
            d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel' AND visible == 1")).click();
        } catch (Exception ignored) {}
    }
}
