#!/usr/bin/env python3
"""
Build a dated "failed tests" TestNG suite from a CI run's results.

Collects every test that FAILED across all module reports of a CI run and
writes ONE runnable TestNG suite — so you can re-run all failures locally in
one shot, and keep a dated history of what failed when.

Usage:
    # latest completed run of the parallel workflow:
    python3 .github/scripts/build-failed-suite.py

    # a specific run id:
    python3 .github/scripts/build-failed-suite.py --run 26881798303

    # parse already-downloaded results instead of fetching from GitHub:
    python3 .github/scripts/build-failed-suite.py --from /path/with/testng-results

    # override the date label (default = today):
    python3 .github/scripts/build-failed-suite.py --date 2026-06-03

Then run the failures locally:
    mvn test -DsuiteXmlFile=failed-suites/failed-<date>.xml

Output:
    failed-suites/failed-<date>.xml   — the runnable suite (overwritten per date)
    failed-suites/history.md          — appended log: date | count | run | classes
"""
import argparse
import glob
import os
import subprocess
import sys
import tempfile
from collections import defaultdict
from datetime import date

try:
    import defusedxml.ElementTree as ET  # XXE-safe
except ImportError:
    # Guarantee the safe parser rather than falling back to the vulnerable stdlib one.
    subprocess.run([sys.executable, "-m", "pip", "install", "--quiet", "defusedxml"])
    import defusedxml.ElementTree as ET

WORKFLOW = "iOS Tests - Full Parallel Suite"
REPO = "eg-abhiyantsingh/iOS_automation_site"
OUT_DIR = "failed-suites"


def sh(cmd):
    return subprocess.run(cmd, capture_output=True, text=True)


def latest_run_id():
    r = sh(["gh", "run", "list", "--repo", REPO, "--workflow", WORKFLOW,
            "--status", "completed", "--limit", "1", "--json", "databaseId"])
    import json
    runs = json.loads(r.stdout or "[]")
    if not runs:
        sys.exit("No completed runs found for workflow: " + WORKFLOW)
    return str(runs[0]["databaseId"])


def download_run(run_id, dest):
    print(f"⤓ downloading artifacts for run {run_id} …")
    r = sh(["gh", "run", "download", run_id, "--repo", REPO, "--dir", dest])
    if r.returncode != 0:
        print(r.stderr.strip())
        sys.exit(f"Could not download artifacts for run {run_id} "
                 "(expired after 7 days, or run still in progress?)")


# A SKIP whose message matches one of these was DENIED a fair run (the dead-session
# circuit breaker, the RunHealth WDA-hopeless / suite-wall fast-skip, or a wedge/timeout
# init failure). Those belong in the fresh-simulator rerun. A SKIP from a missing
# precondition (skipIfPreconditionMissing / "CI skip:") would just skip again — exclude it.
_CASCADE_SKIP_SIGNATURES = (
    "circuit breaker", "RunHealth", "WDA", "hopeless", "wall-clock",
    "Request timeout", "Could not start a new session", "dead Appium session",
    "skipping the rest", "TimeoutException",
)
_PRECONDITION_SKIP_SIGNATURES = ("Precondition", "CI skip", "skipIfPrecondition")


def _skip_message(tm):
    """Best-effort skip message from a <test-method> (TestNG nests it under <exception>)."""
    for path in ("exception/message", "full-stacktrace", "exception/full-stacktrace"):
        txt = tm.findtext(path)
        if txt:
            return txt
    return ""


def _is_cascade_skip(msg):
    if not msg:
        return False
    if any(sig in msg for sig in _PRECONDITION_SKIP_SIGNATURES):
        return False
    return any(sig in msg for sig in _CASCADE_SKIP_SIGNATURES)


def collect_failures(results_dir, include_skips=False):
    """Return {fqcn: set(methods)} for methods that need a rerun.

    Always includes methods that FAILED and did not later PASS. With
    ``include_skips=True`` it ALSO includes SKIP-only methods whose skip message shows
    they were denied a fair run (breaker / WDA-hopeless / wall-clock / wedge timeout) —
    NOT precondition skips. This is how the fresh-simulator rerun recovers the tests a
    wedged runner fast-skipped (run 28246433532).
    """
    status = defaultdict(lambda: defaultdict(set))  # fqcn -> method -> {statuses}
    skip_msgs = defaultdict(dict)                    # fqcn -> method -> message
    xmls = glob.glob(os.path.join(results_dir, "**", "testng-results.xml"), recursive=True)
    print(f"  parsed {len(xmls)} testng-results.xml file(s)")
    for xml in xmls:
        try:
            root = ET.parse(xml).getroot()
        except Exception as e:
            print(f"  ! skip {xml}: {e}")
            continue
        for cls in root.iter("class"):
            fqcn = cls.get("name")
            if not fqcn:
                continue
            for tm in cls.iter("test-method"):
                if tm.get("is-config") == "true":
                    continue
                name = tm.get("name")
                st = tm.get("status")
                if name and st:
                    status[fqcn][name].add(st)
                    if st == "SKIP" and name not in skip_msgs[fqcn]:
                        skip_msgs[fqcn][name] = _skip_message(tm)
    failures = defaultdict(set)
    cascades = defaultdict(set)
    for fqcn, methods in status.items():
        for method, sts in methods.items():
            # failed, and not a retry that eventually passed
            if "FAIL" in sts and "PASS" not in sts:
                failures[fqcn].add(method)
            elif include_skips and sts == {"SKIP"} and _is_cascade_skip(skip_msgs[fqcn].get(method, "")):
                cascades[fqcn].add(method)
    n_casc = sum(len(m) for m in cascades.values())
    if include_skips and n_casc:
        print(f"  + {n_casc} cascade-skipped test(s) eligible for rerun (breaker / WDA-hopeless / wedge)")
    return failures, cascades


def cap_cascades(failures, cascades, cap):
    """Bound the rerun to ``cap`` methods total: ALL genuine failures always stay
    (they are the point of the rerun), cascade-skips fill the remaining budget.

    Run 28458743407: --include-skips produced a 933-test rerun suite; the single
    rerun runner tripped its breaker and 817 of them were skipped anyway. A bounded,
    failures-first suite recovers what matters instead of drowning."""
    n_fail = sum(len(m) for m in failures.values())
    budget = max(0, cap - n_fail)
    kept = defaultdict(set)
    dropped = 0
    for fqcn in sorted(cascades):
        for method in sorted(cascades[fqcn]):
            if budget > 0:
                kept[fqcn].add(method)
                budget -= 1
            else:
                dropped += 1
    if dropped:
        print(f"  ! rerun cap {cap}: kept {n_fail} failures + "
              f"{sum(len(m) for m in kept.values())} cascade-skips, DROPPED {dropped} "
              f"cascade-skips (they re-enter via the next scheduled run)")
    return kept


def _classes_block(lines, group):
    lines.append('        <classes>')
    for fqcn in sorted(group):
        lines.append(f'            <class name="{fqcn}">')
        lines.append('                <methods>')
        for method in sorted(group[fqcn]):
            lines.append(f'                    <include name="{method}"/>')
        lines.append('                </methods>')
        lines.append('            </class>')
    lines.append('        </classes>')


def write_suite(failures, cascades, the_date, run_id):
    """Write the rerun suite: genuine FAILURES in the first <test> block, cascade-skips
    in a second — TestNG runs <test> blocks sequentially, so if the rerun runner dies
    mid-way it has already retried what matters most."""
    os.makedirs(OUT_DIR, exist_ok=True)
    n_fail = sum(len(m) for m in failures.values())
    n_casc = sum(len(m) for m in cascades.values())
    total = n_fail + n_casc
    if total == 0:
        # Nothing failed: don't write an empty suite, and DON'T clobber the last
        # real latest.xml (a green run shouldn't erase the last failure list).
        # append_history still records the zero for tracking.
        return None, 0
    path = os.path.join(OUT_DIR, f"failed-{the_date}.xml")

    lines = ['<?xml version="1.0" encoding="UTF-8"?>',
             '<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">',
             f'<suite name="Failed Tests {the_date} ({n_fail} failures'
             + (f' + {n_casc} cascade-skips' if n_casc else '') + ')" '
             f'verbose="0" time-out="480000">',
             '    <!-- Auto-generated by .github/scripts/build-failed-suite.py'
             f' from CI run {run_id}. Re-run locally: '
             f'mvn test -DsuiteXmlFile={path} -->',
             '    <listeners>',
             '        <listener class-name="com.egalvanic.listeners.ConsoleProgressListener"/>',
             '    </listeners>',
             f'    <test name="CI Failures {the_date}">']
    _classes_block(lines, failures if n_fail else cascades)
    lines.append('    </test>')
    if n_fail and n_casc:
        lines.append(f'    <test name="Cascade-skip recovery {the_date}">')
        _classes_block(lines, cascades)
        lines.append('    </test>')
    lines += ['</suite>', '']
    xml = "\n".join(lines)
    with open(path, "w") as f:
        f.write(xml)
    # Always keep a canonical latest.xml = most recent run's failures, so there's
    # one stable file to run: mvn test -DsuiteXmlFile=failed-suites/latest.xml
    with open(os.path.join(OUT_DIR, "latest.xml"), "w") as f:
        f.write(xml)
    return path, total


def append_history(the_date, total, run_id, failures):
    os.makedirs(OUT_DIR, exist_ok=True)
    hist = os.path.join(OUT_DIR, "history.md")
    header = ("# CI Failure History\n\n"
              "Generated by `.github/scripts/build-failed-suite.py`.\n\n"
              "| Date | Failures | Run | Breakdown |\n|---|---|---|---|\n")
    per_class = ", ".join(f"{c.split('.')[-1]}:{len(m)}" for c, m in sorted(failures.items()))
    if run_id and run_id != "local":
        run_cell = f"[{run_id}](https://github.com/{REPO}/actions/runs/{run_id})"
    else:
        run_cell = "local"
    row = f"| {the_date} | {total} | {run_cell} | {per_class or '—'} |\n"

    existing = ""
    if os.path.exists(hist):
        with open(hist) as f:
            existing = f.read()
    if not existing.strip():
        existing = header
    # Idempotent for real runs: drop any prior row for this run id, then append
    # (so re-processing the same run replaces its row instead of duplicating).
    if run_id and run_id != "local":
        existing = "".join(l for l in existing.splitlines(keepends=True)
                           if f"/runs/{run_id})" not in l)
    if not existing.endswith("\n"):
        existing += "\n"
    with open(hist, "w") as f:
        f.write(existing + row)
    return hist


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--run", help="CI run id (default: latest completed)")
    ap.add_argument("--from", dest="src", help="parse a local results dir instead of fetching")
    ap.add_argument("--date", default=None, help="date label (default: today, ISO)")
    ap.add_argument("--include-skips", action="store_true",
                    help="also rerun cascade-skipped tests (breaker / WDA-hopeless / wedge), "
                         "not precondition skips — used by the in-pipeline fresh-sim rerun")
    ap.add_argument("--max-tests", type=int,
                    default=int(os.environ.get("RERUN_MAX_TESTS", "250")),
                    help="cap the suite size: all genuine failures always kept, cascade-skips "
                         "fill the remainder (default 250 / $RERUN_MAX_TESTS). A 933-test rerun "
                         "suite just breaker-skipped 817 of them (run 28458743407).")
    args = ap.parse_args()
    the_date = args.date or date.today().isoformat()

    if args.src:
        run_id = args.run or "local"
        failures, cascades = collect_failures(args.src, include_skips=args.include_skips)
    else:
        run_id = args.run or latest_run_id()
        with tempfile.TemporaryDirectory() as tmp:
            download_run(run_id, tmp)
            failures, cascades = collect_failures(tmp, include_skips=args.include_skips)

    cascades = cap_cascades(failures, cascades, args.max_tests)
    path, total = write_suite(failures, cascades, the_date, run_id)
    merged = defaultdict(set)
    for grp in (failures, cascades):
        for fqcn, methods in grp.items():
            merged[fqcn].update(methods)
    hist = append_history(the_date, total, run_id, merged)

    print(f"\n✅ {total} test(s) to rerun on {the_date} across {len(merged)} class(es) "
          f"({sum(len(m) for m in failures.values())} genuine failures, "
          f"{sum(len(m) for m in cascades.values())} cascade-skips)")
    for c in sorted(merged):
        print(f"   {c.split('.')[-1]}: {len(merged[c])}")
    if path:
        print(f"\n📄 suite:   {path}  (+ failed-suites/latest.xml)")
        print(f"▶️  run locally: mvn test -DsuiteXmlFile=failed-suites/latest.xml")
    else:
        print("\nℹ️  0 failures — latest.xml left unchanged (last real list preserved).")
    print(f"🗂  history: {hist}")


if __name__ == "__main__":
    main()
