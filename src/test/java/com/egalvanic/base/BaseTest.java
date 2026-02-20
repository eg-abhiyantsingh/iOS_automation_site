package com.egalvanic.base;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetPage;
import com.egalvanic.pages.LoginPage;
import com.egalvanic.pages.SiteSelectionPage;
import com.egalvanic.pages.WelcomePage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.utils.ScreenshotUtil;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * Base Test - Parent class for all Test classes
 * Handles driver lifecycle, report initialization, and common test setup
 */
public class BaseTest {

    // Page Objects
    protected WelcomePage welcomePage;
    protected LoginPage loginPage;
    protected SiteSelectionPage siteSelectionPage;
    protected AssetPage assetPage;

    // Flag to skip setup/teardown for chained tests
    protected static boolean skipNextSetup = false;
    protected static boolean skipNextTeardown = false;

    // ================================================================
    // SUITE LEVEL SETUP/TEARDOWN
    // ================================================================

    @BeforeSuite
    public void suiteSetup() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     eGalvanic iOS Automation - Test Suite Starting           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // Initialize both Extent Reports
        ExtentReportManager.initReports();

        // Cleanup old screenshots (older than 7 days)
        ScreenshotUtil.cleanupOldScreenshots(7);
    }

    @AfterSuite
    public void suiteTeardown() {
        // Flush both reports
        ExtentReportManager.flushReports();

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     eGalvanic iOS Automation - Test Suite Complete           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("📊 Reports generated:");
        System.out.println("   - Detailed: " + ExtentReportManager.getDetailedReportPath());
        System.out.println("   - Client:   " + ExtentReportManager.getClientReportPath());
    }

    // ================================================================
    // TEST LEVEL SETUP/TEARDOWN
    // ================================================================

    @BeforeMethod
    @Parameters({ "deviceName", "udid", "appiumPort", "wdaLocalPort" })
    public void testSetup(
            @Optional String deviceName,
            @Optional String udid,
            @Optional String appiumPort,
            @Optional String wdaLocalPort) {
        // Skip setup for chained tests
        if (skipNextSetup) {
            // Verify the driver is still alive before reusing it
            if (!DriverManager.isDriverActive()) {
                System.out.println("\n⚠️ Chained driver is dead, resetting to fresh setup...");
                skipNextSetup = false;
                skipNextTeardown = false;
                // Fall through to normal setup below
            } else {
                System.out.println("\n🔗 Continuing from previous test (skipping setup)...");
                skipNextSetup = false;
                // Re-initialize page objects with existing driver
                welcomePage = new WelcomePage();
                loginPage = new LoginPage();
                siteSelectionPage = new SiteSelectionPage();
                assetPage = new AssetPage();
                return;
            }
        }

        System.out.println("\n🚀 Setting up test...");

        // Initialize driver with retry logic for CI resilience
        // Note: Do NOT call cleanupStaleDriver() here — test classes with @BeforeClass
        // (e.g., Connections_Test) create their driver before @BeforeMethod runs,
        // and page objects cache that driver reference. Killing it would leave page
        // objects pointing at a dead driver, causing 5-minute hangs.
        // Stale/dead sessions are handled inside DriverManager.initDriver() instead.
        try {
            DriverManager.initDriver(deviceName, udid, appiumPort, wdaLocalPort);
        } catch (Exception e) {
            System.out.println("⚠️ Driver init failed: " + e.getMessage());
            System.out.println("🔄 Retrying driver initialization after cleanup...");
            forceDriverCleanup();
            try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            DriverManager.initDriver(deviceName, udid, appiumPort, wdaLocalPort);
        }

        // Soft restart: kill app process and relaunch to clear navigation/tab state
        // With noReset=true, login data persists but stale screen state is cleared
        // IMPORTANT: terminateApp and activateApp in separate try-catches so a failed
        // terminate doesn't skip the activate (which would leave the app not running)
        boolean terminated = false;
        try {
            DriverManager.getDriver().terminateApp(AppConstants.APP_BUNDLE_ID);
            Thread.sleep(500);
            terminated = true;
        } catch (Exception e) {
            System.out.println("⚠️ terminateApp failed (app may not be running yet): " + e.getMessage());
        }
        try {
            DriverManager.getDriver().activateApp(AppConstants.APP_BUNDLE_ID);
            Thread.sleep(500);
            System.out.println("🔄 App soft-restarted (clean navigation state)");
        } catch (Exception e) {
            System.out.println("⚠️ activateApp failed: " + e.getMessage());
            if (!terminated) {
                // Both failed — session may be dead, try reinitializing driver
                System.out.println("🔄 Session appears dead, reinitializing driver...");
                try {
                    DriverManager.quitDriver();
                } catch (Exception ignored) {}
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                DriverManager.initDriver(deviceName, udid, appiumPort, wdaLocalPort);
            }
        }

        // Initialize Page Objects
        welcomePage = new WelcomePage();
        loginPage = new LoginPage();
        siteSelectionPage = new SiteSelectionPage();
        assetPage = new AssetPage();

        // FAST app state detection (2 seconds max)
        // Skip waiting for welcome page if already logged in
        waitForAppReadyFast();

        System.out.println("✅ Test setup complete\n");
    }

    /**
     * FAST app ready check - detects current state with minimal wait
     * Checks: Dashboard/Asset screens (logged in) OR Welcome/Login page (not logged in)
     */
    private void waitForAppReadyFast() {
        try {
            // 2-second fast check for any known screen
            org.openqa.selenium.support.ui.WebDriverWait fastWait = 
                new org.openqa.selenium.support.ui.WebDriverWait(
                    com.egalvanic.utils.DriverManager.getDriver(), 
                    java.time.Duration.ofSeconds(2)
                );
            
            fastWait.until(driver -> {
                // Check if on dashboard (already logged in) - FASTEST PATH
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.accessibilityId("building.2"));
                    System.out.println("⚡ App ready - Dashboard detected (already logged in)");
                    return true;
                } catch (Exception e) {}
                
                // Check if on asset list (already logged in)
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.accessibilityId("plus"));
                    System.out.println("⚡ App ready - Asset List detected (already logged in)");
                    return true;
                } catch (Exception e) {}
                
                // Check if on welcome page (needs login)
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.accessibilityId("Continue"));
                    System.out.println("⚡ App ready - Welcome page detected");
                    return true;
                } catch (Exception e) {}
                
                // Check if on login page (needs login)
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.accessibilityId("Sign In"));
                    System.out.println("⚡ App ready - Login page detected");
                    return true;
                } catch (Exception e) {}
                
                return false;
            });
        } catch (Exception e) {
            System.out.println("⚠️ Fast app check timeout, continuing...");
        }
    }

    /**
     * Clean up any stale driver left over from a previous failed test or module.
     * Prevents dead driver references in ThreadLocal from blocking new driver creation.
     */
    private void cleanupStaleDriver() {
        try {
            if (DriverManager.isDriverActive()) {
                System.out.println("⚠️ Found existing driver session, cleaning up...");
                DriverManager.quitDriver();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error cleaning stale driver: " + e.getMessage());
            forceDriverCleanup();
        }
    }

    /**
     * Force cleanup of driver resources when normal quit fails.
     * Kills the ThreadLocal reference so a fresh driver can be created.
     */
    private void forceDriverCleanup() {
        try {
            DriverManager.quitDriver();
        } catch (Exception e) {
            System.out.println("⚠️ Force cleanup error (ignored): " + e.getMessage());
        }
    }

    @AfterMethod(alwaysRun = true)
    public void testTeardown(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        // Detect if the Appium session is likely dead/unresponsive.
        // When dead, every Appium HTTP call (screenshot, terminateApp, quit) hangs 7+ min each,
        // turning a single failure into a 14-21 min teardown. Skip all Appium calls and just
        // null the driver reference.
        boolean sessionDead = (result.getStatus() == ITestResult.FAILURE)
                && isSessionLikelyDead(result.getThrowable());

        try {
            // Handle test result
            if (result.getStatus() == ITestResult.FAILURE) {
                if (sessionDead) {
                    // Session is dead — logFail without screenshot to avoid 7+ min hang
                    // from getScreenshotAsBase64() trying to reach the dead Appium server
                    ExtentReportManager.logFail(
                            "Test failed (session dead): " + result.getThrowable().getMessage());
                    System.out.println("❌ Test FAILED: " + testName);
                    System.out.println("⚠️ Session appears dead — skipping Appium calls in teardown");
                } else {
                    try {
                        String screenshotPath = ScreenshotUtil.captureScreenshot(testName + "_FAILED");
                        System.out.println("📸 Screenshot saved: " + screenshotPath);
                    } catch (Exception e) {
                        System.out.println("⚠️ Screenshot capture failed: " + e.getMessage());
                    }
                    ExtentReportManager.logFailWithScreenshot(
                            "Test failed: " + result.getThrowable().getMessage(),
                            result.getThrowable());
                    System.out.println("❌ Test FAILED: " + testName);
                }

            } else if (result.getStatus() == ITestResult.SKIP) {
                String skipReason = (result.getThrowable() != null)
                        ? result.getThrowable().getMessage() : "Unknown reason";
                ExtentReportManager.logSkip("Test skipped: " + skipReason);
                System.out.println("⏭️ Test SKIPPED: " + testName);
                System.out.println("   Skip reason: " + skipReason);
                if (result.getThrowable() != null) {
                    result.getThrowable().printStackTrace(System.out);
                }

            } else if (result.getStatus() == ITestResult.SUCCESS) {
                ExtentReportManager.logPass("Test passed successfully");
                System.out.println("✅ Test PASSED: " + testName);
            }

            ExtentReportManager.removeTests();
        } catch (Exception e) {
            System.out.println("⚠️ Error in test result handling: " + e.getMessage());
        } finally {
            // ALWAYS close app and driver
            if (skipNextTeardown) {
                System.out.println("🔗 Keeping driver alive for next chained test\n");
                skipNextTeardown = false;
                skipNextSetup = true;
            } else if (sessionDead) {
                // Session is dead — don't send any HTTP commands, just null the reference
                DriverManager.forceNullDriver();
                System.out.println("🧹 Test cleanup complete (fast — session was dead)\n");
            } else {
                // On FAILURE: force terminate app so next test starts fresh
                // Without this, noReset=true leaves the app on the failed screen
                if (result.getStatus() == ITestResult.FAILURE) {
                    try {
                        if (DriverManager.isDriverActive()) {
                            DriverManager.getDriver().terminateApp(AppConstants.APP_BUNDLE_ID);
                            System.out.println("🔄 App force terminated after failure (clean slate for next test)");
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not terminate app: " + e.getMessage());
                    }
                }
                DriverManager.quitDriver();
                System.out.println("🧹 Test cleanup complete\n");
            }
        }
    }

    /**
     * Check if a test failure was likely caused by a dead/unresponsive Appium session.
     * Walks the exception chain looking for known session-death indicators.
     * When true, teardown should skip all Appium HTTP calls to avoid 14+ min hangs.
     */
    private boolean isSessionLikelyDead(Throwable t) {
        if (t == null) return false;
        Throwable current = t;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage() != null ? current.getMessage() : "";
            // Known session-dead exception types
            if (className.contains("NoSuchSessionException") ||
                className.contains("SessionNotCreatedException") ||
                className.contains("UnreachableBrowserException")) {
                return true;
            }
            // Known session-dead message patterns
            if (message.contains("Connection refused") ||
                message.contains("Connection reset") ||
                message.contains("Connection timed out") ||
                message.contains("NettyResponseFuture") ||
                message.contains("session is either terminated") ||
                message.contains("Session not found") ||
                message.contains("Could not start a new session") ||
                message.contains("Unable to create session")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    // ================================================================
    // ██████████████████████████████████████████████████████████████
    // ██ OPTIMIZED LOGIN METHODS - DO NOT MODIFY ██
    // ██ These methods are PRODUCTION-READY and FULLY OPTIMIZED ██
    // ██ Last optimized: January 2026 - WORKING PERFECTLY ██
    // ██████████████████████████████████████████████████████████████
    // ================================================================

    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ CRITICAL: DO NOT MODIFY THIS METHOD ║
     * ║ This login flow is fully optimized and handles: ║
     * ║ - Company code entry ║
     * ║ - Credential entry ║
     * ║ - Save Password popup (handled in LoginPage.login()) ║
     * ║ Status: PRODUCTION READY - TESTED & VERIFIED ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void performLogin() {
        System.out.println("🔐 Performing login...");

        // Enter company code - wait for login page to appear
        welcomePage.submitCompanyCode(AppConstants.VALID_COMPANY_CODE);
        loginPage.waitForPageReady();

        // Enter credentials and login (Save Password popup is handled inside login())
        loginPage.loginTurbo(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);


        // Handle new Schedule screen (added Jan 2026)
        // After login, app shows Schedule screen - click "View Sites" to proceed
        siteSelectionPage.handleScheduleScreenIfPresent();
        System.out.println("✅ Login completed");
    }

    // ================================================================
    // SMART NAVIGATION - State-based navigation for faster tests
    // ================================================================

    /**
     * Detect current app state and return appropriate action
     * @return "LOGIN_PAGE", "SITE_SELECTION", "DASHBOARD", "ASSET_LIST", "ASSET_DETAIL", "EDIT_ASSET", "UNKNOWN"
     */
    protected String detectCurrentScreen() {
        System.out.println("🔍 Detecting current screen...");

        // PERFORMANCE FIX: Temporarily reduce implicit wait during screen detection.
        // Each failed findElement waits the full implicit wait (5s). With 7 screen checks
        // and multiple element lookups per check, the worst case was ~75 seconds.
        // With 1-second implicit wait, worst case drops to ~15 seconds.
        io.appium.java_client.AppiumDriver currentDriver = DriverManager.getDriver();
        if (currentDriver != null) {
            try {
                currentDriver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(1));
            } catch (Exception e) {
                // Session may be dead — continue with default timeout
            }
        }

        try {
            // Check DASHBOARD FIRST — most common state with noReset=true
            try {
                if (assetPage.isDashboardDisplayed()) {
                    System.out.println("   → Dashboard (logged in)");
                    return "DASHBOARD";
                }
            } catch (Exception e) {}

            // Check SITE SELECTION — second most common with noReset=true
            // Use lightweight inline check instead of heavy isSelectSiteScreenDisplayed()
            // which polls 5 times × 8 element checks = 40+ findElement calls
            if (currentDriver != null) {
                try {
                    java.util.List<org.openqa.selenium.WebElement> siteTitle = currentDriver.findElements(
                        io.appium.java_client.AppiumBy.accessibilityId("Select Site"));
                    if (!siteTitle.isEmpty()) {
                        System.out.println("   → Site Selection");
                        return "SITE_SELECTION";
                    }
                    // Fallback: check for nav bar with "Site" text
                    java.util.List<org.openqa.selenium.WebElement> siteNav = currentDriver.findElements(
                        io.appium.java_client.AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeNavigationBar' AND name CONTAINS 'Site'"));
                    if (!siteNav.isEmpty()) {
                        System.out.println("   → Site Selection (nav bar)");
                        return "SITE_SELECTION";
                    }
                } catch (Exception e) {}
            }

            // Check WELCOME PAGE - has "Continue" button (NOT "Sign In")
            try {
                if (welcomePage.isContinueButtonDisplayed()) {
                    System.out.println("   → Welcome Page (Company Code)");
                    return "WELCOME_PAGE";
                }
            } catch (Exception e) {}

            // Check LOGIN PAGE - has "Sign In" button and password field
            try {
                if (loginPage.isSignInButtonDisplayed()) {
                    System.out.println("   → Login Page (Email/Password)");
                    return "LOGIN_PAGE";
                }
            } catch (Exception e) {}

            // Check if on Edit Asset screen (has Save Changes or Cancel button in edit mode)
            try {
                if (assetPage.isEditAssetScreenDisplayed()) {
                    System.out.println("   → Edit Asset Screen");
                    return "EDIT_ASSET";
                }
            } catch (Exception e) {}

            // Check if on Asset Detail (has Edit button, Asset Details nav)
            try {
                if (assetPage.isAssetDetailDisplayed()) {
                    System.out.println("   → Asset Detail");
                    return "ASSET_DETAIL";
                }
            } catch (Exception e) {}

            // Check if on Asset List (has plus button for adding assets)
            try {
                if (assetPage.isAssetListDisplayed()) {
                    System.out.println("   → Asset List");
                    return "ASSET_LIST";
                }
            } catch (Exception e) {}

            System.out.println("   → Unknown screen");
            return "UNKNOWN";
        } finally {
            // ALWAYS restore implicit wait to normal value
            if (currentDriver != null) {
                try {
                    currentDriver.manage().timeouts().implicitlyWait(
                        java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
                } catch (Exception e) {
                    // Session may be dead — nothing to restore
                }
            }
        }
    }
    
    /**
     * Smart navigation to Edit Asset screen
     * Simple approach: Try to go to asset directly, if fails do full flow
     */
    protected void smartNavigateToEditAsset() {
        System.out.println("⚡ Smart navigation to Edit Asset...");
        
        // Brief wait for app to stabilize
        try { Thread.sleep(500); } catch (Exception e) {}
        
        // Try to navigate to asset directly (if already logged in)
        try {
            System.out.println("🚀 Attempting direct navigation to asset...");
            
            // Try clicking on Assets tab/button
            if (tryDirectAssetNavigation()) {
                System.out.println("✅ Direct navigation successful!");
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Direct navigation failed: " + e.getMessage());
        }
        
        // Direct navigation failed - do full flow
        System.out.println("🔄 Direct navigation failed, doing full login flow...");
        doFullLoginAndNavigateToEditAsset();
    }
    
    /**
     * Try to navigate directly to Edit Asset (if already logged in)
     */
    private boolean tryDirectAssetNavigation() {
        try {
            // Try to click Assets button/tab
            assetPage.navigateToAssetList();
            shortWait();
            
            // Check if we got to asset list
            if (assetPage.isAssetListDisplayed()) {
                // Select first asset
                assetPage.selectFirstAsset();
                shortWait();
                
                // Click Edit
                assetPage.clickEdit();
                assetPage.waitForEditScreenReady();
                return true;
            }
        } catch (Exception e) {
            // Navigation failed
        }
        return false;
    }
    
    /**
     * Full login flow and navigate to Edit Asset
     */
    private void doFullLoginAndNavigateToEditAsset() {
        System.out.println("🏁 Starting full login flow...");
        
        // Full login (handles company code + credentials)
        performLogin();
        
        // Select site
        String selectedSite = siteSelectionPage.turboSelectSite();
        if (selectedSite == null) {
            selectedSite = siteSelectionPage.selectFirstSiteUltraFast();
        }
        siteSelectionPage.waitForDashboardFast();
        
        // Navigate to asset list
        assetPage.navigateToAssetList();
        shortWait();
        
        // Select first asset
        assetPage.selectFirstAsset();
        shortWait();
        
        // Click Edit
        assetPage.clickEdit();
        assetPage.waitForEditScreenReady();
        
        System.out.println("✅ Full flow complete - on Edit Asset screen");
    }
    
    /**
     * Smart navigation to Dashboard
     * Checks current state and takes shortest path
     */
    protected void smartNavigateToDashboard() {
        // FAST PATH: Check Dashboard first (1 second max)
        if (assetPage != null && assetPage.isDashboardDisplayedFast()) {
            System.out.println("⚡ Smart navigation — already on Dashboard (fast check)");
            return;
        }

        String currentScreen = detectCurrentScreen();
        System.out.println("⚡ Smart navigation to Dashboard from: " + currentScreen);
        
        switch (currentScreen) {
            case "DASHBOARD":
                System.out.println("✅ Already on Dashboard");
                break;
                
            case "ASSET_LIST":
            case "ASSET_DETAIL":
            case "EDIT_ASSET":
                // Go back to dashboard
                System.out.println("🔙 Going back to Dashboard...");
                assetPage.clickBack();
                shortWait();
                // May need multiple backs
                if (!assetPage.isDashboardDisplayed()) {
                    assetPage.clickBack();
                    shortWait();
                }
                break;
                
            case "SITE_SELECTION":
                System.out.println("🌐 Selecting site...");
                siteSelectionPage.turboSelectSite();
                siteSelectionPage.waitForDashboardFast();
                break;
                
            case "LOGIN_PAGE":
                System.out.println("🔐 Logging in...");
                loginPage.loginTurbo(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                siteSelectionPage.turboSelectSite();
                siteSelectionPage.waitForDashboardFast();
                break;
                
            case "WELCOME_PAGE":
                System.out.println("🏁 Full login flow...");
                performLogin();
                siteSelectionPage.turboSelectSite();
                siteSelectionPage.waitForDashboardFast();
                break;
                
            default:
                System.out.println("❓ Unknown - full flow...");
                performLogin();
                siteSelectionPage.turboSelectSite();
                siteSelectionPage.waitForDashboardFast();
                break;
        }
    }



    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ CRITICAL: DO NOT MODIFY THIS METHOD ║
     * ║ Optimized login + navigate to site selection screen ║
     * ║ Status: PRODUCTION READY - TESTED & VERIFIED ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void loginAndGoToDashboard() {
        performLogin();

        // Wait for site selection screen to be ready
        siteSelectionPage.waitForSiteListReady();

        System.out.println("✅ On Site Selection Screen");
    }

    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ CRITICAL: DO NOT MODIFY THIS METHOD ║
     * ║ Optimized login + fast site selection (sub-3 second) ║
     * ║ Uses selectFirstSiteFast() for maximum speed ║
     * ║ Status: PRODUCTION READY - TESTED & VERIFIED ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void loginAndSelectSite() {
        // FAST PATH: Check Dashboard first (1 second max) — skip full detection on happy path
        if (assetPage != null && assetPage.isDashboardDisplayedFast()) {
            System.out.println("✅ loginAndSelectSite — already on Dashboard (fast check)");
            return;
        }

        // Not on Dashboard — detect current screen to avoid typing into wrong field.
        // With noReset=true, the app may be on Site Selection, Login, or Welcome.
        String currentScreen = detectCurrentScreen();
        System.out.println("🔐 loginAndSelectSite — current screen: " + currentScreen);

        if ("DASHBOARD".equals(currentScreen)) {
            System.out.println("✅ Already on Dashboard — skipping login and site selection");
            return;
        }

        if ("SITE_SELECTION".equals(currentScreen)) {
            // Already logged in, just need to select a site
            System.out.println("🔍 Already on Site Selection — skipping login, selecting site...");
        } else {
            // On Welcome/Login page or unknown — do full login
            performLogin();
        }

        // Select first site immediately (combined wait + select)
        System.out.println("🔍 Selecting first available site...");
        String selectedSite = siteSelectionPage.selectFirstSiteFast();
        System.out.println("Selecting first site: (s) " + selectedSite);

        // Wait for dashboard to load after site selection
        siteSelectionPage.waitForDashboardReady();

        System.out.println("✅ Site selected and loaded");
    }

    // ================================================================
    // LOGGING HELPER METHODS
    // ================================================================

    /**
     * Log a test step
     */
    protected void logStep(String stepDescription) {
        ExtentReportManager.logInfo(stepDescription);
        System.out.println("📝 " + stepDescription);
    }

    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ TURBO MODE: Login + Site Selection in minimum time           ║
     * ║ Target: Under 5 seconds for entire operation                 ║
     * ║ Uses: turboSelectSite() + waitForDashboardFast()             ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void loginAndSelectSiteTurbo() {
        long start = System.currentTimeMillis();

        // FAST PATH: Check Dashboard first (1 second max)
        if (assetPage != null && assetPage.isDashboardDisplayedFast()) {
            System.out.println("⚡ TURBO: Already on Dashboard (fast check) — skipping");
            return;
        }

        // Not on Dashboard — detect current screen to avoid typing into wrong field
        String currentScreen = detectCurrentScreen();
        System.out.println("⚡ TURBO: current screen: " + currentScreen);

        if ("DASHBOARD".equals(currentScreen)) {
            System.out.println("⚡ TURBO: Already on Dashboard — skipping");
            return;
        }

        if (!"SITE_SELECTION".equals(currentScreen)) {
            // On Welcome/Login page — do full login
            System.out.println("⚡ TURBO: Starting login...");
            welcomePage.submitCompanyCode(AppConstants.VALID_COMPANY_CODE);
            loginPage.waitForPageReady();
            loginPage.loginTurbo(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);

            // Handle new Schedule screen (added Jan 2026)
            siteSelectionPage.handleScheduleScreenIfPresent();
        }

        // Turbo site selection
        System.out.println("⚡ TURBO: Selecting site...");
        String site = siteSelectionPage.turboSelectSite();

        // Fast dashboard wait
        siteSelectionPage.waitForDashboardFast();

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("⚡ TURBO: Complete in " + elapsed + "ms - Site: " + site);
    }

    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ ULTRA FAST: Random site selection                            ║
     * ║ Uses selectRandomSiteUltraFast() for speed                   ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void loginAndSelectRandomSiteFast() {
        performLogin();
        
        System.out.println("⚡ Selecting random site (fast)...");
        String site = siteSelectionPage.selectRandomSiteUltraFast();
        System.out.println("⚡ Random site: " + site);
        
        siteSelectionPage.waitForDashboardFast();
    }



    /**
     * Log a step with screenshot (uses Base64 for portability)
     */
    protected void logStepWithScreenshot(String stepDescription) {
        ExtentReportManager.logStepWithBase64Screenshot(stepDescription);
        System.out.println("📸 " + stepDescription);
    }

    /**
     * Log warning
     */
    protected void logWarning(String message) {
        ExtentReportManager.logWarning(message);
        System.out.println("⚠️ " + message);
    }

    // ================================================================
    // ASSERTION HELPER METHODS
    // ================================================================

    /**
     * Assert true with logging
     */
    protected void assertTrue(boolean condition, String message) {
        if (condition) {
            logStep("✅ Assertion passed: " + message);
        } else {
            ExtentReportManager.logFail("Assertion failed: " + message);
            throw new AssertionError(message);
        }
    }

    /**
     * Assert false with logging
     */
    protected void assertFalse(boolean condition, String message) {
        if (!condition) {
            logStep("✅ Assertion passed: " + message);
        } else {
            ExtentReportManager.logFail("Assertion failed: " + message);
            throw new AssertionError(message);
        }
    }

    /**
     * Assert equals with logging
     */
    protected void assertEquals(Object actual, Object expected, String message) {
        if (expected.equals(actual)) {
            logStep("✅ Assertion passed: " + message);
        } else {
            String errorMsg = message + " - Expected: " + expected + ", Actual: " + actual;
            ExtentReportManager.logFail(errorMsg);
            throw new AssertionError(errorMsg);
        }
    }

    /**
     * Assert not null with logging
     */
    protected void assertNotNull(Object object, String message) {
        if (object != null) {
            logStep("✅ Assertion passed: " + message);
        } else {
            ExtentReportManager.logFail("Assertion failed: " + message + " (Object is null)");
            throw new AssertionError(message + " - Object is null");
        }
    }

    /**
     * Fail test with message
     */
    protected void fail(String message) {
        ExtentReportManager.logFail("Test FAILED: " + message);
        throw new AssertionError(message);
    }

    // ================================================================
    // WAIT HELPER METHODS (CI-safe explicit waits)
    // ================================================================

    /**
     * Wait for specified milliseconds using explicit wait (CI-safe)
     */
    protected void sleep(int milliseconds) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                    com.egalvanic.utils.DriverManager.getDriver(),
                    java.time.Duration.ofMillis(milliseconds)).until(d -> true);
        } catch (Exception e) {
            // Ignore timeout
        }
    }

    /**
     * Short wait (1 second) - CI-safe
     */
    protected void shortWait() {
        sleep(200); // CI-OPTIMIZED: 300ms -> 200ms
    }

    /**
     * Medium wait (1 second) - CI-safe
     */
    protected void mediumWait() {
        sleep(400); // CI-OPTIMIZED: 600ms -> 400ms
    }

    /**
     * Long wait (2 seconds) - CI-safe
     */
    protected void longWait() {
        sleep(800); // CI-OPTIMIZED: 1200ms -> 800ms
    }

    /**
     * Dismiss any alert that might be present (Save Password, etc.)
     */
    protected void dismissAnyAlert() {
        try {
            welcomePage.handleSavePasswordAlert();
        } catch (Exception e) {
            // No alert present - continue
        }
    }

    /**
     * Mark this test to chain with next test (don't quit driver)
     * Call this at the END of a test that should continue to the next test
     */
    protected void chainToNextTest() {
        skipNextTeardown = true;
        System.out.println("🔗 Test will chain to next dependent test");
    }
}
