# 117 — per-test triage of run 29135128275 + skip anatomy + arc-flash required-field domain rule

**Date:** 2026-07-13
**Prompt:** "check all the fail test case" + "so many skipped, correct all this;
in core attributes 'required field' only means required to create arc flash
value — they are not mandatory"

## Per-test triage (32-agent workflow over all 230 failures)

Full report: `docs/failure-triage-run-29135128275.md` (one verdict + evidence +
action per test). Verdicts on 207/230 (13 agents rate-limited, ~23 pending):

| Verdict | n | Meaning |
|---|---|---|
| AUTOMATION_BUG | 191 | our locator/timing/detection code |
| ENV_INFRA | 11 | breaker/wall-clock/sim cascade |
| INVALID_TEST | 2 | ATS toggle tests (fixed, changelog 116) |
| DATA_FIXTURE | 2 | missing test data |
| APP_BUG | 1 | product-side |

Top automation-bug families with actions: v1.49 IR-photos DOM remap
(SiteVisit_p2 ~10), unscoped giant-tree queries wedging WDA (SiteVisit_p1/p2
~40 — scope to expanded node + withImplicitWait(0) + WDA rebuild on timeout),
Issues v1.48 picker remap (~19), save-evidence detection (~12).

## Skip anatomy (865 skips in the after-rerun report)

- ~600 = dead-session cascades: a wedged WDA makes the NEXT driver init fail
  5× → breaker opens → rest of class skips at 0ms. Teardown already
  quits/rebuilds; the burn-down is the WEDGE SOURCES (giant-tree queries above).
- ~150 = correct guards: camera-crash quarantine (CAM-CRASH-01 app bug),
  precondition skips (fixtures), offline sub-suite gating.
- Rest = suite wall-clock (240 min) truncation.
Skips collapse as the wedge-source families get fixed — they are symptoms,
not 865 individual problems.

## Domain rule locked in (user, twice-confirmed)

Core-Attribute "required" fields = required only for the NFPA 70E / arc-flash
readiness calculation. NEVER save-mandatory. Only identity fields (Name) may
block save. Toggle = view filter. Audited: no remaining test asserts
save-blocking on core attributes (BUG_EDIT_01 targets Name — stays).

## Next (user ticket, queued)

Arc Flash Readiness ZP-2373 parity on iOS iPhone + iPad: adapt web automation
for the NFPA 70E per-asset-class dashboard, iPad form factor, touch/scroll/
responsive checks, CI integration.
