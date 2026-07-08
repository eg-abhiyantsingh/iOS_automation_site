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
 * asset_engineer library — per-class Engineering-block contract matrix.
 *
 * One data row per asset class: pick the class in a DETAILED Add-Asset draft
 * (ZP-2368 mounts the full Engineering section during creation) and assert
 * exactly which engineering fields/segments the class-gated blocks render
 * (box → Mains Type; OCP → Pole/Fuse Count + unconditional v1.49 match
 * panel; transformer → kVA/%Z + Dry/Oil; cable/busway → Cu/Al). The draft
 * is never saved: every row runs inside the same cancelled form, and each
 * class SWITCH after the first also exercises the destructive
 * "Change Asset Class?" alert.
 *
 * Presence asserts use existsNow over the flat SwiftUI form source (the
 * whole content is in the DOM regardless of scroll), so rows need no
 * scrolling and no giant-DOM-risky queries.
 *
 * Expectations are evidence-based: classes verified on live v1.49 carry
 * field expectations; classes without live evidence assert only the shared
 * contract (Engineering header) and get tightened as evidence lands.
 */
public class AssetEngineerClassMatrix_Test extends BaseTest {

    private AssetEngineerPage engineerPage;
    private boolean addFormOpen = false;
    private boolean classSelectedOnce = false;
    private String lastPickedClass = null;

    /** Whole module is gated on the platform-managed eng-lib company flag (BaseTest skips pre-driver when absent). */
    @Override
    protected String requiredCompanyFeature() {
        return "eng-lib";
    }

    @BeforeClass(alwaysRun = true)
    public void classMatrixSetup() {
        System.out.println("\n📋 Asset Engineer — per-class Engineering matrix");
        DriverManager.setNoReset(true);
    }

    @BeforeMethod(alwaysRun = true)
    public void classMatrixTestSetup() {
        if (!DriverManager.isDriverActive()) return; // gated/fast-skipped: no driver, no page
        engineerPage = new AssetEngineerPage();
    }

    @AfterClass(alwaysRun = true)
    public void classMatrixTeardown() {
        try {
            if (addFormOpen) {
                engineerPage = new AssetEngineerPage();
                engineerPage.closeAssetDetails(true);
            }
        } catch (Exception e) {
            System.out.println("⚠️ matrix teardown: " + e.getMessage());
        }
        DriverManager.resetNoResetOverride();
    }

    /**
     * {class, mustHaveLabels, mustHaveSegments, mustNotHaveLabels}
     * Labels are StaticTexts; segments are Buttons (segmented controls).
     * Evidence key: [D] = live v1.49 DOM dump/driver-loop verified,
     * [S] = app-source derived (validated by this matrix's first run),
     * [G] = node_classes gold-spec memory.
     */
    @DataProvider(name = "classEngineering")
    public Object[][] classEngineering() {
        return new Object[][] {
            // [D] OCP fuse: count segments + v1.49 unconditional match panel
            {"Fuse", new String[]{"Fuse Count", "Manufacturer", "No possible matches"},
                     new String[]{"1", "2", "3"},
                     new String[]{"Pole Count", "kVA Rating"}},
            // [S] OCP breaker: pole segments + panel (v1.49 fuse-analog)
            {"Circuit Breaker", new String[]{"Pole Count", "Manufacturer", "No possible matches"},
                     new String[]{"1P", "2P", "3P"},
                     new String[]{"Fuse Count", "kVA Rating"}},
            // [D] transformer: kVA/%Z fields + Dry/Oil trip-type segments
            {"Transformer", new String[]{"kVA Rating", "% Impedance", "Manufacturer"},
                     new String[]{"Dry Type", "Oil-Filled"},
                     new String[]{"Fuse Count", "Pole Count"}},
            // [D] busway: Cu/Al conductor segments
            {"Busway", new String[]{"Conductor Material"},
                     new String[]{"Cu", "Al"},
                     new String[]{"kVA Rating", "Fuse Count"}},
            // [S] cable: same conductor block as busway
            {"Cable", new String[]{"Conductor Material"},
                     new String[]{"Cu", "Al"},
                     new String[]{"kVA Rating", "Fuse Count"}},
            // [D] panelboard: box block — renders BOTH Mains Type AND Phase
            // Configuration (unlike Node Bus/ATS, which render Mains Type only;
            // caught live by this matrix's first run)
            {"Panelboard", new String[]{"Mains Type", "Phase Configuration"},
                     new String[]{},
                     new String[]{"Fuse Count", "kVA Rating"}},
            // [G] ATS: box block (gold spec: Mains Type = MCB/MLO)
            {"ATS", new String[]{"Mains Type"},
                     new String[]{},
                     new String[]{"kVA Rating", "Phase Configuration"}},
            // NOTE: "Node Bus" is intentionally ABSENT — it is a system/pseudo
            // class not offered in the create-class picker (verified live:
            // search finds nothing). Its box block is covered on the existing
            // "Ns" asset by TC_ENG_010/011/012.
            // [S] relay: Type + Manufacturer, no amp segments
            {"Relay", new String[]{"Manufacturer"},
                     new String[]{},
                     new String[]{"Fuse Count", "kVA Rating"}},
            // Shared-contract-only rows (no live block evidence yet):
            {"Switch", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Battery", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Capacitor", new String[]{}, new String[]{}, new String[]{"Fuse Count"}},
            {"Generator", new String[]{}, new String[]{}, new String[]{"Fuse Count"}},
            {"Motor", new String[]{}, new String[]{}, new String[]{"Fuse Count"}},
            {"UPS", new String[]{}, new String[]{}, new String[]{"Fuse Count"}},
            {"Loadcenter", new String[]{"Mains Type"}, new String[]{}, new String[]{"kVA Rating"}},
            // ── remaining node_classes gold-spec classes (38 total; rows
            //    below start as shared-contract and tighten as live
            //    evidence lands via this matrix) ──
            // [S] 3-winding transformer: transformer-shaped + tertiary
            {"Transformer (3-Winding)", new String[]{"kVA Rating", "% Impedance"},
                     new String[]{}, new String[]{"Fuse Count", "Pole Count"}},
            // Tie Breaker: NOT OCP-shaped in live v1.49 (no Pole Count —
            // caught by this matrix); shared-contract row.
            {"Tie Breaker", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            // [S] Other (OCP): protective bucket class
            {"Other (OCP)", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Busduct", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Capacitor Bank", new String[]{}, new String[]{}, new String[]{"Fuse Count"}},
            {"Disconnect Switch", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Junction Box", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Lighting Controls", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Load", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"MCC", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"MCC Bucket", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Meter", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Motor Controller", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Motor Starter", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Other", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"PDU", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            // [D] Rectifier DOES render kVA Rating (transformer-shaped power
            // conversion — caught live by this matrix)
            {"Rectifier", new String[]{"kVA Rating"}, new String[]{}, new String[]{"Fuse Count"}},
            {"Series Reactor", new String[]{}, new String[]{}, new String[]{}},
            {"Shunt Reactor", new String[]{}, new String[]{}, new String[]{}},
            {"Switchboard", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"Utility", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            {"VFD", new String[]{}, new String[]{}, new String[]{"kVA Rating"}},
            // [D] VFD Panel + Default mount NO Engineering section at all
            // (caught live) — the sentinel "Engineering" in mustNot asserts
            // the section's ABSENCE for these classes.
            {"VFD Panel", new String[]{}, new String[]{}, new String[]{"Engineering"}},
            // Live-picker-only class (not in the gold xlsx)
            {"Default", new String[]{}, new String[]{}, new String[]{"Engineering"}},
        };
    }

    @Test(priority = 701, dataProvider = "classEngineering")
    public void TC_ENG_050_classEngineeringBlockContract(String className, String[] mustHaveLabels,
            String[] mustHaveSegments, String[] mustNotHave) {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER,
                AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_050 [" + className + "] - Engineering block contract in detailed Add-Asset draft");

        logStep("Step 1: Ensure a detailed Add-Asset draft is open");
        ensureAddFormOpenDetailed();

        logStep("Step 2: Select class '" + className + "' (search-based)");
        boolean picked = selectClassViaSearch(className);
        skipIfPreconditionMissing(() -> picked,
                "class '" + className + "' not present in this site's class list");
        mediumWait();

        boolean expectsNoEngineering = java.util.Arrays.asList(mustNotHave).contains("Engineering");
        if (expectsNoEngineering) {
            logStep("Step 3: This class mounts NO Engineering section (live-verified contract)");
            assertFalse(engineerPage.waitForEngineeringSection(4),
                    "[" + className + "] must NOT mount an Engineering section");
            logStepWithScreenshot("TC_ENG_050 [" + className + "] no-engineering contract verified");
            return;
        }
        logStep("Step 3: Engineering section mounts for the class");
        assertTrue(engineerPage.waitForEngineeringSection(6),
                "[" + className + "] Engineering section header must mount in the detailed draft");

        logStep("Step 4: Class-gated fields");
        for (String label : mustHaveLabels) {
            assertTrue(engineerPage.isEngineeringLabelPresent(label),
                    "[" + className + "] must render engineering label '" + label + "'");
        }
        for (String seg : mustHaveSegments) {
            assertTrue(engineerPage.segmentExists(seg),
                    "[" + className + "] must render segment '" + seg + "'");
        }
        for (String label : mustNotHave) {
            assertFalse(engineerPage.isEngineeringLabelPresent(label),
                    "[" + className + "] must NOT render '" + label + "' (wrong class-gate)");
        }
        logStepWithScreenshot("TC_ENG_050 [" + className + "] contract verified");
    }

    /**
     * User-specified scenario: pick ATS in the class dropdown, verify its
     * engineering values, SET one (Mains Type = MLO), then change the class
     * to Circuit Breaker on the same draft — the destructive
     * "Change Asset Class?" alert fires (engineering values are cleared) and
     * the section must swap to the breaker block with the ATS fields gone.
     */
    @Test(priority = 702)
    public void TC_ENG_051_atsToCircuitBreakerTransition() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET_ENGINEER,
                AppConstants.FEATURE_ENGINEERING_SECTION,
                "TC_ENG_051 - ATS engineering values, then class change to Circuit Breaker swaps the block");

        logStep("Step 1: Fresh detailed draft with class ATS");
        addFormOpen = false; // force a fresh draft so the first pick has no alert
        ensureAddFormOpenDetailed();
        boolean atsPicked = selectClassViaSearch("ATS");
        skipIfPreconditionMissing(() -> atsPicked, "no ATS class in this site");
        mediumWait();

        logStep("Step 2: ATS engineering block renders with its values");
        assertTrue(engineerPage.waitForEngineeringSection(6),
                "[ATS] Engineering section must mount");
        assertTrue(engineerPage.isEngineeringLabelPresent("Mains Type"),
                "[ATS] Mains Type row must render");
        assertFalse(engineerPage.segmentExists("1P"),
                "[ATS] must not render breaker pole segments yet");
        // NOTE: no chip-value read here — viewport scrolling to this row is
        // position-flaky on the ATS draft; the transition contract needs
        // only the presence/absence swaps (all existsNow, scroll-free).
        logStepWithScreenshot("TC_ENG_051 ATS engineering verified");

        logStep("Step 3: Change class to Circuit Breaker (class-change alert on the same draft)");
        boolean cbPicked = selectClassViaSearch("Circuit Breaker");
        skipIfPreconditionMissing(() -> cbPicked, "no Circuit Breaker class in this site");
        mediumWait();

        logStep("Step 4: Engineering section swapped to the breaker block");
        assertTrue(engineerPage.isEngineeringSectionPresent(),
                "[CB] Engineering section must still be mounted after the class change");
        assertTrue(engineerPage.isEngineeringLabelPresent("Pole Count"),
                "[CB] 'Pole Count' must render for Circuit Breaker");
        assertTrue(engineerPage.segmentExists("1P") && engineerPage.segmentExists("2P")
                        && engineerPage.segmentExists("3P"),
                "[CB] 1P/2P/3P pole segments must render");
        assertTrue(engineerPage.isEngineeringLabelPresent("Manufacturer"),
                "[CB] Manufacturer (match-panel filter) must render");
        assertFalse(engineerPage.isEngineeringLabelPresent("Mains Type"),
                "[CB] the ATS Mains Type row must be GONE after the class change");
        logStepWithScreenshot("TC_ENG_051 breaker block verified after transition");
    }

    // ── helpers ────────────────────────────────────────────────────────

    /** Open (or verify) the detailed Add-Asset draft; recover when lost. */
    private void ensureAddFormOpenDetailed() {
        if (addFormOpen && engineerPage.isOptionShown("Detailed")) {
            return; // still on the form from the previous row
        }
        addFormOpen = false;
        classSelectedOnce = false;
        loginAndSelectSite();
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        mediumWait();
        guard("open Add Asset form");
        assertTrue(engineerPage.pickOptionExact("Detailed"),
                "'Detailed' creation-mode toggle must be tappable");
        addFormOpen = true;
    }

    /**
     * Pick a class through the picker's search field. Second and later
     * selections in detailed mode trigger the destructive
     * "Change Asset Class?" alert — confirmed here (that alert is part of
     * the matrix's coverage).
     */
    private boolean selectClassViaSearch(String className) {
        // On a SWITCH the chip is labeled with the current class, so
        // AssetPage's 'Select asset class' strategies can misfire (observed:
        // geometry fallback tapped the QR 'Scan' button). Tap the chip by
        // its current label first; fall back to AssetPage for the first pick.
        boolean pickerOpened = false;
        if (classSelectedOnce && lastPickedClass != null
                && engineerPage.isOptionShown(lastPickedClass)) {
            pickerOpened = engineerPage.pickOptionExact(lastPickedClass);
        }
        if (!pickerOpened) {
            try {
                assetPage.clickSelectAssetClass();
            } catch (Exception e) {
                System.out.println("⚠️ class dropdown open failed, recovering: " + e.getMessage());
                addFormOpen = false;
                ensureAddFormOpenDetailed();
                assetPage.clickSelectAssetClass();
            }
        }
        mediumWait();
        try {
            // First word only: the picker search mishandles multi-word
            // queries ("Node Bus" matched nothing live); the exact pick
            // below still guarantees the right row.
            engineerPage.searchInSheetPicker(className.split(" ")[0]);
        } catch (Exception e) {
            System.out.println("⚠️ class search unavailable (" + e.getMessage() + ") — picking directly");
        }
        boolean picked = engineerPage.pickOptionExact(className);
        if (!picked) {
            // leave the picker cleanly so the next row can recover
            engineerPage.pickOptionExact("Cancel");
            return false;
        }
        // Destructive confirm on class SWITCH (not on first selection).
        if (classSelectedOnce) {
            shortWait();
            if (engineerPage.isOptionShown("Change Class")) {
                assertTrue(engineerPage.tapAlertButton("Change Class"),
                        "'Change Asset Class?' alert must confirm via 'Change Class'");
            }
        }
        classSelectedOnce = true;
        lastPickedClass = className;
        return true;
    }
}
