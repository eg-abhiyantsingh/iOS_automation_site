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
        return isElementDisplayed(companyCodeField) || isElementDisplayed(companyCodeFieldWithPlaceholder);
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
     * Enter company code with explicit wait (increased timeout for CI)
     */
    public void enterCompanyCode(String companyCode) {
        try {
            // Try with placeholder first - use longer timeout for CI
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.elementToBeClickable(companyCodeFieldWithPlaceholder));
            companyCodeFieldWithPlaceholder.click();
            companyCodeFieldWithPlaceholder.sendKeys(companyCode);
        } catch (Exception e) {
            // Fallback to generic text field
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.elementToBeClickable(companyCodeField));
            companyCodeField.click();
            companyCodeField.sendKeys(companyCode);
        }
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
     * Click continue button with explicit wait
     */
    public void clickContinue() {
        waitForClickable(continueButton);
        continueButton.click();
    }

    /**
     * Submit company code (no hardcoded sleep)
     */
    public void submitCompanyCode(String companyCode) {
        enterCompanyCode(companyCode);
        clickContinue();
    }

    /**
     * Check if error message is displayed
     */
    public boolean isErrorMessageDisplayed() {
        return isElementDisplayed(errorMessage);
    }
}
