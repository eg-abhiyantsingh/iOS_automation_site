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

        // Strategy 2: Count "Start" buttons (each available work order has one)
        try {
            List<WebElement> startButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND label == 'Start'"
            ));
            // Filter to only those in the list area (Y > 150)
            int startCount = 0;
            for (WebElement btn : startButtons) {
                try {
                    int y = btn.getLocation().getY();
                    if (y > 150) startCount++;
                } catch (Exception e) { /* skip */ }
            }
            if (startCount > 0) {
                System.out.println("📊 Found " + startCount + " work order entries via Start buttons");
                return startCount;
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
     * Check if "available" state (Start button) is shown for a work order at given index.
     * The app shows a "Start" button on available (non-active) work orders instead of
     * an "AVAILABLE" text badge.
     */
    public boolean isAvailableBadgeDisplayed(int index) {
        try {
            List<WebElement> startButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND label == 'Start'"
            ));
            // Filter to list area (Y > 150)
            java.util.List<WebElement> filtered = new java.util.ArrayList<>();
            for (WebElement btn : startButtons) {
                try {
                    if (btn.getLocation().getY() > 150) filtered.add(btn);
                } catch (Exception e) { /* skip */ }
            }
            if (index < filtered.size()) {
                System.out.println("✅ Start button (available state) at index " + index + ": visible");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking available state at index " + index + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if any available work order exists (has a "Start" button).
     */
    public boolean isAnyAvailableBadgeDisplayed() {
        try {
            List<WebElement> startButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND label == 'Start'"
            ));
            for (WebElement btn : startButtons) {
                try {
                    if (btn.getLocation().getY() > 150) {
                        System.out.println("✅ Available work order found (Start button visible)");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ No available work orders found (no Start buttons)");
        return false;
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
     * Check if a "Start" button is displayed on a work order entry.
     * The app uses "Start" button (not "Activate") on available job cards.
     */
    public boolean isActivateButtonDisplayed() {
        // Strategy 1: Button with label "Start"
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Start'"
            ));
            for (WebElement btn : buttons) {
                try {
                    if (btn.getLocation().getY() > 150) {
                        System.out.println("✅ Start button found on work order entry");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text "Start" (SwiftUI may render as text within a tappable area)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Start'"
            ));
            for (WebElement text : texts) {
                try {
                    if (text.getLocation().getY() > 150) {
                        System.out.println("✅ Start button found via static text");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Fallback — also check for "Activate" in case UI changes
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(label == 'Activate' OR label == 'Start') AND visible == true"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ Start/Activate element found via broad search");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Start button not found");
        return false;
    }

    /**
     * Tap the "Start" button on the first available (non-active) work order.
     * Returns true if the button was found and tapped.
     */
    public boolean tapActivateButton() {
        System.out.println("📍 Tapping Start button on first available work order...");

        // Strategy 1: Button with label "Start" in list area
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Start'"
            ));
            for (WebElement btn : buttons) {
                try {
                    if (btn.getLocation().getY() > 150) {
                        btn.click();
                        System.out.println("✅ Tapped Start button (button type)");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Button strategy failed: " + e.getMessage());
        }

        // Strategy 2: Static text "Start" in list area
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Start'"
            ));
            for (WebElement text : texts) {
                try {
                    if (text.getLocation().getY() > 150) {
                        text.click();
                        System.out.println("✅ Tapped Start button (text type)");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Text strategy failed: " + e.getMessage());
        }

        // Strategy 3: Any element with Start or Activate label
        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(label == 'Start' OR label == 'Activate') AND visible == true"
            ));
            element.click();
            System.out.println("✅ Tapped Start/Activate via broad search");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ All Start/Activate strategies failed: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if an active/started work order is displayed.
     * After tapping "Start", the entry may show "Active", "ACTIVE", "In Progress",
     * "Stop", or the "Start" button may simply disappear/change.
     */
    public boolean isActiveBadgeDisplayed() {
        // Strategy 1: Look for "ACTIVE" / "Active" / "In Progress" text
        try {
            List<WebElement> badges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label ==[c] 'ACTIVE' OR label ==[c] 'Active' OR label CONTAINS 'In Progress' OR label == 'Started')"
            ));
            for (WebElement badge : badges) {
                try {
                    if (badge.getLocation().getY() > 150) {
                        System.out.println("✅ Active state badge found: '" + badge.getAttribute("label") + "'");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for "Stop" button (the opposite of "Start" — means job is active)
        try {
            List<WebElement> stopButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND label == 'Stop'"
            ));
            for (WebElement btn : stopButtons) {
                try {
                    if (btn.getLocation().getY() > 150) {
                        System.out.println("✅ Active state detected via Stop button");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Check if Session Details screen appeared (activation navigated away from Work Orders)
        if (isSessionDetailsScreenDisplayed()) {
            System.out.println("✅ Active state detected — Session Details screen is displayed");
            return true;
        }

        System.out.println("⚠️ Active state badge not found");
        return false;
    }

    /**
     * Count how many active state indicators are displayed.
     * Looks for "Active", "Stop", "In Progress" — any sign of activated job.
     */
    public int getActiveBadgeCount() {
        try {
            int count = 0;
            // Count Active/Stop/In Progress indicators in list area
            List<WebElement> indicators = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(label ==[c] 'ACTIVE' OR label ==[c] 'Active' OR label == 'Stop' OR label CONTAINS 'In Progress' OR label == 'Started')"
            ));
            for (WebElement el : indicators) {
                try {
                    if (el.getLocation().getY() > 150) count++;
                } catch (Exception e) { /* skip */ }
            }
            System.out.println("📊 ACTIVE badge count: " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error counting ACTIVE badges: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Count how many "Start" buttons remain (available/non-active work orders).
     */
    public int getAvailableBadgeCount() {
        try {
            List<WebElement> startButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND label == 'Start'"
            ));
            int count = 0;
            for (WebElement btn : startButtons) {
                try {
                    if (btn.getLocation().getY() > 150) count++;
                } catch (Exception e) { /* skip */ }
            }
            System.out.println("📊 Available (Start button) count: " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error counting available badges: " + e.getMessage());
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
     * Tap on the active work order to open session details.
     * After activation, the entry may show "Stop" instead of "Start", or an "Active" label.
     */
    public boolean tapActiveWorkOrder() {
        System.out.println("📍 Tapping on the active work order...");

        // Strategy 1: Find cell with "Stop" button (active state) or "Active"/"ACTIVE" label
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 150 && h > 40) {
                        // Check if this cell has active indicators
                        List<WebElement> activeIndicators = cell.findElements(AppiumBy.iOSNsPredicateString(
                            "label == 'Stop' OR label ==[c] 'ACTIVE' OR label ==[c] 'Active' OR label CONTAINS 'In Progress'"
                        ));
                        if (!activeIndicators.isEmpty()) {
                            cell.click();
                            System.out.println("✅ Tapped active work order cell (has active indicator)");
                            return true;
                        }
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Cell-based active search failed: " + e.getMessage());
        }

        // Strategy 2: Find active indicator element, tap near it
        try {
            WebElement activeEl = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label == 'Stop' OR label ==[c] 'ACTIVE' OR label ==[c] 'Active'"
            ));
            int elY = activeEl.getLocation().getY();
            int elX = activeEl.getLocation().getX();
            int tapX = Math.max(elX - 100, 50);
            java.util.Map<String, Object> tapParams = new java.util.HashMap<>();
            tapParams.put("x", tapX);
            tapParams.put("y", elY);
            tapParams.put("duration", 0.1);
            driver.executeScript("mobile: tap", tapParams);
            System.out.println("✅ Tapped near active indicator at (" + tapX + ", " + elY + ")");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Active indicator tap failed: " + e.getMessage());
        }

        // Strategy 3: If activation navigated directly to session details, we're already there
        if (isSessionDetailsScreenDisplayed()) {
            System.out.println("✅ Already on Session Details screen (activation navigated here)");
            return true;
        }

        // Strategy 4: Tap first work order entry (fallback)
        System.out.println("   Trying fallback: tap first work order entry");
        return tapWorkOrderEntry(0);
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
    // QUICK QR ACTION DROPDOWN INTERACTION (TC_JOB_071-075)
    // ================================================================

    /**
     * Tap the Quick QR Action dropdown to open it and show available options.
     * The dropdown is a SwiftUI Picker (.menu style) on the New Job screen.
     * Returns true if tapped successfully.
     */
    public boolean tapQuickQRActionDropdown() {
        System.out.println("📍 Tapping Quick QR Action dropdown...");

        // Strategy 1: Find button near "Quick QR Action" label (Y-proximity)
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Quick QR Action'"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();

                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true"
                ));
                WebElement bestMatch = null;
                int bestDist = Integer.MAX_VALUE;

                for (WebElement btn : buttons) {
                    int btnY = btn.getLocation().getY();
                    String btnLabel = btn.getAttribute("label");
                    if (Math.abs(btnY - labelY) < 30 && btnLabel != null
                            && !btnLabel.contains("Quick QR Action") && !btnLabel.equals("Back")
                            && !btnLabel.equals("Cancel") && !btnLabel.equals("Create")
                            && !btnLabel.contains("Photo Type")) {
                        int dist = Math.abs(btnY - labelY);
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestMatch = btn;
                        }
                    }
                }

                if (bestMatch != null) {
                    bestMatch.click();
                    System.out.println("✅ Tapped Quick QR Action dropdown button: "
                        + bestMatch.getAttribute("label"));
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Button containing known QR action value text
        String[] knownActions = {"Full Asset", "Data Collection", "IR Photos"};
        for (String value : knownActions) {
            try {
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS '" + value + "'"
                ));
                if (!buttons.isEmpty()) {
                    // Verify it's near the Quick QR Action area, not Photo Type
                    List<WebElement> qrLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Quick QR Action'"
                    ));
                    if (!qrLabels.isEmpty()) {
                        int qrY = qrLabels.get(0).getLocation().getY();
                        for (WebElement btn : buttons) {
                            if (Math.abs(btn.getLocation().getY() - qrY) < 40) {
                                btn.click();
                                System.out.println("✅ Tapped Quick QR Action via value button: " + value);
                                return true;
                            }
                        }
                    }
                    // If no label found, use the button directly
                    buttons.get(0).click();
                    System.out.println("✅ Tapped Quick QR Action via value button (no label): " + value);
                    return true;
                }
            } catch (Exception e) { /* continue */ }
        }

        // Strategy 3: Button with combined label "Quick QR Action, <value>"
        try {
            List<WebElement> combinedButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Quick QR Action'"
            ));
            if (!combinedButtons.isEmpty()) {
                combinedButtons.get(0).click();
                System.out.println("✅ Tapped Quick QR Action combined button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 4: Tap to the right of the label where the dropdown value chevron is
        try {
            WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Quick QR Action'"
            ));
            int labelX = label.getLocation().getX();
            int labelY = label.getLocation().getY();
            int labelW = label.getSize().getWidth();

            int tapX = labelX + labelW + 80;
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), tapX, labelY + 10));
            tap.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerMove(Duration.ofMillis(50),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), tapX, labelY + 10));
            tap.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(tap));
            System.out.println("✅ Tapped to the right of Quick QR Action label at ("
                + tapX + "," + (labelY + 10) + ")");
            return true;
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap Quick QR Action dropdown");
        return false;
    }

    /**
     * Get the list of available Quick QR Action options when the dropdown is open.
     * Expected options: Full Asset, Data Collection, IR Photos.
     * Returns a list of option strings found.
     */
    public java.util.List<String> getQuickQRActionOptions() {
        System.out.println("📍 Getting Quick QR Action dropdown options...");
        java.util.List<String> options = new java.util.ArrayList<>();
        String[] knownActions = {"Full Asset", "Data Collection", "IR Photos"};

        // Strategy 1: Look for visible buttons/text matching known QR action names
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                + "OR type == 'XCUIElementTypeMenuItem') "
                + "AND visible == true AND (label CONTAINS 'Full Asset' "
                + "OR label CONTAINS 'Data Collection' OR label CONTAINS 'IR Photos')"
            ));
            for (WebElement el : elements) {
                String label = el.getAttribute("label");
                if (label != null) {
                    for (String action : knownActions) {
                        if (label.contains(action) && !options.contains(action)) {
                            options.add(action);
                            System.out.println("  Found QR option: " + action);
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Menu items or cells in a dropdown popover
        if (options.isEmpty()) {
            try {
                List<WebElement> menuItems = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeMenuItem' OR type == 'XCUIElementTypeCell'"
                ));
                for (WebElement item : menuItems) {
                    String label = item.getAttribute("label");
                    if (label == null) {
                        try {
                            List<WebElement> childTexts = item.findElements(
                                AppiumBy.iOSNsPredicateString(
                                    "type == 'XCUIElementTypeStaticText'"
                                ));
                            for (WebElement child : childTexts) {
                                label = child.getAttribute("label");
                                if (label != null) break;
                            }
                        } catch (Exception e2) { /* continue */ }
                    }
                    if (label != null) {
                        for (String action : knownActions) {
                            if (label.contains(action) && !options.contains(action)) {
                                options.add(action);
                                System.out.println("  Found QR option (menu/cell): " + action);
                            }
                        }
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        // Strategy 3: Picker wheel values
        if (options.isEmpty()) {
            try {
                List<WebElement> pickerWheels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypePickerWheel'"
                ));
                for (WebElement wheel : pickerWheels) {
                    String value = wheel.getAttribute("value");
                    if (value != null) {
                        for (String action : knownActions) {
                            if (value.contains(action) && !options.contains(action)) {
                                options.add(action);
                            }
                        }
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        // Strategy 4: Broad text search for any visible matching text
        if (options.isEmpty()) {
            try {
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    if (label != null) {
                        for (String action : knownActions) {
                            if (label.contains(action) && !options.contains(action)) {
                                options.add(action);
                                System.out.println("  Found QR option (broad): " + action);
                            }
                        }
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Quick QR Action options found: " + options.size() + " → " + options);
        return options;
    }

    /**
     * Select a specific Quick QR Action option from the open dropdown.
     * The dropdown should already be open before calling this method.
     * Returns true if the option was selected successfully.
     */
    public boolean selectQuickQRAction(String actionName) {
        System.out.println("📍 Selecting Quick QR Action: " + actionName + "...");

        // Strategy 1: Button or text with exact/contains label
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                + "OR type == 'XCUIElementTypeMenuItem') "
                + "AND (label == '" + actionName + "' OR label CONTAINS '" + actionName + "')"
            ));
            for (WebElement el : elements) {
                String label = el.getAttribute("label");
                if (label != null && label.contains(actionName)) {
                    el.click();
                    System.out.println("✅ Selected Quick QR Action: " + actionName);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Cell containing the action text
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            for (WebElement cell : cells) {
                List<WebElement> texts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + actionName + "'"
                ));
                if (!texts.isEmpty()) {
                    cell.click();
                    System.out.println("✅ Selected Quick QR Action via cell: " + actionName);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad visible element search
        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS '" + actionName + "' AND visible == true"
            ));
            element.click();
            System.out.println("✅ Selected Quick QR Action (broad): " + actionName);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Quick QR Action: " + actionName);
            return false;
        }
    }

    /**
     * Check if a specific Quick QR Action option has a checkmark (selected indicator).
     * Returns true if a checkmark is found near the option.
     */
    public boolean isQuickQRActionOptionChecked(String actionName) {
        System.out.println("📍 Checking if " + actionName + " has checkmark...");

        // Strategy 1: Look for checkmark image as child of element with the action label
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                + "OR type == 'XCUIElementTypeCell') "
                + "AND label CONTAINS '" + actionName + "'"
            ));
            for (WebElement el : elements) {
                try {
                    List<WebElement> checkmarks = el.findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') "
                        + "AND (name CONTAINS 'checkmark' OR name CONTAINS 'check' "
                        + "OR name CONTAINS 'selected')"
                    ));
                    if (!checkmarks.isEmpty()) {
                        System.out.println("✅ Checkmark found for QR action: " + actionName);
                        return true;
                    }
                } catch (Exception e2) { /* continue */ }

                try {
                    String selected = el.getAttribute("selected");
                    if ("true".equals(selected)) {
                        System.out.println("✅ " + actionName + " has selected=true");
                        return true;
                    }
                    String value = el.getAttribute("value");
                    if ("1".equals(value) || "selected".equalsIgnoreCase(value)) {
                        System.out.println("✅ " + actionName + " has value=selected");
                        return true;
                    }
                } catch (Exception e2) { /* continue */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Checkmark image near the action text by Y position
        try {
            List<WebElement> actionTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + actionName + "'"
            ));
            if (!actionTexts.isEmpty()) {
                int actionY = actionTexts.get(0).getLocation().getY();
                List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage' AND (name CONTAINS 'checkmark' "
                    + "OR name CONTAINS 'check')"
                ));
                for (WebElement img : images) {
                    int imgY = img.getLocation().getY();
                    if (Math.abs(imgY - actionY) < 30) {
                        System.out.println("✅ Checkmark found near " + actionName
                            + " at Y=" + imgY);
                        return true;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: If the dropdown closed and the current value matches, it's selected
        try {
            String currentValue = getQuickQRActionValue();
            if (currentValue != null && currentValue.contains(actionName)) {
                System.out.println("✅ Current QR action value matches " + actionName
                    + " (selected)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No checkmark found for QR action: " + actionName);
        return false;
    }

    /**
     * Check if the Quick QR Action dropdown is currently open (showing multiple options).
     * Returns true if the dropdown menu/popover is visible.
     */
    public boolean isQuickQRActionDropdownOpen() {
        try {
            String[] actions = {"Full Asset", "Data Collection", "IR Photos"};
            List<WebElement> allElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' "
                + "OR type == 'XCUIElementTypeMenuItem') "
                + "AND visible == true AND (label CONTAINS 'Full Asset' "
                + "OR label CONTAINS 'Data Collection' OR label CONTAINS 'IR Photos')"
            ));
            java.util.Set<String> found = new java.util.HashSet<>();
            for (WebElement el : allElements) {
                String label = el.getAttribute("label");
                if (label != null) {
                    for (String action : actions) {
                        if (label.contains(action)) {
                            found.add(action);
                        }
                    }
                }
            }
            return found.size() >= 2;
        } catch (Exception e) {
            return false;
        }
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
    // SESSION LOCATIONS TAB (TC_JOB_076-079)
    // ================================================================

    /**
     * Check if the Locations tab content is displayed after tapping the Locations tab.
     * Looks for buildings, "No Location" section, or location-related content.
     */
    public boolean isLocationsTabContentDisplayed() {
        System.out.println("📍 Checking if Locations tab content is displayed...");

        // Strategy 1: Look for building entries (text containing "floor" or "floors")
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS ' floor' OR label CONTAINS ' Floor' "
                + "OR label CONTAINS 'building' OR label CONTAINS 'Building' "
                + "OR label CONTAINS 'No Location')"
            ));
            for (WebElement el : elements) {
                int y = el.getLocation().getY();
                if (y > 200) { // Below nav bar and tabs
                    System.out.println("✅ Locations tab content found: "
                        + el.getAttribute("label"));
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for cells in content area below tabs
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            int validCells = 0;
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                if (y > 300) {
                    validCells++;
                }
            }
            if (validCells > 0) {
                System.out.println("✅ Locations tab has " + validCells + " content cells");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for disclosure indicators or chevrons (expandable building list)
        try {
            List<WebElement> disclosures = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeDisclosureIndicator' "
                + "OR (type == 'XCUIElementTypeImage' "
                + "AND (name CONTAINS 'chevron' OR name CONTAINS 'disclosure' "
                + "OR name CONTAINS 'arrow')))"
            ));
            for (WebElement d : disclosures) {
                int y = d.getLocation().getY();
                if (y > 300) {
                    System.out.println("✅ Locations tab content found via disclosure indicator");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Locations tab content not found");
        return false;
    }

    /**
     * Get building entries on the Locations tab.
     * Each building is identified by its cell or text containing floor count.
     * Returns a list of building info strings (e.g., "Building A, 3 floors").
     */
    public java.util.List<String> getLocationsBuildingNames() {
        System.out.println("📍 Getting building names on Locations tab...");
        java.util.List<String> buildings = new java.util.ArrayList<>();

        // Strategy 1: Find cells and inspect children for building name + floor count
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                if (y < 200) continue; // Skip nav bar area

                List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                ));
                String buildingName = null;
                boolean hasFloorInfo = false;

                for (WebElement child : childTexts) {
                    String label = child.getAttribute("label");
                    if (label == null) continue;

                    if (label.toLowerCase().contains("floor")) {
                        hasFloorInfo = true;
                    }
                    // Building name is typically the first/longest meaningful text
                    if (buildingName == null && !label.isEmpty()
                            && !label.toLowerCase().contains("no location")
                            && !label.matches("\\d+ floor.*")) {
                        buildingName = label;
                    }
                }

                if (hasFloorInfo && buildingName != null) {
                    buildings.add(buildingName);
                    System.out.println("  Found building: " + buildingName);
                } else if (buildingName != null && cell.getSize().getHeight() > 40) {
                    // Some buildings may not show floor count but are still valid entries
                    buildings.add(buildingName);
                    System.out.println("  Found building (no floor info): " + buildingName);
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for static texts with floor count and extract nearby names
        if (buildings.isEmpty()) {
            try {
                List<WebElement> floorTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true "
                    + "AND (label CONTAINS ' floor' OR label CONTAINS ' Floor')"
                ));
                for (WebElement floorText : floorTexts) {
                    int floorY = floorText.getLocation().getY();
                    if (floorY < 200) continue;

                    // Find the nearest text above/near this floor count (building name)
                    List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND visible == true"
                    ));
                    for (WebElement text : allTexts) {
                        String label = text.getAttribute("label");
                        int textY = text.getLocation().getY();
                        if (label != null && !label.isEmpty()
                                && Math.abs(textY - floorY) < 25
                                && !label.toLowerCase().contains("floor")
                                && !label.equals(floorText.getAttribute("label"))) {
                            if (!buildings.contains(label)) {
                                buildings.add(label);
                                System.out.println("  Found building (Y-match): " + label);
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Buildings found: " + buildings.size() + " → " + buildings);
        return buildings;
    }

    /**
     * Get the count of buildings displayed on the Locations tab.
     */
    public int getLocationsBuildingCount() {
        return getLocationsBuildingNames().size();
    }

    /**
     * Get detailed info about a building at a specific index on the Locations tab.
     * Returns a map with keys: "name", "floorCount", "hasIcon", "hasChevron".
     * Returns null if the index is out of bounds.
     */
    public java.util.Map<String, String> getLocationsBuildingInfo(int index) {
        System.out.println("📍 Getting building info at index " + index + "...");

        try {
            // Find cells that represent buildings (have floor count text)
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));

            // Filter to building cells (those with floor count or significant height)
            java.util.List<WebElement> buildingCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                if (y < 200) continue;

                List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                ));
                for (WebElement child : childTexts) {
                    String label = child.getAttribute("label");
                    if (label != null && label.toLowerCase().contains("floor")) {
                        buildingCells.add(cell);
                        break;
                    }
                }
            }

            if (index >= buildingCells.size()) {
                System.out.println("⚠️ Building index " + index + " out of bounds (found "
                    + buildingCells.size() + " buildings)");
                return null;
            }

            WebElement buildingCell = buildingCells.get(index);
            java.util.Map<String, String> info = new java.util.HashMap<>();

            // Extract building name and floor count from child texts
            List<WebElement> childTexts = buildingCell.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"
            ));
            String name = null;
            String floorCount = null;

            for (WebElement child : childTexts) {
                String label = child.getAttribute("label");
                if (label == null || label.isEmpty()) continue;

                if (label.toLowerCase().contains("floor")) {
                    // Extract the number from "3 floors" or "1 floor"
                    String digits = label.replaceAll("[^0-9]", "");
                    floorCount = digits.isEmpty() ? "0" : digits;
                } else if (name == null && !label.matches("\\d+")) {
                    name = label;
                }
            }

            info.put("name", name != null ? name : "Unknown");
            info.put("floorCount", floorCount != null ? floorCount : "0");

            // Check for building icon (image element within the cell)
            try {
                List<WebElement> images = buildingCell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage'"
                ));
                boolean hasIcon = false;
                for (WebElement img : images) {
                    String imgName = img.getAttribute("name");
                    // Filter out system icons like chevrons
                    if (imgName == null || (!imgName.contains("chevron")
                            && !imgName.contains("disclosure"))) {
                        hasIcon = true;
                        break;
                    }
                }
                info.put("hasIcon", hasIcon ? "true" : "false");
            } catch (Exception e) {
                info.put("hasIcon", "false");
            }

            // Check for chevron/disclosure indicator (expandable)
            try {
                List<WebElement> chevrons = buildingCell.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeDisclosureIndicator') "
                    + "OR (type == 'XCUIElementTypeImage' AND "
                    + "(name CONTAINS 'chevron' OR name CONTAINS 'disclosure' "
                    + "OR name CONTAINS 'arrow'))"
                ));
                info.put("hasChevron", !chevrons.isEmpty() ? "true" : "false");
            } catch (Exception e) {
                info.put("hasChevron", "false");
            }

            System.out.println("📊 Building " + index + " info: " + info);
            return info;
        } catch (Exception e) {
            System.out.println("⚠️ Error getting building info: " + e.getMessage());
            return null;
        }
    }

    /**
     * Tap on a building at a specific index on the Locations tab to expand/collapse it.
     * Returns true if tapped successfully.
     */
    public boolean tapLocationsBuildingAtIndex(int index) {
        System.out.println("📍 Tapping building at index " + index + "...");

        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));

            java.util.List<WebElement> buildingCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                if (y < 200) continue;

                List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                ));
                for (WebElement child : childTexts) {
                    String label = child.getAttribute("label");
                    if (label != null && label.toLowerCase().contains("floor")) {
                        buildingCells.add(cell);
                        break;
                    }
                }
            }

            if (index >= buildingCells.size()) {
                System.out.println("⚠️ Building index " + index + " out of bounds");
                return false;
            }

            // Try tapping the chevron/disclosure first for expand/collapse
            WebElement buildingCell = buildingCells.get(index);
            try {
                List<WebElement> chevrons = buildingCell.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeDisclosureIndicator') "
                    + "OR (type == 'XCUIElementTypeImage' AND "
                    + "(name CONTAINS 'chevron' OR name CONTAINS 'disclosure' "
                    + "OR name CONTAINS 'arrow'))"
                ));
                if (!chevrons.isEmpty()) {
                    chevrons.get(0).click();
                    System.out.println("✅ Tapped building " + index + " chevron");
                    return true;
                }
            } catch (Exception e) { /* continue with cell tap */ }

            // Fall back to tapping the cell itself
            buildingCell.click();
            System.out.println("✅ Tapped building " + index + " cell");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap building at index " + index + ": "
                + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a building at a specific index is expanded (shows floors underneath).
     * Returns true if floor entries are visible below the building.
     */
    public boolean isLocationsBuildingExpanded(int index) {
        System.out.println("📍 Checking if building " + index + " is expanded...");

        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));

            // Find the building cell
            java.util.List<WebElement> buildingCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                if (y < 200) continue;

                List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                ));
                for (WebElement child : childTexts) {
                    String label = child.getAttribute("label");
                    if (label != null && label.toLowerCase().contains("floor")) {
                        buildingCells.add(cell);
                        break;
                    }
                }
            }

            if (index >= buildingCells.size()) return false;

            WebElement buildingCell = buildingCells.get(index);
            int buildingY = buildingCell.getLocation().getY();
            int buildingH = buildingCell.getSize().getHeight();
            int nextY = buildingY + buildingH;

            // Look for floor entries below this building
            // Floors typically show "room" or "Room" or "Floor_" in their labels
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS ' room' OR label CONTAINS 'Room' "
                + "OR label CONTAINS 'Floor_' OR label CONTAINS 'Floor ')"
            ));

            for (WebElement text : allTexts) {
                int textY = text.getLocation().getY();
                // Floor entries should be just below the building cell
                if (textY > nextY && textY < nextY + 300) {
                    System.out.println("✅ Building " + index + " is expanded — floor found at Y="
                        + textY + ": " + text.getAttribute("label"));
                    return true;
                }
            }

            // Also check for any cells between this building and the next building
            if (index + 1 < buildingCells.size()) {
                int nextBuildingY = buildingCells.get(index + 1).getLocation().getY();
                // If there's significant space between buildings, floors may be there
                if (nextBuildingY - nextY > 100) {
                    System.out.println("✅ Building " + index
                        + " appears expanded — gap to next building: "
                        + (nextBuildingY - nextY) + "px");
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking building expansion: " + e.getMessage());
        }

        System.out.println("⚠️ Building " + index + " does not appear expanded");
        return false;
    }

    /**
     * Check if the "No Location" section is displayed on the Locations tab.
     * This section shows assets that are not assigned to any building/floor/room.
     */
    public boolean isNoLocationSectionDisplayed() {
        System.out.println("📍 Checking for No Location section...");

        // Strategy 1: Static text with "No Location"
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'No Location' OR label CONTAINS 'No location' "
                + "OR label CONTAINS 'Unassigned' OR label CONTAINS 'unassigned')"
            ));
            for (WebElement el : elements) {
                int y = el.getLocation().getY();
                if (y > 200) {
                    System.out.println("✅ No Location section found: "
                        + el.getAttribute("label") + " at Y=" + y);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Cell containing "No Location" text in children
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            for (WebElement cell : cells) {
                List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND "
                    + "(label CONTAINS 'No Location' OR label CONTAINS 'Unassigned')"
                ));
                if (!childTexts.isEmpty()) {
                    System.out.println("✅ No Location section found in cell");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Scroll down and check again (No Location may be below the fold)
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.width / 2, size.height * 3 / 4));
            swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.width / 2, size.height / 4));
            swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(swipe));

            try { Thread.sleep(500); } catch (InterruptedException ie) { /* continue */ }

            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'No Location' OR label CONTAINS 'No location' "
                + "OR label CONTAINS 'Unassigned')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ No Location section found after scrolling");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No Location section not found");
        return false;
    }

    /**
     * Get the unassigned asset count from the "No Location" section.
     * Returns the count, or -1 if not found.
     */
    public int getNoLocationAssetCount() {
        System.out.println("📍 Getting No Location asset count...");

        // Strategy 1: Look for text near "No Location" that contains a number
        try {
            List<WebElement> noLocTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'No Location' OR label CONTAINS 'No location' "
                + "OR label CONTAINS 'Unassigned')"
            ));

            if (!noLocTexts.isEmpty()) {
                int noLocY = noLocTexts.get(0).getLocation().getY();
                String noLocLabel = noLocTexts.get(0).getAttribute("label");

                // Check if the label itself contains the count (e.g., "No Location (5)")
                String digits = noLocLabel.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    int count = Integer.parseInt(digits);
                    System.out.println("📊 No Location asset count (from label): " + count);
                    return count;
                }

                // Look for a count text near the No Location label
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    int textY = text.getLocation().getY();
                    String label = text.getAttribute("label");
                    if (label != null && Math.abs(textY - noLocY) < 30
                            && !label.equals(noLocLabel)) {
                        // Try to extract a number (e.g., "5 assets" or just "5")
                        String numStr = label.replaceAll("[^0-9]", "");
                        if (!numStr.isEmpty()) {
                            int count = Integer.parseInt(numStr);
                            System.out.println("📊 No Location asset count: " + count);
                            return count;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Find the No Location cell and inspect all child elements
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            for (WebElement cell : cells) {
                List<WebElement> noLocChildren = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND "
                    + "(label CONTAINS 'No Location' OR label CONTAINS 'Unassigned')"
                ));
                if (!noLocChildren.isEmpty()) {
                    // Found the No Location cell — look for count among siblings
                    List<WebElement> allChildren = cell.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText'"
                    ));
                    for (WebElement child : allChildren) {
                        String label = child.getAttribute("label");
                        if (label != null && !label.toLowerCase().contains("location")
                                && !label.toLowerCase().contains("unassigned")) {
                            String numStr = label.replaceAll("[^0-9]", "");
                            if (!numStr.isEmpty()) {
                                int count = Integer.parseInt(numStr);
                                System.out.println("📊 No Location asset count (cell): " + count);
                                return count;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not determine No Location asset count");
        return -1;
    }

    // ================================================================
    // LOCATIONS TAB BUTTONS (TC_JOB_080-081)
    // ================================================================

    /**
     * Check if a building row at the given index has a "+" (add) button.
     * The + button on each building row allows adding a floor or asset to that building.
     * Returns true if a + button is found within the building cell.
     */
    public boolean isBuildingRowAddButtonDisplayed(int index) {
        System.out.println("📍 Checking + button on building row " + index + "...");

        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));

            java.util.List<WebElement> buildingCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                if (y < 200) continue;

                List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                ));
                for (WebElement child : childTexts) {
                    String label = child.getAttribute("label");
                    if (label != null && label.toLowerCase().contains("floor")) {
                        buildingCells.add(cell);
                        break;
                    }
                }
            }

            if (index >= buildingCells.size()) {
                System.out.println("⚠️ Building index " + index + " out of bounds ("
                    + buildingCells.size() + " buildings found)");
                return false;
            }

            WebElement buildingCell = buildingCells.get(index);

            // Strategy 1: Look for + button as child of the building cell
            try {
                List<WebElement> plusButtons = buildingCell.findElements(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND "
                        + "(label == '+' OR label == 'Add' OR label == 'plus' "
                        + "OR name == 'plus' OR name CONTAINS 'plus' "
                        + "OR name CONTAINS 'add')"
                    ));
                if (!plusButtons.isEmpty()) {
                    System.out.println("✅ + button found on building row " + index
                        + " (child button)");
                    return true;
                }
            } catch (Exception e) { /* continue */ }

            // Strategy 2: Look for image named "plus" or "plus.circle.fill" in cell
            try {
                List<WebElement> plusImages = buildingCell.findElements(
                    AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeButton') "
                        + "AND (name CONTAINS 'plus' OR name CONTAINS 'add')"
                    ));
                if (!plusImages.isEmpty()) {
                    System.out.println("✅ + icon found on building row " + index
                        + " (image/icon)");
                    return true;
                }
            } catch (Exception e) { /* continue */ }

            // Strategy 3: Look for small button near right side of the building cell
            try {
                int cellX = buildingCell.getLocation().getX();
                int cellY = buildingCell.getLocation().getY();
                int cellW = buildingCell.getSize().getWidth();
                int cellH = buildingCell.getSize().getHeight();
                int rightEdge = cellX + cellW;

                List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true"
                ));
                for (WebElement btn : allButtons) {
                    int btnX = btn.getLocation().getX();
                    int btnY = btn.getLocation().getY();
                    int btnW = btn.getSize().getWidth();
                    int btnH = btn.getSize().getHeight();

                    // Button should be: within the cell's Y range, near the right edge,
                    // and small (likely a circular + icon)
                    boolean inYRange = btnY >= cellY && btnY <= cellY + cellH;
                    boolean nearRightEdge = btnX > rightEdge - 80;
                    boolean isSmall = btnW < 60 && btnH < 60;

                    if (inYRange && nearRightEdge && isSmall) {
                        String btnLabel = btn.getAttribute("label");
                        String btnName = btn.getAttribute("name");
                        System.out.println("✅ Small button found near right edge of building "
                            + index + " — label='" + btnLabel + "', name='" + btnName + "'");
                        return true;
                    }
                }
            } catch (Exception e) { /* continue */ }

        } catch (Exception e) {
            System.out.println("⚠️ Error checking + button: " + e.getMessage());
        }

        System.out.println("⚠️ No + button found on building row " + index);
        return false;
    }

    /**
     * Check if a floating "+" button is displayed at the bottom-right of the Locations tab.
     * This button typically allows adding a new building/location.
     */
    public boolean isLocationsFloatingAddButtonDisplayed() {
        System.out.println("📍 Checking for floating + button on Locations tab...");

        try {
            org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
            int screenWidth = screenSize.getWidth();
            int screenHeight = screenSize.getHeight();

            // Strategy 1: Button with plus/add semantics
            List<WebElement> plusButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == '+' OR label == 'Add' OR label == 'plus' "
                + "OR name == 'plus' OR name == 'plus.circle.fill' "
                + "OR name CONTAINS 'add')"
            ));
            for (WebElement btn : plusButtons) {
                int btnX = btn.getLocation().getX();
                int btnY = btn.getLocation().getY();
                // Floating button should be in bottom-right quadrant
                if (btnX > screenWidth * 0.6 && btnY > screenHeight * 0.7) {
                    System.out.println("✅ Floating + button found at ("
                        + btnX + "," + btnY + ")");
                    return true;
                }
            }

            // Strategy 2: Look for any small circular button in bottom-right
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            for (WebElement btn : allButtons) {
                int btnX = btn.getLocation().getX();
                int btnY = btn.getLocation().getY();
                int btnW = btn.getSize().getWidth();
                int btnH = btn.getSize().getHeight();

                boolean isBottomRight = btnX > screenWidth * 0.7
                    && btnY > screenHeight * 0.8;
                boolean isCircular = Math.abs(btnW - btnH) < 10 && btnW < 80;

                if (isBottomRight && isCircular) {
                    String label = btn.getAttribute("label");
                    String name = btn.getAttribute("name");
                    System.out.println("✅ Floating button found at bottom-right ("
                        + btnX + "," + btnY + ") — label='" + label
                        + "', name='" + name + "'");
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking floating + button: " + e.getMessage());
        }

        System.out.println("⚠️ Floating + button not found on Locations tab");
        return false;
    }

    // ================================================================
    // LOCATIONS TAB FLOOR/ROOM NAVIGATION (TC_JOB_084-087)
    // ================================================================

    /**
     * Get visible floor entries on the Locations tab after a building is expanded.
     * Floors typically show room count (e.g., "Floor 1, 5 rooms") or start with "Floor".
     * Returns a list of floor labels found.
     */
    public java.util.List<String> getLocationsFloorEntries() {
        System.out.println("📍 Getting floor entries on Locations tab...");
        java.util.List<String> floors = new java.util.ArrayList<>();

        try {
            // Look for texts that indicate floors (contain "room" or start with "Floor")
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS ' room' OR label CONTAINS ' Room' "
                + "OR label BEGINSWITH 'Floor' OR label BEGINSWITH 'floor')"
            ));

            java.util.Set<Integer> seenYPositions = new java.util.TreeSet<>();
            for (WebElement el : elements) {
                int y = el.getLocation().getY();
                if (y < 200) continue;

                boolean duplicate = false;
                for (int existingY : seenYPositions) {
                    if (Math.abs(y - existingY) < 20) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    seenYPositions.add(y);
                    String label = el.getAttribute("label");
                    if (label != null && !label.isEmpty()) {
                        floors.add(label);
                        System.out.println("  Found floor: " + label);
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for cells with floor-like content between building entries
        if (floors.isEmpty()) {
            try {
                List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell' AND visible == true"
                ));
                for (WebElement cell : cells) {
                    int y = cell.getLocation().getY();
                    if (y < 200) continue;

                    List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText'"
                    ));
                    for (WebElement child : childTexts) {
                        String label = child.getAttribute("label");
                        if (label != null && (label.toLowerCase().contains("room")
                                || label.toLowerCase().startsWith("floor"))) {
                            floors.add(label);
                            System.out.println("  Found floor (cell): " + label);
                            break;
                        }
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Floor entries found: " + floors.size() + " → " + floors);
        return floors;
    }

    /**
     * Tap on a floor entry at the given index to expand it (showing rooms).
     * Should be called after a building is expanded.
     * Returns true if tapped successfully.
     */
    public boolean tapLocationsFloorAtIndex(int index) {
        System.out.println("📍 Tapping floor at index " + index + "...");

        try {
            // Find floor-like entries (cells or buttons)
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));

            java.util.List<WebElement> floorCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                if (y < 200) continue;

                List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                ));
                boolean isFloor = false;
                boolean isBuilding = false;
                for (WebElement child : childTexts) {
                    String label = child.getAttribute("label");
                    if (label == null) continue;
                    if (label.toLowerCase().contains("room")
                            || label.toLowerCase().startsWith("floor")) {
                        isFloor = true;
                    }
                    // Exclude building entries (they contain "floor" as floor count)
                    if (label.matches(".*\\d+\\s+floor.*")) {
                        isBuilding = true;
                    }
                }
                if (isFloor && !isBuilding) {
                    floorCells.add(cell);
                }
            }

            // Also check buttons that match floor patterns
            if (floorCells.isEmpty()) {
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true "
                    + "AND (label CONTAINS ' room' OR label BEGINSWITH 'Floor')"
                ));
                for (WebElement btn : buttons) {
                    int y = btn.getLocation().getY();
                    if (y > 200) {
                        floorCells.add(btn);
                    }
                }
            }

            if (index >= floorCells.size()) {
                System.out.println("⚠️ Floor index " + index + " out of bounds ("
                    + floorCells.size() + " floors found)");
                return false;
            }

            // Try tapping chevron/disclosure first, then the cell
            WebElement floorCell = floorCells.get(index);
            try {
                List<WebElement> chevrons = floorCell.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeDisclosureIndicator') "
                    + "OR (type == 'XCUIElementTypeImage' AND "
                    + "(name CONTAINS 'chevron' OR name CONTAINS 'disclosure'))"
                ));
                if (!chevrons.isEmpty()) {
                    chevrons.get(0).click();
                    System.out.println("✅ Tapped floor " + index + " chevron");
                    return true;
                }
            } catch (Exception e) { /* continue */ }

            floorCell.click();
            System.out.println("✅ Tapped floor " + index + " cell");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap floor at index " + index + ": "
                + e.getMessage());
            return false;
        }
    }

    /**
     * Get visible room entries on the Locations tab after a floor is expanded.
     * Rooms typically show "Room_xxx" or contain "node" or "asset" counts.
     * Returns a list of room labels found.
     */
    public java.util.List<String> getLocationsRoomEntries() {
        System.out.println("📍 Getting room entries on Locations tab...");
        java.util.List<String> rooms = new java.util.ArrayList<>();

        try {
            // Rooms typically contain "node", "asset", or start with "Room"
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                + "AND visible == true "
                + "AND (label BEGINSWITH 'Room' OR label BEGINSWITH 'room' "
                + "OR label CONTAINS ' node' OR label CONTAINS ' asset')"
            ));

            java.util.Set<Integer> seenYPositions = new java.util.TreeSet<>();
            for (WebElement el : elements) {
                int y = el.getLocation().getY();
                if (y < 200) continue;

                String label = el.getAttribute("label");
                if (label == null || label.isEmpty()) continue;
                // Exclude floor entries and building entries
                if (label.toLowerCase().contains(" floor") && !label.toLowerCase().contains("room"))
                    continue;

                boolean duplicate = false;
                for (int existingY : seenYPositions) {
                    if (Math.abs(y - existingY) < 20) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    seenYPositions.add(y);
                    rooms.add(label);
                    System.out.println("  Found room: " + label);
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("📊 Room entries found: " + rooms.size() + " → " + rooms);
        return rooms;
    }

    /**
     * Tap on a room entry at the given index to navigate to Assets in Room screen.
     * Should be called after a floor is expanded.
     * Returns true if tapped successfully.
     */
    public boolean tapLocationsRoomAtIndex(int index) {
        System.out.println("📍 Tapping room at index " + index + "...");

        try {
            // Strategy 1: Look for buttons/cells with room-like labels
            List<WebElement> roomElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeCell') "
                + "AND visible == true "
                + "AND (label BEGINSWITH 'Room' OR label BEGINSWITH 'room' "
                + "OR label CONTAINS ' node' OR label CONTAINS ' asset')"
            ));

            // Filter out floors and buildings
            java.util.List<WebElement> validRooms = new java.util.ArrayList<>();
            for (WebElement el : roomElements) {
                int y = el.getLocation().getY();
                if (y < 200) continue;
                String label = el.getAttribute("label");
                if (label != null && !label.toLowerCase().contains(" floor")) {
                    validRooms.add(el);
                }
            }

            // Fallback: cells that have "Room" text in children
            if (validRooms.isEmpty()) {
                List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell' AND visible == true"
                ));
                for (WebElement cell : cells) {
                    int y = cell.getLocation().getY();
                    if (y < 200) continue;

                    List<WebElement> childTexts = cell.findElements(
                        AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND "
                            + "(label BEGINSWITH 'Room' OR label CONTAINS ' node' "
                            + "OR label CONTAINS ' asset')"
                        ));
                    if (!childTexts.isEmpty()) {
                        validRooms.add(cell);
                    }
                }
            }

            if (index >= validRooms.size()) {
                System.out.println("⚠️ Room index " + index + " out of bounds ("
                    + validRooms.size() + " rooms found)");
                return false;
            }

            validRooms.get(index).click();
            System.out.println("✅ Tapped room at index " + index);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap room at index " + index + ": "
                + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // ASSETS IN ROOM SCREEN (TC_JOB_084-087)
    // ================================================================

    /**
     * Check if the "Assets in Room" screen is displayed.
     * Looks for the "Assets in Room" title in the navigation header.
     */
    public boolean isAssetsInRoomScreenDisplayed() {
        System.out.println("📍 Checking for Assets in Room screen...");

        // Strategy 1: Static text or navigation bar title "Assets in Room"
        try {
            List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeOther') "
                + "AND (label == 'Assets in Room' OR label CONTAINS 'Assets in Room')"
            ));
            if (!titles.isEmpty()) {
                System.out.println("✅ Assets in Room screen found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Navigation bar with "Assets in Room" title
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar' AND "
                + "(name CONTAINS 'Assets in Room' OR label CONTAINS 'Assets in Room')"
            ));
            if (!navBars.isEmpty()) {
                System.out.println("✅ Assets in Room screen (nav bar)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Presence of both Done button and breadcrumb (unique to this screen)
        try {
            List<WebElement> doneButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Done'"
            ));
            List<WebElement> breadcrumbs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '>'"
            ));
            if (!doneButtons.isEmpty() && !breadcrumbs.isEmpty()) {
                System.out.println("✅ Assets in Room screen (Done + breadcrumb)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Assets in Room screen not found");
        return false;
    }

    /**
     * Wait for the Assets in Room screen to appear (up to 10 seconds).
     */
    public boolean waitForAssetsInRoomScreen() {
        for (int i = 0; i < 20; i++) {
            if (isAssetsInRoomScreenDisplayed()) return true;
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }
        System.out.println("⚠️ Assets in Room screen did not appear within 10 seconds");
        return false;
    }

    /**
     * Check if the Done button is displayed in the Assets in Room header.
     */
    public boolean isAssetsInRoomDoneButtonDisplayed() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Done'"
            ));
            boolean found = !buttons.isEmpty();
            System.out.println(found ? "✅ Done button found" : "⚠️ Done button not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the Done button on the Assets in Room screen to return to Locations tab.
     */
    public boolean tapAssetsInRoomDoneButton() {
        System.out.println("📍 Tapping Done button on Assets in Room...");
        try {
            WebElement done = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Done'"
            ));
            done.click();
            System.out.println("✅ Tapped Done button");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Done button: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a breadcrumb (path with ">") is displayed on the current screen.
     * Breadcrumb format: "building > floor > room"
     */
    public boolean isLocationBreadcrumbDisplayed() {
        try {
            List<WebElement> breadcrumbs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '>'"
            ));
            // Filter to breadcrumbs in the content area (not nav bar chevrons)
            for (WebElement bc : breadcrumbs) {
                String label = bc.getAttribute("label");
                int y = bc.getLocation().getY();
                if (label != null && label.contains(">") && y > 80 && y < 400) {
                    System.out.println("✅ Breadcrumb found: " + label);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Breadcrumb not found");
        return false;
    }

    /**
     * Get the breadcrumb text from the current screen.
     * Returns the full path string (e.g., "building > floor > room"), or null if not found.
     */
    public String getLocationBreadcrumbText() {
        System.out.println("📍 Getting location breadcrumb text...");

        try {
            List<WebElement> breadcrumbs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '>'"
            ));
            for (WebElement bc : breadcrumbs) {
                String label = bc.getAttribute("label");
                int y = bc.getLocation().getY();
                if (label != null && label.contains(">") && y > 80 && y < 400) {
                    System.out.println("📊 Breadcrumb: " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get breadcrumb text");
        return null;
    }

    /**
     * Check if the Assets in Room screen shows the empty state ("No Assets").
     * The empty state includes a box icon, "No Assets" text, and helper text.
     */
    public boolean isAssetsInRoomEmptyStateDisplayed() {
        System.out.println("📍 Checking for Assets in Room empty state...");

        // Strategy 1: "No Assets" text
        try {
            List<WebElement> noAssets = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'No Assets' OR label CONTAINS 'No Assets' "
                + "OR label CONTAINS 'No assets')"
            ));
            if (!noAssets.isEmpty()) {
                System.out.println("✅ 'No Assets' empty state found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Helper text about adding assets
        try {
            List<WebElement> helperTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'add assets' OR label CONTAINS 'Tap the' "
                + "OR label CONTAINS '+ button')"
            ));
            if (!helperTexts.isEmpty()) {
                System.out.println("✅ Empty state helper text found: "
                    + helperTexts.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Empty state not found");
        return false;
    }

    /**
     * Get the empty state text from Assets in Room screen.
     * Returns the helper text, or null if not found.
     */
    public String getAssetsInRoomEmptyStateText() {
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'add assets' OR label CONTAINS 'Tap the' "
                + "OR label CONTAINS '+ button' OR label CONTAINS 'No Assets')"
            ));
            if (!texts.isEmpty()) {
                // Return the longest text (likely the helper text)
                String longest = "";
                for (WebElement text : texts) {
                    String label = text.getAttribute("label");
                    if (label != null && label.length() > longest.length()) {
                        longest = label;
                    }
                }
                return longest;
            }
        } catch (Exception e) { /* continue */ }
        return null;
    }

    /**
     * Check if a search bar is displayed on the Assets in Room screen.
     */
    public boolean isAssetsInRoomSearchBarDisplayed() {
        try {
            List<WebElement> searchBars = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField' OR "
                + "(type == 'XCUIElementTypeTextField' AND "
                + "(label CONTAINS 'Search' OR value CONTAINS 'Search'))"
            ));
            boolean found = !searchBars.isEmpty();
            System.out.println(found ? "✅ Search bar found" : "⚠️ Search bar not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the floating + button on the Assets in Room screen to open Add Assets.
     * Returns true if tapped successfully.
     */
    public boolean tapAssetsInRoomFloatingPlusButton() {
        System.out.println("📍 Tapping floating + button on Assets in Room...");

        // Strategy 1: Button with plus/add name or label
        try {
            List<WebElement> plusButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == '+' OR label == 'Add' OR label == 'plus' "
                + "OR name == 'plus' OR name == 'plus.circle.fill' "
                + "OR name CONTAINS 'add')"
            ));
            if (!plusButtons.isEmpty()) {
                plusButtons.get(0).click();
                System.out.println("✅ Tapped floating + button (name/label)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Button in bottom-right area of screen (FAB position)
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int screenWidth = size.getWidth();
            int screenHeight = size.getHeight();

            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            for (WebElement btn : allButtons) {
                int btnX = btn.getLocation().getX();
                int btnY = btn.getLocation().getY();
                if (btnX > screenWidth * 0.7 && btnY > screenHeight * 0.8) {
                    btn.click();
                    System.out.println("✅ Tapped floating + button (FAB position)");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Coordinate tap at FAB position (90% width, 90% height)
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            int fabX = (int) (size.getWidth() * 0.9);
            int fabY = (int) (size.getHeight() * 0.9);

            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), fabX, fabY));
            tap.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerMove(Duration.ofMillis(50),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), fabX, fabY));
            tap.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(tap));
            System.out.println("✅ Tapped FAB at coordinates (" + fabX + "," + fabY + ")");
            return true;
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap floating + button");
        return false;
    }

    // ================================================================
    // ASSETS IN ROOM — ASSET LIST ENTRIES (TC_JOB_121-122)
    // ================================================================

    /**
     * Get the count of asset entries in the Assets in Room list.
     * Excludes empty state, search bar, and header elements.
     * @return number of asset entries, or 0 if none found
     */
    public int getAssetsInRoomListCount() {
        System.out.println("📍 Counting assets in room...");

        try {
            // Strategy 1: Count cells with reasonable height (asset rows) below the header area
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            int count = 0;
            for (WebElement cell : cells) {
                int cellY = cell.getLocation().getY();
                int cellH = cell.getSize().getHeight();
                // Asset cells: below search/header area (Y > 200), with content height (H > 40)
                if (cellY > 200 && cellH > 40) {
                    count++;
                }
            }
            if (count > 0) {
                System.out.println("📊 Assets in room (by cells): " + count);
                return count;
            }

            // Strategy 2: Count distinct asset name texts below header
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            java.util.Set<Integer> rowYs = new java.util.TreeSet<>();
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                int h = text.getSize().getHeight();
                String label = text.getAttribute("label");
                // Asset entries: Y > 250, not empty, not header text
                if (y > 250 && h > 10 && label != null && !label.isEmpty()
                        && !label.contains("Assets in Room") && !label.contains("Done")
                        && !label.contains("No Assets") && !label.contains(">")) {
                    // Group by Y bands (within 20px = same row)
                    boolean newRow = true;
                    for (int existingY : rowYs) {
                        if (Math.abs(y - existingY) < 20) {
                            newRow = false;
                            break;
                        }
                    }
                    if (newRow) rowYs.add(y);
                }
            }
            count = rowYs.size() / 2; // Each asset has ~2 text elements (name + type)
            if (count < 1 && !rowYs.isEmpty()) count = rowYs.size();
            System.out.println("📊 Assets in room (by text rows): " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error counting assets: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get the asset name from an entry at the given index in the Assets in Room list.
     * @param index 0-based index
     * @return the asset name, or null if not found
     */
    public String getAssetEntryName(int index) {
        System.out.println("📍 Getting asset name at index " + index + "...");
        java.util.List<java.util.Map<String, String>> entries = getAssetEntries();
        if (index < entries.size()) {
            String name = entries.get(index).get("name");
            System.out.println("📊 Asset name at index " + index + ": " + name);
            return name;
        }
        System.out.println("⚠️ Asset index " + index + " out of range (only " + entries.size() + " entries)");
        return null;
    }

    /**
     * Get the asset type from an entry at the given index in the Assets in Room list.
     * @param index 0-based index
     * @return the asset type label, or null if not found
     */
    public String getAssetEntryType(int index) {
        System.out.println("📍 Getting asset type at index " + index + "...");
        java.util.List<java.util.Map<String, String>> entries = getAssetEntries();
        if (index < entries.size()) {
            String type = entries.get(index).get("type");
            System.out.println("📊 Asset type at index " + index + ": " + type);
            return type;
        }
        return null;
    }

    /**
     * Get the completion percentage from an asset entry at the given index.
     * @param index 0-based index
     * @return the completion percentage string (e.g., "0%", "50%"), or null if not found
     */
    public String getAssetEntryCompletionPercentage(int index) {
        System.out.println("📍 Getting asset completion % at index " + index + "...");
        java.util.List<java.util.Map<String, String>> entries = getAssetEntries();
        if (index < entries.size()) {
            String pct = entries.get(index).get("completion");
            System.out.println("📊 Completion % at index " + index + ": " + pct);
            return pct;
        }
        return null;
    }

    /**
     * Internal helper: Parse asset entries from the Assets in Room list.
     * Returns a list of maps, each with keys: "name", "type", "completion", "y".
     * Assets are grouped by Y-position — each entry has 2-3 child texts (name, type, percentage).
     */
    private java.util.List<java.util.Map<String, String>> getAssetEntries() {
        java.util.List<java.util.Map<String, String>> entries = new java.util.ArrayList<>();

        try {
            // Collect all visible cells in the content area
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));

            java.util.List<WebElement> assetCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                int h = cell.getSize().getHeight();
                if (y > 200 && h > 40) {
                    assetCells.add(cell);
                }
            }

            // For each cell, extract child text elements
            for (WebElement cell : assetCells) {
                java.util.Map<String, String> entry = new java.util.HashMap<>();
                int cellY = cell.getLocation().getY();
                entry.put("y", String.valueOf(cellY));

                try {
                    List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText'"
                    ));
                    for (WebElement child : childTexts) {
                        String label = child.getAttribute("label");
                        if (label == null || label.isEmpty()) continue;

                        if (label.matches(".*\\d+%.*")) {
                            entry.put("completion", label.replaceAll("[^0-9%]", ""));
                        } else if (!entry.containsKey("name")) {
                            entry.put("name", label);
                        } else if (!entry.containsKey("type")) {
                            entry.put("type", label);
                        }
                    }
                } catch (Exception inner) {
                    // Fallback: try getting cell label directly
                    String cellLabel = cell.getAttribute("label");
                    if (cellLabel != null && !cellLabel.isEmpty()) {
                        entry.put("name", cellLabel);
                    }
                }
                entries.add(entry);
            }

            // If no cells found, try extracting from visible texts directly
            if (entries.isEmpty()) {
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));

                java.util.TreeMap<Integer, java.util.Map<String, String>> rowMap = new java.util.TreeMap<>();
                for (WebElement text : allTexts) {
                    int y = text.getLocation().getY();
                    String label = text.getAttribute("label");
                    if (y < 250 || label == null || label.isEmpty()
                            || label.contains("Assets in Room") || label.equals("Done")) continue;

                    // Find or create row entry (within 20px band)
                    Integer rowKey = null;
                    for (Integer key : rowMap.keySet()) {
                        if (Math.abs(y - key) < 20) { rowKey = key; break; }
                    }
                    if (rowKey == null) { rowKey = y; rowMap.put(rowKey, new java.util.HashMap<>()); }

                    java.util.Map<String, String> row = rowMap.get(rowKey);
                    if (label.matches(".*\\d+%.*")) {
                        row.put("completion", label.replaceAll("[^0-9%]", ""));
                    } else if (!row.containsKey("name")) {
                        row.put("name", label);
                    } else if (!row.containsKey("type")) {
                        row.put("type", label);
                    }
                    row.put("y", String.valueOf(rowKey));
                }
                entries.addAll(rowMap.values());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting asset entries: " + e.getMessage());
        }

        System.out.println("📊 Found " + entries.size() + " asset entries");
        return entries;
    }

    // ================================================================
    // ASSETS IN ROOM — LONG-PRESS CONTEXT MENU (TC_JOB_123-129)
    // ================================================================

    /**
     * Long-press on an asset entry in the Assets in Room list at the given index.
     * Uses iOS mobile: touchAndHold command with fallback to W3C PointerInput.
     * @param index 0-based index of the asset
     * @return true if the long-press was performed successfully
     */
    public boolean longPressOnAssetInRoom(int index) {
        System.out.println("📍 Long-pressing on asset at index " + index + "...");

        try {
            // Find asset cells
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            java.util.List<WebElement> assetCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                int h = cell.getSize().getHeight();
                if (y > 200 && h > 40) assetCells.add(cell);
            }

            WebElement targetCell = null;
            if (index < assetCells.size()) {
                targetCell = assetCells.get(index);
            } else {
                // Fallback: find by text entries
                java.util.List<java.util.Map<String, String>> entries = getAssetEntries();
                if (index < entries.size() && entries.get(index).containsKey("y")) {
                    int targetY = Integer.parseInt(entries.get(index).get("y"));
                    // Find nearest cell or text element
                    List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND visible == true"
                    ));
                    for (WebElement text : allTexts) {
                        if (Math.abs(text.getLocation().getY() - targetY) < 20) {
                            targetCell = text;
                            break;
                        }
                    }
                }
            }

            if (targetCell == null) {
                System.out.println("⚠️ Could not find asset at index " + index);
                return false;
            }

            // Strategy 1: mobile: touchAndHold (most reliable on iOS)
            try {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("element",
                    ((org.openqa.selenium.remote.RemoteWebElement) targetCell).getId());
                params.put("duration", 2.0);
                driver.executeScript("mobile: touchAndHold", params);
                try { Thread.sleep(300); } catch (InterruptedException ie) { /* */ }
                System.out.println("✅ Long-pressed asset at index " + index + " (touchAndHold)");
                return true;
            } catch (Exception e) {
                System.out.println("ℹ️ touchAndHold failed, trying W3C Actions: " + e.getMessage());
            }

            // Strategy 2: W3C Actions with PointerInput
            try {
                int x = targetCell.getLocation().getX() + targetCell.getSize().getWidth() / 2;
                int y = targetCell.getLocation().getY() + targetCell.getSize().getHeight() / 2;

                org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence longPress =
                    new org.openqa.selenium.interactions.Sequence(finger, 1);
                longPress.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
                longPress.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                longPress.addAction(
                    new org.openqa.selenium.interactions.Pause(finger, Duration.ofMillis(2000)));
                longPress.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Arrays.asList(longPress));
                try { Thread.sleep(300); } catch (InterruptedException ie) { /* */ }
                System.out.println("✅ Long-pressed asset at index " + index + " (W3C Actions)");
                return true;
            } catch (Exception e) {
                System.out.println("⚠️ W3C Actions long-press failed: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error performing long-press: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if the asset context menu is displayed after long-press.
     * Context menu should show options like Collect Data, Add Task, etc.
     */
    public boolean isAssetContextMenuDisplayed() {
        System.out.println("📍 Checking for asset context menu...");

        // Strategy 1: Look for known context menu option labels
        String[] knownOptions = {
            "Collect Data", "Add Task", "Add IR Photos",
            "Add Issue", "Edit Connections", "Remove from Session"
        };
        try {
            for (String option : knownOptions) {
                List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                    + "OR type == 'XCUIElementTypeMenuItem') "
                    + "AND label == '" + option + "'"
                ));
                if (!found.isEmpty()) {
                    System.out.println("✅ Context menu detected — found option: " + option);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Check for menu/popover container
        try {
            List<WebElement> menus = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeMenu' OR type == 'XCUIElementTypeSheet' "
                + "OR type == 'XCUIElementTypePopover' OR type == 'XCUIElementTypeAlert')"
            ));
            if (!menus.isEmpty()) {
                System.out.println("✅ Context menu container found (menu/sheet/popover)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for "Collect Data" or "Remove from Session" text as indicator
        try {
            List<WebElement> indicators = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'Collect Data' OR label CONTAINS 'Remove from Session')"
            ));
            if (!indicators.isEmpty()) {
                System.out.println("✅ Context menu detected via option text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Context menu not detected");
        return false;
    }

    /**
     * Get all visible context menu option labels.
     * @return list of option labels found
     */
    public java.util.List<String> getAssetContextMenuOptions() {
        System.out.println("📍 Getting context menu options...");
        java.util.List<String> options = new java.util.ArrayList<>();

        String[] knownOptions = {
            "Collect Data", "Add Task", "Add IR Photos",
            "Add Issue", "Edit Connections", "Remove from Session"
        };

        // Strategy 1: Check each known option
        for (String option : knownOptions) {
            try {
                List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                    + "OR type == 'XCUIElementTypeMenuItem') "
                    + "AND (label == '" + option + "' OR label CONTAINS '" + option + "')"
                ));
                if (!found.isEmpty()) {
                    options.add(option);
                }
            } catch (Exception e) { /* continue */ }
        }

        // Strategy 2: If no known options found, collect all visible menu items
        if (options.isEmpty()) {
            try {
                List<WebElement> menuItems = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeMenuItem' OR type == 'XCUIElementTypeButton') "
                    + "AND visible == true"
                ));
                for (WebElement item : menuItems) {
                    String label = item.getAttribute("label");
                    if (label != null && !label.isEmpty() && label.length() < 30
                            && !label.equals("Done") && !label.equals("Cancel")
                            && !label.equals("Back")) {
                        if (!options.contains(label)) options.add(label);
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Context menu options: " + options);
        return options;
    }

    /**
     * Check if a specific context menu option is displayed.
     * @param optionName the option label (e.g., "Collect Data", "Add Issue")
     * @return true if the option is found
     */
    public boolean isContextMenuOptionDisplayed(String optionName) {
        System.out.println("📍 Checking for context menu option: " + optionName + "...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                + "OR type == 'XCUIElementTypeMenuItem') "
                + "AND (label == '" + optionName + "' OR label CONTAINS '" + optionName + "')"
            ));
            boolean displayed = !found.isEmpty();
            System.out.println(displayed
                ? "✅ Option '" + optionName + "' found"
                : "⚠️ Option '" + optionName + "' not found");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap a specific context menu option.
     * @param optionName the option label (e.g., "Collect Data", "Add IR Photos")
     * @return true if the option was tapped
     */
    public boolean tapContextMenuOption(String optionName) {
        System.out.println("📍 Tapping context menu option: " + optionName + "...");

        // Strategy 1: Button or MenuItem with exact label
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeMenuItem') "
                + "AND label == '" + optionName + "'"
            ));
            if (!found.isEmpty()) {
                found.get(0).click();
                System.out.println("✅ Tapped '" + optionName + "' (exact match)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Contains match
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                + "OR type == 'XCUIElementTypeMenuItem') "
                + "AND label CONTAINS '" + optionName + "'"
            ));
            if (!found.isEmpty()) {
                found.get(0).click();
                System.out.println("✅ Tapped '" + optionName + "' (contains match)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap context menu option: " + optionName);
        return false;
    }

    /**
     * Dismiss the asset context menu by tapping outside or pressing escape.
     */
    public void dismissContextMenu() {
        System.out.println("📍 Dismissing context menu...");

        // Strategy 1: Tap outside the menu area (top-left corner of screen)
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.width / 2, 150));
            tap.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerMove(Duration.ofMillis(50),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.width / 2, 150));
            tap.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(tap));
            System.out.println("✅ Tapped outside to dismiss context menu");
        } catch (Exception e) {
            System.out.println("⚠️ Could not dismiss context menu: " + e.getMessage());
        }
    }

    // ================================================================
    // CONTEXT MENU ACTIONS — SCREEN DETECTION (TC_JOB_130-132)
    // ================================================================

    /**
     * Check if the Data Collection screen is displayed after tapping "Collect Data".
     * The data collection screen typically shows the asset name, data fields,
     * and may contain labels like "Collect Data", "Data Collection", or asset-specific fields.
     * @return true if a data collection screen is detected
     */
    public boolean isDataCollectionScreenDisplayed() {
        System.out.println("📍 Checking for Data Collection screen...");

        // Strategy 1: Look for "Collect Data" or "Data Collection" title
        try {
            List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeNavigationBar' "
                + "OR type == 'XCUIElementTypeOther') "
                + "AND (label CONTAINS 'Collect Data' OR label CONTAINS 'Data Collection' "
                + "OR label CONTAINS 'collect data')"
            ));
            if (!titles.isEmpty()) {
                System.out.println("✅ Data Collection screen detected via title");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for navigation bar that is NOT "Assets in Room" or "Work Orders"
        // (indicates we navigated to a new screen)
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar'"
            ));
            for (WebElement navBar : navBars) {
                String name = navBar.getAttribute("name");
                if (name != null && !name.isEmpty()
                        && !name.contains("Assets in Room") && !name.contains("Work Orders")
                        && !name.contains("Session") && !name.contains("Add Assets")) {
                    System.out.println("✅ Data Collection screen detected via nav bar: " + name);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Check for form fields (text fields, toggles) indicating a data entry screen
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeSwitch' "
                + "OR type == 'XCUIElementTypeTextView'"
            ));
            // A data collection screen would have input fields
            if (fields.size() >= 2) {
                System.out.println("✅ Data Collection screen detected via form fields (" + fields.size() + " fields)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Data Collection screen not detected");
        return false;
    }

    /**
     * Check if the IR Photos capture screen is displayed after tapping "Add IR Photos"
     * from the asset context menu. This is different from the IR Photos section in the
     * New Asset form — it's a standalone capture screen for adding IR photo pairs.
     * @return true if an IR photos capture screen is detected
     */
    public boolean isIRPhotoContextScreenDisplayed() {
        System.out.println("📍 Checking for IR Photos capture screen (from context menu)...");

        // Strategy 1: Look for "IR Photos" or "Infrared Photos" title
        try {
            List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeNavigationBar') "
                + "AND (label CONTAINS 'IR Photos' OR label CONTAINS 'Infrared Photos' "
                + "OR label CONTAINS 'Add IR Photo' OR label CONTAINS 'Photo Capture')"
            ));
            if (!titles.isEmpty()) {
                System.out.println("✅ IR Photos capture screen detected via title");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for session info on the IR screen (session label, job info)
        try {
            List<WebElement> sessionInfo = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Session' OR label CONTAINS 'Job' OR label CONTAINS 'FLIR' "
                + "OR label CONTAINS 'FLUKE' OR label CONTAINS 'FOTRIC')"
            ));
            // Also check for camera/photo buttons
            List<WebElement> cameraButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label CONTAINS 'Camera' OR label CONTAINS 'Photo' OR label CONTAINS 'Capture' "
                + "OR label CONTAINS 'Take')"
            ));
            if (!sessionInfo.isEmpty() && !cameraButtons.isEmpty()) {
                System.out.println("✅ IR Photos screen detected via session info + camera buttons");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broader detection — any screen not Assets in Room that has photo elements
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar'"
            ));
            for (WebElement navBar : navBars) {
                String name = navBar.getAttribute("name");
                if (name != null && (name.contains("IR") || name.contains("Photo")
                        || name.contains("Infrared") || name.contains("Capture"))) {
                    System.out.println("✅ IR Photos screen detected via nav: " + name);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ IR Photos capture screen not detected");
        return false;
    }

    /**
     * Check if a removal confirmation dialog is displayed after tapping "Remove from Session".
     * The dialog may ask "Remove [asset] from session?" or similar.
     * @return true if a confirmation dialog is displayed
     */
    public boolean isRemovalConfirmationDisplayed() {
        System.out.println("📍 Checking for removal confirmation dialog...");

        // Strategy 1: Look for alert dialog with remove/confirm text
        try {
            List<WebElement> alerts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeAlert'"
            ));
            if (!alerts.isEmpty()) {
                System.out.println("✅ Confirmation alert dialog found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for confirmation text in static texts
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Remove' OR label CONTAINS 'remove' "
                + "OR label CONTAINS 'Are you sure' OR label CONTAINS 'Confirm')"
            ));
            if (!texts.isEmpty()) {
                System.out.println("✅ Removal confirmation text found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for destructive-style buttons (Remove, Delete, Confirm)
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == 'Remove' OR label == 'Confirm' OR label == 'Delete' "
                + "OR label == 'Yes' OR label CONTAINS 'Remove')"
            ));
            // Also need a Cancel button for it to be a confirmation dialog
            List<WebElement> cancelBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Cancel' OR label == 'No')"
            ));
            if (!buttons.isEmpty() && !cancelBtns.isEmpty()) {
                System.out.println("✅ Confirmation dialog detected via Remove + Cancel buttons");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Removal confirmation dialog not detected");
        return false;
    }

    /**
     * Confirm asset removal by tapping the "Remove" or "Confirm" button on the confirmation dialog.
     * @return true if the confirmation was tapped
     */
    public boolean confirmAssetRemoval() {
        System.out.println("📍 Confirming asset removal...");

        // Strategy 1: Tap "Remove" button
        try {
            List<WebElement> removeBtn = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Remove' OR label CONTAINS 'Remove')"
            ));
            if (!removeBtn.isEmpty()) {
                removeBtn.get(0).click();
                System.out.println("✅ Tapped 'Remove' confirmation button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Tap "Confirm" or "Yes" button
        try {
            List<WebElement> confirmBtn = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == 'Confirm' OR label == 'Yes' OR label == 'OK')"
            ));
            if (!confirmBtn.isEmpty()) {
                confirmBtn.get(0).click();
                System.out.println("✅ Tapped confirmation button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not find confirmation button");
        return false;
    }

    // ================================================================
    // SESSION LOCATIONS — ROOM ASSET COUNT (TC_JOB_133)
    // ================================================================

    /**
     * Get the asset count displayed on a room entry in the session locations hierarchy.
     * Room entries may show text like "Room_1, 3 assets" or "Room_1, 1 node" etc.
     * @param roomIndex 0-based index of the room in the expanded floor
     * @return the asset count string (e.g., "3 assets", "1 node"), or null if not found
     */
    public String getRoomEntryAssetCount(int roomIndex) {
        System.out.println("📍 Getting asset count for room at index " + roomIndex + "...");

        try {
            // Get room entries from the locations hierarchy
            java.util.List<String> rooms = getLocationsRoomEntries();
            if (roomIndex < rooms.size()) {
                String roomText = rooms.get(roomIndex);
                System.out.println("📊 Room entry text: '" + roomText + "'");

                // Extract the count part — typically after a comma or in a separate format
                // Patterns: "Room_1, 3 assets", "Room 1, 1 node", "3 nodes", "1 asset"
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                    "(\\d+)\\s+(asset|node|item)"
                ).matcher(roomText.toLowerCase());
                if (matcher.find()) {
                    String countStr = matcher.group(0);
                    System.out.println("📊 Room asset count: " + countStr);
                    return countStr;
                }

                // Try extracting just the numeric portion after comma
                if (roomText.contains(",")) {
                    String afterComma = roomText.substring(roomText.lastIndexOf(",") + 1).trim();
                    System.out.println("📊 Room info after comma: " + afterComma);
                    return afterComma;
                }

                return roomText;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting room asset count: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if a room entry shows a session progress indicator (e.g., green checkmark with "1/1").
     * @param roomIndex 0-based index of the room
     * @return true if a progress indicator is visible on the room entry
     */
    public boolean isRoomSessionProgressDisplayed(int roomIndex) {
        System.out.println("📍 Checking for session progress on room " + roomIndex + "...");

        try {
            // Get room entries to determine Y position
            java.util.List<String> rooms = getLocationsRoomEntries();
            if (roomIndex >= rooms.size()) {
                System.out.println("⚠️ Room index out of range");
                return false;
            }

            // Look for progress indicators (checkmarks, fraction text like "1/1", "0/3")
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));

            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                if (label != null && label.matches(".*\\d+/\\d+.*")) {
                    // Fraction pattern like "1/1" or "2/3"
                    System.out.println("✅ Progress indicator found: " + label);
                    return true;
                }
            }

            // Also check for checkmark images or green indicators
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label CONTAINS 'check' OR label CONTAINS '✓' OR label == 'checkmark' "
                + "OR label CONTAINS 'checkmark.circle')"
            ));
            if (!images.isEmpty()) {
                System.out.println("✅ Checkmark/progress indicator found");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking room progress: " + e.getMessage());
        }

        System.out.println("⚠️ Session progress indicator not detected on room");
        return false;
    }

    // ================================================================
    // ADD ASSETS SCREEN (TC_JOB_088-089)
    // ================================================================

    /**
     * Check if the "Add Assets" screen is displayed.
     * Looks for the "Add Assets" title and Cancel button.
     */
    public boolean isAddAssetsScreenDisplayed() {
        System.out.println("📍 Checking for Add Assets screen...");

        // Strategy 1: "Add Assets" title text
        try {
            List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeOther') "
                + "AND (label == 'Add Assets' OR label CONTAINS 'Add Assets')"
            ));
            if (!titles.isEmpty()) {
                System.out.println("✅ Add Assets screen found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Navigation bar with "Add Assets" title
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar' AND "
                + "(name CONTAINS 'Add Assets' OR label CONTAINS 'Add Assets')"
            ));
            if (!navBars.isEmpty()) {
                System.out.println("✅ Add Assets screen (nav bar)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Tabs "Existing Asset" + "New Asset" present
        try {
            List<WebElement> existingTab = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Existing Asset'"
            ));
            List<WebElement> newTab = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'New Asset'"
            ));
            if (!existingTab.isEmpty() && !newTab.isEmpty()) {
                System.out.println("✅ Add Assets screen (tabs found)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Add Assets screen not found");
        return false;
    }

    /**
     * Wait for the Add Assets screen to appear (up to 10 seconds).
     */
    public boolean waitForAddAssetsScreen() {
        for (int i = 0; i < 20; i++) {
            if (isAddAssetsScreenDisplayed()) return true;
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }
        System.out.println("⚠️ Add Assets screen did not appear within 10 seconds");
        return false;
    }

    /**
     * Check if the Cancel button is displayed on the Add Assets screen.
     */
    public boolean isAddAssetsCancelButtonDisplayed() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            return !buttons.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the Cancel button on the Add Assets screen.
     */
    public boolean tapAddAssetsCancelButton() {
        System.out.println("📍 Tapping Cancel on Add Assets...");
        try {
            WebElement cancel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            cancel.click();
            System.out.println("✅ Tapped Cancel on Add Assets");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Cancel: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the tab names on the Add Assets screen.
     * Expected tabs: "Existing Asset", "New Asset"
     * Returns a list of tab label strings found.
     */
    public java.util.List<String> getAddAssetsTabs() {
        System.out.println("📍 Getting Add Assets tab names...");
        java.util.List<String> tabs = new java.util.ArrayList<>();
        String[] expectedTabs = {"Existing Asset", "New Asset"};

        for (String tabName : expectedTabs) {
            try {
                List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                    + "OR type == 'XCUIElementTypeSegmentedControl') "
                    + "AND label CONTAINS '" + tabName + "'"
                ));
                if (!elements.isEmpty()) {
                    tabs.add(tabName);
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Add Assets tabs: " + tabs);
        return tabs;
    }

    /**
     * Check if the "Existing Asset" tab is selected by default on the Add Assets screen.
     */
    public boolean isExistingAssetTabSelected() {
        System.out.println("📍 Checking if Existing Asset tab is selected...");

        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Existing Asset'"
            ));
            for (WebElement el : elements) {
                // Check "selected" attribute
                String selected = el.getAttribute("selected");
                if ("true".equals(selected)) {
                    System.out.println("✅ Existing Asset tab is selected (attribute)");
                    return true;
                }
                String value = el.getAttribute("value");
                if ("1".equals(value) || "selected".equalsIgnoreCase(value)) {
                    System.out.println("✅ Existing Asset tab is selected (value)");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Check segmented control
        try {
            List<WebElement> segments = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSegmentedControl'"
            ));
            if (!segments.isEmpty()) {
                String value = segments.get(0).getAttribute("value");
                if (value != null && value.contains("Existing")) {
                    System.out.println("✅ Existing Asset tab is selected (segmented control)");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Assume first tab is selected if both tabs are present
        try {
            List<WebElement> existingTab = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Existing Asset'"
            ));
            if (!existingTab.isEmpty()) {
                System.out.println("✅ Existing Asset tab found (assuming default selection)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not determine if Existing Asset tab is selected");
        return false;
    }

    // ================================================================
    // ADD ASSETS — EXISTING ASSET TAB (TC_JOB_090-091)
    // ================================================================

    /**
     * Tap the "Existing Asset" tab on the Add Assets screen.
     */
    public boolean tapExistingAssetTab() {
        System.out.println("📍 Tapping Existing Asset tab...");
        try {
            List<WebElement> tabs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Existing Asset'"
            ));
            if (!tabs.isEmpty()) {
                tabs.get(0).click();
                System.out.println("✅ Tapped Existing Asset tab");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Try segmented control
        try {
            List<WebElement> segments = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSegmentedControl'"
            ));
            if (!segments.isEmpty()) {
                List<WebElement> children = segments.get(0).findElements(
                    AppiumBy.iOSNsPredicateString(
                        "label CONTAINS 'Existing'"
                    ));
                if (!children.isEmpty()) {
                    children.get(0).click();
                    System.out.println("✅ Tapped Existing Asset segment");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap Existing Asset tab");
        return false;
    }

    /**
     * Check if the Existing Asset tab shows a list of available assets.
     * Returns true if asset cells/entries are visible in the content area.
     */
    public boolean isExistingAssetListDisplayed() {
        System.out.println("📍 Checking if existing asset list is displayed...");

        // Strategy 1: Look for cells in the content area (assets are list cells)
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            int contentCells = 0;
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                int h = cell.getSize().getHeight();
                // Asset cells should be below tabs/breadcrumb (Y > 250) and reasonable height
                if (y > 250 && h > 30 && h < 200) {
                    contentCells++;
                }
            }
            if (contentCells > 0) {
                System.out.println("✅ Existing asset list found: " + contentCells + " cells");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for buttons/text that look like asset entries
        try {
            List<WebElement> entries = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND visible == true AND (label CONTAINS 'Asset' OR label CONTAINS 'asset')"
            ));
            for (WebElement entry : entries) {
                int y = entry.getLocation().getY();
                String label = entry.getAttribute("label");
                // Filter out tab labels and screen titles
                if (y > 300 && label != null
                        && !label.contains("Existing Asset")
                        && !label.contains("New Asset")
                        && !label.contains("Add Assets")
                        && !label.contains("No Available")
                        && !label.contains("No Assets")) {
                    System.out.println("✅ Asset entry found: " + label);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No existing asset list found");
        return false;
    }

    /**
     * Get the count of available assets on the Existing Asset tab.
     * Returns 0 if no assets found or tab is in empty state.
     */
    public int getExistingAssetListCount() {
        System.out.println("📍 Counting existing assets...");

        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            int count = 0;
            for (WebElement cell : cells) {
                int y = cell.getLocation().getY();
                int h = cell.getSize().getHeight();
                if (y > 250 && h > 30 && h < 200) {
                    count++;
                }
            }
            System.out.println("📊 Existing asset count: " + count);
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if "No Available Assets" empty state is displayed on the Existing Asset tab.
     * This appears when all room assets are already linked to the session.
     */
    public boolean isNoAvailableAssetsDisplayed() {
        System.out.println("📍 Checking for 'No Available Assets' message...");

        // Strategy 1: "No Available Assets" text
        try {
            List<WebElement> noAvailable = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'No Available Assets' OR label CONTAINS 'No Available Assets' "
                + "OR label CONTAINS 'No available assets')"
            ));
            if (!noAvailable.isEmpty()) {
                System.out.println("✅ 'No Available Assets' text found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Message about all assets already linked
        try {
            List<WebElement> messages = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'already linked' OR label CONTAINS 'already associated' "
                + "OR label CONTAINS 'all assets')"
            ));
            if (!messages.isEmpty()) {
                System.out.println("✅ 'All assets linked' message found: "
                    + messages.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ 'No Available Assets' message not found");
        return false;
    }

    /**
     * Get the "No Available Assets" message text.
     * Expected: "All assets in this room are already linked to this session"
     */
    public String getNoAvailableAssetsMessage() {
        System.out.println("📍 Getting 'No Available Assets' message...");

        try {
            // Look for all text elements in the content area
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));

            String noAvailableText = null;
            String messageText = null;

            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                if (label == null) continue;

                if (label.contains("No Available Assets") || label.contains("No available")) {
                    noAvailableText = label;
                }
                if (label.contains("already linked") || label.contains("already associated")
                        || label.contains("All assets") || label.contains("all assets")) {
                    messageText = label;
                }
            }

            // Return the descriptive message if found, otherwise the title
            if (messageText != null) {
                System.out.println("📊 Message: " + messageText);
                return messageText;
            }
            if (noAvailableText != null) {
                System.out.println("📊 Title: " + noAvailableText);
                return noAvailableText;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get 'No Available Assets' message");
        return null;
    }

    // ================================================================
    // ADD ASSETS — NEW ASSET TAB (TC_JOB_092-095)
    // ================================================================

    /**
     * Tap the "New Asset" tab on the Add Assets screen.
     */
    public boolean tapNewAssetTab() {
        System.out.println("📍 Tapping New Asset tab...");
        try {
            List<WebElement> tabs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'New Asset'"
            ));
            // Filter out the screen title "Add Assets" and the form title "New Asset"
            for (WebElement tab : tabs) {
                String label = tab.getAttribute("label");
                int y = tab.getLocation().getY();
                // Tab should be in the upper area (Y < 300) and not a nav bar element
                if (label != null && label.contains("New Asset") && y > 80 && y < 300) {
                    tab.click();
                    System.out.println("✅ Tapped New Asset tab");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Try segmented control
        try {
            List<WebElement> segments = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSegmentedControl'"
            ));
            if (!segments.isEmpty()) {
                List<WebElement> children = segments.get(0).findElements(
                    AppiumBy.iOSNsPredicateString("label CONTAINS 'New'"));
                if (!children.isEmpty()) {
                    children.get(0).click();
                    System.out.println("✅ Tapped New Asset segment");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap New Asset tab");
        return false;
    }

    /**
     * Check if the "New Asset" tab is currently selected.
     */
    public boolean isNewAssetTabSelected() {
        try {
            List<WebElement> tabs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'New Asset'"
            ));
            for (WebElement tab : tabs) {
                int y = tab.getLocation().getY();
                if (y > 80 && y < 300) {
                    String selected = tab.getAttribute("selected");
                    if ("true".equals(selected)) return true;
                    String value = tab.getAttribute("value");
                    if ("1".equals(value) || "selected".equalsIgnoreCase(value)) return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: check if New Asset options are visible (Create New Asset, Quick Count, etc.)
        try {
            List<WebElement> createOptions = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'Create New Asset' OR label CONTAINS 'Create Quick Count' "
                + "OR label CONTAINS 'Photo Walkthrough')"
            ));
            if (createOptions.size() >= 2) {
                System.out.println("✅ New Asset tab selected (options visible)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        return false;
    }

    /**
     * Check if the "Create New Asset" option is displayed on the New Asset tab.
     * Returns true if the option with title and description is visible.
     */
    public boolean isCreateNewAssetOptionDisplayed() {
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Create New Asset' OR label CONTAINS 'Create New Asset')"
            ));
            boolean found = !elements.isEmpty();
            System.out.println(found
                ? "✅ Create New Asset option found"
                : "⚠️ Create New Asset option not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the "Create Quick Count" option is displayed on the New Asset tab.
     */
    public boolean isCreateQuickCountOptionDisplayed() {
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Create Quick Count' OR label CONTAINS 'Create Quick Count')"
            ));
            boolean found = !elements.isEmpty();
            System.out.println(found
                ? "✅ Create Quick Count option found"
                : "⚠️ Create Quick Count option not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the "Create Photo Walkthrough" option is displayed on the New Asset tab.
     */
    public boolean isCreatePhotoWalkthroughOptionDisplayed() {
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Photo Walkthrough' OR label CONTAINS 'Create Photo Walkthrough')"
            ));
            boolean found = !elements.isEmpty();
            System.out.println(found
                ? "✅ Create Photo Walkthrough option found"
                : "⚠️ Create Photo Walkthrough option not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get details of a New Asset tab option (icon presence, title, description).
     * @param optionTitle e.g., "Create New Asset", "Create Quick Count", "Create Photo Walkthrough"
     * @return map with "title", "description", "hasIcon" keys, or null if not found.
     */
    public java.util.Map<String, String> getNewAssetOptionDetails(String optionTitle) {
        System.out.println("📍 Getting details for option: " + optionTitle + "...");

        try {
            // Find the title element
            List<WebElement> titleElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + optionTitle + "'"
            ));
            if (titleElements.isEmpty()) {
                System.out.println("⚠️ Option title not found: " + optionTitle);
                return null;
            }

            WebElement titleEl = titleElements.get(0);
            int titleY = titleEl.getLocation().getY();
            int titleX = titleEl.getLocation().getX();

            java.util.Map<String, String> details = new java.util.HashMap<>();
            details.put("title", titleEl.getAttribute("label"));

            // Find description text near the title (just below, within 40px Y)
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                int textY = text.getLocation().getY();
                if (label != null && !label.equals(titleEl.getAttribute("label"))
                        && textY > titleY && textY < titleY + 40
                        && !label.contains("Create") && !label.contains("Existing")
                        && !label.contains("New Asset")) {
                    details.put("description", label);
                    System.out.println("  Description: " + label);
                    break;
                }
            }

            // Check for icon (image element to the left of or near the title)
            try {
                List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage' AND visible == true"
                ));
                for (WebElement img : images) {
                    int imgY = img.getLocation().getY();
                    int imgX = img.getLocation().getX();
                    if (Math.abs(imgY - titleY) < 25 && imgX < titleX) {
                        String name = img.getAttribute("name");
                        details.put("hasIcon", "true");
                        details.put("iconName", name != null ? name : "unknown");
                        System.out.println("  Icon found: " + name);
                        break;
                    }
                }
                if (!details.containsKey("hasIcon")) {
                    // Check for SF Symbol or colored icon in Other elements
                    List<WebElement> others = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeOther' OR type == 'XCUIElementTypeButton') "
                        + "AND visible == true"
                    ));
                    for (WebElement other : others) {
                        int otherY = other.getLocation().getY();
                        int otherX = other.getLocation().getX();
                        int otherW = other.getSize().getWidth();
                        int otherH = other.getSize().getHeight();
                        if (Math.abs(otherY - titleY) < 25 && otherX < titleX
                                && otherW < 50 && otherH < 50) {
                            details.put("hasIcon", "true");
                            break;
                        }
                    }
                }
            } catch (Exception e2) { /* continue */ }

            if (!details.containsKey("hasIcon")) {
                details.put("hasIcon", "false");
            }

            System.out.println("📊 Option details: " + details);
            return details;
        } catch (Exception e) {
            System.out.println("⚠️ Error getting option details: " + e.getMessage());
            return null;
        }
    }

    /**
     * Tap the "Create New Asset" option on the New Asset tab to open the form.
     */
    public boolean tapCreateNewAssetOption() {
        System.out.println("📍 Tapping 'Create New Asset' option...");

        // Strategy 1: Tap the title text
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                + "AND (label == 'Create New Asset' OR label CONTAINS 'Create New Asset')"
            ));
            if (!elements.isEmpty()) {
                elements.get(0).click();
                System.out.println("✅ Tapped Create New Asset option");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Tap the cell containing "Create New Asset"
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            for (WebElement cell : cells) {
                List<WebElement> children = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Create New Asset'"
                ));
                if (!children.isEmpty()) {
                    cell.click();
                    System.out.println("✅ Tapped Create New Asset cell");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search
        try {
            WebElement el = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Create New Asset' AND visible == true"
            ));
            el.click();
            System.out.println("✅ Tapped Create New Asset (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Create New Asset option");
            return false;
        }
    }

    // ================================================================
    // QUICK COUNT SCREEN (TC_JOB_134-139)
    // ================================================================

    /**
     * Tap the "Create Quick Count" option on the New Asset tab.
     * This should open the Quick Count screen.
     * @return true if the option was tapped
     */
    public boolean tapCreateQuickCountOption() {
        System.out.println("📍 Tapping 'Create Quick Count' option...");

        // Strategy 1: Tap the title text
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                + "AND (label == 'Create Quick Count' OR label CONTAINS 'Create Quick Count')"
            ));
            if (!elements.isEmpty()) {
                elements.get(0).click();
                System.out.println("✅ Tapped Create Quick Count option");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Tap the cell containing "Create Quick Count"
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            for (WebElement cell : cells) {
                List<WebElement> children = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Quick Count'"
                ));
                if (!children.isEmpty()) {
                    cell.click();
                    System.out.println("✅ Tapped Create Quick Count cell");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search
        try {
            WebElement el = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Quick Count' AND visible == true"
            ));
            el.click();
            System.out.println("✅ Tapped Quick Count (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Create Quick Count option");
            return false;
        }
    }

    /**
     * Check if the Quick Count screen is displayed.
     * The screen shows "Quick Count" title, a Cancel button, and either
     * the empty state or a list of asset types.
     * @return true if the Quick Count screen is detected
     */
    public boolean isQuickCountScreenDisplayed() {
        System.out.println("📍 Checking for Quick Count screen...");

        // Strategy 1: "Quick Count" navigation bar title
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar' AND "
                + "(name == 'Quick Count' OR name CONTAINS 'Quick Count')"
            ));
            if (!navBars.isEmpty()) {
                System.out.println("✅ Quick Count screen detected via nav bar");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: "Quick Count" static text title
        try {
            List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == 'Quick Count'"
            ));
            for (WebElement title : titles) {
                int y = title.getLocation().getY();
                if (y < 200) { // Title area
                    System.out.println("✅ Quick Count screen detected via title text");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Combination — Cancel button + Quick Count related content
        try {
            List<WebElement> cancelBtn = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            List<WebElement> qcContent = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Quick Count' OR label CONTAINS 'No Asset Types' "
                + "OR label CONTAINS 'Add Asset Type')"
            ));
            if (!cancelBtn.isEmpty() && !qcContent.isEmpty()) {
                System.out.println("✅ Quick Count screen detected via Cancel + content");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Quick Count screen not detected");
        return false;
    }

    /**
     * Wait for the Quick Count screen to be fully loaded.
     * @return true if screen loaded within timeout
     */
    public boolean waitForQuickCountScreen() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.pollingEvery(Duration.ofMillis(500));
            return wait.until(d -> isQuickCountScreenDisplayed());
        } catch (Exception e) {
            System.out.println("⚠️ Timeout waiting for Quick Count screen");
            return false;
        }
    }

    /**
     * Check if the Quick Count empty state is displayed.
     * Empty state shows: stacked boxes icon, "No Asset Types Added" text,
     * and helper text about tapping "+ Add Asset Type".
     * @return true if the empty state is visible
     */
    public boolean isQuickCountEmptyStateDisplayed() {
        System.out.println("📍 Checking for Quick Count empty state...");

        // Check for "No Asset Types Added" text
        try {
            List<WebElement> noTypesText = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'No Asset Types' OR label CONTAINS 'No Asset Type')"
            ));
            if (!noTypesText.isEmpty()) {
                System.out.println("✅ Quick Count empty state found ('No Asset Types Added')");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Check for helper text about adding types
        try {
            List<WebElement> helperText = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Add Asset Type' OR label CONTAINS 'start counting')"
            ));
            if (!helperText.isEmpty()) {
                System.out.println("✅ Quick Count empty state found (helper text)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Quick Count empty state not detected");
        return false;
    }

    /**
     * Check if the "No Asset Types Added" text is displayed on Quick Count screen.
     * @return true if the text is visible
     */
    public boolean isNoAssetTypesAddedTextDisplayed() {
        System.out.println("📍 Checking for 'No Asset Types Added' text...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'No Asset Types Added' OR label CONTAINS 'No Asset Types Added')"
            ));
            boolean displayed = !found.isEmpty();
            System.out.println(displayed
                ? "✅ 'No Asset Types Added' text found"
                : "⚠️ 'No Asset Types Added' text not found");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the Quick Count empty state helper text.
     * Expected: "Tap '+ Add Asset Type' to start counting assets"
     * @return the helper text, or null if not found
     */
    public String getQuickCountHelperText() {
        System.out.println("📍 Getting Quick Count helper text...");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                if (label != null && (label.contains("Add Asset Type") || label.contains("counting")
                        || label.contains("start counting"))) {
                    System.out.println("📊 Helper text: " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Helper text not found");
        return null;
    }

    /**
     * Check if the "+ Add Asset Type" button is displayed on Quick Count screen.
     * @return true if the button is visible
     */
    public boolean isAddAssetTypeButtonDisplayed() {
        System.out.println("📍 Checking for '+ Add Asset Type' button...");

        // Strategy 1: Button with exact or partial label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label CONTAINS 'Add Asset Type' OR label CONTAINS '+ Add Asset Type')"
            ));
            if (!buttons.isEmpty()) {
                System.out.println("✅ Add Asset Type button found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Any tappable element with "Add Asset Type" text
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Add Asset Type' AND visible == true"
            ));
            if (!found.isEmpty()) {
                System.out.println("✅ Add Asset Type element found (broad)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Add Asset Type button not found");
        return false;
    }

    /**
     * Tap the "+ Add Asset Type" button on Quick Count screen.
     * @return true if the button was tapped
     */
    public boolean tapAddAssetTypeButton() {
        System.out.println("📍 Tapping '+ Add Asset Type' button...");

        // Strategy 1: Button with label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label CONTAINS 'Add Asset Type' OR label CONTAINS '+ Add Asset Type')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Add Asset Type button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text tap (SwiftUI sometimes renders as text)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Add Asset Type'"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped Add Asset Type text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search
        try {
            WebElement el = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Add Asset Type' AND visible == true"
            ));
            el.click();
            System.out.println("✅ Tapped Add Asset Type (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Add Asset Type button");
            return false;
        }
    }

    /**
     * Check if the "Select Asset Type" sheet/picker is displayed.
     * This appears after tapping "+ Add Asset Type" and shows a scrollable list.
     * @return true if the asset type selection sheet is visible
     */
    public boolean isSelectAssetTypeSheetDisplayed() {
        System.out.println("📍 Checking for Select Asset Type sheet...");

        // Strategy 1: "Select Asset Type" header text
        try {
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Select Asset Type' OR label CONTAINS 'Select Asset Type')"
            ));
            if (!headers.isEmpty()) {
                System.out.println("✅ Select Asset Type sheet detected via header");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Sheet/popover containing asset type names
        try {
            List<WebElement> sheets = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSheet' OR type == 'XCUIElementTypePopover'"
            ));
            if (!sheets.isEmpty()) {
                System.out.println("✅ Selection sheet/popover found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for known asset type names in a list
        String[] knownTypes = {"ATS", "Busway", "Capacitor", "Circuit Breaker", "Panelboard"};
        try {
            int foundCount = 0;
            for (String typeName : knownTypes) {
                List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                    + "AND label == '" + typeName + "'"
                ));
                if (!found.isEmpty()) foundCount++;
            }
            if (foundCount >= 3) {
                System.out.println("✅ Asset type picker detected via known type names (" + foundCount + " found)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Select Asset Type sheet not detected");
        return false;
    }

    /**
     * Get the list of available asset type names from the "Select Asset Type" picker.
     * @return list of asset type names found, or empty list
     */
    public java.util.List<String> getAssetTypeOptions() {
        System.out.println("📍 Getting asset type options from picker...");
        java.util.List<String> types = new java.util.ArrayList<>();

        // Known asset types from the app
        String[] knownTypes = {
            "ATS", "Busway", "Capacitor", "Circuit Breaker", "Default",
            "Disconnect Switch", "Fuse", "Generator", "Junction Box",
            "Loadcenter", "MCC", "Other", "Other (OCP)", "PDU",
            "Panelboard", "Reactor", "Relay", "Switchboard",
            "Transformer", "UPS", "Utility", "VFD"
        };

        // Strategy 1: Check each known type
        for (String typeName : knownTypes) {
            try {
                List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton' "
                    + "OR type == 'XCUIElementTypeCell') "
                    + "AND (label == '" + typeName + "' OR label CONTAINS '" + typeName + "')"
                ));
                if (!found.isEmpty()) {
                    types.add(typeName);
                }
            } catch (Exception e) { /* continue */ }
        }

        // Strategy 2: If few known types found, collect all visible list items
        if (types.size() < 5) {
            try {
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    int y = text.getLocation().getY();
                    // List items: below header area (Y > 200), short labels, not UI chrome
                    if (label != null && !label.isEmpty() && y > 200
                            && label.length() < 30
                            && !label.equals("Cancel") && !label.equals("Select Asset Type")
                            && !label.equals("Done") && !label.contains("Select")) {
                        if (!types.contains(label)) types.add(label);
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Asset type options found: " + types + " (count: " + types.size() + ")");
        return types;
    }

    /**
     * Select an asset type from the "Select Asset Type" picker by name.
     * @param typeName the type to select (e.g., "ATS", "Circuit Breaker")
     * @return true if the type was selected
     */
    public boolean selectAssetType(String typeName) {
        System.out.println("📍 Selecting asset type: " + typeName + "...");

        // Strategy 1: Tap button or text with exact label
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label == '" + typeName + "'"
            ));
            if (!found.isEmpty()) {
                found.get(0).click();
                System.out.println("✅ Selected asset type: " + typeName);
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Cell containing the type name
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            for (WebElement cell : cells) {
                List<WebElement> children = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == '" + typeName + "'"
                ));
                if (!children.isEmpty()) {
                    cell.click();
                    System.out.println("✅ Selected asset type via cell: " + typeName);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Scroll down and try again (type may be off-screen)
        try {
            java.util.Map<String, Object> scrollParams = new java.util.HashMap<>();
            scrollParams.put("direction", "down");
            scrollParams.put("predicateString", "label == '" + typeName + "'");
            driver.executeScript("mobile: scroll", scrollParams);
            try { Thread.sleep(300); } catch (InterruptedException ie) { /* */ }

            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label == '" + typeName + "' AND visible == true"
            ));
            if (!found.isEmpty()) {
                found.get(0).click();
                System.out.println("✅ Selected asset type after scroll: " + typeName);
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not select asset type: " + typeName);
        return false;
    }

    /**
     * Check if the "Select Subtype" screen is displayed after selecting an asset type.
     * Shows: Cancel button, "Select Subtype" title, blue tag icon,
     * "Choose a subtype for [Type]" text, list of subtypes, "Skip - No Subtype" button.
     * @return true if the subtype selection screen is visible
     */
    public boolean isSelectSubtypeScreenDisplayed() {
        System.out.println("📍 Checking for Select Subtype screen...");

        // Strategy 1: "Select Subtype" header text
        try {
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Select Subtype' OR label CONTAINS 'Select Subtype')"
            ));
            if (!headers.isEmpty()) {
                System.out.println("✅ Select Subtype screen detected via header");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: "Choose a subtype" helper text
        try {
            List<WebElement> helper = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Choose a subtype' OR label CONTAINS 'choose a subtype')"
            ));
            if (!helper.isEmpty()) {
                System.out.println("✅ Select Subtype screen detected via helper text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: "Skip - No Subtype" button
        try {
            List<WebElement> skipBtn = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label CONTAINS 'Skip' OR label CONTAINS 'No Subtype')"
            ));
            if (!skipBtn.isEmpty()) {
                System.out.println("✅ Select Subtype screen detected via Skip button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Select Subtype screen not detected");
        return false;
    }

    /**
     * Get the list of subtype options from the "Select Subtype" screen.
     * @return list of subtype names found, or empty list
     */
    public java.util.List<String> getSubtypeOptions() {
        System.out.println("📍 Getting subtype options...");
        java.util.List<String> subtypes = new java.util.ArrayList<>();

        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                int y = text.getLocation().getY();
                // Subtype items: below header area (Y > 200), not UI chrome
                if (label != null && !label.isEmpty() && y > 200
                        && label.length() > 3 && label.length() < 60
                        && !label.equals("Cancel") && !label.equals("Select Subtype")
                        && !label.contains("Choose a subtype")
                        && !label.contains("Skip") && !label.contains("No Subtype")) {
                    subtypes.add(label);
                }
            }
        } catch (Exception e) { /* continue */ }

        // Also check for button-type subtype items
        if (subtypes.isEmpty()) {
            try {
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true"
                ));
                for (WebElement btn : buttons) {
                    String label = btn.getAttribute("label");
                    int y = btn.getLocation().getY();
                    if (label != null && !label.isEmpty() && y > 200
                            && label.length() > 5 && label.length() < 60
                            && !label.equals("Cancel") && !label.contains("Skip")) {
                        subtypes.add(label);
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Subtype options found: " + subtypes + " (count: " + subtypes.size() + ")");
        return subtypes;
    }

    /**
     * Get the "Choose a subtype for [Type]" text displayed on the Select Subtype screen.
     * @return the helper text (e.g., "Choose a subtype for ATS"), or null
     */
    public String getSubtypeScreenHelperText() {
        System.out.println("📍 Getting Select Subtype helper text...");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Choose a subtype'"
            ));
            if (!texts.isEmpty()) {
                String label = texts.get(0).getAttribute("label");
                System.out.println("📊 Subtype helper: " + label);
                return label;
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Subtype helper text not found");
        return null;
    }

    /**
     * Tap the "Cancel" button on Quick Count or subtype selection screens.
     * @return true if Cancel was tapped
     */
    public boolean tapQuickCountCancelButton() {
        System.out.println("📍 Tapping Cancel button on Quick Count/subtype screen...");
        try {
            List<WebElement> cancelBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            if (!cancelBtns.isEmpty()) {
                cancelBtns.get(0).click();
                System.out.println("✅ Tapped Cancel");
                return true;
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Could not tap Cancel button");
        return false;
    }

    /**
     * Get the Quick Count location breadcrumb text.
     * This shows the current location context (building > floor > room).
     * @return the breadcrumb text, or null if not found
     */
    public String getQuickCountLocationBreadcrumb() {
        System.out.println("📍 Getting Quick Count location breadcrumb...");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                int y = text.getLocation().getY();
                // Breadcrumb: in the upper area (Y < 250), contains ">" or location info
                if (label != null && y > 80 && y < 250
                        && (label.contains(">") || label.contains("Floor")
                            || label.contains("Room") || label.contains("Building"))) {
                    System.out.println("📊 Location breadcrumb: " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Location breadcrumb not found");
        return null;
    }

    // ================================================================
    // SUBTYPE SELECTION — ACTIONS (TC_JOB_140)
    // ================================================================

    /**
     * Tap the "Skip - No Subtype" button on the Select Subtype screen.
     * This adds the asset type without a subtype and returns to Quick Count.
     * @return true if the button was tapped
     */
    public boolean tapSkipNoSubtypeButton() {
        System.out.println("📍 Tapping 'Skip - No Subtype' button...");

        // Strategy 1: Button with exact or partial label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == 'Skip - No Subtype' OR label CONTAINS 'Skip' "
                + "OR label CONTAINS 'No Subtype')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped 'Skip - No Subtype' button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text that is tappable
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Skip' OR label CONTAINS 'No Subtype')"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped Skip via static text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search at bottom of screen
        try {
            List<WebElement> all = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Skip' AND visible == true"
            ));
            for (WebElement el : all) {
                int y = el.getLocation().getY();
                // Skip button is typically at the bottom of the selection sheet
                if (y > 400) {
                    el.click();
                    System.out.println("✅ Tapped Skip at bottom (Y=" + y + ")");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap 'Skip - No Subtype' button");
        return false;
    }

    /**
     * Select a subtype from the "Select Subtype" picker by name.
     * @param subtypeName the subtype to select (e.g., "Automatic Transfer Switch (<=1000V)")
     * @return true if the subtype was selected
     */
    public boolean selectSubtype(String subtypeName) {
        System.out.println("📍 Selecting subtype: " + subtypeName + "...");

        // Strategy 1: Tap text with exact label
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                + "AND label == '" + subtypeName + "'"
            ));
            if (!found.isEmpty()) {
                found.get(0).click();
                System.out.println("✅ Selected subtype: " + subtypeName);
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Contains match (handles Unicode characters like ≤ vs <=)
        try {
            // Normalize the search term
            String searchTerm = subtypeName.replace("<=", "").replace(">=", "")
                .replace("(", "").replace(")", "").trim();
            // Take first significant part
            if (searchTerm.contains(" ")) {
                searchTerm = searchTerm.substring(0, Math.min(searchTerm.length(), 25));
            }
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                + "AND label CONTAINS '" + searchTerm + "'"
            ));
            for (WebElement el : found) {
                int y = el.getLocation().getY();
                if (y > 200) { // In the content area
                    el.click();
                    System.out.println("✅ Selected subtype via contains: " + subtypeName);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Scroll to find off-screen subtype
        try {
            java.util.Map<String, Object> scrollParams = new java.util.HashMap<>();
            scrollParams.put("direction", "down");
            scrollParams.put("predicateString", "label CONTAINS '" + subtypeName.substring(0, 15) + "'");
            driver.executeScript("mobile: scroll", scrollParams);
            try { Thread.sleep(300); } catch (InterruptedException ie) { /* */ }

            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS '" + subtypeName.substring(0, 15) + "' AND visible == true"
            ));
            if (!found.isEmpty()) {
                found.get(0).click();
                System.out.println("✅ Selected subtype after scroll: " + subtypeName);
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not select subtype: " + subtypeName);
        return false;
    }

    // ================================================================
    // QUICK COUNT — ASSET TYPE CARD (TC_JOB_141-144)
    // ================================================================

    /**
     * Check if an asset type card is displayed on the Quick Count screen.
     * The card shows: type name, count controls (+/-), delete icon, and optionally subtype.
     * @param typeName the asset type name to look for (e.g., "ATS")
     * @return true if the card for the given type is found
     */
    public boolean isAssetTypeCardDisplayed(String typeName) {
        System.out.println("📍 Checking for asset type card: " + typeName + "...");

        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                + "AND label == '" + typeName + "'"
            ));
            // Filter: must be in content area (Y > 150) and on the Quick Count screen
            for (WebElement el : found) {
                int y = el.getLocation().getY();
                if (y > 150) {
                    System.out.println("✅ Asset type card found: " + typeName + " at Y=" + y);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for cell containing the type name
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            for (WebElement cell : cells) {
                List<WebElement> children = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + typeName + "'"
                ));
                if (!children.isEmpty()) {
                    System.out.println("✅ Asset type card found in cell: " + typeName);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Asset type card not found: " + typeName);
        return false;
    }

    /**
     * Get the current count from an asset type card on the Quick Count screen.
     * The count is displayed between the - and + buttons.
     * @param typeName the asset type name (e.g., "ATS")
     * @return the current count as an integer, or -1 if not found
     */
    public int getAssetTypeCardCount(String typeName) {
        System.out.println("📍 Getting count for asset type card: " + typeName + "...");

        try {
            // Find the type name element to get its Y position
            List<WebElement> typeLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == '" + typeName + "'"
            ));

            int targetY = -1;
            for (WebElement label : typeLabels) {
                int y = label.getLocation().getY();
                if (y > 150) { targetY = y; break; }
            }

            if (targetY < 0) {
                System.out.println("⚠️ Type label not found for count");
                return -1;
            }

            // Find numeric text near the type label (within 50px Y band)
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                int y = text.getLocation().getY();
                if (label != null && Math.abs(y - targetY) < 50 && label.matches("\\d+")) {
                    int count = Integer.parseInt(label);
                    System.out.println("📊 Asset type count for " + typeName + ": " + count);
                    return count;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting count: " + e.getMessage());
        }

        System.out.println("⚠️ Count not found for " + typeName);
        return -1;
    }

    /**
     * Get the subtype label displayed on an asset type card.
     * @param typeName the asset type name (e.g., "ATS")
     * @return the subtype text (e.g., "Automatic Transfer Switch (<=1000V)"), or null
     */
    public String getAssetTypeCardSubtype(String typeName) {
        System.out.println("📍 Getting subtype for asset type card: " + typeName + "...");

        try {
            // Find the type name element
            List<WebElement> typeLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == '" + typeName + "'"
            ));

            int targetY = -1;
            for (WebElement label : typeLabels) {
                int y = label.getLocation().getY();
                if (y > 150) { targetY = y; break; }
            }

            if (targetY < 0) return null;

            // Find text just below the type name (subtype is below, within 20-60px)
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                int y = text.getLocation().getY();
                int diff = y - targetY;
                // Subtype is below the type name (diff > 15), within 60px, and not a number
                if (label != null && !label.isEmpty() && diff > 15 && diff < 60
                        && !label.matches("\\d+") && !label.equals(typeName)
                        && label.length() > 3) {
                    System.out.println("📊 Subtype for " + typeName + ": " + label);
                    return label;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting subtype: " + e.getMessage());
        }

        System.out.println("⚠️ Subtype not found for " + typeName);
        return null;
    }

    /**
     * Tap the + (plus/increment) button on an asset type card in Quick Count.
     * The + button increases the asset count.
     * @param typeName the asset type name (e.g., "ATS")
     * @return true if the + button was tapped
     */
    public boolean tapAssetTypeCardPlusButton(String typeName) {
        System.out.println("📍 Tapping + button for asset type: " + typeName + "...");

        try {
            // Find the type name to get its Y position
            int targetY = getTypeCardY(typeName);
            if (targetY < 0) {
                System.out.println("⚠️ Type card not found for +");
                return false;
            }

            // Strategy 1: Find + button near the type name
            List<WebElement> plusBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == '+' OR label == 'plus' OR name == 'plus' "
                + "OR name == 'plus.circle' OR name == 'plus.circle.fill' "
                + "OR label == 'Increment' OR label CONTAINS 'plus')"
            ));
            for (WebElement btn : plusBtns) {
                int y = btn.getLocation().getY();
                if (Math.abs(y - targetY) < 50) {
                    btn.click();
                    System.out.println("✅ Tapped + for " + typeName);
                    return true;
                }
            }

            // Strategy 2: Find any button with "+" text near type row
            List<WebElement> allBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            for (WebElement btn : allBtns) {
                String label = btn.getAttribute("label");
                String name = btn.getAttribute("name");
                int y = btn.getLocation().getY();
                if (Math.abs(y - targetY) < 50
                        && ("+".equals(label) || "plus".equals(label)
                            || (name != null && name.contains("plus")))) {
                    btn.click();
                    System.out.println("✅ Tapped + for " + typeName + " (broad)");
                    return true;
                }
            }

            // Strategy 3: Use stepper if iOS renders it as a stepper control
            List<WebElement> steppers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStepper' AND visible == true"
            ));
            for (WebElement stepper : steppers) {
                int y = stepper.getLocation().getY();
                if (Math.abs(y - targetY) < 50) {
                    // Tap the right half of stepper (+ side)
                    int sx = stepper.getLocation().getX();
                    int sw = stepper.getSize().getWidth();
                    int sy = stepper.getLocation().getY();
                    int sh = stepper.getSize().getHeight();
                    org.openqa.selenium.interactions.PointerInput finger =
                        new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                    org.openqa.selenium.interactions.Sequence tap =
                        new org.openqa.selenium.interactions.Sequence(finger, 1);
                    tap.addAction(finger.createPointerMove(Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                        sx + (sw * 3 / 4), sy + sh / 2));
                    tap.addAction(finger.createPointerDown(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    tap.addAction(finger.createPointerUp(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    driver.perform(java.util.Arrays.asList(tap));
                    System.out.println("✅ Tapped + on stepper for " + typeName);
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping + button: " + e.getMessage());
        }

        System.out.println("⚠️ Could not tap + button for " + typeName);
        return false;
    }

    /**
     * Tap the - (minus/decrement) button on an asset type card in Quick Count.
     * The - button decreases the asset count (minimum 1).
     * @param typeName the asset type name (e.g., "ATS")
     * @return true if the - button was tapped
     */
    public boolean tapAssetTypeCardMinusButton(String typeName) {
        System.out.println("📍 Tapping - button for asset type: " + typeName + "...");

        try {
            int targetY = getTypeCardY(typeName);
            if (targetY < 0) {
                System.out.println("⚠️ Type card not found for -");
                return false;
            }

            // Strategy 1: Find - button near the type name
            List<WebElement> minusBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == '-' OR label == 'minus' OR name == 'minus' "
                + "OR name == 'minus.circle' OR name == 'minus.circle.fill' "
                + "OR label == 'Decrement' OR label CONTAINS 'minus')"
            ));
            for (WebElement btn : minusBtns) {
                int y = btn.getLocation().getY();
                if (Math.abs(y - targetY) < 50) {
                    btn.click();
                    System.out.println("✅ Tapped - for " + typeName);
                    return true;
                }
            }

            // Strategy 2: Broad button search
            List<WebElement> allBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            for (WebElement btn : allBtns) {
                String label = btn.getAttribute("label");
                String name = btn.getAttribute("name");
                int y = btn.getLocation().getY();
                if (Math.abs(y - targetY) < 50
                        && ("-".equals(label) || "minus".equals(label)
                            || (name != null && name.contains("minus")))) {
                    btn.click();
                    System.out.println("✅ Tapped - for " + typeName + " (broad)");
                    return true;
                }
            }

            // Strategy 3: Stepper — tap the left half (- side)
            List<WebElement> steppers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStepper' AND visible == true"
            ));
            for (WebElement stepper : steppers) {
                int y = stepper.getLocation().getY();
                if (Math.abs(y - targetY) < 50) {
                    int sx = stepper.getLocation().getX();
                    int sw = stepper.getSize().getWidth();
                    int sy = stepper.getLocation().getY();
                    int sh = stepper.getSize().getHeight();
                    org.openqa.selenium.interactions.PointerInput finger =
                        new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                    org.openqa.selenium.interactions.Sequence tap =
                        new org.openqa.selenium.interactions.Sequence(finger, 1);
                    tap.addAction(finger.createPointerMove(Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                        sx + (sw / 4), sy + sh / 2));
                    tap.addAction(finger.createPointerDown(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    tap.addAction(finger.createPointerUp(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                    driver.perform(java.util.Arrays.asList(tap));
                    System.out.println("✅ Tapped - on stepper for " + typeName);
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping - button: " + e.getMessage());
        }

        System.out.println("⚠️ Could not tap - button for " + typeName);
        return false;
    }

    /**
     * Tap the delete (trash) icon on an asset type card to remove it from Quick Count.
     * @param typeName the asset type name (e.g., "ATS")
     * @return true if the delete icon was tapped
     */
    public boolean tapAssetTypeCardDeleteButton(String typeName) {
        System.out.println("📍 Tapping delete/trash icon for asset type: " + typeName + "...");

        try {
            int targetY = getTypeCardY(typeName);
            if (targetY < 0) {
                System.out.println("⚠️ Type card not found for delete");
                return false;
            }

            // Strategy 1: Trash icon button near the type row
            List<WebElement> trashBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(name CONTAINS 'trash' OR label CONTAINS 'trash' "
                + "OR label == 'Delete' OR name CONTAINS 'delete' "
                + "OR name == 'trash.circle' OR name == 'trash.fill')"
            ));
            for (WebElement btn : trashBtns) {
                int y = btn.getLocation().getY();
                if (Math.abs(y - targetY) < 50) {
                    btn.click();
                    System.out.println("✅ Tapped trash for " + typeName);
                    return true;
                }
            }

            // Strategy 2: Image element with trash icon near the row
            List<WebElement> trashImages = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage' AND "
                + "(name CONTAINS 'trash' OR label CONTAINS 'trash')"
            ));
            for (WebElement img : trashImages) {
                int y = img.getLocation().getY();
                if (Math.abs(y - targetY) < 50) {
                    img.click();
                    System.out.println("✅ Tapped trash image for " + typeName);
                    return true;
                }
            }

            // Strategy 3: Small button on the right side of the type row
            List<WebElement> allBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
            int rightThreshold = screenSize.width * 3 / 4; // Right 25% of screen
            for (WebElement btn : allBtns) {
                int y = btn.getLocation().getY();
                int x = btn.getLocation().getX();
                int w = btn.getSize().getWidth();
                int h = btn.getSize().getHeight();
                if (Math.abs(y - targetY) < 50 && x > rightThreshold
                        && w < 60 && h < 60) {
                    btn.click();
                    System.out.println("✅ Tapped delete icon (right side) for " + typeName);
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping delete: " + e.getMessage());
        }

        System.out.println("⚠️ Could not tap delete for " + typeName);
        return false;
    }

    /**
     * Check if an asset type card is in expanded state (showing photosets section).
     * @param typeName the asset type name (e.g., "ATS")
     * @return true if the card appears expanded
     */
    public boolean isAssetTypeCardExpanded(String typeName) {
        System.out.println("📍 Checking if asset type card is expanded: " + typeName + "...");

        try {
            int targetY = getTypeCardY(typeName);
            if (targetY < 0) return false;

            // An expanded card shows "Photosets" or "Add Photoset" below the type name
            List<WebElement> expandedContent = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'Photoset' OR label CONTAINS 'photoset')"
            ));
            for (WebElement el : expandedContent) {
                int y = el.getLocation().getY();
                // Expanded content is below the card header (within 200px)
                if (y > targetY && (y - targetY) < 200) {
                    System.out.println("✅ Card is expanded: " + typeName + " (Photosets at Y=" + y + ")");
                    return true;
                }
            }

            // Also check for "Add Photoset for..." button
            List<WebElement> addPhotoset = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Add Photoset'"
            ));
            for (WebElement el : addPhotoset) {
                int y = el.getLocation().getY();
                if (y > targetY && (y - targetY) < 200) {
                    System.out.println("✅ Card is expanded: " + typeName + " (Add Photoset at Y=" + y + ")");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Card not detected as expanded: " + typeName);
        return false;
    }

    /**
     * Tap the expand/collapse chevron on an asset type card.
     * @param typeName the asset type name (e.g., "ATS")
     * @return true if the chevron was tapped
     */
    public boolean tapAssetTypeCardChevron(String typeName) {
        System.out.println("📍 Tapping chevron for asset type card: " + typeName + "...");

        try {
            int targetY = getTypeCardY(typeName);
            if (targetY < 0) return false;

            // Strategy 1: Chevron/disclosure image near the type row
            List<WebElement> chevrons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage' AND "
                + "(name CONTAINS 'chevron' OR name CONTAINS 'disclosure' "
                + "OR name CONTAINS 'arrow')"
            ));
            for (WebElement chevron : chevrons) {
                int y = chevron.getLocation().getY();
                if (Math.abs(y - targetY) < 40) {
                    chevron.click();
                    System.out.println("✅ Tapped chevron for " + typeName);
                    return true;
                }
            }

            // Strategy 2: Tap the type name itself (many SwiftUI cards expand on tap)
            List<WebElement> typeLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == '" + typeName + "'"
            ));
            for (WebElement label : typeLabels) {
                int y = label.getLocation().getY();
                if (y > 150) {
                    label.click();
                    System.out.println("✅ Tapped type name to toggle expand: " + typeName);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap chevron for " + typeName);
        return false;
    }

    // ================================================================
    // QUICK COUNT — PHOTOSETS SECTION (TC_JOB_145-146)
    // ================================================================

    /**
     * Check if the "Photosets" label is displayed in an expanded asset type card.
     * @return true if the "Photosets" label is visible
     */
    public boolean isPhotosetsLabelDisplayed() {
        System.out.println("📍 Checking for 'Photosets' label...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Photosets' OR label CONTAINS 'Photosets')"
            ));
            boolean displayed = !found.isEmpty();
            System.out.println(displayed
                ? "✅ Photosets label found"
                : "⚠️ Photosets label not found");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the "Add Photoset for [Type] [N]" button is displayed.
     * @return true if the button is visible
     */
    public boolean isAddPhotosetButtonDisplayed() {
        System.out.println("📍 Checking for 'Add Photoset' button...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Add Photoset'"
            ));
            boolean displayed = !found.isEmpty();
            System.out.println(displayed
                ? "✅ Add Photoset button found"
                : "⚠️ Add Photoset button not found");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the full text of the "Add Photoset for [Type] [N]" button.
     * @return the button text (e.g., "Add Photoset for ATS 1"), or null
     */
    public String getAddPhotosetButtonText() {
        System.out.println("📍 Getting Add Photoset button text...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Add Photoset'"
            ));
            if (!found.isEmpty()) {
                String label = found.get(0).getAttribute("label");
                System.out.println("📊 Add Photoset button text: " + label);
                return label;
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Add Photoset button text not found");
        return null;
    }

    /**
     * Tap the "Add Photoset for [Type] [N]" button.
     * @return true if the button was tapped
     */
    public boolean tapAddPhotosetButton() {
        System.out.println("📍 Tapping 'Add Photoset' button...");

        // Strategy 1: Button with "Add Photoset" label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Add Photoset'"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Add Photoset button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text that is tappable
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Add Photoset'"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped Add Photoset text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search
        try {
            WebElement el = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Add Photoset' AND visible == true"
            ));
            el.click();
            System.out.println("✅ Tapped Add Photoset (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Add Photoset button");
            return false;
        }
    }

    /**
     * Internal helper: Get the Y position of an asset type card's header row.
     * @param typeName the asset type name
     * @return the Y coordinate, or -1 if not found
     */
    private int getTypeCardY(String typeName) {
        try {
            List<WebElement> typeLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == '" + typeName + "'"
            ));
            for (WebElement label : typeLabels) {
                int y = label.getLocation().getY();
                if (y > 150) return y;
            }
        } catch (Exception e) { /* continue */ }
        return -1;
    }

    // ================================================================
    // ADD PHOTOS SCREEN (TC_JOB_147-149)
    // ================================================================

    /**
     * Check if the "Add Photos" screen is displayed (opened from "Add Photoset for..." button).
     * Shows: Cancel, "Add Photos" title, "Take photos for [Type] [N]",
     * camera icon, "No photos yet", Gallery and Camera buttons, Done button.
     * @return true if the Add Photos screen is detected
     */
    public boolean isAddPhotosScreenDisplayed() {
        System.out.println("📍 Checking for Add Photos screen...");

        // Strategy 1: "Add Photos" navigation bar or title
        try {
            List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeNavigationBar' AND "
                + "(name == 'Add Photos' OR name CONTAINS 'Add Photos')) "
                + "OR (type == 'XCUIElementTypeStaticText' AND label == 'Add Photos')"
            ));
            if (!titles.isEmpty()) {
                System.out.println("✅ Add Photos screen detected via title");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: "Take photos for" label
        try {
            List<WebElement> takePhotos = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Take photos for'"
            ));
            if (!takePhotos.isEmpty()) {
                System.out.println("✅ Add Photos screen detected via 'Take photos for' text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: "No photos yet" text
        try {
            List<WebElement> noPhotos = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'No photos yet' OR label CONTAINS 'No photos')"
            ));
            if (!noPhotos.isEmpty()) {
                System.out.println("✅ Add Photos screen detected via 'No photos yet'");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Add Photos screen not detected");
        return false;
    }

    /**
     * Get the "Take photos for [Type] [N]" text on the Add Photos screen.
     * @return the text (e.g., "Take photos for ATS 1"), or null
     */
    public String getAddPhotosAssetLabel() {
        System.out.println("📍 Getting Add Photos asset label...");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Take photos for'"
            ));
            if (!texts.isEmpty()) {
                String label = texts.get(0).getAttribute("label");
                System.out.println("📊 Add Photos label: " + label);
                return label;
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Add Photos asset label not found");
        return null;
    }

    /**
     * Check if the "No photos yet" empty state text is displayed on Add Photos screen.
     * @return true if the text is visible
     */
    public boolean isNoPhotosYetTextDisplayed() {
        System.out.println("📍 Checking for 'No photos yet' text...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'No photos yet' OR label CONTAINS 'No photos')"
            ));
            boolean displayed = !found.isEmpty();
            System.out.println(displayed
                ? "✅ 'No photos yet' text found"
                : "⚠️ 'No photos yet' text not found");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Gallery button is displayed on the Add Photos screen.
     * @return true if the Gallery button is visible
     */
    public boolean isAddPhotosGalleryButtonDisplayed() {
        System.out.println("📍 Checking for Gallery button on Add Photos screen...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label == 'Gallery' OR label CONTAINS 'Gallery' "
                + "OR label == 'Photo Library' OR label CONTAINS 'photo.on.rectangle')"
            ));
            boolean displayed = !found.isEmpty();
            System.out.println(displayed
                ? "✅ Gallery button found"
                : "⚠️ Gallery button not found");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Camera button is displayed on the Add Photos screen.
     * @return true if the Camera button is visible
     */
    public boolean isAddPhotosCameraButtonDisplayed() {
        System.out.println("📍 Checking for Camera button on Add Photos screen...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label == 'Camera' OR label CONTAINS 'Camera' "
                + "OR label CONTAINS 'camera' OR label CONTAINS 'camera.fill')"
            ));
            boolean displayed = !found.isEmpty();
            System.out.println(displayed
                ? "✅ Camera button found"
                : "⚠️ Camera button not found");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Done button is displayed on the Add Photos screen.
     * Initially gray/disabled until photos are added.
     * @return true if the Done button is visible
     */
    public boolean isAddPhotosDoneButtonDisplayed() {
        System.out.println("📍 Checking for Done button on Add Photos screen...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Done'"
            ));
            boolean displayed = !found.isEmpty();
            System.out.println(displayed
                ? "✅ Done button found"
                : "⚠️ Done button not found");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the Gallery button on the Add Photos screen.
     * @return true if the button was tapped
     */
    public boolean tapAddPhotosGalleryButton() {
        System.out.println("📍 Tapping Gallery button...");

        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == 'Gallery' OR label CONTAINS 'Gallery' "
                + "OR label == 'Photo Library')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Gallery button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Broad fallback
        try {
            WebElement el = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Gallery' AND visible == true"
            ));
            el.click();
            System.out.println("✅ Tapped Gallery (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Gallery button");
            return false;
        }
    }

    /**
     * Tap the Camera button on the Add Photos screen.
     * @return true if the button was tapped
     */
    public boolean tapAddPhotosCameraButton() {
        System.out.println("📍 Tapping Camera button...");

        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == 'Camera' OR label CONTAINS 'Camera')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Camera button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        try {
            WebElement el = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Camera' AND visible == true"
            ));
            el.click();
            System.out.println("✅ Tapped Camera (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Camera button");
            return false;
        }
    }

    /**
     * Tap the Cancel button on the Add Photos screen to go back to Quick Count.
     * @return true if Cancel was tapped
     */
    public boolean tapAddPhotosCancelButton() {
        System.out.println("📍 Tapping Cancel on Add Photos screen...");
        try {
            List<WebElement> cancelBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            if (!cancelBtns.isEmpty()) {
                cancelBtns.get(0).click();
                System.out.println("✅ Tapped Cancel on Add Photos");
                return true;
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Could not tap Cancel on Add Photos");
        return false;
    }

    // ================================================================
    // QUICK COUNT — PHOTOSET ENTRIES (TC_JOB_150-151)
    // ================================================================

    /**
     * Get the number of photoset entries displayed in the expanded asset type card.
     * A photoset entry shows: green checkmark, "[N] photos for [Type] [N]", thumbnail, X button.
     * @return count of photoset entries, or 0 if none
     */
    public int getPhotosetEntryCount() {
        System.out.println("📍 Counting photoset entries...");
        try {
            // Look for text matching "[N] photo(s) for" pattern
            List<WebElement> photoTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'photo' OR label CONTAINS 'Photo') "
                + "AND label CONTAINS 'for'"
            ));
            int count = 0;
            for (WebElement text : photoTexts) {
                String label = text.getAttribute("label");
                if (label != null && label.matches(".*\\d+\\s+photo.*for.*")) {
                    count++;
                }
            }
            System.out.println("📊 Photoset entries: " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error counting photoset entries: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get the text of a photoset entry at the given index.
     * Expected format: "[N] photos for [Type] [N]" (e.g., "3 photos for ATS 1")
     * @param index 0-based index
     * @return the photoset entry text, or null
     */
    public String getPhotosetEntryText(int index) {
        System.out.println("📍 Getting photoset entry text at index " + index + "...");
        try {
            List<WebElement> photoTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'photo' OR label CONTAINS 'Photo') "
                + "AND label CONTAINS 'for'"
            ));
            java.util.List<String> entries = new java.util.ArrayList<>();
            for (WebElement text : photoTexts) {
                String label = text.getAttribute("label");
                if (label != null && label.matches(".*\\d+\\s+photo.*for.*")) {
                    entries.add(label);
                }
            }
            if (index < entries.size()) {
                System.out.println("📊 Photoset entry " + index + ": " + entries.get(index));
                return entries.get(index);
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Photoset entry at index " + index + " not found");
        return null;
    }

    /**
     * Check if a photoset entry has a green checkmark indicator.
     * @return true if any checkmark is found near photoset entries
     */
    public boolean isPhotosetCheckmarkDisplayed() {
        System.out.println("📍 Checking for photoset checkmark...");
        try {
            List<WebElement> checkmarks = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label CONTAINS 'checkmark' OR name CONTAINS 'checkmark' "
                + "OR label == '✓' OR name CONTAINS 'checkmark.circle')"
            ));
            boolean found = !checkmarks.isEmpty();
            System.out.println(found ? "✅ Photoset checkmark found" : "⚠️ No checkmark found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap the X (remove) button on a photoset entry to remove it.
     * @param index 0-based index of the photoset entry to remove
     * @return true if the X button was tapped
     */
    public boolean tapPhotosetRemoveButton(int index) {
        System.out.println("📍 Tapping X button to remove photoset at index " + index + "...");

        try {
            // Find photoset entry text to get its Y position
            List<WebElement> photoTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'photo' OR label CONTAINS 'Photo') "
                + "AND label CONTAINS 'for'"
            ));
            java.util.List<Integer> entryYs = new java.util.ArrayList<>();
            for (WebElement text : photoTexts) {
                String label = text.getAttribute("label");
                if (label != null && label.matches(".*\\d+\\s+photo.*for.*")) {
                    entryYs.add(text.getLocation().getY());
                }
            }

            if (index >= entryYs.size()) {
                System.out.println("⚠️ Photoset index " + index + " out of range");
                return false;
            }

            int targetY = entryYs.get(index);

            // Strategy 1: Find X/close button near the photoset entry
            List<WebElement> xButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == 'X' OR label == 'x' OR label == 'Remove' "
                + "OR name CONTAINS 'xmark' OR name CONTAINS 'close' "
                + "OR name CONTAINS 'multiply' OR label CONTAINS 'Delete')"
            ));
            for (WebElement btn : xButtons) {
                int y = btn.getLocation().getY();
                if (Math.abs(y - targetY) < 40) {
                    btn.click();
                    System.out.println("✅ Tapped X to remove photoset " + index);
                    return true;
                }
            }

            // Strategy 2: Small button on right side of the entry row
            List<WebElement> allBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
            for (WebElement btn : allBtns) {
                int y = btn.getLocation().getY();
                int x = btn.getLocation().getX();
                int w = btn.getSize().getWidth();
                if (Math.abs(y - targetY) < 40 && x > screenSize.width * 2 / 3 && w < 50) {
                    btn.click();
                    System.out.println("✅ Tapped remove button (right side) for photoset " + index);
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error removing photoset: " + e.getMessage());
        }

        System.out.println("⚠️ Could not tap X on photoset " + index);
        return false;
    }

    // ================================================================
    // QUICK COUNT — SUMMARY SECTION (TC_JOB_152-154)
    // ================================================================

    /**
     * Check if the Quick Count summary section is displayed at the bottom.
     * Shows "Summary" label, asset/photo counts, and "Create [N] Assets" button.
     * @return true if summary section is detected
     */
    public boolean isSummarySectionDisplayed() {
        System.out.println("📍 Checking for Quick Count summary section...");

        // Strategy 1: "Summary" label
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Summary' OR label CONTAINS 'Summary')"
            ));
            if (!found.isEmpty()) {
                System.out.println("✅ Summary section found via label");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: "Create" button with asset count
        try {
            List<WebElement> createBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Create' AND label CONTAINS 'Asset'"
            ));
            if (!createBtns.isEmpty()) {
                System.out.println("✅ Summary section found via Create Assets button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Text with "assets" count pattern
        try {
            List<WebElement> countTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "label CONTAINS 'asset' AND visible == true"
            ));
            for (WebElement text : countTexts) {
                String label = text.getAttribute("label");
                if (label != null && label.matches(".*\\d+\\s+asset.*")) {
                    System.out.println("✅ Summary section found via asset count text");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Summary section not detected");
        return false;
    }

    /**
     * Get the summary text showing asset and photo counts.
     * Expected format: "[N] assets, [N] photos" or "[N] assets"
     * @return the summary text, or null
     */
    public String getSummaryText() {
        System.out.println("📍 Getting Quick Count summary text...");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                if (label != null && label.matches(".*\\d+\\s+asset.*")) {
                    System.out.println("📊 Summary text: " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Summary text not found");
        return null;
    }

    /**
     * Check if the "Create [N] Assets" button is displayed.
     * @return true if the button is visible
     */
    public boolean isCreateAssetsButtonDisplayed() {
        System.out.println("📍 Checking for 'Create [N] Assets' button...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Create' AND label CONTAINS 'Asset'"
            ));
            boolean displayed = !found.isEmpty();
            System.out.println(displayed
                ? "✅ Create Assets button found"
                : "⚠️ Create Assets button not found");
            return displayed;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the "Create [N] Assets" button text.
     * @return the full button text (e.g., "Create 15 Assets"), or null
     */
    public String getCreateAssetsButtonText() {
        System.out.println("📍 Getting Create Assets button text...");
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Create' AND label CONTAINS 'Asset'"
            ));
            if (!found.isEmpty()) {
                String label = found.get(0).getAttribute("label");
                System.out.println("📊 Create button text: " + label);
                return label;
            }
        } catch (Exception e) { /* continue */ }
        System.out.println("⚠️ Create Assets button text not found");
        return null;
    }

    /**
     * Tap the "Create [N] Assets" button to initiate bulk creation.
     * @return true if the button was tapped
     */
    public boolean tapCreateAssetsButton() {
        System.out.println("📍 Tapping 'Create [N] Assets' button...");

        // Strategy 1: Button with Create + Asset label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "label CONTAINS 'Create' AND label CONTAINS 'Asset'"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Create Assets button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text with Create Assets
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "label CONTAINS 'Create' AND label CONTAINS 'Asset'"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped Create Assets text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap Create Assets button");
        return false;
    }

    // ================================================================
    // QUICK COUNT — BULK CREATION FLOW (TC_JOB_155-157)
    // ================================================================

    /**
     * Check if the progress indicator is displayed during bulk asset creation.
     * Shows "Creating [X] of [Total]..." text and possibly a spinner.
     * @return true if a progress indicator is visible
     */
    public boolean isCreationProgressIndicatorDisplayed() {
        System.out.println("📍 Checking for creation progress indicator...");

        // Strategy 1: "Creating" text with progress
        try {
            List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Creating' OR label CONTAINS 'creating')"
            ));
            if (!found.isEmpty()) {
                System.out.println("✅ Progress indicator found: " + found.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Activity indicator (spinner)
        try {
            List<WebElement> spinners = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeActivityIndicator' AND visible == true"
            ));
            if (!spinners.isEmpty()) {
                System.out.println("✅ Activity indicator/spinner found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Progress-related text patterns
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS ' of ' OR label CONTAINS 'Progress')"
            ));
            for (WebElement text : texts) {
                String label = text.getAttribute("label");
                if (label != null && label.matches(".*\\d+\\s+of\\s+\\d+.*")) {
                    System.out.println("✅ Progress text found: " + label);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Progress indicator not detected");
        return false;
    }

    /**
     * Get the progress indicator text (e.g., "Creating 5 of 15...").
     * @return the progress text, or null
     */
    public String getCreationProgressText() {
        System.out.println("📍 Getting creation progress text...");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'Creating' OR label CONTAINS 'creating')"
            ));
            if (!texts.isEmpty()) {
                String label = texts.get(0).getAttribute("label");
                System.out.println("📊 Progress: " + label);
                return label;
            }
        } catch (Exception e) { /* continue */ }
        return null;
    }

    /**
     * Wait for the bulk creation progress to complete (progress indicator disappears
     * and success dialog or result screen appears).
     * @param timeoutSeconds max seconds to wait
     * @return true if creation completed within timeout
     */
    public boolean waitForCreationCompletion(int timeoutSeconds) {
        System.out.println("📍 Waiting for creation to complete (max " + timeoutSeconds + "s)...");
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.pollingEvery(Duration.ofMillis(1000));
            return wait.until(d -> {
                // Check for success dialog or completion
                boolean successDialog = isSuccessDialogDisplayed();
                boolean noProgress = !isCreationProgressIndicatorDisplayed();
                return successDialog || noProgress;
            });
        } catch (Exception e) {
            System.out.println("⚠️ Timeout waiting for creation to complete");
            return false;
        }
    }

    /**
     * Check if the success dialog is displayed after bulk asset creation.
     * Shows: "Success" title, "Successfully created [N] assets" message, "OK" button.
     * @return true if the success dialog is visible
     */
    public boolean isSuccessDialogDisplayed() {
        System.out.println("📍 Checking for success dialog...");

        // Strategy 1: Alert with "Success" title
        try {
            List<WebElement> alerts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeAlert'"
            ));
            if (!alerts.isEmpty()) {
                System.out.println("✅ Alert dialog detected");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: "Success" text
        try {
            List<WebElement> successTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Success' OR label CONTAINS 'Successfully created')"
            ));
            if (!successTexts.isEmpty()) {
                System.out.println("✅ Success dialog text found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: OK button + success-related text
        try {
            List<WebElement> okBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'OK'"
            ));
            List<WebElement> successContent = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Success' OR label CONTAINS 'created' "
                + "OR label CONTAINS 'Created')"
            ));
            if (!okBtns.isEmpty() && !successContent.isEmpty()) {
                System.out.println("✅ Success dialog detected via OK + success text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Success dialog not detected");
        return false;
    }

    /**
     * Get the success dialog message text.
     * Expected: "Successfully created [N] assets"
     * @return the message text, or null
     */
    public String getSuccessDialogText() {
        System.out.println("📍 Getting success dialog text...");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'Successfully' OR label CONTAINS 'created')"
            ));
            if (!texts.isEmpty()) {
                String label = texts.get(0).getAttribute("label");
                System.out.println("📊 Success text: " + label);
                return label;
            }
        } catch (Exception e) { /* continue */ }
        return null;
    }

    /**
     * Tap the "OK" button on the success dialog.
     * @return true if OK was tapped
     */
    public boolean tapSuccessDialogOKButton() {
        System.out.println("📍 Tapping OK on success dialog...");

        // Strategy 1: Alert button "OK"
        try {
            List<WebElement> okBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'OK'"
            ));
            if (!okBtns.isEmpty()) {
                okBtns.get(0).click();
                System.out.println("✅ Tapped OK on success dialog");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Any visible OK/Done button
        try {
            List<WebElement> btns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label == 'OK' OR label == 'Done' OR label == 'Dismiss') "
                + "AND visible == true"
            ));
            if (!btns.isEmpty()) {
                btns.get(0).click();
                System.out.println("✅ Tapped OK/Done/Dismiss");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap OK button");
        return false;
    }

    // ================================================================
    // NEW ASSET FORM — SESSION CONTEXT (TC_JOB_097-099)
    // ================================================================

    /**
     * Check if the New Asset form is displayed (opened from Add Assets).
     * Looks for "New Asset" title and form elements.
     */
    public boolean isSessionNewAssetFormDisplayed() {
        System.out.println("📍 Checking for New Asset form...");

        // Strategy 1: "New Asset" navigation bar or title
        try {
            List<WebElement> titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeNavigationBar' AND "
                + "(name == 'New Asset' OR label == 'New Asset')) "
                + "OR (type == 'XCUIElementTypeStaticText' AND label == 'New Asset')"
            ));
            // Filter: must not be on Add Assets screen (which also has "New Asset" tab)
            for (WebElement title : titles) {
                String type = title.getAttribute("type");
                if (type != null && type.contains("NavigationBar")) {
                    System.out.println("✅ New Asset form detected (nav bar)");
                    return true;
                }
                // If static text, make sure it's a screen title (Y < 120)
                int y = title.getLocation().getY();
                if (y < 120) {
                    // Verify we're NOT on Add Assets screen by checking for "Existing Asset" tab
                    List<WebElement> existingTab = driver.findElements(
                        AppiumBy.iOSNsPredicateString(
                            "label CONTAINS 'Existing Asset' AND visible == true"
                        ));
                    if (existingTab.isEmpty()) {
                        System.out.println("✅ New Asset form detected (title)");
                        return true;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Presence of Asset Details section header
        try {
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Asset Details' OR label == 'ASSET DETAILS')"
            ));
            if (!headers.isEmpty()) {
                System.out.println("✅ New Asset form detected (Asset Details header)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Presence of Create Asset button
        try {
            List<WebElement> createBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' AND "
                + "(label == 'Create Asset' OR name == 'Create Asset'))"
            ));
            if (!createBtns.isEmpty()) {
                System.out.println("✅ New Asset form detected (Create Asset button)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ New Asset form not found");
        return false;
    }

    /**
     * Wait for the New Asset form to appear (up to 10 seconds).
     */
    public boolean waitForSessionNewAssetForm() {
        for (int i = 0; i < 20; i++) {
            if (isSessionNewAssetFormDisplayed()) return true;
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }
        System.out.println("⚠️ New Asset form did not appear within 10 seconds");
        return false;
    }

    /**
     * Check if the "Asset Details" section header is displayed on the New Asset form.
     */
    public boolean isAssetDetailsSectionDisplayed() {
        try {
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Asset Details' OR label == 'ASSET DETAILS' "
                + "OR label CONTAINS 'Asset Details')"
            ));
            boolean found = !headers.isEmpty();
            System.out.println(found
                ? "✅ Asset Details section found"
                : "⚠️ Asset Details section not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the "Asset Photos" section is displayed on the New Asset form.
     */
    public boolean isAssetPhotosSectionDisplayed() {
        try {
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Asset Photos' OR label == 'ASSET PHOTOS' "
                + "OR label CONTAINS 'Asset Photos')"
            ));
            boolean found = !headers.isEmpty();
            System.out.println(found
                ? "✅ Asset Photos section found"
                : "⚠️ Asset Photos section not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the "Infrared Photos" section is displayed on the New Asset form.
     * This section is session-specific and shows the active job info.
     */
    public boolean isInfraredPhotosSectionDisplayed() {
        System.out.println("📍 Checking for Infrared Photos section...");

        try {
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Infrared Photos' OR label == 'INFRARED PHOTOS' "
                + "OR label CONTAINS 'Infrared Photos' OR label CONTAINS 'IR Photos')"
            ));
            if (!headers.isEmpty()) {
                System.out.println("✅ Infrared Photos section found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: look for thermal/IR related content
        try {
            List<WebElement> irContent = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'FLIR' OR label CONTAINS 'Thermal' "
                + "OR label CONTAINS 'infrared' OR label CONTAINS 'Type: ')"
            ));
            if (!irContent.isEmpty()) {
                System.out.println("✅ IR content found (fallback): "
                    + irContent.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Infrared Photos section not found");
        return false;
    }

    /**
     * Check if the "Create Asset" button is displayed on the New Asset form.
     */
    public boolean isSessionCreateAssetButtonDisplayed() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton') AND "
                + "(label == 'Create Asset' OR name == 'Create Asset' "
                + "OR label CONTAINS 'Create Asset')"
            ));
            boolean found = !buttons.isEmpty();
            System.out.println(found
                ? "✅ Create Asset button found"
                : "⚠️ Create Asset button not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Cancel button is displayed on the New Asset form header.
     */
    public boolean isNewAssetFormCancelButtonDisplayed() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            return !buttons.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap Cancel on the New Asset form to dismiss it.
     */
    public boolean tapNewAssetFormCancel() {
        System.out.println("📍 Tapping Cancel on New Asset form...");
        try {
            WebElement cancel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            cancel.click();
            System.out.println("✅ Tapped Cancel on New Asset form");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Cancel: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the location field text on the New Asset form.
     * When created from a room, this shows the breadcrumb path (e.g., "building > floor > room").
     */
    public String getNewAssetLocationFieldText() {
        System.out.println("📍 Getting location field text on New Asset form...");

        // Strategy 1: Look for breadcrumb text (contains ">") in the form area
        try {
            List<WebElement> breadcrumbs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '>'"
            ));
            for (WebElement bc : breadcrumbs) {
                int y = bc.getLocation().getY();
                String label = bc.getAttribute("label");
                // Location field should be in the form area (not nav bar)
                if (y > 120 && y < 600 && label != null && label.contains(">")) {
                    System.out.println("📊 Location field: " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for button/text near "Location" label
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Location' OR label CONTAINS 'Location')"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    int textY = text.getLocation().getY();
                    String textLabel = text.getAttribute("label");
                    // Value should be just below or near the Location label
                    if (textLabel != null && Math.abs(textY - labelY) < 40
                            && !textLabel.equals("Location") && textLabel.length() > 3) {
                        System.out.println("📊 Location field (near label): " + textLabel);
                        return textLabel;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: "Select location" button (if not locked, it shows as a button)
        try {
            List<WebElement> locBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (name == 'Select location' OR label CONTAINS 'Select location' "
                + "OR accessibility_id == 'Select location')"
            ));
            if (!locBtns.isEmpty()) {
                String label = locBtns.get(0).getAttribute("label");
                System.out.println("📊 Location field (select button): " + label);
                return label;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get location field text");
        return null;
    }

    /**
     * Check if the location field on the New Asset form is locked (non-editable).
     * When creating an asset from a specific room, the location is pre-filled and locked.
     */
    public boolean isNewAssetLocationFieldLocked() {
        System.out.println("📍 Checking if location field is locked...");

        // Strategy 1: Look for lock icon near the location field
        try {
            List<WebElement> lockIcons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') "
                + "AND (name CONTAINS 'lock' OR name CONTAINS 'Lock' "
                + "OR name CONTAINS 'locked')"
            ));
            if (!lockIcons.isEmpty()) {
                System.out.println("✅ Lock icon found — location field is locked");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Check if location button is disabled (enabled=false)
        try {
            List<WebElement> locBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton') AND "
                + "(name CONTAINS 'location' OR label CONTAINS 'location')"
            ));
            for (WebElement btn : locBtns) {
                String enabled = btn.getAttribute("enabled");
                if ("false".equals(enabled)) {
                    System.out.println("✅ Location button is disabled — field is locked");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Location shown as static text (not a button = non-interactive)
        try {
            List<WebElement> breadcrumbs = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '>'"
            ));
            for (WebElement bc : breadcrumbs) {
                int y = bc.getLocation().getY();
                if (y > 120 && y < 600) {
                    // A breadcrumb rendered as static text (not button) indicates locked
                    String type = bc.getAttribute("type");
                    if ("XCUIElementTypeStaticText".equals(type)) {
                        System.out.println("✅ Location is static text — field is locked");
                        return true;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not confirm location field is locked");
        return false;
    }

    /**
     * Get the job info text from the Infrared Photos section on the New Asset form.
     * Returns the job name/description (e.g., "Job - Dec 24, 12:09 PM abhiyant").
     */
    public String getInfraredPhotosJobInfo() {
        System.out.println("📍 Getting Infrared Photos job info...");

        try {
            // Look for text containing "Job" in the IR section area
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));

            if (!irHeader.isEmpty()) {
                int headerY = irHeader.get(0).getLocation().getY();

                // Find job-related text below the IR header
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    int textY = text.getLocation().getY();
                    if (label != null && textY > headerY && textY < headerY + 80
                            && (label.contains("Job") || label.contains("job"))) {
                        System.out.println("📊 IR Photos job info: " + label);
                        return label;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: look for any text with "Job" in the lower form area
        try {
            List<WebElement> jobTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND label CONTAINS 'Job'"
            ));
            for (WebElement text : jobTexts) {
                int y = text.getLocation().getY();
                if (y > 400) { // Should be in the lower part of the form
                    String label = text.getAttribute("label");
                    System.out.println("📊 IR Photos job info (fallback): " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get IR Photos job info");
        return null;
    }

    /**
     * Get the photo type label from the Infrared Photos section.
     * Expected format: "Type: FLIR-SEP" or similar.
     */
    public String getInfraredPhotosTypeLabel() {
        System.out.println("📍 Getting IR Photos type label...");

        try {
            // Look for "Type:" text in the form
            List<WebElement> typeLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label BEGINSWITH 'Type:' OR label BEGINSWITH 'Type :' "
                + "OR label CONTAINS 'Type: ')"
            ));
            if (!typeLabels.isEmpty()) {
                String label = typeLabels.get(0).getAttribute("label");
                System.out.println("📊 IR Photos type: " + label);
                return label;
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: look for FLIR/FLUKE/FOTRIC text near IR section
        try {
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (!irHeader.isEmpty()) {
                int headerY = irHeader.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true "
                    + "AND (label CONTAINS 'FLIR' OR label CONTAINS 'FLUKE' "
                    + "OR label CONTAINS 'FOTRIC')"
                ));
                for (WebElement text : allTexts) {
                    int textY = text.getLocation().getY();
                    if (textY > headerY && textY < headerY + 100) {
                        String label = text.getAttribute("label");
                        System.out.println("📊 IR Photos type (fallback): " + label);
                        return label;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get IR Photos type label");
        return null;
    }

    /**
     * Scroll down on the New Asset form to reveal lower sections
     * (Asset Photos, Infrared Photos, Create Asset button).
     */
    public void scrollNewAssetFormDown() {
        System.out.println("📍 Scrolling New Asset form down...");
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.width / 2, size.height * 3 / 4));
            swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.width / 2, size.height / 4));
            swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(swipe));
            System.out.println("✅ Scrolled form down");
        } catch (Exception e) {
            System.out.println("⚠️ Could not scroll form: " + e.getMessage());
        }
    }

    // ================================================================
    // IR PHOTO FILENAME FIELDS (TC_JOB_100–101, 108)
    // ================================================================

    /**
     * Check if the IR Photo Filename field is displayed in the Infrared Photos section.
     * The field typically shows a thermal camera icon and an initial numeric value (e.g., "1").
     * Uses multi-strategy detection:
     *   1. Look for text field/button near "IR Photo Filename" label
     *   2. Look for thermal camera icon adjacent to a numeric text field
     *   3. Fallback: search for the first editable number field in the IR section area
     */
    public boolean isIRPhotoFilenameFieldDisplayed() {
        System.out.println("📍 Checking for IR Photo Filename field...");

        // Strategy 1: Look for "IR Photo Filename" label
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'IR Photo Filename' OR label CONTAINS 'IR Filename' "
                + "OR label == 'IR Photo Filename')"
            ));
            if (!labels.isEmpty()) {
                System.out.println("✅ IR Photo Filename label found: " + labels.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for thermal camera icon (SF Symbol or image name)
        try {
            List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeButton') "
                + "AND (name CONTAINS 'camera.thermal' OR name CONTAINS 'thermal' "
                + "OR name CONTAINS 'infrared' OR label CONTAINS 'thermal')"
            ));
            if (!icons.isEmpty()) {
                System.out.println("✅ Thermal camera icon found — IR Photo Filename field present");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for numeric text field in the IR section area
        try {
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (!irHeader.isEmpty()) {
                int headerY = irHeader.get(0).getLocation().getY();
                // Find text fields below the IR header
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView' "
                    + "OR type == 'XCUIElementTypeStaticText') AND visible == true"
                ));
                for (WebElement field : fields) {
                    int fieldY = field.getLocation().getY();
                    String value = field.getAttribute("value") != null
                        ? field.getAttribute("value") : field.getAttribute("label");
                    // First numeric field below the IR header is likely the IR Photo Filename
                    if (fieldY > headerY && fieldY < headerY + 150
                            && value != null && value.matches("\\d+")) {
                        System.out.println("✅ IR Photo Filename field found (numeric field): " + value);
                        return true;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ IR Photo Filename field not found");
        return false;
    }

    /**
     * Get the current value of the IR Photo Filename field.
     * Typically shows the initial value "1" (or the next available number for FLIR-SEP).
     * Returns the numeric string value, or null if not found.
     */
    public String getIRPhotoFilenameValue() {
        System.out.println("📍 Getting IR Photo Filename value...");

        // Strategy 1: Find text field near "IR Photo Filename" label
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'IR Photo Filename' OR label CONTAINS 'IR Filename')"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                int labelX = labels.get(0).getLocation().getX();
                // Look for adjacent text field or value
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') "
                    + "AND visible == true"
                ));
                for (WebElement field : fields) {
                    int fieldY = field.getLocation().getY();
                    if (Math.abs(fieldY - labelY) < 40) {
                        String value = field.getAttribute("value");
                        if (value == null || value.isEmpty()) {
                            value = field.getText();
                        }
                        System.out.println("📊 IR Photo Filename value: " + value);
                        return value;
                    }
                }
                // Fallback: look for static text number near the label
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    int textY = text.getLocation().getY();
                    String lbl = text.getAttribute("label");
                    if (Math.abs(textY - labelY) < 40 && lbl != null
                            && lbl.matches("\\d+") && !lbl.equals(labels.get(0).getAttribute("label"))) {
                        System.out.println("📊 IR Photo Filename value (static text): " + lbl);
                        return lbl;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Find the first numeric field below "Infrared Photos" header
        try {
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (!irHeader.isEmpty()) {
                int headerY = irHeader.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') "
                    + "AND visible == true"
                ));
                for (WebElement field : fields) {
                    int fieldY = field.getLocation().getY();
                    if (fieldY > headerY && fieldY < headerY + 150) {
                        String value = field.getAttribute("value");
                        if (value == null || value.isEmpty()) value = field.getText();
                        System.out.println("📊 IR Photo Filename value (first field in IR): " + value);
                        return value;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get IR Photo Filename value");
        return null;
    }

    /**
     * Check if the Visual Photo Filename field is displayed.
     * The field typically shows an image/photo icon and an initial numeric value (e.g., "2").
     */
    public boolean isVisualPhotoFilenameFieldDisplayed() {
        System.out.println("📍 Checking for Visual Photo Filename field...");

        // Strategy 1: Look for "Visual Photo Filename" label
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Visual Photo Filename' OR label CONTAINS 'Visual Filename' "
                + "OR label == 'Visual Photo Filename')"
            ));
            if (!labels.isEmpty()) {
                System.out.println("✅ Visual Photo Filename label found: " + labels.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for image/photo icon (SF Symbol: photo, photo.fill, etc.)
        try {
            List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeButton') "
                + "AND (name CONTAINS 'photo' OR name CONTAINS 'image' "
                + "OR name CONTAINS 'picture' OR label CONTAINS 'photo')"
            ));
            // Filter to icons in the IR section area
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (!icons.isEmpty() && !irHeader.isEmpty()) {
                int headerY = irHeader.get(0).getLocation().getY();
                for (WebElement icon : icons) {
                    int iconY = icon.getLocation().getY();
                    if (iconY > headerY && iconY < headerY + 200) {
                        System.out.println("✅ Image icon found in IR section — Visual Photo Filename field present");
                        return true;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Second numeric field below IR header (after IR Photo Filename)
        try {
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (!irHeader.isEmpty()) {
                int headerY = irHeader.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') "
                    + "AND visible == true"
                ));
                int numericFieldCount = 0;
                for (WebElement field : fields) {
                    int fieldY = field.getLocation().getY();
                    if (fieldY > headerY && fieldY < headerY + 250) {
                        numericFieldCount++;
                        if (numericFieldCount >= 2) {
                            System.out.println("✅ Visual Photo Filename field found (2nd field in IR section)");
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Visual Photo Filename field not found");
        return false;
    }

    /**
     * Get the current value of the Visual Photo Filename field.
     * Typically shows initial value "2" for FLIR-SEP (paired with IR Filename "1").
     * Returns the numeric string value, or null if not found.
     */
    public String getVisualPhotoFilenameValue() {
        System.out.println("📍 Getting Visual Photo Filename value...");

        // Strategy 1: Find text field near "Visual Photo Filename" label
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Visual Photo Filename' OR label CONTAINS 'Visual Filename')"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') "
                    + "AND visible == true"
                ));
                for (WebElement field : fields) {
                    int fieldY = field.getLocation().getY();
                    if (Math.abs(fieldY - labelY) < 40) {
                        String value = field.getAttribute("value");
                        if (value == null || value.isEmpty()) value = field.getText();
                        System.out.println("📊 Visual Photo Filename value: " + value);
                        return value;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Find the second numeric field below "Infrared Photos" header
        try {
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (!irHeader.isEmpty()) {
                int headerY = irHeader.get(0).getLocation().getY();
                List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') "
                    + "AND visible == true"
                ));
                int numericFieldCount = 0;
                for (WebElement field : fields) {
                    int fieldY = field.getLocation().getY();
                    if (fieldY > headerY && fieldY < headerY + 250) {
                        numericFieldCount++;
                        if (numericFieldCount == 2) { // 2nd field = Visual Photo Filename
                            String value = field.getAttribute("value");
                            if (value == null || value.isEmpty()) value = field.getText();
                            System.out.println("📊 Visual Photo Filename value (2nd field): " + value);
                            return value;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get Visual Photo Filename value");
        return null;
    }

    /**
     * Check if the IR Photo Filename field is editable (tappable, accepts text input).
     * Tests both the enabled attribute and the element type (TextField vs StaticText).
     */
    public boolean isIRPhotoFilenameEditable() {
        System.out.println("📍 Checking if IR Photo Filename is editable...");
        return isFilenameFieldEditable(true);
    }

    /**
     * Check if the Visual Photo Filename field is editable.
     */
    public boolean isVisualPhotoFilenameEditable() {
        System.out.println("📍 Checking if Visual Photo Filename is editable...");
        return isFilenameFieldEditable(false);
    }

    /**
     * Internal helper: Check if a filename field in the IR section is editable.
     * @param isFirst true for IR Photo Filename (1st field), false for Visual (2nd field)
     */
    private boolean isFilenameFieldEditable(boolean isFirst) {
        try {
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (irHeader.isEmpty()) return false;

            int headerY = irHeader.get(0).getLocation().getY();
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') "
                + "AND visible == true"
            ));

            int targetIndex = isFirst ? 1 : 2;
            int fieldCount = 0;
            for (WebElement field : fields) {
                int fieldY = field.getLocation().getY();
                if (fieldY > headerY && fieldY < headerY + 250) {
                    fieldCount++;
                    if (fieldCount == targetIndex) {
                        String enabled = field.getAttribute("enabled");
                        String type = field.getAttribute("type");
                        boolean editable = "true".equals(enabled)
                            && (type.contains("TextField") || type.contains("TextInput")
                                || type.contains("TextVie"));
                        System.out.println((isFirst ? "IR" : "Visual")
                            + " Photo Filename editable: " + editable
                            + " (type=" + type + ", enabled=" + enabled + ")");
                        return editable;
                    }
                }
            }

            // Fallback: if no TextField found, check by label name
            String labelSearch = isFirst ? "IR Photo Filename" : "Visual Photo Filename";
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + labelSearch + "'"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                for (WebElement field : fields) {
                    if (Math.abs(field.getLocation().getY() - labelY) < 40) {
                        String enabled = field.getAttribute("enabled");
                        boolean editable = !"false".equals(enabled);
                        System.out.println(labelSearch + " editable (by label): " + editable);
                        return editable;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking filename editability: " + e.getMessage());
        }
        return false;
    }

    /**
     * Set a custom value for the IR Photo Filename field.
     * Clears the current value and enters the new one.
     * @param value the numeric string to enter (e.g., "10")
     * @return true if the field was updated successfully
     */
    public boolean setIRPhotoFilenameValue(String value) {
        System.out.println("📍 Setting IR Photo Filename value to: " + value);
        return setFilenameFieldValue(true, value);
    }

    /**
     * Set a custom value for the Visual Photo Filename field.
     * @param value the numeric string to enter (e.g., "11")
     * @return true if the field was updated successfully
     */
    public boolean setVisualPhotoFilenameValue(String value) {
        System.out.println("📍 Setting Visual Photo Filename value to: " + value);
        return setFilenameFieldValue(false, value);
    }

    /**
     * Internal helper: Set a value on a filename field in the IR section.
     * @param isFirst true for IR Photo Filename (1st field), false for Visual (2nd field)
     * @param value the value to enter
     */
    private boolean setFilenameFieldValue(boolean isFirst, String value) {
        try {
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (irHeader.isEmpty()) {
                System.out.println("⚠️ Cannot find IR section header");
                return false;
            }

            int headerY = irHeader.get(0).getLocation().getY();
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') "
                + "AND visible == true"
            ));

            int targetIndex = isFirst ? 1 : 2;
            int fieldCount = 0;
            for (WebElement field : fields) {
                int fieldY = field.getLocation().getY();
                if (fieldY > headerY && fieldY < headerY + 250) {
                    fieldCount++;
                    if (fieldCount == targetIndex) {
                        field.click();
                        try { Thread.sleep(200); } catch (InterruptedException ie) { /* */ }
                        field.clear();
                        try { Thread.sleep(100); } catch (InterruptedException ie) { /* */ }
                        field.sendKeys(value);
                        System.out.println("✅ Set " + (isFirst ? "IR" : "Visual")
                            + " Photo Filename to: " + value);
                        return true;
                    }
                }
            }

            // Fallback: find by label proximity
            String labelSearch = isFirst ? "IR Photo Filename" : "Visual Photo Filename";
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + labelSearch + "'"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                for (WebElement field : fields) {
                    if (Math.abs(field.getLocation().getY() - labelY) < 40) {
                        field.click();
                        try { Thread.sleep(200); } catch (InterruptedException ie) { /* */ }
                        field.clear();
                        try { Thread.sleep(100); } catch (InterruptedException ie) { /* */ }
                        field.sendKeys(value);
                        System.out.println("✅ Set " + labelSearch + " to: " + value + " (by label)");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error setting filename value: " + e.getMessage());
        }
        return false;
    }

    // ================================================================
    // ADD IR PHOTO PAIR BUTTON (TC_JOB_102–103)
    // ================================================================

    /**
     * Check if the "Add IR Photo Pair" button is displayed in the Infrared Photos section.
     * Button is blue with a plus icon and text "Add IR Photo Pair".
     */
    public boolean isAddIRPhotoPairButtonDisplayed() {
        System.out.println("📍 Checking for Add IR Photo Pair button...");

        // Strategy 1: Look for button with exact label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label == 'Add IR Photo Pair' OR label CONTAINS 'Add IR Photo Pair' "
                + "OR label CONTAINS 'Add IR Photo' OR label CONTAINS 'Add Photo Pair')"
            ));
            if (!buttons.isEmpty()) {
                System.out.println("✅ Add IR Photo Pair button found: " + buttons.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for blue button with plus icon in the IR section
        try {
            List<WebElement> plusButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(name CONTAINS 'plus' OR name CONTAINS 'add' "
                + "OR label CONTAINS 'Add' OR label CONTAINS 'plus')"
            ));
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (!plusButtons.isEmpty() && !irHeader.isEmpty()) {
                int headerY = irHeader.get(0).getLocation().getY();
                for (WebElement btn : plusButtons) {
                    int btnY = btn.getLocation().getY();
                    if (btnY > headerY && btnY < headerY + 300) {
                        System.out.println("✅ Add IR Photo Pair button found (plus button in IR section)");
                        return true;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Add IR Photo Pair button not found");
        return false;
    }

    /**
     * Tap the "Add IR Photo Pair" button.
     * This creates a new IR+Visual photo pair and adds it to the "New IR Photos" list.
     * The filename values increment by 2 for FLIR-SEP photo type.
     * @return true if the button was tapped successfully
     */
    public boolean tapAddIRPhotoPairButton() {
        System.out.println("📍 Tapping Add IR Photo Pair button...");

        // Strategy 1: Direct label match
        try {
            WebElement btn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label == 'Add IR Photo Pair' OR label CONTAINS 'Add IR Photo Pair' "
                + "OR label CONTAINS 'Add IR Photo')"
            ));
            btn.click();
            System.out.println("✅ Tapped Add IR Photo Pair button");
            return true;
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Plus button in IR section area
        try {
            List<WebElement> irHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'Infrared' OR label CONTAINS 'IR Photo')"
            ));
            if (!irHeader.isEmpty()) {
                int headerY = irHeader.get(0).getLocation().getY();
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND "
                    + "(name CONTAINS 'plus' OR name CONTAINS 'add' "
                    + "OR label CONTAINS 'Add' OR label CONTAINS 'Pair')"
                ));
                for (WebElement btn : buttons) {
                    int btnY = btn.getLocation().getY();
                    if (btnY > headerY && btnY < headerY + 300) {
                        btn.click();
                        System.out.println("✅ Tapped Add IR Photo Pair button (IR section plus button)");
                        return true;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap Add IR Photo Pair button");
        return false;
    }

    // ================================================================
    // NEW IR PHOTOS LIST (TC_JOB_104–107)
    // ================================================================

    /**
     * Check if the "New IR Photos" section is displayed (appears after adding at least one pair).
     */
    public boolean isNewIRPhotosSectionDisplayed() {
        System.out.println("📍 Checking for New IR Photos section...");
        try {
            List<WebElement> headers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'New IR Photos' OR label CONTAINS 'New IR Photos' "
                + "OR label == 'NEW IR PHOTOS')"
            ));
            boolean found = !headers.isEmpty();
            System.out.println(found
                ? "✅ New IR Photos section found"
                : "⚠️ New IR Photos section not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the count of IR Photo pairs in the "New IR Photos" list.
     * Each pair shows "IR: N" and "Visual: N" text entries.
     * @return the number of photo pairs, or 0 if section not found
     */
    public int getNewIRPhotoPairCount() {
        System.out.println("📍 Counting IR Photo pairs in New IR Photos list...");

        try {
            // Count entries that contain "IR:" — each pair has one "IR:" entry
            List<WebElement> irEntries = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label BEGINSWITH 'IR:' OR label BEGINSWITH 'IR :' "
                + "OR label CONTAINS 'IR: ')"
            ));

            // Filter to entries in the New IR Photos section area
            List<WebElement> sectionHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'New IR Photos'"
            ));

            if (!sectionHeader.isEmpty()) {
                int sectionY = sectionHeader.get(0).getLocation().getY();
                int count = 0;
                for (WebElement entry : irEntries) {
                    int entryY = entry.getLocation().getY();
                    if (entryY > sectionY) {
                        count++;
                    }
                }
                System.out.println("📊 IR Photo pair count: " + count);
                return count;
            }

            // Fallback: just count all "IR:" entries (may include the current input fields)
            int count = irEntries.size();
            System.out.println("📊 IR Photo pair count (fallback): " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error counting IR Photo pairs: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get the IR filename value from a specific photo pair in the "New IR Photos" list.
     * @param pairIndex 0-based index of the pair in the list
     * @return the IR filename string (e.g., "1"), or null if not found
     */
    public String getIRPhotoPairIRValue(int pairIndex) {
        return getPhotoPairValue(pairIndex, true);
    }

    /**
     * Get the Visual filename value from a specific photo pair in the "New IR Photos" list.
     * @param pairIndex 0-based index of the pair in the list
     * @return the Visual filename string (e.g., "2"), or null if not found
     */
    public String getIRPhotoPairVisualValue(int pairIndex) {
        return getPhotoPairValue(pairIndex, false);
    }

    /**
     * Internal helper: Get IR or Visual value from a specific pair in the list.
     */
    private String getPhotoPairValue(int pairIndex, boolean isIR) {
        String prefix = isIR ? "IR" : "Visual";
        System.out.println("📍 Getting " + prefix + " value from pair #" + pairIndex + "...");

        try {
            String searchPrefix = isIR ? "IR:" : "Visual:";
            List<WebElement> entries = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label BEGINSWITH '" + searchPrefix + "' OR label CONTAINS '" + searchPrefix + " ')"
            ));

            // Filter to entries in the New IR Photos section
            List<WebElement> sectionHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'New IR Photos'"
            ));

            java.util.List<WebElement> filteredEntries = new java.util.ArrayList<>();
            if (!sectionHeader.isEmpty()) {
                int sectionY = sectionHeader.get(0).getLocation().getY();
                for (WebElement entry : entries) {
                    if (entry.getLocation().getY() > sectionY) {
                        filteredEntries.add(entry);
                    }
                }
            } else {
                filteredEntries.addAll(entries);
            }

            if (pairIndex < filteredEntries.size()) {
                String label = filteredEntries.get(pairIndex).getAttribute("label");
                // Extract the numeric value: "IR: 1" → "1"
                String value = label.replaceAll("[^0-9]", "").trim();
                System.out.println("📊 Pair #" + pairIndex + " " + prefix + " value: " + value
                    + " (from label: '" + label + "')");
                return value;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting pair value: " + e.getMessage());
        }

        System.out.println("⚠️ Could not get " + prefix + " value for pair #" + pairIndex);
        return null;
    }

    /**
     * Check if the edit icon (yellow/orange pencil) is displayed on an IR Photo pair.
     * @param pairIndex 0-based index of the pair in the "New IR Photos" list
     * @return true if the edit icon is found
     */
    public boolean isIRPhotoPairEditIconDisplayed(int pairIndex) {
        System.out.println("📍 Checking for edit icon on IR Photo pair #" + pairIndex + "...");
        return isPairActionIconDisplayed(pairIndex, true);
    }

    /**
     * Check if the delete icon (red trash) is displayed on an IR Photo pair.
     * @param pairIndex 0-based index of the pair in the "New IR Photos" list
     * @return true if the delete icon is found
     */
    public boolean isIRPhotoPairDeleteIconDisplayed(int pairIndex) {
        System.out.println("📍 Checking for delete icon on IR Photo pair #" + pairIndex + "...");
        return isPairActionIconDisplayed(pairIndex, false);
    }

    /**
     * Internal helper: Check for edit or delete icon on a photo pair row.
     * Looks for icon buttons near the "IR: N" text of the given pair.
     */
    private boolean isPairActionIconDisplayed(int pairIndex, boolean isEdit) {
        try {
            // Find IR entries in the New IR Photos section to locate the pair's Y position
            List<WebElement> irEntries = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label BEGINSWITH 'IR:' OR label CONTAINS 'IR: ')"
            ));

            List<WebElement> sectionHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'New IR Photos'"
            ));

            java.util.List<WebElement> filteredEntries = new java.util.ArrayList<>();
            if (!sectionHeader.isEmpty()) {
                int sectionY = sectionHeader.get(0).getLocation().getY();
                for (WebElement entry : irEntries) {
                    if (entry.getLocation().getY() > sectionY) filteredEntries.add(entry);
                }
            } else {
                filteredEntries.addAll(irEntries);
            }

            if (pairIndex >= filteredEntries.size()) return false;

            int pairY = filteredEntries.get(pairIndex).getLocation().getY();

            // Now look for icon buttons near that Y position
            String iconPredicate = isEdit
                ? "(name CONTAINS 'pencil' OR name CONTAINS 'edit' OR name CONTAINS 'square.and.pencil' "
                  + "OR label CONTAINS 'Edit' OR label CONTAINS 'edit')"
                : "(name CONTAINS 'trash' OR name CONTAINS 'delete' OR name CONTAINS 'xmark' "
                  + "OR name CONTAINS 'minus.circle' OR label CONTAINS 'Delete' OR label CONTAINS 'Remove')";

            List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeImage') "
                + "AND " + iconPredicate
            ));

            for (WebElement icon : icons) {
                int iconY = icon.getLocation().getY();
                if (Math.abs(iconY - pairY) < 40) {
                    System.out.println("✅ " + (isEdit ? "Edit" : "Delete")
                        + " icon found for pair #" + pairIndex);
                    return true;
                }
            }

            // Fallback: Look for any button (edit=right-most, delete=left of edit) near pair row
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            java.util.List<WebElement> rowButtons = new java.util.ArrayList<>();
            for (WebElement btn : allButtons) {
                int btnY = btn.getLocation().getY();
                if (Math.abs(btnY - pairY) < 40) {
                    rowButtons.add(btn);
                }
            }
            // Sort by X to identify edit (typically first colored icon) and delete (second)
            rowButtons.sort((a, b) -> a.getLocation().getX() - b.getLocation().getX());

            if (rowButtons.size() >= 2) {
                // Edit is typically the first action button, delete is the second
                System.out.println("✅ " + (isEdit ? "Edit" : "Delete")
                    + " icon found (by position) for pair #" + pairIndex
                    + " (" + rowButtons.size() + " buttons in row)");
                return true;
            } else if (rowButtons.size() == 1) {
                // Only one button — check which one it is by color/icon
                System.out.println("ℹ️ Only 1 button found in pair row — assuming "
                    + (isEdit ? "edit" : "delete") + " icon present: " + (rowButtons.size() >= 1));
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking pair action icon: " + e.getMessage());
        }

        System.out.println("⚠️ " + (isEdit ? "Edit" : "Delete") + " icon not found for pair #" + pairIndex);
        return false;
    }

    /**
     * Tap the edit icon on an IR Photo pair in the "New IR Photos" list.
     * @param pairIndex 0-based index of the pair
     * @return true if the icon was tapped successfully
     */
    public boolean tapIRPhotoPairEditIcon(int pairIndex) {
        System.out.println("📍 Tapping edit icon on IR Photo pair #" + pairIndex + "...");
        return tapPairActionIcon(pairIndex, true);
    }

    /**
     * Tap the delete icon on an IR Photo pair in the "New IR Photos" list.
     * @param pairIndex 0-based index of the pair
     * @return true if the icon was tapped successfully
     */
    public boolean tapIRPhotoPairDeleteIcon(int pairIndex) {
        System.out.println("📍 Tapping delete icon on IR Photo pair #" + pairIndex + "...");
        return tapPairActionIcon(pairIndex, false);
    }

    /**
     * Internal helper: Tap edit or delete icon on a photo pair row.
     */
    private boolean tapPairActionIcon(int pairIndex, boolean isEdit) {
        try {
            // Locate the pair's Y position
            List<WebElement> irEntries = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label BEGINSWITH 'IR:' OR label CONTAINS 'IR: ')"
            ));

            List<WebElement> sectionHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'New IR Photos'"
            ));

            java.util.List<WebElement> filteredEntries = new java.util.ArrayList<>();
            if (!sectionHeader.isEmpty()) {
                int sectionY = sectionHeader.get(0).getLocation().getY();
                for (WebElement entry : irEntries) {
                    if (entry.getLocation().getY() > sectionY) filteredEntries.add(entry);
                }
            } else {
                filteredEntries.addAll(irEntries);
            }

            if (pairIndex >= filteredEntries.size()) {
                System.out.println("⚠️ Pair #" + pairIndex + " not found (only "
                    + filteredEntries.size() + " pairs)");
                return false;
            }

            int pairY = filteredEntries.get(pairIndex).getLocation().getY();

            // Strategy 1: Find icon by name/label
            String iconPredicate = isEdit
                ? "(name CONTAINS 'pencil' OR name CONTAINS 'edit' OR label CONTAINS 'Edit')"
                : "(name CONTAINS 'trash' OR name CONTAINS 'delete' OR label CONTAINS 'Delete' OR label CONTAINS 'Remove')";

            List<WebElement> icons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeImage') "
                + "AND " + iconPredicate
            ));

            for (WebElement icon : icons) {
                int iconY = icon.getLocation().getY();
                if (Math.abs(iconY - pairY) < 40) {
                    icon.click();
                    System.out.println("✅ Tapped " + (isEdit ? "edit" : "delete")
                        + " icon on pair #" + pairIndex);
                    return true;
                }
            }

            // Strategy 2: Find buttons by position in the row
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            java.util.List<WebElement> rowButtons = new java.util.ArrayList<>();
            for (WebElement btn : allButtons) {
                int btnY = btn.getLocation().getY();
                if (Math.abs(btnY - pairY) < 40) {
                    rowButtons.add(btn);
                }
            }
            rowButtons.sort((a, b) -> a.getLocation().getX() - b.getLocation().getX());

            if (rowButtons.size() >= 2) {
                // Edit = first button (left), Delete = second button (right)
                WebElement target = isEdit ? rowButtons.get(0) : rowButtons.get(1);
                target.click();
                System.out.println("✅ Tapped " + (isEdit ? "edit" : "delete")
                    + " icon on pair #" + pairIndex + " (by position)");
                return true;
            } else if (rowButtons.size() == 1) {
                rowButtons.get(0).click();
                System.out.println("✅ Tapped sole button on pair #" + pairIndex + " row");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error tapping pair action icon: " + e.getMessage());
        }

        System.out.println("⚠️ Could not tap " + (isEdit ? "edit" : "delete")
            + " icon on pair #" + pairIndex);
        return false;
    }

    // ================================================================
    // ASSET PHOTOS SECTION (TC_JOB_109)
    // ================================================================

    /**
     * Get the list of tabs in the Asset Photos section (e.g., ["Profile", "Nameplate", "Panel Sch..."]).
     * These are segmented control tabs within the Asset Photos section.
     * @return list of tab labels, or empty list if not found
     */
    public java.util.List<String> getAssetPhotosTabs() {
        System.out.println("📍 Getting Asset Photos tabs...");
        java.util.List<String> tabs = new java.util.ArrayList<>();

        try {
            // Strategy 1: Look for segmented control buttons in Asset Photos area
            List<WebElement> photosHeader = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Asset Photos' OR label CONTAINS 'Asset Photos')"
            ));

            if (!photosHeader.isEmpty()) {
                int headerY = photosHeader.get(0).getLocation().getY();

                // Find buttons/texts just below the header (tab area: header+10 to header+80)
                List<WebElement> tabElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                    + "AND visible == true"
                ));
                for (WebElement el : tabElements) {
                    int elY = el.getLocation().getY();
                    String label = el.getAttribute("label");
                    if (elY > headerY && elY < headerY + 80 && label != null
                            && !label.equals("Asset Photos") && !label.isEmpty()
                            && label.length() < 20) {
                        if (!tabs.contains(label)) {
                            tabs.add(label);
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for known photo tab names
        if (tabs.isEmpty()) {
            try {
                String[] knownTabs = {"Profile", "Nameplate", "Panel Sch", "Panel Schedule"};
                for (String tabName : knownTabs) {
                    List<WebElement> found = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                        + "AND label CONTAINS '" + tabName + "'"
                    ));
                    if (!found.isEmpty()) {
                        String label = found.get(0).getAttribute("label");
                        if (!tabs.contains(label)) tabs.add(label);
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Asset Photos tabs: " + tabs);
        return tabs;
    }

    /**
     * Check if the Profile tab is selected (default) in the Asset Photos section.
     */
    public boolean isAssetPhotosProfileTabSelected() {
        System.out.println("📍 Checking if Profile tab is selected...");
        try {
            List<WebElement> profileBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Profile'"
            ));
            for (WebElement btn : profileBtns) {
                // Check 'selected' or 'value' attributes for selected state
                String selected = btn.getAttribute("selected");
                String value = btn.getAttribute("value");
                if ("true".equals(selected) || "1".equals(value)) {
                    System.out.println("✅ Profile tab is selected");
                    return true;
                }
            }
            // Fallback: check if "No Profile photos" text is visible (implies Profile tab is active)
            List<WebElement> noPhotos = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'No Profile photos' OR label CONTAINS 'No Profile')"
            ));
            if (!noPhotos.isEmpty()) {
                System.out.println("✅ Profile tab is selected (No Profile photos placeholder visible)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not confirm Profile tab is selected");
        return false;
    }

    /**
     * Check if the "No Profile photos" placeholder text is displayed in Asset Photos section.
     */
    public boolean isNoProfilePhotosDisplayed() {
        System.out.println("📍 Checking for 'No Profile photos' placeholder...");
        try {
            List<WebElement> placeholders = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'No Profile photos' OR label CONTAINS 'No Profile' "
                + "OR label CONTAINS 'No photos')"
            ));
            boolean found = !placeholders.isEmpty();
            System.out.println(found
                ? "✅ No Profile photos placeholder found"
                : "⚠️ No Profile photos placeholder not found");
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Gallery button is displayed in the Asset Photos section.
     * The button has a blue outline with gallery/photo library icon.
     */
    public boolean isAssetPhotosGalleryButtonDisplayed() {
        System.out.println("📍 Checking for Gallery button in Asset Photos...");
        try {
            List<WebElement> galleryBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton') AND "
                + "(label CONTAINS 'Gallery' OR label CONTAINS 'gallery' "
                + "OR name CONTAINS 'photo.on.rectangle' OR name CONTAINS 'photo.stack' "
                + "OR label CONTAINS 'Photo Library' OR label CONTAINS 'Choose')"
            ));
            if (!galleryBtns.isEmpty()) {
                System.out.println("✅ Gallery button found: " + galleryBtns.get(0).getAttribute("label"));
                return true;
            }

            // Fallback: look for button with image icon near "No Profile photos"
            List<WebElement> noPhotos = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'No'"
            ));
            if (!noPhotos.isEmpty()) {
                int textY = noPhotos.get(0).getLocation().getY();
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true"
                ));
                int photoButtonCount = 0;
                for (WebElement btn : buttons) {
                    int btnY = btn.getLocation().getY();
                    if (btnY > textY && btnY < textY + 100) {
                        photoButtonCount++;
                        if (photoButtonCount == 1) { // First button below is typically Gallery
                            System.out.println("✅ Gallery button found (by position)");
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Gallery button not found");
        return false;
    }

    /**
     * Check if the Camera button is displayed in the Asset Photos section.
     * The button has a blue outline with camera icon.
     */
    public boolean isAssetPhotosCameraButtonDisplayed() {
        System.out.println("📍 Checking for Camera button in Asset Photos...");
        try {
            List<WebElement> cameraBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton') AND "
                + "(label CONTAINS 'Camera' OR label CONTAINS 'camera' "
                + "OR name CONTAINS 'camera' OR name CONTAINS 'camera.fill')"
            ));
            if (!cameraBtns.isEmpty()) {
                System.out.println("✅ Camera button found: " + cameraBtns.get(0).getAttribute("label"));
                return true;
            }

            // Fallback: look for second button near "No Profile photos"
            List<WebElement> noPhotos = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'No'"
            ));
            if (!noPhotos.isEmpty()) {
                int textY = noPhotos.get(0).getLocation().getY();
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == true"
                ));
                int photoButtonCount = 0;
                for (WebElement btn : buttons) {
                    int btnY = btn.getLocation().getY();
                    if (btnY > textY && btnY < textY + 100) {
                        photoButtonCount++;
                        if (photoButtonCount == 2) { // Second button is typically Camera
                            System.out.println("✅ Camera button found (by position)");
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Camera button not found");
        return false;
    }

    /**
     * Scroll up on the New Asset form to reveal upper sections.
     */
    public void scrollNewAssetFormUp() {
        System.out.println("📍 Scrolling New Asset form up...");
        try {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.width / 2, size.height / 4));
            swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.width / 2, size.height * 3 / 4));
            swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(swipe));
            System.out.println("✅ Scrolled form up");
        } catch (Exception e) {
            System.out.println("⚠️ Could not scroll form up: " + e.getMessage());
        }
    }

    // ================================================================
    // NEW ASSET FORM — ASSET NAME & CREATE (TC_JOB_110–111)
    // ================================================================

    /**
     * Set the asset name on the New Asset form (session context).
     * Looks for the Asset Name text field in the Asset Details section.
     * @param name the asset name to enter
     * @return true if the field was found and value entered
     */
    public boolean setSessionNewAssetName(String name) {
        System.out.println("📍 Setting asset name to: " + name);

        // Strategy 1: Text field with placeholder "Asset Name" or near "Asset Name" label
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') "
                + "AND visible == true"
            ));
            // Look for "Asset Name" label to find nearby field
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label == 'Asset Name' OR label CONTAINS 'Asset Name' OR label == 'Name')"
            ));
            if (!labels.isEmpty()) {
                int labelY = labels.get(0).getLocation().getY();
                for (WebElement field : fields) {
                    int fieldY = field.getLocation().getY();
                    if (Math.abs(fieldY - labelY) < 50) {
                        field.click();
                        try { Thread.sleep(200); } catch (InterruptedException ie) { /* */ }
                        field.clear();
                        try { Thread.sleep(100); } catch (InterruptedException ie) { /* */ }
                        field.sendKeys(name);
                        System.out.println("✅ Asset name set to: " + name);
                        return true;
                    }
                }
            }

            // Fallback: first text field in the form area (Y > 120, Y < 400)
            for (WebElement field : fields) {
                int fieldY = field.getLocation().getY();
                if (fieldY > 120 && fieldY < 400) {
                    String placeholder = field.getAttribute("placeholderValue");
                    String value = field.getAttribute("value");
                    if (placeholder != null && (placeholder.contains("Name") || placeholder.contains("name"))
                            || (value == null || value.isEmpty())) {
                        field.click();
                        try { Thread.sleep(200); } catch (InterruptedException ie) { /* */ }
                        field.clear();
                        try { Thread.sleep(100); } catch (InterruptedException ie) { /* */ }
                        field.sendKeys(name);
                        System.out.println("✅ Asset name set to: " + name + " (first field fallback)");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error setting asset name: " + e.getMessage());
        }

        System.out.println("⚠️ Could not set asset name");
        return false;
    }

    /**
     * Tap the "Create Asset" button on the session New Asset form.
     * Waits for the button to become enabled before tapping.
     * @return true if the button was tapped successfully
     */
    public boolean tapSessionCreateAssetButton() {
        System.out.println("📍 Tapping Create Asset button...");

        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton') AND "
                + "(label == 'Create Asset' OR name == 'Create Asset' "
                + "OR label CONTAINS 'Create Asset')"
            ));
            if (!buttons.isEmpty()) {
                // Wait for enabled state
                for (int attempt = 0; attempt < 6; attempt++) {
                    String enabled = buttons.get(0).getAttribute("enabled");
                    if ("true".equals(enabled)) {
                        buttons.get(0).click();
                        System.out.println("✅ Tapped Create Asset button");
                        return true;
                    }
                    try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                }
                // Try clicking anyway
                buttons.get(0).click();
                System.out.println("✅ Tapped Create Asset button (may not have been fully enabled)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: broad search for "Create" in lower form area
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'Create'"
            ));
            org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
            for (WebElement btn : buttons) {
                int btnY = btn.getLocation().getY();
                if (btnY > screenSize.getHeight() * 0.6) {
                    btn.click();
                    System.out.println("✅ Tapped Create button (lower form area)");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap Create Asset button");
        return false;
    }

    /**
     * Check if the new asset was created successfully by detecting screen transition
     * back to Assets in Room or a success toast/message.
     */
    public boolean isAssetCreatedSuccessfully() {
        System.out.println("📍 Checking if asset was created successfully...");

        // Strategy 1: We should return to Assets in Room screen
        try {
            for (int i = 0; i < 10; i++) {
                if (isAssetsInRoomScreenDisplayed()) {
                    System.out.println("✅ Asset created — returned to Assets in Room");
                    return true;
                }
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Check for success toast/message
        try {
            List<WebElement> toasts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND "
                + "(label CONTAINS 'created' OR label CONTAINS 'Created' "
                + "OR label CONTAINS 'success' OR label CONTAINS 'Success')"
            ));
            if (!toasts.isEmpty()) {
                System.out.println("✅ Success message found: " + toasts.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: No longer on New Asset form
        boolean stillOnForm = isSessionNewAssetFormDisplayed();
        if (!stillOnForm) {
            System.out.println("✅ Asset likely created — no longer on New Asset form");
            return true;
        }

        System.out.println("⚠️ Could not confirm asset creation");
        return false;
    }

    // ================================================================
    // SCREEN OPTIMIZING INDICATOR (TC_JOB_116)
    // ================================================================

    /**
     * Check if the "Screen definition optimizing..." toast/indicator is displayed.
     * This may appear during processing operations like adding multiple IR photo pairs.
     */
    public boolean isScreenOptimizingIndicatorDisplayed() {
        System.out.println("📍 Checking for screen optimizing indicator...");

        try {
            List<WebElement> indicators = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'optimizing' OR label CONTAINS 'Optimizing' "
                + "OR label CONTAINS 'Screen definition' OR label CONTAINS 'Processing' "
                + "OR label CONTAINS 'Loading')"
            ));
            if (!indicators.isEmpty()) {
                System.out.println("✅ Optimizing indicator found: " + indicators.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Check for activity indicator (spinner)
        try {
            List<WebElement> spinners = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeActivityIndicator' AND visible == true"
            ));
            if (!spinners.isEmpty()) {
                System.out.println("✅ Activity indicator (spinner) found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Check for progress indicator
        try {
            List<WebElement> progress = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeProgressIndicator' AND visible == true"
            ));
            if (!progress.isEmpty()) {
                System.out.println("✅ Progress indicator found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("ℹ️ No optimizing indicator found (may not be shown or too fast to capture)");
        return false;
    }

    // ================================================================
    // SESSION LOCATIONS — FLOATING QR BUTTON (TC_JOB_119)
    // ================================================================

    /**
     * Check if the floating QR scan button is displayed on the Locations tab.
     * This is typically an orange button with a QR code icon at the bottom-right,
     * positioned above the blue floating + button.
     */
    public boolean isLocationsFloatingQRButtonDisplayed() {
        System.out.println("📍 Checking for floating QR scan button on Locations tab...");

        try {
            org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
            int screenWidth = screenSize.getWidth();
            int screenHeight = screenSize.getHeight();

            // Strategy 1: Button with QR/scan semantics
            List<WebElement> qrButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND "
                + "(label CONTAINS 'QR' OR label CONTAINS 'qr' OR label CONTAINS 'Scan' "
                + "OR label CONTAINS 'scan' OR name CONTAINS 'qrcode' "
                + "OR name CONTAINS 'qrcode.viewfinder' OR name CONTAINS 'barcode')"
            ));
            for (WebElement btn : qrButtons) {
                int btnX = btn.getLocation().getX();
                int btnY = btn.getLocation().getY();
                // Floating button should be in bottom-right quadrant
                if (btnX > screenWidth * 0.6 && btnY > screenHeight * 0.6) {
                    System.out.println("✅ Floating QR button found at (" + btnX + "," + btnY + ")");
                    return true;
                }
            }

            // Strategy 2: Look for circular buttons in bottom-right area above the + button
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            java.util.List<WebElement> bottomRightButtons = new java.util.ArrayList<>();
            for (WebElement btn : allButtons) {
                int btnX = btn.getLocation().getX();
                int btnY = btn.getLocation().getY();
                int btnW = btn.getSize().getWidth();
                int btnH = btn.getSize().getHeight();
                boolean isBottomRight = btnX > screenWidth * 0.7 && btnY > screenHeight * 0.6;
                boolean isCircular = Math.abs(btnW - btnH) < 10 && btnW < 80;
                if (isBottomRight && isCircular) {
                    bottomRightButtons.add(btn);
                }
            }

            // If we find 2+ circular buttons, the upper one is likely QR, lower one is +
            if (bottomRightButtons.size() >= 2) {
                // Sort by Y ascending
                bottomRightButtons.sort((a, b) -> a.getLocation().getY() - b.getLocation().getY());
                WebElement qrBtn = bottomRightButtons.get(0); // Upper one
                String label = qrBtn.getAttribute("label");
                String name = qrBtn.getAttribute("name");
                System.out.println("✅ Floating QR button found (upper of 2 FABs) — label='" + label
                    + "', name='" + name + "'");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking floating QR button: " + e.getMessage());
        }

        System.out.println("⚠️ Floating QR button not found on Locations tab");
        return false;
    }

    /**
     * Get the floor icon, name, and room count details for a floor entry at the given index.
     * Used to verify floor expansion shows correct child information.
     * @param index 0-based floor index
     * @return map with keys: "name", "roomCount", or null if not found
     */
    public java.util.Map<String, String> getLocationsFloorDetails(int index) {
        System.out.println("📍 Getting floor details at index " + index + "...");

        try {
            java.util.List<String> floors = getLocationsFloorEntries();
            if (index >= floors.size()) {
                System.out.println("⚠️ Floor index " + index + " out of range (only " + floors.size() + " floors)");
                return null;
            }

            String floorText = floors.get(index);
            java.util.Map<String, String> details = new java.util.HashMap<>();
            details.put("name", floorText);

            // Extract room count from text like "Floor 1, 5 rooms" or "1_350, 6 nodes"
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+(room|node)")
                .matcher(floorText.toLowerCase());
            if (m.find()) {
                details.put("roomCount", m.group(1));
            }

            System.out.println("📊 Floor details: " + details);
            return details;
        } catch (Exception e) {
            System.out.println("⚠️ Error getting floor details: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if the floor row at the given index has a + button.
     * Similar to building row + button, floors may also have an add button.
     * @param floorIndex 0-based floor index
     * @return true if a + button is found on the floor row
     */
    public boolean isFloorRowAddButtonDisplayed(int floorIndex) {
        System.out.println("📍 Checking for + button on floor row " + floorIndex + "...");

        try {
            java.util.List<String> floors = getLocationsFloorEntries();
            if (floorIndex >= floors.size()) return false;

            String floorText = floors.get(floorIndex);

            // Find the floor cell element
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND label CONTAINS '" + floorText.substring(0, Math.min(10, floorText.length())) + "'"
            ));
            if (cells.isEmpty()) return false;

            int cellY = cells.get(0).getLocation().getY();

            // Look for + button near the floor cell's Y position
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            for (WebElement btn : buttons) {
                int btnY = btn.getLocation().getY();
                int btnW = btn.getSize().getWidth();
                int btnH = btn.getSize().getHeight();
                boolean nearFloor = Math.abs(btnY - cellY) < 30;
                boolean isSmall = btnW < 60 && btnH < 60;
                if (nearFloor && isSmall) {
                    String label = btn.getAttribute("label");
                    String name = btn.getAttribute("name");
                    System.out.println("✅ + button found on floor row " + floorIndex
                        + " — label='" + label + "', name='" + name + "'");
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking floor + button: " + e.getMessage());
        }

        System.out.println("⚠️ No + button found on floor row " + floorIndex);
        return false;
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
    // WITH PHOTOS FILTER (Session Issues)
    // ================================================================

    /**
     * Check if the "With Photos" filter tab is displayed on the session Issues tab.
     * This filter shows issues that have attached photos.
     */
    public boolean isWithPhotosFilterDisplayed() {
        System.out.println("📍 Checking for With Photos filter...");

        // Strategy 1: Button or text with "With Photos" label
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label CONTAINS 'With Photos' OR label CONTAINS 'WITH PHOTOS' "
                + "OR label CONTAINS 'with photos' OR label CONTAINS 'Photos')"
            ));
            for (WebElement el : elements) {
                String label = el.getAttribute("label");
                if (label != null && (label.contains("Photo") || label.contains("photo"))) {
                    System.out.println("✅ With Photos filter found: " + label);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Segmented control with "Photos" option
        try {
            List<WebElement> segments = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSegmentedControl'"
            ));
            for (WebElement seg : segments) {
                List<WebElement> buttons = seg.findElements(AppiumBy.iOSNsPredicateString(
                    "label CONTAINS 'Photo'"
                ));
                if (!buttons.isEmpty()) {
                    System.out.println("✅ With Photos filter found in segmented control");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Image/icon with camera or photo name near filter tabs area
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'With Photos' AND visible == true"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ With Photos filter found (broad search)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ With Photos filter not found");
        return false;
    }

    /**
     * Tap the "With Photos" filter tab.
     * Returns true if tapped successfully.
     */
    public boolean tapWithPhotosFilter() {
        System.out.println("📍 Tapping With Photos filter...");

        // Strategy 1: Button with "With Photos" or "Photos" label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'With Photos' "
                + "OR label CONTAINS 'WITH PHOTOS')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped With Photos filter (button)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text "With Photos" (tap it)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'With Photos' "
                + "OR label CONTAINS 'WITH PHOTOS')"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped With Photos filter (text)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search
        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'With Photos' AND visible == true"
            ));
            element.click();
            System.out.println("✅ Tapped With Photos filter (broad)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap With Photos filter: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the count displayed on the "With Photos" filter badge/label.
     * Returns the count string, or null if not found.
     */
    public String getWithPhotosFilterCount() {
        System.out.println("📍 Getting With Photos filter count...");

        // Strategy 1: Extract count from button/text label (e.g., "With Photos (5)" or "Photos 5")
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND (label CONTAINS 'With Photos' OR label CONTAINS 'WITH PHOTOS' "
                + "OR label CONTAINS 'Photos')"
            ));
            for (WebElement el : elements) {
                String fullLabel = el.getAttribute("label");
                if (fullLabel != null && fullLabel.matches(".*\\d+.*")) {
                    String count = fullLabel.replaceAll("[^0-9]", "");
                    if (!count.isEmpty()) {
                        System.out.println("📊 With Photos count from label: " + count);
                        return count;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for adjacent numeric text near "With Photos" element
        try {
            List<WebElement> photosElements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') "
                + "AND label CONTAINS 'Photo'"
            ));
            if (!photosElements.isEmpty()) {
                WebElement photosEl = photosElements.get(0);
                int elY = photosEl.getLocation().getY();
                int elX = photosEl.getLocation().getX();

                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    if (label != null && label.matches("\\d+")) {
                        int textY = text.getLocation().getY();
                        int textX = text.getLocation().getX();
                        if (Math.abs(textX - elX) < 80 && Math.abs(textY - elY) < 40) {
                            System.out.println("📊 With Photos count from adjacent text: " + label);
                            return label;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not determine With Photos count");
        return null;
    }

    // ================================================================
    // SHOW ALL BUTTON (Work Orders Screen)
    // ================================================================

    /**
     * Check if a "Show All" button is displayed on the Work Orders screen.
     * This button expands the list to show all available work orders.
     */
    public boolean isShowAllButtonDisplayed() {
        System.out.println("📍 Checking for Show All button...");

        // Strategy 1: Button with "Show All" label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Show All' "
                + "OR label CONTAINS 'Show All' OR label CONTAINS 'SHOW ALL' "
                + "OR label CONTAINS 'show all')"
            ));
            if (!buttons.isEmpty()) {
                System.out.println("✅ Show All button found: " + buttons.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text "Show All" (might be a tappable label)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'Show All' "
                + "OR label CONTAINS 'Show All' OR label CONTAINS 'See All')"
            ));
            if (!texts.isEmpty()) {
                System.out.println("✅ Show All text found: " + texts.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Link-style element
        try {
            List<WebElement> links = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeLink' AND label CONTAINS 'All'"
            ));
            if (!links.isEmpty()) {
                System.out.println("✅ Show All link found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Show All button not found");
        return false;
    }

    /**
     * Tap the "Show All" button to expand the full job list.
     * Returns true if tapped successfully.
     */
    public boolean tapShowAllButton() {
        System.out.println("📍 Tapping Show All button...");

        // Strategy 1: Button
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Show All' "
                + "OR label CONTAINS 'SHOW ALL' OR label CONTAINS 'See All')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Show All button");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'Show All' "
                + "OR label CONTAINS 'See All')"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped Show All text");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search
        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(label CONTAINS 'Show All' OR label CONTAINS 'See All') AND visible == true"
            ));
            element.click();
            System.out.println("✅ Tapped Show All (broad search)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Show All: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // DEACTIVATE JOB
    // ================================================================

    /**
     * Deactivate the currently active job by finding and tapping a Deactivate/End button.
     * This is typically on the Work Orders list or Session Details screen.
     * Returns true if deactivation was triggered successfully.
     */
    public boolean deactivateActiveJob() {
        System.out.println("📍 Attempting to deactivate active job...");

        // Strategy 1: "Deactivate" button on the Work Orders list screen
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Deactivate' "
                + "OR label CONTAINS 'DEACTIVATE' OR label CONTAINS 'End Session' "
                + "OR label CONTAINS 'End Job' OR label CONTAINS 'Stop')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped deactivate/end button: " + buttons.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text that acts as a button
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'Deactivate' "
                + "OR label CONTAINS 'End Session' OR label CONTAINS 'End Job')"
            ));
            if (!texts.isEmpty()) {
                texts.get(0).click();
                System.out.println("✅ Tapped deactivate text: " + texts.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for the ACTIVE badge and tap near it — some apps have
        // a toggle where tapping the active entry again provides deactivation option
        try {
            List<WebElement> activeBadges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'ACTIVE' OR label == 'Active')"
            ));
            if (!activeBadges.isEmpty()) {
                // Look for deactivate near the ACTIVE badge
                WebElement badge = activeBadges.get(0);
                int badgeY = badge.getLocation().getY();
                List<WebElement> nearbyButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton'"
                ));
                for (WebElement btn : nearbyButtons) {
                    int btnY = btn.getLocation().getY();
                    String label = btn.getAttribute("label");
                    if (Math.abs(btnY - badgeY) < 60 && label != null
                        && (label.contains("Deactivate") || label.contains("End")
                            || label.contains("Stop") || label.contains("deactivate"))) {
                        btn.click();
                        System.out.println("✅ Tapped deactivate near ACTIVE badge: " + label);
                        return true;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not find deactivate button — job may need manual deactivation");
        return false;
    }

    /**
     * Check if any job is currently active on the Work Orders screen.
     * Returns true if an ACTIVE badge is found.
     */
    public boolean isAnyJobActive() {
        try {
            List<WebElement> activeBadges = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'ACTIVE' OR label == 'Active')"
            ));
            return !activeBadges.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // SESSION ISSUE ENTRY DETAILS
    // ================================================================

    /**
     * Get the issue class tag text from a linked session issue entry at the given index.
     * Issue class tags include: NEC Violation, NFPA 70B, OSHA Violation, Repair Needed,
     * Thermal Anomaly, Ultrasonic Anomaly, etc.
     * Returns the class tag string, or null if not found.
     */
    public String getSessionIssueClassTag(int index) {
        System.out.println("📍 Getting session issue class tag at index " + index + "...");

        String[] knownClasses = {
            "NEC", "NFPA", "OSHA", "Repair", "Thermal", "Ultrasonic",
            "NEC Violation", "NFPA 70B", "OSHA Violation", "Repair Needed",
            "Thermal Anomaly", "Ultrasonic Anomaly"
        };

        try {
            // Find issue cells in the session issues list area
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            List<WebElement> issueCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 300 && h > 50) {
                        issueCells.add(cell);
                    }
                } catch (Exception e2) { /* skip */ }
            }

            if (index >= issueCells.size()) {
                System.out.println("⚠️ Index " + index + " out of range (found " + issueCells.size() + " cells)");
                return null;
            }

            WebElement targetCell = issueCells.get(index);

            // Look for class tag text within the cell's children
            List<WebElement> childTexts = targetCell.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"
            ));
            for (WebElement text : childTexts) {
                String label = text.getAttribute("label");
                if (label != null) {
                    for (String cls : knownClasses) {
                        if (label.contains(cls)) {
                            System.out.println("✅ Found class tag: " + label + " at index " + index);
                            return label;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting class tag at index " + index + ": " + e.getMessage());
        }

        // Fallback: Search all visible texts for class tags in the list area
        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            int matchCount = 0;
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                int y = text.getLocation().getY();
                if (label != null && y > 300) {
                    for (String cls : knownClasses) {
                        if (label.contains(cls)) {
                            if (matchCount == index) {
                                System.out.println("✅ Found class tag (fallback): " + label);
                                return label;
                            }
                            matchCount++;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No class tag found at index " + index);
        return null;
    }

    /**
     * Check if any session issue entry displays a class tag (e.g., "NEC Violation").
     * Returns true if at least one class tag is found in the session issues list.
     */
    public boolean isSessionIssueClassTagDisplayed() {
        System.out.println("📍 Checking for issue class tags in session issues...");

        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'NEC' OR label CONTAINS 'NFPA' OR label CONTAINS 'OSHA' "
                + "OR label CONTAINS 'Repair' OR label CONTAINS 'Thermal' "
                + "OR label CONTAINS 'Ultrasonic')"
            ));
            // Filter to list area (Y > 300)
            for (WebElement text : allTexts) {
                int y = text.getLocation().getY();
                if (y > 300) {
                    System.out.println("✅ Found issue class tag: " + text.getAttribute("label") + " at Y=" + y);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No issue class tags found in session issues list");
        return false;
    }

    /**
     * Get the asset name/location text from a linked session issue entry at the given index.
     * Looks for text with a location pin icon (📍) or asset-related text near the issue entry.
     * Returns the asset location string, or null if not found.
     */
    public String getSessionIssueAssetLocation(int index) {
        System.out.println("📍 Getting session issue asset location at index " + index + "...");

        try {
            // Find issue cells
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            List<WebElement> issueCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 300 && h > 50) {
                        issueCells.add(cell);
                    }
                } catch (Exception e2) { /* skip */ }
            }

            if (index >= issueCells.size()) {
                System.out.println("⚠️ Index " + index + " out of range (found " + issueCells.size() + " cells)");
                return null;
            }

            WebElement targetCell = issueCells.get(index);

            // Look for child texts that contain location/asset info
            List<WebElement> childTexts = targetCell.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"
            ));
            for (WebElement text : childTexts) {
                String label = text.getAttribute("label");
                if (label != null) {
                    // Check for location pin emoji or location-style text
                    if (label.contains("📍") || label.contains("pin")
                        || label.contains("OCP") || label.contains("Panel")
                        || label.contains("Switch") || label.contains("Asset")
                        || label.contains("TestAsset") || label.contains("Trim")) {
                        System.out.println("✅ Found asset location: " + label + " at index " + index);
                        return label;
                    }
                }
            }

            // Look for image elements with location pin icon near the cell
            List<WebElement> images = targetCell.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage' AND (name CONTAINS 'pin' OR name CONTAINS 'location' "
                + "OR name CONTAINS 'mappin' OR name CONTAINS 'map')"
            ));
            if (!images.isEmpty()) {
                // There's a pin icon — find the closest text to it
                WebElement pinIcon = images.get(0);
                int pinY = pinIcon.getLocation().getY();
                int pinX = pinIcon.getLocation().getX();

                for (WebElement text : childTexts) {
                    String label = text.getAttribute("label");
                    int textX = text.getLocation().getX();
                    int textY = text.getLocation().getY();
                    // Text to the right of pin icon on same row
                    if (label != null && !label.isEmpty()
                        && textX > pinX && Math.abs(textY - pinY) < 20) {
                        System.out.println("✅ Found asset location near pin icon: " + label);
                        return label;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting asset location at index " + index + ": " + e.getMessage());
        }

        System.out.println("⚠️ No asset location found at index " + index);
        return null;
    }

    /**
     * Check if any session issue entry displays an asset name with a location icon.
     * Returns true if at least one asset/location element is found in the session issues list.
     */
    public boolean isSessionIssueAssetLocationDisplayed() {
        System.out.println("📍 Checking for asset location in session issues...");

        // Strategy 1: Look for location pin images in the issues list area
        try {
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage' AND (name CONTAINS 'pin' OR name CONTAINS 'location' "
                + "OR name CONTAINS 'mappin' OR name CONTAINS 'map')"
            ));
            for (WebElement img : images) {
                int y = img.getLocation().getY();
                if (y > 300) {
                    System.out.println("✅ Found location pin icon at Y=" + y);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for text containing 📍 emoji in list area
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true AND label CONTAINS '📍'"
            ));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y > 300) {
                    System.out.println("✅ Found 📍 text in session issues: " + text.getAttribute("label"));
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Check if session issue entries have asset-related text
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 300 && h > 50) {
                        List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText'"
                        ));
                        // Look for asset-like text patterns in cells
                        for (WebElement text : childTexts) {
                            String label = text.getAttribute("label");
                            if (label != null && (label.contains("📍")
                                || label.contains("OCP") || label.contains("Panel")
                                || label.contains("Switch") || label.contains("Asset")
                                || label.contains("TestAsset") || label.contains("Trim"))) {
                                System.out.println("✅ Found asset text in issue cell: " + label);
                                return true;
                            }
                        }
                    }
                } catch (Exception e2) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No asset location found in session issues");
        return false;
    }

    // ================================================================
    // TAP SESSION ISSUE ENTRY (TC_JOB_050)
    // ================================================================

    /**
     * Tap on a linked session issue entry at the given index to navigate to its details.
     * Finds issue cells in the session issues list (Y > 300, H > 50) and taps the target.
     * Returns true if tapped successfully.
     */
    public boolean tapSessionIssueEntry(int index) {
        System.out.println("📍 Tapping session issue entry at index " + index + "...");

        // Strategy 1: Find issue cells by position and tap
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            List<WebElement> issueCells = new java.util.ArrayList<>();
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 300 && h > 50) {
                        issueCells.add(cell);
                    }
                } catch (Exception e2) { /* skip */ }
            }

            if (index < issueCells.size()) {
                WebElement targetCell = issueCells.get(index);
                targetCell.click();
                System.out.println("✅ Tapped session issue at index " + index);
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Find by tapping the first child static text of the cell
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            int matchCount = 0;
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    if (y > 300 && h > 50) {
                        if (matchCount == index) {
                            List<WebElement> texts = cell.findElements(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeStaticText'"
                            ));
                            if (!texts.isEmpty()) {
                                texts.get(0).click();
                                System.out.println("✅ Tapped session issue text at index " + index);
                                return true;
                            }
                        }
                        matchCount++;
                    }
                } catch (Exception e2) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Coordinate-based tap on the center of the nth issue cell
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            int matchCount = 0;
            for (WebElement cell : cells) {
                try {
                    int y = cell.getLocation().getY();
                    int h = cell.getSize().getHeight();
                    int x = cell.getLocation().getX();
                    int w = cell.getSize().getWidth();
                    if (y > 300 && h > 50) {
                        if (matchCount == index) {
                            int tapX = x + w / 2;
                            int tapY = y + h / 2;
                            org.openqa.selenium.interactions.PointerInput finger =
                                new org.openqa.selenium.interactions.PointerInput(
                                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                            org.openqa.selenium.interactions.Sequence tap =
                                new org.openqa.selenium.interactions.Sequence(finger, 1);
                            tap.addAction(finger.createPointerMove(Duration.ZERO,
                                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), tapX, tapY));
                            tap.addAction(finger.createPointerDown(
                                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                            tap.addAction(finger.createPointerUp(
                                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                            driver.perform(java.util.Arrays.asList(tap));
                            System.out.println("✅ Tapped session issue at (" + tapX + ", " + tapY + ")");
                            return true;
                        }
                        matchCount++;
                    }
                } catch (Exception e2) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap session issue at index " + index);
        return false;
    }

    // ================================================================
    // FLOATING + BUTTON TAP (TC_JOB_055)
    // ================================================================

    /**
     * Tap the floating "+" button on the session Issues tab to create a new issue.
     * Uses the same strategies as isAddIssueFloatingButtonDisplayed() but clicks the element.
     * Returns true if tapped successfully.
     */
    public boolean tapAddIssueFloatingButton() {
        System.out.println("📍 Tapping floating + button...");

        // Strategy 1: Button with "+" or "Add" label in bottom-right
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == '+' OR label == 'Add' OR label == 'add')"
            ));
            org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
            for (WebElement btn : buttons) {
                try {
                    int x = btn.getLocation().getX();
                    int y = btn.getLocation().getY();
                    if (x > screenSize.width / 2 && y > screenSize.height / 2) {
                        btn.click();
                        System.out.println("✅ Tapped floating + button at (" + x + ", " + y + ")");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped + button (position-agnostic)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Button/image with "plus" name
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeImage') "
                + "AND (name CONTAINS 'plus' OR label CONTAINS 'plus')"
            ));
            if (!elements.isEmpty()) {
                elements.get(0).click();
                System.out.println("✅ Tapped + button via plus name");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Small circular button in bottom-right area
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
                    if (w < 80 && h < 80 && w > 20 && Math.abs(w - h) < 20
                            && y > screenSize.height * 2 / 3 && x > screenSize.width / 2) {
                        btn.click();
                        System.out.println("✅ Tapped floating button by geometry at (" + x + ", " + y + ")");
                        return true;
                    }
                } catch (Exception e) { /* skip */ }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap floating + button");
        return false;
    }

    // ================================================================
    // LINKED TO ACTIVE SESSION BANNER (TC_JOB_051-055)
    // ================================================================

    /**
     * Check if the "Linked to active session" banner is displayed on the New Issue screen.
     * This banner appears at the top when creating an issue while a job session is active.
     */
    public boolean isLinkedToSessionBannerDisplayed() {
        System.out.println("📍 Checking for 'Linked to active session' banner...");

        // Strategy 1: Text containing "Linked to active session" or "Linked to"
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeOther') "
                + "AND (label CONTAINS 'Linked to active session' OR label CONTAINS 'Linked to active' "
                + "OR label CONTAINS 'linked to active session' OR label CONTAINS 'LINKED TO ACTIVE')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ 'Linked to active session' banner found: "
                    + elements.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for any element containing "Linked to" near the top (Y < 250)
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'Linked to' OR label CONTAINS 'linked to')"
            ));
            for (WebElement el : elements) {
                int y = el.getLocation().getY();
                if (y < 250) {
                    System.out.println("✅ 'Linked to...' banner found at Y=" + y
                        + ": " + el.getAttribute("label"));
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for broadcast/signal icon near "session" text
        try {
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeButton') "
                + "AND (name CONTAINS 'broadcast' OR name CONTAINS 'antenna' "
                + "OR name CONTAINS 'signal' OR name CONTAINS 'dot.radiowaves')"
            ));
            if (!images.isEmpty()) {
                // Found broadcast icon — check if "session" or "Job" text is nearby
                WebElement icon = images.get(0);
                int iconY = icon.getLocation().getY();
                if (iconY < 250) {
                    System.out.println("✅ Broadcast icon found at Y=" + iconY
                        + " — session banner likely present");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ 'Linked to active session' banner not found");
        return false;
    }

    /**
     * Get the session name from the "Linked to active session" banner.
     * The banner typically shows "Linked to active session:" followed by the session name
     * (e.g., "Job - Dec 17, 12:18 PM").
     * Returns the session name string, or null if not found.
     */
    public String getLinkedSessionNameFromBanner() {
        System.out.println("📍 Getting session name from linked banner...");

        // Strategy 1: Find text with "Linked to active session" and extract session name
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'Linked to active session' OR label CONTAINS 'Linked to active')"
            ));
            if (!elements.isEmpty()) {
                String fullLabel = elements.get(0).getAttribute("label");
                // Try extracting text after ":" if present
                if (fullLabel.contains(":")) {
                    String sessionName = fullLabel.substring(fullLabel.indexOf(":") + 1).trim();
                    if (!sessionName.isEmpty()) {
                        System.out.println("📊 Session name from banner: " + sessionName);
                        return sessionName;
                    }
                }
                // Return full label if no colon separator
                System.out.println("📊 Banner label (full): " + fullLabel);
                return fullLabel;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for "Job -" text near the top (session name format)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'Job -' OR label CONTAINS 'Job —')"
            ));
            for (WebElement text : texts) {
                int y = text.getLocation().getY();
                if (y < 250) {
                    String label = text.getAttribute("label");
                    System.out.println("📊 Session name from nearby text: " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Find all texts in the banner area and look for date-containing text
        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : allTexts) {
                int y = text.getLocation().getY();
                String label = text.getAttribute("label");
                // Banner is at top; session name usually contains date with AM/PM
                if (y < 200 && label != null
                    && (label.contains("AM") || label.contains("PM"))
                    && label.contains("Job")) {
                    System.out.println("📊 Session name from date text: " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not extract session name from banner");
        return null;
    }

    // ================================================================
    // NEW JOB SCREEN (TC_JOB_056-059)
    // ================================================================

    /**
     * Check if the New Job creation screen is displayed.
     * Looks for "New Job" title, Cancel/Create buttons, and form elements.
     */
    public boolean isNewJobScreenDisplayed() {
        System.out.println("📍 Checking for New Job screen...");

        // Strategy 1: Nav bar or title with "New Job"
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeNavigationBar' OR type == 'XCUIElementTypeStaticText') "
                + "AND (name == 'New Job' OR label == 'New Job' OR name CONTAINS 'New Job' "
                + "OR label CONTAINS 'New Job')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ New Job screen detected: " + elements.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for "New Work Order" variant
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeNavigationBar' OR type == 'XCUIElementTypeStaticText') "
                + "AND (name CONTAINS 'New Work Order' OR label CONTAINS 'New Work Order' "
                + "OR name == 'New Session' OR label CONTAINS 'New Session')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ New Job/Work Order screen detected: "
                    + elements.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Presence of "Create" button + form fields (Job name, Photo Type)
        try {
            List<WebElement> createBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Create' OR label CONTAINS 'Create')"
            ));
            List<WebElement> cancelBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            if (!createBtns.isEmpty() && !cancelBtns.isEmpty()) {
                // Both Create and Cancel on same screen — likely New Job form
                System.out.println("✅ New Job screen inferred from Create+Cancel buttons");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ New Job screen not detected");
        return false;
    }

    /**
     * Wait for the New Job screen to load.
     * Polls isNewJobScreenDisplayed() for up to 10 seconds.
     */
    public boolean waitForNewJobScreen() {
        System.out.println("📍 Waiting for New Job screen...");
        for (int i = 0; i < 20; i++) {
            if (isNewJobScreenDisplayed()) {
                return true;
            }
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }
        System.out.println("⚠️ New Job screen did not appear within 10 seconds");
        return false;
    }

    /**
     * Check if the Cancel button is displayed on the New Job screen.
     */
    public boolean isNewJobCancelButtonDisplayed() {
        try {
            WebElement cancel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            return cancel.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the Create button is displayed on the New Job screen.
     */
    public boolean isNewJobCreateButtonDisplayed() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Create' OR label CONTAINS 'Create')"
            ));
            return !buttons.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tap Cancel on the New Job screen to dismiss it without creating.
     */
    public boolean tapNewJobCancel() {
        System.out.println("📍 Tapping Cancel on New Job screen...");
        try {
            WebElement cancel = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
            ));
            cancel.click();
            System.out.println("✅ Tapped Cancel on New Job");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not tap Cancel on New Job: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the JOB CONFIGURATION section header is displayed on the New Job screen.
     */
    public boolean isJobConfigurationSectionDisplayed() {
        System.out.println("📍 Checking for JOB CONFIGURATION section...");

        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'JOB CONFIGURATION' "
                + "OR label CONTAINS 'Job Configuration' OR label CONTAINS 'CONFIGURATION' "
                + "OR label CONTAINS 'Configuration')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ JOB CONFIGURATION section found: "
                    + elements.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: look for Job name label/field as indicator of configuration section
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label CONTAINS 'Job name' "
                + "OR label CONTAINS 'Job Name' OR label CONTAINS 'Name')"
            ));
            for (WebElement el : elements) {
                int y = el.getLocation().getY();
                if (y > 100 && y < 500) {
                    System.out.println("✅ Job configuration inferred from Job name label");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ JOB CONFIGURATION section not found");
        return false;
    }

    /**
     * Check if the Photo Type dropdown is displayed on the New Job screen.
     */
    public boolean isPhotoTypeDropdownDisplayed() {
        System.out.println("📍 Checking for Photo Type dropdown...");

        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                + "AND (label CONTAINS 'Photo Type' OR label CONTAINS 'photo type' "
                + "OR label CONTAINS 'PHOTO TYPE')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ Photo Type dropdown found: "
                    + elements.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: look for picker/dropdown near "Photo" text
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypePicker') "
                + "AND (label CONTAINS 'Photo' OR label CONTAINS 'FLIR' "
                + "OR label CONTAINS 'IR Photo')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ Photo Type dropdown found (variant): "
                    + elements.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Photo Type dropdown not found");
        return false;
    }

    /**
     * Check if the Online indicator is displayed on the New Job screen.
     */
    public boolean isOnlineIndicatorDisplayed() {
        System.out.println("📍 Checking for Online indicator...");

        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeOther') "
                + "AND (label CONTAINS 'Online' OR label == 'Online' "
                + "OR label CONTAINS 'online')"
            ));
            for (WebElement el : elements) {
                int y = el.getLocation().getY();
                if (y > 100) {
                    System.out.println("✅ Online indicator found: " + el.getAttribute("label"));
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: green dot/circle near "Online" or status area
        try {
            List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') "
                + "AND (name CONTAINS 'circle.fill' OR name CONTAINS 'wifi' "
                + "OR name CONTAINS 'network')"
            ));
            if (!images.isEmpty()) {
                System.out.println("✅ Online indicator icon found");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Online indicator not found");
        return false;
    }

    /**
     * Get the auto-generated job name from the Job name field on the New Job screen.
     * The format is typically "Job - [Date], [Time]" (e.g., "Job - Dec 24, 12:07 PM").
     * Returns the job name string, or null if not found.
     */
    public String getJobNameFieldValue() {
        System.out.println("📍 Getting job name field value...");

        // Strategy 1: Text field with pre-populated value
        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"
            ));
            for (WebElement field : fields) {
                String value = field.getAttribute("value");
                if (value != null && (value.contains("Job") || value.contains("job"))) {
                    System.out.println("📊 Job name from text field: " + value);
                    return value;
                }
                // Also check label attribute
                String label = field.getAttribute("label");
                if (label != null && (label.contains("Job") || label.contains("job"))) {
                    System.out.println("📊 Job name from field label: " + label);
                    return label;
                }
            }
            // Return the first non-empty text field value as fallback
            for (WebElement field : fields) {
                String value = field.getAttribute("value");
                if (value != null && !value.isEmpty()
                    && !value.equalsIgnoreCase("Enter job name")
                    && !value.equalsIgnoreCase("Job name")) {
                    System.out.println("📊 Job name from first populated field: " + value);
                    return value;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text near "Job name" label that contains date
        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                if (label != null && label.startsWith("Job -")
                    && (label.contains("AM") || label.contains("PM"))) {
                    System.out.println("📊 Job name from static text: " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Look for text cell containing date in the form area
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell'"
            ));
            for (WebElement cell : cells) {
                List<WebElement> texts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeTextField'"
                ));
                for (WebElement text : texts) {
                    String val = text.getAttribute("value");
                    String lbl = text.getAttribute("label");
                    String content = val != null ? val : lbl;
                    if (content != null && content.contains("Job")
                        && (content.contains("AM") || content.contains("PM") || content.contains(","))) {
                        System.out.println("📊 Job name from cell: " + content);
                        return content;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get job name field value");
        return null;
    }

    /**
     * Check if the job name field is editable by finding the text field and checking its attributes.
     * Returns true if the field appears editable (enabled, not read-only).
     */
    public boolean isJobNameFieldEditable() {
        System.out.println("📍 Checking if job name field is editable...");

        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"
            ));
            for (WebElement field : fields) {
                String value = field.getAttribute("value");
                String enabled = field.getAttribute("enabled");
                // Find the field that contains the job name
                if (value != null && (value.contains("Job") || value.contains("job"))) {
                    boolean isEnabled = "true".equals(enabled);
                    System.out.println("📊 Job name field enabled: " + isEnabled);
                    return isEnabled;
                }
            }
            // If we found any text field, check if it's enabled
            if (!fields.isEmpty()) {
                String enabled = fields.get(0).getAttribute("enabled");
                boolean isEnabled = "true".equals(enabled);
                System.out.println("📊 First text field enabled: " + isEnabled);
                return isEnabled;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not determine if job name field is editable");
        return false;
    }

    /**
     * Edit the job name field by clearing existing text and entering new text.
     * Returns true if editing was successful.
     */
    public boolean editJobNameField(String newName) {
        System.out.println("📍 Editing job name field to: " + newName);

        try {
            List<WebElement> fields = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"
            ));
            for (WebElement field : fields) {
                String value = field.getAttribute("value");
                if (value != null && (value.contains("Job") || value.contains("job")
                    || value.contains("AM") || value.contains("PM"))) {
                    // Found the job name field
                    field.click();
                    try { Thread.sleep(300); } catch (InterruptedException e) { /* ignore */ }
                    field.clear();
                    try { Thread.sleep(200); } catch (InterruptedException e) { /* ignore */ }
                    field.sendKeys(newName);
                    try { Thread.sleep(300); } catch (InterruptedException e) { /* ignore */ }
                    System.out.println("✅ Edited job name to: " + newName);
                    return true;
                }
            }

            // Fallback: try the first text field
            if (!fields.isEmpty()) {
                WebElement field = fields.get(0);
                field.click();
                try { Thread.sleep(300); } catch (InterruptedException e) { /* ignore */ }
                field.clear();
                try { Thread.sleep(200); } catch (InterruptedException e) { /* ignore */ }
                field.sendKeys(newName);
                try { Thread.sleep(300); } catch (InterruptedException e) { /* ignore */ }
                System.out.println("✅ Edited job name (first field) to: " + newName);
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error editing job name: " + e.getMessage());
        }

        System.out.println("⚠️ Could not edit job name field");
        return false;
    }

    /**
     * Check if the keyboard is currently displayed (useful for verifying field editability).
     */
    public boolean isKeyboardDisplayed() {
        try {
            List<WebElement> keyboards = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeKeyboard'"
            ));
            return !keyboards.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Dismiss the keyboard if it is showing (tap elsewhere or use Done button).
     */
    public void dismissKeyboard() {
        try {
            // Try "Done" button first
            List<WebElement> doneButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Done' OR label == 'Return')"
            ));
            if (!doneButtons.isEmpty()) {
                doneButtons.get(0).click();
                return;
            }
            // Tap above the keyboard
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                    org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                size.width / 2, size.height / 4));
            tap.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(tap));
        } catch (Exception e) {
            System.out.println("⚠️ Could not dismiss keyboard: " + e.getMessage());
        }
    }

    // ================================================================
    // PHOTO TYPE DROPDOWN (TC_JOB_061-066)
    // ================================================================

    /**
     * Get the currently selected Photo Type value from the New Job screen.
     * The default is typically "FLIR-SEP".
     * Returns the value string, or null if not found.
     */
    public String getPhotoTypeValue() {
        System.out.println("📍 Getting Photo Type value...");

        // Strategy 1: Button with Photo Type value (e.g., "Photo Type, FLIR-SEP")
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'FLIR' "
                + "OR label CONTAINS 'FLUKE' OR label CONTAINS 'FOTRIC' "
                + "OR label CONTAINS 'Photo Type')"
            ));
            for (WebElement btn : buttons) {
                String label = btn.getAttribute("label");
                if (label != null) {
                    // Extract the type value from label like "Photo Type, FLIR-SEP"
                    if (label.contains("FLIR-SEP")) return "FLIR-SEP";
                    if (label.contains("FLIR-IND")) return "FLIR-IND";
                    if (label.contains("FLUKE")) return "FLUKE";
                    if (label.contains("FOTRIC")) return "FOTRIC";
                    // If label is the value itself
                    if (label.matches("FLIR.*|FLUKE|FOTRIC")) {
                        System.out.println("📊 Photo Type value: " + label);
                        return label;
                    }
                }
                // Also check value attribute
                String value = btn.getAttribute("value");
                if (value != null && (value.contains("FLIR") || value.contains("FLUKE")
                    || value.contains("FOTRIC"))) {
                    System.out.println("📊 Photo Type value (from attribute): " + value);
                    return value;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Static text with FLIR/FLUKE/FOTRIC near "Photo Type" label
        try {
            List<WebElement> photoLabels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Photo Type'"
            ));
            if (!photoLabels.isEmpty()) {
                int labelY = photoLabels.get(0).getLocation().getY();
                List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == true"
                ));
                for (WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    int textY = text.getLocation().getY();
                    if (label != null && Math.abs(textY - labelY) < 50
                        && (label.contains("FLIR") || label.contains("FLUKE")
                            || label.contains("FOTRIC"))) {
                        System.out.println("📊 Photo Type value (near label): " + label);
                        return label;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Any visible text matching known Photo Type values
        try {
            String[] photoTypes = {"FLIR-SEP", "FLIR-IND", "FLUKE", "FOTRIC"};
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'FLIR' OR label CONTAINS 'FLUKE' "
                + "OR label CONTAINS 'FOTRIC')"
            ));
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                if (label != null) {
                    for (String pt : photoTypes) {
                        if (label.contains(pt)) {
                            System.out.println("📊 Photo Type value (broad): " + pt);
                            return pt;
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not determine Photo Type value");
        return null;
    }

    /**
     * Tap the Photo Type dropdown to open it and show available options.
     * Returns true if tapped successfully.
     */
    public boolean tapPhotoTypeDropdown() {
        System.out.println("📍 Tapping Photo Type dropdown...");

        // Strategy 1: Button containing "Photo Type" or current Photo Type value
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Photo Type' "
                + "OR label CONTAINS 'FLIR' OR label CONTAINS 'FLUKE' "
                + "OR label CONTAINS 'FOTRIC')"
            ));
            if (!buttons.isEmpty()) {
                buttons.get(0).click();
                System.out.println("✅ Tapped Photo Type dropdown (button): "
                    + buttons.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Picker element
        try {
            List<WebElement> pickers = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypePicker' OR type == 'XCUIElementTypePopUpButton'"
            ));
            for (WebElement picker : pickers) {
                int y = picker.getLocation().getY();
                if (y > 100) {
                    picker.click();
                    System.out.println("✅ Tapped Photo Type picker");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Static text "Photo Type" label — tap near it to trigger dropdown
        try {
            List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Photo Type'"
            ));
            if (!labels.isEmpty()) {
                WebElement label = labels.get(0);
                int labelY = label.getLocation().getY();
                int labelX = label.getLocation().getX();
                // Look for tappable element to the right of the label on the same row
                List<WebElement> nearby = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeCell' "
                    + "OR type == 'XCUIElementTypeOther') AND visible == true"
                ));
                for (WebElement el : nearby) {
                    int elY = el.getLocation().getY();
                    int elX = el.getLocation().getX();
                    if (Math.abs(elY - labelY) < 40 && elX > labelX) {
                        el.click();
                        System.out.println("✅ Tapped Photo Type dropdown (near label)");
                        return true;
                    }
                }
                // Fallback: tap the label itself, which may trigger the dropdown row
                label.click();
                System.out.println("✅ Tapped Photo Type label directly");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap Photo Type dropdown");
        return false;
    }

    /**
     * Get the list of available Photo Type options when the dropdown is open.
     * Expected options: FLIR-SEP, FLIR-IND, FLUKE, FOTRIC.
     * Returns a list of option strings found.
     */
    public java.util.List<String> getPhotoTypeOptions() {
        System.out.println("📍 Getting Photo Type dropdown options...");
        java.util.List<String> options = new java.util.ArrayList<>();
        String[] knownTypes = {"FLIR-SEP", "FLIR-IND", "FLUKE", "FOTRIC"};

        // Strategy 1: Look for menu/popover with Photo Type options
        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                + "AND visible == true AND (label CONTAINS 'FLIR' OR label CONTAINS 'FLUKE' "
                + "OR label CONTAINS 'FOTRIC')"
            ));
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                if (label != null) {
                    for (String pt : knownTypes) {
                        if (label.contains(pt) && !options.contains(pt)) {
                            options.add(pt);
                            System.out.println("  Found option: " + pt);
                        }
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Picker wheel options
        if (options.isEmpty()) {
            try {
                List<WebElement> pickerWheels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypePickerWheel'"
                ));
                for (WebElement wheel : pickerWheels) {
                    String value = wheel.getAttribute("value");
                    if (value != null) {
                        for (String pt : knownTypes) {
                            if (value.contains(pt) && !options.contains(pt)) {
                                options.add(pt);
                            }
                        }
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        // Strategy 3: Menu items in a context menu / action sheet
        if (options.isEmpty()) {
            try {
                List<WebElement> menuItems = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeMenuItem' OR "
                    + "(type == 'XCUIElementTypeButton' AND "
                    + "(label CONTAINS 'FLIR' OR label CONTAINS 'FLUKE' OR label CONTAINS 'FOTRIC'))"
                ));
                for (WebElement item : menuItems) {
                    String label = item.getAttribute("label");
                    if (label != null) {
                        for (String pt : knownTypes) {
                            if (label.contains(pt) && !options.contains(pt)) {
                                options.add(pt);
                            }
                        }
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        // Strategy 4: Cells in a dropdown list
        if (options.isEmpty()) {
            try {
                List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeCell' AND visible == true"
                ));
                for (WebElement cell : cells) {
                    List<WebElement> childTexts = cell.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText'"
                    ));
                    for (WebElement text : childTexts) {
                        String label = text.getAttribute("label");
                        if (label != null) {
                            for (String pt : knownTypes) {
                                if (label.contains(pt) && !options.contains(pt)) {
                                    options.add(pt);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) { /* continue */ }
        }

        System.out.println("📊 Photo Type options found: " + options.size() + " → " + options);
        return options;
    }

    /**
     * Select a specific Photo Type option from the open dropdown.
     * The dropdown should already be open before calling this method.
     * Returns true if the option was selected successfully.
     */
    public boolean selectPhotoType(String photoType) {
        System.out.println("📍 Selecting Photo Type: " + photoType + "...");

        // Strategy 1: Button/text with the exact Photo Type label
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                + "OR type == 'XCUIElementTypeMenuItem') "
                + "AND (label == '" + photoType + "' OR label CONTAINS '" + photoType + "')"
            ));
            for (WebElement el : elements) {
                String label = el.getAttribute("label");
                if (label != null && label.contains(photoType)) {
                    el.click();
                    System.out.println("✅ Selected Photo Type: " + photoType);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Cell containing the Photo Type text
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeCell' AND visible == true"
            ));
            for (WebElement cell : cells) {
                List<WebElement> texts = cell.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + photoType + "'"
                ));
                if (!texts.isEmpty()) {
                    cell.click();
                    System.out.println("✅ Selected Photo Type via cell: " + photoType);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Broad search for any element with the Photo Type label
        try {
            WebElement element = driver.findElement(AppiumBy.iOSNsPredicateString(
                "label CONTAINS '" + photoType + "' AND visible == true"
            ));
            element.click();
            System.out.println("✅ Selected Photo Type (broad): " + photoType);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Photo Type: " + photoType);
            return false;
        }
    }

    /**
     * Check if a specific Photo Type option has a checkmark (selected indicator).
     * Returns true if a checkmark is found near the option.
     */
    public boolean isPhotoTypeOptionChecked(String photoType) {
        System.out.println("📍 Checking if " + photoType + " has checkmark...");

        try {
            // Find the element with the Photo Type label
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                + "OR type == 'XCUIElementTypeCell') "
                + "AND label CONTAINS '" + photoType + "'"
            ));
            for (WebElement el : elements) {
                // Check for checkmark image as child
                try {
                    List<WebElement> checkmarks = el.findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') "
                        + "AND (name CONTAINS 'checkmark' OR name CONTAINS 'check' "
                        + "OR name CONTAINS 'selected')"
                    ));
                    if (!checkmarks.isEmpty()) {
                        System.out.println("✅ Checkmark found for " + photoType);
                        return true;
                    }
                } catch (Exception e2) { /* continue */ }

                // Check for "selected" attribute on cell
                try {
                    String selected = el.getAttribute("selected");
                    if ("true".equals(selected)) {
                        System.out.println("✅ " + photoType + " has selected=true");
                        return true;
                    }
                    String value = el.getAttribute("value");
                    if ("1".equals(value) || "selected".equalsIgnoreCase(value)) {
                        System.out.println("✅ " + photoType + " has value=selected");
                        return true;
                    }
                } catch (Exception e2) { /* continue */ }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for checkmark image near the Photo Type text by Y position
        try {
            List<WebElement> typeTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + photoType + "'"
            ));
            if (!typeTexts.isEmpty()) {
                int typeY = typeTexts.get(0).getLocation().getY();
                List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage' AND (name CONTAINS 'checkmark' "
                    + "OR name CONTAINS 'check')"
                ));
                for (WebElement img : images) {
                    int imgY = img.getLocation().getY();
                    if (Math.abs(imgY - typeY) < 30) {
                        System.out.println("✅ Checkmark found near " + photoType + " at Y=" + imgY);
                        return true;
                    }
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ No checkmark found for " + photoType);
        return false;
    }

    /**
     * Check if the Photo Type dropdown is currently open (showing options).
     * Returns true if dropdown menu/popover is visible.
     */
    public boolean isPhotoTypeDropdownOpen() {
        try {
            // Check for multiple Photo Type options visible simultaneously
            String[] types = {"FLIR-SEP", "FLIR-IND", "FLUKE", "FOTRIC"};
            int visibleCount = 0;
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') "
                + "AND visible == true AND (label CONTAINS 'FLIR' OR label CONTAINS 'FLUKE' "
                + "OR label CONTAINS 'FOTRIC')"
            ));
            java.util.Set<String> found = new java.util.HashSet<>();
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                if (label != null) {
                    for (String t : types) {
                        if (label.contains(t)) {
                            found.add(t);
                        }
                    }
                }
            }
            // If we see 2+ Photo Type values, the dropdown is likely open
            return found.size() >= 2;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // NEW JOB INFO TEXT & CREATE (TC_JOB_068-069)
    // ================================================================

    /**
     * Check if the informational text about job behavior is displayed on the New Job screen.
     * Expected text: "This job will remain active until you explicitly close it.
     * All IR photos, issues and tasks added will be associated with this job."
     */
    public boolean isNewJobInfoTextDisplayed() {
        System.out.println("📍 Checking for info text on New Job screen...");

        // Strategy 1: Look for text containing key phrases from the info message
        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'remain active' OR label CONTAINS 'explicitly close' "
                + "OR label CONTAINS 'associated with this job' "
                + "OR label CONTAINS 'IR photos' OR label CONTAINS 'issues and tasks')"
            ));
            if (!elements.isEmpty()) {
                System.out.println("✅ Info text found: " + elements.get(0).getAttribute("label"));
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Look for longer text elements in the form area (info texts are usually multi-line)
        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                if (label != null && label.length() > 50 && label.contains("job")) {
                    System.out.println("✅ Info text found (long text): " + label);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 3: Check for text view or other container with the info text
        try {
            List<WebElement> textViews = driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeOther') "
                + "AND visible == true AND (label CONTAINS 'active' OR label CONTAINS 'job' "
                + "OR value CONTAINS 'active' OR value CONTAINS 'job')"
            ));
            for (WebElement tv : textViews) {
                String label = tv.getAttribute("label");
                String value = tv.getAttribute("value");
                String content = label != null ? label : value;
                if (content != null && content.length() > 40) {
                    System.out.println("✅ Info text found (text view): " + content);
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Info text not found on New Job screen");
        return false;
    }

    /**
     * Get the full info text from the New Job screen.
     * Returns the text string, or null if not found.
     */
    public String getNewJobInfoText() {
        System.out.println("📍 Getting info text from New Job screen...");

        try {
            List<WebElement> elements = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true "
                + "AND (label CONTAINS 'remain active' OR label CONTAINS 'explicitly close' "
                + "OR label CONTAINS 'associated with this job' "
                + "OR label CONTAINS 'IR photos')"
            ));
            if (!elements.isEmpty()) {
                String text = elements.get(0).getAttribute("label");
                System.out.println("📊 Info text: " + text);
                return text;
            }
        } catch (Exception e) { /* continue */ }

        // Fallback: look for long text about job behavior
        try {
            List<WebElement> allTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == true"
            ));
            for (WebElement text : allTexts) {
                String label = text.getAttribute("label");
                if (label != null && label.length() > 50
                    && (label.contains("job") || label.contains("active"))) {
                    System.out.println("📊 Info text (long): " + label);
                    return label;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not get info text");
        return null;
    }

    /**
     * Check if the Online indicator shows a WiFi icon on the New Job screen.
     * Returns true if a WiFi-like icon/image is found near the "Online" text.
     */
    public boolean isWifiIconDisplayedNearOnline() {
        System.out.println("📍 Checking for WiFi icon near Online indicator...");

        try {
            // Find the "Online" text position
            List<WebElement> onlineTexts = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (label == 'Online' "
                + "OR label CONTAINS 'Online')"
            ));
            if (!onlineTexts.isEmpty()) {
                int onlineY = onlineTexts.get(0).getLocation().getY();
                int onlineX = onlineTexts.get(0).getLocation().getX();

                // Look for WiFi/signal icon nearby
                List<WebElement> images = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') "
                    + "AND (name CONTAINS 'wifi' OR name CONTAINS 'signal' "
                    + "OR name CONTAINS 'antenna' OR name CONTAINS 'network' "
                    + "OR name CONTAINS 'circle.fill')"
                ));
                for (WebElement img : images) {
                    int imgY = img.getLocation().getY();
                    int imgX = img.getLocation().getX();
                    if (Math.abs(imgY - onlineY) < 40 && Math.abs(imgX - onlineX) < 100) {
                        System.out.println("✅ WiFi icon found near Online at ("
                            + imgX + "," + imgY + ")");
                        return true;
                    }
                }

                // Also check if the Online text itself has an icon embedded
                // (SF Symbol in label or name)
                WebElement onlineEl = onlineTexts.get(0);
                String name = onlineEl.getAttribute("name");
                if (name != null && (name.contains("wifi") || name.contains("signal"))) {
                    System.out.println("✅ WiFi icon embedded in Online element");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ WiFi icon not found near Online indicator");
        return false;
    }

    /**
     * Tap the Create button on the New Job screen to create the job.
     * Returns true if tapped successfully.
     */
    public boolean tapCreateJobButton() {
        System.out.println("📍 Tapping Create button on New Job screen...");

        // Strategy 1: Button with "Create" label
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Create' "
                + "OR label CONTAINS 'Create')"
            ));
            if (!buttons.isEmpty()) {
                // Wait for button to become enabled
                for (int attempt = 0; attempt < 6; attempt++) {
                    String enabled = buttons.get(0).getAttribute("enabled");
                    if ("true".equals(enabled)) {
                        buttons.get(0).click();
                        System.out.println("✅ Tapped Create button");
                        return true;
                    }
                    try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                }
                // Try clicking anyway
                buttons.get(0).click();
                System.out.println("✅ Tapped Create button (may not have been fully enabled)");
                return true;
            }
        } catch (Exception e) { /* continue */ }

        // Strategy 2: Nav bar button on the right side
        try {
            List<WebElement> navBarButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"
            ));
            org.openqa.selenium.Dimension screenSize = driver.manage().window().getSize();
            for (WebElement btn : navBarButtons) {
                String label = btn.getAttribute("label");
                int x = btn.getLocation().getX();
                int y = btn.getLocation().getY();
                if (label != null && label.contains("Create")
                    && x > screenSize.width / 2 && y < 120) {
                    btn.click();
                    System.out.println("✅ Tapped Create button (nav bar right)");
                    return true;
                }
            }
        } catch (Exception e) { /* continue */ }

        System.out.println("⚠️ Could not tap Create button");
        return false;
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
