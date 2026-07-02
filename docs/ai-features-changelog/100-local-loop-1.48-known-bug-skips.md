# 100 — Local loop on v1.48: known-bug skips for ATS_ECR_07/10 + strict Create-button checker

**Date:** 2026-07-02
**User direction:** "ats name trimming is bug 07 and 10 so skip that" — both are
confirmed product bugs; skip them instead of failing the suite.

## Local one-test loop results (v1.48, iPhone 17 Pro Max, iOS 26.2)
- `CAP_EAD_05_editAPhaseSerialNumber` — **PASS** (1m29s): the CI save-evidence bucket
  is not reproducible locally; save flow is sound on 1.48.
- `ATS_ECR_10_verifyNameTrimming` — reproduced the product bug live: created asset
  unfindable by trimmed OR original name (untrimmed storage breaks lookup).
- `ATS_ECR_07_verifyNameWithOnlySpaces` — exposed a **vacuous pass**: the enabled-check
  swallowed element-not-found into `false`, so the test "passed" without ever seeing
  the Create button.

## Changes
1. **BUGS.md Category 3**: new entries **ATS-VAL-01** (Create enabled for spaces-only
   name) and **ATS-VAL-02** (asset name stored untrimmed; breaks lookup).
2. **ATS_ECR_07 / ATS_ECR_10** now SKIP with the bug id when the buggy behavior is
   observed — and **auto-pass once the app fix ships** (the checks stay live; only the
   failure outcome is converted to a documented skip).
3. **AssetPage.isCreateAssetButtonEnabledStrict()** — tri-state (enabled / disabled /
   NULL = not locatable) with the same multi-strategy locators clickCreateAsset uses,
   plus an any-type fallback for SwiftUI disabled-button rendering. ATS_ECR_07 treats
   NULL as a precondition skip ("cannot verify"), never as a pass.

## Validation
- Compile clean; `ATS_ECR_10` SKIPs with `KNOWN APP BUG ATS-VAL-02`; `ATS_ECR_07`
  SKIPs honestly (locally the button is not locatable at all after a spaces-only
  name; on CI iOS 18.5, where it is found enabled, it will skip with ATS-VAL-01).
- These skip messages do NOT match the cascade-skip signatures, so the rerun job
  correctly leaves them alone.
