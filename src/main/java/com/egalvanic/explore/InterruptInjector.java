package com.egalvanic.explore;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.ios.IOSDriver;

import java.time.Duration;
import java.util.Map;
import java.util.Random;

/**
 * iOS-specific chaos: backgrounding, rotation, relaunch, permission flips, low-memory.
 * Uncertain commands are routed through {@code executeScript} so compilation never
 * depends on an optional driver method; every call is best-effort and returns a label.
 */
public final class InterruptInjector {

    private final String bundleId = AppConstants.APP_BUNDLE_ID;

    private IOSDriver driver() { return DriverManager.getDriver(); }

    public String backgroundForeground() {
        try { driver().runAppInBackground(Duration.ofSeconds(3)); return "background 3s + foreground"; }
        catch (Exception e) { return "background unsupported: " + e.getMessage(); }
    }

    public String rotate() {
        try {
            driver().executeScript("mobile: setOrientation", Map.of("orientation", "landscape"));
            driver().executeScript("mobile: setOrientation", Map.of("orientation", "portrait"));
            return "rotate landscape↔portrait";
        } catch (Exception e) { return "rotate unsupported: " + e.getMessage(); }
    }

    public String relaunch() {
        try { driver().terminateApp(bundleId); driver().activateApp(bundleId); return "terminate + activate"; }
        catch (Exception e) { return "relaunch failed: " + e.getMessage(); }
    }

    public String setPermission(String access, String value) {
        try {
            driver().executeScript("mobile: setPermission",
                    Map.of("bundleId", bundleId, "access", Map.of(access, value)));
            return "permission " + access + "=" + value;
        } catch (Exception e) { return "setPermission unsupported: " + e.getMessage(); }
    }

    public String lowMemory(String udid) {
        try {
            new ProcessBuilder("xcrun", "simctl", "spawn", udid == null ? "booted" : udid,
                    "notifyutil", "-p", "UIApplicationMemoryWarning").inheritIO().start();
            return "low-memory signal";
        } catch (Exception e) { return "low-memory unsupported: " + e.getMessage(); }
    }

    public String fireRandom(Random r) {
        return switch (r.nextInt(4)) {
            case 0 -> backgroundForeground();
            case 1 -> rotate();
            case 2 -> relaunch();
            default -> setPermission("photos", r.nextBoolean() ? "yes" : "no");
        };
    }
}
