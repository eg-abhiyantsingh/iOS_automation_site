package com.egalvanic.tests;

import com.egalvanic.base.ZP3914BaseTest;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.SessionFormsTabPage;
import com.egalvanic.pages.SessionFormsTabPage.FormTypeRow;
import com.egalvanic.pages.SessionFormsTabPage.InstanceRow;
import com.egalvanic.pages.WorkOrderFormsPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.Waits;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * ZP-3914 (iOS 1.59) — EG Forms "preload previous values" (ghost values, EGFormGhosts.swift): when an asset already
 * has a SUBMITTED instance of a form, a NEW instance of that form shows the submitted entries as blue placeholder
 * text in its empty fields, and multi-edit explains them with the banner
 * 'Blue italic values come from each asset's last submitted form — left untouched, they save on Submit.'
 *
 * Writes real data, so it is self-contained: on 'Switch-1' (room 'Optional Notes Room_21' of the NETA fixture,
 * linked on demand like ZP3928_EgFormsMatrix_Test) it adds form A, types a unique number, submits it, adds form B of
 * the same title and reads the ghost — then deletes BOTH instances (teardown repeats the delete if an assert fired).
 */
public final class ZP3914_GhostValues_Test extends ZP3914BaseTest {

    private static final String FEATURE = "EG Forms ghost values (ZP-3914)";
    private static final WorkTypeCatalog FIXTURE = WorkTypeCatalog.NETA_TESTING;
    private static final String ROOM = "Optional Notes Room_21";
    private static final String ASSET = "Switch-1";

    private WorkOrderFormsPage forms;
    /** Titles of instances this test added on ASSET (one entry per instance) — deleted in teardown. */
    private final List<String> created = new ArrayList<>();

    @BeforeMethod(alwaysRun = true)
    public void initForms() {
        if (DriverManager.isDriverActive()) forms = new WorkOrderFormsPage();
    }

    /** Session › Assets › ROOM with ASSET listed. */
    private void openAssetRoomOrSkip(String tcId) {
        loginAndWaitForTabs(tcId);
        openFixtureOrSkip(FIXTURE, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(), tcId + ": session details did not settle open");
        skipIfPreconditionMissing(() -> wo.tapSessionTab("Assets"), tcId + ": 'Assets' session tab not tappable");
        sleep(1200);
        skipIfPreconditionMissing(() -> forms.openRoomNamed(ROOM), tcId + ": room '" + ROOM + "' not reachable in " + FIXTURE.fixtureName());
        if (forms.assetRowFormCount(ASSET) < 0) {
            logStep("'" + ASSET + "' not in the room yet — linking it (fixture self-provisioning)");
            forms.linkExistingAsset(ASSET);
        }
        skipIfPreconditionMissing(() -> forms.assetRowFormCount(ASSET) >= 0, tcId + ": '" + ASSET + "' could not be linked into '" + ROOM + "'");
    }

    /** Asset row menu › Add Form › first catalogue form; returns its title with the editor open (null on failure). */
    private String addForm() {
        List<String> menu = forms.openAssetRowMenu(ASSET);
        if (!menu.contains(WorkOrderFormsPage.ADD_FORM_MENU_ITEM)) {
            logStep("'" + ASSET + "' menu has no '" + WorkOrderFormsPage.ADD_FORM_MENU_ITEM + "': " + menu);
            forms.pressVisibleButton("Cancel");
            return null;
        }
        forms.pressVisibleButton(WorkOrderFormsPage.ADD_FORM_MENU_ITEM);
        if (!Waits.until(forms::isAddProcedureSheetOpen, 8_000, 500)) return null;
        String title = forms.chooseFirstAddableForm();
        if (title != null) created.add(title);
        return title;
    }

    @Test(priority = 1, timeOut = 900_000)
    public void TC_GV_01_submittedEntryGhostsIntoTheNextInstance() {
        featureTest(FEATURE, "TC_GV_01 - Submit form A with a value; a new form B of the same title shows that value as its ghost placeholder");
        openAssetRoomOrSkip("TC_GV_01");
        String titleA = addForm();
        assertNotNull(titleA, "'Add Form' on '" + ASSET + "' must open a form in '" + WorkOrderFormsPage.EDIT_FORM_TITLE + "'");
        String[] fresh = forms.firstTextFieldState();
        skipIfPreconditionMissing(() -> fresh != null, "TC_GV_01: '" + titleA + "' has no text field to carry a ghost value");
        String value = String.valueOf(100 + System.currentTimeMillis() % 800) + ".25";
        String read = forms.fillFirstField(value);
        assertEquals(read, value, "Form A's first field must hold the typed value");
        String alert = formsTab.submitForm();
        logStep("Submit of form A — alert: '" + alert + "'");
        skipIfPreconditionMissing(() -> !alert.startsWith("Missing Required"),
                "TC_GV_01: '" + titleA + "' has required fields beyond the first — it cannot be submitted generically");
        assertTrue(Waits.until(forms::isAssetsInRoomOpen, 15_000, 600), "Submitting form A must return to 'Assets in Room'");
        logStepWithScreenshot("Form A '" + titleA + "' submitted with " + value);

        String titleB = addForm();
        assertNotNull(titleB, "A second 'Add Form' must open");
        skipIfPreconditionMissing(() -> titleA.equals(titleB), "TC_GV_01: the second add picked '" + titleB + "' instead of '" + titleA + "'");
        String[] ghost = forms.firstTextFieldState();
        assertNotNull(ghost, "Form B has the same first text field");
        logStep("Form B first field: value='" + ghost[0] + "' placeholder='" + ghost[1] + "'");
        assertEquals(ghost[1], value, "Form B's empty first field must show form A's submitted value as its ghost placeholder");
        assertTrue(ghost[0].isEmpty() || ghost[0].equals(ghost[1]),
                "The ghost is a placeholder, not an entry — the field's own value must still be empty (value '" + ghost[0] + "')");
        logStepWithScreenshot("TC_GV_01: ghost '" + value + "' shown in a fresh '" + titleB + "'");
        assertTrue(forms.closeEditForm(), "Close form B without entering anything");
    }

    @Test(priority = 2, timeOut = 900_000)
    public void TC_GV_02_multiEditExplainsGhostValues() {
        featureTest(FEATURE, "TC_GV_02 - Multi-edit over a form with ghost data shows the blue-italic explanation banner");
        openAssetRoomOrSkip("TC_GV_02");
        String titleA = addForm();
        assertNotNull(titleA, "'Add Form' must open a form");
        skipIfPreconditionMissing(() -> forms.firstTextFieldState() != null, "TC_GV_02: '" + titleA + "' has no text field");
        String value = String.valueOf(100 + System.currentTimeMillis() % 800) + ".75";
        assertEquals(forms.fillFirstField(value), value, "Form A takes the value");
        String alert = formsTab.submitForm();
        skipIfPreconditionMissing(() -> !alert.startsWith("Missing Required"), "TC_GV_02: '" + titleA + "' cannot be submitted generically");
        assertTrue(Waits.until(forms::isAssetsInRoomOpen, 15_000, 600), "Back on 'Assets in Room' after submitting");
        String titleB = addForm();
        skipIfPreconditionMissing(() -> titleA.equals(titleB), "TC_GV_02: second add picked '" + titleB + "'");
        assertTrue(forms.closeEditForm(), "Leave form B as an untouched draft (it carries ghosts)");
        forms.backToRoomList();
        assertTrue(formsTab.openFormsTab(), "Open the session 'Forms' tab");
        FormTypeRow type = formsTab.formTypeRows().stream().filter(r -> r.head.startsWith(titleA)).findFirst().orElse(null);
        assertNotNull(type, "The Forms tab must list '" + titleA + "' — rows " + formsTab.formTypeRows());
        assertTrue(formsTab.openFormType(type.label), "Open '" + type.label + "'");
        assertTrue(formsTab.search(ASSET), "Narrow the list to '" + ASSET + "'");
        List<InstanceRow> rows = formsTab.instanceRows();
        InstanceRow draftB = rows.stream().filter(r -> ASSET.equals(r.asset) && !"Submitted".equals(r.status)).findFirst().orElse(null);
        assertNotNull(draftB, "Form B (draft on '" + ASSET + "') must be listed — " + rows);
        InstanceRow other = rows.stream().filter(r -> r != draftB).findFirst().orElse(null);
        skipIfPreconditionMissing(() -> other != null, "TC_GV_02: multi-edit needs a second form of '" + titleA + "'");
        assertTrue(formsTab.enterSelectMode(), "Enter select mode");
        assertTrue(formsTab.toggleRow(draftB) && formsTab.toggleRow(other), "Select form B and one more");
        assertTrue(formsTab.openMultiEdit(), "'Edit' opens the multi-form editor");
        assertTrue(eventually(formsTab::isGhostBannerShown, 6), "Multi-edit over a form with ghost data must explain the blue values");
        assertEquals(formsTab.ghostBannerText(), SessionFormsTabPage.GHOST_BANNER, "Ghost banner copy");
        logStepWithScreenshot("TC_GV_02: ghost banner shown");
        assertTrue(formsTab.closeEditor(true), "Close the editor without saving");
        if (formsTab.isSelectMode()) formsTab.exitSelectMode();

        // Clean up HERE, through the Forms tab's own multi-select Delete — getting back into the room from this tab
        // is unreliable (two forms leaked that way, 2026-10-21), and it exercises the real delete path that
        // TC_ME_04 only cancels. Only when the filtered list is exactly this test's forms.
        List<InstanceRow> mine = formsTab.instanceRows().stream().filter(r -> ASSET.equals(r.asset)).toList();
        if (mine.size() == created.size()) {
            assertTrue(formsTab.enterSelectMode(), "Select mode for cleanup");
            for (InstanceRow r : mine) assertTrue(formsTab.toggleRow(r), "Select '" + r + "' for deletion");
            String deleteAlert = formsTab.deleteSelected("Delete");
            assertTrue(deleteAlert.startsWith("Delete " + mine.size() + " form"),
                    "Deleting asks 'Delete " + mine.size() + " forms?' — got '" + deleteAlert + "'");
            assertTrue(eventually(() -> formsTab.instanceRows().stream().noneMatch(r -> ASSET.equals(r.asset)), 10),
                    "Multi-select Delete must remove the selected forms — still " + formsTab.instanceRows());
            created.clear();
            logStep("Deleted this test's " + mine.size() + " form(s) via Forms tab › Select › Delete");
        } else {
            logStep("Forms tab lists " + mine.size() + " '" + ASSET + "' form(s) vs " + created.size() + " created — teardown deletes by room");
        }
    }

    /** Delete every instance this test added (back on the room screen first). */
    @AfterMethod(alwaysRun = true)
    public void deleteCreatedForms(ITestResult result) {
        captureFailureState(result);
        if (created.isEmpty() || forms == null) return;
        try {
            if (formsTab.isEditorOpen()) formsTab.closeEditor(true);
            if (forms.isEditFormOpen()) forms.closeEditForm();
            if (formsTab.isSelectMode()) formsTab.exitSelectMode();
            // Back to the room. The Assets tab keeps its room-search text and can leave the keyboard up, which made
            // one openRoomNamed() miss (retest 2026-10-21 leaked two forms) — retry from a fresh tab tap.
            for (int attempt = 1; attempt <= 3 && !forms.isAssetsInRoomOpen(); attempt++) {
                try { com.egalvanic.utils.DriverManager.getDriver().executeScript("mobile: hideKeyboard"); } catch (Exception ignored) { }
                wo.tapSessionTab("Assets");
                sleep(1500);
                if (!forms.openRoomNamed(ROOM)) System.out.println("🔁 ghost-test cleanup: room not opened (attempt " + attempt + ")");
            }
            for (String title : new ArrayList<>(created)) {
                boolean ok = forms.deleteFormInstance(ASSET, title);
                System.out.println((ok ? "🧹 deleted" : "⚠️ could not delete") + " ghost-test form '" + title + "' on '" + ASSET + "'");
                if (ok) created.remove(title);
            }
        } catch (Exception e) {
            System.out.println("⚠️ ghost-test cleanup: " + e.getMessage());
        } finally {
            if (!created.isEmpty()) System.out.println("⚠️ LEAKED ghost-test forms on '" + ASSET + "': " + created);
            created.clear();
        }
    }
}
