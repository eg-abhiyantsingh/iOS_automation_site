package com.egalvanic.explore;

import com.egalvanic.utils.DriverManager;
import org.openqa.selenium.OutputType;

/** AI vision oracle. No-op (returns null) when no {@link AiClient} is configured. */
public final class VisualAnomalyDetector {

    private final AiClient ai; // nullable

    public VisualAnomalyDetector(AiClient ai) { this.ai = ai; }

    public ExploreFinding inspect(String step) {
        if (ai == null) return null;
        try {
            String b64 = DriverManager.getDriver().getScreenshotAs(OutputType.BASE64);
            String verdict = ai.classifyScreenshot(b64, step);
            if (verdict != null && !verdict.toLowerCase().startsWith("ok")) {
                return ExploreFinding.high("VISUAL_ANOMALY", step, verdict);
            }
        } catch (Exception ignored) { /* vision is advisory; never break the crawl */ }
        return null;
    }
}
