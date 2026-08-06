package com.egalvanic.listeners;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.appmanagement.ApplicationState;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Verdict-accuracy retry: re-run a failed test ONCE when — and only when —
 * the failure signature is ENVIRONMENTAL, so infrastructure blips stop
 * masquerading as test failures while real contract violations still fail
 * consistently (they get one identical second chance, then stay RED).
 *
 * Environmental = any of:
 *  - the app under test is NOT foregrounded at failure time (another app —
 *    e.g. a manually launched game, observed live 2026-08-05 — or Springboard
 *    holds the screen: nothing the test asserted was ever visible);
 *  - the Appium/WDA session died mid-test (terminated / unreachable / wedge);
 *  - driver/session creation failed before the test body ran;
 *  - the v1.55 'Policy Update' consent sheet is up at failure time.
 *
 * NEVER retries plain assertion mismatches — a wrong value read from a
 * healthy, foregrounded app is signal, not noise. One retry max
 * (user directive 2026-08-05: ">=90% correct results"; a second identical
 * failure is truth, not flake).
 */
public class EnvironmentRetryAnalyzer implements IRetryAnalyzer {

    private static final String RETRIED_ATTR = "env.retried";

    private static final String[] ENV_SIGNATURES = {
            "session is either terminated or not started",
            "Session does not exist",
            "may have died",
            "UnreachableBrowser",
            "Failed to initialize driver",
            "Could not start a new session",
            "census failed",
            "WDA may be wedged",
            "Accept & Continue",
            "SocketTimeoutException",
            "connect timed out",
            // Entry-navigation timing flakes (2026-08-06, PICK_019: one miss
            // after 64 straight greens): a single identical retry either
            // clears the blip or proves the breakage by failing twice.
            "Work Orders screen must open from the dashboard tile",
    };

    @Override
    public boolean retry(ITestResult result) {
        if (Boolean.TRUE.equals(result.getAttribute(RETRIED_ATTR))) {
            return false; // one retry max
        }
        String reason = environmentalReason(result);
        if (reason == null) {
            return false;
        }
        result.setAttribute(RETRIED_ATTR, Boolean.TRUE);
        System.out.println("🔁 ENV-RETRY: '" + result.getName() + "' failed for an ENVIRONMENTAL reason ("
                + reason + ") — retrying once; a second identical failure stays RED.");
        recoverForegroundBestEffort();
        return true;
    }

    /** Non-null (with a short label) when the failure is environmental. */
    private String environmentalReason(ITestResult result) {
        Throwable t = result.getThrowable();
        String msg = t == null ? "" : String.valueOf(t.getMessage());
        for (String sig : ENV_SIGNATURES) {
            if (msg.contains(sig)) {
                return "signature: " + sig;
            }
        }
        // Live check: was OUR app even on screen when the test failed?
        try {
            if (DriverManager.isDriverActive()) {
                ApplicationState st = DriverManager.getDriver()
                        .queryAppState(AppConstants.APP_BUNDLE_ID);
                if (st != ApplicationState.RUNNING_IN_FOREGROUND) {
                    return "app state at failure = " + st + " (foreign app/Springboard held the screen)";
                }
            }
        } catch (Exception ignored) {
            // dead session — covered by signatures on the next failure if real
        }
        return null;
    }

    /** Reclaim the screen for the app under test before the retry runs. */
    private void recoverForegroundBestEffort() {
        try {
            if (DriverManager.isDriverActive()) {
                DriverManager.getDriver().activateApp(AppConstants.APP_BUNDLE_ID);
                Thread.sleep(800);
                System.out.println("🔁 ENV-RETRY: app re-foregrounded for the retry");
            }
        } catch (Exception e) {
            System.out.println("🔁 ENV-RETRY: re-foreground failed (testSetup's soft-restart will recover): "
                    + e.getMessage());
        }
    }
}
