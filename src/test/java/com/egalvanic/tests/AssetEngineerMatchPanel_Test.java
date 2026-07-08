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
 * asset_engineer library — MATCH-PANEL search / pagination / eligibility
 * deep coverage (gaps G8 + G10).
 *
 * App truth this encodes:
 *  - MatchResultsPanel.swift :100 — "Load more (N)" pagination button when
 *    the result set exceeds one page; :129-130 — the truncated "{n}+
 *    possible matches" header variant accompanies it.
 *  - NodeEngineerinSection eligibleManufacturers / eligibleTripTypes
 *    :167-200 (ZP-2457) — Trip Type options are library-eligibility
 *    filtered; "Ground Fault" is never a Trip TYPE (GF is its own
 *    settings pair, see GF_ADD_TOGGLE surface).
 *
 * Fixture discipline: everything runs inside CANCELLED Detailed Add-Asset
 * drafts — Fuse for the panel probes (small-DOM SAFE surface per module
 * memory; NEVER transformer details, which wedge WDA on v1.49) and a
 * Circuit Breaker draft only for the ZP-2457 smoke. Nothing is ever saved.
 */
public class AssetEngineerMatchPanel_Test extends BaseTest {

    /** Exact live placeholder of the match panel's search field (with quotes). */
    private static final String MATCH_SEARCH_PLACEHOLDER = "e.g. \"QD\" or \"Formula\"";

    private AssetEngineerPage engineerPage;
    private boolean libraryChecked = false;

    /** Whole module is gated on the platform-managed eng-lib company flag (BaseTest skips pre-driver when absent). */
    @Override
    protected String requiredCompanyFeature() {
        return "eng-lib";
    }

    @BeforeClass(alwaysRun = true)
    public void matchPanelSetup() {
        System.out.println("\n📋 Asset Engineer — match-panel search/pagination/eligibility (G8+G10)");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void matchPanelTestSetup() {
        if (!DriverManager.isDriverActive()) return; // gated/fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void matchPanelTeardown() {
        try {
            engineerPage = new AssetEngineerPage();
            engineerPage.closeAssetDetails(true);
        } catch (Exception ignored) { }
        DriverManager.resetNoResetOverride();
    }

    // ── fixtures ───────────────────────────────────────────────────────

    /**
     * The match panel does not render at all until the SKM library is
     * cached — on a fresh container every fixture here would read an empty
     * header. Gate once per class run (CustomSheet pattern).
     */
    private void ensureReadyOnce() {
        if (libraryChecked) return;
        loginAndSelectSite();
        engineerPage.ensureLibraryDownloaded(600);
        libraryChecked = true;
    }

    /** Detailed Add-Asset draft of the given class (search-picked, TC_ENG_113 flow). */
    private void openDetailedDraftOfClass(String classSearchFirstWord, String classExact) {
        loginAndSelectSite();
        ensureReadyOnce();
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        mediumWait();
        assertTrue(engineerPage.pickOptionExact("Detailed"), "'Detailed' toggle must be tappable");
        assetPage.clickSelectAssetClass();
        mediumWait();
        try {
            engineerPage.searchInSheetPicker(classSearchFirstWord);
        } catch (Exception e) {
            System.out.println("⚠️ class search: " + e.getMessage());
        }
        boolean picked = engineerPage.pickOptionExact(classExact);
        skipIfPreconditionMissing(() -> picked, "class '" + classExact + "' not available in the picker");
        mediumWait();
    }

    /**
     * Pick a library-backed manufacturer in the open Fuse draft so the
     * match list populates (same eligible set TC_ENG_110 established live).
     */
    private void pickFuseManufacturer() {
        engineerPage.openEngineeringPickerBelowLabel("Manufacturer");
        final boolean picked = engineerPage.pickOptionExact("ABB")
                || engineerPage.pickOptionExact("Generic")
                || engineerPage.pickOptionExact("SCHNEIDER/SQUARE D");
        skipIfPreconditionMissing(() -> picked,
                "no known manufacturer option in the fuse draft menu — capture the menu and extend the list");
        assertTrue(engineerPage.waitForMatchHeader(12),
                "match header must render after the manufacturer pick");
    }

    /** Poll the match count until it changes past {@code floor} (matcher recompute is async). */
    private int waitForMatchCountAbove(int floor, int maxPolls) {
        int count = engineerPage.getMatchCount();
        for (int i = 0; i < maxPolls && count <= floor; i++) {
            shortWait();
            count = engineerPage.getMatchCount();
        }
        return count;
    }

    // ═══════════ G8 — match-panel anatomy + search narrowing ═══════════

    @Test(priority = 2001)
    public void TC_ENG_200_unsetPanelAnatomyThenManufacturerFiresHeader() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_200 - Fuse draft: unset panel shows search field + zero header; manufacturer pick fires the count header");

        logStep("Step 1: Detailed Fuse draft (small-DOM SAFE surface)");
        openDetailedDraftOfClass("Fuse", "Fuse");

        logStep("Step 2: Unset panel anatomy — search field + zero header");
        assertTrue(engineerPage.waitForMatchHeader(10),
                "match panel header must render on the unset fuse draft");
        assertEquals(engineerPage.getMatchHeaderText(), "No possible matches",
                "unset fuse draft panel must show the zero header");
        assertTrue(engineerPage.isMatchSearchFieldShown(),
                "match search field ('" + MATCH_SEARCH_PLACEHOLDER + "') must render on the unset panel");

        logStep("Step 3: Manufacturer pick updates the count header");
        pickFuseManufacturer();
        String header = engineerPage.getMatchHeaderText();
        assertTrue(!header.isEmpty(),
                "match header must be non-empty after the manufacturer pick");
        assertTrue(header.matches("\\d+\\+? possible match(es)?|No possible matches"),
                "header must follow the '{n}[+] possible match(es)' format; got '" + header + "'");
        logStepWithScreenshot("TC_ENG_200 header='" + header + "'");

        logStep("Step 4: Discard the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "details must close with discard");
    }

    /**
     * Real catalog substrings: fuse catalog/type strings carry amp-rating
     * digits ("0"/"6"/"60" all occur across ABB/Generic fuse rows), so each
     * term must NARROW the list without emptying it.
     */
    @DataProvider(name = "narrowingTerms")
    public Object[][] narrowingTerms() {
        return new Object[][] { {"0"}, {"6"}, {"60"} };
    }

    @Test(priority = 2002, dataProvider = "narrowingTerms")
    public void TC_ENG_201_searchNarrowsWithoutEmptying(String term) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_201 [" + term + "] - Match search with a real substring narrows the list but keeps matches");

        logStep("Step 1: Fuse draft with a populated match list");
        openDetailedDraftOfClass("Fuse", "Fuse");
        pickFuseManufacturer();
        final int before = engineerPage.getMatchCount();
        skipIfPreconditionMissing(() -> before > 0,
                "manufacturer has zero fuse matches in this library snapshot — narrowing needs a baseline");

        logStep("Step 2: Type the real substring '" + term + "' into the match search");
        engineerPage.typeIntoEngineeringField(MATCH_SEARCH_PLACEHOLDER, term);
        mediumWait(); // 250ms debounce + recompute
        assertTrue(engineerPage.waitForMatchHeader(10),
                "match header must survive the search recompute for '" + term + "'");
        int narrowed = engineerPage.getMatchCount();

        logStep("Step 3: Count narrowed but not emptied");
        assertTrue(narrowed <= before,
                "search '" + term + "' must never GROW the list; before=" + before + " after=" + narrowed);
        assertTrue(narrowed > 0,
                "search '" + term + "' is a real catalog substring and must keep at least one match;"
                        + " before=" + before + " after=" + narrowed);
        logStepWithScreenshot("TC_ENG_201 [" + term + "] " + before + " → " + narrowed);

        logStep("Step 4: Discard the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "details must close with discard");
    }

    @Test(priority = 2003)
    public void TC_ENG_202_gibberishZeroStateThenClearRestores() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_202 - Gibberish search shows the zero state; clearing it restores the match list");

        logStep("Step 1: Fuse draft with a populated match list");
        openDetailedDraftOfClass("Fuse", "Fuse");
        pickFuseManufacturer();
        final int before = engineerPage.getMatchCount();
        skipIfPreconditionMissing(() -> before > 0,
                "manufacturer has zero fuse matches in this library snapshot — restore needs a baseline");

        logStep("Step 2: Gibberish search shows the search-specific zero state");
        engineerPage.typeIntoEngineeringField(MATCH_SEARCH_PLACEHOLDER, "zzqx987");
        mediumWait(); // 250ms debounce + recompute
        String emptyState = engineerPage.getMatchEmptyStateText();
        assertTrue(emptyState.contains("No matches"),
                "gibberish search must show the 'No matches' empty state; got '" + emptyState + "'");

        logStep("Step 3: Clearing the search restores the list");
        engineerPage.clearAndTypeIntoEngineeringField(MATCH_SEARCH_PLACEHOLDER, "");
        assertTrue(engineerPage.waitForMatchHeader(10),
                "match header must re-render once the search is cleared");
        int restored = waitForMatchCountAbove(0, 10);
        assertTrue(restored > 0,
                "clearing the search must restore matches; before=" + before + " restored=" + restored);
        logStepWithScreenshot("TC_ENG_202 before=" + before + " restored=" + restored);

        logStep("Step 4: Discard the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "details must close with discard");
    }

    // ═══════════ G10 — pagination (MatchResultsPanel.swift :100/:129) ═══════════

    @Test(priority = 2004)
    public void TC_ENG_203_loadMorePaginationOrSinglePageHeader() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_203 - 'Load more' grows the loaded count; without it the header must not show the truncated 'N+' variant");

        logStep("Step 1: Fuse draft with a populated match list");
        openDetailedDraftOfClass("Fuse", "Fuse");
        pickFuseManufacturer();

        // Both states are legitimate (data-dependent list size) — assert the
        // matching contract for whichever renders; never skip here.
        if (engineerPage.isLoadMoreShown()) {
            logStep("Step 2a: Truncated list — 'Load more' must strictly grow the loaded count");
            final int before = engineerPage.getMatchCount();
            assertTrue(before > 0,
                    "a 'Load more' button with a zero-count header is inconsistent; header='"
                            + engineerPage.getMatchHeaderText() + "'");
            assertTrue(engineerPage.tapLoadMore(), "'Load more' button must accept the tap");
            int after = waitForMatchCountAbove(before, 15);
            assertTrue(after > before,
                    "'Load more' must strictly increase the loaded match count; before=" + before
                            + " after=" + after);
            logStepWithScreenshot("TC_ENG_203 paginated " + before + " → " + after);
        } else {
            logStep("Step 2b: list fits one page (no 'Load more') — header must not claim truncation");
            String header = engineerPage.getMatchHeaderText();
            assertTrue(!header.contains("+"),
                    "without 'Load more' the header must not show the truncated 'N+' variant; got '"
                            + header + "'");
            logStepWithScreenshot("TC_ENG_203 single page, header='" + header + "'");
        }

        logStep("Step 3: Discard the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "details must close with discard");
    }

    // ═══════════ ZP-2457 — Trip Type eligibility smoke (breaker draft) ═══════════

    @Test(priority = 2005)
    public void TC_ENG_204_breakerTripTypeNeverOffersGroundFault() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_204 - ZP-2457: breaker Trip Type picker never offers 'Ground Fault' (eligibility-filtered)");

        logStep("Step 1: Detailed Circuit Breaker draft");
        openDetailedDraftOfClass("Circuit", "Circuit Breaker");
        assertTrue(engineerPage.waitForMatchHeader(10),
                "breaker draft must render the unconditional OCP match panel");

        logStep("Step 2: Locate the Trip Type row (eligibleTripTypes render is library-gated)");
        boolean tripTypeShown = engineerPage.isEngineeringLabelPresent("Trip Type")
                || engineerPage.scrollToEngineeringLabel("Trip Type");

        // Both states are legitimate: the row renders only when the library
        // exposes eligible trip types for the current filter state.
        if (tripTypeShown) {
            logStep("Step 3a: Trip Type renders — open it and assert 'Ground Fault' is absent");
            engineerPage.openEngineeringPickerBelowLabel("Trip Type");
            mediumWait();
            assertFalse(engineerPage.isOptionShown("Ground Fault"),
                    "ZP-2457: 'Ground Fault' must NEVER appear as a Trip Type option"
                            + " (GF is its own settings pair, not a trip type)");
            logStepWithScreenshot("TC_ENG_204 Trip Type picker open, Ground Fault absent");
            if (engineerPage.isSheetPickerOpen(1)) {
                if (!engineerPage.tapRowByExactLabel("Cancel")) {
                    engineerPage.tapRowByExactLabel("Close");
                }
            } else {
                engineerPage.dismissMenuOverlay();
            }
        } else {
            logStep("Step 3b: Trip Type gated off in the unset state — panel must still be healthy");
            System.out.println("ℹ️ TC_ENG_204: no 'Trip Type' label on the unset breaker draft"
                    + " (eligibleTripTypes empty pre-filter, ZP-2457 gating) — asserting panel integrity instead");
            assertEquals(engineerPage.getMatchHeaderText(), "No possible matches",
                    "unset breaker draft must keep the zero header when Trip Type is gated off");
            logStepWithScreenshot("TC_ENG_204 Trip Type gated off; panel healthy");
        }

        logStep("Step 4: Discard the draft");
        assertTrue(engineerPage.closeAssetDetails(true), "details must close with discard");
    }

    // ═══════════ discard integrity ═══════════

    @Test(priority = 2006)
    public void TC_ENG_205_discardedPanelStateNeverLeaksIntoFreshDraft() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_205 - Discarded draft's manufacturer + search never leak into a fresh draft");

        logStep("Step 1: Fuse draft — mutate panel state (manufacturer + search text)");
        openDetailedDraftOfClass("Fuse", "Fuse");
        pickFuseManufacturer();
        engineerPage.typeIntoEngineeringField(MATCH_SEARCH_PLACEHOLDER, "6");
        mediumWait();

        logStep("Step 2: Discard the mutated draft");
        assertTrue(engineerPage.closeAssetDetails(true), "mutated draft must discard cleanly");

        logStep("Step 3: Fresh draft starts pristine — zero header + blank search");
        openDetailedDraftOfClass("Fuse", "Fuse");
        assertTrue(engineerPage.waitForMatchHeader(10),
                "fresh fuse draft must render the match panel after the discard");
        assertEquals(engineerPage.getMatchHeaderText(), "No possible matches",
                "fresh draft must start at the unset zero header — discarded manufacturer state leaked");
        assertEquals(engineerPage.getEngineeringFieldValue(MATCH_SEARCH_PLACEHOLDER), "",
                "match search must start blank in a fresh draft — discarded search text leaked");
        logStepWithScreenshot("TC_ENG_205 fresh draft pristine after discard");

        logStep("Step 4: Discard the probe draft");
        assertTrue(engineerPage.closeAssetDetails(true), "probe draft must discard cleanly");
    }
}
