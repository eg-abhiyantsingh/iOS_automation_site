package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;

import io.appium.java_client.AppiumBy;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * WorkTypeCreateForm_Test — TC_WTC_FORM_* — the v1.55 "New Work Order" create
 * form's required Work Type ROW (design doc Class 1,
 * docs/worktype-create-dropdown-design-2026-08-04.md; catalog truth
 * docs/worktype-gold-spec-2026-07-21.md).
 *
 * All locators and interaction semantics are PROBE-PINNED (PROBE_E/F/G,
 * 2026-08-04, app v1.55):
 *  - form nav: Cancel / 'New Work Order' / Create; config rows are Buttons
 *    named '&lt;Label&gt;, &lt;value&gt;' ('Photo Type, FLIR-SEP',
 *    'Priority, Medium', 'Equipment, None');
 *  - the Work Type row is 'Work Type, *, &lt;value&gt;' — the required marker
 *    '*' is its OWN middle segment, so the value MUST be prefix-parsed
 *    (wo.getCreateFormWorkTypeValue()); the naive last-comma-segment parse
 *    (getCreateFormRowValue) shreds 'Clean, Tighten, Torque' → 'Torque';
 *  - picker = stacked bottom sheet with its own 'Work Type' NavigationBar,
 *    14 full-width option Buttons all on screen (General first, then the 13
 *    display names in case-sensitive lexicographic order), radio semantics
 *    (selected row value=='1'/selected==true, checkmark.circle.fill);
 *  - a CENTER TAP on an option COMMITS AND CLOSES instantly — there is NO
 *    sheet Done, swipe-down does NOT dismiss; the only safe no-op close is
 *    re-tapping the selected row (wo.closeWorkTypePickerNoChange()).
 *
 * BLOCKS: 1 row anatomy (001-010), 2 value-readback matrix ×14 (011-024,
 * runner runValueReadback), 3 comma-name contract (025-028), 4 state
 * persistence (029-040), 5 stability (041-046).
 *
 * HARD POLICY: this class NEVER taps Create — no Work Order is ever created,
 * so there is nothing to clean up. Every test recovers to the Work Orders
 * list (picker-aware cancel in a finally). Driver lifecycle / noReset(true)
 * skeleton is inherited from WorkTypeBaseTest exactly like WorkType_List_Test
 * (wtClassSetup/wtClassTeardown/wtInitPage).
 */
public class WorkTypeCreateForm_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "Work Type Create Dropdown (v1.55)";

    private static final String DEFAULT_WORK_TYPE = WorkTypeCatalog.GENERAL.displayName(); // "General"

    // ── type-bound direct locators (PROBE_E-pinned; used only where no page
    //    primitive exists — rect.y ordering, sibling-row regression, nav gating)
    private static final By ROW_PRIORITY = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Priority,' AND visible == 1");
    private static final By ROW_PHOTO_TYPE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Photo Type,' AND visible == 1");
    private static final By ROW_EQUIPMENT = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Equipment,' AND visible == 1");
    private static final By ROW_WORK_TYPE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Work Type,' AND visible == 1");
    private static final By LABEL_WORK_TYPE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name == 'Work Type' AND visible == 1");
    private static final By BTN_CREATE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name == 'Create' AND visible == 1");
    private static final By NAME_TEXT_FIELD = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeTextField' AND visible == 1");

    // ────────────────────────── shared plumbing ──────────────────────────

    /** Entry: login → Work Orders list → open the create form, crash-guarded. */
    private void openFormGuarded(String tc) {
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(),
                "'New Work Order' create form must open from the Work Orders list (" + tc + ")");
        verifyAppAlive(tc + ": create form open");
    }

    /**
     * Navigation hygiene for finally blocks: close the picker sheet if it is
     * still up (re-tap selected row — the ONLY safe no-op close, PROBE_F),
     * then cancel the form and settle on the Work Orders list. Best-effort by
     * design — contract asserts never live here.
     */
    private void recoverToList(String tc) {
        try {
            if (wo.isWorkTypePickerOpen()) {
                wo.closeWorkTypePickerNoChange();
            }
            wo.cancelCreateForm();
            wo.waitForWorkOrdersScreen();
        } catch (Exception e) {
            System.out.println("⚠️ " + tc + " recoverToList: " + e.getMessage());
        }
    }

    /** Presence probe for the type-bound direct locators above. */
    private boolean elementPresent(By locator) {
        try {
            return !DriverManager.getDriver().findElements(locator).isEmpty();
        } catch (Exception e) {
            System.out.println("⚠️ elementPresent: " + e.getMessage());
            return false;
        }
    }

    /**
     * Block-2 runner — value-readback contract for one catalog entry:
     * fresh form → open picker → tap the option (commits + closes, verified
     * inside selectWorkTypeInPicker) → the ROW must read back EXACTLY the
     * display name via the comma-safe prefix parser.
     */
    private void runValueReadback(WorkTypeCatalog wt, String tc) {
        String name = wt.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(),
                    "Work Type picker sheet must open from the create-form row (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(name),
                    "Center-tap on option '" + name + "' must commit and close the sheet (verified select)");
            assertEquals(wo.getCreateFormWorkTypeValue(), name,
                    "Work Type row must read back exactly '" + name + "' (comma-safe prefix parse)");
            verifyAppAlive(tc + ": readback verified for '" + name + "'");
            logStepWithScreenshot(tc + " verified: row reads '" + name + "'");
        } finally {
            recoverToList(tc);
        }
    }

    /**
     * Block-3 runner — punctuation-bearing display name round-trips INTACT:
     * value equals the full display name, is NOT a comma-split fragment, and
     * the required-marker '*' segment survives the selection.
     */
    private void runCommaNameDeep(WorkTypeCatalog wt, String tc) {
        String name = wt.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(),
                    "Work Type picker sheet must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(name),
                    "Option '" + name + "' must commit and close (" + tc + ")");
            String value = wo.getCreateFormWorkTypeValue();
            logStep(tc + " row value read back: '" + value + "'");
            assertEquals(value, name,
                    "Row must carry the FULL display name '" + name
                    + "' — any shorter string is the comma-split regression this test pins");
            String naiveFragment = WorkOrderPage.rowPriority("Work Type, *, " + name);
            if (!naiveFragment.equals(name)) {
                // Comma-bearing name: the naive last-segment parse diverges —
                // pin that the primitive never regresses to it.
                assertTrue(!naiveFragment.equals(value),
                        "Row value must NOT equal the naive last-comma-segment fragment '"
                        + naiveFragment + "' (getCreateFormRowValue-style parsing is banned for Work Type)");
            }
            assertTrue(wo.workTypeRowHasRequiredMarker(),
                    "Required marker '*' segment must survive selecting '" + name + "'");
            logStepWithScreenshot(tc + " verified: '" + name + "' round-trips intact with marker");
        } finally {
            recoverToList(tc);
        }
    }

    // ═══════════════ Block 1 — row anatomy (TC_WTC_FORM_001-010) ═══════════════

    @Test(priority = 1)
    public void TC_WTC_FORM_001_workTypeRowPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_001 - create form carries the v1.55 Work Type config row");
        String tc = "TC_WTC_FORM_001";
        openFormGuarded(tc);
        try {
            assertTrue(wo.isCreateFormWorkTypeRowPresent(),
                    "v1.55 create form must carry the 'Work Type, *, <value>' row Button");
            logStepWithScreenshot(tc + " verified: Work Type row present");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 2)
    public void TC_WTC_FORM_002_requiredMarkerPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_002 - Work Type row carries the required-marker '*' segment");
        String tc = "TC_WTC_FORM_002";
        openFormGuarded(tc);
        try {
            assertTrue(wo.workTypeRowHasRequiredMarker(),
                    "Work Type row name must carry the '*' required-marker as its own segment "
                    + "('Work Type, *, <value>')");
            logStepWithScreenshot(tc + " verified: required marker present");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 3)
    public void TC_WTC_FORM_003_defaultValueIsGeneral() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_003 - fresh create form defaults the Work Type value to 'General'");
        String tc = "TC_WTC_FORM_003";
        openFormGuarded(tc);
        try {
            assertEquals(wo.getCreateFormWorkTypeValue(), DEFAULT_WORK_TYPE,
                    "Fresh create form must default the Work Type row value to 'General'");
            logStepWithScreenshot(tc + " verified: default value is General");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 4)
    public void TC_WTC_FORM_004_rowSitsBelowPriorityRow() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_004 - Work Type row renders below the Priority row (form order)");
        String tc = "TC_WTC_FORM_004";
        openFormGuarded(tc);
        try {
            WebElement priorityRow = DriverManager.getDriver().findElement(ROW_PRIORITY);
            WebElement workTypeRow = DriverManager.getDriver().findElement(ROW_WORK_TYPE);
            int priorityY = priorityRow.getRect().y;
            int workTypeY = workTypeRow.getRect().y;
            logStep(tc + " rect.y: Priority=" + priorityY + " WorkType=" + workTypeY);
            assertTrue(workTypeY > priorityY,
                    "Work Type row (y=" + workTypeY + ") must render BELOW the Priority row (y="
                    + priorityY + ") — probe-pinned form order");
            logStepWithScreenshot(tc + " verified: row order Priority → Work Type");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 5)
    public void TC_WTC_FORM_005_workTypeLabelStaticTextPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_005 - StaticText 'Work Type' label renders on the create form");
        String tc = "TC_WTC_FORM_005";
        openFormGuarded(tc);
        try {
            assertTrue(elementPresent(LABEL_WORK_TYPE),
                    "A visible StaticText named 'Work Type' must render on the create form (row label)");
            logStepWithScreenshot(tc + " verified: 'Work Type' label StaticText present");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 6)
    public void TC_WTC_FORM_006_photoTypeRowStillPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_006 - regression: Photo Type row survives the v1.55 form change");
        String tc = "TC_WTC_FORM_006";
        openFormGuarded(tc);
        try {
            assertTrue(elementPresent(ROW_PHOTO_TYPE),
                    "Create form must still carry the 'Photo Type, <value>' row (v1.55 regression guard)");
            logStepWithScreenshot(tc + " verified: Photo Type row present");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 7)
    public void TC_WTC_FORM_007_priorityRowStillPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_007 - regression: Priority row survives the v1.55 form change");
        String tc = "TC_WTC_FORM_007";
        openFormGuarded(tc);
        try {
            assertTrue(elementPresent(ROW_PRIORITY),
                    "Create form must still carry the 'Priority, <value>' row (v1.55 regression guard)");
            logStepWithScreenshot(tc + " verified: Priority row present");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 8)
    public void TC_WTC_FORM_008_equipmentRowStillPresent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_008 - regression: Equipment row survives the v1.55 form change");
        String tc = "TC_WTC_FORM_008";
        openFormGuarded(tc);
        try {
            assertTrue(elementPresent(ROW_EQUIPMENT),
                    "Create form must still carry the 'Equipment, <value>' row (v1.55 regression guard)");
            logStepWithScreenshot(tc + " verified: Equipment row present");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 9)
    public void TC_WTC_FORM_009_openFormRendersCleanly() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_009 - open create form renders content and shows no error alert");
        String tc = "TC_WTC_FORM_009";
        openFormGuarded(tc);
        try {
            verifyNotBlank("New Work Order create form (" + tc + ")");
            verifyNoErrorAlert();
            assertTrue(wo.isCreateFormWorkTypeRowPresent(),
                    "Rendered form must include the Work Type row (render-integrity anchor)");
            logStepWithScreenshot(tc + " verified: form renders clean, alert-free");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 10)
    public void TC_WTC_FORM_010_rowSurvivesAliveProbe() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_010 - Work Type row still present after the app-alive probe");
        String tc = "TC_WTC_FORM_010";
        openFormGuarded(tc);
        try {
            verifyAppAlive(tc + ": alive probe on the open create form");
            assertTrue(wo.isCreateFormWorkTypeRowPresent(),
                    "Work Type row must still be present after verifyAppAlive (no transient render)");
            logStepWithScreenshot(tc + " verified: row survives alive probe");
        } finally {
            recoverToList(tc);
        }
    }

    // ═══════ Block 2 — value-readback matrix, all 14 catalog entries ═══════
    // (TC_WTC_FORM_011-024; runner runValueReadback)

    @Test(priority = 11)
    public void TC_WTC_FORM_011_readbackGeneral() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_011 - select 'General' → row reads back exactly 'General'");
        runValueReadback(WorkTypeCatalog.GENERAL, "TC_WTC_FORM_011");
    }

    @Test(priority = 12)
    public void TC_WTC_FORM_012_readbackArcFlashDataCollection() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_012 - select 'Arc Flash Data Collection' → exact row readback");
        runValueReadback(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WTC_FORM_012");
    }

    @Test(priority = 13)
    public void TC_WTC_FORM_013_readbackArcFlashLabelPlacement() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_013 - select 'Arc Flash Label Placement' → exact row readback");
        runValueReadback(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WTC_FORM_013");
    }

    @Test(priority = 14)
    public void TC_WTC_FORM_014_readbackCleaning() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_014 - select 'Cleaning' → exact row readback");
        runValueReadback(WorkTypeCatalog.CLEANING, "TC_WTC_FORM_014");
    }

    @Test(priority = 15)
    public void TC_WTC_FORM_015_readbackCleanTightenTorque() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_015 - select 'Clean, Tighten, Torque' → exact row readback");
        runValueReadback(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WTC_FORM_015");
    }

    @Test(priority = 16)
    public void TC_WTC_FORM_016_readbackConditionAssessment() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_016 - select 'Condition Assessment' → exact row readback");
        runValueReadback(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WTC_FORM_016");
    }

    @Test(priority = 17)
    public void TC_WTC_FORM_017_readbackDeEnergizedVisual() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_017 - select 'De-Energized Visual Inspection' → exact row readback");
        runValueReadback(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WTC_FORM_017");
    }

    @Test(priority = 18)
    public void TC_WTC_FORM_018_readbackDgaFluidSample() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_018 - select 'DGA / Fluid Sample Analysis' → exact row readback");
        runValueReadback(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WTC_FORM_018");
    }

    @Test(priority = 19)
    public void TC_WTC_FORM_019_readbackInfraredThermography() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_019 - select 'Infrared Thermography' → exact row readback");
        runValueReadback(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WTC_FORM_019");
    }

    @Test(priority = 20)
    public void TC_WTC_FORM_020_readbackInsulationResistance() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_020 - select 'Insulation Resistance Testing' → exact row readback");
        runValueReadback(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WTC_FORM_020");
    }

    @Test(priority = 21)
    public void TC_WTC_FORM_021_readbackNetaTesting() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_021 - select 'NETA Testing' → exact row readback");
        runValueReadback(WorkTypeCatalog.NETA_TESTING, "TC_WTC_FORM_021");
    }

    @Test(priority = 22)
    public void TC_WTC_FORM_022_readbackPanelScheduleUpdates() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_022 - select 'Panel Schedule Updates' → exact row readback");
        runValueReadback(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WTC_FORM_022");
    }

    @Test(priority = 23)
    public void TC_WTC_FORM_023_readbackShutdownComposite() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_023 - select 'Shutdown (Composite)' → exact row readback");
        runValueReadback(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WTC_FORM_023");
    }

    @Test(priority = 24)
    public void TC_WTC_FORM_024_readbackUpsMaintenance() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_024 - select 'UPS Maintenance' → exact row readback");
        runValueReadback(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WTC_FORM_024");
    }

    // ═══════ Block 3 — comma/punctuation-name contract (TC_WTC_FORM_025-028) ═══════

    @Test(priority = 25)
    public void TC_WTC_FORM_025_commaNameCleanTightenTorqueIntact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_025 - 'Clean, Tighten, Torque' round-trips intact (comma-embedded name)");
        runCommaNameDeep(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WTC_FORM_025");
    }

    @Test(priority = 26)
    public void TC_WTC_FORM_026_punctuationNameDgaFluidSampleIntact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_026 - 'DGA / Fluid Sample Analysis' round-trips intact (slash-embedded name)");
        runCommaNameDeep(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WTC_FORM_026");
    }

    @Test(priority = 27)
    public void TC_WTC_FORM_027_punctuationNameShutdownCompositeIntact() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_027 - 'Shutdown (Composite)' round-trips intact (parenthesized name)");
        runCommaNameDeep(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WTC_FORM_027");
    }

    @Test(priority = 28)
    public void TC_WTC_FORM_028_antiCommaSplitPin() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_028 - anti-regression pin: row value is never a comma-split fragment");
        String tc = "TC_WTC_FORM_028";
        String full = WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE.displayName(); // 'Clean, Tighten, Torque'
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(),
                    "Work Type picker sheet must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(full),
                    "Option '" + full + "' must commit and close (" + tc + ")");
            String value = wo.getCreateFormWorkTypeValue();
            logStep(tc + " row value read back: '" + value + "'");
            assertEquals(value, full,
                    "Row value must be the FULL comma-embedded display name");
            // Explicit pins against every naive comma-split fragment — the exact
            // failure mode of last-segment parsing on 'Work Type, *, Clean, Tighten, Torque'.
            assertTrue(!"Torque".equals(value),
                    "Anti-regression pin: value must never collapse to the last-segment fragment 'Torque'");
            assertTrue(!"Tighten, Torque".equals(value),
                    "Anti-regression pin: value must never collapse to the fragment 'Tighten, Torque'");
            assertTrue(!"Clean".equals(value),
                    "Anti-regression pin: value must never collapse to the first-segment fragment 'Clean'");
            logStepWithScreenshot(tc + " verified: no comma-split fragment ever surfaces");
        } finally {
            recoverToList(tc);
        }
    }

    // ═══════ Block 4 — state persistence (TC_WTC_FORM_029-040) ═══════

    @Test(priority = 29)
    public void TC_WTC_FORM_029_selectionSurvivesPickerReopen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_029 - committed selection is radio-marked when the picker reopens");
        String tc = "TC_WTC_FORM_029";
        String pick = WorkTypeCatalog.INFRARED_THERMOGRAPHY.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "First picker open must succeed (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit and close (" + tc + ")");
            assertTrue(wo.openWorkTypePicker(), "Picker must reopen after a committed selection");
            assertTrue(wo.isWorkTypeOptionSelected(pick),
                    "Reopened sheet must radio-mark the committed selection '" + pick
                    + "' (value=='1' / checkmark.circle.fill)");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "No-op close (re-tap selected row) must dismiss the sheet");
            assertEquals(wo.getCreateFormWorkTypeValue(), pick,
                    "Row value must still read '" + pick + "' after the reopen + no-op close");
            logStepWithScreenshot(tc + " verified: selection survives picker reopen");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 30)
    public void TC_WTC_FORM_030_freshFormPickerMarksGeneralSelected() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_030 - fresh form's picker opens with 'General' radio-marked");
        String tc = "TC_WTC_FORM_030";
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open on a fresh form (" + tc + ")");
            assertEquals(wo.getSelectedWorkTypeInPicker(), DEFAULT_WORK_TYPE,
                    "Fresh form's picker must radio-mark the default 'General' as selected");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "No-op close must dismiss the sheet (" + tc + ")");
            logStepWithScreenshot(tc + " verified: default radio state is General");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 31)
    public void TC_WTC_FORM_031_selectionPersistsWhileEditingName() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_031 - Work Type selection persists while editing the Name field");
        String tc = "TC_WTC_FORM_031";
        String pick = WorkTypeCatalog.CLEANING.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit and close (" + tc + ")");
            WebElement nameField = DriverManager.getDriver().findElement(NAME_TEXT_FIELD);
            nameField.click();
            shortWait();
            nameField.sendKeys(" X");
            wo.dismissKeyboard();
            mediumWait();
            assertEquals(wo.getCreateFormWorkTypeValue(), pick,
                    "Work Type row must still read '" + pick + "' after typing into the Name field");
            logStepWithScreenshot(tc + " verified: selection unaffected by name edit");
        } finally {
            recoverToList(tc); // form is cancelled — the name edit is never persisted
        }
    }

    @Test(priority = 32)
    public void TC_WTC_FORM_032_cancelResetsFormToGeneral() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_032 - cancel → reopen: fresh form defaults back to 'General' (no state leak)");
        String tc = "TC_WTC_FORM_032";
        String pick = WorkTypeCatalog.NETA_TESTING.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit and close (" + tc + ")");
            assertEquals(wo.getCreateFormWorkTypeValue(), pick,
                    "Sanity: row must read '" + pick + "' before the cancel");
            wo.cancelCreateForm();
            assertTrue(wo.waitForWorkOrdersScreen(),
                    "Cancel must land back on the Work Orders list");
            assertTrue(wo.openCreateForm(),
                    "Create form must reopen for the state-leak probe");
            assertEquals(wo.getCreateFormWorkTypeValue(), DEFAULT_WORK_TYPE,
                    "Reopened form must default back to 'General' — a cancelled selection must not leak");
            logStepWithScreenshot(tc + " verified: no state leak across form opens");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 33)
    public void TC_WTC_FORM_033_openCloseCyclesIdempotent() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_033 - two picker open + no-op-close cycles leave the value untouched");
        String tc = "TC_WTC_FORM_033";
        openFormGuarded(tc);
        try {
            for (int cycle = 1; cycle <= 2; cycle++) {
                assertTrue(wo.openWorkTypePicker(),
                        "Picker must open on cycle " + cycle + " (" + tc + ")");
                assertTrue(wo.closeWorkTypePickerNoChange(),
                        "No-op close must dismiss the sheet on cycle " + cycle);
                assertEquals(wo.getCreateFormWorkTypeValue(), DEFAULT_WORK_TYPE,
                        "Value must remain 'General' after open/close cycle " + cycle);
            }
            logStepWithScreenshot(tc + " verified: open/close dance is idempotent");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 34)
    public void TC_WTC_FORM_034_reselectSameValueKeepsValue() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_034 - selecting the same value twice keeps the value");
        String tc = "TC_WTC_FORM_034";
        String pick = WorkTypeCatalog.UPS_MAINTENANCE.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "First picker open must succeed (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "First selection of '" + pick + "' must commit (" + tc + ")");
            assertTrue(wo.openWorkTypePicker(), "Picker must reopen for the re-selection");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Re-selecting the already-selected '" + pick + "' must commit and close (same-value tap)");
            assertEquals(wo.getCreateFormWorkTypeValue(), pick,
                    "Row value must still read '" + pick + "' after selecting it twice");
            logStepWithScreenshot(tc + " verified: same-value re-selection is stable");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 35)
    public void TC_WTC_FORM_035_selectAThenBEndsWithB() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_035 - selecting A then B leaves the row reading B");
        String tc = "TC_WTC_FORM_035";
        String a = WorkTypeCatalog.CLEANING.displayName();
        String b = WorkTypeCatalog.CONDITION_ASSESSMENT.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open for selection A (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(a),
                    "Selection A ('" + a + "') must commit (" + tc + ")");
            assertTrue(wo.openWorkTypePicker(), "Picker must reopen for selection B");
            assertTrue(wo.selectWorkTypeInPicker(b),
                    "Selection B ('" + b + "') must commit (" + tc + ")");
            assertEquals(wo.getCreateFormWorkTypeValue(), b,
                    "Row must read the LAST committed selection '" + b + "', not '" + a + "'");
            logStepWithScreenshot(tc + " verified: A→B ends with B");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 36)
    public void TC_WTC_FORM_036_createButtonSurvivesPickerDance() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_036 - Create button still present after the full picker dance");
        String tc = "TC_WTC_FORM_036";
        String pick = WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit (" + tc + ")");
            assertTrue(wo.openWorkTypePicker(), "Picker must reopen (" + tc + ")");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "No-op close must dismiss the sheet (" + tc + ")");
            assertTrue(elementPresent(BTN_CREATE),
                    "Create button must still be present on the form after open→select→reopen→close");
            logStepWithScreenshot(tc + " verified: Create button intact after picker dance");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 37)
    public void TC_WTC_FORM_037_requiredMarkerPersistsAfterSelection() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_037 - required marker '*' persists after a selection");
        String tc = "TC_WTC_FORM_037";
        String pick = WorkTypeCatalog.PANEL_SCHEDULE_UPDATES.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit (" + tc + ")");
            assertTrue(wo.workTypeRowHasRequiredMarker(),
                    "Required marker '*' segment must persist after selecting '" + pick + "'");
            logStepWithScreenshot(tc + " verified: marker persists post-selection");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 38)
    public void TC_WTC_FORM_038_noOpClosePreservesPriorSelection() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_038 - no-op close leaves a prior non-default selection untouched");
        String tc = "TC_WTC_FORM_038";
        String pick = WorkTypeCatalog.NETA_TESTING.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit (" + tc + ")");
            assertTrue(wo.openWorkTypePicker(), "Picker must reopen (" + tc + ")");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "No-op close must dismiss the sheet without changing the value");
            assertEquals(wo.getCreateFormWorkTypeValue(), pick,
                    "Row must still read '" + pick + "' after the no-op close");
            logStepWithScreenshot(tc + " verified: no-op close preserves selection");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 39)
    public void TC_WTC_FORM_039_selectionSurvivesTwoReopenCycles() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_039 - selection stays radio-marked across TWO reopen/no-op-close cycles");
        String tc = "TC_WTC_FORM_039";
        String pick = WorkTypeCatalog.INSULATION_RESISTANCE.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit (" + tc + ")");
            for (int cycle = 1; cycle <= 2; cycle++) {
                assertTrue(wo.openWorkTypePicker(),
                        "Picker must reopen on cycle " + cycle + " (" + tc + ")");
                assertTrue(wo.isWorkTypeOptionSelected(pick),
                        "Cycle " + cycle + ": reopened sheet must still radio-mark '" + pick + "'");
                assertTrue(wo.closeWorkTypePickerNoChange(),
                        "Cycle " + cycle + ": no-op close must dismiss the sheet");
            }
            assertEquals(wo.getCreateFormWorkTypeValue(), pick,
                    "Row must still read '" + pick + "' after two reopen/no-op-close cycles");
            logStepWithScreenshot(tc + " verified: selection stable across 2 reopen cycles");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 40)
    public void TC_WTC_FORM_040_nameFieldDefaultUnaffectedBySelection() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_040 - Work Type selection does not clobber the Name field default");
        String tc = "TC_WTC_FORM_040";
        String pick = WorkTypeCatalog.DGA_FLUID_SAMPLE.displayName();
        openFormGuarded(tc);
        try {
            WebElement nameField = DriverManager.getDriver().findElement(NAME_TEXT_FIELD);
            String before = nameField.getAttribute("value");
            logStep(tc + " Name field default before selection: '" + before + "'");
            assertTrue(before != null && !before.isEmpty(),
                    "Name field must carry a non-empty default ('Work Order - <date>' per PROBE_E)");
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit (" + tc + ")");
            String after = DriverManager.getDriver().findElement(NAME_TEXT_FIELD).getAttribute("value");
            assertEquals(after, before,
                    "Name field value must be untouched by the Work Type selection");
            logStepWithScreenshot(tc + " verified: name default intact after selection");
        } finally {
            recoverToList(tc);
        }
    }

    // ═══════ Block 5 — stability guards (TC_WTC_FORM_041-046) ═══════

    @Test(priority = 41)
    public void TC_WTC_FORM_041_healthyWithPickerOpen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_041 - app alive, rendering, alert-free WHILE the picker sheet is open");
        String tc = "TC_WTC_FORM_041";
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            verifyAppAlive(tc + ": picker sheet open");
            verifyNotBlank("Work Type picker sheet (" + tc + ")");
            verifyNoErrorAlert();
            logStepWithScreenshot(tc + " verified: healthy with sheet up");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 42)
    public void TC_WTC_FORM_042_healthyAfterSelection() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_042 - app alive, rendering, alert-free after a selection commit");
        String tc = "TC_WTC_FORM_042";
        String pick = WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit (" + tc + ")");
            verifyAppAlive(tc + ": selection committed");
            verifyNotBlank("Create form after selecting '" + pick + "' (" + tc + ")");
            verifyNoErrorAlert();
            logStepWithScreenshot(tc + " verified: healthy after selection");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 43)
    public void TC_WTC_FORM_043_healthyAfterNoOpClose() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_043 - app alive, rendering, alert-free after a no-op close; value intact");
        String tc = "TC_WTC_FORM_043";
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "No-op close (re-tap selected row) must dismiss the sheet (" + tc + ")");
            verifyAppAlive(tc + ": no-op close done");
            verifyNotBlank("Create form after no-op close (" + tc + ")");
            verifyNoErrorAlert();
            assertEquals(wo.getCreateFormWorkTypeValue(), DEFAULT_WORK_TYPE,
                    "No-op close must leave the default 'General' untouched");
            logStepWithScreenshot(tc + " verified: healthy after no-op close");
        } finally {
            recoverToList(tc);
        }
    }

    @Test(priority = 44)
    public void TC_WTC_FORM_044_healthyAfterFormCancel() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_044 - app alive, rendering, alert-free after cancelling a form with a selection");
        String tc = "TC_WTC_FORM_044";
        String pick = WorkTypeCatalog.SHUTDOWN_COMPOSITE.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Picker must open (" + tc + ")");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Selecting '" + pick + "' must commit (" + tc + ")");
            wo.cancelCreateForm();
            assertTrue(wo.waitForWorkOrdersScreen(),
                    "Cancel must land back on the Work Orders list");
            verifyAppAlive(tc + ": form cancelled");
            verifyNotBlank("Work Orders list after form cancel (" + tc + ")");
            verifyNoErrorAlert();
            logStepWithScreenshot(tc + " verified: healthy after form cancel");
        } finally {
            recoverToList(tc); // no-op when already on the list
        }
    }

    @Test(priority = 45)
    public void TC_WTC_FORM_045_twoFullCyclesLeaveListHealthy() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_045 - two back-to-back open/select/cancel cycles leave the WO list healthy");
        String tc = "TC_WTC_FORM_045";
        String[] picks = {
                WorkTypeCatalog.CLEANING.displayName(),
                WorkTypeCatalog.NETA_TESTING.displayName()
        };
        openWorkOrdersScreenWT();
        try {
            for (int i = 0; i < picks.length; i++) {
                int cycle = i + 1;
                assertTrue(wo.openCreateForm(),
                        "Cycle " + cycle + ": create form must open (" + tc + ")");
                assertTrue(wo.openWorkTypePicker(),
                        "Cycle " + cycle + ": picker must open (" + tc + ")");
                assertTrue(wo.selectWorkTypeInPicker(picks[i]),
                        "Cycle " + cycle + ": selecting '" + picks[i] + "' must commit (" + tc + ")");
                wo.cancelCreateForm();
                assertTrue(wo.waitForWorkOrdersScreen(),
                        "Cycle " + cycle + ": cancel must restore the Work Orders list");
                verifyAppAlive(tc + ": cycle " + cycle + " complete");
            }
            verifyNotBlank("Work Orders list after two full cycles (" + tc + ")");
            verifyNoErrorAlert();
            logStepWithScreenshot(tc + " verified: two full cycles, list healthy");
        } finally {
            recoverToList(tc); // no-op when already on the list
        }
    }

    @Test(priority = 46)
    public void TC_WTC_FORM_046_guardedFullDance() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_WTC_FORM_046 - full dance (open→select→reopen→no-op close→cancel) alive at every step");
        String tc = "TC_WTC_FORM_046";
        String pick = WorkTypeCatalog.UPS_MAINTENANCE.displayName();
        openFormGuarded(tc);
        try {
            assertTrue(wo.openWorkTypePicker(), "Step 1: picker must open (" + tc + ")");
            verifyAppAlive(tc + ": step 1 picker open");
            assertTrue(wo.selectWorkTypeInPicker(pick),
                    "Step 2: selecting '" + pick + "' must commit (" + tc + ")");
            verifyAppAlive(tc + ": step 2 selection committed");
            assertEquals(wo.getCreateFormWorkTypeValue(), pick,
                    "Step 2: row must read '" + pick + "'");
            assertTrue(wo.openWorkTypePicker(), "Step 3: picker must reopen (" + tc + ")");
            verifyAppAlive(tc + ": step 3 picker reopened");
            assertTrue(wo.closeWorkTypePickerNoChange(),
                    "Step 4: no-op close must dismiss the sheet (" + tc + ")");
            verifyAppAlive(tc + ": step 4 no-op close");
            wo.cancelCreateForm();
            assertTrue(wo.waitForWorkOrdersScreen(),
                    "Step 5: cancel must restore the Work Orders list");
            verifyAppAlive(tc + ": step 5 form cancelled");
            verifyNotBlank("Work Orders list after the full dance (" + tc + ")");
            verifyNoErrorAlert();
            logStepWithScreenshot(tc + " verified: full dance healthy end-to-end");
        } finally {
            recoverToList(tc); // no-op when already on the list
        }
    }
}
