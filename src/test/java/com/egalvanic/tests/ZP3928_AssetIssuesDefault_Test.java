package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetPage;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

/**
 * ZP-3928 (iOS 1.56) §2 — the asset-details Issues section defaults to showing ALL issues.
 *
 * Surface (probe 1.63, 2026-09-21): the section header is «Issues» «(N)» with a «Filter» button and
 * an «Add» button; rows are Buttons «title, description, status». There are no inline All/Open chips —
 * the filter lives behind «Filter». So "defaults to All" is proven two ways, strongest first:
 *
 *   1. ARITHMETIC — the header count "(N)" equals the number of issues the backend holds for this
 *      asset across ALL statuses (SLD payload, matched on the node's label). If the default were
 *      Open-only, N would drop as soon as the asset has a resolved/closed issue.
 *   2. MENU — the Filter menu marks the all-issues option as the current one.
 *
 * Before 1.56 the section opened filtered to Open, hiding resolved history from the technician.
 */
public final class ZP3928_AssetIssuesDefault_Test extends BaseTest {

    private static final String FEATURE = "Asset issues default to All (ZP-3928)";

    /** Asset that carries issues on the automation site (4 Open issues on 2026-10-02) — a non-zero count. */
    private static final String ISSUE_BEARING_ASSET = "abhiy ant";

    /** Site the app landed on — the ONLY SLD the backend cross-check reads. */
    private String landedSite;

    private String openAssetIssuesSection() {
        loginAndSelectSite();
        landedSite = siteSelectionPage.getCurrentSiteName();
        assetPage.navigateToAssetListTurbo();
        String name = assetPage.openSharedAssetForEditOrFallback(ISSUE_BEARING_ASSET);
        logStep("Opened asset: " + name + " (site: " + landedSite + ")");
        if (!assetPage.isEditAssetScreenDisplayed()) throw new SkipException("Asset details did not open");
        assetPage.scrollToIssuesSection();
        mediumWait();
        skipIfPreconditionMissing(() -> assetPage.assetIssuesHeaderCount() >= 0,
                "the asset Issues section header ('Issues (N)') is not on screen");
        return name;
    }

    /** All-status issue count for the asset from the SLD payload, or -1 when the API cannot answer. */
    private int backendIssueCountForAsset(String assetLabel) {
        // Read ONLY the landed site's SLD. The old version walked every accessible SLD (247 on this
        // account) downloading each full payload until a label matched — it blew TC_AI_01's 6-min budget
        // on 2026-10-02, and a same-named asset on another site could have answered for the wrong one.
        try {
            TestDataApi api = new TestDataApi();
            api.login();
            String sldId = landedSite == null ? null : api.resolveSldIdByName(landedSite);
            if (sldId == null) {
                logStep("Backend: could not resolve an SLD id for site '" + landedSite + "'");
                return -1;
            }
            String json = api.getSldDetails(sldId);
            String nodeId = TestDataApi.liveNodeIdByLabel(json, assetLabel);
            if (nodeId == null) {
                logStep("Backend: asset '" + assetLabel + "' not found on site '" + landedSite + "' (" + sldId + ")");
                return -1;
            }
            int n = TestDataApi.countIssuesForNode(json, nodeId);
            logStep("Backend: asset '" + assetLabel + "' = node " + nodeId + " on '" + landedSite + "' → " + n + " issue(s), all statuses");
            return n;
        } catch (Exception e) {
            logStep("Backend unavailable for the cross-check: " + e);
        }
        return -1;
    }

    @Test(priority = 1)
    public void TC_AI_01_headerCountEqualsAllStatusBackendCount() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, FEATURE,
                "TC_AI_01 - The asset Issues section shows ALL issues by default (count matches the backend across statuses)");
        String asset = openAssetIssuesSection();
        int shown = assetPage.assetIssuesHeaderCount();
        int rows  = assetPage.assetIssueRowCount();
        logStep("UI: header (N)=" + shown + " · visible rows=" + rows);

        int backend = backendIssueCountForAsset(asset);
        if (backend < 0) {
            throw new SkipException("Backend cross-check unavailable — the header count alone cannot prove 'All' vs 'Open'");
        }
        assertEquals(shown, backend,
                "The asset Issues section must default to ALL issues: backend holds " + backend + " issue(s) for '" + asset
                + "' across all statuses but the section header shows (" + shown + "). A smaller number means the default "
                + "filter hides resolved/closed history from the technician (the pre-1.56 behaviour).");
        logStepWithScreenshot("TC_AI_01: header (" + shown + ") = backend " + backend);
    }

    @Test(priority = 2)
    public void TC_AI_02_filterMenuMarksAllAsCurrent() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, FEATURE,
                "TC_AI_02 - The section's Filter menu offers the status set and marks All as current");
        openAssetIssuesSection();
        skipIfPreconditionMissing(() -> assetPage.tapAssetIssuesFilter(), "the Issues section 'Filter' button did not open a menu");
        mediumWait();
        List<String> options = assetPage.visibleIssueFilterOptions();
        String current = assetPage.currentIssueFilterOption();
        logStep("filter options: " + options + " · marked current: " + current);
        assertTrue(!options.isEmpty(), "The Filter menu should list status options (All / Open / Resolved / …)");
        assertTrue(options.contains("All") || options.contains("Show All Issues"),
                "The Filter menu must offer the all-issues option — got " + options);
        if (current == null) {
            logStep("The menu exposes no selected-state attribute on this build — the default is proven arithmetically by TC_AI_01");
        } else {
            assertTrue(current.equals("All") || current.equals("Show All Issues"),
                    "The default Issues filter must be All, but the menu marks '" + current + "' as current");
        }
        logStepWithScreenshot("TC_AI_02: filter menu");
        assetPage.dismissMenuIfOpen();
    }

    @Test(priority = 4)
    public void TC_AI_04_defaultReturnsOnReopen() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, FEATURE,
                "TC_AI_04 - After narrowing the filter, reopening the asset restores the All default");
        openAssetIssuesSection();
        int allCount = assetPage.assetIssuesHeaderCount();
        skipIfPreconditionMissing(() -> assetPage.tapAssetIssuesFilter(), "Filter button did not open a menu");
        mediumWait();
        // 1.67 labels the narrowing option "Show Unresolved Only" (the old "Open" kept for older builds).
        String narrowing = assetPage.chooseIssueFilterOption(AssetPage.ISSUE_FILTER_UNRESOLVED)
                ? AssetPage.ISSUE_FILTER_UNRESOLVED
                : (assetPage.chooseIssueFilterOption("Open") ? "Open" : null);
        skipIfPreconditionMissing(() -> narrowing != null, "the Filter menu offers no narrowing option ('"
                + AssetPage.ISSUE_FILTER_UNRESOLVED + "' / 'Open')");
        mediumWait();
        int narrowCount = assetPage.assetIssuesHeaderCount();

        // MENU oracle — works even when every issue is unresolved and the counts cannot differ.
        skipIfPreconditionMissing(() -> assetPage.tapAssetIssuesFilter(), "Filter button did not reopen the menu");
        mediumWait();
        String chosen = assetPage.currentIssueFilterOption();
        assetPage.dismissMenuIfOpen();
        logStep("header count — default: " + allCount + " · after '" + narrowing + "': " + narrowCount
                + " · menu marks current: " + chosen);
        assertEquals(chosen, narrowing, "Choosing '" + narrowing + "' must take effect before the reopen check");

        logStep("Closing and reopening the asset");
        assetPage.clickCloseButton();
        mediumWait();
        openAssetIssuesSection();
        int again = assetPage.assetIssuesHeaderCount();
        skipIfPreconditionMissing(() -> assetPage.tapAssetIssuesFilter(), "Filter button did not open after reopen");
        mediumWait();
        String afterReopen = assetPage.currentIssueFilterOption();
        assetPage.dismissMenuIfOpen();
        logStep("after reopen — header count: " + again + " · menu marks current: " + afterReopen);
        assertTrue(AssetPage.ISSUE_FILTER_ALL.equals(afterReopen) || "All".equals(afterReopen),
                "Reopening the asset must restore the all-issues default, but the Filter menu marks '"
                + afterReopen + "' (the narrowing choice was '" + narrowing + "')");
        assertEquals(again, allCount,
                "Reopening must restore the All count (" + allCount + ") rather than the narrowed count (" + narrowCount
                + ") — got " + again);
        logStepWithScreenshot("TC_AI_04: default restored on reopen");
    }
}
