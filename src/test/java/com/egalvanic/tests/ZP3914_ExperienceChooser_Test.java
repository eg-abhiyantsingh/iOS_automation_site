package com.egalvanic.tests;

import com.egalvanic.base.ZP3914BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.ExperiencePage;
import com.egalvanic.pages.ExperiencePage.Experience;
import com.egalvanic.utils.DriverManager;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * ZP-3914 (iOS 1.59) — "Experience chooser" + "Site Walk: Account Manager Experience" (entry and persistence).
 *
 * The login flow already passes the post-sign-in chooser on every test (it always picks Field Technician), so
 * these cases exercise the chooser where a user meets it again: Settings › 'Switch Experience'. Every string is
 * asserted verbatim against the 1.69 QA build (probe 2026-10-21) and AppStrings.Experience (1.70 source).
 */
public final class ZP3914_ExperienceChooser_Test extends ZP3914BaseTest {

    private static final String FEATURE = "Experience chooser (ZP-3914)";

    private void openChooserOrSkip(String tcId) {
        loginAndWaitForTabs(tcId);
        boolean offered = accountManagerOffered(tcId);
        skipIfPreconditionMissing(() -> offered, tcId + ": no 'Switch Experience' row — the QA user holds a single"
                + " experience (needs role 'Account Manager' + company feature 'site-walks')");
        skipIfPreconditionMissing(() -> experience.openFromSettings(), tcId + ": 'Switch Experience' did not open the chooser");
    }

    @Test(priority = 1)
    public void TC_EXP_01_switchExperienceOpensChooserWithBothCards() {
        featureTest(FEATURE, "TC_EXP_01 - Settings › Switch Experience shows the chooser with both experiences, current one selected");
        openChooserOrSkip("TC_EXP_01");
        assertTrue(experience.isChooserShown(), "Chooser title '" + ExperiencePage.CHOOSER_TITLE + "' must be shown");
        assertTrue(experience.isSubtitleShown(), "Chooser subtitle '" + ExperiencePage.CHOOSER_SUBTITLE + "' must be shown");
        List<String> cards = experience.cardLabels();
        logStep("Cards: " + cards);
        assertEquals(cards, List.of(Experience.FIELD_TECHNICIAN.cardLabel(), Experience.ACCOUNT_MANAGER.cardLabel()),
                "The chooser must offer exactly Field Technician then Account Manager with the 1.59 descriptions");
        Map<Experience, Boolean> sel = experience.cardSelection();
        assertEquals(sel.get(Experience.FIELD_TECHNICIAN), Boolean.TRUE,
                "The CURRENT experience (Field Technician — the login always picks it) must be the highlighted card: " + sel);
        assertEquals(sel.get(Experience.ACCOUNT_MANAGER), Boolean.FALSE, "Account Manager must not be highlighted: " + sel);
        assertTrue(experience.isContinueEnabled(), "'Continue' must be enabled while a pickable card is highlighted");
        assertTrue(experience.tabBarLabels().isEmpty(), "The tab bar must be hidden while the chooser is pushed from Settings");
        assertTrue(experience.backToSettings(), "The chooser's back button ('Settings') must return to Settings");
        assertTrue(experience.isOn(Experience.FIELD_TECHNICIAN),
                "Backing out without Continue must keep the Field Technician tabs: " + experience.tabBarLabels());
        logStepWithScreenshot("TC_EXP_01: chooser contract verified");
    }

    @Test(priority = 2)
    public void TC_EXP_02_highlightMovesButNothingChangesWithoutContinue() {
        featureTest(FEATURE, "TC_EXP_02 - Tapping a card only highlights it; the experience changes on Continue");
        openChooserOrSkip("TC_EXP_02");
        assertTrue(experience.selectCard(Experience.ACCOUNT_MANAGER), "Tapping the Account Manager card must highlight it");
        Map<Experience, Boolean> sel = experience.cardSelection();
        assertEquals(sel.get(Experience.FIELD_TECHNICIAN), Boolean.FALSE,
                "Exactly one card may be highlighted — Field Technician must lose the highlight: " + sel);
        assertTrue(experience.backToSettings(), "Back must leave the chooser");
        assertTrue(experience.isOn(Experience.FIELD_TECHNICIAN),
                "Without Continue the experience must stay Field Technician — tabs " + experience.tabBarLabels());
        logStepWithScreenshot("TC_EXP_02: highlight-only verified");
    }

    @Test(priority = 3)
    public void TC_EXP_03_accountManagerShowsSiteWalksTabs() {
        featureTest(FEATURE, "TC_EXP_03 - Choosing Account Manager switches the app to Site Walks · Settings");
        openChooserOrSkip("TC_EXP_03");
        assertTrue(experience.selectCard(Experience.ACCOUNT_MANAGER), "Account Manager card must be selectable");
        assertTrue(experience.pressContinue(), "'Continue' must close the chooser");
        assertTrue(eventually(() -> experience.isOn(Experience.ACCOUNT_MANAGER), 20),
                "The Account Manager experience must show exactly the tabs " + ExperiencePage.ACCOUNT_MANAGER_TABS
                        + " — got " + experience.tabBarLabels());
        assertTrue(walks.waitForList(20_000), "Account Manager home must be the Site Walks list ('New Site Walk')");
        logStepWithScreenshot("TC_EXP_03: Account Manager home");
    }

    @Test(priority = 4)
    public void TC_EXP_04_experienceSurvivesAppRelaunch() {
        featureTest(FEATURE, "TC_EXP_04 - The chosen experience survives an app relaunch");
        openChooserOrSkip("TC_EXP_04");
        assertTrue(experience.selectCard(Experience.ACCOUNT_MANAGER) && experience.pressContinue(),
                "Switching to Account Manager must succeed");
        assertTrue(eventually(() -> experience.isOn(Experience.ACCOUNT_MANAGER), 20), "Account Manager tabs expected");
        logStep("Relaunching the app (terminate + activate)");
        DriverManager.getDriver().terminateApp(AppConstants.APP_BUNDLE_ID);
        sleep(800);
        DriverManager.getDriver().activateApp(AppConstants.APP_BUNDLE_ID);
        sleep(2500);
        assertTrue(waitForExperienceAfterRelaunch(Experience.ACCOUNT_MANAGER, 30),
                "After a relaunch the app must come back in the Account Manager experience (stored choice) — tabs "
                        + experience.tabBarLabels());
        assertFalse(experience.isChooserShown(), "A relaunch must not ask again once an experience is chosen");
        logStepWithScreenshot("TC_EXP_04: experience persisted across relaunch");
    }

    @Test(priority = 5)
    public void TC_EXP_05_switchBackToFieldTechnicianRestoresSiteTabs() {
        featureTest(FEATURE, "TC_EXP_05 - Switching back to Field Technician restores Site · Assets · Connections · SLD · Settings");
        openChooserOrSkip("TC_EXP_05");
        assertTrue(experience.selectCard(Experience.ACCOUNT_MANAGER) && experience.pressContinue(), "Switch to Account Manager");
        assertTrue(eventually(() -> experience.isOn(Experience.ACCOUNT_MANAGER), 20), "Account Manager tabs expected");
        assertTrue(experience.openFromSettings(), "Account Manager Settings must also offer 'Switch Experience'");
        Map<Experience, Boolean> sel = experience.cardSelection();
        assertEquals(sel.get(Experience.ACCOUNT_MANAGER), Boolean.TRUE,
                "Reopened from Account Manager, the highlighted card must be Account Manager: " + sel);
        assertTrue(experience.selectCard(Experience.FIELD_TECHNICIAN) && experience.pressContinue(), "Switch back to Field Technician");
        assertTrue(eventually(() -> experience.isOn(Experience.FIELD_TECHNICIAN), 20),
                "Field Technician must restore " + ExperiencePage.FIELD_TECH_TABS + " — got " + experience.tabBarLabels());
        logStepWithScreenshot("TC_EXP_05: back on Field Technician");
    }
}
