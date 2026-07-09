package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetEngineerPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * asset_engineer module — v1.49 SKM Equipment Library + Engineering section
 * (ZP-2161 library/matcher, ZP-2794 encrypted transport).
 *
 * COVERAGE MAP (all locators from live v1.49 DOM dumps, target/engdump/):
 *   100s  Settings → Equipment Library card, Load Device Library alert,
 *         download flow (the state gate for everything below).
 *   200s  Engineering section on Asset Details per class:
 *         box (Node Bus "Ns"), fuse ("Trim600639 Fuse"),
 *         transformer ("Transformer-1") — labels, locked System Voltage,
 *         segments, v1.49 unconditional OCP match panel.
 *   300s  SKM matching: manufacturer menu, match header, numeric input
 *         filtering, bound-card fixture ("Test Busway" is library-bound),
 *         unlink draft-integrity (discard must not mutate data).
 *   400s  Custom equipment sheet (open, Save gating, cancel).
 *   500s  Add Asset (detailed) engineering mount points.
 *
 * DATA CONTRACT: every test is draft-only — asset details are always closed
 * via Discard; the only persistent mutation is the on-device library cache
 * (the module's own purpose). "Test Busway" must stay library-bound; the
 * unlink test verifies discard leaves it bound.
 *
 * The library download is per-app-container: NO_RESET=false wipes it each
 * driver session, so this class pins noReset(true) and TC_ENG_003 (or the
 * ensureEngineeringReady fallback in later tests) re-downloads when needed.
 */
public class AssetEngineer_Test extends BaseTest {

    /**
     * Library download takes 2–5 minutes in practice (network + 440k-row
     * SwiftData insert; ~25–60s on a warm local sim but much longer on CI
     * runners / cold caches). Poll for up to 10 minutes.
     */
    private static final int DOWNLOAD_TIMEOUT_S = 600;

    private AssetEngineerPage engineerPage;

    /** Whole module is gated on the platform-managed eng-lib company flag (BaseTest skips pre-driver when absent). */
    @Override
    protected String requiredCompanyFeature() {
        return "eng-lib";
    }

    @BeforeClass(alwaysRun = true)
    public void assetEngineerClassSetup() {
        System.out.println("\n📋 Asset Engineer Suite (v1.49 asset_engineer library) — Starting");
        DriverManager.setNoReset(true); // keep library cache + login across driver rebuilds
    }

    @BeforeMethod(alwaysRun = true)
    public void assetEngineerTestSetup() {
        // Gated/fast-skipped test: no driver exists, and BasePage's constructor
        // would throw. The test won't run, so the page object isn't needed.
        if (!DriverManager.isDriverActive()) return;
        engineerPage = new AssetEngineerPage(); // driver may have been rebuilt
    }

    @AfterClass(alwaysRun = true)
    public void assetEngineerClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    // ── shared navigation helpers ──────────────────────────────────────

    /** Login (idempotent) → Assets list → open asset → Engineering section. */
    private void openAssetEngineering(String assetPrefix) {
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        engineerPage.openAssetCardByPrefix(assetPrefix);
        guard("open '" + assetPrefix + "' details");
        assertTrue(engineerPage.swipeToEngineeringSection(),
                "Engineering section title should be reachable on '" + assetPrefix + "' details");
    }

    /**
     * Library gate for standalone runs: when the not-downloaded banner is on
     * the opened asset, run the Settings download and reopen the asset.
     * A failed download throws VerificationError (never becomes a SKIP).
     */
    private void ensureEngineeringUnlocked(String assetPrefix) {
        if (engineerPage.isLibraryMissingBannerShown()) {
            System.out.println("⏬ library missing — downloading via Settings (standalone-run gate)");
            engineerPage.closeAssetDetails(true);
            engineerPage.ensureLibraryDownloaded(DOWNLOAD_TIMEOUT_S);
            assetPage.navigateToAssetList();
            shortWait();
            engineerPage.openAssetCardByPrefix(assetPrefix);
            engineerPage.swipeToEngineeringSection();
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 100s — Settings / Equipment Library
    // ═════════════════════════════════════════════════════════════════

    @Test(priority = 101)
    public void TC_ENG_001_verifyEquipmentLibraryCardInSettings() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_001 - Verify Settings shows the Equipment Library card with a state subtitle");

        logStep("Step 1: Login and open Settings");
        loginAndSelectSite();
        engineerPage.openSettings();
        guard("open Settings tab");

        logStep("Step 2: Scroll to the Equipment Library card");
        assertTrue(engineerPage.scrollToLibraryCard(),
                "The 'Load Latest Equipment Library' card should become visible in Settings");
        assertTrue(engineerPage.isEngineeringLabelPresent(AssetEngineerPage.SETTINGS_SECTION_HEADER),
                "The 'Equipment Library' section header should be present above the card");

        logStep("Step 3: Card exposes a concrete library state");
        String subtitle = engineerPage.getLibraryCardSubtitle();
        assertNotNull(subtitle, "Library card subtitle must be readable");
        assertTrue(!subtitle.isEmpty(),
                "Library card must show a state subtitle (Not yet downloaded / Last updated … / counts), got empty");
        logStepWithScreenshot("TC_ENG_001: card present, state = '" + subtitle + "'");
    }

    @Test(priority = 102)
    public void TC_ENG_002_verifyLoadDeviceLibraryAlertContent() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_002 - Verify the Load Device Library confirmation alert (exact copy + both actions)");

        logStep("Step 1: Login, open Settings, tap the library card");
        loginAndSelectSite();
        engineerPage.openSettings();

        // Alert inspection needs WDA's auto-accept paused for the whole dance,
        // or the monitor presses Download before the copy can be read.
        engineerPage.withAlertsManual(() -> {
            engineerPage.tapLibraryCard();

            logStep("Step 2: Alert appears (animation-tolerant wait)");
            assertTrue(engineerPage.isLoadDialogShown(8),
                    "'Load Device Library?' alert should appear after tapping the card");

            logStep("Step 3: Exact message + both buttons");
            assertEquals(engineerPage.getLoadDialogMessage(),
                    "Downloads the full device library (~5 MB) to enable offline matching in the Engineering section. Replaces any prior cached library.",
                    "Alert message must match the v1.49 copy exactly");
            assertTrue(engineerPage.loadDialogHasButtons(),
                    "Alert must offer both Cancel and Download actions");

            logStep("Step 4: Cancel dismisses without downloading");
            assertTrue(engineerPage.tapLoadDialogButton("Cancel"),
                    "Cancel should dismiss the alert");
        });
        logStepWithScreenshot("TC_ENG_002: alert copy + Cancel verified");
    }

    // Explicit timeOut: the download alone can take 5 minutes, which would
    // trip the 360s GlobalTestTimeout default. Explicit values always win
    // over the transformer (see GlobalTestTimeout javadoc).
    @Test(priority = 103, timeOut = 780_000)
    public void TC_ENG_003_downloadEquipmentLibraryEndToEnd() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_003 - Download the SKM equipment library end-to-end (state gate for the module)");

        logStep("Step 1: Login and ensure the library is downloaded (downloads when missing)");
        loginAndSelectSite();
        boolean downloadedNow = engineerPage.ensureLibraryDownloaded(DOWNLOAD_TIMEOUT_S);
        System.out.println("ℹ️ download performed this run: " + downloadedNow);

        logStep("Step 2: Settings reports a downloaded terminal state");
        assertTrue(engineerPage.isLibraryDownloadedPerSettings(),
                "After ensureLibraryDownloaded, Settings must show 'Last updated …' or the counts summary; got '"
                        + engineerPage.getLibraryCardSubtitle() + "'");
        logStepWithScreenshot("TC_ENG_003: library ready (" + engineerPage.getLibraryCardSubtitle() + ")");
    }

    @Test(priority = 104)
    public void TC_ENG_004_verifyDownloadedStateSubtitleFormat() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_EQUIPMENT_LIBRARY,
                "TC_ENG_004 - Verify the downloaded-state subtitle format (counts summary or Last updated)");

        logStep("Step 1: Login, open Settings, read the card state");
        loginAndSelectSite();
        engineerPage.openSettings();
        assertTrue(engineerPage.scrollToLibraryCard(), "Library card should be reachable");
        String subtitle = engineerPage.getLibraryCardSubtitle();

        logStep("Step 2: Assert the exact expected format for the observed state");
        if (subtitle.contains(" frames,")) {
            // Fresh-download success string: "<n> frames, <n> sensors, <n> trip
            // units, <n> segments, <n> kVA entries, <n> cable / busway entries"
            // (counts are locale-grouped — never assert grouping).
            assertTrue(subtitle.contains(" sensors,") && subtitle.contains(" trip units,")
                            && subtitle.contains(" kVA entries,") && subtitle.contains("cable / busway entries"),
                    "Counts summary must list frames/sensors/trip units/kVA/cable-busway; got '" + subtitle + "'");
        } else {
            assertTrue(subtitle.startsWith(AssetEngineerPage.STATE_LAST_UPDATED_PREFIX),
                    "Idle downloaded state must start with 'Last updated'; got '" + subtitle
                            + "' (run TC_ENG_003 first on a fresh container)");
        }
        logStepWithScreenshot("TC_ENG_004: state format OK — '" + subtitle + "'");
    }

    // ═════════════════════════════════════════════════════════════════
    // 200s — Engineering section per asset class
    // ═════════════════════════════════════════════════════════════════

    @Test(priority = 201)
    public void TC_ENG_010_verifyEngineeringSectionUnlockedOnNodeBus() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_010 - Verify Engineering section present and not-downloaded banner gone (Node Bus)");

        logStep("Step 1: Open Node Bus asset engineering");
        openAssetEngineering("Ns");
        ensureEngineeringUnlocked("Ns");

        logStep("Step 2: Section present, banner absent");
        assertTrue(engineerPage.isEngineeringSectionPresent(),
                "'Engineering' section title must exist on asset details");
        assertFalse(engineerPage.isLibraryMissingBannerShown(),
                "'Equipment library not downloaded' banner must be GONE once the library is cached");
        logStepWithScreenshot("TC_ENG_010: engineering unlocked on Node Bus");

        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 202)
    public void TC_ENG_011_verifyBoxBlockFieldsOnNodeBus() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_011 - Verify box-class block: Mains Type + Phase Configuration + locked System Voltage");

        logStep("Step 1: Open Node Bus asset engineering");
        openAssetEngineering("Ns");
        ensureEngineeringUnlocked("Ns");

        logStep("Step 2: Box block fields (v1.49 renders Mains Type only — no Phase Configuration row)");
        assertTrue(engineerPage.isEngineeringLabelPresent("Mains Type"),
                "'Mains Type' label must render for box classes (Node Bus)");
        assertFalse(engineerPage.isEngineeringLabelPresent("Phase Configuration"),
                "v1.49 does NOT render 'Phase Configuration' on Node Bus — if this appears, the layout changed again");

        logStep("Step 3: System Voltage is propagated read-only (em dash + lock, no editable control)");
        assertTrue(engineerPage.isSystemVoltageLockedRowShown(),
                "System Voltage row must render locked (label + '—' + lock.fill) on Node Bus");
        logStepWithScreenshot("TC_ENG_011: box block verified");

        engineerPage.closeAssetDetails(true);
    }

    /**
     * DOMAIN CONSTRAINT (user-confirmed): Mains Type is NOT selectable on
     * Node Bus — the menu opens and lists the server enums, but a pick does
     * not apply. This test therefore verifies the menu contents (proof the
     * /v2/equipment-library/enums sync populated EnumNodeMainsType) and a
     * clean dismiss; the selection ROUND-TRIP is tested on ATS (TC_ENG_016).
     */
    @Test(priority = 203)
    public void TC_ENG_012_verifyMainsTypeMenuShowsLibraryEnums() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_012 - Mains Type menu lists server-synced enums on Node Bus (selection not supported here)");

        logStep("Step 1: Open Node Bus asset engineering");
        openAssetEngineering("Ns");
        ensureEngineeringUnlocked("Ns");
        String before = engineerPage.getPickerValueBelowLabel("Mains Type");

        logStep("Step 2: Open the Mains Type menu");
        engineerPage.openEngineeringPickerBelowLabel("Mains Type");
        assertTrue(engineerPage.waitForOptionShown("MCB", 6),
                "Mains Type menu must list 'MCB' (server enum from /v2/equipment-library/enums)");

        logStep("Step 3: Menu lists the full server-enum set (observed live on v1.49)");
        assertTrue(engineerPage.isOptionShown("MLO"), "Mains Type menu must list 'MLO'");
        assertTrue(engineerPage.isOptionShown("FDS"), "Mains Type menu must list 'FDS'");
        assertTrue(engineerPage.isOptionShown("NFDS"), "Mains Type menu must list 'NFDS'");
        assertTrue(engineerPage.isOptionShown("None"),
                "Mains Type menu must include the 'None' clear row (allowClear picker)");
        logStepWithScreenshot("TC_ENG_012: enum menu contents verified");

        logStep("Step 4: Dismiss the menu; chip must be unchanged");
        engineerPage.dismissMenuOverlay();
        assertEquals(engineerPage.getPickerValueBelowLabel("Mains Type"), before,
                "Dismissing the menu without a pick must leave the Mains Type chip unchanged");

        engineerPage.closeAssetDetails(true);
    }

    /**
     * Mains Type SELECTION round-trip. Domain rule (user-confirmed): to
     * select a Mains Type you need a Panelcode-class asset. No Panelcode
     * asset exists in the QA site, so the pick happens inside the Add Asset
     * DETAILED form (ZP-2368 mounts the full engineering section during
     * creation) and the form is cancelled — nothing is ever saved.
     */
    @Test(priority = 207)
    public void TC_ENG_016_verifyMainsTypeSelectionOnPanelcodeDraft() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_016 - Mains Type selects on a Panelcode draft (Add Asset detailed); cancel discards");

        logStep("Step 1: Open Add Asset in Detailed mode");
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        mediumWait();
        guard("open Add Asset form");
        assertTrue(engineerPage.pickOptionExact("Detailed"),
                "'Detailed' creation-mode toggle must be tappable");

        logStep("Step 2: Pick the Panelcode class");
        assetPage.clickSelectAssetClass();
        mediumWait();
        // The class list is long and searchable — search instead of scrolling
        // (also discovers the exact class name: Panelcode/Panel Code/…).
        engineerPage.searchInSheetPicker("anel");
        final String pickedClass = engineerPage.pickFirstVisibleOptionContaining("anel");
        skipIfPreconditionMissing(() -> pickedClass != null,
                "no Panel* class in this site's class list — cannot exercise Mains Type selection");
        System.out.println("ℹ️ picked class: " + pickedClass);
        mediumWait();

        logStep("Step 3: Engineering section mounts in the form; open Mains Type and pick MCB");
        assertTrue(engineerPage.swipeToEngineeringSection(),
                "Engineering section must mount in the detailed Add Asset form (ZP-2368)");
        engineerPage.openEngineeringPickerBelowLabel("Mains Type");
        assertTrue(engineerPage.pickOptionExact("MCB"),
                "'MCB' must be pickable in the Panelboard Mains Type menu");

        logStep("Step 4: Picking MCB opens the 'Create a Main Breaker?' sheet (panel main auto-wiring)");
        assertTrue(engineerPage.waitForOptionShown("Create Main", 8),
                "Selecting MCB on a panel must open the Create-Main sheet with a 'Create Main' action");
        assertTrue(engineerPage.isEngineeringLabelPresent("Create a Main Breaker?"),
                "Create-Main sheet must be titled 'Create a Main Breaker?'");
        assertTrue(engineerPage.isEngineeringLabelPresent("Main Details"),
                "Create-Main sheet must show the 'Main Details' section (Name/Subtype/Pole Count)");
        logStepWithScreenshot("TC_ENG_016: MCB selection registered — Create-Main sheet shown");

        logStep("Step 5: Cancel the Create-Main sheet — no child breaker is created");
        assertTrue(engineerPage.pickOptionExact("Cancel"),
                "Create-Main sheet Cancel must be tappable");
        assertTrue(engineerPage.waitForLabelGone("Create a Main Breaker?", 6),
                "Create-Main sheet must dismiss on Cancel");

        logStep("Step 6: Cancel the whole draft — nothing persists");
        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 204)
    public void TC_ENG_013_verifyFuseBlockSegmentsAndSubtype() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_013 - Verify fuse block: Fuse Count segments 1/2/3 + subtype picker + locked voltage");

        logStep("Step 1: Open the Fuse asset engineering");
        openAssetEngineering("Trim600639 Fuse");
        ensureEngineeringUnlocked("Trim600639 Fuse");

        logStep("Step 2: Fuse-specific fields (v1.49 layout)");
        assertTrue(engineerPage.isEngineeringLabelPresent("Fuse Count"),
                "'Fuse Count' label must render for fuse classes");
        assertTrue(engineerPage.segmentExists("1") && engineerPage.segmentExists("2")
                        && engineerPage.segmentExists("3"),
                "Fuse Count must render plain 1/2/3 segments (count style, not 1P/2P/3P)");
        assertTrue(engineerPage.isSubtypePickerShown(),
                "'Select asset subtype' picker chip must render on the unbound fuse");
        assertTrue(engineerPage.isSystemVoltageLockedRowShown(),
                "System Voltage must render locked on the fuse (propagated read-only)");
        logStepWithScreenshot("TC_ENG_013: fuse block verified");

        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 205)
    public void TC_ENG_014_verifyUnconditionalMatchPanelOnFuse() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_014 - Verify v1.49 OCP match panel renders unset: header, Add Custom, search, empty-state");

        logStep("Step 1: Open the Fuse asset engineering");
        openAssetEngineering("Trim600639 Fuse");
        ensureEngineeringUnlocked("Trim600639 Fuse");

        logStep("Step 2: Match panel renders before any manufacturer is chosen (v1.49 behavior)");
        assertEquals(engineerPage.getMatchHeaderText(), "No possible matches",
                "Unset OCP match panel must show the zero-state header");
        assertTrue(engineerPage.isAddCustomButtonShown(),
                "'Add Custom' chip must render in the match panel");
        assertTrue(engineerPage.isMatchSearchFieldShown(),
                "Match search field with placeholder e.g. \"QD\" or \"Formula\" must render");
        assertEquals(engineerPage.getMatchEmptyStateText(),
                "No matches — refine the filters above or load the SKM library from Settings.",
                "Empty-state hint must match the v1.49 copy exactly");

        logStep("Step 3: Manufacturer filter lives inside the panel");
        assertTrue(engineerPage.isEngineeringLabelPresent("Manufacturer"),
                "'Manufacturer' filter label must render in the fuse match panel");
        logStepWithScreenshot("TC_ENG_014: unconditional match panel verified");

        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 206)
    public void TC_ENG_015_verifyTransformerBlockFields() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_015 - Verify transformer block: kVA/% impedance fields, Dry/Oil type segments, voltages");

        logStep("Step 1: Open Transformer-1 engineering");
        openAssetEngineering("Transformer-1");
        ensureEngineeringUnlocked("Transformer-1");

        logStep("Step 2: Transformer numeric fields (exact live placeholders)");
        assertTrue(engineerPage.isEngineeringLabelPresent("kVA Rating"),
                "'kVA Rating' label must render for transformer classes");
        assertTrue(engineerPage.isEngineeringFieldPresent("Enter kva rating"),
                "kVA Rating text field (placeholder 'Enter kva rating') must render");
        assertTrue(engineerPage.isEngineeringLabelPresent("% Impedance"),
                "'% Impedance' label must render for transformer classes");
        assertTrue(engineerPage.isEngineeringFieldPresent("Enter % impedance"),
                "% Impedance text field (placeholder 'Enter % impedance') must render");

        logStep("Step 3: Type segments + voltages (primary locked, secondary picker)");
        assertTrue(engineerPage.segmentExists("Dry Type") && engineerPage.segmentExists("Oil-Filled"),
                "Transformer 'Type' must render as Dry Type / Oil-Filled segments");
        assertTrue(engineerPage.isEngineeringLabelPresent("Primary Voltage"),
                "'Primary Voltage' label must render");
        assertTrue(engineerPage.isEngineeringLabelPresent("Secondary Voltage"),
                "'Secondary Voltage' label must render");
        logStepWithScreenshot("TC_ENG_015: transformer block verified");

        engineerPage.closeAssetDetails(true);
    }

    // ═════════════════════════════════════════════════════════════════
    // 300s — SKM matching + bound card
    // ═════════════════════════════════════════════════════════════════

    /**
     * WEDGE CONTAINMENT: this test runs LAST (priority 601) and deliberately
     * does NOT pick a manufacturer. On v1.49, a registered pick renders the
     * match panel on the transformer's giant DOM and WDA's next snapshot
     * times out, killing the session (reproduced twice — see changelog).
     * Open-verify-dismiss covers the library-backed menu contents without
     * ever triggering the wedge; the match-panel surface itself is covered
     * on the fuse (TC_ENG_014), whose DOM is safe.
     */
    @Test(priority = 601)
    public void TC_ENG_020_verifyTransformerManufacturerMenuIsLibraryBacked() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_020 - Transformer Manufacturer menu lists only library-backed manufacturers");

        logStep("Step 1: Open Transformer-1 engineering");
        openAssetEngineering("Transformer-1");
        ensureEngineeringUnlocked("Transformer-1");
        String before = engineerPage.getPickerValueBelowLabel("Manufacturer");

        logStep("Step 2: Open the Manufacturer picker (menu of library-backed manufacturers)");
        engineerPage.openEngineeringPickerBelowLabel("Manufacturer");
        assertTrue(engineerPage.waitForOptionShown("Generic", 6),
                "Manufacturer menu must offer 'Generic' (library-backed transformer manufacturer)");
        assertTrue(engineerPage.isOptionShown("SCHNEIDER/SQUARE D"),
                "Manufacturer menu must offer 'SCHNEIDER/SQUARE D' (library-backed transformer manufacturer)");
        logStepWithScreenshot("TC_ENG_020: library-backed manufacturer menu verified");

        logStep("Step 3: Dismiss without picking (a pick triggers the documented WDA wedge)");
        engineerPage.dismissMenuOverlay();
        assertEquals(engineerPage.getPickerValueBelowLabel("Manufacturer"), before,
                "Dismissing the menu without a pick must leave the Manufacturer chip unchanged");

        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 302)
    public void TC_ENG_021_verifyKvaFieldNumericInputFiltering() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_021 - kVA field strips non-numeric input and allows a single decimal point");

        logStep("Step 1: Open Transformer-1 engineering");
        openAssetEngineering("Transformer-1");
        ensureEngineeringUnlocked("Transformer-1");

        logStep("Step 2: Type mixed garbage into kVA Rating");
        engineerPage.typeIntoEngineeringField("Enter kva rating", "12a3.5.7");
        String value = engineerPage.getEngineeringFieldValue("Enter kva rating");
        assertEquals(value, "123.57",
                "EngineeringDoubleField must keep digits + first dot only ('12a3.5.7' → '123.57')");
        logStepWithScreenshot("TC_ENG_021: input filter verified ('" + value + "')");

        logStep("Step 3: Discard draft");
        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 303)
    public void TC_ENG_022_verifyBoundLibraryCardOnBusway() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_022 - Bound asset shows Library Matched card + Unlink + busway config card");

        logStep("Step 1: Open the bound busway fixture");
        openAssetEngineering("Test Busway");
        skipIfPreconditionMissing(() -> engineerPage.isBoundCardShown(),
                "'Test Busway' is no longer library-bound — bound-card fixture missing");

        logStep("Step 2: Bound card contents");
        assertTrue(engineerPage.isEngineeringLabelPresent(AssetEngineerPage.BOUND_LIBRARY_MATCHED),
                "'Library Matched' title must render on the bound card");
        assertTrue(engineerPage.isUnlinkButtonShown(), "'Unlink' button must render on the bound card");

        logStep("Step 3: Busway configurator card renders below the bound card");
        assertTrue(engineerPage.isEngineeringLabelPresent("Busway Configuration"),
                "'Busway Configuration' header must render for the bound busway");
        assertTrue(engineerPage.isEngineeringLabelPresent("Length"),
                "'Length' field must render in the busway config card");
        assertTrue(engineerPage.isEngineeringLabelPresent("Qty per Phase"),
                "'Qty per Phase' field must render in the busway config card");

        logStep("Step 4: Bound state hides the input stack (subtype/voltage/matcher)");
        assertFalse(engineerPage.isSubtypePickerShown(),
                "Subtype picker must be hidden while a library row is bound");
        assertFalse(engineerPage.isSystemVoltageLockedRowShown(),
                "System Voltage row must be hidden while a library row is bound (busway carries no own voltage)");
        logStepWithScreenshot("TC_ENG_022: bound card + config card verified");

        engineerPage.closeAssetDetails(true);
    }

    @Test(priority = 304)
    public void TC_ENG_023_verifyUnlinkDraftDiscardKeepsBinding() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_LIBRARY_MATCHING,
                "TC_ENG_023 - Unlink is draft-only: discarding must keep the asset bound (data integrity)");

        logStep("Step 1: Open the bound busway fixture");
        openAssetEngineering("Test Busway");
        skipIfPreconditionMissing(() -> engineerPage.isBoundCardShown(),
                "'Test Busway' is no longer library-bound — bound-card fixture missing");

        logStep("Step 2: Unlink in the draft");
        assertTrue(engineerPage.tapUnlink(), "Unlink must remove the bound card from the draft UI");
        assertTrue(engineerPage.areConductorMaterialSegmentsShown(),
                "Unbinding must re-surface the busway input stack (Cu/Al conductor segments)");

        logStep("Step 3: Close with Discard, reopen — binding must survive");
        assertTrue(engineerPage.closeAssetDetails(true), "Details should close back to the Assets list");
        engineerPage.openAssetCardByPrefix("Test Busway");
        engineerPage.swipeToEngineeringSection();
        assertTrue(engineerPage.isBoundCardShown(),
                "DATA INTEGRITY: discarding an unlink draft must leave 'Test Busway' library-bound");
        logStepWithScreenshot("TC_ENG_023: unlink discard integrity verified");

        engineerPage.closeAssetDetails(true);
    }

    // ═════════════════════════════════════════════════════════════════
    // 400s — Custom equipment sheet
    // ═════════════════════════════════════════════════════════════════

    @Test(priority = 401)
    public void TC_ENG_030_verifyAddCustomSheetOpensWithSaveDisabled() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_CUSTOM_EQUIPMENT,
                "TC_ENG_030 - Add Custom sheet opens with Identity fields and Save disabled until identity is set");

        logStep("Step 1: Open the Fuse asset engineering (unconditional match panel)");
        openAssetEngineering("Trim600639 Fuse");
        ensureEngineeringUnlocked("Trim600639 Fuse");

        logStep("Step 2: Open the Add Custom sheet");
        engineerPage.tapAddCustom();
        assertTrue(engineerPage.isCustomSheetOpen(8),
                "'Add Custom Equipment' sheet must open from the match panel");

        logStep("Step 3: Save is gated on identity (Manufacturer or Type / Catalog)");
        assertFalse(engineerPage.isCustomSaveEnabled(),
                "Save must be DISABLED while Manufacturer and Type / Catalog are both blank");

        logStep("Step 4: Typing a manufacturer enables Save");
        engineerPage.typeCustomField("Manufacturer", "QA Custom Mfr");
        assertTrue(engineerPage.isCustomSaveEnabled(),
                "Save must ENABLE once Manufacturer is non-blank");

        logStep("Step 5: Cancel leaves no binding");
        engineerPage.tapCustomSheetButton("Cancel");
        mediumWait();
        assertFalse(engineerPage.isCustomEntryCardShown(),
                "Cancelling the custom sheet must not create a CUSTOM ENTRY binding");
        logStepWithScreenshot("TC_ENG_030: custom sheet gating verified");

        engineerPage.closeAssetDetails(true);
    }

    // ═════════════════════════════════════════════════════════════════
    // 500s — Add Asset (detailed) engineering mount
    // ═════════════════════════════════════════════════════════════════

    @Test(priority = 501)
    public void TC_ENG_040_verifyAddAssetModesAndClassPicker() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER, AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_040 - Add Asset offers Quick/Detailed modes and a class picker with Circuit Breaker");

        logStep("Step 1: Open the Add Asset form");
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        mediumWait();
        guard("open Add Asset form");

        logStep("Step 2: Creation-mode toggles present (engineering mounts only in Detailed)");
        assertTrue(engineerPage.isOptionShown("Quick"), "'Quick' creation-mode toggle must render");
        assertTrue(engineerPage.isOptionShown("Detailed"), "'Detailed' creation-mode toggle must render");

        logStep("Step 3: Class picker opens and offers Circuit Breaker");
        assetPage.clickSelectAssetClass();
        mediumWait();
        assertTrue(engineerPage.isOptionShown("Circuit Breaker"),
                "Class picker must list 'Circuit Breaker'");
        logStepWithScreenshot("TC_ENG_040: add-asset modes + class picker verified");

        logStep("Step 4: Cancel the class picker, then the draft — nothing created");
        // The class picker is a PUSHED fullscreen list, not a menu — a
        // coordinate tap outside cannot dismiss it; its Cancel button can.
        assertTrue(engineerPage.pickOptionExact("Cancel"),
                "Class picker Cancel must be tappable");
        assertTrue(engineerPage.closeAssetDetails(true),
                "Add Asset form must cancel back to the Assets list (no stranded draft)");
    }
}
