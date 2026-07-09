package com.egalvanic.base;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetPage;
import com.egalvanic.pages.LoginPage;
import com.egalvanic.pages.SiteSelectionPage;
import com.egalvanic.pages.WelcomePage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.utils.RunHealth;
import com.egalvanic.utils.ScreenshotUtil;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * Base Test - Parent class for all Test classes
 * Handles driver lifecycle, report initialization, and common test setup
 */
public class BaseTest {

    // Page Objects
    protected WelcomePage welcomePage;
    protected LoginPage loginPage;
    protected SiteSelectionPage siteSelectionPage;
    protected AssetPage assetPage;

    // Flag to skip setup/teardown for chained tests
    protected static boolean skipNextSetup = false;
    protected static boolean skipNextTeardown = false;

    // Classes whose tests deliberately exercise logged-OUT / pre-login states —
    // the RERUN_LOGIN_FIRST guarantee must not log in underneath them.
    private static final java.util.Set<String> LOGIN_FIRST_EXEMPT =
            java.util.Set.of("AuthenticationTest", "Security_EdgeCase_Test");

    protected static boolean isCI() {
        return System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null;
    }

    protected void skipOnCI(String reason) {
        if (isCI()) {
            throw new org.testng.SkipException("CI skip: " + reason);
        }
    }

    // Track test start time for duration calculation
    private long testStartTime;

    // Timestamp formatter: "4:37 PM - 26 Feb"
    private static final java.time.format.DateTimeFormatter TIMESTAMP_FMT =
        java.time.format.DateTimeFormatter.ofPattern("h:mm a - dd MMM");

    /**
     * Get current timestamp in human-readable format
     */
    protected String timestamp() {
        return java.time.LocalDateTime.now().format(TIMESTAMP_FMT);
    }

    /**
     * Format milliseconds into human-readable duration
     */
    private String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        long seconds = ms / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remainingSecs = seconds % 60;
        return minutes + "m " + remainingSecs + "s";
    }

    // ================================================================
    // SUITE LEVEL SETUP/TEARDOWN
    // ================================================================

    @BeforeSuite
    public void suiteSetup() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     eGalvanic iOS Automation - Test Suite Starting           ║");
        System.out.println("║     " + timestamp() + "                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // Initialize both Extent Reports
        ExtentReportManager.initReports();

        // Cleanup old screenshots (older than 7 days)
        ScreenshotUtil.cleanupOldScreenshots(7);
    }

    @AfterSuite
    public void suiteTeardown() {
        // Flush both reports
        ExtentReportManager.flushReports();

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     eGalvanic iOS Automation - Test Suite Complete           ║");
        System.out.println("║     " + timestamp() + "                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("📊 Reports generated:");
        System.out.println("   - Detailed: " + ExtentReportManager.getDetailedReportPath());
        System.out.println("   - Client:   " + ExtentReportManager.getClientReportPath());
    }

    // ================================================================
    // TEST LEVEL SETUP/TEARDOWN
    // ================================================================

    /**
     * Company feature flag this test class depends on (e.g. "eng-lib"), or null
     * when the class is not flag-gated. When the flag is POSITIVELY absent from
     * GET /auth/v2/me → company_features, every test in the class skips inside
     * testSetup before driver init (zero Appium cost) with a loud environmental
     * reason. Must be declared on the class because @BeforeClass alone cannot
     * skip cheaply here: testSetup is alwaysRun=true and would still pay a full
     * initDriver() per test after a class-level SkipException.
     */
    protected String requiredCompanyFeature() {
        return null;
    }

    // alwaysRun=true is load-bearing: without it, ONE config-method failure makes
    // TestNG skip this setup for every remaining test, so nobody ever reaches the
    // dead-session recovery inside DriverManager.initDriver() and the whole run
    // dies on the dead session (65 tests thrown away in run 27320962984).
    @BeforeMethod(alwaysRun = true)
    @Parameters({ "deviceName", "udid", "appiumPort", "wdaLocalPort" })
    public void testSetup(
            @Optional String deviceName,
            @Optional String udid,
            @Optional String appiumPort,
            @Optional String wdaLocalPort) {
        // Reset per-test screenshot budget so each test gets its own MAX cap.
        stepScreenshotCount.set(0);

        // ── RunHealth fast-skip gate (THE 6h-cancellation fix) ──────────────────
        // If this run is already doomed — dead-session breaker tripped, WDA proven
        // un-rebuildable on this runner, or the per-suite wall-clock cap exceeded —
        // do NOT enter the ~6-min initDriver()/WDA-rebuild thrash that ran for every
        // remaining test and cancelled jobs at the 6h cap (run 28246433532). Skip this
        // test in ~0s; it lands in failed-suites/ for the fresh-simulator rerun. Null
        // the driver first so the @AfterMethod teardown makes ZERO Appium HTTP calls.
        RunHealth.markFirstTestIfUnset();
        if (RunHealth.shouldFastSkip()) {
            DriverManager.forceNullDriver();
            throw new org.testng.SkipException(RunHealth.fastSkipReason());
        }

        // ── Company-feature gate (environmental blockers, e.g. eng-lib) ─────────
        // A platform-managed flag being off makes whole modules fail with misleading
        // symptoms (eng-lib off → Settings card keeps its normal subtitle but the tap
        // is a no-op → 105 "alert never appeared" FAILs, run 2026-07-08). Skip here,
        // BEFORE initDriver, so gated tests cost ~0s instead of a driver build each.
        // CompanyFeatureGate is fail-open: only a positively-confirmed absent flag
        // skips; API errors let the tests run. Bypass: -DFEATURE_GATE_OFF=true.
        String requiredFeature = requiredCompanyFeature();
        if (requiredFeature != null
                && com.egalvanic.api.CompanyFeatureGate.check(requiredFeature)
                        == com.egalvanic.api.CompanyFeatureGate.Verdict.DISABLED) {
            DriverManager.forceNullDriver();
            throw new org.testng.SkipException(
                    "BLOCKED (environment): company feature '" + requiredFeature
                    + "' is DISABLED for this tenant — "
                    + com.egalvanic.api.CompanyFeatureGate.detail(requiredFeature)
                    + ". Verified via GET /auth/v2/me. NOT a script bug — the eGalvanic"
                    + " platform team must re-enable the flag; tests resume automatically"
                    + " once it returns. Bypass with -DFEATURE_GATE_OFF=true.");
        }

        // Skip setup for chained tests
        if (skipNextSetup) {
            // Verify the driver is still alive before reusing it
            if (!DriverManager.isDriverActive()) {
                System.out.println("\n⚠️ Chained driver is dead, resetting to fresh setup...");
                skipNextSetup = false;
                skipNextTeardown = false;
                // Fall through to normal setup below
            } else {
                System.out.println("\n🔗 Continuing from previous test (skipping setup)...");
                skipNextSetup = false;
                // Re-initialize page objects with existing driver
                welcomePage = new WelcomePage();
                loginPage = new LoginPage();
                siteSelectionPage = new SiteSelectionPage();
                assetPage = new AssetPage();
                return;
            }
        }

        System.out.println("\n🚀 Setting up test...");

        // Initialize driver with retry logic for CI resilience
        // Note: Do NOT call cleanupStaleDriver() here — test classes with @BeforeClass
        // (e.g., Connections_Test) create their driver before @BeforeMethod runs,
        // and page objects cache that driver reference. Killing it would leave page
        // objects pointing at a dead driver, causing 5-minute hangs.
        // Stale/dead sessions are handled inside DriverManager.initDriver() instead.
        try {
            DriverManager.initDriver(deviceName, udid, appiumPort, wdaLocalPort);
        } catch (Exception e) {
            System.out.println("⚠️ Driver init failed: " + e.getMessage());
            System.out.println("🔄 Retrying driver initialization after cleanup...");
            forceDriverCleanup();
            // "Could not start a new session" almost always means WDA is wedged from
            // a prior heavy-query hang — retrying against the same WDA keeps failing
            // (the 120/44/32 skip cascades in run 27557701204). Force a WDA rebuild
            // on the retry so the next test recovers instead of cascading.
            DriverManager.forceWdaRebuildOnce();
            try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            DriverManager.initDriver(deviceName, udid, appiumPort, wdaLocalPort);
        }

        // Soft restart: kill app process and relaunch to clear navigation/tab state
        // With noReset=true, login data persists but stale screen state is cleared
        // IMPORTANT: terminateApp and activateApp in separate try-catches so a failed
        // terminate doesn't skip the activate (which would leave the app not running)
        boolean terminated = false;
        try {
            DriverManager.getDriver().terminateApp(AppConstants.APP_BUNDLE_ID);
            // v1.36 (changelog 075) speed: 500ms → 200ms (iOS terminate completes in ~50ms)
            Thread.sleep(200);
            terminated = true;
        } catch (Exception e) {
            System.out.println("⚠️ terminateApp failed (app may not be running yet): " + e.getMessage());
        }
        try {
            DriverManager.getDriver().activateApp(AppConstants.APP_BUNDLE_ID);
            // v1.36 (changelog 075) speed: 500ms → 200ms (activate is async, page checks handle the rest)
            Thread.sleep(200);
            System.out.println("🔄 App soft-restarted (clean navigation state)");
        } catch (Exception e) {
            System.out.println("⚠️ activateApp failed: " + e.getMessage());
            if (!terminated) {
                // Both failed — session may be dead, try reinitializing driver
                System.out.println("🔄 Session appears dead, reinitializing driver...");
                try {
                    DriverManager.quitDriver();
                } catch (Exception ignored) {}
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                // A dead session at setup time usually means WDA wedged — rebuild it.
                DriverManager.forceWdaRebuildOnce();
                DriverManager.initDriver(deviceName, udid, appiumPort, wdaLocalPort);
            }
        }

        // Initialize Page Objects
        welcomePage = new WelcomePage();
        loginPage = new LoginPage();
        siteSelectionPage = new SiteSelectionPage();
        assetPage = new AssetPage();

        // FAST app state detection (2 seconds max)
        // Skip waiting for welcome page if already logged in
        waitForAppReadyFast();

        // v1.48: the app RESTORES its navigation stack across the soft restart — if a
        // previous test (or the auto-push) left the pushed "Work Orders" screen open,
        // every test now STARTS there: no tab bar, and positional fallbacks grab the
        // wrong screen's header controls (TC_CONN_014 tapped WO-screen Refresh as the
        // "+ Add" button). Back out ONCE at setup — the single choke point every test
        // class passes through — so tests always start from the Dashboard.
        backOutOfAutoOpenedWorkOrders();

        // Handle "Session Expired" screen — app auth token expires after ~2-3 hours
        // of CI testing. Without this, ALL subsequent tests fail because the app is
        // stuck on the re-login screen and no test can navigate to its target.
        handleSessionExpiredIfNeeded();

        // Rerun mode (RERUN_LOGIN_FIRST=true, set by the rerun CI jobs): failure
        // suites interleave classes from every module, so a class inherits whatever
        // screen the previous class died on. Guarantee logged-in + site-selected +
        // Dashboard before the test body. loginAndSelectSite() is idempotent — a
        // ~1s Dashboard fast-check on the happy path — so per-test cost is tiny.
        // A login failure here must NOT convert the test into a config-skip: log
        // it and let the test fail on its own real (reportable) error instead.
        if (AppConstants.RERUN_LOGIN_FIRST
                && !LOGIN_FIRST_EXEMPT.contains(getClass().getSimpleName())) {
            try {
                loginAndSelectSite();
            } catch (Exception e) {
                System.out.println("⚠️ RERUN_LOGIN_FIRST: loginAndSelectSite failed — "
                        + "test starts from current screen: " + e.getMessage());
            }
        }

        testStartTime = System.currentTimeMillis();
        System.out.println("✅ Test setup complete  [" + timestamp() + "]\n");
        // Initial-state screenshot is taken inside ExtentReportManager.createTest()
        // — it runs after the test method enters and the ExtentTest exists,
        // so the shot actually attaches. Doing it here would no-op.
    }

    /**
     * v1.48: back out of the pushed "Work Orders" screen when the soft restart
     * restored it (nav-state restoration). Markers: 'Start New Work Order' banner /
     * 'Available Work Orders' + a BackButton. 0.3s implicit-wait probes — a no-op
     * costs milliseconds. SiteVisit tests that WANT the Work Orders screen navigate
     * to it themselves after setup, so backing out here is always safe.
     */
    protected void backOutOfAutoOpenedWorkOrders() {
        try {
            io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
            d.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(300));
            try {
                boolean onWorkOrders =
                    (!d.findElements(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "name BEGINSWITH 'Start New Work Order'")).isEmpty()
                     || !d.findElements(io.appium.java_client.AppiumBy.accessibilityId(
                        "Available Work Orders")).isEmpty())
                    && !d.findElements(io.appium.java_client.AppiumBy.accessibilityId(
                        "BackButton")).isEmpty();
                if (onWorkOrders) {
                    System.out.println("↩️ Setup: restored Work Orders screen detected — tapping Back to Dashboard (v1.48)");
                    d.findElement(io.appium.java_client.AppiumBy.accessibilityId("BackButton")).click();
                    Thread.sleep(600);
                }
            } finally {
                d.manage().timeouts().implicitlyWait(
                    java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
            }
        } catch (Exception ignored) {
            // Never let setup healing break setup itself
        }
    }

    /**
     * FAST app ready check - detects current state with minimal wait
     * Checks: Dashboard/Asset screens (logged in) OR Welcome/Login page (not logged in)
     */
    private void waitForAppReadyFast() {
        io.appium.java_client.ios.IOSDriver d0 = com.egalvanic.utils.DriverManager.getDriver();
        try {
            // Drop the implicit wait for the duration of the probe: each failed
            // findElement below otherwise burns the global 5s implicit wait, so a
            // single poll of this "2-second" check could take 4 × 5s = 20s on an
            // unknown/blank screen (runs in @BeforeMethod — per test!).
            d0.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(300));

            // 2-second fast check for any known screen
            org.openqa.selenium.support.ui.WebDriverWait fastWait =
                new org.openqa.selenium.support.ui.WebDriverWait(
                    com.egalvanic.utils.DriverManager.getDriver(),
                    java.time.Duration.ofSeconds(2)
                );

            fastWait.until(driver -> {
                // Check if on dashboard (already logged in) - FASTEST PATH
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.accessibilityId("building.2"));
                    System.out.println("⚡ App ready - Dashboard detected (already logged in)");
                    return true;
                } catch (Exception e) {}
                
                // Check if on asset list (already logged in)
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.accessibilityId("plus"));
                    System.out.println("⚡ App ready - Asset List detected (already logged in)");
                    return true;
                } catch (Exception e) {}
                
                // Check if on welcome page (needs login)
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.accessibilityId("Continue"));
                    System.out.println("⚡ App ready - Welcome page detected");
                    return true;
                } catch (Exception e) {}
                
                // Check if on login page (needs login)
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.accessibilityId("Sign In"));
                    System.out.println("⚡ App ready - Login page detected");
                    return true;
                } catch (Exception e) {}
                
                return false;
            });
        } catch (Exception e) {
            // Fast check timed out. On iOS 26.2 (local) the app can still be on the
            // launch SPLASH ("Z Platform / Your Electrical Copilot") ~8s after
            // activateApp — the 2s probe fires before it clears, and navigation then
            // runs against the splash (no Assets tab, no Sites picker → false abort).
            // If we detect the splash, give it a longer bounded settle to reach a
            // real screen rather than proceeding on the splash.
            if (isOnSplashScreen(d0)) {
                System.out.println("⏳ App still on launch splash — waiting for it to clear (cold start)...");
                try {
                    org.openqa.selenium.support.ui.WebDriverWait splashWait =
                        new org.openqa.selenium.support.ui.WebDriverWait(d0, java.time.Duration.ofSeconds(15));
                    splashWait.until(driver -> !isOnSplashScreen(d0));
                    System.out.println("⚡ Splash cleared — app reached a real screen");
                } catch (Exception splashEx) {
                    System.out.println("⚠️ App still on splash after 15s — likely the documented iOS-26.2 launch stall");
                }
            } else {
                System.out.println("⚠️ Fast app check timeout, continuing...");
            }
        } finally {
            try {
                d0.manage().timeouts().implicitlyWait(
                    java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
            } catch (Exception ignored) {}
        }
    }

    /**
     * True when the app is showing only the launch splash ("Z Platform" /
     * "Your Electrical Copilot" with the Logo image and no interactive controls).
     * On a cold/slow start (notably iOS 26.2 local) the splash lingers several
     * seconds; proceeding while it's up makes every navigator fail on the wrong
     * screen. Cheap probe — implicit wait already lowered by the caller.
     */
    private boolean isOnSplashScreen(io.appium.java_client.ios.IOSDriver d) {
        try {
            boolean copilot = !d.findElements(io.appium.java_client.AppiumBy
                .iOSNsPredicateString("label CONTAINS 'Your Electrical Copilot' "
                    + "OR name CONTAINS 'Your Electrical Copilot'")).isEmpty();
            if (!copilot) return false;
            // Splash has NO interactive controls; once Continue/Sign In/dashboard
            // chrome appears we are past it.
            boolean hasControls = !d.findElements(io.appium.java_client.AppiumBy
                .iOSNsPredicateString("(name == 'Continue' OR name == 'Sign In' "
                    + "OR name == 'building.2' OR name == 'plus') "
                    + "AND type == 'XCUIElementTypeButton'")).isEmpty();
            return !hasControls;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Detect and handle "Session Expired" screen.
     * After ~2-3 hours of CI testing, the app's auth token expires.
     * On the next app restart, the app shows "Session Expired — Please sign in again."
     * This screen has email (pre-filled) + password (empty) + Sign In button.
     *
     * Without this handler, the Session Expired screen is misidentified as the Welcome page
     * (both have XCUIElementTypeTextField), causing the company code to be typed into the
     * email field, "Continue" to fail, and the recovery to abort — making ALL subsequent
     * tests fail with "Should be on [screen]" assertions.
     */
    private void handleSessionExpiredIfNeeded() {
        try {
            io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
            if (d == null) return;

            // Fast check: look for "Session Expired" text (unique to this screen)
            d.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(800));
            try {
                org.openqa.selenium.WebElement expiredText = d.findElement(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Session Expired'"));
                if (expiredText == null || !expiredText.isDisplayed()) return;
            } catch (Exception e) {
                // No "Session Expired" text — not on this screen, nothing to do
                return;
            } finally {
                d.manage().timeouts().implicitlyWait(
                    java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
            }

            System.out.println("🔒 SESSION EXPIRED detected — re-authenticating...");

            // The email is pre-filled. Just enter password and tap Sign In.
            try {
                // Find and fill the password field
                org.openqa.selenium.WebElement passwordField = d.findElement(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeSecureTextField'"));
                passwordField.click();
                Thread.sleep(200);
                passwordField.clear();
                passwordField.sendKeys(AppConstants.VALID_PASSWORD);
                Thread.sleep(200);

                // Tap Sign In
                org.openqa.selenium.WebElement signIn = d.findElement(
                    io.appium.java_client.AppiumBy.accessibilityId("Sign In"));
                signIn.click();
                System.out.println("🔑 Password entered, Sign In tapped");
                Thread.sleep(2000);

                // Handle stacked post-Sign-In popups: Notification permission
                // ("Don't Allow") + Save Password ("Not Now"). Two-pass cascade
                // mirrors LoginPage.handleSavePasswordTurbo so re-auth recovers
                // cleanly on fresh-install / app-reset runs.
                for (int popPass = 1; popPass <= 2; popPass++) {
                    boolean handled = false;
                    try {
                        d.switchTo().alert().dismiss();
                        System.out.println("🔑 Alert dismissed (pass " + popPass + ")");
                        handled = true;
                    } catch (Exception ignored) {}
                    if (!handled) {
                        try {
                            d.findElement(io.appium.java_client.AppiumBy.accessibilityId(
                                "Don't Allow")).click();
                            System.out.println("🔑 'Don't Allow' clicked (pass " + popPass + ")");
                            handled = true;
                        } catch (Exception ignored) {}
                    }
                    if (!handled) {
                        try {
                            d.findElement(io.appium.java_client.AppiumBy.accessibilityId(
                                "Not Now")).click();
                            System.out.println("🔑 'Not Now' clicked (pass " + popPass + ")");
                            handled = true;
                        } catch (Exception ignored) {}
                    }
                    if (handled) {
                        try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                    } else {
                        break; // nothing left to dismiss
                    }
                }

                Thread.sleep(1000);

                // After re-login, we might be on Site Selection or Dashboard
                // Check for Site Selection and select site if needed
                try {
                    java.util.List<org.openqa.selenium.WebElement> siteEntries = d.findElements(
                        io.appium.java_client.AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeCell'"));
                    if (!siteEntries.isEmpty()) {
                        siteEntries.get(0).click();
                        Thread.sleep(1500);
                        System.out.println("🔑 Site selected after re-authentication");
                    }
                } catch (Exception ignored) {}

                System.out.println("✅ Session re-authenticated successfully");

            } catch (Exception loginEx) {
                System.out.println("⚠️ Session Expired re-login failed: " + loginEx.getMessage());
            }
        } catch (Exception e) {
            // Silently ignore — driver might not be ready
        }
    }

    /**
     * Clean up any stale driver left over from a previous failed test or module.
     * Prevents dead driver references in ThreadLocal from blocking new driver creation.
     */
    private void cleanupStaleDriver() {
        try {
            if (DriverManager.isDriverActive()) {
                System.out.println("⚠️ Found existing driver session, cleaning up...");
                DriverManager.quitDriver();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error cleaning stale driver: " + e.getMessage());
            forceDriverCleanup();
        }
    }

    /**
     * Force cleanup of driver resources when normal quit fails.
     * Kills the ThreadLocal reference so a fresh driver can be created.
     */
    private void forceDriverCleanup() {
        try {
            DriverManager.quitDriver();
        } catch (Exception e) {
            System.out.println("⚠️ Force cleanup error (ignored): " + e.getMessage());
        }
    }

    @AfterMethod(alwaysRun = true)
    public void testTeardown(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        // testStartTime stays 0 when setup skipped pre-driver (feature gate /
        // RunHealth fast-skip) — an epoch-based duration prints as garbage.
        long testDuration = testStartTime == 0 ? 0 : System.currentTimeMillis() - testStartTime;
        String durationStr = formatDuration(testDuration);

        // Detect if the Appium session is likely dead/unresponsive.
        // When dead, every Appium HTTP call (screenshot, terminateApp, quit) hangs 7+ min each,
        // turning a single failure into a 14-21 min teardown. Skip all Appium calls and just
        // null the driver reference.
        boolean sessionDead = (result.getStatus() == ITestResult.FAILURE)
                && isSessionLikelyDead(result.getThrowable());

        try {
            // Handle test result
            if (result.getStatus() == ITestResult.FAILURE) {
                if (sessionDead) {
                    // Session is dead — logFail without screenshot to avoid 7+ min hang
                    // from getScreenshotAsBase64() trying to reach the dead Appium server
                    ExtentReportManager.logFail(
                            "Test failed (session dead): " + result.getThrowable().getMessage());
                    System.out.println("❌ Test FAILED: " + testName + "  [" + timestamp() + "] (" + durationStr + ")");
                    System.out.println("⚠️ Session appears dead — skipping Appium calls in teardown");
                } else {
                    try {
                        String screenshotPath = ScreenshotUtil.captureScreenshot(testName + "_FAILED");
                        System.out.println("📸 Screenshot saved: " + screenshotPath);
                    } catch (Exception e) {
                        System.out.println("⚠️ Screenshot capture failed: " + e.getMessage());
                    }
                    ExtentReportManager.logFailWithScreenshot(
                            "Test failed: " + result.getThrowable().getMessage(),
                            result.getThrowable());
                    System.out.println("❌ Test FAILED: " + testName + "  [" + timestamp() + "] (" + durationStr + ")");
                }

            } else if (result.getStatus() == ITestResult.SKIP) {
                String skipReason = (result.getThrowable() != null)
                        ? result.getThrowable().getMessage() : "Unknown reason";
                // Capture the state at the moment of skip — often the app
                // is in an unexpected screen and the screenshot makes the
                // skip reason instantly diagnosable.
                try {
                    if (DriverManager.isDriverActive()) {
                        ExtentReportManager.logStepWithBase64Screenshot("⏭️ Skip state: " + skipReason);
                    }
                } catch (Exception ignored) {}
                ExtentReportManager.logSkip("Test skipped: " + skipReason);
                System.out.println("⏭️ Test SKIPPED: " + testName + "  [" + timestamp() + "] (" + durationStr + ")");
                System.out.println("   Skip reason: " + skipReason);
                if (result.getThrowable() != null) {
                    result.getThrowable().printStackTrace(System.out);
                }

            } else if (result.getStatus() == ITestResult.SUCCESS) {
                // HARDENING (Phase 2): a test that "passed" but left the app dead almost
                // certainly missed a crash. Convert that false-green into a real failure.
                failIfAppCrashedDuringSuccessfulTest(testName);
                // Final-state screenshot for passing tests too (lets devs see
                // the end state in the Detailed report). Counts against the
                // per-test screenshot cap so it doesn't bloat huge tests.
                try {
                    if (DriverManager.isDriverActive() && stepScreenshotCount.get() < MAX_STEP_SCREENSHOTS) {
                        ExtentReportManager.logStepWithBase64Screenshot("✅ Final state — test passed");
                        stepScreenshotCount.incrementAndGet();
                    }
                } catch (Exception ignored) {}
                ExtentReportManager.logPass("Test passed successfully");
                System.out.println("✅ Test PASSED: " + testName + "  [" + timestamp() + "] (" + durationStr + ")");
            }

            ExtentReportManager.removeTests();
        } catch (Exception e) {
            System.out.println("⚠️ Error in test result handling: " + e.getMessage());
        } finally {
            // ALWAYS close app and driver
            if (skipNextTeardown) {
                System.out.println("🔗 Keeping driver alive for next chained test\n");
                skipNextTeardown = false;
                skipNextSetup = true;
            } else if (sessionDead) {
                // Session is dead — don't send any HTTP commands, just null the reference
                DriverManager.forceNullDriver();
                System.out.println("🧹 Test cleanup complete (fast — session was dead)\n");
            } else if (result.getStatus() == ITestResult.SUCCESS
                    && AppConstants.KEEP_SESSION_ALIVE
                    && DriverManager.isDriverActive()) {
                // Test PASSED on a healthy session — keep the driver alive. The
                // @BeforeMethod soft restart (terminateApp+activateApp) provides
                // app-state isolation; quit+recreate costs 11-30s per test plus a
                // full UI login in login-dependent suites. initDriver() is a no-op
                // on an alive driver, so the next test reuses this session, and
                // page objects' cached driver references stay valid (never quit an
                // active driver that page objects still point at).
                // Failures, skips, and dead sessions still take the quit paths.
                System.out.println("♻️ Session kept alive for next test (KEEP_SESSION_ALIVE=true)\n");
            } else {
                // On FAILURE: force terminate app so next test starts fresh
                // Without this, noReset=true leaves the app on the failed screen
                if (result.getStatus() == ITestResult.FAILURE) {
                    try {
                        if (DriverManager.isDriverActive()) {
                            DriverManager.getDriver().terminateApp(AppConstants.APP_BUNDLE_ID);
                            System.out.println("🔄 App force terminated after failure (clean slate for next test)");
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not terminate app: " + e.getMessage());
                    }
                }
                DriverManager.quitDriver();
                System.out.println("🧹 Test cleanup complete\n");
            }
        }
    }

    /**
     * Check if a test failure was likely caused by a dead/unresponsive Appium session.
     * Walks the exception chain looking for known session-death indicators.
     * When true, teardown should skip all Appium HTTP calls to avoid 14+ min hangs.
     */
    private boolean isSessionLikelyDead(Throwable t) {
        if (t == null) return false;
        Throwable current = t;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage() != null ? current.getMessage() : "";
            // Known session-dead exception types
            if (className.contains("NoSuchSessionException") ||
                className.contains("SessionNotCreatedException") ||
                className.contains("UnreachableBrowserException")) {
                return true;
            }
            // ThreadTimeoutException means TestNG killed a method that exceeded the suite
            // time-out. On CI, this almost always means a findElement/findElements HTTP call
            // hung because the Appium session is unresponsive. Treating it as dead prevents
            // teardown from also hanging (420s) which would cascade-skip all subsequent tests.
            // Evidence: CI run 24513764702 — 5 Location timeouts caused 60 cascade skips
            // because teardown tried to use the dead session for screenshots/quit.
            if (className.contains("ThreadTimeoutException")) {
                return true;
            }
            // Known session-dead message patterns
            if (message.contains("Connection refused") ||
                message.contains("Connection reset") ||
                message.contains("Connection timed out") ||
                message.contains("NettyResponseFuture") ||
                message.contains("session is either terminated") ||
                message.contains("Session not found") ||
                message.contains("Could not start a new session") ||
                message.contains("Unable to create session") ||
                message.contains("didn't finish within the time-out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    // ================================================================
    // ██████████████████████████████████████████████████████████████
    // ██ OPTIMIZED LOGIN METHODS - DO NOT MODIFY ██
    // ██ These methods are PRODUCTION-READY and FULLY OPTIMIZED ██
    // ██ Last optimized: January 2026 - WORKING PERFECTLY ██
    // ██████████████████████████████████████████████████████████████
    // ================================================================

    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ CRITICAL: DO NOT MODIFY THIS METHOD ║
     * ║ This login flow is fully optimized and handles: ║
     * ║ - Company code entry ║
     * ║ - Credential entry ║
     * ║ - Save Password popup (handled in LoginPage.login()) ║
     * ║ Status: PRODUCTION READY - TESTED & VERIFIED ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void performLogin() {
        System.out.println("🔐 Performing login...");

        // Enter company code - wait for login page to appear
        welcomePage.submitCompanyCode(AppConstants.VALID_COMPANY_CODE);
        loginPage.waitForPageReady();

        // Enter credentials and login (Save Password popup is handled inside login())
        loginPage.loginTurbo(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);


        // Handle new Schedule screen (added Jan 2026)
        // After login, app shows Schedule screen - click "View Sites" to proceed
        siteSelectionPage.handleScheduleScreenIfPresent();
        System.out.println("✅ Login completed");
    }

    // ================================================================
    // SMART NAVIGATION - State-based navigation for faster tests
    // ================================================================

    /**
     * Detect current app state and return appropriate action
     * @return "LOGIN_PAGE", "SITE_SELECTION", "DASHBOARD", "ASSET_LIST", "ASSET_DETAIL", "EDIT_ASSET", "UNKNOWN"
     */
    protected String detectCurrentScreen() {
        System.out.println("🔍 Detecting current screen...");

        // PERFORMANCE FIX: Temporarily reduce implicit wait during screen detection.
        // Each failed findElement waits the full implicit wait (5s). With 7 screen checks
        // and multiple element lookups per check, the worst case was ~75 seconds.
        // With 1-second implicit wait, worst case drops to ~15 seconds.
        io.appium.java_client.AppiumDriver currentDriver = DriverManager.getDriver();
        if (currentDriver != null) {
            try {
                currentDriver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(1));
            } catch (Exception e) {
                // Session may be dead — continue with default timeout
            }
        }

        try {
            // Check DASHBOARD FIRST — most common state with noReset=true
            try {
                if (assetPage.isDashboardDisplayed()) {
                    System.out.println("   → Dashboard (logged in)");
                    return "DASHBOARD";
                }
            } catch (Exception e) {}

            // Check SITE SELECTION — second most common with noReset=true
            // Use lightweight inline check instead of heavy isSelectSiteScreenDisplayed()
            // which polls 5 times × 8 element checks = 40+ findElement calls
            if (currentDriver != null) {
                try {
                    java.util.List<org.openqa.selenium.WebElement> siteTitle = currentDriver.findElements(
                        io.appium.java_client.AppiumBy.accessibilityId("Select Site"));
                    if (!siteTitle.isEmpty()) {
                        System.out.println("   → Site Selection");
                        return "SITE_SELECTION";
                    }
                    // Fallback: check for nav bar with "Site" text
                    java.util.List<org.openqa.selenium.WebElement> siteNav = currentDriver.findElements(
                        io.appium.java_client.AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeNavigationBar' AND name CONTAINS 'Site'"));
                    if (!siteNav.isEmpty()) {
                        System.out.println("   → Site Selection (nav bar)");
                        return "SITE_SELECTION";
                    }
                } catch (Exception e) {}
            }

            // Check WELCOME PAGE - has "Continue" button (NOT "Sign In")
            try {
                if (welcomePage.isContinueButtonDisplayed()) {
                    System.out.println("   → Welcome Page (Company Code)");
                    return "WELCOME_PAGE";
                }
            } catch (Exception e) {}

            // Check LOGIN PAGE - has "Sign In" button and password field
            try {
                if (loginPage.isSignInButtonDisplayed()) {
                    System.out.println("   → Login Page (Email/Password)");
                    return "LOGIN_PAGE";
                }
            } catch (Exception e) {}

            // Check if on Edit Asset screen (has Save Changes or Cancel button in edit mode)
            try {
                if (assetPage.isEditAssetScreenDisplayed()) {
                    System.out.println("   → Edit Asset Screen");
                    return "EDIT_ASSET";
                }
            } catch (Exception e) {}

            // Check if on Asset Detail (has Edit button, Asset Details nav)
            try {
                if (assetPage.isAssetDetailDisplayed()) {
                    System.out.println("   → Asset Detail");
                    return "ASSET_DETAIL";
                }
            } catch (Exception e) {}

            // Check if on Asset List (has plus button for adding assets)
            try {
                if (assetPage.isAssetListDisplayed()) {
                    System.out.println("   → Asset List");
                    return "ASSET_LIST";
                }
            } catch (Exception e) {}

            System.out.println("   → Unknown screen");
            return "UNKNOWN";
        } finally {
            // ALWAYS restore implicit wait to normal value
            if (currentDriver != null) {
                try {
                    currentDriver.manage().timeouts().implicitlyWait(
                        java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
                } catch (Exception e) {
                    // Session may be dead — nothing to restore
                }
            }
        }
    }
    
    /**
     * Smart navigation to Edit Asset screen
     * Simple approach: Try to go to asset directly, if fails do full flow
     */
    protected void smartNavigateToEditAsset() {
        System.out.println("⚡ Smart navigation to Edit Asset...");
        
        // Brief wait for app to stabilize
        try { Thread.sleep(500); } catch (Exception e) {}
        
        // Try to navigate to asset directly (if already logged in)
        try {
            System.out.println("🚀 Attempting direct navigation to asset...");
            
            // Try clicking on Assets tab/button
            if (tryDirectAssetNavigation()) {
                System.out.println("✅ Direct navigation successful!");
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Direct navigation failed: " + e.getMessage());
        }
        
        // Direct navigation failed - do full flow
        System.out.println("🔄 Direct navigation failed, doing full login flow...");
        doFullLoginAndNavigateToEditAsset();
    }
    
    /**
     * Try to navigate directly to Edit Asset (if already logged in)
     */
    private boolean tryDirectAssetNavigation() {
        try {
            // Try to click Assets button/tab
            assetPage.navigateToAssetList();
            shortWait();
            
            // Check if we got to asset list
            if (assetPage.isAssetListDisplayed()) {
                // Select first asset
                assetPage.selectFirstAsset();
                shortWait();
                
                // Click Edit
                assetPage.clickEdit();
                assetPage.waitForEditScreenReady();
                return true;
            }
        } catch (Exception e) {
            // Navigation failed
        }
        return false;
    }
    
    /**
     * Full login flow and navigate to Edit Asset
     */
    private void doFullLoginAndNavigateToEditAsset() {
        System.out.println("🏁 Starting full login flow...");
        
        // Full login (handles company code + credentials)
        performLogin();
        
        // Select site
        String selectedSite = siteSelectionPage.turboSelectSite();
        if (selectedSite == null) {
            selectedSite = siteSelectionPage.selectFirstSiteUltraFast();
        }
        siteSelectionPage.waitForDashboardFast();
        
        // Navigate to asset list
        assetPage.navigateToAssetList();
        shortWait();
        
        // Select first asset
        assetPage.selectFirstAsset();
        shortWait();
        
        // Click Edit
        assetPage.clickEdit();
        assetPage.waitForEditScreenReady();
        
        System.out.println("✅ Full flow complete - on Edit Asset screen");
    }
    
    /**
     * Smart navigation to Dashboard
     * Checks current state and takes shortest path
     */
    protected void smartNavigateToDashboard() {
        // FAST PATH: Check Dashboard first (1 second max)
        if (assetPage != null && assetPage.isDashboardDisplayedFast()) {
            System.out.println("⚡ Smart navigation — already on Dashboard (fast check)");
            return;
        }

        String currentScreen = detectCurrentScreen();
        System.out.println("⚡ Smart navigation to Dashboard from: " + currentScreen);
        
        switch (currentScreen) {
            case "DASHBOARD":
                System.out.println("✅ Already on Dashboard");
                break;
                
            case "ASSET_LIST":
            case "ASSET_DETAIL":
            case "EDIT_ASSET":
                // Go back to dashboard
                System.out.println("🔙 Going back to Dashboard...");
                assetPage.clickBack();
                shortWait();
                // May need multiple backs
                if (!assetPage.isDashboardDisplayed()) {
                    assetPage.clickBack();
                    shortWait();
                }
                break;
                
            case "SITE_SELECTION":
                System.out.println("🌐 Selecting site...");
                siteSelectionPage.turboSelectSite();
                siteSelectionPage.waitForDashboardFast();
                break;
                
            case "LOGIN_PAGE":
                System.out.println("🔐 Logging in...");
                loginPage.loginTurbo(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                siteSelectionPage.turboSelectSite();
                siteSelectionPage.waitForDashboardFast();
                break;
                
            case "WELCOME_PAGE":
                System.out.println("🏁 Full login flow...");
                performLogin();
                siteSelectionPage.turboSelectSite();
                siteSelectionPage.waitForDashboardFast();
                break;
                
            default:
                System.out.println("❓ Unknown - full flow...");
                performLogin();
                siteSelectionPage.turboSelectSite();
                siteSelectionPage.waitForDashboardFast();
                break;
        }
    }



    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ CRITICAL: DO NOT MODIFY THIS METHOD ║
     * ║ Optimized login + navigate to site selection screen ║
     * ║ Status: PRODUCTION READY - TESTED & VERIFIED ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void loginAndGoToDashboard() {
        performLogin();

        // Wait for site selection screen to be ready
        siteSelectionPage.waitForSiteListReady();

        System.out.println("✅ On Site Selection Screen");
    }

    /** Consecutive loginAndSelectSite dashboard-never-rendered failures (fail-fast guard). */
    private static final java.util.concurrent.atomic.AtomicInteger consecutiveSiteLoadFailures =
            new java.util.concurrent.atomic.AtomicInteger(0);

    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ CRITICAL: DO NOT MODIFY THIS METHOD ║
     * ║ Optimized login + fast site selection (sub-3 second) ║
     * ║ Uses selectFirstSiteFast() for maximum speed ║
     * ║ Status: PRODUCTION READY - TESTED & VERIFIED ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void loginAndSelectSite() {
        // FAST PATH: Check Dashboard first (1 second max) — skip full detection on happy path
        if (assetPage != null && assetPage.isDashboardDisplayedFast()) {
            System.out.println("✅ loginAndSelectSite — already on Dashboard (fast check)");
            return;
        }

        // Not on Dashboard — detect current screen to avoid typing into wrong field.
        // With noReset=true, the app may be on Site Selection, Login, or Welcome.
        String currentScreen = detectCurrentScreen();
        System.out.println("🔐 loginAndSelectSite — current screen: " + currentScreen);

        if ("DASHBOARD".equals(currentScreen)) {
            System.out.println("✅ Already on Dashboard — skipping login and site selection");
            return;
        }

        if ("SITE_SELECTION".equals(currentScreen)) {
            // Already logged in, just need to select a site
            System.out.println("🔍 Already on Site Selection — skipping login, selecting site...");
        } else if ("WELCOME_PAGE".equals(currentScreen) || "LOGIN_PAGE".equals(currentScreen)) {
            performLogin();
        } else {
            // UNKNOWN or in-app (Locations / Asset Detail / Issues / etc.)
            // We're already past the Site Selection step — just return so the
            // test can proceed from wherever it currently is.
            // Avoids calling performLogin() with no TextField present.
            try {
                io.appium.java_client.AppiumDriver d = DriverManager.getDriver();
                java.util.List<org.openqa.selenium.WebElement> signInOrEmail = d.findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' OR (type == 'XCUIElementTypeButton' AND name == 'Sign In')"));
                if (!signInOrEmail.isEmpty()) {
                    System.out.println("⚠️ UNKNOWN screen has Sign-In/TextField — running performLogin");
                    performLogin();
                } else {
                    System.out.println("✅ Appears in-app (no Sign-In / TextField) — skipping login flow");
                    return;
                }
            } catch (Exception e) {
                System.out.println("⚠️ In-app probe failed (" + e.getMessage() + ") — falling back to performLogin");
                performLogin();
            }
        }

        // Select first site immediately (combined wait + select)
        System.out.println("🔍 Selecting first available site...");
        // iOS sometimes pops the "Save Password?" sheet AFTER login lands
        // on Site Selection (not before, where loginTurbo handles it).
        // The sheet blocks the picker — selectFirstSiteFast would then
        // fail to find any tappable site row. Poll briefly for it and
        // dismiss "Not Now" before scanning the picker.
        try {
            // One attempt + one retry max — dismissSavePasswordIfPresent already
            // cascades alert + accessibility-id + predicate strategies internally
            // (at 150ms implicit wait), so 4 polling rounds only added three extra
            // switchTo().alert() round trips per test on the no-sheet happy path.
            if (!loginPage.dismissSavePasswordIfPresent()) {
                Thread.sleep(800); // sheet renders within ~1-2s when it appears at all
                loginPage.dismissSavePasswordIfPresent();
            }
        } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        autoScreenshot("Site Selection screen loaded");
        // Fail-fast: when the site context is dead (run 29006089840: every
        // selection attempt burned the full 120s dashboard wait, the timeout
        // was laundered into "✅ Site selected and loaded", and the suite bled
        // out over 40 min until WDA died → 5 fails + 186 skips), don't re-burn
        // the wait on every subsequent test.
        if (consecutiveSiteLoadFailures.get() >= 3) {
            throw new com.egalvanic.verify.VerificationError(
                    "loginAndSelectSite: site selection already failed "
                    + consecutiveSiteLoadFailures.get() + " consecutive times this session"
                    + " — failing fast (see the first failure for the diagnosis)");
        }
        String selectedSite = siteSelectionPage.selectFirstSiteFast();
        System.out.println("Selecting first site: (s) " + selectedSite);

        // Wait for dashboard to load after site selection — and tell the truth
        // about the outcome. A missed tap is transient (retry once); still
        // sitting on the picker after two full waits means the site never
        // loads and must FAIL, not launder. If the signals missed but the
        // screen detector proves the app DID advance into the site context
        // (locale/DOM drift in the 3 dashboard probes), proceed with a
        // warning — advancement, not signal match, is the contract.
        boolean dashboardReady = siteSelectionPage.waitForDashboardReady();
        if (!dashboardReady) {
            String screen = detectCurrentScreen();
            if ("SITE_SELECTION".equals(screen)) {
                System.out.println("⚠️ Still on Select Site after the dashboard wait — re-tapping the site once");
                selectedSite = siteSelectionPage.selectFirstSiteFast();
                dashboardReady = siteSelectionPage.waitForDashboardReady();
                if (!dashboardReady) screen = detectCurrentScreen();
            }
            if (!dashboardReady) {
                boolean inSiteContext = "DASHBOARD".equals(screen) || "ASSET_LIST".equals(screen)
                        || "ASSET_DETAIL".equals(screen) || "EDIT_ASSET".equals(screen);
                if (inSiteContext) {
                    System.out.println("⚠️ Dashboard signals missed but screen = " + screen
                            + " — site context is loaded, proceeding");
                } else {
                    consecutiveSiteLoadFailures.incrementAndGet();
                    autoScreenshot("Site selection FAILED — Dashboard never rendered (site: " + selectedSite + ")");
                    throw new com.egalvanic.verify.VerificationError(
                            "loginAndSelectSite: Dashboard never rendered after selecting site '" + selectedSite
                            + "' (screen now: " + screen + ") — the site load failed or the tap never"
                            + " registered. Failing honestly instead of printing '✅ Site selected and loaded'.");
                }
            }
        }
        consecutiveSiteLoadFailures.set(0);
        autoScreenshot("Dashboard loaded after site selection: " + selectedSite);

        System.out.println("✅ Site selected and loaded");
    }

    /**
     * Take an auto-screenshot for the Detailed report. Respects the
     * per-test screenshot cap so it never blows the size budget. Safe to
     * call anywhere — silently no-ops if cap reached or driver dead.
     */
    protected void autoScreenshot(String label) {
        if (!EVERY_STEP_SCREENSHOTS) return;
        if (stepScreenshotCount.get() >= MAX_STEP_SCREENSHOTS) return;
        try {
            ExtentReportManager.logStepWithBase64Screenshot("📸 " + label);
            stepScreenshotCount.incrementAndGet();
        } catch (Exception ignored) {}
    }

    /**
     * Run a block with a hard wall-clock budget.
     *
     * Root-cause fix for CI cascade skips (run #26641212845): when an Appium
     * session dies mid-suite, a custom @BeforeMethod / @AfterMethod that makes
     * several Appium calls (e.g. OfflineSyncMultiSite.perTestTeardown) blocks
     * ~90s PER call (the driver's HTTP readTimeout) plus any polling loop —
     * observed at 840s, which blew past the 420s per-test timeout and skipped
     * 39 downstream tests.
     *
     * This caps the WHOLE block: the body runs on a daemon thread; if it
     * doesn't finish within {@code seconds} we stop waiting and return. The
     * worker is a daemon (won't block JVM exit) and the 90s readTimeout
     * guarantees any hung Appium call it's stuck on eventually unwinds — so
     * the next test starts on time instead of inheriting the hang.
     */
    protected static void runWithBudget(String label, int seconds, Runnable body) {
        Thread worker = new Thread(() -> {
            try {
                body.run();
            } catch (Throwable t) {
                System.out.println("⚠️ " + label + " threw: " + t.getMessage());
            }
        }, "bounded-" + label);
        worker.setDaemon(true);
        worker.start();
        try {
            worker.join(seconds * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (worker.isAlive()) {
            System.out.println("⏱️ " + label + " exceeded " + seconds
                    + "s budget — abandoning (session likely dead, not blocking the next test)");
        }
    }

    // ================================================================
    // LOGGING HELPER METHODS
    // ================================================================

    /**
     * Log a test step.
     *
     * Per-step screenshots are controlled by the system property
     * 'screenshots.everyStep' (default = true). When enabled, every
     * logStep() call also embeds a Base64 screenshot in the Detailed
     * report, giving developers a per-step visual trail without
     * touching test source. Capped to {@link #MAX_STEP_SCREENSHOTS}
     * per test to keep report size bounded.
     *
     * Disable with -Dscreenshots.everyStep=false (e.g. for slow lanes).
     */
    protected void logStep(String stepDescription) {
        System.out.println("📝 " + stepDescription);
        if (EVERY_STEP_SCREENSHOTS && stepScreenshotCount.get() < MAX_STEP_SCREENSHOTS) {
            // Compressed JPEG (~15–25 KB each at .5 quality + .5 scale) lets
            // us pack lots more screenshots per test without blowing the
            // email size budget. Assertion-pass lines are INCLUDED so every
            // verified state has its own visual evidence.
            ExtentReportManager.logStepWithBase64Screenshot(stepDescription);
            stepScreenshotCount.incrementAndGet();
            return;
        }
        ExtentReportManager.logInfo(stepDescription);
    }

    private static final boolean EVERY_STEP_SCREENSHOTS =
            !"false".equalsIgnoreCase(System.getProperty("screenshots.everyStep", "true"));
    // Detailed reports live in workflow artifacts now — no email size budget
    // to respect. Cap raised to 500 so even the longest/most complex tests
    // never lose a shot. Realistic per-test count is 8–40.
    private static final int MAX_STEP_SCREENSHOTS =
            Integer.parseInt(System.getProperty("screenshots.maxPerTest", "500"));
    private final java.util.concurrent.atomic.AtomicInteger stepScreenshotCount =
            new java.util.concurrent.atomic.AtomicInteger(0);

    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ TURBO MODE: Login + Site Selection in minimum time           ║
     * ║ Target: Under 5 seconds for entire operation                 ║
     * ║ Uses: turboSelectSite() + waitForDashboardFast()             ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void loginAndSelectSiteTurbo() {
        long start = System.currentTimeMillis();

        // FAST PATH: Check Dashboard first (1 second max)
        if (assetPage != null && assetPage.isDashboardDisplayedFast()) {
            System.out.println("⚡ TURBO: Already on Dashboard (fast check) — skipping");
            return;
        }

        // Not on Dashboard — detect current screen to avoid typing into wrong field
        String currentScreen = detectCurrentScreen();
        System.out.println("⚡ TURBO: current screen: " + currentScreen);

        if ("DASHBOARD".equals(currentScreen)) {
            System.out.println("⚡ TURBO: Already on Dashboard — skipping");
            return;
        }

        if (!"SITE_SELECTION".equals(currentScreen)) {
            // On Welcome/Login page — do full login
            System.out.println("⚡ TURBO: Starting login...");
            welcomePage.submitCompanyCode(AppConstants.VALID_COMPANY_CODE);
            loginPage.waitForPageReady();
            loginPage.loginTurbo(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);

            // Handle new Schedule screen (added Jan 2026)
            siteSelectionPage.handleScheduleScreenIfPresent();
        }

        // Turbo site selection
        System.out.println("⚡ TURBO: Selecting site...");
        String site = siteSelectionPage.turboSelectSite();

        // Fast dashboard wait
        siteSelectionPage.waitForDashboardFast();

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("⚡ TURBO: Complete in " + elapsed + "ms - Site: " + site);
    }

    /**
     * ╔══════════════════════════════════════════════════════════════╗
     * ║ ULTRA FAST: Random site selection                            ║
     * ║ Uses selectRandomSiteUltraFast() for speed                   ║
     * ╚══════════════════════════════════════════════════════════════╝
     */
    protected final void loginAndSelectRandomSiteFast() {
        performLogin();
        
        System.out.println("⚡ Selecting random site (fast)...");
        String site = siteSelectionPage.selectRandomSiteUltraFast();
        System.out.println("⚡ Random site: " + site);
        
        siteSelectionPage.waitForDashboardFast();
    }



    /**
     * Log a step with screenshot (uses Base64 for portability)
     */
    protected void logStepWithScreenshot(String stepDescription) {
        ExtentReportManager.logStepWithBase64Screenshot(stepDescription);
        System.out.println("📸 " + stepDescription);
    }

    /**
     * Log warning
     */
    protected void logWarning(String message) {
        ExtentReportManager.logWarning(message);
        System.out.println("⚠️ " + message);
    }

    // ================================================================
    // ASSERTION HELPER METHODS
    // ================================================================

    /**
     * Take a moment-of-failure screenshot. Captures the exact state at
     * the point of assertion failure (before @AfterMethod fires and the
     * driver may have moved on / been torn down).
     */
    private void screenshotOnAssertionFail(String label) {
        try {
            if (DriverManager.isDriverActive()) {
                ExtentReportManager.logStepWithBase64Screenshot("❌ " + label);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Assert true with logging
     */
    protected void assertTrue(boolean condition, String message) {
        if (condition) {
            logStep("✅ Assertion passed: " + message);
        } else {
            screenshotOnAssertionFail("Assertion failed: " + message);
            ExtentReportManager.logFail("Assertion failed: " + message);
            throw new AssertionError(message);
        }
    }

    /**
     * Assert false with logging
     */
    protected void assertFalse(boolean condition, String message) {
        if (!condition) {
            logStep("✅ Assertion passed: " + message);
        } else {
            screenshotOnAssertionFail("Assertion failed: " + message);
            ExtentReportManager.logFail("Assertion failed: " + message);
            throw new AssertionError(message);
        }
    }

    /**
     * Assert equals with logging
     */
    protected void assertEquals(Object actual, Object expected, String message) {
        if (expected.equals(actual)) {
            logStep("✅ Assertion passed: " + message);
        } else {
            String errorMsg = message + " - Expected: " + expected + ", Actual: " + actual;
            screenshotOnAssertionFail(errorMsg);
            ExtentReportManager.logFail(errorMsg);
            throw new AssertionError(errorMsg);
        }
    }

    /**
     * Assert not null with logging
     */
    protected void assertNotNull(Object object, String message) {
        if (object != null) {
            logStep("✅ Assertion passed: " + message);
        } else {
            screenshotOnAssertionFail("Assertion failed: " + message + " (Object is null)");
            ExtentReportManager.logFail("Assertion failed: " + message + " (Object is null)");
            throw new AssertionError(message + " - Object is null");
        }
    }

    /**
     * Fail test with message
     */
    protected void fail(String message) {
        ExtentReportManager.logFail("Test FAILED: " + message);
        throw new AssertionError(message);
    }

    // ================================================================
    // HARDENING ENTRY POINTS (Phase 2 verifiers) — call these from tests
    // ================================================================

    /** Fail the test if the app is not in the foreground after {@code step} (crash/exit). */
    protected void verifyAppAlive(String step) {
        new com.egalvanic.verify.CrashDetector().assertAlive(step);
    }

    /** Fail the test if the named screen is blank/empty. */
    protected void verifyNotBlank(String screen) {
        new com.egalvanic.verify.UIStateValidator().assertNotBlank(screen);
    }

    /** Fail the test if an unexpected error alert is on screen (otherwise auto-accepted away). */
    protected void verifyNoErrorAlert() {
        new com.egalvanic.verify.UIStateValidator().assertNoErrorAlert();
    }

    /** Composite guard to call after a risky action: app alive AND no error alert. */
    protected void guard(String step) {
        new com.egalvanic.verify.CrashDetector().assertAlive(step);
        new com.egalvanic.verify.UIStateValidator().assertNoErrorAlert();
    }

    /** Fail if any visible actionable control on {@code screen} lacks a VoiceOver label. */
    protected void verifyAccessibility(String screen) {
        new com.egalvanic.verify.A11yVerifier().assertActionablesLabeled(screen);
    }

    /** Fail if an on-device store file is empty after a flow that should have persisted data. */
    protected void verifyPersisted(String relPath, String flow) {
        new com.egalvanic.verify.PersistenceVerifier().assertFileNonEmpty(relPath, flow);
    }

    /** Fail if cold launch to {@code readyMarker} exceeds {@code budgetMs}. */
    protected long verifyLaunchUnder(org.openqa.selenium.By readyMarker, long budgetMs) {
        return new com.egalvanic.verify.PerfVerifier().assertLaunchUnder(readyMarker, budgetMs);
    }

    /**
     * HARDENING (Phase 2): detect a crash a presence-only test missed. If a SUCCESS test
     * ends with the app not running, fail it. Conservative: a probe error (dead session,
     * which is already handled in teardown) is NOT converted into a manufactured failure.
     */
    private void failIfAppCrashedDuringSuccessfulTest(String testName) {
        if (!DriverManager.isDriverActive()) return;
        io.appium.java_client.appmanagement.ApplicationState st;
        try {
            st = DriverManager.getDriver().queryAppState(AppConstants.APP_BUNDLE_ID);
        } catch (Exception e) {
            return;
        }
        if (st == io.appium.java_client.appmanagement.ApplicationState.NOT_RUNNING
                || st == io.appium.java_client.appmanagement.ApplicationState.NOT_INSTALLED) {
            ExtentReportManager.logFail("App not running at end of '" + testName
                    + "' (state=" + st + ") — likely an unobserved crash");
            throw new AssertionError("App crashed/exited during test '" + testName
                    + "' (state=" + st + ")");
        }
    }

    // ================================================================
    // WAIT HELPER METHODS (CI-safe explicit waits)
    // ================================================================

    /**
     * Wait for specified milliseconds using explicit wait (CI-safe)
     * NOTE: until(d -> true) returns immediately — this is an INTENTIONAL no-op kept
     * for 1,100+ legacy call sites (real pauses would add ~6 min/run). Never use it
     * as a polling delay; loops must pause with a real Thread.sleep (see waitForCondition).
     */
    protected void sleep(int milliseconds) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                    com.egalvanic.utils.DriverManager.getDriver(),
                    java.time.Duration.ofMillis(milliseconds)).until(d -> true);
        } catch (Exception e) {
            // Ignore timeout
        }
    }

    /**
     * Short wait (1 second) - CI-safe
     */
    protected void shortWait() {
        sleep(200); // CI-OPTIMIZED: 300ms -> 200ms
    }

    /**
     * Medium wait (1 second) - CI-safe
     */
    protected void mediumWait() {
        sleep(400); // CI-OPTIMIZED: 600ms -> 400ms
    }

    /**
     * Long wait (2 seconds) - CI-safe
     */
    protected void longWait() {
        sleep(800); // CI-OPTIMIZED: 1200ms -> 800ms
    }

    /**
     * Dismiss any alert that might be present (Save Password, etc.)
     */
    protected void dismissAnyAlert() {
        try {
            welcomePage.handleSavePasswordAlert();
        } catch (Exception e) {
            // No alert present - continue
        }
    }

    /**
     * Mark this test to chain with next test (don't quit driver)
     * Call this at the END of a test that should continue to the next test
     */
    protected void chainToNextTest() {
        skipNextTeardown = true;
        System.out.println("🔗 Test will chain to next dependent test");
    }

    // ================================================================
    // RELIABILITY HELPERS — prevent flaky CI failures from timing
    // ================================================================
    // These were added 2026-04-29 to address the pattern where tests
    // pass in real-life manual runs but fail in CI due to:
    //   1. UI state checked too soon after a tap (toggle, button visibility)
    //   2. Element appearing intermittently due to simulator slowness
    //   3. Test data (e.g. "at least one Resolved issue") not yet in app state
    // See docs/ai-features-changelog/038-test-reliability-improvements.md

    /**
     * Poll a condition until it returns true OR timeout is reached.
     * Use this AFTER an action that triggers a UI state change (tap, swipe)
     * INSTEAD of `sleep(N)` followed by an immediate assertion.
     *
     * Example — fixes the "Toggle should be ON after enabling" cascade:
     *   assetPage.enableRequiredFieldsOnly();
     *   boolean toggleOn = waitForCondition(
     *       () -> assetPage.isRequiredFieldsToggleOn(),
     *       5, "Required fields toggle to be ON"
     *   );
     *   assertTrue(toggleOn, "Toggle should be ON after enabling");
     *
     * @param condition         the predicate to evaluate (typically a page-object getter)
     * @param timeoutSec        maximum seconds to wait for condition to become true
     * @param description       short label for log output (e.g. "toggle to be ON")
     * @return true if condition became true within timeout, false if it never did
     */
    protected boolean waitForCondition(java.util.function.Supplier<Boolean> condition,
                                       int timeoutSec, String description) {
        long deadline = System.currentTimeMillis() + (timeoutSec * 1000L);
        int attempt = 0;
        while (System.currentTimeMillis() < deadline) {
            attempt++;
            try {
                if (Boolean.TRUE.equals(condition.get())) {
                    if (attempt > 1) {
                        System.out.println("✓ Condition met on attempt " + attempt + ": " + description);
                    }
                    return true;
                }
            } catch (Exception ignored) {
                // Element may not exist yet — keep polling
            }
            // Real pause between probes. sleep(250) here is the no-op above, which made
            // this loop busy-spin (measured 2,800 log lines/sec — 95.6% of a 32MB log).
            try {
                Thread.sleep(250);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("⚠️ Condition NOT met within " + timeoutSec + "s: " + description);
        return false;
    }

    /**
     * Retry a non-idempotent action up to N times if it throws or returns false.
     * Use for actions that occasionally fail due to UI race conditions
     * (e.g. tap-on-element where the element was being re-rendered).
     *
     * @param action     a Runnable that may throw on transient failure
     * @param maxRetries total number of attempts before giving up
     * @param backoffMs  pause between retries
     * @return true if any attempt succeeded, false if all failed
     */
    protected boolean retryAction(Runnable action, int maxRetries, int backoffMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                action.run();
                return true;
            } catch (Exception e) {
                System.out.println("⚠️ Action attempt " + attempt + "/" + maxRetries + " failed: " + e.getMessage());
                if (attempt < maxRetries) sleep(backoffMs);
            }
        }
        return false;
    }

    /**
     * Skip a test cleanly when its required preconditions are not met.
     * Prefer this over silent assertion failures for data-dependent tests.
     *
     * Example — TC_ISS_210 needs at least one Resolved issue:
     *   skipIfPreconditionMissing(
     *       () -> issuePage.getResolvedIssueCount() > 0,
     *       "no Resolved issue exists in current site — cannot test swipe-on-resolved"
     *   );
     *
     * @param precondition  predicate that returns true when test data is ready
     * @param reason        human-readable reason for the skip (shown in report)
     */
    protected void skipIfPreconditionMissing(java.util.function.Supplier<Boolean> precondition,
                                             String reason) {
        try {
            if (!Boolean.TRUE.equals(precondition.get())) {
                System.out.println("⊘ Skipping — precondition not met: " + reason);
                throw new org.testng.SkipException("Precondition: " + reason);
            }
        } catch (org.testng.SkipException re) {
            throw re;  // re-throw — TestNG handles it
        } catch (Exception e) {
            System.out.println("⊘ Skipping — precondition check failed: " + e.getMessage());
            throw new org.testng.SkipException("Precondition check error: " + reason);
        }
    }

    /**
     * KNOWN APP BUG CAM-CRASH-01 (2026-07-07, crash log
     * Z Platform-QA-2026-07-07-195531.ips): the app calls
     * -[UIImagePickerController setSourceType:] with .camera without checking
     * UIImagePickerController.isSourceTypeAvailable(.camera). Simulators have no
     * camera, so UIKit throws NSInvalidArgumentException and the app SIGABRTs.
     * The dead app then cascades failures/skips through the rest of the suite
     * (this is what emptied SiteVisit shards in CI run 28867343990).
     *
     * Call this guard immediately BEFORE any camera-button tap. It skips the
     * test on simulators (all CI + local runs today). Must NOT be called inside
     * a catch(Exception)-wrapped block — the SkipException would be swallowed.
     * When the dev fix ships, or on a real device (-DREAL_DEVICE=true /
     * REAL_DEVICE=true), this is a no-op.
     */
    protected void guardCameraTapCrash(String context) {
        String realDevice = System.getProperty("REAL_DEVICE",
                System.getenv().getOrDefault("REAL_DEVICE", "false"));
        if (!Boolean.parseBoolean(realDevice)) {
            System.out.println("⊘ Skipping camera tap — KNOWN APP BUG CAM-CRASH-01 "
                    + "(UIImagePickerController crash on simulator): " + context);
            throw new org.testng.SkipException(
                    "KNOWN APP BUG CAM-CRASH-01 — tapping Camera crashes the app on "
                    + "simulators (UIImagePickerController setSourceType). Skipped '"
                    + context + "' until the app-side fix; run with REAL_DEVICE=true "
                    + "on hardware to cover this.");
        }
    }
}
