package com.egalvanic.explore;

import com.egalvanic.base.BaseTest;
import com.egalvanic.verify.AssetLoadVerifier;
import com.egalvanic.verify.CrashDetector;
import com.egalvanic.verify.UIStateValidator;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Autonomous exploratory crawl (Phase 3). Disabled by default — it is NOT in any suite
 * XML and additionally guards on {@code RUN_EXPLORATORY=true}, so it never affects normal
 * CI. Enable on the macOS runners with a live session:
 *
 *   RUN_EXPLORATORY=true mvn -Dtest=ExploratoryCrawlTest test
 *
 * AI is opt-in: set {@code ANTHROPIC_API_KEY} to enable risk-prioritised actions and
 * screenshot anomaly detection; otherwise it runs as a verifier-backed monkey.
 */
public class ExploratoryCrawlTest extends BaseTest {

    @Test
    public void exploratoryCrawl() {
        if (!"true".equalsIgnoreCase(System.getenv("RUN_EXPLORATORY"))) {
            throw new SkipException("Exploratory crawl disabled (set RUN_EXPLORATORY=true to enable).");
        }

        loginAndSelectSite(); // reuse the framework's real entry into an authenticated session

        AiClient ai = AiClient.fromEnv(); // null when no key => deterministic monkey + no vision
        ExploratoryEngine engine = new ExploratoryEngine(
                new Crawler(),
                new ActionSelector(ai),
                new Oracle(new CrashDetector(), new UIStateValidator(), new AssetLoadVerifier()),
                new VisualAnomalyDetector(ai),
                new InterruptInjector(),
                new WorkflowGraph());

        int minutes = parseEnv("EXPLORE_MINUTES", 20);
        int maxSteps = parseEnv("EXPLORE_MAX_STEPS", 800);

        ExploreReport report = engine.run(ExploreBudget.of(minutes, maxSteps));
        report.writeArtifact("reports/exploration/crawl-report.txt");
        System.out.println(report.summary());

        report.failIfCritical(); // crashes/freezes turn the build RED; lower-severity findings are logged
    }

    private static int parseEnv(String key, int dflt) {
        try { String v = System.getenv(key); return v == null ? dflt : Integer.parseInt(v); }
        catch (NumberFormatException e) { return dflt; }
    }
}
