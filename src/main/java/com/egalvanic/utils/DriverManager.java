package com.egalvanic.utils;

import com.egalvanic.constants.AppConstants;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.remote.http.ClientConfig;

import java.net.URL;
import java.time.Duration;

/**
 * Driver Manager - Singleton driver management
 * Handles driver initialization and cleanup
 * Optimized for CI/CD environments (GitHub Actions)
 *
 * Uses static volatile field instead of ThreadLocal because TestNG's
 * suite-level time-out runs @BeforeMethod in a thread pool — ThreadLocal
 * causes "Driver not initialized" when subclass @BeforeMethod runs on
 * a different pool thread than the one that created the driver.
 */
public class DriverManager {

    private static volatile IOSDriver driver;

    // Override for noReset - allows test classes to skip app reinstall
    private static boolean noResetOverride = false;
    private static boolean useNoResetOverride = false;

    /**
     * Set noReset override for Edit Asset tests (skip app reinstall)
     * Call this BEFORE driver initialization in @BeforeClass
     */
    public static void setNoReset(boolean noReset) {
        noResetOverride = noReset;
        useNoResetOverride = true;
        System.out.println("📱 noReset override set to: " + noReset);
    }

    /**
     * Reset noReset override to default
     */
    public static void resetNoResetOverride() {
        useNoResetOverride = false;
        noResetOverride = false;
    }

    private DriverManager() {
        // Private constructor
    }

    /**
     * Initialize iOS Driver with optimized settings for CI environments
     */
    public static void initDriver() {
        initDriver(null, null, null, null);
    }

    /**
     * Initialize iOS Driver with custom parameters for parallel testing
     *
     * @param deviceName   Optional device name (uses default if null)
     * @param udid         Optional device UDID (uses default if null)
     * @param appiumPort   Optional Appium server port (uses default if null)
     * @param wdaLocalPort Optional WDA local port (uses default if null)
     */
    public static void initDriver(String deviceName, String udid, String appiumPort, String wdaLocalPort) {
        // Check for stale driver: exists but session is dead
        IOSDriver existingDriver = driver;
        if (existingDriver != null) {
            try {
                if (existingDriver.getSessionId() == null) {
                    System.out.println("⚠️ Found dead driver session, removing...");
                    driver = null;
                }
            } catch (Exception e) {
                System.out.println("⚠️ Stale driver detected, removing: " + e.getMessage());
                driver = null;
            }
        }

        if (driver == null) {
            try {
                // Use parameters if provided, otherwise fall back to config defaults
                String server = (appiumPort != null)
                        ? "http://127.0.0.1:" + appiumPort
                        : AppConstants.APPIUM_SERVER;
                String device = (deviceName != null) ? deviceName : AppConstants.DEVICE_NAME;
                String deviceUdid = (udid != null) ? udid : AppConstants.UDID;

                URL appiumServer = new URL(server);

                XCUITestOptions options = new XCUITestOptions();

                // Platform Configuration
                options.setPlatformName(AppConstants.PLATFORM_NAME);
                options.setAutomationName(AppConstants.AUTOMATION_NAME);
                options.setDeviceName(device);
                options.setPlatformVersion(AppConstants.PLATFORM_VERSION);
                options.setUdid(deviceUdid);
                options.setApp(AppConstants.APP_PATH);

                // Set WDA local port for parallel execution (prevents port conflicts)
                if (wdaLocalPort != null) {
                    options.setCapability("appium:wdaLocalPort", Integer.parseInt(wdaLocalPort));
                }

                // ========== CRITICAL: WDA TIMEOUT FIXES FOR CI ==========
                // Simulator boot timeout - 5 minutes (CI simulators can be slow to boot)
                options.setCapability("appium:simulatorBootTimeout", 300000);
                // WDA launch timeout (build + start) - 10 minutes for CI environments (first run needs to build WDA)
                options.setWdaLaunchTimeout(Duration.ofMillis(600000));
                // WDA connection timeout - 5 minutes
                options.setWdaConnectionTimeout(Duration.ofMillis(300000));
                // App launch timeout - 5 minutes
                options.setCapability("appium:launchTimeout", 300000);
                // Command timeout - 10 minutes idle (for long operations)
                options.setNewCommandTimeout(Duration.ofSeconds(600));

                // WDA startup retry settings - more retries for CI stability
                options.setCapability("appium:wdaStartupRetries", 5);
                options.setCapability("appium:wdaStartupRetryInterval", 60000);

                // ========== PERFORMANCE OPTIMIZATIONS ==========
                // Don't rebuild WDA each time (saves 60-90 seconds)
                options.setUseNewWDA(false);
                options.setCapability("appium:usePreinstalledWDA", false);
                // Don't wait for app to be idle (faster element detection)
                options.setWaitForQuiescence(false);
                options.setCapability("appium:shouldUseSingletonTestManager", false);
                options.setCapability("appium:waitForIdleTimeout", 0);

                // ========== ELEMENT VISIBILITY SETTINGS ==========
                options.setCapability("appium:simpleIsVisibleCheck", true);
                options.setCapability("appium:maxTypingFrequency", 60);

                // ========== ALERT HANDLING ==========
                options.setCapability("appium:autoAcceptAlerts", true);
                options.setCapability("appium:autoDismissAlerts", false);

                // ========== RESET BEHAVIOR (Configurable in AppConstants) ==========
                // FULL_RESET=true: Clean install every test (slow but guaranteed clean state)
                // FULL_RESET=false + NO_RESET=false: Clear app data only (fast, usually sufficient)
                // NO_RESET=true: Keep all data (fastest, for Edit Asset tests)
                boolean noReset = useNoResetOverride ? noResetOverride : AppConstants.NO_RESET;
                options.setFullReset(AppConstants.FULL_RESET);
                options.setNoReset(noReset);

                System.out.println("📱 Reset Mode: fullReset=" + AppConstants.FULL_RESET + ", noReset=" + noReset);

                System.out.println("📱 Initializing iOS Driver...");
                System.out.println("📱 Device: " + device);
                System.out.println("📱 UDID: " + deviceUdid);
                System.out.println("📱 Platform Version: " + AppConstants.PLATFORM_VERSION);
                System.out.println("📱 App Path: " + AppConstants.APP_PATH);
                System.out.println("📱 Appium Server: " + server);
                if (wdaLocalPort != null) {
                    System.out.println("📱 WDA Local Port: " + wdaLocalPort);
                }

                // ========== HTTP CLIENT TIMEOUT (CRITICAL FOR HUNG-SESSION RECOVERY) ==========
                // Without this, when the iOS WDA bridge dies mid-test, every Appium HTTP
                // request blocks indefinitely on Unsafe.park — TestNG's suite time-out
                // (420s = 7 min per test) is the only thing that catches it. Forensics
                // on run #24876293380 showed 16 tests hung this way, wasting 112+ min
                // total on dead-session probes.
                //
                // readTimeout(90s) caps every Appium command at 90 seconds. Healthy
                // calls return in <1s; the only impact is on dead sessions, which
                // now fail-fast in 90s instead of 420s. Failed tests still run their
                // teardown, get screenshots, and the next test starts cleanly.
                ClientConfig httpConfig = ClientConfig.defaultConfig()
                        .baseUrl(appiumServer)
                        .connectionTimeout(Duration.ofSeconds(60))
                        .readTimeout(Duration.ofSeconds(90));
                IOSDriver newDriver = new IOSDriver(httpConfig, options);
                newDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));

                driver = newDriver;
                System.out.println("✅ iOS Driver initialized successfully");

            } catch (Exception e) {
                System.err.println("❌ Failed to initialize driver: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize driver: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Get driver instance
     */
    public static IOSDriver getDriver() {
        IOSDriver d = driver;
        if (d == null) {
            throw new IllegalStateException("Driver not initialized. Call initDriver() first.");
        }
        return d;
    }

    /**
     * Terminate/close the app without quitting driver
     * Use this to reset app state between tests
     */
    public static void terminateApp() {
        IOSDriver d = driver;
        if (d != null) {
            try {
                String bundleId = AppConstants.APP_BUNDLE_ID;
                d.terminateApp(bundleId);
                System.out.println("✅ App terminated");
            } catch (Exception e) {
                System.err.println("⚠️ Error terminating app: " + e.getMessage());
            }
        }
    }

    /**
     * Close app and quit driver completely
     * Ensures clean state for next test
     *
     * Flow:
     * 1. Terminate app using bundle ID
     * 2. Verify termination
     * 3. Quit WebDriver session
     */
    public static void quitDriver() {
        IOSDriver d = driver;
        if (d != null) {
            String bundleId = AppConstants.APP_BUNDLE_ID;

            try {
                // Step 1: Terminate the app
                try {
                    ApplicationState state = d.queryAppState(bundleId);
                    if (state != ApplicationState.NOT_RUNNING) {
                        d.terminateApp(bundleId);
                        Thread.sleep(500);  // Wait for app to terminate

                        // Verify termination
                        ApplicationState newState = d.queryAppState(bundleId);
                        if (newState == ApplicationState.NOT_RUNNING) {
                            System.out.println("✅ App terminated successfully");
                        } else {
                            System.out.println("⚠️ App may still be running (state: " + newState + ")");
                            // Try force terminate
                            d.terminateApp(bundleId);
                        }
                    } else {
                        System.out.println("✅ App was not running");
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Could not terminate app: " + e.getMessage());
                }

                // Step 2: Quit driver
                d.quit();
                System.out.println("✅ Driver closed successfully");

            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
                // Try to force quit even on error
                try {
                    d.quit();
                } catch (Exception e2) {
                    // Ignore
                }
            } finally {
                driver = null;
            }
        }
    }

    /**
     * Force-null the driver reference without sending any HTTP commands to Appium.
     * Use this when the Appium session is dead/unresponsive to avoid hanging
     * on quitDriver() which tries terminateApp + quit (both HTTP calls).
     */
    public static void forceNullDriver() {
        driver = null;
        System.out.println("🔌 Driver reference force-nulled (no Appium HTTP calls)");
    }

    /**
     * Check if driver is active
     */
    public static boolean isDriverActive() {
        try {
            IOSDriver d = driver;
            return d != null && d.getSessionId() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
