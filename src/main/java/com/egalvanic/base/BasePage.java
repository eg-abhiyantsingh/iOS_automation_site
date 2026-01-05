package com.egalvanic.base;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Function;

/**
 * Base Page - Parent class for all Page Objects
 * Implements PageFactory with AjaxElementLocatorFactory for lazy loading
 * Enhanced with better wait strategies for CI environments
 */
public abstract class BasePage {

    protected IOSDriver driver;
    protected WebDriverWait wait;
    protected FluentWait<IOSDriver> fluentWait;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public BasePage() {
        this.driver = DriverManager.getDriver();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(AppConstants.EXPLICIT_WAIT));
        
        // Initialize FluentWait for more flexible waiting
        this.fluentWait = new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(AppConstants.EXPLICIT_WAIT))
            .pollingEvery(Duration.ofMillis(500))
            .ignoring(NoSuchElementException.class)
            .ignoring(StaleElementReferenceException.class);
        
        // Initialize PageFactory with AjaxElementLocatorFactory for lazy loading
        PageFactory.initElements(
            new AppiumFieldDecorator(driver, Duration.ofSeconds(AppConstants.AJAX_TIMEOUT)),
            this
        );
    }

    // ================================================================
    // ENHANCED WAIT METHODS
    // ================================================================

    /**
     * Wait for element to be visible with custom timeout
     */
    protected WebElement waitForVisibility(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    /**
     * Wait for element to be visible with custom timeout
     */
    protected WebElement waitForVisibility(WebElement element, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return customWait.until(ExpectedConditions.visibilityOf(element));
    }

    /**
     * Wait for element to be clickable
     */
    protected WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Wait for element to be clickable with custom timeout
     */
    protected WebElement waitForClickable(WebElement element, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return customWait.until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Wait for element to be invisible
     */
    protected boolean waitForInvisibility(WebElement element) {
        try {
            return wait.until(ExpectedConditions.invisibilityOf(element));
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Wait for element to be present by locator
     */
    protected WebElement waitForPresence(By locator, int timeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return customWait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }
    
    /**
     * Fluent wait for custom condition
     */
    protected <T> T fluentWaitFor(Function<IOSDriver, T> condition) {
        return fluentWait.until(condition);
    }

    /**
     * Wait for element with retry logic
     */
    protected boolean waitForElementWithRetry(WebElement element, int maxAttempts, int waitBetweenAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                if (element.isDisplayed()) {
                    return true;
                }
            } catch (Exception e) {
                System.out.println("⚠️ Attempt " + (i + 1) + " failed, retrying...");
            }
            sleep(waitBetweenAttempts);
        }
        return false;
    }

    // ================================================================
    // ACTION METHODS WITH RETRY LOGIC
    // ================================================================

    /**
     * Click element with wait and retry logic
     */
    protected void click(WebElement element) {
        int maxRetries = 3;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                waitForClickable(element, 10).click();
                return; // Success
            } catch (StaleElementReferenceException e) {
                lastException = e;
                System.out.println("⚠️ Stale element on click attempt " + attempt + ", retrying...");
                sleep(500);
            } catch (Exception e) {
                lastException = e;
                System.out.println("⚠️ Click failed on attempt " + attempt + ": " + e.getMessage());
                sleep(500);
            }
        }
        
        throw new RuntimeException("Failed to click element after " + maxRetries + " attempts", lastException);
    }

    /**
     * Click element with custom timeout
     */
    protected void click(WebElement element, int timeoutSeconds) {
        try {
            waitForClickable(element, timeoutSeconds).click();
        } catch (StaleElementReferenceException e) {
            // Retry once if stale
            sleep(500);
            waitForClickable(element, timeoutSeconds).click();
        }
    }

    /**
     * Send keys with wait and clear
     */
    protected void sendKeys(WebElement element, String text) {
        waitForVisibility(element);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Get text from element
     */
    protected String getText(WebElement element) {
        try {
            return waitForVisibility(element).getText();
        } catch (Exception e) {
            return element.getAttribute("value");
        }
    }

    /**
     * Get attribute from element
     */
    protected String getAttribute(WebElement element, String attribute) {
        try {
            return waitForVisibility(element).getAttribute(attribute);
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // VERIFICATION METHODS
    // ================================================================

    /**
     * Check if element is displayed (with short wait)
     */
    protected boolean isElementDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is enabled
     */
    protected boolean isElementEnabled(WebElement element) {
        try {
            return element.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is selected
     */
    protected boolean isElementSelected(WebElement element) {
        try {
            return element.isSelected();
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // UTILITY METHODS (CI-safe explicit waits)
    // ================================================================

    /**
     * Wait for a condition to be true (generic explicit wait)
     */
    protected boolean waitForCondition(java.util.function.Supplier<Boolean> condition, int timeoutSeconds) {
        try {
            WebDriverWait conditionWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            return conditionWait.until(d -> condition.get());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Short wait - uses explicit wait polling (CI-safe)
     */
    protected void shortWait() {
        new WebDriverWait(driver, Duration.ofSeconds(1))
            .until(d -> true);
    }

    /**
     * Medium wait - uses explicit wait polling (CI-safe)
     */
    protected void mediumWait() {
        new WebDriverWait(driver, Duration.ofSeconds(2))
            .until(d -> true);
    }

    /**
     * Long wait - uses explicit wait polling (CI-safe)
     */
    protected void longWait() {
        new WebDriverWait(driver, Duration.ofSeconds(3))
            .until(d -> true);
    }

    /**
     * Custom wait - uses explicit wait polling (CI-safe)
     */
    protected void sleep(int milliseconds) {
        new WebDriverWait(driver, Duration.ofMillis(milliseconds))
            .until(d -> true);
    }

    /**
     * Scroll down
     */
    protected void scrollDown() {
        // iOS scroll implementation
        // Can be extended based on specific needs
    }

    /**
     * Scroll up
     */
    protected void scrollUp() {
        // iOS scroll implementation
        // Can be extended based on specific needs
    }

    /**
     * Get driver instance
     */
    protected IOSDriver getDriver() {
        return driver;
    }

    /**
     * Handle iOS alerts including Save Password prompt (CI-safe)
     */
    protected void handleAlert() {
        try {
            // Wait for alert using explicit wait (CI-safe)
            WebDriverWait alertWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            alertWait.until(ExpectedConditions.alertIsPresent());
            
            // Accept the alert (for "Save Password" etc.)
            driver.switchTo().alert().accept();
            System.out.println("✅ Alert handled successfully");
        } catch (Exception e) {
            System.out.println("⚠️ No alert to handle");
        }
    }
    
    /**
     * Handle Save Password alert specifically
     * Tries multiple approaches to dismiss the popup
     */
    protected void handleSavePasswordAlert() {
        System.out.println("🔍 Looking for Save Password popup...");
        
        // Quick check - try alert first (fastest path)
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("✅ Alert dismissed");
            return;
        } catch (Exception e) {
            // No system alert - continue with other checks
        }
        
        // All possible button names for iOS Save Password popup
        String[] buttonNames = {
            "Not Now", "not now", "NOT NOW",
            "Don't Save", "Dont Save", "Don't save",
            "Never for This Website",
            "Cancel", "cancel"
        };
        
        for (String btnName : buttonNames) {
            try {
                org.openqa.selenium.WebElement btn = driver.findElement(
                    io.appium.java_client.AppiumBy.accessibilityId(btnName)
                );
                btn.click();
                System.out.println("✅ Clicked: " + btnName);
                return;
            } catch (Exception e) {}
        }
        
        // Try finding any button with "Not" in the name
        try {
            org.openqa.selenium.WebElement btn = driver.findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name CONTAINS 'Not' OR label CONTAINS 'Not')"
                )
            );
            btn.click();
            System.out.println("✅ Clicked button containing 'Not'");
            return;
        } catch (Exception e) {
            // No popup found - this is normal
        }
        
        System.out.println("⚠️ No Save Password popup found");
    }

    /**
     * Handle Save Password alert - Robust version
     * Tries multiple strategies to dismiss the popup
     */
    public void handleSavePasswordAlertFast() {
        System.out.println("🔍 Checking for Save Password popup...");
        
        // Strategy 1: Try system alert dismiss
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("✅ System alert dismissed");
            return;
        } catch (Exception e) {}
        
        // Strategy 2: Try system alert accept (sometimes works better)
        try {
            driver.switchTo().alert().accept();
            System.out.println("✅ System alert accepted");
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Try "Not Now" button (most common for Save Password)
        try {
            org.openqa.selenium.WebElement btn = driver.findElement(
                io.appium.java_client.AppiumBy.accessibilityId("Not Now")
            );
            btn.click();
            System.out.println("✅ Clicked: Not Now");
            return;
        } catch (Exception e) {}
        
        // Strategy 4: Try "Don't Save" or similar buttons
        String[] buttonNames = {"Don't Save", "Dont Save", "Cancel", "Never", "No"};
        for (String btnName : buttonNames) {
            try {
                org.openqa.selenium.WebElement btn = driver.findElement(
                    io.appium.java_client.AppiumBy.accessibilityId(btnName)
                );
                btn.click();
                System.out.println("✅ Clicked: " + btnName);
                return;
            } catch (Exception e) {}
        }
        
        // Strategy 5: Try finding button by predicate containing "Not" or "Don't"
        try {
            org.openqa.selenium.WebElement btn = driver.findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Not' OR label CONTAINS \"Don't\" OR label CONTAINS 'Cancel')"
                )
            );
            btn.click();
            System.out.println("✅ Clicked dismiss button via predicate");
            return;
        } catch (Exception e) {}
        
        // No popup found - this is normal
        System.out.println("ℹ️ No Save Password popup detected");
    }

    /**
     * Check if an alert is present
     */
    private boolean isAlertPresent() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
