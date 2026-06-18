# 088 — Multi-select job dispatch (run any combination) + selectable API testing

**Request:** a multi-selector on "Run workflow" so you can pick several jobs (e.g. API
testing + a couple of modules) to run together in the parallel suite(s).

## GitHub constraint
`workflow_dispatch` has **no native multi-select dropdown** — `type: choice` is single-pick,
and `type: boolean` checkboxes are capped at 10 inputs (we have ~20 jobs). The standard
multi-select pattern is a **comma-separated free-text `string` input** + whole-token matching.

## What changed (both parallel suites: ios-tests-parallel.yml + ios-tests-repodeveloper-parallel.yml)
- `job_selection` input: `type: choice` → **`type: string`** (default `all`). You now type a
  **comma-separated list, NO spaces**, e.g. `authentication-only,connections-only,api-contract`
  or `assets-p1-...,issues-p1-...`. `all` still runs everything.
- Every job's `if:` rewritten to whole-token matching:
  `contains(format(',{0},', github.event.inputs.job_selection), ',<token>,')`. The comma
  wrapping makes it an exact-token match (so `sitevisit-only` never matches `sitevisit-phase1`,
  etc.). Multi-line folded `if:` blocks (sitevisit, zp323) converted too. 0 old-style gates left.
- New selectable **`api-contract`** job: runs `testng-api-contract.xml` (auth contract + SLD
  data-integrity) on **ubuntu, no simulator, ~30s**. Runs under `all` and when selected
  explicitly. Uploads `api-contract-report`.

## How to use
On "Run workflow" → "Which jobs to run", type any of (comma-separated, no spaces):
`all` · `smoke` · `authentication-only` · `site-selection-only` · `assets-p1..p6-…` ·
`issues-p1..p3-…` · `location-only` · `connections-only` · `offline-only` · `sitevisit-only` ·
`sitevisit-phase1/2/3` · `workorder-planning-only` · `zp323-only` · **`api-contract`** ·
`rerun-failures`.

Examples: `api-contract` (API only) · `authentication-only,connections-only` (two modules) ·
`assets-p1-creation-edit-busway-capacitor-bug,api-contract` (a module + API).

## Note on RBAC
There are no RBAC test classes in the repo yet, so there's no `rbac` token to run. The
`api-contract` job is the API-testing entry point; an RBAC suite would need to be written
first (then it gets its own token + job the same way).
