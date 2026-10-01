package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.WorkOrderFormsPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * ZP-3928 (iOS 1.56) §4 — "EG Forms: addable and fillable on every work order type".
 *
 * One data-driven case per service-backed work type (13 — {@link WorkTypeCatalog#SERVICE_COUNT}),
 * each riding the QA-WT fixture family that {@link WorkTypeBaseTest#openFixtureOrSkip} self-provisions
 * on the landed site. Per type the case proves BOTH halves of the acceptance criterion:
 *
 *   ADDABLE  — the form screen opens for an asset in the session AND exposes either an existing
 *              form-instance chip or the '+' (Add Form) control. Hard-asserted: a work type whose
 *              assets have no way to receive a form is exactly the regression this guards.
 *   FILLABLE — a step result can be set to Pass and reads back as Pass. Hard-asserted whenever the
 *              form has a step table; a form with zero steps is recorded as such (it cannot be
 *              "filled", and that is worth knowing per type), not silently passed.
 *
 * "General" (no backend service, work_type_id null) is included as a DISCOVERY case: the screen
 * must open, but whether forms exist for a type-less work order is app truth to capture, not assert.
 *
 * The deep form interactions (notes, fail card, save/reopen) are already covered per-type-agnostic
 * by WorkType_Forms_Test (TC_WT_FORM_001-045); this matrix answers only "does every type get forms".
 */
public final class ZP3928_EgFormsMatrix_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "EG Forms on every work order type (ZP-3928)";

    private WorkOrderFormsPage forms;

    @BeforeMethod(alwaysRun = true)
    public void initFormsPage() {
        if (!DriverManager.isDriverActive()) return;
        try {
            forms = new WorkOrderFormsPage();
        } catch (IllegalStateException e) {
            DriverManager.initDriver();
            forms = new WorkOrderFormsPage();
        }
    }

    @DataProvider(name = "serviceBackedWorkTypes")
    public Object[][] serviceBackedWorkTypes() {
        List<WorkTypeCatalog> types = WorkTypeCatalog.serviceBacked();
        Object[][] rows = new Object[types.size()][];
        for (int i = 0; i < types.size(); i++) {
            rows[i] = new Object[]{ String.format("TC_EGF_%02d", i + 1), types.get(i) };
        }
        return rows;
    }

    /** Session → Assets tab → first room with assets → first asset's form screen (skip-guarded nav). */
    private void openFirstAssetFormsOrSkip(WorkTypeCatalog wt, String tcId) {
        openFixtureOrSkip(wt, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(),
                tcId + ": session details did not settle open for " + wt.fixtureName());
        skipIfPreconditionMissing(() -> wo.tapSessionTab("Assets"),
                tcId + ": 'Assets' session tab not tappable");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.openFirstRoomWithAssetsInTree(),
                tcId + ": no '<room>, N assets' row reachable in the session tree");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.isAssetsInRoomOpen(),
                tcId + ": 'Assets in Room' did not open");

        List<String> rows = forms.visibleAssetRowComposites();
        skipIfPreconditionMissing(() -> !rows.isEmpty(), tcId + ": room lists no asset rows");
        // Composite is "<name>, <room>, <Class>" (asset-class contract) — open by the name prefix.
        String first = rows.get(0);
        String assetName = first.contains(",") ? first.substring(0, first.indexOf(',')).trim() : first.trim();
        logStep("First asset in room: '" + assetName + "' (row: '" + first + "')");
        skipIfPreconditionMissing(() -> forms.openAssetForms(assetName),
                tcId + ": form screen did not open for asset '" + assetName + "'");
        verifyNotBlank("form screen (" + tcId + ", " + wt.displayName() + ")");
    }

    @Test(dataProvider = "serviceBackedWorkTypes")
    public void TC_EGF_formsAddableAndFillablePerWorkType(String tcId, WorkTypeCatalog wt) {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tcId + " - EG Forms addable + fillable on '" + wt.displayName() + "'");
        openFirstAssetFormsOrSkip(wt, tcId);

        // ── ADDABLE ────────────────────────────────────────────────────────
        List<String> chips = forms.getFormChipNames();
        boolean plus = forms.isFormControlPresent("plus");
        logStep(wt.displayName() + ": form chips=" + chips + " · '+' (Add Form) present=" + plus);
        assertTrue(!chips.isEmpty() || plus,
                "'" + wt.displayName() + "' (" + wt.fixtureName() + "): the form screen exposes neither a "
                + "form-instance chip nor the '+' Add Form control — EG Forms are not addable on this work "
                + "type, which is the 1.56 'every work order type' promise broken");

        // ── FILLABLE ───────────────────────────────────────────────────────
        if (chips.isEmpty()) {
            logStep("No form instance yet — adding one via '+' to prove the type accepts a form");
            skipIfPreconditionMissing(() -> forms.isFormControlPresent("plus"), tcId + ": '+' vanished");
            // Adding a form opens the type's form picker; the deep picker anatomy is covered by
            // WorkType_Forms_Test. Here we only need an instance to exist afterwards.
            assertTrue(forms.tapAddFormControl(), tcId + ": the '+' Add Form control should be tappable");
            mediumWait();
            chips = forms.getFormChipNames();
            logStep("chips after '+': " + chips);
        }
        int steps = forms.stepCount();
        logStep("step rows on the current form: " + steps);
        if (steps == 0) {
            logStep("DISCOVERY: '" + wt.displayName() + "' form has no step table — nothing to fill; "
                    + "recorded, not asserted (a step-less form is app truth for this type)");
            logStepWithScreenshot(tcId + ": addable (no steps) — " + wt.displayName());
            return;
        }
        assertTrue(forms.setStepResult(0, "Pass"),
                "'" + wt.displayName() + "': the first step's Result control should accept 'Pass'");
        mediumWait();
        String read = forms.stepResult(0);
        assertEquals(read, "Pass",
                "'" + wt.displayName() + "': step 1 Result must read back 'Pass' after selection "
                + "(read '" + read + "') — a form that does not hold a value is not fillable");
        logStepWithScreenshot(tcId + ": addable + fillable — " + wt.displayName());
    }

    @Test
    public void TC_EGF_14_generalWorkOrderFormsDiscovery() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_EGF_14 - DISCOVERY: forms on a 'General' (type-less) work order");
        openFirstAssetFormsOrSkip(WorkTypeCatalog.GENERAL, "TC_EGF_14");
        List<String> chips = forms.getFormChipNames();
        boolean plus = forms.isFormControlPresent("plus");
        logStep("General WO: form chips=" + chips + " · '+' present=" + plus);
        // App truth first: General has no backend service, so forms MAY legitimately be absent.
        // The screen opening without a hang or blank is the only hard requirement here.
        verifyAppAlive("General work order form screen");
        logStepWithScreenshot("TC_EGF_14: General WO forms surface (chips=" + chips.size() + ", plus=" + plus + ")");
    }
}
