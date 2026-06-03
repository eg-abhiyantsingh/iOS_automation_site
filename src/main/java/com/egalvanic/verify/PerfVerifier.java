package com.egalvanic.verify;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Device-side launch performance verifier.
 *
 * Boundary (honest): this measures app LAUNCH responsiveness from the automation side
 * (terminate → activate → time-to-ready-element). It is a coarse regression guard, not
 * a profiler — deep memory/CPU/energy/frame-rate jank needs Xcode Instruments / XCTest
 * metrics run from the build pipeline, and backend load needs k6/JMeter. Use this to
 * catch gross launch regressions and watchdog-risk slow starts.
 */
public final class PerfVerifier {

    private IOSDriver driver() { return DriverManager.getDriver(); }
    private final String bundleId = AppConstants.APP_BUNDLE_ID;

    /**
     * Cold-ish launch: terminate the app, re-activate it, and measure ms until
     * {@code readyMarker} appears. Hard-fails if it exceeds {@code budgetMs}.
     * (A cold launch over ~4s risks the iOS watchdog and feels broken to users.)
     */
    public long assertLaunchUnder(By readyMarker, long budgetMs) {
        try {
            driver().terminateApp(bundleId);
        } catch (Exception e) {
            System.out.println("[PerfVerifier] terminateApp note: " + e.getMessage());
        }
        long t0 = System.currentTimeMillis();
        driver().activateApp(bundleId);
        try {
            new WebDriverWait(driver(), Duration.ofSeconds(Math.max(5, budgetMs / 1000 + 5)))
                    .until(d -> !d.findElements(readyMarker).isEmpty());
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - t0;
            throw new VerificationError("[PerfVerifier] app did not reach ready state ("
                    + readyMarker + ") within budget after launch — " + elapsed
                    + "ms elapsed, possible hang/slow-launch.");
        }
        long launchMs = System.currentTimeMillis() - t0;
        System.out.println("[PerfVerifier] launch-to-ready = " + launchMs + "ms (budget " + budgetMs + "ms)");
        if (launchMs > budgetMs) {
            throw new VerificationError("[PerfVerifier] launch time " + launchMs
                    + "ms exceeds budget " + budgetMs + "ms (watchdog/slow-start risk).");
        }
        return launchMs;
    }

    /** Non-failing measurement, for trend logging. */
    public long measureLaunch(By readyMarker) {
        try { driver().terminateApp(bundleId); } catch (Exception ignored) {}
        long t0 = System.currentTimeMillis();
        driver().activateApp(bundleId);
        try {
            new WebDriverWait(driver(), Duration.ofSeconds(30))
                    .until(d -> !d.findElements(readyMarker).isEmpty());
        } catch (Exception e) { return -1; }
        return System.currentTimeMillis() - t0;
    }
}
