package com.egalvanic.verify;

import com.egalvanic.utils.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Detects broken UI states the happy-path suite walks straight past: blank screens,
 * never-resolving spinners, and unexpected error alerts (which the driver currently
 * auto-accepts via {@code appium:autoAcceptAlerts=true}, hiding them entirely).
 */
public final class UIStateValidator {

    private IOSDriver driver() { return DriverManager.getDriver(); }

    /** A real screen must have meaningful content — guards white/blank screens after navigation. */
    public void assertNotBlank(String screen) {
        long content = driver().findElements(AppiumBy.iOSNsPredicateString(
                "visible == 1 AND (type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
              + "OR type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeImage' "
              + "OR type == 'XCUIElementTypeTextField')")).size();
        if (content < 2) {
            throw new VerificationError("Blank/empty screen: " + screen
                    + " (only " + content + " visible content elements)");
        }
    }

    /** A spinner must resolve within the timeout, otherwise it is a hang — not a pass. */
    public void assertSpinnerResolves(String screen, int timeoutSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < end) {
            if (spinners().isEmpty()) return;
            pause(250);
        }
        throw new VerificationError("Activity indicator never resolved on " + screen
                + " within " + timeoutSec + "s (stuck loading)");
    }

    /** Unexpected error alert => fail (otherwise auto-accepted and invisible). */
    public void assertNoErrorAlert() {
        List<WebElement> alerts = driver().findElements(AppiumBy.iOSClassChain("**/XCUIElementTypeAlert"));
        for (WebElement a : alerts) {
            String txt = text(a).toLowerCase();
            if (txt.contains("error") || txt.contains("failed") || txt.contains("went wrong")
                    || txt.contains("unable") || txt.contains("crash")) {
                throw new VerificationError("Error alert surfaced: \"" + text(a) + "\"");
            }
        }
    }

    private List<WebElement> spinners() {
        return driver().findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeActivityIndicator' AND visible == 1"));
    }

    private static String text(WebElement e) {
        try { String t = e.getText(); return t == null ? "" : t; } catch (Exception ex) { return ""; }
    }

    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
