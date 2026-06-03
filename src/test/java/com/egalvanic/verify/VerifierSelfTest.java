package com.egalvanic.verify;

import org.testng.annotations.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

/**
 * Driver-free proof that the hardening is USEFUL: each verifier must go RED on a real
 * defect and GREEN when correct. Runs on a plain JVM (no Appium / simulator) — wired via
 * {@code testng-verify-selftest.xml}.
 *
 * <p>This is the local validation gate; the device-dependent verifiers (CrashDetector,
 * UIStateValidator) are validated on the macOS CI runners against the live app.
 */
public class VerifierSelfTest {

    private final StateIntegrityChecker sic = new StateIntegrityChecker();

    // ---- StateIntegrityChecker: catches the "data corruption across flows" bug class ----

    @Test
    public void createExactlyOne_passes_whenItemTrulyAdded() {
        var before = sic.capture(() -> List.of("A", "B"));
        var after  = sic.capture(() -> List.of("A", "B", "C"));
        sic.assertCreatedExactlyOne(before, after, "C"); // must NOT throw
    }

    @Test
    public void createExactlyOne_FAILS_whenNothingWasCreated() {
        var before = sic.capture(() -> List.of("A", "B"));
        var after  = sic.capture(() -> List.of("A", "B")); // create silently did nothing
        assertThrows(VerificationError.class, () -> sic.assertCreatedExactlyOne(before, after, "C"));
    }

    @Test
    public void createExactlyOne_FAILS_whenWrongItemAppeared() {
        var before = sic.capture(() -> List.of("A", "B"));
        var after  = sic.capture(() -> List.of("A", "B", "WRONG"));
        assertThrows(VerificationError.class, () -> sic.assertCreatedExactlyOne(before, after, "C"));
    }

    @Test
    public void noLossOrDup_FAILS_onDataLoss() {
        var before = sic.capture(() -> List.of("A", "B", "C"));
        var after  = sic.capture(() -> List.of("A", "C")); // B vanished after the flow
        assertThrows(VerificationError.class, () -> sic.assertNoLossOrDup(before, after));
    }

    @Test
    public void noLossOrDup_FAILS_onDuplication() {
        var before = sic.capture(() -> List.of("A", "B"));
        var after  = sic.capture(() -> List.of("A", "B", "B")); // duplicate created
        assertThrows(VerificationError.class, () -> sic.assertNoLossOrDup(before, after));
    }

    @Test
    public void capture_FAILS_whenReaderReturnsNull() {
        assertThrows(VerificationError.class, () -> sic.capture(() -> null)); // swallowed-failure guard
    }

    // ---- ImageAnalysis: catches the "PDF/asset failed to render" bug class ----

    @Test
    public void blankRender_isDetectedAsBlank() {
        BufferedImage blank = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        var g = blank.getGraphics();
        g.setColor(new Color(0xEE, 0xEE, 0xEE)); // uniform grey "failed to load" tile
        g.fillRect(0, 0, 300, 300);
        g.dispose();
        assertTrue(ImageAnalysis.analyzeFull(blank).looksBlank(), "uniform tile must read as blank");
    }

    @Test
    public void realRender_isNotBlank() {
        BufferedImage real = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        Random r = new Random(7);
        for (int x = 0; x < 300; x++)
            for (int y = 0; y < 300; y++)
                real.setRGB(x, y, r.nextInt(0xFFFFFF)); // varied content == a rendered page
        assertFalse(ImageAnalysis.analyzeFull(real).looksBlank(), "varied content must NOT read as blank");
    }
}
