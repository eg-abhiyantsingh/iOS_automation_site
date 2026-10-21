package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.ZP3914BaseTest;
import com.egalvanic.pages.SiteWalkPage;
import com.egalvanic.pages.SiteWalkPage.WalkRow;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ZP-3914 (iOS 1.59) — "Site Walk: Account Manager Experience" + "Walk wizard".
 *
 * Runs in the Account Manager experience (entered via Settings › Switch Experience, then restored). Walks are
 * created only through the 'Assign customer and site later' path — no customer / site is ever created — and each
 * test deletes the walks it created by backend id (GET /site-walk/list diff), so nothing is left on QA.
 */
public final class ZP3914_SiteWalkWizard_Test extends ZP3914BaseTest {

    private static final String FEATURE = "Site Walk wizard (ZP-3914)";

    @Test(priority = 1)
    public void TC_SWW_01_siteWalksListContract() {
        featureTest(FEATURE, "TC_SWW_01 - Account Manager home is the Site Walks list (New Site Walk, Refresh, network menu, walk rows)");
        openSiteWalksOrSkip("TC_SWW_01");
        assertTrue(walks.isListShown(), "Nav title 'Site Walks' + 'New Site Walk' must be shown");
        assertTrue(walks.isRefreshEnabled(), "'Refresh' must be enabled while online");
        assertFalse(walks.isOfflineIcon(), "The network control must show the online Wi-Fi icon");
        List<WalkRow> rows = walks.walkRows();
        logStep("Walk rows on screen: " + rows.size() + (rows.isEmpty() ? "" : " — first " + rows.get(0)));
        TestDataApi a = api();
        int backend = a == null ? -1 : a.listSiteWalks().size();
        if (backend > 0) {
            assertFalse(rows.isEmpty(), "The backend lists " + backend + " walk(s) for this user but the list shows none");
        }
        for (WalkRow r : rows) {
            assertTrue(List.of(SiteWalkPage.ICON_NOT_SYNCED, SiteWalkPage.ICON_OPEN, SiteWalkPage.ICON_COMPLETE).contains(r.icon),
                    "Every walk row carries a status icon (not synced / open / complete) — '" + r.label + "' shows '" + r.icon + "'");
            assertEquals(r.complete, SiteWalkPage.ICON_COMPLETE.equals(r.icon),
                    "A row is marked complete (value=1) exactly when it shows the complete icon: " + r);
        }
        logStepWithScreenshot("TC_SWW_01: list contract verified (" + rows.size() + " rows)");
    }

    @Test(priority = 2)
    public void TC_SWW_02_customerStepContractAndCancel() {
        featureTest(FEATURE, "TC_SWW_02 - Wizard step 1: customers, search, New Customer / Assign later; Cancel creates nothing");
        openSiteWalksOrSkip("TC_SWW_02");
        Set<String> before = walkIds();
        assertTrue(walks.openWizard(), "'New Site Walk' must open the wizard at 'Step 1 of 3'");
        assertEquals(walks.wizardTitle(), SiteWalkPage.WIZ_CUSTOMER, "Step 1 is titled 'Customer'");
        assertTrue(walks.isTextVisible(SiteWalkPage.CHOOSE_CUSTOMER) || !walks.customerRows().isEmpty(),
                "Step 1 lists customers under '" + SiteWalkPage.CHOOSE_CUSTOMER + "'");
        List<String> customers = walks.customerRows();
        logStep("Customer rows: " + customers.size() + " (first " + (customers.isEmpty() ? "-" : customers.get(0)) + ")");
        for (String c : customers) {
            assertTrue(c.matches("^.+, \\d+ sites?$"), "Customer rows read '<name>, N site(s)' — got '" + c + "'");
        }
        String query = "zzqq-no-customer-" + System.currentTimeMillis() % 1000;
        assertTrue(walks.searchCustomers(query), "The 'Search customers' field must accept typing");
        assertEquals(walks.customerSearchText(), query, "The search field must read back the typed query");
        assertTrue(walks.customerRows().isEmpty(), "A query matching nothing must hide every customer row — still "
                + walks.customerRows());
        assertTrue(walks.isTextVisible(SiteWalkPage.NEW_CUSTOMER), "'New Customer' must stay available after a search");
        assertTrue(walks.isAssignLaterOffered(), "'" + SiteWalkPage.ASSIGN_LATER + "' must stay available after a search");
        assertTrue(walks.isTextVisible(SiteWalkPage.DEFER_FOOTER), "Footer '" + SiteWalkPage.DEFER_FOOTER + "' expected");
        assertTrue(walks.cancelWizard(), "'Cancel' must close the wizard back to the list");
        assertEquals(walkIds().size(), before.size(), "Cancelling the wizard must not create a walk");
        logStepWithScreenshot("TC_SWW_02: step 1 contract verified");
    }

    @Test(priority = 3)
    public void TC_SWW_03_customerLeadsToSiteStepAndBack() {
        featureTest(FEATURE, "TC_SWW_03 - Choosing a customer goes to 'Step 2 of 3' (its sites); back returns to step 1");
        openSiteWalksOrSkip("TC_SWW_03");
        assertTrue(walks.openWizard(), "Wizard must open");
        String customer = walks.chooseFirstCustomerWithSites();
        skipIfPreconditionMissing(() -> customer != null, "TC_SWW_03: no customer with at least one site on this tenant");
        String name = customer.substring(0, customer.lastIndexOf(", "));
        int siteCount = Integer.parseInt(customer.replaceAll("^.*, (\\d+) sites?$", "$1"));
        assertEquals(walks.currentStep(), 2, "Choosing a customer must advance to 'Step 2 of 3'");
        assertEquals(walks.wizardTitle(), name, "Step 2 is titled with the chosen customer's name");
        assertTrue(walks.isTextVisible(SiteWalkPage.CHOOSE_SITE), "Step 2 lists sites under '" + SiteWalkPage.CHOOSE_SITE + "'");
        List<String> sites = walks.siteRows();
        logStep("Sites of '" + name + "': " + sites);
        assertFalse(sites.isEmpty(), "'" + customer + "' must list its site(s) on step 2");
        if (siteCount <= 5) {
            assertEquals(sites.size(), siteCount, "Step 2 must list exactly the " + siteCount + " site(s) the customer row promised");
        }
        assertFalse(walks.isNoSitesMessageShown(), "A customer with sites must not show the 'no sites yet' message");
        assertTrue(walks.isTextVisible(SiteWalkPage.SITE_LATER), "'" + SiteWalkPage.SITE_LATER + "' must be offered on step 2");
        assertTrue(walks.wizardBack(), "The navigation back button must leave step 2");
        assertEquals(walks.currentStep(), 1, "Back from step 2 must return to 'Step 1 of 3'");
        assertTrue(walks.cancelWizard(), "Cancel must close the wizard");
        logStepWithScreenshot("TC_SWW_03: customer → site step → back verified");
    }

    @Test(priority = 4)
    public void TC_SWW_04_servicesGateStartWalk() {
        featureTest(FEATURE, "TC_SWW_04 - Step 3 'Services': Start Walk is disabled until a service is picked");
        openSiteWalksOrSkip("TC_SWW_04");
        assertTrue(walks.openWizard(), "Wizard must open");
        assertTrue(walks.chooseAssignLater(), "'Assign customer and site later' must jump to 'Step 3 of 3'");
        assertEquals(walks.wizardTitle(), SiteWalkPage.WIZ_SERVICES, "Step 3 is titled 'Services'");
        Map<String, Boolean> services = walks.services();
        logStep("Services offered: " + services.keySet());
        assertFalse(services.isEmpty(), "Step 3 must list the company's services");
        assertFalse(services.containsValue(true), "No service is pre-selected: " + services);
        assertFalse(walks.isStartWalkEnabled(), "'Start Walk' must be disabled until a service is picked");
        String first = services.keySet().iterator().next();
        assertTrue(walks.pickService(first), "Tapping '" + first + "' must select it (value=1)");
        assertTrue(walks.isStartWalkEnabled(), "'Start Walk' must enable once a service is picked");
        assertTrue(walks.tapService(first), "Tapping the service again must be possible");
        assertTrue(eventually(() -> !Boolean.TRUE.equals(walks.services().get(first)), 4), "A second tap must deselect it");
        assertFalse(walks.isStartWalkEnabled(), "'Start Walk' must disable again with no service picked");
        assertTrue(walks.cancelWizard(), "Cancel must close the wizard");
        logStepWithScreenshot("TC_SWW_04: services gate verified");
    }

    @Test(priority = 5)
    public void TC_SWW_05_modeTrayOffersCountAndJournal() {
        featureTest(FEATURE, "TC_SWW_05 - Start Walk opens the mode tray (Count / Journal); Cancel creates nothing");
        openSiteWalksOrSkip("TC_SWW_05");
        Set<String> before = walkIds();
        assertTrue(walks.openWizard() && walks.chooseAssignLater(), "Wizard → Assign later must reach Services");
        String service = walks.services().keySet().iterator().next();
        assertTrue(walks.pickService(service), "Pick a service");
        assertTrue(walks.pressStartWalk(), "'Start Walk' must open the mode tray");
        assertTrue(walks.isTextVisible(SiteWalkPage.MODE_SUBTITLE), "Tray subtitle '" + SiteWalkPage.MODE_SUBTITLE + "' expected");
        List<String> modes = walks.modeOptions();
        logStep("Modes: " + modes);
        assertEquals(modes.size(), 2, "Exactly two walk modes are offered: " + modes);
        assertTrue(modes.get(0).startsWith(SiteWalkPage.MODE_COUNT + ", "), "First mode is 'Count, STRUCTURED, …' — got " + modes.get(0));
        assertTrue(modes.get(1).startsWith(SiteWalkPage.MODE_JOURNAL + ", "), "Second mode is 'Journal, FASTEST, …' — got " + modes.get(1));
        assertTrue(walks.cancelModeTray(), "'Cancel' must close the tray");
        assertEquals(walks.currentStep(), 3, "Cancelling the tray returns to the Services step");
        assertTrue(walks.cancelWizard(), "Cancel the wizard");
        assertEquals(walkIds().size(), before.size(), "No walk may be created until a mode is picked");
        logStepWithScreenshot("TC_SWW_05: mode tray verified");
    }

    @Test(priority = 6)
    public void TC_SWW_06_createUnassignedCountWalk() {
        featureTest(FEATURE, "TC_SWW_06 - A Count walk created with 'Assign later' opens its detail and reaches the backend");
        openSiteWalksOrSkip("TC_SWW_06");
        Set<String> before = walkIds();
        String service = walks.createUnassignedCountWalk();
        assertNotNull(service, "Wizard → Assign later → service → Start Walk → Count must open the new walk");
        String title = walks.walkTitle();
        logStep("New walk title: '" + title + "' (service " + service + ")");
        assertTrue(title.startsWith("Site Walk — "), "An unassigned walk is titled 'Site Walk — <date>' — got '" + title + "'");
        assertTrue(walks.isTextVisible(SiteWalkPage.COUNT_TAG), "Detail shows the 'COUNT' tag");
        assertTrue(walks.isTextVisible(SiteWalkPage.START_FIRST_ROOM), "'Start First Room' expected on a new walk");
        assertTrue(walks.isTextVisible(SiteWalkPage.COUNT_WITHOUT_LOCATION), "'Count assets without a location' expected");
        assertFalse(walks.isCompleteWalkEnabled(), "'Complete Site Walk' must be disabled before anything is counted");
        assertTrue(walks.isCompleteHintShown(), "Hint '" + SiteWalkPage.HINT_COUNT + "' must explain why");
        assertTrue(eventually(() -> !walkIdsDiff(before).isEmpty(), 20),
                "The walk must reach the backend (GET /site-walk/list) while online");
        Set<String> fresh = trackNewWalks(before);
        assertEquals(fresh.size(), 1, "Exactly one new walk expected — got " + fresh);
        TestDataApi.SiteWalkRow row = walkRow(fresh.iterator().next());
        assertNotNull(row, "The new walk must be readable from the list endpoint");
        assertEquals(row.walkType, "count", "Backend walk_type must be 'count' for a Count walk: " + row);
        assertTrue(row.sldId == null || row.sldId.isEmpty(), "'Assign customer and site later' must leave the walk without a site: " + row);
        assertTrue(title.contains(LocalDate.now().toString()) || title.matches("Site Walk — \\d{4}-\\d{2}-\\d{2}"),
                "Title date must be yyyy-MM-dd: '" + title + "'");
        assertTrue(walks.backToList(), "Back must return to the list");
        logStepWithScreenshot("TC_SWW_06: walk created — " + row);
    }

    @Test(priority = 7)
    public void TC_SWW_07_deleteWalkAsksThenRemoves() {
        featureTest(FEATURE, "TC_SWW_07 - Long-press › Delete Site Walk asks first; Cancel keeps it, Delete removes it");
        openSiteWalksOrSkip("TC_SWW_07");
        deleteTrackedWalksNow();   // no same-titled walk from an earlier case may remain
        Set<String> before = walkIds();
        assertNotNull(walks.createUnassignedCountWalk(), "A fresh walk is needed to delete");
        String title = walks.walkTitle();
        assertTrue(eventually(() -> !walkIdsDiff(before).isEmpty(), 20), "The new walk must reach the backend first");
        Set<String> fresh = trackNewWalks(before);
        String id = fresh.iterator().next();
        assertTrue(walks.backToList(), "Back to the list");
        List<WalkRow> mine = walks.walkRows().stream().filter(r -> r.label.contains(title)).toList();
        skipIfPreconditionMissing(() -> mine.size() == 1, "TC_SWW_07: " + mine.size() + " rows titled '" + title
                + "' — cannot tell which one is this test's walk");
        String cancelText = walks.deleteWalkRow(mine.get(0), "Cancel");
        assertEquals(cancelText, SiteWalkPage.DELETE_WALK + " | " + SiteWalkPage.DELETE_MESSAGE,
                "Delete must ask 'Delete Site Walk' / '" + SiteWalkPage.DELETE_MESSAGE + "' first");
        assertNotNull(walkRow(id), "Cancel must keep the walk on the backend");
        assertEquals(walks.walkRows().stream().filter(r -> r.label.contains(title)).count(), 1L, "Cancel must keep the row");
        WalkRow again = walks.walkRows().stream().filter(r -> r.label.contains(title)).findFirst().orElseThrow();
        String deleteText = walks.deleteWalkRow(again, SiteWalkPage.DELETE_WALK);
        assertFalse(deleteText.isEmpty(), "The confirmation must appear again");
        assertTrue(eventually(() -> walks.walkRows().stream().noneMatch(r -> r.label.contains(title)), 10),
                "The row must leave the list after 'Delete Site Walk'");
        assertTrue(eventually(() -> walkRow(id) == null, 20), "The walk must be deleted on the backend too");
        logStepWithScreenshot("TC_SWW_07: delete flow verified");
    }

    @Test(priority = 8)
    public void TC_SWW_08_backFromServicesKeepsCustomerSites() {
        featureTest(FEATURE, "TC_SWW_08 - Customer → 'Assign a site later' → back: step 2 still lists that customer's sites");
        openSiteWalksOrSkip("TC_SWW_08");
        assertTrue(walks.openWizard(), "Wizard must open");
        walks.waitForCustomers(15_000);
        String customer = walks.customerRowWithSites(1, 5).orElse(null);
        skipIfPreconditionMissing(() -> customer != null, "TC_SWW_08: no customer with 1-5 sites on screen");
        assertTrue(walks.chooseCustomer(customer), "Choosing '" + customer + "' must open step 2");
        String name = customer.substring(0, customer.lastIndexOf(", "));
        List<String> sites = walks.siteRows();
        assertFalse(sites.isEmpty(), "Step 2 must list the customer's sites first: " + customer);
        assertTrue(walks.chooseSiteLater(), "'" + SiteWalkPage.SITE_LATER + "' must advance to 'Step 3 of 3'");
        assertEquals(walks.wizardTitle(), SiteWalkPage.WIZ_SERVICES, "Step 3 is titled 'Services'");
        assertTrue(walks.wizardBack(), "Back from Services must return to step 2");
        assertEquals(walks.currentStep(), 2, "Back from Services must land on 'Step 2 of 3'");
        logStepWithScreenshot("Step 2 after coming back from Services");
        // 1.69 (probe 2026-10-21, NewSiteWalkViewModel.deferAssignment): the step-2 'Assign a site later' button
        // reuses the step-1 "defer both" action, which clears the customer and its site list — so coming back
        // shows the title 'Site' and "This customer has no sites yet…" for a customer that has sites.
        assertFalse(walks.isNoSitesMessageShown(), "After going back, step 2 says '" + SiteWalkPage.NO_SITES_FOR_CUSTOMER
                + "' although '" + customer + "' has sites — the customer choice was silently dropped");
        assertEquals(walks.wizardTitle(), name, "After going back, step 2 must still be titled with the customer '" + name + "'");
        assertEquals(walks.siteRows(), sites, "After going back, step 2 must list the same sites as before");
        logStepWithScreenshot("TC_SWW_08: customer kept after back-navigation");
    }

    @Test(priority = 9)
    public void TC_SWW_09_createWalkForCustomerSite() {
        featureTest(FEATURE, "TC_SWW_09 - Customer → site → service → Count: the walk is named after the site and linked to it");
        openSiteWalksOrSkip("TC_SWW_09");
        Set<String> before = walkIds();
        assertTrue(walks.openWizard(), "Wizard must open");
        String customer = walks.chooseFirstCustomerWithSites();
        skipIfPreconditionMissing(() -> customer != null, "TC_SWW_09: no customer with at least one site on this tenant");
        String site = walks.chooseFirstSite();
        assertNotNull(site, "Tapping a site must advance to 'Step 3 of 3'");
        String customerName = customer.substring(0, customer.lastIndexOf(", "));
        assertTrue(walks.isTextVisible(customerName) || walks.isTextVisible(SiteWalkPage.WIZ_SITE),
                "Step 3's back button must lead back to the site step");
        String service = walks.services().keySet().stream().findFirst().orElse(null);
        assertNotNull(service, "Step 3 must list services");
        assertTrue(walks.pickService(service), "Pick '" + service + "'");
        assertTrue(walks.pressStartWalk(), "'Start Walk' must open the mode tray");
        assertTrue(walks.chooseCountMode(), "Count must create the walk and open its detail");
        String title = walks.walkTitle();
        logStep("Walk for site '" + site + "' titled '" + title + "'");
        assertEquals(title, site, "A walk started for a site is titled with the site's name");
        assertTrue(eventually(() -> !walkIdsDiff(before).isEmpty(), 20), "The walk must reach the backend while online");
        Set<String> fresh = trackNewWalks(before);
        assertEquals(fresh.size(), 1, "Exactly one new walk expected — got " + fresh);
        TestDataApi.SiteWalkRow row = walkRow(fresh.iterator().next());
        assertNotNull(row, "The new walk must be readable from the list endpoint");
        assertEquals(row.name, site, "Backend walk name must be the site name: " + row);
        assertTrue(row.sldId != null && !row.sldId.isEmpty(), "A walk started for a site must carry its sld_id: " + row);
        assertEquals(row.status, "draft", "A new walk is a draft until completed: " + row);
        assertEquals(row.walkType, "count", "walk_type must be 'count': " + row);
        assertTrue(walks.backToList(), "Back must return to the list");
        logStepWithScreenshot("TC_SWW_09: site walk created — " + row);
    }

    private Set<String> walkIdsDiff(Set<String> before) {
        Set<String> now = new java.util.LinkedHashSet<>(walkIds());
        now.removeAll(before);
        return now;
    }

    @org.testng.annotations.AfterMethod(alwaysRun = true)
    public void cleanupWalksAfterEachCase(org.testng.ITestResult result) {
        captureFailureState(result);
        try {
            if (walks != null && walks.currentStep() > 0) walks.cancelWizard();
        } catch (Exception ignored) { }
        deleteTrackedWalksNow();
    }
}
