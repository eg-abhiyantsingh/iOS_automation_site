package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
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

    private String openAssetIssuesSection() {
        loginAndSelectSite();
        assetPage.navigateToAssetListTurbo();
        String name = assetPage.openSharedAssetForEditOrFallback(null);
        logStep("Opened asset: " + name);
        if (!assetPage.isEditAssetScreenDisplayed()) throw new SkipException("Asset details did not open");
        assetPage.scrollToIssuesSection();
        mediumWait();
        skipIfPreconditionMissing(() -> assetPage.assetIssuesHeaderCount() >= 0,
                "the asset Issues section header ('Issues (N)') is not on screen");
        return name;
    }

    /** All-status issue count for the asset from the SLD payload, or -1 when the API cannot answer. */
    private int backendIssueCountForAsset(String assetLabel) {
        try {
            TestDataApi api = new TestDataApi();
            api.login();
            List<String> slds = api.accessibleSldIds();
            if (slds.isEmpty()) { String f = api.firstSldId(); if (f != null) slds = List.of(f); }
            for (String sldId : slds) {
                String json = api.getSldDetails(sldId);
                String nodeId = TestDataApi.extractSiblingField(json, "label", assetLabel, "id");
                if (nodeId == null) continue;
                int n = TestDataApi.countIssuesForNode(json, nodeId);
                logStep("Backend: asset '" + assetLabel + "' = node " + nodeId + " on SLD " + sldId + " → " + n + " issue(s), all statuses");
                return n;
            }
            logStep("Backend: asset '" + assetLabel + "' not found in any accessible SLD");
        } catch (Exception e) {
            logStep("Backend unavailable for the cross-check: " + e.getMessage());
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
                "TC_AI_04 - After filtering to Open, reopening the asset restores the All default");
        openAssetIssuesSection();
        int allCount = assetPage.assetIssuesHeaderCount();
        skipIfPreconditionMissing(() -> assetPage.tapAssetIssuesFilter(), "Filter button did not open a menu");
        mediumWait();
        skipIfPreconditionMissing(() -> assetPage.chooseIssueFilterOption("Open"), "the Filter menu offers no 'Open' option");
        mediumWait();
        int openCount = assetPage.assetIssuesHeaderCount();
        logStep("header count — default: " + allCount + " · after 'Open': " + openCount);

        logStep("Closing and reopening the asset");
        assetPage.clickCloseButton();
        mediumWait();
        openAssetIssuesSection();
        int again = assetPage.assetIssuesHeaderCount();
        logStep("header count after reopen: " + again);
        assertEquals(again, allCount,
                "Reopening the asset must restore the All default (count " + allCount + ") rather than remembering the Open "
                + "filter (count " + openCount + ") — got " + again);
        logStepWithScreenshot("TC_AI_04: default restored on reopen");
    }
}
