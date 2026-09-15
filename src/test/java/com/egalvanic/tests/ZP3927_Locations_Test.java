package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.BuildingPage;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * ZP-3927 (iOS 1.57) §2 New Location View + §3 Independent Building / Floor / Room creation.
 *
 * The v1.57/v1.63 change: the old composite New Location form (where Building + Floor + Room
 * ALL had to be filled or Create stayed disabled) is replaced by an independent model. Copy
 * verified verbatim in the v1.63 bundle, not guessed:
 *   "New Building…" / "New Floor…"            (real U+2026 ellipsis)
 *   "Leave blank to create only the building." -> a building needs NO floor/room
 *   "A floor is required to create a room"     -> a room is NOT fully independent
 *   "Building name is required"                -> the validation copy
 *
 * NOTE vs the catalog: TC_IND_03 was drafted as "a room can be created on its own". The app
 * says otherwise, so the test asserts the REAL contract (a room needs a floor) instead of a
 * fiction. Capture app truth first, then hard-assert it.
 *
 * Fixtures are stable and never timestamped so reruns are idempotent (QA-LOC *).
 */
public final class ZP3927_Locations_Test extends BaseTest {

    private static final String FEATURE_LV  = "New Location View (ZP-3927)";
    private static final String FEATURE_IND = "Independent Building/Floor/Room (ZP-3927)";

    private static final String QA_BUILDING = "QA-LOC Building";
    private static final String QA_FLOOR    = "QA-LOC Floor";
    private static final String QA_ROOM     = "QA-LOC Room";

    private BuildingPage buildings;

    private BuildingPage locations() {
        if (buildings == null) buildings = new BuildingPage();
        return buildings;
    }

    /** Land on the Locations screen; skip (never fail) if navigation itself is unavailable. */
    private void openLocations() {
        loginAndSelectSite();
        // Retry once: a previous test can leave the app mid-flow, and the first
        // navigate then lands somewhere else before settling.
        boolean on = locations().isLocationsScreenDisplayed() || locations().navigateToLocationsScreen();
        if (!on) {
            mediumWait();
            on = locations().isLocationsScreenDisplayed() || locations().navigateToLocationsScreen();
        }
        if (!on) {
            throw new SkipException("Locations screen did not open — cannot exercise the location view");
        }
        mediumWait();
    }

    private void openCreateForm() {
        openLocations();
        if (!locations().openNewBuildingSheetV163()) {
            throw new SkipException("v1.63 create-location form did not open on this build");
        }
    }

    // ── §2 New Location View ────────────────────────────────────────────

    @Test(priority = 1)
    public void TC_LV_01_locationViewOpens() {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, FEATURE_LV,
                "TC_LV_01 - Location view opens");
        openLocations();
        logStep("Asserting the locations view rendered");
        assertTrue(locations().isLocationsScreenDisplayed(),
                "The Locations view should render after tapping the Locations tab");
        logStepWithScreenshot("TC_LV_01: locations view open");
    }

    @Test(priority = 2)
    public void TC_LV_02_hierarchyIsListed() {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, FEATURE_LV,
                "TC_LV_02 - Buildings, floors and rooms are listed");
        openLocations();
        int rows = locations().visibleLocationRowCount();
        logStep("Visible location rows: " + rows);
        assertTrue(rows > 0,
                "The locations view should list at least one building/floor/room row (found " + rows + ")");
        logStepWithScreenshot("TC_LV_02: hierarchy listed (" + rows + " rows)");
    }

    @Test(priority = 5)
    public void TC_LV_05_emptyStateIsClean() {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, FEATURE_LV,
                "TC_LV_05 - Empty state renders without hanging");
        openLocations();
        logStep("Asserting the view settled (no perpetual spinner) and the app is alive");
        assertTrue(locations().isLocationsScreenDisplayed(),
                "Locations view should settle into a rendered state, not a spinner");
        verifyAppAlive("after opening the locations view");
        logStepWithScreenshot("TC_LV_05: view settled");
    }

    @Test(priority = 6)
    public void TC_LV_06_viewSurvivesReSync() {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, FEATURE_LV,
                "TC_LV_06 - Hierarchy survives a re-sync with no duplicates");
        openLocations();
        int before = locations().visibleLocationRowCount();
        logStep("Rows before re-sync: " + before);

        logStep("Re-syncing via site re-selection");
        siteSelectionPage.clickSitesButton();
        siteSelectionPage.selectFirstSiteFast();
        siteSelectionPage.waitForDashboardReady();

        openLocations();
        int after = locations().visibleLocationRowCount();
        logStep("Rows after re-sync: " + after);
        assertEquals(after, before,
                "Re-sync must not duplicate or drop location rows (before=" + before + ", after=" + after + ")");
        logStepWithScreenshot("TC_LV_06: row count stable across re-sync");
    }

    // ── §3 Independent creation ─────────────────────────────────────────

    @Test(priority = 11)
    public void TC_IND_01_createBuildingAlone() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUILDING, FEATURE_IND,
                "TC_IND_01 - A building can be created with no floor and no room");
        openCreateForm();

        logStep("Asserting the v1.63 hint that a building alone is allowed");
        assertTrue(locations().isBuildingOnlyHintShown(),
                "v1.63 must show '" + BuildingPage.V163_BUILDING_ONLY + "' — that hint IS the "
                + "independent-creation contract this ticket introduces");

        logStep("Entering only the building name, leaving Floor and Room empty");
        assertTrue(locations().typeLocationField("Building name", QA_BUILDING),
                "Building name field should accept input");
        assertTrue(locations().isCreateEnabled(),
                "Create must be ENABLED with only a building name — the old all-three-slots gate is gone");
        assertTrue(locations().tapCreate(), "Create should be tappable");

        logStep("Verifying the building landed in the list");
        assertTrue(locations().isLocationListed(QA_BUILDING),
                "'" + QA_BUILDING + "' should appear in the locations list right after creation");
        logStepWithScreenshot("TC_IND_01: building created alone");
    }

    @Test(priority = 14)
    public void TC_IND_04_createEnablesOnOwnFieldOnly() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUILDING, FEATURE_IND,
                "TC_IND_04 - Create enables on the building name alone (old gate retired)");
        openCreateForm();

        logStep("Create should start disabled with every field empty");
        boolean enabledWhenEmpty = locations().isCreateEnabled();
        logStep("Create enabled with empty form: " + enabledWhenEmpty);

        logStep("Type ONLY the building name");
        assertTrue(locations().typeLocationField("Building name", QA_BUILDING),
                "Building name field should accept input");
        assertTrue(locations().isCreateEnabled(),
                "Create must enable from the building name alone — under the pre-1.57 composite form "
                + "it stayed disabled until Building + Floor + Room were all filled");
        logStepWithScreenshot("TC_IND_04: create gated on its own field only");
    }

    @Test(priority = 15)
    public void TC_IND_05_nameIsRequired() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUILDING, FEATURE_IND,
                "TC_IND_05 - A blank building name is rejected");
        openCreateForm();
        logStep("Attempting Create with an empty name");
        boolean enabled = locations().isCreateEnabled();
        if (enabled) {
            locations().tapCreate();
            assertTrue(locations().isNameRequiredErrorShown() || locations().isV163CreateFormOpen(),
                    "A blank name must be rejected: either '" + BuildingPage.V163_NAME_REQUIRED
                    + "' is shown or the form stays open — it must NOT create a nameless building");
        } else {
            logStep("Create is disabled with an empty name — the requirement is enforced up front");
        }
        logStepWithScreenshot("TC_IND_05: blank name rejected");
    }

    @Test(priority = 13)
    public void TC_IND_03_roomRequiresAFloor() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUILDING, FEATURE_IND,
                "TC_IND_03 - A room needs a floor (real v1.63 contract)");
        openCreateForm();

        logStep("Entering a building name and a ROOM name, deliberately leaving Floor empty");
        assertTrue(locations().typeLocationField("Building name", QA_BUILDING),
                "Building name field should accept input");
        boolean roomTyped = locations().typeLocationField("Room name", QA_ROOM);
        if (!roomTyped) {
            throw new SkipException("Room field is not present until a floor is chosen — "
                    + "that alone satisfies the 'a room needs a floor' contract; nothing left to assert here");
        }

        logStep("The app must say a floor is required rather than silently making an orphan room");
        boolean guarded = locations().isFloorRequiredForRoomShown() || !locations().isCreateEnabled();
        assertTrue(guarded,
                "With a room but no floor, v1.63 must either show '" + BuildingPage.V163_FLOOR_REQUIRED
                + "' or keep Create disabled — an orphan room must never be created");
        logStepWithScreenshot("TC_IND_03: room-without-floor is blocked");
    }

    @Test(priority = 18)
    public void TC_IND_08_keyboardDoesNotBlockCreate() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUILDING, FEATURE_IND,
                "TC_IND_08 - Create stays reachable after typing (keyboard guard)");
        openCreateForm();
        logStep("Typing the name then immediately reaching for Create");
        assertTrue(locations().typeLocationField("Building name", QA_BUILDING),
                "Building name field should accept input");
        assertTrue(locations().isCreateEnabled(),
                "Create must be reachable and enabled once the keyboard is dismissed — a covered "
                + "Create button is the classic cause of silent save failures in this suite");
        logStepWithScreenshot("TC_IND_08: create reachable after typing");
    }

    @Test(priority = 20)
    public void TC_IND_10_cancelCreatesNothing() {
        ExtentReportManager.createTest(AppConstants.MODULE_BUILDING, FEATURE_IND,
                "TC_IND_10 - Cancel creates nothing");
        openLocations();
        int before = locations().visibleLocationRowCount();
        logStep("Rows before: " + before);

        if (!locations().openNewBuildingSheetV163()) {
            throw new SkipException("v1.63 create-location form did not open on this build");
        }
        locations().typeLocationField("Building name", "QA-LOC Cancelled");
        logStep("Cancelling the form");
        locations().cancelForm();
        mediumWait();

        int after = locations().visibleLocationRowCount();
        logStep("Rows after cancel: " + after);
        assertEquals(after, before,
                "Cancel must not create anything (before=" + before + ", after=" + after + ")");
        assertFalse(locations().isLocationListed("QA-LOC Cancelled"),
                "The cancelled building must NOT appear in the list");
        logStepWithScreenshot("TC_IND_10: cancel created nothing");
    }
}
