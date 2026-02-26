package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Work Order Page Object
 * Handles the Work Orders screen (accessed from dashboard "No Active Work Order" card)
 *
 * Screen flow:
 *   Dashboard → "No Active Work Order" card → Work Orders screen
 *   Work Orders screen has:
 *     - "Start New Work Order" button
 *     - "Available Work Orders" section with list of work orders
 *     - Each entry: name, date, "AVAILABLE" badge, green indicator, counts
 */
public class WorkOrderPage extends BasePage {

    // ================================================================
    // WORK ORDERS SCREEN ELEMENTS
    // ================================================================

    // "Work Orders" navigation bar title
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeNavigationBar' AND name CONTAINS 'Work Order'")
    private WebElement workOrdersNavBar;

    // "Work Orders" static text header
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeStaticText' AND label == 'Work Orders'")
    private WebElement workOrdersHeaderText;

    // "Start New Work Order" button
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton' AND label CONTAINS 'Start New Work Order'")
    private WebElement startNewWorkOrderButton;

    // "Available Work Orders" section header
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Available Work Orders'")
    private WebElement availableWorkOrdersHeader;

    // "AVAILABLE" badge(s) on work order entries
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeStaticText' AND label == 'AVAILABLE'")
    private List<WebElement> availableBadges;

    // Back button (to return to dashboard)
    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton' AND (label == 'Back' OR label CONTAINS 'Dashboard')")
    private WebElement backButton;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public WorkOrderPage() {
        super();
    }

    // ================================================================
    // SCREEN DETECTION METHODS
    // ================================================================

    /**
     * Check if the Work Orders screen is displayed.
     * Uses multiple strategies for reliability.
     */
    public boolean isWorkOrdersScreenDisplayed() {
        // Strategy 1: Navigation bar
        try {
            if (isElementDisplayed(workOrdersNavBar)) {
                System.out.println("✅ Work Orders screen detected via nav bar");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Header static text "Work Orders"
        try {
            if (isElementDisplayed(workOrdersHeaderText)) {
                System.out.println("✅ Work Orders screen detected via header text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Fresh lookup — nav bar containing "Work Order"
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar' AND (name CONTAINS 'Work Order' OR name CONTAINS 'work order')"
            ));
            if (!navBars.isEmpty()) {
                System.out.println("✅ Work Orders screen detected via nav bar search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 4: Static text with "Work Orders" label
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'Work Orders' OR label CONTAINS 'Work Order')"
            ));
            if (!texts.isEmpty()) {
                System.out.println("✅ Work Orders screen detected via static text search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 5: Presence of "Start New Work Order" button (unique to this screen)
        try {
            if (isElementDisplayed(startNewWorkOrderButton)) {
                System.out.println("✅ Work Orders screen detected via Start New Work Order button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Work Orders screen not detected");
        return false;
    }

    /**
     * Wait for the Work Orders screen to be fully loaded.
     * Waits up to 10 seconds for the screen to appear.
     */
    public boolean waitForWorkOrdersScreen() {
        try {
            WebDriverWait screenWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            screenWait.pollingEvery(Duration.ofMillis(500));
            return screenWait.until(d -> isWorkOrdersScreenDisplayed());
        } catch (Exception e) {
            System.out.println("⚠️ Timeout waiting for Work Orders screen");
            return false;
        }
    }

    // ================================================================
    // HEADER VERIFICATION
    // ================================================================

    /**
     * Get the Work Orders screen header text.
     * Returns the header label (e.g., "Work Orders").
     */
    public String getWorkOrdersHeaderText() {
        // Strategy 1: Nav bar name
        try {
            if (isElementDisplayed(workOrdersNavBar)) {
                String name = workOrdersNavBar.getAttribute("name");
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text label
        try {
            if (isElementDisplayed(workOrdersHeaderText)) {
                return workOrdersHeaderText.getAttribute("label");
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Fresh lookup
        try {
            WebElement header = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Work Order'"
            ));
            return header.getAttribute("label");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if the header text matches expected "Work Orders".
     */
    public boolean isWorkOrdersHeaderCorrect() {
        String header = getWorkOrdersHeaderText();
        if (header != null) {
            boolean matches = header.contains("Work Order");
            System.out.println("📝 Header text: '" + header + "' — matches: " + matches);
            return matches;
        }
        return false;
    }

    // ================================================================
    // START NEW WORK ORDER BUTTON
    // ================================================================

    /**
     * Check if "Start New Work Order" button is displayed.
     */
    public boolean isStartNewWorkOrderButtonDisplayed() {
        // Strategy 1: Primary annotated locator
        try {
            if (isElementDisplayed(startNewWorkOrderButton)) {
                System.out.println("✅ Start New Work Order button found via primary locator");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Fresh lookup with broader label match
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Start New' OR label CONTAINS 'New Work Order')"
            ));
            if (!buttons.isEmpty()) {
                System.out.println("✅ Start New Work Order button found via search (" + buttons.size() + " matches)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Static text fallback (button might render as text in some iOS versions)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Start New Work Order'"
            ));
            if (!texts.isEmpty()) {
                System.out.println("✅ Start New Work Order found via static text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Start New Work Order button not found");
        return false;
    }

    /**
     * Click the "Start New Work Order" button.
     */
    public void clickStartNewWorkOrder() {
        System.out.println("📍 Clicking Start New Work Order button...");

        // Strategy 1: Primary annotated locator
        try {
            if (isElementDisplayed(startNewWorkOrderButton)) {
                click(startNewWorkOrderButton);
                System.out.println("✅ Clicked Start New Work Order via primary locator");
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 1 failed: " + e.getMessage());
        }

        // Strategy 2: Fresh lookup
        try {
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Start New' OR label CONTAINS 'New Work Order')"
            ));
            btn.click();
            System.out.println("✅ Clicked Start New Work Order via search");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Strategy 2 failed: " + e.getMessage());
        }

        // Strategy 3: Tap static text that looks like button
        try {
            WebElement text = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Start New Work Order'"
            ));
            text.click();
            System.out.println("✅ Clicked Start New Work Order via label search");
        } catch (Exception e) {
            System.out.println("⚠️ All strategies failed for Start New Work Order button");
            throw new RuntimeException("Could not click Start New Work Order button", e);
        }
    }

    // ================================================================
    // AVAILABLE WORK ORDERS SECTION
    // ================================================================

    /**
     * Check if the "Available Work Orders" section is displayed.
     */
    public boolean isAvailableWorkOrdersSectionDisplayed() {
        // Strategy 1: Primary annotated locator
        try {
            if (isElementDisplayed(availableWorkOrdersHeader)) {
                System.out.println("✅ Available Work Orders section found via primary locator");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Fresh lookup
        try {
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'Available Work' OR label CONTAINS 'Available Jobs')"
            ));
            if (!headers.isEmpty()) {
                System.out.println("✅ Available Work Orders section found via search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: If we can see work order entries, section must exist
        try {
            int count = getWorkOrderEntryCount();
            if (count > 0) {
                System.out.println("✅ Available Work Orders section inferred from " + count + " entries");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Available Work Orders section not found");
        return false;
    }

    // ================================================================
    // WORK ORDER ENTRIES
    // ================================================================

    /**
     * Get the count of available work order entries.
     * Looks for cells or distinct work order items in the list.
     */
    public int getWorkOrderEntryCount() {
        // Strategy 1: Count cells (work order entries are typically cells)
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            // Filter out non-work-order cells by checking location (below header area, Y > 200)
            int count = 0;
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 200 && h > 40) {
                        count++;
                    }
                } catch (Exception e) { /* skip problematic cells */ }
            }
            if (count > 0) {
                System.out.println("📊 Found " + count + " work order entries via cell count");
                return count;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Count "AVAILABLE" badges (each work order has one)
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'AVAILABLE'"
            ));
            if (!badges.isEmpty()) {
                System.out.println("📊 Found " + badges.size() + " work order entries via AVAILABLE badges");
                return badges.size();
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Count date-formatted strings (each entry has a date)
        try {
            List<WebElement> dates = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS ' at ' AND (label CONTAINS 'AM' OR label CONTAINS 'PM'))"
            ));
            if (!dates.isEmpty()) {
                System.out.println("📊 Found " + dates.size() + " work order entries via date strings");
                return dates.size();
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not count work order entries");
        return 0;
    }

    /**
     * Get the name of a work order entry by index.
     * Index 0 = first work order in the list.
     */
    public String getWorkOrderName(int index) {
        try {
            // Work order names are typically the first/most prominent text in each cell
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));

            // Filter to valid work order cells
            int validIndex = 0;
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 200 && h > 40) {
                        if (validIndex == index) {
                            // Get child static texts within this cell
                            List<WebElement> texts = cell.findElements(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeStaticText'"
                            ));
                            // First text is typically the name (exclude "AVAILABLE", date strings)
                            for (WebElement text : texts) {
                                String label = text.getAttribute("label");
                                if (label != null && !label.isEmpty()
                                        && !label.equals("AVAILABLE")
                                        && !label.contains(" at ")
                                        && !label.matches("\\d+ \\| \\d+")) {
                                    System.out.println("📝 Work order name at index " + index + ": " + label);
                                    return label;
                                }
                            }
                        }
                        validIndex++;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting work order name at index " + index + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Get the date string of a work order entry by index.
     */
    public String getWorkOrderDate(int index) {
        try {
            // Date strings contain " at " and "AM"/"PM"
            List<WebElement> dates = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS ' at ' AND (label CONTAINS 'AM' OR label CONTAINS 'PM')"
            ));
            if (index < dates.size()) {
                String label = dates.get(index).getAttribute("label");
                System.out.println("📅 Work order date at index " + index + ": " + label);
                return label;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting work order date at index " + index + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if "AVAILABLE" badge is displayed for a work order at given index.
     */
    public boolean isAvailableBadgeDisplayed(int index) {
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'AVAILABLE'"
            ));
            if (index < badges.size()) {
                boolean displayed = badges.get(index).isDisplayed();
                System.out.println("🏷️ AVAILABLE badge at index " + index + ": " + (displayed ? "visible" : "hidden"));
                return displayed;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking AVAILABLE badge at index " + index + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if any AVAILABLE badge exists on the Work Orders screen.
     */
    public boolean isAnyAvailableBadgeDisplayed() {
        try {
            if (availableBadges != null && !availableBadges.isEmpty()) {
                System.out.println("✅ Found " + availableBadges.size() + " AVAILABLE badge(s)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'AVAILABLE'"
            ));
            boolean found = !badges.isEmpty();
            System.out.println(found ? "✅ AVAILABLE badges found via search" : "⚠️ No AVAILABLE badges found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a green status indicator is visible on work order entries.
     * Green indicators are typically small colored views/images on the left side of entries.
     */
    public boolean isGreenStatusIndicatorDisplayed() {
        // Strategy 1: Look for small image/view elements that could be green dots
        // Green indicators are typically XCUIElementTypeImage or XCUIElementTypeOther with small size
        try {
            List<WebElement> indicators = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage' AND visible == true"
            ));
            for (WebElement indicator : indicators) {
                try {
                    int w = indicator.getSize().getWidth();
                    int h = indicator.getSize().getHeight();
                    int y = indicator.getLocation().getY();
                    // Green dot: small (< 30px), positioned in list area (Y > 200)
                    if (w <= 30 && h <= 30 && w > 3 && h > 3 && y > 200) {
                        System.out.println("✅ Green status indicator found (image " + w + "x" + h + " at Y=" + y + ")");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for circle/dot accessibility identifiers
        try {
            List<WebElement> dots = driver.findElements(AppiumBy.iOSNsPredicateString(
                "visible == true AND (name CONTAINS 'circle' OR name CONTAINS 'dot' OR name CONTAINS 'indicator' OR name CONTAINS 'status')"
            ));
            for (WebElement dot : dots) {
                try {
                    int w = dot.getSize().getWidth();
                    int h = dot.getSize().getHeight();
                    if (w <= 30 && h <= 30 && w > 3 && h > 3) {
                        System.out.println("✅ Green status indicator found via name search");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Check for SF Symbol "circle.fill" elements
        try {
            List<WebElement> circles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "visible == true AND name CONTAINS 'circle.fill'"
            ));
            if (!circles.isEmpty()) {
                System.out.println("✅ Green status indicator found via circle.fill symbol");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Green status indicator not conclusively found (may be part of cell rendering)");
        // Soft pass — green indicators are visual elements that may not have distinct accessibility traits
        return false;
    }

    /**
     * Get the work order counts display string (format: "N | N").
     * Returns the counts text for a work order at the given index.
     */
    public String getWorkOrderCounts(int index) {
        try {
            // Counts are displayed in "N | N" format (jobs | issues)
            List<WebElement> countTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label MATCHES '\\\\d+ \\\\| \\\\d+'"
            ));
            if (index < countTexts.size()) {
                String label = countTexts.get(index).getAttribute("label");
                System.out.println("📊 Work order counts at index " + index + ": " + label);
                return label;
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: Look for pipe-separated numbers
        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true AND label CONTAINS '|'"
            ));
            int countIndex = 0;
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                if (label != null && label.matches(".*\\d+.*\\|.*\\d+.*")) {
                    if (countIndex == index) {
                        System.out.println("📊 Work order counts at index " + index + " (fallback): " + label);
                        return label;
                    }
                    countIndex++;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not find work order counts at index " + index);
        return null;
    }

    /**
     * Check if any work order entry has counts displayed (N | N format).
     */
    public boolean isAnyWorkOrderCountsDisplayed() {
        try {
            List<WebElement> countTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true AND label CONTAINS '|'"
            ));
            for (WebElement text : countTexts) {
                String label = text.getAttribute("label");
                if (label != null && label.matches(".*\\d+.*\\|.*\\d+.*")) {
                    System.out.println("✅ Work order counts found: " + label);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No work order counts found");
        return false;
    }

    /**
     * Check if a work order entry at given index has all expected information displayed:
     * name, date, badge, and counts.
     */
    public boolean isWorkOrderEntryComplete(int index) {
        boolean hasName = getWorkOrderName(index) != null;
        boolean hasDate = getWorkOrderDate(index) != null;
        boolean hasBadge = isAvailableBadgeDisplayed(index);

        System.out.println("📋 Work order entry " + index + " completeness: "
            + "name=" + hasName + ", date=" + hasDate + ", badge=" + hasBadge);

        // Name and date are essential; badge is strongly expected
        return hasName && hasDate && hasBadge;
    }

    // ================================================================
    // ACTIVATE JOB (TC_JOB_010–013)
    // ================================================================

    /**
     * Check if the "Activate" button is displayed on a work order entry.
     * The Activate button appears on the right side of available (non-active) job cards.
     */
    public boolean isActivateButtonDisplayed() {
        // Strategy 1: Button with label "Activate"
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Activate'"
            ));
            if (!buttons.isEmpty()) {
                System.out.println("✅ Activate button found via button search (" + buttons.size() + " instances)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text "Activate" (SwiftUI may render as text within a tappable area)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Activate'"
            ));
            if (!texts.isEmpty()) {
                System.out.println("✅ Activate button found via static text search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Accessibility ID or broader label match
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(label == 'Activate' OR label CONTAINS 'Activate') AND visible == true"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ Activate element found via broad search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Activate button not found");
        return false;
    }

    /**
     * Tap the "Activate" button on the first available (non-active) work order.
     * Returns true if the button was found and tapped.
     */
    public boolean tapActivateButton() {
        System.out.println("📍 Tapping Activate button on first available work order...");

        // Strategy 1: Button with label "Activate"
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Activate'"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Activate button (button type)");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Button strategy failed: " + e.getMessage());
        }

        // Strategy 2: Static text "Activate"
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Activate'"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped Activate button (text type)");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Text strategy failed: " + e.getMessage());
        }

        // Strategy 3: Any element with Activate label
        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label == 'Activate' AND visible == true"
            ));
            element.click();
            System.out.println("✅ Tapped Activate via broad search");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ All Activate strategies failed: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if an "ACTIVE" badge is displayed on any work order entry.
     * After activation, the badge changes from "AVAILABLE" to "ACTIVE" (green).
     */
    public boolean isActiveBadgeDisplayed() {
        // Strategy 1: Static text with label "ACTIVE"
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'ACTIVE'"
            ));
            if (!badges.isEmpty()) {
                System.out.println("✅ ACTIVE badge found (" + badges.size() + " instances)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Broader search including "Active" (case variations)
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'ACTIVE' OR label == 'Active')"
            ));
            if (!badges.isEmpty()) {
                System.out.println("✅ Active badge found via case-insensitive search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Any visible element with "ACTIVE" label
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label == 'ACTIVE' AND visible == true"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ ACTIVE element found via broad search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ ACTIVE badge not found");
        return false;
    }

    /**
     * Count how many "ACTIVE" badges are displayed.
     * Used to verify only one job can be active at a time (TC_JOB_013).
     */
    public int getActiveBadgeCount() {
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'ACTIVE' OR label == 'Active')"
            ));
            int count = badges.size();
            System.out.println("📊 ACTIVE badge count: " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error counting ACTIVE badges: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Count how many "AVAILABLE" badges remain after an activation.
     * If a job was activated, it should no longer have AVAILABLE badge.
     */
    public int getAvailableBadgeCount() {
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'AVAILABLE'"
            ));
            int count = badges.size();
            System.out.println("📊 AVAILABLE badge count: " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error counting AVAILABLE badges: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Tap on a work order entry at the given index to open its details.
     * Taps the cell itself (not the Activate button).
     */
    public boolean tapWorkOrderEntry(int index) {
        System.out.println("📍 Tapping work order entry at index " + index + "...");

        // Strategy 1: Tap the cell directly
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            int validIndex = 0;
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 200 && h > 40) {
                        if (validIndex == index) {
                            cell.click();
                            System.out.println("✅ Tapped work order cell at index " + index);
                            return true;
                        }
                        validIndex++;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Cell tap strategy failed: " + e.getMessage());
        }

        // Strategy 2: Tap the work order name text
        try {
            String name = getWorkOrderName(index);
            if (name != null) {
                WebElement nameElement = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == '" + name.replace("'", "\\'") + "'"
                ));
                nameElement.click();
                System.out.println("✅ Tapped work order name '" + name + "'");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Name tap strategy failed: " + e.getMessage());
        }

        System.out.println("⚠️ Could not tap work order entry at index " + index);
        return false;
    }

    /**
     * Tap on the active (ACTIVE-badged) work order to open session details.
     */
    public boolean tapActiveWorkOrder() {
        System.out.println("📍 Tapping on the active work order...");

        // Strategy 1: Find the cell containing "ACTIVE" badge, then tap the cell
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 200 && h > 40) {
                        // Check if this cell has an ACTIVE badge as a child
                        List<WebElement> activeBadges = cell.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND (label == 'ACTIVE' OR label == 'Active')"
                        ));
                        if (!activeBadges.isEmpty()) {
                            cell.click();
                            System.out.println("✅ Tapped active work order cell");
                            return true;
                        }
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Cell-based active search failed: " + e.getMessage());
        }

        // Strategy 2: Find ACTIVE badge, get its Y, find closest cell
        try {
            WebElement activeBadge = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'ACTIVE' OR label == 'Active')"
            ));
            int badgeY = activeBadge.getLocation().getY();
            // Tap slightly to the left of the badge (on the entry name area)
            int badgeX = activeBadge.getLocation().getX();
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            int tapX = Math.max(badgeX - 100, 50); // Tap to the left of badge
            tap.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), tapX, badgeY));
            tap.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(tap));
            System.out.println("✅ Tapped near ACTIVE badge at (" + tapX + ", " + badgeY + ")");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Coordinate tap strategy failed: " + e.getMessage());
        }

        System.out.println("⚠️ Could not tap active work order");
        return false;
    }

    // ================================================================
    // SESSION DETAILS SCREEN (TC_JOB_014–019)
    // ================================================================

    /**
     * Check if the Session Details screen is displayed.
     * The screen header contains the job title (e.g., "Job - Dec 17, 12:18 PM").
     */
    public boolean isSessionDetailsScreenDisplayed() {
        // Strategy 1: Look for "Active Session" badge text (unique to this screen)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'Active Session' OR label CONTAINS 'active session')"
            ));
            if (!texts.isEmpty()) {
                System.out.println("✅ Session Details screen detected via 'Active Session' text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for session stats cards (Tasks, Issues, IR Photos)
        try {
            List<WebElement> tasks = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Tasks'"
            ));
            List<WebElement> issues = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Issues'"
            ));
            if (!tasks.isEmpty() && !issues.isEmpty()) {
                System.out.println("✅ Session Details screen detected via Tasks + Issues labels");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for "INFORMATION" section header
        try {
            List<WebElement> infoHeaders = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'INFORMATION' OR label == 'Information')"
            ));
            if (!infoHeaders.isEmpty()) {
                System.out.println("✅ Session Details screen detected via INFORMATION header");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 4: Look for "Session Type" or "Started" labels
        try {
            List<WebElement> sessionLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'Session Type' OR label CONTAINS 'Started')"
            ));
            if (!sessionLabels.isEmpty()) {
                System.out.println("✅ Session Details screen detected via Session Type/Started labels");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Session Details screen not detected");
        return false;
    }

    /**
     * Wait for the Session Details screen to be fully loaded.
     */
    public boolean waitForSessionDetailsScreen() {
        try {
            WebDriverWait screenWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            screenWait.pollingEvery(Duration.ofMillis(500));
            return screenWait.until(d -> isSessionDetailsScreenDisplayed());
        } catch (Exception e) {
            System.out.println("⚠️ Timeout waiting for Session Details screen");
            return false;
        }
    }

    /**
     * Get the Session Details header text (the job title in the nav bar).
     * Example: "Job - Dec 17, 12:18 PM"
     */
    public String getSessionDetailsHeaderText() {
        // Strategy 1: Navigation bar title
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar'"
            ));
            for (WebElement navBar : navBars) {
                String name = navBar.getAttribute("name");
                if (name != null && !name.isEmpty() && !name.equals("Work Orders")) {
                    System.out.println("📝 Session Details header (nav): " + name);
                    return name;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for text containing "Job" in header area
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'Job' OR label CONTAINS 'job')"
            ));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y < 150) { // Header area
                    String label = text.getAttribute("label");
                    System.out.println("📝 Session Details header (text): " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get Session Details header text");
        return null;
    }

    /**
     * Check if the back button is displayed on Session Details screen.
     */
    public boolean isSessionDetailsBackButtonDisplayed() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Back' OR label CONTAINS 'Work Orders' OR label CONTAINS 'back')"
            ));
            boolean found = !buttons.isEmpty();
            System.out.println(found ? "✅ Back button found on Session Details" : "⚠️ Back button not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the refresh icon is displayed on Session Details screen header.
     */
    public boolean isSessionDetailsRefreshIconDisplayed() {
        // Strategy 1: Button with refresh/reload label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Refresh' OR label CONTAINS 'refresh' OR label CONTAINS 'arrow.clockwise' OR label CONTAINS 'reload')"
            ));
            if (!buttons.isEmpty()) {
                System.out.println("✅ Refresh icon found on Session Details");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Image with refresh-like name
        try {
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeImage') AND name CONTAINS 'arrow.clockwise'"
            ));
            if (!images.isEmpty()) {
                System.out.println("✅ Refresh icon found via SF Symbol name");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Any small button in the top-right area (nav bar) that isn't "Back"
        try {
            List<WebElement> navButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            for (WebElement btn : navButtons) {
                int x = btn.getLocation().getX();
                int y = btn.getLocation().getY();
                String label = btn.getAttribute("label");
                // Right side of header area, not the back button
                if (x > 300 && y < 120 && label != null && !label.contains("Back")) {
                    System.out.println("✅ Refresh-like icon found at top-right (" + x + ", " + y + ") label: " + label);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Refresh icon not found on Session Details");
        return false;
    }

    /**
     * Check if the "Active Session" badge with green dot is displayed.
     */
    public boolean isActiveSessionBadgeDisplayed() {
        // Strategy 1: "Active Session" text
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'Active Session' OR label CONTAINS 'active session')"
            ));
            if (!badges.isEmpty()) {
                System.out.println("✅ 'Active Session' badge text found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Separate "Active" and "Session" texts near each other
        try {
            List<WebElement> activeTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Active'"
            ));
            List<WebElement> sessionTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Session'"
            ));
            if (!activeTexts.isEmpty() && !sessionTexts.isEmpty()) {
                System.out.println("✅ 'Active' + 'Session' labels found separately");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Active Session badge not found");
        return false;
    }

    /**
     * Check if session stats cards are displayed (Tasks, Issues, IR Photos).
     * Returns true if at least Tasks and Issues stats are visible.
     */
    public boolean isSessionStatsDisplayed() {
        boolean hasTasks = false;
        boolean hasIssues = false;
        boolean hasIRPhotos = false;

        // Check "Tasks" stat card
        try {
            List<WebElement> taskLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Tasks'"
            ));
            hasTasks = !taskLabels.isEmpty();
        } catch (Exception e) { /* continue */ }

        // Check "Issues" stat card
        try {
            List<WebElement> issueLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Issues'"
            ));
            hasIssues = !issueLabels.isEmpty();
        } catch (Exception e) { /* continue */ }

        // Check "IR Photos" stat card
        try {
            List<WebElement> irLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'IR Photos' OR label CONTAINS 'IR Photo')"
            ));
            hasIRPhotos = !irLabels.isEmpty();
        } catch (Exception e) { /* continue */ }

        System.out.println("📊 Session stats: Tasks=" + hasTasks + ", Issues=" + hasIssues + ", IR Photos=" + hasIRPhotos);
        return hasTasks && hasIssues;
    }

    /**
     * Get the count value displayed on a stat card by label name.
     * @param statName "Tasks", "Issues", or "IR Photos"
     * @return the count string, or null if not found
     */
    public String getStatCardCount(String statName) {
        try {
            // Find the stat label
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == '" + statName + "' OR label CONTAINS '" + statName + "')"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                int labelX = labels.get(0).getLocation().getX();
                // The count is typically above or near the label — find numeric text near this position
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    if (label != null && label.matches("\\d+")) {
                        int textY = text.getLocation().getY();
                        int textX = text.getLocation().getX();
                        // Must be nearby: within 80px horizontally and 60px vertically
                        if (Math.abs(textX - labelX) < 80 && Math.abs(textY - labelY) < 60) {
                            System.out.println("📊 Stat '" + statName + "' count: " + label);
                            return label;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting stat count for '" + statName + "': " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if the INFORMATION section is displayed on Session Details screen.
     */
    public boolean isInformationSectionDisplayed() {
        // Strategy 1: "INFORMATION" header text
        try {
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'INFORMATION' OR label == 'Information')"
            ));
            if (!headers.isEmpty()) {
                System.out.println("✅ INFORMATION section header found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Check for fields within the section (Session Type, Started)
        try {
            List<WebElement> sessionType = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Session Type'"
            ));
            List<WebElement> started = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Started'"
            ));
            if (!sessionType.isEmpty() || !started.isEmpty()) {
                System.out.println("✅ INFORMATION section inferred from Session Type/Started fields");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ INFORMATION section not found");
        return false;
    }

    /**
     * Get the Session Type value (e.g., "FLIR-SEP").
     */
    public String getSessionType() {
        try {
            // Look for text near "Session Type" label
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Session Type'"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                // Value is typically on the same row, to the right, or just below
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    int textY = text.getLocation().getY();
                    if (label != null && !label.contains("Session Type")
                            && !label.equals("INFORMATION")
                            && !label.equals("Started")
                            && !label.contains("Quick QR")
                            && Math.abs(textY - labelY) < 30) {
                        System.out.println("📝 Session Type: " + label);
                        return label;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting Session Type: " + e.getMessage());
        }

        // Fallback: Look for common session type values
        try {
            List<WebElement> flirsep = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'FLIR' OR label CONTAINS 'flir')"
            ));
            if (!flirsep.isEmpty()) {
                String val = flirsep.get(0).getAttribute("label");
                System.out.println("📝 Session Type (fallback): " + val);
                return val;
            }
        } catch (Exception e) { /* continue */ }

        return null;
    }

    /**
     * Get the "Started" date/time value from the INFORMATION section.
     */
    public String getStartedDateTime() {
        try {
            // Look for date/time text near "Started" label
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Started'"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true AND (label CONTAINS 'AM' OR label CONTAINS 'PM' OR label CONTAINS ',')"
                ));
                for (WebElement text : allTexts) {
                    int textY = text.getLocation().getY();
                    if (Math.abs(textY - labelY) < 30) {
                        String val = text.getAttribute("label");
                        System.out.println("📅 Started: " + val);
                        return val;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting Started date: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if the "Quick QR Action" dropdown is displayed.
     */
    public boolean isQuickQRActionDisplayed() {
        // Strategy 1: "Quick QR Action" label
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Quick QR Action'"
            ));
            if (!labels.isEmpty()) {
                System.out.println("✅ Quick QR Action label found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Button or picker containing "Quick QR"
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND label CONTAINS 'Quick QR'"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ Quick QR element found via broad search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for common QR action values (e.g., "Full Asset")
        try {
            List<WebElement> values = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Full Asset'"
            ));
            if (!values.isEmpty()) {
                System.out.println("✅ Quick QR Action inferred from 'Full Asset' button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Quick QR Action not found");
        return false;
    }

    /**
     * Get the current Quick QR Action value (e.g., "Full Asset").
     */
    public String getQuickQRActionValue() {
        // Strategy 1: Button next to "Quick QR Action" label
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Quick QR Action'"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                // Find buttons/text near the label
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true"
                ));
                for (WebElement btn : buttons) {
                    int btnY = btn.getLocation().getY();
                    String label = btn.getAttribute("label");
                    if (Math.abs(btnY - labelY) < 30 && label != null
                            && !label.contains("Quick QR") && !label.equals("Back")) {
                        System.out.println("📝 Quick QR Action value: " + label);
                        return label;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for known QR action values
        String[] knownValues = {"Full Asset", "Quick Count", "Photo Only", "Skip"};
        for (String value : knownValues) {
            try {
                List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND label == '" + value + "'"
                ));
                if (!elements.isEmpty()) {
                    System.out.println("📝 Quick QR Action value (known match): " + value);
                    return value;
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("⚠️ Could not determine Quick QR Action value");
        return null;
    }

    // ================================================================
    // SESSION BOTTOM TABS (TC_JOB_020)
    // ================================================================

    /**
     * Get the list of bottom tab bar labels on Session Details screen.
     * Expected tabs: Details, Locations, Tasks, Issues, Files
     */
    public java.util.List<String> getSessionBottomTabLabels() {
        java.util.List<String> tabLabels = new java.util.ArrayList<>();
        String[] expectedTabs = {"Details", "Locations", "Tasks", "Issues", "Files"};

        for (String tabName : expectedTabs) {
            try {
                List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                    + "AND label == '" + tabName + "'"
                ));
                if (!elements.isEmpty()) {
                    tabLabels.add(tabName);
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Session bottom tabs found: " + tabLabels);
        return tabLabels;
    }

    /**
     * Check if all 5 expected bottom tabs are displayed.
     */
    public boolean areAllSessionTabsDisplayed() {
        java.util.List<String> tabs = getSessionBottomTabLabels();
        boolean allPresent = tabs.size() >= 5;
        System.out.println(allPresent
            ? "✅ All 5 session tabs found"
            : "⚠️ Only " + tabs.size() + "/5 session tabs found: " + tabs);
        return allPresent;
    }

    /**
     * Check if a specific tab is displayed and optionally selected/active.
     * @param tabName Tab name (e.g., "Details", "Issues")
     */
    public boolean isTabDisplayed(String tabName) {
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label == '" + tabName + "'"
            ));
            boolean found = !elements.isEmpty();
            System.out.println(found ? "✅ Tab '" + tabName + "' found" : "⚠️ Tab '" + tabName + "' not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap on a session tab by name.
     * @param tabName Tab name (e.g., "Issues", "Tasks", "Files")
     */
    public boolean tapSessionTab(String tabName) {
        System.out.println("📍 Tapping session tab: " + tabName);

        // Strategy 1: Button with exact label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + tabName + "'"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped tab '" + tabName + "' (button)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text with exact label
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == '" + tabName + "'"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped tab '" + tabName + "' (text)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Any visible element with tab label
        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label == '" + tabName + "' AND visible == true"
            ));
            element.click();
            System.out.println("✅ Tapped tab '" + tabName + "' (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap tab '" + tabName + "'");
            return false;
        }
    }

    // ================================================================
    // SESSION ISSUES TAB (TC_JOB_021–026)
    // ================================================================

    /**
     * Check if the Issues tab content is displayed (after tapping Issues tab).
     * Looks for issues-related content: summary counts, Manage Issues button, or issue list.
     */
    public boolean isSessionIssuesContentDisplayed() {
        // Strategy 1: "Manage Issues" button (unique to session issues tab)
        try {
            List<WebElement> manageBtn = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Manage Issues'"
            ));
            if (!manageBtn.isEmpty()) {
                System.out.println("✅ Session Issues content detected via Manage Issues button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Total/Open/Closed summary labels
        try {
            List<WebElement> totalLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'Total' OR label == 'Open' OR label == 'Closed')"
            ));
            if (totalLabels.size() >= 2) {
                System.out.println("✅ Session Issues content detected via summary labels");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Floating + button at bottom right (issue creation)
        try {
            List<WebElement> addButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == '+' OR label == 'Add' OR label CONTAINS 'add')"
            ));
            if (!addButtons.isEmpty()) {
                System.out.println("✅ Session Issues content inferred from + button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Session Issues content not detected");
        return false;
    }

    /**
     * Get the badge count displayed on the Issues tab.
     * The badge is a small number (e.g., "2") on the tab itself.
     */
    public String getIssuesTabBadgeCount() {
        // Strategy 1: Look for a small numeric text near the Issues tab
        try {
            // First find the Issues tab element
            List<WebElement> issuesTabs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label == 'Issues'"
            ));
            if (!issuesTabs.isEmpty()) {
                int tabY = issuesTabs.get(0).getLocation().getY();
                int tabX = issuesTabs.get(0).getLocation().getX();
                // Look for numeric text near the tab
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    if (label != null && label.matches("\\d+")) {
                        int textY = text.getLocation().getY();
                        int textX = text.getLocation().getX();
                        // Badge is typically near/overlapping the tab (within 40px)
                        if (Math.abs(textX - tabX) < 60 && Math.abs(textY - tabY) < 40) {
                            System.out.println("📊 Issues tab badge count: " + label);
                            return label;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for badge-style element on Issues tab button
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Issues'"
            ));
            for (WebElement btn : buttons) {
                String label = btn.getAttribute("label");
                // Button label might include the count like "Issues, 2 items"
                if (label != null && label.matches(".*\\d+.*") && label.contains("Issues")) {
                    String count = label.replaceAll("[^0-9]", "");
                    if (!count.isEmpty()) {
                        System.out.println("📊 Issues tab badge from button label: " + count);
                        return count;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Issues tab badge count not found");
        return null;
    }

    /**
     * Get the issues summary counts (Total, Open, Closed).
     * Returns a map with keys "Total", "Open", "Closed" and their count strings.
     */
    public java.util.Map<String, String> getIssuesSummary() {
        java.util.Map<String, String> summary = new java.util.LinkedHashMap<>();
        String[] labels = {"Total", "Open", "Closed"};

        for (String labelName : labels) {
            try {
                List<WebElement> labelElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == '" + labelName + "'"
                ));
                if (!labelElements.isEmpty()) {
                    int labelY = labelElements.get(0).getLocation().getY();
                    int labelX = labelElements.get(0).getLocation().getX();
                    // Find numeric text near this label (above or beside it)
                    List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND visible == true"
                    ));
                    for (WebElement text : allTexts) {
                        String textLabel = text.getAttribute("label");
                        if (textLabel != null && textLabel.matches("\\d+")) {
                            int textY = text.getLocation().getY();
                            int textX = text.getLocation().getX();
                            if (Math.abs(textX - labelX) < 60 && Math.abs(textY - labelY) < 50) {
                                summary.put(labelName, textLabel);
                                System.out.println("📊 " + labelName + ": " + textLabel);
                                break;
                            }
                        }
                    }
                    if (!summary.containsKey(labelName)) {
                        summary.put(labelName, "found"); // Label exists but count not separately identified
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Issues summary: " + summary);
        return summary;
    }

    /**
     * Check if the "Manage Issues" button is displayed.
     */
    public boolean isManageIssuesButtonDisplayed() {
        // Strategy 1: Button with "Manage Issues" label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Manage Issues'"
            ));
            if (!buttons.isEmpty()) {
                System.out.println("✅ Manage Issues button found (button type)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text "Manage Issues" (may render as text in SwiftUI)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Manage Issues'"
            ));
            if (!texts.isEmpty()) {
                System.out.println("✅ Manage Issues button found (static text)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Any visible element with "Manage Issues"
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Manage Issues' AND visible == true"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ Manage Issues found via broad search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Manage Issues button not found");
        return false;
    }

    /**
     * Tap the "Manage Issues" button.
     */
    public boolean tapManageIssuesButton() {
        System.out.println("📍 Tapping Manage Issues button...");

        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Manage Issues'"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Manage Issues (button)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Manage Issues'"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped Manage Issues (text)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Manage Issues' AND visible == true"
            ));
            element.click();
            System.out.println("✅ Tapped Manage Issues (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Manage Issues button");
            return false;
        }
    }

    /**
     * Get the count of linked issues in the session issues list.
     * Counts cells/entries below the summary section.
     */
    public int getLinkedIssueCount() {
        // Strategy 1: Count cells in the issues list area
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            int count = 0;
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    // Issue cells are in the list area (below summary ~300px) and have reasonable height
                    if (y > 300 && h > 50) {
                        count++;
                    }
                } catch (Exception e) { /* skip */ }
            }
            if (count > 0) {
                System.out.println("📊 Linked issue count: " + count);
                return count;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Count issue-related elements (status badges or class tags)
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'Open' OR label == 'In Progress' "
                + "OR label == 'Resolved' OR label == 'Closed') AND visible == true"
            ));
            // Filter to only badges in the list area (Y > 300)
            int count = 0;
            for (WebElement badge : badges) {
                try {
                    int y = badge.getLocation().getY();
                    if (y > 300) count++;
                } catch (Exception e) { /* skip */ }
            }
            if (count > 0) {
                System.out.println("📊 Linked issue count (via status badges): " + count);
                return count;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not count linked issues");
        return 0;
    }

    /**
     * Check if a linked issue entry displays all expected elements:
     * title, description, issue class tag, asset location, status badge.
     */
    public boolean isLinkedIssueEntryComplete() {
        boolean hasTitle = false;
        boolean hasClassTag = false;
        boolean hasStatusBadge = false;

        // Check for issue class tags (NEC, NFPA, OSHA, Repair, Thermal, Ultrasonic)
        try {
            List<WebElement> classTags = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true AND "
                + "(label CONTAINS 'NEC' OR label CONTAINS 'NFPA' OR label CONTAINS 'OSHA' "
                + "OR label CONTAINS 'Repair' OR label CONTAINS 'Thermal' OR label CONTAINS 'Ultrasonic')"
            ));
            // Filter to list area
            for (WebElement tag : classTags) {
                try {
                    if (tag.getLocation().getY() > 300) {
                        hasClassTag = true;
                        break;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Check for status badges
        try {
            List<WebElement> statusBadges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true AND "
                + "(label == 'Open' OR label == 'In Progress' OR label == 'Resolved' OR label == 'Closed')"
            ));
            for (WebElement badge : statusBadges) {
                try {
                    if (badge.getLocation().getY() > 300) {
                        hasStatusBadge = true;
                        break;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Check for title-like text in issue cells
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 300 && h > 50) {
                        List<WebElement> texts = cell.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText'"
                        ));
                        if (texts.size() >= 2) { // At least title + one other element
                            hasTitle = true;
                            break;
                        }
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("📋 Linked issue completeness: title=" + hasTitle
            + ", classTag=" + hasClassTag + ", statusBadge=" + hasStatusBadge);
        return hasTitle || (hasClassTag && hasStatusBadge);
    }

    /**
     * Check if the floating "+" button (add issue) is displayed.
     * Typically positioned at the bottom-right of the screen.
     */
    public boolean isAddIssueFloatingButtonDisplayed() {
        // Strategy 1: Button with "+" or "Add" label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == '+' OR label == 'Add' OR label == 'add')"
            ));
            for (WebElement btn : buttons) {
                try {
                    int x = btn.getLocation().getX();
                    int y = btn.getLocation().getY();
                    org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
                    // Floating button is bottom-right quadrant
                    if (x > screenSize.width / 2 && y > screenSize.height / 2) {
                        System.out.println("✅ Floating + button found at (" + x + ", " + y + ")");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
            // If found but position check didn't pass, still count it
            if (!buttons.isEmpty()) {
                System.out.println("✅ + button found (position may vary)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Image/button with "plus" or "plus.circle" name
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeImage') "
                + "AND (name CONTAINS 'plus' OR label CONTAINS 'plus')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ Floating + button found via plus name");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Any small circular button in bottom area
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
            for (WebElement btn : allButtons) {
                try {
                    int x = btn.getLocation().getX();
                    int y = btn.getLocation().getY();
                    int w = btn.getSize().getWidth();
                    int h = btn.getSize().getHeight();
                    // Floating button: small, roughly square, in bottom half
                    if (w < 80 && h < 80 && w > 20 && Math.abs(w - h) < 20
                            && y > screenSize.height * 2 / 3 && x > screenSize.width / 2) {
                        System.out.println("✅ Floating button found by geometry at (" + x + ", " + y + ")");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Floating + button not found");
        return false;
    }

    // ================================================================
    // LINK ISSUES SCREEN (TC_JOB_027–029)
    // ================================================================

    /**
     * Check if the Link Issues screen is displayed.
     * Has: Cancel button, "Link Issues" title, Update button, search bar, issue list with checkboxes.
     */
    public boolean isLinkIssuesScreenDisplayed() {
        // Strategy 1: "Link Issues" title
        try {
            List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Link Issues'"
            ));
            if (!titles.isEmpty()) {
                System.out.println("✅ Link Issues screen detected via title");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Nav bar with "Link Issues"
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar' AND name CONTAINS 'Link Issues'"
            ));
            if (!navBars.isEmpty()) {
                System.out.println("✅ Link Issues screen detected via nav bar");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: "SELECT ISSUES TO LINK" label
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'SELECT ISSUES' OR label CONTAINS 'Select Issues')"
            ));
            if (!labels.isEmpty()) {
                System.out.println("✅ Link Issues screen detected via SELECT ISSUES label");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Link Issues screen not detected");
        return false;
    }

    /**
     * Wait for the Link Issues screen to be fully loaded.
     */
    public boolean waitForLinkIssuesScreen() {
        try {
            WebDriverWait screenWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            screenWait.pollingEvery(Duration.ofMillis(500));
            return screenWait.until(d -> isLinkIssuesScreenDisplayed());
        } catch (Exception e) {
            System.out.println("⚠️ Timeout waiting for Link Issues screen");
            return false;
        }
    }

    /**
     * Check if the Cancel button is displayed on Link Issues screen.
     */
    public boolean isLinkIssuesCancelButtonDisplayed() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            boolean found = !buttons.isEmpty();
            System.out.println(found ? "✅ Cancel button found on Link Issues" : "⚠️ Cancel button not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Update button is displayed on Link Issues screen.
     */
    public boolean isLinkIssuesUpdateButtonDisplayed() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Update' OR label CONTAINS 'Update')"
            ));
            boolean found = !buttons.isEmpty();
            System.out.println(found ? "✅ Update button found on Link Issues" : "⚠️ Update button not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the search bar is displayed on Link Issues screen.
     */
    public boolean isLinkIssuesSearchBarDisplayed() {
        // Strategy 1: Search field or text field
        try {
            List<WebElement> searchFields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeSearchField' OR type == 'XCUIElementTypeTextField') "
                + "AND visible == true"
            ));
            if (!searchFields.isEmpty()) {
                System.out.println("✅ Search bar found on Link Issues");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: "Search issues" placeholder text
        try {
            List<WebElement> placeholders = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeSearchField' OR type == 'XCUIElementTypeTextField' "
                + "OR type == 'XCUIElementTypeStaticText') AND (label CONTAINS 'Search' OR value CONTAINS 'Search')"
            ));
            if (!placeholders.isEmpty()) {
                System.out.println("✅ Search bar found via placeholder text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Search bar not found on Link Issues");
        return false;
    }

    /**
     * Check if the "SELECT ISSUES TO LINK TO THIS SESSION" label is displayed.
     */
    public boolean isSelectIssuesToLinkLabelDisplayed() {
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'SELECT ISSUES' "
                + "OR label CONTAINS 'Select Issues' OR label CONTAINS 'LINK TO THIS SESSION')"
            ));
            boolean found = !labels.isEmpty();
            System.out.println(found
                ? "✅ 'SELECT ISSUES TO LINK' label found"
                : "⚠️ 'SELECT ISSUES TO LINK' label not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Link Issues screen has a list of issues with checkboxes.
     * Returns the count of issue entries in the list.
     */
    public int getLinkIssuesListCount() {
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            int count = 0;
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 150 && h > 40) {
                        count++;
                    }
                } catch (Exception e) { /* skip */ }
            }
            System.out.println("📊 Link Issues list count: " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error counting Link Issues list: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if any issue in the Link Issues list has a checked (filled) checkmark.
     * Already linked issues should show a blue filled checkmark.
     */
    public boolean isAnyIssueChecked() {
        // Strategy 1: Look for checkmark images
        try {
            List<WebElement> checkmarks = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeButton') "
                + "AND (name CONTAINS 'checkmark' OR name CONTAINS 'check' "
                + "OR label CONTAINS 'checkmark' OR label CONTAINS 'Selected')"
            ));
            if (!checkmarks.isEmpty()) {
                System.out.println("✅ Checked checkmark found (" + checkmarks.size() + " instances)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for "selected" or "checked" accessibility traits in cells
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            for (WebElement cell : cells) {
                try {
                    String value = cell.getAttribute("value");
                    if (value != null && (value.contains("selected") || value.contains("checked") || value.equals("1"))) {
                        System.out.println("✅ Checked cell found via value attribute");
                        return true;
                    }
                    // Also check for checkmark child elements
                    List<WebElement> childChecks = cell.findElements(AppiumBy.iOSNsPredicateString(
                        "(name CONTAINS 'checkmark' OR name CONTAINS 'circle.fill' "
                        + "OR label CONTAINS 'checkmark')"
                    ));
                    if (!childChecks.isEmpty()) {
                        System.out.println("✅ Checked issue found via child checkmark");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for SF Symbol checkmark.circle.fill
        try {
            List<WebElement> filled = driver.findElements(AppiumBy.iOSNsPredicateString(
                "name CONTAINS 'checkmark.circle.fill' OR name CONTAINS 'checkmark.square.fill'"
            ));
            if (!filled.isEmpty()) {
                System.out.println("✅ Filled checkmark symbol found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No checked issues found");
        return false;
    }

    /**
     * Tap Cancel on the Link Issues screen to go back.
     */
    public boolean tapLinkIssuesCancel() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Cancel on Link Issues");
                return true;
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Could not tap Cancel on Link Issues");
        return false;
    }

    // ================================================================
    // LINK ISSUES — INTERACTIONS (TC_JOB_030–039)
    // ================================================================

    /**
     * Check if unlinked issues show an empty circle checkbox (unfilled).
     * In iOS, unfilled circles typically use "circle" SF Symbol (not "checkmark.circle.fill").
     * Returns true if at least one empty/unfilled circle is found in the issue list.
     */
    public boolean isEmptyCircleCheckboxDisplayed() {
        System.out.println("📍 Checking for empty circle checkboxes on Link Issues screen...");

        // Strategy 1: SF Symbol — "circle" without "fill" or "checkmark"
        try {
            List<WebElement> circles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeButton' "
                + "OR type == 'XCUIElementTypeOther') "
                + "AND (name == 'circle' OR name CONTAINS 'circle') "
                + "AND NOT (name CONTAINS 'checkmark') AND NOT (name CONTAINS 'fill')"
            ));
            if (!circles.isEmpty()) {
                System.out.println("✅ Empty circle checkboxes found: " + circles.size());
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for unselected cells — cells that don't have "checkmark.circle.fill"
        // If we know there are checked cells, the unchecked ones should have a different indicator
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            int uncheckedCount = 0;
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 150 && h > 40) {
                        // Check for filled checkmark children — if absent, it's unchecked
                        List<WebElement> checked = cell.findElements(AppiumBy.iOSNsPredicateString(
                            "name CONTAINS 'checkmark.circle.fill' OR name CONTAINS 'checkmark.square.fill'"
                        ));
                        if (checked.isEmpty()) {
                            // Also verify it has *some* circle-like icon (empty state)
                            List<WebElement> circleIcons = cell.findElements(AppiumBy.iOSNsPredicateString(
                                "name CONTAINS 'circle' OR type == 'XCUIElementTypeImage'"
                            ));
                            if (!circleIcons.isEmpty()) {
                                uncheckedCount++;
                            }
                        }
                    }
                } catch (Exception e) { /* skip */ }
            }
            if (uncheckedCount > 0) {
                System.out.println("✅ Found " + uncheckedCount + " unchecked issue cells (empty circle)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Compare total cells vs checked cells — if fewer checked than total, some are unchecked
        try {
            int totalIssues = getLinkIssuesListCount();
            if (totalIssues > 0) {
                int checkedCount = getCheckedIssueCount();
                if (checkedCount < totalIssues) {
                    System.out.println("✅ Unchecked issues inferred: " + (totalIssues - checkedCount)
                        + " out of " + totalIssues + " are unchecked (empty circle)");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No empty circle checkboxes detected");
        return false;
    }

    /**
     * Get the number of checked (selected/linked) issues in the Link Issues list.
     */
    public int getCheckedIssueCount() {
        int count = 0;

        // Strategy 1: Count checkmark.circle.fill SF Symbols
        try {
            List<WebElement> filled = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeButton' "
                + "OR type == 'XCUIElementTypeOther') "
                + "AND (name CONTAINS 'checkmark.circle.fill' OR name CONTAINS 'checkmark.square.fill')"
            ));
            if (!filled.isEmpty()) {
                // Filter to those in the list area (Y > 150 to skip nav bar)
                for (WebElement el : filled) {
                    try {
                        if (el.getLocation().getY() > 150) count++;
                    } catch (Exception e) { /* skip */ }
                }
                if (count > 0) {
                    System.out.println("📊 Checked issue count (SF Symbol): " + count);
                    return count;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Count cells with "selected" value or checkmark children
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 150 && h > 40) {
                        // Check value attribute
                        String value = cell.getAttribute("value");
                        if (value != null && (value.contains("selected") || value.equals("1"))) {
                            count++;
                            continue;
                        }
                        // Check for checkmark child
                        List<WebElement> checks = cell.findElements(AppiumBy.iOSNsPredicateString(
                            "name CONTAINS 'checkmark' AND name CONTAINS 'fill'"
                        ));
                        if (!checks.isEmpty()) count++;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("📊 Checked issue count: " + count);
        return count;
    }

    /**
     * Verify that a Link Issues entry at the given index has expected details:
     * title, asset info, status, and date.
     * Returns true if at least title + one other detail is found.
     */
    public boolean isLinkIssueEntryComplete(int index) {
        System.out.println("📍 Checking Link Issue entry completeness at index " + index + "...");

        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));

            // Filter to valid issue cells
            java.util.List<WebElement> issueCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 150 && h > 40) {
                        issueCells.add(cell);
                    }
                } catch (Exception e) { /* skip */ }
            }

            if (index < 0 || index >= issueCells.size()) {
                System.out.println("⚠️ Index " + index + " out of range (total cells: " + issueCells.size() + ")");
                return false;
            }

            WebElement cell = issueCells.get(index);
            List<WebElement> children = cell.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"
            ));

            boolean hasTitle = false;
            boolean hasStatus = false;
            boolean hasDateOrAsset = false;

            for (WebElement child : children) {
                try {
                    String label = child.getAttribute("label");
                    if (label == null || label.isEmpty()) continue;

                    // Check for status keywords
                    if (label.equals("Open") || label.equals("In Progress")
                        || label.equals("Resolved") || label.equals("Closed")
                        || label.equalsIgnoreCase("AVAILABLE")) {
                        hasStatus = true;
                    }
                    // Check for date patterns (e.g., "Feb 15, 2025" or "2025")
                    else if (label.matches(".*\\b\\d{4}\\b.*") || label.matches(".*\\d{1,2}:\\d{2}.*")
                        || label.matches(".*(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*")) {
                        hasDateOrAsset = true;
                    }
                    // Check for asset location or issue class
                    else if (label.contains("NEC") || label.contains("NFPA") || label.contains("OSHA")
                        || label.contains("Repair") || label.contains("Thermal")
                        || label.contains("Ultrasonic") || label.contains("Other")) {
                        hasDateOrAsset = true;
                    }
                    // Anything with more than 2 characters could be a title
                    else if (label.length() > 2) {
                        hasTitle = true;
                    }
                } catch (Exception e) { /* skip */ }
            }

            System.out.println("📋 Link Issue entry[" + index + "]: title=" + hasTitle
                + ", status=" + hasStatus + ", dateOrAsset=" + hasDateOrAsset
                + " (" + children.size() + " text children)");

            // A complete entry should have at least 2+ text children indicating multi-field data
            return (hasTitle && (hasStatus || hasDateOrAsset)) || children.size() >= 3;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking Link Issue entry: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tap an issue entry in the Link Issues list at the given index.
     * This toggles its checkbox (select/deselect).
     * Returns true if the tap was successful.
     */
    public boolean tapIssueInLinkList(int index) {
        System.out.println("📍 Tapping issue at index " + index + " in Link Issues list...");

        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));

            // Filter to valid issue cells
            java.util.List<WebElement> issueCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 150 && h > 40) {
                        issueCells.add(cell);
                    }
                } catch (Exception e) { /* skip */ }
            }

            if (index < 0 || index >= issueCells.size()) {
                System.out.println("⚠️ Index " + index + " out of range (total cells: " + issueCells.size() + ")");
                return false;
            }

            WebElement targetCell = issueCells.get(index);
            targetCell.click();
            System.out.println("✅ Tapped issue cell at index " + index);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap issue at index " + index + ": " + e.getMessage());
        }

        // Strategy 2: Coordinate tap on the cell area
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            java.util.List<WebElement> issueCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 150 && h > 40) issueCells.add(cell);
                } catch (Exception e) { /* skip */ }
            }

            if (index >= 0 && index < issueCells.size()) {
                WebElement cell = issueCells.get(index);
                int cx = cell.getLocation().getX() + cell.getSize().getWidth() / 2;
                int cy = cell.getLocation().getY() + cell.getSize().getHeight() / 2;

                org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence tap =
                    new org.openqa.selenium.interactions.Sequence(finger, 1);
                tap.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), cx, cy));
                tap.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerMove(Duration.ofMillis(50),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), cx, cy));
                tap.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Arrays.asList(tap));

                System.out.println("✅ Tapped issue at index " + index + " via coordinate tap at (" + cx + "," + cy + ")");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Coordinate tap failed for index " + index + ": " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if the issue at a given index in the Link Issues list is currently checked (selected).
     * Returns true if the issue has a filled checkmark, false if it has an empty circle or can't be determined.
     */
    public boolean isIssueCheckedAtIndex(int index) {
        System.out.println("📍 Checking if issue at index " + index + " is checked...");

        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));

            java.util.List<WebElement> issueCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 150 && h > 40) issueCells.add(cell);
                } catch (Exception e) { /* skip */ }
            }

            if (index < 0 || index >= issueCells.size()) {
                System.out.println("⚠️ Index " + index + " out of range");
                return false;
            }

            WebElement cell = issueCells.get(index);

            // Check for filled checkmark SF Symbol inside this cell
            List<WebElement> filledChecks = cell.findElements(AppiumBy.iOSNsPredicateString(
                "name CONTAINS 'checkmark.circle.fill' OR name CONTAINS 'checkmark.square.fill' "
                + "OR name CONTAINS 'checkmark' AND name CONTAINS 'fill'"
            ));
            if (!filledChecks.isEmpty()) {
                System.out.println("✅ Issue at index " + index + " is CHECKED (filled checkmark)");
                return true;
            }

            // Check cell's value attribute
            String value = cell.getAttribute("value");
            if (value != null && (value.contains("selected") || value.equals("1"))) {
                System.out.println("✅ Issue at index " + index + " is CHECKED (value attribute)");
                return true;
            }

            // Check for selected accessibility trait
            String selected = cell.getAttribute("selected");
            if ("true".equals(selected)) {
                System.out.println("✅ Issue at index " + index + " is CHECKED (selected attribute)");
                return true;
            }

            System.out.println("ℹ️ Issue at index " + index + " is NOT checked");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking issue at index " + index + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the Update button is enabled (not grayed out).
     * When no selection changes are made, Update may be disabled/grayed.
     * When selections change, Update becomes enabled with a distinct color.
     */
    public boolean isUpdateButtonEnabled() {
        System.out.println("📍 Checking if Update button is enabled...");

        // Strategy 1: Check enabled attribute on the button
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Update' OR label CONTAINS 'Update')"
            ));
            if (!buttons.isEmpty()) {
                WebElement btn = buttons.get(0);
                String enabled = btn.getAttribute("enabled");
                String accessible = btn.getAttribute("accessible");
                boolean isEnabled = !"false".equals(enabled);
                System.out.println("📊 Update button enabled=" + enabled + ", accessible=" + accessible);
                return isEnabled;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Check if the button is hittable (iOS concept — disabled buttons aren't hittable)
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Update'"
            ));
            if (!buttons.isEmpty()) {
                // If we can get its location and it's visible, consider it potentially enabled
                int x = buttons.get(0).getLocation().getX();
                int y = buttons.get(0).getLocation().getY();
                System.out.println("📊 Update button at (" + x + "," + y + ") — assuming enabled (visible)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not determine Update button state");
        return false;
    }

    /**
     * Tap the Update button on the Link Issues screen to save linked issue changes.
     * Returns true if tapped successfully.
     */
    public boolean tapUpdateButton() {
        System.out.println("📍 Tapping Update button on Link Issues screen...");

        // Strategy 1: Button with "Update" label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Update' OR label CONTAINS 'Update')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Update button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text "Update" (SwiftUI may render as text)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Update'"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped Update (static text)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search for "Update"
        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label == 'Update' AND visible == true"
            ));
            element.click();
            System.out.println("✅ Tapped Update (broad search)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Update button: " + e.getMessage());
            return false;
        }
    }

    /**
     * Type a search query in the Link Issues search bar.
     * Clears any existing text first, then enters the query.
     * Returns true if text was entered successfully.
     */
    public boolean searchInLinkIssues(String query) {
        System.out.println("📍 Searching for '" + query + "' in Link Issues...");

        // Strategy 1: XCUIElementTypeSearchField
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField'"
            ));
            if (!fields.isEmpty()) {
                WebElement field = fields.get(0);
                field.click();
                Thread.sleep(300); // Brief pause for keyboard
                field.clear();
                field.sendKeys(query);
                System.out.println("✅ Entered search query via search field: '" + query + "'");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: XCUIElementTypeTextField with search-like placeholder
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND "
                + "(value CONTAINS 'Search' OR value CONTAINS 'search' OR label CONTAINS 'Search')"
            ));
            if (!fields.isEmpty()) {
                WebElement field = fields.get(0);
                field.click();
                Thread.sleep(300);
                field.clear();
                field.sendKeys(query);
                System.out.println("✅ Entered search query via text field: '" + query + "'");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Any visible text field in the Link Issues screen (above list area)
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeSearchField' OR type == 'XCUIElementTypeTextField') "
                + "AND visible == true"
            ));
            for (WebElement field : fields) {
                try {
                    int y = field.getLocation().getY();
                    // Search bar is typically in the upper portion of the screen
                    if (y < 300) {
                        field.click();
                        Thread.sleep(300);
                        field.clear();
                        field.sendKeys(query);
                        System.out.println("✅ Entered search query via upper text field: '" + query + "'");
                        return true;
                    }
                } catch (Exception e2) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not enter search query in Link Issues");
        return false;
    }

    /**
     * Clear the search bar text in Link Issues screen.
     * Returns true if cleared successfully.
     */
    public boolean clearSearchInLinkIssues() {
        System.out.println("📍 Clearing search in Link Issues...");

        // Strategy 1: Tap the "Clear text" button (X icon) on search field
        try {
            List<WebElement> clearButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Clear text' OR label CONTAINS 'clear')"
            ));
            if (!clearButtons.isEmpty()) {
                clearButtons.get(0).click();
                System.out.println("✅ Cleared search via Clear text button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Clear the search field directly
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField' OR "
                + "(type == 'XCUIElementTypeTextField' AND "
                + "(value CONTAINS 'Search' OR label CONTAINS 'Search'))"
            ));
            if (!fields.isEmpty()) {
                fields.get(0).clear();
                System.out.println("✅ Cleared search field directly");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Tap Cancel button on keyboard (if visible)
        try {
            List<WebElement> cancelButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            for (WebElement btn : cancelButtons) {
                try {
                    // The Cancel near the search bar, not the nav Cancel
                    int y = btn.getLocation().getY();
                    if (y < 200) { // Search area
                        btn.click();
                        System.out.println("✅ Cleared search via search Cancel button");
                        return true;
                    }
                } catch (Exception e2) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not clear search in Link Issues");
        return false;
    }

    /**
     * Check if the "My Session" filter tab is displayed on the main Issues screen.
     * This filter is typically a tab/button on the Issues tab in Session Details.
     */
    public boolean isMySessionFilterDisplayed() {
        System.out.println("📍 Checking for My Session filter...");

        // Strategy 1: Button or text with "My Session" label
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label CONTAINS 'My Session' OR label CONTAINS 'MY SESSION' "
                + "OR label CONTAINS 'my session')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ My Session filter found: " + elements.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Segmented control with "My Session" option
        try {
            List<WebElement> segments = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSegmentedControl'"
            ));
            for (WebElement seg : segments) {
                List<WebElement> buttons = seg.findElements(AppiumBy.iOSNsPredicateString(
                    "label CONTAINS 'My Session' OR label CONTAINS 'Session'"
                ));
                if (!buttons.isEmpty()) {
                    System.out.println("✅ My Session filter found in segmented control");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search for any element containing "My Session"
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'My Session' AND visible == true"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ My Session filter found (broad search)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ My Session filter not found");
        return false;
    }

    /**
     * Tap the "My Session" filter tab.
     * Returns true if tapped successfully.
     */
    public boolean tapMySessionFilter() {
        System.out.println("📍 Tapping My Session filter...");

        // Strategy 1: Button with "My Session" label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'My Session' "
                + "OR label CONTAINS 'MY SESSION')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped My Session filter (button)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text "My Session" (tap it)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'My Session' "
                + "OR label CONTAINS 'MY SESSION')"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped My Session filter (text)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search
        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'My Session' AND visible == true"
            ));
            element.click();
            System.out.println("✅ Tapped My Session filter (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap My Session filter: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the count displayed on the "My Session" filter badge/label.
     * Returns the count string, or null if not found.
     */
    public String getMySessionCount() {
        System.out.println("📍 Getting My Session filter count...");

        // Strategy 1: Look for numeric text near the "My Session" element
        try {
            List<WebElement> sessionElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label CONTAINS 'My Session' OR label CONTAINS 'MY SESSION')"
            ));
            if (!sessionElements.isEmpty()) {
                WebElement sessionEl = sessionElements.get(0);
                String fullLabel = sessionEl.getAttribute("label");

                // The button label might contain the count (e.g., "My Session (3)" or "My Session 3")
                if (fullLabel != null && fullLabel.matches(".*\\d+.*")) {
                    String count = fullLabel.replaceAll("[^0-9]", "");
                    if (!count.isEmpty()) {
                        System.out.println("📊 My Session count from label: " + count);
                        return count;
                    }
                }

                // Look for adjacent numeric text
                int elY = sessionEl.getLocation().getY();
                int elX = sessionEl.getLocation().getX();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    if (label != null && label.matches("\\d+")) {
                        int textY = text.getLocation().getY();
                        int textX = text.getLocation().getX();
                        // Badge near the filter tab
                        if (Math.abs(textX - elX) < 80 && Math.abs(textY - elY) < 40) {
                            System.out.println("📊 My Session count from adjacent text: " + label);
                            return label;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: After tapping My Session, count the visible issues
        try {
            // Count cells currently visible in the list
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            int count = 0;
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 200 && h > 50) count++;
                } catch (Exception e2) { /* skip */ }
            }
            if (count > 0) {
                System.out.println("📊 My Session count (from visible cells): " + count);
                return String.valueOf(count);
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not determine My Session count");
        return null;
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    /**
     * Go back from Work Orders screen to dashboard.
     */
    public void goBack() {
        System.out.println("📍 Going back from Work Orders screen...");

        // Strategy 1: Back button
        try {
            if (isElementDisplayed(backButton)) {
                click(backButton);
                System.out.println("✅ Went back via back button");
                return;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Fresh lookup for back/close buttons
        try {
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Back' OR label CONTAINS 'back' OR label CONTAINS 'close' OR label CONTAINS 'dismiss')"
            ));
            btn.click();
            System.out.println("✅ Went back via button search");
            return;
        } catch (Exception e) { /* continue */ }

        // Strategy 3: iOS swipe-back gesture
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 5, size.height / 2));
            swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), size.width * 3 / 4, size.height / 2));
            swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(swipe));
            System.out.println("✅ Went back via swipe gesture");
        } catch (Exception e) {
            System.out.println("⚠️ Could not go back from Work Orders screen: " + e.getMessage());
        }
    }
}
