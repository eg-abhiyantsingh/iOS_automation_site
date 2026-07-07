package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import com.egalvanic.verify.VerificationError;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page object for the v1.49 "asset_engineer" module (ZP-2161 SKM equipment
 * library + ZP-2794 encrypted transport):
 *
 *  1. Settings → Equipment Library card ("Load Latest Equipment Library"),
 *     the "Load Device Library?" alert, download progress + terminal states.
 *  2. Asset Details → Engineering section: not-downloaded banner, subtype /
 *     voltage pickers, class-gated blocks (box / OCP / transformer / cable /
 *     busway), segmented controls, numeric fields.
 *  3. SKM match panel ("{n} possible matches", match cards, search, Add Custom).
 *  4. Bound-library card (LIBRARY MATCHED / CUSTOM ENTRY, Unlink, Edit).
 *  5. Add/Edit Custom Equipment sheet.
 *
 * LOCATOR CONTRACT (from live v1.49 DOM dumps in target/engdump/ — see
 * docs/ai-features-changelog entry for this module):
 *  - The app sets ZERO custom accessibilityIdentifiers on these screens;
 *    every element's name == label == visible text. All locators are
 *    text-based and therefore locale-fragile (widen for French if needed).
 *  - Engineering pickers use placeholder "Select…" (U+2026 HORIZONTAL
 *    ELLIPSIS); the custom-attribute pickers BELOW them use ASCII "Select...".
 *    Never confuse the two.
 *  - Asset Details is ONE flat sibling list inside a single content Other —
 *    there are no card containers. Field rows are label-StaticText followed
 *    by the control as next siblings; geometry (y-offsets) is the only
 *    reliable label→control association.
 *  - NEVER use `mobile: scroll` with predicateString on asset-details DOMs:
 *    the unlocked engineering tree (100KB+ source) wedges WDA and kills the
 *    session (observed live; see giant-DOM memory). W3C pointer swipes
 *    (BasePage.scrollDown/scrollUp) + `visible`-attribute probes only.
 *  - Settings card Button folds its subtitle into its name:
 *    "Load Latest Equipment Library, <state>". The state suffix changes
 *    (Not yet downloaded / Inserting … / Last updated … / counts summary /
 *    Download failed: …) — always match BEGINSWITH on the title.
 *  - Success counts are locale-formatted ("1,28,303 frames" on en_IN) —
 *    never assert digit grouping, match " frames," fragment only.
 */
public class AssetEngineerPage extends BasePage {

    // ── Exact strings (v1.49 live DOM) ─────────────────────────────────
    /** U+2026 HORIZONTAL ELLIPSIS — engineering picker placeholder. */
    public static final String SELECT_ELLIPSIS = "Select…";
    public static final String SETTINGS_SECTION_HEADER = "Equipment Library";
    public static final String LIBRARY_CARD_TITLE = "Load Latest Equipment Library";
    public static final String STATE_NOT_DOWNLOADED = "Not yet downloaded";
    public static final String STATE_LAST_UPDATED_PREFIX = "Last updated";
    public static final String STATE_FAILED_PREFIX = "Download failed";
    public static final String LOAD_DIALOG_TITLE = "Load Device Library?";
    public static final String LOAD_DIALOG_MESSAGE_PREFIX = "Downloads the full device library";
    public static final String BANNER_NOT_DOWNLOADED = "Equipment library not downloaded";
    public static final String ENGINEERING_HEADER = "Engineering";
    public static final String BOUND_LIBRARY_MATCHED_UPPER = "LIBRARY MATCHED";
    public static final String BOUND_LIBRARY_MATCHED = "Library Matched";
    public static final String BOUND_CUSTOM_ENTRY_UPPER = "CUSTOM ENTRY";
    public static final String BOUND_CUSTOM_ENTRY = "Custom Entry";
    public static final String CUSTOM_SHEET_TITLE = "Add Custom Equipment";
    public static final String CUSTOM_SHEET_EDIT_TITLE = "Edit Custom Equipment";

    // ── Locators ───────────────────────────────────────────────────────
    private static final By SETTINGS_TAB = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (name == 'gear' OR label == 'Settings')");
    private static final By SETTINGS_NAVBAR = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeNavigationBar' AND name == 'Settings'");
    private static final By LIBRARY_SECTION_HEADER = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name == '" + SETTINGS_SECTION_HEADER + "'");
    private static final By LIBRARY_CARD = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name BEGINSWITH '" + LIBRARY_CARD_TITLE + "'");
    private static final By LOAD_DIALOG = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeAlert' AND name == '" + LOAD_DIALOG_TITLE + "'");
    private static final By ANY_ALERT = AppiumBy.className("XCUIElementTypeAlert");
    private static final By BANNER = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name == '" + BANNER_NOT_DOWNLOADED + "'");
    private static final By ENGINEERING_TITLE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND name == '" + ENGINEERING_HEADER + "'");
    private static final By ASSET_DETAILS_NAVBAR = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeNavigationBar' AND name == 'Asset Details'");
    private static final By MATCH_HEADER = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND (name CONTAINS 'possible match')");
    private static final By ADD_CUSTOM_BUTTON = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label CONTAINS 'Add Custom'");
    private static final By BOUND_CARD_TITLE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND (name == '" + BOUND_LIBRARY_MATCHED_UPPER
                    + "' OR name == '" + BOUND_LIBRARY_MATCHED + "')");
    private static final By CUSTOM_ENTRY_TITLE = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND (name == '" + BOUND_CUSTOM_ENTRY_UPPER
                    + "' OR name == '" + BOUND_CUSTOM_ENTRY + "')");
    private static final By UNLINK_BUTTON = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Unlink'");

    public AssetEngineerPage() {
        super();
    }

    // ═════════════════════════════════════════════════════════════════
    // Small utilities (visible-attr probes + W3C swipes — giant-DOM safe)
    // ═════════════════════════════════════════════════════════════════

    /** True when at least one match of the locator reports visible="true". */
    private boolean visibleNow(By locator) {
        return withImplicitWait(0, () -> {
            try {
                for (WebElement e : driver.findElements(locator)) {
                    try {
                        if ("true".equals(e.getAttribute("visible"))) return true;
                    } catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }
            return false;
        });
    }

    /** Swipe (content up) until locator is visible; max swipes bounded. */
    private boolean swipeUntilVisible(By locator, int maxSwipes) {
        for (int i = 0; i <= maxSwipes; i++) {
            if (visibleNow(locator)) return true;
            if (i < maxSwipes) {
                scrollDown();
                pause(650);
            }
        }
        return false;
    }

    /** Real bounded pause (BasePage.sleep is an intentional no-op). */
    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    /**
     * W3C pointer press at an element's center — THE tap primitive for this
     * module. On iOS 26.2, XCUIElement.click() silently no-ops not only on
     * menu rows but intermittently on ordinary buttons too (observed live:
     * Add Custom chip, Settings library card on its second tap). A real
     * touch-down → 120ms → touch-up always registers.
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

    private WebElement firstVisible(By locator) {
        return withImplicitWait(0, () -> {
            try {
                for (WebElement e : driver.findElements(locator)) {
                    try {
                        if ("true".equals(e.getAttribute("visible"))) return e;
                    } catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }
            return null;
        });
    }

    /**
     * Last visible match in document order. Menu/popover overlays are
     * appended at the END of the accessibility tree, so when the same label
     * exists both in the page background AND as an open-menu row (e.g. a
     * custom-attribute chip already set to "MCB" behind a Mains Type menu),
     * the LAST visible match is the menu row. Tapping the first would land
     * on the background element and dismiss the menu with no selection
     * (diagnosed live on Node Bus "Ns").
     */
    private WebElement lastVisible(By locator) {
        return withImplicitWait(0, () -> {
            WebElement last = null;
            try {
                for (WebElement e : driver.findElements(locator)) {
                    try {
                        if ("true".equals(e.getAttribute("visible"))) last = e;
                    } catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }
            return last;
        });
    }

    // ═════════════════════════════════════════════════════════════════
    // 1. Settings → Equipment Library card
    // ═════════════════════════════════════════════════════════════════

    /**
     * Open the Settings tab. Multi-strategy; throws VerificationError when
     * every strategy fails (unswallowable — do not catch-and-continue).
     */
    public void openSettings() {
        // Already there?
        if (existsNow(SETTINGS_NAVBAR) || existsNow(LIBRARY_SECTION_HEADER)) return;

        // Strategy 1: tab-bar button by icon name 'gear'
        By gearTab = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == 'gear'");
        if (existsNow(gearTab)) {
            try { driver.findElement(gearTab).click(); } catch (Exception e) {
                System.out.println("⚠️ Settings tab by 'gear' failed: " + e.getMessage());
            }
        }
        if (verifyOnSettings(5)) return;

        // Strategy 2: tab-bar button by label 'Settings'
        By byLabel = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Settings'");
        if (existsNow(byLabel)) {
            try { driver.findElement(byLabel).click(); } catch (Exception e) {
                System.out.println("⚠️ Settings tab by label failed: " + e.getMessage());
            }
        }
        if (verifyOnSettings(5)) return;

        // Strategy 3: combined predicate (icon OR label)
        if (existsNow(SETTINGS_TAB)) {
            try { driver.findElement(SETTINGS_TAB).click(); } catch (Exception e) {
                System.out.println("⚠️ Settings tab combined predicate failed: " + e.getMessage());
            }
        }
        if (verifyOnSettings(6)) return;

        throw new VerificationError("openSettings: exhausted all strategies — Settings screen never rendered");
    }

    /** Bounded render-verification for the Settings screen. */
    public boolean verifyOnSettings(int timeoutSeconds) {
        return waitForCondition(() -> existsNow(SETTINGS_NAVBAR) || existsNow(LIBRARY_SECTION_HEADER)
                || existsNow(LIBRARY_CARD), timeoutSeconds);
    }

    /**
     * Bring the Equipment Library card into the viewport.
     * The card sits below the fold (y≈944 on a 874px window) and reports
     * visible="false" until scrolled even though it is findable in the DOM.
     */
    public boolean scrollToLibraryCard() {
        return swipeUntilVisible(LIBRARY_CARD, 4);
    }

    /**
     * The card's state subtitle. The card Button's name is
     * "Load Latest Equipment Library, <subtitle>" — parse the suffix.
     * Fallback: read the second StaticText child via known state prefixes.
     * Returns "" when the card cannot be found (feed into a hard assert).
     */
    public String getLibraryCardSubtitle() {
        // Strategy 1: parse the folded Button name
        try {
            if (existsNow(LIBRARY_CARD)) {
                String name = driver.findElement(LIBRARY_CARD).getAttribute("name");
                if (name != null && name.length() > LIBRARY_CARD_TITLE.length() + 2) {
                    return name.substring(LIBRARY_CARD_TITLE.length() + 2);
                }
                if (name != null && name.equals(LIBRARY_CARD_TITLE)) return "";
            }
        } catch (Exception e) {
            System.out.println("⚠️ subtitle via card name failed: " + e.getMessage());
        }
        // Strategy 2: known-state StaticTexts anywhere on the Settings screen
        By anyState = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (name == '" + STATE_NOT_DOWNLOADED + "'"
                        + " OR name BEGINSWITH '" + STATE_LAST_UPDATED_PREFIX + "'"
                        + " OR name BEGINSWITH '" + STATE_FAILED_PREFIX + "'"
                        + " OR name CONTAINS ' frames,'"
                        + " OR name BEGINSWITH 'Inserting'"
                        + " OR name BEGINSWITH 'Downloading'"
                        + " OR name == 'Starting…' OR name BEGINSWITH 'Clearing'"
                        + " OR name BEGINSWITH 'Saving' OR name == 'Complete')");
        try {
            if (existsNow(anyState)) {
                return driver.findElement(anyState).getAttribute("name");
            }
        } catch (Exception e) {
            System.out.println("⚠️ subtitle via state StaticText failed: " + e.getMessage());
        }
        return "";
    }

    /** True when the library reports a downloaded terminal state. */
    public boolean isLibraryDownloadedPerSettings() {
        String sub = getLibraryCardSubtitle();
        return sub.startsWith(STATE_LAST_UPDATED_PREFIX) || sub.contains(" frames,");
    }

    /** Tap the Equipment Library card (must be scrolled into view first). */
    public void tapLibraryCard() {
        if (!scrollToLibraryCard()) {
            throw new VerificationError("tapLibraryCard: Equipment Library card never became visible");
        }
        // Strategy 1: the card Button itself
        try {
            if (pressElement(driver.findElement(LIBRARY_CARD))) return;
        } catch (Exception e) {
            System.out.println("⚠️ card tap strategy 1 failed: " + e.getMessage());
        }
        // Strategy 2: the title StaticText child
        By title = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + LIBRARY_CARD_TITLE + "'");
        try {
            if (pressElement(driver.findElement(title))) return;
        } catch (Exception e) {
            System.out.println("⚠️ card tap strategy 2 failed: " + e.getMessage());
        }
        throw new VerificationError("tapLibraryCard: could not tap the Equipment Library card");
    }

    /**
     * Wait for the "Load Device Library?" alert. The alert fade-in animation
     * is slow (observed >1.2s); a tap dispatched mid-animation can report
     * failure yet land — always verify with this, never trust click returns.
     */
    public boolean isLoadDialogShown(int timeoutSeconds) {
        return isElementDisplayed(LOAD_DIALOG, timeoutSeconds);
    }

    /**
     * Exact alert message text; "" when unavailable. The alert element lands
     * in the DOM a beat before its message StaticText finishes rendering
     * (observed live), so poll briefly instead of a single 0-wait probe.
     */
    public String getLoadDialogMessage() {
        By msg = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH '" + LOAD_DIALOG_MESSAGE_PREFIX + "'");
        waitForCondition(() -> existsNow(msg), 4);
        try {
            if (existsNow(msg)) return driver.findElement(msg).getAttribute("name");
        } catch (Exception e) {
            System.out.println("⚠️ getLoadDialogMessage: " + e.getMessage());
        }
        return "";
    }

    /** True when the alert exposes both Cancel and Download actions. */
    public boolean loadDialogHasButtons() {
        By dl = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Download'");
        By ca = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Cancel'");
        return existsNow(dl) && existsNow(ca);
    }

    /**
     * Tap an alert button ("Download" / "Cancel") with a mobile:alert
     * fallback, then verify the alert dismissed.
     */
    public boolean tapLoadDialogButton(String buttonLabel) {
        By btn = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + buttonLabel + "'");
        try {
            pressElement(driver.findElement(btn));
        } catch (Exception e) {
            System.out.println("⚠️ alert '" + buttonLabel + "' direct tap failed: " + e.getMessage()
                    + " — trying mobile:alert");
            try {
                java.util.HashMap<String, Object> args = new java.util.HashMap<>();
                args.put("action", "Cancel".equals(buttonLabel) ? "dismiss" : "accept");
                args.put("buttonLabel", buttonLabel);
                driver.executeScript("mobile: alert", args);
            } catch (Exception e2) {
                System.out.println("⚠️ mobile:alert fallback failed: " + e2.getMessage());
            }
        }
        // Outcome check — the only trustworthy signal (animation race).
        return isElementGone(ANY_ALERT, 6);
    }

    /**
     * Poll the card subtitle until a terminal download state.
     * Returns one of: "SUCCESS_COUNTS", "LAST_UPDATED", "FAILED", "TIMEOUT".
     * Progress transitions are logged (Starting… → Downloading device
     * library… → Inserting N frames/sensors/trip units/settings/kVA
     * entries/cable-busway entries… → Saving to device… → Complete).
     */
    public String waitForDownloadTerminal(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        String lastProgress = "";
        while (System.currentTimeMillis() < deadline) {
            String sub = getLibraryCardSubtitle();
            if (sub.contains(" frames,")) return "SUCCESS_COUNTS";
            if (sub.startsWith(STATE_LAST_UPDATED_PREFIX)) return "LAST_UPDATED";
            if (sub.startsWith(STATE_FAILED_PREFIX)) {
                System.out.println("❌ library download failed: " + sub);
                return "FAILED";
            }
            if (!sub.isEmpty() && !sub.equals(lastProgress)) {
                System.out.println("⏳ library download progress: " + sub);
                lastProgress = sub;
            }
            pause(2000);
        }
        return "TIMEOUT";
    }

    /**
     * Composite state gate: make sure the SKM equipment library is cached on
     * the device. Fast no-op when Settings already reports a downloaded
     * state; otherwise runs card → alert → Download → poll.
     * Throws VerificationError on FAILED / TIMEOUT / unreachable UI so a
     * broken download can never be laundered into a SKIP.
     *
     * @return true when a download was performed, false when already cached.
     */
    public boolean ensureLibraryDownloaded(int downloadTimeoutSeconds) {
        openSettings();
        if (!scrollToLibraryCard()) {
            throw new VerificationError("ensureLibraryDownloaded: Equipment Library card not found in Settings");
        }
        String sub = getLibraryCardSubtitle();
        if (sub.startsWith(STATE_LAST_UPDATED_PREFIX) || sub.contains(" frames,")) {
            System.out.println("✅ equipment library already cached (" + sub + ")");
            return false;
        }
        if (sub.startsWith("Inserting") || sub.startsWith("Downloading") || sub.startsWith("Starting")
                || sub.startsWith("Clearing") || sub.startsWith("Saving")) {
            System.out.println("⏳ download already in flight (" + sub + ") — waiting");
        } else {
            tapLibraryCard();
            if (!isLoadDialogShown(8)) {
                throw new VerificationError("ensureLibraryDownloaded: 'Load Device Library?' alert never appeared");
            }
            tapLoadDialogButton("Download");
        }
        String terminal = waitForDownloadTerminal(downloadTimeoutSeconds);
        if (!terminal.equals("SUCCESS_COUNTS") && !terminal.equals("LAST_UPDATED")) {
            throw new VerificationError("ensureLibraryDownloaded: terminal state = " + terminal
                    + " (subtitle: " + getLibraryCardSubtitle() + ")");
        }
        System.out.println("✅ equipment library ready (" + terminal + ")");
        return true;
    }

    // ═════════════════════════════════════════════════════════════════
    // 2. Asset Details → Engineering section
    // ═════════════════════════════════════════════════════════════════

    /**
     * Open an asset from the Assets list by cell-name prefix (cell names are
     * "Name, Location, Class" e.g. "Transformer-1, No Location, Transformer").
     * Throws VerificationError when the details screen never renders.
     */
    public void openAssetCardByPrefix(String namePrefix) {
        By cell = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH '" + namePrefix + "'");
        if (!swipeUntilVisible(cell, 4)) {
            throw new VerificationError("openAssetCardByPrefix: no asset cell starting with '"
                    + namePrefix + "' on the Assets list");
        }
        try {
            pressElement(driver.findElement(cell));
        } catch (Exception e) {
            System.out.println("⚠️ asset cell tap failed, retrying once: " + e.getMessage());
            try { pressElement(driver.findElement(cell)); } catch (Exception e2) {
                throw new VerificationError("openAssetCardByPrefix: cell tap failed twice for '"
                        + namePrefix + "': " + e2.getMessage());
            }
        }
        if (!waitForCondition(() -> existsNow(ASSET_DETAILS_NAVBAR), 10)) {
            throw new VerificationError("openAssetCardByPrefix: Asset Details never rendered for '"
                    + namePrefix + "'");
        }
    }

    /** Swipe until the "Engineering" section title is on screen. */
    public boolean swipeToEngineeringSection() {
        return swipeUntilVisible(ENGINEERING_TITLE, 6);
    }

    public boolean isEngineeringSectionPresent() {
        return existsNow(ENGINEERING_TITLE);
    }

    /** The orange not-downloaded banner inside the Engineering card. */
    public boolean isLibraryMissingBannerShown() {
        return existsNow(BANNER);
    }

    /** Exact-match engineering field label presence (StaticText). */
    public boolean isEngineeringLabelPresent(String label) {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + label + "'"));
    }

    /**
     * Swipe a field label into the viewport, INCLUDING its control below.
     * Chip READERS are visible-only, and a label that lands at the bottom
     * edge leaves its chip off-screen (diagnosed live on the ATS draft) —
     * so when the label sits in the bottom 40% of the window, nudge one
     * more swipe to bring the whole row in.
     */
    public boolean scrollToEngineeringLabel(String label) {
        By labelBy = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + label + "'");
        if (!swipeUntilVisible(labelBy, 6)) return false;
        try {
            WebElement lbl = firstVisible(labelBy);
            if (lbl != null) {
                int winH = driver.manage().window().getSize().getHeight();
                if (lbl.getLocation().getY() > winH * 0.6) {
                    scrollDown();
                    pause(650);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ scrollToEngineeringLabel nudge: " + e.getMessage());
        }
        return visibleNow(labelBy) || swipeUntilVisible(labelBy, 2);
    }

    /**
     * The locked System Voltage row: label + em-dash value + lock icon,
     * with no editable control (v1.49 renders propagated voltage read-only
     * on downstream assets — observed on Node Bus and Fuse).
     */
    public boolean isSystemVoltageLockedRowShown() {
        boolean label = isEngineeringLabelPresent("System Voltage");
        boolean lockIcon = existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeImage' AND name == 'lock.fill'"));
        boolean emDash = existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '—'"));
        return label && lockIcon && emDash;
    }

    /** Cu / Al conductor-material segments (unbound cable/busway block). */
    public boolean areConductorMaterialSegmentsShown() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cu'"))
                && existsNow(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == 'Al'"));
    }

    /** Unbound subtype picker chip ("Select asset subtype"). */
    public boolean isSubtypePickerShown() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Select asset subtype'"));
    }

    /** The match panel's search field (exact live placeholder with quotes). */
    public boolean isMatchSearchFieldShown() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND placeholderValue == 'e.g. \"QD\" or \"Formula\"'"));
    }

    /** Exact empty-state hint under the match panel ("" when absent). */
    public String getMatchEmptyStateText() {
        By empty = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND (name BEGINSWITH 'No matches')");
        try {
            if (existsNow(empty)) return driver.findElement(empty).getAttribute("name");
        } catch (Exception e) {
            System.out.println("⚠️ getMatchEmptyStateText: " + e.getMessage());
        }
        return "";
    }

    /** Presence probe for a segmented-control option (does not tap). */
    public boolean segmentExists(String segmentLabel) {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + segmentLabel + "'"));
    }

    /** Wait for a picker/menu option row to render (menus animate in). */
    public boolean waitForOptionShown(String optionLabel, int timeoutSeconds) {
        return waitForCondition(() -> isOptionShown(optionLabel), timeoutSeconds);
    }

    /**
     * Open a SwiftUI Form Picker row (custom-sheet Function / I²t / Dial /
     * Pri-Sec Connection). Form pickers expose their row as an element whose
     * label is either the bare title or "title, currentValue" — match both,
     * prefer the topmost (last) visible, W3C-press it (menu rows and picker
     * rows share the iOS 26 tap quirk).
     */
    public boolean openFormPicker(String pickerLabel) {
        By row = AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeOther'"
                        + " OR type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeStaticText')"
                        + " AND (label == '" + pickerLabel + "' OR label BEGINSWITH '" + pickerLabel + ", ')");
        if (!swipeUntilVisible(row, 4)) {
            System.out.println("⚠️ openFormPicker('" + pickerLabel + "'): row never visible");
            return false;
        }
        WebElement el = lastVisible(row);
        if (el == null) return false;
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
            pause(500);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ openFormPicker('" + pickerLabel + "') press: " + e.getMessage());
            return false;
        }
    }

    /** Wait until a StaticText label disappears (sheet/overlay dismissal). */
    public boolean waitForLabelGone(String label, int timeoutSeconds) {
        return isElementGone(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + label + "'"), timeoutSeconds);
    }

    /**
     * Swipe within a fullscreen option list (e.g. the asset-class picker)
     * until the option row is visible. Menus never need this; long pushed
     * lists do.
     */
    public boolean scrollToOption(String optionLabel, int maxSwipes) {
        return swipeUntilVisible(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + optionLabel + "'"), maxSwipes);
    }

    /**
     * Pick the first VISIBLE option row whose label contains the fragment
     * (case-sensitive). Overlay sheets hide the background, so the
     * visibility filter naturally excludes same-text background elements.
     * Returns the picked label, or null when nothing matched.
     */
    public String pickFirstVisibleOptionContaining(String fragment) {
        By rows = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS '" + fragment + "'");
        waitForCondition(() -> firstVisible(rows) != null, 5);
        WebElement el = firstVisible(rows);
        if (el == null) {
            System.out.println("⚠️ pickFirstVisibleOptionContaining('" + fragment + "'): no visible match");
            return null;
        }
        try {
            String label = el.getAttribute("label");
            org.openqa.selenium.Rectangle r = el.getRect();
            System.out.println("🎯 picking option containing '" + fragment + "': '" + label + "'");
            java.util.HashMap<String, Object> args = new java.util.HashMap<>();
            args.put("x", r.getX() + r.getWidth() / 2);
            args.put("y", r.getY() + r.getHeight() / 2);
            driver.executeScript("mobile: tap", args);
            return label;
        } catch (Exception e) {
            System.out.println("⚠️ pickFirstVisibleOptionContaining tap: " + e.getMessage());
            return null;
        }
    }

    /**
     * Tap a button INSIDE a native alert. Alerts live in their own window:
     * W3C coordinate presses can miss them (observed live on 'Change Class'),
     * while XCUIElement.click() targets alert buttons reliably — the exact
     * inverse of the menu-row quirk. Verifies by alert dismissal.
     */
    public boolean tapAlertButton(String buttonLabel) {
        By btn = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + buttonLabel + "'");
        waitForCondition(() -> existsNow(ANY_ALERT) && existsNow(btn), 5);
        try {
            if (existsNow(btn)) driver.findElement(btn).click();
        } catch (Exception e) {
            System.out.println("⚠️ tapAlertButton('" + buttonLabel + "') click: " + e.getMessage()
                    + " — trying mobile:alert");
            try {
                java.util.HashMap<String, Object> args = new java.util.HashMap<>();
                args.put("action", "accept");
                args.put("buttonLabel", buttonLabel);
                driver.executeScript("mobile: alert", args);
            } catch (Exception e2) {
                System.out.println("⚠️ mobile:alert fallback: " + e2.getMessage());
            }
        }
        return isElementGone(ANY_ALERT, 6);
    }

    /** Presence probe for a picker/menu/list option row of any row type. */
    public boolean isOptionShown(String optionLabel) {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeCell'"
                        + " OR type == 'XCUIElementTypeStaticText' OR type == 'XCUIElementTypeOther')"
                        + " AND label == '" + optionLabel + "'"));
    }

    /** Presence probe for an engineering TextField by exact placeholder. */
    public boolean isEngineeringFieldPresent(String placeholder) {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND placeholderValue == '" + placeholder + "'"));
    }

    public boolean isUnlinkButtonShown() {
        return existsNow(UNLINK_BUTTON);
    }

    /** Dismiss an open Menu-style picker by tapping a neutral point. */
    public void dismissMenuOverlay() {
        try {
            java.util.HashMap<String, Object> args = new java.util.HashMap<>();
            args.put("x", 200);
            args.put("y", 90);
            driver.executeScript("mobile: tap", args);
            pause(500);
        } catch (Exception e) {
            System.out.println("⚠️ dismissMenuOverlay: " + e.getMessage());
        }
    }

    /**
     * Number of engineering "Select…" (U+2026) picker buttons currently in
     * the DOM. Custom-attribute pickers use ASCII "Select..." and are
     * excluded by the exact match.
     */
    public int getEngineeringSelectButtonCount() {
        return withImplicitWait(0, () ->
                driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == '" + SELECT_ELLIPSIS + "'")).size());
    }

    /**
     * Open the engineering picker that sits directly below the given field
     * label (flat-sibling geometry: the picker Button is the nearest Button
     * below the label within ~130px). Handles both unset ("Select…") and
     * already-set (label == current value) picker chips.
     * Throws VerificationError when the picker cannot be opened.
     */
    public void openEngineeringPickerBelowLabel(String fieldLabel) {
        By labelBy = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + fieldLabel + "'");
        if (!swipeUntilVisible(labelBy, 6)) {
            throw new VerificationError("openEngineeringPickerBelowLabel: label '" + fieldLabel
                    + "' never became visible");
        }
        WebElement target = withImplicitWait(0, () -> {
            try {
                WebElement lbl = firstVisible(labelBy);
                if (lbl == null) lbl = driver.findElement(labelBy);
                int labelY = lbl.getLocation().getY();
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton'"));
                // Two passes: prefer an unset engineering chip ("Select…",
                // U+2026 — custom-attribute chips are ASCII "Select..." and
                // must never match), then fall back to the nearest visible
                // Button in the window (value-set chips show their value).
                WebElement bestEllipsis = null, bestAny = null;
                int bestEllipsisDy = Integer.MAX_VALUE, bestAnyDy = Integer.MAX_VALUE;
                for (WebElement b : buttons) {
                    try {
                        if (!"true".equals(b.getAttribute("visible"))) continue;
                        int dy = b.getLocation().getY() - labelY;
                        if (dy < 0 || dy > 130) continue;
                        String bl = b.getAttribute("label");
                        if (SELECT_ELLIPSIS.equals(bl) && dy < bestEllipsisDy) {
                            bestEllipsisDy = dy;
                            bestEllipsis = b;
                        }
                        if (dy < bestAnyDy) {
                            bestAnyDy = dy;
                            bestAny = b;
                        }
                    } catch (Exception ignored) { }
                }
                return bestEllipsis != null ? bestEllipsis : bestAny;
            } catch (Exception e) {
                System.out.println("⚠️ picker geometry query failed: " + e.getMessage());
                return null;
            }
        });
        if (target == null) {
            throw new VerificationError("openEngineeringPickerBelowLabel: no tappable picker button below '"
                    + fieldLabel + "'");
        }
        try {
            org.openqa.selenium.Rectangle r = target.getRect();
            System.out.println("🎯 picker below '" + fieldLabel + "': pressing Button label='"
                    + target.getAttribute("label") + "' at x=" + r.getX() + " y=" + r.getY()
                    + " w=" + r.getWidth() + " h=" + r.getHeight());
            pressElement(target);
        } catch (Exception e) {
            throw new VerificationError("openEngineeringPickerBelowLabel: tap failed for picker below '"
                    + fieldLabel + "': " + e.getMessage());
        }
        pause(500);
    }

    /** Diagnostic: log every visible Button (label + frame) on screen. */
    public void debugLogVisibleButtons(String tag) {
        withImplicitWait(0, () -> {
            try {
                StringBuilder sb = new StringBuilder("🔍 visible Buttons " + tag + ": ");
                for (WebElement e : driver.findElements(AppiumBy.className("XCUIElementTypeButton"))) {
                    try {
                        if ("true".equals(e.getAttribute("visible"))) {
                            org.openqa.selenium.Rectangle r = e.getRect();
                            sb.append("['").append(e.getAttribute("label")).append("' y=")
                              .append(r.getY()).append("] ");
                        }
                    } catch (Exception ignored) { }
                }
                System.out.println(sb);
            } catch (Exception e) {
                System.out.println("⚠️ debugLogVisibleButtons: " + e.getMessage());
            }
            return null;
        });
    }

    /**
     * Current displayed value of the picker chip below a label ("" if none).
     * VISIBLE elements only — invisible background content (e.g. the asset
     * list behind a fullscreen Add Asset sheet) must never be read as the
     * chip value (observed live: reader returned an asset cell's name).
     */
    public String getPickerValueBelowLabel(String fieldLabel) {
        By labelBy = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + fieldLabel + "'");
        return withImplicitWait(0, () -> {
            try {
                WebElement lbl = firstVisible(labelBy);
                if (lbl == null) return "";
                int labelY = lbl.getLocation().getY();
                List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton'"));
                WebElement best = null;
                int bestDy = Integer.MAX_VALUE;
                for (WebElement b : buttons) {
                    try {
                        if (!"true".equals(b.getAttribute("visible"))) continue;
                        int dy = b.getLocation().getY() - labelY;
                        if (dy >= 0 && dy <= 130 && dy < bestDy) {
                            bestDy = dy;
                            best = b;
                        }
                    } catch (Exception ignored) { }
                }
                if (best != null) {
                    String v = best.getAttribute("label");
                    return v == null ? "" : v;
                }
            } catch (Exception e) {
                System.out.println("⚠️ getPickerValueBelowLabel: " + e.getMessage());
            }
            return "";
        });
    }

    /**
     * Poll the picker chip below a label until it shows the expected value
     * (SwiftUI re-renders the chip a beat after the menu closes). The reader
     * is visible-only, so bring the row into the viewport first — after
     * menu interactions the form can sit scrolled elsewhere.
     */
    public boolean waitForPickerValueBelowLabel(String fieldLabel, String expected, int timeoutSeconds) {
        scrollToEngineeringLabel(fieldLabel);
        return waitForCondition(() -> expected.equals(getPickerValueBelowLabel(fieldLabel)), timeoutSeconds);
    }

    /**
     * Tap a segmented-control option by its exact label
     * (e.g. "Cu"/"Al", "1P"/"2P"/"3P", "Dry Type"/"Oil-Filled", "On"/"Off").
     */
    public boolean tapSegment(String segmentLabel) {
        By seg = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + segmentLabel + "'");
        if (!swipeUntilVisible(seg, 4)) {
            System.out.println("⚠️ segment '" + segmentLabel + "' never visible");
            return false;
        }
        try {
            return pressElement(driver.findElement(seg));
        } catch (Exception e) {
            System.out.println("⚠️ segment tap '" + segmentLabel + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Type into an engineering numeric/text field identified by its exact
     * placeholder (ModernTextField shows the placeholder as `value` when
     * empty, and always as `placeholderValue`). Dismisses the keyboard after
     * typing (mandatory before tapping any button — keyboard covers it).
     */
    public void typeIntoEngineeringField(String placeholder, String text) {
        By field = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND placeholderValue == '" + placeholder + "'");
        By fieldFallback = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND value == '" + placeholder + "'");
        By target = swipeUntilVisible(field, 5) ? field
                : (swipeUntilVisible(fieldFallback, 1) ? fieldFallback : null);
        if (target == null) {
            throw new VerificationError("typeIntoEngineeringField: no TextField with placeholder '"
                    + placeholder + "'");
        }
        try {
            WebElement el = driver.findElement(target);
            el.click();
            el.sendKeys(text);
        } catch (Exception e) {
            throw new VerificationError("typeIntoEngineeringField('" + placeholder + "'): " + e.getMessage());
        }
        dismissKeyboard();
    }

    /**
     * Clear an engineering field then type — for input-filter matrices where
     * consecutive cases hit the same field (plain sendKeys would append).
     */
    public void clearAndTypeIntoEngineeringField(String placeholder, String text) {
        By field = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND placeholderValue == '" + placeholder + "'");
        if (!swipeUntilVisible(field, 5)) {
            throw new VerificationError("clearAndTypeIntoEngineeringField: no TextField with placeholder '"
                    + placeholder + "'");
        }
        try {
            WebElement el = firstVisible(field);
            if (el == null) el = driver.findElement(field);
            el.click();
            try {
                el.clear();
            } catch (Exception clearErr) {
                System.out.println("⚠️ clear() failed (" + clearErr.getMessage() + ") — select-all delete fallback");
                el.sendKeys(""); // backspaces
            }
            if (!text.isEmpty()) el.sendKeys(text);
            // NOTE: do NOT tap around the field to force end-editing — a
            // mid-composition tap disrupts the typing pipeline and injects
            // digit artifacts (observed live: '1e5' read back as '152').
            // The display therefore shows the RAW typed text for rejected
            // characters (SwiftUI only redraws on a state change) while the
            // draft holds the filtered value — expectations encode that.
        } catch (Exception e) {
            throw new VerificationError("clearAndTypeIntoEngineeringField('" + placeholder + "'): " + e.getMessage());
        }
        dismissKeyboard();
        pause(400);
    }

    /**
     * Read the current value of a field by placeholder ("" when empty/absent).
     * VISIBLE-first: a background field behind an open sheet can share the
     * placeholder (e.g. the Add-Asset 'Enter name' behind the Create-Main
     * sheet) and must never be the one read.
     */
    public String getEngineeringFieldValue(String placeholder) {
        By field = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND placeholderValue == '" + placeholder + "'");
        try {
            WebElement visible = firstVisible(field);
            if (visible != null) {
                String v = visible.getAttribute("value");
                if (v == null || v.equals(placeholder)) return "";
                return v;
            }
            if (existsNow(field)) {
                String v = driver.findElement(field).getAttribute("value");
                if (v == null || v.equals(placeholder)) return "";
                return v;
            }
        } catch (Exception e) {
            System.out.println("⚠️ getEngineeringFieldValue: " + e.getMessage());
        }
        return "";
    }

    /** Dismiss the keyboard via toolbar Done / Return / tap-out fallbacks. */
    public void dismissKeyboard() {
        By keyboard = AppiumBy.className("XCUIElementTypeKeyboard");
        if (!existsNow(keyboard)) return;
        // Strategy 1: toolbar Done above the keyboard
        By done = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name == 'Done' OR label == 'Done')");
        try {
            if (existsNow(done)) {
                driver.findElement(done).click();
                if (isElementGone(keyboard, 3)) return;
            }
        } catch (Exception ignored) { }
        // Strategy 2: keyboard Return/return key
        By ret = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name == 'Return' OR name == 'return' OR name == 'Done')");
        try {
            if (existsNow(ret)) {
                driver.findElement(ret).click();
                if (isElementGone(keyboard, 3)) return;
            }
        } catch (Exception ignored) { }
        // Strategy 3: native hideKeyboard
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) { }
        // Strategy 4: tap a neutral point (nav bar area)
        if (existsNow(keyboard)) {
            try {
                java.util.HashMap<String, Object> args = new java.util.HashMap<>();
                args.put("x", 200);
                args.put("y", 100);
                driver.executeScript("mobile: tap", args);
            } catch (Exception ignored) { }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 3. Sheet pickers (manufacturer, subtype, frame/sensor/plug >8 opts)
    // ═════════════════════════════════════════════════════════════════

    /** A searchable option sheet is open (search field or Done toolbar). */
    public boolean isSheetPickerOpen(int timeoutSeconds) {
        By searchField = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField' OR (type == 'XCUIElementTypeTextField' AND value BEGINSWITH 'Search')");
        By doneBtn = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Done'");
        return waitForCondition(() -> existsNow(searchField) || existsNow(doneBtn), timeoutSeconds);
    }

    /**
     * Type into the sheet's search field to filter options.
     * MUST target only a VISIBLE search field: a generic
     * "value BEGINSWITH 'Search'" predicate also matches the Assets list's
     * search box hiding BEHIND the overlay (observed live — typing into it
     * filters the background list and destabilizes WDA).
     */
    public void searchInSheetPicker(String text) {
        By searchField = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField' OR (type == 'XCUIElementTypeTextField' AND value BEGINSWITH 'Search')");
        WebElement f = firstVisible(searchField);
        if (f == null) {
            throw new VerificationError("searchInSheetPicker: no VISIBLE search field in the open picker");
        }
        try {
            f.click();
            f.sendKeys(text);
        } catch (Exception e) {
            throw new VerificationError("searchInSheetPicker: search field unusable: " + e.getMessage());
        }
    }

    /**
     * Pick an option row by exact label in the open sheet/menu, verifying the
     * overlay closes. Works for both Menu-mode pickers (option Buttons) and
     * sheet-mode (rows + optional Done).
     */
    public boolean pickOptionExact(String optionLabel) {
        // Menu rows are BUTTONS — always prefer them. A background element
        // (e.g. a StaticText with the same text elsewhere on the page, still
        // visible under the dimmed overlay) tapped by mistake lands OUTSIDE
        // the menu → menu dismisses with no selection (observed live on
        // Mains Type). Sheet rows can be XCUIElementTypeOther (v1.36
        // pattern) — those are the fallback pass only.
        By buttonRow = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + optionLabel + "'");
        By anyRow = AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeStaticText'"
                        + " OR type == 'XCUIElementTypeOther')"
                        + " AND label == '" + optionLabel + "'");
        // Menus animate in — a tap dispatched mid-animation misses. Wait for
        // a visible row, then a short settle for the frame to stabilize.
        waitForCondition(() -> firstVisible(buttonRow) != null || firstVisible(anyRow) != null, 5);
        pause(400);
        try {
            // LAST visible match: menu rows live at the end of the tree;
            // same-label background elements (custom-attribute chips) come
            // first and must not be tapped — see lastVisible javadoc.
            WebElement el = lastVisible(buttonRow);
            if (el == null) el = lastVisible(anyRow);
            if (el == null && existsNow(buttonRow)) el = driver.findElement(buttonRow);
            if (el == null) {
                System.out.println("⚠️ pickOptionExact: option '" + optionLabel + "' not present");
                return false;
            }
            org.openqa.selenium.Rectangle r = el.getRect();
            int cx = r.getX() + r.getWidth() / 2;
            int cy = r.getY() + r.getHeight() / 2;
            System.out.println("🎯 pickOptionExact('" + optionLabel + "'): coordinate-tap "
                    + el.getAttribute("type") + " center=(" + cx + "," + cy + ")");
            // iOS 26 SwiftUI Menu rows silently swallow BOTH
            // XCUIElement.click() AND `mobile: tap` (diagnosed live: menu
            // open, correct row, dead-center tap, no selection). A W3C
            // pointer sequence with a real press duration (~120ms between
            // down and up) goes through a different synthesis path and is
            // what the menu's gesture recognizer accepts.
            try {
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
            } catch (Exception w3cErr) {
                System.out.println("⚠️ W3C press failed (" + w3cErr.getMessage()
                        + ") — falling back to element click");
                el.click();
            }
        } catch (Exception e) {
            System.out.println("⚠️ pickOptionExact('" + optionLabel + "') tap: " + e.getMessage());
            return false;
        }
        pause(600);
        // Sheet variants keep a Done button after selection — close it.
        By doneBtn = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Done'");
        By searchField = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField'");
        if (existsNow(searchField) && existsNow(doneBtn)) {
            try { driver.findElement(doneBtn).click(); } catch (Exception ignored) { }
        }
        return true;
    }

    /** Count visible, plausibly-option rows in the open sheet (probe). */
    public int getSheetOptionCount() {
        return withImplicitWait(0, () -> {
            try {
                int n = 0;
                for (WebElement e : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeCell' OR type == 'XCUIElementTypeButton'"))) {
                    try {
                        String l = e.getAttribute("label");
                        if (l != null && !l.isBlank() && !l.equals("Done") && !l.equals("Cancel")
                                && !l.equals("None") && !l.equals("Close")) n++;
                    } catch (Exception ignored) { }
                }
                return n;
            } catch (Exception e) {
                return 0;
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════
    // 4. Match panel
    // ═════════════════════════════════════════════════════════════════

    /** "{n}[+] possible match(es)" / "No possible matches" header ("" if absent). */
    public String getMatchHeaderText() {
        try {
            if (existsNow(MATCH_HEADER)) {
                return driver.findElement(MATCH_HEADER).getAttribute("name");
            }
        } catch (Exception e) {
            System.out.println("⚠️ getMatchHeaderText: " + e.getMessage());
        }
        return "";
    }

    /** Wait for the match panel header to render (matcher is async). */
    public boolean waitForMatchHeader(int timeoutSeconds) {
        return waitForCondition(() -> existsNow(MATCH_HEADER), timeoutSeconds);
    }

    public boolean isAddCustomButtonShown() {
        return existsNow(ADD_CUSTOM_BUTTON);
    }

    public void tapAddCustom() {
        if (!swipeUntilVisible(ADD_CUSTOM_BUTTON, 4)) {
            throw new VerificationError("tapAddCustom: 'Add Custom' button never visible");
        }
        try {
            if (!pressElement(driver.findElement(ADD_CUSTOM_BUTTON))) {
                throw new VerificationError("tapAddCustom: press did not dispatch");
            }
        } catch (VerificationError ve) {
            throw ve;
        } catch (Exception e) {
            throw new VerificationError("tapAddCustom: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 5. Bound-library card
    // ═════════════════════════════════════════════════════════════════

    public boolean isBoundCardShown() {
        return existsNow(BOUND_CARD_TITLE);
    }

    public boolean isCustomEntryCardShown() {
        return existsNow(CUSTOM_ENTRY_TITLE);
    }

    /** Unbind the library row; verifies the bound card disappears. */
    public boolean tapUnlink() {
        if (!swipeUntilVisible(UNLINK_BUTTON, 4)) {
            System.out.println("⚠️ Unlink button never visible");
            return false;
        }
        try {
            if (!pressElement(driver.findElement(UNLINK_BUTTON))) return false;
        } catch (Exception e) {
            System.out.println("⚠️ Unlink tap: " + e.getMessage());
            return false;
        }
        return waitForCondition(() -> !existsNow(BOUND_CARD_TITLE) && !existsNow(CUSTOM_ENTRY_TITLE), 5);
    }

    // ═════════════════════════════════════════════════════════════════
    // 6. Custom Equipment sheet
    // ═════════════════════════════════════════════════════════════════

    public boolean isCustomSheetOpen(int timeoutSeconds) {
        By title = AppiumBy.iOSNsPredicateString(
                "name == '" + CUSTOM_SHEET_TITLE + "' OR name == '" + CUSTOM_SHEET_EDIT_TITLE + "'");
        return waitForCondition(() -> existsNow(title), timeoutSeconds);
    }

    /**
     * Type into a custom-sheet Form TextField located by its placeholder.
     * VISIBLE fields only — a background field behind the sheet with the
     * same placeholder must never receive the keystrokes.
     */
    public void typeCustomField(String placeholder, String text) {
        By field = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeTextField' AND (placeholderValue == '" + placeholder
                        + "' OR value == '" + placeholder + "')");
        try {
            WebElement el = firstVisible(field);
            if (el == null) el = driver.findElement(field);
            el.click();
            el.sendKeys(text);
        } catch (Exception e) {
            throw new VerificationError("typeCustomField('" + placeholder + "'): " + e.getMessage());
        }
        dismissKeyboard();
    }

    /** Enabled state of the sheet's Save button (Save gates on identity). */
    public boolean isCustomSaveEnabled() {
        By save = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Save'");
        try {
            if (existsNow(save)) {
                return "true".equals(driver.findElement(save).getAttribute("enabled"));
            }
        } catch (Exception e) {
            System.out.println("⚠️ isCustomSaveEnabled: " + e.getMessage());
        }
        return false;
    }

    public void tapCustomSheetButton(String label) {
        By btn = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == '" + label + "'");
        try {
            pressElement(driver.findElement(btn));
        } catch (Exception e) {
            throw new VerificationError("tapCustomSheetButton('" + label + "'): " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // 7. Close / discard
    // ═════════════════════════════════════════════════════════════════

    public boolean isDiscardAlertShown() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeAlert' AND name BEGINSWITH 'Discard Changes'"));
    }

    /**
     * Close Asset Details. When the draft is dirty the "Discard Changes?"
     * alert appears — discarded (or kept) per the flag. Returns true when we
     * end up back on the Assets list.
     */
    public boolean closeAssetDetails(boolean discardChanges) {
        By close = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name == 'Close' OR label == 'Close')");
        By cancel = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'");
        try {
            if (existsNow(close)) {
                driver.findElement(close).click();
            } else if (existsNow(cancel)) {
                // Add Asset form uses Cancel instead of Close.
                driver.findElement(cancel).click();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Close/Cancel tap: " + e.getMessage());
        }
        By assetListSignal = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name == 'plus'");
        // The Discard alert animates in slowly (same race as the load
        // dialog) — poll for EITHER the alert OR the list, then branch.
        waitForCondition(() -> existsNow(ANY_ALERT)
                || (!existsNow(ASSET_DETAILS_NAVBAR) && existsNow(assetListSignal)), 6);
        if (existsNow(ANY_ALERT)) {
            String action = discardChanges ? "Discard" : "Keep Editing";
            By btn = AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label BEGINSWITH '" + action + "'");
            try {
                if (existsNow(btn)) driver.findElement(btn).click();
            } catch (Exception e) {
                System.out.println("⚠️ discard-alert '" + action + "' tap: " + e.getMessage());
            }
            if (!discardChanges) return false; // intentionally staying on details
        }
        return waitForCondition(() -> !existsNow(ASSET_DETAILS_NAVBAR) && existsNow(assetListSignal), 8);
    }
}
