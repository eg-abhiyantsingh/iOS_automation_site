package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.utils.DriverManager;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * DISCOVERY PROBE for ZP-3914 (iOS 1.59) — NOT a test case (no TC_ id, never in a CI suite).
 *
 * Logs in through the project's own login flow, writes the live Appium session id to
 * {@code /tmp/zp/hold/session.txt}, then keeps the session open so an external driver
 * (scratchpad probe.py, raw W3C calls) can walk the 1.59 surfaces and capture the DOM the
 * build really renders before any locator is written. Ends when {@code /tmp/zp/hold/stop}
 * appears or after PROBE_HOLD_MIN minutes (default 40). Appium's newCommandTimeout (600 s)
 * is reset by every external command, so an active probe never times the session out.
 */
public final class ZP3914_SessionHold_Probe extends BaseTest {

    private static final Path DIR = Paths.get("/tmp/zp/hold");

    /** Explicit timeOut: GlobalTestTimeout only caps methods that set none (360 s). */
    @Test(timeOut = 3L * 60 * 60 * 1000)
    public void holdSessionForDiscovery() throws Exception {
        // PROBE_SKIP_LOGIN=true holds the session on the fresh-install screens (Welcome / sign-in /
        // terms / experience chooser) so those can be walked by hand.
        if (!Boolean.parseBoolean(System.getProperty("PROBE_SKIP_LOGIN", "false"))) loginAndSelectSite();
        Files.createDirectories(DIR);
        Files.deleteIfExists(DIR.resolve("stop"));
        String sid = String.valueOf(DriverManager.getDriver().getSessionId());
        Files.write(DIR.resolve("session.txt"), sid.getBytes(StandardCharsets.UTF_8));
        System.out.println("🔓 PROBE SESSION " + sid + " held — stop with: touch " + DIR.resolve("stop"));
        int minutes = Integer.parseInt(System.getProperty("PROBE_HOLD_MIN", "40"));
        long deadline = System.currentTimeMillis() + minutes * 60_000L;
        while (System.currentTimeMillis() < deadline && !Files.exists(DIR.resolve("stop"))) {
            Thread.sleep(2000);
        }
        System.out.println("🔒 PROBE SESSION released");
    }
}
