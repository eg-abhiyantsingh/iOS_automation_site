package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * THROWAWAY exploration dump for the v1.49 Engineering section (asset_engineer
 * library module). Phase 3: post-download surfaces — manufacturer picker sheet,
 * match panel + match cards, Add Custom sheet, fuse/busway blocks, and the
 * Add-Asset (detailed) circuit-breaker engineering block (cancelled, no residue).
 *
 * LESSONS BAKED IN from phase 2 (run died):
 *  - NEVER use `mobile: scroll` with predicateString on asset-details DOMs —
 *    the transformer details tree (100KB source) wedges WDA and kills the
 *    session. W3C pointer swipes only.
 *  - Element `visible` attr is the tap-readiness oracle; page source contains
 *    the FULL flat content regardless of scroll position.
 *  - Alert taps can report failure yet land (animation race) — verify outcome,
 *    don't trust the click return.
 * Run: mvn test -Dtest=_EngDump
 * Delete after the module lands (same lifecycle as _WoDomDump/_LocDump3).
 */
public class _EngDump extends BaseTest {

    private static final Path OUT = Paths.get("target", "engdump");

    @BeforeClass(alwaysRun = true)
    public void engDumpClassSetup() {
        DriverManager.setNoReset(true); // keep app container (library cache + login) across sessions
    }

    @AfterClass(alwaysRun = true)
    public void engDumpClassTeardown() {
        DriverManager.resetNoResetOverride();
    }

    private void dump(String name) {
        try {
            Files.createDirectories(OUT);
            String src = DriverManager.getDriver().getPageSource();
            Files.write(OUT.resolve(name + ".xml"), src.getBytes(StandardCharsets.UTF_8));
            byte[] png = DriverManager.getDriver().getScreenshotAs(OutputType.BYTES);
            Files.write(OUT.resolve(name + ".png"), png);
            System.out.println("🗂  dumped " + name + " (" + src.length() + " chars)");
        } catch (Exception e) {
            System.out.println("⚠️ dump '" + name + "' failed: " + e.getMessage());
        }
    }

    private void settle(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    private boolean present(String predicate) {
        return !DriverManager.getDriver().findElements(
                AppiumBy.iOSNsPredicateString(predicate)).isEmpty();
    }

    private boolean visibleNow(String predicate) {
        for (WebElement e : DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(predicate))) {
            try {
                if ("true".equals(e.getAttribute("visible"))) return true;
            } catch (Exception ignored) { }
        }
        return false;
    }

    private boolean tap(String predicate) {
        try {
            DriverManager.getDriver().findElement(AppiumBy.iOSNsPredicateString(predicate)).click();
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ tap failed [" + predicate + "]: " + e.getMessage());
            return false;
        }
    }

    /** W3C pointer swipe — no element-tree queries, safe on giant DOMs. */
    private void swipe(boolean up) {
        try {
            Dimension size = DriverManager.getDriver().manage().window().getSize();
            int x = size.width / 2;
            int startY = (int) (size.height * (up ? 0.72 : 0.30));
            int endY = (int) (size.height * (up ? 0.30 : 0.72));
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence s = new Sequence(finger, 1);
            s.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY));
            s.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            s.addAction(finger.createPointerMove(Duration.ofMillis(350), PointerInput.Origin.viewport(), x, endY));
            s.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            DriverManager.getDriver().perform(Arrays.asList(s));
        } catch (Exception e) {
            System.out.println("⚠️ swipe: " + e.getMessage());
        }
    }

    /** Swipe (content up) until predicate is visible, max n swipes. */
    private boolean swipeUntilVisible(String predicate, int maxSwipes) {
        for (int i = 0; i <= maxSwipes; i++) {
            if (visibleNow(predicate)) return true;
            if (i < maxSwipes) { swipe(true); settle(700); }
        }
        return false;
    }

    private void openAssetFromList(String namePrefix) {
        assetPage.navigateToAssetList();
        settle(1000);
        clearAssetListSearchIfPolluted();
        String cellPred = "type == 'XCUIElementTypeButton' AND name BEGINSWITH '" + namePrefix + "'";
        swipeUntilVisible(cellPred, 3);
        tap(cellPred);
        settle(1800);
    }

    /** If a previous step accidentally typed into the Assets search box, clear it. */
    private void clearAssetListSearchIfPolluted() {
        try {
            List<WebElement> fields = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeSearchField' OR type == 'XCUIElementTypeTextField'"));
            for (WebElement f : fields) {
                String v;
                try { v = f.getAttribute("value"); } catch (Exception e) { continue; }
                if (v != null && !v.isBlank() && !v.startsWith("Search by name")) {
                    System.out.println("🧹 clearing polluted asset search: '" + v + "'");
                    try {
                        tap("type == 'XCUIElementTypeButton' AND (label == 'Clear text' OR name == 'Clear text')");
                    } catch (Exception ignored) { }
                    try { f.clear(); } catch (Exception ignored) { }
                    settle(600);
                }
            }
        } catch (Exception ignored) { }
    }

    /** Close any details/add sheet; discard draft changes if prompted. */
    private void closeAndDiscard(String dumpNameForAlert) {
        tap("type == 'XCUIElementTypeButton' AND (name == 'Close' OR label == 'Close' OR label == 'Cancel')");
        settle(900);
        if (present("type == 'XCUIElementTypeAlert'")) {
            if (dumpNameForAlert != null) dump(dumpNameForAlert);
            tap("type == 'XCUIElementTypeButton' AND label BEGINSWITH 'Discard'");
            settle(900);
        }
    }

    @Test
    public void dumpUnlockedEngineeringSurfaces() {
        DriverManager.getDriver().manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
        try {
            loginAndSelectSite();

            // ── A. Ensure the equipment library is downloaded ─────────────
            try {
                siteSelectionPage.tapSettingsTab();
                settle(1200);
                swipeUntilVisible("type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Load Latest Equipment Library'", 3);
                boolean notDownloaded = present(
                        "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Load Latest Equipment Library' AND name CONTAINS 'Not yet downloaded'");
                System.out.println("ℹ️ library state: " + (notDownloaded ? "NOT downloaded" : "downloaded"));
                if (notDownloaded) {
                    tap("type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Load Latest Equipment Library'");
                    // Alert animates in — poll for the Download button.
                    long alertDeadline = System.currentTimeMillis() + 8000;
                    while (System.currentTimeMillis() < alertDeadline
                            && !present("type == 'XCUIElementTypeAlert'")) settle(500);
                    dump("33-load-dialog-v2");
                    tap("type == 'XCUIElementTypeButton' AND label == 'Download'");
                    settle(1000);
                    long deadline = System.currentTimeMillis() + 360_000;
                    while (System.currentTimeMillis() < deadline) {
                        settle(5000);
                        if (present("name CONTAINS ' frames,' OR name BEGINSWITH 'Last updated' OR name BEGINSWITH 'Download failed'")) break;
                    }
                }
                dump("34-settings-library-state");
            } catch (Exception e) {
                System.out.println("⚠️ library ensure step: " + e.getMessage());
            }

            // ── B. Fuse: full source is enough (flat content, no scroll needed) ──
            try {
                openAssetFromList("Trim600639 Fuse");
                dump("80-fuse-details");
                closeAndDiscard(null);
            } catch (Exception e) {
                System.out.println("⚠️ fuse step: " + e.getMessage());
                closeAndDiscard(null);
            }

            // ── C. Busway: Cu segment → cable/busway matcher ──────────────
            try {
                openAssetFromList("Test Busway");
                dump("85-busway-details");
                if (swipeUntilVisible("type == 'XCUIElementTypeButton' AND label == 'Cu'", 5)
                        && tap("type == 'XCUIElementTypeButton' AND label == 'Cu'")) {
                    settle(1600);
                    dump("86-busway-after-cu");
                }
                closeAndDiscard("87-busway-discard-alert");
            } catch (Exception e) {
                System.out.println("⚠️ busway step: " + e.getMessage());
                closeAndDiscard(null);
            }

            // ── D. Add Asset (detailed): circuit-breaker engineering block ──
            try {
                assetPage.navigateToAssetList();
                settle(800);
                assetPage.clickAddAsset();
                settle(1500);
                dump("90-add-asset-initial");
                try {
                    assetPage.clickSelectAssetClass();
                    settle(1200);
                    dump("91-class-picker");
                    assetPage.selectAssetClass("Circuit Breaker");
                    settle(1800);
                } catch (Exception e) {
                    System.out.println("⚠️ class selection: " + e.getMessage());
                }
                dump("92-add-breaker-engineering");
                closeAndDiscard("93-add-discard-alert");
            } catch (Exception e) {
                System.out.println("⚠️ add-asset step: " + e.getMessage());
                closeAndDiscard(null);
            }

            // ── E. LAST (riskiest — giant DOM): transformer match panel ──
            // Phase-2/3 lesson: WDA sessions die on this screen when any
            // hidden-element interaction or predicate-scroll happens. Taps on
            // VISIBLE elements + page-source dumps only.
            try {
                openAssetFromList("Transformer-1");
                boolean found = swipeUntilVisible("type == 'XCUIElementTypeButton' AND label == 'Select…'", 5);
                System.out.println("ℹ️ Manufacturer Select… visible=" + found);
                if (found && tap("type == 'XCUIElementTypeButton' AND label == 'Select…'")) {
                    settle(1200);
                    // MENU with library-backed entries (observed: Generic, SCHNEIDER/SQUARE D)
                    boolean picked = tap("type == 'XCUIElementTypeButton' AND label == 'SCHNEIDER/SQUARE D'");
                    if (!picked) picked = tap("type == 'XCUIElementTypeButton' AND label == 'Generic'");
                    System.out.println("ℹ️ manufacturer picked=" + picked);
                    settle(2000);
                    dump("73-xfmr-after-manufacturer");
                    if (swipeUntilVisible("type == 'XCUIElementTypeButton' AND label CONTAINS 'Add Custom'", 2)
                            && tap("type == 'XCUIElementTypeButton' AND label CONTAINS 'Add Custom'")) {
                        settle(1500);
                        dump("75-custom-sheet");
                        tap("type == 'XCUIElementTypeButton' AND label == 'Cancel'");
                        settle(900);
                    }
                }
                closeAndDiscard("76-xfmr-discard-alert");
            } catch (Exception e) {
                System.out.println("⚠️ transformer step: " + e.getMessage());
                closeAndDiscard(null);
            }
        } finally {
            try {
                DriverManager.getDriver().manage().timeouts()
                        .implicitlyWait(Duration.ofSeconds(com.egalvanic.constants.AppConstants.IMPLICIT_WAIT));
            } catch (Exception ignored) { }
        }
    }

}
