＃ 037 — Cascading Test Failure Fix (203 failures → probably ~10 real bugs)

**Date**: 2026-04-21
**Prompt**: "check why so many test case are fail" → "yes do it without asking"
**Source run**: https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24559960369

---

## The Diagnosis

Analyzed all 238 failures across run #24559960369 and found the dominant pattern:

| Suite | Tests 1→N Pass | Trigger Test Fails | N+1 → End |
|---|---|---|---|
| **Issues P1** | TC_001–041 all pass (~60s each) | **TC_ISS_042** | 61 tests fail (~235s each) |
| **Connections** | TC_001–046 all pass (~40s each) | **TC_CONN_047** | 44 tests fail (~125s each) |

One test breaks app state → every subsequent test can't navigate back → cascade. **The ~235s failure duration is tests burning through the 4-minute per-test timeout trying to find the "Issues screen" that's covered by a modal sheet.**

### Root cause per cascade

1. **TC_ISS_042** taps "Select Asset" to open the picker, then asserts the picker is displayed. When the assertion fails, the test's cleanup code (`tapCancelAssetPicker()`, `tapCancelNewIssue()`) never runs. **The modal stays open.** Next test's `ensureOnIssuesScreen()` tries to tap the Issues tab, but the tab bar is hidden under the modal.

2. **TC_CONN_047** opens Connection details + tries Edit mode. Same pattern: on assertion failure, the detail screen stays open and blocks the Connections tab.

## The Fix

Two complementary changes in each affected test class:

### 1. `@AfterMethod` cleanup that dismisses any open modal/screen

Runs after every test regardless of pass/fail. Uses the existing safe `tapCancel*()` / `goBackFrom*()` / `dismiss*()` methods (all internally try/caught) to close anything that might be open.

```java
@AfterMethod(alwaysRun = true)
public void dismissAnyOpenPickerOrSheet() {
    try {
        if (issuePage == null) return;
        if (issuePage.isIssuesScreenDisplayed()) return;
        try { issuePage.tapCancelAssetPicker(); } catch (Exception ignored) {}
        try { issuePage.tapCancelNewIssue(); } catch (Exception ignored) {}
    } catch (Exception ignored) {}
}
```

### 2. `ensureOnXxxScreen()` becomes modal-aware

Before attempting navigation (which fails if a modal is blocking the tab bar), try dismissing any open modal first:

```java
private boolean ensureOnIssuesScreen() {
    if (issuePage.isIssuesScreenDisplayed()) return true;

    // Dismiss any modal left open by a prior test BEFORE attempting tab navigation.
    // Tab bar is hidden under modal sheets — without this, the Issues tab tap can't register.
    try { issuePage.tapCancelAssetPicker(); } catch (Exception ignored) {}
    try { issuePage.tapCancelNewIssue(); } catch (Exception ignored) {}

    // ... existing navigation logic ...
}
```

## Files Modified

1. [src/test/java/com/egalvanic/tests/Issue_Phase1_Test.java](../../src/test/java/com/egalvanic/tests/Issue_Phase1_Test.java) — added `@AfterMethod dismissAnyOpenPickerOrSheet()`, modal-dismiss prelude in `ensureOnIssuesScreen()`
2. [src/test/java/com/egalvanic/tests/Issue_Phase2_Test.java](../../src/test/java/com/egalvanic/tests/Issue_Phase2_Test.java) — same changes (helper was copy-pasted across both files)
3. [src/test/java/com/egalvanic/tests/Connections_Test.java](../../src/test/java/com/egalvanic/tests/Connections_Test.java) — added `@AfterMethod dismissAnyOpenDialogOrScreen()` using `goBackFromConnectionDetails`, `dismissSourceNodeDropdown`, `dismissTargetNodeDropdown`, `dismissConnectionTypeDropdown`, `dismissOptionsMenu`, `tapOnCancelButton` — plus modal-dismiss prelude in `ensureOnConnectionsScreen()`

## Expected Impact

Before: 238 total failures
- 53 "Should be on Issues screen"
- 38 "Should be on Connections screen"
- 53 ThreadTimeoutException (cascade tails)
- 8 "Should be on New Issue form"

After (expected): **~150 fewer failures**. Remaining ~85 failures should be:
- Actual product bugs (the ~10 root causes)
- 34 Offline tests (driver init issue — separate fix needed)
- Assets-P2/P3/P6 failures unrelated to this cascade

## What This Does NOT Fix

- **The trigger tests themselves** (TC_ISS_042, TC_CONN_047) — these have their own bugs (likely locator drift on the picker detection). They still need separate investigation — but now when they fail, they fail ONCE instead of poisoning 60+ downstream tests.
- **Offline suite 100% skip** (34 tests) — driver init issue, unrelated pattern.
- **6-hour timeout on 5 cancelled jobs** — need test suite splits or per-test speed-ups, not covered here.

## Verification

- `mvn compile test-compile -q` passes cleanly — no new warnings, no errors.
- Both @AfterMethod bodies are wrapped in top-level try/catch so a failing cleanup can't mask a test failure or break the @AfterMethod chain.
- All dismissal methods used are confirmed safe (read their source — each has internal try/catch).

## Memory Saved This Turn

[~/.claude/projects/.../memory/feedback_execute_without_asking.md](~/.claude/projects/-Users-abhiyantsingh-Downloads-iOS-automation-site/memory/feedback_execute_without_asking.md) — "once user approves a plan, implement immediately; don't re-ask"
