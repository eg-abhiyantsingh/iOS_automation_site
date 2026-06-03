# Failed-test suites (dated)

One **runnable TestNG suite per CI run**, containing exactly the tests that
**failed** in that run — so you can re-run all failures locally in one shot and
keep a dated history of what broke when.

## Generate (from a CI run's results)

```bash
# latest completed run of "iOS Tests - Full Parallel Suite":
python3 .github/scripts/build-failed-suite.py

# a specific run:
python3 .github/scripts/build-failed-suite.py --run 26881798303

# from already-downloaded results (no GitHub fetch):
python3 .github/scripts/build-failed-suite.py --from /path/with/testng-results
```

This writes:
- `failed-suites/failed-<YYYY-MM-DD>.xml` — the runnable suite (e.g. `failed-2026-06-03.xml`)
- `failed-suites/history.md` — appended log: date | failure count | run link | per-class breakdown

A test that failed then **passed on retry** is excluded (it's flaky-but-green, not a failure). Config methods (`@BeforeX`) and SKIPs are excluded too.

## Run the failures locally

```bash
mvn test -DsuiteXmlFile=failed-suites/failed-2026-06-03.xml
```

## Auto-generation in CI

The parallel workflow's summary job also runs the generator against the run's
results and uploads `failed-suites/` as the **`failed-suite`** artifact, so every
run produces a downloadable dated failures file even if you don't run the script
locally. Commit the dated file (and the `history.md` row) when you want it tracked
in the repo over time.
