# Photo Viewer: Zoom & Markup Tools — QA Test Design (prepared ahead of build)

**Status: PREPARED — feature not yet in QA.** Written 2026-06-25 from the user story
(relates to TKT-933 shared viewer/zoom; MOB-FR-18 offline conflict handling; PLAT-FR-04 audit).
Activate when the build lands: fill locators in `PhotoViewerMarkup_Test.java` (dormant skeleton,
`enabled = false`, not in any suite XML) and flip the suite wiring.

## 0. Test-readiness blockers (chase BEFORE calling this testable)
| # | Open decision (from story) | Why QA is blocked | What we need |
|---|---|---|---|
| B1 | Overlay vs destructive model | AC1/NFR "original image unmodified" is only testable if overlay is confirmed (story assumes it) | Written confirmation + storage shape (coordinates/type/label/colour/author/timestamp) |
| B2 | Max annotations per photo **[TBD]** | TC_PVM_112 boundary test has no number | The cap value + exact error copy confirmed ("Maximum annotations reached for this photo.") |
| B3 | Versioning + edit/delete permissions | Permission matrix tests (TC_PVM_140s) need roles: who edits/deletes whose annotations | Role × action matrix |
| B4 | Annotations in generated reports **[TBD]** | Report-pipeline tests can't be designed | Yes/No; if yes, which report types |
| B5 | MOB-FR-18 conflict handling | Offline conflict tests (TC_PVM_133-134) undefined | Merge/last-write-wins/manual-resolve rule |
| B6 | Which photo types get markup | Story scopes to equipment photos (not SLD/PDF/video) — is the IR photo pair viewer included? | List of entry points on iOS |

## 1. Automation strategy (this repo)
- **iOS (Appium/XCUITest)** — primary: markup mode, annotation CRUD, label validation, unsaved-warning,
  offline save, VoiceOver label exposure, touch-target sizes.
- **API (`TestDataApi`)** — strongest evidence layer: original-image **byte-level immutability**
  (GET image bytes before/after annotate → SHA-256 equal), overlay **structured-data shape**,
  author/timestamp fields, sanitisation server-side, audit-log entries (PLAT-FR-04).
- **Web (Playwright, ground-truth)** — cross-platform visibility (annotate on iOS → visible on web),
  keyboard access + focus order (web-only a11y ACs).
- **Zoom-anchoring** (AC4) — semi-automated on iOS: read the annotation element rect and the image rect
  before/after `mobile: pinch`/pan; assert the annotation's *relative* position within the image rect is
  constant (±2%). Pixel-perfect check stays manual/visual (screenshot pair in the report).
- Known trap to avoid from day one: this viewer will be a SwiftUI sheet over the photo — expect
  [[swiftui-previous-screen-bleed-through]]; all new locators must be scoped/budgeted from the start
  (existsNow / withImplicitWait(0) / `name IN {…}`), never bare whole-tree CONTAINS.

## 2. Test cases — TC_PVM_*
Legend: P1 = must-pass for sign-off; A = automatable iOS, W = web, API = API-level, M = manual/visual.

### Entry, mode & permissions (TC_PVM_001-019)
| ID | P | How | Case |
|---|---|---|---|
| TC_PVM_001 | P1 | A | Photo opens in shared viewer; markup entry point visible for user WITH edit permission |
| TC_PVM_002 | P1 | A | Markup entry point NOT visible/enabled for view-only user (needs B3 roles) |
| TC_PVM_003 | P2 | A | Enter markup mode → annotation toolbar appears (arrow/callout tools per build) |
| TC_PVM_004 | P2 | A | Exit markup mode with no changes → no unsaved-warning, viewer state intact |
| TC_PVM_005 | P3 | A | Markup entry point absent on non-photo attachments (PDF) — out-of-scope guard |
| TC_PVM_006 | P3 | A | Markup entry point absent on SLD viewer — out-of-scope guard |

### Annotation create/edit/delete (TC_PVM_020-049)
| ID | P | How | Case |
|---|---|---|---|
| TC_PVM_020 | P1 | A | Add arrow + mandatory label → renders on photo (AC1 first half) |
| TC_PVM_021 | P1 | A+API | Save → persisted as overlay; reopen same photo → annotation present (AC1) |
| TC_PVM_022 | P1 | API | **Original image byte-identical after annotate** (SHA-256 before/after) — evidential integrity |
| TC_PVM_023 | P1 | A | Reopen as second user with view rights → annotation visible **with author + timestamp** (AC2) |
| TC_PVM_024 | P2 | A | Add callout type (if in build) with label → renders + persists |
| TC_PVM_025 | P2 | A | Select existing annotation → edit controls shown (label + colour) per permissions |
| TC_PVM_026 | P2 | A | Edit label of own annotation → persists on reopen |
| TC_PVM_027 | P2 | A | Change colour from approved palette → persists; only palette colours offered (no free colour input) |
| TC_PVM_028 | P2 | A | Delete own annotation → gone on reopen (permission per B3) |
| TC_PVM_029 | P2 | API | Overlay stored as structured data: coordinates, type, label, colour, author, timestamp all present |
| TC_PVM_030 | P3 | A | Multiple annotations on one photo → all render, all persist independently |
| TC_PVM_031 | P3 | A | Annotation on a zoomed-in region → saves with correct image-space coordinates |

### Validation (TC_PVM_100-119) — exact copy from story
| ID | P | How | Case |
|---|---|---|---|
| TC_PVM_100 | P1 | A | Save with empty label → save BLOCKED; inline message **"Add a short label for this annotation."**; focus moves to offending annotation (AC3) |
| TC_PVM_101 | P1 | A | Label blur with empty text → same message on blur (per "On blur + save") |
| TC_PVM_102 | P2 | A | Label 1 char → accepted (lower boundary) |
| TC_PVM_103 | P2 | A | Label exactly 200 chars → accepted (upper boundary) |
| TC_PVM_104 | P2 | A | Label 201 chars → rejected or truncated per build (boundary) |
| TC_PVM_105 | P1 | A+API | Script/HTML payload in label → stored/rendered inert on iOS AND web (client+server sanitised) — reuse Security_EdgeCase payload set |
| TC_PVM_106 | P2 | A | Unicode/emoji/RTL label → accepted, renders, round-trips intact |
| TC_PVM_107 | P2 | A | Whitespace-only label → treated as empty → blocked with label-required message |
| TC_PVM_108 | P2 | API | Server-side validation: direct API write with empty label rejected (client bypass) |
| TC_PVM_112 | P2 | A | Add annotation past max cap → **"Maximum annotations reached for this photo."** — *BLOCKED on B2 for the cap value* |

### Unsaved / error states (TC_PVM_120-129)
| ID | P | How | Case |
|---|---|---|---|
| TC_PVM_120 | P1 | A | Unsaved annotation + close viewer → warning shown; Cancel keeps editing (AC5) |
| TC_PVM_121 | P1 | A | Unsaved annotation + navigate away (back/tab) → warning; Discard actually discards |
| TC_PVM_122 | P2 | A | Viewer shows an unsaved-changes indicator while dirty |
| TC_PVM_123 | P1 | A | Save fails (airplane-mode toggle mid-save) → unsaved annotations REMAIN visible + retry offered |
| TC_PVM_124 | P2 | A | Retry after restoring connectivity → save succeeds, annotation persists |
| TC_PVM_125 | P3 | A | Annotation data still loading → non-blocking indicator; viewer usable |

### Offline & sync (TC_PVM_130-139) — mutation inbox, reuse Offline patterns
| ID | P | How | Case |
|---|---|---|---|
| TC_PVM_130 | P1 | A | Go offline → annotate → save → confirmed on-device (AC6); queue grows (Offline pattern: assert mutation-inbox growth) |
| TC_PVM_131 | P1 | A+W | Reconnect → mutation processed → annotation visible on web with author/timestamp (AC7) |
| TC_PVM_132 | P2 | A | Offline annotation survives app kill + relaunch before sync |
| TC_PVM_133 | P2 | M | Conflict: same photo annotated on two devices offline → per MOB-FR-18 — *BLOCKED on B5* |
| TC_PVM_134 | P3 | A | Offline edit of an existing (synced) annotation → syncs correctly |

### Zoom & pan anchoring (TC_PVM_150-159) — AC4, ties to TKT-933
| ID | P | How | Case |
|---|---|---|---|
| TC_PVM_150 | P1 | A(semi) | Pinch-zoom in → annotation stays anchored: relative position within image rect constant ±2% |
| TC_PVM_151 | P1 | A(semi) | Pan while zoomed → annotation moves with the image (not the screen) |
| TC_PVM_152 | P2 | A(semi) | Zoom out to fit → annotation scales back, still anchored |
| TC_PVM_153 | P2 | M | Visual: annotation arrowhead points at the same pixel feature at 1×, 2×, max zoom (screenshot triple in report) |
| TC_PVM_154 | P2 | A | Add annotation while zoomed, save, reopen at 1× → renders at correct spot |
| TC_PVM_155 | P3 | A | Rapid zoom/pan while overlay visible → no crash, viewer responsive (perf NFR guard: interaction ≤ threshold) |

### Accessibility (TC_PVM_160-169) — WCAG 2.1 AA
| ID | P | How | Case |
|---|---|---|---|
| TC_PVM_160 | P1 | A | Every annotation exposes its mandatory label as accessibility name (XCUITest a11y tree) |
| TC_PVM_161 | P2 | A | Markup controls have accessible labels + touch targets ≥ 44×44pt (rect check) |
| TC_PVM_162 | P2 | W | Keyboard: all markup controls reachable; focus order viewer → tools → fields → save/cancel |
| TC_PVM_163 | P2 | W | Save error announced; focus moves to offending annotation field |
| TC_PVM_164 | P3 | M | Palette contrast ≥ 3:1 against typical image backgrounds or non-colour indicator present |

### Audit & security (TC_PVM_170-179)
| ID | P | How | Case |
|---|---|---|---|
| TC_PVM_170 | P1 | API | Create/edit/delete each writes an audit entry with author identity (PLAT-FR-04) |
| TC_PVM_171 | P2 | API | Annotation author cannot be spoofed via API payload |
| TC_PVM_172 | P2 | A | View-only user gets no edit controls on selected annotation (per B3) |

### Report pipeline (TC_PVM_180+) — *entirely BLOCKED on B4*
Designed only after the render-in-reports decision; if "yes", add: annotation renders in the
customer PDF at correct position, respects zoom-independent coordinates, label legible.

## 3. Coverage vs story ACs
| Story AC | Covered by |
|---|---|
| Arrow+label → overlay persisted, original unmodified | TC_PVM_020/021/**022** |
| View-rights users see annotations + author/timestamp | TC_PVM_023 |
| Label required blocks save | TC_PVM_100/101/107/108 |
| Zoom/pan anchoring | TC_PVM_150-154 |
| Unsaved-warning on navigate away | TC_PVM_120/121 |
| Offline save confirmed on-device | TC_PVM_130/132 |
| Sync on reconnect → visible on web | TC_PVM_131 |

**47 cases total: 13 P1 / 22 P2 / 12 P3 — 4 blocked on decisions (B2, B4, B5 ×2).**

## 4. Fixtures to prepare (can do NOW, before build)
- A dedicated fixture asset with a known equipment photo on acme (stable name `QA-PVM Photo Asset`),
  so byte-hash comparisons have a deterministic target. Create via existing asset+photo flows.
- Two accounts: editor + view-only (needs B3 role names) — verify on acme tenant.
- Baseline SHA-256 of the fixture photo recorded in the test (fetched via API at runtime, not hard-coded).
