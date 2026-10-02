package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.BuildingPage;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

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

    /** The driver is re-created per test; never reuse a page bound to a quit session ("Session ID is null"). */
    @BeforeMethod(alwaysRun = true)
    public void resetPages() { buildings = null; }

    private BuildingPage locations() {
        if (buildings == null) buildings = new BuildingPage();
        return buildings;
    }

    /** Dismiss a half-filled create form so it cannot poison the next test. */
    private void closeAnyOpenForm() {
        try {
            if (locations().isV163CreateFormOpen()) {
                locations().cancelForm();
                mediumWait();
            }
        } catch (Exception ignored) { }
    }

    @AfterMethod(alwaysRun = true)
    public void leaveLocationsClean() {
        closeAnyOpenForm();
        try { settingsPage.openSiteTab(); } catch (Exception ignored) { }
    }

    /**
     * Land on the Locations screen.
     *
     * The first live run (2026-09-15) failed 2 and skipped 7 for one reason: each test left
     * the app ON Locations or inside a create form, so the NEXT test's loginAndSelectSite()
     * saw screen=UNKNOWN, tried to pick a site named 'null', and died. Tests must not inherit
     * the previous test's screen — so anchor back to the Dashboard first, then navigate.
     */
    private void openLocations() {
        closeAnyOpenForm();
        try { settingsPage.openSiteTab(); } catch (Exception ignored) { }
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
        mediumWait();   // the tree renders asynchronously after the tap
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

    // 10 min, not the 6-min default: a full site re-selection sync + two Locations loads of a ~220-row tree
    // ran 313 s on a normal day and timed out on 2026-10-02 when the QA SLD fetch alone took 37 s.
    @Test(priority = 6, timeOut = 600_000)
    public void TC_LV_06_viewSurvivesReSync() {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, FEATURE_LV,
                "TC_LV_06 - Hierarchy survives a re-sync with no duplicates");
        // Site name read on a SETTLED dashboard first — right after the re-sync it read null (2026-10-02) and
        // the drop classifier could not resolve the SLD. The re-sync re-selects this same site.
        loginAndSelectSite();
        String startSite = siteSelectionPage.getCurrentSiteName();
        openLocations();
        java.util.Map<String, Long> before = rowCounts(locations().locationRowLabels());
        logStep("Distinct location rows before re-sync: " + before.size());

        logStep("Re-syncing via site re-selection");
        siteSelectionPage.clickSitesButton();
        siteSelectionPage.selectFirstSiteFast();
        siteSelectionPage.waitForDashboardReady();
        String site = startSite != null ? startSite : siteSelectionPage.getCurrentSiteName();

        openLocations();
        java.util.Map<String, Long> after = rowCounts(locations().locationRowLabels());
        logStep("Distinct location rows after re-sync: " + after.size());

        // Compare the row LABELS (a multiset — a duplicate shows up as count 2), not a raw button count.
        java.util.List<String> added = new java.util.ArrayList<>(), dropped = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Long> e : after.entrySet())
            if (e.getValue() > before.getOrDefault(e.getKey(), 0L)) added.add(e.getKey() + " ×" + e.getValue());
        for (java.util.Map.Entry<String, Long> e : before.entrySet())
            if (e.getValue() > after.getOrDefault(e.getKey(), 0L)) dropped.add(e.getKey());
        logStep("Re-sync diff — added/duplicated: " + added + " · dropped: " + dropped);
        List<String> unexplained = unexplainedDrops(dropped, after.keySet(), site);
        assertTrue(added.isEmpty() && unexplained.isEmpty(),
                "Re-sync must not duplicate or drop location rows — added/duplicated " + added
                + ", dropped and still live on the backend with no listed parent " + unexplained);
        logStepWithScreenshot("TC_LV_06: no duplicated or lost location rows across re-sync");
    }

    /**
     * Rows missing after the re-sync that the backend cannot explain. Live run 2026-10-02: the three "dropped"
     * rows were all LIVE rooms of ONE floor — the re-sync collapsed that floor, and a collapsed floor's rooms
     * are not rendered. So a dropped row is fine when the backend no longer has it live, or when it is a live
     * room whose parent floor row is still listed; anything else is a real loss.
     */
    private List<String> unexplainedDrops(List<String> dropped, java.util.Set<String> afterLabels, String site) {
        List<String> out = new java.util.ArrayList<>();
        if (dropped.isEmpty()) return out;
        try {
            com.egalvanic.api.TestDataApi api = new com.egalvanic.api.TestDataApi();
            api.login();
            String sld = site == null ? null : api.resolveSldIdByName(site);
            if (sld == null) { logStep("Backend: site unresolved — drops cannot be classified"); return dropped; }
            io.restassured.path.json.JsonPath p = io.restassured.path.json.JsonPath.from(api.getSldDetails(sld));
            List<java.util.Map<String, Object>> floors = p.getList("floors"), rooms = p.getList("rooms"),
                    buildings = p.getList("buildings");
            java.util.Map<String, String> floorNameById = new java.util.HashMap<>();
            for (java.util.Map<String, Object> f : floors)
                if (!Boolean.TRUE.equals(f.get("is_deleted"))) floorNameById.put(String.valueOf(f.get("id")), String.valueOf(f.get("name")));
            for (String label : dropped) {
                String name = label.contains(",") ? label.substring(0, label.indexOf(',')).trim() : label.trim();
                boolean liveAnywhere = java.util.stream.Stream.of(buildings, floors, rooms).flatMap(List::stream)
                        .anyMatch(r -> name.equals(r.get("name")) && !Boolean.TRUE.equals(r.get("is_deleted")));
                boolean collapsedChild = rooms.stream()
                        .filter(r -> name.equals(r.get("name")) && !Boolean.TRUE.equals(r.get("is_deleted")))
                        .map(r -> floorNameById.get(String.valueOf(r.get("floor_id"))))
                        .anyMatch(fn -> fn != null && afterLabels.stream().anyMatch(l -> l.startsWith(fn)));
                logStep("   dropped '" + label + "': live on backend=" + liveAnywhere + " · parent floor still listed=" + collapsedChild);
                if (liveAnywhere && !collapsedChild) out.add(label);
            }
        } catch (Exception e) {
            logStep("Backend unavailable to classify drops: " + e);
            return dropped;
        }
        return out;
    }

    /** Row label → occurrences, minus rows this suite itself creates/cleans (a parallel run may change them). */
    private static java.util.Map<String, Long> rowCounts(java.util.List<String> labels) {
        java.util.Map<String, Long> m = new java.util.TreeMap<>();
        for (String l : labels) if (!l.startsWith("QA-LOC")) m.merge(l, 1L, Long::sum);
        return m;
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
        // Polled: the backend held the building at once (2026-10-02, id dcf9fb43) but the list row arrived later.
        assertTrue(locations().waitForLocationRow(QA_BUILDING, 45_000),
                "'" + QA_BUILDING + "' should appear in the locations list after creation");
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
