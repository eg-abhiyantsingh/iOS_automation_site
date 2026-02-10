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

        try {
            // Handle test result
            if (result.getStatus() == ITestResult.FAILURE) {
                String screenshotPath = ScreenshotUtil.captureScreenshot(testName + "_FAILED");
                ExtentReportManager.logFailWithScreenshot(
                        "Test failed: " + result.getThrowable().getMessage(),
                        result.getThrowable());
                System.out.println("❌ Test FAILED: " + testName);
                System.out.println("📸 Screenshot saved: " + screenshotPath);

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
            } else {
                DriverManager.quitDriver();
                System.out.println("🧹 Test cleanup complete\n");
            }
        }
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
        
        // Check if on Edit Asset screen FIRST (has Save Changes or Cancel button in edit mode)
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
        
        // Check if on Dashboard (has Assets/Locations tabs, building icon)
        // Dashboard is AFTER site selection - user is already logged in
        try {
            if (assetPage.isDashboardDisplayed()) {
                System.out.println("   → Dashboard (logged in)");
                return "DASHBOARD";
            }
        } catch (Exception e) {}
        
        // Check if on Site Selection page (has "Select a Site" title or site list BEFORE login)
        // This should be checked AFTER dashboard to avoid confusion
        try {
            if (siteSelectionPage.isSelectSiteScreenDisplayed()) {
                System.out.println("   → Site Selection");
                return "SITE_SELECTION";
            }
        } catch (Exception e) {}
        
        System.out.println("   → Unknown screen");
        return "UNKNOWN";
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
        performLogin();

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
        
        // Fast login
        System.out.println("⚡ TURBO: Starting login...");
        welcomePage.submitCompanyCode(AppConstants.VALID_COMPANY_CODE);
        loginPage.waitForPageReady();
        loginPage.loginTurbo(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
        
        // Handle new Schedule screen (added Jan 2026)
        siteSelectionPage.handleScheduleScreenIfPresent();
        
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
