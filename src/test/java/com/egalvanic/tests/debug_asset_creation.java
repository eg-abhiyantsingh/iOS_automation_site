package com.egalvanic.tests;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.WebElement;

/**
 * Simple Appium script to install and open iOS app
 */
public class debug_asset_creation {

    public static void main(String[] args) {
        IOSDriver driver = null;
        try {
            // Appium server URL
            URL appiumServer = new URL("http://localhost:4723");

            // Set up options
            XCUITestOptions options = new XCUITestOptions();
            options.setPlatformName("iOS");
            options.setAutomationName("XCUITest");
            options.setDeviceName("iPhone 17 Pro");
            options.setPlatformVersion("26.2");
            options.setUdid("B745C0EF-01AA-4355-8B08-86812A8CBBAA");

            // Path to your app file
            String appPath = "/Users/abhiyantsingh/Downloads/Z Platform-QA.app";
            options.setApp(appPath);

            // Set timeouts
            options.setNewCommandTimeout(Duration.ofSeconds(300));
            options.setWdaLaunchTimeout(Duration.ofMillis(120000));
            options.setWdaConnectionTimeout(Duration.ofMillis(120000));
            options.setUseNewWDA(false);
            options.setWaitForQuiescence(true);

            // Auto accept alerts
            options.setCapability("autoAcceptAlerts", true);

            System.out.println("📱 Connecting to Appium server...");
            System.out.println("📱 Installing app from: " + appPath);

            // Create the driver
            driver = new IOSDriver(appiumServer, options);

            System.out.println("✅ App installed and launched successfully!");

            // Wait for app to load
            Thread.sleep(2000);

            // Enter company code
            driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeTextField' AND value == '(e.g. acme.egalvanic)'"))
                    .sendKeys("acme.egalvanic");

            driver.findElement(AppiumBy.accessibilityId("Continue")).click();
            Thread.sleep(2000);

            // Enter email
            driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND visible == 1"))
                    .sendKeys("rahul+acme@egalvanic.com");
            Thread.sleep(2000);

            // Enter password
            driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSecureTextField' AND visible == 1"))
                    .sendKeys("RP@egalvanic123");
            Thread.sleep(2000);

            // Click Sign In
            driver.findElement(AppiumBy.accessibilityId("Sign In")).click();

            // Wait for login
            Thread.sleep(5000);

            System.out.println("✅ Notification popup auto-dismissed by Appium");

            // Click on search box and enter text
            WebElement searchBox = driver.findElement(AppiumBy.iOSNsPredicateString("value == 'Search sites...'"));
            searchBox.click();
            Thread.sleep(500);
            searchBox.sendKeys("test");
            System.out.println("✅ Entered 'test' in search box");
            Thread.sleep(1000);

            // Clear search box using X button
            driver.findElement(AppiumBy.accessibilityId("xmark.circle.fill")).click();
            System.out.println("✅ Search box cleared");
            Thread.sleep(1000);

            // Get all site buttons
            java.util.List<WebElement> allButtons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            java.util.List<WebElement> sites = new java.util.ArrayList<>();

            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name != null && !name.equals("Emoji") && !name.equals("dictation")
                        && !name.equals("Create New Site") && !name.equals("Cancel")
                        && !name.equals("xmark.circle.fill") && name.contains(",")) {
                    sites.add(btn);
                }
            }

            System.out.println("📋 Found " + sites.size() + " sites");

            // Select random site
            if (sites.size() > 0) {
                int randomIndex = new java.util.Random().nextInt(sites.size());
                String siteName = sites.get(randomIndex).getAttribute("name");
                sites.get(randomIndex).click();
                System.out.println(
                        "✅ Selected site: " + siteName + " (Index: " + randomIndex + " of " + sites.size() + ")");
            } else {
                System.out.println("❌ No sites found");
            }

            Thread.sleep(8000);

            // ============================================================
            // DISABLE WiFi
            // ============================================================
            // ============================================================
            // DISABLE WiFi
            // ============================================================
            driver.findElement(AppiumBy.accessibilityId("Wi-Fi")).click();
            System.out.println("✅ WiFi button clicked");
            Thread.sleep(1000);

            driver.findElement(AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeWindow[1]/XCUIElementTypeOther[3]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther[2]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther[2]/XCUIElementTypeOther"))
                    .click();
            System.out.println("✅ WiFi disabled");
            Thread.sleep(2000);

            // ============================================================
            // ENABLE WiFi (use "Wi-Fi Off" accessibility ID)
            // ============================================================
            driver.findElement(AppiumBy.accessibilityId("Wi-Fi Off")).click();
            System.out.println("✅ Wi-Fi Off button clicked to enable");
            Thread.sleep(1000);

            driver.findElement(AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeWindow[1]/XCUIElementTypeOther[3]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther[2]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther[2]/XCUIElementTypeOther"))
                    .click();
            System.out.println("✅ WiFi enabled");
            Thread.sleep(1000);

            driver.findElement(AppiumBy.accessibilityId("Locations")).click();
            System.out.println("✅ Locations clicked");
            Thread.sleep(2000);

            // ============================================================
            // DEBUG: Find elements on Locations screen
            // ============================================================
            // ============================================================
            // LOCATIONS: Go to Locations
            // ============================================================
            driver.findElement(AppiumBy.accessibilityId("Locations")).click();
            System.out.println("✅ Locations clicked");
            Thread.sleep(2000);

            // ============================================================
            // LOCATIONS: Go to Locations
            // ============================================================
            driver.findElement(AppiumBy.accessibilityId("Locations")).click();
            System.out.println("✅ Locations clicked");
            Thread.sleep(2000);

            // ============================================================
            // CREATE NEW BUILDING
            // ============================================================
            // Click + (Add) button
            driver.findElement(AppiumBy.accessibilityId("plus")).click();
            System.out.println("✅ Add button clicked");
            Thread.sleep(1000);

            // Enter building name "1"
            driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Building Name'"))
                    .click();
            Thread.sleep(500);
            driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Building Name'"))
                    .sendKeys("1");
            System.out.println("✅ Entered building name: 1");
            Thread.sleep(500);

            // Click Save
            driver.findElement(AppiumBy.accessibilityId("Save")).click();
            System.out.println("✅ Save button clicked - Building created");
            Thread.sleep(2000);

        } catch (Exception e) {
            System.err.println("❌ Error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("✅ Driver closed successfully");
            }
        }
    }
}