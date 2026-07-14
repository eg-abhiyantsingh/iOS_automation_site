package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.Test;

/**
 * TEMPORARY diagnostic — v1.50 Interrupting Rating dropdown + Save-button probe.
 * Not part of any suite; run explicitly with -Dtest=DebugProbe_Test.
 */
public class DebugProbe_Test extends BaseTest {

    @Test
    public void PROBE_interruptingRatingAnatomy() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "PROBE - Interrupting Rating v1.50 anatomy"
        );

        loginAndSelectSite();
        assetPage.openSharedAssetForEditOrFallback(null);
        assetPage.changeAssetClassToDisconnectSwitch();
        assetPage.scrollFormDown();
        shortWait();

        assetPage.debugProbeRatingAndSave("10 kA",
            "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad");
    }
}
