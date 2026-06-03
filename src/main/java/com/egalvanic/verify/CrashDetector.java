package com.egalvanic.verify;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.OutputType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * Detects the bug class the current suite is blind to: the app crashing, exiting, or
 * freezing mid-flow. Today {@code queryAppState} is called in exactly one place
 * (DriverManager teardown); this brings it into the test body.
 */
public final class CrashDetector {

    private final String bundleId = AppConstants.APP_BUNDLE_ID;

    private IOSDriver driver() { return DriverManager.getDriver(); }

    /** Hard-fail if the app under test is not running in the foreground after a step. */
    public void assertAlive(String afterStep) {
        ApplicationState state;
        try {
            state = driver().queryAppState(bundleId);
        } catch (Exception e) {
            // A dead/unresponsive WDA session also surfaces here — that is itself a defect.
            throw new VerificationError("Could not query app state after: " + afterStep
                    + " — session may be dead", e);
        }
        if (state != ApplicationState.RUNNING_IN_FOREGROUND) {
            captureForensics(afterStep, state);
            throw new VerificationError("App not in foreground after: " + afterStep
                    + " — state=" + state + " (likely crash / unexpected exit)");
        }
    }

    /**
     * Detect a frozen / ANR UI: the element tree is byte-identical across {@code windowSec}
     * DESPITE an interaction, while the app still claims foreground.
     */
    public void assertNotFrozen(String afterStep, int windowSec) {
        String before = hash(safePageSource());
        nudge();
        sleepReal(windowSec * 1000L);
        String after = hash(safePageSource());
        boolean foreground;
        try {
            foreground = driver().queryAppState(bundleId) == ApplicationState.RUNNING_IN_FOREGROUND;
        } catch (Exception e) {
            throw new VerificationError("Session unresponsive while checking freeze after: " + afterStep, e);
        }
        if (foreground && before.equals(after)) {
            captureForensics(afterStep + " [FROZEN]", ApplicationState.RUNNING_IN_FOREGROUND);
            throw new VerificationError("UI appears frozen after: " + afterStep
                    + " — element tree unchanged for " + windowSec + "s despite interaction");
        }
    }

    /** Non-throwing convenience for the crawler / teardown: true if alive & foreground. */
    public boolean isAlive() {
        try { return driver().queryAppState(bundleId) == ApplicationState.RUNNING_IN_FOREGROUND; }
        catch (Exception e) { return false; }
    }

    private void nudge() {
        try { driver().executeScript("mobile: scroll", Map.of("direction", "down")); }
        catch (Exception ignored) { /* best-effort; the hash+state comparison still decides */ }
    }

    private String safePageSource() {
        try { return driver().getPageSource(); } catch (Exception e) { return "<unavailable>"; }
    }

    private void captureForensics(String label, ApplicationState state) {
        try {
            String b64 = driver().getScreenshotAs(OutputType.BASE64);
            com.egalvanic.utils.ExtentReportManager.logFail("CRASH/FREEZE @ " + label
                    + " state=" + state + "<br/><img style='width:320px' src='data:image/png;base64," + b64 + "'/>");
        } catch (Exception ignored) {}
        try { // iOS syslog requires appium:showIOSLog=true — guard so its absence never masks the crash
            var logs = driver().manage().logs().get("syslog").getAll();
            System.out.println("---- syslog tail @ " + label + " ----");
            logs.stream().skip(Math.max(0, logs.size() - 40)).forEach(le -> System.out.println(le.getMessage()));
        } catch (Exception ignored) {}
    }

    private static String hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(d);
        } catch (Exception e) { return Integer.toString(s.length()); }
    }

    /** A REAL pause (unlike BaseTest.sleep, which is until(d->true) and returns immediately). */
    private static void sleepReal(long ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }
}
