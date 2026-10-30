package com.egalvanic.tests;

import com.egalvanic.base.ZP3914BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ExperiencePage.Experience;
import com.egalvanic.pages.SiteWalkPage;
import com.egalvanic.pages.SiteWalkPage.WalkRow;
import com.egalvanic.pages.SyncNetworkPage;
import com.egalvanic.utils.DriverManager;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

/**
 * ZP-3914 (iOS 1.59) — "Offline support via SwiftData" + "Offline sync" for site walks, and the Settings › Sync &
 * Network surface that exposes the same queue.
 *
 * The oracle is the backend: while offline a new walk must NOT appear in GET /site-walk/list; after 'Sync N records'
 * it must. The app is always put back online in teardown (ZP3914BaseTest also checks), and every walk the case
 * created is deleted by id — including one that only reached the backend at sync time.
 */
public final class ZP3914_SiteWalkOffline_Test extends ZP3914BaseTest {

    private static final String FEATURE = "Site Walk offline (ZP-3914)";

    /** Rows queued for upload (icon 'Arrow Up Circle'). */
    private List<WalkRow> queuedRows() {
        return walks.walkRows().stream().filter(r -> SiteWalkPage.ICON_NOT_SYNCED.equals(r.icon)).toList();
    }

    @Test(priority = 1)
    public void TC_SWO_01_networkMenuTogglesOfflineMode() {
        featureTest(FEATURE, "TC_SWO_01 - The list's network menu switches Offline/Online (icon, Refresh, menu items)");
        openSiteWalksOrSkip("TC_SWO_01");
        List<String> online = walks.openNetworkMenu();
        logStep("Menu while online: " + online);
        assertTrue(online.contains(SiteWalkPage.GO_OFFLINE), "Online, the menu offers '" + SiteWalkPage.GO_OFFLINE + "': " + online);
        assertFalse(online.contains(SiteWalkPage.GO_ONLINE), "Online, the menu must not offer '" + SiteWalkPage.GO_ONLINE + "'");
        assertTrue(walks.pressMenuItem(SiteWalkPage.GO_OFFLINE), "Press '" + SiteWalkPage.GO_OFFLINE + "'");
        assertTrue(eventually(walks::isOfflineIcon, 6), "The toolbar must switch to the 'Wi-Fi Off' icon (nothing queued)");
        assertFalse(walks.isRefreshEnabled(), "'Refresh' must be disabled offline");
        List<String> offline = walks.openNetworkMenu();
        logStep("Menu while offline: " + offline);
        assertTrue(offline.contains(SiteWalkPage.GO_ONLINE), "Offline, the menu offers '" + SiteWalkPage.GO_ONLINE + "': " + offline);
        assertTrue(walks.pressMenuItem(SiteWalkPage.GO_ONLINE), "Press '" + SiteWalkPage.GO_ONLINE + "'");
        assertTrue(eventually(() -> !walks.isOfflineIcon(), 8), "Back online the 'Wi-Fi Off' icon must go");
        assertTrue(eventually(walks::isRefreshEnabled, 6), "'Refresh' must enable again online");
        logStepWithScreenshot("TC_SWO_01: network menu verified");
    }

    @Test(priority = 2)
    public void TC_SWO_02_offlineWalkIsQueuedThenSynced() {
        featureTest(FEATURE, "TC_SWO_02 - A walk made offline is queued locally (not on the backend) and uploads on 'Sync N records'");
        openSiteWalksOrSkip("TC_SWO_02");
        Set<String> before = walkIds();
        int queuedBefore = queuedRows().size();
        assertTrue(walks.goOffline(), "Go offline");
        assertNotNull(walks.createUnassignedCountWalk(), "The wizard must work offline");
        assertTrue(walks.isCountWalkDetailShown(), "The offline walk opens like an online one");
        assertTrue(walks.backToList(), "Back to the list");
        assertTrue(eventually(() -> queuedRows().size() == queuedBefore + 1, 6),
                "The new walk must be listed with the not-synced icon '" + SiteWalkPage.ICON_NOT_SYNCED + "' — rows " + walks.walkRows());
        assertTrue(walks.queueBadge() >= 1, "The network control must show the queued-record count — got " + walks.queueBadge());
        sleep(3000);
        Set<String> whileOffline = walkIds();
        whileOffline.removeAll(before);
        assertTrue(whileOffline.isEmpty(), "Offline, nothing may reach the backend — new ids " + whileOffline);
        List<String> menu = walks.openNetworkMenu();
        logStep("Offline menu with a queue: " + menu);
        assertTrue(menu.stream().anyMatch(i -> i.matches("^Sync \\d+ records?$")), "The menu must offer 'Sync N records' — " + menu);
        walks.closeMenu();
        assertTrue(walks.isOffline(), "Still offline while the walk waits in the queue");
        assertTrue(walks.goOnline(), "Go online (the menu must then offer '" + SiteWalkPage.GO_OFFLINE + "')");
        int synced = walks.syncQueued();
        logStep("Synced records: " + synced);
        assertTrue(synced >= 1, "'Sync N records' must be offered once online with a queue (got " + synced + ")");
        assertTrue(eventually(() -> walks.queueBadge() == 0, 30), "The queue badge must clear after syncing");
        assertTrue(eventually(() -> walkIds().size() > before.size(), 30), "After sync the walk must reach the backend");
        Set<String> fresh = trackNewWalks(before);
        // 'Sync N records' flushes the WHOLE queue: a walk an earlier test left queued (TC_SWC_02 hit a backend 503
        // on CI run 39602048381, so its walk stayed local) arrives too. Expect ours plus every row queued before.
        if (queuedBefore > 0) logStep(queuedBefore + " walk(s) were already queued before this test — they sync too");
        assertEquals(fresh.size(), queuedBefore + 1, "Our walk plus the " + queuedBefore + " already-queued one(s) must arrive — got " + fresh);
        assertTrue(eventually(() -> queuedRows().size() == queuedBefore, 10),
                "After sync no row may keep the not-synced icon — rows " + walks.walkRows());
        logStepWithScreenshot("TC_SWO_02: offline queue → sync verified (" + walkRow(fresh.iterator().next()) + ")");
    }

    @Test(priority = 3)
    public void TC_SWO_03_queueSurvivesRelaunch() {
        featureTest(FEATURE, "TC_SWO_03 - An unsynced walk survives an app relaunch (SwiftData) and still syncs afterwards");
        openSiteWalksOrSkip("TC_SWO_03");
        Set<String> before = walkIds();
        int queuedBefore = queuedRows().size();
        assertTrue(walks.goOffline(), "Go offline");
        assertNotNull(walks.createUnassignedCountWalk(), "Create a walk offline");
        assertTrue(walks.backToList(), "Back to the list");
        assertTrue(eventually(() -> queuedRows().size() == queuedBefore + 1, 6), "The walk is queued before the relaunch");
        logStep("Relaunching the app with the walk still queued");
        DriverManager.getDriver().terminateApp(AppConstants.APP_BUNDLE_ID);
        sleep(800);
        DriverManager.getDriver().activateApp(AppConstants.APP_BUNDLE_ID);
        sleep(2500);
        assertTrue(waitForExperienceAfterRelaunch(Experience.ACCOUNT_MANAGER, 30), "The app must come back in Account Manager");
        assertTrue(walks.waitForList(20_000), "The Site Walks list must load after the relaunch");
        assertTrue(eventually(() -> queuedRows().size() == queuedBefore + 1, 10),
                "After a relaunch the unsynced walk must still be listed with '" + SiteWalkPage.ICON_NOT_SYNCED + "' — rows " + walks.walkRows());
        Set<String> afterRelaunch = walkIds();
        afterRelaunch.removeAll(before);
        assertTrue(afterRelaunch.isEmpty(), "A relaunch alone must not upload the queue (nothing flushes automatically) — new ids " + afterRelaunch);
        assertTrue(walks.goOnline(), "Online (network mode resets to online at launch — this is a no-op then)");
        assertTrue(walks.syncQueued() >= 1, "'Sync N records' must still be offered for the persisted queue");
        assertTrue(eventually(() -> walkIds().size() > before.size(), 30), "The persisted walk must reach the backend after sync");
        trackNewWalks(before);
        logStepWithScreenshot("TC_SWO_03: queue persisted across relaunch and synced");
    }

    @Test(priority = 4)
    public void TC_SWO_04_settingsSyncAndNetworkMirrorsOfflineMode() {
        featureTest(FEATURE, "TC_SWO_04 - Settings › Network Mode and Sync Queue Analyzer reflect the offline state");
        loginAndWaitForTabs("TC_SWO_04");
        assertTrue(sync.openSyncSection(), "Settings must show the 'Network Mode' row");
        assertEquals(sync.networkModeSubtitle(), SyncNetworkPage.CONNECTED, "Network Mode starts 'Connected'");
        assertTrue(sync.openAnalyzer(), "'Sync Queue Analyzer' must open");
        assertTrue(sync.isHistorySegmentShown() || sync.pendingCount() >= 0, "The analyzer shows its Pending / History segments");
        assertFalse(sync.isOfflineBannerShown(), "No offline banner while connected");
        int pending = sync.pendingCount();
        logStep("Pending operations: " + pending);
        if (pending == 0) {
            assertTrue(sync.isEmptyStateShown(), "An empty queue says '" + SyncNetworkPage.NO_PENDING + "'");
        }
        assertTrue(sync.backToSettings(), "Back to Settings");
        assertTrue(sync.setOnline(false), "Turning Network Mode off must switch the subtitle to '" + SyncNetworkPage.OFFLINE_MODE + "'");
        assertTrue(sync.openAnalyzer(), "Open the analyzer offline");
        assertTrue(sync.isOfflineBannerShown(), "Offline, the analyzer shows '" + SyncNetworkPage.OFFLINE_BANNER + "'");
        assertFalse(sync.isFlushEnabled(), "'" + SyncNetworkPage.FLUSH + "' must be disabled offline");
        assertTrue(sync.backToSettings(), "Back to Settings");
        assertTrue(sync.setOnline(true), "Turning Network Mode on must restore '" + SyncNetworkPage.CONNECTED + "'");
        logStepWithScreenshot("TC_SWO_04: Sync & Network verified");
    }

    @AfterMethod(alwaysRun = true)
    public void backOnlineAndCleanUp(ITestResult result) {
        captureFailureState(result);
        try {
            if (sync != null && sync.openSyncSection() && !sync.isOnline()) sync.setOnline(true);
        } catch (Exception ignored) { }
        try {
            if (walks != null && experience.isOn(Experience.ACCOUNT_MANAGER)) {
                if (!walks.isListShown()) walks.backToList();
                walks.goOnline();
                if (walks.queueBadge() > 0) {
                    Set<String> before = walkIds();
                    walks.syncQueued();   // never leave a queued test walk behind — upload it, then delete by id
                    eventually(() -> walkIds().size() > before.size(), 20);
                    trackNewWalks(before);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ TC_SWO cleanup: " + e.getMessage());
        }
        deleteTrackedWalksNow();
    }
}
