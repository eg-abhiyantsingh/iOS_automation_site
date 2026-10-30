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
    /** Per title: instances on ASSET before this class first added one (v1.73 'Select a Form' read) — cleanup floor. */
    private final java.util.Map<String, Integer> baseline = new java.util.HashMap<>();

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

    /**
     * v1.73: 'Add Form' sits inside the 'Select a Form' sheet, and 'Forms (n)' opens that sheet only when the asset has
     * at least two forms — with exactly one it opens the form itself and there is no way to add another from the room
     * (finding EGF-ADD-01, 2026-10-30). Keep ASSET at ≥2 forms: attach one provisioning form through the web's
     * form-coverage / bulk-forms routes (idempotent: nothing happens when two are already there), then pull it into
     * the app with the room's Refresh. The provisioning form is fixture data and stays.
     */
    private void ensureAssetHasTwoForms(String tcId) {
        if (!com.egalvanic.constants.AppConstants.appVersionAtLeast(1, 73)) return;
        com.egalvanic.api.TestDataApi a = api();
        if (a == null) return;
        try {
            com.google.gson.JsonObject wos = com.google.gson.JsonParser.parseString(a.listWorkOrdersJson(FIXTURE.fixtureName())).getAsJsonObject();
            com.google.gson.JsonArray items = wos.get("data").isJsonArray() ? wos.getAsJsonArray("data")
                    : wos.getAsJsonObject("data").getAsJsonArray("items");
            for (com.google.gson.JsonElement w : items) {
                String session = w.getAsJsonObject().get("id").getAsString();
                java.net.http.HttpResponse<String> r = a.get("/eg-form-instance/by-session/" + session + "/form-coverage?limit=500&offset=0");
                if (r.statusCode() / 100 != 2) continue;
                com.google.gson.JsonObject cov = com.google.gson.JsonParser.parseString(r.body()).getAsJsonObject().getAsJsonObject("data");
                for (com.google.gson.JsonElement e : cov.getAsJsonArray("assets")) {
                    com.google.gson.JsonObject asset = e.getAsJsonObject();
                    if (!ASSET.equals(asset.get("label").getAsString()) || !ROOM.equals(asset.get("room_name").getAsString())) continue;
                    java.util.Set<String> attached = asset.getAsJsonObject("forms").keySet();
                    if (attached.size() >= 2) return;
                    String classKey = asset.get("class_key").getAsString();
                    String formId = null, formTitle = null;
                    for (com.google.gson.JsonElement c : cov.getAsJsonArray("classes")) {
                        if (!classKey.equals(c.getAsJsonObject().get("class_key").getAsString())) continue;
                        for (com.google.gson.JsonElement f : c.getAsJsonObject().getAsJsonArray("forms")) {
                            String id = f.getAsJsonObject().get("eg_form_id").getAsString();
                            String t = f.getAsJsonObject().get("title").getAsString();
                            // Never the title the ghost tests add (cleanup deletes that title down to its baseline).
                            if (attached.contains(id) || t.startsWith("Bolted Connections") || t.matches("(?i)^test[\\s_-].*")) continue;
                            if (formTitle == null || t.compareTo(formTitle) < 0) { formId = id; formTitle = t; }
                        }
                    }
                    if (formId == null) { logStep(tcId + ": no second form applies to '" + classKey + "' — cannot provision"); return; }
                    String body = "{\"node_ids\":[\"" + asset.get("node_id").getAsString() + "\"],\"eg_form_ids\":[\"" + formId + "\"],\"action\":\"add\"}";
                    java.net.http.HttpResponse<String> add = a.post("/eg-form-instance/by-session/" + session + "/bulk-forms", body);
                    logStep(tcId + ": provisioned '" + formTitle + "' on '" + ASSET + "' (v1.73 needs ≥2 forms for 'Add Form') → HTTP "
                            + add.statusCode() + " " + add.body().substring(0, Math.min(160, add.body().length())));
                    forms.refreshRoom();
                    Waits.until(() -> forms.assetRowFormCount(ASSET) >= 0, 30_000, 800);
                    sleep(2000);
                    return;
                }
            }
        } catch (RuntimeException e) {
            logStep(tcId + ": provisioning a second form failed — " + e.getMessage());
        }
    }

    /** Asset row menu › Add Form › first catalogue form; returns its title with the editor open (null on failure). */
    private String addForm() {
        List<String> menu = forms.openAssetRowMenu(ASSET);
        if (menu.isEmpty()) {
            // Right after a room Refresh the menu can draw after the 5 s read (seen 2026-10-30: 'Forms (2)' on screen,
            // menu read []). Dismiss whatever is up and read once more.
            try { DriverManager.getDriver().executeScript("mobile: tap", java.util.Map.of("x", 200, "y", 40)); } catch (Exception ignored) { }
            sleep(1500);
            menu = forms.openAssetRowMenu(ASSET);
        }
        if (!WorkOrderFormsPage.offersAddForm(menu)) {
            logStep("'" + ASSET + "' menu offers no way to add a form: " + menu);
            forms.pressVisibleButton("Cancel");
            return null;
        }
        // 1.69: menu 'Add Form'; v1.73: 'Forms (n)' › 'Select a Form' › 'Add Form' — both end on 'Add Procedure'.
        if (!forms.pressAddForm(menu)) return null;
        String title = forms.chooseFirstAddableForm();
        if (title != null) {
            baseline.putIfAbsent(title, forms.lastCountOf(title));
            created.add(title);
        }
        return title;
    }

    @Test(priority = 1, timeOut = 900_000)
    public void TC_GV_01_submittedEntryGhostsIntoTheNextInstance() {
        featureTest(FEATURE, "TC_GV_01 - Submit form A with a value; a new form B of the same title shows that value as its ghost placeholder");
        openAssetRoomOrSkip("TC_GV_01");
        ensureAssetHasTwoForms("TC_GV_01");
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
        ensureAssetHasTwoForms("TC_GV_02");
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
        assertTrue(formsTab.enterSelectModeOn(draftB), "Enter select mode on form B");
        assertTrue(formsTab.ensureSelected(other), "Select form B and one more");
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
            // Start select mode ON one of this test's rows: v1.73 pre-selects the long-pressed row, and a delete must
            // never carry a row that is not ours.
            assertTrue(formsTab.enterSelectModeOn(mine.get(0)), "Select mode for cleanup");
            for (InstanceRow r : mine) assertTrue(formsTab.ensureSelected(r), "Select '" + r + "' for deletion");
            int sel = formsTab.selectionCircles()[0];
            assertEquals(sel, mine.size(), "Only this test's " + mine.size() + " form(s) may be selected before Delete — " + sel + " are");
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

    // ── EG Forms renderer (ZP-3743, shipped with iOS 1.59): fixed chrome, index drawer, no in-form Delete ──────────

    private static final String RENDERER = "EG Forms renderer (ZP-3914)";

    @Test(priority = 3, timeOut = 900_000)
    public void TC_EGF_01_editorChromeIsFixedAndHasNoDelete() {
        featureTest(RENDERER, "TC_EGF_01 - Open form: 'Close' top-left, fixed footer 'Sections' · 'Save' · 'Submit', and no 'Delete' inside the form");
        openAssetRoomOrSkip("TC_EGF_01");
        String title = ASSET + "'s form";
        assertTrue(forms.openExistingForm(ASSET), "'" + ASSET + "' › Forms must open its form in '" + WorkOrderFormsPage.EDIT_FORM_TITLE + "'");
        List<com.egalvanic.base.BasePage.SnapNode> buttons = forms.visibleButtons();
        logStep("Editor buttons: " + buttons.stream().map(b -> b.text() + "@" + b.y).toList());
        int h = DriverManager.getDriver().manage().window().getSize().getHeight();
        assertTrue(buttons.stream().anyMatch(b -> b.is("Close") && b.y < 200), "'Close' sits in the top bar");
        for (String f : List.of(WorkOrderFormsPage.SECTIONS, "Submit")) {
            assertTrue(buttons.stream().anyMatch(b -> b.is(f) && b.y > h - 200), "'" + f + "' sits in the fixed footer (bottom 200 pt)");
        }
        assertTrue(buttons.stream().anyMatch(b -> (b.is("Save") || b.is("Save Draft")) && b.y > h - 200), "'Save' sits in the fixed footer");
        assertTrue(buttons.stream().noneMatch(b -> b.text().matches("(?i)^delete( form)?$")),
                "Delete moved off the open form (long-press / swipe / asset form picker) — no 'Delete' inside the editor");
        // The footer is fixed: scrolling the form must not move it.
        int submitY = buttons.stream().filter(b -> b.is("Submit")).mapToInt(b -> b.y).findFirst().orElse(-1);
        swipeUpOnce();
        int submitY2 = forms.visibleButtons().stream().filter(b -> b.is("Submit")).mapToInt(b -> b.y).findFirst().orElse(-1);
        assertEquals(submitY2, submitY, "'Submit' stays put while the form scrolls (fixed footer)");
        logStepWithScreenshot("TC_EGF_01: editor chrome verified on '" + title + "'");
        assertTrue(forms.closeEditForm(), "Close the untouched form");
    }

    @Test(priority = 4, timeOut = 900_000)
    public void TC_EGF_02_footerStaysAboveTheKeyboard() {
        featureTest(RENDERER, "TC_EGF_02 - With the keyboard up, 'Save' and 'Submit' stay visible above it");
        openAssetRoomOrSkip("TC_EGF_02");
        String title = ASSET + "'s form";
        assertTrue(forms.openExistingForm(ASSET), "'" + ASSET + "' › Forms must open its form");
        skipIfPreconditionMissing(() -> forms.focusFirstTextField(), "TC_EGF_02: '" + title + "' has no text field to raise the keyboard");
        int kb = forms.keyboardTop();
        List<com.egalvanic.base.BasePage.SnapNode> buttons = forms.visibleButtons();
        logStep("Keyboard top " + kb + "; buttons " + buttons.stream().map(b -> b.text() + "@" + b.y).toList());
        for (String f : List.of("Submit")) {
            assertTrue(buttons.stream().anyMatch(b -> b.is(f) && b.y + b.h <= kb), "'" + f + "' stays above the keyboard (top " + kb + ")");
        }
        assertTrue(buttons.stream().anyMatch(b -> (b.is("Save") || b.is("Save Draft")) && b.y + b.h <= kb), "'Save' stays above the keyboard");
        logStepWithScreenshot("TC_EGF_02: footer above the keyboard");
        assertTrue(forms.closeEditForm(), "Close the form without saving");
    }

    @Test(priority = 5, timeOut = 900_000)
    public void TC_EGF_03_sectionsDrawerJumpsToAContainer() {
        featureTest(RENDERER, "TC_EGF_03 - 'Sections' opens the index drawer; choosing a section scrolls the form to it");
        openAssetRoomOrSkip("TC_EGF_03");
        assertTrue(forms.openExistingForm(ASSET), "'" + ASSET + "' › Forms must open its form");
        List<String> entries = forms.openSectionsDrawer();
        assertFalse(entries.isEmpty(), "'Sections' must open an index of the form's sections");
        logStep("Sections index: " + entries);
        String target = entries.get(entries.size() - 1);   // the last section: the one furthest from the top
        int h = DriverManager.getDriver().manage().window().getSize().getHeight();
        assertTrue(forms.pressText(target), "Choose '" + target + "' in the index");
        assertTrue(eventually(() -> { int y = forms.textY(target); return y > 80 && y < h - 200; }, 6),
                "The form scrolls '" + target + "' into view (y " + forms.textY(target) + ")");
        logStepWithScreenshot("TC_EGF_03: jumped to '" + target + "'");
        assertTrue(forms.closeEditForm(), "Close the untouched form");
    }

    private void swipeUpOnce() {
        try {
            DriverManager.getDriver().executeScript("mobile: swipe", java.util.Map.of("direction", "up"));
        } catch (Exception e) {
            System.out.println("⚠️ swipe: " + e.getMessage());
        }
        sleep(1200);
    }

    /**
     * v1.73 cleanup: for each title this test added, open its Forms-tab type row(s), narrow to ASSET and delete
     * newest-first until the asset is back at its pre-test count ({@link #baseline}). Removes from {@link #created}
     * what it deleted; whatever remains is reported as leaked by the caller.
     */
    private void deleteViaFormsTab() {
        for (String title : new java.util.LinkedHashSet<>(created)) {
            int floor = baseline.getOrDefault(title, 0);
            int toDelete = (int) created.stream().filter(title::equals).count();
            if (!formsTab.openFormsTab()) { System.out.println("⚠️ ghost cleanup: Forms tab did not open"); return; }
            for (FormTypeRow type : formsTab.formTypeRows().stream().filter(r -> r.head.equals(title)).toList()) {
                if (toDelete == 0) break;
                if (!formsTab.openFormType(type.label)) continue;
                formsTab.search(ASSET);
                List<InstanceRow> mine = new ArrayList<>(formsTab.instanceRows().stream().filter(r -> ASSET.equals(r.asset)).toList());
                java.util.Collections.reverse(mine);   // newest last in the list → delete from the bottom up
                for (InstanceRow r : mine) {
                    int onAsset = (int) formsTab.instanceRows().stream().filter(x -> ASSET.equals(x.asset)).count();
                    if (toDelete == 0 || onAsset <= floor) break;
                    boolean ok = formsTab.deleteInstanceRow(r);
                    System.out.println((ok ? "🧹 deleted" : "⚠️ could not delete") + " ghost-test form '" + title + "' on '" + ASSET + "' (" + r + ")");
                    if (ok) { toDelete--; created.remove(title); }
                }
                formsTab.clearSearch();
                formsTab.backToFormTypes();
            }
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
            if (!forms.lastSelectFormRows.isEmpty() || com.egalvanic.constants.AppConstants.appVersionAtLeast(1, 73)) {
                // v1.73: the room no longer lists an asset's forms, so the long-press delete there is gone — delete
                // through the session Forms tab (row long-press › Delete), never below the pre-test count.
                deleteViaFormsTab();
                return;
            }
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
