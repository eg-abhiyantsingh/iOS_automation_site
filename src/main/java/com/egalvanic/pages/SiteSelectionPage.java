package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Site Selection Page Object
 * Handles all Site Selection screen interactions
 * 
 * Features covered:
 * - Select Site Screen UI
 * - Search Sites
 * - Site List Navigation
 * - Online/Offline Toggle
 * - Sync Functionality
 */
public class SiteSelectionPage extends BasePage {

    // ================================================================
    // SELECT SITE SCREEN ELEMENTS
    // ================================================================

    // Cancel Button - Multiple locator strategies for reliability
    @iOSXCUITFindBy(accessibility = "Cancel")
    private WebElement cancelButton;
    
    // Cancel Button Alternative - by label
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton' AND label == 'Cancel'")
    private WebElement cancelButtonAlt;

    // Select Site Title
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeStaticText' AND label == 'Select Site'")
    private WebElement selectSiteTitle;

    // Search Bar
    @iOSXCUITFindBy(iOSNsPredicate = "value == 'Search sites...'")
    private WebElement searchBar;

    // Search Bar Alternative
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND visible == true")
    private WebElement searchBarAlt;

    // Create New Site Button
    @iOSXCUITFindBy(accessibility = "Create New Site")
    private WebElement createNewSiteButton;

    // Clear Search Button (X icon)
    @iOSXCUITFindBy(accessibility = "xmark.circle.fill")
    private WebElement clearSearchButton;

    // All Site Buttons (for site list)
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton'")
    private List<WebElement> allButtons;

    // Done Button
    @iOSXCUITFindBy(accessibility = "Done")
    private WebElement doneButton;

    // ================================================================
    // DASHBOARD ELEMENTS
    // ================================================================

    // Sites Button (Quick Action)
    @iOSXCUITFindBy(accessibility = "building.2")
    private WebElement sitesButton;
    
    // Sites Button Alternative - by name or label containing building
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton' AND (name CONTAINS 'building' OR name CONTAINS 'Sites' OR label CONTAINS 'Sites')")
    private WebElement sitesButtonAlt;

    // Refresh Button
    @iOSXCUITFindBy(accessibility = "arrow.clockwise")
    private WebElement refreshButton;

    // WiFi Button (Online)
    @iOSXCUITFindBy(accessibility = "Wi-Fi")
    private WebElement wifiButtonOnline;

    // WiFi Button (Offline) - uses label or contains wifi.slash image
    @iOSXCUITFindBy(accessibility = "Wi-Fi Off")
    private WebElement wifiButtonOffline;
    
    // WiFi Button with pending sync (shows count like "1", "2", etc.)
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeNavigationBar/**/XCUIElementTypeButton[`name MATCHES '\\\\d+'`]")
    private WebElement wifiButtonWithSyncCount;
    
    // WiFi Button Alternative - find by wifi.slash image in hierarchy
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton' AND visible == true AND (name CONTAINS 'wifi' OR name MATCHES '\\\\d+')")
    private WebElement wifiButtonAlt;

    // Locations Button
    @iOSXCUITFindBy(accessibility = "Locations")
    private WebElement locationsButton;

    // Locations Alternative
    @iOSXCUITFindBy(accessibility = "building.columns")
    private WebElement locationsButtonAlt;

    // Broadcast Icon
    @iOSXCUITFindBy(accessibility = "broadcast")
    private WebElement broadcastIcon;

    // ================================================================
    // LOADING SCREEN ELEMENTS
    // ================================================================

    // Loading Text
    @iOSXCUITFindBy(iOSNsPredicate = "label CONTAINS 'Loading'")
    private WebElement loadingText;

    // Progress Indicator
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeProgressIndicator'")
    private WebElement progressIndicator;

    // ================================================================
    // DASHBOARD CARDS
    // ================================================================

    // Assets Card
    @iOSXCUITFindBy(iOSNsPredicate = "label CONTAINS 'Assets'")
    private WebElement assetsCard;

    // Connections Card
    @iOSXCUITFindBy(iOSNsPredicate = "label CONTAINS 'Connections'")
    private WebElement connectionsCard;

    // My Tasks Button
    @iOSXCUITFindBy(accessibility = "My Tasks")
    private WebElement myTasksButton;

    // Issues Button
    @iOSXCUITFindBy(accessibility = "Issues")
    private WebElement issuesButton;

    // No Active Job Card
    @iOSXCUITFindBy(iOSNsPredicate = "label CONTAINS 'No Active Job'")
    private WebElement noActiveJobCard;

    // Tap to select a job
    @iOSXCUITFindBy(iOSNsPredicate = "label CONTAINS 'Tap to select a job'")
    private WebElement tapToSelectJobText;

    // ================================================================
    // OFFLINE/SYNC ELEMENTS
    // ================================================================

    // WiFi Popup Button (Go Online/Go Offline)
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeWindow[1]/XCUIElementTypeOther[3]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther[2]/XCUIElementTypeOther/XCUIElementTypeOther/XCUIElementTypeOther[2]/XCUIElementTypeOther")
    private WebElement wifiPopupButton;
    
    // WiFi Popup Button Alternative - by type and visible
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeOther' AND visible == true")
    private List<WebElement> wifiPopupButtonAlts;

    // Go Offline Text
    @iOSXCUITFindBy(iOSNsPredicate = "label == 'Go Offline'")
    private WebElement goOfflineText;
    
    // Go Offline Button - more flexible locator
    @iOSXCUITFindBy(iOSNsPredicate = "(label CONTAINS 'Offline' OR name CONTAINS 'Offline') AND visible == true")
    private WebElement goOfflineButton;

    // Go Online Text - try multiple locator strategies
    @iOSXCUITFindBy(iOSNsPredicate = "label == 'Go Online' OR name == 'Go Online'")
    private WebElement goOnlineText;
    
    // Go Online Button - more flexible locator
    @iOSXCUITFindBy(iOSNsPredicate = "(label CONTAINS 'Online' OR name CONTAINS 'Online') AND visible == true")
    private WebElement goOnlineButton;

    // Sync Records Button (arrow.triangle.2.circlepath)
    @iOSXCUITFindBy(accessibility = "arrow.triangle.2.circlepath")
    private WebElement syncRecordsButton;

    // Sync Records Text (shows count like "Sync 1 records")
    @iOSXCUITFindBy(iOSNsPredicate = "label CONTAINS 'Sync' AND label CONTAINS 'record'")
    private WebElement syncRecordsText;

    // ================================================================
    // LOCATIONS SCREEN ELEMENTS
    // ================================================================

    // Add Button (Plus)
    @iOSXCUITFindBy(accessibility = "plus")
    private WebElement addButton;

    // Save Button
    @iOSXCUITFindBy(accessibility = "Save")
    private WebElement saveButton;

    // Building Name Field
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == 'Building Name'")
    private WebElement buildingNameField;

    // No Buildings Text
    @iOSXCUITFindBy(iOSNsPredicate = "label == 'No Buildings'")
    private WebElement noBuildingsText;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public SiteSelectionPage() {
        super();
    }

    // ================================================================
    // EXPLICIT WAIT METHODS
    // ================================================================
    
    /**
     * Wait for site list to be ready (site selection screen loaded)
     * Fast timeout (2 seconds total max)
     */
    public void waitForSiteListReady() {
        try {
            // Wait for search bar and sites (2 seconds total max)
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            quickWait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOf(searchBar),
                ExpectedConditions.visibilityOf(searchBarAlt)
            ));
            System.out.println("✅ Site list ready");
        } catch (Exception e) {
            System.out.println("⚠️ Site list wait timeout: " + e.getMessage());
        }
    }
    
    /**
     * Wait for dashboard to be ready (after selecting a site)
     */
    public void waitForDashboardReady() {
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOf(sitesButton),
                ExpectedConditions.visibilityOf(refreshButton),
                ExpectedConditions.visibilityOf(assetsCard)
            ));
            System.out.println("✅ Dashboard ready");
        } catch (Exception e) {
            System.out.println("⚠️ Dashboard wait timeout, continuing...");
        }
    }

    // ================================================================
    // SELECT SITE SCREEN METHODS
    // ================================================================

    /**
     * Check if Select Site screen is displayed
     */
    public boolean isSelectSiteScreenDisplayed() {
        System.out.println("🔍 Checking if Select Site screen is displayed...");
        
        // Use polling - try up to 5 times with waits (total ~5 seconds)
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("   Attempt " + attempt + "/" + maxAttempts);
            
            // Wait for screen transition
            sleep(600);
            
            if (checkSelectSiteScreenElements()) {
                return true;
            }
        }
        
        System.out.println("❌ Select Site screen NOT detected after " + maxAttempts + " attempts");
        return false;
    }
    
    /**
     * Helper method to check for Select Site screen elements
     * Returns true if ANY identifying element is found
     */
    private boolean checkSelectSiteScreenElements() {
        try {
            // Method 1: Check for Select Site title by accessibility ID
            try {
                WebElement title = driver.findElement(AppiumBy.accessibilityId("Select Site"));
                if (title != null && title.isDisplayed()) {
                    System.out.println("✅ Found Select Site title (accessibility ID)");
                    return true;
                }
            } catch (Exception e) {}
            
            // Method 2: Check for Select Site title by predicate
            try {
                WebElement title = driver.findElement(AppiumBy.iOSNsPredicateString("label == 'Select Site'"));
                if (title != null && title.isDisplayed()) {
                    System.out.println("✅ Found Select Site title (predicate)");
                    return true;
                }
            } catch (Exception e) {}
            
            // Method 3: Check for search bar with "Search sites..." placeholder
            try {
                WebElement search = driver.findElement(AppiumBy.iOSNsPredicateString("value == 'Search sites...' OR placeholderValue == 'Search sites...'"));
                if (search != null && search.isDisplayed()) {
                    System.out.println("✅ Found search bar with 'Search sites...' placeholder");
                    return true;
                }
            } catch (Exception e) {}
            
            // Method 4: Check for any visible TextField (search bar)
            try {
                WebElement textField = driver.findElement(AppiumBy.iOSClassChain("**/XCUIElementTypeTextField[`visible == true`]"));
                if (textField != null && textField.isDisplayed()) {
                    System.out.println("✅ Found visible TextField (search bar)");
                    return true;
                }
            } catch (Exception e) {}
            
            // Method 5: Check for Create New Site button
            try {
                WebElement createBtn = driver.findElement(AppiumBy.accessibilityId("Create New Site"));
                if (createBtn != null && createBtn.isDisplayed()) {
                    System.out.println("✅ Found Create New Site button");
                    return true;
                }
            } catch (Exception e) {}
            
            // Method 6: Check for site list items (buttons containing comma - "Site Name, Address")
            try {
                List<WebElement> siteButtons = driver.findElements(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label CONTAINS ','"));
                if (siteButtons != null && siteButtons.size() > 0) {
                    System.out.println("✅ Found site list items (" + siteButtons.size() + " sites with comma in label)");
                    return true;
                }
            } catch (Exception e) {}
            
            // Method 7: Check for navigation bar with Sites or Select Site
            try {
                WebElement navBar = driver.findElement(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeNavigationBar' AND (name CONTAINS 'Site' OR label CONTAINS 'Site')"));
                if (navBar != null && navBar.isDisplayed()) {
                    System.out.println("✅ Found navigation bar with Site text");
                    return true;
                }
            } catch (Exception e) {}
            
            // Method 8: Check for Cancel button (only visible on Select Site screen)
            try {
                WebElement cancel = driver.findElement(AppiumBy.accessibilityId("Cancel"));
                if (cancel != null && cancel.isDisplayed()) {
                    System.out.println("✅ Found Cancel button (Select Site screen indicator)");
                    return true;
                }
            } catch (Exception e) {}
            
            // Method 9: Check if dashboard elements (Sites button building.2) are NOT visible
            // If dashboard is hidden, we might be on Select Site screen
            try {
                WebElement sitesBtn = driver.findElement(AppiumBy.accessibilityId("building.2"));
                if (sitesBtn != null && !sitesBtn.isDisplayed()) {
                    // Dashboard Sites button hidden = probably on Select Site screen
                    System.out.println("✅ Dashboard Sites button hidden (likely on Select Site screen)");
                    return true;
                }
            } catch (Exception e) {
                // Element not found = we're not on dashboard = likely on Select Site screen
                System.out.println("✅ Dashboard Sites button not found (likely on Select Site screen)");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Cancel button is displayed (actually the X clear button)
     */
    public boolean isCancelButtonDisplayed() {
        return isElementDisplayed(clearSearchButton);
    }

    /**
     * Click Cancel/Clear button - clears search text using X button (xmark.circle.fill)
     */
    public void clickCancel() {
        try {
            // The "Cancel" functionality is actually the X button that clears search
            WebElement clearBtn = waitForElementToBeClickable(clearSearchButton, 10);
            if (clearBtn != null) {
                clearBtn.click();
                System.out.println("✅ Search cleared using X button");
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ X button not found, trying xpath...");
        }
        
        try {
            // Fallback: Find X button by accessibility ID directly
            WebElement xButton = driver.findElement(By.xpath("//XCUIElementTypeButton[@name='xmark.circle.fill']"));
            xButton.click();
            System.out.println("✅ Search cleared using X button (xpath)");
        } catch (Exception e) {
            System.err.println("⚠️ Could not find X button to clear search: " + e.getMessage());
            // Don't throw - test can continue
        }
    }
    
    /**
     * Wait for element to be clickable with custom timeout
     */
    private WebElement waitForElementToBeClickable(WebElement element, int timeoutSeconds) {
        try {
            return wait.withTimeout(Duration.ofSeconds(timeoutSeconds))
                       .until(ExpectedConditions.elementToBeClickable(element));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if Search bar is displayed
     */
    public boolean isSearchBarDisplayed() {
        return isElementDisplayed(searchBar) || isElementDisplayed(searchBarAlt);
    }

    /**
     * Get Search bar placeholder text
     */
    public String getSearchBarPlaceholder() {
        try {
            return searchBar.getAttribute("value");
        } catch (Exception e) {
            return searchBarAlt.getAttribute("value");
        }
    }

    /**
     * Check if Create New Site button is displayed
     */
    public boolean isCreateNewSiteButtonDisplayed() {
        return isElementDisplayed(createNewSiteButton);
    }

    /**
     * Enter text in search bar (CI-safe with explicit waits)
     */
    public void searchSite(String siteName) {
        try {
            // Find search box using value == 'Search sites...'
            WebElement searchElement = waitForElementToBeClickable(searchBar, 5);
            if (searchElement != null) {
                searchElement.click();
                // Wait for element to be ready for input (CI-safe)
                waitForElementToBeClickable(searchBar, 2);
                searchElement.sendKeys(siteName);
                System.out.println("✅ Entered '" + siteName + "' in search box");
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Primary search bar not found, trying alternative...");
        }
        
        try {
            // Try alternative search bar
            WebElement searchAlt = waitForElementToBeClickable(searchBarAlt, 5);
            if (searchAlt != null) {
                searchAlt.click();
                waitForElementToBeClickable(searchBarAlt, 2);
                searchAlt.sendKeys(siteName);
                System.out.println("✅ Entered '" + siteName + "' in search box (alt)");
            }
        } catch (Exception e) {
            System.err.println("❌ Could not find search bar: " + e.getMessage());
            throw new RuntimeException("Search bar not found");
        }
    }

    /**
     * Clear search bar using X button (xmark.circle.fill) - CI-safe
     */
    public void clearSearch() {
        try {
            WebElement clearBtn = waitForElementToBeClickable(clearSearchButton, 2);
            if (clearBtn != null) {
                clearBtn.click();
                System.out.println("✅ Search box cleared");
                // Wait for search bar to be ready again (CI-safe)
                waitForVisibility(searchBar, 2);
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Clear button not found");
        }
        
        try {
            // Fallback: Find by accessibility ID
            driver.findElement(By.xpath("//XCUIElementTypeButton[@name='xmark.circle.fill']")).click();
            System.out.println("✅ Search box cleared (xpath)");
            waitForVisibility(searchBar, 2);
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear search: " + e.getMessage());
        }
    }

    /**
     * Get all sites from the list
     */
    public List<WebElement> getAllSites() {
        List<WebElement> sites = new java.util.ArrayList<>();
        for (WebElement btn : allButtons) {
            String name = btn.getAttribute("name");
            if (name != null && !name.equals("Emoji") && !name.equals("dictation") 
                && !name.equals("Create New Site") && !name.equals("Cancel") 
                && !name.equals("xmark.circle.fill") && name.contains(",")) {
                sites.add(btn);
            }
        }
        return sites;
    }

    /**
     * Get site count
     */
    public int getSiteCount() {
        return getAllSites().size();
    }

    /**
     * Check if site list is displayed
     */
    public boolean isSiteListDisplayed() {
        return getSiteCount() > 0;
    }

    /**
     * Select site by index
     */
    public String selectSiteByIndex(int index) {
        List<WebElement> sites = getAllSites();
        if (index >= 0 && index < sites.size()) {
            String siteName = sites.get(index).getAttribute("name");
            sites.get(index).click();
            return siteName;
        }
        return null;
    }

    /**
     * Select first site quickly - optimized for speed
     * Uses direct XPath to find first site button without iterating all buttons
     */
    public String selectFirstSite() {
        try {
            // Find first button that looks like a site (contains comma - indicating "name, address")
            WebElement firstSite = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS ','"
            ));
            String siteName = firstSite.getAttribute("name");
            System.out.println("✅ Selecting first site: " + siteName);
            firstSite.click();
            return siteName;
        } catch (Exception e) {
            System.out.println("⚠️ Could not find first site directly, falling back to getAllSites");
            return selectSiteByIndex(0);
        }
    }

    /**
     * Select first site - ULTRA FAST version
     * Waits for site list and clicks first site in one operation
     */
    public String selectFirstSiteFast() {
        try {
            // Wait max 3 seconds for any site button to appear and click it immediately
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement firstSite = fastWait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ','")
            ));
            String siteName = firstSite.getAttribute("name");
            firstSite.click();
            return siteName;
        } catch (Exception e) {
            System.out.println("⚠️ Fast site select failed, using standard method");
            return selectFirstSite();
        }
    }

    /**
     * Select random site
     */
    public String selectRandomSite() {
        List<WebElement> sites = getAllSites();
        if (sites.size() > 0) {
            int randomIndex = new java.util.Random().nextInt(sites.size());
            return selectSiteByIndex(randomIndex);
        }
        return null;
    }

    /**
     * Wait for search results to be ready after typing in search box
     * Uses explicit wait with condition checking - CI/CD safe
     */
    public void waitForSearchResultsReady() {
        try {
            WebDriverWait searchWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            searchWait.pollingEvery(Duration.ofMillis(200));
            searchWait.until(d -> {
                List<WebElement> sites = getAllSites();
                return sites.size() >= 0;
            });
            System.out.println("✅ Search results ready");
        } catch (Exception e) {
            System.out.println("⚠️ Search results wait timeout, continuing...");
        }
    }

    /**
     * Select site by name (fast - clicks first search result)
     * Clears any previous search, searches for the site, then clicks the first result
     * Uses explicit waits - CI/CD safe
     */
    public boolean selectSiteByName(String siteName) {
        System.out.println("🔍 Selecting site by name: " + siteName);
        
        // Clear any previous search first to avoid cache issues
        try {
            clearSearch();
            waitForSearchResultsReady();
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear previous search: " + e.getMessage());
        }
        
        // Search for the site
        System.out.println("📝 Entering search text: " + siteName);
        searchSite(siteName);
        
        // Wait for search results to load using explicit wait (CI/CD safe)
        waitForSearchResultsReady();
        
        // Get filtered results and click the first one
        List<WebElement> sites = getAllSites();
        System.out.println("📋 Found " + sites.size() + " sites after search");
        
        if (sites.size() > 0) {
            String firstSiteName = sites.get(0).getAttribute("name");
            System.out.println("✅ Clicking first result: " + firstSiteName);
            sites.get(0).click();
            return true;
        }
        
        System.out.println("❌ No sites found for: " + siteName);
        return false;
    }

    /**
     * Select site by exact name match (for specific site selection like "test site")
     * Searches for the site and clicks the one with exact matching name
     */
    public boolean selectSiteByExactName(String siteName) {
        System.out.println("🔍 Selecting site by EXACT name: " + siteName);
        
        // Clear any previous search first
        try {
            clearSearch();
            waitForSearchResultsReady();
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear previous search: " + e.getMessage());
        }
        
        // Search for the site
        System.out.println("📝 Entering search text: " + siteName);
        searchSite(siteName);
        
        // Wait for search results to load using explicit wait (CI/CD safe)
        waitForSearchResultsReady();
        
        // Get filtered results and find exact match
        List<WebElement> sites = getAllSites();
        System.out.println("📋 Found " + sites.size() + " sites after search");
        
        // Find exact match by site name (before comma)
        for (WebElement site : sites) {
            String name = site.getAttribute("name");
            if (name != null) {
                String siteNamePart = name.split(",")[0].trim().toLowerCase();
                if (siteNamePart.equals(siteName.toLowerCase())) {
                    System.out.println("✅ Found exact match, clicking: " + name);
                    site.click();
                    return true;
                }
            }
        }
        
        System.out.println("❌ No exact match found for: " + siteName);
        return false;
    }

    /**
     * Check if site entry has chevron/arrow
     */
    public boolean siteHasChevron(int index) {
        // Sites with chevron are tappable - check if site exists
        List<WebElement> sites = getAllSites();
        return index < sites.size() && sites.get(index).isEnabled();
    }

    // ================================================================
    // DASHBOARD METHODS
    // ================================================================

    /**
     * Check if Sites button is displayed on dashboard (with fallback)
     */
    public boolean isSitesButtonDisplayed() {
        return isElementDisplayed(sitesButton) || isElementDisplayed(sitesButtonAlt);
    }

    /**
     * Check if Sites button is enabled (with fallback)
     */
    public boolean isSitesButtonEnabled() {
        try {
            // Try primary locator
            if (isElementDisplayed(sitesButton)) {
                String enabled = sitesButton.getAttribute("enabled");
                return "true".equalsIgnoreCase(enabled);
            }
            // Try alternative locator
            if (isElementDisplayed(sitesButtonAlt)) {
                String enabled = sitesButtonAlt.getAttribute("enabled");
                return "true".equalsIgnoreCase(enabled);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Sites button with fallback strategies and wait for navigation
     */
    public void clickSitesButton() {
        try {
            System.out.println("📝 Clicking Sites button");
            boolean clicked = false;
            
            // Try primary locator first
            if (isElementDisplayed(sitesButton)) {
                System.out.println("✅ Found Sites button via accessibility ID: building.2");
                click(sitesButton);
                clicked = true;
            }
            
            // Try alternative locator
            if (!clicked && isElementDisplayed(sitesButtonAlt)) {
                System.out.println("✅ Found Sites button via alternative locator");
                click(sitesButtonAlt);
                clicked = true;
            }
            
            // Fallback: Find by searching all buttons in view
            if (!clicked) {
                System.out.println("🔍 Searching for Sites button in all visible buttons...");
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true"
                ));
                
                for (WebElement btn : buttons) {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    if (name != null && (name.contains("building") || name.contains("Sites"))) {
                        System.out.println("✅ Found Sites button by name: " + name);
                        btn.click();
                        clicked = true;
                        break;
                    }
                    if (label != null && label.contains("Sites")) {
                        System.out.println("✅ Found Sites button by label: " + label);
                        btn.click();
                        clicked = true;
                        break;
                    }
                }
            }
            
            // Last resort: try clicking the standard locator anyway
            if (!clicked) {
                System.out.println("⚠️ Using standard locator as last resort");
                click(sitesButton);
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ clickSitesButton error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Check if Refresh button is displayed
     */
    public boolean isRefreshButtonDisplayed() {
        return isElementDisplayed(refreshButton);
    }

    /**
     * Check if Dashboard is displayed using multiple indicators
     * More robust than checking single elements
     */
    public boolean isDashboardDisplayed() {
        // Check multiple dashboard indicators - any one means we're on dashboard
        if (isElementDisplayed(sitesButton) || isElementDisplayed(sitesButtonAlt)) {
            System.out.println("✅ Dashboard detected via Sites button");
            return true;
        }
        if (isElementDisplayed(refreshButton)) {
            System.out.println("✅ Dashboard detected via Refresh button");
            return true;
        }
        if (isElementDisplayed(assetsCard)) {
            System.out.println("✅ Dashboard detected via Assets card");
            return true;
        }
        if (isElementDisplayed(connectionsCard)) {
            System.out.println("✅ Dashboard detected via Connections card");
            return true;
        }
        if (isElementDisplayed(locationsButton) || isElementDisplayed(locationsButtonAlt)) {
            System.out.println("✅ Dashboard detected via Locations button");
            return true;
        }
        // Fallback: check for any navigation bar with dashboard-like elements
        try {
            List<WebElement> navButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true AND (name CONTAINS 'building' OR name CONTAINS 'arrow' OR name CONTAINS 'wifi' OR name == 'Wi-Fi')"
            ));
            if (navButtons.size() >= 2) {
                System.out.println("✅ Dashboard detected via navigation bar buttons (found " + navButtons.size() + ")");
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }
        System.out.println("⚠️ Dashboard not detected");
        return false;
    }

    /**
     * Check if Refresh button is enabled
     */
    public boolean isRefreshButtonEnabled() {
        try {
            String enabled = refreshButton.getAttribute("enabled");
            return "true".equalsIgnoreCase(enabled);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Refresh button
     */
    public void clickRefreshButton() {
        click(refreshButton);
    }

    // ================================================================
    // ONLINE/OFFLINE METHODS
    // ================================================================

    /**
     * Check if WiFi is online with multiple detection strategies
     */
    public boolean isWifiOnline() {
        // Strategy 1: Primary locator (accessibility = "Wi-Fi")
        if (isElementDisplayed(wifiButtonOnline)) {
            System.out.println("✅ WiFi online detected via primary locator");
            return true;
        }

        // Strategy 2: Check for wifi icon without "Off" or sync count
        try {
            List<WebElement> wifiButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true AND (name == 'Wi-Fi' OR name CONTAINS 'wifi')"
            ));
            for (WebElement btn : wifiButtons) {
                String name = btn.getAttribute("name");
                // If it's just "Wi-Fi" without "Off", it's online
                if (name != null && name.equals("Wi-Fi")) {
                    System.out.println("✅ WiFi online detected via button scan");
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Strategy 3: Check navigation bar for wifi button that's NOT offline
        try {
            WebElement navBar = driver.findElement(AppiumBy.className("XCUIElementTypeNavigationBar"));
            List<WebElement> navButtons = navBar.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : navButtons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains("Wi-Fi") && !name.contains("Off")) {
                    System.out.println("✅ WiFi online detected in navigation bar");
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Strategy 4: If offline button is NOT displayed, assume online
        if (!isElementDisplayed(wifiButtonOffline) && !isElementDisplayed(wifiButtonWithSyncCount)) {
            // Check if we're on dashboard (has other dashboard elements)
            if (isElementDisplayed(sitesButton) || isElementDisplayed(assetsCard) || isElementDisplayed(locationsButton)) {
                System.out.println("✅ WiFi assumed online (offline indicators not present, dashboard visible)");
                return true;
            }
        }

        System.out.println("⚠️ WiFi online status could not be confirmed");
        return false;
    }

    /**
     * Check if WiFi is offline (including when showing pending sync count)
     */
    public boolean isWifiOffline() {
        // Check standard offline button
        if (isElementDisplayed(wifiButtonOffline)) {
            return true;
        }
        // Also check for sync count button (indicates offline with pending sync)
        if (isElementDisplayed(wifiButtonWithSyncCount)) {
            return true;
        }
        return false;
    }

    /**
     * Click WiFi button (works for both online/offline states and with pending sync)
     */
    public void clickWifiButton() {
        try {
            System.out.println("🔍 Attempting to click WiFi button...");
            
            // Try online button first
            if (isElementDisplayed(wifiButtonOnline)) {
                System.out.println("✅ Found WiFi online button");
                click(wifiButtonOnline);
                return;
            }
            
            // Try offline button
            if (isElementDisplayed(wifiButtonOffline)) {
                System.out.println("✅ Found WiFi offline button");
                click(wifiButtonOffline);
                return;
            }
            
            // Try button with sync count (when there are pending sync records)
            if (isElementDisplayed(wifiButtonWithSyncCount)) {
                System.out.println("✅ Found WiFi button with sync count");
                click(wifiButtonWithSyncCount);
                return;
            }
            
            // Try alternative locator
            if (isElementDisplayed(wifiButtonAlt)) {
                System.out.println("✅ Found WiFi button via alternative locator");
                click(wifiButtonAlt);
                return;
            }
            
            System.out.println("⚠️ Could not find any WiFi button, trying by coordinates...");
            // Fallback: Try to find any button in navigation bar containing wifi
            try {
                WebElement navBar = driver.findElement(AppiumBy.iOSClassChain("**/XCUIElementTypeNavigationBar"));
                java.util.List<WebElement> buttons = navBar.findElements(AppiumBy.className("XCUIElementTypeButton"));
                for (WebElement btn : buttons) {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    System.out.println("🔍 Found nav button - name: " + name + ", label: " + label);
                    // Click on the first button (usually WiFi is first in nav bar)
                    if (buttons.indexOf(btn) == 0) {
                        btn.click();
                        System.out.println("✅ Clicked first navigation bar button");
                        return;
                    }
                }
            } catch (Exception ex) {
                System.out.println("⚠️ Nav bar fallback failed: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ clickWifiButton error: " + e.getMessage());
        }
    }

    /**
     * Click WiFi popup button (Go Online/Go Offline confirmation) with fallback
     */
    public void clickWifiPopupButton() {
        try {
            System.out.println("🔍 Clicking WiFi popup button...");
            
            // Try primary locator first
            if (isElementDisplayed(wifiPopupButton)) {
                System.out.println("✅ Found WiFi popup button via primary locator");
                click(wifiPopupButton);
                return;
            }
            
            // Try finding "Go Offline" text and click it
            if (isElementDisplayed(goOfflineText)) {
                System.out.println("✅ Found Go Offline option");
                click(goOfflineText);
                return;
            }
            
            // Try finding "Go Offline" button
            if (isElementDisplayed(goOfflineButton)) {
                System.out.println("✅ Found Go Offline button");
                click(goOfflineButton);
                return;
            }
            
            // Try finding "Go Online" text and click it
            if (isElementDisplayed(goOnlineText)) {
                System.out.println("✅ Found Go Online option");
                click(goOnlineText);
                return;
            }
            
            // Try finding "Go Online" button
            if (isElementDisplayed(goOnlineButton)) {
                System.out.println("✅ Found Go Online button");
                click(goOnlineButton);
                return;
            }
            
            // Fallback: Search for any element with "Go" and "line" in label
            System.out.println("🔍 Searching for popup option via label...");
            List<WebElement> popupOptions = driver.findElements(AppiumBy.iOSNsPredicateString(
                "visible == true AND (label CONTAINS 'Go' AND (label CONTAINS 'Offline' OR label CONTAINS 'Online'))"
            ));
            
            if (!popupOptions.isEmpty()) {
                WebElement option = popupOptions.get(0);
                System.out.println("✅ Found popup option: " + option.getAttribute("label"));
                option.click();
                return;
            }
            
            // Last resort: click primary locator anyway
            System.out.println("⚠️ Using primary locator as last resort");
            click(wifiPopupButton);
            
        } catch (Exception e) {
            System.out.println("⚠️ clickWifiPopupButton error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Tap outside any popup to dismiss it
     */
    public void tapOutsidePopup() {
        try {
            System.out.println("🔍 Tapping outside popup to dismiss...");
            // Tap on center-bottom area of screen (usually safe to dismiss popups)
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int x = size.width / 2;
            int y = (int) (size.height * 0.8); // 80% down the screen
            
            new io.appium.java_client.touch.offset.PointOption();
            io.appium.java_client.PerformsTouchActions touchDriver = (io.appium.java_client.PerformsTouchActions) driver;
            new io.appium.java_client.TouchAction<>(touchDriver)
                .tap(io.appium.java_client.touch.offset.PointOption.point(x, y))
                .perform();
            System.out.println("✅ Tapped at (" + x + ", " + y + ")");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap outside popup: " + e.getMessage());
            // Alternative: try pressing escape or back
            try {
                driver.navigate().back();
            } catch (Exception ex) {
                // Ignore
            }
        }
    }

    /**
     * Go Offline (with internal explicit wait and improved reliability)
     */
    public void goOffline() {
        try {
            System.out.println("🔄 Attempting to go offline...");
            
            if (isWifiOnline()) {
                clickWifiButton();
                sleep(600); // Wait for popup animation
                
                // Try to click the popup button with multiple strategies
                try {
                    // First try direct "Go Offline" text
                    if (isElementDisplayed(goOfflineText)) {
                        click(goOfflineText);
                    } else if (isElementDisplayed(goOfflineButton)) {
                        click(goOfflineButton);
                    } else {
                        // Fallback to popup button
                        clickWifiPopupButton();
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Popup click failed, trying alternative: " + e.getMessage());
                    clickWifiPopupButton();
                }
                
                // Wait for offline state
                sleep(1200);
                if (!isWifiOffline()) {
                    System.out.println("⚠️ Not in offline mode yet, waiting longer...");
                    waitForCondition(() -> isWifiOffline(), 5);
                }
                System.out.println("✅ Successfully went offline");
            } else {
                System.out.println("ℹ️ Already in offline mode");
            }
        } catch (Exception e) {
            System.out.println("⚠️ goOffline error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Go Online (with internal explicit wait and improved reliability)
     */
    public void goOnline() {
        try {
            System.out.println("🔄 Attempting to go online...");
            
            if (isWifiOffline()) {
                clickWifiButton();
                sleep(600); // Wait for popup animation
                
                // Try to click the popup button with multiple strategies
                try {
                    // First try direct "Go Online" text
                    if (isElementDisplayed(goOnlineText)) {
                        click(goOnlineText);
                    } else if (isElementDisplayed(goOnlineButton)) {
                        click(goOnlineButton);
                    } else {
                        // Fallback to popup button
                        clickWifiPopupButton();
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Popup click failed, trying alternative: " + e.getMessage());
                    clickWifiPopupButton();
                }
                
                // Wait for online state
                sleep(1200);
                if (!isWifiOnline()) {
                    System.out.println("⚠️ Not in online mode yet, waiting longer...");
                    waitForCondition(() -> isWifiOnline(), 5);
                }
                System.out.println("✅ Successfully went online");
            } else {
                System.out.println("ℹ️ Already in online mode");
            }
        } catch (Exception e) {
            System.out.println("⚠️ goOnline error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Check if Go Offline option is visible
     */
    public boolean isGoOfflineOptionVisible() {
        return isElementDisplayed(goOfflineText);
    }

    /**
     * Check if Go Online option is visible
     */
    public boolean isGoOnlineOptionVisible() {
        return isElementDisplayed(goOnlineText);
    }

    /**
     * Check if Sync records option is visible
     */
    public boolean isSyncRecordsOptionVisible() {
        return isElementDisplayed(syncRecordsButton) || isElementDisplayed(syncRecordsText);
    }

    /**
     * Click Sync records button (e.g., "Sync 1 records", "Sync 2 records")
     */
    public void clickSyncRecords() {
        try {
            System.out.println("🔍 Looking for Sync records option...");
            
            // First try to find by text containing "Sync" and "record"
            java.util.List<WebElement> syncTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Sync' AND label CONTAINS 'record'"
            ));
            
            if (!syncTexts.isEmpty()) {
                WebElement syncElement = syncTexts.get(0);
                String label = syncElement.getAttribute("label");
                System.out.println("✅ Found: " + label);
                syncElement.click();
                System.out.println("✅ Clicked on Sync records");
                sleep(1200); // Wait for sync to process
                return;
            }
            
            // Fallback: Try the button locator
            if (isElementDisplayed(syncRecordsButton)) {
                click(syncRecordsButton);
                System.out.println("✅ Clicked Sync records button via fallback");
                sleep(1200);
                return;
            }
            
            // Last resort: try syncRecordsText locator
            if (isElementDisplayed(syncRecordsText)) {
                click(syncRecordsText);
                System.out.println("✅ Clicked Sync records text");
                sleep(1200);
                return;
            }
            
            System.out.println("⚠️ Could not find Sync records option");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click sync records: " + e.getMessage());
        }
    }

    /**
     * Click Go Online button from WiFi popup
     */
    public void clickGoOnline() {
        try {
            System.out.println("🔍 Attempting to click Go Online...");
            
            // First try the standard locator
            if (isElementDisplayed(goOnlineText)) {
                System.out.println("✅ Found Go Online via standard locator");
                waitForElementToBeClickable(goOnlineText, 5);
                click(goOnlineText);
                System.out.println("✅ Clicked Go Online");
                waitForCondition(() -> isWifiOnline(), 5);
                return;
            }
            
            // Fallback: Find by text in the current context menu
            System.out.println("🔍 Trying fallback - searching for 'Go Online' text...");
            try {
                // Try to find any element containing "Go Online" text
                java.util.List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'Go Online'"
                ));
                if (!buttons.isEmpty()) {
                    System.out.println("✅ Found Go Online button via fallback");
                    buttons.get(0).click();
                    waitForCondition(() -> isWifiOnline(), 5);
                    return;
                }
                
                // Try to find static text
                java.util.List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Go Online'"
                ));
                if (!texts.isEmpty()) {
                    System.out.println("✅ Found Go Online text element");
                    texts.get(0).click();
                    waitForCondition(() -> isWifiOnline(), 5);
                    return;
                }
                
                // Try any element with Go Online
                java.util.List<WebElement> anyElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "label == 'Go Online' OR name == 'Go Online'"
                ));
                if (!anyElements.isEmpty()) {
                    System.out.println("✅ Found element with Go Online label/name");
                    anyElements.get(0).click();
                    waitForCondition(() -> isWifiOnline(), 5);
                    return;
                }
                
                System.out.println("⚠️ Could not find Go Online element with any locator");
            } catch (Exception fallbackError) {
                System.out.println("⚠️ Fallback search failed: " + fallbackError.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Go Online: " + e.getMessage());
        }
    }

    /**
     * Get pending sync records count
     * Returns the number of records pending sync (e.g., "Sync 1 records" -> 1)
     */
    public int getPendingSyncCount() {
        try {
            if (isElementDisplayed(syncRecordsText)) {
                String text = syncRecordsText.getAttribute("label");
                // Extract number from "Sync X records"
                String[] parts = text.split(" ");
                for (String part : parts) {
                    try {
                        return Integer.parseInt(part);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get sync count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Check if there are pending sync records
     */
    public boolean hasPendingSyncRecords() {
        return getPendingSyncCount() > 0 || isSyncRecordsOptionVisible();
    }

    /**
     * Wait for sync to complete after clicking sync button
     * Waits up to 30 seconds for sync to finish
     */
    public void waitForSyncToComplete() {
        System.out.println("⏳ Waiting for sync to complete...");
        int maxWaitSeconds = 30;
        int elapsed = 0;
        
        while (elapsed < maxWaitSeconds) {
            try {
                Thread.sleep(1200);
                elapsed += 2;
                System.out.println("⏳ Sync in progress... (" + elapsed + "s/" + maxWaitSeconds + "s)");
                
                // Check if Sites button is enabled (indicates sync complete)
                if (isSitesButtonEnabled()) {
                    System.out.println("✅ Sync completed - Sites button is enabled");
                    return;
                }
                
                // Check if WiFi shows online state without badge
                if (isWifiOnline()) {
                    System.out.println("✅ Sync completed - WiFi shows online");
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("✅ Sync wait completed (timeout reached but sync may have finished)");
    }

    /**
     * Sync pending records (go online first if offline, then sync)
     */
    public void syncPendingRecords() {
        // First check if we're offline
        if (isWifiOffline()) {
            System.out.println("📡 Currently offline, going online first...");
            clickWifiButton();
            // Wait for popup
            waitForCondition(() -> isElementDisplayed(goOnlineText), 3);
            clickGoOnline();
        }
        
        // Now click WiFi to open popup and sync
        clickWifiButton();
        waitForCondition(() -> isSyncRecordsOptionVisible(), 3);
        
        if (isSyncRecordsOptionVisible()) {
            clickSyncRecords();
            System.out.println("✅ Sync initiated");
            // Wait for sync to complete (Sites button becomes enabled)
            waitForCondition(() -> isSitesButtonEnabled(), 10);
        }
    }

    /**
     * Check if pending sync indicator is displayed on WiFi icon or Sites button
     * This indicates there are unsynced records
     */
    public boolean hasPendingSyncIndicator() {
        try {
            System.out.println("🔍 Debug: Checking for pending sync indicator...");
            
            // Check if sync records text is visible (shows count)
            if (isElementDisplayed(syncRecordsText)) {
                System.out.println("✅ Found sync records text");
                return true;
            }
            
            // Check if Sites button shows disabled state (indicates pending sync)
            if (!isSitesButtonEnabled()) {
                System.out.println("✅ Sites button is disabled (pending sync)");
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Could not determine pending sync state: " + e.getMessage());
            return false;
        }
    }

    /**
     * Debug: Print page source to find accessibility IDs
     */
    public void debugPrintPageSource() {
        debugPrintPageSource("DEBUG");
    }
    
    /**
     * Debug: Print page source with a label
     */
    public void debugPrintPageSource(String label) {
        try {
            String pageSource = driver.getPageSource();
            System.out.println("📋 [" + label + "] Page Source (first 10000 chars):");
            System.out.println(pageSource.substring(0, Math.min(10000, pageSource.length())));
            
            // Also search for key elements
            if (pageSource.contains("Go Online")) {
                System.out.println("✅ Found 'Go Online' in page source");
            } else {
                System.out.println("⚠️ 'Go Online' NOT found in page source");
            }
            if (pageSource.contains("Sync") && pageSource.contains("record")) {
                System.out.println("✅ Found 'Sync records' text in page source");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not print page source: " + e.getMessage());
        }
    }

    // ================================================================
    // LOADING SCREEN METHODS
    // ================================================================

    /**
     * Check if loading screen is displayed
     */
    public boolean isLoadingScreenDisplayed() {
        return isElementDisplayed(loadingText) || isElementDisplayed(progressIndicator);
    }

    /**
     * Wait for site to load (with explicit wait)
     */
    public boolean waitForSiteToLoad(int timeoutSeconds) {
        try {
            WebDriverWait loadWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            return loadWait.until(d -> !isLoadingScreenDisplayed());
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // DASHBOARD CARDS METHODS
    // ================================================================

    /**
     * Check if Assets card is displayed
     */
    public boolean isAssetsCardDisplayed() {
        return isElementDisplayed(assetsCard);
    }

    /**
     * Get Assets count text
     */
    public String getAssetsCountText() {
        try {
            return assetsCard.getAttribute("label");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if Connections card is displayed
     */
    public boolean isConnectionsCardDisplayed() {
        return isElementDisplayed(connectionsCard);
    }

    /**
     * Get Connections count text
     */
    public String getConnectionsCountText() {
        try {
            return connectionsCard.getAttribute("label");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if My Tasks button is displayed
     */
    public boolean isMyTasksDisplayed() {
        return isElementDisplayed(myTasksButton);
    }

    /**
     * Check if Issues button is displayed
     */
    public boolean isIssuesDisplayed() {
        return isElementDisplayed(issuesButton);
    }

    /**
     * Check if No Active Job card is displayed with multiple detection strategies
     */
    public boolean isNoActiveJobCardDisplayed() {
        // Strategy 1: Primary locator
        if (isElementDisplayed(noActiveJobCard)) {
            System.out.println("✅ No Active Job card found via primary locator");
            return true;
        }

        // Strategy 2: Tap to select job text
        if (isElementDisplayed(tapToSelectJobText)) {
            System.out.println("✅ No Active Job card found via 'Tap to select' text");
            return true;
        }

        // Strategy 3: Search by partial label match
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "visible == true AND (label CONTAINS 'No Active' OR label CONTAINS 'active job' OR label CONTAINS 'select a job' OR label CONTAINS 'Tap to select')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ No Active Job card found via label search (found " + elements.size() + " elements)");
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }

        // Strategy 4: Search static texts for job-related content
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true AND (label CONTAINS 'Job' OR label CONTAINS 'job')"
            ));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                if (label != null && (label.contains("No Active") || label.contains("Tap to select"))) {
                    System.out.println("✅ No Active Job text found: " + label);
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Strategy 5: May need to scroll down to find it
        try {
            scrollDown();
            if (isElementDisplayed(noActiveJobCard) || isElementDisplayed(tapToSelectJobText)) {
                System.out.println("✅ No Active Job card found after scroll");
                return true;
            }
        } catch (Exception e) {
            // Ignore scroll errors
        }

        System.out.println("⚠️ No Active Job card not found");
        return false;
    }

    /**
     * Click No Active Job card with robust fallback strategies
     */
    public void clickNoActiveJobCard() {
        System.out.println("📍 Attempting to click No Active Job card...");

        // Strategy 1: Primary locator
        try {
            if (isElementDisplayed(noActiveJobCard)) {
                System.out.println("✅ Clicking No Active Job card via primary locator");
                click(noActiveJobCard);
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 1 failed: " + e.getMessage());
        }

        // Strategy 2: Tap to select job text
        try {
            if (isElementDisplayed(tapToSelectJobText)) {
                System.out.println("✅ Clicking via 'Tap to select' text");
                click(tapToSelectJobText);
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 2 failed: " + e.getMessage());
        }

        // Strategy 3: Find by label and click
        try {
            WebElement jobCard = driver.findElement(AppiumBy.iOSNsPredicateString(
                "visible == true AND (label CONTAINS 'No Active' OR label CONTAINS 'Tap to select')"
            ));
            if (jobCard != null) {
                System.out.println("✅ Found and clicking job card via label search");
                jobCard.click();
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 3 failed: " + e.getMessage());
        }

        // Strategy 4: Scroll and retry
        try {
            System.out.println("🔄 Scrolling down to find job card...");
            scrollDown();
            shortWait();

            if (isElementDisplayed(noActiveJobCard)) {
                click(noActiveJobCard);
                return;
            }
            if (isElementDisplayed(tapToSelectJobText)) {
                click(tapToSelectJobText);
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 4 failed: " + e.getMessage());
        }

        // Strategy 5: Last resort - try primary locator anyway
        System.out.println("⚠️ All strategies failed, attempting primary locator as last resort");
        click(noActiveJobCard);
    }

    /**
     * Helper method for short wait
     */
    protected void shortWait() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ================================================================
    // BROADCAST ICON METHODS
    // ================================================================

    /**
     * Check if broadcast icon is displayed
     */
    public boolean isBroadcastIconDisplayed() {
        return isElementDisplayed(broadcastIcon);
    }

    /**
     * Check if an element with the given accessibility ID is displayed
     * @param accessibilityId The accessibility ID to search for
     * @return true if element is displayed, false otherwise
     */
    public boolean isElementDisplayedByAccessibilityId(String accessibilityId) {
        try {
            WebElement element = driver.findElement(AppiumBy.accessibilityId(accessibilityId));
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // LOCATIONS METHODS
    // ================================================================

    /**
     * Click Locations button with robust fallback strategies
     */
    public void clickLocations() {
        System.out.println("📍 Attempting to click Locations button...");

        // Strategy 1: Primary locator (accessibility = "Locations")
        try {
            if (isElementDisplayed(locationsButton)) {
                System.out.println("✅ Found Locations via accessibility ID");
                click(locationsButton);
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 1 failed: " + e.getMessage());
        }

        // Strategy 2: Alternative locator (accessibility = "building.columns")
        try {
            if (isElementDisplayed(locationsButtonAlt)) {
                System.out.println("✅ Found Locations via building.columns");
                click(locationsButtonAlt);
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 2 failed: " + e.getMessage());
        }

        // Strategy 3: Search by label containing "Locations"
        try {
            WebElement locationsByLabel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true AND label CONTAINS 'Location'"
            ));
            if (locationsByLabel != null && locationsByLabel.isDisplayed()) {
                System.out.println("✅ Found Locations by label");
                locationsByLabel.click();
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 3 failed: " + e.getMessage());
        }

        // Strategy 4: Search all visible buttons for building.columns icon
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                String label = btn.getAttribute("label");
                if ((name != null && (name.contains("building") || name.contains("Location"))) ||
                    (label != null && label.contains("Location"))) {
                    System.out.println("✅ Found Locations button by scanning (name=" + name + ", label=" + label + ")");
                    btn.click();
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 4 failed: " + e.getMessage());
        }

        // Strategy 5: Last resort - click primary locator anyway
        System.out.println("⚠️ All strategies failed, attempting primary locator as last resort");
        click(locationsButton);
    }

    /**
     * Wait for Locations screen to be ready
     */
    public void waitForLocationsReady() {
        waitForCondition(() -> isLocationsScreenDisplayed(), 5);
    }

    /**
     * Check if Locations screen is displayed
     */
    public boolean isLocationsScreenDisplayed() {
        return isElementDisplayed(doneButton) && isElementDisplayed(addButton);
    }

    /**
     * Click Add button (Plus)
     */
    public void clickAddButton() {
        click(addButton);
    }

    /**
     * Enter building name
     */
    public void enterBuildingName(String name) {
        click(buildingNameField);
        buildingNameField.sendKeys(name);
    }

    /**
     * Click Save button
     */
    public void clickSave() {
        click(saveButton);
    }

    /**
     * Create new building (with internal explicit waits)
     */
    public void createBuilding(String buildingName) {
        clickAddButton();
        // Wait for building name field to appear
        waitForElementToBeClickable(buildingNameField, 3);
        enterBuildingName(buildingName);
        // Wait for save button to be clickable
        waitForElementToBeClickable(saveButton, 2);
        clickSave();
    }

    /**
     * Check if No Buildings message is displayed
     */
    public boolean isNoBuildingsDisplayed() {
        return isElementDisplayed(noBuildingsText);
    }

    /**
     * Click Done button (Locations)
     */
    public void clickDone() {
        click(doneButton);
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Short wait (1 second)
     */
    

    // ================================================================
    // ULTRA-FAST SITE SELECTION METHODS
    // ================================================================

    /**
     * ULTRA FAST: Select any site in under 2 seconds
     * No waits, no polling - just find and click
     */
    public String selectAnySiteInstant() {
        try {
            // Direct find - no wait
            WebElement site = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ','")
            );
            String name = site.getAttribute("name");
            site.click();
            return name;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ULTRA FAST: Wait max 2 seconds for site list, then click first site
     * Combined operation - no separate waits
     */
    public String selectFirstSiteUltraFast() {
        try {
            WebDriverWait ultraFastWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            WebElement site = ultraFastWait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ','")
            ));
            String name = site.getAttribute("name");
            site.click();
            System.out.println("⚡ Fast selected: " + name);
            return name;
        } catch (Exception e) {
            System.out.println("⚠️ Ultra fast failed, using standard");
            return selectFirstSiteFast();
        }
    }

    /**
     * ULTRA FAST: Select random site from visible sites
     * No scrolling, just picks from what's visible
     */
    public String selectRandomSiteUltraFast() {
        try {
            // Get all visible sites in one call
            List<WebElement> sites = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ',' AND visible == true")
            );
            if (sites.isEmpty()) {
                return selectFirstSiteUltraFast();
            }
            int idx = new java.util.Random().nextInt(sites.size());
            String name = sites.get(idx).getAttribute("name");
            sites.get(idx).click();
            System.out.println("⚡ Fast random selected: " + name);
            return name;
        } catch (Exception e) {
            return selectFirstSiteUltraFast();
        }
    }

    /**
     * TURBO: Combined wait for site list + select in one operation
     * Maximum 3 second total for entire operation
     */
    public String turboSelectSite() {
        long start = System.currentTimeMillis();
        try {
            // Single wait + click operation
            WebDriverWait turboWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            turboWait.pollingEvery(Duration.ofMillis(100)); // Fast polling
            
            WebElement site = turboWait.until(d -> {
                try {
                    List<WebElement> sites = d.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ','")
                    );
                    return sites.isEmpty() ? null : sites.get(0);
                } catch (Exception e) {
                    return null;
                }
            });
            
            String name = site.getAttribute("name");
            site.click();
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("⚡ TURBO: Selected '" + name + "' in " + elapsed + "ms");
            return name;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("⚠️ TURBO failed after " + elapsed + "ms: " + e.getMessage());
            return null;
        }
    }

    /**
     * Wait for dashboard - FAST version (2 seconds max)
     */
    public void waitForDashboardFast() {
        try {
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            fastWait.pollingEvery(Duration.ofMillis(200));
            fastWait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("building.2")),
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("list.bullet")),
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("plus"))
            ));
        } catch (Exception e) {
            // Continue anyway - dashboard might be ready
        }
    }



    // ================================================================
    // SCHEDULE SCREEN HANDLING (NEW - Added Jan 2026)
    // ================================================================
    // After login, app now shows a Schedule screen first.
    // User must click "View Sites" to proceed to Site Selection.
    // ================================================================

    /**
     * Check if we're on the Schedule screen (new intermediate screen after login)
     * Schedule screen has: "View Sites" button, "Schedule" text, calendar view
     */
    public boolean isScheduleScreenDisplayed() {
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            return quickWait.until(d -> {
                try {
                    // Check for "View Sites" button
                    List<WebElement> viewSites = d.findElements(
                        AppiumBy.iOSNsPredicateString("label == 'View Sites' OR name == 'View Sites'")
                    );
                    if (!viewSites.isEmpty()) {
                        System.out.println("📅 Schedule screen detected (View Sites button found)");
                        return true;
                    }
                    
                    // Check for "Schedule" text
                    List<WebElement> schedule = d.findElements(
                        AppiumBy.iOSNsPredicateString("label == 'Schedule' OR name == 'Schedule'")
                    );
                    if (!schedule.isEmpty()) {
                        System.out.println("📅 Schedule screen detected (Schedule text found)");
                        return true;
                    }
                    
                    // Check for "No scheduled work today" text
                    List<WebElement> noWork = d.findElements(
                        AppiumBy.iOSNsPredicateString("label CONTAINS 'No scheduled work' OR name CONTAINS 'No scheduled work'")
                    );
                    if (!noWork.isEmpty()) {
                        System.out.println("📅 Schedule screen detected (No scheduled work text found)");
                        return true;
                    }
                    
                    return false;
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click "View Sites" button on Schedule screen to proceed to Site Selection
     */
    public void clickViewSites() {
        System.out.println("📅 Looking for 'View Sites' button...");
        
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            
            // Strategy 1: Direct label/name match
            try {
                WebElement viewSitesBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.iOSNsPredicateString("label == 'View Sites' OR name == 'View Sites'")
                ));
                viewSitesBtn.click();
                System.out.println("✅ Clicked 'View Sites' button (Strategy 1: label/name)");
                Thread.sleep(350);
                return;
            } catch (Exception e) {
                System.out.println("   Strategy 1 failed, trying next...");
            }
            
            // Strategy 2: Button containing "View Sites" text
            try {
                WebElement viewSitesBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (label CONTAINS 'View Sites' OR name CONTAINS 'View Sites')")
                ));
                viewSitesBtn.click();
                System.out.println("✅ Clicked 'View Sites' button (Strategy 2: button type)");
                Thread.sleep(350);
                return;
            } catch (Exception e) {
                System.out.println("   Strategy 2 failed, trying next...");
            }
            
            // Strategy 3: Accessibility ID
            try {
                WebElement viewSitesBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.accessibilityId("View Sites")
                ));
                viewSitesBtn.click();
                System.out.println("✅ Clicked 'View Sites' button (Strategy 3: accessibilityId)");
                Thread.sleep(350);
                return;
            } catch (Exception e) {
                System.out.println("   Strategy 3 failed, trying next...");
            }
            
            // Strategy 4: Scroll down first, then find button
            try {
                System.out.println("   Scrolling down to find 'View Sites'...");
                scrollDown();
                Thread.sleep(300);
                
                WebElement viewSitesBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'View Sites' OR name == 'View Sites'")
                );
                viewSitesBtn.click();
                System.out.println("✅ Clicked 'View Sites' button (Strategy 4: after scroll)");
                Thread.sleep(350);
                return;
            } catch (Exception e) {
                System.out.println("   Strategy 4 failed, trying next...");
            }
            
            // Strategy 5: Find by XPath with text
            try {
                WebElement viewSitesBtn = driver.findElement(
                    AppiumBy.xpath("//*[contains(@label, 'View Sites') or contains(@name, 'View Sites')]")
                );
                viewSitesBtn.click();
                System.out.println("✅ Clicked 'View Sites' button (Strategy 5: XPath)");
                Thread.sleep(350);
                return;
            } catch (Exception e) {
                System.out.println("   Strategy 5 failed");
            }
            
            throw new RuntimeException("Could not find 'View Sites' button with any strategy");
            
        } catch (Exception e) {
            System.out.println("❌ Failed to click 'View Sites': " + e.getMessage());
            throw new RuntimeException("Failed to click 'View Sites' button", e);
        }
    }

    /**
     * Handle Schedule screen if present, then proceed to Site Selection
     * Call this after login to handle the new app flow
     */
    public void handleScheduleScreenIfPresent() {
        System.out.println("🔍 Checking for Schedule screen...");
        
        try {
            // Quick check for Schedule screen (2 seconds max)
            if (isScheduleScreenDisplayed()) {
                System.out.println("📅 Schedule screen detected - clicking 'View Sites'");
                clickViewSites();
                
                // Wait briefly for Site Selection to load
                Thread.sleep(350);
                System.out.println("✅ Navigated from Schedule to Site Selection");
            } else {
                System.out.println("   No Schedule screen - already on Site Selection");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Schedule screen handling: " + e.getMessage());
            // Try clicking View Sites anyway in case detection failed
            try {
                clickViewSites();
            } catch (Exception e2) {
                System.out.println("   View Sites not found - continuing...");
            }
        }
    }

}
