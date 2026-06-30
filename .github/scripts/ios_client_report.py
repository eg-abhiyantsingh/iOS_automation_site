#!/usr/bin/env python3
"""
Build a CLIENT-READY iOS QA verification report whose UI matches the eGalvanic
*web* client report (cover banner + pass-rate ring + Feature Area Coverage table +
Functional Issues + Verifications Passed), instead of the raw ExtentReports Spark
theme that the iOS suite shipped before.

It reads the AUTHORITATIVE TestNG data — every
``<results-dir>/<module>-report/target/surefire-reports/testng-results.xml`` — so
the numbers are exact (no fragile HTML-regex scraping). Per-CI-module results are
rolled up into client-friendly *feature areas* (Asset Management, Issue Tracking,
Site Visits, …) so the client sees product areas, not "assets-part4".

TWO-REPORT RERUN SUPPORT
------------------------
Pass ``--rerun <dir>`` to fold a fresh-simulator rerun back in: any (class, method)
present in the rerun overrides its original outcome (FAIL→PASS = recovered). This is
how the "After Rerun" report is produced. Without ``--rerun`` you get the
"Before Rerun" report. Both share identical styling so the client can compare them.

Usage
-----
  # Before-rerun report from all downloaded module artifacts:
  python3 .github/scripts/ios_client_report.py \
      --results all-reports --exclude failures-rerun \
      --out Consolidated_Client_Report_Before_Rerun.html \
      --phase before

  # After-rerun report (rerun outcomes override originals):
  python3 .github/scripts/ios_client_report.py \
      --results all-reports --exclude failures-rerun \
      --rerun all-reports/failures-rerun-report \
      --out Consolidated_Client_Report_After_Rerun.html \
      --phase after
"""
import argparse
import glob
import html as html_lib
import os
import re
import sys
from collections import defaultdict

import subprocess

try:
    import defusedxml.ElementTree as ET  # XXE-safe
except ImportError:  # pragma: no cover
    # Guarantee the safe parser rather than falling back to the vulnerable stdlib one
    # (matches .github/scripts/build-failed-suite.py).
    subprocess.run([sys.executable, "-m", "pip", "install", "--quiet", "defusedxml"])
    import defusedxml.ElementTree as ET


# ── CI module key → client-facing feature area ────────────────────────────────
# Order matters: first matching prefix wins.
AREA_RULES = [
    ("auth", "User Login & Access"),
    ("smoke", "Smoke / Core CRUD"),
    ("site-selection", "Site & Facility Selection"),
    ("site", "Site & Facility Selection"),
    ("connections", "Electrical Connections"),
    ("connection", "Electrical Connections"),
    ("location", "Locations (Buildings, Floors, Rooms)"),
    ("issues", "Issue Tracking"),
    ("issue", "Issue Tracking"),
    ("assets", "Asset Management"),
    ("asset", "Asset Management"),
    ("offline", "Offline & Sync"),
    ("sitevisit", "Site Visits & Work Orders"),
    ("workorder", "Work Order Planning"),
    ("zp323", "ZP-323 New Features"),
    ("api-contract", "API Contract & Data Integrity"),
    ("api", "API Contract & Data Integrity"),
    ("s3-drift", "Infrastructure (S3 Policy)"),
    ("s3", "Infrastructure (S3 Policy)"),
    ("security", "Security & Edge Cases"),
]


def area_for(module_key: str) -> str:
    key = module_key.lower()
    for prefix, area in AREA_RULES:
        if key.startswith(prefix) or prefix in key:
            return area
    # Fall back to a Title-Cased version of the raw module key.
    return module_key.replace("-", " ").replace("_", " ").title()


def fmt_duration(ms: float) -> str:
    try:
        s = float(ms) / 1000.0
    except (TypeError, ValueError):
        return ""
    if s < 60:
        return f"{s:.0f}s"
    m, rem = divmod(int(s), 60)
    return f"{m}m {rem}s"


def module_from_path(xml_path: str) -> str:
    """Derive the CI module key from an artifact path.

    Artifacts download as ``<results-dir>/<module>-report/target/surefire-reports/...``.
    Find the path component ending in ``-report`` and strip the suffix; otherwise use
    the parent-of-target component.
    """
    parts = xml_path.replace("\\", "/").split("/")
    for p in parts:
        if p.endswith("-report"):
            return p[: -len("-report")]
    # Fallback: component just before 'target'
    if "target" in parts:
        i = parts.index("target")
        if i > 0:
            return parts[i - 1]
    return os.path.basename(os.path.dirname(xml_path)) or "unknown"


def parse_methods(results_dir: str, exclude: set):
    """Return list of per-method dicts: {module, area, cls, method, status, ms}.

    A method that appears multiple times (TestNG retries) collapses to PASS if it
    ever passed, else FAIL if it ever failed, else SKIP — matching build-failed-suite.py.
    """
    raw = defaultdict(lambda: {"statuses": set(), "ms": 0.0, "module": "", "cls": ""})
    pattern = os.path.join(results_dir, "**", "testng-results.xml")
    for xml_path in sorted(glob.glob(pattern, recursive=True)):
        module = module_from_path(xml_path)
        if any(ex and ex in module for ex in exclude):
            continue
        try:
            root = ET.parse(xml_path).getroot()
        except Exception as e:  # pragma: no cover
            print(f"  ! skip {xml_path}: {e}", file=sys.stderr)
            continue
        for cls in root.iter("class"):
            fqcn = cls.get("name") or ""
            for tm in cls.iter("test-method"):
                if tm.get("is-config") == "true":
                    continue
                name = tm.get("name")
                status = tm.get("status")
                if not name or not status:
                    continue
                key = (fqcn, name)
                rec = raw[key]
                rec["statuses"].add(status)
                rec["module"] = module
                rec["cls"] = fqcn
                try:
                    rec["ms"] = max(rec["ms"], float(tm.get("duration-ms", 0)))
                except (TypeError, ValueError):
                    pass

    methods = []
    for (fqcn, name), rec in raw.items():
        sts = rec["statuses"]
        status = "PASS" if "PASS" in sts else ("FAIL" if "FAIL" in sts else "SKIP")
        methods.append({
            "module": rec["module"],
            "area": area_for(rec["module"]),
            "cls": fqcn,
            "method": name,
            "status": status,
            "ms": rec["ms"],
        })
    return methods


def merge_rerun(primary, rerun):
    """Override primary outcomes with rerun outcomes, keyed by (class, method).

    Marks each merged method with 'reran' and 'recovered' (was FAIL, now PASS).
    """
    rr = {(m["cls"], m["method"]): m for m in rerun}
    merged = []
    for m in primary:
        key = (m["cls"], m["method"])
        if key in rr:
            new = dict(m)
            new["status"] = rr[key]["status"]
            new["ms"] = rr[key]["ms"] or m["ms"]
            new["reran"] = True
            new["recovered"] = (m["status"] != "PASS" and rr[key]["status"] == "PASS")
            merged.append(new)
        else:
            n = dict(m)
            n["reran"] = False
            n["recovered"] = False
            merged.append(n)
    return merged


def status_lower(s):
    return {"PASS": "pass", "FAIL": "fail", "SKIP": "skip"}.get(s, "skip")


def build_html(methods, phase, run_url=None, recovered_count=0, still_failing_after=0):
    # ── Aggregate by feature area ──
    areas = defaultdict(lambda: {"passed": 0, "failed": 0, "skipped": 0, "tests": []})
    for m in methods:
        a = areas[m["area"]]
        if m["status"] == "PASS":
            a["passed"] += 1
        elif m["status"] == "FAIL":
            a["failed"] += 1
        else:
            a["skipped"] += 1
        a["tests"].append(m)

    total_passed = sum(a["passed"] for a in areas.values())
    total_failed = sum(a["failed"] for a in areas.values())
    total_skipped = sum(a["skipped"] for a in areas.values())
    total_tests = total_passed + total_failed + total_skipped
    pass_rate = (total_passed / total_tests * 100.0) if total_tests else 0.0

    # ── Feature Area Coverage rows ──
    area_rows = []
    for name in sorted(areas):
        a = areas[name]
        total = a["passed"] + a["failed"] + a["skipped"]
        rate = (a["passed"] / total * 100.0) if total else 0
        if a["failed"] == 0 and a["skipped"] == 0:
            state_cls, state_label = "ok", "Passed"
        elif a["passed"] == 0:
            state_cls, state_label = "fail", "Failed"
        else:
            state_cls, state_label = "partial", "Partial"
        area_rows.append(f"""
      <tr>
        <td class="area-name">{html_lib.escape(name)}</td>
        <td class="num">{total}</td>
        <td class="num passed">{a['passed']}</td>
        <td class="num failed">{a['failed']}</td>
        <td class="num">{a['skipped']}</td>
        <td class="rate"><span class="rate-pill rate-{state_cls}">{rate:.0f}%</span></td>
        <td><span class="status-chip status-{state_cls}">{state_label}</span></td>
      </tr>""")

    # ── Functional Issues (failures, grouped by area) ──
    failures = [m for m in methods if m["status"] == "FAIL"]
    if failures:
        grouped = defaultdict(list)
        for f in failures:
            grouped[f["area"]].append(f)
        parts = []
        for area in sorted(grouped):
            items = grouped[area]
            rows = []
            for f in items:
                short = f["method"]
                rows.append(f"""
          <li>
            <div class="fail-name">{html_lib.escape(short)}</div>
            <div class="fail-duration">{html_lib.escape(f['cls'].split('.')[-1])} &bull; ran for {fmt_duration(f['ms'])}</div>
          </li>""")
            parts.append(f"""
      <div class="fail-area">
        <h3>{html_lib.escape(area)} <span class="count-pill">{len(items)}</span></h3>
        <ul class="fail-list">{''.join(rows)}</ul>
      </div>""")
        func_fail_html = f"""
    <section class="section">
      <div class="section-header">
        <h2>Functional Issues &mdash; Needs Attention</h2>
        <p class="section-lead">{len(failures)} verification{'' if len(failures) == 1 else 's'} did not pass. These are genuine regressions or environment issues in the current build that require engineering review.</p>
      </div>
      {''.join(parts)}
    </section>"""
    else:
        func_fail_html = """
    <section class="section">
      <div class="section-header">
        <h2>Functional Issues &mdash; Needs Attention</h2>
        <p class="section-lead"><strong>No functional regressions detected.</strong> All feature-level verifications passed.</p>
      </div>
    </section>"""

    # ── Rerun recovery banner (after-report only) ──
    rerun_html = ""
    if phase == "after":
        rerun_html = f"""
    <section class="section">
      <div class="section-header">
        <h2>Rerun Recovery</h2>
        <p class="section-lead">
          Every test that did not pass on the first run was automatically re-executed on a
          <strong>fresh simulator</strong>. <strong class="text-resolved">{recovered_count} recovered</strong>
          (passed on rerun &mdash; transient/environment flake, not a product defect) &bull;
          <strong class="text-active">{still_failing_after} still failing</strong> after rerun (genuine, needs engineering review).
          The numbers above reflect the post-rerun state.
        </p>
      </div>
    </section>"""

    # ── Verifications passed summary ──
    passing_summary = f"""
    <section class="section">
      <div class="section-header">
        <h2>Verifications Passed</h2>
        <p class="section-lead"><strong>{total_passed} verifications executed successfully</strong> across {len(areas)} product areas. See the Feature Area Coverage table above for the distribution.</p>
      </div>
    </section>"""

    # ── Executive paragraph ──
    phase_label = "After Rerun" if phase == "after" else ("Before Rerun" if phase == "before" else "")
    exec_paragraph = (
        f'Across <strong>{total_tests} automated verifications</strong> run against the '
        f'eGalvanic iOS Z&nbsp;Platform app, <strong class="text-passed">{total_passed} passed</strong>, '
        f'<strong class="text-failed">{total_failed} did not pass</strong>'
        + (f', and <strong>{total_skipped} were skipped</strong>' if total_skipped else '')
        + f'. Overall pass rate: <strong>{pass_rate:.1f}%</strong>.'
    )
    if phase == "after" and recovered_count:
        exec_paragraph += (
            f' {recovered_count} verification{"" if recovered_count == 1 else "s"} that failed on the '
            f'first run recovered on a fresh-simulator rerun.'
        )

    run_footer = f' &bull; <a href="{html_lib.escape(run_url)}" style="color:inherit;">CI run</a>' if run_url else ""
    tag = f' &mdash; {phase_label}' if phase_label else ""

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>eGalvanic iOS Z Platform &mdash; QA Verification Report{tag}</title>
<style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  :root {{
    --ink: #1b2330; --subtle: #5c6878; --border: #e3e7ec;
    --pass: #2d7a4a; --pass-soft: #e6f4ec;
    --fail: #b83a42; --fail-soft: #fbeceb;
    --partial: #c47608; --partial-soft: #fdf3e2;
    --accent: #1f3a5f; --accent-soft: #eff4fa;
  }}
  body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; background: #f7f8fa; color: var(--ink); line-height: 1.55; font-size: 14px; -webkit-font-smoothing: antialiased; }}
  .container {{ max-width: 1000px; margin: 0 auto; padding: 0 24px; }}
  .cover {{ background: linear-gradient(135deg, #1f3a5f 0%, #2a4a7a 100%); color: #fff; padding: 48px 0 40px; margin-bottom: 32px; }}
  .cover .wrap {{ max-width: 1000px; margin: 0 auto; padding: 0 24px; }}
  .cover .client-note {{ font-size: 13px; letter-spacing: 0.08em; text-transform: uppercase; opacity: 0.75; margin-bottom: 8px; }}
  .cover h1 {{ font-size: 28px; font-weight: 600; letter-spacing: -0.01em; margin-bottom: 8px; }}
  .cover .meta {{ font-size: 14px; opacity: 0.85; }}
  .phase-badge {{ display:inline-block; margin-left:10px; padding:2px 10px; border-radius:999px; background:rgba(255,255,255,0.18); font-size:12px; font-weight:600; letter-spacing:0.04em; }}
  .section {{ margin-bottom: 36px; background: #fff; border: 1px solid var(--border); border-radius: 10px; padding: 28px; box-shadow: 0 1px 2px rgba(16,24,40,0.04); }}
  .section-header {{ margin-bottom: 20px; }}
  .section-header h2 {{ font-size: 18px; font-weight: 600; color: var(--ink); margin-bottom: 8px; }}
  .section-lead {{ font-size: 14px; color: var(--subtle); }}
  .exec-hero {{ display: grid; grid-template-columns: 180px 1fr; gap: 32px; align-items: center; }}
  .exec-hero .ring {{ position: relative; width: 160px; height: 160px; }}
  .exec-hero .ring svg {{ width: 100%; height: 100%; transform: rotate(-90deg); }}
  .exec-hero .ring-center {{ position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }}
  .exec-hero .ring-center .big {{ font-size: 32px; font-weight: 700; color: var(--ink); }}
  .exec-hero .ring-center .small {{ font-size: 11px; color: var(--subtle); letter-spacing: 0.08em; text-transform: uppercase; margin-top: 4px; }}
  .key-stats {{ display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-top: 24px; }}
  .key-stat {{ padding: 16px 18px; border-radius: 8px; border: 1px solid var(--border); }}
  .key-stat .n {{ font-size: 24px; font-weight: 700; margin-bottom: 2px; }}
  .key-stat .l {{ font-size: 11px; color: var(--subtle); letter-spacing: 0.08em; text-transform: uppercase; }}
  .key-stat.passed .n {{ color: var(--pass); }}
  .key-stat.failed .n {{ color: var(--fail); }}
  .key-stat.skipped .n {{ color: var(--partial); }}
  .key-stat.total .n {{ color: var(--ink); }}
  .key-stat.rate .n {{ color: var(--accent); }}
  table.coverage-table {{ width: 100%; border-collapse: collapse; }}
  .coverage-table th, .coverage-table td {{ padding: 12px 14px; text-align: left; border-bottom: 1px solid var(--border); font-size: 13.5px; vertical-align: middle; }}
  .coverage-table th {{ font-size: 11px; font-weight: 600; color: var(--subtle); letter-spacing: 0.08em; text-transform: uppercase; background: #fafbfc; }}
  .coverage-table td.num {{ text-align: right; font-variant-numeric: tabular-nums; }}
  .coverage-table td.passed {{ color: var(--pass); font-weight: 600; }}
  .coverage-table td.failed {{ color: var(--fail); font-weight: 600; }}
  .coverage-table td.rate {{ text-align: right; }}
  .coverage-table td.area-name {{ font-weight: 500; color: var(--ink); }}
  .rate-pill {{ display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }}
  .rate-ok {{ background: var(--pass-soft); color: var(--pass); }}
  .rate-partial {{ background: var(--partial-soft); color: var(--partial); }}
  .rate-fail {{ background: var(--fail-soft); color: var(--fail); }}
  .status-chip {{ display: inline-block; padding: 3px 10px; border-radius: 4px; font-size: 11px; font-weight: 600; letter-spacing: 0.03em; text-transform: uppercase; }}
  .status-ok {{ background: var(--pass-soft); color: var(--pass); }}
  .status-fail {{ background: var(--fail-soft); color: var(--fail); }}
  .status-partial {{ background: var(--partial-soft); color: var(--partial); }}
  .fail-area {{ margin-bottom: 20px; }}
  .fail-area h3 {{ font-size: 15px; margin-bottom: 10px; color: var(--ink); display: flex; align-items: center; gap: 10px; }}
  .count-pill {{ background: var(--fail-soft); color: var(--fail); font-size: 12px; font-weight: 600; padding: 1px 8px; border-radius: 999px; }}
  .fail-list {{ list-style: none; }}
  .fail-list li {{ padding: 12px 16px; border-left: 3px solid var(--fail); background: #fff; margin-bottom: 6px; border-radius: 0 6px 6px 0; }}
  .fail-name {{ font-size: 13.5px; color: var(--ink); line-height: 1.45; font-family: 'SF Mono', Menlo, monospace; }}
  .fail-duration {{ font-size: 11.5px; color: var(--subtle); margin-top: 3px; }}
  .text-passed {{ color: var(--pass); }}
  .text-failed {{ color: var(--fail); }}
  .text-resolved {{ color: var(--pass); }}
  .text-active {{ color: var(--fail); }}
  footer {{ text-align: center; padding: 28px 0 40px; font-size: 12px; color: var(--subtle); }}
  @media print {{
    body {{ background: #fff; }}
    .cover {{ break-inside: avoid; page-break-inside: avoid; }}
    .section {{ break-inside: avoid; page-break-inside: avoid; box-shadow: none; }}
  }}
  @media (max-width: 640px) {{
    .exec-hero {{ grid-template-columns: 1fr; }}
    .key-stats {{ grid-template-columns: repeat(2, 1fr); }}
    .coverage-table {{ display: block; overflow-x: auto; }}
  }}
</style>
</head>
<body>

<div class="cover">
  <div class="wrap">
    <div class="client-note">QA Verification Report &bull; For Client Review</div>
    <h1>eGalvanic iOS Z&nbsp;Platform &mdash; Automated Test Results{('<span class="phase-badge">' + phase_label + '</span>') if phase_label else ''}</h1>
    <div class="meta">Platform: iOS (XCUITest) &bull; Pass rate: <strong style="color:#fff;">{pass_rate:.1f}%</strong>{run_footer}</div>
  </div>
</div>

<div class="container">
  <section class="section">
    <div class="section-header"><h2>Executive Summary</h2></div>
    <div class="exec-hero">
      <div class="ring">
        <svg viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="44" fill="none" stroke="#e3e7ec" stroke-width="8"/>
          <circle cx="50" cy="50" r="44" fill="none" stroke="#2d7a4a" stroke-width="8"
                  stroke-dasharray="{pass_rate / 100 * 276.46:.2f} 276.46" stroke-linecap="round"/>
        </svg>
        <div class="ring-center">
          <div class="big">{pass_rate:.0f}%</div>
          <div class="small">Pass Rate</div>
        </div>
      </div>
      <div><p style="font-size: 15px; line-height: 1.65;">{exec_paragraph}</p></div>
    </div>
    <div class="key-stats">
      <div class="key-stat total"><div class="n">{total_tests}</div><div class="l">Total Verifications</div></div>
      <div class="key-stat passed"><div class="n">{total_passed}</div><div class="l">Passed</div></div>
      <div class="key-stat failed"><div class="n">{total_failed}</div><div class="l">Did Not Pass</div></div>
      <div class="key-stat skipped"><div class="n">{total_skipped}</div><div class="l">Skipped</div></div>
      <div class="key-stat rate"><div class="n">{len(areas)}</div><div class="l">Feature Areas</div></div>
    </div>
  </section>
  {rerun_html}
  <section class="section">
    <div class="section-header">
      <h2>Feature Area Coverage</h2>
      <p class="section-lead">Verification outcome grouped by product area.</p>
    </div>
    <table class="coverage-table">
      <thead>
        <tr>
          <th>Product Area</th>
          <th class="num">Verifications</th>
          <th class="num">Passed</th>
          <th class="num">Did Not Pass</th>
          <th class="num">Skipped</th>
          <th class="num">Pass Rate</th>
          <th>Overall</th>
        </tr>
      </thead>
      <tbody>{''.join(area_rows)}</tbody>
    </table>
  </section>

  {func_fail_html}
  {passing_summary}

  <footer>
    Generated automatically from CI run{run_footer} &bull;
    {total_tests} automated verifications across {len(areas)} feature areas &bull;
    For questions, contact QA
  </footer>
</div>
</body>
</html>
"""


def main():
    ap = argparse.ArgumentParser(description="Build a web-styled iOS client report from TestNG XML.")
    ap.add_argument("--results", required=True, help="dir containing <module>-report/.../testng-results.xml")
    ap.add_argument("--out", required=True, help="output HTML path")
    ap.add_argument("--rerun", default=None, help="optional rerun results dir whose outcomes override primary")
    ap.add_argument("--exclude", default="", help="comma-separated module substrings to drop from primary")
    ap.add_argument("--phase", default="", choices=["", "before", "after"], help="report phase label")
    ap.add_argument("--run-url", default=os.environ.get("RUN_URL", ""), help="CI run URL for the footer")
    args = ap.parse_args()

    exclude = {e.strip() for e in args.exclude.split(",") if e.strip()}
    primary = parse_methods(args.results, exclude)

    recovered = still_failing = 0
    methods = primary
    if args.rerun:
        rerun = parse_methods(args.rerun, set())
        methods = merge_rerun(primary, rerun)
        recovered = sum(1 for m in methods if m.get("recovered"))
        still_failing = sum(1 for m in methods if m.get("reran") and m["status"] == "FAIL")

    html_out = build_html(
        methods, args.phase, run_url=(args.run_url or None),
        recovered_count=recovered, still_failing_after=still_failing,
    )
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with open(args.out, "w") as f:
        f.write(html_out)

    total = len(methods)
    passed = sum(1 for m in methods if m["status"] == "PASS")
    failed = sum(1 for m in methods if m["status"] == "FAIL")
    skipped = sum(1 for m in methods if m["status"] == "SKIP")
    rate = (passed / total * 100.0) if total else 0.0
    print(f"Wrote {args.out}  ({os.path.getsize(args.out) / 1024:.1f} KB)")
    print(f"Phase: {args.phase or 'n/a'} | {total} total, {passed} passed, {failed} failed, "
          f"{skipped} skipped, {rate:.1f}% pass rate"
          + (f" | rerun: {recovered} recovered, {still_failing} still failing" if args.rerun else ""))


if __name__ == "__main__":
    main()
