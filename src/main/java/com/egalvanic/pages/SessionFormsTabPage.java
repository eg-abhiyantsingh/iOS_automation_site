package com.egalvanic.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ZP-3914 (iOS 1.59) "NETA-3 Forms Tab" + "Multi-edit working sets" + EG Forms ghost values, inside a work-order
 * session. Captured live on the 1.69 QA build (QA-WT10 NETA Testing, probe 2026-10-21); source SessionFormsTab.swift,
 * EGFormView.swift (1.70).
 *
 *  Level 1 (bottom tab 'Forms'): Buttons '<form title>, <asset classes>, <submitted>/<total>, <Submitted | N draft(s)>'.
 *  Level 2 (a form type): nav title = form title, nav 'Select'; TextField 'Search asset or location' (+ 'Close' to clear);
 *    Button 'Filter by room or status'; rows = StaticTexts <asset | 'No asset'> [+ room line], tally 'k/n', status chip
 *    'Not Started' / 'In Progress' / 'Submitted' (rows are NOT buttons — tap the asset text). Empty: 'No matches'
 *    (search) / 'No forms' (filter).
 *  Filter sheet: nav 'Filters' (+ 'Done'), STATUS 'All' / 'Draft' / 'Submitted', ROOM 'All rooms' / '<room>' — the
 *    active option carries value="1"; 'Clear filters' appears once a filter is active.
 *  Select mode: nav 'All' + 'Done'; each row gets a circle Image ('circle' → 'Selected'); with ≥1 selected the bar
 *    'Edit' · 'Mark As..' (menu 'Draft' / 'Submitted', no confirmation) · 'Delete' (Alert 'Delete N form(s)?' /
 *    'The selected forms and their entered data will be removed from this work order.' — Cancel · Delete).
 *  Multi-edit ('Edit'): 'Close', 'Edit Form', chip 'Filter and sort assets', 'Editing:', '<N> assets · <M> submitted',
 *    per-asset header with 'Copy this section to the other assets' + 'Mark section done'; footer 'Sections',
 *    'Save Draft', 'Submit' (accessibility labels). Ghost banner (multi-edit only, needs a participant with ghost
 *    data): 'Blue italic values come from each asset's last submitted form — left untouched, they save on Submit.'
 *  Single form (tap a row): 'Edit Form' without the 'Editing:' chip; procedure table rows have a Result Button
 *    ('—' / 'Pass' / 'Fail' / 'N/A' menu) and a 'Value / Notes' TextField.
 *
 * v1.73 (live probe 2026-10-30, iOS 26.2) — same features, new chrome; both layouts are supported:
 *  Level 1 rows read '<title>, <s>/<t> submitted - <asset classes>, <N drafts | Submitted>'.
 *  Level 2 rows are Buttons '<asset>, <room> - <k>/<n> sections, <status>' ('No asset, <k>/<n> sections, …').
 *  No nav 'Select': a row's long-press menu offers 'Select' · 'Copy Data To…' · 'Delete'. 'Select' enters select
 *    mode WITH that row already selected; each row gets a Button 'Select' (name 'circle' / 'checkmark.circle.fill',
 *    value "1" when on). Bottom bar: '<N> Selected' · 'Select All' / 'Cancel' · 'Edit' · 'More' (menu 'Mark Draft'
 *    / 'Mark Submitted') · 'Delete'.
 */
public class SessionFormsTabPage extends SnapPage {

    public static final String FORMS_TAB = "Forms";
    public static final String SEARCH_PLACEHOLDER = "Search asset or location";
    public static final String FILTER_BUTTON = "Filter by room or status";
    public static final String FILTERS_TITLE = "Filters";
    public static final String CLEAR_FILTERS = "Clear filters";
    public static final String NO_MATCHES = "No matches";
    public static final String NO_FORMS = "No forms";
    public static final String SELECT = "Select";
    public static final String EDIT_FORM = "Edit Form";
    public static final String EDITING_CHIP = "Editing:";
    public static final String MARK_AS = "Mark As..";
    public static final String DELETE_MESSAGE = "The selected forms and their entered data will be removed from this work order.";
    public static final String GHOST_BANNER = "Blue italic values come from each asset's last submitted form — left untouched, they save on Submit.";
    public static final String COPY_SECTION = "Copy this section to the other assets";
    public static final String MARK_SECTION_DONE = "Mark section done";

    private static final Pattern TYPE_ROW = Pattern.compile("^(.+), (\\d+)/(\\d+), (Submitted|\\d+ drafts?)$");
    /** v1.73 Level-1 row: '<title>, <s>/<t> submitted - <classes>, <state>'. */
    private static final Pattern TYPE_ROW_173 = Pattern.compile("^(.+?), (\\d+)/(\\d+) submitted - (.+), (Submitted|\\d+ drafts?)$");
    /** v1.73 Level-2 row button: '<asset>, [<room> - ]<k>/<n> sections, <status>'. */
    private static final Pattern INSTANCE_ROW_173 = Pattern.compile("^(.+?), (?:(.+) - )?(\\d+/\\d+) sections?, (.+)$");
    private static final Pattern CHIP = Pattern.compile("^(Not Started|In Progress|Submitted)$");
    private static final Pattern TALLY = Pattern.compile("^\\d+/\\d+$");
    private static final Pattern MULTI_SUMMARY = Pattern.compile("^(\\d+) assets? · (\\d+) submitted$");

    /** A Level-1 form-type row. */
    public static final class FormTypeRow {
        public final String label, head;
        public final int submitted, total;
        public final String state;
        FormTypeRow(Matcher m, String label) {
            this(label, m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), m.group(m.groupCount()));
        }
        FormTypeRow(String label, String head, int submitted, int total, String state) {
            this.label = label; this.head = head; this.submitted = submitted; this.total = total; this.state = state;
        }
        @Override public String toString() { return label; }
    }

    /** A Level-2 instance row. */
    public static final class InstanceRow {
        public final String asset, room, tally, status;
        public final int chipY;
        InstanceRow(String asset, String room, String tally, String status, int chipY) {
            this.asset = asset; this.room = room; this.tally = tally; this.status = status; this.chipY = chipY;
        }
        @Override public String toString() { return asset + (room.isEmpty() ? "" : " @ " + room) + " " + tally + " " + status; }
    }

    // ── Level 1 ────────────────────────────────────────────────────────────────────────────────────────────

    /** Bottom session bar 'Forms'. */
    public boolean openFormsTab() {
        // A focused search field (the room filter on the Assets tab) lifts the session bar above the keyboard
        // (TC_GV_02, 2026-10-21) — put the keyboard away first so the bar is back at the bottom.
        if (isKeyboardUp()) dismissKeyboard();
        Optional<SnapNode> tab = find(n -> "Button".equals(n.type) && n.is(FORMS_TAB) && n.y > by(780));
        if (tab.isEmpty()) return false;
        tapNode(tab.get());
        return waitUntil(() -> !formTypeRows().isEmpty() || isTextVisible("No forms in this work order"), 10_000);
    }

    public boolean isFormsTabOffered() {
        if (isKeyboardUp()) dismissKeyboard();
        return find(n -> "Button".equals(n.type) && n.is(FORMS_TAB) && n.y > by(780)).isPresent();
    }

    public List<FormTypeRow> formTypeRows() {
        List<FormTypeRow> out = new ArrayList<>();
        for (SnapNode n : findAll(x -> "Button".equals(x.type) && x.w > 300)) {
            Matcher m173 = TYPE_ROW_173.matcher(n.label);
            if (m173.matches()) { out.add(new FormTypeRow(m173, n.label)); continue; }
            Matcher m = TYPE_ROW.matcher(n.label);
            if (m.matches()) out.add(new FormTypeRow(m, n.label));
        }
        return out;
    }

    /** Open the Level-1 row with this exact label; true once Level 2 (Select + search) is up. */
    public boolean openFormType(String rowLabel) {
        if (!pressButton(rowLabel)) return false;
        return waitUntil(this::isInstanceListShown, 8_000);
    }

    // ── Level 2 ────────────────────────────────────────────────────────────────────────────────────────────

    public boolean isInstanceListShown() {
        return find(n -> "TextField".equals(n.type) && (SEARCH_PLACEHOLDER.equals(n.value) || n.y > 100 && n.y < 140)).isPresent()
                && labelled("Button", FILTER_BUTTON).isPresent();
    }

    public List<InstanceRow> instanceRows() {
        List<SnapNode> s = snapshot();
        List<InstanceRow> out = new ArrayList<>();
        // v1.73: one Button per row carries everything.
        for (SnapNode n : s) {
            if (!n.visible || !"Button".equals(n.type) || n.w < 300 * screenW() / 440 - 60) continue;
            Matcher m = INSTANCE_ROW_173.matcher(n.label);
            if (m.matches()) out.add(new InstanceRow(m.group(1), m.group(2) == null ? "" : m.group(2), m.group(3), m.group(4), n.y));
        }
        if (!out.isEmpty()) {
            out.sort(java.util.Comparator.comparingInt(r -> r.chipY));
            return out;
        }
        for (SnapNode chip : s) {
            if (!chip.visible || !"StaticText".equals(chip.type) || chip.x < rx(300) || !CHIP.matcher(chip.label).matches()) continue;
            String tally = s.stream().filter(n -> n.visible && "StaticText".equals(n.type) && TALLY.matcher(n.label).matches()
                    && Math.abs(n.y - chip.y) <= 6).map(n -> n.label).findFirst().orElse("");
            String asset = s.stream().filter(n -> n.visible && "StaticText".equals(n.type) && n.x < 70
                            && n.y >= chip.y - 16 && n.y <= chip.y + 3)
                    .min(java.util.Comparator.comparingInt(n -> n.y)).map(n -> n.label).orElse("");
            String room = s.stream().filter(n -> n.visible && "StaticText".equals(n.type) && n.x < 70
                            && n.y > chip.y + 3 && n.y <= chip.y + 20)
                    .findFirst().map(n -> n.label).orElse("");
            out.add(new InstanceRow(asset, room, tally, chip.label, chip.y));
        }
        out.sort(java.util.Comparator.comparingInt(r -> r.chipY));
        return out;
    }

    public boolean search(String text) {
        Optional<SnapNode> f = find(n -> "TextField".equals(n.type) && n.y > 100 && n.y < 140);
        if (f.isEmpty()) return false;
        tapNode(f.get());
        sleep(600);
        replaceFocusedText(text, 40);
        sleep(1200);
        dismissKeyboard();
        return true;
    }

    /** The search field's clear button ('Close' beside it). */
    public boolean clearSearch() {
        Optional<SnapNode> x = find(n -> "Button".equals(n.type) && n.is("Close") && n.y > 100 && n.y < 140);
        x.ifPresent(this::tapNode);
        sleep(800);
        dismissKeyboard();
        return x.isPresent();
    }

    public boolean isNoMatchesShown() { return labelled("StaticText", NO_MATCHES).isPresent(); }
    public boolean isNoFormsShown()   { return labelled("StaticText", NO_FORMS).isPresent(); }

    /** Empty list under an active filter: 1.69 'No forms'; v1.73 'No matches' + an inline 'Clear Filters' button. */
    public boolean isFilteredEmptyShown() { return isNoFormsShown() || isNoMatchesShown(); }

    /** 'Clear filters' (1.69, in the sheet) / 'Clear Filters' (v1.73, inline under 'No matches'), any case. */
    private Optional<SnapNode> clearFiltersControl() {
        return find(n -> ("Button".equals(n.type) || "StaticText".equals(n.type)) && n.text().equalsIgnoreCase(CLEAR_FILTERS));
    }

    public boolean isClearFiltersOffered() { return clearFiltersControl().isPresent(); }

    // ── filter sheet ───────────────────────────────────────────────────────────────────────────────────────

    public boolean openFilters() {
        if (!pressButton(FILTER_BUTTON)) return false;
        return waitUntil(() -> find(n -> "NavigationBar".equals(n.type) && n.is(FILTERS_TITLE)).isPresent(), 6_000);
    }

    /** Filter sheet options (label → active), status and room sections together. */
    public java.util.Map<String, Boolean> filterOptions() {
        java.util.Map<String, Boolean> out = new java.util.LinkedHashMap<>();
        Optional<SnapNode> bar = find(n -> "NavigationBar".equals(n.type) && n.is(FILTERS_TITLE));
        if (bar.isEmpty()) return out;
        for (SnapNode n : findAll(x -> "Button".equals(x.type) && x.y > bar.get().y + 40 && x.w > 300)) out.put(n.label, n.isOn());
        return out;
    }

    public boolean chooseFilter(String option) {
        Optional<SnapNode> bar = find(n -> "NavigationBar".equals(n.type) && n.is(FILTERS_TITLE));
        if (bar.isEmpty()) return false;
        Optional<SnapNode> b = find(n -> "Button".equals(n.type) && n.is(option) && n.y > bar.get().y + 40);
        if (b.isEmpty()) return false;
        tapNode(b.get());
        return waitUntil(() -> Boolean.TRUE.equals(filterOptions().get(option)), 4_000);
    }

    public boolean closeFilters() {
        Optional<SnapNode> bar = find(n -> "NavigationBar".equals(n.type) && n.is(FILTERS_TITLE));
        if (bar.isEmpty()) return true;
        Optional<SnapNode> done = find(n -> ("Button".equals(n.type) || "Other".equals(n.type)) && n.is("Done")
                && Math.abs(n.y - bar.get().y) < 40);
        done.ifPresent(this::tapNode);
        return waitUntil(() -> find(n -> "NavigationBar".equals(n.type) && n.is(FILTERS_TITLE)).isEmpty(), 6_000);
    }

    /** Reset any active filter (opens the sheet, 'Clear filters', closes). */
    public void resetFilters() {
        if (!openFilters()) return;
        pressButton(CLEAR_FILTERS);
        sleep(800);
        closeFilters();
    }

    /** With the filter sheet already open: 'Clear filters', then close it (v1.73: the inline 'Clear Filters' if the sheet has none). */
    public void resetFiltersFromOpenSheet() {
        Optional<SnapNode> inSheet = find(n -> "Button".equals(n.type) && n.text().equalsIgnoreCase(CLEAR_FILTERS)
                && find(b -> "NavigationBar".equals(b.type) && b.is(FILTERS_TITLE)).map(b -> n.y > b.y).orElse(false));
        inSheet.ifPresent(this::tapNode);
        sleep(800);
        closeFilters();
        if (inSheet.isEmpty()) {
            clearFiltersControl().ifPresent(this::tapNode);
            sleep(800);
        }
    }

    // ── select mode / multi-edit ───────────────────────────────────────────────────────────────────────────

    /** The 1.69 nav 'Select' button (absent from v1.73, where the row long-press menu offers 'Select'). */
    private Optional<SnapNode> navSelect() {
        return find(n -> "Button".equals(n.type) && n.is(SELECT) && n.y < 200 && n.x > rx(300));
    }

    /** True on the v1.73 layout: select mode is entered from a row's long-press menu. */
    public boolean selectsFromRowMenu() {
        return navSelect().isEmpty();
    }

    /** Long-press a Level-2 row and return its context-menu items (menu left open — {@link #dismissMenu()}). */
    public List<String> openRowMenu(InstanceRow row) {
        int x = 150 * screenW() / 440, y = row.chipY + 22;
        try {
            driver.executeScript("mobile: touchAndHold", java.util.Map.of("x", x, "y", y, "duration", 1.4));
        } catch (Exception e) {
            System.out.println("⚠️ openRowMenu: " + e.getMessage());
            return List.of();
        }
        List<String> items = new ArrayList<>();
        waitUntil(() -> {
            items.clear();
            for (SnapNode n : findAll(m -> "Button".equals(m.type) && m.w > 200 && m.w < 300 && m.x > 60 && m.x < 140)) items.add(n.label);
            return !items.isEmpty();
        }, 4_000);
        return items;
    }

    /** Select mode is reachable: the 1.69 nav 'Select', or (v1.73) 'Select' in a row's long-press menu. */
    public boolean isSelectEntryOffered() {
        if (navSelect().isPresent()) return true;
        List<InstanceRow> rows = instanceRows();
        if (rows.isEmpty()) return false;
        List<String> items = openRowMenu(rows.get(0));
        dismissMenu();
        return items.contains(SELECT);
    }

    /**
     * Enter select mode. 1.69: nav 'Select' (nothing selected). v1.73: first row › long-press › 'Select' — that row
     * starts SELECTED; {@link #selectedOnEntry()} tells callers which behaviour applies.
     */
    public boolean enterSelectMode() {
        Optional<SnapNode> nav = navSelect();
        if (nav.isPresent()) {
            tapNode(nav.get());
            return waitUntil(this::isSelectMode, 5_000);
        }
        List<InstanceRow> rows = instanceRows();
        if (rows.isEmpty()) return false;
        if (!openRowMenu(rows.get(0)).contains(SELECT)) { dismissMenu(); return false; }
        if (!pressButton(SELECT)) return false;
        return waitUntil(this::isSelectMode, 5_000);
    }

    /**
     * Enter select mode ON a given row: v1.73 long-presses exactly that row (so the pre-selected row is the caller's
     * own); 1.69 enters via the nav button and selects it. Destructive flows (Delete) must start here, never from
     * {@link #enterSelectMode()}, whose v1.73 pre-selection is whatever row happens to be first.
     */
    public boolean enterSelectModeOn(InstanceRow row) {
        if (navSelect().isPresent()) return enterSelectMode() && ensureSelected(row);
        if (!openRowMenu(row).contains(SELECT)) { dismissMenu(); return false; }
        if (!pressButton(SELECT)) return false;
        return waitUntil(this::isSelectMode, 5_000) && isRowSelected(row);
    }

    /**
     * v1.73 single delete: row long-press › 'Delete' (+ the confirmation, if one is raised). True once the list has
     * one row fewer. Used by test cleanup only, on rows the test itself created.
     */
    public boolean deleteInstanceRow(InstanceRow row) {
        int before = instanceRows().size();
        if (!openRowMenu(row).contains("Delete")) { dismissMenu(); return false; }
        withAlertsManual(() -> {
            pressButton("Delete");
            if (waitUntil(() -> find(n -> "Alert".equals(n.type)).isPresent(), 2_500)) {
                SnapNode a = find(n -> "Alert".equals(n.type)).get();
                find(n -> "Button".equals(n.type) && n.is("Delete") && n.y > a.y + 40).ifPresent(this::tapNode);
            }
        });
        return waitUntil(() -> instanceRows().size() == before - 1 || (before == 1 && isNoFormsShown()), 8_000);
    }

    /** Level 2 → Level 1 (the nav back button), true once form-type rows show. */
    public boolean backToFormTypes() {
        for (int i = 0; i < 3 && formTypeRows().isEmpty(); i++) {
            Optional<SnapNode> back = find(n -> "Button".equals(n.type) && (n.is("BackButton") || n.name.equals("BackButton")) && n.y < 140);
            if (back.isEmpty()) break;
            tapNode(back.get());
            sleep(1200);
        }
        return !formTypeRows().isEmpty();
    }

    /** '<N> Selected' on the v1.73 bar; −1 when not shown. */
    public int selectedCount() {
        return find(n -> "StaticText".equals(n.type) && n.label.matches("^\\d+ Selected$"))
                .map(n -> Integer.parseInt(n.label.replaceAll("\\D+", ""))).orElse(-1);
    }

    /** Rows selected the moment select mode opens: 0 on 1.69, 1 (the long-pressed row) on v1.73. */
    public int selectedOnEntry() {
        return selectsFromRowMenu() ? 1 : 0;
    }

    private Optional<SnapNode> bottomBar(String label) {
        return find(n -> "Button".equals(n.type) && n.is(label) && n.y > by(700));
    }

    public boolean isSelectMode() {
        boolean v169 = find(n -> "Button".equals(n.type) && n.is("All") && n.y < 120).isPresent()
                && find(n -> "Button".equals(n.type) && n.is("Done") && n.y < 120).isPresent();
        return v169 || find(n -> "StaticText".equals(n.type) && n.label.matches("^\\d+ Selected$")).isPresent()
                || bottomBar("Select All").isPresent();
    }

    /** Select mode's 'All' (1.69, top-left) / 'Select All' (v1.73, bottom bar). */
    public boolean selectAll() {
        Optional<SnapNode> all = find(n -> "Button".equals(n.type) && n.is("All") && n.y < 120);
        if (all.isEmpty()) all = bottomBar("Select All");
        all.ifPresent(this::tapNode);
        if (all.isPresent()) sleep(800);
        return all.isPresent();
    }

    /** Leave select mode: 1.69 'Done' (top) / v1.73 'Cancel' (bottom bar). */
    public boolean exitSelectMode() {
        Optional<SnapNode> done = find(n -> "Button".equals(n.type) && n.is("Done") && n.y < 120);
        if (done.isEmpty()) done = bottomBar("Cancel");
        done.ifPresent(this::tapNode);
        return waitUntil(() -> !isSelectMode(), 5_000);
    }

    /** Selection circles on screen: [selected, unselected]. 1.69 Images 'Selected'/'circle'; v1.73 Buttons 'Select'. */
    public int[] selectionCircles() {
        int on = 0, off = 0;
        for (SnapNode n : findAll(x -> "Image".equals(x.type) && x.x < 45 && x.y > 140)) {
            if (n.is("Selected")) on++; else if (n.is("circle")) off++;
        }
        // No bottom cap: on CI's 874-pt screen the 9th row sits under the selection bar yet still counts as a listed row
        // (TC_ME_01/05 'Expected 9, Actual 8', run 41080855780). Bar buttons never carry the 'Select' circle label.
        for (SnapNode n : findAll(x -> "Button".equals(x.type) && x.is(SELECT) && x.x < 50 && x.w < 60 && x.y > 140)) {
            if (isCircleOn(n)) on++; else off++;
        }
        return new int[]{on, off};
    }

    private static boolean isCircleOn(SnapNode n) {
        return "1".equals(n.value) || n.name.contains("checkmark");
    }

    private Optional<SnapNode> circleOf(InstanceRow row) {
        Optional<SnapNode> c = find(n -> "Button".equals(n.type) && n.is(SELECT) && n.x < 50 && Math.abs(n.y - row.chipY) <= 10);
        return c.isPresent() ? c : find(n -> "Image".equals(n.type) && n.x < 45 && Math.abs(n.y - row.chipY) <= 10);
    }

    /** Is this row's circle on? */
    public boolean isRowSelected(InstanceRow row) {
        return circleOf(row).map(n -> isCircleOn(n) || n.is("Selected")).orElse(false);
    }

    /** Toggle a row's selection circle (rows are located by their status chip / row button). */
    public boolean toggleRow(InstanceRow row) {
        int before = selectionCircles()[0];
        Optional<SnapNode> circle = circleOf(row);
        if (circle.isPresent()) tapNode(circle.get()); else tapPoint(31, row.chipY + 10);
        return waitUntil(() -> selectionCircles()[0] != before, 4_000);
    }

    /** Select the row unless it already is (v1.73 pre-selects the long-pressed row). */
    public boolean ensureSelected(InstanceRow row) {
        return isRowSelected(row) || toggleRow(row);
    }

    public boolean isSelectionBarShown() {
        return labelled("Button", "Edit").isPresent()
                && (labelled("Button", MARK_AS).isPresent() || bottomBar("More").isPresent())
                && find(n -> "Button".equals(n.type) && n.is("Delete") && n.y > 400).isPresent();
    }

    public boolean isEditEnabled() {
        return labelled("Button", "Edit").map(n -> n.enabled).orElse(false);
    }

    /**
     * Status menu items, normalised to 'Draft' / 'Submitted' (menu left open — call {@link #dismissMenu()}).
     * 1.69: bar 'Mark As..' › 'Draft' / 'Submitted'. v1.73: bar 'More' › 'Mark Draft' / 'Mark Submitted'.
     */
    public List<String> openMarkAsMenu() {
        if (!pressButton(MARK_AS)) {
            Optional<SnapNode> more = bottomBar("More");
            if (more.isEmpty()) return List.of();
            tapNode(more.get());
        }
        List<String> items = new ArrayList<>();
        waitUntil(() -> {
            items.clear();
            for (SnapNode n : findAll(x -> "Button".equals(x.type) && x.w > 200
                    && (x.is("Draft") || x.is("Submitted") || x.is("Mark Draft") || x.is("Mark Submitted")))) {
                items.add(n.label.replaceFirst("^Mark ", ""));
            }
            return !items.isEmpty();
        }, 4_000);
        return items;
    }

    /** Close an open menu without choosing (tap the navigation title). */
    public void dismissMenu() {
        tapPoint(180, 78);
        sleep(800);
    }

    /** 'Delete' on the selection bar, read the confirmation, press {@code answer}; returns the alert text. */
    public String deleteSelected(String answer) {
        final String[] text = {""};
        withAlertsManual(() -> {
            Optional<SnapNode> del = find(n -> "Button".equals(n.type) && n.is("Delete") && n.y > 400);
            if (del.isEmpty()) return;
            tapNode(del.get());
            if (!waitUntil(() -> find(n -> "Alert".equals(n.type)).isPresent(), 4_000)) return;
            SnapNode a = find(n -> "Alert".equals(n.type)).get();
            StringBuilder b = new StringBuilder();
            for (SnapNode t : findAll(n -> "StaticText".equals(n.type) && n.y >= a.y && n.y <= a.y + a.h)) {
                if (b.length() > 0) b.append(" | ");
                b.append(t.label);
            }
            text[0] = b.toString();
            find(n -> "Button".equals(n.type) && n.is(answer) && n.y > a.y + 60).ifPresent(this::tapNode);
            sleep(1500);
        });
        return text[0];
    }

    /** 'Edit' on the selection bar; true once the multi-edit editor ('Editing:') is up. */
    public boolean openMultiEdit() {
        if (!pressButton("Edit")) return false;
        return waitUntil(this::isMultiEditOpen, 12_000);
    }

    public boolean isEditorOpen() {
        return labelled("StaticText", EDIT_FORM).isPresent();
    }

    public boolean isMultiEditOpen() {
        return isEditorOpen() && labelled("StaticText", EDITING_CHIP).isPresent();
    }

    /** '<N> assets · <M> submitted' → {N, M}; null when absent. */
    public int[] multiEditSummary() {
        for (SnapNode n : findAll(x -> "StaticText".equals(x.type))) {
            Matcher m = MULTI_SUMMARY.matcher(n.label);
            if (m.matches()) return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        }
        return null;
    }

    public boolean isGhostBannerShown() {
        return find(n -> "StaticText".equals(n.type) && n.label.startsWith("Blue italic values come from")).isPresent();
    }

    public String ghostBannerText() {
        return find(n -> "StaticText".equals(n.type) && n.label.startsWith("Blue italic values come from")).map(n -> n.label).orElse("");
    }

    public List<String> editorFooter() {
        // Left to right: the three sit on one bar but not on one y (icon button vs text buttons differ by a few pt).
        return findAll(n -> "Button".equals(n.type) && n.y > by(820) && (n.is("Sections") || n.is("Save Draft") || n.is("Submit")))
                .stream().sorted(java.util.Comparator.comparingInt(n -> n.x)).map(n -> n.label).toList();
    }

    public int sectionToolCount(String accessibilityLabel) {
        return findAll(n -> "Button".equals(n.type) && n.is(accessibilityLabel)).size();
    }

    /**
     * 'Close' the editor. Unsaved edits raise 'Save changes before closing?' (Save & Close · Discard Changes ·
     * Keep Editing) — {@code discardIfAsked} picks Discard Changes. True once the editor is gone.
     */
    public boolean closeEditor(boolean discardIfAsked) {
        final boolean[] gone = {false};
        withAlertsManual(() -> {
            dismissKeyboard();
            Optional<SnapNode> close = find(n -> "Button".equals(n.type) && n.is("Close") && n.y < 110);
            if (close.isEmpty()) return;
            tapNode(close.get());
            sleep(1200);
            if (discardIfAsked) pressButton("Discard Changes");
            gone[0] = waitUntil(() -> !isEditorOpen(), 8_000);
        });
        return gone[0];
    }

    /** Tap a Level-2 row (its asset text; v1.73: the row button) to open that single form. */
    public boolean openInstance(InstanceRow row) {
        Optional<SnapNode> t = find(n -> "StaticText".equals(n.type) && n.is(row.asset) && n.x < 70
                && n.y >= row.chipY - 16 && n.y <= row.chipY + 3);
        if (t.isPresent()) tapNode(t.get());
        else tapPoint(150 * screenW() / 440, row.chipY + 22);
        return waitUntil(this::isEditorOpen, 10_000);
    }

    // ── single-form procedure table (ghost values) ───────────────────────────────────────────────────────────

    /** Result buttons of the procedure table, top to bottom ('—' / 'Pass' / 'Fail' / 'N/A'). */
    public List<SnapNode> resultButtons() {
        return findAll(n -> "Button".equals(n.type) && n.x > 150 * screenW() / 440 && n.x < 220 * screenW() / 440 && n.w > 100 && n.w < 160
                && (n.is("—") || n.is("Pass") || n.is("Fail") || n.is("N/A")));
    }

    /** 'Value / Notes' TextFields of the procedure table, top to bottom. */
    public List<SnapNode> notesFields() {
        return findAll(n -> "TextField".equals(n.type) && n.x > rx(280) && n.y > 300);
    }

    /** Pick a Result option for row {@code index}; true once that row's button reads it. */
    public boolean setResult(int index, String option) {
        List<SnapNode> rs = resultButtons();
        if (rs.size() <= index) return false;
        int y = rs.get(index).y;
        tapNode(rs.get(index));
        if (!waitUntil(() -> labelled("Button", "N/A").filter(n -> n.w > 200).isPresent(), 4_000)) return false;
        Optional<SnapNode> opt = find(n -> "Button".equals(n.type) && n.is(option) && n.w > 200);
        if (opt.isEmpty()) return false;
        tapNode(opt.get());
        return waitUntil(() -> resultButtons().stream().anyMatch(b -> Math.abs(b.y - y) <= 4 && b.is(option)), 4_000);
    }

    /** Type into the 'Value / Notes' field of row {@code index}; returns the field's value afterwards. */
    public String typeNotes(int index, String text) {
        List<SnapNode> fs = notesFields();
        if (fs.size() <= index) return null;
        SnapNode f = fs.get(index);
        tapPoint(Math.min(f.centerX(), 420), f.centerY());
        sleep(600);
        typeKeys(text);
        sleep(800);
        dismissKeyboard();
        List<SnapNode> after = notesFields();
        return after.size() > index ? after.get(index).value : null;
    }

    /**
     * Submit the open single form. An incomplete form asks 'Submit with incomplete sections?' (Submit Anyway ·
     * Cancel); 'Missing Required Fields' (OK) blocks. Returns the alert title seen ("" when none) — the editor is
     * closed on success.
     */
    public String submitForm() {
        final String[] seen = {""};
        withAlertsManual(() -> {
            dismissKeyboard();
            if (!pressButton("Submit")) return;
            sleep(1500);
            Optional<SnapNode> alert = find(n -> "Alert".equals(n.type));
            if (alert.isPresent()) {
                seen[0] = alert.get().label;
                if (!pressButton("Submit Anyway")) pressButton("OK");
                sleep(2000);
            }
            waitUntil(() -> !isEditorOpen(), 15_000);
        });
        return seen[0];
    }
}
