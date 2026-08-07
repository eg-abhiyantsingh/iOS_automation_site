package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Page object for the work-type PM-FORMS execution flow (v1.51, live-probed
 * 2026-07-27 — probe runs 12-15; user-verified screenshots same day).
 *
 * Anatomy contract (all probe-verified):
 *  - WO list rows are ONE full-width Button '<name>, <type label>, <priority>';
 *    the trailing activation CIRCLE is a tap ZONE at the right edge, raising
 *    the 'Start Work Order'/'Cancel' alert; ACTIVE rows append ', ACTIVE' to
 *    the composite and show an 'ACTIVE' StaticText; the Start-New button's
 *    composite carries its state ('…, End current work order session first'
 *    while a session is active / '…, Begin capturing IR photos…' when free).
 *  - Session Assets tab: tree of 'Bldg_x, N floors' / 'Floor …' Buttons;
 *    expanding a floor reveals room Buttons named '<room>, N assets'.
 *  - Assets-in-Room rows: Buttons '<asset name>, <class>, <formCount>' — the
 *    trailing number is the per-asset FORM BADGE.
 *  - Form screen (opens on asset tap): chip strip at y≈69 with one Button per
 *    form instance named '<Work Type> — <Procedure>' + a 'plus' Button; nav
 *    Buttons Back / trash / square.and.pencil / checkmark; 'Procedure Steps'
 *    info; 'Result' + 'Value / Notes' table headers; per-step Result dropdown
 *    Buttons (named '—' until set, then 'Pass'/'Fail') with a sibling
 *    TextField per row for Value/Notes; a Fail result reveals a
 *    'Description of Failure' section with a Photos picker.
 *
 * GIANT-DOM RULES: every query here is TYPE-bound (untyped name-CONTAINS
 * scans wedge WDA — probe run 13) and the session tree is only walked through
 * the bounded helpers below.
 */
public class WorkOrderFormsPage extends BasePage {

    /** Chip strip lives at the very top of the form sheet (probe: y≈69). */
    private static final int CHIP_ZONE_MAX_Y = 160;
    /** Result table rows start below the headers (probe: headers y≈307). */
    private static final int TABLE_ZONE_MIN_Y = 320;

    private static String pq(String s) {
        return "'" + s.replace("'", "\\'") + "'";
    }

    // ═══════════════════════ WO list — circle activation ═══════════════════

    /** The Start-New button composite carries the session state. */
    public String getStartNewComposite() {
        try {
            return withImplicitWait(0, () -> {
                List<WebElement> els = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Start New Work Order'"));
                return els.isEmpty() ? null : els.get(0).getAttribute("name");
            });
        } catch (Exception e) {
            return null;
        }
    }

    /** True when a session is active (Start-New demands ending it first). */
    public boolean isStartNewBlockedByActiveSession() {
        String c = getStartNewComposite();
        return c != null && c.contains("End current work order session");
    }

    /**
     * Composite of the row for {@code namePrefix}, or null (raw — may end
     * ', ACTIVE'). ACTIVE rows can report visible==0 while rendering (probe
     * 2026-07-27), so a rect-checked no-visible-filter fallback backs up the
     * strict query — same contract as WorkOrderPage.onScreenRowOrNull.
     */
    public String rowComposite(String namePrefix) {
        try {
            return withImplicitWait(0, () -> {
                List<WebElement> rows = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH "
                        + pq(namePrefix)));
                if (!rows.isEmpty()) return rows.get(0).getAttribute("name");
                int screenH;
                try {
                    screenH = driver.manage().window().getSize().getHeight();
                } catch (Exception e) {
                    screenH = 900;
                }
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name BEGINSWITH " + pq(namePrefix)))) {
                    try {
                        Rectangle r = el.getRect();
                        if (r.height > 20 && r.y > 80 && r.y < screenH - 40) {
                            return el.getAttribute("name");
                        }
                    } catch (Exception ignored) { }
                }
                return null;
            });
        } catch (Exception e) {
            return null;
        }
    }

    /** ACTIVE-badge contract: the active row's composite ends with ', ACTIVE'. */
    public boolean isRowActive(String namePrefix) {
        String c = rowComposite(namePrefix);
        return c != null && c.endsWith(", ACTIVE");
    }

    /**
     * Tap the activation CIRCLE (right-edge zone) of the row WITHOUT
     * confirming the alert. Returns true when the 'Start Work Order' alert is
     * up (alerts are left paused=manual for the caller to Confirm/Cancel via
     * {@link #confirmStartAlert()} / {@link #cancelStartAlert()}).
     */
    public boolean tapCircleExpectAlert(String namePrefix) {
        try {
            driver.setSetting("defaultAlertAction", "");
            WebElement row = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH "
                    + pq(namePrefix)));
            Rectangle r = row.getRect();
            driver.executeScript("mobile: tap",
                    Map.of("x", r.x + r.width - 35, "y", r.y + r.height / 2));
            return waitForCondition(() -> existsNow(START_ALERT_CONFIRM), 6);
        } catch (Exception e) {
            System.out.println("⚠️ tapCircleExpectAlert: " + e.getMessage());
            return false;
        }
    }

    private static final By START_ALERT_CONFIRM = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name == 'Start Work Order' AND visible == 1");
    private static final By START_ALERT_CANCEL = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND name == 'Cancel' AND visible == 1");

    /** Confirm the Start alert by coordinates; restores auto-accept. */
    public boolean confirmStartAlert() {
        boolean ok = tapAlertButton(START_ALERT_CONFIRM);
        restoreAutoAlerts();
        return ok;
    }

    /** Cancel the Start alert by coordinates; restores auto-accept. */
    public boolean cancelStartAlert() {
        boolean ok = tapAlertButton(START_ALERT_CANCEL);
        restoreAutoAlerts();
        return ok;
    }

    private boolean tapAlertButton(By locator) {
        try {
            WebElement btn = driver.findElement(locator);
            Rectangle r = btn.getRect();
            driver.executeScript("mobile: tap",
                    Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ tapAlertButton: " + e.getMessage());
            return false;
        }
    }

    private void restoreAutoAlerts() {
        try { driver.setSetting("defaultAlertAction", "accept"); } catch (Exception ignored) { }
    }

    /** Count of ACTIVE StaticText badges currently visible (radio invariant ≤ 1). */
    public int visibleActiveBadgeCount() {
        try {
            return withImplicitWait(0, () -> {
                int texts = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND name == 'ACTIVE' AND visible == 1")).size();
                if (texts > 0) return texts;
                // v1.55: neither the badge StaticText nor the row Button
                // reliably reports visible==1 (twin-visibility quirk; local
                // 2026-08-05: gate read the ACTIVE composite while the
                // visible==1 count was 0). Count DISTINCT ', ACTIVE' row
                // composites at ANY visibility — SwiftUI recycler ghosts share
                // the name, so the dedup keeps the radio invariant exact.
                java.util.Set<String> distinct = new java.util.HashSet<>();
                for (WebElement b : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name ENDSWITH ', ACTIVE'"))) {
                    try { distinct.add(b.getAttribute("name")); } catch (Exception ignored) { }
                }
                return distinct.size();
            });
        } catch (Exception e) {
            return -1;
        }
    }

    // ═══════════════════ session tree → room with assets ═══════════════════

    /**
     * From the session's Assets tab, expand floors as needed and open the
     * first room advertising a non-zero asset count ('<room>, N assets').
     * Bounded: at most one floor expansion + a short scroll sweep.
     */
    public boolean openFirstRoomWithAssetsInTree() {
        try {
            return withImplicitWait(0, () -> {
                // v1.55: the session lands on the DETAILS tab — enter the
                // Assets tab first (bottom strip Button, rect.y>800).
                if (roomsWithAssets().isEmpty() && !isAssetsInRoomOpen()) {
                    try {
                        WebElement assetsTab = driver.findElement(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeButton' AND name == 'Assets' AND visible == 1 AND rect.y > 800"));
                        System.out.println("🌳 entering session Assets tab (v1.55)");
                        org.openqa.selenium.Rectangle tr = assetsTab.getRect();
                        driver.executeScript("mobile: tap",
                                java.util.Map.of("x", tr.x + tr.width / 2, "y", tr.y + tr.height / 2));
                        pauseMs(1000);
                    } catch (Exception ignored) { }
                }
                List<WebElement> rooms = roomsWithAssets();
                // v1.55 session tree, probe-pinned 2026-08-07: room rows are
                // FULL-PATH composites '<bldg> › <floor>, <room>' and clicking
                // one NAVIGATES into 'Assets in Room' (no expansion step; the
                // trailing segment is the ROOM NAME, not a count). Iterate
                // path rows until one holds ACTIVE assets; empty rooms
                // ('No Active Assets') are backed out of.
                // FAST PASS (2026-08-07): count-advertised rooms ('N assets')
                // are definitive asset-bearers. Batch run 3 proved the walk
                // must be DETERMINISTIC: tests inherit arbitrary scroll state,
                // and the enter-and-back-out path walk burned 5 of the 6
                // budget minutes touring empty debris rooms (7 timeout kills,
                // 6 dry skips). So: scroll to TOP first, then sweep the WHOLE
                // list for a count row; only then fall back to a BOUNDED walk.
                if (rooms.isEmpty()) {
                    for (int s = 0; s < 4; s++) swipe("down"); // deterministic start: top of tree
                    rooms = roomsWithAssets();
                    for (int s = 0; s < 8 && rooms.isEmpty(); s++) {
                        swipe("up");
                        rooms = roomsWithAssets();
                    }
                    if (rooms.isEmpty()) {
                        for (int s = 0; s < 9; s++) swipe("down"); // restore top for the path walk
                    }
                }
                if (!rooms.isEmpty()) {
                    String name = rooms.get(0).getAttribute("name");
                    System.out.println("🚪 opening room (fast pass): " + name);
                    rooms.get(0).click();
                    return true;
                }
                java.util.Set<String> seenPaths = new java.util.HashSet<>();
                if (rooms.isEmpty()) {
                    int pathSwipes = 0;
                    // BOUNDED last resort: 3 room entries max — each entry
                    // costs ~30-45s and the whole method must stay well under
                    // the 6-minute test cap (batch run 3 lesson).
                    for (int i = 0; i < 3; i++) {
                        WebElement pathRow = null;
                        String pathName = null;
                        for (WebElement b : driver.findElements(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS ' › ' "
                                + "AND rect.y > 120 AND rect.y < 800"))) {
                            String n;
                            try { n = b.getAttribute("name"); } catch (Exception e) { continue; }
                            if (n == null || seenPaths.contains(n)) continue;
                            pathRow = b;
                            pathName = n;
                            break;
                        }
                        if (pathRow == null) {
                            if (seenPaths.isEmpty() || pathSwipes >= 2) break;
                            pathSwipes++;
                            swipe("up"); // more path rows may sit below the fold
                            continue;
                        }
                        seenPaths.add(pathName);
                        System.out.println("🚪 v1.55 path-row into room: '" + pathName + "'");
                        try { pathRow.click(); } catch (Exception e) { continue; }
                        pauseMs(1200);
                        if (isAssetsInRoomOpen()) {
                            if (!visibleAssetRowComposites().isEmpty()) return true;
                            System.out.println("🚪 room has no ACTIVE assets — backing out to the tree");
                            try {
                                WebElement back = driver.findElement(AppiumBy.iOSNsPredicateString(
                                        "type == 'XCUIElementTypeButton' AND name == 'BackButton' AND visible == 1"));
                                org.openqa.selenium.Rectangle br = back.getRect();
                                driver.executeScript("mobile: tap",
                                        java.util.Map.of("x", br.x + br.width / 2, "y", br.y + br.height / 2));
                                pauseMs(1000);
                            } catch (Exception e) {
                                System.out.println("⚠️ back-out failed after empty room: " + e.getMessage());
                                return false;
                            }
                        }
                    }
                }
                // Expansion cascade: building rows read '<name>, N floor(s)',
                // floor rows '<name>, N room(s)' (v1.55 tree). Each level is
                // expanded at most ONCE — a second tap TOGGLES it closed.
                // SKIPPED on the path-row tree shape (those rows contain
                // ' floor' but clicking them ENTERS a room, not an expansion).
                if (rooms.isEmpty() && seenPaths.isEmpty()) {
                    for (String suffix : new String[]{" floor", " room"}) {
                        List<WebElement> expandables = driver.findElements(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS '" + suffix + "'"));
                        if (!expandables.isEmpty()) {
                            try {
                                System.out.println("🌳 expanding: " + expandables.get(0).getAttribute("name"));
                                expandables.get(0).click();
                            } catch (Exception ignored) { }
                            pauseMs(800);
                        }
                        rooms = roomsWithAssets();
                        if (!rooms.isEmpty()) break;
                    }
                }
                for (int i = 0; i < 6 && rooms.isEmpty(); i++) {
                    swipe("up");
                    rooms = roomsWithAssets();
                }
                if (!rooms.isEmpty()) {
                    String name = rooms.get(0).getAttribute("name");
                    System.out.println("🚪 opening room: " + name);
                    rooms.get(0).click();
                    return true;
                }
                // v1.55 third strategy (PROBE_L): some trees expose room rows
                // as BARE-NAMED Buttons (no count composite, no subtitle) —
                // 'Room 101 - Conference_396'. Open candidates in tree order;
                // empty rooms are backed out of (fixture rooms are often
                // empty) until an ASSET-BEARING one is found. Candidates are
                // RE-QUERIED per attempt: navigation invalidates elements.
                java.util.Set<String> visited = new java.util.HashSet<>();
                for (int attempt = 0; attempt < 4; attempt++) {
                    WebElement cand = findBareRoomCandidate(visited);
                    if (cand == null && attempt > 0) {
                        // Back-out RESETS the tree's expansion (observed
                        // 2026-08-07: after one candidate the scan went dry
                        // every time) — re-expand once, then rescan.
                        for (String suffix : new String[]{" floor", " room"}) {
                            List<WebElement> expandables = driver.findElements(AppiumBy.iOSNsPredicateString(
                                    "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS '" + suffix + "'"));
                            if (!expandables.isEmpty()) {
                                try { expandables.get(0).click(); } catch (Exception ignored) { }
                                pauseMs(800);
                            }
                            if (findBareRoomCandidate(visited) != null || !roomsWithAssets().isEmpty()) break;
                        }
                        List<WebElement> counted = roomsWithAssets();
                        if (!counted.isEmpty()) {
                            System.out.println("🚪 opening count-advertised room after re-expansion");
                            counted.get(0).click();
                            return true;
                        }
                        cand = findBareRoomCandidate(visited);
                    }
                    if (cand == null) break;
                    String candName;
                    try { candName = cand.getAttribute("name"); } catch (Exception e) { break; }
                    visited.add(candName);
                    System.out.println("🚪 trying bare-named room row: '" + candName + "'");
                    try {
                        org.openqa.selenium.Rectangle r = cand.getRect();
                        driver.executeScript("mobile: tap",
                                java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                    } catch (Exception e) { continue; }
                    pauseMs(1200);
                    if (!isAssetsInRoomOpen()) continue;
                    if (!visibleAssetRowComposites().isEmpty()) return true;
                    System.out.println("🚪 room '" + candName + "' is empty — backing out to the tree");
                    try {
                        WebElement back = driver.findElement(AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeButton' AND name == 'BackButton' AND visible == 1"));
                        org.openqa.selenium.Rectangle br = back.getRect();
                        driver.executeScript("mobile: tap",
                                java.util.Map.of("x", br.x + br.width / 2, "y", br.y + br.height / 2));
                        pauseMs(1000);
                    } catch (Exception e) {
                        System.out.println("⚠️ back-out failed: " + e.getMessage());
                        return false;
                    }
                }
                // Give-up diagnostics: what IS on screen (names tell whether we
                // are on the tree, inside a room, or somewhere unexpected).
                try {
                    StringBuilder sb = new StringBuilder();
                    for (WebElement b : driver.findElements(AppiumBy.iOSNsPredicateString(
                            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                            + "OR type == 'XCUIElementTypeNavigationBar') AND visible == 1"))) {
                        try {
                            org.openqa.selenium.Rectangle r = b.getRect();
                            sb.append("[").append(b.getAttribute("type").replace("XCUIElementType", ""))
                              .append(" y").append(r.y).append(" '").append(b.getAttribute("name")).append("'] ");
                        } catch (Exception ignored) { }
                        if (sb.length() > 1800) break;
                    }
                    System.out.println("🔎 give-up screen census: " + sb);
                } catch (Exception e) {
                    System.out.println("🔎 give-up census failed: " + e.getMessage());
                }
                System.out.println("⚠️ no ASSET-BEARING room found (visited " + visited + ")");
                return false;
            });
        } catch (Exception e) {
            System.out.println("⚠️ openFirstRoomWithAssetsInTree: " + e.getMessage());
            return false;
        }
    }

    /**
     * First visible bare-named room-row candidate (v1.55 tree) not yet
     * visited. Excludes every known CONTROL name — 'Add' especially: it is a
     * toolbar Button that passed the old filter and got tapped as a "room"
     * (observed 2026-08-07, forms audit run 2).
     */
    private WebElement findBareRoomCandidate(java.util.Set<String> visited) {
        for (WebElement b : driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > 120 AND rect.y < 800"))) {
            String n;
            try { n = b.getAttribute("name"); } catch (Exception e) { continue; }
            if (n == null || n.isEmpty() || n.contains(" floor") || n.contains(" room")
                    || "plus".equals(n) || "qrcode.viewfinder".equals(n)
                    || "arrow.clockwise".equals(n) || "BackButton".equals(n) || "Done".equals(n)
                    || "Add".equals(n) || "Edit".equals(n) || "Filter".equals(n) || "Sort".equals(n)
                    || n.startsWith("Search") || visited.contains(n)) continue;
            return b;
        }
        return null;
    }

    private List<WebElement> roomsWithAssets() {
        // Legacy shape first: composite Button '<room>, N asset(s)'.
        List<WebElement> rows = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS ' asset'"));
        List<WebElement> nonZero = new ArrayList<>();
        for (WebElement r : rows) {
            try {
                String n = r.getAttribute("name");
                if (n != null && n.matches(".*,\\s*[1-9]\\d*\\s+assets?\\s*$")) nonZero.add(r);
            } catch (Exception ignored) { }
        }
        if (!nonZero.isEmpty()) return nonZero;
        // v1.55 tree (PROBE_L 2026-08-04): the room row Button is named JUST
        // '<room name>' — the 'N asset(s)' count is a SUBTITLE StaticText twin.
        // Pair count-texts to row Buttons by y-band (±40pt), bounded queries.
        try {
            List<WebElement> counts = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == 1 AND name MATCHES '[1-9][0-9]* assets?'"));
            if (counts.isEmpty()) return nonZero;
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > 120 AND rect.y < 820"));
            for (WebElement c : counts) {
                int cy = c.getRect().y;
                WebElement best = null;
                int bestDy = Integer.MAX_VALUE;
                for (WebElement b : buttons) {
                    try {
                        String n = b.getAttribute("name");
                        if (n == null || n.isEmpty() || n.contains(" floor") || n.contains(" room")
                                || "plus".equals(n) || "qrcode.viewfinder".equals(n)) continue;
                        int dy = Math.abs(b.getRect().y - cy);
                        if (dy < bestDy) { bestDy = dy; best = b; }
                    } catch (Exception ignored) { }
                }
                if (best != null && bestDy <= 40) nonZero.add(best);
            }
        } catch (Exception e) {
            System.out.println("⚠️ roomsWithAssets subtitle-pairing: " + e.getMessage());
        }
        return nonZero;
    }

    /** 'Assets in Room' nav bar present? */
    public boolean isAssetsInRoomOpen() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar' AND name == 'Assets in Room'"));
    }

    // ═══════════════════ assets-in-room rows + form badge ═══════════════════

    /** Visible asset-row composites ('<name>, <class>, <formCount>'). */
    public List<String> visibleAssetRowComposites() {
        List<String> out = new ArrayList<>();
        try {
            withImplicitWait(0, () -> {
                int i = 0;
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND name MATCHES '.+, .+, \\\\d+'"))) {
                    try {
                        String n = el.getAttribute("name");
                        if (n != null) out.add(n);
                    } catch (Exception ignored) { }
                    if (++i >= 15) break;
                }
                return null;
            });
        } catch (Exception e) {
            System.out.println("⚠️ visibleAssetRowComposites: " + e.getMessage());
        }
        return out;
    }

    /** Form-badge count parsed off the asset row, or -1 when not found. */
    public int assetFormBadge(String assetNamePrefix) {
        String composite = rowComposite(assetNamePrefix);
        if (composite == null) return -1;
        try {
            String[] parts = composite.split(",\\s*");
            return Integer.parseInt(parts[parts.length - 1].trim());
        } catch (Exception e) {
            return -1;
        }
    }

    /** Tap the asset row → its form screen opens (verified via chip strip). */
    public boolean openAssetForms(String assetNamePrefix) {
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH "
                    + pq(assetNamePrefix))).click();
        } catch (Exception e) {
            System.out.println("⚠️ openAssetForms tap: " + e.getMessage());
            return false;
        }
        return waitForCondition(this::isFormScreenOpen, 8);
    }

    // ═══════════════════════════ form screen ════════════════════════════════

    /** Form screen signature: the 'Procedure Steps' info + a chip in the top zone. */
    public boolean isFormScreenOpen() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH 'Procedure Steps'"))
            || !getFormChipNames().isEmpty();
    }

    /** Names of the form-instance chips (top strip), in visual order. */
    public List<String> getFormChipNames() {
        List<String> out = new ArrayList<>();
        try {
            withImplicitWait(0, () -> {
                Set<String> seen = new LinkedHashSet<>();
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1"))) {
                    try {
                        Rectangle r = el.getRect();
                        if (r.y > CHIP_ZONE_MAX_Y) continue;
                        String n = el.getAttribute("name");
                        if (n == null || n.isEmpty()) continue;
                        if (n.equals("plus") || n.equals("Back") || n.equals("trash")
                                || n.equals("checkmark") || n.equals("square.and.pencil")) continue;
                        seen.add(n);
                    } catch (Exception ignored) { }
                }
                out.addAll(seen);
                return null;
            });
        } catch (Exception e) {
            System.out.println("⚠️ getFormChipNames: " + e.getMessage());
        }
        return out;
    }

    /** Select a form-instance chip by (partial) name. */
    public boolean selectFormChip(String nameFragment) {
        try {
            List<WebElement> chips = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND visible == 1 AND name CONTAINS "
                            + pq(nameFragment))));
            for (WebElement chip : chips) {
                if (chip.getRect().y <= CHIP_ZONE_MAX_Y) {
                    chip.click();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ selectFormChip: " + e.getMessage());
            return false;
        }
    }

    /** Nav-zone control presence (Back / trash / square.and.pencil / checkmark / plus). */
    public boolean isFormControlPresent(String controlName) {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name == " + pq(controlName) + " AND visible == 1"));
    }

    /**
     * Per-step Result dropdown Buttons, top-to-bottom (table zone only — chips
     * for unfilled forms are also named '—', so geometry disambiguates).
     * Values: '—' (unset), 'Pass', 'Fail'.
     */
    public List<WebElement> resultDropdowns() {
        List<WebElement> out = new ArrayList<>();
        try {
            withImplicitWait(0, () -> {
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND visible == 1 AND "
                        + "(name == '—' OR name == 'Pass' OR name == 'Fail')"))) {
                    try {
                        if (el.getRect().y >= TABLE_ZONE_MIN_Y) out.add(el);
                    } catch (Exception ignored) { }
                }
                return null;
            });
        } catch (Exception e) {
            System.out.println("⚠️ resultDropdowns: " + e.getMessage());
        }
        return out;
    }

    public int stepCount() {
        return resultDropdowns().size();
    }

    /** Current value of the Nth (0-based) step's Result dropdown, or null. */
    public String stepResult(int index) {
        List<WebElement> dds = resultDropdowns();
        if (index < 0 || index >= dds.size()) return null;
        try {
            return dds.get(index).getAttribute("name");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Open the Nth step's Result dropdown and choose {@code value} (Pass/Fail).
     * The options render as Buttons/StaticTexts once the dropdown opens —
     * multi-strategy tap on the exact value below the dropdown's own row.
     */
    public boolean setStepResult(int index, String value) {
        List<WebElement> dds = resultDropdowns();
        if (index < 0 || index >= dds.size()) return false;
        try {
            WebElement dd = dds.get(index);
            Rectangle ddRect = dd.getRect();
            dd.click();
            pauseMs(700);
            // Strategy 1: exact-name Button that is NOT one of the table cells we
            // already track (a fresh option appears once the picker is open).
            List<WebElement> options = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND name == " + pq(value) + " AND visible == 1")));
            for (WebElement opt : options) {
                Rectangle r = opt.getRect();
                boolean isTheDropdownItself = Math.abs(r.y - ddRect.y) < 5 && Math.abs(r.x - ddRect.x) < 5;
                if (!isTheDropdownItself) {
                    opt.click();
                    return waitForCondition(() -> value.equals(stepResult(index)), 5);
                }
            }
            // Strategy 2: StaticText option (menu items are sometimes texts).
            List<WebElement> textOpts = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND name == " + pq(value) + " AND visible == 1")));
            for (WebElement opt : textOpts) {
                Rectangle r = opt.getRect();
                if (r.y < ddRect.y - 10 || r.y > ddRect.y + 10) {
                    opt.click();
                    return waitForCondition(() -> value.equals(stepResult(index)), 5);
                }
            }
            System.out.println("⚠️ setStepResult: no '" + value + "' option surfaced");
            return false;
        } catch (Exception e) {
            System.out.println("⚠️ setStepResult: " + e.getMessage());
            return false;
        }
    }

    /** Value/Notes TextFields in the table zone, top-to-bottom. */
    public List<WebElement> noteFields() {
        List<WebElement> out = new ArrayList<>();
        try {
            withImplicitWait(0, () -> {
                for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND visible == 1"))) {
                    try {
                        if (el.getRect().y >= TABLE_ZONE_MIN_Y - 20) out.add(el);
                    } catch (Exception ignored) { }
                }
                return null;
            });
        } catch (Exception e) {
            System.out.println("⚠️ noteFields: " + e.getMessage());
        }
        return out;
    }

    /** Type into the Nth step's Value/Notes field, then dismiss the keyboard. */
    public boolean typeStepNotes(int index, String text) {
        List<WebElement> fields = noteFields();
        if (index < 0 || index >= fields.size()) return false;
        try {
            WebElement f = fields.get(index);
            f.click();
            f.clear();
            f.sendKeys(text);
            hideKeyboardSafe();
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ typeStepNotes: " + e.getMessage());
            return false;
        }
    }

    /**
     * Readback of the Nth step's Value/Notes field. 'value' first; iOS 18.5
     * returns null there after typing (CI run 31156460536: FORM_035/036
     * "got 'null'" while 26.2 reads fine) — fall back to 'label', and
     * re-query once (keyboard dismissal re-renders the field list).
     */
    public String stepNotes(int index) {
        for (int attempt = 0; attempt < 2; attempt++) {
            List<WebElement> fields = noteFields();
            if (index < 0 || index >= fields.size()) return null;
            try {
                WebElement f = fields.get(index);
                String v = f.getAttribute("value");
                if (v == null || v.isEmpty()) {
                    String l = f.getAttribute("label");
                    if (l != null && !l.isEmpty()) v = l;
                }
                if (v != null && !v.isEmpty()) return v;
            } catch (Exception ignored) { }
            pauseMs(600);
        }
        return null;
    }

    /**
     * Bounded diagnostic census of the form's interactive surface — CI-side
     * ground truth for the 18.5 step-surface divergences. ONE getPageSource
     * call parsed locally: per-element getAttribute reads cost a WDA round
     * trip EACH and burned 9 minutes on this screen (local measurement,
     * 2026-08-07) — never census that way. Never throws.
     */
    public List<String> debugTableZoneCensus() {
        List<String> out = new ArrayList<>();
        try {
            String src = driver.getPageSource();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "<(XCUIElementType(?:Button|TextField|TextView|StaticText|SegmentedControl))"
                    + "([^>]*)/?>").matcher(src);
            while (m.find() && out.size() < 60) {
                String attrs = m.group(2);
                String name = attrVal(attrs, "name");
                String label = attrVal(attrs, "label");
                String value = attrVal(attrs, "value");
                String y = attrVal(attrs, "y");
                String visible = attrVal(attrs, "visible");
                if (!"true".equals(visible)) continue;
                out.add(m.group(1).replace("XCUIElementType", "") + " y=" + y
                        + " | name='" + name + "' | label='" + label + "' | value='" + value + "'");
            }
        } catch (Exception e) {
            out.add("census error: " + e.getMessage());
        }
        return out;
    }

    private static String attrVal(String attrs, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                key + "=\"([^\"]*)\"").matcher(attrs);
        return m.find() ? m.group(1) : "";
    }

    /** The Fail-path failure card ('… — Failure Details' / 'Description of Failure'). */
    public boolean isFailureDetailsVisible() {
        return existsNow(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND visible == 1 AND "
                + "(name CONTAINS 'Failure Details' OR name == 'Description of Failure')"));
    }

    /** Type the failure description (TextView inside the failure card). */
    public boolean typeFailureDescription(String text) {
        try {
            List<WebElement> views = withImplicitWait(0, () -> driver.findElements(
                    AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextView' AND visible == 1")));
            if (views.isEmpty()) return false;
            WebElement v = views.get(views.size() - 1);
            v.click();
            v.sendKeys(text);
            hideKeyboardSafe();
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ typeFailureDescription: " + e.getMessage());
            return false;
        }
    }

    /** Save/complete the form via the checkmark nav control. */
    public boolean saveForm() {
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'checkmark' AND visible == 1")).click();
            pauseMs(800);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ saveForm: " + e.getMessage());
            return false;
        }
    }

    /** Leave the form screen via Back. */
    public boolean backFromForm() {
        try {
            driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Back' AND visible == 1")).click();
            pauseMs(600);
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ backFromForm: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────── internals ─────────────────────────────────

    private void swipe(String direction) {
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("direction", direction);
            driver.executeScript("mobile: swipe", args);
        } catch (Exception e) {
            System.out.println("⚠️ swipe(" + direction + "): " + e.getMessage());
        }
    }

    private void hideKeyboardSafe() {
        try {
            driver.executeScript("mobile: hideKeyboard");
        } catch (Exception e) {
            // Fallback: tap above the table (info banner zone) to resign focus.
            try {
                driver.executeScript("mobile: tap", Map.of("x", 220, "y", 200));
            } catch (Exception ignored) { }
        }
    }

    private void pauseMs(long ms) {
        com.egalvanic.utils.Waits.until(() -> false, ms);
    }
}
