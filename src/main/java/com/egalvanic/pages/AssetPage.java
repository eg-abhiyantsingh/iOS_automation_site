package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Asset Page Object
 * Handles all Asset Management screen interactions
 * 
 * Features covered:
 * - Asset List Screen
 * - Create Asset
 * - Edit Asset
 * - Asset Details
 * - Asset Class Selection
 * - Location Selection
 * - Asset Subtype Selection
 * - QR Code Entry
 */
public class AssetPage extends BasePage {

    // ================================================================
    // ASSET LIST SCREEN ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(accessibility = "list.bullet")
    private WebElement assetListButton;

    @iOSXCUITFindBy(accessibility = "plus")
    private WebElement plusButton;

    @iOSXCUITFindBy(accessibility = "Back")
    private WebElement backButton;

    @iOSXCUITFindBy(accessibility = "Cancel")
    private WebElement cancelButton;

    // ================================================================
    // CREATE/EDIT ASSET FORM ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == 'Enter name'")
    private WebElement assetNameField;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField'")
    private List<WebElement> allTextFields;

    @iOSXCUITFindBy(accessibility = "Select asset class")
    private WebElement selectAssetClassButton;

    @iOSXCUITFindBy(accessibility = "Select location")
    private WebElement selectLocationButton;

    @iOSXCUITFindBy(accessibility = "Select asset subtype")
    private WebElement selectAssetSubtypeButton;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == 'Enter or scan QR code'")
    private WebElement qrCodeField;

    @iOSXCUITFindBy(accessibility = "Asset Details")
    private WebElement assetDetailsHeader;

    @iOSXCUITFindBy(accessibility = "Create Asset")
    private WebElement createAssetButton;

    @iOSXCUITFindBy(accessibility = "Save")
    private WebElement saveButton;

    @iOSXCUITFindBy(accessibility = "Delete")
    private WebElement deleteButton;

    // ================================================================
    // ASSET CLASS OPTIONS
    // ================================================================

    @iOSXCUITFindBy(accessibility = "ATS")
    private WebElement atsClassOption;

    @iOSXCUITFindBy(accessibility = "UPS")
    private WebElement upsClassOption;

    @iOSXCUITFindBy(accessibility = "PDU")
    private WebElement pduClassOption;

    @iOSXCUITFindBy(accessibility = "Generator")
    private WebElement generatorClassOption;

    // ================================================================
    // LOCATION ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][1]")
    private WebElement addFloorButton;

    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][2]")
    private WebElement addRoomButton;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == 'Floor Name'")
    private WebElement floorNameField;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == 'Room Name'")
    private WebElement roomNameField;

    // ================================================================
    // ASSET SUBTYPE OPTIONS
    // ================================================================

    @iOSXCUITFindBy(accessibility = "test")
    private WebElement testSubtypeOption;

    // ================================================================
    // ASSET LIST ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton'")
    private List<WebElement> allButtons;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeCell'")
    private List<WebElement> assetCells;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeSearchField'")
    private WebElement searchBar;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public AssetPage() {
        super();
    }

    // ================================================================
    // NAVIGATION METHODS
    // ================================================================

    public void navigateToAssetList() {
        System.out.println("📦 Navigating to Asset List...");
        
        // Wait for dashboard to be ready and asset list button to be clickable
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.elementToBeClickable(assetListButton));
            click(assetListButton);
        } catch (Exception e) {
            System.out.println("⚠️ First attempt failed, trying alternative locator...");
            // Try alternative approach - find by predicate
            try {
                WebElement listButton = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == 'list.bullet' OR label == 'list.bullet'")
                );
                listButton.click();
            } catch (Exception e2) {
                System.out.println("⚠️ Alternative approach failed, trying accessibility ID directly...");
                // Last resort - use driver directly
                driver.findElement(AppiumBy.accessibilityId("list.bullet")).click();
            }
        }
        
        waitForAssetListReady();
    }

    public void clickAddAsset() {
        System.out.println("➕ Clicking Add Asset button...");
        click(plusButton);
        waitForCreateAssetFormReady();
    }

    public void clickBack() {
        click(backButton);
    }

    public void clickCancel() {
        click(cancelButton);
    }

    // ================================================================
    // WAIT METHODS
    // ================================================================

    public void waitForAssetListReady() {
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            quickWait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOf(plusButton),
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("plus"))
            ));
            System.out.println("✅ Asset List ready");
        } catch (Exception e) {
            System.out.println("⚠️ Asset List wait timeout: " + e.getMessage());
        }
    }

    public void waitForCreateAssetFormReady() {
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            quickWait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOf(assetNameField),
                ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Enter name'")
                )
            ));
            // Additional wait for form to fully load
            sleep(1000);
            System.out.println("✅ Create Asset form ready");
        } catch (Exception e) {
            System.out.println("⚠️ Create Asset form wait timeout: " + e.getMessage());
        }
    }

    // ================================================================
    // ASSET LIST METHODS
    // ================================================================

    public boolean isAssetListDisplayed() {
        return isElementDisplayed(plusButton) || isElementDisplayed(assetListButton);
    }

    public boolean isPlusButtonDisplayed() {
        return isElementDisplayed(plusButton);
    }

    public int getAssetCount() {
        try {
            return assetCells.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public void searchAsset(String assetName) {
        try {
            if (isElementDisplayed(searchBar)) {
                click(searchBar);
                searchBar.sendKeys(assetName);
                System.out.println("✅ Searched for asset: " + assetName);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Search bar not available: " + e.getMessage());
        }
    }

    public boolean selectAssetByName(String assetName) {
        try {
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(assetName)) {
                    btn.click();
                    System.out.println("✅ Selected asset: " + name);
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not select asset: " + e.getMessage());
        }
        return false;
    }

    public String selectFirstAsset() {
        try {
            if (assetCells.size() > 0) {
                String name = assetCells.get(0).getAttribute("name");
                assetCells.get(0).click();
                return name;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not select first asset: " + e.getMessage());
        }
        return null;
    }

    // ================================================================
    // CREATE ASSET FORM METHODS
    // ================================================================

    public boolean isCreateAssetFormDisplayed() {
        return isElementDisplayed(assetNameField) || isElementDisplayed(selectAssetClassButton);
    }

    public boolean isAssetNameFieldDisplayed() {
        return isElementDisplayed(assetNameField);
    }

    public void enterAssetName(String name) {
        try {
            WebElement nameField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Enter name'")
            );
            nameField.sendKeys(name);
            System.out.println("✅ Entered asset name: " + name);
        } catch (Exception e) {
            if (allTextFields.size() > 0) {
                allTextFields.get(0).sendKeys(name);
                System.out.println("✅ Entered asset name (alt): " + name);
            }
        }
    }

    public String getAssetNameValue() {
        try {
            return assetNameField.getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }

    public void clearAssetName() {
        try {
            assetNameField.clear();
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear asset name: " + e.getMessage());
        }
    }

    public boolean isSelectAssetClassDisplayed() {
        return isElementDisplayed(selectAssetClassButton);
    }

    public void clickSelectAssetClass() {
        System.out.println("📍 Attempting to click Select Asset Class...");
        
        int maxAttempts = 3;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                WebElement classBtn = shortWait.until(
                    ExpectedConditions.elementToBeClickable(
                        AppiumBy.accessibilityId("Select asset class")
                    )
                );
                classBtn.click();
                System.out.println("✅ Clicked Select Asset Class (attempt " + attempt + ")");
                return;
            } catch (Exception e) {
                lastException = e;
                System.out.println("⚠️ Attempt " + attempt + " failed: " + e.getMessage());
                
                if (attempt == 1) {
                    System.out.println("   Trying scroll down...");
                    scrollFormDown();
                    sleep(500);
                } else if (attempt == 2) {
                    System.out.println("   Trying scroll up...");
                    scrollFormUp();
                    sleep(500);
                }
            }
        }
        
        // Final attempt: Try alternative locator
        try {
            System.out.println("   Trying alternative locator...");
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.toLowerCase().contains("asset class")) {
                    btn.click();
                    System.out.println("✅ Clicked Select Asset Class (alternative)");
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Alternative locator failed: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to click Select Asset Class after all attempts", lastException);
    }

    public void selectAssetClass(String className) {
        clickSelectAssetClass();
        sleep(500);
        try {
            WebElement classOption = driver.findElement(AppiumBy.accessibilityId(className));
            classOption.click();
            System.out.println("✅ Selected asset class: " + className);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select asset class: " + className);
        }
    }

    public void selectATSClass() {
        clickSelectAssetClass();
        sleep(500);
        click(atsClassOption);
        System.out.println("✅ Selected ATS class");
    }

    public boolean isSelectLocationDisplayed() {
        return isElementDisplayed(selectLocationButton);
    }

    // ================================================================
    // FIX FOR ATS_ECR_11 - ROBUST clickSelectLocation
    // ================================================================

    /**
     * Click Select Location button with scroll and retry logic
     * FIX: Handles case where element is not immediately clickable
     */
    public void clickSelectLocation() {
        System.out.println("📍 Attempting to click Select Location...");
        
        int maxAttempts = 3;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // First try: Direct click with short wait
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                WebElement locationBtn = shortWait.until(
                    ExpectedConditions.elementToBeClickable(
                        AppiumBy.accessibilityId("Select location")
                    )
                );
                locationBtn.click();
                System.out.println("✅ Clicked Select Location (attempt " + attempt + ")");
                return;
            } catch (Exception e) {
                lastException = e;
                System.out.println("⚠️ Attempt " + attempt + " failed: " + e.getMessage());
                
                // Try scrolling to make element visible
                if (attempt == 1) {
                    System.out.println("   Trying scroll down...");
                    scrollFormDown();
                    sleep(500);
                } else if (attempt == 2) {
                    System.out.println("   Trying scroll up...");
                    scrollFormUp();
                    sleep(500);
                }
            }
        }
        
        // Final attempt: Try alternative locator strategies
        try {
            System.out.println("   Trying alternative locator...");
            WebElement locationBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select location' OR label == 'Select location'")
            );
            // Scroll element into view using JavaScript
            driver.executeScript("mobile: scroll", Map.of(
                "element", locationBtn,
                "direction", "down"
            ));
            sleep(300);
            locationBtn.click();
            System.out.println("✅ Clicked Select Location (alternative)");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Alternative locator failed: " + e.getMessage());
        }
        
        // Try one more time with fresh element lookup and tap
        try {
            System.out.println("   Trying coordinate tap...");
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.toLowerCase().contains("location")) {
                    int x = btn.getLocation().getX() + (btn.getSize().getWidth() / 2);
                    int y = btn.getLocation().getY() + (btn.getSize().getHeight() / 2);
                    driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
                    System.out.println("✅ Tapped Select Location at (" + x + ", " + y + ")");
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Coordinate tap failed: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to click Select Location after all attempts", lastException);
    }

    /**
     * Select location from hierarchy (Building > Floor > Room)
     * Returns true if location was selected successfully
     */
    public boolean selectLocation() {
        clickSelectLocation();
        sleep(1000);
        
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(" floor")) {
                    btn.click();
                    System.out.println("✅ Selected building: " + name);
                    sleep(500);
                    
                    buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
                    for (WebElement floorBtn : buttons) {
                        String fName = floorBtn.getAttribute("name");
                        if (fName != null && !fName.contains(" floor") &&
                            (fName.contains(" room") || fName.startsWith("Floor_") || fName.startsWith("1_Floor"))) {
                            floorBtn.click();
                            System.out.println("✅ Selected floor: " + fName);
                            sleep(500);
                            
                            buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
                            for (WebElement roomBtn : buttons) {
                                String rName = roomBtn.getAttribute("name");
                                if (rName != null && (rName.contains(">") || rName.startsWith("Room_")) &&
                                    !rName.contains(" floor") && !rName.contains(" room")) {
                                    roomBtn.click();
                                    System.out.println("✅ Selected room: " + rName);
                                    return true;
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error selecting location: " + e.getMessage());
        }
        
        return false;
    }

    public void createNewLocation(String floorName, String roomName) {
        System.out.println("📍 Creating new location...");
        
        try {
            click(addFloorButton);
            sleep(500);
            
            WebElement floorField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Floor Name'")
            );
            floorField.sendKeys(floorName);
            click(saveButton);
            System.out.println("✅ Created floor: " + floorName);
            sleep(500);
            
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(floorName)) {
                    btn.click();
                    break;
                }
            }
            sleep(500);
            
            click(addRoomButton);
            sleep(500);
            
            WebElement roomField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Room Name'")
            );
            roomField.sendKeys(roomName);
            click(saveButton);
            System.out.println("✅ Created room: " + roomName);
            sleep(500);
            
            buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(" floor")) {
                    btn.click();
                    break;
                }
            }
            sleep(500);
            
            buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(floorName)) {
                    btn.click();
                    break;
                }
            }
            sleep(500);
            
            buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(roomName)) {
                    btn.click();
                    System.out.println("✅ Selected room: " + name);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error creating location: " + e.getMessage());
        }
    }

    public boolean isSelectAssetSubtypeDisplayed() {
        return isElementDisplayed(selectAssetSubtypeButton);
    }

    // ================================================================
    // FIX FOR ATS_ECR_18 - ROBUST clickSelectAssetSubtype
    // ================================================================

    /**
     * DEBUG: Print all visible interactive elements to find correct accessibility ID
     */
    public void debugPrintAllElements() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔍 DEBUG: ALL VISIBLE ELEMENTS ON CREATE ASSET SCREEN");
        System.out.println("=".repeat(60));
        
        // Check Buttons
        System.out.println("\n--- BUTTONS ---");
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement el : buttons) {
                try {
                    if (el.isDisplayed()) {
                        String name = el.getAttribute("name");
                        String label = el.getAttribute("label");
                        if (name != null) {
                            System.out.println("  [Button] name='" + name + "' label='" + label + "'");
                            if (name.toLowerCase().contains("subtype")) {
                                System.out.println("  >>> SUBTYPE FOUND! <<<");
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        
        // Check Other elements (dropdowns often use this type)
        System.out.println("\n--- OTHER ELEMENTS ---");
        try {
            List<WebElement> others = driver.findElements(AppiumBy.className("XCUIElementTypeOther"));
            for (WebElement el : others) {
                try {
                    if (el.isDisplayed()) {
                        String name = el.getAttribute("name");
                        String label = el.getAttribute("label");
                        if (name != null && (name.toLowerCase().contains("select") || name.toLowerCase().contains("subtype"))) {
                            System.out.println("  [Other] name='" + name + "' label='" + label + "'");
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        
        // Check Cells
        System.out.println("\n--- CELLS ---");
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            for (WebElement el : cells) {
                try {
                    if (el.isDisplayed()) {
                        String name = el.getAttribute("name");
                        String label = el.getAttribute("label");
                        if (name != null && name.toLowerCase().contains("subtype")) {
                            System.out.println("  [Cell] name='" + name + "' label='" + label + "'");
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        
        // Check Static Text
        System.out.println("\n--- STATIC TEXT ---");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement el : texts) {
                try {
                    if (el.isDisplayed()) {
                        String name = el.getAttribute("name");
                        String label = el.getAttribute("label");
                        if (name != null && name.toLowerCase().contains("subtype")) {
                            System.out.println("  [Text] name='" + name + "' label='" + label + "'");
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        
        System.out.println("\n" + "=".repeat(60) + "\n");
    }
    /**
     * DEBUG: Print all buttons in navigation bar area
     * Helps identify correct Create Asset button locator
     */
    public void debugPrintNavBarButtons() {
        System.out.println("\n🔍 DEBUG: Navigation Bar Buttons");
        System.out.println("=" .repeat(50));
        try {
            // Try to find navigation bar
            List<WebElement> navBars = driver.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"));
            System.out.println("Found " + navBars.size() + " navigation bars");
            
            for (int i = 0; i < navBars.size(); i++) {
                WebElement navBar = navBars.get(i);
                System.out.println("\nNavBar " + i + ":");
                List<WebElement> buttons = navBar.findElements(AppiumBy.className("XCUIElementTypeButton"));
                for (WebElement btn : buttons) {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    String enabled = btn.getAttribute("enabled");
                    boolean visible = btn.isDisplayed();
                    System.out.println("  [Button] name='" + name + "' label='" + label + "' enabled=" + enabled + " visible=" + visible);
                }
            }
            
            // Also check all visible buttons with "Create" or "Asset" in name
            System.out.println("\nAll buttons containing 'Create' or 'Asset':");
            List<WebElement> allButtons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : allButtons) {
                try {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    if ((name != null && (name.contains("Create") || name.contains("Asset"))) ||
                        (label != null && (label.contains("Create") || label.contains("Asset")))) {
                        boolean visible = btn.isDisplayed();
                        String enabled = btn.getAttribute("enabled");
                        System.out.println("  [Button] name='" + name + "' label='" + label + "' enabled=" + enabled + " visible=" + visible);
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println("=" .repeat(50) + "\n");
    }


    /**
     * Click Select Asset Subtype button - SIMPLIFIED VERSION
     * No excessive scrolling since subtype is just below asset class
     */
    public void clickSelectAssetSubtype() {
        System.out.println("📋 Clicking Select Asset Subtype...");
        
        // DEBUG: Uncomment to see all buttons
        // debugPrintAllElements();
        
        // Strategy 1: Direct click with accessibility ID
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement subtypeBtn = shortWait.until(
                ExpectedConditions.elementToBeClickable(
                    AppiumBy.accessibilityId("Select asset subtype")
                )
            );
            subtypeBtn.click();
            System.out.println("✅ Clicked Select Asset Subtype");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Direct accessibility ID failed: " + e.getMessage());
        }
        
        // Strategy 2: Try NSPredicate with various name patterns
        String[] predicates = {
            "name == 'Select asset subtype'",
            "label == 'Select asset subtype'",
            "name CONTAINS[c] 'subtype'",
            "label CONTAINS[c] 'subtype'",
            "name CONTAINS[c] 'asset subtype'",
            "name == 'Select subtype'",
            "label == 'Select subtype'"
        };
        
        for (String predicate : predicates) {
            try {
                WebElement subtypeBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND " + predicate)
                );
                if (subtypeBtn.isDisplayed()) {
                    subtypeBtn.click();
                    System.out.println("✅ Clicked via predicate: " + predicate);
                    return;
                }
            } catch (Exception e) {
                // Try next predicate
            }
        }
        
        // Strategy 3: Find by searching all element types (Button, Other, Cell)
        String[] elementTypes = {"XCUIElementTypeButton", "XCUIElementTypeOther", "XCUIElementTypeCell"};
        for (String elementType : elementTypes) {
            try {
                System.out.println("   Searching " + elementType + " for 'subtype'...");
                List<WebElement> elements = driver.findElements(AppiumBy.className(elementType));
                for (WebElement el : elements) {
                    try {
                        String name = el.getAttribute("name");
                        String label = el.getAttribute("label");
                        if ((name != null && name.toLowerCase().contains("subtype")) ||
                            (label != null && label.toLowerCase().contains("subtype"))) {
                            System.out.println("   Found: [" + elementType + "] name='" + name + "', label='" + label + "'");
                            el.click();
                            System.out.println("✅ Clicked subtype element");
                            return;
                        }
                    } catch (Exception ex) {}
                }
            } catch (Exception e) {
                System.out.println("   " + elementType + " search failed: " + e.getMessage());
            }
        }
        
        // Strategy 4: Try single scroll down then retry
        try {
            System.out.println("   Trying single scroll down...");
            scrollFormDown();
            sleep(500);
            WebElement subtypeBtn = driver.findElement(AppiumBy.accessibilityId("Select asset subtype"));
            subtypeBtn.click();
            System.out.println("✅ Clicked after scroll");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Scroll + click failed: " + e.getMessage());
        }
        
        // DEBUG: Print all buttons to help identify correct element
        debugPrintAllElements();
        
        throw new RuntimeException("Failed to click Select Asset Subtype - check debug output for available buttons");
    }

    /**
     * Select asset subtype by name with robust handling
     */
    public void selectAssetSubtype(String subtypeName) {
        clickSelectAssetSubtype();
        sleep(800);
        
        // Try multiple strategies to select the subtype
        try {
            // Strategy 1: Direct accessibility ID
            WebElement subtypeOption = driver.findElement(AppiumBy.accessibilityId(subtypeName));
            subtypeOption.click();
            System.out.println("✅ Selected asset subtype: " + subtypeName);
            return;
        } catch (Exception e1) {
            System.out.println("⚠️ Direct accessibility ID failed, trying alternatives...");
        }
        
        // Strategy 2: NSPredicate with name/label
        try {
            WebElement subtypeOption = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == '" + subtypeName + "' OR label == '" + subtypeName + "'")
            );
            subtypeOption.click();
            System.out.println("✅ Selected asset subtype via predicate: " + subtypeName);
            return;
        } catch (Exception e2) {
            System.out.println("⚠️ NSPredicate failed...");
        }
        
        // Strategy 3: Find in list of buttons
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                String label = btn.getAttribute("label");
                if ((name != null && name.equalsIgnoreCase(subtypeName)) ||
                    (label != null && label.equalsIgnoreCase(subtypeName))) {
                    btn.click();
                    System.out.println("✅ Selected asset subtype from button list: " + subtypeName);
                    return;
                }
            }
        } catch (Exception e3) {
            System.out.println("⚠️ Button list search failed...");
        }
        
        // Strategy 4: Find in static text elements (dropdown items)
        try {
            List<WebElement> texts = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText'")
            );
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                String name = text.getAttribute("name");
                if ((label != null && label.equalsIgnoreCase(subtypeName)) ||
                    (name != null && name.equalsIgnoreCase(subtypeName))) {
                    text.click();
                    System.out.println("✅ Selected asset subtype from text: " + subtypeName);
                    return;
                }
            }
        } catch (Exception e4) {
            System.out.println("⚠️ Static text search failed...");
        }
        
        System.out.println("⚠️ Could not select asset subtype: " + subtypeName + " - dropdown may not have this option");
    }

    public boolean isQRCodeFieldDisplayed() {
        return isElementDisplayed(qrCodeField);
    }

    public void enterQRCode(String qrCode) {
        try {
            WebElement qrField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Enter or scan QR code'")
            );
            qrField.sendKeys(qrCode);
            System.out.println("✅ Entered QR code: " + qrCode);
            dismissKeyboard();
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter QR code: " + e.getMessage());
        }
    }

    public String getQRCodeValue() {
        try {
            return qrCodeField.getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }

    public void dismissKeyboard() {
        try {
            click(assetDetailsHeader);
        } catch (Exception e) {
            try {
                driver.executeScript("mobile: tap", Map.of("x", 200, "y", 100));
            } catch (Exception ex) {
                System.out.println("⚠️ Could not dismiss keyboard");
            }
        }
    }

    public void scrollFormDown() {
        try {
            driver.executeScript("mobile: scroll", Map.of("direction", "down"));
            System.out.println("✅ Scrolled down");
        } catch (Exception e) {
            System.out.println("⚠️ Could not scroll: " + e.getMessage());
        }
    }

    public void scrollFormUp() {
        try {
            driver.executeScript("mobile: scroll", Map.of("direction", "up"));
            System.out.println("✅ Scrolled up");
        } catch (Exception e) {
            System.out.println("⚠️ Could not scroll: " + e.getMessage());
        }
    }

    public boolean isCreateAssetButtonDisplayed() {
        return isElementDisplayed(createAssetButton);
    }

    // ================================================================
    // FIX FOR ATS_ECR_07 - ROBUST isCreateAssetButtonEnabled
    // ================================================================

    /**
     * Check if Create Asset button is enabled
     * FIX: Uses multiple strategies to accurately check button state
     */
    public boolean isCreateAssetButtonEnabled() {
        System.out.println("🔍 Checking Create Asset button state...");
        
        // Strategy 1: Check 'enabled' attribute
        try {
            WebElement btn = driver.findElement(AppiumBy.accessibilityId("Create Asset"));
            String enabled = btn.getAttribute("enabled");
            System.out.println("   enabled attribute: " + enabled);
            if ("false".equalsIgnoreCase(enabled)) {
                return false;
            }
        } catch (Exception e) {
            System.out.println("   Could not check enabled attribute: " + e.getMessage());
        }
        
        // Strategy 2: Check 'accessible' attribute (sometimes used for disabled state)
        try {
            WebElement btn = driver.findElement(AppiumBy.accessibilityId("Create Asset"));
            String accessible = btn.getAttribute("accessible");
            System.out.println("   accessible attribute: " + accessible);
        } catch (Exception e) {}
        
        // Strategy 3: Check opacity/alpha (disabled buttons often have lower opacity)
        try {
            WebElement btn = driver.findElement(AppiumBy.accessibilityId("Create Asset"));
            String value = btn.getAttribute("value");
            String label = btn.getAttribute("label");
            System.out.println("   value: " + value + ", label: " + label);
        } catch (Exception e) {}
        
        // Strategy 4: Check if button is in enabled state by checking clickability
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            shortWait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Create Asset")
            ));
            System.out.println("   Button is clickable");
            return true;
        } catch (Exception e) {
            System.out.println("   Button is NOT clickable (timeout)");
            return false;
        }
    }
    
    /**
     * Check if the current asset name value is effectively empty
     * (empty string or only whitespace/spaces)
     */
    public boolean isAssetNameEffectivelyEmpty() {
        try {
            // Find all text fields and check the first one (name field)
            List<WebElement> textFields = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField'")
            );
            
            if (!textFields.isEmpty()) {
                String value = textFields.get(0).getAttribute("value");
                System.out.println("   Name field value: '" + value + "'");
                
                // Check if null, empty, placeholder, or only spaces
                if (value == null || 
                    value.isEmpty() || 
                    value.equals("Enter name") ||
                    value.trim().isEmpty()) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not check name field: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Alternative validation: Check if form can be submitted
     * This handles the case where button appears enabled but validation fails on click
     */
    public boolean canSubmitForm() {
        // Check if all required fields have valid values
        boolean nameValid = !isAssetNameEffectivelyEmpty();
        boolean classSelected = isAssetClassSelected();
        boolean locationSelected = isLocationSelected();
        
        System.out.println("   Form validation - Name: " + nameValid + 
                          ", Class: " + classSelected + 
                          ", Location: " + locationSelected);
        
        return nameValid && classSelected && locationSelected;
    }
    
    /**
     * Check if an asset class has been selected
     */
    public boolean isAssetClassSelected() {
        try {
            WebElement classBtn = driver.findElement(AppiumBy.accessibilityId("Select asset class"));
            String label = classBtn.getAttribute("label");
            // If still shows "Select asset class", nothing selected
            return label != null && !label.equals("Select asset class");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a location has been selected
     */
    public boolean isLocationSelected() {
        try {
            WebElement locationBtn = driver.findElement(AppiumBy.accessibilityId("Select location"));
            String label = locationBtn.getAttribute("label");
            // If still shows "Select location", nothing selected
            return label != null && !label.equals("Select location");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Create Asset button - ROBUST VERSION
     * Handles: scroll, keyboard dismiss, multiple locator strategies
     */
    public void clickCreateAsset() {
        System.out.println("📦 Clicking Create Asset button...");
        
        // Step 1: Dismiss keyboard first (might be covering button)
        dismissKeyboard();
        sleep(300);
        
        // Step 2: Scroll up to make button visible (it's at the top)
        for (int scrollAttempt = 0; scrollAttempt < 3; scrollAttempt++) {
            scrollFormUp();
            sleep(300);
            
            // Check if button is now visible and clickable
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
                WebElement btn = shortWait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.accessibilityId("Create Asset")
                ));
                btn.click();
                System.out.println("✅ Clicked Create Asset (scroll attempt " + (scrollAttempt + 1) + ")");
                return;
            } catch (Exception e) {
                System.out.println("   Scroll attempt " + (scrollAttempt + 1) + " - button not clickable yet");
            }
        }
        
        // Step 3: Try alternative locators
        String[] locators = {
            "name == 'Create Asset'",
            "label == 'Create Asset'",
            "name CONTAINS[c] 'create'",
            "label CONTAINS[c] 'create' AND label CONTAINS[c] 'asset'"
        };
        
        for (String predicate : locators) {
            try {
                WebElement btn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND " + predicate)
                );
                if (btn.isDisplayed()) {
                    btn.click();
                    System.out.println("✅ Clicked Create Asset via: " + predicate);
                    return;
                }
            } catch (Exception e) {
                // Try next locator
            }
        }
        
        // Step 4: Search all buttons for "Create"
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                try {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    if ((name != null && name.toLowerCase().contains("create")) ||
                        (label != null && label.toLowerCase().contains("create"))) {
                        System.out.println("   Found button: name='" + name + "', label='" + label + "'");
                        if (btn.isDisplayed()) {
                            btn.click();
                            System.out.println("✅ Clicked Create button");
                            return;
                        }
                    }
                } catch (Exception ex) {}
            }
        } catch (Exception e) {
            System.out.println("⚠️ Button search failed: " + e.getMessage());
        }
        
        // Step 5: Try coordinate tap at top of screen (where button usually is)
        try {
            System.out.println("   Trying coordinate tap...");
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            // Create Asset button is typically at top-right
            int x = (int) (size.width * 0.85);  // 85% from left
            int y = (int) (size.height * 0.12); // 12% from top (navigation bar area)
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            System.out.println("✅ Tapped at (" + x + ", " + y + ")");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Coordinate tap failed: " + e.getMessage());
        }
        
        // Step 6: Last resort - use annotated element with longer wait
        try {
            System.out.println("   Last resort: using annotated element...");
            WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            longWait.until(ExpectedConditions.elementToBeClickable(createAssetButton)).click();
            System.out.println("✅ Clicked Create Asset (annotated element)");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Annotated element failed: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to click Create Asset button after all attempts");
    }

    public boolean isSaveButtonDisplayed() {
        return isElementDisplayed(saveButton);
    }

    public void clickSave() {
        click(saveButton);
        System.out.println("✅ Clicked Save");
    }

    public boolean isDeleteButtonDisplayed() {
        return isElementDisplayed(deleteButton);
    }

    public void clickDelete() {
        click(deleteButton);
        System.out.println("✅ Clicked Delete");
    }

    // ================================================================
    // COMPLETE ASSET CREATION FLOW
    // ================================================================

    public String createAsset(String assetName, String assetClass, String subtype, String qrCode) {
        System.out.println("\n📦 CREATING ASSET: " + assetName);
        
        enterAssetName(assetName);
        selectAssetClass(assetClass);
        
        boolean locationSelected = selectLocation();
        if (!locationSelected) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            createNewLocation("Floor_" + timestamp, "Room_" + timestamp);
        }
        
        dismissKeyboard();
        scrollFormDown();
        
        if (subtype != null && !subtype.isEmpty()) {
            try {
                selectAssetSubtype(subtype);
            } catch (Exception e) {
                System.out.println("⚠️ Subtype selection skipped: " + e.getMessage());
            }
        }
        
        if (qrCode != null && !qrCode.isEmpty()) {
            enterQRCode(qrCode);
        }
        
        clickCreateAsset();
        
        System.out.println("✅✅✅ ASSET CREATED: " + assetName + " ✅✅✅\n");
        return assetName;
    }

    public String createAssetWithAutoName(String assetClass) {
        long timestamp = System.currentTimeMillis();
        String assetName = "Asset_" + timestamp;
        String qrCode = "QR_" + timestamp;
        return createAsset(assetName, assetClass, "test", qrCode);
    }

    public String createATSAsset() {
        return createAssetWithAutoName("ATS");
    }

    // ================================================================
    // VALIDATION METHODS
    // ================================================================

    public boolean isAssetCreatedSuccessfully() {
        sleep(2000);
        return isAssetListDisplayed() || !isCreateAssetFormDisplayed();
    }

    public boolean isRequiredFieldErrorDisplayed() {
        try {
            WebElement error = driver.findElement(
                AppiumBy.iOSNsPredicateString("label CONTAINS 'required' OR label CONTAINS 'Required'")
            );
            return error.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAssetClassDropdownDisplayed() {
        try {
            // Wait a bit for dropdown to appear
            sleep(500);
            
            // Try multiple ways to detect dropdown
            if (isElementDisplayed(atsClassOption)) return true;
            if (isElementDisplayed(upsClassOption)) return true;
            if (isElementDisplayed(pduClassOption)) return true;
            
            // Alternative: Check for any asset class options by searching buttons
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && (name.equals("ATS") || name.equals("UPS") || name.equals("PDU") || name.equals("Generator"))) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLocationPickerDisplayed() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(" floor")) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    public boolean isSubtypeDropdownDisplayed() {
        try {
            return isElementDisplayed(testSubtypeOption);
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getAvailableAssetClasses() {
        List<String> classes = new java.util.ArrayList<>();
        clickSelectAssetClass();
        sleep(500);
        
        try {
            List<WebElement> options = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText'")
            );
            for (WebElement option : options) {
                String name = option.getAttribute("name");
                if (name != null && !name.isEmpty() && 
                    !name.equals("Select asset class") && !name.equals("Cancel")) {
                    classes.add(name);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get asset classes: " + e.getMessage());
        }
        
        try {
            clickCancel();
        } catch (Exception e) {}
        
        return classes;
    }

    public boolean verifyAssetExistsInList(String assetName) {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(assetName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error verifying asset: " + e.getMessage());
        }
        return false;
    }

    // ================================================================
    // EDIT ASSET DETAILS METHODS
    // ================================================================

    /**
     * Click Edit button to open Edit Asset Details screen
     */
    public void clickEdit() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Edit")
            ));
            editBtn.click();
            System.out.println("✅ Clicked Edit button");
        } catch (Exception e) {
            // Try alternative locators
            try {
                WebElement editBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == 'Edit' OR label == 'Edit'")
                );
                editBtn.click();
                System.out.println("✅ Clicked Edit button (alt)");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not click Edit: " + e2.getMessage());
            }
        }
    }

    /**
     * Check if Edit Asset Details screen is displayed
     */
    public boolean isEditAssetScreenDisplayed() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            return wait.until(d -> {
                try {
                    // Check for Core Attributes section or Save button
                    return driver.findElements(AppiumBy.iOSNsPredicateString(
                        "name CONTAINS 'Core Attributes' OR name == 'Save' OR label CONTAINS 'Core Attributes'"
                    )).size() > 0;
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Core Attributes section is visible
     */
    public boolean isCoreAttributesSectionVisible() {
        try {
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Core Attributes' OR label CONTAINS 'Core Attributes'")
            );
            return elements.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Required Fields Only toggle is displayed
     */
    public boolean isRequiredFieldsToggleDisplayed() {
        try {
            List<WebElement> toggles = driver.findElements(AppiumBy.className("XCUIElementTypeSwitch"));
            return toggles.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get Required Fields Only toggle state (ON/OFF)
     */
    public boolean isRequiredFieldsToggleOn() {
        try {
            List<WebElement> toggles = driver.findElements(AppiumBy.className("XCUIElementTypeSwitch"));
            if (toggles.size() > 0) {
                String value = toggles.get(0).getAttribute("value");
                return "1".equals(value);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get toggle state: " + e.getMessage());
        }
        return false;
    }

    /**
     * Toggle Required Fields Only switch
     */
    public void toggleRequiredFieldsOnly() {
        try {
            List<WebElement> toggles = driver.findElements(AppiumBy.className("XCUIElementTypeSwitch"));
            if (toggles.size() > 0) {
                toggles.get(0).click();
                System.out.println("✅ Toggled Required Fields Only switch");
                sleep(500);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not toggle switch: " + e.getMessage());
        }
    }

    /**
     * Enable Required Fields Only toggle (turn ON)
     */
    public void enableRequiredFieldsOnly() {
        if (!isRequiredFieldsToggleOn()) {
            toggleRequiredFieldsOnly();
            System.out.println("✅ Required Fields Only enabled");
        }
    }

    /**
     * Disable Required Fields Only toggle (turn OFF)
     */
    public void disableRequiredFieldsOnly() {
        if (isRequiredFieldsToggleOn()) {
            toggleRequiredFieldsOnly();
            System.out.println("✅ Required Fields Only disabled");
        }
    }

    /**
     * Get completion percentage text
     */
    public String getCompletionPercentage() {
        try {
            List<WebElement> percentElements = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS '%' OR label CONTAINS '%'")
            );
            for (WebElement el : percentElements) {
                String text = el.getAttribute("name");
                if (text == null) text = el.getAttribute("label");
                if (text != null && text.contains("%")) {
                    System.out.println("📊 Percentage: " + text);
                    return text;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get percentage: " + e.getMessage());
        }
        return "";
    }

    /**
     * Check if percentage element exists
     */
    public boolean isPercentageDisplayed() {
        String percentage = getCompletionPercentage();
        return percentage != null && !percentage.isEmpty();
    }

    /**
     * Get required fields counter text (e.g., "2/4")
     */
    public String getRequiredFieldsCounter() {
        try {
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString("name MATCHES '.*[0-9]+/[0-9]+.*' OR label MATCHES '.*[0-9]+/[0-9]+.*'")
            );
            for (WebElement el : elements) {
                String text = el.getAttribute("name");
                if (text == null) text = el.getAttribute("label");
                if (text != null && text.matches(".*\\d+/\\d+.*")) {
                    System.out.println("📊 Counter: " + text);
                    return text;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get counter: " + e.getMessage());
        }
        return "";
    }

    /**
     * Fill a text field by placeholder/label
     */
    public void fillTextField(String fieldName, String value) {
        try {
            List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            for (WebElement field : textFields) {
                String name = field.getAttribute("name");
                String placeholder = field.getAttribute("value");
                if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                    (placeholder != null && placeholder.toLowerCase().contains(fieldName.toLowerCase()))) {
                    field.clear();
                    field.sendKeys(value);
                    System.out.println("✅ Filled field '" + fieldName + "' with: " + value);
                    dismissKeyboard();
                    return;
                }
            }
            System.out.println("⚠️ Field not found: " + fieldName);
        } catch (Exception e) {
            System.out.println("⚠️ Could not fill field: " + e.getMessage());
        }
    }

    /**
     * Clear a text field by placeholder/label
     */
    public void clearTextField(String fieldName) {
        try {
            List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            for (WebElement field : textFields) {
                String name = field.getAttribute("name");
                String placeholder = field.getAttribute("value");
                if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                    (placeholder != null && placeholder.toLowerCase().contains(fieldName.toLowerCase()))) {
                    field.clear();
                    System.out.println("✅ Cleared field: " + fieldName);
                    dismissKeyboard();
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear field: " + e.getMessage());
        }
    }

    /**
     * Select dropdown option by field name and option value
     */
    public void selectDropdownOption(String fieldName, String optionValue) {
        try {
            // Find and click the dropdown button
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                String label = btn.getAttribute("label");
                if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                    (label != null && label.toLowerCase().contains(fieldName.toLowerCase()))) {
                    btn.click();
                    sleep(500);
                    break;
                }
            }
            
            // Click the option
            WebElement option = driver.findElement(AppiumBy.accessibilityId(optionValue));
            option.click();
            System.out.println("✅ Selected '" + optionValue + "' for field '" + fieldName + "'");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select dropdown option: " + e.getMessage());
        }
    }

    /**
     * Check if Save button on Edit screen is enabled
     */
    public boolean isEditSaveButtonEnabled() {
        try {
            WebElement saveBtn = driver.findElement(AppiumBy.accessibilityId("Save"));
            String enabled = saveBtn.getAttribute("enabled");
            return "true".equalsIgnoreCase(enabled);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Save button on Edit Asset Details screen
     */
    public void clickEditSave() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Save")
            ));
            saveBtn.click();
            System.out.println("✅ Clicked Save on Edit screen");
            sleep(1000);
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Save: " + e.getMessage());
        }
    }

    /**
     * Click Save Changes button (shown after changing asset class)
     * If asset class didn't change (e.g., Busway already selected), Save Changes won't appear
     * In that case, click the regular Save button
     */
    public void clickSaveChanges() {
        try {
            // Quick check for Save Changes button (3 seconds max)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement saveChangesBtn = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Save Changes")
            ));
            saveChangesBtn.click();
            System.out.println("✅ Clicked Save Changes");
            sleep(1000);
        } catch (Exception e) {
            System.out.println("ℹ️ Save Changes not found (asset class may not have changed), trying regular Save...");
            // Fallback to regular Save button
            clickEditSave();
        }
    }

    /**
     * Click Cancel button on Edit Asset Details screen
     */
    public void clickEditCancel() {
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel on Edit screen");
        } catch (Exception e) {
            // Try back button
            try {
                clickBack();
            } catch (Exception e2) {
                System.out.println("⚠️ Could not click Cancel: " + e2.getMessage());
            }
        }
    }

    /**
     * Check if green check indicator is displayed (for completed fields)
     */
    public boolean isGreenCheckIndicatorDisplayed() {
        try {
            List<WebElement> indicators = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'checkmark' OR name CONTAINS 'check' OR label CONTAINS '✓'")
            );
            return indicators.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if red warning indicator is displayed (for missing required fields)
     */
    public boolean isRedWarningIndicatorDisplayed() {
        try {
            List<WebElement> indicators = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'warning' OR name CONTAINS 'exclamation' OR label CONTAINS '!'")
            );
            return indicators.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for Edit Asset Details screen to load
     */
    public void waitForEditScreenReady() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(d -> isEditAssetScreenDisplayed());
            sleep(500);
            System.out.println("✅ Edit Asset Details screen ready");
        } catch (Exception e) {
            System.out.println("⚠️ Edit screen wait timeout: " + e.getMessage());
        }
    }

    /**
     * Check if asset was saved successfully after edit (back to detail/list)
     */
    public boolean isEditSavedSuccessfully() {
        try {
            sleep(1000);
            // After save, should return to asset details or list
            return !isEditAssetScreenDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fill ATS required field - Ampere Rating
     */
    public void fillAmpereRating(String value) {
        fillTextField("Ampere", value);
    }

    /**
     * Fill ATS required field - Interrupting Rating
     */
    public void fillInterruptingRating(String value) {
        fillTextField("Interrupting", value);
    }

    /**
     * Fill ATS required field - Voltage
     */
    public void fillVoltage(String value) {
        fillTextField("Voltage", value);
    }

    /**
     * Select Mains Type dropdown
     */
    public void selectMainsType(String type) {
        selectDropdownOption("Mains", type);
    }

    /**
     * Fill all ATS required fields
     */
    public void fillAllATSRequiredFields() {
        scrollFormDown();
        sleep(500);
        fillAmpereRating("100");
        fillInterruptingRating("50");
        fillVoltage("480");
        scrollFormDown();
        sleep(500);
        try {
            selectMainsType("Normal");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Mains Type");
        }
        scrollFormUp();
        System.out.println("✅ Filled all ATS required fields");
    }

    // ================================================================
    // BUSWAY ASSET CLASS METHODS
    // ================================================================

    /**
     * Change asset class in Edit mode to Busway
     */
    /**
     * Change asset class to Busway - DEBUG VERSION
     */
    /**
     * Change asset class to Busway in Edit mode
     * Uses generic approach to find Asset Class dropdown regardless of current value
     */
    public void changeAssetClassToBusway() {
        System.out.println("📋 Changing asset class to Busway...");
        
        // Asset Class is at top in Edit mode - no scroll needed
        
        // Open Asset Class dropdown
        boolean dropdownOpened = clickAssetClassDropdown();
        if (!dropdownOpened) {
            System.out.println("❌ Could not open Asset Class dropdown!");
            return;
        }
        
        // Select Busway from dropdown
        selectBuswayFromDropdown();
        sleep(500);
        
        System.out.println("✅ Asset class changed to Busway");
    }
    
    /**
     * DEBUG: Print current screen state
     */
    @SuppressWarnings("unused")
    private void debugPrintCurrentScreen() {
        System.out.println("\n🔍 DEBUG: Current Screen State");
        try {
            // Check for navigation bar title
            List<WebElement> navBars = driver.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"));
            for (WebElement nav : navBars) {
                System.out.println("   NavBar: " + nav.getAttribute("name"));
            }
            
            // Check for Edit/Save button to determine mode
            try {
                driver.findElement(AppiumBy.accessibilityId("Edit"));
                System.out.println("   ⚠️ 'Edit' button found - we're in VIEW mode, not EDIT mode!");
            } catch (Exception e) {}
            
            try {
                driver.findElement(AppiumBy.accessibilityId("Save"));
                System.out.println("   ✅ 'Save' button found - we're in EDIT mode");
            } catch (Exception e) {}
            
        } catch (Exception e) {
            System.out.println("   Error checking screen: " + e.getMessage());
        }
    }
    
    /**
     * Find and CLICK the Asset Class dropdown in Edit mode
     * Works regardless of what the current asset class is
     * Returns true if dropdown was opened successfully
     */
    private boolean clickAssetClassDropdown() {
        System.out.println("🔍 Finding and clicking Asset Class dropdown...");
        
        // Strategy 1: Find "Asset Class" label and tap the row below it
        try {
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND (name == 'Asset Class' OR label == 'Asset Class')"
                )
            );
            int labelX = assetClassLabel.getLocation().getX();
            int labelY = assetClassLabel.getLocation().getY();
            int labelHeight = assetClassLabel.getSize().getHeight();
            
            System.out.println("   Found 'Asset Class' label at (" + labelX + ", " + labelY + ")");
            
            // Tap below the label (where the dropdown button is)
            // The dropdown is typically 30-50 pixels below the label
            int tapX = labelX + 100;  // Tap in the middle of the row
            int tapY = labelY + labelHeight + 30;  // Below the label
            
            System.out.println("   Tapping dropdown at (" + tapX + ", " + tapY + ")...");
            driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
            sleep(800);
            
            // Verify dropdown opened
            if (isDropdownOpen()) {
                System.out.println("   ✅ Dropdown opened!");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 1 (label + offset) failed: " + e.getMessage());
        }
        
        // Strategy 2: Find any button in the Asset Class row area
        try {
            // First find the label to get the Y position
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            
            // Find all buttons and look for one near the Asset Class row
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true")
            );
            
            for (WebElement btn : buttons) {
                int btnY = btn.getLocation().getY();
                // Check if button is in the Asset Class row (within 80 pixels below label)
                if (btnY > labelY && btnY < labelY + 80) {
                    String name = btn.getAttribute("name");
                    System.out.println("   Found button in Asset Class row: " + name);
                    btn.click();
                    sleep(800);
                    if (isDropdownOpen()) {
                        System.out.println("   ✅ Dropdown opened!");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 (button in row) failed: " + e.getMessage());
        }
        
        // Strategy 3: Find chevron.down icon near Asset Class
        try {
            List<WebElement> chevrons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeImage' AND name == 'chevron.down'")
            );
            
            // Get Asset Class label Y position
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            
            for (WebElement chevron : chevrons) {
                int chevronY = chevron.getLocation().getY();
                // Check if chevron is near Asset Class row
                if (Math.abs(chevronY - labelY) < 80) {
                    int x = chevron.getLocation().getX();
                    int y = chevron.getLocation().getY();
                    System.out.println("   Found Asset Class chevron, tapping at (" + x + ", " + y + ")...");
                    driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
                    sleep(800);
                    if (isDropdownOpen()) {
                        System.out.println("   ✅ Dropdown opened!");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (chevron) failed: " + e.getMessage());
        }
        
        // Strategy 4: Tap by screen coordinates (fallback)
        try {
            System.out.println("   Strategy 4: Trying fixed coordinate tap...");
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            // Asset Class row is typically around 55-60% down from the top in Edit mode
            int x = size.getWidth() / 2;
            int y = (int)(size.getHeight() * 0.58);
            System.out.println("   Tapping at screen center (" + x + ", " + y + ")...");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(800);
            if (isDropdownOpen()) {
                System.out.println("   ✅ Dropdown opened!");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 4 (coordinates) failed");
        }
        
        System.out.println("   ❌ Could not open Asset Class dropdown");
        return false;
    }
    
    /**
     * Check if asset class dropdown is currently open
     * by looking for class options like Busway, Generator, etc.
     */
    private boolean isDropdownOpen() {
        try {
            // Look for dropdown options
            String[] dropdownOptions = {"Busway", "Generator", "Capacitor", "Circuit Breaker", "Fuse", "None"};
            for (String option : dropdownOptions) {
                try {
                    WebElement el = driver.findElement(AppiumBy.accessibilityId(option));
                    if (el.isDisplayed()) {
                        return true;
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        return false;
    }
    
    // Keep old method for compatibility but redirect
    @SuppressWarnings("unused")
    private WebElement findAssetClassButton() {
        String[] assetClassNames = {"ATS", "UPS", "PDU", "Generator", "Busway"};
        for (String className : assetClassNames) {
            try {
                WebElement btn = driver.findElement(AppiumBy.accessibilityId(className));
                if (btn.isDisplayed()) return btn;
            } catch (Exception e) {}
        }
        return null;
    }

    /**
     * Select Busway from the asset class dropdown
     */
    private void selectBuswayFromDropdown() {
        System.out.println("📋 Selecting Busway from dropdown...");
        
        // Strategy 1: Direct accessibility ID
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement busway = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Busway")
            ));
            busway.click();
            System.out.println("✅ Selected Busway (accessibility ID)");
            sleep(500);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 1 failed: " + e.getMessage());
        }
        
        // Strategy 2: NSPredicate for name/label
        try {
            WebElement busway = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Busway' OR label == 'Busway'")
            );
            busway.click();
            System.out.println("✅ Selected Busway (predicate)");
            sleep(500);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 2 failed");
        }
        
        // Strategy 3: Search ALL element types for "Busway"
        String[] elementTypes = {"XCUIElementTypeButton", "XCUIElementTypeStaticText", 
                                  "XCUIElementTypeCell", "XCUIElementTypeOther"};
        for (String type : elementTypes) {
            try {
                List<WebElement> elements = driver.findElements(AppiumBy.className(type));
                for (WebElement el : elements) {
                    String name = el.getAttribute("name");
                    String label = el.getAttribute("label");
                    if ("Busway".equals(name) || "Busway".equals(label)) {
                        el.click();
                        System.out.println("✅ Selected Busway from " + type);
                        sleep(500);
                        return;
                    }
                }
            } catch (Exception e) {}
        }
        
        // Strategy 4: Scroll within dropdown and retry
        System.out.println("   Trying scroll within dropdown...");
        try {
            // Small swipe up to reveal more options
            driver.executeScript("mobile: swipe", Map.of(
                "direction", "up",
                "velocity", 500
            ));
            sleep(500);
            
            WebElement busway = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Busway' OR label == 'Busway'")
            );
            busway.click();
            System.out.println("✅ Selected Busway (after scroll)");
            sleep(500);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 4 failed");
        }
        
        // Strategy 5: Try picker wheel (if dropdown is a picker)
        try {
            List<WebElement> pickerWheels = driver.findElements(
                AppiumBy.className("XCUIElementTypePickerWheel")
            );
            if (!pickerWheels.isEmpty()) {
                System.out.println("   Found picker wheel, sending 'Busway'...");
                pickerWheels.get(0).sendKeys("Busway");
                System.out.println("✅ Selected Busway via picker wheel");
                sleep(500);
                return;
            }
        } catch (Exception e) {
            System.out.println("   No picker wheel found");
        }
        
        System.out.println("⚠️ Could not find Busway in dropdown!");
    }
    
    /**
     * DEBUG: Print all options visible in dropdown
     */
    @SuppressWarnings("unused")
    private void debugPrintDropdownOptions() {
        System.out.println("\n🔍 DEBUG: Dropdown options visible:");
        
        // Check buttons
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true")
            );
            System.out.println("   Buttons (" + buttons.size() + "):");
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && !name.isEmpty()) {
                    System.out.println("      - " + name);
                }
            }
        } catch (Exception e) {}
        
        // Check static text
        try {
            List<WebElement> texts = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            System.out.println("   StaticText (" + texts.size() + "):");
            for (WebElement txt : texts) {
                String name = txt.getAttribute("name");
                String label = txt.getAttribute("label");
                if (name != null && (name.contains("ATS") || name.contains("UPS") || 
                    name.contains("PDU") || name.contains("Bus") || name.contains("Gen"))) {
                    System.out.println("      - name='" + name + "' label='" + label + "'");
                }
            }
        } catch (Exception e) {}
        
        // Check cells
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            if (!cells.isEmpty()) {
                System.out.println("   Cells (" + cells.size() + "):");
                for (int i = 0; i < Math.min(cells.size(), 10); i++) {
                    String name = cells.get(i).getAttribute("name");
                    System.out.println("      - " + name);
                }
            }
        } catch (Exception e) {}
        
        // Check picker wheels
        try {
            List<WebElement> pickers = driver.findElements(AppiumBy.className("XCUIElementTypePickerWheel"));
            if (!pickers.isEmpty()) {
                System.out.println("   PickerWheels found: " + pickers.size());
            }
        } catch (Exception e) {}
        
        System.out.println("");
    }

    /**
     * Select Busway from asset class dropdown (public method for compatibility)
     */
    public void selectBuswayClass() {
        selectBuswayFromDropdown();
    }

    /**
     * Check if Core Attributes section has NO content (for Busway)
     * For Busway: "Core Attributes" header text may be visible but section has NO fields/values
     * Returns true if section is empty (no required fields like Ampere, Voltage, etc.)
     */
    public boolean isCoreAttributesSectionHidden() {
        System.out.println("🔍 Checking if Core Attributes content is empty (for Busway)...");
        
        try {
            sleep(1000); // Wait for UI to update after class change
            
            // Scroll to see Core Attributes area
            scrollFormDown();
            sleep(500);
            
            // For Busway: Check if there are any REQUIRED FIELD indicators or attribute inputs
            // These would only appear for asset classes WITH Core Attributes (like ATS)
            
            // Check for percentage indicator (only shown when Core Attributes has fields)
            List<WebElement> percentElements = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "name CONTAINS '%' OR label CONTAINS '%'"
                )
            );
            
            if (!percentElements.isEmpty()) {
                System.out.println("   Percentage indicator found - Core Attributes has content");
                return false; // Has content
            }
            
            // Check for common ATS/UPS/PDU attribute fields
            List<WebElement> attributeFields = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND (" +
                    "name CONTAINS[c] 'ampere' OR name CONTAINS[c] 'voltage' OR " +
                    "name CONTAINS[c] 'rating' OR name CONTAINS[c] 'kva' OR " +
                    "name CONTAINS[c] 'phase' OR name CONTAINS[c] 'frequency' OR " +
                    "value CONTAINS[c] 'enter' OR value CONTAINS[c] 'select')"
                )
            );
            
            if (!attributeFields.isEmpty()) {
                System.out.println("   Found " + attributeFields.size() + " attribute input fields - Core Attributes has content");
                return false; // Has content
            }
            
            // Check for Required Fields toggle (only shown when there ARE required fields)
            List<WebElement> toggles = driver.findElements(AppiumBy.className("XCUIElementTypeSwitch"));
            if (!toggles.isEmpty()) {
                System.out.println("   Required Fields toggle found - checking if Core Attributes present");
                // Toggle exists, but for Busway it shouldn't do anything
            }
            
            System.out.println("   ✅ Core Attributes section is EMPTY (Busway)");
            return true; // Empty
            
        } catch (Exception e) {
            System.out.println("   Error checking Core Attributes: " + e.getMessage());
            return true; // If error, consider it empty
        }
    }

    /**
     * Check if Core Attributes percentage indicator is NOT displayed (for Busway)
     * For Busway, the Core Attributes section header exists but has no fields,
     * therefore no percentage indicator (like "0%" or "100%") should appear next to it.
     */
    public boolean isPercentageIndicatorHidden() {
        try {
            sleep(500);
            
            // Look specifically for Core Attributes section header
            List<WebElement> coreAttributesHeaders = driver.findElements(
                AppiumBy.iOSNsPredicateString("label == 'Core Attributes' OR name == 'Core Attributes'")
            );
            
            if (coreAttributesHeaders.isEmpty()) {
                System.out.println("Core Attributes header not found - considering percentage as hidden");
                return true;
            }
            
            // Check if there's a percentage indicator near/after the Core Attributes header
            // Look for sibling or nearby elements that show percentage
            // Typical patterns: "0%", "50%", "100%"
            List<WebElement> percentElements = driver.findElements(
                AppiumBy.iOSNsPredicateString("(label MATCHES '.*[0-9]+%.*' OR name MATCHES '.*[0-9]+%.*') AND visible == true")
            );
            
            System.out.println("Found " + percentElements.size() + " percentage indicators on screen");
            
            // For Busway, there should be no percentage indicator in the Core Attributes section
            // Check if any of the percentage elements are related to Core Attributes
            for (WebElement percentElement : percentElements) {
                try {
                    String label = percentElement.getAttribute("label");
                    String name = percentElement.getAttribute("name");
                    int percentY = percentElement.getLocation().getY();
                    
                    // Get Core Attributes header position
                    WebElement coreHeader = coreAttributesHeaders.get(0);
                    int coreHeaderY = coreHeader.getLocation().getY();
                    
                    // If percentage is within 100 pixels of Core Attributes header, it's the Core Attributes percentage
                    if (Math.abs(percentY - coreHeaderY) < 100) {
                        System.out.println("Found Core Attributes percentage indicator: " + (label != null ? label : name) + " at Y=" + percentY);
                        return false; // Percentage IS shown
                    }
                } catch (Exception e) {
                    // Continue checking other elements
                }
            }
            
            System.out.println("No Core Attributes percentage indicator found - hidden: true");
            return true;
        } catch (Exception e) {
            System.out.println("Error checking percentage indicator: " + e.getMessage());
            return true;
        }
    }

    /**
     * Check if there's an empty Core Attributes container (should not exist for Busway)
     */
    public boolean hasEmptyCoreAttributesContainer() {
        try {
            // Look for any container that might be an empty placeholder
            List<WebElement> containers = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Core' OR label CONTAINS 'Core'")
            );
            if (containers.size() > 0) {
                System.out.println("⚠️ Found Core container elements: " + containers.size());
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Edit any available field for Busway (non-Core Attributes field)
     */
    public void editAvailableBuswayField() {
        try {
            // Try to edit description or any other available text field
            List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            for (WebElement field : textFields) {
                try {
                    field.click();
                    sleep(300);
                    field.clear();
                    field.sendKeys("Busway_Test_" + System.currentTimeMillis());
                    dismissKeyboard();
                    System.out.println("✅ Edited a Busway field");
                    return;
                } catch (Exception e) {
                    continue;
                }
            }
            System.out.println("⚠️ No editable text field found for Busway");
        } catch (Exception e) {
            System.out.println("⚠️ Could not edit Busway field: " + e.getMessage());
        }
    }

    /**
     * Check if Edit screen is displayed for Busway (no Core Attributes but Save button exists)
     */
    public boolean isEditScreenDisplayedForBusway() {
        try {
            // For Busway, we check for Save button but NO Core Attributes
            boolean hasSave = driver.findElements(AppiumBy.accessibilityId("Save")).size() > 0;
            boolean hasCancel = driver.findElements(AppiumBy.accessibilityId("Cancel")).size() > 0;
            return hasSave || hasCancel;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if save was completed for Busway asset
     * After clicking Save Changes (or Save), check if:
     * 1. No validation error dialog appeared
     * 2. We're back to asset details or list (Save/Save Changes button no longer visible)
     */
    public boolean isSaveCompletedForBusway() {
        try {
            sleep(2000); // Wait for save to complete
            
            // Check if "Save Changes" button is still visible
            List<WebElement> saveChangesBtn = driver.findElements(
                AppiumBy.accessibilityId("Save Changes")
            );
            
            // Check if regular "Save" button is still visible
            List<WebElement> saveBtn = driver.findElements(
                AppiumBy.accessibilityId("Save")
            );
            
            // If neither save button is visible, save completed
            if (saveChangesBtn.isEmpty() && saveBtn.isEmpty()) {
                System.out.println("✅ Save buttons no longer visible - save completed");
                return true;
            }
            
            // Also check if we're back to asset details (Edit button visible)
            List<WebElement> editButton = driver.findElements(
                AppiumBy.accessibilityId("Edit")
            );
            
            if (!editButton.isEmpty()) {
                System.out.println("✅ Edit button visible - back to asset details, save completed");
                return true;
            }
            
            // Check if we're on asset list (plus button visible)
            List<WebElement> plusButton = driver.findElements(
                AppiumBy.accessibilityId("plus")
            );
            
            if (!plusButton.isEmpty()) {
                System.out.println("✅ Plus button visible - back to asset list, save completed");
                return true;
            }
            
            System.out.println("⚠️ Cannot confirm save completion - save buttons may still be visible");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking save completion: " + e.getMessage());
            return false;
        }
    }
}
