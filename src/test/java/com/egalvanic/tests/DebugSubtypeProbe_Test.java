package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * TEMPORARY diagnostic — v1.51 Asset Subtype row anatomy on the edit screen.
 * Run with -Dtest=DebugSubtypeProbe_Test.
 */
public class DebugSubtypeProbe_Test extends BaseTest {

    @Test
    public void PROBE_subtypeRowAnatomy() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, "Probe",
            "PROBE - v1.51 Asset Subtype row anatomy (ATS)");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        assetPage.openSharedAssetForEditOrFallback(null);
        assetPage.changeAssetClassToATS();
        shortWait();

        for (int pass = 0; pass < 3; pass++) {
            java.util.List<WebElement> els = d.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS[c] 'subtype' OR name CONTAINS[c] 'subtype' OR value CONTAINS[c] 'subtype'"));
            System.out.println("PROBE-ST pass " + pass + ": " + els.size() + " subtype-ish elements");
            for (WebElement e : els) {
                try {
                    System.out.println("   el: type=" + e.getTagName()
                        + " name=" + e.getAttribute("name")
                        + " label=" + e.getAttribute("label")
                        + " value=" + e.getAttribute("value")
                        + " visible=" + e.getAttribute("visible")
                        + " y=" + e.getRect().y);
                } catch (Exception ignored) {}
            }
            if (!els.isEmpty()) break;
            assetPage.scrollFormDown();
            shortWait();
        }

        // Set-state variant: rows carrying the subtype VALUE ('Transfer Switch')
        java.util.List<WebElement> vals = d.findElements(AppiumBy.iOSNsPredicateString(
            "label CONTAINS 'Transfer Switch' OR name CONTAINS 'Transfer Switch'"));
        System.out.println("PROBE-ST set-state: " + vals.size() + " 'Transfer Switch' elements");
        for (WebElement e : vals) {
            try {
                System.out.println("   ts: type=" + e.getTagName()
                    + " name=" + e.getAttribute("name")
                    + " visible=" + e.getAttribute("visible")
                    + " y=" + e.getRect().y);
            } catch (Exception ignored) {}
        }
        // Full static-text census of the lower form region for orientation
        int i = 0;
        for (WebElement t : d.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
            try {
                int y = t.getRect().y;
                if (y > 700 && i++ < 30) {
                    System.out.println("   census: y=" + y + " '" + t.getAttribute("name") + "'");
                }
            } catch (Exception ignored) {}
        }
    }
}
