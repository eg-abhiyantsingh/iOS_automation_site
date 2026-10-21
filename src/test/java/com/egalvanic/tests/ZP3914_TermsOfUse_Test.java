package com.egalvanic.tests;

import com.egalvanic.api.TestDataApi;
import com.egalvanic.base.ZP3914BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.LegalConsentPage;
import com.egalvanic.utils.DriverManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.testng.annotations.Test;

import java.net.http.HttpResponse;
import java.util.List;

/**
 * ZP-3914 (iOS 1.59) — "Terms of Use acceptance" (ZP-3804 gate, LegalAcceptanceGateView.swift).
 *
 * What a QA tenant can prove: the QA user has accepted everything (any v1/v2 login records acceptance — the reason
 * the app moved to login v3/v4), so the GATE ITSELF ('I Agree', read-to-the-end, 'Decline & Sign Out') can only be
 * shown with a user whose acceptance is pending — that needs a backend-side document version bump and is out of
 * reach here. These cases pin everything around it: the backend contract the gate reads, that an accepted user is
 * never stopped by it (fresh sign-in AND relaunch), and the sign-in screen's implicit consent with working links.
 */
public final class ZP3914_TermsOfUse_Test extends ZP3914BaseTest {

    private static final String FEATURE = "Terms of Use acceptance (ZP-3914)";
    /** Gate copy (AppStrings.Legal) — none of it may show for an accepted user. */
    private static final List<String> GATE_TEXTS = List.of("I Agree", "I Agree — read to the end to enable",
            "Decline & Sign Out", "Read to the end to continue", "Checking your acceptance status…",
            "Couldn't verify acceptance");
    private static final String CONSENT_PREFIX = "By signing in, you agree to our";

    private LegalConsentPage legalPage;

    /** Created on first use: page objects bind to the live driver, which does not exist at class construction. */
    private LegalConsentPage legal() {
        if (legalPage == null) legalPage = new LegalConsentPage();
        return legalPage;
    }

    private JsonObject acceptanceCheck(String tcId) {
        TestDataApi a = requireApi(tcId);
        HttpResponse<String> r = a.get("/legal/acceptance/check");
        assertEquals(r.statusCode(), 200, "GET /legal/acceptance/check must answer 200 — body " + r.body());
        JsonElement e = JsonParser.parseString(r.body());
        assertTrue(e.isJsonObject(), "The acceptance check answers a JSON object — " + r.body());
        JsonObject o = e.getAsJsonObject();
        if (o.has("data") && o.get("data").isJsonObject() && !o.has("all_accepted")) o = o.getAsJsonObject("data");
        return o;
    }

    private String gateTextOnScreen() {
        return GATE_TEXTS.stream().filter(experience::isTextVisible).findFirst().orElse(null);
    }

    @Test(priority = 1)
    public void TC_TOU_01_acceptanceCheckContract() {
        featureTest(FEATURE, "TC_TOU_01 - GET /legal/acceptance/check: all_accepted (bool) + pending docs; the QA user has nothing pending");
        JsonObject o = acceptanceCheck("TC_TOU_01");
        logStep("Acceptance check: " + o);
        assertTrue(o.has("all_accepted") && o.get("all_accepted").isJsonPrimitive() && o.get("all_accepted").getAsJsonPrimitive().isBoolean(),
                "'all_accepted' must be a boolean — " + o);
        assertFalse(o.has("check_failed") && o.get("check_failed").isJsonPrimitive() && o.get("check_failed").getAsBoolean(),
                "The backend must not report check_failed for the QA user — " + o);
        assertTrue(o.get("all_accepted").getAsBoolean(), "The QA user has accepted the current terms — " + o);
        if (o.has("pending") && o.get("pending").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("pending");
            for (String doc : List.of("terms_and_conditions", "privacy_policy")) {
                assertTrue(!p.has(doc) || p.get(doc).isJsonNull(), "Nothing may be pending when all_accepted is true — " + doc + ": " + p.get(doc));
            }
        }
    }

    @Test(priority = 2)
    public void TC_TOU_02_acceptedUserIsNeverGated() {
        featureTest(FEATURE, "TC_TOU_02 - An accepted user reaches the app with no terms gate, also after a relaunch");
        loginAndWaitForTabs("TC_TOU_02");
        String gate = gateTextOnScreen();
        assertTrue(gate == null, "An accepted user must not see the terms gate after sign-in — saw '" + gate + "'");
        assertTrue(experience.isOn(com.egalvanic.pages.ExperiencePage.Experience.FIELD_TECHNICIAN), "Signed in to the app's tabs");
        DriverManager.getDriver().terminateApp(AppConstants.APP_BUNDLE_ID);
        sleep(800);
        DriverManager.getDriver().activateApp(AppConstants.APP_BUNDLE_ID);
        assertTrue(waitForExperienceAfterRelaunch(com.egalvanic.pages.ExperiencePage.Experience.FIELD_TECHNICIAN, 30),
                "After a relaunch the app must open straight into its tabs");
        String gate2 = gateTextOnScreen();
        assertTrue(gate2 == null, "A relaunch must not raise the terms gate for an accepted user — saw '" + gate2 + "'");
        logStepWithScreenshot("TC_TOU_02: no gate for an accepted user");
    }

    @Test(priority = 3, timeOut = 600_000)
    public void TC_TOU_03_signInShowsImplicitConsentWithLinks() {
        featureTest(FEATURE, "TC_TOU_03 - Sign-in states 'By signing in, you agree to our Terms & Conditions and Privacy Policy'; both links open the document");
        loginAndWaitForTabs("TC_TOU_03");
        assertTrue(experience.logoutFromSettings(), "Settings › Logout must sign out");
        try {
            // After a logout the app remembers the company: it opens the returning-user sign-in chooser ('Welcome
            // back', no company code, no email field) — the company-code screen only shows on a clean install.
            if (eventually(() -> welcomePage.isCompanyCodeScreenNow(), 8)) {
                welcomePage.submitCompanyCode(AppConstants.VALID_COMPANY_CODE);
            }
            assertTrue(eventually(() -> loginPage.revealPasswordFormIfNeeded(null), 30),
                    "The sign-in screen must load after logout and reveal its password form ('Use my password')");
            assertTrue(eventually(() -> loginPage.isConsentImplicit(), 10),
                    "Sign-in must state consent ('" + CONSENT_PREFIX + " …') instead of a Terms checkbox");
            assertTrue(legal().isConsentShown(), "The consent line reads '" + LegalConsentPage.CONSENT_PREFIX + " …'");
            assertTrue(legal().isLinkShown(LegalConsentPage.TERMS) && legal().isLinkShown(LegalConsentPage.PRIVACY),
                    "The consent line links both '" + LegalConsentPage.TERMS + "' and '" + LegalConsentPage.PRIVACY + "'");
            logStepWithScreenshot("Sign-in consent text");
            assertTrue(legal().openDocument(LegalConsentPage.TERMS), "'" + LegalConsentPage.TERMS + "' must open its document");
            logStepWithScreenshot("Terms & Conditions document");
            assertTrue(legal().closeDocument(), "The Terms document must close back to sign-in");
            assertTrue(legal().openDocument(LegalConsentPage.PRIVACY), "'" + LegalConsentPage.PRIVACY + "' must open its document");
            logStepWithScreenshot("Privacy Policy document");
            assertTrue(legal().closeDocument(), "The Privacy document must close back to sign-in");
        } finally {
            loginAndSelectSite();   // leave the class signed in for whatever runs next
        }
        String gate = gateTextOnScreen();
        assertTrue(gate == null, "Signing back in must not raise the gate for an accepted user — saw '" + gate + "'");
        logStepWithScreenshot("TC_TOU_03: consent + links verified, signed back in");
    }
}
