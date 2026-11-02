package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.ZP3914BaseTest;
import com.egalvanic.pages.SiteWalkPage;
import com.egalvanic.pages.SiteWalkPage.WalkRow;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

/**
 * ZP-3914 (iOS 1.59) — "Count-mode site walk" (asset count grid, rooms, completion).
 *
 * Every case starts its own unassigned Count walk (wizard 'Assign customer and site later'), so cases never share
 * counts; walks are deleted by backend id afterwards. Counting is done on the FIRST class card's FIRST subtype —
 * the catalogue differs per company, so the test reads names off the screen instead of hard-coding 'ATS'.
 */
public final class ZP3914_CountGrid_Test extends ZP3914BaseTest {

    private static final String FEATURE = "Count grid (ZP-3914)";

    /** Walk id of the case's walk (tracked for deletion). */
    private String walkId;

    /** New unassigned Count walk, open on its detail; FAILS (not skips) when the wizard can't make one. */
    private void newCountWalk(String tcId) {
        openSiteWalksOrSkip(tcId);
        Set<String> before = walkIds();
        String created = walks.createUnassignedCountWalk();
        if (created == null) trackNewWalks(before);   // a walk may exist behind an unconfirmed last step — never leak it
        assertNotNull(created, tcId + ": the wizard must create a Count walk");
        // 45 s: on CI one walk took longer than 20 s to post (TC_SWC_06, run 41080855780) while the next case's walk made it.
        // A walk the app queued (runner network drop) is synced and its detail re-opened by the helper.
        Set<String> fresh = awaitNewWalkOnBackend(before, tcId, 45);
        assertEquals(fresh.size(), 1, tcId + ": exactly one new walk expected — got " + fresh);
        walkId = fresh.iterator().next();
    }

    /** Expand the first class card and return {class, subtype}. */
    private String[] firstClassAndSubtype() {
        List<String> cards = walks.classCards();
        assertFalse(cards.isEmpty(), "The grid must list asset-class cards");
        String cls = cards.get(0).replaceAll(", \\d+$", "");
        assertTrue(walks.expandClass(cls), "Tapping the '" + cls + "' card must reveal its subtype steppers");
        List<String> subs = walks.subtypeRows();
        assertFalse(subs.isEmpty(), "'" + cls + "' must list at least one subtype with a stepper");
        logStep("Counting on class '" + cls + "', subtype '" + subs.get(0) + "' (subtypes " + subs + ")");
        return new String[]{cls, subs.get(0)};
    }

    @Test(priority = 1)
    public void TC_SWC_01_gridContractWithoutLocation() {
        featureTest(FEATURE, "TC_SWC_01 - 'Count assets without a location' opens the whole-site grid at zero");
        newCountWalk("TC_SWC_01");
        assertTrue(walks.openCountWithoutLocation(), "'" + SiteWalkPage.COUNT_WITHOUT_LOCATION + "' must open the count grid");
        assertTrue(walks.isTextVisible(SiteWalkPage.GRID_WHOLE_SITE), "The location-less grid says '" + SiteWalkPage.GRID_WHOLE_SITE + "'");
        assertEquals(walks.gridTotalText(), SiteWalkPage.NONE_COUNTED, "A fresh grid's total reads '" + SiteWalkPage.NONE_COUNTED + "'");
        List<String> cards = walks.classCards();
        logStep("Class cards: " + cards);
        assertFalse(cards.isEmpty(), "The grid must offer asset classes to count");
        for (String c : cards) {
            assertFalse(c.matches("^.+, \\d+$"), "No class may show a count on a fresh grid — '" + c + "'");
        }
        assertFalse(walks.isFinishEnabled(), "'" + SiteWalkPage.SAVE_ASSETS + "' must be disabled while nothing is counted");
        logStepWithScreenshot("TC_SWC_01: grid contract verified");
    }

    @Test(priority = 2)
    public void TC_SWC_02_stepperDrivesSubtypeClassAndTotal() {
        featureTest(FEATURE, "TC_SWC_02 - '+' / '−' move the subtype count, the class badge, the unit rows and the total (1 asset / N assets)");
        newCountWalk("TC_SWC_02");
        assertTrue(walks.openCountWithoutLocation(), "Open the grid");
        String[] cs = firstClassAndSubtype();
        String cls = cs[0], sub = cs[1];
        assertEquals(walks.subtypeCount(sub), 0, "A subtype starts at 0");
        assertFalse(walks.isRemoveEnabled(sub), "'Remove' must be disabled at 0");
        assertTrue(walks.increment(sub), "'Add' must raise '" + sub + "' to 1");
        assertEquals(walks.gridTotalText(), "1 asset", "Total must read the singular '1 asset'");
        assertEquals(walks.classCount(cls), 1, "Class card must read '" + cls + ", 1' — cards " + walks.classCards());
        assertTrue(walks.isRemoveEnabled(sub), "'Remove' must enable above 0");
        assertTrue(walks.increment(sub), "'Add' again must raise it to 2");
        assertEquals(walks.gridTotalText(), "2 assets", "Total must read '2 assets'");
        assertEquals(walks.classCount(cls), 2, "Class card must read '" + cls + ", 2'");
        List<String> units = walks.unitRows(cls);
        logStep("Unit rows: " + units);
        assertEquals(units, List.of(cls + " 1", cls + " 2"), "Each counted unit gets its own numbered row");
        assertTrue(walks.decrement(sub), "'Remove' must lower it to 1");
        assertEquals(walks.gridTotalText(), "1 asset", "Total must follow the decrement back to '1 asset'");
        assertEquals(walks.unitRows(cls), List.of(cls + " 1"), "The last unit row must go with the decrement");
        assertTrue(walks.isFinishEnabled(), "'" + SiteWalkPage.SAVE_ASSETS + "' must enable once something is counted");
        logStepWithScreenshot("TC_SWC_02: stepper arithmetic verified");
    }

    @Test(priority = 3)
    public void TC_SWC_03_saveAssetsPersistsAndUnlocksCompletion() {
        featureTest(FEATURE, "TC_SWC_03 - 'Save Assets' returns to the walk, unlocks 'Complete Site Walk' and reaches the backend");
        newCountWalk("TC_SWC_03");
        assertFalse(walks.isCompleteWalkEnabled(), "Before counting, completion is locked");
        assertTrue(walks.openCountWithoutLocation(), "Open the grid");
        String sub = firstClassAndSubtype()[1];
        assertTrue(walks.increment(sub), "Count one '" + sub + "'");
        assertTrue(walks.finishGrid(), "'" + SiteWalkPage.SAVE_ASSETS + "' must return to the walk detail");
        assertTrue(eventually(walks::isCompleteWalkEnabled, 6), "'" + SiteWalkPage.COMPLETE_WALK + "' must enable after a saved count");
        assertFalse(walks.isCompleteHintShown(), "The '" + SiteWalkPage.HINT_COUNT + "' hint must go once something is counted");
        assertTrue(eventually(() -> {
            TestDataApi.SiteWalkRow r = walkRow(walkId);
            return r != null && r.assetCount == 1;
        }, 30), "Backend asset_count must become 1 — got " + walkRow(walkId));
        logStepWithScreenshot("TC_SWC_03: saved count — " + walkRow(walkId));
    }

    @Test(priority = 4)
    public void TC_SWC_04_searchFiltersClassCards() {
        featureTest(FEATURE, "TC_SWC_04 - Grid search narrows the class cards; no match says 'No assets found'");
        newCountWalk("TC_SWC_04");
        assertTrue(walks.openCountWithoutLocation(), "Open the grid");
        List<String> all = walks.classCards();
        String first = all.get(0).replaceAll(", \\d+$", "");
        assertTrue(walks.searchGrid(first), "Typing in '" + SiteWalkPage.SEARCH_ASSETS + "' must work");
        List<String> hits = walks.classCards();
        logStep("Search '" + first + "' → " + hits);
        assertFalse(hits.isEmpty(), "Searching a listed class name must keep that class");
        assertTrue(hits.stream().anyMatch(h -> h.startsWith(first)), "The searched class must be among the hits: " + hits);
        assertTrue(walks.searchGrid("zzqq-no-asset-class"), "Search must accept a no-match query");
        assertTrue(eventually(() -> walks.classCards().isEmpty(), 4), "A no-match query must hide every class card — still " + walks.classCards());
        assertTrue(walks.isTextVisible(SiteWalkPage.ADD_ASSET_CLASS), "A miss still offers '" + SiteWalkPage.ADD_ASSET_CLASS + "'");
        assertTrue(walks.isGridNoMatchShown(), "A no-match query must say '" + SiteWalkPage.GRID_NO_MATCH + "'");
        walks.clearGridSearch();
        assertTrue(eventually(() -> walks.classCards().size() >= Math.min(all.size(), 3), 5),
                "Clearing the search must bring the classes back — " + walks.classCards());
        logStepWithScreenshot("TC_SWC_04: grid search verified");
    }

    @Test(priority = 5)
    public void TC_SWC_05_unsavedCountsAskBeforeLeaving() {
        featureTest(FEATURE, "TC_SWC_05 - Leaving a grid with unsaved counts asks 'Save changes?'; Discard keeps nothing");
        newCountWalk("TC_SWC_05");
        assertTrue(walks.openCountWithoutLocation(), "Open the grid");
        String sub = firstClassAndSubtype()[1];
        assertTrue(walks.increment(sub), "Count one '" + sub + "'");
        String alert = walks.backFromGrid("Cancel");
        assertEquals(alert, SiteWalkPage.SAVE_CHANGES_TITLE, "Back with unsaved counts must ask '" + SiteWalkPage.SAVE_CHANGES_TITLE + "'");
        assertTrue(walks.isGridShown(), "'Cancel' must keep the grid open");
        assertEquals(walks.gridTotalText(), "1 asset", "'Cancel' must keep the unsaved count");
        String again = walks.backFromGrid("Discard");
        assertEquals(again, SiteWalkPage.SAVE_CHANGES_TITLE, "The question must come back on the next Back");
        assertTrue(eventually(walks::isCountWalkDetailShown, 8), "'Discard' must leave the grid for the walk detail");
        assertFalse(walks.isCompleteWalkEnabled(), "Discarded counts must not unlock completion");
        assertTrue(walks.isCompleteHintShown(), "The '" + SiteWalkPage.HINT_COUNT + "' hint must still show");
        TestDataApi.SiteWalkRow r = walkRow(walkId);
        assertTrue(r == null || r.assetCount == 0, "Backend asset_count must stay 0 after Discard: " + r);
        logStepWithScreenshot("TC_SWC_05: unsaved-changes guard verified");
    }

    @Test(priority = 6)
    public void TC_SWC_06_roomFlowListsCountedRoom() {
        featureTest(FEATURE, "TC_SWC_06 - 'Start First Room': name the room, count, 'Complete Room' → the room is listed with its count");
        newCountWalk("TC_SWC_06");
        assertTrue(walks.startFirstRoom(), "'" + SiteWalkPage.START_FIRST_ROOM + "' must open a room grid");
        assertTrue(walks.isRoomHeaderShown(),
                "A room grid asks for '" + SiteWalkPage.ROOM_NAME + "' / '" + SiteWalkPage.FLOOR + "' (field placeholders)");
        assertFalse(walks.isFinishEnabled(), "'" + SiteWalkPage.COMPLETE_ROOM + "' must be disabled while nothing is counted");
        String room = "QA-AUTO Room " + (System.currentTimeMillis() % 10_000);
        assertTrue(walks.typeRoomName(room), "The room name field must take '" + room + "'");
        String sub = firstClassAndSubtype()[1];
        assertTrue(walks.increment(sub), "Count one '" + sub + "' in the room");
        assertTrue(walks.isFinishEnabled(), "'" + SiteWalkPage.COMPLETE_ROOM + "' must enable once the room has a count");
        assertTrue(walks.finishGrid(), "'" + SiteWalkPage.COMPLETE_ROOM + "' must return to the walk detail");
        List<String> rooms = walks.roomRows();
        logStep("Rooms: " + rooms);
        assertTrue(rooms.contains(room + ", 1 asset"), "The detail must list '" + room + ", 1 asset' — got " + rooms);
        assertTrue(walks.isTextVisible(SiteWalkPage.START_NEXT_ROOM), "After the first room the action reads '" + SiteWalkPage.START_NEXT_ROOM + "'");
        assertTrue(eventually(walks::isCompleteWalkEnabled, 6), "A counted room must unlock '" + SiteWalkPage.COMPLETE_WALK + "'");
        logStepWithScreenshot("TC_SWC_06: room flow verified");
    }

    @Test(priority = 7)
    public void TC_SWC_07_completeWalkMarksItComplete() {
        featureTest(FEATURE, "TC_SWC_07 - 'Complete Site Walk' marks the walk complete (list check + backend status)");
        newCountWalk("TC_SWC_07");
        String title = walks.walkTitle();
        assertTrue(walks.openCountWithoutLocation(), "Open the grid");
        String sub = firstClassAndSubtype()[1];
        assertTrue(walks.increment(sub) && walks.finishGrid(), "Count one asset and save");
        assertTrue(eventually(walks::isCompleteWalkEnabled, 6), "Completion must be unlocked");
        assertTrue(walks.completeWalk(), "'" + SiteWalkPage.COMPLETE_WALK + "' must complete the walk");
        assertTrue(eventually(() -> {
            TestDataApi.SiteWalkRow r = walkRow(walkId);
            return r != null && "complete".equals(r.status);
        }, 30), "Backend status must become 'complete' — got " + walkRow(walkId));
        if (!walks.isListShown()) walks.backToList();
        assertTrue(walks.waitForList(10_000), "Back on the list");
        List<WalkRow> mine = walks.walkRows().stream().filter(r -> r.label.contains(title)).toList();
        logStep("Rows titled '" + title + "': " + mine);
        if (mine.size() == 1) {
            assertTrue(mine.get(0).complete && SiteWalkPage.ICON_COMPLETE.equals(mine.get(0).icon),
                    "The completed walk's row must show the complete check — " + mine.get(0));
        } else {
            logStep("Row check skipped: " + mine.size() + " rows share the title '" + title + "' (backend status verified)");
        }
        logStepWithScreenshot("TC_SWC_07: walk completed — " + walkRow(walkId));
    }

    @AfterMethod(alwaysRun = true)
    public void deleteCaseWalk(ITestResult result) {
        captureFailureState(result);
        walkId = null;
        try {
            if (walks != null && walks.isGridShown()) walks.backFromGrid("Discard");
            if (walks != null && !walks.isListShown()) walks.backToList();
        } catch (Exception ignored) { }
        syncLeftoverQueuedWalks(result);
        deleteTrackedWalksNow();
    }
}
