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
 * asset_engineer library — Settings card granular behavior + the library
 * persistence contract:
 *   "once downloaded you don't need to download again; only after app
 *    reinstall you need to download again" (user-confirmed domain rule).
 *
 * TC_ENG_102 proves it by relaunching the app process (terminate+activate,
 * NOT reinstall) and asserting the downloaded state survives. The
 * ensureLibraryDownloaded gate keeps every row self-sufficient.
 */
public class AssetEngineerSettings_Test extends BaseTest {

    private static final int DOWNLOAD_TIMEOUT_S = 600;

    private AssetEngineerPage engineerPage;

    /** Whole module is gated on the platform-managed eng-lib company flag (BaseTest skips pre-driver when absent). */
    @Override
    protected String requiredCompanyFeature() {
        return "eng-lib";
    }

    @BeforeClass(alwaysRun = true)
    public void settingsSetup() {
        System.out.println("\n📋 Asset Engineer — Settings card granular");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void settingsTestSetup() {
        if (!DriverManager.isDriverActive()) return; // gated/fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void settingsTeardown() {
        DriverManager.resetNoResetOverride();
    }

    @Test(priority = 1101)
    public void TC_ENG_100_cardNameCompositionContract() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_100 - Card accessibility name = 'Load Latest Equipment Library, <state>' composition");

        loginAndSelectSite();
        engineerPage.openSettings();
        assertTrue(engineerPage.scrollToLibraryCard(), "library card reachable");
        String subtitle = engineerPage.getLibraryCardSubtitle();
        assertTrue(!subtitle.isEmpty(),
                "card name must fold a non-empty state subtitle after the title (locator contract)");
        logStepWithScreenshot("TC_ENG_100 state='" + subtitle + "'");
    }

    @Test(priority = 1102)
    public void TC_ENG_101_loadDialogReopensStably() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_101 - Load dialog opens/cancels twice in a row without state drift");

        loginAndSelectSite();
        engineerPage.openSettings();
        assertTrue(engineerPage.scrollToLibraryCard(), "library card reachable");
        String before = engineerPage.getLibraryCardSubtitle();

        for (int round = 1; round <= 2; round++) {
            engineerPage.tapLibraryCard();
            assertTrue(engineerPage.isLoadDialogShown(8),
                    "round " + round + ": 'Load Device Library?' alert must appear");
            assertTrue(engineerPage.tapLoadDialogButton("Cancel"),
                    "round " + round + ": Cancel must dismiss the alert");
        }
        assertEquals(engineerPage.getLibraryCardSubtitle(), before,
                "two open/cancel rounds must not change the library state");
        logStepWithScreenshot("TC_ENG_101 verified");
    }

    @Test(priority = 1103, timeOut = 780_000)
    public void TC_ENG_102_libraryPersistsAcrossAppRelaunch() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_102 - Downloaded library persists across app relaunch (no reinstall)");

        logStep("Step 1: Ensure the library is downloaded");
        loginAndSelectSite();
        engineerPage.ensureLibraryDownloaded(DOWNLOAD_TIMEOUT_S);
        assertTrue(engineerPage.isLibraryDownloadedPerSettings(), "precondition: library cached");

        logStep("Step 2: Relaunch the app process (terminate + activate — NOT a reinstall)");
        String bundleId = AppConstants.APP_BUNDLE_ID;
        DriverManager.getDriver().terminateApp(bundleId);
        shortWait();
        DriverManager.getDriver().activateApp(bundleId);
        longWait();

        logStep("Step 3: Library must still be cached after relaunch");
        loginAndSelectSite(); // fast-paths if session survived; logs in if needed
        engineerPage.openSettings();
        assertTrue(engineerPage.scrollToLibraryCard(), "library card reachable after relaunch");
        assertTrue(engineerPage.isLibraryDownloadedPerSettings(),
                "PERSISTENCE CONTRACT: downloaded library must survive an app relaunch; got '"
                        + engineerPage.getLibraryCardSubtitle() + "'");
        logStepWithScreenshot("TC_ENG_102 persistence verified");
    }

    @Test(priority = 1104)
    public void TC_ENG_103_downloadedIdleSubtitleIsRelativeTime() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_103 - Idle downloaded state renders 'Last updated <relative time>'");

        loginAndSelectSite();
        engineerPage.ensureLibraryDownloaded(DOWNLOAD_TIMEOUT_S);
        // Re-enter Settings so a fresh-download counts string (if any) resets
        // to the idle representation.
        assetPage.navigateToAssetList();
        shortWait();
        engineerPage.openSettings();
        assertTrue(engineerPage.scrollToLibraryCard(), "library card reachable");
        String subtitle = engineerPage.getLibraryCardSubtitle();
        if (subtitle.contains(" frames,")) {
            // Same-session fresh download — counts summary is the valid idle form.
            assertTrue(subtitle.contains("cable / busway entries"),
                    "fresh-download counts summary must be complete; got '" + subtitle + "'");
        } else {
            assertTrue(subtitle.matches("Last updated .+"),
                    "idle downloaded subtitle must be 'Last updated <relative>'; got '" + subtitle + "'");
        }
        logStepWithScreenshot("TC_ENG_103 subtitle='" + subtitle + "'");
    }

    /**
     * Both dialog actions, each with its exact contract:
     * Cancel keeps the current state; Download on an ALREADY-CACHED library
     * re-downloads and replaces it ("Replaces any prior cached library.").
     */
    @DataProvider(name = "dialogActions")
    public Object[][] dialogActions() {
        return new Object[][] { {"Cancel"}, {"Download"} };
    }

    @Test(priority = 1105, dataProvider = "dialogActions", timeOut = 780_000)
    public void TC_ENG_104_dialogActionContract(String action) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_104 [" + action + "] - dialog action contract (Cancel keeps / Download replaces)");

        loginAndSelectSite();
        engineerPage.ensureLibraryDownloaded(DOWNLOAD_TIMEOUT_S);
        engineerPage.openSettings();
        assertTrue(engineerPage.scrollToLibraryCard(), "library card reachable");
        String before = engineerPage.getLibraryCardSubtitle();

        engineerPage.tapLibraryCard();
        assertTrue(engineerPage.isLoadDialogShown(8), "'Load Device Library?' alert must appear");
        assertTrue(engineerPage.tapLoadDialogButton(action), action + " must dismiss the alert");

        if ("Cancel".equals(action)) {
            assertEquals(engineerPage.getLibraryCardSubtitle(), before,
                    "Cancel must keep the library state unchanged");
        } else {
            String terminal = engineerPage.waitForDownloadTerminal(DOWNLOAD_TIMEOUT_S);
            assertEquals(terminal, "SUCCESS_COUNTS",
                    "Re-download over a cached library must complete with the counts summary "
                            + "(replaces prior cache); got " + terminal + " / '"
                            + engineerPage.getLibraryCardSubtitle() + "'");
        }
        logStepWithScreenshot("TC_ENG_104 [" + action + "] verified");
    }
}
