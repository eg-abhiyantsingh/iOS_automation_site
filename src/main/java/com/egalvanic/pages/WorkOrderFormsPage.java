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
                                "type == 'XCUIElementTypeButton' AND name == 'Assets' AND visible == 1 AND rect.y > 700"));
                        System.out.println("🌳 entering session Assets tab (v1.55)");
                        org.openqa.selenium.Rectangle tr = assetsTab.getRect();
                        driver.executeScript("mobile: tap",
                                java.util.Map.of("x", tr.x + tr.width / 2, "y", tr.y + tr.height / 2));
                        pauseMs(1000);
                    } catch (Exception ignored) { }
                }
                switchToAllRoomsIfOffered();
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
                    WebElement room = rooms.get(0);
                    pressCenter(room);
                    if (!waitForCondition(this::isAssetsInRoomOpen, 6)) {
                        try { room.click(); } catch (Exception ignored) { }
                    }
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
    private static final java.util.Set<String> SESSION_TABS =
            java.util.Set.of("Details", "Assets", "Forms", "Issues", "More", "Tasks", "IR", "Files");

    /**
     * 1.67: a session with no active work opens its Assets tab on "No rooms have active work" + 'Switch to
     * All Rooms' — there are NO room rows until it is pressed. Coordinate press; true when it was offered.
     */
    private boolean switchToAllRoomsIfOffered() {
        try {
            List<WebElement> sw = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label == 'Switch to All Rooms' OR name == 'Switch to All Rooms')"
                    + " AND visible == 1"));
            if (sw.isEmpty()) return false;
            org.openqa.selenium.Rectangle r = sw.get(0).getRect();
            driver.executeScript("mobile: tap", java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
            pauseMs(1500);
            System.out.println("🌳 'No rooms have active work' — switched to All Rooms");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement findBareRoomCandidate(java.util.Set<String> visited) {
        for (WebElement b : driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == 1 AND rect.y > 120 AND rect.y < 800"))) {
            String n;
            try { n = b.getAttribute("name"); } catch (Exception e) { continue; }
            if (n == null || n.isEmpty() || n.contains(" floor") || n.contains(" room")
                    || "plus".equals(n) || "qrcode.viewfinder".equals(n)
                    || "arrow.clockwise".equals(n) || "BackButton".equals(n) || "Done".equals(n)
                    || "Add".equals(n) || "Edit".equals(n) || "Filter".equals(n) || "Sort".equals(n)
                    || n.startsWith("Search") || visited.contains(n)
                    // 1.67: the session tab strip sits at y≈786 (inside the window above) — its buttons were
                    // being "tried" as rooms (PhotoCategories, 2026-10-02); same for the empty-state controls.
                    || SESSION_TABS.contains(n) || n.endsWith(", Issues") || n.endsWith(", Assets")
                    || "Switch to All Rooms".equals(n) || "Scan Qr Code".equals(n) || "checklist".equals(n)) continue;
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
        WebElement row;
        try {
            row = driver.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND name BEGINSWITH "
                    + pq(assetNamePrefix)));
        } catch (Exception e) {
            System.out.println("⚠️ openAssetForms find: " + e.getMessage());
            return false;
        }
        // Coordinate press first (click() is a silent no-op on 1.67+ SwiftUI rows), then verify;
        // fall back to click() once if the form screen did not appear.
        pressCenter(row);
        if (waitForCondition(this::isFormScreenOpen, 6)) return true;
        try { row.click(); } catch (Exception ignored) { }
        return waitForCondition(this::isFormScreenOpen, 6);
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

    /** Tap the nav-zone '+' (Add Form) control. False when it is not on screen. */
    public boolean tapAddFormControl() {
        try {
            WebElement plus = withImplicitWait(0, () -> {
                List<WebElement> l = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND name == 'plus' AND visible == 1"));
                return l.isEmpty() ? null : l.get(0);
            });
            if (plus == null) return false;
            plus.click();
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ tapAddFormControl: " + e.getMessage());
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

    /**
     * Type into the Nth step's Value/Notes field, then dismiss the keyboard.
     * iOS 18.5: element.click() does NOT focus SwiftUI TextFields — the D01
     * census (CI run 31185473008) showed the field completely empty after a
     * "successful" click+sendKeys, while 26.2 typed fine. Coordinate-tap to
     * focus, confirm the keyboard actually appeared, re-find the field
     * (focus re-renders the list), type, then VERIFY the text landed.
     */
    public boolean typeStepNotes(int index, String text) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            List<WebElement> fields = noteFields();
            if (index < 0 || index >= fields.size()) return false;
            try {
                Rectangle r = fields.get(index).getRect();
                driver.executeScript("mobile: tap",
                        java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                waitForCondition(() -> existsNow(AppiumBy.className("XCUIElementTypeKeyboard")), 4);
                List<WebElement> fresh = noteFields();
                if (index >= fresh.size()) continue;
                WebElement f = fresh.get(index);
                f.clear();
                f.sendKeys(text);
                hideKeyboardSafe();
                String got = stepNotes(index);
                if (got != null && got.contains(text)) return true;
                System.out.println("⚠️ typeStepNotes attempt " + attempt
                        + ": text did not land (readback '" + got + "')");
            } catch (Exception e) {
                System.out.println("⚠️ typeStepNotes attempt " + attempt + ": " + e.getMessage());
            }
        }
        return false;
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

    // ═══════════════ v1.69 form instances + 'Copy Data To' (ZP-3928 §6) ═══════════════
    // Live 1.69 (2026-10-05, QA-WT10 NETA): an 'Assets in Room' row is '<name>, <Class>, <formCount>' with a
    // leading chevron Button 'Forward' that EXPANDS the row inline to its form instances (title StaticText +
    // status line 'Not Started' / 'In Progress' / …). Tapping the row itself opens Asset Details, not forms.
    // Long-pressing an instance title → menu 'Open Form' / 'Copy Data To…' / 'Delete'. 'Copy Data To…' →
    // sheet nav 'Copy Data' (Cancel · Copy), SearchField 'Search assets...', 'Select all (N)', target rows
    // '<asset>, <room> · <status>'; a query matching nothing shows 'No assets found'.

    public static final String COPY_DATA_MENU_ITEM = "Copy Data To…";
    public static final String COPY_DATA_TITLE = "Copy Data";
    public static final String COPY_DATA_SEARCH = "Search assets...";
    public static final String COPY_DATA_NO_RESULTS = "No assets found";
    private static final java.util.regex.Pattern FORM_STATUS =
            java.util.regex.Pattern.compile("^(Not Started|In Progress|Submitted|Completed|Draft)$");

    /** Asset rows advertising at least one form ('<name>, <Class>, N' with N ≥ 1), top to bottom. */
    public List<SnapNode> assetRowsWithForms() {
        List<SnapNode> out = new ArrayList<>();
        for (SnapNode n : snapshot()) {
            if (!n.visible || !"Button".equals(n.type) || n.x < 40) continue;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^.+, .+, (\\d+)$").matcher(n.label);
            if (m.matches() && Integer.parseInt(m.group(1)) > 0) out.add(n);
        }
        out.sort(java.util.Comparator.comparingInt(n -> n.y));
        return out;
    }

    /** Expand an asset row (chevron 'Forward' on the same line) and return its form-instance titles. */
    public List<String> expandAssetFormInstances(SnapNode row) {
        SnapNode chevron = snapshot().stream()
                .filter(n -> n.visible && "Button".equals(n.type) && n.is("Forward") && Math.abs(n.y - row.y) < 12)
                .findFirst().orElse(null);
        if (chevron == null) { System.out.println("⚠️ no 'Forward' chevron beside '" + row.label + "'"); return List.of(); }
        driver.executeScript("mobile: tap", Map.of("x", chevron.centerX(), "y", chevron.centerY()));
        List<String> titles = new ArrayList<>();
        com.egalvanic.utils.Waits.until(() -> { titles.clear(); titles.addAll(formInstanceTitlesBelow(row)); return !titles.isEmpty(); },
                8_000, 700);
        System.out.println("📋 form instances under '" + row.label + "': " + titles);
        return titles;
    }

    /** Titles directly above a status line, between this row and the next asset row. */
    private List<String> formInstanceTitlesBelow(SnapNode row) {
        List<SnapNode> s = snapshot();
        int nextRowY = s.stream().filter(n -> n.visible && "Button".equals(n.type) && n.x >= 40 && n.y > row.y + 20
                        && n.label.matches("^.+, .+, \\d+$")).mapToInt(n -> n.y).min().orElse(Integer.MAX_VALUE);
        List<String> out = new ArrayList<>();
        for (SnapNode st : s) {
            if (!st.visible || !"StaticText".equals(st.type) || !FORM_STATUS.matcher(st.label).matches()) continue;
            if (st.y <= row.y || st.y >= nextRowY) continue;
            s.stream().filter(t -> t.visible && "StaticText".equals(t.type) && t.y < st.y && st.y - t.y < 40
                            && t.x >= 100 && !FORM_STATUS.matcher(t.label).matches())
                    .max(java.util.Comparator.comparingInt(t -> t.y)).ifPresent(t -> out.add(t.label));
        }
        return out;
    }

    /** Long-press a form-instance title and choose 'Copy Data To…'; true once the 'Copy Data' sheet is up. */
    public boolean openCopyDataTo(String instanceTitle) {
        SnapNode t = snapshot().stream()
                .filter(n -> n.visible && "StaticText".equals(n.type) && instanceTitle.equals(n.label))
                .findFirst().orElse(null);
        if (t == null) return false;
        if (!longPressAt(t.centerX(), t.centerY(), 1.2)) return false;
        SnapNode item = null;
        long end = System.currentTimeMillis() + 5_000;
        while (item == null && System.currentTimeMillis() < end) {
            item = snapshot().stream().filter(n -> n.visible && "Button".equals(n.type)
                    && (n.is(COPY_DATA_MENU_ITEM) || n.label.startsWith("Copy Data To"))).findFirst().orElse(null);
            if (item == null) sleep(400);
        }
        if (item == null) { System.out.println("⚠️ long-press menu offered no '" + COPY_DATA_MENU_ITEM + "'"); return false; }
        driver.executeScript("mobile: tap", Map.of("x", item.centerX(), "y", item.centerY()));
        return com.egalvanic.utils.Waits.until(this::isCopyDataSheetOpen, 8_000, 500);
    }

    public boolean isCopyDataSheetOpen() {
        return snapshot().stream().anyMatch(n -> n.visible && ("SearchField".equals(n.type) && n.label.startsWith("Search assets")
                || "StaticText".equals(n.type) && n.is(COPY_DATA_TITLE)));
    }

    public boolean isCopyDataSearchFieldPresent() {
        return snapshot().stream().anyMatch(n -> n.visible && "SearchField".equals(n.type)
                && (n.is(COPY_DATA_SEARCH) || n.label.startsWith("Search assets")));
    }

    /** Target rows '<asset>, <room> · <status>' currently listed in the sheet. */
    public List<String> copyDataTargets() {
        List<String> out = new ArrayList<>();
        for (SnapNode n : snapshot()) {
            if (n.visible && "Button".equals(n.type) && n.label.contains(" · ") && n.label.contains(", ")) out.add(n.label);
        }
        return out;
    }

    /** 'Select all (N)' count, or -1 when the control is absent. */
    public int copyDataSelectAllCount() {
        for (SnapNode n : snapshot()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^Select all \\((\\d+)\\)$").matcher(n.label);
            if (n.visible && m.matches()) return Integer.parseInt(m.group(1));
        }
        return -1;
    }

    public boolean isCopyDataNoResultsShown() {
        return snapshot().stream().anyMatch(n -> n.visible && "StaticText".equals(n.type)
                && (n.is(COPY_DATA_NO_RESULTS) || n.label.startsWith("No matches")));
    }

    /** Focus the sheet's search field (coordinate press) and replace its text via key actions. */
    public boolean typeCopyDataSearch(String text) {
        SnapNode f = snapshot().stream().filter(n -> n.visible && "SearchField".equals(n.type)).findFirst().orElse(null);
        if (f == null) return false;
        driver.executeScript("mobile: tap", Map.of("x", f.centerX(), "y", f.centerY()));
        sleep(500);
        // The field is reachable in the page source but element lookups 404 inside this sheet (probe
        // 2026-10-05) — key actions go to the focused field, which is what a user's typing does.
        StringBuilder keys = new StringBuilder();
        for (int i = 0; i < 24; i++) keys.append(org.openqa.selenium.Keys.BACK_SPACE);
        keys.append(text);
        new org.openqa.selenium.interactions.Actions(driver).sendKeys(keys.toString()).perform();
        sleep(1500);
        return true;
    }

    /** Cancel out of the sheet (twice when the search field holds focus); true once it is gone. */
    public boolean cancelCopyData() {
        for (int i = 0; i < 3 && isCopyDataSheetOpen(); i++) {
            SnapNode c = snapshot().stream().filter(n -> n.visible && "Button".equals(n.type) && n.is("Cancel") && n.y < 140)
                    .findFirst().orElse(null);
            if (c == null) break;
            driver.executeScript("mobile: tap", Map.of("x", c.centerX(), "y", c.centerY()));
            sleep(1200);
        }
        return !isCopyDataSheetOpen();
    }

    // ═══════════════ v1.69 Add Form / fill / delete (ZP-3928 §4 'EG Forms on every work order type') ═══════════════
    // Live 1.69 (2026-10-05): long-press an 'Assets in Room' row → menu (View Full Asset Page · Add Issue · Forms (N) ·
    // Add Form · Manage Services · Add IR Photos · Collect AF Data · Collect COM Data · Link Task · Remove from Work
    // Order). 'Add Form' → sheet 'Add Procedure' with tabs Procedure / Standalone Form; choosing a standalone form
    // CREATES the instance and opens 'Edit Form' (Close · Sections · Save Draft · Submit); the row badge grows by 1.
    // Expanded rows show chevron 'Go Down' instead of 'Forward'. Instance long-press 'Delete' → Sheet
    // 'Delete this form?' (Delete / Cancel).

    public static final String ADD_FORM_MENU_ITEM = "Add Form";
    public static final String ADD_PROCEDURE_TITLE = "Add Procedure";
    public static final String EDIT_FORM_TITLE = "Edit Form";
    private static final java.util.regex.Pattern ASSET_ROW =
            java.util.regex.Pattern.compile("^(.+?), (.+?)(?:, (\\d+))?$");
    private static final Set<String> ADD_SHEET_CHROME = Set.of("Procedure", "Standalone Form", "Cancel");

    /** Asset rows in 'Assets in Room' (with or without a form badge), top to bottom. */
    public List<SnapNode> assetRows() {
        List<SnapNode> out = new ArrayList<>();
        for (SnapNode n : snapshot()) {
            if (n.visible && "Button".equals(n.type) && n.x >= 40 && n.w > 200 && n.y > 180 && n.y < 760
                    && ASSET_ROW.matcher(n.label).matches() && !n.label.contains(" › ")) out.add(n);
        }
        out.sort(java.util.Comparator.comparingInt(n -> n.y));
        return out;
    }

    /** Form badge of the row whose name is {@code assetName}; 0 when the row shows none; -1 when the row is gone. */
    public int assetRowFormCount(String assetName) {
        for (SnapNode n : assetRows()) {
            java.util.regex.Matcher m = ASSET_ROW.matcher(n.label);
            if (m.matches() && m.group(1).equals(assetName)) return m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        }
        return -1;
    }

    public static String assetNameOf(SnapNode row) {
        java.util.regex.Matcher m = ASSET_ROW.matcher(row.label);
        return m.matches() ? m.group(1) : row.label;
    }

    private SnapNode rowNamed(String assetName) {
        return assetRows().stream().filter(n -> assetName.equals(assetNameOf(n))).findFirst().orElse(null);
    }

    /** Long-press the asset row; returns the context-menu item labels (empty when no menu appeared). */
    public List<String> openAssetRowMenu(String assetName) {
        SnapNode row = rowNamed(assetName);
        if (row == null || !longPressAt(row.centerX(), row.centerY(), 1.2)) return List.of();
        List<String> items = new ArrayList<>();
        com.egalvanic.utils.Waits.until(() -> {
            items.clear();
            for (SnapNode n : snapshot()) {
                if (n.visible && "Button".equals(n.type) && n.w >= 200 && n.w <= 300 && n.x > 40 && n.x < 120) items.add(n.label);
            }
            return items.contains(ADD_FORM_MENU_ITEM) || items.size() >= 3;
        }, 5_000, 400);
        System.out.println("📋 asset menu for '" + assetName + "': " + items);
        return items;
    }

    /** Press a visible button by exact label (coordinates); true when it was on screen. */
    public boolean pressVisibleButton(String label) {
        SnapNode b = snapshot().stream().filter(n -> n.visible && "Button".equals(n.type) && n.is(label)).findFirst().orElse(null);
        if (b == null) return false;
        driver.executeScript("mobile: tap", Map.of("x", b.centerX(), "y", b.centerY()));
        return true;
    }

    public boolean isAddProcedureSheetOpen() {
        return snapshot().stream().anyMatch(n -> n.visible && "StaticText".equals(n.type) && n.is(ADD_PROCEDURE_TITLE));
    }

    /** Selectable form options on the current Add Procedure tab. */
    public List<String> addProcedureOptions() {
        List<String> out = new ArrayList<>();
        for (SnapNode n : snapshot()) {
            if (n.visible && "Button".equals(n.type) && n.y > 180 && n.w > 300 && !ADD_SHEET_CHROME.contains(n.label)) out.add(n.label);
        }
        return out;
    }

    /**
     * On the Add Procedure sheet: prefer the Standalone Form tab ("FOR THIS ASSET" first), else the Procedure tab;
     * choose the first option. Returns the chosen form TITLE once 'Edit Form' is open, else null.
     */
    public String chooseFirstAddableForm() {
        List<String> opts = List.of();
        if (pressVisibleButton("Standalone Form")) { sleep(1500); opts = addProcedureOptions(); }
        if (opts.isEmpty() && pressVisibleButton("Procedure")) { sleep(1500); opts = addProcedureOptions(); }
        if (opts.isEmpty()) return null;
        // Prefer catalogue forms over ad-hoc library entries named 'Test…' (e.g. 'Test_26_06_02' has no input
        // fields at all — it cannot demonstrate "fillable"; TC_EGF_02 2026-10-05).
        String option = opts.stream().filter(o -> !o.matches("(?i)^test[\\s_-].*")).findFirst().orElse(null);
        if (option == null) { System.out.println("ℹ️ only ad-hoc 'Test…' forms offered: " + opts); return null; }
        String title = option.contains(", Applies to:") ? option.substring(0, option.indexOf(", Applies to:")) : option;
        System.out.println("➕ adding form '" + title + "'");
        if (!pressVisibleButton(option)) return null;
        return com.egalvanic.utils.Waits.until(this::isEditFormOpen, 10_000, 600) ? title : null;
    }

    public boolean isEditFormOpen() {
        return snapshot().stream().anyMatch(n -> n.visible && "StaticText".equals(n.type) && n.is(EDIT_FORM_TITLE));
    }

    /**
     * FILL: type {@code value} into the first editable text field of the open form and return what the field reads
     * back; when the form has no text field, set the first step Result to 'Pass' and return its read-back.
     * Null when the form offers nothing fillable.
     */
    public String fillFirstField(String value) {
        try {
            List<WebElement> fields = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "visible == 1 AND (type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeTextView') AND rect.y > 150 AND rect.y < 760")));
            if (!fields.isEmpty()) {
                WebElement f = fields.get(0);
                pressCenter(f);
                sleep(500);
                new org.openqa.selenium.interactions.Actions(driver).sendKeys(value).perform();
                sleep(800);
                String read = f.getAttribute("value");
                System.out.println("✍️ typed '" + value + "' → field reads '" + read + "'");
                return read;
            }
        } catch (Exception e) {
            System.out.println("⚠️ fillFirstField text: " + e.getMessage());
        }
        if (stepCount() > 0 && setStepResult(0, "Pass")) return stepResult(0);
        // Toggle-only forms: flip the first switch and report its new state ('1'/'0').
        try {
            List<WebElement> sw = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "visible == 1 AND type == 'XCUIElementTypeSwitch' AND rect.y > 150 AND rect.y < 760")));
            if (!sw.isEmpty()) {
                String was = sw.get(0).getAttribute("value");
                pressCenter(sw.get(0));
                sleep(800);
                String now = sw.get(0).getAttribute("value");
                System.out.println("✍️ toggled first switch " + was + " → " + now);
                return was != null && !was.equals(now) ? "Pass" : null;
            }
        } catch (Exception e) {
            System.out.println("⚠️ fillFirstField switch: " + e.getMessage());
        }
        return null;
    }

    /** Close the Edit Form screen (discarding unsaved typing when asked); true once back on 'Assets in Room'. */
    public boolean closeEditForm() {
        try { driver.executeScript("mobile: hideKeyboard"); } catch (Exception ignored) { }
        pressVisibleButton("Close");
        sleep(1500);
        for (String d : new String[]{"Discard", "Discard Changes", "Don't Save", "Leave"}) {
            if (pressVisibleButton(d)) { sleep(1500); break; }
        }
        return com.egalvanic.utils.Waits.until(this::isAssetsInRoomOpen, 8_000, 600);
    }

    /** Delete the form instance {@code title} under {@code assetName} (expand → long-press → Delete → confirm). */
    public boolean deleteFormInstance(String assetName, String title) {
        SnapNode row = rowNamed(assetName);
        if (row == null) return false;
        long before = formInstancesOf(assetName).stream().filter(title::equals).count();
        if (before == 0) { System.out.println("⚠️ deleteFormInstance: '" + title + "' not listed under '" + assetName + "'"); return false; }
        SnapNode t = snapshot().stream().filter(n -> n.visible && "StaticText".equals(n.type) && n.is(title) && n.y > row.y)
                .min(java.util.Comparator.comparingInt(n -> n.y)).orElse(null);
        if (t == null || !longPressAt(t.centerX(), t.centerY(), 1.2)) return false;
        sleep(1200);
        final boolean[] ok = {false};
        withAlertsManual(() -> {
            if (!pressVisibleButton("Delete")) return;
            sleep(1500);
            // Confirmation Sheet 'Delete this form?' — its destructive button is the lower 'Delete'.
            SnapNode confirm = snapshot().stream().filter(n -> n.visible && "Button".equals(n.type) && n.is("Delete") && n.y > 500)
                    .findFirst().orElse(null);
            if (confirm != null) driver.executeScript("mobile: tap", Map.of("x", confirm.centerX(), "y", confirm.centerY()));
            ok[0] = true;
        });
        sleep(2500);
        if (!ok[0]) return false;
        // VERIFY — a 'Delete' press that hit the wrong control used to report success and leak the form
        // (two 'Test_26_06_02' instances stayed live on QA-WT02, 2026-10-05).
        long after = formInstancesOf(assetName).stream().filter(title::equals).count();
        System.out.println("🗑️ '" + title + "' on '" + assetName + "': " + before + " → " + after);
        return after == before - 1;
    }

    /**
     * Form-instance titles under the asset (expanding its row when collapsed). Live 1.69: the row badge only renders
     * on some work types (NETA rows read '<name>, <Class>, N'; Arc Flash rows stay '<name>, <Class>' even with a form
     * attached), so the instance list — not the badge — is the persistence oracle.
     */
    public List<String> formInstancesOf(String assetName) {
        SnapNode row = rowNamed(assetName);
        if (row == null) return List.of();
        boolean collapsed = snapshot().stream().anyMatch(n -> n.visible && "Button".equals(n.type)
                && n.is("Forward") && Math.abs(n.y - row.y) < 12);
        if (collapsed) return expandAssetFormInstances(row);
        return formInstanceTitlesBelow(row);
    }

    /**
     * Cheap, bounded check from the session Assets tab: does any room row advertise assets ('…, N assets')?
     * Swipes at most 6 times over ONE snapshot each — never enters rooms (the enter-and-back-out walk wedges WDA
     * on fixtures whose rooms are all empty, TC_EGF_14 2026-10-05).
     */
    public boolean hasRoomAdvertisingAssets() {
        switchToAllRoomsIfOffered();
        for (int i = 0; i < 7; i++) {
            if (snapshot().stream().anyMatch(n -> n.visible && "Button".equals(n.type)
                    && n.label.matches(".*, [1-9]\\d* assets?$"))) return true;
            if (i < 6) { swipe("up"); sleep(500); }
        }
        for (int i = 0; i < 6; i++) swipe("down");
        return false;
    }

    /** Room rows advertising assets ('…, N assets'), collected over a bounded sweep; leaves the list at the top. */
    public List<String> roomsAdvertisingAssets() {
        switchToAllRoomsIfOffered();
        Set<String> out = new LinkedHashSet<>();
        for (int i = 0; i < 7; i++) {
            for (SnapNode n : snapshot()) {
                if (n.visible && "Button".equals(n.type) && n.label.matches(".*, [1-9]\\d* assets?$")) out.add(n.label);
            }
            if (i < 6) { swipe("up"); sleep(400); }
        }
        for (int i = 0; i < 7; i++) swipe("down");
        return new ArrayList<>(out);
    }

    /**
     * Open the room row with exactly this label; true once 'Assets in Room' is up AND its breadcrumb names the room.
     * 1.69 room lists are long (a fixture room sat at y=3810) and a tap right after a swipe lands on whatever row the
     * momentum carried under the finger — an empty room (TC_EGF_01 read 0 asset rows, 2026-10-05). So: type the room
     * name into the list's 'Search building, floor, or room' field first; swipe only as a fallback, with a settle.
     */
    public boolean openRoomByLabel(String label) {
        String room = roomNameOf(label);
        SnapNode search = snapshot().stream().filter(n -> n.visible && "SearchField".equals(n.type)).findFirst().orElse(null);
        if (search != null && room != null) {
            driver.executeScript("mobile: tap", Map.of("x", search.centerX(), "y", search.centerY()));
            sleep(500);
            StringBuilder keys = new StringBuilder();
            for (int i = 0; i < 40; i++) keys.append(org.openqa.selenium.Keys.BACK_SPACE);
            keys.append(room);
            new org.openqa.selenium.interactions.Actions(driver).sendKeys(keys.toString()).perform();
            sleep(1500);
            try { driver.executeScript("mobile: hideKeyboard"); } catch (Exception ignored) { }
            sleep(500);
        }
        for (int i = 0; i < 8; i++) {
            SnapNode first = rowLabelled(label);
            SnapNode r = null;
            if (first != null) {
                // Only tap a row whose position has SETTLED: keyboard hide / scroll momentum move the list under
                // the finger (TC_EGF_01 opened 'Optional Notes Room_51' for '…Room_21', 2026-10-05).
                sleep(900);
                SnapNode again = rowLabelled(label);
                if (again != null && Math.abs(again.y - first.y) <= 2) r = again;
                else { i--; if (i < -4) break; continue; }
            }
            if (r != null) {
                driver.executeScript("mobile: tap", Map.of("x", r.centerX(), "y", r.centerY()));
                boolean open = com.egalvanic.utils.Waits.until(this::isAssetsInRoomOpen, 8_000, 500);
                boolean right = open && (room == null || snapshot().stream().anyMatch(n -> n.visible
                        && "StaticText".equals(n.type) && n.label.contains(room)));
                if (open && !right) System.out.println("⚠️ opened a room whose breadcrumb does not name '" + room + "'");
                return right;
            }
            swipe("up"); sleep(1200);   // let the scroll settle before the next snapshot
        }
        return false;
    }

    private SnapNode rowLabelled(String label) {
        return snapshot().stream().filter(n -> n.visible && "Button".equals(n.type) && label.equals(n.label)
                && n.y > 120 && n.y < 760).findFirst().orElse(null);
    }

    /** '<bldg> › <floor>, <room>, N assets' → '<room>'. */
    static String roomNameOf(String label) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^.*?, (.+), [1-9]\\d* assets?$").matcher(label);
        return m.matches() ? m.group(1) : null;
    }

    /** From 'Assets in Room' back to the session's room list. */
    public boolean backToRoomList() {
        if (!pressVisibleButton("Back")) pressVisibleButton("BackButton");
        return com.egalvanic.utils.Waits.until(() -> !isAssetsInRoomOpen(), 6_000, 500);
    }

    // ── fixture self-provisioning (1.69): link an existing asset into a session room ──────────────────────────
    // Live 2026-10-05 (QA-WT12): an empty fixture's room reads 'No Active Assets — N inactive assets here — tap
    // "Link Existing" to pull them in'; '+' (Add) → New Asset / Link Existing Asset / Photo Walkthrough / Quick Count;
    // 'Link Existing Assets' sheet lists the room's assets ('<name>, <Class>') with a bottom 'Add (N)'; then a
    // 'Set Up Forms' sheet (Not Now / Create Forms (N)) — 'Not Now' so nothing else is generated.

    /** Open the room whose row names {@code roomName} (with or without an asset count), settle-checked. */
    public boolean openRoomNamed(String roomName) {
        java.util.function.Predicate<String> match = l -> l.endsWith(", " + roomName) || l.contains(", " + roomName + ", ");
        // Filter first when the list offers a search field (the row then sits at the top, no long swipe walk).
        SnapNode search = snapshot().stream().filter(n -> n.visible && ("SearchField".equals(n.type)
                || "TextField".equals(n.type) && n.y < 200)).findFirst().orElse(null);
        if (search != null) {
            driver.executeScript("mobile: tap", Map.of("x", search.centerX(), "y", search.centerY()));
            sleep(500);
            StringBuilder keys = new StringBuilder();
            for (int k = 0; k < 40; k++) keys.append(org.openqa.selenium.Keys.BACK_SPACE);
            keys.append(roomName);
            new org.openqa.selenium.interactions.Actions(driver).sendKeys(keys.toString()).perform();
            sleep(1500);
            try { driver.executeScript("mobile: hideKeyboard"); } catch (Exception ignored) { }
            sleep(800);
        }
        int unstable = 0;
        for (int i = 0; i < 25; i++) {
            SnapNode first = snapshot().stream().filter(n -> n.visible && "Button".equals(n.type) && match.test(n.label)
                    && n.y > 140 && n.y < 680).findFirst().orElse(null);
            if (first != null) {
                sleep(900);
                SnapNode again = snapshot().stream().filter(n -> n.visible && "Button".equals(n.type) && n.label.equals(first.label))
                        .findFirst().orElse(null);
                if (again != null && Math.abs(again.y - first.y) <= 2) {
                    driver.executeScript("mobile: tap", Map.of("x", again.centerX(), "y", again.centerY()));
                    return com.egalvanic.utils.Waits.until(this::isAssetsInRoomOpen, 8_000, 500)
                            && snapshot().stream().anyMatch(n -> n.visible && "StaticText".equals(n.type) && n.label.contains(roomName));
                }
                if (++unstable < 4) { i--; continue; }   // still moving — re-check without swiping (bounded)
            }
            unstable = 0;
            swipe("up");
            sleep(1500);
        }
        System.out.println("⚠️ openRoomNamed: no settled row naming '" + roomName + "' (search field " + (search != null) + ")");
        return false;
    }

    /** In 'Assets in Room': + › Link Existing Asset › pick {@code assetName} › Add (N) › Not Now. True once its row is listed. */
    public boolean linkExistingAsset(String assetName) {
        SnapNode add = snapshot().stream().filter(n -> n.visible && "Button".equals(n.type) && n.is("Add") && n.y > 600)
                .findFirst().orElse(null);
        if (add == null) return false;
        driver.executeScript("mobile: tap", Map.of("x", add.centerX(), "y", add.centerY()));
        sleep(1500);
        if (!pressVisibleButton("Link Existing Asset")) return false;
        if (!com.egalvanic.utils.Waits.until(() -> snapshot().stream().anyMatch(n -> n.visible && n.is("Link Existing Assets")), 8_000, 500)) return false;
        SnapNode row = snapshot().stream().filter(n -> n.visible && "Button".equals(n.type) && n.label.startsWith(assetName + ", "))
                .findFirst().orElse(null);
        if (row == null) { pressVisibleButton("Cancel"); return false; }
        driver.executeScript("mobile: tap", Map.of("x", row.centerX(), "y", row.centerY()));
        sleep(1000);
        SnapNode confirm = snapshot().stream().filter(n -> n.visible && "Button".equals(n.type) && n.label.startsWith("Add ("))
                .findFirst().orElse(null);
        if (confirm == null) { pressVisibleButton("Cancel"); return false; }
        driver.executeScript("mobile: tap", Map.of("x", confirm.centerX(), "y", confirm.centerY()));
        com.egalvanic.utils.Waits.until(() -> pressVisibleButton("Not Now") || assetRowFormCount(assetName) >= 0, 10_000, 800);
        sleep(1500);
        boolean linked = com.egalvanic.utils.Waits.until(() -> assetRowFormCount(assetName) >= 0, 8_000, 800);
        System.out.println((linked ? "🔗 linked '" : "⚠️ could not link '") + assetName + "' into the room");
        return linked;
    }
}
