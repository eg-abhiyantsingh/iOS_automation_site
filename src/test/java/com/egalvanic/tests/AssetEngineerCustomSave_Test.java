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
 * asset_engineer library — Custom Equipment SAVE + Edit round-trip (gap G4).
 *
 * The never-before-tested enabled-Save path of the "Add Custom Equipment"
 * sheet, plus the Edit round-trip on the resulting orange "Custom Entry"
 * bound card. App truth: CustomEqpLibSheet.swift buildSelection /
 * hydrateFromExisting, NodeEngineeringSection.swift bound card (:659-841)
 * + applyCustomSelection (:368-403).
 *
 * EVERYTHING runs inside a CANCELLED Detailed Fuse Add-Asset draft — the
 * fuse match panel renders unconditionally (v1.49 OCP layout), so the
 * Add Custom chip is always reachable, and closeAssetDetails(true) at the
 * end of every test guarantees nothing ever persists server-side. The
 * final chain step re-enters a fresh draft to PROVE the discard left no
 * residue.
 */
public class AssetEngineerCustomSave_Test extends BaseTest {

    private AssetEngineerPage engineerPage;
    private boolean libraryChecked = false;

    /**
     * The match panel (host of the Add Custom chip) does not render at all
     * until the SKM library is cached — on a fresh container every fixture
     * here would fail at tapAddCustom. Gate once per class run (same
     * pattern as AssetEngineerCustomSheet_Test).
     */
    private void ensureReadyOnce() {
        if (libraryChecked) return;
        loginAndSelectSite();
        engineerPage.ensureLibraryDownloaded(600);
        libraryChecked = true;
    }

    /** Whole module is gated on the platform-managed eng-lib company flag (BaseTest skips pre-driver when absent). */
    @Override
    protected String requiredCompanyFeature() {
        return "eng-lib";
    }

    @BeforeClass(alwaysRun = true)
    public void customSaveSetup() {
        System.out.println("\n📋 Asset Engineer — custom equipment SAVE + Edit round-trip (cancelled drafts)");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void customSaveTestSetup() {
        if (!DriverManager.isDriverActive()) return; // gated/fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void customSaveTeardown() {
        try {
            engineerPage = new AssetEngineerPage();
            if (engineerPage.isCustomSheetOpen(1)) {
                engineerPage.tapCustomSheetButton("Cancel");
            }
            engineerPage.closeAssetDetails(true);
        } catch (Exception ignored) {
        }
        DriverManager.resetNoResetOverride();
    }

    // ── fixtures ───────────────────────────────────────────────────────

    /** Back out of any open sheet/draft, land on the Assets list. */
    private void resetToAssetsList() {
        try {
            if (engineerPage.isCustomSheetOpen(1)) {
                engineerPage.tapCustomSheetButton("Cancel");
                mediumWait();
            }
        } catch (Exception ignored) { }
        engineerPage.closeAssetDetails(true);
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
    }

    /**
     * Fresh Detailed Fuse Add-Asset draft. The fuse OCP match panel renders
     * unconditionally once the class is picked, so waitForMatchHeader is the
     * fixture anchor.
     */
    private void openFreshFuseDraft() {
        ensureReadyOnce();
        resetToAssetsList();
        assetPage.clickAddAsset();
        mediumWait();
        assertTrue(engineerPage.pickOptionExact("Detailed"), "'Detailed' toggle must be tappable");
        assetPage.clickSelectAssetClass();
        mediumWait();
        try {
            engineerPage.searchInSheetPicker("Fuse");
        } catch (Exception e) {
            System.out.println("⚠️ class search: " + e.getMessage());
        }
        boolean picked = engineerPage.pickOptionExact("Fuse");
        skipIfPreconditionMissing(() -> picked, "Fuse class not available in the class picker");
        mediumWait();
        assertTrue(engineerPage.waitForMatchHeader(10),
                "fuse draft must render the unconditional match panel (v1.49 OCP layout)");
    }

    /** Open the Add Custom sheet from the fresh fuse draft's match panel. */
    private void openCustomSheetFromDraft() {
        engineerPage.tapAddCustom();
        assertTrue(engineerPage.isCustomSheetOpen(5),
                "'Add Custom Equipment' sheet must open from the fuse draft's match panel");
    }

    /** Fresh fuse draft with the custom sheet already open. */
    private void openFreshFuseDraftWithSheet() {
        openFreshFuseDraft();
        openCustomSheetFromDraft();
    }

    /**
     * Save the sheet and verify the ORANGE "Custom Entry" bound card binds:
     * sheet title gone, Unlink renders, custom-entry card renders.
     */
    private void saveSheetAndAwaitCustomCard(String context) {
        engineerPage.dismissKeyboard();
        assertTrue(engineerPage.isCustomSaveEnabled(),
                "Save must be ENABLED before tapping it (" + context + ")");
        engineerPage.tapCustomSheetButton("Save");
        assertTrue(engineerPage.waitForLabelGone(AssetEngineerPage.CUSTOM_SHEET_TITLE, 8),
                "custom sheet must close after an enabled Save (" + context + ")");
        assertTrue(engineerPage.waitForOptionShown("Unlink", 10),
                "'Unlink' must appear once the custom entry binds (" + context + ")");
        assertTrue(engineerPage.isCustomEntryCardShown(),
                "orange 'Custom Entry' bound card must render after Save (" + context + ")");
    }

    /** Unlink the custom binding and verify the match panel/input stack returns. */
    private void unlinkAndAssertPanelReturns() {
        assertTrue(engineerPage.tapUnlink(),
                "Unlink must remove the draft's custom binding (bound cards must disappear)");
        assertTrue(engineerPage.waitForMatchHeader(10),
                "match panel must return after unlinking the custom entry");
        assertTrue(engineerPage.isAddCustomButtonShown(),
                "'Add Custom' chip must return after unlinking the custom entry");
    }

    /** Discard the whole draft — nothing from this test may persist. */
    private void discardDraft() {
        assertTrue(engineerPage.closeAssetDetails(true),
                "draft must close with Discard back to the Assets list");
    }

    // ═══════════ focused: the enabled-Save path ═══════════

    /** Save-and-bind works from EITHER identity anchor (gates proven by TC_ENG_081). */
    @DataProvider(name = "saveAnchors")
    public Object[][] saveAnchors() {
        return new Object[][] {
                {"Manufacturer", "QA Save Mfr 170"},
                {"Type / Catalog", "QA-SAVE-CAT-170"},
        };
    }

    @Test(priority = 1101, dataProvider = "saveAnchors")
    public void TC_ENG_170_enabledSaveBindsCustomEntryCard(String field, String value) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_170 [" + field + "] - Enabled Save from this identity anchor binds the Custom Entry card");

        logStep("Step 1: Fresh Fuse draft with the Add Custom sheet open");
        openFreshFuseDraftWithSheet();

        logStep("Step 2: Fill '" + field + "' — Save must enable from this anchor alone");
        engineerPage.typeCustomField(field, value);
        assertTrue(engineerPage.isCustomSaveEnabled(),
                "Save must ENABLE once '" + field + "' is non-blank");

        logStep("Step 3: Save — sheet closes and the orange Custom Entry card binds");
        saveSheetAndAwaitCustomCard("anchor=" + field);
        logStepWithScreenshot("TC_ENG_170 [" + field + "] custom entry bound");

        logStep("Step 4: Unlink, then discard the draft");
        unlinkAndAssertPanelReturns();
        discardDraft();
    }

    @Test(priority = 1102)
    public void TC_ENG_171_disabledSaveIsNoOp() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_171 - Blank-identity Save stays disabled and tapping it is a no-op");

        logStep("Step 1: Fresh Fuse draft with the Add Custom sheet open");
        openFreshFuseDraftWithSheet();

        logStep("Step 2: Save must start DISABLED with a blank identity");
        assertFalse(engineerPage.isCustomSaveEnabled(),
                "Save must be DISABLED while every identity field is blank");

        logStep("Step 3: Tapping the disabled Save must NOT close the sheet or bind anything");
        engineerPage.tapCustomSheetButton("Save");
        assertTrue(engineerPage.isCustomSheetOpen(2),
                "sheet must stay open after tapping the DISABLED Save button");
        assertFalse(engineerPage.isCustomEntryCardShown(),
                "no Custom Entry card may bind from a disabled Save");
        logStepWithScreenshot("TC_ENG_171 disabled Save verified as a no-op");

        logStep("Step 4: Cancel the sheet, then discard the draft");
        engineerPage.tapCustomSheetButton("Cancel");
        assertTrue(engineerPage.waitForLabelGone(AssetEngineerPage.CUSTOM_SHEET_TITLE, 8),
                "sheet must close on Cancel");
        assertFalse(engineerPage.isCustomEntryCardShown(),
                "cancelled sheet must not leave a Custom Entry card behind");
        discardDraft();
    }

    // ═══════════ the full save → card → edit → unlink → integrity chain ═══════════

    @Test(priority = 1103)
    public void TC_ENG_172_saveEditUnlinkDiscardChain() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_172 - Full chain: save w/ setting + Amps → card mirrors → Edit hydrated → cancel edit → unlink → discard leaves no residue");

        final String mfr = "QA Save Mfr 172";
        final String settingLabel = "QA-PU-172";

        logStep("Step 1: Fresh Fuse draft with the Add Custom sheet open");
        openFreshFuseDraftWithSheet();

        logStep("Step 2: Identity — Manufacturer + Amps 400 (continuous current)");
        engineerPage.typeCustomField("Manufacturer", mfr);
        engineerPage.typeCustomField("Amps", "400");

        logStep("Step 3: Add one free-form setting row (Function default Pickup) with label+value");
        assertTrue(engineerPage.pickOptionExact("Add Setting"),
                "'Add Setting' must add a free-form settings row");
        assertTrue(engineerPage.waitForOptionShown("Function", 6),
                "new settings row must expose the Function picker");
        assertTrue(engineerPage.isOptionShown("Pickup"),
                "new row's Function must default to 'Pickup'");
        engineerPage.typeCustomField("e.g. \"INST (Ii)\"", settingLabel);
        engineerPage.typeCustomField("Value", "1.5");

        logStep("Step 4: Save — the orange Custom Entry card binds (buildSelection path)");
        saveSheetAndAwaitCustomCard("chain save");
        logStepWithScreenshot("TC_ENG_172 saved: custom entry bound");

        logStep("Step 5: The saved setting's label renders in the card's SETTINGS list");
        assertTrue(engineerPage.isEngineeringLabelPresent(settingLabel),
                "saved setting label '" + settingLabel + "' must render on the bound card");

        logStep("Step 6: Mirroring law — Amps=400 must surface on the bound engineering block");
        // Layered read of ONE fact ("400" renders after save): editable field
        // first, then a picker-style Amps row, then the card's static "400"/
        // "400 A" text (the bound card renders amps as plain text, not a
        // TextField — brief-sanctioned fallback, see gap G4).
        String fieldMirror = engineerPage.getEngineeringFieldValue("Amps");
        String pickerMirror = "";
        if (!fieldMirror.contains("400") && engineerPage.isEngineeringLabelPresent("Amps")) {
            pickerMirror = engineerPage.getPickerValueBelowLabel("Amps");
        }
        boolean mirrored = fieldMirror.contains("400")
                || pickerMirror.contains("400")
                || engineerPage.isEngineeringLabelPresent("400 A")
                || engineerPage.isEngineeringLabelPresent("400");
        assertTrue(mirrored, "saved Amps=400 must mirror onto the bound card; field='"
                + fieldMirror + "', picker='" + pickerMirror + "', no '400'/'400 A' text found");

        logStep("Step 7: Edit round-trip — sheet reopens TITLED 'Edit Custom Equipment', hydrated");
        assertTrue(engineerPage.tapRowByExactLabel("Edit"),
                "'Edit' on the bound card must be tappable");
        assertTrue(engineerPage.waitForOptionShown(AssetEngineerPage.CUSTOM_SHEET_EDIT_TITLE, 8),
                "reopened sheet must carry the '" + AssetEngineerPage.CUSTOM_SHEET_EDIT_TITLE
                        + "' title (hydrateFromExisting path)");
        String hydratedMfr = engineerPage.getEngineeringFieldValue("Manufacturer");
        assertEquals(hydratedMfr, mfr,
                "edit sheet must HYDRATE the saved manufacturer; got '" + hydratedMfr + "'");
        logStepWithScreenshot("TC_ENG_172 edit sheet hydrated");

        logStep("Step 8: Cancel the edit — the custom card must stay bound");
        engineerPage.tapCustomSheetButton("Cancel");
        assertTrue(engineerPage.waitForLabelGone(AssetEngineerPage.CUSTOM_SHEET_EDIT_TITLE, 8),
                "edit sheet must close on Cancel");
        assertTrue(engineerPage.isCustomEntryCardShown(),
                "Custom Entry card must survive a cancelled edit");

        logStep("Step 9: Unlink — card gone, match panel/input stack returns");
        unlinkAndAssertPanelReturns();

        logStep("Step 10: Discard the whole draft");
        discardDraft();

        logStep("Step 11: Integrity — a fresh Fuse draft must carry NO custom entry residue");
        openFreshFuseDraft();
        assertFalse(engineerPage.isCustomEntryCardShown(),
                "fresh Fuse draft must have NO Custom Entry card — the discarded save leaked");
        assertTrue(engineerPage.isAddCustomButtonShown(),
                "fresh Fuse draft must show the unbound 'Add Custom' chip");
        logStepWithScreenshot("TC_ENG_172 integrity: no residue in a fresh draft");
        discardDraft();
    }

    // ═══════════ data-driven: Amps mirroring + settings labels ═══════════

    @DataProvider(name = "ampsMirror")
    public Object[][] ampsMirror() {
        return new Object[][] { {"250"}, {"1600"} };
    }

    @Test(priority = 1104, dataProvider = "ampsMirror")
    public void TC_ENG_173_savedAmpsMirrorsOntoBoundCard(String amps) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_173 [" + amps + "A] - Saved Amps value mirrors onto the bound engineering block");

        logStep("Step 1: Fresh Fuse draft, sheet with Manufacturer + Amps=" + amps);
        openFreshFuseDraftWithSheet();
        engineerPage.typeCustomField("Manufacturer", "QA Mirror Mfr 173");
        engineerPage.typeCustomField("Amps", amps);

        logStep("Step 2: Save and bind");
        saveSheetAndAwaitCustomCard("amps=" + amps);

        logStep("Step 3: '" + amps + "' must render on the bound block (field/picker/static text)");
        String fieldMirror = engineerPage.getEngineeringFieldValue("Amps");
        String pickerMirror = "";
        if (!fieldMirror.contains(amps) && engineerPage.isEngineeringLabelPresent("Amps")) {
            pickerMirror = engineerPage.getPickerValueBelowLabel("Amps");
        }
        boolean mirrored = fieldMirror.contains(amps)
                || pickerMirror.contains(amps)
                || engineerPage.isEngineeringLabelPresent(amps + " A")
                || engineerPage.isEngineeringLabelPresent(amps);
        assertTrue(mirrored, "saved Amps=" + amps + " must mirror after save; field='"
                + fieldMirror + "', picker='" + pickerMirror + "', no '" + amps + "' text found");
        logStepWithScreenshot("TC_ENG_173 [" + amps + "A] mirror verified");

        logStep("Step 4: Unlink, then discard the draft");
        unlinkAndAssertPanelReturns();
        discardDraft();
    }

    /** Free-form setting rows survive Save and render on the bound card. */
    @DataProvider(name = "settingRows")
    public Object[][] settingRows() {
        return new Object[][] {
                {"INST (Ii)", "0.08"},
                {"Delay (t)", "24"},
        };
    }

    @Test(priority = 1105, dataProvider = "settingRows")
    public void TC_ENG_174_savedSettingLabelRendersOnCard(String label, String value) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_174 [" + label + "] - Saved free-form setting renders its label on the bound card");

        logStep("Step 1: Fresh Fuse draft, sheet with Manufacturer anchor");
        openFreshFuseDraftWithSheet();
        engineerPage.typeCustomField("Manufacturer", "QA Set Mfr 174");

        logStep("Step 2: Add a setting row '" + label + "' = '" + value + "'");
        assertTrue(engineerPage.pickOptionExact("Add Setting"),
                "'Add Setting' must add a free-form settings row");
        assertTrue(engineerPage.waitForOptionShown("Function", 6),
                "new settings row must expose the Function picker");
        engineerPage.typeCustomField("e.g. \"INST (Ii)\"", label);
        engineerPage.typeCustomField("Value", value);

        logStep("Step 3: Save and bind");
        saveSheetAndAwaitCustomCard("setting=" + label);

        logStep("Step 4: The setting's label must render in the card's settings list");
        assertTrue(engineerPage.isEngineeringLabelPresent(label),
                "saved setting label '" + label + "' must render on the bound card");
        logStepWithScreenshot("TC_ENG_174 [" + label + "] setting rendered on card");

        logStep("Step 5: Unlink, then discard the draft");
        unlinkAndAssertPanelReturns();
        discardDraft();
    }

    // ═══════════ edit-and-resave (applyCustomSelection edit path) ═══════════

    @Test(priority = 1106)
    public void TC_ENG_175_editResaveKeepsCustomBinding() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_175 - Edit sheet re-Save keeps the Custom Entry binding (applyCustomSelection edit path)");

        final String mfr = "QA Mfr 175";

        logStep("Step 1: Save a minimal custom entry in a fresh Fuse draft");
        openFreshFuseDraftWithSheet();
        engineerPage.typeCustomField("Manufacturer", mfr);
        saveSheetAndAwaitCustomCard("initial save");

        logStep("Step 2: Reopen via Edit — sheet carries the edit title");
        assertTrue(engineerPage.tapRowByExactLabel("Edit"),
                "'Edit' on the bound card must be tappable");
        assertTrue(engineerPage.waitForOptionShown(AssetEngineerPage.CUSTOM_SHEET_EDIT_TITLE, 8),
                "reopened sheet must carry the '" + AssetEngineerPage.CUSTOM_SHEET_EDIT_TITLE + "' title");

        logStep("Step 3: Extend the identity (Type / Catalog) and re-Save");
        engineerPage.typeCustomField("Type / Catalog", "QA-CAT-175-EDIT");
        engineerPage.dismissKeyboard();
        assertTrue(engineerPage.isCustomSaveEnabled(),
                "Save must stay ENABLED in the hydrated edit sheet");
        engineerPage.tapCustomSheetButton("Save");
        assertTrue(engineerPage.waitForLabelGone(AssetEngineerPage.CUSTOM_SHEET_EDIT_TITLE, 8),
                "edit sheet must close after re-Save");

        logStep("Step 4: The Custom Entry card must still be bound after the edit-save");
        assertTrue(engineerPage.waitForOptionShown("Unlink", 10),
                "'Unlink' must still render after the edit re-Save");
        assertTrue(engineerPage.isCustomEntryCardShown(),
                "Custom Entry card must survive an edit re-Save (applyCustomSelection)");
        logStepWithScreenshot("TC_ENG_175 edit re-save kept the binding");

        logStep("Step 5: Unlink, then discard the draft");
        unlinkAndAssertPanelReturns();
        discardDraft();
    }
}
