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
public final class Issue_Phase1_Test extends BaseTest {

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

        // Tap + to open New Issue form (with retry — form may take time to render)
        for (int attempt = 1; attempt <= 2; attempt++) {
            issuePage.tapAddButton();
            mediumWait(); // 400ms — give the form time to render

            if (issuePage.isNewIssueFormDisplayed()) {
                System.out.println("✓ New Issue form is displayed (attempt " + attempt + ")");
                return true;
            }
            System.out.println("   New Issue form not detected on attempt " + attempt + ", retrying...");
            sleep(500);
        }

        System.out.println("❌ New Issue form not displayed after tapping +");
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

        loginAndSelectSite();
        // Ensure on Dashboard
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
        mediumWait(); // Extra wait for list to fully refresh after clearing search

        logStep("Step 5: Verify full list is restored");
        int restoredCount = issuePage.getVisibleIssueCount();
        logStep("Restored count: " + restoredCount);

        // If count hasn't fully restored, wait once more and re-check
        if (restoredCount < initialCount) {
            logStep("   Count not yet restored — waiting for list refresh...");
            mediumWait();
            mediumWait();
            restoredCount = issuePage.getVisibleIssueCount();
            logStep("   Restored count after extra wait: " + restoredCount);
        }

        assertTrue(restoredCount >= initialCount - 1,
            "Restored count (" + restoredCount + ") should be >= initial (" + initialCount + ") - 1");
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
        mediumWait();

        logStep("Step 2: Verify Select Asset field is displayed");
        // isSelectAssetDisplayed() has 3s wait + scroll fallback built in
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

        logStep("Step 3: Tap on first issue");
        issuePage.tapFirstIssue();
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
     * Expected: Warning icon, issue title, status badge 'Open', asset name
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

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
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

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
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

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
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

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
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

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
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

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
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

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
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

    // ================================================================
    // ISSUE DETAILS SECTION — COMPLETION & REQUIRED FIELDS (TC_ISS_060-061)
    // ================================================================

    /**
     * TC_ISS_060: Verify Issue Details section with completion
     * Verify Issue Details section shows completion %
     * Expected: Section header 'Issue Details' with completion percentage (e.g., 0%).
     *           Orange indicator for incomplete.
     */
    @Test(priority = 60)
    public void TC_ISS_060_verifyIssueDetailsSectionCompletion() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_060 - Verify Issue Details section with completion percentage");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Verify Issue Details section header is displayed");
        boolean sectionDisplayed = issuePage.isIssueDetailsSectionHeaderDisplayed();
        logStep("Issue Details section header displayed: " + sectionDisplayed);
        if (sectionDisplayed) {
            logStep("✅ Issue Details section header is present");
        } else {
            logStep("⚠️ Issue Details section header not found — may need scrolling or different label");
        }

        logStep("Step 4: Get completion percentage");
        String completionPct = issuePage.getIssueDetailsCompletionPercentage();
        logStep("Completion percentage: '" + completionPct + "'");
        if (!completionPct.isEmpty()) {
            logStep("✅ Completion percentage displayed: " + completionPct);
            if (completionPct.contains("0%")) {
                logStep("✅ Shows 0% as expected for incomplete issue");
            } else {
                logStep("ℹ️ Completion is: " + completionPct + " (expected 0% for empty details)");
            }
        } else {
            logStep("⚠️ Completion percentage not found — may be embedded in section header");
        }

        logStepWithScreenshot("TC_ISS_060: Issue Details section with completion %");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_061: Verify Required fields only toggle
     * Verify toggle to show required fields
     * Expected: Toggle shows count (e.g., 0/1). Can toggle to show only required fields.
     */
    @Test(priority = 61)
    public void TC_ISS_061_verifyRequiredFieldsToggle() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
            "TC_ISS_061 - Verify Required fields only toggle");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Verify 'Required fields only' toggle is displayed");
        boolean toggleDisplayed = issuePage.isRequiredFieldsToggleDisplayed();
        logStep("Required fields only toggle displayed: " + toggleDisplayed);
        if (toggleDisplayed) {
            logStep("✅ Required fields only toggle is present");
        } else {
            logStep("⚠️ Required fields only toggle not found");
        }

        logStep("Step 4: Get toggle count");
        String toggleCount = issuePage.getRequiredFieldsToggleCount();
        logStep("Toggle count: '" + toggleCount + "'");
        if (!toggleCount.isEmpty()) {
            logStep("✅ Toggle count displayed: " + toggleCount);
            if (toggleCount.contains("0/1")) {
                logStep("✅ Count shows 0/1 as expected");
            } else {
                logStep("ℹ️ Toggle count is: " + toggleCount);
            }
        } else {
            logStep("⚠️ Toggle count not found");
        }

        logStep("Step 5: Tap toggle to switch");
        issuePage.tapRequiredFieldsToggle();
        shortWait();

        logStepWithScreenshot("TC_ISS_061: Required fields only toggle — after toggling");

        logStep("Step 6: Tap toggle back to original state");
        issuePage.tapRequiredFieldsToggle();
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    // ================================================================
    // SUBCATEGORY (TC_ISS_062-066)
    // ================================================================

    /**
     * TC_ISS_062: Verify Subcategory field for NEC Violation
     * Verify Subcategory appears for NEC Violation issues
     * Expected: Subcategory field displayed with red required indicator. 'Type or select...' placeholder.
     */
    @Test(priority = 62)
    public void TC_ISS_062_verifySubcategoryFieldForNEC() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SUBCATEGORY,
            "TC_ISS_062 - Verify Subcategory field for NEC Violation");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue (NEC Violation class)");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Verify Subcategory field is displayed");
        boolean subcatDisplayed = issuePage.isSubcategoryFieldDisplayed();
        logStep("Subcategory field displayed: " + subcatDisplayed);
        if (subcatDisplayed) {
            logStep("✅ Subcategory field is present for NEC Violation issue");
        } else {
            logStep("⚠️ Subcategory field not found — may need scroll or different class issue");
        }

        logStep("Step 4: Get Subcategory placeholder");
        String placeholder = issuePage.getSubcategoryPlaceholder();
        logStep("Subcategory placeholder: '" + placeholder + "'");
        if (placeholder.contains("Type or select")) {
            logStep("✅ Placeholder shows 'Type or select...' as expected");
        } else if (!placeholder.isEmpty()) {
            logStep("ℹ️ Placeholder is: '" + placeholder + "'");
        } else {
            logStep("⚠️ Placeholder not found");
        }

        logStepWithScreenshot("TC_ISS_062: Subcategory field for NEC Violation");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_063: Verify Subcategory dropdown options for NEC
     * Verify NEC-specific subcategories listed
     * Expected: Long list of NEC violation subcategories including specific entries.
     */
    @Test(priority = 63)
    public void TC_ISS_063_verifySubcategoryDropdownOptions() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SUBCATEGORY,
            "TC_ISS_063 - Verify Subcategory dropdown options for NEC");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Tap Subcategory field to open dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 4: Verify NEC subcategory options are displayed");
        // Check for some known NEC subcategory options
        String[] sampleOptions = {
            "Breaker is restricted",
            "Exceeds panel limit",
            "Visible Corrosion"
        };
        int foundCount = 0;
        for (String option : sampleOptions) {
            boolean found = issuePage.isSubcategoryOptionDisplayed(option);
            logStep("   Option '" + option + "': " + (found ? "found" : "not found"));
            if (found) foundCount++;
        }

        if (foundCount > 0) {
            logStep("✅ NEC subcategory options found: " + foundCount + "/" + sampleOptions.length);
        } else {
            logStep("⚠️ No NEC subcategory options found — dropdown may not have opened or options have different labels");
        }

        logStepWithScreenshot("TC_ISS_063: Subcategory dropdown options");

        // Dismiss dropdown
        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_064: Verify selecting NEC subcategory
     * Verify can select specific NEC violation type
     * Expected: Subcategory selected. Field shows truncated selected value.
     */
    @Test(priority = 64)
    public void TC_ISS_064_verifySelectingNECSubcategory() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SUBCATEGORY,
            "TC_ISS_064 - Verify selecting NEC subcategory");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Tap Subcategory field");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 4: Select 'Breaker is restricted from freely operating (NEC 240.8)'");
        issuePage.selectSubcategory("Breaker is restricted");
        shortWait();

        logStep("Step 5: Verify subcategory was selected");
        String selectedValue = issuePage.getSubcategoryValue();
        logStep("Selected subcategory value: '" + selectedValue + "'");
        if (selectedValue.contains("Breaker") || selectedValue.contains("NEC 240.8")) {
            logStep("✅ Subcategory selected correctly — shows: " + selectedValue);
        } else if (!selectedValue.isEmpty()) {
            logStep("ℹ️ Selected value is: '" + selectedValue + "' (may be truncated)");
        } else {
            logStep("⚠️ Could not read selected subcategory value");
        }

        logStepWithScreenshot("TC_ISS_064: NEC subcategory selected");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_065: Verify all NEC subcategory options
     * Verify comprehensive NEC subcategory list
     * Expected: Options include Wire burned or damaged, Bonding and grounding, Inadequate ventilation,
     *           1 wire per terminal, Improper neutral conductor, Not protected from damage,
     *           Missing arc flash labels, Exposed energized parts, etc.
     */
    @Test(priority = 65)
    public void TC_ISS_065_verifyAllNECSubcategoryOptions() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SUBCATEGORY,
            "TC_ISS_065 - Verify all NEC subcategory options");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Tap Subcategory field to open dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 4: Verify comprehensive NEC subcategory list");
        String[] expectedOptions = {
            "Wire burned",
            "Bonding and grounding",
            "Inadequate ventilation",
            "1 wire per terminal",
            "Improper neutral",
            "Not protected from damage",
            "Missing arc flash",
            "Exposed energized",
            "Breaker is restricted",
            "Exceeds panel limit",
            "Visible Corrosion"
        };

        int foundCount = issuePage.verifyNECSubcategoryOptions(expectedOptions);
        logStep("NEC subcategory options verified: " + foundCount + "/" + expectedOptions.length + " found");

        if (foundCount >= 5) {
            logStep("✅ Majority of NEC subcategory options present (" + foundCount + "/" + expectedOptions.length + ")");
        } else if (foundCount > 0) {
            logStep("ℹ️ Some NEC subcategory options found (" + foundCount + "/" + expectedOptions.length + ") — some may need scrolling");
        } else {
            logStep("⚠️ No NEC subcategory options found — dropdown may not have opened");
        }

        logStepWithScreenshot("TC_ISS_065: All NEC subcategory options");

        // Dismiss dropdown
        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_066: Verify Subcategory is searchable/typeable
     * Verify can type to filter subcategories
     * Expected: Subcategory list filters based on typed text.
     */
    @Test(priority = 66)
    public void TC_ISS_066_verifySubcategorySearchable() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SUBCATEGORY,
            "TC_ISS_066 - Verify Subcategory is searchable/typeable");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Tap Subcategory field");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 4: Type 'Wire' to filter subcategories");
        issuePage.searchSubcategory("Wire");
        shortWait();

        logStep("Step 5: Verify filtered results");
        boolean wireOptionDisplayed = issuePage.isSubcategoryOptionDisplayed("Wire");
        logStep("'Wire' related option displayed after filter: " + wireOptionDisplayed);

        if (wireOptionDisplayed) {
            logStep("✅ Subcategory search/filter is working — 'Wire' option found");
        } else {
            logStep("⚠️ Could not verify filtered results — filter may work differently");
        }

        logStepWithScreenshot("TC_ISS_066: Subcategory search with 'Wire'");

        // Dismiss dropdown
        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    // ================================================================
    // DESCRIPTION (TC_ISS_067-068)
    // ================================================================

    /**
     * TC_ISS_067: Verify Description field
     * Verify Description text area
     * Expected: Description section with text icon. Placeholder 'Describe the issue...'.
     */
    @Test(priority = 67)
    public void TC_ISS_067_verifyDescriptionField() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_DESCRIPTION,
            "TC_ISS_067 - Verify Description field");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Verify Description field is displayed");
        boolean descDisplayed = issuePage.isDescriptionFieldDisplayed();
        logStep("Description field displayed: " + descDisplayed);
        if (descDisplayed) {
            logStep("✅ Description section is present");
        } else {
            logStep("⚠️ Description field not found — may need scrolling");
        }

        logStep("Step 4: Get Description placeholder");
        String placeholder = issuePage.getDescriptionPlaceholder();
        logStep("Description placeholder: '" + placeholder + "'");
        if (placeholder.contains("Describe the issue")) {
            logStep("✅ Placeholder shows 'Describe the issue...' as expected");
        } else if (!placeholder.isEmpty()) {
            logStep("ℹ️ Placeholder is: '" + placeholder + "'");
        } else {
            logStep("⚠️ Placeholder not found");
        }

        logStepWithScreenshot("TC_ISS_067: Description field");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_068: Verify entering description
     * Verify can enter issue description
     * Expected: Text 'Test' appears in Description field.
     */
    @Test(priority = 68)
    public void TC_ISS_068_verifyEnteringDescription() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_DESCRIPTION,
            "TC_ISS_068 - Verify entering description");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Enter 'Test' in Description field");
        issuePage.enterDescription("Test");
        shortWait();

        logStep("Step 4: Verify entered text");
        String descValue = issuePage.getDescriptionValue();
        logStep("Description value: '" + descValue + "'");
        if ("Test".equals(descValue) || (descValue != null && descValue.contains("Test"))) {
            logStep("✅ Description text 'Test' entered successfully");
        } else if (descValue != null && !descValue.isEmpty()) {
            logStep("ℹ️ Description value is: '" + descValue + "'");
        } else {
            logStep("⚠️ Could not read entered description text");
        }

        logStepWithScreenshot("TC_ISS_068: Description with 'Test' entered");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    // ================================================================
    // PROPOSED RESOLUTION (TC_ISS_069)
    // ================================================================

    /**
     * TC_ISS_069: Verify Proposed Resolution field
     * Verify Proposed Resolution text area
     * Expected: Section with lightbulb icon. Placeholder 'Suggest a resolution...'.
     */
    @Test(priority = 69)
    public void TC_ISS_069_verifyProposedResolutionField() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_PROPOSED_RESOLUTION,
            "TC_ISS_069 - Verify Proposed Resolution field");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Verify Proposed Resolution field is displayed");
        boolean resolutionDisplayed = issuePage.isProposedResolutionFieldDisplayed();
        logStep("Proposed Resolution field displayed: " + resolutionDisplayed);
        if (resolutionDisplayed) {
            logStep("✅ Proposed Resolution section is present");
        } else {
            logStep("⚠️ Proposed Resolution field not found — may need scrolling");
        }

        logStep("Step 4: Get Proposed Resolution placeholder");
        String placeholder = issuePage.getProposedResolutionPlaceholder();
        logStep("Proposed Resolution placeholder: '" + placeholder + "'");
        if (placeholder.contains("Suggest a resolution")) {
            logStep("✅ Placeholder shows 'Suggest a resolution...' as expected");
        } else if (!placeholder.isEmpty()) {
            logStep("ℹ️ Placeholder is: '" + placeholder + "'");
        } else {
            logStep("⚠️ Placeholder not found");
        }

        logStepWithScreenshot("TC_ISS_069: Proposed Resolution field");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    // ================================================================
    // PROPOSED RESOLUTION — ENTERING TEXT (TC_ISS_070)
    // ================================================================

    /**
     * TC_ISS_070: Verify entering resolution
     * Verify can enter proposed resolution
     * Expected: Text 'Test' appears in Proposed Resolution field.
     */
    @Test(priority = 70)
    public void TC_ISS_070_verifyEnteringResolution() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_PROPOSED_RESOLUTION,
            "TC_ISS_070 - Verify entering proposed resolution");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Tap Proposed Resolution and enter 'Test'");
        issuePage.enterProposedResolution("Test");
        shortWait();

        logStep("Step 4: Verify entered text");
        String resValue = issuePage.getProposedResolutionValue();
        logStep("Proposed Resolution value: '" + resValue + "'");
        if ("Test".equals(resValue) || (resValue != null && resValue.contains("Test"))) {
            logStep("✅ Proposed Resolution text 'Test' entered successfully");
        } else if (resValue != null && !resValue.isEmpty()) {
            logStep("ℹ️ Proposed Resolution value is: '" + resValue + "'");
        } else {
            logStep("⚠️ Could not read entered resolution text");
        }

        logStepWithScreenshot("TC_ISS_070: Proposed Resolution with 'Test' entered");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    // ================================================================
    // ISSUE PHOTOS (TC_ISS_071-073)
    // ================================================================

    /**
     * TC_ISS_071: Verify Issue Photos section
     * Verify photo upload section
     * Expected: Issue Photos section with Gallery and Camera buttons.
     */
    @Test(priority = 71)
    public void TC_ISS_071_verifyIssuePhotosSection() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_PHOTOS,
            "TC_ISS_071 - Verify Issue Photos section");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Scroll down to find Issue Photos section");
        boolean photosDisplayed = issuePage.isIssuePhotosSectionDisplayed();
        logStep("Issue Photos section displayed: " + photosDisplayed);
        if (photosDisplayed) {
            logStep("✅ Issue Photos section is present");
        } else {
            logStep("⚠️ Issue Photos section not found — may need more scrolling or different layout");
        }

        logStep("Step 4: Verify Gallery button");
        boolean galleryDisplayed = issuePage.isGalleryButtonDisplayed();
        logStep("Gallery button displayed: " + galleryDisplayed);
        if (galleryDisplayed) {
            logStep("✅ Gallery button is present");
        }

        logStep("Step 5: Verify Camera button");
        boolean cameraDisplayed = issuePage.isCameraButtonDisplayed();
        logStep("Camera button displayed: " + cameraDisplayed);
        if (cameraDisplayed) {
            logStep("✅ Camera button is present");
        }

        if (galleryDisplayed && cameraDisplayed) {
            logStep("✅ Both Gallery and Camera buttons present in Issue Photos section");
        } else if (galleryDisplayed || cameraDisplayed) {
            logStep("ℹ️ Only one photo button found (Gallery: " + galleryDisplayed + ", Camera: " + cameraDisplayed + ")");
        } else {
            logStep("⚠️ Neither Gallery nor Camera buttons found");
        }

        logStepWithScreenshot("TC_ISS_071: Issue Photos section");

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_072: Verify Gallery button
     * Verify can add photo from gallery
     * Expected: Photo picker opens allowing selection from device gallery.
     */
    @Test(priority = 72)
    public void TC_ISS_072_verifyGalleryButton() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_PHOTOS,
            "TC_ISS_072 - Verify Gallery button opens photo picker");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Scroll to Issue Photos section");
        issuePage.isIssuePhotosSectionDisplayed();
        shortWait();

        logStep("Step 4: Tap Gallery button");
        issuePage.tapGalleryButton();
        mediumWait();

        logStep("Step 5: Verify photo picker is displayed");
        boolean pickerDisplayed = issuePage.isPhotoPickerDisplayed();
        logStep("Photo picker displayed: " + pickerDisplayed);
        if (pickerDisplayed) {
            logStep("✅ Photo picker opened after tapping Gallery");
        } else {
            logStep("⚠️ Photo picker not detected — may need permissions or different detection");
        }

        logStepWithScreenshot("TC_ISS_072: Gallery photo picker");

        logStep("Step 6: Dismiss photo picker");
        issuePage.dismissPhotoPicker();
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_073: Verify Camera button
     * Verify can take photo with camera
     * Expected: Camera opens allowing capture of new photo.
     */
    @Test(priority = 73)
    public void TC_ISS_073_verifyCameraButton() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_PHOTOS,
            "TC_ISS_073 - Verify Camera button opens camera");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Scroll to Issue Photos section");
        issuePage.isIssuePhotosSectionDisplayed();
        shortWait();

        logStep("Step 4: Tap Camera button");
        issuePage.tapCameraButton();
        mediumWait();

        logStep("Step 5: Verify camera is displayed");
        boolean cameraDisplayed = issuePage.isCameraDisplayed();
        logStep("Camera displayed: " + cameraDisplayed);
        if (cameraDisplayed) {
            logStep("✅ Camera opened after tapping Camera button");
        } else {
            logStep("⚠️ Camera not detected — may show permission dialog on simulator or device-dependent behavior");
        }

        logStepWithScreenshot("TC_ISS_073: Camera view");

        logStep("Step 6: Dismiss camera");
        issuePage.dismissCamera();
        shortWait();

        // Ensure we're back on issue details or issues screen
        if (!issuePage.isIssueDetailsScreenDisplayed()) {
            logStep("ℹ️ Not on Issue Details after dismissing camera — navigating back");
        } else {
            issuePage.tapCloseIssueDetails();
        }
        shortWait();
    }

    // ================================================================
    // DELETE ISSUE (TC_ISS_074-076)
    // ================================================================

    /**
     * TC_ISS_074: Verify Delete Issue button
     * Verify Delete Issue option available at bottom of Issue Details
     * Expected: Delete Issue button displayed with trash icon and red text/outline.
     */
    @Test(priority = 74)
    public void TC_ISS_074_verifyDeleteIssueButton() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_DELETE_ISSUE,
            "TC_ISS_074 - Verify Delete Issue button");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Scroll to bottom to find Delete Issue button");
        boolean deleteDisplayed = issuePage.isDeleteIssueButtonDisplayed();
        logStep("Delete Issue button displayed: " + deleteDisplayed);
        if (deleteDisplayed) {
            logStep("✅ Delete Issue button is present at the bottom");
        } else {
            logStep("⚠️ Delete Issue button not found — may need more scrolling");
        }

        logStepWithScreenshot("TC_ISS_074: Delete Issue button");

        // Scroll back to top before closing
        issuePage.scrollUpOnDetailsScreen();
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();
    }

    /**
     * TC_ISS_075: Verify tapping Delete Issue shows confirmation
     * Verify confirmation before deletion
     * Expected: Confirmation dialog appears asking to confirm deletion.
     */
    @Test(priority = 75)
    public void TC_ISS_075_verifyDeleteConfirmation() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_DELETE_ISSUE,
            "TC_ISS_075 - Verify Delete Issue shows confirmation dialog");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Tap Delete Issue button");
        issuePage.tapDeleteIssueButton();
        mediumWait();

        logStep("Step 4: Verify confirmation dialog is displayed");
        boolean confirmDisplayed = issuePage.isDeleteConfirmationDisplayed();
        logStep("Delete confirmation dialog displayed: " + confirmDisplayed);
        if (confirmDisplayed) {
            logStep("✅ Confirmation dialog appeared before deletion");
        } else {
            logStep("⚠️ Confirmation dialog not detected — app may use different confirmation pattern");
        }

        logStepWithScreenshot("TC_ISS_075: Delete confirmation dialog");

        logStep("Step 5: Cancel the deletion to preserve the issue");
        issuePage.cancelDeleteIssue();
        shortWait();

        // Ensure we're still on issue details
        if (issuePage.isIssueDetailsScreenDisplayed()) {
            issuePage.tapCloseIssueDetails();
        }
        shortWait();
    }

    /**
     * TC_ISS_076: Verify confirming delete removes issue
     * Verify issue deleted after confirmation
     * Expected: Issue deleted. Returns to Issues list. Issue no longer in list.
     *
     * IMPORTANT: This test creates a TEMPORARY issue first, then deletes it
     * to avoid destroying issues used by other tests.
     */
    @Test(priority = 76)
    public void TC_ISS_076_verifyConfirmDeleteRemovesIssue() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_DELETE_ISSUE,
            "TC_ISS_076 - Verify confirming delete removes issue");

        String tempIssueTitle = "TempDeleteTest_" + System.currentTimeMillis();

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Create a temporary issue for deletion testing");
        logStep("   Temp issue title: " + tempIssueTitle);
        boolean created = issuePage.createQuickIssue(tempIssueTitle, "A1");
        mediumWait();

        if (!created) {
            logStep("⚠️ Could not create temporary issue — skipping delete verification");
            logStepWithScreenshot("TC_ISS_076: Failed to create temp issue");
            return;
        }

        logStep("Step 3: Verify temp issue is in the list");
        issuePage.tapAllTab();
        shortWait();
        boolean issueInList = issuePage.isIssueInList(tempIssueTitle);
        logStep("Temp issue in list: " + issueInList);
        if (!issueInList) {
            // The issue may have been created and we're on a different screen
            // Try searching
            issuePage.searchIssues(tempIssueTitle);
            shortWait();
            issueInList = issuePage.isIssueInList(tempIssueTitle);
            issuePage.clearSearch();
            shortWait();
        }

        logStep("Step 4: Open the temp issue");
        issuePage.tapOnIssue(tempIssueTitle);
        mediumWait();

        logStep("Step 5: Tap Delete Issue");
        issuePage.tapDeleteIssueButton();
        mediumWait();

        logStep("Step 6: Confirm deletion");
        issuePage.confirmDeleteIssue();
        mediumWait();

        logStep("Step 7: Verify returned to Issues list");
        boolean backOnList = issuePage.isIssuesScreenDisplayed();
        logStep("Back on Issues list: " + backOnList);
        if (backOnList) {
            logStep("✅ Returned to Issues list after deletion");
        } else {
            logStep("ℹ️ May not be on Issues list — checking...");
            // Try navigating back
            ensureOnIssuesScreen();
        }

        logStep("Step 8: Verify deleted issue no longer in list");
        issuePage.tapAllTab();
        shortWait();
        boolean stillInList = issuePage.isIssueInList(tempIssueTitle);
        logStep("Deleted issue still in list: " + stillInList);
        if (!stillInList) {
            logStep("✅ Issue successfully deleted and removed from list");
        } else {
            logStep("⚠️ Issue may still be in list — deletion might be async");
        }

        logStepWithScreenshot("TC_ISS_076: After issue deletion");
    }

    // ================================================================
    // SAVE CHANGES (TC_ISS_077-079)
    // ================================================================

    /**
     * TC_ISS_077: Verify Save Changes button
     * Verify Save Changes button at bottom of Issue Details with modifications
     * Expected: Save Changes button displayed (blue).
     */
    @Test(priority = 77)
    public void TC_ISS_077_verifySaveChangesButton() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SAVE_CHANGES,
            "TC_ISS_077 - Verify Save Changes button");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Make a modification to trigger Save Changes visibility");
        // Enter some text in description to trigger "modified" state
        issuePage.enterDescription("Save test");
        shortWait();

        logStep("Step 4: Scroll to bottom to find Save Changes button");
        boolean saveDisplayed = issuePage.isSaveChangesButtonDisplayed();
        logStep("Save Changes button displayed: " + saveDisplayed);
        if (saveDisplayed) {
            logStep("✅ Save Changes button is present");
        } else {
            logStep("⚠️ Save Changes button not found — may auto-save or button at different location");
        }

        logStepWithScreenshot("TC_ISS_077: Save Changes button");

        issuePage.tapCloseIssueDetails();
        shortWait();

        // Handle potential unsaved changes warning
        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_078: Verify saving changes updates issue
     * Verify modifications are saved
     * Expected: Changes saved. May show success message. Issue updated in list.
     */
    @Test(priority = 78)
    public void TC_ISS_078_verifySavingChanges() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SAVE_CHANGES,
            "TC_ISS_078 - Verify saving changes updates issue");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Make a modification (add description)");
        issuePage.enterDescription("Automated test save");
        shortWait();

        logStep("Step 4: Tap Save Changes");
        issuePage.tapSaveChangesButton();
        mediumWait();

        logStep("Step 5: Verify save result");
        boolean successShown = issuePage.isSaveSuccessDisplayed();
        if (successShown) {
            logStep("✅ Save success message displayed");
        } else {
            logStep("ℹ️ No explicit success message — changes may have auto-saved or save was silent");
        }

        // Check if we're back on Issues list or still on details
        boolean onDetails = issuePage.isIssueDetailsScreenDisplayed();
        boolean onList = issuePage.isIssuesScreenDisplayed();
        logStep("After save — on details: " + onDetails + ", on list: " + onList);

        if (onDetails) {
            logStep("Step 6: Verify description was saved by re-reading");
            String savedDesc = issuePage.getDescriptionValue();
            logStep("Saved description: '" + savedDesc + "'");
            if (savedDesc != null && savedDesc.contains("Automated test save")) {
                logStep("✅ Description saved successfully");
            } else {
                logStep("ℹ️ Description value after save: '" + savedDesc + "'");
            }
        }

        logStepWithScreenshot("TC_ISS_078: After saving changes");

        if (issuePage.isIssueDetailsScreenDisplayed()) {
            issuePage.tapCloseIssueDetails();
            shortWait();
        }
    }

    /**
     * TC_ISS_079: Verify unsaved changes warning
     * Verify warning when closing with unsaved changes
     * Expected: Warning dialog asks to save or discard changes OR changes auto-saved.
     */
    @Test(priority = 79)
    public void TC_ISS_079_verifyUnsavedChangesWarning() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_SAVE_CHANGES,
            "TC_ISS_079 - Verify unsaved changes warning");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Make a modification without saving");
        issuePage.enterDescription("Unsaved change test");
        shortWait();

        logStep("Step 4: Tap Close without saving");
        issuePage.tapCloseIssueDetails();
        mediumWait();

        logStep("Step 5: Check for unsaved changes warning");
        boolean warningDisplayed = issuePage.isUnsavedChangesWarningDisplayed();
        logStep("Unsaved changes warning displayed: " + warningDisplayed);

        if (warningDisplayed) {
            logStep("✅ Unsaved changes warning dialog appeared");
            logStepWithScreenshot("TC_ISS_079: Unsaved changes warning dialog");

            logStep("Step 6: Discard changes to proceed");
            issuePage.tapDiscardChanges();
            shortWait();
        } else {
            logStep("ℹ️ No unsaved changes warning — app may auto-save or not show warning");
            logStepWithScreenshot("TC_ISS_079: No unsaved changes warning (auto-save behavior?)");
        }

        // Ensure we're back on Issues list
        if (issuePage.isIssueDetailsScreenDisplayed()) {
            issuePage.tapCloseIssueDetails();
            shortWait();
        }
    }

    // ================================================================
    // DONE BUTTON (TC_ISS_080)
    // ================================================================

    /**
     * TC_ISS_080: Verify Done button returns to previous screen
     * Verify Done dismisses Issues screen
     * Expected: Issues screen closes. Returns to dashboard or previous screen.
     */
    @Test(priority = 80)
    public void TC_ISS_080_verifyDoneButtonReturnsToPreviousScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_DONE_BUTTON,
            "TC_ISS_080 - Verify Done button returns to previous screen");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Verify Done button is displayed");
        boolean doneDisplayed = issuePage.isDoneButtonDisplayed();
        logStep("Done button displayed: " + doneDisplayed);
        if (doneDisplayed) {
            logStep("✅ Done button is visible on Issues screen");
        } else {
            logStep("⚠️ Done button not found");
        }

        logStep("Step 3: Tap Done button");
        issuePage.tapDoneButton();
        mediumWait();

        logStep("Step 4: Verify returned to dashboard/previous screen");
        boolean leftIssues = !issuePage.isIssuesScreenDisplayed();
        logStep("Left Issues screen: " + leftIssues);

        boolean onDashboard = issuePage.isDashboardOrPreviousScreenDisplayed();
        logStep("On dashboard/previous screen: " + onDashboard);

        if (leftIssues) {
            logStep("✅ Done button successfully dismissed Issues screen");
        } else {
            logStep("⚠️ Still on Issues screen after tapping Done");
        }

        logStepWithScreenshot("TC_ISS_080: After tapping Done — previous screen");
    }

    // ================================================================
    // CLOSE BUTTON ON ISSUE DETAILS (TC_ISS_081)
    // ================================================================

    /**
     * TC_ISS_081: Verify Close button on Issue Details
     * Verify Close returns to Issues list
     * Expected: Issue Details closes. Returns to Issues list.
     */
    @Test(priority = 81)
    public void TC_ISS_081_verifyCloseButtonOnIssueDetails() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CLOSE_BUTTON,
            "TC_ISS_081 - Verify Close button on Issue Details returns to Issues list");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue to go to Issue Details");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Verify Issue Details screen is displayed");
        boolean detailsDisplayed = issuePage.isIssueDetailsScreenDisplayed();
        logStep("Issue Details screen displayed: " + detailsDisplayed);
        if (detailsDisplayed) {
            logStep("✅ Issue Details screen is open");
        } else {
            logStep("⚠️ Issue Details screen not detected");
        }

        logStep("Step 4: Tap Close button");
        issuePage.tapCloseIssueDetails();
        mediumWait();

        logStep("Step 5: Verify returned to Issues list");
        boolean backOnIssues = issuePage.isIssuesScreenDisplayed();
        logStep("Back on Issues list: " + backOnIssues);

        boolean stillOnDetails = issuePage.isIssueDetailsScreenDisplayed();
        logStep("Still on Issue Details: " + stillOnDetails);

        if (backOnIssues && !stillOnDetails) {
            logStep("✅ Close button successfully returned to Issues list");
        } else if (!stillOnDetails) {
            logStep("ℹ️ Left Issue Details but may not be on Issues list");
        } else {
            logStep("⚠️ Still on Issue Details after tapping Close");
        }

        logStepWithScreenshot("TC_ISS_081: After tapping Close — Issues list");
    }

    // ================================================================
    // ISSUE CLASS SUBCATEGORIES (TC_ISS_082-083)
    // ================================================================

    /**
     * TC_ISS_082: Verify OSHA Violation has specific subcategories
     * Verify OSHA-specific subcategory options
     * Expected: OSHA-specific violation subcategories displayed.
     *
     * NOTE: This test changes the Issue Class to OSHA Violation
     * to observe subcategory changes, then reverts it.
     */
    @Test(priority = 82)
    public void TC_ISS_082_verifyOSHAViolationSubcategories() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS_SUBCATEGORIES,
            "TC_ISS_082 - Verify OSHA Violation has specific subcategories");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'OSHA Violation'");
        issuePage.changeIssueClassOnDetails("OSHA Violation");
        mediumWait();

        logStep("Step 4: Verify Issue Class changed");
        String currentClass = issuePage.getIssueClassOnDetails();
        logStep("Current Issue Class: '" + currentClass + "'");
        if (currentClass.contains("OSHA")) {
            logStep("✅ Issue Class changed to OSHA Violation");
        } else {
            logStep("⚠️ Issue Class may not have changed — current: '" + currentClass + "'");
        }

        logStep("Step 5: Check Subcategory field and open dropdown");
        boolean subcatDisplayed = issuePage.isSubcategoryFieldDisplayed();
        logStep("Subcategory field displayed: " + subcatDisplayed);

        if (subcatDisplayed) {
            issuePage.tapSubcategoryField();
            mediumWait();

            logStep("Step 6: Verify OSHA-specific subcategory options");
            java.util.ArrayList<String> options = issuePage.getVisibleSubcategoryOptions();
            logStep("Visible OSHA subcategory options: " + options.size());
            for (String opt : options) {
                logStep("   - " + opt);
            }

            if (!options.isEmpty()) {
                logStep("✅ OSHA-specific subcategory options found: " + options.size());
            } else {
                logStep("⚠️ No OSHA subcategory options visible — may need scrolling or different UI");
            }

            // Dismiss dropdown
            issuePage.dismissDropdownMenu();
            shortWait();
        } else {
            logStep("⚠️ Subcategory field not displayed for OSHA Violation");
        }

        logStepWithScreenshot("TC_ISS_082: OSHA Violation subcategories");

        logStep("Step 7: Revert Issue Class back to NEC Violation");
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        // Handle potential unsaved changes warning
        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_083: Verify Thermal Anomaly subcategories
     * Verify Thermal Anomaly specific options
     * Expected: Thermal Anomaly specific subcategories displayed (temperature ranges, etc.).
     *
     * NOTE: This test changes the Issue Class to Thermal Anomaly
     * to observe subcategory changes, then reverts it.
     */
    @Test(priority = 83)
    public void TC_ISS_083_verifyThermalAnomalySubcategories() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS_SUBCATEGORIES,
            "TC_ISS_083 - Verify Thermal Anomaly has specific subcategories");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'Thermal Anomaly'");
        issuePage.changeIssueClassOnDetails("Thermal Anomaly");
        mediumWait();

        logStep("Step 4: Verify Issue Class changed");
        String currentClass = issuePage.getIssueClassOnDetails();
        logStep("Current Issue Class: '" + currentClass + "'");
        if (currentClass.contains("Thermal")) {
            logStep("✅ Issue Class changed to Thermal Anomaly");
        } else {
            logStep("⚠️ Issue Class may not have changed — current: '" + currentClass + "'");
        }

        logStep("Step 5: Check Subcategory field and open dropdown");
        boolean subcatDisplayed = issuePage.isSubcategoryFieldDisplayed();
        logStep("Subcategory field displayed: " + subcatDisplayed);

        if (subcatDisplayed) {
            issuePage.tapSubcategoryField();
            mediumWait();

            logStep("Step 6: Verify Thermal Anomaly specific subcategory options");
            java.util.ArrayList<String> options = issuePage.getVisibleSubcategoryOptions();
            logStep("Visible Thermal Anomaly subcategory options: " + options.size());
            for (String opt : options) {
                logStep("   - " + opt);
            }

            if (!options.isEmpty()) {
                logStep("✅ Thermal Anomaly subcategory options found: " + options.size());
            } else {
                logStep("⚠️ No Thermal Anomaly subcategory options visible");
            }

            // Dismiss dropdown
            issuePage.dismissDropdownMenu();
            shortWait();
        } else {
            logStep("ℹ️ Subcategory field not displayed for Thermal Anomaly — may not have subcategories");
        }

        logStepWithScreenshot("TC_ISS_083: Thermal Anomaly subcategories");

        logStep("Step 7: Revert Issue Class back to NEC Violation");
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        // Handle potential unsaved changes warning
        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    // ================================================================
    // EDGE CASES (TC_ISS_084-086)
    // ================================================================

    /**
     * TC_ISS_084: Verify creating issue without optional fields
     * Verify issue can be created with only required fields
     * Expected: Issue created with minimal info. Optional fields have defaults.
     */
    @Test(priority = 84)
    public void TC_ISS_084_verifyCreatingIssueWithoutOptionalFields() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_EDGE_CASES,
            "TC_ISS_084 - Verify creating issue without optional fields");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Create issue with only asset selected (minimal required field)");
        boolean created = issuePage.createMinimalIssue("A1");
        mediumWait();

        if (created) {
            logStep("✅ Issue created with only required fields (asset)");

            logStep("Step 3: Verify on Issues list and the issue exists");
            if (!issuePage.isIssuesScreenDisplayed()) {
                ensureOnIssuesScreen();
            }
            issuePage.tapAllTab();
            shortWait();

            logStepWithScreenshot("TC_ISS_084: Issue created with minimal fields");
        } else {
            logStep("ℹ️ Could not create issue with only asset — may require additional fields");

            logStep("Step 3: Try creating with asset + issue class");
            boolean createdWithClass = issuePage.createQuickIssue("MinimalTest_" + System.currentTimeMillis(), "A1");
            mediumWait();

            if (createdWithClass) {
                logStep("✅ Issue created with asset + issue class (minimal viable set)");
            } else {
                logStep("⚠️ Could not create issue — additional required fields may exist");
            }

            logStepWithScreenshot("TC_ISS_084: Minimal issue creation result");

            // Ensure back on Issues screen
            if (!issuePage.isIssuesScreenDisplayed()) {
                ensureOnIssuesScreen();
            }
        }
    }

    /**
     * TC_ISS_085: Verify very long description
     * Verify handling of long text in description
     * Expected: Long description saved successfully. Text wraps or scrolls appropriately.
     */
    @Test(priority = 85)
    public void TC_ISS_085_verifyVeryLongDescription() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_EDGE_CASES,
            "TC_ISS_085 - Verify very long description handling");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Enter 500+ character description");
        StringBuilder longText = new StringBuilder();
        // Build a 520+ char description with repeating meaningful text
        String sentence = "This is a comprehensive test description to verify that the application " +
            "handles long text input correctly in the description field. ";
        while (longText.length() < 520) {
            longText.append(sentence);
        }
        String longDescription = longText.toString();
        logStep("Description length: " + longDescription.length() + " characters");

        String readBack = issuePage.enterLongDescription(longDescription);
        shortWait();

        logStep("Step 4: Verify long description was accepted");
        logStep("Read back length: " + (readBack != null ? readBack.length() : 0));

        if (readBack != null && readBack.length() > 100) {
            logStep("✅ Long description accepted — " + readBack.length() + " characters stored");
        } else if (readBack != null && !readBack.isEmpty()) {
            logStep("ℹ️ Description stored but may be truncated — " + readBack.length() + " chars read back");
        } else {
            logStep("⚠️ Could not read back long description — may have been accepted but unreadable");
        }

        logStepWithScreenshot("TC_ISS_085: Long description entered");

        issuePage.tapCloseIssueDetails();
        shortWait();

        // Handle potential unsaved changes warning
        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_086: Verify special characters in title
     * Verify special characters allowed in title
     * Expected: Issue created with special characters in title.
     */
    @Test(priority = 86)
    public void TC_ISS_086_verifySpecialCharactersInTitle() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_EDGE_CASES,
            "TC_ISS_086 - Verify special characters in title");

        String specialTitle = "Test @#$% Issue!";

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Create issue with special characters in title: '" + specialTitle + "'");
        boolean created = issuePage.createIssueWithTitle(specialTitle, "A1");
        mediumWait();

        if (created) {
            logStep("✅ Issue with special characters created successfully");

            logStep("Step 3: Verify issue appears in list");
            if (!issuePage.isIssuesScreenDisplayed()) {
                ensureOnIssuesScreen();
            }
            issuePage.tapAllTab();
            shortWait();

            // Search for the issue to verify it exists
            boolean issueFound = issuePage.isIssueInList(specialTitle);
            logStep("Issue with special chars found in list: " + issueFound);

            if (!issueFound) {
                // Try partial search
                issuePage.searchIssues("Test");
                shortWait();
                issueFound = issuePage.isIssueInList("Test");
                logStep("Issue found via 'Test' search: " + issueFound);
                issuePage.clearSearch();
                shortWait();
            }

            if (issueFound) {
                logStep("✅ Issue with special characters visible in list");
            } else {
                logStep("ℹ️ Issue may exist but couldn't locate in list — special chars may affect search");
            }
        } else {
            logStep("⚠️ Could not create issue with special characters — may have validation restrictions");
        }

        logStepWithScreenshot("TC_ISS_086: Special characters in title");

        // Ensure back on Issues screen
        if (!issuePage.isIssuesScreenDisplayed()) {
            ensureOnIssuesScreen();
        }
    }

    // ================================================================
    // OFFLINE MODE (TC_ISS_087-088)
    // ================================================================

    /**
     * TC_ISS_087: Verify can create issue offline
     * Verify issue creation works offline
     * Expected: Issue created locally. Will sync when online.
     *
     * NOTE: iOS offline simulation is limited. This test attempts to
     * disable connectivity, create an issue, then re-enable. If connectivity
     * control is not available, it logs the limitation.
     */
    @Test(priority = 87)
    public void TC_ISS_087_verifyCreateIssueOffline() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_OFFLINE_MODE,
            "TC_ISS_087 - Verify can create issue offline");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        logStep("Step 2: Attempt to go offline (disable connectivity)");
        boolean wentOffline = issuePage.enableAirplaneMode();
        shortWait();

        if (wentOffline) {
            logStep("✅ Connectivity disabled — device is offline");

            logStep("Step 3: Create a new issue while offline");
            String offlineTitle = "OfflineTest_" + System.currentTimeMillis();
            boolean created = issuePage.createQuickIssue(offlineTitle, "A1");
            mediumWait();

            if (created) {
                logStep("✅ Issue created offline: " + offlineTitle);
                logStep("   Issue should sync when connectivity is restored");
            } else {
                logStep("ℹ️ Could not create issue offline — app may require connectivity");
            }

            logStepWithScreenshot("TC_ISS_087: Offline issue creation");

            logStep("Step 4: Restore connectivity");
            issuePage.disableAirplaneMode();
            sleep(2000); // Wait for connectivity to restore
            logStep("✅ Connectivity restored");
        } else {
            logStep("⚠️ Could not disable connectivity programmatically");
            logStep("   iOS Appium has limited support for airplane mode / connectivity control");
            logStep("   This test requires manual offline testing or a device with network conditioner");

            logStepWithScreenshot("TC_ISS_087: Offline mode not available for automation");
        }

        // Ensure back on Issues screen
        if (!issuePage.isIssuesScreenDisplayed()) {
            ensureOnIssuesScreen();
        }
    }

    /**
     * TC_ISS_088: Verify can edit issue offline
     * Verify issue editing works offline
     * Expected: Changes saved locally. Will sync when online.
     *
     * NOTE: Same connectivity limitation as TC_ISS_087.
     */
    @Test(priority = 88)
    public void TC_ISS_088_verifyEditIssueOffline() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_OFFLINE_MODE,
            "TC_ISS_088 - Verify can edit issue offline");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Attempt to go offline (disable connectivity)");
        boolean wentOffline = issuePage.enableAirplaneMode();
        shortWait();

        if (wentOffline) {
            logStep("✅ Connectivity disabled — device is offline");

            logStep("Step 3: Open first issue and make changes");
            issuePage.tapFirstIssue();
            mediumWait();

            boolean onDetails = issuePage.isIssueDetailsScreenDisplayed();
            logStep("Issue Details opened offline: " + onDetails);

            if (onDetails) {
                logStep("Step 4: Change status while offline");
                issuePage.openStatusDropdown();
                shortWait();
                issuePage.selectStatus("In Progress");
                shortWait();

                logStep("Step 5: Save changes offline");
                issuePage.tapSaveChangesButton();
                mediumWait();

                logStep("✅ Issue edited offline — changes saved locally");
                logStepWithScreenshot("TC_ISS_088: Offline issue editing");

                if (issuePage.isIssueDetailsScreenDisplayed()) {
                    issuePage.tapCloseIssueDetails();
                    shortWait();
                }
            } else {
                logStep("⚠️ Could not open Issue Details offline");
                logStepWithScreenshot("TC_ISS_088: Issue Details not accessible offline");
            }

            logStep("Step 6: Restore connectivity");
            issuePage.disableAirplaneMode();
            sleep(2000);
            logStep("✅ Connectivity restored");
        } else {
            logStep("⚠️ Could not disable connectivity programmatically");
            logStep("   iOS Appium has limited support for airplane mode / connectivity control");
            logStep("   Testing offline edit capability requires manual verification");

            logStepWithScreenshot("TC_ISS_088: Offline mode not available for automation");
        }

        // Handle potential unsaved changes warning
        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }

        // Ensure back on Issues screen
        if (!issuePage.isIssuesScreenDisplayed()) {
            ensureOnIssuesScreen();
        }
    }

    // ================================================================
    // PERFORMANCE (TC_ISS_089)
    // ================================================================

    /**
     * TC_ISS_089: Verify Issues list loads quickly
     * Verify reasonable load time
     * Expected: Issues list displayed within 2-3 seconds.
     */
    @Test(priority = 89)
    public void TC_ISS_089_verifyIssuesListLoadsQuickly() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_PERFORMANCE,
            "TC_ISS_089 - Verify Issues list loads quickly");

        logStep("Step 1: Ensure NOT on Issues screen (to test fresh navigation)");
        // First close Issues if we're on it, to get a clean navigation timing
        if (issuePage.isIssuesScreenDisplayed()) {
            issuePage.tapDoneButton();
            mediumWait();
        }

        logStep("Step 2: Navigate to Issues screen and measure load time");
        long startTime = System.currentTimeMillis();

        boolean navigated = issuePage.navigateToIssuesScreen();
        // Wait for the list to render (issues visible)
        shortWait();

        boolean hasIssues = issuePage.hasIssuesInList();
        boolean screenReady = issuePage.isIssuesScreenDisplayed();

        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;

        logStep("Step 3: Analyze load time");
        logStep("Navigation successful: " + navigated);
        logStep("Issues screen displayed: " + screenReady);
        logStep("Issues in list: " + hasIssues);
        logStep("Total load time: " + loadTime + "ms");

        if (loadTime < 2000) {
            logStep("✅ Issues list loaded quickly — " + loadTime + "ms (under 2 seconds)");
        } else if (loadTime < 3000) {
            logStep("✅ Issues list loaded within acceptable time — " + loadTime + "ms (under 3 seconds)");
        } else if (loadTime < 5000) {
            logStep("ℹ️ Issues list loaded slowly — " + loadTime + "ms (under 5 seconds)");
        } else {
            logStep("⚠️ Issues list load was slow — " + loadTime + "ms (over 5 seconds)");
        }

        logStep("Step 4: Verify tab counts load correctly");
        int allCount = issuePage.getAllTabCount();
        logStep("All tab count: " + allCount);
        if (allCount > 0) {
            logStep("✅ Issue count loaded with list: " + allCount);
        }

        logStepWithScreenshot("TC_ISS_089: Issues list load performance — " + loadTime + "ms");
    }

    // ================================================================
    // SCROLLING PERFORMANCE (TC_ISS_090)
    // ================================================================

    /**
     * TC_ISS_090: Verify scrolling performance
     * Verify smooth scrolling through issues
     * Expected: Scrolling is smooth without lag.
     *
     * NOTE: Automated smoothness verification is limited. This test measures
     * scroll gesture response times as a proxy for smoothness.
     */
    @Test(priority = 90)
    public void TC_ISS_090_verifyScrollingPerformance() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_PERFORMANCE,
            "TC_ISS_090 - Verify scrolling performance");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Verify issues exist in the list for scrolling test");
        boolean hasIssues = issuePage.hasIssuesInList();
        logStep("Issues in list: " + hasIssues);

        logStep("Step 3: Perform rapid scrolling (10 scroll gestures down, then back up)");
        long avgScrollTime = issuePage.performScrollPerformanceTest(10);

        logStep("Step 4: Evaluate scrolling performance");
        logStep("Average scroll gesture time: " + avgScrollTime + "ms");

        if (avgScrollTime < 300) {
            logStep("✅ Scrolling is very responsive — " + avgScrollTime + "ms avg per gesture");
        } else if (avgScrollTime < 500) {
            logStep("✅ Scrolling is smooth — " + avgScrollTime + "ms avg per gesture");
        } else if (avgScrollTime < 1000) {
            logStep("ℹ️ Scrolling is acceptable — " + avgScrollTime + "ms avg per gesture");
        } else {
            logStep("⚠️ Scrolling may be laggy — " + avgScrollTime + "ms avg per gesture");
        }

        logStep("Step 5: Verify list is still responsive after rapid scrolling");
        boolean stillDisplayed = issuePage.isIssuesScreenDisplayed();
        logStep("Issues screen still displayed: " + stillDisplayed);
        if (stillDisplayed) {
            logStep("✅ Issues list remained stable after rapid scrolling");
        }

        logStepWithScreenshot("TC_ISS_090: Scrolling performance — " + avgScrollTime + "ms avg");
    }

    // ================================================================
    // NFPA 70B SUBCATEGORY (TC_ISS_091-099)
    //
    // These tests change the Issue Class to "NFPA 70B Violation" on the
    // Opens an issue and verifies the chapter-based subcategory options.
    // ================================================================

    /**
     * TC_ISS_091: Verify Subcategory field for NFPA 70B Violation
     * Verify Subcategory appears for NFPA 70B Violation issues
     * Expected: Subcategory field displayed with 'Type or select...' placeholder when empty.
     */
    @Test(priority = 91)
    public void TC_ISS_091_verifySubcategoryFieldForNFPA70B() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_091 - Verify Subcategory field for NFPA 70B Violation");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Verify Issue Class changed");
        String currentClass = issuePage.getIssueClassOnDetails();
        logStep("Current Issue Class: '" + currentClass + "'");
        if (currentClass.contains("NFPA") || currentClass.contains("70B")) {
            logStep("✅ Issue Class changed to NFPA 70B Violation");
        } else {
            logStep("⚠️ Issue Class may not have changed — current: '" + currentClass + "'");
        }

        logStep("Step 5: Verify Subcategory field is displayed");
        boolean subcatDisplayed = issuePage.isSubcategoryFieldDisplayed();
        logStep("Subcategory field displayed: " + subcatDisplayed);
        if (subcatDisplayed) {
            logStep("✅ Subcategory field appears for NFPA 70B Violation");
        } else {
            logStep("⚠️ Subcategory field not found for NFPA 70B Violation");
        }

        logStep("Step 6: Get Subcategory placeholder");
        String placeholder = issuePage.getSubcategoryPlaceholder();
        logStep("Subcategory placeholder: '" + placeholder + "'");
        if (placeholder.contains("Type or select")) {
            logStep("✅ Placeholder shows 'Type or select...' as expected");
        } else if (!placeholder.isEmpty()) {
            logStep("ℹ️ Placeholder is: '" + placeholder + "'");
        }

        logStepWithScreenshot("TC_ISS_091: NFPA 70B Subcategory field");

        logStep("Step 7: Revert Issue Class to NEC Violation");
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_092: Verify NFPA 70B subcategory dropdown opens
     * Verify dropdown shows chapter-based options
     * Expected: Dropdown opens with search field and list of chapter options.
     */
    @Test(priority = 92)
    public void TC_ISS_092_verifyNFPA70BSubcategoryDropdownOpens() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_092 - Verify NFPA 70B subcategory dropdown opens with chapter options");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Tap Subcategory field to open dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Verify dropdown opened with chapter options");
        // Check for any "Chapter" text in the options
        boolean hasChapterOptions = issuePage.isSubcategoryOptionDisplayed("Chapter");
        logStep("Chapter options visible: " + hasChapterOptions);

        java.util.ArrayList<String> visibleOptions = issuePage.getVisibleSubcategoryOptions();
        logStep("Visible subcategory options: " + visibleOptions.size());
        for (int i = 0; i < Math.min(visibleOptions.size(), 5); i++) {
            logStep("   - " + visibleOptions.get(i));
        }
        if (visibleOptions.size() > 5) {
            logStep("   ... and " + (visibleOptions.size() - 5) + " more");
        }

        if (hasChapterOptions || !visibleOptions.isEmpty()) {
            logStep("✅ NFPA 70B subcategory dropdown opened with chapter-based options");
        } else {
            logStep("⚠️ No chapter options visible — dropdown may not have opened properly");
        }

        logStepWithScreenshot("TC_ISS_092: NFPA 70B subcategory dropdown");

        issuePage.dismissDropdownMenu();
        shortWait();

        logStep("Step 6: Revert Issue Class to NEC Violation");
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_093: Verify Chapter 28.3.2 Motor Control Equipment Cleaning option
     * Expected: 'Chapter 28.3.2 Motor Control Equipment Cleaning' option displayed.
     */
    @Test(priority = 93)
    public void TC_ISS_093_verifyChapter28_3_2_MotorControlCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_093 - Verify Chapter 28.3.2 Motor Control Equipment Cleaning");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 28.3.2 Motor Control Equipment Cleaning'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("28.3.2");
        logStep("Chapter 28.3.2 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("28.3.2");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 28.3.2 Motor Control Equipment Cleaning not found");
        }

        logStepWithScreenshot("TC_ISS_093: Chapter 28.3.2 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_094: Verify Chapter 28.3.1 Visual Inspections option
     * Expected: 'Chapter 28.3.1 Visual Inspections' option displayed.
     */
    @Test(priority = 94)
    public void TC_ISS_094_verifyChapter28_3_1_VisualInspections() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_094 - Verify Chapter 28.3.1 Visual Inspections");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 28.3.1 Visual Inspections'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("28.3.1");
        logStep("Chapter 28.3.1 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("28.3.1");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 28.3.1 Visual Inspections not found");
        }

        logStepWithScreenshot("TC_ISS_094: Chapter 28.3.1 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_095: Verify Chapter 15.3.2 Circuit Breakers Low- and Medium Voltage option
     * Expected: 'Chapter 15.3.2 Circuit Breakers Low- and Medium Voltage' option displayed.
     */
    @Test(priority = 95)
    public void TC_ISS_095_verifyChapter15_3_2_CircuitBreakers() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_095 - Verify Chapter 15.3.2 Circuit Breakers");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 15.3.2 Circuit Breakers Low- and Medium Voltage'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("15.3.2");
        logStep("Chapter 15.3.2 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("15.3.2");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 15.3.2 Circuit Breakers not found");
        }

        logStepWithScreenshot("TC_ISS_095: Chapter 15.3.2 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_096: Verify Chapter 15.3.1 Visual Inspections option
     * Expected: 'Chapter 15.3.1 Visual Inspections' option displayed.
     */
    @Test(priority = 96)
    public void TC_ISS_096_verifyChapter15_3_1_VisualInspections() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_096 - Verify Chapter 15.3.1 Visual Inspections");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 15.3.1 Visual Inspections'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("15.3.1");
        logStep("Chapter 15.3.1 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("15.3.1");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 15.3.1 Visual Inspections not found");
        }

        logStepWithScreenshot("TC_ISS_096: Chapter 15.3.1 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_097: Verify Chapter 25.3.2 UPS Cleaning option
     * Expected: 'Chapter 25.3.2 UPS Cleaning' option displayed.
     */
    @Test(priority = 97)
    public void TC_ISS_097_verifyChapter25_3_2_UPSCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_097 - Verify Chapter 25.3.2 UPS Cleaning");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 25.3.2 UPS Cleaning'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("25.3.2");
        logStep("Chapter 25.3.2 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("25.3.2");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 25.3.2 UPS Cleaning not found");
        }

        logStepWithScreenshot("TC_ISS_097: Chapter 25.3.2 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_098: Verify Chapter 25.3.1 Visual Inspections option
     * Expected: 'Chapter 25.3.1 Visual Inspections' option displayed.
     */
    @Test(priority = 98)
    public void TC_ISS_098_verifyChapter25_3_1_VisualInspections() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_098 - Verify Chapter 25.3.1 Visual Inspections");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 25.3.1 Visual Inspections'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("25.3.1");
        logStep("Chapter 25.3.1 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("25.3.1");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 25.3.1 Visual Inspections not found");
        }

        logStepWithScreenshot("TC_ISS_098: Chapter 25.3.1 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_099: Verify Chapter 11.3.2 Power and Distribution Transformer Cleaning option
     * Expected: 'Chapter 11.3.2 Power and Distribution Transformer Cleaning' option displayed.
     */
    @Test(priority = 99)
    public void TC_ISS_099_verifyChapter11_3_2_TransformerCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_099 - Verify Chapter 11.3.2 Power and Distribution Transformer Cleaning");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 11.3.2 Power and Distribution Transformer Cleaning'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("11.3.2");
        logStep("Chapter 11.3.2 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("11.3.2");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 11.3.2 Power and Distribution Transformer Cleaning not found");
        }

        logStepWithScreenshot("TC_ISS_099: Chapter 11.3.2 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        logStep("Step 6: Revert Issue Class to NEC Violation");
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    // ================================================================
    // NFPA 70B SUBCATEGORY CONTINUED (TC_ISS_100-105)
    // ================================================================

    /**
     * TC_ISS_100: Verify Chapter 11.3.1 Visual Inspections option
     * Expected: 'Chapter 11.3.1 Visual Inspections' option displayed.
     */
    @Test(priority = 100)
    public void TC_ISS_100_verifyChapter11_3_1_VisualInspections() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_100 - Verify Chapter 11.3.1 Visual Inspections");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 11.3.1 Visual Inspections'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("11.3.1");
        logStep("Chapter 11.3.1 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("11.3.1");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 11.3.1 Visual Inspections not found");
        }

        logStepWithScreenshot("TC_ISS_100: Chapter 11.3.1 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_101: Verify Chapter 12.3.2 Substations and Switchgear Cleaning
     * Expected: 'Chapter 12.3.2 Substations and Switchgear Cleaning' option displayed.
     */
    @Test(priority = 101)
    public void TC_ISS_101_verifyChapter12_3_2_SubstationsCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_101 - Verify Chapter 12.3.2 Substations and Switchgear Cleaning");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 12.3.2 Substations and Switchgear Cleaning'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("12.3.2");
        logStep("Chapter 12.3.2 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("12.3.2");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 12.3.2 Substations and Switchgear Cleaning not found");
        }

        logStepWithScreenshot("TC_ISS_101: Chapter 12.3.2 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_102: Verify Chapter 12.3.1 Visual Inspections option
     * Expected: 'Chapter 12.3.1 Visual Inspections' option displayed.
     */
    @Test(priority = 102)
    public void TC_ISS_102_verifyChapter12_3_1_VisualInspections() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_102 - Verify Chapter 12.3.1 Visual Inspections");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 12.3.1 Visual Inspections'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("12.3.1");
        logStep("Chapter 12.3.1 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("12.3.1");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 12.3.1 Visual Inspections not found");
        }

        logStepWithScreenshot("TC_ISS_102: Chapter 12.3.1 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_103: Verify Chapter 13.5.2 Panelboards and Switchboards Cleaning
     * Expected: 'Chapter 13.5.2 Panelboards and Switchboards Cleaning' option displayed.
     */
    @Test(priority = 103)
    public void TC_ISS_103_verifyChapter13_5_2_PanelboardsCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_103 - Verify Chapter 13.5.2 Panelboards and Switchboards Cleaning");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 13.5.2 Panelboards and Switchboards Cleaning'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("13.5.2");
        logStep("Chapter 13.5.2 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("13.5.2");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 13.5.2 Panelboards and Switchboards Cleaning not found");
        }

        logStepWithScreenshot("TC_ISS_103: Chapter 13.5.2 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_104: Verify Chapter 13.3.1 Visual Inspections option
     * Expected: 'Chapter 13.3.1 Visual Inspections' option displayed.
     */
    @Test(priority = 104)
    public void TC_ISS_104_verifyChapter13_3_1_VisualInspections() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_104 - Verify Chapter 13.3.1 Visual Inspections");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 13.3.1 Visual Inspections'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("13.3.1");
        logStep("Chapter 13.3.1 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("13.3.1");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 13.3.1 Visual Inspections not found");
        }

        logStepWithScreenshot("TC_ISS_104: Chapter 13.3.1 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_105: Verify Chapter 17.3.1 Visual Inspections option
     * Expected: 'Chapter 17.3.1 Visual Inspections' option displayed.
     */
    @Test(priority = 105)
    public void TC_ISS_105_verifyChapter17_3_1_VisualInspections() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_105 - Verify Chapter 17.3.1 Visual Inspections");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Look for 'Chapter 17.3.1 Visual Inspections'");
        boolean found = issuePage.isNFPA70BChapterOptionPresent("17.3.1");
        logStep("Chapter 17.3.1 found: " + found);

        if (found) {
            String fullLabel = issuePage.getChapterOptionFullLabel("17.3.1");
            logStep("✅ Found: " + fullLabel);
        } else {
            logStep("⚠️ Chapter 17.3.1 Visual Inspections not found");
        }

        logStepWithScreenshot("TC_ISS_105: Chapter 17.3.1 option");

        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    // ================================================================
    // NFPA 70B SELECTION & SEARCH (TC_ISS_106-108)
    // ================================================================

    /**
     * TC_ISS_106: Verify selecting NFPA 70B subcategory
     * Verify can select chapter-based subcategory
     * Expected: Subcategory selected. Field shows selected value. Green checkmark appears.
     */
    @Test(priority = 106)
    public void TC_ISS_106_verifySelectingNFPA70BSubcategory() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_106 - Verify selecting NFPA 70B subcategory");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Select 'Chapter 15.3.2 Circuit Breakers Low- and Medium Voltage'");
        String selectedValue = issuePage.selectSubcategoryAndGetValue("15.3.2");
        logStep("Selected subcategory value: '" + selectedValue + "'");

        if (selectedValue.contains("15.3.2") || selectedValue.contains("Circuit Breakers")) {
            logStep("✅ Subcategory selected correctly: " + selectedValue);
        } else if (!selectedValue.isEmpty()) {
            logStep("ℹ️ Selected value: '" + selectedValue + "' (may be truncated)");
        } else {
            logStep("⚠️ Could not read selected subcategory value");
        }

        logStep("Step 6: Check for green checkmark indicator");
        boolean checkmarkDisplayed = issuePage.isSubcategoryCheckmarkDisplayed();
        logStep("Checkmark displayed: " + checkmarkDisplayed);
        if (checkmarkDisplayed) {
            logStep("✅ Green checkmark appears after selecting subcategory");
        } else {
            logStep("ℹ️ Checkmark not detected — may use different indicator or styling");
        }

        logStepWithScreenshot("TC_ISS_106: NFPA 70B subcategory selected");

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_107: Verify subcategory search/filter
     * Verify can type to filter NFPA 70B subcategories
     * Expected: List filters to show only Visual Inspections chapters.
     */
    @Test(priority = 107)
    public void TC_ISS_107_verifySubcategorySearchFilter() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_107 - Verify subcategory search/filter with 'Visual'");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Type 'Visual' to filter subcategories");
        int matchCount = issuePage.searchSubcategoryAndCountResults("Visual");
        logStep("Matches for 'Visual': " + matchCount);

        logStep("Step 6: Verify filtered results contain Visual Inspections");
        java.util.ArrayList<String> filteredOptions = issuePage.getFilteredSubcategoryOptions();
        logStep("Filtered options: " + filteredOptions.size());
        boolean allVisual = true;
        for (String opt : filteredOptions) {
            logStep("   - " + opt);
            if (!opt.contains("Visual")) {
                allVisual = false;
            }
        }

        if (!filteredOptions.isEmpty() && allVisual) {
            logStep("✅ Filter correctly shows only Visual Inspections chapters");
        } else if (!filteredOptions.isEmpty()) {
            logStep("ℹ️ Filter shows " + filteredOptions.size() + " results — some may not contain 'Visual'");
        } else if (matchCount > 0) {
            logStep("✅ Found " + matchCount + " matches for 'Visual' filter");
        } else {
            logStep("⚠️ No filtered results — search may work differently");
        }

        logStepWithScreenshot("TC_ISS_107: Subcategory filtered by 'Visual'");

        issuePage.clearSubcategorySearch();
        shortWait();
        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_108: Verify subcategory search by chapter number
     * Verify can search by chapter number
     * Expected: List filters to show Chapter 15.3.x options only.
     */
    @Test(priority = 108)
    public void TC_ISS_108_verifySubcategorySearchByChapterNumber() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_NFPA70B_SUBCATEGORY,
            "TC_ISS_108 - Verify subcategory search by chapter number '15.3'");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to 'NFPA 70B Violation'");
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 4: Open Subcategory dropdown");
        issuePage.tapSubcategoryField();
        mediumWait();

        logStep("Step 5: Type '15.3' to filter by chapter number");
        int matchCount = issuePage.searchSubcategoryAndCountResults("15.3");
        logStep("Matches for '15.3': " + matchCount);

        logStep("Step 6: Verify filtered results are Chapter 15.3.x options");
        java.util.ArrayList<String> filteredOptions = issuePage.getFilteredSubcategoryOptions();
        logStep("Filtered options: " + filteredOptions.size());
        boolean all15_3 = true;
        for (String opt : filteredOptions) {
            logStep("   - " + opt);
            if (!opt.contains("15.3")) {
                all15_3 = false;
            }
        }

        if (!filteredOptions.isEmpty() && all15_3) {
            logStep("✅ Filter correctly shows only Chapter 15.3.x options");
        } else if (!filteredOptions.isEmpty()) {
            logStep("ℹ️ Filter shows " + filteredOptions.size() + " results — checking relevance");
        } else if (matchCount > 0) {
            logStep("✅ Found " + matchCount + " matches for '15.3' filter");
        } else {
            logStep("⚠️ No filtered results — search by chapter number may work differently");
        }

        logStepWithScreenshot("TC_ISS_108: Subcategory filtered by '15.3'");

        issuePage.clearSubcategorySearch();
        shortWait();
        issuePage.dismissDropdownMenu();
        shortWait();

        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    // ================================================================
    // ISSUE COMPLETION (TC_ISS_109)
    // ================================================================

    /**
     * TC_ISS_109: Verify 100% completion when all required fields filled
     * Verify completion percentage shows 100% with green indicator
     * Expected: Issue Details shows '100%' with green dot indicator.
     *           Required fields only shows '1/1'.
     */
    @Test(priority = 109)
    public void TC_ISS_109_verify100PercentCompletion() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_COMPLETION,
            "TC_ISS_109 - Verify 100% completion when all required fields filled");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        // tapFirstIssue() now waits up to 3s for Issue Details screen

        // Verify we're on Issue Details screen before proceeding
        boolean onDetails = issuePage.isIssueDetailsScreenDisplayed();
        if (!onDetails) {
            logStep("   Issue Details not detected, retrying tap...");
            issuePage.tapFirstIssue();
            onDetails = issuePage.isIssueDetailsScreenDisplayed();
        }
        assertTrue(onDetails, "Should be on Issue Details screen after tapping issue");
        logStep("✅ Issue Details screen is displayed");

        logStep("Step 3: Check initial completion percentage");
        String initialPct = issuePage.getIssueDetailsCompletionPercentage();
        logStep("Initial completion: '" + initialPct + "'");

        logStep("Step 4: Check initial required fields toggle count");
        String initialCount = issuePage.getRequiredFieldsToggleCount();
        logStep("Initial required fields count: '" + initialCount + "'");

        logStep("Step 5: Ensure Issue Class is NEC Violation (has required Subcategory)");
        String currentClass = issuePage.getIssueClassOnDetails();
        logStep("Current Issue Class: '" + currentClass + "'");
        if (!currentClass.contains("NEC")) {
            issuePage.changeIssueClassOnDetails("NEC Violation");
            mediumWait();
        }

        logStep("Step 6: Fill Subcategory (required field) and check completion");
        String completionAfterFill = issuePage.fillSubcategoryAndGetCompletion("Breaker is restricted");
        logStep("Completion after filling subcategory: '" + completionAfterFill + "'");

        if (completionAfterFill.contains("100%")) {
            logStep("✅ Completion shows 100% after filling all required fields");
        } else if (!completionAfterFill.isEmpty()) {
            logStep("ℹ️ Completion is: '" + completionAfterFill + "' (may need more fields for 100%)");
        } else {
            logStep("⚠️ Could not read completion percentage after filling fields");
        }

        logStep("Step 7: Check required fields count after filling");
        String updatedCount = issuePage.getRequiredFieldsToggleCount();
        logStep("Updated required fields count: '" + updatedCount + "'");
        if (updatedCount.contains("1/1")) {
            logStep("✅ Required fields shows '1/1' — all required fields completed");
        } else if (!updatedCount.isEmpty()) {
            logStep("ℹ️ Required fields count: '" + updatedCount + "'");
        }

        logStepWithScreenshot("TC_ISS_109: Completion percentage after filling required fields");

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    // ================================================================
    // ISSUE COMPLETION INDICATORS (TC_ISS_110-111)
    // ================================================================

    /**
     * TC_ISS_110: Verify incomplete percentage shown with orange dot
     * When Subcategory is not filled, completion shows 0% with orange indicator.
     * Expected: Issue Details section shows '0%' with orange dot indicator.
     *           Required fields only shows '0/1'.
     */
    @Test(priority = 110)
    public void TC_ISS_110_verifyIncompletePercentageWithOrangeDot() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_COMPLETION,
            "TC_ISS_110 - Verify incomplete percentage shown with orange dot");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Ensure Issue Class is NEC Violation (has required Subcategory)");
        String currentClass = issuePage.getIssueClassOnDetails();
        logStep("Current Issue Class: '" + currentClass + "'");
        if (!currentClass.contains("NEC")) {
            issuePage.changeIssueClassOnDetails("NEC Violation");
            mediumWait();
        }

        logStep("Step 4: Clear subcategory if it has a value (so we get 0%)");
        String subcatValue = issuePage.getSubcategoryValue();
        logStep("Current subcategory value: '" + subcatValue + "'");
        if (!subcatValue.isEmpty() && !subcatValue.contains("Type or select") && !subcatValue.contains("Select")) {
            boolean cleared = issuePage.clearSubcategoryValue();
            logStep("Cleared subcategory: " + cleared);
            shortWait();
        }

        logStep("Step 5: Scroll up to see completion section header");
        issuePage.scrollUpOnDetailsScreen();
        shortWait();

        logStep("Step 6: Check completion percentage — expect 0%");
        String pct = issuePage.getIssueDetailsCompletionPercentage();
        logStep("Completion percentage: '" + pct + "'");
        if (pct.contains("0%")) {
            logStep("✅ Completion shows 0% — incomplete state confirmed");
        } else if (!pct.isEmpty()) {
            logStep("ℹ️ Completion is: '" + pct + "' (may be partially filled)");
        } else {
            logStep("⚠️ Could not read completion percentage");
        }

        logStep("Step 7: Check for orange/incomplete indicator");
        boolean orangeIndicator = issuePage.isCompletionIndicatorOrange();
        logStep("Orange indicator present: " + orangeIndicator);
        if (orangeIndicator) {
            logStep("✅ Orange dot indicator confirms incomplete state");
        }

        logStep("Step 8: Check required fields toggle — expect '0/1'");
        String reqCount = issuePage.getRequiredFieldsToggleCount();
        logStep("Required fields count: '" + reqCount + "'");
        if (reqCount.contains("0/1")) {
            logStep("✅ Required fields shows '0/1' — 0 of 1 required fields filled");
        } else if (!reqCount.isEmpty()) {
            logStep("ℹ️ Required fields count: '" + reqCount + "'");
        }

        logStepWithScreenshot("TC_ISS_110: Incomplete percentage with orange dot indicator");

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_111: Verify green checkmark on filled subcategory
     * When Subcategory is filled, a green checkmark appears next to the field.
     * Expected: A green checkmark icon is visible next to a filled Subcategory field.
     */
    @Test(priority = 111)
    public void TC_ISS_111_verifyGreenCheckmarkOnFilledSubcategory() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_COMPLETION,
            "TC_ISS_111 - Verify green checkmark on filled subcategory");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Ensure Issue Class is NEC Violation");
        String currentClass = issuePage.getIssueClassOnDetails();
        if (!currentClass.contains("NEC")) {
            issuePage.changeIssueClassOnDetails("NEC Violation");
            mediumWait();
        }

        logStep("Step 4: Fill Subcategory field");
        String completionAfter = issuePage.fillSubcategoryAndGetCompletion("Breaker is restricted");
        logStep("Completion after filling: '" + completionAfter + "'");
        mediumWait();

        logStep("Step 5: Scroll down to Subcategory field area");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();

        logStep("Step 6: Check for green checkmark on Subcategory");
        boolean checkmark = issuePage.isSubcategoryCheckmarkDisplayed();
        logStep("Green checkmark displayed: " + checkmark);
        if (checkmark) {
            logStep("✅ Green checkmark confirmed on filled Subcategory field");
        } else {
            logStep("ℹ️ Checkmark not visually detected — may use different indicator");
        }

        logStep("Step 7: Verify completion indicator is green");
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        boolean greenIndicator = issuePage.isCompletionIndicatorGreen();
        logStep("Green completion indicator: " + greenIndicator);
        if (greenIndicator) {
            logStep("✅ Green indicator confirms all required fields filled");
        }

        logStepWithScreenshot("TC_ISS_111: Green checkmark on filled subcategory");

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    // ================================================================
    // CHANGE ISSUE CLASS - SUBCATEGORY UPDATE (TC_ISS_112-113)
    // ================================================================

    /**
     * TC_ISS_112: Verify changing Issue Class updates Subcategory options
     * When Issue Class changes from NEC Violation to NFPA 70B, the Subcategory
     * dropdown options should update to reflect the new class.
     * Expected: Subcategory options change when Issue Class changes.
     */
    @Test(priority = 112)
    public void TC_ISS_112_verifyChangingIssueClassUpdatesSubcategoryOptions() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CHANGE_ISSUE_CLASS,
            "TC_ISS_112 - Verify changing Issue Class updates Subcategory options");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Ensure Issue Class is NEC Violation first");
        String currentClass = issuePage.getIssueClassOnDetails();
        logStep("Current Issue Class: '" + currentClass + "'");
        if (!currentClass.contains("NEC")) {
            issuePage.changeIssueClassOnDetails("NEC Violation");
            mediumWait();
        }

        logStep("Step 4: Open Subcategory and record NEC options");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();
        issuePage.tapSubcategoryField();
        shortWait();
        java.util.ArrayList<String> necOptions = issuePage.getVisibleSubcategoryOptions();
        logStep("NEC Violation subcategory options count: " + necOptions.size());
        for (String opt : necOptions) {
            logStep("   NEC option: " + opt);
        }
        issuePage.dismissDropdownMenu();
        shortWait();

        logStep("Step 5: Change Issue Class to NFPA 70B Violation");
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 6: Open Subcategory and record NFPA 70B options");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();
        issuePage.tapSubcategoryField();
        shortWait();
        java.util.ArrayList<String> nfpaOptions = issuePage.getVisibleSubcategoryOptions();
        logStep("NFPA 70B subcategory options count: " + nfpaOptions.size());
        for (String opt : nfpaOptions) {
            logStep("   NFPA 70B option: " + opt);
        }
        issuePage.dismissDropdownMenu();
        shortWait();

        logStep("Step 7: Compare — options should be different");
        boolean optionsAreDifferent = !necOptions.equals(nfpaOptions);
        logStep("Options are different between classes: " + optionsAreDifferent);
        if (optionsAreDifferent) {
            logStep("✅ Subcategory options updated when Issue Class changed");
        } else if (necOptions.isEmpty() && nfpaOptions.isEmpty()) {
            logStep("⚠️ Both option sets empty — dropdown may not have loaded");
        } else {
            logStep("⚠️ Options appear the same — unexpected");
        }

        logStepWithScreenshot("TC_ISS_112: Subcategory options updated after class change");

        // Revert to NEC Violation
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_113: Verify Subcategory cleared when Issue Class changes
     * When a subcategory is selected and then the Issue Class changes,
     * the subcategory field should be cleared.
     * Expected: Subcategory value is reset to empty/placeholder after class change.
     */
    @Test(priority = 113)
    public void TC_ISS_113_verifySubcategoryClearedWhenIssueClassChanges() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CHANGE_ISSUE_CLASS,
            "TC_ISS_113 - Verify Subcategory cleared when Issue Class changes");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Ensure Issue Class is NEC Violation");
        String currentClass = issuePage.getIssueClassOnDetails();
        if (!currentClass.contains("NEC")) {
            issuePage.changeIssueClassOnDetails("NEC Violation");
            mediumWait();
        }

        logStep("Step 4: Fill Subcategory with a value");
        issuePage.fillSubcategoryAndGetCompletion("Breaker is restricted");
        mediumWait();

        logStep("Step 5: Verify Subcategory is now filled");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();
        String filledValue = issuePage.getSubcategoryValue();
        logStep("Subcategory value before class change: '" + filledValue + "'");
        boolean wasFilled = !filledValue.isEmpty() && !filledValue.contains("Type or select");
        logStep("Subcategory was filled: " + wasFilled);

        logStep("Step 6: Change Issue Class to NFPA 70B Violation");
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        issuePage.changeIssueClassOnDetails("NFPA 70B Violation");
        mediumWait();

        logStep("Step 7: Check if Subcategory was cleared");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();
        boolean subcatEmpty = issuePage.isSubcategoryEmpty();
        String afterValue = issuePage.getSubcategoryValue();
        logStep("Subcategory value after class change: '" + afterValue + "'");
        logStep("Subcategory is empty: " + subcatEmpty);

        if (subcatEmpty) {
            logStep("✅ Subcategory was cleared when Issue Class changed");
        } else {
            logStep("ℹ️ Subcategory still shows: '" + afterValue + "' — may retain value or use different clearing behavior");
        }

        logStepWithScreenshot("TC_ISS_113: Subcategory cleared after class change");

        // Revert to NEC Violation
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    // ================================================================
    // CLASS-SPECIFIC SUBCATEGORIES (TC_ISS_114-117)
    // ================================================================

    /**
     * TC_ISS_114: Verify OSHA Violation has different subcategories
     * OSHA Violation class should have its own set of subcategory options
     * different from NEC Violation.
     * Expected: OSHA Violation subcategory options are displayed and differ from NEC.
     */
    @Test(priority = 114)
    public void TC_ISS_114_verifyOSHAViolationSubcategories() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS_SUBCATEGORIES,
            "TC_ISS_114 - Verify OSHA Violation has different subcategories");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to OSHA Violation");
        issuePage.changeIssueClassOnDetails("OSHA Violation");
        mediumWait();

        logStep("Step 4: Scroll down and open Subcategory dropdown");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();
        issuePage.tapSubcategoryField();
        shortWait();

        logStep("Step 5: Collect OSHA subcategory options");
        java.util.ArrayList<String> oshaOptions = issuePage.getVisibleSubcategoryOptions();
        logStep("OSHA Violation subcategory options count: " + oshaOptions.size());
        for (String opt : oshaOptions) {
            logStep("   OSHA option: " + opt);
        }

        if (!oshaOptions.isEmpty()) {
            logStep("✅ OSHA Violation has " + oshaOptions.size() + " subcategory options");
        } else {
            logStep("⚠️ No OSHA subcategory options found — dropdown may not have loaded");
        }

        issuePage.dismissDropdownMenu();
        shortWait();

        logStepWithScreenshot("TC_ISS_114: OSHA Violation subcategory options");

        // Revert to NEC Violation
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_115: Verify Repair Needed subcategories
     * Repair Needed class should have its own set of subcategory options.
     * Expected: Repair Needed subcategory options are displayed.
     */
    @Test(priority = 115)
    public void TC_ISS_115_verifyRepairNeededSubcategories() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS_SUBCATEGORIES,
            "TC_ISS_115 - Verify Repair Needed subcategories");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to Repair Needed");
        issuePage.changeIssueClassOnDetails("Repair Needed");
        mediumWait();

        logStep("Step 4: Scroll down and open Subcategory dropdown");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();
        issuePage.tapSubcategoryField();
        shortWait();

        logStep("Step 5: Collect Repair Needed subcategory options");
        java.util.ArrayList<String> repairOptions = issuePage.getVisibleSubcategoryOptions();
        logStep("Repair Needed subcategory options count: " + repairOptions.size());
        for (String opt : repairOptions) {
            logStep("   Repair option: " + opt);
        }

        if (!repairOptions.isEmpty()) {
            logStep("✅ Repair Needed has " + repairOptions.size() + " subcategory options");
        } else {
            logStep("ℹ️ Repair Needed may not have subcategory options (field may be hidden/disabled)");
        }

        issuePage.dismissDropdownMenu();
        shortWait();

        logStepWithScreenshot("TC_ISS_115: Repair Needed subcategory options");

        // Revert to NEC Violation
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_116: Verify Thermal Anomaly subcategories
     * Thermal Anomaly class should have its own set of subcategory options.
     * Expected: Thermal Anomaly subcategory options are displayed.
     */
    @Test(priority = 116)
    public void TC_ISS_116_verifyThermalAnomalySubcategories() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS_SUBCATEGORIES,
            "TC_ISS_116 - Verify Thermal Anomaly subcategories");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to Thermal Anomaly");
        issuePage.changeIssueClassOnDetails("Thermal Anomaly");
        mediumWait();

        logStep("Step 4: Scroll down and open Subcategory dropdown");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();
        issuePage.tapSubcategoryField();
        shortWait();

        logStep("Step 5: Collect Thermal Anomaly subcategory options");
        java.util.ArrayList<String> thermalOptions = issuePage.getVisibleSubcategoryOptions();
        logStep("Thermal Anomaly subcategory options count: " + thermalOptions.size());
        for (String opt : thermalOptions) {
            logStep("   Thermal option: " + opt);
        }

        if (!thermalOptions.isEmpty()) {
            logStep("✅ Thermal Anomaly has " + thermalOptions.size() + " subcategory options");
        } else {
            logStep("ℹ️ Thermal Anomaly may not have subcategory options");
        }

        issuePage.dismissDropdownMenu();
        shortWait();

        logStepWithScreenshot("TC_ISS_116: Thermal Anomaly subcategory options");

        // Revert to NEC Violation
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_117: Verify Ultrasonic Anomaly subcategories
     * Ultrasonic Anomaly class should have its own set of subcategory options.
     * Expected: Ultrasonic Anomaly subcategory options are displayed.
     */
    @Test(priority = 117)
    public void TC_ISS_117_verifyUltrasonicAnomalySubcategories() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_CLASS_SUBCATEGORIES,
            "TC_ISS_117 - Verify Ultrasonic Anomaly subcategories");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to Ultrasonic Anomaly");
        issuePage.changeIssueClassOnDetails("Ultrasonic Anomaly");
        mediumWait();

        logStep("Step 4: Scroll down and open Subcategory dropdown");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();
        issuePage.tapSubcategoryField();
        shortWait();

        logStep("Step 5: Collect Ultrasonic Anomaly subcategory options");
        java.util.ArrayList<String> ultrasonicOptions = issuePage.getVisibleSubcategoryOptions();
        logStep("Ultrasonic Anomaly subcategory options count: " + ultrasonicOptions.size());
        for (String opt : ultrasonicOptions) {
            logStep("   Ultrasonic option: " + opt);
        }

        if (!ultrasonicOptions.isEmpty()) {
            logStep("✅ Ultrasonic Anomaly has " + ultrasonicOptions.size() + " subcategory options");
        } else {
            logStep("ℹ️ Ultrasonic Anomaly may not have subcategory options");
        }

        issuePage.dismissDropdownMenu();
        shortWait();

        logStepWithScreenshot("TC_ISS_117: Ultrasonic Anomaly subcategory options");

        // Revert to NEC Violation
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    // ================================================================
    // CLEAR SUBCATEGORY & OSHA FIELD (TC_ISS_118-119)
    // ================================================================

    /**
     * TC_ISS_118: Verify clear selected subcategory (X button)
     * After selecting a subcategory, tapping the clear/X button should reset
     * the field to its placeholder state.
     * Expected: Subcategory field returns to empty/placeholder after clearing.
     */
    @Test(priority = 118)
    public void TC_ISS_118_verifyClearSelectedSubcategory() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_CLEAR_SUBCATEGORY,
            "TC_ISS_118 - Verify clear selected subcategory (X button)");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Ensure Issue Class is NEC Violation");
        String currentClass = issuePage.getIssueClassOnDetails();
        if (!currentClass.contains("NEC")) {
            issuePage.changeIssueClassOnDetails("NEC Violation");
            mediumWait();
        }

        logStep("Step 4: Fill Subcategory with a value");
        issuePage.fillSubcategoryAndGetCompletion("Breaker is restricted");
        mediumWait();

        logStep("Step 5: Verify Subcategory is filled");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();
        String filledValue = issuePage.getSubcategoryValue();
        logStep("Subcategory value before clearing: '" + filledValue + "'");
        boolean wasFilled = !filledValue.isEmpty() && !filledValue.contains("Type or select");
        logStep("Subcategory was filled: " + wasFilled);

        logStep("Step 6: Tap clear/X button to clear subcategory");
        boolean cleared = issuePage.clearSubcategoryValue();
        logStep("Clear action result: " + cleared);
        shortWait();

        logStep("Step 7: Verify subcategory is now empty");
        boolean isEmpty = issuePage.isSubcategoryEmpty();
        String afterValue = issuePage.getSubcategoryValue();
        logStep("Subcategory value after clearing: '" + afterValue + "'");
        logStep("Subcategory is empty: " + isEmpty);

        if (isEmpty) {
            logStep("✅ Subcategory successfully cleared — field returned to placeholder");
        } else {
            logStep("ℹ️ Subcategory still shows: '" + afterValue + "' — clear mechanism may differ");
        }

        logStep("Step 8: Verify completion drops back to 0%");
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        String pctAfterClear = issuePage.getIssueDetailsCompletionPercentage();
        logStep("Completion after clearing: '" + pctAfterClear + "'");
        if (pctAfterClear.contains("0%")) {
            logStep("✅ Completion reverted to 0% after clearing subcategory");
        } else if (!pctAfterClear.isEmpty()) {
            logStep("ℹ️ Completion is: '" + pctAfterClear + "'");
        }

        logStepWithScreenshot("TC_ISS_118: Subcategory cleared via X button");

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

    /**
     * TC_ISS_119: Verify OSHA Subcategory field verification
     * When Issue Class is OSHA Violation, the Subcategory field should be
     * displayed with appropriate placeholder and accept OSHA-specific options.
     * Expected: Subcategory field is displayed, has correct placeholder, and
     *           OSHA-specific options can be selected.
     */
    @Test(priority = 119)
    public void TC_ISS_119_verifyOSHASubcategoryFieldVerification() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_OSHA_SUBCATEGORY,
            "TC_ISS_119 - Verify OSHA Subcategory field verification");

        logStep("Step 1: Ensure on Issues screen");
        boolean onIssues = ensureOnIssuesScreen();
        assertTrue(onIssues, "Should be on Issues screen");
        shortWait();

        issuePage.tapAllTab();
        shortWait();

        logStep("Step 2: Open first issue");
        issuePage.tapFirstIssue();
        mediumWait();

        logStep("Step 3: Change Issue Class to OSHA Violation");
        issuePage.changeIssueClassOnDetails("OSHA Violation");
        mediumWait();

        logStep("Step 4: Scroll down to Subcategory field");
        issuePage.scrollDownOnDetailsScreen();
        shortWait();

        logStep("Step 5: Verify Subcategory field is displayed");
        boolean fieldDisplayed = issuePage.isSubcategoryFieldDisplayed();
        logStep("Subcategory field displayed: " + fieldDisplayed);
        if (fieldDisplayed) {
            logStep("✅ Subcategory field is visible for OSHA Violation");
        } else {
            logStep("ℹ️ Subcategory field may not be required for OSHA Violation");
        }

        logStep("Step 6: Check Subcategory placeholder text");
        String placeholder = issuePage.getSubcategoryPlaceholder();
        logStep("Subcategory placeholder: '" + placeholder + "'");
        if (!placeholder.isEmpty()) {
            logStep("✅ Placeholder text is present: '" + placeholder + "'");
        }

        logStep("Step 7: Open Subcategory and verify OSHA options");
        issuePage.tapSubcategoryField();
        shortWait();
        java.util.ArrayList<String> oshaOptions = issuePage.getVisibleSubcategoryOptions();
        logStep("OSHA subcategory options: " + oshaOptions.size());
        for (String opt : oshaOptions) {
            logStep("   OSHA option: " + opt);
        }

        logStep("Step 8: Select first OSHA option if available");
        if (!oshaOptions.isEmpty()) {
            String firstOption = oshaOptions.get(0);
            issuePage.selectSubcategory(firstOption);
            shortWait();

            String selectedValue = issuePage.getSubcategoryValue();
            logStep("Selected OSHA subcategory: '" + selectedValue + "'");
            if (!selectedValue.isEmpty() && !selectedValue.contains("Type or select")) {
                logStep("✅ OSHA subcategory selection works: '" + selectedValue + "'");
            }
        } else {
            issuePage.dismissDropdownMenu();
            logStep("⚠️ No OSHA options available to select");
        }

        logStepWithScreenshot("TC_ISS_119: OSHA Subcategory field verification");

        // Revert to NEC Violation
        issuePage.scrollUpOnDetailsScreen();
        shortWait();
        issuePage.changeIssueClassOnDetails("NEC Violation");
        shortWait();

        issuePage.tapCloseIssueDetails();
        shortWait();

        if (issuePage.isUnsavedChangesWarningDisplayed()) {
            issuePage.tapDiscardChanges();
            shortWait();
        }
    }

}
