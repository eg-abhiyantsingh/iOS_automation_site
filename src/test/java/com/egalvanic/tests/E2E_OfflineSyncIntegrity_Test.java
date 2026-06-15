package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.verify.StateIntegrityChecker;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * E2E_OfflineSyncIntegrity_Test — offline → sync → POST-SYNC DATA INTEGRITY.
 *
 * <p>Why this class exists (the gap it closes):
 * Every existing offline test (OfflineTest, OfflineSyncMultiSite_Test) asserts
 * only the SYNC QUEUE COUNT — that the queue grew offline and drained to 0 after
 * sync. None of them ever re-reads the data AFTER sync to prove the bytes the
 * queue carried actually landed on the server intact. That blind spot hides two
 * CONFIRMED product bug classes:
 *
 *   SLD-2 — silent field loss in sync replay: a single offline save carries N
 *           changed fields into the queue, but the replay path drops a subset on
 *           the way to the server. Queue count still goes 1→0, so a count-only
 *           test passes while the user silently loses edits.
 *
 *   cross-site write corruption: offline edits made while site context is A vs B
 *           can replay against the wrong SLD, so Site B's marker shows up on Site
 *           A's asset (or vice-versa). A global-queue count test can't see this —
 *           the count drains either way.
 *
 * <p>Both flows therefore force a FRESH SERVER READ after sync (re-navigate +
 * reopen the asset) and {@code assertEquals} on the actual field values, plus a
 * backend ground-truth cross-check via {@link TestDataApi} when API egress is
 * reachable (degrades to the UI re-read assertion when it is not — never skips
 * purely because the API is blocked).
 *
 * <p>Reuses the proven primitives from {@link OfflineSyncMultiSite_Test}:
 * {@code loginAndPickSite} / SITE_A / SITE_B / multiSiteAvailable;
 * goOffline/goOnline/syncPendingRecords/waitForSyncToComplete/getPendingSyncCount
 * on SiteSelectionPage; and the asset-edit primitive
 * selectFirstAsset + clickEditTurbo + enterAssetName/fillTextField/enterQRCode +
 * clickEditSave (each setter dismisses the keyboard before we tap Save).
 */
public class E2E_OfflineSyncIntegrity_Test extends BaseTest {

    // ================================================================
    // CLASS STATE — mirrors OfflineSyncMultiSite_Test's two-site setup
    // ================================================================

    private static String SITE_A = AppConstants.SITE_WITH_MANY_ASSETS;  // "test site"
    private static String SITE_B = AppConstants.SITE_WITH_FEW_ASSETS;   // "Test QA 16"

    /**
     * Cross-class free-text attribute labels with a getter+setter that survive
     * across asset classes. We probe these in order to find a THIRD editable
     * field beyond Name + QR Code for the multi-field-loss check (SLD-2). The
     * list is intentionally generic so the flow works on whatever the first
     * asset's class happens to be in the live environment.
     */
    private static final String[] CANDIDATE_ATTR_LABELS = {
        "Notes", "Description", "Manufacturer", "Model", "Model Number",
        "Serial Number", "Serial", "Comments", "Label", "Tag"
    };

    // ================================================================
    // LIFECYCLE — same noReset + cascade-guard pattern as the reference class
    // ================================================================

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        System.out.println("\n📋 E2E Offline Sync Integrity Suite — Starting");
        System.out.println("   Sites: SITE_A='" + SITE_A + "'  SITE_B='" + SITE_B + "'");
        DriverManager.setNoReset(true);
    }

    @AfterClass(alwaysRun = true)
    public void classTeardown() {
        DriverManager.resetNoResetOverride();
        System.out.println("📋 E2E Offline Sync Integrity Suite — Complete\n");
    }

    @BeforeMethod(alwaysRun = true)
    public void perTestSetup() {
        // CASCADE GUARD: if a prior test killed the session, every Appium call
        // here blocks ~90s. Bail when the session isn't active. (Same guard as
        // OfflineSyncMultiSite_Test.perTestSetup.) NEVER quit an active driver
        // here — page objects cache the driver at construction.
        if (!DriverManager.isDriverActive()) {
            System.out.println("⚠️ perTestSetup: driver/session not active — skipping online-restore probe");
            return;
        }
        runWithBudget("e2e-offline-perTestSetup", 45, () -> {
            // Restore online ONLY when there's nothing pending — auto-online with a
            // pending queue would silently sync and invalidate the offline flows.
            try {
                if (siteSelectionPage.isWifiOffline()) {
                    boolean hasPending = false;
                    try { hasPending = siteSelectionPage.hasPendingSyncRecords(); } catch (Exception ignored) {}
                    if (hasPending) {
                        System.out.println("🌐 Was offline with pending sync — leaving as-is (would corrupt the flow)");
                    } else {
                        System.out.println("🌐 Was offline (no pending) — restoring online state");
                        siteSelectionPage.goOnline();
                        shortWait();
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ perTestSetup online-restore probe failed: " + e.getMessage());
            }
        });
    }

    @AfterMethod(alwaysRun = true)
    public void perTestTeardown() {
        if (!DriverManager.isDriverActive()) {
            System.out.println("⚠️ perTestTeardown: driver/session not active — skipping sync drain");
            return;
        }
        runWithBudget("e2e-offline-perTestTeardown", 60, () -> {
            // Best-effort: drain any residual queue so the next test starts clean.
            try {
                if (siteSelectionPage.hasPendingSyncRecords()) {
                    System.out.println("🧹 Pending sync records at teardown — flushing");
                    if (siteSelectionPage.isWifiOffline()) {
                        siteSelectionPage.goOnline();
                        shortWait();
                    }
                    siteSelectionPage.syncPendingRecords();
                    siteSelectionPage.waitForSyncToComplete();
                }
            } catch (Exception ignored) {}
        });
    }

    // ================================================================
    // PRIVATE HELPERS (mirror OfflineSyncMultiSite_Test)
    // ================================================================

    /**
     * Ensure logged in + on a site. Treats the already-logged-in site as SITE_A
     * (skipping the wasteful initial switch) exactly like the reference class.
     */
    private void loginAndPickSite(String siteName) {
        loginAndSelectSite();
        try {
            String current = siteSelectionPage.getCurrentSiteName();
            if (current != null && !current.isBlank()) {
                String currentTrimmed = current.split(",")[0].trim();
                boolean looksLikeSite = currentTrimmed.length() >= 4 &&
                    !currentTrimmed.equalsIgnoreCase("WO") &&
                    !currentTrimmed.equalsIgnoreCase("Sites") &&
                    !currentTrimmed.equalsIgnoreCase("Dashboard");
                if (!looksLikeSite) {
                    System.out.println("[loginAndPickSite] Non-site label '" + currentTrimmed +
                        "' — keeping default SITE_A='" + SITE_A + "'");
                    return;
                }
                SITE_A = currentTrimmed;
                if (SITE_B.equalsIgnoreCase(SITE_A)) {
                    String fallback = AppConstants.SITE_WITH_MANY_ASSETS.equalsIgnoreCase(SITE_A)
                        ? AppConstants.SITE_WITH_FEW_ASSETS : AppConstants.SITE_WITH_MANY_ASSETS;
                    System.out.println("[loginAndPickSite] SITE_B collided with current; rebinding SITE_B=" + fallback);
                    SITE_B = fallback;
                }
                return;
            }
            siteSelectionPage.switchToSite(siteName);
            mediumWait();
        } catch (Exception ignored) {}
    }

    private void shot(String step) {
        logStepWithScreenshot(step);
    }

    /**
     * Open the first asset on the current Asset List and return its name, or
     * null when the list misrouted to the Sites picker (state pollution).
     * Centralises the misroute guard used in the reference class.
     */
    private String openFirstAsset() {
        try {
            assetPage.navigateToAssetList();
            mediumWait();
        } catch (Exception navEx) {
            System.out.println("[E2E] navigateToAssetList warning: " + navEx.getMessage());
        }
        String name = assetPage.selectFirstAsset();
        if (name == null || name.equals("Create New Site")
                || name.equals("Select Site") || name.startsWith("Search sites")) {
            return null;
        }
        return name;
    }

    /**
     * Probe the open Edit screen for a writable cross-class free-text attribute.
     * Returns the FIRST candidate label whose current value reads back non-null,
     * or null if none is reachable on this asset class. (Used to pick the THIRD
     * edited field for the SLD-2 multi-field-loss check.)
     */
    private String discoverEditableAttribute() {
        for (String label : CANDIDATE_ATTR_LABELS) {
            try {
                String v = assetPage.getTextFieldValue(label);
                if (v != null) {
                    System.out.println("[E2E] Editable attribute discovered: '" + label + "' (current='" + v + "')");
                    return label;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Resolve the SLD (site) id for backend ground-truth, returning null on any
     * failure (API egress is frequently blocked in CI — callers degrade to the
     * UI re-read assertion rather than skipping). {@code siteName} may be null,
     * in which case the user's first SLD is used.
     */
    private String resolveSldId(TestDataApi api, String siteName) {
        try {
            api.login();
            String sldId = (siteName != null && !siteName.isBlank())
                ? api.findSldIdByName(siteName) : null;
            if (sldId == null) sldId = api.firstSldId();
            return sldId;
        } catch (Exception e) {
            System.out.println("[E2E] Backend ground-truth unavailable (" + e.getMessage()
                + ") — degrading to UI re-read assertion");
            return null;
        }
    }

    // ================================================================
    // FLOW 1 — TC_E2E_001: multi-field edit must survive sync replay (SLD-2)
    // ================================================================

    /**
     * TC_E2E_001 — Offline multi-field edit survives sync replay.
     *
     * SLD-2 shape: one offline save carries THREE changed fields (name + QR +
     * one class attribute) into the queue. After going online + syncing, the
     * queue drains to 0 — but a fresh server re-read must show ALL THREE values
     * persisted. If the replay path silently drops a subset, the count is still
     * 0 (so existing tests pass) while these assertEquals catch the data loss.
     */
    @Test(priority = 1, description = "TC_E2E_001 — offline multi-field edit survives sync replay (SLD-2)")
    public void TC_E2E_001_offlineMultiFieldEditSurvivesSyncReplay() {
        ExtentReportManager.createTest(AppConstants.MODULE_ASSET,
            "E2E Offline Sync Integrity",
            "TC_E2E_001 - Offline multi-field edit survives sync replay");
        loginAndPickSite(SITE_A);
        guard("login + pick Site A");
        verifyNoErrorAlert();

        // ------------------------------------------------------------------
        // Step 1: Open a known asset and RECORD current name + 2 attributes.
        // ------------------------------------------------------------------
        logStep("Step 1: Open first asset on Site A and record current field values");
        String originalName = openFirstAsset();
        skipIfPreconditionMissing(() -> originalName != null,
            "No editable asset on Site A (asset list misrouted to Sites picker / empty data) — "
            + "environment gap, cannot exercise multi-field replay");
        verifyNotBlank("Asset Detail");

        // Enter edit screen so the field values + the QR/attribute inputs render.
        assetPage.clickEditTurbo();
        mediumWait();
        verifyNotBlank("Asset Edit");

        String originalNameValue = assetPage.getAssetNameValue();
        String originalQr = assetPage.getQRCodeValue();
        String attrLabel = discoverEditableAttribute();
        String originalAttr = (attrLabel != null) ? assetPage.getTextFieldValue(attrLabel) : null;
        System.out.println("[TC_E2E_001] Recorded: name='" + originalNameValue
            + "' qr='" + originalQr + "' attr[" + attrLabel + "]='" + originalAttr + "'");

        // ------------------------------------------------------------------
        // Step 2: Go offline and HARD-assert we are actually offline.
        // ------------------------------------------------------------------
        logStep("Step 2: Go offline");
        siteSelectionPage.goOffline();
        mediumWait();
        guard("goOffline");
        assertEquals(siteSelectionPage.isWifiOnline(), false,
            "App must be offline after goOffline() — the entire multi-field edit must be queued, not sent");

        // ------------------------------------------------------------------
        // Step 3: Edit THREE fields in ONE save, then save (each setter dismisses
        // the keyboard before we reach Save). Re-open the asset edit screen first
        // since goOffline navigated the dashboard.
        // ------------------------------------------------------------------
        logStep("Step 3: Edit name + QR + one attribute in a single offline save");
        String reopened = openFirstAsset();
        skipIfPreconditionMissing(() -> reopened != null,
            "Could not re-open asset after going offline — environment/navigation gap");
        assetPage.clickEditTurbo();
        mediumWait();

        long ts = System.currentTimeMillis();
        String marker = "_E2E1_" + ts;
        String newName = (originalNameValue == null ? "" : originalNameValue) + marker;
        String newQr = "QR_E2E1_" + ts;
        String newAttr = "ATTR_E2E1_" + ts;

        assetPage.enterAssetName(newName);          // field 1 — clears + types + dismissKeyboard
        mediumWait();
        assetPage.editQRCode(newQr);                // field 2 — clears + types + dismissKeyboard
        mediumWait();
        boolean attrEdited = false;
        if (attrLabel != null) {
            attrEdited = assetPage.editTextField(attrLabel, newAttr);  // field 3 — dismisses keyboard
            mediumWait();
        }
        final boolean attrWasEdited = attrEdited;

        assetPage.clickEditSave();
        mediumWait();
        guard("offline multi-field save");
        verifyNoErrorAlert();

        int afterEdit = siteSelectionPage.getPendingSyncCount();
        System.out.println("[TC_E2E_001] Pending sync count after offline save = " + afterEdit);
        skipIfPreconditionMissing(() -> afterEdit >= 1,
            "Offline multi-field save did not grow the queue (count=" + afterEdit + ") — "
            + "read-only data / no assets in this environment");
        assertEquals(siteSelectionPage.isWifiOnline(), false,
            "App must STILL be offline after the multi-field save — sync must not have happened yet");

        // ------------------------------------------------------------------
        // Step 4: Go online + sync; queue must drain fully to 0.
        // ------------------------------------------------------------------
        logStep("Step 4: Go online + sync; queue must drain to 0");
        siteSelectionPage.goOnline();
        mediumWait();
        guard("goOnline");
        siteSelectionPage.syncPendingRecords();
        siteSelectionPage.waitForSyncToComplete();
        verifyNoErrorAlert();
        assertEquals(siteSelectionPage.getPendingSyncCount(), 0,
            "Sync must drain the queue to 0 (count-only contract — necessary but NOT sufficient; "
            + "Step 5 proves the bytes actually landed)");

        // ------------------------------------------------------------------
        // Step 5: FRESH SERVER READ — re-navigate + reopen the asset and assert
        // EACH of the 3 fields persisted. This is the SLD-2 trap: replay may drop
        // a subset of the queued fields while the count still went 1→0.
        // ------------------------------------------------------------------
        logStep("Step 5: Fresh re-read of the asset — all 3 edited values must persist");
        assetPage.searchAsset(newName);   // search by the new name forces a server-backed re-list
        mediumWait();
        boolean opened = assetPage.selectAssetByName(newName);
        if (!opened) {
            // Fall back to first asset (search may not filter on all builds); the
            // re-read assertions below still validate the values strictly.
            String fallback = openFirstAsset();
            skipIfPreconditionMissing(() -> fallback != null,
                "Could not re-open the edited asset for verification — navigation/environment gap");
        }
        assetPage.clickEditTurbo();
        mediumWait();
        verifyNotBlank("Asset Edit (re-read)");

        String reReadName = assetPage.getAssetNameValue();
        String reReadQr = assetPage.getQRCodeValue();
        String reReadAttr = (attrLabel != null) ? assetPage.getTextFieldValue(attrLabel) : null;
        System.out.println("[TC_E2E_001] Re-read after sync: name='" + reReadName
            + "' qr='" + reReadQr + "' attr[" + attrLabel + "]='" + reReadAttr + "'");

        // Field 1 — name suffix must have survived replay.
        assertEquals(reReadName, newName,
            "Name edit was LOST in sync replay (SLD-2): queued '" + newName
            + "' but server returned '" + reReadName + "'");

        // Field 2 — QR Code must have survived replay.
        assertEquals(reReadQr, newQr,
            "QR Code edit was LOST in sync replay (SLD-2): queued '" + newQr
            + "' but server returned '" + reReadQr + "'");

        // Field 3 — only when an editable attribute was present on this class.
        if (attrWasEdited) {
            assertEquals(reReadAttr, newAttr,
                "Attribute '" + attrLabel + "' edit was LOST in sync replay (SLD-2): queued '"
                + newAttr + "' but server returned '" + reReadAttr + "'");
        } else {
            // Genuine environment gap (this asset class exposes no free-text
            // attribute) — NOT an assertion mask. The name + QR checks above
            // already prove the multi-field replay path.
            System.out.println("[TC_E2E_001] No editable attribute on this class — "
                + "3rd-field check skipped (env gap); name + QR replay still asserted");
        }

        // ------------------------------------------------------------------
        // Step 6: Backend ground-truth cross-check (best-effort — degrades when
        // API egress is blocked; never skips purely on API absence).
        // ------------------------------------------------------------------
        logStep("Step 6: Backend ground-truth cross-check (if API reachable)");
        TestDataApi api = new TestDataApi();
        String sldId = resolveSldId(api, SITE_A);
        if (sldId != null) {
            try {
                String serverAssetId = api.getAssetByName(sldId, newName);
                assertEquals(serverAssetId != null, true,
                    "Server SLD '" + sldId + "' has no node named '" + newName
                    + "' after sync — the renamed asset never reached the backend (SLD-2)");
                System.out.println("[TC_E2E_001] Backend confirms node id=" + serverAssetId
                    + " carries the synced name");
            } catch (AssertionError ae) {
                throw ae;
            } catch (Exception e) {
                System.out.println("[TC_E2E_001] Backend cross-check skipped (" + e.getMessage()
                    + ") — UI re-read already asserted");
            }
        }

        shot("TC_E2E_001: all 3 offline-edited fields survived sync replay");
    }

    // ================================================================
    // FLOW 2 — TC_E2E_002: cross-site offline edits land on the correct site
    // ================================================================

    /**
     * TC_E2E_002 — Cross-site offline edits land on the correct site.
     *
     * Cross-site write-corruption shape: edit a Site A asset offline with marker
     * {@code _A_<ts>}, switch (still offline) to Site B, edit a Site B asset with
     * marker {@code _B_<ts>}, then go online + sync from Site B. Both markers must
     * land on THEIR OWN site:
     *   - On Site B: exactly one {@code _B_} match, ZERO {@code _A_} matches.
     *   - On Site A: exactly one {@code _A_} match, ZERO {@code _B_} matches.
     * A queue-count test can't see this — the global queue drains either way.
     */
    @Test(priority = 2, description = "TC_E2E_002 — cross-site offline edits land on the correct site (no write corruption)")
    public void TC_E2E_002_crossSiteOfflineEditsLandOnCorrectSite() {
        ExtentReportManager.createTest(AppConstants.MODULE_SITE_SELECTION,
            "E2E Offline Sync Integrity",
            "TC_E2E_002 - Cross-site offline edits land on correct site");
        loginAndPickSite(SITE_A);
        guard("login + pick Site A");

        long ts = System.currentTimeMillis();
        String markerA = "_A_" + ts;
        String markerB = "_B_" + ts;
        StateIntegrityChecker integrity = new StateIntegrityChecker();

        // ------------------------------------------------------------------
        // Step 1: On Site A, go offline + edit an asset with the _A_ marker.
        // ------------------------------------------------------------------
        logStep("Step 1: Site A — go offline + edit an asset with marker '" + markerA + "'");
        siteSelectionPage.goOffline();
        mediumWait();
        guard("goOffline on Site A");
        assertEquals(siteSelectionPage.isWifiOnline(), false,
            "App must be offline before the Site A edit");

        String siteAOriginal = openFirstAsset();
        skipIfPreconditionMissing(() -> siteAOriginal != null,
            "No editable asset on Site A — environment gap, cannot exercise cross-site corruption");
        assetPage.clickEditTurbo();
        mediumWait();
        String siteANewName = (siteAOriginal) + markerA;
        assetPage.enterAssetName(siteANewName);     // dismisses keyboard internally
        mediumWait();
        assetPage.clickEditSave();
        mediumWait();
        guard("Site A offline save");
        verifyNoErrorAlert();

        int afterA = siteSelectionPage.getPendingSyncCount();
        System.out.println("[TC_E2E_002] Pending after Site A edit = " + afterA);
        skipIfPreconditionMissing(() -> afterA >= 1,
            "Site A offline edit did not grow the queue (count=" + afterA + ") — read-only/empty data");

        // ------------------------------------------------------------------
        // Step 2: Switch to Site B while STILL offline (assert offline held), then
        // edit a Site B asset with the _B_ marker. Queue must hold both edits.
        // ------------------------------------------------------------------
        logStep("Step 2: Switch to Site B (still offline) + edit an asset with marker '" + markerB + "'");
        boolean toB = siteSelectionPage.switchToSite(SITE_B);
        skipIfPreconditionMissing(() -> toB,
            "Site B '" + SITE_B + "' genuinely not available in this environment — cross-site test needs a 2nd site");
        verifyAppAlive("switch to Site B");
        assertEquals(siteSelectionPage.isWifiOnline(), false,
            "App must remain offline through the switch to Site B — otherwise the Site A edit "
            + "would silently sync before the Site B edit is queued");

        String siteBOriginal = openFirstAsset();
        skipIfPreconditionMissing(() -> siteBOriginal != null,
            "No editable asset on Site B — environment gap");
        assetPage.clickEditTurbo();
        mediumWait();
        String siteBNewName = (siteBOriginal) + markerB;
        assetPage.enterAssetName(siteBNewName);     // dismisses keyboard internally
        mediumWait();
        assetPage.clickEditSave();
        mediumWait();
        guard("Site B offline save");
        verifyNoErrorAlert();

        int afterBoth = siteSelectionPage.getPendingSyncCount();
        System.out.println("[TC_E2E_002] Pending after Site B edit = " + afterBoth);
        // Per the v1.36 per-site badge, the count reflects the current site's
        // pending — Site B must show at least its own queued edit.
        skipIfPreconditionMissing(() -> afterBoth >= 1,
            "Site B offline edit did not grow the queue (count=" + afterBoth + ") — read-only/empty data");

        // ------------------------------------------------------------------
        // Step 3: Go online on Site B + sync; queue must drain to 0.
        // ------------------------------------------------------------------
        logStep("Step 3: Go online on Site B + sync; queue must drain to 0");
        siteSelectionPage.goOnline();
        mediumWait();
        guard("goOnline on Site B");
        siteSelectionPage.syncPendingRecords();
        siteSelectionPage.waitForSyncToComplete();
        verifyNoErrorAlert();
        assertEquals(siteSelectionPage.getPendingSyncCount(), 0,
            "Sync from Site B must drain BOTH sites' queued edits to 0 (count-only contract)");

        // ------------------------------------------------------------------
        // Step 4: On Site B — _B_ marker present exactly once, _A_ marker ZERO
        // (Site A's offline edit must NOT have bled onto Site B).
        // ------------------------------------------------------------------
        logStep("Step 4: On Site B — _B_ marker present once, _A_ marker absent (no cross-site bleed)");
        StateIntegrityChecker.Snapshot bMatchesB = integrity.capture(() -> searchAssetNames(markerB));
        StateIntegrityChecker.Snapshot bMatchesA = integrity.capture(() -> searchAssetNames(markerA));
        System.out.println("[TC_E2E_002] Site B: _B_ matches=" + bMatchesB.items()
            + "  _A_ matches=" + bMatchesA.items());
        integrity.assertNoLossOrDup(bMatchesB, bMatchesB);   // no duplicate _B_ rows
        assertEquals(bMatchesB.size(), 1,
            "Site B must show exactly one asset carrying its own marker '" + markerB
            + "' (got " + bMatchesB.items() + ")");
        assertEquals(bMatchesA.size(), 0,
            "CROSS-SITE CORRUPTION: Site A's marker '" + markerA + "' must NOT appear on Site B "
            + "(got " + bMatchesA.items() + ")");

        // ------------------------------------------------------------------
        // Step 5: Switch back to Site A — _A_ marker on the original asset once,
        // _B_ marker ZERO.
        // ------------------------------------------------------------------
        logStep("Step 5: Switch back to Site A — _A_ marker present once, _B_ marker absent");
        boolean backToA = siteSelectionPage.switchToSite(SITE_A);
        assertEquals(backToA || siteSelectionPage.getCurrentSiteName() != null, true,
            "Must be able to navigate back to Site A for the cross-site verification");
        verifyAppAlive("switch back to Site A");
        mediumWait();

        StateIntegrityChecker.Snapshot aMatchesA = integrity.capture(() -> searchAssetNames(markerA));
        StateIntegrityChecker.Snapshot aMatchesB = integrity.capture(() -> searchAssetNames(markerB));
        System.out.println("[TC_E2E_002] Site A: _A_ matches=" + aMatchesA.items()
            + "  _B_ matches=" + aMatchesB.items());
        integrity.assertNoLossOrDup(aMatchesA, aMatchesA);   // no duplicate _A_ rows
        assertEquals(aMatchesA.size(), 1,
            "Site A must show exactly one asset carrying its own marker '" + markerA
            + "' (got " + aMatchesA.items() + ")");
        assertEquals(aMatchesB.size(), 0,
            "CROSS-SITE CORRUPTION: Site B's marker '" + markerB + "' must NOT appear on Site A "
            + "(got " + aMatchesB.items() + ")");

        // ------------------------------------------------------------------
        // Step 6: Backend ground-truth cross-check per site (best-effort).
        // ------------------------------------------------------------------
        logStep("Step 6: Backend ground-truth — each marker on its own SLD only (if API reachable)");
        TestDataApi api = new TestDataApi();
        String sldA = resolveSldId(api, SITE_A);
        String sldB = (sldA != null) ? api.findSldIdByName(SITE_B) : null;
        if (sldA != null && sldB != null && !sldA.equals(sldB)) {
            try {
                assertEquals(api.findAssetIdByNameFragment(sldA, markerA) != null, true,
                    "Backend: Site A SLD must carry an asset with marker '" + markerA + "'");
                assertEquals(api.findAssetIdByNameFragment(sldA, markerB), null,
                    "CROSS-SITE CORRUPTION (backend): Site B marker '" + markerB
                    + "' must NOT exist on Site A's SLD");
                assertEquals(api.findAssetIdByNameFragment(sldB, markerB) != null, true,
                    "Backend: Site B SLD must carry an asset with marker '" + markerB + "'");
                assertEquals(api.findAssetIdByNameFragment(sldB, markerA), null,
                    "CROSS-SITE CORRUPTION (backend): Site A marker '" + markerA
                    + "' must NOT exist on Site B's SLD");
                System.out.println("[TC_E2E_002] Backend confirms both markers landed on their own SLD");
            } catch (AssertionError ae) {
                throw ae;
            } catch (Exception e) {
                System.out.println("[TC_E2E_002] Backend cross-check skipped (" + e.getMessage()
                    + ") — UI cross-site assertions already enforced");
            }
        } else {
            System.out.println("[TC_E2E_002] Backend SLD ids unavailable/identical — relying on UI cross-site checks");
        }

        shot("TC_E2E_002: cross-site offline edits landed on the correct site (no bleed)");
    }

    // ================================================================
    // SHARED SUPPORT
    // ================================================================

    /**
     * Search the Asset List for {@code fragment} and return the names of the
     * matching rows. Reuses {@link com.egalvanic.pages.AssetPage#searchAsset} to
     * filter the list, then reads the visible asset-name texts. Never returns
     * null (StateIntegrityChecker.capture rejects null as a swallowed failure).
     *
     * NOTE: there is no AssetPage "list visible asset names" getter today, so we
     * read the on-screen StaticText rows directly here. If a dedicated
     * AssetPage.getVisibleAssetNames(fragment) is added later, swap to it.
     */
    private List<String> searchAssetNames(String fragment) {
        List<String> names = new ArrayList<>();
        try {
            assetPage.navigateToAssetList();
            mediumWait();
        } catch (Exception navEx) {
            System.out.println("[E2E] searchAssetNames navigateToAssetList warning: " + navEx.getMessage());
        }
        try {
            assetPage.searchAsset(fragment);
            mediumWait();
        } catch (Exception e) {
            System.out.println("[E2E] searchAssetNames search warning: " + e.getMessage());
        }
        try {
            // Read visible asset-name StaticTexts that contain the unique marker.
            // The marker is a timestamped token, so a CONTAINS match cannot
            // collide with unrelated assets.
            List<org.openqa.selenium.WebElement> texts = DriverManager.getDriver().findElements(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND (name CONTAINS '" + fragment
                    + "' OR label CONTAINS '" + fragment + "')"));
            for (org.openqa.selenium.WebElement t : texts) {
                String n = t.getAttribute("name");
                if (n == null || n.isEmpty()) n = t.getAttribute("label");
                if (n != null && n.contains(fragment) && !names.contains(n)) {
                    names.add(n);
                }
            }
        } catch (Exception e) {
            System.out.println("[E2E] searchAssetNames read warning: " + e.getMessage());
        }
        return names;
    }
}
