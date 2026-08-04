package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.utils.ExtentReportManager;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * WorkTypeCreatePicker_Test — TC_WTC_PICK_* — the v1.55 Work Type PICKER SHEET
 * on the iOS "New Work Order" create form (design doc Class 2,
 * docs/worktype-create-dropdown-design-2026-08-04.md).
 *
 * SCOPE: the option surface itself — sheet open/close semantics, exact
 * 14-option census (General pinned FIRST, then the 13 service display names in
 * case-sensitive lexicographic order), per-option presence + position +
 * selection round-trip, radio-mark state across reopen, tap-commits-and-closes
 * semantics, the re-tap-selected no-op close, and app-health stability under
 * repeated open/select cycles. This class NEVER taps Create — no Work Order is
 * ever created here (E2E creation lives in WorkTypeCreateE2E_Test).
 *
 * APP TRUTH (probe-pinned PROBE_E/F/G 2026-08-04, v1.55 — all locators in
 * WorkOrderPage's "v1.55 Work Type create-form dropdown" section are pinned to
 * these probes):
 *  - Create-form row is a Button named 'Work Type, *, <value>' — the required
 *    marker '*' is its own segment, so the value MUST be prefix-parsed
 *    (getCreateFormWorkTypeValue), never last-comma-segment parsed:
 *    'Clean, Tighten, Torque' embeds commas.
 *  - Picker = stacked bottom sheet with its OWN 'Work Type' NavigationBar;
 *    14 full-width option Buttons, ALL visible without scrolling.
 *  - Radio semantics: selected option Button reads value=='1'/selected==true
 *    (checkmark.circle.fill); a CENTER TAP on an option COMMITS AND CLOSES the
 *    sheet immediately — there is NO sheet Done, and swipe-down does NOT
 *    dismiss. The only safe no-op close is re-tapping the selected row
 *    (closeWorkTypePickerNoChange).
 *
 * HYGIENE: every test opens a FRESH create form (openWorkOrdersScreenWT →
 * openCreateForm) and restores the Work Orders list in a finally block
 * (close-any-open-sheet → cancelCreateForm → waitForWorkOrdersScreen). Cancel
 * discards the form, so no cleanup debt is ever left behind.
 *
 * TC ID SCHEME: TC_WTC_PICK_001..008 surface census; 009..022 per-option
 * presence; 023..036 per-option census position; 037..050 per-option selection
 * round-trip; 051..062 radio state; 063..070 commit/no-op-close semantics;
 * 071..076 stability guards.
 */
public class WorkTypeCreatePicker_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "Work Type Create Dropdown (v1.55)";

    // ─────────────────────────── expected census ───────────────────────────

    /**
     * The 13 non-General display names in case-sensitive lexicographic order
     * (String::compareTo — probe-observed: 'DGA / Fluid Sample Analysis' sorts
     * BEFORE 'De-Energized Visual Inspection'; 'Clean, Tighten, Torque' sorts
     * BEFORE 'Cleaning').
     */
    private static List<String> expectedNonGeneralSorted() {
        List<String> expected = new ArrayList<>();
        for (WorkTypeCatalog wt : WorkTypeCatalog.values()) {
            if (wt == WorkTypeCatalog.GENERAL) continue;
            expected.add(wt.displayName());
        }
        Collections.sort(expected, String::compareTo);
        return expected;
    }

    /** Full expected 14-entry census: 'General' pinned FIRST, then the sorted 13. */
    private static List<String> expectedCensus() {
        List<String> full = new ArrayList<>();
        full.add(WorkTypeCatalog.GENERAL.displayName());
        full.addAll(expectedNonGeneralSorted());
        return full;
    }

    // ─────────────────────────── shared flow helpers ───────────────────────────

    /**
     * Entry with crash guard: login → dashboard → Work Orders list → fresh
     * 'New Work Order' create form, with the v1.55 Work Type row proven
     * present (flow precondition for every picker interaction).
     */
    private void openCreateFormGuarded(String tc) {
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(),
                "'New Work Order' create form must open from the Work Orders list (verified: form nav bar)");
        verifyAppAlive(tc + ": create form open");
        assertTrue(wo.isCreateFormWorkTypeRowPresent(),
                "v1.55 create form must carry the 'Work Type, *, <value>' config row");
    }

    /** Open the Work Type sheet with a verified-open assert + crash guard. */
    private void openPickerGuarded(String tc) {
        assertTrue(wo.openWorkTypePicker(),
                "Work Type picker sheet must open (verified: own 'Work Type' NavigationBar)");
        verifyAppAlive(tc + ": picker sheet open");
    }

    /** Read + log the option census (bounded, type-bound primitive). */
    private List<String> censusLogged(String tc) {
        List<String> census = wo.getWorkTypePickerOptions(14);
        logStep(tc + " census (" + census.size() + " rows): " + census);
        return census;
    }

    /**
     * Nav hygiene ONLY (finally-block; contracts are asserted in the test
     * bodies): close any open sheet via the sole safe no-op path, cancel the
     * form (discards it — nothing was created), restore the list.
     */
    private void formHygiene() {
        try {
            if (wo.isWorkTypePickerOpen()) {
                wo.closeWorkTypePickerNoChange();
            }
            wo.cancelCreateForm();
            shortWait();
            wo.waitForWorkOrdersScreen();
        } catch (Exception e) {
            System.out.println("⚠️ formHygiene (non-fatal): " + e.getMessage());
        }
    }

    // ─────────────────────────── group runners ───────────────────────────

    /** Presence matrix runner: {@code wt.displayName()} appears in the census. */
    private void runOptionPresent(WorkTypeCatalog wt, String tc) {
        openCreateFormGuarded(tc);
        try {
            openPickerGuarded(tc);
            List<String> census = censusLogged(tc);
            assertTrue(census.contains(wt.displayName()),
                    "Picker census must contain '" + wt.displayName() + "' — got " + census);
            verifyAppAlive(tc + ": option present");
            logStepWithScreenshot(tc + " verified: '" + wt.displayName() + "' present in picker");
        } finally {
            formHygiene();
        }
    }

    /** Position matrix runner: {@code wt.displayName()} sits at its EXACT expected census index. */
    private void runOptionPosition(WorkTypeCatalog wt, String tc) {
        int expectedIdx = expectedCensus().indexOf(wt.displayName());
        openCreateFormGuarded(tc);
        try {
            openPickerGuarded(tc);
            List<String> census = censusLogged(tc);
            int actualIdx = census.indexOf(wt.displayName());
            assertEquals(actualIdx, expectedIdx,
                    "'" + wt.displayName() + "' must sit at census index " + expectedIdx
                    + " ('General' pinned first, then case-sensitive lexicographic) — census: " + census);
            verifyAppAlive(tc + ": option position verified");
            logStepWithScreenshot(tc + " verified: '" + wt.displayName() + "' at index " + expectedIdx);
        } finally {
            formHygiene();
        }
    }

    /**
     * Selection matrix runner: selecting {@code wt} commits + closes
     * (primitive-verified) AND the row value prefix-parses back to the exact
     * display name (comma-safe — the whole point of getCreateFormWorkTypeValue).
     */
    private void runOptionSelects(WorkTypeCatalog wt, String tc) {
        openCreateFormGuarded(tc);
        try {
            openPickerGuarded(tc);
            assertTrue(wo.selectWorkTypeInPicker(wt.displayName()),
                    "selectWorkTypeInPicker('" + wt.displayName()
                    + "') must tap-commit, close the sheet, and flip the row value");
            assertEquals(wo.getCreateFormWorkTypeValue(), wt.displayName(),
                    "Work Type row value must read back the selected option exactly (comma-safe prefix parse)");
            verifyAppAlive(tc + ": selection committed");
            logStepWithScreenshot(tc + " verified: selected '" + wt.displayName() + "'");
        } finally {
            formHygiene();
        }
    }

    /**
     * Radio chain runner: for each chain element in order — select it
     * (commits + closes), REOPEN, and assert the reopened sheet marks exactly
     * that element as selected; close no-change before the next hop.
     */
    private void runSelectionChain(String tc, WorkTypeCatalog... chain) {
        openCreateFormGuarded(tc);
        try {
            for (WorkTypeCatalog wt : chain) {
                openPickerGuarded(tc);
                assertTrue(wo.selectWorkTypeInPicker(wt.displayName()),
                        "Chain hop must commit '" + wt.displayName() + "'");
                openPickerGuarded(tc);
                assertEquals(wo.getSelectedWorkTypeInPicker(), wt.displayName(),
                        "Reopened picker must radio-mark the latest committed selection '"
                        + wt.displayName() + "'");
                assertTrue(wo.closeWorkTypePickerNoChange(),
                        "No-op close (re-tap selected row) must succeed between chain hops");
                verifyAppAlive(tc + ": chain hop '" + wt.displayName() + "' verified");
            }
            logStepWithScreenshot(tc + " verified: reopen marks the latest selection at every hop");
        } finally {
            formHygiene();
        }
    }

    // ═══════════════ Block 1 — SURFACE census (001..008) ═══════════════

    @Test(priority = 1)
    public void TC_WTC_PICK_001_pickerOpensVerified() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_001 - Work Type row tap opens the picker sheet (verified open)");
        openCreateFormGuarded("TC_WTC_PICK_001");
        try {
            assertTrue(wo.openWorkTypePicker(),
                    "openWorkTypePicker must report a VERIFIED open (own 'Work Type' nav appears)");
            verifyAppAlive("TC_WTC_PICK_001: picker opened");
            logStepWithScreenshot("TC_WTC_PICK_001 verified: picker sheet opened");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 2)
    public void TC_WTC_PICK_002_sheetNavWorkTypePresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_002 - open sheet carries its own 'Work Type' NavigationBar");
        openCreateFormGuarded("TC_WTC_PICK_002");
        try {
            openPickerGuarded("TC_WTC_PICK_002");
            assertTrue(wo.isWorkTypePickerOpen(),
                    "isWorkTypePickerOpen must read TRUE while the sheet's 'Work Type' nav bar is up");
            logStepWithScreenshot("TC_WTC_PICK_002 verified: 'Work Type' sheet nav present");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 3)
    public void TC_WTC_PICK_003_censusExactlyFourteen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_003 - option census is EXACTLY 14 (all visible, no scroll)");
        openCreateFormGuarded("TC_WTC_PICK_003");
        try {
            openPickerGuarded("TC_WTC_PICK_003");
            List<String> census = censusLogged("TC_WTC_PICK_003");
            assertEquals(census.size(), 14,
                    "Picker must expose EXACTLY 14 options (General + 13 services) — got " + census);
            logStepWithScreenshot("TC_WTC_PICK_003 verified: 14-option census");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 4)
    public void TC_WTC_PICK_004_censusHasNoChromeEntries() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_004 - census contains NO 'Done'/'Cancel'/'Create' chrome (no sheet Done exists)");
        openCreateFormGuarded("TC_WTC_PICK_004");
        try {
            openPickerGuarded("TC_WTC_PICK_004");
            List<String> census = censusLogged("TC_WTC_PICK_004");
            assertFalse(census.contains("Done") || census.contains("Cancel") || census.contains("Create"),
                    "Census must contain NO chrome entries — the sheet has NO Done (any 'Done' is the "
                    + "background WO-list nav, PROBE_F locator trap) — got " + census);
            logStepWithScreenshot("TC_WTC_PICK_004 verified: chrome-free census");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 5)
    public void TC_WTC_PICK_005_censusHasNoDuplicates() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_005 - census has no duplicate option labels");
        openCreateFormGuarded("TC_WTC_PICK_005");
        try {
            openPickerGuarded("TC_WTC_PICK_005");
            List<String> census = censusLogged("TC_WTC_PICK_005");
            Set<String> unique = new LinkedHashSet<>(census);
            assertEquals(unique.size(), census.size(),
                    "Every census label must be unique (twinned StaticTexts must not leak into the "
                    + "Button census) — got " + census);
            logStepWithScreenshot("TC_WTC_PICK_005 verified: no duplicate labels");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 6)
    public void TC_WTC_PICK_006_generalPinnedFirst() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_006 - 'General' is the FIRST census entry");
        openCreateFormGuarded("TC_WTC_PICK_006");
        try {
            openPickerGuarded("TC_WTC_PICK_006");
            List<String> census = censusLogged("TC_WTC_PICK_006");
            assertTrue(!census.isEmpty(), "Census must not be empty");
            assertEquals(census.get(0), WorkTypeCatalog.GENERAL.displayName(),
                    "'General' must be pinned FIRST in the picker — got " + census);
            logStepWithScreenshot("TC_WTC_PICK_006 verified: 'General' first");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 7)
    public void TC_WTC_PICK_007_nonGeneralLexicographicOrder() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_007 - the 13 non-General entries are in case-sensitive lexicographic order");
        openCreateFormGuarded("TC_WTC_PICK_007");
        try {
            openPickerGuarded("TC_WTC_PICK_007");
            List<String> census = censusLogged("TC_WTC_PICK_007");
            assertEquals(census.size(), 14,
                    "Order check needs the full 14-entry census — got " + census);
            List<String> expected = expectedNonGeneralSorted();
            assertEquals(census.subList(1, 14), expected,
                    "Entries 2..14 must equal WorkTypeCatalog display names sorted case-sensitively "
                    + "(String::compareTo — 'DGA / Fluid Sample Analysis' before "
                    + "'De-Energized Visual Inspection')");
            logStepWithScreenshot("TC_WTC_PICK_007 verified: lexicographic order holds");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 8)
    public void TC_WTC_PICK_008_everyCensusEntryIsCatalogBacked() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_008 - every census entry resolves to a WorkTypeCatalog display name (no foreign labels)");
        openCreateFormGuarded("TC_WTC_PICK_008");
        try {
            openPickerGuarded("TC_WTC_PICK_008");
            List<String> census = censusLogged("TC_WTC_PICK_008");
            assertTrue(!census.isEmpty(), "Census must not be empty");
            List<String> foreign = new ArrayList<>();
            for (String label : census) {
                if (WorkTypeCatalog.byDisplayName(label) == null) foreign.add(label);
            }
            assertTrue(foreign.isEmpty(),
                    "Every picker label must EXACTLY equal a WorkTypeCatalog.displayName() — foreign "
                    + "labels found: " + foreign);
            logStepWithScreenshot("TC_WTC_PICK_008 verified: census is catalog-backed");
        } finally {
            formHygiene();
        }
    }

    // ═══════════════ Block 2 — per-option PRESENCE (009..022) ═══════════════
    // Picker order (probe-pinned): General first, then the sorted 13.

    @Test(priority = 9)
    public void TC_WTC_PICK_009_generalPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_009 - 'General' present in the picker census");
        runOptionPresent(WorkTypeCatalog.GENERAL, "TC_WTC_PICK_009");
    }

    @Test(priority = 10)
    public void TC_WTC_PICK_010_afDataCollectionPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_010 - 'Arc Flash Data Collection' present in the picker census");
        runOptionPresent(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WTC_PICK_010");
    }

    @Test(priority = 11)
    public void TC_WTC_PICK_011_afLabelPlacementPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_011 - 'Arc Flash Label Placement' present in the picker census");
        runOptionPresent(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WTC_PICK_011");
    }

    @Test(priority = 12)
    public void TC_WTC_PICK_012_cleanTightenTorquePresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_012 - 'Clean, Tighten, Torque' present in the picker census (comma name)");
        runOptionPresent(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WTC_PICK_012");
    }

    @Test(priority = 13)
    public void TC_WTC_PICK_013_cleaningPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_013 - 'Cleaning' present in the picker census");
        runOptionPresent(WorkTypeCatalog.CLEANING, "TC_WTC_PICK_013");
    }

    @Test(priority = 14)
    public void TC_WTC_PICK_014_conditionAssessmentPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_014 - 'Condition Assessment' present in the picker census");
        runOptionPresent(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WTC_PICK_014");
    }

    @Test(priority = 15)
    public void TC_WTC_PICK_015_dgaFluidSamplePresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_015 - 'DGA / Fluid Sample Analysis' present in the picker census");
        runOptionPresent(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WTC_PICK_015");
    }

    @Test(priority = 16)
    public void TC_WTC_PICK_016_deEnergizedVisualPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_016 - 'De-Energized Visual Inspection' present in the picker census");
        runOptionPresent(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WTC_PICK_016");
    }

    @Test(priority = 17)
    public void TC_WTC_PICK_017_infraredThermographyPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_017 - 'Infrared Thermography' present in the picker census");
        runOptionPresent(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WTC_PICK_017");
    }

    @Test(priority = 18)
    public void TC_WTC_PICK_018_insulationResistancePresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_018 - 'Insulation Resistance Testing' present in the picker census");
        runOptionPresent(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WTC_PICK_018");
    }

    @Test(priority = 19)
    public void TC_WTC_PICK_019_netaTestingPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_019 - 'NETA Testing' present in the picker census");
        runOptionPresent(WorkTypeCatalog.NETA_TESTING, "TC_WTC_PICK_019");
    }

    @Test(priority = 20)
    public void TC_WTC_PICK_020_panelScheduleUpdatesPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_020 - 'Panel Schedule Updates' present in the picker census");
        runOptionPresent(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WTC_PICK_020");
    }

    @Test(priority = 21)
    public void TC_WTC_PICK_021_shutdownCompositePresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_021 - 'Shutdown (Composite)' present in the picker census");
        runOptionPresent(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WTC_PICK_021");
    }

    @Test(priority = 22)
    public void TC_WTC_PICK_022_upsMaintenancePresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_022 - 'UPS Maintenance' present in the picker census");
        runOptionPresent(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WTC_PICK_022");
    }

    // ═══════════════ Block 3 — per-option census POSITION (023..036) ═══════════════

    @Test(priority = 23)
    public void TC_WTC_PICK_023_generalAtIndex0() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_023 - 'General' sits at census index 0");
        runOptionPosition(WorkTypeCatalog.GENERAL, "TC_WTC_PICK_023");
    }

    @Test(priority = 24)
    public void TC_WTC_PICK_024_afDataCollectionAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_024 - 'Arc Flash Data Collection' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WTC_PICK_024");
    }

    @Test(priority = 25)
    public void TC_WTC_PICK_025_afLabelPlacementAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_025 - 'Arc Flash Label Placement' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WTC_PICK_025");
    }

    @Test(priority = 26)
    public void TC_WTC_PICK_026_cleanTightenTorqueAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_026 - 'Clean, Tighten, Torque' sits BEFORE 'Cleaning' (comma sorts low)");
        runOptionPosition(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WTC_PICK_026");
    }

    @Test(priority = 27)
    public void TC_WTC_PICK_027_cleaningAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_027 - 'Cleaning' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.CLEANING, "TC_WTC_PICK_027");
    }

    @Test(priority = 28)
    public void TC_WTC_PICK_028_conditionAssessmentAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_028 - 'Condition Assessment' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WTC_PICK_028");
    }

    @Test(priority = 29)
    public void TC_WTC_PICK_029_dgaFluidSampleAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_029 - 'DGA / Fluid Sample Analysis' sits BEFORE 'De-Energized…' (case-sensitive sort)");
        runOptionPosition(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WTC_PICK_029");
    }

    @Test(priority = 30)
    public void TC_WTC_PICK_030_deEnergizedVisualAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_030 - 'De-Energized Visual Inspection' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WTC_PICK_030");
    }

    @Test(priority = 31)
    public void TC_WTC_PICK_031_infraredThermographyAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_031 - 'Infrared Thermography' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WTC_PICK_031");
    }

    @Test(priority = 32)
    public void TC_WTC_PICK_032_insulationResistanceAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_032 - 'Insulation Resistance Testing' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WTC_PICK_032");
    }

    @Test(priority = 33)
    public void TC_WTC_PICK_033_netaTestingAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_033 - 'NETA Testing' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.NETA_TESTING, "TC_WTC_PICK_033");
    }

    @Test(priority = 34)
    public void TC_WTC_PICK_034_panelScheduleUpdatesAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_034 - 'Panel Schedule Updates' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WTC_PICK_034");
    }

    @Test(priority = 35)
    public void TC_WTC_PICK_035_shutdownCompositeAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_035 - 'Shutdown (Composite)' sits at its sorted census index");
        runOptionPosition(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WTC_PICK_035");
    }

    @Test(priority = 36)
    public void TC_WTC_PICK_036_upsMaintenanceAtSortedIndex() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_036 - 'UPS Maintenance' sits LAST (index 13)");
        runOptionPosition(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WTC_PICK_036");
    }

    // ═══════════════ Block 4 — per-option SELECTION round-trip (037..050) ═══════════════

    @Test(priority = 37)
    public void TC_WTC_PICK_037_selectGeneral() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_037 - selecting 'General' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.GENERAL, "TC_WTC_PICK_037");
    }

    @Test(priority = 38)
    public void TC_WTC_PICK_038_selectAfDataCollection() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_038 - selecting 'Arc Flash Data Collection' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WTC_PICK_038");
    }

    @Test(priority = 39)
    public void TC_WTC_PICK_039_selectAfLabelPlacement() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_039 - selecting 'Arc Flash Label Placement' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WTC_PICK_039");
    }

    @Test(priority = 40)
    public void TC_WTC_PICK_040_selectCleanTightenTorque() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_040 - selecting 'Clean, Tighten, Torque' reads back comma-intact (prefix parse)");
        runOptionSelects(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WTC_PICK_040");
    }

    @Test(priority = 41)
    public void TC_WTC_PICK_041_selectCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_041 - selecting 'Cleaning' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.CLEANING, "TC_WTC_PICK_041");
    }

    @Test(priority = 42)
    public void TC_WTC_PICK_042_selectConditionAssessment() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_042 - selecting 'Condition Assessment' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WTC_PICK_042");
    }

    @Test(priority = 43)
    public void TC_WTC_PICK_043_selectDgaFluidSample() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_043 - selecting 'DGA / Fluid Sample Analysis' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WTC_PICK_043");
    }

    @Test(priority = 44)
    public void TC_WTC_PICK_044_selectDeEnergizedVisual() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_044 - selecting 'De-Energized Visual Inspection' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WTC_PICK_044");
    }

    @Test(priority = 45)
    public void TC_WTC_PICK_045_selectInfraredThermography() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_045 - selecting 'Infrared Thermography' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WTC_PICK_045");
    }

    @Test(priority = 46)
    public void TC_WTC_PICK_046_selectInsulationResistance() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_046 - selecting 'Insulation Resistance Testing' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WTC_PICK_046");
    }

    @Test(priority = 47)
    public void TC_WTC_PICK_047_selectNetaTesting() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_047 - selecting 'NETA Testing' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.NETA_TESTING, "TC_WTC_PICK_047");
    }

    @Test(priority = 48)
    public void TC_WTC_PICK_048_selectPanelScheduleUpdates() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_048 - selecting 'Panel Schedule Updates' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WTC_PICK_048");
    }

    @Test(priority = 49)
    public void TC_WTC_PICK_049_selectShutdownComposite() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_049 - selecting 'Shutdown (Composite)' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WTC_PICK_049");
    }

    @Test(priority = 50)
    public void TC_WTC_PICK_050_selectUpsMaintenance() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_050 - selecting 'UPS Maintenance' commits and the row reads it back");
        runOptionSelects(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WTC_PICK_050");
    }

    // ═══════════════ Block 5 — RADIO state (051..062) ═══════════════

    @Test(priority = 51)
    public void TC_WTC_PICK_051_defaultOpenMarksGeneral() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_051 - fresh form: default open radio-marks 'General'");
        openCreateFormGuarded("TC_WTC_PICK_051");
        try {
            openPickerGuarded("TC_WTC_PICK_051");
            assertEquals(wo.getSelectedWorkTypeInPicker(), WorkTypeCatalog.GENERAL.displayName(),
                    "Default open must radio-mark 'General' (value=='1'/checkmark.circle.fill row)");
            logStepWithScreenshot("TC_WTC_PICK_051 verified: 'General' marked by default");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 52)
    public void TC_WTC_PICK_052_defaultOpenGeneralIsSelectedPredicate() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_052 - fresh form: isWorkTypeOptionSelected('General') reads true");
        openCreateFormGuarded("TC_WTC_PICK_052");
        try {
            openPickerGuarded("TC_WTC_PICK_052");
            assertTrue(wo.isWorkTypeOptionSelected(WorkTypeCatalog.GENERAL.displayName()),
                    "isWorkTypeOptionSelected('General') must read TRUE on a fresh form's default open");
            logStepWithScreenshot("TC_WTC_PICK_052 verified: selected-predicate true for 'General'");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 53)
    public void TC_WTC_PICK_053_defaultMarkStableTwoReads() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_053 - default radio mark is stable across two consecutive reads");
        openCreateFormGuarded("TC_WTC_PICK_053");
        try {
            openPickerGuarded("TC_WTC_PICK_053");
            String first = wo.getSelectedWorkTypeInPicker();
            String second = wo.getSelectedWorkTypeInPicker();
            logStep("TC_WTC_PICK_053 reads: first='" + first + "' second='" + second + "'");
            assertEquals(first, WorkTypeCatalog.GENERAL.displayName(),
                    "First read of the default mark must be 'General'");
            assertEquals(second, first,
                    "Two consecutive selected-row reads must be identical (no flicker/ghost mark)");
            logStepWithScreenshot("TC_WTC_PICK_053 verified: mark stable across two reads");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 54)
    public void TC_WTC_PICK_054_reopenMarksCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_054 - after selecting 'Cleaning', reopen radio-marks 'Cleaning'");
        openCreateFormGuarded("TC_WTC_PICK_054");
        try {
            openPickerGuarded("TC_WTC_PICK_054");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.CLEANING.displayName()),
                    "Selecting 'Cleaning' must commit and close");
            openPickerGuarded("TC_WTC_PICK_054");
            assertEquals(wo.getSelectedWorkTypeInPicker(), WorkTypeCatalog.CLEANING.displayName(),
                    "Reopened picker must radio-mark the committed 'Cleaning'");
            logStepWithScreenshot("TC_WTC_PICK_054 verified: reopen marks 'Cleaning'");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 55)
    public void TC_WTC_PICK_055_reopenMarksCommaName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_055 - after selecting 'Clean, Tighten, Torque', reopen marks it (comma name)");
        openCreateFormGuarded("TC_WTC_PICK_055");
        try {
            openPickerGuarded("TC_WTC_PICK_055");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE.displayName()),
                    "Selecting 'Clean, Tighten, Torque' must commit and close");
            openPickerGuarded("TC_WTC_PICK_055");
            assertEquals(wo.getSelectedWorkTypeInPicker(), WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE.displayName(),
                    "Reopened picker must radio-mark 'Clean, Tighten, Torque' with commas intact");
            logStepWithScreenshot("TC_WTC_PICK_055 verified: reopen marks the comma name");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 56)
    public void TC_WTC_PICK_056_reopenedMarkStableTwoReads() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_056 - after selecting 'UPS Maintenance', the reopened mark is stable across two reads");
        openCreateFormGuarded("TC_WTC_PICK_056");
        try {
            openPickerGuarded("TC_WTC_PICK_056");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.UPS_MAINTENANCE.displayName()),
                    "Selecting 'UPS Maintenance' must commit and close");
            openPickerGuarded("TC_WTC_PICK_056");
            String first = wo.getSelectedWorkTypeInPicker();
            String second = wo.getSelectedWorkTypeInPicker();
            logStep("TC_WTC_PICK_056 reads: first='" + first + "' second='" + second + "'");
            assertEquals(first, WorkTypeCatalog.UPS_MAINTENANCE.displayName(),
                    "Reopened mark must be the committed 'UPS Maintenance'");
            assertEquals(second, first,
                    "Two consecutive reads of the reopened mark must be identical");
            logStepWithScreenshot("TC_WTC_PICK_056 verified: reopened mark stable");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 57)
    public void TC_WTC_PICK_057_reopenSelectedPredicateForInfrared() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_057 - after selecting 'Infrared Thermography', reopened isWorkTypeOptionSelected reads true");
        openCreateFormGuarded("TC_WTC_PICK_057");
        try {
            openPickerGuarded("TC_WTC_PICK_057");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.INFRARED_THERMOGRAPHY.displayName()),
                    "Selecting 'Infrared Thermography' must commit and close");
            openPickerGuarded("TC_WTC_PICK_057");
            assertTrue(wo.isWorkTypeOptionSelected(WorkTypeCatalog.INFRARED_THERMOGRAPHY.displayName()),
                    "isWorkTypeOptionSelected('Infrared Thermography') must read TRUE after reopen");
            logStepWithScreenshot("TC_WTC_PICK_057 verified: selected-predicate true after reopen");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 58)
    public void TC_WTC_PICK_058_defaultMarkMovesOffGeneral() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_058 - after selecting 'NETA Testing', reopened 'General' is NO LONGER marked");
        openCreateFormGuarded("TC_WTC_PICK_058");
        try {
            openPickerGuarded("TC_WTC_PICK_058");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.NETA_TESTING.displayName()),
                    "Selecting 'NETA Testing' must commit and close");
            openPickerGuarded("TC_WTC_PICK_058");
            assertFalse(wo.isWorkTypeOptionSelected(WorkTypeCatalog.GENERAL.displayName()),
                    "'General' must lose its radio mark once 'NETA Testing' is committed (single-select radio)");
            logStepWithScreenshot("TC_WTC_PICK_058 verified: default mark moved off 'General'");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 59)
    public void TC_WTC_PICK_059_radioExclusivityAfterXThenY() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_059 - X then Y: reopened sheet marks ONLY Y (X unmarked)");
        final String x = WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT.displayName();
        final String y = WorkTypeCatalog.PANEL_SCHEDULE_UPDATES.displayName();
        openCreateFormGuarded("TC_WTC_PICK_059");
        try {
            openPickerGuarded("TC_WTC_PICK_059");
            assertTrue(wo.selectWorkTypeInPicker(x), "Selecting X='" + x + "' must commit and close");
            openPickerGuarded("TC_WTC_PICK_059");
            assertTrue(wo.selectWorkTypeInPicker(y), "Selecting Y='" + y + "' must commit and close");
            openPickerGuarded("TC_WTC_PICK_059");
            assertFalse(wo.isWorkTypeOptionSelected(x),
                    "Previous selection X='" + x + "' must be UNMARKED after committing Y (radio exclusivity)");
            assertEquals(wo.getSelectedWorkTypeInPicker(), y,
                    "The single marked row must be the latest commit Y='" + y + "'");
            logStepWithScreenshot("TC_WTC_PICK_059 verified: only Y marked after X→Y");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 60)
    public void TC_WTC_PICK_060_chainAcrossAlphabet() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_060 - chain General → 'Arc Flash Data Collection' → 'UPS Maintenance': each reopen marks the latest");
        runSelectionChain("TC_WTC_PICK_060",
                WorkTypeCatalog.GENERAL,
                WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION,
                WorkTypeCatalog.UPS_MAINTENANCE);
    }

    @Test(priority = 61)
    public void TC_WTC_PICK_061_chainWithCommaName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_061 - chain 'Clean, Tighten, Torque' → 'NETA Testing' → 'Cleaning': each reopen marks the latest");
        runSelectionChain("TC_WTC_PICK_061",
                WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE,
                WorkTypeCatalog.NETA_TESTING,
                WorkTypeCatalog.CLEANING);
    }

    @Test(priority = 62)
    public void TC_WTC_PICK_062_chainMidAlphabet() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_062 - chain 'DGA / Fluid Sample Analysis' → 'Shutdown (Composite)' → 'Condition Assessment': each reopen marks the latest");
        runSelectionChain("TC_WTC_PICK_062",
                WorkTypeCatalog.DGA_FLUID_SAMPLE,
                WorkTypeCatalog.SHUTDOWN_COMPOSITE,
                WorkTypeCatalog.CONDITION_ASSESSMENT);
    }

    // ═══════════════ Block 6 — COMMIT semantics (063..070) ═══════════════

    @Test(priority = 63)
    public void TC_WTC_PICK_063_tapCommitClosesSheet() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_063 - a single option tap commits AND closes the sheet (no Done step)");
        openCreateFormGuarded("TC_WTC_PICK_063");
        try {
            openPickerGuarded("TC_WTC_PICK_063");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.CLEANING.displayName()),
                    "Selecting 'Cleaning' must commit");
            assertFalse(wo.isWorkTypePickerOpen(),
                    "Sheet must be CLOSED immediately after the option tap — tap commits and closes, no Done exists");
            logStepWithScreenshot("TC_WTC_PICK_063 verified: tap-commit closed the sheet");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 64)
    public void TC_WTC_PICK_064_tapCommitClosesSheetCommaName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_064 - tap-commit closes the sheet for the comma name 'Clean, Tighten, Torque'");
        openCreateFormGuarded("TC_WTC_PICK_064");
        try {
            openPickerGuarded("TC_WTC_PICK_064");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE.displayName()),
                    "Selecting 'Clean, Tighten, Torque' must commit");
            assertFalse(wo.isWorkTypePickerOpen(),
                    "Sheet must be CLOSED immediately after tapping the comma-named option");
            logStepWithScreenshot("TC_WTC_PICK_064 verified: comma-name tap-commit closed the sheet");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 65)
    public void TC_WTC_PICK_065_noopCloseOnDefaultGeneral() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_065 - closeWorkTypePickerNoChange succeeds on default and value stays 'General'");
        openCreateFormGuarded("TC_WTC_PICK_065");
        try {
            openPickerGuarded("TC_WTC_PICK_065");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "closeWorkTypePickerNoChange (re-tap selected row — the only safe no-op close; "
                    + "swipe does NOT dismiss) must return true");
            assertEquals(wo.getCreateFormWorkTypeValue(), WorkTypeCatalog.GENERAL.displayName(),
                    "Row value must remain the default 'General' after a no-op close");
            logStepWithScreenshot("TC_WTC_PICK_065 verified: no-op close kept 'General'");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 66)
    public void TC_WTC_PICK_066_noopCloseAfterCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_066 - no-op close after selecting 'Cleaning' leaves the value 'Cleaning'");
        openCreateFormGuarded("TC_WTC_PICK_066");
        try {
            openPickerGuarded("TC_WTC_PICK_066");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.CLEANING.displayName()),
                    "Selecting 'Cleaning' must commit and close");
            openPickerGuarded("TC_WTC_PICK_066");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "closeWorkTypePickerNoChange must return true on the reopened sheet");
            assertEquals(wo.getCreateFormWorkTypeValue(), WorkTypeCatalog.CLEANING.displayName(),
                    "Row value must remain 'Cleaning' after the no-op close");
            logStepWithScreenshot("TC_WTC_PICK_066 verified: no-op close kept 'Cleaning'");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 67)
    public void TC_WTC_PICK_067_noopCloseAfterCommaName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_067 - no-op close after 'Clean, Tighten, Torque' leaves the comma value intact");
        openCreateFormGuarded("TC_WTC_PICK_067");
        try {
            openPickerGuarded("TC_WTC_PICK_067");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE.displayName()),
                    "Selecting 'Clean, Tighten, Torque' must commit and close");
            openPickerGuarded("TC_WTC_PICK_067");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "closeWorkTypePickerNoChange must return true on the reopened sheet");
            assertEquals(wo.getCreateFormWorkTypeValue(), WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE.displayName(),
                    "Row value must remain 'Clean, Tighten, Torque' — commas intact — after the no-op close");
            logStepWithScreenshot("TC_WTC_PICK_067 verified: no-op close kept the comma value");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 68)
    public void TC_WTC_PICK_068_noopCloseActuallyClosesSheet() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_068 - after closeWorkTypePickerNoChange the sheet is CLOSED");
        openCreateFormGuarded("TC_WTC_PICK_068");
        try {
            openPickerGuarded("TC_WTC_PICK_068");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "closeWorkTypePickerNoChange must return true");
            assertFalse(wo.isWorkTypePickerOpen(),
                    "Sheet must be gone after the no-op close (its 'Work Type' nav bar dismissed)");
            logStepWithScreenshot("TC_WTC_PICK_068 verified: no-op close dismissed the sheet");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 69)
    public void TC_WTC_PICK_069_noopCloseValueIdenticalAcrossReopen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_069 - value is IDENTICAL before and after a reopen + no-op close cycle");
        openCreateFormGuarded("TC_WTC_PICK_069");
        try {
            openPickerGuarded("TC_WTC_PICK_069");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.NETA_TESTING.displayName()),
                    "Selecting 'NETA Testing' must commit and close");
            String before = wo.getCreateFormWorkTypeValue();
            assertEquals(before, WorkTypeCatalog.NETA_TESTING.displayName(),
                    "Pre-cycle value must be the committed 'NETA Testing'");
            openPickerGuarded("TC_WTC_PICK_069");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "closeWorkTypePickerNoChange must return true");
            String after = wo.getCreateFormWorkTypeValue();
            logStep("TC_WTC_PICK_069 value before='" + before + "' after='" + after + "'");
            assertEquals(after, before,
                    "Reopen + no-op close must leave the row value byte-identical");
            logStepWithScreenshot("TC_WTC_PICK_069 verified: value unchanged across the cycle");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 70)
    public void TC_WTC_PICK_070_reopenAfterCommitStillFullCensus() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_070 - picker reopens after a commit and still shows the full 14-option census");
        openCreateFormGuarded("TC_WTC_PICK_070");
        try {
            openPickerGuarded("TC_WTC_PICK_070");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.CONDITION_ASSESSMENT.displayName()),
                    "Selecting 'Condition Assessment' must commit and close");
            assertTrue(wo.openWorkTypePicker(),
                    "Picker must reopen after a commit (open→commit→open must not wedge the row)");
            List<String> census = censusLogged("TC_WTC_PICK_070");
            assertEquals(census.size(), 14,
                    "Reopened sheet must still expose all 14 options — got " + census);
            logStepWithScreenshot("TC_WTC_PICK_070 verified: reopen after commit shows full census");
        } finally {
            formHygiene();
        }
    }

    // ═══════════════ Block 7 — STABILITY guards (071..076) ═══════════════

    @Test(priority = 71)
    public void TC_WTC_PICK_071_healthyAfterOpen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_071 - app alive, rendering, alert-free with the sheet OPEN");
        openCreateFormGuarded("TC_WTC_PICK_071");
        try {
            openPickerGuarded("TC_WTC_PICK_071");
            verifyAppAlive("TC_WTC_PICK_071: sheet open");
            // NEVER verifyNotBlank with the sheet stacked: the unbounded
            // visible==1 census over form+sheet wedges WDA until the SESSION
            // DIES (observed live 2026-08-04, 2m40s → 'session terminated').
            // Bounded typed census proves rendering instead (giant-DOM rule).
            assertEquals(Integer.valueOf(wo.getWorkTypePickerOptions(14).size()), Integer.valueOf(14),
                    "Sheet must render all 14 option rows (bounded census — rendering proof)");
            verifyNoErrorAlert();
            logStepWithScreenshot("TC_WTC_PICK_071 verified: healthy with sheet open");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 72)
    public void TC_WTC_PICK_072_healthyAfterSelect() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_072 - app alive, rendering, alert-free right after a tap-commit");
        openCreateFormGuarded("TC_WTC_PICK_072");
        try {
            openPickerGuarded("TC_WTC_PICK_072");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.INSULATION_RESISTANCE.displayName()),
                    "Selecting 'Insulation Resistance Testing' must commit and close");
            verifyAppAlive("TC_WTC_PICK_072: after commit");
            verifyNotBlank("Create form after Work Type commit (TC_WTC_PICK_072)");
            verifyNoErrorAlert();
            logStepWithScreenshot("TC_WTC_PICK_072 verified: healthy after tap-commit");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 73)
    public void TC_WTC_PICK_073_healthyAfterNoopClose() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_073 - app alive, rendering, alert-free right after the no-op close");
        openCreateFormGuarded("TC_WTC_PICK_073");
        try {
            openPickerGuarded("TC_WTC_PICK_073");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "closeWorkTypePickerNoChange must return true");
            verifyAppAlive("TC_WTC_PICK_073: after no-op close");
            verifyNotBlank("Create form after no-op picker close (TC_WTC_PICK_073)");
            verifyNoErrorAlert();
            logStepWithScreenshot("TC_WTC_PICK_073 verified: healthy after no-op close");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 74)
    public void TC_WTC_PICK_074_threeCyclesStayHealthy() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_074 - 3 consecutive open→select→reopen cycles stay healthy");
        final WorkTypeCatalog[] cycle = {
                WorkTypeCatalog.CLEANING,
                WorkTypeCatalog.NETA_TESTING,
                WorkTypeCatalog.UPS_MAINTENANCE,
        };
        openCreateFormGuarded("TC_WTC_PICK_074");
        try {
            for (int i = 0; i < cycle.length; i++) {
                String tag = "TC_WTC_PICK_074 cycle " + (i + 1) + " (" + cycle[i].displayName() + ")";
                openPickerGuarded("TC_WTC_PICK_074");
                assertTrue(wo.selectWorkTypeInPicker(cycle[i].displayName()),
                        tag + ": selection must commit and close");
                openPickerGuarded("TC_WTC_PICK_074");
                assertEquals(wo.getSelectedWorkTypeInPicker(), cycle[i].displayName(),
                        tag + ": reopen must mark the latest commit");
                assertTrue(wo.closeWorkTypePickerNoChange(),
                        tag + ": no-op close must succeed");
                verifyAppAlive(tag);
                verifyNoErrorAlert();
                logStep(tag + " healthy");
            }
            verifyNotBlank("Create form after 3 picker cycles (TC_WTC_PICK_074)");
            logStepWithScreenshot("TC_WTC_PICK_074 verified: 3 cycles, app healthy throughout");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 75)
    public void TC_WTC_PICK_075_doubleCensusReadIdentical() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_075 - two census reads on one open sheet are identical (content + order)");
        openCreateFormGuarded("TC_WTC_PICK_075");
        try {
            openPickerGuarded("TC_WTC_PICK_075");
            List<String> first = censusLogged("TC_WTC_PICK_075 read-1");
            List<String> second = censusLogged("TC_WTC_PICK_075 read-2");
            assertEquals(first.size(), 14,
                    "First census read must return the full 14 options — got " + first);
            assertEquals(second, first,
                    "Second census read on the SAME open sheet must be identical in content and order "
                    + "(no reflow/ghost rows)");
            logStepWithScreenshot("TC_WTC_PICK_075 verified: census stable across double read");
        } finally {
            formHygiene();
        }
    }

    @Test(priority = 76)
    public void TC_WTC_PICK_076_cancelRestoresWorkOrdersList() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_PICK_076 - Cancel after picker use restores a healthy Work Orders list");
        openCreateFormGuarded("TC_WTC_PICK_076");
        boolean canceled = false;
        try {
            openPickerGuarded("TC_WTC_PICK_076");
            assertTrue(wo.selectWorkTypeInPicker(WorkTypeCatalog.INFRARED_THERMOGRAPHY.displayName()),
                    "Selecting 'Infrared Thermography' must commit and close");
            wo.cancelCreateForm();
            shortWait();
            canceled = true;
            assertTrue(wo.waitForWorkOrdersScreen(),
                    "Cancel must land back on the Work Orders list (form discarded — nothing created)");
            verifyAppAlive("TC_WTC_PICK_076: list restored");
            verifyNotBlank("Work Orders list after create-form Cancel (TC_WTC_PICK_076)");
            verifyNoErrorAlert();
            logStepWithScreenshot("TC_WTC_PICK_076 verified: Cancel restored the list, app healthy");
        } finally {
            if (!canceled) formHygiene();
        }
    }
}
