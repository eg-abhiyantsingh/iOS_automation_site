package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * ZP-3928 (iOS 1.56) §6 — decimal point on the number pad.
 *
 * Extends the Thermal temperature family (TC_ISS_159-163 already assert the numeric
 * keyboards). The decisive oracle is the ROUND TRIP: type 72.5, save, reopen, and read
 * back exactly "72.5". A keyboard-shows-a-dot check would pass even if the field silently
 * dropped the separator and stored 725 — a 10x data error on a temperature reading.
 */
public final class ZP3928_NumberPadDecimal_Test extends BaseTest {

    private static final String FEATURE = "Number pad decimals (ZP-3928)";
    private static final String DECIMAL_PROBLEM = "72.5";
    private static final String DECIMAL_REF     = "70.5";
    /** An issue that is already Thermal on the automation site (Problem/Reference Temp exposed, 2026-10-02). */
    private static final String THERMAL_TITLE_PREFIX = "Thermal Anomaly on ";

    private IssuePage issuePage;

    /** The driver is re-created per test; never reuse a page bound to a quit session ("Session ID is null"). */
    @BeforeMethod(alwaysRun = true)
    public void resetPages() { issuePage = null; }
    private IssuePage issues() {
        if (issuePage == null) issuePage = new IssuePage();
        return issuePage;
    }

    /**
     * Open the first issue's details on a THERMAL issue — the temperature fields only exist for that
     * class. Probe 1.63: the automation site's first issues are NEC / NFPA 70B violations, so the
     * class is switched on the details screen when needed (changeIssueClassOnDetails), and the run
     * SKIPs, not fails, if that cannot be done.
     */
    private void openFirstIssueDetails() {
        loginAndSelectSite();
        if (!issues().navigateToIssuesScreen()) throw new SkipException("Issues screen did not open");
        issues().tapAllTab();
        mediumWait();
        // Prefer an issue that is ALREADY Thermal (no class mutation on the shared QA site); fall back to
        // the first issue + class switch only when the site has none.
        if (!issues().openIssueByTitlePrefix(THERMAL_TITLE_PREFIX)) {
            if (!issues().tapFirstIssue()) throw new SkipException("No issue available to open");
            mediumWait();
        }
        if (!issues().isIssueDetailsScreenDisplayed()) {
            throw new SkipException("Issue Details did not open — cannot reach the temperature fields");
        }
        if (!issues().isProblemTempFieldPresent()) {
            logStep("Not a Thermal issue — switching the class to Thermal to expose the temperature fields");
            boolean changed = issues().changeIssueClassOnDetails("Thermal");
            mediumWait();
            skipIfPreconditionMissing(() -> changed && issues().isProblemTempFieldPresent(),
                    "no Thermal issue available and the class could not be switched on the details screen");
        }
    }

    /** Save, close, reopen the SAME issue (matched by title) and land back on details. Null title ⇒ skip. */
    private void saveCloseAndReopen(String title) {
        // Coordinate press + verified (the old tapSaveChangesButton() click() is a silent no-op on 1.67 —
        // nothing was saved, so the reopen read an empty field).
        assertTrue(issues().commitIssueChanges(), "'Save Changes' must commit the typed temperature");
        mediumWait();
        issues().cancelSheetIfOpen();            // 'Close' when the details stayed open after Save
        mediumWait();
        if (!issues().navigateToIssuesScreen()) throw new SkipException("Issues screen did not reopen after save");
        issues().tapAllTab();
        mediumWait();
        // Reopen the SAME issue by title — position-based reopening broke when the list reordered.
        if (title == null || !issues().openIssueByTitlePrefix(title)) {
            throw new SkipException("could not reopen '" + title + "' after saving");
        }
        String reopened = issues().openIssueTitle();
        skipIfPreconditionMissing(() -> title != null && title.equals(reopened),
                "the list reordered after save (reopened '" + reopened + "', expected '" + title + "') — round trip "
                + "cannot be attributed to the same issue");
        issues().isProblemTempFieldPresent();    // scrolls the Thermal fields into view
    }

    /** Numbers can come back as "72.5", "72.5 °F" or "72.50" — compare on the numeric value. */
    private static Double numeric(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || ".".equals(cleaned)) return null;
        try { return Double.parseDouble(cleaned); } catch (NumberFormatException e) { return null; }
    }

    @Test(priority = 2)
    public void TC_NPD_02_problemTempKeepsItsDecimal() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_NPD_02 - 72.5 saves and reads back as 72.5, not 725 (save → reopen round trip)");
        openFirstIssueDetails();
        String title = issues().openIssueTitle();
        logStep("Issue under test: '" + title + "'");

        // Always make a REAL edit: an earlier run may already have saved 72.5 here, and re-typing the stored value
        // leaves 'Save Changes' disabled (nothing to save) — TC_NPD_02 failed exactly so on 2026-10-05.
        Double current = numeric(issues().getProblemTempValue());
        final double target = (current != null && Math.abs(current - 72.5) < 0.001) ? 73.5 : 72.5;
        logStep("Problem Temp currently " + current + " → typing " + target);
        issues().enterProblemTemp(String.valueOf(target));
        mediumWait();
        String typed = issues().getProblemTempValue();
        Double gotTyped = numeric(typed);
        logStep("Problem Temp reads back before save: " + typed + "  (numeric " + gotTyped + ")");
        assertTrue(gotTyped != null && Math.abs(gotTyped - target) < 0.001,
                "The field must hold " + target + " right after typing — it holds '" + typed + "'. A value 10x larger "
                + "means the number pad swallowed the '.', a 10x error on a temperature reading.");

        logStep("Saving, closing and reopening the same issue");
        saveCloseAndReopen(title);
        String read = issues().getProblemTempValue();
        Double got = numeric(read);
        logStep("Problem Temp after reopen: " + read + "  (numeric " + got + ")");
        assertTrue(got != null && Math.abs(got - target) < 0.001,
                "The decimal must SURVIVE the save: expected " + target + " after reopen but the field holds " + got
                + " (raw '" + read + "'). Typing correctly but storing 10x (or dropping the value) is the data-loss "
                + "case this ticket exists to prevent.");
        logStepWithScreenshot("TC_NPD_02: " + target + " round-tripped through save");
    }

    @Test(priority = 3)
    public void TC_NPD_03_referenceTempKeepsItsDecimal() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_NPD_03 - Reference Temp accepts a decimal");
        openFirstIssueDetails();

        logStep("Typing " + DECIMAL_REF + " into Reference Temp");
        issues().enterReferenceTemp(DECIMAL_REF);
        mediumWait();

        String read = issues().getReferenceTempValue();
        Double got = numeric(read);
        logStep("Reference Temp reads back: " + read + "  (numeric " + got + ")");
        assertTrue(got != null,
                "Reference Temp should hold a numeric value after typing " + DECIMAL_REF
                + " — it read '" + read + "'");
        assertTrue(Math.abs(got - 70.5) < 0.001,
                "Reference Temp must keep its decimal: expected 70.5, got " + got + " (raw '" + read + "')");
        logStepWithScreenshot("TC_NPD_03: decimal preserved in Reference Temp");
    }

    @Test(priority = 4)
    public void TC_NPD_04_secondDecimalPointIsHandled() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_NPD_04 - A second decimal point does not corrupt the value");
        openFirstIssueDetails();

        logStep("Typing the malformed value 72.5.5");
        issues().enterProblemTemp("72.5.5");
        mediumWait();

        String read = issues().getProblemTempValue();
        logStep("Field holds: " + read);
        // App truth first: whatever it keeps, it must be a parseable single number,
        // never a corrupt string that later breaks Delta T.
        Double got = numeric(read);
        assertTrue(read == null || read.isEmpty() || got != null,
                "After a double-decimal entry the field must hold either nothing or a single parseable "
                + "number — it holds '" + read + "', which would break the Delta T calculation");
        verifyAppAlive("after typing a malformed decimal");
        logStepWithScreenshot("TC_NPD_04: malformed decimal handled safely");
    }
}
