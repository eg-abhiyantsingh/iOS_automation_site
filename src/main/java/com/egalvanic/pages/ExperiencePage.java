package com.egalvanic.pages;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ZP-3914 (iOS 1.59) "Experience chooser" / "Site Walk: Account Manager Experience".
 *
 * Live contract (1.69 QA build, probe 2026-10-21; source ExperienceChooserView.swift / AMTabView.swift):
 *  - Post sign-in, a user holding the 'Account Manager' role plus another role (and company feature 'site-walks')
 *    gets "Choose your experience" / "You can switch at any time in Settings." with two card Buttons
 *    'Field Technician, Sites, assets, connections and single-line diagrams' and
 *    'Account Manager, Review past site walks or start a new one.' — the highlighted card carries value="1"
 *    (trait Selected) — and a 'Continue' Button. BaseTest's login always picks Field Technician.
 *  - Later: Settings › bottom 'Switch Experience' (above Logout) pushes the same chooser; its back button reads
 *    'Settings' and the tab bar is hidden while it is up.
 *  - Field Technician tab bar: Site · Assets · Connections · SLD · Settings. Account Manager: Site Walks · Settings.
 *    Switching back to Field Technician lands on the same site's Dashboard. The choice is UserDefaults
 *    ('experience.selected') — it survives an app relaunch and is cleared by logout / reinstall.
 */
public class ExperiencePage extends SnapPage {

    public enum Experience {
        FIELD_TECHNICIAN("Field Technician", "Sites, assets, connections and single-line diagrams"),
        ACCOUNT_MANAGER("Account Manager", "Review past site walks or start a new one.");

        public final String title, description;
        Experience(String title, String description) { this.title = title; this.description = description; }
        public String cardLabel() { return title + ", " + description; }
    }

    public static final String CHOOSER_TITLE = "Choose your experience";
    public static final String CHOOSER_SUBTITLE = "You can switch at any time in Settings.";
    public static final String CONTINUE = "Continue";
    public static final String SWITCH_EXPERIENCE = "Switch Experience";
    public static final List<String> FIELD_TECH_TABS = List.of("Site", "Assets", "Connections", "SLD", "Settings");
    public static final List<String> ACCOUNT_MANAGER_TABS = List.of("Site Walks", "Settings");

    public boolean isChooserShown() {
        return labelled("StaticText", CHOOSER_TITLE).isPresent();
    }

    public boolean isSubtitleShown() {
        return labelled("StaticText", CHOOSER_SUBTITLE).isPresent();
    }

    /** Card label → selected (value="1"), for the cards currently on screen, top to bottom. */
    public Map<Experience, Boolean> cardSelection() {
        Map<Experience, Boolean> out = new LinkedHashMap<>();
        for (Experience e : Experience.values()) {
            find(n -> "Button".equals(n.type) && n.label.startsWith(e.title + ", ")).ifPresent(n -> out.put(e, n.isOn()));
        }
        return out;
    }

    /** Exact card labels on screen (to assert the copy verbatim). */
    public List<String> cardLabels() {
        return findAll(n -> "Button".equals(n.type) && (n.label.startsWith(Experience.FIELD_TECHNICIAN.title + ", ")
                || n.label.startsWith(Experience.ACCOUNT_MANAGER.title + ", "))).stream().map(n -> n.label).toList();
    }

    public boolean isContinueEnabled() {
        return labelled("Button", CONTINUE).map(n -> n.enabled).orElse(false);
    }

    /** Highlight a card; true once it reads selected. */
    public boolean selectCard(Experience e) {
        Optional<SnapNode> card = find(n -> "Button".equals(n.type) && n.label.startsWith(e.title + ", "));
        if (card.isEmpty()) return false;
        tapNode(card.get());
        return waitUntil(() -> Boolean.TRUE.equals(cardSelection().get(e)), 4_000);
    }

    /** Continue; true once the chooser has left the screen. */
    public boolean pressContinue() {
        if (!pressButton(CONTINUE)) return false;
        return waitUntil(() -> !isChooserShown(), 10_000);
    }

    /**
     * Settings tab at its ROOT. The tab keeps whatever was pushed on it last — the fresh-install Session Recording
     * pass leaves 'Session Analytics' open (TC_SWW_06 skipped on it, 2026-10-21) — so pop with the 'Settings' back
     * button (top-left, y<140) until the root list shows.
     */
    public boolean openSettingsRoot() {
        SettingsPage settings = new SettingsPage();
        if (!settings.openSettingsTab()) {
            System.out.println("🔁 openSettingsRoot: Settings tab not reached — re-activating the app and retrying once");
            reactivateApp();
            if (!openTab("Settings") || !waitUntil(settings::isSettingsScreenDisplayed, 5_000)) {
                if (!settings.openSettingsTab()) return false;
            }
        }
        // SettingsPage's element-query check can say 'Settings' while another tab shows: on CI TC_TOU_03 failed with
        // the Connections tab up and no Logout row (run 41131914013). Trust the tab bar's selection when it has one.
        String selected = selectedTab();
        if (selected != null && !"Settings".equals(selected)) {
            System.out.println("🔁 openSettingsRoot: tab bar shows '" + selected + "' selected — pressing the Settings tab");
            if (openTab("Settings")) waitUntil(() -> "Settings".equals(selectedTab()), 5_000);
        }
        for (int i = 0; i < 4; i++) {
            // A pushed screen's back chevron sits at the top-left; the root has none (its large title is not a Button).
            Optional<SnapNode> back = find(n -> "Button".equals(n.type) && n.y > 40 && n.y < 140 && n.x < 60 && n.w < 220);
            if (back.isEmpty()) return true;
            System.out.println("🧭 Settings tab is on a pushed screen — back via '" + back.get().text() + "'");
            tapNode(back.get());
            sleep(900);
        }
        System.out.println("⚠️ openSettingsRoot: still a back button after 4 pops — top buttons "
                + findAll(n -> "Button".equals(n.type) && n.y < 140).stream().map(n -> n.text() + "@" + n.x + "," + n.y).toList());
        return true;   // let the caller's own search decide
    }

    /** Settings › 'Switch Experience' (scrolls to the bottom); true once the chooser is up. */
    public boolean openFromSettings() {
        long t0 = System.currentTimeMillis();
        if (!openSettingsRoot()) return false;
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 5 && labelled("Button", SWITCH_EXPERIENCE).isEmpty(); i++) {
            swipeScreen("up");
            sleep(500);
        }
        long t2 = System.currentTimeMillis();
        boolean ok = pressSettledButton(SWITCH_EXPERIENCE) && waitUntil(this::isChooserShown, 8_000);
        System.out.println("⏱ openFromSettings: settings tab " + (t1 - t0) + "ms, scroll " + (t2 - t1) + "ms, open "
                + (System.currentTimeMillis() - t2) + "ms → " + ok);
        return ok;
    }

    /** Is the 'Switch Experience' row offered in Settings (only when the user can enter more than one experience). */
    /** Did the last {@link #isSwitchExperienceOffered()} actually reach Settings (false = could not even look)? */
    public boolean lastOfferCheckReachedSettings;

    public boolean isSwitchExperienceOffered() {
        lastOfferCheckReachedSettings = false;
        if (!openSettingsRoot()) {
            System.out.println("⚠️ isSwitchExperienceOffered: Settings tab not reachable — tabs " + tabBarLabels());
            return false;
        }
        lastOfferCheckReachedSettings = true;
        for (int i = 0; i < 6; i++) {
            if (labelled("Button", SWITCH_EXPERIENCE).isPresent()) return true;
            if (labelled("Button", "Logout").isPresent()) {
                // Logout is the last row and 'Switch Experience' sits right above it — one more settled look.
                if (settled(n -> "Button".equals(n.type) && n.is("Logout")).isPresent()
                        && labelled("Button", SWITCH_EXPERIENCE).isPresent()) return true;
                System.out.println("ℹ️ Settings bottom reached without 'Switch Experience' — buttons "
                        + findAll(n -> "Button".equals(n.type) && n.y > 400 && n.y < 860).stream().map(SnapNode::text).toList());
                return false;
            }
            swipeScreen("up");
            sleep(500);
        }
        System.out.println("ℹ️ 'Switch Experience' not found after scrolling — buttons "
                + findAll(n -> "Button".equals(n.type)).stream().map(SnapNode::text).toList());
        return labelled("Button", SWITCH_EXPERIENCE).isPresent();
    }

    /**
     * Settings › 'Logout' (bottom row) and confirm when asked; true once the signed-out Welcome / sign-in screen
     * shows (no tab bar). Runs with alerts manual so the confirmation is answered explicitly, not auto-accepted.
     */
    public boolean logoutFromSettings() {
        if (!openSettingsRoot()) return false;
        scrollLogoutClearOfTabBar();
        if (labelled("Button", "Logout").isEmpty()) {
            System.out.println("🔁 logoutFromSettings: no 'Logout' row — tabs " + tabBarLabels() + ", selected '" + selectedTab()
                    + "' — pressing the Settings tab and looking again");
            if (openTab("Settings")) sleep(1500);
            scrollLogoutClearOfTabBar();
        }
        final boolean[] pressed = {false};
        withAlertsManual(() -> {
            Optional<SnapNode> logout = settled(n -> "Button".equals(n.type) && n.is("Logout"));
            if (logout.isEmpty()) return;
            // Never the row's centre while the tab bar covers it: a full-width row's centre (x 201 at 402 pt) is the
            // 'Connections' tab. CI runs 41131914013 and 41188709540 ended on Connections with Settings selected
            // just before — the Logout press was the only tap.
            SnapNode b = logout.get();
            int barTop = tabBarTop(snapshot());
            int y = Math.min(b.centerY(), barTop - 8);
            if (y <= b.y) {
                System.out.println("⚠️ logoutFromSettings: 'Logout' (y " + b.y + "-" + (b.y + b.h) + ") is under the tab bar (top "
                        + barTop + ") — not pressing");
                return;
            }
            tapPoint(b.centerX(), y);
            pressed[0] = true;
            sleep(1200);
            Optional<SnapNode> alert = find(n -> "Alert".equals(n.type) || "Sheet".equals(n.type));
            if (alert.isPresent()) {
                int top = alert.get().y;
                find(n -> "Button".equals(n.type) && n.y > top + 40
                        && (n.is("Logout") || n.is("Log Out") || n.is("Sign Out") || n.is("Yes"))).ifPresent(this::tapNode);
                sleep(1500);
            }
        });
        return pressed[0] && waitUntil(() -> tabBarLabels().isEmpty(), 20_000);
    }

    /**
     * Swipe up until 'Logout' has its whole frame above the tab bar. A row resting under the bar still reads visible,
     * and sleep() does not wait, so a read right after a swipe can catch the row mid-scroll — that only costs one more
     * swipe here (the list clamps at its bottom, where the last row clears the bar).
     */
    private void scrollLogoutClearOfTabBar() {
        for (int i = 0; i < 6; i++) {
            List<SnapNode> s = snapshot();
            int barTop = tabBarTop(s);
            if (s.stream().anyMatch(n -> n.visible && "Button".equals(n.type) && n.is("Logout") && n.y + n.h <= barTop)) return;
            swipeScreen("up");
        }
    }

    /** Top of the visible tab bar in {@code s}; Integer.MAX_VALUE when none (or the read failed). */
    private static int tabBarTop(List<SnapNode> s) {
        return s.stream().filter(n -> n.visible && "TabBar".equals(n.type)).mapToInt(n -> n.y).min().orElse(Integer.MAX_VALUE);
    }

    /** Back from a Settings-pushed chooser without changing anything. */
    public boolean backToSettings() {
        Optional<SnapNode> back = find(n -> "Button".equals(n.type) && n.is("Settings") && n.y < 140);
        if (back.isEmpty()) return false;
        tapNode(back.get());
        return waitUntil(() -> !isChooserShown(), 6_000);
    }

    public boolean isOn(Experience e) {
        List<String> tabs = tabBarLabels();
        return e == Experience.ACCOUNT_MANAGER ? tabs.equals(ACCOUNT_MANAGER_TABS) : tabs.containsAll(FIELD_TECH_TABS);
    }

    /** Make {@code e} the active experience via Settings (no-op when already on it); true once its tab bar shows. */
    public boolean switchTo(Experience e) {
        long t0 = System.currentTimeMillis();
        if (isOn(e)) return true;
        if (!openFromSettings()) {
            // One more press: the row tap was swallowed once right after a site-walk test tore down (TC_SWC_02 on
            // v1.73, 2026-10-30: 'open 11311ms → false' with the chooser never shown) — the chooser itself is fine.
            if (isChooserShown() || !openFromSettings()) {
                if (!isChooserShown()) return false;
            }
        }
        long t1 = System.currentTimeMillis();
        if (!selectCard(e)) return false;
        if (!pressContinue()) return false;
        long t2 = System.currentTimeMillis();
        boolean ok = waitUntil(() -> isOn(e), 20_000);
        System.out.println("⏱ switchTo(" + e + "): chooser " + (t1 - t0) + "ms, pick+continue " + (t2 - t1)
                + "ms, tabs " + (System.currentTimeMillis() - t2) + "ms → " + ok);
        return ok;
    }
}
