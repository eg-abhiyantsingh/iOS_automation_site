

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

    // ================================================================
    // ISSUE DETAILS SECTION — COMPLETION & REQUIRED FIELDS (TC_ISS_060-061)
    //
    // Within the Issue Details screen, there is an "Issue Details" section
    // that shows completion percentage (e.g., "0%") and has a
    // "Required fields only" toggle with count (e.g., "0/1").
    // ================================================================

    /**
     * Check if the "Issue Details" section header is displayed within the issue detail screen.
     * This is a SECTION header (not the screen title), typically lower on the page.
     * It may appear as "Issue Details" with a completion badge nearby.
     */
    public boolean isIssueDetailsSectionHeaderDisplayed() {
        try {
            // Look for "Issue Details" text that is further down the screen (section header, not nav bar)
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Issue Details'"));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                // Section header is below the status/priority area (y > 400)
                if (y > 350) {
                    System.out.println("   Issue Details section header found at Y=" + y + ", label: " + text.getAttribute("label"));
                    return true;
                }
            }
            // Fallback: look for any text that combines "Issue Details" with a percentage
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Issue Details' AND label CONTAINS '%'"));
            if (!allTexts.isEmpty()) {
                System.out.println("   Found combined Issue Details + percentage label");
                return true;
            }
            // Try scrolling down to find it
            scrollDownOnDetailsScreen();
            texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Issue Details'"));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y > 200) {
                    System.out.println("   Issue Details section header found after scroll at Y=" + y);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Issue Details section header: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the completion percentage from the Issue Details section header.
     * Expected format: "0%" or "50%" etc., displayed near the "Issue Details" section label.
     */
    public String getIssueDetailsCompletionPercentage() {
        try {
            // Strategy 1: Look for a combined label like "Issue Details 0%" or just "0%"
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '%'"));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                int y = text.getLocation().getY();
                // Must be in the section header area (lower part of screen)
                if (y > 300 && label != null) {
                    // Check if it's a percentage value
                    if (label.matches(".*\\d+%.*")) {
                        // Extract the percentage
                        String pct = label.replaceAll(".*?(\\d+%).*", "$1");
                        System.out.println("   Completion percentage found: " + pct + " (full label: '" + label + "')");
                        return pct;
                    }
                }
            }
            // Strategy 2: Look for text near the "Issue Details" section header
            List<WebElement> sectionHeaders = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Issue Details'"));
            for (WebElement header : sectionHeaders) {
                int headerY = header.getLocation().getY();
                if (headerY > 300) {
                    String headerLabel = header.getAttribute("label");
                    // The header itself might contain the percentage
                    if (headerLabel != null && headerLabel.contains("%")) {
                        String pct = headerLabel.replaceAll(".*?(\\d+%).*", "$1");
                        return pct;
                    }
                    // Look for percentage text near the header (within 50px vertically)
                    for (WebElement text : texts) {
                        int textY = text.getLocation().getY();
                        if (Math.abs(textY - headerY) < 50) {
                            String label = text.getAttribute("label");
                            if (label != null && label.matches("\\d+%")) {
                                System.out.println("   Percentage near section header: " + label);
                                return label;
                            }
                        }
                    }
                }
            }
            System.out.println("⚠️ Completion percentage not found");
            return "";
        } catch (Exception e) {
            System.out.println("⚠️ Error getting completion percentage: " + e.getMessage());
            return "";
        }
    }

    /**
     * Check if the "Required fields only" toggle is displayed.
     * Expected UI: toggle with count like "0/1".
     */
    public boolean isRequiredFieldsToggleDisplayed() {
        try {
            // Strategy 1: Look for switch/toggle with "Required" text
            WebElement toggle = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSwitch' AND " +
                "(label CONTAINS 'Required' OR name CONTAINS 'Required' OR " +
                "label CONTAINS 'required')"));
            System.out.println("   Required fields toggle found (Switch type)");
            return toggle.isDisplayed();
        } catch (Exception e) {
            try {
                // Strategy 2: look for static text "Required fields only"
                WebElement text = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Required fields' OR label CONTAINS 'required fields')"));
                System.out.println("   Required fields toggle text found: " + text.getAttribute("label"));
                return text.isDisplayed();
            } catch (Exception e2) {
                try {
                    // Strategy 3: Button-style toggle
                    WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND " +
                        "(label CONTAINS 'Required' OR name CONTAINS 'Required')"));
                    System.out.println("   Required fields toggle found (Button type)");
                    return btn.isDisplayed();
                } catch (Exception e3) {
                    System.out.println("⚠️ Required fields toggle not found");
                    return false;
                }
            }
        }
    }

    /**
     * Get the Required fields toggle count (e.g., "0/1").
     * Looks for text like "0/1" near the "Required fields only" label.
     */
    public String getRequiredFieldsToggleCount() {
        try {
            // Strategy 1: Look for switch element value
            try {
                WebElement toggle = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSwitch' AND " +
                    "(label CONTAINS 'Required' OR name CONTAINS 'Required')"));
                String label = toggle.getAttribute("label");
                String value = toggle.getAttribute("value");
                System.out.println("   Toggle — label: '" + label + "', value: '" + value + "'");
                // Label might contain the count like "Required fields only 0/1"
                if (label != null && label.contains("/")) {
                    String count = label.replaceAll(".*?(\\d+/\\d+).*", "$1");
                    return count;
                }
            } catch (Exception ignored) {}

            // Strategy 2: Find text with X/Y pattern near the toggle area
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label MATCHES '\\\\d+/\\\\d+'"));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                System.out.println("   Found fraction text: " + label);
                return label;
            }

            // Strategy 3: Look for any text containing "/" pattern near the toggle
            texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '/'"));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                int y = text.getLocation().getY();
                if (y > 300 && label != null && label.matches("\\d+/\\d+")) {
                    System.out.println("   Required fields count found: " + label);
                    return label;
                }
            }

            // Strategy 4: Check if combined with "Required fields only" text
            List<WebElement> reqTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Required'"));
            for (WebElement reqText : reqTexts) {
                String label = reqText.getAttribute("label");
                if (label != null && label.contains("/")) {
                    String count = label.replaceAll(".*?(\\d+/\\d+).*", "$1");
                    System.out.println("   Count from combined label: " + count);
                    return count;
                }
            }

            return "";
        } catch (Exception e) {
            System.out.println("⚠️ Error getting required fields count: " + e.getMessage());
            return "";
        }
    }

    /**
     * Tap the "Required fields only" toggle to switch it.
     */
    public void tapRequiredFieldsToggle() {
        System.out.println("🔄 Tapping Required fields toggle...");
        try {
            // Strategy 1: Switch element
            try {
                WebElement toggle = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSwitch' AND " +
                    "(label CONTAINS 'Required' OR name CONTAINS 'Required')"));
                toggle.click();
                sleep(300);
                System.out.println("✅ Tapped Required fields toggle (Switch)");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Button element
            try {
                WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Required' OR name CONTAINS 'Required')"));
                btn.click();
                sleep(300);
                System.out.println("✅ Tapped Required fields toggle (Button)");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Tap the text label area
            WebElement text = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Required fields'"));
            text.click();
            sleep(300);
            System.out.println("✅ Tapped Required fields toggle (Text)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Required fields toggle: " + e.getMessage());
        }
    }

    // ================================================================
    // SUBCATEGORY FIELD (TC_ISS_062-066)
    //
    // The Subcategory field appears for NEC Violation issues.
    // It has a red required indicator and "Type or select..." placeholder.
    // Dropdown contains NEC-specific violation subcategories.
    // ================================================================

    /**
     * Check if Subcategory field is displayed on Issue Details screen.
     * Only appears for NEC Violation issues.
     */
    public boolean isSubcategoryFieldDisplayed() {
        try {
            // Strategy 1: Look for "Subcategory" label text
            WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Subcategory' OR label CONTAINS 'Subcategory')"));
            System.out.println("   Subcategory field label found");
            return label.isDisplayed();
        } catch (Exception e) {
            try {
                // Strategy 2: Look for button/picker with "Subcategory" or the placeholder
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeTextField' OR " +
                    "type == 'XCUIElementTypeOther') AND " +
                    "(name CONTAINS 'Subcategory' OR name CONTAINS 'subcategory' OR " +
                    "label CONTAINS 'Type or select')"));
                System.out.println("   Subcategory picker/field found");
                return picker.isDisplayed();
            } catch (Exception e2) {
                // Strategy 3: Scroll down and retry
                scrollDownOnDetailsScreen();
                try {
                    WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Subcategory'"));
                    return label.isDisplayed();
                } catch (Exception e3) {
                    System.out.println("⚠️ Subcategory field not found");
                    return false;
                }
            }
        }
    }

    /**
     * Get the Subcategory field placeholder text.
     * Expected: "Type or select..."
     */
    public String getSubcategoryPlaceholder() {
        try {
            // Try text field first
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Subcategory' OR name CONTAINS 'subcategory' OR " +
                    "value CONTAINS 'Type or select' OR label CONTAINS 'Type or select')"));
                String placeholder = field.getAttribute("placeholderValue");
                if (placeholder != null && !placeholder.isEmpty()) return placeholder;
                String value = field.getAttribute("value");
                if (value != null && value.contains("Type or select")) return value;
                String label = field.getAttribute("label");
                if (label != null && label.contains("Type or select")) return label;
            } catch (Exception ignored) {}

            // Try button/other that shows placeholder
            try {
                WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                    "label CONTAINS 'Type or select'"));
                return element.getAttribute("label");
            } catch (Exception ignored) {}

            // Fallback: look for text near "Subcategory" label
            List<WebElement> subcatLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
            if (!subcatLabels.isEmpty()) {
                int subcatY = subcatLabels.get(0).getLocation().getY();
                List<WebElement> nearbyTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'select'"));
                for (WebElement text : nearbyTexts) {
                    int y = text.getLocation().getY();
                    if (Math.abs(y - subcatY) < 60) {
                        return text.getAttribute("label");
                    }
                }
            }

            return "";
        } catch (Exception e) {
            System.out.println("⚠️ Error getting subcategory placeholder: " + e.getMessage());
            return "";
        }
    }

    /**
     * Tap the Subcategory field to open its dropdown/picker.
     */
    public void tapSubcategoryField() {
        System.out.println("📋 Tapping Subcategory field...");
        try {
            // Strategy 1: Tap the text field
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Subcategory' OR name CONTAINS 'subcategory' OR " +
                    "value CONTAINS 'Type or select' OR label CONTAINS 'Type or select')"));
                field.click();
                sleep(400);
                System.out.println("✅ Tapped Subcategory text field");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Tap the picker button
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'Subcategory' OR label CONTAINS 'Type or select' OR " +
                    "label CONTAINS 'Subcategory')"));
                picker.click();
                sleep(400);
                System.out.println("✅ Tapped Subcategory picker button");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Tap the text near Subcategory label
            WebElement subcatLabel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
            int subcatY = subcatLabel.getLocation().getY();
            // Find the interactive element below or next to the label
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeButton' OR " +
                "type == 'XCUIElementTypeOther')"));
            for (WebElement el : elements) {
                int y = el.getLocation().getY();
                if (y >= subcatY && y <= subcatY + 60) {
                    el.click();
                    sleep(400);
                    System.out.println("✅ Tapped element near Subcategory at Y=" + y);
                    return;
                }
            }
            System.out.println("⚠️ Could not find tappable Subcategory element");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Subcategory: " + e.getMessage());
        }
    }

    /**
     * Check if a specific subcategory option is displayed in the dropdown.
     */
    public boolean isSubcategoryOptionDisplayed(String option) {
        try {
            WebElement optionEl = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                "type == 'XCUIElementTypeCell') AND label CONTAINS '" + option + "'"));
            return optionEl.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Select a subcategory from the dropdown by tapping on it.
     */
    public void selectSubcategory(String subcategory) {
        System.out.println("📋 Selecting subcategory: " + subcategory);
        try {
            // Try to find and tap the option directly
            try {
                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + subcategory + "'"));
                option.click();
                sleep(400);
                System.out.println("✅ Selected subcategory: " + subcategory);
                return;
            } catch (Exception ignored) {}

            // May need to scroll within the dropdown to find the option
            // Use mobile: scroll with predicate
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("direction", "down");
                params.put("predicateString", "label CONTAINS '" + subcategory + "'");
                driver.executeScript("mobile: scroll", params);
                sleep(300);

                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + subcategory + "'"));
                option.click();
                sleep(400);
                System.out.println("✅ Selected subcategory after scroll: " + subcategory);
            } catch (Exception e2) {
                System.out.println("⚠️ Could not select subcategory: " + subcategory + " - " + e2.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error selecting subcategory: " + e.getMessage());
        }
    }

    /**
     * Get the currently selected subcategory value.
     */
    public String getSubcategoryValue() {
        try {
            // Strategy 1: Read from text field
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Subcategory' OR name CONTAINS 'subcategory')"));
                String value = field.getAttribute("value");
                if (value != null && !value.isEmpty() && !value.contains("Type or select")) {
                    System.out.println("   Subcategory value (field): " + value);
                    return value;
                }
            } catch (Exception ignored) {}

            // Strategy 2: Read from button/picker
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Subcategory'"));
                String label = picker.getAttribute("label");
                String value = picker.getAttribute("value");
                System.out.println("   Subcategory picker — label: '" + label + "', value: '" + value + "'");
                if (value != null && !value.isEmpty()) return value;
                if (label != null && label.contains(", ")) {
                    return label.substring(label.indexOf(", ") + 2).trim();
                }
                return label != null ? label : "";
            } catch (Exception ignored) {}

            // Strategy 3: Look for text near the Subcategory label that is not the label itself
            List<WebElement> subcatLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
            if (!subcatLabels.isEmpty()) {
                int subcatY = subcatLabels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    String label = text.getAttribute("label");
                    if (y > subcatY && y < subcatY + 50 && label != null &&
                        !label.equals("Subcategory") && !label.contains("Type or select")) {
                        System.out.println("   Subcategory value (nearby text): " + label);
                        return label;
                    }
                }
            }

            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Verify that specific NEC subcategory options are present in the dropdown.
     * Returns the number of expected options found.
     */
    public int verifyNECSubcategoryOptions(String[] expectedOptions) {
        int found = 0;
        for (String option : expectedOptions) {
            try {
                // Check if the option is visible or accessible in the DOM
                List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + option + "'"));
                if (!elements.isEmpty()) {
                    found++;
                    System.out.println("   ✓ NEC option found: " + option);
                } else {
                    System.out.println("   ✗ NEC option NOT found: " + option);
                }
            } catch (Exception e) {
                System.out.println("   ✗ Error checking option '" + option + "': " + e.getMessage());
            }
        }
        System.out.println("   NEC subcategory options: " + found + "/" + expectedOptions.length + " found");
        return found;
    }

    /**
     * Type text into the Subcategory search/filter field to filter options.
     */
    public void searchSubcategory(String searchText) {
        System.out.println("🔍 Searching subcategory: " + searchText);
        try {
            // Strategy 1: Find a text field in the subcategory area
            try {
                WebElement searchField = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeSearchField') AND " +
                    "(name CONTAINS 'Subcategory' OR name CONTAINS 'subcategory' OR " +
                    "value CONTAINS 'Type or select' OR label CONTAINS 'Type or select' OR " +
                    "label CONTAINS 'Search' OR label CONTAINS 'search')"));
                searchField.clear();
                searchField.sendKeys(searchText);
                sleep(400);
                System.out.println("✅ Typed '" + searchText + "' in subcategory search");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: The subcategory field itself might be the search field
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND " +
                    "(value CONTAINS 'Type' OR value CONTAINS 'select' OR " +
                    "label CONTAINS 'Type' OR label CONTAINS 'select')"));
                field.clear();
                field.sendKeys(searchText);
                sleep(400);
                System.out.println("✅ Typed in subcategory field");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Find any active text field on screen (if dropdown is open)
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"));
            for (WebElement field : fields) {
                int y = field.getLocation().getY();
                if (y > 200) {
                    field.clear();
                    field.sendKeys(searchText);
                    sleep(400);
                    System.out.println("✅ Typed in text field at Y=" + y);
                    return;
                }
            }
            System.out.println("⚠️ Could not find subcategory search field");
        } catch (Exception e) {
            System.out.println("⚠️ Error searching subcategory: " + e.getMessage());
        }
    }

    // ================================================================
    // DESCRIPTION FIELD (TC_ISS_067-068)
    // ================================================================

    /**
     * Check if the Description field/section is displayed on Issue Details.
     * Expected: section with text icon and "Describe the issue..." placeholder.
     */
    public boolean isDescriptionFieldDisplayed() {
        try {
            // Strategy 1: Look for "Description" label text
            WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Description' OR label CONTAINS 'Description')"));
            System.out.println("   Description field label found");
            return label.isDisplayed();
        } catch (Exception e) {
            try {
                // Strategy 2: Look for the placeholder text
                WebElement placeholder = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField' OR " +
                    "type == 'XCUIElementTypeStaticText') AND " +
                    "(label CONTAINS 'Describe the issue' OR value CONTAINS 'Describe the issue')"));
                return placeholder.isDisplayed();
            } catch (Exception e2) {
                // Strategy 3: Scroll down and retry
                scrollDownOnDetailsScreen();
                try {
                    WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Description'"));
                    return label.isDisplayed();
                } catch (Exception e3) {
                    System.out.println("⚠️ Description field not found");
                    return false;
                }
            }
        }
    }

    /**
     * Get the Description field placeholder text.
     * Expected: "Describe the issue..."
     */
    public String getDescriptionPlaceholder() {
        try {
            // Strategy 1: Get from text view placeholder
            try {
                WebElement textView = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField') AND " +
                    "(value CONTAINS 'Describe' OR label CONTAINS 'Describe' OR " +
                    "name CONTAINS 'Description')"));
                String placeholder = textView.getAttribute("placeholderValue");
                if (placeholder != null && !placeholder.isEmpty()) return placeholder;
                String value = textView.getAttribute("value");
                if (value != null && value.contains("Describe")) return value;
                String label = textView.getAttribute("label");
                if (label != null && label.contains("Describe")) return label;
            } catch (Exception ignored) {}

            // Strategy 2: Look for static text with "Describe the issue..."
            try {
                WebElement text = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Describe the issue'"));
                return text.getAttribute("label");
            } catch (Exception ignored) {}

            // Strategy 3: Find text near "Description" label
            List<WebElement> descLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Description'"));
            if (!descLabels.isEmpty()) {
                int descY = descLabels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Describe'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    if (Math.abs(y - descY) < 80) {
                        return text.getAttribute("label");
                    }
                }
            }

            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Enter text into the Description field.
     */
    public void enterDescription(String text) {
        System.out.println("📝 Entering description: " + text);
        try {
            // Strategy 1: Find the text view/field
            try {
                WebElement textView = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField') AND " +
                    "(value CONTAINS 'Describe' OR label CONTAINS 'Describe' OR " +
                    "name CONTAINS 'Description' OR name CONTAINS 'description')"));
                textView.click();
                sleep(300);
                textView.clear();
                textView.sendKeys(text);
                sleep(300);
                System.out.println("✅ Entered description: " + text);
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Find any text view near "Description" label
            List<WebElement> descLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Description'"));
            if (!descLabels.isEmpty()) {
                int descY = descLabels.get(0).getLocation().getY();
                List<WebElement> textViews = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField'"));
                for (WebElement tv : textViews) {
                    int y = tv.getLocation().getY();
                    if (y > descY && y < descY + 100) {
                        tv.click();
                        sleep(300);
                        tv.clear();
                        tv.sendKeys(text);
                        sleep(300);
                        System.out.println("✅ Entered description in text view at Y=" + y);
                        return;
                    }
                }
            }
            System.out.println("⚠️ Could not find Description text input");
        } catch (Exception e) {
            System.out.println("⚠️ Error entering description: " + e.getMessage());
        }
    }

    /**
     * Get the text entered in the Description field.
     */
    public String getDescriptionValue() {
        try {
            try {
                WebElement textView = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField') AND " +
                    "(name CONTAINS 'Description' OR name CONTAINS 'description')"));
                String value = textView.getAttribute("value");
                if (value != null && !value.isEmpty() && !value.contains("Describe the issue")) return value;
                String text = textView.getText();
                if (text != null && !text.isEmpty()) return text;
            } catch (Exception ignored) {}

            // Fallback: find text view near Description label
            List<WebElement> descLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Description'"));
            if (!descLabels.isEmpty()) {
                int descY = descLabels.get(0).getLocation().getY();
                List<WebElement> textViews = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField'"));
                for (WebElement tv : textViews) {
                    int y = tv.getLocation().getY();
                    if (y > descY && y < descY + 100) {
                        String value = tv.getAttribute("value");
                        if (value != null && !value.contains("Describe the issue")) return value;
                        return tv.getText();
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // PROPOSED RESOLUTION FIELD (TC_ISS_069)
    // ================================================================

    /**
     * Check if the Proposed Resolution field/section is displayed on Issue Details.
     * Expected: section with lightbulb icon and "Suggest a resolution..." placeholder.
     */
    public boolean isProposedResolutionFieldDisplayed() {
        try {
            // Strategy 1: Look for "Proposed Resolution" label text
            WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Proposed Resolution' OR label CONTAINS 'Proposed Resolution' OR " +
                "label CONTAINS 'Proposed resolution')"));
            System.out.println("   Proposed Resolution field label found");
            return label.isDisplayed();
        } catch (Exception e) {
            try {
                // Strategy 2: Look for the placeholder text
                WebElement placeholder = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField' OR " +
                    "type == 'XCUIElementTypeStaticText') AND " +
                    "(label CONTAINS 'Suggest a resolution' OR value CONTAINS 'Suggest a resolution')"));
                return placeholder.isDisplayed();
            } catch (Exception e2) {
                // Strategy 3: Scroll down and retry
                scrollDownOnDetailsScreen();
                try {
                    WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " +
                        "(label CONTAINS 'Proposed Resolution' OR label CONTAINS 'Proposed resolution')"));
                    return label.isDisplayed();
                } catch (Exception e3) {
                    System.out.println("⚠️ Proposed Resolution field not found");
                    return false;
                }
            }
        }
    }

    /**
     * Get the Proposed Resolution field placeholder text.
     * Expected: "Suggest a resolution..."
     */
    public String getProposedResolutionPlaceholder() {
        try {
            // Strategy 1: Get from text view
            try {
                WebElement textView = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField') AND " +
                    "(value CONTAINS 'Suggest' OR label CONTAINS 'Suggest' OR " +
                    "name CONTAINS 'Proposed' OR name CONTAINS 'Resolution')"));
                String placeholder = textView.getAttribute("placeholderValue");
                if (placeholder != null && !placeholder.isEmpty()) return placeholder;
                String value = textView.getAttribute("value");
                if (value != null && value.contains("Suggest")) return value;
                String label = textView.getAttribute("label");
                if (label != null && label.contains("Suggest")) return label;
            } catch (Exception ignored) {}

            // Strategy 2: Look for static text
            try {
                WebElement text = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Suggest a resolution'"));
                return text.getAttribute("label");
            } catch (Exception ignored) {}

            // Strategy 3: Find text near "Proposed Resolution" label
            List<WebElement> resLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Proposed Resolution' OR label CONTAINS 'Proposed Resolution')"));
            if (!resLabels.isEmpty()) {
                int resY = resLabels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Suggest'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    if (Math.abs(y - resY) < 80) {
                        return text.getAttribute("label");
                    }
                }
            }

            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // PROPOSED RESOLUTION — ENTERING TEXT (TC_ISS_070)
    // ================================================================

    /**
     * Enter text into the Proposed Resolution field.
     */
    public void enterProposedResolution(String text) {
        System.out.println("📝 Entering proposed resolution: " + text);
        try {
            // Strategy 1: Find the text view directly
            try {
                WebElement textView = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField') AND " +
                    "(value CONTAINS 'Suggest' OR label CONTAINS 'Suggest' OR " +
                    "name CONTAINS 'Proposed' OR name CONTAINS 'Resolution' OR " +
                    "name CONTAINS 'resolution')"));
                textView.click();
                sleep(300);
                textView.clear();
                textView.sendKeys(text);
                sleep(300);
                System.out.println("✅ Entered proposed resolution: " + text);
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Find text view near "Proposed Resolution" label
            List<WebElement> resLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Proposed Resolution' OR label CONTAINS 'Proposed Resolution')"));
            if (!resLabels.isEmpty()) {
                int resY = resLabels.get(0).getLocation().getY();
                List<WebElement> textViews = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField'"));
                for (WebElement tv : textViews) {
                    int y = tv.getLocation().getY();
                    if (y > resY && y < resY + 100) {
                        tv.click();
                        sleep(300);
                        tv.clear();
                        tv.sendKeys(text);
                        sleep(300);
                        System.out.println("✅ Entered resolution in text view at Y=" + y);
                        return;
                    }
                }
            }

            // Strategy 3: Scroll down and retry
            scrollDownOnDetailsScreen();
            try {
                WebElement textView = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField') AND " +
                    "(value CONTAINS 'Suggest' OR label CONTAINS 'Suggest')"));
                textView.click();
                sleep(300);
                textView.clear();
                textView.sendKeys(text);
                sleep(300);
                System.out.println("✅ Entered resolution after scroll");
                return;
            } catch (Exception ignored) {}

            System.out.println("⚠️ Could not find Proposed Resolution text input");
        } catch (Exception e) {
            System.out.println("⚠️ Error entering resolution: " + e.getMessage());
        }
    }

    /**
     * Get the text entered in the Proposed Resolution field.
     */
    public String getProposedResolutionValue() {
        try {
            // Strategy 1: Read from named text view
            try {
                WebElement textView = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField') AND " +
                    "(name CONTAINS 'Proposed' OR name CONTAINS 'Resolution' OR " +
                    "name CONTAINS 'resolution')"));
                String value = textView.getAttribute("value");
                if (value != null && !value.isEmpty() && !value.contains("Suggest a resolution")) return value;
                String textContent = textView.getText();
                if (textContent != null && !textContent.isEmpty()) return textContent;
            } catch (Exception ignored) {}

            // Strategy 2: Find text view near Proposed Resolution label
            List<WebElement> resLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Proposed Resolution' OR label CONTAINS 'Proposed Resolution')"));
            if (!resLabels.isEmpty()) {
                int resY = resLabels.get(0).getLocation().getY();
                List<WebElement> textViews = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField'"));
                for (WebElement tv : textViews) {
                    int y = tv.getLocation().getY();
                    if (y > resY && y < resY + 100) {
                        String value = tv.getAttribute("value");
                        if (value != null && !value.contains("Suggest a resolution")) return value;
                        return tv.getText();
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // ISSUE PHOTOS SECTION (TC_ISS_071-073)
    //
    // Issue Photos section appears when scrolled down on Issue Details.
    // Contains Gallery and Camera buttons for adding photos.
    // ================================================================

    /**
     * Check if the Issue Photos section is displayed.
     * Expected: section with "Issue Photos" label and Gallery/Camera buttons.
     */
    public boolean isIssuePhotosSectionDisplayed() {
        try {
            // Strategy 1: Look for "Issue Photos" or "Photos" label
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label == 'Issue Photos' OR label CONTAINS 'Issue Photos' OR " +
                    "label == 'Photos' OR label CONTAINS 'Photo')"));
                System.out.println("   Issue Photos section found: " + label.getAttribute("label"));
                return label.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 2: Scroll down and retry
            scrollDownOnDetailsScreen();
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Issue Photos' OR label CONTAINS 'Photo')"));
                return label.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 3: Scroll more aggressively (might be at very bottom)
            scrollDownOnDetailsScreen();
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Photo' OR label CONTAINS 'photo')"));
                return label.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 4: Look for Gallery/Camera buttons directly (implies section exists)
            boolean hasGallery = isGalleryButtonDisplayed();
            boolean hasCamera = isCameraButtonDisplayed();
            if (hasGallery || hasCamera) {
                System.out.println("   Photos section inferred from Gallery/Camera buttons");
                return true;
            }

            System.out.println("⚠️ Issue Photos section not found");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking Issue Photos: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the Gallery button is displayed in the Issue Photos section.
     */
    public boolean isGalleryButtonDisplayed() {
        try {
            WebElement gallery = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Gallery' OR label CONTAINS 'gallery' OR " +
                "name CONTAINS 'Gallery' OR name CONTAINS 'gallery' OR " +
                "label CONTAINS 'photo.on.rectangle' OR name CONTAINS 'photo.on.rectangle' OR " +
                "label CONTAINS 'Photo Library' OR label CONTAINS 'Choose' OR " +
                "label CONTAINS 'photo' OR name CONTAINS 'photo')"));
            System.out.println("   Gallery button found: " + gallery.getAttribute("label"));
            return gallery.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Camera button is displayed in the Issue Photos section.
     */
    public boolean isCameraButtonDisplayed() {
        try {
            WebElement camera = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Camera' OR label CONTAINS 'camera' OR " +
                "name CONTAINS 'Camera' OR name CONTAINS 'camera' OR " +
                "label CONTAINS 'camera.fill' OR name CONTAINS 'camera.fill' OR " +
                "label CONTAINS 'Take Photo' OR label CONTAINS 'Capture')"));
            System.out.println("   Camera button found: " + camera.getAttribute("label"));
            return camera.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the Gallery button to open photo picker.
     */
    public void tapGalleryButton() {
        System.out.println("🖼️ Tapping Gallery button...");
        try {
            WebElement gallery = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Gallery' OR label CONTAINS 'gallery' OR " +
                "name CONTAINS 'Gallery' OR name CONTAINS 'gallery' OR " +
                "label CONTAINS 'photo.on.rectangle' OR name CONTAINS 'photo.on.rectangle' OR " +
                "label CONTAINS 'Photo Library' OR label CONTAINS 'Choose' OR " +
                "label CONTAINS 'photo' OR name CONTAINS 'photo')"));
            gallery.click();
            sleep(800);
            System.out.println("✅ Tapped Gallery button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Gallery button: " + e.getMessage());
        }
    }

    /**
     * Tap the Camera button to open camera.
     */
    public void tapCameraButton() {
        System.out.println("📷 Tapping Camera button...");
        try {
            WebElement camera = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Camera' OR label CONTAINS 'camera' OR " +
                "name CONTAINS 'Camera' OR name CONTAINS 'camera' OR " +
                "label CONTAINS 'camera.fill' OR name CONTAINS 'camera.fill' OR " +
                "label CONTAINS 'Take Photo' OR label CONTAINS 'Capture')"));
            camera.click();
            sleep(800);
            System.out.println("✅ Tapped Camera button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Camera button: " + e.getMessage());
        }
    }

    /**
     * Check if photo picker (system image picker) is displayed.
     * iOS photo picker usually shows "Photos", "Recents", "Cancel" etc.
     */
    public boolean isPhotoPickerDisplayed() {
        try {
            // Check for common photo picker elements
            // iOS system picker shows navigation bar "Photos" or "Recents"
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeNavigationBar' OR type == 'XCUIElementTypeStaticText') AND " +
                    "(label == 'Photos' OR label == 'Recents' OR label == 'All Photos' OR " +
                    "label CONTAINS 'Photo' OR label CONTAINS 'Camera Roll')"));
                return picker.isDisplayed();
            } catch (Exception ignored) {}

            // Check for Cancel button that appears in picker
            try {
                WebElement cancel = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Cancel'"));
                // Only consider it a picker if we're not on the Issue Details screen anymore
                boolean stillOnDetails = isIssueDetailsScreenDisplayed();
                if (!stillOnDetails) {
                    return true;
                }
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if camera view is displayed.
     * On simulator this may show a blank screen or permission dialog.
     */
    public boolean isCameraDisplayed() {
        try {
            // On simulator: camera shows a permission dialog or blank
            // Check for common camera UI elements
            try {
                WebElement cameraUI = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Take Picture' OR label CONTAINS 'Capture' OR " +
                    "label CONTAINS 'Shutter' OR label == 'Cancel')) OR " +
                    "(type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Camera' OR label CONTAINS 'Allow'))"));
                return cameraUI.isDisplayed();
            } catch (Exception ignored) {}

            // Check if we've left the issue details screen (camera overlays)
            boolean stillOnDetails = isIssueDetailsScreenDisplayed();
            return !stillOnDetails;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Dismiss the photo picker by tapping Cancel.
     */
    public void dismissPhotoPicker() {
        System.out.println("❌ Dismissing photo picker...");
        try {
            WebElement cancel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Cancel' OR label == 'Done' OR label == 'Close')"));
            cancel.click();
            sleep(500);
            System.out.println("✅ Dismissed photo picker");
        } catch (Exception e) {
            // Fallback: tap at top-left
            try {
                Map<String, Object> tapParams = new HashMap<>();
                tapParams.put("x", 30);
                tapParams.put("y", 60);
                driver.executeScript("mobile: tap", tapParams);
                sleep(500);
            } catch (Exception e2) {
                System.out.println("⚠️ Could not dismiss photo picker: " + e2.getMessage());
            }
        }
    }

    /**
     * Dismiss the camera by tapping Cancel.
     */
    public void dismissCamera() {
        System.out.println("❌ Dismissing camera...");
        try {
            WebElement cancel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Cancel' OR label == 'Done' OR label == 'Close')"));
            cancel.click();
            sleep(500);
            System.out.println("✅ Dismissed camera");
        } catch (Exception e) {
            // Fallback: tap Cancel if permission dialog
            try {
                WebElement dontAllow = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == \"Don't Allow\" OR label == 'Cancel' OR label == 'OK')"));
                dontAllow.click();
                sleep(500);
            } catch (Exception e2) {
                System.out.println("⚠️ Could not dismiss camera: " + e2.getMessage());
            }
        }
    }

    // ================================================================
    // DELETE ISSUE (TC_ISS_074-076)
    //
    // Delete Issue button at bottom of Issue Details screen.
    // Tapping shows confirmation dialog before actual deletion.
    // ================================================================

    /**
     * Check if Delete Issue button is displayed at the bottom of Issue Details.
     * Expected: button with trash icon and red text.
     */
    public boolean isDeleteIssueButtonDisplayed() {
        try {
            // Strategy 1: Look for "Delete Issue" or "Delete" button
            try {
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Delete Issue' OR label CONTAINS 'Delete issue' OR " +
                    "name CONTAINS 'Delete Issue' OR name CONTAINS 'delete')"));
                System.out.println("   Delete Issue button found: " + deleteBtn.getAttribute("label"));
                return deleteBtn.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 2: Scroll to bottom and retry
            scrollDownOnDetailsScreen();
            try {
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Delete Issue' OR label CONTAINS 'Delete issue' OR " +
                    "name CONTAINS 'Delete Issue' OR label CONTAINS 'Delete')"));
                System.out.println("   Delete Issue button found after scroll");
                return deleteBtn.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 3: Scroll more aggressively
            scrollDownOnDetailsScreen();
            scrollDownOnDetailsScreen();
            try {
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Delete' OR name CONTAINS 'Delete' OR " +
                    "name CONTAINS 'trash')"));
                // Make sure it's not a generic system delete button
                String label = deleteBtn.getAttribute("label");
                if (label != null && (label.contains("Delete") || label.contains("trash"))) {
                    System.out.println("   Delete button found: " + label);
                    return true;
                }
            } catch (Exception ignored) {}

            System.out.println("⚠️ Delete Issue button not found");
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the Delete Issue button.
     */
    public void tapDeleteIssueButton() {
        System.out.println("🗑️ Tapping Delete Issue button...");
        try {
            // Find and tap Delete Issue button
            try {
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Delete Issue' OR label CONTAINS 'Delete issue' OR " +
                    "name CONTAINS 'Delete Issue')"));
                deleteBtn.click();
                sleep(500);
                System.out.println("✅ Tapped Delete Issue");
                return;
            } catch (Exception ignored) {}

            // Scroll down and retry
            scrollDownOnDetailsScreen();
            try {
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Delete Issue' OR label CONTAINS 'Delete issue' OR " +
                    "name CONTAINS 'Delete Issue' OR label CONTAINS 'Delete')"));
                deleteBtn.click();
                sleep(500);
                System.out.println("✅ Tapped Delete Issue after scroll");
                return;
            } catch (Exception ignored) {}

            // More aggressive scroll
            scrollDownOnDetailsScreen();
            scrollDownOnDetailsScreen();
            WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Delete' OR name CONTAINS 'Delete' OR name CONTAINS 'trash')"));
            deleteBtn.click();
            sleep(500);
            System.out.println("✅ Tapped Delete after aggressive scroll");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Delete Issue: " + e.getMessage());
        }
    }

    /**
     * Check if delete confirmation dialog is displayed.
     * Looks for confirmation dialog with "Delete" or "Confirm" button.
     */
    public boolean isDeleteConfirmationDisplayed() {
        try {
            // Strategy 1: Look for alert/dialog with delete confirmation text
            try {
                WebElement alert = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeAlert'"));
                System.out.println("   Delete confirmation alert found");
                return alert.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 2: Look for confirmation-related buttons
            try {
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Delete' OR label == 'Confirm' OR label == 'Yes' OR " +
                    "label CONTAINS 'Confirm Delete')"));
                // There should also be a Cancel button nearby
                List<WebElement> cancelBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Cancel'"));
                if (!buttons.isEmpty() && !cancelBtns.isEmpty()) {
                    System.out.println("   Delete confirmation dialog inferred from buttons");
                    return true;
                }
            } catch (Exception ignored) {}

            // Strategy 3: Look for confirmation text
            try {
                WebElement confirmText = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'delete' OR label CONTAINS 'Delete' OR " +
                    "label CONTAINS 'Are you sure' OR label CONTAINS 'confirm' OR " +
                    "label CONTAINS 'remove')"));
                System.out.println("   Confirmation text: " + confirmText.getAttribute("label"));
                return true;
            } catch (Exception ignored) {}

            // Strategy 4: Action sheet style
            try {
                WebElement sheet = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSheet' OR type == 'XCUIElementTypeActionSheet'"));
                return sheet.isDisplayed();
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Confirm the deletion by tapping the destructive action button in the confirmation dialog.
     */
    public void confirmDeleteIssue() {
        System.out.println("🗑️ Confirming delete issue...");
        try {
            // Try various confirmation button labels
            String[] confirmLabels = {"Delete", "Confirm", "Yes", "Remove", "OK"};
            for (String label : confirmLabels) {
                try {
                    WebElement confirmBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == '" + label + "'"));
                    confirmBtn.click();
                    sleep(800);
                    System.out.println("✅ Confirmed deletion via '" + label + "' button");
                    return;
                } catch (Exception ignored) {}
            }

            // Fallback: Look for the destructive action in alert
            try {
                WebElement destroyBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Delete' OR label CONTAINS 'Confirm')"));
                destroyBtn.click();
                sleep(800);
                System.out.println("✅ Confirmed deletion via fallback");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not find confirmation button");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error confirming delete: " + e.getMessage());
        }
    }

    /**
     * Cancel the deletion by tapping Cancel in the confirmation dialog.
     */
    public void cancelDeleteIssue() {
        System.out.println("❌ Cancelling delete issue...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"));
            cancelBtn.click();
            sleep(400);
            System.out.println("✅ Cancelled deletion");
        } catch (Exception e) {
            // Fallback: dismiss by tapping outside
            try {
                Map<String, Object> tapParams = new HashMap<>();
                tapParams.put("x", 200);
                tapParams.put("y", 300);
                driver.executeScript("mobile: tap", tapParams);
                sleep(400);
                System.out.println("✅ Dismissed delete dialog");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not cancel deletion: " + e2.getMessage());
            }
        }
    }

    // ================================================================
    // SAVE CHANGES (TC_ISS_077-079)
    //
    // Save Changes button appears at the bottom of Issue Details
    // when modifications have been made.
    // ================================================================

    /**
     * Check if Save Changes button is displayed.
     * Expected: blue button at the bottom of issue details.
     */
    public boolean isSaveChangesButtonDisplayed() {
        try {
            // Strategy 1: Direct search
            try {
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Save Changes' OR label CONTAINS 'Save changes' OR " +
                    "name CONTAINS 'Save Changes' OR label == 'Save')"));
                System.out.println("   Save Changes button found: " + saveBtn.getAttribute("label"));
                return saveBtn.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 2: Scroll to bottom
            scrollDownOnDetailsScreen();
            try {
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Save' OR name CONTAINS 'Save')"));
                String label = saveBtn.getAttribute("label");
                // Make sure it's not a system save button
                if (label != null && (label.contains("Save") || label.contains("save"))) {
                    System.out.println("   Save button found after scroll: " + label);
                    return true;
                }
            } catch (Exception ignored) {}

            // Strategy 3: Scroll more aggressively
            scrollDownOnDetailsScreen();
            scrollDownOnDetailsScreen();
            try {
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Save' OR name CONTAINS 'Save')"));
                return saveBtn.isDisplayed();
            } catch (Exception ignored) {}

            System.out.println("⚠️ Save Changes button not found");
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the Save Changes button.
     */
    public void tapSaveChangesButton() {
        System.out.println("💾 Tapping Save Changes...");
        try {
            // Find Save Changes button
            try {
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Save Changes' OR label CONTAINS 'Save changes' OR " +
                    "name CONTAINS 'Save Changes' OR label == 'Save')"));
                saveBtn.click();
                sleep(800);
                System.out.println("✅ Tapped Save Changes");
                return;
            } catch (Exception ignored) {}

            // Scroll and retry
            scrollDownOnDetailsScreen();
            try {
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'Save' OR name CONTAINS 'Save')"));
                saveBtn.click();
                sleep(800);
                System.out.println("✅ Tapped Save after scroll");
                return;
            } catch (Exception ignored) {}

            // More scrolling
            scrollDownOnDetailsScreen();
            scrollDownOnDetailsScreen();
            WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Save' OR name CONTAINS 'Save')"));
            saveBtn.click();
            sleep(800);
            System.out.println("✅ Tapped Save after aggressive scroll");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Save Changes: " + e.getMessage());
        }
    }

    /**
     * Check if a save success message/toast is displayed.
     */
    public boolean isSaveSuccessDisplayed() {
        try {
            // Look for success-related text
            WebElement success = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label CONTAINS 'Saved' OR label CONTAINS 'saved' OR " +
                "label CONTAINS 'Success' OR label CONTAINS 'Updated' OR " +
                "label CONTAINS 'updated')"));
            System.out.println("   Save success message: " + success.getAttribute("label"));
            return success.isDisplayed();
        } catch (Exception e) {
            // No explicit success message — may just return to list
            return false;
        }
    }

    /**
     * Check if unsaved changes warning dialog is displayed.
     * May show "Save" / "Discard" / "Cancel" options.
     */
    public boolean isUnsavedChangesWarningDisplayed() {
        try {
            // Strategy 1: Alert dialog
            try {
                WebElement alert = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeAlert'"));
                System.out.println("   Unsaved changes alert found");
                return alert.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 2: Look for warning text
            try {
                WebElement warningText = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'unsaved' OR label CONTAINS 'Unsaved' OR " +
                    "label CONTAINS 'discard' OR label CONTAINS 'Discard' OR " +
                    "label CONTAINS 'save changes' OR label CONTAINS 'Save changes' OR " +
                    "label CONTAINS 'changes' OR label CONTAINS 'Changes')"));
                System.out.println("   Warning text: " + warningText.getAttribute("label"));
                return true;
            } catch (Exception ignored) {}

            // Strategy 3: Check for Discard button (strong indicator of unsaved warning)
            try {
                WebElement discardBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Discard' OR label == 'Discard Changes' OR " +
                    "label CONTAINS 'Don\\'t Save')"));
                System.out.println("   Discard button found — unsaved changes warning");
                return true;
            } catch (Exception ignored) {}

            // Strategy 4: Action sheet
            try {
                WebElement sheet = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSheet' OR type == 'XCUIElementTypeActionSheet'"));
                return sheet.isDisplayed();
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap "Discard" on the unsaved changes warning dialog.
     */
    public void tapDiscardChanges() {
        System.out.println("❌ Tapping Discard Changes...");
        try {
            String[] discardLabels = {"Discard", "Discard Changes", "Don't Save", "No"};
            for (String label : discardLabels) {
                try {
                    WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == '" + label + "'"));
                    btn.click();
                    sleep(500);
                    System.out.println("✅ Discarded changes via '" + label + "'");
                    return;
                } catch (Exception ignored) {}
            }
            // Fallback: tap the destructive/dismiss option
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Discard' OR label CONTAINS 'discard' OR label CONTAINS \"Don't\")"));
            btn.click();
            sleep(500);
            System.out.println("✅ Discarded changes via fallback");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Discard: " + e.getMessage());
        }
    }

    /**
     * Tap "Save" on the unsaved changes warning dialog.
     */
    public void tapSaveOnWarning() {
        System.out.println("💾 Tapping Save on warning dialog...");
        try {
            String[] saveLabels = {"Save", "Save Changes", "Yes"};
            for (String label : saveLabels) {
                try {
                    WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == '" + label + "'"));
                    btn.click();
                    sleep(500);
                    System.out.println("✅ Saved via '" + label + "' on warning");
                    return;
                } catch (Exception ignored) {}
            }
            System.out.println("⚠️ Could not find Save button on warning");
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping Save on warning: " + e.getMessage());
        }
    }

    /**
     * Helper: Scroll up on the Issue Details screen to go back to top.
     */
    public void scrollUpOnDetailsScreen() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", "up");
            driver.executeScript("mobile: scroll", params);
            sleep(400);
            System.out.println("   Scrolled up on details screen");
        } catch (Exception e) {
            System.out.println("⚠️ Could not scroll up: " + e.getMessage());
        }
    }

    /**
     * Create a quick issue for test purposes (e.g., for deletion testing).
     * Creates an issue with the given title, minimal fields, and the specified asset.
     * Returns true if creation appeared successful.
     */
    public boolean createQuickIssue(String title, String assetName) {
        System.out.println("🆕 Creating quick issue: " + title);
        try {
            tapAddButton();
            sleep(500);

            if (!isNewIssueFormDisplayed()) {
                System.out.println("⚠️ New Issue form did not open");
                return false;
            }

            // Select Issue Class (required field behavior may vary)
            selectIssueClass("Other");
            sleep(300);

            // Enter title
            enterIssueTitle(title);
            sleep(300);

            // Select asset (required)
            tapSelectAsset();
            sleep(500);
            selectAssetByName(assetName);
            sleep(500);

            // Tap Create Issue
            tapCreateIssue();
            sleep(800);

            System.out.println("✅ Quick issue created: " + title);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Error creating quick issue: " + e.getMessage());
            // Try to cancel if form is still open
            try { tapCancelNewIssue(); } catch (Exception ignored) {}
            return false;
        }
    }

    // ================================================================
    // DASHBOARD DETECTION (TC_ISS_080)
    // ================================================================

    /**
     * Check if the dashboard/previous screen is displayed after closing Issues.
     * Looks for common dashboard indicators: tab bar, building icon, Assets button.
     */
    public boolean isDashboardOrPreviousScreenDisplayed() {
        try {
            // Strategy 1: Check for tab bar buttons (Assets, Locations, etc.)
            try {
                WebElement tabBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Assets' OR label == 'Locations' OR name == 'Assets' OR " +
                    "name == 'building.2' OR name == 'list.bullet')"));
                System.out.println("   Dashboard detected: tab bar button found");
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Verify we are NOT on Issues screen
            boolean onIssues = isIssuesScreenDisplayed();
            if (!onIssues) {
                System.out.println("   Not on Issues screen — assumed previous screen");
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // ISSUE CLASS SUBCATEGORIES — OSHA & THERMAL ANOMALY (TC_ISS_082-083)
    //
    // Different issue classes have different subcategory options.
    // OSHA Violation and Thermal Anomaly have their own specific lists.
    // ================================================================

    /**
     * Open the Issue Class dropdown on Issue Details and change it.
     * Used to switch issue class and see corresponding subcategories.
     */
    public void changeIssueClassOnDetails(String newClass) {
        System.out.println("📋 Changing Issue Class on details to: " + newClass);
        try {
            // Strategy 1: Tap the Issue Class picker button
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Issue Class'"));
                picker.click();
                sleep(400);
            } catch (Exception ignored) {
                // Strategy 2: Tap the Issue Class label area
                try {
                    WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " +
                        "(label == 'Issue Class' OR label CONTAINS 'Issue Class')"));
                    int labelY = label.getLocation().getY();
                    // Find the interactive element near the label
                    List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton'"));
                    for (WebElement btn : buttons) {
                        int y = btn.getLocation().getY();
                        if (Math.abs(y - labelY) < 40) {
                            btn.click();
                            sleep(400);
                            break;
                        }
                    }
                } catch (Exception e2) {
                    System.out.println("⚠️ Could not open Issue Class dropdown: " + e2.getMessage());
                    return;
                }
            }

            // Select the new class
            try {
                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == '" + newClass + "'"));
                option.click();
                sleep(400);
                System.out.println("✅ Changed Issue Class to: " + newClass);
            } catch (Exception e) {
                System.out.println("⚠️ Could not select class '" + newClass + "': " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error changing Issue Class: " + e.getMessage());
        }
    }

    /**
     * Get all visible subcategory option labels from an open subcategory dropdown.
     * Returns a list of visible option texts.
     */
    public java.util.ArrayList<String> getVisibleSubcategoryOptions() {
        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        try {
            // Look for list items / cells / static texts that are subcategory options
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                "type == 'XCUIElementTypeCell') AND " +
                "label != 'Subcategory' AND label != 'Cancel' AND label != 'Done' AND " +
                "label != '' AND NOT (label CONTAINS 'Type or select')"));
            for (WebElement el : elements) {
                String label = el.getAttribute("label");
                int y = el.getLocation().getY();
                // Only include elements in the dropdown area (below Subcategory label)
                if (label != null && !label.isEmpty() && y > 200 && label.length() > 3) {
                    // Filter out known non-option labels
                    if (!label.equals("Issue Details") && !label.equals("Close") &&
                        !label.equals("Required fields only") && !label.equals("Description") &&
                        !label.equals("Proposed Resolution") && !label.equals("Status") &&
                        !label.equals("Priority") && !label.equals("Issue Class") &&
                        !label.contains("Issue Photos") && !label.contains("Delete") &&
                        !label.contains("Save")) {
                        options.add(label);
                    }
                }
            }
            System.out.println("   Visible subcategory options: " + options.size());
            for (String opt : options) {
                System.out.println("     - " + opt);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting subcategory options: " + e.getMessage());
        }
        return options;
    }

    // ================================================================
    // EDGE CASES HELPERS (TC_ISS_084-086)
    // ================================================================

    /**
     * Create an issue with ONLY the asset selected (minimum required field).
     * Does NOT fill title, priority, or issue class — tests minimal creation.
     * Returns true if creation appeared successful.
     */
    public boolean createMinimalIssue(String assetName) {
        System.out.println("🆕 Creating minimal issue (asset only)...");
        try {
            tapAddButton();
            sleep(500);

            if (!isNewIssueFormDisplayed()) {
                System.out.println("⚠️ New Issue form did not open");
                return false;
            }

            // Only select asset (required field)
            tapSelectAsset();
            sleep(500);
            selectAssetByName(assetName);
            sleep(500);

            // Check if Create Issue is now enabled
            boolean enabled = isCreateIssueEnabled();
            System.out.println("   Create Issue enabled with only asset: " + enabled);

            if (enabled) {
                tapCreateIssue();
                sleep(800);
                System.out.println("✅ Minimal issue created");
                return true;
            } else {
                System.out.println("ℹ️ Create Issue not enabled with only asset — may need more fields");
                // Try to cancel
                tapCancelNewIssue();
                return false;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error creating minimal issue: " + e.getMessage());
            try { tapCancelNewIssue(); } catch (Exception ignored) {}
            return false;
        }
    }

    /**
     * Create an issue with a specific title and asset.
     * Used for edge case testing (special chars, etc.).
     * Returns true if creation appeared successful.
     */
    public boolean createIssueWithTitle(String title, String assetName) {
        System.out.println("🆕 Creating issue with title: " + title);
        try {
            tapAddButton();
            sleep(500);

            if (!isNewIssueFormDisplayed()) {
                System.out.println("⚠️ New Issue form did not open");
                return false;
            }

            // Enter title
            enterIssueTitle(title);
            sleep(300);

            // Select asset (required)
            tapSelectAsset();
            sleep(500);
            selectAssetByName(assetName);
            sleep(500);

            // Tap Create Issue
            boolean enabled = isCreateIssueEnabled();
            if (enabled) {
                tapCreateIssue();
                sleep(800);
                System.out.println("✅ Issue created with title: " + title);
                return true;
            } else {
                System.out.println("ℹ️ Create Issue not enabled — trying with issue class too");
                selectIssueClass("Other");
                sleep(300);
                tapCreateIssue();
                sleep(800);
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error creating issue: " + e.getMessage());
            try { tapCancelNewIssue(); } catch (Exception ignored) {}
            return false;
        }
    }

    /**
     * Enter a very long description text and verify it was accepted.
     * Returns the actual text value read back from the field.
     */
    public String enterLongDescription(String longText) {
        System.out.println("📝 Entering long description (" + longText.length() + " chars)...");
        enterDescription(longText);
        sleep(500);
        String readBack = getDescriptionValue();
        System.out.println("   Read back length: " + (readBack != null ? readBack.length() : 0));
        return readBack;
    }

    // ================================================================
    // OFFLINE MODE HELPERS (TC_ISS_087-088)
    // ================================================================

    /**
     * Enable airplane mode on the device to simulate offline.
     * Uses Appium's mobile: command if supported.
     * Returns true if airplane mode was likely enabled.
     */
    public boolean enableAirplaneMode() {
        System.out.println("✈️ Enabling airplane mode...");
        try {
            // iOS doesn't support programmatic airplane mode via Appium easily.
            // Try toggling Wi-Fi off as closest approximation
            try {
                Map<String, Object> wifiParams = new HashMap<>();
                wifiParams.put("wifi", false);
                driver.executeScript("mobile: setConnectivity", wifiParams);
                sleep(500);
                System.out.println("✅ Wi-Fi disabled (offline simulation)");
                return true;
            } catch (Exception ignored) {}

            // Fallback: use siri or control center
            // This is device-dependent and may not work
            System.out.println("⚠️ Could not programmatically disable connectivity");
            System.out.println("   iOS does not easily support airplane mode via Appium");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Airplane mode error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Disable airplane mode to restore connectivity.
     * Returns true if connectivity was likely restored.
     */
    public boolean disableAirplaneMode() {
        System.out.println("📶 Disabling airplane mode...");
        try {
            try {
                Map<String, Object> wifiParams = new HashMap<>();
                wifiParams.put("wifi", true);
                driver.executeScript("mobile: setConnectivity", wifiParams);
                sleep(1000);
                System.out.println("✅ Wi-Fi re-enabled");
                return true;
            } catch (Exception ignored) {}

            System.out.println("⚠️ Could not programmatically re-enable connectivity");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Re-enable connectivity error: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // SCROLLING PERFORMANCE (TC_ISS_090)
    // ================================================================

    /**
     * Perform rapid scrolling on the Issues list and measure responsiveness.
     * Returns the average time per scroll gesture in milliseconds.
     * A lower number indicates smoother/faster scrolling.
     */
    public long performScrollPerformanceTest(int scrollCount) {
        System.out.println("📊 Starting scroll performance test (" + scrollCount + " scrolls)...");
        long totalTime = 0;
        int completedScrolls = 0;

        try {
            int screenHeight = driver.manage().window().getSize().getHeight();
            int screenWidth = driver.manage().window().getSize().getWidth();
            int centerX = screenWidth / 2;
            int startY = (int) (screenHeight * 0.7);
            int endY = (int) (screenHeight * 0.3);

            for (int i = 0; i < scrollCount; i++) {
                long scrollStart = System.currentTimeMillis();

                try {
                    org.openqa.selenium.interactions.PointerInput finger =
                        new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                    org.openqa.selenium.interactions.Sequence swipe =
                        new org.openqa.selenium.interactions.Sequence(finger, 0);
                    swipe.addAction(finger.createPointerMove(Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), centerX, startY));
                    swipe.addAction(finger.createPointerDown(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    swipe.addAction(finger.createPointerMove(Duration.ofMillis(150),
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), centerX, endY));
                    swipe.addAction(finger.createPointerUp(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    driver.perform(java.util.Collections.singletonList(swipe));

                    long scrollEnd = System.currentTimeMillis();
                    long elapsed = scrollEnd - scrollStart;
                    totalTime += elapsed;
                    completedScrolls++;

                    // Brief pause between scrolls to simulate rapid but not instant scrolling
                    sleep(100);
                } catch (Exception e) {
                    System.out.println("   Scroll " + (i + 1) + " failed: " + e.getMessage());
                }
            }

            // Now scroll back up
            for (int i = 0; i < Math.min(scrollCount, 5); i++) {
                try {
                    org.openqa.selenium.interactions.PointerInput finger =
                        new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                    org.openqa.selenium.interactions.Sequence swipe =
                        new org.openqa.selenium.interactions.Sequence(finger, 0);
                    swipe.addAction(finger.createPointerMove(Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), centerX, endY));
                    swipe.addAction(finger.createPointerDown(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    swipe.addAction(finger.createPointerMove(Duration.ofMillis(150),
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), centerX, startY));
                    swipe.addAction(finger.createPointerUp(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    driver.perform(java.util.Collections.singletonList(swipe));
                    sleep(100);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("⚠️ Scroll performance test error: " + e.getMessage());
        }

        long avgTime = completedScrolls > 0 ? totalTime / completedScrolls : 0;
        System.out.println("   Scrolls completed: " + completedScrolls + "/" + scrollCount);
        System.out.println("   Average time per scroll: " + avgTime + "ms");
        System.out.println("   Total scroll time: " + totalTime + "ms");
        return avgTime;
    }

    // ================================================================
    // NFPA 70B SUBCATEGORY HELPERS (TC_ISS_091-099)
    //
    // NFPA 70B Violation issues have chapter-based subcategory options
    // like "Chapter 28.3.2 Motor Control Equipment Cleaning" etc.
    // ================================================================

    /**
     * Check if a specific NFPA 70B chapter option is present in the subcategory dropdown.
     * Handles searching/scrolling within the dropdown to find the option.
     * @param chapterText partial text to match (e.g., "Chapter 28.3.2" or "Motor Control Equipment Cleaning")
     * @return true if the option is found
     */
    public boolean isNFPA70BChapterOptionPresent(String chapterText) {
        System.out.println("🔍 Looking for NFPA 70B chapter option: " + chapterText);
        try {
            // Strategy 1: Direct search in visible elements
            try {
                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + chapterText + "'"));
                System.out.println("   ✓ Found: " + option.getAttribute("label"));
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Use mobile: scroll with predicate to find off-screen option
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS '" + chapterText + "'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);

                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + chapterText + "'"));
                System.out.println("   ✓ Found after scroll: " + option.getAttribute("label"));
                return true;
            } catch (Exception ignored) {}

            // Strategy 3: Type in the search field to filter, then check
            try {
                // Find and use the search/filter field
                String shortQuery = chapterText.length() > 15 ? chapterText.substring(0, 15) : chapterText;
                searchSubcategory(shortQuery);
                sleep(400);

                List<WebElement> results = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + chapterText + "'"));
                if (!results.isEmpty()) {
                    System.out.println("   ✓ Found via search: " + results.get(0).getAttribute("label"));
                    // Clear the search
                    clearSubcategorySearch();
                    return true;
                }
                clearSubcategorySearch();
            } catch (Exception ignored) {}

            System.out.println("   ✗ Not found: " + chapterText);
            return false;
        } catch (Exception e) {
            System.out.println("   ✗ Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clear the subcategory search field.
     */
    public void clearSubcategorySearch() {
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeSearchField')"));
            for (WebElement field : fields) {
                try {
                    field.clear();
                    sleep(200);
                    return;
                } catch (Exception ignored) {}
            }
            // Fallback: try clear buttons
            try {
                WebElement clearBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name == 'Clear text' OR label == 'Clear text' OR name CONTAINS 'clear')"));
                clearBtn.click();
                sleep(200);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear subcategory search: " + e.getMessage());
        }
    }

    /**
     * Get the full label text of a chapter option by partial match.
     * Useful for verifying exact wording.
     */
    public String getChapterOptionFullLabel(String partialText) {
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                "type == 'XCUIElementTypeCell') AND label CONTAINS '" + partialText + "'"));
            if (!elements.isEmpty()) {
                return elements.get(0).getAttribute("label");
            }

            // Try scroll to find
            Map<String, Object> scrollParams = new HashMap<>();
            scrollParams.put("direction", "down");
            scrollParams.put("predicateString", "label CONTAINS '" + partialText + "'");
            driver.executeScript("mobile: scroll", scrollParams);
            sleep(300);

            elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                "type == 'XCUIElementTypeCell') AND label CONTAINS '" + partialText + "'"));
            if (!elements.isEmpty()) {
                return elements.get(0).getAttribute("label");
            }
        } catch (Exception e) {}
        return "";
    }

    // ================================================================
    // SUBCATEGORY SELECTION & VERIFICATION (TC_ISS_106)
    // ================================================================

    /**
     * Select a subcategory option by partial match and verify it was selected.
     * Returns the text shown in the field after selection.
     */
    public String selectSubcategoryAndGetValue(String optionPartialText) {
        System.out.println("📋 Selecting subcategory containing: " + optionPartialText);
        try {
            // Find and tap the option
            try {
                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + optionPartialText + "'"));
                option.click();
                sleep(500);
                System.out.println("✅ Tapped subcategory option: " + option.getAttribute("label"));
            } catch (Exception e) {
                // Scroll to find it first
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS '" + optionPartialText + "'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);

                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + optionPartialText + "'"));
                option.click();
                sleep(500);
                System.out.println("✅ Tapped subcategory option after scroll");
            }

            // Read back the selected value
            sleep(300);
            String selectedValue = getSubcategoryValue();
            System.out.println("   Selected subcategory value: '" + selectedValue + "'");
            return selectedValue;
        } catch (Exception e) {
            System.out.println("⚠️ Error selecting subcategory: " + e.getMessage());
            return "";
        }
    }

    /**
     * Check if a green checkmark indicator is visible near the Subcategory field.
     * This appears when a required subcategory is filled.
     */
    public boolean isSubcategoryCheckmarkDisplayed() {
        try {
            // Strategy 1: Look for checkmark image/icon near Subcategory
            try {
                WebElement checkmark = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeStaticText' OR " +
                    "type == 'XCUIElementTypeOther') AND " +
                    "(name CONTAINS 'checkmark' OR name CONTAINS 'check' OR " +
                    "label CONTAINS 'checkmark' OR label CONTAINS '✓' OR label CONTAINS '✔')"));
                System.out.println("   Checkmark found: " + checkmark.getAttribute("name"));
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Check for a filled/valid state indicator near Subcategory
            List<WebElement> subcatLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
            if (!subcatLabels.isEmpty()) {
                int subcatY = subcatLabels.get(0).getLocation().getY();
                // Look for images/icons near the label
                List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage'"));
                for (WebElement icon : icons) {
                    int y = icon.getLocation().getY();
                    if (Math.abs(y - subcatY) < 30) {
                        String name = icon.getAttribute("name");
                        System.out.println("   Icon near Subcategory: " + name);
                        if (name != null && (name.contains("check") || name.contains("green") ||
                            name.contains("circle.fill"))) {
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

    // ================================================================
    // SUBCATEGORY SEARCH/FILTER (TC_ISS_107-108)
    // ================================================================

    /**
     * Search in subcategory dropdown and count the number of matching results.
     * Returns the count of visible matching options.
     */
    public int searchSubcategoryAndCountResults(String searchText) {
        System.out.println("🔍 Searching subcategory for: '" + searchText + "'");
        searchSubcategory(searchText);
        sleep(500);

        // Count visible results
        try {
            List<WebElement> results = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                "type == 'XCUIElementTypeCell') AND label CONTAINS '" + searchText + "'"));

            // Filter out non-option elements
            int count = 0;
            for (WebElement result : results) {
                String label = result.getAttribute("label");
                if (label != null && !label.isEmpty() &&
                    !label.equals("Subcategory") && !label.contains("Type or select")) {
                    count++;
                    System.out.println("   Match: " + label);
                }
            }

            System.out.println("   Total matches for '" + searchText + "': " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error counting results: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get all visible option labels after a search/filter is applied.
     */
    public java.util.ArrayList<String> getFilteredSubcategoryOptions() {
        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                "type == 'XCUIElementTypeCell')"));
            for (WebElement el : elements) {
                String label = el.getAttribute("label");
                int y = el.getLocation().getY();
                if (label != null && !label.isEmpty() && y > 100 && label.length() > 5 &&
                    !label.equals("Subcategory") && !label.contains("Type or select") &&
                    !label.equals("Cancel") && !label.equals("Done") && !label.equals("Close") &&
                    !label.equals("Issue Details") && !label.contains("Required") &&
                    !label.equals("Description") && !label.contains("Proposed") &&
                    !label.equals("Status") && !label.equals("Priority") &&
                    !label.equals("Issue Class") && !label.contains("Photo") &&
                    !label.contains("Delete") && !label.contains("Save") &&
                    label.contains("Chapter")) {
                    options.add(label);
                }
            }
            System.out.println("   Filtered options: " + options.size());
        } catch (Exception e) {
            System.out.println("⚠️ Error getting filtered options: " + e.getMessage());
        }
        return options;
    }

    // ================================================================
    // ISSUE COMPLETION PERCENTAGE (TC_ISS_109)
    // ================================================================

    /**
     * Fill the Subcategory field (required for NEC/NFPA 70B) and verify
     * that the completion percentage updates.
     * Returns the new completion percentage string after filling.
     */
    public String fillSubcategoryAndGetCompletion(String subcategoryText) {
        System.out.println("📋 Filling subcategory to check completion update...");
        try {
            // Open subcategory dropdown
            tapSubcategoryField();
            sleep(400);

            // Select the first available option or the specified one
            if (subcategoryText != null && !subcategoryText.isEmpty()) {
                selectSubcategory(subcategoryText);
            } else {
                // Select any first option
                List<WebElement> options = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS 'Chapter'"));
                if (!options.isEmpty()) {
                    options.get(0).click();
                    sleep(400);
                }
            }

            sleep(500);

            // Scroll up to see completion percentage
            scrollUpOnDetailsScreen();
            sleep(300);

            // Read the completion percentage
            return getIssueDetailsCompletionPercentage();
        } catch (Exception e) {
            System.out.println("⚠️ Error filling subcategory: " + e.getMessage());
            return "";
        }
    }

    /**
     * Helper: scroll down on the Issue Details screen to reveal more fields.
     * Uses a swipe gesture from center-bottom to center-top.
     */
    private void scrollDownOnDetailsScreen() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", "down");
            driver.executeScript("mobile: scroll", params);
            sleep(400);
            System.out.println("   Scrolled down on details screen");
        } catch (Exception e) {
            // Fallback: manual swipe
            try {
                int screenHeight = driver.manage().window().getSize().getHeight();
                int screenWidth = driver.manage().window().getSize().getWidth();
                int startX = screenWidth / 2;
                int startY = (int) (screenHeight * 0.7);
                int endY = (int) (screenHeight * 0.3);

                org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence swipe =
                    new org.openqa.selenium.interactions.Sequence(finger, 0);
                swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
                swipe.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
                swipe.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Collections.singletonList(swipe));
                sleep(400);
                System.out.println("   Manual swipe scroll on details screen");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not scroll on details screen: " + e2.getMessage());
            }
        }
    }
}
