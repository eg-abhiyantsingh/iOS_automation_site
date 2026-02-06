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
 * DEBUG TEST - Comprehensive element analysis
 */
public class DebugLocationElementsTest extends BaseTest {

    @BeforeClass
    public void setupNoReset() {
        DriverManager.setNoReset(true);
    }

    @Test(priority = 1)
    public void debugLocationElements() throws InterruptedException {
        IOSDriver driver = DriverManager.getDriver();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("DEBUG: COMPREHENSIVE ELEMENT ANALYSIS");
        System.out.println("=".repeat(60));
        
        Thread.sleep(1000);
        
        // Step 1: Click Locations
        System.out.println("\n>>> STEP 1: Navigate to Locations");
        try {
            WebElement locBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label == 'Locations'"));
            locBtn.click();
            Thread.sleep(2000);  // Wait for screen to fully load
            System.out.println("✅ On Locations screen\n");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }
        
        // Step 2: Print ALL element types
        System.out.println(">>> STEP 2: COUNT ALL ELEMENT TYPES");
        System.out.println("-".repeat(50));
        String[] types = {"XCUIElementTypeCell", "XCUIElementTypeButton", "XCUIElementTypeStaticText", 
                          "XCUIElementTypeOther", "XCUIElementTypeTable", "XCUIElementTypeCollectionView",
                          "XCUIElementTypeScrollView", "XCUIElementTypeImage"};
        for (String type : types) {
            try {
                List<WebElement> els = driver.findElements(AppiumBy.className(type));
                System.out.println("  " + type + ": " + els.size());
            } catch (Exception ignored) {}
        }
        
        // Step 3: Print ALL visible elements with ANY type
        System.out.println("\n>>> STEP 3: ALL VISIBLE ELEMENTS (predicate query)");
        System.out.println("-".repeat(50));
        try {
            List<WebElement> allVisible = driver.findElements(AppiumBy.iOSNsPredicateString(
                "visible == true AND (type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeOther')"));
            System.out.println("Found " + allVisible.size() + " visible elements:\n");
            
            int shown = 0;
            for (WebElement el : allVisible) {
                if (shown >= 25) {
                    System.out.println("... (showing first 25)");
                    break;
                }
                try {
                    String type = el.getAttribute("type");
                    String label = el.getAttribute("label");
                    String name = el.getAttribute("name");
                    int y = el.getLocation().getY();
                    
                    if (label != null && !label.isEmpty() && y > 100 && y < 800) {
                        System.out.println("[" + shown + "] y=" + y + " " + type + " | \"" + label + "\"");
                        shown++;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        // Step 4: Try to find floors/buildings using text patterns
        System.out.println("\n>>> STEP 4: FIND ELEMENTS WITH 'Floor' OR 'Building' IN LABEL");
        System.out.println("-".repeat(50));
        try {
            List<WebElement> floors = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Floor' OR label CONTAINS 'Building' OR label CONTAINS 'room'"));
            System.out.println("Found " + floors.size() + " matching elements:\n");
            
            for (int i = 0; i < Math.min(15, floors.size()); i++) {
                try {
                    WebElement el = floors.get(i);
                    String type = el.getAttribute("type");
                    String label = el.getAttribute("label");
                    int y = el.getLocation().getY();
                    System.out.println("[" + i + "] y=" + y + " " + type + " | \"" + label + "\"");
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        // Step 5: Click first matching element
        System.out.println("\n>>> STEP 5: CLICK FIRST FLOOR/BUILDING ELEMENT");
        System.out.println("-".repeat(50));
        try {
            List<WebElement> floors = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Floor' OR label CONTAINS 'Building'"));
            if (floors.size() > 0) {
                WebElement first = floors.get(0);
                String label = first.getAttribute("label");
                System.out.println("Clicking: \"" + label + "\"");
                first.click();
                Thread.sleep(1000);
                System.out.println("✅ Clicked!\n");
                
                // Check what appeared
                System.out.println("After click - finding elements again:");
                floors = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "label CONTAINS 'Floor' OR label CONTAINS 'Building' OR label CONTAINS 'room' OR label CONTAINS 'Room'"));
                System.out.println("Now " + floors.size() + " matching elements:\n");
                
                for (int i = 0; i < Math.min(15, floors.size()); i++) {
                    try {
                        String l = floors.get(i).getAttribute("label");
                        String t = floors.get(i).getAttribute("type");
                        int y = floors.get(i).getLocation().getY();
                        System.out.println("[" + i + "] y=" + y + " " + t + " | \"" + l + "\"");
                    } catch (Exception ignored) {}
                }
            } else {
                System.out.println("No floor/building elements found!");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        // Step 6: Look for rooms (NOT floors)
        System.out.println("\n>>> STEP 6: FIND ROOM ELEMENTS (exclude 'Floor')");
        System.out.println("-".repeat(50));
        try {
            List<WebElement> allText = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton'"));
            System.out.println("Checking " + allText.size() + " text/button elements for rooms:\n");
            
            int shown = 0;
            for (WebElement el : allText) {
                if (shown >= 15) break;
                try {
                    String label = el.getAttribute("label");
                    int y = el.getLocation().getY();
                    
                    // Skip if it's a floor, building, or navigation
                    if (label == null || label.isEmpty()) continue;
                    if (label.contains("Floor") || label.contains("Building") || 
                        label.contains("Location") || label.equals("Add") ||
                        label.contains("Search") || y < 150 || y > 780) continue;
                    
                    String type = el.getAttribute("type");
                    System.out.println("[" + shown + "] y=" + y + " " + type + " | \"" + label + "\"");
                    shown++;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("DEBUG COMPLETE");
        System.out.println("=".repeat(60));
    }
}
