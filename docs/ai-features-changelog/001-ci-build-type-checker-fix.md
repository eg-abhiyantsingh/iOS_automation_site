# 001 — CI Build Type-Checker Timeout Fix

**Date:** 2026-04-09 to 2026-04-12  
**Branch:** `fix/disable-flash-on-ipads-without-flash` (and PR #175)  
**Repo:** `Egalvanic/eg-pz-mobile-iOS`  
**File Modified:** `Egalvanic PZ/Views/Assets/AddAssetView.swift`

---

## Problem

The GitHub Actions CI build was failing with Swift type-checker timeout errors. The Xcode build step would hang and eventually fail during the compilation of `AddAssetView.swift`.

## Root Cause

`AddAssetView.swift` had a single `var body` computed property that was **519 lines long**. Swift's type-checker uses a constraint solver that explores branches multiplicatively. Each `if/else`, `switch`, or ternary operator doubles the number of branches. At ~520 lines with nested conditionals, the solver exceeded Swift's hardcoded **SolverBindingThreshold of 1 million branches**.

This threshold is hardcoded in the Swift compiler — there is NO CLI flag, Xcode setting, or environment variable to increase it. The only fix is to reduce the complexity of the body.

## Code Changes

Extracted 6 `@ViewBuilder` computed properties from the single 519-line `body`:

1. `headerSection` — title, breadcrumb, category selector
2. `assetDetailsSection` — name, type, description fields
3. `locationSection` — building/floor/room pickers
4. `photoSection` — camera button, photo grid
5. `additionalFieldsSection` — custom fields
6. `actionButtonsSection` — save/cancel buttons

Each `@ViewBuilder` property creates an **opaque return type boundary**. This prevents the constraint solver from exploring branches across properties multiplicatively. The solver handles each property independently, staying well under the 1M threshold.

**Result:** `body` went from 519 → 111 lines. CI build succeeded on run 24183148525.

## Key Concepts

- **SolverBindingThreshold** — Swift's hardcoded limit of 1M type inference branches. Cannot be overridden.
- **Opaque return type** — `@ViewBuilder` properties return `some View`, which hides the concrete type from the parent scope, breaking the combinatorial explosion.
- **This is NOT about time or memory** — It's a hard branch count limit, not a timeout.
