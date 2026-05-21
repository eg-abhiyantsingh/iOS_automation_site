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
import java.util.Arrays;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
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
     * Click element with wait, retry, and coordinate tap fallback.
     * If standard .click() fails 3 times, falls back to tapping element center via coordinates.
     * Also dismisses keyboard first if the element is in the lower half of the screen.
     */
    protected void click(WebElement element) {
        int maxRetries = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                WebElement clickable = waitForClickable(element, 10);

                // Dismiss keyboard if element is in lower screen half (may be occluded)
                if (attempt == 1) {
                    try {
                        int elY = clickable.getLocation().getY();
                        int screenH = driver.manage().window().getSize().height;
                        if (elY > screenH * 0.55 && isKeyboardShown()) {
                            // CAUTION: use only the SAFE non-destructive variant here.
                            // The full dismissKeyboard cascade taps the keyboard's
                            // Return/Done key, which on login-screen SecureTextField
                            // SUBMITS the form with whatever's currently filled.
                            // (Regression in TC_ISS_002-010 — login submitted with
                            // empty password.) Use tapOutside-only and accept that
                            // some keyboards persist; the click() itself often
                            // dismisses by shifting focus.
                            try {
                                java.util.Map<String, Object> args = new java.util.HashMap<>();
                                args.put("strategy", "tapOutside");
                                driver.executeScript("mobile: hideKeyboard", args);
                                sleep(150);
                            } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                }

                clickable.click();
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

        // Fallback: coordinate tap on element center (handles table cells, custom views)
        try {
            int x = element.getLocation().getX() + element.getSize().getWidth() / 2;
            int y = element.getLocation().getY() + element.getSize().getHeight() / 2;
            if (y > 120) { // Avoid nav bar zone
                driver.executeScript("mobile: tap", java.util.Map.of("x", x, "y", y));
                System.out.println("✅ Click succeeded via coordinate tap at (" + x + ", " + y + ")");
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Coordinate tap fallback also failed");
        }

        throw new RuntimeException("Failed to click element after " + maxRetries + " attempts + coordinate fallback", lastException);
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

    // ================================================================
    // KEYBOARD DISMISSAL — bulletproof multi-strategy + verify-after-each
    // ================================================================
    // All page objects MUST use these methods (NOT raw mobile:hideKeyboard
    // or driver.hideKeyboard()) — see commit 3cc6d80 for rationale.

    /**
     * Returns true if the iOS keyboard is currently visible.
     * Note: the XCUIElementTypeKeyboard element can persist in the DOM after the
     * keyboard is hidden (iOS quirk), so we require visible == 1 to avoid false
     * positives. On iOS 18.5 the visible attribute is reliable for this check.
     */
    public boolean isKeyboardShown() {
        try {
            return !driver.findElements(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeKeyboard' AND visible == 1")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Dismiss the iOS keyboard using a cascade of strategies, verifying
     * isKeyboardShown() after each. Returns silently when keyboard is
     * absent. Used everywhere — do NOT call mobile:hideKeyboard directly.
     *
     * Login-form guard: when a "Sign In"/"Connexion" button is visible we
     * are on the auth screen — pressing Return on a focused Email/Password
     * field submits the form prematurely. We narrow the Return-key skip
     * to that case only; for every other screen Return is the standard
     * iOS gesture and dismisses the keyboard reliably.
     *
     * Strategies (each verifies):
     *   1a/b: mobile:hideKeyboard with strategy={tapOutside,swipeDown}
     *   1c: plain mobile:hideKeyboard (default)
     *   2:  Coordinate tap in the safe zone above the keyboard (NOT the
     *       status bar — status-bar taps are absorbed by the system and
     *       never reach the app, so they cannot dismiss the keyboard).
     *   3:  Tap a static-text label sitting above the keyboard (kills the
     *       text-field's first-responder without triggering any control).
     *   4:  Tap Done/Terminé on the keyboard accessory bar.
     *   5:  sendKeys("\n") to the focused text input (skipped on the login
     *       screen to avoid form submission with empty/partial creds).
     *   6:  Short scroll swipe — iOS dismisses the keyboard on user scroll.
     */
    public void dismissKeyboard() {
        if (!isKeyboardShown()) {
            return;
        }

        // Detect login-form context ONCE — drives which strategies are safe.
        boolean loginContext = false;
        try {
            driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == 1 AND " +
                "(label CONTAINS[c] 'Sign In' OR label CONTAINS[c] 'Sign in' OR " +
                " name CONTAINS[c] 'Sign In' OR name CONTAINS[c] 'signIn' OR " +
                " label CONTAINS[c] 'Connexion' OR label CONTAINS[c] 'Se connecter')"));
            loginContext = true;
        } catch (Exception ignored) {}

        // Strategy 1a/b: mobile:hideKeyboard with non-destructive strategies
        for (String strategy : new String[]{"tapOutside", "swipeDown"}) {
            try {
                java.util.Map<String, Object> args = new java.util.HashMap<>();
                args.put("strategy", strategy);
                driver.executeScript("mobile: hideKeyboard", args);
                sleep(250);
                if (!isKeyboardShown()) {
                    System.out.println("   Keyboard dismissed via mobile:hideKeyboard[" + strategy + "]");
                    return;
                }
            } catch (Exception ignored) { /* try next */ }
        }

        // Strategy 1c: Plain mobile:hideKeyboard (default strategy, no args)
        try {
            driver.executeScript("mobile: hideKeyboard");
            sleep(250);
            if (!isKeyboardShown()) {
                System.out.println("   Keyboard dismissed via mobile:hideKeyboard (default)");
                return;
            }
        } catch (Exception ignored) {}

        // Compute the keyboard's top edge so we can target a tap point strictly above it.
        int screenWidth = driver.manage().window().getSize().getWidth();
        int screenHeight = driver.manage().window().getSize().getHeight();
        int keyboardTopY = (int) (screenHeight * 0.55); // sane default
        try {
            WebElement kb = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeKeyboard' AND visible == 1"));
            int kbY = kb.getLocation().getY();
            if (kbY > 100 && kbY < screenHeight) {
                keyboardTopY = kbY;
            }
        } catch (Exception ignored) {}

        // Strategy 2: Coordinate tap in the safe zone (well below nav bar, well above keyboard).
        // This is the workhorse for TextField/SecureTextField — a plain background tap
        // makes the focused field resign first responder and the keyboard slides away.
        try {
            int safeY = Math.max(140, keyboardTopY - 80);
            if (safeY < keyboardTopY && safeY > 120) {
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence tap = new Sequence(finger, 0);
                tap.addAction(finger.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), screenWidth / 2, safeY));
                tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Collections.singletonList(tap));
                sleep(300);
                if (!isKeyboardShown()) {
                    System.out.println("   Keyboard dismissed via safe-zone tap (X=" +
                        (screenWidth / 2) + ", Y=" + safeY + ")");
                    return;
                }
            }
        } catch (Exception ignored) {}

        // Strategy 3: Tap a static-text label sitting in the safe zone. StaticText
        // is non-interactive in iOS — tapping it cannot trigger navigation, but it
        // still steals first-responder away from the focused field.
        try {
            java.util.List<WebElement> labels = driver.findElements(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == 1"));
            for (WebElement lbl : labels) {
                try {
                    int lblY = lbl.getLocation().getY();
                    int lblH = lbl.getSize().getHeight();
                    if (lblY > 130 && lblY + lblH < keyboardTopY - 20 && lblH > 0 && lblH < 60) {
                        int cx = lbl.getLocation().getX() + Math.max(5, lbl.getSize().getWidth() / 2);
                        int cy = lblY + Math.max(5, lblH / 2);
                        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                        Sequence tap = new Sequence(finger, 0);
                        tap.addAction(finger.createPointerMove(Duration.ZERO,
                            PointerInput.Origin.viewport(), cx, cy));
                        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                        driver.perform(java.util.Collections.singletonList(tap));
                        sleep(300);
                        if (!isKeyboardShown()) {
                            System.out.println("   Keyboard dismissed via StaticText tap (Y=" + lblY + ")");
                            return;
                        }
                        break; // one safe-zone label is enough; don't spam taps
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // Strategy 4: Tap Done/Terminé on keyboard accessory bar. Filter Y > 40%
        // screen to avoid nav-bar Done buttons.
        try {
            int minY = (int) (screenHeight * 0.4);
            java.util.List<WebElement> doneBtns = driver.findElements(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeKey') AND " +
                "(label == 'Done' OR label == 'Terminé' OR label == 'OK' OR label == 'return')"));
            for (WebElement btn : doneBtns) {
                try {
                    int btnY = btn.getLocation().getY();
                    if (btnY > minY) {
                        btn.click();
                        sleep(300);
                        if (!isKeyboardShown()) {
                            System.out.println("   Keyboard dismissed via Done/Terminé key (Y=" + btnY + ")");
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // Strategy 5: sendKeys("\n") to the focused text input — but ONLY when
        // we are NOT on the login screen. On non-login screens this is the
        // natural keyboard-dismiss gesture and does not submit the form.
        if (!loginContext) {
            try {
                WebElement focused = driver.findElement(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR " +
                    " type == 'XCUIElementTypeTextView' OR " +
                    " type == 'XCUIElementTypeSecureTextField') AND hasKeyboardFocus == 1"));
                focused.sendKeys("\n");
                sleep(300);
                if (!isKeyboardShown()) {
                    System.out.println("   Keyboard dismissed via \\n to focused field (non-login)");
                    return;
                }
            } catch (Exception ignored) {}
        }

        // Strategy 6: Short scroll swipe in the safe zone — iOS auto-dismisses
        // the keyboard when the user scrolls a form view.
        try {
            int startX = screenWidth / 2;
            int startY = Math.max(180, keyboardTopY - 100);
            int endY   = Math.max(140, keyboardTopY - 180);
            if (startY > endY + 20) {
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence swipe = new Sequence(finger, 0);
                swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), startX, startY));
                swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(250),
                    PointerInput.Origin.viewport(), startX, endY));
                swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Collections.singletonList(swipe));
                sleep(400);
                if (!isKeyboardShown()) {
                    System.out.println("   Keyboard dismissed via short scroll swipe");
                    return;
                }
            }
        } catch (Exception ignored) {}

        if (isKeyboardShown()) {
            System.out.println("⚠️ Keyboard still visible after all dismiss strategies");
        }
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
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = size.width / 2;
            int startY = (int) (size.height * 0.7);
            int endY = (int) (size.height * 0.3);
            
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence scroll = new Sequence(finger, 1);
            scroll.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), startX, endY));
            scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            
            driver.perform(Arrays.asList(scroll));
            System.out.println("📜 Scrolled down");
        } catch (Exception e) {
            System.out.println("⚠️ Scroll down failed: " + e.getMessage());
        }
    }

    /**
     * Scroll up
     */
    protected void scrollUp() {
        try {
            Dimension size = driver.manage().window().getSize();
            int startX = size.width / 2;
            int startY = (int) (size.height * 0.3);
            int endY = (int) (size.height * 0.7);
            
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence scroll = new Sequence(finger, 1);
            scroll.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), startX, endY));
            scroll.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            
            driver.perform(Arrays.asList(scroll));
            System.out.println("📜 Scrolled up");
        } catch (Exception e) {
            System.out.println("⚠️ Scroll up failed: " + e.getMessage());
        }
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
