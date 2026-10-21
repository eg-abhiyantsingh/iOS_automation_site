package com.egalvanic.pages;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ZP-3914 (iOS 1.59) "Photo Viewer" · "Zoom tools" · "Markup tools". Captured live on the 1.69 QA build
 * (Assets › Generator-1, probe 2026-10-21); source Core/UI/Markup/*, Views/Supporting/FullImageView.swift (1.70).
 *
 *  Asset Details › category bar 'Photos' › 'Asset Photos' card: category Buttons 'Profile' / 'Nameplate' /
 *  'Panel Schedule' (label gains ', (N)'), 'Gallery', 'Camera' (disabled on a simulator). Gallery → iOS PHPicker
 *  ('Private Access to Photos' banner, 3-up grid) → 'Review Photo' (Cancel · Mark Up · Use Photo; NO 'Retake' for
 *  library picks). 'Mark Up' → editor nav 'Mark Up Photo' (Cancel · Done); 'Undo' / 'Clear All' (disabled until a
 *  mark exists); colours Red (default, value="1") · Orange · Yellow · Green · Cyan · Magenta · White; slider
 *  'Thickness' (or 'Arrowhead' while an arrow is selected); tools Select (default) · Text · Arrow · Callout · Box ·
 *  Draw. A placed mark is not in the accessibility tree — it shows as Undo/Clear All enabling plus 'Label' + 'Delete'
 *  buttons. Text tool + tap → label bar: TextField 'Add a label...', counter 'n/200', 'Text marks need a label.',
 *  'Cancel' / 'Done' (Done disabled while empty); typing is capped at 200. Cancel with marks → Sheet
 *  'Discard markup?' / 'Your marks will be lost. The photo itself is not affected.' (Discard · Keep Editing) —
 *  an XCUIElementTypeSheet, so run under {@link #withAlertsManual}. Editor 'Done' composes the marks into the image
 *  and stages the photo (asset nav now shows 'Cancel'; leaving asks 'Discard Changes?').
 *  Viewer (tap a thumbnail): 'Mark Up' + 'Close' over a zoomable scroll view; pinch grows the image frame
 *  (440×331 → 1174×881 at scale 3, probe). Double-tap zoom (2.5×) landed 2026-10-07 — after the 1.69 build.
 */
public class PhotoMarkupPage extends SnapPage {

    public static final String REVIEW_TITLE = "Review Photo";
    public static final String USE_PHOTO = "Use Photo";
    public static final String MARK_UP = "Mark Up";
    public static final String RETAKE = "Retake";
    public static final String EDITOR_TITLE = "Mark Up Photo";
    public static final List<String> TOOLS = List.of("Select", "Text", "Arrow", "Callout", "Box", "Draw");
    public static final List<String> COLOURS = List.of("Red", "Orange", "Yellow", "Green", "Cyan", "Magenta", "White");
    public static final String LABEL_PLACEHOLDER = "Add a label...";
    public static final String TEXT_NEEDS_LABEL = "Text marks need a label.";
    public static final String DISCARD_TITLE = "Discard markup?";
    public static final String DISCARD_MESSAGE = "Your marks will be lost. The photo itself is not affected.";
    public static final int LABEL_MAX = 200;

    // ── asset photos ───────────────────────────────────────────────────────────────────────────────────────

    /** On Asset Details: category-bar 'Photos' jump; true once the 'Asset Photos' card and 'Gallery' are visible. */
    public boolean jumpToAssetPhotos() {
        Optional<SnapNode> chip = find(n -> "Button".equals(n.type) && n.is("Photos") && n.y < 160);
        chip.ifPresent(this::tapNode);
        return waitUntil(() -> labelled("Button", "Gallery").isPresent() && labelled("StaticText", "Asset Photos").isPresent(), 8_000);
    }

    public boolean isCameraEnabled() {
        return labelled("Button", "Camera").map(n -> n.enabled).orElse(false);
    }

    /** Count shown on the selected category chip ('Profile, (N)' → N; plain 'Profile' → 0). */
    public int categoryCount(String category) {
        Optional<SnapNode> c = find(n -> "Button".equals(n.type) && (n.is(category) || n.label.startsWith(category + ", (")));
        if (c.isEmpty()) return -1;
        String l = c.get().label;
        return l.startsWith(category + ", (") ? Integer.parseInt(l.replaceAll("\\D+", "")) : 0;
    }

    /** Gallery → PHPicker → first photo; true once 'Review Photo' is up. */
    public boolean pickFirstLibraryPhoto() {
        if (!pressButton("Gallery")) return false;
        if (!waitUntil(() -> find(n -> n.name.equals("photos_sectioned_layout") || n.label.startsWith("Private Access to Photos")).isPresent(), 12_000)) {
            return false;
        }
        sleep(1200);
        Optional<SnapNode> grid = find(n -> "photos_sectioned_layout".equals(n.name));
        int x = 72, y = grid.map(g -> g.y + 70).orElse(406);
        tapPoint(x, y);
        return waitUntil(this::isReviewShown, 12_000);
    }

    public boolean isReviewShown() {
        return labelled("StaticText", REVIEW_TITLE).isPresent();
    }

    public List<String> reviewButtons() {
        return findAll(n -> "Button".equals(n.type) && (n.is("Cancel") || n.is(MARK_UP) || n.is(USE_PHOTO) || n.is(RETAKE)))
                .stream().map(n -> n.label).toList();
    }

    public boolean openMarkupFromReview() {
        if (!pressButton(MARK_UP)) return false;
        return waitUntil(this::isEditorShown, 8_000);
    }

    // ── markup editor ──────────────────────────────────────────────────────────────────────────────────────

    public boolean isEditorShown() {
        return find(n -> "NavigationBar".equals(n.type) && n.is(EDITOR_TITLE)).isPresent()
                || labelled("StaticText", EDITOR_TITLE).isPresent();
    }

    /** Press a visible Button by label (Undo, Clear All, Use Photo, Cancel…); true when one was there. */
    public boolean pressEditorButton(String label) {
        boolean ok = pressButton(label);
        if (ok) sleep(900);
        return ok;
    }

    public boolean isEnabled(String buttonLabel) {
        return labelled("Button", buttonLabel).map(n -> n.enabled).orElse(false);
    }

    public boolean isPresent(String buttonLabel) {
        return labelled("Button", buttonLabel).isPresent();
    }

    /** Which tool / colour is selected (value="1"); "" when none. */
    public String selectedOf(List<String> group) {
        return group.stream().filter(g -> labelled("Button", g).map(SnapNode::isOn).orElse(false)).findFirst().orElse("");
    }

    public List<String> presentOf(List<String> group) {
        return group.stream().filter(this::isPresent).toList();
    }

    public boolean chooseTool(String tool) {
        if (!pressButton(tool)) return false;
        return waitUntil(() -> tool.equals(selectedOf(TOOLS)), 3_000);
    }

    /** Frame of the photo canvas (largest image in the editor), or null. */
    public SnapNode canvas() {
        return findAll(n -> "Image".equals(n.type) && n.h > 200 && n.w > 300).stream().findFirst().orElse(null);
    }

    /** Drag across the canvas from 25% to 70% of its frame (Arrow / Box / Draw need a ≥1% drag). */
    public boolean dragOnCanvas() {
        SnapNode c = canvas();
        if (c == null) return false;
        int x1 = c.x + c.w / 4, y1 = c.y + c.h / 3, x2 = c.x + c.w * 7 / 10, y2 = c.y + c.h * 2 / 3;
        var finger = new org.openqa.selenium.interactions.PointerInput(org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var seq = new org.openqa.selenium.interactions.Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(java.time.Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x1, y1));
        seq.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(new org.openqa.selenium.interactions.Pause(finger, java.time.Duration.ofMillis(150)));
        seq.addAction(finger.createPointerMove(java.time.Duration.ofMillis(400), org.openqa.selenium.interactions.PointerInput.Origin.viewport(), (x1 + x2) / 2, (y1 + y2) / 2));
        seq.addAction(finger.createPointerMove(java.time.Duration.ofMillis(400), org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x2, y2));
        seq.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
        sleep(900);
        return true;
    }

    /** Single tap in the middle of the canvas (places Text / Callout marks). */
    public boolean tapCanvasCentre() {
        SnapNode c = canvas();
        if (c == null) return false;
        tapPoint(c.centerX(), c.centerY());
        sleep(900);
        return true;
    }

    public boolean isLabelBarShown() {
        return find(n -> "TextField".equals(n.type) && (LABEL_PLACEHOLDER.equals(n.value) || n.y > by(400) && n.y < by(620))).isPresent()
                && find(n -> "StaticText".equals(n.type) && n.label.matches("\\d+/200")).isPresent();
    }

    /** Label bar counter 'n/200' → n; -1 when absent. */
    public int labelCounter() {
        return find(n -> "StaticText".equals(n.type) && n.label.matches("\\d+/200"))
                .map(n -> Integer.parseInt(n.label.substring(0, n.label.indexOf('/')))).orElse(-1);
    }

    public String labelFieldValue() {
        return find(n -> "TextField".equals(n.type) && n.y > by(400) && n.y < by(620)).map(n -> n.value).orElse(null);
    }

    /** The label bar's own Done (the lower one, above the keyboard / tool bar), enabled state. */
    public boolean isLabelDoneEnabled() {
        return labelBarButton("Done").map(n -> n.enabled).orElse(false);
    }

    /**
     * The label bar's own button: the lowest one ABOVE the keyboard. The keyboard's return key is also a Button
     * named 'Done' (label 'done') and is always enabled — TC_PM_04 read it on 2026-10-21 while the bar's Done was
     * correctly grey.
     */
    private Optional<SnapNode> labelBarButton(String label) {
        java.util.List<SnapNode> s = snapshot();
        int kbTop = s.stream().filter(n -> n.visible && "Keyboard".equals(n.type)).mapToInt(n -> n.y).min().orElse(screenH());
        return s.stream().filter(n -> n.visible && "Button".equals(n.type) && n.is(label) && n.y > 300 && n.y < kbTop)
                .max(java.util.Comparator.comparingInt(n -> n.y));
    }

    public boolean pressLabelDone() {
        Optional<SnapNode> d = labelBarButton("Done");
        if (d.isEmpty() || !d.get().enabled) return false;
        tapNode(d.get());
        return waitUntil(() -> !isLabelBarShown(), 5_000);
    }

    /** One keystroke into the focused label field (no batching — human pace). */
    public void typeKey(String key) {
        typeKeys(key);
        sleep(250);
    }

    public boolean typeLabel(String text) {
        typeKeys(text);
        sleep(800);
        return true;
    }

    /** Editor top 'Cancel' with marks → 'Discard markup?' sheet; presses {@code answer}; returns the sheet text. */
    public String cancelEditor(String answer) {
        final String[] text = {""};
        withAlertsManual(() -> {
            Optional<SnapNode> cancel = find(n -> "Button".equals(n.type) && n.is("Cancel") && n.y < 120);
            if (cancel.isEmpty()) return;
            tapNode(cancel.get());
            if (!waitUntil(() -> find(n -> ("Sheet".equals(n.type) || "Alert".equals(n.type))).isPresent(), 4_000)) return;
            SnapNode sheet = find(n -> "Sheet".equals(n.type) || "Alert".equals(n.type)).get();
            StringBuilder b = new StringBuilder();
            for (SnapNode t : findAll(n -> "StaticText".equals(n.type) && n.y >= sheet.y && n.y <= sheet.y + sheet.h)) {
                if (b.length() > 0) b.append(" | ");
                b.append(t.label);
            }
            text[0] = b.toString();
            find(n -> "Button".equals(n.type) && n.is(answer) && n.y >= sheet.y).ifPresent(this::tapNode);
            sleep(1200);
        });
        return text[0];
    }

    /** Editor top 'Done' (composes the marks); true once the editor is gone. */
    public boolean doneEditor() {
        Optional<SnapNode> done = find(n -> "Button".equals(n.type) && n.is("Done") && n.y < 120);
        if (done.isEmpty()) return false;
        tapNode(done.get());
        return waitUntil(() -> !isEditorShown(), 12_000);
    }

    // ── viewer / zoom ──────────────────────────────────────────────────────────────────────────────────────

    /** Tap the first photo thumbnail under 'Asset Photos'; true once the viewer ('Mark Up' + 'Close') is up. */
    public boolean openFirstThumbnail() {
        Optional<SnapNode> chips = find(n -> "Button".equals(n.type) && (n.is("Profile") || n.label.startsWith("Profile, (")));
        int y = chips.map(c -> c.y + 115).orElse(320);
        tapPoint(85, y);
        return waitUntil(this::isViewerShown, 8_000);
    }

    public boolean isViewerShown() {
        return find(n -> "Button".equals(n.type) && n.is(MARK_UP) && n.y < 140).isPresent()
                && find(n -> "Button".equals(n.type) && n.is("Close") && n.y < 140).isPresent();
    }

    /** Width of the photo in the viewer (grows when zoomed); -1 when not found. */
    public int viewerImageWidth() {
        return findAll(n -> "Image".equals(n.type) && n.h > 200).stream().mapToInt(n -> n.w).max().orElse(-1);
    }

    /** Pinch the viewer's zoom scroll view by {@code scale}. */
    public boolean pinchViewer(double scale) {
        try {
            List<org.openqa.selenium.WebElement> svs = driver.findElements(io.appium.java_client.AppiumBy.className("XCUIElementTypeScrollView"));
            for (org.openqa.selenium.WebElement e : svs) {
                org.openqa.selenium.Rectangle r = e.getRect();
                if (r.height > 600 && r.height < 900 && "true".equals(e.getAttribute("visible"))) {
                    driver.executeScript("mobile: pinch", Map.of("elementId", ((org.openqa.selenium.remote.RemoteWebElement) e).getId(),
                            "scale", scale, "velocity", scale >= 1 ? 2.0 : -2.0));
                    sleep(1200);
                    return true;
                }
            }
        } catch (Exception ex) {
            System.out.println("⚠️ pinchViewer: " + ex.getMessage());
        }
        return false;
    }

    /** Double-tap the centre of the viewer. */
    public void doubleTapViewer() {
        try {
            driver.executeScript("mobile: doubleTap", Map.of("x", screenW() / 2, "y", by(520)));
        } catch (Exception e) {
            System.out.println("⚠️ doubleTapViewer: " + e.getMessage());
        }
        sleep(1200);
    }

    public boolean closeViewer() {
        Optional<SnapNode> close = find(n -> "Button".equals(n.type) && n.is("Close") && n.y < 140);
        close.ifPresent(this::tapNode);
        return waitUntil(() -> !isViewerShown(), 5_000);
    }

    /**
     * Leave Asset Details discarding the staged photo: nav 'Cancel' → Alert 'Discard Changes?' ('You have unsaved
     * changes. Are you sure you want to discard them?') → 'Discard'. Returns the alert title ("" when none).
     */
    public String discardAssetChanges() {
        final String[] title = {""};
        withAlertsManual(() -> {
            Optional<SnapNode> cancel = find(n -> "Button".equals(n.type) && n.is("Cancel") && n.y < 120);
            if (cancel.isEmpty()) return;
            tapNode(cancel.get());
            if (waitUntil(() -> find(n -> "Alert".equals(n.type)).isPresent(), 4_000)) {
                title[0] = find(n -> "Alert".equals(n.type)).map(n -> n.label).orElse("");
                pressButton("Discard");
                sleep(1500);
            }
        });
        return title[0];
    }
}
