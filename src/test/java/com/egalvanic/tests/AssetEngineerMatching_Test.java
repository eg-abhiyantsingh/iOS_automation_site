package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetEngineerPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * asset_engineer library — LIVE MATCHING probes.
 *
 * ⚠️ QUARANTINE CLASS — MUST STAY LAST IN THE SUITE. On v1.49, rendering
 * match RESULTS on the transformer DETAILS screen wedges WDA and kills the
 * session (documented app defect, changelog 107). These probes explore the
 * matching surface on SMALLER DOMs (fuse details ~88KB, Add-Asset drafts
 * ~67KB) where the wedge has not been observed; if a wedge does occur the
 * suite is already past every other class, so nothing cascades.
 *
 * Every step here verifies outcome by cheap breadth probes (existsNow) —
 * never getPageSource.
 */
public class AssetEngineerMatching_Test extends BaseTest {

    private AssetEngineerPage engineerPage;

    @BeforeClass(alwaysRun = true)
    public void matchingSetup() {
        System.out.println("\n📋 Asset Engineer — live matching probes (quarantined last)");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void matchingTestSetup() {
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void matchingTeardown() {
        try {
            engineerPage = new AssetEngineerPage();
            engineerPage.closeAssetDetails(true);
        } catch (Exception e) {
            System.out.println("⚠️ matching teardown: " + e.getMessage());
        }
        DriverManager.resetNoResetOverride();
    }

    @Test(priority = 1201)
    public void TC_ENG_110_fuseManufacturerPickSurfacesMatchHeader() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_110 - Fuse: picking a manufacturer updates the match header (small-DOM probe)");

        logStep("Step 1: Open fuse engineering (unset panel: 'No possible matches')");
        loginAndSelectSite();
        // The match panel only exists once the SKM library is cached.
        engineerPage.ensureLibraryDownloaded(600);
        assetPage.navigateToAssetList();
        shortWait();
        engineerPage.openAssetCardByPrefix("Trim600639 Fuse");
        assertTrue(engineerPage.swipeToEngineeringSection(), "engineering reachable");
        assertEquals(engineerPage.getMatchHeaderText(), "No possible matches",
                "precondition: unset fuse panel shows the zero header");

        logStep("Step 2: Pick a manufacturer in the panel filter");
        engineerPage.openEngineeringPickerBelowLabel("Manufacturer");
        // The fuse manufacturer list is library-routed; pick any first real
        // option (menu when ≤20 eligible; sheet with search when more).
        final boolean picked = engineerPage.pickOptionExact("ABB")
                || engineerPage.pickOptionExact("Generic")
                || engineerPage.pickOptionExact("SCHNEIDER/SQUARE D");
        skipIfPreconditionMissing(() -> picked,
                "no known manufacturer option in the fuse menu — capture the menu and extend the list");

        logStep("Step 3: Match header updates (results or a legitimate zero-state)");
        assertTrue(engineerPage.waitForMatchHeader(10),
                "match header must still render after picking a manufacturer");
        String header = engineerPage.getMatchHeaderText();
        assertTrue(header.matches("\\d+\\+? possible match(es)?|No possible matches"),
                "header must follow the '{n}[+] possible match(es)' format; got '" + header + "'");
        logStepWithScreenshot("TC_ENG_110 header='" + header + "'");

        logStep("Step 4: Discard the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "details must close with discard");
    }

    @Test(priority = 1202)
    public void TC_ENG_111_matchSearchZeroStateOnGibberish() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_111 - Match search with gibberish shows the search-specific empty state");

        logStep("Step 1: Open fuse engineering");
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        engineerPage.openAssetCardByPrefix("Trim600639 Fuse");
        assertTrue(engineerPage.swipeToEngineeringSection(), "engineering reachable");

        logStep("Step 2: Type gibberish into the match search");
        engineerPage.typeIntoEngineeringField("e.g. \"QD\" or \"Formula\"", "zzqx987");
        mediumWait(); // 250ms debounce + recompute

        logStep("Step 3: Search-specific zero state");
        assertEquals(engineerPage.getMatchEmptyStateText(), "No matches for that search.",
                "gibberish search must show the search-specific empty-state copy");
        logStepWithScreenshot("TC_ENG_111 verified");

        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 1203)
    public void TC_ENG_112_buswayDraftConductorTogglesFireMatcher() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_112 - Busway draft: Cu fires the cable/busway matcher; Al re-fires it");

        logStep("Step 1: Detailed Busway draft");
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        mediumWait();
        assertTrue(engineerPage.pickOptionExact("Detailed"), "'Detailed' toggle");
        assetPage.clickSelectAssetClass();
        mediumWait();
        try {
            engineerPage.searchInSheetPicker("Busway");
        } catch (Exception e) {
            System.out.println("⚠️ class search: " + e.getMessage());
        }
        skipIfPreconditionMissing(() -> engineerPage.pickOptionExact("Busway"), "no Busway class");
        mediumWait();

        logStep("Step 2: No match header before a conductor material is set");
        assertEquals(engineerPage.getMatchHeaderText(), "",
                "cable/busway matcher must NOT render before Conductor Material is picked");

        logStep("Step 3: Cu fires the matcher");
        assertTrue(engineerPage.tapSegment("Cu"), "Cu segment must be tappable");
        assertTrue(engineerPage.waitForMatchHeader(10),
                "match header must render once Conductor Material = Cu");
        String cuHeader = engineerPage.getMatchHeaderText();
        assertTrue(cuHeader.matches("\\d+\\+? possible match(es)?|No possible matches"),
                "Cu header format; got '" + cuHeader + "'");

        logStep("Step 4: Al keeps the matcher alive");
        assertTrue(engineerPage.tapSegment("Al"), "Al segment must be tappable");
        assertTrue(engineerPage.waitForMatchHeader(10),
                "match header must survive the Cu→Al toggle");
        logStepWithScreenshot("TC_ENG_112 cu='" + cuHeader + "' al='"
                + engineerPage.getMatchHeaderText() + "'");

        logStep("Step 5: Cancel the draft");
        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 1204)
    public void TC_ENG_113_breakerDraftShowsUnconditionalPanel() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_113 - Circuit Breaker draft renders the v1.49 unconditional OCP match panel");

        logStep("Step 1: Detailed Circuit Breaker draft");
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        mediumWait();
        assertTrue(engineerPage.pickOptionExact("Detailed"), "'Detailed' toggle");
        assetPage.clickSelectAssetClass();
        mediumWait();
        try {
            engineerPage.searchInSheetPicker("Circuit");
        } catch (Exception e) {
            System.out.println("⚠️ class search: " + e.getMessage());
        }
        skipIfPreconditionMissing(() -> engineerPage.pickOptionExact("Circuit Breaker"),
                "no Circuit Breaker class");
        mediumWait();

        logStep("Step 2: Unconditional panel pieces (v1.49 OCP layout)");
        assertEquals(engineerPage.getMatchHeaderText(), "No possible matches",
                "unset breaker panel must show the zero header");
        assertTrue(engineerPage.isAddCustomButtonShown(), "'Add Custom' chip must render");
        assertTrue(engineerPage.isMatchSearchFieldShown(), "match search field must render");
        assertTrue(engineerPage.isEngineeringLabelPresent("Manufacturer"),
                "Manufacturer filter must render in the breaker panel");
        logStepWithScreenshot("TC_ENG_113 verified");

        logStep("Step 3: Cancel the draft");
        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 1205)
    public void TC_ENG_114_transformerDraftManufacturerPickFiresMatcher() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_114 - Transformer DRAFT: manufacturer pick fires the matcher (small-DOM wedge probe)");

        logStep("Step 1: Detailed Transformer draft (~67KB DOM vs the wedging 100KB details)");
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        mediumWait();
        assertTrue(engineerPage.pickOptionExact("Detailed"), "'Detailed' toggle");
        assetPage.clickSelectAssetClass();
        mediumWait();
        try {
            engineerPage.searchInSheetPicker("Transformer");
        } catch (Exception e) {
            System.out.println("⚠️ class search: " + e.getMessage());
        }
        skipIfPreconditionMissing(() -> engineerPage.pickOptionExact("Transformer"),
                "no Transformer class");
        mediumWait();

        logStep("Step 2: Pick a library-backed manufacturer in the draft");
        engineerPage.openEngineeringPickerBelowLabel("Manufacturer");
        final boolean picked = engineerPage.pickOptionExact("Generic")
                || engineerPage.pickOptionExact("SCHNEIDER/SQUARE D");
        skipIfPreconditionMissing(() -> picked, "no library-backed transformer manufacturer in menu");

        logStep("Step 3: Matcher fires without wedging (breadth probes only)");
        assertTrue(engineerPage.waitForMatchHeader(12),
                "transformer match header must render after the manufacturer pick");
        String header = engineerPage.getMatchHeaderText();
        assertTrue(header.matches("\\d+\\+? possible match(es)?|No possible matches"),
                "header format; got '" + header + "'");
        logStepWithScreenshot("TC_ENG_114 header='" + header + "'");

        logStep("Step 4: Cancel the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "draft must cancel cleanly");
    }
}
