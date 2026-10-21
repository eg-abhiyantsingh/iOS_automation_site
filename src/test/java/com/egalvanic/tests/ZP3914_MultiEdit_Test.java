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

/**
 * ZP-3914 (iOS 1.59) — "Multi-edit working sets": Forms tab › form type › 'Select' → pick forms → 'Edit' opens ONE
 * editor over all of them ('Editing:' + '<N> assets · <M> submitted', per-section 'Copy this section to the other
 * assets' / 'Mark section done', footer Sections · Save Draft · Submit); 'Mark As..' (Draft / Submitted) and
 * 'Delete' (confirmation) act on the selection.
 *
 * Non-destructive by design: the editor is closed without saving, the Mark As menu is dismissed without choosing,
 * and the delete confirmation is answered 'Cancel' — the fixture's forms are shared by every run.
 */
public final class ZP3914_MultiEdit_Test extends ZP3914BaseTest {

    private static final String FEATURE = "Multi-edit working sets (ZP-3914)";
    private static final WorkTypeCatalog FIXTURE = WorkTypeCatalog.NETA_TESTING;

    /** Forms tab › the form type with the most instances (≥2 needed), in select mode. Returns the type row. */
    private FormTypeRow selectModeOrSkip(String tcId) {
        loginAndWaitForTabs(tcId);
        openFixtureOrSkip(FIXTURE, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(), tcId + ": session details did not settle open");
        assertTrue(formsTab.openFormsTab(), "The session 'Forms' tab must open");
        List<FormTypeRow> types = formsTab.formTypeRows();
        FormTypeRow t = types.stream().max(java.util.Comparator.comparingInt(r -> r.total)).orElse(null);
        skipIfPreconditionMissing(() -> t != null && t.total >= 2, tcId + ": multi-edit needs a form type with ≥2 forms — " + types);
        assertTrue(formsTab.openFormType(t.label), "Open '" + t.label + "'");
        assertTrue(formsTab.enterSelectMode(), "'" + SessionFormsTabPage.SELECT + "' must switch the list to select mode ('All' + 'Done')");
        return t;
    }

    /** Select the first {@code n} rows; returns them. */
    private List<InstanceRow> selectFirst(int n) {
        List<InstanceRow> rows = formsTab.instanceRows();
        assertTrue(rows.size() >= n, "Need " + n + " rows to select — got " + rows);
        for (int i = 0; i < n; i++) {
            assertTrue(formsTab.toggleRow(rows.get(i)), "Tapping the circle of '" + rows.get(i) + "' must select it");
        }
        return rows.subList(0, n);
    }

    @Test(priority = 1)
    public void TC_ME_01_selectModeAndSelectionBar() {
        featureTest(FEATURE, "TC_ME_01 - Select mode: a circle per row, the action bar appears with a selection, 'Done' exits");
        selectModeOrSkip("TC_ME_01");
        int rows = formsTab.instanceRows().size();
        int[] circles = formsTab.selectionCircles();
        logStep("Circles selected/unselected: " + circles[0] + "/" + circles[1] + " for " + rows + " rows");
        assertEquals(circles[0], 0, "Nothing is selected on entering select mode");
        assertEquals(circles[1], rows, "Every visible row gets an empty selection circle");
        assertFalse(formsTab.isSelectionBarShown() && formsTab.isEditEnabled(), "With nothing selected 'Edit' must not be actionable");
        selectFirst(2);
        assertEquals(formsTab.selectionCircles()[0], 2, "Two rows must read selected");
        assertTrue(formsTab.isSelectionBarShown(), "A selection shows the bar 'Edit' · '" + SessionFormsTabPage.MARK_AS + "' · 'Delete'");
        assertTrue(formsTab.isEditEnabled(), "'Edit' must be enabled with forms selected");
        assertTrue(formsTab.exitSelectMode(), "'Done' must leave select mode ('Select' is back)");
        assertEquals(formsTab.selectionCircles()[0] + formsTab.selectionCircles()[1], 0, "No circles outside select mode");
        logStepWithScreenshot("TC_ME_01: select mode verified");
    }

    @Test(priority = 2)
    public void TC_ME_02_multiEditorSummarisesTheWorkingSet() {
        featureTest(FEATURE, "TC_ME_02 - 'Edit' on 2 forms opens one editor: 'Editing:', '2 assets · M submitted', section tools, footer");
        selectModeOrSkip("TC_ME_02");
        List<InstanceRow> picked = selectFirst(2);
        long submitted = picked.stream().filter(r -> "Submitted".equals(r.status)).count();
        assertTrue(formsTab.openMultiEdit(), "'Edit' must open the multi-form editor");
        assertTrue(formsTab.isTextVisible(SessionFormsTabPage.EDIT_FORM), "Editor title '" + SessionFormsTabPage.EDIT_FORM + "'");
        int[] summary = formsTab.multiEditSummary();
        assertNotNull(summary, "The editor states '<N> assets · <M> submitted'");
        logStep("Working set summary: " + summary[0] + " assets · " + summary[1] + " submitted");
        assertEquals(summary[0], 2, "The working set is the 2 selected forms");
        assertEquals(summary[1], (int) submitted, "'submitted' must count the selected forms already submitted (" + picked + ")");
        assertTrue(formsTab.sectionToolCount(SessionFormsTabPage.COPY_SECTION) >= 1,
                "Each section offers '" + SessionFormsTabPage.COPY_SECTION + "'");
        assertTrue(formsTab.sectionToolCount(SessionFormsTabPage.MARK_SECTION_DONE) >= 1,
                "Each section offers '" + SessionFormsTabPage.MARK_SECTION_DONE + "'");
        assertEquals(formsTab.editorFooter(), List.of("Sections", "Save Draft", "Submit"), "Footer actions: Sections · Save Draft · Submit");
        assertTrue(formsTab.closeEditor(true), "'Close' without edits must leave the editor");
        assertTrue(eventually(() -> !formsTab.isSelectMode(), 5), "Closing the editor ends select mode");
        logStepWithScreenshot("TC_ME_02: multi-edit editor verified");
    }

    @Test(priority = 3)
    public void TC_ME_03_markAsMenuOffersDraftAndSubmitted() {
        featureTest(FEATURE, "TC_ME_03 - 'Mark As..' offers Draft / Submitted; dismissing it changes nothing");
        selectModeOrSkip("TC_ME_03");
        List<String> before = formsTab.instanceRows().stream().map(r -> r.status).toList();
        selectFirst(2);
        List<String> items = formsTab.openMarkAsMenu();
        logStep("Mark As menu: " + items);
        assertTrue(items.contains("Draft") && items.contains("Submitted"), "'" + SessionFormsTabPage.MARK_AS + "' offers Draft and Submitted — " + items);
        formsTab.dismissMenu();
        assertTrue(formsTab.exitSelectMode(), "Leave select mode");
        assertEquals(formsTab.instanceRows().stream().map(r -> r.status).toList(), before, "Dismissing the menu must not change any status");
        logStepWithScreenshot("TC_ME_03: Mark As menu verified");
    }

    @Test(priority = 4)
    public void TC_ME_04_deleteAsksAndCancelKeepsForms() {
        featureTest(FEATURE, "TC_ME_04 - Delete on 2 forms asks 'Delete 2 forms?' with the data-loss warning; Cancel keeps them");
        FormTypeRow t = selectModeOrSkip("TC_ME_04");
        int before = formsTab.instanceRows().size();
        selectFirst(2);
        String alert = formsTab.deleteSelected("Cancel");
        logStep("Delete confirmation: " + alert);
        assertTrue(alert.startsWith("Delete 2 forms?"), "Delete must ask 'Delete 2 forms?' first — got '" + alert + "'");
        assertTrue(alert.contains(SessionFormsTabPage.DELETE_MESSAGE), "The question warns '" + SessionFormsTabPage.DELETE_MESSAGE + "'");
        assertTrue(eventually(() -> formsTab.instanceRows().size() == before, 5), "'Cancel' must keep all " + before + " forms");
        formsTab.exitSelectMode();
        logStepWithScreenshot("TC_ME_04: delete confirmation verified on '" + t.head + "'");
    }

    @Test(priority = 5)
    public void TC_ME_05_allSelectsEveryRow() {
        featureTest(FEATURE, "TC_ME_05 - 'All' selects every form of the type");
        selectModeOrSkip("TC_ME_05");
        int rows = formsTab.instanceRows().size();
        assertTrue(formsTab.selectAll(), "'All' must be pressable in select mode");
        assertTrue(eventually(() -> formsTab.selectionCircles()[0] == rows, 4),
                "'All' must select every visible row (" + rows + ") — selected " + formsTab.selectionCircles()[0]);
        assertTrue(formsTab.isSelectionBarShown(), "The action bar shows for the full selection");
        assertTrue(formsTab.exitSelectMode(), "'Done' exits select mode");
        logStepWithScreenshot("TC_ME_05: select-all verified");
    }

    @AfterMethod(alwaysRun = true)
    public void leaveSelectModeClean(ITestResult result) {
        captureFailureState(result);
        try {
            if (formsTab == null) return;
            if (formsTab.isEditorOpen()) formsTab.closeEditor(true);
            if (formsTab.isSelectMode()) formsTab.exitSelectMode();
        } catch (Exception ignored) { }
    }
}
