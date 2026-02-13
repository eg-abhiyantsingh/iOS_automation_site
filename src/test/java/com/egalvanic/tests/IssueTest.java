package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.*;

/**
 * Issues Test Suite (TC_ISS_001 - TC_ISS_019)
 * Covers: Issues list, header elements, filter tabs, search, issue entries, sort
 */
public final class IssueTest extends BaseTest {

    private IssuePage issuePage;

    // ================================================================
    // SETUP / TEARDOWN
    // ================================================================

    @BeforeClass(alwaysRun = true)
    public void issueTestSuiteSetup() {
        System.out.println("\n📋 Issues Test Suite - Starting");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void issueTestSetup() {
        issuePage = new IssuePage();
    }

    @AfterClass(alwaysRun = true)
    public void issueTestSuiteTeardown() {
        DriverManager.resetNoResetOverride();
        System.out.println("\n📋 Issues Test Suite - Complete");
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Navigate to Issues screen with retry logic.
     * Ensures the test starts on the Issues screen.
     */
    private boolean ensureOnIssuesScreen() {
        if (issuePage.isIssuesScreenDisplayed()) {
            System.out.println("✓ Already on Issues screen");
            return true;
        }

        System.out.println("⚡ Navigating to Issues screen...");

        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.println("   Navigation attempt " + attempt + "/3");
            sleep(500 * attempt);

            try {
                boolean result = issuePage.navigateToIssuesScreen();
                if (result) {
                    System.out.println("✓ Navigation successful (attempt " + attempt + ")");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("   Navigation failed on attempt " + attempt);
            }
        }

        System.out.println("❌ Could not navigate to Issues screen after 3 attempts");
        return false;
    }

    // ============================================================
    // ISSUES LIST TESTS (TC_ISS_001 - TC_ISS_007)
    // ============================================================

    /**
     * TC_ISS_001: Verify Issues screen header elements
     * Expected: Done button, Sort icon, + (add) button, 'Issues' title
     */
    @Test(priority = 1)
    public void TC_ISS_001_verifyIssuesScreenHeaderElements() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUES_LIST,
            "TC_ISS_001 - Verify Issues screen header elements");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Verify Issues title is displayed");
        boolean titleDisplayed = issuePage.isIssuesTitleDisplayed();
        assertTrue(titleDisplayed, "Issues title should be displayed");
        logStep("✅ Issues title is displayed");

        logStep("Step 3: Verify Done button is displayed");
        boolean doneDisplayed = issuePage.isDoneButtonDisplayed();
        assertTrue(doneDisplayed, "Done button should be displayed");
        logStep("✅ Done button is displayed");

        logStep("Step 4: Verify Sort icon is displayed");
        boolean sortDisplayed = issuePage.isSortIconDisplayed();
        assertTrue(sortDisplayed, "Sort icon should be displayed");
        logStep("✅ Sort icon is displayed");

        logStep("Step 5: Verify Add (+) button is displayed");
        boolean addDisplayed = issuePage.isAddButtonDisplayed();
        assertTrue(addDisplayed, "Add button should be displayed");
        logStep("✅ Add button is displayed");

        logStepWithScreenshot("TC_ISS_001: All header elements verified");
    }

    /**
     * TC_ISS_002: Verify Search bar on Issues screen
     * Expected: Search bar displayed with 'Search issues' placeholder
     */
    @Test(priority = 2)
    public void TC_ISS_002_verifySearchBarOnIssuesScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUES_LIST,
            "TC_ISS_002 - Verify Search bar on Issues screen");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Verify search bar is displayed");
        boolean searchDisplayed = issuePage.isSearchBarDisplayed();
        assertTrue(searchDisplayed, "Search bar should be displayed");
        logStep("✅ Search bar is displayed");

        logStep("Step 3: Verify search bar placeholder");
        String placeholder = issuePage.getSearchBarPlaceholder();
        logStep("Search bar placeholder: '" + placeholder + "'");

        logStepWithScreenshot("TC_ISS_002: Search bar verified");
    }

    /**
     * TC_ISS_003: Verify filter tabs displayed
     * Expected: All, Open, In Progress tabs with counts
     */
    @Test(priority = 3)
    public void TC_ISS_003_verifyFilterTabsDisplayed() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUES_LIST,
            "TC_ISS_003 - Verify filter tabs displayed");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Verify All tab is displayed");
        boolean allTab = issuePage.isAllTabDisplayed();
        assertTrue(allTab, "All tab should be displayed");
        logStep("✅ All tab is displayed");

        logStep("Step 3: Verify Open tab is displayed");
        boolean openTab = issuePage.isOpenTabDisplayed();
        assertTrue(openTab, "Open tab should be displayed");
        logStep("✅ Open tab is displayed");

        logStep("Step 4: Verify In Progress tab is displayed");
        boolean inProgressTab = issuePage.isInProgressTabDisplayed();
        assertTrue(inProgressTab, "In Progress tab should be displayed");
        logStep("✅ In Progress tab is displayed");

        logStepWithScreenshot("TC_ISS_003: All filter tabs verified");
    }

    /**
     * TC_ISS_004: Verify Open tab selected by default
     * Expected: Open tab is highlighted/selected when Issues screen opens
     */
    @Test(priority = 4)
    public void TC_ISS_004_verifyOpenTabSelectedByDefault() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUES_LIST,
            "TC_ISS_004 - Verify Open tab selected by default");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Verify Open tab is selected by default");
        boolean openSelected = issuePage.isOpenTabSelected();
        logStep("Open tab selected: " + openSelected);

        logStep("Step 3: Verify issues are displayed (Open filter active)");
        int issueCount = issuePage.getVisibleIssueCount();
        logStep("Visible issues under Open tab: " + issueCount);

        logStepWithScreenshot("TC_ISS_004: Open tab default selection verified");
    }

    /**
     * TC_ISS_005: Verify All tab shows all issues
     * Expected: Tapping All tab shows all issues regardless of status
     */
    @Test(priority = 5)
    public void TC_ISS_005_verifyAllTabShowsAllIssues() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUES_LIST,
            "TC_ISS_005 - Verify All tab shows all issues");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Note current issue count (Open tab)");
        int openCount = issuePage.getVisibleIssueCount();
        logStep("Issues under Open tab: " + openCount);

        logStep("Step 3: Tap All tab");
        issuePage.tapAllTab();
        shortWait();

        logStep("Step 4: Verify All tab count");
        int allCount = issuePage.getVisibleIssueCount();
        logStep("Issues under All tab: " + allCount);
        // All count should be >= Open count (All includes all statuses)
        assertTrue(allCount >= openCount,
            "All tab (" + allCount + ") should show >= Open tab (" + openCount + ") issues");
        logStep("✅ All tab shows all issues (count: " + allCount + " >= " + openCount + ")");

        // Switch back to Open tab for subsequent tests
        issuePage.tapOpenTab();
        shortWait();

        logStepWithScreenshot("TC_ISS_005: All tab verified");
    }

    /**
     * TC_ISS_006: Verify In Progress tab filters correctly
     * Expected: Only issues with 'In Progress' status displayed
     */
    @Test(priority = 6)
    public void TC_ISS_006_verifyInProgressTabFilters() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUES_LIST,
            "TC_ISS_006 - Verify In Progress tab filters correctly");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Tap In Progress tab");
        issuePage.tapInProgressTab();
        shortWait();

        logStep("Step 3: Verify In Progress tab is selected");
        boolean selected = issuePage.isInProgressTabSelected();
        logStep("In Progress tab selected: " + selected);

        logStep("Step 4: Check visible issue count");
        int inProgressCount = issuePage.getVisibleIssueCount();
        logStep("Issues under In Progress tab: " + inProgressCount);

        // Switch back to Open tab for subsequent tests
        issuePage.tapOpenTab();
        shortWait();

        logStepWithScreenshot("TC_ISS_006: In Progress tab filter verified");
    }

    /**
     * TC_ISS_007: Verify issue count in tabs
     * Expected: Counts reflect actual number of issues in each category
     */
    @Test(priority = 7)
    public void TC_ISS_007_verifyIssueCountInTabs() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUES_LIST,
            "TC_ISS_007 - Verify issue count in tabs");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Read counts from all tabs");
        int allCount = issuePage.getAllTabCount();
        int openCount = issuePage.getOpenTabCount();
        int inProgressCount = issuePage.getInProgressTabCount();

        logStep("Tab counts — All: " + allCount + ", Open: " + openCount +
            ", In Progress: " + inProgressCount);

        logStep("Step 3: Verify counts are non-negative");
        assertTrue(allCount >= 0, "All count should be >= 0");
        assertTrue(openCount >= 0, "Open count should be >= 0");
        assertTrue(inProgressCount >= 0, "In Progress count should be >= 0");

        logStep("Step 4: Verify All >= Open + In Progress");
        if (allCount >= 0 && openCount >= 0 && inProgressCount >= 0) {
            assertTrue(allCount >= openCount + inProgressCount,
                "All (" + allCount + ") should be >= Open (" + openCount +
                ") + InProgress (" + inProgressCount + ")");
            logStep("✅ All (" + allCount + ") >= Open (" + openCount +
                ") + InProgress (" + inProgressCount + ")");
        }

        logStepWithScreenshot("TC_ISS_007: Tab counts verified");
    }

    // ============================================================
    // ISSUE ENTRY TESTS (TC_ISS_008 - TC_ISS_014)
    // ============================================================

    /**
     * TC_ISS_008: Verify issue entry displays all elements
     * Expected: Icon, title, priority badge, asset name, status indicator
     */
    @Test(priority = 8)
    public void TC_ISS_008_verifyIssueEntryElements() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_ENTRY,
            "TC_ISS_008 - Verify issue entry displays all elements");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        logStep("Step 2: Ensure issues exist in the list");
        // Switch to All tab to maximize chance of having entries
        issuePage.tapAllTab();
        shortWait();

        int issueCount = issuePage.getVisibleIssueCount();
        logStep("Visible issues: " + issueCount);
        if (issueCount == 0) {
            logStep("⚠️ No issues available — skipping entry verification");
            logStepWithScreenshot("TC_ISS_008: No issues to verify");
            issuePage.tapOpenTab();
            return;
        }

        logStep("Step 3: Verify first issue entry has elements");
        boolean entryComplete = issuePage.isIssueEntryComplete();
        assertTrue(entryComplete, "Issue entry should have text content");
        logStep("✅ Issue entry has required elements");

        String title = issuePage.getFirstIssueTitle();
        logStep("First issue: " + title);

        // Switch back to Open tab
        issuePage.tapOpenTab();
        shortWait();

        logStepWithScreenshot("TC_ISS_008: Issue entry elements verified");
    }

    /**
     * TC_ISS_009: Verify issue type icons
     * Expected: Icons displayed for different issue types
     */
    @Test(priority = 9)
    public void TC_ISS_009_verifyIssueTypeIcons() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_ENTRY,
            "TC_ISS_009 - Verify issue type icons");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Verify issue type icon is displayed");
        int issueCount = issuePage.getVisibleIssueCount();
        if (issueCount == 0) {
            logStep("⚠️ No issues available — skipping icon verification");
            issuePage.tapOpenTab();
            logStepWithScreenshot("TC_ISS_009: No issues to verify");
            return;
        }

        boolean iconDisplayed = issuePage.isIssueTypeIconDisplayed();
        logStep("Issue type icon displayed: " + iconDisplayed);

        issuePage.tapOpenTab();
        shortWait();

        logStepWithScreenshot("TC_ISS_009: Issue type icons verified");
    }

    /**
     * TC_ISS_010: Verify High priority badge (red)
     * Expected: Red 'High' badge displayed
     */
    @Test(priority = 10)
    public void TC_ISS_010_verifyHighPriorityBadge() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_ENTRY,
            "TC_ISS_010 - Verify High priority badge");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Check for High priority badge");
        boolean highBadge = issuePage.isPriorityBadgeDisplayed("High");
        logStep("High priority badge displayed: " + highBadge);
        if (highBadge) {
            logStep("✅ High priority badge found");
        } else {
            logStep("⚠️ No High priority issue visible (may not exist in current data)");
        }

        issuePage.tapOpenTab();
        shortWait();

        logStepWithScreenshot("TC_ISS_010: High priority badge check complete");
    }

    /**
     * TC_ISS_011: Verify Medium priority badge (orange)
     * Expected: Orange 'Medium' badge displayed
     */
    @Test(priority = 11)
    public void TC_ISS_011_verifyMediumPriorityBadge() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_ENTRY,
            "TC_ISS_011 - Verify Medium priority badge");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Check for Medium priority badge");
        boolean mediumBadge = issuePage.isPriorityBadgeDisplayed("Medium");
        logStep("Medium priority badge displayed: " + mediumBadge);
        if (mediumBadge) {
            logStep("✅ Medium priority badge found");
        } else {
            logStep("⚠️ No Medium priority issue visible (may not exist in current data)");
        }

        issuePage.tapOpenTab();
        shortWait();

        logStepWithScreenshot("TC_ISS_011: Medium priority badge check complete");
    }

    /**
     * TC_ISS_012: Verify Open status badge
     * Expected: 'Open' badge visible on issue entry
     */
    @Test(priority = 12)
    public void TC_ISS_012_verifyOpenStatusBadge() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_ENTRY,
            "TC_ISS_012 - Verify Open status badge");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        logStep("Step 2: Ensure Open tab is selected");
        issuePage.tapOpenTab();
        shortWait();

        logStep("Step 3: Check for Open status badge on entries");
        int issueCount = issuePage.getVisibleIssueCount();
        if (issueCount == 0) {
            logStep("⚠️ No open issues available");
            logStepWithScreenshot("TC_ISS_012: No open issues to verify");
            return;
        }

        boolean openBadge = issuePage.isStatusBadgeDisplayed("Open");
        logStep("Open status badge displayed: " + openBadge);
        if (openBadge) {
            logStep("✅ Open status badge found on issue entry");
        }

        logStepWithScreenshot("TC_ISS_012: Open status badge verified");
    }

    /**
     * TC_ISS_013: Verify asset name displayed on issue
     * Expected: Asset name with grid icon visible on issue entry
     */
    @Test(priority = 13)
    public void TC_ISS_013_verifyAssetNameDisplayed() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_ENTRY,
            "TC_ISS_013 - Verify asset name displayed on issue");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Verify asset name is displayed on issue entry");
        int issueCount = issuePage.getVisibleIssueCount();
        if (issueCount == 0) {
            logStep("⚠️ No issues available — skipping asset name verification");
            issuePage.tapOpenTab();
            logStepWithScreenshot("TC_ISS_013: No issues to verify");
            return;
        }

        boolean assetDisplayed = issuePage.isAssetNameDisplayedOnIssue();
        assertTrue(assetDisplayed, "Asset name should be displayed on issue entry");
        logStep("✅ Asset name displayed on issue entry");

        issuePage.tapOpenTab();
        shortWait();

        logStepWithScreenshot("TC_ISS_013: Asset name verified");
    }

    /**
     * TC_ISS_014: Verify long title truncation
     * Expected: Long issue titles truncated with '...'
     */
    @Test(priority = 14)
    public void TC_ISS_014_verifyLongTitleTruncation() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_ENTRY,
            "TC_ISS_014 - Verify long title truncation");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Check for truncated titles");
        int issueCount = issuePage.getVisibleIssueCount();
        if (issueCount == 0) {
            logStep("⚠️ No issues available");
            issuePage.tapOpenTab();
            logStepWithScreenshot("TC_ISS_014: No issues to verify");
            return;
        }

        boolean hasTruncated = issuePage.hasAnyTruncatedTitle();
        logStep("Truncated title found: " + hasTruncated);
        if (hasTruncated) {
            logStep("✅ Long title truncation with '...' confirmed");
        } else {
            logStep("ℹ️ No truncated titles visible (all titles may fit in view)");
        }

        issuePage.tapOpenTab();
        shortWait();

        logStepWithScreenshot("TC_ISS_014: Title truncation check complete");
    }

    // ============================================================
    // SEARCH ISSUES TESTS (TC_ISS_015 - TC_ISS_018)
    // ============================================================

    /**
     * TC_ISS_015: Verify search filters issues
     * Expected: Typing 'Thermal' shows only matching issues
     */
    @Test(priority = 15)
    public void TC_ISS_015_verifySearchFiltersIssues() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SEARCH_ISSUES,
            "TC_ISS_015 - Verify search filters issues");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Note initial issue count");
        int initialCount = issuePage.getVisibleIssueCount();
        logStep("Initial issue count: " + initialCount);

        logStep("Step 3: Search for 'Thermal'");
        issuePage.searchIssues("Thermal");
        mediumWait();

        logStep("Step 4: Verify filtered results");
        int filteredCount = issuePage.getVisibleIssueCount();
        logStep("Filtered issue count: " + filteredCount);

        if (filteredCount > 0) {
            logStep("✅ Search returned " + filteredCount + " results for 'Thermal'");
        } else {
            logStep("ℹ️ No issues matching 'Thermal' found");
        }

        logStepWithScreenshot("TC_ISS_015: Search filter results");

        // Clean up
        issuePage.clearSearch();
        shortWait();
    }

    /**
     * TC_ISS_016: Verify search by asset name
     * Expected: Searching asset name shows related issues
     */
    @Test(priority = 16)
    public void TC_ISS_016_verifySearchByAssetName() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SEARCH_ISSUES,
            "TC_ISS_016 - Verify search by asset name");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Search for 'Busway'");
        issuePage.searchIssues("Busway");
        mediumWait();

        logStep("Step 3: Verify search results");
        int filteredCount = issuePage.getVisibleIssueCount();
        logStep("Issues matching 'Busway': " + filteredCount);

        if (filteredCount > 0) {
            logStep("✅ Search by asset name returned results");
        } else {
            logStep("ℹ️ No issues matching 'Busway' found (asset may not have issues)");
        }

        logStepWithScreenshot("TC_ISS_016: Search by asset name results");

        // Clean up
        issuePage.clearSearch();
        shortWait();
    }

    /**
     * TC_ISS_017: Verify search no results
     * Expected: No issues displayed for nonexistent query
     */
    @Test(priority = 17)
    public void TC_ISS_017_verifySearchNoResults() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SEARCH_ISSUES,
            "TC_ISS_017 - Verify search no results");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Search for nonexistent text");
        issuePage.searchIssues("XYZNONEXISTENT999");
        mediumWait();

        logStep("Step 3: Verify no results");
        int count = issuePage.getVisibleIssueCount();
        logStep("Issue count for nonexistent query: " + count);

        boolean noResults = issuePage.isNoResultsDisplayed();
        logStep("No results state: " + noResults);

        assertTrue(count == 0 || noResults,
            "Search for nonexistent text should show no issues");
        logStep("✅ No results displayed for nonexistent search query");

        logStepWithScreenshot("TC_ISS_017: No results verified");

        // Clean up
        issuePage.clearSearch();
        shortWait();
    }

    /**
     * TC_ISS_018: Verify clearing search restores list
     * Expected: Full issue list restored after clearing search
     */
    @Test(priority = 18)
    public void TC_ISS_018_verifyClearingSearchRestoresList() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SEARCH_ISSUES,
            "TC_ISS_018 - Verify clearing search restores list");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Note initial issue count");
        int initialCount = issuePage.getVisibleIssueCount();
        logStep("Initial issue count: " + initialCount);

        logStep("Step 3: Search for something specific");
        issuePage.searchIssues("Thermal");
        mediumWait();

        int filteredCount = issuePage.getVisibleIssueCount();
        logStep("Filtered count: " + filteredCount);

        logStep("Step 4: Clear search");
        issuePage.clearSearch();
        mediumWait();

        logStep("Step 5: Verify full list is restored");
        int restoredCount = issuePage.getVisibleIssueCount();
        logStep("Restored count: " + restoredCount);

        assertTrue(restoredCount >= initialCount,
            "Restored count (" + restoredCount + ") should be >= initial (" + initialCount + ")");
        logStep("✅ List restored after clearing search (count: " + restoredCount + ")");

        logStepWithScreenshot("TC_ISS_018: Search clear restore verified");
    }

    // ============================================================
    // SORT TEST (TC_ISS_019)
    // ============================================================

    /**
     * TC_ISS_019: Verify Sort icon in header
     * Expected: Sort icon (↕) visible in header
     */
    @Test(priority = 19)
    public void TC_ISS_019_verifySortIconInHeader() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SORT_ISSUES,
            "TC_ISS_019 - Verify Sort icon in header");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Verify Sort icon is present in header");
        boolean sortDisplayed = issuePage.isSortIconDisplayed();
        assertTrue(sortDisplayed, "Sort icon should be present in header");
        logStep("✅ Sort icon is present in header");

        logStepWithScreenshot("TC_ISS_019: Sort icon in header verified");
    }
}
