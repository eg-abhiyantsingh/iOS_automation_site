# 106 — App under test bumped to v1.49 (apps/Z-Platform-QA.zip)

**Date:** 2026-07-06
**Trigger:** User updated the app locally ("Z Platform-QA") and asked for the
zip so GitHub Actions uses the latest build.

## What changed
- `apps/Z-Platform-QA.zip`: **v1.48 → v1.49** (from
  `~/Downloads/Z Platform-QA.app`, built 2026-07-06 12:59).
- Bundle: `com.egalvanic.zplatform-QA`, `DTPlatformName=iphonesimulator`,
  arm64, `MinimumOSVersion=18.5` (== CI simulators' iOS 18.5 — at the floor,
  so a future CI iOS downgrade would break install).
- Untracked local `apps/Z Platform-QA.app` refreshed to v1.49 too (rsync
  --delete) so local runs match CI.

## How it was packaged & verified
- `ditto -c -k --norsrc --keepParent` (zip root = `Z Platform-QA.app/`, same
  layout as the old zip; `--norsrc` because the first attempt left `._*`
  AppleDouble junk after Info-ZIP `unzip` on extraction).
- Round-trip verified: `unzip` output `diff -r`-identical to the source .app.
- CI step simulated verbatim (unzip -o → rename-guard → PlistBuddy bundle-id
  extraction): produces `apps/Z Platform-QA.app`, bundle id + 1.49 resolve.

## Caution for the next run
v1.48 already moved SiteVisit + (still unfixed) Issues/Location DOM
(`docs/failure-analysis-2026-07-04-run-28666174784.md`). v1.49 may move more.
Treat the next full run as a NEW baseline; do the Issues remap against the
v1.49 DOM, not v1.48.
