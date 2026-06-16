# Second-opinion brief: the iOS Appium "giant-DOM WDA-wedge" problem

Paste this into ChatGPT / Gemini / another model for an independent critique of the
plan. It is self-contained.

## Setup
- iOS app automation: **Java + Appium 8.5.1 (XCUITest) + TestNG**, app is a **SwiftUI**
  app, ~1,690 `@Test` across ~20 modules, run as ~20 parallel CI jobs (one macOS
  runner + one booted simulator each, iOS 18.5).
- `DriverManager` is a **process-wide singleton** (one Appium/WDA session per JVM).

## The confirmed root cause
On a few "heavy" screens, a single WebDriverAgent accessibility query takes **~90 s**
(it hits the Appium client `readTimeout` of 90 s). That query **wedges WDA**; the
session then dies, and downstream tests **skip en masse** ("Could not start a new
session"). One CI run logged **196 skips across 3 jobs** from this; one job had **372
failed driver inits vs 123 successful**.

The heavy screens share two properties:
1. **Previous-screen bleed-through**: a pushed SwiftUI detail screen keeps the previous
   screen live in the a11y tree behind it, so a whole-tree query matches both.
2. **Large/repeated content**: e.g. an "OSHA Violation" issue-details screen renders a
   long subcategory list; an asset "Edit" screen has a bleed-through asset list behind
   the form; a Locations tree expands a 33-floor building into a huge subtree.

The slow queries are **whole-tree `findElements` with broad predicates**, especially
ones that match `XCUIElementTypeOther` (SwiftUI custom views) — on a bleed-through
screen this returns thousands of nodes and the snapshot serialization blows past the
10 s `customSnapshotTimeout`, landing on the 90 s readTimeout.

## What we already tried (and the result)
1. **`withImplicitWait(0)` around the query** — NO help. It bounds the *wait for an
   element to appear*, not the *cost of resolving a broad predicate*. The 90 s query is
   still 90 s.
2. **Wall-clock budget (e.g. 45 s) checked between strategies** — partial. It stops a
   method from running *many* slow queries, but it **cannot interrupt a single
   in-flight 90 s WDA command**, and the hang just moves to the next un-budgeted method.
3. **Dropping `XCUIElementTypeOther` from one specific whole-tree predicate** — worked
   for *that* query (got fast), but there are many such queries; the hang relocated to
   the option-*checking* helpers and to the issue-class-change step.
4. **WDA-rebuild recovery** (`useNewWDA=true` retry on session-creation failure, done
   inline in `initDriver`) — fires (5-7×/job) and recovers dead sessions, but **can't
   outpace constant re-wedging**, and only triggers on a *dead* session, not a *slow*
   one (a slow-but-alive WDA instead hangs `@BeforeMethod` setup → "testSetup didn't
   finish" skips).
5. **Per-test GlobalTestTimeout (360 s)** — good guardrail (caps 6 h hangs) but it
   converts "slow-but-passing" into "killed-and-skipped" on these screens.

Net: **normal screens are now healthy** (Auth, Site Selection, Work Order, ZP-323 all
pass with 0 session deaths; one asset suite's 120-skip cascade went to 0). The
**heavy/bleed-through screens are not fixed** — the query cost is the wall.

## Constraints
- `snapshotMaxDepth` is already trimmed 50→40. Lowering it further is suspect: the cost
  here looks like **breadth** (thousands of sibling `Other` nodes from bleed-through),
  not **depth**, so cutting depth risks breaking legit nested form-field finds without
  fixing breadth.
- The app is a separate (production) repo — **we can only change the test automation**,
  not the app's SwiftUI navigation that causes the bleed-through.
- Local validation is currently blocked (no Xcode → no `xcodebuild` → Appium can't build
  WDA), so each iteration is a ~5 h CI cycle until that's restored.

## The plan we're considering (critique this)
1. **Replace whole-tree predicates with scoped class-chain queries** — i.e. query only
   descendants of a specific container (the form/section that holds the field), e.g.
   `**/XCUIElementTypeScrollView[...]/**/XCUIElementTypeTextField`, so a query never
   resolves the full bleed-through tree. Hypothesis: this caps breadth at the container,
   making queries fast regardless of bleed-through.
2. **Read fields structurally, front-screen-only** — anchor on a known unique element of
   the *front* screen and navigate relative to it, never a global `CONTAINS` scan.
3. **Lower the Appium client `readTimeout`** from 90 s → ~30 s so a wedged query fails
   fast and the budget/recovery kicks in sooner (risk: false-failing slow-but-valid
   commands on a loaded runner).

## Questions for the second model
1. Is the **scoped class-chain** approach sound for capping a11y-query cost on a
   bleed-through SwiftUI tree, or does the XCUITest snapshot still resolve the whole
   tree before scoping (making it no faster)? How does Appium/WDA actually scope a
   class-chain query internally — does it snapshot globally then filter, or query the
   subtree natively?
2. Is there an Appium/XCUITest setting that bounds **query breadth or cost** (not just
   depth/timeout) — e.g. a way to exclude `XCUIElementTypeOther`, or a "first match"
   mode that stops at the first hit instead of resolving all matches?
3. Given the bleed-through is an app-side SwiftUI artifact we can't change, is there a
   reliable client-side way to **scope queries to the frontmost screen only** (e.g. via
   the navigation bar's current view controller, window hierarchy, or a key-window
   predicate)?
4. Would switching specific heavy reads to **`mobile: source` with a bounded
   `excludedAttributes`/format, or `mobile: viewportScreenshot` + OCR**, be more robust
   than element queries on these screens?
5. Is lowering `readTimeout` to ~30 s net-positive here, or will it cause more flake on
   legitimately-slow-but-valid commands?
6. Any better architectural option we're missing (e.g. fully separate Appium sessions
   per heavy module, `settings` toggles, `waitForIdleTimeout`, snapshot caching)?

## What a good answer looks like
Concrete, XCUITest-specific guidance on whether scoped class-chain queries actually
avoid the whole-tree snapshot cost, plus any breadth-bounding setting or frontmost-screen
scoping technique — ideally with the exact capability/predicate/class-chain syntax.
