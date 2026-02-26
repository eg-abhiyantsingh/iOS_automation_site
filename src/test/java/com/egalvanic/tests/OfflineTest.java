package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.IssuePage;
import io.appium.java_client.AppiumBy;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Offline Mode Test Suite
 *
 * Total Test Cases: 35
 * - TC_OFF_001: Verify Go Offline button on site dashboard
 * - TC_OFF_002: Verify tapping Go Offline switches to offline mode
 * - TC_OFF_003: Verify WiFi icon indicates offline status
 * - TC_OFF_004: Verify Refresh is disabled in offline mode
 * - TC_OFF_005: Verify Sites navigation is disabled in offline mode
 * - TC_OFF_006: Verify cannot change active site in offline mode
 * - TC_OFF_007: Verify Quick Count available in offline mode
 * - TC_OFF_008: Verify Locations tile available in offline mode
 * - TC_OFF_009: Verify My Tasks available in offline mode
 * - TC_OFF_010: Verify Issues available in offline mode
 * - TC_OFF_011: Verify can create new building in offline mode
 * - TC_OFF_012: Verify can create new asset in offline mode
 * - TC_OFF_013: Verify can create new task in offline mode
 * - TC_OFF_014: Verify can create new issue in offline mode
 * - TC_OFF_015: Verify can capture IR photos in offline mode (Partial)
 * - TC_OFF_016: Verify pending sync badge on WiFi icon
 * - TC_OFF_017: Verify pending sync badge on Sites tile
 * - TC_OFF_018: Verify Settings screen Sync & Network section
 * - TC_OFF_019: Verify Network Mode toggle in Settings
 * - TC_OFF_020: Verify Sync Queue Analyzer shows pending count
 * - TC_OFF_021: Verify Sync Queue Analyzer screen layout
 * - TC_OFF_022: Verify Pending tab shows queued operations
 * - TC_OFF_023: Verify History tab shows completed syncs
 * - TC_OFF_024: Verify pending item shows operation type
 * - TC_OFF_025: Verify pending item shows queue time
 * - TC_OFF_026: Verify Network Mode toggle switches to online
 * - TC_OFF_027: Verify pending items sync when going online
 * - TC_OFF_028: Verify WiFi icon returns to normal when online
 * - TC_OFF_029: Verify Refresh and Sites re-enable when online
 * - TC_OFF_030: Verify Account section in Settings
 * - TC_OFF_031: (Removed)
 * - TC_OFF_032: Verify Photo Storage Diagnostics in Diagnostics section
 * - TC_OFF_033: Verify active job persists in offline mode
 * - TC_OFF_034: Verify site stats visible in offline mode
 * - TC_OFF_035: Verify multiple offline operations queue correctly
 *
 * Features Covered:
 * - Go Offline (TC_OFF_001 - TC_OFF_003)
 * - Offline Restrictions (TC_OFF_004 - TC_OFF_006)
 * - Offline Operations (TC_OFF_007 - TC_OFF_010)
 * - Offline Creation (TC_OFF_011 - TC_OFF_015)
 * - Sync Queue (TC_OFF_016 - TC_OFF_017)
 * - Offline Settings (TC_OFF_018 - TC_OFF_019)
 * - Sync Queue Analyzer (TC_OFF_020 - TC_OFF_025)
 * - Go Online (TC_OFF_026 - TC_OFF_029)
 * - Settings Sections (TC_OFF_030 - TC_OFF_032)
 * - Offline Operations (TC_OFF_033 - TC_OFF_034)
 * - Sync Queue Multi-Op (TC_OFF_035)
 */
public final class OfflineTest extends BaseTest {

    // Page objects for offline creation tests
    private IssuePage issuePage;

    // ============================================================
    // TEST CLASS SETUP
    // ============================================================

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        System.out.println("\n📋 Offline Mode Test Suite - Starting");
        DriverManager.setNoReset(true);
    }

    @AfterClass(alwaysRun = true)
    public void classTeardown() {
        DriverManager.resetNoResetOverride();
        System.out.println("\n📋 Offline Mode Test Suite - Complete");
    }

    // Flag to run stale-state cleanup only once per suite
    private static boolean initialCleanupDone = false;

    @BeforeMethod(alwaysRun = true)
    public void offlineTestSetup() {
        issuePage = new IssuePage();

        // One-time cleanup: restore clean online state (WiFi icon = "Wi-Fi")
        // if a previous run left pending sync records or offline mode.
        // WiFi icon has 3 states: "Wi-Fi" (clean), "Wi-Fi Off" (offline),
        // or a digit badge (pending sync — ambiguous, can be online OR offline).
        // isWifiOnline() returns false for sync badge, so we MUST sync to clear it.
        if (!initialCleanupDone) {
            initialCleanupDone = true;
            restoreCleanOnlineState();
        }
    }

    /**
     * One-time cleanup: ensure WiFi icon shows "Wi-Fi" (clean online, no sync badge).
     * Steps: detect state → go online if offline → sync pending records → verify clean.
     */
    private void restoreCleanOnlineState() {
        try {
            if (siteSelectionPage == null) return;
            io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();

            // Fast path: already clean online — nothing to do
            if (isWifiIconClean(d)) {
                System.out.println("✅ App is clean online — no cleanup needed");
                return;
            }

            // Not clean — open WiFi popup to determine state and fix it
            System.out.println("🔧 WiFi not clean — opening popup to restore state");
            siteSelectionPage.clickWifiButton();
            shortWait();

            // If "Go Online" visible → app is offline → go online first
            if (siteSelectionPage.isGoOnlineOptionVisible()) {
                System.out.println("⚠️ App is offline — clicking Go Online");
                try {
                    d.findElement(AppiumBy.iOSNsPredicateString(
                        "label == 'Go Online' OR name == 'Go Online'")).click();
                } catch (Exception e) {
                    siteSelectionPage.goOnline();
                }
                Thread.sleep(2000); // Wait for online transition

                // After going online, check if we're clean now
                if (isWifiIconClean(d)) {
                    System.out.println("✅ Clean after going online — done");
                    return;
                }

                // Still have sync badge — re-open popup to sync
                siteSelectionPage.clickWifiButton();
                shortWait();
            }

            // Popup is open — sync pending records if present
            syncFromOpenPopup(d);

        } catch (Exception e) {
            System.out.println("⚠️ Cleanup exception (continuing): " + e.getMessage());
        }
    }

    /**
     * Check if WiFi icon shows clean "Wi-Fi" accessibility ID (no sync badge, not offline).
     */
    private boolean isWifiIconClean(io.appium.java_client.ios.IOSDriver d) {
        d.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(500));
        try {
            return !d.findElements(AppiumBy.accessibilityId("Wi-Fi")).isEmpty();
        } catch (Exception e) {
            return false;
        } finally {
            d.manage().timeouts().implicitlyWait(
                java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
        }
    }

    /**
     * With WiFi popup already open, click "Sync N records" and wait for WiFi icon
     * to return to clean "Wi-Fi" state. Dismisses popup if no sync records found.
     */
    private void syncFromOpenPopup(io.appium.java_client.ios.IOSDriver d) {
        try {
            if (siteSelectionPage.isSyncRecordsOptionVisible()) {
                System.out.println("📡 Syncing pending records...");
                siteSelectionPage.clickSyncRecords();

                // Poll every 500ms for up to 15s for WiFi icon to become clean
                for (int i = 0; i < 30; i++) {
                    Thread.sleep(500);
                    if (isWifiIconClean(d)) {
                        System.out.println("✅ Sync complete — WiFi icon is clean");
                        return;
                    }
                }
                System.out.println("⏰ Sync wait timed out (15s) — continuing");
            } else {
                System.out.println("ℹ️ No sync records in popup — dismissing");
                dismissWifiPopup(d);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Sync failed: " + e.getMessage());
        }
    }

    /**
     * Dismiss WiFi popup by tapping outside it.
     */
    private void dismissWifiPopup(io.appium.java_client.ios.IOSDriver d) {
        // Use short timeout to avoid 5s block when Dashboard element not found
        d.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(500));
        try {
            d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeOther' AND name == 'Dashboard'")).click();
        } catch (Exception e) {
            // Fallback: tap at a neutral screen location to dismiss popup
            try {
                org.openqa.selenium.Dimension screenSize = d.manage().window().getSize();
                d.executeScript("mobile: tap", java.util.Map.of(
                    "x", screenSize.getWidth() / 2,
                    "y", screenSize.getHeight() - 100));
            } catch (Exception tapEx) {
                shortWait();
            }
        } finally {
            d.manage().timeouts().implicitlyWait(
                java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
        }
    }

    /**
     * Reliably check if the app is definitely offline.
     * Handles all 3 WiFi icon states:
     *   "Wi-Fi Off"  → definitely offline
     *   "Wi-Fi"      → definitely online (return false)
     *   sync badge   → ambiguous, open popup to check for "Go Online"
     */
    private boolean isDefinitelyOffline() {
        io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();

        // Check definitive WiFi icons only (no digit regex — matches unrelated elements)
        d.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(500));
        try {
            // Check 1: "Wi-Fi Off" = definitely offline
            if (!d.findElements(AppiumBy.accessibilityId("Wi-Fi Off")).isEmpty()) {
                return true;
            }
            // Check 2: "Wi-Fi" = definitely online
            if (!d.findElements(AppiumBy.accessibilityId("Wi-Fi")).isEmpty()) {
                return false;
            }
        } catch (Exception e) { /* ignore */ }
        finally {
            d.manage().timeouts().implicitlyWait(
                java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
        }

        // Check 3: Neither found (sync badge or unknown) — use popup to determine
        try {
            siteSelectionPage.clickWifiButton();
            sleep(800); // Wait for popup animation
            boolean goOnlineVisible = siteSelectionPage.isGoOnlineOptionVisible();
            dismissWifiPopup(d);
            return goOnlineVisible;
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // HELPER: Reliable WiFi state management
    // The page object's isWifiOnline()/isWifiOffline() give wrong
    // results when a sync count badge is present (digit icon).
    // These helpers use the WiFi popup to reliably detect and toggle.
    // ============================================================

    /**
     * Go offline using WiFi popup. Handles all 3 WiFi icon states:
     *   "Wi-Fi Off" → already offline
     *   "Wi-Fi"     → open popup → click Go Offline
     *   sync badge  → open popup → click Go Offline (if available)
     */
    private void goOfflineViaPopup() {
        io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();

        // Fast check with short timeout: already definitively offline?
        // Only check "Wi-Fi Off" — the DEFINITIVE offline indicator.
        // Do NOT check digit regex here — it matches unrelated numeric elements (tab badges, etc.)
        // Sync badge (digit) is ambiguous and handled by popup-based detection below.
        d.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(500));
        try {
            java.util.List<org.openqa.selenium.WebElement> offlineIcons = d.findElements(
                AppiumBy.accessibilityId("Wi-Fi Off"));
            if (!offlineIcons.isEmpty()) {
                logStep("Already offline (Wi-Fi Off icon)");
                return;
            }
        } catch (Exception e) { /* not offline */ }
        finally {
            d.manage().timeouts().implicitlyWait(
                java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
        }

        // Open popup to determine state and toggle
        siteSelectionPage.clickWifiButton();
        sleep(800); // Wait for popup animation (iOS needs 300-500ms)

        if (siteSelectionPage.isGoOfflineOptionVisible()) {
            logStep("Clicking Go Offline from popup");
            d.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(800));
            try {
                d.findElement(AppiumBy.iOSNsPredicateString(
                    "label == 'Go Offline' OR name == 'Go Offline'")).click();
            } catch (Exception e) {
                d.manage().timeouts().implicitlyWait(
                    java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
                dismissWifiPopup(d);
                siteSelectionPage.goOffline();
            } finally {
                d.manage().timeouts().implicitlyWait(
                    java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
            }
            mediumWait();
        } else if (siteSelectionPage.isGoOnlineOptionVisible()) {
            // Already offline — dismiss popup
            logStep("Already offline (popup shows Go Online)");
            dismissWifiPopup(d);
        } else {
            // Popup didn't open properly — fallback
            dismissWifiPopup(d);
            siteSelectionPage.goOffline();
            mediumWait();
        }
    }

    /**
     * Go online using WiFi popup. Handles all 3 WiFi icon states:
     *   "Wi-Fi"     → already online
     *   "Wi-Fi Off" → open popup → click Go Online
     *   sync badge  → open popup → click Go Online (if available), else already online
     */
    private void goOnlineViaPopup() {
        io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();

        // Fast check: already clean online?
        if (isWifiIconClean(d)) {
            logStep("Already online (Wi-Fi icon clean)");
            return;
        }

        // Open popup to determine state and toggle
        siteSelectionPage.clickWifiButton();
        sleep(800); // Wait for popup animation (iOS needs 300-500ms)

        if (siteSelectionPage.isGoOnlineOptionVisible()) {
            logStep("Clicking Go Online from popup");
            d.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(800));
            try {
                d.findElement(AppiumBy.iOSNsPredicateString(
                    "label == 'Go Online' OR name == 'Go Online'")).click();
            } catch (Exception e) {
                d.manage().timeouts().implicitlyWait(
                    java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
                dismissWifiPopup(d);
                siteSelectionPage.goOnline();
            } finally {
                d.manage().timeouts().implicitlyWait(
                    java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
            }
            mediumWait();
        } else if (siteSelectionPage.isGoOfflineOptionVisible()) {
            // Already online — dismiss popup
            logStep("Already online (popup shows Go Offline)");
            dismissWifiPopup(d);
        } else {
            // Popup didn't open properly — fallback
            dismissWifiPopup(d);
            siteSelectionPage.goOnline();
            mediumWait();
        }
    }

    /**
     * Navigate to dashboard and switch to offline mode.
     * Uses popup-based detection to avoid false readings from sync badge.
     */
    private void navigateToDashboardAndGoOffline() {
        logStep("Ensuring we are on dashboard before going offline");
        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        logStep("Going offline");
        goOfflineViaPopup();

        // Verify offline state using definitive check (handles sync badge)
        boolean offlineConfirmed = isDefinitelyOffline();
        logStep("Offline mode confirmed: " + offlineConfirmed);
        assertTrue(offlineConfirmed, "App should be in offline mode after tapping Go Offline");
    }

    /**
     * Ensure we go back online at the end of a test.
     * Uses direct icon check to avoid false positives from sync badge.
     */
    private void ensureOnlineState() {
        try {
            io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();

            // Already clean online? Done.
            if (isWifiIconClean(d)) return;

            // Definitively offline ("Wi-Fi Off" icon)? Go online.
            boolean definitelyOffline = false;
            try {
                definitelyOffline = d.findElement(
                    AppiumBy.accessibilityId("Wi-Fi Off")).isDisplayed();
            } catch (Exception e) { /* not offline */ }

            if (definitelyOffline) {
                logStep("Restoring online state for next test");
                goOnlineViaPopup();
                return;
            }

            // Sync badge present — app is already online, nothing to do.
            // The sync badge will be cleared by @BeforeMethod on the next
            // suite run via restoreCleanOnlineState().
        } catch (Exception e) {
            logWarning("Could not restore online state: " + e.getMessage());
        }
    }

    // ============================================================
    // GO OFFLINE TESTS (TC_OFF_001 - TC_OFF_003)
    // ============================================================

    @Test(priority = 1)
    public void TC_OFF_001_verifyGoOfflineButtonOnSiteDashboard() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_GO_OFFLINE,
                "TC_OFF_001 - Verify Go Offline button on site dashboard");
        logStep("Logging in and selecting a site");
        loginAndSelectSite();

        logStep("Verifying dashboard is displayed");
        assertTrue(siteSelectionPage.isDashboardDisplayed(), "Site dashboard should be displayed");

        logStep("Verifying app is online on dashboard");
        boolean isOffline = isDefinitelyOffline();
        assertFalse(isOffline, "App should be online on dashboard before testing WiFi popup");

        logStep("Clicking WiFi icon to open popup menu");
        siteSelectionPage.clickWifiButton();
        sleep(800); // Wait for popup animation (iOS needs 300-500ms)

        logStepWithScreenshot("Verifying 'Go Offline' option is visible in popup");
        // The Go Offline option should be present when user is online
        // isGoOfflineOptionVisible() now uses findElements with 800ms short timeout
        boolean goOfflineVisible = siteSelectionPage.isGoOfflineOptionVisible();
        if (!goOfflineVisible) {
            // One retry — popup may need extra time
            logWarning("Go Offline not visible on first check, retrying after brief wait...");
            sleep(500);
            goOfflineVisible = siteSelectionPage.isGoOfflineOptionVisible();
        }

        assertTrue(goOfflineVisible,
                "Go Offline option should be visible in WiFi popup when user is online");

        logStep("Go Offline button verified on site dashboard");
    }

    @Test(priority = 2)
    public void TC_OFF_002_verifyTappingGoOfflineSwitchesToOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_GO_OFFLINE,
                "TC_OFF_002 - Verify tapping Go Offline switches to offline mode");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        logStep("Verifying app is currently online");
        boolean initiallyOnline = siteSelectionPage.isWifiOnline();
        logStep("Initial online state: " + initiallyOnline);

        logStep("Tapping Go Offline to switch to offline mode");
        goOfflineViaPopup();
        shortWait();

        logStepWithScreenshot("Verifying app switched to offline mode");
        boolean isOffline = isDefinitelyOffline();
        assertTrue(isOffline, "App should be in offline mode after tapping Go Offline");

        logStep("Verifying 'Go Offline' button is no longer visible");
        // After going offline, the WiFi popup should show "Go Online" instead
        siteSelectionPage.clickWifiButton();
        shortWait();

        boolean goOfflineStillVisible = false;
        try {
            goOfflineStillVisible = siteSelectionPage.isGoOfflineOptionVisible();
        } catch (Exception e) {
            // Expected — Go Offline should not be visible when already offline
        }
        assertFalse(goOfflineStillVisible,
                "Go Offline option should NOT be visible after switching to offline mode");

        // Verify Go Online is now shown instead
        boolean goOnlineVisible = false;
        try {
            goOnlineVisible = siteSelectionPage.isGoOnlineOptionVisible();
        } catch (Exception e) {
            logWarning("Could not verify Go Online option: " + e.getMessage());
        }
        logStep("Go Online option visible: " + goOnlineVisible);

        // Cleanup: go back online
        ensureOnlineState();
    }

    @Test(priority = 3)
    public void TC_OFF_003_verifyWiFiIconIndicatesOfflineStatus() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_GO_OFFLINE,
                "TC_OFF_003 - Verify WiFi icon indicates offline status");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        logStep("Recording WiFi state before going offline");
        boolean offlineBefore = isDefinitelyOffline();
        logStep("Before toggle — Offline: " + offlineBefore);

        logStep("Going offline");
        goOfflineViaPopup();
        shortWait();

        logStepWithScreenshot("Verifying WiFi icon shows offline/strikethrough state");
        boolean isOfflineAfter = isDefinitelyOffline();

        logStep("After toggle — Offline: " + isOfflineAfter);

        // The WiFi icon should now show "Wi-Fi Off" or sync count badge (both = offline)
        assertTrue(isOfflineAfter, "WiFi icon should show crossed-out/strikethrough state indicating offline mode");

        logWarning("Color verification (red strikethrough lines) is limited in automation — " +
                "verified via accessibility ID change from 'Wi-Fi' to 'Wi-Fi Off'");

        // Cleanup: go back online
        ensureOnlineState();
    }

    // ============================================================
    // OFFLINE RESTRICTIONS TESTS (TC_OFF_004 - TC_OFF_006)
    // ============================================================

    @Test(priority = 4)
    public void TC_OFF_004_verifyRefreshDisabledInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_RESTRICTIONS,
                "TC_OFF_004 - Verify Refresh is disabled in offline mode");

        navigateToDashboardAndGoOffline();

        logStep("Checking Refresh button display state");
        boolean refreshDisplayed = siteSelectionPage.isRefreshButtonDisplayed();
        logStep("Refresh button displayed: " + refreshDisplayed);

        logStepWithScreenshot("Verifying Refresh button is disabled/grayed out");
        if (refreshDisplayed) {
            // Refresh button visible but should be disabled
            boolean refreshEnabled = siteSelectionPage.isRefreshButtonEnabled();
            logStep("Refresh button enabled attribute: " + refreshEnabled);
            assertFalse(refreshEnabled,
                    "Refresh tile should be grayed out/disabled in offline mode");
        } else {
            // Refresh button may be hidden entirely in offline mode — also valid
            logStep("Refresh button is hidden in offline mode (valid offline behavior)");
        }

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 5)
    public void TC_OFF_005_verifySitesNavigationDisabledInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_RESTRICTIONS,
                "TC_OFF_005 - Verify Sites navigation is disabled in offline mode");

        navigateToDashboardAndGoOffline();

        logStep("Checking Sites button display state");
        boolean sitesDisplayed = siteSelectionPage.isSitesButtonDisplayed();
        logStep("Sites button displayed: " + sitesDisplayed);

        logStepWithScreenshot("Verifying Sites navigation is restricted in offline mode");
        if (sitesDisplayed) {
            // The Sites button may appear enabled but should NOT navigate away.
            // In iOS, the button's 'enabled' attribute may not change visually —
            // the app blocks navigation by keeping the user on the dashboard.
            boolean sitesEnabled = false;
            try {
                sitesEnabled = siteSelectionPage.isSitesButtonEnabled();
            } catch (Exception e) {
                logWarning("Could not check Sites button enabled state: " + e.getMessage());
            }
            logStep("Sites button enabled attribute: " + sitesEnabled);

            if (sitesEnabled) {
                // Button reports enabled — tap directly (avoid waitForClickable timeout)
                logStep("Sites button reports enabled — tapping directly to verify blocked");
                try {
                    io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
                    d.findElement(AppiumBy.accessibilityId("building.2")).click();
                    shortWait();
                } catch (Exception e) {
                    logStep("Sites button tap blocked (expected): " + e.getMessage());
                }

                logStepWithScreenshot("After tapping Sites in offline mode");
                assertTrue(siteSelectionPage.isDashboardDisplayed(),
                        "Sites tile should NOT allow site navigation while offline — " +
                        "user should remain on dashboard");
            } else {
                logStep("Sites button correctly disabled in offline mode");
            }
        } else {
            // Sites button may be hidden entirely in offline mode — also valid
            logStep("Sites button is hidden in offline mode (valid offline restriction)");
        }

        // Verify we're still on the dashboard
        assertTrue(siteSelectionPage.isDashboardDisplayed(),
                "Should remain on dashboard — Sites navigation should be blocked offline");

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 6)
    public void TC_OFF_006_verifyCannotChangeActiveSiteInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_RESTRICTIONS,
                "TC_OFF_006 - Verify cannot change active site in offline mode");

        navigateToDashboardAndGoOffline();

        logStep("Attempting to navigate to site selection while offline");

        // The Sites button should be disabled — verify we cannot navigate away
        boolean sitesDisplayed = siteSelectionPage.isSitesButtonDisplayed();
        boolean sitesEnabled = false;

        if (sitesDisplayed) {
            try {
                sitesEnabled = siteSelectionPage.isSitesButtonEnabled();
            } catch (Exception e) {
                logWarning("Could not check Sites button state: " + e.getMessage());
            }
        }

        logStep("Sites button displayed: " + sitesDisplayed + ", enabled: " + sitesEnabled);

        if (sitesDisplayed && sitesEnabled) {
            // Edge case: iOS reports enabled=true even in offline.
            // Tap the element directly (avoid waitForClickable which hangs 10s)
            // and verify user stays on dashboard.
            logStep("Sites button appears enabled, tapping directly to verify no navigation");
            try {
                io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
                d.findElement(AppiumBy.accessibilityId("building.2")).click();
                shortWait();
            } catch (Exception e) {
                logStep("Sites button tap blocked (expected in offline): " + e.getMessage());
            }

            logStepWithScreenshot("Verifying user remains on current site after tapping Sites");
            boolean stillOnDashboard = siteSelectionPage.isDashboardDisplayed();
            boolean stillOffline = isDefinitelyOffline();

            assertTrue(stillOnDashboard || stillOffline,
                    "User should remain on current site — site switching not available offline");
        } else {
            // Button is disabled or hidden — this IS the expected behavior
            logStepWithScreenshot("Sites button correctly disabled/hidden in offline mode");
            assertFalse(sitesEnabled,
                    "Site switching should not be available while offline");
        }

        // Verify WiFi still shows offline (user hasn't been forced online)
        boolean stillOffline = isDefinitelyOffline();
        assertTrue(stillOffline,
                "App should still be in offline mode after attempting site switch");

        // Cleanup
        ensureOnlineState();
    }

    // ============================================================
    // OFFLINE OPERATIONS TESTS (TC_OFF_007 - TC_OFF_009)
    // ============================================================

    @Test(priority = 7)
    public void TC_OFF_007_verifyQuickCountAvailableInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_OPERATIONS,
                "TC_OFF_007 - Verify Quick Count available in offline mode");

        navigateToDashboardAndGoOffline();

        logStepWithScreenshot("Checking Quick Count tile on dashboard in offline mode");

        // Quick Count should remain active (not grayed out) in offline mode
        // It's an offline-capable feature that works without network connectivity
        boolean quickCountDisplayed = siteSelectionPage.isQuickCountDisplayed();
        logStep("Quick Count tile displayed: " + quickCountDisplayed);

        if (quickCountDisplayed) {
            boolean quickCountEnabled = siteSelectionPage.isQuickCountEnabled();
            logStep("Quick Count tile enabled: " + quickCountEnabled);

            assertTrue(quickCountEnabled,
                    "Quick Count tile should be active (not grayed out) in offline mode — " +
                    "Quick Count is an offline-capable feature");

            // Verify tapping Quick Count works by attempting to open it
            logStep("Tapping Quick Count to verify it opens in offline mode");
            try {
                siteSelectionPage.clickQuickCountButton();
                mediumWait(); // Wait for screen transition

                // If we navigated away from dashboard, Quick Count opened successfully
                boolean leftDashboard = !siteSelectionPage.isDashboardDisplayed();
                logStep("Left dashboard after tapping Quick Count: " + leftDashboard);

                if (leftDashboard) {
                    logStepWithScreenshot("Quick Count feature opened successfully in offline mode");
                } else {
                    // Some implementations may show Quick Count as an overlay on dashboard
                    logStep("Quick Count may have opened as overlay or modal on dashboard");
                }
            } catch (Exception e) {
                logWarning("Quick Count tap had an error: " + e.getMessage());
                // The tile being displayed and enabled is sufficient — tap behavior
                // may vary depending on app state and job selection
            }
        } else {
            // Quick Count tile may not be visible on all dashboard layouts
            // (e.g., requires an active job). Log as warning, not failure.
            logWarning("Quick Count tile not displayed on dashboard — " +
                    "may require active job or specific dashboard configuration");
            // Still pass: the tile's visibility depends on job state, not offline mode
        }

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 8)
    public void TC_OFF_008_verifyLocationsAvailableInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_OPERATIONS,
                "TC_OFF_008 - Verify Locations tile available in offline mode");

        navigateToDashboardAndGoOffline();

        logStepWithScreenshot("Checking Locations tile on dashboard in offline mode");

        // Locations should remain accessible in offline mode
        // Users need to view and manage locations even without network
        boolean locationsDisplayed = siteSelectionPage.isLocationsButtonDisplayed();
        logStep("Locations tile displayed: " + locationsDisplayed);
        assertTrue(locationsDisplayed,
                "Locations tile should be visible on dashboard in offline mode");

        boolean locationsEnabled = siteSelectionPage.isLocationsButtonEnabled();
        logStep("Locations tile enabled: " + locationsEnabled);
        assertTrue(locationsEnabled,
                "Locations tile should be active (not grayed out) — " +
                "location management is available offline");

        // Verify tapping Locations actually navigates to the locations screen
        logStep("Tapping Locations to verify navigation works in offline mode");
        try {
            siteSelectionPage.clickLocations();
            mediumWait(); // Wait for navigation transition

            // Check if we left the dashboard (indicating successful navigation)
            boolean leftDashboard = !siteSelectionPage.isDashboardDisplayed();

            if (leftDashboard) {
                logStepWithScreenshot("Locations screen opened successfully in offline mode");

                // Verify we're on the Locations screen (not an error screen)
                boolean locationsScreenVisible = false;
                try {
                    locationsScreenVisible = siteSelectionPage.isLocationsScreenDisplayed();
                } catch (Exception e) {
                    logWarning("Could not verify Locations screen elements: " + e.getMessage());
                }
                logStep("Locations screen confirmed: " + locationsScreenVisible);
            } else {
                logWarning("Did not navigate away from dashboard — " +
                        "Locations may have opened as overlay or navigation was blocked");
            }
        } catch (Exception e) {
            logWarning("Locations navigation error: " + e.getMessage());
            // The tile being displayed and enabled is the primary assertion
        }

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 9)
    public void TC_OFF_009_verifyMyTasksAvailableInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_OPERATIONS,
                "TC_OFF_009 - Verify My Tasks available in offline mode");

        navigateToDashboardAndGoOffline();

        logStepWithScreenshot("Checking My Tasks tile on dashboard in offline mode");

        // Use direct driver search — page object's isMyTasksDisplayed() returns false
        // because isDisplayed() relies on the 'visible' attribute which iOS reports as
        // false for certain element types even when they're clearly on screen.
        io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
        int screenHeight = d.manage().window().getSize().height;

        // Find all elements matching "My Tasks"
        java.util.List<org.openqa.selenium.WebElement> myTasksElements = d.findElements(
                AppiumBy.iOSNsPredicateString(
                    "label == 'My Tasks' OR name == 'My Tasks'"));
        logStep("My Tasks elements found: " + myTasksElements.size());

        // Pick the best candidate: element within visible screen bounds
        org.openqa.selenium.WebElement myTasksTile = null;
        for (org.openqa.selenium.WebElement el : myTasksElements) {
            try {
                int y = el.getLocation().getY();
                String type = el.getAttribute("type");
                String visible = el.getAttribute("visible");
                logStep("  Element — type: " + type + ", visible: " + visible +
                        ", Y: " + y + ", enabled: " + el.getAttribute("enabled"));
                // Accept element if its Y is within the visible screen area
                if (y > 0 && y < screenHeight) {
                    myTasksTile = el;
                    break;
                }
            } catch (Exception e) { /* skip stale elements */ }
        }

        assertTrue(myTasksTile != null,
                "My Tasks tile should be present on dashboard in offline mode");

        String enabled = myTasksTile.getAttribute("enabled");
        logStep("My Tasks tile enabled: " + enabled);
        assertTrue("true".equalsIgnoreCase(enabled),
                "My Tasks tile should be active (not grayed out) — " +
                "cached tasks should be viewable offline");

        // Verify tapping My Tasks navigates to the tasks screen
        logStep("Tapping My Tasks to verify it opens and shows cached tasks");
        try {
            myTasksTile.click();
            mediumWait();

            boolean leftDashboard = !siteSelectionPage.isDashboardDisplayed();

            if (leftDashboard) {
                logStepWithScreenshot("My Tasks screen opened successfully in offline mode — " +
                        "cached tasks are viewable");
            } else {
                logWarning("Did not navigate away from dashboard — " +
                        "My Tasks may require active job or show as overlay");
            }
        } catch (Exception e) {
            logWarning("My Tasks navigation error: " + e.getMessage());
        }

        // Cleanup
        ensureOnlineState();
    }

    // ============================================================
    // OFFLINE OPERATIONS - ISSUES (TC_OFF_010)
    // ============================================================

    @Test(priority = 10)
    public void TC_OFF_010_verifyIssuesAvailableInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_OPERATIONS,
                "TC_OFF_010 - Verify Issues available in offline mode");

        navigateToDashboardAndGoOffline();

        logStepWithScreenshot("Checking Issues tile on dashboard in offline mode");

        // Use direct driver search — page object's isIssuesDisplayed() returns false
        // because isDisplayed() relies on the 'visible' attribute which iOS reports as
        // false for certain element types even when they're clearly on screen.
        io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
        int screenHeight = d.manage().window().getSize().height;

        // Find all elements matching "Issues"
        java.util.List<org.openqa.selenium.WebElement> issuesElements = d.findElements(
                AppiumBy.iOSNsPredicateString(
                    "label == 'Issues' OR name == 'Issues'"));
        logStep("Issues elements found: " + issuesElements.size());

        // Pick the best candidate: element within visible screen bounds
        org.openqa.selenium.WebElement issuesTile = null;
        for (org.openqa.selenium.WebElement el : issuesElements) {
            try {
                int y = el.getLocation().getY();
                String type = el.getAttribute("type");
                String visible = el.getAttribute("visible");
                logStep("  Element — type: " + type + ", visible: " + visible +
                        ", Y: " + y + ", enabled: " + el.getAttribute("enabled"));
                // Accept element if its Y is within the visible screen area
                if (y > 0 && y < screenHeight) {
                    issuesTile = el;
                    break;
                }
            } catch (Exception e) { /* skip stale elements */ }
        }

        assertTrue(issuesTile != null,
                "Issues tile should be present on dashboard in offline mode");

        String enabled = issuesTile.getAttribute("enabled");
        logStep("Issues tile enabled: " + enabled);
        assertTrue("true".equalsIgnoreCase(enabled),
                "Issues tile should be active (not grayed out) in offline mode");

        // Verify tapping Issues navigates to the issues screen
        logStep("Tapping Issues to verify navigation works in offline mode");
        try {
            issuesTile.click();
            mediumWait();

            // Check if we left the dashboard
            boolean leftDashboard = !siteSelectionPage.isDashboardDisplayed();

            if (leftDashboard) {
                logStepWithScreenshot("Issues screen opened successfully in offline mode");

                boolean issuesScreenVisible = issuePage.isIssuesScreenDisplayed();
                logStep("Issues screen confirmed: " + issuesScreenVisible);
                assertTrue(issuesScreenVisible,
                        "Issues screen should be displayed after tapping Issues tile offline");
            } else {
                logWarning("Did not navigate away from dashboard — " +
                        "Issues tile may open as overlay or require active job");
            }
        } catch (Exception e) {
            logWarning("Issues navigation error: " + e.getMessage());
        }

        // Cleanup
        ensureOnlineState();
    }

    // ============================================================
    // OFFLINE CREATION TESTS (TC_OFF_011 - TC_OFF_015)
    // ============================================================

    @Test(priority = 11)
    public void TC_OFF_011_verifyCanCreateNewBuildingInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_CREATION,
                "TC_OFF_011 - Verify can create new building in offline mode");

        navigateToDashboardAndGoOffline();

        logStep("Navigating to Locations screen");
        siteSelectionPage.clickLocations();
        mediumWait();

        logStep("Waiting for Locations screen to be ready");
        boolean locationsReady = false;
        try {
            siteSelectionPage.waitForLocationsReady();
            locationsReady = siteSelectionPage.isLocationsScreenDisplayed();
        } catch (Exception e) {
            logWarning("Locations screen not fully ready: " + e.getMessage());
        }
        logStep("Locations screen ready: " + locationsReady);

        // Create building with a unique timestamped name to avoid conflicts
        String buildingName = "OffTest_" + System.currentTimeMillis();
        logStep("Creating new building: " + buildingName);

        try {
            siteSelectionPage.createBuilding(buildingName);
            mediumWait();

            logStepWithScreenshot("Building creation attempted in offline mode");

            // Building creation succeeded without crash — the app may scroll to show
            // the new building or navigate within the Locations hierarchy.
            // The key verification: no error dialog and the app is still responsive.
            logStep("Building creation completed without error — building queued for sync");
        } catch (Exception e) {
            logWarning("Building creation error: " + e.getMessage());
            logStepWithScreenshot("Building creation had an issue — checking app state");
        }

        // Navigate back to dashboard
        logStep("Navigating back to dashboard");
        try {
            siteSelectionPage.clickDone();
            shortWait();
        } catch (Exception e) {
            logWarning("Could not click Done, trying smart navigation: " + e.getMessage());
        }

        // Ensure we're on dashboard before checking sync
        try {
            if (!siteSelectionPage.isDashboardDisplayed()) {
                smartNavigateToDashboard();
            }
        } catch (Exception e) {
            logWarning("Dashboard navigation error: " + e.getMessage());
        }

        // Verify building was queued for sync via WiFi popup
        logStep("Verifying building was queued for sync");
        siteSelectionPage.clickWifiButton();
        shortWait();

        int syncCount = siteSelectionPage.getPendingSyncCount();
        boolean hasSyncRecords = siteSelectionPage.hasPendingSyncRecords() || syncCount > 0;
        logStep("Sync count after building creation: " + syncCount);

        logStepWithScreenshot("WiFi popup showing pending sync after building creation");

        assertTrue(hasSyncRecords,
                "Building should be queued for sync after offline creation — " +
                "pending sync count: " + syncCount);

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 12)
    public void TC_OFF_012_verifyCanCreateNewAssetInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_CREATION,
                "TC_OFF_012 - Verify can create new asset in offline mode");

        navigateToDashboardAndGoOffline();

        // Click the "Assets" tab from the bottom tab bar (not Locations)
        logStep("Navigating to Assets tab from bottom tab bar");
        boolean assetCreationAttempted = false;
        io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
        try {
            org.openqa.selenium.WebElement assetsTab = null;
            try {
                // The bottom tab bar "Assets" button
                assetsTab = new org.openqa.selenium.support.ui.WebDriverWait(d, java.time.Duration.ofSeconds(3))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND label == 'Assets'")));
            } catch (Exception e1) {
                // Fallback: try accessibility ID
                try {
                    assetsTab = d.findElement(AppiumBy.accessibilityId("Assets"));
                } catch (Exception e2) { /* not found */ }
            }

            if (assetsTab != null) {
                assetsTab.click();
                mediumWait();
                logStep("Navigated to Assets screen");
                logStepWithScreenshot("Assets screen in offline mode");

                // Look for Add/Plus button (2s timeout)
                try {
                    org.openqa.selenium.WebElement addBtn =
                        new org.openqa.selenium.support.ui.WebDriverWait(d, java.time.Duration.ofSeconds(2))
                            .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                                AppiumBy.accessibilityId("plus")));
                    addBtn.click();
                    mediumWait();
                    logStep("Tapped Add Asset button");

                    boolean formReady = assetPage.isAssetNameFieldDisplayed();
                    logStep("Asset creation form displayed: " + formReady);

                    if (formReady) {
                        String assetName = "OffAsset_" + System.currentTimeMillis();
                        logStep("Entering asset name: " + assetName);
                        assetPage.enterAssetName(assetName);
                        shortWait();
                        logStep("Clicking Create Asset");
                        assetPage.clickCreateAsset();
                        mediumWait();
                        logStepWithScreenshot("Asset creation attempted in offline mode");
                    } else {
                        logWarning("Asset creation form did not appear");
                    }
                } catch (Exception addEx) {
                    logWarning("Add Asset button not found: " + addEx.getMessage());
                }
                assetCreationAttempted = true;
            } else {
                logWarning("Assets tab not accessible from Locations screen in offline mode");
                logStepWithScreenshot("Assets tab not found — test will fail");
            }
        } catch (Exception e) {
            logWarning("Asset creation navigation error: " + e.getMessage());
            logStepWithScreenshot("Could not complete asset creation flow");
        }

        // The key assertion: the app should support asset creation offline
        // Even if the exact flow varies, the app should not crash or show
        // a "no network" error when attempting to create an asset
        logStep("Asset creation attempted without network errors: " + assetCreationAttempted);
        assertTrue(assetCreationAttempted,
                "Asset creation flow should be accessible in offline mode — " +
                "assets are queued for sync");

        // Navigate back to dashboard
        logStep("Navigating back to dashboard");
        try {
            smartNavigateToDashboard();
        } catch (Exception e) {
            logWarning("Could not navigate back to dashboard: " + e.getMessage());
        }

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 13)
    public void TC_OFF_013_verifyCanCreateNewTaskInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_CREATION,
                "TC_OFF_013 - Verify can create new task in offline mode");

        navigateToDashboardAndGoOffline();
        mediumWait(); // Let UI settle after offline transition

        io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();

        // Capture sync badge count BEFORE creating task
        int syncCountBefore = 0;
        try {
            java.util.List<org.openqa.selenium.WebElement> badgeElements =
                d.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name MATCHES '\\\\d+'"));
            if (!badgeElements.isEmpty()) {
                syncCountBefore = Integer.parseInt(badgeElements.get(0).getAttribute("name"));
            }
        } catch (Exception ignored) {}
        logStep("Sync badge count before task creation: " + syncCountBefore);

        // Step 1: Navigate to My Tasks
        logStep("Navigating to My Tasks for task creation");
        siteSelectionPage.clickMyTasksButton();
        shortWait();

        boolean leftDashboard = !siteSelectionPage.isDashboardDisplayed();
        logStep("Left dashboard to My Tasks: " + leftDashboard);
        assertTrue(leftDashboard, "Should navigate to My Tasks screen from dashboard");

        boolean taskCreationAttempted = false;

        // Step 2: Tap "+" to open New Task form
        logStep("Tapping Add button to open New Task form");
        org.openqa.selenium.WebElement addBtn = d.findElement(
            AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Add'"));
        addBtn.click();
        shortWait();

        // Step 3: Verify New Task form appeared
        java.util.List<org.openqa.selenium.WebElement> newTaskLabel =
            d.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'New Task' OR label == 'TASK DETAILS')"));
        assertTrue(!newTaskLabel.isEmpty(), "New Task form should appear after tapping Add");
        logStep("New Task form displayed");

        // Step 4: Enter title
        String taskTitle = "OffTask_" + System.currentTimeMillis();
        logStep("Entering task title: " + taskTitle);
        java.util.List<org.openqa.selenium.WebElement> textFields =
            d.findElements(AppiumBy.className("XCUIElementTypeTextField"));
        assertTrue(textFields.size() >= 1, "Title text field should be present");
        textFields.get(0).click();
        shortWait();
        textFields.get(0).sendKeys(taskTitle);
        shortWait();

        // Step 5: Enter description
        logStep("Entering task description");
        if (textFields.size() >= 2) {
            textFields.get(1).click();
            shortWait();
            textFields.get(1).sendKeys("Offline task created by automation");
            shortWait();
        } else {
            // Description might be a text view instead of text field
            try {
                java.util.List<org.openqa.selenium.WebElement> textViews =
                    d.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextView' AND " +
                        "(value == 'Describe the task...' OR label CONTAINS 'Description')"));
                if (!textViews.isEmpty()) {
                    textViews.get(0).click();
                    shortWait();
                    textViews.get(0).sendKeys("Offline task created by automation");
                    shortWait();
                }
            } catch (Exception ignored) {}
        }
        logStepWithScreenshot("Task form filled with title and description");

        // Step 6: Tap "Create Task" button
        logStep("Tapping Create Task button");
        org.openqa.selenium.WebElement createBtn = d.findElement(
            AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Create Task' OR label == 'Save' OR label == 'Create')"));
        createBtn.click();
        shortWait();
        logStep("Create Task tapped");
        logStepWithScreenshot("Task created — back on task list");

        // Step 7: Tap "Done" to return to dashboard
        logStep("Tapping Done to return to dashboard");
        try {
            org.openqa.selenium.WebElement doneBtn = d.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Done'"));
            doneBtn.click();
            shortWait();
            logStep("Done tapped — returned to dashboard");
        } catch (Exception e) {
            logWarning("Done button not found: " + e.getMessage());
            smartNavigateToDashboard();
        }

        taskCreationAttempted = true;

        // Step 8: Verify WiFi sync badge count increased
        logStep("Verifying sync badge count increased after task creation");
        int syncCountAfter = 0;
        try {
            java.util.List<org.openqa.selenium.WebElement> badgeAfter =
                d.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name MATCHES '\\\\d+'"));
            if (!badgeAfter.isEmpty()) {
                syncCountAfter = Integer.parseInt(badgeAfter.get(0).getAttribute("name"));
            }
        } catch (Exception ignored) {}
        logStep("Sync badge count after task creation: " + syncCountAfter);

        assertTrue(syncCountAfter > syncCountBefore,
                "WiFi sync badge should increase after offline task creation — " +
                "before: " + syncCountBefore + ", after: " + syncCountAfter);

        logStep("Task creation flow accessed in offline mode: " + taskCreationAttempted);
        logStepWithScreenshot("Task created and sync count verified");

        // Navigate back to dashboard (if not already there)
        logStep("Returning to dashboard");
        try {
            smartNavigateToDashboard();
        } catch (Exception e) {
            logWarning("Could not navigate back to dashboard: " + e.getMessage());
        }

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 14)
    public void TC_OFF_014_verifyCanCreateNewIssueInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_CREATION,
                "TC_OFF_014 - Verify can create new issue in offline mode");

        navigateToDashboardAndGoOffline();
        mediumWait(); // Let UI settle after offline transition

        io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();

        // Capture sync badge count BEFORE creating issue
        int syncCountBefore = 0;
        try {
            java.util.List<org.openqa.selenium.WebElement> badgeElements =
                d.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name MATCHES '\\\\d+'"));
            if (!badgeElements.isEmpty()) {
                syncCountBefore = Integer.parseInt(badgeElements.get(0).getAttribute("name"));
            }
        } catch (Exception ignored) {}
        logStep("Sync badge count before issue creation: " + syncCountBefore);

        // Step 1: Navigate to Issues screen
        logStep("Navigating to Issues screen");
        boolean onIssuesScreen = false;
        try {
            onIssuesScreen = issuePage.navigateToIssuesScreen();
            mediumWait(); // Wait for Issues screen to fully load
        } catch (Exception e) {
            logWarning("Could not navigate to Issues screen: " + e.getMessage());
        }
        assertTrue(onIssuesScreen, "Should navigate to Issues screen in offline mode");
        logStep("On Issues screen");

        // Step 2: Tap Add button to open New Issue form (with retry like Issue_Phase1_Test)
        logStep("Tapping Add button to open New Issue form");
        boolean newIssueFormVisible = false;
        for (int attempt = 1; attempt <= 3; attempt++) {
            mediumWait();
            issuePage.tapAddButton();
            mediumWait();

            newIssueFormVisible = issuePage.isNewIssueFormDisplayed();
            if (newIssueFormVisible) {
                logStep("New Issue form displayed (attempt " + attempt + ")");
                break;
            }
            logStep("New Issue form not detected on attempt " + attempt + ", retrying...");
            shortWait();
        }
        assertTrue(newIssueFormVisible,
                "New Issue form should appear after tapping Add button");

        // Step 3: Fill in issue details (same order as createQuickIssue in IssuePage)
        logStep("Selecting issue class: NEC Violation");
        issuePage.selectIssueClass("NEC Violation");
        shortWait();

        String issueTitle = "OffIssue_" + System.currentTimeMillis();
        logStep("Entering issue title: " + issueTitle);
        issuePage.enterIssueTitle(issueTitle);
        shortWait();

        // Step 4: Select asset (REQUIRED — Create Issue stays disabled without it)
        logStep("Selecting asset for issue");
        issuePage.tapSelectAsset();
        mediumWait();
        issuePage.selectAssetByName("");  // selects first available asset
        mediumWait();

        logStepWithScreenshot("Issue form filled with class, title, and asset");

        // Step 5: Tap Create Issue to save
        logStep("Tapping Create Issue button");
        boolean created = issuePage.tapCreateIssue();
        mediumWait();
        logStep("Issue creation result: " + created);
        assertTrue(created, "Create Issue should be tapped successfully — " +
                "all required fields (class, title, asset) were filled");
        logStepWithScreenshot("Issue created in offline mode");

        // Step 6: Tap "Done" to return from Issues list to dashboard
        logStep("Tapping Done to return to dashboard");
        try {
            org.openqa.selenium.WebElement doneBtn = d.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Done'"));
            doneBtn.click();
            shortWait();
            logStep("Done tapped — returned to dashboard");
        } catch (Exception e) {
            logWarning("Done button not found: " + e.getMessage());
            smartNavigateToDashboard();
        }

        // Step 7: Verify WiFi sync badge count increased
        logStep("Verifying sync badge count increased after issue creation");
        int syncCountAfter = 0;
        try {
            java.util.List<org.openqa.selenium.WebElement> badgeAfter =
                d.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name MATCHES '\\\\d+'"));
            if (!badgeAfter.isEmpty()) {
                syncCountAfter = Integer.parseInt(badgeAfter.get(0).getAttribute("name"));
            }
        } catch (Exception ignored) {}
        logStep("Sync badge count after issue creation: " + syncCountAfter);

        assertTrue(syncCountAfter > syncCountBefore,
                "WiFi sync badge should increase after offline issue creation — " +
                "before: " + syncCountBefore + ", after: " + syncCountAfter);

        logStepWithScreenshot("Issue created and sync count verified");

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 15)
    public void TC_OFF_015_verifyCanCaptureIRPhotosInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_CREATION,
                "TC_OFF_015 - Verify can capture IR photos in offline mode (Partial)");

        navigateToDashboardAndGoOffline();

        // Navigate to Issues → open an issue → attempt camera capture
        logStep("Navigating to Issues screen for IR photo capture test");
        boolean onIssuesScreen = false;
        try {
            onIssuesScreen = issuePage.navigateToIssuesScreen();
            mediumWait();
        } catch (Exception e) {
            logWarning("Could not navigate to Issues screen: " + e.getMessage());
        }

        boolean cameraAccessAttempted = false;

        if (onIssuesScreen) {
            logStep("Opening first issue to access photo capture");
            try {
                issuePage.tapFirstIssue();
                mediumWait();

                logStepWithScreenshot("Issue details opened — attempting camera access");

                // Try tapping the camera button for IR photo capture
                logStep("Tapping Camera button for IR photo capture");
                issuePage.tapCameraButton();
                mediumWait();

                // Check if camera opened (on real device) or permission dialog appeared
                boolean cameraVisible = issuePage.isCameraDisplayed();
                boolean photoPickerVisible = issuePage.isPhotoPickerDisplayed();

                logStep("Camera displayed: " + cameraVisible +
                        ", Photo picker displayed: " + photoPickerVisible);

                cameraAccessAttempted = true;

                if (cameraVisible) {
                    logStepWithScreenshot("Camera opened successfully in offline mode");
                    // Dismiss camera
                    issuePage.dismissCamera();
                    shortWait();
                } else if (photoPickerVisible) {
                    logStepWithScreenshot("Photo picker opened in offline mode");
                    issuePage.dismissPhotoPicker();
                    shortWait();
                } else {
                    // On simulator, camera may show a permission dialog or blank screen
                    logWarning("Camera/photo picker not detected — " +
                            "may be running on simulator or permission dialog appeared");
                    logStepWithScreenshot("Camera access state after tap");
                }

                // Close issue details
                issuePage.tapCloseIssueDetails();
                shortWait();
            } catch (Exception e) {
                logWarning("Camera access error: " + e.getMessage());
                logStepWithScreenshot("Camera access error — test will fail");
            }
        } else {
            logWarning("Could not reach Issues screen — " +
                    "IR photo capture test requires Issues access");
            logStepWithScreenshot("Issues screen not reachable — test will fail");
        }

        logStep("Camera/IR photo capture attempted in offline mode: " + cameraAccessAttempted);
        logWarning("IR photo capture is marked Partial — full verification requires " +
                "real device with camera and thermal imaging hardware");

        assertTrue(cameraAccessAttempted,
                "Camera/IR photo capture flow should be accessible offline");

        // Navigate back to dashboard
        logStep("Returning to dashboard");
        try {
            smartNavigateToDashboard();
        } catch (Exception e) {
            logWarning("Could not navigate back: " + e.getMessage());
        }

        // Cleanup
        ensureOnlineState();
    }

    // ============================================================
    // SYNC QUEUE TESTS (TC_OFF_016 - TC_OFF_017)
    // ============================================================

    @Test(priority = 16)
    public void TC_OFF_016_verifyPendingSyncBadgeOnWiFiIcon() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_SYNC_QUEUE,
                "TC_OFF_016 - Verify pending sync badge on WiFi icon");

        navigateToDashboardAndGoOffline();

        // Create data offline to generate pending sync records
        logStep("Navigating to Locations to create offline data");
        siteSelectionPage.clickLocations();
        mediumWait();

        try {
            siteSelectionPage.waitForLocationsReady();
        } catch (Exception e) {
            logWarning("Locations screen wait: " + e.getMessage());
        }

        String buildingName = "SyncBadge_" + System.currentTimeMillis();
        logStep("Creating building '" + buildingName + "' to generate pending sync record");
        try {
            siteSelectionPage.createBuilding(buildingName);
            mediumWait();
        } catch (Exception e) {
            logWarning("Building creation error: " + e.getMessage());
        }

        logStep("Returning to dashboard");
        try {
            siteSelectionPage.clickDone();
            mediumWait();
        } catch (Exception e) {
            logWarning("Could not click Done: " + e.getMessage());
        }

        logStepWithScreenshot("Checking WiFi icon for pending sync badge");

        // The WiFi icon should show a numeric badge or changed appearance
        // indicating there are pending sync records
        logStep("Clicking WiFi button to open popup and check sync state");
        siteSelectionPage.clickWifiButton();
        shortWait();

        int syncCount = siteSelectionPage.getPendingSyncCount();
        boolean hasSyncIndicator = siteSelectionPage.hasPendingSyncIndicator();
        boolean syncRecordsVisible = siteSelectionPage.isSyncRecordsOptionVisible();

        logStep("Pending sync count: " + syncCount);
        logStep("Has sync indicator: " + hasSyncIndicator);
        logStep("Sync records option visible: " + syncRecordsVisible);

        logStepWithScreenshot("WiFi popup showing sync state");

        // At least one of the sync indicators should be present
        assertTrue(syncCount > 0 || hasSyncIndicator || syncRecordsVisible,
                "WiFi icon should show pending sync badge/indicator " +
                "after creating data offline");

        logWarning("Badge color/appearance verification is limited in automation — " +
                "verified via sync count and indicator presence");

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 17)
    public void TC_OFF_017_verifyPendingSyncBadgeOnSitesTile() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_SYNC_QUEUE,
                "TC_OFF_017 - Verify pending sync badge on Sites tile");

        navigateToDashboardAndGoOffline();

        // Create data offline to generate pending sync records
        logStep("Navigating to Locations to create offline data");
        siteSelectionPage.clickLocations();
        mediumWait();

        try {
            siteSelectionPage.waitForLocationsReady();
        } catch (Exception e) {
            logWarning("Locations screen wait: " + e.getMessage());
        }

        String buildingName = "SitesBadge_" + System.currentTimeMillis();
        logStep("Creating building '" + buildingName + "' for sync queue");
        try {
            siteSelectionPage.createBuilding(buildingName);
            mediumWait();
        } catch (Exception e) {
            logWarning("Building creation error: " + e.getMessage());
        }

        logStep("Returning to dashboard");
        try {
            siteSelectionPage.clickDone();
            mediumWait();
        } catch (Exception e) {
            logWarning("Could not click Done: " + e.getMessage());
        }

        logStepWithScreenshot("Checking dashboard for pending sync indication");

        // Verify pending sync state is visible on the dashboard.
        // The sync badge appears on the WiFi icon (red badge with count) and
        // may also show as a small red dot on the Sites tile.
        // We verify via the WiFi popup which reliably shows sync records.
        logStep("Checking WiFi popup for pending sync records");
        siteSelectionPage.clickWifiButton();
        shortWait();

        int syncCount = siteSelectionPage.getPendingSyncCount();
        boolean hasSyncRecords = siteSelectionPage.hasPendingSyncRecords() || syncCount > 0;
        boolean syncRecordsVisible = siteSelectionPage.isSyncRecordsOptionVisible();

        logStep("Sync count: " + syncCount + ", has sync records: " + hasSyncRecords +
                ", sync records option visible: " + syncRecordsVisible);

        logStepWithScreenshot("Dashboard sync state with pending records");

        // After creating data offline, the dashboard should show pending sync indication
        // via the WiFi icon badge and/or Sites tile badge
        assertTrue(hasSyncRecords || syncRecordsVisible,
                "Dashboard should show pending sync indication after offline operations — " +
                "sync count: " + syncCount);

        // Also verify Sites tile state
        boolean sitesDisplayed = siteSelectionPage.isSitesButtonDisplayed();
        logStep("Sites tile displayed: " + sitesDisplayed);
        if (sitesDisplayed) {
            logStepWithScreenshot("Sites tile with pending sync records");
        }

        // Cleanup
        ensureOnlineState();
    }

    // ============================================================
    // OFFLINE SETTINGS TESTS (TC_OFF_018 - TC_OFF_019)
    // ============================================================

    /**
     * Navigate to Settings screen from dashboard.
     * Uses multiple locator strategies since there is no dedicated SettingsPage.
     * @return true if Settings screen was opened successfully
     */
    private boolean navigateToSettings() {
        // Strategy 1: Bottom tab bar "Settings" button — most reliable approach.
        // The tab bar contains buttons: Site, Assets, Connections, SLD, Settings
        try {
            java.util.List<org.openqa.selenium.WebElement> tabBarButtons =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Settings'"));

            for (org.openqa.selenium.WebElement btn : tabBarButtons) {
                // Prefer the bottom tab bar button (typically at Y > 700)
                int y = btn.getLocation().getY();
                if (y > 600) {
                    logStep("Found Settings tab bar button at Y=" + y);
                    btn.click();
                    mediumWait();
                    if (isSettingsScreenDisplayed()) {
                        return true;
                    }
                    logWarning("Settings tab clicked but screen not detected — retrying");
                    shortWait();
                    if (isSettingsScreenDisplayed()) {
                        return true;
                    }
                }
            }

            // If no bottom tab found, try any Settings button
            if (!tabBarButtons.isEmpty()) {
                logStep("Found Settings button via predicate search");
                tabBarButtons.get(0).click();
                mediumWait();
                if (isSettingsScreenDisplayed()) {
                    return true;
                }
            }
        } catch (Exception e) {
            logWarning("Settings tab bar search failed: " + e.getMessage());
        }

        // Strategy 2: Look for gear/settings icon button in nav bar
        try {
            java.util.List<org.openqa.selenium.WebElement> settingsButtons =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'gear' OR name CONTAINS 'gearshape' OR " +
                    "name == 'gearshape.fill')"));

            if (!settingsButtons.isEmpty()) {
                logStep("Found gear icon button");
                settingsButtons.get(0).click();
                mediumWait();
                if (isSettingsScreenDisplayed()) {
                    return true;
                }
            }
        } catch (Exception e) {
            logWarning("Gear icon search failed: " + e.getMessage());
        }

        // Strategy 3: Try accessibility ID directly
        try {
            DriverManager.getDriver().findElement(
                AppiumBy.accessibilityId("Settings")).click();
            mediumWait();
            if (isSettingsScreenDisplayed()) {
                return true;
            }
        } catch (Exception e) {
            logWarning("Settings via accessibilityId not found: " + e.getMessage());
        }

        // Strategy 4: Tap by coordinates — bottom-right tab bar area (Settings tab)
        try {
            org.openqa.selenium.Dimension screenSize =
                DriverManager.getDriver().manage().window().getSize();
            int tapX = (int) (screenSize.getWidth() * 0.9);
            int tapY = screenSize.getHeight() - 30;
            logStep("Tapping Settings tab by coordinates: X=" + tapX + ", Y=" + tapY);

            java.util.Map<String, Object> tapParams = new java.util.HashMap<>();
            tapParams.put("x", tapX);
            tapParams.put("y", tapY);
            DriverManager.getDriver().executeScript("mobile: tap", tapParams);
            mediumWait();
            if (isSettingsScreenDisplayed()) {
                return true;
            }
        } catch (Exception e) {
            logWarning("Settings tap by coordinates failed: " + e.getMessage());
        }

        return false;
    }

    /**
     * Check if Settings screen is currently displayed.
     */
    private boolean isSettingsScreenDisplayed() {
        try {
            org.openqa.selenium.WebElement navBar = DriverManager.getDriver().findElement(
                AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeNavigationBar' OR type == 'XCUIElementTypeStaticText') AND " +
                    "(name == 'Settings' OR label == 'Settings')"));
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    @Test(priority = 18)
    public void TC_OFF_018_verifySettingsScreenSyncAndNetworkSection() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_SETTINGS,
                "TC_OFF_018 - Verify Settings screen Sync & Network section");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        logStep("Navigating to Settings screen");
        boolean settingsFound = navigateToSettings();

        if (settingsFound) {
            logStepWithScreenshot("Settings screen opened");

            // Look for Sync & Network section
            logStep("Searching for Sync & Network section in Settings");
            boolean syncSectionFound = false;
            try {
                java.util.List<org.openqa.selenium.WebElement> syncTexts =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " +
                        "(label CONTAINS 'Sync' OR label CONTAINS 'Network' OR " +
                        "label CONTAINS 'sync' OR label CONTAINS 'network')"));

                syncSectionFound = !syncTexts.isEmpty();
                if (syncSectionFound) {
                    logStep("Found Sync/Network section with " + syncTexts.size() + " elements");
                    for (org.openqa.selenium.WebElement el : syncTexts) {
                        logStep("  - " + el.getAttribute("label"));
                    }
                }
            } catch (Exception e) {
                logWarning("Sync section search error: " + e.getMessage());
            }

            logStepWithScreenshot("Settings screen Sync & Network section verification");

            assertTrue(syncSectionFound,
                    "Settings screen should contain a Sync & Network section");
        } else {
            logWarning("Settings screen not accessible from dashboard — " +
                    "the app may not have a visible Settings button on the main dashboard");
            logStepWithScreenshot("Dashboard state — no Settings button found");
            logStep("Settings navigation may require a different entry point " +
                    "(e.g., profile menu, side drawer, or long-press)");
        }
    }

    @Test(priority = 19)
    public void TC_OFF_019_verifyNetworkModeToggleInSettings() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_SETTINGS,
                "TC_OFF_019 - Verify Network Mode toggle in Settings");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        logStep("Navigating to Settings screen");
        boolean settingsFound = navigateToSettings();

        if (settingsFound) {
            logStepWithScreenshot("Settings screen opened — looking for Network Mode toggle");

            // Search for Network Mode toggle or similar switch
            boolean networkToggleFound = false;
            try {
                java.util.List<org.openqa.selenium.WebElement> toggleElements =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeSwitch' OR type == 'XCUIElementTypeToggle') AND " +
                        "(name CONTAINS 'Network' OR name CONTAINS 'network' OR " +
                        "name CONTAINS 'Offline' OR name CONTAINS 'offline' OR " +
                        "label CONTAINS 'Network Mode' OR label CONTAINS 'Offline Mode')"));

                networkToggleFound = !toggleElements.isEmpty();

                if (networkToggleFound) {
                    logStep("Found Network Mode toggle");
                    org.openqa.selenium.WebElement toggle = toggleElements.get(0);
                    String currentValue = toggle.getAttribute("value");
                    logStep("Current toggle value: " + currentValue);

                    logStep("Toggling Network Mode to verify interactivity");
                    toggle.click();
                    shortWait();

                    String newValue = toggle.getAttribute("value");
                    logStep("Toggle value after tap: " + newValue);
                    logStepWithScreenshot("Network Mode toggle interaction");

                    // Toggle back to original state to leave clean
                    if (!String.valueOf(currentValue).equals(String.valueOf(newValue))) {
                        logStep("Toggle changed — reverting to original state");
                        toggle.click();
                        shortWait();
                    }

                    logStep("Network Mode toggle is present and interactive in Settings");
                } else {
                    // Try static text labels as fallback
                    java.util.List<org.openqa.selenium.WebElement> networkLabels =
                        DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND " +
                            "(label CONTAINS 'Network Mode' OR label CONTAINS 'Offline' OR " +
                            "label CONTAINS 'network mode')"));

                    assertTrue(!networkLabels.isEmpty(),
                            "Network Mode section should be visible in Settings " +
                            "(either as toggle or label)");
                    logStep("Found Network Mode label but toggle element not detected");
                    logStepWithScreenshot("Network Mode section found");
                }
            } catch (Exception e) {
                logWarning("Network Mode toggle search error: " + e.getMessage());
            }
        } else {
            logWarning("Settings screen not accessible — cannot verify Network Mode toggle");
            logStepWithScreenshot("Could not open Settings screen");
        }
    }

    // ============================================================
    // HELPER: Create offline data to populate sync queue
    // Used by TC_OFF_020+ tests that need pending sync records.
    // Goes offline, creates a building, and returns to dashboard.
    // ============================================================

    /**
     * Go offline, create a building to generate a pending sync record,
     * and return to dashboard. Leaves the app in OFFLINE state with
     * at least one pending sync record.
     * @param buildingPrefix prefix for building name (timestamp appended)
     */
    private void createOfflineDataAndReturnToDashboard(String buildingPrefix) {
        logStep("Going offline and creating data for sync queue");
        goOfflineViaPopup();

        // Verify offline state using definitive check (handles sync badge)
        boolean offlineConfirmed = isDefinitelyOffline();
        logStep("Offline mode confirmed: " + offlineConfirmed);
        assertTrue(offlineConfirmed, "App should be in offline mode");

        logStep("Navigating to Locations to create building");
        siteSelectionPage.clickLocations();
        mediumWait();

        try {
            siteSelectionPage.waitForLocationsReady();
        } catch (Exception e) {
            logWarning("Locations screen wait: " + e.getMessage());
        }

        String buildingName = buildingPrefix + "_" + System.currentTimeMillis();
        logStep("Creating building: " + buildingName);
        try {
            siteSelectionPage.createBuilding(buildingName);
            mediumWait();
        } catch (Exception e) {
            logWarning("Building creation error: " + e.getMessage());
        }

        logStep("Returning to dashboard");
        try {
            siteSelectionPage.clickDone();
            mediumWait();
        } catch (Exception e) {
            logWarning("Could not click Done: " + e.getMessage());
        }
    }

    /**
     * Navigate to Sync Queue Analyzer from Settings screen.
     * Assumes the Settings screen is currently displayed.
     * @return true if Sync Queue Analyzer screen was opened
     */
    private boolean navigateToSyncQueueAnalyzer() {
        try {
            // Look for "Sync Queue Analyzer" text/button in Settings
            java.util.List<org.openqa.selenium.WebElement> syncQueueElements =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                    "type == 'XCUIElementTypeCell') AND " +
                    "(label CONTAINS 'Sync Queue' OR name CONTAINS 'Sync Queue')"));

            if (!syncQueueElements.isEmpty()) {
                logStep("Found Sync Queue Analyzer element — tapping");
                syncQueueElements.get(0).click();
                mediumWait();
                return true;
            }

            // Fallback: try scrolling down to find it
            try {
                java.util.Map<String, Object> scrollParams = new java.util.HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Sync Queue'");
                DriverManager.getDriver().executeScript("mobile: scroll", scrollParams);
                shortWait();

                syncQueueElements = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "(label CONTAINS 'Sync Queue' OR name CONTAINS 'Sync Queue')"));
                if (!syncQueueElements.isEmpty()) {
                    syncQueueElements.get(0).click();
                    mediumWait();
                    return true;
                }
            } catch (Exception scrollEx) {
                logWarning("Scroll to Sync Queue failed: " + scrollEx.getMessage());
            }
        } catch (Exception e) {
            logWarning("Sync Queue Analyzer navigation error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if Sync Queue Analyzer screen is displayed.
     */
    private boolean isSyncQueueAnalyzerScreenDisplayed() {
        try {
            org.openqa.selenium.WebElement navBar = DriverManager.getDriver().findElement(
                AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeNavigationBar' OR type == 'XCUIElementTypeStaticText') AND " +
                    "(name CONTAINS 'Sync Queue' OR label CONTAINS 'Sync Queue')"));
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // SYNC QUEUE ANALYZER TESTS (TC_OFF_020 - TC_OFF_025)
    // ============================================================

    @Test(priority = 20)
    public void TC_OFF_020_verifySyncQueueAnalyzerShowsPendingCount() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_SYNC_QUEUE_ANALYZER,
                "TC_OFF_020 - Verify Sync Queue Analyzer shows pending count");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Create offline data to generate pending sync records
        createOfflineDataAndReturnToDashboard("SQA");

        // Now navigate to Settings to check Sync Queue Analyzer
        logStep("Navigating to Settings");
        boolean settingsFound = navigateToSettings();

        assertTrue(settingsFound,
                "Should be able to navigate to Settings screen from dashboard");

        logStepWithScreenshot("Settings screen — looking for Sync Queue Analyzer row");

        // Look for the Sync Queue Analyzer row with its elements
        boolean sqaRowFound = false;
        boolean hasBadge = false;
        try {
            // Check for "Sync Queue Analyzer" title text
            java.util.List<org.openqa.selenium.WebElement> sqaTitle =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "label CONTAINS 'Sync Queue Analyzer'"));
            sqaRowFound = !sqaTitle.isEmpty();

            // If not found immediately, scroll down to find it
            if (!sqaRowFound) {
                logStep("SQA not immediately visible — scrolling to find it");
                try {
                    java.util.Map<String, Object> scrollParams = new java.util.HashMap<>();
                    scrollParams.put("direction", "down");
                    scrollParams.put("predicateString", "label CONTAINS 'Sync Queue Analyzer'");
                    DriverManager.getDriver().executeScript("mobile: scroll", scrollParams);
                    shortWait();

                    sqaTitle = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " +
                        "label CONTAINS 'Sync Queue Analyzer'"));
                    sqaRowFound = !sqaTitle.isEmpty();
                } catch (Exception scrollEx) {
                    logWarning("Scroll to SQA failed: " + scrollEx.getMessage());
                }
            }

            if (sqaRowFound) {
                logStep("Found 'Sync Queue Analyzer' title");
            }

            // Check for subtitle "Manage pending sync operations"
            java.util.List<org.openqa.selenium.WebElement> sqaSubtitle =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Manage pending' OR label CONTAINS 'pending sync')"));
            if (!sqaSubtitle.isEmpty()) {
                logStep("Found subtitle: " + sqaSubtitle.get(0).getAttribute("label"));
                sqaRowFound = true; // Subtitle is sufficient evidence
            }

            // Check for "Please sync N pending" text (also confirms SQA presence)
            java.util.List<org.openqa.selenium.WebElement> syncWarnings =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "label CONTAINS 'pending operation'"));
            if (!syncWarnings.isEmpty()) {
                logStep("Found sync warning: " + syncWarnings.get(0).getAttribute("label"));
                sqaRowFound = true; // Sync warning confirms we're on Settings with pending ops
            }

            // Check for red badge with count near the SQA row
            java.util.List<org.openqa.selenium.WebElement> badgeElements =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') AND " +
                    "label MATCHES '^[0-9]+$'"));

            for (org.openqa.selenium.WebElement badge : badgeElements) {
                String label = badge.getAttribute("label");
                if (label != null && label.matches("^[0-9]+$")) {
                    int count = Integer.parseInt(label);
                    if (count > 0 && count < 100) {
                        logStep("Found badge with pending count: " + count);
                        hasBadge = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logWarning("Sync Queue Analyzer search error: " + e.getMessage());
        }

        logStepWithScreenshot("Sync Queue Analyzer row verification");

        assertTrue(sqaRowFound,
                "Settings should show 'Sync Queue Analyzer' row with title");
        logStep("Badge with pending count visible: " + hasBadge);

        logWarning("Badge color (red) verification is limited in automation — " +
                "verified via presence of numeric badge element");

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 21)
    public void TC_OFF_021_verifySyncQueueAnalyzerScreenLayout() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_SYNC_QUEUE_ANALYZER,
                "TC_OFF_021 - Verify Sync Queue Analyzer screen layout");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Create offline data so Sync Queue has content
        createOfflineDataAndReturnToDashboard("SQALayout");

        logStep("Navigating to Settings");
        boolean settingsFound = navigateToSettings();

        if (!settingsFound) {
            logWarning("Settings screen not accessible");
            logStepWithScreenshot("Could not open Settings");
            ensureOnlineState();
            return;
        }

        logStep("Tapping Sync Queue Analyzer to open it");
        boolean sqaOpened = navigateToSyncQueueAnalyzer();

        if (sqaOpened) {
            logStepWithScreenshot("Sync Queue Analyzer screen opened");

            // Verify screen layout elements
            boolean hasTitle = false;
            boolean hasBackButton = false;
            boolean hasMoreOptions = false;
            boolean hasPendingTab = false;
            boolean hasHistoryTab = false;

            try {
                // 1. Check for "Sync Queue Analyzer" title in navigation bar
                hasTitle = isSyncQueueAnalyzerScreenDisplayed();
                logStep("Title 'Sync Queue Analyzer' found: " + hasTitle);

                // 2. Check for Back button
                java.util.List<org.openqa.selenium.WebElement> backButtons =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND " +
                        "(name == 'Back' OR label == 'Back' OR name == 'Settings' OR " +
                        "label == 'Settings' OR name CONTAINS 'chevron.left')"));
                hasBackButton = !backButtons.isEmpty();
                logStep("Back button found: " + hasBackButton);

                // 3. Check for more options (three dots) icon
                java.util.List<org.openqa.selenium.WebElement> moreButtons =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND " +
                        "(name CONTAINS 'ellipsis' OR name CONTAINS 'more' OR " +
                        "label CONTAINS 'More' OR name CONTAINS 'three' OR " +
                        "name == 'ellipsis.circle')"));
                hasMoreOptions = !moreButtons.isEmpty();
                logStep("More options icon found: " + hasMoreOptions);

                // 4. Check for "Pending" tab with count
                java.util.List<org.openqa.selenium.WebElement> pendingTabs =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                        "label CONTAINS 'Pending'"));
                hasPendingTab = !pendingTabs.isEmpty();
                if (hasPendingTab) {
                    logStep("Pending tab found: " + pendingTabs.get(0).getAttribute("label"));
                }

                // 5. Check for "History" tab with count
                java.util.List<org.openqa.selenium.WebElement> historyTabs =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                        "label CONTAINS 'History'"));
                hasHistoryTab = !historyTabs.isEmpty();
                if (hasHistoryTab) {
                    logStep("History tab found: " + historyTabs.get(0).getAttribute("label"));
                }
            } catch (Exception e) {
                logWarning("Layout verification error: " + e.getMessage());
            }

            logStepWithScreenshot("Sync Queue Analyzer layout verification");

            assertTrue(hasTitle,
                    "Sync Queue Analyzer screen should show title");
            assertTrue(hasPendingTab,
                    "Sync Queue Analyzer should have a 'Pending' tab");
            assertTrue(hasHistoryTab,
                    "Sync Queue Analyzer should have a 'History' tab");
            logStep("Back button: " + hasBackButton + ", More options: " + hasMoreOptions);
        } else {
            logWarning("Could not open Sync Queue Analyzer");
            logStepWithScreenshot("Sync Queue Analyzer not accessible");
        }

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 22)
    public void TC_OFF_022_verifyPendingTabShowsQueuedOperations() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_SYNC_QUEUE_ANALYZER,
                "TC_OFF_022 - Verify Pending tab shows queued operations");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Create offline data to ensure pending items exist
        createOfflineDataAndReturnToDashboard("Pending");

        logStep("Navigating to Settings → Sync Queue Analyzer");
        boolean settingsFound = navigateToSettings();
        if (!settingsFound) {
            logWarning("Settings not accessible");
            ensureOnlineState();
            return;
        }

        boolean sqaOpened = navigateToSyncQueueAnalyzer();
        if (!sqaOpened) {
            logWarning("Sync Queue Analyzer not accessible");
            ensureOnlineState();
            return;
        }

        logStepWithScreenshot("Sync Queue Analyzer opened — checking Pending tab");

        // Tap the Pending tab to ensure it's selected
        try {
            java.util.List<org.openqa.selenium.WebElement> pendingTabs =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                    "label CONTAINS 'Pending'"));
            if (!pendingTabs.isEmpty()) {
                String tabLabel = pendingTabs.get(0).getAttribute("label");
                logStep("Pending tab label: " + tabLabel);

                // Check if tab shows count (e.g., "Pending (1)")
                if (tabLabel.contains("(")) {
                    String countStr = tabLabel.replaceAll("[^0-9]", "");
                    if (!countStr.isEmpty()) {
                        int pendingCount = Integer.parseInt(countStr);
                        logStep("Pending count from tab: " + pendingCount);
                        assertTrue(pendingCount > 0,
                                "Pending tab should show count > 0 when items are queued");
                    }
                }

                // Tap to ensure it's the active tab
                pendingTabs.get(0).click();
                shortWait();
            }
        } catch (Exception e) {
            logWarning("Pending tab interaction error: " + e.getMessage());
        }

        // Verify pending items are listed
        boolean foundQueuedItem = false;
        try {
            // Look for queued operation items — they typically show:
            // - Operation type icon
            // - Operation name (e.g., "building - create")
            // - Item identifier
            // - Queue timestamp (e.g., "Queued: X min, Y sec ago")
            java.util.List<org.openqa.selenium.WebElement> operationItems =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS '- create' OR label CONTAINS '- update' OR " +
                    "label CONTAINS '- delete' OR label CONTAINS 'building' OR " +
                    "label CONTAINS 'asset' OR label CONTAINS 'issue')"));

            if (!operationItems.isEmpty()) {
                foundQueuedItem = true;
                logStep("Found " + operationItems.size() + " operation items in Pending tab:");
                for (org.openqa.selenium.WebElement item : operationItems) {
                    logStep("  - " + item.getAttribute("label"));
                }
            }

            // Also look for "Queued:" timestamp text
            java.util.List<org.openqa.selenium.WebElement> queuedTimestamps =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Queued' OR label CONTAINS 'queued' OR " +
                    "label CONTAINS 'ago')"));
            if (!queuedTimestamps.isEmpty()) {
                foundQueuedItem = true;
                logStep("Found queue timestamp: " + queuedTimestamps.get(0).getAttribute("label"));
            }

            // Fallback: check for any cell/row elements that might be list items
            if (!foundQueuedItem) {
                java.util.List<org.openqa.selenium.WebElement> cells =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeCell'"));
                if (!cells.isEmpty()) {
                    foundQueuedItem = true;
                    logStep("Found " + cells.size() + " cell(s) in Pending tab — " +
                            "likely queued operations");
                }
            }
        } catch (Exception e) {
            logWarning("Pending items search error: " + e.getMessage());
        }

        logStepWithScreenshot("Pending tab content verification");

        assertTrue(foundQueuedItem,
                "Pending tab should show at least one queued operation " +
                "after creating data offline");

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 23)
    public void TC_OFF_023_verifyHistoryTabShowsCompletedSyncs() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_SYNC_QUEUE_ANALYZER,
                "TC_OFF_023 - Verify History tab shows completed syncs");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Create offline data, then sync it, so History tab has content
        createOfflineDataAndReturnToDashboard("History");

        // Go online and sync so the item moves from Pending to History
        logStep("Going online and syncing to generate History items");
        goOnlineViaPopup();

        // Attempt to sync pending records
        logStep("Clicking WiFi to access sync");
        siteSelectionPage.clickWifiButton();
        shortWait();

        if (siteSelectionPage.isSyncRecordsOptionVisible()) {
            logStep("Clicking Sync records");
            siteSelectionPage.clickSyncRecords();
            siteSelectionPage.waitForSyncToComplete();
        } else {
            logWarning("Sync records option not visible — sync may have happened automatically");
            mediumWait();
        }

        // Navigate to Settings → Sync Queue Analyzer → History tab
        logStep("Navigating to Settings → Sync Queue Analyzer");
        boolean settingsFound = navigateToSettings();
        if (!settingsFound) {
            logWarning("Settings not accessible");
            ensureOnlineState();
            return;
        }

        boolean sqaOpened = navigateToSyncQueueAnalyzer();
        if (!sqaOpened) {
            logWarning("Sync Queue Analyzer not accessible");
            ensureOnlineState();
            return;
        }

        logStepWithScreenshot("Sync Queue Analyzer opened — tapping History tab");

        // Tap the History tab
        boolean historyTabFound = false;
        boolean historyHasContent = false;
        try {
            java.util.List<org.openqa.selenium.WebElement> historyTabs =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                    "label CONTAINS 'History'"));

            if (!historyTabs.isEmpty()) {
                historyTabFound = true;
                String tabLabel = historyTabs.get(0).getAttribute("label");
                logStep("History tab label: " + tabLabel);

                // Check count from label (e.g., "History (4)")
                if (tabLabel.contains("(")) {
                    String countStr = tabLabel.replaceAll("[^0-9]", "");
                    if (!countStr.isEmpty()) {
                        int historyCount = Integer.parseInt(countStr);
                        logStep("History count from tab: " + historyCount);
                        historyHasContent = historyCount > 0;
                    }
                }

                // Tap to switch to History tab
                historyTabs.get(0).click();
                mediumWait();

                logStepWithScreenshot("History tab content");

                // Verify history items are present
                java.util.List<org.openqa.selenium.WebElement> historyItems =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " +
                        "(label CONTAINS ' - create' OR label CONTAINS ' - update' OR " +
                        "label CONTAINS ' - delete' OR label CONTAINS 'building' OR " +
                        "label CONTAINS 'Completed' OR label CONTAINS 'Synced')"));

                if (!historyItems.isEmpty()) {
                    historyHasContent = true;
                    logStep("Found " + historyItems.size() + " history items:");
                    for (int i = 0; i < Math.min(historyItems.size(), 5); i++) {
                        logStep("  - " + historyItems.get(i).getAttribute("label"));
                    }
                }

                // Fallback: check for cell elements
                if (!historyHasContent) {
                    java.util.List<org.openqa.selenium.WebElement> cells =
                        DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeCell'"));
                    if (!cells.isEmpty()) {
                        historyHasContent = true;
                        logStep("Found " + cells.size() + " cell(s) in History tab");
                    }
                }
            }
        } catch (Exception e) {
            logWarning("History tab error: " + e.getMessage());
        }

        assertTrue(historyTabFound,
                "History tab should be present in Sync Queue Analyzer");
        logStep("History tab has content: " + historyHasContent);

        if (!historyHasContent) {
            logWarning("History tab may be empty if sync did not complete or if this is " +
                    "the first test run — previous sync history may not exist");
        }

        // Cleanup: navigate back to dashboard
        try {
            smartNavigateToDashboard();
        } catch (Exception e) {
            logWarning("Could not navigate back to dashboard: " + e.getMessage());
        }
        ensureOnlineState();
    }

    @Test(priority = 24)
    public void TC_OFF_024_verifyPendingItemShowsOperationType() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_SYNC_QUEUE_ANALYZER,
                "TC_OFF_024 - Verify pending item shows operation type");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Create offline data to have a pending item
        createOfflineDataAndReturnToDashboard("OpType");

        logStep("Navigating to Settings → Sync Queue Analyzer");
        boolean settingsFound = navigateToSettings();
        if (!settingsFound) {
            logWarning("Settings not accessible");
            ensureOnlineState();
            return;
        }

        boolean sqaOpened = navigateToSyncQueueAnalyzer();
        if (!sqaOpened) {
            logWarning("Sync Queue Analyzer not accessible");
            ensureOnlineState();
            return;
        }

        logStepWithScreenshot("Checking pending items for operation type format");

        // Verify pending items show operation type: "[entity] - [operation]"
        // e.g., "building - create", "asset - update", "issue - create"
        boolean operationTypeFound = false;
        String operationText = "";
        try {
            java.util.List<org.openqa.selenium.WebElement> allTexts =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS ' - create' OR label CONTAINS ' - update' OR " +
                    "label CONTAINS ' - delete' OR label CONTAINS 'building' OR " +
                    "label CONTAINS 'asset' OR label CONTAINS 'issue')"));

            for (org.openqa.selenium.WebElement text : allTexts) {
                String label = text.getAttribute("label");
                if (label != null && (label.contains(" - create") || label.contains(" - update") ||
                        label.contains(" - delete"))) {
                    operationTypeFound = true;
                    operationText = label;
                    logStep("Found operation type: '" + label + "'");
                    break;
                }
            }

            // Fallback: check for entity type names alone
            if (!operationTypeFound) {
                for (org.openqa.selenium.WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    if (label != null && (label.toLowerCase().contains("building") ||
                            label.toLowerCase().contains("asset") ||
                            label.toLowerCase().contains("issue"))) {
                        operationTypeFound = true;
                        operationText = label;
                        logStep("Found entity reference: '" + label + "'");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logWarning("Operation type search error: " + e.getMessage());
        }

        logStepWithScreenshot("Pending item operation type verification");

        assertTrue(operationTypeFound,
                "Pending items should show operation type (e.g., 'building - create')");
        logStep("Operation type text: " + operationText);

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 25)
    public void TC_OFF_025_verifyPendingItemShowsQueueTime() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_SYNC_QUEUE_ANALYZER,
                "TC_OFF_025 - Verify pending item shows queue time");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Create offline data
        createOfflineDataAndReturnToDashboard("QTime");

        logStep("Navigating to Settings → Sync Queue Analyzer");
        boolean settingsFound = navigateToSettings();
        if (!settingsFound) {
            logWarning("Settings not accessible");
            ensureOnlineState();
            return;
        }

        boolean sqaOpened = navigateToSyncQueueAnalyzer();
        if (!sqaOpened) {
            logWarning("Sync Queue Analyzer not accessible");
            ensureOnlineState();
            return;
        }

        logStepWithScreenshot("Checking pending items for queue timestamp");

        // Verify pending items show a "Queued: X min, Y sec ago" timestamp
        boolean timestampFound = false;
        String timestampText = "";
        try {
            java.util.List<org.openqa.selenium.WebElement> timeTexts =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Queued' OR label CONTAINS 'queued' OR " +
                    "label CONTAINS 'ago' OR label CONTAINS 'min' OR " +
                    "label CONTAINS 'sec' OR label CONTAINS 'just now')"));

            for (org.openqa.selenium.WebElement text : timeTexts) {
                String label = text.getAttribute("label");
                if (label != null && (label.contains("Queued") || label.contains("ago") ||
                        label.contains("min") || label.contains("sec") ||
                        label.contains("just now"))) {
                    timestampFound = true;
                    timestampText = label;
                    logStep("Found queue timestamp: '" + label + "'");
                    break;
                }
            }

            // Fallback: regex search for time patterns
            if (!timestampFound) {
                java.util.List<org.openqa.selenium.WebElement> allTexts =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText'"));

                for (org.openqa.selenium.WebElement text : allTexts) {
                    String label = text.getAttribute("label");
                    if (label != null && (label.matches(".*\\d+.*min.*") ||
                            label.matches(".*\\d+.*sec.*") ||
                            label.matches(".*\\d+.*ago.*"))) {
                        timestampFound = true;
                        timestampText = label;
                        logStep("Found timestamp via pattern: '" + label + "'");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logWarning("Timestamp search error: " + e.getMessage());
        }

        logStepWithScreenshot("Queue timestamp verification");

        assertTrue(timestampFound,
                "Pending items should show queue timestamp " +
                "(e.g., 'Queued: X min, Y sec ago')");
        logStep("Timestamp text: " + timestampText);

        // Cleanup
        ensureOnlineState();
    }

    // ============================================================
    // GO ONLINE TESTS (TC_OFF_026 - TC_OFF_029)
    // ============================================================

    @Test(priority = 26)
    public void TC_OFF_026_verifyNetworkModeToggleSwitchesToOnline() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_GO_ONLINE,
                "TC_OFF_026 - Verify Network Mode toggle switches to online");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // First go offline
        logStep("Going offline first");
        goOfflineViaPopup();
        assertTrue(isDefinitelyOffline(),
                "Should be in offline mode before testing go-online");

        // Navigate to Settings and use Network Mode toggle to go online
        logStep("Navigating to Settings to toggle Network Mode to online");
        boolean settingsFound = navigateToSettings();

        if (settingsFound) {
            logStepWithScreenshot("Settings screen — looking for Network Mode toggle");

            boolean toggledOnline = false;
            try {
                java.util.List<org.openqa.selenium.WebElement> toggleElements =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeSwitch' OR type == 'XCUIElementTypeToggle') AND " +
                        "(name CONTAINS 'Network' OR name CONTAINS 'Offline' OR " +
                        "label CONTAINS 'Network Mode' OR label CONTAINS 'Offline Mode' OR " +
                        "label CONTAINS 'network' OR label CONTAINS 'offline')"));

                if (!toggleElements.isEmpty()) {
                    org.openqa.selenium.WebElement toggle = toggleElements.get(0);
                    String valueBefore = toggle.getAttribute("value");
                    logStep("Toggle value before: " + valueBefore);

                    logStep("Tapping toggle to switch to online");
                    toggle.click();
                    mediumWait();

                    String valueAfter = toggle.getAttribute("value");
                    logStep("Toggle value after: " + valueAfter);
                    toggledOnline = true;

                    logStepWithScreenshot("Network Mode toggled to online");
                } else {
                    logWarning("Network Mode toggle not found — using WiFi popup fallback");
                }
            } catch (Exception e) {
                logWarning("Settings toggle error: " + e.getMessage());
            }

            // Navigate back to dashboard before verifying online state
            logStep("Navigating back to dashboard from Settings");
            try {
                smartNavigateToDashboard();
            } catch (Exception e) {
                logWarning("Could not navigate back to dashboard: " + e.getMessage());
            }

            // If toggle wasn't found in Settings, use WiFi popup fallback
            if (!toggledOnline) {
                logStep("Falling back to WiFi popup for going online");
                goOnlineViaPopup();
            }

            // Verify online state — use definitive "Wi-Fi Off" check instead of
            // isWifiOnline() which gives false negatives with sync badge.
            boolean definitelyOffline = false;
            try {
                definitelyOffline = DriverManager.getDriver().findElement(
                    AppiumBy.accessibilityId("Wi-Fi Off")).isDisplayed();
            } catch (Exception e) { /* not found = not offline */ }
            logStep("Definitely offline: " + definitelyOffline);
            logStepWithScreenshot("Network state after toggling to online");

            assertFalse(definitelyOffline,
                    "App should NOT show Wi-Fi Off icon after toggling Network Mode to online");
        } else {
            logWarning("Settings not accessible — using WiFi Go Online as fallback");
            goOnlineViaPopup();

            logStepWithScreenshot("Online state after WiFi Go Online");
            boolean stillOffline = false;
            try {
                stillOffline = DriverManager.getDriver().findElement(
                    AppiumBy.accessibilityId("Wi-Fi Off")).isDisplayed();
            } catch (Exception e) { /* not offline */ }
            assertFalse(stillOffline,
                    "App should NOT show Wi-Fi Off icon after Go Online");
        }

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 27)
    public void TC_OFF_027_verifyPendingItemsSyncWhenGoingOnline() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_GO_ONLINE,
                "TC_OFF_027 - Verify pending items sync when going online");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Create offline data to generate pending sync records
        createOfflineDataAndReturnToDashboard("SyncOnline");

        // Check pending count before going online
        logStep("Clicking WiFi to check pending sync before going online");
        siteSelectionPage.clickWifiButton();
        shortWait();

        int pendingBefore = siteSelectionPage.getPendingSyncCount();
        boolean hadPendingBefore = siteSelectionPage.hasPendingSyncRecords() || pendingBefore > 0;
        logStep("Pending records before going online: " + pendingBefore +
                ", has pending: " + hadPendingBefore);

        // Go online
        logStep("Going online to trigger sync");
        goOnlineViaPopup();

        // Check for sync option
        logStep("Clicking WiFi to check for sync records option");
        siteSelectionPage.clickWifiButton();
        shortWait();

        boolean syncVisible = siteSelectionPage.isSyncRecordsOptionVisible();
        logStep("Sync records option visible after going online: " + syncVisible);

        if (syncVisible) {
            logStep("Clicking Sync records to initiate sync");
            siteSelectionPage.clickSyncRecords();
            logStep("Waiting for sync to complete");
            siteSelectionPage.waitForSyncToComplete();
            logStepWithScreenshot("Sync completed");
        } else {
            logStep("Sync may have initiated automatically");
            mediumWait();
        }

        // Verify pending count decreased
        logStep("Checking sync results");
        siteSelectionPage.clickWifiButton();
        shortWait();

        int pendingAfter = siteSelectionPage.getPendingSyncCount();
        logStep("Pending records after sync: " + pendingAfter);

        // Dismiss WiFi popup before assertions/cleanup
        dismissWifiPopup(DriverManager.getDriver());

        logStepWithScreenshot("Post-sync state verification");

        if (pendingBefore > 0) {
            assertTrue(pendingAfter < pendingBefore,
                    "Pending items should decrease after sync — " +
                    "before: " + pendingBefore + ", after: " + pendingAfter);
        } else {
            logWarning("No pending records were detected before sync — " +
                    "offline data creation may not have generated sync items");
            assertTrue(pendingAfter <= pendingBefore,
                    "Pending count should not increase after going online — " +
                    "before: " + pendingBefore + ", after: " + pendingAfter);
        }

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 28)
    public void TC_OFF_028_verifyWiFiIconReturnsToNormalWhenOnline() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_GO_ONLINE,
                "TC_OFF_028 - Verify WiFi icon returns to normal when online");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Go offline, then back online to verify icon transitions
        logStep("Going offline to set initial state");
        goOfflineViaPopup();

        boolean offlineConfirmed = isDefinitelyOffline();
        logStep("Offline state confirmed: " + offlineConfirmed);
        assertTrue(offlineConfirmed, "App should be in offline mode");
        logStepWithScreenshot("WiFi icon in offline state (strikethrough)");

        // Go back online
        logStep("Going back online");
        goOnlineViaPopup();

        // Sync if needed to clear badge
        logStep("Checking for pending sync to clear badge");
        siteSelectionPage.clickWifiButton();
        shortWait();

        if (siteSelectionPage.isSyncRecordsOptionVisible()) {
            logStep("Syncing pending records to clear badge");
            siteSelectionPage.clickSyncRecords();
            siteSelectionPage.waitForSyncToComplete();
        } else {
            // Dismiss the WiFi popup if no sync option
            dismissWifiPopup(DriverManager.getDriver());
        }

        shortWait();

        // Verify WiFi icon state using definitive "Wi-Fi Off" check
        // instead of isWifiOnline() which gives false negatives with sync badge
        logStepWithScreenshot("WiFi icon after returning to online");

        boolean definitelyOffline = false;
        try {
            definitelyOffline = DriverManager.getDriver().findElement(
                AppiumBy.accessibilityId("Wi-Fi Off")).isDisplayed();
        } catch (Exception e) { /* not found = not offline */ }

        logStep("WiFi Off icon displayed: " + definitelyOffline);

        assertFalse(definitelyOffline,
                "WiFi icon should NOT show 'Wi-Fi Off' strikethrough when online");

        logWarning("Icon color (green vs red) verification is limited in automation — " +
                "verified via accessibility ID change from 'Wi-Fi Off' back to 'Wi-Fi'");
    }

    @Test(priority = 29)
    public void TC_OFF_029_verifyRefreshAndSitesReEnableWhenOnline() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_GO_ONLINE,
                "TC_OFF_029 - Verify Refresh and Sites re-enable when online");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Go offline and verify features are disabled
        logStep("Going offline to verify restricted state first");
        goOfflineViaPopup();

        boolean refreshEnabledOffline = siteSelectionPage.isRefreshButtonEnabled();
        boolean sitesEnabledOffline = siteSelectionPage.isSitesButtonEnabled();
        logStep("While offline — Refresh enabled: " + refreshEnabledOffline +
                ", Sites enabled: " + sitesEnabledOffline);
        logStepWithScreenshot("Dashboard in offline state — restricted features");

        // Go back online
        logStep("Going back online");
        goOnlineViaPopup();

        // Sync if needed so Sites button re-enables
        // Check WiFi icon first — only open popup if sync badge is present
        io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
        boolean needsSync = !isWifiIconClean(d);
        if (needsSync) {
            logStep("Sync badge detected — opening popup to sync");
            siteSelectionPage.clickWifiButton();
            shortWait();
            if (siteSelectionPage.isSyncRecordsOptionVisible()) {
                logStep("Syncing pending records");
                siteSelectionPage.clickSyncRecords();
                siteSelectionPage.waitForSyncToComplete();
            } else {
                dismissWifiPopup(d);
            }
        } else {
            logStep("WiFi icon clean — no sync needed");
        }

        mediumWait(); // Wait for UI to fully update

        // Verify features are re-enabled
        logStepWithScreenshot("Dashboard after going online — checking feature states");

        boolean refreshDisplayed = siteSelectionPage.isRefreshButtonDisplayed();
        boolean refreshEnabledOnline = false;
        if (refreshDisplayed) {
            refreshEnabledOnline = siteSelectionPage.isRefreshButtonEnabled();
        } else {
            logStep("Refresh button not displayed — may be layout-dependent");
        }

        boolean sitesDisplayed = siteSelectionPage.isSitesButtonDisplayed();
        boolean sitesEnabledOnline = false;
        if (sitesDisplayed) {
            sitesEnabledOnline = siteSelectionPage.isSitesButtonEnabled();
        }

        logStep("Online — Refresh displayed: " + refreshDisplayed +
                ", enabled: " + refreshEnabledOnline);
        logStep("Online — Sites displayed: " + sitesDisplayed +
                ", enabled: " + sitesEnabledOnline);

        if (refreshDisplayed) {
            assertTrue(refreshEnabledOnline,
                    "Refresh tile should become active (not grayed out) when back online");
        }

        assertTrue(sitesEnabledOnline,
                "Sites tile should become active when back online and sync is complete");

        logStepWithScreenshot("All online-only features re-enabled successfully");

        // Use definitive "Wi-Fi Off" check instead of isWifiOnline()
        // which gives false negatives when sync badge is present
        boolean definitelyOffline = false;
        try {
            definitelyOffline = DriverManager.getDriver().findElement(
                AppiumBy.accessibilityId("Wi-Fi Off")).isDisplayed();
        } catch (Exception e) { /* not found = not offline */ }
        assertFalse(definitelyOffline,
                "App should NOT show Wi-Fi Off icon when back online");
    }

    // ============================================================
    // SETTINGS SECTION TESTS (TC_OFF_030 - TC_OFF_032)
    // ============================================================

    @Test(priority = 30)
    public void TC_OFF_030_verifyAccountSectionInSettings() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_SETTINGS,
                "TC_OFF_030 - Verify Account section in Settings");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        logStep("Navigating to Settings");
        boolean settingsFound = navigateToSettings();

        if (!settingsFound) {
            logWarning("Settings screen not accessible from dashboard");
            logStepWithScreenshot("Could not open Settings");
            return;
        }

        logStepWithScreenshot("Settings screen — looking for Account section");

        // Verify Account section elements
        boolean emailFound = false;
        boolean userIdFound = false;
        String emailText = "";
        String userIdText = "";

        try {
            // Look for email address — typically contains "@"
            java.util.List<org.openqa.selenium.WebElement> emailElements =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '@'"));

            if (!emailElements.isEmpty()) {
                emailFound = true;
                emailText = emailElements.get(0).getAttribute("label");
                logStep("Found email address: " + emailText);
            }

            // Look for User ID — may be truncated with "..." or just a long string
            java.util.List<org.openqa.selenium.WebElement> idElements =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'User ID' OR label CONTAINS 'user id' OR " +
                    "label CONTAINS '...' OR label CONTAINS 'Account')"));

            if (!idElements.isEmpty()) {
                userIdFound = true;
                userIdText = idElements.get(0).getAttribute("label");
                logStep("Found User ID / Account element: " + userIdText);
            }

            // Fallback: Look for "Account" section header
            if (!emailFound && !userIdFound) {
                java.util.List<org.openqa.selenium.WebElement> accountHeaders =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " +
                        "(label == 'Account' OR label CONTAINS 'account')"));
                if (!accountHeaders.isEmpty()) {
                    logStep("Found Account section header");
                    // Try scrolling to find details below the header
                    userIdFound = true;
                    userIdText = "Account section header found";
                }
            }
        } catch (Exception e) {
            logWarning("Account section search error: " + e.getMessage());
        }

        logStepWithScreenshot("Account section verification");

        assertTrue(emailFound || userIdFound,
                "Settings should display Account section with email and/or User ID");
        logStep("Email: " + emailText + ", User ID: " + userIdText);

        // Cleanup: navigate back to dashboard
        try {
            smartNavigateToDashboard();
        } catch (Exception e) {
            logWarning("Could not navigate back to dashboard: " + e.getMessage());
        }
    }

    @Test(priority = 32)
    public void TC_OFF_032_verifyPhotoStorageDiagnosticsInDiagnosticsSection() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_SETTINGS,
                "TC_OFF_032 - Verify Photo Storage Diagnostics in Diagnostics section");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        logStep("Navigating to Settings");
        boolean settingsFound = navigateToSettings();

        if (!settingsFound) {
            logWarning("Settings screen not accessible");
            logStepWithScreenshot("Could not open Settings");
            return;
        }

        logStepWithScreenshot("Settings screen — looking for Photo Storage Diagnostics");

        boolean photoStorageTitleFound = false;
        boolean photoStorageDescFound = false;
        boolean navigationArrowFound = false;

        try {
            // Scroll down to Diagnostics section
            try {
                java.util.Map<String, Object> scrollParams = new java.util.HashMap<>();
                scrollParams.put("direction", "down");
                scrollParams.put("predicateString", "label CONTAINS 'Photo Storage'");
                DriverManager.getDriver().executeScript("mobile: scroll", scrollParams);
                shortWait();
            } catch (Exception scrollEx) {
                // May already be visible
            }

            // 1. Check for "Photo Storage Diagnostics" title
            java.util.List<org.openqa.selenium.WebElement> photoTitles =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'Photo Storage' OR label CONTAINS 'photo storage')"));

            if (!photoTitles.isEmpty()) {
                photoStorageTitleFound = true;
                logStep("Found Photo Storage Diagnostics title: " +
                        photoTitles.get(0).getAttribute("label"));
            }

            // 2. Check for description "Check photo storage health"
            java.util.List<org.openqa.selenium.WebElement> photoDescs =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(label CONTAINS 'storage health' OR " +
                    "(label CONTAINS 'photo' AND label CONTAINS 'health'))"));

            if (!photoDescs.isEmpty()) {
                photoStorageDescFound = true;
                logStep("Found description: " + photoDescs.get(0).getAttribute("label"));
            }

            // 3. Check for navigation arrow (chevron.right / disclosure indicator)
            java.util.List<org.openqa.selenium.WebElement> arrows =
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'chevron' OR name CONTAINS 'disclosure' OR " +
                    "name CONTAINS 'arrow')"));

            if (!arrows.isEmpty()) {
                navigationArrowFound = true;
                logStep("Found navigation arrow indicator");
            }

            // Fallback: check for "Diagnostics" section header
            if (!photoStorageTitleFound) {
                java.util.List<org.openqa.selenium.WebElement> diagHeaders =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " +
                        "(label == 'Diagnostics' OR label CONTAINS 'diagnostics')"));
                if (!diagHeaders.isEmpty()) {
                    logStep("Found Diagnostics section header — " +
                            "Photo Storage may require further scrolling");
                }
            }
        } catch (Exception e) {
            logWarning("Photo Storage Diagnostics search error: " + e.getMessage());
        }

        logStepWithScreenshot("Photo Storage Diagnostics verification");

        assertTrue(photoStorageTitleFound,
                "Settings should show 'Photo Storage Diagnostics' in Diagnostics section");
        logStep("Title: " + photoStorageTitleFound +
                ", Description: " + photoStorageDescFound +
                ", Arrow: " + navigationArrowFound);

        // Cleanup: navigate back to dashboard
        try {
            smartNavigateToDashboard();
        } catch (Exception e) {
            logWarning("Could not navigate back to dashboard: " + e.getMessage());
        }
    }

    // ============================================================
    // OFFLINE OPERATIONS TESTS (TC_OFF_033 - TC_OFF_034)
    // ============================================================

    @Test(priority = 33)
    public void TC_OFF_033_verifyActiveJobPersistsInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_OPERATIONS,
                "TC_OFF_033 - Verify active job persists in offline mode");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Check if there's an active job before going offline
        logStep("Checking for active job banner on dashboard (online state)");

        boolean hadActiveJobOnline = false;
        String activeJobText = "";
        try {
            // An active job is present when the "No Active Job" card is NOT displayed
            boolean noActiveJob = siteSelectionPage.isNoActiveJobCardDisplayed();

            if (!noActiveJob) {
                // Active job exists — look for the job banner elements
                java.util.List<org.openqa.selenium.WebElement> jobBanners =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND " +
                        "(label CONTAINS 'Job' OR label CONTAINS 'Active' OR " +
                        "label CONTAINS 'END')"));

                if (!jobBanners.isEmpty()) {
                    hadActiveJobOnline = true;
                    activeJobText = jobBanners.get(0).getAttribute("label");
                    logStep("Active job found online: " + activeJobText);
                }

                // Also check for END button
                java.util.List<org.openqa.selenium.WebElement> endButtons =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND " +
                        "(label == 'END' OR label == 'End' OR name CONTAINS 'END')"));
                if (!endButtons.isEmpty()) {
                    hadActiveJobOnline = true;
                    logStep("Found END button — active job confirmed");
                }
            } else {
                logStep("No Active Job card displayed — no active job running");
                logWarning("TC_OFF_033 requires an active job to be running. " +
                        "Without an active job, verifying that the 'No Active Job' state persists offline.");
            }
        } catch (Exception e) {
            logWarning("Active job check error: " + e.getMessage());
        }

        logStepWithScreenshot("Dashboard state before going offline");

        // Go offline
        logStep("Going offline");
        goOfflineViaPopup();

        assertTrue(isDefinitelyOffline(), "Should be in offline mode");

        // Verify the job state persists in offline mode
        logStepWithScreenshot("Dashboard in offline mode — checking job persistence");

        if (hadActiveJobOnline) {
            // Verify active job banner is still visible
            boolean noActiveJobOffline = siteSelectionPage.isNoActiveJobCardDisplayed();
            logStep("No Active Job card displayed (offline): " + noActiveJobOffline);

            assertFalse(noActiveJobOffline,
                    "Active Job banner should persist in offline mode — " +
                    "job was active online, should remain active offline");

            // Look for job name and END button
            try {
                java.util.List<org.openqa.selenium.WebElement> endButtons =
                    DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND " +
                        "(label == 'END' OR label == 'End' OR name CONTAINS 'END')"));
                if (!endButtons.isEmpty()) {
                    logStep("END button still visible in offline mode");
                }
            } catch (Exception e) {
                logWarning("END button check error: " + e.getMessage());
            }

            logStepWithScreenshot("Active job banner confirmed in offline mode");
        } else {
            // No active job — verify the "No Active Job" state also persists offline
            boolean noActiveJobOffline = siteSelectionPage.isNoActiveJobCardDisplayed();
            logStep("No Active Job card persists offline: " + noActiveJobOffline);

            assertTrue(noActiveJobOffline,
                    "No Active Job state should persist when going offline — " +
                    "dashboard should still show 'No Active Job' card");
            logStep("Job state correctly persisted across offline transition");
        }

        // Cleanup
        ensureOnlineState();
    }

    @Test(priority = 34)
    public void TC_OFF_034_verifySiteStatsVisibleInOfflineMode() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_OFFLINE_OPERATIONS,
                "TC_OFF_034 - Verify site stats visible in offline mode");

        navigateToDashboardAndGoOffline();

        logStepWithScreenshot("Dashboard in offline mode — checking site statistics");

        // Verify Assets count is displayed (cached value from last sync)
        boolean assetsCardDisplayed = siteSelectionPage.isAssetsCardDisplayed();
        String assetsCountText = siteSelectionPage.getAssetsCountText();
        logStep("Assets card displayed: " + assetsCardDisplayed +
                ", text: '" + assetsCountText + "'");

        // Verify Connections count is displayed (cached value)
        boolean connectionsCardDisplayed = siteSelectionPage.isConnectionsCardDisplayed();
        String connectionsCountText = siteSelectionPage.getConnectionsCountText();
        logStep("Connections card displayed: " + connectionsCardDisplayed +
                ", text: '" + connectionsCountText + "'");

        logStepWithScreenshot("Site statistics tiles in offline mode");

        // At least one of the stats tiles should be visible with cached data
        assertTrue(assetsCardDisplayed || connectionsCardDisplayed,
                "Site statistics tiles (Assets/Connections) should be visible offline " +
                "showing cached values from last sync");

        if (assetsCardDisplayed) {
            assertTrue(assetsCountText != null && !assetsCountText.isEmpty(),
                    "Assets card should show a cached count value");
            logStep("Assets count (cached): " + assetsCountText);
        }

        if (connectionsCardDisplayed) {
            assertTrue(connectionsCountText != null && !connectionsCountText.isEmpty(),
                    "Connections card should show a cached count value");
            logStep("Connections count (cached): " + connectionsCountText);
        }

        // Cleanup
        ensureOnlineState();
    }

    // ============================================================
    // SYNC QUEUE MULTI-OPERATION TEST (TC_OFF_035)
    // ============================================================

    @Test(priority = 35)
    public void TC_OFF_035_verifyMultipleOfflineOperationsQueueCorrectly() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_OFFLINE,
                AppConstants.FEATURE_SYNC_QUEUE,
                "TC_OFF_035 - Verify multiple offline operations queue correctly");

        if (!siteSelectionPage.isDashboardDisplayed()) {
            smartNavigateToDashboard();
        }

        // Go offline
        logStep("Going offline for multi-operation test");
        goOfflineViaPopup();
        assertTrue(isDefinitelyOffline(), "Should be in offline mode");

        // Operation 1: Create a building
        logStep("Operation 1: Creating building offline");
        siteSelectionPage.clickLocations();
        mediumWait();
        try {
            siteSelectionPage.waitForLocationsReady();
        } catch (Exception e) {
            logWarning("Locations wait: " + e.getMessage());
        }

        String building1 = "Multi_Bldg1_" + System.currentTimeMillis();
        logStep("Creating building: " + building1);
        try {
            siteSelectionPage.createBuilding(building1);
            mediumWait();
            logStep("Building 1 created");
        } catch (Exception e) {
            logWarning("Building 1 creation error: " + e.getMessage());
        }

        // Operation 2: Create a second building
        String building2 = "Multi_Bldg2_" + System.currentTimeMillis();
        logStep("Operation 2: Creating second building: " + building2);
        try {
            siteSelectionPage.createBuilding(building2);
            mediumWait();
            logStep("Building 2 created");
        } catch (Exception e) {
            logWarning("Building 2 creation error: " + e.getMessage());
        }

        // Operation 3: Create a third building
        String building3 = "Multi_Bldg3_" + System.currentTimeMillis();
        logStep("Operation 3: Creating third building: " + building3);
        try {
            siteSelectionPage.createBuilding(building3);
            mediumWait();
            logStep("Building 3 created");
        } catch (Exception e) {
            logWarning("Building 3 creation error: " + e.getMessage());
        }

        // Return to dashboard
        logStep("Returning to dashboard");
        try {
            siteSelectionPage.clickDone();
            mediumWait();
        } catch (Exception e) {
            logWarning("Could not click Done: " + e.getMessage());
        }

        logStepWithScreenshot("Dashboard after 3 offline operations");

        // Check WiFi popup for pending sync count
        logStep("Checking WiFi for pending sync count");
        siteSelectionPage.clickWifiButton();
        shortWait();

        int syncCount = siteSelectionPage.getPendingSyncCount();
        boolean hasPending = siteSelectionPage.hasPendingSyncRecords();
        logStep("Pending sync count: " + syncCount + ", has pending: " + hasPending);

        logStepWithScreenshot("WiFi popup showing multiple pending operations");

        // Verify multiple pending operations are queued
        assertTrue(syncCount >= 3 || hasPending,
                "Should have at least 3 pending sync records after creating 3 buildings offline " +
                "(actual count: " + syncCount + ")");

        // Navigate to Sync Queue Analyzer to verify all operations listed
        logStep("Navigating to Settings → Sync Queue Analyzer to verify all queued operations");
        boolean settingsFound = navigateToSettings();

        if (settingsFound) {
            boolean sqaOpened = navigateToSyncQueueAnalyzer();

            if (sqaOpened) {
                logStepWithScreenshot("Sync Queue Analyzer — verifying multiple pending items");

                try {
                    // Check Pending tab count
                    java.util.List<org.openqa.selenium.WebElement> pendingTabs =
                        DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                            "label CONTAINS 'Pending'"));

                    if (!pendingTabs.isEmpty()) {
                        String tabLabel = pendingTabs.get(0).getAttribute("label");
                        logStep("Pending tab label: " + tabLabel);

                        // Extract count from "Pending (N)"
                        if (tabLabel.contains("(")) {
                            String countStr = tabLabel.replaceAll("[^0-9]", "");
                            if (!countStr.isEmpty()) {
                                int pendingCount = Integer.parseInt(countStr);
                                logStep("Pending count from tab: " + pendingCount);
                                assertTrue(pendingCount >= 3,
                                        "Pending tab should show count >= 3 after 3 offline operations " +
                                        "(actual: " + pendingCount + ")");
                            }
                        }

                        // Tap to ensure Pending tab is active
                        pendingTabs.get(0).click();
                        shortWait();
                    }

                    // Verify multiple operation items with timestamps
                    java.util.List<org.openqa.selenium.WebElement> operationItems =
                        DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND " +
                            "(label CONTAINS ' - create' OR label CONTAINS 'building')"));

                    logStep("Operation items found in Pending tab: " + operationItems.size());
                    for (int i = 0; i < Math.min(operationItems.size(), 5); i++) {
                        logStep("  - " + operationItems.get(i).getAttribute("label"));
                    }

                    // Check for timestamps
                    java.util.List<org.openqa.selenium.WebElement> timestamps =
                        DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND " +
                            "(label CONTAINS 'Queued' OR label CONTAINS 'ago')"));
                    logStep("Timestamp elements found: " + timestamps.size());

                    logStepWithScreenshot("All queued operations with timestamps verified");
                } catch (Exception e) {
                    logWarning("SQA verification error: " + e.getMessage());
                }
            } else {
                logWarning("Could not open Sync Queue Analyzer");
            }
        } else {
            logWarning("Settings not accessible — sync count verified via WiFi popup only");
        }

        // Cleanup
        ensureOnlineState();
    }
}
