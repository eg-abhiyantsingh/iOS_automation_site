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
 * asset_engineer library — Custom Equipment sheet deep coverage (ZP-2267).
 *
 * The sheet branches by the host class's eqp_lib type:
 *  - protective (fuse/breaker/relay): Identity + free-form Settings rows
 *    ("Add Setting" → Function picker with 15 subtypes, suffix pickers
 *    I²t On/Off and Dial R1–R5)
 *  - transformer: "Transformer Specifications" + Pri/Sec Connection
 *    pickers (Delta / Wye / Wye-Ground / Wye-Ground-Resistor)
 *  - cable/busway: "Cable Specifications" with per-kind defaults
 *    (Conductor Type "Copper"; Duct "Bus" for busway / "Magnetic" for
 *    cable; Length "100")
 *
 * Protective rows run on the fuse details fixture; transformer and busway
 * rows run inside detailed Add-Asset DRAFTS. Everything cancels — the
 * sheet is never saved, drafts are always discarded.
 */
public class AssetEngineerCustomSheet_Test extends BaseTest {

    private AssetEngineerPage engineerPage;
    /** Which host surface currently owns an open custom sheet. */
    private String sheetHost = null;       // "fuse" | "xfmr-draft" | "busway-draft"
    private boolean settingRowAdded = false;
    private boolean libraryChecked = false;

    /**
     * The match panel (host of the Add Custom chip) does not render at all
     * until the SKM library is cached — on a fresh container every fixture
     * here would fail at tapAddCustom (observed live on a fresh Pro Max
     * container). Gate once per class run.
     */
    private void ensureLibraryReadyOnce() {
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
    public void customSheetSetup() {
        System.out.println("\n📋 Asset Engineer — custom equipment sheet deep coverage");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void customSheetTestSetup() {
        if (!DriverManager.isDriverActive()) return; // gated/fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void customSheetTeardown() {
        try {
            engineerPage = new AssetEngineerPage();
            if (engineerPage.isCustomSheetOpen(1)) {
                engineerPage.tapCustomSheetButton("Cancel");
            }
            engineerPage.closeAssetDetails(true);
        } catch (Exception e) {
            System.out.println("⚠️ custom-sheet teardown: " + e.getMessage());
        }
        DriverManager.resetNoResetOverride();
    }

    // ── fixtures ───────────────────────────────────────────────────────

    /** Fuse details → Add Custom sheet (protective branch). */
    private void ensureFuseCustomSheet() {
        if ("fuse".equals(sheetHost) && engineerPage.isCustomSheetOpen(1)) return;
        ensureLibraryReadyOnce();
        resetToAssetsList();
        engineerPage.openAssetCardByPrefix("Trim600639 Fuse");
        assertTrue(engineerPage.swipeToEngineeringSection(), "fuse engineering reachable");
        engineerPage.tapAddCustom();
        assertTrue(engineerPage.isCustomSheetOpen(8), "'Add Custom Equipment' sheet must open (fuse)");
        sheetHost = "fuse";
        settingRowAdded = false;
    }

    /** Fuse sheet with one free-form Settings row added (Function default Pickup). */
    private void ensureFuseSheetWithSettingRow() {
        ensureFuseCustomSheet();
        if (settingRowAdded && engineerPage.isOptionShown("Function")) return;
        assertTrue(engineerPage.pickOptionExact("Add Setting"),
                "'Add Setting' must add a free-form settings row");
        assertTrue(engineerPage.waitForOptionShown("Function", 6),
                "new settings row must expose the Function picker");
        settingRowAdded = true;
    }

    private void resetToAssetsList() {
        try {
            if (engineerPage.isCustomSheetOpen(1)) {
                engineerPage.tapCustomSheetButton("Cancel");
                mediumWait();
            }
        } catch (Exception ignored) { }
        engineerPage.closeAssetDetails(true);
        sheetHost = null;
        settingRowAdded = false;
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
    }

    /** Detailed Add-Asset draft of the given class (search-picked). */
    private void openDraftOfClass(String classSearch, String classExact) {
        ensureLibraryReadyOnce();
        resetToAssetsList();
        assetPage.clickAddAsset();
        mediumWait();
        assertTrue(engineerPage.pickOptionExact("Detailed"), "'Detailed' toggle must be tappable");
        assetPage.clickSelectAssetClass();
        mediumWait();
        try {
            engineerPage.searchInSheetPicker(classSearch);
        } catch (Exception e) {
            System.out.println("⚠️ class search: " + e.getMessage());
        }
        boolean picked = engineerPage.pickOptionExact(classExact);
        skipIfPreconditionMissing(() -> picked, "class '" + classExact + "' not available");
        mediumWait();
    }

    // ═══════════ protective branch (fuse) ═══════════

    @Test(priority = 1001)
    public void TC_ENG_080_protectiveIdentityFields() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_080 - Protective custom sheet: Identity section with all four fields");

        ensureFuseCustomSheet();
        assertTrue(engineerPage.isEngineeringLabelPresent("Identity"),
                "'Identity' section header must render");
        assertTrue(engineerPage.isEngineeringFieldPresent("Manufacturer"),
                "'Manufacturer' field must render");
        assertTrue(engineerPage.isEngineeringFieldPresent("Type / Catalog"),
                "'Type / Catalog' field must render");
        assertTrue(engineerPage.isEngineeringFieldPresent("Style / Model"),
                "'Style / Model' field must render");
        assertTrue(engineerPage.isEngineeringFieldPresent("Amps"),
                "'Amps' field must render for protective classes");
        logStepWithScreenshot("TC_ENG_080 identity fields verified");
    }

    /** Save enables from EITHER identity anchor, independently. */
    @DataProvider(name = "saveGateFields")
    public Object[][] saveGateFields() {
        return new Object[][] {
                {"Manufacturer", "QA Gate Mfr"},
                {"Type / Catalog", "QA-CAT-42"},
        };
    }

    @Test(priority = 1002, dataProvider = "saveGateFields")
    public void TC_ENG_081_saveGateFromEitherIdentityField(String field, String value) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_081 [" + field + "] - Save enables from this identity field alone");

        // Fresh sheet per row so the other identity field is guaranteed blank.
        sheetHost = null;
        ensureFuseCustomSheet();
        assertFalse(engineerPage.isCustomSaveEnabled(),
                "Save must start DISABLED with a blank identity");
        engineerPage.typeCustomField(field, value);
        assertTrue(engineerPage.isCustomSaveEnabled(),
                "Save must ENABLE once '" + field + "' is non-blank");
        engineerPage.tapCustomSheetButton("Cancel");
        mediumWait();
        sheetHost = null;
    }

    @Test(priority = 1003)
    public void TC_ENG_082_freeFormSettingsSection() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_082 - Free-form Settings section with the Add Setting action (fuse branch)");

        ensureFuseCustomSheet();
        assertTrue(engineerPage.isEngineeringLabelPresent("Settings"),
                "'Settings' free-form section header must render for the fuse branch");
        assertTrue(engineerPage.isOptionShown("Add Setting"),
                "'Add Setting' action must render");
        logStepWithScreenshot("TC_ENG_082 verified");
    }

    @Test(priority = 1004)
    public void TC_ENG_083_addSettingRowAnatomy() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_083 - Add Setting row: Function picker (default Pickup), label + value fields");

        ensureFuseSheetWithSettingRow();
        assertTrue(engineerPage.isOptionShown("Pickup"),
                "new row's Function must default to 'Pickup'");
        assertTrue(engineerPage.isEngineeringFieldPresent("e.g. \"INST (Ii)\""),
                "row must offer the label field (placeholder e.g. \"INST (Ii)\")");
        assertTrue(engineerPage.isEngineeringFieldPresent("Value"),
                "row must offer the Value field");
        logStepWithScreenshot("TC_ENG_083 verified");
    }

    /** All 15 Function subtype options (verbatim from AppStrings). */
    @DataProvider(name = "functionOptions")
    public Object[][] functionOptions() {
        return new Object[][] {
                {"Time Delay"}, {"INST"}, {"INST Delay"}, {"INST Bands"},
                {"INST Tolerance"}, {"ST Delay (I²t)"}, {"Clearing Slope"},
                {"Clearing Bands"}, {"Iᵖt"}, {"Current Set"}, {"TCC Bands"},
                {"Equation 1"}, {"Equation 2"}, {"Equation 3"}, {"Pickup"},
        };
    }

    @Test(priority = 1005, dataProvider = "functionOptions")
    public void TC_ENG_084_functionOptionSelectable(String option) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_084 [" + option + "] - Function subtype selectable in the free-form row");

        ensureFuseSheetWithSettingRow();
        assertTrue(engineerPage.openFormPicker("Function"),
                "Function picker must open");
        assertTrue(engineerPage.pickOptionExact(option),
                "'" + option + "' must be pickable in the Function menu");
        assertTrue(engineerPage.waitForOptionShown(option, 6),
                "Function picker must display '" + option + "' after the pick");
    }

    /** I²t suffix options for ST Delay (I²t). */
    @DataProvider(name = "i2tOptions")
    public Object[][] i2tOptions() {
        return new Object[][] { {"On"}, {"Off"} };
    }

    @Test(priority = 1006, dataProvider = "i2tOptions")
    public void TC_ENG_085_i2tSuffixSelectable(String option) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_085 [" + option + "] - I²t suffix option selectable on ST Delay (I²t)");

        ensureFuseSheetWithSettingRow();
        assertTrue(engineerPage.openFormPicker("Function"), "Function picker must open");
        assertTrue(engineerPage.pickOptionExact("ST Delay (I²t)"), "ST Delay (I²t) must be pickable");
        assertTrue(engineerPage.waitForOptionShown("I²t", 6),
                "the I²t suffix picker must appear for ST Delay (I²t)");
        assertTrue(engineerPage.openFormPicker("I²t"), "I²t suffix picker must open");
        assertTrue(engineerPage.pickOptionExact(option),
                "'" + option + "' must be pickable in the I²t suffix menu");
    }

    /** Dial suffix options for INST Bands. */
    @DataProvider(name = "dialOptions")
    public Object[][] dialOptions() {
        return new Object[][] { {"R1"}, {"R2"}, {"R3"}, {"R4"}, {"R5"} };
    }

    @Test(priority = 1007, dataProvider = "dialOptions")
    public void TC_ENG_086_dialSuffixSelectable(String option) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_086 [" + option + "] - Dial suffix option selectable on INST Bands");

        ensureFuseSheetWithSettingRow();
        assertTrue(engineerPage.openFormPicker("Function"), "Function picker must open");
        assertTrue(engineerPage.pickOptionExact("INST Bands"), "INST Bands must be pickable");
        assertTrue(engineerPage.waitForOptionShown("Dial", 6),
                "the Dial suffix picker must appear for INST Bands");
        assertTrue(engineerPage.openFormPicker("Dial"), "Dial suffix picker must open");
        assertTrue(engineerPage.pickOptionExact(option),
                "'" + option + "' must be pickable in the Dial menu");
    }

    @Test(priority = 1008)
    public void TC_ENG_087_cancelDoesNotPersistSheetState() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_087 - Cancelled sheet state does not persist across reopen");

        sheetHost = null;
        ensureFuseCustomSheet();
        engineerPage.typeCustomField("Style / Model", "S-EPHEMERAL-1");
        engineerPage.tapCustomSheetButton("Cancel");
        mediumWait();
        sheetHost = null;

        ensureFuseCustomSheet();
        assertEquals(engineerPage.getEngineeringFieldValue("Style / Model"), "",
                "reopened sheet must start blank — cancelled input must not persist");
        logStepWithScreenshot("TC_ENG_087 verified");
    }

    // ═══════════ transformer branch (Add-Asset draft) ═══════════

    @Test(priority = 1010)
    public void TC_ENG_088_transformerSpecificationsSection() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_088 - Transformer custom sheet: Specifications section + connection defaults");

        openDraftOfClass("ransformer", "Transformer");
        openAddCustomFromDraft();

        assertTrue(engineerPage.isEngineeringLabelPresent("Transformer Specifications"),
                "'Transformer Specifications' section must render for transformer class");
        assertTrue(engineerPage.isEngineeringFieldPresent("kVA Rating"),
                "'kVA Rating' field must render");
        assertTrue(engineerPage.isEngineeringFieldPresent("Z (%)"),
                "'Z (%)' field must render");
        assertTrue(engineerPage.isEngineeringFieldPresent("Primary Voltage"),
                "'Primary Voltage' field must render");
        assertTrue(engineerPage.isEngineeringFieldPresent("Secondary Voltage"),
                "'Secondary Voltage' field must render");
        assertTrue(engineerPage.isOptionShown("Delta"),
                "Pri Connection must default to 'Delta'");
        assertTrue(engineerPage.isOptionShown("Wye-Ground"),
                "Sec Connection must default to 'Wye-Ground'");
        logStepWithScreenshot("TC_ENG_088 verified");
        engineerPage.tapCustomSheetButton("Cancel");
        mediumWait();
        sheetHost = null;
    }

    /** Pri Connection option set (EqpLibSelection.connectionOptions). */
    @DataProvider(name = "connectionOptions")
    public Object[][] connectionOptions() {
        return new Object[][] {
                {"Wye"}, {"Wye-Ground-Resistor"}, {"Wye-Ground"}, {"Delta"},
        };
    }

    @Test(priority = 1011, dataProvider = "connectionOptions")
    public void TC_ENG_089_priConnectionOptionSelectable(String option) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_089 [" + option + "] - Pri Connection option selectable");

        ensureTransformerDraftSheet();
        assertTrue(engineerPage.openFormPicker("Pri Connection"),
                "Pri Connection picker must open");
        assertTrue(engineerPage.pickOptionExact(option),
                "'" + option + "' must be pickable for Pri Connection");
        assertTrue(engineerPage.waitForOptionShown(option, 6),
                "Pri Connection must display '" + option + "'");
    }

    private void ensureTransformerDraftSheet() {
        if ("xfmr-draft".equals(sheetHost) && engineerPage.isCustomSheetOpen(1)) return;
        openDraftOfClass("ransformer", "Transformer");
        openAddCustomFromDraft();
        sheetHost = "xfmr-draft";
    }

    // ═══════════ busway branch (Add-Asset draft) ═══════════

    @Test(priority = 1012)
    public void TC_ENG_090_buswayCableSpecificationsDefaults() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_090 - Busway custom sheet: Cable Specifications with busway defaults");

        openDraftOfClass("usway", "Busway");
        // Busway matcher requires a conductor material before the panel/sheet.
        engineerPage.tapSegment("Cu");
        mediumWait();
        openAddCustomFromDraft();

        assertTrue(engineerPage.isEngineeringLabelPresent("Cable Specifications"),
                "'Cable Specifications' section must render for busway class");
        assertEquals(engineerPage.getEngineeringFieldValue("Conductor Type"), "Copper",
                "Conductor Type must default to 'Copper'");
        assertTrue(engineerPage.isEngineeringFieldPresent("Size (Amps)"),
                "busway variant must use the 'Size (Amps)' field");
        assertEquals(engineerPage.getEngineeringFieldValue("Duct Material"), "Bus",
                "busway Duct Material must default to 'Bus'");
        assertEquals(engineerPage.getEngineeringFieldValue("Length (ft)"), "100",
                "Length (ft) must default to '100'");
        logStepWithScreenshot("TC_ENG_090 verified");
        engineerPage.tapCustomSheetButton("Cancel");
        mediumWait();
        sheetHost = null;
    }

    @Test(priority = 1013)
    public void TC_ENG_091_cableCableSpecificationsDefaults() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_091 - Cable custom sheet: Cable Specifications with cable defaults");

        openDraftOfClass("able", "Cable");
        engineerPage.tapSegment("Cu");
        mediumWait();
        openAddCustomFromDraft();

        assertTrue(engineerPage.isEngineeringLabelPresent("Cable Specifications"),
                "'Cable Specifications' section must render for cable class");
        assertTrue(engineerPage.isEngineeringFieldPresent("Cable Size"),
                "cable variant must use the 'Cable Size' field (typeable '4/0')");
        assertEquals(engineerPage.getEngineeringFieldValue("Duct Material"), "Magnetic",
                "cable Duct Material must default to 'Magnetic'");
        logStepWithScreenshot("TC_ENG_091 verified");
        engineerPage.tapCustomSheetButton("Cancel");
        mediumWait();
        sheetHost = null;
    }

    // ── shared ─────────────────────────────────────────────────────────

    /** Open the Add Custom sheet from an Add-Asset draft's match panel. */
    private void openAddCustomFromDraft() {
        if (!engineerPage.isAddCustomButtonShown()) {
            skipIfPreconditionMissing(() -> engineerPage.isAddCustomButtonShown(),
                    "no 'Add Custom' chip on this draft's match panel — layout changed");
        }
        engineerPage.tapAddCustom();
        assertTrue(engineerPage.isCustomSheetOpen(8),
                "'Add Custom Equipment' sheet must open from the draft's match panel");
    }
}
