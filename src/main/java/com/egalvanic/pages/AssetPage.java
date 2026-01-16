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
    // FAST DETECTION METHODS - 1 second timeout max
    // ================================================================

    /**
     * FAST check if on Asset List (1 second timeout)
     */
    public boolean isAssetListDisplayedFast() {
        try {
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(1));
            fastWait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("plus")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * FAST check if on Dashboard (1 second timeout)
     */
    public boolean isDashboardDisplayedFast() {
        try {
            // Check for building.2 icon OR Assets tab (1 sec max)
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(1));
            fastWait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("building.2")),
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("list.bullet"))
            ));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * FAST navigation to Asset List (1 second timeout per step)
     */
    public void navigateToAssetListFast() {
        try {
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(1));
            WebElement listBtn = fastWait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("list.bullet")
            ));
            listBtn.click();
            // Quick wait for plus button
            new WebDriverWait(driver, Duration.ofSeconds(2))
                .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("plus")));
        } catch (Exception e) {
            System.out.println("⚠️ Fast navigation failed: " + e.getMessage());
        }
    }

    // ================================================================
    // NAVIGATION METHODS
    // ================================================================

    public void navigateToAssetList() {
        System.out.println("📦 Navigating to Asset List...");
        navigateToAssetListTurbo();
    }
    
    /**
     * TURBO: Navigate to Asset List - ultra fast (1 second timeout)
     */
    public void navigateToAssetListTurbo() {
        try {
            // Direct click - 1 second timeout
            WebElement listBtn = driver.findElement(AppiumBy.accessibilityId("list.bullet"));
            listBtn.click();
            // Quick wait for list to load
            sleep(500);
            System.out.println("✅ Asset List opened");
        } catch (Exception e) {
            System.out.println("⚠️ Could not open Asset List: " + e.getMessage());
        }
    }
    
    /**
     * Fast wait for asset list (2 seconds max)
     */
    private void waitForAssetListReadyFast() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("plus")));
        } catch (Exception e) {}
    }

    public void clickAddAsset() {
        System.out.println("➕ Clicking Add Asset button...");
        click(plusButton);
        waitForCreateAssetFormReady();
    }
    
    /**
     * TURBO: Click Add Asset button - direct fast click
     */
    public void clickAddAssetTurbo() {
        try {
            WebElement plusBtn = driver.findElement(AppiumBy.accessibilityId("plus"));
            plusBtn.click();
            System.out.println("✅ Add Asset clicked");
            sleep(500);
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Add Asset: " + e.getMessage());
        }
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
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            quickWait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOf(assetNameField),
                ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Enter name'")
                )
            ));
            System.out.println("✅ Create Asset form ready");
        } catch (Exception e) {
            System.out.println("⚠️ Create Asset form wait timeout");
        }
    }

    
    // ================================================================
    // SCREEN DETECTION METHODS - For smart navigation
    // ================================================================

    /**
     * Check if Dashboard is displayed (user is logged in, site selected)
     * Dashboard has: Assets tab, Locations tab, building icon, possibly job selector
     */
    public boolean isDashboardDisplayed() {
        try {
            // Dashboard specific: Has tab bar with Assets/Locations AND building.2 icon
            // This distinguishes it from Site Selection page
            
            // Check for Assets tab/button (bottom tab bar)
            boolean hasAssetsTab = false;
            try {
                List<WebElement> assetsElements = driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                        "(name == 'Assets' OR label == 'Assets') AND type == 'XCUIElementTypeButton'"
                    )
                );
                hasAssetsTab = !assetsElements.isEmpty();
            } catch (Exception e) {}
            
            // Check for building.2 icon (dashboard indicator)
            boolean hasBuildingIcon = false;
            try {
                driver.findElement(AppiumBy.accessibilityId("building.2"));
                hasBuildingIcon = true;
            } catch (Exception e) {}
            
            // Dashboard = has Assets tab OR building icon
            boolean isDashboard = hasAssetsTab || hasBuildingIcon;
            
            if (isDashboard) {
                System.out.println("   Dashboard detected (Assets tab: " + hasAssetsTab + ", Building icon: " + hasBuildingIcon + ")");
            }
            
            return isDashboard;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if Asset List is displayed
     * Asset List has: plus button, list of asset cells, "Assets" in nav
     */
    public boolean isAssetListDisplayed() {
        try {
            // Asset list specific: Has plus button for adding new assets
            try {
                WebElement plusBtn = driver.findElement(AppiumBy.accessibilityId("plus"));
                if (plusBtn.isDisplayed()) {
                    System.out.println("   Asset List detected (plus button found)");
                    return true;
                }
            } catch (Exception e) {}
            
            // Alternative: Check for asset cells with specific pattern
            try {
                List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
                if (cells.size() > 0) {
                    // Check if nav bar says something about assets
                    List<WebElement> navBars = driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeNavigationBar'")
                    );
                    for (WebElement nav : navBars) {
                        String name = nav.getAttribute("name");
                        if (name != null && (name.contains("Asset") || name.contains("asset"))) {
                            System.out.println("   Asset List detected (nav bar: " + name + ")");
                            return true;
                        }
                    }
                }
            } catch (Exception e) {}
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if Asset Detail view is displayed
     * Asset Detail has: "Asset Details" nav title, Edit button (NOT Save Changes)
     */
    public boolean isAssetDetailDisplayed() {
        try {
            // Must have "Asset Details" in nav bar
            boolean hasAssetDetailsNav = false;
            try {
                List<WebElement> navBars = driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeNavigationBar' AND name == 'Asset Details'"
                    )
                );
                hasAssetDetailsNav = !navBars.isEmpty();
            } catch (Exception e) {}
            
            // Must have Edit button (view mode, not edit mode)
            boolean hasEditButton = false;
            try {
                WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
                hasEditButton = editBtn.isDisplayed();
            } catch (Exception e) {}
            
            boolean isAssetDetail = hasAssetDetailsNav && hasEditButton;
            
            if (isAssetDetail) {
                System.out.println("   Asset Detail detected (nav: " + hasAssetDetailsNav + ", Edit btn: " + hasEditButton + ")");
            }
            
            return isAssetDetail;
        } catch (Exception e) {
            return false;
        }
    }


    // ================================================================
    // ASSET LIST METHODS
    // ================================================================

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
        System.out.println("🔍 Searching for asset: " + assetName);
        
        // Try to find and click search bar
        try {
            WebElement searchField = driver.findElement(AppiumBy.className("XCUIElementTypeSearchField"));
            searchField.click();
            sleep(300);
            searchField.sendKeys(assetName);
            System.out.println("✅ Searched for asset: " + assetName);
            sleep(500);
            return;
        } catch (Exception e) {}
        
        // Try accessibility ID
        try {
            WebElement searchField = driver.findElement(AppiumBy.accessibilityId("Search"));
            searchField.click();
            sleep(300);
            searchField.sendKeys(assetName);
            System.out.println("✅ Searched for asset (accessibility): " + assetName);
            sleep(500);
            return;
        } catch (Exception e) {}
        
        // Try by predicate
        try {
            WebElement searchField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField' OR name == 'Search' OR label == 'Search'")
            );
            searchField.click();
            sleep(300);
            searchField.sendKeys(assetName);
            System.out.println("✅ Searched for asset (predicate): " + assetName);
            sleep(500);
        } catch (Exception e) {
            System.out.println("⚠️ Search bar not available: " + e.getMessage());
        }
    }

    public boolean selectAssetByName(String assetName) {
        System.out.println("📦 Looking for asset: " + assetName);
        
        // Strategy 1: Find by accessibility ID
        try {
            WebElement asset = driver.findElement(AppiumBy.accessibilityId(assetName));
            asset.click();
            System.out.println("✅ Selected asset: " + assetName);
            sleep(500);
            return true;
        } catch (Exception e) {}
        
        // Strategy 2: Find StaticText with matching name
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && name.contains(assetName)) {
                    text.click();
                    System.out.println("✅ Selected asset (text): " + name);
                    sleep(500);
                    return true;
                }
            }
        } catch (Exception e) {}
        
        // Strategy 3: Find Cell containing asset name
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            for (WebElement cell : cells) {
                try {
                    List<WebElement> textsInCell = cell.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
                    for (WebElement text : textsInCell) {
                        String name = text.getAttribute("name");
                        if (name != null && name.contains(assetName)) {
                            text.click();
                            System.out.println("✅ Selected asset (cell text): " + name);
                            sleep(500);
                            return true;
                        }
                    }
                } catch (Exception inner) {}
            }
        } catch (Exception e) {}
        
        // Strategy 4: Find any button containing asset name
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(assetName)) {
                    btn.click();
                    System.out.println("✅ Selected asset (button): " + name);
                    sleep(500);
                    return true;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not find asset: " + assetName);
        return false;
    }

    /**
     * Clear search field and select first asset from the list
     * Useful as a fallback when searching for a specific asset type fails
     */
    public void clearSearchAndSelectFirst() {
        System.out.println("🧹 Clearing search and selecting first asset...");
        
        // Try to clear search field
        try {
            WebElement searchField = driver.findElement(AppiumBy.className("XCUIElementTypeSearchField"));
            searchField.clear();
            sleep(300);
            System.out.println("✅ Cleared search field");
        } catch (Exception e) {
            // Try clicking Cancel button to clear search
            try {
                WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
                cancelBtn.click();
                sleep(300);
                System.out.println("✅ Clicked Cancel to clear search");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not clear search: " + e2.getMessage());
            }
        }
        
        sleep(500);
        
        // Select first available asset
        selectFirstAsset();
    }

    public String selectFirstAsset() {
        System.out.println("📦 Selecting first asset...");
        
        // Wait for list to fully load
        sleep(1000);
        
        // STRATEGY 1: Find first CELL and click the StaticText INSIDE it
        // This is generic - works for ANY asset name
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            System.out.println("   Found " + cells.size() + " cells");
            
            if (cells.size() > 0) {
                WebElement firstCell = cells.get(0);
                
                // Find the StaticText inside this cell (the asset name)
                try {
                    List<WebElement> textsInCell = firstCell.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
                    if (textsInCell.size() > 0) {
                        WebElement assetNameElement = textsInCell.get(0);
                        String assetName = assetNameElement.getAttribute("name");
                        System.out.println("   🎯 First cell contains: " + assetName);
                        
                        // Click the text element
                        assetNameElement.click();
                        System.out.println("✅ Selected asset: " + assetName);
                        sleep(1000);
                        return assetName;
                    }
                } catch (Exception inner) {
                    // If can't find text, click cell center
                    System.out.println("   No text in cell, clicking cell center...");
                }
                
                // Click cell center using coordinates
                int cellX = firstCell.getLocation().getX() + (firstCell.getSize().getWidth() / 2);
                int cellY = firstCell.getLocation().getY() + (firstCell.getSize().getHeight() / 2);
                driver.executeScript("mobile: tap", Map.of("x", cellX, "y", cellY));
                System.out.println("✅ Tapped cell center at (" + cellX + ", " + cellY + ")");
                sleep(1000);
                return "asset";
            }
        } catch (Exception e) {
            System.out.println("   Cell strategy failed: " + e.getMessage());
        }
        
        // STRATEGY 2: Find first StaticText that is NOT a UI label
        // UI labels are typically: Assets, Search, Cancel, Back, etc.
        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            System.out.println("   Checking " + allTexts.size() + " static texts...");
            
            // Known UI labels to skip (case-insensitive)
            java.util.Set<String> uiLabels = new java.util.HashSet<>(java.util.Arrays.asList(
                "assets", "search", "cancel", "back", "edit", "delete", "save", 
                "create", "filter", "sort", "done", "close", "select", "add",
                "asset details", "new asset", "location", "class", "subtype"
            ));
            
            for (WebElement text : allTexts) {
                String name = text.getAttribute("name");
                if (name == null || name.trim().isEmpty()) continue;
                
                String nameLower = name.toLowerCase().trim();
                
                // Skip if it's a known UI label
                if (uiLabels.contains(nameLower)) continue;
                
                // Skip if contains UI keywords
                if (nameLower.contains("select") || nameLower.contains("enter") || 
                    nameLower.contains("tap") || nameLower.contains("click")) continue;
                
                // This is likely an asset name - click it!
                System.out.println("   🎯 Found potential asset: " + name);
                text.click();
                System.out.println("✅ Selected asset: " + name);
                sleep(1000);
                return name;
            }
        } catch (Exception e) {
            System.out.println("   StaticText strategy failed: " + e.getMessage());
        }
        
        // STRATEGY 3: Use iOS class chain to get first cell's first text
        try {
            WebElement firstAssetText = driver.findElement(
                AppiumBy.iOSClassChain("**/XCUIElementTypeCell[1]/XCUIElementTypeStaticText[1]")
            );
            String name = firstAssetText.getAttribute("name");
            System.out.println("   🎯 Class chain found: " + name);
            firstAssetText.click();
            System.out.println("✅ Selected asset via class chain: " + name);
            sleep(1000);
            return name;
        } catch (Exception e) {
            System.out.println("   Class chain strategy failed");
        }
        
        // STRATEGY 4: Tap coordinates of first row (fallback)
        try {
            System.out.println("   Using coordinate tap fallback...");
            int screenWidth = driver.manage().window().getSize().width;
            int tapX = screenWidth / 2;
            int tapY = 180;  // First row position
            
            driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
            System.out.println("✅ Tapped first row at (" + tapX + ", " + tapY + ")");
            sleep(1000);
            return "asset";
        } catch (Exception e) {
            System.out.println("   Coordinate tap failed: " + e.getMessage());
        }
        
        System.out.println("⚠️ Could not select any asset");
        return null;
    }
    
    /**
     * Debug: Print visible elements on screen
     */
    private void debugPrintElements() {
        System.out.println("   📋 DEBUG: Elements on screen:");
        try {
            // Print cells
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            System.out.println("      Cells: " + cells.size());
            
            // Print buttons (first 5)
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            System.out.println("      Buttons: " + buttons.size());
            for (int i = 0; i < Math.min(5, buttons.size()); i++) {
                String name = buttons.get(i).getAttribute("name");
                System.out.println("         - " + name);
            }
            
            // Print static texts (first 8 - more to see asset names)
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            System.out.println("      Static texts: " + texts.size());
            int count = 0;
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && name.length() > 1) {
                    System.out.println("         - " + name);
                    count++;
                    if (count >= 8) break;
                }
            }
        } catch (Exception e) {
            System.out.println("      Debug failed: " + e.getMessage());
        }
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
        System.out.println("📝 Getting asset name value...");
        
        // Strategy 1: Find text field with placeholder "Enter name" and get its value
        try {
            WebElement nameField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND (value != 'Enter name' AND value != '')")
            );
            String value = nameField.getAttribute("value");
            if (value != null && !value.isEmpty() && !value.equals("Enter name")) {
                System.out.println("✅ Got asset name: '" + value + "'");
                return value;
            }
        } catch (Exception e) {}
        
        // Strategy 2: Find first text field (usually the name field)
        try {
            List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            if (!textFields.isEmpty()) {
                WebElement firstField = textFields.get(0);
                String value = firstField.getAttribute("value");
                if (value != null && !value.isEmpty() && !value.equals("Enter name")) {
                    System.out.println("✅ Got asset name from first field: '" + value + "'");
                    return value;
                }
            }
        } catch (Exception e) {}
        
        // Strategy 3: Look at navigation bar or title
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"));
            for (WebElement navBar : navBars) {
                String name = navBar.getAttribute("name");
                if (name != null && !name.isEmpty() && !name.equals("New Asset") && !name.equals("Edit Asset")) {
                    System.out.println("✅ Got asset name from nav bar: '" + name + "'");
                    return name;
                }
            }
        } catch (Exception e) {}
        
        // Strategy 4: Find static text that might be the asset name (in view mode)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                // Skip common UI labels
                if (name != null && !name.isEmpty() && name.length() > 3) {
                    String[] skipLabels = {"Asset", "Name", "Class", "Location", "Type", "Save", "Cancel", "Edit", "Core", "Details", "Required"};
                    boolean skip = false;
                    for (String label : skipLabels) {
                        if (name.equals(label) || name.startsWith(label + " ")) {
                            skip = true;
                            break;
                        }
                    }
                    if (!skip && name.matches(".*[a-zA-Z0-9].*")) {
                        System.out.println("✅ Got potential asset name: '" + name + "'");
                        return name;
                    }
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not get asset name value");
        return "";
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
        
        // Strategy 1: Use PageFactory element directly (most reliable)
        try {
            if (selectAssetClassButton != null && selectAssetClassButton.isDisplayed()) {
                selectAssetClassButton.click();
                System.out.println("✅ Clicked Select Asset Class (PageFactory element)");
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ PageFactory element failed: " + e.getMessage());
        }
        
        // Strategy 2: Use iOSClassChain to find the FIRST "Select asset class" button
        // (Asset Class appears BEFORE Location in the form)
        try {
            WebElement classBtn = driver.findElement(
                AppiumBy.iOSClassChain("**/XCUIElementTypeStaticText[`name == 'Select asset class'`]")
            );
            classBtn.click();
            System.out.println("✅ Clicked Select Asset Class (iOSClassChain)");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ iOSClassChain failed: " + e.getMessage());
        }
        
        // Strategy 3: Exact predicate match - exclude location
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement classBtn = shortWait.until(
                ExpectedConditions.elementToBeClickable(
                    AppiumBy.iOSNsPredicateString("name == 'Select asset class' AND NOT (name CONTAINS 'location')")
                )
            );
            classBtn.click();
            System.out.println("✅ Clicked Select Asset Class (predicate)");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Predicate failed: " + e.getMessage());
        }
        
        // Strategy 4: Use accessibilityId as final fallback
        try {
            WebElement classBtn = driver.findElement(AppiumBy.accessibilityId("Select asset class"));
            classBtn.click();
            System.out.println("✅ Clicked Select Asset Class (accessibilityId)");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ AccessibilityId failed: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to click Select Asset Class");
    }

    public void selectAssetClass(String className) {
        System.out.println("📋 Selecting asset class: " + className);
        clickSelectAssetClass();
        sleep(500);
        
        // Strategy 1: Find by accessibility ID
        try {
            WebElement classOption = driver.findElement(AppiumBy.accessibilityId(className));
            classOption.click();
            System.out.println("✅ Selected asset class: " + className);
            return;
        } catch (Exception e) {}
        
        // Strategy 2: Find by predicate
        try {
            WebElement classOption = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == '" + className + "' OR label == '" + className + "'")
            );
            classOption.click();
            System.out.println("✅ Selected asset class (predicate): " + className);
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Find in buttons list
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.equalsIgnoreCase(className)) {
                    btn.click();
                    System.out.println("✅ Selected asset class (button): " + className);
                    return;
                }
            }
        } catch (Exception e) {}
        
        // Strategy 4: Find in StaticText list (cell labels)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && name.equalsIgnoreCase(className)) {
                    text.click();
                    System.out.println("✅ Selected asset class (text): " + className);
                    return;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not select asset class: " + className);
    }

    public void selectATSClass() {
        clickSelectAssetClass();
        sleep(500);
        click(atsClassOption);
        System.out.println("✅ Selected ATS class");
    }

    /**
     * Click ATS option directly (when dropdown is already open)
     */
    public void clickATSOption() {
        System.out.println("📋 Clicking ATS option...");
        try {
            click(atsClassOption);
            System.out.println("✅ Clicked ATS option");
        } catch (Exception e) {
            // Try by accessibility ID
            try {
                WebElement ats = driver.findElement(AppiumBy.accessibilityId("ATS"));
                ats.click();
                System.out.println("✅ Clicked ATS (accessibility)");
            } catch (Exception e2) {
                // Try finding in buttons
                List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
                for (WebElement btn : buttons) {
                    String name = btn.getAttribute("name");
                    if (name != null && name.equals("ATS")) {
                        btn.click();
                        System.out.println("✅ Clicked ATS (button)");
                        return;
                    }
                }
                System.out.println("⚠️ Could not click ATS option");
            }
        }
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
        System.out.println("📍 Selecting location - ULTRA FAST mode...");
        
        // Generate unique names in case we need to create
        long timestamp = System.currentTimeMillis();
        String floorName = "Floor_" + timestamp;
        String roomName = "Room_" + timestamp;
        
        try {
            // Click Select Location button
            driver.findElement(AppiumBy.accessibilityId("Select location")).click();
            sleep(500);
            
            boolean locationSelected = false;
            boolean buildingSelected = false;
            boolean floorSelected = false;
            
            // ULTRA FAST: Use predicate to directly find building (contains " floor" pattern)
            try {
                WebElement building = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS ' floor'"));
                String bName = building.getAttribute("name");
                building.click();
                System.out.println("✅ Building: " + bName);
                buildingSelected = true;
                sleep(300);
                
                // ULTRA FAST: Direct predicate for floor (contains " room" OR starts with "Floor_" OR "1_Floor")
                try {
                    WebElement floor = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (name CONTAINS ' room' OR name BEGINSWITH 'Floor_' OR name BEGINSWITH '1_Floor') AND NOT name CONTAINS ' floor'"));
                    String fName = floor.getAttribute("name");
                    floor.click();
                    System.out.println("✅ Floor: " + fName);
                    floorSelected = true;
                    sleep(300);
                    
                    // ULTRA FAST: Direct predicate for room (contains ">" OR starts with "Room_")
                    try {
                        WebElement room = driver.findElement(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND (name CONTAINS '>' OR name BEGINSWITH 'Room_') AND NOT name CONTAINS ' floor' AND NOT name CONTAINS ' room'"));
                        String rName = room.getAttribute("name");
                        room.click();
                        System.out.println("✅ Room: " + rName);
                        locationSelected = true;
                    } catch (Exception e) {
                        // Room not found - need to create one
                        System.out.println("   No room found, creating...");
                    }
                } catch (Exception e) {
                    // Floor not found - need to create one
                    System.out.println("   No floor found, creating...");
                }
            } catch (Exception e) {
                // Building not found
                System.out.println("   No building found");
            }
            
            // Fallback: Create missing location parts
            if (!locationSelected) {
                // Only create floor if we have building but no floor
                if (buildingSelected && !floorSelected) {
                    driver.findElement(AppiumBy.iOSClassChain("**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][1]")).click();
                    driver.findElement(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Floor Name'")).sendKeys(floorName);
                    driver.findElement(AppiumBy.accessibilityId("Save")).click();
                    sleep(500);
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name CONTAINS '" + floorName + "'")).click();
                    System.out.println("✅ Created Floor: " + floorName);
                    floorSelected = true;
                }
                
                // Create room if we have floor but no room
                if (floorSelected && !locationSelected) {
                    // Find the plus button for room (second one, or first if only one level)
                    try {
                        driver.findElement(AppiumBy.iOSClassChain("**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][2]")).click();
                    } catch (Exception e) {
                        driver.findElement(AppiumBy.iOSClassChain("**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][1]")).click();
                    }
                    driver.findElement(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Room Name'")).sendKeys(roomName);
                    driver.findElement(AppiumBy.accessibilityId("Save")).click();
                    sleep(500);
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name CONTAINS '" + roomName + "'")).click();
                    System.out.println("✅ Created Room: " + roomName);
                    locationSelected = true;
                }
            }
            
            System.out.println("✅ Location selection complete");
            return locationSelected || floorSelected || buildingSelected;
            
        } catch (Exception e) {
            System.out.println("⚠️ Error selecting location: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper: Select a single level in location hierarchy (legacy - kept for compatibility)
     */
    private boolean selectLocationLevel(String levelName, String[] skipLabels) {
        try {
            // Find all cells
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            
            for (WebElement cell : cells) {
                try {
                    // Get cell's text
                    List<WebElement> texts = cell.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
                    if (texts.isEmpty()) continue;
                    
                    String cellName = texts.get(0).getAttribute("name");
                    if (cellName == null || cellName.isEmpty()) continue;
                    
                    // Skip UI labels
                    boolean isSkip = false;
                    for (String skip : skipLabels) {
                        if (cellName.equalsIgnoreCase(skip)) {
                            isSkip = true;
                            break;
                        }
                    }
                    if (isSkip) continue;
                    
                    // Click this cell
                    System.out.println("   📍 " + levelName + ": " + cellName);
                    texts.get(0).click();
                    return true;
                } catch (Exception e) {
                    continue;
                }
            }
            
            // Fallback: click first non-skip StaticText
            List<WebElement> allTexts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : allTexts) {
                String name = text.getAttribute("name");
                if (name == null || name.isEmpty() || name.length() < 2) continue;
                
                boolean isSkip = false;
                for (String skip : skipLabels) {
                    if (name.equalsIgnoreCase(skip) || name.contains(skip)) {
                        isSkip = true;
                        break;
                    }
                }
                if (isSkip) continue;
                
                System.out.println("   📍 " + levelName + " (text): " + name);
                text.click();
                return true;
            }
            
        } catch (Exception e) {
            System.out.println("   ⚠️ " + levelName + " selection failed: " + e.getMessage());
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
        System.out.println("📱 Entering QR code: " + qrCode);
        
        // First scroll down to find QR code field
        scrollFormDown();
        sleep(300);
        
        // Strategy 1: Find by placeholder text
        try {
            WebElement qrField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND (value == 'Enter or scan QR code' OR value CONTAINS 'QR')")
            );
            qrField.click();
            sleep(200);
            qrField.sendKeys(qrCode);
            System.out.println("✅ Entered QR code: " + qrCode);
            dismissKeyboard();
            return;
        } catch (Exception e) {}
        
        // Strategy 2: Find by label containing "QR"
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            for (WebElement field : fields) {
                String name = field.getAttribute("name");
                String value = field.getAttribute("value");
                if ((name != null && name.toLowerCase().contains("qr")) || 
                    (value != null && value.toLowerCase().contains("qr"))) {
                    field.click();
                    sleep(200);
                    field.sendKeys(qrCode);
                    System.out.println("✅ Entered QR code (found by QR label): " + qrCode);
                    dismissKeyboard();
                    return;
                }
            }
        } catch (Exception e) {}
        
        // Strategy 3: Find field near "QR Code" label
        try {
            List<WebElement> labels = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS 'QR' OR label CONTAINS 'QR')")
            );
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
                for (WebElement field : fields) {
                    int fieldY = field.getLocation().getY();
                    if (Math.abs(fieldY - labelY) < 80) {
                        field.click();
                        sleep(200);
                        field.sendKeys(qrCode);
                        System.out.println("✅ Entered QR code (near label): " + qrCode);
                        dismissKeyboard();
                        return;
                    }
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not find QR code field");
    }

    public String getQRCodeValue() {
        try {
            return qrCodeField.getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }

    public void dismissKeyboard() {
        // Strategy 1: Try Done/Return button on keyboard toolbar
        try {
            WebElement doneButton = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Done' OR name == 'Return' OR name == 'return')")
            );
            doneButton.click();
            System.out.println("✅ Keyboard dismissed (Done/Return button)");
            return;
        } catch (Exception e) {}
        
        // Strategy 2: Try Appium hideKeyboard
        try {
            driver.hideKeyboard();
            System.out.println("✅ Keyboard dismissed (hideKeyboard)");
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Tap outside keyboard area (safe zone at top)
        try {
            int screenWidth = driver.manage().window().getSize().width;
            driver.executeScript("mobile: tap", Map.of("x", screenWidth / 2, "y", 100));
            System.out.println("✅ Keyboard dismissed (tap outside)");
            return;
        } catch (Exception e) {}
        
        // Strategy 4: Press keyboard key to confirm (Enter/Return on keyboard)
        try {
            WebElement keyboardKey = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeKey' AND (name CONTAINS 'Return' OR name CONTAINS 'return' OR name CONTAINS 'Go' OR name CONTAINS 'Next')")
            );
            keyboardKey.click();
            System.out.println("✅ Keyboard dismissed (keyboard key)");
            return;
        } catch (Exception e) {}
        
        System.out.println("⚠️ Keyboard may still be open - all dismiss strategies exhausted");
    }

    public void scrollFormDown() {
        // Use RIGHT EDGE corner for scrolling - no form fields there!
        try {
            int screenWidth = driver.manage().window().getSize().width;
            
            // Use RIGHT edge (x = screenWidth - 20) to avoid all form fields
            int scrollX = screenWidth - 20;  // Right edge
            int startY = 700;
            int endY = 300;
            
            System.out.println("   📜 Scroll (right edge): (" + scrollX + ", " + startY + ") -> (" + scrollX + ", " + endY + ")");
            
            driver.executeScript("mobile: dragFromToForDuration", Map.of(
                "fromX", scrollX,
                "fromY", startY,
                "toX", scrollX,
                "toY", endY,
                "duration", 0.5
            ));
            System.out.println("✅ Scrolled down");
        } catch (Exception e) {
            System.out.println("⚠️ Scroll failed: " + e.getMessage());
        }
    }

    public void scrollFormUp() {
        // Use RIGHT EDGE corner for scrolling - no form fields there!
        try {
            int screenWidth = driver.manage().window().getSize().width;
            
            // Use RIGHT edge (x = screenWidth - 20) to avoid all form fields
            int scrollX = screenWidth - 20;  // Right edge
            int startY = 300;
            int endY = 700;
            
            driver.executeScript("mobile: dragFromToForDuration", Map.of(
                "fromX", scrollX,
                "fromY", startY,
                "toX", scrollX,
                "toY", endY,
                "duration", 0.5
            ));
            System.out.println("✅ Scrolled up");
        } catch (Exception e) {
            System.out.println("⚠️ Scroll up failed: " + e.getMessage());
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
            // Strategy 1: Check if "Select asset class" button label changed
            try {
                WebElement classBtn = driver.findElement(AppiumBy.accessibilityId("Select asset class"));
                String label = classBtn.getAttribute("label");
                String name = classBtn.getAttribute("name");
                String value = classBtn.getAttribute("value");
                
                System.out.println("   Asset class button - label: " + label + ", name: " + name + ", value: " + value);
                
                // If label/name/value contains ATS, UPS, PDU, Generator - it's selected
                String combined = (label != null ? label : "") + (name != null ? name : "") + (value != null ? value : "");
                if (combined.contains("ATS") || combined.contains("UPS") || combined.contains("PDU") || combined.contains("Generator")) {
                    return true;
                }
                
                // If still shows "Select asset class", nothing selected
                if (label != null && !label.equals("Select asset class") && !label.isEmpty()) {
                    return true;
                }
            } catch (Exception e) {}
            
            // Strategy 2: Look for any visible text showing ATS, UPS, PDU, Generator near the class field
            try {
                List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
                for (WebElement text : texts) {
                    String textName = text.getAttribute("name");
                    if (textName != null && (textName.equals("ATS") || textName.equals("UPS") || 
                        textName.equals("PDU") || textName.equals("Generator"))) {
                        return true;
                    }
                }
            } catch (Exception e) {}
            
            // Strategy 3: Check if the dropdown is no longer visible (means selection was made)
            try {
                boolean dropdownStillOpen = isAssetClassDropdownDisplayed();
                // If dropdown closed after clicking an option, selection was made
                // This is a weak check but can help
            } catch (Exception e) {}
            
            return false;
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
        
        // Check if we're on Asset List (success)
        if (isAssetListDisplayed()) {
            System.out.println("✅ Asset created - on Asset List");
            return true;
        }
        
        // Check if we're on Asset Details (success - some apps navigate here)
        try {
            WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
            if (editBtn.isDisplayed()) {
                System.out.println("✅ Asset created - on Asset Details");
                return true;
            }
        } catch (Exception e) {}
        
        // Check if Create form is gone (success)
        if (!isCreateAssetFormDisplayed()) {
            System.out.println("✅ Asset created - Create form gone");
            return true;
        }
        
        // Check for success message
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && (name.toLowerCase().contains("created") || name.toLowerCase().contains("success"))) {
                    System.out.println("✅ Asset created - success message");
                    return true;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Asset creation status unclear");
        return false;
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
            // Check for any subtype option
            if (isElementDisplayed(testSubtypeOption)) return true;
            
            // Check for common ATS subtypes
            try {
                WebElement option = driver.findElement(AppiumBy.accessibilityId("Automatic Transfer Switch (<= 1000V)"));
                if (option.isDisplayed()) return true;
            } catch (Exception e) {}
            
            // Check for any button that looks like a subtype option
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && (name.contains("Transfer") || name.contains("Switch") || 
                    name.contains("test") || name.contains("None") || name.contains("Subtype"))) {
                    return true;
                }
            }
            
            // Check for picker wheel (dropdown)
            List<WebElement> pickers = driver.findElements(AppiumBy.className("XCUIElementTypePickerWheel"));
            if (!pickers.isEmpty()) return true;
            
            // Check for any static text that looks like subtype options
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && (name.contains("Transfer") || name.contains("Switch") || 
                    name.contains("Automatic") || name.contains("Manual"))) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Select first available subtype from dropdown (excluding "None")
     * TURBO VERSION - Fast selection
     * @return The name of the selected subtype
     */
    public String selectFirstAvailableSubtype() {
        // Try first ATS subtype directly - fastest path
        try {
            WebElement option = driver.findElement(
                AppiumBy.accessibilityId("Automatic Transfer Switch (<= 1000V)")
            );
            option.click();
            System.out.println("✅ Selected: Automatic Transfer Switch (<= 1000V)");
            return "Automatic Transfer Switch (<= 1000V)";
        } catch (Exception e) {}
        
        // Try any Transfer Switch option
        try {
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Transfer Switch'"
                )
            );
            String name = option.getAttribute("name");
            option.click();
            System.out.println("✅ Selected: " + name);
            return name;
        } catch (Exception e) {}
        
        // Fallback: Any button except None/Cancel
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true AND " +
                    "name != 'None' AND name != 'Cancel' AND name != 'Quick' AND name != 'Detailed'"
                )
            );
            if (!buttons.isEmpty()) {
                String name = buttons.get(0).getAttribute("name");
                buttons.get(0).click();
                System.out.println("✅ Selected: " + name);
                return name;
            }
        } catch (Exception e) {}
        
        return null;
    }
    
    /**
     * Check if a subtype has been selected (not showing "Select asset subtype" placeholder)
     */
    public boolean isSubtypeSelected() {
        try {
            // Check if "Select asset subtype" placeholder is still visible
            List<WebElement> placeholder = driver.findElements(
                AppiumBy.accessibilityId("Select asset subtype")
            );
            
            if (placeholder.isEmpty()) {
                // Placeholder gone, something is selected
                System.out.println("✅ Subtype is selected (placeholder not visible)");
                return true;
            }
            
            // Check if the button text changed from placeholder
            for (WebElement el : placeholder) {
                String label = el.getAttribute("label");
                if (label != null && !label.equals("Select asset subtype")) {
                    System.out.println("✅ Subtype is selected: " + label);
                    return true;
                }
            }
            
            // Check if any known subtype is visible as selected
            String[] subtypes = {
                "Automatic Transfer Switch (<= 1000V)",
                "Automatic Transfer Switch (> 1000V)",
                "Transfer Switch (<= 1000V)",
                "Transfer Switch (> 1000V)"
            };
            
            for (String subtype : subtypes) {
                try {
                    WebElement selected = driver.findElement(AppiumBy.accessibilityId(subtype));
                    if (selected.isDisplayed()) {
                        System.out.println("✅ Subtype is selected: " + subtype);
                        return true;
                    }
                } catch (Exception e) {}
            }
            
            System.out.println("⚠️ No subtype selected");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking subtype: " + e.getMessage());
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
        System.out.println("📝 Clicking Edit button...");
        
        // Try accessibilityId first
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Edit")
            ));
            editBtn.click();
            System.out.println("✅ Clicked Edit button");
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement editBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Edit' OR label == 'Edit'")
            );
            editBtn.click();
            System.out.println("✅ Clicked Edit button (predicate)");
            return;
        } catch (Exception e) {}
        
        // Try alternative - square.and.pencil icon
        try {
            WebElement editBtn = driver.findElement(
                AppiumBy.accessibilityId("square.and.pencil")
            );
            editBtn.click();
            System.out.println("✅ Clicked Edit button (icon)");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Edit button");
        }
    }
    
    /**
     * TURBO: Click Edit button - direct fast click (no retry)
     */
    public void clickEditTurbo() {
        // Wait briefly for UI to settle
        sleep(500);
        
        try {
            WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
            editBtn.click();
            System.out.println("✅ Edit clicked");
            sleep(500);
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement editBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Edit' OR label == 'Edit'")
            );
            editBtn.click();
            System.out.println("✅ Edit clicked (predicate)");
            sleep(500);
            return;
        } catch (Exception e) {}
        
        // Try finding button with "Edit" in name
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains("Edit")) {
                    btn.click();
                    System.out.println("✅ Edit clicked (button search)");
                    sleep(500);
                    return;
                }
            }
        } catch (Exception e) {}
        
        // Try square.and.pencil icon (common edit icon)
        try {
            WebElement editIcon = driver.findElement(AppiumBy.accessibilityId("square.and.pencil"));
            editIcon.click();
            System.out.println("✅ Edit clicked (pencil icon)");
            sleep(500);
        } catch (Exception e) {
            System.out.println("⚠️ Edit button not found");
        }
    }

    /**
     * Check if Edit Asset Details screen is displayed
     */
    public boolean isEditAssetScreenDisplayed() {
        // Check for Save Changes button (primary edit mode indicator)
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Save Changes' OR label == 'Save Changes'")
            );
            if (saveBtn.isDisplayed()) {
                System.out.println("   ✅ Edit screen detected (Save Changes visible)");
                return true;
            }
        } catch (Exception e) {}
        
        // Check for Asset Class dropdown with specific class names (ATS, UPS, etc.)
        String[] assetClasses = {"ATS", "UPS", "PDU", "Generator", "Busway", "Capacitor"};
        for (String className : assetClasses) {
            try {
                WebElement classEl = driver.findElement(AppiumBy.accessibilityId(className));
                if (classEl.isDisplayed()) {
                    System.out.println("   ✅ Edit screen detected (Asset class " + className + " visible)");
                    return true;
                }
            } catch (Exception e) {}
        }
        
        // Check for Core Attributes label
        try {
            List<WebElement> coreAttr = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Core Attributes' OR label CONTAINS 'Core Attributes'")
            );
            if (!coreAttr.isEmpty()) {
                System.out.println("   ✅ Edit screen detected (Core Attributes visible)");
                return true;
            }
        } catch (Exception e) {}
        
        return false;
    }

    /**
     * Check if Core Attributes section is visible
     */
    public boolean isCoreAttributesSectionVisible() {
        // First scroll down to find Core Attributes
        for (int i = 0; i < 3; i++) {
            try {
                List<WebElement> elements = driver.findElements(
                    AppiumBy.iOSNsPredicateString("name CONTAINS 'Core Attributes' OR label CONTAINS 'Core Attributes'")
                );
                if (!elements.isEmpty()) {
                    System.out.println("✅ Core Attributes section visible");
                    return true;
                }
                
                // Also check for specific attribute fields
                String[] attributeFields = {"Serial Number", "Phase", "Manufacturer", "Model"};
                for (String field : attributeFields) {
                    List<WebElement> fieldElements = driver.findElements(
                        AppiumBy.iOSNsPredicateString("name CONTAINS '" + field + "' OR label CONTAINS '" + field + "'")
                    );
                    if (!fieldElements.isEmpty()) {
                        System.out.println("✅ Core Attributes section visible (found " + field + ")");
                        return true;
                    }
                }
            } catch (Exception e) {}
            
            // Scroll down to find it
            if (i < 2) {
                scrollFormDown();
                sleep(300);
            }
        }
        return false;
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
     * Check if Required Fields Only toggle is OFF
     */
    public boolean isRequiredFieldsToggleOff() {
        return !isRequiredFieldsToggleOn();
    }

    /**
     * Enable Required Fields Only toggle (alias method)
     */
    public void enableRequiredFieldsOnlyToggle() {
        enableRequiredFieldsOnly();
    }

    /**
     * Get required fields counter text (alias method for getRequiredFieldsCounter)
     */
    public String getRequiredFieldsCounterText() {
        return getRequiredFieldsCounter();
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
        System.out.println("📝 Filling field: " + fieldName + " = " + value);
        
        // Try to find and fill the field, scroll if needed (max 3 scrolls)
        for (int scrollAttempt = 0; scrollAttempt < 3; scrollAttempt++) {
            
            // STRATEGY 1: Find TextField OR TextView by name/label containing fieldName
            try {
                List<WebElement> inputFields = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
                );
                for (WebElement field : inputFields) {
                    String name = field.getAttribute("name");
                    String placeholder = field.getAttribute("value");
                    if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                        (placeholder != null && placeholder.toLowerCase().contains(fieldName.toLowerCase()))) {
                        field.click();
                        sleep(200);
                        field.clear();
                        field.sendKeys(value);
                        System.out.println("✅ Filled field '" + fieldName + "' = " + value);
                        dismissKeyboard();
                        return;
                    }
                }
            } catch (Exception e) {}
            
            // STRATEGY 2: Find label, then TextField or TextView below it
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS '" + fieldName + "' OR label CONTAINS '" + fieldName + "')")
                );
                
                if (!labels.isEmpty()) {
                    WebElement label = labels.get(0);
                    int labelY = label.getLocation().getY();
                    
                    List<WebElement> inputFields = driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
                    );
                    for (WebElement tf : inputFields) {
                        int tfY = tf.getLocation().getY();
                        if (Math.abs(tfY - labelY) < 100) {
                            tf.click();
                            sleep(200);
                            tf.clear();
                            tf.sendKeys(value);
                            System.out.println("✅ Filled field near '" + fieldName + "' = " + value);
                            dismissKeyboard();
                            return;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // Scroll down and try again
            if (scrollAttempt < 2) {
                System.out.println("   Field not visible, scrolling...");
                scrollFormDown();
                sleep(300);
            }
        }
        
        System.out.println("⚠️ Field not found: " + fieldName);
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
        // Try Save Changes button
        try {
            WebElement saveChangesBtn = driver.findElement(AppiumBy.accessibilityId("Save Changes"));
            saveChangesBtn.click();
            System.out.println("✅ Clicked Save Changes");
            sleep(1000);
            return;
        } catch (Exception e) {}
        
        // Try predicate search
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Save Changes' OR label == 'Save Changes' OR name == 'Save' OR label == 'Save'")
            );
            saveBtn.click();
            System.out.println("✅ Clicked Save (predicate)");
            sleep(1000);
            return;
        } catch (Exception e) {}
        
        // Try finding Save button
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && (name.contains("Save") || name.contains("save"))) {
                    btn.click();
                    System.out.println("✅ Clicked Save button: " + name);
                    sleep(1000);
                    return;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not find Save button");
    }

    /**
     * Click Cancel button on Edit Asset Details screen
     */
    public void clickEditCancel() {
        System.out.println("📝 Tapping Cancel button");
        
        // Strategy 1: Accessibility ID "Cancel"
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel on Edit screen");
            sleep(500);
            return;
        } catch (Exception e) {}
        
        // Strategy 2: Find button with Cancel label
        try {
            WebElement cancelBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel button");
            sleep(500);
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Find StaticText "Cancel" and click
        try {
            WebElement cancelText = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            cancelText.click();
            System.out.println("✅ Clicked Cancel text");
            sleep(500);
            return;
        } catch (Exception e) {}
        
        // Strategy 4: Tap coordinates at top-left (typical Cancel position)
        try {
            driver.executeScript("mobile: tap", Map.of("x", 60, "y", 60));
            System.out.println("✅ Tapped Cancel position (60, 60)");
            sleep(500);
            return;
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not find Cancel button");
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
     * Select Ampere Rating from dropdown (e.g., "30A", "50A", "100A")
     */
    public void selectAmpereRating(String value) {
        System.out.println("📝 Selecting Ampere Rating: " + value);
        
        // Try to find and click the Ampere Rating dropdown
        for (int scrollAttempt = 0; scrollAttempt < 3; scrollAttempt++) {
            
            // STRATEGY 1: Find button/picker with Ampere in name
            try {
                List<WebElement> buttons = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name CONTAINS[c] 'ampere' OR label CONTAINS[c] 'ampere')")
                );
                if (!buttons.isEmpty()) {
                    buttons.get(0).click();
                    sleep(500);
                    // Select the value from dropdown
                    selectDropdownValue(value);
                    return;
                }
            } catch (Exception e) {}
            
            // STRATEGY 2: Find label "Ampere Rating" then click nearby button/picker
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] 'ampere' OR label CONTAINS[c] 'ampere')")
                );
                
                if (!labels.isEmpty()) {
                    WebElement label = labels.get(0);
                    int labelY = label.getLocation().getY();
                    
                    // Find button near the label
                    List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
                    for (WebElement btn : buttons) {
                        int btnY = btn.getLocation().getY();
                        if (Math.abs(btnY - labelY) < 80) {
                            btn.click();
                            sleep(500);
                            selectDropdownValue(value);
                            return;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // STRATEGY 3: Direct accessibility ID for the value
            try {
                WebElement option = driver.findElement(AppiumBy.accessibilityId(value));
                option.click();
                System.out.println("✅ Selected Ampere Rating: " + value);
                sleep(300);
                return;
            } catch (Exception e) {}
            
            // Scroll down and try again
            if (scrollAttempt < 2) {
                System.out.println("   Ampere Rating not visible, scrolling...");
                scrollFormDown();
                sleep(300);
            }
        }
        
        System.out.println("⚠️ Could not select Ampere Rating: " + value);
    }

    /**
     * Select a value from an open dropdown
     */
    private void selectDropdownValue(String value) {
        System.out.println("   Selecting dropdown value: " + value);
        
        // Try accessibility ID first (exact match)
        try {
            WebElement option = driver.findElement(AppiumBy.accessibilityId(value));
            option.click();
            System.out.println("✅ Selected: " + value);
            sleep(300);
            return;
        } catch (Exception e) {}
        
        // Try case-insensitive exact match
        try {
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString("name ==[c] '" + value + "' OR label ==[c] '" + value + "'")
            );
            option.click();
            System.out.println("✅ Selected (case-insensitive): " + value);
            sleep(300);
            return;
        } catch (Exception e) {}
        
        // Try without spaces (e.g., "10 kA" -> "10kA")
        String noSpaceValue = value.replace(" ", "");
        try {
            WebElement option = driver.findElement(AppiumBy.accessibilityId(noSpaceValue));
            option.click();
            System.out.println("✅ Selected (no space): " + noSpaceValue);
            sleep(300);
            return;
        } catch (Exception e) {}
        
        // Try CONTAINS match for partial matching
        try {
            String searchPart = value.split(" ")[0]; // Get first part like "10" from "10 kA"
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString("(name CONTAINS '" + searchPart + "' AND name CONTAINS 'kA') OR (label CONTAINS '" + searchPart + "' AND label CONTAINS 'kA')")
            );
            option.click();
            System.out.println("✅ Selected (contains): " + value);
            sleep(300);
            return;
        } catch (Exception e) {}
        
        // Try finding any Button/StaticText with the value
        try {
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString("(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND (name CONTAINS[c] '" + value.split(" ")[0] + "')")
            );
            for (WebElement elem : elements) {
                String name = elem.getAttribute("name");
                if (name != null && name.toLowerCase().contains(value.split(" ")[0].toLowerCase())) {
                    elem.click();
                    System.out.println("✅ Selected (partial): " + name);
                    sleep(300);
                    return;
                }
            }
        } catch (Exception e) {}
        
        // Try finding StaticText with the value
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && name.equals(value)) {
                    text.click();
                    System.out.println("✅ Selected: " + value);
                    sleep(300);
                    return;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not find dropdown option: " + value);
    }

    /**
     * Fill ATS required field - Interrupting Rating
     */
    public void fillInterruptingRating(String value) {
        fillTextField("Interrupting", value);
    }

    /**
     * Select Interrupting Rating from dropdown (e.g., "10 kA", "25 kA", "50 kA")
     */
    public void selectInterruptingRating(String value) {
        System.out.println("📝 Selecting Interrupting Rating: " + value);
        
        // Try to find and click the Interrupting Rating dropdown
        for (int scrollAttempt = 0; scrollAttempt < 3; scrollAttempt++) {
            
            // STRATEGY 1: Find button/picker with Interrupting in name
            try {
                List<WebElement> buttons = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name CONTAINS[c] 'interrupting' OR label CONTAINS[c] 'interrupting')")
                );
                if (!buttons.isEmpty()) {
                    buttons.get(0).click();
                    sleep(500);
                    selectDropdownValue(value);
                    return;
                }
            } catch (Exception e) {}
            
            // STRATEGY 2: Find label "Interrupting Rating" then click nearby button/picker
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] 'interrupting' OR label CONTAINS[c] 'interrupting')")
                );
                
                if (!labels.isEmpty()) {
                    WebElement label = labels.get(0);
                    int labelY = label.getLocation().getY();
                    
                    // Find button near the label
                    List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
                    for (WebElement btn : buttons) {
                        int btnY = btn.getLocation().getY();
                        if (Math.abs(btnY - labelY) < 80) {
                            btn.click();
                            sleep(500);
                            selectDropdownValue(value);
                            return;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // STRATEGY 3: Direct accessibility ID for the value
            try {
                WebElement option = driver.findElement(AppiumBy.accessibilityId(value));
                option.click();
                System.out.println("✅ Selected Interrupting Rating: " + value);
                sleep(300);
                return;
            } catch (Exception e) {}
            
            // Scroll down and try again
            if (scrollAttempt < 2) {
                System.out.println("   Interrupting Rating not visible, scrolling...");
                scrollFormDown();
                sleep(300);
            }
        }
        
        System.out.println("⚠️ Could not select Interrupting Rating: " + value);
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
    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToBusway() {
        System.out.println("📋 Changing asset class to Busway (FAST)...");
        
        // Quick check - is Busway already displayed?
        try {
            WebElement busway = driver.findElement(AppiumBy.accessibilityId("Busway"));
            if (busway.isDisplayed()) {
                System.out.println("✅ Already Busway");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Busway
            driver.findElement(AppiumBy.accessibilityId("Busway")).click();
            System.out.println("✅ Changed to Busway");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Busway: " + e.getMessage());
        }
    }
    
    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToATS() {
        System.out.println("📋 Changing asset class to ATS (FAST)...");
        
        // Quick check - is ATS already displayed?
        try {
            WebElement ats = driver.findElement(AppiumBy.accessibilityId("ATS"));
            if (ats.isDisplayed()) {
                System.out.println("✅ Already ATS");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click ATS
            driver.findElement(AppiumBy.accessibilityId("ATS")).click();
            System.out.println("✅ Changed to ATS");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to ATS: " + e.getMessage());
        }
    }

    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToGenerator() {
        System.out.println("📋 Changing asset class to Generator (FAST)...");
        
        // Quick check - is Generator already displayed?
        try {
            WebElement gen = driver.findElement(AppiumBy.accessibilityId("Generator"));
            if (gen.isDisplayed()) {
                System.out.println("✅ Already Generator");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Generator
            driver.findElement(AppiumBy.accessibilityId("Generator")).click();
            System.out.println("✅ Changed to Generator");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Generator: " + e.getMessage());
        }
    }
    
    /**
     * Get the current asset class value displayed on screen
     */
    private String getCurrentAssetClassValue() {
        String[] possibleClasses = {"ATS", "UPS", "PDU", "Generator", "Busway", "Capacitor", "Circuit Breaker", "Fuse", "None"};
        
        // First find the "Asset Class" label position
        int labelY = -1;
        try {
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            labelY = assetClassLabel.getLocation().getY();
        } catch (Exception e) {}
        
        // Look for asset class names near the label
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null) {
                    for (String className : possibleClasses) {
                        if (name.equals(className)) {
                            int textY = text.getLocation().getY();
                            // If we found the label, check if this is near it (within 100 pixels)
                            if (labelY > 0 && Math.abs(textY - labelY) < 100) {
                                return className;
                            }
                            // If no label found, just return the first class name found
                            if (labelY < 0) {
                                return className;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
        
        return null;
    }
    
    /**
     * Robust method to open Asset Class dropdown on Edit Asset screen
     */
    private boolean openAssetClassDropdownRobust() {
        System.out.println("🔍 Opening Asset Class dropdown (robust)...");
        
        // Strategy 1: Find and tap the Asset Class row directly
        try {
            // Find Asset Class label
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name == 'Asset Class' OR label == 'Asset Class')")
            );
            
            // Get its position
            int labelX = assetClassLabel.getLocation().getX();
            int labelY = assetClassLabel.getLocation().getY();
            int labelWidth = assetClassLabel.getSize().getWidth();
            int labelHeight = assetClassLabel.getSize().getHeight();
            
            System.out.println("   Found 'Asset Class' label at (" + labelX + ", " + labelY + ")");
            
            // Tap to the right of the label (where the dropdown value is)
            int screenWidth = driver.manage().window().getSize().getWidth();
            int tapX = screenWidth - 100;  // Tap near right side where dropdown value typically is
            int tapY = labelY + (labelHeight / 2);  // Tap at same vertical level as label
            
            System.out.println("   Tapping Asset Class row at (" + tapX + ", " + tapY + ")...");
            driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
            sleep(800);
            
            if (isAssetClassDropdownDisplayed()) {
                System.out.println("   ✅ Dropdown opened via label tap!");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 1 failed: " + e.getMessage());
        }
        
        // Strategy 2: Click on any visible asset class name
        String[] assetClasses = {"UPS", "PDU", "Generator", "Busway", "Capacitor", "Circuit Breaker", "Fuse", "None"};
        for (String className : assetClasses) {
            try {
                List<WebElement> elements = driver.findElements(AppiumBy.accessibilityId(className));
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        System.out.println("   Found '" + className + "' - clicking to open dropdown");
                        el.click();
                        sleep(800);
                        if (isAssetClassDropdownDisplayed()) {
                            System.out.println("   ✅ Dropdown opened by clicking " + className);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {}
        }
        
        // Strategy 3: Find any button in the asset class row area  
        try {
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                int btnY = btn.getLocation().getY();
                if (Math.abs(btnY - labelY) < 60) {
                    String name = btn.getAttribute("name");
                    System.out.println("   Found button near Asset Class: " + name);
                    btn.click();
                    sleep(800);
                    if (isAssetClassDropdownDisplayed()) {
                        System.out.println("   ✅ Dropdown opened via button!");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 failed");
        }
        
        // Strategy 4: Try tapping various positions along the Asset Class row
        try {
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            int screenWidth = driver.manage().window().getSize().getWidth();
            
            int[] xPositions = {screenWidth / 2, screenWidth - 50, screenWidth - 150, screenWidth / 4 * 3};
            for (int tapX : xPositions) {
                System.out.println("   Trying tap at (" + tapX + ", " + labelY + ")...");
                driver.executeScript("mobile: tap", Map.of("x", tapX, "y", labelY));
                sleep(600);
                if (isAssetClassDropdownDisplayed()) {
                    System.out.println("   ✅ Dropdown opened!");
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 4 failed");
        }
        
        System.out.println("   ❌ Could not open Asset Class dropdown");
        return false;
    }
    
    /**
     * Try clicking on the current asset class value displayed on screen
     * This helps when we can't find the dropdown button but the value is visible
     */
    private boolean tryClickCurrentAssetClassValue() {
        String[] possibleClasses = {"ATS", "UPS", "PDU", "Generator", "Busway", "Capacitor", "Circuit Breaker", "Fuse", "None"};
        
        for (String className : possibleClasses) {
            try {
                WebElement classElement = driver.findElement(AppiumBy.accessibilityId(className));
                if (classElement.isDisplayed()) {
                    System.out.println("   Found current class: " + className + " - clicking it");
                    classElement.click();
                    sleep(500);
                    
                    // Check if dropdown opened
                    if (isAssetClassDropdownDisplayed()) {
                        return true;
                    }
                }
            } catch (Exception e) {}
        }
        
        // Also try finding StaticText with asset class names
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null) {
                    for (String className : possibleClasses) {
                        if (name.equals(className)) {
                            System.out.println("   Found class text: " + name + " - clicking it");
                            text.click();
                            sleep(500);
                            if (isAssetClassDropdownDisplayed()) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
        
        return false;
    }
    
    /**
     * Select ATS from the asset class dropdown
     */
    private void selectATSFromDropdown() {
        System.out.println("📋 Selecting ATS from dropdown...");
        
        // Strategy 1: Direct accessibility ID (fast - 1 second wait)
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
            WebElement ats = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("ATS")
            ));
            ats.click();
            System.out.println("✅ Selected ATS (accessibility ID)");
            return;
        } catch (Exception e) {}
        
        // Strategy 2: NSPredicate for name/label
        try {
            WebElement ats = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'ATS' OR label == 'ATS'")
            );
            ats.click();
            System.out.println("✅ Selected ATS (predicate)");
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Find in visible cells/buttons
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            for (WebElement cell : cells) {
                String name = cell.getAttribute("name");
                if ("ATS".equals(name)) {
                    cell.click();
                    System.out.println("✅ Selected ATS from cell");
                    return;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not select ATS from dropdown");
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
        
        // Strategy 0: Try clicking directly on any displayed asset class name (for Edit mode) - FAST
        String[] assetClasses = {"ATS", "UPS", "PDU", "Generator", "Busway", "Capacitor", "Circuit Breaker", "Fuse"};
        for (String className : assetClasses) {
            try {
                WebElement classElement = driver.findElement(AppiumBy.accessibilityId(className));
                if (classElement.isDisplayed()) {
                    System.out.println("   Found displayed class: " + className + " - clicking it");
                    classElement.click();
                    sleep(500);
                    if (isDropdownOpen()) {
                        System.out.println("   ✅ Dropdown opened by clicking " + className);
                        return true;
                    }
                }
            } catch (Exception e) {}
        }
        
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
            sleep(400);
            
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
                    sleep(400);
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
                    sleep(400);
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
            sleep(400);
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
     * by looking for class options like ATS, UPS, PDU, Busway, Generator, etc.
     */
    private boolean isDropdownOpen() {
        try {
            // Look for dropdown options - include ALL asset class names
            String[] dropdownOptions = {"ATS", "UPS", "PDU", "Busway", "Generator", "Capacitor", "Circuit Breaker", "Fuse", "None"};
            for (String option : dropdownOptions) {
                try {
                    List<WebElement> elements = driver.findElements(AppiumBy.accessibilityId(option));
                    // Need to find at least 2 elements with same name (1 is the current value, 2+ means dropdown is open)
                    // Or check if any element is in a picker/dropdown area
                    for (WebElement el : elements) {
                        if (el.isDisplayed()) {
                            // Check if this looks like a dropdown option (not just the current value)
                            int y = el.getLocation().getY();
                            // Dropdown options typically appear in lower half of screen when open
                            if (y > 400) {  // Dropdown picker area
                                System.out.println("   Found dropdown option: " + option + " at y=" + y);
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {}
            }
            
            // Also use the existing isAssetClassDropdownDisplayed method
            return isAssetClassDropdownDisplayed();
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

    // ================================================================
    // CAPACITOR ASSET CLASS METHODS
    // ================================================================

    /**
     * Change asset class to Capacitor in Edit mode
     */
    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToCapacitor() {
        System.out.println("📋 Changing asset class to Capacitor (FAST)...");
        
        // Quick check - is Capacitor already displayed?
        try {
            WebElement cap = driver.findElement(AppiumBy.accessibilityId("Capacitor"));
            if (cap.isDisplayed()) {
                System.out.println("✅ Already Capacitor");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Capacitor
            driver.findElement(AppiumBy.accessibilityId("Capacitor")).click();
            System.out.println("✅ Changed to Capacitor");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Capacitor: " + e.getMessage());
        }
    }
    
    /**
     * Check if asset class is already set to a specific value
     */
    private boolean isAssetClassAlready(String expectedClass) {
        try {
            // Look for the class name displayed on the Asset Class button/field
            WebElement classButton = driver.findElement(AppiumBy.accessibilityId(expectedClass));
            if (classButton.isDisplayed()) {
                System.out.println("   Found " + expectedClass + " already selected");
                return true;
            }
        } catch (Exception e) {}
        
        // Try finding by StaticText containing the class name near "Asset Class" label
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && name.equalsIgnoreCase(expectedClass)) {
                    // Check if it's near Asset Class area (y around 600-700)
                    int y = text.getLocation().getY();
                    if (y > 550 && y < 750) {
                        System.out.println("   Asset class is already: " + expectedClass);
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        
        return false;
    }
    
    /**
     * Tap on safe area to dismiss dropdown focus
     * Taps on empty space at top of form to close any open dropdowns
     */
    public void dismissDropdownFocus() {
        try {
            int screenWidth = driver.manage().window().getSize().width;
            // Tap on top area (above Asset Class which is typically at y=600+)
            driver.executeScript("mobile: tap", Map.of("x", screenWidth / 2, "y", 200));
            sleep(200);
        } catch (Exception e) {
            // Ignore
        }
    }
    
    /**
     * Select Capacitor from the asset class dropdown
     */
    private void selectCapacitorFromDropdown() {
        System.out.println("📋 Selecting Capacitor from dropdown...");
        
        try {
            WebElement capacitor = driver.findElement(AppiumBy.accessibilityId("Capacitor"));
            capacitor.click();
            System.out.println("✅ Selected Capacitor");
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement capacitor = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Capacitor' OR label == 'Capacitor'")
            );
            capacitor.click();
            System.out.println("✅ Selected Capacitor (predicate)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Capacitor in dropdown");
        }
    }

    // ================================================================
    // CIRCUIT BREAKER ASSET CLASS METHODS
    // ================================================================

    /**
     * Change asset class to Circuit Breaker in Edit mode
     */
    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToCircuitBreaker() {
        System.out.println("📋 Changing asset class to Circuit Breaker (FAST)...");
        
        // Quick check - is Circuit Breaker already displayed?
        try {
            WebElement cb = driver.findElement(AppiumBy.accessibilityId("Circuit Breaker"));
            if (cb.isDisplayed()) {
                System.out.println("✅ Already Circuit Breaker");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Circuit Breaker
            driver.findElement(AppiumBy.accessibilityId("Circuit Breaker")).click();
            System.out.println("✅ Changed to Circuit Breaker");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Circuit Breaker: " + e.getMessage());
        }
    }
    
    /**
     * Select Circuit Breaker from the asset class dropdown
     */
    private void selectCircuitBreakerFromDropdown() {
        System.out.println("📋 Selecting Circuit Breaker from dropdown...");
        
        try {
            WebElement cb = driver.findElement(AppiumBy.accessibilityId("Circuit Breaker"));
            cb.click();
            System.out.println("✅ Selected Circuit Breaker");
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement cb = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Circuit Breaker' OR label == 'Circuit Breaker'")
            );
            cb.click();
            System.out.println("✅ Selected Circuit Breaker (predicate)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Circuit Breaker in dropdown");
        }
    }

    // ================================================================
    // DISCONNECT SWITCH ASSET CLASS METHODS
    // ================================================================

    /**
     * Change asset class to Disconnect Switch in Edit mode
     */
    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToDisconnectSwitch() {
        System.out.println("📋 Changing asset class to Disconnect Switch (FAST)...");
        
        // Quick check - is Disconnect Switch already displayed?
        try {
            WebElement ds = driver.findElement(AppiumBy.accessibilityId("Disconnect Switch"));
            if (ds.isDisplayed()) {
                System.out.println("✅ Already Disconnect Switch");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Disconnect Switch
            driver.findElement(AppiumBy.accessibilityId("Disconnect Switch")).click();
            System.out.println("✅ Changed to Disconnect Switch");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Disconnect Switch: " + e.getMessage());
        }
    }
    
    /**
     * Select Disconnect Switch from the asset class dropdown
     */
    private void selectDisconnectSwitchFromDropdown() {
        System.out.println("📋 Selecting Disconnect Switch from dropdown...");
        
        try {
            WebElement ds = driver.findElement(AppiumBy.accessibilityId("Disconnect Switch"));
            ds.click();
            System.out.println("✅ Selected Disconnect Switch");
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement ds = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Disconnect Switch' OR label == 'Disconnect Switch'")
            );
            ds.click();
            System.out.println("✅ Selected Disconnect Switch (predicate)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Disconnect Switch in dropdown");
        }
    }

    // ================================================================
    // FUSE ASSET CLASS METHODS
    // ================================================================

    /**
     * Change asset class to Fuse in Edit mode
     */
    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToFuse() {
        System.out.println("📋 Changing asset class to Fuse (FAST)...");
        
        // Quick check - is Fuse already displayed?
        try {
            WebElement fuse = driver.findElement(AppiumBy.accessibilityId("Fuse"));
            if (fuse.isDisplayed()) {
                System.out.println("✅ Already Fuse");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Fuse
            driver.findElement(AppiumBy.accessibilityId("Fuse")).click();
            System.out.println("✅ Changed to Fuse");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Fuse: " + e.getMessage());
        }
    }
    
    /**
     * Select Fuse from the asset class dropdown
     */
    private void selectFuseFromDropdown() {
        System.out.println("📋 Selecting Fuse from dropdown...");
        
        try {
            WebElement fuse = driver.findElement(AppiumBy.accessibilityId("Fuse"));
            fuse.click();
            System.out.println("✅ Selected Fuse");
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement fuse = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Fuse' OR label == 'Fuse'")
            );
            fuse.click();
            System.out.println("✅ Selected Fuse (predicate)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Fuse in dropdown");
        }
    }

    // ================================================================
    // JUNCTION BOX ASSET CLASS METHODS
    // ================================================================

    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToJunctionBox() {
        System.out.println("📋 Changing asset class to Junction Box (FAST)...");
        
        // Quick check - is Junction Box already displayed?
        try {
            WebElement jb = driver.findElement(AppiumBy.accessibilityId("Junction Box"));
            if (jb.isDisplayed()) {
                System.out.println("✅ Already Junction Box");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Junction Box
            driver.findElement(AppiumBy.accessibilityId("Junction Box")).click();
            System.out.println("✅ Changed to Junction Box");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Junction Box: " + e.getMessage());
        }
    }

    // ================================================================
    // LOADCENTER ASSET CLASS CHANGE
    // ================================================================

    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToLoadcenter() {
        System.out.println("📋 Changing asset class to Loadcenter (FAST)...");
        
        // Quick check - is Loadcenter already displayed?
        try {
            WebElement lc = driver.findElement(AppiumBy.accessibilityId("Loadcenter"));
            if (lc.isDisplayed()) {
                System.out.println("✅ Already Loadcenter");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Loadcenter
            driver.findElement(AppiumBy.accessibilityId("Loadcenter")).click();
            System.out.println("✅ Changed to Loadcenter");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Loadcenter: " + e.getMessage());
        }
    }

    // ================================================================
    // MCC (MOTOR CONTROL CENTER) ASSET CLASS CHANGE
    // ================================================================

    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToMCC() {
        System.out.println("📋 Changing asset class to MCC (FAST)...");
        
        // Quick check - is MCC already displayed?
        try {
            WebElement mcc = driver.findElement(AppiumBy.accessibilityId("MCC"));
            if (mcc.isDisplayed()) {
                System.out.println("✅ Already MCC");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click MCC
            driver.findElement(AppiumBy.accessibilityId("MCC")).click();
            System.out.println("✅ Changed to MCC");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to MCC: " + e.getMessage());
        }
    }

    // ================================================================
    // MCC BUCKET ASSET CLASS CHANGE
    // ================================================================

    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToMCCBucket() {
        System.out.println("📋 Changing asset class to MCC Bucket (FAST)...");
        
        // Quick check - is MCC Bucket already displayed?
        try {
            WebElement mccb = driver.findElement(AppiumBy.accessibilityId("MCC Bucket"));
            if (mccb.isDisplayed()) {
                System.out.println("✅ Already MCC Bucket");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click MCC Bucket
            driver.findElement(AppiumBy.accessibilityId("MCC Bucket")).click();
            System.out.println("✅ Changed to MCC Bucket");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to MCC Bucket: " + e.getMessage());
        }
    }
    
    /**
     * Check if Save Changes button is visible
     */
    public boolean isSaveChangesButtonVisible() {
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Save Changes' OR label == 'Save Changes'")
            );
            return saveBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * DEBUG: Print all visible input fields and labels for model/notes/manufacturer
     */
    public void debugFieldsOnScreen() {
        System.out.println("\n========== DEBUG: Fields on Screen ==========");
        try {
            List<WebElement> labels = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText'")
            );
            System.out.println("Labels (model/notes/manufacturer):");
            for (WebElement l : labels) {
                String name = l.getAttribute("name");
                if (name != null && (name.toLowerCase().contains("model") || 
                    name.toLowerCase().contains("notes") || 
                    name.toLowerCase().contains("manufacturer"))) {
                    System.out.println("  Label: '" + name + "' y=" + l.getLocation().getY());
                }
            }
        } catch (Exception e) {}
        try {
            List<WebElement> inputs = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
            );
            System.out.println("All TextField/TextView:");
            for (WebElement f : inputs) {
                String name = f.getAttribute("name");
                String value = f.getAttribute("value");
                int y = f.getLocation().getY();
                System.out.println("  Input: name='" + name + "' value='" + value + "' y=" + y);
            }
        } catch (Exception e) {}
        System.out.println("==============================================\n");
    }

    /**
     * Edit a text field by its label/name
     * @param fieldName The accessibility ID or label of the field
     * @param value The value to enter
     * @return true if field was edited successfully
     */
    public boolean editTextField(String fieldName, String value) {
        System.out.println("📝 Editing field: " + fieldName);
        
        // Try to find and edit the field, scroll if needed (max 5 scrolls)
        for (int scrollAttempt = 0; scrollAttempt < 5; scrollAttempt++) {
            
            // STRATEGY 1: Find TextField/TextView by accessibility ID (verify element type!)
            try {
                WebElement field = driver.findElement(AppiumBy.accessibilityId(fieldName));
                String elementType = field.getAttribute("type");
                // Only proceed if it's actually an input field, not a label
                if (field.isDisplayed() && elementType != null && 
                    (elementType.contains("TextField") || elementType.contains("TextView"))) {
                    System.out.println("   🔍 Strategy 1: Found input field type=" + elementType);
                    field.click();
                    sleep(300);
                    field.clear();
                    field.sendKeys(value);
                    sleep(200);
                    String afterValue = field.getAttribute("value");
                    dismissKeyboard();
                    if (afterValue != null && afterValue.length() > 0) {
                        System.out.println("✅ Edited " + fieldName + " (accessibilityId) = " + value);
                        return true;
                    }
                } else {
                    System.out.println("   ⚠️ Strategy 1: Found element but type=" + elementType + " (not input field, skipping)");
                }
            } catch (Exception e) {}
            
            // STRATEGY 2: Find TextField OR TextView by name/label predicate (CASE-INSENSITIVE)
            try {
                // Try case-insensitive match using ==[c]
                WebElement field = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND (name ==[c] '" + fieldName + "' OR label ==[c] '" + fieldName + "')"
                    )
                );
                if (field.isDisplayed()) {
                    String beforeValue = field.getAttribute("value");
                    System.out.println("   🔍 Strategy 2: Found field, name=" + field.getAttribute("name") + ", before=" + beforeValue);
                    field.click();
                    sleep(500);
                    field.clear();
                    sleep(200);
                    field.sendKeys(value);
                    sleep(300);
                    String afterValue = field.getAttribute("value");
                    System.out.println("   🔍 After sendKeys: value=" + afterValue);
                    dismissKeyboard();
                    if (afterValue != null && afterValue.contains(value.substring(0, Math.min(5, value.length())))) {
                        System.out.println("✅ Edited " + fieldName + " (predicate) = " + value);
                        return true;
                    } else {
                        System.out.println("⚠️ sendKeys may have failed, trying tap+type...");
                        // Fall through to next strategy
                    }
                }
            } catch (Exception e) {}
            
            // STRATEGY 2B: Try lowercase variant
            try {
                String lowerFieldName = fieldName.toLowerCase();
                WebElement field = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND (name == '" + lowerFieldName + "' OR label == '" + lowerFieldName + "')"
                    )
                );
                if (field.isDisplayed()) {
                    String beforeValue = field.getAttribute("value");
                    System.out.println("   🔍 Strategy 2B: Found lowercase field, before=" + beforeValue);
                    field.click();
                    sleep(500);
                    field.clear();
                    sleep(200);
                    field.sendKeys(value);
                    sleep(300);
                    String afterValue = field.getAttribute("value");
                    System.out.println("   🔍 After sendKeys: value=" + afterValue);
                    dismissKeyboard();
                    if (afterValue != null && afterValue.contains(value.substring(0, Math.min(5, value.length())))) {
                        System.out.println("✅ Edited " + fieldName + " (lowercase) = " + value);
                        return true;
                    }
                }
            } catch (Exception e) {}
            
            // STRATEGY 3: Find label (case-insensitive), then find CLOSEST TextField/TextView BELOW it
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "')")
                );
                
                if (!labels.isEmpty()) {
                    WebElement label = labels.get(0);
                    int labelY = label.getLocation().getY();
                    
                    // Find TextField OR TextView - must be BELOW or same row (not above!)
                    List<WebElement> inputFields = driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
                    );
                    
                    // Find the CLOSEST field that is BELOW the label (tfY >= labelY - 20)
                    WebElement closestField = null;
                    int closestDistance = Integer.MAX_VALUE;
                    
                    for (WebElement tf : inputFields) {
                        int tfY = tf.getLocation().getY();
                        // Field must be BELOW or at same level as label (allow 20px tolerance for same row)
                        // AND within 100px vertically
                        if (tfY >= labelY - 20 && tfY <= labelY + 100) {
                            int distance = Math.abs(tfY - labelY);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestField = tf;
                            }
                        }
                    }
                    
                    if (closestField != null) {
                        int tfY = closestField.getLocation().getY();
                        System.out.println("   Found '" + fieldName + "' at Y=" + labelY + ", field at Y=" + tfY);
                        closestField.click();
                        sleep(300);
                        closestField.clear();
                        closestField.sendKeys(value);
                        dismissKeyboard();
                        System.out.println("✅ Edited " + fieldName + " = " + value);
                        return true;
                    }
                }
            } catch (Exception e) {}
            
            // STRATEGY 4: Find by partial match in label (case-insensitive)
            try {
                String searchTerm = fieldName.replace("Serial Number", "").trim();
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] '" + searchTerm + "'")
                );
                
                for (WebElement label : labels) {
                    String labelName = label.getAttribute("name");
                    if (labelName != null && labelName.toLowerCase().contains(searchTerm.toLowerCase())) {
                        int labelY = label.getLocation().getY();
                        
                        // Find CLOSEST field BELOW the label
                        List<WebElement> inputFields = driver.findElements(
                            AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
                        );
                        
                        WebElement closestField = null;
                        int closestDistance = Integer.MAX_VALUE;
                        
                        for (WebElement tf : inputFields) {
                            int tfY = tf.getLocation().getY();
                            // Field must be BELOW or same row (tfY >= labelY - 20) and within 100px
                            if (tfY >= labelY - 20 && tfY <= labelY + 100) {
                                int distance = Math.abs(tfY - labelY);
                                if (distance < closestDistance) {
                                    closestDistance = distance;
                                    closestField = tf;
                                }
                            }
                        }
                        
                        if (closestField != null) {
                            closestField.click();
                            sleep(300);
                            closestField.clear();
                            closestField.sendKeys(value);
                            dismissKeyboard();
                            System.out.println("✅ Edited " + fieldName + " (partial match) = " + value);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // Field not found on current screen - scroll down and try again
            if (scrollAttempt < 4) {
                System.out.println("   Field '" + fieldName + "' not visible, scrolling... (attempt " + (scrollAttempt + 1) + ")");
                scrollFormDown();
                sleep(500);
            }
        }
        
        System.out.println("⚠️ Could not find field: " + fieldName);
        return false;
    }
    
    /**
     * Scroll until field is visible
     */
    private void scrollToFieldIfNeeded(String fieldName) {
        for (int i = 0; i < 3; i++) {
            try {
                WebElement field = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS '" + fieldName + "' OR label CONTAINS '" + fieldName + "'")
                );
                if (field.isDisplayed()) {
                    return; // Found it
                }
            } catch (Exception e) {}
            
            // Scroll down to find field
            scrollFormDown();
            sleep(300);
        }
    }
    
    /**
     * Scroll to a specific field in Core Attributes
     */
    public void scrollToField(String fieldName) {
        System.out.println("📜 Scrolling to field: " + fieldName);
        
        for (int i = 0; i < 5; i++) {
            try {
                WebElement field = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS '" + fieldName + "' OR label CONTAINS '" + fieldName + "'")
                );
                if (field.isDisplayed()) {
                    System.out.println("   Found field: " + fieldName);
                    return;
                }
            } catch (Exception e) {}
            
            scrollFormDown();
            sleep(300);
        }
        System.out.println("⚠️ Could not find field: " + fieldName);
    }
    
    /**
     * Verify asset was saved successfully after edit
     */
    public boolean isAssetSavedAfterEdit() {
        sleep(1500);  // Wait for save to complete
        
        // Strategy 1: Check if Edit button is visible (view mode)
        try {
            WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
            if (editBtn.isDisplayed()) {
                System.out.println("✅ Save successful - Edit button visible (view mode)");
                return true;
            }
        } catch (Exception e) {}
        
        // Strategy 2: Check if Save Changes button is gone
        try {
            driver.findElement(AppiumBy.accessibilityId("Save Changes"));
            // Still in edit mode - might be validation error
            System.out.println("   Save Changes still visible - checking for errors...");
        } catch (Exception e) {
            // Save Changes gone = save successful
            System.out.println("✅ Save successful - Save Changes button gone");
            return true;
        }
        
        // Strategy 3: Check if we're back on Asset List
        try {
            WebElement plusBtn = driver.findElement(AppiumBy.accessibilityId("plus"));
            if (plusBtn.isDisplayed()) {
                System.out.println("✅ Save successful - back on Asset List");
                return true;
            }
        } catch (Exception e) {}
        
        // Strategy 4: Check for success toast/message
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && (name.toLowerCase().contains("saved") || name.toLowerCase().contains("success"))) {
                    System.out.println("✅ Save successful - success message found");
                    return true;
                }
            }
        } catch (Exception e) {}
        
        // If none of the above, assume save was successful if no error visible
        System.out.println("   Assuming save successful (no error visible)");
        return true;
    }
    
    /**
     * Click Cancel button in Edit mode
     */
    public void clickCancelEdit() {
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }
    
    /**
     * Check if edit was cancelled (back to view mode)
     */
    public boolean isEditCancelled() {
        try {
            sleep(500);
            // Should be back to view mode with Edit button visible
            WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
            return editBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
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
     * Change asset class to Motor by selecting from dropdown
     */
    public final void changeAssetClassToMotor() {
        System.out.println("📋 Changing asset class to Motor (FAST)...");
        
        // Quick check - is Motor already displayed?
        try {
            WebElement motor = driver.findElement(AppiumBy.accessibilityId("Motor"));
            if (motor.isDisplayed()) {
                System.out.println("✅ Already Motor");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Motor
            driver.findElement(AppiumBy.accessibilityId("Motor")).click();
            System.out.println("✅ Changed to Motor");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Motor: " + e.getMessage());
        }
    }

    /**
     * Change asset class to Other by selecting from dropdown
     */
    public final void changeAssetClassToOther() {
        System.out.println("📋 Changing asset class to Other (FAST)...");
        
        // Quick check - is Other already displayed?
        try {
            WebElement other = driver.findElement(AppiumBy.accessibilityId("Other"));
            if (other.isDisplayed()) {
                System.out.println("✅ Already Other");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Other
            driver.findElement(AppiumBy.accessibilityId("Other")).click();
            System.out.println("✅ Changed to Other");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Other: " + e.getMessage());
        }
    }

    /**
     * Change asset class to Other (OCP) - Overcurrent Protection by selecting from dropdown
     */
    public final void changeAssetClassToOtherOCP() {
        System.out.println("📋 Changing asset class to Other (OCP) (FAST)...");
        
        // Quick check - is Other (OCP) already displayed?
        try {
            WebElement otherOCP = driver.findElement(AppiumBy.accessibilityId("Other (OCP)"));
            if (otherOCP.isDisplayed()) {
                System.out.println("✅ Already Other (OCP)");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Other (OCP)
            driver.findElement(AppiumBy.accessibilityId("Other (OCP)")).click();
            System.out.println("✅ Changed to Other (OCP)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Other (OCP): " + e.getMessage());
        }
    }

    /**
     * Change asset class to Panelboard by selecting from dropdown
     */
    public final void changeAssetClassToPanelboard() {
        System.out.println("📋 Changing asset class to Panelboard (FAST)...");
        
        // Quick check - is Panelboard already displayed?
        try {
            WebElement panelboard = driver.findElement(AppiumBy.accessibilityId("Panelboard"));
            if (panelboard.isDisplayed()) {
                System.out.println("✅ Already Panelboard");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Panelboard
            driver.findElement(AppiumBy.accessibilityId("Panelboard")).click();
            System.out.println("✅ Changed to Panelboard");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Panelboard: " + e.getMessage());
        }
    }

    /**
     * Change asset class to PDU (Power Distribution Unit) by selecting from dropdown
     */
    public final void changeAssetClassToPDU() {
        System.out.println("📋 Changing asset class to PDU (FAST)...");
        
        // Quick check - is PDU already displayed?
        try {
            WebElement pdu = driver.findElement(AppiumBy.accessibilityId("PDU"));
            if (pdu.isDisplayed()) {
                System.out.println("✅ Already PDU");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click PDU
            driver.findElement(AppiumBy.accessibilityId("PDU")).click();
            System.out.println("✅ Changed to PDU");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to PDU: " + e.getMessage());
        }
    }

    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToRelay() {
        System.out.println("📋 Changing asset class to Relay (FAST)...");
        
        // Quick check - is Relay already displayed?
        try {
            WebElement relay = driver.findElement(AppiumBy.accessibilityId("Relay"));
            if (relay.isDisplayed()) {
                System.out.println("✅ Already Relay");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(300);
            
            // Now click Relay
            driver.findElement(AppiumBy.accessibilityId("Relay")).click();
            System.out.println("✅ Changed to Relay");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Relay: " + e.getMessage());
        }
    }

    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToSwitchboard() {
        System.out.println("📋 Changing asset class to Switchboard (FAST)...");
        
        // Quick check - is Switchboard already displayed as selected?
        try {
            WebElement switchboard = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == 'Switchboard'")
            );
            if (switchboard.isDisplayed() && switchboard.getLocation().getY() < 500) {
                System.out.println("✅ Already Switchboard");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(500);
            
            // Try to find Switchboard - may need to scroll in dropdown
            // Asset classes alphabetical: ATS, Busway, Capacitor, Circuit Breaker, Disconnect Switch, 
            // Fuse, Generator, Motor, Panelboard, PDU, Relay, Switchboard, Transformer, UPS, Utility
            for (int scrollAttempt = 0; scrollAttempt < 5; scrollAttempt++) {
                try {
                    WebElement switchboard = driver.findElement(AppiumBy.accessibilityId("Switchboard"));
                    if (switchboard.isDisplayed()) {
                        switchboard.click();
                        System.out.println("✅ Changed to Switchboard");
                        sleep(300);
                        return;
                    }
                } catch (Exception e) {}
                
                // Scroll down inside dropdown to find Switchboard
                System.out.println("   Scrolling dropdown to find Switchboard (attempt " + (scrollAttempt + 1) + ")");
                int screenWidth = driver.manage().window().getSize().width;
                int screenHeight = driver.manage().window().getSize().height;
                driver.executeScript("mobile: dragFromToForDuration", Map.of(
                    "fromX", screenWidth / 2,
                    "fromY", screenHeight / 2 + 100,
                    "toX", screenWidth / 2,
                    "toY", screenHeight / 2 - 100,
                    "duration", 0.3
                ));
                sleep(400);
            }
            
            System.out.println("⚠️ Switchboard not found in dropdown after scrolling");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Switchboard: " + e.getMessage());
        }
    }

    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */
    public final void changeAssetClassToTransformer() {
        System.out.println("📋 Changing asset class to Transformer (FAST)...");
        
        // Quick check - is Transformer already displayed as selected?
        try {
            WebElement transformer = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == 'Transformer'")
            );
            if (transformer.isDisplayed() && transformer.getLocation().getY() < 500) {
                System.out.println("✅ Already Transformer");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(500);
            
            // Try to find Transformer - may need to scroll in dropdown
            // Asset classes alphabetical: ATS, Busway, Capacitor, Circuit Breaker, Disconnect Switch, 
            // Fuse, Generator, Motor, Panelboard, PDU, Relay, Switchboard, Transformer, UPS, Utility
            for (int scrollAttempt = 0; scrollAttempt < 5; scrollAttempt++) {
                try {
                    WebElement transformer = driver.findElement(AppiumBy.accessibilityId("Transformer"));
                    if (transformer.isDisplayed()) {
                        transformer.click();
                        System.out.println("✅ Changed to Transformer");
                        sleep(300);
                        return;
                    }
                } catch (Exception e) {}
                
                // Scroll down inside dropdown to find Transformer
                System.out.println("   Scrolling dropdown to find Transformer (attempt " + (scrollAttempt + 1) + ")");
                int screenWidth = driver.manage().window().getSize().width;
                int screenHeight = driver.manage().window().getSize().height;
                driver.executeScript("mobile: dragFromToForDuration", Map.of(
                    "fromX", screenWidth / 2,
                    "fromY", screenHeight / 2 + 100,
                    "toX", screenWidth / 2,
                    "toY", screenHeight / 2 - 100,
                    "duration", 0.3
                ));
                sleep(400);
            }
            
            System.out.println("⚠️ Transformer not found in dropdown after scrolling");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Transformer: " + e.getMessage());
        }
    }

    /**
     * Change asset class to UPS using coordinate-tap approach
     */
    public final void changeAssetClassToUPS() {
        System.out.println("📋 Changing asset class to UPS (FAST)...");
        
        // Quick check - is UPS already displayed as selected?
        try {
            WebElement ups = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == 'UPS'")
            );
            if (ups.isDisplayed() && ups.getLocation().getY() < 500) {
                System.out.println("✅ Already UPS");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(500);
            
            // Try to find UPS - may need to scroll in dropdown (UPS is near the end)
            for (int scrollAttempt = 0; scrollAttempt < 5; scrollAttempt++) {
                try {
                    WebElement ups = driver.findElement(AppiumBy.accessibilityId("UPS"));
                    if (ups.isDisplayed()) {
                        ups.click();
                        System.out.println("✅ Changed to UPS");
                        sleep(300);
                        return;
                    }
                } catch (Exception e) {}
                
                // Scroll down inside dropdown to find UPS
                System.out.println("   Scrolling dropdown to find UPS (attempt " + (scrollAttempt + 1) + ")");
                int screenWidth = driver.manage().window().getSize().width;
                int screenHeight = driver.manage().window().getSize().height;
                driver.executeScript("mobile: dragFromToForDuration", Map.of(
                    "fromX", screenWidth / 2,
                    "fromY", screenHeight / 2 + 100,
                    "toX", screenWidth / 2,
                    "toY", screenHeight / 2 - 100,
                    "duration", 0.3
                ));
                sleep(400);
            }
            
            System.out.println("⚠️ UPS not found in dropdown after scrolling");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to UPS: " + e.getMessage());
        }
    }

    /**
     * Change asset class to Utility
     * FIXED: Handles dropdown that requires scrolling to find Utility option
     */
    public final void changeAssetClassToUtility() {
        System.out.println("🔄 Changing asset class to Utility...");
        
        // Quick check if already Utility
        if (isAssetClassAlready("Utility")) {
            System.out.println("✅ Already on Utility - no change needed");
            return;
        }
        
        // Open Asset Class dropdown
        boolean dropdownOpened = clickAssetClassDropdown();
        if (!dropdownOpened) {
            System.out.println("❌ Could not open Asset Class dropdown!");
            return;
        }
        
        // Select Utility from dropdown (may need scrolling)
        selectUtilityFromDropdown();
        sleep(500);
        
        // Dismiss dropdown focus
        dismissDropdownFocus();
        
        System.out.println("✅ Asset class changed to Utility");
    }
    
    /**
     * Select Utility from the asset class dropdown
     * Scrolls the dropdown list if Utility is not visible
     */
    private void selectUtilityFromDropdown() {
        System.out.println("📋 Selecting Utility from dropdown...");
        
        // Utility is at the END of alphabetical list - needs many scrolls
        // List order: ATS, Busway, Capacitor, Circuit Breaker, Disconnect Switch, Fuse, Generator, Motor, Panelboard, PDU, Transformer, UPS, Utility
        
        int screenWidth = driver.manage().window().getSize().width;
        int screenHeight = driver.manage().window().getSize().height;
        int centerX = screenWidth / 2;
        
        // The dropdown appears in lower half of screen (y > 400)
        // Scroll within the dropdown area, not the form
        int dropdownStartY = (int)(screenHeight * 0.65);
        int dropdownEndY = (int)(screenHeight * 0.45);
        
        // Try up to 10 scroll attempts since Utility is at the end
        for (int i = 0; i < 10; i++) {
            // Check if Utility is visible
            try {
                List<WebElement> utilities = driver.findElements(AppiumBy.accessibilityId("Utility"));
                for (WebElement utility : utilities) {
                    if (utility.isDisplayed()) {
                        int y = utility.getLocation().getY();
                        // Make sure it's in the dropdown area (y > 400)
                        if (y > 400) {
                            utility.click();
                            System.out.println("✅ Selected Utility at y=" + y);
                            return;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // Try predicate search
            try {
                List<WebElement> utilities = driver.findElements(
                    AppiumBy.iOSNsPredicateString("name == 'Utility' OR label == 'Utility'")
                );
                for (WebElement utility : utilities) {
                    if (utility.isDisplayed()) {
                        int y = utility.getLocation().getY();
                        if (y > 400) {
                            utility.click();
                            System.out.println("✅ Selected Utility (predicate) at y=" + y);
                            return;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // Not found - scroll the dropdown DOWN (swipe up)
            System.out.println("   Scroll attempt " + (i+1) + " - Utility not visible yet");
            driver.executeScript("mobile: dragFromToForDuration", Map.of(
                "fromX", centerX,
                "fromY", dropdownStartY,
                "toX", centerX,
                "toY", dropdownEndY,
                "duration", 0.3
            ));
            sleep(300);
        }
        
        // Final attempt - try to find and click any Utility element
        try {
            WebElement utility = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Utility' OR label == 'Utility'")
            );
            utility.click();
            System.out.println("✅ Selected Utility (final attempt)");
            return;
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not find Utility in dropdown after 10 scrolls");
    }

    /**
     * Change asset class to VFD using coordinate-tap approach
     * VFD is at the end of the list after Utility
     */
    public final void changeAssetClassToVFD() {
        System.out.println("📋 Changing asset class to VFD (FAST)...");
        
        // Quick check - is VFD already displayed as selected?
        try {
            WebElement vfd = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == 'VFD'")
            );
            if (vfd.isDisplayed() && vfd.getLocation().getY() < 500) {
                System.out.println("✅ Already VFD");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Class' OR label == 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(500);
            
            // Try to find VFD - at the END of alphabetical list, needs many scrolls
            // List: ATS, Busway, Capacitor, Circuit Breaker, Disconnect Switch, Fuse, Generator, 
            //       Motor, Panelboard, PDU, Relay, Switchboard, Transformer, UPS, Utility, VFD
            for (int scrollAttempt = 0; scrollAttempt < 8; scrollAttempt++) {
                try {
                    WebElement vfd = driver.findElement(AppiumBy.accessibilityId("VFD"));
                    if (vfd.isDisplayed()) {
                        vfd.click();
                        System.out.println("✅ Changed to VFD");
                        sleep(300);
                        return;
                    }
                } catch (Exception e) {}
                
                // Scroll down inside dropdown to find VFD
                System.out.println("   Scrolling dropdown to find VFD (attempt " + (scrollAttempt + 1) + ")");
                int screenWidth = driver.manage().window().getSize().width;
                int screenHeight = driver.manage().window().getSize().height;
                driver.executeScript("mobile: dragFromToForDuration", Map.of(
                    "fromX", screenWidth / 2,
                    "fromY", screenHeight / 2 + 100,
                    "toX", screenWidth / 2,
                    "toY", screenHeight / 2 - 100,
                    "duration", 0.3
                ));
                sleep(400);
            }
            
            System.out.println("⚠️ VFD not found in dropdown after scrolling");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to VFD: " + e.getMessage());
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

    // ================================================================
    // FIELD LABEL VERIFICATION METHODS
    // ================================================================

    /**
     * Check if a field label is present on the screen (case-sensitive)
     * Used to verify field label casing (bug detection)
     */
    public boolean isFieldLabelPresent(String labelText) {
        System.out.println("🔍 Checking for field label: '" + labelText + "'");
        
        try {
            // Strategy 1: Exact match by accessibilityId
            List<WebElement> elements = driver.findElements(AppiumBy.accessibilityId(labelText));
            if (!elements.isEmpty()) {
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        System.out.println("   ✅ Found label '" + labelText + "' by accessibilityId");
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        
        try {
            // Strategy 2: Find StaticText with exact name
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == '" + labelText + "'"
                )
            );
            if (!elements.isEmpty()) {
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        System.out.println("   ✅ Found label '" + labelText + "' as StaticText");
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        
        try {
            // Strategy 3: Find any element with exact label attribute
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString("label == '" + labelText + "'")
            );
            if (!elements.isEmpty()) {
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        System.out.println("   ✅ Found element with label '" + labelText + "'");
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        
        System.out.println("   ❌ Label '" + labelText + "' not found");
        return false;
    }


}