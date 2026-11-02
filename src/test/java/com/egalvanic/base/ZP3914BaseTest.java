package com.egalvanic.base;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ExperiencePage;
import com.egalvanic.pages.ExperiencePage.Experience;
import com.egalvanic.pages.PhotoMarkupPage;
import com.egalvanic.pages.SessionFormsTabPage;
import com.egalvanic.pages.SiteWalkPage;
import com.egalvanic.pages.SyncNetworkPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.utils.ScreenshotUtil;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared plumbing for the ZP-3914 (iOS 1.59) module — docs/ai-features-changelog/193-*.md.
 *
 * Rides {@link WorkTypeBaseTest} (noReset within a class, QA-WT fixture navigation, lazily-authenticated
 * {@link TestDataApi}) and adds:
 *  - state normalisation BEFORE every test: the soft restart re-raises the 2FA set-up prompt, and the experience
 *    choice (UserDefaults) survives restarts — a test that died in the Account Manager experience would otherwise
 *    hand the next test a 'Site Walks · Settings' tab bar that loginAndSelectSite() does not recognise;
 *  - exact-id cleanup of site walks: every walk a test creates is found by diffing GET /site-walk/list ids around
 *    the action and deleted by id afterwards (walk rows carry no id and their order within a day is not stable,
 *    so the UI can never tell "my" walk from a same-titled one);
 *  - Account Manager entry for the Site Walk tests, with an honest SKIP when the QA user cannot enter it
 *    (needs the 'Account Manager' role + company feature 'site-walks').
 */
public abstract class ZP3914BaseTest extends WorkTypeBaseTest {

    protected static final String MODULE = "ZP-3914 (iOS 1.59)";

    protected ExperiencePage experience;
    protected SiteWalkPage walks;
    protected SessionFormsTabPage formsTab;
    protected PhotoMarkupPage photos;
    protected SyncNetworkPage sync;

    /** Walk ids created by this class — deleted by id in teardown. */
    private final Set<String> createdWalkIds = new LinkedHashSet<>();

    @BeforeMethod(alwaysRun = true)
    public void zp3914InitPages() {
        if (!DriverManager.isDriverActive()) return;
        experience = new ExperiencePage();
        walks = new SiteWalkPage();
        formsTab = new SessionFormsTabPage();
        photos = new PhotoMarkupPage();
        sync = new SyncNetworkPage();
        normalizeToFieldTechnician();
    }

    /** Set once the failure screen has been captured for the current test (subclass cleanups run first). */
    private boolean failureStateCaptured;

    @BeforeMethod(alwaysRun = true)
    public void zp3914ResetFailureCapture() {
        failureStateCaptured = false;
    }

    /**
     * Save the screen AS IT FAILED, before any cleanup moves the app. TestNG runs subclass and ZP-3914 @AfterMethods
     * BEFORE BaseTest.testTeardown, so its '_FAILED' screenshot used to show the restored Field Technician dashboard
     * instead of the failing wizard (first TC_SWW run, 2026-10-21). The file keeps the '<method>_FAILED_' prefix the
     * customer defect report collects. Idempotent per test — call it first in any subclass @AfterMethod.
     */
    protected void captureFailureState(ITestResult result) {
        if (failureStateCaptured || result == null || result.getStatus() != ITestResult.FAILURE) return;
        failureStateCaptured = true;
        if (!DriverManager.isDriverActive()) return;
        try {
            String path = ScreenshotUtil.captureScreenshot(result.getMethod().getMethodName() + "_FAILED_atFailure");
            System.out.println("📸 ZP-3914 failure state (before cleanup): " + path);
            logStepWithScreenshot("Screen at the moment of failure (before ZP-3914 cleanup)");
        } catch (Throwable t) {
            System.out.println("⚠️ ZP-3914 failure capture: " + t.getMessage());
        }
    }

    /** Never leave a class in Account Manager / offline: the next class expects the Field Technician app. */
    @AfterMethod(alwaysRun = true)
    public void zp3914RestoreFieldTechnician(ITestResult result) {
        captureFailureState(result);
        if (!DriverManager.isDriverActive() || experience == null) return;
        try {
            if (walks != null && experience.isOn(Experience.ACCOUNT_MANAGER) && walks.isOffline()) walks.goOnline();
        } catch (Exception e) {
            System.out.println("⚠️ ZP-3914 restore (network): " + e.getMessage());
        }
        normalizeToFieldTechnician();
    }

    /**
     * The 2FA set-up prompt re-appears a few seconds AFTER a relaunch (it raced a single probe in TC_EXP_04,
     * 2026-10-21) and a stored Account Manager choice survives relaunch: poll briefly, dismissing the prompt and
     * switching back to Field Technician when needed. Returns at once on a Field Technician tab bar or a signed-out
     * screen (no tab bar after the prompt window).
     */
    protected void normalizeToFieldTechnician() {
        try {
            for (int i = 0; i < 6; i++) {
                experience.dismissMfaSetupPromptIfPresent();
                if (experience.isOn(Experience.ACCOUNT_MANAGER)) {
                    System.out.println("🧭 Account Manager experience is on — switching back to Field Technician");
                    experience.switchTo(Experience.FIELD_TECHNICIAN);
                    return;
                }
                if (experience.isOn(Experience.FIELD_TECHNICIAN)) return;
                sleep(700);
            }
        } catch (Exception e) {
            System.out.println("⚠️ ZP-3914 state normalisation: " + e.getMessage());
        }
    }

    /** After a relaunch: wait for {@code e}'s tab bar, dismissing the late 2FA prompt on the way. */
    protected boolean waitForExperienceAfterRelaunch(Experience e, int seconds) {
        return eventually(() -> {
            experience.dismissMfaSetupPromptIfPresent();
            return experience.isOn(e);
        }, seconds);
    }

    @AfterClass(alwaysRun = true)
    public void zp3914DeleteCreatedWalks() {
        if (createdWalkIds.isEmpty()) return;
        TestDataApi a = api();
        if (a == null) {
            System.out.println("⚠️ ZP-3914: backend unreachable — could not delete test walks " + createdWalkIds);
            return;
        }
        for (String id : createdWalkIds) a.deleteSiteWalk(id);
        createdWalkIds.clear();
    }

    // ── site walk helpers ────────────────────────────────────────────────────────────────────────────────

    /** Current non-deleted walk ids (empty set when the backend is unreachable). */
    protected Set<String> walkIds() {
        TestDataApi a = api();
        if (a == null) return Set.of();
        return a.listSiteWalks().stream().map(w -> w.id).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Ids present now but not in {@code before}; each is remembered for teardown deletion. */
    protected Set<String> trackNewWalks(Set<String> before) {
        Set<String> now = walkIds();
        Set<String> fresh = new LinkedHashSet<>(now);
        fresh.removeAll(before);
        createdWalkIds.addAll(fresh);
        return fresh;
    }

    /** Remember an id created out of band (e.g. found after a sync). */
    protected void trackWalk(String id) {
        if (id != null) createdWalkIds.add(id);
    }

    /** Delete this test's walks right away (keeps same-titled rows from piling up across tests). */
    protected void deleteTrackedWalksNow() {
        zp3914DeleteCreatedWalks();
    }

    protected TestDataApi.SiteWalkRow walkRow(String id) {
        TestDataApi a = api();
        if (a == null || id == null) return null;
        return a.listSiteWalks().stream().filter(w -> id.equals(w.id)).findFirst().orElse(null);
    }

    /** Login (Field Technician), then enter the Account Manager experience and land on a loaded Site Walks list. */
    /**
     * Login, then wait for a real tab bar. After a soft restart the app can sit on its launch / loading screen or
     * the late 2FA prompt for a while; loginAndSelectSite() then reports "in-app" and returns early (TC_EXP_03
     * skipped this way, 2026-10-21). SKIPs naming the real reason when no tab bar ever shows.
     */
    protected void loginAndWaitForTabs(String tcId) {
        loginAndSelectSite();
        java.util.function.BooleanSupplier tabs = () -> {
            experience.dismissMfaSetupPromptIfPresent();
            return !experience.tabBarLabels().isEmpty();
        };
        if (!eventually(tabs, 30)) {
            System.out.println("🔁 " + tcId + ": no tab bar 30 s after login — retrying loginAndSelectSite()");
            loginAndSelectSite();
        }
        boolean ok = eventually(tabs, 20);
        skipIfPreconditionMissing(() -> ok, tcId + ": the app never showed its tab bar after login (launch/loading screen)");
    }

    /**
     * True when the app can enter Account Manager (already in it, or Settings offers 'Switch Experience').
     * The row needs the user's roles, which the app fetches ONCE per launch (ContentView `.task` → ExperienceManager
     * .refresh). A failed or late fetch resolves to Field Technician only and is not retried until the next launch,
     * so the row is missing for the rest of that app session (TC_SWW_05, CI run 41453293677, right after a soft
     * restart). One relaunch re-runs the fetch; a user who really lost the role still skips after it.
     */
    protected boolean accountManagerOffered(String tcId) {
        if (experience.isOn(Experience.ACCOUNT_MANAGER)) return true;
        boolean offered = experience.isSwitchExperienceOffered();
        if (!offered && experience.lastOfferCheckReachedSettings) {
            System.out.println("🔁 " + tcId + ": no 'Switch Experience' — relaunching once so the app fetches the roles again");
            DriverManager.getDriver().terminateApp(AppConstants.APP_BUNDLE_ID);
            DriverManager.getDriver().activateApp(AppConstants.APP_BUNDLE_ID);
            waitForExperienceAfterRelaunch(Experience.FIELD_TECHNICIAN, 30);
            offered = experience.isSwitchExperienceOffered();
            System.out.println("   " + (offered ? "✓ offered after the relaunch" : "✗ still not offered after the relaunch"));
        }
        if (!offered && !experience.lastOfferCheckReachedSettings) {
            skipIfPreconditionMissing(() -> false, tcId + ": the Settings tab could not be opened (tabs "
                    + experience.tabBarLabels() + ") — environment, not the feature");
        }
        return offered;
    }

    protected void openSiteWalksOrSkip(String tcId) {
        loginAndWaitForTabs(tcId);
        boolean offered = accountManagerOffered(tcId);
        skipIfPreconditionMissing(() -> offered, tcId + ": Settings offers no 'Switch Experience' — the QA user cannot"
                + " enter the Account Manager experience (needs role 'Account Manager' + company feature 'site-walks')");
        boolean switched = experience.switchTo(Experience.ACCOUNT_MANAGER);
        skipIfPreconditionMissing(() -> switched, tcId + ": could not switch to the Account Manager experience");
        skipIfPreconditionMissing(() -> walks.waitForList(20_000), tcId + ": the Site Walks list never finished loading");
    }

    /** Poll {@code c} until true or {@code seconds} elapse. */
    protected boolean eventually(java.util.function.BooleanSupplier c, int seconds) {
        return com.egalvanic.utils.Waits.until(c, seconds * 1000L, 600);
    }

    protected void featureTest(String feature, String name) {
        ExtentReportManager.createTest(MODULE, feature, name);
    }
}
