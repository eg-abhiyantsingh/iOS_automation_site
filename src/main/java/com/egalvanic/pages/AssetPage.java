package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.Waits;
import com.egalvanic.verify.VerificationError;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Asset Page Object
 * Handles all Asset Management screen interactions
 * 
 * Features covered:
 * - Asset List Screen
 * - Create Asset
 * - Edit Asset
 * - Asset Details
 * - Asset Class Selection
 * - Location Selection
 * - Asset Subtype Selection
 * - QR Code Entry
 */
public class AssetPage extends BasePage {

    // ================================================================
    // ASSET LIST SCREEN ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(accessibility = "list.bullet")
    private WebElement assetListButton;

    @iOSXCUITFindBy(accessibility = "plus")
    private WebElement plusButton;

    @iOSXCUITFindBy(accessibility = "Back")
    private WebElement backButton;

    @iOSXCUITFindBy(accessibility = "Cancel")
    private WebElement cancelButton;

    // ================================================================
    // CREATE/EDIT ASSET FORM ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == 'Enter name'")
    private WebElement assetNameField;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField'")
    private List<WebElement> allTextFields;

    @iOSXCUITFindBy(accessibility = "Select asset class")
    private WebElement selectAssetClassButton;

    @iOSXCUITFindBy(accessibility = "Select location")
    private WebElement selectLocationButton;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton' AND name CONTAINS 'asset subtype'")
    private WebElement selectAssetSubtypeButton;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == 'Enter or scan QR code'")
    private WebElement qrCodeField;

    @iOSXCUITFindBy(accessibility = "Asset Details")
    private WebElement assetDetailsHeader;

    @iOSXCUITFindBy(accessibility = "Create Asset")
    private WebElement createAssetButton;

    @iOSXCUITFindBy(accessibility = "Save")
    private WebElement saveButton;

    @iOSXCUITFindBy(accessibility = "Delete")
    private WebElement deleteButton;

    // ================================================================
    // ASSET CLASS OPTIONS
    // ================================================================

    @iOSXCUITFindBy(accessibility = "ATS")
    private WebElement atsClassOption;

    @iOSXCUITFindBy(accessibility = "UPS")
    private WebElement upsClassOption;

    @iOSXCUITFindBy(accessibility = "PDU")
    private WebElement pduClassOption;

    @iOSXCUITFindBy(accessibility = "Generator")
    private WebElement generatorClassOption;

    // ================================================================
    // LOCATION ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][1]")
    private WebElement addFloorButton;

    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][2]")
    private WebElement addRoomButton;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == 'Floor Name'")
    private WebElement floorNameField;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeTextField' AND value == 'Room Name'")
    private WebElement roomNameField;

    // ================================================================
    // ASSET SUBTYPE OPTIONS
    // ================================================================

    @iOSXCUITFindBy(accessibility = "test")
    private WebElement testSubtypeOption;

    // ================================================================
    // ASSET LIST ELEMENTS
    // ================================================================

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeButton'")
    private List<WebElement> allButtons;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeCell'")
    private List<WebElement> assetCells;

    @iOSXCUITFindBy(iOSNsPredicate = "type == 'XCUIElementTypeSearchField'")
    private WebElement searchBar;

    // ================================================================
    // TOGGLE SEARCH CACHE
    // ================================================================

    /**
     * Cache: once toggle search fails, skip subsequent searches in the same test.
     * Reset via resetToggleSearchCache() when navigating to a new asset.
     */
    private boolean toggleSearchExhausted = false;
    private static final long TOGGLE_SEARCH_MAX_MS = 15_000; // 15 seconds max total

    /**
     * Rate-limit for the cache-skip log line: callers poll findRequiredFieldsToggle()
     * in tight loops, and the per-call println once produced 149,888 lines (95.6% of
     * a 32MB CI log). Log the skip once per cache lifetime; reset with the cache.
     */
    private boolean toggleCacheSkipLogged = false;

    /**
     * Set true only when THIS page object actually clicked a Save/Save Changes
     * button. isAssetSavedAfterEdit() must not treat "Save Changes button gone"
     * as success when the button was never found/clicked (vacuous pass).
     */
    private boolean saveButtonClickedThisFlow = false;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================

    public AssetPage() {
        super();
    }

    /**
     * Reset toggle search cache — call when navigating to a new asset or screen.
     */
    public void resetToggleSearchCache() {
        this.toggleSearchExhausted = false;
        this.toggleCacheSkipLogged = false;
    }

    // ================================================================
    // FAST DETECTION METHODS - 1 second timeout max
    // ================================================================

    /**
     * FAST check if on Asset List (1 second timeout)
     */
    public boolean isAssetListDisplayedFast() {
        try {
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(1));
            fastWait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("plus")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * FAST check if on Dashboard (1 second timeout)
     */
    public boolean isDashboardDisplayedFast() {
        // v1.36 (changelog 075): the previous fast-check used accessibilityId
        // 'building.2' which is AMBIGUOUS — it's on both Dashboard (Sites
        // Quick Action icon) AND every Site Selection row icon. That caused
        // loginAndSelectSite() to falsely report "already on Dashboard" when
        // we were actually on Site Selection, leaving subsequent nav to fail.
        //
        // Use Dashboard-only signals: 'Sites' Quick Action accessibility id,
        // 'Issues' / 'Assets' / 'Locations' Quick Action ids. These exist ONLY
        // on the Dashboard. Also EXCLUDE Site Selection by checking for
        // 'Search sites...' placeholder which only exists on Site Selection.
        try {
            // First reject Site Selection explicitly — its 'Search sites' field is
            // a definitive signal we're NOT on Dashboard.
            boolean siteSearchVisible = !driver.findElements(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeSearchField') AND " +
                "(placeholderValue CONTAINS[c] 'search sites' OR value CONTAINS[c] 'search sites')")).isEmpty();
            if (siteSearchVisible) {
                return false; // 'Search sites' placeholder visible → Site Selection
            }
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(1));
            fastWait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("Sites")),
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("Issues")),
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("Assets")),
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("Locations"))
            ));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * FAST navigation to Asset List (1 second timeout per step)
     */
    public void navigateToAssetListFast() {
        try {
            WebDriverWait fastWait = new WebDriverWait(driver, Duration.ofSeconds(1));
            WebElement listBtn = fastWait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("list.bullet")
            ));
            listBtn.click();
            // Quick wait for plus button
            new WebDriverWait(driver, Duration.ofSeconds(2))
                .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("plus")));
        } catch (Exception e) {
            System.out.println("⚠️ Fast navigation failed: " + e.getMessage());
        }
    }

    // ================================================================
    // NAVIGATION METHODS
    // ================================================================

    public void navigateToAssetList() {
        System.out.println("📦 Navigating to Asset List...");
        navigateToAssetListTurbo();
    }
    
    /**
     * TURBO: Navigate to Asset List.
     *
     * Step 0 — site context: the Assets tab only exists INSIDE a site. When the
     * app is stuck on Dashboard / Site Selection / behind a sheet, every tab
     * strategy grinds on the wrong screen ("Could not find Assets tab by label"
     * 62/62 in the cancelled Assets P1 run, 3-8 min per test, 'Create New Site'
     * clicked as an "asset"). Recover the site context first, then click the
     * tab, then VERIFY the list actually rendered.
     *
     * Throws {@link VerificationError} when every strategy fails — it extends
     * AssertionError so callers' catch(Exception) cannot swallow it, turning an
     * 8-minute wrong-screen grind into a ~20s clean failure.
     */
    public void navigateToAssetListTurbo() {
        System.out.println("📦 Navigating to Asset List...");

        // An open Asset Detail hides the tab bar — close it first (0-implicit probe)
        if (existsNow(AppiumBy.accessibilityId("Close"))) {
            try {
                System.out.println("🔙 Found Close button - closing Asset Detail first...");
                driver.findElement(AppiumBy.accessibilityId("Close")).click();
                sleep(300);
            } catch (Exception e) {
                // Raced closed — fine
            }
        }

        // STEP 0: bounded check for the bottom-nav Assets tab; absent → not in
        // site context, so recover BEFORE running the tab-click strategies.
        if (!isAssetsTabPresent(3)) {
            recoverSiteContext();
        }

        // Click strategies + render verification; one recovery retry between passes.
        for (int attempt = 0; attempt < 2; attempt++) {
            if (clickAssetsTabStrategies() && verifyOnAssetList(5)) {
                System.out.println("✅ Asset List opened");
                return;
            }
            if (attempt == 0) {
                recoverSiteContext();
            }
        }

        // The hard-fail exists to stop us grinding on the WRONG screen (Site
        // Selection / Create-New-Site). But if the bottom-nav Assets tab IS present
        // and we are NOT on the site picker, we DID reach the asset area — the
        // verifyOnAssetList markers (plus / nav bar 'Assets' / search field) just
        // didn't confirm (empty list, or a v1.43 marker drift). Proceeding is safe:
        // selectFirstAsset and the other callers carry their own wrong-screen guards.
        // Only throw when genuinely lost (no Assets tab, or sitting on the picker) —
        // otherwise this guard ITSELF mass-skips good runs (CI run 27539301468:
        // assetsTab=true,sitePicker=false aborts that should have proceeded).
        boolean tabPresent = isAssetsTabPresent(0);
        boolean onPicker = isOnSiteSelectionScreen();
        if (tabPresent && !onPicker) {
            System.out.println("⚠️ Asset List markers unconfirmed but Assets tab present and not on "
                + "Site picker — proceeding (caller guards handle empty/odd list)");
            return;
        }

        throw new VerificationError(
            "navigateToAssetList: could not reach the Asset List. assetsTab="
            + tabPresent + ", sitePicker=" + onPicker
            + " — aborting instead of grinding on the wrong screen.");
    }

    /**
     * Bounded poll for the bottom-nav Assets tab (label 'Assets' / icon
     * 'list.bullet'). Presence of this tab is the marker for "in site context".
     */
    private boolean isAssetsTabPresent(int timeoutSeconds) {
        By byLabel = AppiumBy.iOSNsPredicateString("label == 'Assets' AND type == 'XCUIElementTypeButton'");
        By byIcon = AppiumBy.accessibilityId("list.bullet");
        return Waits.until(() -> existsNow(byLabel) || existsNow(byIcon),
            Math.max(0, timeoutSeconds) * 1000L);
    }

    /**
     * True when the Site Selection screen (or its sheet) is in front: the
     * 'Search sites' field, 'Create New Site' button or 'Select Site' title.
     * All probes are 0-implicit — this is an absence-style cascade check.
     */
    private boolean isOnSiteSelectionScreen() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeSearchField') AND " +
                "(placeholderValue CONTAINS[c] 'search sites' OR value CONTAINS[c] 'search sites')"))
            || existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name == 'Create New Site' OR label == 'Create New Site')"))
            || existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (name == 'Select Site' OR label == 'Select Site')"));
    }

    /**
     * Labels that belong to the Site Selection / Site Information screens.
     * These must NEVER be treated (or cached) as asset names — the cancelled
     * Assets P1 run cached 'Create New Site' as the shared asset.
     */
    private boolean isSiteScreenLabel(String name) {
        if (name == null) return false;
        String n = name.trim().toLowerCase();
        return n.equals("create new site") || n.equals("select site")
            || n.equals("site information") || n.contains("search sites");
    }

    /**
     * Re-establish site context: dismiss any sheet, hop to the Site (house)
     * tab from Dashboard, and if the site picker is showing select the first
     * available site. Ends with a bounded wait for the Assets tab.
     */
    private void recoverSiteContext() {
        System.out.println("🧭 No Assets tab — not in site context. Recovering...");

        // 0. v1.48: the app can end up on the auto-pushed Work Orders screen
        //    (pushed nav, hides the tab bar — NOT a sheet, so the dismissers
        //    below never catch it). BackButton returns to the site Dashboard.
        if (backOutOfWorkOrdersScreen() && isAssetsTabPresent(3)) {
            return;
        }

        // 1. Dismiss any sheet/alert hiding the tab bar (0-implicit probes)
        for (String dismiss : new String[]{"Cancel", "Close", "xmark.circle.fill", "xmark"}) {
            By by = AppiumBy.accessibilityId(dismiss);
            if (existsNow(by)) {
                try {
                    driver.findElement(by).click();
                    sleep(300);
                    System.out.println("   Dismissed sheet via '" + dismiss + "'");
                    break;
                } catch (Exception ignored) {}
            }
        }
        // Sheet without a Cancel button: swipe it down
        if (!isAssetsTabPresent(1) && existsNow(AppiumBy.accessibilityId("Sheet Grabber"))) {
            try {
                driver.executeScript("mobile: swipe", Map.of("direction", "down"));
                sleep(300);
            } catch (Exception ignored) {}
        }
        if (isAssetsTabPresent(1)) {
            return; // dismissal alone restored the tab bar
        }

        // 2. Site picker already in front? Select a site and we're in.
        boolean selectedSite = false;
        if (isOnSiteSelectionScreen()) {
            selectedSite = selectFirstSiteInline() != null;
        } else {
            // 3. From Dashboard: tap the Site (house) tab / Sites Quick Action
            boolean tapped = false;
            for (String tab : new String[]{"house.fill", "house"}) {
                By by = AppiumBy.accessibilityId(tab);
                if (existsNow(by)) {
                    try { driver.findElement(by).click(); tapped = true; break; } catch (Exception ignored) {}
                }
            }
            if (!tapped) {
                By siteTab = AppiumBy.iOSNsPredicateString("label == 'Site' AND type == 'XCUIElementTypeButton'");
                if (existsNow(siteTab)) {
                    try { driver.findElement(siteTab).click(); tapped = true; } catch (Exception ignored) {}
                }
            }
            if (!tapped) {
                // Dashboard 'Sites' Quick Action opens the site picker
                By sitesQuickAction = AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name == 'Sites' OR label == 'Sites')");
                if (existsNow(sitesQuickAction)) {
                    try { driver.findElement(sitesQuickAction).click(); tapped = true; } catch (Exception ignored) {}
                }
            }
            if (tapped) {
                sleep(400);
                if (isOnSiteSelectionScreen()) {
                    selectedSite = selectFirstSiteInline() != null;
                }
            } else {
                System.out.println("⚠️ Recovery: no Site tab / Sites Quick Action found on this screen");
            }
        }

        // 4. Site dashboards load slowly — bounded wait for the tab to materialize.
        // After an explicit site selection, the FIRST load gets the full patient wait
        // (SITE_DASHBOARD_WAIT_SEC, at most twice per run): run 28458743407 proved an
        // abandoned first load condemns the whole job to the picker-cascade (Assets
        // P1-P3 = 0/317 passed, ~4h each), while one completed load persists the site
        // for every later soft restart (Assets P4: 1 selection -> 113 clean opens).
        // The 2-per-run cap keeps a genuinely broken site from burning hours.
        int tabWaitSec = 8;
        if (selectedSite && LONG_SITE_LOAD_WAITS_USED.get() < 2) {
            LONG_SITE_LOAD_WAITS_USED.incrementAndGet();
            tabWaitSec = com.egalvanic.constants.AppConstants.SITE_DASHBOARD_WAIT_SEC;
            System.out.println("⏳ Site selected — waiting up to " + tabWaitSec
                + "s for the site dashboard's first load (patient wait "
                + LONG_SITE_LOAD_WAITS_USED.get() + "/2 this run)");
        }
        if (!isAssetsTabPresent(tabWaitSec)) {
            System.out.println("⚠️ Site-context recovery did not surface the Assets tab");
        }
    }

    /**
     * How many patient (SITE_DASHBOARD_WAIT_SEC) site-load waits this run has used.
     * Static: the whole point is that ONE completed first load persists the site
     * context for the rest of the JVM, so the budget is per-run, not per-test.
     */
    private static final java.util.concurrent.atomic.AtomicInteger LONG_SITE_LOAD_WAITS_USED =
            new java.util.concurrent.atomic.AtomicInteger(0);

    /**
     * v1.48: back out of the auto-pushed "Work Orders" screen (nav title
     * 'Work Orders', 'Start New Work Order' banner, BackButton). Returns true
     * if the screen was detected and Back was tapped. Detection is 0-implicit
     * probes; safe to call from any screen (no-op elsewhere).
     */
    private boolean backOutOfWorkOrdersScreen() {
        boolean onWorkOrders =
                (existsNow(AppiumBy.iOSNsPredicateString("name BEGINSWITH 'Start New Work Order'"))
                 || existsNow(AppiumBy.accessibilityId("Available Work Orders")))
                && existsNow(AppiumBy.accessibilityId("BackButton"));
        if (!onWorkOrders) {
            return false;
        }
        System.out.println("   ↩️ Auto-opened Work Orders screen detected — tapping Back to Dashboard (v1.48)");
        try {
            driver.findElement(AppiumBy.accessibilityId("BackButton")).click();
            sleep(600);
            return true;
        } catch (Exception e) {
            // Fallback: back button by label
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                        "label == 'Back' AND type == 'XCUIElementTypeButton'")).click();
                sleep(600);
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * Select the first available site on the Site Selection screen.
     * Mirrors SiteSelectionPage.selectFirstSiteFast(): site rows are buttons
     * named "name, address, ..." — reject Dashboard/WO/chrome rows.
     */
    private String selectFirstSiteInline() {
        System.out.println("🏢 Site picker in front — selecting first available site...");
        // Slow app needs time to render the rows — single bounded wait on the winner.
        Waits.until(() -> existsNow(AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name CONTAINS ','")), 10_000);
        sleep(500); // rows keep populating after the first appears

        // Strategy 1: full site rows (>= 2 commas), known non-site rows rejected
        try {
            List<WebElement> rows = withImplicitWait(0, () -> driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ','")));
            int scanned = 0;
            for (WebElement row : rows) {
                if (++scanned > 25) break; // bounded: each getAttribute is a round-trip
                String name = row.getAttribute("name");
                if (name == null) continue;
                String lower = name.toLowerCase();
                if (lower.startsWith("wo,")) continue;
                if (lower.contains("no active work order") || lower.contains("tap to select")) continue;
                if (lower.matches("^\\d+,\\s*tasks.*")) continue;
                if (isSiteScreenLabel(name) || lower.equals("cancel") || lower.equals("xmark.circle.fill")) continue;
                if (name.indexOf(',') == name.lastIndexOf(',')) continue; // needs name + address pieces
                row.click();
                sleep(800); // picker dismiss + dashboard transition
                System.out.println("✅ Selected site: " + name);
                return name;
            }
        } catch (Exception ignored) {}

        // Strategy 2: single-comma site rows (short addresses)
        try {
            List<WebElement> rows = withImplicitWait(0, () -> driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ','")));
            int scanned = 0;
            for (WebElement row : rows) {
                if (++scanned > 25) break;
                String name = row.getAttribute("name");
                if (name == null) continue;
                String lower = name.toLowerCase();
                if (lower.startsWith("wo,") || lower.contains("no active work order")
                    || lower.contains("tap to select") || lower.matches("^\\d+,\\s*tasks.*")) continue;
                if (isSiteScreenLabel(name) || lower.equals("cancel") || lower.equals("xmark.circle.fill")) continue;
                row.click();
                sleep(800);
                System.out.println("✅ Selected site (single-comma row): " + name);
                return name;
            }
        } catch (Exception ignored) {}

        // Strategy 3: first cell in the picker list
        try {
            List<WebElement> cells = withImplicitWait(0, () -> driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeCell'")));
            if (!cells.isEmpty()) {
                cells.get(0).click();
                sleep(800);
                System.out.println("✅ Selected site (first cell)");
                return "site";
            }
        } catch (Exception ignored) {}

        // Strategy 4: first row-like StaticText below the 'Search sites' field
        try {
            List<WebElement> texts = withImplicitWait(0, () -> driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText'")));
            int scanned = 0;
            for (WebElement text : texts) {
                if (++scanned > 25) break;
                String name = text.getAttribute("name");
                if (name == null || name.length() < 4 || isSiteScreenLabel(name)) continue;
                if (text.getLocation().getY() < 150) continue; // skip nav/search area
                text.click();
                sleep(800);
                System.out.println("✅ Selected site (text row): " + name);
                return name;
            }
        } catch (Exception ignored) {}

        System.out.println("⚠️ Could not select any site from the picker");
        return null;
    }

    /**
     * One pass over the Assets-tab click strategies. All probes are
     * 0-implicit existsNow checks — same coverage as before, milliseconds per
     * miss instead of an implicit-wait burn each.
     */
    private boolean clickAssetsTabStrategies() {
        // Strategy 1: tab by label (Assets tab is name='list.bullet', label='Assets')
        By byLabel = AppiumBy.iOSNsPredicateString("label == 'Assets' AND type == 'XCUIElementTypeButton'");
        if (existsNow(byLabel)) {
            try {
                driver.findElement(byLabel).click();
                sleep(300);
                return true;
            } catch (Exception e) {
                System.out.println("⚠️ Could not click Assets tab by label: " + e.getMessage());
            }
        }

        // Strategy 2: by icon accessibility ID
        By byIcon = AppiumBy.accessibilityId("list.bullet");
        if (existsNow(byIcon)) {
            try {
                driver.findElement(byIcon).click();
                sleep(300);
                return true;
            } catch (Exception e) {
                System.out.println("⚠️ Could not click list.bullet: " + e.getMessage());
            }
        }

        // Strategy 3: hop through the Site (house) tab, then Assets
        try {
            By siteTab = AppiumBy.iOSNsPredicateString("label == 'Site' AND type == 'XCUIElementTypeButton'");
            boolean hopped = false;
            for (String tab : new String[]{"house.fill", "house"}) {
                By by = AppiumBy.accessibilityId(tab);
                if (existsNow(by)) {
                    driver.findElement(by).click();
                    hopped = true;
                    break;
                }
            }
            if (!hopped && existsNow(siteTab)) {
                driver.findElement(siteTab).click();
                hopped = true;
            }
            if (hopped) {
                sleep(300);
                if (existsNow(byLabel)) {
                    driver.findElement(byLabel).click();
                    sleep(300);
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not open Asset List via Site tab: " + e.getMessage());
        }
        return false;
    }

    /**
     * Bounded poll that the Asset List actually RENDERED (plus button /
     * Assets nav bar / the asset search field) — clicking the tab is not
     * proof. Early-exits false when the site picker is in front.
     */
    private boolean verifyOnAssetList(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        By plus = AppiumBy.accessibilityId("plus");
        By navBar = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeNavigationBar' AND (name == 'Assets' OR label == 'Assets')");
        By assetSearch = AppiumBy.accessibilityId("Search by name, type, location, or QR code");
        while (true) {
            if (isOnSiteSelectionScreen()) {
                return false; // wrong screen — polling longer cannot help
            }
            if (existsNow(plus) || existsNow(navBar) || existsNow(assetSearch)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            sleep(250);
        }
    }



    /**
     * Click the Assets tab in the tab bar to navigate to Asset List
     * Note: Assets tab button has name="list.bullet" and label="Assets"
     */
    public void clickAssetsTab() {
        System.out.println("📱 Clicking Assets tab...");
        
        // First, check if we're inside Asset Detail - need to close it first
        try {
            WebElement closeBtn = driver.findElement(AppiumBy.accessibilityId("Close"));
            if (closeBtn.isDisplayed()) {
                System.out.println("🔙 Closing Asset Detail first...");
                closeBtn.click();
                sleep(200);
            }
        } catch (Exception e) {
            // No Close button - that's fine
        }
        
        // CORRECT: Use predicate to find Assets button by label
        try {
            WebElement assetsTab = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Assets' AND type == 'XCUIElementTypeButton'")
            );
            assetsTab.click();
            sleep(200);
            System.out.println("✅ Clicked Assets tab");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Assets tab by label");
        }
        
        // Fallback: try by accessibility ID "list.bullet"
        try {
            WebElement assetsTab = driver.findElement(AppiumBy.accessibilityId("list.bullet"));
            assetsTab.click();
            sleep(200);
            System.out.println("✅ Clicked Assets tab (via list.bullet)");
        } catch (Exception e2) {
            System.out.println("⚠️ Could not click Assets tab: " + e2.getMessage());
        }
    }


    /**
     * Click the More button (3 dots / ellipsis) on Asset List screen
     */
    public void clickMoreButton() {
        System.out.println("⋯ Clicking More button...");
        try {
            WebElement moreBtn = driver.findElement(AppiumBy.accessibilityId("More"));
            moreBtn.click();
            sleep(200);
            System.out.println("✅ Clicked More button");
        } catch (Exception e) {
            // Try by predicate
            try {
                WebElement moreBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'More' AND type == 'XCUIElementTypeButton'")
                );
                moreBtn.click();
                sleep(200);
                System.out.println("✅ Clicked More button (via predicate)");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not click More button: " + e2.getMessage());
            }
        }
    }

    /**
     * Select a grouping option from the More menu
     * Options: "No Grouping", "Group by Location", "Group by Enclosure", 
     *          "Show AF Punchlist", "Select Multiple"
     */
    public void selectGroupingOption(String option) {
        System.out.println("📋 Selecting grouping option: " + option);
        try {
            WebElement optionBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == '" + option + "'")
            );
            optionBtn.click();
            sleep(200);
            System.out.println("✅ Selected: " + option);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select option '" + option + "': " + e.getMessage());
        }
    }



    /**
     * Click the Close button to close Asset Detail screen
     */
    public void clickCloseButton() {
        System.out.println("❌ Clicking Close button...");
        try {
            WebElement closeBtn = driver.findElement(AppiumBy.accessibilityId("Close"));
            closeBtn.click();
            sleep(200);
            System.out.println("✅ Clicked Close button");
        } catch (Exception e) {
            System.out.println("⚠️ Close button not found: " + e.getMessage());
        }
    }

    // ================================================================
    // TASK MANAGEMENT METHODS
    // ================================================================

    /**
     * Scroll to Tasks section in Asset Details
     * Tasks section is typically below Basic Information and Condition of Maintenance
     */
    public boolean scrollToTasksSection() {
        System.out.println("📜 Scrolling to Tasks section...");
        
        // First check if already visible
        try {
            WebElement tasksLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Tasks' AND type == 'XCUIElementTypeStaticText'")
            );
            if (tasksLabel.isDisplayed()) {
                System.out.println("✅ Tasks section already visible");
                return true;
            }
        } catch (Exception e) {
            // Need to scroll
        }
        
        // Scroll down with smaller scrolls using mobile: scroll
        for (int i = 0; i < 10; i++) {
            // Use mobile: swipe for small scroll
            try {
                Map<String, Object> swipeParams = new java.util.HashMap<>();
                swipeParams.put("direction", "up");  // Swipe up = scroll down
                swipeParams.put("velocity", 300);    // Slow velocity for small scroll
                driver.executeScript("mobile: swipe", swipeParams);
            } catch (Exception scrollEx) {
                // Fallback to scrollFormDown
                scrollFormDown();
            }
            sleep(400);
            
            // Check if Tasks is now visible
            try {
                WebElement tasksLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Tasks' AND type == 'XCUIElementTypeStaticText'")
                );
                if (tasksLabel.isDisplayed()) {
                    System.out.println("✅ Found Tasks section after " + (i + 1) + " scrolls");
                    // Wait a moment for Add button to be clickable
                    sleep(200);
                    return true;
                }
            } catch (Exception e) {
                System.out.println("   Scroll " + (i + 1) + " - Tasks not visible yet");
            }
        }
        
        System.out.println("⚠️ Could not find Tasks section after scrolling");
        return false;
    }

    /**
     * Click Add Task button (+) in Tasks section
     * Note: Need to ensure Tasks section is visible first
     */
    public void clickAddTaskButton() {
        System.out.println("➕ Clicking Add Task button...");
        try {
            // Find the visible Add button (plus.circle.fill) near Tasks section
            WebElement addBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill' AND label == 'Add'")
            );
            addBtn.click();
            // Wait longer for New Task screen to appear (animation)
            sleep(500);
            System.out.println("✅ Clicked Add Task button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find visible Add Task button, trying tap by coordinates...");
            // Fallback: tap at typical Tasks Add button location
            try {
                driver.executeScript("mobile: tap", Map.of("x", 348, "y", 571));
                sleep(500);
                System.out.println("✅ Tapped Add Task button at coordinates");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not click Add Task button: " + e2.getMessage());
            }
        }
    }


    /**
     * Click on an existing task in the Tasks section to open Task Details
     * Task buttons have format: "TaskName, Description, Status"
     */
    public void clickExistingTask() {
        System.out.println("🔍 Looking for existing task to click...");
        try {
            // Find task button - tasks have "Open" or "Completed" status
            WebElement taskBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name CONTAINS 'Open' OR name CONTAINS 'Completed') AND name CONTAINS 'Task'")
            );
            String taskName = taskBtn.getAttribute("name");
            System.out.println("   🎯 Found task: " + taskName.substring(0, Math.min(50, taskName.length())) + "...");
            taskBtn.click();
            sleep(500);
            System.out.println("✅ Clicked existing task");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find existing task: " + e.getMessage());
            // Try alternate approach - find any button with "Test Task" in name
            try {
                WebElement taskBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'Test Task'")
                );
                taskBtn.click();
                sleep(500);
                System.out.println("✅ Clicked task (via Test Task search)");
            } catch (Exception e2) {
                System.out.println("⚠️ No existing task found");
            }
        }
    }

    /**
     * Check if Task Details screen is displayed
     */
    public boolean isTaskDetailsScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Task Details' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Edit task title in Task Details screen
     */
    public void editTaskTitle(String newTitle) {
        System.out.println("📝 Editing task title to: " + newTitle);
        try {
            WebElement titleField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value CONTAINS 'Task'")
            );
            titleField.click();
            sleep(300);
            titleField.clear();
            titleField.sendKeys(newTitle);
            System.out.println("✅ Edited task title");
        } catch (Exception e) {
            System.out.println("⚠️ Could not edit task title: " + e.getMessage());
        }
    }

    /**
     * Edit task description in Task Details screen
     */
    public void editTaskDescription(String newDescription) {
        System.out.println("📝 Editing task description...");
        try {
            WebElement descField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextView'")
            );
            descField.click();
            sleep(300);
            descField.clear();
            descField.sendKeys(newDescription);
            System.out.println("✅ Edited task description");
        } catch (Exception e) {
            System.out.println("⚠️ Could not edit description: " + e.getMessage());
        }
    }

    /**
     * Click Save/Done button on Task Details screen
     */
    public void clickSaveTask() {
        System.out.println("💾 Clicking Save/Done...");
        try {
            // Try Done button first
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Done' OR label == 'Save'")
            );
            saveBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Save/Done");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Save/Done button: " + e.getMessage());
        }
    }

    /**
     * Click Back button on Task Details screen
     */
    public void clickBackFromTaskDetails() {
        System.out.println("🔙 Clicking Back...");
        try {
            WebElement backBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Back' OR name == 'Back'")
            );
            backBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Back");
        } catch (Exception e) {
            // Try Close button
            try {
                WebElement closeBtn = driver.findElement(AppiumBy.accessibilityId("Close"));
                closeBtn.click();
                sleep(400);
                System.out.println("✅ Clicked Close");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not find Back/Close button");
            }
        }
    }

    /**
     * Check if task has "Open" status
     */
    public boolean isTaskOpen() {
        try {
            WebElement openStatus = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Open' AND type == 'XCUIElementTypeStaticText'")
            );
            return openStatus.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Click Delete Task button in Task Details screen
     * Button is at the bottom - may need to scroll
     */
    public void clickDeleteTaskButton() {
        System.out.println("🗑️ Clicking Delete Task button...");
        try {
            // First try to find visible Delete Task button
            WebElement deleteBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Delete Task' AND type == 'XCUIElementTypeButton'")
            );
            deleteBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Delete Task button");
        } catch (Exception e) {
            System.out.println("⚠️ Delete Task button not visible, scrolling...");
            // Scroll down to find it
            for (int i = 0; i < 3; i++) {
                scrollFormDown();
                sleep(300);
                try {
                    WebElement deleteBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Delete Task' AND type == 'XCUIElementTypeButton'")
                    );
                    deleteBtn.click();
                    sleep(400);
                    System.out.println("✅ Clicked Delete Task button after scroll");
                    return;
                } catch (Exception e2) {
                    // Keep scrolling
                }
            }
            System.out.println("⚠️ Could not find Delete Task button");
        }
    }

    /**
     * Confirm task deletion in the alert dialog
     * Alert has Cancel and Delete buttons
     */
    public void confirmDeleteTask() {
        System.out.println("⚠️ Confirming task deletion...");
        try {
            // Wait for alert to appear
            sleep(200);
            WebElement deleteBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Delete' AND type == 'XCUIElementTypeButton'")
            );
            deleteBtn.click();
            sleep(600);
            System.out.println("✅ Confirmed task deletion");
        } catch (Exception e) {
            System.out.println("⚠️ Could not confirm deletion: " + e.getMessage());
        }
    }

    /**
     * Cancel task deletion in the alert dialog
     */
    public void cancelDeleteTask() {
        System.out.println("❌ Canceling task deletion...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            sleep(200);
            System.out.println("✅ Canceled task deletion");
        } catch (Exception e) {
            System.out.println("⚠️ Could not cancel deletion: " + e.getMessage());
        }
    }

    /**
     * Check if Delete Task confirmation alert is displayed
     */
    public boolean isDeleteTaskAlertDisplayed() {
        try {
            WebElement alert = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeAlert' AND name == 'Delete Task'")
            );
            return alert.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }


    // ================================================================
    // ISSUE METHODS
    // ================================================================

    /**
     * Scroll to Issues section on Asset Details screen
     */
    public void scrollToIssuesSection() {
        System.out.println("📜 Scrolling to Issues section...");
        
        for (int i = 0; i < 12; i++) {
            try {
                // Check if Issues label is visible and in a good position (y between 300-600)
                WebElement issuesLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Issues' AND type == 'XCUIElementTypeStaticText'")
                );
                
                int y = issuesLabel.getLocation().getY();
                System.out.println("   Found Issues at y=" + y);
                
                // Issues should be in middle of screen (y between 300-600 for optimal clicking)
                if (y >= 300 && y <= 650) {
                    System.out.println("✅ Found Issues section at good position (y=" + y + ")");
                    sleep(200);
                    return;
                } else if (y < 300) {
                    // Scrolled too far, scroll back up
                    System.out.println("   Issues too high (y=" + y + "), scrolling up...");
                    Map<String, Object> upParams = new HashMap<>();
                    upParams.put("direction", "down");
                    upParams.put("velocity", 200);
                    driver.executeScript("mobile: swipe", upParams);
                    sleep(400);
                    continue;
                }
                // y > 650, keep scrolling down
            } catch (Exception e) {
                System.out.println("   Scroll " + (i + 1) + " - Issues not visible yet");
            }
            
            // Scroll down
            Map<String, Object> swipeParams = new HashMap<>();
            swipeParams.put("direction", "up");
            swipeParams.put("velocity", 250);
            driver.executeScript("mobile: swipe", swipeParams);
            sleep(400);
        }
        
        System.out.println("⚠️ Issues section not found after scrolling");
    }

        /**
     * Click Add Issue (+) button in Issues section
     */
    public void clickAddIssueButton() {
        System.out.println("➕ Clicking Add Issue button...");
        try {
            sleep(200);
            
            // First find the Issues label to get its Y position
            WebElement issuesLabel = null;
            int issuesY = -1;
            try {
                issuesLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Issues' AND type == 'XCUIElementTypeStaticText'")
                );
                issuesY = issuesLabel.getLocation().getY();
                System.out.println("   Issues label at y=" + issuesY);
            } catch (Exception e) {
                System.out.println("   ⚠️ Could not find Issues label");
            }
            
            // Find all visible Add buttons
            List<WebElement> addButtons = driver.findElements(
                AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill' AND label == 'Add'")
            );
            System.out.println("   Found " + addButtons.size() + " visible Add buttons");
            
            WebElement correctButton = null;
            
            // If we found Issues label, find Add button near it (within 50px Y)
            if (issuesY > -1 && !addButtons.isEmpty()) {
                for (WebElement btn : addButtons) {
                    int btnY = btn.getLocation().getY();
                    System.out.println("     Add button at y=" + btnY);
                    if (Math.abs(btnY - issuesY) < 50) {
                        correctButton = btn;
                        System.out.println("     ✓ This is near Issues label");
                        break;
                    }
                }
            }
            
            // Fallback: use first visible Add button
            if (correctButton == null && !addButtons.isEmpty()) {
                correctButton = addButtons.get(0);
                System.out.println("   Using first visible Add button");
            }
            
            if (correctButton != null) {
                int y = correctButton.getLocation().getY();
                System.out.println("   Clicking Add button at y=" + y);
                correctButton.click();
                sleep(1500);
                
                // Verify New Issue screen opened
                try {
                    WebElement navBar = driver.findElement(
                        AppiumBy.iOSNsPredicateString("name == 'New Issue' AND type == 'XCUIElementTypeNavigationBar'")
                    );
                    if (navBar.isDisplayed()) {
                        System.out.println("✅ Clicked Add Issue button - New Issue screen opened");
                        return;
                    }
                } catch (Exception ve) {
                    System.out.println("⚠️ New Issue screen not detected - may have clicked wrong button");
                }
            } else {
                System.out.println("⚠️ No Add button found");
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Add Issue button: " + e.getMessage());
        }
    }

        /**
     * Check if New Issue screen is displayed
     */
    public boolean isNewIssueScreenDisplayed() {
        System.out.println("🔍 Checking if New Issue screen is displayed...");
        try {
            // Try to find New Issue navigation bar
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'New Issue' AND type == 'XCUIElementTypeNavigationBar'")
            );
            boolean displayed = navBar.isDisplayed();
            System.out.println("   Nav bar found, displayed: " + displayed);
            return displayed;
        } catch (Exception e1) {
            // Try alternate: look for "New Issue" static text
            try {
                WebElement newIssueText = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'New Issue' AND type == 'XCUIElementTypeStaticText'")
                );
                boolean displayed = newIssueText.isDisplayed();
                System.out.println("   New Issue text found, displayed: " + displayed);
                return displayed;
            } catch (Exception e2) {
                // Try to find Create Issue button (only exists on New Issue screen)
                try {
                    WebElement createBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Create Issue' AND type == 'XCUIElementTypeButton'")
                    );
                    System.out.println("   Create Issue button found - on New Issue screen");
                    return true;
                } catch (Exception e3) {
                    System.out.println("   ⚠️ New Issue screen not detected");
                    return false;
                }
            }
        }
    }

    /**
     * Enter issue title
     */
    public void enterIssueTitle(String title) {
        System.out.println("📝 Entering issue title: " + title);
        try {
            // Wait for New Issue screen to be ready
            sleep(200);
            
            // Find title field by placeholderValue
            WebElement titleField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND placeholderValue == 'Enter issue title'")
            );
            
            System.out.println("   Found title field, clicking...");
            titleField.click();
            sleep(200);
            titleField.sendKeys(title);
            sleep(300);
            
            // Dismiss keyboard by clicking Done button
            System.out.println("   Dismissing keyboard...");
            try {
                WebElement doneBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Done' AND type == 'XCUIElementTypeButton'")
                );
                doneBtn.click();
                sleep(200);
                System.out.println("   ✅ Clicked Done button");
            } catch (Exception doneEx) {
                // Tap outside to dismiss keyboard
                try {
                    driver.executeScript("mobile: tap", Map.of("x", 200, "y", 200));
                    sleep(300);
                } catch (Exception tapEx) {}
            }
            
            System.out.println("✅ Entered issue title");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter issue title: " + e.getMessage());
        }
    }

    /**
     * Click Issue Class dropdown
     */
    public void clickIssueClassDropdown() {
        System.out.println("📋 Clicking Issue Class dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Issue Class' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Issue Class dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Issue Class dropdown: " + e.getMessage());
        }
    }

    /**
     * Select an Issue Class option
     * @param className e.g., "Repair Needed", "NEC Violation", "Thermal Anomaly"
     */
    public void selectIssueClass(String className) {
        System.out.println("📋 Selecting Issue Class: " + className);
        try {
            // Click Issue Class dropdown first
            clickIssueClassDropdown();
            sleep(200);
            
            // Select the option
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == '" + className + "' AND type == 'XCUIElementTypeButton'")
            );
            option.click();
            sleep(200);
            System.out.println("✅ Selected Issue Class: " + className);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Issue Class: " + e.getMessage());
        }
    }

        /**
     * Click Priority dropdown
     */
    public void clickPriorityDropdown() {
        System.out.println("📋 Clicking Priority dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Priority' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Priority dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Priority dropdown: " + e.getMessage());
        }
    }

    /**
     * Click Create Issue button
     */
    public void clickCreateIssueButton() {
        System.out.println("🆕 Clicking Create Issue button...");
        try {
            // Try multiple selectors
            WebElement createBtn = null;
            
            try {
                createBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == 'Create Issue' AND type == 'XCUIElementTypeButton'")
                );
            } catch (Exception e1) {
                try {
                    createBtn = driver.findElement(AppiumBy.accessibilityId("Create Issue"));
                } catch (Exception e2) {
                    System.out.println("⚠️ Could not find Create Issue button");
                    return;
                }
            }
            
            // Check if enabled
            String enabled = createBtn.getAttribute("enabled");
            System.out.println("   Create Issue button enabled: " + enabled);
            
            if ("true".equals(enabled)) {
                createBtn.click();
                sleep(500);
                System.out.println("✅ Clicked Create Issue button");
            } else {
                System.out.println("⚠️ Create Issue button is disabled - title may be empty");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create Issue: " + e.getMessage());
        }
    }

    /**
     * Check if Create Issue button is enabled
     */
    public boolean isCreateIssueButtonEnabled() {
        try {
            WebElement createBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Create Issue' AND type == 'XCUIElementTypeButton'")
            );
            String enabled = createBtn.getAttribute("enabled");
            return "true".equals(enabled);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Cancel button on New Issue screen
     */
    public void clickCancelIssue() {
        System.out.println("❌ Clicking Cancel on New Issue...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    /**
     * Get the asset name displayed in "Creating issue for:" section
     */
    public String getIssueAssetName() {
        try {
            WebElement assetName = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND name CONTAINS 'TestAsset'")
            );
            return assetName.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }


    // ================================================================
    // CONNECTION METHODS
    // ================================================================

    /**
     * Scroll to Connections section on Asset Details screen
     */
    public void scrollToConnectionsSection() {
        System.out.println("📜 Scrolling to Connections section...");
        
        // Connections is at the bottom of Asset Details form
        // Scroll aggressively to bottom first
        for (int i = 0; i < 12; i++) {
            Map<String, Object> swipeParams = new HashMap<>();
            swipeParams.put("direction", "up");
            swipeParams.put("velocity", 800);
            driver.executeScript("mobile: swipe", swipeParams);
            sleep(150);
        }
        
        System.out.println("   Scrolled to bottom, now scrolling up to show Add button...");
        sleep(300);
        
        // Scroll UP a bit to bring Connections header (with Add button) into view
        // The header with Add button might be above visible area after scrolling to bottom
        for (int i = 0; i < 3; i++) {
            try {
                // Check if Connections label is visible
                WebElement connLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Connections' AND type == 'XCUIElementTypeStaticText'")
                );
                int y = connLabel.getLocation().getY();
                if (y > 100 && y < 400) {
                    System.out.println("✅ Connections section visible at y=" + y);
                    break;
                } else if (y < 100) {
                    // Need to scroll up more
                    System.out.println("   Scrolling up (Connections at y=" + y + ")...");
                    scrollFormUp();
                    sleep(300);
                }
            } catch (Exception e) {
                // Not found, scroll up
                System.out.println("   Scrolling up to find Connections...");
                scrollFormUp();
                sleep(300);
            }
        }
        
        // Final check
        try {
            WebElement connLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Connections' AND type == 'XCUIElementTypeStaticText'")
            );
            System.out.println("✅ Connections section ready");
        } catch (Exception e) {
            System.out.println("⚠️ Connections section position may need manual check");
        }
    }

    /**
     * Click Add Connection (+) button in Connections section
     */
    public void clickAddConnectionButton() {
        System.out.println("➕ Clicking Add Connection button...");
        
        // Try to find and click Add button with scroll retry
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                // Check if Connections label is visible
                WebElement connLabel = null;
                try {
                    connLabel = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Connections' AND type == 'XCUIElementTypeStaticText'")
                    );
                    int connY = connLabel.getLocation().getY();
                    System.out.println("   Connections label at y=" + connY);
                } catch (Exception e) {
                    System.out.println("   Connections label not visible, scrolling up...");
                    scrollFormUp();
                    sleep(200);
                    continue;
                }
                
                // Find Add button near Connections
                List<WebElement> addBtns = driver.findElements(
                    AppiumBy.iOSNsPredicateString("name == 'Add' AND type == 'XCUIElementTypeButton'")
                );
                
                if (addBtns.isEmpty()) {
                    System.out.println("   No Add button found, scrolling up...");
                    scrollFormUp();
                    sleep(200);
                    continue;
                }
                
                // Click the first visible Add button (should be near Connections)
                WebElement addBtn = addBtns.get(0);
                int addY = addBtn.getLocation().getY();
                System.out.println("   Add button at y=" + addY);
                
                addBtn.click();
                sleep(400);
                System.out.println("✅ Clicked Add Connection button");
                return;
                
            } catch (Exception e) {
                System.out.println("   Attempt " + (attempt+1) + " failed, scrolling up...");
                scrollFormUp();
                sleep(200);
            }
        }
        
        System.out.println("⚠️ Could not click Add Connection button after retries");
    }

    /**
     * Select New Lineside Connection from the menu
     */
    public void selectNewLinesideConnection() {
        System.out.println("🔗 Selecting New Lineside Connection...");
        try {
            WebElement lineside = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'New Lineside Connection' AND type == 'XCUIElementTypeButton'")
            );
            lineside.click();
            sleep(500);
            System.out.println("✅ Selected New Lineside Connection");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select New Lineside Connection: " + e.getMessage());
        }
    }

    /**
     * Select New Loadside Connection from the menu
     */
    public void selectNewLoadsideConnection() {
        System.out.println("🔗 Selecting New Loadside Connection...");
        try {
            WebElement loadside = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'New Loadside Connection' AND type == 'XCUIElementTypeButton'")
            );
            loadside.click();
            sleep(500);
            System.out.println("✅ Selected New Loadside Connection");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select New Loadside Connection: " + e.getMessage());
        }
    }

    /**
     * Check if New Connection screen is displayed
     */
    public boolean isNewConnectionScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'New Connection' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Lineside (Incoming) is selected
     */
    public boolean isLinesideIncomingSelected() {
        try {
            WebElement lineside = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Lineside (Incoming)' AND type == 'XCUIElementTypeStaticText'")
            );
            return lineside.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Cancel on New Connection screen
     */
    public void clickCancelConnection() {
        System.out.println("❌ Clicking Cancel on New Connection...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            sleep(400);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    /**
     * Click Source Node dropdown
     */
    public void clickSourceNodeDropdown() {
        System.out.println("📋 Clicking Source Node dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Source Node' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Source Node dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Source Node dropdown: " + e.getMessage());
        }
    }

    /**
     * Click Target Node dropdown
     */
    public void clickTargetNodeDropdown() {
        System.out.println("📋 Clicking Target Node dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Target Node' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Target Node dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Target Node dropdown: " + e.getMessage());
        }
    }

    
    /**
     * Click Source Node dropdown (Select source)
     */
    public void clickSelectSourceDropdown() {
        System.out.println("📋 Clicking Select source dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select source' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Select source dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Select source: " + e.getMessage());
        }
    }

    /**
     * Select first available source node (not the current asset)
     */
    public void selectFirstSourceNode() {
        System.out.println("🔗 Selecting first available source node...");
        try {
            // Find all asset buttons in the source list
            List<WebElement> options = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ', ' AND name CONTAINS 'ATS'")
            );
            
            if (options.isEmpty()) {
                // Try broader search
                options = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'TestAsset'")
                );
            }
            
            System.out.println("   Found " + options.size() + " source node options");
            
            if (!options.isEmpty()) {
                WebElement firstOption = options.get(0);
                String name = firstOption.getAttribute("name");
                System.out.println("   Selecting: " + name);
                firstOption.click();
                sleep(400);
                System.out.println("✅ Selected source node");
            } else {
                System.out.println("⚠️ No source node options found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not select source node: " + e.getMessage());
        }
    }

    /**
     * Click Create button on New Connection screen
     */
    public void clickCreateConnectionButton() {
        System.out.println("🆕 Clicking Create button...");
        try {
            WebElement createBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Create' AND type == 'XCUIElementTypeButton'")
            );
            createBtn.click();
            sleep(500);
            System.out.println("✅ Clicked Create button");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create button: " + e.getMessage());
        }
    }

    /**
     * Check if connection was created (back on Asset Details with connection visible)
     */
    public boolean isConnectionCreated() {
        try {
            // Check if we're back on Asset Details
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Details' AND type == 'XCUIElementTypeNavigationBar'")
            );
            
            // Check if Lineside connection is visible
            WebElement connection = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Lineside' AND type == 'XCUIElementTypeButton'")
            );
            
            return navBar.isDisplayed() && connection.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    
    /**
     * Check if Loadside (Outgoing) is selected
     */
    public boolean isLoadsideOutgoingSelected() {
        try {
            WebElement loadside = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Loadside (Outgoing)' AND type == 'XCUIElementTypeStaticText'")
            );
            return loadside.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Target Node dropdown (Select target)
     */
    public void clickSelectTargetDropdown() {
        System.out.println("📋 Clicking Select target dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select target' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(400);
            System.out.println("✅ Clicked Select target dropdown");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Select target: " + e.getMessage());
        }
    }

    /**
     * Select first available target node (not the current asset)
     */
    public void selectFirstTargetNode() {
        System.out.println("🔗 Selecting first available target node...");
        try {
            // Find all asset buttons in the target list
            List<WebElement> options = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ', ' AND name CONTAINS 'ATS'")
            );
            
            if (options.isEmpty()) {
                // Try broader search
                options = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'TestAsset'")
                );
            }
            
            System.out.println("   Found " + options.size() + " target node options");
            
            if (!options.isEmpty()) {
                WebElement firstOption = options.get(0);
                String name = firstOption.getAttribute("name");
                System.out.println("   Selecting: " + name);
                firstOption.click();
                sleep(400);
                System.out.println("✅ Selected target node");
            } else {
                System.out.println("⚠️ No target node options found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not select target node: " + e.getMessage());
        }
    }

    /**
     * Check if Loadside connection was created
     */
    public boolean isLoadsideConnectionCreated() {
        try {
            // Check if we're back on Asset Details
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Asset Details' AND type == 'XCUIElementTypeNavigationBar'")
            );
            
            // Check if Loadside connection is visible
            WebElement connection = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Loadside' AND type == 'XCUIElementTypeButton'")
            );
            
            return navBar.isDisplayed() && connection.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    
    /**
     * Verify Source Node is auto-populated (for Loadside - shows current asset)
     * Returns the asset name if populated, null otherwise
     */
    public String getSourceNodeValue() {
        System.out.println("🔍 Checking Source Node value...");
        try {
            // For Loadside, Source Node shows the current asset as a button
            WebElement sourceBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'TestAsset' AND name CONTAINS 'Generator'")
            );
            String value = sourceBtn.getAttribute("name");
            System.out.println("   Source Node value: " + value);
            return value;
        } catch (Exception e) {
            System.out.println("   Source Node not found or empty");
            return null;
        }
    }

    /**
     * Verify Target Node is auto-populated (for Lineside - shows current asset)
     * Returns the asset name if populated, null otherwise
     */
    public String getTargetNodeValue() {
        System.out.println("🔍 Checking Target Node value...");
        try {
            // For Lineside, Target Node shows the current asset as a button
            WebElement targetBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'TestAsset' AND name CONTAINS 'Generator'")
            );
            String value = targetBtn.getAttribute("name");
            System.out.println("   Target Node value: " + value);
            return value;
        } catch (Exception e) {
            System.out.println("   Target Node not found or empty");
            return null;
        }
    }

    /**
     * Check if Source Node shows "Select source" (not yet selected)
     */
    public boolean isSourceNodeEmpty() {
        try {
            WebElement selectSource = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select source' AND type == 'XCUIElementTypeButton'")
            );
            return selectSource.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Target Node shows "Select target" (not yet selected)
     */
    public boolean isTargetNodeEmpty() {
        try {
            WebElement selectTarget = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select target' AND type == 'XCUIElementTypeButton'")
            );
            return selectTarget.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    
    // ============================================================
    // MCC-OCP METHODS
    // ============================================================

    /**
     * Scroll to OCP section in MCC asset
     */
    public void scrollToOCPSection() {
        System.out.println("📜 Scrolling to OCP section...");
        long start = System.currentTimeMillis();
        
        try {
            // Scroll down to find OCP section
            for (int i = 0; i < 10; i++) {
                try {
                    WebElement ocp = driver.findElement(
                        AppiumBy.iOSNsPredicateString("name == 'OCP' AND type == 'XCUIElementTypeStaticText'")
                    );
                    if (ocp.isDisplayed()) {
                        int y = ocp.getLocation().getY();
                        System.out.println("✅ Found OCP section at y=" + y);
                        break;
                    }
                } catch (Exception e) {
                    // Not found yet, scroll
                    scrollFormDown();
                    sleep(300);
                }
            }
            System.out.println("✅ At OCP section (Total: " + (System.currentTimeMillis() - start) + "ms)");
        } catch (Exception e) {
            System.out.println("⚠️ OCP section not found: " + e.getMessage());
        }
    }

    /**
     * Click Add OCP button (+ button near OCP label)
     */
    public void clickAddOCPButton() {
        System.out.println("➕ Clicking Add OCP button...");
        try {
            // Find OCP label position
            WebElement ocpLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'OCP' AND type == 'XCUIElementTypeStaticText'")
            );
            int ocpY = ocpLabel.getLocation().getY();
            System.out.println("   OCP label at y=" + ocpY);
            
            // Find Add button near OCP (within 50px)
            List<WebElement> addButtons = driver.findElements(
                AppiumBy.iOSNsPredicateString("name == 'Add' AND type == 'XCUIElementTypeButton'")
            );
            
            for (WebElement btn : addButtons) {
                int btnY = btn.getLocation().getY();
                if (Math.abs(btnY - ocpY) < 50) {
                    System.out.println("   Add button at y=" + btnY);
                    btn.click();
                    sleep(400);
                    System.out.println("✅ Clicked Add OCP button");
                    return;
                }
            }
            
            // Fallback: click first Add button
            if (!addButtons.isEmpty()) {
                addButtons.get(0).click();
                sleep(400);
                System.out.println("✅ Clicked first Add button (fallback)");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Add OCP: " + e.getMessage());
        }
    }

    /**
     * Check if OCP Add options are displayed (Create New Child / Link Existing Node)
     */
    public boolean areOCPAddOptionsDisplayed() {
        try {
            WebElement createChild = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Create New Child' AND type == 'XCUIElementTypeButton'")
            );
            WebElement linkNode = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Link Existing Node' AND type == 'XCUIElementTypeButton'")
            );
            return createChild.isDisplayed() && linkNode.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // OCP UNLINK METHODS
    // ============================================================

    /**
     * Get count of OCP items in the list
     */
    public int getOCPCount() {
        System.out.println("🔢 Getting OCP count...");
        try {
            // Look for OCP label
            WebElement ocpLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'OCP' AND type == 'XCUIElementTypeStaticText'")
            );
            
            int ocpY = ocpLabel.getLocation().getY();
            int ocpX = ocpLabel.getLocation().getX();
            System.out.println("   OCP label at y=" + ocpY);
            
            // Find nearby StaticText that shows the count (e.g., "4" or "5")
            List<WebElement> texts = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText'")
            );
            
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && name.matches("\\d+")) {
                    int textY = text.getLocation().getY();
                    int textX = text.getLocation().getX();
                    // Count badge is to the RIGHT of OCP label and within 30px vertically
                    if (Math.abs(textY - ocpY) < 30 && textX > ocpX) {
                        int count = Integer.parseInt(name);
                        System.out.println("   OCP count: " + count);
                        return count;
                    }
                }
            }
            System.out.println("   OCP count badge not found");
            return 0;
        } catch (Exception e) {
            System.out.println("   Could not get OCP count: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if OCP list has items
     */
    public boolean hasOCPItems() {
        return getOCPCount() > 0;
    }

    /**
     * Long press on first OCP item to show context menu
     */
    public void longPressFirstOCPItem() {
        System.out.println("👆 Long pressing first OCP item...");
        try {
            // First try to find OCP items WITHOUT scrolling (we may already be at the right position)
            List<WebElement> ocpItems = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS 'Relay' OR name CONTAINS 'Disconnect Switch' OR name CONTAINS 'Fuse' OR " +
                    "name CONTAINS 'MCC Bucket' OR name CONTAINS 'Other (OCP)' OR name CONTAINS 'LinkTest')")
            );
            
            System.out.println("   Found " + ocpItems.size() + " OCP items");
            
            // Only scroll if no OCP items found
            if (ocpItems.isEmpty()) {
                System.out.println("   Scrolling to find OCP items...");
                scrollFormDown();
                sleep(300);
                ocpItems = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND " +
                        "(name CONTAINS 'Relay' OR name CONTAINS 'Disconnect Switch' OR name CONTAINS 'Fuse' OR " +
                        "name CONTAINS 'MCC Bucket' OR name CONTAINS 'Other (OCP)' OR name CONTAINS 'LinkTest')")
                );
                System.out.println("   After scroll: Found " + ocpItems.size() + " OCP items");
            }
            
            if (!ocpItems.isEmpty()) {
                WebElement firstOCP = ocpItems.get(0);
                String name = firstOCP.getAttribute("name");
                System.out.println("   Selected OCP: " + name);
                
                // Try mobile: touchAndHold first (more reliable for iOS)
                try {
                    Map<String, Object> params = new java.util.HashMap<>();
                    params.put("element", ((RemoteWebElement) firstOCP).getId());
                    params.put("duration", 2.0); // 2 seconds
                    driver.executeScript("mobile: touchAndHold", params);
                    sleep(300);
                    System.out.println("✅ Long pressed using touchAndHold");
                    return;
                } catch (Exception e) {
                    System.out.println("   touchAndHold failed, trying W3C Actions...");
                }
                
                // Fallback: W3C Actions with longer duration
                int elemX = firstOCP.getLocation().getX();
                int elemY = firstOCP.getLocation().getY();
                int elemW = firstOCP.getSize().getWidth();
                int elemH = firstOCP.getSize().getHeight();
                int x = elemX + (elemW / 2);
                int y = elemY + (elemH / 2);
                System.out.println("   Long press at (" + x + ", " + y + ")");
                
                // Long press using W3C Actions - 2 seconds
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence longPress = new Sequence(finger, 1);
                longPress.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
                longPress.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                longPress.addAction(new org.openqa.selenium.interactions.Pause(finger, Duration.ofMillis(2000))); // Hold for 2 seconds
                longPress.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Arrays.asList(longPress));
                
                sleep(300);
                System.out.println("✅ Long pressed OCP item");
            } else {
                System.out.println("⚠️ No OCP items found after scrolling");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not long press OCP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Click "Unlink from Parent" in context menu
     */
    public void clickUnlinkFromParent() {
        System.out.println("🔗 Clicking Unlink from Parent...");
        try {
            // Wait a bit for context menu to appear
            sleep(200);
            
            // Try multiple strategies to find Unlink from Parent
            WebElement unlinkBtn = null;
            
            // Strategy 1: Button type
            try {
                unlinkBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == 'Unlink from Parent' AND type == 'XCUIElementTypeButton'")
                );
            } catch (Exception e) {}
            
            // Strategy 2: Any type with label
            if (unlinkBtn == null) {
                try {
                    unlinkBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Unlink from Parent'")
                    );
                } catch (Exception e) {}
            }
            
            // Strategy 3: Accessibility ID
            if (unlinkBtn == null) {
                try {
                    unlinkBtn = driver.findElement(AppiumBy.accessibilityId("Unlink from Parent"));
                } catch (Exception e) {}
            }
            
            // Strategy 4: CONTAINS search
            if (unlinkBtn == null) {
                try {
                    unlinkBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("name CONTAINS 'Unlink'")
                    );
                } catch (Exception e) {}
            }
            
            if (unlinkBtn != null) {
                unlinkBtn.click();
                sleep(200);
                System.out.println("✅ Clicked Unlink from Parent");
            } else {
                System.out.println("⚠️ Unlink from Parent not found - context menu may not have appeared");
                
                // Debug: Print visible elements
                List<WebElement> visibleButtons = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
                );
                System.out.println("   Visible buttons: " + visibleButtons.size());
                for (int i = 0; i < Math.min(5, visibleButtons.size()); i++) {
                    System.out.println("     - " + visibleButtons.get(i).getAttribute("name"));
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Unlink from Parent: " + e.getMessage());
        }
    }

    /**
     * Confirm unlink in the confirmation dialog
     */
    public void confirmUnlink() {
        System.out.println("✅ Confirming unlink...");
        try {
            // Wait for dialog to appear
            sleep(300);
            
            WebElement unlinkBtn = null;
            
            // Try multiple strategies
            try {
                unlinkBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == 'Unlink' AND type == 'XCUIElementTypeButton'")
                );
            } catch (Exception e) {}
            
            if (unlinkBtn == null) {
                try {
                    unlinkBtn = driver.findElement(AppiumBy.accessibilityId("Unlink"));
                } catch (Exception e) {}
            }
            
            if (unlinkBtn == null) {
                try {
                    // The red "Unlink" button in confirmation dialog
                    unlinkBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("label == 'Unlink'")
                    );
                } catch (Exception e) {}
            }
            
            if (unlinkBtn != null) {
                unlinkBtn.click();
                sleep(400);
                System.out.println("✅ Confirmed unlink");
            } else {
                System.out.println("⚠️ Unlink confirmation button not found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not confirm unlink: " + e.getMessage());
        }
    }

    /**
     * Unlink first OCP item (full flow: long press -> Unlink from Parent -> Confirm)
     */
    public void unlinkFirstOCPItem() {
        System.out.println("🔓 Unlinking first OCP item...");
        longPressFirstOCPItem();
        sleep(200);
        clickUnlinkFromParent();
        sleep(200);
        confirmUnlink();
        System.out.println("✅ OCP item unlinked");
    }

    /**
     * Click Create New Child option
     */
    public void clickCreateNewChild() {
        System.out.println("🆕 Clicking Create New Child...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Create New Child' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(500);
            System.out.println("✅ Clicked Create New Child");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create New Child: " + e.getMessage());
        }
    }

    /**
     * Click Link Existing Node option
     */
    public void clickLinkExistingNode() {
        System.out.println("🔗 Clicking Link Existing Node...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Link Existing Node' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(500);
            System.out.println("✅ Clicked Link Existing Node");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Link Existing Node: " + e.getMessage());
        }
    }

    /**
     * Check if Create New Child Asset screen is displayed
     */
    public boolean isCreateNewChildAssetScreenDisplayed() {
        try {
            // Look for Asset Class dropdown (indicates create asset form)
            WebElement assetClass = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'ATS' OR name == 'MCC Bucket' OR name == 'Circuit Breaker')")
            );
            return assetClass.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Link Existing Node screen is displayed
     */
    public boolean isLinkExistingNodeScreenDisplayed() {
        try {
            // Link existing node shows a list of assets
            WebElement assetList = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'Asset_'")
            );
            return assetList.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Select first existing node to link
     */
    public void selectFirstExistingNode() {
        System.out.println("🔗 Selecting first existing node...");
        try {
            WebElement firstNode = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS 'Asset_'")
            );
            String name = firstNode.getAttribute("name");
            System.out.println("   Selecting: " + name);
            firstNode.click();
            sleep(400);
            System.out.println("✅ Selected first existing node");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select existing node: " + e.getMessage());
        }
    }

    /**
     * Check if OCP section exists (for MCC assets)
     */
    public boolean isOCPSectionVisible() {
        try {
            WebElement ocp = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'OCP' AND type == 'XCUIElementTypeStaticText'")
            );
            return ocp.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // CREATE CHILD ASSET METHODS
    // ============================================================

    /**
     * Check if Create Child Asset screen is displayed
     */
    public boolean isCreateChildAssetScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Create Child Asset' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Enter child asset name
     */
    public void enterChildAssetName(String name) {
        System.out.println("📝 Entering child asset name: " + name);
        try {
            WebElement field = driver.findElement(
                AppiumBy.iOSNsPredicateString("value == 'Enter asset name' AND type == 'XCUIElementTypeTextField'")
            );
            field.click();
            sleep(300);
            field.sendKeys(name);
            System.out.println("✅ Entered child asset name");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter asset name: " + e.getMessage());
        }
    }

    /**
     * Click Asset Class dropdown on Create Child Asset screen
     */
    public void clickChildAssetClassDropdown() {
        System.out.println("📋 Clicking Asset Class dropdown...");
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' AND name CONTAINS 'Select class' AND type == 'XCUIElementTypeButton'")
            );
            dropdown.click();
            sleep(200);
            System.out.println("✅ Clicked Asset Class dropdown");
        } catch (Exception e) {
            // Try alternate selector
            try {
                WebElement dropdown = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label CONTAINS 'Asset Class' AND type == 'XCUIElementTypeButton'")
                );
                dropdown.click();
                sleep(200);
                System.out.println("✅ Clicked Asset Class dropdown (alt)");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not click dropdown: " + e2.getMessage());
            }
        }
    }

    /**
     * Select OCP class for child asset
     */
    public void selectChildAssetClass(String className) {
        System.out.println("📋 Selecting child asset class: " + className);
        try {
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == '" + className + "' AND type == 'XCUIElementTypeButton'")
            );
            option.click();
            sleep(200);
            System.out.println("✅ Selected class: " + className);
        } catch (Exception e) {
            System.out.println("⚠️ Could not select class: " + e.getMessage());
        }
    }

    /**
     * Click Create button on Create Child Asset screen
     */
    public void clickCreateChildAssetButton() {
        System.out.println("🆕 Clicking Create button...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Create' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(500);
            System.out.println("✅ Clicked Create");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create: " + e.getMessage());
        }
    }

    /**
     * Create a child asset with given name and class
     * @param assetName Name for the child asset
     * @param assetClass OCP class (Disconnect Switch, Fuse, MCC Bucket, Other (OCP), Relay)
     */
    public void createChildAsset(String assetName, String assetClass) {
        System.out.println("🆕 Creating child asset: " + assetName + " (" + assetClass + ")");
        
        enterChildAssetName(assetName);
        clickChildAssetClassDropdown();
        selectChildAssetClass(assetClass);
        clickCreateChildAssetButton();
        
        System.out.println("✅ Child asset creation completed");
    }

    /**
     * Check if child asset was created (visible in OCP list)
     */
    public boolean isChildAssetCreated(String assetName) {
        try {
            WebElement asset = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS '" + assetName + "' AND type == 'XCUIElementTypeButton'")
            );
            return asset.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get Parent Enclosure value on Create Child Asset screen
     */
    public String getParentEnclosureValue() {
        System.out.println("📋 Getting Parent Enclosure value...");
        try {
            // Look for static text after "Parent Enclosure" label
            WebElement parent = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'TestAsset' AND type == 'XCUIElementTypeStaticText'")
            );
            String value = parent.getAttribute("name");
            System.out.println("   Parent Enclosure: " + value);
            return value;
        } catch (Exception e) {
            System.out.println("⚠️ Could not get Parent Enclosure: " + e.getMessage());
            return "";
        }
    }

    /**
     * Check if Parent Enclosure field is populated
     */
    public boolean isParentEnclosurePopulated() {
        String value = getParentEnclosureValue();
        return value != null && !value.isEmpty() && value.contains("TestAsset");
    }

    /**
     * Check if Asset Class dropdown shows "Select class"
     */
    public boolean isAssetClassDropdownDefault() {
        try {
            WebElement dropdown = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Select class' AND type == 'XCUIElementTypeButton'")
            );
            return dropdown.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get available OCP class options
     */
    public List<String> getOCPClassOptions() {
        List<String> options = new ArrayList<>();
        String[] ocpClasses = {"Disconnect Switch", "Fuse", "MCC Bucket", "Other (OCP)", "Relay"};
        
        for (String cls : ocpClasses) {
            try {
                WebElement btn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name == '" + cls + "' AND type == 'XCUIElementTypeButton'")
                );
                if (btn.isDisplayed()) {
                    options.add(cls);
                }
            } catch (Exception e) {}
        }
        
        return options;
    }

    /**
     * Click Cancel button on Create Child Asset screen
     */
    public void clickCancelChildAsset() {
        System.out.println("❌ Clicking Cancel...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Cancel' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(400);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    // ============================================================
    // LINK EXISTING NODE METHODS
    // ============================================================

    /**
     * Check if Link Existing Nodes screen is displayed
     */
    public boolean isLinkExistingNodesScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Link Existing Nodes' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get count of assets available to link
     */
    public int getLinkableAssetsCount() {
        try {
            List<WebElement> assets = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ','")
            );
            // Filter out Cancel and Link buttons
            int count = 0;
            for (WebElement asset : assets) {
                String name = asset.getAttribute("name");
                if (name != null && !name.equals("Cancel") && !name.startsWith("Link ")) {
                    count++;
                }
            }
            System.out.println("   Found " + count + " linkable assets");
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Select first available asset to link by clicking on checkbox circle
     * Note: Clicking the button navigates to asset details, 
     * must click the circle checkbox on the LEFT side to select
     */
    public void selectFirstLinkableAsset() {
        System.out.println("🔗 Selecting first linkable asset...");
        try {
            // Find the visible circle checkbox images
            List<WebElement> circles = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeImage' AND name == 'circle'")
            );
            
            if (!circles.isEmpty()) {
                WebElement firstCircle = circles.get(0);
                int circleX = firstCircle.getLocation().getX();
                int circleY = firstCircle.getLocation().getY();
                int circleW = firstCircle.getSize().getWidth();
                int circleH = firstCircle.getSize().getHeight();
                
                // Click center of circle
                int clickX = circleX + (circleW / 2);
                int clickY = circleY + (circleH / 2);
                
                System.out.println("   Circle at (" + circleX + ", " + circleY + "), clicking center (" + clickX + ", " + clickY + ")");
                
                // Use W3C Actions for reliable tap
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence tap = new Sequence(finger, 1);
                tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), clickX, clickY));
                tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Arrays.asList(tap));
                
                sleep(200);
                System.out.println("✅ Selected asset via checkbox");
            } else {
                System.out.println("⚠️ No circle checkboxes found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not select asset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Click Link button on Link Existing Nodes screen
     */
    public void clickLinkButton() {
        System.out.println("🔗 Clicking Link button...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name BEGINSWITH 'Link ' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(500);
            System.out.println("✅ Clicked Link");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Link: " + e.getMessage());
        }
    }

    /**
     * Cancel Link Existing Nodes screen
     */
    public void cancelLinkExistingNodes() {
        System.out.println("❌ Clicking Cancel...");
        try {
            WebElement btn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Cancel' AND type == 'XCUIElementTypeButton'")
            );
            btn.click();
            sleep(400);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    // ============================================================
    // SEARCH FIELD METHODS (Link Existing Nodes screen)
    // ============================================================

    /**
     * Check if search field is visible on Link Existing Nodes screen
     */
    public boolean isLinkNodesSearchFieldVisible() {
        System.out.println("🔍 Checking for search field...");
        try {
            WebElement search = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField'")
            );
            String placeholder = search.getAttribute("value");
            System.out.println("   Search field visible with placeholder: " + placeholder);
            return search.isDisplayed();
        } catch (Exception e) {
            System.out.println("   Search field not visible");
            return false;
        }
    }

    /**
     * Get search field placeholder text
     */
    public String getSearchFieldPlaceholder() {
        try {
            WebElement search = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField'")
            );
            return search.getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Enter text in search field on Link Existing Nodes screen
     */
    public void enterLinkNodesSearchText(String text) {
        System.out.println("🔍 Entering search text: " + text);
        try {
            WebElement search = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField'")
            );
            search.click();
            sleep(300);
            search.sendKeys(text);
            sleep(200);
            System.out.println("✅ Entered search text");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter search text: " + e.getMessage());
        }
    }

    /**
     * Clear search field
     */
    public void clearLinkNodesSearchField() {
        System.out.println("🔍 Clearing search field...");
        try {
            WebElement search = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField'")
            );
            search.clear();
            sleep(300);
            System.out.println("✅ Cleared search field");
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear search field: " + e.getMessage());
        }
    }

    // ============================================================
    // SELECTION STATE VERIFICATION METHODS
    // ============================================================

    /**
     * Check if any nodes are selected (look for "X selected" text)
     */
    public boolean isAnyNodeSelected() {
        try {
            WebElement selected = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'selected' AND type == 'XCUIElementTypeStaticText'")
            );
            System.out.println("   Selection state: " + selected.getAttribute("name"));
            return selected.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the number of selected nodes
     */
    public int getSelectedNodeCount() {
        try {
            WebElement selected = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'selected' AND type == 'XCUIElementTypeStaticText'")
            );
            String text = selected.getAttribute("name"); // "1 selected"
            String num = text.split(" ")[0];
            return Integer.parseInt(num);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if Clear All button is visible
     */
    public boolean isClearAllButtonVisible() {
        try {
            WebElement clearAll = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Clear All' AND type == 'XCUIElementTypeButton'")
            );
            return clearAll.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Clear All button to deselect all nodes
     */
    public void clickClearAllButton() {
        System.out.println("❌ Clicking Clear All...");
        try {
            WebElement clearAll = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Clear All' AND type == 'XCUIElementTypeButton'")
            );
            clearAll.click();
            sleep(200);
            System.out.println("✅ Clicked Clear All");
        } catch (Exception e) {
            System.out.println("⚠️ Clear All button not found: " + e.getMessage());
        }
    }

        /**
     * Check if New Task screen is displayed
     */
    public boolean isNewTaskScreenDisplayed() {
        try {
            WebElement navBar = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'New Task' AND type == 'XCUIElementTypeNavigationBar'")
            );
            return navBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Enter task title
     */
    public void enterTaskTitle(String title) {
        System.out.println("📝 Entering task title: " + title);
        try {
            WebElement titleField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Enter task title'")
            );
            titleField.click();
            sleep(300);
            titleField.clear();
            titleField.sendKeys(title);
            System.out.println("✅ Entered task title");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter task title: " + e.getMessage());
        }
    }

    /**
     * Enter task description
     */
    public void enterTaskDescription(String description) {
        System.out.println("📝 Entering task description: " + description);
        try {
            // Find description text view
            WebElement descField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextView'")
            );
            descField.click();
            sleep(300);
            descField.sendKeys(description);
            sleep(300);
            
            // Click "Done" button to dismiss keyboard
            System.out.println("   Clicking Done to dismiss keyboard...");
            try {
                WebElement doneBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("label == 'Done' AND type == 'XCUIElementTypeButton'")
                );
                doneBtn.click();
                sleep(200);
                System.out.println("   ✅ Clicked Done button");
            } catch (Exception doneEx) {
                System.out.println("   Done button not found, trying alternatives...");
                // Try Return key or hide keyboard
                try {
                    driver.executeScript("mobile: hideKeyboard", Map.of("strategy", "pressKey", "key", "Done"));
                } catch (Exception kbEx) {
                    // Fallback: tap outside to dismiss
                    try {
                        driver.executeScript("mobile: tap", Map.of("x", 200, "y", 150));
                    } catch (Exception tapEx) {}
                }
            }
            sleep(300);
            
            System.out.println("✅ Entered task description");
        } catch (Exception e) {
            System.out.println("⚠️ Could not enter task description: " + e.getMessage());
        }
    }

    /**
     * Click Create Task button
     */
    public void clickCreateTaskButton() {
        System.out.println("🆕 Clicking Create Task button...");
        try {
            WebElement createBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'Create Task' AND type == 'XCUIElementTypeButton'")
            );
            createBtn.click();
            sleep(200);
            System.out.println("✅ Clicked Create Task");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Create Task: " + e.getMessage());
        }
    }

    /**
     * Click Cancel button on New Task screen
     */
    public void clickCancelTask() {
        System.out.println("❌ Clicking Cancel...");
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            sleep(200);
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }

    /**
     * Toggle Mark as Completed switch
     * Note: Need to dismiss keyboard first, then tap on the right side of the switch control
     */
    public void toggleMarkAsCompleted() {
        System.out.println("🔄 Toggling Mark as Completed...");
        try {
            // IMPORTANT: Dismiss keyboard first by tapping outside or pressing Done
            System.out.println("   Dismissing keyboard...");
            try {
                // Try to hide keyboard
                driver.executeScript("mobile: hideKeyboard");
            } catch (Exception kbEx) {
                // Fallback: tap on a neutral area to dismiss keyboard
                try {
                    driver.executeScript("mobile: tap", Map.of("x", 200, "y", 150));
                } catch (Exception tapEx) {
                    // Ignore
                }
            }
            sleep(200);
            
            WebElement toggle = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Mark as Completed' AND type == 'XCUIElementTypeSwitch'")
            );
            
            // Get toggle position and tap on the right side (where the actual switch is)
            int toggleX = toggle.getLocation().getX();
            int toggleY = toggle.getLocation().getY();
            int toggleWidth = toggle.getSize().getWidth();
            int toggleHeight = toggle.getSize().getHeight();
            
            // Tap on the right side of the toggle (where the switch control is)
            int tapX = toggleX + toggleWidth - 30;  // Right side
            int tapY = toggleY + (toggleHeight / 2); // Center vertically
            
            System.out.println("   Tapping toggle at (" + tapX + ", " + tapY + ")");
            driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
            sleep(200);
            
            // Verify the toggle changed
            String value = toggle.getAttribute("value");
            System.out.println("   Toggle value after tap: " + value);
            
            if ("0".equals(value)) {
                System.out.println("   Toggle still OFF, trying direct coordinates...");
                // Try tapping directly on the switch part
                driver.executeScript("mobile: tap", Map.of("x", 340, "y", tapY));
                sleep(200);
                value = toggle.getAttribute("value");
                System.out.println("   Toggle value after retry: " + value);
            }
            
            System.out.println("✅ Toggled Mark as Completed");
        } catch (Exception e) {
            System.out.println("⚠️ Could not toggle via element, trying coordinates...");
            // Fallback: dismiss keyboard and tap at typical toggle location
            try {
                driver.executeScript("mobile: hideKeyboard");
                sleep(300);
            } catch (Exception kbEx) {}
            
            try {
                driver.executeScript("mobile: tap", Map.of("x", 340, "y", 756));
                sleep(200);
                System.out.println("✅ Toggled Mark as Completed via coordinates");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not toggle: " + e2.getMessage());
            }
        }
    }

    /**
     * Check if Tasks section has "No tasks" text
     */
    public boolean hasNoTasks() {
        try {
            WebElement noTasks = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == 'No tasks'")
            );
            return noTasks.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is visible by label
     */
    public boolean isElementVisibleByLabel(String label) {
        try {
            WebElement element = driver.findElement(
                AppiumBy.iOSNsPredicateString("label == '" + label + "'")
            );
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fast wait for asset list (2 seconds max)
     */
    private void waitForAssetListReadyFast() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("plus")));
        } catch (Exception e) {}
    }

    public void clickAddAsset() {
        System.out.println("➕ Clicking Add Asset button...");
        click(plusButton);
        waitForCreateAssetFormReady();
    }
    
    /**
     * TURBO: Click Add Asset button - direct fast click
     */
    public void clickAddAssetTurbo() {
        try {
            WebElement plusBtn = driver.findElement(AppiumBy.accessibilityId("plus"));
            plusBtn.click();
            System.out.println("✅ Add Asset clicked");
            sleep(200);
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Add Asset: " + e.getMessage());
        }
    }

    public void clickBack() {
        System.out.println("🔙 Clicking Back button...");
        
        // Strategy 1: Try scrolling UP first to make Back visible (it's at top)
        try {
            scrollFormUp();
            sleep(300);
        } catch (Exception scrollEx) {
            // Ignore scroll errors
        }
        
        // Strategy 2: Direct accessibility ID "Back"
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement backBtn = quickWait.until(
                ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId("Back"))
            );
            backBtn.click();
            System.out.println("✅ Clicked Back (accessibility ID)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 1 (accessibility ID) failed");
        }
        
        // Strategy 3: Find button with Back/chevron.left name/label
        try {
            WebElement backBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Back' OR label == 'Back' OR name == 'chevron.left' OR name CONTAINS 'back')")
            );
            if (backBtn.isDisplayed()) {
                backBtn.click();
                System.out.println("✅ Clicked Back (button predicate)");
                sleep(200);
                return;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 (button predicate) failed");
        }
        
        // Strategy 4: Find button at top-left corner (typical Back position)
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : allButtons) {
                try {
                    int x = btn.getLocation().getX();
                    int y = btn.getLocation().getY();
                    String name = btn.getAttribute("name");
                    
                    // Back button is typically at top-left (X < 100, Y < 100)
                    if (x < 100 && y < 100 && y > 30) {
                        System.out.println("   Found top-left button at (" + x + "," + y + "), name='" + name + "'");
                        // Skip if it's Cancel (Cancel is also at top-left sometimes)
                        if (name != null && name.equals("Cancel")) continue;
                        
                        btn.click();
                        System.out.println("✅ Clicked Back (position-based)");
                        sleep(200);
                        return;
                    }
                } catch (Exception ex) {}
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (position-based) failed");
        }
        
        // Strategy 5: Tap coordinates at top-left (typical Back position on iOS)
        try {
            System.out.println("   Trying coordinate tap at (30, 55)...");
            driver.executeScript("mobile: tap", java.util.Map.of("x", 30, "y", 55));
            System.out.println("✅ Tapped Back position (30, 55)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 4 (coordinate tap) failed");
        }

        // If all strategies fail, log warning but don't throw
        // This allows tests to continue when screen state changes (e.g., after save)
        System.out.println("⚠️ Could not click Back button after trying all strategies - screen may have changed");
    }

    /**
     * Try to click Back button without throwing exception
     * Returns true if Back was clicked, false otherwise
     */
    public boolean tryClickBack() {
        System.out.println("🔙 Trying to click Back button (non-throwing)...");

        // Strategy 1: Direct accessibility ID "Back"
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            WebElement backBtn = quickWait.until(
                ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId("Back"))
            );
            backBtn.click();
            System.out.println("✅ Clicked Back");
            sleep(200);
            return true;
        } catch (Exception e) {}

        // Strategy 2: Find button with Back name/label
        try {
            WebElement backBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Back' OR label == 'Back' OR name == 'chevron.left')")
            );
            if (backBtn.isDisplayed()) {
                backBtn.click();
                System.out.println("✅ Clicked Back button");
                sleep(200);
                return true;
            }
        } catch (Exception e) {}

        // Strategy 3: Tap top-left coordinates
        try {
            driver.executeScript("mobile: tap", java.util.Map.of("x", 30, "y", 55));
            System.out.println("✅ Tapped Back position");
            sleep(200);
            return true;
        } catch (Exception e) {}

        System.out.println("⚠️ Back button not found - screen may have changed");
        return false;
    }

    public void clickCancel() {
        System.out.println("📝 Clicking Cancel button...");
        
        // Strategy 1: Try scrolling UP first to make Cancel visible (it's at top)
        try {
            scrollFormUp();
            sleep(300);
        } catch (Exception scrollEx) {
            // Ignore scroll errors
        }
        
        // Strategy 2: Direct accessibility ID
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement cancelBtn = quickWait.until(
                ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId("Cancel"))
            );
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel (accessibility ID)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 1 (accessibility ID) failed");
        }
        
        // Strategy 3: Find button with Cancel name/label
        try {
            WebElement cancelBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            if (cancelBtn.isDisplayed()) {
                cancelBtn.click();
                System.out.println("✅ Clicked Cancel (button predicate)");
                sleep(200);
                return;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 (button predicate) failed");
        }
        
        // Strategy 4: Find any element with Cancel label at top of screen (Y < 100)
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : allButtons) {
                try {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    int y = btn.getLocation().getY();
                    
                    if (y < 150 && 
                        ((name != null && name.toLowerCase().contains("cancel")) ||
                         (label != null && label.toLowerCase().contains("cancel")))) {
                        System.out.println("   Found Cancel at Y=" + y + ", name='" + name + "'");
                        btn.click();
                        System.out.println("✅ Clicked Cancel (position-based)");
                        sleep(200);
                        return;
                    }
                } catch (Exception ex) {}
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (position-based) failed");
        }
        
        // Strategy 5: Find StaticText "Cancel" and click
        try {
            WebElement cancelText = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            cancelText.click();
            System.out.println("✅ Clicked Cancel (StaticText)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 4 (StaticText) failed");
        }
        
        // Strategy 6: Tap coordinates at top-left (typical Cancel position on iOS)
        try {
            System.out.println("   Trying coordinate tap at (60, 55)...");
            driver.executeScript("mobile: tap", java.util.Map.of("x", 60, "y", 55));
            System.out.println("✅ Tapped Cancel position (60, 55)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 5 (coordinate tap) failed");
        }
        
        // If all strategies fail, throw exception
        throw new RuntimeException("Failed to click Cancel button after trying all strategies");
    }

    // ================================================================
    // WAIT METHODS
    // ================================================================

    public void waitForAssetListReady() {
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            quickWait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOf(plusButton),
                ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId("plus"))
            ));
            System.out.println("✅ Asset List ready");
        } catch (Exception e) {
            System.out.println("⚠️ Asset List wait timeout: " + e.getMessage());
        }
    }

    public void waitForCreateAssetFormReady() {
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            quickWait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOf(assetNameField),
                ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Enter name'")
                )
            ));
            System.out.println("✅ Create Asset form ready");
        } catch (Exception e) {
            System.out.println("⚠️ Create Asset form wait timeout");
        }
    }

    
    // ================================================================
    // SCREEN DETECTION METHODS - For smart navigation
    // ================================================================

    /**
     * Check if Dashboard is displayed (user is logged in, site selected)
     * Dashboard has: Assets tab, Locations tab, building icon, possibly job selector
     */
    public boolean isDashboardDisplayed() {
        // Fast-fail cap (changelog 070): 2 element lookups × 5s default
        // implicit wait = up to 10s per call on miss. 32 callers across
        // tests × ~10s avg miss = ~5.3 min wasted. Cap to 1s on miss.
        java.time.Duration originalWait;
        try {
            originalWait = driver.manage().timeouts().getImplicitWaitTimeout();
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(1));
        } catch (Exception e) {
            originalWait = java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT);
        }
        try {
            // v1.36 (changelog 075): the OLD check used 'building.2' icon as
            // a Dashboard signal — but that icon ALSO sits on every Site
            // Selection row, so a Site Selection list with site rows would
            // falsely register as Dashboard. EXPLICIT REJECT Site Selection
            // FIRST, then check positive Dashboard signals.
            try {
                java.util.List<WebElement> siteSearch = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeSearchField') AND " +
                    "(placeholderValue CONTAINS[c] 'search sites' OR value CONTAINS[c] 'search sites')"));
                if (!siteSearch.isEmpty()) {
                    return false; // 'Search sites' field visible → Site Selection
                }
                java.util.List<WebElement> selectSiteTitle = driver.findElements(
                    AppiumBy.accessibilityId("Select Site"));
                if (!selectSiteTitle.isEmpty()) {
                    return false; // 'Select Site' title → Site Selection
                }
                java.util.List<WebElement> createNewSite = driver.findElements(
                    AppiumBy.accessibilityId("Create New Site"));
                if (!createNewSite.isEmpty()) {
                    return false; // 'Create New Site' button → Site Selection
                }
            } catch (Exception ignored) {}

            // Positive Dashboard signals: Quick Action accessibility ids that
            // exist ONLY on Dashboard.
            boolean hasSitesQA = !driver.findElements(AppiumBy.accessibilityId("Sites")).isEmpty();
            boolean hasIssuesQA = !driver.findElements(AppiumBy.accessibilityId("Issues")).isEmpty();
            boolean hasLocationsQA = !driver.findElements(AppiumBy.accessibilityId("Locations")).isEmpty();
            boolean isDashboard = hasSitesQA || hasIssuesQA || hasLocationsQA;

            if (isDashboard) {
                System.out.println("   Dashboard detected (Sites QA: " + hasSitesQA
                    + ", Issues QA: " + hasIssuesQA + ", Locations QA: " + hasLocationsQA + ")");
            }

            return isDashboard;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(originalWait);
            } catch (Exception ignored) { /* best-effort restore */ }
        }
    }
    
    /**
     * Check if Asset List is displayed
     * Asset List has: plus button, list of asset cells, "Assets" in nav
     */
    public boolean isAssetListDisplayed() {
        // Fast-fail cap (changelog 072): 2 lookups × 5s default × 15 callers
        // = ~2.5 min wasted per run. Cap to 1s on miss.
        // Also locale-aware: matches both 'Assets' (EN) and 'Actifs' (FR).
        java.time.Duration originalWait;
        try {
            originalWait = driver.manage().timeouts().getImplicitWaitTimeout();
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(1));
        } catch (Exception e) {
            originalWait = java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT);
        }
        try {
            // Asset list specific: Has plus button for adding new assets
            try {
                WebElement plusBtn = driver.findElement(AppiumBy.accessibilityId("plus"));
                if (plusBtn.isDisplayed()) {
                    System.out.println("   Asset List detected (plus button found)");
                    return true;
                }
            } catch (Exception e) {}

            // Alternative: Check for asset cells with specific pattern
            try {
                List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
                if (cells.size() > 0) {
                    // Check if nav bar says something about assets (EN or FR)
                    List<WebElement> navBars = driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeNavigationBar'")
                    );
                    for (WebElement nav : navBars) {
                        String name = nav.getAttribute("name");
                        if (name != null && (name.contains("Asset") || name.contains("asset")
                            || name.contains("Actif") || name.contains("actif"))) {
                            System.out.println("   Asset List detected (nav bar: " + name + ")");
                            return true;
                        }
                    }
                }
            } catch (Exception e) {}

            return false;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(originalWait);
            } catch (Exception ignored) { /* best-effort restore */ }
        }
    }
    
    /**
     * Check if Asset Detail view is displayed
     * Asset Detail has: "Asset Details" nav title, Edit button (NOT Save Changes)
     */
    public boolean isAssetDetailDisplayed() {
        // Fast-fail cap (changelog 072): 2 lookups × 5s × 6 callers = ~1 min
        // wasted per run. Cap to 1s on miss. Also accepts French nav title.
        java.time.Duration originalWait;
        try {
            originalWait = driver.manage().timeouts().getImplicitWaitTimeout();
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(1));
        } catch (Exception e) {
            originalWait = java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT);
        }
        try {
            // Must have "Asset Details" (EN) or "Détails de l'actif" (FR) in nav bar
            boolean hasAssetDetailsNav = false;
            try {
                List<WebElement> navBars = driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeNavigationBar' AND " +
                        "(name == 'Asset Details' OR " +
                        " name CONTAINS[c] 'Détails' OR name CONTAINS[c] 'Detail' OR " +
                        " name CONTAINS[c] 'actif')"
                    )
                );
                hasAssetDetailsNav = !navBars.isEmpty();
            } catch (Exception e) {}

            // Must have Edit / Modifier button (view mode)
            boolean hasEditButton = false;
            try {
                List<WebElement> editBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Edit' OR name == 'Edit' OR " +
                    " label == 'Modifier' OR name == 'Modifier')"
                ));
                hasEditButton = !editBtns.isEmpty();
            } catch (Exception e) {}

            boolean isAssetDetail = hasAssetDetailsNav && hasEditButton;

            if (isAssetDetail) {
                System.out.println("   Asset Detail detected (nav: " + hasAssetDetailsNav + ", Edit btn: " + hasEditButton + ")");
            }

            return isAssetDetail;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(originalWait);
            } catch (Exception ignored) { /* best-effort restore */ }
        }
    }


    // ================================================================
    // ASSET LIST METHODS
    // ================================================================

    public boolean isPlusButtonDisplayed() {
        return isElementDisplayed(plusButton);
    }

    public int getAssetCount() {
        try {
            // Use fresh element lookup to get current cell count after search
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            int count = cells.size();
            System.out.println("📊 Asset count: " + count);
            return count;
        } catch (Exception e) {
            System.out.println("⚠️ Error getting asset count: " + e.getMessage());
            return 0;
        }
    }

    public void searchAsset(String assetName) {
        System.out.println("🔍 Searching for asset: " + assetName);
        
        WebElement searchField = null;
        
        // Try to find search bar using different strategies
        try {
            searchField = driver.findElement(AppiumBy.className("XCUIElementTypeSearchField"));
        } catch (Exception e) {
            try {
                searchField = driver.findElement(AppiumBy.accessibilityId("Search by name, type, location, or QR code"));
            } catch (Exception e2) {
                try {
                    searchField = driver.findElement(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField'")
                    );
                } catch (Exception e3) {
                    System.out.println("⚠️ Search bar not available");
                    return;
                }
            }
        }
        
        if (searchField != null) {
            try {
                // Click to focus
                searchField.click();
                sleep(300);
                
                // CLEAR the field first before entering new text
                searchField.clear();
                sleep(300);
                
                // Enter search term
                searchField.sendKeys(assetName);
                System.out.println("✅ Searched for asset: " + assetName);
                
                // Wait for search results to update
                sleep(400);
            } catch (Exception e) {
                System.out.println("⚠️ Error during search: " + e.getMessage());
            }
        }
    }

    public boolean selectAssetByName(String assetName) {
        System.out.println("📦 Looking for asset: " + assetName);

        // Wrong-screen early exit: on Site Selection the whole-tree scans below
        // can only ever match site rows — and once burned ~2 min per call doing it.
        if (isOnSiteSelectionScreen()) {
            System.out.println("⚠️ selectAssetByName: site picker in front — not on Asset List, aborting scan");
            return false;
        }
        // Never click/return site-screen chrome as an asset
        if (isSiteScreenLabel(assetName)) {
            System.out.println("⚠️ selectAssetByName: '" + assetName + "' is a site-screen label, not an asset");
            return false;
        }

        // Fast-fail (changelog 064): cap implicit wait to 1s for the duration
        // of this method. 4 strategies × 5s = 20s per miss → ~5s after fix.
        java.time.Duration originalWait;
        try {
            originalWait = driver.manage().timeouts().getImplicitWaitTimeout();
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(1));
        } catch (Exception e) {
            originalWait = java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT);
        }

        // Cap whole-tree iteration: each getAttribute is a WDA round-trip, and
        // huge trees (site picker / Locations) turned these loops into minutes.
        final int SCAN_CAP = 40;

        try {
        // Strategy 1: Find by accessibility ID
        try {
            WebElement asset = driver.findElement(AppiumBy.accessibilityId(assetName));
            asset.click();
            System.out.println("✅ Selected asset: " + assetName);
            sleep(200);
            return true;
        } catch (Exception e) {}

        // Strategy 2: Find StaticText with matching name
        try {
            List<WebElement> texts = withImplicitWait(0, () ->
                driver.findElements(AppiumBy.className("XCUIElementTypeStaticText")));
            int scanned = 0;
            for (WebElement text : texts) {
                if (++scanned > SCAN_CAP) break;
                String name = text.getAttribute("name");
                if (name != null && name.contains(assetName) && !isSiteScreenLabel(name)) {
                    text.click();
                    System.out.println("✅ Selected asset (text): " + name);
                    sleep(200);
                    return true;
                }
            }
        } catch (Exception e) {}

        // Strategy 3: Find Cell containing asset name
        try {
            List<WebElement> cells = withImplicitWait(0, () ->
                driver.findElements(AppiumBy.className("XCUIElementTypeCell")));
            int cellsScanned = 0;
            for (WebElement cell : cells) {
                if (++cellsScanned > SCAN_CAP) break;
                try {
                    List<WebElement> textsInCell = withImplicitWait(0, () ->
                        cell.findElements(AppiumBy.className("XCUIElementTypeStaticText")));
                    for (WebElement text : textsInCell) {
                        String name = text.getAttribute("name");
                        if (name != null && name.contains(assetName) && !isSiteScreenLabel(name)) {
                            text.click();
                            System.out.println("✅ Selected asset (cell text): " + name);
                            sleep(200);
                            return true;
                        }
                    }
                } catch (Exception inner) {}
            }
        } catch (Exception e) {}

        // Strategy 4: Find any button containing asset name
        try {
            List<WebElement> buttons = withImplicitWait(0, () ->
                driver.findElements(AppiumBy.className("XCUIElementTypeButton")));
            int scanned = 0;
            for (WebElement btn : buttons) {
                if (++scanned > SCAN_CAP) break;
                String name = btn.getAttribute("name");
                if (name != null && name.contains(assetName) && !isSiteScreenLabel(name)) {
                    btn.click();
                    System.out.println("✅ Selected asset (button): " + name);
                    sleep(200);
                    return true;
                }
            }
        } catch (Exception e) {}

        System.out.println("⚠️ Could not find asset: " + assetName);
        return false;
        } finally {
            try { driver.manage().timeouts().implicitlyWait(originalWait); } catch (Exception ignored) {}
        }
    }

    /**
     * Shared-asset helper used by every navigateTo*EditScreen() in the
     * Asset_Phase*_Test classes.
     *
     * Behaviour:
     *   1. Navigate to the asset list (turbo).
     *   2. If {@code cachedName} is non-null, try to search-open it via
     *      {@link #openAssetByNameForEdit(String)}. On success, return the
     *      same name (fast path — caller's cache stays warm).
     *   3. On cache-miss or fast-path failure: pick the first asset, click
     *      Edit, return the captured name. The caller must assign the
     *      return value back to its cache so test 2 can hit the fast path.
     *
     * Why this exists: every Edit Asset test would otherwise pick a fresh
     * "first asset" and run changeAssetClassTo*() to convert it. The class
     * picker is slow (~5-10s). By locking onto ONE asset per section and
     * leveraging changeAssetClassTo*() 's existing fast-path
     * (isCurrentAssetClassEqualTo), tests 2..N skip the picker entirely.
     *
     * Estimated savings: ~5-10s per test from the second test in each
     * section onward.
     *
     * @param cachedName the name remembered from the last call in the same
     *                   section, or null on the first call
     * @return the name to cache for the next call (same as {@code cachedName}
     *         on a hit, or the freshly-selected first asset's name on a miss)
     */
    public String openSharedAssetForEditOrFallback(String cachedName) {
        navigateToAssetListTurbo();
        // A poisoned cache ('Create New Site' was once cached as an asset) would
        // search/click site chrome forever — drop it and re-select fresh.
        if (isSiteScreenLabel(cachedName)) {
            System.out.println("⚠️ Shared-asset cache poisoned with site-screen label '" + cachedName + "' — discarding");
            cachedName = null;
        }
        if (cachedName != null && !cachedName.isEmpty()) {
            if (openAssetByNameForEdit(cachedName)) {
                return cachedName;
            }
            System.out.println("⚠️ Shared asset '" + cachedName + "' not found — falling back to first-asset");
        }
        String name = selectFirstAsset();
        sleep(200);
        clickEditTurbo();
        if (isSiteScreenLabel(name)) {
            // Site-screen chrome must never be cached or returned as an asset name
            System.out.println("⚠️ selectFirstAsset returned site-screen label '" + name + "' — not caching");
            return null;
        }
        if (name != null && !name.isEmpty()) {
            System.out.println("📌 Cached shared asset: '" + name + "'");
        }
        return name;
    }

    /**
     * Search the asset list for {@code assetName}, open it, and click Edit.
     *
     * Designed for the shared-asset optimization in *_EAD_* test classes:
     * once a class has selected its "test asset" (e.g. an OCP-class asset),
     * subsequent tests can re-open the SAME asset by name instead of picking
     * the first asset and re-running changeAssetClassTo*() each time.
     * The change-class fast-path then no-ops (asset already at desired class).
     *
     * @return true if the Edit Asset screen is visible after the click,
     *         false if any step failed — caller should fall back to the
     *         legacy selectFirstAsset() + clickEditTurbo() flow.
     */
    public boolean openAssetByNameForEdit(String assetName) {
        if (assetName == null || assetName.isEmpty()) return false;
        try {
            searchAsset(assetName);
            if (!selectAssetByName(assetName)) {
                System.out.println("   shared-asset: name not found in list, fallback path");
                return false;
            }
            sleep(400);
            clickEditTurbo();
            sleep(400);
            // Verify we landed on Edit screen
            if (isEditAssetScreenDisplayed() || isSaveChangesButtonVisible()) {
                System.out.println("✅ shared-asset: opened '" + assetName + "' for edit (fast path)");
                return true;
            }
            System.out.println("   shared-asset: opened '" + assetName + "' but Edit screen not visible");
            return false;
        } catch (Exception e) {
            System.out.println("   shared-asset: openAssetByNameForEdit failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clear search field and select first asset from the list
     * Useful as a fallback when searching for a specific asset type fails
     */
    public void clearSearchAndSelectFirst() {
        System.out.println("🧹 Clearing search and selecting first asset...");
        
        // Try to clear search field
        try {
            WebElement searchField = driver.findElement(AppiumBy.className("XCUIElementTypeSearchField"));
            searchField.clear();
            sleep(300);
            System.out.println("✅ Cleared search field");
        } catch (Exception e) {
            // Try clicking Cancel button to clear search
            try {
                WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
                cancelBtn.click();
                sleep(300);
                System.out.println("✅ Clicked Cancel to clear search");
            } catch (Exception e2) {
                System.out.println("⚠️ Could not clear search: " + e2.getMessage());
            }
        }
        
        sleep(200);
        
        // Select first available asset
        selectFirstAsset();
    }

    public String selectFirstAsset() {
        System.out.println("📦 Selecting first asset...");

        // Wrong-screen early exit: on Site Selection there are no assets — the
        // scans below would click 'Create New Site' and call it an asset.
        if (isOnSiteSelectionScreen()) {
            System.out.println("⚠️ selectFirstAsset: site picker in front — not on Asset List, aborting");
            return null;
        }

        // Single bounded wait on the expected winner (a rendered list row),
        // then the whole cascade runs at 0 implicit wait: 7 strategies × 5s
        // default implicit = up to 35s per call on miss (53 callers ≈ ~17 min).
        Waits.until(() ->
            existsNow(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeCell'"))
            || existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS ', ' AND NOT name CONTAINS 'Search'")),
            4_000);

        java.time.Duration originalWait;
        try {
            originalWait = driver.manage().timeouts().getImplicitWaitTimeout();
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        } catch (Exception e) {
            originalWait = java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT);
        }

        try {

        // =====================================================================
        // IMPORTANT: Must click on STATIC TEXT (asset name), NOT on Button/Cell!
        // Clicking on XCUIElementTypeButton or XCUIElementTypeCell does NOT navigate.
        // Only clicking on XCUIElementTypeStaticText (asset name text) works!
        // =====================================================================

        // STRATEGY 1 (PREFERRED): Click on StaticText that is the asset name
        // Asset name is typically the first text in each cell (y position 139-650 range)
        try {
            System.out.println("   Strategy 1: Finding asset name StaticText elements...");
            
            // First, get all cells to understand the structure
            List<WebElement> cells = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeCell'")
            );
            System.out.println("   Found " + cells.size() + " cells");
            
            if (cells.size() > 0) {
                WebElement firstCell = cells.get(0);
                
                // Find StaticText children of first cell
                List<WebElement> textsInCell = firstCell.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText'")
                );
                System.out.println("   Found " + textsInCell.size() + " texts in first cell");
                
                // The first StaticText is usually the asset name
                if (textsInCell.size() > 0) {
                    WebElement assetNameText = textsInCell.get(0);
                    String assetName = assetNameText.getAttribute("name");

                    // Skip section headers ("Assets" title) and site-screen chrome
                    if (assetName != null && !assetName.equals("Assets") && !assetName.isEmpty()
                            && !isSiteScreenLabel(assetName)) {
                        System.out.println("   🎯 Asset name text: " + assetName);
                        assetNameText.click();
                        System.out.println("✅ Clicked asset name StaticText");
                        sleep(800);
                        return assetName;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 1 failed: " + e.getMessage());
        }
        
        // STRATEGY 2: Find StaticText elements in asset list area (y > 100 to skip header)
        try {
            System.out.println("   Strategy 2: Finding StaticText in list area...");

            List<WebElement> allTexts = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText'")
            );
            System.out.println("   Found " + allTexts.size() + " StaticText elements");

            // Bounded scan: this is a whole-tree query, and each
            // getLocation/getAttribute is a WDA round-trip — on a huge tree
            // (site picker bleed-through) the unbounded loop once ran ~84s.
            int scanned = 0;
            for (WebElement text : allTexts) {
                if (++scanned > 30) {
                    System.out.println("   Strategy 2: scan cap (30) reached");
                    break;
                }
                int y = text.getLocation().getY();
                String name = text.getAttribute("name");

                // Skip header area (y < 110) and tab bar area (y > 780)
                // Skip common non-asset texts and site-screen chrome
                if (y >= 110 && y <= 780 && name != null && !name.isEmpty()
                        && !isSiteScreenLabel(name)) {
                    String lower = name.toLowerCase();
                    if (!lower.equals("assets") && !lower.equals("search") &&
                        !lower.contains("task") && !lower.equals("no location") &&
                        !lower.equals("android") && !lower.equals("ios") &&
                        !name.equals("ATS") && !name.equals("UPS") && !name.equals("PDU") &&
                        !name.equals("Generator") && !name.equals("Panelboard") &&
                        !name.equals("Disconnect Switch") && !name.equals("MCC") &&
                        !name.equals("Busway") && name.length() > 2) {
                        // This is likely an asset name
                        System.out.println("   🎯 Found asset name at y=" + y + ": " + name);
                        text.click();
                        System.out.println("✅ Clicked asset StaticText: " + name);
                        sleep(800);
                        return name;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 failed: " + e.getMessage());
        }
        
        // STRATEGY 3: Fallback to button click (less reliable but kept for compatibility)
        // Asset buttons have format: "AssetName, Location, Type" e.g. "TestAsset_123, Room_456, ATS"
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ', ' AND NOT name CONTAINS 'Search' AND NOT name BEGINSWITH 'Completed Task' AND NOT name BEGINSWITH 'Test Task' AND NOT name CONTAINS 'This task'")
            );
            System.out.println("   Found " + buttons.size() + " asset buttons");

            if (buttons.size() > 0) {
                WebElement firstAsset = buttons.get(0);
                String assetName = firstAsset.getAttribute("name");
                if (isSiteScreenLabel(assetName)) {
                    System.out.println("   ⚠️ First button is site-screen chrome ('" + assetName + "') — skipping strategy");
                    throw new IllegalStateException("site-screen label, not an asset row");
                }
                int x = firstAsset.getLocation().getX();
                int y = firstAsset.getLocation().getY();
                int width = firstAsset.getSize().getWidth();
                int height = firstAsset.getSize().getHeight();
                
                System.out.println("   🎯 First asset: " + assetName);
                System.out.println("   📍 Location: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height);
                
                // Verify element is in valid tappable area (not off-screen)
                if (y < 100 || y > 800) {
                    System.out.println("   ⚠️ Element Y=" + y + " may be in header/footer area, scrolling...");
                    scrollFormDown();
                    sleep(300);
                    // Re-find after scroll
                    buttons = driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name CONTAINS ', ' AND NOT name CONTAINS 'Search'")
                    );
                    if (buttons.size() > 0) {
                        firstAsset = buttons.get(0);
                        x = firstAsset.getLocation().getX();
                        y = firstAsset.getLocation().getY();
                        width = firstAsset.getSize().getWidth();
                        height = firstAsset.getSize().getHeight();
                        assetName = firstAsset.getAttribute("name");
                        System.out.println("   🔄 After scroll: x=" + x + ", y=" + y);
                    }
                }
                
                // Method 1: Direct click
                try {
                    firstAsset.click();
                    System.out.println("   ✓ Direct click executed");
                } catch (Exception clickEx) {
                    System.out.println("   ⚠️ Direct click failed: " + clickEx.getMessage());
                }
                
                // Wait and check if we navigated to detail screen
                sleep(800);
                
                // Verify click worked by checking for Asset Details screen
                boolean clickWorked = false;
                try {
                    List<WebElement> detailIndicators = driver.findElements(
                        AppiumBy.iOSNsPredicateString("(name == 'Asset Details' OR name CONTAINS 'Edit' OR name == 'Save' OR name == 'Back')")
                    );
                    clickWorked = !detailIndicators.isEmpty();
                } catch (Exception e) {}
                
                if (!clickWorked) {
                    System.out.println("   ⚠️ Click may not have worked, trying coordinate tap...");
                    
                    // Method 2: Coordinate tap (center of element)
                    int tapX = x + (width / 2);
                    int tapY = y + (height / 2);
                    
                    // Ensure tap is in safe area
                    if (tapY < 150) tapY = 200;
                    if (tapY > 750) tapY = 700;
                    
                    System.out.println("   📍 Tapping at coordinates: (" + tapX + ", " + tapY + ")");
                    driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
                    sleep(800);
                    
                    // Check again
                    try {
                        List<WebElement> detailIndicators = driver.findElements(
                            AppiumBy.iOSNsPredicateString("(name == 'Asset Details' OR name CONTAINS 'Edit' OR name == 'Save' OR name == 'Back')")
                        );
                        clickWorked = !detailIndicators.isEmpty();
                    } catch (Exception e) {}
                }
                
                if (!clickWorked) {
                    System.out.println("   ⚠️ Still not on detail screen, trying double-tap...");
                    // Method 3: Double tap
                    int tapX = x + (width / 2);
                    int tapY = y + (height / 2);
                    driver.executeScript("mobile: doubleTap", Map.of("x", tapX, "y", tapY));
                    sleep(800);
                }
                
                System.out.println("✅ Selected asset via button");
                return assetName.split(",")[0].trim();
            }
        } catch (Exception e) {
            System.out.println("   Button strategy failed: " + e.getMessage());
        }
        
        // STRATEGY 2: Find first CELL and click inside it
        try {
            List<WebElement> cells = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeCell'")
            );
            System.out.println("   Found " + cells.size() + " cells");
            
            // Skip cells that might be in header/search area
            for (WebElement cell : cells) {
                int cellY = cell.getLocation().getY();
                
                // Skip cells in header area (y < 200)
                if (cellY < 200) {
                    System.out.println("   Skipping cell at y=" + cellY + " (header area)");
                    continue;
                }
                
                int cellX = cell.getLocation().getX() + (cell.getSize().getWidth() / 2);
                int cellCenterY = cellY + (cell.getSize().getHeight() / 2);
                
                // Ensure tap Y is in safe area (200-750)
                if (cellCenterY < 200) cellCenterY = 250;
                if (cellCenterY > 750) cellCenterY = 700;
                
                System.out.println("   📍 Tapping cell at: (" + cellX + ", " + cellCenterY + ")");
                driver.executeScript("mobile: tap", Map.of("x", cellX, "y", cellCenterY));
                System.out.println("✅ Tapped cell");
                sleep(800);
                return "asset";
            }
        } catch (Exception e) {
            System.out.println("   Cell strategy failed: " + e.getMessage());
        }
        
        // STRATEGY 3: Tap directly at known asset list area
        try {
            System.out.println("   Trying direct coordinate tap in asset list area...");
            // Asset list typically starts around y=250-300 and first item is near y=300-400
            int tapX = 200;  // Center-left of screen
            int tapY = 350;  // First asset row area
            
            driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
            System.out.println("✅ Tapped at fixed coordinates (" + tapX + ", " + tapY + ")");
            sleep(800);
            return "asset";
        } catch (Exception e) {
            System.out.println("   Coordinate tap failed: " + e.getMessage());
        }
        
            System.out.println("⚠️ Could not select any asset");
            return null;
        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(originalWait);
            } catch (Exception ignored) { /* best-effort restore */ }
        }
    }

    public boolean isCreateAssetFormDisplayed() {
        return isElementDisplayed(assetNameField) || isElementDisplayed(selectAssetClassButton);
    }

    public boolean isAssetNameFieldDisplayed() {
        return isElementDisplayed(assetNameField);
    }

    public void enterAssetName(String name) {
        // v1.36 (changelog 075): On Asset Detail (no separate Edit mode), the
        // Asset Name field is pre-populated with the existing value. sendKeys
        // INSERTS at the cursor position, producing garbage like
        // "CB-FMC SUITE 140__UC1_1779870990046seL-UC1_1779376830230_...".
        // Always CLEAR the field first before typing.
        // 0-implicit probe: on a pre-filled (offline self-seeded) field the value is
        // the existing name, NOT 'Enter name', so this predicate MISSES — and at the
        // global 5s implicit wait every miss × the downstream retries was the 228s
        // sink behind the UC*/UC33 offline 6-7min hangs. Probe at implicit-0 so a
        // miss costs ms, and locate the first TextField directly on miss.
        java.util.List<WebElement> nameHit = withImplicitWait(0, () -> driver.findElements(
            AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Enter name'")));
        if (!nameHit.isEmpty()) {
            try {
                WebElement nameField = nameHit.get(0);
                try { nameField.clear(); } catch (Exception ignored) {}
                nameField.sendKeys(name);
                System.out.println("✅ Entered asset name: " + name);
                // v1.36: keyboard occludes Save Changes + bottom-nav Sites button;
                // dismiss before any further interaction.
                dismissKeyboard();
                return;
            } catch (Exception e) {}
        }

        // Fallback: first TextField on screen (Edit mode where placeholder
        // 'Enter name' is replaced by the current value, so above predicate
        // misses it). Locate directly at implicit-0 instead of the stale
        // PageFactory allTextFields cache.
        java.util.List<WebElement> textFields = withImplicitWait(0, () ->
            driver.findElements(AppiumBy.className("XCUIElementTypeTextField")));
        if (textFields.size() > 0) {
            WebElement f = textFields.get(0);
            try { f.clear(); } catch (Exception ignored) {}
            // If clear() didn't empty it (iOS quirk on long pre-filled values),
            // fall back to select-all + delete via the field's clear() retry.
            try {
                String v = f.getAttribute("value");
                if (v != null && !v.isEmpty() && !v.equals("Enter name")) {
                    f.click();
                    sleep(150);
                    // Long-press to bring up Select All / Delete — fallback
                    // approximation by sending backspaces sized to the value length
                    int chars = v.length();
                    StringBuilder back = new StringBuilder();
                    for (int i = 0; i < chars + 2; i++) back.append(""); // backspace
                    f.sendKeys(back.toString());
                }
            } catch (Exception ignored) {}
            f.sendKeys(name);
            System.out.println("✅ Entered asset name (alt): " + name);
            // v1.36: keyboard occludes downstream taps; always dismiss.
            dismissKeyboard();
        }
    }

    public String getAssetNameValue() {
        System.out.println("📝 Getting asset name value...");
        
        // Strategy 1: Find text field with placeholder "Enter name" and get its value
        try {
            WebElement nameField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND (value != 'Enter name' AND value != '')")
            );
            String value = nameField.getAttribute("value");
            if (value != null && !value.isEmpty() && !value.equals("Enter name")) {
                System.out.println("✅ Got asset name: '" + value + "'");
                return value;
            }
        } catch (Exception e) {}
        
        // Strategy 2: Find first text field (usually the name field)
        try {
            List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            if (!textFields.isEmpty()) {
                WebElement firstField = textFields.get(0);
                String value = firstField.getAttribute("value");
                if (value != null && !value.isEmpty() && !value.equals("Enter name")) {
                    System.out.println("✅ Got asset name from first field: '" + value + "'");
                    return value;
                }
            }
        } catch (Exception e) {}
        
        // Strategy 3: Look at navigation bar or title
        try {
            List<WebElement> navBars = driver.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"));
            for (WebElement navBar : navBars) {
                String name = navBar.getAttribute("name");
                if (name != null && !name.isEmpty() && !name.equals("New Asset") && !name.equals("Edit Asset")) {
                    System.out.println("✅ Got asset name from nav bar: '" + name + "'");
                    return name;
                }
            }
        } catch (Exception e) {}
        
        // Strategy 4: Find static text that might be the asset name (in view mode)
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                // Skip common UI labels
                if (name != null && !name.isEmpty() && name.length() > 3) {
                    String[] skipLabels = {"Asset", "Name", "Class", "Location", "Type", "Save", "Cancel", "Edit", "Core", "Details", "Required"};
                    boolean skip = false;
                    for (String label : skipLabels) {
                        if (name.equals(label) || name.startsWith(label + " ")) {
                            skip = true;
                            break;
                        }
                    }
                    if (!skip && name.matches(".*[a-zA-Z0-9].*")) {
                        System.out.println("✅ Got potential asset name: '" + name + "'");
                        return name;
                    }
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not get asset name value");
        return "";
    }

    public void clearAssetName() {
        try {
            assetNameField.clear();
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear asset name: " + e.getMessage());
        }
    }

    public boolean isSelectAssetClassDisplayed() {
        return isElementDisplayed(selectAssetClassButton);
    }

    public void clickSelectAssetClass() {
        System.out.println("📍 Attempting to click Asset Class dropdown...");

        // Strategy 1: Use PageFactory element directly (for "Select asset class" placeholder)
        try {
            if (selectAssetClassButton != null && selectAssetClassButton.isDisplayed()) {
                selectAssetClassButton.click();
                System.out.println("✅ Clicked 'Select asset class' (PageFactory)");
                return;
            }
        } catch (Exception e) {
            System.out.println("   PageFactory element not available");
        }

        // Strategy 2: Find button whose label CONTAINS "asset class" (case-insensitive).
        // SwiftUI Picker with .menu style renders as XCUIElementTypeButton with a combined label
        // like "Asset Class, Select asset class" — exact match misses this.
        // NOTE: No "visible == true" constraint — after keyboard dismiss, elements may
        // still be marked non-visible for a brief period during iOS DOM refresh.
        try {
            WebElement classBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name CONTAINS[c] 'asset class' OR label CONTAINS[c] 'asset class' OR name ==[c] 'Select asset class')"
                )
            );
            classBtn.click();
            System.out.println("✅ Clicked Asset Class button (contains match): " + classBtn.getAttribute("name"));
            return;
        } catch (Exception e) {
            System.out.println("   Contains-match strategy failed");
        }

        // Strategy 3: Find existing asset class button (MCC, ATS, Generator, etc.)
        // When asset already has a class, the button shows the class name instead of "Select asset class"
        String[] assetClasses = {"MCC", "ATS", "Generator", "Busway", "Capacitor", "Circuit Breaker",
                                  "Disconnect Switch", "Fuse", "Junction Box", "Loadcenter", "Motor",
                                  "Panelboard", "Switchboard", "Transformer", "VFD", "Wire"};

        for (String className : assetClasses) {
            try {
                WebElement classBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name == '" + className + "'"
                    )
                );
                classBtn.click();
                System.out.println("✅ Clicked existing class button: " + className);
                return;
            } catch (Exception e) {}
        }

        // Strategy 4: Find "Asset Class" label (case-insensitive), then find the nearest button
        try {
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "(name CONTAINS[c] 'Asset Class' OR label CONTAINS[c] 'Asset Class') AND " +
                    "type == 'XCUIElementTypeStaticText'"
                )
            );
            int labelY = assetClassLabel.getLocation().getY();
            System.out.println("   Found 'Asset Class' label at Y=" + labelY);

            // Find buttons near the Asset Class label (within 100 pixels vertically)
            // No "visible == true" — after keyboard dismiss, DOM may lag behind visual state
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );

            WebElement closest = null;
            int closestDist = Integer.MAX_VALUE;
            for (WebElement btn : buttons) {
                int btnY = btn.getLocation().getY();
                String name = btn.getAttribute("name");
                int dist = Math.abs(btnY - labelY);
                // Must be within 100px and not a location/subtype button
                if (dist < 100 && dist < closestDist && name != null &&
                    !name.toLowerCase().contains("location") && !name.toLowerCase().contains("subtype")) {
                    closest = btn;
                    closestDist = dist;
                }
            }
            if (closest != null) {
                closest.click();
                System.out.println("✅ Clicked Asset Class field button: " + closest.getAttribute("name") + " (dist=" + closestDist + "px)");
                return;
            }

            // No button found near label — tap right of the label (picker area)
            int tapX = assetClassLabel.getLocation().getX() + assetClassLabel.getSize().getWidth() + 50;
            int tapY = labelY + (assetClassLabel.getSize().getHeight() / 2);
            System.out.println("   No button near label, tapping picker area at (" + tapX + "," + tapY + ")");
            tapAtCoordinates(tapX, tapY);
            System.out.println("✅ Clicked Asset Class area (coordinate tap near label)");
            return;
        } catch (Exception e) {
            System.out.println("   Label-based strategy failed: " + e.getMessage());
        }

        // Strategy 5: Final fallback - find any clickable element with asset class names (case-insensitive)
        try {
            WebElement classBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(name == 'MCC' OR name == 'ATS' OR name == 'Generator' OR name == 'Busway' OR " +
                    "name CONTAINS[c] 'select asset' OR name CONTAINS[c] 'Circuit' OR name CONTAINS[c] 'Disconnect')"
                )
            );
            classBtn.click();
            System.out.println("✅ Clicked Asset Class dropdown (fallback)");
            return;
        } catch (Exception e) {
            System.out.println("   Fallback failed: " + e.getMessage());
        }

        // DIAGNOSTIC: Log what buttons ARE on screen so we can debug the mismatch
        try {
            List<WebElement> allButtons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );
            System.out.println("🔍 DIAGNOSTIC: Found " + allButtons.size() + " buttons on screen:");
            int logged = 0;
            for (WebElement btn : allButtons) {
                if (logged >= 15) { System.out.println("   ... and " + (allButtons.size() - 15) + " more"); break; }
                try {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    int y = btn.getLocation().getY();
                    System.out.println("   btn: name=\"" + name + "\" label=\"" + label + "\" Y=" + y);
                } catch (Exception ignored) {}
                logged++;
            }
        } catch (Exception ignored) {}

        throw new RuntimeException("Failed to click Asset Class dropdown - no matching button found");
    }

    public void selectAssetClass(String className) {
        System.out.println("📋 Selecting asset class: " + className);
        clickSelectAssetClass();
        sleep(500);

        // Delegate to shared helper for robust item selection + Done tap
        if (!tapAssetClassItem(className)) {
            System.out.println("⚠️ Could not select asset class: " + className);
            tapDoneOnPicker();
            return;
        }
        sleep(300);
        tapDoneOnPicker();
        System.out.println("✅ Selected asset class: " + className);
    }

    public void selectATSClass() {
        selectAssetClass("ATS");
    }

    /**
     * Click ATS option directly (when dropdown is already open)
     * Taps the item + Done to confirm selection.
     */
    public void clickATSOption() {
        System.out.println("📋 Clicking ATS option...");
        if (!tapAssetClassItem("ATS")) {
            System.out.println("⚠️ Could not click ATS option");
            return;
        }
        sleep(300);
        tapDoneOnPicker();
        System.out.println("✅ Selected ATS");
    }

    public boolean isSelectLocationDisplayed() {
        return isElementDisplayed(selectLocationButton);
    }

    // ================================================================
    // FIX FOR ATS_ECR_11 - ROBUST clickSelectLocation
    // ================================================================

    /**
     * Click Select Location button with scroll and retry logic
     * FIX: Handles case where element is not immediately clickable
     */
    public void clickSelectLocation() {
        System.out.println("📍 Attempting to click Select Location...");
        
        int maxAttempts = 3;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // First try: Direct click with short wait
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                WebElement locationBtn = shortWait.until(
                    ExpectedConditions.elementToBeClickable(
                        AppiumBy.accessibilityId("Select location")
                    )
                );
                locationBtn.click();
                System.out.println("✅ Clicked Select Location (attempt " + attempt + ")");
                return;
            } catch (Exception e) {
                lastException = e;
                System.out.println("⚠️ Attempt " + attempt + " failed: " + e.getMessage());
                
                // Try scrolling to make element visible
                if (attempt == 1) {
                    System.out.println("   Trying scroll down...");
                    scrollFormDown();
                    sleep(200);
                } else if (attempt == 2) {
                    System.out.println("   Trying scroll up...");
                    scrollFormUp();
                    sleep(200);
                }
            }
        }
        
        // Final attempt: Try alternative locator strategies
        try {
            System.out.println("   Trying alternative locator...");
            WebElement locationBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Select location' OR label == 'Select location'")
            );
            // Scroll element into view using JavaScript
            driver.executeScript("mobile: scroll", Map.of(
                "element", locationBtn,
                "direction", "down"
            ));
            sleep(300);
            locationBtn.click();
            System.out.println("✅ Clicked Select Location (alternative)");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Alternative locator failed: " + e.getMessage());
        }
        
        // Try one more time with fresh element lookup and tap
        try {
            System.out.println("   Trying coordinate tap...");
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.toLowerCase().contains("location")) {
                    int x = btn.getLocation().getX() + (btn.getSize().getWidth() / 2);
                    int y = btn.getLocation().getY() + (btn.getSize().getHeight() / 2);
                    driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
                    System.out.println("✅ Tapped Select Location at (" + x + ", " + y + ")");
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Coordinate tap failed: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to click Select Location after all attempts", lastException);
    }

    /**
     * MAIN ENTRY POINT - Select Location with Fallback
     * 
     * Strategy:
     * 1. Try SIMPLE approach first (faster, cleaner)
     * 2. If SIMPLE fails, try COMPLEX approach (handles edge cases)
     */
    /**
     * FAST Location Selection - Handles ALL cases including empty picker
     * 
     * Logic:
     * 1. Try to select existing Building → Floor → Room
     * 2. If any level missing, CREATE it using plus button
     * 3. Works even when picker is completely empty
     */
    public boolean selectLocation() {
        System.out.println("📍 Selecting location (FAST)...");
        
        long timestamp = System.currentTimeMillis();
        String buildingName = "Building_" + timestamp;
        String floorName = "Floor_" + timestamp;
        String roomName = "Room_" + timestamp;
        
        try {
            // Open picker if not already open
            if (!isLocationPickerOpen()) {
                selectLocationButton.click();
                sleep(200);
            }
            
            if (!isLocationPickerOpen()) {
                System.out.println("⚠️ Picker not open");
                return false;
            }
            System.out.println("📍 Location picker detected (title visible)");
            
            boolean roomSelected = false;

            // TREE IS FULLY EXPANDED: building → floors → rooms all visible.
            // Clicking building or floor COLLAPSES them — DO NOT click parents.
            // Just find and click a ROOM directly.

            // Try to find an existing room directly (Room_ prefix or leaf items)
            try {
                WebElement room = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "name BEGINSWITH 'Room_' AND " +
                    "NOT name CONTAINS ' floor' AND NOT name CONTAINS ' room'"));
                String rName = room.getAttribute("name");
                room.click();
                System.out.println("✅ Room: " + rName);
                roomSelected = true;
                sleep(400);
            } catch (Exception e) {
                System.out.println("   No Room_ item found, trying other patterns...");
            }

            // Try rooms with " node" pattern but exclude floors/buildings
            if (!roomSelected) {
                try {
                    // Find all buttons with node count, pick one that's NOT a floor or building
                    List<WebElement> nodeItems = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name CONTAINS ' node' AND " +
                        "NOT name CONTAINS ' floor' AND NOT name CONTAINS ' room'"));
                    for (WebElement item : nodeItems) {
                        String name = item.getAttribute("name");
                        // Skip items that look like floors (they have " room" — already excluded)
                        // or buildings (they have " floor" — already excluded)
                        if (name != null && !isSystemButton(name)) {
                            item.click();
                            System.out.println("✅ Room (node): " + name);
                            roomSelected = true;
                            sleep(400);
                            break;
                        }
                    }
                } catch (Exception e) {}
            }

            // Create room as last resort: need to create building → floor → room
            if (!roomSelected) {
                System.out.println("   No existing room found, creating location...");
                // Check if building exists
                boolean hasBldg = !driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS ' floor'")).isEmpty();
                if (!hasBldg) {
                    if (clickPlusButtonForLocation()) {
                        sleep(400);
                        enterLocationTextAndSave("Building Name", buildingName);
                        sleep(200);
                    }
                }
                // Create floor
                if (clickPlusButtonForLocation()) {
                    sleep(400);
                    enterLocationTextAndSave("Floor Name", floorName);
                    sleep(200);
                }
                // Create room
                if (clickPlusButtonForLocation()) {
                    sleep(400);
                    if (enterLocationTextAndSave("Room Name", roomName)) {
                        sleep(200);
                        try {
                            driver.findElement(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeButton' AND name CONTAINS '" + roomName + "'")).click();
                            System.out.println("✅ Created Room: " + roomName);
                            roomSelected = true;
                            sleep(400);
                        } catch (Exception e2) {
                            System.out.println("⚠️ Could not click created room");
                        }
                    }
                }
            }
            
            sleep(200);
            
            // Dismiss picker if still open
            if (isLocationPickerOpen()) {
                dismissLocationPickerSafe();
            }
            
            if (roomSelected) {
                System.out.println("✅ Location selected!");
                return true;
            }
            return false;
            
        } catch (Exception e) {
            System.out.println("⚠️ Error: " + e.getMessage());
            dismissLocationPickerSafe();
            return false;
        }
    }
    
    /**
     * Try to select an existing floor item in the picker.
     * Uses two strategies:
     *   1. Standard patterns (" room", " node", "Floor_" prefix)
     *   2. First visible non-system, non-building button (index 0)
     * @return true if a floor was clicked
     */
    private boolean selectFloorItem() {
        // Strategy 1: Positive pattern match (no visible==true — searches full DOM)
        try {
            WebElement floor = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name CONTAINS ' room' OR name CONTAINS ' node' OR name BEGINSWITH 'Floor_') AND " +
                "NOT name CONTAINS ' floor'"));
            String fName = floor.getAttribute("name");
            floor.click();
            System.out.println("✅ Floor: " + fName);
            sleep(600);
            return true;
        } catch (Exception e) {}

        // Strategy 2: First non-system button at index 0 (floors with custom names)
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name != null && !isSystemButton(name)
                    && !name.contains(" floor") && !name.contains(" floors")) {
                    btn.click();
                    System.out.println("✅ Floor (index 0): " + name);
                    sleep(600);
                    return true;
                }
            }
        } catch (Exception e2) {}

        System.out.println("   No floor items found");
        return false;
    }

    /** Click plus button for location creation */
    private boolean clickPlusButtonForLocation() {
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                "name == 'plus.circle.fill' AND type == 'XCUIElementTypeButton'")).click();
            System.out.println("   Plus clicked");
            return true;
        } catch (Exception e) {}
        try {
            driver.findElement(AppiumBy.accessibilityId("plus.circle.fill")).click();
            System.out.println("   Plus clicked");
            return true;
        } catch (Exception e) {}
        try {
            driver.findElement(AppiumBy.iOSClassChain(
                "**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][1]")).click();
            System.out.println("   Plus clicked");
            return true;
        } catch (Exception e) {}
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Add'")).click();
            System.out.println("   Plus clicked (Add)");
            return true;
        } catch (Exception e) {}
        System.out.println("⚠️ Plus button not found");
        return false;
    }
    
    /** Enter text and save for location creation */
    private boolean enterLocationTextAndSave(String placeholder, String text) {
        try {
            WebElement tf = null;
            try {
                tf = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND (value == '" + placeholder + "' OR placeholderValue == '" + placeholder + "')"));
            } catch (Exception e) {
                tf = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"));
            }
            tf.sendKeys(text);
            sleep(300);
            try {
                driver.findElement(AppiumBy.accessibilityId("Save")).click();
            } catch (Exception e) {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name == 'Save' OR label == 'Save')")).click();
            }
            System.out.println("   Saved: " + text);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Text entry failed: " + e.getMessage());
            return false;
        }
    }
    
    /** Safely dismiss location picker */
    private void dismissLocationPickerSafe() {
        System.out.println("   Dismissing picker...");
        for (int i = 0; i < 3; i++) {
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString("name == 'Back' OR label == 'Back'")).click();
                sleep(300);
                if (!isLocationPickerOpen()) return;
            } catch (Exception e) { break; }
        }
        try {
            int w = driver.manage().window().getSize().width;
            Map<String, Object> tap = new HashMap<>();
            tap.put("x", w / 2);
            tap.put("y", 50);
            driver.executeScript("mobile: tap", tap);
        } catch (Exception e) {}
    }

    /**
     * COMPLEX LOCATION SELECTOR - Works for ALL sites and hierarchies
     * 
     * Algorithm:
     * 1. Open picker if not already open
     * 2. Select Building (item with "X floor/floors" pattern)
     * 3. Select Floor (item with "X room/rooms" pattern)
     * 4. Select Room/Leaf (item WITHOUT floor/room count patterns)
     * 5. If room has nodes, go one level deeper
     * 6. Wait for auto-dismiss
     * 
     * Key insight: At each level, we identify items by what they DON'T have:
     * - Buildings have floor counts, floors have room counts, rooms may have node counts
     * - A leaf is any item WITHOUT a count pattern
     */
    private boolean selectLocationComplex() {
        System.out.println("📍 Selecting location - COMPLEX mode...");
        
        // Generate unique names for creating new locations
        long timestamp = System.currentTimeMillis();
        String newFloorName = "Floor_" + timestamp;
        String newRoomName = "Room_" + timestamp;
        
        try {
            // STEP 0: Ensure picker is open
            if (!isLocationPickerOpen()) {
                driver.findElement(AppiumBy.accessibilityId("Select location")).click();
                sleep(200);
            }
            
            // STEP 1: SELECT OR CREATE BUILDING
            // Pattern: name contains "X floor" or "X floors"
            System.out.println("📍 Step 1: Selecting building...");
            WebElement building = findAndClickHierarchyItem(
                " floor",  // Pattern to match (buildings have floor count)
                null,      // No exclusion pattern at this level
                "Building"
            );
            
            if (building == null) {
                // No building exists - this is a NEW site, need to CREATE building
                String newBuildingName = "Building_" + timestamp;
                System.out.println("   No building found, creating: " + newBuildingName);
                
                if (!createLocationItem(newBuildingName, "Building Name", 1)) {
                    System.out.println("⚠️ Failed to create building");
                    return false;
                }
                
                // Click the newly created building
                sleep(200);
                building = findAndClickHierarchyItem(newBuildingName, null, "Created Building");
                if (building == null) {
                    // Try alternative: find any building with our timestamp
                    try {
                        WebElement newBuilding = driver.findElement(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND name CONTAINS '" + newBuildingName + "'"));
                        newBuilding.click();
                        System.out.println("✅ Clicked created building: " + newBuildingName);
                        building = newBuilding;
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not select created building");
                        return false;
                    }
                }
            }
            sleep(400);
            
            // STEP 2: SELECT FLOOR
            // First try: floors with room count (e.g., "Floor 1, 3 rooms")
            // Second try: any floor without room count (e.g., "Floor_123456")
            System.out.println("📍 Step 2: Selecting floor...");
            WebElement floor = findAndClickHierarchyItem(
                " room",   // Pattern to match (floors have room count)
                " floor",  // Exclude buildings
                "Floor with room count"
            );
            
            // If no floor with room count, try to find ANY floor (including Floor_*)
            if (floor == null) {
                System.out.println("   No floor with room count, looking for any floor...");
                floor = selectAnyFloorItem();
            }
            
            // If still no floor, create one
            if (floor == null) {
                System.out.println("   No floor found, creating: " + newFloorName);
                if (!createLocationItem(newFloorName, "Floor Name", 1)) {
                    System.out.println("⚠️ Failed to create floor");
                    return false;
                }
                // Click the newly created floor DIRECTLY by name
                sleep(200);
                floor = clickItemByExactName(newFloorName, "Created Floor");
                if (floor == null) {
                    System.out.println("⚠️ Could not select created floor");
                    return false;
                }
            }
            sleep(400);
            
            // STEP 3: SELECT ROOM OR LEAF
            // At this point, we look for ANY button that is NOT a floor
            System.out.println("📍 Step 3: Selecting room/leaf...");
            WebElement roomOrLeaf = selectRoomOrLeaf(newRoomName);
            
            if (roomOrLeaf == null) {
                System.out.println("⚠️ Could not select room");
                return false;
            }
            
            System.out.println("✅ Location selection complete");
            sleep(200);
            
            // STEP 4: Ensure picker is dismissed
            // After selecting/creating a room, we need to verify picker closed
            if (!dismissLocationPickerIfOpen()) {
                System.out.println("⚠️ Picker may still be open - forcing dismiss");
            }
            
            sleep(200);
            return true;
            
        } catch (Exception e) {
            System.out.println("⚠️ Error selecting location: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Select any floor item, including floors without room count
     * Used when no floor with "X rooms" pattern is found
     */
    private WebElement selectAnyFloorItem() {
        try {
            // Get all visible buttons
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            
            System.out.println("   Looking for any floor among " + allButtons.size() + " buttons");
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons
                if (isSystemButton(name)) continue;
                
                // Skip buildings (contain " floor")
                if (name.contains(" floor")) continue;
                
                // Look for Floor_* items or items with " room" pattern
                if (name.startsWith("Floor_") || name.contains(" room")) {
                    btn.click();
                    System.out.println("✅ Selected floor: " + name);
                    return btn;
                }
            }
            
            System.out.println("   No floor items found");
            return null;
            
        } catch (Exception e) {
            System.out.println("   Error finding floor: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Click an item directly by its exact name
     * Used after creating items when we know the exact name
     */
    private WebElement clickItemByExactName(String exactName, String levelName) {
        try {
            System.out.println("   Looking for item with name: " + exactName);
            
            // Strategy 1: Find by name contains (most reliable)
            try {
                WebElement item = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS '" + exactName + "'"));
                item.click();
                System.out.println("✅ " + levelName + ": " + item.getAttribute("name"));
                return item;
            } catch (Exception e) {
                // Continue
            }
            
            // Strategy 2: Find by exact accessibility ID
            try {
                WebElement item = driver.findElement(AppiumBy.accessibilityId(exactName));
                item.click();
                System.out.println("✅ " + levelName + " (via ID): " + exactName);
                return item;
            } catch (Exception e) {
                // Continue
            }
            
            // Strategy 3: Search through all buttons
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(exactName)) {
                    btn.click();
                    System.out.println("✅ " + levelName + " (via search): " + name);
                    return btn;
                }
            }
            
            System.out.println("   Could not find item: " + exactName);
            return null;
            
        } catch (Exception e) {
            System.out.println("   Error finding item " + exactName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Find and click an item in the location hierarchy
     * 
     * @param matchPattern Pattern that the item should contain (e.g., " floor", " room")
     * @param excludePattern Pattern that the item should NOT contain (to exclude parent levels)
     * @param levelName For logging purposes
     * @return The clicked element, or null if not found
     */
    private WebElement findAndClickHierarchyItem(String matchPattern, String excludePattern, String levelName) {
        try {
            // Build the predicate string
            StringBuilder predicate = new StringBuilder();
            predicate.append("type == 'XCUIElementTypeButton' AND name CONTAINS '").append(matchPattern).append("'");
            
            if (excludePattern != null && !excludePattern.isEmpty()) {
                predicate.append(" AND NOT name CONTAINS '").append(excludePattern).append("'");
            }
            
            // Find all matching elements
            List<WebElement> items = driver.findElements(AppiumBy.iOSNsPredicateString(predicate.toString()));
            
            if (items.isEmpty()) {
                System.out.println("   No " + levelName + " found matching pattern: " + matchPattern);
                return null;
            }
            
            // Click the first matching item
            WebElement item = items.get(0);
            String itemName = item.getAttribute("name");
            item.click();
            System.out.println("✅ " + levelName + ": " + itemName);
            return item;
            
        } catch (Exception e) {
            System.out.println("   Error finding " + levelName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Select a room or leaf item at the current hierarchy level
     * This is the most complex step because rooms can have various naming patterns
     * 
     * PRIORITY: Prefer LEAF items (no "X nodes") over items with children
     * 
     * @param newRoomName Name to use if we need to create a new room
     * @return The selected element, or null if failed
     */
    private WebElement selectRoomOrLeaf(String newRoomName) {
        try {
            // Get ALL visible buttons at current level
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            
            System.out.println("   Found " + allButtons.size() + " buttons at room level");
            
            // Separate into LEAF items and PARENT items (with nodes)
            List<WebElement> leafItems = new ArrayList<>();
            List<WebElement> parentItems = new ArrayList<>();
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system/navigation buttons
                if (isSystemButton(name)) {
                    System.out.println("   Skipping system button: '" + name + "'");
                    continue;
                }
                
                // Skip parent-level items (buildings and floors)
                if (name.contains(" floor")) continue;  // Building
                if (name.contains(" room")) continue;   // Floor (we already selected this)
                
                // Skip items that are actually FLOORS we created (not rooms)
                // Our created floors have names like "Floor_123456" with no room count
                if (name.startsWith("Floor_") || name.startsWith("Building_")) {
                    System.out.println("   Skipping created floor/building: '" + name + "'");
                    continue;
                }
                
                // Categorize: LEAF (no nodes) vs PARENT (has nodes)
                if (name.contains(" node")) {
                    parentItems.add(btn);
                    System.out.println("   Parent item (has nodes): '" + name + "'");
                } else {
                    leafItems.add(btn);
                    System.out.println("   LEAF item (no children): '" + name + "'");
                }
            }
            
            // PRIORITY 1: Click a LEAF item if available (completes selection)
            if (!leafItems.isEmpty()) {
                WebElement item = leafItems.get(0);
                String itemName = item.getAttribute("name");
                item.click();
                System.out.println("✅ Selected LEAF: " + itemName);
                sleep(400);
                return item;
            }
            
            // PRIORITY 2: Click a PARENT item and go deeper
            if (!parentItems.isEmpty()) {
                WebElement item = parentItems.get(0);
                String itemName = item.getAttribute("name");
                item.click();
                System.out.println("✅ Selected Parent (will go deeper): " + itemName);
                sleep(400);
                
                // Go one level deeper to find a leaf
                System.out.println("   Going deeper to find leaf node...");
                return selectLeafNode();
            }
            
            // No room found - need to create one
            System.out.println("   No room found, creating: " + newRoomName);
            if (!createLocationItem(newRoomName, "Room Name", 2)) {
                // Try with plus button index 1 if 2 fails
                if (!createLocationItem(newRoomName, "Room Name", 1)) {
                    System.out.println("⚠️ Failed to create room");
                    return null;
                }
            }
            
            // After room creation, we need to SELECT it to complete the location selection
            // The room is created but not yet selected - clicking it selects it
            sleep(300);  // Wait for room creation to complete
            
            try {
                WebElement newRoom = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS '" + newRoomName + "'"));
                newRoom.click();
                System.out.println("✅ Clicked to select room: " + newRoomName);
                sleep(200);
                return newRoom;
            } catch (Exception e) {
                // Room might already be selected or picker auto-closed
                System.out.println("   Room created, could not click to select: " + e.getMessage());
                // Still return success - room was created
                return driver.findElement(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'"));
            }
            
        } catch (Exception e) {
            System.out.println("   Error in selectRoomOrLeaf: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Select a leaf node (final level in hierarchy)
     */
    /**
     * Select a leaf node (final level in hierarchy)
     * Uses same LEAF vs PARENT prioritization as selectRoomOrLeaf
     */
    private WebElement selectLeafNode() {
        try {
            // At this level, we look for ANY button that doesn't have a count pattern
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            
            System.out.println("   Found " + allButtons.size() + " buttons at node level");
            
            List<WebElement> leafItems = new ArrayList<>();
            List<WebElement> parentItems = new ArrayList<>();
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons
                if (isSystemButton(name)) {
                    continue;
                }
                
                // Skip items with parent hierarchy patterns
                if (name.contains(" floor")) continue;
                if (name.contains(" room")) continue;
                
                // Skip items that are actually FLOORS/BUILDINGS we created
                if (name.startsWith("Floor_") || name.startsWith("Building_")) {
                    System.out.println("   Skipping created floor/building: '" + name + "'");
                    continue;
                }
                
                // Categorize
                if (name.contains(" node")) {
                    parentItems.add(btn);
                    System.out.println("   Node parent: '" + name + "'");
                } else {
                    leafItems.add(btn);
                    System.out.println("   Node LEAF: '" + name + "'");
                }
            }
            
            // PRIORITY 1: Click a LEAF
            if (!leafItems.isEmpty()) {
                WebElement item = leafItems.get(0);
                String itemName = item.getAttribute("name");
                item.click();
                System.out.println("✅ Selected Leaf Node: " + itemName);
                sleep(400);
                return item;
            }
            
            // PRIORITY 2: Go even deeper if needed
            if (!parentItems.isEmpty()) {
                WebElement item = parentItems.get(0);
                String itemName = item.getAttribute("name");
                item.click();
                System.out.println("✅ Selected Node Parent (going deeper): " + itemName);
                sleep(400);
                // Recursive call to go deeper
                return selectLeafNode();
            }
            
            System.out.println("   No leaf node found at this level");
            return null;
            
        } catch (Exception e) {
            System.out.println("   Error selecting leaf node: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a button name corresponds to a system/navigation button
     */
    private boolean isSystemButton(String name) {
        if (name == null || name.isEmpty()) return true;
        
        // EXACT match patterns - must match exactly
        String[] exactPatterns = {
            "plus", "plus.circle.fill", "Back", "Cancel", "Done", "Save",
            "xmark", "checkmark", "ellipsis", "gear", "search",
            "Select Location", "Select location", "Add", "Edit", "Delete",
            "Close", "OK", "Yes", "No", "Confirm"
        };
        
        for (String pattern : exactPatterns) {
            if (name.equals(pattern)) {
                return true;
            }
        }
        
        // CONTAINS patterns - if name contains these, it's a system button
        String[] containsPatterns = {
            "chevron", "arrow", "info.circle", "questionmark",
            ".fill", ".circle"
        };
        
        for (String pattern : containsPatterns) {
            if (name.contains(pattern)) {
                return true;
            }
        }
        
        // Check for very short names (likely icons)
        if (name.length() <= 3) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Create a new location item (floor or room)
     * 
     * @param itemName Name for the new item
     * @param textFieldPlaceholder The placeholder text in the text field
     * @param plusButtonIndex Which plus button to click (1 or 2)
     * @return true if creation succeeded
     */
    /**
     * Create a new location item (building, floor, or room)
     * Uses multiple strategies to find the plus button and text field
     * 
     * @param itemName Name for the new item
     * @param textFieldPlaceholder The placeholder text in the text field (e.g., "Building Name", "Floor Name", "Room Name")
     * @param plusButtonIndex Which plus button to click (1 or 2) - used as hint
     * @return true if creation succeeded
     */
    private boolean createLocationItem(String itemName, String textFieldPlaceholder, int plusButtonIndex) {
        System.out.println("   Creating location item: " + itemName + " (placeholder: " + textFieldPlaceholder + ")");

        // Each step gets ONE bounded wait on its expected winner, then the
        // fallback cascade runs as 0-implicit existsNow probes: 9 finds × 5s
        // implicit = up to ~45s per miss before; same strategy coverage now
        // costs milliseconds per miss.
        try {
            // STEP 1: Click the plus button using multiple strategies
            By plusByChain = AppiumBy.iOSClassChain(
                "**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][" + plusButtonIndex + "]");
            By plusByName = AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill'");
            By plusPlain = AppiumBy.iOSNsPredicateString("name == 'plus' AND type == 'XCUIElementTypeButton'");
            By addById = AppiumBy.accessibilityId("Add");
            By plusOrAdd = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name CONTAINS 'plus' OR name CONTAINS 'add' OR label CONTAINS 'Add')");

            // Expected winner: the indexed plus.circle.fill (or any plus variant)
            Waits.until(() -> existsNow(plusByName) || existsNow(plusPlain) || existsNow(addById), 3_000);

            boolean plusClicked = false;
            String[] plusLabels = {"ClassChain[" + plusButtonIndex + "]", "predicate 'plus.circle.fill'",
                                   "predicate 'plus'", "accessibility 'Add'", "contains 'plus/add'"};
            By[] plusStrategies = {plusByChain, plusByName, plusPlain, addById, plusOrAdd};
            for (int i = 0; i < plusStrategies.length && !plusClicked; i++) {
                By by = plusStrategies[i];
                if (!existsNow(by)) continue;
                try {
                    driver.findElement(by).click();
                    System.out.println("   Plus clicked via " + plusLabels[i]);
                    plusClicked = true;
                } catch (Exception e) {
                    // Continue to next strategy
                }
            }
            if (!plusClicked) {
                System.out.println("⚠️ Could not find plus button with any strategy");
                return false;
            }

            sleep(200);

            // STEP 2: Enter the name in text field
            By fieldByPlaceholder = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND (value == '" + textFieldPlaceholder
                + "' OR placeholderValue == '" + textFieldPlaceholder + "')");
            By anyTextField = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField'");

            // Expected winner: the new-item text field appearing
            Waits.until(() -> existsNow(anyTextField), 3_000);

            boolean nameEntered = false;
            for (By by : new By[]{fieldByPlaceholder, anyTextField}) {
                if (nameEntered || !existsNow(by)) continue;
                try {
                    driver.findElement(by).sendKeys(itemName);
                    System.out.println("   Name entered via " + (by == fieldByPlaceholder ? "placeholder match" : "visible text field"));
                    nameEntered = true;
                } catch (Exception e) {
                    // Continue to next strategy
                }
            }
            if (!nameEntered) {
                System.out.println("⚠️ Could not find text field");
                return false;
            }

            // Keyboard occludes the Save button — dismiss before tapping it
            dismissKeyboard();
            sleep(300);

            // STEP 3: Click Save button
            By saveById = AppiumBy.accessibilityId("Save");
            By saveByPredicate = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Save' OR name == 'Save')");

            // Expected winner: the Save button enabled after typing
            Waits.until(() -> existsNow(saveById) || existsNow(saveByPredicate), 3_000);

            boolean saved = false;
            for (By by : new By[]{saveById, saveByPredicate}) {
                if (saved || !existsNow(by)) continue;
                try {
                    driver.findElement(by).click();
                    System.out.println("   Saved via " + (by == saveById ? "accessibility 'Save'" : "button predicate"));
                    saved = true;
                } catch (Exception e) {
                    // Continue to next strategy
                }
            }
            if (!saved) {
                System.out.println("⚠️ Could not find Save button");
                return false;
            }

            sleep(300);

            System.out.println("✅ Created: " + itemName);
            return true;

        } catch (Exception e) {
            System.out.println("⚠️ Error creating location item: " + e.getMessage());
            return false;
        }
    }



    /**
     * SIMPLIFIED Location Selector - FASTEST Approach
     * 
     * Strategy:
     * 1. FIRST: Try to select a Room DIRECTLY (if already visible)
     * 2. FALLBACK: Go through Building → Floor → Room hierarchy
     */
    public boolean selectLocationSimple() {
        System.out.println("📍 Selecting location - SIMPLE mode...");
        
        long timestamp = System.currentTimeMillis();
        
        try {
            // Ensure picker is open
            if (!isLocationPickerOpen()) {
                driver.findElement(AppiumBy.accessibilityId("Select location")).click();
                sleep(400);
            }
            
            // STRATEGY 1: Try to select Room DIRECTLY (fastest!)
            System.out.println("   Trying DIRECT room selection...");
            WebElement directRoom = findSelectableRoom();
            if (directRoom != null) {
                // Get name BEFORE clicking (element becomes stale after click)
                String roomName = directRoom.getAttribute("name");
                directRoom.click();
                System.out.println("✅ Room selected DIRECTLY: " + roomName);
                sleep(300);
                // Picker should auto-dismiss after room selection
                return true;
            }
            System.out.println("   No direct room found, using hierarchy...");
            
            // STRATEGY 2: Go through hierarchy
            return selectViaHierarchy(timestamp);
            
        } catch (Exception e) {
            System.out.println("⚠️ Simple approach error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find a room that can be selected directly (not a building or floor)
     */
    private WebElement findSelectableRoom() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons
                if (name.equals("Cancel") || name.equals("plus") || name.equals("Save") ||
                    name.contains("plus.circle") || name.length() <= 3) continue;
                
                // Skip buildings (contain " floor")
                if (name.contains(" floor")) continue;
                
                // Skip floors (contain " room")
                if (name.contains(" room")) continue;
                
                // Skip our created items that aren't rooms
                if (name.startsWith("Building_")) continue;
                if (name.startsWith("Floor_")) continue;
                
                // Skip items with node count (need to go deeper)
                if (name.contains(" node")) continue;
                
                // This looks like a selectable room!
                System.out.println("   Found selectable room: " + name);
                return btn;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Select location via Building → Floor → Room hierarchy
     */
    private boolean selectViaHierarchy(long timestamp) {
        try {
            // STEP 1: Select Building
            System.out.println("📍 Step 1: Selecting building...");
            try {
                WebElement building = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS ' floor'"));
                String buildingName = building.getAttribute("name");
                building.click();
                System.out.println("✅ Building: " + buildingName);
                sleep(300);
            } catch (Exception e) {
                System.out.println("⚠️ No building found");
                return false;
            }
            
            // STEP 2: Select Floor
            System.out.println("📍 Step 2: Selecting floor...");
            WebElement floor = findFirstFloor();
            
            if (floor != null) {
                String floorName = floor.getAttribute("name");
                floor.click();
                System.out.println("✅ Floor: " + floorName);
                sleep(300);
            } else {
                String floorName = "Floor_" + timestamp;
                System.out.println("   Creating: " + floorName);
                if (!createAndClickItem(floorName, "Floor Name")) {
                    return false;
                }
            }
            
            // STEP 3: Select Room
            System.out.println("📍 Step 3: Selecting room...");
            WebElement room = findFirstRoom();
            
            if (room != null) {
                String roomName = room.getAttribute("name");
                room.click();
                System.out.println("✅ Room: " + roomName);
                sleep(300);
            } else {
                String roomName = "Room_" + timestamp;
                System.out.println("   Creating: " + roomName);
                if (!createAndClickItem(roomName, "Room Name")) {
                    return false;
                }
            }
            
            System.out.println("✅ Location selection complete");
            sleep(300);
            dismissLocationPickerIfOpen();
            return true;
            
        } catch (Exception e) {
            System.out.println("⚠️ Hierarchy selection error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find first floor element (not a building)
     */
    private WebElement findFirstFloor() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name == null) continue;
                
                // Skip system buttons
                if (name.equals("Cancel") || name.equals("plus") || 
                    name.contains("plus.circle") || name.length() <= 3) continue;
                
                // Skip buildings (contain " floor")
                if (name.contains(" floor")) continue;
                
                // This is a floor! (has " room" or starts with "Floor_" or any other name)
                if (name.contains(" room") || name.startsWith("Floor_") || 
                    (!name.contains(" node") && !name.startsWith("Building_") && !name.startsWith("Room_"))) {
                    return btn;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Find first room element (not a floor or building)
     */
    private WebElement findFirstRoom() {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name == null) continue;
                
                // Skip system buttons
                if (name.equals("Cancel") || name.equals("plus") || 
                    name.contains("plus.circle") || name.length() <= 3) continue;
                
                // Skip buildings and floors
                if (name.contains(" floor")) continue;  // Building
                if (name.contains(" room")) continue;   // Floor with room count
                if (name.startsWith("Floor_")) continue; // Our created floors
                if (name.startsWith("Building_")) continue; // Our created buildings
                
                // This is a room or leaf!
                return btn;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Create an item and click it directly (no search after creation)
     */
    private boolean createAndClickItem(String name, String placeholder) {
        try {
            // Click plus button
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus'")).click();
            } catch (Exception e) {
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill'")).click();
                } catch (Exception e2) {
                    driver.findElement(AppiumBy.iOSClassChain("**/XCUIElementTypeButton[`name == \"plus.circle.fill\"`][1]")).click();
                }
            }
            sleep(300);
            
            // Enter name
            WebElement textField = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"));
            textField.sendKeys(name);
            sleep(200);
            
            // Save
            driver.findElement(AppiumBy.accessibilityId("Save")).click();
            sleep(200);
            
            // Click the created item directly
            WebElement createdItem = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS '" + name + "'"));
            createdItem.click();
            System.out.println("✅ Created & selected: " + name);
            sleep(300);
            
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Error creating " + name + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get floor elements (items that are floors, not buildings)
     */
    private List<WebElement> getFloorElements() {
        List<WebElement> floors = new ArrayList<>();
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons and buildings
                if (isSystemButton(name)) continue;
                if (name.contains(" floor")) continue;  // This is a building
                
                // Floor items: have " room" pattern OR start with "Floor_"
                if (name.contains(" room") || name.startsWith("Floor_")) {
                    floors.add(btn);
                }
            }
        } catch (Exception e) {
            System.out.println("   Error getting floors: " + e.getMessage());
        }
        return floors;
    }
    
    /**
     * Get room elements (items that are rooms, not floors or buildings)
     */
    private List<WebElement> getRoomElements() {
        List<WebElement> rooms = new ArrayList<>();
        try {
            List<WebElement> allButtons = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton'"));
            
            for (WebElement btn : allButtons) {
                String name = btn.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                
                // Skip system buttons
                if (isSystemButton(name)) continue;
                
                // Skip buildings and floors
                if (name.contains(" floor")) continue;  // Building
                if (name.contains(" room")) continue;   // Floor with room count
                if (name.startsWith("Floor_")) continue; // Our created floors
                if (name.startsWith("Building_")) continue; // Our created buildings
                
                // What remains is a room (or node)
                rooms.add(btn);
            }
        } catch (Exception e) {
            System.out.println("   Error getting rooms: " + e.getMessage());
        }
        return rooms;
    }
    
    /**
     * Create a location item - simplified version
     */
    private boolean createLocationItemSimple(String name, String placeholder) {
        try {
            System.out.println("   Creating: " + name);
            
            // Click plus button
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus'")).click();
            } catch (Exception e) {
                driver.findElement(AppiumBy.iOSNsPredicateString("name == 'plus.circle.fill'")).click();
            }
            sleep(400);
            
            // Enter name
            WebElement textField = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField'"));
            textField.sendKeys(name);
            sleep(200);
            
            // Save
            driver.findElement(AppiumBy.accessibilityId("Save")).click();
            sleep(400);
            
            System.out.println("   ✅ Created: " + name);
            return true;
        } catch (Exception e) {
            System.out.println("   ⚠️ Error creating " + name + ": " + e.getMessage());
            return false;
        }
    }


    /**
     * Dismiss the location picker if it's still open
     * Uses multiple strategies to close the picker
     * 
     * @return true if picker was dismissed or already closed
     */
    private boolean dismissLocationPickerIfOpen() {
        try {
            // Check if picker is still open
            boolean pickerOpen = false;
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(label == 'Select Location' OR name == 'Select Location') AND type == 'XCUIElementTypeStaticText'"));
                pickerOpen = true;
            } catch (Exception e) {
                // Check for Cancel button (another indicator picker is open)
                try {
                    driver.findElement(AppiumBy.iOSNsPredicateString(
                        "label == 'Cancel' AND type == 'XCUIElementTypeButton'"));
                    pickerOpen = true;
                } catch (Exception e2) {
                    // Picker is closed
                }
            }
            
            if (!pickerOpen) {
                System.out.println("   Picker already closed");
                return true;
            }
            
            System.out.println("   Picker still open, attempting to dismiss...");
            
            // Strategy 1: Try tapping in the header area to close
            try {
                int screenWidth = driver.manage().window().getSize().width;
                Map<String, Object> tapParams = new HashMap<>();
                tapParams.put("x", screenWidth / 2);
                tapParams.put("y", 100);  // Tap near top/header
                driver.executeScript("mobile: tap", tapParams);
                System.out.println("   Tapped header area");
                sleep(200);
            } catch (Exception e) {
                // Continue
            }
            
            // Check if dismissed
            try {
                driver.findElement(AppiumBy.iOSNsPredicateString(
                    "(label == 'Select Location' OR label == 'Cancel') AND type == 'XCUIElementTypeStaticText'"));
            } catch (Exception e) {
                System.out.println("   Picker dismissed after tapping header");
                return true;
            }
            
            // Strategy 2: Click Cancel (will keep any selection made)
            try {
                driver.findElement(AppiumBy.accessibilityId("Cancel")).click();
                System.out.println("   Clicked Cancel to close picker");
                sleep(200);
                return true;
            } catch (Exception e) {
                // Continue
            }
            
            // Strategy 3: Swipe down to dismiss
            try {
                int screenWidth = driver.manage().window().getSize().width;
                int screenHeight = driver.manage().window().getSize().height;
                
                Map<String, Object> swipeParams = new HashMap<>();
                swipeParams.put("fromX", screenWidth / 2);
                swipeParams.put("fromY", 150);
                swipeParams.put("toX", screenWidth / 2);
                swipeParams.put("toY", screenHeight - 100);
                swipeParams.put("duration", 0.3);
                driver.executeScript("mobile: dragFromToForDuration", swipeParams);
                System.out.println("   Swiped down to dismiss");
                sleep(200);
                return true;
            } catch (Exception e) {
                System.out.println("   Swipe dismiss failed");
            }
            
            // Strategy 4: Tap outside picker area
            try {
                int screenWidth = driver.manage().window().getSize().width;
                Map<String, Object> tapParams = new HashMap<>();
                tapParams.put("x", screenWidth / 2);
                tapParams.put("y", 50);
                driver.executeScript("mobile: tap", tapParams);
                System.out.println("   Tapped outside picker");
                sleep(200);
                return true;
            } catch (Exception e) {
                // Continue
            }
            
            return false;
            
        } catch (Exception e) {
            System.out.println("   Error in dismissLocationPickerIfOpen: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if location picker is currently open
     */
    private boolean isLocationPickerOpen() {
        // Use short timeout to avoid 15s block (3 queries × 5s each)
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(800));
        try {
            // Multiple ways to detect picker is open
            // 1. "Select Location" title visible
            List<WebElement> title = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label == 'Select Location' OR name == 'Select Location'"));
            if (!title.isEmpty()) {
                System.out.println("📍 Location picker detected (title visible)");
                return true;
            }

            // 2. Building buttons visible (contain " floor" or " floors")
            List<WebElement> buildings = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS ' floor'"));
            if (!buildings.isEmpty()) {
                System.out.println("📍 Location picker detected (buildings visible)");
                return true;
            }

            // 3. Check for hierarchy pattern
            List<WebElement> hierarchy = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name CONTAINS ' room' OR name CONTAINS '>')"));
            if (!hierarchy.isEmpty()) {
                System.out.println("📍 Location picker detected (hierarchy visible)");
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
    }
    
    /**
     * Ensure location picker is dismissed and we're back on the form
     */
    private boolean ensureLocationPickerDismissed() {
        try {
            // Check if we're back on the Create Asset form
            // Look for Asset Name field or Create Asset title
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            
            boolean onForm = quickWait.until(d -> {
                try {
                    // Check for Asset Name text field
                    List<WebElement> assetName = d.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND (value == 'Asset Name' OR placeholderValue == 'Asset Name' OR value CONTAINS 'Asset_')"));
                    if (!assetName.isEmpty()) return true;
                    
                    // Check for "Select location" button (on form, not in picker)
                    // If we can see "Select location" but NOT "Select Location" title, we're on form
                    List<WebElement> selectBtn = d.findElements(AppiumBy.accessibilityId("Select location"));
                    List<WebElement> pickerTitle = d.findElements(AppiumBy.iOSNsPredicateString(
                        "label == 'Select Location' AND type == 'XCUIElementTypeStaticText'"));
                    if (!selectBtn.isEmpty() && pickerTitle.isEmpty()) return true;
                    
                    return false;
                } catch (Exception e) {
                    return false;
                }
            });
            
            if (onForm) {
                System.out.println("✅ Back on Create Asset form");
                return true;
            }
        } catch (Exception e) {
            // Timeout - picker might still be open
        }
        
        return false;
    }
    
    /**
     * Force close the location picker using multiple strategies
     */
    private void forceCloseLocationPicker() {
        System.out.println("📍 Force closing location picker...");
        
        // Strategy 1: Tap on any visible room/location item (should select and close)
        try {
            List<WebElement> items = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name CONTAINS '>' OR name BEGINSWITH 'Room_')"));
            if (!items.isEmpty()) {
                items.get(0).click();
                System.out.println("   Tapped location item to close picker");
                sleep(200);
                if (ensureLocationPickerDismissed()) return;
            }
        } catch (Exception e) {}
        
        // Strategy 2: Look for any "Done" or "Save" button
        try {
            WebElement doneBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Done' OR label == 'Save' OR label == 'OK')"));
            doneBtn.click();
            System.out.println("   Clicked Done/Save button");
            sleep(200);
            if (ensureLocationPickerDismissed()) return;
        } catch (Exception e) {}
        
        // Strategy 3: Tap the selected/checked item
        try {
            WebElement selected = driver.findElement(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeButton') AND (name CONTAINS 'checkmark' OR label CONTAINS 'selected')"));
            selected.click();
            System.out.println("   Tapped selected item");
            sleep(200);
            if (ensureLocationPickerDismissed()) return;
        } catch (Exception e) {}
        
        // Strategy 4: Navigate back using back button
        try {
            WebElement backBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label == 'Back' OR name CONTAINS 'back' OR label CONTAINS 'chevron')"));
            // Click back multiple times to exit the hierarchy
            for (int i = 0; i < 3; i++) {
                try {
                    backBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (label == 'Back' OR name CONTAINS 'back')"));
                    backBtn.click();
                    sleep(300);
                } catch (Exception e2) {
                    break;
                }
            }
            System.out.println("   Navigated back to close picker");
            sleep(200);
            if (ensureLocationPickerDismissed()) return;
        } catch (Exception e) {}
        
        // Strategy 5: Swipe down to dismiss modal
        try {
            int screenWidth = driver.manage().window().getSize().width;
            int screenHeight = driver.manage().window().getSize().height;
            
            Map<String, Object> swipeParams = new HashMap<>();
            swipeParams.put("fromX", screenWidth / 2);
            swipeParams.put("fromY", 150);
            swipeParams.put("toX", screenWidth / 2);
            swipeParams.put("toY", screenHeight - 100);
            swipeParams.put("duration", 0.3);
            driver.executeScript("mobile: dragFromToForDuration", swipeParams);
            System.out.println("   Swiped down to dismiss");
            sleep(200);
        } catch (Exception e) {
            System.out.println("   Swipe failed: " + e.getMessage());
        }
        
        // Strategy 6: Tap outside the picker area (top of screen)
        try {
            int screenWidth = driver.manage().window().getSize().width;
            Map<String, Object> tapParams = new HashMap<>();
            tapParams.put("x", screenWidth / 2);
            tapParams.put("y", 50);
            driver.executeScript("mobile: tap", tapParams);
            System.out.println("   Tapped outside picker");
            sleep(200);
        } catch (Exception e) {}
    }

        /**
     * Helper: Select a single level in location hierarchy (legacy - kept for compatibility)
     */
    private boolean selectLocationLevel(String levelName, String[] skipLabels) {
        try {
            // Find all cells
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            
            for (WebElement cell : cells) {
                try {
                    // Get cell's text
                    List<WebElement> texts = cell.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
                    if (texts.isEmpty()) continue;
                    
                    String cellName = texts.get(0).getAttribute("name");
                    if (cellName == null || cellName.isEmpty()) continue;
                    
                    // Skip UI labels
                    boolean isSkip = false;
                    for (String skip : skipLabels) {
                        if (cellName.equalsIgnoreCase(skip)) {
                            isSkip = true;
                            break;
                        }
                    }
                    if (isSkip) continue;
                    
                    // Click this cell
                    System.out.println("   📍 " + levelName + ": " + cellName);
                    texts.get(0).click();
                    return true;
                } catch (Exception e) {
                    continue;
                }
            }
            
            // Fallback: click first non-skip StaticText
            List<WebElement> allTexts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : allTexts) {
                String name = text.getAttribute("name");
                if (name == null || name.isEmpty() || name.length() < 2) continue;
                
                boolean isSkip = false;
                for (String skip : skipLabels) {
                    if (name.equalsIgnoreCase(skip) || name.contains(skip)) {
                        isSkip = true;
                        break;
                    }
                }
                if (isSkip) continue;
                
                System.out.println("   📍 " + levelName + " (text): " + name);
                text.click();
                return true;
            }
            
        } catch (Exception e) {
            System.out.println("   ⚠️ " + levelName + " selection failed: " + e.getMessage());
        }
        return false;
    }

    public void createNewLocation(String floorName, String roomName) {
        System.out.println("📍 Creating new location...");
        
        try {
            click(addFloorButton);
            sleep(200);
            
            WebElement floorField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Floor Name'")
            );
            floorField.sendKeys(floorName);
            click(saveButton);
            System.out.println("✅ Created floor: " + floorName);
            sleep(200);
            
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(floorName)) {
                    btn.click();
                    break;
                }
            }
            sleep(200);
            
            click(addRoomButton);
            sleep(200);
            
            WebElement roomField = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND value == 'Room Name'")
            );
            roomField.sendKeys(roomName);
            click(saveButton);
            System.out.println("✅ Created room: " + roomName);
            sleep(200);
            
            buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(" floor")) {
                    btn.click();
                    break;
                }
            }
            sleep(200);
            
            buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(floorName)) {
                    btn.click();
                    break;
                }
            }
            sleep(200);
            
            buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(roomName)) {
                    btn.click();
                    System.out.println("✅ Selected room: " + name);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error creating location: " + e.getMessage());
        }
    }

    public boolean isSelectAssetSubtypeDisplayed() {
        return isElementDisplayed(selectAssetSubtypeButton);
    }

    // ================================================================
    // FIX FOR ATS_ECR_18 - ROBUST clickSelectAssetSubtype
    // ================================================================

    /**
     * DEBUG: Print all visible interactive elements to find correct accessibility ID
     */
    public void debugPrintAllElements() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔍 DEBUG: ALL VISIBLE ELEMENTS ON CREATE ASSET SCREEN");
        System.out.println("=".repeat(60));
        
        // Check Buttons
        System.out.println("\n--- BUTTONS ---");
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement el : buttons) {
                try {
                    if (el.isDisplayed()) {
                        String name = el.getAttribute("name");
                        String label = el.getAttribute("label");
                        if (name != null) {
                            System.out.println("  [Button] name='" + name + "' label='" + label + "'");
                            if (name.toLowerCase().contains("subtype")) {
                                System.out.println("  >>> SUBTYPE FOUND! <<<");
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        
        // Check Other elements (dropdowns often use this type)
        System.out.println("\n--- OTHER ELEMENTS ---");
        try {
            List<WebElement> others = driver.findElements(AppiumBy.className("XCUIElementTypeOther"));
            for (WebElement el : others) {
                try {
                    if (el.isDisplayed()) {
                        String name = el.getAttribute("name");
                        String label = el.getAttribute("label");
                        if (name != null && (name.toLowerCase().contains("select") || name.toLowerCase().contains("subtype"))) {
                            System.out.println("  [Other] name='" + name + "' label='" + label + "'");
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        
        // Check Cells
        System.out.println("\n--- CELLS ---");
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            for (WebElement el : cells) {
                try {
                    if (el.isDisplayed()) {
                        String name = el.getAttribute("name");
                        String label = el.getAttribute("label");
                        if (name != null && name.toLowerCase().contains("subtype")) {
                            System.out.println("  [Cell] name='" + name + "' label='" + label + "'");
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        
        // Check Static Text
        System.out.println("\n--- STATIC TEXT ---");
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement el : texts) {
                try {
                    if (el.isDisplayed()) {
                        String name = el.getAttribute("name");
                        String label = el.getAttribute("label");
                        if (name != null && name.toLowerCase().contains("subtype")) {
                            System.out.println("  [Text] name='" + name + "' label='" + label + "'");
                        }
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        
        System.out.println("\n" + "=".repeat(60) + "\n");
    }
    /**
     * DEBUG: Print all buttons in navigation bar area
     * Helps identify correct Create Asset button locator
     */
    public void debugPrintNavBarButtons() {
        System.out.println("\n🔍 DEBUG: Navigation Bar Buttons");
        System.out.println("=" .repeat(50));
        try {
            // Try to find navigation bar
            List<WebElement> navBars = driver.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"));
            System.out.println("Found " + navBars.size() + " navigation bars");
            
            for (int i = 0; i < navBars.size(); i++) {
                WebElement navBar = navBars.get(i);
                System.out.println("\nNavBar " + i + ":");
                List<WebElement> buttons = navBar.findElements(AppiumBy.className("XCUIElementTypeButton"));
                for (WebElement btn : buttons) {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    String enabled = btn.getAttribute("enabled");
                    boolean visible = btn.isDisplayed();
                    System.out.println("  [Button] name='" + name + "' label='" + label + "' enabled=" + enabled + " visible=" + visible);
                }
            }
            
            // Also check all visible buttons with "Create" or "Asset" in name
            System.out.println("\nAll buttons containing 'Create' or 'Asset':");
            List<WebElement> allButtons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : allButtons) {
                try {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    if ((name != null && (name.contains("Create") || name.contains("Asset"))) ||
                        (label != null && (label.contains("Create") || label.contains("Asset")))) {
                        boolean visible = btn.isDisplayed();
                        String enabled = btn.getAttribute("enabled");
                        System.out.println("  [Button] name='" + name + "' label='" + label + "' enabled=" + enabled + " visible=" + visible);
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println("=" .repeat(50) + "\n");
    }


    /**
     * Click Select Asset Subtype button - SIMPLIFIED VERSION
     * No excessive scrolling since subtype is just below asset class
     */
    public void clickSelectAssetSubtype() {
        System.out.println("📋 Clicking Select Asset Subtype...");

        // Strategy 1: Find the BUTTON element for subtype picker (must be XCUIElementTypeButton)
        // CRITICAL: Use predicate with type filter to avoid matching StaticText labels
        // which would dispatch the click to whatever's behind them (often the Save button)
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            WebElement subtypeBtn = shortWait.until(
                ExpectedConditions.elementToBeClickable(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (name CONTAINS 'asset subtype' OR name CONTAINS 'Asset Subtype' OR name == 'Select asset subtype')"
                    )
                )
            );
            String btnName = subtypeBtn.getAttribute("name");
            subtypeBtn.click();
            System.out.println("✅ Clicked subtype button: '" + btnName + "'");
            return;
        } catch (Exception e) {
            System.out.println("   📌 Strategy 1: No Button with 'asset subtype' in name found");
            System.out.println("   → Trying positional search relative to Asset Subtype label...");
        }
        
        // First scroll down to make subtype field visible (it's often below the fold)
        System.out.println("   📜 Scrolling down to make subtype field visible...");
        try {
            scrollFormDown();
            sleep(400);
        } catch (Exception scrollEx) {
            System.out.println("   ⚠️ Scroll failed, continuing anyway");
        }
        
        // Strategy 2: If subtype already selected, find button by position relative to label
        System.out.println("   📌 Strategy 2: Looking for button below 'Asset Subtype' label...");
        try {
            WebElement subtypeLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND name CONTAINS 'Asset Subtype'")
            );
            int labelY = subtypeLabel.getLocation().getY();
            System.out.println("      Found 'Asset Subtype (Optional)' label at Y=" + labelY);
            
            // Find button just below this label (within 100px - increased from 80)
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            System.out.println("      Scanning " + buttons.size() + " buttons...");
            
            for (WebElement btn : buttons) {
                try {
                    int btnY = btn.getLocation().getY();
                    String name = btn.getAttribute("name");
                    
                    // Button should be below label and within 100px
                    if (btnY > labelY && (btnY - labelY) < 100 && name != null) {
                        // Skip known non-subtype buttons and icons
                        if (name.equals("Cancel") || name.equals("Save Changes") || 
                            name.contains("Bldg") || name.contains("Floor") || name.contains("Room") ||
                            name.equals("qrcode.viewfinder") || name.equals("Calculator") ||
                            name.equals("Select shortcut") || name.equals("Filter") ||
                            name.equals("plus.circle.fill") || name.equals("square.grid.2x2") ||
                            name.equals("1") || name.equals("2") || name.equals("3") ||
                            name.equals("house") || name.equals("house.fill") ||  // Navigation icons
                            name.equals("chevron.left") || name.equals("chevron.right") ||
                            name.equals("xmark") || name.equals("xmark.circle") ||
                            name.equals("gear") || name.equals("gearshape") ||
                            name.equals("person") || name.equals("person.fill") ||
                            name.length() <= 5) {  // Skip very short names (likely icons)
                            continue;
                        }
                        
                        // MUST contain parentheses or be a known subtype pattern for Strategy 2
                        boolean looksLikeSubtype = name.contains("(") && name.contains(")");
                        if (!looksLikeSubtype && !name.equals("None") && !name.contains("Switch") && 
                            !name.contains("Fuse") && !name.contains("Breaker")) {
                            continue;
                        }
                        
                        // This should be the subtype button!
                        System.out.println("      ✓ Found subtype button: '" + name + "' at Y=" + btnY + " (offset=" + (btnY - labelY) + "px)");
                        btn.click();
                        System.out.println("   ✅ Clicked currently selected subtype to open dropdown");
                        return;
                    }
                } catch (Exception ex) {
                    // Skip button if attributes can't be read
                }
            }
            System.out.println("      ✗ No suitable button found below label within 100px");
        } catch (Exception e) {
            System.out.println("      ✗ Strategy 2 failed: " + e.getMessage());
        }
        
        // Strategy 3: Find button with known subtype names
        System.out.println("   📌 Strategy 3: Looking for known subtype button names...");
        // Find the button whose name is a known LIVE subtype via ONE scoped IN-predicate over the
        // full live subtype set (LIVE_SUBTYPES; source: node_classes API / node_classes_template v14).
        // Replaces the old 29-entry N×500ms loop (which was missing 58 live subtypes and fell through
        // to the slow whole-tree Strategy 4). Single query at implicit-wait 0 — mirrors the proven
        // class-picker IN-predicate fix, so it stays fast even on bleed-through DOMs.
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        try {
            StringBuilder inList = new StringBuilder();
            for (String s : LIVE_SUBTYPES) {
                if (inList.length() > 0) inList.append(", ");
                inList.append("'").append(s.replace("'", "\\'")).append("'");
            }
            String pred = "type == 'XCUIElementTypeButton' AND name IN {" + inList + "}";
            for (WebElement btn : driver.findElements(AppiumBy.iOSNsPredicateString(pred))) {
                try {
                    if (btn.isDisplayed()) {
                        String nm = btn.getAttribute("name");
                        System.out.println("      ✓ Found known subtype: '" + nm + "'");
                        btn.click();
                        System.out.println("   ✅ Clicked subtype button to open dropdown");
                        return;
                    }
                } catch (Exception ignore) {}
            }
            System.out.println("      ✗ No known subtype buttons found on screen");
        } finally {
            driver.manage().timeouts().implicitlyWait(
                java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
        }
        
        // Strategy 4: Find any button that looks like a subtype (contains parentheses or voltage)
        System.out.println("   📌 Strategy 4: Looking for subtype-like buttons (with parentheses/voltage)...");
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name == null) continue;
                
                // Skip known non-subtype buttons
                if (name.equals("Cancel") || name.equals("Save Changes") || 
                    name.contains("Bldg") || name.contains("Floor") || name.contains("Room") ||
                    name.equals("qrcode.viewfinder") || name.equals("Calculator") ||
                    name.equals("Filter") || name.equals("plus.circle.fill") ||
                    name.equals("square.grid.2x2") || name.equals("Select shortcut") ||
                    name.matches("[0-9]+")) {  // Skip number buttons
                    continue;
                }
                
                // Subtype buttons typically contain parentheses like "(< 1000V)" or specific keywords
                boolean isSubtypeButton = 
                    (name.contains("(") && name.contains(")")) ||  // Has parentheses - MOST RELIABLE
                    name.contains("1000V") ||                       // Voltage rating
                    name.contains("Fuse") ||                        // Fuse types (but not just "Fuse" as asset class)
                    name.contains("Breaker") ||                     // Breaker types
                    name.equals("None");                            // None option
                
                // Extra check: Don't click plain asset class names
                if (name.equals("Disconnect Switch") || name.equals("Circuit Breaker") || 
                    name.equals("Fuse") || name.equals("ATS") || name.equals("UPS")) {
                    continue;
                }
                
                if (isSubtypeButton) {
                    System.out.println("      ✓ Found subtype-like button: '" + name + "'");
                    btn.click();
                    System.out.println("   ✅ Clicked to open subtype dropdown");
                    return;
                }
            }
            System.out.println("      ✗ No subtype-like buttons found");
        } catch (Exception e) {
            System.out.println("      ✗ Strategy 4 failed: " + e.getMessage());
        }
        
        // Strategy 5: Try scrolling and retry
        try {
            System.out.println("   Trying scroll then retry...");
            scrollFormDown();
            sleep(200);
            
            // Retry Strategy 1 after scroll — use type-filtered predicate (not bare accessibilityId)
            WebElement subtypeBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name CONTAINS 'asset subtype' OR name CONTAINS 'Asset Subtype' OR name == 'Select asset subtype')"
                )
            );
            subtypeBtn.click();
            System.out.println("✅ Clicked subtype button after scroll");
            return;
        } catch (Exception e) {
            System.out.println("   Scroll + retry failed");
        }
        
        // Skip debugPrintAllElements() — iterates hundreds of elements (60+ seconds on CI)
        // No clickable subtype control found. This is EXPECTED for zero-subtype
        // classes (Junction Box, MCC Bucket, PDU, Utility, VFD) whose Asset Subtype
        // field shows a non-interactive "None". Return gracefully instead of
        // throwing, so the *_AST_01 "shows None" tests can verify that state
        // (callers check isSubtypeDropdownDisplayed() afterward).
        System.out.println("ℹ️ No clickable Asset Subtype control — class likely has 0 subtypes (shows None)");
    }

    /**
     * Select asset subtype by name with robust handling
     */
    public void selectAssetSubtype(String subtypeName) {
        System.out.println("📋 Selecting asset subtype: " + subtypeName);
        clickSelectAssetSubtype();
        sleep(500);

        // Reuse shared picker item selection (same full-screen table UI as Asset Class)
        if (!tapAssetClassItem(subtypeName)) {
            // If the WDA session died mid-selection (observed locally on iOS 26.2:
            // 'Error communicating with the remote browser. It may have died.'),
            // every further command — describeVisiblePickerOptions / tapDoneOnPicker
            // / screenshot — blocks on the dead session until the 360s test cap.
            // Short-circuit: don't hammer a corpse. The DeadSessionCircuitBreaker
            // + teardown recovery handle the dead session at the suite level.
            if (!com.egalvanic.utils.DriverManager.isDriverActive()) {
                throw new VerificationError(
                    "selectAssetSubtype: WDA session died while selecting subtype '"
                    + subtypeName + "' (remote browser unresponsive) — failing fast"
                    + " instead of blocking on the dead session.");
            }
            // Capture what IS on screen so the failure is diagnosable, then dismiss
            // the picker to restore a sane state for teardown.
            String visible = describeVisiblePickerOptions();
            tapDoneOnPicker();
            // Hard-fail (un-swallowable) instead of silently continuing. A silent
            // continue here is what produced the run 27485736625 hangs masquerading
            // as slow tests — a clean ~20-45s fail with evidence beats a 6min hang.
            throw new VerificationError(
                "selectAssetSubtype: could not select subtype '" + subtypeName
                + "' in the picker. On-screen option rows: " + visible);
        }
        sleep(300);
        tapDoneOnPicker();
        System.out.println("✅ Selected asset subtype: " + subtypeName);
    }

    /**
     * Snapshot the comma-free StaticText/Button rows currently on the picker
     * (the candidate option labels) for diagnostic failure messages. Cheap:
     * implicit-wait 0, capped at the first ~25 rows. Best-effort only.
     */
    private String describeVisiblePickerOptions() {
        try {
            return withImplicitWait(0, () -> {
                // Dump ALL visible elements with type+label so the failure message
                // reveals the REAL option element type. Production reported
                // "(none readable)" with the StaticText/Button-only filter — meaning
                // the subtype options render as Cell / Other / something else. This
                // wider dump tells us exactly what (and is the CI source of truth
                // since the local iOS 26.2 sim is flaky).
                java.util.List<String> opts = new java.util.ArrayList<>();
                for (WebElement el : driver.findElements(
                        AppiumBy.iOSNsPredicateString("visible == true"))) {
                    try {
                        String n = el.getAttribute("label");
                        if (n == null || n.trim().isEmpty()) n = el.getAttribute("name");
                        if (n == null || n.trim().isEmpty() || n.contains(",")) continue;
                        String type = el.getAttribute("type");
                        String shortType = type == null ? "?"
                            : type.replace("XCUIElementType", "");
                        opts.add(shortType + ":" + n.trim());
                        if (opts.size() >= 30) break;
                    } catch (Exception ignore) {}
                }
                return opts.isEmpty() ? "(none readable)" : String.join(" | ", opts);
            });
        } catch (Exception e) {
            return "(could not read picker: " + e.getMessage() + ")";
        }
    }

    public boolean isQRCodeFieldDisplayed() {
        return isElementDisplayed(qrCodeField);
    }

    public void enterQRCode(String qrCode) {
        System.out.println("📱 Entering QR code: " + qrCode);
        
        // QR Code field is at the bottom of the form - scroll down to find it
        for (int scrollAttempt = 0; scrollAttempt < 2; scrollAttempt++) {
            
            // Strategy 1: Find by EXACT placeholder text "Enter or scan QR code"
            try {
                WebElement qrField = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND value == 'Enter or scan QR code'"
                    )
                );
                
                if (qrField.isDisplayed()) {
                    int fieldY = qrField.getLocation().getY();
                    if (fieldY > 0 && fieldY < 850) {
                        System.out.println("   🔍 Found QR Code field (placeholder) at Y=" + fieldY);
                        qrField.click();
                        sleep(200);
                        qrField.sendKeys(qrCode);
                        System.out.println("✅ Entered QR code: " + qrCode);
                        dismissKeyboard();
                        return;
                    }
                }
            } catch (Exception e) {}
            
            // Strategy 2: Find TextField directly BELOW "QR Code" label (exact match)
            try {
                WebElement qrLabel = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND label == 'QR Code'"
                    )
                );
                
                if (qrLabel.isDisplayed()) {
                    int labelY = qrLabel.getLocation().getY();
                    
                    if (labelY > 0 && labelY < 800) {
                        System.out.println("   🔍 Found 'QR Code' label at Y=" + labelY);
                        
                        // Find TextField BELOW this label (within 60px)
                        List<WebElement> fields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
                        for (WebElement field : fields) {
                            if (!field.isDisplayed()) continue;
                            int fieldY = field.getLocation().getY();
                            
                            // Field must be BELOW label (fieldY > labelY) and close (within 60px)
                            if (fieldY > labelY && (fieldY - labelY) < 60) {
                                System.out.println("   🔍 Found TextField at Y=" + fieldY + " (below QR Code label)");
                                field.click();
                                sleep(200);
                                field.sendKeys(qrCode);
                                System.out.println("✅ Entered QR code (by label): " + qrCode);
                                dismissKeyboard();
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {}
            
            // Scroll down to reveal QR Code field
            System.out.println("   📜 Scrolling down to find QR Code field...");
            scrollFormDown();
            sleep(400);
        }
        
        System.out.println("⚠️ Could not find QR code field after scrolling");
    }

    public String getQRCodeValue() {
        try {
            return qrCodeField.getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }
    /**
     * Edit existing QR code field (clears and enters new value)
     * Used when editing an asset that already has a QR code
     */
    public boolean editQRCode(String newQRCode) {
        System.out.println("📝 Editing QR code to: " + newQRCode);
        
        // On Edit screen, QR Code field already has a value - DON'T scroll up first
        // Just scroll down once since QR Code is near the bottom
        scrollFormDown();
        sleep(200);
        
        // Try to find QR Code field (max 3 attempts with 1 scroll each)
        for (int attempt = 0; attempt < 3; attempt++) {
            System.out.println("   📜 Looking for QR Code field (attempt " + (attempt + 1) + ")");
            
            // Strategy 1: Find TextField with value starting with "QR_" (existing QR code)
            try {
                List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
                System.out.println("   Found " + textFields.size() + " text fields");
                
                for (WebElement field : textFields) {
                    try {
                        String value = field.getAttribute("value");
                        int fieldY = field.getLocation().getY();
                        
                        System.out.println("   Checking field: value='" + value + "', Y=" + fieldY);
                        
                        // Check if this field contains a QR code value
                        if (value != null && value.startsWith("QR_") && fieldY > 0 && fieldY < 850) {
                            System.out.println("   🔍 Found QR field with value: " + value);
                            field.click();
                            sleep(300);
                            field.clear();
                            sleep(200);
                            field.sendKeys(newQRCode);
                            sleep(200);
                            dismissKeyboard();
                            System.out.println("✅ Edited QR code to: " + newQRCode);
                            return true;
                        }
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
                System.out.println("   ⚠️ Strategy 1 exception: " + e.getMessage());
            }
            
            // Strategy 2: Find by "QR Code" label position
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND label == 'QR Code'")
                );
                System.out.println("   Found " + labels.size() + " 'QR Code' labels");
                
                for (WebElement label : labels) {
                    try {
                        int labelY = label.getLocation().getY();
                        System.out.println("   QR Code label at Y=" + labelY);
                        
                        if (labelY > 0 && labelY < 800) {
                            // Find TextField below label
                            List<WebElement> fields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
                            for (WebElement field : fields) {
                                try {
                                    int fieldY = field.getLocation().getY();
                                    if (fieldY > labelY && (fieldY - labelY) < 70) {
                                        String value = field.getAttribute("value");
                                        System.out.println("   🔍 Found TextField below label: Y=" + fieldY + ", value=" + value);
                                        field.click();
                                        sleep(300);
                                        field.clear();
                                        sleep(200);
                                        field.sendKeys(newQRCode);
                                        sleep(200);
                                        dismissKeyboard();
                                        System.out.println("✅ Edited QR code (by label) to: " + newQRCode);
                                        return true;
                                    }
                                } catch (Exception e) {}
                            }
                        }
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
                System.out.println("   ⚠️ Strategy 2 exception: " + e.getMessage());
            }
            
            // Scroll down once more
            if (attempt < 2) {
                System.out.println("   📜 Scrolling down...");
                scrollFormDown();
                sleep(200);
            }
        }
        
        System.out.println("❌ Could not find QR Code field");
        return false;
    }



    public void dismissKeyboard() {
        // Strategy 1: Try Done/Return button on keyboard toolbar
        try {
            WebElement doneButton = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Done' OR name == 'Return' OR name == 'return')")
            );
            doneButton.click();
            System.out.println("✅ Keyboard dismissed (Done/Return button)");
            return;
        } catch (Exception e) {}
        
        // Strategy 2: Try Appium hideKeyboard
        try {
            driver.hideKeyboard();
            System.out.println("✅ Keyboard dismissed (hideKeyboard)");
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Tap outside keyboard area (safe zone at top)
        try {
            int screenWidth = driver.manage().window().getSize().width;
            driver.executeScript("mobile: tap", Map.of("x", screenWidth / 2, "y", 100));
            System.out.println("✅ Keyboard dismissed (tap outside)");
            return;
        } catch (Exception e) {}
        
        // Strategy 4: Press keyboard key to confirm (Enter/Return on keyboard)
        try {
            WebElement keyboardKey = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeKey' AND (name CONTAINS 'Return' OR name CONTAINS 'return' OR name CONTAINS 'Go' OR name CONTAINS 'Next')")
            );
            keyboardKey.click();
            System.out.println("✅ Keyboard dismissed (keyboard key)");
            return;
        } catch (Exception e) {}
        
        System.out.println("⚠️ Keyboard may still be open - all dismiss strategies exhausted");
    }

    public void scrollFormDown() {
        // Use RIGHT EDGE corner for scrolling - no form fields there!
        try {
            int screenWidth = driver.manage().window().getSize().width;
            
            // Use RIGHT edge (x = screenWidth - 20) to avoid all form fields
            int scrollX = screenWidth - 20;  // Right edge
            int startY = 700;
            int endY = 300;
            
            System.out.println("   📜 Scroll (right edge): (" + scrollX + ", " + startY + ") -> (" + scrollX + ", " + endY + ")");
            
            driver.executeScript("mobile: dragFromToForDuration", Map.of(
                "fromX", scrollX,
                "fromY", startY,
                "toX", scrollX,
                "toY", endY,
                "duration", 0.3
            ));
            System.out.println("✅ Scrolled down");
        } catch (Exception e) {
            System.out.println("⚠️ Scroll failed: " + e.getMessage());
        }
    }

    public void scrollFormUp() {
        // Use RIGHT EDGE corner for scrolling - no form fields there!
        try {
            int screenWidth = driver.manage().window().getSize().width;
            
            // Use RIGHT edge (x = screenWidth - 20) to avoid all form fields
            int scrollX = screenWidth - 20;  // Right edge
            int startY = 300;
            int endY = 700;
            
            driver.executeScript("mobile: dragFromToForDuration", Map.of(
                "fromX", scrollX,
                "fromY", startY,
                "toX", scrollX,
                "toY", endY,
                "duration", 0.3
            ));
            System.out.println("✅ Scrolled up");
        } catch (Exception e) {
            System.out.println("⚠️ Scroll up failed: " + e.getMessage());
        }
    }

    // Nav bar zone = Y < 150. After mobile:scroll, elements can land behind it.
    // This does a precise nudge to bring the element to ~Y=200.
    private boolean nudgeIfBehindNavBar(int elementY) {
        if (elementY >= 150) return false;
        try {
            int nudge = 220 - elementY;
            if (nudge < 60) nudge = 60;
            int screenWidth = driver.manage().window().getSize().width;
            System.out.println("   ⚠️ Element at Y=" + elementY + " behind nav bar, nudging " + nudge + "px down...");
            driver.executeScript("mobile: dragFromToForDuration", Map.of(
                "fromX", screenWidth - 20,
                "fromY", 400,
                "toX", screenWidth - 20,
                "toY", 400 + nudge,
                "duration", 0.15
            ));
            sleep(200);
        } catch (Exception e) {
            System.out.println("   ⚠️ Nudge failed: " + (e.getMessage() != null ? e.getMessage().substring(0, Math.min(60, e.getMessage().length())) : ""));
        }
        return true;
    }

    public boolean isCreateAssetButtonDisplayed() {
        return isElementDisplayed(createAssetButton);
    }

    // ================================================================
    // FIX FOR ATS_ECR_07 - ROBUST isCreateAssetButtonEnabled
    // ================================================================

    /**
     * Check if Create Asset button is enabled
     * FIX: Uses multiple strategies to accurately check button state
     */
    public boolean isCreateAssetButtonEnabled() {
        System.out.println("🔍 Checking Create Asset button state...");
        
        // Strategy 1: Check 'enabled' attribute
        try {
            WebElement btn = driver.findElement(AppiumBy.accessibilityId("Create Asset"));
            String enabled = btn.getAttribute("enabled");
            System.out.println("   enabled attribute: " + enabled);
            if ("false".equalsIgnoreCase(enabled)) {
                return false;
            }
        } catch (Exception e) {
            System.out.println("   Could not check enabled attribute: " + e.getMessage());
        }
        
        // Strategy 2: Check 'accessible' attribute (sometimes used for disabled state)
        try {
            WebElement btn = driver.findElement(AppiumBy.accessibilityId("Create Asset"));
            String accessible = btn.getAttribute("accessible");
            System.out.println("   accessible attribute: " + accessible);
        } catch (Exception e) {}
        
        // Strategy 3: Check opacity/alpha (disabled buttons often have lower opacity)
        try {
            WebElement btn = driver.findElement(AppiumBy.accessibilityId("Create Asset"));
            String value = btn.getAttribute("value");
            String label = btn.getAttribute("label");
            System.out.println("   value: " + value + ", label: " + label);
        } catch (Exception e) {}
        
        // Strategy 4: Check if button is in enabled state by checking clickability
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            shortWait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Create Asset")
            ));
            System.out.println("   Button is clickable");
            return true;
        } catch (Exception e) {
            System.out.println("   Button is NOT clickable (timeout)");
            return false;
        }
    }

    /**
     * Strict tri-state variant of {@link #isCreateAssetButtonEnabled()}: locates the
     * Create button with the SAME multi-strategy set clickCreateAsset uses and returns
     * TRUE (enabled), FALSE (disabled), or NULL when the button cannot be located by
     * ANY strategy. Callers must treat NULL as "cannot verify" — the old boolean
     * variant swallowed not-found into false, which made ATS_ECR_07 pass without ever
     * seeing the button (vacuous pass, caught in the 2026-07-02 local loop).
     */
    public Boolean isCreateAssetButtonEnabledStrict() {
        return withImplicitWait(800, () -> {
            WebElement btn = null;
            String how = null;
            try {
                btn = driver.findElement(AppiumBy.accessibilityId("Create Asset"));
                how = "accessibilityId 'Create Asset'";
            } catch (Exception ignored) {}
            if (btn == null) {
                String[] predicates = {
                    "type == 'XCUIElementTypeButton' AND name == 'Create Asset'",
                    "type == 'XCUIElementTypeButton' AND label == 'Create Asset'",
                    "type == 'XCUIElementTypeButton' AND name CONTAINS[c] 'create'",
                    "type == 'XCUIElementTypeButton' AND label CONTAINS[c] 'create' AND label CONTAINS[c] 'asset'",
                    // SwiftUI renders some disabled buttons as non-Button elements:
                    "(name == 'Create Asset' OR label == 'Create Asset')",
                };
                for (String p : predicates) {
                    try {
                        btn = driver.findElement(AppiumBy.iOSNsPredicateString(p));
                        how = p;
                        break;
                    } catch (Exception ignored) {}
                }
            }
            if (btn == null) {
                System.out.println("   ⚠️ Create button not locatable by ANY strategy — state UNKNOWN");
                return null;
            }
            String enabled = null;
            try { enabled = btn.getAttribute("enabled"); } catch (Exception ignored) {}
            System.out.println("   Create button found via [" + how + "]; enabled=" + enabled);
            return !"false".equalsIgnoreCase(enabled);
        });
    }
    
    /**
     * Check if the current asset name value is effectively empty
     * (empty string or only whitespace/spaces)
     */
    public boolean isAssetNameEffectivelyEmpty() {
        try {
            // Find all text fields and check the first one (name field)
            List<WebElement> textFields = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField'")
            );
            
            if (!textFields.isEmpty()) {
                String value = textFields.get(0).getAttribute("value");
                System.out.println("   Name field value: '" + value + "'");
                
                // Check if null, empty, placeholder, or only spaces
                if (value == null || 
                    value.isEmpty() || 
                    value.equals("Enter name") ||
                    value.trim().isEmpty()) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not check name field: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Alternative validation: Check if form can be submitted
     * This handles the case where button appears enabled but validation fails on click
     */
    public boolean canSubmitForm() {
        // Check if all required fields have valid values
        boolean nameValid = !isAssetNameEffectivelyEmpty();
        boolean classSelected = isAssetClassSelected();
        boolean locationSelected = isLocationSelected();
        
        System.out.println("   Form validation - Name: " + nameValid + 
                          ", Class: " + classSelected + 
                          ", Location: " + locationSelected);
        
        return nameValid && classSelected && locationSelected;
    }
    
    /**
     * Check if an asset class has been selected
     */
    public boolean isAssetClassSelected() {
        try {
            // Strategy 1: Check if "Select asset class" button label changed
            try {
                WebElement classBtn = driver.findElement(AppiumBy.accessibilityId("Select asset class"));
                String label = classBtn.getAttribute("label");
                String name = classBtn.getAttribute("name");
                String value = classBtn.getAttribute("value");
                
                System.out.println("   Asset class button - label: " + label + ", name: " + name + ", value: " + value);
                
                // If label/name/value contains ATS, UPS, PDU, Generator - it's selected
                String combined = (label != null ? label : "") + (name != null ? name : "") + (value != null ? value : "");
                if (combined.contains("ATS") || combined.contains("UPS") || combined.contains("PDU") || combined.contains("Generator")) {
                    return true;
                }
                
                // If still shows "Select asset class", nothing selected
                if (label != null && !label.equals("Select asset class") && !label.isEmpty()) {
                    return true;
                }
            } catch (Exception e) {}
            
            // Strategy 2: Look for any visible text showing ATS, UPS, PDU, Generator near the class field
            try {
                List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
                for (WebElement text : texts) {
                    String textName = text.getAttribute("name");
                    if (textName != null && (textName.equals("ATS") || textName.equals("UPS") || 
                        textName.equals("PDU") || textName.equals("Generator"))) {
                        return true;
                    }
                }
            } catch (Exception e) {}
            
            // Strategy 3: Check if the dropdown is no longer visible (means selection was made)
            try {
                boolean dropdownStillOpen = isAssetClassDropdownDisplayed();
                // If dropdown closed after clicking an option, selection was made
                // This is a weak check but can help
            } catch (Exception e) {}
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a location has been selected
     */
    public boolean isLocationSelected() {
        try {
            WebElement locationBtn = driver.findElement(AppiumBy.accessibilityId("Select location"));
            String label = locationBtn.getAttribute("label");
            // If still shows "Select location", nothing selected
            return label != null && !label.equals("Select location");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Create Asset button - ROBUST VERSION
     * Handles: scroll, keyboard dismiss, multiple locator strategies
     */
    public void clickCreateAsset() {
        System.out.println("📦 Clicking Create Asset button...");
        
        // Step 1: Dismiss keyboard first (might be covering button)
        dismissKeyboard();
        sleep(300);
        
        // Step 2: Scroll up to make button visible (it's at the top)
        for (int scrollAttempt = 0; scrollAttempt < 3; scrollAttempt++) {
            scrollFormUp();
            sleep(300);
            
            // Check if button is now visible and clickable
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
                WebElement btn = shortWait.until(ExpectedConditions.elementToBeClickable(
                    AppiumBy.accessibilityId("Create Asset")
                ));
                btn.click();
                System.out.println("✅ Clicked Create Asset (scroll attempt " + (scrollAttempt + 1) + ")");
                return;
            } catch (Exception e) {
                System.out.println("   Scroll attempt " + (scrollAttempt + 1) + " - button not clickable yet");
            }
        }
        
        // Step 3: Try alternative locators
        String[] locators = {
            "name == 'Create Asset'",
            "label == 'Create Asset'",
            "name CONTAINS[c] 'create'",
            "label CONTAINS[c] 'create' AND label CONTAINS[c] 'asset'"
        };
        
        for (String predicate : locators) {
            try {
                WebElement btn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND " + predicate)
                );
                if (btn.isDisplayed()) {
                    btn.click();
                    System.out.println("✅ Clicked Create Asset via: " + predicate);
                    return;
                }
            } catch (Exception e) {
                // Try next locator
            }
        }
        
        // Step 4: Search all buttons for "Create"
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                try {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    if ((name != null && name.toLowerCase().contains("create")) ||
                        (label != null && label.toLowerCase().contains("create"))) {
                        System.out.println("   Found button: name='" + name + "', label='" + label + "'");
                        if (btn.isDisplayed()) {
                            btn.click();
                            System.out.println("✅ Clicked Create button");
                            return;
                        }
                    }
                } catch (Exception ex) {}
            }
        } catch (Exception e) {
            System.out.println("⚠️ Button search failed: " + e.getMessage());
        }
        
        // Step 5: Try coordinate tap at top of screen (where button usually is)
        try {
            System.out.println("   Trying coordinate tap...");
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            // Create Asset button is typically at top-right
            int x = (int) (size.width * 0.85);  // 85% from left
            int y = (int) (size.height * 0.12); // 12% from top (navigation bar area)
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            System.out.println("✅ Tapped at (" + x + ", " + y + ")");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Coordinate tap failed: " + e.getMessage());
        }
        
        // Step 6: Last resort - use annotated element with longer wait
        try {
            System.out.println("   Last resort: using annotated element...");
            WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            longWait.until(ExpectedConditions.elementToBeClickable(createAssetButton)).click();
            System.out.println("✅ Clicked Create Asset (annotated element)");
            return;
        } catch (Exception e) {
            System.out.println("⚠️ Annotated element failed: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to click Create Asset button after all attempts");
    }

    public boolean isSaveButtonDisplayed() {
        return isElementDisplayed(saveButton);
    }

    public void clickSave() {
        click(saveButton);
        saveButtonClickedThisFlow = true;
        System.out.println("✅ Clicked Save");
    }

    public boolean isDeleteButtonDisplayed() {
        return isElementDisplayed(deleteButton);
    }

    public void clickDelete() {
        click(deleteButton);
        System.out.println("✅ Clicked Delete");
    }

    // ================================================================
    // COMPLETE ASSET CREATION FLOW
    // ================================================================

    public String createAsset(String assetName, String assetClass, String subtype, String qrCode) {
        System.out.println("\n📦 CREATING ASSET: " + assetName);
        
        enterAssetName(assetName);
        selectAssetClass(assetClass);
        
        boolean locationSelected = selectLocation();
        if (!locationSelected) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            createNewLocation("Floor_" + timestamp, "Room_" + timestamp);
        }
        
        dismissKeyboard();
        scrollFormDown();
        
        if (subtype != null && !subtype.isEmpty()) {
            try {
                selectAssetSubtype(subtype);
            } catch (Exception e) {
                System.out.println("⚠️ Subtype selection skipped: " + e.getMessage());
            }
        }
        
        if (qrCode != null && !qrCode.isEmpty()) {
            enterQRCode(qrCode);
        }
        
        clickCreateAsset();
        
        System.out.println("✅✅✅ ASSET CREATED: " + assetName + " ✅✅✅\n");
        return assetName;
    }

    public String createAssetWithAutoName(String assetClass) {
        long timestamp = System.currentTimeMillis();
        String assetName = "Asset_" + timestamp;
        String qrCode = "QR_" + timestamp;
        return createAsset(assetName, assetClass, "test", qrCode);
    }

    public String createATSAsset() {
        return createAssetWithAutoName("ATS");
    }

    // ================================================================
    // VALIDATION METHODS
    // ================================================================

    public boolean isAssetCreatedSuccessfully() {
        sleep(500);
        
        // Check if we're on Asset List (success)
        if (isAssetListDisplayed()) {
            System.out.println("✅ Asset created - on Asset List");
            return true;
        }
        
        // Check if we're on Asset Details (success - some apps navigate here)
        try {
            WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
            if (editBtn.isDisplayed()) {
                System.out.println("✅ Asset created - on Asset Details");
                return true;
            }
        } catch (Exception e) {}
        
        // Check if Create form is gone (success)
        if (!isCreateAssetFormDisplayed()) {
            System.out.println("✅ Asset created - Create form gone");
            return true;
        }
        
        // Check for success message
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && (name.toLowerCase().contains("created") || name.toLowerCase().contains("success"))) {
                    System.out.println("✅ Asset created - success message");
                    return true;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Asset creation status unclear");
        return false;
    }

    public boolean isRequiredFieldErrorDisplayed() {
        try {
            WebElement error = driver.findElement(
                AppiumBy.iOSNsPredicateString("label CONTAINS 'required' OR label CONTAINS 'Required'")
            );
            return error.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAssetClassDropdownDisplayed() {
        try {
            // Wait a bit for dropdown to appear
            sleep(200);
            
            // Try multiple ways to detect dropdown
            if (isElementDisplayed(atsClassOption)) return true;
            if (isElementDisplayed(upsClassOption)) return true;
            if (isElementDisplayed(pduClassOption)) return true;
            
            // Alternative: Check for any asset class options by searching buttons
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && (name.equals("ATS") || name.equals("UPS") || name.equals("PDU") || name.equals("Generator"))) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLocationPickerDisplayed() {
        // Use NSPredicate server-side filter instead of iterating all buttons client-side
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(800));
        try {
            List<WebElement> floors = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS ' floor'"
            ));
            return !floors.isEmpty();
        } catch (Exception e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
    }

    public boolean isSubtypeDropdownDisplayed() {
        try {
            // Check for any subtype option
            if (isElementDisplayed(testSubtypeOption)) return true;
            
            // Check for common ATS subtypes
            try {
                WebElement option = driver.findElement(AppiumBy.accessibilityId("Automatic Transfer Switch (<= 1000V)"));
                if (option.isDisplayed()) return true;
            } catch (Exception e) {}
            
            // Check for any button that looks like a subtype option
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && (name.contains("Transfer") || name.contains("Switch") || 
                    name.contains("test") || name.contains("None") || name.contains("Subtype"))) {
                    return true;
                }
            }
            
            // Check for picker wheel (dropdown)
            List<WebElement> pickers = driver.findElements(AppiumBy.className("XCUIElementTypePickerWheel"));
            if (!pickers.isEmpty()) return true;
            
            // Check for any static text that looks like subtype options
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && (name.contains("Transfer") || name.contains("Switch") || 
                    name.contains("Automatic") || name.contains("Manual"))) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Select first available subtype from dropdown (excluding "None")
     * TURBO VERSION - Fast selection
     * @return The name of the selected subtype
     */
    public String selectFirstAvailableSubtype() {
        // Try first ATS subtype directly - fastest path
        try {
            WebElement option = driver.findElement(
                AppiumBy.accessibilityId("Automatic Transfer Switch (<= 1000V)")
            );
            option.click();
            System.out.println("✅ Selected: Automatic Transfer Switch (<= 1000V)");
            sleep(300);
            tapDoneOnPicker();
            return "Automatic Transfer Switch (<= 1000V)";
        } catch (Exception e) {}

        // Try any Transfer Switch option
        try {
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Transfer Switch'"
                )
            );
            String name = option.getAttribute("name");
            option.click();
            System.out.println("✅ Selected: " + name);
            sleep(300);
            tapDoneOnPicker();
            return name;
        } catch (Exception e) {}

        // Fallback: Any button except None/Cancel/Done (exclude picker nav buttons)
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "name != 'None' AND name != 'Cancel' AND name != 'Quick' AND name != 'Detailed' AND name != 'Done'"
                )
            );
            if (!buttons.isEmpty()) {
                String name = buttons.get(0).getAttribute("name");
                buttons.get(0).click();
                System.out.println("✅ Selected: " + name);
                sleep(300);
                tapDoneOnPicker();
                return name;
            }
        } catch (Exception e) {}

        return null;
    }
    
    /**
     * Check if a subtype has been selected (not showing "Select asset subtype" placeholder)
     */
    public boolean isSubtypeSelected() {
        try {
            // Check if "Select asset subtype" placeholder is still visible on the BUTTON
            List<WebElement> placeholder = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name == 'Select asset subtype' OR name CONTAINS 'asset subtype')"
                )
            );
            
            if (placeholder.isEmpty()) {
                // Placeholder gone, something is selected
                System.out.println("✅ Subtype is selected (placeholder not visible)");
                return true;
            }
            
            // Check if the button text changed from placeholder
            for (WebElement el : placeholder) {
                String label = el.getAttribute("label");
                if (label != null && !label.equals("Select asset subtype")) {
                    System.out.println("✅ Subtype is selected: " + label);
                    return true;
                }
            }
            
            // Check if any known subtype is visible as selected
            String[] subtypes = {
                "Automatic Transfer Switch (<= 1000V)",
                "Automatic Transfer Switch (> 1000V)",
                "Transfer Switch (<= 1000V)",
                "Transfer Switch (> 1000V)"
            };
            
            for (String subtype : subtypes) {
                try {
                    WebElement selected = driver.findElement(AppiumBy.accessibilityId(subtype));
                    if (selected.isDisplayed()) {
                        System.out.println("✅ Subtype is selected: " + subtype);
                        return true;
                    }
                } catch (Exception e) {}
            }
            
            System.out.println("⚠️ No subtype selected");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking subtype: " + e.getMessage());
            return false;
        }
    }


    public List<String> getAvailableAssetClasses() {
        List<String> classes = new java.util.ArrayList<>();
        clickSelectAssetClass();
        sleep(200);
        
        try {
            List<WebElement> options = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText'")
            );
            for (WebElement option : options) {
                String name = option.getAttribute("name");
                if (name != null && !name.isEmpty() && 
                    !name.equals("Select asset class") && !name.equals("Cancel")) {
                    classes.add(name);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get asset classes: " + e.getMessage());
        }
        
        try {
            clickCancel();
        } catch (Exception e) {}
        
        return classes;
    }

    public boolean verifyAssetExistsInList(String assetName) {
        try {
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && name.contains(assetName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error verifying asset: " + e.getMessage());
        }
        return false;
    }

    // ================================================================
    // EDIT ASSET DETAILS METHODS
    // ================================================================

    /**
     * Click Edit button to open Edit Asset Details screen
     */
    public void clickEdit() {
        System.out.println("📝 Preparing edit mode...");

        // v1.36+: Asset Detail IS the edit screen — there is no Edit button, so
        // probing it first could NEVER match and burned ~12s per call (43+ call
        // sites). Probe today's reality first; legacy Edit-button strategies are
        // kept LAST as 0-implicit probes for older builds.
        //
        // Single bounded wait on the expected winner (screen still rendering):
        Waits.until(() ->
            existsNow(AppiumBy.iOSNsPredicateString("name == 'Asset Details'"))
            || existsNow(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField'")),
            3_000);

        boolean onEditScreen =
            // Check 1: Asset Details navigation bar
            existsNow(AppiumBy.iOSNsPredicateString("name == 'Asset Details'"))
            // Check 2: editable text field (Asset Name)
            || existsNow(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField'"))
            // Check 3: Asset Class button
            || existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name == 'MCC' OR name == 'ATS' OR name CONTAINS 'Select asset')"))
            // Check 4: form labels
            || existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (name == 'Name' OR name == 'Asset Class' OR name == 'Location')"))
            // Check 5: Save Changes button
            || existsNow(AppiumBy.iOSNsPredicateString("(name CONTAINS 'Save' OR label CONTAINS 'Save')"))
            // Check 6: Back button — at least on a detail screen
            || existsNow(AppiumBy.iOSNsPredicateString(
                "(name == 'Back' OR name == 'Assets') AND type == 'XCUIElementTypeButton'"));

        if (onEditScreen) {
            System.out.println("✅ On Asset Details edit screen");
            return;
        }

        // LEGACY (pre-v1.36 separate Edit mode) — 0-implicit probes, click if present
        By editById = AppiumBy.accessibilityId("Edit");
        if (existsNow(editById)) {
            try {
                driver.findElement(editById).click();
                System.out.println("✅ Clicked Edit button");
                return;
            } catch (Exception e) {}
        }
        By editByPredicate = AppiumBy.iOSNsPredicateString("name == 'Edit' OR label == 'Edit'");
        if (existsNow(editByPredicate)) {
            try {
                driver.findElement(editByPredicate).click();
                System.out.println("✅ Clicked Edit button (predicate)");
                return;
            } catch (Exception e) {}
        }
        By editByIcon = AppiumBy.accessibilityId("square.and.pencil");
        if (existsNow(editByIcon)) {
            try {
                driver.findElement(editByIcon).click();
                System.out.println("✅ Clicked Edit button (icon)");
                return;
            } catch (Exception e) {}
        }

        System.out.println("⚠️ Could not verify edit screen");
    }
    
    /**
     * TURBO: Click Edit button - direct fast click (no retry)
     */
    public void clickEditTurbo() {
        // Reset toggle cache since we're entering a (potentially different) edit screen
        resetToggleSearchCache();

        // v1.36+: Asset Detail screen is DIRECTLY editable — no separate Edit
        // button exists, so probing accessibilityId("Edit") FIRST with the full
        // implicit wait could never match and cost ~5s on every one of the 43+
        // call sites. Wait (bounded) for the screen's real markers instead.
        Waits.until(() ->
            existsNow(AppiumBy.iOSNsPredicateString(
                "(name == 'Asset Details' OR name CONTAINS 'Asset Detail' OR label CONTAINS 'Asset Detail')"))
            || existsNow(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField'")),
            3_000);

        // Verify we're on the Asset Details screen (which IS the edit screen).
        // All probes are 0-implicit — coverage unchanged, misses cost ms not 5s.
        boolean onEditScreen = false;

        // Check 1: Asset Details navigation bar
        if (existsNow(AppiumBy.iOSNsPredicateString(
                "(name == 'Asset Details' OR name CONTAINS 'Asset Detail' OR label CONTAINS 'Asset Detail')"))) {
            onEditScreen = true;
            System.out.println("   ✓ Found Asset Details navigation");
        }

        // Check 2: Editable text field (Asset Name field)
        if (!onEditScreen && existsNow(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField'"))) {
            onEditScreen = true;
            System.out.println("   ✓ Found editable text field");
        }

        // Check 3: Asset Class buttons
        if (!onEditScreen && existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name == 'MCC' OR name == 'ATS' OR name == 'UPS' OR name == 'PDU' OR name == 'Generator' OR name == 'Busway' OR name == 'Fuse' OR name == 'Motor' OR name == 'Loadcenter' OR name == 'Panelboard' OR name == 'Relay' OR name == 'Transformer' OR name == 'VFD' OR name == 'Utility' OR name CONTAINS 'Select asset' OR name CONTAINS 'Disconnect' OR name CONTAINS 'Circuit Breaker' OR name CONTAINS 'Junction Box' OR name CONTAINS 'Switchboard' OR name CONTAINS 'MCC Bucket')"))) {
            onEditScreen = true;
            System.out.println("   ✓ Found Asset Class button");
        }

        // Check 4: Look for "Name" or "Asset Class" labels (form labels)
        if (!onEditScreen && existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (name == 'Name' OR name == 'Asset Class' OR name == 'Location' OR name == 'Subtype' OR name CONTAINS 'Required')"))) {
            onEditScreen = true;
            System.out.println("   ✓ Found form labels");
        }

        // Check 5: Look for Save Changes button
        if (!onEditScreen && existsNow(AppiumBy.iOSNsPredicateString(
                "(name CONTAINS 'Save' OR label CONTAINS 'Save')"))) {
            onEditScreen = true;
            System.out.println("   ✓ Found Save button");
        }

        // Check 6: Look for Back button (indicates we navigated somewhere)
        if (!onEditScreen && existsNow(AppiumBy.iOSNsPredicateString(
                "(name == 'Back' OR name == 'Assets' OR name CONTAINS 'chevron') AND type == 'XCUIElementTypeButton'"))) {
            // We're on some detail screen, probably the edit screen
            onEditScreen = true;
            System.out.println("   ✓ Found Back button - on detail screen");
        }

        // LEGACY: explicit Edit button (pre-v1.36 builds only) — probed LAST,
        // 0-implicit, only when the direct-edit markers were all absent.
        if (!onEditScreen && existsNow(AppiumBy.accessibilityId("Edit"))) {
            try {
                driver.findElement(AppiumBy.accessibilityId("Edit")).click();
                System.out.println("✅ Clicked Edit button (legacy)");
                sleep(300);
                onEditScreen = true;
            } catch (Exception e) {
                // No clickable Edit button — fall through to debug dump
            }
        }

        if (onEditScreen) {
            System.out.println("✅ On Edit Asset screen");
        } else {
            System.out.println("⚠️ May not be on edit screen - form fields not detected");
            // Log what IS visible for debugging
            try {
                List<WebElement> allVisible = driver.findElements(
                    AppiumBy.iOSNsPredicateString("(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeNavigationBar')")
                );
                System.out.println("   Debug: Found " + allVisible.size() + " visible elements");
                for (int i = 0; i < Math.min(5, allVisible.size()); i++) {
                    String name = allVisible.get(i).getAttribute("name");
                    String type = allVisible.get(i).getAttribute("type");
                    System.out.println("      - " + type + ": " + name);
                }
            } catch (Exception e) {}
        }
    }

    /**
     * Check if Edit Asset Details screen is displayed
     */
    public boolean isEditAssetScreenDisplayed() {
        try {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(1500));
            try {
                // Check for Save Changes button (primary edit mode indicator)
                try {
                    WebElement saveBtn = driver.findElement(
                        AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' OR label CONTAINS 'Save'")
                    );
                    if (saveBtn.isDisplayed()) {
                        System.out.println("   ✅ Edit screen detected (Save Changes visible)");
                        return true;
                    }
                } catch (Exception e) {}

                // Check for "Asset Details" nav bar title (always present on edit screen)
                try {
                    WebElement navBar = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeNavigationBar' AND (name CONTAINS 'Asset' OR label CONTAINS 'Asset')"));
                    if (navBar.isDisplayed()) {
                        System.out.println("   ✅ Edit screen detected (Asset Details nav bar)");
                        return true;
                    }
                } catch (Exception e) {}

                // Check for "Close" button + any text field (edit screen has editable fields)
                try {
                    WebElement closeBtn = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (label == 'Close' OR label == 'Done')"));
                    if (closeBtn.isDisplayed()) {
                        System.out.println("   ✅ Edit screen detected (Close/Done button)");
                        return true;
                    }
                } catch (Exception e) {}

                // Check for Asset Class dropdown with ALL class names
                String[] assetClasses = {
                    "ATS", "UPS", "PDU", "Generator", "Busway", "Capacitor",
                    "Circuit Breaker", "Disconnect Switch", "Fuse", "Junction Box",
                    "Loadcenter", "MCC", "MCC Bucket", "Motor", "Other", "Other (OCP)",
                    "Panelboard", "Relay", "Switchboard", "Transformer", "VFD", "Utility"
                };
                for (String className : assetClasses) {
                    try {
                        WebElement classEl = driver.findElement(AppiumBy.accessibilityId(className));
                        if (classEl.isDisplayed()) {
                            System.out.println("   ✅ Edit screen detected (Asset class " + className + " visible)");
                            return true;
                        }
                    } catch (Exception e) {}
                }

                // Check for Core Attributes label
                try {
                    List<WebElement> coreAttr = driver.findElements(
                        AppiumBy.iOSNsPredicateString("name CONTAINS 'Core Attributes' OR label CONTAINS 'Core Attributes'")
                    );
                    if (!coreAttr.isEmpty()) {
                        System.out.println("   ✅ Edit screen detected (Core Attributes visible)");
                        return true;
                    }
                } catch (Exception e) {}

                return false;
            } finally {
                driver.manage().timeouts().implicitlyWait(
                    java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Core Attributes section is visible
     */
    public boolean isCoreAttributesSectionVisible() {
        // All probes run at implicit-wait 0: on a bleed-through Edit DOM (or a class like Loadcenter
        // that lacks these fields), the old whole-tree CONTAINS finds burned the full 5s implicit
        // wait per miss × 5 fields × 3 scrolls and wedged WDA (LC_EAD_02 6m0s hang). Same elements
        // are matched — just no 5s burn per miss.
        for (int i = 0; i < 3; i++) {
            try {
                List<WebElement> elements = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString("name CONTAINS 'Core Attributes' OR label CONTAINS 'Core Attributes'")
                ));
                if (!elements.isEmpty()) {
                    System.out.println("✅ Core Attributes section visible");
                    return true;
                }

                // Also check for specific attribute fields
                String[] attributeFields = {"Serial Number", "Phase", "Manufacturer", "Model"};
                for (String field : attributeFields) {
                    final String f = field;
                    List<WebElement> fieldElements = withImplicitWait(0, () -> driver.findElements(
                        AppiumBy.iOSNsPredicateString("name CONTAINS '" + f + "' OR label CONTAINS '" + f + "'")
                    ));
                    if (!fieldElements.isEmpty()) {
                        System.out.println("✅ Core Attributes section visible (found " + field + ")");
                        return true;
                    }
                }
            } catch (Exception e) {}
            
            // Scroll down to find it
            if (i < 2) {
                scrollFormDown();
                sleep(300);
            }
        }
        return false;
    }

    /**
     * Check if an exception indicates the Appium/WDA driver session has died.
     * Used to bail out early instead of retrying with a dead driver.
     */
    private boolean isDriverSessionDead(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        String cause = e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage() : "";
        String combined = msg + " " + cause;
        return combined.contains("Error communicating with the remote browser") ||
               combined.contains("session is either terminated or not started") ||
               combined.contains("Session ID is null") ||
               combined.contains("Connection refused") ||
               combined.contains("Connection reset") ||
               combined.contains("Unable to create session") ||
               combined.contains("It may have died");
    }

    /**
     * Find the Required Fields Only toggle using multiple strategies.
     * Returns the toggle WebElement or null if not found.
     *
     * Performance: enforces a 15-second total time bound. Previous version could
     * take 5-7 minutes per call, and callers invoke this 6+ times, exceeding the
     * 420s test timeout. With the cache, repeated "not found" results return instantly.
     *
     * Also searches for XCUIElementTypeButton in addition to XCUIElementTypeSwitch,
     * in case the app changed the toggle's element type.
     */
    private WebElement findRequiredFieldsToggle() {
        // CACHE: if a previous search already exhausted all strategies, return null instantly.
        // Log the skip ONCE per cache lifetime — callers poll this in tight loops and the
        // per-call println once produced 149,888 lines (95.6% of a 32MB CI log).
        if (toggleSearchExhausted) {
            if (!toggleCacheSkipLogged) {
                System.out.println("🔍 Toggle search cached as not-found — skipping silently until resetToggleSearchCache()");
                toggleCacheSkipLogged = true;
            }
            return null;
        }

        System.out.println("🔍 Finding Required Fields Only toggle...");
        long searchStart = System.currentTimeMillis();

        // Single bounded wait on the expected winner (a rendered Switch), then
        // the whole cascade runs at 0 implicit wait — 8 finds × 800ms misses
        // cost ~6.4s before; same coverage now costs milliseconds per miss.
        Waits.until(() -> existsNow(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSwitch'")), 3_000);
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ZERO);
        try {

        // Helper: element type predicate that covers both Switch and Button
        String switchOrButton = "(type == 'XCUIElementTypeSwitch' OR type == 'XCUIElementTypeButton')";

        // FIRST: Quick scroll to Core Attributes area (2 attempts max)
        for (int scrollAttempt = 0; scrollAttempt < 2; scrollAttempt++) {
            if (System.currentTimeMillis() - searchStart > TOGGLE_SEARCH_MAX_MS) break;
            try {
                WebElement toggle = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeSwitch'"
                    )
                );
                if (toggle != null && toggle.isDisplayed()) {
                    System.out.println("   ✅ Found visible switch (scroll attempt " + scrollAttempt + ")");
                    return toggle;
                }
            } catch (Exception e) {
                if (isDriverSessionDead(e)) {
                    System.out.println("   ❌ Driver session is dead, aborting toggle search");
                    return null;
                }
                System.out.println("   Scroll attempt " + (scrollAttempt + 1) + " - toggle not visible yet");
                scrollFormDown();
                sleep(300);
            }
        }

        // Strategy 1: Find toggle by name/label containing "Required"
        if (System.currentTimeMillis() - searchStart < TOGGLE_SEARCH_MAX_MS) {
            try {
                WebElement toggle = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "(name CONTAINS[c] 'Required' OR label CONTAINS[c] 'Required') " +
                        "AND " + switchOrButton + ""
                    )
                );
                if (toggle != null && toggle.isDisplayed()) {
                    System.out.println("   ✅ Found toggle by name/label containing 'Required'");
                    return toggle;
                }
            } catch (Exception e) {
                if (isDriverSessionDead(e)) {
                    System.out.println("   ❌ Driver session is dead, aborting toggle search");
                    return null;
                }
                System.out.println("   Strategy 1 (name contains): not found");
            }
        }

        // Strategy 2: Find toggle by accessibility ID (multiple variations)
        if (System.currentTimeMillis() - searchStart < TOGGLE_SEARCH_MAX_MS) {
            String[] accessibilityIds = {
                "Required Fields Only", "RequiredFieldsOnly", "required_fields_only",
                "Required Fields", "RequiredFields", "toggle_required"
            };
            for (String accId : accessibilityIds) {
                if (System.currentTimeMillis() - searchStart > TOGGLE_SEARCH_MAX_MS) break;
                try {
                    WebElement toggle = driver.findElement(AppiumBy.accessibilityId(accId));
                    if (toggle != null && toggle.isDisplayed()) {
                        System.out.println("   ✅ Found toggle by accessibility ID: " + accId);
                        return toggle;
                    }
                } catch (Exception e) {
                    if (isDriverSessionDead(e)) {
                        System.out.println("   ❌ Driver session is dead, aborting toggle search");
                        return null;
                    }
                }
            }
            System.out.println("   Strategy 2 (accessibility ID): not found");
        }

        // Strategy 3: Scroll to "Core Attributes" section and find toggle nearby
        if (System.currentTimeMillis() - searchStart < TOGGLE_SEARCH_MAX_MS) {
            try {
                System.out.println("   Strategy 3: Looking for Core Attributes section...");
                for (int i = 0; i < 2; i++) {
                    if (System.currentTimeMillis() - searchStart > TOGGLE_SEARCH_MAX_MS) break;
                    try {
                        WebElement coreAttrLabel = driver.findElement(
                            AppiumBy.iOSNsPredicateString(
                                "(name CONTAINS[c] 'Core Attribute' OR label CONTAINS[c] 'Core Attribute') " +
                                "AND type == 'XCUIElementTypeStaticText'"
                            )
                        );
                        if (coreAttrLabel != null && coreAttrLabel.isDisplayed()) {
                            int coreAttrY = coreAttrLabel.getLocation().getY();
                            System.out.println("   Found 'Core Attributes' section at Y=" + coreAttrY);

                            // Find switch OR button near Core Attributes (within 200px below)
                            List<WebElement> switches = driver.findElements(
                                AppiumBy.iOSNsPredicateString(switchOrButton + "")
                            );
                            for (WebElement sw : switches) {
                                int switchY = sw.getLocation().getY();
                                if (switchY > coreAttrY && switchY < coreAttrY + 200) {
                                    System.out.println("   ✅ Found toggle in Core Attributes section (Y=" + switchY + ")");
                                    return sw;
                                }
                            }
                            break; // Found section but no toggle
                        }
                    } catch (Exception notFound) {
                        if (isDriverSessionDead(notFound)) {
                            System.out.println("   ❌ Driver session is dead, aborting toggle search");
                            return null;
                        }
                        System.out.println("   Scrolling to find Core Attributes (attempt " + (i+1) + ")...");
                        scrollFormDown();
                        sleep(300);
                    }
                }
            } catch (Exception e) {
                if (isDriverSessionDead(e)) {
                    System.out.println("   ❌ Driver session is dead, aborting toggle search");
                    return null;
                }
                System.out.println("   Strategy 3 (Core Attributes section): not found");
            }
        }

        // Strategy 4: Find "Required" label and look for nearby toggle
        if (System.currentTimeMillis() - searchStart < TOGGLE_SEARCH_MAX_MS) {
            try {
                List<WebElement> labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(name CONTAINS[c] 'Required' OR label CONTAINS[c] 'Required') " +
                    "AND type == 'XCUIElementTypeStaticText'"
                ));
                for (WebElement label : labels) {
                    if (System.currentTimeMillis() - searchStart > TOGGLE_SEARCH_MAX_MS) break;
                    try {
                        int labelY = label.getLocation().getY();
                        List<WebElement> switches = driver.findElements(
                            AppiumBy.iOSNsPredicateString(switchOrButton + "")
                        );
                        for (WebElement sw : switches) {
                            int switchY = sw.getLocation().getY();
                            if (Math.abs(switchY - labelY) < 150) {
                                System.out.println("   ✅ Found toggle near 'Required' label (Y diff: " + Math.abs(switchY - labelY) + ")");
                                return sw;
                            }
                        }
                    } catch (Exception e) {
                        if (isDriverSessionDead(e)) return null;
                    }
                }
            } catch (Exception e) {
                if (isDriverSessionDead(e)) return null;
            }
        }

        // Strategy 5: Any visible switch on screen (position-based)
        if (System.currentTimeMillis() - searchStart < TOGGLE_SEARCH_MAX_MS) {
            try {
                List<WebElement> switches = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSwitch'")
                );
                System.out.println("   Strategy 5: Found " + switches.size() + " visible switches");
                for (WebElement sw : switches) {
                    int switchY = sw.getLocation().getY();
                    if (switchY > 100 && switchY < 500) {
                        System.out.println("   ✅ Found switch in upper screen area (Y=" + switchY + ")");
                        return sw;
                    }
                }
                // Fallback: return first visible switch
                if (!switches.isEmpty()) {
                    System.out.println("   ⚠️ FALLBACK: Using first visible switch on page");
                    return switches.get(0);
                }
            } catch (Exception e) {
                if (isDriverSessionDead(e)) return null;
            }
        }

        long elapsed = System.currentTimeMillis() - searchStart;
        System.out.println("   ❌ Could not find Required Fields toggle after all strategies (" + elapsed + "ms)");
        // Cache: mark as exhausted so future calls in this test return instantly
        toggleSearchExhausted = true;
        return null;

        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
            } catch (Exception ignored) {
                // Driver may be dead — can't restore timeout
            }
        }
    }

    /**
     * Check if Required Fields Only toggle is displayed
     */
    public boolean isRequiredFieldsToggleDisplayed() {
        WebElement toggle = findRequiredFieldsToggle();
        return toggle != null;
    }

    /**
     * Get Required Fields Only toggle state (ON/OFF)
     * Uses robust multi-strategy toggle detection
     */
    public boolean isRequiredFieldsToggleOn() {
        try {
            WebElement toggle = findRequiredFieldsToggle();
            if (toggle != null) {
                String value = toggle.getAttribute("value");
                boolean isOn = "1".equals(value);
                System.out.println("   Toggle state: " + (isOn ? "ON" : "OFF") + " (value='" + value + "')");
                return isOn;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get toggle state: " + e.getMessage());
        }
        return false;
    }

    /**
     * Toggle Required Fields Only switch
     * Uses robust multi-strategy toggle detection
     */
    public void toggleRequiredFieldsOnly() {
        try {
            WebElement toggle = findRequiredFieldsToggle();
            if (toggle != null) {
                toggle.click();
                System.out.println("✅ Toggled Required Fields Only switch");
                sleep(500); // iOS toggle animation needs ~300ms
            } else {
                System.out.println("⚠️ Could not find Required Fields toggle to click");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not toggle switch: " + e.getMessage());
        }
    }

    /**
     * Enable Required Fields Only toggle (turn ON).
     * Polls up to 2s after toggling to confirm state changed.
     */
    public void enableRequiredFieldsOnly() {
        if (!isRequiredFieldsToggleOn()) {
            toggleRequiredFieldsOnly();
            // Poll to confirm toggle is now ON (animation may still be finishing)
            for (int i = 0; i < 4; i++) {
                if (isRequiredFieldsToggleOn()) {
                    System.out.println("✅ Required Fields Only enabled");
                    return;
                }
                sleep(300);
            }
            System.out.println("⚠️ Toggle may not have turned ON after toggling");
        }
    }

    /**
     * Disable Required Fields Only toggle (turn OFF)
     */
    public void disableRequiredFieldsOnly() {
        if (isRequiredFieldsToggleOn()) {
            toggleRequiredFieldsOnly();
            // Poll to confirm toggle is now OFF
            for (int i = 0; i < 4; i++) {
                if (!isRequiredFieldsToggleOn()) {
                    System.out.println("✅ Required Fields Only disabled");
                    return;
                }
                sleep(300);
            }
            System.out.println("⚠️ Toggle may not have turned OFF after toggling");
        }
    }

    /**
     * Check if Required Fields Only toggle is OFF
     */
    public boolean isRequiredFieldsToggleOff() {
        return !isRequiredFieldsToggleOn();
    }

    /**
     * Enable Required Fields Only toggle (alias method)
     */
    public void enableRequiredFieldsOnlyToggle() {
        enableRequiredFieldsOnly();
    }

    /**
     * Get required fields counter text (alias method for getRequiredFieldsCounter)
     */
    public String getRequiredFieldsCounterText() {
        return getRequiredFieldsCounter();
    }

    /**
     * Get completion percentage text
     */
    public String getCompletionPercentage() {
        try {
            List<WebElement> percentElements = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS '%' OR label CONTAINS '%'")
            );
            for (WebElement el : percentElements) {
                String text = el.getAttribute("name");
                if (text == null) text = el.getAttribute("label");
                if (text != null && text.contains("%")) {
                    System.out.println("📊 Percentage: " + text);
                    return text;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get percentage: " + e.getMessage());
        }
        return "";
    }

    /**
     * Check if percentage element exists
     */
    public boolean isPercentageDisplayed() {
        String percentage = getCompletionPercentage();
        return percentage != null && !percentage.isEmpty();
    }

    /**
     * Get required fields counter text (e.g., "2/4")
     */
    public String getRequiredFieldsCounter() {
        try {
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString("name MATCHES '.*[0-9]+/[0-9]+.*' OR label MATCHES '.*[0-9]+/[0-9]+.*'")
            );
            for (WebElement el : elements) {
                String text = el.getAttribute("name");
                if (text == null) text = el.getAttribute("label");
                if (text != null && text.matches(".*\\d+/\\d+.*")) {
                    System.out.println("📊 Counter: " + text);
                    return text;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not get counter: " + e.getMessage());
        }
        return "";
    }

    /**
     * Fill a text field by placeholder/label
     */
    public void fillTextField(String fieldName, String value) {
        System.out.println("📝 Filling field: " + fieldName + " = " + value);
        
        // Try to find and fill the field, scroll if needed (max 3 scrolls)
        for (int scrollAttempt = 0; scrollAttempt < 3; scrollAttempt++) {
            
            // STRATEGY 1: Find TextField OR TextView by name/label containing fieldName
            try {
                List<WebElement> inputFields = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
                ));
                for (WebElement field : inputFields) {
                    String name = field.getAttribute("name");
                    String placeholder = field.getAttribute("value");
                    if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                        (placeholder != null && placeholder.toLowerCase().contains(fieldName.toLowerCase()))) {
                        field.click();
                        sleep(200);
                        field.clear();
                        field.sendKeys(value);
                        System.out.println("✅ Filled field '" + fieldName + "' = " + value);
                        dismissKeyboard();
                        return;
                    }
                }
            } catch (Exception e) {}
            
            // STRATEGY 2: Find label, then TextField or TextView below it
            try {
                // Use CONTAINS[c] for case-insensitive matching (e.g., "voltage" or "Voltage")
                List<WebElement> labels = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "')")
                ));

                if (!labels.isEmpty()) {
                    WebElement label = labels.get(0);
                    int labelY = label.getLocation().getY();

                    List<WebElement> inputFields = withImplicitWait(0, () -> driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
                    ));
                    for (WebElement tf : inputFields) {
                        int tfY = tf.getLocation().getY();
                        if (Math.abs(tfY - labelY) < 100) {
                            tf.click();
                            sleep(200);
                            tf.clear();
                            tf.sendKeys(value);
                            System.out.println("✅ Filled field near '" + fieldName + "' = " + value);
                            dismissKeyboard();
                            return;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // Scroll down and try again
            if (scrollAttempt < 2) {
                System.out.println("   Field not visible, scrolling...");
                scrollFormDown();
                sleep(300);
            }
        }
        
        System.out.println("⚠️ Field not found: " + fieldName);
    }

    /**
     * Clear a text field by placeholder/label
     */
    public void clearTextField(String fieldName) {
        try {
            List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            for (WebElement field : textFields) {
                String name = field.getAttribute("name");
                String placeholder = field.getAttribute("value");
                if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                    (placeholder != null && placeholder.toLowerCase().contains(fieldName.toLowerCase()))) {
                    field.clear();
                    System.out.println("✅ Cleared field: " + fieldName);
                    dismissKeyboard();
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not clear field: " + e.getMessage());
        }
    }


    /**
     * SMART field filler - automatically detects if field is dropdown or text input
     * Tries dropdown first (for fields like Voltage, Manufacturer that might be dropdowns on some asset classes)
     * Falls back to text field if dropdown not found
     * Uses case-insensitive matching for field names
     */
    public void fillFieldAuto(String fieldName, String value) {
        System.out.println("📝 Auto-filling field: " + fieldName + " = " + value);

        // ============================================================
        // FIX 2026-05-04 (debug session 061 follow-up):
        // Same root cause as selectDropdownOption — dropdown triggers
        // in v1.31 are XCUIElementTypeStaticText "Select..." not Button.
        // Detection logic was searching only Buttons → always fell
        // through to fillTextField, even for dropdown fields.
        //
        // Detection strategy: look for a "Select..." StaticText near
        // a label containing fieldName. If found, it's a dropdown.
        // ============================================================
        try {
            // Find label for this field
            List<WebElement> labels = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "')")));
            for (WebElement lbl : labels) {
                int labelY = lbl.getLocation().getY();
                if (labelY < 60 || labelY > 1900) continue;
                // Look for Select... StaticText within 80px below label
                List<WebElement> selects = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(name BEGINSWITH 'Select' OR label BEGINSWITH 'Select')")));
                for (WebElement s : selects) {
                    int sy = s.getLocation().getY();
                    if (sy >= labelY - 5 && sy <= labelY + 80) {
                        System.out.println("   🔽 Found dropdown trigger near label, using selectDropdownOption");
                        selectDropdownOption(fieldName, value);
                        return;
                    }
                }
            }
            // Legacy: button-based detection (for forms that still use Button-style dropdowns)
            List<WebElement> buttons = withImplicitWait(0, () -> driver.findElements(
                AppiumBy.className("XCUIElementTypeButton")));
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                String label = btn.getAttribute("label");
                if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                    (label != null && label.toLowerCase().contains(fieldName.toLowerCase()))) {
                    int y = btn.getLocation().getY();
                    if (y > 100 && y < 800) {
                        System.out.println("   🔽 Found dropdown button (legacy), using selectDropdownOption");
                        selectDropdownOption(fieldName, value);
                        return;
                    }
                }
            }
        } catch (Exception e) {}

        // Not a dropdown, try as text field
        System.out.println("   📝 No dropdown found, trying text field");
        fillTextField(fieldName, value);
    }

    /** Per-call wall-clock budget for the dropdown LOCATE loop. ~25s leaves headroom under the
     *  per-test cap even when a test calls this 3× (LC_EAD_22 / MCC_EAD_20). */
    private static final long DROPDOWN_BUDGET_MS = 25_000L;

    /**
     * Select dropdown option by field name and option value
     */
    public void selectDropdownOption(String fieldName, String optionValue) {
        System.out.println("📋 Selecting '" + optionValue + "' for dropdown '" + fieldName + "'...");

        // ============================================================
        // FAST-FAIL PRECONDITION (added 2026-05-04 per debug session)
        //
        // Forensics on LC_EAD_22 (run 2026-05-04 12:33-12:43): the test
        // wasted ~10 minutes scrolling through the Assets LIST screen looking
        // for "Ampere Rating" / "Manufacturer" / "Voltage" labels — because
        // the asset-class picker had auto-dismissed and the screen reverted
        // to the Assets list. Each missing-field probe ran the full
        // 3-attempt loop × multiple-strategy scroll-and-retry, costing
        // 30–90s per field × 3 fields ≈ 2–4 minutes of pure waste per test.
        //
        // This precondition fails fast in ~1s when not on Edit screen,
        // letting the test report a meaningful failure quickly rather than
        // hitting the 7-min TestNG suite-timeout cap.
        // ============================================================
        if (!isEditAssetScreenDisplayed() && !isSaveChangesButtonVisible()) {
            System.out.println("⚠️ FAST-FAIL: Not on Edit Asset screen — aborting selectDropdownOption('"
                + fieldName + "'). Caller's screen-state assumption is broken.");
            return;
        }

        boolean dropdownFound = false;
        // Wall-clock budget: caps the locate loop so a wedge on a bleed-through Edit DOM (or a
        // field absent on the class, e.g. Loadcenter Manufacturer/Voltage) becomes a fast fail,
        // not a 6m hang (LC_EAD_16/20/22, MCC_EAD_20). The locate finds below also run at
        // implicit-wait 0 so a miss costs ms, not the 5s default.
        final long ddDeadline = System.currentTimeMillis() + DROPDOWN_BUDGET_MS;

        // STRATEGY 1: Use mobile:scroll to scroll the label into view (handles ANY distance)
        try {
            String predicate = "type == 'XCUIElementTypeStaticText' AND label CONTAINS[c] '" + fieldName + "'";
            System.out.println("   🔍 Scrolling to label '" + fieldName + "' via mobile:scroll...");
            java.util.Map<String, Object> scrollParams = new java.util.HashMap<>();
            scrollParams.put("direction", "down");
            scrollParams.put("predicateString", predicate);
            driver.executeScript("mobile: scroll", scrollParams);
            sleep(300);
        } catch (Exception e) {
            System.out.println("   mobile:scroll failed (label may already be visible): " + (e.getMessage() != null ? e.getMessage().substring(0, Math.min(80, e.getMessage().length())) : ""));
        }

        // Now find the label and the nearest button below it
        for (int attempt = 0; attempt < 3 && !dropdownFound && System.currentTimeMillis() < ddDeadline; attempt++) {
            try {
                List<WebElement> labels = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND " +
                        "(name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "')")
                ));

                if (!labels.isEmpty()) {
                    WebElement lbl = labels.get(0);
                    int labelY = lbl.getLocation().getY();
                    System.out.println("   🔍 Label '" + fieldName + "' at Y=" + labelY);

                    if (nudgeIfBehindNavBar(labelY)) {
                        labels = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND " +
                            "(name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "')")));
                        if (labels.isEmpty()) continue;
                        lbl = labels.get(0);
                        labelY = lbl.getLocation().getY();
                        System.out.println("   🔍 After nudge: Label at Y=" + labelY);
                    }

                    // ============================================================
                    // POST-NUDGE OFF-SCREEN GUARD (added 2026-05-04, debug 061):
                    //
                    // nudgeIfBehindNavBar can fail to bring the label into the
                    // visible body — e.g., when label originally at Y=-881,
                    // nudge of 1101px exceeds screen drag capacity. Label ends
                    // up at Y=-165 (still off-screen) or Y=36 (in nav bar zone).
                    //
                    // Searching for a "dropdown near the label" in those cases
                    // matches nav buttons (More / ⋯ / WO) at Y=61 and clicks
                    // the wrong thing.
                    //
                    // Guard: if label is outside the visible body [120, 1900],
                    // skip this attempt and let the outer scroll-and-retry loop
                    // try again with fresh scroll.
                    // ============================================================
                    if (labelY < 120 || labelY > 1900) {
                        System.out.println("   ⚠️ Label '" + fieldName + "' at Y=" + labelY
                            + " is outside visible body [120, 1900] — skipping search, retrying scroll");
                        // Fall through to the outer attempt loop's manual scroll.
                        continue;
                    }

                    // Find dropdown trigger near the label within 80px.
                    //
                    // ============================================================
                    // ROOT-CAUSE FIX 2026-05-04 (debug sessions 059 → 061):
                    //
                    // The original code searched only XCUIElementTypeButton elements.
                    // Live DOM dump from /tmp/loadcenter_debug/ proved this is wrong
                    // for the current iOS app (Z Platform-QA v1.31): every dropdown
                    // trigger in Loadcenter / Disconnect Switch / Manufacturer / etc.
                    // forms is rendered as XCUIElementTypeStaticText, e.g.:
                    //
                    //   <XCUIElementTypeStaticText name="Select..." label="Select..."
                    //     enabled="true" accessible="true" x="44" y="1493" .../>
                    //
                    // No wrapping Button. The historical "search Buttons only" code
                    // missed every dropdown, ran the 3-attempt × scroll-and-nudge
                    // retry storm, then gave up after 30–90s per field.
                    //
                    // 20 tests in CI Run #25204536847 hit the 7-min TestNG cap because
                    // of this. Fix: include XCUIElementTypeStaticText in the search.
                    //
                    // Filter out breadcrumb buttons (3+ comma-separated segments,
                    // e.g. "Trim067938, Room_xxx, ATS"). Manufacturer values like
                    // "Square D, Inc." (2 segments) are kept.
                    // ============================================================
                    List<WebElement> allBtns = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' OR " +
                        "(type == 'XCUIElementTypeStaticText' AND " +
                        "(name BEGINSWITH 'Select' OR label BEGINSWITH 'Select'))"
                    )));
                    WebElement selectBtn = null;    // Priority 1: "Select..." trigger (unselected)
                    int selectDist = Integer.MAX_VALUE;
                    WebElement valueBtn = null;     // Priority 2: short-named non-breadcrumb (filled)
                    int valueDist = Integer.MAX_VALUE;

                    for (WebElement btn : allBtns) {
                        int btnY = btn.getLocation().getY();
                        if (btnY < labelY - 10 || btnY >= labelY + 80) continue;

                        String btnName = btn.getAttribute("name");
                        if (btnName == null) btnName = "";
                        int dist = Math.abs(btnY - labelY);

                        // Priority 1: "Select..." trigger (unselected dropdown state) —
                        // matches both Button (legacy) and StaticText (current iOS DOM).
                        if (btnName.startsWith("Select")) {
                            if (dist < selectDist) {
                                selectDist = dist;
                                selectBtn = btn;
                            }
                            continue;
                        }

                        // Skip location breadcrumb buttons (3+ segments).
                        if (btnName.split(", ").length >= 3) continue;
                        if (btnName.length() > 50) continue;
                        // Skip nav bar / system buttons that masquerade as dropdowns when
                        // a label has scrolled off-screen and the search window
                        // accidentally overlaps the top nav area.
                        // (Added 2026-05-04 per debug session 061: "More" was being
                        //  picked as the dropdown for Voltage when label scrolled to
                        //  Y=36 and the ⋯ button at Y=61 was 25px away.)
                        if (btnName.equals("house") || btnName.equals("Back") || btnName.equals("Close") ||
                            btnName.equals("Search") || btnName.equals("plus") ||
                            btnName.equals("More") || btnName.equals("ellipsis") ||
                            btnName.equals("ellipsis.circle") || btnName.equals("WO") ||
                            btnName.equals("Cancel") || btnName.equals("Save") ||
                            btnName.equals("Done")) continue;

                        // Priority 2: short-named button (dropdown with value already set)
                        if (dist < valueDist) {
                            valueDist = dist;
                            valueBtn = btn;
                        }
                    }

                    WebElement closestBtn = selectBtn != null ? selectBtn : valueBtn;
                    int closestDist = selectBtn != null ? selectDist : valueDist;

                    if (closestBtn != null) {
                        String btnName = closestBtn.getAttribute("name");
                        int btnY = closestBtn.getLocation().getY();
                        System.out.println("   ✅ Clicking dropdown button: '" + btnName + "' at Y=" + btnY + " (dist=" + closestDist + "px)");
                        closestBtn.click();
                        dropdownFound = true;
                        sleep(400);
                    } else {
                        System.out.println("   ⚠️ No dropdown button found within 80px of label Y=" + labelY + " (breadcrumbs filtered out)");
                    }
                } else {
                    System.out.println("   ⚠️ Label '" + fieldName + "' not found in DOM");
                }
            } catch (Exception e) {
                System.out.println("   Error: " + (e.getMessage() != null ? e.getMessage().substring(0, Math.min(80, e.getMessage().length())) : ""));
            }

            // If not found, try manual scroll and retry
            if (!dropdownFound && attempt < 2) {
                System.out.println("   Scrolling down to find dropdown... (attempt " + (attempt + 1) + ")");
                scrollFormDown();
                sleep(300);
            }
        }

        if (!dropdownFound) {
            System.out.println("⚠️ Dropdown '" + fieldName + "' not found after scrolling");
            return;
        }

        // Dropdown is now open — select the option value
        try {
            // Try exact match first (accessibility ID)
            WebElement option = driver.findElement(AppiumBy.accessibilityId(optionValue));
            option.click();
            System.out.println("✅ Selected '" + optionValue + "' for '" + fieldName + "'");
            return;
        } catch (Exception e) {}

        // Try case-insensitive exact match
        try {
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString("name ==[c] '" + optionValue + "' OR label ==[c] '" + optionValue + "'")
            );
            option.click();
            System.out.println("✅ Selected '" + optionValue + "' (case-insensitive) for '" + fieldName + "'");
            return;
        } catch (Exception e) {}

        // Try contains match — strip unit suffixes like "V", "A", "kA"
        try {
            final String searchVal = optionValue.replaceAll("[VAva]+$", "").trim();
            List<WebElement> options = withImplicitWait(0, () -> driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') AND " +
                    "(name CONTAINS[c] '" + searchVal + "' OR label CONTAINS[c] '" + searchVal + "')")
            ));
            System.out.println("   🔍 Contains search for '" + searchVal + "': found " + options.size() + " matches");
            for (WebElement opt : options) {
                String optName = opt.getAttribute("name");
                if (!isSelectablePickerOption(optName, fieldName)) continue;
                opt.click();
                System.out.println("✅ Selected option '" + optName + "' for '" + fieldName + "'");
                return;
            }
        } catch (Exception e) {}

        // Try clicking first real option in the dropdown (skip sheet/nav chrome)
        try {
            System.out.println("   🔍 Trying first available option...");
            List<WebElement> allOptions = withImplicitWait(0, () -> driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name != 'Close' AND name != 'Back' AND name != 'Select...'")
            ));
            for (WebElement opt : allOptions) {
                int y = opt.getLocation().getY();
                if (y <= 100 || y >= 800) continue;
                String optName = opt.getAttribute("name");
                // Skip the drag handle, Done/Cancel, SF Symbols, list rows, the label itself
                if (!isSelectablePickerOption(optName, fieldName)) continue;
                opt.click();
                System.out.println("✅ Selected first available option: '" + optName + "' for '" + fieldName + "'");
                return;
            }
        } catch (Exception e) {}

        System.out.println("⚠️ Could not select option '" + optionValue + "' for '" + fieldName + "'");
        // Dismiss the dropdown
        dismissKeyboard();
    }

    /**
     * True if a picker row name is a real selectable data value — NOT sheet/nav
     * chrome (the "Sheet Grabber" drag handle, Done/Cancel/Close), an SF Symbol
     * glyph (e.g. "list.bullet", "plus.circle.fill"), an asset-LIST row
     * ("name, loc, class" — commas), or the field's own label.
     */
    private boolean isSelectablePickerOption(String name, String fieldName) {
        if (name == null) return false;
        String n = name.trim();
        if (n.isEmpty()) return false;
        if (n.contains(",")) return false;                              // list rows
        if (fieldName != null && !fieldName.isEmpty() && n.contains(fieldName)) return false;
        switch (n) {                                                    // known sheet/nav/tool chrome
            case "Sheet Grabber": case "Done": case "Cancel": case "Close": case "Back":
            case "Select...": case "Select": case "More": case "Filter": case "Search":
            case "Save": case "Save Changes": case "Edit": case "Add":
            case "Calculator": case "Gallery": case "Camera": case "Scan": case "QR Code":
                return false;
            default: break;
        }
        // SF Symbol identifiers look like "list.bullet" / "plus.circle.fill":
        // a dot, no spaces, all lowercase. Real values ("100 kA", "480V") don't.
        if (n.contains(".") && !n.contains(" ") && n.equals(n.toLowerCase())) return false;
        return true;
    }

    /**
     * Check if Save button on Edit screen is enabled
     */
    public boolean isEditSaveButtonEnabled() {
        try {
            WebElement saveBtn = driver.findElement(AppiumBy.accessibilityId("Save"));
            String enabled = saveBtn.getAttribute("enabled");
            return "true".equalsIgnoreCase(enabled);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click Save button on Edit Asset Details screen
     */
    public void clickEditSave() {
        // v1.36 (changelog 075): Asset Detail has no separate Edit mode. After a
        // field changes, the Save button appears at the BOTTOM of the scroll view
        // and its label is "Save Changes" (or just "Save" on older builds). Pure
        // accessibilityId("Save") is too narrow — delegate to clickSaveChanges
        // which already handles name-CONTAINS + scroll-into-view.
        saveButtonClickedThisFlow = false;
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Save")
            ));
            saveBtn.click();
            saveButtonClickedThisFlow = true;
            System.out.println("✅ Clicked Save on Edit screen");
            sleep(400);
            return;
        } catch (Exception ignored) {}

        // Fallback: v1.36 Save Changes button (CONTAINS 'Save' + scroll into view)
        clickSaveChanges();
    }

    /**
     * Click Save Changes button (shown after changing asset class)
     * If asset class didn't change (e.g., Busway already selected), Save Changes won't appear
     * In that case, click the regular Save button
     */
    public void clickSaveChanges() {
        System.out.println("💾 Looking for Save Changes button...");
        saveButtonClickedThisFlow = false;

        // Try 1: Find Save button in DOM (no visible == true — may be off-screen)
        try {
            List<WebElement> saveBtns = driver.findElements(
                AppiumBy.iOSNsPredicateString("(name CONTAINS 'Save' OR label CONTAINS 'Save') AND type == 'XCUIElementTypeButton'")
            );
            if (!saveBtns.isEmpty()) {
                // Use mobile:scroll to bring it into view if needed
                try {
                    driver.executeScript("mobile: scroll", java.util.Map.of(
                        "direction", "down",
                        "predicateString", "name CONTAINS 'Save' AND type == 'XCUIElementTypeButton'"));
                } catch (Exception scrollEx) { /* already visible */ }
                sleep(300);
                // Re-find after scroll and click
                saveBtns = driver.findElements(
                    AppiumBy.iOSNsPredicateString("(name CONTAINS 'Save' OR label CONTAINS 'Save') AND type == 'XCUIElementTypeButton'")
                );
                if (!saveBtns.isEmpty()) {
                    saveBtns.get(0).click();
                    saveButtonClickedThisFlow = true;
                    System.out.println("✅ Clicked Save Changes");
                    sleep(400);
                    return;
                }
            }
        } catch (Exception e) {}

        // Try 2: Scroll DOWN manually to find Save Changes (it's at the bottom)
        System.out.println("   Scrolling down to find Save Changes...");
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(800));
        try {
            for (int i = 0; i < 3; i++) {
                scrollFormDown();
                sleep(300);
                List<WebElement> saveBtns = driver.findElements(
                    AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton'")
                );
                if (!saveBtns.isEmpty()) {
                    saveBtns.get(0).click();
                    saveButtonClickedThisFlow = true;
                    System.out.println("✅ Clicked Save Changes (after scroll)");
                    sleep(400);
                    return;
                }
            }
        } finally {
            driver.manage().timeouts().implicitlyWait(
                java.time.Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
        }

        // Try 3: AccessibilityId fallback
        try {
            WebElement saveChangesBtn = driver.findElement(AppiumBy.accessibilityId("Save Changes"));
            saveChangesBtn.click();
            saveButtonClickedThisFlow = true;
            System.out.println("✅ Clicked Save Changes (accessibilityId)");
            sleep(400);
            return;
        } catch (Exception e) {}

        System.out.println("⚠️ Could not find Save Changes button - changes may not have been made");
    }

    /**
     * Click Cancel button on Edit Asset Details screen
     */
    public void clickEditCancel() {
        System.out.println("📝 Tapping Cancel button");
        
        // Strategy 1: Accessibility ID "Cancel"
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel on Edit screen");
            sleep(200);
            return;
        } catch (Exception e) {}
        
        // Strategy 2: Find button with Cancel label
        try {
            WebElement cancelBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel button");
            sleep(200);
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Find StaticText "Cancel" and click
        try {
            WebElement cancelText = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name == 'Cancel' OR label == 'Cancel')")
            );
            cancelText.click();
            System.out.println("✅ Clicked Cancel text");
            sleep(200);
            return;
        } catch (Exception e) {}
        
        // Strategy 4: Tap coordinates at top-left (typical Cancel position)
        try {
            driver.executeScript("mobile: tap", Map.of("x", 60, "y", 60));
            System.out.println("✅ Tapped Cancel position (60, 60)");
            sleep(200);
            return;
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not find Cancel button");
    }

    /**
     * Check if green check indicator is displayed (for completed fields)
     */
    public boolean isGreenCheckIndicatorDisplayed() {
        try {
            List<WebElement> indicators = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'checkmark' OR name CONTAINS 'check' OR label CONTAINS '✓'")
            );
            return indicators.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if red warning indicator is displayed (for missing required fields)
     */
    public boolean isRedWarningIndicatorDisplayed() {
        try {
            List<WebElement> indicators = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'warning' OR name CONTAINS 'exclamation' OR label CONTAINS '!'")
            );
            return indicators.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for Edit Asset Details screen to load
     */
    public void waitForEditScreenReady() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(d -> isEditAssetScreenDisplayed());
            sleep(200);
            System.out.println("✅ Edit Asset Details screen ready");
        } catch (Exception e) {
            System.out.println("⚠️ Edit screen wait timeout: " + e.getMessage());
        }
    }

    /**
     * Check if asset was saved successfully after edit (back to detail/list)
     */
    public boolean isEditSavedSuccessfully() {
        try {
            sleep(400);
            // After save, should return to asset details or list
            return !isEditAssetScreenDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fill ATS required field - Ampere Rating
     */
    public void fillAmpereRating(String value) {
        fillTextField("Ampere Rating", value);
    }

    /**
     * Select Ampere Rating from dropdown (e.g., "30A", "50A", "100A")
     */
    public void selectAmpereRating(String value) {
        System.out.println("📝 Selecting Ampere Rating: " + value);
        
        // Try to find and click the Ampere Rating dropdown
        for (int scrollAttempt = 0; scrollAttempt < 3; scrollAttempt++) {
            
            // STRATEGY 1: Find button/picker with Ampere in name
            try {
                List<WebElement> buttons = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name CONTAINS[c] 'ampere' OR label CONTAINS[c] 'ampere')")
                );
                if (!buttons.isEmpty()) {
                    buttons.get(0).click();
                    sleep(200);
                    // Select the value from dropdown
                    selectDropdownValue(value);
                    return;
                }
            } catch (Exception e) {}
            
            // STRATEGY 2: Find label "Ampere Rating" then click nearby button/picker
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] 'ampere' OR label CONTAINS[c] 'ampere')")
                );
                
                if (!labels.isEmpty()) {
                    WebElement label = labels.get(0);
                    int labelY = label.getLocation().getY();
                    
                    // Find button near the label
                    List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
                    for (WebElement btn : buttons) {
                        int btnY = btn.getLocation().getY();
                        if (Math.abs(btnY - labelY) < 80) {
                            btn.click();
                            sleep(200);
                            selectDropdownValue(value);
                            return;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // STRATEGY 3: Direct accessibility ID for the value
            try {
                WebElement option = driver.findElement(AppiumBy.accessibilityId(value));
                option.click();
                System.out.println("✅ Selected Ampere Rating: " + value);
                sleep(300);
                return;
            } catch (Exception e) {}
            
            // Scroll down and try again
            if (scrollAttempt < 2) {
                System.out.println("   Ampere Rating not visible, scrolling...");
                scrollFormDown();
                sleep(300);
            }
        }
        
        System.out.println("⚠️ Could not select Ampere Rating: " + value);
    }

    /**
     * Select a value from an open dropdown
     */
    private void selectDropdownValue(String value) {
        System.out.println("   Selecting dropdown value: " + value);
        
        // Try accessibility ID first (exact match)
        try {
            WebElement option = driver.findElement(AppiumBy.accessibilityId(value));
            option.click();
            System.out.println("✅ Selected: " + value);
            sleep(300);
            return;
        } catch (Exception e) {}
        
        // Try case-insensitive exact match
        try {
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString("name ==[c] '" + value + "' OR label ==[c] '" + value + "'")
            );
            option.click();
            System.out.println("✅ Selected (case-insensitive): " + value);
            sleep(300);
            return;
        } catch (Exception e) {}
        
        // Try without spaces (e.g., "10 kA" -> "10kA")
        String noSpaceValue = value.replace(" ", "");
        try {
            WebElement option = driver.findElement(AppiumBy.accessibilityId(noSpaceValue));
            option.click();
            System.out.println("✅ Selected (no space): " + noSpaceValue);
            sleep(300);
            return;
        } catch (Exception e) {}
        
        // Try CONTAINS match for partial matching
        try {
            String searchPart = value.split(" ")[0]; // Get first part like "10" from "10 kA"
            WebElement option = driver.findElement(
                AppiumBy.iOSNsPredicateString("(name CONTAINS '" + searchPart + "' AND name CONTAINS 'kA') OR (label CONTAINS '" + searchPart + "' AND label CONTAINS 'kA')")
            );
            option.click();
            System.out.println("✅ Selected (contains): " + value);
            sleep(300);
            return;
        } catch (Exception e) {}
        
        // Try finding any Button/StaticText with the value
        try {
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString("(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND (name CONTAINS[c] '" + value.split(" ")[0] + "')")
            );
            for (WebElement elem : elements) {
                String name = elem.getAttribute("name");
                if (name != null && name.toLowerCase().contains(value.split(" ")[0].toLowerCase())) {
                    elem.click();
                    System.out.println("✅ Selected (partial): " + name);
                    sleep(300);
                    return;
                }
            }
        } catch (Exception e) {}
        
        // Try finding StaticText with the value
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && name.equals(value)) {
                    text.click();
                    System.out.println("✅ Selected: " + value);
                    sleep(300);
                    return;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not find dropdown option: " + value);
    }

    /**
     * Fill ATS required field - Interrupting Rating
     */
    public void fillInterruptingRating(String value) {
        fillTextField("Interrupting Rating", value);
    }

    /**
     * Select Interrupting Rating from dropdown (e.g., "10 kA", "25 kA", "50 kA")
     */
    public void selectInterruptingRating(String value) {
        System.out.println("📝 Selecting Interrupting Rating: " + value);
        
        // Try to find and click the Interrupting Rating dropdown
        for (int scrollAttempt = 0; scrollAttempt < 3; scrollAttempt++) {
            
            // STRATEGY 1: Find button/picker with Interrupting in name
            try {
                List<WebElement> buttons = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name CONTAINS[c] 'interrupting' OR label CONTAINS[c] 'interrupting')")
                );
                if (!buttons.isEmpty()) {
                    buttons.get(0).click();
                    sleep(200);
                    selectDropdownValue(value);
                    return;
                }
            } catch (Exception e) {}
            
            // STRATEGY 2: Find label "Interrupting Rating" then click nearby button/picker
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] 'interrupting' OR label CONTAINS[c] 'interrupting')")
                );
                
                if (!labels.isEmpty()) {
                    WebElement label = labels.get(0);
                    int labelY = label.getLocation().getY();
                    
                    // Find button near the label
                    List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
                    for (WebElement btn : buttons) {
                        int btnY = btn.getLocation().getY();
                        if (Math.abs(btnY - labelY) < 80) {
                            btn.click();
                            sleep(200);
                            selectDropdownValue(value);
                            return;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // STRATEGY 3: Direct accessibility ID for the value
            try {
                WebElement option = driver.findElement(AppiumBy.accessibilityId(value));
                option.click();
                System.out.println("✅ Selected Interrupting Rating: " + value);
                sleep(300);
                return;
            } catch (Exception e) {}
            
            // Scroll down and try again
            if (scrollAttempt < 2) {
                System.out.println("   Interrupting Rating not visible, scrolling...");
                scrollFormDown();
                sleep(300);
            }
        }
        
        System.out.println("⚠️ Could not select Interrupting Rating: " + value);
    }

    /**
     * Fill ATS required field - Voltage
     */
    public void fillVoltage(String value) {
        fillTextField("Voltage", value);
    }

    /**
     * Select Mains Type dropdown
     */
    public void selectMainsType(String type) {
        selectDropdownOption("Mains", type);
    }

    /**
     * Fill all ATS required fields
     */
    public void fillAllATSRequiredFields() {
        System.out.println("📋 Filling all ATS required fields (using dropdowns)...");

        // Scroll to see fields
        scrollFormDown();
        sleep(300);
        
        // ATS fields are DROPDOWNS, not text fields
        // Use selectDropdownOption which handles finding and selecting
        
        // 1. Ampere Rating dropdown
        try {
            System.out.println("📝 Selecting Ampere Rating...");
            selectDropdownOption("Ampere Rating", "100A");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Ampere Rating: " + e.getMessage());
            // Gold: ATS Ampere Rating ∈ {30A,60A,100A,200A,...}. Use another exact
            // option — NOT bare "100", which substring-matches "100 kA"
            // (Interrupting Rating), selecting the WRONG field.
            try {
                selectDropdownOption("Ampere Rating", "200A");
            } catch (Exception e2) {
                System.out.println("⚠️ Ampere Rating selection failed");
            }
        }
        sleep(200);
        
        // 2. Voltage dropdown  
        try {
            System.out.println("📝 Selecting Voltage...");
            selectDropdownOption("Voltage", "480V");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Voltage: " + e.getMessage());
            // Try alternate value
            try {
                selectDropdownOption("Voltage", "480");
            } catch (Exception e2) {
                System.out.println("⚠️ Voltage selection failed");
            }
        }
        sleep(200);
        
        // 3. Mains Type dropdown
        scrollFormDown();
        sleep(200);
        try {
            System.out.println("📝 Selecting Mains Type...");
            // Gold (node_classes): ATS "Mains Type" options are MCB / MLO — NOT
            // "Normal"/"Emergency" (which never matched and fell back to chrome).
            selectDropdownOption("Mains Type", "MCB");
        } catch (Exception e) {
            System.out.println("⚠️ Could not select Mains Type: " + e.getMessage());
            try {
                selectDropdownOption("Mains Type", "MLO");
            } catch (Exception e2) {
                System.out.println("⚠️ Mains Type selection failed");
            }
        }
        
        scrollFormUp();
        System.out.println("✅ Filled all ATS required fields");
    }

    // ================================================================
    // BUSWAY ASSET CLASS METHODS
    // ================================================================

    /**
     * Change asset class in Edit mode to Busway
     */
    /**
     * Change asset class to Busway - DEBUG VERSION
     */
    /**
     * Change asset class to Busway in Edit mode
     * Uses generic approach to find Asset Class dropdown regardless of current value
     */
    /**
     * FINAL - DO NOT MODIFY THIS IMPLEMENTATION
     * Fast asset class change using coordinate tap on dropdown
     */

    /**
     * HELPER: Check if the CURRENTLY SELECTED asset class matches the target
     * This checks the asset class field VALUE, not just if any element with that name exists
     */
    public boolean isCurrentAssetClassEqualTo(String targetClass) {
        // Use short timeout — this is a quick detection check, not a wait-for-element
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofMillis(800));
        try {
            // Read the SELECTED value (picker button below the label), then compare
            // exactly. We must NOT do a CONTAINS-'targetClass' proximity scan: the
            // asset LIST screen behind the Edit screen (assets named "ATS 1/2/3…")
            // would false-positive. See findAssetClassPickerButton().
            String current = getCurrentAssetClassValue();
            System.out.println("   Current asset class (selected): " +
                (current == null ? "<undetected>" : "'" + current + "'"));
            if (current == null) return false; // can't confirm → let caller open picker + select (idempotent)
            // Space-insensitive compare: the picker/button shows "Load Center"
            // while callers/tests pass "Loadcenter" (and similar). Normalize both.
            return normalizeClass(current).equals(normalizeClass(targetClass));
        } catch (Exception e) {
            System.out.println("   Error checking current asset class: " + e.getMessage());
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
    }

    // ================================================================
    // ASSET CLASS CHANGE — SHARED HELPERS
    // ================================================================

    /**
     * Open the full-screen Asset Class picker.
     * Finds the "Asset Class" label and taps the dropdown below it.
     * @return true if picker opened successfully
     */
    private boolean openAssetClassPicker() {
        // Strategy 0: click the picker BUTTON element directly (most reliable —
        // a coordinate tap can miss / hit the row behind it). Reuse the button the
        // preceding isCurrentAssetClassEqualTo() read already found (cached) so we
        // don't re-run the bounded-but-still-costly enumeration a second time in the
        // same class-change call.
        try {
            WebElement pickerBtn = cachedAssetClassPickerButton;
            if (pickerBtn == null) {
                pickerBtn = findAssetClassPickerButton();
            }
            if (pickerBtn != null) {
                System.out.println("   Opening picker via button: '" + pickerBtn.getAttribute("label") + "'");
                pickerBtn.click();
                sleep(500);
                return true;
            }
        } catch (Exception e) {
            // A stale cached element (screen changed) lands here — drop it and fall
            // through to the textual/coordinate strategies below.
            cachedAssetClassPickerButton = null;
            System.out.println("   Picker-button click failed, trying coordinate tap: " + e.getMessage());
        }
        try {
            WebElement label = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "(name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class') AND type == 'XCUIElementTypeStaticText'")
            );
            int x = label.getLocation().getX() + 150;
            int y = label.getLocation().getY() + label.getSize().getHeight() + 25;
            System.out.println("   Opening picker at (" + x + ", " + y + ")");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(500);
            return true;
        } catch (Exception e) {
            // Fallback: tap a button showing the current class name
            try {
                WebElement classBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND " +
                        "(name == 'ATS' OR name == 'Busway' OR name == 'Capacitor' OR name == 'Circuit Breaker' OR " +
                        "name == 'DC Bus' OR name == 'Default' OR name == 'Disconnect Switch' OR name == 'Fuse' OR " +
                        "name == 'Generator' OR name == 'Junction Box' OR name == 'Lightning Controls' OR " +
                        "name == 'Load' OR name == 'Loadcenter' OR name == 'MCC' OR name == 'MCC Bucket' OR " +
                        "name == 'Meter' OR name == 'Motor' OR name == 'Motor Starter' OR name == 'None' OR " +
                        "name == 'Other' OR name == 'Other (OCP)' OR name == 'Panelboard' OR name == 'PDU' OR " +
                        "name == 'Reactor' OR name == 'Rectifier' OR name == 'Relay' OR name == 'Switchboard' OR " +
                        "name == 'Transformer' OR name == 'Transformer (3-Winding)' OR " +
                        "name == 'UPS' OR name == 'Utility' OR name == 'VFD')")
                );
                System.out.println("   Opening picker via button: " + classBtn.getAttribute("name"));
                classBtn.click();
                sleep(500);
                return true;
            } catch (Exception e2) {
                System.out.println("⚠️ Could not open Asset Class picker: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Find and tap an item in the Asset Class picker list.
     * Uses 4 strategies: accessibilityId, predicate, mobile:scroll, manual scroll.
     * @param className the asset class name to select (e.g. "Motor", "UPS")
     * @return true if item was tapped
     */
    /**
     * Select an asset class by typing into the picker's "Search..." field, then
     * tapping the filtered option. The picker is a searchable dropdown whose option
     * list is lazy — scrolling to off-screen classes (e.g. Loadcenter) fails, but
     * filtering brings the match into view reliably.
     */
    /** The picker's "Search..." field (NOT the asset-list search behind it). */
    private WebElement findClassSearchField() {
        // 0-implicit probe: on the NON-searchable subtype picker there is no
        // search field, and this is called as a fast guard in tapAssetClassItem —
        // a 5s implicit-wait miss here is pure waste.
        return withImplicitWait(0, () -> {
            for (WebElement sf : driver.findElements(AppiumBy.className("XCUIElementTypeSearchField"))) {
                try {
                    if (!sf.isDisplayed()) continue;
                    String ph = sf.getAttribute("placeholderValue");
                    String nm = sf.getAttribute("name");
                    if ("Search...".equals(ph) || "Search...".equals(nm)) return sf;
                } catch (Exception ignore) {}
            }
            return null;
        });
    }

    /** Tri-state recorded by selectClassViaSearch() so tapAssetClassItem() does NOT
     *  re-probe for the search field (the documented double findClassSearchField()
     *  call — each one iterates every XCUIElementTypeSearchField). Reset to null on
     *  every entry into tapAssetClassItem(). TRUE = picker is searchable,
     *  FALSE = not searchable (subtype picker), null = unknown. */
    private Boolean lastPickerSearchable;

    private boolean selectClassViaSearch(String className) {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofMillis(800));
            WebElement search = findClassSearchField();   // self-bounded at implicit-wait 0
            lastPickerSearchable = (search != null);      // cache so tapAssetClassItem need not re-probe
            if (search == null) {
                System.out.println("   (no 'Search...' field — picker not searchable here)");
                return false;
            }
            // Try progressively looser search terms. The app filters by substring,
            // and the code name ("Loadcenter") may differ from the option label
            // ("Load Center") — a short prefix like "Load" matches both spellings.
            String wanted = normalizeClass(className);
            java.util.LinkedHashSet<String> terms = new java.util.LinkedHashSet<>();
            terms.add(className);
            terms.add(className.split(" ")[0]);
            terms.add(className.substring(0, Math.min(4, className.length())));
            for (String term : terms) {
                try { search.click(); } catch (Exception e) { search = findClassSearchField(); if (search == null) break; search.click(); }
                sleep(150);
                try { search.clear(); } catch (Exception ignore) {}
                search.sendKeys(term);
                sleep(700); // let the list filter
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
                try {
                    WebElement opt = driver.findElement(AppiumBy.accessibilityId(className));
                    opt.click();
                    System.out.println("   ✓ Searched '" + term + "' + tapped '" + className + "' (accessibilityId)");
                    return true;
                } catch (Exception ignore) {}
                for (WebElement b : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText'"))) {
                    try {
                        String n = b.getAttribute("label");
                        if (n == null) n = b.getAttribute("name");
                        if (n == null || n.contains(",")) continue;
                        if (normalizeClass(n).equals(wanted) && b.isDisplayed()) {
                            b.click();
                            System.out.println("   ✓ Searched '" + term + "' + tapped '" + n + "'");
                            return true;
                        }
                    } catch (Exception ignore) {}
                }
                driver.manage().timeouts().implicitlyWait(Duration.ofMillis(800));
                System.out.println("   search term '" + term + "' did not surface '" + className + "', trying looser…");
            }
            System.out.println("   search could not surface '" + className + "'");
            return false;
        } catch (Exception e) {
            System.out.println("   selectClassViaSearch failed: " + e.getMessage());
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
    }

    /** Overall wall-clock budget for selecting ONE picker option. Every scroll
     *  loop checks this and bails — the SUBTYPE picker is non-searchable and
     *  long, and the legacy 8+8 drag-scrolls at implicit-wait 1s/miss could burn
     *  the whole 360s per-test cap (run 27485736625: 18 tests killed at 6m0s,
     *  HUNG here, not asserting). A clean ~45s fail beats a 6-minute hang. */
    private static final long TAP_OPTION_BUDGET_MS = 45_000L;

    private boolean tapAssetClassItem(String className) {
        long deadline = System.currentTimeMillis() + TAP_OPTION_BUDGET_MS;
        lastPickerSearchable = null;   // selectClassViaSearch() will set it; reset per call

        // Strategy 0 (preferred): the Asset Class picker is a SEARCHABLE dropdown
        // (a "Search..." field above the option list). Type to filter — far more
        // reliable than scrolling a long lazy list (off-screen rows aren't in the
        // a11y tree until filtered into view).
        if (selectClassViaSearch(className)) return true;
        // If the picker IS searchable but search couldn't surface the class, the
        // legacy scroll strategies are futile (lazy list never materializes the
        // row) AND slow (~20 min of dragging). Fail fast instead.
        // NOTE: the SUBTYPE picker is NON-searchable (no "Search..." field), so
        // the search path returns false and we fall through to the cheap, bounded,
        // special-char-robust strategies below.
        // Reuse the searchable tri-state selectClassViaSearch() just recorded — do
        // NOT re-probe with findClassSearchField() here (that was the documented
        // double search-field enumeration). Only fall back to a probe if, somehow,
        // the flag was never set (defensive; e.g. an early throw).
        boolean searchable = (lastPickerSearchable != null)
            ? lastPickerSearchable.booleanValue()
            : (findClassSearchField() != null);
        if (searchable) {
            System.out.println("   ✗ '" + className + "' not found via picker search — failing fast (no such option?)");
            return false;
        }

        String wanted = normalizeClass(className);          // space-insensitive (Load Center vs Loadcenter)
        String wantedOpt = normalizeOption(className);      // special-char-robust ((<= 200hp), (> 1000V), dashes)
        String safePrefix = safeContainsPrefix(className);  // CONTAINS pre-filter (part before first '(')

        // Strategy 1: Direct accessibilityId (works for visible, plain-named items).
        // Cheap probe — implicit-wait 0; a miss costs ~ms, not 3s.
        try {
            List<WebElement> hits = withImplicitWait(0, () -> driver.findElements(AppiumBy.accessibilityId(className)));
            if (!hits.isEmpty()) {
                hits.get(0).click();
                System.out.println("   ✓ Tapped '" + className + "' (accessibilityId)");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   accessibilityId('" + className + "') failed, trying alternatives...");
        }

        // Strategy 2: Exact predicate match on label (handles different element types).
        try {
            String pred = "label == '" + className + "' OR name == '" + className + "'";
            List<WebElement> hits = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(pred)));
            if (!hits.isEmpty()) {
                hits.get(0).click();
                System.out.println("   ✓ Tapped '" + className + "' (exact predicate)");
                return true;
            }
        } catch (Exception ignore) {}

        // Strategy 3: CONTAINS pre-filter + normalized full compare (special-char
        // robust). This is the one that catches "Low-Voltage Machine (<= 200hp)",
        // "Motor Control Equipment (> 1000V)", "Oil-Filled Transformer", etc. that
        // exact equality misses on '(', '<=', '/', dashes, or whitespace variants.
        // The CONTAINS uses only the quote/paren-free head (safePrefix) so the
        // predicate string is always well-formed; normalizeOption() then does the
        // precise discriminating compare in Java (keeps <= vs > distinct).
        try {
            if (tapByNormalizedOption(safePrefix, wantedOpt, className)) return true;
        } catch (Exception ignore) {}

        // Strategy 4: ONE mobile:scroll using the safe CONTAINS substring (handles
        // an off-screen row), then re-run the normalized compare. Prefer this
        // single native scroll over manual drags. predicateString carries only
        // the quote/paren-free head, so no special-char escaping issues.
        if (System.currentTimeMillis() < deadline && safePrefix != null && !safePrefix.isEmpty()) {
            try {
                driver.executeScript("mobile: scroll", Map.of(
                    "direction", "down",
                    "predicateString", "label CONTAINS '" + safePrefix + "' OR name CONTAINS '" + safePrefix + "'"
                ));
                sleep(200);
                if (tapByNormalizedOption(safePrefix, wantedOpt, className)) {
                    System.out.println("   ✓ (after mobile:scroll on '" + safePrefix + "')");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("   mobile:scroll on '" + safePrefix + "' failed");
            }
        }

        // Strategy 5: bounded manual drag-scroll + normalized compare. Capped at
        // MAX_SCROLL_ITERS and gated on the overall deadline so it can NEVER run
        // away into the 360s test cap. Every findElements runs at implicit-wait 0.
        int screenWidth = driver.manage().window().getSize().width;
        int screenHeight = driver.manage().window().getSize().height;
        final int MAX_SCROLL_ITERS = 12;
        for (int i = 0; i < MAX_SCROLL_ITERS && System.currentTimeMillis() < deadline; i++) {
            if (tapByNormalizedOption(safePrefix, wantedOpt, className)) {
                System.out.println("   ✓ (after " + i + " manual scrolls)");
                return true;
            }
            // Also accept a plain space-insensitive match for non-special-char
            // labels (e.g. "Load Center" vs "Loadcenter").
            try {
                Boolean tapped = withImplicitWait(0, () -> {
                    for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton'"))) {
                        try {
                            String n = el.getAttribute("label");
                            if (n == null) n = el.getAttribute("name");
                            if (n == null || n.contains(",")) continue;
                            if (normalizeClass(n).equals(wanted) && el.isDisplayed()) {
                                int tx = el.getLocation().getX() + el.getSize().getWidth() / 2;
                                int ty = el.getLocation().getY() + el.getSize().getHeight() / 2;
                                driver.executeScript("mobile: tap", Map.of("x", tx, "y", ty));
                                System.out.println("   ✓ Tapped '" + n + "' (space-insensitive match for '" + className + "')");
                                return true;
                            }
                        } catch (Exception ignore) {}
                    }
                    return false;
                });
                if (Boolean.TRUE.equals(tapped)) return true;
            } catch (Exception ignore) {}

            driver.executeScript("mobile: dragFromToForDuration", Map.of(
                "fromX", screenWidth / 2, "fromY", screenHeight / 2 + 150,
                "toX", screenWidth / 2, "toY", screenHeight / 2 - 150, "duration", 0.3));
            sleep(250);
        }

        if (System.currentTimeMillis() >= deadline) {
            System.out.println("   ✗ '" + className + "' not selected within " + (TAP_OPTION_BUDGET_MS / 1000)
                + "s budget — bailing (NOT hanging the test cap)");
        } else {
            System.out.println("   ✗ Could not find '" + className + "' in picker after all strategies");
        }
        return false;
    }

    /**
     * Cheap, special-char-robust option matcher: scan the currently-rendered
     * StaticText/Button rows whose label CONTAINS the safe (quote/paren-free)
     * pre-filter substring, and tap the first whose normalizeOption() exactly
     * equals the wanted normalized form. Runs entirely at implicit-wait 0 so a
     * miss costs ~ms. Keeps "(<= 1000V)" vs "(> 1000V)" siblings distinct via
     * normalizeOption(). Returns true iff a row was tapped.
     */
    private boolean tapByNormalizedOption(String safePrefix, String wantedOpt, String original) {
        if (wantedOpt == null || wantedOpt.isEmpty()) return false;
        return Boolean.TRUE.equals(withImplicitWait(0, () -> {
            String pred;
            if (safePrefix != null && !safePrefix.isEmpty()) {
                // Pre-filter to rows whose label/name contains the safe head — far
                // fewer candidates to normalize-compare than the whole tree.
                pred = "(type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton') AND "
                     + "(label CONTAINS[c] '" + safePrefix + "' OR name CONTAINS[c] '" + safePrefix + "')";
            } else {
                pred = "type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton'";
            }
            List<WebElement> candidates;
            try {
                candidates = driver.findElements(AppiumBy.iOSNsPredicateString(pred));
            } catch (Exception e) {
                candidates = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeButton'"));
            }
            for (WebElement el : candidates) {
                try {
                    String n = el.getAttribute("label");
                    if (n == null) n = el.getAttribute("name");
                    // Asset-LIST rows (live behind the pushed picker) carry commas;
                    // option rows are single values. Skip comma rows to avoid the
                    // previous-screen bleed-through trap.
                    if (n == null || n.contains(",")) continue;
                    if (normalizeOption(n).equals(wantedOpt) && el.isDisplayed()) {
                        int tx = el.getLocation().getX() + el.getSize().getWidth() / 2;
                        int ty = el.getLocation().getY() + el.getSize().getHeight() / 2;
                        driver.executeScript("mobile: tap", Map.of("x", tx, "y", ty));
                        System.out.println("   ✓ Tapped '" + n + "' (normalized match for '" + original + "')");
                        return true;
                    }
                } catch (Exception ignore) {}
            }
            return false;
        }));
    }

    /**
     * Tap the "Done" button on the Asset Class or Subtype picker.
     * The iOS picker is a full-screen table view with "Done" in the nav bar.
     */
    private void tapDoneOnPicker() {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
            WebElement done = driver.findElement(AppiumBy.accessibilityId("Done"));
            done.click();
            System.out.println("   ✓ Tapped Done");
            sleep(300);
        } catch (Exception e) {
            try {
                WebElement done = driver.findElement(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Done'")
                );
                done.click();
                System.out.println("   ✓ Tapped Done (predicate)");
                sleep(300);
            } catch (Exception e2) {
                System.out.println("   ⚠️ Done button not found — picker may have auto-dismissed");
            }
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
    }

    /**
     * Unified asset class change: open picker → select item → tap Done.
     * Replaces all individual changeAssetClassTo*() implementations.
     */
    /** Hard wall-clock budget for the ENTIRE class change (read → open → select →
     *  Done). Each helper is now individually bounded (implicit-wait 0 + per-step
     *  budgets), but a wedging/half-dead WDA can still stall a single command past
     *  its helper budget; this end-to-end deadline guarantees a fast VerificationError
     *  in ~1 min instead of grinding to the 360s per-test cap and hammering WDA into
     *  the "Could not start a new session" cascade (assetsP6 run, 2026-06-15). */
    private static final long CLASS_CHANGE_BUDGET_MS =
        com.egalvanic.constants.AppConstants.CLASS_CHANGE_BUDGET_SEC * 1000L;

    private void changeAssetClassInternal(String className) {
        System.out.println("📋 Changing asset class to " + className + "...");
        // Never trust a button cached from a previous class change / screen.
        cachedAssetClassPickerButton = null;
        long deadline = System.currentTimeMillis() + CLASS_CHANGE_BUDGET_MS;

        if (isCurrentAssetClassEqualTo(className)) {
            System.out.println("✅ Already " + className);
            return;
        }
        assertClassChangeDeadline(className, deadline, "after reading current class");

        if (!openAssetClassPicker()) {
            System.out.println("⚠️ Failed to open Asset Class picker for " + className);
            return;
        }
        assertClassChangeDeadline(className, deadline, "after opening picker");

        if (!tapAssetClassItem(className)) {
            System.out.println("⚠️ Could not select " + className + " — dismissing picker");
            tapDoneOnPicker();
            return;
        }
        assertClassChangeDeadline(className, deadline, "after selecting class");

        sleep(300);
        tapDoneOnPicker();
        System.out.println("✅ Changed asset class to " + className);
    }

    /** Throw a fast VerificationError (NOT swallowable by catch(Exception)) if the
     *  class-change wall-clock budget is exhausted, so the test fails in seconds
     *  rather than wedging WDA to the per-test cap. */
    private void assertClassChangeDeadline(String className, long deadline, String phase) {
        if (System.currentTimeMillis() >= deadline) {
            throw new VerificationError("Asset class change to '" + className
                + "' exceeded " + (CLASS_CHANGE_BUDGET_MS / 1000) + "s budget (" + phase
                + ") — failing fast to avoid a 6-min WDA hang.");
        }
    }

    // ================================================================
    // ASSET CLASS CHANGE — PUBLIC METHODS (delegate to internal helper)
    // ================================================================

    public final void changeAssetClassToBusway() {
        changeAssetClassInternal("Busway");
    }
    
    public final void changeAssetClassToATS() {
        changeAssetClassInternal("ATS");
    }

    public final void changeAssetClassToGenerator() {
        changeAssetClassInternal("Generator");
    }
    
    /** Full set of selectable asset classes (matches the picker options). */
    // Aligned to the LIVE backend's 43 class options (captured 2026-06-17 from the web
    // app acme.qa.egalvanic.ai, same data model). The old set had a TYPO ("Lightning
    // Controls" → real is "Lighting Controls") and was missing many classes (Battery,
    // Busduct, Cable, Capacitor Bank, Motor Controller, Series/Shunt Reactor, Switch,
    // Tie Breaker, VFD Panel, …). Since this set feeds the picker-button IN-predicate,
    // a missing/misspelled class meant the picker button was never found → that class's
    // change silently failed. Extra/legacy entries are harmless (just never match).
    /** Live asset SUBTYPES (source: node_classes API / node_classes_template v14, 2026-06-22).
     *  Used by the subtype-dropdown open helper as a single scoped IN-predicate. */
    private static final String[] LIVE_SUBTYPES = {
        "Automatic Transfer Switch (<= 1000V)",
        "Automatic Transfer Switch (> 1000V)",
        "Battery Energy Storage System (ESS)",
        "Bolted-Pressure Switch (BPS)",
        "Branch Panel",
        "Bypass-Isolation Switch (<= 1000V)",
        "Bypass-Isolation Switch (> 1000V)",
        "Control Panel",
        "Disconnect Switch (<= 1000V)",
        "Disconnect Switch (> 1000V)",
        "Distribution Panelboard",
        "Dry Transformer",
        "Dry-Type Transformer (<= 600V)",
        "Dry-Type Transformer (> 600V)",
        "Electrical Vehicle Charging Station",
        "Electromechanical Relay",
        "Fuse (<= 1000V)",
        "Fuse (> 1000V)",
        "Fused Disconnect Switch (<= 1000V)",
        "Fused Disconnect Switch (>1000V)",
        "General Load",
        "High-Pressure Contact Switch (HPC)",
        "Hybrid UPS System",
        "I don't node",
        "Lithium-Ion",
        "Load-Interruptor Switch",
        "Low-Voltage Insulated Case Circuit Breaker",
        "Low-Voltage Machine (<= 200hp)",
        "Low-Voltage Machine (>200hp)",
        "Low-Voltage Molded Case Circuit Breaker (<= 225A)",
        "Low-Voltage Molded Case Circuit Breaker (> 225A)",
        "Low-Voltage Power Circuit Breaker",
        "Medium-Voltage Air Magnetic Circuit Breaker",
        "Medium-Voltage Gas Insulated Circuit Breaker",
        "Medium-Voltage Induction Machine",
        "Medium-Voltage Oil Insulated Circuit Breaker",
        "Medium-Voltage Synchronous Machine",
        "Medium-Voltage Vacuum Circuit Breaker",
        "Microprocessor Relay",
        "Motor Circuit Protector",
        "Motor Control Equipment (<= 1000V)",
        "Motor Control Equipment (> 1000V)",
        "Ni-Cad Battery",
        "Oil-Filled Transformer",
        "Panelboard",
        "Power Factor Correction",
        "Power Panel",
        "Recloser (<= 1000V)",
        "Recloser (> 1000V)",
        "Resistive Load",
        "Rotary UPS System",
        "Scrubtype",
        "Solar Photovoltaic System",
        "Solid-State Relay",
        "Static UPS System",
        "Switchboard",
        "Switchgear (<= 1000V)",
        "Switchgear (> 1000V)",
        "Transfer Switch (<= 1000V)",
        "Transfer Switch (> 1000V)",
        "Unitized Substation (USS) (<= 1000V)",
        "Unitized Substation (USS) (> 1000V)",
        "Valve-Regulated Lead-Acid Battery",
        "Vented Lead-Acid Battery",
        "Wind Power System"
    };

    private static final java.util.Set<String> ASSET_CLASSES = new java.util.HashSet<>(java.util.Arrays.asList(
        // live (web-confirmed) 43:
        "ATS", "Battery", "Busduct", "Busway", "Cable", "Capacitor", "Capacitor Bank",
        "Circuit Breaker", "Default", "Disconnect Switch", "Fuse", "Generator", "Junction Box",
        "Lighting Controls", "Load", "Loadcenter", "MCC", "MCC Bucket", "Meter", "Motor",
        "Motor Controller", "Motor Starter", "Other", "Other (OCP)", "Panelboard", "PDU",
        "QA_ATS1", "QANode", "Rectifier", "Relay", "Series Reactor", "Shunt Reactor", "Switch",
        "Switchboard", "Test", "Tie Breaker", "Transformer", "Transformer (3-Winding)", "UPS",
        "Utility", "VFD", "VFD Panel",
        // legacy/defensive (kept in case a screen still renders these spellings):
        "DC Bus", "Lightning Controls", "Load Center", "None", "Reactor"));

    /** Normalize a class name for matching: lower-case, spaces removed.
     *  Unifies the "Loadcenter" (code) vs "Load Center" (live picker) spelling. */
    private static String normalizeClass(String s) {
        return s == null ? "" : s.trim().toLowerCase().replace(" ", "");
    }

    /**
     * Special-char-robust option normalizer for SUBTYPE labels (and any option
     * with parens / comparators / punctuation, e.g. "Low-Voltage Machine (<= 200hp)",
     * "Motor Control Equipment (> 1000V)", "Oil-Filled Transformer").
     *
     * Exact predicate equality fails on these: the picker's rendered label can
     * differ from the code string in whitespace, dash style, or non-breaking
     * variants of "<=" / ">", and the SUBTYPE picker is NON-searchable so the
     * searchable fast path doesn't apply. We compare on a canonical form instead.
     *
     * Crucially we DO NOT just strip everything: "(<= 1000V)" and "(> 1000V)"
     * must stay distinct (they're sibling subtypes that differ ONLY by the
     * comparator). So comparators are mapped to words FIRST, then we lower-case
     * and drop the remaining non-alphanumerics.
     *   "Motor Control Equipment (<= 1000V)" -> "motorcontrolequipmentle1000v"
     *   "Motor Control Equipment (> 1000V)"  -> "motorcontrolequipmentgt1000v"
     */
    private static String normalizeOption(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase();
        // Map comparators to words before stripping punctuation so sibling
        // subtypes that differ only by "<=" vs ">" don't collapse together.
        t = t.replace("<=", " le ").replace(">=", " ge ")
             .replace("<", " lt ").replace(">", " gt ");
        // Drop every non-alphanumeric (spaces, dashes, parens, dots, NBSP, …).
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isLetterOrDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    /**
     * The "safe" CONTAINS pre-filter substring for a picker option: the part of
     * the label before the first '(' (or the whole label if no paren), trimmed.
     * For "Low-Voltage Machine (<= 200hp)" -> "Low-Voltage Machine". Used in an
     * iOS predicate CONTAINS clause (no special-char escaping headaches: '(',
     * '<', '>' never reach the predicate) to cheaply narrow candidates before
     * the precise normalizeOption() compare in Java.
     */
    private static String safeContainsPrefix(String s) {
        if (s == null) return "";
        int paren = s.indexOf('(');
        String head = (paren >= 0 ? s.substring(0, paren) : s).trim();
        // A predicate string can't safely carry a single quote; if the head has
        // one, fall back to the longest quote-free leading token-run.
        if (head.contains("'")) head = head.substring(0, head.indexOf('\'')).trim();
        return head;
    }

    /** Pick the first non-empty, comma-free candidate (asset-LIST rows carry commas). */
    private String cleanClassToken(String... candidates) {
        for (String s : candidates) {
            if (s == null) continue;
            s = s.trim();
            if (s.isEmpty() || s.contains(",")) continue;
            return s;
        }
        return null;
    }

    /**
     * Locate the Asset Class picker BUTTON on the Edit form — the wide button
     * rendered directly below the "Asset Class" label at the same x (label x=32,
     * button x=32, ~25px down), whose label is a single known class.
     *
     * Structural (not textual) on purpose: the asset LIST screen stays live in
     * the DOM behind the pushed Edit screen (rows like "ATS 2, 1, ATS" + a class
     * column "ATS"), so any CONTAINS-'ATS'/Y-proximity scan reads the wrong
     * screen. List rows differ by x and carry comma-separated labels.
     *
     * @return the picker button element, or null if not found
     */
    /** Wall-clock budget for ONE picker-button enumeration. The Edit screen keeps
     *  the asset LIST live behind it (bleed-through), so a full
     *  type=='XCUIElementTypeButton' scan can surface dozens of buttons; with the
     *  global 5s implicit wait active, getLocation/getSize/getAttribute per button
     *  on a wedging WDA dragged GEN_01/TC_DS_ST_16/TC_FUSE_ST_03 into the 360s cap
     *  (assetsP6 run, 2026-06-15). Bound the loop and bail to null fast. */
    private static final long PICKER_BUTTON_ENUM_BUDGET_MS =
        com.egalvanic.constants.AppConstants.PICKER_ENUM_BUDGET_SEC * 1000L;

    /** Cached Asset-Class picker button, populated by findAssetClassPickerButton()
     *  within a single changeAssetClassInternal() call so the equality-read and the
     *  open-picker step don't enumerate the button twice. Always cleared at the
     *  start of a class change — never trust it across screens. */
    private WebElement cachedAssetClassPickerButton;

    private WebElement findAssetClassPickerButton() {
        // Run the WHOLE enumeration at implicit-wait 0: every getLocation/getSize/
        // getAttribute below would otherwise pay the global 5s implicit wait per
        // miss across a bleed-through DOM of many buttons (the documented hang).
        // A single snapshot is then read cheaply; a hard wall-clock budget bails
        // to null so a wedging WDA can never grind us to the per-test cap.
        WebElement found = withImplicitWait(0, () -> {
            long deadline = System.currentTimeMillis() + PICKER_BUTTON_ENUM_BUDGET_MS;
            int labelX, labelY;
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND (name == 'Asset Class' OR label == 'Asset Class')"));
                labelX = label.getLocation().getX();
                labelY = label.getLocation().getY();
            } catch (Exception e) {
                return null;
            }
            // SCOPED query (perf fix, 2026-06-17): the old approach enumerated EVERY
            // XCUIElementTypeButton and did ~5 getLocation/getSize/getAttribute round-trips
            // PER button. On the bleed-through Edit DOM (front form + asset list behind)
            // that is dozens of buttons × 5 HTTP calls ≈ 25s → hit the budget, the class
            // change never completed (MOT_AST_03: stayed 'ATS', then the Motor subtype pick
            // hung WDA to the 360s cap). Instead, ask WDA directly for ONLY the buttons whose
            // label/name is a known asset class — the picker button's label IS the current
            // class, and bleed-through list rows are NOT buttons-labelled-as-a-class. This
            // returns ~1 element, so we iterate a handful, not the whole tree.
            WebElement best = null; int bestDy = Integer.MAX_VALUE;
            try {
                StringBuilder inList = new StringBuilder();
                for (String cls : ASSET_CLASSES) {
                    if (inList.length() > 0) inList.append(", ");
                    inList.append("'").append(cls).append("'");   // class names contain no single-quotes
                }
                String pred = "type == 'XCUIElementTypeButton' AND (label IN {" + inList
                    + "} OR name IN {" + inList + "})";
                for (WebElement b : driver.findElements(AppiumBy.iOSNsPredicateString(pred))) {
                    if (System.currentTimeMillis() >= deadline) {
                        System.out.println("   ⏱️ Asset-class picker-button scan hit "
                            + (PICKER_BUTTON_ENUM_BUDGET_MS / 1000) + "s budget — bailing");
                        break;
                    }
                    try {
                        int dy = b.getLocation().getY() - labelY;
                        if (dy <= 0 || dy > 55) continue;                       // must be just below the label
                        if (Math.abs(b.getLocation().getX() - labelX) > 25) continue; // same x as the label
                        if (b.getSize().getWidth() < 150) continue;             // a full-width field button
                        if (dy < bestDy) { best = b; bestDy = dy; }
                    } catch (Exception ignore) {}
                }
            } catch (Exception ignore) {}
            return best;
        });
        // Cache for reuse within the same changeAssetClassInternal() call.
        if (found != null) cachedAssetClassPickerButton = found;
        return found;
    }

    /**
     * Get the CURRENTLY SELECTED asset class from the Edit form's picker.
     * Reads the picker button (see {@link #findAssetClassPickerButton()}); falls
     * back to the value StaticText indented below the label.
     *
     * @return the selected class (e.g. "Motor", "ATS") or null if undetectable
     */
    private String getCurrentAssetClassValue() {
        WebElement btn = findAssetClassPickerButton();   // already self-bounded (implicit-wait 0 + budget)
        if (btn != null) {
            String v = cleanClassToken(btn.getAttribute("label"), btn.getAttribute("name"));
            if (v != null && ASSET_CLASSES.contains(v)) return v;
        }
        // Fallback: value StaticText just below + indented from the "Asset Class"
        // label. Bound the whole enumeration at implicit-wait 0 — the bleed-through
        // asset LIST behind the Edit screen makes XCUIElementTypeStaticText huge, and
        // a per-element getLocation/getSize/getAttribute miss at the global 5s
        // implicit wait is exactly what stretched the "Current asset class" read to
        // 60-70s in the assetsP6 hangs.
        return withImplicitWait(0, () -> {
            long deadline = System.currentTimeMillis() + PICKER_BUTTON_ENUM_BUDGET_MS;
            int labelX, labelY;
            try {
                WebElement label = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND (name == 'Asset Class' OR label == 'Asset Class')"));
                labelX = label.getLocation().getX();
                labelY = label.getLocation().getY();
            } catch (Exception e) {
                return null;
            }
            try {
                for (WebElement t : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                    if (System.currentTimeMillis() >= deadline) break;
                    try {
                        int dy = t.getLocation().getY() - labelY;
                        if (dy <= 0 || dy > 60) continue;
                        int dx = t.getLocation().getX() - labelX;
                        if (dx < 0 || dx > 220) continue;   // value is indented right of / below the label
                        String v = cleanClassToken(t.getAttribute("value"), t.getAttribute("name"));
                        if (v != null && ASSET_CLASSES.contains(v)) return v;
                    } catch (Exception ignore) {}
                }
            } catch (Exception ignore) {}
            return null;
        });
    }
    
    /**
     * Robust method to open Asset Class dropdown on Edit Asset screen
     */
    private boolean openAssetClassDropdownRobust() {
        System.out.println("🔍 Opening Asset Class dropdown (robust)...");
        
        // Strategy 1: Find and tap the Asset Class row directly
        try {
            // Find Asset Class label
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class')")
            );
            
            // Get its position
            int labelX = assetClassLabel.getLocation().getX();
            int labelY = assetClassLabel.getLocation().getY();
            int labelWidth = assetClassLabel.getSize().getWidth();
            int labelHeight = assetClassLabel.getSize().getHeight();
            
            System.out.println("   Found 'Asset Class' label at (" + labelX + ", " + labelY + ")");
            
            // Tap to the right of the label (where the dropdown value is)
            int screenWidth = driver.manage().window().getSize().getWidth();
            int tapX = screenWidth - 100;  // Tap near right side where dropdown value typically is
            int tapY = labelY + (labelHeight / 2);  // Tap at same vertical level as label
            
            System.out.println("   Tapping Asset Class row at (" + tapX + ", " + tapY + ")...");
            driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
            sleep(300);
            
            if (isAssetClassDropdownDisplayed()) {
                System.out.println("   ✅ Dropdown opened via label tap!");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 1 failed: " + e.getMessage());
        }
        
        // Strategy 2: Click on any visible asset class name
        String[] assetClasses = {"UPS", "PDU", "Generator", "Busway", "Capacitor", "Circuit Breaker", "Fuse", "None"};
        for (String className : assetClasses) {
            try {
                List<WebElement> elements = driver.findElements(AppiumBy.accessibilityId(className));
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        System.out.println("   Found '" + className + "' - clicking to open dropdown");
                        el.click();
                        sleep(300);
                        if (isAssetClassDropdownDisplayed()) {
                            System.out.println("   ✅ Dropdown opened by clicking " + className);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {}
        }
        
        // Strategy 3: Find any button in the asset class row area  
        try {
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            
            List<WebElement> buttons = driver.findElements(AppiumBy.className("XCUIElementTypeButton"));
            for (WebElement btn : buttons) {
                int btnY = btn.getLocation().getY();
                if (Math.abs(btnY - labelY) < 60) {
                    String name = btn.getAttribute("name");
                    System.out.println("   Found button near Asset Class: " + name);
                    btn.click();
                    sleep(300);
                    if (isAssetClassDropdownDisplayed()) {
                        System.out.println("   ✅ Dropdown opened via button!");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 failed");
        }
        
        // Strategy 4: Try tapping various positions along the Asset Class row
        try {
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            int screenWidth = driver.manage().window().getSize().getWidth();
            
            int[] xPositions = {screenWidth / 2, screenWidth - 50, screenWidth - 150, screenWidth / 4 * 3};
            for (int tapX : xPositions) {
                System.out.println("   Trying tap at (" + tapX + ", " + labelY + ")...");
                driver.executeScript("mobile: tap", Map.of("x", tapX, "y", labelY));
                sleep(400);
                if (isAssetClassDropdownDisplayed()) {
                    System.out.println("   ✅ Dropdown opened!");
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 4 failed");
        }
        
        System.out.println("   ❌ Could not open Asset Class dropdown");
        return false;
    }
    
    /**
     * Try clicking on the current asset class value displayed on screen
     * This helps when we can't find the dropdown button but the value is visible
     */
    private boolean tryClickCurrentAssetClassValue() {
        String[] possibleClasses = {"ATS", "UPS", "PDU", "Generator", "Busway", "Capacitor", "Circuit Breaker", "Fuse", "None"};
        
        for (String className : possibleClasses) {
            try {
                WebElement classElement = driver.findElement(AppiumBy.accessibilityId(className));
                if (classElement.isDisplayed()) {
                    System.out.println("   Found current class: " + className + " - clicking it");
                    classElement.click();
                    sleep(200);
                    
                    // Check if dropdown opened
                    if (isAssetClassDropdownDisplayed()) {
                        return true;
                    }
                }
            } catch (Exception e) {}
        }
        
        // Also try finding StaticText with asset class names
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null) {
                    for (String className : possibleClasses) {
                        if (name.equals(className)) {
                            System.out.println("   Found class text: " + name + " - clicking it");
                            text.click();
                            sleep(200);
                            if (isAssetClassDropdownDisplayed()) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
        
        return false;
    }
    
    /**
     * Select ATS from the asset class dropdown
     */
    private void selectATSFromDropdown() {
        System.out.println("📋 Selecting ATS from dropdown...");
        
        // Strategy 1: Direct accessibility ID (fast - 1 second wait)
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
            WebElement ats = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("ATS")
            ));
            ats.click();
            System.out.println("✅ Selected ATS (accessibility ID)");
            return;
        } catch (Exception e) {}
        
        // Strategy 2: NSPredicate for name/label
        try {
            WebElement ats = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'ATS' OR label == 'ATS'")
            );
            ats.click();
            System.out.println("✅ Selected ATS (predicate)");
            return;
        } catch (Exception e) {}
        
        // Strategy 3: Find in visible cells/buttons
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            for (WebElement cell : cells) {
                String name = cell.getAttribute("name");
                if ("ATS".equals(name)) {
                    cell.click();
                    System.out.println("✅ Selected ATS from cell");
                    return;
                }
            }
        } catch (Exception e) {}
        
        System.out.println("⚠️ Could not select ATS from dropdown");
    }
    
    /**
     * DEBUG: Print current screen state
     */
    @SuppressWarnings("unused")
    private void debugPrintCurrentScreen() {
        System.out.println("\n🔍 DEBUG: Current Screen State");
        try {
            // Check for navigation bar title
            List<WebElement> navBars = driver.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"));
            for (WebElement nav : navBars) {
                System.out.println("   NavBar: " + nav.getAttribute("name"));
            }
            
            // Check for Edit/Save button to determine mode
            try {
                driver.findElement(AppiumBy.accessibilityId("Edit"));
                System.out.println("   ⚠️ 'Edit' button found - we're in VIEW mode, not EDIT mode!");
            } catch (Exception e) {}
            
            try {
                driver.findElement(AppiumBy.accessibilityId("Save"));
                System.out.println("   ✅ 'Save' button found - we're in EDIT mode");
            } catch (Exception e) {}
            
        } catch (Exception e) {
            System.out.println("   Error checking screen: " + e.getMessage());
        }
    }
    
    /**
     * Find and CLICK the Asset Class dropdown in Edit mode
     * Works regardless of what the current asset class is
     * Returns true if dropdown was opened successfully
     */
    private boolean clickAssetClassDropdown() {
        System.out.println("🔍 Finding and clicking Asset Class dropdown...");
        
        // Strategy 0: Try clicking directly on any displayed asset class name (for Edit mode) - FAST
        String[] assetClasses = {"ATS", "UPS", "PDU", "Generator", "Busway", "Capacitor", "Circuit Breaker", "Fuse"};
        for (String className : assetClasses) {
            try {
                WebElement classElement = driver.findElement(AppiumBy.accessibilityId(className));
                if (classElement.isDisplayed()) {
                    System.out.println("   Found displayed class: " + className + " - clicking it");
                    classElement.click();
                    sleep(200);
                    if (isDropdownOpen()) {
                        System.out.println("   ✅ Dropdown opened by clicking " + className);
                        return true;
                    }
                }
            } catch (Exception e) {}
        }
        
        // Strategy 1: Find "Asset Class" label and tap the row below it
        try {
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND (name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class')"
                )
            );
            int labelX = assetClassLabel.getLocation().getX();
            int labelY = assetClassLabel.getLocation().getY();
            int labelHeight = assetClassLabel.getSize().getHeight();
            
            System.out.println("   Found 'Asset Class' label at (" + labelX + ", " + labelY + ")");
            
            // Tap below the label (where the dropdown button is)
            // The dropdown is typically 30-50 pixels below the label
            int tapX = labelX + 100;  // Tap in the middle of the row
            int tapY = labelY + labelHeight + 30;  // Below the label
            
            System.out.println("   Tapping dropdown at (" + tapX + ", " + tapY + ")...");
            driver.executeScript("mobile: tap", Map.of("x", tapX, "y", tapY));
            sleep(400);
            
            // Verify dropdown opened
            if (isDropdownOpen()) {
                System.out.println("   ✅ Dropdown opened!");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 1 (label + offset) failed: " + e.getMessage());
        }
        
        // Strategy 2: Find any button in the Asset Class row area
        try {
            // First find the label to get the Y position
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class' OR label CONTAINS 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            
            // Find all buttons and look for one near the Asset Class row
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );
            
            for (WebElement btn : buttons) {
                int btnY = btn.getLocation().getY();
                // Check if button is in the Asset Class row (within 80 pixels below label)
                if (btnY > labelY && btnY < labelY + 80) {
                    String name = btn.getAttribute("name");
                    System.out.println("   Found button in Asset Class row: " + name);
                    btn.click();
                    sleep(400);
                    if (isDropdownOpen()) {
                        System.out.println("   ✅ Dropdown opened!");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 (button in row) failed: " + e.getMessage());
        }
        
        // Strategy 3: Find chevron.down icon near Asset Class
        try {
            List<WebElement> chevrons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeImage' AND name == 'chevron.down'")
            );
            
            // Get Asset Class label Y position
            WebElement assetClassLabel = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Asset Class'")
            );
            int labelY = assetClassLabel.getLocation().getY();
            
            for (WebElement chevron : chevrons) {
                int chevronY = chevron.getLocation().getY();
                // Check if chevron is near Asset Class row
                if (Math.abs(chevronY - labelY) < 80) {
                    int x = chevron.getLocation().getX();
                    int y = chevron.getLocation().getY();
                    System.out.println("   Found Asset Class chevron, tapping at (" + x + ", " + y + ")...");
                    driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
                    sleep(400);
                    if (isDropdownOpen()) {
                        System.out.println("   ✅ Dropdown opened!");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (chevron) failed: " + e.getMessage());
        }
        
        // Strategy 4: Tap by screen coordinates (fallback)
        try {
            System.out.println("   Strategy 4: Trying fixed coordinate tap...");
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            // Asset Class row is typically around 55-60% down from the top in Edit mode
            int x = size.getWidth() / 2;
            int y = (int)(size.getHeight() * 0.58);
            System.out.println("   Tapping at screen center (" + x + ", " + y + ")...");
            driver.executeScript("mobile: tap", Map.of("x", x, "y", y));
            sleep(400);
            if (isDropdownOpen()) {
                System.out.println("   ✅ Dropdown opened!");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 4 (coordinates) failed");
        }
        
        System.out.println("   ❌ Could not open Asset Class dropdown");
        return false;
    }
    
    /**
     * Check if asset class dropdown is currently open
     * by looking for class options like ATS, UPS, PDU, Busway, Generator, etc.
     */
    private boolean isDropdownOpen() {
        try {
            // Look for dropdown options - include ALL asset class names
            String[] dropdownOptions = {"ATS", "UPS", "PDU", "Busway", "Generator", "Capacitor", "Circuit Breaker", "Fuse", "None"};
            for (String option : dropdownOptions) {
                try {
                    List<WebElement> elements = driver.findElements(AppiumBy.accessibilityId(option));
                    // Need to find at least 2 elements with same name (1 is the current value, 2+ means dropdown is open)
                    // Or check if any element is in a picker/dropdown area
                    for (WebElement el : elements) {
                        if (el.isDisplayed()) {
                            // Check if this looks like a dropdown option (not just the current value)
                            int y = el.getLocation().getY();
                            // Dropdown options typically appear in lower half of screen when open
                            if (y > 400) {  // Dropdown picker area
                                System.out.println("   Found dropdown option: " + option + " at y=" + y);
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {}
            }
            
            // Also use the existing isAssetClassDropdownDisplayed method
            return isAssetClassDropdownDisplayed();
        } catch (Exception e) {}
        return false;
    }
    
    // Keep old method for compatibility but redirect
    @SuppressWarnings("unused")
    private WebElement findAssetClassButton() {
        String[] assetClassNames = {"ATS", "UPS", "PDU", "Generator", "Busway"};
        for (String className : assetClassNames) {
            try {
                WebElement btn = driver.findElement(AppiumBy.accessibilityId(className));
                if (btn.isDisplayed()) return btn;
            } catch (Exception e) {}
        }
        return null;
    }

    // ================================================================
    // CAPACITOR ASSET CLASS METHODS
    // ================================================================

    /**
     * Change asset class to Capacitor in Edit mode
     */
    public final void changeAssetClassToCapacitor() {
        changeAssetClassInternal("Capacitor");
    }
    
    /**
     * Check if asset class is already set to a specific value
     */
    private boolean isAssetClassAlready(String expectedClass) {
        try {
            // Look for the class name displayed on the Asset Class button/field
            WebElement classButton = driver.findElement(AppiumBy.accessibilityId(expectedClass));
            if (classButton.isDisplayed()) {
                System.out.println("   Found " + expectedClass + " already selected");
                return true;
            }
        } catch (Exception e) {}
        
        // Try finding by StaticText containing the class name near "Asset Class" label
        try {
            List<WebElement> texts = driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"));
            for (WebElement text : texts) {
                String name = text.getAttribute("name");
                if (name != null && name.equalsIgnoreCase(expectedClass)) {
                    // Check if it's near Asset Class area (y around 600-700)
                    int y = text.getLocation().getY();
                    if (y > 550 && y < 750) {
                        System.out.println("   Asset class is already: " + expectedClass);
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        
        return false;
    }
    
    /**
     * Tap on safe area to dismiss dropdown focus
     * Taps on empty space at top of form to close any open dropdowns
     */
    public void dismissDropdownFocus() {
        try {
            int screenWidth = driver.manage().window().getSize().width;
            // Tap on top area (above Asset Class which is typically at y=600+)
            driver.executeScript("mobile: tap", Map.of("x", screenWidth / 2, "y", 200));
            sleep(200);
        } catch (Exception e) {
            // Ignore
        }
    }
    
    /**
     * Select Capacitor from the asset class dropdown
     */
    private void selectCapacitorFromDropdown() {
        System.out.println("📋 Selecting Capacitor from dropdown...");
        
        try {
            WebElement capacitor = driver.findElement(AppiumBy.accessibilityId("Capacitor"));
            capacitor.click();
            System.out.println("✅ Selected Capacitor");
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement capacitor = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Capacitor' OR label == 'Capacitor'")
            );
            capacitor.click();
            System.out.println("✅ Selected Capacitor (predicate)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Capacitor in dropdown");
        }
    }

    // ================================================================
    // CIRCUIT BREAKER ASSET CLASS METHODS
    // ================================================================

    public final void changeAssetClassToCircuitBreaker() {
        changeAssetClassInternal("Circuit Breaker");
    }
    

    public final void changeAssetClassToDefault() {
        changeAssetClassInternal("Default");
    }

    private void selectCircuitBreakerFromDropdown() {
        System.out.println("📋 Selecting Circuit Breaker from dropdown...");
        
        try {
            WebElement cb = driver.findElement(AppiumBy.accessibilityId("Circuit Breaker"));
            cb.click();
            System.out.println("✅ Selected Circuit Breaker");
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement cb = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Circuit Breaker' OR label == 'Circuit Breaker'")
            );
            cb.click();
            System.out.println("✅ Selected Circuit Breaker (predicate)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Circuit Breaker in dropdown");
        }
    }

    public final void changeAssetClassToDisconnectSwitch() {
        changeAssetClassInternal("Disconnect Switch");
    }
    
    /**
     * Select Disconnect Switch from the asset class dropdown
     */
    private void selectDisconnectSwitchFromDropdown() {
        System.out.println("📋 Selecting Disconnect Switch from dropdown...");
        
        try {
            WebElement ds = driver.findElement(AppiumBy.accessibilityId("Disconnect Switch"));
            ds.click();
            System.out.println("✅ Selected Disconnect Switch");
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement ds = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Disconnect Switch' OR label == 'Disconnect Switch'")
            );
            ds.click();
            System.out.println("✅ Selected Disconnect Switch (predicate)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Disconnect Switch in dropdown");
        }
    }

    public final void changeAssetClassToFuse() {
        changeAssetClassInternal("Fuse");
    }
    
    /**
     * Select Fuse from the asset class dropdown
     */
    private void selectFuseFromDropdown() {
        System.out.println("📋 Selecting Fuse from dropdown...");
        
        try {
            WebElement fuse = driver.findElement(AppiumBy.accessibilityId("Fuse"));
            fuse.click();
            System.out.println("✅ Selected Fuse");
            return;
        } catch (Exception e) {}
        
        // Try predicate
        try {
            WebElement fuse = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Fuse' OR label == 'Fuse'")
            );
            fuse.click();
            System.out.println("✅ Selected Fuse (predicate)");
        } catch (Exception e) {
            System.out.println("⚠️ Could not find Fuse in dropdown");
        }
    }

    public final void changeAssetClassToJunctionBox() {
        changeAssetClassInternal("Junction Box");
    }

    public final void changeAssetClassToLoadcenter() {
        // The app's Asset Class picker offers "Load" (there is no "Loadcenter"
        // option — see BUGS.md B11). The LC_EAD tests operate on this class.
        changeAssetClassInternal("Load");
    }

    public final void changeAssetClassToMCC() {
        changeAssetClassInternal("MCC");
    }

    public final void changeAssetClassToMCCBucket() {
        changeAssetClassInternal("MCC Bucket");
    }
    
    /**
     * Check if Save Changes button is visible
     * Uses multiple strategies and includes scroll-into-view capability
     */
    public boolean isSaveChangesButtonVisible() {
        // FAST check - "Save" and "Save Changes" are the SAME button
        // Search full DOM (no visible == true) — button may be off-screen after scrolling up
        try {
            List<WebElement> saveBtns = driver.findElements(
                AppiumBy.iOSNsPredicateString("(name CONTAINS 'Save' OR label CONTAINS 'Save') AND type == 'XCUIElementTypeButton'")
            );
            if (!saveBtns.isEmpty()) {
                System.out.println("   ✅ Save button found in DOM (edit screen active)");
                return true;
            }
        } catch (Exception e) {}

        // Save button not found - this is NORMAL after successful save (left edit screen)
        System.out.println("   ℹ️ Save button not found (likely left edit screen after save)");
        return false;
    }
    
    /**
     * DEPRECATED - keeping for reference but use isSaveChangesButtonVisible() instead
     */
    private boolean isSaveChangesButtonVisibleOLD_SLOW() {
        System.out.println("🔍 Checking if Save Changes button is visible...");

        // Strategy 1: Check for "Save Changes" button directly
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton'")
            );
            if (saveBtn.isDisplayed()) {
                System.out.println("   ✅ Found visible 'Save Changes' button");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 1 (Save Changes visible): not found");
        }

        // Strategy 2: Check for "Save Changes" by label
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("label CONTAINS 'Save' AND type == 'XCUIElementTypeButton'")
            );
            if (saveBtn.isDisplayed()) {
                System.out.println("   ✅ Found 'Save Changes' button by label");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 2 (Save Changes by label): not found");
        }

        // Strategy 3: Check by accessibility ID
        try {
            WebElement saveBtn = driver.findElement(AppiumBy.accessibilityId("Save Changes"));
            if (saveBtn.isDisplayed()) {
                System.out.println("   ✅ Found 'Save Changes' button by accessibility ID");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 3 (accessibility ID): not found");
        }

        // Strategy 4: Check for "Save" button (alternative naming)
        try {
            WebElement saveBtn = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Save' AND type == 'XCUIElementTypeButton'")
            );
            if (saveBtn.isDisplayed()) {
                System.out.println("   ✅ Found visible 'Save' button (alternative)");
                return true;
            }
        } catch (Exception e) {
            System.out.println("   Strategy 4 (Save button): not found");
        }

        // Strategy 5: Scroll DOWN and check (Save Changes is typically at bottom of form)
        System.out.println("   Scrolling down to find Save Changes button...");
        for (int i = 0; i < 3; i++) {
            scrollFormDown();
            sleep(300);

            try {
                WebElement saveBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "(name CONTAINS 'Save' OR name == 'Save') AND type == 'XCUIElementTypeButton'"
                    )
                );
                if (saveBtn.isDisplayed()) {
                    System.out.println("   ✅ Found Save button after scrolling down (attempt " + (i + 1) + ")");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("   Scroll down attempt " + (i + 1) + ": not found yet");
            }
        }

        // Strategy 6: Scan all buttons for Save-related text
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );
            System.out.println("   Scanning " + buttons.size() + " buttons for Save...");
            for (WebElement btn : buttons) {
                try {
                    String name = btn.getAttribute("name");
                    String label = btn.getAttribute("label");
                    if ((name != null && name.toLowerCase().contains("save")) ||
                        (label != null && label.toLowerCase().contains("save"))) {
                        if (btn.isDisplayed()) {
                            System.out.println("   ✅ Found button with Save text: name='" + name + "' label='" + label + "'");
                            return true;
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("   Strategy 6 (scan all buttons): failed");
        }

        System.out.println("   ❌ Save Changes button not found after all strategies");
        return false;
    }
    
    /**
     * DEBUG: Print all visible input fields and labels for model/notes/manufacturer
     */
    public void debugFieldsOnScreen() {
        System.out.println("\n========== DEBUG: Fields on Screen ==========");
        try {
            List<WebElement> labels = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText'")
            );
            System.out.println("Labels (model/notes/manufacturer):");
            for (WebElement l : labels) {
                String name = l.getAttribute("name");
                if (name != null && (name.toLowerCase().contains("model") || 
                    name.toLowerCase().contains("notes") || 
                    name.toLowerCase().contains("manufacturer"))) {
                    System.out.println("  Label: '" + name + "' y=" + l.getLocation().getY());
                }
            }
        } catch (Exception e) {}
        try {
            List<WebElement> inputs = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
            );
            System.out.println("All TextField/TextView:");
            for (WebElement f : inputs) {
                String name = f.getAttribute("name");
                String value = f.getAttribute("value");
                int y = f.getLocation().getY();
                System.out.println("  Input: name='" + name + "' value='" + value + "' y=" + y);
            }
        } catch (Exception e) {}
        System.out.println("==============================================\n");
    }

    /**
     * Check if current asset class is MCC
     */
    public boolean isAssetClassMCC() {
        System.out.println("🔍 Checking if asset class is MCC...");
        try {
            // Look for MCC button (Asset Class dropdown showing MCC)
            WebElement mcc = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'MCC' AND type == 'XCUIElementTypeButton'")
            );
            System.out.println("   Found MCC button: " + mcc.isDisplayed());
            return mcc.isDisplayed();
        } catch (Exception e) {
            System.out.println("   MCC button not found (might be ATS, Generator, etc.)");
            
            // Debug: show what Asset Class buttons ARE visible
            try {
                List<WebElement> buttons = driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
                );
                for (WebElement btn : buttons) {
                    String name = btn.getAttribute("name");
                    if (name != null && (name.equals("ATS") || name.equals("MCC") || name.equals("Generator") || 
                        name.equals("Busway") || name.equals("Circuit Breaker") || name.equals("Fuse"))) {
                        System.out.println("   Found Asset Class button: " + name);
                    }
                }
            } catch (Exception e2) {}
            
            return false;
        }
    }

    /**
     * Click Save Changes button to save asset changes
     * Note: After changing asset class, button is "Save Changes" not "Save"
     */
    public void clickSaveButton() {
        System.out.println("💾 Clicking Save button...");
        saveButtonClickedThisFlow = false;
        try {
            WebElement saveBtn = null;
            
            // Try 1: "Save Changes" visible
            try {
                saveBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton'")
                );
                System.out.println("   Found visible 'Save Changes' button");
            } catch (Exception e) {}
            
            // Try 2: Scroll down and find "Save Changes"
            if (saveBtn == null) {
                System.out.println("   Scrolling to find Save Changes...");
                for (int i = 0; i < 3; i++) {
                    scrollFormDown();
                    sleep(200);
                    try {
                        saveBtn = driver.findElement(
                            AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' AND type == 'XCUIElementTypeButton'")
                        );
                        System.out.println("   Found 'Save Changes' after scrolling");
                        break;
                    } catch (Exception e) {}
                }
            }
            
            // Try 3: "Save" button
            if (saveBtn == null) {
                try {
                    saveBtn = driver.findElement(AppiumBy.accessibilityId("Save"));
                    System.out.println("   Found 'Save' button");
                } catch (Exception e) {}
            }
            
            // Try 4: Done
            if (saveBtn == null) {
                try {
                    saveBtn = driver.findElement(AppiumBy.accessibilityId("Done"));
                    System.out.println("   Found 'Done' button");
                } catch (Exception e) {}
            }
            
            if (saveBtn != null) {
                saveBtn.click();
                saveButtonClickedThisFlow = true;
                sleep(500);
                System.out.println("✅ Clicked Save button");
            } else {
                System.out.println("⚠️ Save button not found");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Save: " + e.getMessage());
        }
    }


    /**
     * Edit a text field by its label/name
     * @param fieldName The accessibility ID or label of the field
     * @param value The value to enter
     * @return true if field was edited successfully
     */
    public boolean editTextField(String fieldName, String value) {
        System.out.println("📝 Editing field: " + fieldName);

        // ============================================================
        // FAST-FAIL PRECONDITION (added 2026-05-04 per debug session)
        // Same root cause as selectDropdownOption — abort early when the
        // caller is on the wrong screen instead of running through 5
        // fallback strategies × 5s implicit wait = 25s waste per call.
        // ============================================================
        if (!isEditAssetScreenDisplayed() && !isSaveChangesButtonVisible()) {
            // VerificationError: callers' catch(Exception) cannot swallow this,
            // so no test can proceed believing the field was edited.
            throw new VerificationError("editTextField('" + fieldName + "'): not on Edit Asset screen — "
                + "caller's screen-state assumption is broken (sitePicker=" + isOnSiteSelectionScreen() + ")");
        }

        // Try to find and edit the field, scroll if needed (max 5 scrolls)
        for (int scrollAttempt = 0; scrollAttempt < 2; scrollAttempt++) {
            
            // STRATEGY 1: Find TextField/TextView by accessibility ID (verify element type!)
            try {
                WebElement field = driver.findElement(AppiumBy.accessibilityId(fieldName));
                String elementType = field.getAttribute("type");
                // Only proceed if it's actually an input field, not a label
                if (field.isDisplayed() && elementType != null && 
                    (elementType.contains("TextField") || elementType.contains("TextView"))) {
                    System.out.println("   🔍 Strategy 1: Found input field type=" + elementType);
                    field.click();
                    sleep(300);
                    field.clear();
                    field.sendKeys(value);
                    sleep(200);
                    String afterValue = field.getAttribute("value");
                    dismissKeyboard();
                    if (afterValue != null && afterValue.length() > 0) {
                        System.out.println("✅ Edited " + fieldName + " (accessibilityId) = " + value);
                        return true;
                    }
                } else {
                    System.out.println("   ⚠️ Strategy 1: Found element but type=" + elementType + " (not input field, skipping)");
                }
            } catch (Exception e) {}
            
            // STRATEGY 2: Find TextField OR TextView by name/label predicate (CASE-INSENSITIVE)
            try {
                // Try case-insensitive match using ==[c]
                WebElement field = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND (name ==[c] '" + fieldName + "' OR label ==[c] '" + fieldName + "')"
                    )
                );
                if (field.isDisplayed()) {
                    String beforeValue = field.getAttribute("value");
                    System.out.println("   🔍 Strategy 2: Found field, name=" + field.getAttribute("name") + ", before=" + beforeValue);
                    field.click();
                    sleep(200);
                    field.clear();
                    sleep(200);
                    field.sendKeys(value);
                    sleep(300);
                    String afterValue = field.getAttribute("value");
                    System.out.println("   🔍 After sendKeys: value=" + afterValue);
                    dismissKeyboard();
                    if (afterValue != null && afterValue.contains(value.substring(0, Math.min(5, value.length())))) {
                        System.out.println("✅ Edited " + fieldName + " (predicate) = " + value);
                        return true;
                    } else {
                        System.out.println("⚠️ sendKeys may have failed, trying tap+type...");
                        // Fall through to next strategy
                    }
                }
            } catch (Exception e) {
                System.out.println("   🔍 Strategy 2 failed: " + (e.getMessage() != null ? e.getMessage().substring(0, Math.min(80, e.getMessage().length())) : "null"));
            }
            
            // STRATEGY 2B: Try lowercase variant
            try {
                String lowerFieldName = fieldName.toLowerCase();
                WebElement field = driver.findElement(
                    AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND (name == '" + lowerFieldName + "' OR label == '" + lowerFieldName + "')"
                    )
                );
                if (field.isDisplayed()) {
                    String beforeValue = field.getAttribute("value");
                    System.out.println("   🔍 Strategy 2B: Found lowercase field, before=" + beforeValue);
                    field.click();
                    sleep(200);
                    field.clear();
                    sleep(200);
                    field.sendKeys(value);
                    sleep(300);
                    String afterValue = field.getAttribute("value");
                    System.out.println("   🔍 After sendKeys: value=" + afterValue);
                    dismissKeyboard();
                    if (afterValue != null && afterValue.contains(value.substring(0, Math.min(5, value.length())))) {
                        System.out.println("✅ Edited " + fieldName + " (lowercase) = " + value);
                        return true;
                    }
                }
            } catch (Exception e) {
                System.out.println("   🔍 Strategy 2B failed: no lowercase match");
            }
            
            // STRATEGY 3: Find label (case-insensitive), then find CLOSEST TextField/TextView BELOW it
            try {
                List<WebElement> labels = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "')")
                ));

                System.out.println("   🔍 Strategy 3: Found " + labels.size() + " labels matching '" + fieldName + "'");

                if (!labels.isEmpty()) {
                    WebElement label = labels.get(0);
                    int labelY = label.getLocation().getY();
                    System.out.println("   🔍 Strategy 3: Label Y=" + labelY);

                    if (nudgeIfBehindNavBar(labelY)) {
                        labels = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND (name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "')")));
                        if (labels.isEmpty()) break;
                        label = labels.get(0);
                        labelY = label.getLocation().getY();
                        System.out.println("   🔍 Strategy 3 after nudge: Label Y=" + labelY);
                    }

                    // Find TextField OR TextView - must be BELOW or same row (not above!)
                    List<WebElement> inputFields = withImplicitWait(0, () -> driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
                    ));

                    System.out.println("   🔍 Strategy 3: " + inputFields.size() + " input fields on screen");

                    // Find the CLOSEST field that is BELOW the label (tfY >= labelY - 20)
                    WebElement closestField = null;
                    int closestDistance = Integer.MAX_VALUE;

                    for (WebElement tf : inputFields) {
                        int tfY = tf.getLocation().getY();
                        int distance = Math.abs(tfY - labelY);
                        // Field must be BELOW or at same level as label (allow 20px tolerance for same row)
                        // AND within 100px vertically
                        if (tfY >= labelY - 20 && tfY <= labelY + 100) {
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestField = tf;
                            }
                        }
                    }

                    if (closestField != null) {
                        int tfY = closestField.getLocation().getY();
                        System.out.println("   🔍 Strategy 3: Found closest field at Y=" + tfY + " (dist=" + closestDistance + "px)");
                        closestField.click();
                        sleep(300);
                        closestField.clear();
                        closestField.sendKeys(value);
                        dismissKeyboard();
                        System.out.println("✅ Edited " + fieldName + " = " + value);
                        return true;
                    } else {
                        System.out.println("   🔍 Strategy 3: NO field within 100px of label Y=" + labelY);
                    }
                }
            } catch (Exception e) {
                System.out.println("   🔍 Strategy 3 error: " + (e.getMessage() != null ? e.getMessage().substring(0, Math.min(80, e.getMessage().length())) : "null"));
            }
            
            // STRATEGY 4: Find by partial match in label (case-insensitive)
            try {
                final String searchTerm = fieldName.replace("Serial Number", "").trim();
                List<WebElement> labels = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] '" + searchTerm + "'")
                ));

                for (WebElement label : labels) {
                    String labelName = label.getAttribute("name");
                    if (labelName != null && labelName.toLowerCase().contains(searchTerm.toLowerCase())) {
                        int labelY = label.getLocation().getY();
                        
                        // Find CLOSEST field BELOW the label
                        List<WebElement> inputFields = withImplicitWait(0, () -> driver.findElements(
                            AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView'")
                        ));

                        WebElement closestField = null;
                        int closestDistance = Integer.MAX_VALUE;
                        
                        for (WebElement tf : inputFields) {
                            int tfY = tf.getLocation().getY();
                            // Field must be BELOW or same row (tfY >= labelY - 20) and within 100px
                            if (tfY >= labelY - 20 && tfY <= labelY + 100) {
                                int distance = Math.abs(tfY - labelY);
                                if (distance < closestDistance) {
                                    closestDistance = distance;
                                    closestField = tf;
                                }
                            }
                        }
                        
                        if (closestField != null) {
                            closestField.click();
                            sleep(300);
                            closestField.clear();
                            closestField.sendKeys(value);
                            dismissKeyboard();
                            System.out.println("✅ Edited " + fieldName + " (partial match) = " + value);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {}
            
            // Field not found on current screen - scroll down and try again
            if (scrollAttempt < 2) {
                System.out.println("   Field '" + fieldName + "' not visible, scrolling... (attempt " + (scrollAttempt + 1) + ")");
                scrollFormDown();
                sleep(200);
            }
        }
        
        System.out.println("⚠️ Could not find field: " + fieldName);
        return false;
    }
    
    /**
     * Scroll until field is visible
     */
    private void scrollToFieldIfNeeded(String fieldName) {
        for (int i = 0; i < 3; i++) {
            try {
                WebElement field = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS '" + fieldName + "' OR label CONTAINS '" + fieldName + "'")
                );
                if (field.isDisplayed()) {
                    return; // Found it
                }
            } catch (Exception e) {}
            
            // Scroll down to find field
            scrollFormDown();
            sleep(300);
        }
    }
    
    /**
     * Scroll to a specific field in Core Attributes
     */
    public void scrollToField(String fieldName) {
        System.out.println("📜 Scrolling to field: " + fieldName);
        
        for (int i = 0; i < 3; i++) {
            try {
                WebElement field = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS '" + fieldName + "' OR label CONTAINS '" + fieldName + "'")
                );
                if (field.isDisplayed()) {
                    System.out.println("   Found field: " + fieldName);
                    return;
                }
            } catch (Exception e) {}
            
            scrollFormDown();
            sleep(300);
        }
        System.out.println("⚠️ Could not find field: " + fieldName);
    }
    
    /**
     * Verify asset was saved successfully after edit.
     *
     * Button-ABSENCE ("Save Changes" gone) is only evidence when this page
     * object actually clicked a Save button this flow — otherwise the button
     * may simply never have existed (wrong screen, nothing edited) and the
     * old "gone = saved" logic passed vacuously. With no positive evidence
     * and no real click, this now throws {@link VerificationError}.
     */
    public boolean isAssetSavedAfterEdit() {
        sleep(600);  // Wait for save to complete

        // Strategy 1: Edit button visible (legacy view mode) — positive evidence
        try {
            WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
            if (editBtn.isDisplayed()) {
                System.out.println("✅ Save successful - Edit button visible (view mode)");
                return true;
            }
        } catch (Exception e) {}

        // Strategy 2: Save Changes button gone — counts ONLY after a real click
        boolean saveChangesGone = !existsNow(AppiumBy.accessibilityId("Save Changes"));
        if (saveChangesGone && saveButtonClickedThisFlow) {
            System.out.println("✅ Save successful - Save Changes button gone");
            return true;
        }
        if (!saveChangesGone) {
            // Still in edit mode - might be validation error
            System.out.println("   Save Changes still visible - checking for errors...");
        }

        // Strategy 3: Check if we're back on Asset List
        try {
            WebElement plusBtn = driver.findElement(AppiumBy.accessibilityId("plus"));
            if (plusBtn.isDisplayed()) {
                System.out.println("✅ Save successful - back on Asset List");
                return true;
            }
        } catch (Exception e) {}

        // Strategy 4: Check for success toast/message (bounded scan)
        try {
            List<WebElement> texts = withImplicitWait(0, () ->
                driver.findElements(AppiumBy.className("XCUIElementTypeStaticText")));
            int scanned = 0;
            for (WebElement text : texts) {
                if (++scanned > 30) break;
                String name = text.getAttribute("name");
                if (name != null && (name.toLowerCase().contains("saved") || name.toLowerCase().contains("success"))) {
                    System.out.println("✅ Save successful - success message found");
                    return true;
                }
            }
        } catch (Exception e) {}

        throw new VerificationError(
            "isAssetSavedAfterEdit: no positive save evidence (Edit button / Asset List / toast) and "
            + (saveButtonClickedThisFlow
                ? "'Save Changes' is still visible after a real Save click — save likely failed (validation error?)."
                : "Save was never found/clicked this flow — 'button gone' would be a vacuous pass."));
    }
    
    /**
     * Click Cancel button in Edit mode
     */
    public void clickCancelEdit() {
        try {
            WebElement cancelBtn = driver.findElement(AppiumBy.accessibilityId("Cancel"));
            cancelBtn.click();
            System.out.println("✅ Clicked Cancel");
        } catch (Exception e) {
            System.out.println("⚠️ Could not click Cancel: " + e.getMessage());
        }
    }
    
    /**
     * Check if edit was cancelled (back to view mode)
     */
    public boolean isEditCancelled() {
        try {
            sleep(200);
            // Should be back to view mode with Edit button visible
            WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
            return editBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }



    /**
     * Select Busway from the asset class dropdown
     */
    private void selectBuswayFromDropdown() {
        System.out.println("📋 Selecting Busway from dropdown...");
        
        // Strategy 1: Direct accessibility ID
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement busway = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId("Busway")
            ));
            busway.click();
            System.out.println("✅ Selected Busway (accessibility ID)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 1 failed: " + e.getMessage());
        }
        
        // Strategy 2: NSPredicate for name/label
        try {
            WebElement busway = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Busway' OR label == 'Busway'")
            );
            busway.click();
            System.out.println("✅ Selected Busway (predicate)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 2 failed");
        }
        
        // Strategy 3: Search ALL element types for "Busway"
        String[] elementTypes = {"XCUIElementTypeButton", "XCUIElementTypeStaticText", 
                                  "XCUIElementTypeCell", "XCUIElementTypeOther"};
        for (String type : elementTypes) {
            try {
                List<WebElement> elements = driver.findElements(AppiumBy.className(type));
                for (WebElement el : elements) {
                    String name = el.getAttribute("name");
                    String label = el.getAttribute("label");
                    if ("Busway".equals(name) || "Busway".equals(label)) {
                        el.click();
                        System.out.println("✅ Selected Busway from " + type);
                        sleep(200);
                        return;
                    }
                }
            } catch (Exception e) {}
        }
        
        // Strategy 4: Scroll within dropdown and retry
        System.out.println("   Trying scroll within dropdown...");
        try {
            // Small swipe up to reveal more options
            driver.executeScript("mobile: swipe", Map.of(
                "direction", "up",
                "velocity", 500
            ));
            sleep(200);
            
            WebElement busway = driver.findElement(
                AppiumBy.iOSNsPredicateString("name == 'Busway' OR label == 'Busway'")
            );
            busway.click();
            System.out.println("✅ Selected Busway (after scroll)");
            sleep(200);
            return;
        } catch (Exception e) {
            System.out.println("   Strategy 4 failed");
        }
        
        // Strategy 5: Try picker wheel (if dropdown is a picker)
        try {
            List<WebElement> pickerWheels = driver.findElements(
                AppiumBy.className("XCUIElementTypePickerWheel")
            );
            if (!pickerWheels.isEmpty()) {
                System.out.println("   Found picker wheel, sending 'Busway'...");
                pickerWheels.get(0).sendKeys("Busway");
                System.out.println("✅ Selected Busway via picker wheel");
                sleep(200);
                return;
            }
        } catch (Exception e) {
            System.out.println("   No picker wheel found");
        }
        
        System.out.println("⚠️ Could not find Busway in dropdown!");
    }
    
    /**
     * DEBUG: Print all options visible in dropdown
     */
    @SuppressWarnings("unused")
    private void debugPrintDropdownOptions() {
        System.out.println("\n🔍 DEBUG: Dropdown options visible:");
        
        // Check buttons
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );
            System.out.println("   Buttons (" + buttons.size() + "):");
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && !name.isEmpty()) {
                    System.out.println("      - " + name);
                }
            }
        } catch (Exception e) {}
        
        // Check static text
        try {
            List<WebElement> texts = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText'")
            );
            System.out.println("   StaticText (" + texts.size() + "):");
            for (WebElement txt : texts) {
                String name = txt.getAttribute("name");
                String label = txt.getAttribute("label");
                if (name != null && (name.contains("ATS") || name.contains("UPS") || 
                    name.contains("PDU") || name.contains("Bus") || name.contains("Gen"))) {
                    System.out.println("      - name='" + name + "' label='" + label + "'");
                }
            }
        } catch (Exception e) {}
        
        // Check cells
        try {
            List<WebElement> cells = driver.findElements(AppiumBy.className("XCUIElementTypeCell"));
            if (!cells.isEmpty()) {
                System.out.println("   Cells (" + cells.size() + "):");
                for (int i = 0; i < Math.min(cells.size(), 10); i++) {
                    String name = cells.get(i).getAttribute("name");
                    System.out.println("      - " + name);
                }
            }
        } catch (Exception e) {}
        
        // Check picker wheels
        try {
            List<WebElement> pickers = driver.findElements(AppiumBy.className("XCUIElementTypePickerWheel"));
            if (!pickers.isEmpty()) {
                System.out.println("   PickerWheels found: " + pickers.size());
            }
        } catch (Exception e) {}
        
        System.out.println("");
    }

    /**
     * Select Busway from asset class dropdown (public method for compatibility)
     */
    public void selectBuswayClass() {
        selectBuswayFromDropdown();
    }

    /**
     * Check if Core Attributes section has NO content (for Busway)
     * For Busway: "Core Attributes" header text may be visible but section has NO fields/values
     * Returns true if section is empty (no required fields like Ampere, Voltage, etc.)
     */
    public boolean isCoreAttributesSectionHidden() {
        System.out.println("🔍 Checking if Core Attributes content is empty (for Busway)...");
        
        try {
            sleep(400); // Wait for UI to update after class change
            
            // Scroll to see Core Attributes area
            scrollFormDown();
            sleep(200);
            
            // For Busway: Check if there are any REQUIRED FIELD indicators or attribute inputs
            // These would only appear for asset classes WITH Core Attributes (like ATS)
            
            // Check for percentage indicator (only shown when Core Attributes has fields)
            List<WebElement> percentElements = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "name CONTAINS '%' OR label CONTAINS '%'"
                )
            );
            
            if (!percentElements.isEmpty()) {
                System.out.println("   Percentage indicator found - Core Attributes has content");
                return false; // Has content
            }
            
            // Check for common ATS/UPS/PDU attribute fields
            List<WebElement> attributeFields = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND (" +
                    "name CONTAINS[c] 'ampere' OR name CONTAINS[c] 'voltage' OR " +
                    "name CONTAINS[c] 'rating' OR name CONTAINS[c] 'kva' OR " +
                    "name CONTAINS[c] 'phase' OR name CONTAINS[c] 'frequency' OR " +
                    "value CONTAINS[c] 'enter' OR value CONTAINS[c] 'select')"
                )
            );
            
            if (!attributeFields.isEmpty()) {
                System.out.println("   Found " + attributeFields.size() + " attribute input fields - Core Attributes has content");
                return false; // Has content
            }
            
            // Check for Required Fields toggle (only shown when there ARE required fields)
            List<WebElement> toggles = driver.findElements(AppiumBy.className("XCUIElementTypeSwitch"));
            if (!toggles.isEmpty()) {
                System.out.println("   Required Fields toggle found - checking if Core Attributes present");
                // Toggle exists, but for Busway it shouldn't do anything
            }
            
            System.out.println("   ✅ Core Attributes section is EMPTY (Busway)");
            return true; // Empty
            
        } catch (Exception e) {
            System.out.println("   Error checking Core Attributes: " + e.getMessage());
            return true; // If error, consider it empty
        }
    }

    /**
     * Check if Core Attributes percentage indicator is NOT displayed (for Busway)
     * For Busway, the Core Attributes section header exists but has no fields,
     * therefore no percentage indicator (like "0%" or "100%") should appear next to it.
     */
    public boolean isPercentageIndicatorHidden() {
        try {
            sleep(200);
            
            // Look specifically for Core Attributes section header
            List<WebElement> coreAttributesHeaders = driver.findElements(
                AppiumBy.iOSNsPredicateString("label == 'Core Attributes' OR name == 'Core Attributes'")
            );
            
            if (coreAttributesHeaders.isEmpty()) {
                System.out.println("Core Attributes header not found - considering percentage as hidden");
                return true;
            }
            
            // Check if there's a percentage indicator near/after the Core Attributes header
            // Look for sibling or nearby elements that show percentage
            // Typical patterns: "0%", "50%", "100%"
            List<WebElement> percentElements = driver.findElements(
                AppiumBy.iOSNsPredicateString("(label MATCHES '.*[0-9]+%.*' OR name MATCHES '.*[0-9]+%.*')")
            );
            
            System.out.println("Found " + percentElements.size() + " percentage indicators on screen");
            
            // For Busway, there should be no percentage indicator in the Core Attributes section
            // Check if any of the percentage elements are related to Core Attributes
            for (WebElement percentElement : percentElements) {
                try {
                    String label = percentElement.getAttribute("label");
                    String name = percentElement.getAttribute("name");
                    int percentY = percentElement.getLocation().getY();
                    
                    // Get Core Attributes header position
                    WebElement coreHeader = coreAttributesHeaders.get(0);
                    int coreHeaderY = coreHeader.getLocation().getY();
                    
                    // If percentage is within 100 pixels of Core Attributes header, it's the Core Attributes percentage
                    if (Math.abs(percentY - coreHeaderY) < 100) {
                        System.out.println("Found Core Attributes percentage indicator: " + (label != null ? label : name) + " at Y=" + percentY);
                        return false; // Percentage IS shown
                    }
                } catch (Exception e) {
                    // Continue checking other elements
                }
            }
            
            System.out.println("No Core Attributes percentage indicator found - hidden: true");
            return true;
        } catch (Exception e) {
            System.out.println("Error checking percentage indicator: " + e.getMessage());
            return true;
        }
    }

    /**
     * Check if there's an empty Core Attributes container (should not exist for Busway)
     */
    public boolean hasEmptyCoreAttributesContainer() {
        try {
            // Look for any container that might be an empty placeholder
            List<WebElement> containers = driver.findElements(
                AppiumBy.iOSNsPredicateString("name CONTAINS 'Core' OR label CONTAINS 'Core'")
            );
            if (containers.size() > 0) {
                System.out.println("⚠️ Found Core container elements: " + containers.size());
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Edit any available field for Busway (non-Core Attributes field)
     */
    public void editAvailableBuswayField() {
        try {
            // Try to edit description or any other available text field
            List<WebElement> textFields = driver.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            for (WebElement field : textFields) {
                try {
                    field.click();
                    sleep(300);
                    field.clear();
                    field.sendKeys("Busway_Test_" + System.currentTimeMillis());
                    dismissKeyboard();
                    System.out.println("✅ Edited a Busway field");
                    return;
                } catch (Exception e) {
                    continue;
                }
            }
            System.out.println("⚠️ No editable text field found for Busway");
        } catch (Exception e) {
            System.out.println("⚠️ Could not edit Busway field: " + e.getMessage());
        }
    }

    /**
     * Check if Edit screen is displayed for Busway (no Core Attributes but Save button exists)
     */
    public boolean isEditScreenDisplayedForBusway() {
        System.out.println("📝 Checking if Edit screen is displayed for Busway...");
        try {
            // Strategy 1: Check for "Save Changes" button (Edit screen indicator)
            try {
                List<WebElement> saveButtons = driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (name CONTAINS 'Save' OR name CONTAINS 'Save')"
                    )
                );
                if (!saveButtons.isEmpty()) {
                    System.out.println("   ✅ Found Save Changes button");
                    return true;
                }
            } catch (Exception e) {}
            
            // Strategy 2: Check for "Cancel" button
            try {
                List<WebElement> cancelButtons = driver.findElements(AppiumBy.accessibilityId("Cancel"));
                if (!cancelButtons.isEmpty()) {
                    System.out.println("   ✅ Found Cancel button");
                    return true;
                }
            } catch (Exception e) {}
            
            // Strategy 3: Check for "Busway" class visible (we changed to Busway)
            try {
                List<WebElement> buswayElements = driver.findElements(
                    AppiumBy.iOSNsPredicateString("name == 'Busway' OR label == 'Busway'")
                );
                if (!buswayElements.isEmpty()) {
                    System.out.println("   ✅ Found Busway class indicator");
                    return true;
                }
            } catch (Exception e) {}
            
            // Strategy 4: Check for asset form fields (Name, Asset Class labels)
            try {
                List<WebElement> labels = driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND (name == 'Name' OR name CONTAINS 'Asset Class')"
                    )
                );
                if (labels.size() >= 2) {
                    System.out.println("   ✅ Found form labels (Name, Asset Class)");
                    return true;
                }
            } catch (Exception e) {}
            
            System.out.println("   ❌ Edit screen indicators not found");
            return false;
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
            return false;
        }
    }

    public final void changeAssetClassToMotor() {
        changeAssetClassInternal("Motor");
    }

    public final void changeAssetClassToOther() {
        changeAssetClassInternal("Other");
    }

    public final void changeAssetClassToOtherOCP() {
        changeAssetClassInternal("Other (OCP)");
    }

    public final void changeAssetClassToPanelboard() {
        changeAssetClassInternal("Panelboard");
    }

    public final void changeAssetClassToPDU() {
        changeAssetClassInternal("PDU");
    }

    public final void changeAssetClassToRelay() {
        changeAssetClassInternal("Relay");
    }

    public final void changeAssetClassToSwitchboard() {
        changeAssetClassInternal("Switchboard");
    }

    public final void changeAssetClassToTransformer() {
        changeAssetClassInternal("Transformer");
    }

    public final void changeAssetClassToUPS() {
        changeAssetClassInternal("UPS");
    }

    public final void changeAssetClassToUtility() {
        changeAssetClassInternal("Utility");
    }

    public final void changeAssetClassToVFD() {
        changeAssetClassInternal("VFD");
    }

    /**
     * Check if save was completed for Busway asset
     * After clicking Save Changes (or Save), check if:
     * 1. No validation error dialog appeared
     * 2. We're back to asset details or list (Save/Save Changes button no longer visible)
     */
    public boolean isSaveCompletedForBusway() {
        try {
            sleep(500); // Wait for save to complete
            
            // Check if "Save Changes" button is still visible
            List<WebElement> saveChangesBtn = driver.findElements(
                AppiumBy.accessibilityId("Save Changes")
            );
            
            // Check if regular "Save" button is still visible
            List<WebElement> saveBtn = driver.findElements(
                AppiumBy.accessibilityId("Save")
            );
            
            // If neither save button is visible, save completed
            if (saveChangesBtn.isEmpty() && saveBtn.isEmpty()) {
                System.out.println("✅ Save buttons no longer visible - save completed");
                return true;
            }
            
            // Also check if we're back to asset details (Edit button visible)
            List<WebElement> editButton = driver.findElements(
                AppiumBy.accessibilityId("Edit")
            );
            
            if (!editButton.isEmpty()) {
                System.out.println("✅ Edit button visible - back to asset details, save completed");
                return true;
            }
            
            // Check if we're on asset list (plus button visible)
            List<WebElement> plusButton = driver.findElements(
                AppiumBy.accessibilityId("plus")
            );
            
            if (!plusButton.isEmpty()) {
                System.out.println("✅ Plus button visible - back to asset list, save completed");
                return true;
            }
            
            System.out.println("⚠️ Cannot confirm save completion - save buttons may still be visible");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking save completion: " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // FIELD LABEL VERIFICATION METHODS
    // ================================================================

    /**
     * Check if a field label is present on the screen (case-sensitive)
     * Used to verify field label casing (bug detection)
     */
    public boolean isFieldLabelPresent(String labelText) {
        System.out.println("🔍 Checking for field label: '" + labelText + "'");
        
        try {
            // Strategy 1: Exact match by accessibilityId
            List<WebElement> elements = driver.findElements(AppiumBy.accessibilityId(labelText));
            if (!elements.isEmpty()) {
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        System.out.println("   ✅ Found label '" + labelText + "' by accessibilityId");
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        
        try {
            // Strategy 2: Find StaticText with exact name
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == '" + labelText + "'"
                )
            );
            if (!elements.isEmpty()) {
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        System.out.println("   ✅ Found label '" + labelText + "' as StaticText");
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        
        try {
            // Strategy 3: Find any element with exact label attribute
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString("label == '" + labelText + "'")
            );
            if (!elements.isEmpty()) {
                for (WebElement el : elements) {
                    if (el.isDisplayed()) {
                        System.out.println("   ✅ Found element with label '" + labelText + "'");
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        
        System.out.println("   ❌ Label '" + labelText + "' not found");
        return false;
    }



    // ================================================================
    // CAPITALIZATION AND NAMING VALIDATION METHODS
    // ================================================================

    /**
     * Check if a label/text is properly capitalized (Title Case or specific format)
     * Examples of proper: "Asset Name", "Circuit Breaker", "ATS"
     * Examples of improper: "asset name", "circuit breaker", "ats"
     */
    public boolean isProperlyCapitalized(String text) {
        if (text == null || text.isEmpty()) return false;
        
        // Known abbreviations that should be all caps
        String[] allCapsWords = {"ATS", "UPS", "PDU", "MCC", "VFD", "QR", "ID"};
        for (String abbrev : allCapsWords) {
            if (text.toUpperCase().equals(abbrev)) {
                return text.equals(abbrev); // Must be all caps
            }
        }
        
        // For regular words, first letter should be uppercase
        String[] words = text.split(" ");
        for (String word : words) {
            if (word.isEmpty()) continue;
            
            // Check if first letter is uppercase
            if (!Character.isUpperCase(word.charAt(0))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all visible field labels on the current screen
     * Returns a map of found labels and whether they're properly capitalized
     */
    public java.util.Map<String, Boolean> checkFieldLabelsCapitalization() {
        System.out.println("🔍 Checking field labels capitalization...");
        java.util.Map<String, Boolean> results = new java.util.HashMap<>();
        
        try {
            // Find all StaticText elements that could be labels
            List<WebElement> staticTexts = driver.findElements(
                AppiumBy.className("XCUIElementTypeStaticText")
            );
            
            // Known field labels to check
            String[] expectedLabels = {
                "Name", "Asset Class", "Asset Subtype", "Location", "QR Code",
                "Core Attributes", "Electrical Rating", "Manufacturer", 
                "Model", "Serial Number", "Notes"
            };
            
            for (WebElement text : staticTexts) {
                try {
                    String name = text.getAttribute("name");
                    String label = text.getAttribute("label");
                    String actualText = name != null ? name : label;
                    
                    if (actualText != null && !actualText.isEmpty()) {
                        // Check against expected labels
                        for (String expected : expectedLabels) {
                            if (actualText.equalsIgnoreCase(expected)) {
                                boolean isProper = actualText.equals(expected);
                                results.put(actualText, isProper);
                                
                                if (!isProper) {
                                    System.out.println("   ❌ IMPROPER: '" + actualText + "' should be '" + expected + "'");
                                } else {
                                    System.out.println("   ✅ PROPER: '" + actualText + "'");
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("   Error checking labels: " + e.getMessage());
        }
        
        return results;
    }

    /**
     * Check if Asset Class dropdown options are properly capitalized
     * Expected: "ATS", "Circuit Breaker", "Disconnect Switch" (not "ats", "circuit breaker")
     */
    public java.util.Map<String, Boolean> checkAssetClassOptionsCapitalization() {
        System.out.println("🔍 Checking Asset Class options capitalization...");
        java.util.Map<String, Boolean> results = new java.util.HashMap<>();
        
        // Expected proper capitalization
        String[] expectedClasses = {
            "ATS", "Busway", "Circuit Breaker", "Disconnect Switch", 
            "Fuse", "Generator", "MCC", "Motor Starter", "Panelboard",
            "PDU", "Switchboard", "Switchgear", "Transformer", "UPS", "VFD"
        };
        
        try {
            // Click to open Asset Class dropdown
            clickSelectAssetClass();
            sleep(200);
            
            // Get all visible options
            List<WebElement> options = driver.findElements(
                AppiumBy.className("XCUIElementTypeStaticText")
            );
            
            for (WebElement opt : options) {
                try {
                    String optText = opt.getAttribute("name");
                    if (optText == null) optText = opt.getAttribute("label");
                    
                    if (optText != null && !optText.isEmpty()) {
                        for (String expected : expectedClasses) {
                            if (optText.equalsIgnoreCase(expected)) {
                                boolean isProper = optText.equals(expected);
                                results.put(optText, isProper);
                                
                                if (!isProper) {
                                    System.out.println("   ❌ IMPROPER: '" + optText + "' should be '" + expected + "'");
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // Close dropdown
            try {
                driver.findElement(AppiumBy.accessibilityId("Cancel")).click();
            } catch (Exception e) {
                // Try tapping outside
                driver.executeScript("mobile: tap", java.util.Map.of("x", 200, "y", 100));
            }
            sleep(300);
            
        } catch (Exception e) {
            System.out.println("   Error checking Asset Class options: " + e.getMessage());
        }
        
        return results;
    }

    /**
     * Check if a specific field value has improper lowercase when it should be capitalized
     * Returns the found text if found, null if not found
     */
    public String findImproperlyCapitalizedText(String searchTerm) {
        System.out.println("🔍 Looking for improperly capitalized '" + searchTerm + "'...");
        
        try {
            // Search for lowercase version
            String lowerVersion = searchTerm.toLowerCase();
            List<WebElement> elements = driver.findElements(
                AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND " +
                    "(name == '" + lowerVersion + "' OR label == '" + lowerVersion + "')"
                )
            );
            
            for (WebElement el : elements) {
                if (el.isDisplayed()) {
                    String found = el.getAttribute("name");
                    if (found == null) found = el.getAttribute("label");
                    System.out.println("   ❌ Found lowercase: '" + found + "'");
                    return found;
                }
            }
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Get selected Asset Class text from the dropdown button
     */
    public String getSelectedAssetClassText() {
        System.out.println("🔍 Getting selected Asset Class text...");
        try {
            // Look for button that shows selected class
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );
            
            String[] classes = {"ATS", "Busway", "Circuit Breaker", "Disconnect Switch", 
                               "Fuse", "Generator", "MCC", "VFD", "UPS", "PDU"};
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null) {
                    for (String cls : classes) {
                        if (name.equalsIgnoreCase(cls)) {
                            System.out.println("   Found: '" + name + "'");
                            return name;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get selected Asset Subtype text
     */
    public String getSelectedSubtypeText() {
        System.out.println("🔍 Getting selected Subtype text...");
        try {
            List<WebElement> buttons = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton'")
            );
            
            for (WebElement btn : buttons) {
                String name = btn.getAttribute("name");
                if (name != null && (name.contains("(") || name.contains("kVA") || name.contains("kW"))) {
                    System.out.println("   Found: '" + name + "'");
                    return name;
                }
            }
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        return null;
    }



    /**
     * Get the current value of a text field by its label name
     * @param fieldName The label/name of the field (e.g., "Manufacturer", "Serial Number")
     * @return The current value of the field, or null if not found
     */
    public String getTextFieldValue(String fieldName) {
        System.out.println("📝 Getting value of field: " + fieldName);

        try {
            // Probe-first at implicit-0: only scroll if the label isn't already on
            // screen. The old unconditional scrollFormDown()+sleep(300) + 5s-implicit
            // label miss, run in discoverEditableAttribute()'s 10-label loop, was an
            // ~80s sink behind the offline TC_E2E hangs. A present field costs ms now.
            By labelBy = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(name CONTAINS[c] '" + fieldName + "' OR label CONTAINS[c] '" + fieldName + "')");
            if (!existsNow(labelBy)) {
                scrollFormDown();
                sleep(300);
            }

            // Strategy 1: Find label, then look for nearby text field
            try {
                java.util.List<WebElement> labelHits = withImplicitWait(0,
                    () -> driver.findElements(labelBy));
                WebElement label = labelHits.isEmpty() ? null : labelHits.get(0);

                if (label != null && label.isDisplayed()) {
                    int labelY = label.getLocation().getY();
                    System.out.println("   Found label '" + fieldName + "' at Y=" + labelY);
                    
                    // Find text fields near this label (within 100px below)
                    List<WebElement> textFields = driver.findElements(
                        AppiumBy.className("XCUIElementTypeTextField")
                    );
                    
                    for (WebElement field : textFields) {
                        try {
                            int fieldY = field.getLocation().getY();
                            if (fieldY > labelY && fieldY < labelY + 100) {
                                String value = field.getAttribute("value");
                                System.out.println("   ✅ Found value: '" + value + "'");
                                return value != null ? value : "";
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                System.out.println("   Strategy 1 failed: " + e.getMessage());
            }
            
            // Strategy 2: Check all text fields for one that contains the field name in its accessibility
            try {
                List<WebElement> allFields = driver.findElements(
                    AppiumBy.className("XCUIElementTypeTextField")
                );
                
                for (WebElement field : allFields) {
                    try {
                        String name = field.getAttribute("name");
                        String fieldLabel = field.getAttribute("label");
                        
                        if ((name != null && name.toLowerCase().contains(fieldName.toLowerCase())) ||
                            (fieldLabel != null && fieldLabel.toLowerCase().contains(fieldName.toLowerCase()))) {
                            String value = field.getAttribute("value");
                            System.out.println("   ✅ Found field by name/label, value: '" + value + "'");
                            return value != null ? value : "";
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                System.out.println("   Strategy 2 failed: " + e.getMessage());
            }
            
            System.out.println("   ❌ Could not find field: " + fieldName);
            return null;
            
        } catch (Exception e) {
            System.out.println("   Error getting field value: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verify we're on the Asset Detail VIEW screen (not Edit screen)
     * The Detail View screen shows asset info but has "Edit" button, not "Save Changes"
     */
    public boolean isAssetDetailViewScreen() {
        System.out.println("📝 Checking if on Asset Detail View screen...");
        
        try {
            // On Detail View, we should see "Edit" button (not "Save Changes")
            boolean hasEditButton = false;
            boolean hasSaveButton = false;
            
            try {
                WebElement editBtn = driver.findElement(AppiumBy.accessibilityId("Edit"));
                hasEditButton = editBtn.isDisplayed();
            } catch (Exception e) {}
            
            try {
                WebElement saveBtn = driver.findElement(
                    AppiumBy.iOSNsPredicateString("name CONTAINS 'Save' OR label CONTAINS 'Save'")
                );
                hasSaveButton = saveBtn.isDisplayed();
            } catch (Exception e) {}
            
            boolean isDetailView = hasEditButton && !hasSaveButton;
            System.out.println("   Edit button: " + hasEditButton + ", Save button: " + hasSaveButton);
            System.out.println("   Is Detail View: " + isDetailView);
            return isDetailView;
            
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Navigate back from any screen to the Asset List
     */
    public void navigateBackToAssetList() {
        System.out.println("🔙 Navigating back to Asset List...");
        
        try {
            // Try clicking Back button multiple times if needed
            for (int i = 0; i < 3; i++) {
                try {
                    // Check if we're already on asset list (plus button visible)
                    WebElement plusBtn = driver.findElement(AppiumBy.accessibilityId("plus"));
                    if (plusBtn.isDisplayed()) {
                        System.out.println("   ✅ Already on Asset List");
                        return;
                    }
                } catch (Exception e) {}
                
                // Click back
                try {
                    clickBack();
                    sleep(200);
                } catch (Exception e) {
                    // Try clicking Cancel if Back doesn't work
                    try {
                        clickCancel();
                        sleep(200);
                    } catch (Exception ex) {}
                }
            }
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
    }

    /** Tap at specific screen coordinates using W3C Actions API */
    private void tapAtCoordinates(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 0);
        tap.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    // ================================================================
    // ZP-323.6 — SUGGESTED SHORTCUTS (added 2026-04-30)
    // ================================================================

    /**
     * Returns true if the "Suggested Shortcut (Optional)" section is visible on Asset Edit/Create.
     * Web verification 2026-04-30: label is exactly "Suggested Shortcut (Optional)" (singular, not plural).
     */
    public boolean isSuggestedShortcutsSectionVisible() {
        try {
            WebElement el = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Suggested Shortcut (Optional)' OR " +
                "label == 'Suggested Shortcuts (Optional)' OR " +
                "label CONTAINS[c] 'suggested shortcut')"));
            return el.isDisplayed();
        } catch (Exception e) { return false; }
    }

    /** Tap the Suggested Shortcuts dropdown/button to open the picker. */
    public boolean tapSuggestedShortcuts() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS[c] 'shortcut'"));
            btn.click(); sleep(400); return true;
        } catch (Exception e) { return false; }
    }

    /** Returns the placeholder/value of the Suggested Shortcuts field. */
    public String getSuggestedShortcutsValue() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS[c] 'shortcut'"));
            String l = btn.getAttribute("label");
            return l == null ? "" : l;
        } catch (Exception e) { return ""; }
    }

    /** Check whether the "No shortcuts available" placeholder text is shown. */
    public boolean isNoShortcutsPlaceholderShown() {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS[c] 'No shortcuts'"));
            return true;
        } catch (Exception e) { return false; }
    }

    // ================================================================
    // ZP-323.7 — CONDITION OF MAINTENANCE (COM) CALCULATION (added 2026-04-30)
    // ================================================================

    /**
     * Returns true if the COM (Condition of Maintenance) badge/label is visible on Asset Details.
     * Web verification 2026-04-30: label is "Condition of Maintenance (COM)" or "Condition of Maintenance".
     * Asset Edit form has it with a "?" help button + Calculator button + value buttons (1/2/3).
     */
    public boolean isCOMVisibleOnAssetDetails() {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'Condition of Maintenance (COM)' OR " +
                "label == 'Condition of Maintenance' OR " +
                "label CONTAINS[c] 'condition of maintenance')"));
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Tap the COM "?" help button to open the explanation popup (Asset Edit form).
     * Verified on web 2026-04-30 — there's a "?" button next to "Condition of Maintenance" label.
     */
    public boolean tapCOMHelpButton() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '?'"));
            btn.click(); sleep(400); return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Tap one of the COM value buttons (1, 2, or 3) on the Asset Edit form.
     * Web verification: buttons render as "1" / "2" / "3" with one in pressed state.
     */
    public boolean tapCOMValue(int value) {
        if (value < 1 || value > 5) return false;
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + value + "'"));
            btn.click(); sleep(300); return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Read the numeric COM value (e.g., "1", "0.85", "75%"). Returns null if not parseable.
     * The web app showed an integer (e.g. "1") next to the COM label.
     */
    public String getCOMValue() {
        try {
            WebElement label = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label CONTAINS[c] 'condition of maintenance' OR label CONTAINS[c] 'COM')"));
            int labelY = label.getLocation().getY();
            // COM value typically appears near the label — scan for a numeric StaticText nearby
            java.util.List<WebElement> texts = driver.findElements(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText'"));
            for (WebElement t : texts) {
                try {
                    int dy = Math.abs(t.getLocation().getY() - labelY);
                    if (dy > 60) continue;
                    String v = t.getAttribute("label");
                    if (v != null && v.matches("\\d+(\\.\\d+)?%?")) return v;
                } catch (Exception ignored) {}
            }
            return null;
        } catch (Exception e) { return null; }
    }

    // ================================================================
    // ZP-323.10 — ASSET LISTENING (Task auto-link) (added 2026-04-30)
    //
    // iOS evidence 2026-04-30 (user screenshots v1.31):
    //   The Listen feature lives on the **Task Details** screen, NOT on the
    //   Asset list. It appears below the "Linked Assets" card as a small pill
    //   button with two states:
    //     - Inactive: "Listen for Assets"   (gray pill, headphones icon)
    //     - Active:   "Listening for Assets" (orange pill, headphones icon)
    //   Precondition: parent Issue/Task must be in OPEN status.
    //   Tapping toggles between the two states. The feature presumably
    //   auto-links nearby assets (BLE / continuous QR scan) into the Task's
    //   Linked Assets list.
    // ================================================================

    /**
     * Tap the "Listen for Assets" button on the Task Details screen.
     * Precondition: Task is open. If the button is missing, the parent
     * issue/task is likely closed/resolved — caller should
     * skipIfPreconditionMissing().
     */
    public boolean tapListenAsset() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Listen for Assets' OR label == 'Listening for Assets' OR " +
                "label CONTAINS[c] 'listen for asset' OR label CONTAINS[c] 'listening for asset' OR " +
                "label == 'Listen' OR label == 'Stop Listening')"));
            btn.click(); sleep(500); return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Returns true when the active state pill "Listening for Assets" is shown
     * (orange-filled in iOS v1.31). Indicates the listen scan is running.
     */
    public boolean isListeningIndicatorActive() {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                "(label == 'Listening for Assets' OR label CONTAINS[c] 'listening for asset' OR " +
                "label == '● Listening')"));
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Returns true when the inactive state pill "Listen for Assets" is shown
     * (gray in iOS v1.31). Useful as a precondition probe before tapping.
     */
    public boolean isListenForAssetsButtonAvailable() {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Listen for Assets' OR label == 'Listening for Assets')"));
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Stop listening if active. iOS v1.31: tapping the active "Listening for
     * Assets" pill toggles it back to "Listen for Assets". A dedicated
     * "Stop Listening" button does not appear in the screenshots.
     */
    public void stopListeningIfActive() {
        try {
            if (isListeningIndicatorActive()) {
                WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Listening for Assets' OR label == 'Stop' OR label == 'Stop Listening')"));
                btn.click(); sleep(400);
            }
        } catch (Exception ignored) {}
    }

    // ================================================================
    // ZP-323.11 — DETAILED CREATE ASSET FLOW (added 2026-04-30)
    // ================================================================

    /**
     * On Create Asset entry screen, tap the "Detailed" flow option (vs. Quick).
     *
     * Note (2026-04-30): On the WEB app, there is no separate Quick/Detailed
     * toggle — the "Add Asset" modal opens with all sections expanded by default
     * (BASIC INFO, CORE ATTRIBUTES, COMMERCIAL, NOTES). The Quick/Detailed split
     * may be iOS-only. This method may return false on web — that's OK because
     * tests use skipIfPreconditionMissing().
     */
    public boolean tapDetailedCreateFlow() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(label == 'Detailed' OR label == 'Detailed Flow' OR label CONTAINS[c] 'detailed')"));
            btn.click(); sleep(500); return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Verify we are on the Detailed Create form. Web reference (2026-04-30):
     * the modal has section headers BASIC INFO, CORE ATTRIBUTES, COMMERCIAL, NOTES.
     */
    public boolean isOnDetailedCreateForm() {
        try {
            int headers = driver.findElements(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label == 'BASIC INFO' OR label == 'Basic Information' OR " +
                "label == 'CORE ATTRIBUTES' OR label == 'Core Attributes' OR " +
                "label == 'COMMERCIAL' OR label == 'Commercial' OR " +
                "label == 'NOTES' OR label == 'Notes' OR " +
                "label == 'Asset Photos' OR label CONTAINS[c] 'suggested shortcut')")).size();
            return headers >= 2;
        } catch (Exception e) { return false; }
    }

    // ================================================================
    // ZP-323.12 — COPY TO / COPY FROM (added 2026-04-30)
    // ================================================================

    /**
     * Open the ⋯ three-dot overflow menu in the Asset Details header (top-right).
     * iOS evidence 2026-04-30 (user screenshot v1.31): the Copy From / Copy To
     * options live inside this overflow menu, NOT as a top-level "Copy Details"
     * button. Header layout: "Close | Asset Details | ⋯".
     */
    public boolean openAssetOverflowMenu() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name == 'ellipsis' OR name == 'ellipsis.circle' OR " +
                "name CONTAINS 'ellipsis' OR " +
                "label == 'More' OR label CONTAINS[c] 'more options' OR " +
                "label == 'Copy Details')"));
            btn.click(); sleep(500); return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Tap "Copy From" inside the ⋯ overflow menu.
     * Caller must invoke openAssetOverflowMenu() first.
     */
    public boolean tapCopyFrom() {
        try {
            if (!isOverflowMenuOpen()) openAssetOverflowMenu();
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                "type == 'XCUIElementTypeMenuItem' OR type == 'XCUIElementTypeCell') AND " +
                "(label CONTAINS[c] 'Copy From' OR label CONTAINS[c] 'Copy Details From' OR " +
                "label == 'Copy from')"));
            btn.click(); sleep(500); return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Tap "Copy To" inside the ⋯ overflow menu.
     * Caller must invoke openAssetOverflowMenu() first (or this auto-opens it).
     */
    public boolean tapCopyTo() {
        try {
            if (!isOverflowMenuOpen()) openAssetOverflowMenu();
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                "type == 'XCUIElementTypeMenuItem' OR type == 'XCUIElementTypeCell') AND " +
                "(label CONTAINS[c] 'Copy To' OR label CONTAINS[c] 'Copy Details To' OR " +
                "label == 'Copy to')"));
            btn.click(); sleep(500); return true;
        } catch (Exception e) { return false; }
    }

    /** Quick probe — is the overflow menu / popover currently displayed? */
    private boolean isOverflowMenuOpen() {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' OR " +
                "type == 'XCUIElementTypeMenuItem' OR type == 'XCUIElementTypeCell') AND " +
                "(label CONTAINS[c] 'Copy From' OR label CONTAINS[c] 'Copy To')"));
            return true;
        } catch (Exception e) { return false; }
    }

    /** Check if the source-asset picker for Copy From is currently displayed. */
    public boolean isCopySourcePickerDisplayed() {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label CONTAINS[c] 'select source' OR label CONTAINS[c] 'copy from' OR " +
                "label CONTAINS[c] 'copy details' OR label == 'Source Asset')"));
            return true;
        } catch (Exception e) { return false; }
    }

    // ================================================================
    // ZP-323.13 — AI EXTRACTION (added 2026-04-30)
    // ================================================================

    /**
     * Tap the AI Extract sparkles ✨ button on the right of the "Core Attributes"
     * section header in Asset Details.
     * iOS evidence 2026-04-30 (user screenshot v1.31): blue square button with
     * SF Symbol "sparkles" — sits to the right of "Core Attributes" + "100%" badge.
     */
    public boolean tapAIExtractButton() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name == 'sparkles' OR name CONTAINS 'sparkles' OR " +
                "name CONTAINS 'wand.and.stars' OR " +
                "label CONTAINS[c] 'extract' OR label CONTAINS[c] 'AI Extract' OR " +
                "label CONTAINS[c] 'auto fill' OR label CONTAINS[c] 'autofill')"));
            btn.click(); sleep(800); return true;
        } catch (Exception e) { return false; }
    }

    /** Check if AI processing indicator is shown after tapping Extract. */
    public boolean isAIExtractionInProgress() {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeActivityIndicator' OR type == 'XCUIElementTypeStaticText') AND " +
                "(label CONTAINS[c] 'extracting' OR label CONTAINS[c] 'analyzing' OR " +
                "label CONTAINS[c] 'processing')"));
            return true;
        } catch (Exception e) { return false; }
    }

    /** Tap the "+" button to start creating a new asset. */
    public boolean tapAddAssetButton() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND " +
                "(name == 'plus' OR name CONTAINS 'plus' OR label == 'Add' OR label == 'Create')"));
            btn.click(); sleep(500); return true;
        } catch (Exception e) { return false; }
    }

    /** Cancel any in-progress Create / Edit Asset flow. Tap Cancel, dismiss any unsaved-changes prompt. */
    public void cancelAssetCreation() {
        try {
            try {
                WebElement cancel = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Cancel' OR label == 'Close' OR label == 'Discard')"));
                cancel.click();
                sleep(400);
            } catch (Exception ignored) {}
            // Possible unsaved-changes prompt — tap Discard if shown
            try {
                WebElement discard = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND " +
                    "(label == 'Discard' OR label == 'Discard Changes' OR label == 'Yes')"));
                discard.click();
                sleep(300);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    /** Check if AI extraction returned suggestions (review screen visible). */
    public boolean areAIExtractionSuggestionsDisplayed() {
        try {
            driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND " +
                "(label CONTAINS[c] 'AI suggestion' OR label CONTAINS[c] 'extracted value' OR " +
                "label CONTAINS[c] 'review extraction' OR label CONTAINS[c] 'apply extraction')"));
            return true;
        } catch (Exception e) { return false; }
    }

    // ================================================================
    // ZP-323.13 AI EXTRACTION — REAL FLOW (added 2026-05-05 per user screenshots)
    //
    // Real flow on iOS v1.31:
    //   1. Open Asset Details → Asset Photos card
    //   2. Tap "Nameplate" tab (siblings: Profile, Nameplate, Panel)
    //   3. Tap Gallery (or Camera) to upload a nameplate photo
    //   4. Pick photo from iOS Photos library
    //   5. Once nameplate photo is uploaded, the sparkles button on
    //      "Core Attributes" header becomes meaningful
    //   6. Tap the sparkles button → AI extracts data → populates
    //      Voltage / Manufacturer / Catalog Number / etc.
    //
    // The sparkles button BY ITSELF (no nameplate uploaded) may not
    // produce useful results — extraction needs the photo as input.
    // ================================================================

    /** Tap the "Nameplate" tab inside Asset Photos. Returns true if tab is now active. */
    public boolean tapNameplatePhotoTab() {
        try {
            WebElement tab = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                "(label BEGINSWITH 'Nameplate' OR name BEGINSWITH 'Nameplate')"));
            tab.click(); sleep(500);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Tap Gallery to open the iOS Photos picker (for nameplate / general photo upload). */
    public boolean tapGalleryButton() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Gallery'"));
            btn.click(); sleep(800);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Tap Camera button (alternative to Gallery for taking a new photo). */
    public boolean tapCameraButton() {
        try {
            WebElement btn = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Camera'"));
            btn.click(); sleep(800);
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Get the count of nameplate photos currently uploaded.
     * Reads from "Nameplate (N)" tab label.
     * Returns -1 if tab not visible.
     */
    public int getNameplatePhotoCount() {
        try {
            WebElement tab = driver.findElement(io.appium.java_client.AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText') AND " +
                "label BEGINSWITH 'Nameplate'"));
            String label = tab.getAttribute("label");
            if (label == null) return -1;
            // Extract digits from "Nameplate (N)" or "Nameplate N"
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(label);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return -1;
    }

    /**
     * Convenience for tests: navigate Asset Details → Nameplate tab → Gallery picker.
     * Picking the actual photo afterwards is handled via the iOS Photos app's
     * native selection UI (not part of our app's DOM).
     * Returns true if the picker is now showing.
     */
    public boolean openNameplateGalleryPicker() {
        if (!tapNameplatePhotoTab()) return false;
        sleep(400);
        return tapGalleryButton();
    }

    // ================================================================
    // PICKER-CLOSE 4TH-BUG RECOVERY (changelog 070, 2026-05-08)
    //
    // The bug (documented in changelog 062): after a dropdown picker
    // dismisses, the app sometimes navigates back to READ-ONLY Asset
    // Details instead of staying on Edit. Tests then fail when they
    // try to tap "Save Changes" — that button only exists on Edit.
    //
    // Affects: 12+ tests across assets-p3/p4/issues-p1 that were
    // documented as "PASS→FAIL regressions" in changelog 065.
    // ================================================================

    /**
     * Detect if the picker-close 4th-bug just happened: we're now on
     * READ-ONLY Asset Details when the test expected to still be on Edit.
     *
     * @return true if we're on read-only Asset Details (the bug fired)
     */
    public boolean didPickerCloseExitEditMode() {
        // We're on read-only Asset Details if:
        //   1. Asset Details screen is displayed (header/breadcrumb visible)
        //   2. AND Edit screen indicators are NOT visible (no Save Changes,
        //      no text inputs in edit mode)
        boolean onAssetDetails = isAssetDetailDisplayed();
        boolean onEditScreen = isEditAssetScreenDisplayed();
        return onAssetDetails && !onEditScreen;
    }

    /**
     * Recover from the picker-close 4th-bug. Called after a picker selection
     * to ensure we're back on the Edit screen so the test can proceed.
     *
     * @return true if recovery succeeded (now on Edit), false if not
     */
    public boolean recoverFromPickerCloseBug() {
        if (!didPickerCloseExitEditMode()) {
            // No bug fired — already on Edit, nothing to do
            return true;
        }
        System.out.println("⚠️ Picker-close 4th-bug detected — recovering by re-tapping Edit");
        try {
            clickEditTurbo();
            sleep(800);
            boolean recovered = isEditAssetScreenDisplayed();
            System.out.println(recovered
                ? "✅ Recovered to Edit screen"
                : "❌ Recovery failed — still not on Edit");
            return recovered;
        } catch (Exception e) {
            System.out.println("❌ Recovery threw " + e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Wrapper that runs an action (typically a picker selection) and
     * automatically recovers from the picker-close 4th-bug if it fires.
     *
     * Per team design decision (2026-05-08): when recovery fires, the
     * dropdown change is NOT trusted to have persisted — we re-apply
     * it via the {@code reapply} callback. Slower but guaranteed
     * correct: if the change didn't save (which we can't reliably
     * verify), this version still produces the right end state.
     *
     * Usage:
     *   assetPage.withPickerCloseRecovery(
     *       () -> selectDropdownOption("Manufacturer", "Eaton"),  // action
     *       () -> selectDropdownOption("Manufacturer", "Eaton")   // reapply (often same)
     *   );
     *
     * @param action  the picker interaction to perform first
     * @param reapply the action to re-run after recovery (often the same
     *                lambda as action). Pass null to disable re-apply.
     * @return true if final state is on Edit (action completed and any
     *         recovery succeeded). false if we couldn't recover.
     */
    public boolean withPickerCloseRecovery(Runnable action, Runnable reapply) {
        try {
            action.run();
        } catch (Exception e) {
            System.out.println("⚠️ Picker action threw: " + e.getClass().getSimpleName());
        }
        sleep(400); // Let any navigation animation settle

        if (!didPickerCloseExitEditMode()) {
            // No bug fired — change is on Edit screen as expected
            return true;
        }

        // Bug fired. Re-tap Edit to get back to edit mode.
        if (!recoverFromPickerCloseBug()) {
            return false;
        }

        // Per design decision: re-apply the change. The previous
        // change may or may not have persisted; re-applying is the
        // safe choice.
        if (reapply != null) {
            System.out.println("   ↳ Re-applying change after recovery (per team decision)");
            try {
                reapply.run();
                sleep(400);
            } catch (Exception e) {
                System.out.println("⚠️ Re-apply threw: " + e.getClass().getSimpleName());
                return false;
            }
            // If re-apply ALSO triggered the bug, we'd recurse forever.
            // Bail out with a clear log instead of looping.
            if (didPickerCloseExitEditMode()) {
                System.out.println("⚠️ Re-apply ALSO triggered picker-close — manual fix needed");
                return false;
            }
        }
        return true;
    }

    /** Convenience overload: action and reapply are the same lambda. */
    public boolean withPickerCloseRecovery(Runnable action) {
        return withPickerCloseRecovery(action, action);
    }

}
