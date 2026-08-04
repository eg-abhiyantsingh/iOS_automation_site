package com.egalvanic.verify;

import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Driver-free proof that the polling {@code assertNotBlank} is USEFUL: it must stay
 * RED on a census that never populates, go GREEN the instant content appears
 * mid-window, and treat a spinner-then-content sequence as loading rather than blank
 * (the false RED that hit TC_SEC_009 mid-round-trip). Census/loading probes are
 * injected so this runs on a plain JVM (no Appium / simulator) — companion to
 * {@link VerifierSelfTest}.
 *
 * <p>Self-test windows are tightened (600ms / 50ms) so RED cases don't pay the
 * production 10s window per test; the polling logic under test is window-agnostic.
 */
public class NotBlankPollingSelfTest {

    private static final long WINDOW_MS = 600;
    private static final long INTERVAL_MS = 50;

    @Test
    public void happyPath_firstProbeSucceeds_singleCensusNoPolling() {
        AtomicInteger probes = new AtomicInteger();
        UIStateValidator.assertNotBlank("happy screen",
                () -> { probes.incrementAndGet(); return 9; },
                () -> false, WINDOW_MS, INTERVAL_MS); // must NOT throw
        assertEquals(probes.get(), 1,
                "first probe succeeding must return immediately — this runs suite-wide");
    }

    @Test
    public void censusStaysEmpty_FAILS_afterFullWindow() {
        assertThrows(VerificationError.class, () ->
                UIStateValidator.assertNotBlank("stays blank",
                        () -> 0, () -> false, WINDOW_MS, INTERVAL_MS));
    }

    @Test
    public void contentOnThirdPoll_passes_withoutWaitingOutTheWindow() {
        AtomicInteger probes = new AtomicInteger();
        long start = System.currentTimeMillis();
        UIStateValidator.assertNotBlank("slow render",
                () -> probes.incrementAndGet() >= 3 ? 5 : 0,
                () -> false, 10_000, INTERVAL_MS); // must NOT throw
        long elapsed = System.currentTimeMillis() - start;
        assertEquals(probes.get(), 3, "must return the moment the census populates");
        assertTrue(elapsed < 5_000,
                "must not wait out the window once content appeared (took " + elapsed + "ms)");
    }

    @Test
    public void spinnerThenContent_passes_loadingIsNotBlank() {
        AtomicInteger probes = new AtomicInteger();
        UIStateValidator.assertNotBlank("loading screen",
                () -> probes.incrementAndGet() >= 5 ? 4 : 0,
                () -> probes.get() < 5, // ActivityIndicator visible until content lands
                10_000, INTERVAL_MS);   // must NOT throw
        assertEquals(probes.get(), 5,
                "spinner phase must be polled through, then pass on rendered content");
    }

    @Test
    public void emptyCensusButScreenshotRendered_passes_sheetVisibleQuirk() {
        // v1.51 presented sheets: whole subtree reports visible==0 while the
        // screen renders full content — the pixel fallback must rescue it.
        UIStateValidator.assertNotBlank("presented sheet",
                () -> 0, () -> false, () -> true, WINDOW_MS, INTERVAL_MS); // must NOT throw
    }

    @Test
    public void emptyCensusAndBlankScreenshot_stillFAILS() {
        // The fallback must not weaken the verifier: DOM empty + pixels blank = RED.
        assertThrows(VerificationError.class, () ->
                UIStateValidator.assertNotBlank("truly blank",
                        () -> 0, () -> false, () -> false, WINDOW_MS, INTERVAL_MS));
    }

    @Test
    public void renderedFallback_isLastResort_notConsultedWhenCensusPopulates() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        UIStateValidator.assertNotBlank("healthy census",
                () -> 7, () -> false,
                () -> { fallbackCalls.incrementAndGet(); return true; },
                WINDOW_MS, INTERVAL_MS);
        assertEquals(fallbackCalls.get(), 0,
                "screenshot fallback must not run when the DOM census already proves content");
    }

    @Test
    public void censusWedges_butScreenshotRendered_passes() {
        // WDA snapshot timeout on giant/sheet-stacked DOMs: census THROWS.
        // The pixel fallback must still rescue a visibly rendered screen.
        UIStateValidator.assertNotBlank("wedged census, rendered screen",
                () -> { throw new VerificationError("Content census failed (TimeoutException)"); },
                () -> false, () -> true, WINDOW_MS, INTERVAL_MS); // must NOT throw
    }

    @Test
    public void censusWedges_andScreenshotBlank_FAILS_withTheWedgeVerdict() {
        VerificationError e = expectThrows(VerificationError.class, () ->
                UIStateValidator.assertNotBlank("wedged census, blank screen",
                        () -> { throw new VerificationError("Content census failed (TimeoutException)"); },
                        () -> false, () -> false, WINDOW_MS, INTERVAL_MS));
        assertTrue(e.getMessage().contains("census failed"),
                "the census-wedge verdict must survive when pixels can't rescue: " + e.getMessage());
    }

    @Test
    public void spinnerNeverResolvesIntoContent_FAILS_andVerdictSaysSo() {
        VerificationError e = expectThrows(VerificationError.class, () ->
                UIStateValidator.assertNotBlank("stuck spinner",
                        () -> 0, () -> true, WINDOW_MS, INTERVAL_MS));
        assertTrue(e.getMessage().contains("loading indicator"),
                "verdict should record the unresolved loading indicator: " + e.getMessage());
    }
}
