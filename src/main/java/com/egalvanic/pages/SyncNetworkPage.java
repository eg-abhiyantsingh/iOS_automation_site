package com.egalvanic.pages;

import java.util.Optional;

/**
 * ZP-3914 (iOS 1.59) "Offline support via SwiftData" / "Offline sync" — Settings › 'Sync & Network'.
 * Live 1.69 contract (probe 2026-10-21; SettingsView.swift / SyncQueueAnalyzerView.swift / NetworkState.swift):
 *  - Row 'Network Mode' with subtitle 'Connected' / 'Offline Mode' and an UNLABELLED Switch on the same line
 *    (value 1 = online). Other switches share the screen (Session Recording above it) — always anchor on the
 *    'Network Mode' text's y, never on "the first switch".
 *  - Row 'Sync Queue Analyzer, Manage pending sync operations' → nav 'Sync Queue Analyzer' (back 'Settings'),
 *    toolbar 'Flush Queue Now' (disabled offline) + 'More'; offline banner 'Your offline mode is on'; segments
 *    'Pending (N)' / 'History (N)'; empty 'No Pending Operations' / 'All sync operations have been processed'.
 *  - Network mode is NOT persisted: every launch starts online. Nothing flushes automatically on reconnect.
 */
public class SyncNetworkPage extends SnapPage {

    public static final String NETWORK_MODE = "Network Mode";
    public static final String CONNECTED = "Connected";
    public static final String OFFLINE_MODE = "Offline Mode";
    public static final String ANALYZER = "Sync Queue Analyzer";
    public static final String ANALYZER_ROW = "Sync Queue Analyzer, Manage pending sync operations";
    public static final String FLUSH = "Flush Queue Now";
    public static final String OFFLINE_BANNER = "Your offline mode is on";
    public static final String NO_PENDING = "No Pending Operations";
    public static final String NO_PENDING_SUB = "All sync operations have been processed";

    /** Settings tab scrolled until the 'Network Mode' row is on screen. */
    public boolean openSyncSection() {
        if (!new SettingsPage().openSettingsTab()) return false;
        for (int i = 0; i < 5 && networkModeLabel().isEmpty(); i++) {
            swipeScreen("up");
            sleep(500);
        }
        return networkModeLabel().isPresent();
    }

    private Optional<SnapNode> networkModeLabel() {
        return find(n -> "StaticText".equals(n.type) && n.is(NETWORK_MODE) && n.y > 80 && n.y < by(820));
    }

    /** The switch on the Network Mode row (anchored on the label's y). */
    private Optional<SnapNode> networkSwitch() {
        Optional<SnapNode> lbl = networkModeLabel();
        if (lbl.isEmpty()) return Optional.empty();
        int y = lbl.get().y;
        return find(n -> "Switch".equals(n.type) && n.x > rx(300) && n.y >= y - 15 && n.y <= y + 30);
    }

    /** 'Connected' / 'Offline Mode' (the subtitle under Network Mode), or "". */
    public String networkModeSubtitle() {
        Optional<SnapNode> lbl = networkModeLabel();
        if (lbl.isEmpty()) return "";
        int y = lbl.get().y;
        return find(n -> "StaticText".equals(n.type) && (n.is(CONNECTED) || n.is(OFFLINE_MODE)) && n.y > y && n.y < y + 50)
                .map(n -> n.label).orElse("");
    }

    public boolean isOnline() {
        return networkSwitch().map(SnapNode::isOn).orElse(true);
    }

    /** Flip Network Mode to {@code online}; true once the subtitle agrees. */
    public boolean setOnline(boolean online) {
        if (isOnline() == online) return true;
        Optional<SnapNode> sw = networkSwitch();
        if (sw.isEmpty()) return false;
        tapNode(sw.get());
        String want = online ? CONNECTED : OFFLINE_MODE;
        return waitUntil(() -> want.equals(networkModeSubtitle()), 8_000);
    }

    public boolean openAnalyzer() {
        if (!pressButton(ANALYZER_ROW)) return false;
        return waitUntil(() -> find(n -> "NavigationBar".equals(n.type) && n.is(ANALYZER)).isPresent(), 8_000);
    }

    public boolean isFlushEnabled() {
        return labelled("Button", FLUSH).map(n -> n.enabled).orElse(false);
    }

    public boolean isOfflineBannerShown() {
        return labelled("StaticText", OFFLINE_BANNER).isPresent();
    }

    /** 'Pending (N)' → N; -1 when the segment is absent. */
    public int pendingCount() {
        return find(n -> "Button".equals(n.type) && n.label.matches("^Pending \\(\\d+\\)$"))
                .map(n -> Integer.parseInt(n.label.replaceAll("\\D+", ""))).orElse(-1);
    }

    public boolean isHistorySegmentShown() {
        return find(n -> "Button".equals(n.type) && n.label.matches("^History \\(\\d+\\)$")).isPresent();
    }

    public boolean isEmptyStateShown() {
        return labelled("StaticText", NO_PENDING).isPresent() && labelled("StaticText", NO_PENDING_SUB).isPresent();
    }

    /** Pending row titles ('Site Walk: <id>...' / '<Entity> - <OP>'). */
    public java.util.List<String> pendingRows() {
        return findAll(n -> "StaticText".equals(n.type) && (n.label.startsWith("Site Walk") || n.label.matches("^[A-Za-z ]+ - [A-Z]+$")))
                .stream().map(n -> n.label).toList();
    }

    public boolean backToSettings() {
        Optional<SnapNode> back = find(n -> "Button".equals(n.type) && n.is("Settings") && n.y < 120);
        back.ifPresent(this::tapNode);
        return waitUntil(() -> networkModeLabel().isPresent() || labelled("StaticText", "Settings").isPresent(), 6_000);
    }
}
