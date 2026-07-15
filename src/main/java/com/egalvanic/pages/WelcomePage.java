package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import com.egalvanic.constants.AppConstants;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/**
 * Welcome Page - Company Code Entry Screen
 */
public class WelcomePage extends BasePage {

    // ================================================================
    // PAGE ELEMENTS
    // ================================================================

    // Company Code Text Field
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField'")
    private WebElement companyCodeField;

    // Company Code Field with placeholder
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == '(e.g. acme.egalvanic)'")
    private WebElement companyCodeFieldWithPlaceholder;

    // Continue Button
    @iOSXCUITFindBy(accessibility = "Continue")
    private WebElement continueButton;

    // Welcome Text
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Welcome'")
    private WebElement welcomeText;

    // Error Message
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'not found'")
    private WebElement errorMessage;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public WelcomePage() {
        super();
    }

    // ================================================================
    // EXPLICIT WAIT METHODS
    // ================================================================
    
    /**
     * Wait for welcome page to be ready (explicit wait)
     */
    public void waitForPageReady() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(AppConstants.EXPLICIT_WAIT));
            wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOf(companyCodeField),
                ExpectedConditions.visibilityOf(companyCodeFieldWithPlaceholder)
            ));
            System.out.println("✅ Welcome page ready");
        } catch (Exception e) {
            System.out.println("⚠️ Welcome page wait timeout, continuing...");
        }
    }

    // ================================================================
    // PAGE METHODS
    // ================================================================

    /**
     * Check if page is loaded
     */
    public boolean isPageLoaded() {
        // Must also check Continue button to distinguish from Session Expired screen,
        // which also has a XCUIElementTypeTextField (email field) but no "Continue" button.
        boolean hasTextField = isElementDisplayed(companyCodeField) || isElementDisplayed(companyCodeFieldWithPlaceholder);
        if (!hasTextField) return false;
        return isElementDisplayed(continueButton);
    }

    /**
     * Check if company code field is displayed
     */
    public boolean isCompanyCodeFieldDisplayed() {
        return isElementDisplayed(companyCodeField);
    }

    /**
     * Check if continue button is displayed
     */
    public boolean isContinueButtonDisplayed() {
        return isElementDisplayed(continueButton);
    }

    /**
     * Check if continue button is enabled
     */
    public boolean isContinueButtonEnabled() {
        try {
            String enabled = continueButton.getAttribute("enabled");
            return "true".equalsIgnoreCase(enabled);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Enter company code with explicit wait (increased timeout for CI).
     *
     * The Welcome screen finishes laying out a beat after the field
     * becomes "clickable", and the SwiftUI text field occasionally
     * drops the first keystroke if we type immediately. A short sleep
     * before clicking + a brief settle after typing makes the entry
     * reliable on slower app builds.
     */
    public void enterCompanyCode(String companyCode) {
        // Pre-tap settle — let the Welcome screen fully render before we touch it
        sleepQuietly(1200);
        try {
            // Try with placeholder first
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.elementToBeClickable(companyCodeFieldWithPlaceholder));
            companyCodeFieldWithPlaceholder.click();
            sleepQuietly(300);
            companyCodeFieldWithPlaceholder.clear();
            companyCodeFieldWithPlaceholder.sendKeys(companyCode);
            sleepQuietly(400);
        } catch (Exception e) {
            // Fallback to generic text field
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
                wait.until(ExpectedConditions.elementToBeClickable(companyCodeField));
                companyCodeField.click();
                sleepQuietly(300);
                companyCodeField.clear();
                companyCodeField.sendKeys(companyCode);
                sleepQuietly(400);
            } catch (Exception e2) {
                // Direct click as last resort
                companyCodeField.click();
                sleepQuietly(300);
                companyCodeField.clear();
                companyCodeField.sendKeys(companyCode);
                sleepQuietly(400);
            }
        }
    }

    private static void sleepQuietly(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    /**
     * Clear company code field
     */
    public void clearCompanyCode() {
        try {
            companyCodeField.clear();
        } catch (Exception e) {
            // Field might already be cleared
        }
    }

    /**
     * Click continue button with explicit wait.
     * v1.50: element.click() on SwiftUI buttons is the documented silent
     * no-op family (Continue no-ops left flows stranded on Welcome — CI run
     * 29402715226 / local probe 2026-07-15). Coordinate press first.
     */
    public void clickContinue() {
        waitForClickable(continueButton);
        try {
            org.openqa.selenium.Rectangle r = continueButton.getRect();
            driver.executeScript("mobile: tap", java.util.Map.of(
                "x", r.x + r.width / 2, "y", r.y + r.height / 2));
        } catch (Exception e) {
            continueButton.click();
        }
    }

    /**
     * Submit company code.
     *
     * After tapping Continue the app shows a spinner while it validates
     * the code with the backend — observed to take up to 8 s on slow
     * builds. Sleep here gives the server response time to complete
     * and the transition to the Login screen to start, so the next
     * step (LoginPage.waitForPageReady) finds the email field
     * immediately instead of polling against a stale Welcome screen.
     */
    public void submitCompanyCode(String companyCode) {
        enterCompanyCode(companyCode);
        clickContinue();
        // Post-Continue wait — covers spinner + backend round-trip + screen transition
        sleepQuietly(3000);
        // Precise diagnosis: a stranded APP-LEVEL offline flag (persisted via
        // noReset when an offline test dies before its cleanup) surfaces here
        // as 'Failed to fetch company configuration' — every later click then
        // "fails" mysteriously (wave-3 bite 2026-07-14: 23 auth fails). Name
        // the real cause with a cheap 0-implicit probe.
        try {
            boolean stuckOffline = !withImplicitWait(0, () -> driver.findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND name CONTAINS 'Failed to fetch company configuration'"))
                    .isEmpty());
            if (stuckOffline) {
                throw new com.egalvanic.verify.VerificationError(
                    "submitCompanyCode: app shows 'Failed to fetch company configuration' — the app is "
                    + "stuck in APP-LEVEL OFFLINE mode (stranded by an earlier offline test; persisted via "
                    + "noReset). Reinstall the app or restore online mode; this is environment, not a locator bug.");
            }
        } catch (com.egalvanic.verify.VerificationError ve) {
            throw ve;
        } catch (Exception ignored) { }
    }

    /**
     * Check if error message is displayed
     */
    public boolean isErrorMessageDisplayed() {
        return isElementDisplayed(errorMessage);
    }
}
