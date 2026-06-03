package com.egalvanic.verify;

import com.egalvanic.utils.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * iOS accessibility (VoiceOver / WCAG-for-mobile) verifier — NOT axe-core (that's web).
 *
 * Catches the mobile-a11y bug class XCUITest presence checks never see: actionable
 * controls with no accessibility label (VoiceOver reads them as just "button"), so a
 * blind user cannot operate the screen. This is the iOS twin of the web 'button-name'
 * violations axe surfaced — and on a native app it's a legal/UX defect, not cosmetic.
 *
 * Detection is element-tree based (reliable via XCUITest): every actionable element
 * (button, switch, text field, cell, segmented control) must expose a non-blank
 * `name`/`label`. Dynamic Type scaling + colour contrast need pixel/Instruments work;
 * those are flagged as a follow-up, not silently claimed.
 *
 * Matches the existing verify-package style: instantiated, uses DriverManager, throws
 * VerificationError to hard-fail the build.
 */
public final class A11yVerifier {

    private IOSDriver driver() { return DriverManager.getDriver(); }

    /** Actionable element types a VoiceOver user must be able to identify. */
    private static final List<String> ACTIONABLE = Arrays.asList(
            "XCUIElementTypeButton",
            "XCUIElementTypeSwitch",
            "XCUIElementTypeTextField",
            "XCUIElementTypeSecureTextField",
            "XCUIElementTypeSegmentedControl",
            "XCUIElementTypeCell");

    /**
     * Hard-fail if any VISIBLE actionable control on the current screen lacks an
     * accessibility label. {@code screen} names the screen for the report.
     */
    public void assertActionablesLabeled(String screen) {
        List<String> violations = new ArrayList<>();
        for (String type : ACTIONABLE) {
            List<WebElement> els;
            try {
                els = driver().findElements(AppiumBy.className(type));
            } catch (Exception e) {
                continue;
            }
            for (WebElement el : els) {
                try {
                    if (!el.isDisplayed()) continue;
                    String name = firstNonBlank(el.getAttribute("name"), el.getAttribute("label"));
                    if (name == null) {
                        String rect = safe(el.getAttribute("rect"));
                        violations.add(type + " with no accessibility label/name @ " + rect);
                    }
                } catch (Exception ignoredElementGoneStale) {
                    // element vanished mid-scan — not an a11y finding
                }
            }
        }
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("[A11yVerifier] " + screen + " has "
                    + violations.size() + " unlabeled actionable control(s) (VoiceOver-inoperable):");
            for (String v : violations) sb.append("\n  - ").append(v);
            throw new VerificationError(sb.toString());
        }
    }

    /**
     * Warn-level inventory (no fail) — returns the count of unlabeled actionables so a
     * test can log a11y debt without breaking the build during rollout.
     */
    public int countUnlabeledActionables() {
        int n = 0;
        for (String type : ACTIONABLE) {
            try {
                for (WebElement el : driver().findElements(AppiumBy.className(type))) {
                    if (el.isDisplayed()
                            && firstNonBlank(el.getAttribute("name"), el.getAttribute("label")) == null) {
                        n++;
                    }
                }
            } catch (Exception ignored) { /* type absent on screen */ }
        }
        return n;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private static String safe(String s) { return s == null ? "?" : s; }
}
