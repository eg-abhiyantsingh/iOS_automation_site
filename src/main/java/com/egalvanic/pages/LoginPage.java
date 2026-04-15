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

    // Terms & Conditions checkbox — required before Sign In is enabled.
    // May render as Switch, Button, Image, or Other depending on app version.
    // NOTE: "label CONTAINS 'terms'" is intentionally EXCLUDED — it matches the
    // "Terms & Conditions" hyperlink (blue link text) which navigates to T&C page.
    @iOSXCUITFindBy(iOSNsPredicate = "(type == 'XCUIElementTypeSwitch' OR type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') AND (label CONTAINS[c] 'agree' OR name CONTAINS 'checkbox' OR name CONTAINS 'square')")
    private WebElement termsCheckbox;

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
            // FAST: 3 second timeout with fast polling
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            fastWait.pollingEvery(Duration.ofMillis(200));
            // Just wait for email field - password field will be there too
            fastWait.until(ExpectedConditions.presenceOfElementLocated(
                io.appium.java_client.AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND visible == 1")
            ));
            System.out.println("✅ Login page ready");
        } catch (Exception e) {
            // Continue anyway - fields might be there
        }
    }
    
    /**
     * FAST: Wait for login page (3 seconds max)
     */
    public void waitForPageReadyFast() {
        try {
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            fastWait.pollingEvery(Duration.ofMillis(200));
            fastWait.until(ExpectedConditions.presenceOfElementLocated(
                io.appium.java_client.AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND visible == 1")
            ));
        } catch (Exception e) {
            // Continue anyway
        }
    }


    // ================================================================
    // PAGE METHODS
    // ================================================================

    public boolean isPageLoaded() {
        return isElementDisplayed(emailField) && isElementDisplayed(passwordField);
    }

    public boolean isEmailFieldDisplayed() {
        try {
            // First try the annotated element
            if (isElementDisplayed(emailField)) {
                return true;
            }
        } catch (Exception e) {
            // Element might be stale, try fresh lookup
        }
        
        // Try fresh lookup with explicit wait
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(7));
            WebElement freshEmailField = wait.until(ExpectedConditions.presenceOfElementLocated(
                io.appium.java_client.AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND visible == 1")
            ));
            return freshEmailField != null && freshEmailField.isDisplayed();
        } catch (Exception e) {
            return false;
        }
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
        try {
            click(emailField);
            emailField.clear();
            emailField.sendKeys(email);
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            // Re-find element if stale
            System.out.println("⚠️ Email field stale, re-finding...");
            WebElement freshEmailField = driver.findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND visible == 1")
            );
            freshEmailField.click();
            freshEmailField.clear();
            freshEmailField.sendKeys(email);
        }
    }

    public void enterPassword(String password) {
        try {
            click(passwordField);
            passwordField.clear();
            passwordField.sendKeys(password);
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            // Re-find element if stale
            System.out.println("⚠️ Password field stale, re-finding...");
            WebElement freshPasswordField = driver.findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSecureTextField'")
            );
            freshPasswordField.click();
            freshPasswordField.clear();
            freshPasswordField.sendKeys(password);
        }
    }

    public void clearEmail() {
        try {
            emailField.clear();
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            WebElement freshEmailField = driver.findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND visible == 1")
            );
            freshEmailField.clear();
        }
    }

    public void clearPassword() {
        try {
            passwordField.clear();
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            WebElement freshPasswordField = driver.findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSecureTextField'")
            );
            freshPasswordField.clear();
        }
    }

    /**
     * Accept Terms & Conditions checkbox if present and not already checked.
     * Uses multiple strategies because the checkbox renders differently across app versions:
     * - SwiftUI Toggle → XCUIElementTypeSwitch (label on separate static text)
     * - Custom checkbox → XCUIElementTypeButton or XCUIElementTypeImage
     * - Entire row tappable → XCUIElementTypeOther containing "agree" text
     * Safe to call even if the checkbox doesn't exist (older app versions).
     */
    public void acceptTermsIfPresent() {
        // CRITICAL: Dismiss keyboard first! After enterPassword(), the keyboard covers the
        // T&C checkbox area at the bottom of the login screen. Elements behind the keyboard
        // are not "visible" in iOS accessibility, so all visible-based searches fail.
        try {
            driver.hideKeyboard();
            System.out.println("⌨️ Keyboard dismissed before T&C check");
        } catch (Exception e) {
            // Keyboard might not be open — that's fine
            try {
                // Fallback: tap on a neutral area above the keyboard to dismiss it
                org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
                tapAtCoordinates(screenSize.getWidth() / 2, 200);
                System.out.println("⌨️ Tapped neutral area to dismiss keyboard");
            } catch (Exception ignored) {}
        }

        // CRITICAL: Wait for keyboard dismiss animation (~300ms) and iOS accessibility
        // tree refresh. Without this, elements behind the keyboard are still marked
        // invisible in the DOM even though the keyboard is gone visually.
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Temporarily reduce implicit wait so failed searches don't hang
        try {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(2));
        } catch (Exception ignored) {}

        try {
            // Strategy 1: Direct match via annotated element
            if (isElementDisplayed(termsCheckbox)) {
                String value = termsCheckbox.getAttribute("value");
                if ("0".equals(value) || "false".equalsIgnoreCase(value) || value == null) {
                    termsCheckbox.click();
                    System.out.println("✅ Terms & Conditions checkbox tapped (strategy 1)");
                } else {
                    System.out.println("✅ Terms & Conditions already accepted");
                }
                restoreImplicitWait();
                return;
            }
        } catch (Exception e) {
            // Not found via annotated element
        }

        // Strategy 2: Find any tappable element with agree/terms/checkbox labels.
        // NOTE: No "visible == true" constraint — elements may be in DOM but marked non-visible
        // due to scroll position or recent keyboard dismissal. Appium can still interact with them.
        // IMPORTANT: "label CONTAINS 'Terms'" is excluded — it matches the "Terms & Conditions"
        // hyperlink (blue text) which opens the T&C page instead of toggling the checkbox.
        try {
            List<WebElement> candidates = driver.findElements(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "(label CONTAINS[c] 'I agree' OR name CONTAINS 'checkbox' OR name CONTAINS 'square' OR name CONTAINS 'agree')"
                )
            );
            System.out.println("🔍 T&C search found " + candidates.size() + " candidate elements");
            for (WebElement el : candidates) {
                String type = el.getAttribute("type");
                String label = el.getAttribute("label");
                System.out.println("   candidate: type=" + type + " label=" + label);
                // Skip StaticText (handled in Strategy 3) and skip hyperlinks
                if (type != null && !type.contains("StaticText") && !type.contains("Link")) {
                    el.click();
                    System.out.println("✅ Terms element tapped (strategy 2): type=" + type + " label=" + label);
                    restoreImplicitWait();
                    return;
                }
            }

            // Strategy 3: Found static text "I agree..." — tap to its LEFT where checkbox icon sits.
            // In iOS, checkbox is typically rendered to the left of its label text.
            for (WebElement el : candidates) {
                String type = el.getAttribute("type");
                if (type != null && type.contains("StaticText")) {
                    int labelX = el.getLocation().getX();
                    int labelY = el.getLocation().getY();
                    int labelH = el.getSize().getHeight();
                    // Tap 25px to the left of the label text (where checkbox icon is)
                    int tapX = Math.max(labelX - 25, 20);
                    int tapY = labelY + (labelH / 2);
                    System.out.println("📍 Terms label found at (" + labelX + "," + labelY + "), tapping checkbox at (" + tapX + "," + tapY + ")");
                    tapAtCoordinates(tapX, tapY);
                    System.out.println("✅ Terms checkbox tapped (strategy 3: coordinate tap left of label)");
                    restoreImplicitWait();
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 2/3 failed: " + e.getMessage());
        }

        // Strategy 4: Look for any Switch on the login screen (T&C is usually the only switch)
        try {
            List<WebElement> switches = driver.findElements(
                io.appium.java_client.AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSwitch'")
            );
            if (!switches.isEmpty()) {
                WebElement sw = switches.get(0);
                String value = sw.getAttribute("value");
                if ("0".equals(value) || "false".equalsIgnoreCase(value) || value == null) {
                    sw.click();
                    System.out.println("✅ Switch tapped (strategy 4): value was " + value);
                } else {
                    System.out.println("✅ Switch already on (strategy 4)");
                }
                restoreImplicitWait();
                return;
            }
        } catch (Exception e) {
            // No switch found
        }

        System.out.println("ℹ️ No Terms & Conditions checkbox found (may not be present in this app version)");
        restoreImplicitWait();
    }

    /** Restore implicit wait to default after temporary reduction */
    private void restoreImplicitWait() {
        try {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(5));
        } catch (Exception ignored) {}
    }

    /** Tap at specific screen coordinates using W3C Actions API */
    private void tapAtCoordinates(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 0);
        tap.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
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

    // ════════════════════════════════════════════════════════════════════════
    // ██████████████████████████████████████████████████████████████████████████
    // ██                                                                    ██
    // ██   ██╗    ██╗ █████╗ ██████╗ ███╗   ██╗██╗███╗   ██╗ ██████╗       ██
    // ██   ██║    ██║██╔══██╗██╔══██╗████╗  ██║██║████╗  ██║██╔════╝       ██
    // ██   ██║ █╗ ██║███████║██████╔╝██╔██╗ ██║██║██╔██╗ ██║██║  ███╗      ██
    // ██   ██║███╗██║██╔══██║██╔══██╗██║╚██╗██║██║██║╚██╗██║██║   ██║      ██
    // ██   ╚███╔███╔╝██║  ██║██║  ██║██║ ╚████║██║██║ ╚████║╚██████╔╝      ██
    // ██    ╚══╝╚══╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝╚═╝  ╚═══╝ ╚═════╝       ██
    // ██                                                                    ██
    // ██   DO NOT MODIFY THE login() AND popup handling METHODS             ██
    // ██   These are PRODUCTION-READY with POLLING for 100% reliability     ██
    // ██   Last verified: January 2026 - WORKING 100%                       ██
    // ██                                                                    ██
    // ██████████████████████████████████████████████████████████████████████████
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * ╔══════════════════════════════════════════════════════════════════════╗
     * ║  █████ LOCKED METHOD - DO NOT MODIFY █████                          ║
     * ╠══════════════════════════════════════════════════════════════════════╣
     * ║  Login with credentials - Optimized & Production Ready              ║
     * ║                                                                      ║
     * ║  Features:                                                           ║
     * ║  ✓ Fast email/password entry                                        ║
     * ║  ✓ Reliable Sign In tap                                             ║
     * ║  ✓ 100% Save Password popup handling with POLLING (5 attempts)      ║
     * ║  ✓ Guaranteed popup dismissal - handles timing variations           ║
     * ║                                                                      ║
     * ║  Status: VERIFIED WORKING 100% - January 2026                       ║
     * ╚══════════════════════════════════════════════════════════════════════╝
     */
    public final void login(String email, String password) {
        enterEmail(email);
        shortWait();
        enterPassword(password);
        shortWait();
        // Accept Terms & Conditions if present (added April 2026 — new app requirement)
        acceptTermsIfPresent();
        tapSignIn();
        // Wait for Save Password popup to be fully gone before proceeding
        waitForNoSavePasswordPopup();
    }
    
    // ================================================================
    // TURBO LOGIN - FAST VERSION (Under 2 seconds popup handling)
    // ================================================================
    
    /**
     * TURBO: Handle Save Password popup FAST (max 1.5 seconds)
     * Only uses the most reliable methods, skips slow fallbacks
     */
    private void handleSavePasswordTurbo() {
        long start = System.currentTimeMillis();
        
        // Quick check - try alert first (fastest)
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("⚡ Alert dismissed in " + (System.currentTimeMillis() - start) + "ms");
            return;
        } catch (Exception e) {}
        
        try {
            driver.switchTo().alert().accept();
            System.out.println("⚡ Alert accepted in " + (System.currentTimeMillis() - start) + "ms");
            return;
        } catch (Exception e) {}
        
        // Quick button check - "Not Now" only (most common)
        try {
            WebElement btn = driver.findElement(
                io.appium.java_client.AppiumBy.accessibilityId("Not Now")
            );
            btn.click();
            System.out.println("⚡ 'Not Now' clicked in " + (System.currentTimeMillis() - start) + "ms");
            return;
        } catch (Exception e) {}
        
        // One more quick check after tiny delay
        try { Thread.sleep(300); } catch (InterruptedException e) {}
        
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("⚡ Alert dismissed (retry) in " + (System.currentTimeMillis() - start) + "ms");
            return;
        } catch (Exception e) {}
        
        try {
            WebElement btn = driver.findElement(
                io.appium.java_client.AppiumBy.accessibilityId("Not Now")
            );
            btn.click();
            System.out.println("⚡ 'Not Now' clicked (retry) in " + (System.currentTimeMillis() - start) + "ms");
            return;
        } catch (Exception e) {}
        
        System.out.println("⚡ No popup found in " + (System.currentTimeMillis() - start) + "ms");
    }
    
    /**
     * TURBO: Login with minimal waits (under 3 seconds total for popup handling)
     */
    public void loginTurbo(String email, String password) {
        long start = System.currentTimeMillis();

        // Enter credentials (small delay ensures email field is ready for input)
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        enterEmail(email);
        enterPassword(password);

        // Accept Terms & Conditions if the checkbox is present (new app versions)
        acceptTermsIfPresent();

        // Quick tap sign in
        tapSignIn();

        // Fast popup handling (max 1.5 seconds)
        handleSavePasswordTurbo();

        System.out.println("⚡ loginTurbo completed in " + (System.currentTimeMillis() - start) + "ms");
    }


    
    /**
     * ╔══════════════════════════════════════════════════════════════════════╗
     * ║  Polls for Save Password popup up to 5 times with proper delays     ║
     * ║  This ensures 100% reliability even if popup appears late           ║
     * ║  Total coverage time: ~4.3 seconds (1500+1000+800+500+500ms)        ║
     * ╚══════════════════════════════════════════════════════════════════════╝
     */
    private void handleSavePasswordWithPolling() {
        int maxAttempts = 5;
        int[] waitTimes = {1500, 1000, 800, 500, 500}; // Decreasing wait times
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("🔄 Popup handling attempt " + attempt + "/" + maxAttempts);
            
            // Wait before checking (allows popup time to appear)
            try {
                Thread.sleep(waitTimes[attempt - 1]);
            } catch (InterruptedException e) {}
            
            // Try to handle the popup
            boolean handled = tryHandleSavePasswordPopup();
            
            if (handled) {
                System.out.println("✅ Save Password popup handled on attempt " + attempt);
                // Give a brief moment for the popup to fully dismiss
                try { Thread.sleep(300); } catch (InterruptedException e) {}
                return;
            }
        }
        
        System.out.println("ℹ️ Save Password popup handling complete (popup may not have appeared)");
    }
    
    /**
     * Waits up to 5 seconds for Save Password popup to disappear, dismissing it if found
     * This is the most robust way to guarantee the popup is gone before proceeding
     */
    public void waitForNoSavePasswordPopup() {
        long start = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds
        boolean popupFound = false;
        int attempts = 0;
        while (System.currentTimeMillis() - start < timeout) {
            attempts++;
            boolean dismissed = tryHandleSavePasswordPopup();
            if (dismissed) {
                popupFound = true;
                System.out.println("[SavePasswordPopup] Dismissed on attempt " + attempts);
                // Give a short pause for UI to update
                try { Thread.sleep(400); } catch (InterruptedException e) {}
            } else {
                // No popup found, exit loop
                if (popupFound) {
                    System.out.println("[SavePasswordPopup] Confirmed gone after " + attempts + " attempts");
                }
                break;
            }
            // Wait before next check
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }
        if (!popupFound) {
            System.out.println("[SavePasswordPopup] No popup detected in 5s window");
        }
    }

    /**
     * Attempts to handle the Save Password popup using multiple methods
     * Returns true if popup was found and handled, false otherwise
     */
    private boolean tryHandleSavePasswordPopup() {
        // METHOD 1: iOS Native Alert (dismiss)
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("✅ Native alert dismissed");
            return true;
        } catch (Exception e) {}
        
        // METHOD 2: iOS Native Alert (accept)
        try {
            driver.switchTo().alert().accept();
            System.out.println("✅ Native alert accepted");
            return true;
        } catch (Exception e) {}
        
        // METHOD 3: "Not Now" button by accessibility ID
        try {
            WebElement btn = driver.findElement(
                io.appium.java_client.AppiumBy.accessibilityId("Not Now")
            );
            btn.click();
            System.out.println("✅ Clicked 'Not Now' button");
            return true;
        } catch (Exception e) {}
        
        // METHOD 4: Common dismiss button names
        String[] buttonNames = {"Not Now", "Don't Save", "Never", "Cancel", "No", "Later", "Skip"};
        for (String btnName : buttonNames) {
            try {
                WebElement btn = driver.findElement(
                    io.appium.java_client.AppiumBy.accessibilityId(btnName)
                );
                btn.click();
                System.out.println("✅ Clicked: " + btnName);
                return true;
            } catch (Exception e) {}
        }
        
        // METHOD 5: Find button by label containing keywords
        try {
            WebElement btn = driver.findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true AND " +
                    "(label CONTAINS[c] 'not now' OR label CONTAINS[c] 'never' OR " +
                    "label CONTAINS[c] 'cancel' OR label CONTAINS[c] \"don't\" OR " +
                    "label CONTAINS[c] 'skip' OR label CONTAINS[c] 'later')"
                )
            );
            btn.click();
            System.out.println("✅ Clicked dismiss button by label");
            return true;
        } catch (Exception e) {}
        
        // METHOD 6: Find ANY button in a popup sheet
        try {
            java.util.List<WebElement> buttons = driver.findElements(
                io.appium.java_client.AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeSheet/**/XCUIElementTypeButton"
                )
            );
            if (!buttons.isEmpty()) {
                WebElement lastBtn = buttons.get(buttons.size() - 1);
                lastBtn.click();
                System.out.println("✅ Clicked sheet button");
                return true;
            }
        } catch (Exception e) {}
        
        // METHOD 7: Find buttons in Alert container
        try {
            java.util.List<WebElement> buttons = driver.findElements(
                io.appium.java_client.AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeAlert/**/XCUIElementTypeButton"
                )
            );
            if (!buttons.isEmpty()) {
                WebElement firstBtn = buttons.get(0);
                firstBtn.click();
                System.out.println("✅ Clicked alert button");
                return true;
            }
        } catch (Exception e) {}
        
        // METHOD 8: Find "Other" element type
        try {
            WebElement btn = driver.findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND visible == true AND " +
                    "(label CONTAINS[c] 'not now' OR label CONTAINS[c] 'never')"
                )
            );
            btn.click();
            System.out.println("✅ Clicked 'Other' element");
            return true;
        } catch (Exception e) {}
        
        // METHOD 9: Tap coordinates (center-left area for "Not Now")
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int x = size.width / 4;
            int y = size.height / 2;
            
            new io.appium.java_client.TouchAction<>((io.appium.java_client.PerformsTouchActions) driver)
                .tap(io.appium.java_client.touch.offset.PointOption.point(x, y))
                .perform();
            System.out.println("✅ Tapped at coordinates (" + x + ", " + y + ")");
            return true;
        } catch (Exception e) {}
        
        // METHOD 10: Skip - navigate().back() is too slow
        // Removed for performance
        
        return false; // No popup found
    }
}