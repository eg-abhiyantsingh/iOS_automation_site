package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import io.appium.java_client.AppiumBy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Security & Edge-Case Test Suite (adversarial / real-user input)
 *
 * Hits the auth surface with the inputs real users and attackers actually
 * produce: injection strings, oversized input, unicode/emoji, whitespace,
 * double-taps, and app backgrounding mid-flow. Every test carries hard
 * assertions (no pass-anyway): the app must stay alive (no crash), must not
 * grant access on bad credentials, and must not blank-screen.
 *
 * Runs in the auth module context: app starts logged OUT on the Welcome
 * screen, and BaseTest's per-test soft restart returns it there.
 */
public final class Security_EdgeCase_Test extends BaseTest {

    private static final String FEATURE_SECURITY = "Security & Edge Cases";

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        System.out.println("\n🛡️ Security & Edge-Case Test Suite - Starting");
    }

    // ------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------

    /** Zero-ish-wait dashboard probe — true only if we actually got in. */
    private boolean isOnDashboard() {
        var d = DriverManager.getDriver();
        try {
            d.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
            return !d.findElements(AppiumBy.accessibilityId("building.2")).isEmpty();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                d.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
            } catch (Exception ignored) {}
        }
    }

    /** Welcome screen has no SecureTextField — its presence means we reached Login. */
    private boolean reachedLoginScreen() {
        return loginPage.isPasswordFieldDisplayed();
    }

    private void goToLoginScreen() {
        welcomePage.submitCompanyCode(AppConstants.VALID_COMPANY_CODE);
        loginPage.waitForPageReadyFast();
    }

    // ------------------------------------------------------------
    // Company-code surface
    // ------------------------------------------------------------

    @Test(priority = 1)
    public void TC_SEC_001_sqlInjectionInCompanyCodeIsRejected() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_001 - SQL-injection string in company code is rejected without crash");

        logStep("Submitting SQL-injection payload as company code");
        welcomePage.submitCompanyCode("' OR '1'='1' --");
        mediumWait();

        verifyAppAlive("after SQL-injection company code");
        verifyNotBlank("Welcome screen");
        assertFalse(reachedLoginScreen(),
            "Injection payload must NOT be accepted as a valid company code (login page reached)");
        logStepWithScreenshot("Injection payload rejected, app healthy");
    }

    @Test(priority = 2)
    public void TC_SEC_002_xssPayloadInCompanyCodeIsRejected() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_002 - XSS payload in company code is rejected without crash");

        logStep("Submitting XSS payload as company code");
        welcomePage.submitCompanyCode("<script>alert(1)</script>");
        mediumWait();

        verifyAppAlive("after XSS company code");
        verifyNotBlank("Welcome screen");
        assertFalse(reachedLoginScreen(),
            "XSS payload must NOT be accepted as a valid company code");
        logStepWithScreenshot("XSS payload rejected, app healthy");
    }

    @Test(priority = 3)
    public void TC_SEC_003_oversizedCompanyCodeHandledGracefully() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_003 - 512-char company code neither crashes nor authenticates");

        logStep("Submitting 512-character company code");
        welcomePage.submitCompanyCode("x".repeat(512));
        mediumWait();

        verifyAppAlive("after oversized company code");
        verifyNotBlank("Welcome screen");
        assertFalse(reachedLoginScreen(),
            "Oversized garbage code must NOT reach the login page");
        logStepWithScreenshot("Oversized input handled gracefully");
    }

    // ------------------------------------------------------------
    // Login surface
    // ------------------------------------------------------------

    @Test(priority = 4)
    public void TC_SEC_004_sqlInjectionLoginDoesNotAuthenticate() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_004 - SQL-injection credentials must not authenticate");

        logStep("Navigating to login screen");
        goToLoginScreen();

        logStep("Submitting SQL-injection credentials");
        loginPage.login("admin'--@egalvanic.com", "' OR '1'='1");
        mediumWait();

        verifyAppAlive("after SQL-injection login attempt");
        assertFalse(isOnDashboard(),
            "SECURITY: SQL-injection credentials must NEVER reach the dashboard");
        assertTrue(reachedLoginScreen() || loginPage.isPageLoaded(),
            "App should remain on the login screen after rejected credentials");
        logStepWithScreenshot("Injection login rejected");
    }

    @Test(priority = 5)
    public void TC_SEC_005_xssPayloadInEmailHandledSafely() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_005 - XSS payload in email field handled safely");

        logStep("Navigating to login screen");
        goToLoginScreen();

        logStep("Entering XSS payload as email");
        loginPage.enterEmail("<img src=x onerror=alert(1)>@test.com");
        loginPage.enterPassword(AppConstants.INVALID_PASSWORD);
        loginPage.tapSignIn();
        mediumWait();

        verifyAppAlive("after XSS email login attempt");
        verifyNotBlank("Login screen");
        assertFalse(isOnDashboard(), "XSS-email credentials must not authenticate");
        logStepWithScreenshot("XSS email handled safely");
    }

    @Test(priority = 6)
    public void TC_SEC_006_oversizedCredentialsDoNotCrash() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_006 - 512-char email + password neither crash nor authenticate");

        logStep("Navigating to login screen");
        goToLoginScreen();

        logStep("Entering 512-char email and password");
        loginPage.enterEmail("a".repeat(500) + "@x.com");
        loginPage.enterPassword("p".repeat(512));
        loginPage.tapSignIn();
        mediumWait();

        verifyAppAlive("after oversized credentials");
        assertFalse(isOnDashboard(), "Oversized garbage credentials must not authenticate");
        logStepWithScreenshot("Oversized credentials handled");
    }

    @Test(priority = 7)
    public void TC_SEC_007_unicodeEmojiInputHandledSafely() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_007 - Emoji / RTL / zero-width unicode input handled safely");

        logStep("Navigating to login screen");
        goToLoginScreen();

        logStep("Entering emoji + RTL + zero-width-joiner email");
        loginPage.enterEmail("⚡🔌‍‮user@test.com");
        loginPage.enterPassword("päss😀word");
        mediumWait();

        verifyAppAlive("after unicode input");
        verifyNotBlank("Login screen");
        assertTrue(reachedLoginScreen() || loginPage.isPageLoaded(),
            "Login screen should survive unicode input intact");
        logStepWithScreenshot("Unicode input handled safely");
    }

    @Test(priority = 8)
    public void TC_SEC_008_whitespaceOnlyCredentialsRejected() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_008 - Whitespace-only credentials are rejected");

        logStep("Navigating to login screen");
        goToLoginScreen();

        logStep("Entering whitespace-only credentials");
        loginPage.enterEmail("   ");
        loginPage.enterPassword("   ");
        mediumWait();

        boolean signInEnabled = loginPage.isSignInButtonEnabled();
        if (signInEnabled) {
            loginPage.tapSignIn();
            mediumWait();
        }

        verifyAppAlive("after whitespace-only credentials");
        assertFalse(isOnDashboard(),
            "Whitespace-only credentials must not authenticate (enabled=" + signInEnabled + ")");
        logStepWithScreenshot("Whitespace-only credentials rejected");
    }

    @Test(priority = 9)
    public void TC_SEC_009_doubleTapSignInDoesNotBreakApp() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_009 - Rapid double-tap on Sign In neither crashes nor double-navigates");

        logStep("Navigating to login screen");
        goToLoginScreen();

        logStep("Entering invalid credentials and double-tapping Sign In");
        loginPage.enterEmail(AppConstants.INVALID_EMAIL);
        loginPage.enterPassword(AppConstants.INVALID_PASSWORD);
        loginPage.tapSignIn();
        try {
            loginPage.tapSignIn(); // immediate second tap — real users do this
        } catch (Exception e) {
            logStep("Second tap rejected by app (button disabled) — acceptable: " + e.getMessage());
        }
        mediumWait();

        verifyAppAlive("after double-tap Sign In");
        verifyNotBlank("Login screen");
        assertFalse(isOnDashboard(), "Invalid credentials must not authenticate even when double-submitted");
        logStepWithScreenshot("Double-tap handled safely");
    }

    @Test(priority = 10)
    public void TC_SEC_010_backgroundForegroundMidLoginPreservesScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_AUTHENTICATION, FEATURE_SECURITY,
            "TC_SEC_010 - Backgrounding the app mid-login restores a working login screen");

        logStep("Navigating to login screen and filling email");
        goToLoginScreen();
        loginPage.enterEmail(AppConstants.INVALID_EMAIL);

        logStep("Backgrounding app for 3 seconds (incoming call / app switch)");
        DriverManager.getDriver().runAppInBackground(Duration.ofSeconds(3));
        mediumWait();

        verifyAppAlive("after background/foreground cycle");
        verifyNotBlank("Login screen after foregrounding");
        assertTrue(reachedLoginScreen() || loginPage.isPageLoaded() || welcomePage.isPageLoaded(),
            "App must return to a working auth screen after backgrounding, not a dead/blank state");
        logStepWithScreenshot("Background/foreground cycle survived");
    }
}
