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
 * asset_engineer library — cable/busway field pickers, transformer
 * configurator interactions and voltage picker sheets (gaps G9+G11+G12).
 *
 * App truth (v1.49 source):
 *  - NodeEngineeringSection.swift cableBlock (:1573-1627): Conductor
 *    Description / Insulation Class / Insulation Type / Installation /
 *    Duct Material are EngineeringEnumPickers (ModernPicker: Menu ≤20
 *    items, searchable sheet above; allowClear=true → every surface has
 *    a "None" clear row). The "Size" row is GATED on ``node.eqp_lib``
 *    being set — it must NOT render in an unbound draft.
 *  - NodeEngineeringSection.swift buswayBlock (:1629-1664): "Busway Size
 *    (Amps)", "Insulation" (is_busway classes), "Construction" (is_busway
 *    installations). "Conductor Configuration" is intentionally NOT
 *    surfaced for busway (set silently by the matcher — web parity).
 *  - EngineeringConfigCards.swift TransformerConfigCard (:33-166): only
 *    renders once a library row is BOUND. Its kVA ModernPicker re-pins
 *    draft.kva_rating and draft.percent_impedance (sqrt(r²+x²)) on pick,
 *    which the transformerBlock's mirrored text fields display
 *    (placeholders "Enter kva rating" / "Enter % impedance" —
 *    live-verified by AssetEngineerInputs_Test). Primary/Secondary
 *    Connection ConnectionPickers offer Delta / Wye / Wye-Ground /
 *    Wye-Ground-Resistor and default to Delta / Wye-Ground after a bind.
 *  - VoltagePickerField.swift: draft Primary/Secondary Voltage rows are
 *    tap-to-open SHEET pickers (17 fixed options "120V".."69kV", Cancel
 *    toolbar, no search/Done). Per ZP-2397 the whole voltage stack HIDES
 *    once a library row is bound; the locked read-only "System Voltage"
 *    row exists only on downstream DETAILS screens, never in a draft.
 *
 * Everything runs inside CANCELLED detailed Add-Asset drafts (fixture
 * guards keep a draft open across DataProvider rows, CustomSheet-style);
 * nothing is ever saved and "Create Main" is never touched. Transformer
 * BIND tests sit last in the class (same small-DOM draft surface as
 * TC_ENG_114/115 — the documented WDA wedge lives on transformer DETAILS,
 * not ~67KB drafts).
 */
public class AssetEngineerFieldPickers_Test extends BaseTest {

    private AssetEngineerPage engineerPage;
    private boolean libraryChecked = false;
    /** Exact class name of the currently-open detailed draft (null = none). */
    private String openDraftClass = null;
    /** The open transformer draft has a bound library row + configurator. */
    private boolean transformerBound = false;

    /**
     * The SKM library gate — match panels (and therefore the transformer
     * bind fixture) do not render at all until the library is cached.
     * Gate once per class run (CustomSheet pattern).
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
    public void fieldPickersSetup() {
        System.out.println("\n📋 Asset Engineer — cable/busway field pickers + transformer configurator + voltage sheets");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void fieldPickersTestSetup() {
        if (!DriverManager.isDriverActive()) return; // gated/fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void fieldPickersTeardown() {
        try {
            engineerPage = new AssetEngineerPage();
            engineerPage.closeAssetDetails(true);
        } catch (Exception ignored) {
        }
        DriverManager.resetNoResetOverride();
    }

    // ── fixtures ───────────────────────────────────────────────────────

    private void resetToAssetsList() {
        try {
            engineerPage.closeAssetDetails(true);
        } catch (Exception ignored) { }
        openDraftClass = null;
        transformerBound = false;
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
    }

    /**
     * Detailed Add-Asset draft of the given class (search-picked, class
     * picker searched with the FIRST word only per the locator contract).
     * Reuses the currently-open draft when it is already the right class.
     */
    private void ensureDraftOfClass(String classSearch, String classExact) {
        if (classExact.equals(openDraftClass) && engineerPage.isEngineeringSectionPresent()) return;
        ensureReadyOnce();
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
        final boolean picked = engineerPage.pickOptionExact(classExact);
        skipIfPreconditionMissing(() -> picked, "class '" + classExact + "' not available in this site's class list");
        mediumWait();
        assertTrue(engineerPage.waitForEngineeringSection(8),
                "[" + classExact + "] Engineering section must mount in the detailed draft");
        openDraftClass = classExact;
        transformerBound = false;
    }

    /**
     * Transformer draft with a library row BOUND (TC_ENG_115 flow) so the
     * Transformer Configuration card is mounted. Small-DOM draft surface —
     * the wedging match-render is a DETAILS-screen defect (changelog 107).
     */
    private void ensureBoundTransformerDraft() {
        if (transformerBound && engineerPage.isEngineeringLabelPresent("Transformer Configuration")) return;
        ensureDraftOfClass("Transformer", "Transformer");
        engineerPage.openEngineeringPickerBelowLabel("Manufacturer");
        final boolean picked = engineerPage.pickOptionExact("SCHNEIDER/SQUARE D")
                || engineerPage.pickOptionExact("Generic");
        skipIfPreconditionMissing(() -> picked, "no library-backed transformer manufacturer in the menu");
        assertTrue(engineerPage.waitForMatchHeader(12), "match panel must render after the manufacturer pick");
        skipIfPreconditionMissing(() -> engineerPage.getMatchCount() > 0,
                "manufacturer has zero transformer matches in this library snapshot");
        String card = engineerPage.tapFirstMatchCard();
        assertNotNull(card, "a match card must be tappable in the transformer match list");
        assertTrue(engineerPage.waitForOptionShown("Unlink", 10),
                "'Unlink' must appear once the library row is linked");
        assertTrue(engineerPage.isBoundCardShown(),
                "'Library Matched' bound card must render after tapping the list entry");
        assertTrue(engineerPage.isEngineeringLabelPresent("Transformer Configuration"),
                "the Transformer Configuration card must render below the bound card");
        transformerBound = true;
        System.out.println("ℹ️ bound transformer card: " + card);
    }

    // ═══════════ (A)+(B) cable & busway enum field pickers ═══════════

    /**
     * {classSearch, classExact, fieldLabel} — one row per unbound-draft
     * enum picker. Cable rows from cableBlock :1573-1627; busway rows from
     * buswayBlock :1629-1664 (class-matrix Busway row only pins "Conductor
     * Material", so the busway labels here come straight from AppStrings).
     */
    @DataProvider(name = "fieldPickerRoundTrips")
    public Object[][] fieldPickerRoundTrips() {
        return new Object[][] {
                {"Cable", "Cable", "Conductor Description"},
                {"Cable", "Cable", "Insulation Class"},
                {"Cable", "Cable", "Insulation Type"},
                {"Cable", "Cable", "Installation"},
                {"Cable", "Cable", "Duct Material"},
                {"Busway", "Busway", "Busway Size (Amps)"},
                {"Busway", "Busway", "Insulation"},
                {"Busway", "Busway", "Construction"},
        };
    }

    @Test(priority = 1301, dataProvider = "fieldPickerRoundTrips")
    public void TC_ENG_180_fieldPickerOpensAndNoneRoundTrips(String classSearch, String classExact, String label) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_180 [" + classExact + " / " + label + "] - Enum field picker opens and the None clear row round-trips");

        logStep("Step 1: Detailed " + classExact + " draft with the Engineering section mounted");
        ensureDraftOfClass(classSearch, classExact);

        logStep("Step 2: Class-specific gate rows (app-truth negatives)");
        if ("Cable".equals(classExact)) {
            // cableBlock gates "Size" on node.eqp_lib — unbound draft must not render it.
            assertFalse(engineerPage.isEngineeringLabelPresent("Size"),
                    "[Cable] the 'Size' row is gated on a bound library row and must NOT render in an unbound draft");
        } else {
            // buswayBlock intentionally never surfaces Conductor Configuration (matcher-set, web parity).
            assertFalse(engineerPage.isEngineeringLabelPresent("Conductor Configuration"),
                    "[Busway] 'Conductor Configuration' must NOT be surfaced for busway (matcher sets it silently)");
        }

        logStep("Step 3: '" + label + "' label is present and reachable");
        assertTrue(engineerPage.scrollToEngineeringLabel(label),
                "[" + classExact + "] engineering label '" + label + "' must be reachable in the draft");

        logStep("Step 4: Open the picker — the None clear row proves a real option surface");
        // Establish there is no background 'None' first, so the post-open
        // probe cannot pass off stale form content as an open menu.
        assertFalse(engineerPage.isOptionShown("None"),
                "[" + classExact + " / " + label + "] no 'None' element may pre-exist on the form (probe precondition)");
        engineerPage.openEngineeringPickerBelowLabel(label);
        assertTrue(engineerPage.waitForOptionShown("None", 6),
                "[" + classExact + " / " + label + "] picker must open with its 'None' clear row (menu ≤20 items or searchable sheet)");

        logStep("Step 5: Pick None — chip stays unset and the overlay closes");
        assertTrue(engineerPage.pickOptionExact("None"),
                "[" + classExact + " / " + label + "] the 'None' clear row must be tappable");
        assertTrue(engineerPage.waitForPickerValueBelowLabel(label, AssetEngineerPage.SELECT_ELLIPSIS, 6),
                "[" + classExact + " / " + label + "] chip must show 'Select…' after None (now='"
                        + engineerPage.getPickerValueBelowLabel(label) + "')");
        assertTrue(engineerPage.waitForLabelGone("None", 6),
                "[" + classExact + " / " + label + "] the option overlay must close after the None pick");
        logStepWithScreenshot("TC_ENG_180 [" + classExact + " / " + label + "] round-trip verified");
    }

    // ═══════════ (D) voltage picker sheets — unbound transformer draft ═══════════

    @Test(priority = 1310)
    public void TC_ENG_186_transformerDraftVoltageRowsEditableAndSheetCancelsCleanly() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_186 - Unbound transformer draft: Primary+Secondary voltage rows are editable sheet pickers, no System-Voltage lock");

        logStep("Step 1: Detailed Transformer draft (unbound)");
        ensureDraftOfClass("Transformer", "Transformer");

        logStep("Step 2: Both voltage rows render (primary_secondary_voltage stack)");
        assertTrue(engineerPage.scrollToEngineeringLabel("Primary Voltage"),
                "'Primary Voltage' label must render in the unbound transformer draft");
        assertTrue(engineerPage.isEngineeringLabelPresent("Secondary Voltage"),
                "'Secondary Voltage' label must render next to Primary Voltage");

        logStep("Step 3: No locked System-Voltage row in a DRAFT (lock is a downstream-details law)");
        assertFalse(engineerPage.isSystemVoltageLockedRowShown(),
                "the read-only locked 'System Voltage' row must NOT render in an Add-Asset draft");

        logStep("Step 4: Tap the unset voltage row — the 17-option sheet opens");
        // Both rows show 'Select voltage'; the last one in the tree is the
        // Secondary (right) field — VoltagePickerField rows are tap-target
        // HStacks, not Buttons, so the generic below-label opener does not apply.
        assertTrue(engineerPage.pickOptionExact("Select voltage"),
                "the unset 'Select voltage' row must be tappable");
        assertTrue(engineerPage.waitForOptionShown("120V", 6),
                "voltage sheet must open and list its first option '120V'");
        assertTrue(engineerPage.isOptionShown("120/208V"),
                "voltage sheet must list the composite option '120/208V'");

        logStep("Step 5: Cancel cleanly — sheet closes, nothing selected");
        assertTrue(engineerPage.pickOptionExact("Cancel"), "sheet 'Cancel' must be tappable");
        assertTrue(engineerPage.waitForLabelGone("120V", 6),
                "voltage sheet must close after Cancel ('120V' still on screen)");
        assertTrue(engineerPage.isOptionShown("Select voltage"),
                "voltage rows must remain unset after a cancelled sheet");
        logStepWithScreenshot("TC_ENG_186 verified");
    }

    /** Fixed VoltageOption table rows (VoltagePickerField.swift) — top of the 17-entry list. */
    @DataProvider(name = "voltageOptions")
    public Object[][] voltageOptions() {
        return new Object[][] {
                {"120V"}, {"120/208V"}, {"120/240V"}, {"208V"}, {"240V"}, {"277V"},
        };
    }

    @Test(priority = 1311, dataProvider = "voltageOptions")
    public void TC_ENG_187_voltageSheetListsStandardOption(String option) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_187 [" + option + "] - Voltage sheet lists the standard enum_node_voltages option");

        logStep("Step 1: Unbound transformer draft, voltage row visible");
        ensureDraftOfClass("Transformer", "Transformer");
        assertTrue(engineerPage.scrollToEngineeringLabel("Secondary Voltage"),
                "'Secondary Voltage' row must be reachable");
        // Probe precondition: the option text must not pre-exist on the form,
        // so its later presence can only come from the open sheet.
        assertFalse(engineerPage.isOptionShown(option),
                "'" + option + "' must not pre-exist on the draft form (probe precondition)");

        logStep("Step 2: Open the voltage sheet and find '" + option + "'");
        assertTrue(engineerPage.pickOptionExact("Select voltage"),
                "the unset 'Select voltage' row must be tappable");
        assertTrue(engineerPage.waitForOptionShown(option, 6),
                "voltage sheet must list the standard option '" + option + "'");

        logStep("Step 3: Cancel — no selection leaks onto the draft");
        assertTrue(engineerPage.pickOptionExact("Cancel"), "sheet 'Cancel' must be tappable");
        assertTrue(engineerPage.waitForLabelGone(option, 6),
                "'" + option + "' must disappear with the cancelled sheet (no selection leaked)");
    }

    // ═══════════ (C) transformer configurator — bound draft ═══════════

    @Test(priority = 1320)
    public void TC_ENG_190_kvaTierPickRepinsImpedanceMirrors() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_190 - Bound transformer configurator: kVA tier pick re-pins % Impedance + kVA Rating mirrors");

        logStep("Step 1: Transformer draft with a bound library row (configurator mounted)");
        ensureBoundTransformerDraft();

        logStep("Step 2: ZP-2397 — the voltage stack hides once a library row is bound");
        assertTrue(engineerPage.waitForLabelGone("Primary Voltage", 8),
                "'Primary Voltage' row must hide after a library bind (library pins the voltage rating)");
        assertFalse(engineerPage.isEngineeringLabelPresent("Secondary Voltage"),
                "'Secondary Voltage' row must hide after a library bind");

        logStep("Step 3: Open the kVA tier picker (first control below the card title)");
        assertTrue(engineerPage.scrollToEngineeringLabel("Transformer Configuration"),
                "'Transformer Configuration' card title must be reachable");
        // The kVA ModernPicker is the nearest control below the card title.
        String chipBefore = engineerPage.getPickerValueBelowLabel("Transformer Configuration");
        System.out.println("ℹ️ kVA chip before pick: '" + chipBefore + "'");
        engineerPage.openEngineeringPickerBelowLabel("Transformer Configuration");

        logStep("Step 4: Pick a kVA tier (bound row's own tier first, then common SKM tiers)");
        String pickedTier = null;
        if (chipBefore.matches("\\d+ kVA") && engineerPage.pickOptionExact(chipBefore)) {
            pickedTier = chipBefore; // re-pick fires the set closure → re-pins %Z
        } else {
            for (String tier : new String[] {"500 kVA", "300 kVA", "750 kVA", "1000 kVA", "150 kVA", "225 kVA"}) {
                if (engineerPage.pickOptionExact(tier)) {
                    pickedTier = tier;
                    break;
                }
            }
        }
        final String picked = pickedTier;
        skipIfPreconditionMissing(() -> picked != null,
                "no pickable kVA tier in this bound row's entry list — capture the menu and extend the tier list");
        assertTrue(engineerPage.waitForPickerValueBelowLabel("Transformer Configuration", picked, 8),
                "kVA chip must display '" + picked + "' after the pick (now='"
                        + engineerPage.getPickerValueBelowLabel("Transformer Configuration") + "')");

        logStep("Step 5: The pick re-pins the first-class draft mirrors (kVA Rating + % Impedance)");
        String kvaMirror = engineerPage.getEngineeringFieldValue("Enter kva rating");
        assertFalse(kvaMirror.isEmpty(),
                "'kVA Rating' mirror field must be re-pinned after picking '" + picked + "' (still empty)");
        String zMirror = engineerPage.getEngineeringFieldValue("Enter % impedance");
        assertFalse(zMirror.isEmpty(),
                "'% Impedance' must be re-pinned (sqrt(r²+x²)) after picking '" + picked + "' (still empty)");
        logStepWithScreenshot("TC_ENG_190 picked='" + picked + "' kva='" + kvaMirror + "' z='" + zMirror + "'");
    }

    /**
     * {pickerTitle, optionToPick} — every pick differs from the chip's
     * deterministic prior state (bind defaults Delta / Wye-Ground, then the
     * rows advance it), so the chip TRANSITION proves the menu interaction.
     */
    @DataProvider(name = "connectionPicks")
    public Object[][] connectionPicks() {
        return new Object[][] {
                {"Primary Connection", "Wye"},
                {"Primary Connection", "Wye-Ground-Resistor"},
                {"Secondary Connection", "Delta"},
                {"Secondary Connection", "Wye"},
        };
    }

    @Test(priority = 1321, dataProvider = "connectionPicks")
    public void TC_ENG_192_connectionPickerSelects(String pickerTitle, String option) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_192 [" + pickerTitle + " → " + option + "] - Configurator connection picker selects and renders on the chip");

        logStep("Step 1: Bound transformer configurator");
        ensureBoundTransformerDraft();
        assertTrue(engineerPage.scrollToEngineeringLabel(pickerTitle),
                "'" + pickerTitle + "' picker must be reachable in the configurator card");

        logStep("Step 2: Chip must currently show a DIFFERENT connection (transition-proof)");
        String before = engineerPage.getPickerValueBelowLabel(pickerTitle);
        assertTrue(!option.equals(before),
                "precondition: '" + pickerTitle + "' chip must not already show '" + option + "' (now='" + before + "')");

        logStep("Step 3: Open the picker and select '" + option + "'");
        engineerPage.openEngineeringPickerBelowLabel(pickerTitle);
        assertTrue(engineerPage.pickOptionExact(option),
                "'" + option + "' must be pickable in the " + pickerTitle + " menu");
        assertTrue(engineerPage.waitForPickerValueBelowLabel(pickerTitle, option, 6),
                "'" + pickerTitle + "' chip must display '" + option + "' after the pick (now='"
                        + engineerPage.getPickerValueBelowLabel(pickerTitle) + "')");
        logStepWithScreenshot("TC_ENG_192 [" + pickerTitle + "] '" + before + "' → '" + option + "'");
    }
}
