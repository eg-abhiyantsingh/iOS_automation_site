package com.egalvanic.utils;

import com.egalvanic.constants.AppConstants;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.URL;
import java.time.Duration;

/**
 * Driver Manager - Thread-safe driver management
 * Handles driver initialization and cleanup
 * Optimized for CI/CD environments (GitHub Actions)
 */
public class DriverManager {

    private static ThreadLocal<IOSDriver> driverThreadLocal = new ThreadLocal<>();

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
        if (driverThreadLocal.get() == null) {
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
                // Simulator boot timeout - 4 minutes (CI simulators can be slow to boot)
                options.setCapability("appium:simulatorBootTimeout", 240000);
                // WDA launch timeout (build + start) - 6 minutes for CI environments (increased for stability)
                options.setWdaLaunchTimeout(Duration.ofMillis(360000));
                // WDA connection timeout - 4 minutes
                options.setWdaConnectionTimeout(Duration.ofMillis(240000));
                // App launch timeout - 3 minutes
                options.setCapability("appium:launchTimeout", 180000);
                // Command timeout - 5 minutes idle
                options.setNewCommandTimeout(Duration.ofSeconds(300));

                // WDA startup retry settings - more retries for CI stability
                options.setCapability("appium:wdaStartupRetries", 5);
                options.setCapability("appium:wdaStartupRetryInterval", 30000);

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
                // NO_RESET=true: Keep all data (fastest, for development only)
                options.setFullReset(AppConstants.FULL_RESET);
                options.setNoReset(AppConstants.NO_RESET);
                
                System.out.println("📱 Reset Mode: fullReset=" + AppConstants.FULL_RESET + ", noReset=" + AppConstants.NO_RESET);

                System.out.println("📱 Initializing iOS Driver...");
                System.out.println("📱 Device: " + device);
                System.out.println("📱 UDID: " + deviceUdid);
                System.out.println("📱 Platform Version: " + AppConstants.PLATFORM_VERSION);
                System.out.println("📱 App Path: " + AppConstants.APP_PATH);
                System.out.println("📱 Appium Server: " + server);
                if (wdaLocalPort != null) {
                    System.out.println("📱 WDA Local Port: " + wdaLocalPort);
                }

                IOSDriver driver = new IOSDriver(appiumServer, options);
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));

                driverThreadLocal.set(driver);
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
        IOSDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException("Driver not initialized. Call initDriver() first.");
        }
        return driver;
    }

    /**
     * Quit driver and cleanup
     */
    public static void quitDriver() {
        IOSDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                System.out.println("✅ Driver closed successfully");
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }

    /**
     * Check if driver is active
     */
    public static boolean isDriverActive() {
        try {
            IOSDriver driver = driverThreadLocal.get();
            return driver != null && driver.getSessionId() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
