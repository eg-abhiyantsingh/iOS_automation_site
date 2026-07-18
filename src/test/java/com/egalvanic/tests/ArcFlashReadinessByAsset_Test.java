package com.egalvanic.tests;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ArcFlashPage;
import com.egalvanic.pages.AssetPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;

/**
 * Arc Flash Readiness — per-asset CONTRIBUTION tests (v1.49 data-readiness).
 *
 * The existing Arc Flash suite ({@link ArcFlash_Test} et al.) verifies the
 * dashboard's internal arithmetic. It does NOT verify the thing a user actually
 * cares about: <b>does entering an asset's data actually move the Arc Flash
 * number?</b> These tests close that gap end-to-end, one asset class at a time.
 *
 * Per-class flow (e.g. {@code TC_AFR_001_atsArcFlashReadinessRises}):
 *   1. create a fresh asset of the class (its required fields start empty),
 *   2. open it for edit, lock the class, switch to "Required only",
 *   3. read the BASELINE Arc Flash completion ("X/Y" counter + "N%"),
 *   4. fill ALL of the asset's values,
 *   5. read the AFTER completion,
 *   6. assert the completion strictly INCREASED (more required fields done,
 *      higher %), then save.
 *
 * Why the oracle is sound and non-tautological:
 *   - The app's own {@code NodeV2.af_completion} is
 *     {@code round(completedRequired / totalRequired × 100)}
 *     (ArcFlashCompletionViewModel: Asset Details = Σcompleted ÷ Σrequired).
 *   - We read that value from the asset's OWN edit form, so it is immune to the
 *     parallel-CI concurrency that would blur the site-wide dashboard aggregate.
 *   - "after > before" can only pass if filling the values genuinely marked
 *     required fields complete — a stuck/no-op form makes it go RED.
 */
public class ArcFlashReadinessByAsset_Test extends BaseTest {

    private AssetPage assetPage;

    @BeforeClass(alwaysRun = true)
    public void afReadinessClassSetup() {
        System.out.println("\n📋 Arc Flash Readiness (per-asset contribution) — Starting");
        DriverManager.setNoReset(true); // keep login across driver rebuilds
    }

    @BeforeMethod(alwaysRun = true)
    public void afReadinessTestSetup() {
        if (!DriverManager.isDriverActive()) return; // fast-skipped: no driver
        assetPage = new AssetPage();
    }

    @AfterClass(alwaysRun = true)
    public void afReadinessClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    // ─────────────────────────────────────── tests ──────────────────────────

    @Test(priority = 1)
    public void TC_AFR_001_atsArcFlashReadinessRises() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_READINESS,
                "TC_AFR_001 - ATS: filling all values raises the asset's Arc Flash readiness");
        runReadinessRisesTest("ATS", () -> assetPage.changeAssetClassToATS(),
                () -> assetPage.fillAllATSRequiredFields());
    }

    @Test(priority = 2)
    public void TC_AFR_002_circuitBreakerArcFlashReadinessRises() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_READINESS,
                "TC_AFR_002 - Circuit Breaker: filling all values raises the asset's Arc Flash readiness");
        runReadinessRisesTest("Circuit Breaker", () -> assetPage.changeAssetClassToCircuitBreaker(),
                () -> assetPage.fillAllReadinessFieldsBestEffort());
    }

    @Test(priority = 3)
    public void TC_AFR_003_transformerArcFlashReadinessRises() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_READINESS,
                "TC_AFR_003 - Transformer: filling all values raises the asset's Arc Flash readiness");
        runReadinessRisesTest("Transformer", () -> assetPage.changeAssetClassToTransformer(),
                () -> assetPage.fillAllReadinessFieldsBestEffort());
    }

    @Test(priority = 4)
    public void TC_AFR_004_panelboardArcFlashReadinessRises() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_READINESS,
                "TC_AFR_004 - Panelboard: filling all values raises the asset's Arc Flash readiness");
        runReadinessRisesTest("Panelboard", () -> assetPage.changeAssetClassToPanelboard(),
                () -> assetPage.fillAllReadinessFieldsBestEffort());
    }

    // ─────────────────────────────────────── engine ─────────────────────────

    /**
     * Create a fresh asset of {@code classLabel}, capture its Arc Flash
     * completion, fill every value, and assert the completion strictly rose.
     */
    private void runReadinessRisesTest(String classLabel, Runnable ensureClass, Runnable fillAllValues) {
        logStep("Step 1: Login and open the Asset list");
        loginAndSelectSite();
        verifyAppAlive("home before " + classLabel);

        // 1) Create a fresh asset of this class — its required fields start empty.
        long ts = System.currentTimeMillis();
        String name = "AFR_" + classLabel.replaceAll("\\s+", "") + "_" + ts;
        logStep("Step 2: Create a fresh " + classLabel + " asset (" + name + ")");
        assetPage.navigateToAssetList();
        assetPage.clickAddAsset();
        assetPage.enterAssetName(name);
        assetPage.selectAssetClass(classLabel);
        if (!assetPage.selectLocation()) {
            assetPage.createNewLocation("Floor_" + ts, "Room_" + ts);
        }
        assetPage.clickCreateAsset();
        assertTrue(assetPage.isAssetCreatedSuccessfully(),
                classLabel + ": asset must be created before its Arc Flash readiness can be measured");

        // 2) Open for edit, lock the class, show only required fields.
        logStep("Step 3: Reopen '" + name + "' for edit and lock class = " + classLabel);
        assertTrue(assetPage.openAssetByNameForEdit(name),
                classLabel + ": must reopen '" + name + "' for edit");
        if (!assetPage.isCurrentAssetClassEqualTo(classLabel)) {
            ensureClass.run();
        }
        assertTrue(waitForCondition(() -> assetPage.isCurrentAssetClassEqualTo(classLabel), 6,
                        "asset class reads " + classLabel),
                classLabel + ": class must read '" + classLabel
                        + "' or the class-specific required fields won't exist (silent skip guard)");
        assetPage.enableRequiredFieldsOnly();
        shortWait();

        // 3) BASELINE — required fields still empty.
        int[] before = parseCounter(assetPage.getRequiredFieldsCounter());
        int pctBefore = parsePercent(assetPage.getCompletionPercentage());
        assertTrue(before != null && before[1] > 0,
                classLabel + ": the edit form must expose a required-fields counter 'X/Y'; got '"
                        + assetPage.getRequiredFieldsCounter() + "'");
        logStep(classLabel + " BASELINE readiness: " + before[0] + "/" + before[1] + "  (" + pctBefore + "%)");

        // 4) Fill ALL of the asset's values.
        logStep("Step 4: Fill all " + classLabel + " values");
        fillAllValues.run();
        shortWait();

        // 5) AFTER.
        int[] after = parseCounter(assetPage.getRequiredFieldsCounter());
        int pctAfter = parsePercent(assetPage.getCompletionPercentage());
        assertTrue(after != null,
                classLabel + ": required-fields counter must still be readable after filling; got '"
                        + assetPage.getRequiredFieldsCounter() + "'");
        logStep(classLabel + " AFTER fill readiness: " + after[0] + "/" + after[1] + "  (" + pctAfter + "%)");

        // 6) ORACLE — filling values must raise this asset's Arc Flash completion.
        assertTrue(after[0] <= after[1],
                classLabel + ": completed required fields (" + after[0] + ") cannot exceed the total ("
                        + after[1] + ")");
        assertTrue(after[0] > before[0],
                classLabel + ": filling all values must RAISE the count of completed required fields ("
                        + before[0] + "/" + before[1] + " → " + after[0] + "/" + after[1] + ")");
        assertTrue(pctAfter > pctBefore,
                classLabel + ": Arc Flash completion % must INCREASE after filling all values ("
                        + pctBefore + "% → " + pctAfter + "%)");

        // 7) Persist and confirm the app survived.
        logStep("Step 5: Save changes");
        assetPage.clickSaveChanges();
        verifyAppAlive(classLabel + " Arc Flash readiness saved");
        ExtentReportManager.logPass(classLabel + " Arc Flash readiness rose " + pctBefore + "% → " + pctAfter
                + "% after filling " + (after[0] - before[0]) + " required field(s)");
    }

    // ─────────────────────────────────────── parsing ────────────────────────

    /** "42%" → 42; -1 when no percentage is present. */
    private static int parsePercent(String s) {
        if (s == null) return -1;
        Matcher m = Pattern.compile("(\\d+)\\s*%").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** "3/5" → {3,5}; null when no X/Y counter is present. */
    private static int[] parseCounter(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)").matcher(s);
        return m.find() ? new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) } : null;
    }
}
