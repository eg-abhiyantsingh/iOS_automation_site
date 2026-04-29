#!/usr/bin/env python3
"""
Generate a single consolidated client-facing HTML report from per-suite TestNG
artifacts.

Usage:
  python3 generate_consolidated_report.py \
      --artifacts-dir /path/to/all-reports \
      --output Consolidated_Client_Report.html \
      --run-id 24876293380 \
      --branch release/prod

Inputs (artifacts-dir layout — produced by per-suite GitHub Actions jobs):
  all-reports/
    auth-report/
      target/surefire-reports/testng-results.xml
      reports/client/Client_Report_*.html (optional, ignored)
    site-report/
      target/surefire-reports/testng-results.xml
    ...

Output: a single self-contained HTML file styled to match the
existing Consolidated_Client_Report template (green gradient header,
4 summary cards, collapsible per-module sections, PASS/FAIL badges).
"""

import argparse
import glob
import html
import os
import re
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

# ---------------------------------------------------------------------------
# Module display-name mapping. Keys are the artifact directory names produced
# by the per-suite jobs.
# ---------------------------------------------------------------------------
MODULE_NAMES = {
    "auth-report":            "Authentication",
    "site-report":            "Site Selection",
    "assets-part1-report":    "Asset Management — Part 1 (Creation + Edit + Busway + Capacitor)",
    "assets-part2-report":    "Asset Management — Part 2 (CB + DS + Fuse + Generator + JunctionBox)",
    "assets-part3-report":    "Asset Management — Part 3 (LoadCenter + MCC + Motor + Other)",
    "assets-part4-report":    "Asset Management — Part 4 (OCP + Panelboard + PDU + Relay + SWB)",
    "assets-part5-report":    "Asset Management — Part 5 (UPS + Utility + VFD + Subtypes)",
    "assets-part6-report":    "Asset Management — Part 6 (Subtypes + Tasks + Issues + Connections)",
    "issues-phase1-report":   "Issues — Phase 1 (List + Filter + Search + Creation + ClassChange)",
    "issues-phase2-report":   "Issues — Phase 2 (OSHA + Thermal + Severity + Ultrasonic)",
    "issues-phase3-report":   "Issues — Phase 3 (Ultrasonic Save + Swipe Delete + Sort + Status + CRUD)",
    "connections-report":     "Connections",
    "location-report":        "Locations",
    "offline-report":         "Offline Mode",
    "sitevisit-report":       "Site Visit / Work Orders",
    "smoke-test-reports":     "Smoke CRUD Tests",
}


def prettify(method_name: str) -> str:
    """Turn `TC_ISS_001_verifyXxxYyy` into `TC_ISS_001 — Verify Xxx Yyy`."""
    m = re.match(r"^(TC_[A-Z0-9_]+?_\d+)_(.+)$", method_name)
    if m:
        tc, rest = m.group(1), m.group(2)
        spaced = re.sub(r"([a-z])([A-Z])", r"\1 \2", rest)
        return f"{tc} — {spaced[:1].upper()}{spaced[1:]}"
    m2 = re.match(r"^([A-Z][A-Z_0-9]+?_\d+)_(.+)$", method_name)
    if m2:
        tc, rest = m2.group(1), m2.group(2)
        spaced = re.sub(r"([a-z])([A-Z])", r"\1 \2", rest)
        return f"{tc} — {spaced[:1].upper()}{spaced[1:]}"
    return method_name


def fmt_dur(ms: int) -> str:
    if ms == 0:
        return "0ms"
    s = ms / 1000.0
    if s < 60:
        return f"{s:.0f}s"
    minutes, secs = divmod(int(s), 60)
    if minutes < 60:
        return f"{minutes}m {secs}s"
    hours, rem_min = divmod(minutes, 60)
    return f"{hours}h {rem_min}m"


def collect_results(artifacts_dir: str):
    """Walk the artifacts directory and parse every testng-results.xml."""
    pattern = os.path.join(artifacts_dir, "*", "**", "testng-results.xml")
    modules = []
    for xml_path in sorted(glob.glob(pattern, recursive=True)):
        # The artifact name is the first path segment under artifacts_dir
        rel = os.path.relpath(xml_path, artifacts_dir)
        suite_dir = rel.split(os.sep)[0]
        display_name = MODULE_NAMES.get(suite_dir, suite_dir)
        try:
            tree = ET.parse(xml_path)
            root = tree.getroot()
        except ET.ParseError as exc:
            print(f"warn: skip {xml_path}: {exc}", file=sys.stderr)
            continue

        tests = []
        for tm in root.iter("test-method"):
            if tm.get("is-config") == "true":
                continue
            method_name = tm.get("name", "?")
            status = tm.get("status", "?")
            dur_ms = int(tm.get("duration-ms", "0") or 0)
            tests.append({
                "name": prettify(method_name),
                "raw": method_name,
                "status": status,
                "dur_ms": dur_ms,
            })

        passed = sum(1 for t in tests if t["status"] == "PASS")
        failed = sum(1 for t in tests if t["status"] == "FAIL")
        skipped = sum(1 for t in tests if t["status"] == "SKIP")
        modules.append({
            "name": display_name,
            "tests": tests,
            "total": len(tests),
            "passed": passed,
            "failed": failed,
            "skipped": skipped,
        })
    return modules


def render_html(modules, run_id: str, branch: str, repo: str) -> str:
    total = sum(m["total"] for m in modules)
    passed = sum(m["passed"] for m in modules)
    failed = sum(m["failed"] for m in modules)
    skipped = sum(m["skipped"] for m in modules)
    pass_pct = 100.0 * passed / max(total, 1)

    timestamp = datetime.now(timezone.utc).strftime("%B %d, %Y %H:%M UTC")
    run_url = f"https://github.com/{repo}/actions/runs/{run_id}" if run_id else ""

    parts = []
    parts.append("""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>eGalvanic iOS - Consolidated Test Report</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; background: #f0f0f0; color: #333; }
  .header { background: linear-gradient(135deg, #1a5c2e, #2d8a4e); color: #fff; padding: 25px 40px; }
  .header h1 { font-size: 22px; font-weight: 600; margin-bottom: 5px; }
  .header .subtitle { font-size: 13px; opacity: 0.85; }
  .container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
  .summary-cards { display: flex; gap: 0; margin: 25px 20px; }
  .summary-card { flex: 1; padding: 25px 20px; text-align: center; background: #fff; border: 1px solid #e0e0e0; }
  .summary-card:first-child { border-radius: 8px 0 0 8px; }
  .summary-card:last-child { border-radius: 0 8px 8px 0; }
  .summary-card .number { font-size: 36px; font-weight: 700; }
  .summary-card .label { font-size: 12px; text-transform: uppercase; letter-spacing: 1.5px; color: #888; margin-top: 5px; }
  .summary-card.total { background: #e8f4fd; }
  .summary-card.total .number { color: #2c3e50; }
  .summary-card.passed { background: #e8f8e8; }
  .summary-card.passed .number { color: #27ae60; }
  .summary-card.failed { background: #fde8e8; }
  .summary-card.failed .number { color: #e74c3c; }
  .summary-card.skipped { background: #fef9e7; }
  .summary-card.skipped .number { color: #f39c12; }
  .progress-container { margin: 0 20px 25px; }
  .progress-bar { height: 14px; background: #e0e0e0; border-radius: 7px; overflow: hidden; }
  .progress-fill { height: 100%; background: linear-gradient(90deg, #27ae60, #2ecc71); border-radius: 7px; }
  .progress-label { text-align: right; font-size: 13px; color: #666; margin-top: 5px; }
  .modules { margin: 0 20px 30px; }
  .module-section { background: #fff; border-left: 4px solid #27ae60; margin-bottom: 8px; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
  .module-section.has-fail { border-left-color: #e74c3c; }
  .module-header { padding: 15px 20px; cursor: pointer; display: flex; align-items: center; gap: 12px; user-select: none; }
  .module-header:hover { background: #f8f9fa; }
  .module-arrow { font-size: 10px; color: #999; transition: transform 0.2s; display: inline-block; width: 16px; }
  .module-header[data-expanded="true"] .module-arrow { transform: rotate(90deg); }
  .module-name { font-size: 16px; font-weight: 600; color: #2c3e50; flex: 1; }
  .module-badge { display: inline-block; padding: 4px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; margin-left: 6px; }
  .module-badge.pass { background: #d4edda; color: #155724; }
  .module-badge.fail { background: #f8d7da; color: #721c24; }
  .module-badge.skip { background: #fff3cd; color: #856404; }
  .module-tests { padding: 0 20px 10px 48px; }
  .test-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
  .test-row:last-child { border-bottom: none; }
  .test-row:hover { background: #f8f9fa; }
  .test-name { flex: 1; color: #444; }
  .test-meta { display: flex; align-items: center; gap: 12px; }
  .test-duration { font-size: 12px; color: #999; }
  .test-badge { display: inline-block; padding: 3px 12px; border-radius: 4px; font-size: 11px; font-weight: 700; letter-spacing: 0.5px; min-width: 50px; text-align: center; }
  .test-badge.pass { background: #27ae60; color: #fff; }
  .test-badge.fail { background: #e74c3c; color: #fff; }
  .test-badge.skip { background: #f39c12; color: #fff; }
  .footer { text-align: center; padding: 20px; font-size: 12px; color: #999; }
  @media print { .module-tests { display: block !important; } .module-header { cursor: default; } }
</style>
</head>
<body>
<div class="header">
  <div class="container">
    <h1>eGalvanic iOS Automation - Test Results</h1>""")
    parts.append(
        f'    <div class="subtitle">Consolidated Report &bull; {timestamp}'
        f' &bull; Run #{html.escape(str(run_id))} &bull; Branch {html.escape(branch)}</div>\n'
    )
    parts.append("""  </div>
</div>
<div class="container">
""")
    parts.append(f"""  <div class="summary-cards">
    <div class="summary-card total"><div class="number">{total}</div><div class="label">Total Tests</div></div>
    <div class="summary-card passed"><div class="number">{passed}</div><div class="label">Passed</div></div>
    <div class="summary-card failed"><div class="number">{failed}</div><div class="label">Failed</div></div>
    <div class="summary-card skipped"><div class="number">{skipped}</div><div class="label">Skipped</div></div>
  </div>
  <div class="progress-container">
    <div class="progress-bar"><div class="progress-fill" style="width:{pass_pct:.1f}%"></div></div>
    <div class="progress-label">{pass_pct:.1f}% pass rate</div>
  </div>
  <div class="modules">
""")

    for m in modules:
        section_cls = "module-section"
        if m["failed"] > 0:
            section_cls += " has-fail"
        badges = []
        if m["passed"]:
            badges.append(f'<span class="module-badge pass">{m["passed"]} passed</span>')
        if m["failed"]:
            badges.append(f'<span class="module-badge fail">{m["failed"]} failed</span>')
        if m["skipped"]:
            badges.append(f'<span class="module-badge skip">{m["skipped"]} skipped</span>')
        expanded = "true" if m["failed"] > 0 else "false"
        display = "block" if expanded == "true" else "none"
        parts.append(f'    <div class="{section_cls}">\n')
        parts.append(f'      <div class="module-header" onclick="toggleModule(this)" data-expanded="{expanded}">\n')
        parts.append('        <span class="module-arrow">&#9654;</span>\n')
        parts.append(f'        <span class="module-name">{html.escape(m["name"])}</span>\n')
        parts.append(f'        {"".join(badges)}\n')
        parts.append('      </div>\n')
        parts.append(f'      <div class="module-tests" style="display:{display}">\n')
        for t in m["tests"]:
            cls = t["status"].lower()
            badge_text = {"PASS": "PASS", "FAIL": "FAIL"}.get(t["status"], "SKIP")
            parts.append(f'        <div class="test-row {cls}">\n')
            parts.append(f'          <span class="test-name">{html.escape(t["name"])}</span>\n')
            parts.append('          <span class="test-meta">\n')
            parts.append(f'            <span class="test-duration">{fmt_dur(t["dur_ms"])}</span>\n')
            parts.append(f'            <span class="test-badge {cls}">{badge_text}</span>\n')
            parts.append('          </span>\n')
            parts.append('        </div>\n')
        parts.append('      </div>\n')
        parts.append('    </div>\n\n')

    parts.append(f"""  </div>
</div>
<div class="footer">
  eGalvanic iOS QA Automation &bull; <a href="{run_url}">Run #{run_id}</a> &bull; Generated {timestamp}
</div>
<script>
function toggleModule(header) {{
  var tests = header.nextElementSibling;
  var expanded = header.getAttribute('data-expanded') === 'true';
  if (expanded) {{ tests.style.display = 'none'; header.setAttribute('data-expanded', 'false'); }}
  else {{ tests.style.display = 'block'; header.setAttribute('data-expanded', 'true'); }}
}}
</script>
</body>
</html>
""")
    return "".join(parts)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--artifacts-dir", required=True, help="Directory containing per-suite report artifacts")
    parser.add_argument("--output", required=True, help="Path to write the consolidated HTML report")
    parser.add_argument("--run-id", default="", help="GitHub Actions run ID (for footer link)")
    parser.add_argument("--branch", default="", help="Branch name (for header)")
    parser.add_argument("--repo", default="Egalvanic/eg-pz-mobile-iOS", help="GitHub repo for run-URL link")
    args = parser.parse_args()

    if not os.path.isdir(args.artifacts_dir):
        print(f"error: artifacts directory not found: {args.artifacts_dir}", file=sys.stderr)
        sys.exit(1)

    modules = collect_results(args.artifacts_dir)
    if not modules:
        print(f"warn: no testng-results.xml found under {args.artifacts_dir}", file=sys.stderr)

    html_output = render_html(modules, args.run_id, args.branch, args.repo)
    with open(args.output, "w", encoding="utf-8") as fp:
        fp.write(html_output)

    total = sum(m["total"] for m in modules)
    passed = sum(m["passed"] for m in modules)
    failed = sum(m["failed"] for m in modules)
    skipped = sum(m["skipped"] for m in modules)
    print(f"Wrote {args.output} ({os.path.getsize(args.output):,} bytes)")
    print(f"Modules: {len(modules)} | Tests: {total} | Pass: {passed} | Fail: {failed} | Skip: {skipped}")


if __name__ == "__main__":
    main()
