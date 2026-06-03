package com.egalvanic.explore;

import com.egalvanic.verify.VerificationError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Accumulates findings and turns the crawl's outcome into a build signal + artifact. */
public final class ExploreReport {

    private final List<ExploreFinding> findings = new ArrayList<>();
    private int steps;
    private WorkflowGraph graph;

    public void add(ExploreFinding f) { if (f != null) findings.add(f); }
    public void addAll(List<ExploreFinding> fs) { if (fs != null) findings.addAll(fs); }
    public void setSteps(int steps) { this.steps = steps; }
    public void setGraph(WorkflowGraph graph) { this.graph = graph; }

    public List<ExploreFinding> findings() { return findings; }
    public long count(ExploreFinding.Severity s) {
        return findings.stream().filter(f -> f.severity() == s).count();
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Exploratory crawl: ").append(steps).append(" steps, ")
          .append(findings.size()).append(" findings ")
          .append("(CRITICAL=").append(count(ExploreFinding.Severity.CRITICAL))
          .append(", HIGH=").append(count(ExploreFinding.Severity.HIGH))
          .append(", MEDIUM=").append(count(ExploreFinding.Severity.MEDIUM))
          .append(", LOW=").append(count(ExploreFinding.Severity.LOW)).append(")\n");
        findings.forEach(f -> sb.append("  ").append(f).append('\n'));
        if (graph != null) sb.append(graph.render());
        return sb.toString();
    }

    public void writeArtifact(String path) {
        try {
            Path p = Path.of(path);
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            Files.writeString(p, summary());
            System.out.println("📄 Exploration report written: " + path);
        } catch (IOException e) {
            System.out.println("⚠️ Could not write exploration report: " + e.getMessage());
        }
    }

    /** Fail the build only on high-confidence defects (crashes/freezes). */
    public void failIfCritical() {
        long c = count(ExploreFinding.Severity.CRITICAL);
        if (c > 0) {
            throw new VerificationError(c + " critical anomaly(ies) found during exploration:\n" + summary());
        }
    }
}
