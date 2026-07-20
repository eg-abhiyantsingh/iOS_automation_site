package com.egalvanic.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;

/**
 * ARC FLASH BY ASSET — 100% per-asset-class coverage (TC_AF_101-139).
 *
 * The feature-driven AF suite (dashboard buckets, punchlist toggles,
 * invariants) never says WHICH ASSET had its arc-flash data checked. This
 * class flips the axis: ONE simply-named test per asset class ("Fuse Arc
 * Flash", "ATS Arc Flash", …) covering ALL classes of the node-classes
 * gold spec (+ live-only Node Bus), so the report reads "Fuse Arc Flash:
 * PASS/FAIL" and logs the values actually read for that asset.
 *
 * MATCHING IS BY CLASS, NEVER BY NAME (learned live 2026-07-20): users name
 * assets anything — "Transformer-1-Transformer-2-BUS" is a Node Bus. The
 * asset-list cell's accessibility name is a composite
 *     "<asset name>, <id>, <Class>"
 * so a candidate is accepted ONLY when its trailing segment equals the
 * target class. The ", " boundary makes every gold class an unambiguous
 * suffix (Load vs Loadcenter, MCC vs MCC Bucket, Motor vs Motor Starter,
 * Transformer vs Transformer (3-Winding), VFD vs VFD Panel…).
 *
 * Contract per class:
 *   1. An asset OF THE CLASS exists on the fixture site (class-name search —
 *      the search field covers "name, type, location" — then class-suffix
 *      scan of the cells; bounded scroll + unfiltered rescan as fallback).
 *      If the site has none → honest SKIP naming the missing fixture.
 *   2. Its details screen opens.
 *   3. The arc-flash data surface renders for it: at least one AF-relevant
 *      field label (Voltage / Ampere Rating / Interrupting Rating / …) is
 *      present.
 *   4. The values next to those labels are read from PAGE SOURCE (the XCTest
 *      query layer lies on heavy detail DOMs) and logged into the report.
 *
 * Labels follow the node-classes gold spec ("trust NAMES, verify live");
 * 'Voltage' is the near-universal AF field, so it heads every list. The
 * presence law is any-one-present; every present label gets its value logged.
 */
public class ArcFlashAssetMatrix_Test extends BaseTest {

    private AssetPage assetPageLocal;

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        System.out.println("\n📋 Arc Flash BY ASSET matrix — Starting (39 classes)");
        DriverManager.setNoReset(true); // login once, soft-restart between tests
    }

    @BeforeMethod(alwaysRun = true)
    public void testSetupLocal() {
        if (!DriverManager.isDriverActive()) return;
        assetPageLocal = new AssetPage();
    }

    @AfterClass(alwaysRun = true)
    public void classTeardown() {
        DriverManager.resetNoResetOverride();
    }

    // ── One simply-named test per class ─────────────────────────────────

    @Test
    public void TC_AF_101_atsArcFlash() {
        checkArcFlash("TC_AF_101", "ATS",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Mains Type");
    }

    @Test
    public void TC_AF_102_batteryArcFlash() {
        checkArcFlash("TC_AF_102", "Battery",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_103_busductArcFlash() {
        checkArcFlash("TC_AF_103", "Busduct",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_104_buswayArcFlash() {
        checkArcFlash("TC_AF_104", "Busway",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_105_cableArcFlash() {
        checkArcFlash("TC_AF_105", "Cable",
                "Voltage", "Conductor Material");
    }

    @Test
    public void TC_AF_106_capacitorArcFlash() {
        checkArcFlash("TC_AF_106", "Capacitor",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_107_capacitorBankArcFlash() {
        checkArcFlash("TC_AF_107", "Capacitor Bank",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_108_circuitBreakerArcFlash() {
        checkArcFlash("TC_AF_108", "Circuit Breaker",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Pole Count");
    }

    @Test
    public void TC_AF_109_disconnectSwitchArcFlash() {
        checkArcFlash("TC_AF_109", "Disconnect Switch",
                "Voltage", "Ampere Rating", "Interrupting Rating");
    }

    @Test
    public void TC_AF_110_fuseArcFlash() {
        checkArcFlash("TC_AF_110", "Fuse",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Fuse Count");
    }

    @Test
    public void TC_AF_111_generatorArcFlash() {
        checkArcFlash("TC_AF_111", "Generator",
                "Voltage", "K W");
    }

    @Test
    public void TC_AF_112_junctionBoxArcFlash() {
        checkArcFlash("TC_AF_112", "Junction Box",
                "Voltage");
    }

    @Test
    public void TC_AF_113_lightingControlsArcFlash() {
        checkArcFlash("TC_AF_113", "Lighting Controls",
                "Voltage");
    }

    @Test
    public void TC_AF_114_loadArcFlash() {
        checkArcFlash("TC_AF_114", "Load",
                "Voltage");
    }

    @Test
    public void TC_AF_115_loadcenterArcFlash() {
        checkArcFlash("TC_AF_115", "Loadcenter",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Mains Type");
    }

    @Test
    public void TC_AF_116_mccArcFlash() {
        checkArcFlash("TC_AF_116", "MCC",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_117_mccBucketArcFlash() {
        checkArcFlash("TC_AF_117", "MCC Bucket",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_118_meterArcFlash() {
        checkArcFlash("TC_AF_118", "Meter",
                "Voltage");
    }

    @Test
    public void TC_AF_119_motorArcFlash() {
        checkArcFlash("TC_AF_119", "Motor",
                "Voltage", "R P M", "Horsepower");
    }

    @Test
    public void TC_AF_120_motorControllerArcFlash() {
        checkArcFlash("TC_AF_120", "Motor Controller",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_121_motorStarterArcFlash() {
        checkArcFlash("TC_AF_121", "Motor Starter",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_122_otherArcFlash() {
        checkArcFlash("TC_AF_122", "Other",
                "Voltage");
    }

    @Test
    public void TC_AF_123_otherOcpArcFlash() {
        checkArcFlash("TC_AF_123", "Other (OCP)",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_124_panelboardArcFlash() {
        checkArcFlash("TC_AF_124", "Panelboard",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Mains Type");
    }

    @Test
    public void TC_AF_125_pduArcFlash() {
        checkArcFlash("TC_AF_125", "PDU",
                "Voltage", "K V A");
    }

    @Test
    public void TC_AF_126_rectifierArcFlash() {
        checkArcFlash("TC_AF_126", "Rectifier",
                "Voltage", "K V A");
    }

    @Test
    public void TC_AF_127_relayArcFlash() {
        checkArcFlash("TC_AF_127", "Relay",
                "Voltage", "Manufacturer");
    }

    @Test
    public void TC_AF_128_seriesReactorArcFlash() {
        checkArcFlash("TC_AF_128", "Series Reactor",
                "Voltage");
    }

    @Test
    public void TC_AF_129_shuntReactorArcFlash() {
        checkArcFlash("TC_AF_129", "Shunt Reactor",
                "Voltage");
    }

    @Test
    public void TC_AF_130_switchArcFlash() {
        checkArcFlash("TC_AF_130", "Switch",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_131_switchboardArcFlash() {
        checkArcFlash("TC_AF_131", "Switchboard",
                "Voltage", "Ampere Rating", "Interrupting Rating");
    }

    @Test
    public void TC_AF_132_tieBreakerArcFlash() {
        checkArcFlash("TC_AF_132", "Tie Breaker",
                "Voltage", "Ampere Rating", "Interrupting Rating");
    }

    @Test
    public void TC_AF_133_transformerArcFlash() {
        checkArcFlash("TC_AF_133", "Transformer",
                "Voltage", "Primary Voltage", "Secondary Voltage", "K V A");
    }

    @Test
    public void TC_AF_134_transformer3WindingArcFlash() {
        checkArcFlash("TC_AF_134", "Transformer (3-Winding)",
                "Voltage", "Primary Voltage", "Secondary Voltage", "K V A");
    }

    @Test
    public void TC_AF_135_upsArcFlash() {
        checkArcFlash("TC_AF_135", "UPS",
                "Voltage", "Ampere Rating", "K V A");
    }

    @Test
    public void TC_AF_136_utilityArcFlash() {
        checkArcFlash("TC_AF_136", "Utility",
                "Voltage");
    }

    @Test
    public void TC_AF_137_vfdArcFlash() {
        checkArcFlash("TC_AF_137", "VFD",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_138_vfdPanelArcFlash() {
        checkArcFlash("TC_AF_138", "VFD Panel",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_139_nodeBusArcFlash() {
        // Live-only system class (not in the gold picker) — bus nodes carry
        // AF-relevant electrical data too (System Voltage + Mains Type seen
        // live 2026-07-20), and the fixture site has them.
        checkArcFlash("TC_AF_139", "Node Bus",
                "Voltage", "Mains Type");
    }

    // ── Shared per-class check ───────────────────────────────────────────

    // v1.50 renders the universal AF field as "System Voltage" (Engineering
    // section, seen live on Node Bus 2026-07-20); "Voltage" kept for older
    // builds. Merged ahead of every class's own label list.
    private static final String[] UNIVERSAL_AF_LABELS = {"System Voltage", "Voltage"};

    private void checkArcFlash(String caseId, String className, String... classLabels) {
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
        for (String l : UNIVERSAL_AF_LABELS) merged.add(l);
        for (String l : classLabels) merged.add(l);
        String[] labels = merged.toArray(new String[0]);
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, "Arc Flash by Asset",
                caseId + " - " + className + " Arc Flash — AF data renders on a "
                + className + " asset's details");

        logStep("Step 1: Login and open the Assets list");
        loginAndSelectSite();
        assetPageLocal.navigateToAssetList();
        mediumWait();

        logStep("Step 2: Find an asset whose CLASS is '" + className + "' (never matched by name)");
        final String found = assetPageLocal.findAssetOfClass(className);
        skipIfPreconditionMissing(() -> found != null,
                "no asset of class '" + className + "' on this site — add a fixture asset to cover this class");
        logStep("Asset under test: '" + found + "' (cell declares class '" + className + "')");

        logStep("Step 3: Open the asset's details");
        // findAssetOfClass returns with the asset ON SCREEN; the retry must
        // reproduce that discovery state (NOT re-search className — assets
        // found via the unfiltered rescan disappear under that filter).
        boolean opened = assetPageLocal.selectAssetByName(found);
        if (!opened) {
            assetPageLocal.findAssetOfClass(className);
            opened = assetPageLocal.selectAssetByName(found);
        }
        assertTrue(opened, className + " asset '" + found + "' must open from the list");
        verifyAppAlive(className + " details open");

        logStep("Step 4: Arc-flash data surface renders for this " + className);
        // Real details-ready poll (mediumWait is a documented no-op and the
        // driver runs animationCoolOffTimeout=0): wait for the details nav
        // title before trusting a page-source snapshot, else the scan can
        // catch the push transition mid-flight and false-fail.
        com.egalvanic.utils.Waits.until(() -> {
            try {
                String s = DriverManager.getDriver().getPageSource();
                return sourceHasVisibleText(s, "Asset Details") || sourceHasVisibleText(s, className);
            } catch (Exception e) { return false; }
        }, 6000);
        // Read the whole detail surface ONCE via page source (query layer is
        // unreliable on heavy detail DOMs) and evaluate labels + values on it.
        String source = DriverManager.getDriver().getPageSource();
        List<String> presentLabels = new ArrayList<>();
        StringBuilder valueLog = new StringBuilder();
        scanLabels(source, labels, presentLabels, valueLog);

        // Informational only — the list cell already declared the class.
        boolean classConfirmed = sourceHasVisibleText(source, className);
        logStep("Class text on details: " + (classConfirmed
                ? "confirmed '" + className + "'" : "not rendered as its own element (cell-composite match)"));

        if (presentLabels.isEmpty()) {
            // One scroll — AF fields can start below the fold on long forms.
            try {
                DriverManager.getDriver().executeScript("mobile: scroll",
                        java.util.Map.of("direction", "down"));
                mediumWait();
                source = DriverManager.getDriver().getPageSource();
                scanLabels(source, labels, presentLabels, valueLog);
            } catch (Exception ignored) { }
        }
        assertTrue(!presentLabels.isEmpty(),
                className + " ('" + found + "') must expose at least one arc-flash-relevant field of "
                + java.util.Arrays.toString(labels) + " on its details screen");

        logStep("Step 5: Arc-flash values read for " + className + " ('" + found + "'): " + valueLog);
        logStepWithScreenshot(caseId + " " + className + " Arc Flash: " + presentLabels.size()
                + " AF field(s) verified — " + valueLog);
    }

    /** Collect present labels and their values from one page-source snapshot. */
    private void scanLabels(String source, String[] labels,
                            List<String> presentLabels, StringBuilder valueLog) {
        for (String label : labels) {
            if (presentLabels.contains(label)) continue;
            if (!sourceHasVisibleText(source, label)) continue;
            presentLabels.add(label);
            String value = valueBelowLabel(source, label);
            valueLog.append(label).append(" = '").append(value == null ? "—" : value).append("'; ");
        }
    }

    // ── Class-based asset discovery lives in AssetPage (shared by all
    //    modules since changelog 135): findAssetOfClass / firstVisibleAssetOfClass
    //    / clearSearchFilterVerified / scrollAssetListDown. ──

    // ── Page-source helpers (no element queries on the heavy detail DOM) ──

    /** Visible element with this exact name/label anywhere on the source. */
    private boolean sourceHasVisibleText(String source, String text) {
        String esc = escape(text);
        return Pattern.compile("<XCUIElementType\\w+[^>]*name=\"" + Pattern.quote(esc)
                + "\"[^>]*visible=\"true\"").matcher(source).find()
            || Pattern.compile("<XCUIElementType\\w+[^>]*visible=\"true\"[^>]*name=\""
                + Pattern.quote(esc) + "\"").matcher(source).find();
    }

    /**
     * The value rendered near a field label: the first visible StaticText
     * within 70px BELOW the label (v1.50 details rows are label + value below)
     * or, failing that, the trailing part of a "label, value" composite.
     */
    private String valueBelowLabel(String source, String label) {
        try {
            Integer labelY = null;
            List<int[]> texts = new ArrayList<>();
            List<String> names = new ArrayList<>();
            Matcher m = Pattern.compile("<XCUIElementTypeStaticText([^>]*?)/?>").matcher(source);
            while (m.find()) {
                String attrs = m.group(1);
                if (!attrs.contains("visible=\"true\"")) continue;
                Matcher nm = Pattern.compile("name=\"([^\"]*)\"").matcher(attrs);
                Matcher ym = Pattern.compile(" y=\"(-?\\d+)\"").matcher(attrs);
                if (!nm.find() || !ym.find()) continue;
                String n = unescape(nm.group(1));
                int y = Integer.parseInt(ym.group(1));
                if (n.equals(label) && labelY == null) { labelY = y; continue; }
                texts.add(new int[]{y});
                names.add(n);
            }
            if (labelY != null) {
                for (int i = 0; i < names.size(); i++) {
                    int y = texts.get(i)[0];
                    String v = names.get(i);
                    if (y <= labelY || y >= labelY + 70) continue;
                    if (v.isEmpty() || v.equals(label)) continue;
                    return v;
                }
            }
            // Composite "Label, Value" Buttons (v1.50 select rows)
            Matcher cm = Pattern.compile("name=\"" + Pattern.quote(escape(label))
                    + ", ([^\"]+)\"").matcher(source);
            if (cm.find()) return unescape(cm.group(1));
        } catch (Exception ignored) { }
        return null;
    }

    private static String unescape(String s) {
        return AssetPage.unescapePageSource(s);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
