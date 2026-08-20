package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import io.restassured.path.json.JsonPath;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [iOS] Carry engineering_status through the iOS sync paths — iOS PR #482 / backend #1057.
 *
 * The backend replaces the eqp_engineering_approved boolean with a four-state
 * engineering_status (SKM Data State vocabulary: Incomplete, Estimated, Complete, Verified).
 * App v1.59 stores it as a raw String on NodeV2/SLDDTONode and round-trips it through BOTH
 * sync paths (SLDSyncService <=200 / BackgroundImporter >200). There is deliberately NO iOS
 * UI for the field — the four-state control lives in the web Equipment Designations grid.
 *
 * SELF-GATING: the round-trip test probes /sld/v3 for the field and SKIPs with a pointed
 * message while the qa backend lacks the column (backend #1057 unpromoted). The moment the
 * backend is promoted, the same CI run starts exercising the real round-trip — no test
 * change needed. Full design: docs/engineering-status-sync-test-design.md (TC_ES_*).
 */
public final class EngineeringStatusSync_Test extends BaseTest {

    private static final String FEATURE_ES = "Engineering Status Sync (iOS #482)";

    /**
     * Read node-id -> engineering_status for the first accessible SLD via the API.
     * Returns null when NO node carries the field (backend column not on this env yet).
     * Null VALUES inside the map are recorded as "(null)" so they compare stably.
     */
    private Map<String, String> readEngineeringStatusMap() {
        TestDataApi api = new TestDataApi();
        api.login();
        String sldId = api.firstSldId();
        if (sldId == null || sldId.isEmpty()) {
            throw new SkipException("No accessible SLD id from the API — cannot probe engineering_status");
        }
        JsonPath sld = JsonPath.from(api.getSldDetails(sldId));
        List<Map<String, Object>> nodes = sld.getList("nodes");
        if (nodes == null || nodes.isEmpty()) {
            throw new SkipException("SLD payload has no nodes — cannot probe engineering_status");
        }
        boolean fieldSeen = false;
        Map<String, String> map = new LinkedHashMap<>();
        for (Map<String, Object> n : nodes) {
            Object id = n.get("id");
            if (id == null) continue;
            if (n.containsKey("engineering_status")) {
                fieldSeen = true;
                Object v = n.get("engineering_status");
                map.put(id.toString(), v == null ? "(null)" : v.toString());
            }
        }
        return fieldSeen ? map : null;
    }

    /**
     * TC_ES_020 — "Confirm there is no visible change anywhere in the iOS UI" (ticket QA step).
     * v1.59 carries the field as DATA ONLY; the Asset edit screen must NOT grow an
     * Engineering Status / Data State control.
     */
    @Test(priority = 1)
    public void TC_ES_020_noEngineeringStatusUiOnAssetEditScreen() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, FEATURE_ES,
                "TC_ES_020 - No visible engineering_status UI on the Asset edit screen (data-only PR)");

        logStep("Step 1: Login and open the shared asset's edit screen");
        loginAndSelectSite();
        assetPage.navigateToAssetListTurbo();
        String assetName = assetPage.openSharedAssetForEditOrFallback(null);
        logStep("Opened asset for edit: " + assetName);
        assertTrue(assetPage.isEditAssetScreenDisplayed(),
                "Precondition: must actually be ON the Edit Asset screen before asserting absence "
                + "(an absence assert on the wrong screen would pass vacuously)");

        logStep("Step 2: Assert the four-state control did NOT appear on iOS (web-only control)");
        boolean engStatusLabel = assetPage.isFieldLabelPresent("Engineering Status");
        boolean dataStateLabel = assetPage.isFieldLabelPresent("Data State");
        logStep("'Engineering Status' label present: " + engStatusLabel
                + " | 'Data State' label present: " + dataStateLabel);
        assertFalse(engStatusLabel,
                "iOS PR #482 is data round-tripping ONLY — an 'Engineering Status' control appeared "
                + "on the Asset edit screen (unexpected UI change; ticket says no visible change)");
        assertFalse(dataStateLabel,
                "'Data State' control appeared on the Asset edit screen — ticket says no visible iOS UI change");

        logStepWithScreenshot("TC_ES_020: edit screen has no engineering_status UI (as specified)");
    }

    /** Round-trip fixture: site + asset with web-grid "Approved" set (2026-08-20). Override via -D. */
    private static final String RT_SITE  = System.getProperty("es.rt.site",  "Android Qa Site1");
    private static final String RT_ASSET = System.getProperty("es.rt.asset", "ABB Emax 2 E1.2 — QA created");

    /**
     * TC_ES_005 — the ticket's round-trip flow, executed with the engineering field that
     * EXISTS on this backend today: web grid sets Approved (eqp_engineering_approved=true,
     * done via the real Equipment Designations checkbox) → device syncs the site → device
     * EDITS that same node and saves (produces the round-trip payload) → re-sync → the
     * approval must come back to web UNCHANGED. This proves the sync machinery + payload
     * builders carry engineering fields through a device edit; TC_ES_010 repeats it for
     * the four-state engineering_status the moment backend #1057 lands. RT_SITE has 199
     * nodes → this exercises the ticket's SMALL sync path (SLDSyncService, <=200).
     */
    @Test(priority = 3)
    public void TC_ES_005_legacyApprovalSurvivesDeviceRoundTrip() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, FEATURE_ES,
                "TC_ES_005 - Web-set engineering approval survives a device edit + re-sync (legacy field, small sync path)");

        logStep("Step 1: API — locate fixture node '" + RT_ASSET + "' on site '" + RT_SITE + "'");
        TestDataApi api = new TestDataApi();
        api.login();
        String sldId = api.findSldIdByName(RT_SITE);
        if (sldId == null || sldId.isEmpty()) {
            throw new SkipException("Fixture site '" + RT_SITE + "' not found via API — cannot run the round-trip");
        }
        Boolean before = readApprovedFlag(api, sldId, RT_ASSET);
        if (before == null) {
            throw new SkipException("Fixture asset '" + RT_ASSET + "' not found on '" + RT_SITE + "'");
        }
        if (!before) {
            throw new SkipException("Fixture precondition missing: set the Approved checkbox for '"
                    + RT_ASSET + "' on the web Equipment Designations grid first (it reads false)");
        }
        logStep("Web-set approval confirmed on the sync payload: eqp_engineering_approved = true");

        logStep("Step 2: Device sync-in — select site '" + RT_SITE + "'");
        loginAndSelectSite();
        siteSelectionPage.clickSitesButton();
        assertTrue(siteSelectionPage.selectSiteByName(RT_SITE),
                "Should be able to select fixture site '" + RT_SITE + "' on the device");
        siteSelectionPage.waitForDashboardReady();

        logStep("Step 3: Device edit — open the SAME node, change Notes, save");
        assetPage.navigateToAssetListTurbo();
        assetPage.searchAsset("ABB Emax");
        boolean opened = assetPage.selectAssetByName(RT_ASSET);
        if (!opened) opened = assetPage.selectAssetByName("ABB Emax 2 E1.2");
        assertTrue(opened, "Fixture asset '" + RT_ASSET + "' should open from search results");
        mediumWait();
        boolean edited = assetPage.editTextField("Notes", "QA-ES legacy round-trip probe");
        if (!edited) {
            throw new SkipException("Could not edit Notes on the fixture asset — no round-trip payload produced");
        }
        assetPage.clickSaveChanges();
        mediumWait();
        verifyAppAlive("after saving the fixture asset edit");

        logStep("Step 4: Re-sync — back to Dashboard, then site re-selection pushes/pulls the round-trip payload");
        // The Sites button lives on the DASHBOARD — close the Asset details sheet first
        // (first local run failed here: clickSitesButton from the Asset screen tapped a
        // lookalike and the site picker never opened).
        assetPage.clickCloseButton();
        mediumWait();
        settingsPage.openSiteTab();
        mediumWait();
        siteSelectionPage.clickSitesButton();
        siteSelectionPage.selectSiteByName(RT_SITE);
        siteSelectionPage.waitForDashboardReady();

        logStep("Step 5: API re-read (3 polls / 30s — backend applies mutations asynchronously): "
                + "approval must remain true");
        for (int poll = 1; poll <= 3; poll++) {
            try { Thread.sleep(10_000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            api.invalidateSldCache(sldId);
            Boolean now = readApprovedFlag(api, sldId, RT_ASSET);
            logStep("  poll " + poll + "/3: eqp_engineering_approved = " + now);
            assertTrue(Boolean.TRUE.equals(now),
                    "Web-set engineering approval was LOST/CHANGED by the device round-trip "
                    + "(poll " + poll + " reads " + now + ") — this is the exact data-loss class "
                    + "iOS #482 exists to prevent");
        }
        logStepWithScreenshot("TC_ES_005: approval survived the device edit + re-sync (small sync path, "
                + RT_SITE + ")");
    }

    /** eqp_engineering_approved for the named node, or null when the node isn't found. */
    private Boolean readApprovedFlag(TestDataApi api, String sldId, String assetName) {
        JsonPath sld = JsonPath.from(api.getSldDetails(sldId));
        List<Map<String, Object>> nodes = sld.getList("nodes");
        if (nodes == null) return null;
        for (Map<String, Object> n : nodes) {
            String nm = String.valueOf(n.get("name"));
            String lb = String.valueOf(n.get("label"));
            if (assetName.equals(nm) || assetName.equals(lb)) {
                Object v = n.get("eqp_engineering_approved");
                return v == null ? Boolean.FALSE : Boolean.valueOf(String.valueOf(v));
            }
        }
        return null;
    }

    /**
     * TC_ES_010 — the core regression the PR fixes: a device round-trip must not lose or
     * mutate engineering_status. Snapshot every node's value via API, perform a real device
     * edit-and-save (dirty round-trip payload) plus a full re-sync, then verify the API map
     * is unchanged. GATED: SKIPs while the qa backend lacks the column.
     */
    @Test(priority = 2)
    public void TC_ES_010_deviceRoundTripPreservesEngineeringStatus() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET, FEATURE_ES,
                "TC_ES_010 - Device edit + re-sync round-trip preserves engineering_status (both directions)");

        logStep("Step 1: API probe — is engineering_status in the /sld/v3 payload on this env?");
        Map<String, String> before = readEngineeringStatusMap();
        if (before == null) {
            logStep("engineering_status NOT in the qa sync payload — backend #1057 unpromoted; "
                    + "round-trip is untestable on this env (iOS v1.59 side is ready). "
                    + "The api-contract canary flips the moment the backend lands.");
            throw new SkipException(
                    "engineering_status not on this backend yet (backend #1057 unpromoted) — "
                    + "TC_ES_010 blocked; see docs/engineering-status-sync-test-design.md");
        }
        logStep("Field IS live — snapshot of " + before.size() + " node values taken");

        logStep("Step 2: Device sync-in (login + site selection decodes the payload WITH the field)");
        loginAndSelectSite();

        logStep("Step 3: Real device edit → save (produces the round-trip payload the PR fixes)");
        assetPage.navigateToAssetListTurbo();
        String assetName = assetPage.openSharedAssetForEditOrFallback(null);
        logStep("Editing asset: " + assetName);
        assertTrue(assetPage.isEditAssetScreenDisplayed(), "Must be on the Edit Asset screen to edit");
        boolean edited = assetPage.editTextField("Notes", "QA-ES round-trip probe");
        if (!edited) {
            throw new SkipException("Could not edit the Notes field to dirty the form — "
                    + "round-trip payload not produced; investigate the edit helper before trusting this run");
        }
        assetPage.clickSaveChanges();
        mediumWait();
        verifyAppAlive("after saving the edited asset");

        logStep("Step 4: Force a full re-sync (site re-selection) so the device's payload lands server-side");
        loginAndSelectSite();

        logStep("Step 5: API re-read — every node's engineering_status must be UNCHANGED");
        Map<String, String> after = readEngineeringStatusMap();
        assertTrue(after != null,
                "engineering_status vanished from the payload AFTER the device round-trip — "
                + "this is exactly the data-loss the PR must prevent");
        StringBuilder diffs = new StringBuilder();
        int changed = 0;
        for (Map.Entry<String, String> e : before.entrySet()) {
            String now = after.get(e.getKey());
            if (now == null) continue; // node absent in the after-payload (deleted elsewhere) — not this test's concern
            if (!now.equals(e.getValue())) {
                changed++;
                if (changed <= 10) {
                    diffs.append("\n  node ").append(e.getKey())
                         .append(": '").append(e.getValue()).append("' -> '").append(now).append("'");
                }
            }
        }
        logStep("Nodes compared: " + before.size() + "; values changed: " + changed);
        assertTrue(changed == 0,
                "engineering_status changed for " + changed + " node(s) across a device edit+re-sync — "
                + "device round-trip must not mutate or lose the field:" + diffs);

        logStepWithScreenshot("TC_ES_010: round-trip preserved engineering_status for "
                + before.size() + " nodes");
    }
}
