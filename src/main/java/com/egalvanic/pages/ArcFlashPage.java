package com.egalvanic.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.egalvanic.base.BasePage;
import com.egalvanic.verify.VerificationError;

import io.appium.java_client.AppiumBy;

/**
 * Arc Flash data-readiness surfaces (v1.49, NOT feature-flag gated — available
 * on every tenant, unlike eng-lib).
 *
 * Surfaces covered (source: app-source Views/ArcFlash + SiteTabView + tabs):
 *  1. Site tab "Arc Flash" quick-action card → full-screen "Arc Flash Analysis"
 *     dashboard: Readiness Score ring, "{n} Completed/Remaining/Total Items"
 *     stats, three metric cards (Asset Details / Source/Target / Connection
 *     Details, each "X of Y" + %), "<Metric> Breakdown" disclosure groups with
 *     fixed bucket labels (0%, 1-25%, 26-50%, 51-75%, 76-99%, 100%), Done.
 *  2. Assets tab ellipsis menu "Show/Hide AF Punchlist" row badges.
 *  3. Connections tab — same toggle for edges.
 *
 * Locator contract (mirrors AssetEngineerPage lessons):
 *  - ZERO accessibilityIdentifiers; name == label == visible text.
 *  - iOS 26.2: click() silently no-ops on menu rows AND ordinary buttons —
 *    every tap goes through the W3C pressElement primitive.
 *  - Menus/overlays append at tree end — always take the LAST visible match.
 *  - Percent labels are plain "N%" StaticTexts; the dashboard has many.
 *    Identification is by DOCUMENT ORDER within a scope, never by index math
 *    on the whole tree.
 */
public class ArcFlashPage extends BasePage {

    // ── Exact strings (AppStrings.swift defaults, English) ─────────────
    public static final String QUICK_ACTION_CARD = "Arc Flash";
    public static final String NAV_TITLE = "Arc Flash Analysis";
    public static final String READINESS_SCORE = "Readiness Score";
    public static final String OVERALL_CAPTION = "Overall";
    public static final String LOADING_LABEL = "Loading Arc Flash Analysis...";
    public static final String METRIC_ASSET_DETAILS = "Asset Details";
    public static final String METRIC_SOURCE_TARGET = "Source/Target";
    public static final String METRIC_CONNECTION_DETAILS = "Connection Details";
    public static final String STATUS_CONNECTED = "Connected";
    public static final String STATUS_MISSING_SOURCE = "Missing Source";
    public static final String SHOW_PUNCHLIST = "Show AF Punchlist";
    public static final String HIDE_PUNCHLIST = "Hide AF Punchlist";
    public static final String COLLECT_AF_DATA = "Collect AF Data";
    public static final String PHOTO_CATEGORY_STICKER = "Arc Flash Sticker";
    /** ViewModel bucket labels — closed set, asserted verbatim. */
    public static final String[] BUCKET_LABELS =
            { "0%", "1-25%", "26-50%", "51-75%", "76-99%", "100%" };

    // ── Locators ───────────────────────────────────────────────────────
    private static final By QUICK_ACTION_BUTTON = AppiumBy.iOSNsPredicateString(
            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeOther')"
                    + " AND name == '" + QUICK_ACTION_CARD + "' AND visible == 1");
    private static final By QUICK_ACTION_TEXT = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name == '" + QUICK_ACTION_CARD + "'");
    private static final By NAV_BAR = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeNavigationBar' AND name == '" + NAV_TITLE + "'");
    private static final By NAV_TITLE_TEXT = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name == '" + NAV_TITLE + "'");
    private static final By READINESS_TEXT = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name == '" + READINESS_SCORE + "'");
    private static final By LOADING_TEXT = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH 'Loading Arc Flash'");
    private static final By DONE_BUTTON = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (name == 'Done' OR label == 'Done')");
    private static final By STAT_COMPLETED = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name ENDSWITH ' Completed'");
    private static final By STAT_REMAINING = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name ENDSWITH ' Remaining'");
    private static final By STAT_TOTAL = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name ENDSWITH ' Total Items'");
    /** "X of Y" fraction labels — one per metric card, document order = card order. */
    private static final By CARD_FRACTIONS = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name MATCHES '^[0-9,]+ of [0-9,]+$'");
    /** Plain percent labels ("42%") anywhere on screen. */
    private static final By PERCENT_TEXTS = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name MATCHES '^[0-9]+%$'");
    private static final By BREAKDOWN_HEADER = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name ENDSWITH ' Breakdown'");

    public ArcFlashPage() {
        super();
    }

    // ═════════════════════════════════ Dashboard entry ═════════════════

    /**
     * From the logged-in Site/Dashboard tab, open the Arc Flash dashboard and
     * wait until metrics finish computing. Throws VerificationError on failure
     * (never returns a swallowed false — hardening-layer rule).
     */
    public void openDashboard() {
        if (isDashboardDisplayed(1)) return; // idempotent
        // The quick-action grid sits on the landing dashboard; the card can be
        // below the fold on small windows.
        if (!swipeUntilVisible(QUICK_ACTION_BUTTON, 3) && !swipeUntilVisible(QUICK_ACTION_TEXT, 2)) {
            throw new VerificationError("openDashboard: 'Arc Flash' quick-action card never became visible");
        }
        // Strategy 1: the card element itself; Strategy 2: its title text.
        for (By strat : new By[] { QUICK_ACTION_BUTTON, QUICK_ACTION_TEXT }) {
            try {
                WebElement el = lastVisible(strat);
                if (el != null && pressElement(el) && waitForDashboard(12)) return;
            } catch (Exception e) {
                System.out.println("⚠️ openDashboard strategy failed: " + e.getMessage());
            }
        }
        throw new VerificationError("openDashboard: tapping the Arc Flash card never opened '" + NAV_TITLE + "'");
    }

    /** Nav bar or title text or Readiness header — any proves we are on it. */
    public boolean isDashboardDisplayed(int timeoutSeconds) {
        return waitForCondition(() -> existsNow(NAV_BAR) || existsNow(NAV_TITLE_TEXT)
                || existsNow(READINESS_TEXT), timeoutSeconds);
    }

    /** Dashboard open AND the loading overlay resolved (metrics computed). */
    public boolean waitForDashboard(int timeoutSeconds) {
        if (!isDashboardDisplayed(timeoutSeconds)) return false;
        // Metrics compute async; big sites take seconds. Loading gone = ready.
        return isElementGone(LOADING_TEXT, Math.max(timeoutSeconds, 30));
    }

    public boolean isLoadingOverlayVisible() {
        return existsNow(LOADING_TEXT);
    }

    /** Tap Done (nav-bar leading button) and verify the dashboard closed. */
    public boolean tapDone() {
        try {
            WebElement done = lastVisible(DONE_BUTTON);
            if (done != null) pressElement(done);
        } catch (Exception e) {
            System.out.println("⚠️ tapDone: " + e.getMessage());
        }
        return isElementGone(NAV_TITLE_TEXT, 8) && isElementGone(READINESS_TEXT, 2);
    }

    // ═════════════════════════════════ Readiness card ══════════════════

    /** "{n} Completed" / "{n} Remaining" / "{n} Total Items" — parsed counts. */
    public int getCompletedCount() { return leadingInt(readName(STAT_COMPLETED)); }
    public int getRemainingCount() { return leadingInt(readName(STAT_REMAINING)); }
    public int getTotalItemsCount() { return leadingInt(readName(STAT_TOTAL)); }

    public boolean hasReadinessScoreCard() {
        return existsNow(READINESS_TEXT);
    }

    public boolean hasOverallCaption() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + OVERALL_CAPTION + "'"));
    }

    /**
     * The overall ring percent: the FIRST "N%" StaticText in document order —
     * the readiness card renders above the three metric cards.
     * Returns -1 when unreadable (feed into a hard assert).
     */
    public int getOverallPercent() {
        List<String> all = readAllNames(PERCENT_TEXTS);
        if (all.isEmpty()) return -1;
        return leadingInt(all.get(0));
    }

    /**
     * All "N%" labels in document order. Layout order (verified against
     * ArcFlashCompletionView body): ring% first, then one per metric card.
     */
    public List<Integer> getAllPercents() {
        List<Integer> out = new ArrayList<>();
        for (String s : readAllNames(PERCENT_TEXTS)) out.add(leadingInt(s));
        return out;
    }

    // ═════════════════════════════════ Metric cards ════════════════════

    public boolean hasMetricCard(String title) {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + title + "'"));
    }

    /**
     * "X of Y" fractions, document order = Asset Details, Source/Target,
     * Connection Details (fixed card order in the view body).
     * Each entry is int[]{completed, total}. Commas stripped (locale grouping).
     */
    public List<int[]> getCardFractions() {
        List<int[]> out = new ArrayList<>();
        Pattern p = Pattern.compile("^([0-9,]+) of ([0-9,]+)$");
        for (String s : readAllNames(CARD_FRACTIONS)) {
            Matcher m = p.matcher(s);
            if (m.matches()) {
                out.add(new int[] {
                        Integer.parseInt(m.group(1).replace(",", "")),
                        Integer.parseInt(m.group(2).replace(",", "")) });
            }
        }
        return out;
    }

    /** Tap a metric card by its title text (switches the breakdown section). */
    public boolean selectMetricCard(String title) {
        By t = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + title + "' AND visible == 1");
        try {
            WebElement el = lastVisible(t);
            if (el == null) return false;
            pressElement(el);
            return waitForCondition(() -> title.equals(currentBreakdownMetric()), 6);
        } catch (Exception e) {
            System.out.println("⚠️ selectMetricCard(" + title + "): " + e.getMessage());
            return false;
        }
    }

    // ═════════════════════════════════ Breakdown ═══════════════════════

    /** Full "<Metric> Breakdown" header text, or "" when absent. */
    public String getBreakdownHeader() {
        return readName(BREAKDOWN_HEADER);
    }

    /** The metric named by the breakdown header ("" when absent). */
    public String currentBreakdownMetric() {
        String h = getBreakdownHeader();
        return h.endsWith(" Breakdown") ? h.substring(0, h.length() - " Breakdown".length()) : "";
    }

    /**
     * Visible completion-bucket labels. The DisclosureGroup label composes
     * "<range> · <count> <unit>" pieces; depending on SwiftUI flattening the
     * range may be its own StaticText or a prefix — match both ways.
     */
    public List<String> getVisibleBucketLabels() {
        List<String> found = new ArrayList<>();
        for (String b : BUCKET_LABELS) {
            By exact = AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND (name == '" + b
                            + "' OR name BEGINSWITH '" + b + " ')");
            if (existsNow(exact)) found.add(b);
        }
        return found;
    }

    /** Source/Target breakdown group counts: int[]{connected, missing}; -1 unreadable. */
    public int[] getSourceTargetGroupCounts() {
        return new int[] {
                groupCount(STATUS_CONNECTED),
                groupCount(STATUS_MISSING_SOURCE) };
    }

    private int groupCount(String groupLabel) {
        // Group header renders "<label>" with a "(n)" or "n" sibling; robust
        // path: exact label first, then a combined "<label> (n)" variant.
        By combined = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH '" + groupLabel + "'");
        String s = readName(combined);
        if (s.isEmpty()) return -1;
        Matcher m = Pattern.compile("([0-9]+)").matcher(s.substring(groupLabel.length()));
        return m.find() ? Integer.parseInt(m.group(1)) : -2; // -2: label present, count elsewhere
    }

    public boolean hasSourceTargetGroups() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH '" + STATUS_CONNECTED + "'"))
                || existsNow(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH '" + STATUS_MISSING_SOURCE + "'"));
    }

    /**
     * Expand the first visible bucket (tap its disclosure row) and report
     * whether at least one child row with a trailing "N%" appeared.
     */
    public boolean expandFirstBucketAndCheckRows() {
        List<String> buckets = getVisibleBucketLabels();
        if (buckets.isEmpty()) return false;
        int before = readAllNames(PERCENT_TEXTS).size();
        for (String b : buckets) {
            By row = AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeOther'"
                            + " OR type == 'XCUIElementTypeStaticText')"
                            + " AND (name == '" + b + "' OR name BEGINSWITH '" + b + " ') AND visible == 1");
            try {
                WebElement el = lastVisible(row);
                if (el == null) continue;
                pressElement(el);
                pause(700); // disclosure animation
                if (readAllNames(PERCENT_TEXTS).size() > before) return true;
            } catch (Exception e) {
                System.out.println("⚠️ expand bucket '" + b + "': " + e.getMessage());
            }
        }
        return false;
    }

    // ═════════════════════════════════ Punchlist toggles ═══════════════

    private static final By ASSETS_TAB = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (name == 'Assets' OR label == 'Assets')");
    private static final By CONNECTIONS_TAB = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (name == 'Connections' OR label == 'Connections')");
    /** Tab ellipsis menu button — SwiftUI Menu(label: ellipsis.circle). */
    private static final By ELLIPSIS_MENU = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (name CONTAINS 'ellipsis' OR name == 'More'"
                    + " OR label CONTAINS 'ellipsis' OR label == 'More')");
    /** AF punchlist badge on asset rows (green/red bolt). */
    private static final By AF_BOLT_BADGE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeImage' AND name == 'bolt.circle.fill'");
    /** AF punchlist badges on connection rows (check / x). */
    private static final By AF_EDGE_BADGE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeImage' AND (name == 'checkmark.circle.fill'"
                    + " OR name == 'xmark.circle.fill')");

    /** Bottom tab-bar navigation with verification. */
    public boolean openTab(String tabName) {
        By tab = "Assets".equals(tabName) ? ASSETS_TAB : CONNECTIONS_TAB;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement el = lastVisible(tab);
                if (el != null) {
                    pressElement(el);
                    if (waitForCondition(() -> existsNow(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeNavigationBar' AND name == '" + tabName + "'"))
                            || existsNow(AppiumBy.iOSNsPredicateString(
                                    "type == 'XCUIElementTypeStaticText' AND name == '" + tabName + "'")), 6)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ openTab(" + tabName + ") attempt " + attempt + ": " + e.getMessage());
            }
            pause(800);
        }
        return false;
    }

    /** Open the tab's ellipsis menu; true when at least one menu row appeared. */
    public boolean openEllipsisMenu() {
        try {
            WebElement el = lastVisible(ELLIPSIS_MENU);
            if (el == null) return false;
            pressElement(el);
            return waitForCondition(() -> isPunchlistOptionVisible(), 5);
        } catch (Exception e) {
            System.out.println("⚠️ openEllipsisMenu: " + e.getMessage());
            return false;
        }
    }

    public boolean isPunchlistOptionVisible() {
        return existsNow(punchlistOption(SHOW_PUNCHLIST)) || existsNow(punchlistOption(HIDE_PUNCHLIST));
    }

    /** The visible punchlist menu row's label, or "". */
    public String getPunchlistOptionLabel() {
        if (existsNow(punchlistOption(SHOW_PUNCHLIST))) return SHOW_PUNCHLIST;
        if (existsNow(punchlistOption(HIDE_PUNCHLIST))) return HIDE_PUNCHLIST;
        return "";
    }

    /**
     * Tap the Show/Hide AF Punchlist menu row — W3C press on the LAST visible
     * match (SwiftUI menus append at tree end; click() no-ops on menu rows).
     */
    public boolean tapPunchlistOption(String label) {
        try {
            WebElement el = lastVisible(punchlistOption(label));
            if (el == null) return false;
            pressElement(el);
            pause(600); // menu dismiss animation
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ tapPunchlistOption(" + label + "): " + e.getMessage());
            return false;
        }
    }

    private By punchlistOption(String label) {
        return AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText'"
                        + " OR type == 'XCUIElementTypeOther')"
                        + " AND (name == '" + label + "' OR label == '" + label + "') AND visible == 1");
    }

    /** Count visible AF bolt badges on asset rows. */
    public int countAssetBoltBadges() {
        return countVisible(AF_BOLT_BADGE);
    }

    /** Count visible AF check/x badges on connection rows. */
    public int countEdgeBadges() {
        return countVisible(AF_EDGE_BADGE);
    }

    private int countVisible(By locator) {
        int n = 0;
        try {
            for (WebElement el : withImplicitWait(0, () -> driver.findElements(locator))) {
                try {
                    if ("true".equals(el.getAttribute("visible"))) n++;
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            System.out.println("⚠️ countVisible: " + e.getMessage());
        }
        return n;
    }

    // ═════════════════════ Session overlay + Collect AF Data ═══════════

    /** Overlay-filter menu rows are the raw enum labels (not localized). */
    public static final String OVERLAY_ARC_FLASH = "Arc Flash";
    private static final By COLLECT_AF_ROW = AppiumBy.iOSNsPredicateString(
            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText'"
                    + " OR type == 'XCUIElementTypeOther')"
                    + " AND (name == 'Collect AF Data' OR label == 'Collect AF Data') AND visible == 1");
    private static final By COLLECT_AF_NAVBAR = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeNavigationBar' AND name == 'Collect AF Data'");
    private static final By COLLECT_AF_TITLE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name == 'Collect AF Data'");

    /**
     * Open the session-room trailing filter menu and pick the "Arc Flash"
     * overlay. The filter button is icon-only; strategies: filter-ish system
     * names, then any trailing nav-bar button.
     */
    public boolean selectArcFlashOverlay() {
        By[] menuButtons = {
                AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (name CONTAINS 'decrease'"
                                + " OR name CONTAINS 'filter' OR name CONTAINS 'line.3')"),
                AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeNavigationBar'"), // fallback resolved below
        };
        for (int s = 0; s < menuButtons.length; s++) {
            try {
                WebElement btn;
                if (s == 0) {
                    btn = lastVisible(menuButtons[0]);
                } else {
                    // Trailing button inside the nav bar (rightmost child button).
                    WebElement nav = lastVisible(menuButtons[1]);
                    if (nav == null) continue;
                    List<WebElement> kids = nav.findElements(AppiumBy.className("XCUIElementTypeButton"));
                    btn = kids.isEmpty() ? null : kids.get(kids.size() - 1);
                }
                if (btn == null) continue;
                pressElement(btn);
                By afRow = AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText')"
                                + " AND (name == '" + OVERLAY_ARC_FLASH + "' OR label == '"
                                + OVERLAY_ARC_FLASH + "') AND visible == 1");
                if (waitForCondition(() -> existsNow(afRow), 4)) {
                    WebElement row = lastVisible(afRow);
                    if (row != null) {
                        pressElement(row);
                        pause(700);
                        return true;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ selectArcFlashOverlay strategy " + s + ": " + e.getMessage());
            }
        }
        return false;
    }

    /** Long-press (context menu) an element: W3C down → 900ms → up. */
    public boolean longPressElement(WebElement el) {
        try {
            org.openqa.selenium.Rectangle r = el.getRect();
            int cx = r.getX() + r.getWidth() / 2;
            int cy = r.getY() + r.getHeight() / 2;
            org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence press =
                    new org.openqa.selenium.interactions.Sequence(finger, 1);
            press.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), cx, cy));
            press.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            press.addAction(new org.openqa.selenium.interactions.Pause(finger,
                    java.time.Duration.ofMillis(900)));
            press.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(press));
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ longPressElement: " + e.getMessage());
            return false;
        }
    }

    public boolean isCollectAFDataRowVisible() {
        return existsNow(COLLECT_AF_ROW);
    }

    /**
     * First visible asset row in the session room list. SwiftUI rows may be
     * Cells or plain Buttons/Others; take the first substantial element below
     * the navigation zone (y > 150, height > 40) to dodge nav-bar buttons.
     */
    public WebElement firstRoomAssetRow() {
        By[] strategies = {
                AppiumBy.className("XCUIElementTypeCell"),
                AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1"),
                AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeOther' AND visible == 1 AND name != ''"),
        };
        for (By strat : strategies) {
            try {
                for (WebElement el : withImplicitWait(0, () -> driver.findElements(strat))) {
                    try {
                        if (!"true".equals(el.getAttribute("visible"))) continue;
                        org.openqa.selenium.Rectangle r = el.getRect();
                        if (r.getY() > 150 && r.getHeight() > 40) return el;
                    } catch (Exception ignored) { }
                }
            } catch (Exception e) {
                System.out.println("⚠️ firstRoomAssetRow strategy: " + e.getMessage());
            }
        }
        return null;
    }

    /** Tap the context-menu row and verify the Collect AF Data editor opened. */
    public boolean tapCollectAFData() {
        try {
            WebElement row = lastVisible(COLLECT_AF_ROW);
            if (row == null) return false;
            pressElement(row);
            return waitForCondition(() -> existsNow(COLLECT_AF_NAVBAR) || existsNow(COLLECT_AF_TITLE), 8);
        } catch (Exception e) {
            System.out.println("⚠️ tapCollectAFData: " + e.getMessage());
            return false;
        }
    }

    public boolean isCollectAFEditorOpen(int timeoutSeconds) {
        return waitForCondition(() -> existsNow(COLLECT_AF_NAVBAR) || existsNow(COLLECT_AF_TITLE),
                timeoutSeconds);
    }

    // ═════════════════════════════════ Diagnostics ═════════════════════

    /** Dump page source to target/afdump/<tag>.xml for the fix-it loop. */
    public void dumpSource(String tag) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("target", "afdump");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve(tag + ".xml"), driver.getPageSource());
            System.out.println("🗂  AF DOM dumped: target/afdump/" + tag + ".xml");
        } catch (Exception e) {
            System.out.println("⚠️ dumpSource failed: " + e.getMessage());
        }
    }

    // ═════════════════════════════════ Internals ═══════════════════════

    private String readName(By locator) {
        try {
            if (!existsNow(locator)) return "";
            WebElement el = lastVisible(locator);
            if (el == null) return "";
            String n = el.getAttribute("name");
            return n == null ? "" : n;
        } catch (Exception e) {
            return "";
        }
    }

    private List<String> readAllNames(By locator) {
        List<String> out = new ArrayList<>();
        try {
            for (WebElement el : withImplicitWait(0, () -> driver.findElements(locator))) {
                try {
                    if (!"true".equals(el.getAttribute("visible"))) continue;
                    String n = el.getAttribute("name");
                    if (n != null && !n.isEmpty()) out.add(n);
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            System.out.println("⚠️ readAllNames: " + e.getMessage());
        }
        return out;
    }

    /** LAST visible match in document order (menus/overlays append at tree end). */
    private WebElement lastVisible(By locator) {
        List<WebElement> els = withImplicitWait(0, () -> driver.findElements(locator));
        WebElement last = null;
        for (WebElement el : els) {
            try {
                if ("true".equals(el.getAttribute("visible"))) last = el;
            } catch (Exception ignored) { }
        }
        if (last == null && !els.isEmpty()) last = els.get(els.size() - 1);
        return last;
    }

    private int leadingInt(String s) {
        Matcher m = Pattern.compile("([0-9][0-9,]*)").matcher(s == null ? "" : s);
        return m.find() ? Integer.parseInt(m.group(1).replace(",", "")) : -1;
    }

    private boolean swipeUntilVisible(By locator, int maxSwipes) {
        for (int i = 0; i <= maxSwipes; i++) {
            if (existsNow(locator)) return true;
            if (i < maxSwipes) {
                scrollDown();
                pause(650);
            }
        }
        return false;
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    /**
     * W3C pointer press at the element center — the module tap primitive.
     * iOS 26.2: XCUIElement.click() silently no-ops on menu rows and,
     * intermittently, ordinary buttons (see AssetEngineerPage — same law).
     */
    private boolean pressElement(WebElement el) {
        try {
            org.openqa.selenium.Rectangle r = el.getRect();
            int cx = r.getX() + r.getWidth() / 2;
            int cy = r.getY() + r.getHeight() / 2;
            org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence press =
                    new org.openqa.selenium.interactions.Sequence(finger, 1);
            press.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), cx, cy));
            press.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            press.addAction(new org.openqa.selenium.interactions.Pause(finger,
                    java.time.Duration.ofMillis(120)));
            press.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.Arrays.asList(press));
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ pressElement: " + e.getMessage() + " — falling back to click()");
            try {
                el.click();
                return true;
            } catch (Exception e2) {
                System.out.println("⚠️ pressElement fallback click: " + e2.getMessage());
                return false;
            }
        }
    }
}
