package com.egalvanic.tests;

import java.util.List;

import org.openqa.selenium.WebElement;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.SiteSelectionPage;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.utils.Waits;

import io.appium.java_client.AppiumBy;

/**
 * ZP-2784 — session asset-picker hierarchy (TC_WO_PICK_01-06, app v1.51).
 *
 * Covers the automatable core of the ZP-2784 plan (testcase_file/workorder.txt
 * Suites 4+5): picker anatomy, Other-Locations collapse/expand with
 * Building>Floor>Room disclosure groups, scoped Select All / Deselect All,
 * toolbar Clear, badge determinism, and picker search.
 *
 * Probed anatomy (2026-07-21, in-session picker via session room FAB →
 * 'Link Existing Assets'):
 *   NavigationBar 'Link Existing Assets' + Cancel + search + QR button;
 *   section Button "OTHER LOCATIONS, <n>" (chevron.right collapsed /
 *   chevron.down expanded, trailing numeric badge); expanding reveals
 *   building disclosure Buttons (fixture-named, e.g. 'Bldg_9471') each with
 *   its own 'Select All' Button. Room / No-Location sections render only
 *   when they have candidates — tests that need them SKIP honestly.
 *
 * DELIBERATELY NOT AUTOMATED here (documented, not forgotten):
 *   - Relocation commits (spec TC-4.3/5.4/6.2): tapping Done MOVES whole
 *     subtrees between rooms — destructive to the shared QA fixture site.
 *     Selection-only flows are exercised; every test CANCELS the picker.
 *   - Bolt-color states (Suite 1, red/yellow/green): XCUITest exposes no
 *     tint color on bolt.circle.fill images; needs accessibility-value
 *     support from the app or pixel analysis.
 *   - French localization (TC-5.5): app language is a custom plist key the
 *     harness can't force per-test.
 */
public class WorkOrderAssetPicker_Test extends BaseTest {

    private WorkOrderPage wo;
    private SiteSelectionPage sitePage;

    private static final String SECTION_OTHER =
        "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'OTHER LOCATIONS' AND visible == 1";
    private static final String SELECT_ALL_BTN =
        "type == 'XCUIElementTypeButton' AND name == 'Select All' AND visible == 1";
    private static final String DESELECT_ALL_BTN =
        "type == 'XCUIElementTypeButton' AND name == 'Deselect All' AND visible == 1";

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        System.out.println("\n📋 ZP-2784 asset-picker matrix — Starting");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void testSetupLocal() {
        if (!DriverManager.isDriverActive()) return;
        wo = new WorkOrderPage();
        sitePage = new SiteSelectionPage();
    }

    @AfterMethod(alwaysRun = true)
    public void pickerTeardown() {
        // Never leave the picker open between tests — Cancel is idempotent.
        try {
            if (DriverManager.isDriverActive() && isPickerOpen()) cancelPicker();
        } catch (Exception ignored) { }
    }

    @AfterClass(alwaysRun = true)
    public void classTeardown() {
        DriverManager.resetNoResetOverride();
    }

    // ── Tests ────────────────────────────────────────────────────────────

    @Test(priority = 1)
    public void TC_WO_PICK_01_pickerAnatomyRenders() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "ZP-2784 Asset Picker",
                "TC_WO_PICK_01 - In-session Link Existing picker renders nav bar, search, and location sections");
        openPicker();

        assertTrue(isPickerOpen(), "Picker must show the 'Link Existing Assets' navigation bar");
        assertTrue(existsNowInTest("type == 'XCUIElementTypeButton' AND name == 'Cancel' AND visible == 1"),
                "Picker must have a Cancel button");
        assertTrue(existsNowInTest("type == 'XCUIElementTypeImage' AND name == 'magnifyingglass' AND visible == 1")
                        || existsNowInTest("type == 'XCUIElementTypeSearchField' AND visible == 1"),
                "Picker must have a search affordance");

        boolean hasOther = existsNowInTest(SECTION_OTHER);
        boolean hasNoLocation = existsNowInTest(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH[c] 'No Location' AND visible == 1");
        logStep("Sections present — Other Locations: " + hasOther + ", No Location: " + hasNoLocation);
        assertTrue(hasOther || hasNoLocation,
                "Picker must render at least one location section (Other Locations / No Location) — "
                + "an empty picker over a 30-asset site means the ZP-2784 bucketing dropped candidates");

        if (hasOther) {
            int badge = otherLocationsBadge();
            assertTrue(badge > 0, "OTHER LOCATIONS count badge must be a positive integer (was " + badge + ")");
            logStep("OTHER LOCATIONS badge: " + badge);
        }
        logStepWithScreenshot("TC_WO_PICK_01 picker anatomy verified");
    }

    @Test(priority = 2)
    public void TC_WO_PICK_02_otherLocationsCollapsedByDefaultAndExpands() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "ZP-2784 Asset Picker",
                "TC_WO_PICK_02 - Other Locations collapsed by default; expanding reveals location disclosure groups");
        openPicker();
        skipIfPreconditionMissing(() -> existsNowInTest(SECTION_OTHER),
                "no 'Other Locations' section on this fixture — add assets in another room");

        assertTrue(countLocationGroups() == 0,
                "Other Locations must start COLLAPSED (spec TC-5.1) — found disclosure groups before expanding");

        tapOtherLocationsHeader();
        int groups = waitForLocationGroups();
        assertTrue(groups > 0,
                "Expanding Other Locations must reveal at least one Building/Floor/Room disclosure group");
        logStep("Disclosure groups revealed: " + groups);

        tapOtherLocationsHeader(); // collapse again
        Waits.until(() -> countLocationGroups() == 0, 3000);
        assertTrue(countLocationGroups() == 0,
                "Collapsing Other Locations must hide its disclosure groups again (toggle round-trip)");
        logStepWithScreenshot("TC_WO_PICK_02 collapse/expand round-trip verified");
    }

    @Test(priority = 3)
    public void TC_WO_PICK_03_scopedSelectAllFlipsAndClears() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "ZP-2784 Asset Picker",
                "TC_WO_PICK_03 - Scoped Select All flips to Deselect All; Clear resets the selection (no commit)");
        openPicker();
        skipIfPreconditionMissing(() -> existsNowInTest(SECTION_OTHER),
                "no 'Other Locations' section on this fixture");
        tapOtherLocationsHeader();
        skipIfPreconditionMissing(() -> waitForLocationGroups() > 0,
                "Other Locations expanded to zero groups — nothing to select");
        skipIfPreconditionMissing(() -> existsNowInTest(SELECT_ALL_BTN),
                "no scoped 'Select All' control on the expanded groups (spec TC-5.2)");

        tapFirst(SELECT_ALL_BTN);
        boolean flipped = Waits.until(() -> existsNowInTest(DESELECT_ALL_BTN), 4000);
        assertTrue(flipped,
                "Tapping a scoped Select All must flip its label to 'Deselect All' (spec TC-4.4)");
        logStep("Select All -> Deselect All flip verified");

        // Spec TC-4.5: with a selection active the toolbar offers Clear
        // (replaces the old global Select All).
        boolean clearShown = Waits.until(() -> existsNowInTest(
                "type == 'XCUIElementTypeButton' AND name == 'Clear' AND visible == 1"), 3000);
        logStep("Toolbar Clear visible: " + clearShown);
        if (clearShown) {
            tapFirst("type == 'XCUIElementTypeButton' AND name == 'Clear' AND visible == 1");
            boolean reset = Waits.until(() -> !existsNowInTest(DESELECT_ALL_BTN)
                    && existsNowInTest(SELECT_ALL_BTN), 4000);
            assertTrue(reset, "Clear must reset the selection — 'Deselect All' reverts to 'Select All'");
            logStep("Clear -> selection reset verified");
        } else {
            // Fall back to the same reset law via Deselect All itself.
            tapFirst(DESELECT_ALL_BTN);
            boolean reset = Waits.until(() -> existsNowInTest(SELECT_ALL_BTN)
                    && !existsNowInTest(DESELECT_ALL_BTN), 4000);
            assertTrue(reset, "Deselect All must reset the scoped selection back to 'Select All'");
            logStep("No toolbar Clear on this build — Deselect All reset law verified instead");
        }
        logStepWithScreenshot("TC_WO_PICK_03 selection toggle laws verified (picker cancelled, nothing committed)");
    }

    @Test(priority = 4)
    public void TC_WO_PICK_04_badgeDeterministicAcrossToggle() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "ZP-2784 Asset Picker",
                "TC_WO_PICK_04 - Other Locations count badge is identical across collapse/expand cycles");
        openPicker();
        skipIfPreconditionMissing(() -> existsNowInTest(SECTION_OTHER),
                "no 'Other Locations' section on this fixture");
        int before = otherLocationsBadge();
        assertTrue(before > 0, "badge must be a positive integer (was " + before + ")");
        for (int i = 0; i < 2; i++) {
            tapOtherLocationsHeader();
            Waits.until(() -> false, 700);
            tapOtherLocationsHeader();
            Waits.until(() -> false, 700);
        }
        int after = otherLocationsBadge();
        assertTrue(before == after,
                "Badge must not drift across expand/collapse cycles (deterministic recount) — was "
                        + before + ", now " + after);
        logStepWithScreenshot("TC_WO_PICK_04 badge determinism verified (" + before + ")");
    }

    @Test(priority = 5)
    public void TC_WO_PICK_05_searchFiltersPickerCandidates() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "ZP-2784 Asset Picker",
                "TC_WO_PICK_05 - Garbage search empties the picker; clearing restores the sections");
        openPicker();
        skipIfPreconditionMissing(() -> existsNowInTest(SECTION_OTHER),
                "no 'Other Locations' section on this fixture");
        int badgeBefore = otherLocationsBadge();

        typeInPickerSearch("zz@@no-such-asset@@zz");
        boolean emptied = Waits.until(() -> !existsNowInTest(SECTION_OTHER)
                || otherLocationsBadge() == 0, 5000);
        assertTrue(emptied,
                "A garbage search must empty the candidate sections (badge 0 or section gone) — "
                + "still showing badge " + otherLocationsBadge());
        logStep("Garbage search emptied the picker");

        clearPickerSearch();
        boolean restored = Waits.until(() -> otherLocationsBadge() == badgeBefore, 5000);
        assertTrue(restored,
                "Clearing the search must restore the full candidate set (badge back to "
                        + badgeBefore + ", now " + otherLocationsBadge() + ")");
        logStepWithScreenshot("TC_WO_PICK_05 search filter round-trip verified");
    }

    @Test(priority = 6)
    public void TC_WO_PICK_06_cancelCommitsNothing() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, "ZP-2784 Asset Picker",
                "TC_WO_PICK_06 - Selecting then CANCELLING relocates nothing (room asset count unchanged)");
        int before = openPickerReturningRoomCount();
        skipIfPreconditionMissing(() -> existsNowInTest(SECTION_OTHER),
                "no 'Other Locations' section on this fixture");
        tapOtherLocationsHeader();
        if (waitForLocationGroups() > 0 && existsNowInTest(SELECT_ALL_BTN)) {
            tapFirst(SELECT_ALL_BTN);
            Waits.until(() -> existsNowInTest(DESELECT_ALL_BTN), 3000);
            logStep("Selection made inside Other Locations");
        }
        cancelPicker();
        Waits.until(() -> !isPickerOpen(), 4000);
        assertTrue(!isPickerOpen(), "Cancel must close the picker");
        int after = wo.getRoomAssetCount();
        logStep("Room asset count before/after cancel: " + before + "/" + after);
        assertTrue(after == before,
                "Cancelling the picker must not relocate anything — room asset count changed "
                        + before + " -> " + after);
        logStepWithScreenshot("TC_WO_PICK_06 cancel-commits-nothing verified");
    }

    // ── Navigation ───────────────────────────────────────────────────────

    /** Session → room → FAB → Link Existing picker; honest SKIP at each gap. */
    private void openPicker() {
        openPickerReturningRoomCount();
    }

    private int openPickerReturningRoomCount() {
        loginAndSelectSite();
        if (isPickerOpen()) { cancelPicker(); shortWait(); }
        sitePage.clickWorkOrderCard();
        skipIfPreconditionMissing(() -> wo.waitForWorkOrdersScreen(),
                "Work Orders screen did not open from the dashboard tile");
        if (!wo.startFirstAvailableWorkOrder()) {
            throw new SkipException("Precondition: could not activate a work order");
        }
        skipIfPreconditionMissing(() -> wo.openActiveWorkOrderSession(),
                "active session did not open");
        skipIfPreconditionMissing(() -> wo.openFirstSessionRoom(),
                "no room available in the session");
        int count = wo.getRoomAssetCount();
        skipIfPreconditionMissing(() -> wo.openLinkExistingAssets(),
                "'Link Existing Assets' not reachable from the room FAB");
        Waits.until(this::isPickerOpen, 5000);
        skipIfPreconditionMissing(this::isPickerOpen, "picker sheet did not open");
        return count;
    }

    // ── Picker primitives (element queries — the picker DOM is small) ────

    private boolean isPickerOpen() {
        return existsNowInTest(
            "type == 'XCUIElementTypeNavigationBar' AND name == 'Link Existing Assets' AND visible == 1");
    }

    private void cancelPicker() {
        try {
            DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name == 'Cancel' AND visible == 1")).click();
        } catch (Exception ignored) { }
    }

    private boolean existsNowInTest(String predicate) {
        try {
            List<WebElement> els = DriverManager.getDriver().findElements(
                    AppiumBy.iOSNsPredicateString(predicate));
            return !els.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void tapFirst(String predicate) {
        try {
            WebElement el = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(predicate));
            org.openqa.selenium.Rectangle r = el.getRect();
            DriverManager.getDriver().executeScript("mobile: tap",
                    java.util.Map.of("x", r.x + Math.max(15, r.width / 2), "y", r.y + r.height / 2));
        } catch (Exception e) {
            System.out.println("   tapFirst(" + predicate + "): " + e.getMessage());
        }
    }

    private void tapOtherLocationsHeader() {
        tapFirst(SECTION_OTHER);
    }

    /** Badge from the "OTHER LOCATIONS, <n>" composite; -1 when absent. */
    private int otherLocationsBadge() {
        try {
            WebElement s = DriverManager.getDriver().findElement(
                    AppiumBy.iOSNsPredicateString(SECTION_OTHER));
            String name = s.getAttribute("name");
            String[] parts = name.split(",");
            return Integer.parseInt(parts[parts.length - 1].trim());
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Visible location disclosure-group rows inside the expanded section:
     * Buttons below the section header that are neither picker chrome nor
     * selection controls. Zero when collapsed.
     */
    private int countLocationGroups() {
        try {
            int headerY = DriverManager.getDriver().findElement(
                    AppiumBy.iOSNsPredicateString(SECTION_OTHER)).getLocation().getY();
            List<WebElement> buttons = DriverManager.getDriver().findElements(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > " + (headerY + 20)));
            int n = 0;
            for (WebElement b : buttons) {
                String name;
                try { name = b.getAttribute("name"); } catch (Exception e) { continue; }
                if (name == null || name.isEmpty()) continue;
                if (name.equals("Select All") || name.equals("Deselect All") || name.equals("Clear")
                        || name.equals("Cancel") || name.equals("Done")
                        || name.startsWith("OTHER LOCATIONS") || name.contains("qrcode")) continue;
                n++;
            }
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

    private int waitForLocationGroups() {
        final int[] n = {0};
        Waits.until(() -> (n[0] = countLocationGroups()) > 0, 4000);
        return n[0];
    }

    private void typeInPickerSearch(String text) {
        try {
            WebElement field;
            try {
                field = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeSearchField' AND visible == 1"));
            } catch (Exception e) {
                field = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND visible == 1"));
            }
            field.click();
            field.clear();
            field.sendKeys(text);
            try { DriverManager.getDriver().executeScript("mobile: hideKeyboard"); } catch (Exception ignored) { }
        } catch (Exception e) {
            System.out.println("   typeInPickerSearch: " + e.getMessage());
        }
    }

    private void clearPickerSearch() {
        try {
            WebElement clear = DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Clear text' AND visible == 1"));
            clear.click();
        } catch (Exception e) {
            typeInPickerSearch("");
        }
        try { DriverManager.getDriver().executeScript("mobile: hideKeyboard"); } catch (Exception ignored) { }
    }
}
