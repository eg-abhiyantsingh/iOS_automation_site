package com.egalvanic.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ZP-3914 (iOS 1.59) Account Manager experience: Site Walks list, Walk wizard, mode tray, walk detail, Count grid,
 * rooms, offline queue. Every string below was captured live on the 1.69 QA build (probe 2026-10-21) and matches
 * AppStrings.SiteWalks in the 1.70 source.
 *
 *  - List: nav 'Site Walks'; top-left network menu (label 'Wi-Fi' online / 'Wi-Fi Off' offline; when records are
 *    queued its Button label is the queue COUNT, e.g. '2'); top-right 'Refresh' (disabled offline); 'New Site Walk';
 *    month headers ('October 2026'); rows '<DAY>, <dd>, <title>, <services | No site yet>[, JOURNAL]' whose status
 *    icon is 'Arrow Up Circle' (not yet synced) · 'circle.dotted' (open) · 'Selected' (complete, row value="1").
 *    Row order inside a day is NOT stable (it changed after a sync) — tests identify their walk by backend id.
 *  - Network menu: 'Go Offline' / 'Go Online', plus 'Sync N records' only while something is queued.
 *  - Wizard (sheet): nav 'Customer' + 'Step 1 of 3', 'Cancel', SearchField 'Search customers', 'Choose a customer',
 *    rows '<customer>, N site(s)', 'New Customer', 'Or defer both customer and site to the office.',
 *    'Assign customer and site later, Start walking now, attach both back at the office' (jumps to Step 3).
 *    Step 3: nav 'Services' + 'Step 3 of 3', back button 'Customer', service rows (value="1" when picked),
 *    'Start Walk' disabled until a service is picked.
 *  - Mode tray (half sheet): 'How will you walk this site?' / 'Set once, here. It doesn't change mid-walk.',
 *    'Count, STRUCTURED, …' / 'Journal, FASTEST, …', 'Cancel'. Picking a mode creates the walk and opens it.
 *  - Detail: title 'Site Walk — yyyy-MM-dd' (unassigned), tag 'COUNT', 'Locations', 'Start First Room' /
 *    'Start Next Room', 'Count assets without a location', 'Complete Site Walk' (disabled + hint
 *    'count at least one asset' until something is counted); room rows '<room>, N asset(s)' (value="1").
 *  - Count grid: 'Assets' / 'Counted for the whole site' (no room) or TextFields 'Name this room' /
 *    'Add floor (optional)'; TextField 'Search assets...'; total 'None counted yet' / '1 asset' / 'N assets';
 *    class cards (Button '<class>' or '<class>, N'); expanded card rows '<subtype>' + Buttons 'Remove' (disabled at
 *    0) / 'Add' + count text; unit rows '<class> 1..N' with 'Camera'; 'Save Assets' / 'Complete Room' (disabled at 0).
 *  - Delete: long-press row → 'Delete Site Walk' → Alert 'Delete Site Walk' / 'This removes the walk and its
 *    counts.' (Delete Site Walk · Cancel) — run under {@link #withAlertsManual}.
 */
public class SiteWalkPage extends SnapPage {

    public static final String TAB = "Site Walks";
    public static final String NEW_WALK = "New Site Walk";
    public static final String LOADING = "Loading site walks…";
    public static final String WIZ_CUSTOMER = "Customer";
    public static final String WIZ_SERVICES = "Services";
    public static final String SEARCH_CUSTOMERS = "Search customers";
    /**
     * Upper bound of the network-menu band. 1.69 dropped the menu below the toolbar (items y≈100-250); v1.73 / iOS 26
     * opens it as a popover OVER the toolbar — 'Go Offline' at y≈62 (TC_SWO_01, 2026-10-30). The w 200-300 filter
     * keeps the 41-pt toolbar control itself out.
     */
    private static final int MENU_TOP = 40;
    public static final String CHOOSE_CUSTOMER = "Choose a customer";
    public static final String NEW_CUSTOMER = "New Customer";
    public static final String DEFER_FOOTER = "Or defer both customer and site to the office.";
    public static final String ASSIGN_LATER = "Assign customer and site later";
    public static final String ASSIGN_LATER_SUB = "Start walking now, attach both back at the office";
    public static final String START_WALK = "Start Walk";
    public static final String MODE_TITLE = "How will you walk this site?";
    public static final String MODE_SUBTITLE = "Set once, here. It doesn't change mid-walk.";
    public static final String MODE_COUNT = "Count, STRUCTURED";
    public static final String MODE_JOURNAL = "Journal, FASTEST";
    public static final String COUNT_TAG = "COUNT";
    public static final String LOCATIONS = "Locations";
    public static final String START_FIRST_ROOM = "Start First Room";
    public static final String START_NEXT_ROOM = "Start Next Room";
    public static final String COUNT_WITHOUT_LOCATION = "Count assets without a location";
    public static final String COMPLETE_WALK = "Complete Site Walk";
    public static final String HINT_COUNT = "count at least one asset";
    public static final String GRID_TITLE = "Assets";
    public static final String GRID_WHOLE_SITE = "Counted for the whole site";
    public static final String SEARCH_ASSETS = "Search assets...";
    public static final String NONE_COUNTED = "None counted yet";
    public static final String SAVE_ASSETS = "Save Assets";
    public static final String COMPLETE_ROOM = "Complete Room";
    public static final String ROOM_NAME = "Name this room";
    public static final String FLOOR = "Add floor (optional)";
    public static final String GO_OFFLINE = "Go Offline";
    public static final String GO_ONLINE = "Go Online";
    public static final String ICON_NOT_SYNCED = "Arrow Up Circle";
    public static final String ICON_OPEN = "circle.dotted";
    public static final String ICON_COMPLETE = "Selected";
    public static final String DELETE_WALK = "Delete Site Walk";
    public static final String DELETE_MESSAGE = "This removes the walk and its counts.";
    public static final String GRID_NO_MATCH = "No assets found";
    /** Grid actions that render as full-width Buttons like the class cards (not classes themselves). */
    public static final String ADD_ASSET_CLASS = "Add asset class";
    public static final String ADD_SUBTYPE = "Add subtype";
    public static final String SAVE_CHANGES_TITLE = "Save changes?";
    public static final String SAVE_CHANGES_MESSAGE = "You have unsaved counts or room details.";
    public static final String REOPEN = "Reopen";

    private static final Pattern ROW = Pattern.compile("^(MON|TUE|WED|THU|FRI|SAT|SUN), (\\d{1,2}), (.+)$");
    private static final Pattern STEP = Pattern.compile("^Step (\\d) of 3$");
    private static final Pattern CUSTOMER_ROW = Pattern.compile("^(.+), (\\d+) sites?$");
    private static final Pattern TOTAL = Pattern.compile("^(?:None counted yet|(\\d+) assets?)$");
    private static final Pattern SYNC_ITEM = Pattern.compile("^Sync (\\d+) records?$");

    /** A walk row on the list with its status icon. */
    public static final class WalkRow {
        public final String label, icon;
        public final boolean complete;
        public final SnapNode node;
        WalkRow(SnapNode node, String icon) { this.node = node; this.label = node.label; this.icon = icon; this.complete = node.isOn(); }
        @Override public String toString() { return label + " [" + icon + (complete ? ", complete" : "") + "]"; }
    }

    // ── list ──────────────────────────────────────────────────────────────────────────────────────────────

    public boolean isListShown() {
        return labelled("Button", NEW_WALK).isPresent() && find(n -> "NavigationBar".equals(n.type) && n.is(TAB)).isPresent();
    }

    /** Wait for the list to finish loading ('Loading site walks…' gone, 'New Site Walk' present). */
    public boolean waitForList(long ms) {
        return waitUntil(() -> isListShown() && !isTextVisible(LOADING), ms);
    }

    public List<WalkRow> walkRows() {
        List<SnapNode> s = snapshot();
        List<WalkRow> out = new ArrayList<>();
        // A row running under the bottom tab bar is only half drawn — its status icon is not in the snapshot
        // (TC_SWW_01 on v1.73 / iOS 26: the floating tab bar covered 'WALK SITE … JOURNAL', 2026-10-30). Keep
        // fully drawn rows only.
        int barTop = s.stream().filter(n -> n.visible && "TabBar".equals(n.type)).mapToInt(n -> n.y).min().orElse(Integer.MAX_VALUE);
        for (SnapNode n : s) {
            if (!n.visible || !"Button".equals(n.type) || !ROW.matcher(n.label).matches()) continue;
            if (n.y + n.h > barTop) continue;
            String icon = s.stream().filter(i -> i.visible && "Image".equals(i.type) && i.x > rx(340) && i.x < rx(380)
                            && i.y >= n.y && i.y <= n.y + n.h)
                    .map(i -> i.label.isEmpty() ? i.name : i.label).findFirst().orElse("");
            out.add(new WalkRow(n, icon));
        }
        out.sort(java.util.Comparator.comparingInt(r -> r.node.y));
        return out;
    }

    public boolean isRefreshEnabled() {
        return find(n -> "Button".equals(n.type) && n.is("Refresh") && n.y < 140).map(n -> n.enabled).orElse(false);
    }

    /** Top-left network control: its label is 'Wi-Fi' / 'Wi-Fi Off', or the queue count when records are queued. */
    private Optional<SnapNode> networkControl() {
        return find(n -> ("Button".equals(n.type) || "Other".equals(n.type)) && n.x < 40 && n.y > 40 && n.y < 110
                && (n.is("Wi-Fi") || n.is("Wi-Fi Off") || n.label.matches("\\d+")));
    }

    /** Pending-sync count on the network control (0 when it shows no count). */
    public int queueBadge() {
        return networkControl().filter(n -> n.label.matches("\\d+")).map(n -> Integer.parseInt(n.label)).orElse(0);
    }

    /** True when the toolbar shows the offline Wi-Fi icon. */
    public boolean isOfflineIcon() {
        return find(n -> "Image".equals(n.type) && n.is("Wi-Fi Off") && n.y < 120).isPresent();
    }

    /** Open the network menu; returns its item labels. */
    public List<String> openNetworkMenu() {
        Optional<SnapNode> c = networkControl();
        if (c.isEmpty()) return List.of();
        tapNode(c.get());
        List<String> items = new ArrayList<>();
        waitUntil(() -> {
            items.clear();
            for (SnapNode n : findAll(x -> "Button".equals(x.type) && x.y > MENU_TOP && x.y < 260 && x.w > 200 && x.w < 300)) items.add(n.label);
            return !items.isEmpty();
        }, 4_000);
        return items;
    }

    /** Press an item of the open network menu. */
    public boolean pressMenuItem(String label) {
        Optional<SnapNode> b = find(n -> "Button".equals(n.type) && n.is(label) && n.y > MENU_TOP && n.y < 260);
        b.ifPresent(this::tapNode);
        if (b.isPresent()) sleep(600);
        return b.isPresent();
    }

    /** Close the network menu without choosing (only taps when the menu is actually up). */
    public void closeMenu() {
        boolean open = find(n -> "Button".equals(n.type) && n.y > MENU_TOP && n.y < 260 && n.w > 200 && n.w < 300
                && (n.is(GO_OFFLINE) || n.is(GO_ONLINE) || SYNC_ITEM.matcher(n.label).matches())).isPresent();
        if (open) {
            tapPoint(220, 600);
            sleep(700);
        }
    }

    /**
     * Offline? With nothing queued the toolbar icon says so ('Wi-Fi Off'); with a queue the control shows the COUNT
     * instead and no offline icon is exposed (TC_SWO_02 first run, 2026-10-21: still offline, but the icon check said
     * online) — then the menu decides: it offers 'Go Online' only while offline.
     */
    public boolean isOffline() {
        if (isOfflineIcon()) return true;
        if (queueBadge() == 0) return false;
        List<String> items = openNetworkMenu();
        closeMenu();
        return items.contains(GO_ONLINE);
    }

    /** Switch via the menu, then confirm by reading the menu again (it must now offer the opposite item). */
    private boolean switchNetwork(String press, String expectAfter) {
        List<String> items = openNetworkMenu();
        if (!items.contains(press)) {
            closeMenu();
            return items.contains(expectAfter);   // already in the wanted mode
        }
        if (!pressMenuItem(press)) return false;
        return waitUntil(() -> {
            List<String> now = openNetworkMenu();
            closeMenu();
            return now.contains(expectAfter);
        }, 10_000);
    }

    public boolean goOffline() {
        return switchNetwork(GO_OFFLINE, GO_ONLINE);
    }

    public boolean goOnline() {
        return switchNetwork(GO_ONLINE, GO_OFFLINE);
    }

    /**
     * Menu › 'Sync N records'; returns N (or -1 when the item is not offered). Waits until the menu no longer offers a
     * sync (the queue drained) — the badge alone is not read while the menu covers the control.
     */
    public int syncQueued() {
        List<String> items = openNetworkMenu();
        for (String i : items) {
            Matcher m = SYNC_ITEM.matcher(i);
            if (m.matches()) {
                pressMenuItem(i);
                waitUntil(() -> {
                    List<String> now = openNetworkMenu();
                    closeMenu();
                    return now.stream().noneMatch(x -> SYNC_ITEM.matcher(x).matches()) && queueBadge() == 0;
                }, 45_000);
                return Integer.parseInt(m.group(1));
            }
        }
        closeMenu();
        return -1;
    }

    // ── wizard ─────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 'New Site Walk' → wizard Step 1, returned only once the sheet has stopped sliding in: 'Step 1 of 3' is in the
     * tree from the first animation frame, and a tap on the search field mid-slide lands on nothing (TC_SWW_04/05
     * first run, 2026-10-21).
     */
    public boolean openWizard() {
        if (!pressButton(NEW_WALK)) return false;
        if (!waitUntil(() -> currentStep() == 1, 8_000)) return false;
        return settled(n -> "SearchField".equals(n.type) && n.text().startsWith(SEARCH_CUSTOMERS)).isPresent()
                || currentStep() == 1;
    }

    /** 1..3, or 0 when no wizard step header is on screen. */
    public int currentStep() {
        for (SnapNode n : findAll(x -> "StaticText".equals(x.type))) {
            Matcher m = STEP.matcher(n.label);
            if (m.matches()) return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    /**
     * Navigation-bar title of the current wizard step ('Customer', the chosen customer's name on Step 2, 'Services').
     * The sheet's NavigationBar has NO label attribute — its title is only in 'name' (probe 2026-10-21); the list's
     * own bar ('Site Walks') stays in the tree behind the sheet with visible="false".
     */
    public String wizardTitle() {
        return find(n -> "NavigationBar".equals(n.type) && n.y > 50 && n.y < 100).map(SnapNode::text).orElse("");
    }

    public List<String> customerRows() {
        List<String> out = new ArrayList<>();
        for (SnapNode n : findAll(x -> "Button".equals(x.type) && x.w > 300)) {
            if (CUSTOMER_ROW.matcher(n.label).matches()) out.add(n.label);
        }
        return out;
    }

    /**
     * Type into 'Search customers' and confirm the field took it. The field jumps up into the nav bar when it gains
     * focus, so the tap is made on the settled field and typing waits for the keyboard; false when the field never
     * reads {@code text}.
     */
    public boolean searchCustomers(String text) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            Optional<SnapNode> f = settled(n -> "SearchField".equals(n.type) && n.text().startsWith(SEARCH_CUSTOMERS));
            if (f.isEmpty()) {
                System.out.println("⚠️ searchCustomers: no 'Search customers' field on screen");
                return false;
            }
            if (!isKeyboardUp()) {
                tapNode(f.get());
                waitUntil(this::isKeyboardUp, 3_000);
            }
            replaceFocusedText(text, 30);
            boolean took = waitUntil(() -> find(n -> "SearchField".equals(n.type) && text.equals(n.value)).isPresent(), 3_000);
            if (took) {
                sleep(800);   // the filtered list re-renders after the value lands
                return true;
            }
            System.out.println("⚠️ searchCustomers attempt " + attempt + ": field did not take '" + text + "'");
        }
        return false;
    }

    /** Current text of the customer search field ('' when empty — WDA then reports the placeholder as value). */
    public String customerSearchText() {
        return find(n -> "SearchField".equals(n.type) && n.text().startsWith(SEARCH_CUSTOMERS))
                .map(n -> n.value.equals(n.placeholder) ? "" : n.value).orElse("");
    }

    /** Dismiss the active customer search (its own 'Cancel', right of the field) so the list is unfiltered again. */
    public boolean cancelCustomerSearch() {
        Optional<SnapNode> c = searchDismissButton();
        if (c.isEmpty()) return false;
        tapNode(c.get());
        return waitUntil(() -> customerSearchText().isEmpty() && !isKeyboardUp(), 4_000);
    }

    /**
     * The control that ENDS an active customer search. Up to 1.69 the search took over the nav bar and ended with a
     * top-right 'Cancel'; on v1.73 / iOS 26 the field docks at the BOTTOM above the keyboard with a round '✕' button
     * to its right (TC_SWW_02, 2026-10-30). Either way it is the Button just right of the search field on the same
     * row — the in-field clear button sits inside the field's frame and is excluded.
     */
    private Optional<SnapNode> searchDismissButton() {
        Optional<SnapNode> top = find(n -> "Button".equals(n.type) && n.is("Cancel") && n.y < 140 && n.x > rx(300));
        if (top.isPresent()) return top;
        List<SnapNode> s = snapshot();
        Optional<SnapNode> field = s.stream().filter(n -> n.visible && ("SearchField".equals(n.type) || "TextField".equals(n.type))
                && (n.value.contains(SEARCH_CUSTOMERS) || n.placeholder.contains(SEARCH_CUSTOMERS) || n.label.contains(SEARCH_CUSTOMERS)
                    || n.y > by(500))).max(java.util.Comparator.comparingInt(n -> n.y));
        if (field.isEmpty()) return Optional.empty();
        SnapNode f = field.get();
        return s.stream().filter(n -> n.visible && "Button".equals(n.type) && n.x >= f.x + f.w - 5
                        && Math.abs(n.centerY() - f.centerY()) < 30 && !n.is("Clear text"))
                .min(java.util.Comparator.comparingInt(n -> n.x));
    }

    /** A customer search is in progress (its dismiss control is on screen). */
    public boolean isCustomerSearchActive() {
        return searchDismissButton().isPresent();
    }

    public boolean isAssignLaterOffered() {
        return find(n -> "Button".equals(n.type) && n.label.startsWith(ASSIGN_LATER + ", ")).isPresent();
    }

    /** 'Assign customer and site later' → Step 3 (Services). */
    public boolean chooseAssignLater() {
        if (find(n -> "Button".equals(n.type) && n.label.startsWith(ASSIGN_LATER + ", ")).isEmpty()) {
            // 1.69: the row sits under a long customer list (1.70 / ZP-4674 moves it above); a no-match search
            // brings it to the top (probe 2026-10-21).
            if (!searchCustomers("zzqq-no-customer")) return false;
        }
        Optional<SnapNode> row = settled(n -> "Button".equals(n.type) && n.label.startsWith(ASSIGN_LATER + ", "));
        if (row.isEmpty()) {
            System.out.println("⚠️ chooseAssignLater: row not found after search");
            return false;
        }
        tapNode(row.get());
        boolean ok = waitUntil(() -> currentStep() == 3, 8_000);
        if (!ok) System.out.println("⚠️ chooseAssignLater: still on step " + currentStep() + " after the tap");
        return ok;
    }

    // ── wizard step 2 (a customer's sites) ─────────────────────────────────────────────────────────────────

    public static final String CHOOSE_SITE = "Choose a site";
    public static final String SITE_LATER = "Assign a site later";
    public static final String WIZ_SITE = "Site";
    public static final String NO_SITES_FOR_CUSTOMER =
            "This customer has no sites yet — start the walk without one and attach it later.";

    /** Site rows on Step 2 ('<site>, <address>' or just '<site>'), top to bottom. */
    public List<String> siteRows() {
        return findAll(n -> "Button".equals(n.type) && n.w > 300 && n.y > 150 && !n.is(SITE_LATER))
                .stream().map(SnapNode::text).toList();
    }

    public boolean isNoSitesMessageShown() {
        return labelled("StaticText", NO_SITES_FOR_CUSTOMER).isPresent();
    }

    /** Step 2 'Assign a site later' → Step 3. */
    public boolean chooseSiteLater() {
        Optional<SnapNode> b = settled(n -> "Button".equals(n.type) && n.is(SITE_LATER));
        if (b.isEmpty()) return false;
        tapNode(b.get());
        return waitUntil(() -> currentStep() == 3, 8_000);
    }

    /**
     * Tap the first site row on Step 2; returns the site's NAME (the row's first text line — the second line is the
     * address, so the row label alone can't be split safely) once Step 3 is up, else null.
     */
    public String chooseFirstSite() {
        Optional<SnapNode> row = settled(n -> "Button".equals(n.type) && n.w > 300 && n.y > 150 && !n.is(SITE_LATER));
        if (row.isEmpty()) return null;
        SnapNode r = row.get();
        String name = find(n -> "StaticText".equals(n.type) && n.y >= r.y && n.y < r.y + r.h && n.x > r.x + 30)
                .map(SnapNode::text).orElse(null);
        tapNode(r);
        return waitUntil(() -> currentStep() == 3, 8_000) ? name : null;
    }

    /** Customer rows matching '<name>, N site(s)' with N in [min, max] — the list is long and only partly rendered. */
    public Optional<String> customerRowWithSites(int min, int max) {
        for (String row : customerRows()) {
            Matcher m = CUSTOMER_ROW.matcher(row);
            if (m.matches()) {
                int n = Integer.parseInt(m.group(2));
                if (n >= min && n <= max) return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    /** Tap a customer row by its full label; true once Step 2 is up. */
    public boolean chooseCustomer(String rowLabel) {
        Optional<SnapNode> row = settled(n -> "Button".equals(n.type) && n.is(rowLabel));
        if (row.isEmpty()) return false;
        tapNode(row.get());
        return waitUntil(() -> currentStep() == 2, 8_000);
    }

    /** Tap the first customer that owns at least one site; returns its row label once Step 2 is up, else null. */
    /** The customer list loads after the sheet opens (ProgressView overlay); true once at least one row is up. */
    public boolean waitForCustomers(long ms) {
        return waitUntil(() -> !customerRows().isEmpty(), ms);
    }

    public String chooseFirstCustomerWithSites() {
        waitForCustomers(15_000);
        Optional<String> row = customerRowWithSites(1, Integer.MAX_VALUE);
        if (row.isEmpty()) {
            System.out.println("⚠️ chooseFirstCustomerWithSites: no '<name>, N site(s)' row with N>0 among " + customerRows());
            return null;
        }
        return chooseCustomer(row.get()) ? row.get() : null;
    }

    /** Navigation back one wizard step (the back button carries the previous step's title). */
    public boolean wizardBack() {
        Optional<SnapNode> back = find(n -> "Button".equals(n.type) && n.y > 60 && n.y < 110 && n.x < 30 && n.w < 220 && !n.is("Cancel"));
        if (back.isEmpty()) return false;
        int step = currentStep();
        tapNode(back.get());
        return waitUntil(() -> currentStep() != step, 6_000);
    }

    /**
     * Close the wizard from any step. Only Step 1 has 'Cancel' (top-left); Steps 2-3 have a back chevron, and an
     * active customer search replaces the nav bar with its own 'Cancel' (top-right) — so: drop the mode tray, end
     * the search, walk back to Step 1, then Cancel.
     */
    public boolean cancelWizard() {
        if (isModeTrayShown()) cancelModeTray();
        for (int i = 0; i < 4 && currentStep() > 1; i++) {
            if (!wizardBack()) break;
        }
        if (isCustomerSearchActive()) cancelCustomerSearch();
        Optional<SnapNode> cancel = settled(n -> "Button".equals(n.type) && n.is("Cancel") && n.y < 140 && n.x < 100);
        if (cancel.isEmpty()) {
            System.out.println("⚠️ cancelWizard: no wizard 'Cancel' (step " + currentStep() + ")");
            return false;
        }
        tapNode(cancel.get());
        return waitUntil(this::isListShown, 8_000);
    }

    /** Service rows on Step 3 (label → picked). */
    public java.util.Map<String, Boolean> services() {
        java.util.Map<String, Boolean> out = new java.util.LinkedHashMap<>();
        for (SnapNode n : findAll(x -> "Button".equals(x.type) && x.w > 300 && x.y > 150 && !x.is(START_WALK))) {
            out.put(n.label, n.isOn());
        }
        return out;
    }

    public boolean pickService(String name) {
        Optional<SnapNode> row = find(n -> "Button".equals(n.type) && n.is(name) && n.w > 300);
        if (row.isEmpty()) return false;
        tapNode(row.get());
        return waitUntil(() -> Boolean.TRUE.equals(services().get(name)), 4_000);
    }

    /** Tap a service row without waiting for a state (used to deselect). */
    public boolean tapService(String name) {
        Optional<SnapNode> row = find(n -> "Button".equals(n.type) && n.is(name) && n.w > 300);
        row.ifPresent(this::tapNode);
        return row.isPresent();
    }

    public boolean isStartWalkEnabled() {
        return labelled("Button", START_WALK).map(n -> n.enabled).orElse(false);
    }

    public boolean pressStartWalk() {
        if (!pressButton(START_WALK)) return false;
        return waitUntil(this::isModeTrayShown, 8_000);
    }

    public boolean isModeTrayShown() {
        return labelled("StaticText", MODE_TITLE).isPresent();
    }

    public List<String> modeOptions() {
        return findAll(n -> "Button".equals(n.type) && (n.label.startsWith(MODE_COUNT) || n.label.startsWith(MODE_JOURNAL)))
                .stream().map(n -> n.label).toList();
    }

    /** Pick Count; true once the new walk's detail (tag 'COUNT') is open. */
    public boolean chooseCountMode() {
        Optional<SnapNode> c = find(n -> "Button".equals(n.type) && n.label.startsWith(MODE_COUNT));
        if (c.isEmpty()) return false;
        tapNode(c.get());
        return waitUntil(this::isCountWalkDetailShown, 15_000);
    }

    public boolean cancelModeTray() {
        Optional<SnapNode> c = find(n -> "Button".equals(n.type) && n.is("Cancel") && n.y > by(700));
        if (c.isEmpty()) return false;
        tapNode(c.get());
        return waitUntil(() -> !isModeTrayShown(), 6_000);
    }

    /**
     * Full wizard for an unassigned Count walk: New Site Walk → Assign later → first service → Start Walk → Count.
     * Returns the picked service, or null when any step failed.
     */
    public String createUnassignedCountWalk() {
        if (!openWizard() || !chooseAssignLater()) return null;
        String service = services().keySet().stream().findFirst().orElse(null);
        if (service == null || !pickService(service)) return null;
        if (!pressStartWalk() || !chooseCountMode()) return null;
        return service;
    }

    // ── walk detail ────────────────────────────────────────────────────────────────────────────────────────

    public boolean isCountWalkDetailShown() {
        return labelled("StaticText", COUNT_TAG).isPresent() && labelled("StaticText", LOCATIONS).isPresent();
    }

    /** Large title of the open walk (e.g. 'Site Walk — 2026-10-21'). */
    public String walkTitle() {
        return find(n -> "StaticText".equals(n.type) && n.y > 90 && n.y < 140 && n.x < 40 && n.w > 100).map(n -> n.label).orElse("");
    }

    public boolean isCompleteWalkEnabled() {
        return labelled("Button", COMPLETE_WALK).map(n -> n.enabled).orElse(false);
    }

    public boolean isCompleteHintShown() {
        return find(n -> "StaticText".equals(n.type) && n.label.contains(HINT_COUNT)).isPresent();
    }

    /** Room rows on the walk detail ('<room>, N asset(s)'). */
    public List<String> roomRows() {
        return findAll(n -> "Button".equals(n.type) && n.label.matches("^.+, \\d+ assets?$")).stream().map(n -> n.label).toList();
    }

    public boolean openCountWithoutLocation() {
        if (!pressButton(COUNT_WITHOUT_LOCATION)) return false;
        return waitUntil(this::isGridShown, 8_000);
    }

    public boolean startFirstRoom() {
        if (!pressButton(START_FIRST_ROOM) && !pressButton(START_NEXT_ROOM)) return false;
        return waitUntil(this::isGridShown, 8_000);
    }

    /** From the walk detail back to the list (nav back button 'Site Walks'). */
    public boolean backToList() {
        Optional<SnapNode> b = find(n -> "Button".equals(n.type) && n.is(TAB) && n.y < 140);
        if (b.isEmpty()) return isListShown();
        tapNode(b.get());
        return waitForList(10_000);
    }

    // ── count grid ─────────────────────────────────────────────────────────────────────────────────────────

    public boolean isGridShown() {
        return find(n -> "TextField".equals(n.type) && SEARCH_ASSETS.equals(n.value)).isPresent()
                || find(n -> "TextField".equals(n.type) && n.value.startsWith("Search assets")).isPresent();
    }

    /** 'None counted yet' → 0, '1 asset' → 1, 'N assets' → N; -1 when no total is on screen. */
    public int gridTotal() {
        for (SnapNode n : findAll(x -> "StaticText".equals(x.type) && x.x > rx(250) && x.y < 300)) {
            Matcher m = TOTAL.matcher(n.label);
            if (m.matches()) return m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
        }
        return -1;
    }

    public String gridTotalText() {
        return find(n -> "StaticText".equals(n.type) && n.x > rx(250) && n.y < 300 && TOTAL.matcher(n.label).matches())
                .map(n -> n.label).orElse("");
    }

    /** Class cards ('<class>' or '<class>, N'), top to bottom. */
    public List<String> classCards() {
        return findAll(n -> "Button".equals(n.type) && n.x <= 24 && n.w > 300 && n.y > 220
                && !n.is(SAVE_ASSETS) && !n.is(COMPLETE_ROOM) && !n.label.startsWith(ASSIGN_LATER)
                && !n.is(ADD_ASSET_CLASS) && !n.is(ADD_SUBTYPE))
                .stream().map(n -> n.label).toList();
    }

    /** Count shown on a class card ('<class>, N' → N; plain '<class>' → 0). */
    public int classCount(String className) {
        for (String c : classCards()) {
            if (c.equals(className)) return 0;
            if (c.startsWith(className + ", ") && c.substring(className.length() + 2).matches("\\d+")) {
                return Integer.parseInt(c.substring(className.length() + 2));
            }
        }
        return -1;
    }

    /** Expand a class card; true once its subtype rows (with an 'Add' stepper) are visible. */
    public boolean expandClass(String className) {
        Optional<SnapNode> card = find(n -> "Button".equals(n.type) && n.x <= 24 && n.w > 300
                && (n.is(className) || n.label.startsWith(className + ", ")));
        if (card.isEmpty()) return false;
        tapNode(card.get());
        final int cardY = card.get().y;
        return waitUntil(() -> find(n -> "Button".equals(n.type) && n.is("Add") && n.x > rx(350) && n.y > cardY).isPresent(), 5_000);
    }

    private Optional<SnapNode> subtypeLabel(String subtype) {
        return find(n -> "StaticText".equals(n.type) && n.is(subtype) && n.x < 60);
    }

    private Optional<SnapNode> stepper(String subtype, String which) {
        Optional<SnapNode> lbl = subtypeLabel(subtype);
        if (lbl.isEmpty()) return Optional.empty();
        int y = lbl.get().y;
        return find(n -> "Button".equals(n.type) && n.is(which) && Math.abs(n.y - y) <= 8);
    }

    /** Count text on a subtype row; -1 when the row is not visible. */
    public int subtypeCount(String subtype) {
        Optional<SnapNode> lbl = subtypeLabel(subtype);
        if (lbl.isEmpty()) return -1;
        int y = lbl.get().y;
        return find(n -> "StaticText".equals(n.type) && n.x > rx(330) && n.x < rx(370) && Math.abs(n.y - y) <= 8 && n.label.matches("\\d+"))
                .map(n -> Integer.parseInt(n.label)).orElse(-1);
    }

    public boolean isRemoveEnabled(String subtype) {
        return stepper(subtype, "Remove").map(n -> n.enabled).orElse(false);
    }

    /** Press '+' on a subtype row; true once its count grew by one. */
    public boolean increment(String subtype) {
        int before = subtypeCount(subtype);
        Optional<SnapNode> add = stepper(subtype, "Add");
        if (add.isEmpty()) return false;
        tapNode(add.get());
        return waitUntil(() -> subtypeCount(subtype) == before + 1, 4_000);
    }

    /** Press '−' on a subtype row; true once its count shrank by one. */
    public boolean decrement(String subtype) {
        int before = subtypeCount(subtype);
        Optional<SnapNode> rm = stepper(subtype, "Remove");
        if (rm.isEmpty() || !rm.get().enabled) return false;
        tapNode(rm.get());
        return waitUntil(() -> subtypeCount(subtype) == before - 1, 4_000);
    }

    /** Subtype rows that carry a stepper right now (text left, 'Add' on the same line), top to bottom. */
    public List<String> subtypeRows() {
        List<SnapNode> s = snapshot();
        List<String> out = new ArrayList<>();
        s.stream().filter(a -> a.visible && "Button".equals(a.type) && a.is("Add") && a.x > rx(300))
                .sorted(java.util.Comparator.comparingInt(a -> a.y))
                .forEach(add -> s.stream().filter(n -> n.visible && "StaticText".equals(n.type) && n.x < 60
                                && Math.abs(n.y - add.y) <= 8)
                        .findFirst().ifPresent(n -> out.add(n.text())));
        return out;
    }

    public boolean isGridNoMatchShown() {
        return labelled("StaticText", GRID_NO_MATCH).isPresent();
    }

    /** Clear the grid search with its clear button, or by backspacing. */
    public boolean clearGridSearch() {
        Optional<SnapNode> x = find(n -> "Button".equals(n.type) && (n.is("Clear text") || n.is("Close")) && n.y > 150 && n.y < 280);
        if (x.isPresent()) {
            tapNode(x.get());
            sleep(700);
            dismissKeyboard();
            return true;
        }
        return searchGrid("");
    }

    /** Is the walk-level 'Reopen' shown (a completed walk's footer)? */
    public boolean isReopenShown() {
        return labelled("Button", REOPEN).isPresent();
    }

    /**
     * 'Complete Site Walk'. 1.70 pops straight back to the registry; older builds stay on the detail with 'Reopen'.
     * True once either is visible.
     */
    public boolean completeWalk() {
        Optional<SnapNode> b = settled(n -> "Button".equals(n.type) && n.is(COMPLETE_WALK));
        if (b.isEmpty() || !b.get().enabled) return false;
        tapNode(b.get());
        return waitUntil(() -> isListShown() || isReopenShown(), 10_000);
    }

    /** Open a walk from the list by tapping its row. */
    public boolean openWalkRow(WalkRow row) {
        tapNode(row.node);
        return waitUntil(() -> labelled("StaticText", LOCATIONS).isPresent() || isTextVisible(COUNT_TAG), 10_000);
    }

    /** Unit rows '<class> 1..N' currently listed. */
    public List<String> unitRows(String className) {
        return findAll(n -> "StaticText".equals(n.type) && n.label.matches(Pattern.quote(className) + " \\d+") && n.x > 40 && n.x < 70)
                .stream().map(n -> n.label).toList();
    }

    public boolean searchGrid(String text) {
        Optional<SnapNode> f = find(n -> "TextField".equals(n.type) && n.value.startsWith("Search assets"));
        if (f.isEmpty()) f = find(n -> "TextField".equals(n.type) && n.y > 180 && n.y < 260);
        if (f.isEmpty()) return false;
        tapNode(f.get());
        sleep(600);
        replaceFocusedText(text, 30);
        sleep(1000);
        dismissKeyboard();
        return true;
    }

    /**
     * A room grid's header: TextFields whose PLACEHOLDERS are 'Name this room' / 'Add floor (optional)' (they are
     * fields, not text — isTextVisible() never sees them; TC_SWC_06 first run 2026-10-21).
     */
    public boolean isRoomHeaderShown() {
        return find(n -> "TextField".equals(n.type) && (ROOM_NAME.equals(n.placeholder) || ROOM_NAME.equals(n.value))).isPresent()
                && find(n -> "TextField".equals(n.type) && (FLOOR.equals(n.placeholder) || FLOOR.equals(n.value))).isPresent();
    }

    public boolean typeRoomName(String name) {
        Optional<SnapNode> f = find(n -> "TextField".equals(n.type) && (ROOM_NAME.equals(n.placeholder) || ROOM_NAME.equals(n.value)));
        if (f.isEmpty()) f = find(n -> "TextField".equals(n.type) && n.y < 160);
        if (f.isEmpty()) return false;
        tapNode(f.get());
        sleep(600);
        replaceFocusedText(name, 30);
        sleep(600);
        dismissKeyboard();
        return find(n -> "TextField".equals(n.type) && name.equals(n.value)).isPresent();
    }

    public boolean isFinishEnabled() {
        return find(n -> "Button".equals(n.type) && (n.is(SAVE_ASSETS) || n.is(COMPLETE_ROOM))).map(n -> n.enabled).orElse(false);
    }

    /** 'Save Assets' / 'Complete Room'; true once back on the walk detail. */
    public boolean finishGrid() {
        if (!pressButton(SAVE_ASSETS) && !pressButton(COMPLETE_ROOM)) return false;
        return waitUntil(this::isCountWalkDetailShown, 10_000);
    }

    /**
     * Grid back chevron. With unsaved counts the app asks 'Save changes?' / 'You have unsaved counts or room
     * details.' (Save · Discard · Cancel); {@code answer} picks the button. Returns the alert title seen (or "").
     */
    public String backFromGrid(String answer) {
        final String[] seen = {""};
        withAlertsManual(() -> {
            Optional<SnapNode> back = find(n -> "Button".equals(n.type) && n.is("Back") && n.y < 120);
            if (back.isEmpty()) return;
            tapNode(back.get());
            sleep(1200);
            Optional<SnapNode> alert = find(n -> "Alert".equals(n.type));
            if (alert.isPresent()) {
                seen[0] = alert.get().label;
                find(n -> "Button".equals(n.type) && n.is(answer) && n.y > alert.get().y).ifPresent(this::tapNode);
                sleep(1500);
            }
        });
        return seen[0];
    }

    /** Text of the visible alert (title + message StaticTexts), or "" when none. */
    public String alertText() {
        Optional<SnapNode> a = find(n -> "Alert".equals(n.type));
        if (a.isEmpty()) return "";
        StringBuilder b = new StringBuilder();
        for (SnapNode t : findAll(n -> "StaticText".equals(n.type) && n.y >= a.get().y && n.y <= a.get().y + a.get().h)) {
            if (b.length() > 0) b.append(" | ");
            b.append(t.label);
        }
        return b.toString();
    }

    /**
     * Long-press a list row → 'Delete Site Walk' → read the confirmation → press {@code answer}
     * ('Delete Site Walk' or 'Cancel'). Returns the alert text, "" when the menu or alert never showed.
     */
    public String deleteWalkRow(WalkRow row, String answer) {
        final String[] text = {""};
        withAlertsManual(() -> {
            if (!longPressAt(row.node.centerX(), row.node.centerY(), 1.2)) return;
            // The context menu animates in; its item reaches the tree a beat after the preview (TC_SWW_07 pressed
            // too early on 2026-10-21 — menu visible in the screenshot, nothing pressed). Wait, then press settled.
            if (!waitUntil(() -> labelled("Button", DELETE_WALK).isPresent(), 4_000)) {
                System.out.println("⚠️ deleteWalkRow: no '" + DELETE_WALK + "' menu item after the long-press");
                return;
            }
            if (!pressSettledButton(DELETE_WALK)) return;
            if (!waitUntil(() -> find(n -> "Alert".equals(n.type)).isPresent(), 5_000)) {
                System.out.println("⚠️ deleteWalkRow: no confirmation alert after '" + DELETE_WALK + "'");
                return;
            }
            text[0] = alertText();
            SnapNode alert = find(n -> "Alert".equals(n.type)).get();
            find(n -> "Button".equals(n.type) && n.is(answer) && n.y > alert.y + 60).ifPresent(this::tapNode);
            sleep(1500);
        });
        return text[0];
    }
}
