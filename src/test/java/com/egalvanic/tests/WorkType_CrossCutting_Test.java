package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.ExtentReportManager;

import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * TC_WT_X_* — Work-Type CROSS-CUTTING suite (Class 5 of the TC_WT_* family,
 * docs/worktype-test-design-2026-07-21.md §"Class 5"; domain truth in
 * docs/worktype-gold-spec-2026-07-21.md).
 *
 * WHAT: the seams the per-surface classes cannot see —
 *  - PAR  (14): API↔catalog parity — work_type_id persisted on each durable
 *    QA-WT fixture equals the pinned UUIDv5 service id for all 13 service-backed
 *    types; QA-WT00 General exists AND carries work_type_id = null (existence is
 *    load-bearing: workOrderWorkTypeId() cannot distinguish "null type" from
 *    "WO absent", so General's parity is only meaningful with the id check).
 *  - FLD  (14): per-fixture API field integrity from the company WO list —
 *    priority Medium, est_hours 8 (unquoted JSON number, scanned with a private
 *    raw-token helper), active true, photo_type FLUKE (WT00: FLUKE or FLIR-SEP
 *    — the Android Qa Site1 copy was seeded through the web form default
 *    FLIR-SEP while the ensure path seeds FLUKE; the fixture family lives on
 *    BOTH sites, and the list scan returns the first copy, so the OR is the
 *    real contract for WT00 only).
 *  - CAN   (2): iOS create-form CANARY — the v1.51 "Start New Work Order" form
 *    has NO "Work Type" config row (the 13-option dropdown is web-only today).
 *    These tests assert the row is ABSENT, so they are designed to FLIP RED the
 *    day iOS gains the dropdown. When they fail: that is (very likely) the
 *    feature landing, not a locator bug — update the gold spec + this suite,
 *    do NOT "fix" the locator.
 *  - IDEM  (2): ensureWorkOrderFixture find-or-create idempotency — repeat
 *    calls return the same id and never duplicate rows.
 *  - NEG   (2): createWorkOrder with a bogus (well-formed UUIDv5-shaped)
 *    work_type_id must be rejected non-2xx; if the backend ever ACCEPTS it we
 *    fail LOUDLY (real product finding: missing FK/exists validation). Names
 *    use 'QA-WT-NEG-' + currentTimeMillis so any accidental junk is uniquely
 *    marked and never collides with durable fixtures.
 *  - CAT   (1): /procedures-v2/services catalog stable across two back-to-back
 *    calls (same id-field count; all 13 pinned ids + keys in both bodies).
 *  - LEG   (2): a pre-work-type legacy 'Work Order - *' row opens cleanly on
 *    iOS and carries work_type_id = null server-side (gold spec §2: null on
 *    ALL legacy WOs). SKIPs honestly when the landed site has no legacy row.
 *  - STAB  (4): dashboard → list → fixture → back, two cycles per test, two
 *    sample fixtures (WT08 IR + WT03 PM-Forms), each sampled twice.
 *    Deactivate/activate round-trips are deliberately NOT tested here — too
 *    state-mutating for durable fixtures shared with 4 sibling classes.
 *  - SITE  (2): cross-site parity via API — 'Android Qa Site1' resolves to an
 *    sld id and hosts its own QA-WT08 fixture copy (sld_id-matched).
 *  - NOF   (3): the zero-generated-forms types (WT07 DGA, WT12 Shutdown,
 *    WT13 UPS — procedure targets absent on the QA sites, gold spec §5) still
 *    open cleanly on iOS: verified nav, alive, not blank, no error alert,
 *    clean back-nav.
 *  - CHIP (14): UI↔API parity extension — the SAME datum (priority) read
 *    through both surfaces: the list-row composite chip ('<name>, Medium')
 *    must equal the API priority field for every fixture.
 *
 * WHY: the sibling classes each trust ONE surface (Catalog=API, List/Details/
 * Behavior=UI). Only cross-surface equality proves the iOS app renders what
 * the backend persisted — the exact bug family the work-type feature can
 * introduce (id persisted but label dropped, chip cached stale, etc.).
 *
 * FIXTURES: durable QA-WT00..13 family (gold spec §3), self-provisioned on the
 * landed site by WorkTypeBaseTest.ensureFixturesOnLandedSite() and pre-existing
 * on 'Android Qa Site1'. API-only tests query the company-wide WO list, so they
 * find the family regardless of which site the app landed on.
 *
 * FUTURE WORK (deliberately omitted): offline smoke (airplane-mode on → fixture
 * rows still listed from the local store → recovery on reconnect) needs
 * network-toggle helpers that are not part of the current page/API surface —
 * add once a connectivity utility lands in the framework.
 */
public class WorkType_CrossCutting_Test extends WorkTypeBaseTest {

    private static final String FEATURE_WT = "Work Types (13-option dropdown)";

    /** Well-formed (version nibble 5, RFC variant) UUID that matches NO service. */
    private static final String BOGUS_WORK_TYPE_ID = "ffffffff-ffff-5fff-8fff-ffffffffffff";

    /** Home site of the durable fixture family (gold spec §3). */
    private static final String CROSS_SITE_NAME = "Android Qa Site1";

    // ═════════════════════════════════════════════════════════════════════
    // Group PAR — API ↔ catalog parity of the persisted work_type_id (14)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * The persisted work_type_id of {@code wt}'s durable fixture must equal
     * the pinned catalog service id. GENERAL: fixture must EXIST (hard) and
     * carry null. Service-backed types SKIP when the fixture is absent —
     * existence itself is Class 1's contract (TC_WT_FIX_*), parity is ours.
     */
    private void assertApiParity(WorkTypeCatalog wt, String tc) {
        TestDataApi a = requireApi(tc);
        String fixture = wt.fixtureName();
        String woId = a.findWorkOrderIdByName(fixture);
        logStep(tc + ": fixture '" + fixture + "' resolved to WO id " + woId);
        if (wt == WorkTypeCatalog.GENERAL) {
            assertTrue(woId != null && !woId.isEmpty(),
                    "QA-WT00 General fixture must exist in the company WO list — its null "
                    + "work_type_id is only meaningful when the WO itself is present");
            assertEquals(a.workOrderWorkTypeId(fixture), null,
                    "General fixture must persist work_type_id = null (UI-only 14th option, gold spec §1)");
        } else {
            skipIfPreconditionMissing(() -> woId != null,
                    tc + ": durable fixture '" + fixture + "' absent from the company WO list "
                    + "(family not provisioned — existence is asserted by TC_WT_FIX_*)");
            assertEquals(a.workOrderWorkTypeId(fixture), wt.serviceId(),
                    "work_type_id persisted on '" + fixture + "' must equal the pinned "
                    + wt.name() + " service id (deterministic UUIDv5)");
        }
        logStep(tc + " verified: work_type_id parity holds for " + wt.name());
    }

    @Test(priority = 1, description = "TC_WT_X_PAR_01 - QA-WT01 persisted work_type_id equals pinned Arc Flash Data Collection service id")
    public void TC_WT_X_PAR_01_wt01ArcFlashDataCollectionIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_01 - QA-WT01 persisted work_type_id equals pinned Arc Flash Data Collection service id");
        assertApiParity(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_X_PAR_01");
    }

    @Test(priority = 2, description = "TC_WT_X_PAR_02 - QA-WT02 persisted work_type_id equals pinned Arc Flash Label Placement service id")
    public void TC_WT_X_PAR_02_wt02ArcFlashLabelPlacementIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_02 - QA-WT02 persisted work_type_id equals pinned Arc Flash Label Placement service id");
        assertApiParity(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_X_PAR_02");
    }

    @Test(priority = 3, description = "TC_WT_X_PAR_03 - QA-WT03 persisted work_type_id equals pinned Cleaning service id")
    public void TC_WT_X_PAR_03_wt03CleaningIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_03 - QA-WT03 persisted work_type_id equals pinned Cleaning service id");
        assertApiParity(WorkTypeCatalog.CLEANING, "TC_WT_X_PAR_03");
    }

    @Test(priority = 4, description = "TC_WT_X_PAR_04 - QA-WT04 persisted work_type_id equals pinned Clean, Tighten, Torque service id")
    public void TC_WT_X_PAR_04_wt04CleanTightenTorqueIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_04 - QA-WT04 persisted work_type_id equals pinned Clean, Tighten, Torque service id");
        assertApiParity(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_X_PAR_04");
    }

    @Test(priority = 5, description = "TC_WT_X_PAR_05 - QA-WT05 persisted work_type_id equals pinned Condition Assessment service id")
    public void TC_WT_X_PAR_05_wt05ConditionAssessmentIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_05 - QA-WT05 persisted work_type_id equals pinned Condition Assessment service id");
        assertApiParity(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_X_PAR_05");
    }

    @Test(priority = 6, description = "TC_WT_X_PAR_06 - QA-WT06 persisted work_type_id equals pinned De-Energized Visual Inspection service id")
    public void TC_WT_X_PAR_06_wt06DeEnergizedVisualIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_06 - QA-WT06 persisted work_type_id equals pinned De-Energized Visual Inspection service id");
        assertApiParity(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_X_PAR_06");
    }

    @Test(priority = 7, description = "TC_WT_X_PAR_07 - QA-WT07 persisted work_type_id equals pinned DGA / Fluid Sample Analysis service id")
    public void TC_WT_X_PAR_07_wt07DgaFluidSampleIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_07 - QA-WT07 persisted work_type_id equals pinned DGA / Fluid Sample Analysis service id");
        assertApiParity(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_X_PAR_07");
    }

    @Test(priority = 8, description = "TC_WT_X_PAR_08 - QA-WT08 persisted work_type_id equals pinned Infrared Thermography service id")
    public void TC_WT_X_PAR_08_wt08InfraredThermographyIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_08 - QA-WT08 persisted work_type_id equals pinned Infrared Thermography service id");
        assertApiParity(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_X_PAR_08");
    }

    @Test(priority = 9, description = "TC_WT_X_PAR_09 - QA-WT09 persisted work_type_id equals pinned Insulation Resistance Testing service id")
    public void TC_WT_X_PAR_09_wt09InsulationResistanceIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_09 - QA-WT09 persisted work_type_id equals pinned Insulation Resistance Testing service id");
        assertApiParity(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_X_PAR_09");
    }

    @Test(priority = 10, description = "TC_WT_X_PAR_10 - QA-WT10 persisted work_type_id equals pinned NETA Testing (de-energized-testing) service id")
    public void TC_WT_X_PAR_10_wt10NetaTestingIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_10 - QA-WT10 persisted work_type_id equals pinned NETA Testing (de-energized-testing) service id");
        assertApiParity(WorkTypeCatalog.NETA_TESTING, "TC_WT_X_PAR_10");
    }

    @Test(priority = 11, description = "TC_WT_X_PAR_11 - QA-WT11 persisted work_type_id equals pinned Panel Schedule Updates service id")
    public void TC_WT_X_PAR_11_wt11PanelScheduleUpdatesIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_11 - QA-WT11 persisted work_type_id equals pinned Panel Schedule Updates service id");
        assertApiParity(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_X_PAR_11");
    }

    @Test(priority = 12, description = "TC_WT_X_PAR_12 - QA-WT12 persisted work_type_id equals pinned Shutdown (Composite) service id")
    public void TC_WT_X_PAR_12_wt12ShutdownCompositeIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_12 - QA-WT12 persisted work_type_id equals pinned Shutdown (Composite) service id");
        assertApiParity(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_X_PAR_12");
    }

    @Test(priority = 13, description = "TC_WT_X_PAR_13 - QA-WT13 persisted work_type_id equals pinned UPS Maintenance service id")
    public void TC_WT_X_PAR_13_wt13UpsMaintenanceIdParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_13 - QA-WT13 persisted work_type_id equals pinned UPS Maintenance service id");
        assertApiParity(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_X_PAR_13");
    }

    @Test(priority = 14, description = "TC_WT_X_PAR_14 - QA-WT00 General exists AND persists work_type_id = null")
    public void TC_WT_X_PAR_14_wt00GeneralExistsWithNullWorkType() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_PAR_14 - QA-WT00 General exists AND persists work_type_id = null");
        assertApiParity(WorkTypeCatalog.GENERAL, "TC_WT_X_PAR_14");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group FLD — per-fixture API field integrity (14)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Field integrity of {@code wt}'s fixture as returned by the company WO
     * list: priority Medium, est_hours numerically 8 (raw unquoted token),
     * active true, photo_type FLUKE (GENERAL: FLUKE or FLIR-SEP — the two
     * durable copies were seeded through different paths; see class javadoc.
     * That OR is the genuine contract, not an assertion-weakening shortcut).
     */
    private void assertFieldIntegrity(WorkTypeCatalog wt, String tc) {
        TestDataApi a = requireApi(tc);
        String fixture = wt.fixtureName();
        String obj = jsonObjectContaining(a.listWorkOrdersJson(fixture), "name", fixture);
        skipIfPreconditionMissing(() -> obj != null,
                tc + ": durable fixture '" + fixture + "' absent from the company WO list");
        assertEquals(TestDataApi.extract(obj, "priority"), "Medium",
                "Fixture '" + fixture + "' must be seeded with priority Medium");
        String photo = TestDataApi.extract(obj, "photo_type");
        if (wt == WorkTypeCatalog.GENERAL) {
            assertTrue("FLUKE".equals(photo) || "FLIR-SEP".equals(photo),
                    "QA-WT00 photo_type must be FLUKE (ensure path) or FLIR-SEP (web form default on "
                    + "Android Qa Site1) — the list scan hits whichever site copy first; got '" + photo + "'");
        } else {
            assertEquals(photo, "FLUKE",
                    "Fixture '" + fixture + "' must be seeded with photo_type FLUKE");
        }
        String est = rawJsonToken(obj, "est_hours");
        double estVal;
        try {
            estVal = est == null ? Double.NaN : Double.parseDouble(est);
        } catch (NumberFormatException e) {
            estVal = Double.NaN;
        }
        assertTrue(estVal == 8.0,
                "Fixture '" + fixture + "' est_hours must be numerically 8 — raw token '" + est + "'");
        assertEquals(rawJsonToken(obj, "active"), "true",
                "Fixture '" + fixture + "' must be active (raw JSON boolean true)");
        logStep(tc + " verified: priority=Medium, est_hours=" + est + ", active=true, photo_type=" + photo);
    }

    @Test(priority = 15, description = "TC_WT_X_FLD_01 - QA-WT01 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_01_wt01ArcFlashDataCollectionFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_01 - QA-WT01 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_X_FLD_01");
    }

    @Test(priority = 16, description = "TC_WT_X_FLD_02 - QA-WT02 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_02_wt02ArcFlashLabelPlacementFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_02 - QA-WT02 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_X_FLD_02");
    }

    @Test(priority = 17, description = "TC_WT_X_FLD_03 - QA-WT03 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_03_wt03CleaningFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_03 - QA-WT03 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.CLEANING, "TC_WT_X_FLD_03");
    }

    @Test(priority = 18, description = "TC_WT_X_FLD_04 - QA-WT04 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_04_wt04CleanTightenTorqueFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_04 - QA-WT04 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_X_FLD_04");
    }

    @Test(priority = 19, description = "TC_WT_X_FLD_05 - QA-WT05 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_05_wt05ConditionAssessmentFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_05 - QA-WT05 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_X_FLD_05");
    }

    @Test(priority = 20, description = "TC_WT_X_FLD_06 - QA-WT06 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_06_wt06DeEnergizedVisualFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_06 - QA-WT06 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_X_FLD_06");
    }

    @Test(priority = 21, description = "TC_WT_X_FLD_07 - QA-WT07 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_07_wt07DgaFluidSampleFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_07 - QA-WT07 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_X_FLD_07");
    }

    @Test(priority = 22, description = "TC_WT_X_FLD_08 - QA-WT08 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_08_wt08InfraredThermographyFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_08 - QA-WT08 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_X_FLD_08");
    }

    @Test(priority = 23, description = "TC_WT_X_FLD_09 - QA-WT09 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_09_wt09InsulationResistanceFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_09 - QA-WT09 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_X_FLD_09");
    }

    @Test(priority = 24, description = "TC_WT_X_FLD_10 - QA-WT10 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_10_wt10NetaTestingFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_10 - QA-WT10 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.NETA_TESTING, "TC_WT_X_FLD_10");
    }

    @Test(priority = 25, description = "TC_WT_X_FLD_11 - QA-WT11 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_11_wt11PanelScheduleUpdatesFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_11 - QA-WT11 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_X_FLD_11");
    }

    @Test(priority = 26, description = "TC_WT_X_FLD_12 - QA-WT12 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_12_wt12ShutdownCompositeFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_12 - QA-WT12 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_X_FLD_12");
    }

    @Test(priority = 27, description = "TC_WT_X_FLD_13 - QA-WT13 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE")
    public void TC_WT_X_FLD_13_wt13UpsMaintenanceFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_13 - QA-WT13 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE");
        assertFieldIntegrity(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_X_FLD_13");
    }

    @Test(priority = 28, description = "TC_WT_X_FLD_14 - QA-WT00 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE-or-FLIR-SEP")
    public void TC_WT_X_FLD_14_wt00GeneralFieldIntegrity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_FLD_14 - QA-WT00 API fields: priority Medium, est_hours 8, active true, photo_type FLUKE-or-FLIR-SEP");
        assertFieldIntegrity(WorkTypeCatalog.GENERAL, "TC_WT_X_FLD_14");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group CAN — iOS create-form CANARY (2). These FLIP when iOS gains the
    // Work Type dropdown: a failure here most likely means the feature LANDED
    // — update the gold spec + suite, do not patch the locator.
    // ═════════════════════════════════════════════════════════════════════

    @Test(priority = 29, description = "TC_WT_X_CAN_01 - CANARY: v1.51 Start New Work Order form has NO 'Work Type' row (flips when iOS gains the dropdown)")
    public void TC_WT_X_CAN_01_createFormHasNoWorkTypeRow() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CAN_01 - CANARY: v1.51 Start New Work Order form has NO 'Work Type' row (flips when iOS gains the dropdown)");
        openWorkOrdersScreenWT();
        assertTrue(wo.openCreateForm(), "Start New Work Order form must open");
        verifyAppAlive("TC_WT_X_CAN_01: create form open");
        verifyNotBlank("Start New Work Order form");
        String value = wo.getCreateFormRowValue("Work Type");
        logStep("'Work Type' create-form row probe returned: '" + value + "'");
        assertEquals(value, "",
                "CANARY: the v1.51 iOS create form must NOT contain a populated 'Work Type' config row "
                + "(the 13-option dropdown is web-only today). A non-empty value means iOS has gained the "
                + "work-type dropdown — update the gold spec and this suite instead of patching the locator");
        wo.cancelCreateForm();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(), "Cancel must land back on the Work Orders list");
        logStepWithScreenshot("TC_WT_X_CAN_01 verified: no Work Type row on the create form");
    }

    @Test(priority = 30, description = "TC_WT_X_CAN_02 - CANARY stability: 'Work Type' row stays absent across two open/cancel cycles of the create form")
    public void TC_WT_X_CAN_02_workTypeRowStaysAbsentAcrossReopen() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CAN_02 - CANARY stability: 'Work Type' row stays absent across two open/cancel cycles of the create form");
        openWorkOrdersScreenWT();
        for (int round = 1; round <= 2; round++) {
            assertTrue(wo.openCreateForm(), "Create form must open (round " + round + ")");
            verifyAppAlive("TC_WT_X_CAN_02: create form open, round " + round);
            String value = wo.getCreateFormRowValue("Work Type");
            assertEquals(value, "",
                    "CANARY round " + round + ": 'Work Type' row must stay absent/empty on the v1.51 create form "
                    + "— see TC_WT_X_CAN_01 for the flip semantics");
            wo.cancelCreateForm();
            shortWait();
            assertTrue(wo.waitForWorkOrdersScreen(),
                    "Cancel must restore the Work Orders list (round " + round + ")");
        }
        verifyNoErrorAlert();
        logStepWithScreenshot("TC_WT_X_CAN_02 verified: canary stable across two cycles");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group IDEM — ensureWorkOrderFixture find-or-create idempotency (2)
    // ═════════════════════════════════════════════════════════════════════

    @Test(priority = 31, description = "TC_WT_X_IDEM_01 - repeated ensureWorkOrderFixture calls return the same pre-existing WO id (WT08)")
    public void TC_WT_X_IDEM_01_ensureFixtureReturnsSameIdTwice() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_IDEM_01 - repeated ensureWorkOrderFixture calls return the same pre-existing WO id (WT08)");
        TestDataApi a = requireApi("TC_WT_X_IDEM_01");
        WorkTypeCatalog wt = WorkTypeCatalog.INFRARED_THERMOGRAPHY;
        String fixture = wt.fixtureName();
        String preexisting = a.findWorkOrderIdByName(fixture);
        skipIfPreconditionMissing(() -> preexisting != null,
                "TC_WT_X_IDEM_01: durable fixture '" + fixture + "' absent — the idempotency contract "
                + "needs a pre-existing row (fresh provisioning is Class 1 territory)");
        // Real sld id keeps an accidental create well-formed; it is unused on
        // the find path because the fixture is known-present at this point.
        String sld = a.resolveSldIdByName(CROSS_SITE_NAME);
        String first = a.ensureWorkOrderFixture(fixture, wt.serviceId(), sld);
        String second = a.ensureWorkOrderFixture(fixture, wt.serviceId(), sld);
        logStep("pre-existing=" + preexisting + " first=" + first + " second=" + second);
        assertEquals(first, preexisting,
                "First ensure call must RETURN the pre-existing WO id (find, not create)");
        assertEquals(second, first,
                "Second ensure call must return the identical id — find-or-create must be idempotent");
        logStep("TC_WT_X_IDEM_01 verified: ensure is a stable find for existing fixtures");
    }

    @Test(priority = 32, description = "TC_WT_X_IDEM_02 - two ensureWorkOrderFixture calls do not duplicate the fixture row (WT03 row count unchanged)")
    public void TC_WT_X_IDEM_02_ensureFixtureDoesNotDuplicateRows() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_IDEM_02 - two ensureWorkOrderFixture calls do not duplicate the fixture row (WT03 row count unchanged)");
        TestDataApi a = requireApi("TC_WT_X_IDEM_02");
        WorkTypeCatalog wt = WorkTypeCatalog.CLEANING;
        String fixture = wt.fixtureName();
        int before = countRowsNamed(a.listWorkOrdersJson(fixture), fixture);
        skipIfPreconditionMissing(() -> before >= 1,
                "TC_WT_X_IDEM_02: durable fixture '" + fixture + "' absent — duplication check needs "
                + "at least one pre-existing row");
        String sld = a.resolveSldIdByName(CROSS_SITE_NAME);
        a.ensureWorkOrderFixture(fixture, wt.serviceId(), sld);
        a.ensureWorkOrderFixture(fixture, wt.serviceId(), sld);
        int after = countRowsNamed(a.listWorkOrdersJson(fixture), fixture);
        logStep("rows named '" + fixture + "': before=" + before + " after=" + after);
        assertEquals(after, before,
                "Row count for '" + fixture + "' must be unchanged by two ensure calls — an increase "
                + "means ensureWorkOrderFixture duplicated a durable fixture");
        logStep("TC_WT_X_IDEM_02 verified: no duplicate rows created");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group NEG — bogus work_type_id must be rejected (2)
    // ═════════════════════════════════════════════════════════════════════

    @Test(priority = 33, description = "TC_WT_X_NEG_01 - createWorkOrder with bogus work_type_id ffffffff-...-5fff must be rejected non-2xx")
    public void TC_WT_X_NEG_01_bogusWorkTypeIdIsRejected() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_NEG_01 - createWorkOrder with bogus work_type_id ffffffff-...-5fff must be rejected non-2xx");
        TestDataApi a = requireApi("TC_WT_X_NEG_01");
        String sld = a.resolveSldIdByName(CROSS_SITE_NAME);
        skipIfPreconditionMissing(() -> sld != null && !sld.isEmpty(),
                "TC_WT_X_NEG_01: cannot resolve '" + CROSS_SITE_NAME + "' sld id — a REAL sld is needed "
                + "so a rejection is attributable to the bogus work_type_id alone");
        String name = "QA-WT-NEG-" + System.currentTimeMillis();
        String createdId = null;
        boolean rejected = false;
        String rejection = null;
        try {
            createdId = a.createWorkOrder(name, BOGUS_WORK_TYPE_ID, sld, "FLUKE", "Medium", 8);
        } catch (IllegalStateException e) {
            rejected = true;
            rejection = e.getMessage();
        }
        logStep(rejected ? "Rejected as expected: " + rejection
                         : "ACCEPTED unexpectedly — created id " + createdId);
        assertTrue(rejected,
                "BACKEND ACCEPTED a garbage work_type_id (" + BOGUS_WORK_TYPE_ID + ") and created WO '"
                + name + "' id=" + createdId + " — server-side FK/exists validation is MISSING. This is "
                + "a real product finding, not a script bug; the timestamped name marks the junk row");
        assertTrue(rejection != null && rejection.contains("ir_session/create"),
                "Rejection must come from the /ir_session/create call itself (not auth/lookup plumbing) "
                + "— got: " + rejection);
        logStep("TC_WT_X_NEG_01 verified: bogus work_type_id rejected");
    }

    @Test(priority = 34, description = "TC_WT_X_NEG_02 - rejected bogus-work_type create leaves NO durable WO row behind")
    public void TC_WT_X_NEG_02_bogusCreateLeavesNoResidualRow() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_NEG_02 - rejected bogus-work_type create leaves NO durable WO row behind");
        TestDataApi a = requireApi("TC_WT_X_NEG_02");
        String sld = a.resolveSldIdByName(CROSS_SITE_NAME);
        skipIfPreconditionMissing(() -> sld != null && !sld.isEmpty(),
                "TC_WT_X_NEG_02: cannot resolve '" + CROSS_SITE_NAME + "' sld id — needed for a "
                + "well-formed create attempt");
        String name = "QA-WT-NEG-" + System.currentTimeMillis();
        try {
            a.createWorkOrder(name, BOGUS_WORK_TYPE_ID, sld, "FLUKE", "Medium", 8);
            logStep("Create unexpectedly returned 2xx — residual check below will catch the junk row");
        } catch (IllegalStateException expectedRejection) {
            logStep("Create rejected (expected): " + expectedRejection.getMessage());
        }
        String residual = a.findWorkOrderIdByName(name);
        assertEquals(residual, null,
                "No WO named '" + name + "' may exist after a bogus-work_type create attempt — a non-null "
                + "id means the backend accepted garbage or persisted a partial row (durable junk)");
        logStep("TC_WT_X_NEG_02 verified: no residual row for " + name);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group CAT — services catalog stable across two calls (1)
    // ═════════════════════════════════════════════════════════════════════

    @Test(priority = 35, description = "TC_WT_X_CAT_01 - /procedures-v2/services is stable across two back-to-back calls (same id count, all 13 pinned ids + keys in both)")
    public void TC_WT_X_CAT_01_servicesCatalogStableAcrossTwoCalls() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CAT_01 - /procedures-v2/services is stable across two back-to-back calls (same id count, all 13 pinned ids + keys in both)");
        TestDataApi a = requireApi("TC_WT_X_CAT_01");
        String first = a.workTypeServicesJson();
        String second = a.workTypeServicesJson();
        int idCount1 = countOccurrences(first, "\"id\":");
        int idCount2 = countOccurrences(second, "\"id\":");
        logStep("id-field occurrences: call1=" + idCount1 + " call2=" + idCount2);
        assertTrue(idCount1 > 0, "Catalog JSON must contain id fields — call 1 had none");
        assertEquals(idCount2, idCount1,
                "id-field count must be identical across two back-to-back catalog calls — a drift means "
                + "the services list is unstable within a session");
        for (WorkTypeCatalog wt : WorkTypeCatalog.serviceBacked()) {
            assertTrue(first.contains(wt.serviceId()),
                    "Call 1 must contain the pinned " + wt.name() + " service id " + wt.serviceId());
            assertTrue(second.contains(wt.serviceId()),
                    "Call 2 must contain the pinned " + wt.name() + " service id " + wt.serviceId());
            assertTrue(first.contains("\"" + wt.key() + "\"") && second.contains("\"" + wt.key() + "\""),
                    "Both calls must contain the service key '" + wt.key() + "' (" + wt.name() + ")");
        }
        logStep("TC_WT_X_CAT_01 verified: catalog stable across two calls");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group LEG — legacy pre-work-type 'Work Order - *' rows (2)
    // ═════════════════════════════════════════════════════════════════════

    @Test(priority = 36, description = "TC_WT_X_LEG_01 - a legacy 'Work Order - ' row opens cleanly and back-nav restores the list")
    public void TC_WT_X_LEG_01_legacyRowOpensCleanly() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_LEG_01 - a legacy 'Work Order - ' row opens cleanly and back-nav restores the list");
        openWorkOrdersScreenWT();
        boolean present = wo.scrollWorkOrderListTo("Work Order - ");
        skipIfPreconditionMissing(() -> present,
                "TC_WT_X_LEG_01: no legacy 'Work Order - ' row on the landed site's WO list");
        String composite = wo.getWorkOrderRowComposite("Work Order - ");
        assertTrue(composite != null && composite.startsWith("Work Order - "),
                "Legacy row composite must be readable and begin with 'Work Order - ' — got '" + composite + "'");
        assertTrue(wo.openWorkOrderByName("Work Order - "),
                "Legacy row must open with verified navigation");
        verifyAppAlive("TC_WT_X_LEG_01: legacy work order opened");
        verifyNotBlank("legacy work order screen");
        verifyNoErrorAlert();
        wo.goBack();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(),
                "Back-nav from a legacy work order must restore the Work Orders list");
        logStepWithScreenshot("TC_WT_X_LEG_01 verified: legacy row '" + composite + "' opened and closed cleanly");
    }

    @Test(priority = 37, description = "TC_WT_X_LEG_02 - the legacy row visible on iOS carries work_type_id = null server-side (gold spec: null on all pre-work-type WOs)")
    public void TC_WT_X_LEG_02_legacyRowHasNullWorkTypeServerSide() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_LEG_02 - the legacy row visible on iOS carries work_type_id = null server-side (gold spec: null on all pre-work-type WOs)");
        TestDataApi a = requireApi("TC_WT_X_LEG_02");
        openWorkOrdersScreenWT();
        boolean present = wo.scrollWorkOrderListTo("Work Order - ");
        skipIfPreconditionMissing(() -> present,
                "TC_WT_X_LEG_02: no legacy 'Work Order - ' row on the landed site's WO list");
        String composite = wo.getWorkOrderRowComposite("Work Order - ");
        assertTrue(composite != null && composite.startsWith("Work Order - "),
                "Legacy row composite must be readable — got '" + composite + "'");
        final String comp = composite == null ? "" : composite; // assertTrue threw on null; keeps flow analysis happy
        String chip = WorkOrderPage.rowPriority(comp);
        boolean chipParses = Arrays.asList("Low", "Medium", "High").contains(chip);
        skipIfPreconditionMissing(() -> chipParses,
                "TC_WT_X_LEG_02: legacy composite '" + comp + "' has no parseable Low/Medium/High "
                + "priority suffix — cannot derive the exact server-side name from the row");
        String legacyName = comp.substring(0, comp.length() - (", " + chip).length());
        logStep("Derived legacy WO name: '" + legacyName + "' (chip " + chip + ")");
        String id = a.findWorkOrderIdByName(legacyName);
        assertTrue(id != null,
                "Legacy WO '" + legacyName + "' visible on iOS must exist in the company WO list API — "
                + "a miss means UI/API name divergence");
        assertEquals(a.workOrderWorkTypeId(legacyName), null,
                "Pre-work-type legacy WO '" + legacyName + "' must carry work_type_id = null (gold spec §2)");
        logStep("TC_WT_X_LEG_02 verified: legacy row is null-typed server-side");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group STAB — dashboard→list→fixture→back stability, 2 cycles per test,
    // 2 samples (WT08 IR, WT03 PM-Forms), each sampled twice (4).
    // Deactivate/activate round-trips intentionally excluded (state-mutating
    // against durable fixtures shared with 4 sibling classes).
    // ═════════════════════════════════════════════════════════════════════

    private void stabilityRoundTrip(WorkTypeCatalog wt, String tc) {
        String fixture = wt.fixtureName();
        openWorkOrdersScreenWT();
        boolean onScreen = wo.scrollWorkOrderListTo(fixture);
        skipIfPreconditionMissing(() -> onScreen,
                tc + ": fixture '" + fixture + "' not reachable in the Work Orders list");
        for (int round = 1; round <= 2; round++) {
            assertTrue(wo.openWorkOrderByName(fixture),
                    tc + " round " + round + ": fixture must open with verified navigation");
            verifyAppAlive(tc + " round " + round + ": fixture screen open");
            verifyNotBlank("work order screen (" + fixture + ", round " + round + ")");
            wo.goBack();
            shortWait();
            assertTrue(wo.waitForWorkOrdersScreen(),
                    tc + " round " + round + ": back-nav must restore the Work Orders list");
            assertTrue(wo.scrollWorkOrderListTo(fixture),
                    tc + " round " + round + ": fixture row must still be reachable after back-nav");
        }
        verifyNoErrorAlert();
        logStepWithScreenshot(tc + " verified: two open/back cycles stable for " + fixture);
    }

    @Test(priority = 38, description = "TC_WT_X_STAB_01 - WT08 Infrared Thermography: dashboard→list→fixture→back, two cycles (sample A)")
    public void TC_WT_X_STAB_01_wt08RoundTripStableSampleA() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_STAB_01 - WT08 Infrared Thermography: dashboard→list→fixture→back, two cycles (sample A)");
        stabilityRoundTrip(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_X_STAB_01");
    }

    @Test(priority = 39, description = "TC_WT_X_STAB_02 - WT08 Infrared Thermography: dashboard→list→fixture→back, two cycles (sample B, repeat run)")
    public void TC_WT_X_STAB_02_wt08RoundTripStableSampleB() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_STAB_02 - WT08 Infrared Thermography: dashboard→list→fixture→back, two cycles (sample B, repeat run)");
        stabilityRoundTrip(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_X_STAB_02");
    }

    @Test(priority = 40, description = "TC_WT_X_STAB_03 - WT03 Cleaning: dashboard→list→fixture→back, two cycles (sample A)")
    public void TC_WT_X_STAB_03_wt03RoundTripStableSampleA() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_STAB_03 - WT03 Cleaning: dashboard→list→fixture→back, two cycles (sample A)");
        stabilityRoundTrip(WorkTypeCatalog.CLEANING, "TC_WT_X_STAB_03");
    }

    @Test(priority = 41, description = "TC_WT_X_STAB_04 - WT03 Cleaning: dashboard→list→fixture→back, two cycles (sample B, repeat run)")
    public void TC_WT_X_STAB_04_wt03RoundTripStableSampleB() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_STAB_04 - WT03 Cleaning: dashboard→list→fixture→back, two cycles (sample B, repeat run)");
        stabilityRoundTrip(WorkTypeCatalog.CLEANING, "TC_WT_X_STAB_04");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group SITE — cross-site parity via API (2)
    // ═════════════════════════════════════════════════════════════════════

    @Test(priority = 42, description = "TC_WT_X_SITE_01 - 'Android Qa Site1' resolves to a UUID-shaped sld id via the API")
    public void TC_WT_X_SITE_01_androidQaSite1Resolves() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_SITE_01 - 'Android Qa Site1' resolves to a UUID-shaped sld id via the API");
        TestDataApi a = requireApi("TC_WT_X_SITE_01");
        String sld = a.resolveSldIdByName(CROSS_SITE_NAME);
        logStep("resolveSldIdByName('" + CROSS_SITE_NAME + "') = " + sld);
        assertTrue(sld != null && !sld.isEmpty(),
                "'" + CROSS_SITE_NAME + "' must resolve to an sld id — it is the fixture family's home "
                + "site (gold spec §3)");
        final String sldId = sld == null ? "" : sld; // assertTrue threw on null; keeps flow analysis happy
        assertTrue(sldId.length() == 36 && countOccurrences(sldId, "-") == 4,
                "Resolved sld id must be UUID-shaped (36 chars, 4 dashes) — got '" + sldId + "'");
        logStep("TC_WT_X_SITE_01 verified");
    }

    @Test(priority = 43, description = "TC_WT_X_SITE_02 - the QA-WT08 fixture also exists on Android Qa Site1 (sld_id-matched via the WO list API)")
    public void TC_WT_X_SITE_02_wt08FixtureExistsOnAndroidQaSite1() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_SITE_02 - the QA-WT08 fixture also exists on Android Qa Site1 (sld_id-matched via the WO list API)");
        TestDataApi a = requireApi("TC_WT_X_SITE_02");
        String sld = a.resolveSldIdByName(CROSS_SITE_NAME);
        assertTrue(sld != null && !sld.isEmpty(),
                "'" + CROSS_SITE_NAME + "' must resolve to an sld id (same contract as TC_WT_X_SITE_01) "
                + "— without it cross-site placement cannot be checked");
        final String sldId = sld == null ? "" : sld; // assertTrue threw on null; keeps flow analysis happy
        String json = a.listWorkOrdersJson("QA-WT08");
        String nameOnSite = TestDataApi.extractSiblingField(json, "sld_id", sldId, "name");
        logStep("First 'QA-WT08' search hit on sld " + sldId + ": '" + nameOnSite + "'");
        assertEquals(nameOnSite, WorkTypeCatalog.INFRARED_THERMOGRAPHY.fixtureName(),
                "The QA-WT08 search must contain a row on " + CROSS_SITE_NAME + " (sld_id " + sldId
                + ") named exactly '" + WorkTypeCatalog.INFRARED_THERMOGRAPHY.fixtureName()
                + "' — the fixture family must exist on BOTH sites");
        logStep("TC_WT_X_SITE_02 verified: WT08 fixture present cross-site");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group NOF — zero-generated-forms types open cleanly (3)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * WT07/WT12/WT13 produced 0 generated forms on the QA sites (procedures
     * target classes the sites lack / 0 procedures — gold spec §5). The iOS
     * screen must still open cleanly: verified nav, alive, not blank, no error
     * alert, clean back-nav. Form/task COUNTS are never asserted (async and
     * class-conditional — catalog javadoc contract).
     */
    private void assertOpensCleanlyDespiteNoForms(WorkTypeCatalog wt, String tc) {
        openFixtureOrSkip(wt, tc); // verified nav assert inside
        verifyAppAlive(tc + ": fixture opened");
        verifyNotBlank("work order screen (" + wt.fixtureName() + ")");
        verifyNoErrorAlert();
        wo.goBack();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(),
                tc + ": back-nav from a 0-forms work type must restore the Work Orders list");
        assertTrue(wo.scrollWorkOrderListTo(wt.fixtureName()),
                tc + ": fixture row must still be reachable after the round trip");
        logStepWithScreenshot(tc + " verified: " + wt.fixtureName() + " opens cleanly with 0 generated forms");
    }

    @Test(priority = 44, description = "TC_WT_X_NOF_01 - WT07 DGA Fluid Sample Analysis opens cleanly despite 0 generated forms")
    public void TC_WT_X_NOF_01_wt07DgaOpensCleanlyNoForms() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_NOF_01 - WT07 DGA Fluid Sample Analysis opens cleanly despite 0 generated forms");
        assertOpensCleanlyDespiteNoForms(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_X_NOF_01");
    }

    @Test(priority = 45, description = "TC_WT_X_NOF_02 - WT12 Shutdown (Composite) opens cleanly despite 0 procedures/forms")
    public void TC_WT_X_NOF_02_wt12ShutdownOpensCleanlyNoForms() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_NOF_02 - WT12 Shutdown (Composite) opens cleanly despite 0 procedures/forms");
        assertOpensCleanlyDespiteNoForms(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_X_NOF_02");
    }

    @Test(priority = 46, description = "TC_WT_X_NOF_03 - WT13 UPS Maintenance opens cleanly despite 0 generated forms")
    public void TC_WT_X_NOF_03_wt13UpsOpensCleanlyNoForms() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_NOF_03 - WT13 UPS Maintenance opens cleanly despite 0 generated forms");
        assertOpensCleanlyDespiteNoForms(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_X_NOF_03");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Group CHIP — UI priority chip ↔ API priority field parity (14)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * The SAME datum through both surfaces: the fixture row's composite chip
     * ('&lt;name&gt;, &lt;Priority&gt;') must equal the API priority field —
     * and both must be the seeded 'Medium'. UI-visible + API-absent is a HARD
     * FAIL (that exact divergence is this class's reason to exist).
     */
    private void assertChipApiParity(WorkTypeCatalog wt, String tc) {
        TestDataApi a = requireApi(tc); // skip cheaply before any UI spend
        String fixture = wt.fixtureName();
        openWorkOrdersScreenWT();
        verifyAppAlive(tc + ": Work Orders list open");
        boolean onScreen = wo.scrollWorkOrderListTo(fixture);
        skipIfPreconditionMissing(() -> onScreen,
                tc + ": fixture row '" + fixture + "' not reachable in the Work Orders list");
        String composite = wo.getWorkOrderRowComposite(fixture);
        assertTrue(composite != null && composite.startsWith(fixture),
                "Row composite must be readable and begin with '" + fixture + "' — got '" + composite + "'");
        String chip = WorkOrderPage.rowPriority(composite);
        String obj = jsonObjectContaining(a.listWorkOrdersJson(fixture), "name", fixture);
        assertTrue(obj != null,
                tc + ": row is ON SCREEN but fixture '" + fixture + "' is missing from the company WO "
                + "list API — UI/API divergence (stale local store or name drift)");
        String apiPriority = TestDataApi.extract(obj, "priority");
        assertEquals(chip, apiPriority,
                "UI priority chip must equal the API priority field for '" + fixture + "'");
        assertEquals(apiPriority, "Medium",
                "Seeded fixture priority (API) must be Medium for '" + fixture + "'");
        logStepWithScreenshot(tc + " verified: chip '" + chip + "' == API priority '" + apiPriority + "'");
    }

    @Test(priority = 47, description = "TC_WT_X_CHIP_01 - QA-WT01 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_01_wt01ArcFlashDataCollectionChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_01 - QA-WT01 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.ARC_FLASH_DATA_COLLECTION, "TC_WT_X_CHIP_01");
    }

    @Test(priority = 48, description = "TC_WT_X_CHIP_02 - QA-WT02 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_02_wt02ArcFlashLabelPlacementChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_02 - QA-WT02 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.ARC_FLASH_LABEL_PLACEMENT, "TC_WT_X_CHIP_02");
    }

    @Test(priority = 49, description = "TC_WT_X_CHIP_03 - QA-WT03 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_03_wt03CleaningChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_03 - QA-WT03 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.CLEANING, "TC_WT_X_CHIP_03");
    }

    @Test(priority = 50, description = "TC_WT_X_CHIP_04 - QA-WT04 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_04_wt04CleanTightenTorqueChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_04 - QA-WT04 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "TC_WT_X_CHIP_04");
    }

    @Test(priority = 51, description = "TC_WT_X_CHIP_05 - QA-WT05 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_05_wt05ConditionAssessmentChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_05 - QA-WT05 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.CONDITION_ASSESSMENT, "TC_WT_X_CHIP_05");
    }

    @Test(priority = 52, description = "TC_WT_X_CHIP_06 - QA-WT06 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_06_wt06DeEnergizedVisualChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_06 - QA-WT06 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.DE_ENERGIZED_VISUAL, "TC_WT_X_CHIP_06");
    }

    @Test(priority = 53, description = "TC_WT_X_CHIP_07 - QA-WT07 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_07_wt07DgaFluidSampleChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_07 - QA-WT07 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.DGA_FLUID_SAMPLE, "TC_WT_X_CHIP_07");
    }

    @Test(priority = 54, description = "TC_WT_X_CHIP_08 - QA-WT08 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_08_wt08InfraredThermographyChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_08 - QA-WT08 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.INFRARED_THERMOGRAPHY, "TC_WT_X_CHIP_08");
    }

    @Test(priority = 55, description = "TC_WT_X_CHIP_09 - QA-WT09 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_09_wt09InsulationResistanceChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_09 - QA-WT09 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.INSULATION_RESISTANCE, "TC_WT_X_CHIP_09");
    }

    @Test(priority = 56, description = "TC_WT_X_CHIP_10 - QA-WT10 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_10_wt10NetaTestingChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_10 - QA-WT10 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.NETA_TESTING, "TC_WT_X_CHIP_10");
    }

    @Test(priority = 57, description = "TC_WT_X_CHIP_11 - QA-WT11 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_11_wt11PanelScheduleUpdatesChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_11 - QA-WT11 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.PANEL_SCHEDULE_UPDATES, "TC_WT_X_CHIP_11");
    }

    @Test(priority = 58, description = "TC_WT_X_CHIP_12 - QA-WT12 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_12_wt12ShutdownCompositeChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_12 - QA-WT12 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.SHUTDOWN_COMPOSITE, "TC_WT_X_CHIP_12");
    }

    @Test(priority = 59, description = "TC_WT_X_CHIP_13 - QA-WT13 list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_13_wt13UpsMaintenanceChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_13 - QA-WT13 list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.UPS_MAINTENANCE, "TC_WT_X_CHIP_13");
    }

    @Test(priority = 60, description = "TC_WT_X_CHIP_14 - QA-WT00 General list-row priority chip equals API priority (Medium)")
    public void TC_WT_X_CHIP_14_wt00GeneralChipParity() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE_WT,
                "TC_WT_X_CHIP_14 - QA-WT00 General list-row priority chip equals API priority (Medium)");
        assertChipApiParity(WorkTypeCatalog.GENERAL, "TC_WT_X_CHIP_14");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Private JSON helpers — no JSON library in this project (house rule:
    // extract/extractSiblingField/plain string scanning only).
    // ═════════════════════════════════════════════════════════════════════

    /**
     * The complete JSON object slice containing "{field}":"{value}" (first
     * match; brace-balanced walk — same tradeoffs as
     * {@link TestDataApi#extractSiblingField}). Tolerates a single space after
     * the field colon. Returns null when no such object exists.
     */
    private static String jsonObjectContaining(String json, String field, String value) {
        if (json == null || value == null) return null;
        int at = json.indexOf("\"" + field + "\":\"" + value + "\"");
        if (at < 0) at = json.indexOf("\"" + field + "\": \"" + value + "\"");
        if (at < 0) return null;
        int depth = 0, start = -1;
        for (int i = at - 1; i >= 0; i--) {
            char c = json.charAt(i);
            if (c == '}') depth++;
            else if (c == '{') {
                if (depth == 0) { start = i; break; }
                depth--;
            }
        }
        if (start < 0) return null;
        depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return json.substring(start, i + 1);
        }
        return null;
    }

    /**
     * Raw first-match token of {@code field} inside an object slice — handles
     * UNQUOTED JSON values (numbers, true/false/null) that
     * {@link TestDataApi#extract} cannot see; quoted values return the inner
     * string. Null when the field is absent.
     */
    private static String rawJsonToken(String objJson, String field) {
        if (objJson == null) return null;
        String needle = "\"" + field + "\"";
        int i = objJson.indexOf(needle);
        if (i < 0) return null;
        int j = objJson.indexOf(':', i + needle.length());
        if (j < 0) return null;
        j++;
        while (j < objJson.length() && Character.isWhitespace(objJson.charAt(j))) j++;
        if (j >= objJson.length()) return null;
        if (objJson.charAt(j) == '"') {
            int end = objJson.indexOf('"', j + 1);
            return end < 0 ? null : objJson.substring(j + 1, end);
        }
        int end = j;
        while (end < objJson.length() && ",}] \t\r\n".indexOf(objJson.charAt(end)) < 0) end++;
        return objJson.substring(j, end);
    }

    /** Plain indexOf-loop substring counter (house rule: no JSON library). */
    private static int countOccurrences(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isEmpty()) return 0;
        int count = 0, from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    /**
     * Rows named exactly {@code name} in a WO-list JSON body — counts the
     * name-field pattern with and without a space after the colon (whichever
     * the server emits; the other contributes 0).
     */
    private static int countRowsNamed(String json, String name) {
        return countOccurrences(json, "\"name\":\"" + name + "\"")
             + countOccurrences(json, "\"name\": \"" + name + "\"");
    }
}
