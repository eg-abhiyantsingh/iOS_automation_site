package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import io.restassured.path.json.JsonPath;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * ZP-3928 (iOS 1.56) §5 Copy Data To search · §8 IR photo cache busting · §9 volume buttons.
 */
public final class ZP3928_Misc_Test extends BaseTest {

    private static final String FEATURE_CDT = "Copy Data To search (ZP-3928)";
    private static final String FEATURE_IPC = "IR photo cache busting (ZP-3928)";
    private static final String FEATURE_VOL = "Volume-button capture (ZP-3928)";

    private void openAssetEdit() {
        loginAndSelectSite();
        assetPage.navigateToAssetListTurbo();
        String name = assetPage.openSharedAssetForEditOrFallback(null);
        logStep("Opened asset: " + name);
        if (!assetPage.isEditAssetScreenDisplayed()) {
            throw new SkipException("Edit Asset screen did not open — cannot reach the Copy menu");
        }
    }

    // ── §5 Copy Data To search ──────────────────────────────────────────

    @Test(priority = 1)
    public void TC_CDT_01_searchBarIsInTheCopyToPicker() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, FEATURE_CDT,
                "TC_CDT_01 - The Copy Data To picker has a search bar");
        openAssetEdit();

        logStep("Opening Copy → Copy Data To");
        if (!assetPage.tapCopyTo()) {
            throw new SkipException("Copy Data To is not reachable on this build/asset");
        }
        mediumWait();

        logStep("Asserting a search field exists in the target picker");
        assertTrue(assetPage.isSearchFieldPresentOnScreen(),
                "The 1.56 change adds a search bar to the Copy Data To asset picker — no search "
                + "field was found on the target list");
        logStepWithScreenshot("TC_CDT_01: search bar present in Copy Data To");
    }

    // ── §8 IR cache busting ─────────────────────────────────────────────

    @Test(priority = 20)
    public void TC_IPC_04_photoPayloadCarriesModifiedAt() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE_IPC,
                "TC_IPC_04 - Photo records expose modified_at (the cache-busting key)");

        logStep("Reading the SLD payload the device syncs");
        TestDataApi api = new TestDataApi();
        api.login();
        String sldId = api.firstSldId();
        if (sldId == null || sldId.isEmpty()) throw new SkipException("No accessible SLD");

        JsonPath sld = JsonPath.from(api.getSldDetails(sldId));
        List<Map<String, Object>> issues = sld.getList("issues");
        if (issues == null || issues.isEmpty()) throw new SkipException("No issues in the payload");

        int withModified = 0, photoBearing = 0;
        for (Map<String, Object> i : issues) {
            Object key = i.get("ir_photo_key");
            if (key == null || String.valueOf(key).isEmpty()) continue;
            photoBearing++;
            if (i.get("modified_at") != null) withModified++;
        }
        logStep("Photo-bearing issues: " + photoBearing + " · carrying modified_at: " + withModified);
        if (photoBearing == 0) throw new SkipException("No photo-bearing issues to inspect");

        assertEquals(withModified, photoBearing,
                "Every photo-bearing record must expose modified_at — that timestamp IS the cache-busting "
                + "key. " + (photoBearing - withModified) + " of " + photoBearing + " are missing it, so the "
                + "device cannot tell a replaced photo from a cached one.");
        logStepWithScreenshot("TC_IPC_04: modified_at present on all photo records");
    }

    // ── §9 volume buttons: documented N/A ───────────────────────────────

    @Test(priority = 30)
    public void TC_VOL_01_volumeButtonCaptureIsNotAutomatable() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE_VOL,
                "TC_VOL_01 - Volume-button capture is manual-only (documented N/A)");
        logStep("The iOS Simulator cannot press hardware volume buttons, and WebDriverAgent "
                + "exposes no API for them. The ticket itself notes the feature is likely not feasible.");
        logStep("Coverage decision: MANUAL on a physical device if the feature ships. "
                + "Recorded here so the ticket has an explicit verdict rather than a silent gap.");
        throw new SkipException("Volume-button capture: not automatable on the simulator — manual on device");
    }
}
