# 082 — Asset class-change budget tuning (wave 6)

**Context:** full-suite run 27557701204, Assets P3 completion: 34 pass / 63 fail / **0 skip**,
only 6 death markers (vs 120+ pre-fix). The wave-3 class-change bounding WORKED — no
WDA-death cascade, no 6-min hangs (failures now ~1.5 min). **But** the dominant failure
signature was my own guard: `Asset class change to 'Motor'/'MCC'/'Load' exceeded 60s
budget — failing fast`. 150 VerificationErrors.

## The (partial) regression
Assets P3 *passed* in the original run (245 min — slow). The class-change path snapshots
the bleed-through Edit-screen DOM several times; on a busy CI runner each snapshot can take
up to `CUSTOM_SNAPSHOT_TIMEOUT` (10 s), so a **legitimate** change runs 60-120 s. My wave-3
`CLASS_CHANGE_BUDGET_MS=60s` was too tight — it fast-failed changes that would have
completed, trading "slow-but-passing" for "fast-but-failing" on ~55-60 of the 63.

## Fix
- `CLASS_CHANGE_BUDGET_SEC` 60 → **180 s** (env-overridable): lets a slow-but-working change
  FINISH, still caps well before the ~360 s WDA-death zone (the budget's real job is
  preventing the death cascade, not killing legitimate slow work).
- `PICKER_ENUM_BUDGET_SEC` 8 → **20 s** (env-overridable): a single bleed-through snapshot
  can take ~10 s, so the old 8 s risked bailing mid-enumeration (→ "Failed to open picker").
- Both now in `AppConstants` for tuning without a code change if 180 s still needs adjustment.

Net vs the ORIGINAL (pre-any-fix) Assets P3: same passing behavior restored, PLUS the
WDA-death protection (caps before 360 s) and the wave-3 0-implicit speedups — strictly
better than the 245-min baseline.

## Validation
- `mvn -o -DskipTests test-compile` clean; `testng-verify-selftest.xml` 21/21.
- Real validation is the next full run (waves 1-6): expect Assets P3 failures to drop from
  63 toward the ~2 genuine hangs.
