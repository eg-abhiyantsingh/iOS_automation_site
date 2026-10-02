package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import com.egalvanic.utils.Waits;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The work-order session's Issues tab (ZP-3927 §1 "Unlink Issue" lives here, not on the Issues screen).
 *
 * Probe 1.63 (2026-09-21, four discovery runs):
 *   empty   — «No Issues» · «Get started by linking existing issues or creating new ones» · «Manage Issues» · «Add»
 *   linked  — stats «Total / Open / Closed», «Manage Issues», header «Issues» + «Sort», group label «Open»,
 *             rows = StaticText clusters «title» «status» «description» «class» «asset» (no row handle),
 *             tab button label becomes «1, Issues»
 *   Manage Issues → picker nav «Link Issues» · «Cancel» · «Update» · «Search issues» ·
 *             «Select issues to link to this session» · rows = Buttons «title, status, asset[, Linked to: WO], date»
 *             (value 1 when selected) · footer «⚠️ Issues can only be linked to one session at a time»
 *   long-press a linked row → context menu Button «Unlink Issue» (link.badge.plus)
 *   tap it → Alert «Unlink Issue?» · «Remove “<title>” from this work order?» · «Cancel» · «Unlink Issue»
 *   confirm → row gone, back to the empty state when it was the only one.
 */
public class SessionIssuesPage extends BasePage {

    public static final String EMPTY_TITLE        = "No Issues";
    public static final String EMPTY_HINT         = "Get started by linking existing issues or creating new ones";
    public static final String MANAGE_ISSUES      = "Manage Issues";
    public static final String PICKER_TITLE       = "Link Issues";
    public static final String PICKER_HINT        = "Select issues to link to this session";
    public static final String PICKER_UPDATE      = "Update";
    public static final String PICKER_ONE_SESSION = "Issues can only be linked to one session at a time";
    public static final String PICKER_LINKED_TO   = "Linked to:";
    public static final String MENU_UNLINK        = "Unlink Issue";
    public static final String ALERT_TITLE        = "Unlink Issue?";
    public static final String ALERT_BODY_PREFIX  = "Remove “";
    public static final String ALERT_BODY_SUFFIX  = "” from this work order?";
    public static final String ALERT_CONFIRM      = "Unlink Issue";
    public static final String ALERT_CANCEL       = "Cancel";

    // ── state ────────────────────────────────────────────────────────────────

    public boolean isEmptyState() { return isAnyTextPresent(EMPTY_TITLE) && isAnyTextPresent(MANAGE_ISSUES); }

    public boolean isOnSessionIssuesTab() { return isEmptyState() || (isAnyTextPresent(MANAGE_ISSUES) && isAnyTextPresent("Total")); }

    /** Titles of the linked issues listed on the tab (title StaticTexts below the header). */
    public List<String> linkedIssueTitles() {
        List<String> out = new ArrayList<>();
        try {
            withImplicitWait(0, () -> {
                for (WebElement t : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND visible == 1 AND label CONTAINS ' on '"))) {
                    try {
                        String l = t.getAttribute("label");
                        if (t.getLocation().getY() > 280 && !l.contains(" detected on ") && !out.contains(l)) out.add(l);
                    } catch (Exception ignored) { }
                }
                return null;
            });
        } catch (Exception ignored) { }
        return out;
    }

    public int linkedIssueCount() { return linkedIssueTitles().size(); }

    /** Count folded into the Issues tab button label («1, Issues»); 0 when the label is bare «Issues»; -1 if absent. */
    public int issuesTabBadge() {
        try {
            List<WebElement> l = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND (label == 'Issues' OR label ENDSWITH ', Issues')")));
            if (l.isEmpty()) return -1;
            String label = l.get(0).getAttribute("label");
            if ("Issues".equals(label)) return 0;
            return Integer.parseInt(label.substring(0, label.indexOf(',')).trim());
        } catch (Exception e) { return -1; }
    }

    // ── link picker ──────────────────────────────────────────────────────────

    public boolean openManageIssues() {
        if (isPickerOpen()) return true;
        if (!tapText(MANAGE_ISSUES)) return false;
        sleep(900);
        return isPickerOpen();
    }

    public boolean isPickerOpen() {
        return isAnyTextPresent(PICKER_TITLE) && (isAnyTextPresent(PICKER_UPDATE) || isAnyTextContaining(PICKER_HINT));
    }

    private List<WebElement> pickerRows() {
        try {
            return withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND label CONTAINS ' on ' AND label CONTAINS ', '")));
        } catch (Exception e) { return List.of(); }
    }

    /** Titles of picker rows that are NOT linked to any session (safe to link without moving another session's issue). */
    public List<String> unlinkedCandidateTitles() {
        List<String> out = new ArrayList<>();
        for (WebElement r : pickerRows()) {
            try {
                String l = r.getAttribute("label");
                if (l != null && !l.contains(PICKER_LINKED_TO)) out.add(l.substring(0, l.indexOf(',')).trim());
            } catch (Exception ignored) { }
        }
        return out;
    }

    /**
     * Select (toggle on) the first unlinked candidate; returns its title or null. The row's value
     * flips to "1" when selected — VERIFIED here, with a coordinate-press fallback, because
     * element.click() can be a silent no-op on v1.67 SwiftUI buttons (changelog 178).
     */
    public String selectFirstUnlinkedCandidate() {
        for (WebElement r : pickerRows()) {
            try {
                String l = r.getAttribute("label");
                if (l == null || l.contains(PICKER_LINKED_TO)) continue;
                String title = l.substring(0, l.indexOf(',')).trim();
                String before = r.getAttribute("value");
                if (!"1".equals(before)) {
                    r.click();
                    sleep(500);
                    if (!"1".equals(r.getAttribute("value"))) {
                        Rectangle b = r.getRect();
                        driver.executeScript("mobile: tap",
                                Map.of("x", b.x + b.width / 2, "y", b.y + b.height / 2));
                        sleep(500);
                    }
                }
                System.out.println("   picker row '" + title + "' value " + before + " → " + r.getAttribute("value"));
                return title;
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** Poll the session Issues tab until {@code title} is listed (the list refreshes after Update, not instantly). */
    public boolean waitForLinkedTitle(String title, long timeoutMs) {
        long start = System.currentTimeMillis();
        boolean ok = Waits.until(() -> linkedIssueTitles().contains(title), timeoutMs, 500);
        System.out.println("   linked row '" + title + "' " + (ok ? "listed after " : "NOT listed within ")
                + (System.currentTimeMillis() - start) + "ms");
        return ok;
    }

    /** Is the picker row for this title showing 'Linked to:'? (null when the row is not visible) */
    public Boolean isPickerRowLinked(String title) {
        for (WebElement r : pickerRows()) {
            try {
                String l = r.getAttribute("label");
                if (l != null && l.startsWith(title + ",")) return l.contains(PICKER_LINKED_TO);
            } catch (Exception ignored) { }
        }
        return null;
    }

    public boolean tapUpdate() { boolean ok = tapText(PICKER_UPDATE); sleep(1200); return ok; }
    public boolean cancelPicker() { boolean ok = tapText("Cancel"); sleep(600); return ok; }

    /** Make sure at least one issue is linked to this session; returns the linked title or null when impossible. */
    public String ensureOneLinkedIssue() {
        List<String> now = linkedIssueTitles();
        if (!now.isEmpty()) return now.get(0);
        if (!openManageIssues()) return null;
        String picked = selectFirstUnlinkedCandidate();
        if (picked == null) { cancelPicker(); return null; }
        tapUpdate();
        if (waitForLinkedTitle(picked, 6000)) return picked;
        return linkedIssueCount() > 0 ? linkedIssueTitles().get(0) : null;
    }

    // ── unlink ───────────────────────────────────────────────────────────────

    /** Long-press the row carrying this title (coordinate press — rows expose no handle). */
    public boolean longPressLinkedRow(String title) {
        try {
            List<WebElement> l = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == 1 AND label == '" + title.replace("'", "\\'") + "'")));
            WebElement t = null;
            for (WebElement e : l) { if (e.getLocation().getY() > 280) { t = e; break; } }
            if (t == null) return false;
            org.openqa.selenium.Rectangle r = t.getRect();
            return longPressAt(r.x + Math.min(r.width / 2, 120), r.y + r.height / 2, 1.5);
        } catch (Exception e) { return false; }
    }

    public boolean isUnlinkOffered() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == 1 AND label == '" + MENU_UNLINK + "'")) && !isUnlinkAlertShown();
    }

    public boolean tapUnlinkMenuItem() {
        try {
            List<WebElement> l = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND label == '" + MENU_UNLINK + "'")));
            if (l.isEmpty()) return false;
            l.get(0).click(); sleep(700); return true;
        } catch (Exception e) { return false; }
    }

    /** Close an open context menu without choosing (tap the dimmed backdrop well away from the menu). */
    public boolean dismissContextMenu() {
        try {
            org.openqa.selenium.Dimension d = driver.manage().window().getSize();
            driver.executeScript("mobile: tap", java.util.Map.of("x", d.getWidth() / 2, "y", 120));
            sleep(500);
            return true;
        } catch (Exception e) { return false; }
    }

    public boolean isUnlinkAlertShown() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeAlert' AND (name == '" + ALERT_TITLE + "' OR label == '" + ALERT_TITLE + "')"))
            || isAnyTextPresent(ALERT_TITLE);
    }

    /** The alert's body text, or null. */
    public String unlinkAlertBody() {
        try {
            List<WebElement> l = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == 1 AND label BEGINSWITH 'Remove '")));
            return l.isEmpty() ? null : l.get(0).getAttribute("label");
        } catch (Exception e) { return null; }
    }

    private boolean tapAlertButton(String label) {
        try {
            List<WebElement> l = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND label == '" + label + "'")));
            // Prefer the button INSIDE the alert (lowest on screen among same-labelled buttons).
            WebElement best = null;
            for (WebElement b : l) if (best == null || b.getLocation().getY() > best.getLocation().getY()) best = b;
            if (best == null) return false;
            best.click(); sleep(900); return true;
        } catch (Exception e) { return false; }
    }

    public boolean confirmUnlinkAlert() { return tapAlertButton(ALERT_CONFIRM); }
    /**
     * Run {@code body} with WDA's auto-accept paused, so the «Unlink Issue?» alert can be OBSERVED and answered
     * deliberately (the session runs autoAcceptAlerts=true for the post-Sign-In popups, which otherwise races
     * every alert poll — documented 2026-07-09). The setting is always restored afterwards.
     */
    public void withAlertsManual(Runnable body) {
        boolean paused = false;
        try { driver.setSetting("defaultAlertAction", ""); paused = true; }
        catch (Exception e) { System.out.println("⚠️ withAlertsManual: could not pause defaultAlertAction — " + e.getMessage()); }
        try { body.run(); }
        finally {
            if (paused) { try { driver.setSetting("defaultAlertAction", "accept"); } catch (Exception ignored) { } }
        }
    }

    /** Wait briefly for the «Unlink Issue?» alert to appear. */
    public boolean waitForUnlinkAlert(int maxSeconds) {
        for (int i = 0; i < maxSeconds * 2; i++) { if (isUnlinkAlertShown()) return true; sleep(500); }
        return false;
    }

    /** Long-press the «Manage Issues» control — a deliberately NON-row target for the negative case. */
    public boolean longPressManageIssues() {
        try {
            List<WebElement> l = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND label == '" + MANAGE_ISSUES + "'")));
            if (l.isEmpty()) return false;
            org.openqa.selenium.Rectangle r = l.get(0).getRect();
            return longPressAt(r.x + r.width / 2, r.y + r.height / 2, 1.2);
        } catch (Exception e) { return false; }
    }

    public boolean cancelUnlinkAlert()  { return tapAlertButton(ALERT_CANCEL); }

    /** Long-press → Unlink Issue → confirm. True when the row is gone afterwards. */
    public boolean unlinkIssue(String title) {
        if (!longPressLinkedRow(title)) return false;
        sleep(500);
        if (!tapUnlinkMenuItem()) { dismissContextMenu(); return false; }
        // autoAcceptAlerts may already have pressed the confirm before we look — check the outcome, not the dialog.
        if (isUnlinkAlertShown()) confirmUnlinkAlert();
        for (int i = 0; i < 10; i++) {
            if (!linkedIssueTitles().contains(title)) return true;
            sleep(400);
        }
        return false;
    }

    /** Unlink every linked issue (fixture reset). */
    public void unlinkAllLinked() {
        for (int guard = 0; guard < 6; guard++) {
            List<String> titles = linkedIssueTitles();
            if (titles.isEmpty()) return;
            if (!unlinkIssue(titles.get(0))) return;
        }
    }
}
