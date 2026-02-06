package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * SIMPLE DEBUG - Just print current screen state
 */
public class DebugScreenStateTest extends BaseTest {

    @BeforeClass
    public void setupNoReset() {
        DriverManager.setNoReset(true);
    }

    @Test(priority = 1)
    public void debugScreenState() throws InterruptedException {
        IOSDriver driver = DriverManager.getDriver();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CURRENT SCREEN STATE");
        System.out.println("=".repeat(60));
        
        // Wait for app to stabilize
        Thread.sleep(2000);
        
        // Print ALL visible text
        System.out.println("\n>>> ALL STATIC TEXT:");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            System.out.println("Found " + texts.size() + " text elements:\n");
            for (int i = 0; i < Math.min(20, texts.size()); i++) {
                try {
                    String label = texts.get(i).getAttribute("label");
                    if (label != null && !label.isEmpty() && label.length() < 100) {
                        System.out.println("  [" + i + "] \"" + label + "\"");
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        // Print ALL buttons
        System.out.println("\n>>> ALL BUTTONS:");
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            System.out.println("Found " + buttons.size() + " buttons:\n");
            for (int i = 0; i < Math.min(15, buttons.size()); i++) {
                try {
                    String label = buttons.get(i).getAttribute("label");
                    int y = buttons.get(i).getLocation().getY();
                    if (label != null && !label.isEmpty()) {
                        System.out.println("  [" + i + "] y=" + y + " \"" + label + "\"");
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        // Check for specific screens
        System.out.println("\n>>> SCREEN DETECTION:");
        
        // Dashboard check
        try {
            driver.findElement(AppiumBy.accessibilityId("building.2"));
            System.out.println("  ✅ DASHBOARD detected (building.2 found)");
        } catch (Exception e) {
            System.out.println("  ❌ Not on Dashboard");
        }
        
        // Login check
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString("label == 'Sign In' OR label == 'Sign in'"));
            System.out.println("  ✅ LOGIN SCREEN detected");
        } catch (Exception e) {
            System.out.println("  ❌ Not on Login screen");
        }
        
        // Welcome check
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString("label CONTAINS 'Change Company'"));
            System.out.println("  ✅ WELCOME/COMPANY screen detected");
        } catch (Exception e) {
            System.out.println("  ❌ Not on Welcome screen");
        }
        
        // Site Selection check
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString("label CONTAINS 'Continue' OR label CONTAINS 'Site'"));
            System.out.println("  ✅ SITE SELECTION detected");
        } catch (Exception e) {
            System.out.println("  ❌ Not on Site Selection");
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("DEBUG COMPLETE");
        System.out.println("=".repeat(60));
    }
}
