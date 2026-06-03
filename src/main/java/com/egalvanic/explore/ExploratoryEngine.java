package com.egalvanic.explore;

import java.util.List;
import java.util.Random;

/**
 * The exploratory loop:
 *   scan → choose action → perform → run Oracle (Phase-2 verifiers) → AI visual check →
 *   record transition → periodically inject an iOS interrupt and re-check.
 *
 * AI components are optional; with no {@link AiClient} this is a verifier-backed monkey,
 * which already catches crashes, blank screens, stuck spinners and error alerts that the
 * scripted happy-path suite structurally cannot.
 */
public final class ExploratoryEngine {

    private final Crawler crawler;
    private final ActionSelector selector;
    private final Oracle oracle;
    private final VisualAnomalyDetector visual;
    private final InterruptInjector interrupts;
    private final WorkflowGraph graph;
    private final Random rnd = new Random();

    public ExploratoryEngine(Crawler crawler, ActionSelector selector, Oracle oracle,
                             VisualAnomalyDetector visual, InterruptInjector interrupts,
                             WorkflowGraph graph) {
        this.crawler = crawler;
        this.selector = selector;
        this.oracle = oracle;
        this.visual = visual;
        this.interrupts = interrupts;
        this.graph = graph;
    }

    public ExploreReport run(ExploreBudget budget) {
        ExploreReport report = new ExploreReport();
        report.setGraph(graph);

        int steps = 0;
        String prevScreen = crawler.screenSignature();

        while (!budget.exhausted(steps)) {
            steps++;
            List<UiNode> nodes = crawler.scan();
            Action action = selector.choose(nodes, "step " + steps);

            if (action.isNone()) {
                report.add(ExploreFinding.low("DEAD_END", "step " + steps,
                        "no interactable nodes; relaunching: " + interrupts.relaunch()));
                report.addAll(oracle.check("after recovery relaunch"));
                prevScreen = crawler.screenSignature();
                continue;
            }

            String label = action.node().describe();
            try {
                perform(action);
            } catch (Exception e) {
                report.add(ExploreFinding.medium("ACTION_FAILED", label, e.getMessage()));
            }

            report.addAll(oracle.check("after " + action.kind() + " " + label));
            report.add(visual.inspect("after " + label));

            String screen = crawler.screenSignature();
            graph.record(prevScreen, action.kind() + ":" + label, screen);
            prevScreen = screen;

            if (steps % 15 == 0) {
                String what = interrupts.fireRandom(rnd);
                report.addAll(oracle.check("after interrupt: " + what));
            }
        }

        report.setSteps(steps);
        return report;
    }

    private void perform(Action a) {
        if ("type".equals(a.kind())) {
            try { a.node().element.clear(); } catch (Exception ignored) {}
            a.node().element.sendKeys(a.input());
        } else {
            a.node().element.click();
        }
    }
}
