package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.Test;

/**
 * TEMPORARY diagnostic — v1.50 Interrupting Rating dropdown + Save-button probe.
 * Not part of any suite; run explicitly with -Dtest=DebugProbe_Test.
 */
public class DebugProbe_Test extends BaseTest {

    @Test
    public void PROBE_interruptingRatingAnatomy() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "PROBE - Interrupting Rating v1.50 anatomy"
        );

        loginAndSelectSite();
        assetPage.openSharedAssetForEditOrFallback(null);
        assetPage.changeAssetClassToDisconnectSwitch();
        assetPage.scrollFormDown();
        shortWait();

        assetPage.debugProbeRatingAndSave("10 kA",
            "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad");
    }

    @Test
    public void PROBE_newIssueFormAnatomy() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ISSUES,
            AppConstants.FEATURE_ISSUES_LIST,
            "PROBE - v1.50 New Issue form anatomy"
        );

        loginAndSelectSite();
        com.egalvanic.pages.IssuePage issuePage = new com.egalvanic.pages.IssuePage();
        issuePage.navigateToIssuesScreen();
        shortWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(
                "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad/issues-list.xml"),
                com.egalvanic.utils.DriverManager.getDriver().getPageSource());
        } catch (Exception e) { System.out.println("PROBE: list dump failed: " + e.getMessage()); }

        issuePage.tapAddButton();
        mediumWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(
                "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad/new-issue-form.xml"),
                com.egalvanic.utils.DriverManager.getDriver().getPageSource());
            System.out.println("PROBE: New Issue form dumped");
        } catch (Exception e) { System.out.println("PROBE: form dump failed: " + e.getMessage()); }
    }
}
