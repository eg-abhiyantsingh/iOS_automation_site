package com.egalvanic.verify;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Detects broken UI states the happy-path suite walks straight past: blank screens,
 * never-resolving spinners, and unexpected error alerts (which the driver currently
 * auto-accepts via {@code appium:autoAcceptAlerts=true}, hiding them entirely).
 */
public final class UIStateValidator {

    // Blank verdicts poll instead of one-shot: a screen mid-load (spinner up, content
    // not laid out yet) is "still loading", not blank — a one-shot census condemned
    // exactly that state (TC_SEC_009: ~61s login round trip read as blank). Only a
    // census that stays empty for the whole window is a real blank screen.
    static final long BLANK_POLL_WINDOW_MS = 10_000;
    static final long BLANK_POLL_INTERVAL_MS = 500;
    static final int MIN_CONTENT_ELEMENTS = 2;

    private IOSDriver driver() { return DriverManager.getDriver(); }

    /** A real screen must have meaningful content — guards white/blank screens after navigation. */
    public void assertNotBlank(String screen) {
        IOSDriver d = driver();
        try {
            // The 10s poll window IS the presence wait; per-probe implicit wait drops
            // to 0 so an empty census costs one snapshot (next probe in 500ms), not 5s.
            d.manage().timeouts().implicitlyWait(Duration.ZERO);
            assertNotBlank(screen, this::visibleContentCount, this::loadingIndicatorVisible,
                    this::screenshotLooksRendered, BLANK_POLL_WINDOW_MS, BLANK_POLL_INTERVAL_MS);
        } finally {
            try {
                d.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Driver-free core — census and loading probes are injectable so the RED/GREEN
     * behavior is provable on a plain JVM (see NotBlankPollingSelfTest). Suite-wide
     * cost stays one census on the happy path: the first probe succeeding returns
     * immediately, polling only kicks in while the screen is empty.
     */
    static void assertNotBlank(String screen, IntSupplier census, BooleanSupplier loadingVisible,
                               long windowMs, long intervalMs) {
        assertNotBlank(screen, census, loadingVisible, () -> false, windowMs, intervalMs);
    }

    static void assertNotBlank(String screen, IntSupplier census, BooleanSupplier loadingVisible,
                               BooleanSupplier renderedFallback, long windowMs, long intervalMs) {
        long deadline = System.currentTimeMillis() + windowMs;
        boolean sawLoading = false;
        int content = 0;
        VerificationError censusFailure = null;
        while (true) {
            try {
                content = census.getAsInt();
                censusFailure = null;
            } catch (VerificationError wedge) {
                // Census itself died (WDA snapshot timeout on a giant/sheet-
                // stacked DOM — observed live 2026-08-04 with the Work Type
                // picker open). Don't fail here: the pixel fallback below can
                // still prove the screen is rendered.
                censusFailure = wedge;
                content = 0;
            }
            if (content >= MIN_CONTENT_ELEMENTS) return;
            // spinner/progress visible == still loading, keep waiting out the window
            if (censusFailure == null) {
                sawLoading |= loadingVisible.getAsBoolean();
            }
            if (System.currentTimeMillis() >= deadline) break;
            pause(intervalMs);
        }
        // Pixel-level second opinion before failing: presented sheets can hide
        // the whole subtree from the accessibility census (visible==0 quirk),
        // and a wedged census can't see ANYTHING while pixels render fine.
        if (renderedFallback.getAsBoolean()) {
            System.out.println("🖼️ assertNotBlank(" + screen + "): DOM census "
                    + (censusFailure != null ? "WEDGED (" + censusFailure.getMessage() + ")"
                                             : "saw only " + content + " elements")
                    + " but the screenshot is visibly rendered — treating as non-blank");
            return;
        }
        if (censusFailure != null) throw censusFailure;
        throw new VerificationError("Blank/empty screen: " + screen
                + " (only " + content + " visible content elements after " + (windowMs / 1000) + "s"
                + (sawLoading ? "; a loading indicator was visible but never resolved into content" : "")
                + ")");
    }

    /**
     * Pixel-level second opinion for the DOM census. SwiftUI presented sheets
     * (v1.51 New Work Order form, observed live 2026-08-04) can report EVERY
     * descendant visible==0 while the screen renders full content — a 0-element
     * census with a visibly rich screenshot. A blank screen has near-uniform
     * pixels; a rendered form does not (ImageAnalysis thresholds are the same
     * ones AssetLoadVerifier trusts).
     */
    private boolean screenshotLooksRendered() {
        try {
            byte[] png = driver().getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(png));
            return img != null && !ImageAnalysis.analyzeFull(img).looksBlank();
        } catch (Exception e) {
            return false; // no screenshot -> no second opinion, keep the DOM verdict
        }
    }

    private int visibleContentCount() {
        try {
            return driver().findElements(AppiumBy.iOSNsPredicateString(
                    "visible == 1 AND (type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                  + "OR type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeImage' "
                  + "OR type == 'XCUIElementTypeTextField')")).size();
        } catch (Exception censusFailure) {
            // Giant-DOM screens (e.g. 100+ row Work Orders lists) can wedge the
            // full visible==1 census in WDA (proxy TimeoutException — observed
            // live 2026-07-21 on TC_WT_LIST_308). Fall back to bounded
            // first-match probes: MIN_CONTENT_ELEMENTS hits prove non-blank
            // cheaply; a fully wedged WDA still fails loudly below.
            int cheap = 0;
            for (String probe : new String[]{
                    "type == 'XCUIElementTypeNavigationBar'",
                    "type == 'XCUIElementTypeButton'",
                    "type == 'XCUIElementTypeStaticText'"}) {
                try {
                    driver().findElement(AppiumBy.iOSNsPredicateString(probe));
                    if (++cheap >= MIN_CONTENT_ELEMENTS) return cheap;
                } catch (Exception ignored) { }
            }
            if (cheap > 0) return cheap;
            throw new VerificationError("Content census failed ("
                    + censusFailure.getClass().getSimpleName()
                    + ") and bounded fallback probes found nothing — WDA may be wedged",
                    censusFailure);
        }
    }

    /** Spinner or determinate progress bar — the screen is loading, not blank. */
    private boolean loadingIndicatorVisible() {
        return !driver().findElements(AppiumBy.iOSNsPredicateString(
                "visible == 1 AND (type == 'XCUIElementTypeActivityIndicator' "
              + "OR type == 'XCUIElementTypeProgressIndicator')")).isEmpty();
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
