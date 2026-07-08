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
 * asset_engineer library — SkmTripConfigCard breaker configurator (gaps G2+G5+G6).
 *
 * App truth (v1.49, app-source):
 *  - NodeEngineeringSection.swift breaker branch (~line 617): unbound OCP
 *    inputs are Pole Count / Type / Manufacturer / Frame Amps, plus a
 *    MUTUALLY-EXCLUSIVE pair split on has_trip_unit — Trip Amps
 *    (fixed-trip) XOR Sensor Amps + Plug Amps (trip-unit). [G6]
 *  - Binding a match (applyPickedMatch) cascade-resolves frame/sensor/
 *    plug/trip-unit AND backfills the first-class draft columns
 *    (frame_amps from cont_current, pole_count ?? 3, ...). [G5]
 *  - While bound, the ENTIRE input stack is hidden — only the bound card
 *    + configurator cards render (NodeEngineeringSection body ~line 267).
 *  - SkmTripConfigCard (EngineeringConfigCards.swift): header
 *    "Frame / Trip Configuration"; breaker slugs render Frame + a sensor
 *    tier row labeled "Sensor" (SST) or "Trip" (thermal-mag/MCP) + "Plug";
 *    the trip unit is NEVER user-facing (tripUnitPicker exists but is
 *    never invoked — auto-resolved from the frame's f_size); the segments
 *    editor header inside the card is "Settings" (engineering.settings) —
 *    "Trip Settings" belongs to the custom sheet only.
 *  - unbindLibrary() (NodeEngineeringSection ~line 960) clears only the
 *    binding: "First-class fields stay populated".
 *
 * Every flow runs inside ONE chained detailed Circuit Breaker Add-Asset
 * draft (unbound → bound → unlinked); nothing is ever saved — the final
 * test and the class teardown discard the draft.
 */
public class AssetEngineerTripConfig_Test extends BaseTest {

    private AssetEngineerPage engineerPage;
    private boolean libraryChecked = false;
    /** Chained draft state: null | "unbound" | "bound" | "unlinked". */
    private String draftPhase = null;

    /** Whole module is gated on the platform-managed eng-lib company flag (BaseTest skips pre-driver when absent). */
    @Override
    protected String requiredCompanyFeature() {
        return "eng-lib";
    }

    @BeforeClass(alwaysRun = true)
    public void tripConfigSetup() {
        System.out.println("\n📋 Asset Engineer — breaker Frame/Trip configurator (SkmTripConfigCard)");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void tripConfigTestSetup() {
        if (!DriverManager.isDriverActive()) return; // gated/fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void tripConfigTeardown() {
        try {
            engineerPage = new AssetEngineerPage();
            engineerPage.closeAssetDetails(true);
        } catch (Exception e) {
            System.out.println("⚠️ trip-config teardown: " + e.getMessage());
        }
        DriverManager.resetNoResetOverride();
    }

    // ── fixtures ───────────────────────────────────────────────────────

    /**
     * The match panel does not render at all until the SKM library is
     * cached — on a fresh container every fixture here would die at the
     * manufacturer pick. Gate once per class run (CustomSheet pattern).
     */
    private void ensureLibraryReadyOnce() {
        if (libraryChecked) return;
        loginAndSelectSite();
        engineerPage.ensureLibraryDownloaded(600);
        libraryChecked = true;
    }

    /** Fresh detailed Circuit Breaker Add-Asset draft (always rebuilds). */
    private void buildBreakerDraft() {
        ensureLibraryReadyOnce();
        try {
            engineerPage.closeAssetDetails(true);
        } catch (Exception ignored) { }
        draftPhase = null;
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        mediumWait();
        assertTrue(engineerPage.pickOptionExact("Detailed"), "'Detailed' toggle must be tappable");
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
        assertTrue(engineerPage.waitForEngineeringSection(10),
                "Engineering section must render in the detailed breaker draft");
        draftPhase = "unbound";
    }

    /** Unbound breaker draft — reuses the live one when still intact. */
    private void ensureUnboundBreakerDraft() {
        if ("unbound".equals(draftPhase) && engineerPage.isEngineeringSectionPresent()) return;
        buildBreakerDraft();
    }

    /**
     * Bound breaker draft: manufacturer pick fires the OCP matcher, then a
     * tap on the first match card binds it (TC_ENG_113/115 laws). Rebuilds
     * from scratch unless the chained draft is already bound — the pre-bind
     * tests scroll the form, and the swipe helpers only travel downward.
     */
    private void ensureBoundBreakerDraft() {
        if ("bound".equals(draftPhase) && engineerPage.isBoundCardShown()) return;
        buildBreakerDraft();
        engineerPage.openEngineeringPickerBelowLabel("Manufacturer");
        final boolean picked = engineerPage.pickOptionExact("Generic")
                || engineerPage.pickOptionExact("SCHNEIDER/SQUARE D")
                || engineerPage.pickOptionExact("ABB");
        skipIfPreconditionMissing(() -> picked,
                "no library-backed breaker manufacturer in the menu — capture the menu and extend the list");
        assertTrue(engineerPage.waitForMatchHeader(12),
                "match header must render after the manufacturer pick");
        String header = engineerPage.getMatchHeaderText();
        assertTrue(header.matches("\\d+\\+? possible match(es)?|No possible matches"),
                "header must follow the '{n}[+] possible match(es)' format; got '" + header + "'");
        skipIfPreconditionMissing(() -> engineerPage.getMatchCount() > 0,
                "manufacturer has zero breaker matches in this library snapshot (header='" + header + "')");
        String card = engineerPage.tapFirstMatchCard();
        assertNotNull(card, "a match card must be tappable below the header to bind the draft");
        System.out.println("ℹ️ bound match card: " + card);
        assertTrue(engineerPage.waitForOptionShown("Unlink", 10),
                "'Unlink' must appear once the library row is linked");
        assertTrue(engineerPage.isBoundCardShown(),
                "'Library Matched' bound card must render after tapping the match entry — gate into the configurator");
        draftPhase = "bound";
    }

    /** Bound draft that has been Unlinked in place (fields must survive). */
    private void ensureUnlinkedBreakerDraft() {
        if ("unlinked".equals(draftPhase) && engineerPage.isEngineeringSectionPresent()) return;
        ensureBoundBreakerDraft();
        assertTrue(engineerPage.tapUnlink(), "Unlink must remove the draft binding");
        draftPhase = "unlinked";
    }

    // ═══════════ pre-bind: breaker OCP input stack (G6) ═══════════

    /** Breaker OCP input labels in form order (NodeEngineeringSection default branch). */
    @DataProvider(name = "preBindInputLabels")
    public Object[][] preBindInputLabels() {
        return new Object[][] {
                {"Pole Count"}, {"Type"}, {"Manufacturer"}, {"Frame Amps"},
        };
    }

    @Test(priority = 1301, dataProvider = "preBindInputLabels")
    public void TC_ENG_140_breakerOcpInputStackRenders(String label) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_140 [" + label + "] - Unbound breaker draft renders this OCP input row");

        logStep("Step 1: Unbound detailed Circuit Breaker draft");
        ensureUnboundBreakerDraft();

        logStep("Step 2: '" + label + "' row must be reachable in the input stack");
        assertTrue(engineerPage.scrollToEngineeringLabel(label),
                "'" + label + "' must render in the unbound breaker OCP input stack"
                        + " (NodeEngineeringSection default device_role=2 branch)");
        logStepWithScreenshot("TC_ENG_140 [" + label + "] verified");
    }

    @Test(priority = 1302)
    public void TC_ENG_141_tripAmpsVsSensorPlugMutualExclusion() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_141 - Trip Amps XOR (Sensor Amps + Plug Amps): the has_trip_unit split is exclusive");

        logStep("Step 1: Unbound breaker draft, Frame Amps region in view");
        ensureUnboundBreakerDraft();
        assertTrue(engineerPage.scrollToEngineeringLabel("Frame Amps"),
                "'Frame Amps' must render for the circuit_breaker eqp_lib type");

        logStep("Step 2: Exactly one amperage branch renders (G6 law)");
        boolean tripAmps = engineerPage.isEngineeringLabelPresent("Trip Amps");
        boolean sensorAmps = engineerPage.isEngineeringLabelPresent("Sensor Amps");
        boolean plugAmps = engineerPage.isEngineeringLabelPresent("Plug Amps");
        System.out.println("ℹ️ amperage split: tripAmps=" + tripAmps
                + " sensorAmps=" + sensorAmps + " plugAmps=" + plugAmps);
        if (tripAmps) {
            assertFalse(sensorAmps,
                    "mutual exclusion violated: 'Trip Amps' AND 'Sensor Amps' both render — the"
                            + " has_trip_unit split (NodeEngineeringSection ~line 631) leaked both branches");
            assertFalse(plugAmps,
                    "mutual exclusion violated: 'Trip Amps' AND 'Plug Amps' both render");
            logStep("fixed-trip branch active: Trip Amps only");
        } else {
            assertTrue(sensorAmps,
                    "neither 'Trip Amps' nor 'Sensor Amps' rendered — the has_trip_unit split lost both branches");
            assertTrue(plugAmps,
                    "'Sensor Amps' without 'Plug Amps' — the trip-unit branch must render the pair");
            logStep("trip-unit branch active: Sensor Amps + Plug Amps pair");
        }
        logStepWithScreenshot("TC_ENG_141 verified (tripAmps=" + tripAmps + ")");
    }

    // ═══════════ bind: the configurator card (G2) ═══════════

    @Test(priority = 1303)
    public void TC_ENG_142_bindingRendersTripConfigCard() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_142 - Binding a breaker match renders the 'Frame / Trip Configuration' card");

        logStep("Step 1: Bind the draft via manufacturer pick + first match card");
        ensureBoundBreakerDraft();

        logStep("Step 2: 'Frame / Trip Configuration' card renders below the bound card");
        assertTrue(engineerPage.isTripConfigCardShown(),
                "'" + AssetEngineerPage.TRIP_CONFIG_HEADER + "' card must render for a bound breaker"
                        + " (supportedCategory covers mccb/iccb/pcb slugs)");
        assertTrue(engineerPage.scrollToEngineeringLabel(AssetEngineerPage.TRIP_CONFIG_HEADER),
                "the trip config card header must be scrollable into view");
        logStepWithScreenshot("TC_ENG_142 configurator card verified");
    }

    @Test(priority = 1304)
    public void TC_ENG_143_frameRowAutoResolvedAndRepickable() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_143 - Frame row is auto-resolved by the bind (G5 backfill) and its picker reopens");

        logStep("Step 1: Bound breaker draft with the Frame row in view");
        ensureBoundBreakerDraft();
        assertTrue(engineerPage.scrollToEngineeringLabel("Frame"),
                "'Frame' row must render inside the trip config card for a breaker slug");

        logStep("Step 2: Frame chip carries the cascade-resolved rating (never unset)");
        String frameChip = engineerPage.getPickerValueBelowLabel("Frame");
        assertFalse(frameChip.isEmpty(),
                "a picker chip must sit below the 'Frame' row; reader returned ''");
        assertFalse(AssetEngineerPage.SELECT_ELLIPSIS.equals(frameChip)
                        || frameChip.equalsIgnoreCase("Select frame"),
                "binding must auto-resolve the frame (applyPickedMatch cascade) — chip still unset: '"
                        + frameChip + "'");
        assertTrue(frameChip.matches(".*\\d.*"),
                "frame chip must carry the ampere-ish rating ('{sz_name} {amps}A @ {volts}V'); got '"
                        + frameChip + "'");

        logStep("Step 3: Reopen the Frame picker — the current option must be listed; re-pick it");
        engineerPage.openEngineeringPickerBelowLabel("Frame");
        boolean sheetMode = engineerPage.isSheetPickerOpen(3);
        System.out.println("ℹ️ frame picker mode: " + (sheetMode ? "sheet (>8 frames)" : "menu (≤8 frames)"));
        if (sheetMode) {
            try {
                engineerPage.searchInSheetPicker(frameChip.split(" ")[0]);
            } catch (Exception e) {
                System.out.println("⚠️ frame sheet search: " + e.getMessage());
            }
        }
        assertTrue(engineerPage.pickOptionExact(frameChip),
                "the currently-selected frame '" + frameChip + "' must be listed and pickable in the reopened picker");
        assertTrue(engineerPage.waitForPickerValueBelowLabel("Frame", frameChip, 8),
                "Frame chip must retain '" + frameChip + "' after re-picking the same option");
        logStepWithScreenshot("TC_ENG_143 frame='" + frameChip + "' (sheetMode=" + sheetMode + ")");
    }

    @Test(priority = 1305)
    public void TC_ENG_144_sensorAndPlugRowsRenderTripUnitNever() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_144 - Sensor tier ('Sensor'/'Trip') + 'Plug' rows render; 'Trip Unit' is never user-facing");

        logStep("Step 1: Bound breaker draft, trip config card in view");
        ensureBoundBreakerDraft();
        engineerPage.scrollToEngineeringLabel(AssetEngineerPage.TRIP_CONFIG_HEADER);

        logStep("Step 2: Sensor tier row — 'Sensor' for SST, 'Trip' for thermal-mag/MCP");
        boolean sstLabel = engineerPage.isEngineeringLabelPresent("Sensor");
        boolean thermalMagLabel = engineerPage.isEngineeringLabelPresent("Trip");
        assertTrue(sstLabel || thermalMagLabel,
                "the sensor tier row must render as 'Sensor' (use_sst) or 'Trip' (thermal-mag/MCP)"
                        + " for breaker slugs — got neither (sensorPicker, EngineeringConfigCards.swift)");
        String sensorRow = sstLabel ? "Sensor" : "Trip";
        System.out.println("ℹ️ sensor tier row label: '" + sensorRow + "'");

        logStep("Step 3: 'Plug' row renders (breakers are non-relay/non-fuse)");
        assertTrue(engineerPage.isEngineeringLabelPresent("Plug"),
                "'Plug' row must render in the trip config card for breaker slugs");

        logStep("Step 4: Both rows expose a picker chip (resolved value or 'Select …' placeholder)");
        engineerPage.scrollToEngineeringLabel(sensorRow);
        String sensorChip = engineerPage.getPickerValueBelowLabel(sensorRow);
        assertFalse(sensorChip.isEmpty(),
                "a picker chip must sit below the '" + sensorRow + "' row; reader returned ''");
        engineerPage.scrollToEngineeringLabel("Plug");
        String plugChip = engineerPage.getPickerValueBelowLabel("Plug");
        assertFalse(plugChip.isEmpty(),
                "a picker chip must sit below the 'Plug' row; reader returned ''");
        System.out.println("ℹ️ chips: " + sensorRow + "='" + sensorChip + "' Plug='" + plugChip
                + "' (an unresolved partial-search pick legitimately shows the 'Select …' placeholder;"
                + " both rows also disable when the tier has ≤1 option — presence is the law here)");

        logStep("Step 5: 'Trip Unit' must NOT render — auto-resolved from the frame, never a row");
        assertFalse(engineerPage.isEngineeringLabelPresent("Trip Unit"),
                "'Trip Unit' rendered as a user-facing row — app truth says tripUnitPicker is never"
                        + " invoked (auto-resolved from frame f_size on every frame change)");
        logStepWithScreenshot("TC_ENG_144 " + sensorRow + "='" + sensorChip + "' Plug='" + plugChip + "'");
    }

    @Test(priority = 1306)
    public void TC_ENG_145_segmentsEditorRendersInsideCard() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_145 - Trip settings segments editor renders inside the configurator card when segments exist");

        logStep("Step 1: Bound breaker draft, trip config card in view");
        ensureBoundBreakerDraft();
        engineerPage.scrollToEngineeringLabel(AssetEngineerPage.TRIP_CONFIG_HEADER);

        logStep("Step 2: Segments editor header — app truth: 'Settings' (engineering.settings)"
                + " inside SkmTripConfigCard; 'Trip Settings' belongs to the custom sheet only");
        boolean cardSettingsHeader = engineerPage.scrollToEngineeringLabel("Settings");
        boolean customSheetHeader = engineerPage.isEngineeringLabelPresent(AssetEngineerPage.TRIP_SETTINGS_HEADER);
        skipIfPreconditionMissing(() -> cardSettingsHeader || customSheetHeader,
                "bound device carries no trip-unit segments in this library snapshot — segmentsEditor"
                        + " renders only when segmentsForSelectedTripUnit is non-empty");
        System.out.println("ℹ️ segments header found: "
                + (cardSettingsHeader ? "'Settings' (SkmTripConfigCard)" : "'Trip Settings'"));

        logStep("Step 3: The editor lives INSIDE the trip config card — card header must coexist");
        assertTrue(engineerPage.isTripConfigCardShown(),
                "'" + AssetEngineerPage.TRIP_CONFIG_HEADER + "' card header vanished while the"
                        + " segments editor renders — editor must be nested in the card");
        logStepWithScreenshot("TC_ENG_145 segments editor verified");
    }

    // ═══════════ bound state hides the input stack ═══════════

    /** Input-stack labels that must unmount while a library row is bound. */
    @DataProvider(name = "hiddenWhileBoundLabels")
    public Object[][] hiddenWhileBoundLabels() {
        return new Object[][] {
                {"Frame Amps"}, {"Pole Count"}, {"Trip Amps"},
        };
    }

    @Test(priority = 1307, dataProvider = "hiddenWhileBoundLabels")
    public void TC_ENG_146_boundStateHidesInputStack(String label) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_146 [" + label + "] - Bound draft hides this OCP input row (bound card replaces the stack)");

        logStep("Step 1: Bound breaker draft");
        ensureBoundBreakerDraft();

        logStep("Step 2: '" + label + "' must be unmounted while bound");
        assertFalse(engineerPage.isEngineeringLabelPresent(label),
                "input-stack row '" + label + "' still renders while a library row is bound — the bound"
                        + " card + configurator must REPLACE the engineering inputs"
                        + " (NodeEngineeringSection: isLibraryBound hides engineeringInputs)");
    }

    @Test(priority = 1308)
    public void TC_ENG_147_boundCardIsLibraryMatchedNotCustom() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_147 - A match-card bind renders the green 'Library Matched' card, never 'Custom Entry'");

        logStep("Step 1: Bound breaker draft");
        ensureBoundBreakerDraft();

        logStep("Step 2: Bound-card identity — library match, not custom entry");
        assertTrue(engineerPage.isBoundCardShown(),
                "'Library Matched' bound card must render for a match-card bind");
        assertFalse(engineerPage.isCustomEntryCardShown(),
                "'Custom Entry' card rendered for a real library match — the accent/identity split"
                        + " (sel.isCustom) is broken");
        logStepWithScreenshot("TC_ENG_147 bound-card identity verified");
    }

    // ═══════════ unlink: fields survive (G5) ═══════════

    @Test(priority = 1309)
    public void TC_ENG_148_unlinkKeepsBackfilledFields() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_148 - Unlink removes the bound+configurator cards but keeps the backfilled fields");

        logStep("Step 1: Bound breaker draft, then Unlink in place");
        ensureBoundBreakerDraft();
        assertTrue(engineerPage.tapUnlink(), "Unlink must remove the draft binding");
        draftPhase = "unlinked";

        logStep("Step 2: Bound card AND configurator card unmount together");
        assertFalse(engineerPage.isBoundCardShown(), "bound card must be gone after Unlink");
        assertFalse(engineerPage.isTripConfigCardShown(),
                "'" + AssetEngineerPage.TRIP_CONFIG_HEADER + "' card must unmount with the binding —"
                        + " it reads skm_settings which Unlink clears");

        logStep("Step 3: Engineering section + input stack return");
        assertTrue(engineerPage.isEngineeringSectionPresent(),
                "the Engineering section itself must survive the unbind");
        assertTrue(engineerPage.scrollToEngineeringLabel("Frame Amps"),
                "'Frame Amps' input row must return once the binding is cleared");

        logStep("Step 4: Backfill survives — unbindLibrary keeps first-class fields");
        String frameAmps = engineerPage.getEngineeringFieldValue("Enter frame amps");
        assertFalse(frameAmps.isEmpty(),
                "Frame Amps must keep the match-backfilled value after Unlink (applyPickedMatch wrote"
                        + " draft.frame_amps; unbindLibrary keeps first-class fields) — field read back ''");
        assertTrue(frameAmps.matches("\\d+"),
                "Frame Amps backfill must be the numeric ampere rating; got '" + frameAmps + "'");
        logStepWithScreenshot("TC_ENG_148 frame amps retained after unlink: " + frameAmps);
    }

    @Test(priority = 1310)
    public void TC_ENG_149_postUnlinkMutualExclusionHolds() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_149 - After Unlink the Trip-Amps XOR Sensor+Plug split still holds (has_trip_unit from the match)");

        logStep("Step 1: Unlinked breaker draft (bind → Unlink), Frame Amps in view");
        ensureUnlinkedBreakerDraft();
        assertTrue(engineerPage.scrollToEngineeringLabel("Frame Amps"),
                "'Frame Amps' must render in the post-unlink input stack");

        logStep("Step 2: XOR law re-established with the match-derived has_trip_unit");
        boolean tripAmps = engineerPage.isEngineeringLabelPresent("Trip Amps");
        boolean sensorAmps = engineerPage.isEngineeringLabelPresent("Sensor Amps");
        boolean plugAmps = engineerPage.isEngineeringLabelPresent("Plug Amps");
        System.out.println("ℹ️ post-unlink split: tripAmps=" + tripAmps
                + " sensorAmps=" + sensorAmps + " plugAmps=" + plugAmps);
        if (tripAmps) {
            assertFalse(sensorAmps,
                    "post-unlink mutual exclusion violated: 'Trip Amps' AND 'Sensor Amps' both render");
            assertFalse(plugAmps,
                    "post-unlink mutual exclusion violated: 'Trip Amps' AND 'Plug Amps' both render");
        } else {
            assertTrue(sensorAmps,
                    "post-unlink: neither 'Trip Amps' nor 'Sensor Amps' rendered — the split lost both branches");
            assertTrue(plugAmps,
                    "post-unlink: 'Sensor Amps' without 'Plug Amps' — the trip-unit branch must render the pair");
        }
        logStepWithScreenshot("TC_ENG_149 verified (tripAmps=" + tripAmps + ")");

        logStep("Step 3: Discard the chained draft — nothing persists");
        assertTrue(engineerPage.closeAssetDetails(true), "the draft must discard cleanly");
        draftPhase = null;
    }
}
