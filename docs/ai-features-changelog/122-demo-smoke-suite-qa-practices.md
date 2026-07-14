# 122 — demo smoke suite + QA practices codified

**Date:** 2026-07-14

- **Demo Smoke suite** (`testng-smoke-demo.xml`, CI `run_smoke_demo`,
  `smoke-demo-tests` job): 22 tests picked BY DATA — critical journey +
  PASSED run 29135128275 + <90s each (~7 min test time). Covers login,
  site selection, Equipment Library (flag canary + downloaded state),
  Arc Flash readiness (ring/stat/cards), Assets edit + create-visible,
  Issues list/create-entry, Connections header/new-form, Work Orders entry,
  ZP323 schedule.
- **Report retention 7 → 30 days** (trend lines for stakeholders).
- **docs/qa-practices.md**: flaky policy (RECOVERED=flaky, rate 1.8% vs <2%
  target, quarantine flow), triage taxonomy, pyramid targets, smoke/regression
  split, metrics, report-integrity rule (reports are never hand-edited —
  the honest root-cause story is the credible one).
