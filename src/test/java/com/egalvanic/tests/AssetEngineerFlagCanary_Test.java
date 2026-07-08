package com.egalvanic.tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.egalvanic.api.CompanyFeatureGate;
import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetEngineerPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;

/**
 * eng-lib flag ⇄ UI consistency canary (changelog 111).
 *
 * Deliberately NOT gated on requiredCompanyFeature(): it must run in BOTH
 * tenant modes and assert that the app's UI agrees with the API flag state:
 *
 *   flag ABSENT  → Settings library card renders with the "Engineering
 *                  Library isn't enabled for your company..." caption and the
 *                  tap is a no-op (no Load Device Library alert).
 *   flag PRESENT → no disabled caption, and tapping the card opens the
 *                  "Load Device Library?" alert (cancelled immediately).
 *
 * Catches: flag/UI drift (e.g. server flag restored but app cache stale),
 * regression of the disabled guard, and silently-changed caption copy that
 * would break the UI fallback in AssetEngineerPage.tapLibraryCard().
 * Also serves as the loud CI signal for the current environmental blocker.
 */
public class AssetEngineerFlagCanary_Test extends BaseTest {

    private AssetEngineerPage engineerPage;

    @BeforeClass(alwaysRun = true)
    public void canaryClassSetup() {
        System.out.println("\n📋 Asset Engineer — eng-lib flag/UI consistency canary");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void canaryTestSetup() {
        if (!DriverManager.isDriverActive()) return; // fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void canaryClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    @Test(priority = 130)
    public void TC_ENG_130_engLibFlagMatchesSettingsCardState() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_130 - eng-lib API flag state must match the Settings card UI (disabled caption + tap contract)");

        logStep("Step 1: Read the authoritative flag state from GET /auth/v2/me");
        CompanyFeatureGate.Verdict verdict = CompanyFeatureGate.check("eng-lib");
        skipIfPreconditionMissing(() -> verdict != CompanyFeatureGate.Verdict.INDETERMINATE,
                "eng-lib flag state unverifiable (" + CompanyFeatureGate.detail("eng-lib") + ")");
        boolean flagOn = verdict == CompanyFeatureGate.Verdict.ENABLED;
        System.out.println("🔎 Canary: eng-lib is " + (flagOn ? "ENABLED" : "DISABLED") + " per API");

        logStep("Step 2: Open Settings and reach the Equipment Library card");
        loginAndSelectSite();
        engineerPage.openSettings();
        assertTrue(engineerPage.scrollToLibraryCard(), "Equipment Library card must be reachable");

        if (flagOn) {
            logStep("Step 3 (flag ON): no disabled caption; tap opens the Load alert");
            assertTrue(!engineerPage.isEngLibDisabledBannerVisible(),
                    "Disabled caption must NOT render when eng-lib is enabled");
            try {
                engineerPage.tapLibraryCard();
            } catch (org.testng.SkipException e) {
                throw new AssertionError("UI shows the disabled caption although the API flag is ON — flag/UI drift", e);
            }
            assertTrue(engineerPage.isLoadDialogShown(8),
                    "'Load Device Library?' alert must appear when eng-lib is enabled");
            assertTrue(engineerPage.tapLoadDialogButton("Cancel"), "Cleanup: cancel the alert");
        } else {
            logStep("Step 3 (flag OFF): disabled caption renders and the tap is a no-op");
            assertTrue(engineerPage.isEngLibDisabledBannerVisible(),
                    "Caption '" + AssetEngineerPage.ENGLIB_DISABLED_BANNER
                            + "' must render while eng-lib is disabled");
            logStep("Step 4 (flag OFF): subtitle still shows a normal state (v1.49 quirk, changelog 110)");
            String subtitle = engineerPage.getLibraryCardSubtitle();
            assertTrue(!subtitle.isEmpty(),
                    "Card subtitle must still render a normal state while disabled, got '" + subtitle + "'");
            System.out.println("🔎 Canary: disabled-state subtitle = '" + subtitle + "'");
        }
        logStepWithScreenshot("TC_ENG_130: flag/UI consistency verified (flag "
                + (flagOn ? "ON" : "OFF") + ")");
    }
}
