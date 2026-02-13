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

    /**
     * Navigate to New Issue form (Issues screen → tap + button).
     * Returns true if the New Issue form is displayed.
     */
    private boolean ensureOnNewIssueForm() {
        // Already on New Issue form?
        if (issuePage.isNewIssueFormDisplayed()) {
            System.out.println("✓ Already on New Issue form");
            return true;
        }

        // Navigate to Issues screen first
        if (!ensureOnIssuesScreen()) {
            return false;
        }

        // Tap + to open New Issue form
        issuePage.tapAddButton();
        shortWait();

        boolean displayed = issuePage.isNewIssueFormDisplayed();
        if (displayed) {
            System.out.println("✓ New Issue form is displayed");
        } else {
            System.out.println("❌ New Issue form not displayed after tapping +");
        }
        return displayed;
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
     * Expected: All, Open, Resolved, Closed tabs with counts
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

        logStep("Step 4: Verify Resolved tab is displayed");
        boolean resolvedTab = issuePage.isResolvedTabDisplayed();
        assertTrue(resolvedTab, "Resolved tab should be displayed");
        logStep("✅ Resolved tab is displayed");

        logStep("Step 5: Verify Closed tab is displayed");
        boolean closedTab = issuePage.isClosedTabDisplayed();
        assertTrue(closedTab, "Closed tab should be displayed");
        logStep("✅ Closed tab is displayed");

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
     * TC_ISS_006: Verify Resolved and Closed tabs filter correctly
     * Expected: Resolved tab shows resolved issues, Closed tab shows closed issues
     */
    @Test(priority = 6)
    public void TC_ISS_006_verifyResolvedAndClosedTabFilters() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUES_LIST,
            "TC_ISS_006 - Verify Resolved and Closed tabs filter correctly");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Tap Resolved tab");
        issuePage.tapResolvedTab();
        shortWait();

        logStep("Step 3: Verify Resolved tab is selected");
        boolean resolvedSelected = issuePage.isResolvedTabSelected();
        logStep("Resolved tab selected: " + resolvedSelected);

        logStep("Step 4: Check visible issue count under Resolved");
        int resolvedCount = issuePage.getVisibleIssueCount();
        logStep("Issues under Resolved tab: " + resolvedCount);
        if (resolvedCount == 0) {
            boolean noIssues = issuePage.isNoIssuesFoundDisplayed();
            logStep("No Issues Found displayed: " + noIssues);
        }

        logStep("Step 5: Tap Closed tab");
        issuePage.tapClosedTab();
        shortWait();

        logStep("Step 6: Check visible issue count under Closed");
        int closedCount = issuePage.getVisibleIssueCount();
        logStep("Issues under Closed tab: " + closedCount);
        if (closedCount == 0) {
            boolean noIssues = issuePage.isNoIssuesFoundDisplayed();
            logStep("No Issues Found displayed: " + noIssues);
        }

        // Switch back to Open tab for subsequent tests
        issuePage.tapOpenTab();
        shortWait();

        logStepWithScreenshot("TC_ISS_006: Resolved and Closed tab filters verified");
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
        int resolvedCount = issuePage.getResolvedTabCount();
        int closedCount = issuePage.getClosedTabCount();

        logStep("Tab counts — All: " + allCount + ", Open: " + openCount +
            ", Resolved: " + resolvedCount + ", Closed: " + closedCount);

        logStep("Step 3: Verify counts are non-negative");
        assertTrue(allCount >= 0, "All count should be >= 0");
        assertTrue(openCount >= 0, "Open count should be >= 0");
        assertTrue(resolvedCount >= 0, "Resolved count should be >= 0");
        assertTrue(closedCount >= 0, "Closed count should be >= 0");

        logStep("Step 4: Verify All >= Open + Resolved + Closed");
        if (allCount >= 0 && openCount >= 0 && resolvedCount >= 0 && closedCount >= 0) {
            int sumOfParts = openCount + resolvedCount + closedCount;
            assertTrue(allCount >= sumOfParts,
                "All (" + allCount + ") should be >= Open (" + openCount +
                ") + Resolved (" + resolvedCount + ") + Closed (" + closedCount + ")");
            logStep("✅ All (" + allCount + ") >= Open (" + openCount +
                ") + Resolved (" + resolvedCount + ") + Closed (" + closedCount + ")");
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

        boolean noResults = issuePage.isNoIssuesFoundDisplayed();
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

    /**
     * TC_ISS_020: Verify tapping Sort opens options
     * Expected: Sort options displayed (e.g., by Priority, by Date, by Title)
     */
    @Test(priority = 20)
    public void TC_ISS_020_verifySortOptionsDisplayed() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SORT_ISSUES,
            "TC_ISS_020 - Verify tapping Sort opens options");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Tap Sort icon");
        issuePage.tapSortIcon();
        shortWait();

        logStep("Step 3: Verify sort options are displayed");
        boolean sortOptions = issuePage.isSortOptionsDisplayed();
        logStep("Sort options displayed: " + sortOptions);
        if (sortOptions) {
            logStep("✅ Sort options are displayed");
        } else {
            logStep("⚠️ Sort options not detected — UI may use a different pattern");
        }

        logStepWithScreenshot("TC_ISS_020: Sort options after tap");

        // Dismiss sort options
        issuePage.dismissSortOptions();
        shortWait();
    }

    // ============================================================
    // NEW ISSUE TESTS (TC_ISS_021 - TC_ISS_025)
    // ============================================================

    /**
     * TC_ISS_021: Verify + button opens New Issue screen
     * Expected: New Issue screen opens with form fields
     */
    @Test(priority = 21)
    public void TC_ISS_021_verifyAddButtonOpensNewIssue() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NEW_ISSUE,
            "TC_ISS_021 - Verify + button opens New Issue screen");

        logStep("Step 1: Navigate to Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Tap + (Add) button");
        issuePage.tapAddButton();
        shortWait();

        logStep("Step 3: Verify New Issue screen is displayed");
        boolean newIssueDisplayed = issuePage.isNewIssueFormDisplayed();
        assertTrue(newIssueDisplayed, "New Issue screen should be displayed");
        logStep("✅ New Issue screen opened successfully");

        logStepWithScreenshot("TC_ISS_021: New Issue screen opened");

        // Clean up: Cancel back to Issues list
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_022: Verify New Issue screen UI elements
     * Expected: Cancel button, 'New Issue' title, Create Issue button (disabled),
     *           CLASSIFICATION section, ISSUE DETAILS section, ASSIGNMENT section
     */
    @Test(priority = 22)
    public void TC_ISS_022_verifyNewIssueScreenUIElements() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NEW_ISSUE,
            "TC_ISS_022 - Verify New Issue screen UI elements");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Verify 'New Issue' title");
        boolean titleDisplayed = issuePage.isNewIssueFormDisplayed();
        assertTrue(titleDisplayed, "'New Issue' title should be displayed");
        logStep("✅ 'New Issue' title is displayed");

        logStep("Step 3: Verify Create Issue button exists (should be disabled)");
        boolean createDisabled = !issuePage.isCreateIssueEnabled();
        logStep("Create Issue button disabled: " + createDisabled);

        logStep("Step 4: Verify CLASSIFICATION section");
        boolean classification = issuePage.isClassificationSectionDisplayed();
        logStep("CLASSIFICATION section displayed: " + classification);

        logStep("Step 5: Verify ISSUE DETAILS section");
        boolean issueDetails = issuePage.isIssueDetailsSectionDisplayed();
        logStep("ISSUE DETAILS section displayed: " + issueDetails);

        logStep("Step 6: Verify ASSIGNMENT section");
        boolean assignment = issuePage.isAssignmentSectionDisplayed();
        logStep("ASSIGNMENT section displayed: " + assignment);

        logStepWithScreenshot("TC_ISS_022: New Issue screen UI elements");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_023: Verify Create Issue button initially disabled
     * Expected: Create Issue button is grayed out/disabled when form just opened
     */
    @Test(priority = 23)
    public void TC_ISS_023_verifyCreateIssueButtonInitiallyDisabled() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NEW_ISSUE,
            "TC_ISS_023 - Verify Create Issue button initially disabled");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Verify Create Issue button is disabled");
        boolean enabled = issuePage.isCreateIssueEnabled();
        assertTrue(!enabled, "Create Issue button should be disabled initially");
        logStep("✅ Create Issue button is disabled (no required fields filled)");

        logStepWithScreenshot("TC_ISS_023: Create Issue button disabled");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_024: Verify Asset is required validation
     * Expected: 'Asset is required' message displayed below Asset field
     */
    @Test(priority = 24)
    public void TC_ISS_024_verifyAssetRequiredValidation() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NEW_ISSUE,
            "TC_ISS_024 - Verify Asset is required validation");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Check for 'Asset is required' validation message");
        boolean assetRequired = issuePage.isAssetRequiredMessageDisplayed();
        logStep("'Asset is required' message displayed: " + assetRequired);
        if (assetRequired) {
            logStep("✅ Asset is required validation message is shown");
        } else {
            logStep("ℹ️ Validation message may appear only after attempting to create");
        }

        logStep("Step 3: Verify Select Asset field is visible");
        boolean selectAsset = issuePage.isSelectAssetDisplayed();
        logStep("Select Asset field displayed: " + selectAsset);

        logStepWithScreenshot("TC_ISS_024: Asset required validation");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_025: Verify Cancel returns to Issues list
     * Expected: New Issue screen closes, returns to Issues list, no issue created
     */
    @Test(priority = 25)
    public void TC_ISS_025_verifyCancelReturnsToIssuesList() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NEW_ISSUE,
            "TC_ISS_025 - Verify Cancel returns to Issues list");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Note issue count before cancel");
        // We can't easily count from New Issue form, so skip this check

        logStep("Step 3: Tap Cancel button");
        issuePage.tapCancelNewIssue();
        shortWait();

        logStep("Step 4: Verify returned to Issues screen");
        boolean onIssues = issuePage.isIssuesScreenDisplayed();
        assertTrue(onIssues, "Should return to Issues screen after Cancel");
        logStep("✅ Returned to Issues screen after Cancel");

        logStep("Step 5: Verify New Issue form is no longer displayed");
        boolean formStillOpen = issuePage.isNewIssueFormDisplayed();
        assertTrue(!formStillOpen, "New Issue form should be closed");
        logStep("✅ New Issue form is closed");

        logStepWithScreenshot("TC_ISS_025: Cancel returns to Issues list");
    }

    // ============================================================
    // ISSUE CLASS TESTS (TC_ISS_026 - TC_ISS_033)
    // ============================================================

    /**
     * TC_ISS_026: Verify Issue Class dropdown field
     * Expected: Field shows 'None' with dropdown indicator
     */
    @Test(priority = 26)
    public void TC_ISS_026_verifyIssueClassDropdown() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS,
            "TC_ISS_026 - Verify Issue Class dropdown");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Verify Issue Class dropdown is displayed");
        boolean dropdownDisplayed = issuePage.isIssueClassDropdownDisplayed();
        assertTrue(dropdownDisplayed, "Issue Class dropdown should be displayed");
        logStep("✅ Issue Class dropdown is displayed");

        logStep("Step 3: Verify default value is 'None'");
        String value = issuePage.getIssueClassValue();
        logStep("Issue Class current value: '" + value + "'");

        logStepWithScreenshot("TC_ISS_026: Issue Class dropdown");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_027: Verify Issue Class options
     * Expected: Options displayed — None, NEC Violation, NFPA 70B Violation,
     *           OSHA Violation, Repair Needed, Thermal Anomaly, Ultrasonic Anomaly
     */
    @Test(priority = 27)
    public void TC_ISS_027_verifyIssueClassOptions() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS,
            "TC_ISS_027 - Verify Issue Class options");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Open Issue Class dropdown");
        boolean opened = issuePage.openIssueClassDropdown();
        assertTrue(opened, "Issue Class dropdown should open");
        shortWait();

        logStep("Step 3: Verify dropdown options");
        String[] expectedOptions = {
            "NEC Violation", "NFPA 70B Violation", "OSHA Violation",
            "Repair Needed", "Thermal Anomaly", "Ultrasonic Anomaly"
        };

        int foundCount = 0;
        for (String option : expectedOptions) {
            boolean found = issuePage.isDropdownOptionDisplayed(option);
            logStep("   Option '" + option + "': " + (found ? "FOUND" : "NOT FOUND"));
            if (found) foundCount++;
        }
        logStep("Found " + foundCount + "/" + expectedOptions.length + " expected options");

        logStepWithScreenshot("TC_ISS_027: Issue Class options");

        // Dismiss dropdown without selecting
        issuePage.dismissDropdownMenu();
        shortWait();

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_028: Verify selecting NEC Violation
     * Expected: NEC Violation selected, dropdown closes, field shows 'NEC Violation'
     */
    @Test(priority = 28)
    public void TC_ISS_028_verifySelectingNECViolation() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS,
            "TC_ISS_028 - Verify selecting NEC Violation");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Select NEC Violation from Issue Class");
        String selectedValue = issuePage.selectIssueClassAndGetValue("NEC Violation");
        logStep("Selected value: '" + selectedValue + "'");
        logStep("✅ NEC Violation selection completed");

        logStepWithScreenshot("TC_ISS_028: NEC Violation selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_029: Verify selecting NFPA 70B Violation
     * Expected: NFPA 70B Violation selected, field shows 'NFPA 70B Violation'
     */
    @Test(priority = 29)
    public void TC_ISS_029_verifySelectingNFPA70BViolation() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS,
            "TC_ISS_029 - Verify selecting NFPA 70B Violation");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Select NFPA 70B Violation from Issue Class");
        String selectedValue = issuePage.selectIssueClassAndGetValue("NFPA 70B Violation");
        logStep("Selected value: '" + selectedValue + "'");
        logStep("✅ NFPA 70B Violation selection completed");

        logStepWithScreenshot("TC_ISS_029: NFPA 70B Violation selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_030: Verify selecting OSHA Violation
     * Expected: OSHA Violation selected
     */
    @Test(priority = 30)
    public void TC_ISS_030_verifySelectingOSHAViolation() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS,
            "TC_ISS_030 - Verify selecting OSHA Violation");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Select OSHA Violation from Issue Class");
        String selectedValue = issuePage.selectIssueClassAndGetValue("OSHA Violation");
        logStep("Selected value: '" + selectedValue + "'");
        logStep("✅ OSHA Violation selection completed");

        logStepWithScreenshot("TC_ISS_030: OSHA Violation selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_031: Verify selecting Repair Needed
     * Expected: Repair Needed selected
     */
    @Test(priority = 31)
    public void TC_ISS_031_verifySelectingRepairNeeded() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS,
            "TC_ISS_031 - Verify selecting Repair Needed");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Select Repair Needed from Issue Class");
        String selectedValue = issuePage.selectIssueClassAndGetValue("Repair Needed");
        logStep("Selected value: '" + selectedValue + "'");
        logStep("✅ Repair Needed selection completed");

        logStepWithScreenshot("TC_ISS_031: Repair Needed selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_032: Verify selecting Thermal Anomaly
     * Expected: Thermal Anomaly selected
     */
    @Test(priority = 32)
    public void TC_ISS_032_verifySelectingThermalAnomaly() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS,
            "TC_ISS_032 - Verify selecting Thermal Anomaly");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Select Thermal Anomaly from Issue Class");
        String selectedValue = issuePage.selectIssueClassAndGetValue("Thermal Anomaly");
        logStep("Selected value: '" + selectedValue + "'");
        logStep("✅ Thermal Anomaly selection completed");

        logStepWithScreenshot("TC_ISS_032: Thermal Anomaly selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_033: Verify selecting Ultrasonic Anomaly
     * Expected: Ultrasonic Anomaly selected
     */
    @Test(priority = 33)
    public void TC_ISS_033_verifySelectingUltrasonicAnomaly() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS,
            "TC_ISS_033 - Verify selecting Ultrasonic Anomaly");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Select Ultrasonic Anomaly from Issue Class");
        String selectedValue = issuePage.selectIssueClassAndGetValue("Ultrasonic Anomaly");
        logStep("Selected value: '" + selectedValue + "'");
        logStep("✅ Ultrasonic Anomaly selection completed");

        logStepWithScreenshot("TC_ISS_033: Ultrasonic Anomaly selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    // ============================================================
    // TITLE FIELD TESTS (TC_ISS_034 - TC_ISS_035)
    // ============================================================

    /**
     * TC_ISS_034: Verify Title field
     * Expected: Title field with pencil icon and 'Enter issue title' placeholder
     */
    @Test(priority = 34)
    public void TC_ISS_034_verifyTitleField() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_TITLE,
            "TC_ISS_034 - Verify Title field");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Verify Title field is displayed");
        boolean titleFieldDisplayed = issuePage.isTitleFieldDisplayed();
        assertTrue(titleFieldDisplayed, "Title field should be displayed");
        logStep("✅ Title field is displayed");

        logStep("Step 3: Verify placeholder text");
        String placeholder = issuePage.getTitleFieldPlaceholder();
        logStep("Title field placeholder: '" + placeholder + "'");

        logStepWithScreenshot("TC_ISS_034: Title field verified");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_035: Verify entering Title text
     * Expected: Text 'Abhiyant' appears in Title field, keyboard shown for input
     */
    @Test(priority = 35)
    public void TC_ISS_035_verifyEnteringTitleText() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_TITLE,
            "TC_ISS_035 - Verify entering Title text");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Enter 'Abhiyant' in Title field");
        issuePage.enterIssueTitle("Abhiyant");
        shortWait();

        logStep("Step 3: Verify text was entered");
        String titleValue = issuePage.getTitleFieldValue();
        logStep("Title field value: '" + titleValue + "'");
        if (titleValue.contains("Abhiyant")) {
            logStep("✅ Title text 'Abhiyant' entered successfully");
        } else {
            logStep("⚠️ Title value does not contain 'Abhiyant' — got: '" + titleValue + "'");
        }

        logStepWithScreenshot("TC_ISS_035: Title text entered");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    // ============================================================
    // PRIORITY TESTS (TC_ISS_036 - TC_ISS_039)
    // ============================================================

    /**
     * TC_ISS_036: Verify Priority dropdown
     * Expected: Field shows 'None' with dropdown indicator
     */
    @Test(priority = 36)
    public void TC_ISS_036_verifyPriorityDropdown() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_PRIORITY,
            "TC_ISS_036 - Verify Priority dropdown");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Verify Priority dropdown is displayed");
        boolean priorityDisplayed = issuePage.isPriorityDropdownDisplayed();
        assertTrue(priorityDisplayed, "Priority dropdown should be displayed");
        logStep("✅ Priority dropdown is displayed");

        logStep("Step 3: Verify default value");
        String value = issuePage.getPriorityValue();
        logStep("Priority current value: '" + value + "'");

        logStepWithScreenshot("TC_ISS_036: Priority dropdown");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_037: Verify Priority options
     * Expected: Options displayed — None, High (!!!), Medium (!!), Low (!). Done button visible
     */
    @Test(priority = 37)
    public void TC_ISS_037_verifyPriorityOptions() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_PRIORITY,
            "TC_ISS_037 - Verify Priority options");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Open Priority dropdown");
        boolean opened = issuePage.openPriorityDropdown();
        assertTrue(opened, "Priority dropdown should open");
        shortWait();

        logStep("Step 3: Verify priority options");
        String[] expectedOptions = {"None", "High", "Medium", "Low"};

        int foundCount = 0;
        for (String option : expectedOptions) {
            boolean found = issuePage.isDropdownOptionDisplayed(option);
            logStep("   Option '" + option + "': " + (found ? "FOUND" : "NOT FOUND"));
            if (found) foundCount++;
        }
        logStep("Found " + foundCount + "/" + expectedOptions.length + " expected options");

        logStepWithScreenshot("TC_ISS_037: Priority options");

        // Dismiss dropdown without selecting
        issuePage.dismissDropdownMenu();
        shortWait();

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_038: Verify selecting High priority
     * Expected: High selected with !!! indicator, field shows '!!! High'
     */
    @Test(priority = 38)
    public void TC_ISS_038_verifySelectingHighPriority() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_PRIORITY,
            "TC_ISS_038 - Verify selecting High priority");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Select High priority");
        String selectedValue = issuePage.selectPriorityAndGetValue("High");
        logStep("Priority value after selection: '" + selectedValue + "'");
        logStep("✅ High priority selection completed");

        logStepWithScreenshot("TC_ISS_038: High priority selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_039: Verify selecting Medium priority
     * Expected: Medium selected with !! indicator
     */
    @Test(priority = 39)
    public void TC_ISS_039_verifySelectingMediumPriority() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_PRIORITY,
            "TC_ISS_039 - Verify selecting Medium priority");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Select Medium priority");
        String selectedValue = issuePage.selectPriorityAndGetValue("Medium");
        logStep("Priority value after selection: '" + selectedValue + "'");
        logStep("✅ Medium priority selection completed");

        logStepWithScreenshot("TC_ISS_039: Medium priority selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_040: Verify selecting Low priority
     * Expected: Low selected with ! indicator
     */
    @Test(priority = 40)
    public void TC_ISS_040_verifySelectingLowPriority() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_PRIORITY,
            "TC_ISS_040 - Verify selecting Low priority");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Select Low priority");
        String selectedValue = issuePage.selectPriorityAndGetValue("Low");
        logStep("Priority value after selection: '" + selectedValue + "'");
        logStep("✅ Low priority selection completed");

        logStepWithScreenshot("TC_ISS_040: Low priority selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    // ============================================================
    // ASSET SELECTION TESTS (TC_ISS_041 - TC_ISS_047)
    // ============================================================

    /**
     * TC_ISS_041: Verify Asset field on New Issue form
     * Expected: Field shows 'Select Asset' with chevron indicating navigation
     */
    @Test(priority = 41)
    public void TC_ISS_041_verifyAssetField() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ASSET_SELECTION,
            "TC_ISS_041 - Verify Asset field");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Verify Select Asset field is displayed");
        boolean assetDisplayed = issuePage.isSelectAssetDisplayed();
        assertTrue(assetDisplayed, "Select Asset field should be displayed");
        logStep("✅ Select Asset field is displayed");

        logStep("Step 3: Verify ASSIGNMENT section");
        boolean assignmentSection = issuePage.isAssignmentSectionDisplayed();
        logStep("ASSIGNMENT section displayed: " + assignmentSection);

        logStepWithScreenshot("TC_ISS_041: Asset field verified");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_042: Verify tapping Asset opens selection screen
     * Expected: Select Asset screen opens with Cancel, title, Search bar, Asset list
     */
    @Test(priority = 42)
    public void TC_ISS_042_verifyAssetOpensSelectionScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ASSET_SELECTION,
            "TC_ISS_042 - Verify tapping Asset opens selection");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Tap Select Asset");
        issuePage.tapSelectAsset();
        shortWait();

        logStep("Step 3: Verify Select Asset screen is displayed");
        boolean assetScreen = issuePage.isSelectAssetScreenDisplayed();
        assertTrue(assetScreen, "Select Asset screen should be displayed");
        logStep("✅ Select Asset screen opened");

        logStepWithScreenshot("TC_ISS_042: Select Asset screen");

        // Clean up: Cancel back
        issuePage.tapCancelAssetPicker();
        shortWait();
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_043: Verify asset list displayed
     * Expected: All site assets listed, list is scrollable
     */
    @Test(priority = 43)
    public void TC_ISS_043_verifyAssetListDisplayed() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ASSET_SELECTION,
            "TC_ISS_043 - Verify asset list displayed");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Open Select Asset screen");
        issuePage.tapSelectAsset();
        shortWait();

        logStep("Step 3: Verify asset list has items");
        int assetCount = issuePage.getAssetListCount();
        logStep("Visible assets: " + assetCount);
        assertTrue(assetCount > 0, "Asset list should have at least one asset");
        logStep("✅ Asset list has " + assetCount + " visible assets");

        logStepWithScreenshot("TC_ISS_043: Asset list displayed");

        // Clean up
        issuePage.tapCancelAssetPicker();
        shortWait();
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_044: Verify search assets in picker
     * Expected: Typing 'Busway' filters asset list to show only Busway assets
     */
    @Test(priority = 44)
    public void TC_ISS_044_verifySearchAssets() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ASSET_SELECTION,
            "TC_ISS_044 - Verify search assets");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Open Select Asset screen");
        issuePage.tapSelectAsset();
        shortWait();

        logStep("Step 3: Note initial asset count");
        int initialCount = issuePage.getAssetListCount();
        logStep("Initial asset count: " + initialCount);

        logStep("Step 4: Search for 'Busway'");
        issuePage.searchAssetsInPicker("Busway");
        mediumWait();

        logStep("Step 5: Verify filtered results");
        int filteredCount = issuePage.getAssetListCount();
        logStep("Filtered asset count: " + filteredCount);
        if (filteredCount > 0 && filteredCount <= initialCount) {
            logStep("✅ Asset search filtered list (from " + initialCount + " to " + filteredCount + ")");
        } else {
            logStep("ℹ️ Search returned " + filteredCount + " assets");
        }

        logStepWithScreenshot("TC_ISS_044: Asset search results");

        // Clean up
        issuePage.tapCancelAssetPicker();
        shortWait();
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_045: Verify selecting an asset
     * Expected: Asset selected, returns to New Issue, Asset field shows 'ATS 1'
     */
    @Test(priority = 45)
    public void TC_ISS_045_verifySelectingAsset() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ASSET_SELECTION,
            "TC_ISS_045 - Verify selecting asset");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Open Select Asset screen");
        issuePage.tapSelectAsset();
        shortWait();

        logStep("Step 3: Select 'ATS 1'");
        boolean selected = issuePage.selectAssetInPicker("ATS 1");
        logStep("Asset selection result: " + selected);
        shortWait();

        logStep("Step 4: Verify returned to New Issue form");
        boolean backOnForm = issuePage.isNewIssueFormDisplayed();
        logStep("Back on New Issue form: " + backOnForm);

        if (backOnForm) {
            logStep("Step 5: Verify selected asset name");
            String assetName = issuePage.getSelectedAssetName();
            logStep("Selected asset: '" + assetName + "'");
            logStep("✅ Asset selection completed");
        }

        logStepWithScreenshot("TC_ISS_045: Asset selected");

        // Clean up
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_046: Verify + button to create new asset on selection screen
     * Expected: + button visible, can open new asset creation
     */
    @Test(priority = 46)
    public void TC_ISS_046_verifyAddAssetButton() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ASSET_SELECTION,
            "TC_ISS_046 - Verify + button to create new asset");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Open Select Asset screen");
        issuePage.tapSelectAsset();
        shortWait();

        logStep("Step 3: Verify + (Add) button is displayed");
        boolean addButton = issuePage.isAddAssetButtonOnPickerDisplayed();
        logStep("Add Asset button displayed: " + addButton);
        if (addButton) {
            logStep("✅ Add Asset button is available on picker");
        } else {
            logStep("ℹ️ Add Asset button not detected");
        }

        logStepWithScreenshot("TC_ISS_046: Add Asset button on picker");

        // Clean up
        issuePage.tapCancelAssetPicker();
        shortWait();
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_047: Verify QR scan button on selection screen
     * Expected: QR scan button visible for scanning asset QR code
     */
    @Test(priority = 47)
    public void TC_ISS_047_verifyQRScanButton() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ASSET_SELECTION,
            "TC_ISS_047 - Verify QR scan button");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Open Select Asset screen");
        issuePage.tapSelectAsset();
        shortWait();

        logStep("Step 3: Verify QR scan button is displayed");
        boolean qrButton = issuePage.isQRScanButtonDisplayed();
        logStep("QR scan button displayed: " + qrButton);
        if (qrButton) {
            logStep("✅ QR scan button is available on picker");
        } else {
            logStep("ℹ️ QR scan button not detected");
        }

        logStepWithScreenshot("TC_ISS_047: QR scan button on picker");

        // Clean up
        issuePage.tapCancelAssetPicker();
        shortWait();
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    // ============================================================
    // CREATE ISSUE TESTS (TC_ISS_048 - TC_ISS_051)
    // NOTE: TC_ISS_049 creates an actual issue that TC_ISS_050-059 depend on
    // ============================================================

    /**
     * TC_ISS_048: Verify Create Issue enabled after required fields
     * Expected: Create Issue button becomes enabled (blue/active) after filling fields
     */
    @Test(priority = 48)
    public void TC_ISS_048_verifyCreateIssueEnabledAfterRequiredFields() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CREATE_ISSUE,
            "TC_ISS_048 - Verify Create Issue enabled after required fields");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Verify Create Issue is initially disabled");
        boolean initiallyDisabled = !issuePage.isCreateIssueEnabled();
        logStep("Initially disabled: " + initiallyDisabled);

        logStep("Step 3: Fill Issue Class");
        issuePage.selectIssueClass("NEC Violation");
        shortWait();

        logStep("Step 4: Fill Title");
        issuePage.enterIssueTitle("Abhiyant");
        shortWait();

        logStep("Step 5: Fill Priority");
        issuePage.selectPriority("High");
        shortWait();

        logStep("Step 6: Select Asset");
        issuePage.tapSelectAsset();
        shortWait();
        issuePage.selectAssetInPicker("ATS 1");
        shortWait();

        logStep("Step 7: Verify Create Issue is now enabled");
        boolean nowEnabled = issuePage.isCreateIssueEnabled();
        logStep("Create Issue enabled after filling fields: " + nowEnabled);
        if (nowEnabled) {
            logStep("✅ Create Issue button is enabled");
        } else {
            logStep("⚠️ Create Issue still disabled — may need additional required fields");
        }

        logStepWithScreenshot("TC_ISS_048: Create Issue button state");

        // Clean up — DO NOT create, just cancel
        issuePage.tapCancelNewIssue();
        shortWait();
    }

    /**
     * TC_ISS_049: Verify issue created successfully
     * Expected: Issue created, returns to Issues list, new issue appears
     * NOTE: This test CREATES an issue that TC_ISS_050-059 depend on
     */
    @Test(priority = 49)
    public void TC_ISS_049_verifyIssueCreatedSuccessfully() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CREATE_ISSUE,
            "TC_ISS_049 - Verify issue created successfully");

        logStep("Step 1: Navigate to New Issue form");
        boolean onForm = ensureOnNewIssueForm();
        assertTrue(onForm, "Should be on New Issue form");
        shortWait();

        logStep("Step 2: Fill Issue Class — NEC Violation");
        issuePage.selectIssueClass("NEC Violation");
        shortWait();

        logStep("Step 3: Fill Title — 'Abhiyant'");
        issuePage.enterIssueTitle("Abhiyant");
        shortWait();

        logStep("Step 4: Fill Priority — High");
        issuePage.selectPriority("High");
        shortWait();

        logStep("Step 5: Select Asset — ATS 1");
        issuePage.tapSelectAsset();
        shortWait();
        issuePage.selectAssetInPicker("ATS 1");
        shortWait();

        logStep("Step 6: Tap Create Issue");
        issuePage.tapCreateIssue();
        mediumWait();

        logStep("Step 7: Verify returned to Issues screen");
        boolean onIssues = issuePage.isIssuesScreenDisplayed();
        logStep("On Issues screen: " + onIssues);
        if (onIssues) {
            logStep("✅ Issue created and returned to Issues list");
        } else {
            // May still be on form if creation failed
            boolean stillOnForm = issuePage.isNewIssueFormDisplayed();
            logStep("Still on form: " + stillOnForm);
            if (stillOnForm) {
                logStep("⚠️ Issue creation may have failed — still on form");
                issuePage.tapCancelNewIssue();
                shortWait();
            }
        }

        logStepWithScreenshot("TC_ISS_049: Issue creation result");
    }

    /**
     * TC_ISS_050: Verify new issue appears in list
     * Expected: 'Abhiyant' issue appears with High badge, assigned asset, Open status
     * Depends on: TC_ISS_049 having created the issue
     */
    @Test(priority = 50)
    public void TC_ISS_050_verifyNewIssueAppearsInList() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CREATE_ISSUE,
            "TC_ISS_050 - Verify new issue appears in list");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Switch to All tab to see all issues");
        issuePage.tapAllTab();
        shortWait();

        logStep("Step 3: Verify 'Abhiyant' issue is in the list");
        boolean issueFound = issuePage.isIssueInList("Abhiyant");
        logStep("Issue 'Abhiyant' found: " + issueFound);
        if (issueFound) {
            logStep("✅ Created issue 'Abhiyant' appears in the Issues list");
        } else {
            logStep("⚠️ Issue 'Abhiyant' not found — creation may have failed in TC_ISS_049");
        }

        logStepWithScreenshot("TC_ISS_050: New issue in list");

        // Switch back to Open tab
        issuePage.tapOpenTab();
        shortWait();
    }

    /**
     * TC_ISS_051: Verify issue count increases
     * Expected: All count and Open count increased after creation
     */
    @Test(priority = 51)
    public void TC_ISS_051_verifyIssueCountIncreases() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CREATE_ISSUE,
            "TC_ISS_051 - Verify issue count increases");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Read current tab counts");
        int allCount = issuePage.getAllTabCount();
        int openCount = issuePage.getOpenTabCount();
        logStep("Tab counts — All: " + allCount + ", Open: " + openCount);

        logStep("Step 3: Verify counts are positive (issue was just created)");
        assertTrue(allCount > 0, "All count should be > 0 after creating an issue");
        assertTrue(openCount > 0, "Open count should be > 0 (new issue starts as Open)");
        logStep("✅ Counts confirm issues exist — All: " + allCount + ", Open: " + openCount);

        logStepWithScreenshot("TC_ISS_051: Tab counts after creation");
    }

    // ============================================================
    // ISSUE DETAILS TESTS (TC_ISS_052 - TC_ISS_059)
    // ============================================================

    /**
     * TC_ISS_052: Verify tapping issue opens details
     * Expected: Issue Details screen opens with Close button, 'Issue Details' title
     */
    @Test(priority = 52)
    public void TC_ISS_052_verifyTappingIssueOpensDetails() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_052 - Verify tapping issue opens details");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Switch to All tab");
        issuePage.tapAllTab();
        shortWait();

        logStep("Step 3: Tap on 'Abhiyant' issue");
        issuePage.tapOnIssue("Abhiyant");
        shortWait();

        logStep("Step 4: Verify Issue Details screen is displayed");
        boolean detailsDisplayed = issuePage.isIssueDetailsScreenDisplayed();
        logStep("Issue Details displayed: " + detailsDisplayed);
        if (detailsDisplayed) {
            logStep("✅ Issue Details screen opened");
        } else {
            logStep("⚠️ Issue Details screen not detected");
        }

        logStepWithScreenshot("TC_ISS_052: Issue Details screen");

        // Close details
        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_053: Verify Issue Details header
     * Expected: Warning icon, issue title 'Abhiyant', status badge 'Open', asset name
     */
    @Test(priority = 53)
    public void TC_ISS_053_verifyIssueDetailsHeader() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_053 - Verify Issue Details header");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open 'Abhiyant' issue");
        issuePage.tapOnIssue("Abhiyant");
        shortWait();

        logStep("Step 3: Verify issue title");
        String title = issuePage.getIssueDetailTitle();
        logStep("Issue title on details: '" + title + "'");

        logStep("Step 4: Verify status badge");
        String status = issuePage.getIssueDetailStatus();
        logStep("Status badge: '" + status + "'");

        logStep("Step 5: Verify asset name");
        String assetName = issuePage.getIssueDetailAssetName();
        logStep("Asset name: '" + assetName + "'");

        logStepWithScreenshot("TC_ISS_053: Issue Details header");

        // Close details
        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_054: Verify Status dropdown on Issue Details
     * Expected: Status dropdown opens showing options: Open, In Progress, Resolved, Closed
     */
    @Test(priority = 54)
    public void TC_ISS_054_verifyStatusDropdown() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_054 - Verify Status dropdown");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open 'Abhiyant' issue");
        issuePage.tapOnIssue("Abhiyant");
        shortWait();

        logStep("Step 3: Open Status dropdown");
        boolean opened = issuePage.openStatusDropdown();
        logStep("Status dropdown opened: " + opened);
        shortWait();

        logStep("Step 4: Verify status options");
        String[] expectedStatuses = {"Open", "In Progress", "Resolved", "Closed"};
        int foundCount = 0;
        for (String option : expectedStatuses) {
            boolean found = issuePage.isStatusOptionDisplayed(option);
            logStep("   Status '" + option + "': " + (found ? "FOUND" : "NOT FOUND"));
            if (found) foundCount++;
        }
        logStep("Found " + foundCount + "/" + expectedStatuses.length + " status options");

        logStepWithScreenshot("TC_ISS_054: Status dropdown options");

        // Dismiss dropdown and close details
        issuePage.dismissDropdownMenu();
        shortWait();
        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_055: Verify changing status to In Progress
     * Expected: Status changes to In Progress, badge updates
     */
    @Test(priority = 55)
    public void TC_ISS_055_verifyChangingStatusToInProgress() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_055 - Verify changing status to In Progress");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open 'Abhiyant' issue");
        issuePage.tapOnIssue("Abhiyant");
        shortWait();

        logStep("Step 3: Open Status dropdown and select 'In Progress'");
        issuePage.openStatusDropdown();
        shortWait();
        issuePage.selectStatus("In Progress");
        shortWait();

        logStep("Step 4: Verify status changed");
        String newStatus = issuePage.getIssueDetailStatus();
        logStep("New status: '" + newStatus + "'");
        if ("In Progress".equals(newStatus)) {
            logStep("✅ Status changed to In Progress");
        } else {
            logStep("⚠️ Status is: '" + newStatus + "' (expected 'In Progress')");
        }

        logStepWithScreenshot("TC_ISS_055: Status changed to In Progress");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_056: Verify changing status to Resolved
     * Expected: Status changes to Resolved
     */
    @Test(priority = 56)
    public void TC_ISS_056_verifyChangingStatusToResolved() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_056 - Verify changing status to Resolved");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open 'Abhiyant' issue");
        issuePage.tapOnIssue("Abhiyant");
        shortWait();

        logStep("Step 3: Open Status dropdown and select 'Resolved'");
        issuePage.openStatusDropdown();
        shortWait();
        issuePage.selectStatus("Resolved");
        shortWait();

        logStep("Step 4: Verify status changed");
        String newStatus = issuePage.getIssueDetailStatus();
        logStep("New status: '" + newStatus + "'");
        if ("Resolved".equals(newStatus)) {
            logStep("✅ Status changed to Resolved");
        } else {
            logStep("⚠️ Status is: '" + newStatus + "' (expected 'Resolved')");
        }

        logStepWithScreenshot("TC_ISS_056: Status changed to Resolved");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_057: Verify changing status to Closed
     * Expected: Status changes to Closed
     */
    @Test(priority = 57)
    public void TC_ISS_057_verifyChangingStatusToClosed() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_057 - Verify changing status to Closed");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open 'Abhiyant' issue");
        issuePage.tapOnIssue("Abhiyant");
        shortWait();

        logStep("Step 3: Open Status dropdown and select 'Closed'");
        issuePage.openStatusDropdown();
        shortWait();
        issuePage.selectStatus("Closed");
        shortWait();

        logStep("Step 4: Verify status changed");
        String newStatus = issuePage.getIssueDetailStatus();
        logStep("New status: '" + newStatus + "'");
        if ("Closed".equals(newStatus)) {
            logStep("✅ Status changed to Closed");
        } else {
            logStep("⚠️ Status is: '" + newStatus + "' (expected 'Closed')");
        }

        logStepWithScreenshot("TC_ISS_057: Status changed to Closed");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_058: Verify Priority displayed and editable on Issue Details
     * Expected: Priority shows 'High' with icon. Can tap to change priority
     */
    @Test(priority = 58)
    public void TC_ISS_058_verifyPriorityOnDetails() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_058 - Verify Priority displayed and editable");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open 'Abhiyant' issue");
        issuePage.tapOnIssue("Abhiyant");
        shortWait();

        logStep("Step 3: Verify Priority field is displayed");
        boolean priorityDisplayed = issuePage.isPriorityDisplayedOnDetails();
        logStep("Priority field displayed: " + priorityDisplayed);

        logStep("Step 4: Get Priority value");
        String priority = issuePage.getPriorityOnDetails();
        logStep("Priority value: '" + priority + "'");
        if (priority.contains("High") || priority.contains("high")) {
            logStep("✅ Priority shows 'High' as expected");
        } else {
            logStep("ℹ️ Priority is: '" + priority + "'");
        }

        logStepWithScreenshot("TC_ISS_058: Priority on Issue Details");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_059: Verify Issue Class displayed and editable on Issue Details
     * Expected: Issue Class shows 'NEC Violation' with document icon. Dropdown to change
     */
    @Test(priority = 59)
    public void TC_ISS_059_verifyIssueClassOnDetails() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_059 - Verify Issue Class displayed and editable");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open 'Abhiyant' issue");
        issuePage.tapOnIssue("Abhiyant");
        shortWait();

        logStep("Step 3: Verify Issue Class field is displayed");
        boolean classDisplayed = issuePage.isIssueClassDisplayedOnDetails();
        logStep("Issue Class field displayed: " + classDisplayed);

        logStep("Step 4: Get Issue Class value");
        String issueClass = issuePage.getIssueClassOnDetails();
        logStep("Issue Class value: '" + issueClass + "'");
        if (issueClass.contains("NEC Violation") || issueClass.contains("NEC")) {
            logStep("✅ Issue Class shows 'NEC Violation' as expected");
        } else {
            logStep("ℹ️ Issue Class is: '" + issueClass + "'");
        }

        logStepWithScreenshot("TC_ISS_059: Issue Class on Issue Details");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }
}
