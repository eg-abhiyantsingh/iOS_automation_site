package com.egalvanic.base;

import com.egalvanic.constants.AppConstants;
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
    public void testSetup() {
        // Skip setup for chained tests
        if (skipNextSetup) {
            System.out.println("\n🔗 Continuing from previous test (skipping setup)...");
            skipNextSetup = false;
            // Re-initialize page objects with existing driver
            welcomePage = new WelcomePage();
            loginPage = new LoginPage();
            siteSelectionPage = new SiteSelectionPage();
            return;
        }
        
        System.out.println("\n🚀 Setting up test...");
        
        // Initialize driver
        DriverManager.initDriver();
        
        // Initialize Page Objects
        welcomePage = new WelcomePage();
        loginPage = new LoginPage();
        siteSelectionPage = new SiteSelectionPage();
        
        // Wait for app to load using explicit wait (checks if welcome page is ready)
        welcomePage.waitForPageReady();
        
        System.out.println("✅ Test setup complete\n");
    }

    @AfterMethod
    public void testTeardown(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        
        // Handle test result
        if (result.getStatus() == ITestResult.FAILURE) {
            // Capture screenshot on failure and use it in the report
            String screenshotPath = ScreenshotUtil.captureScreenshot(testName + "_FAILED");
            ExtentReportManager.logFailWithScreenshot(
                "Test failed: " + result.getThrowable().getMessage(),
                result.getThrowable()
            );
            System.out.println("❌ Test FAILED: " + testName);
            System.out.println("📸 Screenshot saved: " + screenshotPath);
            
        } else if (result.getStatus() == ITestResult.SKIP) {
            ExtentReportManager.logSkip("Test skipped: " + 
                (result.getThrowable() != null ? result.getThrowable().getMessage() : "Unknown reason"));
            System.out.println("⏭️ Test SKIPPED: " + testName);
            
        } else if (result.getStatus() == ITestResult.SUCCESS) {
            ExtentReportManager.logPass("Test passed successfully");
            System.out.println("✅ Test PASSED: " + testName);
        }
        
        // Cleanup
        ExtentReportManager.removeTests();
        
        // Skip driver quit for chained tests
        if (skipNextTeardown) {
            System.out.println("🔗 Keeping driver alive for next chained test\n");
            skipNextTeardown = false;
            skipNextSetup = true; // Signal next test to skip setup
            return;
        }
        
        DriverManager.quitDriver();
        System.out.println("🧹 Test cleanup complete\n");
    }

    // ================================================================
    // LOGIN HELPER METHODS
    // ================================================================

    /**
     * Perform complete login flow with explicit waits
     */
    protected void performLogin() {
        System.out.println("🔐 Performing login...");
        
        // Enter company code - wait for login page to appear
        welcomePage.submitCompanyCode(AppConstants.VALID_COMPANY_CODE);
        loginPage.waitForPageReady();
        
        // Enter credentials - wait for site selection screen
        loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
        
        // Wait for Save Password popup to appear
        sleep(2000);
        
        // Handle Save Password alert if present (try multiple times)
        welcomePage.handleSavePasswordAlert();
        sleep(500);
        welcomePage.handleSavePasswordAlert();
        
        System.out.println("✅ Login completed");
    }

    /**
     * Login and navigate to site selection screen (dashboard)
     */
    protected void loginAndGoToDashboard() {
        performLogin();
        
        // Wait for site selection screen to be ready
        siteSelectionPage.waitForSiteListReady();
        
        System.out.println("✅ On Site Selection Screen");
    }

    /**
     * Login and select a site
     */
    protected void loginAndSelectSite() {
        loginAndGoToDashboard();
        
        // Select random site
        siteSelectionPage.selectRandomSite();
        
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
     * Log a step with screenshot
     */
    protected void logStepWithScreenshot(String stepDescription) {
        String screenshotPath = ScreenshotUtil.captureScreenshot(
            stepDescription.replaceAll("[^a-zA-Z0-9]", "_")
        );
        ExtentReportManager.logStepWithScreenshot(stepDescription, screenshotPath);
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
                java.time.Duration.ofMillis(milliseconds)
            ).until(d -> true);
        } catch (Exception e) {
            // Ignore timeout
        }
    }

    /**
     * Short wait (1 second) - CI-safe
     */
    protected void shortWait() {
        sleep(1000);
    }

    /**
     * Medium wait (2 seconds) - CI-safe
     */
    protected void mediumWait() {
        sleep(2000);
    }

    /**
     * Long wait (3 seconds) - CI-safe
     */
    protected void longWait() {
        sleep(3000);
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
