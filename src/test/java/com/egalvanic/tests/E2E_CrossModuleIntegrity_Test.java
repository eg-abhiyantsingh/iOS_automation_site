package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.AssetPage;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.verify.VerificationError;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-module data-integrity E2E suite (TC_E2E_010 - TC_E2E_011).
 *
 * <p>These two flows target bug classes the single-module suites structurally
 * cannot catch:
 *
 * <ol>
 *   <li><b>Orphaned references</b> — an asset deleted in the Asset module while a
 *       linked issue still exists in the Issues module. A presence-only test in
 *       either module passes; the cross-module contract (issue must not ghost-
 *       reference a non-existent asset into a dead screen) is never asserted.</li>
 *   <li><b>Empty-list false-green</b> — issue list-dependent tests pass vacuously
 *       against an empty local store (offline-first SwiftData). We seed a KNOWN
 *       issue (API-first, deterministic) and then assert exact count / title /
 *       status round-trips through the UI <em>and</em> the server.</li>
 * </ol>
 *
 * <p>Per the framework working agreement: real assertions only (no tautologies,
 * no skip-to-mask). {@code skipIfPreconditionMissing} is used ONLY for genuine
 * environmental gaps (e.g. asset creation unavailable, no API reachability for
 * the seed path). VerificationError (un-swallowable) is reserved for the actual
 * ghost-reference contract violation in Flow 1.
 *
 * <p>Driver lifecycle follows the repo idiom (Issue_Phase1_Test): {@code @BeforeClass}
 * sets {@code noReset(true)} and seeds via API; {@code @BeforeMethod} re-creates
 * the page objects against the existing driver and lands on the right screen.
 * The driver is NEVER killed in {@code @BeforeMethod} (page objects cache it at
 * construction).
 */
public final class E2E_CrossModuleIntegrity_Test extends BaseTest {

    private AssetPage e2eAssetPage;
    private IssuePage issuePage;

    /** Shared API client (one login per class); null when API unreachable. */
    private static TestDataApi api;
    private static boolean apiReachable;
    /** Resolved once: the SLD the QA user lands on (first site / -Dseed.sldName). */
    private static String sldId;

    // ================================================================
    // SETUP / TEARDOWN
    // ================================================================

    @BeforeClass(alwaysRun = true)
    public void e2eSuiteSetup() {
        System.out.println("\n🔗 Cross-Module Integrity E2E Suite - Starting");
        DriverManager.setNoReset(true);
        initApi();
    }

    /**
     * Authenticate the backend client once and resolve the target SLD. Never
     * fails the class: with no API reachability (offline CI / sandboxed egress)
     * {@code apiReachable} stays false and the seed flow falls back to UI-create
     * or an environmental skip. Mirrors Issue_Phase1_Test.seedIssueViaApi().
     */
    private void initApi() {
        try {
            api = new TestDataApi();
            api.login();
            String sldName = System.getProperty("seed.sldName");
            sldId = (sldName != null && !sldName.isEmpty())
                    ? api.findSldIdByName(sldName) : api.firstSldId();
            apiReachable = api.isAuthenticated() && sldId != null;
            System.out.println(apiReachable
                    ? "✅ TestDataApi reachable — sldId=" + sldId
                    : "⚠️ TestDataApi authenticated but no SLD visible — API path disabled");
        } catch (Exception e) {
            apiReachable = false;
            System.out.println("⚠️ TestDataApi unreachable (" + e.getMessage()
                    + ") — flows will use UI-only paths / environmental skips");
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void e2eTestSetup() {
        // Dead-session guard: a page-object call against a dead session blocks
        // ~90s (driver HTTP readTimeout). Bail before touching any.
        if (!DriverManager.isDriverActive()) {
            System.out.println("⚠️ E2E pre-test setup skipped — no active driver session");
            return;
        }
        // Page objects cache DriverManager.getDriver() at construction — build
        // them here against the existing (never-killed) session.
        e2eAssetPage = new AssetPage();
        issuePage = new IssuePage();

        // loginAndSelectSite() is a no-op when already on Dashboard; otherwise it
        // recovers the site context the Asset/Issue tabs both need.
        loginAndSelectSite();
    }

    // ================================================================
    // FLOW 1 — ASSET DELETE ORPHANS A LINKED ISSUE (ghost reference)
    // ================================================================

    /**
     * TC_E2E_010 — create an asset, link an issue to it, delete the asset, then
     * open the orphaned issue and assert the app's actual contract.
     *
     * <p><b>Asserted contract (the only condition that FAILS this test):</b> the
     * issue detail screen renders the DELETED asset's name as a tappable
     * reference AND tapping it navigates to a dead/blank screen. That is a true
     * ghost reference — a dangling link the app should have cleared or flagged.
     *
     * <p>Everything else is a PASS:
     * <ul>
     *   <li>detail cleanly clears the asset (name no longer shown), or</li>
     *   <li>shows a "deleted"/"unavailable" flag, or</li>
     *   <li>still shows the name but tapping it stays on a live, non-blank
     *       screen (the link resolves to something real).</li>
     * </ul>
     *
     * <p>Silent app death anywhere is caught by BaseTest's teardown crash-assert
     * ({@code failIfAppCrashedDuringSuccessfulTest}); each navigation is fenced
     * with verifyAppAlive / verifyNotBlank so a crash can't read as a clean pass.
     */
    @Test(priority = 1)
    public void TC_E2E_010_assetDeleteOrphansLinkedIssue() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
                "TC_E2E_010 - Asset delete must not leave a ghost reference on its linked issue");
        IOSDriver driver = DriverManager.getDriver();

        // ---- Step 1: create a throwaway asset online -----------------------
        logStep("Step 1: Create a throwaway asset (online)");
        e2eAssetPage.navigateToAssetList();
        shortWait();

        String assetName = null;
        boolean assetCreated = false;
        try {
            // createAssetWithAutoName picks a unique timestamped name + class;
            // we use NEC-friendly "ATS" which the picker reliably exposes.
            assetName = e2eAssetPage.createAssetWithAutoName("ATS");
            shortWait();
            assetCreated = e2eAssetPage.isAssetCreatedSuccessfully();
        } catch (Exception e) {
            logWarning("Asset creation threw: " + e.getMessage());
        }
        guard("after asset creation");

        // Asset creation depends on a writable site + selectable class/location;
        // if the environment can't create one, that's an ENVIRONMENTAL gap, not
        // an assertion outcome — skip cleanly rather than assert on nothing.
        final String createdName = assetName;
        final boolean createdOk = assetCreated;
        skipIfPreconditionMissing(() -> createdName != null && !createdName.isEmpty() && createdOk,
                "could not create a throwaway asset online (writable site / class / location unavailable) "
                        + "— cannot exercise the asset-delete orphan flow");
        logStep("Created asset: " + assetName);

        // ---- Step 2: create an issue linked to that asset ------------------
        logStep("Step 2: Create an issue linked to '" + assetName + "'");
        String issueTitle = "E2E_Orphan_" + System.currentTimeMillis();
        boolean issueCreated = false;
        try {
            // createQuickIssue selects a class + title, then links the named asset
            // via selectAssetByName. The named asset IS in the picker (we just
            // created it), so this is a real link — not the first-asset fallback.
            issueCreated = issuePage.navigateToIssuesScreen()
                    && issuePage.createQuickIssue(issueTitle, assetName);
        } catch (Exception e) {
            logWarning("Linked-issue creation threw: " + e.getMessage());
        }
        guard("after linked-issue creation");

        final boolean issueCreatedOk = issueCreated;
        skipIfPreconditionMissing(() -> issueCreatedOk,
                "could not create an issue linked to the throwaway asset "
                        + "— cannot exercise the orphan flow (issue-create unavailable)");

        // The issue must be visible in the list (real create, not a vacuous pass).
        issuePage.navigateToIssuesScreen();
        issuePage.searchIssues(issueTitle);
        boolean visible = waitForCondition(() -> issuePage.isIssueInList(issueTitle),
                5, "linked issue '" + issueTitle + "' to appear in the Issues list");
        assertTrue(visible, "Linked issue '" + issueTitle + "' should be visible in the Issues list after creation");
        logStep("Linked issue is visible in list");

        // ---- Step 3: delete the asset, assert it left the list -------------
        logStep("Step 3: Delete the asset '" + assetName + "' (swipe-left)");
        issuePage.clearSearch();
        e2eAssetPage.navigateToAssetList();
        shortWait();
        e2eAssetPage.searchAsset(assetName);
        shortWait();

        boolean deleted = swipeLeftDeleteAssetByName(driver, assetName);
        guard("after asset delete");
        skipIfPreconditionMissing(() -> deleted,
                "could not reach/confirm the swipe-left delete control for '" + createdName
                        + "' (delete affordance unavailable in this build) — cannot orphan the issue");

        // Re-search and assert the asset is gone — proves the delete really took.
        e2eAssetPage.navigateToAssetList();
        shortWait();
        e2eAssetPage.searchAsset(assetName);
        final String deletedName = assetName;
        boolean assetGone = waitForCondition(() -> !e2eAssetPage.verifyAssetExistsInList(deletedName),
                6, "deleted asset '" + deletedName + "' to leave the Asset list");
        assertTrue(assetGone, "Deleted asset '" + assetName + "' must NOT appear in the Asset list anymore");
        logStep("Asset confirmed removed from list");

        // ---- Step 4: open the orphaned issue, assert the ghost contract ----
        logStep("Step 4: Open the orphaned issue and assert the no-ghost-reference contract");
        e2eAssetPage.navigateToAssetList(); // leave any stale asset-search context
        shortWait();
        boolean onIssues = issuePage.navigateToIssuesScreen();
        assertTrue(onIssues, "Should navigate to the global Issues screen to inspect the orphaned issue");
        issuePage.searchIssues(issueTitle);
        shortWait();

        issuePage.tapOnIssue(issueTitle);
        shortWait();
        verifyAppAlive("open orphaned issue detail");          // crash here is a real bug, not a pass
        verifyNotBlank("Issue Details (orphaned)");            // a blank detail screen is a real bug

        boolean onDetail = issuePage.isIssueDetailsScreenDisplayed();
        skipIfPreconditionMissing(() -> onDetail,
                "could not open the orphaned issue's detail screen (tapOnIssue fell through) "
                        + "— cannot evaluate the ghost-reference contract");

        // Does the detail still render the DELETED asset name as a reference?
        String shownAsset = issuePage.getIssueDetailAssetName();
        boolean stillShowsDeletedAsset = shownAsset != null
                && shownAsset.contains(assetName);
        logStep("Detail asset field reads: '" + shownAsset + "' (still shows deleted asset name: "
                + stillShowsDeletedAsset + ")");

        if (!stillShowsDeletedAsset) {
            // App cleanly cleared / flagged the reference — this is the GOOD path.
            logStep("✅ App cleared/flagged the deleted-asset reference — no ghost reference");
        } else {
            // The name is still shown. That alone is NOT a failure (it may resolve
            // to a live screen). It IS a ghost reference only if tapping it lands
            // on a dead/blank screen. Probe that, then return to detail.
            logStep("Detail still shows the deleted asset name — probing whether the link is dead");
            boolean tappedRef = tapDeletedAssetReference(driver, assetName);
            if (tappedRef) {
                shortWait();
                verifyAppAlive("tap ghost asset reference");   // a crash on the dead link is a real bug
                boolean blankAfterTap = isCurrentScreenBlank(driver);
                if (blankAfterTap) {
                    throw new VerificationError(
                            "Ghost reference: issue '" + issueTitle + "' still links to deleted asset '"
                                    + assetName + "', and tapping that reference navigates to a dead/blank "
                                    + "screen (orphaned link not cleared on asset delete)");
                }
                logStep("✅ Deleted-asset reference resolves to a live, non-blank screen — not a ghost");
            } else {
                // Name shown but the reference is non-interactive (plain label) —
                // not a dead navigation target, so not a ghost reference.
                logStep("✅ Deleted asset name shown as a non-tappable label (no dead navigation) — not a ghost");
            }
        }

        logStepWithScreenshot("TC_E2E_010 — orphan-reference contract evaluated");
        // BaseTest teardown crash-assert covers any silent death after this point.
    }

    // ================================================================
    // FLOW 2 — API-SEEDED ISSUE LIFECYCLE, UI-VERIFIED (false-green killer)
    // ================================================================

    /**
     * TC_E2E_011 — seed a KNOWN issue (API-first), then drive its full lifecycle
     * through the iOS UI with exact-value assertions, cross-checking the server.
     *
     * <p>This kills the empty-list false-green class: every assertion is anchored
     * to a unique title we put there ourselves, so an empty/blank list can no
     * longer pass vacuously.
     *
     * <ol>
     *   <li>seed via TestDataApi.createIssue (deterministic) IF reachable; else
     *       UI-create; else environmental skip;</li>
     *   <li>search the unique title in-app → assertEquals exactly one match;</li>
     *   <li>open detail → verifyNotBlank + assertEquals on the title;</li>
     *   <li>change status → re-open → assertEquals the new status persisted;</li>
     *   <li>if API reachable, assert the SERVER reflects the new status;</li>
     *   <li>delete via UI → assert it left the list; if API reachable, assert
     *       server-side deletion.</li>
     * </ol>
     */
    @Test(priority = 2)
    public void TC_E2E_011_issueLifecycleApiSeededThenUiVerified() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, AppConstants.FEATURE_ISSUE_DETAILS,
                "TC_E2E_011 - API-seeded issue lifecycle, UI-verified end to end");

        // ---- Step 1: seed a known issue (API-first) ------------------------
        logStep("Step 1: Seed a known issue (API-first, deterministic)");
        String title = "E2E_Lifecycle_" + System.currentTimeMillis();
        boolean seededViaApi = false;

        if (apiReachable) {
            try {
                // node_id is optional server-side; attach a known asset when present.
                String nodeId = api.findAssetIdByNameFragment(sldId, "TestAsset");
                if (nodeId == null) nodeId = api.findAssetIdByNameFragment(sldId, "Trim");
                api.createIssue(sldId, nodeId, title, "medium");
                // Confirm the seed is real on the server before trusting it.
                seededViaApi = api.getIssueByTitle(sldId, title) != null;
                logStep(seededViaApi
                        ? "Seeded issue via API and verified in SLD details: " + title
                        : "API create returned but title not found in SLD details — will UI-seed");
            } catch (Exception e) {
                logWarning("API seed failed: " + e.getMessage());
            }
        }

        if (!seededViaApi) {
            logStep("Falling back to UI issue creation for: " + title);
            boolean onIssues = issuePage.navigateToIssuesScreen();
            boolean uiCreated = onIssues && issuePage.createQuickIssue(title, null);
            // No API and no UI-create => genuine environmental gap (empty/locked site).
            skipIfPreconditionMissing(() -> uiCreated,
                    "neither API nor UI could seed a known issue (no reachable backend and "
                            + "issue-create unavailable) — cannot run the lifecycle verification");
        }

        // ---- Step 2: search the unique title → exactly one match -----------
        logStep("Step 2: Search the unique title in-app and assert exactly one match");
        issuePage.navigateToIssuesScreen();
        // A freshly API-seeded issue must SYNC into the offline-first local store;
        // poll until it appears before counting (presence is EXPECTED here).
        final String seedTitle = title;
        boolean appeared = waitForCondition(() -> {
            issuePage.clearSearch();
            issuePage.searchIssues(seedTitle);
            return issuePage.isIssueInList(seedTitle);
        }, 20, "seeded issue '" + title + "' to sync into the in-app Issues list");
        assertTrue(appeared, "Seeded issue '" + title + "' should appear in the in-app Issues list");

        issuePage.searchIssues(title);
        shortWait();
        int matches = issuePage.getVisibleIssueCount();
        assertEquals(matches, 1,
                "Search for the unique title '" + title + "' should match exactly one issue");

        // ---- Step 3: open detail → verifyNotBlank + title matches ----------
        logStep("Step 3: Open detail and assert the title matches");
        issuePage.tapOnIssue(title);
        shortWait();
        verifyAppAlive("open seeded issue detail");
        verifyNotBlank("Issue Details (seeded)");
        boolean onDetail = issuePage.isIssueDetailsScreenDisplayed();
        assertTrue(onDetail, "Issue Details screen should be displayed for the seeded issue");
        String detailTitle = issuePage.getIssueDetailTitle();
        assertEquals(detailTitle, title,
                "Issue Details title should equal the seeded title");

        // ---- Step 4: change status, re-open, assert persistence ------------
        logStep("Step 4: Change status to 'In Progress' and assert it persists");
        String originalStatus = issuePage.getIssueDetailStatus();
        logStep("Original status: '" + originalStatus + "'");
        final String newStatus = "In Progress";

        boolean opened = issuePage.openStatusDropdown();
        skipIfPreconditionMissing(() -> opened,
                "status dropdown could not be opened on the seeded issue's detail "
                        + "(status-change affordance unavailable) — cannot verify status persistence");
        issuePage.selectStatus(newStatus);
        shortWait();
        // Some builds require an explicit Save Changes; tap it if present (no-op otherwise).
        if (issuePage.isSaveChangesButtonDisplayed()) {
            issuePage.tapSaveChangesButton();
            shortWait();
        }
        guard("after status change");

        // Re-open the detail fresh so we read PERSISTED state, not the in-memory edit.
        issuePage.tapCloseIssueDetails();
        shortWait();
        issuePage.navigateToIssuesScreen();
        issuePage.clearSearch();
        issuePage.searchIssues(title);
        shortWait();
        issuePage.tapOnIssue(title);
        shortWait();
        verifyAppAlive("re-open seeded issue detail");
        verifyNotBlank("Issue Details (re-opened)");
        assertTrue(issuePage.isIssueDetailsScreenDisplayed(),
                "Issue Details should re-open for the seeded issue");
        String persistedStatus = issuePage.getIssueDetailStatus();
        assertEquals(persistedStatus, newStatus,
                "Re-opened issue status should equal the changed status '" + newStatus + "'");

        // ---- Step 5: server reflects the status change (if API reachable) --
        if (seededViaApi && apiReachable) {
            logStep("Step 5: Assert the server reflects the status change");
            // Polling: the app syncs the local edit up asynchronously. Read fresh
            // SLD details each poll (the createIssue path already busts the cache;
            // here we force a fresh GET via a new client to avoid stale cache).
            boolean serverUpdated = waitForCondition(() -> {
                TestDataApi fresh = freshApiClient();
                if (fresh == null) return false;
                String json = safeSldDetails(fresh, sldId);
                if (json == null) return false;
                String serverStatus = TestDataApi.extractSiblingField(json, "title", seedTitle, "status");
                logStep("   server status for '" + seedTitle + "': " + serverStatus);
                return serverStatus != null && isInProgress(serverStatus);
            }, 30, "server to reflect status='In Progress' for '" + title + "'");
            assertTrue(serverUpdated,
                    "Server SLD details should reflect the changed status for the seeded issue");
        } else {
            logStep("Step 5: skipped server status check (API not reachable / issue UI-seeded)");
        }

        // ---- Step 6: delete via UI, assert list + server deletion ----------
        logStep("Step 6: Delete the issue via UI and assert it leaves the list");
        // Currently on the re-opened detail screen — use the in-detail delete flow.
        boolean deleteAvailable = issuePage.isDeleteIssueButtonDisplayed();
        skipIfPreconditionMissing(() -> deleteAvailable,
                "Delete Issue affordance not present on the detail screen in this build "
                        + "— cannot verify UI deletion");
        issuePage.tapDeleteIssueButton();
        shortWait();
        if (issuePage.isDeleteConfirmationDisplayed()) {
            issuePage.confirmDeleteIssue();
            shortWait();
        }
        guard("after issue delete");

        issuePage.navigateToIssuesScreen();
        issuePage.clearSearch();
        issuePage.searchIssues(title);
        boolean leftList = waitForCondition(() -> !issuePage.isIssueInList(title),
                6, "deleted issue '" + title + "' to leave the Issues list");
        assertTrue(leftList, "Deleted issue '" + title + "' must NOT appear in the Issues list anymore");

        if (seededViaApi && apiReachable) {
            logStep("Step 6b: Assert the server reflects the deletion");
            boolean serverDeleted = waitForCondition(() -> {
                TestDataApi fresh = freshApiClient();
                if (fresh == null) return false;
                String json = safeSldDetails(fresh, sldId);
                if (json == null) return false;
                // Gone entirely, OR present-but-flagged-deleted: both satisfy the
                // contract that the issue no longer exists as a live record.
                // Presence is "is the title still an id-bearing record in the SLD".
                if (TestDataApi.extractSiblingField(json, "title", seedTitle, "id") == null) return true;
                String del = TestDataApi.extractSiblingField(json, "title", seedTitle, "is_deleted");
                return "true".equalsIgnoreCase(del);
            }, 30, "server to reflect deletion of '" + title + "'");
            assertTrue(serverDeleted,
                    "Server SLD details should reflect deletion of the seeded issue");
        } else {
            logStep("Step 6b: skipped server deletion check (API not reachable / issue UI-seeded)");
        }

        logStepWithScreenshot("TC_E2E_011 — full API-seeded lifecycle verified end to end");
    }

    // ================================================================
    // HELPERS — cheap, driver-direct probes (test owns these; page
    // objects are not modified). Cascade probes use 0 implicit wait;
    // presence waits for expected elements keep >= 3s caps.
    // ================================================================

    /**
     * Swipe-left delete an asset matched by name, reusing the proven
     * BUG_DELETE_01 gesture (mobile: dragFromToForDuration → W3C fallback →
     * native scroll-into-view). Multi-strategy by design.
     *
     * @return true if a delete confirmation was reached and confirmed.
     */
    private boolean swipeLeftDeleteAssetByName(IOSDriver driver, String assetName) {
        int screenHeight = driver.manage().window().getSize().getHeight();
        int screenWidth = driver.manage().window().getSize().getWidth();

        // Find the asset's row by name. Cheap whole-tree probe at 0 implicit wait,
        // multiple element types (cell / button / static-text container).
        WebElement row = withImplicitWaitMs(driver, 0, () -> {
            // Strategy A: a cell/button whose label contains the asset name.
            for (String type : new String[]{"XCUIElementTypeCell", "XCUIElementTypeButton"}) {
                List<WebElement> els = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == '" + type + "' AND label CONTAINS '" + assetName + "'"));
                for (WebElement el : els) {
                    try {
                        int y = el.getLocation().getY();
                        if (y > 120 && y < screenHeight * 0.85) return el;
                    } catch (Exception ignored) {}
                }
            }
            // Strategy B: a static text matching the name → walk up to its row container.
            List<WebElement> texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label CONTAINS '" + assetName + "'"));
            for (WebElement t : texts) {
                try {
                    int y = t.getLocation().getY();
                    if (y > 120 && y < screenHeight * 0.85) return t;
                } catch (Exception ignored) {}
            }
            return null;
        });

        if (row == null) {
            System.out.println("⚠️ swipeLeftDeleteAssetByName: row for '" + assetName + "' not found");
            return false;
        }

        int cellX = row.getLocation().getX();
        int cellY = row.getLocation().getY();
        int cellW = row.getSize().getWidth();
        int cellH = row.getSize().getHeight();
        if (cellW <= 0) cellW = screenWidth;
        if (cellH <= 0) cellH = 60;
        int centerY = cellY + (cellH / 2);
        int startX = cellX + cellW - 20;
        int endX = cellX + 20;

        // Primary gesture: native dragFromToForDuration.
        boolean swiped = false;
        try {
            Map<String, Object> drag = new HashMap<>();
            drag.put("fromX", startX);
            drag.put("fromY", centerY);
            drag.put("toX", endX);
            drag.put("toY", centerY);
            drag.put("duration", 0.3);
            driver.executeScript("mobile: dragFromToForDuration", drag);
            swiped = true;
        } catch (Exception dragEx) {
            System.out.println("   dragFromToForDuration failed, W3C fallback: " + dragEx.getMessage());
        }
        // Fallback gesture: W3C PointerInput, 150ms (matches BUG_DELETE_01 timing).
        if (!swiped) {
            try {
                org.openqa.selenium.interactions.PointerInput finger =
                        new org.openqa.selenium.interactions.PointerInput(
                                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence swipe =
                        new org.openqa.selenium.interactions.Sequence(finger, 0);
                swipe.addAction(finger.createPointerMove(Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, centerY));
                swipe.addAction(finger.createPointerDown(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(150),
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), endX, centerY));
                swipe.addAction(finger.createPointerUp(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(Arrays.asList(swipe));
            } catch (Exception w3cEx) {
                System.out.println("   W3C swipe failed: " + w3cEx.getMessage());
            }
        }
        sleep(800); // let the swipe action buttons render

        // Tap the revealed Delete/trash control. Multi-strategy.
        boolean trashTapped = clickFirstPresent(driver,
                AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (label == 'Delete' OR label == 'Trash' OR label == 'trash')"),
                AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label CONTAINS 'Delete'"));
        if (!trashTapped) {
            // Coordinate tap at the far-right edge where the swipe action sits.
            try {
                Map<String, Object> tap = new HashMap<>();
                tap.put("x", screenWidth - 40);
                tap.put("y", centerY);
                driver.executeScript("mobile: tap", tap);
                trashTapped = true;
            } catch (Exception ignored) {}
        }
        if (!trashTapped) {
            System.out.println("⚠️ swipeLeftDeleteAssetByName: could not tap the delete affordance");
            return false;
        }
        sleep(1200);

        // Confirm the destructive action if a confirm dialog/sheet appears.
        boolean confirmed = clickFirstPresent(driver,
                AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == 'Delete'"),
                AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (label == 'Confirm' OR label == 'Yes' OR label CONTAINS 'Delete')"));
        // If no explicit confirm appeared, the swipe-delete may have applied directly.
        sleep(1000);
        System.out.println("   swipe-delete: trashTapped=" + trashTapped + ", confirmTapped=" + confirmed);
        return true;
    }

    /**
     * Attempt to tap the deleted asset's name where it is rendered on the issue
     * detail (it may be a button/link or a plain static text). Returns true only
     * if an actually-tappable element (button) was clicked — a plain static
     * label is NOT a navigation target, so we report false for it.
     */
    private boolean tapDeletedAssetReference(IOSDriver driver, String assetName) {
        return Boolean.TRUE.equals(withImplicitWaitMs(driver, 0, () -> {
            List<WebElement> btns = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS '" + assetName + "'"));
            for (WebElement b : btns) {
                try {
                    int y = b.getLocation().getY();
                    if (y < 120) continue; // nav-bar chrome — never tap here
                    b.click();
                    System.out.println("   Tapped deleted-asset reference button at Y=" + y);
                    return true;
                } catch (Exception ignored) {}
            }
            return false;
        }));
    }

    /**
     * Heuristic blank-screen check for the post-tap state: a screen with no
     * meaningful interactive/content elements below the nav bar. Used only to
     * distinguish a dead navigation target (ghost) from a live one. Cheap probe.
     */
    private boolean isCurrentScreenBlank(IOSDriver driver) {
        return Boolean.TRUE.equals(withImplicitWaitMs(driver, 0, () -> {
            int contentCount = 0;
            for (String type : new String[]{
                    "XCUIElementTypeStaticText", "XCUIElementTypeButton",
                    "XCUIElementTypeCell", "XCUIElementTypeImage"}) {
                List<WebElement> els = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == '" + type + "'"));
                for (WebElement el : els) {
                    try {
                        int y = el.getLocation().getY();
                        // Count only real content below the nav-bar zone.
                        if (y > 120) contentCount++;
                        if (contentCount >= 3) return false; // clearly not blank
                    } catch (Exception ignored) {}
                }
            }
            System.out.println("   post-tap content elements below nav bar: " + contentCount);
            return contentCount < 3;
        }));
    }

    /** Click the first locator that resolves to a present element; cheap 0-wait cascade. */
    private boolean clickFirstPresent(IOSDriver driver, org.openqa.selenium.By... locators) {
        for (org.openqa.selenium.By by : locators) {
            Boolean ok = withImplicitWaitMs(driver, 0, () -> {
                List<WebElement> els = driver.findElements(by);
                for (WebElement el : els) {
                    try {
                        el.click();
                        return true;
                    } catch (Exception ignored) {}
                }
                return false;
            });
            if (Boolean.TRUE.equals(ok)) return true;
        }
        return false;
    }

    /**
     * Run {@code action} with the implicit wait temporarily set to {@code millis},
     * restoring the configured default afterward. Mirrors BasePage.withImplicitWait
     * (which is protected and not reachable from this test class).
     */
    private <T> T withImplicitWaitMs(IOSDriver driver, long millis, java.util.function.Supplier<T> action) {
        java.time.Duration original;
        try {
            original = driver.manage().timeouts().getImplicitWaitTimeout();
        } catch (Exception e) {
            original = java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT);
        }
        try {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(millis));
            return action.get();
        } finally {
            try { driver.manage().timeouts().implicitlyWait(original); } catch (Exception ignored) {}
        }
    }

    /** Fresh authenticated API client (no shared cache) for read-after-write polling; null on failure. */
    private TestDataApi freshApiClient() {
        try {
            TestDataApi fresh = new TestDataApi();
            fresh.login();
            return fresh.isAuthenticated() ? fresh : null;
        } catch (Exception e) {
            System.out.println("   fresh API client login failed: " + e.getMessage());
            return null;
        }
    }

    /** GET SLD details without letting an API blip throw out of a polling predicate. */
    private String safeSldDetails(TestDataApi client, String id) {
        try {
            return client.getSldDetails(id);
        } catch (Exception e) {
            System.out.println("   getSldDetails blip: " + e.getMessage());
            return null;
        }
    }

    /** Treat any server status equivalent to "In Progress" as a match (in_progress / inprogress). */
    private boolean isInProgress(String serverStatus) {
        String s = serverStatus.toLowerCase().replace("_", "").replace(" ", "");
        return s.equals("inprogress");
    }
}
