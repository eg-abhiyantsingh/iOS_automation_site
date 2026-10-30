package com.egalvanic.tests;

import com.egalvanic.base.ZP3914BaseTest;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.SessionFormsTabPage;
import com.egalvanic.pages.SessionFormsTabPage.FormTypeRow;
import com.egalvanic.pages.SessionFormsTabPage.InstanceRow;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * ZP-3914 (iOS 1.59) — "NETA-3 Forms Tab": the work-order session's 'Forms' tab, Level 1 (one row per form type
 * with a submitted/total tally) and Level 2 (one row per form instance, search, room/status filters, single-form
 * editor). Read-only: nothing here changes a form — the multi-edit and ghost-value classes own the writes.
 *
 * Fixture: QA-WT10 'NETA Testing' (self-provisioned by {@link com.egalvanic.base.WorkTypeBaseTest}), whose forms
 * live on the session; a tenant with no forms there SKIPs with the reason instead of passing vacuously.
 */
public final class ZP3914_FormsTab_Test extends ZP3914BaseTest {

    private static final String FEATURE = "Session Forms tab (ZP-3914)";
    private static final WorkTypeCatalog FIXTURE = WorkTypeCatalog.NETA_TESTING;

    /** Open the fixture session's Forms tab and return its Level-1 rows (SKIP when there are none). */
    private List<FormTypeRow> openFormsTabOrSkip(String tcId) {
        loginAndWaitForTabs(tcId);
        openFixtureOrSkip(FIXTURE, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(), tcId + ": session details did not settle open");
        assertTrue(formsTab.isFormsTabOffered(), "A work-order session must offer the 'Forms' tab (ZP-3914 Forms Tab)");
        assertTrue(formsTab.openFormsTab(), "The 'Forms' tab must open and list form types");
        List<FormTypeRow> rows = formsTab.formTypeRows();
        logStep("Forms tab rows: " + rows);
        skipIfPreconditionMissing(() -> !rows.isEmpty(), tcId + ": '" + FIXTURE.fixtureName() + "' has no forms yet");
        return rows;
    }

    /** Open the form type with the most instances (Level 2). */
    private FormTypeRow openBusiestType(List<FormTypeRow> rows) {
        FormTypeRow t = rows.stream().max(java.util.Comparator.comparingInt(r -> r.total)).orElseThrow();
        assertTrue(formsTab.openFormType(t.label), "Tapping '" + t.label + "' must open its form list (Level 2)");
        return t;
    }

    @Test(priority = 1)
    public void TC_FT_01_levelOneRowsCarryConsistentTallies() {
        featureTest(FEATURE, "TC_FT_01 - Level 1: one row per form type with '<submitted>/<total>' and a matching state");
        List<FormTypeRow> rows = openFormsTabOrSkip("TC_FT_01");
        for (FormTypeRow r : rows) {
            assertTrue(r.total > 0, "A listed form type has at least one form: " + r);
            assertTrue(r.submitted >= 0 && r.submitted <= r.total, "Submitted can't exceed the total: " + r);
            if ("Submitted".equals(r.state)) {
                assertEquals(r.submitted, r.total, "'Submitted' is shown only when every form of the type is submitted: " + r);
            } else {
                int drafts = Integer.parseInt(r.state.replaceAll("\\D+", ""));
                assertTrue(drafts >= 1 && drafts <= r.total - r.submitted,
                        "'N drafts' must count unsubmitted forms only (1.." + (r.total - r.submitted) + "): " + r);
            }
        }
        logStepWithScreenshot("TC_FT_01: " + rows.size() + " form type(s) verified");
    }

    @Test(priority = 2)
    public void TC_FT_02_levelTwoListsEveryInstance() {
        featureTest(FEATURE, "TC_FT_02 - Level 2 lists one row per form (count = Level-1 total) with tally + status chip");
        FormTypeRow t = openBusiestType(openFormsTabOrSkip("TC_FT_02"));
        assertTrue(formsTab.isInstanceListShown(), "Level 2 shows '" + SessionFormsTabPage.SEARCH_PLACEHOLDER
                + "' and '" + SessionFormsTabPage.FILTER_BUTTON + "'");
        assertTrue(formsTab.isSelectEntryOffered(), "Level 2 offers 'Select' (multi-edit entry: nav button ≤1.69, row long-press menu v1.73)");
        List<InstanceRow> rows = formsTab.instanceRows();
        logStep("Instances of '" + t.head + "': " + rows);
        if (t.total <= 7) {   // everything fits on one screen
            assertEquals(rows.size(), t.total, "Level 2 must list every form the Level-1 tally counts (" + t.label + ")");
        } else {
            assertFalse(rows.isEmpty(), "Level 2 must list the forms of '" + t.label + "'");
        }
        long submitted = rows.stream().filter(r -> "Submitted".equals(r.status)).count();
        if (rows.size() == t.total) {
            assertEquals((int) submitted, t.submitted, "Level-2 'Submitted' chips must add up to the Level-1 tally " + t.submitted);
        }
        for (InstanceRow r : rows) {
            assertTrue(r.tally.matches("^\\d+/\\d+$"), "Each row shows a 'done/total' section tally — " + r);
            assertFalse(r.asset.isEmpty(), "Each row names its asset (or 'No asset') — " + r);
        }
        logStepWithScreenshot("TC_FT_02: Level 2 contract verified");
    }

    @Test(priority = 3)
    public void TC_FT_03_searchNarrowsByAssetOrLocation() {
        featureTest(FEATURE, "TC_FT_03 - Search matches asset or location; a miss says 'No matches'; clearing restores the list");
        openBusiestType(openFormsTabOrSkip("TC_FT_03"));
        List<InstanceRow> all = formsTab.instanceRows();
        InstanceRow probe = all.stream().filter(r -> !r.room.isEmpty()).findFirst()
                .orElse(all.stream().filter(r -> !"No asset".equals(r.asset)).findFirst().orElse(null));
        skipIfPreconditionMissing(() -> probe != null, "TC_FT_03: no row with an asset or room to search for");
        String term = probe.room.isEmpty() ? probe.asset : probe.room;
        assertTrue(formsTab.search(term), "The search field must accept typing");
        List<InstanceRow> hits = formsTab.instanceRows();
        logStep("Search '" + term + "' → " + hits);
        assertFalse(hits.isEmpty(), "Searching '" + term + "' (taken from a listed row) must find it");
        for (InstanceRow r : hits) {
            assertTrue((r.asset + " " + r.room).toLowerCase().contains(term.toLowerCase()),
                    "Every hit must match '" + term + "' by asset or location — " + r);
        }
        assertTrue(hits.size() <= all.size(), "Search can only narrow the list");
        assertTrue(formsTab.search("zzqq-no-form"), "Search must accept a no-match query");
        assertTrue(eventually(formsTab::isNoMatchesShown, 4), "A miss must say '" + SessionFormsTabPage.NO_MATCHES + "'");
        assertTrue(formsTab.instanceRows().isEmpty(), "A miss lists no rows");
        assertTrue(formsTab.clearSearch(), "The search clear button must be there");
        assertTrue(eventually(() -> formsTab.instanceRows().size() == all.size(), 5),
                "Clearing the search must restore all " + all.size() + " rows — got " + formsTab.instanceRows().size());
        logStepWithScreenshot("TC_FT_03: search verified");
    }

    @Test(priority = 4)
    public void TC_FT_04_statusAndRoomFilters() {
        featureTest(FEATURE, "TC_FT_04 - Filter sheet: status All/Draft/Submitted + rooms; 'Clear filters' resets");
        FormTypeRow t = openBusiestType(openFormsTabOrSkip("TC_FT_04"));
        int all = formsTab.instanceRows().size();
        assertTrue(formsTab.openFilters(), "'" + SessionFormsTabPage.FILTER_BUTTON + "' must open the 'Filters' sheet");
        Map<String, Boolean> opts = formsTab.filterOptions();
        logStep("Filter options: " + opts);
        for (String o : List.of("All", "Draft", "Submitted", "All rooms")) {
            assertTrue(opts.containsKey(o), "The filter sheet offers '" + o + "' — " + opts.keySet());
        }
        assertEquals(opts.get("All"), Boolean.TRUE, "Status 'All' is active by default");
        assertEquals(opts.get("All rooms"), Boolean.TRUE, "'All rooms' is active by default");
        assertFalse(formsTab.isTextVisible(SessionFormsTabPage.CLEAR_FILTERS), "'Clear filters' only appears once a filter is active");
        assertTrue(formsTab.chooseFilter("Submitted"), "Choosing 'Submitted' must mark it active");
        assertEquals(formsTab.filterOptions().get("All"), Boolean.FALSE, "Only one status can be active");
        assertTrue(formsTab.closeFilters(), "'Done' closes the sheet");
        List<InstanceRow> sub = formsTab.instanceRows();
        logStep("Submitted filter → " + sub);
        if (t.submitted == 0) {
            assertTrue(sub.isEmpty() && formsTab.isNoFormsShown(), "With nothing submitted the filter shows '" + SessionFormsTabPage.NO_FORMS + "'");
        } else {
            assertFalse(sub.isEmpty(), "The type has " + t.submitted + " submitted form(s) — the filter must show them");
            assertTrue(sub.stream().allMatch(r -> "Submitted".equals(r.status)), "The 'Submitted' filter shows only submitted forms — " + sub);
        }
        assertTrue(formsTab.openFilters(), "Re-open the sheet");
        assertTrue(formsTab.isTextVisible(SessionFormsTabPage.CLEAR_FILTERS), "With a filter active the sheet offers '" + SessionFormsTabPage.CLEAR_FILTERS + "'");
        formsTab.resetFiltersFromOpenSheet();
        assertTrue(eventually(() -> formsTab.instanceRows().size() == all, 5),
                "'Clear filters' must restore all " + all + " rows — got " + formsTab.instanceRows().size());
        logStepWithScreenshot("TC_FT_04: filters verified");
    }

    @Test(priority = 5)
    public void TC_FT_05_rowOpensSingleFormEditor() {
        featureTest(FEATURE, "TC_FT_05 - Tapping a row opens that one form ('Edit Form', no multi-edit 'Editing:' chip); Close returns");
        openBusiestType(openFormsTabOrSkip("TC_FT_05"));
        List<InstanceRow> rows = formsTab.instanceRows();
        assertFalse(rows.isEmpty(), "Level 2 lists forms");
        assertTrue(formsTab.openInstance(rows.get(0)), "Tapping '" + rows.get(0) + "' must open its editor");
        assertTrue(formsTab.isEditorOpen(), "The single form opens in '" + SessionFormsTabPage.EDIT_FORM + "'");
        assertFalse(formsTab.isMultiEditOpen(), "A single form must not show the multi-edit '" + SessionFormsTabPage.EDITING_CHIP + "' chip");
        assertTrue(formsTab.closeEditor(true), "'Close' must leave the editor");
        assertTrue(eventually(() -> formsTab.instanceRows().size() == rows.size(), 6), "Back on Level 2 with the same rows");
        logStepWithScreenshot("TC_FT_05: single-form editor verified");
    }

    @AfterMethod(alwaysRun = true)
    public void leaveFormsTabClean(ITestResult result) {
        captureFailureState(result);
        try {
            if (formsTab != null && formsTab.isEditorOpen()) formsTab.closeEditor(true);
        } catch (Exception ignored) { }
    }
}
