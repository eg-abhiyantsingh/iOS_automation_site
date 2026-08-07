# 154 — Retry-loop bug + 18.5 typing truth + final caps → full-suite dispatch (2026-08-08)

## Proof run 31185473008 (worktype, all prior fixes): 4/9 green, big convergence
- details 87P/2F/17S — **DET_204 nav-hijack census PASSED on CI** (13/13)
- behavior 69P/1F/7S — **19 PM_FORMS skips converted** by the Assets candidate
- forms 43P/2F/1S — wedge timeouts gone this run; only notes-null remained
- list 106P/3F — 211/212 (900s caps) passed; 201/207 flip-flopped at 540s
- createform joined green (retry proven); cross/catalog/createpicker stayed green

## Three root causes closed this session
1. **ENV-RETRY infinite loop (framework bug, THE find of the run):**
   EnvironmentRetryAnalyzer stored its "one retry" flag on the ITestResult —
   but each retry gets a NEW result object, so the cap NEVER engaged. A test
   whose retry kept failing environmentally retried forever: E2E_032 looped
   ~3.5h (2:57→6:15 PM), wedged the driver, tripped the breaker → 19 cascade
   skips. Fix: static per-test-name ConcurrentHashMap cap. Self-tests 39/39.
2. **18.5 notes typing (D01 census ground truth):** the TextField was
   completely EMPTY after a "successful" element.click()+sendKeys — click
   does not focus SwiftUI TextFields on 18.5. typeStepNotes now
   coordinate-taps, confirms the keyboard, re-finds the field, types, and
   VERIFIES the text landed (loops once). FORM_035/036 still green on 26.2.
3. **LIST_201/207 flip-flop:** explicit timeOut=900_000 (same as 211/212).

## Dispatch
Full parallel suite (run_all) dispatched — the 95%-correct-report cycle:
in-run env-retry (now truly once) → 3-shard rerun triage → hand
classification of every surviving fail from artifacts → client report (web
template) states only verified verdicts.
