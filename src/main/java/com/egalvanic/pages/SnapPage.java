package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import com.egalvanic.utils.Waits;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Shared plumbing for the snapshot-driven page objects written for ZP-3914 (iOS 1.59 features, probed live on the
 * 1.69 QA build 2026-10-21). Every read is ONE page-source snapshot ({@link #snapshot()}); every press is a
 * coordinate tap on the node's centre — element.click() is a silent no-op on SwiftUI buttons since 1.67, and a
 * per-element WDA query costs seconds on the heavy screens these features live on.
 */
abstract class SnapPage extends BasePage {

    // ── screen-size adaptation ─────────────────────────────────────────────────────────────────────────────
    // Thresholds were measured on the local iPhone 17 Pro Max (440×956 pt); CI runs an iPhone 16 Pro (402×874).
    // Right-anchored x and bottom-anchored y shift by the size difference — rx()/by() translate them.

    private int[] screen;

    private int[] screen() {
        if (screen == null) {
            try {
                org.openqa.selenium.Dimension d = driver.manage().window().getSize();
                screen = new int[]{d.getWidth(), d.getHeight()};
            } catch (Exception e) {
                screen = new int[]{440, 956};
            }
        }
        return screen;
    }

    protected int screenW() { return screen()[0]; }
    protected int screenH() { return screen()[1]; }

    /** A right-anchored x measured on the 440-pt-wide probe device, on this screen. */
    protected int rx(int x) { return x - (440 - screenW()); }

    /** A bottom-anchored y measured on the 956-pt-high probe device, on this screen. */
    protected int by(int y) { return y - (956 - screenH()); }

    /** First visible node matching {@code p}, top to bottom. */
    protected Optional<SnapNode> find(Predicate<SnapNode> p) {
        return snapshot().stream().filter(n -> n.visible && p.test(n))
                .min(java.util.Comparator.comparingInt(n -> n.y));
    }

    protected List<SnapNode> findAll(Predicate<SnapNode> p) {
        return snapshot().stream().filter(n -> n.visible && p.test(n))
                .sorted(java.util.Comparator.comparingInt((SnapNode n) -> n.y).thenComparingInt(n -> n.x)).toList();
    }

    /** Visible node whose label or name equals {@code text}, optionally restricted to an element type. */
    protected Optional<SnapNode> labelled(String type, String text) {
        return find(n -> (type == null || type.equals(n.type)) && n.is(text));
    }

    /**
     * The node once its position has SETTLED (two snapshots ≥600 ms apart agree within 2 pt). A tap right after a
     * swipe lands wherever the momentum carried the list (repo rule since 2026-10-05; TC_EXP_05 pressed under a
     * still-moving Settings list, 2026-10-21). Empty when the node is gone or never settles.
     */
    protected Optional<SnapNode> settled(Predicate<SnapNode> p) {
        Optional<SnapNode> prev = find(p);
        for (int i = 0; i < 6 && prev.isPresent(); i++) {
            sleep(600);
            Optional<SnapNode> now = find(p);
            if (now.isEmpty()) return now;
            if (Math.abs(now.get().y - prev.get().y) <= 2) return now;
            prev = now;
        }
        return Optional.empty();
    }

    /** Press a Button by label once it has stopped moving. */
    protected boolean pressSettledButton(String label) {
        Optional<SnapNode> b = settled(n -> "Button".equals(n.type) && n.is(label));
        b.ifPresent(this::tapNode);
        return b.isPresent();
    }

    public boolean isTextVisible(String text) {
        return find(n -> n.is(text)).isPresent();
    }

    protected void tapNode(SnapNode n) {
        tapPoint(n.centerX(), n.centerY());
    }

    protected void tapPoint(int x, int y) {
        driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
    }

    /** Press the first visible Button labelled {@code label}; false when none is on screen. */
    protected boolean pressButton(String label) {
        Optional<SnapNode> b = labelled("Button", label);
        b.ifPresent(this::tapNode);
        return b.isPresent();
    }

    /** Key actions into whatever field holds focus — the way a user types (element sendKeys 404s in sheets). */
    protected void typeKeys(String text) {
        new org.openqa.selenium.interactions.Actions(driver).sendKeys(text).perform();
    }

    /** Clear the focused field ({@code n} backspaces) and type {@code text}. */
    protected void replaceFocusedText(String text, int n) {
        StringBuilder keys = new StringBuilder();
        for (int i = 0; i < n; i++) keys.append(org.openqa.selenium.Keys.BACK_SPACE);
        keys.append(text);
        typeKeys(keys.toString());
    }

    /** 'up' moves the content up (reveals what is below). */
    protected void swipeScreen(String direction) {
        try {
            driver.executeScript("mobile: swipe", Map.of("direction", direction));
        } catch (Exception e) {
            System.out.println("⚠️ swipe(" + direction + "): " + e.getMessage());
        }
    }

    protected boolean isKeyboardUp() {
        return snapshot().stream().anyMatch(n -> "Keyboard".equals(n.type) && n.visible);
    }

    /**
     * Dismiss the software keyboard. 1.69 SwiftUI fields have no 'Done' accessory and mobile: hideKeyboard reports
     * "Did not know how to dismiss the keyboard" (probe 2026-10-21) — the keyboard's own return key does it
     * ('next' moves to the next field first, so press up to three times).
     */
    protected boolean dismissKeyboard() {
        for (int i = 0; i < 3 && isKeyboardUp(); i++) {
            Optional<SnapNode> ret = find(n -> ("Button".equals(n.type) || "Key".equals(n.type)) && n.y > by(600)
                    && (n.is("Done") || n.is("done") || n.is("return") || n.is("Return") || n.is("next")
                    || n.is("Next") || n.is("search") || n.is("Search") || n.is("Go")));
            if (ret.isEmpty()) {
                try { driver.executeScript("mobile: hideKeyboard"); } catch (Exception ignored) { }
                break;
            }
            tapNode(ret.get());
            sleep(700);
        }
        return !isKeyboardUp();
    }

    /**
     * Bring our app back to the front for WDA. On CI (2026-10-21, TC_SWW_01) every tap right after a soft restart
     * failed with "Invalid parameter not satisfying: point.x != INFINITY" — WDA's active-application lookup had lost
     * the app, although its tab bar was on screen. activateApp re-anchors it; harmless when it is already active.
     */
    protected void reactivateApp() {
        try {
            driver.executeScript("mobile: activateApp", Map.of("bundleId", com.egalvanic.constants.AppConstants.APP_BUNDLE_ID));
        } catch (Exception e) {
            System.out.println("⚠️ reactivateApp: " + e.getMessage());
        }
        sleep(1500);
    }

    protected boolean waitUntil(java.util.function.BooleanSupplier c, long timeoutMs) {
        return Waits.until(c, timeoutMs, 500);
    }

    /** Labels of the bottom tab bar's buttons, left to right (empty when no tab bar is visible). */
    public List<String> tabBarLabels() {
        List<SnapNode> s = snapshot();
        Optional<SnapNode> bar = s.stream().filter(n -> n.visible && "TabBar".equals(n.type)).findFirst();
        if (bar.isEmpty()) return List.of();
        int top = bar.get().y;
        return s.stream().filter(n -> n.visible && "Button".equals(n.type) && n.y >= top - 2)
                .sorted(java.util.Comparator.comparingInt(n -> n.x)).map(n -> n.label).toList();
    }

    /** Press a bottom tab-bar button by label. */
    public boolean openTab(String label) {
        List<SnapNode> s = snapshot();
        Optional<SnapNode> bar = s.stream().filter(n -> n.visible && "TabBar".equals(n.type)).findFirst();
        if (bar.isEmpty()) return false;
        int top = bar.get().y;
        Optional<SnapNode> tab = s.stream().filter(n -> n.visible && "Button".equals(n.type) && n.y >= top - 2 && n.is(label))
                .findFirst();
        tab.ifPresent(this::tapNode);
        return tab.isPresent();
    }
}
