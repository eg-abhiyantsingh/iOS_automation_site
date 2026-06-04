# Failed-test suites (dated) — the list of what failed, and how to re-run it

One **runnable TestNG suite per CI run**, containing exactly the tests that
**failed** in that run — so you can re-run all failures locally in one shot and
keep a dated history of what broke when.

## Where the list lives (you can SEE it here, in the repo)

After every full parallel CI run, **`failed-suite-collector.yml`** commits these
files **back to this folder on `main`** (not just a 30-day artifact) so they're
visible and tracked in git history:

- **`latest.xml`** — the most recent run's failures. The stable file to run.
- **`failed-<YYYY-MM-DD>.xml`** — that day's run (e.g. `failed-2026-06-03.xml`).
- **`history.md`** — the **authoritative list of dates** | failure count | run link | per-class breakdown.

The collector is a `workflow_run` workflow that fires after the parallel suite
**completes — including when the run was manually or platform-cancelled (6h cap)**.
That's deliberate: a job inside the parallel suite can't commit on a cancelled
run, so the collector lives outside it. (A green run leaves `latest.xml` untouched
so it always holds the last *real* failure list.)

> If `main` is branch-protected and CI can't push, the same files are still
> attached to the run as the **`failed-suite`** artifact (Actions → run →
> Artifacts → `failed-suite`).

## Re-run a date's failures IN CI (triage real vs. script failures)

Actions → **"Rerun Failed Tests (by date)"** → Run workflow:
- **`failed_date`** dropdown — `latest` (newest) or a recent date. *The dropdown
  can be stale; `history.md` is the real list.*
- **`custom_date`** — type any `YYYY-MM-DD` from `history.md` to run that exact day.

It boots the simulator and runs only that run's failures, then uploads a report
(with stack traces + screenshots) so you can tell a REAL functionality failure
from a SCRIPT/automation failure. A date with 0 failures is skipped (no sim spin-up).

## Run all the failures locally (one command)

```bash
# the latest CI failures:
mvn test -DsuiteXmlFile=failed-suites/latest.xml

# a specific day:
mvn test -DsuiteXmlFile=failed-suites/failed-2026-06-04.xml
```

## Regenerate the list yourself

```bash
# from the latest completed CI run of "iOS Tests - Full Parallel Suite":
python3 .github/scripts/build-failed-suite.py

# from a specific run id:
python3 .github/scripts/build-failed-suite.py --run 26881798303

# from a LOCAL run you just did (no GitHub fetch) — turns whatever you just ran
# into a re-runnable failures file immediately:
mvn test                       # or any -Dtest=... / -DsuiteXmlFile=...
python3 .github/scripts/build-failed-suite.py --from target/surefire-reports
```

Writes `failed-<date>.xml`, `latest.xml`, and appends a `history.md` row.

A test that failed then **passed on retry** is excluded (flaky-but-green, not a
failure). Config methods (`@BeforeX`) and SKIPs are excluded too.

## Quarantine (`quarantine.txt`)

The CI **test-gate** fails the build on any failure whose method name does **not**
match a token in `quarantine.txt` (known bugs with a `BUGS.md` entry). Remove a
token once that test is fixed so the gate guards it again.
