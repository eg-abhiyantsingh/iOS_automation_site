package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Page Object for Issues screen.
 *
 * Actual UI layout (from screenshots):
 *
 * HEADER:
 *   [Done]                [↕ Sort]  [+]
 *   Issues   (large bold title)
 *   [🔍 Search issues]
 *   [All N] [Open] [✓ Resolved N] [⊗ Closed N]
 *
 * FILTER TABS: All, Open, Resolved, Closed
 *   - Tab label format: "All 1", "Open", "Resolved 1", "Closed 0"
 *   - Count is space-separated (NOT parenthesized)
 *   - Open tab is selected (blue) by default
 *
 * EMPTY STATE: ⚠ "No Issues Found" + "Create a new issue to track problems or concerns"
 *
 * NEW ISSUE FORM (via + button):
 *   Cancel  |  New Issue  |  Create Issue (disabled until valid)
 *   CLASSIFICATION: Issue Class (None ⌃) — dropdown options:
 *     None, Canadian Codes Rough Draft, NEC Violation, NFPA 70B Violation,
 *     OSHA Violation, Other, Repair Needed, Replacement Needed,
 *     Thermal Anomaly, Ultrasonic Anomaly
 *   ISSUE DETAILS:
 *     Title: "Enter issue title" placeholder
 *     Priority: None ⌃ → None, High (!!!), Medium (!!), Low (!)
 *   ASSIGNMENT:
 *     Asset: "Select Asset >" → asset picker with search
 *     "Asset is required" validation
 */
public class IssuePage extends BasePage {

    // ================================================================
    // PAGE ELEMENTS
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
     * Check if Issues screen is displayed.
     * Looks for "Issues" nav bar/title OR the unique combination of
     * search bar + filter tabs (All/Open/Resolved).
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

            // Strategy 2: Large "Issues" title text near top
            try {
                List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Issues'"));
                for (WebElement title : titles) {
                    int y = title.getLocation().getY();
                    if (y < 200) {
                        System.out.println("✓ Issues screen detected (title text at y=" + y + ")");
                        return true;
                    }
                }
            } catch (Exception e2) {}

            // Strategy 3: Search bar + filter tabs unique to Issues
            try {
                WebElement searchBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSearchField'"));
                // Check for at least one filter tab
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label BEGINSWITH 'All' OR label BEGINSWITH 'Open' OR label CONTAINS 'Resolved')"));
                if (searchBar.isDisplayed() && tab.isDisplayed()) {
                    System.out.println("✓ Issues screen detected (search + filter tabs)");
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
     * Check if Sort icon (↕) is displayed in header.
     * From screenshot: two-arrow sort icon in top-right area.
     */
    public boolean isSortIconDisplayed() {
        try {
            WebElement sort = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name CONTAINS 'sort' OR name CONTAINS 'arrow.up.arrow.down' OR " +
                "name CONTAINS 'line.3.horizontal.decrease' OR " +
                "label CONTAINS 'Sort' OR label CONTAINS '↕')"));
            return sort.isDisplayed();
        } catch (Exception e) {
            // Fallback: look for any button in the top-right area that isn't Done or Add
            try {
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"));
                for (WebElement btn : buttons) {
                    int x = btn.getLocation().getX();
                    int y = btn.getLocation().getY();
                    String name = btn.getAttribute("name");
                    // Sort icon is in top-right, between Done(left) and +(right)
                    if (y < 150 && x > 200 && name != null &&
                        !name.equals("Done") && !name.equals("Add") &&
                        !name.contains("plus")) {
                        System.out.println("   Sort icon candidate: " + name + " at (" + x + "," + y + ")");
                        return true;
                    }
                }
            } catch (Exception e2) {}
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
     * Check if search bar is displayed.
     * From screenshot: "Search issues" placeholder in a search field.
     */
    public boolean isSearchBarDisplayed() {
        try {
            WebElement searchBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField'"));
            return searchBar.isDisplayed();
        } catch (Exception e) {
            // Fallback: text field with search placeholder
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

            // Try tapping the clear (x) button if it exists
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
    //
    // Actual tabs from screenshot: All, Open, Resolved, Closed
    // Label format: "All 1", "Open", "Resolved 1", "Closed 0"
    // Count is space-separated (NOT in parentheses).
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
     * Check if Resolved tab is displayed
     */
    public boolean isResolvedTabDisplayed() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Resolved'"));
            return tab.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Closed tab is displayed.
     * This is the 4th tab (partially visible in screenshot, circled-X icon).
     */
    public boolean isClosedTabDisplayed() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Closed'"));
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
     * Tap Resolved filter tab
     */
    public void tapResolvedTab() {
        System.out.println("📋 Tapping Resolved tab...");
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Resolved'"));
            tab.click();
            sleep(400);
            System.out.println("✅ Tapped Resolved tab");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Resolved tab: " + e.getMessage());
        }
    }

    /**
     * Tap Closed filter tab
     */
    public void tapClosedTab() {
        System.out.println("📋 Tapping Closed tab...");
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Closed'"));
            tab.click();
            sleep(400);
            System.out.println("✅ Tapped Closed tab");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Closed tab: " + e.getMessage());
        }
    }

    /**
     * Check if Open tab is currently selected (active).
     * From screenshot: selected tab has blue background.
     */
    public boolean isOpenTabSelected() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'Open'"));
            // Check selected attribute
            String selected = tab.getAttribute("selected");
            if ("true".equals(selected)) return true;
            // Fallback: check value attribute
            String value = tab.getAttribute("value");
            if ("1".equals(value)) return true;
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
            if ("true".equals(selected)) return true;
            String value = tab.getAttribute("value");
            return "1".equals(value);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Resolved tab is currently selected
     */
    public boolean isResolvedTabSelected() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Resolved'"));
            String selected = tab.getAttribute("selected");
            if ("true".equals(selected)) return true;
            String value = tab.getAttribute("value");
            return "1".equals(value);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract count from a filter tab label.
     *
     * ACTUAL FORMAT (from screenshot): "All 1", "Resolved 1", "Open", "Closed 0"
     * The count is the trailing number after the last space.
     * Tabs with 0 issues may omit the count entirely (e.g., just "Open").
     */
    private int extractCountFromTabLabel(String tabLabel) {
        if (tabLabel == null || tabLabel.isEmpty()) return -1;
        try {
            // Format: "All 1" or "Resolved 1" — count is trailing number
            String trimmed = tabLabel.trim();
            int lastSpace = trimmed.lastIndexOf(' ');
            if (lastSpace >= 0 && lastSpace < trimmed.length() - 1) {
                String numStr = trimmed.substring(lastSpace + 1);
                return Integer.parseInt(numStr);
            }
            // No space with trailing number — might be "Open" with 0 count
            return 0;
        } catch (NumberFormatException e) {
            // Trailing part isn't a number — tab has no count shown
            return 0;
        }
    }

    /**
     * Get count from All tab
     */
    public int getAllTabCount() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'All'"));
            String label = tab.getAttribute("label");
            System.out.println("   All tab label: '" + label + "'");
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
            System.out.println("   Open tab label: '" + label + "'");
            return extractCountFromTabLabel(label);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Get count from Resolved tab
     */
    public int getResolvedTabCount() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Resolved'"));
            String label = tab.getAttribute("label");
            System.out.println("   Resolved tab label: '" + label + "'");
            return extractCountFromTabLabel(label);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Get count from Closed tab
     */
    public int getClosedTabCount() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Closed'"));
            String label = tab.getAttribute("label");
            System.out.println("   Closed tab label: '" + label + "'");
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
                String label = cell.getAttribute("label");
                if (label != null && !label.isEmpty()) {
                    return label;
                }
                // Try finding a static text within the cell area
                List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"));
                int cellY = cell.getLocation().getY();
                for (WebElement text : texts) {
                    int textY = text.getLocation().getY();
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
     * Check if issue type icon is displayed on any issue entry.
     * Icons: ❗ for violations, 🔧 for Repair, 🌡️ for Thermal
     */
    public boolean isIssueTypeIconDisplayed() {
        try {
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage'"));
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
            // Fallback: any image element in the list area
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
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"));
            WebElement cell = getFirstIssueEntry();
            if (cell != null) {
                int cellY = cell.getLocation().getY();
                int cellH = cell.getSize().getHeight();
                for (WebElement text : texts) {
                    int textY = text.getLocation().getY();
                    if (textY >= cellY && textY <= cellY + cellH) {
                        String label = text.getAttribute("label");
                        // Exclude known non-asset labels
                        if (label != null && label.length() > 1 && label.length() < 50 &&
                            !label.equals("Issues") && !label.contains("Open") &&
                            !label.contains("Resolved") && !label.contains("Closed") &&
                            !label.equals("All") &&
                            !label.equals("High") && !label.equals("Medium") && !label.equals("Low") &&
                            !label.contains("No Issues")) {
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
     * Check if a status badge is displayed on issue entries (not filter tabs).
     * @param status "Open", "Resolved", "Closed"
     */
    public boolean isStatusBadgeDisplayed(String status) {
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == '" + status + "'"));
            for (WebElement badge : badges) {
                int y = badge.getLocation().getY();
                // Status badges on entries are below the filter tabs (y > ~300)
                if (y > 300) {
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
     * Check if any issue entry has required elements (title text at minimum)
     */
    public boolean isIssueEntryComplete() {
        try {
            WebElement cell = getFirstIssueEntry();
            if (cell == null) return false;

            int cellY = cell.getLocation().getY();
            int cellH = cell.getSize().getHeight();
            boolean hasText = false;

            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y >= cellY && y <= cellY + cellH) {
                    hasText = true;
                    break;
                }
            }

            System.out.println("   Issue entry complete: hasText=" + hasText);
            return hasText;
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
                "name CONTAINS 'line.3.horizontal.decrease' OR " +
                "label CONTAINS 'Sort' OR label CONTAINS '↕')"));
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
     * Check if empty state / "No Issues Found" message is displayed.
     * From screenshot: "No Issues Found" + "Create a new issue to track problems or concerns"
     */
    public boolean isNoIssuesFoundDisplayed() {
        try {
            WebElement noIssues = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'No Issues Found'"));
            return noIssues.isDisplayed();
        } catch (Exception e) {
            // Fallback: partial match
            try {
                WebElement noIssues = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'No Issues' OR label CONTAINS 'No issues' OR " +
                    "label CONTAINS 'No results')"));
                return noIssues.isDisplayed();
            } catch (Exception e2) {
                return getVisibleIssueCount() == 0;
            }
        }
    }

    // ================================================================
    // NEW ISSUE FORM (from + button screenshots)
    // ================================================================

    /**
     * Tap Add (+) button to open New Issue form
     */
    public void tapAddButton() {
        System.out.println("➕ Tapping Add button...");
        try {
            WebElement addBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name == 'Add' OR name == 'plus' OR name CONTAINS 'plus' OR label == 'Add')"));
            addBtn.click();
            sleep(500);
            System.out.println("✅ Tapped Add button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Add: " + e.getMessage());
        }
    }

    /**
     * Check if New Issue form is displayed
     */
    public boolean isNewIssueFormDisplayed() {
        try {
            WebElement navBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'New Issue'"));
            return navBar.isDisplayed();
        } catch (Exception e) {
            try {
                WebElement navBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeNavigationBar' AND name == 'New Issue'"));
                return navBar.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Select Issue Class from dropdown.
     * Options: Canadian Codes Rough Draft, NEC Violation, NFPA 70B Violation,
     *          OSHA Violation, Other, Repair Needed, Replacement Needed,
     *          Thermal Anomaly, Ultrasonic Anomaly
     */
    public void selectIssueClass(String className) {
        System.out.println("📋 Selecting Issue Class: " + className);
        try {
            // Tap the Issue Class picker button (shows "None ⌃")
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Issue Class'"));
            picker.click();
            sleep(400);

            // Select the option from the dropdown
            WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + className + "'"));
            option.click();
            sleep(300);
            System.out.println("✅ Selected Issue Class: " + className);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Issue Class: " + e.getMessage());
        }
    }

    /**
     * Enter issue title
     */
    public void enterIssueTitle(String title) {
        System.out.println("📝 Entering issue title: " + title);
        try {
            WebElement titleField = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND " +
                "(placeholderValue == 'Enter issue title' OR value == 'Enter issue title')"));
            titleField.click();
            sleep(200);
            titleField.sendKeys(title);
            sleep(300);
            System.out.println("✅ Entered issue title");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter title: " + e.getMessage());
        }
    }

    /**
     * Select Priority from dropdown.
     * Options: None, High, Medium, Low
     */
    public void selectPriority(String priority) {
        System.out.println("📋 Selecting Priority: " + priority);
        try {
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Priority'"));
            picker.click();
            sleep(400);

            WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + priority + "'"));
            option.click();
            sleep(300);
            System.out.println("✅ Selected Priority: " + priority);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Priority: " + e.getMessage());
        }
    }

    /**
     * Tap Select Asset to open asset picker
     */
    public void tapSelectAsset() {
        System.out.println("📋 Tapping Select Asset...");
        try {
            WebElement selectAsset = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(label == 'Select Asset' OR label CONTAINS 'Select Asset') AND " +
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeCell')"));
            selectAsset.click();
            sleep(500);
            System.out.println("✅ Opened Select Asset picker");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Select Asset: " + e.getMessage());
        }
    }

    /**
     * Select an asset by name in the asset picker.
     * From screenshot: assets listed as "ATS 1\nB1 > F1 > R1"
     */
    public void selectAssetByName(String assetName) {
        System.out.println("📋 Selecting asset: " + assetName);
        try {
            WebElement asset = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS '" + assetName + "'"));
            asset.click();
            sleep(500);
            System.out.println("✅ Selected asset: " + assetName);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select asset: " + e.getMessage());
        }
    }

    /**
     * Tap Create Issue button
     */
    public void tapCreateIssue() {
        System.out.println("🆕 Tapping Create Issue...");
        try {
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(label == 'Create Issue' OR name == 'Create Issue') AND type == 'XCUIElementTypeButton'"));
            String enabled = btn.getAttribute("enabled");
            if ("true".equals(enabled)) {
                btn.click();
                sleep(500);
                System.out.println("✅ Tapped Create Issue");
            } else {
                System.out.println("⚠️ Create Issue button is disabled");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Create Issue: " + e.getMessage());
        }
    }

    /**
     * Tap Cancel on New Issue form
     */
    public void tapCancelNewIssue() {
        System.out.println("❌ Tapping Cancel...");
        try {
            WebElement cancel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label == 'Cancel' AND type == 'XCUIElementTypeButton'"));
            cancel.click();
            sleep(300);
            System.out.println("✅ Cancelled New Issue");
        } catch (Exception e) {
            System.out.println("⚠️ Could not cancel: " + e.getMessage());
        }
    }

    /**
     * Check if Create Issue button is enabled
     */
    public boolean isCreateIssueEnabled() {
        try {
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(label == 'Create Issue' OR name == 'Create Issue') AND type == 'XCUIElementTypeButton'"));
            return "true".equals(btn.getAttribute("enabled"));
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // SORT OPTIONS (TC_ISS_020)
    // ================================================================

    /**
     * Check if sort options are displayed after tapping sort icon.
     * Looks for any sort-related buttons/options that appeared.
     */
    public boolean isSortOptionsDisplayed() {
        try {
            // Strategy 1: Look for sort option buttons (Priority, Date, Title, etc.)
            List<WebElement> options = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Priority' OR label CONTAINS 'Date' OR " +
                "label CONTAINS 'Title' OR label CONTAINS 'Created' OR " +
                "label CONTAINS 'Name' OR label CONTAINS 'Newest' OR " +
                "label CONTAINS 'Oldest' OR label CONTAINS 'Ascending' OR " +
                "label CONTAINS 'Descending')"));
            if (!options.isEmpty()) {
                for (WebElement opt : options) {
                    System.out.println("   Sort option found: " + opt.getAttribute("label"));
                }
                return true;
            }
            // Strategy 2: Check for action sheet or popover
            List<WebElement> sheets = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSheet' OR type == 'XCUIElementTypeActionSheet'"));
            if (!sheets.isEmpty()) {
                System.out.println("   Sort action sheet found");
                return true;
            }
            // Strategy 3: Any new menu/popover containing sort-like options
            List<WebElement> menus = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeOther' AND label CONTAINS 'Sort'"));
            return !menus.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Dismiss sort options by tapping outside the popup
     */
    public void dismissSortOptions() {
        try {
            // Tap at a neutral coordinate to dismiss the sort popup
            Map<String, Object> tapParams = new HashMap<>();
            tapParams.put("x", 200);
            tapParams.put("y", 300);
            driver.executeScript("mobile: tap", tapParams);
            sleep(300);
            System.out.println("✅ Dismissed sort options");
        } catch (Exception e) {
            // Fallback: tap the sort icon again to toggle
            try {
                tapSortIcon();
            } catch (Exception e2) {
                System.out.println("⚠️ Could not dismiss sort options");
            }
        }
    }

    // ================================================================
    // NEW ISSUE FORM SECTIONS (TC_ISS_022, TC_ISS_024)
    // ================================================================

    /**
     * Check if CLASSIFICATION section is displayed on New Issue form
     */
    public boolean isClassificationSectionDisplayed() {
        try {
            WebElement section = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'CLASSIFICATION' OR label == 'Classification')"));
            return section.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if ISSUE DETAILS section is displayed on New Issue form
     */
    public boolean isIssueDetailsSectionDisplayed() {
        try {
            WebElement section = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'ISSUE DETAILS' OR label == 'Issue Details')"));
            return section.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if ASSIGNMENT section is displayed on New Issue form
     */
    public boolean isAssignmentSectionDisplayed() {
        try {
            WebElement section = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'ASSIGNMENT' OR label == 'Assignment')"));
            return section.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if "Asset is required" validation message is displayed
     */
    public boolean isAssetRequiredMessageDisplayed() {
        try {
            WebElement msg = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Asset is required' OR label CONTAINS 'Asset is required')"));
            return msg.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Select Asset field is displayed on New Issue form
     */
    public boolean isSelectAssetDisplayed() {
        try {
            WebElement asset = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(label CONTAINS 'Select Asset' OR label CONTAINS 'Asset') AND " +
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeCell' OR " +
                "type == 'XCUIElementTypeStaticText')"));
            return asset.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // ISSUE CLASS DROPDOWN (TC_ISS_026 - TC_ISS_033)
    //
    // SwiftUI Picker with .menu style:
    //   Static label: XCUIElementTypeStaticText, label="Issue Class" (NOT clickable)
    //   Picker button: XCUIElementTypeButton, name CONTAINS "Issue Class" (clickable)
    //   Button label format: "Issue Class, None" → "Issue Class, NEC Violation"
    // ================================================================

    /**
     * Check if Issue Class dropdown is displayed on New Issue form
     */
    public boolean isIssueClassDropdownDisplayed() {
        try {
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Issue Class'"));
            return picker.isDisplayed();
        } catch (Exception e) {
            // Fallback: check for static text label at least
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Issue Class'"));
                return label.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Get the current value of Issue Class picker.
     * Reads the picker button label and extracts the value part.
     * Format: "Issue Class, NEC Violation" → returns "NEC Violation"
     * Or the button may have a separate value attribute.
     */
    public String getIssueClassValue() {
        try {
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Issue Class'"));
            String label = picker.getAttribute("label");
            String name = picker.getAttribute("name");
            String value = picker.getAttribute("value");
            System.out.println("   Issue Class picker — label: '" + label +
                "', name: '" + name + "', value: '" + value + "'");

            // Try value attribute first
            if (value != null && !value.isEmpty() && !value.equals("0") && !value.equals("1")) {
                return value;
            }
            // Extract from label: "Issue Class, VALUE"
            if (label != null && label.contains(", ")) {
                return label.substring(label.indexOf(", ") + 2).trim();
            }
            // Extract from name: "Issue Class, VALUE"
            if (name != null && name.contains(", ")) {
                return name.substring(name.indexOf(", ") + 2).trim();
            }
            return label != null ? label : "";
        } catch (Exception e) {
            System.out.println("⚠️ Could not get Issue Class value: " + e.getMessage());
            return "";
        }
    }

    /**
     * Open Issue Class dropdown without selecting an option.
     * Returns true if dropdown opened successfully.
     */
    public boolean openIssueClassDropdown() {
        try {
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Issue Class'"));
            picker.click();
            sleep(500);
            System.out.println("✅ Opened Issue Class dropdown");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not open Issue Class dropdown: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a specific option is displayed in an open dropdown menu.
     * @param option The option text to look for (e.g., "NEC Violation")
     */
    public boolean isDropdownOptionDisplayed(String option) {
        try {
            WebElement optBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + option + "'"));
            return optBtn.isDisplayed();
        } catch (Exception e) {
            // Fallback: check static text (some menus render options as text)
            try {
                WebElement optText = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == '" + option + "'"));
                return optText.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Select an Issue Class option and return the newly selected value.
     * Opens the dropdown, selects, and reads back the picker value.
     */
    public String selectIssueClassAndGetValue(String className) {
        selectIssueClass(className);
        sleep(300);
        return getIssueClassValue();
    }

    /**
     * Dismiss an open dropdown menu by tapping at a neutral coordinate.
     * Used when verifying dropdown options without selecting one.
     */
    public void dismissDropdownMenu() {
        try {
            Map<String, Object> tapParams = new HashMap<>();
            tapParams.put("x", 200);
            tapParams.put("y", 100);
            driver.executeScript("mobile: tap", tapParams);
            sleep(300);
            System.out.println("✅ Dismissed dropdown menu");
        } catch (Exception e) {
            System.out.println("⚠️ Could not dismiss dropdown menu: " + e.getMessage());
        }
    }

    // ================================================================
    // TITLE FIELD (TC_ISS_034, TC_ISS_035)
    // ================================================================

    /**
     * Check if Title text field is displayed on New Issue form.
     * From screenshot: pencil icon + "Enter issue title" placeholder.
     */
    public boolean isTitleFieldDisplayed() {
        try {
            WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND " +
                "(placeholderValue CONTAINS 'issue title' OR placeholderValue CONTAINS 'Issue title' OR " +
                "placeholderValue == 'Enter issue title' OR value == 'Enter issue title')"));
            return field.isDisplayed();
        } catch (Exception e) {
            // Fallback: any text field in the form area
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"));
                return field.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Get the placeholder text of the Title field
     */
    public String getTitleFieldPlaceholder() {
        try {
            WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"));
            String placeholder = field.getAttribute("placeholderValue");
            if (placeholder == null || placeholder.isEmpty()) {
                placeholder = field.getAttribute("value");
            }
            System.out.println("   Title field placeholder: '" + placeholder + "'");
            return placeholder != null ? placeholder : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the current value of the Title field
     */
    public String getTitleFieldValue() {
        try {
            WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"));
            String val = field.getAttribute("value");
            System.out.println("   Title field value: '" + val + "'");
            return val != null ? val : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // PRIORITY DROPDOWN (TC_ISS_036 - TC_ISS_039)
    //
    // SwiftUI Picker with .menu style:
    //   Static label: XCUIElementTypeStaticText, label="Priority" (NOT clickable)
    //   Picker button: XCUIElementTypeButton, name CONTAINS "Priority" (clickable)
    //   Options: None, High (!!!), Medium (!!), Low (!)
    // ================================================================

    /**
     * Check if Priority dropdown is displayed on New Issue form
     */
    public boolean isPriorityDropdownDisplayed() {
        try {
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Priority'"));
            return picker.isDisplayed();
        } catch (Exception e) {
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Priority'"));
                return label.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Get the current value of Priority picker.
     * Format: "Priority, None" → returns "None"
     */
    public String getPriorityValue() {
        try {
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Priority'"));
            String label = picker.getAttribute("label");
            String name = picker.getAttribute("name");
            String value = picker.getAttribute("value");
            System.out.println("   Priority picker — label: '" + label +
                "', name: '" + name + "', value: '" + value + "'");

            if (value != null && !value.isEmpty() && !value.equals("0") && !value.equals("1")) {
                return value;
            }
            if (label != null && label.contains(", ")) {
                return label.substring(label.indexOf(", ") + 2).trim();
            }
            if (name != null && name.contains(", ")) {
                return name.substring(name.indexOf(", ") + 2).trim();
            }
            return label != null ? label : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Open Priority dropdown without selecting an option.
     * Returns true if dropdown opened successfully.
     */
    public boolean openPriorityDropdown() {
        try {
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Priority'"));
            picker.click();
            sleep(500);
            System.out.println("✅ Opened Priority dropdown");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not open Priority dropdown: " + e.getMessage());
            return false;
        }
    }

    /**
     * Select a Priority option and return the newly selected value.
     */
    public String selectPriorityAndGetValue(String priority) {
        selectPriority(priority);
        sleep(300);
        return getPriorityValue();
    }

    // ================================================================
    // ASSET SELECTION SCREEN (TC_ISS_041 - TC_ISS_047)
    //
    // From screenshot: Cancel | "Select Asset" title | [+ QR]
    // Search bar + scrollable asset list with "ATS 1\nB1 > F1 > R1"
    // ================================================================

    /**
     * Check if Select Asset screen is displayed.
     * Looks for "Select Asset" nav title on the picker screen.
     */
    public boolean isSelectAssetScreenDisplayed() {
        try {
            WebElement title = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeNavigationBar') AND " +
                "(label == 'Select Asset' OR name == 'Select Asset')"));
            return title.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the count of visible assets in the asset picker list
     */
    public int getAssetListCount() {
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            System.out.println("   Visible assets in picker: " + cells.size());
            return cells.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Search for an asset in the asset picker search bar
     */
    public void searchAssetsInPicker(String query) {
        System.out.println("🔍 Searching for asset: " + query);
        try {
            WebElement searchBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField'"));
            searchBar.click();
            sleep(300);
            searchBar.sendKeys(query);
            sleep(500);
            System.out.println("✅ Entered asset search: " + query);
        } catch (Exception e) {
            System.out.println("⚠️ Asset search failed: " + e.getMessage());
        }
    }

    /**
     * Select an asset in the picker by tapping on it.
     * Returns true if tap was successful.
     */
    public boolean selectAssetInPicker(String assetName) {
        System.out.println("📋 Selecting asset in picker: " + assetName);
        try {
            WebElement asset = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS '" + assetName + "'"));
            asset.click();
            sleep(500);
            System.out.println("✅ Selected asset: " + assetName);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not select asset: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if + (Add) button is displayed on the asset picker screen.
     * This allows creating a new asset from within the picker.
     */
    public boolean isAddAssetButtonOnPickerDisplayed() {
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
     * Check if QR scan button is displayed on the asset picker screen
     */
    public boolean isQRScanButtonDisplayed() {
        try {
            WebElement qrBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name CONTAINS 'qr' OR name CONTAINS 'QR' OR name CONTAINS 'scan' OR " +
                "name CONTAINS 'barcode' OR label CONTAINS 'QR' OR label CONTAINS 'Scan' OR " +
                "name CONTAINS 'viewfinder')"));
            return qrBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the selected asset name shown on the New Issue form after selection.
     * After picking an asset, the Asset field shows the asset name instead of "Select Asset".
     */
    public String getSelectedAssetName() {
        try {
            // Look for the asset field area — it will show the asset name
            // Strategy 1: look for text near the ASSIGNMENT section
            WebElement assignmentSection = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'ASSIGNMENT' OR label == 'Assignment')"));
            int sectionY = assignmentSection.getLocation().getY();

            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y > sectionY && y < sectionY + 100) {
                    String label = text.getAttribute("label");
                    if (label != null && !label.isEmpty() &&
                        !label.equals("ASSIGNMENT") && !label.equals("Assignment") &&
                        !label.equals("Asset is required") && !label.equals("Select Asset")) {
                        System.out.println("   Selected asset name: " + label);
                        return label;
                    }
                }
            }
            // Strategy 2: check the button/cell that used to say "Select Asset"
            try {
                WebElement assetField = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeCell') AND " +
                    "NOT label BEGINSWITH 'Select Asset'"));
                String label = assetField.getAttribute("label");
                if (label != null && !label.isEmpty()) {
                    return label;
                }
            } catch (Exception ignored) {}
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Cancel/go back from asset picker screen
     */
    public void tapCancelAssetPicker() {
        System.out.println("❌ Cancelling asset picker...");
        try {
            WebElement cancel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label == 'Cancel' AND type == 'XCUIElementTypeButton'"));
            cancel.click();
            sleep(300);
            System.out.println("✅ Cancelled asset picker");
        } catch (Exception e) {
            System.out.println("⚠️ Could not cancel asset picker: " + e.getMessage());
        }
    }

    // ================================================================
    // ISSUE LIST VERIFICATION (TC_ISS_050 - TC_ISS_051)
    // ================================================================

    /**
     * Check if an issue with the given title exists in the issue list.
     * Searches for a StaticText or Cell with matching label.
     */
    public boolean isIssueInList(String title) {
        try {
            WebElement issue = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeCell') AND " +
                "label CONTAINS '" + title + "'"));
            System.out.println("   Issue '" + title + "' found in list");
            return issue.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap on an issue in the list by its title to open Issue Details
     */
    public void tapOnIssue(String title) {
        System.out.println("📋 Tapping on issue: " + title);
        try {
            // Strategy 1: tap cell containing the title
            WebElement issue = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND label CONTAINS '" + title + "'"));
            issue.click();
            sleep(500);
            System.out.println("✅ Tapped on issue: " + title);
        } catch (Exception e) {
            // Strategy 2: tap the static text and hope its parent cell responds
            try {
                WebElement issueText = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + title + "'"));
                issueText.click();
                sleep(500);
                System.out.println("✅ Tapped on issue text: " + title);
            } catch (Exception e2) {
                System.out.println("⚠️ Could not tap on issue: " + title + " - " + e2.getMessage());
            }
        }
    }

    // ================================================================
    // ISSUE DETAILS SCREEN (TC_ISS_052 - TC_ISS_059)
    //
    // From screenshot: Close button | "Issue Details" title
    // Warning icon + Title + Status badge + Asset name
    // Status dropdown (Open, In Progress, Resolved, Closed)
    // Priority field (editable)
    // Issue Class field (editable)
    // ================================================================

    /**
     * Check if Issue Details screen is displayed
     */
    public boolean isIssueDetailsScreenDisplayed() {
        try {
            WebElement title = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeNavigationBar') AND " +
                "(label == 'Issue Details' OR name == 'Issue Details')"));
            return title.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the issue title shown on Issue Details screen
     */
    public String getIssueDetailTitle() {
        try {
            // The title appears as a prominent text below the nav bar
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                int y = text.getLocation().getY();
                // Skip known labels, look for the issue title in the header area
                if (label != null && y > 80 && y < 300 &&
                    !label.equals("Issue Details") && !label.equals("Close") &&
                    !label.equals("Open") && !label.equals("In Progress") &&
                    !label.equals("Resolved") && !label.equals("Closed") &&
                    !label.contains("Status") && label.length() > 1) {
                    System.out.println("   Issue detail title: " + label);
                    return label;
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the status badge text on Issue Details screen (e.g., "Open", "In Progress")
     */
    public String getIssueDetailStatus() {
        try {
            // Look for status badge text — known statuses
            String[] statuses = {"Open", "In Progress", "Resolved", "Closed"};
            for (String status : statuses) {
                try {
                    WebElement badge = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label == '" + status + "'"));
                    int y = badge.getLocation().getY();
                    // Status badge is in the header area (not filter tabs which are higher)
                    if (y > 150 && y < 400) {
                        System.out.println("   Issue status: " + status);
                        return status;
                    }
                } catch (Exception ignored) {}
            }
            // Fallback: check button with status label
            for (String status : statuses) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label CONTAINS '" + status + "'"));
                    return status;
                } catch (Exception ignored) {}
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the asset name shown on Issue Details screen
     */
    public String getIssueDetailAssetName() {
        try {
            // Asset name often appears with a grid/location icon
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                int y = text.getLocation().getY();
                // Asset name is in the header section, usually after title and status
                if (label != null && y > 200 && y < 400 &&
                    !label.equals("Issue Details") && !label.equals("Open") &&
                    !label.equals("In Progress") && !label.equals("Resolved") &&
                    !label.equals("Closed") && !label.equals("Close") &&
                    !label.contains("Status") && !label.contains("Priority") &&
                    !label.contains("Class") && label.length() > 1 && label.length() < 50) {
                    // Heuristic: asset names often contain spaces or specific patterns
                    System.out.println("   Issue detail asset candidate: " + label);
                    return label;
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Close Issue Details screen by tapping Close/Back button
     */
    public void tapCloseIssueDetails() {
        System.out.println("❌ Closing Issue Details...");
        try {
            WebElement closeBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Close' OR name == 'Close' OR label == 'Back' OR name == 'Back')"));
            closeBtn.click();
            sleep(300);
            System.out.println("✅ Closed Issue Details");
        } catch (Exception e) {
            System.out.println("⚠️ Could not close Issue Details: " + e.getMessage());
        }
    }

    /**
     * Open Status dropdown on Issue Details screen.
     * The status picker button shows current status.
     */
    public boolean openStatusDropdown() {
        try {
            // Strategy 1: Button with current status value
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name CONTAINS 'Status' OR label CONTAINS 'Status')"));
            picker.click();
            sleep(500);
            System.out.println("✅ Opened Status dropdown");
            return true;
        } catch (Exception e) {
            // Strategy 2: Look for button with known status labels
            String[] statuses = {"Open", "In Progress", "Resolved", "Closed"};
            for (String status : statuses) {
                try {
                    List<WebElement> btns = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == '" + status + "'"));
                    for (WebElement btn : btns) {
                        int y = btn.getLocation().getY();
                        if (y > 200 && y < 500) {
                            btn.click();
                            sleep(500);
                            System.out.println("✅ Opened Status dropdown via '" + status + "' button");
                            return true;
                        }
                    }
                } catch (Exception ignored) {}
            }
            System.out.println("⚠️ Could not open Status dropdown");
            return false;
        }
    }

    /**
     * Check if a status option is displayed in the open dropdown
     */
    public boolean isStatusOptionDisplayed(String status) {
        return isDropdownOptionDisplayed(status);
    }

    /**
     * Select a status from the dropdown on Issue Details screen
     */
    public void selectStatus(String status) {
        System.out.println("📋 Selecting status: " + status);
        try {
            WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + status + "'"));
            option.click();
            sleep(400);
            System.out.println("✅ Selected status: " + status);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select status: " + e.getMessage());
        }
    }

    /**
     * Check if Priority field is displayed on Issue Details screen
     */
    public boolean isPriorityDisplayedOnDetails() {
        try {
            WebElement priority = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Priority' OR label CONTAINS 'Priority')"));
            return priority.isDisplayed();
        } catch (Exception e) {
            // Fallback: check for priority button
            try {
                WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Priority'"));
                return btn.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Get Priority value on Issue Details screen.
     * Looks for priority text (High, Medium, Low) or the picker button value.
     */
    public String getPriorityOnDetails() {
        try {
            // Try reading from the picker button
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Priority'"));
            String label = picker.getAttribute("label");
            String value = picker.getAttribute("value");
            System.out.println("   Priority on details — label: '" + label + "', value: '" + value + "'");

            if (value != null && !value.isEmpty() && !value.equals("0") && !value.equals("1")) {
                return value;
            }
            if (label != null && label.contains(", ")) {
                return label.substring(label.indexOf(", ") + 2).trim();
            }
            return label != null ? label : "";
        } catch (Exception e) {
            // Fallback: look for known priority text
            String[] priorities = {"High", "Medium", "Low"};
            for (String p : priorities) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label == '" + p + "'"));
                    return p;
                } catch (Exception ignored) {}
            }
            return "";
        }
    }

    /**
     * Check if Issue Class field is displayed on Issue Details screen
     */
    public boolean isIssueClassDisplayedOnDetails() {
        try {
            WebElement issueClass = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Issue Class' OR label CONTAINS 'Issue Class')"));
            return issueClass.isDisplayed();
        } catch (Exception e) {
            try {
                WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Issue Class'"));
                return btn.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    /**
     * Get Issue Class value on Issue Details screen
     */
    public String getIssueClassOnDetails() {
        try {
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'Issue Class'"));
            String label = picker.getAttribute("label");
            String value = picker.getAttribute("value");
            System.out.println("   Issue Class on details — label: '" + label + "', value: '" + value + "'");

            if (value != null && !value.isEmpty() && !value.equals("0") && !value.equals("1")) {
                return value;
            }
            if (label != null && label.contains(", ")) {
                return label.substring(label.indexOf(", ") + 2).trim();
            }
            return label != null ? label : "";
        } catch (Exception e) {
            return "";
        }
    }
}
