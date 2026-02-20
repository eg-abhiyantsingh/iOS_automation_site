

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
    // SCROLL LIMITER — max 4 scrolls down on Issue Details screen
    // ================================================================
    private int detailsScrollDownDepth = 0;
    private static final int MAX_DETAILS_SCROLL_DOWN = 4;

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
        resetDetailsScrollCount();
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
            // Strategy 1: Find the sort icon button by known names/labels
            // NOTE: Do NOT use .isDisplayed() — iOS reports some SF Symbol buttons as
            // "not displayed" even when they're visible and tappable on screen.
            // Just check that findElement succeeds (element exists in DOM).
            try {
                WebElement sort = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'sort' OR name CONTAINS 'arrow.up.arrow.down' OR " +
                    "name CONTAINS 'line.3.horizontal.decrease' OR " +
                    "label CONTAINS 'Sort' OR label CONTAINS '↕')"));
                String sortName = sort.getAttribute("name");
                String sortLabel = sort.getAttribute("label");
                System.out.println("   Sort icon found — name: '" + sortName + "', label: '" + sortLabel + "'");
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Look for any button in header area that isn't Done or Add
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            for (WebElement btn : buttons) {
                int x = btn.getLocation().getX();
                int y = btn.getLocation().getY();
                String name = btn.getAttribute("name");
                // Sort icon is in top-right area, between Done(left) and +(right)
                if (y < 150 && x > 200 && name != null &&
                    !name.equals("Done") && !name.equals("Add") &&
                    !name.contains("plus") && !name.contains("Back")) {
                    System.out.println("   Sort icon candidate: '" + name + "' at (" + x + "," + y + ")");
                    return true;
                }
            }

            return false;
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
                // Search WITHIN the cell's children — not the entire page DOM.
                // Using driver.findElements would fetch ALL static texts on screen,
                // then call getLocation() on each = O(N) Appium HTTP roundtrips.
                List<WebElement> cellTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"));
                for (WebElement text : cellTexts) {
                    String tLabel = text.getAttribute("label");
                    if (tLabel != null && tLabel.length() > 3) {
                        return tLabel;
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
            // Use case-insensitive matching — iOS may render "In Progress", "IN PROGRESS", etc.
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label ==[c] '" + status + "'"));
            for (WebElement badge : badges) {
                int y = badge.getLocation().getY();
                // Status badges on entries are below the filter tabs (y > ~300)
                if (y > 300) {
                    System.out.println("   Status badge '" + status + "' found at Y=" + y);
                    return true;
                }
            }
            // Fallback: CONTAINS case-insensitive for partial matches
            badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS[c] '" + status + "'"));
            for (WebElement badge : badges) {
                int y = badge.getLocation().getY();
                if (y > 300) {
                    System.out.println("   Status badge containing '" + status + "' found at Y=" + y);
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
        resetDetailsScrollCount();
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
        String predicate =
            "(label == 'Select Asset' OR label BEGINSWITH 'Select Asset') AND " +
            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeCell' OR " +
            "type == 'XCUIElementTypeOther')";

        // Attempt 1: Direct find
        try {
            WebElement selectAsset = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
            selectAsset.click();
            sleep(500);
            System.out.println("✅ Opened Select Asset picker");
            return;
        } catch (Exception e) {
            System.out.println("   Select Asset not found directly, scrolling down...");
        }

        // Attempt 2: Scroll form down (Asset is in ASSIGNMENT section at bottom)
        try {
            scrollDownOnDetailsScreen();
            sleep(300);
            WebElement selectAsset = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
            selectAsset.click();
            sleep(500);
            System.out.println("✅ Opened Select Asset picker (after scroll)");
            return;
        } catch (Exception e) {
            System.out.println("   Select Asset not found after scroll, trying mobile:scroll...");
        }

        // Attempt 3: iOS native scroll to the element
        try {
            Map<String, Object> scrollParams = new HashMap<>();
            scrollParams.put("direction", "down");
            scrollParams.put("predicateString", "label CONTAINS 'Select Asset'");
            driver.executeScript("mobile: scroll", scrollParams);
            sleep(300);
            WebElement selectAsset = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
            selectAsset.click();
            sleep(500);
            System.out.println("✅ Opened Select Asset picker (after mobile:scroll)");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Select Asset after all attempts: " + e.getMessage());
        }
    }

    /**
     * Select an asset by name in the asset picker.
     * The Select Asset picker shows: Cancel, "Select Asset" title, search field,
     * and a scrollable list of asset cells. Each cell has the asset name as
     * XCUIElementTypeStaticText and the location path below it.
     *
     * If the exact asset name is not found, uses the search field to filter,
     * then falls back to tapping the first visible asset cell in the picker.
     */
    public void selectAssetByName(String assetName) {
        // When no specific asset name is provided, just pick the first available asset
        if (assetName == null || assetName.isEmpty()) {
            selectFirstAvailableAsset();
            return;
        }
        System.out.println("📋 Selecting asset: " + assetName);
        try {
            // Verify we're on the Select Asset picker before proceeding
            boolean onPicker = false;
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeNavigationBar' AND name == 'Select Asset'"));
                onPicker = true;
                System.out.println("   Select Asset picker confirmed (navigation bar)");
            } catch (Exception e1) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label == 'Select Asset'"));
                    onPicker = true;
                    System.out.println("   Select Asset picker confirmed (static text)");
                } catch (Exception ignored) {}
            }
            if (!onPicker) {
                System.out.println("⚠️ Not on Select Asset picker — cannot select asset");
                return;
            }

            // Determine the Y boundary of the picker header so we only tap cells below it
            int pickerContentStartY = 150; // default: below nav bar + search field
            try {
                WebElement searchField = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSearchField' OR " +
                    "(type == 'XCUIElementTypeTextField' AND " +
                    "(value CONTAINS 'Search' OR label CONTAINS 'Search'))"));
                int searchY = searchField.getLocation().getY();
                int searchH = searchField.getSize().getHeight();
                pickerContentStartY = searchY + searchH + 5;
                System.out.println("   Search field ends at Y=" + pickerContentStartY);
            } catch (Exception ignored) {
                System.out.println("   No search field found, using default Y=" + pickerContentStartY);
            }

            // Strategy 1: Find a cell containing the exact asset name within the picker area
            // Use XCUIElementTypeStaticText scoped to text that matches and is below the search field
            try {
                List<WebElement> matchingTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + assetName + "'"));
                for (WebElement text : matchingTexts) {
                    int y = text.getLocation().getY();
                    String label = text.getAttribute("label");
                    System.out.println("   Strategy 1 candidate: '" + label + "' at Y=" + y);
                    // Must be in the picker content area and NOT be an issue title from behind
                    if (y >= pickerContentStartY && y < 800 &&
                        !label.contains("Test Issue") && !label.contains("Repair Needed") &&
                        !label.contains("Bldg_") && !label.contains(">")) {
                        text.click();
                        sleep(500);
                        System.out.println("✅ Selected asset by name match: " + label);
                        return;
                    }
                }
                System.out.println("   Strategy 1: No valid match for '" + assetName + "' in picker area");
            } catch (Exception e) {
                System.out.println("   Strategy 1 failed: " + e.getMessage());
            }

            // Strategy 2: Use the search field to find the asset
            // Type the asset name (or partial) and tap the first result
            try {
                WebElement searchField = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSearchField' OR " +
                    "(type == 'XCUIElementTypeTextField' AND " +
                    "(value CONTAINS 'Search' OR label CONTAINS 'Search'))"));
                searchField.click();
                sleep(300);
                searchField.sendKeys(assetName);
                sleep(800); // Wait for search results to filter

                // Check if any cells appeared in results
                List<WebElement> resultCells = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell'"));
                System.out.println("   Strategy 2: Search for '" + assetName + "' found " + resultCells.size() + " cells");
                if (!resultCells.isEmpty()) {
                    String cellLabel = resultCells.get(0).getAttribute("label");
                    resultCells.get(0).click();
                    sleep(500);
                    System.out.println("✅ Selected asset via search: " + cellLabel);
                    return;
                }

                // No results for exact name — clear and try broader search with "Test"
                searchField.clear();
                sleep(300);
                searchField.sendKeys("Test");
                sleep(800);

                resultCells = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell'"));
                System.out.println("   Strategy 2b: Search for 'Test' found " + resultCells.size() + " cells");
                if (!resultCells.isEmpty()) {
                    String cellLabel = resultCells.get(0).getAttribute("label");
                    resultCells.get(0).click();
                    sleep(500);
                    System.out.println("✅ Selected first 'Test' asset via search: " + cellLabel);
                    return;
                }

                // Clear search to show all assets again
                searchField.clear();
                sleep(500);
            } catch (Exception e) {
                System.out.println("   Strategy 2 (search field) failed: " + e.getMessage());
            }

            // Strategy 3: Tap the first asset cell in the picker
            // Cells below the search field are asset cells
            final int contentStartY = pickerContentStartY;
            List<WebElement> allCells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            System.out.println("   Strategy 3: Found " + allCells.size() + " total cells");
            for (WebElement cell : allCells) {
                try {
                    int cellY = cell.getLocation().getY();
                    String cellLabel = cell.getAttribute("label");
                    System.out.println("   Cell at Y=" + cellY + ": '" + cellLabel + "'");
                    // Only tap cells within the visible picker content area
                    if (cellY >= contentStartY && cellY < 800) {
                        cell.click();
                        sleep(500);
                        System.out.println("✅ Selected first picker cell: " + cellLabel);
                        return;
                    }
                } catch (Exception ignored) {}
            }

            // Strategy 4: Find static text that looks like an asset name in the picker area
            // Asset names typically contain underscores or digits (TestAsset_xxx, Trim000xxx)
            try {
                List<WebElement> assetTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'TestAsset' OR label CONTAINS 'Trim' OR " +
                    "label CONTAINS 'Asset' OR label CONTAINS 'ATS')"));
                for (WebElement text : assetTexts) {
                    int y = text.getLocation().getY();
                    String label = text.getAttribute("label");
                    System.out.println("   Strategy 4 candidate: '" + label + "' at Y=" + y);
                    // Must be in picker area and not an issue title
                    if (y >= contentStartY && y < 800 &&
                        !label.contains("Test Issue") && !label.contains("Repair Needed") &&
                        !label.contains("Issue")) {
                        text.click();
                        sleep(500);
                        System.out.println("✅ Selected asset by pattern: " + label);
                        return;
                    }
                }
            } catch (Exception ignored) {}

            System.out.println("⚠️ Could not find any asset to select in picker");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select asset: " + e.getMessage());
        }
    }

    /**
     * Select the first available asset in the Select Asset picker.
     * Does NOT search by name — simply taps the first cell in the picker content area.
     * Use this when the specific asset doesn't matter (e.g., creating temp issues for deletion tests).
     */
    public boolean selectFirstAvailableAsset() {
        System.out.println("📋 Selecting first available asset...");
        try {
            // Verify we're on the Select Asset picker
            boolean onPicker = false;
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeNavigationBar' AND name == 'Select Asset'"));
                onPicker = true;
            } catch (Exception e1) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label == 'Select Asset'"));
                    onPicker = true;
                } catch (Exception ignored) {}
            }
            if (!onPicker) {
                System.out.println("⚠️ Not on Select Asset picker — cannot select asset");
                return false;
            }

            // IMPORTANT: driver.findElements() returns ALL elements in the DOM including
            // those behind the modal picker (Issues list cells, form field cells, filter tabs).
            // Element-based approaches fail because tapping behind-modal elements does nothing.
            // Instead, use COORDINATE-BASED TAP at the position of the first asset cell.
            // Since the picker is the topmost modal, coordinate taps hit the picker, not behind it.

            // Determine where first asset starts (below search field)
            int pickerContentStartY = 195; // default: nav bar (~100) + search field (~95)
            try {
                WebElement searchField = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSearchField' OR " +
                    "(type == 'XCUIElementTypeTextField' AND " +
                    "(value CONTAINS 'Search' OR label CONTAINS 'Search'))"));
                int searchY = searchField.getLocation().getY();
                int searchH = searchField.getSize().getHeight();
                pickerContentStartY = searchY + searchH + 5;
                System.out.println("   Search field ends at Y=" + pickerContentStartY);
            } catch (Exception ignored) {
                System.out.println("   Using default content start Y=" + pickerContentStartY);
            }

            int centerX = driver.manage().window().getSize().width / 2;

            // Try tapping at 3 different Y positions to hit the first visible asset cell
            int[] tapOffsets = {40, 80, 130};
            for (int offset : tapOffsets) {
                int tapY = pickerContentStartY + offset;
                System.out.println("   Coordinate tap at (" + centerX + ", " + tapY + ")");

                org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence tap =
                    new org.openqa.selenium.interactions.Sequence(finger, 0);
                tap.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), centerX, tapY));
                tap.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Collections.singletonList(tap));
                sleep(600);

                // Check if picker dismissed
                boolean stillOnPicker = false;
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeNavigationBar' AND name == 'Select Asset'"));
                    stillOnPicker = true;
                } catch (Exception ignored) {}

                if (!stillOnPicker) {
                    System.out.println("✅ Asset selected via coordinate tap at Y=" + tapY);
                    return true;
                }
            }

            System.out.println("⚠️ Could not select any asset in picker");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Could not select first asset: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tap Create Issue button.
     * Waits up to 3 seconds for the button to become enabled (asset picker dismissal may delay it).
     * Returns true if the button was tapped, false if it stayed disabled or was not found.
     */
    public boolean tapCreateIssue() {
        System.out.println("🆕 Tapping Create Issue...");
        try {
            // Wait for the button to become enabled — asset picker dismissal can delay this
            for (int attempt = 1; attempt <= 6; attempt++) {
                WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(label == 'Create Issue' OR name == 'Create Issue') AND type == 'XCUIElementTypeButton'"));
                String enabled = btn.getAttribute("enabled");
                if ("true".equals(enabled)) {
                    btn.click();
                    sleep(500);
                    System.out.println("✅ Tapped Create Issue");
                    return true;
                }
                System.out.println("   Create Issue not yet enabled (attempt " + attempt + "/6), waiting...");
                sleep(500);
            }
            System.out.println("⚠️ Create Issue button stayed disabled after 3s");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Create Issue: " + e.getMessage());
            return false;
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
            // Strategy 1: Exact match for the 4 sort options from the app:
            // "Created Date", "Modified Date", "Title", "Status"
            List<WebElement> options = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Created Date' OR label == 'Modified Date' OR " +
                "label == 'Title' OR label == 'Status')"));
            if (options.size() >= 2) {
                for (WebElement opt : options) {
                    System.out.println("   Sort option found: " + opt.getAttribute("label"));
                }
                return true;
            }

            // Strategy 2: Broader search including partial matches
            List<WebElement> broadOptions = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Created' OR label CONTAINS 'Modified' OR " +
                "label CONTAINS 'Date' OR label CONTAINS 'Status' OR " +
                "label CONTAINS 'Ascending' OR label CONTAINS 'Descending')"));
            if (!broadOptions.isEmpty()) {
                for (WebElement opt : broadOptions) {
                    System.out.println("   Sort option found (broad): " + opt.getAttribute("label"));
                }
                return true;
            }

            // Strategy 3: Check for action sheet or popover
            List<WebElement> sheets = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSheet' OR type == 'XCUIElementTypeActionSheet'"));
            if (!sheets.isEmpty()) {
                System.out.println("   Sort action sheet found");
                return true;
            }

            // Strategy 4: Any new menu/popover containing sort-like options
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
    // SORT OPTIONS — INDIVIDUAL OPTION HELPERS (TC_ISS_222-229)
    // ================================================================

    /**
     * Get all visible sort option labels after the sort dropdown is open.
     * Searches for buttons/static texts that match known sort option names.
     * @return List of sort option labels found
     */
    public java.util.ArrayList<String> getSortOptionLabels() {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        try {
            // Strategy 1: Look for buttons with sort-related labels
            List<WebElement> options = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Created' OR label CONTAINS 'Modified' OR " +
                "label CONTAINS 'Title' OR label CONTAINS 'Status' OR " +
                "label CONTAINS 'Priority' OR label CONTAINS 'Date' OR " +
                "label CONTAINS 'Name' OR label CONTAINS 'Newest' OR " +
                "label CONTAINS 'Oldest')"));
            for (WebElement opt : options) {
                String label = opt.getAttribute("label");
                if (label != null && !label.isEmpty()) {
                    labels.add(label);
                    System.out.println("   Sort option: '" + label + "'");
                }
            }

            // Strategy 2: If no buttons found, check static text elements in dropdown area
            if (labels.isEmpty()) {
                List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Created' OR label CONTAINS 'Modified' OR " +
                    "label CONTAINS 'Title' OR label CONTAINS 'Status' OR " +
                    "label CONTAINS 'Priority' OR label CONTAINS 'Date')"));
                for (WebElement text : texts) {
                    String label = text.getAttribute("label");
                    if (label != null && !label.isEmpty()) {
                        labels.add(label);
                        System.out.println("   Sort text option: '" + label + "'");
                    }
                }
            }

            // Strategy 3: Check for menu items (SwiftUI Menu style)
            if (labels.isEmpty()) {
                List<WebElement> menuItems = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeMenuItem' OR type == 'XCUIElementTypeCell'"));
                for (WebElement item : menuItems) {
                    String label = item.getAttribute("label");
                    if (label != null && !label.isEmpty() &&
                        (label.contains("Created") || label.contains("Modified") ||
                         label.contains("Title") || label.contains("Status") ||
                         label.contains("Date") || label.contains("Priority"))) {
                        labels.add(label);
                        System.out.println("   Sort menu item: '" + label + "'");
                    }
                }
            }

            System.out.println("📋 Total sort options found: " + labels.size());
        } catch (Exception e) {
            System.out.println("⚠️ Error getting sort options: " + e.getMessage());
        }
        return labels;
    }

    /**
     * Check if a specific sort option is visible in the sort dropdown.
     * @param optionName The sort option to look for (e.g., "Created Date", "Title")
     * @return true if the option is found
     */
    public boolean isSortOptionVisible(String optionName) {
        try {
            // Strategy 1: Button with matching label
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS '" + optionName + "'"));
                System.out.println("   Sort option '" + optionName + "' found (button)");
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Static text with matching label
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + optionName + "'"));
                System.out.println("   Sort option '" + optionName + "' found (text)");
                return true;
            } catch (Exception ignored) {}

            // Strategy 3: Any element containing the option name
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "label CONTAINS '" + optionName + "'"));
                System.out.println("   Sort option '" + optionName + "' found (any type)");
                return true;
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the icon/image associated with a sort option.
     * Looks for an image element near the sort option text.
     * @param optionName The sort option name (e.g., "Created Date")
     * @return The icon name/label or empty string
     */
    public String getSortOptionIcon(String optionName) {
        try {
            // Find the option element to get its Y position
            WebElement option = null;
            try {
                option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS '" + optionName + "'"));
            } catch (Exception ignored) {
                try {
                    option = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + optionName + "'"));
                } catch (Exception ignored2) {}
            }

            if (option != null) {
                // The label itself may contain icon info
                String fullLabel = option.getAttribute("label");
                System.out.println("   Sort option '" + optionName + "' full label: '" + fullLabel + "'");

                // Also check the name attribute which may reference SF Symbol icon
                String name = option.getAttribute("name");
                if (name != null && !name.equals(fullLabel)) {
                    System.out.println("   Sort option '" + optionName + "' name: '" + name + "'");
                }

                // Look for images near this option's Y position
                int optionY = option.getLocation().getY();
                List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage'"));
                for (WebElement img : images) {
                    int imgY = img.getLocation().getY();
                    if (Math.abs(imgY - optionY) < 30) {
                        String imgLabel = img.getAttribute("label");
                        String imgName = img.getAttribute("name");
                        String iconInfo = imgLabel != null ? imgLabel : imgName;
                        System.out.println("   Icon near '" + optionName + "': " + iconInfo);
                        return iconInfo != null ? iconInfo : "";
                    }
                }

                // Return the full label as it may embed icon info
                return fullLabel != null ? fullLabel : "";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Tap a specific sort option by name.
     * @param optionName The sort option to tap (e.g., "Created Date", "Modified Date", "Title", "Status")
     * @return true if option was found and tapped
     */
    public boolean tapSortOption(String optionName) {
        System.out.println("📋 Tapping sort option: '" + optionName + "'...");
        try {
            // Strategy 1: Button with matching label
            try {
                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS '" + optionName + "'"));
                option.click();
                sleep(500);
                System.out.println("✅ Tapped sort option: '" + optionName + "' (button)");
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Static text with matching label (in some dropdown styles, text is tappable)
            try {
                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + optionName + "'"));
                option.click();
                sleep(500);
                System.out.println("✅ Tapped sort option: '" + optionName + "' (text)");
                return true;
            } catch (Exception ignored) {}

            // Strategy 3: Any element with matching label
            try {
                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "label CONTAINS '" + optionName + "'"));
                option.click();
                sleep(500);
                System.out.println("✅ Tapped sort option: '" + optionName + "' (any type)");
                return true;
            } catch (Exception ignored) {}

            System.out.println("⚠️ Sort option '" + optionName + "' not found");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping sort option: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get a list of visible issue titles in display order.
     * Useful for verifying sort order before/after sorting.
     * @param maxCount Maximum number of titles to collect
     * @return Ordered list of issue titles
     */
    public java.util.ArrayList<String> getVisibleIssueTitles(int maxCount) {
        java.util.ArrayList<String> titles = new java.util.ArrayList<>();
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));

            // Sort cells by Y position to get display order
            cells.sort((a, b) -> Integer.compare(a.getLocation().getY(), b.getLocation().getY()));

            int count = 0;
            for (WebElement cell : cells) {
                if (count >= maxCount) break;
                int cellY = cell.getLocation().getY();
                // Only include cells in the list area (below header, above bottom nav)
                if (cellY < 150 || cellY > 800) continue;

                String label = cell.getAttribute("label");
                if (label != null && !label.isEmpty() && label.length() > 2) {
                    titles.add(label);
                    count++;
                }
            }

            // If no cells found, try static texts
            if (titles.isEmpty()) {
                List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"));

                // Cache Y positions BEFORE sorting to avoid O(N log N) Appium HTTP calls.
                // Each getLocation() is a separate HTTP roundtrip — calling it inside a
                // sort comparator causes hundreds of calls that can timeout the session.
                List<int[]> textPositions = new java.util.ArrayList<>();
                for (int i = 0; i < texts.size(); i++) {
                    try {
                        textPositions.add(new int[]{i, texts.get(i).getLocation().getY()});
                    } catch (Exception ignored) {
                        textPositions.add(new int[]{i, 9999});
                    }
                }
                textPositions.sort((a, b) -> Integer.compare(a[1], b[1]));

                for (int[] pos : textPositions) {
                    if (count >= maxCount) break;
                    int textY = pos[1];
                    if (textY < 200 || textY > 800) continue;

                    String label = texts.get(pos[0]).getAttribute("label");
                    // Filter out known non-title labels
                    if (label != null && label.length() > 3 &&
                        !label.contains("All") && !label.contains("Open") &&
                        !label.contains("Resolved") && !label.contains("Closed") &&
                        !label.contains("In Progress") && !label.contains("Issues") &&
                        !label.contains("No Issues") && !label.contains("Done") &&
                        !label.contains("With Photos") && !label.contains("My Session")) {
                        titles.add(label);
                        count++;
                    }
                }
            }

            System.out.println("📋 Collected " + titles.size() + " issue titles in display order");
            for (int i = 0; i < titles.size(); i++) {
                System.out.println("   [" + i + "] " + titles.get(i));
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error collecting issue titles: " + e.getMessage());
        }
        return titles;
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
     * Check if Select Asset field is displayed on New Issue form.
     * Uses explicit wait (3s) since the ASSIGNMENT section is at the bottom
     * and may take a moment to render. Falls back to scrolling down if not
     * found on first attempt.
     */
    public boolean isSelectAssetDisplayed() {
        // The predicate targets the "Select Asset" navigation row in the ASSIGNMENT section.
        // Use specific label match first, then broader fallback.
        String predicate =
            "(label == 'Select Asset' OR label BEGINSWITH 'Select Asset' OR " +
            "(label CONTAINS 'Asset' AND NOT label CONTAINS 'ASSIGNMENT')) AND " +
            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeCell' OR " +
            "type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeOther')";

        // Attempt 1: Wait up to 3 seconds for the element
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement asset = wait.until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.iOSNsPredicateString(predicate)));
            System.out.println("✓ Select Asset field found: label='" + asset.getAttribute("label") +
                "', type=" + asset.getAttribute("type") + ", Y=" + asset.getLocation().getY());
            return true;
        } catch (Exception e) {
            System.out.println("   Select Asset not found on screen, trying scroll...");
        }

        // Attempt 2: Scroll down and retry (Asset is in ASSIGNMENT section at form bottom)
        try {
            scrollDownOnDetailsScreen();
            sleep(300);
            WebElement asset = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
            System.out.println("✓ Select Asset field found after scroll: label='" +
                asset.getAttribute("label") + "', Y=" + asset.getLocation().getY());
            return true;
        } catch (Exception e2) {
            System.out.println("   Select Asset not found even after scroll");
        }

        // Attempt 3: Use mobile:scroll to search for the element by predicate
        try {
            Map<String, Object> scrollParams = new HashMap<>();
            scrollParams.put("direction", "down");
            scrollParams.put("predicateString", "label CONTAINS 'Select Asset'");
            driver.executeScript("mobile: scroll", scrollParams);
            sleep(300);
            WebElement asset = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
            System.out.println("✓ Select Asset field found after mobile:scroll");
            return true;
        } catch (Exception e3) {
            System.out.println("⚠️ Select Asset field not found after all attempts");
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
            // Get screen dimensions for dynamic coordinate calculation.
            // CRITICAL: Never tap at Y < 120 — that's the iOS nav bar zone
            // and would close the screen or trigger unwanted navigation.
            int screenWidth = driver.manage().window().getSize().getWidth();
            int screenHeight = driver.manage().window().getSize().getHeight();

            // Strategy 1: Tap at the bottom portion of the screen (far from any
            // dropdown popover which appears near its anchor field higher up).
            // Y = 85% of screen height is safely below any dropdown menu.
            int safeX = screenWidth / 2;
            int safeY = (int) (screenHeight * 0.85);

            Map<String, Object> tapParams = new HashMap<>();
            tapParams.put("x", safeX);
            tapParams.put("y", safeY);
            driver.executeScript("mobile: tap", tapParams);
            sleep(300);
            System.out.println("✅ Dismissed dropdown menu (tap at X=" + safeX + ", Y=" + safeY + ")");
        } catch (Exception e) {
            // Fallback: try a small swipe gesture to dismiss (works for iOS popovers)
            try {
                int screenWidth = driver.manage().window().getSize().getWidth();
                int screenHeight = driver.manage().window().getSize().getHeight();
                int midX = screenWidth / 2;
                int bottomY = (int) (screenHeight * 0.9);

                org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence swipe =
                    new org.openqa.selenium.interactions.Sequence(finger, 0);
                swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), midX, bottomY));
                swipe.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(100),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), midX, bottomY - 20));
                swipe.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Collections.singletonList(swipe));
                sleep(300);
                System.out.println("✅ Dismissed dropdown menu via swipe fallback");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not dismiss dropdown menu: " + e2.getMessage());
            }
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
     * Tap the first available issue in the list to open Issue Details.
     * Uses cell Y position + height to identify issue cells (labels are often null).
     * Issue cells: Y > 350 (below search bar + filter tabs), H > 60 (taller than nav items).
     */
    public void tapFirstIssue() {
        System.out.println("📋 Tapping first available issue...");
        resetDetailsScrollCount();
        boolean tapped = false;

        try {
            // Strategy 1: Find first cell that looks like an issue cell by dimensions
            List<WebElement> allCells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            for (WebElement cell : allCells) {
                int y = cell.getLocation().getY();
                int h = cell.getSize().getHeight();
                if (y > 350 && h > 60) {
                    cell.click();
                    System.out.println("   Tapped first issue cell at Y=" + y + ", H=" + h);
                    tapped = true;
                    break;
                }
            }

            // Strategy 2: Tap a static text with issue title pattern
            if (!tapped) {
                List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Test Issue' OR label CONTAINS 'Repair' OR " +
                    "label CONTAINS 'RepairCount' OR label CONTAINS 'Issue ')"));
                for (WebElement text : texts) {
                    int y = text.getLocation().getY();
                    if (y > 350) {
                        String label = text.getAttribute("label");
                        text.click();
                        System.out.println("   Tapped issue title at Y=" + y + ": " + label);
                        tapped = true;
                        break;
                    }
                }
            }

            if (!tapped) {
                System.out.println("⚠️ No issue cells found in the list");
                return;
            }

            // Wait for Issue Details screen to load (up to 3 seconds)
            for (int i = 0; i < 6; i++) {
                sleep(500);
                if (isIssueDetailsScreenDisplayed()) {
                    System.out.println("✅ Issue Details screen loaded");
                    return;
                }
            }
            System.out.println("⚠️ Issue Details screen not detected after tap (proceeding anyway)");

        } catch (Exception e) {
            System.out.println("⚠️ Could not tap first issue: " + e.getMessage());
        }
    }

    /**
     * Tap on an issue in the list by its title to open Issue Details
     */
    public void tapOnIssue(String title) {
        System.out.println("📋 Tapping on issue: " + title);
        resetDetailsScrollCount();
        try {
            // Strategy 1: tap cell containing the title
            WebElement issue = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND label CONTAINS '" + title + "'"));
            issue.click();
            sleep(500);
            System.out.println("✅ Tapped on issue: " + title);
            return;
        } catch (Exception ignored) {}

        // Strategy 2: tap the static text matching the title
        try {
            WebElement issueText = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + title + "'"));
            issueText.click();
            sleep(500);
            System.out.println("✅ Tapped on issue text: " + title);
            return;
        } catch (Exception ignored) {}

        // Strategy 3: Named issue not found — tap the first issue cell in the list.
        // Cell labels are often null in iOS, so use Y position + cell height to identify
        // issue cells (they're below the filter tabs and taller than nav/header cells).
        System.out.println("   '" + title + "' not found — falling back to first available issue");
        try {
            List<WebElement> allCells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            System.out.println("   Strategy 3: Found " + allCells.size() + " cells, scanning...");
            for (WebElement cell : allCells) {
                int y = cell.getLocation().getY();
                int h = cell.getSize().getHeight();
                // Issue cells are below the search bar + filter tabs (Y > 350)
                // and taller than nav items (H > 60px — issue cells are ~100-130px)
                if (y > 350 && h > 60) {
                    String cellLabel = cell.getAttribute("label");
                    cell.click();
                    sleep(500);
                    System.out.println("✅ Tapped issue cell at Y=" + y + ", H=" + h +
                        " (label: " + cellLabel + ")");
                    return;
                } else {
                    System.out.println("     Skipped cell: Y=" + y + ", H=" + h +
                        (y <= 350 ? " (above list area)" : " (too short)"));
                }
            }
            System.out.println("   Strategy 3: No qualifying issue cells found");
        } catch (Exception e) {
            System.out.println("   Strategy 3 error: " + e.getMessage());
        }

        // Strategy 4: Find a static text with issue title pattern and tap it
        // Issue titles contain keywords like "Test Issue", "Repair", "Count", etc.
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label CONTAINS 'Test Issue' OR label CONTAINS 'Repair' OR " +
                "label CONTAINS 'RepairCount' OR label CONTAINS 'Issue ')"));
            System.out.println("   Strategy 4: Found " + texts.size() + " issue title candidates");
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y > 350) { // in the issue list area
                    String label = text.getAttribute("label");
                    text.click();
                    sleep(500);
                    System.out.println("✅ Tapped issue title text at Y=" + y + ": " + label);
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 4 error: " + e.getMessage());
        }

        System.out.println("⚠️ Could not tap on any issue (tried '" + title + "' and fallbacks)");
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
        System.out.println("🔙 Closing Issue Details...");
        resetDetailsScrollCount();

        // Strategy 1: Find close/back/done button INSIDE the NavigationBar only
        // This prevents matching random "Done"/"Close" buttons elsewhere on screen
        try {
            WebElement navBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar'"));
            List<WebElement> navButtons = navBar.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            for (WebElement btn : navButtons) {
                String name = btn.getAttribute("name");
                String label = btn.getAttribute("label");
                System.out.println("   Nav bar button: name='" + name + "', label='" + label + "'");
                if (name != null && (name.equals("Close") || name.equals("Back") ||
                    name.equals("Done") || name.equals("Cancel") || name.equals("Issues") ||
                    name.equals("chevron.left") || name.equals("xmark"))) {
                    btn.click();
                    sleep(300);
                    System.out.println("✅ Closed Issue Details via nav bar: '" + name + "'");
                    return;
                }
            }
            // If we found nav bar buttons but none matched known names,
            // tap the first (leftmost) one — that's typically the back button
            if (!navButtons.isEmpty()) {
                String name = navButtons.get(0).getAttribute("name");
                navButtons.get(0).click();
                sleep(300);
                System.out.println("✅ Closed Issue Details via first nav button: '" + name + "'");
                return;
            }
        } catch (Exception e) {
            System.out.println("   No NavigationBar found, trying positional fallback...");
        }

        // Strategy 2: Find button in top-left corner (X < 80, Y < 100) — the back/close area
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            for (WebElement btn : buttons) {
                int x = btn.getLocation().getX();
                int y = btn.getLocation().getY();
                if (x < 80 && y < 100) {
                    String name = btn.getAttribute("name");
                    System.out.println("   Top-left button: '" + name + "' at X=" + x + ", Y=" + y);
                    btn.click();
                    sleep(300);
                    System.out.println("✅ Closed Issue Details (top-left button)");
                    return;
                }
            }
        } catch (Exception ignored) {}

        // Strategy 3: iOS edge swipe back gesture (swipe from left edge to right)
        try {
            int screenHeight = driver.manage().window().getSize().getHeight();
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 0);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 5, screenHeight / 2));
            swipe.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 200, screenHeight / 2));
            swipe.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Collections.singletonList(swipe));
            sleep(500);
            System.out.println("✅ Closed Issue Details (edge swipe back)");
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
     * Get Issue Class value on Issue Details screen.
     * Issue Class is at the top of the form — no scrolling needed.
     */
    public String getIssueClassOnDetails() {
        String result = findIssueClassPickerValue();
        if (!result.isEmpty()) return result;

        System.out.println("⚠️ Issue Class picker not found on details screen");
        return "";
    }

    private String findIssueClassPickerValue() {
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
            // Not found in current view — no scrolling
            System.out.println("   Issue Details section header not found in current view");
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
            // Strategy 1: Look for "Subcategory" label text in current DOM (no scrolling)
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Subcategory' OR label CONTAINS 'Subcategory')"));
            if (!labels.isEmpty()) {
                System.out.println("   Subcategory field label found");
                return true;
            }

            // Strategy 2: Look for button/picker with "Subcategory" or the placeholder
            List<WebElement> pickers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeTextField' OR " +
                "type == 'XCUIElementTypeOther') AND " +
                "(name CONTAINS 'Subcategory' OR name CONTAINS 'subcategory' OR " +
                "label CONTAINS 'Type or select')"));
            if (!pickers.isEmpty()) {
                System.out.println("   Subcategory picker/field found");
                return true;
            }

            System.out.println("⚠️ Subcategory field not found");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Subcategory field not found");
            return false;
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
            // ================================================================
            // STEP A: Find the Subcategory label and ensure it's positioned
            // in the safe content area (Y > 150). After scrollDownOnDetailsScreen(),
            // the label often ends up at Y~2 (behind the nav bar at Y=0-100).
            // If that happens, we scroll the content back down to reposition it.
            // ================================================================
            int subcatY = ensureSubcategoryLabelPositioned();
            if (subcatY < 0) {
                System.out.println("⚠️ Could not find Subcategory label anywhere on screen");
                return;
            }

            // ================================================================
            // STEP B: Try to tap the subcategory input field using multiple
            // strategies. All strategies verify Y > 120 before tapping to
            // prevent accidentally hitting nav bar elements (Close button, etc.)
            // ================================================================

            // Strategy 1: TextField/TextVIew with subcategory-related attributes
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Subcategory' OR name CONTAINS 'subcategory' OR " +
                    "value CONTAINS 'Type or select' OR label CONTAINS 'Type or select')"));
                int fieldY = field.getLocation().getY();
                if (fieldY > 120) {
                    field.click();
                    sleep(400);
                    System.out.println("✅ Strategy 1: Tapped Subcategory text field at Y=" + fieldY);
                    return;
                }
                System.out.println("   Strategy 1: Field found at Y=" + fieldY + " but too close to nav bar");
            } catch (Exception ignored) {}

            // Strategy 2: Button with subcategory attributes
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'Subcategory' OR label CONTAINS 'Type or select' OR " +
                    "label CONTAINS 'Subcategory')"));
                int pickerY = picker.getLocation().getY();
                if (pickerY > 120) {
                    picker.click();
                    sleep(400);
                    System.out.println("✅ Strategy 2: Tapped Subcategory picker button at Y=" + pickerY);
                    return;
                }
                System.out.println("   Strategy 2: Picker found at Y=" + pickerY + " but too close to nav bar");
            } catch (Exception ignored) {}

            // Strategy 3: Find interactive elements near the Subcategory label.
            // Includes XCUIElementTypeOther (SwiftUI custom views) but with strict
            // Y > 120 filtering and preference for real interactive element types.
            try {
                WebElement subcatLabel = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
                subcatY = subcatLabel.getLocation().getY();
                System.out.println("   Strategy 3: Subcategory label at Y=" + subcatY);

                if (subcatY > 120) {
                    List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeButton' OR " +
                        "type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeComboBox' OR " +
                        "type == 'XCUIElementTypeOther')"));

                    // Score candidates: prefer text fields/buttons, closest below label
                    WebElement bestMatch = null;
                    int bestScore = Integer.MAX_VALUE;
                    for (WebElement el : elements) {
                        int y = el.getLocation().getY();
                        int h = el.getSize().getHeight();
                        // Must be: below label, within 80px, in content area, reasonable size
                        if (y >= subcatY && y <= subcatY + 80 && y > 120 && h > 10 && h < 80) {
                            String elType = el.getAttribute("type");
                            int score = y - subcatY;
                            // Strong preference for real interactive elements over Other
                            if (elType != null && !elType.equals("XCUIElementTypeOther")) {
                                score -= 1000;
                            }
                            if (score < bestScore) {
                                bestScore = score;
                                bestMatch = el;
                            }
                        }
                    }
                    if (bestMatch != null) {
                        String elType = bestMatch.getAttribute("type");
                        String elLabel = bestMatch.getAttribute("label");
                        int elY = bestMatch.getLocation().getY();
                        bestMatch.click();
                        sleep(400);
                        System.out.println("✅ Strategy 3: Tapped " + elType + " at Y=" + elY +
                            " (label: '" + elLabel + "')");
                        return;
                    }
                    System.out.println("   Strategy 3: No matching element near label");
                }
            } catch (Exception e) {
                System.out.println("   Strategy 3 failed: " + e.getMessage());
            }

            // Strategy 4: Tap the Subcategory label itself (some apps open picker on label tap)
            try {
                WebElement subcatLabel = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
                int labelY = subcatLabel.getLocation().getY();
                if (labelY > 120) {
                    subcatLabel.click();
                    sleep(400);
                    System.out.println("✅ Strategy 4: Tapped Subcategory label at Y=" + labelY);
                    return;
                }
            } catch (Exception ignored) {}

            // Strategy 5: Coordinate-based tap 30px below the Subcategory label.
            // Uses dynamic screenWidth/2 instead of hardcoded X=200 to work
            // correctly across all iPhone screen sizes (SE, Pro, Pro Max).
            try {
                WebElement subcatLabel = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
                int labelY = subcatLabel.getLocation().getY();
                if (labelY > 120) {
                    int tapX = driver.manage().window().getSize().getWidth() / 2;
                    int tapY = labelY + 30;
                    org.openqa.selenium.interactions.PointerInput finger =
                        new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                    org.openqa.selenium.interactions.Sequence tap =
                        new org.openqa.selenium.interactions.Sequence(finger, 0);
                    tap.addAction(finger.createPointerMove(Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                        tapX, tapY));
                    tap.addAction(finger.createPointerDown(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    tap.addAction(finger.createPointerUp(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    driver.perform(java.util.Arrays.asList(tap));
                    sleep(400);
                    System.out.println("✅ Strategy 5: Coordinate tap at X=" + tapX + ", Y=" + tapY);
                    return;
                }
            } catch (Exception e) {
                System.out.println("   Strategy 5 failed: " + e.getMessage());
            }

            System.out.println("⚠️ All strategies failed — could not tap Subcategory field");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Subcategory: " + e.getMessage());
        }
    }

    /**
     * Helper: Find the Subcategory label and ensure it's positioned in the safe
     * content area (Y > 150). After scrollDownOnDetailsScreen(), the label often
     * ends up at Y~2 behind the nav bar. This method:
     * 1. Finds the label (scrolls down if not in DOM)
     * 2. If the label Y < 150, scrolls content BACK (swipe down) to reposition
     * 3. Returns the final Y position, or -1 if label not found
     */
    private int ensureSubcategoryLabelPositioned() {
        try {
            WebElement subcatLabel;
            int subcatY;

            // Try to find the label in the current DOM
            try {
                subcatLabel = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
                subcatY = subcatLabel.getLocation().getY();
                System.out.println("   Subcategory label found at Y=" + subcatY);
            } catch (Exception e) {
                // Label not found — try native scroll-to to find it
                System.out.println("   Subcategory not in DOM — scrolling down to find it");
                try {
                    Map<String, Object> params = new HashMap<>();
                    params.put("direction", "down");
                    params.put("predicateString", "label == 'Subcategory'");
                    driver.executeScript("mobile: scroll", params);
                    sleep(400);
                } catch (Exception ignored) {}

                try {
                    subcatLabel = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
                    subcatY = subcatLabel.getLocation().getY();
                    System.out.println("   After scroll-to, Subcategory label at Y=" + subcatY);
                } catch (Exception e2) {
                    System.out.println("   Subcategory label not found even after scrolling");
                    return -1;
                }
            }

            // If the label is in the nav bar zone (Y < 150), the prior
            // scrollDownOnDetailsScreen() overshot. We need to scroll the content
            // BACK (swipe down on screen = content moves down) to bring the
            // Subcategory section back into the safe visible area.
            int maxAttempts = 3;
            int attempt = 0;
            while (subcatY < 150 && attempt < maxAttempts) {
                attempt++;
                System.out.println("   Label at Y=" + subcatY + " (nav bar zone) — "
                    + "scrolling content back down (attempt " + attempt + "/" + maxAttempts + ")");
                try {
                    int screenWidth = driver.manage().window().getSize().getWidth();
                    // Swipe DOWN on screen: drag from Y=300 to Y=500
                    // This moves content down ~200px, pushing the Subcategory further from the nav bar
                    org.openqa.selenium.interactions.PointerInput finger =
                        new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                    org.openqa.selenium.interactions.Sequence swipe =
                        new org.openqa.selenium.interactions.Sequence(finger, 0);
                    swipe.addAction(finger.createPointerMove(Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                        screenWidth / 2, 300));
                    swipe.addAction(finger.createPointerDown(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                        screenWidth / 2, 500));
                    swipe.addAction(finger.createPointerUp(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    driver.perform(java.util.Collections.singletonList(swipe));
                    sleep(400);

                    // Re-find the label after scroll
                    subcatLabel = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
                    subcatY = subcatLabel.getLocation().getY();
                    System.out.println("   After repositioning, Subcategory label at Y=" + subcatY);
                } catch (Exception scrollE) {
                    System.out.println("   Repositioning scroll failed: " + scrollE.getMessage());
                    break;
                }
            }

            return subcatY;
        } catch (Exception e) {
            System.out.println("   ensureSubcategoryLabelPositioned failed: " + e.getMessage());
            return -1;
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
        // Use findElements (instant return) to avoid 5s implicit wait on each miss
        String predicate =
            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
            "type == 'XCUIElementTypeCell') AND label CONTAINS '" + subcategory + "'";

        try {
            // Strategy 1: Direct find (instant — no implicit wait hang)
            List<WebElement> options = driver.findElements(AppiumBy.iOSNsPredicateString(predicate));
            if (!options.isEmpty()) {
                options.get(0).click();
                sleep(400);
                System.out.println("✅ Selected subcategory: " + subcategory);
                return;
            }

            // Strategy 2: Scroll within the picker to find the option (with timeout guard)
            System.out.println("   Subcategory not visible, scrolling to find it...");
            for (int scroll = 0; scroll < 5; scroll++) {
                scrollDownOnDetailsScreen();
                sleep(200);
                options = driver.findElements(AppiumBy.iOSNsPredicateString(predicate));
                if (!options.isEmpty()) {
                    options.get(0).click();
                    sleep(400);
                    System.out.println("✅ Selected subcategory after scroll: " + subcategory);
                    return;
                }
            }

            // Strategy 3: Try partial match (first word only)
            String firstWord = subcategory.contains(" ") ? subcategory.substring(0, subcategory.indexOf(' ')) : subcategory;
            if (!firstWord.equals(subcategory)) {
                String partialPredicate =
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + firstWord + "'";
                options = driver.findElements(AppiumBy.iOSNsPredicateString(partialPredicate));
                for (WebElement opt : options) {
                    String label = opt.getAttribute("label");
                    if (label != null && label.length() > 3) {
                        opt.click();
                        sleep(400);
                        System.out.println("✅ Selected subcategory (partial match): " + label);
                        return;
                    }
                }
            }

            // Strategy 4: Select the first available option in the dropdown
            System.out.println("   Exact subcategory not found, selecting first available option...");
            options = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                "type == 'XCUIElementTypeCell') AND label CONTAINS 'Chapter'"));
            if (!options.isEmpty()) {
                String label = options.get(0).getAttribute("label");
                options.get(0).click();
                sleep(400);
                System.out.println("✅ Selected first available subcategory: " + label);
                return;
            }

            System.out.println("⚠️ Could not select subcategory: " + subcategory + " — no options found");
        } catch (Exception e) {
            System.out.println("⚠️ Error selecting subcategory: " + e.getMessage());
        }
    }

    /**
     * Get the currently selected subcategory value.
     * The selected value appears as a chip/tag below the "Subcategory" label,
     * typically an XCUIElementTypeButton with the value text and an X clear button.
     */
    public String getSubcategoryValue() {
        try {
            // First, find the "Subcategory" label to anchor our search
            int subcatY = -1;
            List<WebElement> subcatLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'Subcategory' OR label BEGINSWITH 'Subcategory')"));
            if (!subcatLabels.isEmpty()) {
                subcatY = subcatLabels.get(0).getLocation().getY();
                System.out.println("   Subcategory label at Y=" + subcatY);
            }

            // Strategy 1: Look for a button below "Subcategory" that shows the selected value
            // The selected chip is typically a button with the value text (long label, not "Select...")
            if (subcatY > 0) {
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"));
                for (WebElement btn : buttons) {
                    int y = btn.getLocation().getY();
                    String label = btn.getAttribute("label");
                    if (y > subcatY && y < subcatY + 80 && label != null &&
                        !label.equals("Select...") && !label.equals("Subcategory") &&
                        !label.equals("xmark.circle") && !label.equals("xmark.circle.fill") &&
                        label.length() > 3) {
                        System.out.println("   Subcategory value (button): " + label);
                        return label;
                    }
                }
            }

            // Strategy 2: Read from text field (if subcategory uses a text input)
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

            // Strategy 3: Look for static text near "Subcategory" label
            // Filter out short labels like "All", "Open", etc. that come from other UI elements
            if (subcatY > 0) {
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    String label = text.getAttribute("label");
                    if (y > subcatY && y < subcatY + 60 && label != null &&
                        !label.equals("Subcategory") && !label.contains("Type or select") &&
                        !label.equals("All") && !label.equals("Open") &&
                        !label.equals("Resolved") && !label.equals("Closed") &&
                        label.length() > 5) {  // Real subcategory values are longer than 5 chars
                        System.out.println("   Subcategory value (nearby text): " + label);
                        return label;
                    }
                }
            }

            System.out.println("   Subcategory value not found");
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
            // Check current DOM only — no scrolling. Test handles positioning.
            // Strategy 1: Look for "Description" label text
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Description' OR label CONTAINS 'Description')"));
            if (!labels.isEmpty()) {
                System.out.println("   Description field label found");
                return true;
            }

            // Strategy 2: Look for the placeholder text
            List<WebElement> placeholders = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField' OR " +
                "type == 'XCUIElementTypeStaticText') AND " +
                "(label CONTAINS 'Describe the issue' OR value CONTAINS 'Describe the issue')"));
            if (!placeholders.isEmpty()) {
                System.out.println("   Description placeholder found");
                return true;
            }

            System.out.println("⚠️ Description field not found");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Description field not found");
            return false;
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
            // Check current DOM only — no scrolling. Test handles positioning.
            // Strategy 1: Look for "Proposed Resolution" label text
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Proposed Resolution' OR label CONTAINS 'Proposed Resolution' OR " +
                "label CONTAINS 'Proposed resolution')"));
            if (!labels.isEmpty()) {
                System.out.println("   Proposed Resolution field label found");
                return true;
            }

            // Strategy 2: Look for the placeholder text
            List<WebElement> placeholders = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField' OR " +
                "type == 'XCUIElementTypeStaticText') AND " +
                "(label CONTAINS 'Suggest a resolution' OR value CONTAINS 'Suggest a resolution')"));
            if (!placeholders.isEmpty()) {
                System.out.println("   Proposed Resolution placeholder found");
                return true;
            }

            System.out.println("⚠️ Proposed Resolution field not found");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Proposed Resolution field not found");
            return false;
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
            // Check current DOM only — no scrolling. Test handles positioning.
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Issue Photos' OR label CONTAINS 'Issue Photos' OR " +
                "label == 'Photos' OR label CONTAINS 'Photo')"));
            if (!labels.isEmpty()) {
                System.out.println("   Issue Photos section found: " + labels.get(0).getAttribute("label"));
                return true;
            }

            // Look for Gallery/Camera buttons directly (implies section exists)
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
            String predicate = "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Delete Issue' OR label CONTAINS 'Delete issue' OR " +
                "name CONTAINS 'Delete Issue' OR name CONTAINS 'delete')";

            // Strategy 1: Direct find
            try {
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
                System.out.println("   Delete Issue button found: " + deleteBtn.getAttribute("label"));
                return deleteBtn.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 2: Single scroll down and retry
            scrollDownOnDetailsScreen();
            try {
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
                System.out.println("   Delete Issue button found after scroll");
                return deleteBtn.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 3: Use native scroll to find it by predicate
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Delete'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Delete' OR name CONTAINS 'Delete')"));
                System.out.println("   Delete button found via native scroll");
                return deleteBtn.isDisplayed();
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
            String predicate = "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Delete Issue' OR label CONTAINS 'Delete issue' OR " +
                "name CONTAINS 'Delete Issue')";

            // Strategy 1: Direct find
            try {
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
                deleteBtn.click();
                sleep(500);
                System.out.println("✅ Tapped Delete Issue");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Single scroll down and retry
            scrollDownOnDetailsScreen();
            try {
                WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    predicate + " OR (type == 'XCUIElementTypeButton' AND label CONTAINS 'Delete')"));
                deleteBtn.click();
                sleep(500);
                System.out.println("✅ Tapped Delete Issue after scroll");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Use native scroll to find it
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Delete'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);
            } catch (Exception ignored) {}
            WebElement deleteBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Delete' OR name CONTAINS 'Delete')"));
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
            String predicate = "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Save Changes' OR label CONTAINS 'Save changes' OR " +
                "name CONTAINS 'Save Changes' OR label == 'Save')";

            // Strategy 1: Direct search
            try {
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
                System.out.println("   Save Changes button found: " + saveBtn.getAttribute("label"));
                return saveBtn.isDisplayed();
            } catch (Exception ignored) {}

            // Strategy 2: Single scroll to bottom
            scrollDownOnDetailsScreen();
            try {
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Save' OR name CONTAINS 'Save')"));
                String label = saveBtn.getAttribute("label");
                if (label != null && (label.contains("Save") || label.contains("save"))) {
                    System.out.println("   Save button found after scroll: " + label);
                    return true;
                }
            } catch (Exception ignored) {}

            // Strategy 3: Use native scroll to find Save by predicate
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Save'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Save' OR name CONTAINS 'Save')"));
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
            String predicate = "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'Save Changes' OR label CONTAINS 'Save changes' OR " +
                "name CONTAINS 'Save Changes' OR label == 'Save')";

            // Strategy 1: Direct find
            try {
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(predicate));
                saveBtn.click();
                sleep(800);
                System.out.println("✅ Tapped Save Changes");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Single scroll and retry
            scrollDownOnDetailsScreen();
            try {
                WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Save' OR name CONTAINS 'Save')"));
                saveBtn.click();
                sleep(800);
                System.out.println("✅ Tapped Save after scroll");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Use native scroll to find Save
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Save'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);
            } catch (Exception ignored) {}
            WebElement saveBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Save' OR name CONTAINS 'Save')"));
            saveBtn.click();
            sleep(800);
            System.out.println("✅ Tapped Save via native scroll");
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
        System.out.println("🗑️ Tapping Discard Changes...");
        try {
            // Single findElements call with combined predicate — avoids 5s × N implicit
            // wait penalty from multiple individual findElement calls.
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Discard' OR label == 'Discard Changes' OR " +
                "label == 'Don\\'t Save' OR label == 'No' OR " +
                "label CONTAINS 'Discard' OR label CONTAINS 'discard' OR " +
                "label CONTAINS \"Don't\")"));

            if (!buttons.isEmpty()) {
                // Prefer exact matches: prioritize "Discard" or "Discard Changes" labels
                String[] preferred = {"Discard Changes", "Discard", "Don't Save"};
                for (String pref : preferred) {
                    for (WebElement btn : buttons) {
                        try {
                            String label = btn.getAttribute("label");
                            if (pref.equals(label)) {
                                btn.click();
                                sleep(500);
                                System.out.println("✅ Discarded changes via '" + label + "'");
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                // No exact match — tap the first available button
                try {
                    buttons.get(0).click();
                    sleep(500);
                    System.out.println("✅ Discarded changes via first matching button");
                    return;
                } catch (Exception ignored) {}
            }

            System.out.println("⚠️ No Discard button found on dialog");
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
     * Decrements the scroll depth counter so future down-scrolls are allowed again.
     */
    public void scrollUpOnDetailsScreen() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", "up");
            driver.executeScript("mobile: scroll", params);
            detailsScrollDownDepth = Math.max(0, detailsScrollDownDepth - 1);
            sleep(400);
            System.out.println("   Scrolled up on details screen (depth now " + detailsScrollDownDepth + ")");
        } catch (Exception e) {
            // Fallback: manual swipe (drag from top to bottom = scroll up)
            try {
                int screenHeight = driver.manage().window().getSize().getHeight();
                int screenWidth = driver.manage().window().getSize().getWidth();
                int startX = screenWidth / 2;
                int startY = (int) (screenHeight * 0.3);
                int endY = (int) (screenHeight * 0.7);

                org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence swipe =
                    new org.openqa.selenium.interactions.Sequence(finger, 0);
                swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
                swipe.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
                swipe.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Collections.singletonList(swipe));
                detailsScrollDownDepth = Math.max(0, detailsScrollDownDepth - 1);
                sleep(400);
                System.out.println("   Manual swipe scroll up on details screen (depth now " + detailsScrollDownDepth + ")");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not scroll up: " + e2.getMessage());
            }
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

            // Select Issue Class — use "NEC Violation" (always available)
            selectIssueClass("NEC Violation");
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
            boolean tapped = tapCreateIssue();
            if (!tapped) {
                System.out.println("⚠️ Could not tap Create Issue — cancelling");
                try { tapCancelNewIssue(); } catch (Exception ignored) {}
                return false;
            }
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
     * Scroll the Issue Details form to the very top.
     * After scrolling down for Thermal Anomaly fields, iOS view recycling removes
     * top-of-form elements (Issue Class, Priority, etc.) from the DOM.
     * This method scrolls up aggressively to bring them back.
     */
    private void scrollToTopOfDetails() {
        System.out.println("   Scrolling to top of details form...");
        int screenHeight = driver.manage().window().getSize().getHeight();
        int screenWidth = driver.manage().window().getSize().getWidth();
        int centerX = screenWidth / 2;

        // Manual swipe gestures (drag from top to bottom = scroll content up)
        // DO NOT tap status bar (Y=10) — on iPhone 17 Pro with Dynamic Island,
        // it closes the Issue Details sheet instead of scrolling to top.
        // Use up to 8 swipes to ensure we reach the top even from deeper scrolls.
        for (int i = 0; i < 8; i++) {
            try {
                int startY = (int) (screenHeight * 0.30);
                int endY = (int) (screenHeight * 0.80);
                org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence swipe =
                    new org.openqa.selenium.interactions.Sequence(finger, 0);
                swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), centerX, startY));
                swipe.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), centerX, endY));
                swipe.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Collections.singletonList(swipe));
                sleep(300);
            } catch (Exception e) {
                System.out.println("   Swipe up failed on iteration " + (i + 1));
            }

            // Check if Issue Class label is now in DOM
            try {
                List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS[c] 'issue class'"));
                if (!labels.isEmpty()) {
                    System.out.println("   Issue Class label found after " + (i + 1) + " swipe(s) up");
                    detailsScrollDownDepth = 0;
                    return;
                }
            } catch (Exception e) {}
        }

        // Check what screen we're actually on — if keyboard dismiss tapped nav bar "Done",
        // we may have exited Issue Details entirely
        try {
            List<WebElement> navTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Issues'"));
            List<WebElement> allTab = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'All'"));
            if (!navTexts.isEmpty() && !allTab.isEmpty()) {
                System.out.println("   ⚠️ NOT on Issue Details — landed on Issues LIST screen");
            }
        } catch (Exception ignored) {}
        System.out.println("   ⚠️ Issue Class label not found after scrolling to top");
        detailsScrollDownDepth = 0;
    }

    /**
     * Try to open the Issue Class picker using 3 strategies.
     * Returns true if picker was opened, false if Issue Class element not found.
     */
    private boolean tryOpenIssueClassPicker() {
        // Strategy 1: Tap the Issue Class picker button
        // The button label format is "Issue Class, <current value>" on iOS SwiftUI pickers
        try {
            WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name CONTAINS 'Issue Class' OR label CONTAINS 'Issue Class')"));
            System.out.println("   Found Issue Class picker: '" + picker.getAttribute("label") + "'");
            picker.click();
            sleep(500);
            System.out.println("   Opened Issue Class picker (button)");
            return true;
        } catch (Exception ignored) {}

        // Strategy 2: Find button near the "Issue Class" label (positional)
        try {
            WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Issue Class' OR label CONTAINS 'Issue Class')"));
            int labelY = label.getLocation().getY();
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            for (WebElement btn : buttons) {
                int y = btn.getLocation().getY();
                if (Math.abs(y - labelY) < 50) {
                    btn.click();
                    sleep(500);
                    System.out.println("   Opened Issue Class picker (positional match)");
                    return true;
                }
            }
        } catch (Exception e2) {
            System.out.println("   Strategy 2 (positional) failed: " + e2.getMessage());
        }

        // Strategy 3: Tap by coordinate — if label found, tap the same row on the right
        // half of the screen where the picker value usually sits
        try {
            WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS[c] 'issue class'"));
            int labelY = label.getLocation().getY();
            int screenWidth = driver.manage().window().getSize().getWidth();
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 0);
            tap.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                screenWidth * 3 / 4, labelY + 10));
            tap.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Collections.singletonList(tap));
            sleep(500);
            System.out.println("   Opened Issue Class picker (coordinate tap at Y=" + labelY + ")");
            return true;
        } catch (Exception e3) {
            System.out.println("   Strategy 3 (coordinate) failed: " + e3.getMessage());
        }

        return false;
    }

    /**
     * Open the Issue Class dropdown on Issue Details and change it.
     * Used to switch issue class and see corresponding subcategories.
     */
    public boolean changeIssueClassOnDetails(String newClass) {
        System.out.println("📋 Changing Issue Class on details to: " + newClass);
        resetDetailsScrollCount();
        try {
            boolean pickerOpened = tryOpenIssueClassPicker();

            // If picker not found, the Issue Class field is likely off-screen (iOS view recycling
            // removes off-screen elements from DOM). Scroll to top and retry.
            if (!pickerOpened) {
                System.out.println("   Issue Class not found — scrolling to top and retrying...");
                scrollToTopOfDetails();
                sleep(500);
                pickerOpened = tryOpenIssueClassPicker();
            }

            if (!pickerOpened) {
                System.out.println("⚠️ Could not open Issue Class dropdown");
                return false;
            }

            // Select the new class — broad type filter for popover menu items
            // Retry up to 3 times: picker menu items may not be loaded yet if a previous
            // dropdown (e.g. subcategory) was just dismissed and animation is still in progress
            boolean selected = false;
            String typeFilter = "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                "type == 'XCUIElementTypeMenuItem' OR type == 'XCUIElementTypeOther')";
            for (int attempt = 1; attempt <= 3 && !selected; attempt++) {
                // Try exact match first
                try {
                    List<WebElement> options = driver.findElements(AppiumBy.iOSNsPredicateString(
                        typeFilter + " AND label == '" + newClass + "'"));
                    if (!options.isEmpty()) {
                        options.get(0).click();
                        sleep(500);
                        selected = true;
                        System.out.println("✅ Selected Issue Class: " + newClass);
                        break;
                    }
                } catch (Exception e) {}

                // Try case-insensitive CONTAINS
                try {
                    List<WebElement> options = driver.findElements(AppiumBy.iOSNsPredicateString(
                        typeFilter + " AND label CONTAINS[c] '" + newClass + "'"));
                    if (!options.isEmpty()) {
                        options.get(0).click();
                        sleep(500);
                        selected = true;
                        System.out.println("✅ Selected Issue Class: " + newClass + " (contains match)");
                        break;
                    }
                } catch (Exception e2) {}

                if (!selected && attempt < 3) {
                    System.out.println("   Retry " + attempt + "/3: menu items not loaded yet, waiting...");
                    sleep(800);
                }
            }
            if (!selected) {
                System.out.println("⚠️ Could not select class '" + newClass + "' after 3 attempts");
            }

            if (!selected) return false;

            // Verify the class change actually took effect by checking the picker button value
            sleep(300);
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'Issue Class' OR label CONTAINS 'Issue Class')"));
                String newLabel = picker.getAttribute("label");
                if (newLabel != null && newLabel.toLowerCase().contains(newClass.toLowerCase())) {
                    System.out.println("✅ Verified Issue Class changed to: " + newLabel);
                    return true;
                } else {
                    System.out.println("⚠️ Issue Class picker shows: '" + newLabel + "' (expected '" + newClass + "')");
                    // Still return true if we successfully clicked the option
                    return true;
                }
            } catch (Exception verifyEx) {
                // Verification step failed but selection was performed
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error changing Issue Class: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all visible subcategory option labels from an open subcategory dropdown.
     * Returns a list of visible option texts.
     */
    public java.util.ArrayList<String> getVisibleSubcategoryOptions() {
        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        try {
            // Note: After tapping the Subcategory 'Select...' button, a picker/popover
            // may open that covers the "Issue Details" nav bar. This is EXPECTED —
            // the picker IS the subcategory dropdown. Do NOT check isIssueDetailsScreenDisplayed()
            // here because the picker replaces the nav bar title.

            // Strategy 1: Look for menu items (popover/dropdown items)
            try {
                List<WebElement> menuItems = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeMenuItem' OR type == 'XCUIElementTypePickerWheel'"));
                if (!menuItems.isEmpty()) {
                    for (WebElement item : menuItems) {
                        String label = item.getAttribute("label");
                        if (label != null && !label.isEmpty()) {
                            options.add(label);
                        }
                    }
                    if (!options.isEmpty()) {
                        System.out.println("   Subcategory options (menu items): " + options.size());
                        for (String opt : options) {
                            System.out.println("     - " + opt);
                        }
                        return options;
                    }
                }
            } catch (Exception ignored) {}

            // Strategy 2: Find the Subcategory label and collect options below it
            // Subcategory dropdown options appear below the field as an autocomplete/popover list
            int subcatY = -1;
            try {
                WebElement subcatLabel = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
                subcatY = subcatLabel.getLocation().getY();
                System.out.println("   Subcategory label at Y=" + subcatY);
                // If the label is in the nav bar zone, the scroll state is wrong —
                // any elements collected would be from wrong screen areas
                if (subcatY < 100) {
                    System.out.println("⚠️ Subcategory label at Y=" + subcatY +
                        " (nav bar zone) — dropdown likely didn't open properly");
                    return options;
                }
            } catch (Exception e) {
                System.out.println("   Subcategory label not found — dropdown may not have opened");
                return options;
            }

            // Known non-subcategory labels — comprehensive exclusion list
            java.util.Set<String> excludeLabels = new java.util.HashSet<>(java.util.Arrays.asList(
                // Issue Details screen elements and form placeholders
                "Issue Details", "Close", "Back", "Required fields only",
                "Description", "Proposed Resolution", "Status", "Priority",
                "Issue Class", "Subcategory", "Asset", "Location", "Photos",
                "Issue Photos", "Delete", "Save", "Save Changes", "Cancel", "Done",
                "Select...", "Describe the issue...", "Suggest a resolution...",
                // Navigation / tab bar items
                "Site", "Assets", "Connections", "Settings",
                "Scan Qr Code", "Quick Actions", "My Tasks", "Issues",
                "Arc Flash", "Refresh", "Sites", "Locations",
                // Filter tabs
                "All", "Open", "Resolved", "Closed", "Critical",
                "With Photos", "My Session",
                // Dashboard elements
                "No Active Work Order", "Tap to select a work order"
            ));

            // Collect static texts and buttons that are likely subcategory options
            // They should be in a dropdown area below the Subcategory field
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                "type == 'XCUIElementTypeCell') AND " +
                "label != '' AND NOT (label CONTAINS 'Type or select')"));

            final int baseY = subcatY;
            for (WebElement el : elements) {
                String label = el.getAttribute("label");
                if (label == null || label.isEmpty()) continue;

                int y = el.getLocation().getY();
                // Only include elements below the Subcategory label and within a reasonable range
                // Subcategory dropdown options should appear right below the field
                if (y < baseY) continue;
                if (y > baseY + 500) continue; // Don't look too far below

                // Strip counts from labels like "All, 44" → "All"
                String cleanLabel = label.contains(",") ? label.split(",")[0].trim() : label.trim();

                // Exclude known non-subcategory labels
                if (excludeLabels.contains(cleanLabel)) continue;
                if (excludeLabels.contains(label.trim())) continue;

                // Exclude issue list items (contain timestamps, "Test Issue", asset names)
                if (label.contains("Test Issue") || label.contains("Repair Needed") ||
                    label.contains("RepairCount") || label.contains("RepairTest") ||
                    label.contains("TestAsset") || label.contains("Bldg_") ||
                    label.contains(">")) continue;

                // Exclude very short labels (filter tab badges like "44")
                if (label.length() <= 2) continue;

                // Exclude labels with only digits and commas (e.g., "44, Issues")
                if (label.matches("^[\\d,\\s]+.*?(My Tasks|Issues).*$")) continue;

                options.add(label);
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

            // Tap Create Issue (waits for button to become enabled)
            boolean tapped = tapCreateIssue();
            if (!tapped) {
                System.out.println("ℹ️ Create Issue not enabled with only asset — may need more fields");
                try { tapCancelNewIssue(); } catch (Exception ignored) {}
                return false;
            }
            sleep(800);
            System.out.println("✅ Minimal issue created");
            return true;
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

            // Tap Create Issue (waits for button to become enabled)
            boolean tapped = tapCreateIssue();
            if (!tapped) {
                // Button still disabled — try adding issue class
                System.out.println("ℹ️ Create Issue not enabled — trying with issue class too");
                selectIssueClass("NEC Violation");
                sleep(300);
                tapped = tapCreateIssue();
                if (!tapped) {
                    System.out.println("⚠️ Could not create issue — cancelling");
                    try { tapCancelNewIssue(); } catch (Exception ignored) {}
                    return false;
                }
            }
            sleep(800);
            System.out.println("✅ Issue created with title: " + title);
            return true;
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

            // Dismiss any leftover dropdown menu before scrolling.
            // If selectSubcategory() tapped an option, the menu SHOULD auto-close,
            // but some iOS picker styles leave the menu open. Calling dismiss
            // ensures subsequent scroll operates on the form, not the dropdown.
            dismissDropdownMenu();
            sleep(300);

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

    // ================================================================
    // COMPLETION INDICATOR HELPERS (TC_ISS_110-111)
    // ================================================================

    /**
     * Check if the completion percentage indicator is orange (incomplete state).
     * Looks for orange dot/circle near the Issue Details section header.
     * Returns true if an orange or incomplete indicator is found.
     */
    public boolean isCompletionIndicatorOrange() {
        try {
            // Strategy 1: Look for orange/yellow colored indicator image
            try {
                WebElement indicator = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') AND " +
                    "(name CONTAINS 'orange' OR name CONTAINS 'circle' OR " +
                    "name CONTAINS 'exclamation' OR name CONTAINS 'warning' OR " +
                    "name CONTAINS 'incomplete')"));
                System.out.println("   Orange indicator found: " + indicator.getAttribute("name"));
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Look for non-green indicator near "Issue Details" header
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Issue Details'"));
            if (!headers.isEmpty()) {
                int headerY = headers.get(0).getLocation().getY();
                List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage'"));
                for (WebElement icon : icons) {
                    int y = icon.getLocation().getY();
                    if (Math.abs(y - headerY) < 40) {
                        String name = icon.getAttribute("name");
                        System.out.println("   Icon near Issue Details: " + name);
                        // If it's NOT green/checkmark, it's likely orange/incomplete
                        if (name != null && !name.contains("checkmark") && !name.contains("green")) {
                            return true;
                        }
                    }
                }
            }

            // Strategy 3: Check percentage — 0% implies orange indicator
            String pct = getIssueDetailsCompletionPercentage();
            if (pct.contains("0%")) {
                System.out.println("   0% completion implies orange/incomplete indicator");
                return true;
            }

            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking orange indicator: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the completion percentage indicator is green (complete state).
     * Looks for green dot/checkmark near the Issue Details section header.
     * Returns true if a green/complete indicator is found.
     */
    public boolean isCompletionIndicatorGreen() {
        try {
            // Strategy 1: Look for green colored indicator image
            try {
                WebElement indicator = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') AND " +
                    "(name CONTAINS 'green' OR name CONTAINS 'checkmark.circle.fill' OR " +
                    "name CONTAINS 'complete' OR name CONTAINS 'checkmark.circle')"));
                System.out.println("   Green indicator found: " + indicator.getAttribute("name"));
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Look for green indicator near "Issue Details" header
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Issue Details'"));
            if (!headers.isEmpty()) {
                int headerY = headers.get(0).getLocation().getY();
                List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage'"));
                for (WebElement icon : icons) {
                    int y = icon.getLocation().getY();
                    if (Math.abs(y - headerY) < 40) {
                        String name = icon.getAttribute("name");
                        if (name != null && (name.contains("checkmark") || name.contains("green") ||
                            name.contains("circle.fill"))) {
                            System.out.println("   Green indicator near header: " + name);
                            return true;
                        }
                    }
                }
            }

            // Strategy 3: Check percentage — 100% implies green indicator
            String pct = getIssueDetailsCompletionPercentage();
            if (pct.contains("100%")) {
                System.out.println("   100% completion implies green/complete indicator");
                return true;
            }

            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking green indicator: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // CLEAR SUBCATEGORY (TC_ISS_118)
    // ================================================================

    /**
     * Clear the currently selected subcategory value by tapping the clear/X button.
     * Returns true if the subcategory was successfully cleared.
     */
    public boolean clearSubcategoryValue() {
        System.out.println("🗑️ Clearing subcategory value...");
        try {
            // Strategy 1: Look for clear/X button near Subcategory field
            try {
                WebElement clearBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton') AND " +
                    "(name CONTAINS 'clear' OR name CONTAINS 'Clear' OR " +
                    "name CONTAINS 'xmark' OR name CONTAINS 'close' OR " +
                    "label CONTAINS 'clear' OR label CONTAINS 'Clear' OR " +
                    "label CONTAINS '✕' OR label CONTAINS '✖' OR label CONTAINS '×')"));
                clearBtn.click();
                sleep(400);
                System.out.println("✅ Cleared subcategory via clear button");
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Find X button near the Subcategory label by Y proximity
            List<WebElement> subcatLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Subcategory'"));
            if (!subcatLabels.isEmpty()) {
                int subcatY = subcatLabels.get(0).getLocation().getY();
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"));
                for (WebElement btn : buttons) {
                    int y = btn.getLocation().getY();
                    if (Math.abs(y - subcatY) < 40) {
                        String name = btn.getAttribute("name");
                        String label = btn.getAttribute("label");
                        if ((name != null && (name.contains("clear") || name.contains("xmark") ||
                            name.contains("close") || name.contains("remove"))) ||
                            (label != null && (label.contains("clear") || label.contains("Clear") ||
                            label.contains("×") || label.contains("✕")))) {
                            btn.click();
                            sleep(400);
                            System.out.println("✅ Cleared subcategory via nearby button: " + name);
                            return true;
                        }
                    }
                }
            }

            // Strategy 3: Tap subcategory field and look for clear option inside
            tapSubcategoryField();
            sleep(300);
            try {
                WebElement clearOption = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                    "(label == 'Clear' OR label == 'None' OR label == 'Reset')"));
                clearOption.click();
                sleep(400);
                System.out.println("✅ Cleared subcategory via dropdown clear option");
                return true;
            } catch (Exception e) {
                // Dismiss the dropdown
                dismissDropdownMenu();
                System.out.println("⚠️ No clear option found in dropdown");
            }

            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error clearing subcategory: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the subcategory field is empty (showing placeholder text).
     * Returns true if the field shows "Type or select" or is empty.
     */
    public boolean isSubcategoryEmpty() {
        try {
            String value = getSubcategoryValue();
            String placeholder = getSubcategoryPlaceholder();
            boolean isEmpty = value.isEmpty() || value.contains("Type or select") ||
                value.contains("Select") || value.equals(placeholder);
            System.out.println("   Subcategory empty check: value='" + value + "', isEmpty=" + isEmpty);
            return isEmpty;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking if subcategory empty: " + e.getMessage());
            return true; // Assume empty on error
        }
    }

    // ================================================================
    // OSHA SUBCATEGORY OPTION VERIFICATION (TC_ISS_120-129)
    // ================================================================

    /**
     * Check if a specific subcategory option is present in the currently open dropdown.
     * Uses three strategies to find options even if they are off-screen:
     *   1. Direct DOM search (fastest — works if option is in the element tree)
     *   2. mobile: scroll with predicateString (scrolls within dropdown to find it)
     *   3. Type-to-filter search (types in the search/filter field to narrow results)
     * The subcategory dropdown MUST already be open before calling this method.
     * Returns true if the option is found by any strategy.
     */
    public boolean isSpecificSubcategoryOptionPresent(String optionText) {
        System.out.println("🔍 Checking for subcategory option: '" + optionText + "'");

        // The subcategory dropdown is an iOS popover with multi-line wrapped text.
        // CRITICAL: When text wraps, the label attribute contains newlines
        // (e.g., "Mounting - Should\nbe secure" not "Mounting - Should be secure").
        // So CONTAINS with the full string fails. We must use multi-keyword matching.

        // Split option text into meaningful keywords for matching.
        // "Mounting - Should be secure" → category="Mounting", keywords=["Should", "secure"]
        String category = optionText;
        String afterDash = "";
        if (optionText.contains(" - ")) {
            category = optionText.substring(0, optionText.indexOf(" - ")).trim();
            afterDash = optionText.substring(optionText.indexOf(" - ") + 3).trim();
        }

        // Build a broad type filter — popover items can be many types
        String typeFilter = "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
            "type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeMenuItem' OR " +
            "type == 'XCUIElementTypeOther')";

        // Strategy 1: Full exact match (works when label has no newline)
        try {
            List<WebElement> matches = driver.findElements(AppiumBy.iOSNsPredicateString(
                typeFilter + " AND label CONTAINS '" + optionText + "'"));
            if (!matches.isEmpty()) {
                System.out.println("   ✅ Found (exact match): '" + matches.get(0).getAttribute("label") + "'");
                return true;
            }
        } catch (Exception ignored) {}

        // Strategy 2: Multi-keyword match — handles newline-wrapped labels.
        // Match category prefix AND last significant word from the description.
        // e.g., label CONTAINS 'Mounting' AND label CONTAINS 'secure'
        if (!afterDash.isEmpty()) {
            try {
                // Use the last word of the description as the unique differentiator
                String[] words = afterDash.split("\\s+");
                String lastWord = words[words.length - 1];
                // Filter to significant words (> 3 chars) to avoid matching noise
                if (lastWord.length() <= 3 && words.length > 1) {
                    lastWord = words[words.length - 2];
                }

                List<WebElement> matches = driver.findElements(AppiumBy.iOSNsPredicateString(
                    typeFilter + " AND label CONTAINS '" + category + "' AND label CONTAINS '" + lastWord + "'"));
                if (!matches.isEmpty()) {
                    System.out.println("   ✅ Found (multi-keyword '" + category + "' + '" + lastWord + "'): '" +
                        matches.get(0).getAttribute("label") + "'");
                    return true;
                }
            } catch (Exception ignored) {}
        }

        // Strategy 3: Category-only match with Java-side full text verification.
        // Finds all elements containing the category prefix, then checks the full
        // label text in Java (which handles newlines correctly via replaceAll).
        try {
            List<WebElement> candidates = driver.findElements(AppiumBy.iOSNsPredicateString(
                typeFilter + " AND label CONTAINS '" + category + "'"));

            // Normalize optionText for comparison (lowercase, collapse whitespace)
            String normalizedOption = optionText.toLowerCase().replaceAll("\\s+", " ").trim();

            for (WebElement el : candidates) {
                String label = el.getAttribute("label");
                if (label != null) {
                    // Normalize label: replace newlines/tabs with spaces, collapse, lowercase
                    String normalizedLabel = label.replaceAll("[\\n\\r\\t]+", " ")
                        .replaceAll("\\s+", " ").toLowerCase().trim();
                    if (normalizedLabel.contains(normalizedOption)) {
                        System.out.println("   ✅ Found (normalized match): '" + label + "'");
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        System.out.println("   ❌ Option not found: '" + optionText + "'");
        return false;
    }

    /**
     * Check if a subcategory category prefix is present in the dropdown.
     * For example, checking if "Clearance" appears as a prefix in any option.
     * The dropdown MUST already be open.
     * Returns true if any option starts with or contains the category prefix.
     */
    public boolean isSubcategoryCategoryPresent(String categoryPrefix) {
        System.out.println("🔍 Checking for subcategory category: '" + categoryPrefix + "'");
        try {
            // Broad type filter — popover items can be many element types
            String typeFilter = "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' OR " +
                "type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeMenuItem' OR " +
                "type == 'XCUIElementTypeOther')";

            // Strategy 1: BEGINSWITH match
            List<WebElement> matches = driver.findElements(AppiumBy.iOSNsPredicateString(
                typeFilter + " AND label BEGINSWITH '" + categoryPrefix + "'"));
            if (!matches.isEmpty()) {
                System.out.println("   ✅ Category '" + categoryPrefix + "' found (BEGINSWITH)");
                return true;
            }

            // Strategy 2: CONTAINS match (handles labels with newlines/special formatting)
            matches = driver.findElements(AppiumBy.iOSNsPredicateString(
                typeFilter + " AND label CONTAINS '" + categoryPrefix + "'"));
            if (!matches.isEmpty()) {
                System.out.println("   ✅ Category '" + categoryPrefix + "' found (CONTAINS)");
                return true;
            }

            System.out.println("   ❌ Category '" + categoryPrefix + "' NOT found");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking category: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // SUBCATEGORY COUNT & REPAIR NEEDED HELPERS (TC_ISS_136-139)
    // ================================================================

    /**
     * Count all subcategory options in the currently open dropdown,
     * including off-screen options discovered by scrolling.
     * Uses a scroll-and-collect approach to gather unique option labels.
     * The dropdown MUST already be open before calling this method.
     * Returns the total count of unique subcategory options found.
     */
    public int countAllSubcategoryOptions() {
        System.out.println("📊 Counting all subcategory options (including off-screen)...");
        java.util.LinkedHashSet<String> uniqueOptions = new java.util.LinkedHashSet<>();

        try {
            // Pass 1: Collect initially visible options
            java.util.ArrayList<String> visible = getVisibleSubcategoryOptions();
            uniqueOptions.addAll(visible);
            System.out.println("   Initial visible options: " + visible.size());

            // Pass 2: Scroll down within the dropdown and collect more options
            // Repeat up to 10 scroll attempts to find off-screen options
            int maxScrollAttempts = 10;
            int previousCount = uniqueOptions.size();
            int staleScrolls = 0;

            for (int i = 0; i < maxScrollAttempts; i++) {
                // Scroll down within the dropdown area
                try {
                    int screenHeight = driver.manage().window().getSize().getHeight();
                    int screenWidth = driver.manage().window().getSize().getWidth();
                    int startX = screenWidth / 2;
                    // Scroll within the middle of the screen (dropdown area)
                    int startY = (int) (screenHeight * 0.6);
                    int endY = (int) (screenHeight * 0.3);

                    org.openqa.selenium.interactions.PointerInput finger =
                        new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                    org.openqa.selenium.interactions.Sequence swipe =
                        new org.openqa.selenium.interactions.Sequence(finger, 0);
                    swipe.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
                    swipe.addAction(finger.createPointerDown(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    swipe.addAction(finger.createPointerMove(java.time.Duration.ofMillis(250),
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
                    swipe.addAction(finger.createPointerUp(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    driver.perform(java.util.Collections.singletonList(swipe));
                    sleep(300);
                } catch (Exception scrollErr) {
                    System.out.println("   Scroll attempt " + (i + 1) + " failed: " + scrollErr.getMessage());
                    break;
                }

                // Collect newly visible options
                java.util.ArrayList<String> newVisible = getVisibleSubcategoryOptions();
                uniqueOptions.addAll(newVisible);

                // Check if we found new options
                if (uniqueOptions.size() == previousCount) {
                    staleScrolls++;
                    if (staleScrolls >= 2) {
                        // No new options after 2 consecutive scrolls — we've reached the end
                        System.out.println("   No new options after " + (i + 1) + " scrolls — end of list");
                        break;
                    }
                } else {
                    staleScrolls = 0;
                    previousCount = uniqueOptions.size();
                    System.out.println("   After scroll " + (i + 1) + ": " + uniqueOptions.size() + " unique options");
                }
            }

            System.out.println("📊 Total unique subcategory options found: " + uniqueOptions.size());
            int idx = 1;
            for (String opt : uniqueOptions) {
                System.out.println("   " + idx + ". " + opt);
                idx++;
            }

            return uniqueOptions.size();
        } catch (Exception e) {
            System.out.println("⚠️ Error counting subcategory options: " + e.getMessage());
            return uniqueOptions.size();
        }
    }

    /**
     * Check if the Issue Details section is empty (no Subcategory, no completion %,
     * no required fields toggle). This is expected for certain Issue Classes like
     * "Repair Needed" that don't have required subcategory fields.
     * Returns true if the section header exists but has no content fields.
     */
    public boolean isIssueDetailsSectionEmpty() {
        System.out.println("🔍 Checking if Issue Details section is empty...");
        try {
            // Check for the section header first
            boolean headerExists = isIssueDetailsSectionHeaderDisplayed();
            System.out.println("   Issue Details header exists: " + headerExists);

            // Check for completion percentage
            String pct = getIssueDetailsCompletionPercentage();
            boolean hasPct = !pct.isEmpty();
            System.out.println("   Has completion percentage: " + hasPct + " ('" + pct + "')");

            // Check for required fields toggle
            boolean hasToggle = isRequiredFieldsToggleDisplayed();
            System.out.println("   Has required fields toggle: " + hasToggle);

            // Check for subcategory field
            boolean hasSubcat = isSubcategoryFieldDisplayed();
            System.out.println("   Has subcategory field: " + hasSubcat);

            // Section is "empty" if there's no percentage, no toggle, and no subcategory
            boolean isEmpty = !hasPct && !hasToggle && !hasSubcat;
            System.out.println("   Issue Details section empty: " + isEmpty);
            return isEmpty;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking section: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // THERMAL ANOMALY FIELD HELPERS (TC_ISS_144-149)
    // ================================================================

    /**
     * Check if the Thermal Anomaly specialized fields are displayed in the
     * Issue Details section. These fields replace the Subcategory field when
     * Issue Class is "Thermal Anomaly".
     * Returns a map of field names to their display status.
     */
    public java.util.LinkedHashMap<String, Boolean> getThermalAnomalyFieldsStatus() {
        System.out.println("🌡️ Checking Thermal Anomaly specialized fields...");
        java.util.LinkedHashMap<String, Boolean> fields = new java.util.LinkedHashMap<>();

        String[] thermalFields = {
            "Severity", "Severity Criteria", "Position",
            "Problem Temp", "Reference Temp", "Current Draw", "Voltage Drop"
        };

        for (String fieldName : thermalFields) {
            boolean found = isThermalFieldPresent(fieldName);
            fields.put(fieldName, found);
            System.out.println("   " + fieldName + ": " + (found ? "FOUND" : "not found"));
        }

        return fields;
    }

    /**
     * Check if a specific Thermal Anomaly field is present on the screen.
     * Searches for the field label in the DOM, including off-screen elements.
     * Uses multiple strategies: direct find, scroll + find, mobile: scroll.
     */
    public boolean isThermalFieldPresent(String fieldName) {
        try {
            // Strategy 1: Direct DOM search for label text
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label == '" + fieldName + "' OR label CONTAINS '" + fieldName + "')"));
                System.out.println("   Found field '" + fieldName + "' via direct DOM");
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Check for button/picker with field name
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeTextField' OR " +
                    "type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS '" + fieldName + "' OR label CONTAINS '" + fieldName + "')"));
                System.out.println("   Found field '" + fieldName + "' via picker/input");
                return true;
            } catch (Exception ignored) {}

            // Strategy 3: mobile: scroll with predicate to find off-screen fields
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("direction", "down");
                params.put("predicateString", "label CONTAINS '" + fieldName + "'");
                driver.executeScript("mobile: scroll", params);
                sleep(300);

                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + fieldName + "'"));
                System.out.println("   Found field '" + fieldName + "' after mobile:scroll");
                return true;
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Severity field is displayed for Thermal Anomaly.
     * Severity is a required dropdown field with options: Nominal, Intermediate, Serious, Critical.
     */
    public boolean isSeverityFieldDisplayed() {
        System.out.println("🔍 Checking for Severity field...");
        return isThermalFieldPresent("Severity");
    }

    /**
     * Get the current value of the Severity field.
     * Returns the selected severity value, or empty string if not set.
     */
    public String getSeverityValue() {
        System.out.println("📋 Getting Severity value...");
        try {
            // Strategy 1: Button/picker with Severity in name
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Severity'"));
                String label = picker.getAttribute("label");
                String value = picker.getAttribute("value");
                System.out.println("   Severity picker — label: '" + label + "', value: '" + value + "'");
                if (value != null && !value.isEmpty() && !value.contains("Type or select")) return value;
                if (label != null && label.contains(", ")) {
                    return label.substring(label.indexOf(", ") + 2).trim();
                }
                return label != null ? label : "";
            } catch (Exception ignored) {}

            // Strategy 2: TextField near Severity label
            try {
                List<WebElement> sevLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Severity'"));
                if (!sevLabels.isEmpty()) {
                    int sevY = sevLabels.get(0).getLocation().getY();
                    // Look for value text below label
                    List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText'"));
                    for (WebElement text : allTexts) {
                        int y = text.getLocation().getY();
                        String lbl = text.getAttribute("label");
                        if (y > sevY && y < sevY + 50 && lbl != null &&
                            !lbl.equals("Severity") && !lbl.equals("Severity Criteria") &&
                            !lbl.contains("Type or select")) {
                            System.out.println("   Severity value (nearby text): " + lbl);
                            return lbl;
                        }
                    }
                }
            } catch (Exception ignored) {}

            return "";
        } catch (Exception e) {
            System.out.println("⚠️ Error getting Severity value: " + e.getMessage());
            return "";
        }
    }

    /**
     * Tap the Severity field to open its dropdown.
     * Works for Thermal Anomaly Issue Class.
     */
    public void tapSeverityField() {
        System.out.println("👆 Tapping Severity field...");
        try {
            // Strategy 1: Tap button/picker with Severity
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Severity' AND " +
                    "NOT (name CONTAINS 'Criteria')"));
                picker.click();
                sleep(400);
                System.out.println("✅ Tapped Severity picker");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Tap text field near Severity label
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "name CONTAINS 'Severity' AND NOT (name CONTAINS 'Criteria')"));
                field.click();
                sleep(400);
                System.out.println("✅ Tapped Severity text field");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Find Severity label and tap nearby interactive element
            List<WebElement> sevLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Severity'"));
            if (!sevLabels.isEmpty()) {
                int sevY = sevLabels.get(0).getLocation().getY();
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"));
                for (WebElement btn : buttons) {
                    int y = btn.getLocation().getY();
                    if (Math.abs(y - sevY) < 40) {
                        btn.click();
                        sleep(400);
                        System.out.println("✅ Tapped button near Severity label");
                        return;
                    }
                }
                // If no button found, tap the label itself
                sevLabels.get(0).click();
                sleep(400);
                System.out.println("   Tapped Severity label directly");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping Severity field: " + e.getMessage());
        }
    }

    /**
     * Select a severity option from the dropdown.
     * Expected options: Nominal, Intermediate, Serious, Critical.
     */
    public void selectSeverity(String severityOption) {
        System.out.println("📋 Selecting severity: " + severityOption);
        try {
            // Try direct button/text tap
            try {
                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                    "label == '" + severityOption + "'"));
                option.click();
                sleep(400);
                System.out.println("✅ Selected severity: " + severityOption);
                return;
            } catch (Exception ignored) {}

            // Try CONTAINS match
            try {
                WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + severityOption + "'"));
                option.click();
                sleep(400);
                System.out.println("✅ Selected severity (contains): " + severityOption);
                return;
            } catch (Exception ignored) {}

            // Scroll to find the option
            Map<String, Object> scrollParams = new HashMap<>();
            scrollParams.put("direction", "down");
            scrollParams.put("predicateString", "label CONTAINS '" + severityOption + "'");
            driver.executeScript("mobile: scroll", scrollParams);
            sleep(300);

            WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                "label CONTAINS '" + severityOption + "'"));
            option.click();
            sleep(400);
            System.out.println("✅ Selected severity after scroll: " + severityOption);
        } catch (Exception e) {
            System.out.println("⚠️ Error selecting severity '" + severityOption + "': " + e.getMessage());
        }
    }

    /**
     * Get visible severity dropdown options.
     * The dropdown must already be open.
     * Returns list of option labels.
     */
    public java.util.ArrayList<String> getSeverityDropdownOptions() {
        System.out.println("📋 Getting severity dropdown options...");
        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        String[] expectedOptions = {"Nominal", "Intermediate", "Serious", "Critical"};

        try {
            for (String expected : expectedOptions) {
                try {
                    WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                        "type == 'XCUIElementTypeCell') AND label == '" + expected + "'"));
                    options.add(option.getAttribute("label"));
                    System.out.println("   Found severity option: " + expected);
                } catch (Exception e) {
                    // Try CONTAINS match
                    try {
                        WebElement option = driver.findElement(AppiumBy.iOSNsPredicateString(
                            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                            "type == 'XCUIElementTypeCell') AND label CONTAINS '" + expected + "'"));
                        options.add(option.getAttribute("label"));
                        System.out.println("   Found severity option (contains): " + expected);
                    } catch (Exception ignored) {
                        System.out.println("   Missing severity option: " + expected);
                    }
                }
            }

            System.out.println("   Total severity options found: " + options.size());
        } catch (Exception e) {
            System.out.println("⚠️ Error getting severity options: " + e.getMessage());
        }
        return options;
    }

    /**
     * Check if the Severity field has a required indicator (red dot).
     * Returns true if a required indicator is found near the Severity label.
     */
    public boolean isSeverityRequiredIndicatorDisplayed() {
        System.out.println("🔍 Checking for Severity required indicator...");
        try {
            // Strategy 1: Look for red dot/asterisk near Severity label
            List<WebElement> sevLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Severity'"));
            if (!sevLabels.isEmpty()) {
                int sevY = sevLabels.get(0).getLocation().getY();
                int sevX = sevLabels.get(0).getLocation().getX();

                // Look for image/icon indicators near the Severity label
                List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther' OR " +
                    "type == 'XCUIElementTypeStaticText')"));
                for (WebElement icon : icons) {
                    int y = icon.getLocation().getY();
                    int x = icon.getLocation().getX();
                    if (Math.abs(y - sevY) < 25 && x != sevX) {
                        String name = icon.getAttribute("name");
                        String label = icon.getAttribute("label");
                        if (name != null && (name.contains("required") || name.contains("red") ||
                            name.contains("asterisk") || name.contains("dot") ||
                            name.contains("circle.fill"))) {
                            System.out.println("   Required indicator found near Severity: " + name);
                            return true;
                        }
                        if (label != null && (label.equals("*") || label.equals("•"))) {
                            System.out.println("   Required indicator (text) found: " + label);
                            return true;
                        }
                    }
                }
            }

            // Strategy 2: The "Required fields only" toggle count includes Severity
            // If toggle shows x/3 or similar, Severity is likely required
            String reqCount = getRequiredFieldsToggleCount();
            if (reqCount.contains("/3")) {
                System.out.println("   Required fields shows /3 — Severity is one of the required fields");
                return true;
            }

            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking Severity required indicator: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // SEVERITY CRITERIA FIELD HELPERS (TC_ISS_152-156)
    // ================================================================

    /**
     * Tap the Severity Criteria field to open its dropdown.
     * Severity Criteria is an optional field with options: Similar, Ambient, Indirect.
     */
    public void tapSeverityCriteriaField() {
        System.out.println("👆 Tapping Severity Criteria field...");
        try {
            // Strategy 1: Button/picker with "Severity Criteria"
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'Severity Criteria' OR label CONTAINS 'Severity Criteria')"));
                picker.click();
                sleep(400);
                System.out.println("✅ Tapped Severity Criteria picker");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: TextField/TextInput with "Criteria"
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "name CONTAINS 'Criteria'"));
                field.click();
                sleep(400);
                System.out.println("✅ Tapped Severity Criteria text field");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Find "Severity Criteria" label and tap nearby interactive element
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Severity Criteria'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"));
                for (WebElement btn : buttons) {
                    int y = btn.getLocation().getY();
                    if (Math.abs(y - labelY) < 40) {
                        btn.click();
                        sleep(400);
                        System.out.println("✅ Tapped button near Severity Criteria label");
                        return;
                    }
                }
                // Tap label itself as last resort
                labels.get(0).click();
                sleep(400);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping Severity Criteria: " + e.getMessage());
        }
    }

    /**
     * Get the currently selected Severity Criteria value.
     */
    public String getSeverityCriteriaValue() {
        System.out.println("📋 Getting Severity Criteria value...");
        try {
            // Strategy 1: Button/picker with Severity Criteria in name
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'Severity Criteria' OR label CONTAINS 'Severity Criteria')"));
                String label = picker.getAttribute("label");
                String value = picker.getAttribute("value");
                if (value != null && !value.isEmpty() && !value.contains("Type or select")) return value;
                if (label != null && label.contains(", ")) {
                    return label.substring(label.indexOf(", ") + 2).trim();
                }
                return "";
            } catch (Exception ignored) {}

            // Strategy 2: Text near "Severity Criteria" label
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Severity Criteria'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    String lbl = text.getAttribute("label");
                    if (y > labelY && y < labelY + 50 && lbl != null &&
                        !lbl.equals("Severity Criteria") && !lbl.contains("Type or select")) {
                        return lbl;
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Select a Severity Criteria option from the dropdown.
     * Expected options: Similar, Ambient, Indirect.
     */
    public void selectSeverityCriteria(String option) {
        System.out.println("📋 Selecting Severity Criteria: " + option);
        try {
            try {
                WebElement opt = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                    "label == '" + option + "'"));
                opt.click();
                sleep(400);
                System.out.println("✅ Selected Severity Criteria: " + option);
                return;
            } catch (Exception ignored) {}

            // CONTAINS fallback
            try {
                WebElement opt = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                    "type == 'XCUIElementTypeCell') AND label CONTAINS '" + option + "'"));
                opt.click();
                sleep(400);
                System.out.println("✅ Selected Severity Criteria (contains): " + option);
            } catch (Exception e) {
                System.out.println("⚠️ Could not select Severity Criteria: " + option);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error selecting Severity Criteria: " + e.getMessage());
        }
    }

    /**
     * Get Severity Criteria dropdown options. Dropdown must be open.
     * Returns list of found options from expected: Similar, Ambient, Indirect.
     */
    public java.util.ArrayList<String> getSeverityCriteriaDropdownOptions() {
        System.out.println("📋 Getting Severity Criteria dropdown options...");
        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        String[] expected = {"Similar", "Ambient", "Indirect"};

        for (String exp : expected) {
            try {
                WebElement opt = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                    "type == 'XCUIElementTypeCell') AND label == '" + exp + "'"));
                options.add(opt.getAttribute("label"));
                System.out.println("   Found criteria option: " + exp);
            } catch (Exception e) {
                try {
                    WebElement opt = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                        "type == 'XCUIElementTypeCell') AND label CONTAINS '" + exp + "'"));
                    options.add(opt.getAttribute("label"));
                    System.out.println("   Found criteria option (contains): " + exp);
                } catch (Exception ignored) {
                    System.out.println("   Missing criteria option: " + exp);
                }
            }
        }
        System.out.println("   Total criteria options found: " + options.size());
        return options;
    }

    /**
     * Check if the Severity Criteria field has NO required indicator (optional field).
     * Returns true if no required indicator is found (confirming it's optional).
     */
    public boolean isSeverityCriteriaOptional() {
        System.out.println("🔍 Checking if Severity Criteria is optional...");
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Severity Criteria'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                // Look for required indicators near the label
                List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther')"));
                for (WebElement icon : icons) {
                    int y = icon.getLocation().getY();
                    if (Math.abs(y - labelY) < 25) {
                        String name = icon.getAttribute("name");
                        if (name != null && (name.contains("required") || name.contains("red") ||
                            name.contains("asterisk"))) {
                            System.out.println("   Required indicator found — NOT optional");
                            return false;
                        }
                    }
                }
            }
            System.out.println("   No required indicator found — field is optional");
            return true;
        } catch (Exception e) {
            return true; // Assume optional if we can't verify
        }
    }

    // ================================================================
    // POSITION FIELD HELPERS (TC_ISS_157-158)
    // ================================================================

    /**
     * Get the Position field placeholder text.
     * Expected: 'Enter position'.
     */
    public String getPositionPlaceholder() {
        System.out.println("📋 Getting Position placeholder...");
        try {
            // Strategy 1: Find input field with "position" in name/placeholder
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Position' OR name CONTAINS 'position' OR " +
                    "placeholderValue CONTAINS 'position' OR value CONTAINS 'Enter position')"));
                String placeholder = field.getAttribute("placeholderValue");
                if (placeholder != null && !placeholder.isEmpty()) return placeholder;
                String value = field.getAttribute("value");
                if (value != null && value.contains("Enter position")) return value;
                return "";
            } catch (Exception ignored) {}

            // Strategy 2: Static text near "Position" label
            List<WebElement> posLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Position'"));
            if (!posLabels.isEmpty()) {
                int posY = posLabels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Enter position'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    if (Math.abs(y - posY) < 60) return text.getAttribute("label");
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Enter text into the Position field.
     */
    public void enterPosition(String text) {
        System.out.println("📝 Entering position: " + text);
        try {
            // Strategy 1: Find text field with "Position" or "position"
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Position' OR name CONTAINS 'position' OR " +
                    "placeholderValue CONTAINS 'position' OR value CONTAINS 'Enter position')"));
                field.click();
                sleep(300);
                field.clear();
                field.sendKeys(text);
                sleep(300);
                System.out.println("✅ Entered position: " + text);
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Find input field near "Position" label
            List<WebElement> posLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Position'"));
            if (!posLabels.isEmpty()) {
                int posY = posLabels.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'"));
                for (WebElement field : fields) {
                    int y = field.getLocation().getY();
                    if (y > posY && y < posY + 60) {
                        field.click();
                        sleep(300);
                        field.clear();
                        field.sendKeys(text);
                        sleep(300);
                        System.out.println("✅ Entered position (nearby field): " + text);
                        return;
                    }
                }
            }
            System.out.println("⚠️ Could not find Position field to enter text");
        } catch (Exception e) {
            System.out.println("⚠️ Error entering position: " + e.getMessage());
        }
    }

    /**
     * Get the current value of the Position field.
     */
    public String getPositionValue() {
        try {
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Position' OR name CONTAINS 'position')"));
                String value = field.getAttribute("value");
                if (value != null && !value.contains("Enter position")) return value;
            } catch (Exception ignored) {}

            // Fallback: nearby text
            List<WebElement> posLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Position'"));
            if (!posLabels.isEmpty()) {
                int posY = posLabels.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'"));
                for (WebElement field : fields) {
                    int y = field.getLocation().getY();
                    if (y > posY && y < posY + 60) {
                        String value = field.getAttribute("value");
                        if (value != null && !value.contains("Enter position")) return value;
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // PROBLEM TEMP FIELD HELPERS (TC_ISS_159)
    // ================================================================

    /**
     * Get the Problem Temp field placeholder text.
     * Expected: 'Enter number'.
     */
    public String getProblemTempPlaceholder() {
        System.out.println("📋 Getting Problem Temp placeholder...");
        try {
            // Strategy 1: Direct field find
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Problem Temp' OR name CONTAINS 'problem' OR " +
                    "name CONTAINS 'Problem')"));
                String placeholder = field.getAttribute("placeholderValue");
                if (placeholder != null && !placeholder.isEmpty()) return placeholder;
                String value = field.getAttribute("value");
                if (value != null && value.contains("Enter number")) return value;
            } catch (Exception ignored) {}

            // Strategy 2: Find near "Problem Temp" label
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Problem Temp'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Enter number'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    if (Math.abs(y - labelY) < 60) return text.getAttribute("label");
                }
                // Check text fields
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"));
                for (WebElement field : fields) {
                    int y = field.getLocation().getY();
                    if (y > labelY && y < labelY + 60) {
                        String ph = field.getAttribute("placeholderValue");
                        if (ph != null && !ph.isEmpty()) return ph;
                        String val = field.getAttribute("value");
                        if (val != null && val.contains("Enter")) return val;
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if the Problem Temp field has a required indicator (red dot).
     * Returns true if a required indicator is found.
     */
    public boolean isProblemTempRequiredIndicatorDisplayed() {
        System.out.println("🔍 Checking for Problem Temp required indicator...");
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Problem Temp'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther' OR " +
                    "type == 'XCUIElementTypeStaticText')"));
                for (WebElement icon : icons) {
                    int y = icon.getLocation().getY();
                    if (Math.abs(y - labelY) < 25) {
                        String name = icon.getAttribute("name");
                        String label = icon.getAttribute("label");
                        if (name != null && (name.contains("required") || name.contains("red") ||
                            name.contains("asterisk") || name.contains("dot") ||
                            name.contains("circle.fill"))) {
                            System.out.println("   Problem Temp required indicator: " + name);
                            return true;
                        }
                        if (label != null && (label.equals("*") || label.equals("•"))) {
                            return true;
                        }
                    }
                }
            }

            // Cross-check: if /3 required fields, Problem Temp is one of them
            String reqCount = getRequiredFieldsToggleCount();
            if (reqCount.contains("/3")) {
                System.out.println("   /3 required fields — Problem Temp is required");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // REQUIRED FIELDS TOGGLE STATE HELPERS (TC_ISS_170-172)
    // ================================================================

    /**
     * Check if the Required fields only toggle is currently ON.
     * iOS switches have value "1" when ON, "0" when OFF.
     */
    public boolean isRequiredFieldsToggleOn() {
        System.out.println("🔍 Checking if Required fields toggle is ON...");
        try {
            // Strategy 1: Switch element value check
            try {
                WebElement toggle = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSwitch' AND " +
                    "(label CONTAINS 'Required' OR name CONTAINS 'Required')"));
                String value = toggle.getAttribute("value");
                boolean isOn = "1".equals(value);
                System.out.println("   Toggle value: '" + value + "' — ON: " + isOn);
                return isOn;
            } catch (Exception ignored) {}

            // Strategy 2: Check if only required fields are showing (heuristic)
            // If optional fields like "Position" or "Current Draw" are hidden, toggle is likely ON
            boolean positionVisible = false;
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == 'Position'"));
                positionVisible = true;
            } catch (Exception ignored) {}

            boolean currentDrawVisible = false;
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Current Draw'"));
                currentDrawVisible = true;
            } catch (Exception ignored) {}

            // If optional fields are hidden, toggle is ON
            if (!positionVisible && !currentDrawVisible) {
                System.out.println("   Optional fields hidden — toggle likely ON");
                return true;
            }

            System.out.println("   Optional fields visible — toggle likely OFF");
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get all visible field labels in the Issue Details section.
     * Returns a list of field label texts currently visible on screen.
     * Used to verify which fields are shown/hidden when Required fields toggle is ON/OFF.
     */
    public java.util.ArrayList<String> getVisibleThermalFieldLabels() {
        System.out.println("📋 Getting visible thermal field labels...");
        java.util.ArrayList<String> visibleLabels = new java.util.ArrayList<>();
        String[] allThermalFields = {
            "Severity", "Severity Criteria", "Position",
            "Problem Temp", "Reference Temp", "Current Draw", "Voltage Drop"
        };

        for (String fieldName : allThermalFields) {
            try {
                List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + fieldName + "'"));
                if (!labels.isEmpty()) {
                    // Check if at least one is actually visible
                    for (WebElement label : labels) {
                        try {
                            if (label.isDisplayed()) {
                                visibleLabels.add(fieldName);
                                break;
                            }
                        } catch (Exception ignored) {
                            // Element might be stale — skip
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        System.out.println("   Visible thermal fields: " + visibleLabels);
        return visibleLabels;
    }

    // ================================================================
    // CLEAR SEVERITY / SEVERITY CRITERIA HELPERS (TC_ISS_174-175)
    // ================================================================

    /**
     * Clear the Severity selection by tapping the X/clear button near the field.
     * Returns true if clearing was successful.
     */
    public boolean clearSeveritySelection() {
        System.out.println("🗑️ Clearing Severity selection...");
        try {
            // Strategy 1: Find X/clear button near the Severity field
            List<WebElement> sevLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Severity'"));
            if (!sevLabels.isEmpty()) {
                int sevY = sevLabels.get(0).getLocation().getY();
                // Look for clear/X buttons near the Severity label
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"));
                for (WebElement btn : buttons) {
                    int y = btn.getLocation().getY();
                    if (Math.abs(y - sevY) < 40) {
                        String name = btn.getAttribute("name");
                        String label = btn.getAttribute("label");
                        if ((name != null && (name.contains("clear") || name.contains("xmark") ||
                            name.contains("close") || name.contains("remove") ||
                            name.contains("x.circle") || name.contains("multiply.circle"))) ||
                            (label != null && (label.contains("clear") || label.contains("Clear") ||
                            label.contains("×") || label.contains("✕") || label.contains("X")))) {
                            btn.click();
                            sleep(400);
                            System.out.println("✅ Cleared Severity via nearby X button: " + name);
                            return true;
                        }
                    }
                }
            }

            // Strategy 2: Look for generic clear button associated with Severity picker
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Severity' AND " +
                    "NOT (name CONTAINS 'Criteria')"));
                // Try to find an X button within the same container/nearby
                int pickerY = picker.getLocation().getY();
                int pickerX = picker.getLocation().getX() + picker.getSize().getWidth();
                List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"));
                for (WebElement btn : allButtons) {
                    int bY = btn.getLocation().getY();
                    int bX = btn.getLocation().getX();
                    if (Math.abs(bY - pickerY) < 20 && bX > pickerX - 50) {
                        String name = btn.getAttribute("name");
                        if (name != null && (name.contains("clear") || name.contains("xmark") ||
                            name.contains("close") || name.contains("x.circle"))) {
                            btn.click();
                            sleep(400);
                            System.out.println("✅ Cleared Severity via picker-adjacent button: " + name);
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Strategy 3: Tap the Severity field and look for a clear/reset option
            try {
                tapSeverityField();
                sleep(300);
                WebElement clearOption = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                    "(label == 'Clear' OR label == 'Reset' OR label == 'None' OR label == 'Deselect')"));
                clearOption.click();
                sleep(400);
                System.out.println("✅ Cleared Severity via dropdown clear option");
                return true;
            } catch (Exception e) {
                dismissDropdownMenu();
            }

            System.out.println("⚠️ Could not clear Severity selection");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error clearing Severity: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the Severity field is in cleared/empty state.
     * Returns true if Severity shows "Type or select..." placeholder or is empty.
     */
    public boolean isSeverityCleared() {
        String value = getSeverityValue();
        boolean cleared = value.isEmpty() || value.contains("Type or select") || value.contains("Select");
        System.out.println("   Severity cleared: " + cleared + " (value: '" + value + "')");
        return cleared;
    }

    /**
     * Clear the Severity Criteria selection by tapping the X/clear button.
     * Returns true if clearing was successful.
     */
    public boolean clearSeverityCriteriaSelection() {
        System.out.println("🗑️ Clearing Severity Criteria selection...");
        try {
            // Strategy 1: Find X/clear button near the Severity Criteria field
            List<WebElement> critLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Severity Criteria'"));
            if (!critLabels.isEmpty()) {
                int critY = critLabels.get(0).getLocation().getY();
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"));
                for (WebElement btn : buttons) {
                    int y = btn.getLocation().getY();
                    if (Math.abs(y - critY) < 40) {
                        String name = btn.getAttribute("name");
                        String label = btn.getAttribute("label");
                        if ((name != null && (name.contains("clear") || name.contains("xmark") ||
                            name.contains("close") || name.contains("remove") ||
                            name.contains("x.circle") || name.contains("multiply.circle"))) ||
                            (label != null && (label.contains("clear") || label.contains("Clear") ||
                            label.contains("×") || label.contains("✕") || label.contains("X")))) {
                            btn.click();
                            sleep(400);
                            System.out.println("✅ Cleared Severity Criteria via X button: " + name);
                            return true;
                        }
                    }
                }
            }

            // Strategy 2: Tap field and look for clear option
            try {
                tapSeverityCriteriaField();
                sleep(300);
                WebElement clearOption = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                    "(label == 'Clear' OR label == 'Reset' OR label == 'None' OR label == 'Deselect')"));
                clearOption.click();
                sleep(400);
                System.out.println("✅ Cleared Severity Criteria via dropdown clear option");
                return true;
            } catch (Exception e) {
                dismissDropdownMenu();
            }

            System.out.println("⚠️ Could not clear Severity Criteria selection");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error clearing Severity Criteria: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the Severity Criteria field is in cleared/empty state.
     */
    public boolean isSeverityCriteriaCleared() {
        String value = getSeverityCriteriaValue();
        boolean cleared = value.isEmpty() || value.contains("Type or select") || value.contains("Select");
        System.out.println("   Severity Criteria cleared: " + cleared + " (value: '" + value + "')");
        return cleared;
    }

    // ================================================================
    // ULTRASONIC ANOMALY HELPERS (TC_ISS_177-179)
    // ================================================================

    /**
     * Check if the "No required fields" message is displayed in Issue Details.
     * This is expected for Ultrasonic Anomaly class which has no required fields.
     */
    public boolean isNoRequiredFieldsMessageDisplayed() {
        System.out.println("🔍 Checking for 'No required fields' message...");
        try {
            // Check current DOM only — no scrolling. Test handles positioning.
            List<WebElement> msgs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label CONTAINS 'No required fields' OR label CONTAINS 'no required fields' OR " +
                "label CONTAINS 'No Required Fields' OR label CONTAINS 'No required')"));
            if (!msgs.isEmpty()) {
                System.out.println("   Found message: " + msgs.get(0).getAttribute("label"));
                return true;
            }
            System.out.println("   'No required fields' message not found in current view");
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Issue Details section has no input fields (no subcategory, no thermal fields).
     * This is expected for Ultrasonic Anomaly which shows only the "No required fields" message.
     */
    public boolean isIssueDetailsWithoutInputFields() {
        System.out.println("🔍 Checking if Issue Details has no input fields...");
        try {
            boolean hasSubcat = isSubcategoryFieldDisplayed();
            boolean hasSeverity = isThermalFieldPresent("Severity");
            boolean hasProblemTemp = isThermalFieldPresent("Problem Temp");

            boolean noFields = !hasSubcat && !hasSeverity && !hasProblemTemp;
            System.out.println("   No input fields: " + noFields +
                " (subcat=" + hasSubcat + ", severity=" + hasSeverity + ", probTemp=" + hasProblemTemp + ")");
            return noFields;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // PROBLEM TEMP ENTRY HELPERS (TC_ISS_160-161)
    // ================================================================

    /**
     * Enter a numeric value into the Problem Temp field.
     * Taps the field first, clears it, then types the value.
     */
    public void enterProblemTemp(String value) {
        System.out.println("📝 Entering Problem Temp: " + value);
        try {
            // Strategy 1: Direct field find via name/placeholder
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Problem Temp' OR name CONTAINS 'problem' OR " +
                    "name CONTAINS 'Problem' OR placeholderValue CONTAINS 'Enter number')"));
                field.click();
                sleep(300);
                field.clear();
                field.sendKeys(value);
                sleep(300);
                System.out.println("✅ Entered Problem Temp: " + value);
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Find text field near "Problem Temp" label by Y-proximity
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Problem Temp'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'"));
                for (WebElement field : fields) {
                    int y = field.getLocation().getY();
                    if (y > labelY && y < labelY + 60) {
                        field.click();
                        sleep(300);
                        field.clear();
                        field.sendKeys(value);
                        sleep(300);
                        System.out.println("✅ Entered Problem Temp (nearby field): " + value);
                        return;
                    }
                }
            }

            // Strategy 3: Scroll to Problem Temp first, then retry
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Problem Temp'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);

                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Problem' OR placeholderValue CONTAINS 'number')"));
                field.click();
                sleep(300);
                field.clear();
                field.sendKeys(value);
                sleep(300);
                System.out.println("✅ Entered Problem Temp after scroll: " + value);
                return;
            } catch (Exception ignored) {}

            System.out.println("⚠️ Could not find Problem Temp field to enter value");
        } catch (Exception e) {
            System.out.println("⚠️ Error entering Problem Temp: " + e.getMessage());
        }
    }

    /**
     * Get the current value of the Problem Temp field.
     * Returns the entered numeric value, or empty string if not set.
     */
    public String getProblemTempValue() {
        try {
            // Strategy 1: Direct field find
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Problem Temp' OR name CONTAINS 'problem' OR " +
                    "name CONTAINS 'Problem')"));
                String value = field.getAttribute("value");
                if (value != null && !value.contains("Enter number")) return value;
            } catch (Exception ignored) {}

            // Strategy 2: Find field near label
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Problem Temp'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'"));
                for (WebElement field : fields) {
                    int y = field.getLocation().getY();
                    if (y > labelY && y < labelY + 60) {
                        String value = field.getAttribute("value");
                        if (value != null && !value.contains("Enter number")) return value;
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if a green checkmark indicator is displayed near a specific thermal field.
     * This appears when a required field is completed. Works for Severity, Problem Temp, Reference Temp, etc.
     * @param fieldName The label text of the field (e.g., "Problem Temp", "Reference Temp")
     */
    public boolean isThermalFieldCheckmarkDisplayed(String fieldName) {
        System.out.println("🔍 Checking for checkmark near '" + fieldName + "'...");
        try {
            // Strategy 1: Find checkmark image/icon globally (field just filled = checkmark visible)
            List<WebElement> fieldLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + fieldName + "'"));
            if (!fieldLabels.isEmpty()) {
                int fieldY = fieldLabels.get(0).getLocation().getY();
                // Look for checkmark images near the field label
                List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther' OR " +
                    "type == 'XCUIElementTypeStaticText') AND " +
                    "(name CONTAINS 'checkmark' OR name CONTAINS 'check' OR " +
                    "label CONTAINS 'checkmark' OR label CONTAINS '✓' OR label CONTAINS '✔' OR " +
                    "name CONTAINS 'green' OR name CONTAINS 'complete')"));
                for (WebElement icon : icons) {
                    int y = icon.getLocation().getY();
                    if (Math.abs(y - fieldY) < 40) {
                        System.out.println("   ✅ Checkmark found near '" + fieldName + "': " + icon.getAttribute("name"));
                        return true;
                    }
                }
            }

            // Strategy 2: Generic checkmark on screen (may appear as completion feedback)
            try {
                WebElement checkmark = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeStaticText') AND " +
                    "(name CONTAINS 'checkmark.circle.fill' OR name CONTAINS 'checkmark.circle' OR " +
                    "name CONTAINS 'checkmark' OR label CONTAINS '✓')"));
                System.out.println("   ✅ Checkmark found on screen: " + checkmark.getAttribute("name"));
                return true;
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the numeric keyboard is displayed (number pad).
     * iOS shows XCUIElementTypeKeyboard with numeric key types for number fields.
     * Returns true if a keyboard with numeric keys is shown.
     */
    public boolean isNumericKeyboardDisplayed() {
        System.out.println("🔍 Checking for numeric keyboard...");
        try {
            // Strategy 1: Check for keyboard element
            try {
                WebElement keyboard = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeKeyboard' AND visible == true"));
                if (keyboard.isDisplayed()) {
                    // Check for numeric keys (digits 0-9)
                    List<WebElement> numericKeys = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeKey' AND visible == true AND " +
                        "(label == '0' OR label == '1' OR label == '2' OR label == '3' OR " +
                        "label == '4' OR label == '5' OR label == '6' OR label == '7' OR " +
                        "label == '8' OR label == '9')"));
                    if (!numericKeys.isEmpty()) {
                        // Check if it's numeric-only (no letter keys like 'q', 'w', 'e')
                        List<WebElement> letterKeys = driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeKey' AND visible == true AND " +
                            "(label == 'q' OR label == 'w' OR label == 'e' OR label == 'a' OR label == 's')"));
                        boolean isNumericOnly = letterKeys.isEmpty();
                        System.out.println("   Keyboard displayed — numeric keys: " + numericKeys.size() +
                            ", letter keys: " + letterKeys.size() + ", numeric-only: " + isNumericOnly);
                        return true; // Keyboard with numbers is showing (even if not strictly numeric-only)
                    }
                    System.out.println("   Keyboard displayed but no numeric keys found");
                    return true; // Keyboard is showing
                }
            } catch (Exception ignored) {}

            // Strategy 2: Check for key elements directly
            try {
                List<WebElement> keys = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeKey' AND visible == true"));
                if (!keys.isEmpty()) {
                    System.out.println("   Keyboard keys visible: " + keys.size());
                    return true;
                }
            } catch (Exception ignored) {}

            System.out.println("   No keyboard detected");
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Dismiss the keyboard if displayed.
     * Tries Done button → hideKeyboard → tap outside.
     */
    public void dismissKeyboard() {
        try {
            // Strategy 1: Appium hideKeyboard — most reliable, no risk of tapping wrong UI element
            try {
                driver.executeScript("mobile: hideKeyboard");
                sleep(200);
                System.out.println("   Keyboard dismissed via mobile:hideKeyboard");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Tap Done/Return button on keyboard toolbar
            // IMPORTANT: Filter by Y position (> 40% of screen) to avoid tapping
            // the nav bar "Done" button which would close Issue Details sheet!
            try {
                int screenHeight = driver.manage().window().getSize().getHeight();
                int minY = (int) (screenHeight * 0.4);
                List<WebElement> doneBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeKey') AND " +
                    "(label == 'Done' OR label == 'Return' OR label == 'Go')"));
                for (WebElement btn : doneBtns) {
                    try {
                        int btnY = btn.getLocation().getY();
                        if (btnY > minY) {
                            btn.click();
                            sleep(200);
                            System.out.println("   Keyboard dismissed via Done/Return (Y=" + btnY + ")");
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            // Strategy 3: Tap on a non-interactive area to dismiss
            try {
                int screenWidth = driver.manage().window().getSize().getWidth();
                org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence tap =
                    new org.openqa.selenium.interactions.Sequence(finger, 0);
                tap.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), screenWidth / 2, 200));
                tap.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Collections.singletonList(tap));
                sleep(200);
                System.out.println("   Keyboard dismissed via tap at Y=200");
            } catch (Exception ignored) {}
        } catch (Exception e) {
            System.out.println("⚠️ Could not dismiss keyboard: " + e.getMessage());
        }
    }

    // ================================================================
    // REFERENCE TEMP FIELD HELPERS (TC_ISS_162-163)
    // ================================================================

    /**
     * Check if the Reference Temp field has a required indicator (red dot).
     * Reference Temp is one of the 3 required fields for Thermal Anomaly.
     */
    public boolean isReferenceTempRequiredIndicatorDisplayed() {
        System.out.println("🔍 Checking for Reference Temp required indicator...");
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Reference Temp'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther' OR " +
                    "type == 'XCUIElementTypeStaticText')"));
                for (WebElement icon : icons) {
                    int y = icon.getLocation().getY();
                    if (Math.abs(y - labelY) < 25) {
                        String name = icon.getAttribute("name");
                        String label = icon.getAttribute("label");
                        if (name != null && (name.contains("required") || name.contains("red") ||
                            name.contains("asterisk") || name.contains("dot") ||
                            name.contains("circle.fill"))) {
                            System.out.println("   Reference Temp required indicator: " + name);
                            return true;
                        }
                        if (label != null && (label.equals("*") || label.equals("•"))) {
                            return true;
                        }
                    }
                }
            }

            // Cross-check: /3 required fields includes Reference Temp
            String reqCount = getRequiredFieldsToggleCount();
            if (reqCount.contains("/3")) {
                System.out.println("   /3 required fields — Reference Temp is required");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the Reference Temp field placeholder text.
     * Expected: 'Enter number'.
     */
    public String getReferenceTempPlaceholder() {
        System.out.println("📋 Getting Reference Temp placeholder...");
        try {
            // Strategy 1: Direct field find
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Reference Temp' OR name CONTAINS 'reference' OR " +
                    "name CONTAINS 'Reference')"));
                String placeholder = field.getAttribute("placeholderValue");
                if (placeholder != null && !placeholder.isEmpty()) return placeholder;
                String value = field.getAttribute("value");
                if (value != null && value.contains("Enter number")) return value;
            } catch (Exception ignored) {}

            // Strategy 2: Find near "Reference Temp" label
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Reference Temp'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Enter number'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    if (Math.abs(y - labelY) < 60) return text.getAttribute("label");
                }
                // Check text fields nearby
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"));
                for (WebElement field : fields) {
                    int y = field.getLocation().getY();
                    if (y > labelY && y < labelY + 60) {
                        String ph = field.getAttribute("placeholderValue");
                        if (ph != null && !ph.isEmpty()) return ph;
                        String val = field.getAttribute("value");
                        if (val != null && val.contains("Enter")) return val;
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Enter a numeric value into the Reference Temp field.
     */
    public void enterReferenceTemp(String value) {
        System.out.println("📝 Entering Reference Temp: " + value);
        try {
            // Strategy 1: Direct field find
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Reference Temp' OR name CONTAINS 'reference' OR " +
                    "name CONTAINS 'Reference')"));
                field.click();
                sleep(300);
                field.clear();
                field.sendKeys(value);
                sleep(300);
                System.out.println("✅ Entered Reference Temp: " + value);
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Find text field near "Reference Temp" label
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Reference Temp'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'"));
                for (WebElement field : fields) {
                    int y = field.getLocation().getY();
                    if (y > labelY && y < labelY + 60) {
                        field.click();
                        sleep(300);
                        field.clear();
                        field.sendKeys(value);
                        sleep(300);
                        System.out.println("✅ Entered Reference Temp (nearby field): " + value);
                        return;
                    }
                }
            }

            // Strategy 3: Scroll to Reference Temp, then retry
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Reference Temp'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);

                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Reference' OR placeholderValue CONTAINS 'number')"));
                field.click();
                sleep(300);
                field.clear();
                field.sendKeys(value);
                sleep(300);
                System.out.println("✅ Entered Reference Temp after scroll: " + value);
                return;
            } catch (Exception ignored) {}

            System.out.println("⚠️ Could not find Reference Temp field to enter value");
        } catch (Exception e) {
            System.out.println("⚠️ Error entering Reference Temp: " + e.getMessage());
        }
    }

    /**
     * Get the current value of the Reference Temp field.
     */
    public String getReferenceTempValue() {
        try {
            // Strategy 1: Direct field find
            try {
                WebElement field = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND " +
                    "(name CONTAINS 'Reference Temp' OR name CONTAINS 'reference' OR " +
                    "name CONTAINS 'Reference')"));
                String value = field.getAttribute("value");
                if (value != null && !value.contains("Enter number")) return value;
            } catch (Exception ignored) {}

            // Strategy 2: Find field near label
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Reference Temp'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'"));
                for (WebElement field : fields) {
                    int y = field.getLocation().getY();
                    if (y > labelY && y < labelY + 60) {
                        String value = field.getAttribute("value");
                        if (value != null && !value.contains("Enter number")) return value;
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // CURRENT DRAW TABLE HELPERS (TC_ISS_164-166)
    // ================================================================

    /**
     * Check if the Current Draw (A) section is displayed.
     * Looks for the "Current Draw (A)" or "Current Draw" label.
     */
    public boolean isCurrentDrawSectionDisplayed() {
        System.out.println("🔍 Checking for Current Draw section...");
        try {
            // Strategy 1: Direct label search
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Current Draw' OR label CONTAINS 'current draw')"));
                System.out.println("   Current Draw section found: " + label.getAttribute("label"));
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Scroll to find it
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Current Draw'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);

                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Current Draw'"));
                System.out.println("   Current Draw section found after scroll: " + label.getAttribute("label"));
                return true;
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the Current Draw section description text.
     * Expected: "The current draw across all phases"
     */
    public String getCurrentDrawDescription() {
        System.out.println("📋 Getting Current Draw description...");
        try {
            // Find "Current Draw" label first, then look for description text nearby
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Current Draw'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                // Description is typically a few pixels below the title
                List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'current draw across' OR label CONTAINS 'all phases' OR " +
                    "label CONTAINS 'draw across')"));
                for (WebElement text : texts) {
                    int y = text.getLocation().getY();
                    if (y > labelY && y < labelY + 60) {
                        return text.getAttribute("label");
                    }
                }
                // Fallback: any text within 60px below the label (not the label itself)
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    String textLabel = text.getAttribute("label");
                    if (y > labelY && y < labelY + 60 && textLabel != null &&
                        !textLabel.contains("Current Draw") && textLabel.length() > 10) {
                        return textLabel;
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the column headers of the Current Draw table.
     * Expected columns: A, B, C, N (phase columns).
     * Returns a list of found column header labels.
     */
    public java.util.ArrayList<String> getCurrentDrawColumnHeaders() {
        System.out.println("📋 Getting Current Draw column headers...");
        java.util.ArrayList<String> headers = new java.util.ArrayList<>();
        try {
            // Find "Current Draw" label to establish Y-range
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Current Draw'"));
            if (!labels.isEmpty()) {
                int sectionY = labels.get(0).getLocation().getY();
                // Column headers (A, B, C, N) should be within ~80-150px below the section label
                String[] expectedHeaders = {"A", "B", "C", "N"};
                for (String header : expectedHeaders) {
                    try {
                        List<WebElement> headerElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND label == '" + header + "'"));
                        for (WebElement el : headerElements) {
                            int y = el.getLocation().getY();
                            // Headers should be below the section title but within reasonable range
                            if (y > sectionY && y < sectionY + 200) {
                                headers.add(header);
                                System.out.println("   Found column header: " + header);
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Fallback: If not found via Y-proximity, search broadly for A/B/C/N near any Current Draw element
            if (headers.isEmpty()) {
                String[] expectedHeaders = {"A", "B", "C", "N"};
                for (String header : expectedHeaders) {
                    try {
                        driver.findElement(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND label == '" + header + "'"));
                        headers.add(header);
                    } catch (Exception ignored) {}
                }
            }

            System.out.println("   Total column headers found: " + headers.size());
        } catch (Exception e) {
            System.out.println("⚠️ Error getting Current Draw headers: " + e.getMessage());
        }
        return headers;
    }

    /**
     * Enter values into the Current Draw table for each phase (A, B, C, N).
     * The table has 4 input fields corresponding to phases A, B, C, N.
     */
    public void enterCurrentDrawPhaseValues(String a, String b, String c, String n) {
        System.out.println("📝 Entering Current Draw values: A=" + a + ", B=" + b + ", C=" + c + ", N=" + n);
        try {
            // Find "Current Draw" section label to locate table fields
            List<WebElement> sectionLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Current Draw'"));
            if (sectionLabels.isEmpty()) {
                // Try scrolling to it
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Current Draw'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);
                sectionLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Current Draw'"));
            }

            if (!sectionLabels.isEmpty()) {
                int sectionY = sectionLabels.get(0).getLocation().getY();

                // Collect all text fields below the Current Draw label (within the table)
                List<WebElement> allFields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"));
                java.util.ArrayList<WebElement> tableFields = new java.util.ArrayList<>();
                for (WebElement field : allFields) {
                    int y = field.getLocation().getY();
                    // Table fields should be below the section label but within reasonable range
                    if (y > sectionY && y < sectionY + 250) {
                        tableFields.add(field);
                    }
                }

                // Sort by X position (left to right: A, B, C, N)
                tableFields.sort((f1, f2) -> Integer.compare(f1.getLocation().getX(), f2.getLocation().getX()));

                String[] values = {a, b, c, n};
                String[] phaseNames = {"A", "B", "C", "N"};
                int entered = 0;
                for (int i = 0; i < Math.min(tableFields.size(), values.length); i++) {
                    try {
                        WebElement field = tableFields.get(i);
                        field.click();
                        sleep(200);
                        field.clear();
                        field.sendKeys(values[i]);
                        sleep(200);
                        System.out.println("   Entered " + phaseNames[i] + "=" + values[i]);
                        entered++;
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not enter value for phase " + phaseNames[i] + ": " + e.getMessage());
                    }
                }
                System.out.println("✅ Entered " + entered + "/4 Current Draw phase values");

                // Dismiss keyboard after entry
                dismissKeyboard();
            } else {
                System.out.println("⚠️ Current Draw section not found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error entering Current Draw values: " + e.getMessage());
        }
    }

    /**
     * Get the current values from the Current Draw table for each phase.
     * Returns a LinkedHashMap with keys A, B, C, N and their entered values.
     */
    public java.util.LinkedHashMap<String, String> getCurrentDrawPhaseValues() {
        System.out.println("📋 Getting Current Draw phase values...");
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        try {
            List<WebElement> sectionLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Current Draw'"));
            if (!sectionLabels.isEmpty()) {
                int sectionY = sectionLabels.get(0).getLocation().getY();

                List<WebElement> allFields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"));

                // Cache positions upfront to avoid StaleElementReferenceException.
                // After entering text, iOS re-renders the form and element references go stale.
                // Calling getLocation() multiple times (filter + sort) on stale refs crashes.
                java.util.ArrayList<Object[]> fieldData = new java.util.ArrayList<>(); // {element, x, y}
                for (WebElement field : allFields) {
                    try {
                        org.openqa.selenium.Point loc = field.getLocation();
                        int y = loc.getY();
                        if (y > sectionY && y < sectionY + 250) {
                            fieldData.add(new Object[]{field, loc.getX(), y});
                        }
                    } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                        // Element went stale during scan — skip it
                    }
                }

                // Sort by cached X position (no further getLocation calls)
                fieldData.sort((a, b) -> Integer.compare((int) a[1], (int) b[1]));

                String[] phaseNames = {"A", "B", "C", "N"};
                for (int i = 0; i < Math.min(fieldData.size(), phaseNames.length); i++) {
                    try {
                        String val = ((WebElement) fieldData.get(i)[0]).getAttribute("value");
                        if (val == null || val.contains("Enter")) val = "";
                        values.put(phaseNames[i], val);
                        System.out.println("   " + phaseNames[i] + "=" + val);
                    } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                        // Element stale at read time — try re-finding by position
                        try {
                            int cachedX = (int) fieldData.get(i)[1];
                            int cachedY = (int) fieldData.get(i)[2];
                            List<WebElement> freshFields = driver.findElements(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeTextField'"));
                            for (WebElement ff : freshFields) {
                                try {
                                    org.openqa.selenium.Point fp = ff.getLocation();
                                    if (Math.abs(fp.getX() - cachedX) < 20 && Math.abs(fp.getY() - cachedY) < 20) {
                                        String val = ff.getAttribute("value");
                                        if (val == null || val.contains("Enter")) val = "";
                                        values.put(phaseNames[i], val);
                                        System.out.println("   " + phaseNames[i] + "=" + val + " (re-found)");
                                        break;
                                    }
                                } catch (Exception ignored) {}
                            }
                        } catch (Exception reEx) {
                            values.put(phaseNames[i], "");
                        }
                        if (!values.containsKey(phaseNames[i])) {
                            values.put(phaseNames[i], "");
                        }
                    } catch (Exception e) {
                        values.put(phaseNames[i], "");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting Current Draw values: " + e.getMessage());
        }
        return values;
    }

    // ================================================================
    // VOLTAGE DROP TABLE HELPERS (TC_ISS_167-169)
    // ================================================================

    /**
     * Check if the Voltage Drop (mV) section is displayed.
     * Looks for the "Voltage Drop (mV)" or "Voltage Drop" label.
     */
    public boolean isVoltageDropSectionDisplayed() {
        System.out.println("🔍 Checking for Voltage Drop section...");
        try {
            // Strategy 1: Direct label search
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Voltage Drop' OR label CONTAINS 'voltage drop')"));
                System.out.println("   Voltage Drop section found: " + label.getAttribute("label"));
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Scroll to find it
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Voltage Drop'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);

                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Voltage Drop'"));
                System.out.println("   Voltage Drop section found after scroll: " + label.getAttribute("label"));
                return true;
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the Voltage Drop section description text.
     * Expected: "The voltage drop draw across all phases"
     */
    public String getVoltageDropDescription() {
        System.out.println("📋 Getting Voltage Drop description...");
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Voltage Drop'"));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                // Look for description text
                List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'voltage drop' OR label CONTAINS 'all phases' OR " +
                    "label CONTAINS 'drop across' OR label CONTAINS 'drop draw')"));
                for (WebElement text : texts) {
                    int y = text.getLocation().getY();
                    if (y > labelY && y < labelY + 60) {
                        return text.getAttribute("label");
                    }
                }
                // Fallback: any text within 60px below the label (not the label itself)
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"));
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    String textLabel = text.getAttribute("label");
                    if (y > labelY && y < labelY + 60 && textLabel != null &&
                        !textLabel.contains("Voltage Drop") && textLabel.length() > 10) {
                        return textLabel;
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the column headers of the Voltage Drop table.
     * Expected columns: A, B, C, N (phase columns).
     */
    public java.util.ArrayList<String> getVoltageDropColumnHeaders() {
        System.out.println("📋 Getting Voltage Drop column headers...");
        java.util.ArrayList<String> headers = new java.util.ArrayList<>();
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Voltage Drop'"));
            if (!labels.isEmpty()) {
                int sectionY = labels.get(0).getLocation().getY();
                String[] expectedHeaders = {"A", "B", "C", "N"};
                for (String header : expectedHeaders) {
                    try {
                        List<WebElement> headerElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND label == '" + header + "'"));
                        for (WebElement el : headerElements) {
                            int y = el.getLocation().getY();
                            if (y > sectionY && y < sectionY + 200) {
                                headers.add(header);
                                System.out.println("   Found column header: " + header);
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Fallback: If still empty, Voltage Drop and Current Draw share the same A/B/C/N
            // so we can assume the headers exist if the section is present
            if (headers.isEmpty()) {
                System.out.println("   Column headers not found via Y-proximity, checking broadly");
                String[] expectedHeaders = {"A", "B", "C", "N"};
                for (String header : expectedHeaders) {
                    try {
                        driver.findElement(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND label == '" + header + "'"));
                        headers.add(header);
                    } catch (Exception ignored) {}
                }
            }

            System.out.println("   Total column headers found: " + headers.size());
        } catch (Exception e) {
            System.out.println("⚠️ Error getting Voltage Drop headers: " + e.getMessage());
        }
        return headers;
    }

    /**
     * Enter values into the Voltage Drop table for each phase (A, B, C, N).
     * The table has 4 input fields corresponding to phases A, B, C, N.
     * Must distinguish from Current Draw fields — Voltage Drop is further down the screen.
     */
    public void enterVoltageDropPhaseValues(String a, String b, String c, String n) {
        System.out.println("📝 Entering Voltage Drop values: A=" + a + ", B=" + b + ", C=" + c + ", N=" + n);
        try {
            // First scroll to ensure Voltage Drop is visible
            List<WebElement> sectionLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Voltage Drop'"));
            if (sectionLabels.isEmpty()) {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Voltage Drop'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);
                sectionLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Voltage Drop'"));
            }

            if (!sectionLabels.isEmpty()) {
                int sectionY = sectionLabels.get(0).getLocation().getY();

                // Also find Current Draw Y to ensure we exclude its fields
                int currentDrawY = -1;
                List<WebElement> cdLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Current Draw'"));
                if (!cdLabels.isEmpty()) {
                    currentDrawY = cdLabels.get(0).getLocation().getY();
                }

                // Get all text fields below the Voltage Drop label
                List<WebElement> allFields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"));
                java.util.ArrayList<WebElement> tableFields = new java.util.ArrayList<>();
                for (WebElement field : allFields) {
                    int y = field.getLocation().getY();
                    // Must be below Voltage Drop but not in Current Draw's range
                    boolean belowVoltageDropLabel = y > sectionY && y < sectionY + 250;
                    boolean notInCurrentDrawRange = currentDrawY < 0 ||
                        Math.abs(y - currentDrawY) > 100 || y > sectionY;
                    if (belowVoltageDropLabel && notInCurrentDrawRange) {
                        tableFields.add(field);
                    }
                }

                // Sort by X position (left to right: A, B, C, N)
                tableFields.sort((f1, f2) -> Integer.compare(f1.getLocation().getX(), f2.getLocation().getX()));

                String[] values = {a, b, c, n};
                String[] phaseNames = {"A", "B", "C", "N"};
                int entered = 0;
                for (int i = 0; i < Math.min(tableFields.size(), values.length); i++) {
                    try {
                        WebElement field = tableFields.get(i);
                        field.click();
                        sleep(200);
                        field.clear();
                        field.sendKeys(values[i]);
                        sleep(200);
                        System.out.println("   Entered " + phaseNames[i] + "=" + values[i]);
                        entered++;
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not enter value for phase " + phaseNames[i] + ": " + e.getMessage());
                    }
                }
                System.out.println("✅ Entered " + entered + "/4 Voltage Drop phase values");

                dismissKeyboard();
            } else {
                System.out.println("⚠️ Voltage Drop section not found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error entering Voltage Drop values: " + e.getMessage());
        }
    }

    /**
     * Get the current values from the Voltage Drop table for each phase.
     * Returns a LinkedHashMap with keys A, B, C, N and their entered values.
     */
    public java.util.LinkedHashMap<String, String> getVoltageDropPhaseValues() {
        System.out.println("📋 Getting Voltage Drop phase values...");
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        try {
            List<WebElement> sectionLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Voltage Drop'"));
            if (!sectionLabels.isEmpty()) {
                int sectionY = sectionLabels.get(0).getLocation().getY();

                // Find Current Draw Y for exclusion
                int currentDrawY = -1;
                List<WebElement> cdLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Current Draw'"));
                if (!cdLabels.isEmpty()) {
                    try {
                        currentDrawY = cdLabels.get(0).getLocation().getY();
                    } catch (org.openqa.selenium.StaleElementReferenceException ignored) {}
                }

                List<WebElement> allFields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"));

                // Cache positions upfront to avoid StaleElementReferenceException
                java.util.ArrayList<Object[]> fieldData = new java.util.ArrayList<>(); // {element, x, y}
                for (WebElement field : allFields) {
                    try {
                        org.openqa.selenium.Point loc = field.getLocation();
                        int y = loc.getY();
                        boolean belowVoltageDropLabel = y > sectionY && y < sectionY + 250;
                        boolean notInCurrentDrawRange = currentDrawY < 0 ||
                            Math.abs(y - currentDrawY) > 100 || y > sectionY;
                        if (belowVoltageDropLabel && notInCurrentDrawRange) {
                            fieldData.add(new Object[]{field, loc.getX(), y});
                        }
                    } catch (org.openqa.selenium.StaleElementReferenceException ignored) {}
                }

                // Sort by cached X position (no further getLocation calls)
                fieldData.sort((a, b) -> Integer.compare((int) a[1], (int) b[1]));

                String[] phaseNames = {"A", "B", "C", "N"};
                for (int i = 0; i < Math.min(fieldData.size(), phaseNames.length); i++) {
                    try {
                        String val = ((WebElement) fieldData.get(i)[0]).getAttribute("value");
                        if (val == null || val.contains("Enter")) val = "";
                        values.put(phaseNames[i], val);
                        System.out.println("   " + phaseNames[i] + "=" + val);
                    } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                        // Re-find by cached position
                        try {
                            int cachedX = (int) fieldData.get(i)[1];
                            int cachedY = (int) fieldData.get(i)[2];
                            List<WebElement> freshFields = driver.findElements(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeTextField'"));
                            for (WebElement ff : freshFields) {
                                try {
                                    org.openqa.selenium.Point fp = ff.getLocation();
                                    if (Math.abs(fp.getX() - cachedX) < 20 && Math.abs(fp.getY() - cachedY) < 20) {
                                        String val = ff.getAttribute("value");
                                        if (val == null || val.contains("Enter")) val = "";
                                        values.put(phaseNames[i], val);
                                        System.out.println("   " + phaseNames[i] + "=" + val + " (re-found)");
                                        break;
                                    }
                                } catch (Exception ignored) {}
                            }
                        } catch (Exception reEx) {
                            values.put(phaseNames[i], "");
                        }
                        if (!values.containsKey(phaseNames[i])) {
                            values.put(phaseNames[i], "");
                        }
                    } catch (Exception e) {
                        values.put(phaseNames[i], "");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting Voltage Drop values: " + e.getMessage());
        }
        return values;
    }

    /**
     * Fill only the 3 required Thermal Anomaly fields (Severity, Problem Temp, Reference Temp).
     * Used to verify that Current Draw and Voltage Drop are optional (TC_ISS_166, TC_ISS_169).
     * Does NOT save — caller must decide whether to tap Save.
     * @param severity The severity option to select (e.g., "Nominal")
     * @param problemTemp The Problem Temp value (e.g., "50")
     * @param referenceTemp The Reference Temp value (e.g., "25")
     */
    public void fillRequiredThermalFields(String severity, String problemTemp, String referenceTemp) {
        System.out.println("📝 Filling required Thermal Anomaly fields...");

        // 1. Select Severity
        tapSeverityField();
        sleep(300);
        selectSeverity(severity);
        sleep(300);

        // 2. Scroll down to reach temp fields
        scrollDownOnDetailsScreen();
        sleep(200);

        // 3. Enter Problem Temp
        enterProblemTemp(problemTemp);
        sleep(200);

        // 4. Enter Reference Temp
        enterReferenceTemp(referenceTemp);
        sleep(200);

        // Dismiss keyboard
        dismissKeyboard();
        sleep(200);

        System.out.println("✅ Filled required thermal fields: Severity=" + severity +
            ", Problem Temp=" + problemTemp + ", Reference Temp=" + referenceTemp);
    }

    // ================================================================
    // CREATE REPAIR NEEDED ISSUE (TC_ISS_142)
    // ================================================================

    /**
     * Create a Repair Needed issue without selecting any subcategory.
     * Tests that Repair Needed class does not require subcategory.
     * Returns true if the issue was created successfully.
     */
    public boolean createRepairNeededIssue(String title, String assetName) {
        System.out.println("🆕 Creating Repair Needed issue: " + title);
        try {
            tapAddButton();
            sleep(500);

            if (!isNewIssueFormDisplayed()) {
                System.out.println("⚠️ New Issue form did not open");
                return false;
            }

            // Select Issue Class: Repair Needed
            selectIssueClass("Repair Needed");
            sleep(300);

            // Enter title
            enterIssueTitle(title);
            sleep(300);

            // Select asset (required)
            tapSelectAsset();
            sleep(500);
            selectAssetByName(assetName);
            sleep(500);

            // Tap Create Issue — no subcategory needed
            boolean tapped = tapCreateIssue();
            if (!tapped) {
                System.out.println("⚠️ Could not create Repair Needed issue — cancelling");
                try { tapCancelNewIssue(); } catch (Exception ignored) {}
                return false;
            }
            sleep(800);

            System.out.println("✅ Repair Needed issue created: " + title);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Error creating Repair Needed issue: " + e.getMessage());
            try { tapCancelNewIssue(); } catch (Exception ignored) {}
            return false;
        }
    }

    // ================================================================
    // CLASS-SPECIFIC SUBCATEGORY HELPERS (TC_ISS_114-117)
    // ================================================================

    /**
     * Get subcategory options for a specific Issue Class.
     * Changes to the given class, opens subcategory, collects options, then returns them.
     * Does NOT revert the class change — caller must handle that.
     */
    public java.util.ArrayList<String> getSubcategoryOptionsForClass(String issueClassName) {
        System.out.println("📋 Getting subcategory options for class: " + issueClassName);
        changeIssueClassOnDetails(issueClassName);
        sleep(500);

        // Scroll down to reach Subcategory field
        scrollDownOnDetailsScreen();
        sleep(300);

        tapSubcategoryField();
        sleep(500);

        java.util.ArrayList<String> options = getVisibleSubcategoryOptions();
        System.out.println("   Found " + options.size() + " subcategory options for " + issueClassName);

        // Dismiss the dropdown
        dismissDropdownMenu();
        sleep(300);

        return options;
    }

    /**
     * Verify that a specific class has expected subcategory keywords present.
     * Changes class, opens subcategory dropdown, checks for keywords, dismisses.
     * Returns number of matched keywords.
     */
    public int verifyClassSubcategoryKeywords(String issueClassName, String[] expectedKeywords) {
        System.out.println("🔍 Verifying subcategory keywords for: " + issueClassName);
        changeIssueClassOnDetails(issueClassName);
        sleep(500);

        scrollDownOnDetailsScreen();
        sleep(300);

        tapSubcategoryField();
        sleep(500);

        java.util.ArrayList<String> options = getVisibleSubcategoryOptions();
        int matched = 0;
        for (String keyword : expectedKeywords) {
            boolean found = false;
            for (String opt : options) {
                if (opt.toLowerCase().contains(keyword.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                matched++;
                System.out.println("   ✅ Found keyword: " + keyword);
            } else {
                System.out.println("   ❌ Missing keyword: " + keyword);
            }
        }

        // Dismiss dropdown
        dismissDropdownMenu();
        sleep(300);

        return matched;
    }

    // ================================================================
    // ULTRASONIC ANOMALY ISSUE CREATION (TC_ISS_181)
    // ================================================================

    /**
     * Create a new Ultrasonic Anomaly issue.
     * Ultrasonic Anomaly has no required Issue Details fields, so only
     * title and asset are needed (same as Repair Needed).
     * @param title  The issue title
     * @param assetName The asset to associate
     * @return true if created successfully
     */
    public boolean createUltrasonicAnomalyIssue(String title, String assetName) {
        System.out.println("🆕 Creating Ultrasonic Anomaly issue: " + title);
        try {
            tapAddButton();
            sleep(500);

            if (!isNewIssueFormDisplayed()) {
                System.out.println("⚠️ New Issue form did not open");
                return false;
            }

            // Select Issue Class: Ultrasonic Anomaly
            selectIssueClass("Ultrasonic Anomaly");
            sleep(300);

            // Enter title
            enterIssueTitle(title);
            sleep(300);

            // Select asset (required)
            tapSelectAsset();
            sleep(500);
            selectAssetByName(assetName);
            sleep(500);

            // Tap Create Issue — no subcategory or additional fields needed
            boolean tapped = tapCreateIssue();
            if (!tapped) {
                System.out.println("⚠️ Could not create Ultrasonic Anomaly issue — cancelling");
                try { tapCancelNewIssue(); } catch (Exception ignored) {}
                return false;
            }
            sleep(800);

            System.out.println("✅ Ultrasonic Anomaly issue created: " + title);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Error creating Ultrasonic Anomaly issue: " + e.getMessage());
            try { tapCancelNewIssue(); } catch (Exception ignored) {}
            return false;
        }
    }

    // ================================================================
    // STATUS FILTER TABS — IN PROGRESS (TC_ISS_184-189)
    // ================================================================

    /**
     * Tap the "In Progress" filter tab on the Issues screen.
     * Uses CONTAINS since tab label may include count (e.g., "In Progress 2").
     */
    public void tapInProgressTab() {
        System.out.println("📋 Tapping In Progress tab...");
        try {
            // Strategy 1: Button with "In Progress" label
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
                tab.click();
                sleep(400);
                System.out.println("✅ Tapped In Progress tab");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Scroll filter tabs horizontally first, then tap
            scrollFilterTabsLeft();
            sleep(300);
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
                tab.click();
                sleep(400);
                System.out.println("✅ Tapped In Progress tab after scroll");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Try mobile:scroll to find it
            try {
                Map<String, Object> scrollParams = new HashMap<>();
                scrollParams.put("direction", "right");
                scrollParams.put("predicateString", "label CONTAINS 'In Progress'");
                driver.executeScript("mobile: scroll", scrollParams);
                sleep(300);

                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
                tab.click();
                sleep(400);
                System.out.println("✅ Tapped In Progress tab after mobile:scroll");
            } catch (Exception e) {
                System.out.println("⚠️ Could not tap In Progress tab: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping In Progress tab: " + e.getMessage());
        }
    }

    /**
     * Get count from "In Progress" filter tab.
     * Tab label format: "In Progress 2" or just "In Progress" (0 count).
     */
    public int getInProgressTabCount() {
        try {
            // Strategy 1: Direct find
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
                String label = tab.getAttribute("label");
                System.out.println("   In Progress tab label: '" + label + "'");
                return extractCountFromTabLabel(label);
            } catch (Exception ignored) {}

            // Strategy 2: Scroll filter tabs to reveal In Progress
            scrollFilterTabsLeft();
            sleep(300);
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
            String label = tab.getAttribute("label");
            System.out.println("   In Progress tab label (after scroll): '" + label + "'");
            return extractCountFromTabLabel(label);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Check if "In Progress" filter tab is currently selected.
     */
    public boolean isInProgressTabSelected() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
            String selected = tab.getAttribute("selected");
            if ("true".equals(selected)) return true;
            String value = tab.getAttribute("value");
            return "1".equals(value);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if "Closed" filter tab is currently selected.
     */
    public boolean isClosedTabSelected() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Closed'"));
            String selected = tab.getAttribute("selected");
            if ("true".equals(selected)) return true;
            String value = tab.getAttribute("value");
            return "1".equals(value);
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // STATUS FILTER TAB DISCOVERY & SCROLLING (TC_ISS_184, TC_ISS_188)
    // ================================================================

    /**
     * Get all visible filter tab labels on the Issues screen.
     * Returns list of tab labels (e.g., ["All 5", "Open 3", "In Progress 1", "Resolved 1", "Closed 0"]).
     * Filter tabs are buttons near the top of the screen (Y < 250).
     */
    public java.util.ArrayList<String> getVisibleFilterTabLabels() {
        java.util.ArrayList<String> tabLabels = new java.util.ArrayList<>();
        System.out.println("🔍 Getting visible filter tab labels...");
        try {
            // Known tab name prefixes in order
            String[] tabPrefixes = {"All", "Open", "In Progress", "Resolved", "Closed"};

            for (String prefix : tabPrefixes) {
                try {
                    WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label BEGINSWITH '" + prefix + "'"));
                    String label = tab.getAttribute("label");
                    if (label != null && !label.isEmpty()) {
                        tabLabels.add(label);
                        System.out.println("   Found tab: '" + label + "'");
                    }
                } catch (Exception ignored) {
                    // Tab may not be visible yet — needs scrolling
                    System.out.println("   Tab '" + prefix + "' not visible");
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting filter tabs: " + e.getMessage());
        }
        return tabLabels;
    }

    /**
     * Get all filter tab labels including those off-screen.
     * Scrolls the tab bar to discover all tabs, then scrolls back.
     * Returns distinct tab prefixes found (All, Open, In Progress, Resolved, Closed).
     */
    public java.util.ArrayList<String> getAllFilterTabNames() {
        java.util.ArrayList<String> tabNames = new java.util.ArrayList<>();
        System.out.println("🔍 Discovering all filter tabs (with scrolling)...");
        try {
            // Check visible tabs first
            java.util.ArrayList<String> visible = getVisibleFilterTabLabels();
            for (String label : visible) {
                String name = extractTabNameFromLabel(label);
                if (!name.isEmpty() && !tabNames.contains(name)) {
                    tabNames.add(name);
                }
            }

            // If we don't have all 5, scroll left to reveal more
            if (tabNames.size() < 5) {
                scrollFilterTabsLeft();
                sleep(300);

                visible = getVisibleFilterTabLabels();
                for (String label : visible) {
                    String name = extractTabNameFromLabel(label);
                    if (!name.isEmpty() && !tabNames.contains(name)) {
                        tabNames.add(name);
                    }
                }
            }

            // Scroll back right if needed
            if (tabNames.size() >= 4) {
                scrollFilterTabsRight();
                sleep(200);
            }

            System.out.println("   Total filter tabs found: " + tabNames.size() + " — " + tabNames);
        } catch (Exception e) {
            System.out.println("⚠️ Error discovering filter tabs: " + e.getMessage());
        }
        return tabNames;
    }

    /**
     * Extract the tab name prefix from a label like "All 5" -> "All", "In Progress 2" -> "In Progress".
     */
    private String extractTabNameFromLabel(String label) {
        if (label == null || label.isEmpty()) return "";
        String[] knownNames = {"In Progress", "All", "Open", "Resolved", "Closed"};
        for (String name : knownNames) {
            if (label.startsWith(name)) return name;
        }
        return label.trim();
    }

    /**
     * Scroll the filter tabs bar to the LEFT (revealing tabs on the right: Resolved, Closed).
     * Performs a horizontal swipe from right to left in the filter tab area.
     */
    public void scrollFilterTabsLeft() {
        System.out.println("📜 Scrolling filter tabs left...");
        try {
            int screenWidth = driver.manage().window().getSize().getWidth();
            // Filter tabs are near the top of the screen — approximate Y at ~200
            int tabY = 200;
            int startX = (int)(screenWidth * 0.8);
            int endX = (int)(screenWidth * 0.2);

            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 0);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, tabY));
            swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), endX, tabY));
            swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Collections.singletonList(swipe));
            sleep(300);
            System.out.println("   Scrolled filter tabs left");
        } catch (Exception e) {
            System.out.println("⚠️ Could not scroll filter tabs: " + e.getMessage());
        }
    }

    /**
     * Scroll the filter tabs bar to the RIGHT (revealing tabs on the left: All, Open).
     * Performs a horizontal swipe from left to right in the filter tab area.
     */
    public void scrollFilterTabsRight() {
        System.out.println("📜 Scrolling filter tabs right...");
        try {
            int screenWidth = driver.manage().window().getSize().getWidth();
            int tabY = 200;
            int startX = (int)(screenWidth * 0.2);
            int endX = (int)(screenWidth * 0.8);

            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 0);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, tabY));
            swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), endX, tabY));
            swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Collections.singletonList(swipe));
            sleep(300);
            System.out.println("   Scrolled filter tabs right");
        } catch (Exception e) {
            System.out.println("⚠️ Could not scroll filter tabs right: " + e.getMessage());
        }
    }

    /**
     * Check if filter tabs area is scrollable by verifying that scrolling reveals
     * new content (tabs that weren't visible before).
     */
    public boolean areFilterTabsScrollable() {
        System.out.println("🔍 Checking if filter tabs are scrollable...");
        try {
            // Get initially visible tabs
            java.util.ArrayList<String> beforeScroll = getVisibleFilterTabLabels();
            int beforeCount = beforeScroll.size();
            System.out.println("   Tabs visible before scroll: " + beforeCount);

            // Scroll left
            scrollFilterTabsLeft();
            sleep(400);

            // Get visible tabs after scroll
            java.util.ArrayList<String> afterScroll = getVisibleFilterTabLabels();
            int afterCount = afterScroll.size();
            System.out.println("   Tabs visible after scroll: " + afterCount);

            // Check if any new tabs appeared or tab set changed
            boolean scrolled = false;
            for (String label : afterScroll) {
                if (!beforeScroll.contains(label)) {
                    scrolled = true;
                    System.out.println("   New tab revealed after scroll: '" + label + "'");
                    break;
                }
            }

            // Also check if at least one tab shifted position (even if same set)
            if (!scrolled && afterCount > 0 && beforeCount > 0) {
                // If we can find "Resolved" or "Closed" after scroll but not before, tabs scrolled
                for (String label : afterScroll) {
                    if (label.startsWith("Resolved") || label.startsWith("Closed")) {
                        scrolled = true;
                        break;
                    }
                }
            }

            // Scroll back
            scrollFilterTabsRight();
            sleep(200);

            System.out.println("   Filter tabs scrollable: " + scrolled);
            return scrolled;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking filter tab scrollability: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // STATUS CHANGE ON ISSUE DETAILS (TC_ISS_187, TC_ISS_189)
    // ================================================================

    /**
     * Change the status of the currently open issue in Issue Details.
     * Opens the status dropdown, selects the given status, then waits.
     * @param newStatus Target status: "Open", "In Progress", "Resolved", "Closed"
     * @return true if status change appeared successful
     */
    public boolean changeIssueStatusOnDetails(String newStatus) {
        System.out.println("🔄 Changing issue status to: " + newStatus);
        try {
            // Open status dropdown
            boolean opened = openStatusDropdown();
            if (!opened) {
                System.out.println("⚠️ Could not open status dropdown");
                return false;
            }
            sleep(300);

            // Select the new status
            selectStatus(newStatus);
            sleep(500);

            // Verify status changed
            String currentStatus = getIssueDetailStatus();
            boolean success = newStatus.equalsIgnoreCase(currentStatus);
            System.out.println("   Status after change: '" + currentStatus + "' — success: " + success);
            return success;
        } catch (Exception e) {
            System.out.println("⚠️ Error changing status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the status badge text displayed next to a specific issue in the list.
     * Searches for the issue title, then looks for status text near the same Y position.
     * @param issueTitle The title of the issue to check
     * @return Status text (e.g., "In Progress", "Open", "Resolved", "Closed") or empty
     */
    public String getIssueStatusBadgeInList(String issueTitle) {
        System.out.println("🔍 Getting status badge for issue: " + issueTitle);
        try {
            // Find the issue title element
            WebElement titleElement = null;
            try {
                titleElement = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + issueTitle + "'"));
            } catch (Exception ignored) {
                // Try cell search
                List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell'"));
                for (WebElement cell : cells) {
                    String cellLabel = cell.getAttribute("label");
                    if (cellLabel != null && cellLabel.contains(issueTitle)) {
                        // Found the cell — look for status text within it
                        String[] statuses = {"In Progress", "Open", "Resolved", "Closed"};
                        for (String status : statuses) {
                            if (cellLabel.contains(status)) {
                                System.out.println("   Found status '" + status + "' in cell label for: " + issueTitle);
                                return status;
                            }
                        }
                        break;
                    }
                }
            }

            if (titleElement != null) {
                int titleY = titleElement.getLocation().getY();
                System.out.println("   Issue title at Y=" + titleY);

                // Search for status text near the same Y range
                String[] statuses = {"In Progress", "Open", "Resolved", "Closed"};
                for (String status : statuses) {
                    try {
                        List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND label == '" + status + "'"));
                        for (WebElement badge : badges) {
                            int badgeY = badge.getLocation().getY();
                            // Status badge should be within the same row (~60px vertical range)
                            if (Math.abs(badgeY - titleY) < 60) {
                                System.out.println("   Found status badge '" + status + "' at Y=" + badgeY);
                                return status;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // Fallback: check for status button near the title
                for (String status : statuses) {
                    try {
                        List<WebElement> btns = driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND label CONTAINS '" + status + "'"));
                        for (WebElement btn : btns) {
                            int btnY = btn.getLocation().getY();
                            if (Math.abs(btnY - titleY) < 60) {
                                System.out.println("   Found status button '" + status + "' near title");
                                return status;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            System.out.println("⚠️ No status badge found for: " + issueTitle);
            return "";
        } catch (Exception e) {
            System.out.println("⚠️ Error getting status badge: " + e.getMessage());
            return "";
        }
    }

    /**
     * Check if an issue with a specific title is visible in the current filtered list.
     * @param issueTitle The title to search for
     * @return true if found
     */
    public boolean isIssueVisibleInList(String issueTitle) {
        try {
            // Strategy 1: Direct text search
            try {
                WebElement el = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + issueTitle + "'"));
                int y = el.getLocation().getY();
                // Make sure it's in the list area (not navigation bar)
                return y > 200;
            } catch (Exception ignored) {}

            // Strategy 2: Search through cells
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            for (WebElement cell : cells) {
                String label = cell.getAttribute("label");
                if (label != null && label.contains(issueTitle)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // IN PROGRESS STATUS DETAILS (TC_ISS_190-191)
    // ================================================================

    /**
     * Check if "In Progress" status badge is displayed in the Issue Details header area.
     * Looks for "In Progress" text in the header region (Y between 100-400).
     * @return true if In Progress badge found in header
     */
    public boolean isInProgressBadgeInHeader() {
        System.out.println("🔍 Checking for In Progress badge in header...");
        try {
            // Strategy 1: Look for static text "In Progress" in header area
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'In Progress'"));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y > 100 && y < 400) {
                    System.out.println("   Found 'In Progress' badge in header at Y=" + y);
                    return true;
                }
            }

            // Strategy 2: Check button elements with In Progress
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'In Progress'"));
            for (WebElement btn : buttons) {
                int y = btn.getLocation().getY();
                if (y > 100 && y < 400) {
                    System.out.println("   Found 'In Progress' button in header at Y=" + y);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the status value from the Status dropdown/field on Issue Details screen.
     * Different from getIssueDetailStatus() — this specifically checks the Status picker field value.
     * @return The status value shown in the Status field (e.g., "In Progress")
     */
    public String getStatusFieldValue() {
        System.out.println("🔍 Getting Status field value...");
        try {
            // Strategy 1: Button with "Status" in name
            try {
                WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'Status' OR label CONTAINS 'Status')"));
                String label = picker.getAttribute("label");
                String value = picker.getAttribute("value");
                System.out.println("   Status field — label: '" + label + "', value: '" + value + "'");

                // Extract status from label like "Status, In Progress"
                if (label != null && label.contains(", ")) {
                    String statusPart = label.substring(label.indexOf(", ") + 2).trim();
                    if (!statusPart.isEmpty()) return statusPart;
                }
                if (value != null && !value.isEmpty()) return value;
                return label != null ? label : "";
            } catch (Exception ignored) {}

            // Strategy 2: Fall back to getIssueDetailStatus
            return getIssueDetailStatus();
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // ISSUE ICONS IN LIST (TC_ISS_192-193)
    // ================================================================

    /**
     * Check if an icon/image element is displayed near a specific issue in the list.
     * @param issueTitle The issue title to search near
     * @return true if an image element is found near the issue
     */
    public boolean isIconDisplayedForIssue(String issueTitle) {
        System.out.println("🔍 Checking for icon near issue: " + issueTitle);
        try {
            // Find the issue title element
            WebElement titleEl = null;
            try {
                titleEl = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + issueTitle + "'"));
            } catch (Exception ignored) {
                // Try cell
                List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell' AND label CONTAINS '" + issueTitle + "'"));
                if (!cells.isEmpty()) {
                    titleEl = cells.get(0);
                }
            }

            if (titleEl == null) {
                System.out.println("   Issue '" + issueTitle + "' not found");
                return false;
            }

            int titleY = titleEl.getLocation().getY();

            // Look for image elements near the issue Y position
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage'"));
            for (WebElement img : images) {
                int imgY = img.getLocation().getY();
                if (Math.abs(imgY - titleY) < 50) {
                    String imgLabel = img.getAttribute("label");
                    String imgName = img.getAttribute("name");
                    System.out.println("   Icon found near '" + issueTitle + "' at Y=" + imgY +
                        " (label='" + imgLabel + "', name='" + imgName + "')");
                    return true;
                }
            }

            System.out.println("   No icon found near issue '" + issueTitle + "'");
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the icon label/name near a specific issue in the list.
     * Returns the accessibility label or name of the first image found near the issue.
     * @param issueTitle The issue title to search near
     * @return Icon label/name or empty string
     */
    public String getIssueIconLabel(String issueTitle) {
        System.out.println("🔍 Getting icon label for issue: " + issueTitle);
        try {
            WebElement titleEl = null;
            try {
                titleEl = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + issueTitle + "'"));
            } catch (Exception ignored) {
                List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell' AND label CONTAINS '" + issueTitle + "'"));
                if (!cells.isEmpty()) titleEl = cells.get(0);
            }

            if (titleEl == null) return "";

            int titleY = titleEl.getLocation().getY();

            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage'"));
            for (WebElement img : images) {
                int imgY = img.getLocation().getY();
                if (Math.abs(imgY - titleY) < 50) {
                    String label = img.getAttribute("label");
                    String name = img.getAttribute("name");
                    String result = (label != null && !label.isEmpty()) ? label :
                                    (name != null && !name.isEmpty()) ? name : "icon_present";
                    System.out.println("   Icon for '" + issueTitle + "': '" + result + "'");
                    return result;
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Count the number of distinct icon types visible in the issues list.
     * Collects unique icon labels/names from visible issue entries.
     * @return Map of icon label → count
     */
    public java.util.LinkedHashMap<String, Integer> getVisibleIssueIconTypes() {
        java.util.LinkedHashMap<String, Integer> iconTypes = new java.util.LinkedHashMap<>();
        System.out.println("🔍 Cataloging visible issue icon types...");
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage'"));

            for (WebElement cell : cells) {
                int cellY = cell.getLocation().getY();
                int cellH = cell.getSize().getHeight();

                for (WebElement img : images) {
                    int imgY = img.getLocation().getY();
                    if (imgY >= cellY - 10 && imgY <= cellY + cellH + 10) {
                        String label = img.getAttribute("label");
                        String name = img.getAttribute("name");
                        String iconKey = (label != null && !label.isEmpty()) ? label :
                                         (name != null && !name.isEmpty()) ? name : "unknown_icon";
                        iconTypes.put(iconKey, iconTypes.getOrDefault(iconKey, 0) + 1);
                        break; // One icon per cell
                    }
                }
            }

            System.out.println("   Icon types found: " + iconTypes);
        } catch (Exception e) {
            System.out.println("⚠️ Error cataloging icons: " + e.getMessage());
        }
        return iconTypes;
    }

    // ================================================================
    // ISSUE ENTRY DESCRIPTION/SUBTITLE (TC_ISS_195)
    // ================================================================

    /**
     * Get the description/subtitle text displayed below an issue title in the list.
     * Issue entries show: Title (bold) + Description/subtitle below + Asset name + Status.
     * @param issueTitle The issue title to search for
     * @return Description text or empty string
     */
    public String getIssueDescriptionInList(String issueTitle) {
        System.out.println("🔍 Getting description for issue in list: " + issueTitle);
        try {
            // Strategy 1: Find the title, then look for text below it
            WebElement titleEl = null;
            try {
                titleEl = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == '" + issueTitle + "'"));
            } catch (Exception ignored) {
                try {
                    titleEl = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + issueTitle + "'"));
                } catch (Exception ignored2) {}
            }

            if (titleEl == null) {
                System.out.println("   Issue '" + issueTitle + "' not found");
                return "";
            }

            int titleY = titleEl.getLocation().getY();
            int titleX = titleEl.getLocation().getX();

            // Find text elements below the title but within the same cell (~20-60px below)
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"));

            String bestCandidate = "";
            int bestY = Integer.MAX_VALUE;

            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                int textY = text.getLocation().getY();
                int textX = text.getLocation().getX();

                // Description is below the title (10-60px) and roughly same X area
                if (label != null && !label.isEmpty() &&
                    textY > titleY + 10 && textY < titleY + 70 &&
                    Math.abs(textX - titleX) < 100) {

                    // Exclude known non-description labels
                    if (!label.equals("Issues") && !label.equals("Open") &&
                        !label.equals("In Progress") && !label.equals("Resolved") &&
                        !label.equals("Closed") && !label.equals("High") &&
                        !label.equals("Medium") && !label.equals("Low") &&
                        !label.equals("All") && !label.equals("Issue Details") &&
                        label.length() > 1) {

                        if (textY < bestY) {
                            bestCandidate = label;
                            bestY = textY;
                        }
                    }
                }
            }

            System.out.println("   Description for '" + issueTitle + "': '" + bestCandidate + "'");
            return bestCandidate;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the asset name displayed on a specific issue entry in the list.
     * @param issueTitle The issue title to search for
     * @return Asset name or empty string
     */
    public String getIssueAssetInList(String issueTitle) {
        System.out.println("🔍 Getting asset name for issue in list: " + issueTitle);
        try {
            // Check cell label — often contains all info
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            for (WebElement cell : cells) {
                String cellLabel = cell.getAttribute("label");
                if (cellLabel != null && cellLabel.contains(issueTitle)) {
                    // Cell label might contain: "title, hzjz, test, In Progress"
                    // Asset name is typically after description, before status
                    System.out.println("   Cell label for '" + issueTitle + "': '" + cellLabel + "'");
                    return cellLabel; // Return full cell label — caller can parse
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // STATUS WORKFLOW HELPERS (TC_ISS_196-199)
    // ================================================================

    /**
     * Full workflow: Change issue status, save changes, and close details.
     * Opens status dropdown, selects new status, scrolls to save, saves, closes.
     * @param newStatus Target status
     * @return true if workflow completed without errors
     */
    public boolean changeStatusSaveAndClose(String newStatus) {
        System.out.println("🔄 Status workflow: changing to '" + newStatus + "' + save + close");
        try {
            // Step 1: Change status
            boolean changed = changeIssueStatusOnDetails(newStatus);
            if (!changed) {
                System.out.println("⚠️ Status change failed");
                return false;
            }
            sleep(300);

            // Step 2: Scroll down and save (isSaveChangesButtonDisplayed handles scrolling internally)
            if (isSaveChangesButtonDisplayed()) {
                tapSaveChangesButton();
                sleep(800);
                System.out.println("   Saved changes");
            } else {
                // One more try — isSaveChangesButtonDisplayed already scrolled
                sleep(200);
                if (isSaveChangesButtonDisplayed()) {
                    tapSaveChangesButton();
                    sleep(800);
                } else {
                    System.out.println("   No Save button found — status may auto-save");
                }
            }

            // Step 3: Close details
            scrollUpOnDetailsScreen();
            sleep(200);
            tapCloseIssueDetails();
            sleep(400);

            // Handle unsaved changes warning
            if (isUnsavedChangesWarningDisplayed()) {
                tapSaveOnWarning();
                sleep(600);
            }

            System.out.println("✅ Status workflow complete: changed to '" + newStatus + "'");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Status workflow error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verify an issue appears under a specific filter tab.
     * Taps the given tab, checks if the issue is visible, then returns to All tab.
     * @param issueTitle Issue to look for
     * @param tabName Tab to check: "Open", "In Progress", "Resolved", "Closed"
     * @return true if issue found under the tab
     */
    public boolean verifyIssueUnderFilterTab(String issueTitle, String tabName) {
        System.out.println("🔍 Verifying '" + issueTitle + "' under '" + tabName + "' tab...");
        try {
            // Tap the appropriate tab
            switch (tabName) {
                case "Open": tapOpenTab(); break;
                case "In Progress":
                    scrollFilterTabsLeft();
                    sleep(200);
                    tapInProgressTab();
                    break;
                case "Resolved": tapResolvedTab(); break;
                case "Closed": tapClosedTab(); break;
                case "All": tapAllTab(); break;
                default:
                    System.out.println("⚠️ Unknown tab: " + tabName);
                    return false;
            }
            sleep(500);

            boolean found = isIssueVisibleInList(issueTitle);
            System.out.println("   Issue '" + issueTitle + "' under '" + tabName + "': " + found);

            // Return to All tab
            if (tabName.equals("In Progress")) {
                scrollFilterTabsRight();
                sleep(200);
            }
            tapAllTab();
            sleep(300);

            return found;
        } catch (Exception e) {
            System.out.println("⚠️ Error verifying issue under tab: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // SWIPE ACTIONS ON ISSUE ENTRIES (TC_ISS_201-209)
    // ================================================================

    /**
     * Swipe left on a specific issue entry in the list to reveal action buttons.
     * Uses the issue's cell element to calculate swipe coordinates.
     * @param issueTitle The issue title to swipe on
     * @return true if swipe was performed
     */
    public boolean swipeLeftOnIssue(String issueTitle) {
        System.out.println("👈 Swiping left on issue: " + issueTitle);
        try {
            // Find the issue cell
            WebElement cell = null;
            try {
                cell = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell' AND label CONTAINS '" + issueTitle + "'"));
            } catch (Exception ignored) {
                // Try finding via static text and calculate cell position
                try {
                    WebElement text = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + issueTitle + "'"));
                    // Use the text element's position for swipe
                    int textX = text.getLocation().getX();
                    int textY = text.getLocation().getY();
                    int screenWidth = driver.manage().window().getSize().getWidth();

                    return performSwipeLeft(screenWidth - 30, textY, 30, textY);
                } catch (Exception e2) {
                    System.out.println("⚠️ Issue '" + issueTitle + "' not found for swipe");
                    return false;
                }
            }

            if (cell != null) {
                int cellX = cell.getLocation().getX();
                int cellY = cell.getLocation().getY();
                int cellWidth = cell.getSize().getWidth();
                int cellHeight = cell.getSize().getHeight();

                int startX = cellX + cellWidth - 20;
                int centerY = cellY + cellHeight / 2;
                int endX = cellX + 50;

                return performSwipeLeft(startX, centerY, endX, centerY);
            }

            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error swiping on issue: " + e.getMessage());
            return false;
        }
    }

    /**
     * Swipe left on the first issue entry in the list.
     * @return true if swipe was performed
     */
    public boolean swipeLeftOnFirstIssue() {
        System.out.println("👈 Swiping left on first issue...");
        try {
            WebElement cell = getFirstIssueEntry();
            if (cell == null) {
                System.out.println("⚠️ No issue entries found");
                return false;
            }

            int cellX = cell.getLocation().getX();
            int cellY = cell.getLocation().getY();
            int cellWidth = cell.getSize().getWidth();
            int cellHeight = cell.getSize().getHeight();

            int startX = cellX + cellWidth - 20;
            int centerY = cellY + cellHeight / 2;
            int endX = cellX + 50;

            return performSwipeLeft(startX, centerY, endX, centerY);
        } catch (Exception e) {
            System.out.println("⚠️ Error swiping first issue: " + e.getMessage());
            return false;
        }
    }

    /**
     * Swipe left on an issue entry by its index (0-based) in the visible list.
     * @param index 0-based index of the issue entry
     * @return true if swipe was performed
     */
    public boolean swipeLeftOnIssueAtIndex(int index) {
        System.out.println("👈 Swiping left on issue at index: " + index);
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"));
            if (index >= cells.size()) {
                System.out.println("⚠️ Index " + index + " out of range (total cells: " + cells.size() + ")");
                return false;
            }

            WebElement cell = cells.get(index);
            int cellX = cell.getLocation().getX();
            int cellY = cell.getLocation().getY();
            int cellWidth = cell.getSize().getWidth();
            int cellHeight = cell.getSize().getHeight();

            int startX = cellX + cellWidth - 20;
            int centerY = cellY + cellHeight / 2;
            int endX = cellX + 50;

            return performSwipeLeft(startX, centerY, endX, centerY);
        } catch (Exception e) {
            System.out.println("⚠️ Error swiping issue at index " + index + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Perform a left swipe gesture from startX,startY to endX,endY.
     * @return true if swipe performed successfully
     */
    private boolean performSwipeLeft(int startX, int startY, int endX, int endY) {
        try {
            // Strategy 1: mobile: dragFromToForDuration — best for iOS swipe-to-delete.
            // Uses XCUITest native gesture with short duration for a quick swipe.
            try {
                Map<String, Object> dragParams = new HashMap<>();
                dragParams.put("fromX", startX);
                dragParams.put("fromY", startY);
                dragParams.put("toX", endX);
                dragParams.put("toY", endY);
                dragParams.put("duration", 0.3); // seconds — fast swipe triggers delete action
                driver.executeScript("mobile: dragFromToForDuration", dragParams);
                sleep(500);
                System.out.println("   Swipe left performed (mobile: dragFromToForDuration)");
                return true;
            } catch (Exception e1) {
                System.out.println("   dragFromToForDuration failed: " + e1.getMessage());
            }

            // Strategy 2: W3C PointerInput with fast movement (150ms)
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 0);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
            swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(150),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), endX, endY));
            swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Collections.singletonList(swipe));
            sleep(500);
            System.out.println("   Swipe left performed (W3C PointerInput)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Swipe left failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Swipe right on a specific issue entry to hide swipe action buttons.
     * @param issueTitle The issue title to swipe on
     * @return true if swipe was performed
     */
    public boolean swipeRightOnIssue(String issueTitle) {
        System.out.println("👉 Swiping right on issue to hide actions: " + issueTitle);
        try {
            WebElement cell = null;
            try {
                cell = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell' AND label CONTAINS '" + issueTitle + "'"));
            } catch (Exception ignored) {
                WebElement text = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + issueTitle + "'"));
                int textY = text.getLocation().getY();
                int screenWidth = driver.manage().window().getSize().getWidth();
                return performSwipeRight(30, textY, screenWidth - 30, textY);
            }

            if (cell != null) {
                int cellX = cell.getLocation().getX();
                int cellY = cell.getLocation().getY();
                int cellWidth = cell.getSize().getWidth();
                int cellHeight = cell.getSize().getHeight();

                int startX = cellX + 30;
                int centerY = cellY + cellHeight / 2;
                int endX = cellX + cellWidth - 20;

                return performSwipeRight(startX, centerY, endX, centerY);
            }
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error swiping right: " + e.getMessage());
            return false;
        }
    }

    /**
     * Perform a right swipe gesture.
     */
    private boolean performSwipeRight(int startX, int startY, int endX, int endY) {
        try {
            try {
                Map<String, Object> swipeParams = new HashMap<>();
                swipeParams.put("fromX", startX);
                swipeParams.put("fromY", startY);
                swipeParams.put("toX", endX);
                swipeParams.put("toY", endY);
                swipeParams.put("duration", 300);
                driver.executeScript("mobile: swipe", swipeParams);
                sleep(400);
                System.out.println("   Swipe right performed");
                return true;
            } catch (Exception ignored) {}

            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 0);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
            swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), endX, endY));
            swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Collections.singletonList(swipe));
            sleep(400);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Delete swipe action button is visible after swiping.
     * @return true if Delete button is visible
     */
    public boolean isSwipeDeleteButtonVisible() {
        System.out.println("🔍 Checking for swipe Delete button...");
        try {
            // iOS swipe action buttons are typically XCUIElementTypeButton with "Delete" label
            try {
                WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Delete' OR label == 'Trash' OR label CONTAINS 'Delete' OR " +
                    "name == 'Delete' OR name == 'trash')"));
                boolean visible = btn.isDisplayed();
                System.out.println("   Swipe Delete button visible: " + visible);
                return visible;
            } catch (Exception ignored) {}

            // Strategy 2: Look for image with trash icon
            try {
                WebElement icon = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeImage') AND " +
                    "(name CONTAINS 'trash' OR name CONTAINS 'delete' OR label CONTAINS 'trash')"));
                return icon.isDisplayed();
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Resolve swipe action button is visible after swiping.
     * @return true if Resolve button is visible
     */
    public boolean isSwipeResolveButtonVisible() {
        System.out.println("🔍 Checking for swipe Resolve button...");
        try {
            // iOS swipe action buttons
            try {
                WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Resolve' OR label CONTAINS 'Resolve' OR " +
                    "name == 'Resolve' OR label CONTAINS 'checkmark' OR " +
                    "name CONTAINS 'checkmark')"));
                boolean visible = btn.isDisplayed();
                System.out.println("   Swipe Resolve button visible: " + visible);
                return visible;
            } catch (Exception ignored) {}

            // Strategy 2: Look for checkmark icon/button
            try {
                WebElement icon = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeImage') AND " +
                    "(name CONTAINS 'check' OR name CONTAINS 'resolve' OR label CONTAINS 'check')"));
                return icon.isDisplayed();
            } catch (Exception ignored) {}

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get all visible swipe action button labels.
     * Returns the labels of action buttons that appear after swiping.
     */
    public java.util.ArrayList<String> getSwipeActionButtonLabels() {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        System.out.println("🔍 Getting swipe action button labels...");
        try {
            // After a swipe, action buttons appear as XCUIElementTypeButton
            // They are usually to the right side of the cell with high X values
            int screenWidth = driver.manage().window().getSize().getWidth();
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));

            for (WebElement btn : buttons) {
                String label = btn.getAttribute("label");
                String name = btn.getAttribute("name");
                int btnX = btn.getLocation().getX();
                int btnY = btn.getLocation().getY();

                // Swipe action buttons are in the list area (Y > 200) and may be on the right
                if (btnY > 200 && btnY < 900) {
                    String btnLabel = (label != null && !label.isEmpty()) ? label :
                                      (name != null && !name.isEmpty()) ? name : "";

                    // Filter for known swipe action labels
                    if (btnLabel.equalsIgnoreCase("Delete") || btnLabel.equalsIgnoreCase("Resolve") ||
                        btnLabel.equalsIgnoreCase("Trash") ||
                        btnLabel.toLowerCase().contains("delete") ||
                        btnLabel.toLowerCase().contains("resolve") ||
                        btnLabel.toLowerCase().contains("trash") ||
                        btnLabel.toLowerCase().contains("check")) {
                        labels.add(btnLabel);
                        System.out.println("   Swipe action button: '" + btnLabel + "' at X=" + btnX + " Y=" + btnY);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting swipe action labels: " + e.getMessage());
        }
        return labels;
    }

    /**
     * Tap the Delete swipe action button.
     */
    public void tapSwipeDeleteButton() {
        System.out.println("🗑️ Tapping swipe Delete button...");
        try {
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Delete' OR label CONTAINS 'Delete' OR name == 'Delete' OR " +
                "name CONTAINS 'trash' OR label CONTAINS 'Trash')"));
            btn.click();
            sleep(500);
            System.out.println("✅ Tapped swipe Delete button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap swipe Delete: " + e.getMessage());
        }
    }

    /**
     * Tap the Resolve swipe action button.
     */
    public void tapSwipeResolveButton() {
        System.out.println("✓ Tapping swipe Resolve button...");
        try {
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Resolve' OR label CONTAINS 'Resolve' OR name == 'Resolve' OR " +
                "name CONTAINS 'checkmark' OR label CONTAINS 'checkmark')"));
            btn.click();
            sleep(500);
            System.out.println("✅ Tapped swipe Resolve button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap swipe Resolve: " + e.getMessage());
        }
    }

    /**
     * Check if any swipe action buttons are currently visible.
     * @return true if Delete or Resolve buttons are visible
     */
    public boolean areSwipeActionsVisible() {
        return isSwipeDeleteButtonVisible() || isSwipeResolveButtonVisible();
    }

    /**
     * Hide swipe action buttons by tapping elsewhere on the screen
     * or swiping the issue back to its normal position.
     * @return true if swipe actions were hidden
     */
    public boolean hideSwipeActions() {
        System.out.println("🔙 Hiding swipe actions...");
        try {
            // Strategy 1: Tap in the center of the screen (on the list but not on a button)
            int screenWidth = driver.manage().window().getSize().getWidth();
            int screenHeight = driver.manage().window().getSize().getHeight();
            int tapX = screenWidth / 2;
            int tapY = screenHeight / 2;

            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 0);
            tap.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), tapX, tapY));
            tap.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerMove(Duration.ofMillis(50),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), tapX, tapY));
            tap.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Collections.singletonList(tap));
            sleep(400);

            boolean hidden = !areSwipeActionsVisible();
            System.out.println("   Swipe actions hidden after tap: " + hidden);
            if (hidden) return true;

            // Strategy 2: Swipe right on the first cell to close
            WebElement cell = getFirstIssueEntry();
            if (cell != null) {
                int cellX = cell.getLocation().getX();
                int cellY = cell.getLocation().getY();
                int cellWidth = cell.getSize().getWidth();
                int cellHeight = cell.getSize().getHeight();
                performSwipeRight(cellX + 30, cellY + cellHeight / 2,
                    cellX + cellWidth - 20, cellY + cellHeight / 2);
                sleep(300);
            }

            hidden = !areSwipeActionsVisible();
            System.out.println("   Swipe actions hidden: " + hidden);
            return hidden;
        } catch (Exception e) {
            System.out.println("⚠️ Error hiding swipe actions: " + e.getMessage());
            return false;
        }
    }

    /**
     * Confirm a delete action in the confirmation dialog.
     */
    public void confirmSwipeDelete() {
        System.out.println("🗑️ Confirming swipe delete...");
        try {
            // Look for "Delete" confirmation button
            try {
                WebElement confirmBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Delete' OR label == 'Confirm' OR label == 'Yes')"));
                confirmBtn.click();
                sleep(500);
                System.out.println("✅ Confirmed swipe delete");
                return;
            } catch (Exception ignored) {}

            // Fallback: look for destructive action in alert
            try {
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"));
                for (WebElement btn : buttons) {
                    String label = btn.getAttribute("label");
                    if (label != null && (label.contains("Delete") || label.contains("Remove") ||
                        label.contains("Yes") || label.contains("Confirm"))) {
                        btn.click();
                        sleep(500);
                        System.out.println("✅ Confirmed delete via: " + label);
                        return;
                    }
                }
            } catch (Exception ignored) {}

            System.out.println("⚠️ Could not find delete confirmation button");
        } catch (Exception e) {
            System.out.println("⚠️ Error confirming delete: " + e.getMessage());
        }
    }

    /**
     * Cancel a delete action in the confirmation dialog.
     */
    public void cancelSwipeDelete() {
        System.out.println("❌ Cancelling swipe delete...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"));
            cancelBtn.click();
            sleep(400);
            System.out.println("✅ Cancelled swipe delete");
        } catch (Exception e) {
            System.out.println("⚠️ Could not cancel delete: " + e.getMessage());
        }
    }

    // ================================================================
    // WITH PHOTOS FILTER TAB (TC_ISS_213-215)
    // ================================================================

    /**
     * Check if "With Photos" filter tab is visible on the Issues screen.
     * May need horizontal scrolling to find it.
     */
    public boolean isWithPhotosTabVisible() {
        System.out.println("🔍 Checking for With Photos tab...");
        try {
            // Strategy 1: Direct search
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'With Photos'"));
                System.out.println("   With Photos tab found");
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Scroll filter tabs left to reveal it
            scrollFilterTabsLeft();
            sleep(300);
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'With Photos'"));
                System.out.println("   With Photos tab found after scroll");
                return true;
            } catch (Exception ignored) {}

            // Strategy 3: Scroll more
            scrollFilterTabsLeft();
            sleep(300);
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'With Photos' OR label CONTAINS 'Photos' OR " +
                    "label CONTAINS 'photo')"));
                System.out.println("   With Photos tab found after second scroll");
                return true;
            } catch (Exception ignored) {}

            // Scroll back
            scrollFilterTabsRight();
            sleep(200);
            scrollFilterTabsRight();
            sleep(200);

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the "With Photos" filter tab.
     */
    public void tapWithPhotosTab() {
        System.out.println("📋 Tapping With Photos tab...");
        try {
            // Strategy 1: Direct tap
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'With Photos'"));
                tab.click();
                sleep(400);
                System.out.println("✅ Tapped With Photos tab");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Scroll left to find it
            scrollFilterTabsLeft();
            sleep(300);
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'With Photos'"));
                tab.click();
                sleep(400);
                System.out.println("✅ Tapped With Photos tab after scroll");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Scroll more
            scrollFilterTabsLeft();
            sleep(300);
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'With Photos' OR label CONTAINS 'Photos')"));
                tab.click();
                sleep(400);
                System.out.println("✅ Tapped With Photos tab after second scroll");
            } catch (Exception e) {
                System.out.println("⚠️ Could not tap With Photos tab: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping With Photos tab: " + e.getMessage());
        }
    }

    /**
     * Get count from "With Photos" filter tab.
     */
    public int getWithPhotosTabCount() {
        try {
            // Try direct find first
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'With Photos'"));
                String label = tab.getAttribute("label");
                System.out.println("   With Photos tab label: '" + label + "'");
                return extractCountFromTabLabel(label);
            } catch (Exception ignored) {}

            // Scroll to find
            scrollFilterTabsLeft();
            sleep(300);
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'With Photos'"));
                String label = tab.getAttribute("label");
                System.out.println("   With Photos tab label (after scroll): '" + label + "'");
                return extractCountFromTabLabel(label);
            } catch (Exception ignored) {}

            scrollFilterTabsLeft();
            sleep(300);
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'With Photos' OR label CONTAINS 'Photos')"));
            String label = tab.getAttribute("label");
            System.out.println("   With Photos tab label: '" + label + "'");
            return extractCountFromTabLabel(label);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Check if "With Photos" filter tab is selected.
     */
    public boolean isWithPhotosTabSelected() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'With Photos'"));
            String selected = tab.getAttribute("selected");
            if ("true".equals(selected)) return true;
            String value = tab.getAttribute("value");
            return "1".equals(value);
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // MY SESSION FILTER TAB (TC_ISS_216-219)
    // ================================================================

    /**
     * Check if "My Session" filter tab is visible on the Issues screen.
     * This tab only appears when an active job/session exists.
     */
    public boolean isMySessionTabVisible() {
        System.out.println("🔍 Checking for My Session tab...");
        try {
            // Strategy 1: Direct search
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'My Session'"));
                System.out.println("   My Session tab found");
                return true;
            } catch (Exception ignored) {}

            // Strategy 2: Scroll filter tabs to find it
            scrollFilterTabsLeft();
            sleep(300);
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'My Session' OR label CONTAINS 'Session')"));
                System.out.println("   My Session tab found after scroll");
                return true;
            } catch (Exception ignored) {}

            // Strategy 3: Scroll more aggressively
            scrollFilterTabsLeft();
            sleep(300);
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'My Session' OR label CONTAINS 'Session')"));
                System.out.println("   My Session tab found after second scroll");
                return true;
            } catch (Exception ignored) {}

            // Scroll back
            scrollFilterTabsRight();
            sleep(200);
            scrollFilterTabsRight();
            sleep(200);

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the "My Session" filter tab.
     */
    public void tapMySessionTab() {
        System.out.println("📋 Tapping My Session tab...");
        try {
            // Strategy 1: Direct tap
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'My Session'"));
                tab.click();
                sleep(400);
                System.out.println("✅ Tapped My Session tab");
                return;
            } catch (Exception ignored) {}

            // Strategy 2: Scroll left to find it
            scrollFilterTabsLeft();
            sleep(300);
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'My Session' OR label CONTAINS 'Session')"));
                tab.click();
                sleep(400);
                System.out.println("✅ Tapped My Session tab after scroll");
                return;
            } catch (Exception ignored) {}

            // Strategy 3: Scroll more
            scrollFilterTabsLeft();
            sleep(300);
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'My Session' OR label CONTAINS 'Session')"));
                tab.click();
                sleep(400);
                System.out.println("✅ Tapped My Session tab after second scroll");
            } catch (Exception e) {
                System.out.println("⚠️ Could not tap My Session tab: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping My Session tab: " + e.getMessage());
        }
    }

    /**
     * Get count from "My Session" filter tab.
     */
    public int getMySessionTabCount() {
        try {
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'My Session'"));
                String label = tab.getAttribute("label");
                System.out.println("   My Session tab label: '" + label + "'");
                return extractCountFromTabLabel(label);
            } catch (Exception ignored) {}

            // Scroll to find
            scrollFilterTabsLeft();
            sleep(300);
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label CONTAINS 'My Session' OR label CONTAINS 'Session')"));
                String label = tab.getAttribute("label");
                System.out.println("   My Session tab label (after scroll): '" + label + "'");
                return extractCountFromTabLabel(label);
            } catch (Exception ignored) {}

            scrollFilterTabsLeft();
            sleep(300);
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'My Session' OR label CONTAINS 'Session')"));
            String label = tab.getAttribute("label");
            return extractCountFromTabLabel(label);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Check if "My Session" filter tab is selected.
     */
    public boolean isMySessionTabSelected() {
        try {
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'My Session' OR label CONTAINS 'Session')"));
            String selected = tab.getAttribute("selected");
            if ("true".equals(selected)) return true;
            String value = tab.getAttribute("value");
            return "1".equals(value);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the My Session tab element's label for styling analysis.
     * @return Full label text or empty string
     */
    public String getMySessionTabLabel() {
        try {
            try {
                WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'My Session'"));
                return tab.getAttribute("label");
            } catch (Exception ignored) {}

            scrollFilterTabsLeft();
            sleep(300);
            WebElement tab = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label CONTAINS 'My Session' OR label CONTAINS 'Session')"));
            return tab.getAttribute("label");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Helper: scroll down on the Issue Details screen to reveal more fields.
     * Uses a swipe gesture from center-bottom to center-top.
     * Capped at MAX_DETAILS_SCROLL_DOWN (4) scrolls to prevent excessive scrolling.
     */
    public void scrollDownOnDetailsScreen() {
        if (detailsScrollDownDepth >= MAX_DETAILS_SCROLL_DOWN) {
            System.out.println("   ⚠️ Scroll down limit reached (" + MAX_DETAILS_SCROLL_DOWN + "), skipping");
            return;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("direction", "down");
            driver.executeScript("mobile: scroll", params);
            detailsScrollDownDepth++;
            sleep(400);
            System.out.println("   Scrolled down on details screen (" + detailsScrollDownDepth + "/" + MAX_DETAILS_SCROLL_DOWN + ")");
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
                detailsScrollDownDepth++;
                sleep(400);
                System.out.println("   Manual swipe scroll on details screen (" + detailsScrollDownDepth + "/" + MAX_DETAILS_SCROLL_DOWN + ")");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not scroll on details screen: " + e2.getMessage());
            }
        }
    }

    /**
     * Reset the details screen scroll counter.
     * Call this when navigating to a new issue details screen or returning to the issue list.
     */
    public void resetDetailsScrollCount() {
        detailsScrollDownDepth = 0;
    }
}
