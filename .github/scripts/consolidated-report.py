#!/usr/bin/env python3
"""
Consolidated Automation Report for the iOS parallel CI suite.

Merges every module's testng-results.xml into ONE self-contained HTML report
that lists EVERY test case (pass / fail / skip), grouped by module — same style
as the eGalvanic Web automation consolidated report.

This report is for the ARTIFACT only. It is intentionally NOT emailed (it can be
large). The summary email keeps linking to the workflow artifacts.

Usage:
  python3 consolidated-report.py <results-dir> <output-path> [options]
    results-dir : dir to search recursively for testng-results.xml (e.g. all-reports)
    output-path : path for the generated HTML

Options (the two-report rerun flow):
  --exclude SUBSTR   skip testng-results.xml whose path contains SUBSTR (repeatable);
                     used to keep the rerun's results out of the BEFORE report
  --rerun DIR        merge a fresh-simulator rerun: outcomes in DIR OVERRIDE the
                     primary run per (class, method) — a FAIL that passed on rerun
                     shows PASS with a RECOVERED badge (final state, what the user
                     actually has after the pipeline finishes)
  --label TEXT       suffix shown in the report header (e.g. "After Rerun")

Without --rerun the classic worst-case dedup applies (FAIL > SKIP > PASS), so
failures never hide. With --rerun the rerun has the last word — that is the point.

Writes GITHUB_OUTPUT: total_tests/total_passed/total_failed/total_skipped/pass_rate.
"""

import argparse
import os
import sys
import glob
import subprocess
from datetime import datetime
from collections import OrderedDict

try:
    import defusedxml.ElementTree as ET  # XXE-safe
except ImportError:
    subprocess.run([sys.executable, "-m", "pip", "install", "--quiet", "defusedxml"])
    import defusedxml.ElementTree as ET

# ── iOS test class (simple name) → module ─────────────────────────────────
CLASS_TO_MODULE = {
    'AuthenticationTest': 'Authentication',
    'SiteSelectionTest': 'Site Selection',
    'Asset_Phase1_Test': 'Asset Management',
    'Asset_Phase2_Test': 'Asset Management',
    'Asset_Phase3_Test': 'Asset Management',
    'Asset_Phase4_Test': 'Asset Management',
    'Asset_Phase5_Test': 'Asset Management',
    'Asset_Phase6_Test': 'Asset Management',
    'Issue_Phase1_Test': 'Issues',
    'Issue_Phase2_Test': 'Issues',
    'Issue_Phase3_Test': 'Issues',
    'LocationTest': 'Location',
    'Connections_Test': 'Connections',
    'OfflineTest': 'Offline & Sync',
    'OfflineSyncMultiSite_Test': 'Offline & Sync',
    'SiteVisit_phase1': 'Site Visit / Work Orders',
    'SiteVisit_phase2': 'Site Visit / Work Orders',
    'SiteVisit_phase3': 'Site Visit / Work Orders',
    'WorkOrderPlanning_Test': 'Work Order Planning',
    'ZP323_NewFeatures_Test': 'ZP-323 New Features',
    'S3BucketPolicyDriftTest': 'Security / Infra',
    'DebugLocationElementsTest': 'Debug',
    'DebugScreenStateTest': 'Debug',
}

MODULE_ORDER = [
    'Authentication', 'Site Selection', 'Asset Management', 'Issues',
    'Location', 'Connections', 'Offline & Sync', 'Site Visit / Work Orders',
    'Work Order Planning', 'ZP-323 New Features', 'Security / Infra', 'Debug',
]


def parse_testng_xml(filepath):
    tests = []
    try:
        root = ET.parse(filepath).getroot()
    except Exception as e:
        print(f"  WARNING: could not parse {filepath}: {e}")
        return tests
    for suite in root.findall('.//suite'):
        for test_elem in suite.findall('.//test'):
            for cls in test_elem.findall('.//class'):
                fqcn = cls.get('name', '')
                simple = fqcn.rsplit('.', 1)[-1] if '.' in fqcn else fqcn
                module = CLASS_TO_MODULE.get(simple, 'Other')
                for method in cls.findall('test-method'):
                    if method.get('is-config') == 'true':
                        continue
                    tests.append({
                        'module': module,
                        'name': method.get('name', 'Unknown'),
                        'description': method.get('description', ''),
                        'status': method.get('status', 'UNKNOWN'),
                        'duration_ms': int(method.get('duration-ms', '0')),
                        'class_name': simple,
                    })
    return tests


def find_and_parse_all(results_dir, exclude=()):
    all_tests = []
    xmls = glob.glob(os.path.join(results_dir, '**', 'testng-results.xml'), recursive=True)
    xmls = [f for f in xmls if not any(e and e in f for e in exclude)]
    if not xmls:
        print(f"WARNING: no testng-results.xml found in {results_dir}")
        return all_tests
    print(f"Found {len(xmls)} testng-results.xml file(s):")
    for f in sorted(xmls):
        t = parse_testng_xml(f)
        print(f"  {f}: {len(t)} test methods")
        all_tests.extend(t)
    # Deduplicate (class+method); keep FAIL > SKIP > PASS so failures never hide.
    pri = {'FAIL': 0, 'SKIP': 1, 'PASS': 2}
    seen = {}
    for t in all_tests:
        key = (t['class_name'], t['name'])
        if key not in seen or pri.get(t['status'], 3) < pri.get(seen[key]['status'], 3):
            seen[key] = t
    deduped = list(seen.values())
    if len(deduped) != len(all_tests):
        print(f"  Deduplicated: {len(all_tests)} → {len(deduped)}")
    return deduped


def merge_rerun(primary, rerun_dir):
    """Final-state merge: the fresh-simulator rerun has the LAST WORD per
    (class, method). A test that failed/skipped originally but passed on rerun
    becomes PASS and is flagged 'recovered' (shown with a RECOVERED badge).
    Tests never rerun keep their original outcome. This is the report the user
    asked for: "if 50 of the 100 failures pass on rerun, the updated report
    shows everything, 50 failing, the rest passing"."""
    rerun_tests = {}
    for t in find_and_parse_all(rerun_dir):
        rerun_tests[(t['class_name'], t['name'])] = t
    merged = []
    overridden = recovered = 0
    for t in primary:
        key = (t['class_name'], t['name'])
        r = rerun_tests.get(key)
        if r is None:
            merged.append(t)
            continue
        overridden += 1
        final = dict(t)
        final['status'] = r['status']
        final['duration_ms'] = r['duration_ms'] or t['duration_ms']
        final['recovered'] = (t['status'] != 'PASS' and r['status'] == 'PASS')
        recovered += 1 if final['recovered'] else 0
        merged.append(final)
    print(f"Rerun merge: {overridden} outcome(s) overridden by the rerun, "
          f"{recovered} RECOVERED (fail/skip → pass)")
    return merged


def group_by_module(tests):
    modules = OrderedDict((m, []) for m in MODULE_ORDER)
    for t in tests:
        modules.setdefault(t['module'], []).append(t)
    return OrderedDict((k, v) for k, v in modules.items() if v)


def escape_html(text):
    return (text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
                .replace('"', '&quot;').replace("'", '&#x27;'))


def fmt_dur(ms):
    if ms >= 60000:
        return f"{ms // 60000}m {(ms % 60000) // 1000}s"
    if ms >= 1000:
        return f"{ms // 1000}s"
    return f"{ms}ms"


def generate_html(modules, label=''):
    total = sum(len(v) for v in modules.values())
    passed = sum(1 for v in modules.values() for t in v if t['status'] == 'PASS')
    failed = sum(1 for v in modules.values() for t in v if t['status'] == 'FAIL')
    skipped = sum(1 for v in modules.values() for t in v if t['status'] == 'SKIP')
    recovered = sum(1 for v in modules.values() for t in v if t.get('recovered'))
    pass_rate = (passed / total * 100) if total else 0
    date_str = datetime.now().strftime("%B %d, %Y %H:%M")
    label_html = f' &bull; <strong style="color:#7ee2a8;">{escape_html(label)}</strong>' if label else ''
    recovered_note = (f' &bull; {recovered} recovered on fresh-simulator rerun'
                      if recovered else '')

    html = f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>eGalvanic iOS - Consolidated Test Report</title>
<style>
  * {{ margin:0; padding:0; box-sizing:border-box; }}
  body {{ font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif; background:#f5f6fa; color:#333; line-height:1.5; }}
  .header {{ background:linear-gradient(135deg,#2c3e50 0%,#34495e 100%); color:#fff; padding:28px 32px; }}
  .header h1 {{ font-size:22px; font-weight:600; letter-spacing:.3px; margin-bottom:4px; }}
  .header .subtitle {{ font-size:13px; color:#bdc3c7; }}
  .controls {{ padding:18px 32px 0; }}
  .controls input {{ width:100%; max-width:420px; padding:9px 12px; border:1px solid #d0d5dd; border-radius:8px; font-size:13px; }}
  .controls .filters {{ margin-top:10px; }}
  .controls button {{ border:1px solid #d0d5dd; background:#fff; padding:6px 14px; border-radius:20px; font-size:12px; cursor:pointer; margin-right:6px; }}
  .controls button.active {{ background:#2c3e50; color:#fff; border-color:#2c3e50; }}
  .summary {{ display:flex; gap:16px; padding:18px 32px; flex-wrap:wrap; }}
  .stat-card {{ flex:1; min-width:120px; padding:18px 16px; border-radius:10px; text-align:center; box-shadow:0 2px 6px rgba(0,0,0,.06); }}
  .stat-card .number {{ font-size:32px; font-weight:700; line-height:1.1; }}
  .stat-card .label {{ font-size:12px; font-weight:500; text-transform:uppercase; letter-spacing:.5px; margin-top:4px; }}
  .card-total {{ background:#fff; border:2px solid #e9ecef; }} .card-total .number {{ color:#2c3e50; }}
  .card-passed {{ background:#d4edda; border:2px solid #c3e6cb; }} .card-passed .number {{ color:#155724; }}
  .card-failed {{ background:#f8d7da; border:2px solid #f5c6cb; }} .card-failed .number {{ color:#721c24; }}
  .card-skipped {{ background:#fff3cd; border:2px solid #ffeeba; }} .card-skipped .number {{ color:#856404; }}
  .progress-wrap {{ padding:0 32px 20px; }}
  .progress-bar {{ height:8px; border-radius:4px; background:#e9ecef; overflow:hidden; display:flex; }}
  .progress-pass {{ background:#28a745; }} .progress-fail {{ background:#dc3545; }} .progress-skip {{ background:#ffc107; }}
  .progress-label {{ font-size:13px; color:#666; margin-top:6px; text-align:right; }}
  .modules {{ padding:0 32px 32px; }}
  .module {{ background:#fff; border-radius:10px; margin-bottom:16px; box-shadow:0 1px 4px rgba(0,0,0,.06); overflow:hidden; }}
  .module-header {{ padding:14px 20px; display:flex; justify-content:space-between; align-items:center; cursor:pointer; user-select:none; }}
  .module-header:hover {{ background:#fafbfc; }}
  .module-name {{ font-size:15px; font-weight:600; color:#2c3e50; }}
  .module-stats {{ display:flex; gap:8px; align-items:center; }}
  .mini-badge {{ font-size:11px; font-weight:600; padding:3px 8px; border-radius:3px; }}
  .mini-pass {{ background:#d4edda; color:#155724; }} .mini-fail {{ background:#f8d7da; color:#721c24; }} .mini-skip {{ background:#fff3cd; color:#856404; }}
  .module-all-pass {{ border-left:4px solid #28a745; }} .module-has-fail {{ border-left:4px solid #dc3545; }} .module-all-skip {{ border-left:4px solid #ffc107; }}
  .test-list {{ border-top:1px solid #f0f2f5; }}
  .test-row {{ padding:9px 20px; display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #f8f9fa; font-size:13px; }}
  .test-row:last-child {{ border-bottom:none; }} .test-row:hover {{ background:#fafbfc; }}
  .test-name {{ color:#444; flex:1; padding-right:12px; }}
  .test-class {{ color:#9aa1a9; font-size:11px; padding-right:12px; }}
  .test-duration {{ color:#999; font-size:11px; min-width:50px; text-align:right; padding-right:12px; }}
  .badge {{ display:inline-block; padding:3px 10px; border-radius:3px; font-size:11px; font-weight:600; min-width:44px; text-align:center; }}
  .badge-pass {{ background:#28a745; color:#fff; }} .badge-fail {{ background:#dc3545; color:#fff; }} .badge-skip {{ background:#ffc107; color:#000; }}
  .badge-recovered {{ background:#e6f4ec; color:#2d7a4a; border:1px solid #2d7a4a; margin-right:6px; min-width:0; }}
  .footer {{ background:#fff; border-top:1px solid #e9ecef; padding:16px 32px; font-size:11px; color:#999; text-align:center; }}
  .footer a {{ color:#007bff; text-decoration:none; }}
  .toggle {{ transition:transform .2s; display:inline-block; margin-right:8px; font-size:12px; color:#999; }}
  .module.collapsed .test-list {{ display:none; }} .module.collapsed .toggle {{ transform:rotate(-90deg); }}
  .test-row.hidden, .module.hidden {{ display:none; }}
</style></head><body>
<div class="header">
  <h1>eGalvanic iOS Automation - Test Results</h1>
  <div class="subtitle">Consolidated Report from Parallel CI Execution &bull; {date_str}{label_html}</div>
</div>
<div class="controls">
  <input id="search" type="text" placeholder="Filter tests by name or class..." oninput="applyFilters()">
  <div class="filters">
    <button id="f-all" class="active" onclick="setFilter('ALL')">All</button>
    <button id="f-fail" onclick="setFilter('FAIL')">Failed</button>
    <button id="f-skip" onclick="setFilter('SKIP')">Skipped</button>
    <button id="f-pass" onclick="setFilter('PASS')">Passed</button>
  </div>
</div>
<div class="summary">
  <div class="stat-card card-total"><div class="number">{total}</div><div class="label">Total Tests</div></div>
  <div class="stat-card card-passed"><div class="number">{passed}</div><div class="label">Passed</div></div>
  <div class="stat-card card-failed"><div class="number">{failed}</div><div class="label">Failed</div></div>
  <div class="stat-card card-skipped"><div class="number">{skipped}</div><div class="label">Skipped</div></div>
</div>
<div class="progress-wrap">
  <div class="progress-bar">
    <div class="progress-pass" style="width:{passed/total*100 if total else 0:.1f}%"></div>
    <div class="progress-fail" style="width:{failed/total*100 if total else 0:.1f}%"></div>
    <div class="progress-skip" style="width:{skipped/total*100 if total else 0:.1f}%"></div>
  </div>
  <div class="progress-label">{pass_rate:.1f}% pass rate &bull; {total} tests{recovered_note}</div>
</div>
<div class="modules">
"""
    sort_order = {'FAIL': 0, 'SKIP': 1, 'PASS': 2}
    for mod_name, tests in modules.items():
        mp = sum(1 for t in tests if t['status'] == 'PASS')
        mf = sum(1 for t in tests if t['status'] == 'FAIL')
        ms = sum(1 for t in tests if t['status'] == 'SKIP')
        mod_class = 'module-has-fail' if mf else ('module-all-skip' if (ms and not mp) else 'module-all-pass')
        collapsed = '' if mf else 'collapsed'
        html += f"""  <div class="module {mod_class} {collapsed}">
    <div class="module-header" onclick="this.parentElement.classList.toggle('collapsed')">
      <div class="module-name"><span class="toggle">&#9660;</span>{escape_html(mod_name)}</div>
      <div class="module-stats">
"""
        if mp: html += f'        <span class="mini-badge mini-pass">{mp} passed</span>\n'
        if mf: html += f'        <span class="mini-badge mini-fail">{mf} failed</span>\n'
        if ms: html += f'        <span class="mini-badge mini-skip">{ms} skipped</span>\n'
        html += """      </div>
    </div>
    <div class="test-list">
"""
        for t in sorted(tests, key=lambda x: (sort_order.get(x['status'], 3), x['name'])):
            disp = t['description'] if t['description'] else t['name']
            sl = t['status'].lower()
            badge = f'badge-{sl}' if sl in ('pass', 'fail', 'skip') else 'badge-pass'
            recovered_badge = ('<span class="badge badge-recovered">RECOVERED</span>'
                               if t.get('recovered') else '')
            html += f"""      <div class="test-row" data-status="{t['status']}" data-search="{escape_html((disp + ' ' + t['class_name']).lower())}">
        <span class="test-name">{escape_html(disp)}</span>
        <span class="test-class">{escape_html(t['class_name'])}</span>
        <span class="test-duration">{fmt_dur(t['duration_ms'])}</span>
        {recovered_badge}<span class="badge {badge}">{t['status']}</span>
      </div>
"""
        html += "    </div>\n  </div>\n"

    gh_link = ''
    server_url, repo, run_id = (os.environ.get('GITHUB_SERVER_URL', ''),
                                os.environ.get('GITHUB_REPOSITORY', ''),
                                os.environ.get('GITHUB_RUN_ID', ''))
    if server_url and repo and run_id:
        gh_link = f' &bull; <a href="{server_url}/{repo}/actions/runs/{run_id}">View on GitHub Actions</a>'

    html += f"""</div>
<div class="footer">Generated by <strong>eGalvanic iOS Automation Framework</strong> &bull; {date_str}{gh_link}</div>
<script>
  var curFilter = 'ALL';
  function setFilter(s) {{
    curFilter = s;
    ['ALL','FAIL','SKIP','PASS'].forEach(function(k){{
      document.getElementById('f-'+k.toLowerCase()).classList.toggle('active', k===s);
    }});
    applyFilters();
  }}
  function applyFilters() {{
    var q = (document.getElementById('search').value || '').toLowerCase();
    document.querySelectorAll('.module').forEach(function(mod) {{
      var anyVisible = false;
      mod.querySelectorAll('.test-row').forEach(function(row) {{
        var okStatus = (curFilter==='ALL') || (row.getAttribute('data-status')===curFilter);
        var okSearch = !q || (row.getAttribute('data-search')||'').indexOf(q) !== -1;
        var show = okStatus && okSearch;
        row.classList.toggle('hidden', !show);
        if (show) anyVisible = true;
      }});
      mod.classList.toggle('hidden', !anyVisible);
      if (anyVisible && (q || curFilter!=='ALL')) mod.classList.remove('collapsed');
    }});
  }}
</script>
</body></html>"""
    return html, {'total': total, 'passed': passed, 'failed': failed, 'skipped': skipped, 'pass_rate': pass_rate}


def main():
    ap = argparse.ArgumentParser(description="Consolidated all-tests HTML report")
    ap.add_argument("results_dir")
    ap.add_argument("output_path")
    ap.add_argument("--exclude", action="append", default=[],
                    help="skip testng-results.xml whose path contains this substring")
    ap.add_argument("--rerun", default=None,
                    help="rerun results dir; its outcomes OVERRIDE the primary run (final state)")
    ap.add_argument("--label", default="",
                    help="header suffix, e.g. 'After Rerun'")
    args = ap.parse_args()
    results_dir, output_path = args.results_dir, args.output_path
    print("=" * 60)
    print("  iOS Consolidated Automation Report"
          + (f"  [{args.label}]" if args.label else ""))
    print("=" * 60)
    all_tests = find_and_parse_all(results_dir, exclude=args.exclude)
    if not all_tests:
        print("ERROR: no test results found.")
        sys.exit(1)
    if args.rerun:
        all_tests = merge_rerun(all_tests, args.rerun)
    modules = group_by_module(all_tests)
    print(f"\nParsed {len(all_tests)} tests across {len(modules)} modules:")
    for mod, tests in modules.items():
        p = sum(1 for t in tests if t['status'] == 'PASS')
        f = sum(1 for t in tests if t['status'] == 'FAIL')
        s = sum(1 for t in tests if t['status'] == 'SKIP')
        print(f"  [{'FAIL' if f else 'PASS'}] {mod}: {p} passed, {f} failed, {s} skipped")
    html, stats = generate_html(modules, label=args.label)
    os.makedirs(os.path.dirname(output_path) or '.', exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as fh:
        fh.write(html)
    print(f"\nConsolidated report written: {output_path}")
    print(f"  Total {stats['total']} | Passed {stats['passed']} | Failed {stats['failed']} | Skipped {stats['skipped']} | {stats['pass_rate']:.1f}% pass")
    gh = os.environ.get('GITHUB_OUTPUT', '')
    if gh:
        with open(gh, 'a') as fh:
            for k in ('total', 'passed', 'failed', 'skipped'):
                fh.write(f"total_{k if k!='total' else 'tests'}={stats[k]}\n")
            fh.write(f"pass_rate={stats['pass_rate']:.1f}\n")
    print("Done.")


if __name__ == '__main__':
    main()
