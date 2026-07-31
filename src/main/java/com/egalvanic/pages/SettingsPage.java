package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import com.egalvanic.utils.Waits;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Settings tab page object (Settings › Account / Language / Session Analytics /
 * Sync & Network sections).
 *
 * Primary job: the "Session Recording" analytics toggle (Session Analytics
 * section). It defaults ON in every fresh install and — per the app's own
 * subtitle — "Keeping this off improves performance and reduces battery
 * usage". Since NO_RESET=false means every new Appium session is a clean
 * install, BaseTest auto-disables it once per install via
 * {@code ensureSessionRecordingDisabledIfFreshInstall()}.
 *
 * Locator notes:
 * - Tab-bar buttons swallow element.click() on v1.50+ → coordinate tap.
 * - The app cannot be forced to English (custom appLanguage plist key), so
 *   every label predicate carries the French variants too.
 * - This screen has MULTIPLE switches (Session Recording, Network Mode, and
 *   Equipment Library toggles below the fold). Never grab "the first switch";
 *   every strategy anchors on the Session Recording label/name. Toggling
 *   Network Mode by mistake would put the whole suite offline.
 */
public class SettingsPage extends BasePage {

    /** Tab-bar / nav-bar titles: English + French (app may persist Français). */
    private static final String SETTINGS_TITLES =
            "(label == 'Settings' OR label == 'Réglages' OR label == 'Paramètres' "
            + "OR name == 'Settings' OR name == 'Réglages' OR name == 'Paramètres')";

    /** Session Recording row title, EN + FR. */
    private static final String RECORDING_LABELS =
            "(label CONTAINS[c] 'Session Recording' OR name CONTAINS[c] 'Session Recording' "
            + "OR label CONTAINS[c] 'Enregistrement' OR name CONTAINS[c] 'Enregistrement')";

    // ================================================================
    // NAVIGATION
    // ================================================================

    /** W3C-safe coordinate press — v1.50 tab/tile Buttons swallow element.click(). */
    private void tapCenter(WebElement el) {
        org.openqa.selenium.Rectangle r = el.getRect();
        driver.executeScript("mobile: tap", java.util.Map.of(
                "x", r.x + r.width / 2, "y", r.y + r.height / 2));
    }

    /**
     * Open the Settings tab from any screen that shows the bottom tab bar.
     * Strategies: tab-bar button (bottom-anchored) → any Settings button →
     * accessibility id → bottom-right coordinate tap.
     */
    public boolean openSettingsTab() {
        if (isSettingsScreenDisplayed()) {
            return true;
        }

        // Strategy 1+2: Settings button, preferring the bottom tab bar (y > 600pt)
        try {
            List<WebElement> buttons = withImplicitWait(300, () ->
                    driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND " + SETTINGS_TITLES)));
            WebElement fallback = null;
            for (WebElement btn : buttons) {
                if (btn.getLocation().getY() > 600) {
                    tapCenter(btn);
                    if (Waits.until(this::isSettingsScreenDisplayed, 4000)) return true;
                } else if (fallback == null) {
                    fallback = btn;
                }
            }
            if (fallback != null) {
                tapCenter(fallback);
                if (Waits.until(this::isSettingsScreenDisplayed, 4000)) return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Settings tab button search failed: " + e.getMessage());
        }

        // Strategy 3: accessibility id
        try {
            WebElement el = withImplicitWait(300, () ->
                    driver.findElement(AppiumBy.accessibilityId("Settings")));
            tapCenter(el);
            if (Waits.until(this::isSettingsScreenDisplayed, 4000)) return true;
        } catch (Exception ignored) { }

        // Strategy 4: coordinate tap on the bottom-right tab slot
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            driver.executeScript("mobile: tap", java.util.Map.of(
                    "x", (int) (size.getWidth() * 0.9), "y", size.getHeight() - 30));
            return Waits.until(this::isSettingsScreenDisplayed, 4000);
        } catch (Exception e) {
            System.out.println("⚠️ Settings coordinate tap failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Settings screen proof: nav bar titled Settings (type is unambiguous), a
     * top-anchored Settings static text (tab-bar copies excluded by y < 500),
     * or the unique "Session Analytics" section header.
     */
    public boolean isSettingsScreenDisplayed() {
        try {
            return withImplicitWait(200, () -> {
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeNavigationBar' OR type == 'XCUIElementTypeStaticText')"
                        + " AND " + SETTINGS_TITLES))) {
                    try {
                        String type = el.getAttribute("type");
                        if ("XCUIElementTypeNavigationBar".equals(type)) return true;
                        if (el.getLocation().getY() < 500 && el.isDisplayed()) return true;
                    } catch (Exception ignored) { }
                }
                return !driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND "
                        + "(label CONTAINS[c] 'Session Analytics' OR label CONTAINS[c] 'Sync & Network')"))
                        .isEmpty();
            });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Return to the Site tab (Dashboard root). Callers re-verify the dashboard;
     * this only performs the tap.
     */
    public boolean openSiteTab() {
        try {
            List<WebElement> buttons = withImplicitWait(300, () ->
                    driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND (label == 'Site' OR name == 'Site')")));
            for (WebElement btn : buttons) {
                if (btn.getLocation().getY() > 600) {
                    tapCenter(btn);
                    return Waits.until(() -> !isSettingsScreenDisplayed(), 4000);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Site tab button search failed: " + e.getMessage());
        }
        // Coordinate fallback: bottom-left tab slot
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            driver.executeScript("mobile: tap", java.util.Map.of(
                    "x", (int) (size.getWidth() * 0.1), "y", size.getHeight() - 30));
            return Waits.until(() -> !isSettingsScreenDisplayed(), 4000);
        } catch (Exception e) {
            System.out.println("⚠️ Site tab coordinate tap failed: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // SESSION RECORDING TOGGLE
    // ================================================================

    /**
     * Locate the Session Recording switch. Strategies (fast → slow), all
     * anchored to the row label so Network Mode can never be matched:
     * 1. Switch whose own name/label carries the row title (SwiftUI usually
     *    exposes the Toggle this way).
     * 2. Row-anchored: the switch vertically nearest the title text (≤ 60pt).
     * 3. XPath document-order: first switch following the title text.
     * 4. One scroll down, then retry 1-3 (small devices).
     *
     * @return the switch element, or null if not found.
     */
    public WebElement findSessionRecordingSwitch() {
        WebElement sw = findSwitchNoScroll();
        if (sw != null) return sw;
        try {
            driver.executeScript("mobile: scroll", java.util.Map.of("direction", "down"));
        } catch (Exception ignored) { }
        return findSwitchNoScroll();
    }

    private WebElement findSwitchNoScroll() {
        // Strategy 1: the switch itself is named after the row
        try {
            List<WebElement> named = withImplicitWait(200, () ->
                    driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeSwitch' AND " + RECORDING_LABELS)));
            if (!named.isEmpty()) return named.get(0);
        } catch (Exception ignored) { }

        // Strategy 2: nearest switch to the row title (same row ⇒ |Δcenter-y| ≤ 60pt)
        try {
            WebElement anchor = withImplicitWait(200, () -> {
                List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " + RECORDING_LABELS));
                return labels.isEmpty() ? null : labels.get(0);
            });
            if (anchor != null) {
                int anchorY = anchor.getRect().y + anchor.getRect().height / 2;
                List<WebElement> switches = withImplicitWait(200, () ->
                        driver.findElements(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeSwitch'")));
                WebElement best = null;
                int bestDelta = 61;
                for (WebElement sw : switches) {
                    int delta = Math.abs(sw.getRect().y + sw.getRect().height / 2 - anchorY);
                    if (delta < bestDelta) {
                        bestDelta = delta;
                        best = sw;
                    }
                }
                if (best != null) return best;
            }
        } catch (Exception ignored) { }

        // Strategy 3: document order — first switch after the title text
        try {
            List<WebElement> els = withImplicitWait(200, () -> driver.findElements(AppiumBy.xpath(
                    "(//XCUIElementTypeStaticText[contains(@label,'Session Recording')"
                    + " or contains(@label,'Enregistrement')]/following::XCUIElementTypeSwitch)[1]")));
            if (!els.isEmpty()) return els.get(0);
        } catch (Exception ignored) { }

        return null;
    }

    /** Current toggle value: "1" = ON, "0" = OFF, null = switch/value not readable. */
    public String sessionRecordingValue() {
        try {
            WebElement sw = findSessionRecordingSwitch();
            return sw == null ? null : sw.getAttribute("value");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Idempotently turn Session Recording OFF. Assumes the Settings screen is
     * open. Tap → verify by value readback → coordinate-tap retry.
     *
     * @return true only when the switch reads "0" at the end (already-off
     *         counts); false when the switch is missing or still ON.
     */
    public boolean disableSessionRecordingIfOn() {
        WebElement sw = findSessionRecordingSwitch();
        if (sw == null) {
            System.out.println("⚠️ Session Recording switch not found on Settings screen");
            return false;
        }
        String value = safeValue(sw);
        if ("0".equals(value)) {
            System.out.println("✅ Session Recording already OFF");
            return true;
        }
        System.out.println("🎛️ Session Recording is " + ("1".equals(value) ? "ON" : "value=" + value)
                + " — toggling OFF");

        try {
            sw.click();
        } catch (Exception e) {
            System.out.println("⚠️ switch.click() failed (" + e.getMessage() + ") — coordinate tap");
            try { tapCenter(sw); } catch (Exception ignored) { }
        }
        if (Waits.until(() -> "0".equals(sessionRecordingValueFast()), 3000)) {
            System.out.println("✅ Session Recording OFF (verified by value readback)");
            return true;
        }

        // click() can silently no-op on SwiftUI toggles — coordinate tap and re-verify
        try {
            WebElement fresh = findSessionRecordingSwitch();
            if (fresh != null) tapCenter(fresh);
        } catch (Exception ignored) { }
        boolean off = Waits.until(() -> "0".equals(sessionRecordingValueFast()), 3000);
        System.out.println(off ? "✅ Session Recording OFF (coordinate-tap retry)"
                : "❌ Session Recording still not OFF after tap + retry");
        return off;
    }

    /** Poll-friendly readback: re-finds the switch each call (stale-safe), no scroll, zero implicit wait. */
    private String sessionRecordingValueFast() {
        try {
            return withImplicitWait(0, () -> {
                WebElement sw = findSwitchNoScroll();
                return sw == null ? null : sw.getAttribute("value");
            });
        } catch (Exception e) {
            return null;
        }
    }

    private String safeValue(WebElement sw) {
        try {
            return sw.getAttribute("value");
        } catch (Exception e) {
            return null;
        }
    }
}
