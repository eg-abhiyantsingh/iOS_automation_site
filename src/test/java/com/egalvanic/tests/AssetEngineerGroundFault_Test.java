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
 * asset_engineer library — Ground Fault settings coverage (gap G3).
 *
 * App truth: the GF block lives INSIDE SkmTripConfigCard on a breaker-bound
 * asset (EngineeringConfigCards.swift ~964-1056) with a dedicated picker
 * sheet (GroundFaultPickerSheet.swift, title "Pick Ground Fault Library").
 * Tapping "Add Ground Fault Settings" has TWO valid, data-dependent
 * outcomes: the GF settings seed directly from a sibling pair in the
 * library ("Ground Fault Settings" header), or the "No GF setting pair
 * found" notice renders with an "Add anyway" escape hatch.
 *
 * Every flow here runs inside a CANCELLED detailed Circuit Breaker
 * Add-Asset draft bound via a match-card tap — nothing ever persists.
 * TC_ENG_163's DataProvider rows share ONE live seeded draft (rebuilding
 * the bound-breaker fixture per row would triple WDA exposure); TC_ENG_164
 * reuses and then discards it, and @AfterClass is the safety net.
 */
public class AssetEngineerGroundFault_Test extends BaseTest {

    private AssetEngineerPage engineerPage;
    private boolean libraryChecked = false;
    /** TC_ENG_163/164 share one seeded GF draft (rebuilt if it died). */
    private boolean gfFixtureLive = false;

    /** Whole module is gated on the platform-managed eng-lib company flag (BaseTest skips pre-driver when absent). */
    @Override
    protected String requiredCompanyFeature() {
        return "eng-lib";
    }

    @BeforeClass(alwaysRun = true)
    public void groundFaultSetup() {
        System.out.println("\n📋 Asset Engineer — Ground Fault settings coverage (gap G3)");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void groundFaultTestSetup() {
        if (!DriverManager.isDriverActive()) return; // gated/fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void groundFaultTeardown() {
        try {
            engineerPage = new AssetEngineerPage();
            engineerPage.closeAssetDetails(true);
        } catch (Exception ignored) {
        }
        DriverManager.resetNoResetOverride();
    }

    // ── fixtures ───────────────────────────────────────────────────────

    /**
     * The trip card (host of the GF block) only exists once the SKM
     * library is cached — gate once per class run (CustomSheet pattern).
     */
    private void ensureReadyOnce() {
        if (libraryChecked) return;
        loginAndSelectSite();
        engineerPage.ensureLibraryDownloaded(600);
        libraryChecked = true;
    }

    private void resetToAssetsList() {
        try {
            engineerPage.closeAssetDetails(true);
        } catch (Exception ignored) {
        }
        gfFixtureLive = false;
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
    }

    /**
     * Detailed Circuit Breaker draft, bound to the first library match —
     * ends with the bound card AND the Frame/Trip configuration card
     * rendered (the GF block's host). Mirrors TC_ENG_113/115.
     */
    private void openBoundBreakerDraft() {
        ensureReadyOnce();
        resetToAssetsList();
        assetPage.clickAddAsset();
        mediumWait();
        assertTrue(engineerPage.pickOptionExact("Detailed"), "'Detailed' toggle must be tappable");
        assetPage.clickSelectAssetClass();
        mediumWait();
        try {
            engineerPage.searchInSheetPicker("Circuit");
        } catch (Exception e) {
            System.out.println("⚠️ class search: " + e.getMessage());
        }
        skipIfPreconditionMissing(() -> engineerPage.pickOptionExact("Circuit Breaker"),
                "no Circuit Breaker class");
        mediumWait();

        engineerPage.openEngineeringPickerBelowLabel("Manufacturer");
        final boolean picked = engineerPage.pickOptionExact("SCHNEIDER/SQUARE D")
                || engineerPage.pickOptionExact("Generic")
                || engineerPage.pickOptionExact("ABB");
        skipIfPreconditionMissing(() -> picked,
                "no library-backed breaker manufacturer in the menu — capture it and extend the list");
        assertTrue(engineerPage.waitForMatchHeader(12),
                "match panel must render after the breaker manufacturer pick");
        skipIfPreconditionMissing(() -> engineerPage.getMatchCount() > 0,
                "manufacturer has zero breaker matches in this library snapshot");

        String card = engineerPage.tapFirstMatchCard();
        assertNotNull(card, "a match card must be tappable in the breaker match list");
        System.out.println("ℹ️ bound breaker card: " + card);
        assertTrue(engineerPage.waitForOptionShown("Unlink", 10),
                "'Unlink' must appear once the breaker library row is linked");
        assertTrue(engineerPage.isBoundCardShown(),
                "'Library Matched' bound card must render after the match tap");
        assertTrue(engineerPage.isTripConfigCardShown(),
                "'" + AssetEngineerPage.TRIP_CONFIG_HEADER
                        + "' card must render on a breaker-bound draft (GF block host)");
    }

    /** Poll for the "Ground Fault Settings" header (probes self-pace on WDA round-trips). */
    private boolean waitForGfSettingsHeader(int probes) {
        for (int i = 0; i < probes; i++) {
            if (engineerPage.isEngineeringLabelPresent(AssetEngineerPage.GF_SETTINGS_HEADER)) return true;
            shortWait();
        }
        return false;
    }

    /**
     * Taps "Add Ground Fault Settings" and classifies the outcome:
     * "seeded" (header from a sibling pair) or "no-pair" (notice). Both are
     * valid app states per the GF spec — the assert is that EXACTLY ONE
     * renders.
     */
    private String tapGfToggleAndClassify() {
        assertTrue(engineerPage.scrollToEngineeringLabel(AssetEngineerPage.GF_ADD_TOGGLE),
                "'" + AssetEngineerPage.GF_ADD_TOGGLE + "' row must be reachable on the bound trip card");
        assertTrue(engineerPage.tapRowByExactLabel(AssetEngineerPage.GF_ADD_TOGGLE),
                "'" + AssetEngineerPage.GF_ADD_TOGGLE + "' row must be tappable");
        boolean seeded = false;
        boolean noPair = false;
        for (int i = 0; i < 20 && !seeded && !noPair; i++) {
            seeded = engineerPage.isEngineeringLabelPresent(AssetEngineerPage.GF_SETTINGS_HEADER);
            noPair = engineerPage.isGfNoPairNoticeShown();
            if (!seeded && !noPair) shortWait();
        }
        // Settle re-probe: the two outcomes must be mutually exclusive.
        seeded = engineerPage.isEngineeringLabelPresent(AssetEngineerPage.GF_SETTINGS_HEADER);
        noPair = engineerPage.isGfNoPairNoticeShown();
        assertTrue(seeded || noPair,
                "tapping '" + AssetEngineerPage.GF_ADD_TOGGLE + "' must yield EITHER the '"
                        + AssetEngineerPage.GF_SETTINGS_HEADER + "' header OR the '"
                        + AssetEngineerPage.GF_NO_PAIR_PREFIX + "' notice — neither rendered");
        assertFalse(seeded && noPair,
                "exactly one GF outcome may render; got BOTH the settings header AND the no-pair notice");
        String branch = seeded ? "seeded" : "no-pair";
        System.out.println("🔎 GF toggle outcome: " + branch);
        return branch;
    }

    /** Seeds the GF settings on the current bound draft, via Add-anyway when the pair is missing. */
    private String seedGfSettings() {
        String branch = tapGfToggleAndClassify();
        if ("no-pair".equals(branch)) {
            assertTrue(engineerPage.isOptionShown(AssetEngineerPage.GF_ADD_ANYWAY),
                    "'" + AssetEngineerPage.GF_ADD_ANYWAY + "' must render under the no-pair notice");
            assertTrue(engineerPage.tapRowByExactLabel(AssetEngineerPage.GF_ADD_ANYWAY),
                    "'" + AssetEngineerPage.GF_ADD_ANYWAY + "' must be tappable");
        }
        assertTrue(waitForGfSettingsHeader(20),
                "'" + AssetEngineerPage.GF_SETTINGS_HEADER
                        + "' header must render after seeding (branch: " + branch + ")");
        return branch;
    }

    /** Live seeded-GF breaker draft shared by TC_ENG_163 rows + TC_ENG_164. */
    private void ensureGfSeededFixture() {
        if (gfFixtureLive
                && (engineerPage.isEngineeringLabelPresent(AssetEngineerPage.GF_SETTINGS_HEADER)
                        || engineerPage.scrollToEngineeringLabel(AssetEngineerPage.GF_SETTINGS_HEADER))) {
            return;
        }
        gfFixtureLive = false;
        openBoundBreakerDraft();
        seedGfSettings();
        gfFixtureLive = true;
    }

    // ═══════════ tests ═══════════

    @Test(priority = 1601)
    public void TC_ENG_160_gfToggleVisibleAndTapYieldsExactlyOneOutcome() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_160 - 'Add Ground Fault Settings' renders on the bound trip card; tap yields exactly one GF outcome");

        logStep("Step 1: Bound Circuit Breaker draft with the Frame/Trip card");
        openBoundBreakerDraft();

        logStep("Step 2: 'Add Ground Fault Settings' row is reachable on the trip card");
        assertTrue(engineerPage.scrollToEngineeringLabel(AssetEngineerPage.GF_ADD_TOGGLE),
                "'" + AssetEngineerPage.GF_ADD_TOGGLE + "' row must be visible on the bound '"
                        + AssetEngineerPage.TRIP_CONFIG_HEADER + "' card");

        logStep("Step 3: Tap it — GF seeds from a sibling pair OR the no-pair notice renders (data-dependent)");
        String branch = tapGfToggleAndClassify();
        logStep("Step 4: Outcome branch = '" + branch + "' (both are valid app states)");
        logStepWithScreenshot("TC_ENG_160 GF toggle outcome: " + branch);

        logStep("Step 5: Discard the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "GF draft must close with discard confirmed");
    }

    @Test(priority = 1602)
    public void TC_ENG_161_noPairNoticeOffersAddAnywayWhichSeedsSettings() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_161 - No-pair notice offers 'Add anyway' and tapping it seeds the GF settings");

        logStep("Step 1: Bound Circuit Breaker draft, tap the GF toggle");
        openBoundBreakerDraft();
        final String branch = tapGfToggleAndClassify();
        if (!"no-pair".equals(branch)) {
            engineerPage.closeAssetDetails(true); // tidy the draft before skipping
        }
        skipIfPreconditionMissing(() -> "no-pair".equals(branch),
                "GF seeded directly from a sibling pair — the no-pair branch is unreachable with this library snapshot");

        logStep("Step 2: 'Add anyway' escape hatch renders under the notice");
        assertTrue(engineerPage.isOptionShown(AssetEngineerPage.GF_ADD_ANYWAY),
                "'" + AssetEngineerPage.GF_ADD_ANYWAY + "' must render alongside the '"
                        + AssetEngineerPage.GF_NO_PAIR_PREFIX + "' notice");

        logStep("Step 3: Tapping 'Add anyway' seeds the GF settings header");
        assertTrue(engineerPage.tapRowByExactLabel(AssetEngineerPage.GF_ADD_ANYWAY),
                "'" + AssetEngineerPage.GF_ADD_ANYWAY + "' must be tappable");
        assertTrue(waitForGfSettingsHeader(20),
                "'" + AssetEngineerPage.GF_SETTINGS_HEADER
                        + "' header must render after 'Add anyway'");
        logStepWithScreenshot("TC_ENG_161 GF seeded via Add anyway");

        logStep("Step 4: Discard the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "GF draft must close with discard confirmed");
    }

    @Test(priority = 1603)
    public void TC_ENG_162_changeRowOpensGfPickerSearchEmptyStateAndCancel() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_162 - 'Change…' opens the GF picker; gibberish search shows the exact empty state; Cancel returns to the card");

        logStep("Step 1: Bound Circuit Breaker draft with GF settings seeded");
        openBoundBreakerDraft();
        String branch = seedGfSettings();
        System.out.println("ℹ️ GF seeded via branch: " + branch);

        logStep("Step 2: 'Change…' row opens the 'Pick Ground Fault Library' sheet");
        boolean changeTapped = engineerPage.tapRowByExactLabel(AssetEngineerPage.GF_CHANGE_ELLIPSIS);
        if (!changeTapped) {
            assertTrue(engineerPage.scrollToOption(AssetEngineerPage.GF_CHANGE_ELLIPSIS, 4),
                    "'" + AssetEngineerPage.GF_CHANGE_ELLIPSIS
                            + "' row must be reachable inside the seeded GF settings");
            changeTapped = engineerPage.tapRowByExactLabel(AssetEngineerPage.GF_CHANGE_ELLIPSIS);
        }
        assertTrue(changeTapped,
                "'" + AssetEngineerPage.GF_CHANGE_ELLIPSIS + "' row must be tappable");
        assertTrue(engineerPage.isGfPickerOpen(6),
                "GF picker sheet titled '" + AssetEngineerPage.GF_PICKER_TITLE
                        + "' must present after '" + AssetEngineerPage.GF_CHANGE_ELLIPSIS + "'");

        logStep("Step 3: Gibberish search shows the exact GF empty state");
        engineerPage.gfPickerSearch("zzqx994");
        mediumWait(); // debounce + recompute
        boolean empty = false;
        for (int i = 0; i < 15 && !empty; i++) {
            empty = engineerPage.isGfEmptyStateShown();
            if (!empty) shortWait();
        }
        assertTrue(empty,
                "gibberish GF search must show exactly '" + AssetEngineerPage.GF_NO_MATCHES + "'");
        logStepWithScreenshot("TC_ENG_162 GF picker empty state verified");

        logStep("Step 4: Cancel dismisses the sheet back to the card");
        assertTrue(engineerPage.tapRowByExactLabel("Cancel"),
                "the GF picker's 'Cancel' button must be tappable");
        assertTrue(engineerPage.waitForLabelGone(AssetEngineerPage.GF_PICKER_TITLE, 6),
                "'" + AssetEngineerPage.GF_PICKER_TITLE + "' must leave the DOM after Cancel");
        assertTrue(engineerPage.isEngineeringLabelPresent(AssetEngineerPage.GF_SETTINGS_HEADER)
                        || engineerPage.scrollToEngineeringLabel(AssetEngineerPage.GF_SETTINGS_HEADER),
                "seeded '" + AssetEngineerPage.GF_SETTINGS_HEADER
                        + "' card must survive a cancelled picker");

        logStep("Step 5: Discard the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "GF draft must close with discard confirmed");
    }

    /** GF fixed slots (EngineeringConfigCards.swift GF block layout). */
    @DataProvider(name = "gfFixedSlots")
    public Object[][] gfFixedSlots() {
        return new Object[][] {
                {"Sensor A"},
                {"Plug A"},
                {"Frame A"},
        };
    }

    @Test(priority = 1604, dataProvider = "gfFixedSlots")
    public void TC_ENG_163_gfFixedSlotRenders(String slot) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_163 [" + slot + "] - Fixed slot renders inside the seeded Ground Fault Settings");

        // Rows share ONE live seeded draft; TC_ENG_164 discards it.
        ensureGfSeededFixture();
        boolean present = engineerPage.isEngineeringLabelPresent(slot)
                || engineerPage.scrollToEngineeringLabel(slot);
        assertTrue(present,
                "GF fixed slot '" + slot + "' must render inside the seeded '"
                        + AssetEngineerPage.GF_SETTINGS_HEADER + "' card");
        logStepWithScreenshot("TC_ENG_163 slot '" + slot + "' verified");
    }

    @Test(priority = 1605)
    public void TC_ENG_164_discardIntegrityLeavesNoResidue() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_164 - Cancelled GF-seeded draft discards cleanly (no residue in the DOM)");

        logStep("Step 1: Live GF-seeded breaker draft (rebuilt if the shared fixture died)");
        ensureGfSeededFixture();

        logStep("Step 2: Discard the draft — must confirm");
        assertTrue(engineerPage.closeAssetDetails(true),
                "Add-Asset draft with seeded GF settings must close with discard confirmed");
        gfFixtureLive = false;

        logStep("Step 3: The GF card left the DOM with the draft");
        assertTrue(engineerPage.waitForLabelGone(AssetEngineerPage.GF_SETTINGS_HEADER, 6),
                "'" + AssetEngineerPage.GF_SETTINGS_HEADER
                        + "' must leave the DOM after discard — draft residue detected otherwise");
        logStepWithScreenshot("TC_ENG_164 discard integrity verified");
    }
}
