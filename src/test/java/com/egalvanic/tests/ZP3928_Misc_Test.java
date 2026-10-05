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
 * ZP-3928 (iOS 1.56) §8 IR photo cache busting · §9 volume buttons (§5 Copy Data To: ZP3928_CopyDataTo_Test).
 */
public final class ZP3928_Misc_Test extends BaseTest {

    private static final String FEATURE_IPC = "IR photo cache busting (ZP-3928)";
    private static final String FEATURE_VOL = "Volume-button capture (ZP-3928)";

    // §5 'Copy Data To' search moved to ZP3928_CopyDataTo_Test (1.69): it is the EG Forms copy sheet,
    // not the asset screen's 'Copy Details To' this class used to probe.

    // ── §8 IR cache busting ─────────────────────────────────────────────

    /**
     * modified_at was added to ir_photos for 1.56 cache busting. On the automation site (2026-10-02) every
     * IR photo created since 2026-07-28 carries it (set at creation) and the 14 null rows were all created
     * on/before 2026-07-01 — legacy rows the backend never backfilled. Rows created on/after this boundary
     * must carry it; older nulls are reported, not failed (a never-modified photo has nothing to bust).
     */
    private static final String IR_MODIFIED_AT_SINCE = "2026-07-15";

    @Test(priority = 20)
    public void TC_IPC_04_photoPayloadCarriesModifiedAt() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE_IPC,
                "TC_IPC_04 - IR / issue photo records carry modified_at (the cache-busting key)");
        // The payload of the site the device is ON (not an arbitrary first SLD).
        loginAndSelectSite();
        String site = siteSelectionPage.getCurrentSiteName();
        TestDataApi api = new TestDataApi();
        api.login();
        String sldId = site == null ? null : api.resolveSldIdByName(site);
        if (sldId == null) sldId = api.firstSldId();
        if (sldId == null || sldId.isEmpty()) throw new SkipException("No accessible SLD");
        logStep("Reading the SLD payload the device syncs for '" + site + "' (" + sldId + ")");

        // ir_photos — the records the 1.56 cache busting keys on (issue rows carry no photo fields at all;
        // the old version looked for ir_photo_key on ISSUES and could only ever skip).
        JsonPath sld = JsonPath.from(api.getSldDetails(sldId));
        List<Map<String, Object>> ir = liveRows(sld.getList("ir_photos"));
        if (ir.isEmpty()) throw new SkipException("No IR photo records on '" + site + "' — nothing to inspect");
        int keyMissing = 0, recentMissing = 0, recent = 0, legacyNull = 0, beforeCreation = 0;
        for (Map<String, Object> p : ir) {
            if (!p.containsKey("modified_at")) { keyMissing++; continue; }
            String created = String.valueOf(p.get("date_created"));
            Object mod = p.get("modified_at");
            boolean isRecent = created.compareTo(IR_MODIFIED_AT_SINCE) >= 0;
            if (isRecent) recent++;
            if (mod == null) { if (isRecent) recentMissing++; else legacyNull++; continue; }
            if (String.valueOf(mod).compareTo(created.substring(0, Math.min(19, created.length()))) < 0) beforeCreation++;
        }
        logStep("IR photos: " + ir.size() + " live · created since " + IR_MODIFIED_AT_SINCE + ": " + recent
                + " · legacy rows with null modified_at: " + legacyNull);
        assertEquals(keyMissing, 0, "Every IR photo record must expose the modified_at field (" + keyMissing
                + " of " + ir.size() + " lack the key entirely)");
        assertEquals(recentMissing, 0, recentMissing + " of " + recent + " IR photos created since "
                + IR_MODIFIED_AT_SINCE + " have a NULL modified_at — the device cannot tell a replaced photo from a cached one");
        assertEquals(beforeCreation, 0, beforeCreation + " IR photo(s) report a modified_at earlier than their creation time");

        // photos (issue / node photos) — every live record must carry it.
        List<Map<String, Object>> photos = liveRows(sld.getList("photos"));
        long photoMissing = photos.stream().filter(p -> p.get("modified_at") == null).count();
        logStep("Photos: " + photos.size() + " live · missing modified_at: " + photoMissing);
        assertEquals(photoMissing, 0L, photoMissing + " of " + photos.size() + " photo records lack modified_at");
        logStepWithScreenshot("TC_IPC_04: modified_at present where cache busting needs it");
    }

    private static List<Map<String, Object>> liveRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        if (rows == null) return out;
        for (Map<String, Object> r : rows) if (!Boolean.TRUE.equals(r.get("is_deleted"))) out.add(r);
        return out;
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
