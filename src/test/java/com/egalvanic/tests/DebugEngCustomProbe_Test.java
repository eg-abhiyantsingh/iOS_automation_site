package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * TEMPORARY diagnostic — v1.51 Asset Engineer engineering-section anatomy on a
 * Fuse ('Add Custom' affordance). Run with -Dtest=DebugEngCustomProbe_Test.
 */
public class DebugEngCustomProbe_Test extends BaseTest {

    @Test
    public void PROBE_engineeringSectionAnatomy() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, "Probe",
            "PROBE - v1.51 engineering section / Add Custom anatomy (fuse)");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.AssetEngineerPage eng = new com.egalvanic.pages.AssetEngineerPage();
        eng.openAssetCardByPrefix("Trim600639 Fuse");
        boolean engReachable = eng.swipeToEngineeringSection();
        System.out.println("PROBE-ENG: engineering section reachable = " + engReachable);

        // census of the engineering region: buttons + statictexts on screen
        System.out.println("PROBE-ENG: on-screen census:");
        int i = 0;
        for (WebElement e : d.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND visible == 1"))) {
            try {
                if (i++ > 40) break;
                System.out.println("   e: " + e.getTagName().substring(16) + " y=" + e.getRect().y
                    + " '" + e.getAttribute("name") + "'");
            } catch (Exception ignored) {}
        }
        // anything custom-ish in the whole tree
        for (WebElement e : d.findElements(AppiumBy.iOSNsPredicateString(
                "name CONTAINS[c] 'custom' OR label CONTAINS[c] 'custom'"))) {
            try {
                System.out.println("   custom-hit: " + e.getTagName().substring(16)
                    + " y=" + e.getRect().y + " '" + e.getAttribute("name") + "' visible=" + e.getAttribute("visible"));
            } catch (Exception ignored) {}
        }
    }
}
