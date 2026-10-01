package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.FlirImportPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * ZP-3927 (iOS 1.57) §4 SD-card / drive import · §5 30-minute time window · §7 already-uploaded photos.
 *
 * Surface (probe 1.63): work-order session › More › IR › "Add IR Photos" → three routes —
 * From Photos (library) · Browse & Select (Files or a drive — the SD-card path) · Auto-Match by Name
 * (drive, IR-key matching). A simulator has no camera or drive, so everything the drive itself would
 * render (candidate list, "already uploaded" chips, the "Last 30 min" window filter) is device-only
 * and gets an explicit verdict here instead of a silent gap — the same treatment TC_VOL_01 gives the
 * volume buttons. What IS provable on the simulator, and is asserted: the IR screen and its copy, the
 * three routes and their subtitles, and that each route opens a real picker (or an explicit no-drive
 * state) rather than doing nothing.
 */
public final class ZP3927_FlirImport_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "IR photo import routes: drive / SD card / FLIR (ZP-3927)";

    private FlirImportPage flir;

    @BeforeMethod(alwaysRun = true)
    public void initPages() {
        if (!DriverManager.isDriverActive()) return;
        try { flir = new FlirImportPage(); } catch (IllegalStateException e) { DriverManager.initDriver(); flir = new FlirImportPage(); }
    }

    /** Session › More › IR, skip-guarded at each stage. */
    private void openIrScreenOrSkip(String tcId) {
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(), tcId + ": session details did not settle open");
        skipIfPreconditionMissing(() -> flir.openIrScreenFromSession(),
                tcId + ": the IR screen (session › More › IR) did not open on this build");
    }

    private void openRoutePopupOrSkip(String tcId) {
        openIrScreenOrSkip(tcId);
        skipIfPreconditionMissing(() -> flir.openAddIrPhotosMenu(),
                tcId + ": 'Add IR Photos' did not open the import-route popup");
    }

    @Test(priority = 1)
    public void TC_FI_01_irScreenAndItsCopy() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_FI_01 - The session IR screen renders its 1.57 copy and an Add IR Photos entry");
        openIrScreenOrSkip("TC_FI_01");
        int count = flir.irPhotoCount();
        logStep("IR photo counter: " + count);
        assertTrue(flir.isAnyVisibleText(FlirImportPage.ADD_IR_PHOTOS),
                "The IR screen must expose '" + FlirImportPage.ADD_IR_PHOTOS + "' — without it no import route is reachable");
        assertTrue(flir.isAnyVisibleTextContaining("IR & visual photo keys") || flir.isAnyVisibleTextContaining("IR &amp; visual photo keys"),
                "The IR screen should explain the key rule ('" + FlirImportPage.IR_KEYS_HINT + "') — the Auto-Match route depends on it");
        if (count == 0) {
            assertTrue(flir.isAnyVisibleText(FlirImportPage.NO_IR_PHOTOS),
                    "With 0 IR photos the screen must show the explicit '" + FlirImportPage.NO_IR_PHOTOS + "' empty state, not a blank list");
        }
        logStepWithScreenshot("TC_FI_01: IR screen (" + count + " photos)");
    }

    @Test(priority = 2)
    public void TC_FI_02_addIrPhotosOffersDriveRoutes() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_FI_02 - 'Add IR Photos' offers From Photos, Browse & Select (Files/drive) and Auto-Match by Name");
        openRoutePopupOrSkip("TC_FI_02");
        List<String> routes = flir.visibleRoutes();
        logStep("routes offered: " + routes);
        assertEquals(routes, Arrays.asList(FlirImportPage.ROUTES),
                "The 1.57 import popup must offer exactly the three routes in order " + Arrays.toString(FlirImportPage.ROUTES)
                + " — got " + routes + ". The two drive routes ARE the SD-card import this ticket adds.");
        assertTrue(flir.isRouteSubtitleShown(FlirImportPage.ROUTE_BROWSE_SUB),
                "'" + FlirImportPage.ROUTE_BROWSE + "' should carry its subtitle '" + FlirImportPage.ROUTE_BROWSE_SUB + "'");
        assertTrue(flir.isRouteSubtitleShown(FlirImportPage.ROUTE_AUTO_MATCH_SUB),
                "'" + FlirImportPage.ROUTE_AUTO_MATCH + "' should carry its subtitle '" + FlirImportPage.ROUTE_AUTO_MATCH_SUB + "'");
        logStepWithScreenshot("TC_FI_02: three import routes");
        flir.dismissRoutePopup();
    }

    @Test(priority = 3)
    public void TC_FI_03_fromPhotosOpensTheLibraryPicker() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_FI_03 - 'From Photos' presents the photo-library picker");
        openRoutePopupOrSkip("TC_FI_03");
        assertTrue(flir.tapRoute(FlirImportPage.ROUTE_FROM_PHOTOS), "'From Photos' should be tappable");
        mediumWait();
        boolean presented = flir.isSystemPickerPresented();
        logStep("library picker presented: " + presented);
        assertTrue(presented, "'From Photos' must present the system photo picker (the IR screen chrome should be covered)");
        verifyAppAlive("with the photo picker presented from the IR screen");
        logStepWithScreenshot("TC_FI_03: library picker");
        flir.dismiss();
    }

    @Test(priority = 4)
    public void TC_FI_04_browseAndSelectOpensFilesOrDrivePicker() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_FI_04 - 'Browse & Select' presents the Files / drive picker (the SD-card path)");
        openRoutePopupOrSkip("TC_FI_04");
        assertTrue(flir.tapRoute(FlirImportPage.ROUTE_BROWSE), "'Browse & Select' should be tappable");
        mediumWait();
        boolean presented = flir.isSystemPickerPresented();
        logStep("Files/drive picker presented: " + presented);
        assertTrue(presented, "'Browse & Select' must present the Files document picker — that is how an SD card or drive is "
                + "browsed; a route that does nothing is the regression this guards");
        verifyAppAlive("with the Files picker presented from the IR screen");
        logStepWithScreenshot("TC_FI_04: Files/drive picker");
        flir.dismiss();
    }

    @Test(priority = 5)
    public void TC_FI_05_autoMatchWithoutADriveIsExplicit() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_FI_05 - 'Auto-Match by Name' with no drive gives an explicit state (and any time-window chips are the declared seven)");
        openRoutePopupOrSkip("TC_FI_05");
        assertTrue(flir.tapRoute(FlirImportPage.ROUTE_AUTO_MATCH), "'Auto-Match by Name' should be tappable");
        mediumWait();
        List<String> windows = flir.visibleTimeFilters();
        boolean driveScreen = flir.isSelectDriveScreenShown();
        boolean picker = !driveScreen && flir.isSystemPickerPresented();
        logStep("drive-selection screen: " + driveScreen + " · time-window chips: " + windows + " · other picker: " + picker);
        verifyAppAlive("after choosing Auto-Match by Name without a drive");
        assertTrue(driveScreen || picker || !windows.isEmpty(),
                "'Auto-Match by Name' with no drive must lead somewhere explicit — probe 1.63 shows the '"
                + FlirImportPage.SELECT_DRIVE_TITLE + "' screen ('" + FlirImportPage.DRIVE_MATCH_HINT + "', '"
                + FlirImportPage.SELECT_DRIVE_BUTTON + "'); it must not silently do nothing");
        if (driveScreen) {
            assertTrue(flir.isAnyVisibleText(FlirImportPage.SELECT_DRIVE_BUTTON),
                    "The drive-selection screen must offer '" + FlirImportPage.SELECT_DRIVE_BUTTON + "'");
            assertTrue(flir.isAnyVisibleTextContaining("matched to this work order"),
                    "The drive-selection screen should explain the by-file-name match rule ('" + FlirImportPage.DRIVE_MATCH_HINT + "')");
        }
        if (!windows.isEmpty()) {
            assertTrue(windows.contains(FlirImportPage.TIME_FILTER_30_MIN),
                    "'" + FlirImportPage.TIME_FILTER_30_MIN + "' is the ticket's headline window and must be offered — got " + windows);
            assertEquals(windows, Arrays.asList(FlirImportPage.TIME_FILTERS),
                    "The time-window set must be exactly the seven the build declares, in order — got " + windows);
        } else {
            logStep("Time-window chips (FLIRDriveTimeFilter: " + Arrays.toString(FlirImportPage.TIME_FILTERS)
                    + ") render only with a drive/camera present — device-only from here");
        }
        logStepWithScreenshot("TC_FI_05: auto-match without a drive");
        flir.dismiss();
    }

    @Test(priority = 6)
    public void TC_FI_06_alreadyUploadedDeselectedByDefault_deviceOnly() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_FI_06 - Already-uploaded photos deselected by default (device-only verdict)");
        logStep("Needs a FLIR camera or its SD card on the device; the simulator has neither, so the candidate list "
                + "('" + FlirImportPage.NEWEST_FIRST + "', '" + FlirImportPage.ALREADY_UPLOADED + "' chips, '"
                + FlirImportPage.SELECT_ALL + "' / '" + FlirImportPage.DESELECT_ALL + "') never renders here.");
        logStep("Manual check on device: mount the card → Browse & Select / Auto-Match → candidates list newest first → "
                + "photos already on the asset carry '" + FlirImportPage.ALREADY_UPLOADED + "' and start UNCHECKED → '"
                + FlirImportPage.IMPORT + "' imports only the checked ones.");
        throw new SkipException("Drive candidate list is device-only (no camera/drive on the simulator) — manual on device");
    }

    @Test(priority = 7)
    public void TC_FI_07_importWithinThirtyMinuteWindow_deviceOnly() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_FI_07 - Timestamp-based selection within the 30-minute window (device-only verdict)");
        logStep("Photos seeded on the simulator library at 5 m / 20 m / 3 h / 2 d ago (mtime-driven) are ready for a "
                + "device run: with '" + FlirImportPage.TIME_FILTER_30_MIN + "' only the 5 m and 20 m captures may be listed.");
        throw new SkipException("Window-filtered import needs the drive candidate list — device-only");
    }
}
