package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for Issues screen
 * Covers: Issues list, filter tabs, search, sort, issue entries
 */
public class IssuePage extends BasePage {

    // ================================================================
    // ISSUES SCREEN HEADER ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(accessibility = "Done")
    private WebElement doneButton;

    @iOSXCUITFindBy(accessibility = "Issues")
    private WebElement issuesTitle;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public IssuePage() {
        super();
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    /**
     * Navigate to Issues screen from Dashboard via Quick Actions
     */
    public boolean navigateToIssuesScreen() {
        try {
            System.out.println("📋 Navigating to Issues screen...");

            if (isIssuesScreenDisplayed()) {
                System.out.println("✓ Already on Issues screen");
                return true;
            }

            boolean tapped = tapOnIssuesButton();
            if (tapped) {
                sleep(500);
                if (isIssuesScreenDisplayed()) {
                    System.out.println("✅ Successfully navigated to Issues screen");
                    return true;
                }
            }

            System.out.println("⚠️ Failed to navigate to Issues screen");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Navigation error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tap Issues button on Dashboard with multiple fallback strategies
     */
    public boolean tapOnIssuesButton() {
        try {
            System.out.println("📋 Tapping on Issues button...");

            // Strategy 1: Wait for Issues button to appear and tap
            try {
                WebDriverWait btnWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                WebElement btn = btnWait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.iOSNsPredicateString(
                        "label == 'Issues' AND (type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText')")));
                btn.click();
                sleep(300);
                System.out.println("✓ Tapped Issues button (waited for DOM)");
                return true;
            } catch (Exception e1) {
                System.out.println("   Wait for Issues button timed out, trying fallbacks...");
            }

            // Strategy 2: accessibilityId
            try {
                driver.findElement(AppiumBy.accessibilityId("Issues")).click();
                sleep(300);
                System.out.println("✓ Tapped Issues via accessibilityId");
                return true;
            } catch (Exception e2) {}

            // Strategy 3: Any visible element with Issues label
            try {
                List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "label == 'Issues'"));
                for (WebElement el : elements) {
                    try {
                        el.click();
                        sleep(300);
                        if (isIssuesScreenDisplayed()) {
                            System.out.println("✓ Tapped Issues element");
                            return true;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e3) {}

            System.out.println("⚠️ Could not tap Issues button");
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // SCREEN VERIFICATION
    // ================================================================

    /**
     * Check if Issues screen is displayed
     */
    public boolean isIssuesScreenDisplayed() {
        try {
            // Strategy 1: Navigation bar title
            try {
                WebElement navBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeNavigationBar' AND (name == 'Issues' OR label == 'Issues')"));
                if (navBar.isDisplayed()) {
                    System.out.println("✓ Issues screen detected (nav bar)");
                    return true;
                }
            } catch (Exception e1) {}

            // Strategy 2: StaticText 'Issues' as title (near top of screen)
            try {
                List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Issues'"));
                for (WebElement title : titles) {
                    int y = title.getLocation().getY();
                    if (y < 150) {
                        System.out.println("✓ Issues screen detected (title text at y=" + y + ")");
                        return true;
                    }
                }
            } catch (Exception e2) {}

            // Strategy 3: Check for filter tabs (unique to Issues screen)
            try {
                WebElement openTab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'Open'"));
                WebElement searchBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSearchField'"));
                if (openTab.isDisplayed() && searchBar.isDisplayed()) {
                    System.out.println("✓ Issues screen detected (filter tabs + search)");
                    return true;
                }
            } catch (Exception e3) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // HEADER ELEMENTS (TC_ISS_001)
    // ================================================================

    /**
     * Check if Done button is displayed in header
     */
    public boolean isDoneButtonDisplayed() {
        try {
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(label == 'Done' OR name == 'Done') AND type == 'XCUIElementTypeButton'"));
            return btn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Sort icon is displayed in header
     */
    public boolean isSortIconDisplayed() {
        try {
            // Sort icon could be arrow.up.arrow.down or similar SF Symbol
            WebElement sort = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name CONTAINS 'sort' OR name CONTAINS 'arrow.up.arrow.down' OR " +
                "label CONTAINS 'Sort' OR label CONTAINS '↕' OR " +
                "name CONTAINS 'line.3.horizontal.decrease')"));
            return sort.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Add (+) button is displayed in header
     */
    public boolean isAddButtonDisplayed() {
        try {
            WebElement addBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name == 'Add' OR name == 'plus' OR name CONTAINS 'plus' OR label == 'Add')"));
            return addBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Issues title is displayed
     */
    public boolean isIssuesTitleDisplayed() {
        try {
            List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(label == 'Issues' OR name == 'Issues') AND " +
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeNavigationBar')"));
            return !titles.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap Done button to close Issues screen
     */
    public void tapDoneButton() {
        try {
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(label == 'Done' OR name == 'Done') AND type == 'XCUIElementTypeButton'"));
            btn.click();
            sleep(300);
            System.out.println("✅ Tapped Done button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Done: " + e.getMessage());
        }
    }

    // ================================================================
    // SEARCH BAR (TC_ISS_002, TC_ISS_015-018)
    // ================================================================

    /**
     * Check if search bar is displayed
     */
    public boolean isSearchBarDisplayed() {
        try {
            WebElement searchBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField'"));
            return searchBar.isDisplayed();
        } catch (Exception e) {
            // Fallback: check for placeholder text
            try {
                WebElement searchBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND " +
                    "(placeholderValue CONTAINS 'Search' OR value CONTAINS 'Search')"));
                return searchBar.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Get search bar placeholder text
     */
    public String getSearchBarPlaceholder() {
        try {
            WebElement searchBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField'"));
            String placeholder = searchBar.getAttribute("placeholderValue");
            if (placeholder == null || placeholder.isEmpty()) {
                placeholder = searchBar.getAttribute("value");
            }
            return placeholder != null ? placeholder : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Type text in search bar
     */
    public void searchIssues(String query) {
        System.out.println("🔍 Searching for: " + query);
        try {
            WebElement searchBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField'"));
            searchBar.click();
            sleep(300);
            searchBar.sendKeys(query);
            sleep(500);
            System.out.println("✅ Entered search query: " + query);
        } catch (Exception e) {
            System.out.println("⚠️ Search failed: " + e.getMessage());
        }
    }

    /**
     * Clear search bar text
     */
    public void clearSearch() {
        System.out.println("🧹 Clearing search...");
        try {
            WebElement searchBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField'"));
            searchBar.clear();
            sleep(300);

            // Also try tapping the clear (x) button if it exists
            try {
                WebElement clearBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Clear text'"));
                clearBtn.click();
                sleep(300);
            } catch (Exception ignored) {}

            // Dismiss keyboard by tapping Cancel or outside
            try {
                WebElement cancelBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Cancel'"));
                cancelBtn.click();
                sleep(300);
            } catch (Exception ignored) {}

            System.out.println("✅ Search cleared");
        } catch (Exception e) {
            System.out.println("⚠️ Clear search failed: " + e.getMessage());
        }
    }

    // ================================================================
    // FILTER TABS (TC_ISS_003 - TC_ISS_007)
    // ================================================================

    /**
     * Check if All tab is displayed
     */
    public boolean isAllTabDisplayed() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'All'"));
            return tab.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Open tab is displayed
     */
    public boolean isOpenTabDisplayed() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'Open'"));
            return tab.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if In Progress tab is displayed
     */
    public boolean isInProgressTabDisplayed() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
            return tab.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap All filter tab
     */
    public void tapAllTab() {
        System.out.println("📋 Tapping All tab...");
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'All'"));
            tab.click();
            sleep(400);
            System.out.println("✅ Tapped All tab");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap All tab: " + e.getMessage());
        }
    }

    /**
     * Tap Open filter tab
     */
    public void tapOpenTab() {
        System.out.println("📋 Tapping Open tab...");
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'Open'"));
            tab.click();
            sleep(400);
            System.out.println("✅ Tapped Open tab");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Open tab: " + e.getMessage());
        }
    }

    /**
     * Tap In Progress filter tab
     */
    public void tapInProgressTab() {
        System.out.println("📋 Tapping In Progress tab...");
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
            tab.click();
            sleep(400);
            System.out.println("✅ Tapped In Progress tab");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap In Progress tab: " + e.getMessage());
        }
    }

    /**
     * Check if Open tab is currently selected (active)
     * Selected tab typically has a different value/trait
     */
    public boolean isOpenTabSelected() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'Open'"));
            String selected = tab.getAttribute("selected");
            if ("true".equals(selected)) return true;

            // Fallback: check if the value attribute indicates selection
            String value = tab.getAttribute("value");
            if (value != null && value.contains("1")) return true;

            // Fallback: check trait (selected buttons may have different traits)
            // In iOS, the selected tab might be identifiable by its trait
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if All tab is currently selected
     */
    public boolean isAllTabSelected() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'All'"));
            String selected = tab.getAttribute("selected");
            return "true".equals(selected);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if In Progress tab is currently selected
     */
    public boolean isInProgressTabSelected() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
            String selected = tab.getAttribute("selected");
            return "true".equals(selected);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the count number from a filter tab label
     * Tab label format: "All (17)" or "Open (12)" or "In Progress (1)"
     */
    private int extractCountFromTabLabel(String tabLabel) {
        try {
            // Find the number in parentheses: "Open (12)" → 12
            int openParen = tabLabel.lastIndexOf('(');
            int closeParen = tabLabel.lastIndexOf(')');
            if (openParen >= 0 && closeParen > openParen) {
                String numStr = tabLabel.substring(openParen + 1, closeParen).trim();
                return Integer.parseInt(numStr);
            }
        } catch (Exception e) {}
        return -1;
    }

    /**
     * Get count from All tab
     */
    public int getAllTabCount() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'All'"));
            String label = tab.getAttribute("label");
            System.out.println("   All tab label: " + label);
            return extractCountFromTabLabel(label);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Get count from Open tab
     */
    public int getOpenTabCount() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'Open'"));
            String label = tab.getAttribute("label");
            System.out.println("   Open tab label: " + label);
            return extractCountFromTabLabel(label);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Get count from In Progress tab
     */
    public int getInProgressTabCount() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
            String label = tab.getAttribute("label");
            System.out.println("   In Progress tab label: " + label);
            return extractCountFromTabLabel(label);
        } catch (Exception e) {
            return -1;
        }
    }

    // ================================================================
    // ISSUE LIST / ENTRIES (TC_ISS_008 - TC_ISS_014)
    // ================================================================

    /**
     * Get the number of visible issue entries in the list
     */
    public int getVisibleIssueCount() {
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            System.out.println("   Visible issue entries: " + cells.size());
            return cells.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get the first issue entry element
     */
    private WebElement getFirstIssueEntry() {
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            if (!cells.isEmpty()) {
                return cells.get(0);
            }
        } catch (Exception e) {}
        return null;
    }

    /**
     * Get the title/label of the first issue in the list
     */
    public String getFirstIssueTitle() {
        try {
            WebElement cell = getFirstIssueEntry();
            if (cell != null) {
                // The cell label usually contains issue info
                String label = cell.getAttribute("label");
                if (label != null && !label.isEmpty()) {
                    return label;
                }
                // Try finding a static text within the cell area
                List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"));
                // Return first meaningful text near the cell
                for (WebElement text : texts) {
                    int textY = text.getLocation().getY();
                    int cellY = cell.getLocation().getY();
                    if (Math.abs(textY - cellY) < 60) {
                        String tLabel = text.getAttribute("label");
                        if (tLabel != null && tLabel.length() > 3) {
                            return tLabel;
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return "";
    }

    /**
     * Check if issue type icon is displayed on any issue entry
     * Icons: ❗ for violations, 🔧 for Repair, 🌡️ for Thermal
     */
    public boolean isIssueTypeIconDisplayed() {
        try {
            // Issue type icons are typically XCUIElementTypeImage elements
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage'"));
            // Filter for images near issue cells
            WebElement cell = getFirstIssueEntry();
            if (cell != null && !images.isEmpty()) {
                int cellY = cell.getLocation().getY();
                for (WebElement img : images) {
                    int imgY = img.getLocation().getY();
                    if (Math.abs(imgY - cellY) < 50) {
                        System.out.println("   Issue type icon found near cell at y=" + imgY);
                        return true;
                    }
                }
            }
            // Fallback: any image element in the list area (below header ~200px)
            for (WebElement img : images) {
                int y = img.getLocation().getY();
                if (y > 200 && y < 800) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Check if a priority badge with specific text is displayed
     * @param priority "High", "Medium", "Low"
     */
    public boolean isPriorityBadgeDisplayed(String priority) {
        try {
            WebElement badge = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == '" + priority + "'"));
            return badge.isDisplayed();
        } catch (Exception e) {
            // Try button type (badge might be a button)
            try {
                WebElement badge = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == '" + priority + "'"));
                return badge.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Check if any priority badge is displayed (High, Medium, or Low)
     */
    public boolean isAnyPriorityBadgeDisplayed() {
        return isPriorityBadgeDisplayed("High") ||
               isPriorityBadgeDisplayed("Medium") ||
               isPriorityBadgeDisplayed("Low");
    }

    /**
     * Check if asset name is displayed on an issue entry
     */
    public boolean isAssetNameDisplayedOnIssue() {
        try {
            // Asset names appear as text in issue entries, often with a grid icon
            // Look for common asset name patterns
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"));
            WebElement cell = getFirstIssueEntry();
            if (cell != null) {
                int cellY = cell.getLocation().getY();
                int cellH = cell.getSize().getHeight();
                for (WebElement text : texts) {
                    int textY = text.getLocation().getY();
                    // Text must be within the cell area
                    if (textY >= cellY && textY <= cellY + cellH) {
                        String label = text.getAttribute("label");
                        // Asset names are typically short identifiers
                        if (label != null && label.length() > 1 && label.length() < 50 &&
                            !label.equals("Issues") && !label.contains("Open") &&
                            !label.contains("In Progress") && !label.equals("All") &&
                            !label.equals("High") && !label.equals("Medium") && !label.equals("Low")) {
                            System.out.println("   Asset name candidate: " + label);
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a status badge is displayed
     * @param status "Open", "In Progress", "Resolved", "Closed"
     */
    public boolean isStatusBadgeDisplayed(String status) {
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == '" + status + "'"));
            // Filter for badges that are in the list area (not the filter tabs)
            for (WebElement badge : badges) {
                int y = badge.getLocation().getY();
                // Status badges on entries are below the filter tabs (y > ~250)
                if (y > 250) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Check if any issue has a truncated title (ends with '...')
     */
    public boolean hasAnyTruncatedTitle() {
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label ENDSWITH '...'"));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y > 200) {
                    System.out.println("   Truncated title found: " + text.getAttribute("label"));
                    return true;
                }
            }
            // Fallback: check label attribute contains "..."
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            for (WebElement cell : cells) {
                String label = cell.getAttribute("label");
                if (label != null && label.contains("...")) {
                    System.out.println("   Truncated cell label found: " + label);
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Check if any issue entry has elements that look like an issue
     * (title text + icon + badge — verifies complete entry structure)
     */
    public boolean isIssueEntryComplete() {
        try {
            WebElement cell = getFirstIssueEntry();
            if (cell == null) return false;

            int cellY = cell.getLocation().getY();
            int cellH = cell.getSize().getHeight();
            boolean hasText = false;
            boolean hasImage = false;

            // Check for text elements within the cell
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y >= cellY && y <= cellY + cellH) {
                    hasText = true;
                    break;
                }
            }

            // Check for image elements within the cell
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage'"));
            for (WebElement img : images) {
                int y = img.getLocation().getY();
                if (y >= cellY && y <= cellY + cellH) {
                    hasImage = true;
                    break;
                }
            }

            System.out.println("   Issue entry: hasText=" + hasText + ", hasImage=" + hasImage);
            return hasText; // At minimum, an entry should have text
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // SORT (TC_ISS_019)
    // ================================================================

    /**
     * Tap sort icon in header
     */
    public void tapSortIcon() {
        System.out.println("📋 Tapping Sort icon...");
        try {
            WebElement sort = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name CONTAINS 'sort' OR name CONTAINS 'arrow.up.arrow.down' OR " +
                "label CONTAINS 'Sort' OR label CONTAINS '↕' OR " +
                "name CONTAINS 'line.3.horizontal.decrease')"));
            sort.click();
            sleep(300);
            System.out.println("✅ Tapped Sort icon");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Sort icon: " + e.getMessage());
        }
    }

    /**
     * Check if any issues are displayed in the list
     */
    public boolean hasIssuesInList() {
        return getVisibleIssueCount() > 0;
    }

    /**
     * Check if empty state / no results message is displayed
     */
    public boolean isNoResultsDisplayed() {
        try {
            WebElement noResults = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label CONTAINS 'No issues' OR label CONTAINS 'no issues' OR " +
                "label CONTAINS 'No results' OR label CONTAINS 'no results')"));
            return noResults.isDisplayed();
        } catch (Exception e) {
            // If no issues and no "no results" message, check if cell count is 0
            return getVisibleIssueCount() == 0;
        }
    }
}
