#!/usr/bin/env python3
"""
Combine the per-module *Detailed_Report*.html files (ExtentReports detailed reports,
each with inline screenshots) into ONE self-contained HTML with a module tab switcher.

Each module report is embedded in an isolated <iframe srcdoc="..."> so its bundled
ExtentReports CSS/JS (and the inline <img> screenshots) render exactly as standalone,
with no style bleed between modules. A left sidebar switches modules.

Usage:
  python3 scripts/combine_detailed_reports.py <input-dir-or-glob> [output.html]

Examples:
  python3 scripts/combine_detailed_reports.py consolidated-detailed-reports/
  python3 scripts/combine_detailed_reports.py reports/detailed/ /tmp/Combined.html
"""
import glob
import html
import os
import re
import sys


def discover(arg):
    """Return a sorted list of per-module detailed HTML files from a dir or glob."""
    if os.path.isdir(arg):
        files = glob.glob(os.path.join(arg, "*Detailed_Report*.html"))
        if not files:  # fall back to any html in the dir
            files = glob.glob(os.path.join(arg, "*.html"))
    else:
        files = glob.glob(arg)
    # de-dup by module, keeping the NEWEST timestamped file per module
    by_module = {}
    for f in files:
        if "Combined_Detailed_Report" in os.path.basename(f):
            continue
        mod = module_key(f)
        if mod not in by_module or os.path.getmtime(f) > os.path.getmtime(by_module[mod]):
            by_module[mod] = f
    return [by_module[k] for k in sorted(by_module, key=module_sort_key)]


def module_key(path):
    base = os.path.basename(path)
    # strip _Detailed_Report_<timestamp>.html (and a few known variants)
    return re.sub(r"[_-]?Detailed[_-]?Report.*\.html$", "", base, flags=re.I) or base


_ORDER = ["auth", "site", "assets-part1", "assets-part2", "assets-part3", "assets-part4",
          "assets-part5", "assets-part6", "issues-phase1", "issues-phase2", "issues-phase3",
          "connections", "location", "offline", "sitevisit", "workorder", "zp"]


def module_sort_key(key):
    k = key.lower()
    for i, name in enumerate(_ORDER):
        # word-boundary match so "sitevisit" does NOT match the "site" prefix
        if k == name or k.startswith(name + "-") or k.startswith(name + "_") or k.startswith(name + " "):
            return (i, k)
    return (len(_ORDER), k)


def pretty(key):
    s = key.replace("-", " ").replace("_", " ").strip()
    return re.sub(r"\b(\w)", lambda m: m.group(1).upper(), s)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    inp = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
        inp if os.path.isdir(inp) else ".", "Combined_Detailed_Report.html")

    files = discover(inp)
    if not files:
        print(f"error: no detailed-report HTML found at {inp}", file=sys.stderr)
        sys.exit(1)

    print(f"Combining {len(files)} module report(s):")
    modules = []
    for f in files:
        with open(f, encoding="utf-8", errors="replace") as fh:
            content = fh.read()
        key = module_key(f)
        # srcdoc attribute: escape & first, then the " delimiter (and ' for safety)
        srcdoc = content.replace("&", "&amp;").replace('"', "&quot;")
        modules.append({"key": key, "name": pretty(key), "srcdoc": srcdoc,
                        "size": len(content), "file": os.path.basename(f)})
        print(f"  • {pretty(key):42} ({len(content):>9,} bytes)  {os.path.basename(f)}")

    nav = "\n".join(
        f'      <button class="eg-tab{" active" if i == 0 else ""}" '
        f'onclick="egShow({i})" id="eg-tab-{i}">{html.escape(m["name"])}</button>'
        for i, m in enumerate(modules))
    # LAZY: the combined file embeds every screenshot, so it can be hundreds of MB.
    # The module markup lives in a data-srcdoc ATTRIBUTE (getAttribute auto-unescapes
    # the &amp;/&quot; back to raw HTML); JS moves it into the iframe's srcdoc only when
    # the tab is first clicked. So the browser renders just ONE module report at a time
    # (not all 12 at once → no freeze on open). Module 0 is wired on load.
    frames = "\n".join(
        f'    <iframe class="eg-frame" id="eg-frame-{i}" data-srcdoc="{m["srcdoc"]}" '
        f'style="display:{"block" if i == 0 else "none"}"></iframe>'
        for i, m in enumerate(modules))

    doc = f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>eGalvanic iOS — Combined Detailed Report ({len(modules)} modules)</title>
<style>
  *{{box-sizing:border-box}} html,body{{margin:0;height:100%;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;background:#1e272e}}
  .eg-wrap{{display:flex;height:100vh}}
  .eg-side{{width:250px;flex:0 0 250px;background:#161e24;color:#cfd8dc;overflow-y:auto;border-right:1px solid #0d1317}}
  .eg-side h2{{font-size:13px;letter-spacing:1px;text-transform:uppercase;color:#7fd1a8;padding:16px 16px 8px;margin:0}}
  .eg-tab{{display:block;width:100%;text-align:left;background:transparent;color:#cfd8dc;border:0;border-left:3px solid transparent;padding:11px 16px;font-size:13px;cursor:pointer}}
  .eg-tab:hover{{background:#1f2a31}}
  .eg-tab.active{{background:#22303a;border-left-color:#2ecc71;color:#fff;font-weight:600}}
  .eg-main{{flex:1;position:relative}}
  .eg-frame{{width:100%;height:100%;border:0;background:#fff}}
  .eg-foot{{padding:10px 16px;font-size:11px;color:#6b7b85}}
</style></head>
<body>
<div class="eg-wrap">
  <nav class="eg-side">
    <h2>Modules ({len(modules)})</h2>
{nav}
    <div class="eg-foot">Combined detailed report &bull; each panel is the module's full ExtentReport with inline screenshots.</div>
  </nav>
  <main class="eg-main">
{frames}
  </main>
</div>
<script>
function egLoad(f){{
  // lazy: copy data-srcdoc -> srcdoc once, so only the viewed module renders
  if(!f.getAttribute('srcdoc') && f.getAttribute('data-srcdoc')!==null){{
    f.srcdoc = f.getAttribute('data-srcdoc');
    f.removeAttribute('data-srcdoc');
  }}
}}
function egShow(n){{
  document.querySelectorAll('.eg-frame').forEach(function(f,i){{
    var on = (i===n); f.style.display = on?'block':'none'; if(on) egLoad(f);
  }});
  document.querySelectorAll('.eg-tab').forEach(function(t,i){{ t.classList.toggle('active', i===n); }});
}}
egShow(0);  // wire the first module on load
</script>
</body></html>
"""
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    with open(out, "w", encoding="utf-8") as fh:
        fh.write(doc)
    print(f"\n✅ Combined report: {out} ({os.path.getsize(out):,} bytes, {len(modules)} modules)")


if __name__ == "__main__":
    main()
