package com.egalvanic.base;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.DriverManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Shared plumbing for the TC_WT_* work-type suite
 * (docs/worktype-gold-spec-2026-07-21.md).
 *
 * Design decisions (2026-07-21):
 *  - The suite's fixtures are the durable QA-WT00..13 work orders — one per
 *    work type plus the null-type "General". They are SELF-PROVISIONING: the
 *    first test resolves the site the app actually landed on (first-site
 *    ordering drifted once already: "(s) Wild Goose Brewery" now sorts before
 *    "Android Qa Site1") and find-or-creates any missing fixture via
 *    TestDataApi, so the suite never depends on manual seeding or on site
 *    ordering staying stable.
 *  - API access is best-effort: if the backend is unreachable the UI tests
 *    still run against whatever fixtures exist; API-dependent tests call
 *    {@link #requireApi(String)} and SKIP honestly instead of false-failing.
 */
public abstract class WorkTypeBaseTest extends BaseTest {

    protected WorkOrderPage wo;

    private static TestDataApi api;
    private static boolean apiLoginFailed = false;
    private static boolean fixturesEnsured = false;
    private static String landedSiteName;
    private static String landedSldId;

    @BeforeClass(alwaysRun = true)
    public void wtClassSetup() {
        DriverManager.setNoReset(true);
    }

    @AfterClass(alwaysRun = true)
    public void wtClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    @BeforeMethod(alwaysRun = true)
    public void wtInitPage() {
        if (!DriverManager.isDriverActive()) return;
        try {
            wo = new WorkOrderPage();
        } catch (IllegalStateException e) {
            DriverManager.initDriver();
            wo = new WorkOrderPage();
        }
    }

    // ── backend access ──────────────────────────────────────────────────────

    /** Lazily-authenticated shared TestDataApi; null if the backend is unreachable. */
    protected static synchronized TestDataApi api() {
        if (api == null && !apiLoginFailed) {
            try {
                TestDataApi candidate = new TestDataApi();
                candidate.login();
                api = candidate;
            } catch (Exception e) {
                apiLoginFailed = true;
                System.out.println("⚠️ TestDataApi login failed — API-backed checks will SKIP: " + e.getMessage());
            }
        }
        return api;
    }

    /** SKIP (never false-fail) when a test needs the backend and it's down. */
    protected TestDataApi requireApi(String tcId) {
        TestDataApi a = api();
        skipIfPreconditionMissing(() -> a != null,
                tcId + ": QA backend API unreachable — cannot verify server-side contract");
        return a;
    }

    // ── work-type picker option set (BACKEND-DERIVED, self-updating) ────────

    private static java.util.List<String> cachedPickerOptions;

    /**
     * The option list the iOS Work Type picker MUST show, derived from the
     * BACKEND service catalog: 'General' pinned first, then every service
     * display name in case-sensitive lexicographic order.
     *
     * WHY backend-derived (2026-08-05): the catalog is CUSTOMER-EXTENSIBLE.
     * Two services ('abhiyant Preventive', 'abhiyant service corrective')
     * were added in QA, the picker correctly grew to 16 options, and the
     * hard-coded "exactly 14" assertions started reporting a FALSE FAILURE
     * against a correct app (PROBE_O evidence: both extras have identical
     * option geometry and appear in GET /procedures-v2/services). Deriving the
     * expectation keeps the count/order contracts real while letting the
     * product add work types without breaking the suite.
     *
     * Falls back to the pinned {@link WorkTypeCatalog} (14) when the backend
     * is unreachable, so offline/air-gapped runs still assert something sane.
     */
    protected static synchronized java.util.List<String> expectedPickerOptions() {
        if (cachedPickerOptions != null) return cachedPickerOptions;
        java.util.List<String> names = new java.util.ArrayList<>();
        TestDataApi a = api();
        if (a != null) {
            try {
                String json = a.workTypeServicesJson();
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                while (m.find()) names.add(m.group(1));
            } catch (Exception e) {
                System.out.println("⚠️ expectedPickerOptions: service catalog read failed — "
                        + "falling back to the pinned enum: " + e.getMessage());
                names.clear();
            }
        }
        if (names.isEmpty()) {
            for (WorkTypeCatalog wt : WorkTypeCatalog.values()) {
                if (wt != WorkTypeCatalog.GENERAL) names.add(wt.displayName());
            }
        }
        java.util.Collections.sort(names, String::compareTo);
        java.util.List<String> full = new java.util.ArrayList<>();
        full.add(WorkTypeCatalog.GENERAL.displayName());
        full.addAll(names);
        cachedPickerOptions = full;
        System.out.println("📋 expected picker options (" + full.size() + ", backend-derived): " + full);
        return full;
    }

    /** Expected picker option COUNT (backend services + the UI-only General). */
    protected static int expectedPickerOptionCount() {
        return expectedPickerOptions().size();
    }

    // ── fixture provisioning on the landed site ────────────────────────────

    /**
     * Ensure the QA-WT fixture family exists on the site the app landed on.
     * Runs the expensive path once per JVM; later calls are no-ops. Returns
     * true when the family is known-present (pre-existing or just created).
     */
    protected boolean ensureFixturesOnLandedSite() {
        if (fixturesEnsured) return true;
        TestDataApi a = api();
        if (a == null) return false;
        try {
            if (landedSiteName == null) {
                landedSiteName = siteSelectionPage.getCurrentSiteName();
            }
            if (landedSiteName == null || landedSiteName.isEmpty()) {
                System.out.println("⚠️ ensureFixtures: could not read landed site name from dashboard");
                return false;
            }
            landedSldId = a.resolveSldIdByName(landedSiteName);
            if (landedSldId == null) {
                System.out.println("⚠️ ensureFixtures: no sld id for site '" + landedSiteName + "'");
                return false;
            }
            System.out.println("🌱 ensuring QA-WT fixtures on '" + landedSiteName + "' (" + landedSldId + ")");
            for (WorkTypeCatalog wt : WorkTypeCatalog.values()) {
                // Site-scoped: same-named fixtures on OTHER sites must not
                // satisfy the ensure (first-site drift, 2026-08-03).
                String existing = a.findWorkOrderIdByNameOnSld(wt.fixtureName(), landedSldId);
                if (existing == null) {
                    a.createWorkOrder(wt.fixtureName(), wt.serviceId(), landedSldId,
                            "FLUKE", "Medium", 8);
                    fixturesCreatedThisSession = true;
                }
            }
            fixturesEnsured = true;
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ ensureFixturesOnLandedSite: " + e.getMessage());
            return false;
        }
    }

    protected String landedSiteName() { return landedSiteName; }
    protected String landedSldId()    { return landedSldId; }

    // ── navigation ──────────────────────────────────────────────────────────

    private static boolean fixturesCreatedThisSession = false;
    private static boolean resyncedAfterEnsure = false;

    /**
     * Login → dashboard → ensure fixtures → Work Orders list.
     *
     * Sync contract (probe-verified 2026-07-21, gold-spec §3b/§3d): the app
     * pulls sessions ONLY inside the whole-SLD sync, which fires on the login
     * site-selection — cold relaunch and list pull-to-refresh do NOT refetch,
     * and the dashboard Sites quick-action hop is unreliable (probe run 6:
     * silent no-select). CI's per-job fresh install syncs naturally; the
     * BEST-EFFORT mid-session resync below only runs when this session
     * actually CREATED a fixture (family self-heal), and any wreckage is
     * recovered by re-running the idempotent loginAndSelectSite.
     */
    protected void openWorkOrdersScreenWT() {
        loginAndSelectSite();
        // NOTE (app-source verified 2026-08-05): the dashboard 'WO' chip is
        // the session PICKER — it shows whenever active-flagged WOs exist on
        // the site, NOT "a session is running". Session state is IN-MEMORY
        // only (AppStateManager.setActiveSession — no persistence), so a
        // fresh install can never carry a leftover session. Do NOT "end" the
        // chip here; the redirect fallback below covers the one real case
        // (a session started EARLIER IN THIS APP SESSION hijacks the tile).
        ensureFixturesOnLandedSite();
        if (fixturesCreatedThisSession && !resyncedAfterEnsure) {
            resyncedAfterEnsure = true;
            // DETERMINISTIC resync (2026-08-05, CI run 30923680769): the app
            // pulls work orders ONLY during the login/site-selection sync —
            // fixtures API-created mid-session are invisible until the next
            // full login. The old dashboard site-hop is unreliable (probe run
            // 6: silent no-select) and let 283 CI tests skip with "fixture not
            // present". Relaunch the app and log in again: guaranteed
            // whole-SLD sync that includes the just-created fixtures.
            System.out.println("🔄 fixtures were just created — app relaunch + re-login for a guaranteed WO re-sync");
            try {
                com.egalvanic.utils.DriverManager.getDriver()
                        .terminateApp(com.egalvanic.constants.AppConstants.APP_BUNDLE_ID);
                Thread.sleep(400);
            } catch (Exception e) {
                System.out.println("⚠️ terminateApp during resync: " + e.getMessage());
            }
            try {
                com.egalvanic.utils.DriverManager.getDriver()
                        .activateApp(com.egalvanic.constants.AppConstants.APP_BUNDLE_ID);
                Thread.sleep(600);
            } catch (Exception e) {
                System.out.println("⚠️ activateApp during resync: " + e.getMessage());
            }
            loginAndSelectSite();
        }
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        boolean onList = wo.waitForWorkOrdersScreen();
        if (!onList && wo.isSessionSurfacePresent()) {
            // Redirect fallback: the tile landed in the (still-)active session
            // — its nav 'Done' returns to the Work Orders list.
            System.out.println("🧹 tile landed in the active session — exiting via Done");
            try {
                org.openqa.selenium.WebElement done = com.egalvanic.utils.DriverManager.getDriver()
                        .findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeButton' AND name == 'Done' AND visible == 1 AND rect.y < 120"));
                org.openqa.selenium.Rectangle r = done.getRect();
                com.egalvanic.utils.DriverManager.getDriver().executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                mediumWait();
            } catch (Exception e) {
                System.out.println("⚠️ session-exit Done not found: " + e.getMessage());
            }
            onList = wo.waitForWorkOrdersScreen();
        }
        if (!onList) {
            // Second identical attempt IN-PLACE (2026-08-06): the tile-nav
            // timing flake hit 1× locally (PICK_019, one miss in 65) and 2×
            // on CI run 31108181084 (E2E_015/032) — a fresh dashboard + tile
            // tap clears the blip; failing twice in a row is the real signal.
            System.out.println("🔁 entry-nav retry: dashboard → Work Orders tile (attempt 2)");
            loginAndSelectSite();
            siteSelectionPage.clickWorkOrderCard();
            shortWait();
            onList = wo.waitForWorkOrdersScreen();
        }
        assertTrue(onList, "Work Orders screen must open from the dashboard tile");
    }

    /**
     * Open the fixture WO for {@code wt}. SKIPs (precondition, not failure)
     * when the row cannot be brought on screen — e.g. backend down AND fixture
     * family absent on the landed site.
     */
    protected void openFixtureOrSkip(WorkTypeCatalog wt, String tcId) {
        openWorkOrdersScreenWT();
        boolean onScreen = wo.scrollWorkOrderListTo(wt.fixtureName());
        skipIfPreconditionMissing(() -> onScreen,
                tcId + ": fixture '" + wt.fixtureName() + "' not present in the Work Orders list");
        boolean opened = wo.openWorkOrderByName(wt.fixtureName());
        if (!opened) {
            // Row-tap flake (CI 18.5: BEH_006/036/040, DET_033/204 on run
            // 31133987978 — first tap verified-nav timed out, retries green).
            // Late nav first: the 12s verify can expire just before the
            // details screen lands, and a second tap would then mis-fire.
            if (wo.isSessionDetailsScreenDisplayed()) {
                System.out.println("ℹ️ " + tcId + ": nav completed late — details screen present");
                opened = true;
            } else {
                System.out.println("🔁 " + tcId + ": row-open retry (attempt 2): " + wt.fixtureName());
                openWorkOrdersScreenWT();
                if (wo.scrollWorkOrderListTo(wt.fixtureName())) {
                    opened = wo.openWorkOrderByName(wt.fixtureName());
                }
            }
        }
        if (!opened) {
            // Third attempt behind an app soft-restart: double-flakes survive
            // re-anchoring (~3 per 200 opens on the 18.5 CI sims, run
            // 31156460536: LIST_104/065, BEH_061) but restart clears the
            // stuck nav state (same cure as the session nav hijack).
            System.out.println("🔁 " + tcId + ": row-open attempt 3 (soft-restart): " + wt.fixtureName());
            try {
                com.egalvanic.utils.DriverManager.getDriver()
                        .terminateApp(com.egalvanic.constants.AppConstants.APP_BUNDLE_ID);
                shortWait();
                com.egalvanic.utils.DriverManager.getDriver()
                        .activateApp(com.egalvanic.constants.AppConstants.APP_BUNDLE_ID);
                loginAndSelectSite();
            } catch (Exception re) {
                System.out.println("⚠️ " + tcId + ": soft-restart threw — " + re.getMessage());
            }
            openWorkOrdersScreenWT();
            if (wo.scrollWorkOrderListTo(wt.fixtureName())) {
                opened = wo.openWorkOrderByName(wt.fixtureName());
            }
        }
        assertTrue(opened, "Fixture row must open (verified nav): " + wt.fixtureName());
    }
}
