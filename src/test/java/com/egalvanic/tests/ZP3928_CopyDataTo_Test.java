package com.egalvanic.tests;

import com.egalvanic.base.BasePage.SnapNode;
import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.WorkOrderFormsPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * ZP-3928 (iOS 1.56) "Forms: search bar in 'Copy Data To'".
 *
 * 'Copy Data To' is the EG Forms copy sheet (CopyFormDataSheet), NOT the asset screen's "Copy Details To".
 * Live 1.69 path (probe 2026-10-05, QA-WT10 NETA): session › Assets › room with assets › chevron 'Forward' on
 * an asset row expands its form instances › long-press an instance › 'Copy Data To…' › 'Copy Data' sheet with
 * a 'Search assets...' field, 'Select all (N)' and same-form target rows '<asset>, <room> · <status>'.
 * Every case Cancels out — nothing is ever copied.
 */
public final class ZP3928_CopyDataTo_Test extends WorkTypeBaseTest {

    private static final String FEATURE = "EG Forms 'Copy Data To' search (ZP-3928)";
    /** NETA fixture: its assets carry a shared form (De-Energized Electrical Testing on Switch-1/2/3). */
    private static final WorkTypeCatalog FIXTURE = WorkTypeCatalog.NETA_TESTING;
    private static final String NO_MATCH_QUERY = "zzqq-no-such-asset";

    private WorkOrderFormsPage forms;

    @BeforeMethod(alwaysRun = true)
    public void initPages() {
        if (!DriverManager.isDriverActive()) return;
        try { forms = new WorkOrderFormsPage(); } catch (IllegalStateException e) { DriverManager.initDriver(); forms = new WorkOrderFormsPage(); }
    }

    /** Never leave the sheet up (and never press 'Copy'). */
    @AfterMethod(alwaysRun = true)
    public void cancelSheet() {
        try { if (forms != null && forms.isCopyDataSheetOpen()) forms.cancelCopyData(); } catch (Exception ignored) { }
    }

    /**
     * Opens the Copy Data sheet for the first form instance that HAS same-form peers (a sheet with no targets
     * cannot show whether search filters). Returns the targets listed before any search.
     */
    private List<String> openCopyDataWithPeersOrSkip(String tcId) {
        openFixtureOrSkip(FIXTURE, tcId);
        skipIfPreconditionMissing(() -> wo.ensureSessionDetailsOpen(), tcId + ": session details did not settle open");
        skipIfPreconditionMissing(() -> wo.tapSessionTab("Assets"), tcId + ": 'Assets' session tab not tappable");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.openFirstRoomWithAssetsInTree(), tcId + ": no room with assets reachable");
        mediumWait();
        skipIfPreconditionMissing(() -> forms.isAssetsInRoomOpen(), tcId + ": 'Assets in Room' did not open");

        List<SnapNode> rows = forms.assetRowsWithForms();
        logStep("Asset rows advertising forms: " + rows.size());
        skipIfPreconditionMissing(() -> !rows.isEmpty(), tcId + ": no asset in the room advertises a form");
        for (SnapNode row : rows.subList(0, Math.min(3, rows.size()))) {
            List<String> instances = forms.expandAssetFormInstances(row);
            for (String title : instances) {
                logStep("Long-press '" + title + "' on '" + row.label + "' → " + WorkOrderFormsPage.COPY_DATA_MENU_ITEM);
                if (!forms.openCopyDataTo(title)) continue;
                List<String> targets = forms.copyDataTargets();
                logStep("Copy Data targets: " + targets);
                if (!targets.isEmpty()) return targets;
                logStep("'" + title + "' has no same-form peers in this work order — trying the next instance");
                forms.cancelCopyData();
            }
        }
        skipIfPreconditionMissing(() -> false, tcId + ": no form instance with same-form peers — search cannot be judged");
        return List.of();
    }

    private static String assetOf(String target) {
        return target.substring(0, target.indexOf(',')).trim();
    }

    @Test(priority = 1)
    public void TC_CDT_01_searchBarIsInTheCopyToPicker() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_CDT_01 - The EG Forms 'Copy Data To' sheet has a search bar");
        List<String> targets = openCopyDataWithPeersOrSkip("TC_CDT_01");
        assertTrue(forms.isCopyDataSearchFieldPresent(),
                "1.56 adds a search bar to the forms 'Copy Data To' sheet — the sheet listed " + targets.size()
                + " target(s) " + targets + " but exposed no '" + WorkOrderFormsPage.COPY_DATA_SEARCH + "' field");
        assertEquals(forms.copyDataSelectAllCount(), targets.size(),
                "'Select all (N)' must count exactly the listed targets " + targets);
        logStepWithScreenshot("TC_CDT_01: search bar present; " + targets.size() + " target(s)");
    }

    @Test(priority = 2)
    public void TC_CDT_02_searchFiltersToTheMatchingAsset() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_CDT_02 - Typing an asset name narrows the targets to that asset");
        List<String> targets = openCopyDataWithPeersOrSkip("TC_CDT_02");
        String wanted = assetOf(targets.get(targets.size() - 1));
        logStep("Searching for '" + wanted + "'");
        assertTrue(forms.typeCopyDataSearch(wanted), "The search field should accept typing");
        List<String> filtered = forms.copyDataTargets();
        logStep("Targets after search: " + filtered);
        assertFalse(filtered.isEmpty(), "Searching for a listed asset ('" + wanted + "') must keep it in the list");
        for (String t : filtered) {
            assertTrue(t.toLowerCase().contains(wanted.toLowerCase()),
                    "Every remaining target must match '" + wanted + "' — got '" + t + "' in " + filtered);
        }
        assertEquals(forms.copyDataSelectAllCount(), filtered.size(),
                "'Select all (N)' must follow the filtered list " + filtered);
        if (targets.size() > 1) {
            assertTrue(filtered.size() < targets.size(),
                    "A specific asset name must narrow " + targets.size() + " targets — still " + filtered);
        }
        logStepWithScreenshot("TC_CDT_02: '" + wanted + "' → " + filtered.size() + " target(s)");
    }

    @Test(priority = 3)
    public void TC_CDT_03_queryMatchingNothingShowsTheEmptyState() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_CDT_03 - A query matching no asset empties the list and says so");
        openCopyDataWithPeersOrSkip("TC_CDT_03");
        assertTrue(forms.typeCopyDataSearch(NO_MATCH_QUERY), "The search field should accept typing");
        List<String> filtered = forms.copyDataTargets();
        assertTrue(filtered.isEmpty(), "No target may match '" + NO_MATCH_QUERY + "' — got " + filtered);
        assertTrue(forms.isCopyDataNoResultsShown(),
                "An empty search result must say so ('" + WorkOrderFormsPage.COPY_DATA_NO_RESULTS + "'), not show a blank sheet");
        logStepWithScreenshot("TC_CDT_03: empty state shown");
    }

    @Test(priority = 4)
    public void TC_CDT_04_clearingTheSearchRestoresAllTargets() {
        ExtentReportManager.createTest(AppConstants.MODULE_JOBS, FEATURE,
                "TC_CDT_04 - Clearing the search restores the full list; Cancel copies nothing");
        List<String> targets = openCopyDataWithPeersOrSkip("TC_CDT_04");
        forms.typeCopyDataSearch(NO_MATCH_QUERY);
        assertTrue(forms.copyDataTargets().isEmpty(), "Precondition: the no-match query should empty the list");
        forms.typeCopyDataSearch("");
        List<String> restored = forms.copyDataTargets();
        assertEquals(restored.size(), targets.size(),
                "Clearing the search must restore every target (before " + targets + ", after " + restored + ")");
        assertTrue(forms.cancelCopyData(), "Cancel must close the Copy Data sheet");
        verifyAppAlive("after cancelling Copy Data");
        logStepWithScreenshot("TC_CDT_04: list restored, sheet cancelled");
    }
}
