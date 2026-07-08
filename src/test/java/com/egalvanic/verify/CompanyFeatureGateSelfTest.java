package com.egalvanic.verify;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.egalvanic.api.CompanyFeatureGate;
import com.egalvanic.api.CompanyFeatureGate.Verdict;

/**
 * Driver-free proof of the CompanyFeatureGate semantics (changelog 110).
 * The gate may ONLY skip tests on a positively-confirmed absent flag —
 * every ambiguous input must fail OPEN (INDETERMINATE), because an API blip
 * silently skipping 105 tests is worse than the tests failing honestly.
 */
public class CompanyFeatureGateSelfTest {

    private static final String ME_WITH_ENGLIB =
            "{\"id\":\"u1\",\"company_features\":[\"emp\",\"eng-lib\",\"ops-core\"],\"email\":\"x@y.z\"}";
    private static final String ME_WITHOUT_ENGLIB =
            "{\"id\":\"u1\",\"company_features\":[\"emp\",\"ops-core\",\"reporting-v2\"],\"email\":\"x@y.z\"}";

    @Test
    public void flagPresent_isEnabled() {
        assertEquals(CompanyFeatureGate.verdictFromMeJson(ME_WITH_ENGLIB, "eng-lib"), Verdict.ENABLED,
                "flag present in company_features must be ENABLED");
    }

    @Test
    public void flagAbsent_isDisabled_theOnlySkippingVerdict() {
        assertEquals(CompanyFeatureGate.verdictFromMeJson(ME_WITHOUT_ENGLIB, "eng-lib"), Verdict.DISABLED,
                "flag absent from a present company_features array must be DISABLED (real 2026-07-07 incident shape)");
    }

    @Test
    public void substringFlagNames_mustNotFalseMatch() {
        String me = "{\"company_features\":[\"eng-library-v2\",\"xeng-lib\"]}";
        assertEquals(CompanyFeatureGate.verdictFromMeJson(me, "eng-lib"), Verdict.DISABLED,
                "'eng-lib' must not substring-match 'eng-library-v2' or 'xeng-lib'");
        assertEquals(CompanyFeatureGate.verdictFromMeJson(me, "eng-library-v2"), Verdict.ENABLED,
                "the longer sibling flag itself must still match exactly");
    }

    @Test
    public void emptyFeaturesArray_isDisabled() {
        assertEquals(CompanyFeatureGate.verdictFromMeJson("{\"company_features\":[]}", "eng-lib"), Verdict.DISABLED,
                "an explicitly empty company_features array is a positive 'no flags' answer");
    }

    @Test
    public void missingFeaturesKey_failsOpen() {
        assertEquals(CompanyFeatureGate.verdictFromMeJson("{\"id\":\"u1\",\"email\":\"x@y.z\"}", "eng-lib"),
                Verdict.INDETERMINATE,
                "response without company_features must fail OPEN, never DISABLED");
    }

    @Test
    public void malformedInputs_failOpen() {
        assertEquals(CompanyFeatureGate.verdictFromMeJson(null, "eng-lib"), Verdict.INDETERMINATE,
                "null body must fail OPEN");
        assertEquals(CompanyFeatureGate.verdictFromMeJson("<html>502 Bad Gateway</html>", "eng-lib"),
                Verdict.INDETERMINATE, "non-JSON body must fail OPEN");
        assertEquals(CompanyFeatureGate.verdictFromMeJson(ME_WITH_ENGLIB, null), Verdict.INDETERMINATE,
                "null flag must fail OPEN");
        assertEquals(CompanyFeatureGate.verdictFromMeJson(ME_WITH_ENGLIB, ""), Verdict.INDETERMINATE,
                "empty flag must fail OPEN");
    }

    @Test
    public void liveResponseShape_fromIncidentDay_parses() {
        // Verbatim shape returned by acme QA on 2026-07-08 (flag absent).
        String live = "{\"company_features\": [\"eg-forms\", \"ai_blueprint_recommendations\","
                + " \"weekly_opportunity_emails\", \"weekly_ops_emails\", \"panel-schedule-builder\","
                + " \"reporting-v2\", \"emp\", \"account_emails\", \"asset_bulk_impexp\","
                + " \"bulk-modify-classes\", \"bulk-ops\", \"bulk-pm-settings\", \"bulk-upload\","
                + " \"export-site\", \"sales-core\", \"ai_opportunities\", \"sales-agent\","
                + " \"on_demand_ai_actions\", \"ai_scheduling\", \"work_order_emails\", \"ops-core\"]}";
        assertEquals(CompanyFeatureGate.verdictFromMeJson(live, "eng-lib"), Verdict.DISABLED,
                "the exact live incident payload must yield DISABLED");
        assertEquals(CompanyFeatureGate.verdictFromMeJson(live, "emp"), Verdict.ENABLED,
                "a flag that IS live-present must yield ENABLED");
    }

    @Test
    public void bypassFlag_shortCircuitsToIndeterminate() {
        String prev = System.getProperty("FEATURE_GATE_OFF");
        try {
            System.setProperty("FEATURE_GATE_OFF", "true");
            assertTrue(CompanyFeatureGate.gateBypassed(), "sysprop must engage the bypass");
            assertEquals(CompanyFeatureGate.check("eng-lib"), Verdict.INDETERMINATE,
                    "bypassed gate must never skip (and must not hit the network)");
        } finally {
            if (prev == null) System.clearProperty("FEATURE_GATE_OFF");
            else System.setProperty("FEATURE_GATE_OFF", prev);
        }
    }
}
