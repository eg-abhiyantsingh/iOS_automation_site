package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.utils.Waits;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Settings › Session Analytics › "Session Recording" — auto-disable canary.
 *
 * The toggle defaults ON in every clean install and slows the whole app
 * ("Keeping this off improves performance and reduces battery usage" — the
 * app's own subtitle). The framework turns it OFF once per Appium session via
 * BaseTest.ensureSessionRecordingDisabledIfFreshInstall(), armed by
 * DriverManager at session creation.
 *
 * These tests are the live proof that the mechanism works end-to-end on a
 * real install — all hard-asserted, no pass-anyway:
 * - TC_SET_001: after the standard login path, the switch must read OFF.
 * - TC_SET_002: the disable primitive is idempotent on an OFF switch.
 * - TC_SET_003: deterministically exercises the ON→OFF tap path (flips the
 *   switch ON by hand, then requires the primitive to bring it back OFF), so
 *   the transition is covered even when the install arrived already-OFF.
 */
public final class SettingsSessionRecording_Test extends BaseTest {

    private static final String FEATURE_SESSION_ANALYTICS = "Session Analytics";

    private void requireFeatureEnabled() {
        if (!AppConstants.DISABLE_SESSION_RECORDING) {
            throw new SkipException(
                    "DISABLE_SESSION_RECORDING=false — auto-disable is switched off for this run");
        }
    }

    /** Login (any starting screen), open Settings, and hard-fail if either step dies. */
    private void openSettingsFromAnywhere() {
        loginAndSelectSite();
        assertTrue(settingsPage.openSettingsTab(),
                "Settings tab should open from the Dashboard tab bar");
    }

    @Test(priority = 1)
    public void TC_SET_001_sessionRecordingIsOffAfterFreshInstallLogin() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_SETTINGS,
                FEATURE_SESSION_ANALYTICS,
                "TC_SET_001 - Session Recording is auto-disabled after fresh-install login");
        requireFeatureEnabled();

        logStep("Standard login path (fresh-install pass runs inside if armed)");
        openSettingsFromAnywhere();
        logStepWithScreenshot("Settings screen open — reading Session Recording switch");

        String value = settingsPage.sessionRecordingValue();
        assertNotNull(value, "Session Recording switch should exist on the Settings screen "
                + "(Session Analytics section)");
        assertEquals(value, "0",
                "Session Recording must be OFF after the fresh-install auto-disable pass");

        settingsPage.openSiteTab();
    }

    @Test(priority = 2)
    public void TC_SET_002_disableIsIdempotentWhenAlreadyOff() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_SETTINGS,
                FEATURE_SESSION_ANALYTICS,
                "TC_SET_002 - disableSessionRecordingIfOn is idempotent on an OFF switch");
        requireFeatureEnabled();

        openSettingsFromAnywhere();

        logStep("Calling disable primitive on an already-OFF switch");
        assertTrue(settingsPage.disableSessionRecordingIfOn(),
                "Idempotent call must succeed when the switch is already OFF");
        assertEquals(settingsPage.sessionRecordingValue(), "0",
                "Switch must still read OFF after the idempotent call");

        settingsPage.openSiteTab();
    }

    @Test(priority = 3)
    public void TC_SET_003_disableBringsAnOnSwitchBackOff() {
        ExtentReportManager.createTest(
                AppConstants.MODULE_SETTINGS,
                FEATURE_SESSION_ANALYTICS,
                "TC_SET_003 - disable primitive flips an ON switch back OFF");
        requireFeatureEnabled();

        openSettingsFromAnywhere();

        logStep("Arming the scenario: flipping Session Recording ON by hand");
        org.openqa.selenium.WebElement sw = settingsPage.findSessionRecordingSwitch();
        assertNotNull(sw, "Session Recording switch should be locatable");
        if (!"1".equals(sw.getAttribute("value"))) {
            sw.click();
            boolean on = Waits.until(
                    () -> "1".equals(settingsPage.sessionRecordingValue()), 4000);
            assertTrue(on, "Precondition: switch should read ON after the arming tap");
        }
        logStepWithScreenshot("Switch armed ON — running disable primitive");

        assertTrue(settingsPage.disableSessionRecordingIfOn(),
                "Disable primitive must bring an ON switch back OFF");
        assertEquals(settingsPage.sessionRecordingValue(), "0",
                "Switch must read OFF at the end (state left clean for later suites)");
        logStepWithScreenshot("Session Recording back OFF");

        settingsPage.openSiteTab();
    }
}
