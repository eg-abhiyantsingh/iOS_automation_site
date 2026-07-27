package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Page object for the work-type PM-FORMS execution flow (v1.51, live-probed
 * 2026-07-27 — probe runs 12-15; user-verified screenshots same day).
 *
 * Anatomy contract (all probe-verified):
 *  - WO list rows are ONE full-width Button '<name>, <type label>, <priority>';
 *    the trailing activation CIRCLE is a tap ZONE at the right edge, raising
 *    the 'Start Work Order'/'Cancel' alert; ACTIVE rows append ', ACTIVE' to
 *    the composite and show an 'ACTIVE' StaticText; the Start-New button's
 *    composite carries its state ('…, End current work order session first'
 *    while a session is active / '…, Begin capturing IR photos…' when free).
 *  - Session Assets tab: tree of 'Bldg_x, N floors' / 'Floor …' Buttons;
 *    expanding a floor reveals room Buttons named '<room>, N assets'.
 *  - Assets-in-Room rows: Buttons '<asset name>, <class>, <formCount>' — the
 *    trailing number is the per-asset FORM BADGE.
 *  - Form screen (opens on asset tap): chip strip at y≈69 with one Button per
 *    form instance named '<Work Type> — <Procedure>' + a 'plus' Button; nav
 *    Buttons Back / trash / square.and.pencil / checkmark; 'Procedure Steps'
 *    info; 'Result' + 'Value / Notes' table headers; per-step Result dropdown
 *    Buttons (named '—' until set, then 'Pass'/'Fail') with a sibling
 *    TextField per row for Value/Notes; a Fail result reveals a
 *    'Description of Failure' section with a Photos picker.
 *
 * GIANT-DOM RULES: every query here is TYPE-bound (untyped name-CONTAINS
 * scans wedge WDA — probe run 13) and the session tree is only walked through
 * the bounded helpers below.
 */
public class WorkOrderFormsPage extends BasePage {

    /** Chip strip lives at the very top of the form sheet (probe: y≈69). */
    private static final int CHIP_ZONE_MAX_Y = 160;
    /** Result table rows start below the headers (probe: headers y≈307). */
    private static final int TABLE_ZONE_MIN_Y = 320;

    private static String pq(String s) {
        return "'" + s.replace("'", "\\'") + "'";
    }

    // ═══════════════════════ WO list — circle activation ═══════════════════

    /** The Start-New button composite carries the session state. */
    public String getStartNewComposite() {
        try {
            return withImplicitWait(0, () -> {
                List<WebElement> els = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Start New Work Order'"));
                return els.isEmpty() ? null : els.get(0).getAttribute("name");
            });
        } catch (Exception e) {
            return null;
        }
    }

    /** True when a session is active (Start-New demands ending it first). */
    public boolean isStartNewBlockedByActiveSession() {
        String c = getStartNewComposite();
        return c != null && c.contains("End current work order session");
    }

    /**
     * Composite of the row for {@code namePrefix}, or null (raw — may end
     * ', ACTIVE'). ACTIVE rows can report visible==0 while rendering (probe
     * 2026-07-27), so a rect-checked no-visible-filter fallback backs up the
     * strict query — same contract as WorkOrderPage.onScreenRowOrNull.
     */
    public String rowComposite(String namePrefix) {
        try {
            return withImplicitWait(0, () -> {
                List<WebElement> rows = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH "
                        + pq(namePrefix)));
                if (!rows.isEmpty()) return rows.get(0).getAttribute("name");
                int screenH;
                try {
                    screenH = driver.manage().window().getSize().getHeight();
                } catch (Exception e) {
                    screenH = 900;
                }
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name BEGINSWITH " + pq(namePrefix)))) {
                    try {
                        Rectangle r = el.getRect();
                        if (r.height > 20 && r.y > 80 && r.y < screenH - 40) {
                            return el.getAttribute("name");
                        }
                    } catch (Exception ignored) { }
                }
                return null;
            });
        } catch (Exception e) {
            return null;
        }
    }

    /** ACTIVE-badge contract: the active row's composite ends with ', ACTIVE'. */
    public boolean isRowActive(String namePrefix) {
        String c = rowComposite(namePrefix);
        return c != null && c.endsWith(", ACTIVE");
    }

    /**
     * Tap the activation CIRCLE (right-edge zone) of the row WITHOUT
     * confirming the alert. Returns true when the 'Start Work Order' alert is
     * up (alerts are left paused=manual for the caller to Confirm/Cancel via
     * {@link #confirmStartAlert()} / {@link #cancelStartAlert()}).
     */
    public boolean tapCircleExpectAlert(String namePrefix) {
        try {
            driver.setSetting("defaultAlertAction", "");
            WebElement row = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH "
                    + pq(namePrefix)));
            Rectangle r = row.getRect();
            driver.executeScript("mobile: tap",
                    Map.of("x", r.x + r.width - 35, "y", r.y + r.height / 2));
            return waitForCondition(() -> existsNow(START_ALERT_CONFIRM), 6);
        } catch (Exception e) {
            System.out.println("⚠️ tapCircleExpectAlert: " + e.getMessage());
            return false;
        }
    }

    private static final By START_ALERT_CONFIRM = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name == 'Start Work Order' AND visible == 1");
    private static final By START_ALERT_CANCEL = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name == 'Cancel' AND visible == 1");

    /** Confirm the Start alert by coordinates; restores auto-accept. */
    public boolean confirmStartAlert() {
        boolean ok = tapAlertButton(START_ALERT_CONFIRM);
        restoreAutoAlerts();
        return ok;
    }

    /** Cancel the Start alert by coordinates; restores auto-accept. */
    public boolean cancelStartAlert() {
        boolean ok = tapAlertButton(START_ALERT_CANCEL);
        restoreAutoAlerts();
        return ok;
    }

    private boolean tapAlertButton(By locator) {
        try {
            WebElement btn = driver.findElement(locator);
            Rectangle r = btn.getRect();
            driver.executeScript("mobile: tap",
                    Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ tapAlertButton: " + e.getMessage());
            return false;
        }
    }

    private void restoreAutoAlerts() {
        try { driver.setSetting("defaultAlertAction", "accept"); } catch (Exception ignored) { }
    }

    /** Count of ACTIVE StaticText badges currently visible (radio invariant ≤ 1). */
    public int visibleActiveBadgeCount() {
        try {
            return withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'ACTIVE' AND visible == 1")).size());
        } catch (Exception e) {
            return -1;
        }
    }

    // ═══════════════════ session tree → room with assets ═══════════════════

    /**
     * From the session's Assets tab, expand floors as needed and open the
     * first room advertising a non-zero asset count ('<room>, N assets').
     * Bounded: at most one floor expansion + a short scroll sweep.
     */
    public boolean openFirstRoomWithAssetsInTree() {
        try {
            return withImplicitWait(0, () -> {
                List<WebElement> rooms = roomsWithAssets();
                if (rooms.isEmpty()) {
                    List<WebElement> floors = driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH 'Floor'"));
                    if (!floors.isEmpty()) {
                        try {
                            System.out.println("🌳 expanding floor: " + floors.get(0).getAttribute("name"));
                            floors.get(0).click();
                        } catch (Exception ignored) { }
                        pauseMs(800);
                    }
                    for (int i = 0; i < 6 && rooms.isEmpty(); i++) {
                        rooms = roomsWithAssets();
                        if (rooms.isEmpty()) swipe("up");
                    }
                }
                if (rooms.isEmpty()) {
                    System.out.println("⚠️ no '<room>, N assets' row found in the session tree");
                    return false;
                }
                String name = rooms.get(0).getAttribute("name");
                System.out.println("🚪 opening room: " + name);
                rooms.get(0).click();
                return true;
            });
        } catch (Exception e) {
            System.out.println("⚠️ openFirstRoomWithAssetsInTree: " + e.getMessage());
            return false;
        }
    }

    private List<WebElement> roomsWithAssets() {
        List<WebElement> rows = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS ' assets'"));
        List<WebElement> nonZero = new ArrayList<>();
        for (WebElement r : rows) {
            try {
                String n = r.getAttribute("name");
                if (n != null && !n.matches(".*,\\s*0 assets.*")) nonZero.add(r);
            } catch (Exception ignored) { }
        }
        return nonZero;
    }

    /** 'Assets in Room' nav bar present? */
    public boolean isAssetsInRoomOpen() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar' AND name == 'Assets in Room'"));
    }

    // ═══════════════════ assets-in-room rows + form badge ═══════════════════

    /** Visible asset-row composites ('<name>, <class>, <formCount>'). */
    public List<String> visibleAssetRowComposites() {
        List<String> out = new ArrayList<>();
        try {
            withImplicitWait(0, () -> {
                int i = 0;
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND name MATCHES '.+, .+, \\\\d+'"))) {
                    try {
                        String n = el.getAttribute("name");
                        if (n != null) out.add(n);
                    } catch (Exception ignored) { }
                    if (++i >= 15) break;
                }
                return null;
            });
        } catch (Exception e) {
            System.out.println("⚠️ visibleAssetRowComposites: " + e.getMessage());
        }
        return out;
    }

    /** Form-badge count parsed off the asset row, or -1 when not found. */
    public int assetFormBadge(String assetNamePrefix) {
        String composite = rowComposite(assetNamePrefix);
        if (composite == null) return -1;
        try {
            String[] parts = composite.split(",\\s*");
            return Integer.parseInt(parts[parts.length - 1].trim());
        } catch (Exception e) {
            return -1;
        }
    }

    /** Tap the asset row → its form screen opens (verified via chip strip). */
    public boolean openAssetForms(String assetNamePrefix) {
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH "
                    + pq(assetNamePrefix))).click();
        } catch (Exception e) {
            System.out.println("⚠️ openAssetForms tap: " + e.getMessage());
            return false;
        }
        return waitForCondition(this::isFormScreenOpen, 8);
    }

    // ═══════════════════════════ form screen ════════════════════════════════

    /** Form screen signature: the 'Procedure Steps' info + a chip in the top zone. */
    public boolean isFormScreenOpen() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH 'Procedure Steps'"))
            || !getFormChipNames().isEmpty();
    }

    /** Names of the form-instance chips (top strip), in visual order. */
    public List<String> getFormChipNames() {
        List<String> out = new ArrayList<>();
        try {
            withImplicitWait(0, () -> {
                Set<String> seen = new LinkedHashSet<>();
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1"))) {
                    try {
                        Rectangle r = el.getRect();
                        if (r.y > CHIP_ZONE_MAX_Y) continue;
                        String n = el.getAttribute("name");
                        if (n == null || n.isEmpty()) continue;
                        if (n.equals("plus") || n.equals("Back") || n.equals("trash")
                                || n.equals("checkmark") || n.equals("square.and.pencil")) continue;
                        seen.add(n);
                    } catch (Exception ignored) { }
                }
                out.addAll(seen);
                return null;
            });
        } catch (Exception e) {
            System.out.println("⚠️ getFormChipNames: " + e.getMessage());
        }
        return out;
    }

    /** Select a form-instance chip by (partial) name. */
    public boolean selectFormChip(String nameFragment) {
        try {
            List<WebElement> chips = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS "
                            + pq(nameFragment))));
            for (WebElement chip : chips) {
                if (chip.getRect().y <= CHIP_ZONE_MAX_Y) {
                    chip.click();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ selectFormChip: " + e.getMessage());
            return false;
        }
    }

    /** Nav-zone control presence (Back / trash / square.and.pencil / checkmark / plus). */
    public boolean isFormControlPresent(String controlName) {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name == " + pq(controlName) + " AND visible == 1"));
    }

    /**
     * Per-step Result dropdown Buttons, top-to-bottom (table zone only — chips
     * for unfilled forms are also named '—', so geometry disambiguates).
     * Values: '—' (unset), 'Pass', 'Fail'.
     */
    public List<WebElement> resultDropdowns() {
        List<WebElement> out = new ArrayList<>();
        try {
            withImplicitWait(0, () -> {
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND "
                        + "(name == '—' OR name == 'Pass' OR name == 'Fail')"))) {
                    try {
                        if (el.getRect().y >= TABLE_ZONE_MIN_Y) out.add(el);
                    } catch (Exception ignored) { }
                }
                return null;
            });
        } catch (Exception e) {
            System.out.println("⚠️ resultDropdowns: " + e.getMessage());
        }
        return out;
    }

    public int stepCount() {
        return resultDropdowns().size();
    }

    /** Current value of the Nth (0-based) step's Result dropdown, or null. */
    public String stepResult(int index) {
        List<WebElement> dds = resultDropdowns();
        if (index < 0 || index >= dds.size()) return null;
        try {
            return dds.get(index).getAttribute("name");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Open the Nth step's Result dropdown and choose {@code value} (Pass/Fail).
     * The options render as Buttons/StaticTexts once the dropdown opens —
     * multi-strategy tap on the exact value below the dropdown's own row.
     */
    public boolean setStepResult(int index, String value) {
        List<WebElement> dds = resultDropdowns();
        if (index < 0 || index >= dds.size()) return false;
        try {
            WebElement dd = dds.get(index);
            Rectangle ddRect = dd.getRect();
            dd.click();
            pauseMs(700);
            // Strategy 1: exact-name Button that is NOT one of the table cells we
            // already track (a fresh option appears once the picker is open).
            List<WebElement> options = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND name == " + pq(value) + " AND visible == 1")));
            for (WebElement opt : options) {
                Rectangle r = opt.getRect();
                boolean isTheDropdownItself = Math.abs(r.y - ddRect.y) < 5 && Math.abs(r.x - ddRect.x) < 5;
                if (!isTheDropdownItself) {
                    opt.click();
                    return waitForCondition(() -> value.equals(stepResult(index)), 5);
                }
            }
            // Strategy 2: StaticText option (menu items are sometimes texts).
            List<WebElement> textOpts = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND name == " + pq(value) + " AND visible == 1")));
            for (WebElement opt : textOpts) {
                Rectangle r = opt.getRect();
                if (r.y < ddRect.y - 10 || r.y > ddRect.y + 10) {
                    opt.click();
                    return waitForCondition(() -> value.equals(stepResult(index)), 5);
                }
            }
            System.out.println("⚠️ setStepResult: no '" + value + "' option surfaced");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ setStepResult: " + e.getMessage());
            return false;
        }
    }

    /** Value/Notes TextFields in the table zone, top-to-bottom. */
    public List<WebElement> noteFields() {
        List<WebElement> out = new ArrayList<>();
        try {
            withImplicitWait(0, () -> {
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND visible == 1"))) {
                    try {
                        if (el.getRect().y >= TABLE_ZONE_MIN_Y - 20) out.add(el);
                    } catch (Exception ignored) { }
                }
                return null;
            });
        } catch (Exception e) {
            System.out.println("⚠️ noteFields: " + e.getMessage());
        }
        return out;
    }

    /** Type into the Nth step's Value/Notes field, then dismiss the keyboard. */
    public boolean typeStepNotes(int index, String text) {
        List<WebElement> fields = noteFields();
        if (index < 0 || index >= fields.size()) return false;
        try {
            WebElement f = fields.get(index);
            f.click();
            f.clear();
            f.sendKeys(text);
            hideKeyboardSafe();
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ typeStepNotes: " + e.getMessage());
            return false;
        }
    }

    /** Readback of the Nth step's Value/Notes field ('value' attribute). */
    public String stepNotes(int index) {
        List<WebElement> fields = noteFields();
        if (index < 0 || index >= fields.size()) return null;
        try {
            return fields.get(index).getAttribute("value");
        } catch (Exception e) {
            return null;
        }
    }

    /** The Fail-path failure card ('… — Failure Details' / 'Description of Failure'). */
    public boolean isFailureDetailsVisible() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == 1 AND "
                + "(name CONTAINS 'Failure Details' OR name == 'Description of Failure')"));
    }

    /** Type the failure description (TextView inside the failure card). */
    public boolean typeFailureDescription(String text) {
        try {
            List<WebElement> views = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextView' AND visible == 1")));
            if (views.isEmpty()) return false;
            WebElement v = views.get(views.size() - 1);
            v.click();
            v.sendKeys(text);
            hideKeyboardSafe();
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ typeFailureDescription: " + e.getMessage());
            return false;
        }
    }

    /** Save/complete the form via the checkmark nav control. */
    public boolean saveForm() {
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'checkmark' AND visible == 1")).click();
            pauseMs(800);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ saveForm: " + e.getMessage());
            return false;
        }
    }

    /** Leave the form screen via Back. */
    public boolean backFromForm() {
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Back' AND visible == 1")).click();
            pauseMs(600);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ backFromForm: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────── internals ─────────────────────────────────

    private void swipe(String direction) {
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("direction", direction);
            driver.executeScript("mobile: swipe", args);
        } catch (Exception e) {
            System.out.println("⚠️ swipe(" + direction + "): " + e.getMessage());
        }
    }

    private void hideKeyboardSafe() {
        try {
            driver.executeScript("mobile: hideKeyboard");
        } catch (Exception e) {
            // Fallback: tap above the table (info banner zone) to resign focus.
            try {
                driver.executeScript("mobile: tap", Map.of("x", 220, "y", 200));
            } catch (Exception ignored) { }
        }
    }

    private void pauseMs(long ms) {
        com.egalvanic.utils.Waits.until(() -> false, ms);
    }
}
