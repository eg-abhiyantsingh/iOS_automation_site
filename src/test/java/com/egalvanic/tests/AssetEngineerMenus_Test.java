package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetEngineerPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * asset_engineer library — enum-menu selection semantics.
 *
 * All selection rows run on a DETAILED Panelboard Add-Asset draft (the only
 * class where Mains Type selection applies — user-confirmed domain rule;
 * MCB is special-cased because it opens the "Create a Main Breaker?" sheet,
 * covered by TC_ENG_016/071). The draft is shared across rows and discarded
 * in teardown; nothing persists.
 */
public class AssetEngineerMenus_Test extends BaseTest {

    private AssetEngineerPage engineerPage;
    private boolean panelDraftOpen = false;

    /** Whole module is gated on the platform-managed eng-lib company flag (BaseTest skips pre-driver when absent). */
    @Override
    protected String requiredCompanyFeature() {
        return "eng-lib";
    }

    @BeforeClass(alwaysRun = true)
    public void menusSetup() {
        System.out.println("\n📋 Asset Engineer — enum menu semantics");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void menusTestSetup() {
        if (!DriverManager.isDriverActive()) return; // gated/fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void menusTeardown() {
        try {
            engineerPage = new AssetEngineerPage();
            engineerPage.closeAssetDetails(true);
        } catch (Exception e) {
            System.out.println("⚠️ menus teardown: " + e.getMessage());
        }
        DriverManager.resetNoResetOverride();
    }

    private void ensurePanelboardDraft() {
        if (panelDraftOpen && engineerPage.isEngineeringLabelPresent("Mains Type")) return;
        panelDraftOpen = false;
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        mediumWait();
        guard("open Add Asset form");
        assertTrue(engineerPage.pickOptionExact("Detailed"), "'Detailed' toggle must be tappable");
        assetPage.clickSelectAssetClass();
        mediumWait();
        try {
            engineerPage.searchInSheetPicker("anel");
        } catch (Exception e) {
            System.out.println("⚠️ class search: " + e.getMessage());
        }
        String picked = engineerPage.pickFirstVisibleOptionContaining("anel");
        skipIfPreconditionMissing(() -> picked != null, "no Panelboard class in this site");
        mediumWait();
        assertTrue(engineerPage.isEngineeringLabelPresent("Mains Type"),
                "Panelboard draft must render the Mains Type row");
        panelDraftOpen = true;
    }

    /**
     * Non-MCB mains options set the chip directly (MCB is the special
     * Create-Main flow, covered separately).
     */
    @DataProvider(name = "mainsOptions")
    public Object[][] mainsOptions() {
        return new Object[][] { {"MLO"}, {"FDS"}, {"NFDS"} };
    }

    @Test(priority = 901, dataProvider = "mainsOptions")
    public void TC_ENG_070_mainsTypeOptionSelects(String option) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER,
                AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_070 [" + option + "] - Mains Type option selects and renders on the chip");

        ensurePanelboardDraft();
        engineerPage.openEngineeringPickerBelowLabel("Mains Type");
        assertTrue(engineerPage.pickOptionExact(option),
                "'" + option + "' must be pickable in the Panelboard Mains Type menu");
        assertTrue(engineerPage.waitForPickerValueBelowLabel("Mains Type", option, 6),
                "Mains Type chip must display '" + option + "' after the pick (now='"
                        + engineerPage.getPickerValueBelowLabel("Mains Type") + "')");
        logStepWithScreenshot("TC_ENG_070 [" + option + "] verified");
    }

    @Test(priority = 902)
    public void TC_ENG_071_mainsTypeNoneClearsSelection() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER,
                AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_071 - 'None' row clears the Mains Type selection (allowClear picker)");

        ensurePanelboardDraft();
        // Guarantee a value first (independent of row order).
        engineerPage.openEngineeringPickerBelowLabel("Mains Type");
        assertTrue(engineerPage.pickOptionExact("MLO"), "'MLO' must be pickable");
        assertTrue(engineerPage.waitForPickerValueBelowLabel("Mains Type", "MLO", 6),
                "precondition: chip shows MLO");

        engineerPage.openEngineeringPickerBelowLabel("Mains Type");
        assertTrue(engineerPage.pickOptionExact("None"), "'None' clear row must be pickable");
        assertTrue(engineerPage.waitForPickerValueBelowLabel("Mains Type",
                        AssetEngineerPage.SELECT_ELLIPSIS, 6),
                "Chip must return to 'Select…' after None (now='"
                        + engineerPage.getPickerValueBelowLabel("Mains Type") + "')");
        logStepWithScreenshot("TC_ENG_071 verified");
    }

    @Test(priority = 903)
    public void TC_ENG_072_createMainSheetContent() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER,
                AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_072 - Create-Main sheet content: copy, prefilled name, pole segments, actions");

        ensurePanelboardDraft();
        engineerPage.openEngineeringPickerBelowLabel("Mains Type");
        assertTrue(engineerPage.pickOptionExact("MCB"), "'MCB' must be pickable");
        assertTrue(engineerPage.waitForOptionShown("Create Main", 8),
                "Create-Main sheet must open on MCB");

        assertTrue(engineerPage.isEngineeringLabelPresent("Create a Main Breaker?"),
                "Sheet title must be 'Create a Main Breaker?'");
        assertTrue(engineerPage.isEngineeringLabelPresent(
                        "Adds the main as a child of this panel and wires it up automatically (MCB)."),
                "Sheet copy must describe the auto-wiring behavior exactly");
        assertTrue(engineerPage.isEngineeringLabelPresent("Main Details"),
                "Sheet must show the Main Details section");
        assertEquals(engineerPage.getEngineeringFieldValue("Enter name"), "MCB",
                "Main name field must be prefilled with 'MCB'");
        assertTrue(engineerPage.segmentExists("1P") && engineerPage.segmentExists("2P")
                        && engineerPage.segmentExists("3P"),
                "Sheet must offer 1P/2P/3P pole segments");
        logStepWithScreenshot("TC_ENG_072 sheet content verified");

        assertTrue(engineerPage.pickOptionExact("Cancel"), "Sheet Cancel must be tappable");
        assertTrue(engineerPage.waitForLabelGone("Create a Main Breaker?", 6),
                "Sheet must dismiss on Cancel (no main breaker created)");
    }

    @Test(priority = 904)
    public void TC_ENG_073_atsMainsMenuSharesEnumSet() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER,
                AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_073 - ATS Mains Type menu lists the same server-enum set (global enum table)");

        ensurePanelboardDraft();
        // Switch the SAME draft to ATS — also exercises the destructive
        // class-change alert on a dirty engineering draft. On a SWITCH the
        // chip carries the current class name; AssetPage's 'Select asset
        // class' strategies can misfire (observed: tapped the QR Scan
        // button) — press the chip by label instead.
        if (!engineerPage.pickOptionExact("Panelboard")) {
            assetPage.clickSelectAssetClass();
        }
        mediumWait();
        try {
            engineerPage.searchInSheetPicker("ATS");
        } catch (Exception e) {
            System.out.println("⚠️ class search: " + e.getMessage());
        }
        boolean atsPicked = engineerPage.pickOptionExact("ATS");
        skipIfPreconditionMissing(() -> atsPicked, "no ATS class in this site");
        shortWait();
        if (engineerPage.isOptionShown("Change Class")) {
            assertTrue(engineerPage.tapAlertButton("Change Class"),
                    "'Change Asset Class?' alert must confirm via 'Change Class'");
        }
        mediumWait();
        panelDraftOpen = false; // draft is ATS now — later methods must re-open Panelboard

        skipIfPreconditionMissing(() -> engineerPage.isEngineeringLabelPresent("Mains Type"),
                "ATS draft does not render a Mains Type row");
        engineerPage.openEngineeringPickerBelowLabel("Mains Type");
        assertTrue(engineerPage.waitForOptionShown("MCB", 6), "ATS mains menu must list MCB");
        assertTrue(engineerPage.isOptionShown("MLO"), "ATS mains menu must list MLO");
        assertTrue(engineerPage.isOptionShown("FDS"), "ATS mains menu must list FDS");
        assertTrue(engineerPage.isOptionShown("NFDS"), "ATS mains menu must list NFDS");
        assertTrue(engineerPage.isOptionShown("None"), "ATS mains menu must list the None clear row");
        logStepWithScreenshot("TC_ENG_073 ATS enum parity verified");
        engineerPage.dismissMenuOverlay();
    }

    @Test(priority = 905)
    public void TC_ENG_074_fuseSubtypeSheetOpens() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER,
                AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_074 - Fuse subtype picker opens as a searchable sheet and cancels cleanly");

        // Leave any draft, work on the fuse details fixture.
        engineerPage.closeAssetDetails(true);
        panelDraftOpen = false;
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        engineerPage.openAssetCardByPrefix("Trim600639 Fuse");
        assertTrue(engineerPage.swipeToEngineeringSection(), "Engineering section reachable");
        skipIfPreconditionMissing(() -> engineerPage.isSubtypePickerShown(),
                "fuse subtype picker not rendered (bound or no subtypes) — fixture changed");

        assertTrue(engineerPage.pickOptionExact("Select asset subtype"),
                "Subtype chip must be tappable");
        assertTrue(engineerPage.isSheetPickerOpen(6),
                "Subtype picker must open in sheet mode (search field / Done)");
        logStepWithScreenshot("TC_ENG_074 subtype sheet open");
        // Close without selecting: Done first (sheet keeps selection empty), Cancel fallback.
        if (!engineerPage.pickOptionExact("Done")) {
            engineerPage.pickOptionExact("Cancel");
        }
        assertTrue(engineerPage.waitForOptionShown("Select asset subtype", 6),
                "Subtype chip must remain unset after closing the sheet without a pick");
        engineerPage.closeAssetDetails(true);
    }
}
