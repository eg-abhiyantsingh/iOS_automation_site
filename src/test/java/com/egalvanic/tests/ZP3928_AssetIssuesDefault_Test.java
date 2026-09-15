package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * ZP-3928 (iOS 1.56) §2 — the asset-details Issues section defaults to showing ALL issues.
 *
 * Before 1.56 the section opened filtered to Open, so a technician looking at an asset could
 * not see its resolved history without changing a filter. The regression this guards is a
 * silent revert to an Open-only default.
 */
public final class ZP3928_AssetIssuesDefault_Test extends BaseTest {

    private static final String FEATURE = "Asset issues default to All (ZP-3928)";

    private void openAssetIssuesSection() {
        loginAndSelectSite();
        assetPage.navigateToAssetListTurbo();
        String name = assetPage.openSharedAssetForEditOrFallback(null);
        logStep("Opened asset: " + name);
        if (!assetPage.isEditAssetScreenDisplayed()) {
            throw new SkipException("Asset details did not open");
        }
        assetPage.scrollToIssuesSection();
        mediumWait();
    }

    @Test(priority = 1)
    public void TC_AI_01_defaultFilterIsAll() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, FEATURE,
                "TC_AI_01 - The asset Issues section defaults to All, not Open");
        openAssetIssuesSection();

        boolean allSelected  = assetPage.isIssueFilterSelected("All");
        boolean openSelected = assetPage.isIssueFilterSelected("Open");
        logStep("All selected: " + allSelected + " · Open selected: " + openSelected);

        if (!allSelected && !openSelected) {
            throw new SkipException("No issue-filter chips are exposed on the asset details screen — "
                    + "cannot read the default state");
        }
        assertTrue(allSelected,
                "1.56 changes the asset Issues section to default to 'All'. It currently shows "
                + (openSelected ? "'Open'" : "neither") + " as selected, which hides resolved history "
                + "from the technician.");
        logStepWithScreenshot("TC_AI_01: default filter is All");
    }

    @Test(priority = 4)
    public void TC_AI_04_defaultReturnsOnReopen() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, FEATURE,
                "TC_AI_04 - The All default comes back when the asset is reopened");
        openAssetIssuesSection();
        if (!assetPage.isIssueFilterSelected("All") && !assetPage.isIssueFilterSelected("Open")) {
            throw new SkipException("No issue-filter chips exposed — nothing to re-check");
        }

        logStep("Switching the filter away from the default");
        assetPage.tapIssueFilter("Open");
        mediumWait();

        logStep("Closing and reopening the asset");
        assetPage.clickCloseButton();
        mediumWait();
        assetPage.navigateToAssetListTurbo();
        assetPage.openSharedAssetForEditOrFallback(null);
        assetPage.scrollToIssuesSection();
        mediumWait();

        assertTrue(assetPage.isIssueFilterSelected("All"),
                "Reopening the asset should restore the 'All' default rather than remembering 'Open'");
        logStepWithScreenshot("TC_AI_04: default restored on reopen");
    }
}
