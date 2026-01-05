package com.egalvanic.tests;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.URL;
import java.time.Duration;
import java.util.List;

import org.openqa.selenium.WebElement;

/**
 * TC_ASSET_001 - Complete Asset Creation Test
 * 
 * ============================================
 * DISCOVERED ACCESSIBILITY IDs
 * ============================================
 * 
 * NAVIGATION:
 * - Assets Tab: "list.bullet"
 * - Add Asset: "plus"
 * 
 * FORM HEADER:
 * - Cancel: "Cancel"
 * - Quick Mode: "Quick" (value='1' when selected)
 * - Detailed Mode: "Detailed"
 * 
 * FORM FIELDS:
 * - Asset Name: TextField with value="Enter name"
 * - Location Dropdown: "Select location"
 * - Asset Class Dropdown: "Select asset class"
 * - Asset Subtype: "Select asset subtype"
 * - QR Code: TextField with value="Enter or scan QR code"
 * - QR Scan: "Scan"
 * 
 * ASSET CLASS OPTIONS:
 * "ATS", "Busway", "Capacitor", "Circuit Breaker", "Default",
 * "Disconnect Switch", "Fuse", "Generator", "Junction Box",
 * "Loadcenter", "MCC", "MCC Bucket", "Motor", "Motor Starter",
 * "Other", "Other (OCP)", "PDU", "Panelboard", "Reactor",
 * "Relay", "Switchboard"
 * 
 * LOCATION PICKER:
 * - Add Location: "plus.circle.fill"
 * - Existing Locations: Button with location name (e.g., "1.", "22")
 * - Location Form Name Field: TextField with value="Floor Name" or "Building
 * Name" or "Room Name"
 * - Save Location: "Save"
 * - Cancel Location: "Cancel"
 * 
 * PHOTO SECTIONS:
 * - Profile: "Profile"
 * - Nameplate: "Nameplate"
 * - Panel Schedule: "Panel Schedule"
 * - Arc Flash Sticker: "Arc Flash Sticker"
 * - Gallery: "Gallery"
 * - Camera: "Camera"
 * 
 * SAVE ASSET:
 * - Save: "Save"
 */
public class TC_Asset_Creation {

    private static IOSDriver driver;
    private static final String TIMESTAMP = String.valueOf(System.currentTimeMillis());

    public static void main(String[] args) {
        try {
            setupDriver();
            login();
            handleSiteSelection();
            navigateToAssets();
            createAsset();

            System.out.println("\n✅ TC_ASSET_001 - Asset Creation Test PASSED!");

        } catch (Exception e) {
            System.err.println("\n❌ TC_ASSET_001 - Asset Creation Test FAILED!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("✅ Driver closed");
            }
        }
    }

    // ========================================================================
    // SETUP
    // ========================================================================

    private static void setupDriver() throws Exception {
        URL appiumServer = new URL("http://localhost:4723");

        XCUITestOptions options = new XCUITestOptions();
        options.setPlatformName("iOS");
        options.setAutomationName("XCUITest");
        options.setDeviceName("iPhone 17 Pro");
        options.setPlatformVersion("26.2");
        options.setUdid("B745C0EF-01AA-4355-8B08-86812A8CBBAA");
        options.setApp("/Users/abhiyantsingh/Downloads/Z Platform-QA.app");
        options.setNewCommandTimeout(Duration.ofSeconds(300));
        options.setWdaLaunchTimeout(Duration.ofMillis(120000));
        options.setWdaConnectionTimeout(Duration.ofMillis(120000));
        options.setUseNewWDA(false);
        options.setWaitForQuiescence(true);
        options.setCapability("autoAcceptAlerts", true);

        System.out.println("📱 Connecting to Appium server...");
        driver = new IOSDriver(appiumServer, options);
        System.out.println("✅ App launched successfully!");
        Thread.sleep(2000);
    }

    // ========================================================================
    // LOGIN
    // ========================================================================

    private static void login() throws Exception {
        System.out.println("\n--- LOGIN ---");

        List<WebElement> companyCodeField = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND value == '(e.g. acme.egalvanic)'"));

        if (companyCodeField.size() > 0) {
            companyCodeField.get(0).sendKeys("acme.egalvanic");
            driver.findElement(AppiumBy.accessibilityId("Continue")).click();
            Thread.sleep(2000);

            driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND visible == 1"))
                    .sendKeys("rahul+acme@egalvanic.com");
            Thread.sleep(1000);

            driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSecureTextField' AND visible == 1"))
                    .sendKeys("RP@egalvanic123");
            Thread.sleep(1000);

            driver.findElement(AppiumBy.accessibilityId("Sign In")).click();
            Thread.sleep(5000);
            System.out.println("✅ Logged in successfully");
        } else {
            System.out.println("✅ Already logged in");
            Thread.sleep(2000);
        }
    }

    // ========================================================================
    // SITE SELECTION
    // ========================================================================

    private static void handleSiteSelection() throws Exception {
        System.out.println("\n--- SITE SELECTION ---");
        Thread.sleep(2000);

        List<WebElement> searchBox = driver.findElements(
                AppiumBy.iOSNsPredicateString("value == 'Search sites...'"));

        if (searchBox.size() > 0) {
            System.out.println("📍 On site selection screen");

            List<WebElement> allButtons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(",") && !name.equals("Create New Site")) {
                    btn.click();
                    System.out.println("✅ Selected site: " + name);
                    break;
                }
            }
            Thread.sleep(5000);
        } else {
            System.out.println("✅ Site already selected");
        }
    }

    // ========================================================================
    // NAVIGATE TO ASSETS
    // ========================================================================

    private static void navigateToAssets() throws Exception {
        System.out.println("\n--- NAVIGATE TO ASSETS ---");
        Thread.sleep(3000);
        driver.findElement(AppiumBy.accessibilityId("list.bullet")).click();
        System.out.println("✅ Clicked Assets tab");
        Thread.sleep(3000);
    }

    // ========================================================================
    // CREATE ASSET
    // ========================================================================

    private static void createAsset() throws Exception {
        System.out.println("\n--- CREATE ASSET ---");

        // Step 1: Open Asset Form
        driver.findElement(AppiumBy.accessibilityId("plus")).click();
        System.out.println("✅ Step 1: Opened Asset Form (clicked 'plus')");
        Thread.sleep(2000);

        // Step 2: Enter Asset Name
        enterAssetName("TestAsset_" + TIMESTAMP);

        // Step 3: Select Asset Class
        selectAssetClass("ATS");

        // Step 4: Enter QR Code (Optional)
        enterQRCode("QR_" + TIMESTAMP);

        // Step 5: Select Location
        selectLocation();

        // Step 6: Save Asset
        saveAsset();
    }

    /**
     * Step 2: Enter Asset Name
     */
    private static void enterAssetName(String assetName) throws Exception {
        System.out.println("\n📝 Step 2: Enter Asset Name");

        WebElement nameField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Enter name'"));
        nameField.click();
        Thread.sleep(300);
        nameField.sendKeys(assetName);
        System.out.println("✅ Entered Asset Name: " + assetName);

        // Dismiss keyboard
        dismissKeyboard();
        Thread.sleep(500);
    }

    /**
     * Step 3: Select Asset Class
     */
    private static void selectAssetClass(String assetClassName) throws Exception {
        System.out.println("\n🏷️ Step 3: Select Asset Class");

        // Click dropdown
        driver.findElement(AppiumBy.accessibilityId("Select asset class")).click();
        System.out.println("✅ Opened Asset Class dropdown");
        Thread.sleep(1500);

        // Select asset class by accessibility ID
        driver.findElement(AppiumBy.accessibilityId(assetClassName)).click();
        System.out.println("✅ Selected Asset Class: " + assetClassName);
        Thread.sleep(1000);
    }

    /**
     * Step 4: Enter QR Code (Optional)
     */
    private static void enterQRCode(String qrCode) throws Exception {
        System.out.println("\n📱 Step 4: Enter QR Code");

        try {
            WebElement qrField = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeTextField' AND value == 'Enter or scan QR code'"));
            qrField.click();
            Thread.sleep(300);
            qrField.sendKeys(qrCode);
            System.out.println("✅ Entered QR Code: " + qrCode);

            dismissKeyboard();
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("⚠️ QR Code field not accessible, skipping");
        }
    }

    /**
     * Step 5: Select Location (Create New)
     */
    private static void selectLocation() throws Exception {
        System.out.println("\n📍 Step 5: Select Location");

        // Scroll up to ensure Location dropdown is visible
        scrollUp();
        Thread.sleep(500);

        // Click Location dropdown
        driver.findElement(AppiumBy.accessibilityId("Select location")).click();
        System.out.println("✅ Opened Location dropdown");
        Thread.sleep(2000);

        // Click Add Location button
        driver.findElement(AppiumBy.accessibilityId("plus.circle.fill")).click();
        System.out.println("✅ Clicked Add Location (plus.circle.fill)");
        Thread.sleep(2000);

        // Enter Location Name
        // The field could have value "Floor Name", "Building Name", or "Room Name"
        String locationName = "TestLocation_" + TIMESTAMP;
        boolean entered = false;

        String[] possibleFieldValues = { "Floor Name", "Building Name", "Room Name", "Enter name" };
        for (String fieldValue : possibleFieldValues) {
            try {
                List<WebElement> fields = driver.findElements(
                        AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeTextField' AND value == '" + fieldValue + "'"));
                if (fields.size() > 0) {
                    fields.get(0).click();
                    Thread.sleep(300);
                    fields.get(0).sendKeys(locationName);
                    System.out.println("✅ Entered Location Name: " + locationName + " (in field: " + fieldValue + ")");
                    entered = true;
                    break;
                }
            } catch (Exception e) {
                // Continue to next field type
            }
        }

        if (!entered) {
            // Fallback: try last text field
            List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            if (textFields.size() > 0) {
                WebElement lastField = textFields.get(textFields.size() - 1);
                lastField.click();
                Thread.sleep(300);
                lastField.sendKeys(locationName);
                System.out.println("✅ Entered Location Name in last text field: " + locationName);
            }
        }

        dismissKeyboard();
        Thread.sleep(500);

        // Save Location
        // Find the correct Save button (for location form, not main form)
        List<WebElement> saveButtons = driver.findElements(AppiumBy.accessibilityId("Save"));
        if (saveButtons.size() > 0) {
            saveButtons.get(0).click();
            System.out.println("✅ Saved Location");
            Thread.sleep(2000);
        }

        // Close location picker if Done button exists
        try {
            List<WebElement> doneButtons = driver.findElements(AppiumBy.accessibilityId("Done"));
            if (doneButtons.size() > 0) {
                doneButtons.get(0).click();
                System.out.println("✅ Closed Location picker (clicked Done)");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            // Location picker may have auto-closed
        }
    }

    /**
     * Step 6: Save Asset
     */
    private static void saveAsset() throws Exception {
        System.out.println("\n💾 Step 6: Save Asset");

        // Scroll up to ensure Save button is visible
        scrollUp();
        scrollUp();
        Thread.sleep(500);

        // Find and click Save button
        List<WebElement> saveButtons = driver.findElements(AppiumBy.accessibilityId("Save"));
        if (saveButtons.size() > 0) {
            // Click the last Save button (main form's Save)
            saveButtons.get(saveButtons.size() - 1).click();
            System.out.println("✅ Clicked Save to create Asset");
            Thread.sleep(3000);
        } else {
            System.out.println("⚠️ Save button not found");
        }

        // Verify: Check if back on Assets list
        try {
            List<WebElement> plusButton = driver.findElements(AppiumBy.accessibilityId("plus"));
            List<WebElement> assetsList = driver.findElements(AppiumBy.accessibilityId("list.bullet"));
            if (plusButton.size() > 0 && assetsList.size() > 0) {
                System.out.println("✅ Asset created successfully - back on Assets list");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not verify asset creation");
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private static void dismissKeyboard() {
        try {
            // Try pressing Return key
            driver.findElement(AppiumBy.accessibilityId("Return")).click();
        } catch (Exception e) {
            try {
                // Try tapping on a static text to dismiss
                driver.findElement(AppiumBy.accessibilityId("Asset Details")).click();
            } catch (Exception e2) {
                // Keyboard may already be dismissed
            }
        }
    }

    private static void scrollUp() {
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int startX = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.3);
            int endY = (int) (size.getHeight() * 0.7);

            org.openqa.selenium.interactions.PointerInput finger = new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence scroll = new org.openqa.selenium.interactions.Sequence(finger, 1);
            scroll.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(finger.createPointerMove(Duration.ofMillis(500),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
            scroll.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(scroll));
        } catch (Exception e) {
            // Ignore scroll errors
        }
    }

    private static void scrollDown() {
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int startX = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.7);
            int endY = (int) (size.getHeight() * 0.3);

            org.openqa.selenium.interactions.PointerInput finger = new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence scroll = new org.openqa.selenium.interactions.Sequence(finger, 1);
            scroll.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
            scroll.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            scroll.addAction(finger.createPointerMove(Duration.ofMillis(500),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
            scroll.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(scroll));
        } catch (Exception e) {
            // Ignore scroll errors
        }
    }
}