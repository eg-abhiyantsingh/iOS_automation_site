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
 * ARC FLASH BY ASSET — 100% per-asset-class coverage (TC_AF_101-138).
 *
 * The feature-driven AF suite (dashboard buckets, punchlist toggles,
 * invariants) never says WHICH ASSET had its arc-flash data checked. This
 * class flips the axis: ONE simply-named test per asset class ("Fuse Arc
 * Flash", "ATS Arc Flash", …) covering ALL 38 classes of the node-classes
 * gold spec, so the report reads "Fuse Arc Flash: PASS/FAIL" and logs the
 * values actually read for that asset.
 *
 * Contract per class:
 *   1. An asset of the class exists on the fixture site (found by class-token
 *      search with sibling-class exclusion — "Motor Starter 1" can never
 *      satisfy the Motor test). If the site has no such asset → honest SKIP
 *      naming the missing fixture.
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
        System.out.println("\n📋 Arc Flash BY ASSET matrix — Starting (38 classes)");
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

    // ── All 38 asset classes (node-classes gold / live picker) ──────────
    // Used for sibling exclusion: a candidate name that contains a LONGER
    // class name embedding the search token belongs to that other class.
    private static final String[] ALL_CLASSES = {
        "ATS", "Battery", "Busduct", "Busway", "Cable", "Capacitor",
        "Capacitor Bank", "Circuit Breaker", "Disconnect Switch", "Fuse",
        "Generator", "Junction Box", "Lighting Controls", "Load",
        "Loadcenter", "MCC", "MCC Bucket", "Meter", "Motor",
        "Motor Controller", "Motor Starter", "Other", "Other (OCP)",
        "Panelboard", "PDU", "Rectifier", "Relay", "Series Reactor",
        "Shunt Reactor", "Switch", "Switchboard", "Tie Breaker",
        "Transformer", "Transformer (3-Winding)", "UPS", "Utility",
        "VFD", "VFD Panel",
    };

    // ── One simply-named test per class ─────────────────────────────────

    @Test
    public void TC_AF_101_atsArcFlash() {
        checkArcFlash("TC_AF_101", "ATS", "ATS",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Mains Type");
    }

    @Test
    public void TC_AF_102_batteryArcFlash() {
        checkArcFlash("TC_AF_102", "Battery", "Battery",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_103_busductArcFlash() {
        checkArcFlash("TC_AF_103", "Busduct", "Busduct",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_104_buswayArcFlash() {
        checkArcFlash("TC_AF_104", "Busway", "Busway",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_105_cableArcFlash() {
        checkArcFlash("TC_AF_105", "Cable", "Cable",
                "Voltage", "Conductor Material");
    }

    @Test
    public void TC_AF_106_capacitorArcFlash() {
        checkArcFlash("TC_AF_106", "Capacitor", "Capacitor",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_107_capacitorBankArcFlash() {
        checkArcFlash("TC_AF_107", "Capacitor Bank", "Capacitor Bank",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_108_circuitBreakerArcFlash() {
        checkArcFlash("TC_AF_108", "Circuit Breaker", "Breaker",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Pole Count");
    }

    @Test
    public void TC_AF_109_disconnectSwitchArcFlash() {
        checkArcFlash("TC_AF_109", "Disconnect Switch", "Disconnect",
                "Voltage", "Ampere Rating", "Interrupting Rating");
    }

    @Test
    public void TC_AF_110_fuseArcFlash() {
        checkArcFlash("TC_AF_110", "Fuse", "Fuse",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Fuse Count");
    }

    @Test
    public void TC_AF_111_generatorArcFlash() {
        checkArcFlash("TC_AF_111", "Generator", "Generator",
                "Voltage", "K W");
    }

    @Test
    public void TC_AF_112_junctionBoxArcFlash() {
        checkArcFlash("TC_AF_112", "Junction Box", "Junction",
                "Voltage");
    }

    @Test
    public void TC_AF_113_lightingControlsArcFlash() {
        checkArcFlash("TC_AF_113", "Lighting Controls", "Lighting",
                "Voltage");
    }

    @Test
    public void TC_AF_114_loadArcFlash() {
        checkArcFlash("TC_AF_114", "Load", "Load",
                "Voltage");
    }

    @Test
    public void TC_AF_115_loadcenterArcFlash() {
        checkArcFlash("TC_AF_115", "Loadcenter", "Loadcenter",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Mains Type");
    }

    @Test
    public void TC_AF_116_mccArcFlash() {
        checkArcFlash("TC_AF_116", "MCC", "MCC",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_117_mccBucketArcFlash() {
        checkArcFlash("TC_AF_117", "MCC Bucket", "MCC Bucket",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_118_meterArcFlash() {
        checkArcFlash("TC_AF_118", "Meter", "Meter",
                "Voltage");
    }

    @Test
    public void TC_AF_119_motorArcFlash() {
        checkArcFlash("TC_AF_119", "Motor", "Motor",
                "Voltage", "R P M", "Horsepower");
    }

    @Test
    public void TC_AF_120_motorControllerArcFlash() {
        checkArcFlash("TC_AF_120", "Motor Controller", "Motor Controller",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_121_motorStarterArcFlash() {
        checkArcFlash("TC_AF_121", "Motor Starter", "Motor Starter",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_122_otherArcFlash() {
        checkArcFlash("TC_AF_122", "Other", "Other",
                "Voltage");
    }

    @Test
    public void TC_AF_123_otherOcpArcFlash() {
        checkArcFlash("TC_AF_123", "Other (OCP)", "OCP",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_124_panelboardArcFlash() {
        checkArcFlash("TC_AF_124", "Panelboard", "Panelboard",
                "Voltage", "Ampere Rating", "Interrupting Rating", "Mains Type");
    }

    @Test
    public void TC_AF_125_pduArcFlash() {
        checkArcFlash("TC_AF_125", "PDU", "PDU",
                "Voltage", "K V A");
    }

    @Test
    public void TC_AF_126_rectifierArcFlash() {
        checkArcFlash("TC_AF_126", "Rectifier", "Rectifier",
                "Voltage", "K V A");
    }

    @Test
    public void TC_AF_127_relayArcFlash() {
        checkArcFlash("TC_AF_127", "Relay", "Relay",
                "Voltage", "Manufacturer");
    }

    @Test
    public void TC_AF_128_seriesReactorArcFlash() {
        checkArcFlash("TC_AF_128", "Series Reactor", "Series Reactor",
                "Voltage");
    }

    @Test
    public void TC_AF_129_shuntReactorArcFlash() {
        checkArcFlash("TC_AF_129", "Shunt Reactor", "Shunt Reactor",
                "Voltage");
    }

    @Test
    public void TC_AF_130_switchArcFlash() {
        checkArcFlash("TC_AF_130", "Switch", "Switch",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_131_switchboardArcFlash() {
        checkArcFlash("TC_AF_131", "Switchboard", "Switchboard",
                "Voltage", "Ampere Rating", "Interrupting Rating");
    }

    @Test
    public void TC_AF_132_tieBreakerArcFlash() {
        checkArcFlash("TC_AF_132", "Tie Breaker", "Tie Breaker",
                "Voltage", "Ampere Rating", "Interrupting Rating");
    }

    @Test
    public void TC_AF_133_transformerArcFlash() {
        checkArcFlash("TC_AF_133", "Transformer", "Transformer",
                "Voltage", "Primary Voltage", "Secondary Voltage", "K V A");
    }

    @Test
    public void TC_AF_134_transformer3WindingArcFlash() {
        checkArcFlash("TC_AF_134", "Transformer (3-Winding)", "Winding",
                "Voltage", "Primary Voltage", "Secondary Voltage", "K V A");
    }

    @Test
    public void TC_AF_135_upsArcFlash() {
        checkArcFlash("TC_AF_135", "UPS", "UPS",
                "Voltage", "Ampere Rating", "K V A");
    }

    @Test
    public void TC_AF_136_utilityArcFlash() {
        checkArcFlash("TC_AF_136", "Utility", "Utility",
                "Voltage");
    }

    @Test
    public void TC_AF_137_vfdArcFlash() {
        checkArcFlash("TC_AF_137", "VFD", "VFD",
                "Voltage", "Ampere Rating");
    }

    @Test
    public void TC_AF_138_vfdPanelArcFlash() {
        checkArcFlash("TC_AF_138", "VFD Panel", "VFD Panel",
                "Voltage", "Ampere Rating");
    }

    // ── Shared per-class check ───────────────────────────────────────────

    private void checkArcFlash(String caseId, String className, String token, String... labels) {
        ExtentReportManager.createTest(AppConstants.MODULE_ARC_FLASH, "Arc Flash by Asset",
                caseId + " - " + className + " Arc Flash — AF data renders on a "
                + className + " asset's details");

        logStep("Step 1: Login and open the Assets list");
        loginAndSelectSite();
        assetPageLocal.navigateToAssetList();
        mediumWait();

        logStep("Step 2: Find a " + className + " asset (search token '" + token + "')");
        assetPageLocal.searchAsset(token);
        mediumWait();
        String assetName = firstVisibleAssetNameFromSource(className, token);
        if (assetName == null) {
            // Second chance: some fixtures are named by function not class —
            // clear search and scan the full first page for the token.
            assetPageLocal.clearSearchAndSelectFirst();
            shortWait();
            navigateBackToListIfDetailOpened();
            assetName = firstVisibleAssetNameFromSource(className, token);
        }
        final String found = assetName;
        skipIfPreconditionMissing(() -> found != null,
                "no '" + className + "' asset on this site (search token '" + token
                + "') — add a fixture asset to cover this class");
        logStep("Asset under test: '" + found + "'");

        logStep("Step 3: Open the asset's details");
        boolean opened = assetPageLocal.selectAssetByName(found);
        if (!opened) {
            assetPageLocal.searchAsset(token);
            shortWait();
            opened = assetPageLocal.selectAssetByName(found);
        }
        assertTrue(opened, className + " asset '" + found + "' must open from the list");
        mediumWait();
        verifyAppAlive(className + " details open");

        logStep("Step 4: Arc-flash data surface renders for this " + className);
        // Read the whole detail surface ONCE via page source (query layer is
        // unreliable on heavy detail DOMs) and evaluate labels + values on it.
        String source = DriverManager.getDriver().getPageSource();
        List<String> presentLabels = new ArrayList<>();
        StringBuilder valueLog = new StringBuilder();
        scanLabels(source, labels, presentLabels, valueLog);

        // The class value itself confirms we opened the right kind of asset.
        boolean classConfirmed = sourceHasVisibleText(source, className);
        logStep("Class text on details: " + (classConfirmed
                ? "confirmed '" + className + "'" : "not shown (name-match only)"));

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

    // ── Page-source helpers (no element queries on the heavy detail DOM) ──

    /**
     * First visible asset-cell name containing the token (case-insensitive),
     * excluding names that belong to a SIBLING class — i.e. names containing
     * another, longer class name that itself embeds the token ("Main Motor
     * Starter" is a Motor Starter, never a Motor; "Loadcenter A" never a Load).
     */
    private String firstVisibleAssetNameFromSource(String className, String token) {
        try {
            String src = DriverManager.getDriver().getPageSource();
            Matcher m = Pattern.compile("<XCUIElementType(?:StaticText|Button|Cell)([^>]*?)/?>").matcher(src);
            String tokenLc = token.toLowerCase();
            while (m.find()) {
                String attrs = m.group(1);
                if (!attrs.contains("visible=\"true\"")) continue;
                Matcher nm = Pattern.compile("name=\"([^\"]*)\"").matcher(attrs);
                Matcher ym = Pattern.compile(" y=\"(-?\\d+)\"").matcher(attrs);
                if (!nm.find() || !ym.find()) continue;
                String n = unescape(nm.group(1));
                int y = Integer.parseInt(ym.group(1));
                if (y < 150 || y > 860) continue;              // list content zone
                if (!n.toLowerCase().contains(tokenLc)) continue;
                if (n.length() > 80 || n.contains(" › ")) continue; // room rows etc.
                if (belongsToSiblingClass(n, className, tokenLc)) continue;
                return n;
            }
        } catch (Exception e) {
            System.out.println("   firstVisibleAssetNameFromSource: " + e.getMessage());
        }
        return null;
    }

    /** True if the candidate name contains another class's name that embeds the token. */
    private boolean belongsToSiblingClass(String candidate, String className, String tokenLc) {
        String candLc = candidate.toLowerCase();
        for (String other : ALL_CLASSES) {
            if (other.equalsIgnoreCase(className)) continue;
            String otherLc = other.toLowerCase();
            if (otherLc.contains(tokenLc) && candLc.contains(otherLc)) return true;
        }
        return false;
    }

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

    /** If a search fallback accidentally opened a detail, go back to the list. */
    private void navigateBackToListIfDetailOpened() {
        try {
            org.openqa.selenium.WebElement back = DriverManager.getDriver().findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name == 'BackButton' OR name == 'Back') AND visible == 1"));
            back.click();
            shortWait();
        } catch (Exception ignored) { }
    }

    private static String unescape(String s) {
        return s.replace("&amp;", "&").replace("&gt;", ">").replace("&lt;", "<")
                .replace("&quot;", "\"").replace("&apos;", "'");
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
