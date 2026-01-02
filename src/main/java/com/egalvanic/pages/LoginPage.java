package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import com.egalvanic.constants.AppConstants;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Login Page - Email and Password Entry Screen
 */
public class LoginPage extends BasePage {

    // ================================================================
    // PAGE ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND visible == 1")
    private WebElement emailField;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeSecureTextField'")
    private WebElement passwordField;

    @iOSXCUITFindBy(accessibility = "Sign In")
    private WebElement signInButton;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Sign' OR label CONTAINS 'Login')")
    private WebElement signInButtonAlt;

    @iOSXCUITFindBy(accessibility = "Show Password")
    private WebElement showPasswordIcon;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public LoginPage() {
        super();
    }

    // ================================================================
    // EXPLICIT WAIT METHODS
    // ================================================================
    
    /**
     * Wait for login page to be ready (email and password fields visible)
     */
    public void waitForPageReady() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(AppConstants.EXPLICIT_WAIT));
            wait.until(ExpectedConditions.and(
                ExpectedConditions.visibilityOf(emailField),
                ExpectedConditions.visibilityOf(passwordField)
            ));
            System.out.println("✅ Login page ready");
        } catch (Exception e) {
            System.out.println("⚠️ Login page wait timeout, continuing...");
        }
    }

    // ================================================================
    // PAGE METHODS
    // ================================================================

    public boolean isPageLoaded() {
        return isElementDisplayed(emailField) && isElementDisplayed(passwordField);
    }

    public boolean isEmailFieldDisplayed() {
        return isElementDisplayed(emailField);
    }

    public boolean isPasswordFieldDisplayed() {
        return isElementDisplayed(passwordField);
    }

    public boolean isSignInButtonDisplayed() {
        return isElementDisplayed(signInButton);
    }

    public boolean isSignInButtonEnabled() {
        try {
            String enabled = signInButton.getAttribute("enabled");
            return "true".equalsIgnoreCase(enabled);
        } catch (Exception e) {
            return false;
        }
    }

    public void enterEmail(String email) {
        click(emailField);
        emailField.sendKeys(email);
    }

    public void enterPassword(String password) {
        click(passwordField);
        passwordField.sendKeys(password);
    }

    public void clearEmail() {
        emailField.clear();
    }

    public void clearPassword() {
        passwordField.clear();
    }

   /**
 * Click show password toggle - FIXED VERSION
 */
public void clickShowPassword() {
    try {
        showPasswordIcon.click();
    } catch (Exception e1) {
        try {
            // Try alternative locator
            WebElement showBtn = driver.findElement(
                org.openqa.selenium.By.xpath("//XCUIElementTypeButton[contains(@name, 'Show') or contains(@name, 'eye') or contains(@label, 'Show')]")
            );
            showBtn.click();
        } catch (Exception e2) {
            try {
                // Try by accessibility
                WebElement showBtn = driver.findElement(
                    org.openqa.selenium.By.xpath("//XCUIElementTypeButton[contains(@name, 'password') or contains(@label, 'password')]")
                );
                showBtn.click();
            } catch (Exception e3) {
                System.out.println("Show password button not found, skipping...");
            }
        }
    }
}

    /**
     * Dismiss the keyboard to prevent click issues - Using W3C Actions
     */
    public void dismissKeyboard() {
        try {
            driver.hideKeyboard();
            shortWait();
        } catch (Exception e) {
            try {
                driver.hideKeyboard("Done");
                shortWait();
            } catch (Exception ex1) {
                try {
                    driver.hideKeyboard("Return");
                    shortWait();
                } catch (Exception ex2) {
                    try {
                        driver.hideKeyboard("Go");
                        shortWait();
                    } catch (Exception ex3) {
                        try {
                            // Use W3C Actions instead of deprecated TouchAction
                            performTap(100, 100);
                            shortWait();
                        } catch (Exception ex4) {
                            System.out.println("Could not dismiss keyboard, continuing test...");
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Perform tap using W3C Actions (replacement for deprecated TouchAction)
     */
    private void performTap(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1)
            .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    /**
     * Tap Sign In button - Multiple approaches for reliability
     */
    public void tapSignIn() {
        dismissKeyboard();
        shortWait();
        
        // Approach 1: Direct click
        try {
            signInButton.click();
            return;
        } catch (Exception e1) {
            try {
                signInButtonAlt.click();
                return;
            } catch (Exception e2) {
                // Approach 2: JavaScript executor
                try {
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", signInButton);
                    return;
                } catch (Exception e3) {
                    try {
                        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", signInButtonAlt);
                        return;
                    } catch (Exception e4) {
                        // Approach 3: Find by XPath
                        try {
                            WebElement button = driver.findElement(
                                org.openqa.selenium.By.xpath("//XCUIElementTypeButton[contains(@label, 'Sign') or contains(@label, 'Login') or contains(@label, 'Log') or contains(@name, 'Sign') or contains(@name, 'Login') or contains(@name, 'Log')]")
                            );
                            button.click();
                            return;
                        } catch (Exception e5) {
                            // Approach 4: Tap by coordinates using W3C Actions
                            try {
                                List<WebElement> allButtons = driver.findElements(
                                    org.openqa.selenium.By.xpath("//XCUIElementTypeButton")
                                );
                                for (WebElement button : allButtons) {
                                    String label = button.getAttribute("label");
                                    if (label != null && (label.toLowerCase().contains("sign") || 
                                        label.toLowerCase().contains("login") || 
                                        label.toLowerCase().contains("log"))) {
                                        int centerX = button.getLocation().getX() + (button.getSize().getWidth() / 2);
                                        int centerY = button.getLocation().getY() + (button.getSize().getHeight() / 2);
                                        performTap(centerX, centerY);
                                        return;
                                    }
                                }
                            } catch (Exception e6) {
                                throw new RuntimeException("Unable to click sign-in button using any method", e6);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Click Sign In - calls tapSignIn()
     */
    public void clickSignIn() {
        tapSignIn();
    }

    /**
     * Login with credentials - USES YOUR WORKING tapSignIn()
     */
    public void login(String email, String password) {
        enterEmail(email);
        shortWait();
        enterPassword(password);
        shortWait();
        tapSignIn();  // Uses your working method
        
        // Handle any alerts that might appear after login (CI-safe)
        try {
            // Use explicit wait for alert instead of Thread.sleep
            WebDriverWait alertWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            alertWait.until(ExpectedConditions.alertIsPresent());
            handleSavePasswordAlert();
        } catch (Exception e) {
            // No alert appeared - this is normal
        }
    }
}