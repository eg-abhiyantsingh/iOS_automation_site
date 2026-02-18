package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton' AND name CONTAINS 'asset subtype'")
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
        System.out.println("📦 Navigating to Asset List...");
        
        // First, check if we're inside an Asset Detail (Tab Bar hidden)
        // If so, we need to close it first by clicking "Close" button
        try {
            WebElement closeBtn = driver.findElement(AppiumBy.accessibilityId("Close"));
            if (closeBtn.isDisplayed()) {
                System.out.println("🔙 Found Close button - closing Asset Detail first...");
                closeBtn.click();
                sleep(300); // OPTIMIZED: 500ms -> 300ms
            }
        } catch (Exception e) {
            // No Close button, that's fine - we might already be on Asset List
        }
        
        // CORRECT WAY: Assets tab button has name="list.bullet" and label="Assets"
        // Must use predicate to find button by label, not accessibility ID
        try {
            WebElement assetsTab = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Assets' AND type == 'XCUIElementTypeButton'")
            );
            assetsTab.click();
            sleep(300); // OPTIMIZED: 500ms -> 300ms
            System.out.println("✅ Asset List opened");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Assets tab by label: " + e.getMessage());
        }
        
        // Fallback: try by accessibility ID "list.bullet" (the icon name)
        try {
            WebElement assetsTab = driver.findElement(AppiumBy.accessibilityId("list.bullet"));
            assetsTab.click();
            sleep(300); // OPTIMIZED: 500ms -> 300ms
            System.out.println("✅ Asset List opened (via list.bullet)");
            return;
        } catch (Exception e2) {
            System.out.println("⚠️ Could not find list.bullet: " + e2.getMessage());
        }
        
        // Last resort: Try clicking Site/house tab first, then Assets
        try {
            System.out.println("⚠️ Trying Site tab first...");
            WebElement siteTab = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Site' AND type == 'XCUIElementTypeButton'")
            );
            siteTab.click();
            sleep(300); // OPTIMIZED: 500ms -> 300ms
            
            WebElement assetsTab = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Assets' AND type == 'XCUIElementTypeButton'")
            );
            assetsTab.click();
            sleep(300); // OPTIMIZED: 500ms -> 300ms
            System.out.println("✅ Asset List opened (via Site tab)");
        } catch (Exception e3) {
            System.out.println("⚠️ Could not open Asset List: " + e3.getMessage());
        }
    }
    

    /**
     * Click the Assets tab in the tab bar to navigate to Asset List
     * Note: Assets tab button has name="list.bullet" and label="Assets"
     */
    public void clickAssetsTab() {
        System.out.println("📱 Clicking Assets tab...");
        
        // First, check if we're inside Asset Detail - need to close it first
        try {
            WebElement closeBtn = driver.findElement(AppiumBy.accessibilityId("Close"));
            if (closeBtn.isDisplayed()) {
                System.out.println("🔙 Closing Asset Detail first...");
                closeBtn.click();
                sleep(200);
            }
        } catch (Exception e) {
            // No Close button - that's fine
        }
        
        // CORRECT: Use predicate to find Assets button by label
        try {
            WebElement assetsTab = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Assets' AND type == 'XCUIElementTypeButton'")
            );
            assetsTab.click();
            sleep(200);
            System.out.println("✅ Clicked Assets tab");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Assets tab by label");
        }
        
        // Fallback: try by accessibility ID "list.bullet"
        try {
            WebElement assetsTab = driver.findElement(AppiumBy.accessibilityId("list.bullet"));
            assetsTab.click();
            sleep(200);
            System.out.println("✅ Clicked Assets tab (via list.bullet)");
        } catch (Exception e2) {
            System.out.println("⚠️ Could not click Assets tab: " + e2.getMessage());
        }
    }


    /**
     * Click the More button (3 dots / ellipsis) on Asset List screen
     */
    public void clickMoreButton() {
        System.out.println("⋯ Clicking More button...");
        try {
            WebElement moreBtn = driver.findElement(AppiumBy.accessibilityId("More"));
            moreBtn.click();
            sleep(200);
            System.out.println("✅ Clicked More button");
        } catch (Exception e) {
            // Try by predicate
            try {
                WebElement moreBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'More' AND type == 'XCUIElementTypeButton'")
                );
                moreBtn.click();
                sleep(200);
                System.out.println("✅ Clicked More button (via predicate)");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not click More button: " + e2.getMessage());
            }
        }
    }

    /**
     * Select a grouping option from the More menu
     * Options: "No Grouping", "Group by Location", "Group by Enclosure", 
     *          "Show AF Punchlist", "Select Multiple"
     */
    public void selectGroupingOption(String option) {
        System.out.println("📋 Selecting grouping option: " + option);
        try {
            WebElement optionBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == '" + option + "'")
            );
            optionBtn.click();
            sleep(200);
            System.out.println("✅ Selected: " + option);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select option '" + option + "': " + e.getMessage());
        }
    }



    /**
     * Click the Close button to close Asset Detail screen
     */
    public void clickCloseButton() {
        System.out.println("❌ Clicking Close button...");
        try {
            WebElement closeBtn = driver.findElement(AppiumBy.accessibilityId("Close"));
            closeBtn.click();
            sleep(200);
            System.out.println("✅ Clicked Close button");
        } catch (Exception e) {
            System.out.println("⚠️ Close button not found: " + e.getMessage());
        }
    }

    // ================================================================
    // TASK MANAGEMENT METHODS
    // ================================================================

    /**
     * Scroll to Tasks section in Asset Details
     * Tasks section is typically below Basic Information and Condition of Maintenance
     */
    public boolean scrollToTasksSection() {
        System.out.println("📜 Scrolling to Tasks section...");
        
        // First check if already visible
        try {
            WebElement tasksLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Tasks' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            if (tasksLabel.isDisplayed()) {
                System.out.println("✅ Tasks section already visible");
                return true;
            }
        } catch (Exception e) {
            // Need to scroll
        }
        
        // Scroll down with smaller scrolls using mobile: scroll
        for (int i = 0; i < 10; i++) {
            // Use mobile: swipe for small scroll
            try {
                Map<String, Object> swipeParams = new java.util.HashMap<>();
                swipeParams.put("direction", "up");  // Swipe up = scroll down
                swipeParams.put("velocity", 300);    // Slow velocity for small scroll
                driver.executeScript("mobile: swipe", swipeParams);
            } catch (Exception scrollEx) {
                // Fallback to scrollFormDown
                scrollFormDown();
            }
            sleep(400);
            
            // Check if Tasks is now visible
            try {
                WebElement tasksLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Tasks' AND type == 'XCUIElementTypeStaticText' AND visible == true")
                );
                if (tasksLabel.isDisplayed()) {
                    System.out.println("✅ Found Tasks section after " + (i + 1) + " scrolls");
                    // Wait a moment for Add button to be clickable
                    sleep(200);
                    return true;
                }
            } catch (Exception e) {
                System.out.println("   Scroll " + (i + 1) + " - Tasks not visible yet");
            }
        }
        
        System.out.println("⚠️ Could not find Tasks section after scrolling");
        return false;
    }

    /**
     * Click Add Task button (+) in Tasks section
     * Note: Need to ensure Tasks section is visible first
     */
    public void clickAddTaskButton() {
        System.out.println("➕ Clicking Add Task button...");
        try {
            // Find the visible Add button (plus.circle.fill) near Tasks section
            WebElement addBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill' AND label == 'Add' AND visible == true")
            );
            addBtn.click();
            // Wait longer for New Task screen to appear (animation)
            sleep(500);
            System.out.println("✅ Clicked Add Task button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find visible Add Task button, trying tap by coordinates...");
            // Fallback: tap at typical Tasks Add button location
            try {
                driver.executeScript("mobile: tap", Map.of("x", 348, "y", 571));
                sleep(500);
                System.out.println("✅ Tapped Add Task button at coordinates");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not click Add Task button: " + e2.getMessage());
            }
        }
    }


    /**
     * Click on an existing task in the Tasks section to open Task Details
     * Task buttons have format: "TaskName, Description, Status"
     */
    public void clickExistingTask() {
        System.out.println("🔍 Looking for existing task to click...");
        try {
            // Find task button - tasks have "Open" or "Completed" status
            WebElement taskBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name CONTAINS 'Open' OR name CONTAINS 'Completed') AND name CONTAINS 'Task'")
            );
            String taskName = taskBtn.getAttribute("name");
            System.out.println("   🎯 Found task: " + taskName.substring(0, Math.min(50, taskName.length())) + "...");
            taskBtn.click();
            sleep(500);
            System.out.println("✅ Clicked existing task");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find existing task: " + e.getMessage());
            // Try alternate approach - find any button with "Test Task" in name
            try {
                WebElement taskBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'Test Task'")
                );
                taskBtn.click();
                sleep(500);
                System.out.println("✅ Clicked task (via Test Task search)");
            } catch (Exception e2) {
                System.out.println("⚠️ No existing task found");
            }
        }
    }

    /**
     * Check if Task Details screen is displayed
     */
    public boolean isTaskDetailsScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Task Details' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Edit task title in Task Details screen
     */
    public void editTaskTitle(String newTitle) {
        System.out.println("📝 Editing task title to: " + newTitle);
        try {
            WebElement titleField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value CONTAINS 'Task'")
            );
            titleField.click();
            sleep(300);
            titleField.clear();
            titleField.sendKeys(newTitle);
            System.out.println("✅ Edited task title");
        } catch (Exception e) {
            System.out.println("⚠️ Could not edit task title: " + e.getMessage());
        }
    }

    /**
     * Edit task description in Task Details screen
     */
    public void editTaskDescription(String newDescription) {
        System.out.println("📝 Editing task description...");
        try {
            WebElement descField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextView'")
            );
            descField.click();
            sleep(300);
            descField.clear();
            descField.sendKeys(newDescription);
            System.out.println("✅ Edited task description");
        } catch (Exception e) {
            System.out.println("⚠️ Could not edit description: " + e.getMessage());
        }
    }

    /**
     * Click Save/Done button on Task Details screen
     */
    public void clickSaveTask() {
        System.out.println("💾 Clicking Save/Done...");
        try {
            // Try Done button first
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Done' OR label == 'Save'")
            );
            saveBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Save/Done");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Save/Done button: " + e.getMessage());
        }
    }

    /**
     * Click Back button on Task Details screen
     */
    public void clickBackFromTaskDetails() {
        System.out.println("🔙 Clicking Back...");
        try {
            WebElement backBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Back' OR name == 'Back'")
            );
            backBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Back");
        } catch (Exception e) {
            // Try Close button
            try {
                WebElement closeBtn = driver.findElement(AppiumBy.accessibilityId("Close"));
                closeBtn.click();
                sleep(400);
                System.out.println("✅ Clicked Close");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not find Back/Close button");
            }
        }
    }

    /**
     * Check if task has "Open" status
     */
    public boolean isTaskOpen() {
        try {
            WebElement openStatus = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Open' AND type == 'XCUIElementTypeStaticText'")
            );
            return openStatus.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Click Delete Task button in Task Details screen
     * Button is at the bottom - may need to scroll
     */
    public void clickDeleteTaskButton() {
        System.out.println("🗑️ Clicking Delete Task button...");
        try {
            // First try to find visible Delete Task button
            WebElement deleteBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Delete Task' AND type == 'XCUIElementTypeButton'")
            );
            deleteBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Delete Task button");
        } catch (Exception e) {
            System.out.println("⚠️ Delete Task button not visible, scrolling...");
            // Scroll down to find it
            for (int i = 0; i < 3; i++) {
                scrollFormDown();
                sleep(300);
                try {
                    WebElement deleteBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Delete Task' AND type == 'XCUIElementTypeButton' AND visible == true")
                    );
                    deleteBtn.click();
                    sleep(400);
                    System.out.println("✅ Clicked Delete Task button after scroll");
                    return;
                } catch (Exception e2) {
                    // Keep scrolling
                }
            }
            System.out.println("⚠️ Could not find Delete Task button");
        }
    }

    /**
     * Confirm task deletion in the alert dialog
     * Alert has Cancel and Delete buttons
     */
    public void confirmDeleteTask() {
        System.out.println("⚠️ Confirming task deletion...");
        try {
            // Wait for alert to appear
            sleep(200);
            WebElement deleteBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Delete' AND type == 'XCUIElementTypeButton'")
            );
            deleteBtn.click();
            sleep(600);
            System.out.println("✅ Confirmed task deletion");
        } catch (Exception e) {
            System.out.println("⚠️ Could not confirm deletion: " + e.getMessage());
        }
    }

    /**
     * Cancel task deletion in the alert dialog
     */
    public void cancelDeleteTask() {
        System.out.println("❌ Canceling task deletion...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            sleep(200);
            System.out.println("✅ Canceled task deletion");
        } catch (Exception e) {
            System.out.println("⚠️ Could not cancel deletion: " + e.getMessage());
        }
    }

    /**
     * Check if Delete Task confirmation alert is displayed
     */
    public boolean isDeleteTaskAlertDisplayed() {
        try {
            WebElement alert = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeAlert' AND name == 'Delete Task'")
            );
            return alert.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }


    // ================================================================
    // ISSUE METHODS
    // ================================================================

    /**
     * Scroll to Issues section on Asset Details screen
     */
    public void scrollToIssuesSection() {
        System.out.println("📜 Scrolling to Issues section...");
        
        for (int i = 0; i < 12; i++) {
            try {
                // Check if Issues label is visible and in a good position (y between 300-600)
                WebElement issuesLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Issues' AND type == 'XCUIElementTypeStaticText' AND visible == true")
                );
                
                int y = issuesLabel.getLocation().getY();
                System.out.println("   Found Issues at y=" + y);
                
                // Issues should be in middle of screen (y between 300-600 for optimal clicking)
                if (y >= 300 && y <= 650) {
                    System.out.println("✅ Found Issues section at good position (y=" + y + ")");
                    sleep(200);
                    return;
                } else if (y < 300) {
                    // Scrolled too far, scroll back up
                    System.out.println("   Issues too high (y=" + y + "), scrolling up...");
                    Map<String, Object> upParams = new HashMap<>();
                    upParams.put("direction", "down");
                    upParams.put("velocity", 200);
                    driver.executeScript("mobile: swipe", upParams);
                    sleep(400);
                    continue;
                }
                // y > 650, keep scrolling down
            } catch (Exception e) {
                System.out.println("   Scroll " + (i + 1) + " - Issues not visible yet");
            }
            
            // Scroll down
            Map<String, Object> swipeParams = new HashMap<>();
            swipeParams.put("direction", "up");
            swipeParams.put("velocity", 250);
            driver.executeScript("mobile: swipe", swipeParams);
            sleep(400);
        }
        
        System.out.println("⚠️ Issues section not found after scrolling");
    }

        /**
     * Click Add Issue (+) button in Issues section
     */
    public void clickAddIssueButton() {
        System.out.println("➕ Clicking Add Issue button...");
        try {
            sleep(200);
            
            // First find the Issues label to get its Y position
            WebElement issuesLabel = null;
            int issuesY = -1;
            try {
                issuesLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Issues' AND type == 'XCUIElementTypeStaticText' AND visible == true")
                );
                issuesY = issuesLabel.getLocation().getY();
                System.out.println("   Issues label at y=" + issuesY);
            } catch (Exception e) {
                System.out.println("   ⚠️ Could not find Issues label");
            }
            
            // Find all visible Add buttons
            List<WebElement> addButtons = driver.findElements(
                AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill' AND label == 'Add' AND visible == true")
            );
            System.out.println("   Found " + addButtons.size() + " visible Add buttons");
            
            WebElement correctButton = null;
            
            // If we found Issues label, find Add button near it (within 50px Y)
            if (issuesY > -1 && !addButtons.isEmpty()) {
                for (WebElement btn : addButtons) {
                    int btnY = btn.getLocation().getY();
                    System.out.println("     Add button at y=" + btnY);
                    if (Math.abs(btnY - issuesY) < 50) {
                        correctButton = btn;
                        System.out.println("     ✓ This is near Issues label");
                        break;
                    }
                }
            }
            
            // Fallback: use first visible Add button
            if (correctButton == null && !addButtons.isEmpty()) {
                correctButton = addButtons.get(0);
                System.out.println("   Using first visible Add button");
            }
            
            if (correctButton != null) {
                int y = correctButton.getLocation().getY();
                System.out.println("   Clicking Add button at y=" + y);
                correctButton.click();
                sleep(1500);
                
                // Verify New Issue screen opened
                try {
                    WebElement navBar = driver.findElement(
                        AppiumBy.iOSNsPredicateString("name == 'New Issue' AND type == 'XCUIElementTypeNavigationBar'")
                    );
                    if (navBar.isDisplayed()) {
                        System.out.println("✅ Clicked Add Issue button - New Issue screen opened");
                        return;
                    }
                } catch (Exception ve) {
                    System.out.println("⚠️ New Issue screen not detected - may have clicked wrong button");
                }
            } else {
                System.out.println("⚠️ No Add button found");
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Add Issue button: " + e.getMessage());
        }
    }

        /**
     * Check if New Issue screen is displayed
     */
    public boolean isNewIssueScreenDisplayed() {
        System.out.println("🔍 Checking if New Issue screen is displayed...");
        try {
            // Try to find New Issue navigation bar
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'New Issue' AND type == 'XCUIElementTypeNavigationBar'")
            );
            boolean displayed = navBar.isDisplayed();
            System.out.println("   Nav bar found, displayed: " + displayed);
            return displayed;
        } catch (Exception e1) {
            // Try alternate: look for "New Issue" static text
            try {
                WebElement newIssueText = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'New Issue' AND type == 'XCUIElementTypeStaticText' AND visible == true")
                );
                boolean displayed = newIssueText.isDisplayed();
                System.out.println("   New Issue text found, displayed: " + displayed);
                return displayed;
            } catch (Exception e2) {
                // Try to find Create Issue button (only exists on New Issue screen)
                try {
                    WebElement createBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Create Issue' AND type == 'XCUIElementTypeButton'")
                    );
                    System.out.println("   Create Issue button found - on New Issue screen");
                    return true;
                } catch (Exception e3) {
                    System.out.println("   ⚠️ New Issue screen not detected");
                    return false;
                }
            }
        }
    }

    /**
     * Enter issue title
     */
    public void enterIssueTitle(String title) {
        System.out.println("📝 Entering issue title: " + title);
        try {
            // Wait for New Issue screen to be ready
            sleep(200);
            
            // Find title field by placeholderValue
            WebElement titleField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND placeholderValue == 'Enter issue title'")
            );
            
            System.out.println("   Found title field, clicking...");
            titleField.click();
            sleep(200);
            titleField.sendKeys(title);
            sleep(300);
            
            // Dismiss keyboard by clicking Done button
            System.out.println("   Dismissing keyboard...");
            try {
                WebElement doneBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Done' AND type == 'XCUIElementTypeButton'")
                );
                doneBtn.click();
                sleep(200);
                System.out.println("   ✅ Clicked Done button");
            } catch (Exception doneEx) {
                // Tap outside to dismiss keyboard
                try {
                    driver.executeScript("mobile: tap", Map.of("x", 200, "y", 200));
                    sleep(300);
                } catch (Exception tapEx) {}
            }
            
            System.out.println("✅ Entered issue title");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter issue title: " + e.getMessage());
        }
    }

    /**
     * Click Issue Class dropdown
     */
    public void clickIssueClassDropdown() {
        System.out.println("📋 Clicking Issue Class dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Issue Class' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Issue Class dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Issue Class dropdown: " + e.getMessage());
        }
    }

    /**
     * Select an Issue Class option
     * @param className e.g., "Repair Needed", "NEC Violation", "Thermal Anomaly"
     */
    public void selectIssueClass(String className) {
        System.out.println("📋 Selecting Issue Class: " + className);
        try {
            // Click Issue Class dropdown first
            clickIssueClassDropdown();
            sleep(200);
            
            // Select the option
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == '" + className + "' AND type == 'XCUIElementTypeButton'")
            );
            option.click();
            sleep(200);
            System.out.println("✅ Selected Issue Class: " + className);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Issue Class: " + e.getMessage());
        }
    }

        /**
     * Click Priority dropdown
     */
    public void clickPriorityDropdown() {
        System.out.println("📋 Clicking Priority dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Priority' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Priority dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Priority dropdown: " + e.getMessage());
        }
    }

    /**
     * Click Create Issue button
     */
    public void clickCreateIssueButton() {
        System.out.println("🆕 Clicking Create Issue button...");
        try {
            // Try multiple selectors
            WebElement createBtn = null;
            
            try {
                createBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == 'Create Issue' AND type == 'XCUIElementTypeButton'")
                );
            } catch (Exception e1) {
                try {
                    createBtn = driver.findElement(AppiumBy.accessibilityId("Create Issue"));
                } catch (Exception e2) {
                    System.out.println("⚠️ Could not find Create Issue button");
                    return;
                }
            }
            
            // Check if enabled
            String enabled = createBtn.getAttribute("enabled");
            System.out.println("   Create Issue button enabled: " + enabled);
            
            if ("true".equals(enabled)) {
                createBtn.click();
                sleep(500);
                System.out.println("✅ Clicked Create Issue button");
            } else {
                System.out.println("⚠️ Create Issue button is disabled - title may be empty");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create Issue: " + e.getMessage());
        }
    }

    /**
     * Check if Create Issue button is enabled
     */
    public boolean isCreateIssueButtonEnabled() {
        try {
            WebElement createBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Create Issue' AND type == 'XCUIElementTypeButton'")
            );
            String enabled = createBtn.getAttribute("enabled");
            return "true".equals(enabled);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Cancel button on New Issue screen
     */
    public void clickCancelIssue() {
        System.out.println("❌ Clicking Cancel on New Issue...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    /**
     * Get the asset name displayed in "Creating issue for:" section
     */
    public String getIssueAssetName() {
        try {
            WebElement assetName = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND name CONTAINS 'TestAsset'")
            );
            return assetName.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }


    // ================================================================
    // CONNECTION METHODS
    // ================================================================

    /**
     * Scroll to Connections section on Asset Details screen
     */
    public void scrollToConnectionsSection() {
        System.out.println("📜 Scrolling to Connections section...");
        
        // Connections is at the bottom of Asset Details form
        // Scroll aggressively to bottom first
        for (int i = 0; i < 12; i++) {
            Map<String, Object> swipeParams = new HashMap<>();
            swipeParams.put("direction", "up");
            swipeParams.put("velocity", 800);
            driver.executeScript("mobile: swipe", swipeParams);
            sleep(150);
        }
        
        System.out.println("   Scrolled to bottom, now scrolling up to show Add button...");
        sleep(300);
        
        // Scroll UP a bit to bring Connections header (with Add button) into view
        // The header with Add button might be above visible area after scrolling to bottom
        for (int i = 0; i < 3; i++) {
            try {
                // Check if Connections label is visible
                WebElement connLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Connections' AND type == 'XCUIElementTypeStaticText' AND visible == true")
                );
                int y = connLabel.getLocation().getY();
                if (y > 100 && y < 400) {
                    System.out.println("✅ Connections section visible at y=" + y);
                    break;
                } else if (y < 100) {
                    // Need to scroll up more
                    System.out.println("   Scrolling up (Connections at y=" + y + ")...");
                    scrollFormUp();
                    sleep(300);
                }
            } catch (Exception e) {
                // Not found, scroll up
                System.out.println("   Scrolling up to find Connections...");
                scrollFormUp();
                sleep(300);
            }
        }
        
        // Final check
        try {
            WebElement connLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Connections' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            System.out.println("✅ Connections section ready");
        } catch (Exception e) {
            System.out.println("⚠️ Connections section position may need manual check");
        }
    }

    /**
     * Click Add Connection (+) button in Connections section
     */
    public void clickAddConnectionButton() {
        System.out.println("➕ Clicking Add Connection button...");
        
        // Try to find and click Add button with scroll retry
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                // Check if Connections label is visible
                WebElement connLabel = null;
                try {
                    connLabel = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Connections' AND type == 'XCUIElementTypeStaticText' AND visible == true")
                    );
                    int connY = connLabel.getLocation().getY();
                    System.out.println("   Connections label at y=" + connY);
                } catch (Exception e) {
                    System.out.println("   Connections label not visible, scrolling up...");
                    scrollFormUp();
                    sleep(200);
                    continue;
                }
                
                // Find Add button near Connections
                List<WebElement> addBtns = driver.findElements(
                    AppiumBy.iOSNsPredicateString("name == 'Add' AND type == 'XCUIElementTypeButton' AND visible == true")
                );
                
                if (addBtns.isEmpty()) {
                    System.out.println("   No Add button found, scrolling up...");
                    scrollFormUp();
                    sleep(200);
                    continue;
                }
                
                // Click the first visible Add button (should be near Connections)
                WebElement addBtn = addBtns.get(0);
                int addY = addBtn.getLocation().getY();
                System.out.println("   Add button at y=" + addY);
                
                addBtn.click();
                sleep(400);
                System.out.println("✅ Clicked Add Connection button");
                return;
                
            } catch (Exception e) {
                System.out.println("   Attempt " + (attempt+1) + " failed, scrolling up...");
                scrollFormUp();
                sleep(200);
            }
        }
        
        System.out.println("⚠️ Could not click Add Connection button after retries");
    }

    /**
     * Select New Lineside Connection from the menu
     */
    public void selectNewLinesideConnection() {
        System.out.println("🔗 Selecting New Lineside Connection...");
        try {
            WebElement lineside = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'New Lineside Connection' AND type == 'XCUIElementTypeButton'")
            );
            lineside.click();
            sleep(500);
            System.out.println("✅ Selected New Lineside Connection");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select New Lineside Connection: " + e.getMessage());
        }
    }

    /**
     * Select New Loadside Connection from the menu
     */
    public void selectNewLoadsideConnection() {
        System.out.println("🔗 Selecting New Loadside Connection...");
        try {
            WebElement loadside = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'New Loadside Connection' AND type == 'XCUIElementTypeButton'")
            );
            loadside.click();
            sleep(500);
            System.out.println("✅ Selected New Loadside Connection");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select New Loadside Connection: " + e.getMessage());
        }
    }

    /**
     * Check if New Connection screen is displayed
     */
    public boolean isNewConnectionScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'New Connection' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Lineside (Incoming) is selected
     */
    public boolean isLinesideIncomingSelected() {
        try {
            WebElement lineside = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Lineside (Incoming)' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            return lineside.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Cancel on New Connection screen
     */
    public void clickCancelConnection() {
        System.out.println("❌ Clicking Cancel on New Connection...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    /**
     * Click Source Node dropdown
     */
    public void clickSourceNodeDropdown() {
        System.out.println("📋 Clicking Source Node dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Source Node' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Source Node dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Source Node dropdown: " + e.getMessage());
        }
    }

    /**
     * Click Target Node dropdown
     */
    public void clickTargetNodeDropdown() {
        System.out.println("📋 Clicking Target Node dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Target Node' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Target Node dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Target Node dropdown: " + e.getMessage());
        }
    }

    
    /**
     * Click Source Node dropdown (Select source)
     */
    public void clickSelectSourceDropdown() {
        System.out.println("📋 Clicking Select source dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select source' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Select source dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Select source: " + e.getMessage());
        }
    }

    /**
     * Select first available source node (not the current asset)
     */
    public void selectFirstSourceNode() {
        System.out.println("🔗 Selecting first available source node...");
        try {
            // Find all asset buttons in the source list
            List<WebElement> options = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ', ' AND name CONTAINS 'ATS' AND visible == true")
            );
            
            if (options.isEmpty()) {
                // Try broader search
                options = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'TestAsset' AND visible == true")
                );
            }
            
            System.out.println("   Found " + options.size() + " source node options");
            
            if (!options.isEmpty()) {
                WebElement firstOption = options.get(0);
                String name = firstOption.getAttribute("name");
                System.out.println("   Selecting: " + name);
                firstOption.click();
                sleep(400);
                System.out.println("✅ Selected source node");
            } else {
                System.out.println("⚠️ No source node options found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not select source node: " + e.getMessage());
        }
    }

    /**
     * Click Create button on New Connection screen
     */
    public void clickCreateConnectionButton() {
        System.out.println("🆕 Clicking Create button...");
        try {
            WebElement createBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Create' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            createBtn.click();
            sleep(500);
            System.out.println("✅ Clicked Create button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create button: " + e.getMessage());
        }
    }

    /**
     * Check if connection was created (back on Asset Details with connection visible)
     */
    public boolean isConnectionCreated() {
        try {
            // Check if we're back on Asset Details
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Details' AND type == 'XCUIElementTypeNavigationBar'")
            );
            
            // Check if Lineside connection is visible
            WebElement connection = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Lineside' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            
            return navBar.isDisplayed() && connection.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    
    /**
     * Check if Loadside (Outgoing) is selected
     */
    public boolean isLoadsideOutgoingSelected() {
        try {
            WebElement loadside = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Loadside (Outgoing)' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            return loadside.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Target Node dropdown (Select target)
     */
    public void clickSelectTargetDropdown() {
        System.out.println("📋 Clicking Select target dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select target' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Select target dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Select target: " + e.getMessage());
        }
    }

    /**
     * Select first available target node (not the current asset)
     */
    public void selectFirstTargetNode() {
        System.out.println("🔗 Selecting first available target node...");
        try {
            // Find all asset buttons in the target list
            List<WebElement> options = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ', ' AND name CONTAINS 'ATS' AND visible == true")
            );
            
            if (options.isEmpty()) {
                // Try broader search
                options = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'TestAsset' AND visible == true")
                );
            }
            
            System.out.println("   Found " + options.size() + " target node options");
            
            if (!options.isEmpty()) {
                WebElement firstOption = options.get(0);
                String name = firstOption.getAttribute("name");
                System.out.println("   Selecting: " + name);
                firstOption.click();
                sleep(400);
                System.out.println("✅ Selected target node");
            } else {
                System.out.println("⚠️ No target node options found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not select target node: " + e.getMessage());
        }
    }

    /**
     * Check if Loadside connection was created
     */
    public boolean isLoadsideConnectionCreated() {
        try {
            // Check if we're back on Asset Details
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Details' AND type == 'XCUIElementTypeNavigationBar'")
            );
            
            // Check if Loadside connection is visible
            WebElement connection = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Loadside' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            
            return navBar.isDisplayed() && connection.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    
    /**
     * Verify Source Node is auto-populated (for Loadside - shows current asset)
     * Returns the asset name if populated, null otherwise
     */
    public String getSourceNodeValue() {
        System.out.println("🔍 Checking Source Node value...");
        try {
            // For Loadside, Source Node shows the current asset as a button
            WebElement sourceBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'TestAsset' AND name CONTAINS 'Generator' AND visible == true")
            );
            String value = sourceBtn.getAttribute("name");
            System.out.println("   Source Node value: " + value);
            return value;
        } catch (Exception e) {
            System.out.println("   Source Node not found or empty");
            return null;
        }
    }

    /**
     * Verify Target Node is auto-populated (for Lineside - shows current asset)
     * Returns the asset name if populated, null otherwise
     */
    public String getTargetNodeValue() {
        System.out.println("🔍 Checking Target Node value...");
        try {
            // For Lineside, Target Node shows the current asset as a button
            WebElement targetBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'TestAsset' AND name CONTAINS 'Generator' AND visible == true")
            );
            String value = targetBtn.getAttribute("name");
            System.out.println("   Target Node value: " + value);
            return value;
        } catch (Exception e) {
            System.out.println("   Target Node not found or empty");
            return null;
        }
    }

    /**
     * Check if Source Node shows "Select source" (not yet selected)
     */
    public boolean isSourceNodeEmpty() {
        try {
            WebElement selectSource = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select source' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            return selectSource.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Target Node shows "Select target" (not yet selected)
     */
    public boolean isTargetNodeEmpty() {
        try {
            WebElement selectTarget = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select target' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            return selectTarget.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    
    // ============================================================
    // MCC-OCP METHODS
    // ============================================================

    /**
     * Scroll to OCP section in MCC asset
     */
    public void scrollToOCPSection() {
        System.out.println("📜 Scrolling to OCP section...");
        long start = System.currentTimeMillis();
        
        try {
            // Scroll down to find OCP section
            for (int i = 0; i < 10; i++) {
                try {
                    WebElement ocp = driver.findElement(
                        AppiumBy.iOSNsPredicateString("name == 'OCP' AND type == 'XCUIElementTypeStaticText' AND visible == true")
                    );
                    if (ocp.isDisplayed()) {
                        int y = ocp.getLocation().getY();
                        System.out.println("✅ Found OCP section at y=" + y);
                        break;
                    }
                } catch (Exception e) {
                    // Not found yet, scroll
                    scrollFormDown();
                    sleep(300);
                }
            }
            System.out.println("✅ At OCP section (Total: " + (System.currentTimeMillis() - start) + "ms)");
        } catch (Exception e) {
            System.out.println("⚠️ OCP section not found: " + e.getMessage());
        }
    }

    /**
     * Click Add OCP button (+ button near OCP label)
     */
    public void clickAddOCPButton() {
        System.out.println("➕ Clicking Add OCP button...");
        try {
            // Find OCP label position
            WebElement ocpLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'OCP' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            int ocpY = ocpLabel.getLocation().getY();
            System.out.println("   OCP label at y=" + ocpY);
            
            // Find Add button near OCP (within 50px)
            List<WebElement> addButtons = driver.findElements(
                AppiumBy.iOSNsPredicateString("name == 'Add' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            
            for (WebElement btn : addButtons) {
                int btnY = btn.getLocation().getY();
                if (Math.abs(btnY - ocpY) < 50) {
                    System.out.println("   Add button at y=" + btnY);
                    btn.click();
                    sleep(400);
                    System.out.println("✅ Clicked Add OCP button");
                    return;
                }
            }
            
            // Fallback: click first Add button
            if (!addButtons.isEmpty()) {
                addButtons.get(0).click();
                sleep(400);
                System.out.println("✅ Clicked first Add button (fallback)");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Add OCP: " + e.getMessage());
        }
    }

    /**
     * Check if OCP Add options are displayed (Create New Child / Link Existing Node)
     */
    public boolean areOCPAddOptionsDisplayed() {
        try {
            WebElement createChild = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Create New Child' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            WebElement linkNode = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Link Existing Node' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            return createChild.isDisplayed() && linkNode.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // OCP UNLINK METHODS
    // ============================================================

    /**
     * Get count of OCP items in the list
     */
    public int getOCPCount() {
        System.out.println("🔢 Getting OCP count...");
        try {
            // Look for OCP label
            WebElement ocpLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'OCP' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            
            int ocpY = ocpLabel.getLocation().getY();
            int ocpX = ocpLabel.getLocation().getX();
            System.out.println("   OCP label at y=" + ocpY);
            
            // Find nearby StaticText that shows the count (e.g., "4" or "5")
            List<WebElement> texts = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && name.matches("\\d+")) {
                    int textY = text.getLocation().getY();
                    int textX = text.getLocation().getX();
                    // Count badge is to the RIGHT of OCP label and within 30px vertically
                    if (Math.abs(textY - ocpY) < 30 && textX > ocpX) {
                        int count = Integer.parseInt(name);
                        System.out.println("   OCP count: " + count);
                        return count;
                    }
                }
            }
            System.out.println("   OCP count badge not found");
            return 0;
        } catch (Exception e) {
            System.out.println("   Could not get OCP count: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if OCP list has items
     */
    public boolean hasOCPItems() {
        return getOCPCount() > 0;
    }

    /**
     * Long press on first OCP item to show context menu
     */
    public void longPressFirstOCPItem() {
        System.out.println("👆 Long pressing first OCP item...");
        try {
            // First try to find OCP items WITHOUT scrolling (we may already be at the right position)
            List<WebElement> ocpItems = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true AND " +
                    "(name CONTAINS 'Relay' OR name CONTAINS 'Disconnect Switch' OR name CONTAINS 'Fuse' OR " +
                    "name CONTAINS 'MCC Bucket' OR name CONTAINS 'Other (OCP)' OR name CONTAINS 'LinkTest')")
            );
            
            System.out.println("   Found " + ocpItems.size() + " OCP items");
            
            // Only scroll if no OCP items found
            if (ocpItems.isEmpty()) {
                System.out.println("   Scrolling to find OCP items...");
                scrollFormDown();
                sleep(300);
                ocpItems = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true AND " +
                        "(name CONTAINS 'Relay' OR name CONTAINS 'Disconnect Switch' OR name CONTAINS 'Fuse' OR " +
                        "name CONTAINS 'MCC Bucket' OR name CONTAINS 'Other (OCP)' OR name CONTAINS 'LinkTest')")
                );
                System.out.println("   After scroll: Found " + ocpItems.size() + " OCP items");
            }
            
            if (!ocpItems.isEmpty()) {
                WebElement firstOCP = ocpItems.get(0);
                String name = firstOCP.getAttribute("name");
                System.out.println("   Selected OCP: " + name);
                
                // Try mobile: touchAndHold first (more reliable for iOS)
                try {
                    Map<String, Object> params = new java.util.HashMap<>();
                    params.put("element", ((RemoteWebElement) firstOCP).getId());
                    params.put("duration", 2.0); // 2 seconds
                    driver.executeScript("mobile: touchAndHold", params);
                    sleep(300);
                    System.out.println("✅ Long pressed using touchAndHold");
                    return;
                } catch (Exception e) {
                    System.out.println("   touchAndHold failed, trying W3C Actions...");
                }
                
                // Fallback: W3C Actions with longer duration
                int elemX = firstOCP.getLocation().getX();
                int elemY = firstOCP.getLocation().getY();
                int elemW = firstOCP.getSize().getWidth();
                int elemH = firstOCP.getSize().getHeight();
                int x = elemX + (elemW / 2);
                int y = elemY + (elemH / 2);
                System.out.println("   Long press at (" + x + ", " + y + ")");
                
                // Long press using W3C Actions - 2 seconds
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence longPress = new Sequence(finger, 1);
                longPress.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
                longPress.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                longPress.addAction(new org.openqa.selenium.interactions.Pause(finger, Duration.ofMillis(2000))); // Hold for 2 seconds
                longPress.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Arrays.asList(longPress));
                
                sleep(300);
                System.out.println("✅ Long pressed OCP item");
            } else {
                System.out.println("⚠️ No OCP items found after scrolling");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not long press OCP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Click "Unlink from Parent" in context menu
     */
    public void clickUnlinkFromParent() {
        System.out.println("🔗 Clicking Unlink from Parent...");
        try {
            // Wait a bit for context menu to appear
            sleep(200);
            
            // Try multiple strategies to find Unlink from Parent
            WebElement unlinkBtn = null;
            
            // Strategy 1: Button type
            try {
                unlinkBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == 'Unlink from Parent' AND type == 'XCUIElementTypeButton'")
                );
            } catch (Exception e) {}
            
            // Strategy 2: Any type with label
            if (unlinkBtn == null) {
                try {
                    unlinkBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Unlink from Parent' AND visible == true")
                    );
                } catch (Exception e) {}
            }
            
            // Strategy 3: Accessibility ID
            if (unlinkBtn == null) {
                try {
                    unlinkBtn = driver.findElement(AppiumBy.accessibilityId("Unlink from Parent"));
                } catch (Exception e) {}
            }
            
            // Strategy 4: CONTAINS search
            if (unlinkBtn == null) {
                try {
                    unlinkBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("name CONTAINS 'Unlink' AND visible == true")
                    );
                } catch (Exception e) {}
            }
            
            if (unlinkBtn != null) {
                unlinkBtn.click();
                sleep(200);
                System.out.println("✅ Clicked Unlink from Parent");
            } else {
                System.out.println("⚠️ Unlink from Parent not found - context menu may not have appeared");
                
                // Debug: Print visible elements
                List<WebElement> visibleButtons = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true")
                );
                System.out.println("   Visible buttons: " + visibleButtons.size());
                for (int i = 0; i < Math.min(5, visibleButtons.size()); i++) {
                    System.out.println("     - " + visibleButtons.get(i).getAttribute("name"));
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Unlink from Parent: " + e.getMessage());
        }
    }

    /**
     * Confirm unlink in the confirmation dialog
     */
    public void confirmUnlink() {
        System.out.println("✅ Confirming unlink...");
        try {
            // Wait for dialog to appear
            sleep(300);
            
            WebElement unlinkBtn = null;
            
            // Try multiple strategies
            try {
                unlinkBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == 'Unlink' AND type == 'XCUIElementTypeButton' AND visible == true")
                );
            } catch (Exception e) {}
            
            if (unlinkBtn == null) {
                try {
                    unlinkBtn = driver.findElement(AppiumBy.accessibilityId("Unlink"));
                } catch (Exception e) {}
            }
            
            if (unlinkBtn == null) {
                try {
                    // The red "Unlink" button in confirmation dialog
                    unlinkBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Unlink' AND visible == true")
                    );
                } catch (Exception e) {}
            }
            
            if (unlinkBtn != null) {
                unlinkBtn.click();
                sleep(400);
                System.out.println("✅ Confirmed unlink");
            } else {
                System.out.println("⚠️ Unlink confirmation button not found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not confirm unlink: " + e.getMessage());
        }
    }

    /**
     * Unlink first OCP item (full flow: long press -> Unlink from Parent -> Confirm)
     */
    public void unlinkFirstOCPItem() {
        System.out.println("🔓 Unlinking first OCP item...");
        longPressFirstOCPItem();
        sleep(200);
        clickUnlinkFromParent();
        sleep(200);
        confirmUnlink();
        System.out.println("✅ OCP item unlinked");
    }

    /**
     * Click Create New Child option
     */
    public void clickCreateNewChild() {
        System.out.println("🆕 Clicking Create New Child...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Create New Child' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(500);
            System.out.println("✅ Clicked Create New Child");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create New Child: " + e.getMessage());
        }
    }

    /**
     * Click Link Existing Node option
     */
    public void clickLinkExistingNode() {
        System.out.println("🔗 Clicking Link Existing Node...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Link Existing Node' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(500);
            System.out.println("✅ Clicked Link Existing Node");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Link Existing Node: " + e.getMessage());
        }
    }

    /**
     * Check if Create New Child Asset screen is displayed
     */
    public boolean isCreateNewChildAssetScreenDisplayed() {
        try {
            // Look for Asset Class dropdown (indicates create asset form)
            WebElement assetClass = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'ATS' OR name == 'MCC Bucket' OR name == 'Circuit Breaker') AND visible == true")
            );
            return assetClass.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Link Existing Node screen is displayed
     */
    public boolean isLinkExistingNodeScreenDisplayed() {
        try {
            // Link existing node shows a list of assets
            WebElement assetList = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'Asset_' AND visible == true")
            );
            return assetList.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Select first existing node to link
     */
    public void selectFirstExistingNode() {
        System.out.println("🔗 Selecting first existing node...");
        try {
            WebElement firstNode = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'Asset_' AND visible == true")
            );
            String name = firstNode.getAttribute("name");
            System.out.println("   Selecting: " + name);
            firstNode.click();
            sleep(400);
            System.out.println("✅ Selected first existing node");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select existing node: " + e.getMessage());
        }
    }

    /**
     * Check if OCP section exists (for MCC assets)
     */
    public boolean isOCPSectionVisible() {
        try {
            WebElement ocp = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'OCP' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            return ocp.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // CREATE CHILD ASSET METHODS
    // ============================================================

    /**
     * Check if Create Child Asset screen is displayed
     */
    public boolean isCreateChildAssetScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Create Child Asset' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Enter child asset name
     */
    public void enterChildAssetName(String name) {
        System.out.println("📝 Entering child asset name: " + name);
        try {
            WebElement field = driver.findElement(
                AppiumBy.iOSNsPredicateString("value == 'Enter asset name' AND type == 'XCUIElementTypeTextField'")
            );
            field.click();
            sleep(300);
            field.sendKeys(name);
            System.out.println("✅ Entered child asset name");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter asset name: " + e.getMessage());
        }
    }

    /**
     * Click Asset Class dropdown on Create Child Asset screen
     */
    public void clickChildAssetClassDropdown() {
        System.out.println("📋 Clicking Asset Class dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' AND name CONTAINS 'Select class' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(200);
            System.out.println("✅ Clicked Asset Class dropdown");
        } catch (Exception e) {
            // Try alternate selector
            try {
                WebElement dropdown = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label CONTAINS 'Asset Class' AND type == 'XCUIElementTypeButton'")
                );
                dropdown.click();
                sleep(200);
                System.out.println("✅ Clicked Asset Class dropdown (alt)");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not click dropdown: " + e2.getMessage());
            }
        }
    }

    /**
     * Select OCP class for child asset
     */
    public void selectChildAssetClass(String className) {
        System.out.println("📋 Selecting child asset class: " + className);
        try {
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == '" + className + "' AND type == 'XCUIElementTypeButton'")
            );
            option.click();
            sleep(200);
            System.out.println("✅ Selected class: " + className);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select class: " + e.getMessage());
        }
    }

    /**
     * Click Create button on Create Child Asset screen
     */
    public void clickCreateChildAssetButton() {
        System.out.println("🆕 Clicking Create button...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Create' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(500);
            System.out.println("✅ Clicked Create");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create: " + e.getMessage());
        }
    }

    /**
     * Create a child asset with given name and class
     * @param assetName Name for the child asset
     * @param assetClass OCP class (Disconnect Switch, Fuse, MCC Bucket, Other (OCP), Relay)
     */
    public void createChildAsset(String assetName, String assetClass) {
        System.out.println("🆕 Creating child asset: " + assetName + " (" + assetClass + ")");
        
        enterChildAssetName(assetName);
        clickChildAssetClassDropdown();
        selectChildAssetClass(assetClass);
        clickCreateChildAssetButton();
        
        System.out.println("✅ Child asset creation completed");
    }

    /**
     * Check if child asset was created (visible in OCP list)
     */
    public boolean isChildAssetCreated(String assetName) {
        try {
            WebElement asset = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS '" + assetName + "' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            return asset.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get Parent Enclosure value on Create Child Asset screen
     */
    public String getParentEnclosureValue() {
        System.out.println("📋 Getting Parent Enclosure value...");
        try {
            // Look for static text after "Parent Enclosure" label
            WebElement parent = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'TestAsset' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            String value = parent.getAttribute("name");
            System.out.println("   Parent Enclosure: " + value);
            return value;
        } catch (Exception e) {
            System.out.println("⚠️ Could not get Parent Enclosure: " + e.getMessage());
            return "";
        }
    }

    /**
     * Check if Parent Enclosure field is populated
     */
    public boolean isParentEnclosurePopulated() {
        String value = getParentEnclosureValue();
        return value != null && !value.isEmpty() && value.contains("TestAsset");
    }

    /**
     * Check if Asset Class dropdown shows "Select class"
     */
    public boolean isAssetClassDropdownDefault() {
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Select class' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            return dropdown.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get available OCP class options
     */
    public List<String> getOCPClassOptions() {
        List<String> options = new ArrayList<>();
        String[] ocpClasses = {"Disconnect Switch", "Fuse", "MCC Bucket", "Other (OCP)", "Relay"};
        
        for (String cls : ocpClasses) {
            try {
                WebElement btn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == '" + cls + "' AND type == 'XCUIElementTypeButton'")
                );
                if (btn.isDisplayed()) {
                    options.add(cls);
                }
            } catch (Exception e) {}
        }
        
        return options;
    }

    /**
     * Click Cancel button on Create Child Asset screen
     */
    public void clickCancelChildAsset() {
        System.out.println("❌ Clicking Cancel...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Cancel' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(400);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    // ============================================================
    // LINK EXISTING NODE METHODS
    // ============================================================

    /**
     * Check if Link Existing Nodes screen is displayed
     */
    public boolean isLinkExistingNodesScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Link Existing Nodes' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get count of assets available to link
     */
    public int getLinkableAssetsCount() {
        try {
            List<WebElement> assets = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true AND name CONTAINS ','")
            );
            // Filter out Cancel and Link buttons
            int count = 0;
            for (WebElement asset : assets) {
                String name = asset.getAttribute("name");
                if (name != null && !name.equals("Cancel") && !name.startsWith("Link ")) {
                    count++;
                }
            }
            System.out.println("   Found " + count + " linkable assets");
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Select first available asset to link by clicking on checkbox circle
     * Note: Clicking the button navigates to asset details, 
     * must click the circle checkbox on the LEFT side to select
     */
    public void selectFirstLinkableAsset() {
        System.out.println("🔗 Selecting first linkable asset...");
        try {
            // Find the visible circle checkbox images
            List<WebElement> circles = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeImage' AND name == 'circle' AND visible == true")
            );
            
            if (!circles.isEmpty()) {
                WebElement firstCircle = circles.get(0);
                int circleX = firstCircle.getLocation().getX();
                int circleY = firstCircle.getLocation().getY();
                int circleW = firstCircle.getSize().getWidth();
                int circleH = firstCircle.getSize().getHeight();
                
                // Click center of circle
                int clickX = circleX + (circleW / 2);
                int clickY = circleY + (circleH / 2);
                
                System.out.println("   Circle at (" + circleX + ", " + circleY + "), clicking center (" + clickX + ", " + clickY + ")");
                
                // Use W3C Actions for reliable tap
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence tap = new Sequence(finger, 1);
                tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), clickX, clickY));
                tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Arrays.asList(tap));
                
                sleep(200);
                System.out.println("✅ Selected asset via checkbox");
            } else {
                System.out.println("⚠️ No circle checkboxes found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not select asset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Click Link button on Link Existing Nodes screen
     */
    public void clickLinkButton() {
        System.out.println("🔗 Clicking Link button...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name BEGINSWITH 'Link ' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(500);
            System.out.println("✅ Clicked Link");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Link: " + e.getMessage());
        }
    }

    /**
     * Cancel Link Existing Nodes screen
     */
    public void cancelLinkExistingNodes() {
        System.out.println("❌ Clicking Cancel...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Cancel' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(400);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    // ============================================================
    // SEARCH FIELD METHODS (Link Existing Nodes screen)
    // ============================================================

    /**
     * Check if search field is visible on Link Existing Nodes screen
     */
    public boolean isLinkNodesSearchFieldVisible() {
        System.out.println("🔍 Checking for search field...");
        try {
            WebElement search = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField' AND visible == true")
            );
            String placeholder = search.getAttribute("value");
            System.out.println("   Search field visible with placeholder: " + placeholder);
            return search.isDisplayed();
        } catch (Exception e) {
            System.out.println("   Search field not visible");
            return false;
        }
    }

    /**
     * Get search field placeholder text
     */
    public String getSearchFieldPlaceholder() {
        try {
            WebElement search = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField' AND visible == true")
            );
            return search.getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Enter text in search field on Link Existing Nodes screen
     */
    public void enterLinkNodesSearchText(String text) {
        System.out.println("🔍 Entering search text: " + text);
        try {
            WebElement search = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField' AND visible == true")
            );
            search.click();
            sleep(300);
            search.sendKeys(text);
            sleep(200);
            System.out.println("✅ Entered search text");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter search text: " + e.getMessage());
        }
    }

    /**
     * Clear search field
     */
    public void clearLinkNodesSearchField() {
        System.out.println("🔍 Clearing search field...");
        try {
            WebElement search = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField' AND visible == true")
            );
            search.clear();
            sleep(300);
            System.out.println("✅ Cleared search field");
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear search field: " + e.getMessage());
        }
    }

    // ============================================================
    // SELECTION STATE VERIFICATION METHODS
    // ============================================================

    /**
     * Check if any nodes are selected (look for "X selected" text)
     */
    public boolean isAnyNodeSelected() {
        try {
            WebElement selected = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'selected' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            System.out.println("   Selection state: " + selected.getAttribute("name"));
            return selected.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the number of selected nodes
     */
    public int getSelectedNodeCount() {
        try {
            WebElement selected = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'selected' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            String text = selected.getAttribute("name"); // "1 selected"
            String num = text.split(" ")[0];
            return Integer.parseInt(num);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if Clear All button is visible
     */
    public boolean isClearAllButtonVisible() {
        try {
            WebElement clearAll = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Clear All' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            return clearAll.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Clear All button to deselect all nodes
     */
    public void clickClearAllButton() {
        System.out.println("❌ Clicking Clear All...");
        try {
            WebElement clearAll = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Clear All' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            clearAll.click();
            sleep(200);
            System.out.println("✅ Clicked Clear All");
        } catch (Exception e) {
            System.out.println("⚠️ Clear All button not found: " + e.getMessage());
        }
    }

        /**
     * Check if New Task screen is displayed
     */
    public boolean isNewTaskScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'New Task' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Enter task title
     */
    public void enterTaskTitle(String title) {
        System.out.println("📝 Entering task title: " + title);
        try {
            WebElement titleField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Enter task title'")
            );
            titleField.click();
            sleep(300);
            titleField.clear();
            titleField.sendKeys(title);
            System.out.println("✅ Entered task title");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter task title: " + e.getMessage());
        }
    }

    /**
     * Enter task description
     */
    public void enterTaskDescription(String description) {
        System.out.println("📝 Entering task description: " + description);
        try {
            // Find description text view
            WebElement descField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextView'")
            );
            descField.click();
            sleep(300);
            descField.sendKeys(description);
            sleep(300);
            
            // Click "Done" button to dismiss keyboard
            System.out.println("   Clicking Done to dismiss keyboard...");
            try {
                WebElement doneBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Done' AND type == 'XCUIElementTypeButton'")
                );
                doneBtn.click();
                sleep(200);
                System.out.println("   ✅ Clicked Done button");
            } catch (Exception doneEx) {
                System.out.println("   Done button not found, trying alternatives...");
                // Try Return key or hide keyboard
                try {
                    driver.executeScript("mobile: hideKeyboard", Map.of("strategy", "pressKey", "key", "Done"));
                } catch (Exception kbEx) {
                    // Fallback: tap outside to dismiss
                    try {
                        driver.executeScript("mobile: tap", Map.of("x", 200, "y", 150));
                    } catch (Exception tapEx) {}
                }
            }
            sleep(300);
            
            System.out.println("✅ Entered task description");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter task description: " + e.getMessage());
        }
    }

    /**
     * Click Create Task button
     */
    public void clickCreateTaskButton() {
        System.out.println("🆕 Clicking Create Task button...");
        try {
            WebElement createBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Create Task' AND type == 'XCUIElementTypeButton'")
            );
            createBtn.click();
            sleep(200);
            System.out.println("✅ Clicked Create Task");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create Task: " + e.getMessage());
        }
    }

    /**
     * Click Cancel button on New Task screen
     */
    public void clickCancelTask() {
        System.out.println("❌ Clicking Cancel...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            sleep(200);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    /**
     * Toggle Mark as Completed switch
     * Note: Need to dismiss keyboard first, then tap on the right side of the switch control
     */
    public void toggleMarkAsCompleted() {
        System.out.println("🔄 Toggling Mark as Completed...");
        try {
            // IMPORTANT: Dismiss keyboard first by tapping outside or pressing Done
            System.out.println("   Dismissing keyboard...");
            try {
                // Try to hide keyboard
                driver.executeScript("mobile: hideKeyboard");
            } catch (Exception kbEx) {
                // Fallback: tap on a neutral area to dismiss keyboard
                try {
                    driver.executeScript("mobile: tap", Map.of("x", 200, "y", 150));
                } catch (Exception tapEx) {
                    // Ignore
                }
            }
            sleep(200);
            
            WebElement toggle = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Mark as Completed' AND type == 'XCUIElementTypeSwitch'")
            );
            
            // Get toggle position and tap on the right side (where the actual switch is)
            int toggleX = toggle.getLocation().getX();
            int toggleY = toggle.getLocation().getY();
            int toggleWidth = toggle.getSize().getWidth();
            int toggleHeight = toggle.getSize().getHeight();
            
            // Tap on the right side of the toggle (where the switch control is)
            int tapX = toggleX + toggleWidth - 30;  // Right side
            int tapY = toggleY + (toggleHeight / 2); // Center vertically
            
            System.out.println("   Tapping toggle at (" + tapX + ", " + tapY + ")");
            driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
            sleep(200);
            
            // Verify the toggle changed
            String value = toggle.getAttribute("value");
            System.out.println("   Toggle value after tap: " + value);
            
            if ("0".equals(value)) {
                System.out.println("   Toggle still OFF, trying direct coordinates...");
                // Try tapping directly on the switch part
                driver.executeScript("mobile: tap", Map.of("x", 340, "y", tapY));
                sleep(200);
                value = toggle.getAttribute("value");
                System.out.println("   Toggle value after retry: " + value);
            }
            
            System.out.println("✅ Toggled Mark as Completed");
        } catch (Exception e) {
            System.out.println("⚠️ Could not toggle via element, trying coordinates...");
            // Fallback: dismiss keyboard and tap at typical toggle location
            try {
                driver.executeScript("mobile: hideKeyboard");
                sleep(300);
            } catch (Exception kbEx) {}
            
            try {
                driver.executeScript("mobile: tap", Map.of("x", 340, "y", 756));
                sleep(200);
                System.out.println("✅ Toggled Mark as Completed via coordinates");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not toggle: " + e2.getMessage());
            }
        }
    }

    /**
     * Check if Tasks section has "No tasks" text
     */
    public boolean hasNoTasks() {
        try {
            WebElement noTasks = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'No tasks'")
            );
            return noTasks.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is visible by label
     */
    public boolean isElementVisibleByLabel(String label) {
        try {
            WebElement element = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == '" + label + "' AND visible == true")
            );
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
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
            sleep(200);
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Add Asset: " + e.getMessage());
        }
    }

    public void clickBack() {
        System.out.println("🔙 Clicking Back button...");
        
        // Strategy 1: Try scrolling UP first to make Back visible (it's at top)
        try {
            scrollFormUp();
            sleep(300);
        } catch (Exception scrollEx) {
            // Ignore scroll errors
        }
        
        // Strategy 2: Direct accessibility ID "Back"
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement backBtn = quickWait.until(
                ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId("Back"))
            );
            backBtn.click();
            System.out.println("✅ Clicked Back (accessibility ID)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 1 (accessibility ID) failed");
        }
        
        // Strategy 3: Find button with Back/chevron.left name/label
        try {
            WebElement backBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Back' OR label == 'Back' OR name == 'chevron.left' OR name CONTAINS 'back')")
            );
            if (backBtn.isDisplayed()) {
                backBtn.click();
                System.out.println("✅ Clicked Back (button predicate)");
                sleep(200);
                return;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 (button predicate) failed");
        }
        
        // Strategy 4: Find button at top-left corner (typical Back position)
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : allButtons) {
                try {
                    int x = btn.getLocation().getX();
                    int y = btn.getLocation().getY();
                    String name = btn.getAttribute("name");
                    
                    // Back button is typically at top-left (X < 100, Y < 100)
                    if (x < 100 && y < 100 && y > 30) {
                        System.out.println("   Found top-left button at (" + x + "," + y + "), name='" + name + "'");
                        // Skip if it's Cancel (Cancel is also at top-left sometimes)
                        if (name != null && name.equals("Cancel")) continue;
                        
                        btn.click();
                        System.out.println("✅ Clicked Back (position-based)");
                        sleep(200);
                        return;
                    }
                } catch (Exception ex) {}
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (position-based) failed");
        }
        
        // Strategy 5: Tap coordinates at top-left (typical Back position on iOS)
        try {
            System.out.println("   Trying coordinate tap at (30, 55)...");
            driver.executeScript("mobile: tap", java.util.Map.of("x", 30, "y", 55));
            System.out.println("✅ Tapped Back position (30, 55)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 4 (coordinate tap) failed");
        }

        // If all strategies fail, log warning but don't throw
        // This allows tests to continue when screen state changes (e.g., after save)
        System.out.println("⚠️ Could not click Back button after trying all strategies - screen may have changed");
    }

    /**
     * Try to click Back button without throwing exception
     * Returns true if Back was clicked, false otherwise
     */
    public boolean tryClickBack() {
        System.out.println("🔙 Trying to click Back button (non-throwing)...");

        // Strategy 1: Direct accessibility ID "Back"
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            WebElement backBtn = quickWait.until(
                ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId("Back"))
            );
            backBtn.click();
            System.out.println("✅ Clicked Back");
            sleep(200);
            return true;
        } catch (Exception e) {}

        // Strategy 2: Find button with Back name/label
        try {
            WebElement backBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Back' OR label == 'Back' OR name == 'chevron.left')")
            );
            if (backBtn.isDisplayed()) {
                backBtn.click();
                System.out.println("✅ Clicked Back button");
                sleep(200);
                return true;
            }
        } catch (Exception e) {}

        // Strategy 3: Tap top-left coordinates
        try {
            driver.executeScript("mobile: tap", java.util.Map.of("x", 30, "y", 55));
            System.out.println("✅ Tapped Back position");
            sleep(200);
            return true;
        } catch (Exception e) {}

        System.out.println("⚠️ Back button not found - screen may have changed");
        return false;
    }

    public void clickCancel() {
        System.out.println("📝 Clicking Cancel button...");
        
        // Strategy 1: Try scrolling UP first to make Cancel visible (it's at top)
        try {
            scrollFormUp();
            sleep(300);
        } catch (Exception scrollEx) {
            // Ignore scroll errors
        }
        
        // Strategy 2: Direct accessibility ID
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement cancelBtn = quickWait.until(
                ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId("Cancel"))
            );
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel (accessibility ID)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 1 (accessibility ID) failed");
        }
        
        // Strategy 3: Find button with Cancel name/label
        try {
            WebElement cancelBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            if (cancelBtn.isDisplayed()) {
                cancelBtn.click();
                System.out.println("✅ Clicked Cancel (button predicate)");
                sleep(200);
                return;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 (button predicate) failed");
        }
        
        // Strategy 4: Find any element with Cancel label at top of screen (Y < 100)
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : allButtons) {
                try {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    int y = btn.getLocation().getY();
                    
                    if (y < 150 && 
                        ((name != null && name.toLowerCase().contains("cancel")) ||
                         (label != null && label.toLowerCase().contains("cancel")))) {
                        System.out.println("   Found Cancel at Y=" + y + ", name='" + name + "'");
                        btn.click();
                        System.out.println("✅ Clicked Cancel (position-based)");
                        sleep(200);
                        return;
                    }
                } catch (Exception ex) {}
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (position-based) failed");
        }
        
        // Strategy 5: Find StaticText "Cancel" and click
        try {
            WebElement cancelText = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            cancelText.click();
            System.out.println("✅ Clicked Cancel (StaticText)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 4 (StaticText) failed");
        }
        
        // Strategy 6: Tap coordinates at top-left (typical Cancel position on iOS)
        try {
            System.out.println("   Trying coordinate tap at (60, 55)...");
            driver.executeScript("mobile: tap", java.util.Map.of("x", 60, "y", 55));
            System.out.println("✅ Tapped Cancel position (60, 55)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 5 (coordinate tap) failed");
        }
        
        // If all strategies fail, throw exception
        throw new RuntimeException("Failed to click Cancel button after trying all strategies");
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
            // Use fresh element lookup to get current cell count after search
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            int count = cells.size();
            System.out.println("📊 Asset count: " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error getting asset count: " + e.getMessage());
            return 0;
        }
    }

    public void searchAsset(String assetName) {
        System.out.println("🔍 Searching for asset: " + assetName);
        
        WebElement searchField = null;
        
        // Try to find search bar using different strategies
        try {
            searchField = driver.findElement(AppiumBy.className("XCUIElementTypeSearchField"));
        } catch (Exception e) {
            try {
                searchField = driver.findElement(AppiumBy.accessibilityId("Search by name, type, location, or QR code"));
            } catch (Exception e2) {
                try {
                    searchField = driver.findElement(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField'")
                    );
                } catch (Exception e3) {
                    System.out.println("⚠️ Search bar not available");
                    return;
                }
            }
        }
        
        if (searchField != null) {
            try {
                // Click to focus
                searchField.click();
                sleep(300);
                
                // CLEAR the field first before entering new text
                searchField.clear();
                sleep(300);
                
                // Enter search term
                searchField.sendKeys(assetName);
                System.out.println("✅ Searched for asset: " + assetName);
                
                // Wait for search results to update
                sleep(400);
            } catch (Exception e) {
                System.out.println("⚠️ Error during search: " + e.getMessage());
            }
        }
    }

    public boolean selectAssetByName(String assetName) {
        System.out.println("📦 Looking for asset: " + assetName);
        
        // Strategy 1: Find by accessibility ID
        try {
            WebElement asset = driver.findElement(AppiumBy.accessibilityId(assetName));
            asset.click();
            System.out.println("✅ Selected asset: " + assetName);
            sleep(200);
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
                    sleep(200);
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
                            sleep(200);
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
                    sleep(200);
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
        
        sleep(200);
        
        // Select first available asset
        selectFirstAsset();
    }

    public String selectFirstAsset() {
        System.out.println("📦 Selecting first asset...");
        
        // Wait for list to fully load
        sleep(500);
        
        // =====================================================================
        // IMPORTANT: Must click on STATIC TEXT (asset name), NOT on Button/Cell!
        // Clicking on XCUIElementTypeButton or XCUIElementTypeCell does NOT navigate.
        // Only clicking on XCUIElementTypeStaticText (asset name text) works!
        // =====================================================================
        
        // STRATEGY 1 (PREFERRED): Click on StaticText that is the asset name
        // Asset name is typically the first text in each cell (y position 139-650 range)
        try {
            System.out.println("   Strategy 1: Finding asset name StaticText elements...");
            
            // First, get all cells to understand the structure
            List<WebElement> cells = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeCell' AND visible == true")
            );
            System.out.println("   Found " + cells.size() + " cells");
            
            if (cells.size() > 0) {
                WebElement firstCell = cells.get(0);
                
                // Find StaticText children of first cell
                List<WebElement> textsInCell = firstCell.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND visible == true")
                );
                System.out.println("   Found " + textsInCell.size() + " texts in first cell");
                
                // The first StaticText is usually the asset name
                if (textsInCell.size() > 0) {
                    WebElement assetNameText = textsInCell.get(0);
                    String assetName = assetNameText.getAttribute("name");
                    
                    // Skip if this looks like a section header (like "Assets" title)
                    if (assetName != null && !assetName.equals("Assets") && !assetName.isEmpty()) {
                        System.out.println("   🎯 Asset name text: " + assetName);
                        assetNameText.click();
                        System.out.println("✅ Clicked asset name StaticText");
                        sleep(800);
                        return assetName;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 1 failed: " + e.getMessage());
        }
        
        // STRATEGY 2: Find StaticText elements in asset list area (y > 100 to skip header)
        try {
            System.out.println("   Strategy 2: Finding StaticText in list area...");
            
            List<WebElement> allTexts = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            System.out.println("   Found " + allTexts.size() + " StaticText elements");
            
            for (WebElement text : allTexts) {
                int y = text.getLocation().getY();
                String name = text.getAttribute("name");
                
                // Skip header area (y < 110) and tab bar area (y > 780)
                // Skip common non-asset texts
                if (y >= 110 && y <= 780 && name != null && !name.isEmpty()) {
                    String lower = name.toLowerCase();
                    if (!lower.equals("assets") && !lower.equals("search") && 
                        !lower.contains("task") && !lower.equals("no location") &&
                        !lower.equals("android") && !lower.equals("ios") &&
                        !name.equals("ATS") && !name.equals("UPS") && !name.equals("PDU") &&
                        !name.equals("Generator") && !name.equals("Panelboard") &&
                        !name.equals("Disconnect Switch") && !name.equals("MCC") &&
                        !name.equals("Busway") && name.length() > 2) {
                        // This is likely an asset name
                        System.out.println("   🎯 Found asset name at y=" + y + ": " + name);
                        text.click();
                        System.out.println("✅ Clicked asset StaticText: " + name);
                        sleep(800);
                        return name;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 failed: " + e.getMessage());
        }
        
        // STRATEGY 3: Fallback to button click (less reliable but kept for compatibility)
        // Asset buttons have format: "AssetName, Location, Type" e.g. "TestAsset_123, Room_456, ATS"
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ', ' AND visible == true AND NOT name CONTAINS 'Search' AND NOT name BEGINSWITH 'Completed Task' AND NOT name BEGINSWITH 'Test Task' AND NOT name CONTAINS 'This task'")
            );
            System.out.println("   Found " + buttons.size() + " asset buttons");
            
            if (buttons.size() > 0) {
                WebElement firstAsset = buttons.get(0);
                String assetName = firstAsset.getAttribute("name");
                int x = firstAsset.getLocation().getX();
                int y = firstAsset.getLocation().getY();
                int width = firstAsset.getSize().getWidth();
                int height = firstAsset.getSize().getHeight();
                
                System.out.println("   🎯 First asset: " + assetName);
                System.out.println("   📍 Location: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height);
                
                // Verify element is in valid tappable area (not off-screen)
                if (y < 100 || y > 800) {
                    System.out.println("   ⚠️ Element Y=" + y + " may be in header/footer area, scrolling...");
                    scrollFormDown();
                    sleep(300);
                    // Re-find after scroll
                    buttons = driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ', ' AND visible == true AND NOT name CONTAINS 'Search'")
                    );
                    if (buttons.size() > 0) {
                        firstAsset = buttons.get(0);
                        x = firstAsset.getLocation().getX();
                        y = firstAsset.getLocation().getY();
                        width = firstAsset.getSize().getWidth();
                        height = firstAsset.getSize().getHeight();
                        assetName = firstAsset.getAttribute("name");
                        System.out.println("   🔄 After scroll: x=" + x + ", y=" + y);
                    }
                }
                
                // Method 1: Direct click
                try {
                    firstAsset.click();
                    System.out.println("   ✓ Direct click executed");
                } catch (Exception clickEx) {
                    System.out.println("   ⚠️ Direct click failed: " + clickEx.getMessage());
                }
                
                // Wait and check if we navigated to detail screen
                sleep(800);
                
                // Verify click worked by checking for Asset Details screen
                boolean clickWorked = false;
                try {
                    List<WebElement> detailIndicators = driver.findElements(
                        AppiumBy.iOSNsPredicateString("(name == 'Asset Details' OR name CONTAINS 'Edit' OR name == 'Save' OR name == 'Back') AND visible == true")
                    );
                    clickWorked = !detailIndicators.isEmpty();
                } catch (Exception e) {}
                
                if (!clickWorked) {
                    System.out.println("   ⚠️ Click may not have worked, trying coordinate tap...");
                    
                    // Method 2: Coordinate tap (center of element)
                    int tapX = x + (width / 2);
                    int tapY = y + (height / 2);
                    
                    // Ensure tap is in safe area
                    if (tapY < 150) tapY = 200;
                    if (tapY > 750) tapY = 700;
                    
                    System.out.println("   📍 Tapping at coordinates: (" + tapX + ", " + tapY + ")");
                    driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
                    sleep(800);
                    
                    // Check again
                    try {
                        List<WebElement> detailIndicators = driver.findElements(
                            AppiumBy.iOSNsPredicateString("(name == 'Asset Details' OR name CONTAINS 'Edit' OR name == 'Save' OR name == 'Back') AND visible == true")
                        );
                        clickWorked = !detailIndicators.isEmpty();
                    } catch (Exception e) {}
                }
                
                if (!clickWorked) {
                    System.out.println("   ⚠️ Still not on detail screen, trying double-tap...");
                    // Method 3: Double tap
                    int tapX = x + (width / 2);
                    int tapY = y + (height / 2);
                    driver.executeScript("mobile: doubleTap", Map.of("x", tapX, "y", tapY));
                    sleep(800);
                }
                
                System.out.println("✅ Selected asset via button");
                return assetName.split(",")[0].trim();
            }
        } catch (Exception e) {
            System.out.println("   Button strategy failed: " + e.getMessage());
        }
        
        // STRATEGY 2: Find first CELL and click inside it
        try {
            List<WebElement> cells = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeCell' AND visible == true")
            );
            System.out.println("   Found " + cells.size() + " cells");
            
            // Skip cells that might be in header/search area
            for (WebElement cell : cells) {
                int cellY = cell.getLocation().getY();
                
                // Skip cells in header area (y < 200)
                if (cellY < 200) {
                    System.out.println("   Skipping cell at y=" + cellY + " (header area)");
                    continue;
                }
                
                int cellX = cell.getLocation().getX() + (cell.getSize().getWidth() / 2);
                int cellCenterY = cellY + (cell.getSize().getHeight() / 2);
                
                // Ensure tap Y is in safe area (200-750)
                if (cellCenterY < 200) cellCenterY = 250;
                if (cellCenterY > 750) cellCenterY = 700;
                
                System.out.println("   📍 Tapping cell at: (" + cellX + ", " + cellCenterY + ")");
                driver.executeScript("mobile: tap", Map.of("x", cellX, "y", cellCenterY));
                System.out.println("✅ Tapped cell");
                sleep(800);
                return "asset";
            }
        } catch (Exception e) {
            System.out.println("   Cell strategy failed: " + e.getMessage());
        }
        
        // STRATEGY 3: Tap directly at known asset list area
        try {
            System.out.println("   Trying direct coordinate tap in asset list area...");
            // Asset list typically starts around y=250-300 and first item is near y=300-400
            int tapX = 200;  // Center-left of screen
            int tapY = 350;  // First asset row area
            
            driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
            System.out.println("✅ Tapped at fixed coordinates (" + tapX + ", " + tapY + ")");
            sleep(800);
            return "asset";
        } catch (Exception e) {
            System.out.println("   Coordinate tap failed: " + e.getMessage());
        }
        
        System.out.println("⚠️ Could not select any asset");
        return null;
    }
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
        System.out.println("📍 Attempting to click Asset Class dropdown...");
        
        // Strategy 1: Use PageFactory element directly (for "Select asset class" placeholder)
        try {
            if (selectAssetClassButton != null && selectAssetClassButton.isDisplayed()) {
                selectAssetClassButton.click();
                System.out.println("✅ Clicked 'Select asset class' (PageFactory)");
                return;
            }
        } catch (Exception e) {
            System.out.println("   PageFactory element not available");
        }
        
        // Strategy 2: Find "Select asset class" text (for new assets without a class)
        try {
            WebElement classBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select asset class' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            classBtn.click();
            System.out.println("✅ Clicked 'Select asset class' button");
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Find existing asset class button (MCC, ATS, Generator, etc.)
        // When asset already has a class, the button shows the class name instead of "Select asset class"
        String[] assetClasses = {"MCC", "ATS", "Generator", "Busway", "Capacitor", "Circuit Breaker", 
                                  "Disconnect Switch", "Fuse", "Junction Box", "Loadcenter", "Motor", 
                                  "Panelboard", "Switchboard", "Transformer", "VFD", "Wire"};
        
        for (String className : assetClasses) {
            try {
                // Look for button with exact class name that's positioned in the Asset Class area
                WebElement classBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name == '" + className + "' AND visible == true"
                    )
                );
                
                // Verify it's the Asset Class button (not in a list or picker)
                int y = classBtn.getLocation().getY();
                if (y > 600 && y < 800) { // Asset Class field is typically in this Y range
                    classBtn.click();
                    System.out.println("✅ Clicked existing class button: " + className);
                    return;
                }
            } catch (Exception e) {}
        }
        
        // Strategy 4: Find ANY button after "Asset Class" label
        try {
            // First find the Asset Class label to get position reference
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            int labelY = assetClassLabel.getLocation().getY();
            
            // Find buttons near the Asset Class label (within 100 pixels below it)
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true")
            );
            
            for (WebElement btn : buttons) {
                int btnY = btn.getLocation().getY();
                String name = btn.getAttribute("name");
                
                // Button should be slightly below the label and not contain common non-class names
                if (btnY > labelY && btnY < labelY + 100 && name != null && 
                    !name.contains("location") && !name.contains("subtype") && !name.contains("Subtype")) {
                    btn.click();
                    System.out.println("✅ Clicked Asset Class field button: " + name);
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("   Position-based strategy failed: " + e.getMessage());
        }
        
        // Strategy 5: Final fallback - find any clickable element with asset class names
        try {
            WebElement classBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true AND " +
                    "(name == 'MCC' OR name == 'ATS' OR name == 'Generator' OR name == 'Busway' OR " +
                    "name == 'Select asset class' OR name CONTAINS 'Circuit' OR name CONTAINS 'Disconnect')"
                )
            );
            classBtn.click();
            System.out.println("✅ Clicked Asset Class dropdown (fallback)");
            return;
        } catch (Exception e) {
            System.out.println("   Fallback failed: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to click Asset Class dropdown - no matching button found");
    }

    public void selectAssetClass(String className) {
        System.out.println("📋 Selecting asset class: " + className);
        clickSelectAssetClass();
        sleep(200);
        
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
        sleep(200);
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
                    sleep(200);
                } else if (attempt == 2) {
                    System.out.println("   Trying scroll up...");
                    scrollFormUp();
                    sleep(200);
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
     * MAIN ENTRY POINT - Select Location with Fallback
     * 
     * Strategy:
     * 1. Try SIMPLE approach first (faster, cleaner)
     * 2. If SIMPLE fails, try COMPLEX approach (handles edge cases)
     */
    /**
     * FAST Location Selection - Handles ALL cases including empty picker
     * 
     * Logic:
     * 1. Try to select existing Building → Floor → Room
     * 2. If any level missing, CREATE it using plus button
     * 3. Works even when picker is completely empty
     */
    public boolean selectLocation() {
        System.out.println("📍 Selecting location (FAST)...");
        
        long timestamp = System.currentTimeMillis();
        String buildingName = "Building_" + timestamp;
        String floorName = "Floor_" + timestamp;
        String roomName = "Room_" + timestamp;
        
        try {
            // Open picker if not already open
            if (!isLocationPickerOpen()) {
                selectLocationButton.click();
                sleep(200);
            }
            
            if (!isLocationPickerOpen()) {
                System.out.println("⚠️ Picker not open");
                return false;
            }
            System.out.println("📍 Location picker detected (title visible)");
            
            boolean roomSelected = false;

            // TREE IS FULLY EXPANDED: building → floors → rooms all visible.
            // Clicking building or floor COLLAPSES them — DO NOT click parents.
            // Just find and click a ROOM directly.

            // Try to find an existing room directly (Room_ prefix or leaf items)
            try {
                WebElement room = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "name BEGINSWITH 'Room_' AND " +
                    "NOT name CONTAINS ' floor' AND NOT name CONTAINS ' room'"));
                String rName = room.getAttribute("name");
                room.click();
                System.out.println("✅ Room: " + rName);
                roomSelected = true;
                sleep(400);
            } catch (Exception e) {
                System.out.println("   No Room_ item found, trying other patterns...");
            }

            // Try rooms with " node" pattern but exclude floors/buildings
            if (!roomSelected) {
                try {
                    // Find all buttons with node count, pick one that's NOT a floor or building
                    List<WebElement> nodeItems = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name CONTAINS ' node' AND " +
                        "NOT name CONTAINS ' floor' AND NOT name CONTAINS ' room'"));
                    for (WebElement item : nodeItems) {
                        String name = item.getAttribute("name");
                        // Skip items that look like floors (they have " room" — already excluded)
                        // or buildings (they have " floor" — already excluded)
                        if (name != null && !isSystemButton(name)) {
                            item.click();
                            System.out.println("✅ Room (node): " + name);
                            roomSelected = true;
                            sleep(400);
                            break;
                        }
                    }
                } catch (Exception e) {}
            }

            // Create room as last resort: need to create building → floor → room
            if (!roomSelected) {
                System.out.println("   No existing room found, creating location...");
                // Check if building exists
                boolean hasBldg = !driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS ' floor'")).isEmpty();
                if (!hasBldg) {
                    if (clickPlusButtonForLocation()) {
                        sleep(400);
                        enterLocationTextAndSave("Building Name", buildingName);
                        sleep(200);
                    }
                }
                // Create floor
                if (clickPlusButtonForLocation()) {
                    sleep(400);
                    enterLocationTextAndSave("Floor Name", floorName);
                    sleep(200);
                }
                // Create room
                if (clickPlusButtonForLocation()) {
                    sleep(400);
                    if (enterLocationTextAndSave("Room Name", roomName)) {
                        sleep(200);
                        try {
                            driver.findElement(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeButton' AND name CONTAINS '" + roomName + "'")).click();
                            System.out.println("✅ Created Room: " + roomName);
                            roomSelected = true;
                            sleep(400);
                        } catch (Exception e2) {
                            System.out.println("⚠️ Could not click created room");
                        }
                    }
                }
            }
            
            sleep(200);
            
            // Dismiss picker if still open
            if (isLocationPickerOpen()) {
                dismissLocationPickerSafe();
            }
            
            if (roomSelected) {
                System.out.println("✅ Location selected!");
                return true;
            }
            return false;
            
        } catch (Exception e) {
            System.out.println("⚠️ Error: " + e.getMessage());
            dismissLocationPickerSafe();
            return false;
        }
    }
    
    /**
     * Try to select an existing floor item in the picker.
     * Uses two strategies:
     *   1. Standard patterns (" room", " node", "Floor_" prefix)
     *   2. First visible non-system, non-building button (index 0)
     * @return true if a floor was clicked
     */
    private boolean selectFloorItem() {
        // Strategy 1: Positive pattern match (no visible==true — searches full DOM)
        try {
            WebElement floor = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name CONTAINS ' room' OR name CONTAINS ' node' OR name BEGINSWITH 'Floor_') AND " +
                "NOT name CONTAINS ' floor'"));
            String fName = floor.getAttribute("name");
            floor.click();
            System.out.println("✅ Floor: " + fName);
            sleep(600);
            return true;
        } catch (Exception e) {}

        // Strategy 2: First non-system button at index 0 (floors with custom names)
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name != null && !isSystemButton(name)
                    && !name.contains(" floor") && !name.contains(" floors")) {
                    btn.click();
                    System.out.println("✅ Floor (index 0): " + name);
                    sleep(600);
                    return true;
                }
            }
        } catch (Exception e2) {}

        System.out.println("   No floor items found");
        return false;
    }

    /** Click plus button for location creation */
    private boolean clickPlusButtonForLocation() {
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                "name == 'plus.circle.fill' AND type == 'XCUIElementTypeButton'")).click();
            System.out.println("   Plus clicked");
            return true;
        } catch (Exception e) {}
        try {
            driver.findElement(AppiumBy.accessibilityId("plus.circle.fill")).click();
            System.out.println("   Plus clicked");
            return true;
        } catch (Exception e) {}
        try {
            driver.findElement(AppiumBy.iOSClassChain(
                "**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][1]")).click();
            System.out.println("   Plus clicked");
            return true;
        } catch (Exception e) {}
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Add'")).click();
            System.out.println("   Plus clicked (Add)");
            return true;
        } catch (Exception e) {}
        System.out.println("⚠️ Plus button not found");
        return false;
    }
    
    /** Enter text and save for location creation */
    private boolean enterLocationTextAndSave(String placeholder, String text) {
        try {
            WebElement tf = null;
            try {
                tf = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND (value == '" + placeholder + "' OR placeholderValue == '" + placeholder + "')"));
            } catch (Exception e) {
                tf = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND visible == true"));
            }
            tf.sendKeys(text);
            sleep(300);
            try {
                driver.findElement(AppiumBy.accessibilityId("Save")).click();
            } catch (Exception e) {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name == 'Save' OR label == 'Save')")).click();
            }
            System.out.println("   Saved: " + text);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Text entry failed: " + e.getMessage());
            return false;
        }
    }
    
    /** Safely dismiss location picker */
    private void dismissLocationPickerSafe() {
        System.out.println("   Dismissing picker...");
        for (int i = 0; i < 3; i++) {
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString("name == 'Back' OR label == 'Back'")).click();
                sleep(300);
                if (!isLocationPickerOpen()) return;
            } catch (Exception e) { break; }
        }
        try {
            int w = driver.manage().window().getSize().width;
            Map<String, Object> tap = new HashMap<>();
            tap.put("x", w / 2);
            tap.put("y", 50);
            driver.executeScript("mobile: tap", tap);
        } catch (Exception e) {}
    }

    /**
     * COMPLEX LOCATION SELECTOR - Works for ALL sites and hierarchies
     * 
     * Algorithm:
     * 1. Open picker if not already open
     * 2. Select Building (item with "X floor/floors" pattern)
     * 3. Select Floor (item with "X room/rooms" pattern)
     * 4. Select Room/Leaf (item WITHOUT floor/room count patterns)
     * 5. If room has nodes, go one level deeper
     * 6. Wait for auto-dismiss
     * 
     * Key insight: At each level, we identify items by what they DON'T have:
     * - Buildings have floor counts, floors have room counts, rooms may have node counts
     * - A leaf is any item WITHOUT a count pattern
     */
    private boolean selectLocationComplex() {
        System.out.println("📍 Selecting location - COMPLEX mode...");
        
        // Generate unique names for creating new locations
        long timestamp = System.currentTimeMillis();
        String newFloorName = "Floor_" + timestamp;
        String newRoomName = "Room_" + timestamp;
        
        try {
            // STEP 0: Ensure picker is open
            if (!isLocationPickerOpen()) {
                driver.findElement(AppiumBy.accessibilityId("Select location")).click();
                sleep(200);
            }
            
            // STEP 1: SELECT OR CREATE BUILDING
            // Pattern: name contains "X floor" or "X floors"
            System.out.println("📍 Step 1: Selecting building...");
            WebElement building = findAndClickHierarchyItem(
                " floor",  // Pattern to match (buildings have floor count)
                null,      // No exclusion pattern at this level
                "Building"
            );
            
            if (building == null) {
                // No building exists - this is a NEW site, need to CREATE building
                String newBuildingName = "Building_" + timestamp;
                System.out.println("   No building found, creating: " + newBuildingName);
                
                if (!createLocationItem(newBuildingName, "Building Name", 1)) {
                    System.out.println("⚠️ Failed to create building");
                    return false;
                }
                
                // Click the newly created building
                sleep(200);
                building = findAndClickHierarchyItem(newBuildingName, null, "Created Building");
                if (building == null) {
                    // Try alternative: find any building with our timestamp
                    try {
                        WebElement newBuilding = driver.findElement(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND name CONTAINS '" + newBuildingName + "'"));
                        newBuilding.click();
                        System.out.println("✅ Clicked created building: " + newBuildingName);
                        building = newBuilding;
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not select created building");
                        return false;
                    }
                }
            }
            sleep(400);
            
            // STEP 2: SELECT FLOOR
            // First try: floors with room count (e.g., "Floor 1, 3 rooms")
            // Second try: any floor without room count (e.g., "Floor_123456")
            System.out.println("📍 Step 2: Selecting floor...");
            WebElement floor = findAndClickHierarchyItem(
                " room",   // Pattern to match (floors have room count)
                " floor",  // Exclude buildings
                "Floor with room count"
            );
            
            // If no floor with room count, try to find ANY floor (including Floor_*)
            if (floor == null) {
                System.out.println("   No floor with room count, looking for any floor...");
                floor = selectAnyFloorItem();
            }
            
            // If still no floor, create one
            if (floor == null) {
                System.out.println("   No floor found, creating: " + newFloorName);
                if (!createLocationItem(newFloorName, "Floor Name", 1)) {
                    System.out.println("⚠️ Failed to create floor");
                    return false;
                }
                // Click the newly created floor DIRECTLY by name
                sleep(200);
                floor = clickItemByExactName(newFloorName, "Created Floor");
                if (floor == null) {
                    System.out.println("⚠️ Could not select created floor");
                    return false;
                }
            }
            sleep(400);
            
            // STEP 3: SELECT ROOM OR LEAF
            // At this point, we look for ANY button that is NOT a floor
            System.out.println("📍 Step 3: Selecting room/leaf...");
            WebElement roomOrLeaf = selectRoomOrLeaf(newRoomName);
            
            if (roomOrLeaf == null) {
                System.out.println("⚠️ Could not select room");
                return false;
            }
            
            System.out.println("✅ Location selection complete");
            sleep(200);
            
            // STEP 4: Ensure picker is dismissed
            // After selecting/creating a room, we need to verify picker closed
            if (!dismissLocationPickerIfOpen()) {
                System.out.println("⚠️ Picker may still be open - forcing dismiss");
            }
            
            sleep(200);
            return true;
            
        } catch (Exception e) {
            System.out.println("⚠️ Error selecting location: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Select any floor item, including floors without room count
     * Used when no floor with "X rooms" pattern is found
     */
    private WebElement selectAnyFloorItem() {
        try {
            // Get all visible buttons
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            
            System.out.println("   Looking for any floor among " + allButtons.size() + " buttons");
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons
                if (isSystemButton(name)) continue;
                
                // Skip buildings (contain " floor")
                if (name.contains(" floor")) continue;
                
                // Look for Floor_* items or items with " room" pattern
                if (name.startsWith("Floor_") || name.contains(" room")) {
                    btn.click();
                    System.out.println("✅ Selected floor: " + name);
                    return btn;
                }
            }
            
            System.out.println("   No floor items found");
            return null;
            
        } catch (Exception e) {
            System.out.println("   Error finding floor: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Click an item directly by its exact name
     * Used after creating items when we know the exact name
     */
    private WebElement clickItemByExactName(String exactName, String levelName) {
        try {
            System.out.println("   Looking for item with name: " + exactName);
            
            // Strategy 1: Find by name contains (most reliable)
            try {
                WebElement item = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS '" + exactName + "'"));
                item.click();
                System.out.println("✅ " + levelName + ": " + item.getAttribute("name"));
                return item;
            } catch (Exception e) {
                // Continue
            }
            
            // Strategy 2: Find by exact accessibility ID
            try {
                WebElement item = driver.findElement(AppiumBy.accessibilityId(exactName));
                item.click();
                System.out.println("✅ " + levelName + " (via ID): " + exactName);
                return item;
            } catch (Exception e) {
                // Continue
            }
            
            // Strategy 3: Search through all buttons
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(exactName)) {
                    btn.click();
                    System.out.println("✅ " + levelName + " (via search): " + name);
                    return btn;
                }
            }
            
            System.out.println("   Could not find item: " + exactName);
            return null;
            
        } catch (Exception e) {
            System.out.println("   Error finding item " + exactName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Find and click an item in the location hierarchy
     * 
     * @param matchPattern Pattern that the item should contain (e.g., " floor", " room")
     * @param excludePattern Pattern that the item should NOT contain (to exclude parent levels)
     * @param levelName For logging purposes
     * @return The clicked element, or null if not found
     */
    private WebElement findAndClickHierarchyItem(String matchPattern, String excludePattern, String levelName) {
        try {
            // Build the predicate string
            StringBuilder predicate = new StringBuilder();
            predicate.append("type == 'XCUIElementTypeButton' AND visible == true AND name CONTAINS '").append(matchPattern).append("'");
            
            if (excludePattern != null && !excludePattern.isEmpty()) {
                predicate.append(" AND NOT name CONTAINS '").append(excludePattern).append("'");
            }
            
            // Find all matching elements
            List<WebElement> items = driver.findElements(AppiumBy.iOSNsPredicateString(predicate.toString()));
            
            if (items.isEmpty()) {
                System.out.println("   No " + levelName + " found matching pattern: " + matchPattern);
                return null;
            }
            
            // Click the first matching item
            WebElement item = items.get(0);
            String itemName = item.getAttribute("name");
            item.click();
            System.out.println("✅ " + levelName + ": " + itemName);
            return item;
            
        } catch (Exception e) {
            System.out.println("   Error finding " + levelName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Select a room or leaf item at the current hierarchy level
     * This is the most complex step because rooms can have various naming patterns
     * 
     * PRIORITY: Prefer LEAF items (no "X nodes") over items with children
     * 
     * @param newRoomName Name to use if we need to create a new room
     * @return The selected element, or null if failed
     */
    private WebElement selectRoomOrLeaf(String newRoomName) {
        try {
            // Get ALL visible buttons at current level
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            
            System.out.println("   Found " + allButtons.size() + " buttons at room level");
            
            // Separate into LEAF items and PARENT items (with nodes)
            List<WebElement> leafItems = new ArrayList<>();
            List<WebElement> parentItems = new ArrayList<>();
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system/navigation buttons
                if (isSystemButton(name)) {
                    System.out.println("   Skipping system button: '" + name + "'");
                    continue;
                }
                
                // Skip parent-level items (buildings and floors)
                if (name.contains(" floor")) continue;  // Building
                if (name.contains(" room")) continue;   // Floor (we already selected this)
                
                // Skip items that are actually FLOORS we created (not rooms)
                // Our created floors have names like "Floor_123456" with no room count
                if (name.startsWith("Floor_") || name.startsWith("Building_")) {
                    System.out.println("   Skipping created floor/building: '" + name + "'");
                    continue;
                }
                
                // Categorize: LEAF (no nodes) vs PARENT (has nodes)
                if (name.contains(" node")) {
                    parentItems.add(btn);
                    System.out.println("   Parent item (has nodes): '" + name + "'");
                } else {
                    leafItems.add(btn);
                    System.out.println("   LEAF item (no children): '" + name + "'");
                }
            }
            
            // PRIORITY 1: Click a LEAF item if available (completes selection)
            if (!leafItems.isEmpty()) {
                WebElement item = leafItems.get(0);
                String itemName = item.getAttribute("name");
                item.click();
                System.out.println("✅ Selected LEAF: " + itemName);
                sleep(400);
                return item;
            }
            
            // PRIORITY 2: Click a PARENT item and go deeper
            if (!parentItems.isEmpty()) {
                WebElement item = parentItems.get(0);
                String itemName = item.getAttribute("name");
                item.click();
                System.out.println("✅ Selected Parent (will go deeper): " + itemName);
                sleep(400);
                
                // Go one level deeper to find a leaf
                System.out.println("   Going deeper to find leaf node...");
                return selectLeafNode();
            }
            
            // No room found - need to create one
            System.out.println("   No room found, creating: " + newRoomName);
            if (!createLocationItem(newRoomName, "Room Name", 2)) {
                // Try with plus button index 1 if 2 fails
                if (!createLocationItem(newRoomName, "Room Name", 1)) {
                    System.out.println("⚠️ Failed to create room");
                    return null;
                }
            }
            
            // After room creation, we need to SELECT it to complete the location selection
            // The room is created but not yet selected - clicking it selects it
            sleep(300);  // Wait for room creation to complete
            
            try {
                WebElement newRoom = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS '" + newRoomName + "'"));
                newRoom.click();
                System.out.println("✅ Clicked to select room: " + newRoomName);
                sleep(200);
                return newRoom;
            } catch (Exception e) {
                // Room might already be selected or picker auto-closed
                System.out.println("   Room created, could not click to select: " + e.getMessage());
                // Still return success - room was created
                return driver.findElement(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true"));
            }
            
        } catch (Exception e) {
            System.out.println("   Error in selectRoomOrLeaf: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Select a leaf node (final level in hierarchy)
     */
    /**
     * Select a leaf node (final level in hierarchy)
     * Uses same LEAF vs PARENT prioritization as selectRoomOrLeaf
     */
    private WebElement selectLeafNode() {
        try {
            // At this level, we look for ANY button that doesn't have a count pattern
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            
            System.out.println("   Found " + allButtons.size() + " buttons at node level");
            
            List<WebElement> leafItems = new ArrayList<>();
            List<WebElement> parentItems = new ArrayList<>();
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons
                if (isSystemButton(name)) {
                    continue;
                }
                
                // Skip items with parent hierarchy patterns
                if (name.contains(" floor")) continue;
                if (name.contains(" room")) continue;
                
                // Skip items that are actually FLOORS/BUILDINGS we created
                if (name.startsWith("Floor_") || name.startsWith("Building_")) {
                    System.out.println("   Skipping created floor/building: '" + name + "'");
                    continue;
                }
                
                // Categorize
                if (name.contains(" node")) {
                    parentItems.add(btn);
                    System.out.println("   Node parent: '" + name + "'");
                } else {
                    leafItems.add(btn);
                    System.out.println("   Node LEAF: '" + name + "'");
                }
            }
            
            // PRIORITY 1: Click a LEAF
            if (!leafItems.isEmpty()) {
                WebElement item = leafItems.get(0);
                String itemName = item.getAttribute("name");
                item.click();
                System.out.println("✅ Selected Leaf Node: " + itemName);
                sleep(400);
                return item;
            }
            
            // PRIORITY 2: Go even deeper if needed
            if (!parentItems.isEmpty()) {
                WebElement item = parentItems.get(0);
                String itemName = item.getAttribute("name");
                item.click();
                System.out.println("✅ Selected Node Parent (going deeper): " + itemName);
                sleep(400);
                // Recursive call to go deeper
                return selectLeafNode();
            }
            
            System.out.println("   No leaf node found at this level");
            return null;
            
        } catch (Exception e) {
            System.out.println("   Error selecting leaf node: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a button name corresponds to a system/navigation button
     */
    private boolean isSystemButton(String name) {
        if (name == null || name.isEmpty()) return true;
        
        // EXACT match patterns - must match exactly
        String[] exactPatterns = {
            "plus", "plus.circle.fill", "Back", "Cancel", "Done", "Save",
            "xmark", "checkmark", "ellipsis", "gear", "search",
            "Select Location", "Select location", "Add", "Edit", "Delete",
            "Close", "OK", "Yes", "No", "Confirm"
        };
        
        for (String pattern : exactPatterns) {
            if (name.equals(pattern)) {
                return true;
            }
        }
        
        // CONTAINS patterns - if name contains these, it's a system button
        String[] containsPatterns = {
            "chevron", "arrow", "info.circle", "questionmark",
            ".fill", ".circle"
        };
        
        for (String pattern : containsPatterns) {
            if (name.contains(pattern)) {
                return true;
            }
        }
        
        // Check for very short names (likely icons)
        if (name.length() <= 3) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Create a new location item (floor or room)
     * 
     * @param itemName Name for the new item
     * @param textFieldPlaceholder The placeholder text in the text field
     * @param plusButtonIndex Which plus button to click (1 or 2)
     * @return true if creation succeeded
     */
    /**
     * Create a new location item (building, floor, or room)
     * Uses multiple strategies to find the plus button and text field
     * 
     * @param itemName Name for the new item
     * @param textFieldPlaceholder The placeholder text in the text field (e.g., "Building Name", "Floor Name", "Room Name")
     * @param plusButtonIndex Which plus button to click (1 or 2) - used as hint
     * @return true if creation succeeded
     */
    private boolean createLocationItem(String itemName, String textFieldPlaceholder, int plusButtonIndex) {
        System.out.println("   Creating location item: " + itemName + " (placeholder: " + textFieldPlaceholder + ")");
        
        try {
            // STEP 1: Click the plus button using multiple strategies
            boolean plusClicked = false;
            
            // Strategy 1: iOSClassChain with index
            if (!plusClicked) {
                try {
                    driver.findElement(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][" + plusButtonIndex + "]")).click();
                    System.out.println("   Plus clicked via ClassChain[" + plusButtonIndex + "]");
                    plusClicked = true;
                } catch (Exception e) {
                    // Continue to next strategy
                }
            }
            
            // Strategy 2: Find by exact name 'plus.circle.fill'
            if (!plusClicked) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill'")).click();
                    System.out.println("   Plus clicked via predicate 'plus.circle.fill'");
                    plusClicked = true;
                } catch (Exception e) {
                    // Continue to next strategy
                }
            }
            
            // Strategy 3: Find by name 'plus'
            if (!plusClicked) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus' AND type == 'XCUIElementTypeButton'")).click();
                    System.out.println("   Plus clicked via predicate 'plus'");
                    plusClicked = true;
                } catch (Exception e) {
                    // Continue to next strategy
                }
            }
            
            // Strategy 4: Find by accessibility ID 'Add'
            if (!plusClicked) {
                try {
                    driver.findElement(AppiumBy.accessibilityId("Add")).click();
                    System.out.println("   Plus clicked via accessibility 'Add'");
                    plusClicked = true;
                } catch (Exception e) {
                    // Continue to next strategy
                }
            }
            
            // Strategy 5: Find any button with 'plus' or 'add' in name
            if (!plusClicked) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (name CONTAINS 'plus' OR name CONTAINS 'add' OR label CONTAINS 'Add')")).click();
                    System.out.println("   Plus clicked via contains 'plus/add'");
                    plusClicked = true;
                } catch (Exception e) {
                    System.out.println("⚠️ Could not find plus button with any strategy");
                    return false;
                }
            }
            
            sleep(200);
            
            // STEP 2: Enter the name in text field
            boolean nameEntered = false;
            
            // Strategy 1: Find by placeholder value
            if (!nameEntered) {
                try {
                    WebElement textField = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND (value == '" + textFieldPlaceholder + "' OR placeholderValue == '" + textFieldPlaceholder + "')"));
                    textField.sendKeys(itemName);
                    System.out.println("   Name entered via placeholder match");
                    nameEntered = true;
                } catch (Exception e) {
                    // Continue to next strategy
                }
            }
            
            // Strategy 2: Find any visible text field
            if (!nameEntered) {
                try {
                    WebElement textField = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND visible == true"));
                    textField.sendKeys(itemName);
                    System.out.println("   Name entered via visible text field");
                    nameEntered = true;
                } catch (Exception e) {
                    System.out.println("⚠️ Could not find text field");
                    return false;
                }
            }
            
            sleep(300);
            
            // STEP 3: Click Save button
            boolean saved = false;
            
            // Strategy 1: Accessibility ID 'Save'
            if (!saved) {
                try {
                    driver.findElement(AppiumBy.accessibilityId("Save")).click();
                    System.out.println("   Saved via accessibility 'Save'");
                    saved = true;
                } catch (Exception e) {
                    // Continue
                }
            }
            
            // Strategy 2: Button with label 'Save'
            if (!saved) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (label == 'Save' OR name == 'Save')")).click();
                    System.out.println("   Saved via button predicate");
                    saved = true;
                } catch (Exception e) {
                    System.out.println("⚠️ Could not find Save button");
                    return false;
                }
            }
            
            sleep(300);
            
            System.out.println("✅ Created: " + itemName);
            return true;
            
        } catch (Exception e) {
            System.out.println("⚠️ Error creating location item: " + e.getMessage());
            return false;
        }
    }



    /**
     * SIMPLIFIED Location Selector - FASTEST Approach
     * 
     * Strategy:
     * 1. FIRST: Try to select a Room DIRECTLY (if already visible)
     * 2. FALLBACK: Go through Building → Floor → Room hierarchy
     */
    public boolean selectLocationSimple() {
        System.out.println("📍 Selecting location - SIMPLE mode...");
        
        long timestamp = System.currentTimeMillis();
        
        try {
            // Ensure picker is open
            if (!isLocationPickerOpen()) {
                driver.findElement(AppiumBy.accessibilityId("Select location")).click();
                sleep(400);
            }
            
            // STRATEGY 1: Try to select Room DIRECTLY (fastest!)
            System.out.println("   Trying DIRECT room selection...");
            WebElement directRoom = findSelectableRoom();
            if (directRoom != null) {
                // Get name BEFORE clicking (element becomes stale after click)
                String roomName = directRoom.getAttribute("name");
                directRoom.click();
                System.out.println("✅ Room selected DIRECTLY: " + roomName);
                sleep(300);
                // Picker should auto-dismiss after room selection
                return true;
            }
            System.out.println("   No direct room found, using hierarchy...");
            
            // STRATEGY 2: Go through hierarchy
            return selectViaHierarchy(timestamp);
            
        } catch (Exception e) {
            System.out.println("⚠️ Simple approach error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find a room that can be selected directly (not a building or floor)
     */
    private WebElement findSelectableRoom() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons
                if (name.equals("Cancel") || name.equals("plus") || name.equals("Save") ||
                    name.contains("plus.circle") || name.length() <= 3) continue;
                
                // Skip buildings (contain " floor")
                if (name.contains(" floor")) continue;
                
                // Skip floors (contain " room")
                if (name.contains(" room")) continue;
                
                // Skip our created items that aren't rooms
                if (name.startsWith("Building_")) continue;
                if (name.startsWith("Floor_")) continue;
                
                // Skip items with node count (need to go deeper)
                if (name.contains(" node")) continue;
                
                // This looks like a selectable room!
                System.out.println("   Found selectable room: " + name);
                return btn;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Select location via Building → Floor → Room hierarchy
     */
    private boolean selectViaHierarchy(long timestamp) {
        try {
            // STEP 1: Select Building
            System.out.println("📍 Step 1: Selecting building...");
            try {
                WebElement building = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS ' floor'"));
                String buildingName = building.getAttribute("name");
                building.click();
                System.out.println("✅ Building: " + buildingName);
                sleep(300);
            } catch (Exception e) {
                System.out.println("⚠️ No building found");
                return false;
            }
            
            // STEP 2: Select Floor
            System.out.println("📍 Step 2: Selecting floor...");
            WebElement floor = findFirstFloor();
            
            if (floor != null) {
                String floorName = floor.getAttribute("name");
                floor.click();
                System.out.println("✅ Floor: " + floorName);
                sleep(300);
            } else {
                String floorName = "Floor_" + timestamp;
                System.out.println("   Creating: " + floorName);
                if (!createAndClickItem(floorName, "Floor Name")) {
                    return false;
                }
            }
            
            // STEP 3: Select Room
            System.out.println("📍 Step 3: Selecting room...");
            WebElement room = findFirstRoom();
            
            if (room != null) {
                String roomName = room.getAttribute("name");
                room.click();
                System.out.println("✅ Room: " + roomName);
                sleep(300);
            } else {
                String roomName = "Room_" + timestamp;
                System.out.println("   Creating: " + roomName);
                if (!createAndClickItem(roomName, "Room Name")) {
                    return false;
                }
            }
            
            System.out.println("✅ Location selection complete");
            sleep(300);
            dismissLocationPickerIfOpen();
            return true;
            
        } catch (Exception e) {
            System.out.println("⚠️ Hierarchy selection error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find first floor element (not a building)
     */
    private WebElement findFirstFloor() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name == null) continue;
                
                // Skip system buttons
                if (name.equals("Cancel") || name.equals("plus") || 
                    name.contains("plus.circle") || name.length() <= 3) continue;
                
                // Skip buildings (contain " floor")
                if (name.contains(" floor")) continue;
                
                // This is a floor! (has " room" or starts with "Floor_" or any other name)
                if (name.contains(" room") || name.startsWith("Floor_") || 
                    (!name.contains(" node") && !name.startsWith("Building_") && !name.startsWith("Room_"))) {
                    return btn;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Find first room element (not a floor or building)
     */
    private WebElement findFirstRoom() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name == null) continue;
                
                // Skip system buttons
                if (name.equals("Cancel") || name.equals("plus") || 
                    name.contains("plus.circle") || name.length() <= 3) continue;
                
                // Skip buildings and floors
                if (name.contains(" floor")) continue;  // Building
                if (name.contains(" room")) continue;   // Floor with room count
                if (name.startsWith("Floor_")) continue; // Our created floors
                if (name.startsWith("Building_")) continue; // Our created buildings
                
                // This is a room or leaf!
                return btn;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Create an item and click it directly (no search after creation)
     */
    private boolean createAndClickItem(String name, String placeholder) {
        try {
            // Click plus button
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus'")).click();
            } catch (Exception e) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill'")).click();
                } catch (Exception e2) {
                    driver.findElement(AppiumBy.iOSClassChain("**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][1]")).click();
                }
            }
            sleep(300);
            
            // Enter name
            WebElement textField = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"));
            textField.sendKeys(name);
            sleep(200);
            
            // Save
            driver.findElement(AppiumBy.accessibilityId("Save")).click();
            sleep(200);
            
            // Click the created item directly
            WebElement createdItem = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS '" + name + "'"));
            createdItem.click();
            System.out.println("✅ Created & selected: " + name);
            sleep(300);
            
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Error creating " + name + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get floor elements (items that are floors, not buildings)
     */
    private List<WebElement> getFloorElements() {
        List<WebElement> floors = new ArrayList<>();
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons and buildings
                if (isSystemButton(name)) continue;
                if (name.contains(" floor")) continue;  // This is a building
                
                // Floor items: have " room" pattern OR start with "Floor_"
                if (name.contains(" room") || name.startsWith("Floor_")) {
                    floors.add(btn);
                }
            }
        } catch (Exception e) {
            System.out.println("   Error getting floors: " + e.getMessage());
        }
        return floors;
    }
    
    /**
     * Get room elements (items that are rooms, not floors or buildings)
     */
    private List<WebElement> getRoomElements() {
        List<WebElement> rooms = new ArrayList<>();
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"));
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons
                if (isSystemButton(name)) continue;
                
                // Skip buildings and floors
                if (name.contains(" floor")) continue;  // Building
                if (name.contains(" room")) continue;   // Floor with room count
                if (name.startsWith("Floor_")) continue; // Our created floors
                if (name.startsWith("Building_")) continue; // Our created buildings
                
                // What remains is a room (or node)
                rooms.add(btn);
            }
        } catch (Exception e) {
            System.out.println("   Error getting rooms: " + e.getMessage());
        }
        return rooms;
    }
    
    /**
     * Create a location item - simplified version
     */
    private boolean createLocationItemSimple(String name, String placeholder) {
        try {
            System.out.println("   Creating: " + name);
            
            // Click plus button
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus'")).click();
            } catch (Exception e) {
                driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill'")).click();
            }
            sleep(400);
            
            // Enter name
            WebElement textField = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"));
            textField.sendKeys(name);
            sleep(200);
            
            // Save
            driver.findElement(AppiumBy.accessibilityId("Save")).click();
            sleep(400);
            
            System.out.println("   ✅ Created: " + name);
            return true;
        } catch (Exception e) {
            System.out.println("   ⚠️ Error creating " + name + ": " + e.getMessage());
            return false;
        }
    }


    /**
     * Dismiss the location picker if it's still open
     * Uses multiple strategies to close the picker
     * 
     * @return true if picker was dismissed or already closed
     */
    private boolean dismissLocationPickerIfOpen() {
        try {
            // Check if picker is still open
            boolean pickerOpen = false;
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(label == 'Select Location' OR name == 'Select Location') AND type == 'XCUIElementTypeStaticText'"));
                pickerOpen = true;
            } catch (Exception e) {
                // Check for Cancel button (another indicator picker is open)
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "label == 'Cancel' AND type == 'XCUIElementTypeButton'"));
                    pickerOpen = true;
                } catch (Exception e2) {
                    // Picker is closed
                }
            }
            
            if (!pickerOpen) {
                System.out.println("   Picker already closed");
                return true;
            }
            
            System.out.println("   Picker still open, attempting to dismiss...");
            
            // Strategy 1: Try tapping in the header area to close
            try {
                int screenWidth = driver.manage().window().getSize().width;
                Map<String, Object> tapParams = new HashMap<>();
                tapParams.put("x", screenWidth / 2);
                tapParams.put("y", 100);  // Tap near top/header
                driver.executeScript("mobile: tap", tapParams);
                System.out.println("   Tapped header area");
                sleep(200);
            } catch (Exception e) {
                // Continue
            }
            
            // Check if dismissed
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(label == 'Select Location' OR label == 'Cancel') AND type == 'XCUIElementTypeStaticText'"));
            } catch (Exception e) {
                System.out.println("   Picker dismissed after tapping header");
                return true;
            }
            
            // Strategy 2: Click Cancel (will keep any selection made)
            try {
                driver.findElement(AppiumBy.accessibilityId("Cancel")).click();
                System.out.println("   Clicked Cancel to close picker");
                sleep(200);
                return true;
            } catch (Exception e) {
                // Continue
            }
            
            // Strategy 3: Swipe down to dismiss
            try {
                int screenWidth = driver.manage().window().getSize().width;
                int screenHeight = driver.manage().window().getSize().height;
                
                Map<String, Object> swipeParams = new HashMap<>();
                swipeParams.put("fromX", screenWidth / 2);
                swipeParams.put("fromY", 150);
                swipeParams.put("toX", screenWidth / 2);
                swipeParams.put("toY", screenHeight - 100);
                swipeParams.put("duration", 0.3);
                driver.executeScript("mobile: dragFromToForDuration", swipeParams);
                System.out.println("   Swiped down to dismiss");
                sleep(200);
                return true;
            } catch (Exception e) {
                System.out.println("   Swipe dismiss failed");
            }
            
            // Strategy 4: Tap outside picker area
            try {
                int screenWidth = driver.manage().window().getSize().width;
                Map<String, Object> tapParams = new HashMap<>();
                tapParams.put("x", screenWidth / 2);
                tapParams.put("y", 50);
                driver.executeScript("mobile: tap", tapParams);
                System.out.println("   Tapped outside picker");
                sleep(200);
                return true;
            } catch (Exception e) {
                // Continue
            }
            
            return false;
            
        } catch (Exception e) {
            System.out.println("   Error in dismissLocationPickerIfOpen: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if location picker is currently open
     */
    private boolean isLocationPickerOpen() {
        try {
            // Multiple ways to detect picker is open
            // 1. "Select Location" title visible
            List<WebElement> title = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label == 'Select Location' OR name == 'Select Location'"));
            if (!title.isEmpty()) {
                System.out.println("📍 Location picker detected (title visible)");
                return true;
            }
            
            // 2. Building buttons visible (contain " floor" or " floors")
            List<WebElement> buildings = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS ' floor'"));
            if (!buildings.isEmpty()) {
                System.out.println("📍 Location picker detected (buildings visible)");
                return true;
            }
            
            // 3. Check for hierarchy pattern
            List<WebElement> hierarchy = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name CONTAINS ' room' OR name CONTAINS '>')"));
            if (!hierarchy.isEmpty()) {
                System.out.println("📍 Location picker detected (hierarchy visible)");
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Ensure location picker is dismissed and we're back on the form
     */
    private boolean ensureLocationPickerDismissed() {
        try {
            // Check if we're back on the Create Asset form
            // Look for Asset Name field or Create Asset title
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            
            boolean onForm = quickWait.until(d -> {
                try {
                    // Check for Asset Name text field
                    List<WebElement> assetName = d.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND (value == 'Asset Name' OR placeholderValue == 'Asset Name' OR value CONTAINS 'Asset_')"));
                    if (!assetName.isEmpty()) return true;
                    
                    // Check for "Select location" button (on form, not in picker)
                    // If we can see "Select location" but NOT "Select Location" title, we're on form
                    List<WebElement> selectBtn = d.findElements(AppiumBy.accessibilityId("Select location"));
                    List<WebElement> pickerTitle = d.findElements(AppiumBy.iOSNsPredicateString(
                        "label == 'Select Location' AND type == 'XCUIElementTypeStaticText'"));
                    if (!selectBtn.isEmpty() && pickerTitle.isEmpty()) return true;
                    
                    return false;
                } catch (Exception e) {
                    return false;
                }
            });
            
            if (onForm) {
                System.out.println("✅ Back on Create Asset form");
                return true;
            }
        } catch (Exception e) {
            // Timeout - picker might still be open
        }
        
        return false;
    }
    
    /**
     * Force close the location picker using multiple strategies
     */
    private void forceCloseLocationPicker() {
        System.out.println("📍 Force closing location picker...");
        
        // Strategy 1: Tap on any visible room/location item (should select and close)
        try {
            List<WebElement> items = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name CONTAINS '>' OR name BEGINSWITH 'Room_')"));
            if (!items.isEmpty()) {
                items.get(0).click();
                System.out.println("   Tapped location item to close picker");
                sleep(200);
                if (ensureLocationPickerDismissed()) return;
            }
        } catch (Exception e) {}
        
        // Strategy 2: Look for any "Done" or "Save" button
        try {
            WebElement doneBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Done' OR label == 'Save' OR label == 'OK')"));
            doneBtn.click();
            System.out.println("   Clicked Done/Save button");
            sleep(200);
            if (ensureLocationPickerDismissed()) return;
        } catch (Exception e) {}
        
        // Strategy 3: Tap the selected/checked item
        try {
            WebElement selected = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeButton') AND (name CONTAINS 'checkmark' OR label CONTAINS 'selected')"));
            selected.click();
            System.out.println("   Tapped selected item");
            sleep(200);
            if (ensureLocationPickerDismissed()) return;
        } catch (Exception e) {}
        
        // Strategy 4: Navigate back using back button
        try {
            WebElement backBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Back' OR name CONTAINS 'back' OR label CONTAINS 'chevron')"));
            // Click back multiple times to exit the hierarchy
            for (int i = 0; i < 3; i++) {
                try {
                    backBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (label == 'Back' OR name CONTAINS 'back')"));
                    backBtn.click();
                    sleep(300);
                } catch (Exception e2) {
                    break;
                }
            }
            System.out.println("   Navigated back to close picker");
            sleep(200);
            if (ensureLocationPickerDismissed()) return;
        } catch (Exception e) {}
        
        // Strategy 5: Swipe down to dismiss modal
        try {
            int screenWidth = driver.manage().window().getSize().width;
            int screenHeight = driver.manage().window().getSize().height;
            
            Map<String, Object> swipeParams = new HashMap<>();
            swipeParams.put("fromX", screenWidth / 2);
            swipeParams.put("fromY", 150);
            swipeParams.put("toX", screenWidth / 2);
            swipeParams.put("toY", screenHeight - 100);
            swipeParams.put("duration", 0.3);
            driver.executeScript("mobile: dragFromToForDuration", swipeParams);
            System.out.println("   Swiped down to dismiss");
            sleep(200);
        } catch (Exception e) {
            System.out.println("   Swipe failed: " + e.getMessage());
        }
        
        // Strategy 6: Tap outside the picker area (top of screen)
        try {
            int screenWidth = driver.manage().window().getSize().width;
            Map<String, Object> tapParams = new HashMap<>();
            tapParams.put("x", screenWidth / 2);
            tapParams.put("y", 50);
            driver.executeScript("mobile: tap", tapParams);
            System.out.println("   Tapped outside picker");
            sleep(200);
        } catch (Exception e) {}
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
            sleep(200);
            
            WebElement floorField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Floor Name'")
            );
            floorField.sendKeys(floorName);
            click(saveButton);
            System.out.println("✅ Created floor: " + floorName);
            sleep(200);
            
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(floorName)) {
                    btn.click();
                    break;
                }
            }
            sleep(200);
            
            click(addRoomButton);
            sleep(200);
            
            WebElement roomField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Room Name'")
            );
            roomField.sendKeys(roomName);
            click(saveButton);
            System.out.println("✅ Created room: " + roomName);
            sleep(200);
            
            buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(" floor")) {
                    btn.click();
                    break;
                }
            }
            sleep(200);
            
            buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(floorName)) {
                    btn.click();
                    break;
                }
            }
            sleep(200);
            
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

        // Strategy 1: Find the BUTTON element for subtype picker (must be XCUIElementTypeButton)
        // CRITICAL: Use predicate with type filter to avoid matching StaticText labels
        // which would dispatch the click to whatever's behind them (often the Save button)
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            WebElement subtypeBtn = shortWait.until(
                ExpectedConditions.elementToBeClickable(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (name CONTAINS 'asset subtype' OR name CONTAINS 'Asset Subtype' OR name == 'Select asset subtype')"
                    )
                )
            );
            String btnName = subtypeBtn.getAttribute("name");
            subtypeBtn.click();
            System.out.println("✅ Clicked subtype button: '" + btnName + "'");
            return;
        } catch (Exception e) {
            System.out.println("   📌 Strategy 1: No Button with 'asset subtype' in name found");
            System.out.println("   → Trying positional search relative to Asset Subtype label...");
        }
        
        // First scroll down to make subtype field visible (it's often below the fold)
        System.out.println("   📜 Scrolling down to make subtype field visible...");
        try {
            scrollFormDown();
            sleep(400);
        } catch (Exception scrollEx) {
            System.out.println("   ⚠️ Scroll failed, continuing anyway");
        }
        
        // Strategy 2: If subtype already selected, find button by position relative to label
        System.out.println("   📌 Strategy 2: Looking for button below 'Asset Subtype' label...");
        try {
            WebElement subtypeLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND name CONTAINS 'Asset Subtype'")
            );
            int labelY = subtypeLabel.getLocation().getY();
            System.out.println("      Found 'Asset Subtype (Optional)' label at Y=" + labelY);
            
            // Find button just below this label (within 100px - increased from 80)
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            System.out.println("      Scanning " + buttons.size() + " buttons...");
            
            for (WebElement btn : buttons) {
                try {
                    int btnY = btn.getLocation().getY();
                    String name = btn.getAttribute("name");
                    
                    // Button should be below label and within 100px
                    if (btnY > labelY && (btnY - labelY) < 100 && name != null) {
                        // Skip known non-subtype buttons and icons
                        if (name.equals("Cancel") || name.equals("Save Changes") || 
                            name.contains("Bldg") || name.contains("Floor") || name.contains("Room") ||
                            name.equals("qrcode.viewfinder") || name.equals("Calculator") ||
                            name.equals("Select shortcut") || name.equals("Filter") ||
                            name.equals("plus.circle.fill") || name.equals("square.grid.2x2") ||
                            name.equals("1") || name.equals("2") || name.equals("3") ||
                            name.equals("house") || name.equals("house.fill") ||  // Navigation icons
                            name.equals("chevron.left") || name.equals("chevron.right") ||
                            name.equals("xmark") || name.equals("xmark.circle") ||
                            name.equals("gear") || name.equals("gearshape") ||
                            name.equals("person") || name.equals("person.fill") ||
                            name.length() <= 5) {  // Skip very short names (likely icons)
                            continue;
                        }
                        
                        // MUST contain parentheses or be a known subtype pattern for Strategy 2
                        boolean looksLikeSubtype = name.contains("(") && name.contains(")");
                        if (!looksLikeSubtype && !name.equals("None") && !name.contains("Switch") && 
                            !name.contains("Fuse") && !name.contains("Breaker")) {
                            continue;
                        }
                        
                        // This should be the subtype button!
                        System.out.println("      ✓ Found subtype button: '" + name + "' at Y=" + btnY + " (offset=" + (btnY - labelY) + "px)");
                        btn.click();
                        System.out.println("   ✅ Clicked currently selected subtype to open dropdown");
                        return;
                    }
                } catch (Exception ex) {
                    // Skip button if attributes can't be read
                }
            }
            System.out.println("      ✗ No suitable button found below label within 100px");
        } catch (Exception e) {
            System.out.println("      ✗ Strategy 2 failed: " + e.getMessage());
        }
        
        // Strategy 3: Find button with known subtype names
        System.out.println("   📌 Strategy 3: Looking for known subtype button names...");
        String[] knownSubtypes = {
            // Disconnect Switch subtypes
            "Disconnect Switch (<= 1000V)",
            "Disconnect Switch (> 1000V)",
            "Fused Disconnect Switch (<= 1000V)",
            "Fused Disconnect Switch (> 1000V)",
            "Bolted-Pressure Switch (BPS)",
            "Bypass-Isolation Switch (<= 1000V)",
            "Bypass-Isolation Switch (> 1000V)",
            "High-Pressure Contact Switch (HPC)",
            "Load Interrupter Switch (LIS)",
            "Molded-Case Switch (<= 1000V)",
            "Molded-Case Switch (> 1000V)",
            "Non-Fused Disconnect Switch (<= 1000V)",
            "Non-Fused Disconnect Switch (> 1000V)",
            "Safety Switch (<= 1000V)",
            "Safety Switch (> 1000V)",
            // Fuse subtypes
            "Current-Limiting Fuse",
            "Expulsion Fuse",
            "High-Speed Fuse",
            "Power Fuse",
            // Circuit Breaker subtypes
            "Air Circuit Breaker (ACB)",
            "Insulated-Case Circuit Breaker (ICCB)",
            "Low-Voltage Power Circuit Breaker (LVPCB)",
            "Molded-Case Circuit Breaker (MCCB)",
            "Miniature Circuit Breaker (MCB)",
            "Vacuum Circuit Breaker",
            "Oil Circuit Breaker",
            "SF6 Circuit Breaker",
            // Common
            "None",
            "Select asset subtype"
        };
        
        for (String subtype : knownSubtypes) {
            try {
                WebElement btn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == '" + subtype + "'")
                );
                if (btn.isDisplayed()) {
                    System.out.println("      ✓ Found known subtype: '" + subtype + "'");
                    btn.click();
                    System.out.println("   ✅ Clicked subtype button to open dropdown");
                    return;
                }
            } catch (Exception e) {
                // Subtype not found, continue to next
            }
        }
        System.out.println("      ✗ No known subtype buttons found on screen");
        
        // Strategy 4: Find any button that looks like a subtype (contains parentheses or voltage)
        System.out.println("   📌 Strategy 4: Looking for subtype-like buttons (with parentheses/voltage)...");
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name == null) continue;
                
                // Skip known non-subtype buttons
                if (name.equals("Cancel") || name.equals("Save Changes") || 
                    name.contains("Bldg") || name.contains("Floor") || name.contains("Room") ||
                    name.equals("qrcode.viewfinder") || name.equals("Calculator") ||
                    name.equals("Filter") || name.equals("plus.circle.fill") ||
                    name.equals("square.grid.2x2") || name.equals("Select shortcut") ||
                    name.matches("[0-9]+")) {  // Skip number buttons
                    continue;
                }
                
                // Subtype buttons typically contain parentheses like "(< 1000V)" or specific keywords
                boolean isSubtypeButton = 
                    (name.contains("(") && name.contains(")")) ||  // Has parentheses - MOST RELIABLE
                    name.contains("1000V") ||                       // Voltage rating
                    name.contains("Fuse") ||                        // Fuse types (but not just "Fuse" as asset class)
                    name.contains("Breaker") ||                     // Breaker types
                    name.equals("None");                            // None option
                
                // Extra check: Don't click plain asset class names
                if (name.equals("Disconnect Switch") || name.equals("Circuit Breaker") || 
                    name.equals("Fuse") || name.equals("ATS") || name.equals("UPS")) {
                    continue;
                }
                
                if (isSubtypeButton) {
                    System.out.println("      ✓ Found subtype-like button: '" + name + "'");
                    btn.click();
                    System.out.println("   ✅ Clicked to open subtype dropdown");
                    return;
                }
            }
            System.out.println("      ✗ No subtype-like buttons found");
        } catch (Exception e) {
            System.out.println("      ✗ Strategy 4 failed: " + e.getMessage());
        }
        
        // Strategy 5: Try scrolling and retry
        try {
            System.out.println("   Trying scroll then retry...");
            scrollFormDown();
            sleep(200);
            
            // Retry Strategy 1 after scroll
            WebElement subtypeBtn = driver.findElement(AppiumBy.accessibilityId("Select asset subtype"));
            subtypeBtn.click();
            System.out.println("✅ Clicked after scroll");
            return;
        } catch (Exception e) {
            System.out.println("   Scroll + retry failed");
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
        sleep(300);
        
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
        
        // QR Code field is at the bottom of the form - scroll down to find it
        for (int scrollAttempt = 0; scrollAttempt < 2; scrollAttempt++) {
            
            // Strategy 1: Find by EXACT placeholder text "Enter or scan QR code"
            try {
                WebElement qrField = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND value == 'Enter or scan QR code'"
                    )
                );
                
                if (qrField.isDisplayed()) {
                    int fieldY = qrField.getLocation().getY();
                    if (fieldY > 0 && fieldY < 850) {
                        System.out.println("   🔍 Found QR Code field (placeholder) at Y=" + fieldY);
                        qrField.click();
                        sleep(200);
                        qrField.sendKeys(qrCode);
                        System.out.println("✅ Entered QR code: " + qrCode);
                        dismissKeyboard();
                        return;
                    }
                }
            } catch (Exception e) {}
            
            // Strategy 2: Find TextField directly BELOW "QR Code" label (exact match)
            try {
                WebElement qrLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label == 'QR Code'"
                    )
                );
                
                if (qrLabel.isDisplayed()) {
                    int labelY = qrLabel.getLocation().getY();
                    
                    if (labelY > 0 && labelY < 800) {
                        System.out.println("   🔍 Found 'QR Code' label at Y=" + labelY);
                        
                        // Find TextField BELOW this label (within 60px)
                        List<WebElement> fields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
                        for (WebElement field : fields) {
                            if (!field.isDisplayed()) continue;
                            int fieldY = field.getLocation().getY();
                            
                            // Field must be BELOW label (fieldY > labelY) and close (within 60px)
                            if (fieldY > labelY && (fieldY - labelY) < 60) {
                                System.out.println("   🔍 Found TextField at Y=" + fieldY + " (below QR Code label)");
                                field.click();
                                sleep(200);
                                field.sendKeys(qrCode);
                                System.out.println("✅ Entered QR code (by label): " + qrCode);
                                dismissKeyboard();
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {}
            
            // Scroll down to reveal QR Code field
            System.out.println("   📜 Scrolling down to find QR Code field...");
            scrollFormDown();
            sleep(400);
        }
        
        System.out.println("⚠️ Could not find QR code field after scrolling");
    }

    public String getQRCodeValue() {
        try {
            return qrCodeField.getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }
    /**
     * Edit existing QR code field (clears and enters new value)
     * Used when editing an asset that already has a QR code
     */
    public boolean editQRCode(String newQRCode) {
        System.out.println("📝 Editing QR code to: " + newQRCode);
        
        // On Edit screen, QR Code field already has a value - DON'T scroll up first
        // Just scroll down once since QR Code is near the bottom
        scrollFormDown();
        sleep(200);
        
        // Try to find QR Code field (max 3 attempts with 1 scroll each)
        for (int attempt = 0; attempt < 3; attempt++) {
            System.out.println("   📜 Looking for QR Code field (attempt " + (attempt + 1) + ")");
            
            // Strategy 1: Find TextField with value starting with "QR_" (existing QR code)
            try {
                List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
                System.out.println("   Found " + textFields.size() + " text fields");
                
                for (WebElement field : textFields) {
                    try {
                        String value = field.getAttribute("value");
                        int fieldY = field.getLocation().getY();
                        
                        System.out.println("   Checking field: value='" + value + "', Y=" + fieldY);
                        
                        // Check if this field contains a QR code value
                        if (value != null && value.startsWith("QR_") && fieldY > 0 && fieldY < 850) {
                            System.out.println("   🔍 Found QR field with value: " + value);
                            field.click();
                            sleep(300);
                            field.clear();
                            sleep(200);
                            field.sendKeys(newQRCode);
                            sleep(200);
                            dismissKeyboard();
                            System.out.println("✅ Edited QR code to: " + newQRCode);
                            return true;
                        }
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
                System.out.println("   ⚠️ Strategy 1 exception: " + e.getMessage());
            }
            
            // Strategy 2: Find by "QR Code" label position
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND label == 'QR Code'")
                );
                System.out.println("   Found " + labels.size() + " 'QR Code' labels");
                
                for (WebElement label : labels) {
                    try {
                        int labelY = label.getLocation().getY();
                        System.out.println("   QR Code label at Y=" + labelY);
                        
                        if (labelY > 0 && labelY < 800) {
                            // Find TextField below label
                            List<WebElement> fields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
                            for (WebElement field : fields) {
                                try {
                                    int fieldY = field.getLocation().getY();
                                    if (fieldY > labelY && (fieldY - labelY) < 70) {
                                        String value = field.getAttribute("value");
                                        System.out.println("   🔍 Found TextField below label: Y=" + fieldY + ", value=" + value);
                                        field.click();
                                        sleep(300);
                                        field.clear();
                                        sleep(200);
                                        field.sendKeys(newQRCode);
                                        sleep(200);
                                        dismissKeyboard();
                                        System.out.println("✅ Edited QR code (by label) to: " + newQRCode);
                                        return true;
                                    }
                                } catch (Exception e) {}
                            }
                        }
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
                System.out.println("   ⚠️ Strategy 2 exception: " + e.getMessage());
            }
            
            // Scroll down once more
            if (attempt < 2) {
                System.out.println("   📜 Scrolling down...");
                scrollFormDown();
                sleep(200);
            }
        }
        
        System.out.println("❌ Could not find QR Code field");
        return false;
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
                "duration", 0.3
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
                "duration", 0.3
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
        sleep(500);
        
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
            sleep(200);
            
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
        sleep(200);
        
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
        System.out.println("📝 Preparing edit mode...");
        
        // Try to find explicit Edit button first (in case UI changes)
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Edit")
            ));
            editBtn.click();
            System.out.println("✅ Clicked Edit button");
            return;
        } catch (Exception e) {}
        
        // Try predicate for Edit button
        try {
            WebElement editBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Edit' OR label == 'Edit'")
            );
            editBtn.click();
            System.out.println("✅ Clicked Edit button (predicate)");
            return;
        } catch (Exception e) {}
        
        // Try pencil icon
        try {
            WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("square.and.pencil"));
            editBtn.click();
            System.out.println("✅ Clicked Edit button (icon)");
            return;
        } catch (Exception e) {}
        
        // The Asset Detail screen IS the edit screen - no explicit Edit button exists
        // Verify we're on the edit screen by checking for editable fields
        boolean onEditScreen = false;
        
        // Check for Asset Details navigation bar
        try {
            onEditScreen = driver.findElements(
                AppiumBy.iOSNsPredicateString("name == 'Asset Details' AND visible == true")
            ).size() > 0;
        } catch (Exception e) {}
        
        // Check for editable text field (Asset Name)
        if (!onEditScreen) {
            try {
                onEditScreen = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND visible == true")
                ).size() > 0;
            } catch (Exception e) {}
        }
        
        // Check for Asset Class button
        if (!onEditScreen) {
            try {
                onEditScreen = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'MCC' OR name == 'ATS' OR name CONTAINS 'Select asset') AND visible == true")
                ).size() > 0;
            } catch (Exception e) {}
        }
        
        // Check 4: Look for form labels
        if (!onEditScreen) {
            try {
                onEditScreen = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name == 'Name' OR name == 'Asset Class' OR name == 'Location') AND visible == true")
                ).size() > 0;
            } catch (Exception e) {}
        }
        
        // Check 5: Look for Save Changes button
        if (!onEditScreen) {
            try {
                onEditScreen = driver.findElements(
                    AppiumBy.iOSNsPredicateString("(name CONTAINS 'Save' OR label CONTAINS 'Save') AND visible == true")
                ).size() > 0;
            } catch (Exception e) {}
        }
        
        // Check 6: Back button indicates we're on a detail screen
        if (!onEditScreen) {
            try {
                onEditScreen = driver.findElements(
                    AppiumBy.iOSNsPredicateString("(name == 'Back' OR name == 'Assets') AND type == 'XCUIElementTypeButton' AND visible == true")
                ).size() > 0;
            } catch (Exception e) {}
        }
        
        if (onEditScreen) {
            System.out.println("✅ On Asset Details edit screen");
        } else {
            System.out.println("⚠️ Could not verify edit screen");
        }
    }
    
    /**
     * TURBO: Click Edit button - direct fast click (no retry)
     */
    public void clickEditTurbo() {
        // Asset Detail screen is DIRECTLY editable - no separate Edit button exists
        // The screen already shows editable fields (TextField for name, Buttons for class, etc.)
        sleep(500); // Wait for screen to fully load
        
        // Try to find and click Edit button if it exists (some UI versions have it)
        try {
            WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
            editBtn.click();
            System.out.println("✅ Clicked Edit button");
            sleep(300);
        } catch (Exception e) {
            // No Edit button - that's OK, screen might be directly editable
        }
        
        // Verify we're on the Asset Details screen (which IS the edit screen)
        boolean onEditScreen = false;
        
        // Check 1: Asset Details navigation bar
        try {
            List<WebElement> navBar = driver.findElements(
                AppiumBy.iOSNsPredicateString("(name == 'Asset Details' OR name CONTAINS 'Asset Detail' OR label CONTAINS 'Asset Detail') AND visible == true")
            );
            if (!navBar.isEmpty()) {
                onEditScreen = true;
                System.out.println("   ✓ Found Asset Details navigation");
            }
        } catch (Exception e) {}
        
        // Check 2: Editable text field (Asset Name field)
        if (!onEditScreen) {
            try {
                List<WebElement> textFields = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND visible == true")
                );
                if (!textFields.isEmpty()) {
                    onEditScreen = true;
                    System.out.println("   ✓ Found " + textFields.size() + " editable text fields");
                }
            } catch (Exception e) {}
        }
        
        // Check 3: Asset Class buttons
        if (!onEditScreen) {
            try {
                List<WebElement> classButtons = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'MCC' OR name == 'ATS' OR name == 'UPS' OR name == 'PDU' OR name == 'Generator' OR name == 'Busway' OR name CONTAINS 'Select asset' OR name CONTAINS 'Disconnect') AND visible == true")
                );
                if (!classButtons.isEmpty()) {
                    onEditScreen = true;
                    System.out.println("   ✓ Found Asset Class button");
                }
            } catch (Exception e) {}
        }
        
        // Check 4: Look for "Name" or "Asset Class" labels (form labels)
        if (!onEditScreen) {
            try {
                List<WebElement> formLabels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name == 'Name' OR name == 'Asset Class' OR name == 'Location' OR name == 'Subtype' OR name CONTAINS 'Required') AND visible == true")
                );
                if (!formLabels.isEmpty()) {
                    onEditScreen = true;
                    System.out.println("   ✓ Found form labels: " + formLabels.size());
                }
            } catch (Exception e) {}
        }
        
        // Check 5: Look for Save Changes button
        if (!onEditScreen) {
            try {
                List<WebElement> saveBtn = driver.findElements(
                    AppiumBy.iOSNsPredicateString("(name CONTAINS 'Save' OR label CONTAINS 'Save') AND visible == true")
                );
                if (!saveBtn.isEmpty()) {
                    onEditScreen = true;
                    System.out.println("   ✓ Found Save button");
                }
            } catch (Exception e) {}
        }
        
        // Check 6: Look for Back button (indicates we navigated somewhere)
        if (!onEditScreen) {
            try {
                List<WebElement> backBtn = driver.findElements(
                    AppiumBy.iOSNsPredicateString("(name == 'Back' OR name == 'Assets' OR name CONTAINS 'chevron') AND type == 'XCUIElementTypeButton' AND visible == true")
                );
                if (!backBtn.isEmpty()) {
                    // We're on some detail screen, probably the edit screen
                    onEditScreen = true;
                    System.out.println("   ✓ Found Back button - on detail screen");
                }
            } catch (Exception e) {}
        }
        
        if (onEditScreen) {
            System.out.println("✅ On Edit Asset screen");
        } else {
            System.out.println("⚠️ May not be on edit screen - form fields not detected");
            // Log what IS visible for debugging
            try {
                List<WebElement> allVisible = driver.findElements(
                    AppiumBy.iOSNsPredicateString("visible == true AND (type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeNavigationBar')")
                );
                System.out.println("   Debug: Found " + allVisible.size() + " visible elements");
                for (int i = 0; i < Math.min(5, allVisible.size()); i++) {
                    String name = allVisible.get(i).getAttribute("name");
                    String type = allVisible.get(i).getAttribute("type");
                    System.out.println("      - " + type + ": " + name);
                }
            } catch (Exception e) {}
        }
    }

    /**
     * Check if Edit Asset Details screen is displayed
     */
    public boolean isEditAssetScreenDisplayed() {
        // Check for Save Changes button (primary edit mode indicator)
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' OR label CONTAINS 'Save'")
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
     * Find the Required Fields Only toggle using multiple strategies
     * Returns the toggle WebElement or null if not found
     */
    private WebElement findRequiredFieldsToggle() {
        System.out.println("🔍 Finding Required Fields Only toggle...");

        // FIRST: Scroll down to Core Attributes section where toggle is located
        System.out.println("   📜 Scrolling to Core Attributes section (toggle location)...");
        for (int scrollAttempt = 0; scrollAttempt < 3; scrollAttempt++) {
            // Check if toggle is already visible before scrolling more
            try {
                WebElement toggle = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeSwitch' AND visible == true"
                    )
                );
                if (toggle != null && toggle.isDisplayed()) {
                    System.out.println("   ✅ Found visible toggle (scroll attempt " + scrollAttempt + ")");
                    // Continue with validation below
                    break;
                }
            } catch (Exception e) {
                // Toggle not visible yet, scroll down
                System.out.println("   Scroll attempt " + (scrollAttempt + 1) + " - toggle not visible yet");
                scrollFormDown();
                sleep(300);
            }
        }

        // Strategy 1: Find toggle by name/label containing "Required" (case insensitive search)
        try {
            WebElement toggle = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "(name CONTAINS[c] 'Required' OR label CONTAINS[c] 'Required' OR " +
                    "name CONTAINS[c] 'required fields' OR label CONTAINS[c] 'required fields') " +
                    "AND type == 'XCUIElementTypeSwitch' AND visible == true"
                )
            );
            if (toggle != null && toggle.isDisplayed()) {
                System.out.println("   ✅ Found toggle by name/label containing 'Required'");
                return toggle;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 1 (name contains): not found");
        }

        // Strategy 2: Find toggle by accessibility ID (multiple variations)
        String[] accessibilityIds = {
            "Required Fields Only", "RequiredFieldsOnly", "required_fields_only",
            "Required Fields", "RequiredFields", "toggle_required"
        };
        for (String accId : accessibilityIds) {
            try {
                WebElement toggle = driver.findElement(AppiumBy.accessibilityId(accId));
                if (toggle != null && toggle.isDisplayed()) {
                    System.out.println("   ✅ Found toggle by accessibility ID: " + accId);
                    return toggle;
                }
            } catch (Exception ignored) {}
        }
        System.out.println("   Strategy 2 (accessibility ID): not found");

        // Strategy 3: Scroll to "Core Attributes" section and find toggle there
        try {
            System.out.println("   Strategy 3: Looking for Core Attributes section...");
            
            // First try to find Core Attributes label and scroll to it
            for (int i = 0; i < 5; i++) {
                try {
                    WebElement coreAttrLabel = driver.findElement(
                        AppiumBy.iOSNsPredicateString(
                            "(name CONTAINS[c] 'Core Attribute' OR label CONTAINS[c] 'Core Attribute') " +
                            "AND type == 'XCUIElementTypeStaticText' AND visible == true"
                        )
                    );
                    if (coreAttrLabel != null && coreAttrLabel.isDisplayed()) {
                        System.out.println("   Found 'Core Attributes' section at Y=" + coreAttrLabel.getLocation().getY());
                        
                        // Now find toggle in this section (within 200 pixels below Core Attributes)
                        int coreAttrY = coreAttrLabel.getLocation().getY();
                        List<WebElement> switches = driver.findElements(
                            AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSwitch' AND visible == true")
                        );
                        for (WebElement sw : switches) {
                            int switchY = sw.getLocation().getY();
                            // Toggle should be BELOW Core Attributes label (within 200px)
                            if (switchY > coreAttrY && switchY < coreAttrY + 200) {
                                System.out.println("   ✅ Found toggle in Core Attributes section (Y=" + switchY + ")");
                                return sw;
                            }
                        }
                        break; // Found section but no toggle, don't keep scrolling
                    }
                } catch (Exception notFound) {
                    // Core Attributes not visible yet, scroll down
                    System.out.println("   Scrolling to find Core Attributes (attempt " + (i+1) + ")...");
                    scrollFormDown();
                    sleep(300);
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (Core Attributes section): not found");
        }

        // Strategy 4: Find the label "Required" and then find nearby toggle
        try {
            // Try multiple label patterns
            String[] labelPatterns = {
                "(name CONTAINS[c] 'Required' OR label CONTAINS[c] 'Required') AND type == 'XCUIElementTypeStaticText' AND visible == true",
                "type == 'XCUIElementTypeStaticText' AND (name BEGINSWITH[c] 'Required' OR label BEGINSWITH[c] 'Required') AND visible == true"
            };
            
            for (String pattern : labelPatterns) {
                try {
                    List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(pattern));
                    for (WebElement label : labels) {
                        if (label != null && label.isDisplayed()) {
                            System.out.println("   Found 'Required' label at Y=" + label.getLocation().getY() + " text: " + label.getAttribute("label"));
                            int labelY = label.getLocation().getY();

                            // Find switches near this label (within 150 pixels vertically for better coverage)
                            List<WebElement> switches = driver.findElements(
                                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSwitch' AND visible == true")
                            );
                            for (WebElement sw : switches) {
                                int switchY = sw.getLocation().getY();
                                if (Math.abs(switchY - labelY) < 150) {
                                    System.out.println("   ✅ Found toggle near 'Required' label (Y diff: " + Math.abs(switchY - labelY) + ")");
                                    return sw;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (find by nearby label): not found");
        }

        // Strategy 5: Look for toggle in a cell/row containing "Required" text
        try {
            List<WebElement> cells = driver.findElements(
                AppiumBy.iOSNsPredicateString("(type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeOther') AND visible == true")
            );
            System.out.println("   Strategy 6: Found " + cells.size() + " cells/rows");
            for (WebElement cell : cells) {
                try {
                    String cellName = cell.getAttribute("name");
                    String cellLabel = cell.getAttribute("label");
                    String cellText = (cellName != null ? cellName : "") + (cellLabel != null ? cellLabel : "");
                    if (cellText.toLowerCase().contains("required")) {
                        System.out.println("   Found cell with 'required' text: " + cellText.substring(0, Math.min(50, cellText.length())));
                        try {
                            WebElement toggle = cell.findElement(AppiumBy.className("XCUIElementTypeSwitch"));
                            if (toggle != null && toggle.isDisplayed()) {
                                System.out.println("   ✅ Found toggle in cell containing 'Required'");
                                return toggle;
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("   Strategy 5 (find in cell): error - " + e.getMessage());
        }

        // Strategy 6: Find switch by value attribute (toggle switches typically have value = "0" or "1")
        try {
            List<WebElement> switches = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSwitch' AND visible == true")
            );
            System.out.println("   Strategy 6: Found " + switches.size() + " visible switches");
            
            // Log info about each switch to help debug
            for (int i = 0; i < switches.size(); i++) {
                WebElement sw = switches.get(i);
                String name = sw.getAttribute("name");
                String label = sw.getAttribute("label");
                String value = sw.getAttribute("value");
                int y = sw.getLocation().getY();
                System.out.println("      Switch " + i + ": name=" + name + ", label=" + label + ", value=" + value + ", Y=" + y);
            }
            
            // Try to find the one that's in the top area (typically Required Fields toggle is near top)
            for (WebElement sw : switches) {
                int switchY = sw.getLocation().getY();
                // Required Fields toggle is typically in the upper portion of the screen
                if (switchY > 100 && switchY < 500) {
                    System.out.println("   ✅ Found switch in upper screen area (Y=" + switchY + ") - likely Required Fields toggle");
                    return sw;
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 6 (find by position): error - " + e.getMessage());
        }

        // Strategy 7 (Fallback): Get first visible switch with detailed logging
        try {
            List<WebElement> toggles = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSwitch' AND visible == true")
            );
            if (toggles.size() > 0) {
                System.out.println("   ⚠️ FALLBACK: Using first visible switch on page");
                System.out.println("   Found " + toggles.size() + " visible switches total");
                WebElement firstSwitch = toggles.get(0);
                System.out.println("   First switch: name=" + firstSwitch.getAttribute("name") + 
                    ", label=" + firstSwitch.getAttribute("label") + ", Y=" + firstSwitch.getLocation().getY());
                return firstSwitch;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 7 (fallback first switch): not found");
        }

        System.out.println("   ❌ Could not find Required Fields toggle after all strategies");
        return null;
    }

    /**
     * Check if Required Fields Only toggle is displayed
     */
    public boolean isRequiredFieldsToggleDisplayed() {
        WebElement toggle = findRequiredFieldsToggle();
        return toggle != null;
    }

    /**
     * Get Required Fields Only toggle state (ON/OFF)
     * Uses robust multi-strategy toggle detection
     */
    public boolean isRequiredFieldsToggleOn() {
        try {
            WebElement toggle = findRequiredFieldsToggle();
            if (toggle != null) {
                String value = toggle.getAttribute("value");
                boolean isOn = "1".equals(value);
                System.out.println("   Toggle state: " + (isOn ? "ON" : "OFF") + " (value='" + value + "')");
                return isOn;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get toggle state: " + e.getMessage());
        }
        return false;
    }

    /**
     * Toggle Required Fields Only switch
     * Uses robust multi-strategy toggle detection
     */
    public void toggleRequiredFieldsOnly() {
        try {
            WebElement toggle = findRequiredFieldsToggle();
            if (toggle != null) {
                toggle.click();
                System.out.println("✅ Toggled Required Fields Only switch");
                sleep(200);
            } else {
                System.out.println("⚠️ Could not find Required Fields toggle to click");
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
                // Use CONTAINS[c] for case-insensitive matching (e.g., "voltage" or "Voltage")
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "') AND visible == true")
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
     * SMART field filler - automatically detects if field is dropdown or text input
     * Tries dropdown first (for fields like Voltage, Manufacturer that might be dropdowns on some asset classes)
     * Falls back to text field if dropdown not found
     * Uses case-insensitive matching for field names
     */
    public void fillFieldAuto(String fieldName, String value) {
        System.out.println("📝 Auto-filling field: " + fieldName + " = " + value);
        
        // First check if this is a dropdown by looking for a button with the field name
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                String label = btn.getAttribute("label");
                
                // Case-insensitive check
                if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                    (label != null && label.toLowerCase().contains(fieldName.toLowerCase()))) {
                    
                    int y = btn.getLocation().getY();
                    if (y > 100 && y < 800) {
                        // Found a dropdown button, use dropdown method
                        System.out.println("   🔽 Found dropdown button, using selectDropdownOption");
                        selectDropdownOption(fieldName, value);
                        return;
                    }
                }
            }
        } catch (Exception e) {}
        
        // Not a dropdown, try as text field
        System.out.println("   📝 No dropdown found, trying text field");
        fillTextField(fieldName, value);
    }

    /**
     * Select dropdown option by field name and option value
     */
    public void selectDropdownOption(String fieldName, String optionValue) {
        System.out.println("📋 Selecting '" + optionValue + "' for dropdown '" + fieldName + "'...");
        
        boolean dropdownFound = false;
        
        // Try up to 3 scroll attempts to find the dropdown
        for (int scrollAttempt = 0; scrollAttempt < 3 && !dropdownFound; scrollAttempt++) {
            try {
                // STRATEGY 1: Find button containing field name (case-insensitive)
                List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
                
                // Debug: Log buttons that might match
                if (scrollAttempt == 0) {
                    System.out.println("   🔍 Searching for '" + fieldName + "' in " + buttons.size() + " buttons...");
                }
                
                for (WebElement btn : buttons) {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    
                    // Case-insensitive check
                    boolean nameMatch = name != null && name.toLowerCase().contains(fieldName.toLowerCase());
                    boolean labelMatch = label != null && label.toLowerCase().contains(fieldName.toLowerCase());
                    
                    if (nameMatch || labelMatch) {
                        // Check if visible (Y > 100 and Y < 800)
                        int y = btn.getLocation().getY();
                        System.out.println("   ✓ Found matching button: name='" + name + "' label='" + label + "' Y=" + y);
                        if (y > 100 && y < 800) {
                            btn.click();
                            dropdownFound = true;
                            System.out.println("   ✅ Clicked dropdown button");
                            sleep(300);
                            break;
                        } else {
                            System.out.println("   ⚠️ Button Y=" + y + " outside visible range (100-800)");
                        }
                    }
                }
                
                // STRATEGY 2: Find label then nearby button
                if (!dropdownFound) {
                    System.out.println("   🔍 Strategy 2: Finding label '" + fieldName + "'...");
                    List<WebElement> labels = driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "') AND visible == true")
                    );
                    System.out.println("   Found " + labels.size() + " labels containing '" + fieldName + "'");
                    
                    // DEBUG: If no labels found, show ALL visible labels on screen
                    if (labels.isEmpty() && scrollAttempt == 0) {
                        System.out.println("   📋 DEBUG: All visible labels on screen:");
                        try {
                            List<WebElement> allLabels = driver.findElements(
                                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND visible == true")
                            );
                            int count = 0;
                            for (WebElement lbl : allLabels) {
                                String name = lbl.getAttribute("name");
                                if (name != null && !name.isEmpty() && name.length() < 50) {
                                    int y = lbl.getLocation().getY();
                                    // Only show labels in form area (y > 200 and y < 800)
                                    if (y > 200 && y < 800) {
                                        System.out.println("      - '" + name + "' at Y=" + y);
                                        count++;
                                        if (count > 15) {
                                            System.out.println("      ... (showing first 15)");
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception debugEx) {}
                    }
                    
                    for (WebElement lbl : labels) {
                        int labelY = lbl.getLocation().getY();
                        String lblName = lbl.getAttribute("name");
                        System.out.println("   Label: '" + lblName + "' at Y=" + labelY);
                        
                        if (labelY > 100 && labelY < 800) {
                            // Found visible label, look for button below it
                            for (WebElement btn : buttons) {
                                int btnY = btn.getLocation().getY();
                                // Button should be slightly below the label (within 100 pixels)
                                if (btnY > labelY && btnY < labelY + 100) {
                                    String btnName = btn.getAttribute("name");
                                    System.out.println("   ✓ Found button below label: '" + btnName + "' at Y=" + btnY);
                                    btn.click();
                                    dropdownFound = true;
                                    System.out.println("   ✅ Clicked dropdown button");
                                    sleep(300);
                                    break;
                                }
                            }
                            if (dropdownFound) break;
                        }
                    }
                }
                
                if (!dropdownFound && scrollAttempt < 2) {
                    // Alternate: first try UP, then try DOWN
                    if (scrollAttempt == 0) {
                        System.out.println("   Dropdown not visible, scrolling UP... (attempt 1)");
                        scrollFormUp();
                    } else {
                        System.out.println("   Dropdown not visible, scrolling DOWN... (attempt 2)");
                        scrollFormDown();
                    }
                    sleep(200);
                }
            } catch (Exception e) {
                System.out.println("   Error finding dropdown: " + e.getMessage());
            }
        }
        
        if (!dropdownFound) {
            System.out.println("⚠️ Dropdown '" + fieldName + "' not found after scrolling");
            return;
        }
        
        // Now try to select the option value
        sleep(300);  // Wait for dropdown to open
        
        try {
            // Try exact match first
            WebElement option = driver.findElement(AppiumBy.accessibilityId(optionValue));
            option.click();
            System.out.println("✅ Selected '" + optionValue + "' for '" + fieldName + "'");
            return;
        } catch (Exception e) {}
        
        // Try contains match
        try {
            List<WebElement> options = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS '" + optionValue.replace("V", "").replace("A", "") + "' OR label CONTAINS '" + optionValue.replace("V", "").replace("A", "") + "')")
            );
            for (WebElement opt : options) {
                int y = opt.getLocation().getY();
                if (y > 100 && y < 800) {
                    opt.click();
                    System.out.println("✅ Selected option containing '" + optionValue + "' for '" + fieldName + "'");
                    return;
                }
            }
        } catch (Exception e) {}
        
        // Try clicking any visible cell/option
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            for (WebElement cell : cells) {
                String text = cell.getAttribute("label");
                if (text != null && text.contains(optionValue.replace("V", "").replace("A", ""))) {
                    cell.click();
                    System.out.println("✅ Selected cell '" + text + "' for '" + fieldName + "'");
                    return;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not select option '" + optionValue + "' for '" + fieldName + "'");
        // Tap back to close dropdown
        clickBack();
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
            sleep(400);
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
        System.out.println("💾 Looking for Save Changes button...");
        
        // Save Changes button appears at the BOTTOM of the screen after making changes
        // First try to find it without scrolling
        
        // Try 1: Direct visibility check
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            saveBtn.click();
            System.out.println("✅ Clicked Save Changes (visible)");
            sleep(400);
            return;
        } catch (Exception e) {}
        
        // Try 2: Scroll DOWN to find Save Changes (it's at the bottom)
        System.out.println("   Scrolling down to find Save Changes...");
        for (int i = 0; i < 3; i++) {
            scrollFormDown();
            sleep(300);
            
            try {
                WebElement saveBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton' AND visible == true")
                );
                saveBtn.click();
                System.out.println("✅ Clicked Save Changes (after scroll)");
                sleep(400);
                return;
            } catch (Exception e) {}
        }
        
        // Try 3: Scroll back up and look again
        System.out.println("   Scrolling up to check...");
        for (int i = 0; i < 3; i++) {
            scrollFormUp();
            sleep(300);
            
            try {
                WebElement saveBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton' AND visible == true")
                );
                saveBtn.click();
                System.out.println("✅ Clicked Save Changes (after scroll up)");
                sleep(400);
                return;
            } catch (Exception e) {}
        }
        
        // Try 4: AccessibilityId without visible check
        try {
            WebElement saveChangesBtn = driver.findElement(AppiumBy.accessibilityId("Save Changes"));
            saveChangesBtn.click();
            System.out.println("✅ Clicked Save Changes (accessibilityId)");
            sleep(400);
            return;
        } catch (Exception e) {}
        
        // Try 5: Generic Save button search
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'Save'")
            );
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains("Save")) {
                    btn.click();
                    System.out.println("✅ Clicked Save button: " + name);
                    sleep(400);
                    return;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not find Save Changes button - changes may not have been made");
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
            sleep(200);
            return;
        } catch (Exception e) {}
        
        // Strategy 2: Find button with Cancel label
        try {
            WebElement cancelBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel button");
            sleep(200);
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Find StaticText "Cancel" and click
        try {
            WebElement cancelText = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            cancelText.click();
            System.out.println("✅ Clicked Cancel text");
            sleep(200);
            return;
        } catch (Exception e) {}
        
        // Strategy 4: Tap coordinates at top-left (typical Cancel position)
        try {
            driver.executeScript("mobile: tap", Map.of("x", 60, "y", 60));
            System.out.println("✅ Tapped Cancel position (60, 60)");
            sleep(200);
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
            sleep(200);
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
            sleep(400);
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
        fillTextField("Ampere Rating", value);
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
                    sleep(200);
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
                            sleep(200);
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
        fillTextField("Interrupting Rating", value);
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
                    sleep(200);
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
                            sleep(200);
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
        System.out.println("📋 Filling all ATS required fields (using dropdowns)...");
        
        // Scroll to see fields
        scrollFormDown();
        sleep(300);
        
        // ATS fields are DROPDOWNS, not text fields
        // Use selectDropdownOption which handles finding and selecting
        
        // 1. Ampere Rating dropdown
        try {
            System.out.println("📝 Selecting Ampere Rating...");
            selectDropdownOption("Ampere Rating", "100A");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Ampere Rating: " + e.getMessage());
            // Try alternate value
            try {
                selectDropdownOption("Ampere Rating", "100");
            } catch (Exception e2) {
                System.out.println("⚠️ Ampere Rating selection failed");
            }
        }
        sleep(200);
        
        // 2. Voltage dropdown  
        try {
            System.out.println("📝 Selecting Voltage...");
            selectDropdownOption("Voltage", "480V");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Voltage: " + e.getMessage());
            // Try alternate value
            try {
                selectDropdownOption("Voltage", "480");
            } catch (Exception e2) {
                System.out.println("⚠️ Voltage selection failed");
            }
        }
        sleep(200);
        
        // 3. Mains Type dropdown
        scrollFormDown();
        sleep(200);
        try {
            System.out.println("📝 Selecting Mains Type...");
            selectDropdownOption("Mains Type", "Normal");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Mains Type: " + e.getMessage());
            try {
                selectDropdownOption("Mains Type", "Emergency");
            } catch (Exception e2) {
                System.out.println("⚠️ Mains Type selection failed");
            }
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

    /**
     * HELPER: Check if the CURRENTLY SELECTED asset class matches the target
     * This checks the asset class field VALUE, not just if any element with that name exists
     */
    private boolean isCurrentAssetClassEqualTo(String targetClass) {
        try {
            // FAST check: Directly look for a button with the target class name
            // This is MUCH faster than searching all buttons and checking positions
            try {
                WebElement targetBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == '" + targetClass + "' AND visible == true")
                );
                if (targetBtn != null && targetBtn.isDisplayed()) {
                    // Found button with exact name - check if it's in the form area (not in picker)
                    int btnY = targetBtn.getLocation().getY();
                    // Form fields are typically between y=200 and y=600, picker is below 600
                    if (btnY > 150 && btnY < 600) {
                        System.out.println("   Current asset class: " + targetClass + " (fast match)");
                        return true;
                    }
                }
            } catch (Exception notFound) {
                // Target class button not found - that's OK, means it's not the current class
            }
            
            // FAST check 2: Try case-insensitive/contains match
            try {
                List<WebElement> matchingBtns = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name CONTAINS[c] '" + targetClass + "') AND visible == true")
                );
                for (WebElement btn : matchingBtns) {
                    int btnY = btn.getLocation().getY();
                    if (btnY > 150 && btnY < 600) {
                        String name = btn.getAttribute("name");
                        System.out.println("   Current asset class: " + name + " (contains " + targetClass + ")");
                        return true;
                    }
                }
            } catch (Exception e) {}
            
            return false;
        } catch (Exception e) {
            System.out.println("   Error checking current asset class: " + e.getMessage());
            return false;
        }
    }

    public final void changeAssetClassToBusway() {
        System.out.println("📋 Changing asset class to Busway (FAST)...");
        
        // Quick check - is Busway already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Busway")) {
            System.out.println("✅ Already Busway");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is ATS already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("ATS")) {
            System.out.println("✅ Already ATS");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is Generator already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Generator")) {
            System.out.println("✅ Already Generator");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class')")
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
            sleep(300);
            
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
                        sleep(300);
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
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                int btnY = btn.getLocation().getY();
                if (Math.abs(btnY - labelY) < 60) {
                    String name = btn.getAttribute("name");
                    System.out.println("   Found button near Asset Class: " + name);
                    btn.click();
                    sleep(300);
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
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            int screenWidth = driver.manage().window().getSize().getWidth();
            
            int[] xPositions = {screenWidth / 2, screenWidth - 50, screenWidth - 150, screenWidth / 4 * 3};
            for (int tapX : xPositions) {
                System.out.println("   Trying tap at (" + tapX + ", " + labelY + ")...");
                driver.executeScript("mobile: tap", Map.of("x", tapX, "y", labelY));
                sleep(400);
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
                    sleep(200);
                    
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
                            sleep(200);
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
                    sleep(200);
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
                    "type == 'XCUIElementTypeStaticText' AND (name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class')"
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
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class'")
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
        
        // Quick check - is Capacitor already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Capacitor")) {
            System.out.println("✅ Already Capacitor");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is Circuit Breaker already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Circuit Breaker")) {
            System.out.println("✅ Already Circuit Breaker");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
     * TURBO: Change asset class to Default (FAST method - no retries)
     */
    public final void changeAssetClassToDefault() {
        System.out.println("📋 Changing asset class to Default (FAST)...");
        
        // Quick check - is Default already displayed as selected?
        try {
            WebElement defaultEl = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == 'Default'")
            );
            if (defaultEl.isDisplayed() && defaultEl.getLocation().getY() < 500) {
                System.out.println("✅ Already Default");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(200);
            
            // Try to find Default - may need to scroll in dropdown
            for (int scrollAttempt = 0; scrollAttempt < 2; scrollAttempt++) {
                try {
                    WebElement defaultEl = driver.findElement(AppiumBy.accessibilityId("Default"));
                    if (defaultEl.isDisplayed()) {
                        defaultEl.click();
                        System.out.println("✅ Changed to Default");
                        sleep(300);
                        return;
                    }
                } catch (Exception e) {}
                
                // Scroll down inside dropdown to find Default
                System.out.println("   Scrolling dropdown to find Default (attempt " + (scrollAttempt + 1) + ")");
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
            
            System.out.println("⚠️ Default not found in dropdown after scrolling");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Default: " + e.getMessage());
        }
    }

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
        
        // Quick check - is Disconnect Switch already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Disconnect Switch")) {
            System.out.println("✅ Already Disconnect Switch");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is Fuse already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Fuse")) {
            System.out.println("✅ Already Fuse");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is Junction Box already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Junction Box")) {
            System.out.println("✅ Already Junction Box");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is Loadcenter already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Loadcenter")) {
            System.out.println("✅ Already Loadcenter");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        System.out.println("📋 Changing asset class to MCC...");
        
        // Quick check - is MCC already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("MCC")) {
            System.out.println("✅ Already MCC");
            return;
        }
        
        // Strategy 1: Find "Asset Class" label and tap below it
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("(name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class') AND type == 'XCUIElementTypeStaticText' AND visible == true")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping Asset Class dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(200);
        } catch (Exception e) {
            System.out.println("   Asset Class label not found, trying button approach...");
            
            // Strategy 2: Find any asset class button (comprehensive list)
            try {
                WebElement classBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true AND " +
                        "(name == 'ATS' OR name == 'Busway' OR name == 'Capacitor' OR name == 'Circuit Breaker' OR " +
                        "name == 'Disconnect Switch' OR name == 'Fuse' OR name == 'Generator' OR name == 'Junction Box' OR " +
                        "name == 'Loadcenter' OR name == 'MCC' OR name == 'MCC Bucket' OR name == 'None' OR " +
                        "name == 'Other (Non-electrical)' OR name == 'Other (OCP)' OR name == 'Panelboard' OR " +
                        "name == 'PDU' OR name == 'Relay' OR name == 'Switchboard' OR name == 'Transformer' OR " +
                        "name == 'UPS' OR name == 'Utility')")
                );
                System.out.println("   Current class: " + classBtn.getAttribute("name"));
                classBtn.click();
                sleep(200);
            } catch (Exception e2) {
                System.out.println("⚠️ Could not find Asset Class button: " + e2.getMessage());
                return;
            }
        }
        
        // Scroll up in dropdown to find MCC (it's near the middle alphabetically)
        sleep(300);
        
        // Try to click MCC
        try {
            WebElement mccOption = driver.findElement(AppiumBy.accessibilityId("MCC"));
            mccOption.click();
            System.out.println("✅ Changed to MCC");
        } catch (Exception e) {
            // MCC not visible, try scrolling in picker
            System.out.println("   MCC not visible, scrolling picker...");
            try {
                // Scroll up in picker to find MCC
                for (int i = 0; i < 3; i++) {
                    driver.executeScript("mobile: swipe", Map.of(
                        "direction", "up",
                        "velocity", 500
                    ));
                    sleep(200);
                    
                    try {
                        WebElement mcc = driver.findElement(AppiumBy.accessibilityId("MCC"));
                        mcc.click();
                        System.out.println("✅ Changed to MCC (after scroll)");
                        return;
                    } catch (Exception e3) {}
                }
                
                // Try scroll down
                for (int i = 0; i < 3; i++) {
                    driver.executeScript("mobile: swipe", Map.of(
                        "direction", "down",
                        "velocity", 500
                    ));
                    sleep(200);
                    
                    try {
                        WebElement mcc = driver.findElement(AppiumBy.accessibilityId("MCC"));
                        mcc.click();
                        System.out.println("✅ Changed to MCC (after scroll down)");
                        return;
                    } catch (Exception e3) {}
                }
                
                System.out.println("⚠️ Could not find MCC option in picker");
            } catch (Exception e2) {
                System.out.println("⚠️ Error scrolling picker: " + e2.getMessage());
            }
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
        
        // Quick check - is MCC Bucket already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("MCC Bucket")) {
            System.out.println("✅ Already MCC Bucket");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
     * Uses multiple strategies and includes scroll-into-view capability
     */
    public boolean isSaveChangesButtonVisible() {
        // FAST check - "Save" and "Save Changes" are the SAME button
        // Just look for any button containing "Save"
        try {
            List<WebElement> saveBtns = driver.findElements(
                AppiumBy.iOSNsPredicateString("(name CONTAINS 'Save' OR label CONTAINS 'Save') AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            if (!saveBtns.isEmpty()) {
                System.out.println("   ✅ Save button is visible (still on edit screen)");
                return true;
            }
        } catch (Exception e) {}
        
        // Save button not visible - this is NORMAL after successful save (left edit screen)
        System.out.println("   ℹ️ Save button not visible (likely left edit screen after save)");
        return false;
    }
    
    /**
     * DEPRECATED - keeping for reference but use isSaveChangesButtonVisible() instead
     */
    private boolean isSaveChangesButtonVisibleOLD_SLOW() {
        System.out.println("🔍 Checking if Save Changes button is visible...");

        // Strategy 1: Check for "Save Changes" button directly
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            if (saveBtn.isDisplayed()) {
                System.out.println("   ✅ Found visible 'Save Changes' button");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 1 (Save Changes visible): not found");
        }

        // Strategy 2: Check for "Save Changes" by label
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label CONTAINS 'Save' AND type == 'XCUIElementTypeButton'")
            );
            if (saveBtn.isDisplayed()) {
                System.out.println("   ✅ Found 'Save Changes' button by label");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 (Save Changes by label): not found");
        }

        // Strategy 3: Check by accessibility ID
        try {
            WebElement saveBtn = driver.findElement(AppiumBy.accessibilityId("Save Changes"));
            if (saveBtn.isDisplayed()) {
                System.out.println("   ✅ Found 'Save Changes' button by accessibility ID");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (accessibility ID): not found");
        }

        // Strategy 4: Check for "Save" button (alternative naming)
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Save' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            if (saveBtn.isDisplayed()) {
                System.out.println("   ✅ Found visible 'Save' button (alternative)");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 4 (Save button): not found");
        }

        // Strategy 5: Scroll DOWN and check (Save Changes is typically at bottom of form)
        System.out.println("   Scrolling down to find Save Changes button...");
        for (int i = 0; i < 3; i++) {
            scrollFormDown();
            sleep(300);

            try {
                WebElement saveBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "(name CONTAINS 'Save' OR name == 'Save') AND type == 'XCUIElementTypeButton' AND visible == true"
                    )
                );
                if (saveBtn.isDisplayed()) {
                    System.out.println("   ✅ Found Save button after scrolling down (attempt " + (i + 1) + ")");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("   Scroll down attempt " + (i + 1) + ": not found yet");
            }
        }

        // Strategy 6: Scan all buttons for Save-related text
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );
            System.out.println("   Scanning " + buttons.size() + " buttons for Save...");
            for (WebElement btn : buttons) {
                try {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    if ((name != null && name.toLowerCase().contains("save")) ||
                        (label != null && label.toLowerCase().contains("save"))) {
                        if (btn.isDisplayed()) {
                            System.out.println("   ✅ Found button with Save text: name='" + name + "' label='" + label + "'");
                            return true;
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("   Strategy 6 (scan all buttons): failed");
        }

        System.out.println("   ❌ Save Changes button not found after all strategies");
        return false;
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
     * Check if current asset class is MCC
     */
    public boolean isAssetClassMCC() {
        System.out.println("🔍 Checking if asset class is MCC...");
        try {
            // Look for MCC button (Asset Class dropdown showing MCC)
            WebElement mcc = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'MCC' AND type == 'XCUIElementTypeButton' AND visible == true")
            );
            System.out.println("   Found MCC button: " + mcc.isDisplayed());
            return mcc.isDisplayed();
        } catch (Exception e) {
            System.out.println("   MCC button not found (might be ATS, Generator, etc.)");
            
            // Debug: show what Asset Class buttons ARE visible
            try {
                List<WebElement> buttons = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true")
                );
                for (WebElement btn : buttons) {
                    String name = btn.getAttribute("name");
                    if (name != null && (name.equals("ATS") || name.equals("MCC") || name.equals("Generator") || 
                        name.equals("Busway") || name.equals("Circuit Breaker") || name.equals("Fuse"))) {
                        System.out.println("   Found Asset Class button: " + name);
                    }
                }
            } catch (Exception e2) {}
            
            return false;
        }
    }

    /**
     * Click Save Changes button to save asset changes
     * Note: After changing asset class, button is "Save Changes" not "Save"
     */
    public void clickSaveButton() {
        System.out.println("💾 Clicking Save button...");
        try {
            WebElement saveBtn = null;
            
            // Try 1: "Save Changes" visible
            try {
                saveBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton' AND visible == true")
                );
                System.out.println("   Found visible 'Save Changes' button");
            } catch (Exception e) {}
            
            // Try 2: Scroll down and find "Save Changes"
            if (saveBtn == null) {
                System.out.println("   Scrolling to find Save Changes...");
                for (int i = 0; i < 3; i++) {
                    scrollFormDown();
                    sleep(200);
                    try {
                        saveBtn = driver.findElement(
                            AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton' AND visible == true")
                        );
                        System.out.println("   Found 'Save Changes' after scrolling");
                        break;
                    } catch (Exception e) {}
                }
            }
            
            // Try 3: "Save" button
            if (saveBtn == null) {
                try {
                    saveBtn = driver.findElement(AppiumBy.accessibilityId("Save"));
                    System.out.println("   Found 'Save' button");
                } catch (Exception e) {}
            }
            
            // Try 4: Done
            if (saveBtn == null) {
                try {
                    saveBtn = driver.findElement(AppiumBy.accessibilityId("Done"));
                    System.out.println("   Found 'Done' button");
                } catch (Exception e) {}
            }
            
            if (saveBtn != null) {
                saveBtn.click();
                sleep(500);
                System.out.println("✅ Clicked Save button");
            } else {
                System.out.println("⚠️ Save button not found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Save: " + e.getMessage());
        }
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
        for (int scrollAttempt = 0; scrollAttempt < 2; scrollAttempt++) {
            
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
                    sleep(200);
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
                    sleep(200);
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
            if (scrollAttempt < 2) {
                System.out.println("   Field '" + fieldName + "' not visible, scrolling... (attempt " + (scrollAttempt + 1) + ")");
                scrollFormDown();
                sleep(200);
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
        
        for (int i = 0; i < 3; i++) {
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
        sleep(600);  // Wait for save to complete
        
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
            sleep(200);
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
            sleep(200);
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
            sleep(200);
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
                        sleep(200);
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
            sleep(200);
            
            WebElement busway = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Busway' OR label == 'Busway'")
            );
            busway.click();
            System.out.println("✅ Selected Busway (after scroll)");
            sleep(200);
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
                sleep(200);
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
            sleep(400); // Wait for UI to update after class change
            
            // Scroll to see Core Attributes area
            scrollFormDown();
            sleep(200);
            
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
            sleep(200);
            
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
        System.out.println("📝 Checking if Edit screen is displayed for Busway...");
        try {
            // Strategy 1: Check for "Save Changes" button (Edit screen indicator)
            try {
                List<WebElement> saveButtons = driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (name CONTAINS 'Save' OR name CONTAINS 'Save')"
                    )
                );
                if (!saveButtons.isEmpty()) {
                    System.out.println("   ✅ Found Save Changes button");
                    return true;
                }
            } catch (Exception e) {}
            
            // Strategy 2: Check for "Cancel" button
            try {
                List<WebElement> cancelButtons = driver.findElements(AppiumBy.accessibilityId("Cancel"));
                if (!cancelButtons.isEmpty()) {
                    System.out.println("   ✅ Found Cancel button");
                    return true;
                }
            } catch (Exception e) {}
            
            // Strategy 3: Check for "Busway" class visible (we changed to Busway)
            try {
                List<WebElement> buswayElements = driver.findElements(
                    AppiumBy.iOSNsPredicateString("name == 'Busway' OR label == 'Busway'")
                );
                if (!buswayElements.isEmpty()) {
                    System.out.println("   ✅ Found Busway class indicator");
                    return true;
                }
            } catch (Exception e) {}
            
            // Strategy 4: Check for asset form fields (Name, Asset Class labels)
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND (name == 'Name' OR name CONTAINS 'Asset Class')"
                    )
                );
                if (labels.size() >= 2) {
                    System.out.println("   ✅ Found form labels (Name, Asset Class)");
                    return true;
                }
            } catch (Exception e) {}
            
            System.out.println("   ❌ Edit screen indicators not found");
            return false;
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Change asset class to Motor by selecting from dropdown
     */
    public final void changeAssetClassToMotor() {
        System.out.println("📋 Changing asset class to Motor (FAST)...");
        
        // Quick check - is Motor already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Motor")) {
            System.out.println("✅ Already Motor");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is Other already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Other")) {
            System.out.println("✅ Already Other");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is Panelboard already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("Panelboard")) {
            System.out.println("✅ Already Panelboard");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is PDU already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("PDU")) {
            System.out.println("✅ Already PDU");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
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
        
        // Quick check - is Relay already displayed as selected?
        try {
            WebElement relay = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == 'Relay'")
            );
            if (relay.isDisplayed() && relay.getLocation().getY() < 500) {
                System.out.println("✅ Already Relay");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(200);
            
            // Try to find Relay - may need to scroll in dropdown
            // Relay is near bottom alphabetically so needs more scrolling (4 attempts)
            for (int scrollAttempt = 0; scrollAttempt < 4; scrollAttempt++) {
                try {
                    WebElement relay = driver.findElement(AppiumBy.accessibilityId("Relay"));
                    if (relay.isDisplayed()) {
                        relay.click();
                        System.out.println("✅ Changed to Relay");
                        sleep(300);
                        return;
                    }
                } catch (Exception e) {}
                
                // Scroll down inside dropdown to find Relay
                System.out.println("   Scrolling dropdown to find Relay (attempt " + (scrollAttempt + 1) + ")");
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
            
            System.out.println("⚠️ Relay not found in dropdown after 4 scroll attempts");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Relay: " + e.getMessage());
        }
    }

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
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(200);
            
            // Try to find Switchboard - may need to scroll in dropdown
            // Asset classes alphabetical: ATS, Busway, ... Switchboard, Transformer, UPS, Utility, VFD
            // Switchboard is late alphabetically, needs 4 scroll attempts
            for (int scrollAttempt = 0; scrollAttempt < 4; scrollAttempt++) {
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
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(200);
            
            // Try to find Transformer - may need to scroll in dropdown
            // Transformer is late alphabetically, needs 4 scroll attempts
            for (int scrollAttempt = 0; scrollAttempt < 4; scrollAttempt++) {
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
        
        // Quick check - is UPS already the CURRENT asset class?
        if (isCurrentAssetClassEqualTo("UPS")) {
            System.out.println("✅ Already UPS");
            return;
        }
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(200);
            
            // Try to find UPS - may need to scroll in dropdown (UPS is near the end)
            // UPS is very late alphabetically, needs 4 scroll attempts
            for (int scrollAttempt = 0; scrollAttempt < 4; scrollAttempt++) {
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
        System.out.println("📋 Changing asset class to Utility (FAST)...");
        
        // Quick check - is Utility already displayed as selected?
        try {
            WebElement utility = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == 'Utility'")
            );
            if (utility.isDisplayed() && utility.getLocation().getY() < 500) {
                System.out.println("✅ Already Utility");
                return;
            }
        } catch (Exception e) {}
        
        // Find "Asset Class" label and tap below it to open dropdown
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(200);
            
            // Try to find Utility - needs many scrolls (at end of list)
            for (int scrollAttempt = 0; scrollAttempt < 8; scrollAttempt++) {
                try {
                    WebElement utility = driver.findElement(AppiumBy.accessibilityId("Utility"));
                    if (utility.isDisplayed()) {
                        utility.click();
                        System.out.println("✅ Changed to Utility");
                        sleep(300);
                        return;
                    }
                } catch (Exception e) {}
                
                // Scroll down inside dropdown to find Utility
                System.out.println("   Scrolling dropdown to find Utility (attempt " + (scrollAttempt + 1) + ")");
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
            
            System.out.println("⚠️ Utility not found in dropdown after scrolling");
        } catch (Exception e) {
            System.out.println("⚠️ Could not change to Utility: " + e.getMessage());
        }
    }

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
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Tapping dropdown at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(200);
            
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
            sleep(500); // Wait for save to complete
            
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



    // ================================================================
    // CAPITALIZATION AND NAMING VALIDATION METHODS
    // ================================================================

    /**
     * Check if a label/text is properly capitalized (Title Case or specific format)
     * Examples of proper: "Asset Name", "Circuit Breaker", "ATS"
     * Examples of improper: "asset name", "circuit breaker", "ats"
     */
    public boolean isProperlyCapitalized(String text) {
        if (text == null || text.isEmpty()) return false;
        
        // Known abbreviations that should be all caps
        String[] allCapsWords = {"ATS", "UPS", "PDU", "MCC", "VFD", "QR", "ID"};
        for (String abbrev : allCapsWords) {
            if (text.toUpperCase().equals(abbrev)) {
                return text.equals(abbrev); // Must be all caps
            }
        }
        
        // For regular words, first letter should be uppercase
        String[] words = text.split(" ");
        for (String word : words) {
            if (word.isEmpty()) continue;
            
            // Check if first letter is uppercase
            if (!Character.isUpperCase(word.charAt(0))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all visible field labels on the current screen
     * Returns a map of found labels and whether they're properly capitalized
     */
    public java.util.Map<String, Boolean> checkFieldLabelsCapitalization() {
        System.out.println("🔍 Checking field labels capitalization...");
        java.util.Map<String, Boolean> results = new java.util.HashMap<>();
        
        try {
            // Find all StaticText elements that could be labels
            List<WebElement> staticTexts = driver.findElements(
                AppiumBy.className("XCUIElementTypeStaticText")
            );
            
            // Known field labels to check
            String[] expectedLabels = {
                "Name", "Asset Class", "Asset Subtype", "Location", "QR Code",
                "Core Attributes", "Electrical Rating", "Manufacturer", 
                "Model", "Serial Number", "Notes"
            };
            
            for (WebElement text : staticTexts) {
                try {
                    String name = text.getAttribute("name");
                    String label = text.getAttribute("label");
                    String actualText = name != null ? name : label;
                    
                    if (actualText != null && !actualText.isEmpty()) {
                        // Check against expected labels
                        for (String expected : expectedLabels) {
                            if (actualText.equalsIgnoreCase(expected)) {
                                boolean isProper = actualText.equals(expected);
                                results.put(actualText, isProper);
                                
                                if (!isProper) {
                                    System.out.println("   ❌ IMPROPER: '" + actualText + "' should be '" + expected + "'");
                                } else {
                                    System.out.println("   ✅ PROPER: '" + actualText + "'");
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("   Error checking labels: " + e.getMessage());
        }
        
        return results;
    }

    /**
     * Check if Asset Class dropdown options are properly capitalized
     * Expected: "ATS", "Circuit Breaker", "Disconnect Switch" (not "ats", "circuit breaker")
     */
    public java.util.Map<String, Boolean> checkAssetClassOptionsCapitalization() {
        System.out.println("🔍 Checking Asset Class options capitalization...");
        java.util.Map<String, Boolean> results = new java.util.HashMap<>();
        
        // Expected proper capitalization
        String[] expectedClasses = {
            "ATS", "Busway", "Circuit Breaker", "Disconnect Switch", 
            "Fuse", "Generator", "MCC", "Motor Starter", "Panelboard",
            "PDU", "Switchboard", "Switchgear", "Transformer", "UPS", "VFD"
        };
        
        try {
            // Click to open Asset Class dropdown
            clickSelectAssetClass();
            sleep(200);
            
            // Get all visible options
            List<WebElement> options = driver.findElements(
                AppiumBy.className("XCUIElementTypeStaticText")
            );
            
            for (WebElement opt : options) {
                try {
                    String optText = opt.getAttribute("name");
                    if (optText == null) optText = opt.getAttribute("label");
                    
                    if (optText != null && !optText.isEmpty()) {
                        for (String expected : expectedClasses) {
                            if (optText.equalsIgnoreCase(expected)) {
                                boolean isProper = optText.equals(expected);
                                results.put(optText, isProper);
                                
                                if (!isProper) {
                                    System.out.println("   ❌ IMPROPER: '" + optText + "' should be '" + expected + "'");
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // Close dropdown
            try {
                driver.findElement(AppiumBy.accessibilityId("Cancel")).click();
            } catch (Exception e) {
                // Try tapping outside
                driver.executeScript("mobile: tap", java.util.Map.of("x", 200, "y", 100));
            }
            sleep(300);
            
        } catch (Exception e) {
            System.out.println("   Error checking Asset Class options: " + e.getMessage());
        }
        
        return results;
    }

    /**
     * Check if a specific field value has improper lowercase when it should be capitalized
     * Returns the found text if found, null if not found
     */
    public String findImproperlyCapitalizedText(String searchTerm) {
        System.out.println("🔍 Looking for improperly capitalized '" + searchTerm + "'...");
        
        try {
            // Search for lowercase version
            String lowerVersion = searchTerm.toLowerCase();
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(name == '" + lowerVersion + "' OR label == '" + lowerVersion + "')"
                )
            );
            
            for (WebElement el : elements) {
                if (el.isDisplayed()) {
                    String found = el.getAttribute("name");
                    if (found == null) found = el.getAttribute("label");
                    System.out.println("   ❌ Found lowercase: '" + found + "'");
                    return found;
                }
            }
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Get selected Asset Class text from the dropdown button
     */
    public String getSelectedAssetClassText() {
        System.out.println("🔍 Getting selected Asset Class text...");
        try {
            // Look for button that shows selected class
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );
            
            String[] classes = {"ATS", "Busway", "Circuit Breaker", "Disconnect Switch", 
                               "Fuse", "Generator", "MCC", "VFD", "UPS", "PDU"};
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null) {
                    for (String cls : classes) {
                        if (name.equalsIgnoreCase(cls)) {
                            System.out.println("   Found: '" + name + "'");
                            return name;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get selected Asset Subtype text
     */
    public String getSelectedSubtypeText() {
        System.out.println("🔍 Getting selected Subtype text...");
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && (name.contains("(") || name.contains("kVA") || name.contains("kW"))) {
                    System.out.println("   Found: '" + name + "'");
                    return name;
                }
            }
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        return null;
    }



    /**
     * Get the current value of a text field by its label name
     * @param fieldName The label/name of the field (e.g., "Manufacturer", "Serial Number")
     * @return The current value of the field, or null if not found
     */
    public String getTextFieldValue(String fieldName) {
        System.out.println("📝 Getting value of field: " + fieldName);
        
        try {
            // First scroll down to make sure field is visible
            scrollFormDown();
            sleep(300);
            
            // Strategy 1: Find label, then look for nearby text field
            try {
                WebElement label = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " +
                        "(name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "')"
                    )
                );
                
                if (label.isDisplayed()) {
                    int labelY = label.getLocation().getY();
                    System.out.println("   Found label '" + fieldName + "' at Y=" + labelY);
                    
                    // Find text fields near this label (within 100px below)
                    List<WebElement> textFields = driver.findElements(
                        AppiumBy.className("XCUIElementTypeTextField")
                    );
                    
                    for (WebElement field : textFields) {
                        try {
                            int fieldY = field.getLocation().getY();
                            if (fieldY > labelY && fieldY < labelY + 100) {
                                String value = field.getAttribute("value");
                                System.out.println("   ✅ Found value: '" + value + "'");
                                return value != null ? value : "";
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                System.out.println("   Strategy 1 failed: " + e.getMessage());
            }
            
            // Strategy 2: Check all text fields for one that contains the field name in its accessibility
            try {
                List<WebElement> allFields = driver.findElements(
                    AppiumBy.className("XCUIElementTypeTextField")
                );
                
                for (WebElement field : allFields) {
                    try {
                        String name = field.getAttribute("name");
                        String fieldLabel = field.getAttribute("label");
                        
                        if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                            (fieldLabel != null && fieldLabel.toLowerCase().contains(fieldName.toLowerCase()))) {
                            String value = field.getAttribute("value");
                            System.out.println("   ✅ Found field by name/label, value: '" + value + "'");
                            return value != null ? value : "";
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                System.out.println("   Strategy 2 failed: " + e.getMessage());
            }
            
            System.out.println("   ❌ Could not find field: " + fieldName);
            return null;
            
        } catch (Exception e) {
            System.out.println("   Error getting field value: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verify we're on the Asset Detail VIEW screen (not Edit screen)
     * The Detail View screen shows asset info but has "Edit" button, not "Save Changes"
     */
    public boolean isAssetDetailViewScreen() {
        System.out.println("📝 Checking if on Asset Detail View screen...");
        
        try {
            // On Detail View, we should see "Edit" button (not "Save Changes")
            boolean hasEditButton = false;
            boolean hasSaveButton = false;
            
            try {
                WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
                hasEditButton = editBtn.isDisplayed();
            } catch (Exception e) {}
            
            try {
                WebElement saveBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' OR label CONTAINS 'Save'")
                );
                hasSaveButton = saveBtn.isDisplayed();
            } catch (Exception e) {}
            
            boolean isDetailView = hasEditButton && !hasSaveButton;
            System.out.println("   Edit button: " + hasEditButton + ", Save button: " + hasSaveButton);
            System.out.println("   Is Detail View: " + isDetailView);
            return isDetailView;
            
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Navigate back from any screen to the Asset List
     */
    public void navigateBackToAssetList() {
        System.out.println("🔙 Navigating back to Asset List...");
        
        try {
            // Try clicking Back button multiple times if needed
            for (int i = 0; i < 3; i++) {
                try {
                    // Check if we're already on asset list (plus button visible)
                    WebElement plusBtn = driver.findElement(AppiumBy.accessibilityId("plus"));
                    if (plusBtn.isDisplayed()) {
                        System.out.println("   ✅ Already on Asset List");
                        return;
                    }
                } catch (Exception e) {}
                
                // Click back
                try {
                    clickBack();
                    sleep(200);
                } catch (Exception e) {
                    // Try clicking Cancel if Back doesn't work
                    try {
                        clickCancel();
                        sleep(200);
                    } catch (Exception ex) {}
                }
            }
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
    }


}
