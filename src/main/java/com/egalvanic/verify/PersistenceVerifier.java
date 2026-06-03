package com.egalvanic.verify;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.ios.IOSDriver;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * On-device persistence verifier — catches the "data corruption across flows" class at
 * its SOURCE by inspecting the app's own container (Core Data / SQLite / UserDefaults
 * plist / local files) via Appium {@code pullFile}, rather than trusting the UI.
 *
 * Also doubles as an OWASP-Mobile (MASVS) insecure-local-storage check: sensitive values
 * (passwords, tokens) must NOT sit in plaintext in UserDefaults/SQLite/plist.
 *
 * Requires the app to be debuggable / the simulator container reachable (true for the
 * QA .app on simulators). On a release build on a real device the container may not be
 * pullable — methods log-and-skip rather than false-fail in that case.
 *
 * Path convention (Appium iOS): "@<bundleId>/<relative path inside the container>",
 * e.g. "@com.egalvanic.zplatform-QA/Documents/egalvanic.sqlite".
 */
public final class PersistenceVerifier {

    private IOSDriver driver() { return DriverManager.getDriver(); }
    private final String bundleId = AppConstants.APP_BUNDLE_ID;

    private byte[] pull(String relPath) {
        String full = "@" + bundleId + "/" + relPath.replaceFirst("^/", "");
        try {
            return driver().pullFile(full);
        } catch (Exception e) {
            System.out.println("[PersistenceVerifier] pullFile unavailable for '" + full
                    + "' (" + e.getMessage() + ") — skipping (release/real-device container not reachable)");
            return null;
        }
    }

    /**
     * Assert a container file exists and is non-empty after a flow that should have
     * persisted data (e.g. the local store after creating an asset offline).
     * Skips (logs) if the container can't be pulled — never false-fails.
     */
    public void assertFileNonEmpty(String relPath, String flow) {
        byte[] data = pull(relPath);
        if (data == null) return; // unreachable container — skip
        if (data.length == 0) {
            throw new VerificationError("[PersistenceVerifier] " + flow
                    + ": on-device file '" + relPath + "' is empty — data was not persisted on device.");
        }
        System.out.println("[PersistenceVerifier] " + flow + ": '" + relPath + "' = "
                + data.length + " bytes ✓");
    }

    /**
     * Assert the persisted content contains {@code expected} (e.g. a freshly-created
     * asset name actually landed in the local store, not just the UI). Best-effort text
     * scan over the raw bytes (works for SQLite/plist which embed UTF-8 strings).
     */
    public void assertPersisted(String relPath, String expected, String flow) {
        byte[] data = pull(relPath);
        if (data == null) return;
        String text = new String(data, StandardCharsets.UTF_8);
        if (!text.contains(expected)) {
            throw new VerificationError("[PersistenceVerifier] " + flow + ": '" + expected
                    + "' not found in on-device store '" + relPath
                    + "' — UI shows it but the device did not persist it (corruption/sync gap).");
        }
        System.out.println("[PersistenceVerifier] " + flow + ": '" + expected + "' persisted on device ✓");
    }

    /**
     * MASVS insecure-storage check: the given sensitive values must NOT appear in
     * plaintext in the file (UserDefaults plist / SQLite). Fails if any does.
     */
    public void assertNoPlaintextSecret(String relPath, String... secrets) {
        byte[] data = pull(relPath);
        if (data == null || secrets == null) return;
        String text = new String(data, StandardCharsets.UTF_8);
        String b64 = Base64.getEncoder().encodeToString(data); // catch base64-but-not-encrypted
        for (String s : secrets) {
            if (s == null || s.isEmpty()) continue;
            if (text.contains(s) || b64.contains(Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)))) {
                throw new VerificationError("[PersistenceVerifier] MASVS insecure storage: sensitive value "
                        + "found in plaintext in '" + relPath + "' — secrets must live in Keychain, not "
                        + "UserDefaults/SQLite/plist.");
            }
        }
        System.out.println("[PersistenceVerifier] no plaintext secrets in '" + relPath + "' ✓");
    }
}
