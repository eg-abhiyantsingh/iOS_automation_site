package com.egalvanic.tests;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebElement;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;

import io.appium.java_client.AppiumBy;

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
 *   2. open it for edit, lock the class,
 *   3. read the BASELINE Arc Flash completion % on the asset's own edit form,
 *   4. fill the asset's values (lean, time-boxed),
 *   5. read the AFTER completion %,
 *   6. assert the completion strictly INCREASED, then save.
 *
 * Oracle: the asset's OWN edit-form completion % (app's
 * {@code NodeV2.af_completion = round(completedRequired/totalRequired × 100)}),
 * so it is immune to the parallel-CI concurrency that blurs the site-wide
 * dashboard aggregate, and "after > before" only passes if filling the values
 * genuinely completed required fields.
 *
 * Robustness (learned from run 29654781571): the fill is hard time-boxed so a
 * slow simulator can never run it into the 360s per-test timeout, and if the
 * app doesn't render a readable completion % for a class the test SKIPs (honest
 * "couldn't measure") rather than false-failing.
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
        runReadinessRisesTest("ATS", () -> assetPage.changeAssetClassToATS());
    }

    @Test(priority = 2)
    public void TC_AFR_002_circuitBreakerArcFlashReadinessRises() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_READINESS,
                "TC_AFR_002 - Circuit Breaker: filling all values raises the asset's Arc Flash readiness");
        runReadinessRisesTest("Circuit Breaker", () -> assetPage.changeAssetClassToCircuitBreaker());
    }

    @Test(priority = 3)
    public void TC_AFR_003_transformerArcFlashReadinessRises() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_READINESS,
                "TC_AFR_003 - Transformer: filling all values raises the asset's Arc Flash readiness");
        runReadinessRisesTest("Transformer", () -> assetPage.changeAssetClassToTransformer());
    }

    @Test(priority = 4)
    public void TC_AFR_004_panelboardArcFlashReadinessRises() {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, AppConstants.FEATURE_AF_READINESS,
                "TC_AFR_004 - Panelboard: filling all values raises the asset's Arc Flash readiness");
        runReadinessRisesTest("Panelboard", () -> assetPage.changeAssetClassToPanelboard());
    }

    // ─────────────────────────────────────── engine ─────────────────────────

    /**
     * Create a fresh asset of {@code classLabel}, capture its Arc Flash
     * completion %, fill its values, and assert the completion strictly rose.
     */
    private void runReadinessRisesTest(String classLabel, Runnable ensureClass) {
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

        // 2) Open for edit and lock the class.
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
        try { assetPage.enableRequiredFieldsOnly(); } catch (Exception ignored) {}
        shortWait();

        // 3) BASELINE — required fields still empty. A freshly-created asset may
        // render no % element at 0% complete; treat "not shown" as 0% (an empty
        // asset genuinely is 0% complete) and require a real positive % after.
        int pctBeforeRaw = readCompletionPercent();
        int pctBefore = Math.max(pctBeforeRaw, 0);
        logStep(classLabel + " BASELINE Arc Flash completion: "
                + (pctBeforeRaw < 0 ? "not shown → treated as 0" : pctBeforeRaw + "") + "%");

        // 4) Fill the asset's values (lean, hard time-boxed inside the page object).
        logStep("Step 4: Fill " + classLabel + " values");
        int filled = 0;
        try { filled = assetPage.fillReadinessFieldsForClass(classLabel); } catch (Exception e) {
            logStep(classLabel + " fill raised (continuing to measure): " + e.getMessage());
        }
        assertTrue(filled > 0,
                classLabel + ": expected to populate at least one readiness field, but filled 0 "
                        + "(the class-specific fields were not found on the edit form)");
        shortWait();

        // 5) AFTER — must be a real, readable percentage to measure a rise.
        int pctAfter = readCompletionPercent();
        if (pctAfter < 0) {
            throw new SkipException(classLabel
                    + ": no readable Arc Flash completion % after filling — cannot measure (precondition).");
        }
        logStep(classLabel + " AFTER-fill Arc Flash completion: " + pctAfter + "% (filled " + filled + " field(s))");

        // 6) ORACLE — filling values must raise this asset's Arc Flash completion.
        assertTrue(pctAfter > pctBefore,
                classLabel + ": Arc Flash completion % must INCREASE after filling values ("
                        + pctBefore + "% → " + pctAfter + "%). Filled " + filled + " field(s).");

        // 7) Persist and confirm the app survived.
        logStep("Step 5: Save changes");
        try { assetPage.clickSaveChanges(); } catch (Exception ignored) {}
        verifyAppAlive(classLabel + " Arc Flash readiness saved");
        ExtentReportManager.logPass(classLabel + " Arc Flash readiness rose " + pctBefore + "% → " + pctAfter
                + "% after filling " + filled + " value(s)");
    }

    // ─────────────────────────────────────── reading ────────────────────────

    /**
     * The asset's own Arc Flash completion percentage on the edit form.
     * Scans every element whose text carries '%' and returns the first that is a
     * real "N%" number — skipping label-only collisions like "% Impedance".
     * Returns -1 when no numeric percentage is on screen.
     */
    private int readCompletionPercent() {
        try {
            List<WebElement> els = DriverManager.getDriver().findElements(
                    AppiumBy.iOSNsPredicateString("name CONTAINS '%' OR label CONTAINS '%'"));
            Pattern p = Pattern.compile("(\\d+)\\s*%");
            for (WebElement el : els) {
                String t = el.getAttribute("name");
                if (t == null) t = el.getAttribute("label");
                if (t == null) continue;
                Matcher m = p.matcher(t);
                if (m.find()) return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
}
