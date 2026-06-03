package com.egalvanic.explore;

import com.egalvanic.verify.AssetLoadVerifier;
import com.egalvanic.verify.CrashDetector;
import com.egalvanic.verify.UIStateValidator;
import com.egalvanic.verify.VerificationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the Phase-2 verifiers after every action and COLLECTS findings (rather than
 * throwing on the first), so the crawl keeps going and reports everything it finds.
 */
public final class Oracle {

    private final CrashDetector crash;
    private final UIStateValidator ui;
    private final AssetLoadVerifier asset; // reserved for screens with PDF/image containers

    public Oracle(CrashDetector crash, UIStateValidator ui, AssetLoadVerifier asset) {
        this.crash = crash;
        this.ui = ui;
        this.asset = asset;
    }

    public List<ExploreFinding> check(String step) {
        List<ExploreFinding> findings = new ArrayList<>();
        try { crash.assertAlive(step); }
        catch (VerificationError e) { findings.add(ExploreFinding.critical("CRASH", step, e.getMessage())); }

        try { ui.assertNoErrorAlert(); }
        catch (VerificationError e) { findings.add(ExploreFinding.high("ERROR_ALERT", step, e.getMessage())); }

        try { ui.assertNotBlank(step); }
        catch (VerificationError e) { findings.add(ExploreFinding.medium("BLANK_SCREEN", step, e.getMessage())); }

        return findings;
    }
}
