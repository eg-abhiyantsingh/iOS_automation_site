package com.egalvanic.api;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks platform-managed company feature flags (GET /auth/v2/me →
 * company_features) so flag-gated test modules can SKIP with a loud
 * environmental reason instead of producing hundreds of misleading FAILs.
 *
 * Born from the eng-lib incident (changelog 109/110): the flag was silently
 * removed from acme QA on 2026-07-07; the Settings card kept its normal
 * "Not yet downloaded" subtitle but the tap became a no-op, so all 105
 * asset_engineer tests failed with "alert never appeared" — indistinguishable
 * from a script bug without this check.
 *
 * Semantics — deliberately FAIL-OPEN:
 *  - DISABLED is returned ONLY when the API positively answered and the flag
 *    is absent from the company_features array. Only this verdict may skip tests.
 *  - Any login/network/parse problem is INDETERMINATE: the tests run (and fail
 *    naturally if the environment is truly broken). An API blip must never
 *    silently skip a whole module.
 *  - Verdicts are cached per JVM: one API round-trip per flag per run.
 *  - Bypass for debugging the disabled UI itself: -DFEATURE_GATE_OFF=true
 *    (or env FEATURE_GATE_OFF=true).
 */
public final class CompanyFeatureGate {

    public enum Verdict { ENABLED, DISABLED, INDETERMINATE }

    private static final Map<String, Verdict> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> DETAIL = new ConcurrentHashMap<>();

    private CompanyFeatureGate() { }

    /** True when the gate is globally bypassed (-DFEATURE_GATE_OFF / env). */
    public static boolean gateBypassed() {
        String prop = System.getProperty("FEATURE_GATE_OFF",
                System.getenv().getOrDefault("FEATURE_GATE_OFF", "false"));
        return Boolean.parseBoolean(prop);
    }

    /** Cached verdict for the flag; hits the QA API once per JVM per flag. */
    public static Verdict check(String flag) {
        if (flag == null || flag.isEmpty()) return Verdict.INDETERMINATE;
        if (gateBypassed()) {
            System.out.println("⚠️ CompanyFeatureGate: bypassed via FEATURE_GATE_OFF — '" + flag + "' not checked");
            return Verdict.INDETERMINATE;
        }
        return CACHE.computeIfAbsent(flag, CompanyFeatureGate::fetchVerdict);
    }

    /** Human detail behind the last verdict for the flag (for skip messages). */
    public static String detail(String flag) {
        return DETAIL.getOrDefault(flag, "no check performed");
    }

    private static Verdict fetchVerdict(String flag) {
        try {
            TestDataApi api = new TestDataApi();
            api.login();
            HttpResponse<String> resp = api.get("/auth/v2/me");
            if (resp.statusCode() / 100 != 2) {
                DETAIL.put(flag, "GET /auth/v2/me returned HTTP " + resp.statusCode());
                System.out.println("⚠️ CompanyFeatureGate: '" + flag + "' INDETERMINATE — " + detail(flag)
                        + " (failing OPEN, tests will run)");
                return Verdict.INDETERMINATE;
            }
            Verdict v = verdictFromMeJson(resp.body(), flag);
            switch (v) {
                case DISABLED:
                    DETAIL.put(flag, "flag absent from company_features (" + featureCount(resp.body()) + " flags present)");
                    System.out.println("🚫 CompanyFeatureGate: '" + flag + "' is DISABLED for this company — "
                            + detail(flag) + "; gated tests will SKIP");
                    break;
                case ENABLED:
                    DETAIL.put(flag, "flag present in company_features");
                    System.out.println("✅ CompanyFeatureGate: '" + flag + "' enabled");
                    break;
                default:
                    DETAIL.put(flag, "company_features array not found in /auth/v2/me response");
                    System.out.println("⚠️ CompanyFeatureGate: '" + flag + "' INDETERMINATE — " + detail(flag)
                            + " (failing OPEN, tests will run)");
            }
            return v;
        } catch (Exception e) {
            DETAIL.put(flag, "check errored: " + e.getMessage());
            System.out.println("⚠️ CompanyFeatureGate: '" + flag + "' INDETERMINATE — " + detail(flag)
                    + " (failing OPEN, tests will run)");
            return Verdict.INDETERMINATE;
        }
    }

    /**
     * Pure parser (self-testable without a network): DISABLED only when the
     * company_features array was found AND lacks an exact quoted match of the
     * flag ("eng-lib" must not match "eng-library"). Public so the driver-free
     * self-test suite (com.egalvanic.verify) can prove the semantics.
     */
    public static Verdict verdictFromMeJson(String meJson, String flag) {
        if (meJson == null || flag == null || flag.isEmpty()) return Verdict.INDETERMINATE;
        Matcher arr = featuresArray(meJson);
        if (!arr.find()) return Verdict.INDETERMINATE;
        boolean present = Pattern.compile("\"" + Pattern.quote(flag) + "\"")
                .matcher(arr.group(1)).find();
        return present ? Verdict.ENABLED : Verdict.DISABLED;
    }

    private static int featureCount(String meJson) {
        Matcher arr = featuresArray(meJson);
        if (!arr.find()) return 0;
        Matcher items = Pattern.compile("\"[^\"]*\"").matcher(arr.group(1));
        int n = 0;
        while (items.find()) n++;
        return n;
    }

    /** company_features is a flat string array in /auth/v2/me, so [^\]]* is safe. */
    private static Matcher featuresArray(String meJson) {
        return Pattern.compile("\"company_features\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(meJson);
    }
}
