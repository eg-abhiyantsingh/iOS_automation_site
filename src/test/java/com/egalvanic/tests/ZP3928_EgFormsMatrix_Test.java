package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.WorkOrderFormsPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * ZP-3928 (iOS 1.56) §4 — "EG Forms: addable and fillable on every work order type".
 *
 * 1.69 contract (live 2026-10-05): asset rows no longer open a form screen on tap — forms are added from the
 * asset LONG-PRESS menu ('Add Form' → 'Add Procedure' sheet → choosing a form creates it and opens 'Edit Form').
 * Only 5 of the 13 QA-WT fixtures carry generated form instances (backend eg_form_instances: Cleaning, CTT,
 * De-Energized Visual, Insulation Resistance, NETA), so each case ADDS one form, fills it, checks the badge
 * grew by one and DELETES it again — the same proof on every type, no data left behind.
 *
 * One data-driven case per service-backed work type (13 — {@link WorkTypeCatalog#SERVICE_COUNT}),
 * each riding the QA-WT fixture family that {@link WorkTypeBaseTest#openFixtureOrSkip} self-provisions
 * on the landed site. Per type the case proves BOTH halves of the acceptance criterion:
 *
 *   ADDABLE  — the asset menu offers 'Add Form', the Add Procedure sheet lists a form, choosing it opens
 *              'Edit Form' and the asset's form badge grows by exactly one. Hard-asserted.
 *   FILLABLE — the new form's first text field holds the typed value (or, for a Pass/Fail-only form, the
 *              first step Result reads back 'Pass'). Hard-asserted.
 *   CLEANUP  — the added form is deleted and the badge returns to its start value (also in teardown).
 *
 * "General" (no backend service, work_type_id null) is a DISCOVERY case: whether its asset menu offers
 * 'Add Form' is app truth to capture, not assert.
 *
 * The deep form interactions (notes, fail card, save/reopen) are already covered per-type-agnostic
 * by WorkType_Forms_Test (TC_WT_FORM_001-045); this matrix answers only "does every type get forms".
 */
public final class ZP3928_EgFormsMatrix_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "EG Forms on every work order type (ZP-3928)";

    /** Site room + asset (class Switch — several standalone forms apply) used to provision empty fixtures. */
    private static final String PROVISION_ROOM = "Optional Notes Room_21";
    private static final String PROVISION_ASSET = "Switch-1";

    private WorkOrderFormsPage forms;

    @BeforeMethod(alwaysRun = true)
    public void initFormsPage() {
        if (!DriverManager.isDriverActive()) return;
        try {
            forms = new WorkOrderFormsPage();
        } catch (IllegalStateException e) {
            DriverManager.initDriver();
            forms = new WorkOrderFormsPage();
        }
    }

    @DataProvider(name = "serviceBackedWorkTypes")
    public Object[][] serviceBackedWorkTypes() {
        List<WorkTypeCatalog> types = WorkTypeCatalog.serviceBacked();
        // -DEGF_ONLY=02,07 (or env EGF_ONLY) narrows the matrix to those rows — for targeted reruns.
        String only = System.getProperty("EGF_ONLY", System.getenv().getOrDefault("EGF_ONLY", ""));
        java.util.Set<String> pick = new java.util.HashSet<>(java.util.Arrays.asList(only.split("\\s*,\\s*")));
        List<Object[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            String n = String.format("%02d", i + 1);
            if (only.isBlank() || pick.contains(n)) rows.add(new Object[]{ "TC_EGF_" + n, types.get(i) });
        }
        return rows.toArray(new Object[0][]);
    }

    /** Form instance this test created (asset, title) — deleted in teardown even when an assert failed. */
    private String createdAsset, createdTitle;

    @org.testng.annotations.AfterMethod(alwaysRun = true)
    public void deleteCreatedForm() {
        if (createdTitle == null || forms == null) return;
        try {
            if (forms.isEditFormOpen()) forms.closeEditForm();
            boolean deleted = forms.deleteFormInstance(createdAsset, createdTitle);
            System.out.println((deleted ? "🧹 deleted" : "⚠️ could not delete") + " test form '" + createdTitle + "' on '" + createdAsset + "'");
        } catch (Exception e) {
            System.out.println("⚠️ deleteCreatedForm: " + e.getMessage());
        } finally {
            createdAsset = null; createdTitle = null;
        }
    }

    /** Session → Assets tab → first room with assets → 'Assets in Room'; returns the first asset row's name. */
    private String openRoomOrSkip(WorkTypeCatalog wt, String tcId) {
        openFixtureOrSkip(wt, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(),
                tcId + ": session details did not settle open for " + wt.fixtureName());
        skipIfPreconditionMissing(() -> wo.tapSessionTab("Assets"),
                tcId + ": 'Assets' session tab not tappable");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.openFirstRoomWithAssetsInTree(),
                tcId + ": no '<room>, N assets' row reachable in the session tree");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.isAssetsInRoomOpen(), tcId + ": 'Assets in Room' did not open");
        List<com.egalvanic.base.BasePage.SnapNode> rows = forms.assetRows();
        skipIfPreconditionMissing(() -> !rows.isEmpty(), tcId + ": room lists no asset rows");
        String asset = WorkOrderFormsPage.assetNameOf(rows.get(0));
        logStep("First asset in room: '" + asset + "' (row '" + rows.get(0).label + "')");
        return asset;
    }

    @Test(dataProvider = "serviceBackedWorkTypes", timeOut = 600_000)   // first run per fixture may self-provision
    public void TC_EGF_formsAddableAndFillablePerWorkType(String tcId, WorkTypeCatalog wt) {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                tcId + " - EG Forms addable + fillable on '" + wt.displayName() + "'");
        // Open the session's Assets tab, then find an asset whose class HAS forms: 'No procedures apply to this
        // asset's class' is a legitimate empty sheet (e.g. a 'Node Bus' node), not the defect this case guards.
        openFixtureOrSkip(wt, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(), tcId + ": session details did not settle open");
        skipIfPreconditionMissing(() -> wo.tapSessionTab("Assets"), tcId + ": 'Assets' session tab not tappable");
        mediumWait();
        List<String> found = forms.roomsAdvertisingAssets();
        if (found.isEmpty()) {
            // Fixture with no active assets (DGA / Panel Schedule / Shutdown / UPS, 2026-10-05): link a known
            // form-bearing site asset into its room ONCE — the link persists, later runs take the fast path.
            logStep("No room has active assets in " + wt.fixtureName() + " — linking '" + PROVISION_ASSET
                    + "' from '" + PROVISION_ROOM + "' (fixture self-provisioning)");
            boolean linked = forms.openRoomNamed(PROVISION_ROOM) && forms.linkExistingAsset(PROVISION_ASSET);
            logStep("self-provisioned: " + linked);
            forms.backToRoomList();
            if (linked) found = forms.roomsAdvertisingAssets();
        }
        final List<String> rooms = found;
        logStep("Rooms advertising assets: " + rooms);
        skipIfPreconditionMissing(() -> !rooms.isEmpty(), tcId + ": no room with assets in " + wt.fixtureName()
                + " and self-provisioning ('" + PROVISION_ASSET + "' from '" + PROVISION_ROOM + "') failed");

        String asset = null, title = null;
        int before = -1;
        boolean sawAddForm = false;
        List<String> lastMenu = List.of();
        search:
        for (String room : rooms.subList(0, Math.min(3, rooms.size()))) {
            if (!forms.openRoomByLabel(room)) { logStep("could not open room '" + room + "'"); continue; }
            com.egalvanic.utils.Waits.until(() -> !forms.assetRows().isEmpty(), 10_000, 700);
            List<com.egalvanic.base.BasePage.SnapNode> rows = new java.util.ArrayList<>(forms.assetRows());
            // Real equipment first: ad-hoc assets named 'test…' only carry ad-hoc 'Test_…' forms with no inputs.
            rows.sort(java.util.Comparator.comparing(r -> WorkOrderFormsPage.assetNameOf(r).toLowerCase().startsWith("test")));
            logStep("Room '" + room + "': " + rows.size() + " asset row(s)");
            for (com.egalvanic.base.BasePage.SnapNode row : rows.subList(0, Math.min(3, rows.size()))) {
                String candidate = WorkOrderFormsPage.assetNameOf(row);
                int badge = forms.assetRowFormCount(candidate);
                lastMenu = forms.openAssetRowMenu(candidate);
                if (!lastMenu.contains(WorkOrderFormsPage.ADD_FORM_MENU_ITEM)) {
                    forms.pressVisibleButton("Cancel");
                    try { com.egalvanic.utils.DriverManager.getDriver().executeScript("mobile: tap", java.util.Map.of("x", 200, "y", 40)); } catch (Exception ignored) { }
                    continue;
                }
                sawAddForm = true;
                forms.pressVisibleButton(WorkOrderFormsPage.ADD_FORM_MENU_ITEM);
                if (!com.egalvanic.utils.Waits.until(forms::isAddProcedureSheetOpen, 8_000, 500)) continue;
                String chosen = forms.chooseFirstAddableForm();
                if (chosen != null) { asset = candidate; title = chosen; before = badge; break search; }
                logStep("'" + candidate + "' (" + row.label + "): no form applies to this class — trying the next asset");
                forms.pressVisibleButton("Cancel");
                sleep(1000);
            }
            forms.backToRoomList();
        }
        if (title == null) {
            // No asset here carries a catalogue form (e.g. QA-WT02: a Node Bus + an ad-hoc 'test' Busduct whose only
            // forms are input-less 'Test_…' entries). Bring in the known form-bearing asset and use it directly.
            logStep("No catalogue form on the room's assets — linking '" + PROVISION_ASSET + "' from '" + PROVISION_ROOM + "'");
            if (forms.openRoomNamed(PROVISION_ROOM)) {
                if (forms.assetRowFormCount(PROVISION_ASSET) < 0) forms.linkExistingAsset(PROVISION_ASSET);
                if (forms.assetRowFormCount(PROVISION_ASSET) >= 0) {
                    int badge = forms.assetRowFormCount(PROVISION_ASSET);
                    lastMenu = forms.openAssetRowMenu(PROVISION_ASSET);
                    if (lastMenu.contains(WorkOrderFormsPage.ADD_FORM_MENU_ITEM)) {
                        sawAddForm = true;
                        forms.pressVisibleButton(WorkOrderFormsPage.ADD_FORM_MENU_ITEM);
                        if (com.egalvanic.utils.Waits.until(forms::isAddProcedureSheetOpen, 8_000, 500)) {
                            String chosen = forms.chooseFirstAddableForm();
                            if (chosen != null) { asset = PROVISION_ASSET; title = chosen; before = badge; }
                        }
                    }
                }
            }
        }
        // ── ADDABLE ──────────────────────────────────────────────────────────────────────────────
        final boolean addOffered = sawAddForm;
        final List<String> menuSeen = lastMenu;
        assertTrue(addOffered, "'" + wt.displayName() + "' (" + wt.fixtureName() + "): no asset menu offered '"
                + WorkOrderFormsPage.ADD_FORM_MENU_ITEM + "' (last menu " + menuSeen + ") — EG Forms are not addable on this work type");
        assertNotNull(title, "'" + wt.displayName() + "': 'Add Form' was offered but no asset in " + rooms.size()
                + " room(s) had a form that opens in '" + WorkOrderFormsPage.EDIT_FORM_TITLE + "'");
        createdAsset = asset; createdTitle = title;
        logStep("Added form '" + title + "' to '" + asset + "' (badge before " + before + ")");

        // ── FILLABLE ────────────────────────────────────────────────────────────────────────────
        String typed = "1.5";
        String read = forms.fillFirstField(typed);
        assertNotNull(read, "'" + wt.displayName() + "': form '" + title + "' exposes no fillable field");
        assertTrue(read.equals(typed) || read.equals("Pass"),
                "'" + wt.displayName() + "': the first field of '" + title + "' must hold what was entered — read '" + read + "'");
        logStepWithScreenshot(tcId + ": '" + title + "' filled (" + read + ") on " + wt.displayName());

        // ── the added form persisted on the asset ───────────────────────────────────────────────
        assertTrue(forms.closeEditForm(), "Closing the form must return to 'Assets in Room'");
        List<String> instances = forms.formInstancesOf(asset);
        long copies = instances.stream().filter(title::equals).count();
        logStep("'" + asset + "' form instances after adding: " + instances);
        assertTrue(copies >= 1, "'" + wt.displayName() + "': the added form '" + title + "' must be listed under '"
                + asset + "' after closing it — instances " + instances);
        int after = forms.assetRowFormCount(asset);
        if (before > 0 || after > 0) {
            assertEquals(after, Math.max(before, 0) + 1,
                    "'" + wt.displayName() + "': '" + asset + "' form badge must grow by exactly 1 (before " + before + ", after " + after + ")");
        } else {
            logStep("This work type's asset rows render no form badge — instance list used as the oracle");
        }
        // teardown also deletes it (deleteCreatedForm) — verified here so a leak is loud, not silent
        assertTrue(forms.deleteFormInstance(asset, title), "The test form must be deletable (cleanup)");
        createdTitle = null;
        long left = forms.formInstancesOf(asset).stream().filter(title::equals).count();
        assertEquals(left, copies - 1, "Deleting the test form must remove exactly that one '" + title + "' instance");
    }

    @Test
    public void TC_EGF_14_generalWorkOrderFormsDiscovery() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_EGF_14 - DISCOVERY: forms on a 'General' (type-less) work order");
        openFixtureOrSkip(WorkTypeCatalog.GENERAL, "TC_EGF_14");
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(), "TC_EGF_14: session details did not settle open");
        skipIfPreconditionMissing(() -> wo.tapSessionTab("Assets"), "TC_EGF_14: 'Assets' session tab not tappable");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.hasRoomAdvertisingAssets(),
                "TC_EGF_14: the General fixture has no room with assets — nothing to discover (bounded check, no room walk)");
        String asset = openRoomOrSkip(WorkTypeCatalog.GENERAL, "TC_EGF_14");
        List<String> menu = forms.openAssetRowMenu(asset);
        logStep("General WO asset menu: " + menu);
        // App truth first: General has no backend service; whether 'Add Form' is offered is captured, not asserted.
        try { DriverManager.getDriver().executeScript("mobile: tap", java.util.Map.of("x", 200, "y", 40)); } catch (Exception ignored) { }
        verifyAppAlive("General work order asset menu");
        logStepWithScreenshot("TC_EGF_14: General WO — 'Add Form' offered: " + menu.contains(WorkOrderFormsPage.ADD_FORM_MENU_ITEM));
    }
}
