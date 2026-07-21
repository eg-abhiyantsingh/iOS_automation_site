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
                String existing = a.findWorkOrderIdByName(wt.fixtureName());
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
        ensureFixturesOnLandedSite();
        if (fixturesCreatedThisSession && !resyncedAfterEnsure) {
            resyncedAfterEnsure = true;
            System.out.println("🔄 fixtures were just created — best-effort site re-selection for re-sync");
            try {
                siteSelectionPage.switchToSiteByIndex(0);
                siteSelectionPage.waitForDashboardFast();
            } catch (Exception e) {
                System.out.println("⚠️ resync attempt failed (non-fatal): " + e.getMessage());
            }
            // Recover to a known-good dashboard whatever the hop did.
            loginAndSelectSite();
        }
        siteSelectionPage.clickWorkOrderCard();
        shortWait();
        assertTrue(wo.waitForWorkOrdersScreen(),
                "Work Orders screen must open from the dashboard tile");
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
        assertTrue(wo.openWorkOrderByName(wt.fixtureName()),
                "Fixture row must open (verified nav): " + wt.fixtureName());
    }
}
